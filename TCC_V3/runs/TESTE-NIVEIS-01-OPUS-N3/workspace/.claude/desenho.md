# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (o enunciado descreve como variando)

1. **Modalidade de entrega**: como cobra o frete, o prazo e se atende o pedido
   (motoboy até 5 kg). O enunciado diz que entra opção nova quase toda semana.
2. **Cupom**: a condição para valer e como calcula o desconto.
3. **Nível do clube**: percentual de crédito, isenção de frete, brinde.
   O enunciado diz que vão estudar criar mais níveis.
4. **Forma de pagamento**: parcelas permitidas, se atende o pedido e como o
   total final sai do total do pedido (desconto, tarifa, juros).

## O que é igual em todos os casos

- Arredondamento: sempre centavos, HALF_EVEN (`Dinheiro.centavos`).
- Ordem do cálculo: produtos → cupom → frete → seguro → total → pagamento.
- Seguro: **só a porcentagem muda por região**, a conta é a mesma. Por isso
  região é um enum que carrega só a alíquota — não há comportamento por caso.
- Crédito do clube e seguro incidem sobre o subtotal de produtos.
- Ordem de validação dos erros é fixa (1 a 10).

## Estrutura escolhida e por quê

Cada eixo que varia é uma **interface com uma implementação por caso**
(`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`), registrada como
bean do Spring e resolvida por código num `Catalogo<T>` genérico (mapa
código → implementação). Assim:

- o comportamento de cada caso mora num arquivo só dele;
- escolher o caso é uma busca em mapa, não cadeia de `if`/`switch`;
- opção nova = classe nova, sem mexer no cálculo;
- código inexistente = `Optional` vazio → erro na ordem certa.

`CalculadoraResumo` é o único lugar com a ordem do cálculo e a ordem das
validações; ela não conhece caso nenhum, só as interfaces.

## Assinaturas decididas olhando todos os casos

- `ModalidadeEntrega`: `atende(Pedido)` + `Entrega apurar(Pedido)` (frete e prazo
  num retorno só, para não apurar duas vezes) — o caso
  mais exigente é o que cobra por kg e o que limita peso, então recebe o pedido
  inteiro (que expõe `pesoKg()` e `subtotal()`).
- `Cupom`: `aplicavel(ContextoCupom)` e `desconto(ContextoCupom)`. O contexto
  leva itens (LEVE3PAGUE2), subtotal (BEMVINDO10/MENOS50) e frete
  (FRETEGRATIS) — logo o frete é apurado antes do desconto, embora apareça
  depois no resumo.
- `NivelClube`: `isentaFrete()`, `credito(subtotal)`, `brinde(subtotal)`.
- `FormaPagamento`: `permiteParcelas(int)`, `atende(total)` e
  `ResultadoPagamento calcular(total, parcelas)` (totalFinal + valorParcela) —
  o caso mais exigente é o cartão com juros, que define as duas saídas.

A requisição chega com os enums como `String`, para que valor inválido vire o
código de erro da tabela e não erro de desserialização.

## Decisões de leitura do enunciado

- **LEVE3PAGUE2** conta por item do carrinho ("a cada 3 unidades de um mesmo
  item do carrinho"): duas linhas do mesmo produto contam separado.
- **Código de cupom** casa exatamente como está escrito, em maiúsculas; outro
  jeito de escrever é `CUPOM_INVALIDO`.
- **Quem arredonda**: cada caso devolve o valor já em centavos, e a
  `CalculadoraResumo` arredonda só o que ela mesma soma (o total do pedido).
  Assim ninguém arredonda duas vezes.
- O frete que o cupom FRETEGRATIS olha já é o frete do resumo — ou seja, com a
  isenção do OURO aplicada. OURO com FRETEGRATIS dá frete 0 e desconto 0.
