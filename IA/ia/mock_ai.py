def responder_mock(pergunta: str) -> str:
    return f"""
IA Física - MODO SIMULADO

Pergunta recebida:
{pergunta}

Resposta simulada:

1. Dados do problema:
Exemplo: força = 40 N, distância = 10 m, tempo = 5 s.

2. Fórmula usada:
P = W / t

3. Trabalho:
W = F · d = 40 · 10 = 400 J

4. Potência:
P = 400 / 5 = 80 W

5. Resposta final:
80 W

Observação:
Essa resposta é simulada para testar o aplicativo sem consumir crédito da API.
"""