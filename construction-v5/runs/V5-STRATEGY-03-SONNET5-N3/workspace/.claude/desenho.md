# Desenho: serviço de resumo de compra

## O que muda de caso para caso

- **Entrega** (`modalidadeEntrega`): cada opção tem sua própria conta de
  frete, seu prazo e sua própria condição de disponibilidade (ex.: motoboy
  só até 5kg). Novas opções chegam com frequência ("quase toda semana").
  → uma implementação por modalidade, escolhida por um registro
  (Map<String, ModalidadeEntrega>), nunca por if/else encadeado.

- **Cupom** (`cupom`): cada cupom tem sua própria conta de desconto e sua
  própria condição de aplicabilidade (MENOS50 exige subtotal mínimo;
  FRETEGRATIS depende do frete; LEVE3PAGUE2 depende da quantidade por item).
  → uma implementação por cupom, escolhida por registro (Map<String, Cupom>).

- **Nível do clube** (`nivelClube`): cada nível tem seu próprio conjunto de
  vantagens (percentual de crédito, isenção de frete, brinde). "Estamos
  estudando criar mais níveis, cada um com seu conjunto de vantagens."
  → uma implementação por nível, escolhida por registro (Map<String, NivelClube>).

- **Forma de pagamento** (`formaPagamento`): cada forma tem seu próprio
  ajuste sobre o total (desconto Pix, tarifa fixa do boleto, juros Price do
  cartão), sua própria validação de parcelas e sua própria condição de
  disponibilidade (boleto acima de R$1000).
  → uma implementação por forma, escolhida por registro (Map<String, FormaPagamento>).

## O que é igual em todos os casos

- **Seguro por região**: o enunciado diz explicitamente que "é só a
  porcentagem que muda, a conta é a mesma em todas". Não é polimorfismo,
  é uma tabela fixa Map<Regiao, BigDecimal> usada numa única fórmula.

- **Arredondamento**: toda etapa em dinheiro usa "meio para o par"
  (BigDecimal.ROUND_HALF_EVEN, escala 2) — uma função utilitária única.

- **Ordem do cálculo**: subtotal → desconto cupom → frete → seguro →
  total do pedido → ajuste de pagamento → total final. Essa sequência é
  fixa e não varia por nenhum dos casos acima; mora no serviço principal
  (`ResumoCompraService`), que orquestra as peças variáveis.

- **Validação de erros**: a ordem de checagem (1 a 10) é fixa e não varia
  por caso; cada checagem específica (disponibilidade de modalidade,
  aplicabilidade de cupom, disponibilidade de forma de pagamento) é
  delegada ao objeto do próprio caso, mas a sequência de checagem é única
  e mora no serviço.

## Assinaturas (pensadas pelos casos mais exigentes)

```java
interface ModalidadeEntrega {
    void validarDisponibilidade(BigDecimal pesoKg); // lança MODALIDADE_INDISPONIVEL
    BigDecimal calcularFrete(BigDecimal pesoKg); // nenhuma modalidade hoje precisa do subtotal
    int prazoDias();
}

interface Cupom {
    void validarAplicavel(ContextoPedido contexto); // lança CUPOM_NAO_APLICAVEL
    BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete);
}

interface NivelClube {
    BigDecimal percentualCredito();
    boolean freteGratis();
    boolean concedeBrinde(BigDecimal subtotalProdutos);
}

interface FormaPagamento {
    void validarParcelas(int parcelas);        // lança PARCELAMENTO_INVALIDO
    void validarDisponibilidade(BigDecimal totalPedido); // lança FORMA_PAGAMENTO_INDISPONIVEL
    AjustePagamento calcular(BigDecimal totalPedido, int parcelas);
}
```

`ContextoPedido` carrega só itens e subtotal — é o que `LEVE3PAGUE2`
precisa (quantidade por item) e o que `MENOS50` precisa (subtotal). O peso
não entra no contexto porque nenhum cupom usa peso; ele é passado direto
para `ModalidadeEntrega`, que é quem precisa dele.

`AjustePagamento` é um record (ajuste, totalFinal, valorParcela) porque
cada forma de pagamento produz os três juntos a partir da mesma fórmula.

## Por que não generalizar mais

O seguro por região e o arredondamento são tratados como tabela/função
porque o enunciado afirma que a conta é igual; não criamos uma interface
`SeguroRegiao` para isso. Não foi criada estrutura de extensão para nada
que o enunciado não descreve como variando (ex.: moeda, impostos).
