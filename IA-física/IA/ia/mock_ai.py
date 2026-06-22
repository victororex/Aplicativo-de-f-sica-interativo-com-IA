def responder_mock(pergunta: str) -> str:
    return """
# Análise Dimensional

## Explicação

A **Análise Dimensional** verifica se uma fórmula é coerente comparando as dimensões físicas dos dois lados.

## Pontos principais

1. Identifique as grandezas da pergunta.
2. Troque cada grandeza por `[M]`, `[L]` e `[T]`.
3. Simplifique as potências.
4. Compare os dois lados da igualdade.

$$
v = d / t
[v] = [L] / [T] = [L][T]^-1
$$

## Exemplo

Na velocidade, a distância tem dimensão `[L]` e o tempo tem dimensão `[T]`.

## Resumo

Se as dimensões não combinam, a fórmula precisa ser revista antes dos cálculos.
"""
