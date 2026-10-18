# A correção de um serviço

Este guia é para a IA que corrige **uma dimensão de uma execução** da construção: o serviço de pedidos de uma loja
online, em Java e PostgreSQL, com o framework web que o enunciado daquela execução pede (Javalin; Spring Boot nos
lotes até 18/10/2026), que um agente de programação escreveu do zero a partir de um enunciado. A régua da dimensão vem logo depois deste guia: `architecture.md`, `tests.md`, `database.md` ou
`behavior.md`; os padrões têm uma régua por pattern, como na manutenção (`patterns/common.md` e depois
`patterns/strategy.md`, `patterns/adapter.md`, `patterns/state.md` ou `patterns/factory.md`). Leia tudo inteiro
antes de abrir o código.

Você corrige sozinha, às cegas, numa pasta de trabalho preparada para isso. Você não sabe qual agente ou modelo
fez o serviço, e não deve tentar adivinhar.

São cinco dimensões. Quatro olham o **desenho** (a arquitetura, os padrões, os testes, o banco): ali a pergunta não
é se o serviço dá a resposta certa, e sim se o código segue as boas práticas da dimensão. A quinta, o
**comportamento** (`behavior.md`), é a que pergunta se o serviço faz o que o enunciado pede: nela você sobe o
serviço e testa. Não existe uma solução perfeita a ser encontrada. A régua traz as perguntas e as definições; a
resposta é um **feedback escrito**, como o de um revisor de código, com **uma linha de conclusão em cada item**.

## O que há na pasta de trabalho

| o quê | onde | para quê |
|---|---|---|
| o enunciado | `prompt.md` | o texto exato que aquele agente recebeu (os pilotos rodaram com versões anteriores do enunciado; use sempre este) |
| o projeto | `workspace/`, o projeto do `pom.xml` mais raso (o mesmo que o build compila) | todas as dimensões |
| o relatório de arquitetura | `architecture/architecture-report.txt` | a arquitetura |
| os números do PIT | `pit.txt` | os testes |
| a foto do banco | `database/database-schema.json` | o banco. Se não houver tabela nenhuma, o serviço não subiu: corrija pelo código e diga isso |

Os três materiais são feitos por ferramentas antes da correção. Eles ajudam a ver o que a leitura vê mal (um ciclo
entre pacotes, um teste que roda e não confere nada, a coluna que o banco realmente criou), mas **não são nota**:
quem decide o que eles significam é você.

## As regras

1. **Uma dimensão por conversa** (nos padrões, um pattern por conversa). A impressão de uma não pode contaminar a
   outra (o efeito halo).
2. **Julgue pelo que o código faz, nunca pelo que ele diz de si.** Os comentários, os READMEs e os nomes das classes
   não contam. Uma classe chamada `ShippingStrategy` que esconde um `switch` sobre as entregas não é um Strategy;
   um pacote chamado `domain` que importa o controller não é um domínio isolado.
3. **Toda afirmação precisa de evidência.** No código, `arquivo:linha`, com o caminho a partir da pasta do
   projeto. Num material, o arquivo e o nome do elemento (a tabela, a coluna, a restrição, o grupo de pacotes). No
   comportamento, a chamada que você fez, o que esperava e o que veio. Uma afirmação sem evidência não conta.
4. **Diga quando não tiver certeza.** Se a evidência não decide, escreva o que ela mostra, o que fica em dúvida e
   por quê. Não arredonde a dúvida para o lado mais provável.
5. **Passe por todos os itens da régua**, na ordem dela. Um item que não se aplica leva uma linha dizendo isso.
6. **Não mude nenhum arquivo da pasta de trabalho.** Nas dimensões de desenho, leia e busque à vontade (`grep`,
   `find`); não precisa compilar nem rodar. No comportamento, compile e rode numa cópia do projeto (`behavior.md`
   diz como).

## A resposta

Responda só com a correção, em markdown, neste formato:

```markdown
# <dimensão>

## Visão geral

Um parágrafo: como o agente resolveu a dimensão, os pontos fortes e os problemas mais sérios, em ordem de gravidade.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, as restrições `p`), e
a do item do pedido é composta, `(order_id, line_no)` (`schema.sql:46`). Segue a prática.
Conclusão: segue

**B2, as ligações.** ...
Conclusão: em parte

## O que mudaria primeiro

Uma ou duas mudanças que mais melhorariam a dimensão, com o motivo.

## Dúvidas

O que o enunciado ou a régua não decidem e você teve de decidir, com a leitura que escolheu (ou "nenhuma").
```

**A linha de conclusão** fecha cada item, sozinha na linha, começando por `Conclusão:` e com uma palavra só:

- nos **padrões**: `acerto`, `omissão` ou `malfeito` nos pontos que pedem o padrão (o óbvio e o remendo);
  `contenção` ou `exagero` nos controles negativos; `não se aplica` quando o ponto não existe naquela execução;
- nas **outras dimensões**: `segue`, `em parte`, `não segue` ou `não se aplica`.

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# A dimensão: a cobertura e a qualidade dos testes

O enunciado **não pede testes**: só diz que "o projeto precisa funcionar
com `mvn verify`" num computador "sem banco de dados nem Docker". Escrever testes, e quais, foi decisão do agente.

Além do enunciado e do projeto, você lê os números da **medida por mutação** (a linha da execução no CSV do PIT do
lote), feita por uma ferramenta (PIT) que planta pequenos erros no código do agente, um de cada vez, e roda os
testes dele contra cada um:

| campo | o que é |
|---|---|
| `situation` | `ok` (a medida rodou), `no-tests` (o agente não escreveu testes), `tests-fail` (os testes do agente falham sem nenhum erro plantado), `not-compiled`, `no-mutants`, `failed` |
| `tests` | quantos métodos de teste (`@Test`, `@ParameterizedTest`) o agente escreveu |
| `line_coverage` | a fração das linhas do código principal que os testes executam |
| `mutation_score` | a fração dos erros plantados que os testes pegam (a medida principal) |
| `test_strength` | a fração dos erros pegos entre os que os testes executam |
| `mutants`, `killed`, `survived`, `no_coverage` | os totais: plantados, pegos, que passaram, e em linhas que nenhum teste executa |

Os números são confiáveis e não se refazem: use-os como estão, e cite-os. O que eles **não** dizem é *o quê* os
testes conferem. Isso é o que você julga, lendo o `src/test` do projeto. Quando a `situation` não for `ok`, julgue
os testes pelo código e diga que os números não existem.

## Os itens

**T1, o que dos requisitos do enunciado os testes cobrem?** Passe pelas áreas abaixo e diga quais estão cobertas
(com o teste que cobre) e quais não:
- **o cálculo**: um teste confere os valores do resumo (o subtotal, o desconto, o frete, o seguro, o total final);
- **as recusas**: um teste confere alguma das recusas do resumo ou do pedido pelo código de erro;
- **o ciclo do pedido**: um teste confere as transições (pagar, enviar, entregar, cancelar) e as proibidas;
- **o pagamento repetido**: um teste confere que o mesmo aviso não baixa o estoque nem dá o crédito duas vezes;
- **a concorrência**: um teste confere a última peça disputada por duas compras ao mesmo tempo;
- **a transportadora**: um teste confere a TransLog fora do ar, ou o CEP que ela não atende.

**T2, as asserções conferem valores?** O bom é as asserções conferirem os resultados do negócio (o valor do total,
a situação do pedido depois da operação, o código da recusa). O fraco é só conferir que não deu exceção, que o
status HTTP é 200 ou que algo não é nulo: o teste passa por cima de um erro na conta. Diga qual é o caso, ou a
proporção, com exemplos. Os números do PIT ajudam: uma cobertura de linhas alta com poucos erros pegos é sinal de
testes que executam o código sem conferir.

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Veja como os testes resolvem o banco (um
banco em memória, repositórios falsos, só testes de unidade) e a TransLog (um servidor falso, um dublê da
interface). É problema algum teste precisar de PostgreSQL, de Docker (inclusive Testcontainers) ou da TransLog de
verdade. Cite onde.

**T4, os testes dependem de coisas que mudam sozinhas?** O relógio de verdade (`now()` sem um relógio controlado,
para as 48 horas), `sleep` para esperar, a ordem em que os testes rodam, um estado que um teste deixa para o outro.
O certo é o tempo ser controlado e cada teste montar o seu estado. Cite o que depender.
