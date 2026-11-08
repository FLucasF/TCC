# Revisão: manutenção, cenário 03

## Visão geral

O projeto é o da base, com a mesma organização em pacotes: `hotel.web` (as rotas do Javalin, em `HttpApi`), `hotel.reservations` (as regras em `ReservationService` e o SQL em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava os e-mails na tabela `messages`), `hotel.documents`, `hotel.reports`, e a montagem em `hotel.Hotel`. Uma requisição passa por `HttpApi`, depois por `ReservationService`, e chega ao `ReservationStore`/`SentMessages` (PostgreSQL, com o esquema criado por uma migração Flyway). O agente criou o pacote `hotel.notifications` com uma interface `ReservationNotification` (`onConfirmation`/`onCancellation`) e dois ouvintes (`GovernanceNotification` e `FinanceNotification`), registrados em `Hotel.java:33`. O `ReservationService` agora percorre a lista na confirmação e no cancelamento de reserva confirmada. O comportamento está todo certo: 66 de 66 conferências passaram. Há dois problemas principais. O e-mail do hóspede continua montado e enviado à mão dentro do serviço (`ReservationService.java:72-76`), fora da lista de ouvintes, e por isso o Observer ficou pela metade. Além disso, a interface foi posta no pacote `notifications`, o que criou um ciclo novo entre `reservations` e `notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 23 dependências entre eles (a base tinha 17). Há 3 ciclos pelo `tsort`, contra 1 na base. O ciclo `documents` ↔ `reservations` já existia na base. Os novos são `reservations` ↔ `notifications` e o ciclo de três `documents`/`reservations`/`notifications`. As regras continuam sem Javalin, e o JDBC fica só em `reservations`, `rooms` e `messages`, como na base.
**Nota:** com problemas

A borda HTTP, as regras e o acesso a dados continuam como na base, e o agente não mexeu nisso. A montagem está num lugar só: os ouvintes são criados e passados ao serviço em `Hotel.java:27-33`, e o serviço recebe a lista pelo construtor (`ReservationService.java:22-32`). O defeito é o ciclo novo. A interface do ouvinte, `hotel/notifications/ReservationNotification.java:7`, é usada pelo serviço (`ReservationService.java:5`). Ela recebe `Reservation`, e as implementações dependem de `hotel.reservations.Reservation` (`FinanceNotification.java:5`, `GovernanceNotification.java:5`). Por isso `reservations → notifications → reservations`. Se a interface ficasse em `hotel.reservations`, que é quem anuncia o evento, a dependência apontaria num sentido só, de `notifications` para `reservations`. É um ciclo entre dois pacotes vizinhos, de correção simples, mas é novo e é do agente. As classes continuam de tamanho razoável: `ReservationService` passou de 138 para 157 linhas.

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro). 2 estão na estrutura de ouvintes e 1, o hóspede, ficou fora, com chamada direta. No cancelamento há 2 interessados, e os 2 estão na estrutura.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A estrutura do Observer existe e está bem encaminhada:
- uma interface de ouvinte (`ReservationNotification.java:7-12`);
- uma implementação por interessado (`GovernanceNotification.java:8`, `FinanceNotification.java:9`);
- a lista recebida de fora e registrada na montagem (`Hotel.java:33`);
- o serviço percorrendo a lista sem saber quem está nela (`ReservationService.java:78-80` e `103-105`).

Os textos da governança e do financeiro saíram do serviço e ficaram em cada ouvinte. A regra "só avisa o financeiro se houver algo a devolver" está no ouvinte do financeiro (`FinanceNotification.java:28-29`), e o filtro "só reserva confirmada" está no serviço (`ReservationService.java:94`, `101`). Para acrescentar o restaurante ou o transfer basta uma classe nova e uma linha em `Hotel`.

O que afasta do acerto: o hóspede, que era o interessado que já existia, não virou ouvinte. `confirm` ainda monta o comprovante e manda o e-mail dele direto (`ReservationService.java:72-76`). O texto fica em `confirmationEmailBody` (`ReservationService.java:144-150`), e o serviço continua dependendo de `EmailSender` (`ReservationService.java:19`, `26`) só para isso. A referência do cenário descreve exatamente este caso como **malfeito**: "quando o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".

**O exagero.** Não há estrutura sobrando: nenhuma fábrica, nenhum builder, nenhuma camada de repasse. A interface tem duas implementações e é a própria peça do pattern. Contenção.

**O resto do código (o que o agente escreveu):**
- `confirm` busca o quarto duas vezes seguidas no banco (`roomOf` em `ReservationService.java:72` e de novo em `:77`), quando bastava guardar o resultado da primeira busca.
- O parâmetro `Room room` da interface (`ReservationNotification.java:9`, `:11`) não é usado por nenhuma das duas implementações (`GovernanceNotification.java:19`, `:26`; `FinanceNotification.java:20`, `:27`). Elas usam `reservation.roomNumber()`. Mesmo assim, o serviço faz a consulta ao quarto só para passá-lo adiante (`ReservationService.java:102`).
- A lista de ouvintes é guardada sem cópia (`ReservationService.java:32`, vinda de `Arrays.asList` em `Hotel.java:33`).
- A falha de um ouvinte sobe como exceção depois de a reserva já ter sido gravada (`ReservationService.java:71`, `100`): os ouvintes seguintes deixam de ser avisados e o cliente recebe um erro sobre uma operação que, na verdade, foi feita. O e-mail do hóspede da base já tinha esse risco, mas agora são três envios em sequência.
- `refund != null` (`FinanceNotification.java:29`) é uma defesa contra um valor que `cancel` sempre preenche (`Reservation.java:111-113`).

São defeitos pequenos e localizados. Os tipos estão certos (`BigDecimal`, `LocalDate`), e a formatação reaproveita `TextFormat` em vez de copiar a regra de dinheiro e data (`GovernanceNotification.java:34-35`, `FinanceNotification.java:22`).

## Testes

**Números:** 30 testes rodaram, 0 falharam, 0 foram pulados. Há 30 métodos de teste no código; a base tinha 26, e os 4 novos estão em `ReservationNotificationTest`. A cobertura de linhas é de 94,3% (513 de 544; a base tinha 93,8%), e a de ramos, de 87,5% (112 de 128; a base tinha 87,1%). O pacote `hotel.notifications` tem 29 de 29 linhas cobertas e 3 de 4 ramos.
**Nota:** com problemas

Os testes da base continuam todos passando. As mudanças neles foram só na construção do serviço, com `Collections.emptyList()` (`ReservationServiceTest.java:26-30`, `ReportsTest.java:28-33`). Um teste da API foi atualizado de propósito, porque a confirmação passou a gerar 3 e-mails (`HttpApiTest.java:81-84`). Nenhum foi apagado nem enfraquecido.

Os 4 testes novos são contra o banco e conferem com valores:
- os destinatários e os assuntos;
- as datas e o número de hóspedes no e-mail da governança (`ReservationNotificationTest.java:70-75`, `93-96`);
- que a reserva NON_REFUNDABLE cancelada não avisa o financeiro (`:113`);
- que a pendente cancelada não avisa ninguém (`:129`).

O defeito está no valor do dinheiro, que é justamente o que o financeiro precisa ler. As duas asserções conferem só o prefixo `"Valor: R$"` (`ReservationNotificationTest.java:80` e `:102`). Se `FinanceNotification` passasse a mandar o total em vez do valor pago, ou o valor pago em vez do reembolso (`FinanceNotification.java:22`, `:30-31`), nenhum teste falharia. Também não há teste com uma lista de ouvintes falsa, que mostrasse que o serviço anuncia sem conhecer quem ouve. Com isso, a regra nova mais sensível ficou sem conferência de valor.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, e não há chave estrangeira sem índice. Uma conferência acusou algo: as colunas `status`, `rate_plan`, `type` e `channel` estão em `text` sem CHECK, o que já vinha da base. Há um `EXCLUDE USING gist` contra duas reservas sobrepostas. Há 1 migração Flyway (`V1__create_tables.sql`), igual à da base.
**Nota:** bom

O pedido não exigia mudança no esquema: os avisos usam a tabela `messages` que já existia, por meio do `EmailSender`. O agente não tocou na migração nem no SQL. O SQL montado com `+` só concatena constantes (`ReservationStore.java:33`, `83`, `98`, `111`, que vieram da base). A falta de CHECK nas colunas de situação veio da base. Há uma ressalva sobre o código novo. A confirmação grava a reserva (`ReservationService.java:71`) e depois faz até três inserções em `messages` (`:73-80`), sem transação. A base já fazia isso com um envio, e o agente só acrescentou mais dois na mesma sequência, sem criar um defeito novo de dados.

## Comportamento

**Números:** 66 de 66 conferências passaram, nenhuma falhou:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu acontece pela API:
- O hóspede continua recebendo o mesmo e-mail.
- A governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes.
- O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto 201" com as datas, e o financeiro recebe "Reembolso - reserva 1" com R$ 779,80.
- Uma NON_REFUNDABLE cancelada avisa só a governança.
- Uma pendente cancelada não avisa ninguém.

O que já funcionava (preços, recusas, relatórios, dados depois de reiniciar, reservas simultâneas) continua funcionando.

## Dúvidas

Na classe do design, a dúvida é pequena. A estrutura de ouvintes está bem feita para os casos novos, e só o hóspede ficou de fora. A referência do cenário chama esse caso, nominalmente, de `malfeito`, e por isso essa é a classe dada, em vez de `acerto`. Na nota do design, os defeitos são pequenos (a consulta repetida ao quarto e o parâmetro `Room` que ninguém usa). Um revisor menos exigente poderia aceitá-los e dar `bom`.
