# Desenho — serviço de resumo da compra

## O que muda de caso para caso

Quatro "famílias" em que o comportamento muda conforme o caso e o enunciado
diz explicitamente que novos casos entram com frequência própria:

- **Entrega**: cada modalidade tem seu jeito de cobrar o frete, seu prazo e
  suas limitações (motoboy só até 5 kg).
- **Cupom**: cada cupom tem sua regra de desconto e sua condição de uso.
- **Nível do clube**: cada nível tem seu conjunto de vantagens (crédito,
  frete grátis, brinde).
- **Forma de pagamento**: cada forma ajusta o total do seu jeito (desconto,
  tarifa, juros), tem suas parcelas permitidas e suas limitações (boleto só
  até R$ 1.000).

Para cada família, o comportamento de cada caso mora num `enum` com métodos
próprios, e a escolha do caso é um lookup por código (`fromCodigo` →
`Optional`), nunca uma cadeia de `if`.

## O que é igual em todos os casos

- **Região (seguro)**: o enunciado diz que "só a porcentagem muda, a conta é
  a mesma". Então NÃO é família de comportamento: é um `enum` que carrega só
  a taxa, com uma única conta de seguro. (Não criar estrutura para variação
  que o enunciado não descreve.)
- **Arredendamento**: todo valor em dinheiro vira centavos com HALF_EVEN
  (meio para o par). Fica num só lugar: `Dinheiro.arredonda`.
- **Ordem do cálculo e ordem das validações**: a orquestração (subtotal →
  cupom/frete/seguro → total → ajuste) e a verificação dos erros na ordem
  fixa do enunciado moram em `CalculadoraResumo`. A ordem das validações é
  imposta pelo enunciado (primeiro erro encontrado), então é uma sequência
  deliberada — não é escolha de comportamento.

## Assinaturas (decididas olhando o caso mais exigente)

- `ModalidadeEntrega`: `frete(peso)`, `atende(peso)`, `prazoDias()`.
  (peso entra porque econômica/expressa cobram por kg e motoboy limita por kg.)
- `Cupom`: `aplicavel(pedido)`, `desconto(pedido, freteEfetivo)`.
  (freteEfetivo entra por causa do FRETEGRATIS, cujo desconto é igual ao frete.)
- `NivelClube`: `credito(subtotal)`, `freteGratis()`, `brinde(subtotal)`.
- `FormaPagamento`: `parcelasPermitidas(n)`, `atende(total)`,
  `calcular(total, n)` → `ResultadoPagamento(totalFinal, valorParcela)`.
- `Regiao`: `seguro(subtotal)` (uma conta, taxa por constante).

## Detalhes do fluxo

- Frete efetivo = `freteGratis ? 0 : modalidade.frete(peso)`. O OURO zera o
  frete; o FRETEGRATIS mostra o frete e põe o desconto igual a ele.
- Seguro e crédito usam o subtotal de produtos (sem desconto, sem frete).
- Total = subtotal − descontoCupom + frete + seguro.
- Ajuste = totalFinal − total (sai do `calcular` da forma de pagamento).
- Peso do pedido não arredonda; dinheiro arredonda em cada etapa.

## Erros

`CheckoutException(codigo)` lançada na ordem do enunciado; um handler devolve
`{ "erro": CODIGO }`.
