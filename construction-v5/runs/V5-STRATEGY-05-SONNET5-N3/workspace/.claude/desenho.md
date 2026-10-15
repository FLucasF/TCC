# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (e precisa de um lugar próprio por caso)

- **Entrega** (`ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY`, e outras que virão
  quase toda semana): cada uma tem fórmula de custo, prazo e regra de disponibilidade
  próprias. → interface `OpcaoEntrega` (custo, prazoDias, disponivel), uma implementação
  por opção, registradas num `Map<codigo, OpcaoEntrega>` via Spring (`@Component` +
  coleta de `List<OpcaoEntrega>`). Adicionar opção nova = nova classe, nada existente
  é tocado.
- **Cupom** (`BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2`, e o marketing
  inventa mais): cada um tem condição de aplicabilidade e cálculo de desconto próprios.
  → interface `Cupom` (aplicavel, desconto), uma implementação por cupom, registradas
  num `Map<codigo, Cupom>` do mesmo jeito.
- **Nível do clube** (`BRONZE`, `PRATA`, `OURO`, e mais níveis estudados): cada um tem
  seu conjunto de vantagens (percentual de crédito, frete grátis, brinde).
  → interface `NivelClube` (percentualCredito, freteGratis, elegivelBrinde), uma
  implementação por nível, registradas num `Map<codigo, NivelClube>`.
- **Forma de pagamento** (`PIX`, `CARTAO`, `BOLETO` — lista fechada, não varia com o
  tempo segundo o enunciado): cada uma tem parcelamento permitido, disponibilidade e
  ajuste financeiro próprios. → enum `FormaPagamento` + interface
  `CalculoPagamento` (parcelaPermitida, disponivel, calcular), um `Map<FormaPagamento,
  CalculoPagamento>` injetado. Sem cadeia de if/else para escolher.

## O que é igual em todos os casos (não precisa de estrutura por caso)

- **Região** (seguro): a fórmula é sempre "percentual × produtos, arredondado"; só o
  percentual muda. → `enum Regiao` com o percentual como dado (sem classe/estratégia
  por região).
- Arredondamento "meio para o par" em cada etapa → uma função utilitária única
  (`Arredondamento.paraCentavos`), chamada em todos os lugares que geram valor
  monetário.
- A ordem de validação (10 códigos de erro) é uma sequência fixa definida pelo
  enunciado, não uma escolha entre comportamentos — fica numa única classe de
  validação que lança `CheckoutException(codigo)` no primeiro problema encontrado,
  seguindo exatamente a ordem da tabela.

## Fluxo do serviço

`ResumoCompraService`: valida (ordem fixa) → calcula subtotal/peso → resolve
OpcaoEntrega (frete + prazo, com frete zerado se nível tiver freteGratis) → resolve
Cupom (desconto, usando o frete já exibido para FRETEGRATIS) → calcula seguro pela
Regiao → soma o total do pedido → valida disponibilidade da forma de pagamento sobre
esse total → resolve CalculoPagamento (ajuste, parcela, total final) → resolve
NivelClube (crédito, brinde) → monta a resposta.

## Assinaturas pensadas pelo caso mais exigente

- `OpcaoEntrega.custo(BigDecimal pesoTotalKg)`: recebe o peso porque toda fórmula de
  frete depende dele (mesmo `RETIRADA_LOJA`, que ignora, precisa aceitar o mesmo
  parâmetro).
- `Cupom.desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal
  freteExibido)`: recebe os três porque `LEVE3PAGUE2` precisa dos itens,
  `MENOS50`/`BEMVINDO10` do subtotal, e `FRETEGRATIS` do frete — a assinatura cobre o
  cupom mais exigente (`LEVE3PAGUE2`) e serve para todos.
- `CalculoPagamento.calcular(BigDecimal totalPedido, int parcelas)`: `CARTAO` exige
  parcelas para a tabela Price; `PIX`/`BOLETO` recebem o mesmo parâmetro mas ignoram.
