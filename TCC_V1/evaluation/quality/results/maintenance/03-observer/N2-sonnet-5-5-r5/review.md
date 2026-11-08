# Revisão: manutenção, cenário 03

## Visão geral

O sistema da base tem oito pacotes depois da mudança: `hotel.web` (as rotas do Javalin em `HttpApi`), que chama
`hotel.reservations` (`ReservationService` com as regras, `ReservationStore` com o SQL), `hotel.rooms`,
`hotel.reports`, `hotel.documents` (comprovante e formatação), `hotel.messages` (o `EmailSender`, que grava cada
e-mail na tabela `messages`) e a montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por
uma migração do Flyway. Para atender o pedido, o agente criou a interface `ReservationListener` em `hotel.reservations`
(`final/src/main/java/hotel/reservations/ReservationListener.java:6`) e o pacote novo `hotel.notifications`, com um
ouvinte por interessado: `GuestNotice` (o e-mail do hóspede, com o texto igual ao da base), `HousekeepingNotice` e
`FinanceNotice`. O `ReservationService` recebe a lista de ouvintes de fora (`ReservationService.java:15-23`) e a
percorre na confirmação (`:64-66`) e no cancelamento (`:88-90`), e a lista é montada em `Hotel.java:30-31`. Os 30
testes passam, e o roteiro do comportamento passou em 66 de 66 conferências. Não há defeito sério. O ponto mais fraco
são os testes novos: algumas asserções não conferem o valor, como o do reembolso.

## Arquitetura

**Números:** 8 pacotes (7 na base), 21 dependências entre eles (17 na base), 1 ciclo (`hotel.documents` ↔
`hotel.reservations`), o mesmo que a base já tinha. A dependência `hotel.reservations -> hotel.messages` deixou de
existir. As novas são `hotel.notifications -> documents/messages/reservations/rooms` e `hotel -> hotel.notifications`.
**Nota:** bom

A mudança deixou as dependências na direção certa. A interface do ouvinte fica no pacote das regras
(`reservations/ReservationListener.java:6`), e quem manda e-mail fica fora dele (`notifications/*`), de modo que
`ReservationService` não importa mais o `EmailSender` (`ReservationService.java:3-7`; na base, `base/.../ReservationService.java:4`).
A montagem continua num lugar só (`Hotel.java:26-36`). O pacote `hotel.notifications` não usa Javalin nem JDBC (pela
tabela de bibliotecas em `measures/dependencies.md`). O ciclo `documents` ↔ `reservations` veio da base
(`ReservationService.java:3` usa `ReservationReceipt` no check-out e na segunda via, `:105-121`), e o agente não o
piorou. Nenhuma classe cresceu: `ReservationService` caiu de 138 para 134 linhas.

## Design de baixo nível

**Números:** 3 interessados, todos na estrutura: o hóspede, que já existia, a governança e o financeiro. São 2
eventos (confirmação e cancelamento). Nenhum e-mail fica montado no serviço, e não há nenhuma chamada direta ao
`EmailSender` fora dos ouvintes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern está bem aplicado. Há uma interface de ouvinte (`ReservationListener.java:9` e `:13`, com métodos `default`
para que cada ouvinte implemente só os eventos que lhe interessam) e uma implementação por interessado
(`GuestNotice.java:21`, `HousekeepingNotice.java:22,32`, `FinanceNotice.java:22,30`). O serviço só percorre a lista
que recebe de fora (`ReservationService.java:64-66`, `:88-90`) e não sabe quem é avisado. O e-mail do hóspede, que
na base era chamado direto com `confirmationEmailBody`, também virou ouvinte, com o mesmo texto
(`GuestNotice.java:22-30`). A lista é registrada na montagem (`Hotel.java:30-31`): o restaurante e o transfer entram
com uma classe nova e uma linha na montagem, sem mexer no serviço. Os dois eventos têm ouvintes, e não sobrou
nenhum caminho feito à mão.

Não há exagero: a interface tem três implementações, e não apareceu nenhuma fábrica, builder ou camada de repasse.
`List.copyOf` (`ReservationService.java:23`) evita que alguém mude a lista por fora.

O resto do código continua no nível da base. As datas e os valores usam `TextFormat`, o mesmo do comprovante
(`HousekeepingNotice.java:27-29`, `FinanceNotice.java:26,37`), sem duplicar a formatação. Uma escolha discutível: o
serviço anuncia todo cancelamento e passa o `statusBefore`, e cada ouvinte repete a regra "só se estava confirmada"
(`HousekeepingNotice.java:33`, `FinanceNotice.java:31`). Um ouvinte futuro que esqueça esse filtro avisaria sobre
reservas pendentes. Está documentado na interface (`ReservationListener.java:12`) e é uma decisão defensável, porque
o pedido diz "pode ser cancelada sem avisar ninguém". Outros detalhes menores: uma linha em branco dupla
(`ReservationService.java:127-128`); e, quando um ouvinte falha, os seguintes não rodam, e a requisição devolve erro
depois de a reserva já ter sido gravada (`:62-66`, `:86-90`). Esse segundo comportamento já existia na base com o
e-mail do hóspede.

## Testes

**Números:** 30 testes rodaram, 0 falharam (26 na base, todos mantidos). 30 métodos escritos (4 novos). Cobertura de
linhas 94,1% (515/547; base 93,8%) e de ramos 88,1% (111/126; base 87,1%). `hotel.notifications`: 38/38 linhas e
6/6 ramos.
**Nota:** com problemas

Os quatro testes novos (`ReservationServiceTest.java:107`, `:124`, `:142`, `:153`) cobrem os casos do pedido: os
avisos da confirmação, o cancelamento de uma reserva confirmada, a NON_REFUNDABLE sem aviso ao financeiro e a
pendente sem aviso nenhum. Os assuntos são conferidos com o texto exato (`:113`, `:118`, `:132`, `:136`). O agente
também ajustou os testes da base sem enfraquecê-los: o e-mail do hóspede continua conferido com os valores
(`:93-100`), agora filtrado pelo destinatário.

Parte das asserções é fraca:
- O valor do reembolso só é conferido com `contains("R$ ")` (`ReservationServiceTest.java:137`). Se `FinanceNotice`
  escrevesse um valor errado no corpo (`R$ 0,00`, ou o total no lugar do reembolso), nenhum teste falharia.
- O aviso de cancelamento à governança só confere a data de entrada (`:133`). Apagar a data de saída do corpo
  (`HousekeepingNotice.java:40-41`) não quebra nada.
- O valor pago no e-mail do financeiro é comparado com o próprio valor do serviço formatado por `TextFormat`
  (`:119-120`), e não com um número fixo como "R$ 779,80". O teste pega uma troca de campo, mas não um erro de
  formatação.
- O teste pela API só conta que saíram 3 mensagens e supõe que a do hóspede é a primeira (`HttpApiTest.java:81-82`).
  Isso amarra o teste à ordem dos ouvintes, que o pedido diz não importar.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`). Uma migração (`V1__create_tables.sql`), igual à da base.
As conferências não acusaram nada: todas as tabelas têm chave primária, o dinheiro é `numeric(10,2)`, a chave
estrangeira tem índice, as datas não estão em texto e há um `EXCLUDE` contra reserva sobreposta. Ficam só as colunas
de situação e tipo em `text` sem CHECK, como na base. Não há transação explícita (0 `setAutoCommit(false)`).
**Nota:** bom

O pedido não exigia mudança no esquema, e o agente não mexeu nele: a migração da base está intacta, e os e-mails novos
são gravados pela mesma `SentMessages.add`, com SQL parametrizado (`messages/SentMessages.java:39-54`). Os defeitos
apontados vêm da base. O mais próximo da mudança: a gravação da reserva e a das mensagens não ficam numa transação
só (`ReservationService.java:62-66`, `:86-90`). Isso já acontecia na confirmação da base, e agora também vale para o
cancelamento. Se a gravação de uma mensagem falhar, a reserva fica cancelada ou confirmada sem o aviso, mas nenhum
dado da reserva se perde ou se corrompe.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum 46/46, dados depois de reiniciar 3/3, cenário 03 15/15,
reservas simultâneas 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. Na confirmação, o hóspede recebe o mesmo e-mail de antes, a governança
recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento
recebido - reserva 1" com R$ 779,80. No cancelamento de uma reserva confirmada, a governança recebe as datas
liberadas e o financeiro recebe "Reembolso - reserva 1" com R$ 779,80. Na NON_REFUNDABLE cancelada, só a governança
é avisada, e a reserva pendente cancelada não gera nenhuma mensagem. O que já funcionava continua igual.

## Dúvidas

- Na nota do design, pesei como escolha defensável o filtro "só se estava confirmada" repetido em cada ouvinte
  (`HousekeepingNotice.java:33`, `FinanceNotice.java:31`), em vez de o serviço anunciar um evento só para
  cancelamentos de reservas confirmadas. Um revisor mais rígido poderia ver nisso uma duplicação que o próximo
  ouvinte (restaurante, transfer) vai ter de copiar e dar `com problemas`.
- O agente não atualizou o `README.md` (é igual ao da base) para contar dos avisos à equipe. O pedido não exigia isso,
  e não pesei.
