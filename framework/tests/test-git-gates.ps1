# test-git-gates.ps1
# Git-backed tests for the CP5 branch-head rule and the docs-commit path.
# Uses a temporary git repository under $env:TEMP. Never touches the real workspace.
# The identity below is set only inside the temporary repository, for these tests.
#
# Run: powershell -NoProfile -ExecutionPolicy Bypass -File framework\tests\test-git-gates.ps1

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$scripts = Join-Path (Split-Path -Parent $here) 'scripts'
$RunScript = Join-Path $scripts 'sdlc-run.ps1'
. (Join-Path $scripts 'sdlc-lib.ps1')

$script:Pass = 0
$script:Fail = 0
function Assert-Test {
    param([bool]$Condition, [string]$Name)
    if ($Condition) { $script:Pass++; Write-Host "  PASS  $Name" } else { $script:Fail++; Write-Host "  FAIL  $Name" }
}

$WS = Join-Path $env:TEMP ('sdlc-git-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $WS 'project\src') -Force | Out-Null
$cfg = [ordered]@{
    framework_version = '0.1.0'; jira_project_key = 'ABC'; base_branch = 'main'; branch_prefix = 'sdlc'
    cp4_granularity = 'task'; verification = @([ordered]@{ name = 'unit'; command = 'echo ok'; stop_on_failure = $true })
    lint_command = 'echo lint'; dependency_audit_command = $null; policy = [ordered]@{}
}
[System.IO.File]::WriteAllText((Join-Path $WS 'project\sdlc.config.json'), (ConvertTo-Json -InputObject $cfg -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
$enc = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Join-Path $WS 'project\src\app.txt'), "v1`n", $enc)

function G {
    # git writes benign warnings (line endings) to stderr. Relax the preference locally so they are not errors.
    param([string[]]$GitArgs)
    $ErrorActionPreference = 'Continue'
    & git -C $WS @GitArgs 2>$null | Out-Null
    return $LASTEXITCODE
}

function Invoke-Sdlc {
    param([string[]]$Arguments)
    $out = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $RunScript @Arguments -Workspace $WS 2>$null
    return @{ code = $LASTEXITCODE; text = (($out | Out-String).Trim()) }
}

function Write-Doc {
    param([string]$Rel, [string]$Body)
    $full = Join-Path $WS $Rel
    New-Item -ItemType Directory -Path (Split-Path -Parent $full) -Force | Out-Null
    [System.IO.File]::WriteAllText($full, $Body, $enc)
}

$key = 'ABC-70'
$revRow = "## Revision log`n| Rev | Date (UTC) | Trigger | Agent | Summary |`n|---|---|---|---|---|`n| 1 | 2026-11-25T10:00Z | initial | agent | draft |`n"
$docs = "project/docs/sdlc/$key"

Write-Host ''
Write-Host '== Repository setup =='
& git -C $WS init -q -b main
# Test-only identity inside this temporary repository, so the commit path can run. It is deleted with the repo.
& git -C $WS config --local user.name 'SDLC Test Agent'
& git -C $WS config --local user.email 'sdlc-test-agent@example.invalid'
Assert-Test (Test-Path (Join-Path $WS '.git')) 'temporary repository created'
$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', $key)
Assert-Test ($r.code -eq 0) 'run initialized inside the repository'
G @('add', 'project') | Out-Null
G @('commit', '-q', '-m', 'base') | Out-Null
G @('switch', '-q', '-c', "sdlc/$key") | Out-Null
$branch = (& git -C $WS branch --show-current 2>$null)
Assert-Test ($branch -eq "sdlc/$key") 'working branch sdlc/ABC-70 checked out'

Write-Host ''
Write-Host '== CP5 approval binds the branch head =='
Write-Doc "$docs/requirements.md" ("# Requirements: $key`n`n## Functional requirements`n| ID | Requirement |`n|---|---|`n| REQ-001 | Works |`n`n$revRow")
Write-Doc "$docs/implementation-plan.md" ("# Plan: $key`n`n$revRow")
Write-Doc "$docs/implementation-log.md" ("# Log: $key`n`n$revRow")
Write-Doc "$docs/code-review.md" ("# Code review: $key`n`n$revRow")
Write-Doc "$docs/verification.md" ("# Verification: $key`n`n$revRow")
foreach ($pair in @(@('requirements', 'requirements-analyst'), @('implementation-plan', 'implementation-planner'), @('implementation-log', 'developer'), @('code-review', 'code-reviewer'), @('verification', 'verifier'))) {
    $r = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', $key, '-Node', $pair[0], '-Agent', $pair[1], '-Rev', '1')
    if ($r.code -ne 0) { Write-Host "  setup problem completing $($pair[0]): $($r.text)" }
}
G @('add', 'project/docs') | Out-Null
G @('commit', '-q', '-m', "$key T1: implement") | Out-Null

$paths = Get-WorkspacePaths -Workspace $WS -Key $key
$deps = Get-Dependencies
$head1 = (& git -C $WS rev-parse HEAD 2>$null)
$null = Write-ApprovalRecord -Paths $paths -Deps $deps -Key $key -Checkpoint 'CP5' -Approver 'Gate Tester' -GitHead $head1
$g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint 'CP5'
Assert-Test ($g.status -eq 'VALID') 'CP5 is VALID at the approved HEAD'

Write-Host ''
Write-Host '== Documentation commits after CP5 do not invalidate it =='
Write-Doc "$docs/changelog-entry.md" ("## [Unreleased]`n`n### Added`n- Test entry ($key)`n`n$revRow")
G @('add', 'project/docs') | Out-Null
G @('commit', '-q', '-m', "$key CP6 docs") | Out-Null
$g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint 'CP5'
Assert-Test ($g.status -eq 'VALID') 'CP5 stays VALID after a docs-only commit'

Write-Host ''
Write-Host '== Code change after CP5 invalidates it =='
[System.IO.File]::WriteAllText((Join-Path $WS 'project\src\app.txt'), "v2`n", $enc)
G @('add', 'project/src') | Out-Null
G @('commit', '-q', '-m', "$key fix") | Out-Null
$g = Get-GateResult -Paths $paths -Deps $deps -Checkpoint 'CP5'
Assert-Test ($g.status -eq 'INVALID' -and (($g.reasons -join ' ') -like '*code changed after approval*')) 'code commit after approval invalidates CP5'

Write-Host ''
Write-Host '== Docs commit refused when its approval is not valid =='
$r = Invoke-Sdlc -Arguments @('-Command', 'checkpoint-commit', '-Key', $key, '-Checkpoint', 'CP3', '-Detail', "$key CP3: approved")
Assert-Test ($r.code -eq 1 -and $r.text -like '*MISSING*') 'docs commit refused without a CP3 approval'

Write-Host ''
Write-Host '== Push gate and identity =='
$r = Invoke-Sdlc -Arguments @('-Command', 'push', '-Key', $key)
Assert-Test ($r.code -eq 1) 'push refused (no remote configured, local-only)'
[System.IO.File]::WriteAllText((Join-Path $WS 'project\src\app.txt'), "v3`n", $enc)
$r = Invoke-Sdlc -Arguments @('-Command', 'checkpoint-commit', '-Key', $key, '-Checkpoint', 'task', '-CommitPaths', 'project/src/app.txt', '-Detail', "$key T2: small change")
$taskOk = ($r.code -eq 0 -and $r.text -match '"committed":\s+true')
Assert-Test $taskOk 'task commit of listed files succeeds with identity configured'
if (-not $taskOk) { Write-Host "    script output: $($r.text)" }
$r = Invoke-Sdlc -Arguments @('-Command', 'checkpoint-commit', '-Key', $key, '-Checkpoint', 'task', '-CommitPaths', '../outside.txt', '-Detail', "$key T3: bad path")
Assert-Test ($r.code -eq 1) 'commit path outside project/ is refused'

Write-Host ''
Remove-Item -LiteralPath $WS -Recurse -Force
Write-Host "Result: $script:Pass passed, $script:Fail failed"
if ($script:Fail -gt 0) { exit 1 }
exit 0
