# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages` (o `EmailSender` grava os e-mails na tabela
`messages`), `hotel.documents`, `hotel.reports`. A montagem fica em `hotel.Hotel`, e o esquema vem de uma migração do
Flyway. Uma requisição de confirmação passa por `HttpApi` → `ReservationService.confirm` → `ReservationStore.update` e
`EmailSender.send`. O agente mexeu pouco e em lugares certos. Criou a interface `ReservationListener`
(`src/main/java/hotel/reservations/ReservationListener.java:9`) e um ouvinte, `StaffNotifications`, num pacote novo
(`src/main/java/hotel/notifications/StaffNotifications.java:10`). O serviço recebe a lista de ouvintes de fora
(`ReservationService.java:31-37`) e a percorre (`ReservationService.java:114-122`). Quem monta essa lista é o `Hotel`
(`Hotel.java:29`). O comportamento pedido funciona inteiro (66 de 66 conferências) e os 30 testes passam. O mais sério
está no pattern, que ficou pela metade: o e-mail do hóspede continua sendo uma chamada direta dentro do serviço
(`ReservationService.java:81-85`), e um único ouvinte cuida ao mesmo tempo da governança e do financeiro.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 22 dependências entre eles (a base tinha 17). Há 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote novo,
`hotel.notifications`, tem 1 classe e 53 linhas, depende de 4 pacotes e é usado só por `hotel`. A maior classe passou
a ser `ReservationService`, com 171 linhas (eram 138).
**Nota:** bom

As dependências apontam na direção certa. A interface do ouvinte mora em `hotel.reservations`
(`ReservationListener.java:1`), e a implementação em `hotel.notifications` depende de `reservations`, `messages`,
`documents` e `rooms`. O serviço não conhece `hotel.notifications`: nenhum import dele aparece em
`ReservationService.java:3-11`, e por isso não surgiu ciclo novo. A ligação das peças continua num lugar só
(`Hotel.java:27-31`). O `EmailSender` agora é criado uma vez e compartilhado (`Hotel.java:27`). O pacote novo não usa
Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`). A borda HTTP não mudou e continua sem JDBC.
O ciclo `documents`↔`reservations` vem da base e o agente não o piorou. `ReservationService` cresceu 33 linhas, mas
continua com tamanho razoável.

## Design de baixo nível

**Números:** há 3 interessados no sistema: o hóspede, a governança e o financeiro. Ficaram em 1 ouvinte
(`StaffNotifications`) dois deles, a governança e o financeiro, e esse ouvinte serve aos dois eventos. O hóspede ficou
fora da estrutura, numa chamada direta (`ReservationService.java:82-85`). O cancelamento também usa a lista de
ouvintes (`ReservationService.java:107-109`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

**O que aproxima do Observer:** há uma interface de ouvinte com dois eventos, `confirmed` e
`confirmedReservationCancelled`, ambos com implementação vazia por padrão (`ReservationListener.java:11-17`). O serviço
recebe a lista pronta (`ReservationService.java:31-37`, com `List.copyOf`) e avisa os ouvintes nos dois pontos
(`ReservationService.java:86` e `:108`) sem saber quem eles são. Os textos da governança e do financeiro saíram do
serviço e foram para o ouvinte (`StaffNotifications.java:22-52`). A regra "só avisa o financeiro se houver reembolso"
também ficou no ouvinte (`StaffNotifications.java:46`). Um ouvinte que falha é registrado no log e não impede os
outros (`ReservationService.java:114-122`). Para pôr o restaurante ou o transfer, basta escrever uma classe nova e
acrescentá-la em `Hotel.java:29`.

**O que afasta (por isso `malfeito`):**
- O e-mail do hóspede continua montado e enviado à mão dentro de `confirm` (`ReservationService.java:81-85`, com o
  texto em `confirmationEmailBody`, `:158-164`). A referência descreve exatamente este caso como malfeito: o hóspede
  tem chamada direta e só a governança e o financeiro viraram ouvintes. O serviço continua dependendo do
  `EmailSender` (`ReservationService.java:23`). Um efeito disso: o envio ao hóspede não tem a proteção do
  `notifyListeners`. Se ele falhar depois do `store.update` (`:80`), a equipe não é avisada.
- `StaffNotifications` reúne dois interessados, a governança e o financeiro, com os dois endereços fixos em
  `StaffNotifications.java:12-13`. A referência pede uma implementação por interessado. Do jeito que está, tirar ou
  trocar só o financeiro exige editar essa classe em vez de mexer na lista do `Hotel`. O próprio pedido diz que quem é
  avisado vai mudar com o tempo.

**Exagero:** nenhum. A única interface é a que o pattern pede. O construtor antigo de 4 argumentos ficou para os
testes da base, que continuam usando-o (`ReservationService.java:26-29`, chamado em `ReservationServiceTest.java:24` e
`ReportsTest.java:27`). Ele só repassa uma lista vazia, sem acrescentar estrutura.

**O resto do código:** o código novo é curto e claro. Usa os tipos certos (`BigDecimal` com
`TextFormat.money`, `LocalDate` com `TextFormat.date`, reaproveitados de `hotel.documents` em vez de copiados) e nomes
que dizem o que fazem. O `catch (RuntimeException)` em `ReservationService.java:118` é intencional: está documentado
na linha 113 e registra a causa no log. Ficam detalhes pequenos, que não mudam a nota:
- `roomOf` consulta o banco de novo para cada ouvinte e repete a consulta já feita em `confirm`
  (`ReservationService.java:81` e `:86`, `:108`);
- o `static final Logger` foi declarado no meio dos campos de instância (`ReservationService.java:21`);
- o comentário de `EmailSender` ainda diz "Sends e-mails to guests" (`EmailSender.java:3`).

## Testes

**Números:** 30 testes rodaram, sem falha, erro nem pulado, e há 30 métodos escritos (a base tinha 26, todos
mantidos). A cobertura é de 93,6% das linhas (511 de 546) e 87,3% dos ramos (110 de 126); na base era 93,8% e 87,1%.
`hotel.notifications` tem 25 de 25 linhas e 3 de 4 ramos cobertos.
**Nota:** com problemas

**O que está bom:** a classe nova `StaffNotificationsTest` roda contra o banco de verdade e confere quatro regras do
pedido:
- a confirmação com assunto, endereço, as duas datas e o valor pago (`StaffNotificationsTest.java:51-63`);
- o cancelamento de reserva confirmada, com o reembolso (`:66-76`);
- o plano NON_REFUNDABLE, em que o financeiro não é avisado (`:79-86`, pelo total de 4 mensagens);
- a reserva pendente cancelada, que não avisa ninguém (`:89-94`).

A única mudança num teste da base é `HttpApiTest.java:81`, de 1 para 3 mensagens, que é exatamente o que o pedido
muda, e o teste continua conferindo que a primeira mensagem é a do hóspede. Nenhum teste foi enfraquecido sem motivo.
Pela pergunta da mutação, trocar `signum() > 0` por `>= 0` faz `noRefundMeansNoMessageToFinance` falhar, e apagar um
dos envios faz falhar a contagem.

**O que fica sem rede:**
- O número de hóspedes é conferido com `contains("2")` (`StaffNotificationsTest.java:59`). O corpo sempre tem um "2"
  no quarto 201 e no ano 2027, então apagar a linha "Hóspedes" de `StaffNotifications.java:30` não derrubaria nenhum
  teste.
- O aviso de cancelamento à governança só confere a data de entrada (`StaffNotificationsTest.java:72-73`). Trocar ou
  apagar a data de saída em `StaffNotifications.java:44` passaria.
- A regra "um ouvinte que falha não desfaz a reserva nem cala os outros" (`ReservationService.java:114-122`) não tem
  teste com um ouvinte que lança exceção.
- Nenhum teste registra mais de um ouvinte, que é justamente a extensão que o pedido anuncia.

## Banco

**Números:** 3 tabelas, todas com chave primária, criadas por 1 migração do Flyway, `V1__create_tables.sql`, que está
igual à da base. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira está sem índice, e nenhuma data
está guardada como texto. Há a restrição `EXCLUDE USING gist` contra duas reservas do mesmo quarto em noites que se
sobrepõem. A única coisa que as conferências acusam é que as colunas `status`, `rate_plan`, `type` e `channel` são
texto sem CHECK, e isso já estava na base.
**Nota:** bom

O agente não mexeu no esquema nem no acesso ao banco: o `changes.patch` não toca `ReservationStore`, `SentMessages`
nem a migração. Os avisos novos são linhas a mais na tabela `messages`, pelo mesmo `EmailSender` (`EmailSender.java:12-13`),
e continuam lá depois de reiniciar o serviço (conferência "as mensagens continuam lá"). O SQL montado com `+` usa só
constantes (`ReservationStore.java:33, 83, 98, 111`). Um defeito vem da base: não há transação ligando o
`store.update` e a gravação dos e-mails (`ReservationService.java:80-86`; o script conta 0 `setAutoCommit(false)`).
Os avisos novos herdam esse risco, mas o agente não o criou. A falha de um ouvinte fica registrada no log, e a
reserva continua confirmada.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- O hóspede continua recebendo o mesmo e-mail.
- Na confirmação, a governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes, e o
  financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto 201", com as datas, e o
  financeiro recebe "Reembolso - reserva 1", com R$ 779,80.
- No plano não reembolsável, só a governança é avisada.
- A reserva pendente cancelada não manda nada.

O resto do sistema continua igual: preços, recusas, relatórios, os dados depois de reiniciar e as duas reservas
simultâneas.

## Dúvidas

A classe do pattern: o caso se encaixa na descrição de malfeito da referência (o hóspede com chamada direta e só a
equipe como ouvintes). Mesmo assim, a estrutura que existe está bem feita e é extensível, e alguém poderia achar que
`malfeito` pesa demais. Fiquei com a letra da referência. A nota de design (`bom`) considera só o resto do código. Os
detalhes ali (a consulta repetida do quarto, o lugar do `Logger`) são pequenos, mas um revisor mais rigoroso poderia
dar `com problemas`.
