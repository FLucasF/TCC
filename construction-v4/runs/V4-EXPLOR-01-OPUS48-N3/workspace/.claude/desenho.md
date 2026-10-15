# Desenho — serviço de resumo de checkout

## O que é igual em todo pedido
- A sequência do cálculo: subtotal → desconto do cupom → frete → seguro →
  total do pedido → ajuste do pagamento.
- O arredondamento: sempre meio-para-o-par (HALF_EVEN), 2 casas, em cada etapa.
- A conta do seguro: percentual × subtotal (sem desconto, sem frete). Só a
  porcentagem muda por região → **não** é polimorfismo, é um dado (enum com
  campo percentual, mesma fórmula).
- A conta do crédito do clube: percentual × subtotal.
- A ordem de validação dos erros (1 a 10), devolvendo o primeiro problema.

## O que muda de caso para caso (cada caso num lugar só, sem cadeia de ifs)
Quatro famílias. Cada família é um enum cujos membros têm comportamento próprio
(método abstrato ou sobrescrito por membro). A escolha é um lookup por código,
nunca uma sequência de condições. Enunciado diz que surgem casos novos sempre
(entregas, cupons, níveis), então adicionar = acrescentar um membro.

- **ModalidadeEntrega**: frete(peso), prazoDias, disponivel(peso).
  (MOTOBOY sobrescreve disponivel: só até 5 kg.)
- **Cupom**: aplicavel(ctx), desconto(ctx). FRETEGRATIS usa o frete; por isso o
  frete é calculado antes de resolver o desconto do cupom, embora apareça depois
  na ordem do resumo (frete não depende de cupom).
- **NivelClube**: credito(subtotal), aplicaFrete(freteBase) (OURO zera),
  brinde(subtotal) (OURO acima de 500).
- **FormaPagamento**: parcelasValidas(n), disponivel(total),
  calcular(total, n) → (valorFinal, valorParcela). CARTAO concentra Price/sem juros.

## Assinatura pensada pelo caso mais exigente
- Frete do cupom (FRETEGRATIS) e seguro/crédito (base = subtotal sem frete/desconto)
  exigem que o contexto do cupom carregue subtotal, frete e itens.
- Pagamento devolve par (valorFinal, valorParcela) porque CARTAO com juros tem
  valorFinal = parcela × n (≠ total), enquanto sem juros valorFinal = total.

## Estrutura
- `web`: Controller `/checkout/resumo`, DTOs (campos de enum como String para
  controlar o código de erro na ordem certa), handler que transforma
  `CheckoutException(codigo)` em `{ "erro": CODIGO }`.
- `domain`: `CalculadoraResumo` (orquestra ordem e validações), `Dinheiro`
  (arredondamento), os quatro enums + `Regiao`.

## Erros (ordem)
itens → nível → região → modalidade(existe) → modalidade(disponível) →
cupom(existe) → cupom(aplicável) → pagamento(existe) → parcelas → pagamento(disponível).
