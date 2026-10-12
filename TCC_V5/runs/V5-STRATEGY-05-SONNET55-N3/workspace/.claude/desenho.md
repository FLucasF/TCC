# Desenho

## O que muda de caso para caso (cada um vira uma classe própria, registrada como @Component)
- **Entrega** (`ModalidadeEntrega`): custo do frete, prazo, e se atende o pedido (motoboy ≤ 5 kg). Entram opções novas toda semana: basta criar uma classe.
- **Cupom** (`Cupom`): se é aplicável e quanto desconta. Precisa do carrinho (itens, para o LEVE3PAGUE2), do subtotal e do frete (FRETEGRATIS) → a assinatura recebe tudo isso.
- **Nível do clube** (`NivelClube`): crédito, isenção de frete, brinde. Vão entrar níveis novos.
- **Forma de pagamento** (`FormaPagamento`): parcelas permitidas, se atende o total, e como cobra (total final + parcela).

## O que muda só em número (dado, não comportamento)
- **Região**: só a porcentagem do seguro muda; a conta é a mesma → enum com percentual.

## Igual em todos os casos
- O fluxo de cálculo e a ordem das validações (`ResumoService`), arredondamento meio-para-o-par (`Dinheiro`), a busca por código (`Catalogo`), o contrato JSON.

## Escolha entre casos
- `Catalogo<T>` busca pelo código num mapa montado com as implementações injetadas pelo Spring; sem if/switch em cadeia.
- Erros: `ErroPedido(codigo)` → resposta `{ "erro": "CODIGO" }` (HTTP 422).
