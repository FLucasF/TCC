# Desenho

## O que muda de caso para caso (cada caso numa classe só dele)
- **Entrega** (novas toda semana): disponibilidade, frete, prazo. Interface `Entrega`, uma classe por opção.
- **Cupom** (marketing cria): aplicabilidade e desconto. Interface `Cupom`, uma classe por cupom.
- **Nível do clube** (virão mais): crédito, ajuste do frete, brinde. Interface `NivelClube`, uma classe por nível.
- **Forma de pagamento**: aceita quantas parcelas, disponibilidade, cobrança final. Interface `FormaPagamento`, uma classe por forma.

Assinaturas olhando o caso mais exigente: entrega e cupom recebem o `Pedido` inteiro (peso, itens, subtotal);
cupom recebe também o frete (FRETEGRATIS); pagamento recebe o total e as parcelas e devolve total final + parcela.

## Região
Só muda a porcentagem, a conta é a mesma: um `enum Regiao` com a porcentagem. Sem estratégia.

## Igual em todos os casos
`ResumoService` é o pipeline fixo: valida na ordem dos erros, subtotal → frete (nível ajusta) → cupom → seguro →
total → pagamento. Escolha do caso por mapa código→implementação (beans Spring coletados por lista), sem if/switch em cadeia.
Dinheiro: arredondamento meio-para-o-par a cada etapa (`Dinheiro`).
Erros: `PedidoRecusadoException(codigo)` → HTTP 422 `{ "erro": codigo }`.
