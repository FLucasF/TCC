# Desenho: resumo da compra

## O que muda de caso para caso
- Modalidade de entrega: cobrança do frete, prazo, limite de disponibilidade. Cada transportadora tem seu jeito. -> constante de `ModalidadeEntrega` com seus métodos.
- Cupom: condição para valer e cálculo do desconto. -> constante de `Cupom`.
- Forma de pagamento: quais parcelamentos aceita, limite de disponibilidade, cálculo do total final. -> constante de `FormaPagamento`.
- Nível do clube: % de crédito, se o frete é zerado, regra do brinde. -> constante de `NivelClube`.
- Região: só a porcentagem do seguro muda. -> campo em `Regiao`.

## O que é igual em todos os casos
- Ordem do cálculo e arredondamento (HALF_EVEN, centavos a cada etapa).
- Subtotal e peso a partir dos itens.
- Conta do seguro e do crédito (porcentagem × subtotal).
- Busca do código pelo nome da constante e a ordem de validação dos erros.
- Montagem da resposta e do erro `{ "erro": "CODIGO" }`.

## Estrutura
- `ResumoService`: valida na ordem da tabela de erros e compõe o resumo. Não tem `if` por caso: cada enum resolve a própria regra.
- Assinatura dos casos mais exigentes:
  - Cupom recebe `Pedido` (itens, subtotal, frete), porque FRETEGRATIS precisa do frete e LEVE3PAGUE2 precisa dos itens.
  - Forma de pagamento recebe (total do pedido, parcelas) e devolve `Pagamento` (total final, parcelas, valor da parcela).
- `ResumoException` carrega o código; o controller o converte em HTTP 400.

## Premissas
- LEVE3PAGUE2 só é aplicável se algum item tiver 3 ou mais unidades; senão, `CUPOM_NAO_APLICAVEL`.
- Cupom nulo ou em branco equivale a nenhum cupom.
- Parcelas nula equivale a 1.
- Brinde: produtos acima de R$ 500,00 (estritamente), só para OURO.
- Frete zerado do OURO vale para qualquer modalidade, mas a disponibilidade da modalidade continua sendo checada.
