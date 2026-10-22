# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto. `hotel.web.HttpApi` recebe a requisição e chama `hotel.reservations.ReservationService`, que guarda os dados pelo `ReservationStore` (JDBC, PostgreSQL, esquema por Flyway). Os e-mails passam pelo `hotel.messages.EmailSender` e ficam na tabela `messages`. A montagem fica em `hotel.Hotel`. O agente mudou pouco. Criou o pacote `hotel.notifications`, com a interface `ReservationNotifier` e duas implementações (`GovernanceNotifier`, `FinanceNotifier`). Pôs no serviço uma lista de ouvintes, preenchida por `addNotifier`, que ele percorre na confirmação e no cancelamento de uma reserva confirmada (`ReservationService.java:79-81`, `:102-107`). Os dois ouvintes são registrados em `Hotel.java:29-30`. Fez também 4 testes novos. O sistema faz tudo o que o pedido descreve (66 de 66 conferências passam). Há dois defeitos no desenho. O e-mail do hóspede ficou como chamada direta no serviço (`ReservationService.java:75-78`), fora da lista de ouvintes, então o Observer saiu pela metade. E a interface ficou no pacote `notifications`, o que criou um ciclo novo entre `reservations` e `notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 23 dependências entre eles (a base tinha 17). São 3 ciclos pelo `tsort`, contra 1 na base. O ciclo `documents ↔ reservations` já vinha da base. Os novos são `reservations ↔ notifications` e o ciclo de três pacotes `documents → reservations → notifications`.
**Nota:** com problemas

As camadas da base não mudaram. A borda HTTP continua fina, as regras não usam Javalin e o SQL continua em `ReservationStore`/`SentMessages`/`Rooms`. A montagem ficou num lugar só (`Hotel.java:26-30`), e lá os ouvintes são criados e registrados, como deve ser. O problema é onde a interface do ouvinte ficou: `hotel.notifications.ReservationNotifier`, um pacote cujas implementações dependem de `hotel.reservations.Reservation` (`FinanceNotifier.java:5`, `GovernanceNotifier.java:5`). Ao mesmo tempo, o serviço importa a interface (`ReservationService.java:5`). O resultado é um ciclo novo entre dois pacotes vizinhos. Se a interface (o evento que o serviço anuncia) ficasse em `hotel.reservations`, as dependências apontariam num sentido só, de `notifications` para `reservations`. O ciclo é pequeno e fácil de corrigir, mas foi o agente que o criou. O `ReservationService` cresceu de 138 para 158 linhas, um tamanho que ainda não preocupa.

## Design de baixo nível

**Números:** há 3 interessados na confirmação (hóspede, governança e financeiro) e 2 interessados no cancelamento. Na estrutura de ouvintes estão 2 dos 3 da confirmação e os 2 do cancelamento. O hóspede ficou fora: ainda é uma chamada direta ao `EmailSender`, com o texto montado no próprio serviço.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima o código do Observer:
- uma interface de ouvinte com um método por evento (`ReservationNotifier.java:7-11`);
- uma implementação por interessado, cada uma com o seu destino e o seu texto (`GovernanceNotifier.java:19-32`, `FinanceNotifier.java:19-35`);
- o serviço percorre a lista sem saber quem está nela (`ReservationService.java:79-81`, `:104-106`);
- os ouvintes são registrados na montagem (`Hotel.java:29-30`);
- a regra "só avisa o financeiro se houver reembolso" mora no próprio ouvinte (`FinanceNotifier.java:29`), e não no serviço.

O que afasta, e é exatamente o caso que a referência chama de malfeito: o hóspede continua com chamada direta. Em `ReservationService.java:73-78`, o serviço ainda monta o comprovante e o e-mail do hóspede (`confirmationEmailBody`, `:145-151`) e continua dependendo do `EmailSender` (`:20`, `:28`). Por isso o serviço não está fechado para os interessados: se um dia o hóspede tiver que ser avisado do cancelamento, ou se o e-mail dele mudar, a mudança cai de novo no `ReservationService`.

Não há exagero. A interface tem duas implementações reais e o pedido anuncia mais (o restaurante, o transfer). Não aparece fábrica, builder nem camada de repasse.

O design do resto do que o agente escreveu:
- `roomOf(reservation)` é chamado duas vezes seguidas (`ReservationService.java:73` e `:74`), ou seja, duas consultas ao banco para o mesmo quarto. Bastava guardar o `room` antes e passá-lo ao comprovante.
- Há imports que não são usados: `ReservationStatus` e `BigDecimal` em `FinanceNotifier.java:6,8`, e `ReservationStatus` em `GovernanceNotifier.java:6`.
- A interface recebe um `Room` (`ReservationNotifier.java:8,10`). O `FinanceNotifier` não o usa, e o `GovernanceNotifier` só tira dele o número, que já está em `reservation.roomNumber()`. O próprio `GovernanceNotifier` usa os dois para a mesma coisa: `room.number()` no assunto (`:22`) e `reservation.roomNumber()` no corpo (`:36`).
- Cada endereço de e-mail aparece escrito duas vezes no mesmo ouvinte (`FinanceNotifier.java:22,31`, `GovernanceNotifier.java:21,29`).
- A lista de ouvintes é um `ArrayList` que pode mudar depois da construção, por `addNotifier` (`ReservationService.java:29,32-34`). Receber a lista no construtor deixaria o serviço imutável.

São defeitos pequenos e localizados. Nenhum torna o código difícil de mudar.

## Testes

**Números:** rodaram 30 testes, nenhum falhou nem foi pulado, e há 30 métodos escritos. A base tinha 26 testes, também todos passando. A cobertura de linhas foi de 93,8% para 94,4% (523 de 554), e a de ramos de 87,1% para 88,1% (111 de 126). O pacote novo, `hotel.notifications`, tem 36 de 36 linhas e 2 de 2 ramos cobertos.
**Nota:** bom

O agente fez `NotificationTest`, contra o banco, com 4 testes que conferem valores:
- a confirmação manda 3 e-mails, e o da governança traz o assunto com o quarto, as datas no formato `dd/MM/yyyy` e o número de hóspedes. O do financeiro traz o assunto com o número da reserva e `R$ 249,90` (`NotificationTest.java:42-60`);
- o cancelamento de uma reserva confirmada manda os assuntos certos, as datas e o reembolso (`:63-81`);
- a reserva pendente cancelada não manda nada (`:83-92`);
- o `NON_REFUNDABLE` cancelado não avisa o financeiro (`:95-106`).

Pela pergunta da mutação, as regras novas estão cobertas:
- trocar `signum() > 0` por `>= 0` (`FinanceNotifier.java:29`) quebra `:104`;
- apagar o `if (wasConfirmed)` (`ReservationService.java:102`) quebra `:91`;
- tirar um ouvinte da montagem quebra `HttpApiTest.java:81-84`, que confere a montagem real pela API.

O e-mail do hóspede continua conferido com valores em `ReservationServiceTest.java:94-96`. Nenhum teste da base foi apagado. O único mudado foi `HttpApiTest.java:81`, de 1 para 3 e-mails, que é a mudança pedida. Há duas fragilidades pequenas:
- os testes leem os e-mails por posição (`messages.get(1)`, `get(2)`, etc.), mas o pedido diz que a ordem não importa. Trocar a ordem do registro na montagem quebra testes sem quebrar nenhuma regra;
- nenhum teste confere o cancelamento pela API.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`) e 1 migração Flyway (`V1__create_tables.sql`), igual à da base. As conferências de chave primária, de dinheiro em `numeric(10,2)`, de chave estrangeira com índice e de datas em `date` não acusaram nada. O `EXCLUDE USING gist` continua protegendo contra duas reservas na mesma noite. A conferência das colunas de situação e de tipo acusou 4 colunas sem `CHECK` (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`).
**Nota:** bom

O agente não mexeu no esquema nem no acesso ao banco. A mudança não precisava disso, porque os avisos novos são linhas a mais na tabela `messages`, gravadas pelo mesmo `SentMessages` de antes. As migrações da base ficaram como estavam. Os pontos fracos vêm todos da base, e o agente não os piorou:
- as colunas de situação e de tipo em `text` sem `CHECK`;
- os relatórios que trazem a tabela de reservas inteira (`ReservationStore.java:108-111`, usados em `OccupancyReport.java:25` e `RevenueReport.java:25`);
- a gravação da reserva e a dos e-mails sem transação entre elas (`ReservationService.java:72-81`). O agente estendeu esse caminho com mais dois e-mails, mas o padrão já era o mesmo para o e-mail do hóspede.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu. A governança recebe "Reserva confirmada - quarto 201" com as datas de entrada e de saída e a quantidade de hóspedes. O financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80. No cancelamento de uma reserva confirmada saem os avisos "Reserva cancelada" e "Reembolso", e o de reembolso só sai quando há valor a devolver. A reserva pendente cancelada não avisa ninguém. O e-mail do hóspede continua igual, e tudo o que funcionava antes continua funcionando.

## Dúvidas

Na classe do design, escolhi entre `malfeito` e `acerto`. A estrutura de ouvintes está certa para a governança e o financeiro, mas a referência diz com todas as letras que é malfeito "quando o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". É esse o caso aqui (`ReservationService.java:75-78`), então a evidência sustenta `malfeito`.
