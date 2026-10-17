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

# A dimensão: a qualidade do banco

`"dimensao": "banco"`. A pergunta é se o banco que o serviço cria segue as boas práticas de banco relacional:
as ligações entre as entidades, os relacionamentos, a organização e as garantias. Não existe um esquema certo
a ser encontrado. Cada item é uma prática publicada, com a fonte ao lado.

Além do enunciado e do projeto, você recebe a **foto do esquema** (`banco.json`): o que o serviço realmente criou
no PostgreSQL quando subiu, lido do catálogo do banco depois que a suíte de testes rodou. Ela tem:

| chave | o que é |
|---|---|
| `tables` | as tabelas |
| `columns` | cada coluna: a tabela, o nome, o tipo com o tamanho (`numeric(12,2)`, `character varying(255)`), se aceita nulo (`nullable`) e o valor padrão |
| `constraints` | cada restrição: a tabela, o nome, o tipo (`p` chave primária, `f` chave estrangeira, `u` única, `c` CHECK) e a definição |
| `indexes` | cada índice: a tabela, o nome, a definição e se é único |

A foto é a fonte para o que o banco **tem**: ela vale mais que as entidades e as migrações, que dizem o que o
código **pede**. Use o código para entender por quê (uma entidade JPA, um script de migração). Se não houver
foto (`banco.json` diz que o serviço não subiu), responda pelo código (as migrações e as entidades) e diga isso na
`observacao` de cada item.

As tabelas das ferramentas de migração (`flyway_schema_history`, `databasechangelog`) não são do domínio: não
entram nos itens, a não ser no `B9`.

## Os itens

**`B1`: toda tabela tem chave primária?** Fonte: documentação do PostgreSQL, 5.5.4 (*Primary Keys*).
`atende` ou `nao_atende` (cite as tabelas sem chave).

**`B2`: as ligações do domínio são chaves estrangeiras?** Fonte: documentação do PostgreSQL, 5.5.5 (*Foreign
Keys*). Tire as ligações do enunciado: o pedido é de um cliente; o item é de um pedido e é de um produto; o aviso
de pagamento é de um pedido; o aviso ao cliente é de um pedido e de um cliente. Para cada ligação que o esquema
guarda (uma coluna que aponta para outra tabela), confira se há a chave estrangeira.

| valor | quando |
|---|---|
| `atende` | toda ligação guardada tem a chave estrangeira |
| `parcial` | alguma ligação guardada não tem (uma coluna `order_id` sem `FOREIGN KEY`); cite cada uma |
| `nao_atende` | nenhuma ligação tem chave estrangeira |

Uma ligação que o esquema não guarda em coluna nenhuma (o item guardado como JSON dentro do pedido, por exemplo)
não é chave faltando: é o `B8`.

**`B3`: as chaves estrangeiras têm índice?** Fonte: documentação do PostgreSQL, 5.5.5: a chave estrangeira
**não** cria índice nas colunas que apontam, e "costuma ser uma boa ideia" indexá-las. Um índice (ou uma chave
primária, ou uma restrição única) cuja **primeira** coluna é a da chave estrangeira conta.

| valor | quando |
|---|---|
| `atende` | toda chave estrangeira tem índice |
| `parcial` | alguma não tem; cite cada uma |
| `nao_atende` | nenhuma tem |
| `nao_se_aplica` | não há chave estrangeira |

**`B4`: o dinheiro é guardado num tipo exato?** Fontes: documentação do PostgreSQL, 8.1.3 (*Floating-Point
Types*: "se você precisa de armazenamento e cálculo exatos, como valores em dinheiro, use o tipo `numeric`"), e a
wiki do PostgreSQL, *Don't Do This* ("Don't use money").

| valor | quando |
|---|---|
| `atende` | todo valor em dinheiro está em `numeric` (ou em centavos, num inteiro) |
| `nao_atende` | algum está em `real`, `double precision` ou `money`; cite as colunas |

**`B5`: as datas e horas guardam o fuso?** Fonte: wiki do PostgreSQL, *Don't Do This* ("Don't use timestamp
(without time zone)"). O enunciado usa instantes com fuso (`2026-10-14T15:30:00Z`) e a regra das 48 horas.

| valor | quando |
|---|---|
| `atende` | os instantes estão em `timestamp with time zone` (ou num tipo que guarda o instante sem ambiguidade, como um inteiro em UTC, explicado no código) |
| `nao_atende` | algum instante está em `timestamp without time zone`; cite as colunas |
| `nao_se_aplica` | o banco não guarda nenhum instante |

**`B6`: o que é obrigatório é `NOT NULL`?** Fonte: documentação do PostgreSQL, 5.5.2 (*Not-Null Constraints*).
Obrigatório é o que o enunciado diz que todo pedido, item, produto, cliente ou aviso tem (o CPF, a situação, a
data de registro, os valores do resumo, o SKU, a quantidade, o saldo). O que só existe depois (o código de
rastreio antes do envio, o id da transação antes do pagamento) pode ser nulo.

| valor | quando |
|---|---|
| `atende` | as colunas obrigatórias não aceitam nulo |
| `parcial` | algumas aceitam; cite as colunas |
| `nao_atende` | quase nenhuma obrigatória é `NOT NULL` |

**`B7`: as regras invariáveis do negócio estão garantidas no próprio banco?** Fonte: documentação do PostgreSQL,
5.5.1 (*Check Constraints*) e 5.5.3 (*Unique Constraints*). O enunciado tem três regras que valem sempre, não
importa o caminho do código: "o estoque nunca pode ficar negativo"; "o mesmo aviso de pagamento repetido não pode
baixar o estoque duas vezes, nem dar o crédito duas vezes"; e a quantidade do item é positiva. Para cada uma,
diga na `observacao` se o banco a garante (um `CHECK`, uma chave primária ou única com o id da transação) ou se só
o código cuida dela.

| valor | quando |
|---|---|
| `atende` | o banco garante as três |
| `parcial` | garante uma ou duas |
| `nao_atende` | não garante nenhuma: tudo fica só no código |

Uma trava no código que funciona continua sendo uma trava só no código. Isto não diz que o serviço erra (a suíte
mede isso): diz se o banco também protege a regra.

**`B8`: há dado repetido entre tabelas sem motivo?** Fonte: as formas normais (Elmasri & Navathe, *Fundamentals of
Database Systems*, os capítulos de normalização). Repetir um dado que muda (o preço atual do produto, o e-mail do
cliente) em outra tabela cria duas versões que podem divergir.

**Atenção ao que o enunciado pede guardar:** o resumo do pedido "foi calculado quando o pedido foi registrado e não
muda mais". Guardar no pedido os valores do resumo, o preço de cada item na hora da compra e os dados do cliente
daquela compra é uma **foto do momento**, pedida pelo negócio, e não repetição sem motivo. Guardar uma lista
inteira (os itens, os avisos) como texto ou JSON dentro de uma coluna esconde a ligação do banco: isso conta
aqui.

| valor | quando |
|---|---|
| `atende` | não há repetição sem motivo |
| `nao_atende` | há; cite as colunas e diga por que a repetição não é uma foto do momento |

**`B9`: como o esquema é criado?** Fonte: documentação do Spring Boot, *Database Initialization* ("é recomendado
usar um mecanismo só para gerar o esquema"; o Flyway e o Liquibase são as "ferramentas de migração de mais alto
nível"). Veja o `pom.xml`, o `application.properties`/`.yml` (`spring.jpa.hibernate.ddl-auto`), os arquivos
`schema.sql` e as pastas de migração (`db/migration`).

| valor | quando |
|---|---|
| `migracao_versionada` | só o Flyway ou só o Liquibase cria o esquema |
| `ddl_auto` | o Hibernate gera o esquema a partir das entidades (`ddl-auto` `update` ou `create`) |
| `script_sql` | um `schema.sql` ou um script próprio, sem ferramenta de migração |
| `mais_de_um` | dois mecanismos ao mesmo tempo (por exemplo, o Flyway e o `ddl-auto=update`): é o que a documentação desaconselha |

**`B10`: os textos têm tamanho por padrão?** Fonte: wiki do PostgreSQL, *Don't Do This* ("Don't use varchar(n) by
default", "Don't use char(n)"). Este é um item menor: o `character varying(255)` é o padrão do JPA quando o
agente não escolhe o tipo.

| valor | quando |
|---|---|
| `atende` | os textos são `text`, ou têm o tamanho escolhido por um motivo do domínio (o CPF com 11, o CEP com 8) |
| `nao_atende` | os textos têm um tamanho arbitrário (o 255 em todos) ou usam `char(n)` |
