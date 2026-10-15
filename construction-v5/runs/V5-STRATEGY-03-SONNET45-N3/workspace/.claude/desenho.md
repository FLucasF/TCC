# Desenho: Calculadora de Resumo de Compra

## O que muda de caso para caso

- **Modalidade de entrega**: cada uma calcula frete diferente, tem prazo próprio e limitações específicas
- **Cupom**: cada cupom tem regra de desconto e condição de aplicação distintas
- **Nível do clube**: cada nível oferece vantagens diferentes (crédito, frete grátis, brinde)
- **Forma de pagamento**: cada uma aplica ajuste próprio (desconto Pix, tarifa boleto, juros cartão)
- **Região**: apenas a porcentagem de seguro muda

## O que é igual em todos os casos

- Ordem do cálculo: produtos → cupom → frete → seguro → total → ajuste pagamento
- Arredondamento: sempre para centavos com RoundingMode.HALF_EVEN
- Estrutura de validação: ordem fixa dos erros
- Contratos de entrada/saída JSON

## Estrutura escolhida

**Strategy Pattern** para cada dimensão de variação:
- `ModalidadeEntrega` (interface) → uma classe para cada modalidade
- `Cupom` (interface) → uma classe para cada cupom
- `NivelClube` (interface) → uma classe para cada nível
- `FormaPagamento` (interface) → uma classe para cada forma

**Por quê:**
- Cada comportamento mora em um lugar só
- Escolher entre casos = lookup direto, não sequência de ifs
- Fácil adicionar novas modalidades/cupons/níveis sem mexer no existente

**Fluxo:**
1. Validar e calcular dados básicos (executa validações 1-9 e calcula uma vez)
   - Validar pedido básico (itens não vazios, valores válidos)
   - Validar nível do clube
   - Validar região
   - Calcular subtotal dos produtos (uma única vez)
   - Validar modalidade de entrega
   - Calcular frete (pode ser zerado pelo clube OURO)
   - Validar e calcular cupom (se houver)
   - Calcular seguro (% da região sobre produtos)
   - Calcular total do pedido
   - Validar forma de pagamento e parcelamento
2. Validar se forma de pagamento aceita o total (validação 10)
3. Aplicar ajuste da forma de pagamento
4. Calcular crédito e verificar brinde
5. Montar resposta

**Otimizações aplicadas:**
- Subtotal calculado uma única vez e reutilizado
- Lógica de cálculo não duplicada entre validação e execução
- Métodos de validação separados para facilitar manutenção
