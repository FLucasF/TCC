# Desenho - Serviço de Resumo de Compra

## O que varia de caso para caso

1. **Cálculo do frete**: cada modalidade (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY) tem sua fórmula
2. **Desconto do cupom**: cada cupom (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2) tem lógica diferente
3. **Ajuste de pagamento**: cada forma (PIX, CARTAO, BOLETO) muda o valor e pode ter parcelas
4. **Crédito do clube**: cada nível (BRONZE, PRATA, OURO) tem seu percentual e OURO não paga frete
5. **Brinde**: só OURO e apenas se produtos > 500

## O que é igual em todos os casos

- Sequência de cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento
- Arredondamento "meio para o par" em cada etapa
- Validações em ordem específica (pedido, nível, região, modalidade, cupom, forma pagamento, parcelas)
- Cálculo de seguro sempre sobre produtos, sem desconto

## Estrutura escolhida

- **Enums**: ModalidadeEntrega, Cupom, FormaPagamento, NivelClube, Regiao
- **Estratégias**: 
  - `EstrategiaEntrega` - abstrai cálculo de frete
  - `EstrategiaCupom` - abstrai cálculo de desconto
  - `EstrategiaFormaPagamento` - abstrai ajuste de pagamento
  - `EstrategiaClube` - abstrai benefícios do clube
- **Serviço orquestrador**: `ResumoCompraService` executa validações e cálculos em ordem
- **Controller**: `CheckoutController` expõe `/checkout/resumo`

Dessa forma, adicionar novas modalidades/cupons/formas é só criar uma nova estratégia, não mexer na lógica de cálculo.
