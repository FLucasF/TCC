# A correção de um serviço

Você vai corrigir uma dimensão do desenho de um serviço que um agente de programação escreveu do zero: o serviço
de pedidos de uma loja online, em Java com Spring Boot e PostgreSQL. O agente recebeu o enunciado que está na
pasta de trabalho e escreveu o projeto inteiro. Você não sabe qual agente ou modelo fez o trabalho, e não deve
tentar adivinhar.

A pergunta não é se o serviço dá a resposta certa (isso é medido à parte, por uma suíte de testes), e sim se o
desenho segue as boas práticas da dimensão que está nas instruções abaixo. Não existe uma solução perfeita a ser
encontrada: cada item da régua pergunta uma coisa sobre o código, e você responde com um dos valores da lista
fechada daquele item.

## A pasta de trabalho

- `enunciado.md`: o texto exato que o agente recebeu.
- `projeto/`: o projeto do agente, sem a pasta `target/`, sem os arquivos de documentação e **sem comentários**.
  Os comentários foram trocados por linhas em branco, então os números de linha são os do original.
- os materiais da dimensão, quando houver (as instruções da dimensão dizem quais são e como ler cada um).

## As regras

1. **Leia o enunciado inteiro antes de olhar o código.** Os itens da régua citam trechos dele.
2. **Julgue pelo que o código faz, nunca pelo que ele diz de si.** Uma classe chamada `ShippingStrategy` que
   esconde um `switch` sobre as entregas não é um Strategy; um pacote chamado `domain` que importa o controller
   não é um domínio isolado.
3. **Toda resposta precisa de evidência.** No código, o formato é `arquivo:linha`, com o caminho a partir de
   `projeto/`. Num material (o relatório de arquitetura, a foto do banco), cite o arquivo do material e o nome
   do elemento (a tabela, a coluna, a restrição, o grupo de pacotes). Uma resposta sem evidência não conta.
4. **Na dúvida entre dois valores, não escolha o mais provável.** Responda `indeterminado` e conte a dúvida
   no campo `observacao` do item. `indeterminado` é uma resposta válida; um valor chutado não é.
5. **Responda todos os itens**, na ordem da régua, com os ids da régua. Um item que não se aplica ao projeto
   (por exemplo, a foto do banco quando o serviço não subiu) recebe o valor `nao_se_aplica`, quando a lista do
   item o tiver, ou `indeterminado`, com o motivo.
6. **Não mude nenhum arquivo.** Você pode ler à vontade e usar comandos de leitura e busca (`grep`, `find`,
   `cat`). Não precisa compilar nem rodar o projeto.

## A resposta

Responda só com um objeto JSON, sem texto antes ou depois:

```json
{
  "dimensao": "banco",
  "itens": [
    {
      "id": "B1",
      "resposta": "atende",
      "evidencias": [
        {"arquivo": "banco.json", "linha": null, "o_que_mostra": "as 6 tabelas têm PRIMARY KEY"},
        {"arquivo": "src/main/java/com/loja/Order.java", "linha": 18, "o_que_mostra": "@Id no pedido"}
      ],
      "observacao": ""
    }
  ],
  "resumo": "duas ou três frases sobre a dimensão como um todo",
  "duvidas": ""
}
```

A `dimensao` é a das instruções abaixo. A `linha` é `null` quando a evidência é um material e não um arquivo de
código. O `resumo` não muda nenhuma resposta: é só a leitura geral, para quem for revisar a correção.

---

# A dimensão: a cobertura e a qualidade dos testes

`"dimensao": "testes"`. O enunciado **não pede testes**: só diz que "o projeto precisa funcionar com `mvn verify`"
num computador "sem banco de dados nem Docker". Escrever testes, e quais, foi decisão do agente.

Além do enunciado e do projeto, você recebe os números da **medida por mutação** (`testes.json`), feita por uma
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
conferem. Isso é o que você julga, lendo `projeto/src/test`. Quando a `situation` não for `ok`, julgue os testes
pelo código, e diga na `observacao` que os números não existem.

## Os itens

**`T1`: o que dos requisitos do enunciado os testes cobrem?** Uma resposta por área, no formato
`"area": "valor"` dentro da `observacao`, e o valor do item resume:

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
