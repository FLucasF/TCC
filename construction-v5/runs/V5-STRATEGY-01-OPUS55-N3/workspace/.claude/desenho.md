# Desenho — resumo do checkout

## O que muda de caso para caso (comportamento diferente)
- **Modalidade de entrega**: cada uma cobra, tem prazo e limitações próprias, e entra opção nova
  quase toda semana → interface `ModalidadeEntrega` (codigo, atende(carrinho), frete(carrinho),
  prazoDias()), uma classe por modalidade.
- **Cupom**: cada um tem regra própria de desconto e de aplicabilidade → interface `Cupom`
  (codigo, aplicavel(ctx), desconto(ctx)). O caso mais exigente define o contexto:
  FRETEGRATIS precisa do frete já calculado, LEVE3PAGUE2 precisa dos itens, MENOS50 do subtotal.
  Por isso o cupom recebe `ContextoCupom(carrinho, frete)` e é calculado depois do frete.
  Ausência de cupom = objeto `SemCupom` (desconto zero), não um `if` no cálculo.
- **Nível do clube**: cada nível tem seu conjunto de vantagens e virão outros → interface
  `NivelClube` (codigo, percentualCredito, frete(freteModalidade), daBrinde(subtotal)); o próprio nível decide o frete cobrado (OURO zera), uma classe por nível.
- **Forma de pagamento**: regras de parcelas, disponibilidade e ajuste diferentes → interface
  `FormaPagamento` (codigo, parcelasPermitidas(n), disponivel(total), pagar(total, n)),
  uma classe por forma. O caso mais exigente (cartão) define o retorno: total final + parcela.

## O que é igual
- Ordem do cálculo, validação na ordem dos códigos de erro, arredondamento HALF_EVEN em centavos
  (`Dinheiro`), peso e subtotal do carrinho (`Carrinho`).
- **Seguro por região**: só a porcentagem muda, a conta é a mesma → `enum Regiao` com o percentual;
  a conta fica num lugar só (`Regiao.seguro`). Sem strategy.
- **Crédito do clube**: a conta é a mesma, só o percentual muda → o nível só informa o percentual.

## Escolha entre casos
- Cada implementação é um bean Spring com `codigo()`. `Catalogo<T>` monta um mapa código → bean a
  partir da lista injetada; a escolha é um lookup no mapa (nada de if/switch). Nova modalidade,
  cupom, nível ou forma de pagamento = uma classe nova.
- Erros: `ResumoException(codigo)` → `{ "erro": "CODIGO" }` com HTTP 422.
