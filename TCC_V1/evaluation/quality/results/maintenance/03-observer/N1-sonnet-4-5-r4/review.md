# Revisão: manutenção, cenário 03

## Visão geral

O agente mexeu pouco na base e de forma focada. O projeto continua com 7 pacotes. A requisição entra por `hotel.web.HttpApi` (Javalin), passa para `hotel.reservations.ReservationService` (as regras) e chega a `ReservationStore`, `Rooms` e `SentMessages` (JDBC sobre PostgreSQL, com o esquema criado pela migração Flyway `V1`). Os e-mails não saem de verdade: ficam gravados na tabela `messages`. Para atender o pedido, o agente criou a interface `ReservationEventListener` com três implementações (`GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`), e o serviço percorre a lista de ouvintes na confirmação e no cancelamento. O comportamento está todo certo: 66 de 66 conferências passaram, e os 30 testes passam. O problema mais sério é de design. A lista de ouvintes é montada com `new` dentro do construtor do serviço (`src/main/java/hotel/reservations/ReservationService.java:25-28`) e não na montagem do sistema (`Hotel`). Por isso, quando o restaurante ou o transfer entrarem ou saírem, alguém vai ter de mudar o `ReservationService`, que é exatamente o que o pedido queria evitar. Além disso, a regra "reserva que não foi confirmada não avisa ninguém" não vem no evento: cada ouvinte deduz isso por conta própria, olhando o estado da reserva.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles (`measures/dependencies.md`), iguais aos da base (`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. A única diferença entre as medições está em `hotel.reservations`, que passou de 13 classes e 508 linhas para 17 classes e 631 linhas. A maior classe continua sendo `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **O que ficou como estava:** a borda HTTP, as regras e o acesso a dados continuam separados como na base. O agente não criou dependência nova entre pacotes nem ciclo novo. Os ouvintes moram em `hotel.reservations` e só usam `hotel.messages.EmailSender` e `hotel.documents.TextFormat`/`ReservationReceipt`, de que o pacote já dependia. O ciclo `documents` ↔ `reservations` veio da base.
- **O que piorou:**
  - **A montagem:** na base, todas as peças eram criadas e ligadas em `Hotel` (`src/main/java/hotel/Hotel.java:20-27`), e o serviço recebia o `EmailSender` pronto. Agora o serviço cria os próprios colaboradores por dentro (`src/main/java/hotel/reservations/ReservationService.java:25-28`). Ele continua recebendo um `EmailSender` (`ReservationService.java:21`) só para passá-lo aos ouvintes que ele mesmo instancia. Os ouvintes são `final class` sem `public` (`GuestNotifier.java:8`, `HousekeepingNotifier.java:9`, `FinanceNotifier.java:9`), então `Hotel` nem conseguiria registrá-los.
  - **Uma dependência dentro do pacote:** `GuestNotifier` lê `ReservationService.HOTEL_NAME` (`GuestNotifier.java:21` e `:23`). Uma constante de apresentação ficou dentro do serviço e o ouvinte depende dele.

São defeitos localizados, sem ciclo novo, mas o critério "montagem num lugar só" deixou de valer.

## Design de baixo nível

**Números:** há 5 casos de aviso, todos dentro da estrutura e nenhum fora dela: o hóspede na confirmação (o caso que já existia), a governança na confirmação e no cancelamento, e o financeiro na confirmação e no cancelamento. O serviço não monta mais nenhum e-mail (`confirmationEmailBody` foi removido). Mas os 3 ouvintes são criados com `new` dentro do serviço (`ReservationService.java:25-28`), e nenhum é registrado de fora.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Observer:**
- A interface `ReservationEventListener` (`src/main/java/hotel/reservations/ReservationEventListener.java:6-13`) tem os eventos `onConfirmed` e `onCancelled`, e há uma implementação por interessado.
- O e-mail do hóspede, que já existia, também virou ouvinte (`GuestNotifier.java:17-28`), e o texto continua igual ao da base.
- O serviço só percorre a lista nos dois eventos (`ReservationService.java:69-71` e `:92-94`) e não monta mais nenhum e-mail.

**O que afasta do Observer:**
- **A lista não vem de fora.** A referência pede que o serviço receba a lista e não conheça quem é avisado, com cada ouvinte registrado na montagem do sistema. Aqui é o próprio serviço que escolhe quem é avisado, com `List.of(new GuestNotifier(...), new HousekeepingNotifier(...), new FinanceNotifier(...))` em `ReservationService.java:25-28`. O pedido diz que "o restaurante e o transfer entram e saem conforme os contratos", e cada entrada ou saída obriga a editar e recompilar o `ReservationService`. Ou seja, a estrutura existe, mas o serviço continua sendo o lugar que sabe quem recebe os avisos. Por isso a classe é `malfeito`, e não `acerto` (veja as Dúvidas).
- **O evento não diz o que aconteceu.** `onCancelled` não informa se a reserva estava confirmada, então cada ouvinte deduz isso pelo estado depois do cancelamento:
  - `HousekeepingNotifier.java:30` usa `amountPaid().signum() > 0` como sinônimo de "estava confirmada";
  - `FinanceNotifier.java:29` usa `refund() != null && refund().compareTo(BigDecimal.ZERO) > 0`, que mistura "estava confirmada" com "há algo a devolver".

  Um ouvinte novo (o restaurante, o transfer) vai ter de repetir essa dedução. Se ela for feita errado, o restaurante passa a ser avisado do cancelamento de uma reserva pendente.

**Exagero:** não há estrutura sem necessidade. A interface tem três implementações e é o próprio pattern. Não há fábrica, builder nem camada que só repassa.

**O resto do código:** o que o agente acrescentou é curto e legível, mas com estes defeitos:
- **A dedução de "estava confirmada"**, descrita acima e escrita de dois jeitos diferentes (`HousekeepingNotifier.java:30` e `FinanceNotifier.java:29`).
- **Endereços repetidos:** os endereços da governança e do financeiro aparecem como texto solto, cada um repetido duas vezes (`HousekeepingNotifier.java:20` e `:33`, `FinanceNotifier.java:21` e `:31`).
- **Um aviso que falha derruba a operação:** não há isolamento entre os ouvintes. Se um aviso falhar (um erro de banco ao gravar a mensagem), os ouvintes seguintes não rodam, e a confirmação, que já foi gravada em `ReservationService.java:67`, responde erro. A base tinha o mesmo risco com um e-mail só, mas agora ele vale para três.
- **Javadoc desatualizado:** o comentário de `ReservationService.java:60` ainda diz que só o hóspede recebe e-mail.

O resto do código (os tipos `BigDecimal`/`LocalDate`/`enum`, os erros e os nomes) veio da base e está bom.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 30 métodos escritos; a base tinha 26, todos mantidos, e o agente acrescentou 4. A cobertura de linhas foi de 93,8% para 94,3% (514 de 545) e a de ramos, de 87,1% para 87,3% (110 de 126). Em `hotel.reservations` ficaram cobertas 252 de 265 linhas e 58 de 62 ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** bom

**O que os testes novos conferem** (em `src/test/java/hotel/reservations/ReservationServiceTest.java`), sempre com os valores do pedido:
- `:168`: na confirmação, a governança recebe o assunto "Reserva confirmada - quarto 201", com entrada 10/03/2027, saída 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva N" com R$ 779,80.
- `:191`: no cancelamento de uma reserva confirmada, os dois avisos, com as datas e o valor.
- `:216`: na `NON_REFUNDABLE` cancelada, a governança é avisada e o financeiro não.
- `:230`: a reserva pendente cancelada não manda nada (`assertEquals(0, sent.size())`).

**A pergunta da mutação:**
- Trocar a condição de `HousekeepingNotifier.java:30`, ou tirar `> 0` de `FinanceNotifier.java:29`: o teste de `:230` ou o de `:216` falha.
- Trocar o valor ou o formato: os testes que comparam com `R$ 779,80` falham.
- Fica sem rede um ouvinte que mandasse algo a mais no cancelamento de uma reserva confirmada (por exemplo, o hóspede): os testes de `:191` e `:216` filtram as mensagens e não contam o total.

**Os testes da base:** foram ajustados, não enfraquecidos. `ReservationServiceTest.java:91` e `:109` passaram a esperar 3 mensagens e continuam conferindo o e-mail do hóspede com "Desconto: R$ 37,48" e "Total: R$ 212,42". `src/test/java/hotel/web/HttpApiTest.java:81` espera 3 mensagens e procura a do hóspede.

**O nível:** os testes rodam contra o banco de verdade e pela API, sem mocks. Como a lista de ouvintes não é injetável, não há como testar o serviço com um ouvinte de teste. Isso é consequência do defeito de design, não falta dos testes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. A chave estrangeira tem índice e há `EXCLUDE USING gist` contra duas reservas na mesma noite. A migração `V1__create_tables.sql` está igual à da base, e o agente não criou migração nova (`measures/database.md`). A conferência acusou as colunas de situação e de tipo sem `CHECK` (`reservations.status`, `reservations.rate_plan`, `rooms.type` e `messages.channel`), o que veio da base.
**Nota:** bom

**O que o agente mexeu:** nada no banco, e a mudança não precisava. Os novos avisos são gravados pela mesma `SentMessages.add`, com SQL parametrizado (`src/main/java/hotel/messages/SentMessages.java:21-23`), e as mensagens continuam depois de reiniciar (conferência do roteiro).

**O que veio da base:** a falta de `CHECK` nos campos de situação (`src/main/resources/db/migration/V1__create_tables.sql:28` e `:33`) e a gravação da reserva e das mensagens em conexões separadas, sem transação (`ReservationStore.java:64-65` e `SentMessages.java:21`). Com três avisos em vez de um, a janela de estado parcial ficou um pouco maior, mas o padrão é o da base.

## Comportamento

**Números:** 66 de 66 conferências passaram (`measures/behavior.md`):
- a parte comum do sistema: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **Na confirmação:** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80.
- **No cancelamento de uma reserva confirmada:** a governança recebe "Reserva cancelada - quarto 201", com as datas que ficaram livres, e o financeiro recebe "Reembolso - reserva 1" com R$ 779,80.
- **Na `NON_REFUNDABLE` cancelada:** só a governança é avisada.
- **Na reserva pendente cancelada:** ninguém é avisado.

O que já funcionava continua funcionando: preços, recusas, check-in e check-out, relatórios, a persistência depois de reiniciar e a proteção contra duas reservas ao mesmo tempo.

## Dúvidas

- **A classe do pattern ficou entre `malfeito` e `acerto`.** A estrutura do Observer está inteira e todos os casos passam por ela, inclusive o do hóspede. Pesou para `malfeito` o critério da referência ("a lista de ouvintes, que ele recebe de fora", "registrado na montagem do sistema"), que não foi cumprido: o serviço instancia os ouvintes em `ReservationService.java:25-28`. Pesou também o pedido, que diz explicitamente que quem é avisado vai mudar com o tempo. Um revisor que considere a injeção da lista um detalhe de montagem daria `acerto`.
- **O mesmo defeito aparece em duas colunas, por ângulos diferentes.** Na arquitetura ele conta como montagem fora de `Hotel`; no design, como o serviço que ainda conhece os interessados. A nota do design se apoia sobretudo na dedução implícita de "estava confirmada" dentro dos ouvintes.
