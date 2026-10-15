# Desenho do serviço de resumo de compra

## O que muda de caso para caso

- **Modalidades de entrega**: cada uma calcula custo e prazo de forma diferente, algumas têm restrições
- **Cupons**: cada um tem sua regra de desconto e condições de aplicação
- **Formas de pagamento**: cada uma tem seu ajuste (desconto, tarifa, juros)
- **Níveis do clube**: cada nível concede benefícios diferentes
- **Regiões**: cada uma define a porcentagem do seguro

## O que é igual em todos os casos

- A ordem de cálculo: produtos → frete → cupom (que pode usar o frete) → seguro → total → ajuste pagamento
- Arredondamento para centavos usando HALF_EVEN em cada etapa
- Validações e códigos de erro
- Estrutura do JSON de entrada/saída

## Estrutura escolhida

Cada comportamento que varia mora em sua própria classe, identificada por enum:

- `ModalidadeEntrega` (enum) → cada valor conhece seu cálculo, prazo e validação
- `Cupom` (enum) → cada valor conhece seu desconto e condição de aplicação
- `FormaPagamento` (enum) → cada valor conhece seu ajuste
- `NivelClube` (enum) → cada valor conhece seus benefícios
- `Regiao` (enum) → cada valor conhece sua porcentagem de seguro

Um `CalculadoraResumo` orquestra o fluxo de cálculo, delegando para cada enum a responsabilidade de calcular sua parte.

## Por que assim

- Adicionar nova modalidade/cupom/forma de pagamento = adicionar novo valor no enum, sem mexer em condições
- Cada caso mora em um só lugar
- O fluxo de cálculo fica claro e sequencial no orquestrador
- Atende apenas o que o enunciado descreve como variando, sem criar estrutura para variação futura
