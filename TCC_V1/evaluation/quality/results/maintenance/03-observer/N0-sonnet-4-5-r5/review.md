# Revisão: manutenção, cenário 03

## Visão geral

O sistema da base não mudou de forma. A borda HTTP fica em `hotel.web` (`HttpApi`), as regras em
`hotel.reservations` (`ReservationService`, `PriceCalculator`), os dados em classes de acesso com JDBC
(`ReservationStore`, `Rooms`, `SentMessages`), o esquema em PostgreSQL por migração Flyway, e tudo é montado em
`hotel.Hotel`. Uma requisição como `POST /reservations/{id}/confirmation` passa pelo handler
(`src/main/java/hotel/web/HttpApi.java`), vai para `ReservationService.confirm` e chega a `ReservationStore.update`. Para
atender o pedido, o agente criou uma interface de ouvinte (`ReservationEventListener`), um sujeito que guarda a lista
de ouvintes (`ReservationNotifier`) e dois ouvintes (`HousekeepingNotifier`, `FinanceNotifier`), registrados em
`Hotel.java:28-30`. O serviço avisa os ouvintes na confirmação e no cancelamento. O comportamento está todo certo
(66 de 66 conferências passaram). O problema mais sério está no design: o e-mail do hóspede continua sendo uma chamada
direta do serviço (`ReservationService.java:67-71`), fora da lista de ouvintes. Por isso o Observer ficou pela metade.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). `hotel.reservations` passou de 13
classes e 508 linhas para 17 classes e 628 linhas.
**Nota:** bom

O agente não criou pacote novo, dependência nova entre pacotes nem ciclo novo. A montagem continua num lugar só:
`Hotel.java:27-32` cria o `EmailSender`, o `ReservationNotifier` e os dois ouvintes e entrega tudo pronto ao serviço.
O serviço não cria nada por dentro. As regras continuam sem Javalin e sem SQL: `ReservationService.java:72` e `:93`
só chamam o sujeito, e o SQL fica em `ReservationStore`. A borda HTTP ficou como estava, fina (`HttpApi.java:62-79`).

Uma observação, que não muda a nota: os ouvintes de cada setor sabem os endereços e escrevem os textos dos e-mails da
governança e do financeiro, mas ficam no pacote do domínio (`hotel/reservations/HousekeepingNotifier.java`,
`FinanceNotifier.java`). Eles importam `hotel.documents.TextFormat` (`FinanceNotifier.java:3`,
`HousekeepingNotifier.java:3`), o que reforça a aresta `reservations → documents` do ciclo que veio da base. Com o
restaurante e o transfer chegando, um pacote próprio de notificações deixaria o domínio mais limpo.

## Design de baixo nível

**Números:** dos 3 interessados, 2 viraram ouvintes: a governança e o financeiro, na confirmação e no cancelamento.
O hóspede continua fora da estrutura, com a chamada direta em `ReservationService.java:68-71`. Há 1 interface de
ouvinte com 2 implementações e 1 sujeito.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima o código do Observer:** existe uma interface de ouvinte (`ReservationEventListener.java:5,7`) com
uma implementação por interessado novo. O serviço percorre uma lista que recebe de fora (`ReservationService.java:18,22`,
`ReservationNotifier.java:14-24`), e os ouvintes são registrados na montagem (`Hotel.java:29-30`). Os dois eventos,
confirmação (`ReservationService.java:72`) e cancelamento (`:93`), passam pela estrutura. O serviço não monta os
e-mails da governança nem do financeiro.

**O que afasta:** o hóspede, o único interessado que a base já tinha, continua fora da lista. O serviço ainda monta o
comprovante e o e-mail dele e chama o `EmailSender` direto (`ReservationService.java:67-71` e
`confirmationEmailBody`, `:131-137`). Ele ainda recebe o `EmailSender` só para isso (`:17,26`). A referência do
pattern descreve exatamente esse caso ("o hóspede tem chamada direta e só a governança e o financeiro viraram
ouvintes") como malfeito. Por isso o serviço continua conhecendo um destinatário, e quem é avisado não se decide só
na montagem.

**Exagero:** não há. O `ReservationNotifier` é o sujeito do próprio pattern, não uma camada à toa. Não há fábrica,
builder nem interface com uma implementação só fora do pattern.

**O design do resto da mudança:**
- **Regra repetida em cada ouvinte.** A regra "reserva que não estava confirmada não avisa ninguém" ficou dentro de
  cada ouvinte (`HousekeepingNotifier.java:29`, `FinanceNotifier.java:28`), com a flag `wasConfirmed` passada a todos
  (`ReservationEventListener.java:7`). Um ouvinte novo (o restaurante, o transfer) que esqueça esse `if` passa a avisar
  sobre reservas pendentes. Essa decisão é do serviço: ele poderia simplesmente não anunciar o cancelamento de uma
  reserva pendente.
- **Parâmetro redundante.** O `roomNumber` é passado ao lado da reserva (`ReservationService.java:72,93`,
  `ReservationEventListener.java:5,7`), mas é o mesmo `reservation.roomNumber()`.
- **Interface difícil de estender.** A interface tem dois métodos abstratos. Cada evento novo obriga a mexer em todos
  os ouvintes, e um ouvinte que só quer a confirmação tem de escrever um método vazio. É o que o próprio
  `NOTIFICACOES.md` mostra no exemplo do restaurante.
- **Falhas não isoladas.** `ReservationNotifier.java:15-17` não isola as falhas: se um ouvinte lançar uma exceção, os
  seguintes não são avisados, e a chamada responde com erro depois que a reserva já foi gravada como confirmada
  (`ReservationService.java:66`).
- **Teste defensivo sem motivo.** `FinanceNotifier.java:32` testa `refund == null`, que nunca acontece depois de
  `cancel` (`Reservation.java:128`).

O resto do código (dinheiro em `BigDecimal`, datas em `LocalDate`, `enum` para a situação da reserva) veio da base e
não piorou.

## Testes

**Números:** em `final`, 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados (29 métodos escritos). Na base eram
26, todos passando. A cobertura é de 94,5% das linhas (529 de 560) e 87,7% dos ramos (114 de 130). Na base era 93,8%
e 87,1%. O pacote `hotel.reservations` tem 263 de 276 linhas e 62 de 66 ramos cobertos.
**Nota:** com problemas

O agente escreveu 3 testes novos e ajustou os da base sem enfraquecê-los. As contagens de mensagens mudaram de 1 para
3 e passaram a conferir os destinatários e os assuntos novos: `ReservationServiceTest.java:99,105-108,117` e
`HttpApiTest.java:81-84`. Os testes novos são:
- reserva pendente cancelada não manda nada (`ReservationServiceTest.java:176-181`);
- reserva confirmada cancelada avisa a governança, com as duas datas, e o financeiro (`:185-199`);
- reserva NON_REFUNDABLE cancelada não avisa o financeiro (`:203-212`, pela contagem 4).

Os testes rodam contra o banco e um deles pela API.

Pela pergunta da mutação, algumas regras novas ficam sem rede:
- **O corpo do e-mail da governança na confirmação não é conferido** (só o assunto, `:106`). Se as datas ou o número
  de hóspedes sumissem de `HousekeepingNotifier.java:19-23`, nenhum teste falharia.
- **O valor pago no e-mail do financeiro também não é conferido** (só o assunto, `:108`). Trocar `amountPaid()` por
  outro valor em `FinanceNotifier.java:22` passaria.
- **O valor do reembolso é conferido só por `contains("R$ ")`** (`:199`). Qualquer valor passaria.

Além disso, os testes dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(3)`), que o dono disse que não importa.
Se a ordem de registro mudar, os testes quebram sem que o comportamento mude. A montagem dos ouvintes foi copiada
nos testes (`ReportsTest.java:35-39`, `ReservationServiceTest.java:28-33`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira
está sem índice e nenhuma data está em texto. Há um `EXCLUDE USING gist` contra duas reservas na mesma noite. As
colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) estão sem CHECK. Há 1 migração
(`V1__create_tables.sql`), igual à da base. Nenhum SQL é montado com dado de fora: os candidatos em
`ReservationStore.java:33,83,98,111` concatenam só constantes.
**Nota:** bom

A mudança não precisava mexer no banco, e o agente não mexeu: a migração da base está intacta, e os e-mails novos vão
para a tabela `messages` que já existia, pelo `SentMessages`. A falta de CHECK nas colunas de situação veio da base. A
proteção contra reserva duplicada é a restrição `EXCLUDE` da base. Uma observação: a confirmação agora faz quatro
escritas (a reserva e três mensagens, `ReservationService.java:66-72`) sem transação (o script não achou nenhum
`setAutoCommit(false)`). Uma falha no meio deixa a reserva confirmada com parte dos avisos faltando. Esse padrão já
vinha da base, só aumentou o número de escritas.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **Na confirmação,** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto
  201", com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- **No cancelamento de uma reserva confirmada,** a governança recebe "Reserva cancelada - quarto 201", com as datas, e
  o financeiro recebe "Reembolso - reserva 1", com R$ 779,80.
- **Na tarifa NON_REFUNDABLE,** só a governança é avisada.
- **Uma reserva pendente cancelada** não manda nada.

O que já funcionava continua funcionando, e os dados ficam depois de reiniciar.

## Dúvidas

A classe ficou entre `malfeito` e `acerto`. A estrutura do Observer existe e cobre os dois eventos e os dois
interessados novos. Mas a referência do cenário diz, com essas palavras, que manter o hóspede com chamada direta,
enquanto só a governança e o financeiro viram ouvintes, é malfeito. O pedido dizia que o e-mail do hóspede
"continua igual", e isso pode ter levado o agente a não mexer nele. Mesmo assim, manter o texto igual não impedia
mover o envio para um ouvinte. A evidência sustenta melhor `malfeito`.
