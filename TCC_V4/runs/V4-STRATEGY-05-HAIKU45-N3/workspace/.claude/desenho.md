# Desenho: Serviço de Cálculo de Resumo de Compra

## O que muda de caso para caso

1. **Cálculo de frete**: cada modalidade de entrega (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY) tem sua própria fórmula de custo e prazo.
2. **Desconto de cupom**: cada cupom (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2) aplica desconto de forma diferente.
3. **Benefícios de clube**: cada nível (BRONZE, PRATA, OURO) oferece crédito e vantagens distintas.
4. **Percentual de seguro**: cada região tem seu próprio percentual.
5. **Ajuste de pagamento**: cada forma de pagamento (PIX, CARTAO, BOLETO) tem seu próprio ajuste (desconto, taxa ou juros).

## O que é igual em todos os casos

- Fluxo de cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento → total
- Arredondamento "meio para o par" (ROUND_HALF_EVEN) em cada etapa, incluindo somas intermediárias
- Validações de entrada e suas prioridades

## Estrutura escolhida

- **Enums** para tipos fixos (TipoEntrega, Cupom, NivelClube, Regiao, FormaPagamento)
- **Strategy pattern** para comportamentos variáveis:
  - `EstrategiaFrete` para cada tipo de entrega
  - `EstrategiaCupom` para cada cupom
  - `EstrategiaAjustePagamento` para cada forma de pagamento
- **Validador centralizado** que executa validações na ordem de prioridade
- **Serviço orquestrador** que coordena o cálculo completo
- **Controller REST** que expõe o endpoint `/checkout/resumo`
- **BigDecimal** para todos os cálculos monetários, com arredondamento HALF_EVEN após cada operação

## Por que essa estrutura

- Cada variação de comportamento fica isolada em sua própria classe/estratégia, facilitando manutenção e futuros novos cupons/entregas/níveis.
- Validações em uma classe separada deixa clara a ordem de verificação.
- Strategy pattern permite escolher a estratégia correta sem cascata de condicionais.
- BigDecimal evita problemas de ponto flutuante em valores monetários.
- Arredondamento em cada etapa garante precisão conforme especificado (todos os 5 exemplos conferidos passam).
