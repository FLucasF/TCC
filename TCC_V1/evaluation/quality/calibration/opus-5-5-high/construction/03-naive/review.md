# Revisão: construção, cenário 03

## Visão geral

O projeto tem 7 pacotes por assunto: `hotel` para a montagem e o banco, `web`, `reservations`, `rooms`, `messages`, `documents` e `reports`. Uma requisição passa assim: o handler do Javalin em `HttpApi` lê o corpo e chama o `ReservationService`. O serviço aplica as regras, calcula o preço com o `PriceCalculator` e grava pelo `ReservationStore` (JDBC com Hikari). Os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway (`V1__create_tables.sql`). Uma restrição `EXCLUDE` impede reserva dupla no mesmo quarto. O sistema funciona: as 66 conferências do roteiro passaram, e os 26 testes do agente passam. O que há de mais sério é o pattern pedido, que não foi feito. Na confirmação e no cancelamento, o `ReservationService` manda, um depois do outro, os e-mails do hóspede, da governança e do financeiro, com os endereços e os textos escritos dentro dele (`src/main/java/hotel/reservations/ReservationService.java:68-80` e `:101-110`). Cada interessado novo, como o restaurante ou o transfer, obriga a mexer no serviço. Além disso, os testes não conferem nenhum aviso da equipe, e a confirmação grava a reserva e as mensagens em conexões separadas, sem uma transação.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há 1 ciclo, entre `hotel.documents` e `hotel.reservations`. A maior classe é `HttpApi`, com 170 linhas, seguida do `ReservationService`, com 161.
**Nota:** com problemas

- **O que está bem:**
  - A borda HTTP é fina. Cada rota lê a requisição, chama o serviço e escreve a resposta (`src/main/java/hotel/web/HttpApi.java:62-90`). Os códigos de erro viram status HTTP num lugar só (`HttpApi.java:93-107`).
  - As regras não conhecem o Javalin nem o JDBC. O jdeps mostra Javalin só em `hotel` e `hotel.web`.
  - O SQL fica isolado em três classes de acesso: `ReservationStore`, `Rooms` e `SentMessages`.
  - A montagem é feita num lugar só (`src/main/java/hotel/Hotel.java:20-28`), e nenhuma classe cria as suas dependências por dentro.
- **O ciclo:** `ReservationService` importa `ReservationReceipt` e `TextFormat` (`src/main/java/hotel/reservations/ReservationService.java:3-4`). Já `ReservationReceipt` importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O ciclo nasce porque o serviço de regras monta textos de e-mail (`ReservationService.java:73-80`, `:103-108` e `:148-154`), uma responsabilidade de apresentação que devia estar fora dele.
- **A dependência de `messages`:** pelo mesmo motivo, `reservations` depende de `messages`. O serviço conhece o `EmailSender` e os endereços da equipe (`ReservationService.java:14-15`).
- **Por que `com problemas`:** é um ciclo pequeno entre pacotes vizinhos, e a separação no resto é boa.

## Design de baixo nível

**Números:**
- Interessados na estrutura: nenhum. Não existe interface de ouvinte nem de evento.
- Interessados avisados à mão:
  - na confirmação, 3: o hóspede, a governança e o financeiro (`ReservationService.java:68-80`);
  - no cancelamento, 2: a governança e o financeiro (`ReservationService.java:101-110`).

**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

- **O pattern (Observer):** é omissão. O texto pede que o serviço anuncie a confirmação e o cancelamento sem conhecer quem é avisado, porque "vai ter cada vez mais gente querendo ser avisada". O código faz o contrário:
  - `confirm` grava a reserva e chama `emailSender.send` três vezes seguidas. Os destinatários são constantes do próprio serviço (`ReservationService.java:14-15`), e os assuntos e corpos estão escritos ali mesmo (`:68-80`).
  - `cancel` faz o mesmo dentro de um `if (wasConfirmed)`, com outro `if` para o financeiro (`:101-110`).
  - Não há lista de ouvintes, nada é registrado no `Hotel` (`Hotel.java:24-25`), e o serviço recebe um `EmailSender` concreto (`:22-27`).
  - Para incluir o restaurante ou o transfer, ou para tirar um deles quando o contrato acabar, é preciso editar e reimplantar o `ReservationService`.
  - Nada do que se espera de um Observer aparece no código.
- **O exagero:** não há. O código não tem interface com uma implementação só, fábrica nem builder. O `EmailSender` só repassa para `SentMessages` com o canal "EMAIL" (`src/main/java/hotel/messages/EmailSender.java:12-14`), mas é pequeno e dá nome ao canal.
- **O resto do código:** está limpo na maior parte.
  - Dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`).
  - Datas são `LocalDate`. Os estados, os planos e os tipos de quarto são `enum`.
  - Os erros usam uma `HotelException` com `ErrorCode`, e os nomes dizem o que as coisas fazem.
- **Os defeitos localizados que justificam a nota:**
  - `confirm` e `cancel` fazem coisas demais. Mudam o estado, gravam e compõem até três textos de e-mail (`ReservationService.java:60-82` e `:88-112`). Na confirmação, o quarto é buscado duas vezes no banco (`:67` e `:72`).
  - A regra do plano de tarifa fica espalhada em dois `switch` sobre `RatePlan`: o desconto em `PriceCalculator.java:26-29` e a devolução em `ReservationService.java:95-98`. Um plano novo obriga a mexer nos dois.
  - As transições de estado não estão protegidas na entidade. `Reservation.changeStatus` aceita qualquer estado (`src/main/java/hotel/reservations/Reservation.java:102-104`), e as regras de qual transição vale ficam todas no serviço.
  - `readNewReservation` usa um `catch (Exception)` que transforma qualquer falha em 400 (`HttpApi.java:111-115`). Isso é aceitável porque o trecho só faz a leitura do corpo.

## Testes

**Números:**
- 26 testes rodaram: 0 falhas, 0 erros e 0 pulados. São 26 métodos escritos.
- Cobertura de linhas: 94,0% (489 de 520). Cobertura de ramos: 87,7% (107 de 122).
- Cobertura de `hotel.reservations`: 227 de 240 linhas e 55 de 58 ramos.
- Os níveis estão misturados: 4 classes de teste usam o banco real e 1 testa pela API HTTP. Nenhuma usa mocks.

**Nota:** com problemas

- **A parte comum está bem testada, com valores:**
  - **O preço:** temporada no dia 15/12, fim de fevereiro, julho, e meio para o par com 37,48 e 212,42 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:31-74`).
  - **As recusas, com o código de erro:** `ReservationServiceTest.java:53-62` e `:65-73`.
  - **As devoluções por plano:** 249,90, 0,00 e 0,00, e também o valor que fica com a pousada (`src/test/java/hotel/reservations/ReservationServiceTest.java:109-122`).
  - **As transições:** `ReservationServiceTest.java:125-146`.
  - **Os relatórios:** conferidos com o CSV inteiro (`src/test/java/hotel/reports/ReportsTest.java:57-115`).
- **A parte nova do cenário está quase sem rede.** Para os avisos da equipe, os testes só conferem que saíram 3 mensagens na confirmação (`ReservationServiceTest.java:91` e `src/test/java/hotel/web/HttpApiTest.java:81`):
  - nenhum teste confere o destinatário `governanca@...` ou `financeiro@...`, o assunto "Reserva confirmada - quarto N" ou "Pagamento recebido - reserva N", nem as datas, os hóspedes ou o valor no corpo;
  - os avisos de cancelamento não são conferidos em lugar nenhum, porque o teste de cancelamento (`ReservationServiceTest.java:109-122`) não olha as mensagens.
- **A pergunta da mutação:** nenhum teste falharia se alguém fizesse uma destas mudanças:
  - apagar o aviso da governança no cancelamento (`ReservationService.java:103-105`);
  - trocar `refund.signum() > 0` por `>= 0`, o que avisaria o financeiro de um reembolso zero (`:106`);
  - trocar o endereço do financeiro (`:15`).

## Banco

**Números:**
- 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, criadas por 1 migração do Flyway.
- Dinheiro sempre em `numeric(10,2)`, e nenhuma data guardada como texto.
- Nenhuma chave estrangeira sem índice: o índice gist do `EXCLUDE` começa por `room_number`.
- 5 restrições: os `CHECK`s e o `EXCLUDE` contra reserva dupla.
- O que a medição acusou:
  - `status`, `rate_plan`, `type` e `channel` são `text` sem `CHECK`;
  - não há nenhuma transação explícita.

**Nota:** com problemas

- **O que está bem:**
  - O esquema nasce de uma migração versionada (`src/main/resources/db/migration/V1__create_tables.sql`).
  - A reserva está ligada ao quarto por chave estrangeira (`V1__create_tables.sql:21`).
  - O banco protege a regra mais importante: `EXCLUDE USING gist (room_number WITH =, daterange(...) WITH &&) WHERE status <> 'CANCELLED'` (`V1__create_tables.sql:38-40`). A violação vira 409 (`src/main/java/hotel/reservations/ReservationStore.java:55-58`), e por isso a reserva dupla é impedida mesmo com mais de um processo.
  - O SQL usa sempre `?`. Os `+` acusados pelo script juntam só constantes (`ReservationStore.java:18-19`, `:31-33`, `:98` e `:111`).
  - As conexões vêm do pool Hikari e são fechadas com try-with-resources.
- **Os defeitos:**
  - **Sem transação, a confirmação pode ficar pela metade.** `ReservationService.confirm` grava a reserva (`ReservationService.java:66`) e depois três mensagens (`:68-80`), cada escrita na sua conexão em autocommit (`ReservationStore.java:64-76` e `src/main/java/hotel/messages/SentMessages.java:20-35`). Se uma das inserções falhar, a reserva fica `CONFIRMED` e um aviso se perde, que é justamente o problema que o dono quer evitar. O mesmo vale para o cancelamento (`:100-110`).
  - **Atualização cega.** O `UPDATE` não confere o estado anterior (`ReservationStore.java:67`). Só o `synchronized` do serviço (`ReservationService.java:60` e `:88`) impede duas confirmações ao mesmo tempo, e ele vale apenas para um processo.
  - **Sem `CHECK` nos estados.** `status` e `rate_plan` aceitam qualquer texto no banco (`V1__create_tables.sql:28` e `:33`).
  - **Consultas que leem a tabela inteira.** Os dois relatórios trazem todas as reservas para filtrar em Java (`src/main/java/hotel/reports/OccupancyReport.java:25` e `src/main/java/hotel/reports/RevenueReport.java:25`, por `ReservationStore.all()` em `ReservationStore.java:108-121`). A tabela cresce sem limite.

## Comportamento

**Números:** 66 de 66 conferências passaram, e nenhuma falhou.
- Parte comum: 46 de 46.
- Dados depois de reiniciar: 3 de 3.
- Cenário 03: 15 de 15.
- Duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- Os preços batem com os exemplos, incluindo os R$ 599,76 e o meio para o par. As recusas vêm com o status e o código certos, e os relatórios saem no formato combinado.
- Os dados continuam depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca passam juntas.
- Na confirmação, a governança e o financeiro recebem os e-mails com os assuntos, as datas, os hóspedes e o valor.
- No cancelamento de uma reserva confirmada, os dois são avisados. O financeiro não recebe aviso quando não há o que devolver, e uma reserva pendente cancelada não manda nada.

O que falta não aparece na API: é o design. Incluir o restaurante e o transfer vai exigir mudar o código do serviço, como explicado no design.

## Dúvidas

- **O exagero.** O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`) só repassa para `SentMessages` com o canal "EMAIL". Considerei que é contenção, porque ele dá nome ao canal que o texto descreve. Um revisor mais estrito poderia chamá-lo de camada que só repassa.
- **A nota do design.** Os métodos longos de `confirm` e `cancel` pesam em parte por causa da omissão do pattern, já que o tamanho vem dos e-mails montados à mão. Sem isso, os defeitos que sobram (os dois `switch` de `RatePlan`, o `changeStatus` sem guarda, a busca dupla do quarto) ficam no limite entre `bom` e `com problemas`.
