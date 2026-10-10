# Desenho: Serviço de Cálculo de Resumo de Compra

## O que muda de caso para caso

1. **Cupons**: cada cupom tem sua lógica de desconto (%, valor fixo, LEVE3PAGUE2, ou frete grátis)
2. **Modalidades de entrega**: cada uma tem fórmula de cálculo (base + taxa por kg), prazo, e limitações
3. **Formas de pagamento**: cada uma tem seu ajuste (desconto PIX, tarifa boleto, juros cartão)
4. **Níveis de clube**: cada um tem vantagens diferentes (desconto, frete, brinde)
5. **Regiões**: cada uma tem sua taxa de seguro

## O que é igual em todos os casos

- Validações (ordem fixa, sempre checam o mesmo)
- Sequência de cálculo (subtotal → cupom → frete → seguro → ajuste pagamento → total)
- Arredondamento (sempre "meio para o par")
- Crédito do clube (sempre sobre subtotal, sem desconto/frete)

## Estrutura escolhida

- **Cupom, Entrega, Pagamento, Clube**: estratégia (um objeto por tipo, implementa sua lógica)
- **Validador**: centraliza todas as checks em ordem, retorna o primeiro erro
- **Calculadora**: orquestra o fluxo de cálculo, usa os objetos específicos
- **Enums**: para modalidades, formas, níveis, regiões, cupons
- **DTOs**: para request/response, sem banco de dados

## Por quê

- Separação clara: cada tipo de variação mora em um lugar
- Fácil adicionar novo cupom/entrega/pagamento sem mexer no resto
- Validação em um lugar facilita manutenção e testes

## Correções após revisão

1. **Leve3Pague2**: Agregação corrigida para evitar dupla contagem quando item aparece múltiplas vezes
2. **CalculadoraDesconto**: Removido método `aplicarLeve3Pague2()` que não era usado
3. **Subtotal**: Extraído para método estático reutilizável, eliminando duplicação
4. **Testes**: Expandidos para cobrir FRETEGRATIS, brinde, e pagamentos com diferentes formas
