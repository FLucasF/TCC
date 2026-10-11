# Desenho — resumo do checkout

## O que muda de caso para caso
- **Modalidade de entrega** (cresce toda semana): cada uma tem seu jeito de cobrar,
  seu prazo e sua limitação. → interface `ModalidadeEntrega` (`codigo`, `prazoDias`,
  `atende(carrinho)`, `frete(carrinho)`), uma classe `@Component` por modalidade.
- **Cupom** (marketing inventa sempre): cada um tem sua condição e sua conta.
  → interface `Cupom` (`codigo`, `aplicavel(carrinho)`, `desconto(carrinho, frete)`),
  uma classe `@Component` por cupom. A assinatura recebe o frete porque o caso mais
  exigente (FRETEGRATIS) precisa dele; LEVE3PAGUE2 precisa dos itens (vêm no carrinho).
- **Nível do clube** (vão surgir mais, cada um com suas vantagens): enum `NivelClube`
  com corpo próprio por constante (`credito`, `isentaFrete`, `brinde`).
- **Forma de pagamento**: cada uma valida parcelas, disponibilidade e ajusta o total
  de um jeito. → enum `FormaPagamento` com corpo próprio por constante
  (`aceitaParcelas`, `atende(total)`, `pagar(total, parcelas)`).

## O que é igual
- Região: só a porcentagem muda, a conta é a mesma → enum `Regiao` com o percentual
  e uma única conta de seguro (sem estratégia por região).
- Sequência do cálculo, ordem das validações, arredondamento HALF_EVEN para centavos
  (`Dinheiro`), subtotal e peso do carrinho (`Carrinho`).

## Estrutura
- Escolha do caso por mapa código → implementação (`Map` montado das beans / busca
  pelo nome da constante do enum, sem exceção para código nulo ou desconhecido), nunca por if/switch.
- `CheckoutService` orquestra a ordem fixa e lança `CheckoutException(codigo)`;
  `CheckoutController` expõe `POST /checkout/resumo` e traduz a exceção em
  `{ "erro": "CODIGO" }` com HTTP 422.
- Entrega e cupom são beans porque são conjuntos abertos (nova opção = nova classe);
  nível e pagamento são enums com corpo por constante porque são conjuntos fechados
  e pequenos no enunciado.
- LEVE3PAGUE2 conta por linha do carrinho (cada linha é um item; a entrada não tem
  identificador de produto além do nome).
