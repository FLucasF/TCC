# Desenho - Serviço de Cálculo de Resumo de Compra

## O que muda de caso para caso

1. **Modalidade de Entrega**: 4 opções (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY), cada uma com fórmula de cálculo e restrições diferentes
2. **Cupom**: 4 tipos com lógicas distintas (desconto %, valor fixo, frete grátis, quantidade de itens)
3. **Nível do Clube**: 3 níveis, cada um com suas vantagens (crédito %, frete grátis, brinde)
4. **Forma de Pagamento**: 3 formas, cada uma com ajuste diferente (desconto %, taxa, juros)
5. **Região**: 5 regiões, cada uma com percentual de seguro diferente

## O que é igual em todos os casos

1. Validação de entrada (carrinho, nível, região, modalidade, cupom, forma de pagamento)
2. Cálculo do subtotal (∑ preço × quantidade)
3. Aplicação de cupom (desconto ou sem desconto)
4. Cálculo de frete (aplicável ou grátis)
5. Cálculo de seguro (% sobre subtotal)
6. Cálculo de ajuste de pagamento (sobre total anterior)
7. Cálculo de crédito do clube (% sobre subtotal)
8. Arredondamento banker's rounding (meio para o par) em cada etapa

## Estrutura escolhida

- **Controller**: recebe JSON, valida entrada básica, chama service
- **Service**: orquestra todo o cálculo, chama estratégias específicas para variaçõesl
- **Strategies**: cada variação (entrega, cupom, ajuste de pagamento) é uma strategy com interface própria
- **Enums**: REGIAO e NIVEL_CLUBE são enums com lógica associada (seguro e crédito)
- **DTOs**: CheckoutRequest (entrada) e CheckoutResponse (saída)
- **Validators**: validações organizadas em classes específicas (validarPedido, validarNivel, etc)

## Por quê

- Strategies isolam o cálculo variável: adicionar uma nova modalidade de entrega não toca o service
- Enums com lógica encapsulam a variação de seguro/crédito sem múltiplas condições
- Validators separam a lógica de validação, facilitam testes e mantêm o service limpo
- DTOs explicitam contrato com o cliente HTTP
