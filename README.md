# Física Interativa

App Android educacional para Análise Dimensional com tutor IA ("Renato"), trilha de missões,
desafio diário, OCR de fórmulas e voz personalizada.

Stack: Kotlin 2.2 · Jetpack Compose · MVVM · SharedPreferences (local) · FastAPI · SQLite ·
OpenAI Vision (`gpt-4o-mini`) · MeloTTS → OpenVoice V2.

## Funcionalidades

- Cadastro, login, perfil, progresso e histórico locais.
- Aulas e desafio diário de Análise Dimensional com adaptação por desempenho.
- Trilha de campanha por etapas.
- Chat com tutor IA + voz clonada do professor (fallback para TTS do dispositivo).
- OCR de fórmulas em imagem → passos resolvidos + gráfico.
- Painel adaptativo (Dashboard) com XP, nível, evolução e recomendações.

## Pré-requisitos

- **Android Studio Ladybug+** com JDK 17.
- **Python 3.11+** para o backend.
- Opcional: GPU com CUDA para acelerar a voz clonada.
- Opcional: chave da OpenAI (necessária para OCR remoto e Chat remoto).

## Configuração — Android

1. Copie `local.properties.example` (se existir) ou crie um `local.properties`:
   ```properties
   USE_REMOTE_AI=true
   AI_API_BASE_URL=http://10.0.2.2:8000
   ```
2. Abra o projeto no Android Studio e rode no emulador (`10.0.2.2` aponta para `localhost` do host).

## Configuração — Backend

```bash
cd backend
pip install -r requirements.txt
python main.py
```

Variáveis de ambiente principais:

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `OPENAI_API_KEY` | — | Necessária para OCR e Chat reais. |
| `USE_MOCK_AI` | `true` | Em produção, mude para `false`. |
| `JWT_SECRET_KEY` | placeholder | **Obrigatório** trocar em produção (lifespan aborta se estiver no default). |
| `APP_ENV` | `development` | Use `production` para validações estritas. |
| `VOICE_REFERENCE_DIR` | `voices` | Pasta com `professor.wav` (ou outro `.wav` de fallback). |
| `OPENVOICE_CHECKPOINTS_DIR` | `checkpoints_v2` | Pesos do OpenVoice V2. |
| `MELOTTS_LANGUAGE` | `ES` | Código de idioma do MeloTTS. |
| `ALLOWED_ORIGINS` | locais | Em produção, restrinja explicitamente. |

## Arquitetura de estado

- `LocalBackend.java` é o banco local (SharedPreferences) — fonte primária offline.
- `AppStateBus` (`data/state/AppStateBus.kt`) propaga eventos (`LessonCompleted`,
  `DailyChallengeSubmitted`, `CampaignStageSubmitted`, `ChatMessageSent`, `OcrAnalyzed`,
  `LessonOpened`, `ProfileUpdated`) para que `HomeViewModel`, `ProfileViewModel` e
  `DashboardViewModel` se atualizem sem refresh manual.
- Sessões de estudo são iniciadas/finalizadas em `MainActivity.onResume/onPause`.

## Design System

- Paleta fria em `ui/theme/Color.kt` (Background `#08111F`, Primary `#4F8EF7`, etc.).
- Tipografia enxuta em `Type.kt`.
- Componentes em `ui/components/AppComponents.kt`: `GlassCard`, `PrimaryButton`, `GhostButton`,
  `SectionHeader`, `StatusChip`, `EmptyState`, `AppTopBar`, `BottomNavigationBar`.
- Tokens em `DesignTokens.kt` (`Spacing.*`, `Radius.*`, `Elevation.*`).

## Rate limit do backend

- `/chat/message`: 30 req/min por usuário.
- `/chat/speech`: 10 req/min por usuário.
- `/formula/analyze`: 10 req/min por usuário.

Implementação em `backend/app/rate_limit.py` (in-memory; trocar por Redis para multi-instância).

## Logs

`logging.basicConfig` configurado em `backend/main.py`. Marcadores úteis: `[TTS]`, `[CHAT]`,
`[CONFIG]`. No Android: `Log.i/w/e` tag `"TTS"` no `ChatScreen`.

## Build

- Android debug: `./gradlew :app:assembleDebug`
- Backend dev: `python backend/main.py`

## Estrutura

```
app/src/main/java/com/example/testes/
├── data/
│   ├── api/        # Clients (Chat, Content, Stats, Learning, Auth, Formula, Session)
│   ├── local/      # LocalBackend.java (SharedPreferences store)
│   └── state/      # AppStateBus
├── model/          # Data classes
├── navigation/     # NavGraph, Screen
├── ui/
│   ├── components/ # AppComponents (design system)
│   ├── screens/    # Telas
│   └── theme/      # Color, Type, Theme, DesignTokens
└── viewmodel/      # ViewModels (Home, Profile, Chat, Lessons, Dashboard, FormulaScan)

backend/
├── app/
│   ├── routes/     # FastAPI routes
│   ├── services/   # AI, OCR, Speech, etc.
│   ├── rate_limit.py
│   └── config.py
├── tts/            # MeloTTS + OpenVoice V2
├── voices/         # professor.wav
└── main.py
```
