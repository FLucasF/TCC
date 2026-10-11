# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

1. **Modalidade de entrega**: cada modalidade tem sua fórmula de custo, prazo e regras de disponibilidade
   - ECONOMICA: 12,00 + 2,00/kg
   - EXPRESSA: 25,00 + 4,50/kg
   - RETIRADA_LOJA: grátis
   - MOTOBOY: 18,00 fixo, limitado a 5kg
   - *Novas modalidades entram quase toda semana*

2. **Cupom de desconto**: cada cupom tem sua lógica de cálculo e condição de aplicabilidade
   - BEMVINDO10: 10% sobre produtos
   - MENOS50: R$ 50 se produtos >= R$ 300
   - FRETEGRATIS: desconto = valor do frete
   - LEVE3PAGUE2: a cada 3 unidades do mesmo item, 1 grátis
   - *Marketing inventa promoções com frequência*

3. **Forma de pagamento**: cada forma aplica um ajuste diferente sobre o total
   - PIX: 5% de desconto
   - CARTAO: sem juros até 3x, com juros (1,99% a.m., Price) de 4x a 12x
   - BOLETO: tarifa de R$ 3,49, não aceita acima de R$ 1.000

4. **Nível do clube**: cada nível concede benefícios diferentes
   - BRONZE: nada
   - PRATA: 2% de crédito sobre produtos
   - OURO: 5% de crédito, frete grátis, brinde se produtos > R$ 500
   - *Estudando criar mais níveis*

5. **Região**: cada região tem uma porcentagem de seguro
   - Sudeste/Sul: 1%
   - Centro-Oeste: 1,5%
   - Norte: 2,5%
   - Nordeste: 2%

## O que é igual em todos os casos

- Ordem fixa de cálculo: produtos → frete → desconto cupom → seguro → total → ajuste pagamento
  - *Nota: na especificação está "desconto cupom → frete", mas FRETEGRATIS precisa do valor do frete para calcular o desconto, então invertemos essas duas etapas na implementação*
- Arredondamento banker's rounding (meio para par) em cada etapa
- Estrutura de validação em ordem específica (10 regras)
- Cálculo de subtotal (∑ preço × quantidade)
- Cálculo de peso total (∑ peso × quantidade)

## Estrutura escolhida

**Strategy pattern** para cada dimensão que varia:

1. `ModalidadeEntrega` (interface) ← cada modalidade implementa
   - `calcularFrete(pesoTotal): BigDecimal`
   - `obterPrazo(): int`
   - `estaDisponivel(pesoTotal): boolean`

2. `Cupom` (interface) ← cada cupom implementa
   - `calcularDesconto(contexto): BigDecimal`
   - `eAplicavel(contexto): boolean`

3. `FormaPagamento` (interface) ← cada forma implementa
   - `calcularAjuste(total, parcelas): AjustePagamento`
   - `validarParcelas(parcelas): boolean`
   - `estaDisponivel(total): boolean`

4. `NivelClube` (enum com comportamento) ← cada nível tem seus métodos
   - `calcularCredito(subtotalProdutos): BigDecimal`
   - `isentarFrete(): boolean`
   - `concederBrinde(subtotalProdutos): boolean`

5. `Regiao` (enum simples) ← só armazena a porcentagem
   - `percentualSeguro: BigDecimal`

**Por quê:**
- Cada caso vive em seu próprio lugar (não há if/else encadeados)
- Fácil adicionar novas modalidades, cupons, formas de pagamento ou níveis sem mexer no fluxo principal
- O orquestrador (serviço de cálculo) só conhece as interfaces, não os casos concretos
- Cada estratégia é testável isoladamente
