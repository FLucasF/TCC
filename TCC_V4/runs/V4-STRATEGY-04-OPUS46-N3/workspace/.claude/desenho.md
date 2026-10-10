# Desenho — Resumo do Checkout

## O que muda de caso para caso

| Dimensão | O que varia | Frequência de mudança |
|---|---|---|
| Modalidade de entrega | Cálculo do frete, prazo, regra de disponibilidade | Alta (quase toda semana) |
| Cupom | Cálculo do desconto, condição de aplicabilidade | Alta (marketing inventa promoção) |
| Nível do clube | % crédito, frete grátis, brinde | Média (novos níveis em estudo) |
| Forma de pagamento | Cálculo do ajuste, parcelas válidas, disponibilidade | Baixa |
| Região (seguro) | Só a taxa %; cálculo idêntico em todas | Não varia em estrutura |

## O que é igual em todos os casos

1. Fluxo de cálculo: subtotal → desconto → frete → seguro → total → ajuste pagamento.
2. Arredondamento: `HALF_EVEN`, escala 2, aplicado a cada etapa.
3. Seguro: taxa da região × subtotal (sem desconto/frete).
4. Crédito do clube: percentual × subtotal.
5. Ordem de validação dos erros (fixa, 1 a 10).

## Estrutura escolhida

- **Interface + implementações separadas** para entrega, cupom, clube e pagamento.
  Cada caso é uma classe própria. Um `Map<String, Interface>` no service resolve o
  caso pelo código, sem cadeia de `if/else`.
- **Região/seguro**: apenas um `Map<String, BigDecimal>` com as taxas — não há
  variação de comportamento, só de valor.
- **ResumoService** orquestra o fluxo fixo, delega cada passo para a estratégia
  correspondente.
