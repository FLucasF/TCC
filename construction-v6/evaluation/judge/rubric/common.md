# A correção de um serviço

Este guia é para a IA que corrige, numa sessão com o Lucas, **uma dimensão de uma execução** da construção: o serviço
de pedidos de uma loja online, em Java com Spring Boot e PostgreSQL, que um agente de programação escreveu do zero
a partir de um enunciado. A régua da dimensão vem logo depois deste guia (`patterns.md`, `architecture.md`,
`tests.md` ou `database.md`). Leia os dois inteiros antes de abrir o código.

A pergunta não é se o serviço dá a resposta certa (isso é medido à parte, pela suíte de testes), e sim se o
desenho segue as boas práticas da dimensão. Não existe uma solução perfeita a ser encontrada: cada item da régua
pergunta uma coisa sobre o código, e você responde com um dos valores da lista fechada daquele item.

## O que ler, numa pasta de execução (`runs/<lote>/<execução>/`)

| o quê | onde | para quê |
|---|---|---|
| o enunciado | `prompt.md` | o texto exato que **aquele** agente recebeu. Use este, e não o de `experiment/prompt/`: os pilotos rodaram com versões anteriores |
| o projeto | `workspace/`, o projeto do `pom.xml` mais raso (o mesmo que o build compila); só o `pom.xml` e o `src/` | todas as dimensões |
| o relatório de arquitetura | `architecture/architecture-report.txt` | a arquitetura |
| os números do PIT | a linha da execução no CSV do lote (`analysis/<lote>/pit.csv`) | os testes |
| a foto do banco | `suite/database-schema.json` | o banco. Se o arquivo não existir, ou não tiver nenhuma tabela, o serviço não subiu na suíte: corrija pelo código e diga isso |

**O que não abrir:** o `meta.json`, a transcrição (`claude-output.jsonl`), o `run.log`, o `stderr.txt`, o
`build.txt`, os resultados da suíte (`suite-report.txt`, `suite-result.json`) e as correções de outras dimensões.
Eles dizem quem fez, quanto custou ou se o serviço acertou, e nada disso entra no desenho.

**Às cegas.** O nome da pasta da execução diz o modelo (`OPUS55-N0-03`). Não use isso, e não tente adivinhar quem
fez o trabalho. Quando a correção valer para o TCC, o Lucas entrega a execução numa cópia com um nome neutro.

## As regras

1. **Uma dimensão por conversa.** A impressão de uma dimensão não pode contaminar a outra (o efeito halo). Para
   corrigir outra dimensão da mesma execução, comece outra conversa.
2. **Julgue pelo que o código faz, nunca pelo que ele diz de si.** Os comentários, os READMEs e os nomes das classes
   não contam. Uma classe chamada `ShippingStrategy` que esconde um `switch` sobre as entregas não é um Strategy;
   um pacote chamado `domain` que importa o controller não é um domínio isolado.
3. **Toda resposta precisa de evidência.** No código, o formato é `arquivo:linha`, com o caminho a partir da pasta
   do projeto. Num material (o relatório de arquitetura, a foto do banco, o CSV do PIT), cite o arquivo e o nome do
   elemento (a tabela, a coluna, a restrição, o grupo de pacotes). Uma resposta sem evidência não conta.
4. **Na dúvida entre dois valores, não escolha o mais provável.** Responda `indeterminado` e conte a dúvida na
   observação do item. `indeterminado` é uma resposta válida; um valor chutado não é.
5. **Responda todos os itens**, na ordem da régua, com os ids e os valores da régua (sem inventar valor novo). Um
   item que não se aplica recebe `nao_se_aplica`, quando a lista do item o tiver, ou `indeterminado`, com o motivo.
6. **Não mude nenhum arquivo do projeto.** Ler e buscar à vontade (`grep`, `find`); não precisa compilar nem rodar.

## A resposta

Escreva a correção em `runs/<lote>/<execução>/correcao/<dimensão>.md` (`padroes.md`, `arquitetura.md`,
`testes.md` ou `banco.md`), neste formato:

```markdown
# <dimensão>: <execução>

| item | resposta | evidência | observação |
|---|---|---|---|
| B1 | atende | `suite/database-schema.json`: as 6 tabelas têm PRIMARY KEY; `src/main/resources/schema.sql:2` | |
| B2 | parcial | `schema.sql:42`: `order_item.sku` sem `REFERENCES` | as outras quatro ligações têm a chave |

**Resumo.** Duas ou três frases sobre a dimensão como um todo.

**Dúvidas.** O que ficou incerto, se ficou.
```

O resumo não muda nenhuma resposta: é a leitura geral, para o Lucas revisar a correção.
