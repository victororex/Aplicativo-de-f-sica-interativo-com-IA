from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path
import sys

from ia.ai_service import responder_texto, responder_com_imagem, responder_com_documento


PROJECT_ROOT = Path(__file__).resolve().parents[2]
BACKEND_DIR = PROJECT_ROOT / "backend"

if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

_spec = spec_from_file_location("backend_main", BACKEND_DIR / "main.py")
if _spec is None or _spec.loader is None:
    raise ImportError("Nao foi possivel carregar backend/main.py.")

_module = module_from_spec(_spec)
_spec.loader.exec_module(_module)

app = _module.app


def iniciar_chat():
    print("IA Física iniciada.")
    print("1 - Perguntar por texto")
    print("2 - Perguntar com imagem")
    print("3 - Perguntar com documento")
    print("0 - Sair")

    while True:
        opcao = input("\nEscolha uma opção: ")

        if opcao == "0":
            print("Encerrando IA Física.")
            break

        elif opcao == "1":
            pergunta = input("Digite sua pergunta de Física: ")
            resposta = responder_texto(pergunta)
            print("\nResposta da IA:\n")
            print(resposta)

        elif opcao == "2":
            pergunta = input("Digite sua pergunta sobre a imagem: ")
            caminho = input("Digite o caminho da imagem: ")
            resposta = responder_com_imagem(pergunta, caminho)
            print("\nResposta da IA:\n")
            print(resposta)

        elif opcao == "3":
            pergunta = input("Digite sua pergunta sobre o documento: ")
            caminho = input("Digite o caminho do documento PDF/TXT/DOCX: ")
            resposta = responder_com_documento(pergunta, caminho)
            print("\nResposta da IA:\n")
            print(resposta)

        else:
            print("Opção inválida.")


if __name__ == "__main__":
    iniciar_chat()