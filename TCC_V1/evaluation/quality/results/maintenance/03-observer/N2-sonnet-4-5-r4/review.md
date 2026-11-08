# Revisão: manutenção, cenário 03

## Visão geral

O sistema da base continua com a mesma forma: `hotel.web.HttpApi` (Javalin) recebe a requisição e chama
`hotel.reservations.ReservationService`, que aplica as regras e grava pelo `ReservationStore` (JDBC, PostgreSQL com
Flyway). Os e-mails não saem de verdade: o `EmailSender` grava cada um na tabela `messages`. A mudança do agente é
pequena e bem posta. Ele criou a interface `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:5`)
e três ouvintes no pacote novo `hotel.reservations.listeners`: o do hóspede, o da governança e o do financeiro. Agora
o `ReservationService` recebe a lista de ouvintes de fora e a percorre na confirmação e no cancelamento
(`ReservationService.java:63-65`, `:87-89`). A ligação é feita só em `Hotel.java:33-36`. As 66 conferências do
comportamento passaram, os 26 testes passam e o banco não mudou. Não há nada sério. Os pontos fracos são de design:
a regra "reserva pendente cancelada não avisa ninguém" ficou repetida dentro de cada ouvinte, por um parâmetro
booleano. Também falta isolar os ouvintes: se um falha, os seguintes não rodam. O README não foi atualizado e continua
dizendo que só o hóspede recebe e-mail.

## Arquitetura

**Números:** 8 pacotes (7 na base), 21 dependências entre eles (17 na base) e 1 ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote novo
`hotel.reservations.listeners` tem 3 classes e 111 linhas, depende de 4 pacotes e só `hotel` depende dele. A
dependência `hotel.reservations -> hotel.messages` sumiu.
**Nota:** bom

A borda HTTP e o acesso a dados não mudaram. A regra (`ReservationService`) deixou de conhecer o `EmailSender`: a base
tinha `hotel.reservations -> hotel.messages`, e a final não tem mais. Quem fala com o envio de e-mail são os ouvintes,
num subpacote que depende do núcleo e não o contrário (`listeners/FinanceListener.java:3-7`). A montagem continua
num lugar só, `Hotel.java:25-39`. O agente não criou ciclo novo: o ciclo `documents` ↔ `reservations` veio da base, e
o `ReservationService` ainda usa o `ReservationReceipt` no check-out e na segunda via (`ReservationService.java:104-121`).
Nenhuma classe cresceu além da conta. O `ReservationService` caiu de 138 para 132 linhas.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança e financeiro), os 3 como ouvintes e nenhum fora da estrutura. Os 2
eventos (confirmação e cancelamento) passam pela lista. O serviço não monta mais nenhum e-mail. Há 1 interface com 3
implementações.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Observer que a referência descreve:

- `ReservationListener` define os dois eventos (`ReservationListener.java:7-9`).
- O serviço recebe uma `List<ReservationListener>` no construtor (`ReservationService.java:17-23`), anuncia o evento
  e não sabe quem está ouvindo (`:62-65`, `:86-89`).
- O e-mail do hóspede, que na base era uma chamada direta com o texto em `confirmationEmailBody`, também virou ouvinte
  (`GuestNotificationListener.java:20-26`), e o texto ficou igual ao da base (`:33-38`).
- A governança e o financeiro são ouvintes separados (`HousekeepingListener.java`, `FinanceListener.java`), ligados em
  `Hotel.java:33-36`.

Para pôr o restaurante ou o transfer, basta escrever uma classe nova e acrescentá-la à lista. Nada no serviço fica
perguntando qual é o caso.

**O exagero.** Não há. A única interface nova tem três implementações e é o ponto de extensão que o pedido exige.
Não apareceu fábrica, registro dinâmico nem barramento de eventos.

**O resto do código.** Os defeitos são localizados, mas um revisor exigente pediria para mudar:

- **A regra espalhada nos ouvintes.** A regra "reserva que não foi confirmada é cancelada sem avisar ninguém" entra
  como um booleano no contrato (`ReservationListener.java:9`, `ReservationService.java:79`) e é repetida em cada
  ouvinte (`HousekeepingListener.java:29`, `FinanceListener.java:28`). O ouvinte do hóspede simplesmente o ignora
  (`GuestNotificationListener.java:29-30`). O dono avisou que vão entrar o restaurante e o transfer: o próximo ouvinte
  que esquecer de testar o booleano vai avisar sobre cancelamentos de reservas pendentes. Essa decisão caberia ao
  serviço, que só anunciaria o cancelamento de uma reserva confirmada, ou a um evento próprio.
- **Ouvintes sem isolamento.** A lista é percorrida depois do `store.update` e sem proteção
  (`ReservationService.java:61-65`, `:85-89`). Se o ouvinte da governança lançar uma exceção, o financeiro não é
  avisado, a reserva já está gravada como confirmada e a API responde erro. Na base havia um só e-mail, e o risco
  cresceu com o número de ouvintes.
- **Endereços repetidos.** Cada endereço aparece duas vezes, como texto solto: `HousekeepingListener.java:20` e `:31`,
  `FinanceListener.java:21` e `:30`. As linhas de datas também se repetem (`HousekeepingListener.java:22-23` e
  `:33-34`).
- **Método vazio.** O ouvinte do hóspede tem um método vazio (`GuestNotificationListener.java:28-30`), que um `default`
  na interface evitaria.
- **Detalhes menores.** O Javadoc de `ReservationService.java:54` continua dizendo que só o hóspede recebe e-mail. Os
  imports de `GuestNotificationListener.java:3-7` estão fora de ordem.

O dinheiro usa `BigDecimal` e o `TextFormat.money` da base, e as datas usam `LocalDate` com `TextFormat.date`. O
agente reaproveitou a formatação do comprovante em vez de copiá-la.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 26 métodos escritos, os mesmos da base (nenhum
novo). Cobertura de linhas: 94,3% (514 de 545; a base tinha 93,8%). Cobertura de ramos: 88,3% (113 de 128; a base
tinha 87,1%). O pacote `hotel.reservations.listeners` tem 38 de 38 linhas e 6 de 6 ramos cobertos
(`measures/tests.md`).
**Nota:** bom

O agente não criou métodos de teste: ele estendeu os que já existiam e conferiu a parte nova com valores.

- **Confirmação.** `ReservationServiceTest.java:99-113` confere as três mensagens:
  - para o hóspede, o assunto e "Total: R$ 212,42";
  - para a governança, "Reserva confirmada - quarto 101", "Entrada: 10/03/2027", "Saída: 11/03/2027" e
    "Hóspedes: 1";
  - para o financeiro, "Pagamento recebido - reserva N" e "Valor: R$ 212,42".
- **Cancelamento.** `ReservationServiceTest.java:139-155` confere:
  - o total de 9 mensagens, o que prova que a reserva pendente cancelada não avisou ninguém;
  - 2 avisos de cancelamento à governança;
  - 1 reembolso, com "Valor: R$ 249,90";
  - nenhum reembolso para a NON_REFUNDABLE.
- **Pela API.** `HttpApiTest.java:81-84` confere os três destinatários.

**A pergunta da mutação.** Apagar o `if (wasPreviouslyConfirmed)` da governança, ou o `signum() > 0` do financeiro,
muda a contagem de 9 e derruba o teste. Já trocar o assunto ou as datas do aviso de cancelamento da governança não
derruba nada: o teste só olha `startsWith("Reserva cancelada")` (`:142-144`) e não lê o corpo. Esse é o único buraco
da parte nova.

As asserções da base que tiveram de mudar (de 1 para 3 mensagens, `ReservationServiceTest.java:122`, e a troca de
`get(0)` por `contains` em `HttpApiTest.java:81-84`, porque a ordem não importa) foram adaptadas sem perder o que
conferiam. Nenhum teste da base foi apagado ou desligado. Falta um teste do serviço com um ouvinte de mentira, que
mostraria o contrato sem depender dos três reais, mas isso não é exigência.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. Não há chave estrangeira sem índice. A migração `V1__create_tables.sql` está
igual à da base, e o agente não acrescentou nenhuma. As conferências acusaram só as colunas de situação e de tipo sem
`CHECK` (`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`) e nenhuma transação explícita
(`measures/database.md`).
**Nota:** bom

O agente não tocou no banco, e o pedido não exigia: os avisos novos usam a tabela `messages` que já existia, pelo
`EmailSender` (`messages/SentMessages.java:35-50`, SQL com parâmetros). O `EXCLUDE USING gist` contra duas reservas
na mesma noite e os `CHECK` vieram da base. A falta de `CHECK` nas colunas de texto também veio da base. Um ponto que
o agente herdou e ampliou um pouco: a confirmação grava a reserva (`ReservationService.java:61`) e depois faz três
`INSERT` em `messages`, cada um na sua conexão, sem transação. Se um deles falhar, a reserva fica confirmada com
parte dos avisos. Na base o mesmo valia para um `INSERT` só, então isso não muda a nota.

## Comportamento

**Números:** 66 de 66 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.

(`measures/behavior.md`)
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:

- **Na confirmação:** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada -
  quarto 201", com entrada, saída e número de hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1" com
  R$ 779,80.
- **No cancelamento de uma reserva confirmada:** a governança recebe "Reserva cancelada - quarto 201" com as datas, e
  o financeiro recebe "Reembolso - reserva 1" com o valor.
- **NON_REFUNDABLE cancelada:** o financeiro não é avisado.
- **Reserva pendente cancelada:** não manda nada.

O que já funcionava continua funcionando, inclusive os relatórios, os dados depois de reiniciar e a recusa da segunda
reserva simultânea.

## Dúvidas

A nota do design de baixo nível ficou entre `bom` e `com problemas`. O Observer está certo e o código novo é curto.
Dei `com problemas` por dois motivos: a regra "pendente não avisa" está repetida dentro de cada ouvinte, pelo
booleano, justamente onde o dono prevê mais ouvintes; e uma exceção num ouvinte impede os seguintes. Um revisor mais
tolerante aceitaria o código como está.
