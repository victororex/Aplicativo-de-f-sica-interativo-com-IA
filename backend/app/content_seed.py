SUBJECTS = [
    {
        "id": "mecanica",
        "name": "Mecanica",
        "description": "Movimento, forcas, energia, quantidade de movimento e gravitacao.",
        "exam_focus": "ENEM, FUVEST, UNICAMP e vestibulares tradicionais.",
        "sort_order": 1,
        "lessons": [
            {
                "id": "cinematica-escalar",
                "title": "Cinematica escalar",
                "description": "Posicao, deslocamento, velocidade media, aceleracao e graficos.",
                "content": "A cinematica descreve o movimento sem estudar suas causas. Em vestibulares, aparecem calculos de velocidade media, movimento uniforme, movimento uniformemente variado e interpretacao de graficos s x t e v x t.\n\nPontos-chave:\n- Velocidade media: vm = deslocamento / intervalo de tempo.\n- No MRU, a velocidade e constante e o grafico s x t e uma reta.\n- No MUV, a aceleracao e constante: v = v0 + a.t e s = s0 + v0.t + a.t^2/2.\n- A area no grafico v x t representa deslocamento.\n\nDica de vestibular: antes de substituir valores, confira unidades. Km/h deve virar m/s dividindo por 3,6.",
                "exam_tags": "ENEM: graficos; FUVEST: MUV; UNICAMP: interpretacao fisica",
                "sort_order": 1,
            },
            {
                "id": "leis-de-newton",
                "title": "Leis de Newton",
                "description": "Inercia, principio fundamental da dinamica e acao/reacao.",
                "content": "As Leis de Newton explicam como forcas alteram o movimento. A primeira lei trata da inercia: sem forca resultante, o corpo permanece em repouso ou MRU. A segunda lei diz que a forca resultante e F = m.a. A terceira lei afirma que forcas de acao e reacao aparecem em corpos diferentes.\n\nEm problemas, desenhe o diagrama de forcas: peso, normal, tracao, atrito e forcas aplicadas. Depois escolha eixos e aplique a segunda lei.\n\nDica de vestibular: normal nao e sempre igual ao peso. Em plano inclinado, elevador ou movimento vertical, ela pode mudar.",
                "exam_tags": "ENEM: situacoes cotidianas; FUVEST: sistemas de blocos; UNICAMP: plano inclinado",
                "sort_order": 2,
            },
            {
                "id": "trabalho-energia-potencia",
                "title": "Trabalho, energia e potencia",
                "description": "Energia cinetica, potencial, conservacao e rendimento.",
                "content": "Trabalho mede transferencia de energia por uma forca: W = F.d.cos(theta). Energia cinetica e Ec = m.v^2/2. Energia potencial gravitacional e Ep = m.g.h. Quando nao ha dissipacao, a energia mecanica se conserva.\n\nPotencia mede rapidez de transferencia de energia: P = W/t. Em maquinas, rendimento compara energia util e energia fornecida.\n\nDica de vestibular: em montanhas-russas, quedas e lancamentos, conservacao de energia costuma ser mais rapida que cinemática.",
                "exam_tags": "ENEM: rendimento; FUVEST: conservacao; UNICAMP: energia em sistemas",
                "sort_order": 3,
            },
            {
                "id": "impulso-quantidade-movimento",
                "title": "Impulso e quantidade de movimento",
                "description": "Colisoes, conservacao do momento e variacao de velocidade.",
                "content": "Quantidade de movimento e p = m.v. Impulso e I = F.delta_t e tambem representa a variacao da quantidade de movimento. Em sistemas isolados, a quantidade de movimento total se conserva.\n\nColisoes podem ser elasticas, parcialmente inelasticas ou perfeitamente inelasticas. No vestibular, muitas questoes cobram conservacao do momento em batidas, explosoes e recuos.\n\nDica de vestibular: conserve momento como vetor. Defina um sentido positivo antes das contas.",
                "exam_tags": "ENEM: seguranca veicular; FUVEST: colisoes; UNICAMP: explosoes",
                "sort_order": 4,
            },
        ],
    },
    {
        "id": "termologia",
        "name": "Termologia",
        "description": "Calor, temperatura, dilatacao, gases e leis da termodinamica.",
        "exam_focus": "Muito frequente no ENEM por energia, clima, maquinas termicas e cotidiano.",
        "sort_order": 2,
        "lessons": [
            {
                "id": "calorimetria",
                "title": "Calorimetria",
                "description": "Calor sensivel, calor latente e equilibrio termico.",
                "content": "Calor e energia em transito por diferenca de temperatura. Calor sensivel altera temperatura: Q = m.c.deltaT. Calor latente muda estado fisico: Q = m.L. Em equilibrio termico, a soma dos calores trocados e zero.\n\nDica de vestibular: identifique se ha mudanca de estado. Se houver, use calor latente no trecho correto.",
                "exam_tags": "ENEM: aquecimento e mudanca de estado; FUVEST: equilibrio termico",
                "sort_order": 1,
            },
            {
                "id": "gases-termodinamica",
                "title": "Gases e termodinamica",
                "description": "Transformacoes gasosas, trabalho e maquinas termicas.",
                "content": "Para gases ideais, vale p.V = n.R.T. Transformacoes comuns: isotermica, isobarica, isovolumetrica e adiabatica. A primeira lei da termodinamica relaciona calor, trabalho e energia interna: deltaU = Q - W.\n\nMaquinas termicas convertem parte do calor em trabalho, sempre com perdas. Rendimento nunca chega a 100%.\n\nDica de vestibular: em grafico p x V, a area representa trabalho.",
                "exam_tags": "ENEM: maquinas termicas; UNICAMP: graficos p x V",
                "sort_order": 2,
            },
        ],
    },
    {
        "id": "ondulatoria",
        "name": "Ondulatoria",
        "description": "Ondas, som, frequencia, comprimento de onda e fenomenos ondulatorios.",
        "exam_focus": "ENEM cobra som, instrumentos, efeito Doppler e aplicacoes tecnologicas.",
        "sort_order": 3,
        "lessons": [
            {
                "id": "ondas-periodicas",
                "title": "Ondas periodicas",
                "description": "Frequencia, periodo, velocidade e comprimento de onda.",
                "content": "Ondas transportam energia sem transporte liquido de materia. A relacao fundamental e v = lambda.f. Frequencia e o numero de oscilacoes por segundo; periodo e T = 1/f.\n\nFenomenos importantes: reflexao, refracao, difracao, interferencia e polarizacao.\n\nDica de vestibular: se a onda muda de meio, a frequencia permanece constante; velocidade e comprimento de onda podem mudar.",
                "exam_tags": "ENEM: ondas sonoras e eletromagneticas; FUVEST: interferencia",
                "sort_order": 1,
            },
            {
                "id": "acustica",
                "title": "Acustica",
                "description": "Som, altura, intensidade, timbre, eco e ressonancia.",
                "content": "Som e uma onda mecanica longitudinal. Altura depende da frequencia; intensidade depende da energia transportada; timbre diferencia fontes sonoras. Eco e reflexao perceptivel do som. Ressonancia ocorre quando um sistema vibra com grande amplitude ao receber energia em sua frequencia natural.\n\nDica de vestibular: som nao se propaga no vacuo, pois precisa de meio material.",
                "exam_tags": "ENEM: audicao e tecnologia; vestibulares: tubos sonoros e cordas",
                "sort_order": 2,
            },
        ],
    },
    {
        "id": "optica",
        "name": "Optica",
        "description": "Luz, espelhos, lentes, visao, instrumentos opticos e cores.",
        "exam_focus": "Temas frequentes: formacao de imagens, defeitos da visao e refracao.",
        "sort_order": 4,
        "lessons": [
            {
                "id": "espelhos-lentes",
                "title": "Espelhos e lentes",
                "description": "Formacao de imagens, foco, aumento e aplicacoes.",
                "content": "Em espelhos e lentes, raios notaveis ajudam a formar imagens. Espelhos planos formam imagens virtuais, direitas e de mesmo tamanho. Espelhos esfericos e lentes convergentes/divergentes podem formar imagens reais ou virtuais dependendo da posicao do objeto.\n\nDica de vestibular: desenhe os raios principais antes de usar formulas.",
                "exam_tags": "ENEM: instrumentos opticos; FUVEST: construcao de imagens",
                "sort_order": 1,
            },
            {
                "id": "refracao-visao",
                "title": "Refracao e visao",
                "description": "Indice de refracao, Snell, miopia e hipermetropia.",
                "content": "Refracao e a mudanca de direcao da luz ao passar entre meios. O indice de refracao mede quanto a luz reduz sua velocidade em um meio. A lei de Snell relaciona angulos e indices.\n\nMiopia e corrigida com lente divergente; hipermetropia com lente convergente.\n\nDica de vestibular: ao entrar em meio mais refringente, a luz se aproxima da normal.",
                "exam_tags": "ENEM: lentes corretivas; UNICAMP: refracao",
                "sort_order": 2,
            },
        ],
    },
    {
        "id": "eletromagnetismo",
        "name": "Eletromagnetismo",
        "description": "Eletrostatica, circuitos, magnetismo e indução eletromagnetica.",
        "exam_focus": "Circuitos eletricos e consumo de energia sao muito cobrados.",
        "sort_order": 5,
        "lessons": [
            {
                "id": "circuitos-eletricos",
                "title": "Circuitos eletricos",
                "description": "Corrente, tensao, resistencia, potencia e associacoes.",
                "content": "Corrente eletrica e fluxo ordenado de cargas. A primeira lei de Ohm e U = R.i. Potencia eletrica pode ser P = U.i, P = R.i^2 ou P = U^2/R.\n\nResistores em serie somam resistencias; em paralelo, a resistencia equivalente diminui.\n\nDica de vestibular: em consumo de energia, use kWh quando o problema fala de conta de luz.",
                "exam_tags": "ENEM: consumo eletrico; FUVEST: associacao de resistores",
                "sort_order": 1,
            },
            {
                "id": "campo-eletrico-magnetico",
                "title": "Campos eletrico e magnetico",
                "description": "Forcas em cargas, linhas de campo, imas e motores.",
                "content": "Campo eletrico descreve a influencia de cargas no espaco. Campo magnetico aparece em imas e correntes eletricas. Cargas em movimento podem sofrer forca magnetica.\n\nMotores e geradores usam interacao entre eletricidade e magnetismo. Inducao eletromagnetica explica geracao de corrente por variacao de fluxo magnetico.\n\nDica de vestibular: campo magnetico nao realiza trabalho sobre carga isolada quando a forca e perpendicular a velocidade.",
                "exam_tags": "ENEM: motores e geradores; UNICAMP: inducao",
                "sort_order": 2,
            },
        ],
    },
    {
        "id": "fisica-moderna",
        "name": "Fisica moderna",
        "description": "Radioatividade, efeito fotoeletrico, modelos atomicos e relatividade introdutoria.",
        "exam_focus": "ENEM cobra leitura conceitual, tecnologia, radiacao e seguranca.",
        "sort_order": 6,
        "lessons": [
            {
                "id": "radioatividade",
                "title": "Radioatividade",
                "description": "Emissoes alfa, beta, gama, meia-vida e aplicacoes.",
                "content": "Radioatividade e emissao espontanea de particulas ou radiacao por nucleos instaveis. Alfa tem baixo poder de penetracao; beta e intermediaria; gama tem alto poder de penetracao. Meia-vida e o tempo para metade dos nucleos radioativos decair.\n\nAplicacoes incluem medicina nuclear, datacao e industria, sempre com controle de dose.\n\nDica de vestibular: meia-vida reduz a quantidade pela metade a cada intervalo igual.",
                "exam_tags": "ENEM: medicina e seguranca; vestibulares: decaimento",
                "sort_order": 1,
            },
            {
                "id": "efeito-fotoeletrico",
                "title": "Efeito fotoeletrico",
                "description": "Quantizacao da luz e emissao de eletrons por metais.",
                "content": "No efeito fotoeletrico, luz de frequencia suficiente arranca eletrons de um metal. A energia de cada foton e E = h.f. A intensidade aumenta o numero de fotons, mas nao substitui frequencia insuficiente.\n\nEsse fenomeno ajudou a consolidar a ideia de quantizacao da luz.\n\nDica de vestibular: abaixo da frequencia de corte, nao ha emissao, mesmo aumentando a intensidade.",
                "exam_tags": "ENEM: paineis e sensores; FUVEST: quantizacao",
                "sort_order": 2,
            },
        ],
    },
]


DAILY_QUESTIONS = [
    {
        "id": "q-newton-si",
        "question": "Qual e a unidade de forca no Sistema Internacional?",
        "options": ["Newton", "Joule", "Watt", "Pascal"],
        "correct_index": 0,
        "explanation": "Forca e medida em newtons (N). Joule mede energia, watt mede potencia e pascal mede pressao.",
        "subject_id": "mecanica",
        "sort_order": 1,
    },
    {
        "id": "q-inercia",
        "question": "A primeira lei de Newton esta ligada principalmente a qual conceito?",
        "options": ["Inercia", "Acao e reacao", "Potencia", "Calor latente"],
        "correct_index": 0,
        "explanation": "A primeira lei afirma que um corpo tende a manter seu estado de repouso ou movimento retilineo uniforme.",
        "subject_id": "mecanica",
        "sort_order": 2,
    },
    {
        "id": "q-luz-vacuo",
        "question": "Qual e o valor aproximado da velocidade da luz no vacuo?",
        "options": ["300.000 km/s", "150.000 km/s", "1.000 km/s", "3.000 km/s"],
        "correct_index": 0,
        "explanation": "A velocidade da luz no vacuo e aproximadamente 3,0 x 10^8 m/s, ou 300.000 km/s.",
        "subject_id": "optica",
        "sort_order": 3,
    },
    {
        "id": "q-ohm",
        "question": "Qual expressao representa a primeira lei de Ohm?",
        "options": ["U = R.i", "P = m.g", "Q = m.c.deltaT", "v = lambda.f"],
        "correct_index": 0,
        "explanation": "A primeira lei de Ohm relaciona tensao, resistencia e corrente: U = R.i.",
        "subject_id": "eletromagnetismo",
        "sort_order": 4,
    },
    {
        "id": "q-onda",
        "question": "Em uma onda periodica, qual grandeza permanece constante ao mudar de meio?",
        "options": ["Frequencia", "Velocidade", "Comprimento de onda", "Amplitude sempre dobra"],
        "correct_index": 0,
        "explanation": "A frequencia e determinada pela fonte. Ao mudar de meio, velocidade e comprimento de onda podem variar.",
        "subject_id": "ondulatoria",
        "sort_order": 5,
    },
]


CAMPAIGN_NODES = [
    {
        "id": "camp-introducao",
        "title": "Introducao e grandezas",
        "description": "Revisao de unidades, notacao cientifica e leitura de graficos.",
        "subject_id": "mecanica",
        "sort_order": 1,
    },
    {
        "id": "camp-mecanica",
        "title": "Mecanica de vestibular",
        "description": "Cinematica, leis de Newton, energia e quantidade de movimento.",
        "subject_id": "mecanica",
        "sort_order": 2,
    },
    {
        "id": "camp-termologia",
        "title": "Termologia e energia",
        "description": "Calorimetria, gases, maquinas termicas e rendimento.",
        "subject_id": "termologia",
        "sort_order": 3,
    },
    {
        "id": "camp-ondas-optica",
        "title": "Ondas e optica",
        "description": "Som, frequencia, lentes, espelhos e defeitos da visao.",
        "subject_id": "ondulatoria",
        "sort_order": 4,
    },
    {
        "id": "camp-eletricidade",
        "title": "Eletricidade",
        "description": "Circuitos, consumo eletrico, potencia e seguranca.",
        "subject_id": "eletromagnetismo",
        "sort_order": 5,
    },
    {
        "id": "camp-moderna",
        "title": "Fisica moderna",
        "description": "Radioatividade, efeito fotoeletrico e tecnologias atuais.",
        "subject_id": "fisica-moderna",
        "sort_order": 6,
    },
]


AVATAR_ITEMS = [
    {"id": "glasses-none", "category": "Oculos", "name": "Sem oculos", "sort_order": 1},
    {"id": "glasses-round", "category": "Oculos", "name": "Oculos redondo", "sort_order": 2},
    {"id": "glasses-lab", "category": "Oculos", "name": "Oculos de laboratorio", "sort_order": 3},
    {"id": "hat-none", "category": "Chapeu", "name": "Sem chapeu", "sort_order": 1},
    {"id": "hat-cap", "category": "Chapeu", "name": "Bone de estudos", "sort_order": 2},
    {"id": "hat-grad", "category": "Chapeu", "name": "Capelo", "sort_order": 3},
    {"id": "clothes-hoodie", "category": "Roupas", "name": "Moletom", "sort_order": 1},
    {"id": "clothes-lab", "category": "Roupas", "name": "Jaleco", "sort_order": 2},
    {"id": "clothes-school", "category": "Roupas", "name": "Uniforme", "sort_order": 3},
    {"id": "shape-classic", "category": "Formato", "name": "Classico", "sort_order": 1},
    {"id": "shape-rounded", "category": "Formato", "name": "Arredondado", "sort_order": 2},
    {"id": "color-blue", "category": "Cores", "name": "Azul", "sort_order": 1},
    {"id": "color-green", "category": "Cores", "name": "Verde", "sort_order": 2},
    {"id": "color-purple", "category": "Cores", "name": "Roxo", "sort_order": 3},
]


RICH_LESSON_CONTENT = {
    "cinematica-escalar": """A cinematica descreve o movimento sem estudar suas causas. Em vestibulares, ela costuma aparecer em situacoes de percurso, velocidade media, movimento uniforme, movimento uniformemente variado e leitura de graficos.

Ideia central:
Movimento e sempre medido em relacao a um referencial. Um carro pode estar parado em relacao ao motorista e em movimento em relacao a uma pessoa na calcada.

Formulas essenciais:
- Velocidade media: vm = delta_s / delta_t
- Movimento uniforme: s = s0 + v.t
- Movimento uniformemente variado: v = v0 + a.t
- Posicao no MUV: s = s0 + v0.t + (a.t^2)/2
- Torricelli: v^2 = v0^2 + 2.a.delta_s

Exemplo resolvido:
Um ciclista sai do repouso e acelera a 2 m/s^2 durante 5 s.
1. Dados: v0 = 0, a = 2 m/s^2, t = 5 s.
2. Velocidade final: v = v0 + a.t = 0 + 2.5 = 10 m/s.
3. Deslocamento: delta_s = v0.t + a.t^2/2 = 0 + 2.25/2 = 25 m.
Resposta: ele percorre 25 m e chega a 10 m/s.

Leitura visual:
No grafico v x t, a area abaixo da curva e o deslocamento. Se a velocidade cresce de 0 a 10 m/s em 5 s, a area e um triangulo: base 5 e altura 10, entao delta_s = 5.10/2 = 25 m.

Dica de vestibular:
Antes de usar formula, veja se o problema pede distancia percorrida ou deslocamento. Em trajetorias com ida e volta, esses valores podem ser diferentes.""",
    "leis-de-newton": """As Leis de Newton explicam por que os corpos mudam ou mantem seu movimento. O segredo em vestibular e desenhar as forcas antes de fazer conta.

1a Lei - Inercia:
Se a forca resultante for zero, o corpo permanece em repouso ou em movimento retilineo uniforme.

2a Lei - Principio fundamental:
Fresultante = m.a. A aceleracao tem a mesma direcao da forca resultante.

3a Lei - Acao e reacao:
Forcas aparecem em pares, com mesma intensidade e sentidos opostos, mas atuam em corpos diferentes.

Exemplo resolvido:
Um bloco de 4 kg recebe uma forca horizontal de 20 N em uma superficie sem atrito.
1. Dados: m = 4 kg, F = 20 N.
2. Aplicando F = m.a: 20 = 4.a.
3. a = 5 m/s^2.
Resposta: o bloco acelera a 5 m/s^2.

Diagrama de forcas:
Em uma mesa horizontal sem atrito: peso aponta para baixo, normal aponta para cima e a forca aplicada aponta na horizontal. Como peso e normal se equilibram, a aceleracao vem da forca horizontal.

Dica de vestibular:
Normal nao e sempre igual ao peso. Em plano inclinado, elevador ou movimento vertical, ela muda.""",
    "trabalho-energia-potencia": """Energia e uma das ferramentas mais rapidas para resolver problemas de vestibular, principalmente quando existe altura, velocidade, mola ou atrito.

Trabalho:
W = F.d.cos(theta). Trabalho positivo aumenta energia do corpo; trabalho negativo retira energia.

Energia mecanica:
- Cinetica: Ec = m.v^2/2
- Potencial gravitacional: Ep = m.g.h
- Potencial elastica: Ee = k.x^2/2

Conservacao:
Se nao houver forcas dissipativas, Em inicial = Em final.

Exemplo resolvido:
Um corpo de 2 kg cai de uma altura de 5 m, desprezando o ar. Use g = 10 m/s^2.
1. Energia potencial inicial: Ep = m.g.h = 2.10.5 = 100 J.
2. No ponto mais baixo, essa energia vira cinetica: Ec = 100 J.
3. m.v^2/2 = 100 -> 2.v^2/2 = 100 -> v^2 = 100 -> v = 10 m/s.

Visual:
No topo, a barra de energia e quase toda potencial. Durante a queda, a parte potencial diminui e a cinetica aumenta. A soma permanece constante quando nao ha dissipacao.

Dica de vestibular:
Quando o caminho parece complicado, tente energia. Ela depende de estados inicial/final, nao de cada pedaco da trajetoria.""",
    "impulso-quantidade-movimento": """Quantidade de movimento mede o movimento carregado por um corpo: p = m.v. Ela e vetorial, entao o sentido importa.

Impulso:
I = F.delta_t = delta_p. Quanto maior o tempo de colisao, menor pode ser a forca media para a mesma variacao de movimento.

Conservacao:
Em um sistema isolado, a quantidade de movimento total antes e igual a quantidade de movimento total depois.

Exemplo resolvido:
Um carrinho de 2 kg a 3 m/s colide e gruda em outro de 1 kg parado.
1. Momento inicial: p = 2.3 + 1.0 = 6 kg.m/s.
2. Massa final grudada: 3 kg.
3. Conservando: 6 = 3.v -> v = 2 m/s.
Resposta: os carrinhos seguem juntos a 2 m/s.

Visual:
Antes: um carrinho rapido empurra outro parado. Depois: os dois viram um unico conjunto mais pesado e mais lento.

Dica de vestibular:
Em colisoes perfeitamente inelasticas, os corpos saem juntos. Energia mecanica nao se conserva totalmente, mas momento se conserva se o sistema estiver isolado.""",
    "calorimetria": """Calorimetria estuda trocas de energia termica. Temperatura mede agitacao media das particulas; calor e energia em transito.

Calor sensivel:
Q = m.c.deltaT. Usado quando a substancia muda de temperatura.

Calor latente:
Q = m.L. Usado quando muda de estado fisico sem mudar temperatura.

Equilibrio termico:
Em um sistema isolado, calor cedido + calor recebido = 0.

Exemplo resolvido:
100 g de agua recebem 2000 cal. Considere c = 1 cal/g.C.
1. Q = m.c.deltaT
2. 2000 = 100.1.deltaT
3. deltaT = 20 C
Resposta: a agua aquece 20 C.

Visual:
Em uma curva de aquecimento, trechos inclinados indicam aumento de temperatura; patamares horizontais indicam mudanca de estado.

Dica de vestibular:
Se aparecer gelo derretendo, vapor condensando ou agua fervendo, procure calor latente antes de usar Q = m.c.deltaT.""",
    "gases-termodinamica": """Gases e termodinamica conectam pressao, volume, temperatura, calor e trabalho.

Equacao dos gases ideais:
p.V = n.R.T. A temperatura deve estar em kelvin.

Transformacoes comuns:
- Isotermica: temperatura constante.
- Isobarica: pressao constante.
- Isovolumetrica: volume constante.
- Adiabatica: sem troca de calor.

Primeira lei:
deltaU = Q - W. Se o gas realiza trabalho, ele gasta parte da energia recebida.

Exemplo resolvido:
Um gas recebe 500 J de calor e realiza 200 J de trabalho.
deltaU = Q - W = 500 - 200 = 300 J.
Resposta: a energia interna aumenta 300 J.

Visual:
No grafico p x V, a area sob a curva representa o trabalho. Em expansao, o gas realiza trabalho positivo.

Dica de vestibular:
Sempre confira se a temperatura esta em Celsius ou kelvin. Para gases, use T(K) = T(C) + 273.""",
    "ondas-periodicas": """Ondas transportam energia sem transporte liquido de materia. Uma pessoa em uma arquibancada pode fazer uma onda passar sem sair do lugar.

Relacao fundamental:
v = lambda.f
lambda e o comprimento de onda, f e a frequencia e v e a velocidade de propagacao.

Periodo:
T = 1/f. Se a frequencia aumenta, o periodo diminui.

Fenomenos:
- Reflexao: onda volta ao encontrar obstaculo.
- Refracao: onda muda de velocidade ao mudar de meio.
- Difracao: onda contorna obstaculos.
- Interferencia: ondas se somam ou se anulam.

Exemplo resolvido:
Uma onda tem frequencia 20 Hz e comprimento 0,5 m.
v = lambda.f = 0,5.20 = 10 m/s.

Visual:
Cristas sao pontos altos da onda; vales sao pontos baixos. A distancia entre duas cristas consecutivas e lambda.

Dica de vestibular:
Quando a onda muda de meio, a frequencia nao muda. Mudam velocidade e comprimento de onda.""",
    "acustica": """Som e uma onda mecanica longitudinal, portanto precisa de meio material para se propagar.

Qualidades do som:
- Altura: ligada a frequencia. Som agudo tem maior frequencia.
- Intensidade: ligada a energia. Som forte tem maior intensidade.
- Timbre: permite distinguir dois instrumentos tocando a mesma nota.

Eco e reverberacao:
Eco ocorre quando o som refletido chega separado do som original. Reverberacao e uma permanencia mais curta do som no ambiente.

Exemplo resolvido:
Um som demora 2 s para ir ate uma parede e voltar. Use v = 340 m/s.
1. Distancia total percorrida: dtotal = v.t = 340.2 = 680 m.
2. Distancia ate a parede: d = 680/2 = 340 m.

Visual:
Imagine setas saindo da fonte sonora, batendo em uma parede e voltando ao ouvinte. O tempo medido inclui ida e volta.

Dica de vestibular:
Som nao se propaga no vacuo. Ondas eletromagneticas, como luz, propagam-se no vacuo.""",
    "espelhos-lentes": """Optica geometrica usa raios de luz para prever imagens em espelhos e lentes.

Espelho plano:
Imagem virtual, direita, mesmo tamanho e mesma distancia atras do espelho que o objeto esta na frente.

Espelhos esfericos:
Concavo pode formar imagem real ou virtual. Convexo forma imagem virtual, direita e menor.

Lentes:
Convergente junta raios paralelos no foco. Divergente espalha raios como se viessem do foco.

Exemplo resolvido:
Um objeto esta a 30 cm de uma lente convergente de foco 10 cm.
Equacao: 1/f = 1/p + 1/p'
1/10 = 1/30 + 1/p'
1/p' = 1/10 - 1/30 = 2/30 = 1/15
p' = 15 cm.

Visual:
Trace um raio paralelo ao eixo principal que, apos a lente convergente, passa pelo foco. Trace outro passando pelo centro optico, sem desviar. O cruzamento indica a imagem.

Dica de vestibular:
Desenho de raios notaveis evita erro de sinal e ajuda a saber se a imagem e real ou virtual.""",
    "refracao-visao": """Refracao e a mudanca de direcao e velocidade da luz ao passar de um meio para outro.

Indice de refracao:
n = c/v. Quanto maior n, menor a velocidade da luz no meio.

Lei de Snell:
n1.sen(theta1) = n2.sen(theta2)

Visao:
Miopia: imagem se forma antes da retina. Corrige-se com lente divergente.
Hipermetropia: imagem se formaria depois da retina. Corrige-se com lente convergente.

Exemplo resolvido:
Se a luz entra do ar para a agua, ela passa para um meio mais refringente. A velocidade diminui e o raio se aproxima da normal.

Visual:
Desenhe uma linha normal perpendicular a superficie. Ao entrar em meio de maior indice, o raio dobra em direcao a essa normal.

Dica de vestibular:
Frequencia da luz nao muda na refracao. Velocidade e comprimento de onda mudam.""",
    "circuitos-eletricos": """Circuitos aparecem muito no ENEM por consumo de energia, chuveiro, lampadas e conta de luz.

Grandezas:
- Corrente: fluxo de cargas, medida em ampere.
- Tensao: energia por carga, medida em volt.
- Resistencia: oposicao a corrente, medida em ohm.

Lei de Ohm:
U = R.i

Potencia:
P = U.i = R.i^2 = U^2/R

Associacoes:
Em serie, a corrente e a mesma e as resistencias somam.
Em paralelo, a tensao e a mesma e a resistencia equivalente diminui.

Exemplo resolvido:
Um aparelho de 220 V funciona com corrente de 5 A.
P = U.i = 220.5 = 1100 W.
Se funcionar por 2 h: E = 1,1 kW . 2 h = 2,2 kWh.

Visual:
Serie parece uma unica estrada para a corrente. Paralelo parece bifurcacoes: a corrente se divide e depois se junta.

Dica de vestibular:
Para conta de luz, converta watts para quilowatts: 1000 W = 1 kW.""",
    "campo-eletrico-magnetico": """Campos descrevem influencias no espaco. Uma carga cria campo eletrico; correntes e imas criam campo magnetico.

Campo eletrico:
E = F/q. Linhas saem de cargas positivas e entram em cargas negativas.

Forca eletrica:
Cargas de mesmo sinal se repelem; sinais opostos se atraem.

Campo magnetico:
Atua sobre cargas em movimento e fios com corrente. Motores eletricos usam forca magnetica para gerar rotacao.

Inducao:
Variacao de fluxo magnetico pode gerar corrente eletrica. Esse principio aparece em geradores.

Exemplo visual:
Ao aproximar um ima de uma espira, o fluxo magnetico pela espira muda. Essa variacao induz corrente.

Dica de vestibular:
Campo magnetico nao muda o modulo da velocidade de uma carga quando atua perpendicularmente; ele muda a direcao do movimento.""",
    "radioatividade": """Radioatividade e emissao espontanea de radiacao por nucleos instaveis.

Tipos:
- Alfa: particula pesada, baixo alcance, barrada por papel.
- Beta: alcance intermediario, barrada por aluminio fino.
- Gama: onda eletromagnetica de alta energia, exige blindagem mais densa.

Meia-vida:
Tempo necessario para a quantidade de material radioativo cair pela metade.

Exemplo resolvido:
Uma amostra tem 80 g e meia-vida de 10 anos. Depois de 30 anos:
30 anos = 3 meias-vidas.
80 -> 40 -> 20 -> 10 g.
Resposta: restam 10 g.

Visual:
Imagine uma barra que metade se apaga a cada intervalo de meia-vida. A queda e exponencial, nao linear.

Dica de vestibular:
Nao confunda irradiacao com contaminacao. Um objeto irradiado recebeu radiacao; contaminado possui material radioativo nele.""",
    "efeito-fotoeletrico": """O efeito fotoeletrico mostra que a luz tambem se comporta como pacotes de energia, chamados fotons.

Energia do foton:
E = h.f
Quanto maior a frequencia, maior a energia de cada foton.

Frequencia de corte:
Abaixo de certa frequencia, nenhum eletron e arrancado, mesmo aumentando a intensidade.

Intensidade:
Se a frequencia ja e suficiente, aumentar a intensidade aumenta o numero de eletrons emitidos, nao a energia maxima de cada um.

Exemplo conceitual:
Luz vermelha muito intensa pode nao arrancar eletrons de um metal. Luz violeta menos intensa pode arrancar, porque tem frequencia maior.

Visual:
Pense em fotons como bolinhas de energia atingindo uma placa metalica. Cada eletron precisa receber uma energia minima para escapar.

Dica de vestibular:
Esse tema e conceitual: a palavra-chave costuma ser frequencia, nao brilho.""",
}


for subject in SUBJECTS:
    for lesson in subject["lessons"]:
        lesson["content"] = RICH_LESSON_CONTENT.get(lesson["id"], lesson["content"])
