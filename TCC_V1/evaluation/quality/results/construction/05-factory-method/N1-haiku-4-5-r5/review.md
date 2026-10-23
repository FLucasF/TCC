# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto Maven com 7 pacotes (`handler`, `service`, `repository`, `model`, `db`, `util` e o `Main`).
Uma requisição entra numa rota declarada em `Main.java:20-35` e vai para um handler estático (`handler/*Handler.java`).
O handler lê o JSON com Gson, chama um serviço estático (`ReservationService`, `ReportService`, `DocumentGenerator`),
e o serviço chama repositórios estáticos com JDBC puro (`DriverManager` em `db/Database.java:25`). Os dados ficam em
três tabelas que o próprio código cria com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:28-76`). O problema mais
sério é que **o serviço não sobe**. As rotas usam a sintaxe `:id` do Javalin 3 (`Main.java:23-28`), e o Javalin 6.7.0
recusa essa sintaxe no arranque: `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from
':param' to '{param}'` (`measures/database.md`, `measures/behavior.md`). Por isso nenhuma chamada da API funciona, e
nenhum teste pegou o erro, porque não há testes. A leitura mostra outros defeitos que apareceriam mesmo com as rotas
corrigidas:

- o jar não tem Jackson, só Gson (`pom.xml`; `jar tf` não acha `ObjectMapper`), e todas as respostas usam `ctx.json`;
- a confirmação exige um `amountPaid` no corpo em vez de usar o total da reserva (`ReservationHandler.java:132-137`);
- a coluna `guest_country` é `VARCHAR(2)` (`Database.java:50`), e "United Kingdom" não cabe nela.

## Arquitetura

**Números:** 7 pacotes, 10 dependências entre eles, nenhum ciclo. O `handler` depende de 4 pacotes (`model`,
`repository`, `service`, `util`) e usa Javalin e JDBC. O `service` usa JDBC (`SQLException`). Nenhum pacote usa pool.
A maior classe é `ReservationHandler` (291 linhas).
**Nota:** com problemas

As camadas existem e as dependências apontam na direção certa (handler → service → repository → db), sem ciclo. O SQL
fica isolado nos repositórios (`repository/ReservationRepository.java`, `RoomRepository.java`, `MessageRepository.java`).
A separação vaza em alguns lugares:

- **Regra no handler.** O handler decide quando a segunda via existe (`handler/ReservationHandler.java:202-205`). No
  check-out e na segunda via, é o handler que busca o quarto no repositório e monta o documento
  (`ReservationHandler.java:184-185` e `207-208`), enquanto na confirmação isso fica no serviço
  (`service/ReservationService.java:104-105`). A mesma operação mora em duas camadas.
- **Handler que pula o serviço.** O handler vai direto ao repositório (`ReservationHandler.java:6`, `RoomHandler.java`,
  `MessageHandler.java`) e trata `SQLException` em todo método. Assim o JDBC aparece na borda HTTP, como mostra a
  tabela de bibliotecas do jdeps.
- **Montagem espalhada.** Não há um lugar onde as peças são criadas e ligadas. Tudo é `static`: os handlers chamam
  `ReservationService.create(...)`, o serviço chama `ReservationRepository.save(...)`, e o repositório chama
  `Database.getConnection()`, que lê as variáveis de ambiente em campos estáticos (`db/Database.java:10-12`). Nada pode
  ser substituído num teste, e cada chamada abre uma conexão nova sem pool.
- **Handler grande.** `ReservationHandler` (291 linhas) concentra a validação do corpo inteiro num método de cerca de
  90 linhas (`ReservationHandler.java:21-110`).

## Design de baixo nível

**Números:** há 3 tipos de documento (comprovante, fatura, booking receipt). Os 3 são escolhidos num lugar só
(`service/DocumentGenerator.java:21-36`). Os 3 lugares que emitem o documento chamam essa fábrica e não montam nenhum
documento à mão: a confirmação/e-mail (`ReservationService.java:105`), o check-out (`ReservationHandler.java:185`) e a
segunda via (`ReservationHandler.java:208`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma é a de uma fábrica concentrada: `DocumentGenerator.generate(reservation, room)` decide pelo tipo
de hóspede qual documento montar (`DocumentGenerator.java:21-36`) e devolve um `Document` com título, campos e texto.
Os três pontos de emissão só pedem o documento a ela. O e-mail reaproveita `document.text()` e continua sempre em
português (`ReservationService.java:161-162`, `174-180`). Uma agência de turismo seria mais um ramo e um método
`generateX`, sem mexer em quem emite. Há dois pontos que afastam a solução do ideal:

- O tipo de hóspede não existe como tipo. A escolha é feita pela presença de campos que podem ser nulos
  (`guestCpf() != null`, `companyCnpj() != null`), e o `else` final trata qualquer outro caso como estrangeiro
  (`DocumentGenerator.java:21-31`). Um quarto jeito de identificar o hóspede cairia no booking receipt sem aviso.
- As três funções de texto repetem quase as mesmas linhas (`DocumentGenerator.java:59-126`).

Nada disso põe a escolha fora da fábrica, por isso a classe é acerto.

**O exagero.** Não há interfaces com uma implementação só, builders nem camadas que só repassam: a classe é contenção.

**O resto do código.** Os defeitos estão em vários arquivos:

- **Erros como texto.** As regras lançam `IllegalArgumentException("ROOM_NOT_FOUND")` e similares
  (`ReservationService.java:25`, `29`, `33`, `42`, `86`), e o handler converte a mensagem em status com um `switch`
  sobre strings (`ReservationHandler.java:220-243`). O `default` transforma qualquer `IllegalArgumentException`
  inesperada em 400.
- **Exceções engolidas.** O `catch (Exception e)` devolve 400 e esconde qualquer falha como requisição inválida
  (`ReservationHandler.java:107-109`, `146-148`).
- **Duplicação.**
  - Os mesmos quatro `catch` estão copiados em seis métodos do handler.
  - `Main.jsonMap` (`Main.java:41-47`) é cópia de `JsonUtils.map` (`util/JsonUtils.java:7-13`).
  - A lista de 20 colunas da reserva aparece quatro vezes em `ReservationRepository.java` (`76-77`, `94-95`,
    `116-117`, `141-142`).
  - O `Reservation` tem 20 campos posicionais, e os três `with*` copiam o construtor inteiro
    (`model/Reservation.java:24-48`).
- **Tipos fracos.**
  - O plano de tarifa vira `String` em `PricingUtils.calculateDiscount(subtotal, ratePlan.name())`
    (`ReservationService.java:54`; `PricingUtils.java:41-46`).
  - `calculateSubtotal` nunca é usado (`PricingUtils.java:37-39`).
  - Na receita de cancelada, o ramo `else` repete o que já estava atribuído (`ReportService.java:57-63`).
- **Formatação que depende do idioma da máquina.** O dinheiro é formatado com `String.format("%,.2f")` e
  `String.format("%.2f")`, que usam o locale padrão (`DocumentGenerator.java:129`, `133`; `ReportService.java:85`).
  Numa máquina em pt_BR, o CSV sairia `599,76` e o comprovante trocaria os separadores.
- **Uso correto dos tipos do domínio.** `BigDecimal` com `HALF_EVEN` a cada passo (`PricingUtils.java:32-34`, `41-50`)
  e `LocalDate` para datas.

## Testes

**Números:** 0 testes rodaram (0 falhas). Há 0 métodos de teste escritos e não existe `src/test`. Não há relatório de
cobertura de linhas nem de ramos: o JaCoCo não teve o que medir (`measures/tests.md`). O `mvn verify` termina em
BUILD SUCCESS só porque não há nada para rodar.
**Nota:** ruim

O agente declarou o JUnit no `pom.xml`, mas não escreveu nenhum teste. Nenhuma regra tem rede: o preço com a
temporada, o desconto, o cancelamento com devolução, as recusas e, sobretudo, a parte nova do cenário (a escolha do
documento, o ISS de 5%, o booking receipt em inglês, a validação de "exatamente uma identificação"). A pergunta da
mutação tem resposta trivial: trocar `0.05` por `0.5` (`PricingUtils.java:53`), inverter a ordem dos ramos em
`DocumentGenerator.java:21-36` ou apagar a linha do ISS (`DocumentGenerator.java:101`) não derrubaria nada. O defeito
que impede o serviço de subir (`Main.java:23`) seria pego por qualquer teste que levantasse o Javalin.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:

- uma chave estrangeira sem índice (`reservations.room_number → rooms`);
- 4 colunas de situação/tipo sem CHECK;
- nenhuma restrição UNIQUE/EXCLUDE/CHECK;
- 0 pool e 0 transações;
- o esquema criado por `CREATE TABLE` no código;
- **o serviço que não subiu contra o banco vazio** (`measures/database.md`). As tabelas ficaram criadas, mas o
  processo morreu logo depois, ao registrar as rotas.

**Nota:** ruim

- **O serviço não fica de pé.** O critério da skill para `ruim` inclui o serviço que não sobe contra um banco vazio, e
  é o que acontece. As tabelas e os quartos são criados (`db/Database.java:14-22`, `85-89`), mas o processo cai em
  seguida (`Main.java:23`).
- **Colunas que recusam dados válidos.**
  - `guest_country VARCHAR(2)` (`Database.java:50`) recusaria o país do exemplo do enunciado ("United Kingdom"), e o
    insert falharia.
  - `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`Database.java:46-47`) só aceitam os números sem pontuação.
    O texto não fixa o formato da entrada, e o exemplo do comprovante mostra o CPF formatado.
- **Esquema sem evolução.** O `CREATE TABLE IF NOT EXISTS` no código (`Database.java:31-73`) não sabe acrescentar
  uma coluna depois. `Statement` e `ResultSet` ficam fora de try-with-resources (`Database.java:29`, `79-82`).
- **Reserva dupla.**
  - Nada no banco impede duas reservas na mesma noite: não há EXCLUDE nem trava.
  - O serviço consulta os conflitos (`ReservationService.java:40-43`) e grava (`ReservationService.java:80`) em
    conexões separadas, sem transação.
  - Duas reservas simultâneas passariam as duas.
- **Status e plano sem proteção.** O status e o plano são texto livre, sem CHECK (`Database.java:55`, `59`).
- **O que está certo.** O SQL usa parâmetros em todas as consultas, e as conexões são devolvidas com
  try-with-resources nos repositórios (`ReservationRepository.java:14-16`, `92-93`).

## Comportamento

**Números:** 0 conferências passaram e 0 falharam: o roteiro não pôde testar nada, porque o serviço não subiu contra
o banco vazio (`measures/behavior.md`, `measures/behavior-service.log`, exceção em `Main.java:23`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: `java -jar` encerra no arranque, e o site não consegue listar
quartos, criar reservas nem tirar relatórios. A leitura do código mostra que, mesmo com as rotas trocadas para
`{id}`, outros pontos falhariam nas chamadas que o texto descreve:

- **JSON.** Javalin 6 serializa com Jackson, e o jar só tem Gson (`pom.xml`). Toda resposta com `ctx.json` (todas,
  inclusive as de erro, `Main.java:34`) dependeria de um mapeador que não está lá.
- **Confirmação.** Exige um corpo com `amountPaid` (`ReservationHandler.java:132-135`). O texto diz que o valor pago é
  o total da reserva, então um `POST /reservations/{id}/confirmation` sem corpo receberia 400. Um `amountPaid`
  qualquer seria aceito como pago (`ReservationService.java:98-100`).
- **Booking receipt.** "United Kingdom" estoura `guest_country VARCHAR(2)` (`Database.java:50`), e a reserva do
  estrangeiro do exemplo falharia com 500.
- **Dinheiro no JSON.** `amountPaid` sai como `0`, e não `0.00`, antes da confirmação (`BigDecimal.ZERO`,
  `ReservationService.java:76`).

Esses itens vêm da leitura e não foram medidos, porque o serviço não sobe. Eles não mudam a nota, que já é `ruim`
pelo arranque.

## Dúvidas

- A forma do pattern é a de uma **fábrica concentrada** (um método estático que escolhe pelo tipo de hóspede). Não é o
  Factory Method clássico com criador sobrescrito nem um método polimórfico no tipo de hóspede, que não existe como
  tipo. A referência aceita essa forma como acerto. Pesa contra o fato de a escolha depender de campos nulos, com um
  `else` que vale para qualquer coisa.
- Os defeitos latentes descritos em Comportamento (falta do Jackson, confirmação com `amountPaid`, `VARCHAR(2)` do
  país) vêm da leitura do código e do conteúdo do jar. Não foram confirmados por chamada à API, porque o serviço não
  sobe e o projeto não pode ser alterado para contornar isso.
