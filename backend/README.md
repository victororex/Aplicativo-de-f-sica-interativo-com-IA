# Back-end e API

API FastAPI do aplicativo de Fisica com IA. Esta versao ja possui base de MVP:

- cadastro e login com token Bearer;
- senha protegida com hash PBKDF2;
- banco SQLite local para desenvolvimento;
- sessoes de chat e historico persistidos;
- estatisticas de uso por usuario;
- upload autenticado de imagens, PDFs, DOCX e TXT;
- documentacao automatica em `/docs`;
- health check em `/health`;
- configuracao por `.env`.

## Rodar localmente

```powershell
cd backend
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn main:app --reload
```

Servidor:

```text
http://127.0.0.1:8000
```

Documentacao interativa:

```text
http://127.0.0.1:8000/docs
```

## Fluxo recomendado para teste

1. Crie usuario em `POST /auth/register`.
2. Copie o `access_token` retornado.
3. Clique em **Authorize** no Swagger e informe `Bearer SEU_TOKEN`.
4. Envie uma pergunta em `POST /chat/message`.
5. Veja as conversas em `GET /chat/sessions` ou `GET /history`.
6. Veja metricas em `GET /stats/improvement`.

## Endpoints principais

### Autenticacao

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

### Usuario

- `GET /users/me`
- `PUT /users/me`

### Chat

- `POST /chat/message`
- `GET /chat/sessions`
- `GET /chat/sessions/{session_id}`
- `DELETE /chat/sessions/{session_id}`

### Historico

- `GET /history`
- `GET /history/{session_id}`

### Arquivos

- `POST /files/upload`
- `GET /files/{file_id}`
- `DELETE /files/{file_id}`

### Estatisticas

- `GET /stats/improvement`

## IA

Por padrao, a IA usa `USE_MOCK_AI=true`, entao o chat funciona sem chave externa. Para demonstrar com IA real, configure:

```env
USE_MOCK_AI=false
OPENAI_API_KEY=sua_chave
MODEL_NAME=gpt-4o-mini
```

## Voz humana

Para a fala soar realmente humana, configure a chave no backend. O app vai pedir `POST /chat/speech`, receber um MP3 e tocar a resposta com pausas entre os trechos. Se a chave nao estiver configurada, ele usa a voz do aparelho como reserva.

```env
OPENAI_API_KEY=sua_chave
TTS_MODEL=gpt-4o-mini-tts
TTS_VOICE=marin
```

## Pronto para apresentacao

Para apresentar comercialmente, mostre estes pontos:

- o app nunca recebe a chave da IA;
- usuarios possuem token e dados separados;
- historico e metricas ficam salvos no backend;
- uploads passam por validacao de tipo e tamanho;
- a API tem Swagger, health check e configuracao por ambiente.

## Proximos passos para producao real

- trocar SQLite por PostgreSQL;
- configurar HTTPS e dominio;
- usar segredo JWT forte no servidor;
- restringir `ALLOWED_ORIGINS`;
- adicionar rate limit por usuario;
- adicionar logs estruturados e monitoramento;
- criar testes automatizados de API;
- mover arquivos para storage externo em nuvem.
