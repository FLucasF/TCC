# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (comportamento próprio → cada caso num lugar só)

- **Entrega** (`ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY`): cada uma tem
  sua fórmula de frete, seu prazo e sua regra de disponibilidade (motoboy ≤ 5 kg).
  O enunciado diz que entram opções novas quase toda semana → interface
  `ModalidadeEntrega`, uma classe por opção, registro por código.
- **Cupom** (`BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2`): cada um tem
  seu desconto e sua condição de aplicabilidade. Marketing inventa promoção
  direto → interface `Cupom`, uma classe por cupom, registro por código.
- **Nível do clube** (`BRONZE`, `PRATA`, `OURO`): cada nível tem seu conjunto de
  vantagens (crédito %, frete grátis, brinde). Vão criar mais níveis → interface
  `NivelClube`, uma classe por nível, registro por código.
- **Forma de pagamento** (`PIX`, `CARTAO`, `BOLETO`): cada uma tem seu ajuste
  (desconto Pix, juros Price no cartão, tarifa do boleto), suas parcelas
  permitidas e sua disponibilidade (boleto ≤ R$ 1.000) → interface
  `FormaPagamento`, uma classe por forma, registro por código.

Escolher o caso é sempre `registro.buscar(codigo)` (lookup em mapa), nunca uma
cadeia de `if`. Não existir no mapa = erro "...INVALIDA/INVALIDO"; existir mas não
atender = erro "...INDISPONIVEL/NAO_APLICAVEL".

## O que é igual em todos os casos (mora num lugar só)

- **Seguro por região**: o enunciado diz que *só a porcentagem muda, a conta é a
  mesma*. Logo NÃO é comportamento, é parâmetro → `enum Regiao` com o percentual,
  e a conta (percentual × subtotal, arredondado) mora no serviço. Idem a conta do
  crédito do clube (percentual × subtotal).
- **A ordem do cálculo** (subtotal → cupom → frete → seguro → total → pagamento) e
  a **ordem das validações** (10 verificações, devolve a primeira que falha) são
  iguais a todo pedido → ficam no serviço `CalculadoraResumo`, como sequência
  explícita. Aqui a sequência é a própria regra (validação ordenada), não escolha
  de comportamento.
- **Arredondamento** meio-para-o-par (HALF_EVEN) em cada etapa → `Dinheiro`.
- **Lookup por código** é igual para as 4 famílias → genérico `RegistroDeCodigos<T>`.

## Assinaturas (olhando o caso mais exigente)

- `ModalidadeEntrega`: `frete(peso)`, `atende(peso)`, `prazoDias()`, `codigo()`.
- `Cupom`: `aplicavel(ctx)`, `desconto(ctx)` — ctx traz subtotal (MENOS50/BEMVINDO10),
  frete (FRETEGRATIS) e itens (LEVE3PAGUE2).
- `NivelClube`: `credito(subtotal)`, `freteGratis()`, `brinde(subtotal)`, `codigo()`.
- `FormaPagamento`: `parcelasPermitidas(n)`, `atende(total)`,
  `calcula(total, n) -> ResultadoPagamento(valorFinal, valorParcela)`;
  o serviço deriva `ajuste = valorFinal − total`.

## Decisões de borda

- Frete exibido já considera OURO (frete grátis zera). FRETEGRATIS = frete exibido.
- Campos de enum chegam como String no DTO e são validados pelo serviço, para
  devolver o código de erro certo (ausente/ inexistente), não um 400 do Jackson.
- `parcelas` ausente = 1. `cupom` ausente/vazio = sem cupom.
- Juros do cartão: até 3x sem juros (valorFinal = total); de 4x a 12x tabela Price.
- Erro de negócio → HTTP 422 com corpo `{ "erro": "CODIGO" }`. Sucesso → 200.
