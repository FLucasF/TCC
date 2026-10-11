# Desenho - Serviço de Resumo de Compra

## O que varia de caso para caso

1. **Modalidades de entrega**: cada uma tem sua fórmula de cálculo de custo e prazo
2. **Cupons**: cada um tem sua regra de desconto (% fixo, valor fixo, grátis frete, desconto em unidades)
3. **Formas de pagamento**: cada uma tem seu ajuste (desconto no Pix, taxa no boleto, juros no cartão)
4. **Níveis de clube**: cada um tem seus benefícios (crédito%, frete grátis, brinde)
5. **Regiões**: cada uma tem sua taxa de seguro

## O que é igual em todos os casos

- A ordem de cálculo é sempre a mesma
- Todas as operações monetárias usam arredondamento "meio para o par"
- As validações seguem a mesma sequência
- A estrutura de resposta é a mesma

## Estrutura escolhida

**Strategy pattern** para cada variação:
- `EntregaStrategy`: calcula frete e prazo
- `CupomStrategy`: calcula desconto
- `PagamentoStrategy`: calcula ajuste (desconto, juros ou taxa)
- `ClubStrategy`: calcula crédito e brinde

Cada estratégia encapsula a lógica específica de seu tipo, evitando cascata de `if-else`.

**Calculator** orquestra o cálculo na ordem correta e aplica arredondamentos.

**Controller** valida entrada, chama o calculator e retorna resposta ou erro.

## Por quê

- Novo cupom? Nova classe que implementa `CupomStrategy`.
- Nova modalidade de entrega? Nova classe que implementa `EntregaStrategy`.
- A lógica geral não muda, só adiciona novos casos.
- Cada classe tem uma responsabilidade clara: calcular sua parte.
