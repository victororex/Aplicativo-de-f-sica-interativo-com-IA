# Aplicativo de Fisica Interativo com IA

Demo Android em Kotlin/Compose para estudo de **Analise Dimensional**. O app funciona sem servidor para login, aulas, desafios, campanha, progresso e historico de conversa. A IA online e opcional e passa pelo backend Python, mantendo a chave fora do APK.

## O que a demo entrega

- Login e cadastro locais por usuario.
- Aulas autorais de Analise Dimensional, com exemplos e treino guiado.
- Desafio diario que so conta uma vez por dia.
- Campanha em formato de fases com exercicios interativos.
- Minha Evolucao com acertos, tempo estimado, perguntas e fases vencidas.
- Chat **Converse com o titio Renato** com tutor local e opcao de IA online.
- Voz opcional: o usuario escolhe se quer escutar as respostas.
- Suporte, configuracoes, logout e exclusao de conta local.

## Arquitetura

```text
Android Kotlin/Compose
    |
    |-- LocalBackend.java
    |      login, aulas, progresso, desafios, campanha e historico
    |
    |-- ChatApiClient
           tutor local por padrao
           backend opcional para IA e voz online
```

O backend Python fica em `backend/` e e opcional para a demo. Ele deve ser usado quando voce quiser apresentar respostas com IA real ou voz gerada online.

## Rodar o app

Abra o projeto no Android Studio ou use:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:installDebug --console=plain
```

O app abre e funciona sem iniciar o backend.

Conta de teste:

```text
E-mail: aluno@demo.com
Senha: 123456
```

## Usar IA online com seguranca

Por padrao, a IA online vem desligada:

```properties
USE_REMOTE_AI=false
AI_API_BASE_URL=http://10.0.2.2:8000
```

Para ligar a IA online apenas na sua maquina, adicione ao `local.properties`:

```properties
USE_REMOTE_AI=true
AI_API_BASE_URL=http://10.0.2.2:8000
```

Depois configure o backend:

```powershell
cd backend
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

No `backend/.env`, use:

```env
USE_MOCK_AI=false
OPENAI_API_KEY=sua_chave
TTS_MODEL=gpt-4o-mini-tts
TTS_VOICE=marin
```

Inicie:

```powershell
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Importante: a chave fica somente no `.env` do backend. Nao coloque chave no codigo Android nem no repositorio.

## Validacao recomendada

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
python -m compileall backend
```

No emulador, confira:

- cadastro e login sem crash;
- somente Analise Dimensional aparece;
- desafio diario bloqueia nova tentativa no mesmo dia;
- campanha com progresso inicial em 0 para usuario novo;
- aulas e campanha atualizam Minha Evolucao;
- chat mantem historico;
- suporte abre pelo login e perfil;
- logout volta para a entrada;
- exclusao de conta remove o usuario local;
- modo claro e escuro continuam legiveis.

## Observacoes para apresentacao

- O app e uma demo local, pensada para funcionar bem no emulador e em apresentacoes.
- A IA online e opcional para nao bloquear a demo quando nao houver internet ou chave configurada.
- O conteudo e autoral em portugues e focado apenas em Analise Dimensional.
- O arquivo de bugreport gerado pelo emulador nao deve entrar no commit.
