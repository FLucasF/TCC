# Desenho do Serviço de Resumo de Compra

## O que muda de caso para caso

- **Modalidades de entrega**: cada uma calcula o frete de forma diferente, tem prazo próprio e restrições específicas (ex: motoboy só até 5kg)
- **Cupons**: cada cupom tem regra de desconto própria e condições de aplicação (ex: LEVE3PAGUE2 conta unidades, FRETEGRATIS depende do frete calculado)
- **Formas de pagamento**: cada uma aplica ajustes diferentes (desconto no PIX, tarifa no boleto, juros no cartão) e tem restrições (boleto não aceita > R$ 1.000)
- **Níveis do clube**: cada nível oferece benefícios diferentes (crédito, frete grátis, brinde)
- **Regiões**: cada região tem taxa de seguro diferente

## O que é igual

- A ordem de cálculo: produtos → frete → cupom → seguro → total → ajuste pagamento
  (cupom vem depois do frete porque FRETEGRATIS precisa do valor do frete calculado)
- As validações na ordem especificada
- O arredondamento HALF_EVEN em cada etapa
- A estrutura da resposta JSON

## Estrutura escolhida

**Polimorfismo para os comportamentos que variam:**
- Interface `ModalidadeEntrega` com implementações para cada opção (Economica, Expressa, RetiradaLoja, Motoboy)
- Interface `Cupom` com implementações para cada cupom (BemVindo10, Menos50, FreteGratis, Leve3Pague2)
- Interface `FormaPagamento` com implementações para cada forma (Pix, Boleto, Cartao)
- Enum `NivelClube` com os benefícios de cada nível
- Enum `Regiao` com a taxa de seguro de cada região

**Orquestração:**
- `CalculadoraResumo`: executa os passos na ordem correta, delegando para os objetos polimórficos
- Controller REST recebe o JSON, valida, orquestra o cálculo e devolve a resposta

**Por quê:**
- Cada comportamento específico mora num lugar só dele
- Escolher entre casos não é sequência de if/else, é polimorfismo
- Adicionar nova modalidade/cupom/forma de pagamento = criar nova classe, sem mexer nas existentes
- A assinatura das interfaces atende o caso mais exigente (ex: cupom FRETEGRATIS precisa do valor do frete)
