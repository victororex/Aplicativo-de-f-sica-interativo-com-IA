# Física Interativa com IA

Aplicativo mobile para apoio ao ensino de Física, com tutor conversacional, OCR de fórmulas, trilhas gamificadas, estatísticas de aprendizagem e recomendações adaptativas.

O projeto combina um app Android em Kotlin/Jetpack Compose com um backend FastAPI em Python. A proposta é permitir que o estudante tire dúvidas, envie imagens de exercícios, receba explicações passo a passo e acompanhe a própria evolução por meio de métricas educacionais.

## Principais recursos

- **Tutor IA de Física**: conversa com o aluno, responde dúvidas, explica conceitos e resolve exercícios.
- **OCR de fórmulas e exercícios**: recebe imagens, extrai conteúdo matemático e gera resolução comentada.
- **Explicação passo a passo**: estrutura respostas com enunciado, fórmulas, etapas, resposta final e avisos.
- **Síntese de voz**: transforma respostas em áudio com pipeline de TTS e voz de referência.
- **Aprendizado adaptativo**: usa desempenho, frequência e progresso para orientar recomendações.
- **Lógica fuzzy**: classifica evolução do aluno e apoia decisões pedagógicas personalizadas.
- **Analytics educacional**: registra eventos de estudo, uso do chat, progresso e conclusão de atividades.
- **Missões e desafios diários**: adiciona gamificação ao estudo de conteúdos como análise dimensional.
- **Dashboard de progresso**: apresenta evolução geral, pontos fortes, dificuldades e estatísticas.
- **Modo local/mock**: permite desenvolver e testar sem depender de API externa de IA.

## Arquitetura

```text
Aluno
  |
  v
App Android (Kotlin + Jetpack Compose)
  |
  |-- modo local/mock para estudo e testes
  |
  v
Backend FastAPI
  |
  |-- Chat IA
  |-- OCR de fórmulas
  |-- Síntese de voz
  |-- Autenticação e histórico
  |-- Progresso, estatísticas e aprendizagem
  v
SQLite + serviços de IA/TTS
```

## Tecnologias

### Android

- Kotlin
- Jetpack Compose
- Material Design 3
- Navigation Compose
- ViewModel e Lifecycle
- Coroutines
- Coil
- Media3 ExoPlayer
- Renderização de Markdown
- FileProvider para anexos/imagens

### Backend

- Python
- FastAPI
- SQLite
- OpenAI API em modo real
- Modo mock para desenvolvimento
- Pillow para validação de imagens
- SymPy para validação/amostragem de gráficos
- MeloTTS, OpenVoice e Edge TTS para voz
- Pytest e HTTPX para testes

## Estrutura do projeto

```text
.
├── app/                         # Aplicativo Android
│   └── src/main/java/com/example/testes/
│       ├── data/
│       │   ├── api/             # Clientes HTTP e integração com backend
│       │   ├── analytics/       # Motores adaptativos e fuzzy
│       │   ├── image/           # Processamento de imagens de fórmulas
│       │   ├── local/           # Backend local/mock
│       │   └── voice/           # Reprodução e preferências de voz
│       ├── model/               # Modelos de domínio
│       ├── navigation/          # Rotas do app
│       ├── ui/                  # Telas, componentes e tema
│       └── viewmodel/           # Estado das telas
├── backend/                     # API FastAPI
│   ├── app/
│   │   ├── routes/              # Rotas HTTP
│   │   ├── schemas/             # Contratos Pydantic
│   │   └── services/            # IA, OCR, estatísticas e aprendizagem
│   ├── scripts/                 # Instalação, validação e modelos de voz
│   ├── tests/                   # Testes automatizados
│   └── main.py                  # Entrada da API
├── IA-física/IA/                # Versão/experimentos de IA
├── gradle/                      # Configuração Gradle
└── README.md
```

## Fluxos principais

### Conversa com o tutor

```text
Home -> Tutor IA -> Pergunta do aluno -> Backend /chat/message -> Resposta da IA
```

O app pode usar o backend remoto quando `USE_REMOTE_AI=true`. Caso contrário, usa o `LocalBackend` para respostas locais e testes.

### Análise de fórmula por imagem

```text
Tutor IA -> Enviar imagem -> Backend /formula/analyze -> OCR -> Resolução -> App
```

O backend valida tamanho, tipo e assinatura da imagem antes de enviar para a IA. Quando a resposta inclui gráfico, a expressão é validada e amostrada com SymPy; código gerado pelo modelo não é executado.

### Aprendizagem adaptativa

```text
Eventos de uso -> Analytics -> AdaptiveLearningEngine/FuzzyLearningEngine -> Recomendações
```

O sistema considera desempenho, progresso, tempo de estudo e interações para apoiar recomendações e indicadores.

## Pré-requisitos

- Android Studio recente
- JDK 17 ou superior
- Android SDK com API 36
- Python 3.12 recomendado para o backend
- PowerShell no Windows para os scripts de instalação
- Chave da OpenAI apenas se for usar IA/OCR real

## Configuração do Android

1. Clone o repositório:

```powershell
git clone https://github.com/victororex/Aplicativo-de-f-sica-interativo-com-IA.git
cd Aplicativo-de-f-sica-interativo-com-IA
```

2. Crie ou edite `local.properties` na raiz do projeto:

```properties
sdk.dir=C:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
USE_REMOTE_AI=false
AI_API_BASE_URL=http://10.0.2.2:8000
```

Use `10.0.2.2` para acessar o backend rodando no host a partir do emulador Android. Em aparelho físico, use o IP da máquina na rede local.

3. Compile o app:

```powershell
.\gradlew.bat :app:assembleDebug
```

4. Abra no Android Studio ou instale o APK gerado em:

```text
app/build/outputs/apk/debug/
```

## Configuração do backend

1. Entre na pasta do backend:

```powershell
cd backend
```

2. Rode a instalação:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_backend.ps1
```

Para pular download/configuração dos modelos de voz:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_backend.ps1 -SkipVoiceModels
```

3. Configure o ambiente:

```powershell
Copy-Item .env.example .env
```

Variáveis principais:

```env
USE_MOCK_AI=true
OPENAI_API_KEY=
MODEL_NAME=gpt-4o-mini
OCR_MODEL=gpt-4o-mini
AI_TIMEOUT_SECONDS=60
FORMULA_MAX_UPLOAD_MB=8
FORMULA_MAX_DIMENSION=2048
JWT_SECRET_KEY=troque-por-uma-chave-grande-e-secreta
```

Para usar IA real:

```env
USE_MOCK_AI=false
OPENAI_API_KEY=sua_chave_aqui
```

4. Inicie a API:

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000
```

5. Acesse:

- API: `http://127.0.0.1:8000`
- Swagger: `http://127.0.0.1:8000/docs`
- Health check: `http://127.0.0.1:8000/health`

## Endpoints principais

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/health` | Verifica status da API |
| `POST` | `/auth/*` | Autenticação e conta |
| `POST` | `/chat/message` | Envia pergunta ao tutor IA |
| `POST` | `/chat/speech` | Gera áudio WAV a partir de texto |
| `GET` | `/chat/sessions` | Lista histórico de conversas |
| `POST` | `/formula/analyze` | Analisa imagem com fórmula/exercício |
| `POST` | `/files/upload` | Upload de arquivos |
| `GET/POST` | `/stats/*` | Estatísticas de aprendizagem |
| `GET/POST` | `/progress/*` | Progresso do estudante |
| `GET/POST` | `/learning/*` | Dados de aprendizagem adaptativa |
| `GET` | `/voice/status` | Status do pipeline de voz |

## Testes

### Android

```powershell
.\gradlew.bat test
```

Testes relevantes:

- `AdaptiveLearningEngineTest`
- `FuzzyLearningEngineTest`
- `AiMarkdownTest`

### Backend

Na pasta `backend/`:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m compileall app main.py
.\.venv\Scripts\python.exe scripts/validate_setup.py
```

Os testes cobrem validação de imagem, contrato HTTP, markdown de respostas, estatísticas, lógica fuzzy e pipeline de fórmulas em modo mock.

## Segurança e produção

- Troque `JWT_SECRET_KEY` antes de usar em produção.
- Não suba `USE_MOCK_AI=true` em produção se espera respostas reais.
- Restrinja `ALLOWED_ORIGINS` quando `APP_ENV=production`.
- Configure limites de upload conforme infraestrutura.
- Não exponha `OPENAI_API_KEY` em commits, logs ou screenshots.
- Revise `android:usesCleartextTraffic="true"` antes de publicar o app.

## Disciplinas e conceitos aplicados

- Inteligência Artificial
- IA conversacional
- Visão computacional e OCR
- Lógica fuzzy
- Sistemas adaptativos
- Ciência de dados
- Mineração de dados educacionais
- Visualização de dados
- Gamificação
- Desenvolvimento mobile
- APIs REST

## Status

Projeto acadêmico em evolução. A base já contém app Android, backend FastAPI, testes, endpoints de IA/OCR/voz e módulos de aprendizagem adaptativa. Próximos passos naturais incluem documentação de telas, guia de deploy, pipeline CI e exemplos visuais do aplicativo.
