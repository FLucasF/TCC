# Desenho - Serviço de Cálculo de Resumo de Compra

## O que muda de caso para caso

1. **Cupons**: cada um tem uma forma diferente de calcular desconto
   - BEMVINDO10: 10% do subtotal
   - MENOS50: R$ 50 fixo (com condição de mínimo)
   - FRETEGRATIS: transforma em desconto igual ao frete
   - LEVE3PAGUE2: remove itens de graça (modifica subtotal)

2. **Modalidades de entrega**: cada uma tem fórmula de cálculo e limitações
   - ECONOMICA: R$ 12 + R$ 2/kg
   - EXPRESSA: R$ 25 + R$ 4,50/kg
   - RETIRADA_LOJA: grátis
   - MOTOBOY: R$ 18 fixo (limitado a 5 kg)

3. **Níveis de clube**: cada um oferece benefícios diferentes
   - BRONZE: nada
   - PRATA: 2% de crédito, frete normal
   - OURO: 5% de crédito, frete grátis, brinde se > R$ 500

4. **Formas de pagamento**: cada uma afeta o total diferentemente
   - PIX: 5% desconto
   - CARTAO: juros conforme parcelas (até 3x sem juros, depois com juros)
   - BOLETO: tarifa de R$ 3,49 (indisponível > R$ 1.000)

5. **Regiões**: diferentes percentuais de seguro
   - SUDESTE, SUL, CENTRO_OESTE: 1%, 1%, 1.5%
   - NORTE, NORDESTE: 2.5%, 2%

## O que é igual em todos os casos

- Ordem do cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento → total
- Arredondamento "meio para par" em cada etapa
- Validação ocorre primeiro (em ordem específica)
- Resposta sempre JSON com mesmos campos

## Estrutura escolhida

**Padrão Strategy para variabilidade:**
- `CupomStrategy`: interface com implementações para cada cupom
- `ModalidadeEntregaStrategy`: interface com implementações para cada modalidade
- `ClubeMemberStrategy`: interface com implementações para cada nível
- `FormaPagamentoStrategy`: interface com implementações para cada forma

**Entrada/Saída:**
- `CheckoutRequest`: DTO com dados da compra
- `CheckoutResponse`: DTO com resumo calculado

**Validação:**
- Uma classe com validadores em cadeia (fail-first)

**Serviço principal:**
- `CheckoutService`: orquestra cálculos usando as strategies

**Utilitários:**
- `MoneyRounder`: arredonda valores usando banker's rounding
- Conversor de JSON automático via Spring

Assim cada tipo de cupom, entrega, etc. fica num lugar só, e adicionar novos é só criar uma nova implementação da interface.
