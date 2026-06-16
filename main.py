from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path
import sys


BACKEND_DIR = Path(__file__).resolve().parent / "backend"

if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

_spec = spec_from_file_location("backend_main", BACKEND_DIR / "main.py")
if _spec is None or _spec.loader is None:
    raise ImportError("Nao foi possivel carregar backend/main.py.")

_module = module_from_spec(_spec)
_spec.loader.exec_module(_module)

app = _module.app