# Desenho: serviço de resumo do checkout

## O que muda de caso para caso
- **Modalidade de entrega** (4): preço (base + por kg), prazo, disponibilidade (motoboy só até 5 kg).
- **Cupom** (4): regra de desconto (% dos produtos, valor fixo, igual ao frete, por item), condição de aplicação (MENOS50 exige produtos >= 300).
- **Nível do clube** (3): crédito (%), frete zerado (OURO), brinde (OURO com produtos > 500).
- **Forma de pagamento** (3): parcelas permitidas, disponibilidade (boleto até 1.000), ajuste do total (Pix -5%, boleto +3,49, cartão sem juros até 3x e Price 1,99% de 4x a 12x).
- **Região** (5): percentual do seguro.

## O que é igual em todos os casos
- Validação de itens e a ordem dos códigos de erro.
- Subtotal = soma de (preço × quantidade); peso = soma de (peso × quantidade), sem arredondar.
- Total = produtos − desconto + frete + seguro.
- Arredondamento a centavos em cada etapa, meio para o par.
- Montagem da resposta e dos erros `{ "erro": CODIGO }`.

## Estrutura escolhida
- Cada dimensão variável é um `enum` com o comportamento de cada caso dentro da própria constante (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `Regiao`, `FormaPagamento`). Assim, cada regra fica num lugar só.
- A escolha do caso é uma busca pelo nome (`Busca.porNome`), sem sequência de `if`.
- `CalculadoraResumo` é o único lugar que conhece a ordem dos passos. Ele só chama os métodos dos enums.
- Assinaturas pensadas para o caso mais exigente:
  - `Cupom.desconto(Pedido, BigDecimal frete)`: FRETEGRATIS precisa do frete já calculado e LEVE3PAGUE2 precisa dos itens.
  - `FormaPagamento.aplicar(BigDecimal total, int parcelas)`: juros dependem do total e do número de parcelas.
  - `NivelClube.ajustarFrete(frete)` e `temBrinde(subtotal)`: OURO zera o frete, qualquer modalidade.
- Erros: `RecusaPedido` carrega um `Erro`. Um `@RestControllerAdvice` converte em HTTP 400 com `{ "erro": ... }`.
- Sem banco. Dinheiro em `BigDecimal` com `HALF_EVEN`.

## Decisões de interpretação
- Cupom comparado exatamente como escrito (maiúsculas). Cupom vazio ou nulo é tratado como "sem cupom".
- LEVE3PAGUE2 conta por linha do carrinho, não soma linhas de um mesmo produto. A tarefa não diz o contrário; confirmar com o financeiro.
- FRETEGRATIS usa o frete já ajustado pelo clube, então vale 0 para OURO.
- Brinde: produtos estritamente maiores que R$ 500,00.
- Boleto: o limite de R$ 1.000,00 vale para o total do pedido, antes da tarifa.
- Status HTTP de erro: 400 (o enunciado não fixa).
