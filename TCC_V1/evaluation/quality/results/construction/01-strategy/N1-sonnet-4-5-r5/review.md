# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em camadas, separadas em pacotes: `controller` (as rotas), `service` (as regras de
uso), `domain.model` (a `Reservation` com a máquina de estados, `Room`, `RoomType` com capacidade e diária),
`domain.rateplan` (os cinco planos de tarifa, um por classe), `repository` (JDBC puro), `dto`, `exception` e `util`.
Tudo é criado e ligado em `Main.java:36-70`. Uma requisição entra no controller, que lê o corpo e chama o
`ReservationService`. O serviço pede o quarto ao `RoomService`, procura sobreposição no `ReservationRepository` e
constrói a `Reservation`. É o construtor dela que escolhe o plano em `RatePlanFactory` e calcula o preço. Os dados
ficam em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código, e todo o sistema usa **uma única
`Connection`** compartilhada. O pattern pedido (Strategy) está bem aplicado. O defeito mais sério vem do jeito de
reconstruir a reserva lida do banco: o repositório chama de novo o construtor, que valida e recalcula tudo, e passa
`companyCode = null` porque o código da empresa nunca é gravado (`ReservationRepository.java:222-235`). Por isso uma
reserva CORPORATE é criada com 201, mas daí em diante não pode ser lida, confirmada nem cancelada (tudo responde 400).
Ela também deixa o quarto bloqueado para qualquer reserva que se sobreponha a ela. Fora isso, nada impede que o mesmo
quarto seja reservado duas vezes em pedidos simultâneos: isso aconteceu em 8 de 10 rodadas.

## Arquitetura

**Números:** 9 pacotes, 20 dependências entre eles, 1 ciclo (`domain.model` ↔ `domain.rateplan`). O Javalin aparece
só no pacote raiz e em `controller`. O JDBC aparece em `repository`, no pacote raiz e em `service` (neste, só por
causa de `SQLException`). A maior classe é `ReservationRepository` (260 linhas).
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Os handlers leem a requisição, chamam o serviço e escrevem a resposta
  (`ReservationController.java:20-38`, `ReportController.java:19-33`).
- **A montagem fica num lugar só** (`Main.java:38-50`), com as dependências passadas pelo construtor.
- **O SQL fica nos repositórios**, e as regras de preço e de situação moram no domínio, sem Javalin nem JDBC
  (`Reservation.java:57-87`, `domain/rateplan/*`).

Os defeitos, todos localizados:
- **Um ciclo entre domínio e planos.** A `Reservation` chama `RatePlanFactory` (`Reservation.java:48`,
  `Reservation.java:84`), e os planos dependem de `RoomType`, que está em `domain.model`. A entidade escolhe e executa
  a estratégia no construtor quando poderia recebê-la pronta.
- **O repositório escreve nos campos privados do domínio por reflexão** (`ReservationRepository.java:237-256`). A
  camada de dados fura o encapsulamento da entidade porque ela não tem um jeito de ser reconstruída a partir do banco.
- **Os serviços conhecem `java.sql.SQLException`** e repetem o mesmo `try/catch` que embrulha o erro em
  `RuntimeException` (`ReservationService.java:43-57`, `ReservationService.java:73-79` e mais 5 vezes;
  `RoomService.java:19-23`; `MessageService.java:25-29`).
- **Uma regra no controller:** a segunda via só existe para reserva paga, e isso é decidido em
  `ReservationController.java:74-76`, não no serviço.
- **A inicialização duplicada:** `Main.java:114-120` cria os repositórios uma segunda vez só para criar as tabelas.
- **Uma `Connection` só, aberta por `DriverManager`** (`Main.java:112`), compartilhada por todas as threads do Jetty,
  sem pool.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura, todos implementando `RatePlan` (`RatePlan.java:8-19`):
`StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan` e `LongStayRatePlan`. Há
um único `switch`, e ele só escolhe a implementação (`RatePlanFactory.java:12-19`). Nenhum `switch` ou `if` de
comportamento fica fora dos planos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Cada plano tem no seu próprio lugar a validação, o preço, o desconto e a devolução:
- o código da empresa e os 12/18/25% em `CorporateRatePlan.java:13-17` e `CorporateRatePlan.java:27-37`;
- sexta a domingo, 2 noites e o preço fechado em `WeekendPackageRatePlan.java:14-18` e
  `WeekendPackageRatePlan.java:26-55`;
- as 7 noites e os 10% em `LongStayRatePlan.java:18-34`.

O cálculo e o cancelamento pedem ao plano, sem nomear nenhum (`Reservation.java:48-54`, `Reservation.java:84-85`).
`AbstractRatePlan` reúne o cálculo noite a noite com a temporada (`AbstractRatePlan.java:12-30`). O que afasta do
ideal:
- **A estratégia do convênio guarda estado.** `validate` grava `companyCode` num campo (`CorporateRatePlan.java:36`), e
  `calculateDiscount` depende disso (`CorporateRatePlan.java:46`): quem esquecer de chamar `validate` primeiro recebe
  um `NullPointerException`.
- **A reserva não guarda o dado do plano.** O código da empresa não fica na `Reservation`, e isso, junto com a
  reconstrução pelo construtor, causa o defeito do comportamento.
- **O plano é texto solto** (`String ratePlanName`, `Reservation.java:22`), e a fábrica é chamada de novo no
  cancelamento.

**O exagero.** Não há estrutura sem necessidade fora do pattern. A fábrica é o único ponto de seleção que a
referência aceita, e a classe abstrata tira a duplicação do cálculo noite a noite. O único resto inútil é
`RatePlan.getName()`, que ninguém chama.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em `MoneyUtil.java:8-14`, `LocalDate`, e
`enum` para situação, tipo de quarto e canal. Os defeitos que um revisor apontaria:
- **A entidade é reconstruída pelo construtor de criação.** Ao ler do banco, o construtor valida e recalcula o preço de
  novo (`Reservation.java:48-54`), e os valores gravados em `subtotal`, `discount` e `total` são ignorados. O
  resultado é a reflexão de `ReservationRepository.java:237-256` e o bug do convênio. Além disso, uma reserva antiga
  muda de valor se a regra mudar.
- **A formatação está duplicada:** as datas em `MessageService.java:71-73` e em `ReportService.java:66-68`, e o
  dinheiro com `String.format` e trocas de caractere (`MessageService.java:75-77`).
- **O dinheiro zero sai com escala 0.** Isso vem de `BigDecimal.ZERO` em `Reservation.java:45` e em
  `StandardRatePlan.java:22`, e o JSON mostra `0` em vez de `0.00`.
- **Exceções sem tratamento:** o `JsonProcessingException` do corpo inválido não é mapeado (`Main.java:96` só trata
  `BadRequestResponse`), e o `NumberFormatException` do id também não (`ReservationController.java:41`). Os dois viram
  500.
- **Campo numérico ausente vira zero:** `roomNumber` é `int` primitivo (`CreateReservationRequest.java:6`). Sem o
  campo, a resposta é 404 ROOM_NOT_FOUND ("Room 0 not found") em vez de 400.

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 pulados; 9 métodos escritos, num único arquivo (`RatePlanTest.java`).
Cobertura de 9,7% das linhas (72 de 746) e 14,7% dos ramos (22 de 150). `controller`, `service`, `repository` e `dto`
têm 0%. Nenhum teste passa pela API ou pelo banco.
**Nota:** com problemas

**O que os testes conferem.** São testes de unidade só dos planos, mas com os valores do texto:
- a temporada de julho, 599,76 (`RatePlanTest.java:125-133`);
- o NON_REFUNDABLE, 116,97 (`RatePlanTest.java:30-41`);
- Tecnosul, 93,58 (`RatePlanTest.java:44-56`);
- o pacote, 820,00 e a devolução de 410,00 (`RatePlanTest.java:70-85`);
- a estadia longa, 1.749,30 e 174,93 (`RatePlanTest.java:99-111`);
- as recusas de código inválido, de data fora do pacote e de 3 noites na estadia longa.

**O que fica sem rede.** Se alguém trocasse 18 por 81 na Agrovale ou 25 por 52 na Farmavida, nenhum teste falharia: só
a Tecnosul é testada. O mesmo vale para trocar `nights < 7` por `nights < 6` em `LongStayRatePlan.java:21`, porque o
único caso negativo tem 3 noites. Também não há teste para:
- a devolução do convênio e da estadia longa;
- o convênio sem código;
- o limite de 15 de dezembro;
- a máquina de estados (`Reservation.java:57-87`);
- o cancelamento com reserva pendente;
- o comprovante, o e-mail e os dois relatórios;
- os códigos de erro da API.

Como nenhum teste lê uma reserva de volta do banco, o defeito que torna o convênio inutilizável passou sem ser visto.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, criadas por
`CREATE TABLE IF NOT EXISTS` no Java, sem migrações. O dinheiro está em `numeric(10,2)` e as datas em `date`. O script
acusou:
- 0 chaves estrangeiras;
- 0 CHECK, UNIQUE ou EXCLUDE;
- status, plano, tipo e canal em `varchar` sem CHECK;
- 0 pool e 1 `DriverManager.getConnection`;
- 0 transações e 0 `FOR UPDATE`;
- nenhum SQL montado com `+`.
**Nota:** ruim

**Dado do plano perdido.** A tabela `reservations` não tem coluna para o código da empresa
(`ReservationRepository.java:23-43`), e ao ler a reserva o repositório passa `null` no lugar dele
(`ReservationRepository.java:234`). A regra do convênio exige o código, então toda reserva CORPORATE gravada fica
ilegível (400 em GET, na confirmação e no cancelamento) e não há como recuperá-la pela API. Pior ainda, a consulta de
sobreposição mapeia essas linhas (`ReservationRepository.java:156-159`): qualquer reserva nova no mesmo quarto e em
noites sobrepostas também recebe 400. Confirmei isso com `curl`: depois de criar uma CORPORATE no 201, um STANDARD no
201 nas noites seguintes recebeu `400 INVALID_REQUEST "Company code is required for corporate rate plan"`. Os valores
gravados (`subtotal`, `discount`, `total`) nunca são lidos de volta: a reserva é recalculada pelas regras do dia
(`ReservationRepository.java:222-235`).

**Nada protege o quarto contra duas reservas.** A verificação de sobreposição e o `INSERT` são dois comandos separados,
em autocommit, sem transação, trava nem restrição (`ReservationService.java:44-54`). Não há nem um `synchronized`, e o
roteiro criou as duas reservas simultâneas em 8 de 10 rodadas. Também não há chave estrangeira de
`reservations.room_number` para `rooms.number`.

**O resto.**
- Uma única `Connection` é compartilhada por todas as requisições, sem pool e sem reconexão (`Main.java:112`): se a
  conexão cair, o serviço fica sem banco até reiniciar.
- O esquema no código não sabe evoluir: acrescentar a coluna que falta exigiria um `ALTER` que
  `CREATE TABLE IF NOT EXISTS` não faz.
- A confirmação grava a reserva e a mensagem sem transação (`ReservationService.java:74-75`).
- O SQL usa parâmetros e try-with-resources nos statements, e o filtro do financeiro é feito no banco
  (`ReservationRepository.java:194-204`).

## Comportamento

**Números:** 68 de 72 conferências passaram:
- a parte comum, 44 de 46;
- depois de reiniciar, 3 de 3;
- o cenário 01, 20 de 21;
- as reservas simultâneas, 1 de 2 (houve reserva dupla em 8 de 10 rodadas).
**Nota:** ruim

**O que falhou:**
- **O convênio cancelado depois de pago** (esperado 686,22, veio null). A causa é maior do que a conferência mostra:
  a confirmação da reserva CORPORATE já responde 400. Chamei a API à mão para confirmar: a reserva CORPORATE é criada
  com os valores certos (779,80, 93,58 e 686,22), mas `GET /reservations/1`, a confirmação e o cancelamento respondem
  `400 INVALID_REQUEST "Company code is required..."`.

  Para o dono da pousada, isso quer dizer o seguinte. Nenhum hóspede do convênio consegue pagar, e a reserva dele
  também não pode ser cancelada. Ela fica pendurada e bloqueia o quarto naquelas noites: quem tentar reservar recebe
  um erro de dados (400) em vez de "quarto indisponível". Um dos três planos novos não funciona, e cada uso dele
  estraga a agenda de um quarto.
- **As reservas simultâneas.** Em 8 de 10 rodadas, dois pedidos ao mesmo tempo criaram duas reservas para o mesmo
  quarto e as mesmas noites, o que dá overbooking.
- **O corpo que não é JSON** responde 500 em vez de 400 INVALID_REQUEST.
- **`amountPaid` sai `0` em vez de `0.00`**, assim como `discount` no plano normal. O texto pede 2 casas; isso é
  menor.

Funcionaram:
- os outros planos novos, com os valores do texto: Agrovale, Farmavida, o pacote (com a devolução de 410,00) e a
  estadia longa (com a temporada e a devolução);
- as recusas do cenário;
- `companyCode` ignorado nos outros planos;
- a parte comum: preços, e-mail, comprovante e relatórios;
- os dados depois de reiniciar.

Fora do roteiro, um id não numérico em `/reservations/abc` dá 500, e uma criação sem `roomNumber` dá 404
ROOM_NOT_FOUND em vez de 400.

## Dúvidas

- **O defeito do convênio pesa em três colunas.** A causa está no banco (o código da empresa não é gravado) e no
  design (a reconstrução pelo construtor que revalida), e o efeito aparece no comportamento. Dei `ruim` no banco e no
  comportamento, e no design deixei `com problemas`, porque o pattern em si está certo.
- **O comportamento ficou entre `com problemas` e `ruim`.** Pelo critério literal da skill, 20 de 21 conferências do
  cenário e a falha de concorrência apontariam `com problemas`. Escolhi `ruim` porque o convênio fica inutilizável
  depois de criado e bloqueia o quarto, e porque houve reserva dupla.
- **Os testes ficaram entre `com problemas` e `ruim`.** São só 9 testes de unidade, com 9,7% de cobertura, mas eles
  conferem com valores a maior parte da regra nova. Por isso dei `com problemas`.
