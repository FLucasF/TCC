# Desenho

## O que muda de caso para caso
- **Entrega** (novas toda semana): custo do frete, prazo, se atende o pedido (ex.: motoboy ≤ 5 kg).
- **Cupom** (marketing inventa): se é aplicável ao pedido e como calcula o desconto (precisa dos itens, subtotal e frete).
- **Nível do clube** (virão mais níveis): crédito, tratamento do frete, brinde.
- **Forma de pagamento**: parcelamento permitido, disponibilidade (boleto ≤ 1000), valor final e parcela.

## O que é igual
- A ordem do cálculo (subtotal → frete → cupom → seguro → total → pagamento), o arredondamento
  meio-para-o-par a cada etapa, a ordem de validação dos erros, o formato de entrada/saída.
- **Seguro**: só a porcentagem muda por região, a conta é a mesma → enum `Regiao` com o percentual, sem estratégia.

## Estrutura
- Uma interface por eixo variável (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`),
  uma classe `@Component` por caso. Spring injeta a lista; `CalculadoraResumo` indexa por código
  (sem if/switch). Novo caso = nova classe, nada mais muda.
- Assinaturas atendendo o caso mais exigente: tudo recebe `Carrinho` (itens, subtotal, peso);
  `Cupom.desconto` também recebe o frete (FRETEGRATIS); `FormaPagamento` recebe total e parcelas
  (cartão com juros) e valida parcelas/disponibilidade separadamente (ordem de erros 9 e 10).
- Frete: o nível do clube ajusta o frete (OURO zera) antes do cupom, pois FRETEGRATIS iguala o frete cobrado.
- Erros: `ErroCheckout(codigo)` → HTTP 400 `{ "erro": CODIGO }`. Entrada com textos para mapear códigos desconhecidos.
- `CalculadoraResumo` faz o pipeline; `CheckoutController` expõe `POST /checkout/resumo`.
