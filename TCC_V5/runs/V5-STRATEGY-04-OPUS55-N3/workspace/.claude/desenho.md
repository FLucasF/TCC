# Desenho — resumo do checkout

## O que muda de caso para caso (enunciado diz que varia)
- **Modalidade de entrega**: forma de cobrar, prazo e limitação ("entra opção nova toda semana").
- **Cupom**: condição de aplicação e forma de calcular o desconto ("marketing inventa promoção").
- **Nível do clube**: conjunto de vantagens (frete, crédito, brinde) ("estudando criar mais níveis").
- **Forma de pagamento**: parcelas aceitas, disponibilidade e ajuste (desconto Pix, tarifa boleto, juros cartão).

## O que é igual
- Ordem do cálculo, arredondamento HALF_EVEN em centavos, ordem das validações, formato de entrada/saída.
- **Região**: só a porcentagem muda, a conta é a mesma → enum com o percentual (dado, não comportamento).

## Estrutura
- Uma interface por eixo (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`), cada caso
  numa classe `@Component` própria que informa seu `codigo()`.
- `Catalogo<T>` monta um mapa código → implementação a partir dos beans; escolher o caso é uma busca
  no mapa, não cadeia de `if`. Caso novo = classe nova, nada mais muda.
- `CalculadoraResumo` orquestra a ordem fixa e as validações; `Dinheiro` centraliza o arredondamento.

## Assinaturas (pensadas no caso mais exigente)
- Entrega: `atende(Carrinho)` / `frete(Carrinho)` / `prazoDias()` — Econômica/Expressa/Motoboy precisam do peso.
- Cupom: `aplicavel(ContextoCupom)` / `desconto(ContextoCupom)`; contexto tem itens, subtotal e frete,
  porque LEVE3PAGUE2 precisa dos itens e FRETEGRATIS do frete (frete calculado antes do cupom, já com a vantagem do clube — com OURO o FRETEGRATIS desconta 0,00;
  a validação da modalidade já vem antes da do cupom).
- Clube: `frete(freteCalculado)` / `credito(subtotal)` / `brinde(subtotal)`.
- Pagamento: `aceitaParcelas(n)` / `disponivel(total)` / `pagar(total, n)` → valor final e parcela.
