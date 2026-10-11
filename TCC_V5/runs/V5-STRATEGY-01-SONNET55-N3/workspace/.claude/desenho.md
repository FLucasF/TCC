# Desenho

## O que muda de caso para caso (cada caso = uma classe só dele)
- Entrega (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY; entram novas toda semana): atende o carrinho?, frete, prazo.
- Cupom (4 hoje, marketing inventa mais): é aplicável ao carrinho?, desconto (recebe carrinho e frete, pois FRETEGRATIS usa o frete).
- Nível do clube (BRONZE/PRATA/OURO, virão mais): `Vantagens` (crédito, frete grátis, brinde) a partir do subtotal.
- Forma de pagamento (PIX/CARTAO/BOLETO): aceita nº de parcelas?, aceita o total?, cobrança (total final e parcela).

Assinaturas pensadas no caso mais exigente: entrega e cupom recebem o `Carrinho` (itens, subtotal, peso);
cupom também o frete; clube devolve um conjunto de vantagens; pagamento devolve `Cobranca` (final + parcela).

## O que é igual em todos
- Ordem do cálculo, arredondamento meio-para-o-par a cada etapa, ordem de validação/erros: `ServicoResumo`.
- Seguro: a conta é a mesma, só muda a taxa -> enum `Regiao` com a taxa (sem estratégia).

## Escolha entre casos
Cada interface tem `codigo()`; as implementações são `@Component` e um `Catalogo<T>` busca por código
(sem if/switch). Novo caso = nova classe, sem mexer no serviço.
Código desconhecido/ausente vira o erro correspondente. Erros: `PedidoRecusadoException(CodigoErro)` -> `{ "erro": "..." }` (HTTP 422).
