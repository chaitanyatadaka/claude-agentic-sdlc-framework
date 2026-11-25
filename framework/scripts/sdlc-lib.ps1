# sdlc-lib.ps1
# Shared deterministic helpers for the SDLC framework. Windows PowerShell 5.1 compatible.
# Dot-source this file. It makes no workflow decisions; sdlc-run.ps1 and sdlc-approve.ps1 do.
# Nothing here contains a project key, a stack, or a story. Everything is taken from arguments
# or from project/sdlc.config.json.

$ErrorActionPreference = 'Stop'

$script:JiraKeyPattern = '^[A-Z][A-Z0-9]+-[0-9]+$'
# Unfilled template placeholders that must not survive into a completed artifact.
$script:PlaceholderPattern = '<(JIRA-KEY|N|n|k|sha|name|summary|reason|path|command|description|date|sha256|list|summary)[^>]*>'
$script:FrameworkRoot = Split-Path -Parent $PSScriptRoot

function Test-JiraKey {
    param([string]$Key)
    return [bool]($Key -cmatch $script:JiraKeyPattern)
}

function Get-StringSha256 {
    param([string]$Text)
    $bytes = (New-Object System.Text.UTF8Encoding($false)).GetBytes($Text)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash($bytes)
    } finally {
        $sha.Dispose()
    }
    return ([BitConverter]::ToString($hash) -replace '-', '').ToLowerInvariant()
}

function Get-NormalizedSha256 {
    # Hash of the file with CRLF normalized to LF, so Windows checkouts do not change hashes.
    param([string]$Path)
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes) -replace "`r`n", "`n"
    return Get-StringSha256 -Text $text
}

function Write-Utf8File {
    param([string]$Path, [string]$Text)
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($Path, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

function Append-Utf8Line {
    param([string]$Path, [string]$Line)
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    [System.IO.File]::AppendAllText($Path, $Line + "`n", (New-Object System.Text.UTF8Encoding($false)))
}

function ConvertTo-OrderedTree {
    # Converts ConvertFrom-Json output to ordered dictionaries, preserving property order and arrays.
    param($Value)
    if ($null -eq $Value) { return $null }
    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $out = [ordered]@{}
        foreach ($p in $Value.PSObject.Properties) {
            $out[$p.Name] = ConvertTo-OrderedTree -Value $p.Value
        }
        return $out
    }
    if ($Value -is [System.Array]) {
        $items = @()
        foreach ($item in $Value) { $items += ,(ConvertTo-OrderedTree -Value $item) }
        return ,$items
    }
    return $Value
}

function Read-JsonFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    return (Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json)
}

function Write-JsonFile {
    param([string]$Path, $Object)
    Write-Utf8File -Path $Path -Text ((ConvertTo-Json -InputObject $Object -Depth 12) + "`n")
}

function Get-FrameworkPaths {
    return @{
        Dependencies = Join-Path $script:FrameworkRoot 'templates\dependencies.json'
        PolicyDefaults = Join-Path $script:FrameworkRoot 'templates\policy.defaults.json'
    }
}

function Get-WorkspacePaths {
    param([string]$Workspace, [string]$Key)
    $ws = (Resolve-Path -LiteralPath $Workspace).ProviderPath
    $project = Join-Path $ws 'project'
    $docs = Join-Path $project 'docs\sdlc'
    $run = Join-Path $docs $Key
    return @{
        Workspace = $ws
        Project = $project
        DocsRoot = $docs
        Run = $run
        Config = Join-Path $project 'sdlc.config.json'
        State = Join-Path $run 'state.json'
        Manifest = Join-Path $run 'manifest.json'
        History = Join-Path $run 'history.jsonl'
        Approvals = Join-Path $run 'approvals'
        Feedback = Join-Path $run 'feedback'
        Revisions = Join-Path $run 'revisions'
        Lock = Join-Path $docs '.lock'
    }
}

function Get-Dependencies {
    $deps = Read-JsonFile -Path (Get-FrameworkPaths).Dependencies
    if (-not $deps) { throw 'Framework dependency graph is missing (framework/templates/dependencies.json).' }
    return $deps
}

function Get-ProjectConfig {
    param($Paths)
    $cfg = Read-JsonFile -Path $Paths.Config
    if (-not $cfg) {
        throw "Missing project configuration: $($Paths.Config). Copy framework/templates/sdlc.config.example.json there and replace every REPLACE value."
    }
    return $cfg
}

function Get-Policy {
    param($Config)
    $policy = Read-JsonFile -Path (Get-FrameworkPaths).PolicyDefaults
    $result = [ordered]@{}
    foreach ($p in $policy.PSObject.Properties) {
        if ($p.Name -eq 'description') { continue }
        $result[$p.Name] = $p.Value
    }
    if ($Config.policy) {
        foreach ($p in $Config.policy.PSObject.Properties) {
            if (-not $result.Contains($p.Name)) { throw "Unknown policy key in project configuration: $($p.Name)" }
            $result[$p.Name] = $p.Value
        }
    }
    return $result
}

function Get-NodePath {
    param($Paths, $Deps, [string]$Node)
    $def = $Deps.nodes.$Node
    if (-not $def) { throw "Unknown artifact node: $Node" }
    if ($def.base -eq 'project') {
        return (Join-Path $Paths.Project $def.path)
    }
    return (Join-Path $Paths.Run $def.path)
}

function Get-RelativeNodePath {
    param($Paths, $Deps, [string]$Node)
    $def = $Deps.nodes.$Node
    if ($def.base -eq 'project') { return ('project/' + $def.path) }
    return ('project/docs/sdlc/' + (Split-Path -Leaf $Paths.Run) + '/' + $def.path)
}

# ---------------------------------------------------------------- history

function Test-HistoryChain {
    # Verifies every line's hash and link to the previous line. Returns ok, count, last hash.
    param([string]$HistoryPath)
    if (-not (Test-Path -LiteralPath $HistoryPath)) {
        return @{ ok = $true; count = 0; last = 'GENESIS'; reason = '' }
    }
    $prev = 'GENESIS'
    $n = 0
    foreach ($line in (Get-Content -LiteralPath $HistoryPath -Encoding UTF8)) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $n++
        $obj = $line | ConvertFrom-Json
        $body = [ordered]@{}
        foreach ($p in $obj.PSObject.Properties) {
            if ($p.Name -ne 'hash') { $body[$p.Name] = [string]$p.Value }
        }
        $h = Get-StringSha256 -Text (ConvertTo-Json -InputObject $body -Compress)
        if ($h -ne [string]$obj.hash) {
            return @{ ok = $false; count = $n; last = $prev; reason = "hash mismatch at event $n" }
        }
        if ([string]$obj.prev_hash -ne $prev) {
            return @{ ok = $false; count = $n; last = $prev; reason = "chain link broken at event $n" }
        }
        $prev = [string]$obj.hash
    }
    return @{ ok = $true; count = $n; last = $prev; reason = '' }
}

function Add-HistoryEvent {
    param(
        [string]$HistoryPath,
        [string]$Type,
        [string]$Node = '',
        [string]$Actor = 'sdlc-run',
        [string]$Detail = ''
    )
    $chain = Test-HistoryChain -HistoryPath $HistoryPath
    if (-not $chain.ok) { throw "Refusing to append: history chain is broken ($($chain.reason))." }
    $seq = [string]($chain.count + 1)
    $body = [ordered]@{
        seq = $seq
        ts = (Get-Date).ToUniversalTime().ToString('o')
        type = $Type
        node = $Node
        actor = $Actor
        detail = $Detail
        prev_hash = [string]$chain.last
    }
    $hash = Get-StringSha256 -Text (ConvertTo-Json -InputObject $body -Compress)
    $full = [ordered]@{}
    foreach ($k in $body.Keys) { $full[$k] = $body[$k] }
    $full['hash'] = $hash
    Append-Utf8Line -Path $HistoryPath -Line (ConvertTo-Json -InputObject $full -Compress)
}

# ---------------------------------------------------------------- locking

function Enter-RunLock {
    param($Paths, [string]$Key)
    if (Test-Path -LiteralPath $Paths.Lock) {
        $existing = Read-JsonFile -Path $Paths.Lock
        $alive = $false
        if ($existing -and $existing.pid) {
            $alive = [bool](Get-Process -Id ([int]$existing.pid) -ErrorAction SilentlyContinue)
        }
        if ($alive) {
            throw "Another SDLC command is running (pid $($existing.pid), key $($existing.key)). Wait for it to finish."
        }
    }
    Write-JsonFile -Path $Paths.Lock -Object ([ordered]@{ pid = [string]$PID; key = $Key; started = (Get-Date).ToUniversalTime().ToString('o') })
}

function Exit-RunLock {
    param($Paths)
    if (Test-Path -LiteralPath $Paths.Lock) { Remove-Item -LiteralPath $Paths.Lock -Force }
}

# ---------------------------------------------------------------- git (read-only helpers)

function Invoke-GitQuiet {
    # Runs git in the workspace and returns @{ code; out }. Never throws on a non-zero exit.
    param([string]$Workspace, [string[]]$GitArgs)
    $prevPref = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & git -C $Workspace @GitArgs 2>$null
        $code = $LASTEXITCODE
    } catch {
        $out = @()
        $code = 1
    } finally {
        $ErrorActionPreference = $prevPref
    }
    return @{ code = $code; out = @($out) }
}

function Get-GitState {
    param([string]$Workspace, [string]$BaseBranch)
    $state = [ordered]@{
        is_repo = $false
        repo_root_is_workspace = $false
        hooks_path_ok = $false
        remote_configured = $false
        base_branch_exists = $false
        current_branch = ''
        agent_identity_configured = $false
    }
    $top = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('rev-parse', '--show-toplevel')
    if ($top.code -ne 0) { return $state }
    $state.is_repo = $true
    $topPath = [string]$top.out[0]
    $wsNorm = $Workspace.Replace('\', '/').TrimEnd('/')
    $state.repo_root_is_workspace = ([string]::Equals($topPath.TrimEnd('/'), $wsNorm, [System.StringComparison]::OrdinalIgnoreCase))

    $hp = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('config', '--local', '--get', 'core.hooksPath')
    $state.hooks_path_ok = ($hp.code -eq 0 -and [string]$hp.out[0] -eq '.githooks')

    $remotes = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('remote')
    $state.remote_configured = ($remotes.code -eq 0 -and @($remotes.out | Where-Object { $_ }).Count -gt 0)

    $base = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('rev-parse', '--verify', '--quiet', "refs/heads/$BaseBranch")
    $state.base_branch_exists = ($base.code -eq 0)

    $br = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('branch', '--show-current')
    if ($br.code -eq 0 -and $br.out) { $state.current_branch = [string]$br.out[0] }

    # Only checks presence. Values are never read or printed.
    $name = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('config', '--local', '--get', 'user.name')
    $mail = Invoke-GitQuiet -Workspace $Workspace -GitArgs @('config', '--local', '--get', 'user.email')
    $state.agent_identity_configured = ($name.code -eq 0 -and $mail.code -eq 0)
    return $state
}

# ---------------------------------------------------------------- approvals

function Get-ApprovalFiles {
    param($Paths, [string]$Checkpoint, [string]$Unit = '')
    if (-not (Test-Path -LiteralPath $Paths.Approvals)) { return @() }
    $stem = if ($Unit) { "$Checkpoint-$Unit" } else { $Checkpoint }
    $files = Get-ChildItem -LiteralPath $Paths.Approvals -Filter "$stem.v*.json" -File | Sort-Object { [int](($_.BaseName -split '\.v')[-1]) }
    return @($files)
}

function Test-ApprovalRecord {
    # Recomputes the record hash over the record without its own record_sha256 field.
    param([string]$Path)
    $raw = Read-JsonFile -Path $Path
    $tree = ConvertTo-OrderedTree -Value $raw
    $claimed = [string]$tree['record_sha256']
    $body = [ordered]@{}
    foreach ($k in $tree.Keys) { if ($k -ne 'record_sha256') { $body[$k] = $tree[$k] } }
    $actual = Get-StringSha256 -Text (ConvertTo-Json -InputObject $body -Depth 12 -Compress)
    return @{ ok = ($claimed -eq $actual); record = $tree }
}

function Write-ApprovalRecord {
    # Writes a new approval version. Called only by sdlc-approve.ps1 (a human-run console script).
    param(
        $Paths, $Deps, [string]$Key, [string]$Checkpoint, [string]$Unit = '',
        [string]$Approver, [string]$GitHead = '', [string[]]$Commits = @()
    )
    $cp = $Deps.checkpoints.$Checkpoint
    if (-not $cp) { throw "Unknown checkpoint: $Checkpoint" }
    if ($cp.per_unit -and -not $Unit) { throw "$Checkpoint requires a unit (task or batch ID)." }
    if (-not $cp.per_unit -and $Unit) { throw "$Checkpoint does not take a unit." }
    if ([string]::IsNullOrWhiteSpace($Approver)) { throw 'An approver name is required.' }

    $covers = [ordered]@{}
    foreach ($node in $cp.covers) {
        $p = Get-NodePath -Paths $Paths -Deps $Deps -Node $node
        if (-not (Test-Path -LiteralPath $p)) { throw "Cannot approve: $node is missing ($p)." }
        $covers[$node] = [ordered]@{ path = (Get-RelativeNodePath -Paths $Paths -Deps $Deps -Node $node); sha256 = (Get-NormalizedSha256 -Path $p) }
    }
    $inputs = [ordered]@{}
    foreach ($node in $cp.inputs) {
        $p = Get-NodePath -Paths $Paths -Deps $Deps -Node $node
        if (-not (Test-Path -LiteralPath $p)) { throw "Cannot approve: input $node is missing ($p)." }
        $inputs[$node] = Get-NormalizedSha256 -Path $p
    }

    $existing = @(Get-ApprovalFiles -Paths $Paths -Checkpoint $Checkpoint -Unit $Unit)
    $version = $existing.Count + 1
    $supersedes = ''
    if ($existing.Count -gt 0) { $supersedes = $existing[-1].Name }

    $body = [ordered]@{
        key = $Key
        checkpoint = $Checkpoint
        unit = $Unit
        version = [string]$version
        decision = 'approved'
        covers = $covers
        inputs = $inputs
        git = [ordered]@{ head = $GitHead; commits = @($Commits) }
        approved_by = $Approver
        approved_at = (Get-Date).ToUniversalTime().ToString('o')
        supersedes = $supersedes
    }
    $hash = Get-StringSha256 -Text (ConvertTo-Json -InputObject $body -Depth 12 -Compress)
    $record = [ordered]@{}
    foreach ($k in $body.Keys) { $record[$k] = $body[$k] }
    $record['record_sha256'] = $hash

    $stem = if ($Unit) { "$Checkpoint-$Unit" } else { $Checkpoint }
    $file = Join-Path $Paths.Approvals ("$stem.v$version.json")
    Write-JsonFile -Path $file -Object $record
    Add-HistoryEvent -HistoryPath $Paths.History -Type 'approval_recorded' -Node $Checkpoint -Actor $Approver -Detail ("$stem v$version sha256=$hash")
    return $file
}

function Get-GateResult {
    # Deterministic validity check for one checkpoint (and unit, for CP4).
    param($Paths, $Deps, [string]$Checkpoint, [string]$Unit = '', [string]$Branch = '')
    $result = [ordered]@{ checkpoint = $Checkpoint; unit = $Unit; status = 'MISSING'; version = ''; reasons = @() }
    $cp = $Deps.checkpoints.$Checkpoint
    if (-not $cp) { $result.reasons = @("unknown checkpoint $Checkpoint"); return $result }
    if ($cp.per_unit -and -not $Unit) { $result.reasons = @('unit required'); return $result }

    $files = @(Get-ApprovalFiles -Paths $Paths -Checkpoint $Checkpoint -Unit $Unit)
    if ($files.Count -eq 0) { $result.reasons = @('no approval record'); return $result }
    $latest = $files[-1]
    $check = Test-ApprovalRecord -Path $latest.FullName
    $result.version = $latest.Name
    if (-not $check.ok) {
        $result.status = 'INVALID'
        $result.reasons = @('approval record hash mismatch (record edited)')
        return $result
    }
    $rec = $check.record
    $reasons = @()

    foreach ($node in $cp.covers) {
        $p = Get-NodePath -Paths $Paths -Deps $Deps -Node $node
        if (-not (Test-Path -LiteralPath $p)) { $reasons += "$node missing"; continue }
        $now = Get-NormalizedSha256 -Path $p
        if ($now -ne [string]$rec.covers[$node].sha256) { $reasons += "$node changed since approval" }
    }
    foreach ($node in $cp.inputs) {
        $p = Get-NodePath -Paths $Paths -Deps $Deps -Node $node
        if (-not (Test-Path -LiteralPath $p)) { $reasons += "input $node missing"; continue }
        $now = Get-NormalizedSha256 -Path $p
        if ($now -ne [string]$rec.inputs[$node]) { $reasons += "upstream input $node changed since approval" }
    }

    if ($cp.git -eq 'branch-head') {
        # The approved HEAD must still be an ancestor of HEAD, and every commit since then may only
        # touch this run's documents or the project changelog. Any code change invalidates the approval.
        $head = Invoke-GitQuiet -Workspace $Paths.Workspace -GitArgs @('rev-parse', 'HEAD')
        $approvedHead = [string]$rec.git.head
        if ($head.code -ne 0) {
            $reasons += 'cannot read branch HEAD'
        } elseif ([string]$head.out[0] -ne $approvedHead) {
            $anc = Invoke-GitQuiet -Workspace $Paths.Workspace -GitArgs @('merge-base', '--is-ancestor', $approvedHead, 'HEAD')
            if ($anc.code -ne 0) {
                $reasons += 'approved branch HEAD is no longer an ancestor (history rewritten)'
            } else {
                $runKey = Split-Path -Leaf $Paths.Run
                $changed = Invoke-GitQuiet -Workspace $Paths.Workspace -GitArgs @('diff', '--name-only', $approvedHead, 'HEAD')
                $offScope = @($changed.out | Where-Object { $_ -and -not ($_.StartsWith("project/docs/sdlc/$runKey/") -or $_ -eq 'project/CHANGELOG.md') })
                if ($offScope.Count -gt 0) { $reasons += ('code changed after approval: ' + ($offScope -join ', ')) }
            }
        }
    }
    if ($cp.git -eq 'task-commits') {
        foreach ($c in @($rec.git.commits)) {
            $exists = Invoke-GitQuiet -Workspace $Paths.Workspace -GitArgs @('cat-file', '-e', ([string]$c + '^{commit}'))
            if ($exists.code -ne 0) { $reasons += "approved commit $c not found" }
        }
    }

    $result.reasons = $reasons
    if ($reasons.Count -eq 0) { $result.status = 'VALID' } else { $result.status = 'INVALID' }
    return $result
}

# ---------------------------------------------------------------- artifact checks

function Test-ArtifactComplete {
    # Returns a list of problems. Empty list means the artifact is ready to be recorded.
    param($Paths, $Deps, [string]$Node, [int]$Rev)
    $problems = @()
    $def = $Deps.nodes.$Node
    $p = Get-NodePath -Paths $Paths -Deps $Deps -Node $Node
    if (-not (Test-Path -LiteralPath $p)) { return @("file not found: $p") }
    if (-not $def.revisioned) { return $problems }

    $text = Get-Content -LiteralPath $p -Raw -Encoding UTF8
    if ($text -notmatch '## Revision log') { $problems += 'no "## Revision log" section' }
    if ($Rev -gt 0 -and ($text -notmatch ("(?m)^\|\s*$Rev\s*\|"))) { $problems += "no revision log row for rev $Rev" }
    $placeholders = @([regex]::Matches($text, $script:PlaceholderPattern) | ForEach-Object { $_.Value } | Select-Object -Unique)
    if ($placeholders.Count -gt 0) { $problems += ('unfilled placeholders: ' + ($placeholders -join ', ')) }
    return $problems
}

function Get-ManifestNode {
    param($Manifest, [string]$Node)
    if (-not $Manifest) { return $null }
    return $Manifest.nodes.$Node
}
