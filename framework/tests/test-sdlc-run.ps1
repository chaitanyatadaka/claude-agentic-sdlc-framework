# test-sdlc-run.ps1
# Tests for sdlc-lib.ps1, sdlc-run.ps1 and sdlc-approve.ps1.
# Runs against a temporary fake workspace under $env:TEMP. Never touches the real workspace.
# Approval records are written in-process with Write-ApprovalRecord, standing in for the human script,
# because the real script refuses to run without an interactive console.
#
# Run: powershell -NoProfile -ExecutionPolicy Bypass -File framework\tests\test-sdlc-run.ps1

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$frameworkScripts = Join-Path (Split-Path -Parent $here) 'scripts'
$RunScript = Join-Path $frameworkScripts 'sdlc-run.ps1'
$ApproveScript = Join-Path $frameworkScripts 'sdlc-approve.ps1'
$LibScript = Join-Path $frameworkScripts 'sdlc-lib.ps1'
. $LibScript

$script:Pass = 0
$script:Fail = 0
$script:Failures = @()

function Assert-Test {
    param([bool]$Condition, [string]$Name)
    if ($Condition) {
        $script:Pass++
        Write-Host "  PASS  $Name"
    } else {
        $script:Fail++
        $script:Failures += $Name
        Write-Host "  FAIL  $Name"
    }
}

$WS = Join-Path $env:TEMP ('sdlc-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $WS 'project') -Force | Out-Null
$cfg = [ordered]@{
    framework_version = '0.1.0'
    jira_project_key = 'ABC'
    base_branch = 'main'
    branch_prefix = 'sdlc'
    cp4_granularity = 'task'
    verification = @([ordered]@{ name = 'unit'; command = 'echo ok'; stop_on_failure = $true })
    lint_command = 'echo lint'
    dependency_audit_command = $null
    policy = [ordered]@{}
}
[System.IO.File]::WriteAllText((Join-Path $WS 'project\sdlc.config.json'), (ConvertTo-Json -InputObject $cfg -Depth 6), (New-Object System.Text.UTF8Encoding($false)))

function Invoke-Sdlc {
    param([string[]]$Arguments)
    $out = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $RunScript @Arguments -Workspace $WS 2>$null
    $code = $LASTEXITCODE
    $text = ($out | Out-String).Trim()
    $obj = $null
    try { $obj = $text | ConvertFrom-Json } catch { $obj = $null }
    return @{ code = $code; text = $text; obj = $obj }
}

function Write-Artifact {
    param([string]$RelPath, [string]$Text)
    $full = Join-Path $WS $RelPath
    $dir = Split-Path -Parent $full
    if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($full, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

$reqText = @"
# Requirements: ABC-42

## Functional requirements
| ID | Requirement | Acceptance criteria |
|---|---|---|
| REQ-001 | Save a note | Note is persisted; returns id |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T10:00Z | initial | requirements-analyst | first draft |
"@

$archText = @"
# Architecture: ABC-42

## Components
| Component | Responsibility | Requirements |
|---|---|---|
| NoteStore | Persists notes | REQ-001 |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T10:05Z | initial | architect | first draft |
"@

$drText = @"
# Design review: ABC-42

## Findings
| ID | Severity | Concerns | Description | Proposed fix | Disposition |
|---|---|---|---|---|---|
| DR-001 | low | REQ-001 | naming | rename later | |

## Revision log
| Rev | Date (UTC) | Trigger | Agent | Summary |
|---|---|---|---|---|
| 1 | 2026-11-25T10:10Z | initial | design-reviewer | review round 1 |
"@

Write-Host ''
Write-Host '== Validation =='
Assert-Test (Test-JiraKey -Key 'ABC-42') 'accepts ABC-42'
Assert-Test (Test-JiraKey -Key 'PQRS2-7') 'accepts a project key with digits'
Assert-Test (-not (Test-JiraKey -Key 'abc-42')) 'rejects lowercase key'
Assert-Test (-not (Test-JiraKey -Key 'ABC42')) 'rejects key without dash'
Assert-Test (-not (Test-JiraKey -Key '../etc-1')) 'rejects path traversal in key'

$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'bad')
Assert-Test ($r.code -eq 2) 'init rejects invalid key (exit 2)'

Write-Host ''
Write-Host '== Run initialization =='
$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 0) 'init ABC-42 succeeds'
Assert-Test ($r.obj.delivery -eq 'local-only') 'no Git repo gives local-only delivery'
Assert-Test (@($r.obj.warnings | Where-Object { $_ -like '*No Git repository*' }).Count -eq 1) 'init warns that no Git repository exists'
Assert-Test (Test-Path (Join-Path $WS 'project\docs\sdlc\ABC-42\history.jsonl')) 'history file created'
Assert-Test (Test-Path (Join-Path $WS 'project\docs\sdlc\ABC-42\approvals')) 'approvals folder created'
Assert-Test ($r.obj.policy.test_fix_attempts_per_task -eq 3) 'defaults policy is applied'

$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 1) 'second init for same key refused'

$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'ABC-43')
Assert-Test ($r.code -eq 1 -and $r.text -like '*still active*') 'second active run in project refused'
$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'XYZ-1')
Assert-Test ($r.code -eq 1 -and $r.text -like '*does not belong*') 'key from another project is refused by the project key'

Write-Host ''
Write-Host '== Artifact completion =='
Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\input\story.md' -Text "# Story ABC-42`nSave a note."
$r = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', 'ABC-42', '-Node', 'story', '-Agent', 'orchestrator')
Assert-Test ($r.code -eq 0) 'story snapshot completes (not revisioned)'

Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text ($reqText -replace 'Save a note', 'Save a <summary> note')
$r = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', 'ABC-42', '-Node', 'requirements', '-Agent', 'requirements-analyst', '-Rev', '1')
Assert-Test ($r.code -eq 1 -and $r.text -like '*placeholders*') 'placeholder text blocks completion'

Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text $reqText
$r = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', 'ABC-42', '-Node', 'requirements', '-Agent', 'requirements-analyst', '-Rev', '1')
Assert-Test ($r.code -eq 0 -and $r.obj.rev -eq 1) 'clean requirements complete at rev 1'

$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP1')
Assert-Test ($r.code -eq 1 -and $r.obj.status -eq 'MISSING') 'CP1 is MISSING before approval'

Write-Host ''
Write-Host '== Approval (in-process stand-in for the human script) =='
. $LibScript
$paths = Get-WorkspacePaths -Workspace $WS -Key 'ABC-42'
$deps = Get-Dependencies
$null = Write-ApprovalRecord -Paths $paths -Deps $deps -Key 'ABC-42' -Checkpoint 'CP1' -Approver 'Test Approver'
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP1')
Assert-Test ($r.code -eq 0 -and $r.obj.status -eq 'VALID') 'CP1 is VALID after approval'

Write-Host ''
Write-Host '== Invalidation =='
$r = Invoke-Sdlc -Arguments @('-Command', 'revise', '-Key', 'ABC-42', '-Node', 'requirements')
Assert-Test ($r.code -eq 0 -and $r.obj.next_rev -eq 2) 'revise returns next rev 2'
Assert-Test (Test-Path (Join-Path $WS 'project\docs\sdlc\ABC-42\revisions\requirements\v1.md')) 'previous revision preserved'

Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text ($reqText + "`n<!-- edited after approval -->`n")
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP1')
Assert-Test ($r.code -eq 1 -and $r.obj.status -eq 'INVALID' -and ($r.obj.reasons -join ' ') -like '*changed since approval*') 'editing approved requirements invalidates CP1'

Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text $reqText
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP1')
Assert-Test ($r.code -eq 0) 'restoring the approved content makes CP1 VALID again'

# CP2 depends on requirements; approve it, then change requirements upstream.
Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\architecture.md' -Text $archText
Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\design-review.md' -Text $drText
$null = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', 'ABC-42', '-Node', 'architecture', '-Agent', 'architect', '-Rev', '1')
$null = Invoke-Sdlc -Arguments @('-Command', 'complete', '-Key', 'ABC-42', '-Node', 'design-review', '-Agent', 'design-reviewer', '-Rev', '1')
$null = Write-ApprovalRecord -Paths $paths -Deps $deps -Key 'ABC-42' -Checkpoint 'CP2' -Approver 'Test Approver'
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP2')
Assert-Test ($r.code -eq 0) 'CP2 is VALID after approval'

Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text ($reqText + "`n<!-- upstream change -->`n")
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP2')
Assert-Test ($r.code -eq 1 -and ($r.obj.reasons -join ' ') -like '*upstream input requirements changed*') 'upstream requirements change invalidates downstream CP2'
Write-Artifact -RelPath 'project\docs\sdlc\ABC-42\requirements.md' -Text $reqText

Write-Host ''
Write-Host '== Tamper detection =='
$histPath = Join-Path $WS 'project\docs\sdlc\ABC-42\history.jsonl'
$lines = @(Get-Content -LiteralPath $histPath -Encoding UTF8)
$lines[0] = $lines[0] -replace 'run_initialized', 'run_initialised_tampered'
[System.IO.File]::WriteAllText($histPath, (($lines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
$r = Invoke-Sdlc -Arguments @('-Command', 'status', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 1 -and $r.text -like '*integrity*') 'edited history line is detected and status refused'
$lines[0] = $lines[0] -replace 'run_initialised_tampered', 'run_initialized'
[System.IO.File]::WriteAllText($histPath, (($lines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
$r = Invoke-Sdlc -Arguments @('-Command', 'status', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 0 -and $r.obj.history_ok -eq $true) 'restored history verifies again'

$approvalFile = Get-ChildItem -LiteralPath (Join-Path $WS 'project\docs\sdlc\ABC-42\approvals') -Filter 'CP1.v1.json' | Select-Object -First 1
$rec = Get-Content -LiteralPath $approvalFile.FullName -Raw -Encoding UTF8
[System.IO.File]::WriteAllText($approvalFile.FullName, ($rec -replace 'Test Approver', 'Someone Else'), (New-Object System.Text.UTF8Encoding($false)))
$r = Invoke-Sdlc -Arguments @('-Command', 'gate', '-Key', 'ABC-42', '-Checkpoint', 'CP1')
Assert-Test ($r.code -eq 1 -and ($r.obj.reasons -join ' ') -like '*record edited*') 'edited approval record is detected'
[System.IO.File]::WriteAllText($approvalFile.FullName, $rec, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ''
Write-Host '== Gates for push, PR and commit =='
$r = Invoke-Sdlc -Arguments @('-Command', 'push', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 1 -and $r.text -like '*Local-only*') 'push refused in local-only mode'
$r = Invoke-Sdlc -Arguments @('-Command', 'pr-create', '-Key', 'ABC-42', '-Title', 'ABC-42: test')
Assert-Test ($r.code -eq 1 -and $r.text -like '*Local-only*') 'PR creation refused in local-only mode'
$r = Invoke-Sdlc -Arguments @('-Command', 'checkpoint-commit', '-Key', 'ABC-42', '-Checkpoint', 'CP1', '-Detail', 'ABC-42 CP1: approved')
Assert-Test ($r.code -eq 1) 'docs commit refused without a Git repository'

Write-Host ''
Write-Host '== Approval script guard =='
$guard = '' | & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ApproveScript -Key 'ABC-42' -Checkpoint 'CP3' -Workspace $WS 2>$null
$guardCode = $LASTEXITCODE
Assert-Test ($guardCode -eq 2 -and (($guard | Out-String) -like '*interactive*')) 'approval script refuses redirected (non-console) input'

Write-Host ''
Write-Host '== Events and abort =='
$r = Invoke-Sdlc -Arguments @('-Command', 'event', '-Key', 'ABC-42', '-Type', 'human_feedback', '-Node', 'requirements', '-Detail', 'CP1-1')
Assert-Test ($r.code -eq 0) 'allowed event recorded'
$r = Invoke-Sdlc -Arguments @('-Command', 'event', '-Key', 'ABC-42', '-Type', 'approve_everything')
Assert-Test ($r.code -eq 2) 'disallowed event type refused'

$r = Invoke-Sdlc -Arguments @('-Command', 'abort', '-Key', 'ABC-42')
Assert-Test ($r.code -eq 0 -and $r.obj.status -eq 'aborted') 'abort keeps run and marks aborted'
$r = Invoke-Sdlc -Arguments @('-Command', 'init', '-Key', 'ABC-43')
Assert-Test ($r.code -eq 0) 'new run allowed after the previous one is aborted'

Write-Host ''
Remove-Item -LiteralPath $WS -Recurse -Force
Write-Host "Result: $script:Pass passed, $script:Fail failed"
if ($script:Fail -gt 0) {
    Write-Host 'Failures:'
    foreach ($f in $script:Failures) { Write-Host "  - $f" }
    exit 1
}
exit 0
