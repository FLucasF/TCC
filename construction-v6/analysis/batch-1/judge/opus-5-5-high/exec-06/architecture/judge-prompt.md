# A correção de um serviço

Este guia é para a IA que corrige **uma dimensão de uma execução** da construção: o serviço de pedidos de uma loja
online, em Java com Spring Boot e PostgreSQL, que um agente de programação escreveu do zero a partir de um
enunciado. A régua da dimensão vem logo depois deste guia: `architecture.md`, `tests.md`, `database.md` ou
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

# A dimensão: a arquitetura

Além do enunciado e do projeto, você lê o **relatório de arquitetura**
(`architecture/architecture-report.txt`), lido por uma ferramenta (ArchUnit) nas classes compiladas do projeto.
Ele traz fatos, não notas:

- os pacotes logo abaixo do pacote raiz, com quantas classes cada um tem;
- as dependências entre pacotes, cada uma com as dependências de classe que a fazem (`a.X -> b.Y`: a classe `X` do
  pacote `a` usa a classe `Y` do pacote `b`);
- os ciclos: cada grupo de pacotes que dependem uns dos outros em círculo, com as dependências de dentro do grupo;
- as classes de fora da camada web que dependem de um controller, e as que usam a API da web de entrada (as
  anotações e os tipos do Spring MVC, o `ResponseEntity`).

O relatório é confiável no que diz (é lido do código compilado) e é a fonte para as dependências. O que ele **não**
diz é se uma dependência é um problema: isso é o que você julga, abrindo as classes que ele cita.

## Os itens

**A1, a regra de negócio não conhece a web?** A regra de negócio (o cálculo, o ciclo do pedido, o estoque) não
deveria depender de nada que só existe porque o serviço é chamado por HTTP: os tipos do Spring MVC, o
`ResponseEntity`, os códigos HTTP, **e os objetos de entrada e saída da própria API do projeto** (os records do
corpo da requisição e da resposta, quando moram com os controllers). O certo é a regra de negócio receber e devolver
tipos próprios, com a tradução de e para HTTP na camada web. O relatório pega os tipos do Spring; os objetos da API
do projeto você acha nas dependências entre pacotes (por exemplo, um serviço que recebe o `PedidoRequest` do pacote
dos controllers). Cite cada classe de negócio que conhece a web. A pergunta é de sim ou não: **uma classe de
negócio que conhece a web já basta para `não segue`**, mesmo que todo o resto esteja limpo.

**A2, os ciclos entre pacotes são acoplamento real?** Para cada grupo de pacotes em ciclo do relatório, abra as
dependências de classe que fecham o círculo e diga por qual dependência ele fecha. Um ciclo que fecha por um **tipo
de dado** compartilhado (um record, um enum, um evento), que poderia mudar de pacote sem mudar o desenho, é leve. Um
ciclo que fecha por **comportamento** (serviços ou regras de pacotes diferentes chamando uns aos outros nos dois
sentidos, inclusive listeners que se ouvem nos dois sentidos) é estrutural, e é o que pesa. Se o relatório não tem
nenhum grupo, diga isso. A conclusão vem do pior laço: `segue` sem ciclo, `em parte` quando todos os ciclos fecham
por tipo de dado, e `não segue` quando há um laço que fecha por comportamento (**um só já basta**, mesmo que o resto
do grupo seja leve).

**A3, o controller só traduz?** O controller deveria receber a requisição, chamar a regra de negócio e traduzir o
resultado (ou a recusa) para HTTP, sem decidir nada do negócio. É problema um controller fazer conta, decidir uma
recusa do negócio, mudar a situação do pedido ou mexer no estoque. Cite onde.

**A4, o acesso ao banco está isolado?** A regra de negócio deveria falar com os dados por repositórios (Spring Data,
um DAO, uma interface própria), com o SQL e o JDBC, quando existem, em classes de acesso a dados. É problema uma
classe de regra de negócio montar SQL ou usar JDBC direto. Cite onde.

**A5, um cálculo só para o resumo e para o pedido?** O enunciado pede que o resumo antes de confirmar e o do pedido
registrado deem exatamente os mesmos valores ("num sistema antigo, já aconteceu de o resumo mostrar um valor e o
pedido registrar outro"). O certo é os dois fluxos chamarem o mesmo cálculo. É problema haver duas cópias do
cálculo, inteiras ou em parte (uma etapa refeita só num dos fluxos).

**A6, como os pacotes estão organizados?** Descritivo, não é bom nem ruim: por funcionalidade (um pacote por
assunto: pedido, catálogo, cupom), por camada (controller, service, repository), misto, ou quase tudo num pacote só.
