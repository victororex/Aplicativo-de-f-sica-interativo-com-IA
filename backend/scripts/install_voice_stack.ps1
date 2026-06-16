param(
    [string]$PythonExecutable = "python",
    [switch]$SkipModelDownload
)

$ErrorActionPreference = "Stop"
$backendDir = Split-Path -Parent $PSScriptRoot
Push-Location $backendDir
try {
    & $PythonExecutable -m pip install --upgrade pip
    & $PythonExecutable -m pip install -r requirements.txt
    & $PythonExecutable -m pip install --no-deps "https://github.com/myshell-ai/MeloTTS/archive/refs/heads/main.zip"
    & $PythonExecutable -m pip install --no-deps "https://github.com/myshell-ai/OpenVoice/archive/refs/heads/main.zip"
    & $PythonExecutable -m nltk.downloader -d nltk_data cmudict averaged_perceptron_tagger averaged_perceptron_tagger_eng

    if (-not $SkipModelDownload) {
        & $PythonExecutable scripts/download_openvoice_models.py --target checkpoints_v2
    }
    & $PythonExecutable scripts/validate_setup.py
}
finally {
    Pop-Location
}
