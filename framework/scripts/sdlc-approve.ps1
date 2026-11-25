# sdlc-approve.ps1
# HUMAN-ONLY approval recorder. Run it yourself in a PowerShell console window:
#   powershell -NoProfile -ExecutionPolicy Bypass -File framework\scripts\sdlc-approve.ps1 -Key PROJ-1 -Checkpoint CP1
#
# It refuses to run when input or output is redirected, which is the case for an agent's shell.
# That check is friction, not a security boundary: anything that can run PowerShell can run the
# script. The real protection is that approvals are hash-bound, recorded in a hash-chained history,
# and reviewed by a person. Treat approvals as tamper-evident, not tamper-proof, until OS isolation
# (sandboxing or a separate account) is in place.
#
# Exit codes: 0 = recorded or already valid; 1 = refused; 2 = usage or environment error.

param(
    [Parameter(Mandatory = $true)][string]$Key,
    [Parameter(Mandatory = $true)][string]$Checkpoint,
    [string]$Unit = '',
    [string]$Workspace = '.'
)

. (Join-Path $PSScriptRoot 'sdlc-lib.ps1')

if ([Console]::IsInputRedirected -or [Console]::IsOutputRedirected) {
    Write-Host 'Refused: approve only from an interactive PowerShell console window.'
    exit 2
}
if (-not (Test-JiraKey -Key $Key)) { Write-Host "Invalid Jira issue key '$Key'."; exit 2 }
if ($Checkpoint -notmatch '^CP[1-6]$') { Write-Host "Checkpoint must be CP1..CP6."; exit 2 }

$ws = (Resolve-Path -LiteralPath $Workspace).ProviderPath
$paths = Get-WorkspacePaths -Workspace $ws -Key $Key
$deps = Get-Dependencies

if (-not (Test-Path -LiteralPath $paths.State)) { Write-Host "No run for $Key."; exit 2 }
$cp = $deps.checkpoints.$Checkpoint
if ($cp.per_unit -and -not $Unit) { Write-Host "$Checkpoint needs -Unit (task or batch ID)."; exit 2 }
if (-not $cp.per_unit -and $Unit) { Write-Host "$Checkpoint does not take -Unit."; exit 2 }

$exitCode = 0
Enter-RunLock -Paths $paths -Key $Key
try {
    $chain = Test-HistoryChain -HistoryPath $paths.History
    if (-not $chain.ok) {
        Write-Host "Refused: history integrity check failed ($($chain.reason)). Investigate before approving."
        $exitCode = 1
    }

    if ($exitCode -eq 0) {
        $gate = Get-GateResult -Paths $paths -Deps $deps -Checkpoint $Checkpoint -Unit $Unit
        if ($gate.status -eq 'VALID') {
            Write-Host "$Checkpoint $Unit is already validly approved ($($gate.version)). Nothing to do."
            $exitCode = 0
        }
    }

    if ($exitCode -eq 0 -and $gate.status -ne 'VALID') {
        # Show exactly what is being approved.
        Write-Host ''
        Write-Host "=== $Checkpoint $Unit : $($cp.name) ==="
        $blocked = $false
        Write-Host 'Artifacts this approval covers:'
        foreach ($node in $cp.covers) {
            $p = Get-NodePath -Paths $paths -Deps $deps -Node $node
            if (-not (Test-Path -LiteralPath $p)) {
                Write-Host "  MISSING  $node"
                $blocked = $true
                continue
            }
            $sha = Get-NormalizedSha256 -Path $p
            Write-Host ("  {0}  sha256 {1}" -f (Get-RelativeNodePath -Paths $paths -Deps $deps -Node $node), $sha.Substring(0, 16))
            $problems = Test-ArtifactComplete -Paths $paths -Deps $deps -Node $node -Rev ([int](Get-ManifestNode -Manifest (Read-JsonFile -Path $paths.Manifest) -Node $node).rev)
            foreach ($prob in $problems) {
                Write-Host "    PROBLEM: $prob"
                $blocked = $true
            }
        }
        Write-Host 'Upstream inputs (bound to this approval):'
        foreach ($node in $cp.inputs) {
            $p = Get-NodePath -Paths $paths -Deps $deps -Node $node
            if (Test-Path -LiteralPath $p) {
                Write-Host ("  {0}  sha256 {1}" -f $node, (Get-NormalizedSha256 -Path $p).Substring(0, 16))
            } else {
                Write-Host "  MISSING  $node"
                $blocked = $true
            }
        }

        $gitHead = ''
        $commits = @()
        if ($cp.git -eq 'branch-head') {
            $h = Invoke-GitQuiet -Workspace $ws -GitArgs @('rev-parse', 'HEAD')
            if ($h.code -ne 0) { Write-Host 'Cannot read branch HEAD.'; $blocked = $true } else { $gitHead = [string]$h.out[0] }
            Write-Host "Branch HEAD bound to this approval: $gitHead"
        }
        if ($cp.git -eq 'task-commits') {
            $c = Invoke-GitQuiet -Workspace $ws -GitArgs @('log', '-n', '1', '--format=%H', '--fixed-strings', '--grep', "$Key $Unit`:")
            if ($c.code -ne 0 -or -not $c.out) {
                Write-Host "No commit found for '${Key} ${Unit}:'. Commit the unit before approving CP4."
                $blocked = $true
            } else {
                $commits = @([string]$c.out[0])
                Write-Host "Task commit bound to this approval: $($commits[0])"
            }
        }

        if ($blocked) {
            Write-Host 'Refused: resolve the problems above, then run again.'
            $exitCode = 1
        } else {
            Write-Host ''
            $approver = (Read-Host 'Your name (recorded as approver)').Trim()
            $phrase = "APPROVE $Checkpoint"
            if ($Unit) { $phrase = "$phrase $Unit" }
            $typed = (Read-Host "Type exactly '$phrase' to record approval, or anything else to cancel").Trim()
            if ($typed -cne $phrase) {
                Write-Host 'Cancelled. Nothing was recorded.'
                $exitCode = 1
            } elseif ([string]::IsNullOrWhiteSpace($approver)) {
                Write-Host 'Cancelled: an approver name is required.'
                $exitCode = 1
            } else {
                $file = Write-ApprovalRecord -Paths $paths -Deps $deps -Key $Key -Checkpoint $Checkpoint -Unit $Unit -Approver $approver -GitHead $gitHead -Commits $commits
                Write-Host "Recorded: $file"
                $exitCode = 0
            }
        }
    }
} finally {
    Exit-RunLock -Paths $paths
}
exit $exitCode
