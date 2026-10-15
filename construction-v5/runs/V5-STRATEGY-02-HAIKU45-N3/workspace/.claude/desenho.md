# Desenho - Serviço de Cálculo de Resumo da Compra

## O que muda de caso para caso

1. **Cupons**: cada cupom tem sua própria lógica de desconto (% dos produtos, valor fixo, frete grátis, LEVE3PAGUE2)
2. **Modalidades de entrega**: cada uma tem fórmula de custo e prazo diferentes, e restrições (ex.: motoboy até 5kg)
3. **Formas de pagamento**: PIX tem desconto, boleto tem taxa, cartão tem juros por parcelamento
4. **Níveis de clube**: cada um oferece percentual de crédito diferente e vantagens específicas (OURO não paga frete, manda brinde)
5. **Regiões**: cada uma tem percentual de seguro diferente

## O que é igual em todos os casos

- Fluxo de cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento → total
- Arredondamento sempre "meio para o par" (HALF_EVEN) em cada etapa
- Validações ocorrem na mesma ordem para todos os pedidos
- Estrutura de resposta com todos os campos sempre presentes

## Estrutura escolhida

**Padrão Strategy**: cada "caso que muda" é uma classe isolada. Estratégias são criadas uma única vez e reutilizadas durante o cálculo.

**Interfaces de estratégia**:
- `Cupom`: `calcularDesconto(subtotal, itens)`, `ehAplicavel(subtotal)`, `ofereceFretGratis()`
- `Entrega`: `calcularCusto(pesoKg)`, `prazo()`, `podeAtender(pesoKg)`
- `FormaPagamento`: `calcularAjuste(total, parcelas)`, `calcularValorParcela(total, parcelas)`, `parcelaPermitida(parcelas)`, `podeAtender(total)`
- `NivelClube`: `calcularCredito(subtotal)`, `temFreteGratis()`, `temBrinde(subtotal)`
- `Regiao` (enum): `getPercentualSeguro()`

**CheckoutService**: orquestrador central que
- Instancia cada estratégia uma única vez no início
- Executa cálculos na ordem correta
- Delega lógica específica às estratégias (sem hardcodes)
- Valida entrada conforme especificação

**Factories**: `CupomFactory`, `EntregaFactory`, `FormaPagamentoFactory`, `NivelClubeFactory` para criar instâncias

**Por quê**: Elimina duplicação, permite adicionar novos cupons/fretes/formas de pagamento sem tocar no orquestrador. Cada estratégia é responsável por sua própria validação (ex: `podeAtender()`, `ehAplicavel()`). Fácil de testar, expandir e manter.
