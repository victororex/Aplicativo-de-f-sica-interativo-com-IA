def responder_mock(pergunta: str) -> str:
    return """
Resposta curta:
A Analise Dimensional ajuda a conferir se uma formula faz sentido comparando as dimensoes fisicas dos dois lados.

Passo a passo:
* Identifique as grandezas da pergunta.
* Troque cada grandeza por [M], [L] e [T].
* Simplifique as potencias.
* Compare os dois lados da igualdade.

Formula ou relacao dimensional:
v = d / t
[v] = [L] / [T] = [L][T]^-1

Exemplo simples:
Se a pergunta envolve velocidade, pense em distancia dividida por tempo. Distancia tem dimensao [L] e tempo tem dimensao [T].

Resumo final:
Quando as dimensoes nao combinam, a formula precisa ser revista antes dos calculos.
"""
