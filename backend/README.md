# Backend FastAPI

Backend do aplicativo Física Interativa. Fornece IA, OCR matemático, gráficos,
armazenamento, conteúdo e síntese de voz.

## Instalação limpa

Na raiz `backend/`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_backend.ps1
Copy-Item .env.example .env
```

Depois:

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000
```

Swagger: `http://127.0.0.1:8000/docs`

## OCR de fórmulas

Endpoint:

```text
POST /formula/analyze
Content-Type: multipart/form-data
```

Campos:

- `image`: JPG, PNG ou WEBP obrigatório.
- `question`: instrução adicional opcional.

Resposta:

```json
{
  "ocr_text": "texto reconhecido",
  "latex": "v = \\frac{d}{t}",
  "problem_statement": "descrição",
  "steps": [
    {
      "title": "Aplicar a fórmula",
      "explanation": "Divida a distância pelo tempo.",
      "latex": "v = \\frac{100}{20}"
    }
  ],
  "final_answer": "v = 5 m/s",
  "graph": {
    "expression": "5*x",
    "label": "Distância pelo tempo",
    "x_min": 0,
    "x_max": 20,
    "points": [{"x": 0, "y": 0}]
  },
  "narration_text": "Texto preparado para voz.",
  "warnings": []
}
```

O gráfico é opcional. O modelo propõe uma expressão simples e o backend valida e
amostra os pontos com SymPy. Nenhum código fornecido pelo modelo é executado.

## Validação de imagem

- Limite padrão: 8 MB.
- Dimensão normalizada: 2048 px.
- Assinatura decodificada com Pillow.
- Orientação EXIF corrigida.
- Arquivos inválidos retornam `422`.
- Tipo não aceito retorna `415`.
- Arquivo acima do limite retorna `413`.
- Indisponibilidade da IA retorna `503`.

## Configuração

```env
USE_MOCK_AI=true
OPENAI_API_KEY=
MODEL_NAME=gpt-4o-mini
OCR_MODEL=gpt-4o-mini
AI_TIMEOUT_SECONDS=60
FORMULA_MAX_UPLOAD_MB=8
FORMULA_MAX_DIMENSION=2048

OPENVOICE_CHECKPOINTS_DIR=checkpoints_v2
VOICE_REFERENCE_DIR=voices
VOICE_TMP_DIR=tmp
MELOTTS_LANGUAGE=ES
MELOTTS_SPEED=1.0
```

Use `USE_MOCK_AI=true` para desenvolvimento e testes sem chave. Para OCR real,
defina `USE_MOCK_AI=false` e `OPENAI_API_KEY`.

## Voz

`POST /chat/speech` continua recebendo:

```json
{"text": "Texto para narrar"}
```

O retorno é `audio/wav`, gerado por MeloTTS e convertido para a voz de referência
com OpenVoice V2.

## Testes

```powershell
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m compileall app main.py
.\.venv\Scripts\python.exe scripts/validate_setup.py
```

Os testes cobrem normalização, rejeição de arquivos inválidos, segurança das
expressões de gráfico e o contrato HTTP completo em modo mock.

## Endpoints preservados

- `POST /chat/message`
- `POST /chat/speech`
- `POST /files/upload`
- rotas de autenticação, conteúdo, progresso, estatísticas e aprendizado
- `GET /health`
