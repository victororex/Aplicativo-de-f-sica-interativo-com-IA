# Aplicativo de Física Interativo com IA

Aplicativo mobile desenvolvido para auxiliar estudantes no estudo de Física em nível universitário, utilizando Inteligência Artificial para responder dúvidas, explicar conteúdos, resolver exercícios e interagir com o usuário de forma didática.

O projeto possui arquitetura modular, separando o aplicativo mobile, back-end, módulo de IA, banco de dados e futuras integrações com voz, avatar e documentos.

---

## Objetivo do Projeto

O objetivo principal do aplicativo é oferecer uma ferramenta interativa de estudo de Física, permitindo que o usuário:

- Faça perguntas sobre conteúdos de Física;
- Receba explicações passo a passo;
- Envie imagens ou enunciados de exercícios;
- Tenha uma experiência mais interativa com IA;
- Utilize futuramente recursos de voz e avatar;
- Acesse histórico de dúvidas e respostas;
- Estude com apoio de exemplos, fórmulas e resoluções guiadas.

---

## Descrição Geral

O aplicativo funciona como uma plataforma de estudo com IA. O usuário interage pelo celular, enviando dúvidas em texto, imagem ou futuramente por voz. Essas informações são enviadas ao back-end, que processa a requisição, consulta o módulo de IA e retorna uma resposta organizada para o aplicativo.

A IA é focada em Física, com prioridade em explicações didáticas, resolução passo a passo e linguagem adequada ao nível do estudante.

---

## Tecnologias Utilizadas

### Mobile

- Kotlin
- Android Studio
- Jetpack Compose ou XML Layouts
- Retrofit ou Ktor Client para comunicação com API
- Material Design

### Back-end

- Python
- FastAPI
- Uvicorn
- Pydantic
- JWT para autenticação
- Integração com API de IA

### Inteligência Artificial

- Python
- OpenAI API ou outro provedor de IA
- Processamento de texto
- Suporte futuro para imagens
- Suporte futuro para documentos
- Suporte futuro para voz

### Banco de Dados

- PostgreSQL
- SQLAlchemy
- Alembic para migrações
- SQLite apenas para testes locais simples

### Outras Ferramentas

- Git
- GitHub
- VS Code
- Postman ou Insomnia
- Docker futuramente
- `.env` para variáveis sensíveis

---

## Arquitetura do Sistema

```text
Usuário
  |
  v
Aplicativo Mobile Kotlin
  |
  v
Back-end FastAPI
  |
  |---- Banco de Dados
  |
  |---- Módulo de IA em Python
  |
  |---- Serviço de autenticação
  |
  |---- Serviço de histórico
  |
  |---- Serviço de arquivos/imagens
  |
  v
API de Inteligência Artificial
Fluxo Básico de Funcionamento
O usuário abre o aplicativo no celular.
O usuário envia uma dúvida de Física.
O app envia a pergunta para o back-end.
O back-end valida os dados recebidos.
O back-end envia a pergunta para o módulo de IA.
A IA gera uma resposta didática.
O back-end salva a interação no banco de dados.
O app recebe a resposta e exibe ao usuário.
Estrutura Inicial de Pastas
aplicativo-fisica-ia/
│
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
│
├── mobile/
│   ├── README.md
│   └── app/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   │   └── com/
│       │   │   │       └── projeto/
│       │   │   │           └── fisicaia/
│       │   │   │               ├── MainActivity.kt
│       │   │   │               ├── data/
│       │   │   │               │   ├── api/
│       │   │   │               │   │   ├── ApiService.kt
│       │   │   │               │   │   └── RetrofitClient.kt
│       │   │   │               │   ├── model/
│       │   │   │               │   │   ├── QuestionRequest.kt
│       │   │   │               │   │   └── AiResponse.kt
│       │   │   │               │   └── repository/
│       │   │   │               │       └── ChatRepository.kt
│       │   │   │               ├── ui/
│       │   │   │               │   ├── screens/
│       │   │   │               │   │   ├── HomeScreen.kt
│       │   │   │               │   │   ├── ChatScreen.kt
│       │   │   │               │   │   └── HistoryScreen.kt
│       │   │   │               │   └── components/
│       │   │   │               │       ├── MessageBubble.kt
│       │   │   │               │       └── InputBox.kt
│       │   │   │               └── viewmodel/
│       │   │   │                   └── ChatViewModel.kt
│       │   │   └── res/
│       │   │       ├── drawable/
│       │   │       ├── mipmap/
│       │   │       └── values/
│       │   └── build.gradle
│       └── build.gradle
│
├── backend/
│   ├── README.md
│   ├── requirements.txt
│   ├── main.py
│   ├── app/
│   │   ├── __init__.py
│   │   ├── config.py
│   │   ├── database.py
│   │   │
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   ├── auth_routes.py
│   │   │   ├── chat_routes.py
│   │   │   ├── user_routes.py
│   │   │   ├── history_routes.py
│   │   │   └── file_routes.py
│   │   │
│   │   ├── models/
│   │   │   ├── __init__.py
│   │   │   ├── user_model.py
│   │   │   ├── chat_model.py
│   │   │   └── file_model.py
│   │   │
│   │   ├── schemas/
│   │   │   ├── __init__.py
│   │   │   ├── user_schema.py
│   │   │   ├── chat_schema.py
│   │   │   └── file_schema.py
│   │   │
│   │   ├── services/
│   │   │   ├── __init__.py
│   │   │   ├── auth_service.py
│   │   │   ├── ai_service.py
│   │   │   ├── chat_service.py
│   │   │   ├── file_service.py
│   │   │   └── history_service.py
│   │   │
│   │   ├── repositories/
│   │   │   ├── __init__.py
│   │   │   ├── user_repository.py
│   │   │   ├── chat_repository.py
│   │   │   └── file_repository.py
│   │   │
│   │   └── utils/
│   │       ├── __init__.py
│   │       ├── security.py
│   │       ├── validators.py
│   │       └── prompts.py
│   │
│   └── tests/
│       ├── test_auth.py
│       ├── test_chat.py
│       └── test_ai.py
│
├── ai/
│   ├── README.md
│   ├── ia_fisica.py
│   ├── prompts/
│   │   ├── system_prompt.txt
│   │   ├── physics_prompt.txt
│   │   └── correction_prompt.txt
│   ├── processors/
│   │   ├── text_processor.py
│   │   ├── image_processor.py
│   │   ├── document_processor.py
│   │   └── audio_processor.py
│   └── tests/
│       └── test_ia_fisica.py
│
├── database/
│   ├── README.md
│   ├── schema.sql
│   ├── seed.sql
│   └── migrations/
│       └── initial_migration.sql
│
├── docs/
│   ├── arquitetura.md
│   ├── requisitos.md
│   ├── fluxo_usuario.md
│   ├── endpoints.md
│   └── modelo_banco.md
│
└── assets/
    ├── images/
    ├── icons/
    └── diagrams/
Responsabilidades por Módulo
Mobile

Responsável pela interface do usuário.

Principais funções:

Exibir tela inicial;
Exibir chat com IA;
Enviar perguntas para o back-end;
Receber respostas da IA;
Mostrar histórico;
Permitir login e cadastro;
Futuramente enviar imagens, documentos e áudio.
Back-end

Responsável por controlar a comunicação entre o aplicativo, banco de dados e IA.

Principais funções:

Receber requisições do app;
Validar dados;
Autenticar usuários;
Enviar perguntas para o módulo de IA;
Salvar histórico de conversas;
Gerenciar arquivos enviados;
Retornar respostas organizadas ao app.
IA

Responsável pela geração das respostas inteligentes.

Principais funções:

Interpretar perguntas de Física;
Gerar explicações didáticas;
Resolver exercícios passo a passo;
Adaptar a explicação ao nível do usuário;
Gerar respostas mais curtas ou detalhadas;
Futuramente interpretar imagens, PDFs e áudio.
Banco de Dados

Responsável por armazenar os dados principais do sistema.

Principais informações armazenadas:

Usuários;
Histórico de perguntas;
Respostas da IA;
Arquivos enviados;
Preferências do usuário;
Logs básicos de uso.
Modelo Inicial do Banco de Dados
Tabela users

Armazena os dados dos usuários.

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
Tabela chat_sessions

Armazena sessões de conversa.

CREATE TABLE chat_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    title VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
Tabela messages

Armazena mensagens enviadas pelo usuário e respostas da IA.

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES chat_sessions(id),
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

Valores esperados para sender:

user
ai
system
Tabela uploaded_files

Armazena arquivos enviados pelo usuário.

CREATE TABLE uploaded_files (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    session_id INTEGER REFERENCES chat_sessions(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_path TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
Tabela user_preferences

Armazena preferências de uso do usuário.

CREATE TABLE user_preferences (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    explanation_level VARCHAR(50) DEFAULT 'intermediario',
    response_style VARCHAR(50) DEFAULT 'passo_a_passo',
    voice_enabled BOOLEAN DEFAULT FALSE,
    avatar_enabled BOOLEAN DEFAULT FALSE
);
Endpoints Iniciais da API
Autenticação
POST /auth/register
POST /auth/login
GET /auth/me
Chat com IA
POST /chat/message
GET /chat/sessions
GET /chat/sessions/{session_id}
DELETE /chat/sessions/{session_id}
Histórico
GET /history
GET /history/{session_id}
Arquivos
POST /files/upload
GET /files/{file_id}
DELETE /files/{file_id}
Usuário
GET /users/me
PUT /users/me
Exemplo de Requisição para IA
Requisição
POST /chat/message
Content-Type: application/json
Authorization: Bearer token_do_usuario
{
  "session_id": 1,
  "message": "Explique a segunda lei de Newton com exemplo.",
  "subject": "Mecânica",
  "level": "universitario"
}
Resposta
{
  "session_id": 1,
  "user_message": "Explique a segunda lei de Newton com exemplo.",
  "ai_response": "A segunda lei de Newton afirma que a força resultante sobre um corpo é igual ao produto da massa pela aceleração...",
  "created_at": "2026-05-19T14:30:00"
}
Configuração do Ambiente
1. Clonar o repositório
git clone https://github.com/seu-usuario/aplicativo-fisica-ia.git
cd aplicativo-fisica-ia
2. Criar ambiente virtual no back-end
cd backend
python -m venv venv

No Windows PowerShell:

.\venv\Scripts\Activate.ps1

No Linux ou macOS:

source venv/bin/activate
3. Instalar dependências
pip install -r requirements.txt
4. Criar arquivo .env

Crie um arquivo .env dentro da pasta backend/:

APP_NAME=Aplicativo de Física com IA
APP_ENV=development
APP_DEBUG=true

DATABASE_URL=postgresql://usuario:senha@localhost:5432/fisica_ia

JWT_SECRET_KEY=sua_chave_secreta
JWT_ALGORITHM=HS256
JWT_EXPIRE_MINUTES=60

AI_API_KEY=sua_chave_da_api
AI_MODEL=gpt-5.4

Nunca envie o arquivo .env para o GitHub.

5. Executar o servidor
uvicorn main:app --reload

Servidor local:

http://127.0.0.1:8000

Documentação automática da API:

http://127.0.0.1:8000/docs
Dependências Iniciais do Back-end

Arquivo backend/requirements.txt:

fastapi
uvicorn
python-dotenv
pydantic
sqlalchemy
psycopg2-binary
alembic
passlib[bcrypt]
python-jose[cryptography]
python-multipart
openai
Exemplo Inicial do main.py
from fastapi import FastAPI
from app.routes import auth_routes, chat_routes, user_routes, history_routes, file_routes

app = FastAPI(
    title="API - Aplicativo de Física com IA",
    description="Back-end responsável pela comunicação entre app mobile, IA e banco de dados.",
    version="0.1.0"
)

app.include_router(auth_routes.router, prefix="/auth", tags=["Autenticação"])
app.include_router(chat_routes.router, prefix="/chat", tags=["Chat"])
app.include_router(user_routes.router, prefix="/users", tags=["Usuários"])
app.include_router(history_routes.router, prefix="/history", tags=["Histórico"])
app.include_router(file_routes.router, prefix="/files", tags=["Arquivos"])

@app.get("/")
def root():
    return {
        "message": "API do Aplicativo de Física com IA funcionando."
    }
Exemplo Inicial do Serviço de IA

Arquivo backend/app/services/ai_service.py:

import os
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

client = OpenAI(api_key=os.getenv("AI_API_KEY"))

AI_MODEL = os.getenv("AI_MODEL", "gpt-5.4")

def generate_physics_response(user_message: str, level: str = "universitario") -> str:
    system_prompt = """
    Você é uma IA especializada em Física para estudantes universitários.
    Responda de forma didática, clara e passo a passo.
    Quando houver cálculo, mostre as fórmulas usadas.
    Não invente dados. Se faltar informação, explique o que está faltando.
    """

    response = client.responses.create(
        model=AI_MODEL,
        input=[
            {
                "role": "system",
                "content": system_prompt
            },
            {
                "role": "user",
                "content": f"Nível do aluno: {level}\nPergunta: {user_message}"
            }
        ]
    )

    return response.output_text
Exemplo Inicial de Rota do Chat

Arquivo backend/app/routes/chat_routes.py:

from fastapi import APIRouter
from app.schemas.chat_schema import ChatRequest, ChatResponse
from app.services.ai_service import generate_physics_response

router = APIRouter()

@router.post("/message", response_model=ChatResponse)
def send_message(request: ChatRequest):
    ai_response = generate_physics_response(
        user_message=request.message,
        level=request.level
    )

    return ChatResponse(
        session_id=request.session_id,
        user_message=request.message,
        ai_response=ai_response
    )
Exemplo de Schema do Chat

Arquivo backend/app/schemas/chat_schema.py:

from pydantic import BaseModel
from typing import Optional

class ChatRequest(BaseModel):
    session_id: Optional[int] = None
    message: str
    subject: Optional[str] = None
    level: str = "universitario"

class ChatResponse(BaseModel):
    session_id: Optional[int]
    user_message: str
    ai_response: str
Integração com o App Mobile

O app em Kotlin deve consumir a API do back-end.

Exemplo de fluxo:

ChatScreen.kt
   |
   v
ChatViewModel.kt
   |
   v
ChatRepository.kt
   |
   v
ApiService.kt
   |
   v
Back-end FastAPI
Exemplo de Endpoint no Kotlin

Arquivo ApiService.kt:

interface ApiService {

    @POST("chat/message")
    suspend fun sendMessage(
        @Body request: QuestionRequest
    ): AiResponse
}
Exemplo de Model no Kotlin

Arquivo QuestionRequest.kt:

data class QuestionRequest(
    val session_id: Int?,
    val message: String,
    val subject: String?,
    val level: String
)

Arquivo AiResponse.kt:

data class AiResponse(
    val session_id: Int?,
    val user_message: String,
    val ai_response: String
)
Funcionalidades Iniciais
Já planejadas
Chat com IA;
Respostas focadas em Física;
Organização por sessões;
Histórico de conversas;
Cadastro e login;
Banco de dados;
Integração mobile com API.
Futuras funcionalidades
Envio de imagens de exercícios;
Leitura de documentos PDF;
Entrada por voz;
Resposta em voz;
Avatar interativo;
Sistema de níveis de explicação;
Geração de exercícios;
Correção de respostas do aluno;
Salvamento de fórmulas favoritas;
Modo revisão para provas.
Divisão de Responsabilidades da Equipe
Área	Responsável	Função
Front-end Mobile	G.Marques	Interface do aplicativo em Kotlin
Back-end	Cauã	API, autenticação, banco e regras de negócio
IA	Victor	Integração com modelo de IA e lógica de resposta
Avatar	João Marcos	Representação visual/interativa do assistente
Banco de Dados	G.Henrique	Modelagem, tabelas e consultas
Voz/Fala	Maria Luisa	Entrada e saída por voz
Padrão de Resposta da IA

A IA deve seguir este padrão:

Entender a dúvida do usuário;
Identificar o assunto de Física;
Explicar o conceito;
Apresentar fórmulas, se necessário;
Resolver passo a passo;
Dar exemplo;
Finalizar com resumo curto.

Exemplo:

Pergunta: O que é energia cinética?

Resposta esperada:
Energia cinética é a energia associada ao movimento de um corpo.

A fórmula é:

Ec = (m · v²) / 2

Onde:
m = massa do corpo
v = velocidade

Exemplo:
Se um corpo tem massa de 2 kg e velocidade de 3 m/s:

Ec = (2 · 3²) / 2
Ec = (2 · 9) / 2
Ec = 18 / 2
Ec = 9 J

Portanto, a energia cinética é 9 joules.
Regras Importantes para a IA

A IA não deve:

Inventar valores que não foram dados;
Resolver exercícios sem explicar o raciocínio;
Dar respostas vagas;
Usar linguagem excessivamente complexa sem necessidade;
Fugir do tema de Física;
Dar certeza quando houver informação incompleta.

A IA deve:

Explicar passo a passo;
Usar fórmulas corretamente;
Adaptar a resposta ao nível do usuário;
Perguntar quando faltar dado;
Corrigir erros conceituais;
Usar exemplos simples.
Segurança

O projeto deve proteger informações sensíveis.

Nunca enviar para o GitHub:

.env
venv/
__pycache__/
chaves de API
senhas
tokens
banco local
arquivos pessoais
Exemplo de .gitignore
# Python
venv/
__pycache__/
*.pyc
*.pyo
*.pyd

# Environment
.env
.env.local

# Database
*.db
*.sqlite3

# IDE
.vscode/
.idea/

# Android
.gradle/
build/
local.properties
*.apk
*.aab

# Logs
*.log

# OS
.DS_Store
Thumbs.db
Como Rodar o Projeto Localmente
Back-end
cd backend
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --reload
Mobile
Abrir a pasta mobile/ no Android Studio.
Configurar o endereço da API.
Rodar o app em emulador ou celular físico.

Durante testes locais, se o app estiver no emulador Android, o endereço da API pode precisar ser:

http://10.0.2.2:8000

Em celular físico, use o IP da máquina na mesma rede Wi-Fi:

http://SEU_IP_LOCAL:8000
Convenção de Commits

Usar mensagens claras:

git add .
git commit -m "Cria estrutura inicial do backend"
git commit -m "Adiciona rota inicial de chat com IA"
git commit -m "Cria modelos iniciais do banco de dados"
git commit -m "Integra app mobile com endpoint de chat"
Branches Recomendadas
main
develop
feature/mobile-interface
feature/backend-api
feature/ai-integration
feature/database
feature/voice
feature/avatar
Status do Projeto
Em desenvolvimento inicial.

Atualmente o projeto possui:

Estrutura inicial da IA;
Front-end mobile em Kotlin em desenvolvimento;
Planejamento da arquitetura geral;
Definição inicial do back-end;
Definição inicial do banco de dados.
Próximas Etapas
Finalizar a estrutura inicial do back-end;
Criar endpoints principais;
Criar conexão com banco de dados;
Integrar back-end com módulo de IA;
Integrar app Kotlin com API;
Salvar histórico de conversas;
Testar fluxo completo:
usuário pergunta;
app envia;
back-end processa;
IA responde;
resposta aparece no app;
Adicionar suporte a arquivos;
Adicionar suporte a voz;
Adicionar avatar.
Observações Técnicas

Este projeto deve ser mantido modular. O erro mais comum seria misturar a lógica de IA diretamente no aplicativo mobile. Isso deixaria o sistema inseguro, difícil de manter e exporia chaves de API.

A estrutura correta é:

Mobile conversa com Back-end.
Back-end conversa com IA.
Back-end conversa com Banco de Dados.
Mobile nunca acessa diretamente a chave da IA.

Essa separação deixa o projeto mais seguro, organizado e fácil de evoluir.

Licença

Projeto acadêmico desenvolvido para fins educacionais.


Minha crítica direta: o README sozinho não salva projeto bagunçado. O ponto mais importante é vocês **não deixarem a chave da IA dentro do app Kotlin**. Isso seria um erro grave, porque qualquer pessoa poderia extrair a chave do APK. A IA tem que ficar no back-end, e o app só conversa com a API de vocês.
