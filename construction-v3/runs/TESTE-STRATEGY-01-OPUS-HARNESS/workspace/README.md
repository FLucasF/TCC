# Resumo de compra (checkout)

Serviço que calcula o resumo da compra: `POST /checkout/resumo`.

Rodar os testes: `mvn verify`. Subir o serviço: `mvn spring-boot:run`.

## Onde fica cada coisa

A sequência do cálculo é sempre a mesma e mora em `CalculadoraResumo`:
produtos → cupom → frete → imposto → total do pedido → ajuste do pagamento.

O que muda de pedido para pedido mora dentro do caso escolhido, um lugar por caso:

| O que varia | Onde mora | Como entra uma opção nova |
|---|---|---|
| Entrega (custo, prazo, limitações) | `dominio/ModalidadeEntrega` | novo valor do enum com seu `custo`, seu prazo e, se tiver limite, seu `atende` |
| Cupons (condição e desconto) | `dominio/Cupom` | novo valor do enum com seu `desconto` e, se tiver condição, seu `aplicavel` |
| Níveis do clube (vantagens) | `dominio/NivelClube` | novo valor do enum com `credito`, `freteGratis` e `brinde` |
| Imposto por região | `dominio/Regiao` | novo valor do enum com a alíquota (a conta é a mesma para todas) |
| Pagamento (parcelas, ajuste, limites) | `dominio/FormaPagamento` | novo valor do enum com `permite`, `cobrar` e, se tiver limite, `disponivel` |

Nenhum desses casos é escolhido por sequência de condições: o código do site chega
como texto, é lido direto no enum correspondente (`dominio/Codigos`) e o próprio
caso responde pelas suas regras.

Dinheiro é sempre `BigDecimal` arredondado em centavos com "meio para o par"
(`dominio/Dinheiro`).

## Um ponto do enunciado que não fecha

Os exemplos 1 a 4 do anexo trazem o total final **sem somar o imposto da região**,
mesmo que a região seja obrigatória e nenhuma delas tenha alíquota zero. O exemplo 5
soma o imposto normalmente, como descrevem as regras ("Total do pedido = produtos −
desconto + frete + imposto").

A implementação segue as regras escritas e o exemplo 5: o imposto sempre entra no total.
Por isso os exemplos 1 a 4, refeitos com a região `SUDESTE`, fecham assim
(ver `CalculadoraResumoTest`):

| Exemplo | subtotal | cupom | frete | prazo | imposto | ajuste | total final | parcelas |
|---|---|---|---|---|---|---|---|---|
| 1 | 409,70 | 40,97 | 33,10 | 2 | 44,25 | −22,30 | 423,78 | 1× 423,78 |
| 2 | 409,70 | 0,00 | 15,60 | 7 | 49,16 | 33,56 | 508,02 | 6× 84,67 |
| 3 | 399,80 | 50,00 | 18,00 | 0 | 41,98 | 3,49 | 413,27 | 1× 413,27 |
| 4 | 299,10 | 39,80 | 0,00 | 1 | 31,12 | 0,00 | 290,42 | 3× 96,81 |

As partes que os exemplos 1 a 4 fixam e que não dependem do imposto (subtotal, cupom,
frete, prazo) conferem exatamente, e as contas de pagamento desses exemplos estão
testadas com os números originais do anexo em `FormaPagamentoTest`
(Pix sobre 401,83 → 381,74; boleto sobre 367,80 → 371,29; cartão 6× sobre 425,30 →
75,90; cartão 3× sobre 259,30 → 86,43).

Se a intenção era mesmo que esses quatro exemplos não tivessem imposto, é só avisar.
