# A dimensão: a arquitetura

A correção vai em `correcao/arquitetura.md`. Além do enunciado e do projeto, você lê o **relatório de arquitetura**
(`architecture/architecture-report.txt`), lido por uma ferramenta (ArchUnit) nas classes compiladas do projeto. Ele traz fatos, não
notas:

- os pacotes logo abaixo do pacote raiz, com quantas classes cada um tem;
- as dependências entre pacotes, cada uma com as dependências de classe que a fazem (`a.X -> b.Y`: a classe `X` do
  pacote `a` usa a classe `Y` do pacote `b`);
- os ciclos: cada grupo de pacotes que dependem uns dos outros em círculo, com as dependências de dentro do grupo;
- as classes de fora da camada web que dependem de um controller, e as que usam a API da web de entrada (as
  anotações e os tipos do Spring MVC, o `ResponseEntity`).

O relatório é confiável no que diz (é lido do código compilado) e é a fonte para os itens de dependência. O que
ele **não** diz é se uma dependência é um problema: isso é o que você julga, abrindo as classes que ele cita.

## Os itens

**`A1`: a regra de negócio não conhece a web?** A regra de negócio (o cálculo, o ciclo do pedido, o estoque) não
deveria depender de nada que só existe porque o serviço é chamado por HTTP: os tipos do Spring MVC, o
`ResponseEntity`, os códigos HTTP, **e os objetos de entrada e saída da própria API do projeto** (os records do
corpo da requisição e da resposta, quando moram com os controllers). O relatório pega os tipos do Spring; os
objetos da API do projeto você acha nas dependências entre pacotes (por exemplo, um serviço que recebe o
`PedidoRequest` do pacote dos controllers).

| valor | quando |
|---|---|
| `atende` | a regra de negócio recebe e devolve tipos próprios; a tradução de e para HTTP fica na camada web |
| `nao_atende` | alguma classe de regra de negócio usa um tipo da web ou um objeto da API do projeto; cite cada uma |

**`A2`: os ciclos entre pacotes, quando há, são acoplamento real?** Para cada grupo de pacotes em ciclo do
relatório, abra as dependências de classe que fecham o círculo.

| valor | quando |
|---|---|
| `sem_ciclos` | o relatório não tem nenhum grupo |
| `leves` | todo ciclo fecha por um **tipo de dado** compartilhado (um record, um enum, um evento) que poderia mudar de pacote sem mudar o desenho |
| `estruturais` | algum ciclo fecha por **comportamento**: serviços ou regras de pacotes diferentes chamam uns aos outros nos dois sentidos (inclusive por listeners que se ouvem nos dois sentidos) |

Na observação, diga para cada grupo por qual dependência ele fecha.

**`A3`: o controller só traduz?** O controller deveria receber a requisição, chamar a regra de negócio e
traduzir o resultado (ou a recusa) para HTTP, sem decidir nada do negócio.

| valor | quando |
|---|---|
| `atende` | os controllers não têm regra de negócio |
| `nao_atende` | algum controller faz conta, decide uma recusa do negócio, muda a situação do pedido ou mexe no estoque; cite onde |

**`A4`: o acesso ao banco está isolado?** A regra de negócio deveria falar com os dados por repositórios (Spring
Data, um DAO, uma interface própria), sem montar SQL nem usar JDBC no meio da regra.

| valor | quando |
|---|---|
| `atende` | o SQL e o JDBC, quando existem, ficam em classes de acesso a dados |
| `nao_atende` | alguma classe de regra de negócio monta SQL ou usa JDBC direto; cite onde |

**`A5`: um cálculo só para o resumo e para o pedido?** O enunciado pede que o resumo antes de confirmar e o do
pedido registrado deem exatamente os mesmos valores ("num sistema antigo, já aconteceu de o resumo mostrar um valor
e o pedido registrar outro").

| valor | quando |
|---|---|
| `atende` | o resumo e o registro do pedido chamam o mesmo cálculo |
| `nao_atende` | há duas cópias do cálculo, inteiras ou em parte (uma etapa refeita só num dos fluxos) |

**`A6`: como os pacotes estão organizados?** Descritivo, não é bom nem ruim: `por_funcionalidade` (um pacote por
assunto: pedido, catálogo, cupom), `por_camada` (controller, service, repository), `misto`, ou `plano` (quase tudo
num pacote só).
