# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

**Itens do carrinho**: preços, quantidades e pesos variam.

**Modalidade de entrega**: 4 opções (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY), cada uma com:
- Fórmula de cálculo do frete diferente
- Prazo diferente
- Restrições diferentes (ex: motoboy até 5 kg)

**Cupom**: 5 tipos (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2, ou ausente), cada um com:
- Forma de desconto diferente (%, valor fixo, frete grátis, unidade grátis)
- Pré-condição diferente (ex: MENOS50 só acima de R$ 300)

**Forma de pagamento**: 3 opções (PIX, CARTAO, BOLETO), cada uma com:
- Ajuste diferente (desconto, taxa, juros)
- Regra de parcelamento diferente
- Restrições diferentes (boleto acima de R$ 1.000)

**Nível do clube**: 3 níveis (BRONZE, PRATA, OURO), cada um com:
- Crédito na próxima compra (%, nenhum, ou %)
- Benefício de frete (nenhum ou grátis)
- Brinde para produtos altos (sim/não)

**Região**: 5 opções (SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE), cada uma com taxa de seguro diferente.

## O que é igual em todos os casos

A **sequência de cálculo** é sempre a mesma:
1. Soma dos produtos (preço × quantidade)
2. Aplicar cupom
3. Calcular frete
4. Calcular seguro (sobre subtotal, sem desconto, sem frete)
5. Calcular crédito do clube (sobre subtotal, sem desconto)
6. Aplicar ajuste de pagamento
7. Calcular parcelas

Cada valor é **arredondado para centavos** com o método "meio para o par" após cada etapa.

**Validações** seguem ordem fixa, retornando o primeiro problema encontrado.

## Estrutura escolhida

**Enums para o que não muda**: ModalidadeEntrega, FormaPagamento, NivelClube, Regiao — cada um encapsula suas fórmulas e constantes.

**Cupom**: interface `Cupom` com implementações (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2), pois o cálculo varia muito e há pré-condições.

**CalculadorResumo**: serviço que segue a sequência fixa de cálculo:
1. Subtotal dos produtos
2. Calcular frete (com descontos do clube)
3. Aplicar cupom (no desconto de produtos, ou no frete)
4. Calcular seguro
5. Calcular crédito do clube
6. Calcular ajuste de pagamento (PIX: -5%, BOLETO: +3.49, CARTAO: juros ou 0)
7. Calcular parcelas

**ValidadorPedido**: @Service que valida na ordem especificada (10 situações), retorna o primeiro erro. Chamado antes de processar a compra.

**Arredondador**: método estático para arredondar "meio para o par" (HALF_EVEN), reutilizado em todo o código.

**DTOs**: PedidoRequest (entrada), ResumoResponse (saída sucesso), ErroResponse (saída erro).

**AjusteParcelas**: record privada que encapsula o resultado do cálculo de parcelas (ajuste, total, valorParcela).

Isso permite adicionar novas modalidades, cupons, formas de pagamento e níveis de clube sem mexer na lógica central.
