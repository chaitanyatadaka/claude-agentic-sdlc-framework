# sdlc-run.ps1
# Deterministic command dispatcher for the /sdlc orchestrator. Windows PowerShell 5.1.
#
# Usage (from the workspace root):
#   powershell -NoProfile -ExecutionPolicy Bypass -File framework\scripts\sdlc-run.ps1 -Command status -Key PROJ-1
#
# Exit codes: 0 = done or allowed; 1 = refused by a gate or a precondition; 2 = usage or internal error.
# This script never approves anything. Approvals are written only by sdlc-approve.ps1, run by a person.

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('init', 'status', 'revise', 'complete', 'gate', 'event', 'checkpoint-commit', 'push', 'pr-create', 'abort')]
    [string]$Command,
    [string]$Key = '',
    [string]$Node = '',
    [string]$Agent = '',
    [int]$Rev = 0,
    [string]$Checkpoint = '',
    [string]$Unit = '',
    [string]$Type = '',
    [string]$Detail = '',
    [string]$Title = '',
    [string]$CommitPaths = '',
    [string]$Workspace = '.'
)
# Note: do not name a parameter $Paths. PowerShell variable names are case-insensitive, and the
# typed [string] parameter would coerce the hashtable assigned to $paths below into a string.

. (Join-Path $PSScriptRoot 'sdlc-lib.ps1')

function Release-OwnLock {
    # exit inside a try block can skip finally, so release a lock this process owns before exiting.
    if (-not $paths) { return }
    if (Test-Path -LiteralPath $paths.Lock) {
        $l = Read-JsonFile -Path $paths.Lock
        if ($l -and [string]$l.pid -eq [string]$PID) { Remove-Item -LiteralPath $paths.Lock -Force }
    }
}

function Write-Result {
    param($Object, [int]$ExitCode = 0)
    Release-OwnLock
    Write-Output (ConvertTo-Json -InputObject $Object -Depth 12)
    exit $ExitCode
}

function Fail {
    param([string]$Message, [int]$ExitCode = 1)
    Release-OwnLock
    Write-Output (ConvertTo-Json -InputObject ([ordered]@{ ok = $false; error = $Message }) -Depth 4)
    exit $ExitCode
}

if ($Command -ne 'status' -and $Command -ne 'gate' -and -not $Key) { Fail 'Key is required.' 2 }
if ($Key -and -not (Test-JiraKey -Key $Key)) { Fail "Invalid Jira issue key '$Key'. Expected a pattern like ABC-123." 2 }

try {
    $ws = (Resolve-Path -LiteralPath $Workspace).ProviderPath
} catch {
    Fail "Workspace not found: $Workspace" 2
}
if (-not (Test-Path -LiteralPath (Join-Path $ws 'project'))) { Fail "No project/ folder under $ws." 2 }

$deps = Get-Dependencies

if ($Node -and -not ($deps.nodes.PSObject.Properties.Name -contains $Node)) { Fail "Unknown artifact node '$Node'." 2 }
if ($Command -eq 'gate' -and -not $Key) { Fail 'gate needs -Key and -Checkpoint.' 2 }

$pathKey = 'NONE'
if ($Key) { $pathKey = $Key }
$paths = Get-WorkspacePaths -Workspace $ws -Key $pathKey

function Read-State { return (Read-JsonFile -Path $paths.State) }

function Assert-Run {
    if (-not (Test-Path -LiteralPath $paths.State)) { Fail "No run for $Key. Start one with init." 1 }
    $chain = Test-HistoryChain -HistoryPath $paths.History
    if (-not $chain.ok) { Fail "History integrity check failed: $($chain.reason). Stop and investigate before continuing." 1 }
}

function Save-State {
    param($State)
    $State.updated_at = (Get-Date).ToUniversalTime().ToString('o')
    Write-JsonFile -Path $paths.State -Object $State
}

function Get-StatusObject {
    $state = Read-State
    $manifest = Read-JsonFile -Path $paths.Manifest
    $chain = Test-HistoryChain -HistoryPath $paths.History
    $nodes = [ordered]@{}
    foreach ($n in $deps.nodes.PSObject.Properties.Name) {
        $p = Get-NodePath -Paths $paths -Deps $deps -Node $n
        $exists = Test-Path -LiteralPath $p
        $recorded = Get-ManifestNode -Manifest $manifest -Node $n
        $modified = $false
        if ($exists -and $recorded) {
            $modified = ((Get-NormalizedSha256 -Path $p) -ne [string]$recorded.sha256)
        }
        $nodes[$n] = [ordered]@{ exists = $exists; rev = $(if ($recorded) { [int]$recorded.rev } else { 0 }); modified_since_record = $modified }
    }
    $gates = [ordered]@{}
    foreach ($cp in @('CP1', 'CP2', 'CP3', 'CP5', 'CP6')) {
        $g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint $cp -Branch ''
        $gates[$cp] = [ordered]@{ status = $g.status; reasons = $g.reasons }
    }
    return [ordered]@{
        ok = $true
        key = $state.key
        status = $state.status
        phase = $state.phase
        awaiting = $state.awaiting
        delivery = $state.delivery
        branch = $state.branch
        pr_url = $state.pr_url
        history_events = $chain.count
        history_ok = $chain.ok
        git = $state.git
        nodes = $nodes
        gates = $gates
        note = 'CP4 is per unit; query with gate -Checkpoint CP4 -Unit <task>.'
    }
}

switch ($Command) {

    'init' {
        $config = Get-ProjectConfig -Paths $paths
        if ($config.jira_project_key -and $config.jira_project_key -notmatch '^REPLACE') {
            if (-not $Key.StartsWith(($config.jira_project_key + '-'))) {
                Fail "Key $Key does not belong to the configured jira_project_key $($config.jira_project_key)." 1
            }
        }
        if (Test-Path -LiteralPath $paths.Run) { Fail "A run folder for $Key already exists. Use status or resume." 1 }
        if (Test-Path -LiteralPath $paths.DocsRoot) {
            foreach ($other in (Get-ChildItem -LiteralPath $paths.DocsRoot -Directory)) {
                $otherState = Read-JsonFile -Path (Join-Path $other.FullName 'state.json')
                if ($otherState -and @('pr_open', 'aborted') -notcontains [string]$otherState.status) {
                    Fail "Run $($other.Name) is still active (status $($otherState.status)). Only one active run per project is allowed." 1
                }
            }
        }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            foreach ($d in @('input', 'feedback', 'revisions', 'approvals', 'evidence', 'evidence\raw')) {
                New-Item -ItemType Directory -Path (Join-Path $paths.Run $d) -Force | Out-Null
            }
            $git = Get-GitState -Workspace $ws -BaseBranch $config.base_branch
            $warnings = @()
            if (-not $git.is_repo) { $warnings += 'No Git repository at the workspace root. Commits and pushes are unavailable.' }
            elseif (-not $git.repo_root_is_workspace) { $warnings += 'Git repository root is not the workspace root.' }
            if ($git.is_repo -and -not $git.hooks_path_ok) { $warnings += 'core.hooksPath is not .githooks. Git hooks are not active.' }
            if (-not $git.base_branch_exists) { $warnings += "Base branch $($config.base_branch) does not exist yet. Branch creation is deferred." }
            if (-not $git.agent_identity_configured) { $warnings += 'Agent Git identity is not configured. Commits are blocked until it is set.' }
            $delivery = 'local-only'
            if ($git.remote_configured) { $delivery = 'remote' }
            $branch = "$($config.branch_prefix)/$Key"
            $state = [ordered]@{
                key = $Key
                status = 'running'
                phase = 'requirements'
                awaiting = ''
                created_at = (Get-Date).ToUniversalTime().ToString('o')
                updated_at = (Get-Date).ToUniversalTime().ToString('o')
                delivery = $delivery
                branch = $branch
                base_branch = [string]$config.base_branch
                pr_url = $null
                git = $git
                warnings = $warnings
                framework_version = [string]$config.framework_version
            }
            Write-JsonFile -Path $paths.State -Object $state
            Write-JsonFile -Path $paths.Manifest -Object ([ordered]@{ key = $Key; nodes = [ordered]@{} })
            Write-Utf8File -Path $paths.History -Text ''
            Add-HistoryEvent -HistoryPath $paths.History -Type 'run_initialized' -Detail "delivery=$delivery; framework=$($state.framework_version)"
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; key = $Key; run = $paths.Run; delivery = $delivery; branch = $branch; warnings = $warnings; policy = (Get-Policy -Config $config) }) 0
    }

    'status' {
        if (-not $Key) { Fail 'status needs -Key.' 2 }
        Assert-Run
        Write-Result (Get-StatusObject) 0
    }

    'revise' {
        Assert-Run
        if (-not $Node) { Fail 'revise needs -Node.' 2 }
        $def = $deps.nodes.$Node
        if (-not $def -or -not $def.revisioned) { Fail "$Node is not a revisioned artifact." 1 }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $manifest = Read-JsonFile -Path $paths.Manifest
            $recorded = Get-ManifestNode -Manifest $manifest -Node $Node
            $current = 0
            if ($recorded) { $current = [int]$recorded.rev }
            $src = Get-NodePath -Paths $paths -Deps $deps -Node $Node
            $copied = ''
            if (Test-Path -LiteralPath $src) {
                $dest = Join-Path $paths.Revisions "$Node\v$current.md"
                Write-Utf8File -Path $dest -Text ([System.IO.File]::ReadAllText($src, [System.Text.Encoding]::UTF8))
                $copied = $dest
            }
            Add-HistoryEvent -HistoryPath $paths.History -Type 'revision_started' -Node $Node -Detail "from rev $current"
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; node = $Node; previous_rev = $current; next_rev = ($current + 1); preserved_copy = $copied }) 0
    }

    'complete' {
        Assert-Run
        if (-not $Node -or -not $Agent) { Fail 'complete needs -Node and -Agent.' 2 }
        if ($Rev -lt 1 -and $deps.nodes.$Node.revisioned) { Fail 'complete needs -Rev (the revision the agent wrote).' 2 }
        $problems = Test-ArtifactComplete -Paths $paths -Deps $deps -Node $Node -Rev $Rev
        if ($problems.Count -gt 0) {
            Write-Result ([ordered]@{ ok = $false; node = $Node; problems = $problems }) 1
        }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $p = Get-NodePath -Paths $paths -Deps $deps -Node $Node
            $manifest = Read-JsonFile -Path $paths.Manifest
            $nodesTable = [ordered]@{}
            if ($manifest -and $manifest.nodes) {
                foreach ($prop in $manifest.nodes.PSObject.Properties) { $nodesTable[$prop.Name] = $prop.Value }
            }
            $sha = Get-NormalizedSha256 -Path $p
            $newRev = $Rev
            if (-not $deps.nodes.$Node.revisioned) { $newRev = 1 }
            $nodesTable[$Node] = [ordered]@{ path = $deps.nodes.$Node.path; rev = $newRev; sha256 = $sha; produced_by = $Agent; updated_at = (Get-Date).ToUniversalTime().ToString('o') }
            Write-JsonFile -Path $paths.Manifest -Object ([ordered]@{ key = $Key; nodes = $nodesTable })
            Add-HistoryEvent -HistoryPath $paths.History -Type 'node_completed' -Node $Node -Actor $Agent -Detail "rev=$newRev sha256=$sha"
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; node = $Node; rev = $newRev; sha256 = $sha }) 0
    }

    'gate' {
        if (-not $Checkpoint) { Fail 'gate needs -Checkpoint.' 2 }
        if (-not $Key) { Fail 'gate needs -Key.' 2 }
        Assert-Run
        $r = Get-GateResult -Paths $paths -Deps $deps -Checkpoint $Checkpoint -Unit $Unit
        $code = 0
        if ($r.status -ne 'VALID') { $code = 1 }
        Write-Result $r $code
    }

    'event' {
        Assert-Run
        if (-not $Type) { Fail 'event needs -Type.' 2 }
        $allowed = @('human_feedback', 'agent_invoked', 'agent_handoff', 'checkpoint_presented', 'retry', 'failure', 'test_run', 'note', 'resumed', 'escalated')
        if ($allowed -notcontains $Type) { Fail "Event type $Type is not allowed. Allowed: $($allowed -join ', ')." 2 }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            Add-HistoryEvent -HistoryPath $paths.History -Type $Type -Node $Node -Actor ($(if ($Agent) { $Agent } else { 'orchestrator' })) -Detail $Detail
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; type = $Type }) 0
    }

    'checkpoint-commit' {
        Assert-Run
        $state = Read-State
        $git = Get-GitState -Workspace $ws -BaseBranch $state.base_branch
        if (-not $git.is_repo) { Fail 'No Git repository. Commit is unavailable.' 1 }
        if (-not $git.agent_identity_configured) { Fail 'Agent Git identity is not configured for this repository. Commit is blocked until it is set.' 1 }
        if (-not $git.base_branch_exists) { Fail "Base branch $($state.base_branch) does not exist yet." 1 }
        if (-not $Detail) { Fail 'checkpoint-commit needs -Detail (commit message).' 2 }

        $kind = 'docs'
        if ($Checkpoint -eq 'task') { $kind = 'task' }
        if ($kind -eq 'docs') {
            if (-not $Checkpoint) { Fail 'checkpoint-commit needs -Checkpoint (CP1..CP6) or -Checkpoint task.' 2 }
            $g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint $Checkpoint -Unit $Unit
            if ($g.status -ne 'VALID') { Fail "Refusing docs commit: $Checkpoint approval is $($g.status) ($($g.reasons -join '; '))." 1 }
        }
        if ($kind -eq 'task' -and -not $CommitPaths) { Fail 'Task commits need -CommitPaths (files listed for the task).' 2 }

        $toStage = @()
        if ($kind -eq 'docs') { $toStage += "project/docs/sdlc/$Key" }
        if ($CommitPaths) {
            foreach ($item in ($CommitPaths -split ';')) {
                $clean = $item.Trim().Replace('\', '/')
                if (-not $clean) { continue }
                if (-not $clean.StartsWith('project/')) { Fail "Path outside project/ refused: $clean" 1 }
                if ($clean -match '(^|/)\.\.(/|$)' -or $clean.StartsWith('project/docs/sdlc/') -or $clean.StartsWith('project/.claude/') -or $clean -match '(^|/)\.env') {
                    Fail "Path refused by commit policy: $clean" 1
                }
                $toStage += $clean
            }
        }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $addArgs = @('add', '--') + $toStage
            $add = Invoke-GitQuiet -Workspace $ws -GitArgs $addArgs
            if ($add.code -ne 0) { Fail 'git add failed.' 1 }
            $diff = Invoke-GitQuiet -Workspace $ws -GitArgs @('diff', '--cached', '--name-only')
            if ($diff.code -ne 0 -or @($diff.out | Where-Object { $_ }).Count -eq 0) {
                Write-Result ([ordered]@{ ok = $true; committed = $false; reason = 'nothing staged' }) 0
            }
            $commit = Invoke-GitQuiet -Workspace $ws -GitArgs @('commit', '-m', $Detail)
            if ($commit.code -ne 0) { Fail 'git commit failed (pre-commit hook or identity). See git output.' 1 }
            $sha = (Invoke-GitQuiet -Workspace $ws -GitArgs @('rev-parse', 'HEAD')).out[0]
            Add-HistoryEvent -HistoryPath $paths.History -Type 'commit' -Node $Checkpoint -Detail "$sha $Detail"
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; committed = $true; sha = [string]$sha }) 0
    }

    'push' {
        Assert-Run
        $state = Read-State
        if ($state.delivery -ne 'remote') { Fail 'Local-only mode: no remote configured. Push is skipped.' 1 }
        $g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint 'CP5' -Branch $state.branch
        if ($g.status -ne 'VALID') { Fail "Push refused: CP5 is $($g.status) ($($g.reasons -join '; ')). A human must approve CP5 first." 1 }
        $git = Get-GitState -Workspace $ws -BaseBranch $state.base_branch
        if ($git.current_branch -ne $state.branch) { Fail "Push refused: current branch is '$($git.current_branch)', expected '$($state.branch)'." 1 }
        $scope = Invoke-GitQuiet -Workspace $ws -GitArgs @('diff', '--name-only', "$($state.base_branch)...HEAD")
        if ($scope.code -ne 0) { Fail 'Push refused: cannot compare with base branch.' 1 }
        $outside = @($scope.out | Where-Object { $_ -and -not $_.StartsWith('project/') })
        if ($outside.Count -gt 0) { Fail ('Push refused: changes outside project/: ' + ($outside -join ', ')) 1 }
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $push = Invoke-GitQuiet -Workspace $ws -GitArgs @('push', '-u', 'origin', $state.branch)
            if ($push.code -ne 0) { Fail 'git push failed. See git output.' 1 }
            Add-HistoryEvent -HistoryPath $paths.History -Type 'pushed' -Node 'CP5' -Detail $state.branch
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; pushed = $state.branch }) 0
    }

    'pr-create' {
        Assert-Run
        $state = Read-State
        if ($state.delivery -ne 'remote') { Fail 'Local-only mode: PR creation is skipped. Use the PR text manually.' 1 }
        if (-not $Title) { Fail 'pr-create needs -Title.' 2 }
        $g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint 'CP6'
        if ($g.status -ne 'VALID') { Fail "PR creation refused: CP6 is $($g.status) ($($g.reasons -join '; ')). A human must approve CP6 first." 1 }
        $remoteHead = Invoke-GitQuiet -Workspace $ws -GitArgs @('ls-remote', '--heads', 'origin', $state.branch)
        if ($remoteHead.code -ne 0 -or @($remoteHead.out | Where-Object { $_ }).Count -eq 0) {
            Fail 'PR creation refused: branch is not on the remote. Push after CP5 first.' 1
        }
        $gh = Get-Command gh -ErrorAction SilentlyContinue
        $ghPath = 'gh'
        if ($gh) { $ghPath = $gh.Source } elseif (Test-Path -LiteralPath 'C:\Program Files\GitHub CLI\gh.exe') { $ghPath = 'C:\Program Files\GitHub CLI\gh.exe' } else { Fail 'GitHub CLI (gh) is not installed.' 1 }

        $dup = (& $ghPath pr list --head $state.branch --state all --json url 2>$null) | Out-String
        if ($LASTEXITCODE -ne 0) { Fail 'gh pr list failed. Check gh authentication (gh auth status).' 1 }
        if ($dup.Trim() -ne '[]' -and $dup.Trim() -ne '') {
            $existingUrl = (ConvertFrom-Json $dup)[0].url
            Write-Result ([ordered]@{ ok = $true; created = $false; reason = 'PR already exists for this branch'; pr_url = $existingUrl }) 0
        }
        $body = Join-Path $paths.Run 'pr-description.md'
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $url = (& $ghPath pr create --base $state.base_branch --head $state.branch --title $Title --body-file $body 2>$null) | Out-String
            if ($LASTEXITCODE -ne 0) { Fail 'gh pr create failed. Nothing was merged.' 1 }
            $url = $url.Trim()
            $state.pr_url = $url
            $state.status = 'pr_open'
            Save-State -State $state
            Add-HistoryEvent -HistoryPath $paths.History -Type 'pr_opened' -Node 'CP6' -Detail $url
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; created = $true; pr_url = $url; merged = $false }) 0
    }

    'abort' {
        Assert-Run
        $state = Read-State
        Enter-RunLock -Paths $paths -Key $Key
        try {
            $state.status = 'aborted'
            Save-State -State $state
            Add-HistoryEvent -HistoryPath $paths.History -Type 'run_aborted' -Detail 'branch kept; nothing deleted'
        } finally {
            Exit-RunLock -Paths $paths
        }
        Write-Result ([ordered]@{ ok = $true; status = 'aborted'; note = 'Branch and files are kept.' }) 0
    }
}
