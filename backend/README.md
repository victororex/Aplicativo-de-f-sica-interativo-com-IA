# Backend opcional de IA

Este backend FastAPI e opcional para a demo Android. O app usa dados locais para login, aulas, desafios, campanha e progresso. O backend entra apenas quando voce quiser demonstrar:

- resposta com IA online em `POST /chat/message`;
- voz online em MP3 em `POST /chat/speech`;
- chave de IA protegida no `.env`, fora do app Android.

## Rodar localmente

```powershell
cd backend
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Swagger:

```text
http://127.0.0.1:8000/docs
```

No emulador Android, o app acessa o computador por:

```text
http://10.0.2.2:8000
```

## Configurar IA real

No `backend/.env`:

```env
USE_MOCK_AI=false
OPENAI_API_KEY=sua_chave
MODEL_NAME=gpt-4o-mini
TTS_MODEL=gpt-4o-mini-tts
TTS_VOICE=marin
```

No `local.properties` do projeto Android:

```properties
USE_REMOTE_AI=true
AI_API_BASE_URL=http://10.0.2.2:8000
```

Se `USE_REMOTE_AI=false`, o app usa o tutor local e continua funcionando sem backend.

## Endpoints usados pelo app

- `POST /chat/message`
- `POST /chat/speech`

Os demais endpoints do backend ficam como base tecnica antiga do projeto e nao sao necessarios para a demo local atual.

## Validacao

```powershell
python -m compileall backend
```

Nunca coloque `OPENAI_API_KEY` no app Android ou em arquivos versionados.
