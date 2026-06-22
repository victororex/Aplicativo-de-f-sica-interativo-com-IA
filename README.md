Visão Geral

O projeto consiste em um aplicativo mobile para apoio ao ensino de Física, combinando Inteligência Artificial, OCR, Aprendizado Adaptativo e Análise de Dados para criar uma experiência de estudo mais interativa e personalizada.

O principal diferencial do sistema é permitir que o aluno aprenda Física através de conversas com uma IA, envio de imagens de exercícios e acompanhamento do próprio progresso por meio de métricas e recomendações inteligentes.

Objetivo

Criar uma plataforma educacional capaz de:

Auxiliar no aprendizado de Física.
Explicar conceitos de forma interativa.
Resolver exercícios passo a passo.
Interpretar fórmulas através de imagens.
Adaptar o conteúdo ao desempenho do aluno.
Monitorar e analisar a evolução do aprendizado.
Tecnologias Utilizadas
Frontend (Android)
Kotlin
Jetpack Compose
Material Design 3
Navigation Compose
ViewModel
Coroutines
Backend
Python
FastAPI
OCR para fórmulas matemáticas
APIs de IA
Inteligência Artificial
Chat Inteligente
OCR de Fórmulas
Sistema Adaptativo
Lógica Fuzzy
Recomendações Personalizadas
Ciência de Dados
Analytics de Aprendizagem
Coleta de Métricas
Data Mining Educacional
Dashboard de Progresso
Principais Funcionalidades
Tutor IA

O aluno pode conversar com a IA para:

Tirar dúvidas.
Solicitar explicações.
Receber exemplos.
Resolver exercícios.
Obter resumos.
Caminho
Home
 → Tutor IA
OCR de Fórmulas

Permite enviar imagens contendo:

Fórmulas matemáticas.
Exercícios.
Expressões físicas.

O sistema identifica o conteúdo da imagem e encaminha para a IA gerar uma explicação ou resolução.

Tecnologias Relacionadas
Visão Computacional
OCR
Inteligência Artificial
Caminho
Tutor IA
 → Enviar Imagem
 → OCR
 → Resolução pela IA
Sistema Adaptativo

O aplicativo monitora o comportamento do aluno para personalizar a experiência de estudo.

Analisa:

Desempenho.
Frequência de estudo.
Progresso.
Conclusão de atividades.
Componentes
AdaptiveLearningEngine
FuzzyLearningEngine
Relacionado às disciplinas
Inteligência Artificial
Sistemas Inteligentes
Lógica Fuzzy
Sistemas Adaptativos
Ciência de Dados
Analytics Educacional

O sistema registra eventos de aprendizagem para gerar métricas sobre o progresso do aluno.

Exemplos:

Tempo de estudo.
Missões concluídas.
Uso da IA.
Exercícios realizados.
Evolução geral.
Arquivos Relacionados
AnalyticsModels.kt
AdaptiveLearningEngine.kt
FuzzyLearningEngine.kt
Data Mining Educacional

Os dados coletados são utilizados para identificar padrões de aprendizagem.

Objetivos:

Detectar dificuldades.
Identificar conteúdos dominados.
Gerar recomendações.
Apoiar o sistema adaptativo.
Relacionado às disciplinas
Mineração de Dados
Ciência de Dados
Machine Learning
Dashboard de Aprendizagem

Permite acompanhar:

Evolução do estudante.
Estatísticas de desempenho.
Histórico de progresso.
Indicadores de aprendizagem.
Telas Relacionadas
ProgressScreen
GeneralProgressScreen
ImprovementStatsScreen
ProfileScreen
Sistema de Missões

Implementa conceitos de gamificação para aumentar o engajamento.

Funcionalidades:

Missões diárias.
Campanhas de estudo.
Desafios.
Recompensas de progresso.
Caminho
Home
 → Missões
Estrutura do Projeto
app/
│
├── data/
│   ├── analytics/
│   ├── api/
│   ├── image/
│   ├── voice/
│   └── local/
│
├── model/
│
├── navigation/
│
├── ui/
│   └── screens/
│
├── viewmodel/
│
└── assets/
Funcionalidades de IA Identificadas
Chat Inteligente
ChatApiClient
ChatScreen
OCR
FormulaImageProcessor
formula_ocr_service.py
Sistema Adaptativo
AdaptiveLearningEngine.kt
Lógica Fuzzy
FuzzyLearningEngine.kt
Funcionalidades de Ciência de Dados Identificadas
Analytics
AnalyticsModels.kt
Coleta de Dados
Progresso do usuário
Missões
Interações
Desempenho
Data Mining Educacional
Análise de comportamento
Recomendações de estudo
Aprendizado adaptativo
Fluxo Geral do Sistema
Aluno
  ↓
Aplicativo
  ↓
Tutor IA
  ↓
Resposta Inteligente

ou

Aluno
  ↓
Imagem
  ↓
OCR
  ↓
IA
  ↓
Explicação
Disciplinas Aplicadas no Projeto
Inteligência Artificial
IA Conversacional
OCR
Visão Computacional
Lógica Fuzzy
Sistemas Adaptativos
Ciência de Dados
Analytics
Mineração de Dados
Visualização de Dados
Monitoramento de Aprendizagem
Conclusão

O projeto demonstra a integração entre conceitos de Inteligência Artificial e Ciência de Dados aplicados ao contexto educacional. A plataforma utiliza IA para auxiliar o aluno durante os estudos e emprega técnicas de análise de dados para acompanhar desempenho, identificar padrões de aprendizagem e oferecer uma experiência mais personalizada e eficiente.
