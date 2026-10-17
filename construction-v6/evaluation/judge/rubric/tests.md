# A dimensão: a cobertura e a qualidade dos testes

A correção vai em `correcao/testes.md`. O enunciado **não pede testes**: só diz que "o projeto precisa funcionar com `mvn verify`"
num computador "sem banco de dados nem Docker". Escrever testes, e quais, foi decisão do agente.

Além do enunciado e do projeto, você lê os números da **medida por mutação** (a linha da execução no CSV do PIT do lote), feita por uma
ferramenta (PIT) que planta pequenos erros no código do agente, um de cada vez, e roda os testes dele contra cada
um:

| campo | o que é |
|---|---|
| `situation` | `ok` (a medida rodou), `no-tests` (o agente não escreveu testes), `tests-fail` (os testes do agente falham sem nenhum erro plantado), `not-compiled`, `no-mutants`, `failed` |
| `tests` | quantos métodos de teste (`@Test`, `@ParameterizedTest`) o agente escreveu |
| `line_coverage` | a fração das linhas do código principal que os testes executam |
| `mutation_score` | a fração dos erros plantados que os testes pegam (a medida principal) |
| `test_strength` | a fração dos erros pegos entre os que os testes executam |
| `mutants`, `killed`, `survived`, `no_coverage` | os totais: plantados, pegos, que passaram, e em linhas que nenhum teste executa |

Os números são confiáveis e não se refazem: use-os como estão. O que eles **não** dizem é *o quê* os testes
conferem. Isso é o que você julga, lendo o `src/test` do projeto. Quando a `situation` não for `ok`, julgue os testes
pelo código, e diga na observação que os números não existem.

## Os itens

**`T1`: o que dos requisitos do enunciado os testes cobrem?** Uma resposta por área, no formato
`área: valor` na observação, e o valor do item resume:

| área | o que conta como coberto |
|---|---|
| `calculo` | um teste confere os valores do resumo (o subtotal, o desconto, o frete, o seguro, o total final) |
| `recusas` | um teste confere alguma das recusas do resumo ou do pedido pelo código de erro |
| `ciclo` | um teste confere as transições do pedido (pagar, enviar, entregar, cancelar) e as proibidas |
| `pagamento_repetido` | um teste confere que o mesmo aviso de pagamento não baixa o estoque nem dá o crédito duas vezes |
| `concorrencia` | um teste confere a última peça disputada por duas compras ao mesmo tempo |
| `transportadora` | um teste confere a TransLog fora do ar, ou o CEP que ela não atende |

| valor | quando |
|---|---|
| `amplo` | cobre `calculo`, `recusas` e `ciclo`, e pelo menos uma das três últimas |
| `parcial` | cobre algumas das áreas, mas não o que `amplo` pede |
| `minimo` | cobre no máximo uma área |
| `nao_ha_testes` | não há testes |

**`T2`: as asserções conferem valores?**

| valor | quando |
|---|---|
| `valores` | as asserções conferem os resultados do negócio (o valor do total, a situação do pedido depois da operação, o código da recusa) |
| `mistas` | parte confere valores, parte só confere que não deu exceção, que o status HTTP é 200, ou que algo não é nulo |
| `superficiais` | quase todas só conferem que não deu exceção, o status HTTP, ou que algo não é nulo |
| `nao_ha_testes` | não há testes |

**`T3`: os testes rodam sem banco e sem internet, como o enunciado pede?** Veja como os testes resolvem o banco
(um banco em memória, repositórios falsos, só testes de unidade) e a TransLog (um servidor falso, um dublê da
interface).

| valor | quando |
|---|---|
| `atende` | nenhum teste precisa de PostgreSQL, de Docker ou da TransLog de verdade |
| `nao_atende` | algum teste precisa de um deles (inclusive Testcontainers); cite onde |
| `nao_ha_testes` | não há testes |

**`T4`: os testes dependem de coisas que mudam sozinhas?** O relógio de verdade (`now()` sem um relógio
controlado, para as 48 horas), `sleep` para esperar, a ordem em que os testes rodam, um estado que um teste deixa
para o outro.

| valor | quando |
|---|---|
| `atende` | não dependem: o tempo é controlado, cada teste monta o seu estado |
| `nao_atende` | algum teste depende; cite onde |
| `nao_ha_testes` | não há testes |
