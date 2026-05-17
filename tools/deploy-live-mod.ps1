param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$NoClean,
    [switch]$AllowPrivateBadgeJar,
    [switch]$QueuedWorker,
    [string]$StagingRoot = "",
    [string]$SourceProject = "",
    [string]$DeployAttemptedAt = "",
    [int]$PollSeconds = 5
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$deployRoot = Join-Path $StarsectorDir "mods\Weapons Procurement"
$stateRoot = Join-Path $repoRoot ".agent-deploy"
$stageRootBase = Join-Path $stateRoot "staged"
$stateFile = Join-Path $stateRoot "deploy-live-mod.json"
$logFile = Join-Path $stateRoot "deploy-live-mod.log"

if (-not (Test-Path -LiteralPath $repoRoot)) {
    throw "Repository root not found: $repoRoot"
}

$requiredItems = @(
    "data",
    "jars",
    "mod_info.json",
    "README.md",
    "CONFIG.md",
    "CHANGELOG.md",
    "PACKAGING.md"
)
$optionalItems = @(
    "graphics"
)
$items = @($requiredItems + $optionalItems)

function Write-DeployLog {
    param([string]$Message)
    $line = "$(Get-Date -Format o) $Message"
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    Add-Content -LiteralPath $logFile -Value $line
}

function Get-RelativeDeployItems {
    return $script:items
}

function Get-ZipEntryNames {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

function Assert-DeployJarBoundary {
    param([string]$BaseRoot)

    $jarPath = Join-Path $BaseRoot "jars\weapons-procurement.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Deploy jar not found: $jarPath"
    }
    if ($AllowPrivateBadgeJar) {
        return
    }

    $privateTerms = @(
        "WeaponsProcurementBadgeHelper",
        "WeaponsProcurementBadgeConfig",
        "WeaponsProcurementCountUpdater",
        "weaponsprocurement/extensions/WeaponsProcurementExtensions",
        "weaponsprocurement/internal/WeaponsProcurementBadge",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater"
    )
    $privateEntries = @(Get-ZipEntryNames -Path $jarPath | Where-Object {
        $entry = $_
        $privateTerms | Where-Object { $entry.IndexOf($_, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 }
    })
    if ($privateEntries.Count -gt 0) {
        throw "Refusing clean deploy because the jar contains private patched-badge classes. Rebuild clean with build.ps1, or pass -AllowPrivateBadgeJar for an intentional private deploy. Entries: $($privateEntries -join ', ')"
    }
}

function Assert-DeployRoot {
    if ((Split-Path -Leaf $deployRoot) -ne "Weapons Procurement") {
        throw "Refusing to deploy to unexpected root: $deployRoot"
    }
}

function Test-FileReplaceable {
    param([string]$Path)

    $stream = $null
    try {
        $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        return $true
    } catch {
        return $false
    } finally {
        if ($stream -ne $null) {
            $stream.Close()
        }
    }
}

function Get-DeployBlocker {
    foreach ($item in (Get-RelativeDeployItems)) {
        $target = Join-Path $deployRoot $item
        if (-not (Test-Path -LiteralPath $target)) {
            continue
        }

        $targetItem = Get-Item -LiteralPath $target -Force
        if ($targetItem.PSIsContainer) {
            $files = @(Get-ChildItem -LiteralPath $target -Recurse -File -Force -ErrorAction Stop)
        } else {
            $files = @($targetItem)
        }

        foreach ($file in $files) {
            if (-not (Test-FileReplaceable -Path $file.FullName)) {
                return $file.FullName
            }
        }
    }
    return ""
}

function Copy-DeployItem {
    param(
        [string]$Source,
        [string]$Target
    )

    $sourceItem = Get-Item -LiteralPath $Source
    if ($sourceItem.PSIsContainer) {
        New-Item -ItemType Directory -Force -Path $Target | Out-Null
        Copy-Item -Path (Join-Path $Source "*") -Destination $Target -Recurse -Force -ErrorAction Stop
    } else {
        $parent = Split-Path -Parent $Target
        if (-not (Test-Path -LiteralPath $parent)) {
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
        }
        Copy-Item -LiteralPath $Source -Destination $Target -Force -ErrorAction Stop
    }
}

function Replace-DeployItem {
    param(
        [string]$Source,
        [string]$Target
    )

    if ($NoClean) {
        Copy-DeployItem -Source $Source -Target $Target
        return
    }

    $parent = Split-Path -Parent $Target
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    $leaf = Split-Path -Leaf $Target
    $tempTarget = Join-Path $parent (".wp-deploy-" + $leaf + "-" + [guid]::NewGuid().ToString("N"))
    $backupTarget = Join-Path $parent (".wp-deploy-backup-" + $leaf + "-" + [guid]::NewGuid().ToString("N"))
    try {
        Copy-DeployItem -Source $Source -Target $tempTarget
        if (Test-Path -LiteralPath $Target) {
            Move-Item -LiteralPath $Target -Destination $backupTarget -Force -ErrorAction Stop
        }
        Move-Item -LiteralPath $tempTarget -Destination $Target -Force -ErrorAction Stop
        if (Test-Path -LiteralPath $backupTarget) {
            Remove-Item -LiteralPath $backupTarget -Recurse -Force -ErrorAction SilentlyContinue
        }
    } catch {
        if ((-not (Test-Path -LiteralPath $Target)) -and (Test-Path -LiteralPath $backupTarget)) {
            Move-Item -LiteralPath $backupTarget -Destination $Target -Force
        }
        if (Test-Path -LiteralPath $tempTarget) {
            Remove-Item -LiteralPath $tempTarget -Recurse -Force -ErrorAction SilentlyContinue
        }
        throw
    }
}

function New-DeployStaging {
    $runId = (Get-Date -Format "yyyyMMddHHmmss") + "-" + [guid]::NewGuid().ToString("N")
    $stageRoot = Join-Path $stageRootBase $runId
    New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null

    foreach ($item in (Get-RelativeDeployItems)) {
        $source = Join-Path $repoRoot $item
        $target = Join-Path $stageRoot $item
        if (-not (Test-Path -LiteralPath $source)) {
            if ($optionalItems -contains $item) {
                continue
            }
            throw "Deploy source item not found: $source"
        }
        Copy-DeployItem -Source $source -Target $target
    }

    return $stageRoot
}

function Publish-StagedDeploy {
    param([string]$StageRoot)

    Assert-DeployRoot
    Assert-DeployJarBoundary -BaseRoot $StageRoot
    New-Item -ItemType Directory -Force -Path $deployRoot | Out-Null

    foreach ($item in (Get-RelativeDeployItems)) {
        $source = Join-Path $StageRoot $item
        $target = Join-Path $deployRoot $item
        if (-not (Test-Path -LiteralPath $source)) {
            if (-not $NoClean -and ($optionalItems -contains $item) -and (Test-Path -LiteralPath $target)) {
                Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction Stop
            }
            continue
        }
        Replace-DeployItem -Source $source -Target $target
    }
}

function Read-DeployState {
    if (-not (Test-Path -LiteralPath $stateFile)) {
        return $null
    }
    try {
        return Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Write-DeployState {
    param(
        [string]$RunId,
        [int]$ProcessId,
        [string]$StageRoot,
        [string]$Phase
    )
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    [pscustomobject]@{
        RunId = $RunId
        Pid = $ProcessId
        ScriptPath = $PSCommandPath
        RepoRoot = $repoRoot
        DeployRoot = $deployRoot
        StagingRoot = $StageRoot
        Phase = $Phase
        UpdatedAt = (Get-Date).ToString("o")
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
}

function Stop-OlderQueuedDeploy {
    $state = Read-DeployState
    if ($null -eq $state -or $null -eq $state.Pid) {
        return
    }
    $oldPid = [int]$state.Pid
    if ($oldPid -eq $PID) {
        return
    }
    $oldProcess = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
    if ($null -eq $oldProcess) {
        return
    }
    $commandLine = ""
    try {
        $commandLine = [string](Get-CimInstance Win32_Process -Filter "ProcessId = $oldPid" -ErrorAction Stop).CommandLine
    } catch {
    }
    if ($commandLine.IndexOf($PSCommandPath, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Write-Host "Previous deploy pid=$oldPid is alive but is not this deploy script. Leaving it alone."
        return
    }
    Stop-Process -Id $oldPid -Force
    Write-DeployLog "Cancelled older queued deploy pid=$oldPid."
}

function ConvertTo-ProcessArgument {
    param([string]$Value)
    if ($null -eq $Value) {
        return '""'
    }
    return '"' + ($Value -replace '"', '\"') + '"'
}

function Start-MinimizedNoActivateProcess {
    param([string]$FilePath, [object]$ArgumentList)

    if (-not ("QueuedDeploy.NativeMethods" -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

namespace QueuedDeploy {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct STARTUPINFO {
        public UInt32 cb;
        public string lpReserved;
        public string lpDesktop;
        public string lpTitle;
        public UInt32 dwX;
        public UInt32 dwY;
        public UInt32 dwXSize;
        public UInt32 dwYSize;
        public UInt32 dwXCountChars;
        public UInt32 dwYCountChars;
        public UInt32 dwFillAttribute;
        public UInt32 dwFlags;
        public UInt16 wShowWindow;
        public UInt16 cbReserved2;
        public IntPtr lpReserved2;
        public IntPtr hStdInput;
        public IntPtr hStdOutput;
        public IntPtr hStdError;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct PROCESS_INFORMATION {
        public IntPtr hProcess;
        public IntPtr hThread;
        public UInt32 dwProcessId;
        public UInt32 dwThreadId;
    }

    public static class NativeMethods {
        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        public static extern bool CreateProcess(
            string lpApplicationName,
            string lpCommandLine,
            IntPtr lpProcessAttributes,
            IntPtr lpThreadAttributes,
            bool bInheritHandles,
            UInt32 dwCreationFlags,
            IntPtr lpEnvironment,
            string lpCurrentDirectory,
            ref STARTUPINFO lpStartupInfo,
            out PROCESS_INFORMATION lpProcessInformation);

        [DllImport("kernel32.dll", SetLastError = true)]
        public static extern bool CloseHandle(IntPtr hObject);
    }
}
'@
    }

    $argumentText = if ($ArgumentList -is [array]) { $ArgumentList -join " " } else { [string]$ArgumentList }
    $commandLine = '"' + $FilePath + '"'
    if (-not [string]::IsNullOrWhiteSpace($argumentText)) {
        $commandLine += " $argumentText"
    }

    $startupInfo = New-Object QueuedDeploy.STARTUPINFO
    $startupInfo.cb = [Runtime.InteropServices.Marshal]::SizeOf([type][QueuedDeploy.STARTUPINFO])
    $startupInfo.dwFlags = 0x00000001
    $startupInfo.wShowWindow = 7
    $processInfo = New-Object QueuedDeploy.PROCESS_INFORMATION
    $created = [QueuedDeploy.NativeMethods]::CreateProcess($null, $commandLine, [IntPtr]::Zero, [IntPtr]::Zero, $false, 0x00000010, [IntPtr]::Zero, $null, [ref]$startupInfo, [ref]$processInfo)
    if (-not $created) {
        $errorCode = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        Write-DeployLog "CreateProcess queued worker launch failed with Win32 error $errorCode; falling back to minimized Start-Process."
        try {
            $fallbackProcess = Start-Process -FilePath $FilePath -ArgumentList $argumentText -WindowStyle Minimized -PassThru
            return [pscustomobject]@{ Id = [int]$fallbackProcess.Id }
        } catch {
            throw "Failed to start queued deploy worker. CreateProcess Win32 error: $errorCode; Start-Process fallback failed: $($_.Exception.Message)"
        }
    }

    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hThread)
    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hProcess)
    return [pscustomobject]@{ Id = [int]$processInfo.dwProcessId }
}

function Start-QueuedDeploy {
    param([string]$StageRoot)

    Stop-OlderQueuedDeploy

    $runId = Split-Path -Leaf $StageRoot
    $deployAttemptedAtValue = (Get-Date).ToString("o")
    $powerShellPath = (Get-Process -Id $PID).Path
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $PSCommandPath,
        "-StarsectorDir", $StarsectorDir,
        "-QueuedWorker",
        "-StagingRoot", $StageRoot,
        "-SourceProject", $repoRoot,
        "-DeployAttemptedAt", $deployAttemptedAtValue,
        "-PollSeconds", ([string]$PollSeconds)
    )
    if ($NoClean) {
        $args += "-NoClean"
    }
    if ($AllowPrivateBadgeJar) {
        $args += "-AllowPrivateBadgeJar"
    }

    $argumentLine = ($args | ForEach-Object { ConvertTo-ProcessArgument -Value $_ }) -join " "
    $process = Start-MinimizedNoActivateProcess -FilePath $powerShellPath -ArgumentList $argumentLine
    Write-DeployState -RunId $runId -ProcessId $process.Id -StageRoot $StageRoot -Phase "queued"
    Write-DeployLog "Queued deploy runId=$runId pid=$($process.Id) target=$deployRoot stage=$StageRoot."
    Write-Host "Deploy queued: target is locked. Staged files at $StageRoot; hidden worker pid=$($process.Id) will publish after the lock clears."
}

if ($QueuedWorker) {
    if ([string]::IsNullOrWhiteSpace($StagingRoot) -or -not (Test-Path -LiteralPath $StagingRoot)) {
        throw "Queued deploy worker requires an existing -StagingRoot."
    }
    $deployAttemptedAtValue = if ([string]::IsNullOrWhiteSpace($DeployAttemptedAt)) { (Get-Date).ToString("o") } else { $DeployAttemptedAt }
    $sourceProjectValue = if ([string]::IsNullOrWhiteSpace($SourceProject)) { $repoRoot } else { $SourceProject }
    Write-Host "Source project: $sourceProjectValue"
    Write-Host "Time of attempted deploy: $deployAttemptedAtValue"
    $runId = Split-Path -Leaf $StagingRoot
    Write-DeployState -RunId $runId -ProcessId $PID -StageRoot $StagingRoot -Phase "waiting"
    $lastBlocker = ""
    while ($true) {
        $blocker = Get-DeployBlocker
        if ([string]::IsNullOrWhiteSpace($blocker)) {
            break
        }
        if ($blocker -ne $lastBlocker) {
            Write-DeployLog "Waiting for deploy blocker: $blocker"
            $lastBlocker = $blocker
        }
        Start-Sleep -Seconds $PollSeconds
    }
    Publish-StagedDeploy -StageRoot $StagingRoot
    Write-DeployState -RunId $runId -ProcessId $PID -StageRoot $StagingRoot -Phase "completed"
    Write-DeployLog "Completed queued deploy runId=$runId target=$deployRoot."
    exit 0
}

Assert-DeployJarBoundary -BaseRoot $repoRoot
$stagedRoot = New-DeployStaging
$blockerPath = Get-DeployBlocker
if (-not [string]::IsNullOrWhiteSpace($blockerPath)) {
    Start-QueuedDeploy -StageRoot $stagedRoot
    exit 0
}

Publish-StagedDeploy -StageRoot $stagedRoot
$mode = if ($NoClean) { "copy-over" } else { "clean-sync" }
Write-Host "Deployed Weapons Procurement clean package files to $deployRoot ($mode)"
