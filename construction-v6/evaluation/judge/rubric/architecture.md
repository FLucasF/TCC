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
