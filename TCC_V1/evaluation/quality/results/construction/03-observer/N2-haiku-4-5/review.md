# Revisão: construção, cenário 03

## Visão geral

O projeto (`projeto/reservations`, caminhos abaixo a partir de `reservations/src/main/java/com/pousadamarealta/`) tem cinco pacotes: `api` (handlers estáticos do Javalin), `service` (regras e avisos), `domain` (entidades, `Money`, o despachante de eventos), `database` (DAOs com JDBC puro e `CREATE TABLE` no código) e a raiz com o `App`. Uma requisição entra pelo método estático do handler (`App.java:41-54`), que lê o JSON como `JsonNode`, consulta o DAO e o serviço, que são singletons globais (`getInstance()`), e grava no PostgreSQL por `DriverManager`, uma conexão por chamada (`database/DatabaseManager.java:76-78`). Os avisos à governança e ao financeiro são ouvintes de um `ReservationEventDispatcher`, mas o e-mail do hóspede é chamado direto pelo serviço. O mais sério: **pela API, o sistema não cria reservas.** A coluna `guest_cpf VARCHAR(11)` (`database/DatabaseManager.java:51`) recusa o CPF no formato que o próprio texto mostra ("111.222.333-44") e devolve 500. Mesmo com um CPF só de dígitos, a confirmação sem corpo dá 500, porque exige um `amountPaid` que o texto não prevê e confia no valor que o cliente manda (`api/ReservationsHandler.java:88-105`). Além disso, o check-out não devolve o comprovante, o dinheiro sai como "R$ 779.80", o relatório financeiro perde as não reembolsáveis canceladas, não há nada contra reserva dupla e não há teste nenhum de verdade. O comportamento medido: 18 de 66 conferências passaram.

## Arquitetura

**Números:** 5 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `api` e na raiz e o JDBC só em `database` (em `service`, só `SQLException` nas assinaturas). A maior classe é `api/ReservationsHandler.java`, com 244 linhas.
**Nota:** com problemas

As dependências apontam para o lado certo (api → service → database → domain) e o SQL fica todo nos DAOs. Mas a separação vaza em vários pontos:

- **O handler faz regra de negócio.** `api/ReservationsHandler.java:48` decide a capacidade e `:60` a disponibilidade, antes de chamar o serviço. `:103` repete a checagem de PENDING que o serviço já faz (`service/ReservationService.java:46`). `:185` decide quem tem direito à segunda via, olhando `amountPaid == 0` em vez da situação da reserva. `:221-227` deduz o tipo do quarto pelo número.
- **O handler fala direto com o DAO,** sem passar pelo serviço: `api/ReservationsHandler.java:41`, `api/RoomsHandler.java` (`RoomDAO.getInstance().getAllRooms()`) e `api/MessagesHandler.java` (`MessageDAO.getInstance()`). `service/GuestNotificationService.java:26` também busca o quarto no DAO para montar o comprovante.
- **A montagem não está num lugar só.** Cada classe pega as dependências por singleton estático: `ReservationService.getInstance()`, `ReservationDAO.getInstance()`, `MessageService.getInstance()` e `ReservationEventDispatcher.getInstance()` (`service/ReservationService.java:55`). `DatabaseManager` guarda a conexão num `static` mutável (`database/DatabaseManager.java:9,21-23`). O `App` registra os ouvintes (`App.java:35-36`), mas nada é passado por construtor, e por isso nada é testável isoladamente.
- **O tratamento de erros da borda** se resume a "qualquer `RuntimeException` vira 409" (`api/ReservationsHandler.java:108,131,150,169`) e "qualquer outra coisa vira 500" (`api/ApiErrorHandler.java:10`).

São misturas localizadas, sem ciclo. Não chegam a "camadas ausentes", mas alguém precisa tirar as regras dos handlers e ligar as peças no `main`.

## Design de baixo nível

**Números:** dos três interessados na confirmação, 2 estão na estrutura (governança e financeiro, `service/GovernanceNotificationListener.java` e `service/FinanceNotificationListener.java`) e 1 está fora: o hóspede, por chamada direta em `service/ReservationService.java:54`. No cancelamento, os 2 interessados estão na estrutura.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O que aproxima do Observer: há uma interface de ouvinte, `domain/ReservationEventListener.java:3-6`, com `onConfirmed` e `onCancelled`. Há uma implementação por interessado (governança e financeiro), registrada no `App` (`App.java:35-36`). O serviço anuncia sem conhecer os destinos (`service/ReservationService.java:55` e `:109`), e o despachante tem `addListener` e `removeListener` (`domain/ReservationEventDispatcher.java:17-23`). Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha no `App`.

O que afasta:

- **O hóspede não é ouvinte.** `ReservationService.confirmReservation` chama `GuestNotificationService.sendConfirmationEmail(reservation)` antes do anúncio (`service/ReservationService.java:54`). A referência do cenário classifica isso como malfeito.
- **A lista vem de um singleton estático,** e não de fora (`domain/ReservationEventDispatcher.java:7,13-15`), e o serviço a busca por `getInstance()` (`service/ReservationService.java:55,83`).
- **O ouvinte da governança refaz uma checagem que o serviço já fez:** pergunta se a reserva foi paga (`service/GovernanceNotificationListener.java:29`), quando o serviço só anuncia o cancelamento de reservas confirmadas (`service/ReservationService.java:108`).
- **Um ouvinte que falha derruba a operação depois de gravada.** Os ouvintes relançam qualquer erro como `RuntimeException` (`service/FinanceNotificationListener.java:19-20`). Isso acontece depois do `updateReservation` (`service/ReservationService.java:52`), e o handler transforma o erro em 409 OPERATION_NOT_ALLOWED (`api/ReservationsHandler.java:108-110`): a reserva fica confirmada no banco, mas a resposta diz que a operação não foi permitida.

**O exagero.** Não há estrutura sobrando: a interface de ouvinte tem duas implementações e é o ponto de extensão pedido. `MessageService` só cria a `Message` e repassa ao DAO (`service/MessageService.java:17-20`), mas faz o papel da "caixa de saída" que o texto descreve. `mapCheckOutResponse` (`api/ReservationsHandler.java:215-217`) só repassa, mas é uma linha e não forma uma camada.

**O resto do código.** Os defeitos estão espalhados:

- **O dinheiro passa por `double` em todo lado,** apesar de existir um `Money` com `BigDecimal` e HALF_EVEN:
  - as diárias são `double` (`domain/RoomType.java:4-6,9`);
  - a alta temporada calcula `baseRate * 1.20` em `double` (`domain/SeasonalityCalculator.java:11`);
  - o desconto é `subtotal.toDouble() * 0.15` (`domain/Reservation.java:79`);
  - o JSON arredonda com `Math.round(x * 100) / 100.0` (`api/ReservationsHandler.java:206-211,239-241`), e por isso sai `779.8` e não `779.80`;
  - o valor pago vem de `asDouble()` (`api/ReservationsHandler.java:105`);
  - o relatório soma em `double` (`service/ReportService.java:65-75`);
  - os quartos são gravados com `setDouble` (`database/RoomDAO.java:50`).
- **Duplicação:**
  - a dedução do tipo do quarto pelo número aparece duas vezes (`api/ReservationsHandler.java:221-227` e `service/GuestNotificationService.java:70-78`);
  - o bloco "busca e devolve 404 RESERVATION_NOT_FOUND" aparece cinco vezes (`api/ReservationsHandler.java:77-80,97-100,122-125,141-144,160-163,179-182`), e o serviço repete a mesma busca lançando `RuntimeException`;
  - o formatador de dinheiro aparece em três classes, e as três erram o formato pedido: `"0.00"` com `replace(',', '.')` dá "R$ 779.80" e não "R$ 779,80", e não há separador de milhar (`service/GuestNotificationService.java:12,55-57`, `service/FinanceNotificationListener.java:10,38-40`).
- **Os erros:**
  - toda regra violada é uma `RuntimeException` genérica (`service/ReservationService.java:30,43,47,67,82,99`), e o handler a converte em 409 sem distinguir a causa;
  - `catch (Exception)` transforma qualquer falha do relatório, inclusive uma do banco, em "formato inválido" (`api/ReportsHandler.java:28,48`);
  - a situação da reserva é comparada por texto (`getStatus().getCode().equals("PENDING")`, `api/ReservationsHandler.java:103`).
- **O estado:** `Reservation` tem setters públicos de status, valor pago e devolução (`domain/Reservation.java:128-146`), e as transições ficam no serviço, por `if` sobre a situação (`service/ReservationService.java:46-50,66-70,81-85,96-105`), e não na entidade.

## Testes

**Números:** 1 teste rodou, 0 falharam; 1 método de teste escrito; cobertura de 0 de 804 linhas (0,0%) e 0 de 212 ramos (0,0%) (`measures/tests.md`).
**Nota:** ruim

O único teste é o do arquétipo do Maven, `assertTrue(true)` (`src/test/java/com/pousadamarealta/AppTest.java:15-19`). Não há teste de preço, de temporada, de arredondamento, de plano, de cancelamento e devolução, das situações da reserva, dos códigos de erro, nem da parte nova (os avisos à governança e ao financeiro). Pela pergunta da mutação, nada falharia se alguém trocasse `0.15` por `0.51` (`domain/Reservation.java:79`), apagasse o `fireOnCancelled` (`service/ReservationService.java:109`) ou invertesse o `> 0` do reembolso (`service/FinanceNotificationListener.java:27`). O `mvn verify` passa só porque não há o que conferir. Os testes estão praticamente ausentes, e foi justamente a falta de um único teste de criação pela API que deixou passar o 500 do CPF.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number) → rooms` sem índice; status, plano, tipo e canal em `varchar` sem CHECK; nenhuma restrição UNIQUE, EXCLUDE ou CHECK; esquema criado por `CREATE TABLE` no código (0 migrações); nenhum pool (1 `DriverManager.getConnection`); 0 transações; 0 `FOR UPDATE`. Nenhum SQL montado com dado de fora (`measures/database.md`).
**Nota:** ruim

O que está certo: os tipos de dinheiro e de data, a chave estrangeira para o quarto, os `NOT NULL`, o SQL todo com `?` e as conexões fechadas por try-with-resources (`database/ReservationDAO.java:31,61,75`).

O que é sério:

- **Nada impede duas reservas na mesma noite.** A disponibilidade é uma consulta (`database/ReservationDAO.java:109-128`) feita no handler (`api/ReservationsHandler.java:60`), e o `INSERT` vem depois, em outra conexão (`service/ReservationService.java:37`, `database/ReservationDAO.java:27-55`). Não há transação, trava, `synchronized` nem restrição de exclusão no esquema (`database/DatabaseManager.java:46-64`). Dois pedidos simultâneos podem passar os dois pela checagem. O roteiro de concorrência não chegou a medir isso porque todas as criações deram 500, mas pelo código o risco de quarto vendido duas vezes é real.
- **A coluna do CPF tem o tamanho errado.** `guest_cpf VARCHAR(11)` (`database/DatabaseManager.java:51`) recusa o CPF no formato que o texto usa ("111.222.333-44"), e o serviço não normaliza nem valida antes. O resultado é "ERROR: value too long for type character varying(11)" e nenhuma reserva gravada (`measures/behavior.md`).

O que também precisa mudar:

- **O esquema não sabe evoluir.** É criado com `CREATE TABLE IF NOT EXISTS` no código (`database/DatabaseManager.java:39-72`), que não consegue acrescentar uma coluna numa tabela que já existe.
- **Faltam o índice e os CHECKs.** A chave estrangeira não tem índice, e não há CHECK em status, plano e hóspedes (`guests >= 1`, `check_out > check_in`).
- **Falta um pool.** Abre-se uma conexão nova por consulta (`database/DatabaseManager.java:76-78`).
- **O relatório de ocupação faz N+1 consultas,** uma por quarto (`service/ReportService.java:30-32`). Com cinco quartos, isso pesa pouco.

## Comportamento

**Números:** 18 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 14 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 03 | 2 de 15 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A falha que derruba quase tudo:** `POST /reservations` com o CPF "111.222.333-44" responde 500, porque a coluna do banco tem 11 caracteres. Sem reserva criada, caem em cascata a confirmação, os e-mails, o check-in, o check-out, os cancelamentos, os relatórios com dados, a persistência depois de reiniciar e quase todo o cenário 03. Das 2 conferências do cenário que passaram ("o financeiro não é avisado" e "cancelar pendente não manda nada"), nenhuma prova que a regra existe, porque nada foi mandado em caso nenhum. As 10 rodadas simultâneas deram "500 500".

Para separar o que vem desse defeito do que viria depois dele, chamei a API à mão, com um CPF só de dígitos. Apareceram outros erros, que o dono da pousada também sentiria:

- **Confirmar sem corpo** (como o texto descreve) dá 500. O handler exige `amountPaid` no corpo (`api/ReservationsHandler.java:88-94`) e grava o valor que o cliente manda: confirmar uma reserva de R$ 779,80 com `{"amountPaid":1.00}` deixou pago R$ 1,00. O financeiro recebeu "Valor: R$ 1.00", e o relatório financeiro contou R$ 1,00. O texto diz que o pago é o total da reserva.
- **O check-out devolve a reserva, e não o comprovante** (`api/ReservationsHandler.java:168,215-217`).
- **O dinheiro sai no formato errado:** o e-mail do hóspede e o do financeiro mostram "R$ 779.80" em vez de "R$ 779,80" (`service/GuestNotificationService.java:55-57`), e o JSON mostra `779.8` e `0.0` em vez de 2 casas.
- **O relatório financeiro deixa de fora a NON_REFUNDABLE cancelada,** que o texto diz que continua contando. A consulta exclui as canceladas (`database/ReservationDAO.java:93`). O relatório também incluiria as pendentes com valor 0.00.
- **A segunda via sai para uma reserva STANDARD cancelada depois de paga,** porque a regra olha `amountPaid` e não a situação (`api/ReservationsHandler.java:185`).
- **Vários erros de entrada dão 500:** zero hóspedes dá 422 CAPACITY_EXCEEDED em vez de 400 (`:48`); corpo que não é JSON, data inválida, plano desconhecido e id não numérico dão 500 em vez de 400 ou 404 (`:21,36-37`, `domain/RatePlan.java:23`, `api/ApiErrorHandler.java:10`).

O que funcionou nessa chamada à mão: o preço (599,76 na alta temporada; desconto de 37,48 e total de 212,42 no não reembolsável), os e-mails da governança e do financeiro na confirmação, o aviso de cancelamento à governança e o financeiro calado quando não há devolução.

Para o dono da pousada, do jeito que foi entregue, o site não consegue criar nenhuma reserva. E, se conseguisse, a confirmação não funcionaria do jeito combinado, o valor pago poderia ficar errado, e o relatório do contador perderia dinheiro de cancelamentos não reembolsáveis.

## Dúvidas

- O texto não diz em que formato o CPF chega. O roteiro manda formatado, como no comprovante, e o agente supôs 11 dígitos sem validar nem normalizar. Pesei isso como erro, porque o resultado é um 500 e não uma recusa. Ainda assim, mesmo com o CPF só de dígitos, a confirmação sem corpo também dá 500, e a nota do comportamento seria `ruim` de qualquer jeito.
- A falta de proteção contra reserva dupla vem da leitura do código. O roteiro não conseguiu exercitá-la por causa dos 500.
- Na classe do pattern, a lista de ouvintes vir de um singleton estático, e não ser recebida pelo serviço, também afasta do acerto. Mas o motivo decisivo para `malfeito` é o hóspede chamado direto.
