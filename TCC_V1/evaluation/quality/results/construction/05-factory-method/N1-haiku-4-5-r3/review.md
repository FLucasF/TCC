# Revisão: construção, cenário 05

## Visão geral

O projeto tem cinco pacotes: `api` (os handlers do Javalin, que montam o JSON à mão com Gson), `service` (preço,
situações da reserva, documentos e relatórios), `infrastructure` (JDBC puro, com o esquema criado por
`CREATE TABLE IF NOT EXISTS` no código), `domain` (classes de dados) e a classe `Application`, que registra as rotas.
Tudo é `static`: uma requisição vai de `Application.java:29-42` para um método estático do controller, que chama o
`ReservationService` e os repositórios (estáticos também), que usam uma única `Connection` estática
(`Database.java:6`). O defeito mais sério é que **o sistema não responde a nenhuma chamada JSON**: o `pom.xml` não traz
o `jackson-databind`, e o `Application.java:26` liga o `JavalinJackson`. Por isso todo `ctx.json(...)` dá 500 ("It looks
like you don't have an object mapper configured"), inclusive as respostas de erro. Com isso, `GET /rooms`, a criação,
a consulta, a confirmação e o documento quebram, e o roteiro de comportamento passa em só 6 de 83 conferências. A
reserva é gravada no banco mesmo quando a resposta é 500 (conferi à mão: o `POST /reservations` devolveu 500, e a
tabela ficou com a reserva 1 PENDING). Os testes, só de unidade, não pegaram nada disso. Há também defeitos de regra:
a disponibilidade ignora as reservas PENDING, e a confirmação exige um `amountPaid` vindo do cliente.

## Arquitetura

**Números:** 5 pacotes (`com.mareaalta`, `api`, `domain`, `infrastructure`, `service`), 8 dependências entre eles,
sem ciclo. O `service` usa JDBC, e o `api` usa Javalin e Gson. A maior classe é `ReservationController` (243 linhas).
**Nota:** com problemas

As camadas existem, e as dependências apontam no sentido certo (api → service → infrastructure → domain, sem ciclo).
O SQL fica todo em `infrastructure`, e as regras não conhecem o Javalin. Mas a separação vaza em vários pontos:

- **Os controllers vão direto ao banco**, passando por cima do serviço: `ReservationController.java:149` e `:185`
  chamam `RoomRepository.findByNumber`, e `RoomController.java` e `MessageController.java:11` chamam os repositórios
  direto.
- **Há uma regra de negócio na borda HTTP:** qual documento é (o título "Comprovante"/"Fatura"/"Booking receipt") é
  decidido no controller, em `ReservationController.java:223-224`, com o mesmo `if` sobre o tipo de hóspede que o
  serviço já faz em `ReservationService.java:148-157`. Os dados do documento também são montados no controller
  (`ReservationController.java:222-235`).
- **A validação de entrada e a regra de identificação** (exatamente um jeito de se identificar) estão no handler
  (`ReservationController.java:49-78`). A regra "mínimo 1 hóspede", por outro lado, está no serviço
  (`ReservationService.java:29`).
- **Não há montagem:** não existe um lugar que crie e ligue as peças. Tudo é método `static`, e o estado global fica
  em `Database.java:6-9` (`static Connection`). O serviço declara `throws SQLException` (`ReservationService.java:18`),
  e por isso o jdeps mostra o `service` dependendo do JDBC.

São misturas localizadas, num projeto que ainda tem a forma de camadas.

## Design de baixo nível

**Números:** 3 tipos de documento. A escolha do texto está num método só (`ReservationService.generateDocumentText`),
chamado nos 3 lugares (confirmação `ReservationService.java:88`, check-out `ReservationController.java:150`, segunda
via `ReservationController.java:186`). A escolha do título se repete fora dele (`ReservationController.java:223`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** Algumas coisas aproximam o código de uma fábrica concentrada:

- `ReservationService.generateDocumentText` (`ReservationService.java:148-157`) escolhe pelo tipo de hóspede qual
  gerador chamar;
- os três lugares onde o documento sai passam por ele, e nenhum deles monta o texto à mão.

Outras afastam, e por isso a classe é `malfeito` e não `acerto`:

- **A fábrica só devolve o texto.** O documento que vai para a API (o `DocumentResponse`, com o `title`) é montado
  depois, fora dela, em `ReservationController.java:222-235`. Lá, o tipo de documento é decidido de novo, com outro
  `if` encadeado sobre `guestCpf`/`companyCnpj` (`ReservationController.java:223-224`). Para acrescentar o documento
  da agência de turismo, é preciso mexer nos dois `if`s, que podem divergir.
- **O tipo de hóspede não existe como tipo.** São cinco `String` anuláveis na `Reservation` (`Reservation.java:11-15`),
  e a variação é descoberta testando qual delas não é nula. Quando nenhuma é, a fábrica devolve `""` em silêncio
  (`ReservationService.java:156`).
- **Os três geradores são cópias quase inteiras uns dos outros** (`DocumentGenerator.java:13-32`, `:34-58`, `:60-80`).
  As linhas de quarto, datas, diárias, desconto e total se repetem.

**Exagero:** nenhum. Não há interface, fábrica ou camada sobrando: `contenção`.

**O resto do código:**

- **`static` em tudo, com estado mutável global:** a `Connection` compartilhada (`Database.java:6`, `:19-24`) é
  devolvida a todas as threads e fechada por cada repositório no `try-with-resources` (por exemplo
  `ReservationRepository.java:18`).
- **`catch (Exception)` engole qualquer erro e responde 400 INVALID_REQUEST** em todos os handlers
  (`ReservationController.java:93`, `:105`, `:126`, `:139`, `:157`, `:170`, `:192`, `ReportController.java:25`, `:42`).
  Uma falha de SQL vira "pedido inválido", e a causa se perde. O 404/409 é deduzido comparando strings do código de
  erro (`ReservationController.java:89-91`, `:124`).
- **Os tipos errados:**
  - o plano de tarifa entra no preço como `String` (`PricingService.java:41`), mesmo havendo o enum `RatePlan`;
  - o dinheiro vira `double` no JSON (`ReservationController.java:213-218`, `RoomController.java:23`), e por isso
    249.90 sai `249.9`;
  - a formatação usa `String.format("%.2f")`, que depende do locale da máquina (`DocumentGenerator.java:91`,
    `ReportService.java:87`). Num locale pt_BR, o `split("\\.")` de `DocumentGenerator.java:92` quebra.
- **A regra no lugar errado:** o valor pago vem do corpo da confirmação (`ReservationController.java:115-121`), em vez
  de ser o total da reserva, como o texto diz.
- **`null` sem cuidado:** `companyCnpj` sem `companyName` dá `NullPointerException` (`ReservationController.java:65`).
  Por acaso, isso acaba no 400 pelo `catch` genérico.
- **A ordem das validações está trocada:** a disponibilidade é checada antes de "hóspedes < 1" e da capacidade
  (`ReservationService.java:25-35`).

São defeitos espalhados pelas três camadas, e não um ponto só.

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados; 14 métodos escritos em 2 arquivos. Cobertura: linhas
112 de 750 (14,9%), ramos 26 de 204 (12,7%). `api` 0 de 217 linhas, `infrastructure` 0 de 188, `service` 74 de 214.
Nenhum teste pela API nem contra o banco.
**Nota:** ruim

**O que está testado, com valores:**

- `PricingServiceTest.java` confere a alta temporada nas bordas (14 e 15/12, `:16-17`) e a diária com 20% (299.88,
  `:39`);
- o subtotal de 599.76 (`:58`), o desconto de 116.97 (`:65`) e o total (`:73`).

**O que não está:**

- **A parte nova do cenário só tem asserções fracas.** `DocumentGeneratorTest.java` usa `contains` em pedaços do texto.
  O valor do ISS nunca é conferido: só o rótulo "ISS retido (5%)" (`DocumentGeneratorTest.java:58`). Se alguém
  trocasse `0.05` por `0.50` (`DocumentGenerator.java:39`), nenhum teste falharia. O mesmo vale para apagar a linha
  "Desconto"/"Discount", para trocar "1 night" por "1 nights" e para o milhar do *booking receipt*: este já está errado
  (`DocumentGenerator.java:109` escreve `BRL 2783.52`, sem a vírgula) e nenhum teste o pega.
- **O que falta testar:**
  - a escolha do documento por tipo de hóspede (`generateDocumentText`);
  - a regra "exatamente um jeito de se identificar";
  - as situações da reserva, o cancelamento e a devolução;
  - a disponibilidade, os códigos de erro e os relatórios.
- **Sem nenhum teste que suba o serviço**, a falta do mapeador JSON, que derruba o sistema inteiro, passou pelo
  `mvn verify` sem ser notada.

Com 15% de cobertura, nada da API nem do banco, e uma regra da parte nova sem conferir, os testes não protegem o que
foi pedido.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. Conferências que acusaram algo:

- a chave estrangeira `reservations(room_number) → rooms` não tem índice;
- não há CHECK nas colunas de situação, plano e tipo, nem nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- não há pool, há 2 `DriverManager.getConnection`, 0 transações, 0 `FOR UPDATE`;
- não há migrações: o esquema é criado no Java.

**Nota:** ruim

**O que está certo:**

- tipos certos para dinheiro e datas;
- `NOT NULL` no que importa;
- chave estrangeira da reserva para o quarto (`Database.java:55`);
- SQL sempre com parâmetros;
- os quartos semeados com `ON CONFLICT DO NOTHING` (`RoomRepository.java:18`);
- os dados sobrevivem ao reinício.

**O que pesa:**

- **Um quarto pode ter duas reservas na mesma noite, mesmo sem concorrência.** A consulta de disponibilidade só conta
  CONFIRMED, CHECKED_IN e CHECKED_OUT (`ReservationRepository.java:79`). Duas reservas PENDING para o mesmo quarto e as
  mesmas noites são aceitas, embora o texto diga que só as canceladas ficam de fora. Depois, as duas podem ser
  confirmadas, porque a confirmação não volta a checar a disponibilidade (`ReservationService.java:75-86`).
- **Não há nada contra a corrida:** nenhuma restrição (EXCLUDE), trava, transação ou sequer `synchronized` entre a
  checagem (`ReservationService.java:25`) e o `INSERT` (`ReservationService.java:64`).
- **Uma única `Connection` estática é compartilhada por todas as threads** do Jetty (`Database.java:6`, `:19-24`), e
  cada repositório a fecha ao terminar (`try (Connection conn = Database.getConnection()...` em
  `ReservationRepository.java:18`, `MessageRepository.java:14`, `RoomRepository.java:12`). Com duas requisições ao
  mesmo tempo, uma fecha a conexão que a outra está usando.
- **Escritas sem transação:** a confirmação faz o `UPDATE` da reserva e o `INSERT` da mensagem em separado
  (`ReservationService.java:85` e `:94`).
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` no código (`Database.java:39-89`), sem migrações, não sabe
  acrescentar uma coluna numa tabela que já existe.
- **Menores:**
  - `amount_paid` aceita nulo (`Database.java:72`);
  - o relatório de ocupação faz uma consulta por quarto (`ReportService.java:20-21`);
  - as mensagens saem em ordem decrescente de criação (`MessageRepository.java:31`).

## Comportamento

**Números:** 6 de 83 conferências passaram (77 falharam):

| parte | passaram |
|---|---|
| a parte comum | 4 de 46 |
| depois de reiniciar | 1 de 3 |
| o cenário 05 | 0 de 32 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**Por que quase tudo falhou.** O serviço sobe, mas toda resposta JSON dá 500, porque não há mapeador JSON (sem
`jackson-databind` no `pom.xml`, com `JavalinJackson` em `Application.java:26`; a mensagem do Javalin aparece na própria
conferência "dinheiro no JSON"). Na prática, o site não consegue:

- listar os quartos;
- criar, consultar, confirmar, cancelar ou fazer o check-in/check-out de uma reserva;
- pegar o documento.

Nem as recusas funcionam, porque o corpo de erro também é JSON: tudo vira 500. Só respondem os dois relatórios em CSV,
que saem vazios porque nenhuma reserva chegou a ser confirmada.

**Para o dono da pousada, é pior que um sistema parado.** O `POST /reservations` grava a reserva e só depois quebra
(conferi à mão: 500 na resposta, reserva 1 PENDING no banco). O site acha que falhou e tenta de novo, e o banco vai
acumulando reservas pendentes que ninguém vê. O OK de "nenhuma rodada com as duas reservas criadas" é enganoso: as 20
respostas foram 500, e pelo código (`ReservationRepository.java:79` ignora PENDING) as duas reservas de cada rodada
foram gravadas. Esse defeito está contado na coluna do banco.

**O que falharia mesmo com o JSON consertado**, pela leitura:

- a confirmação exige `amountPaid` no corpo e responde 400 sem ele (`ReservationController.java:115`), quando o texto
  diz que o pago é o total;
- o *booking receipt* sai sem o separador de milhar (`BRL 2783.52`, `DocumentGenerator.java:109`);
- o financeiro lista as reservas canceladas que não deixaram dinheiro (amount 0) (`ReservationRepository.java:125`,
  `ReportService.java:71-84`);
- as datas do documento JSON saem como `10/03/2027` (`ReservationController.java:231`), ponto que o texto deixava em
  aberto.

A parte nova (os três documentos) não pôde ser vista em nenhuma chamada.

## Dúvidas

- **A classe do pattern:** fiquei entre `malfeito` e `acerto` para uma "fábrica concentrada". O texto do documento é
  escolhido num método só, `ReservationService.generateDocumentText`, e os três lugares passam por ele. Mas o título do
  documento é escolhido de novo fora, no controller. Fiquei com `malfeito` por essa segunda escolha.
- **A nota dos testes:** fiquei entre `com problemas` e `ruim`. Os testes de preço conferem valores, mas a cobertura
  baixa, a ausência de qualquer teste da API e do banco e o ISS sem conferir pesaram para `ruim`.
