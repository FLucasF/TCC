# Desenho: serviço de resumo de checkout

## O que muda de caso para caso

- **Modalidade de entrega**: fórmula de custo, prazo, restrição (ex.: peso máximo).
- **Cupom**: lógica de desconto e condição de aplicabilidade.
- **Nível do clube**: isenção de frete, taxa de crédito, limiar de brinde.
- **Forma de pagamento**: ajuste sobre o total, restrição de parcelas, restrição de valor.

## O que é igual em todos os casos

- Ordem de cálculo: subtotal → desconto cupom → frete → seguro → total → ajuste pagamento.
- Arredondamento HALF_EVEN (banker's), escala 2, em cada etapa.
- Sequência de validação (10 erros, em ordem, retorna o primeiro).
- Seguro: taxa da região × subtotal bruto (sem desconto, sem frete).
- Crédito do clube: taxa × subtotal bruto.

## Estrutura escolhida

Quatro interfaces de estratégia, cada uma com um registro (Map por código):

- `ModalidadeEntregaStrategy` — `atende(peso)`, `calcularFrete(peso)`, `prazoEntregaDias()`.
- `CupomStrategy` — `aplicavel(subtotal)`, `calcularDesconto(subtotal, frete, itens)`.
- `NivelClubeStrategy` — `isentoFrete()`, `calcularCredito(subtotal)`, `contemBrinde(subtotal)`.
- `FormaPagamentoStrategy` — `aceitaParcelas(n)`, `disponivelParaTotal(total)`, `calcular(total, n)`.

Cada implementação é um `@Component`; o registro é injetado com `List<Strategy>` e indexado por código.

`Regiao` é um enum com a taxa de seguro: só a taxa muda, a fórmula é única.

A validação e o cálculo ficam em `CheckoutService.calcular()`, em sequência explícita, sem ramificações sobre os tipos concretos.
