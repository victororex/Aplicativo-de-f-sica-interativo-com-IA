from pathlib import Path
from pypdf import PdfReader
from docx import Document


def ler_txt(caminho_arquivo: str) -> str:
    with open(caminho_arquivo, "r", encoding="utf-8") as arquivo:
        return arquivo.read()


def ler_pdf(caminho_arquivo: str) -> str:
    leitor = PdfReader(caminho_arquivo)
    textos = []

    for pagina in leitor.pages:
        texto = pagina.extract_text()
        if texto:
            textos.append(texto)

    return "\n".join(textos)


def ler_docx(caminho_arquivo: str) -> str:
    documento = Document(caminho_arquivo)
    paragrafos = []

    for paragrafo in documento.paragraphs:
        if paragrafo.text.strip():
            paragrafos.append(paragrafo.text)

    return "\n".join(paragrafos)


def extrair_texto_documento(caminho_arquivo: str) -> str:
    caminho = Path(caminho_arquivo)

    if not caminho.exists():
        return "ERRO: arquivo não encontrado."

    extensao = caminho.suffix.lower()

    if extensao == ".txt":
        return ler_txt(caminho_arquivo)

    if extensao == ".pdf":
        return ler_pdf(caminho_arquivo)

    if extensao == ".docx":
        return ler_docx(caminho_arquivo)

    return "ERRO: formato não suportado. Use .pdf, .txt ou .docx."