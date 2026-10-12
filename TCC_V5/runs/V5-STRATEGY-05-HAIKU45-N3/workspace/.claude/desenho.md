# Desenho: Serviço de Checkout

## O que muda de caso para caso

- **Modalidade de entrega**: cada uma tem fórmula de cálculo, prazo e restrições diferentes
- **Cupom**: cada um aplica desconto de forma diferente (% valor, valor fixo, frete grátis, desconto por unidade)
- **Forma de pagamento**: cada uma tem ajuste diferente (desconto Pix, taxa boleto, juros cartão) e regras de parcelamento
- **Nível do clube**: cada nível tem benefícios diferentes (crédito %, frete grátis, brinde)
- **Região**: afeta apenas o percentual de seguro

## O que é igual em todos os casos

- Ordem de cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento → total
- Arredondamento "meio para o par" em cada etapa
- Validações em ordem fixa
- Estrutura JSON de entrada/saída

## Estrutura escolhida

**Strategy Pattern** para separar comportamentos variáveis:
- `EstrategiaEntrega`: implementações para cada modalidade
- `EstrategiaCupom`: implementações para cada cupom (incluindo "sem cupom")
- `EstrategiaPagamento`: implementações para cada forma de pagamento

**Serviço orquestrador**: `CheckoutService` que:
1. Valida entrada na ordem especificada, calculando componentes progressivamente
2. Calcula cada componente usando as estratégias
3. Aplica arredondamento conforme necessário
4. Monta a resposta

## Detalhes de implementação

- Validação elimina duplicação: reusa cálculos intermediários
- Arredondamento RoundingMode.HALF_EVEN para "meio para o par"
- Testes cobrem: 5 exemplos do financeiro + validações + casos especiais (brinde, frete OURO, quantidade zero)
- Parcela para cartão com > 3x: total final / parcelas (juros já embutidos no ajuste)

## Por quê

Strategy Pattern mantém cada caso de negócio num lugar só, sem sequências de `if/else`. Adicionar nova modalidade de entrega, cupom ou forma de pagamento é adicionar uma classe que implementa a interface, sem tocar no orquestrador. Validação sem duplicação reduz risco de inconsistência.
