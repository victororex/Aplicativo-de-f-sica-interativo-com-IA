from __future__ import annotations

import argparse
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path


OPENVOICE_V2_URL = "https://myshell-public-repo-hosting.s3.amazonaws.com/openvoice/checkpoints_v2_0417.zip"
OPENVOICE_V2_REPO = "myshell-ai/OpenVoiceV2"


def download_checkpoints(target_dir: Path, force: bool = False, archive: Path | None = None, url: str = OPENVOICE_V2_URL) -> None:
    target_dir = target_dir.resolve()
    converter_config = target_dir / "converter" / "config.json"
    converter_checkpoint = target_dir / "converter" / "checkpoint.pth"
    if converter_config.exists() and converter_checkpoint.exists() and not force:
        print(f"OpenVoice V2 checkpoints already exist at {target_dir}")
        return

    target_dir.parent.mkdir(parents=True, exist_ok=True)
    archive_path = archive.resolve() if archive else target_dir.parent / "checkpoints_v2_0417.zip"
    extract_dir = target_dir.parent / "_openvoice_extract"

    if archive is None and archive_path.exists() and force:
        archive_path.unlink()
    if not archive_path.exists():
        print(f"Downloading OpenVoice V2 checkpoints to {archive_path}")
        try:
            urllib.request.urlretrieve(url, archive_path)
        except Exception as error:
            try:
                from huggingface_hub import snapshot_download

                print(f"ZIP download failed ({error}); trying Hugging Face repository {OPENVOICE_V2_REPO}")
                snapshot_download(
                    repo_id=OPENVOICE_V2_REPO,
                    local_dir=target_dir,
                    allow_patterns=["converter/*", "base_speakers/ses/*"],
                )
                if converter_config.exists() and converter_checkpoint.exists():
                    print(f"OpenVoice V2 checkpoints ready at {target_dir}")
                    return
            except Exception as fallback_error:
                raise RuntimeError(
                    "Could not download OpenVoice V2 checkpoints automatically.\n"
                    f"URL tried: {url}\n"
                    f"Hugging Face repository tried: {OPENVOICE_V2_REPO}\n"
                    "Download the checkpoint zip manually from the OpenVoice V2 documentation "
                    "and run:\n"
                    "  python scripts/download_openvoice_models.py --archive C:\\path\\to\\checkpoints_v2_0417.zip"
                ) from fallback_error

    if extract_dir.exists():
        shutil.rmtree(extract_dir)
    extract_dir.mkdir(parents=True, exist_ok=True)

    print("Extracting OpenVoice V2 checkpoints")
    with zipfile.ZipFile(archive_path) as archive:
        archive.extractall(extract_dir)

    extracted_root = extract_dir / "checkpoints_v2"
    if not extracted_root.exists():
        raise RuntimeError("Downloaded archive did not contain checkpoints_v2.")

    if target_dir.exists():
        shutil.rmtree(target_dir)
    shutil.move(str(extracted_root), str(target_dir))
    shutil.rmtree(extract_dir)
    print(f"OpenVoice V2 checkpoints ready at {target_dir}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Download OpenVoice V2 checkpoints.")
    parser.add_argument("--target", default="checkpoints_v2", help="Destination directory.")
    parser.add_argument("--archive", default=None, help="Use a local checkpoints_v2_0417.zip instead of downloading.")
    parser.add_argument("--url", default=OPENVOICE_V2_URL, help="Checkpoint zip URL.")
    parser.add_argument("--force", action="store_true", help="Download/extract again.")
    args = parser.parse_args()
    try:
        download_checkpoints(
            Path(args.target),
            force=args.force,
            archive=Path(args.archive) if args.archive else None,
            url=args.url,
        )
    except RuntimeError as error:
        print(error, file=sys.stderr)
        raise SystemExit(1) from error


if __name__ == "__main__":
    main()
