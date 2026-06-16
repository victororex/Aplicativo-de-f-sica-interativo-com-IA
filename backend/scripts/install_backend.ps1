param(
    [switch]$SkipVoiceModels,
    [string]$PythonExecutable = ""
)

$ErrorActionPreference = "Stop"
$backendDir = Split-Path -Parent $PSScriptRoot
$venvDir = Join-Path $backendDir ".venv"
$venvPython = Join-Path $venvDir "Scripts\python.exe"

if (-not (Test-Path $venvPython)) {
    if ($PythonExecutable) {
        & $PythonExecutable -m venv $venvDir
    }
    else {
        $launcher = Get-Command py -ErrorAction SilentlyContinue
        if ($launcher) {
        & $launcher.Source -3.12 -m venv $venvDir
        }
        else {
            $python = Get-Command python -ErrorAction Stop
            & $python.Source -m venv $venvDir
        }
    }
}

& (Join-Path $PSScriptRoot "install_voice_stack.ps1") `
    -PythonExecutable $venvPython `
    -SkipModelDownload:$SkipVoiceModels

Write-Host ""
Write-Host "Backend instalado. Para iniciar:"
Write-Host "  $venvPython -m uvicorn main:app --host 0.0.0.0 --port 8000"
