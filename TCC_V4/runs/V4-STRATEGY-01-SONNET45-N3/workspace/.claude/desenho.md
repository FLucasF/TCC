# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

1. **Cupons**: cada cupom tem sua lógica de desconto e suas condições de aplicação
   - BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2

2. **Modalidades de entrega**: cada uma com fórmula de cálculo, prazo e limitações diferentes
   - ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY

3. **Formas de pagamento**: cada uma com seu ajuste (desconto, tarifa ou juros) e regras de parcelamento
   - PIX, BOLETO, CARTAO

4. **Níveis do clube**: cada um com suas vantagens específicas
   - BRONZE, PRATA, OURO

5. **Regiões**: cada uma com porcentagem de seguro diferente
   - SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE

## O que é igual em todos os casos

- A ordem de cálculo: produtos → frete bruto → cupom → frete final (com clube) → seguro → total → ajuste pagamento
  - O frete bruto é calculado antes do cupom para permitir que o cupom FRETEGRATIS use esse valor
  - O frete final considera o benefício de frete grátis do clube OURO
- O arredondamento (meio para o par) aplicado em cada etapa
- A validação dos dados na ordem especificada
- O cálculo do subtotal (soma de preço × quantidade)
- O cálculo do peso total (soma de peso × quantidade)
- A base de cálculo do seguro (sempre sobre produtos sem desconto e sem frete)
- A base de cálculo do crédito (sempre sobre produtos sem desconto e sem frete)

## Estrutura escolhida

Para cada tipo de variação, usar uma interface com implementações específicas:

- `Cupom` (interface) → `BemVindo10`, `Menos50`, `FreteGratis`, `Leve3Pague2`
  - Cada cupom sabe verificar se pode ser aplicado e calcular seu desconto
- `ModalidadeEntrega` (interface) → `Economica`, `Expressa`, `RetiradaLoja`, `Motoboy`
  - Cada modalidade sabe se está disponível para o pedido e calcular frete + prazo
- `FormaPagamento` (interface) → `Pix`, `Boleto`, `Cartao`
  - Cada forma sabe se está disponível, validar parcelas e calcular ajuste
- `NivelClube` (interface) → `Bronze`, `Prata`, `Ouro`
  - Cada nível sabe calcular crédito, brinde e se tem frete grátis
- `Regiao` (enum) → apenas armazena a porcentagem do seguro, não precisa de polimorfismo

Um serviço orquestrador (`CalculadoraResumo`) executa a sequência de cálculos, delegando cada etapa para a implementação correta.

## Por quê

- Cada comportamento específico fica isolado na sua classe, facilitando entender e mudar
- Adicionar novo cupom/modalidade/forma de pagamento = criar uma classe nova, sem mexer nas outras
- A sequência de validação e cálculo fica explícita no orquestrador
- Não há sequências de if/switch escolhendo entre os casos (cada classe implementa seus próprios métodos)
- As validações específicas (ex: parcelamento) ficam em quem conhece as regras, não no orquestrador
