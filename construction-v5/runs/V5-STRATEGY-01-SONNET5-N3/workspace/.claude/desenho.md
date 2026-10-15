# Desenho: resumo de checkout

## O que muda de caso para caso

- **Modalidade de entrega** (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY, e
  outras que vão entrar): fórmula do frete, prazo em dias, e se está
  disponível para o pedido (peso). Vira `enum ModalidadeEntrega` com um
  método abstrato por constante (`calcularFrete`, `prazoDias`,
  `validarDisponibilidade`) — cada opção carrega seu próprio comportamento,
  sem if/else para escolher.

- **Cupom** (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2): fórmula do
  desconto e condição de aplicabilidade. Vira `enum Cupom`, mesma ideia:
  método abstrato por constante (`calcularDesconto`, `validarAplicavel`).

- **Nível do clube** (BRONZE, PRATA, OURO, e outros futuros): percentual de
  crédito, se isenta frete, se dá brinde. Vira `enum NivelClube` com
  métodos por constante.

- **Forma de pagamento** (PIX, CARTAO, BOLETO): parcelas permitidas,
  disponibilidade conforme o total, e cálculo do ajuste/total
  final/parcela. Vira `enum FormaPagamento` com métodos por constante.

- **Região**: só a porcentagem do seguro muda, a conta é sempre a mesma
  (percentual × subtotal dos produtos). Não precisa de método por
  constante, é só um dado: `enum Regiao` com um campo `percentualSeguro`.

## O que é igual em todos os casos

- Ordem fixa do cálculo (subtotal → desconto cupom → frete → seguro →
  total do pedido → ajuste de pagamento) e ordem fixa de validação dos
  erros (1 a 10 do enunciado) — isso é sequência de passos do negócio, não
  escolha entre casos, então fica num único serviço orquestrador.
- Arredondamento "meio para o par" em 2 casas, usado em todo valor em
  dinheiro — um único helper (`Dinheiro.arredondar`).
- Formato de entrada/saída HTTP (JSON) e o envelope de erro `{ "erro": ... }`.

## Estrutura

- `dominio/`: os quatro enums de comportamento (`ModalidadeEntrega`,
  `Cupom`, `NivelClube`, `FormaPagamento`), o enum de dado (`Regiao`), e
  `Item`.
- `servico/CalculadoraResumo`: único lugar que conhece a ordem de
  validação e de cálculo; delega cada decisão ao enum correspondente.
- `web/`: controller + DTOs de request/response.
- `erro/PedidoException` + `@RestControllerAdvice`: mapeia o código de
  erro para `{ "erro": "CODIGO" }` com HTTP 400, mantendo a ordem decidida
  no serviço (primeiro erro encontrado vence).
- `util/Dinheiro`: arredondamento HALF_EVEN compartilhado.

Assinaturas pensadas para o caso mais exigente de cada família (ex.:
`Cupom.calcularDesconto` recebe subtotal, itens e frete — mesmo que
BEMVINDO10 só precise do subtotal — porque FRETEGRATIS precisa do frete e
LEVE3PAGUE2 precisa dos itens).
