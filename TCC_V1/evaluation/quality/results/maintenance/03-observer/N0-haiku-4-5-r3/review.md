# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin com pacotes por assunto: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras em
`ReservationService` e acesso a dados em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que
grava os e-mails na tabela `messages`), `hotel.documents` e `hotel.reports`. A montagem fica em `hotel.Hotel`, e o
esquema vem de uma migração Flyway. Uma requisição de confirmação entra por `HttpApi`, chama
`ReservationService.confirm`, que grava a reserva pelo `ReservationStore` e manda os e-mails pelo `EmailSender` para o
PostgreSQL. O agente criou o pacote `hotel.notifications`. Ele tem uma interface `ReservationNotificationListener`
com dois eventos (confirmada e cancelada) e dois ouvintes, um da governança e um do financeiro, que são ligados em
`Hotel.java:29-32` e percorridos pelo serviço (`ReservationService.java:84-86` e `107-112`). O comportamento pedido
está todo certo (66 de 66 conferências). Os pontos mais sérios: o e-mail do hóspede continua sendo uma chamada direta
dentro do serviço (`ReservationService.java:80-83`), fora da estrutura de ouvintes; um ciclo novo entre
`hotel.reservations` e `hotel.notifications`; e testes novos que não conferem os valores em dinheiro.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 23 dependências entre eles (a base tinha 17). A base já tinha um ciclo,
`hotel.documents` ↔ `hotel.reservations`. A final tem esse e mais um, novo, `hotel.reservations` ↔
`hotel.notifications`, e por isso também o ciclo de três pacotes entre `documents`, `reservations` e `notifications`.
O pacote novo não usa Javalin nem JDBC.
**Nota:** com problemas

As camadas da base continuam inteiras: o pacote novo não fala com HTTP nem com SQL, e os ouvintes só chamam o
`EmailSender` (`notifications/FinanceNotificationListener.java:24`, `GovernanceNotificationListener.java:30`). A
montagem continua num lugar só (`Hotel.java:28-34`). O defeito é o lugar da interface: `ReservationNotificationListener`
ficou em `hotel.notifications`, o serviço a importa (`reservations/ReservationService.java:5`), e os ouvintes do mesmo
pacote importam `Reservation` (`notifications/FinanceNotificationListener.java:5`). Resultado: um ciclo novo entre dois
pacotes vizinhos. Se a interface ficasse em `hotel.reservations`, que é quem anuncia o evento, a dependência teria um
sentido só (`notifications -> reservations`). Detalhe menor: `notifications` depende de `documents` só para usar o
`TextFormat` (`FinanceNotificationListener.java:3`). Nenhuma classe cresceu muito: `ReservationService` foi de 138 para
163 linhas.

## Design de baixo nível

**Números:** 5 avisos ao todo (2 na confirmação para governança e financeiro, 2 no cancelamento para os mesmos, mais o
do hóspede na confirmação). 4 passam pelos ouvintes, e 1, o do hóspede, ficou fora da estrutura, como chamada direta no
serviço. 1 interface, 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

**O pattern.** O que aproxima do Observer: há uma interface de ouvinte com os dois eventos
(`notifications/ReservationNotificationListener.java:6-10`), uma implementação por interessado
(`GovernanceNotificationListener`, `FinanceNotificationListener`) e um serviço que percorre uma lista recebida de fora
(`ReservationService.java:33,38`, chamadas em `84-86` e `109-111`). Os ouvintes são registrados na montagem
(`Hotel.java:29-32`). A regra "só avisa o financeiro se houver o que devolver" ficou dentro do ouvinte do financeiro
(`FinanceNotificationListener.java:29-30`), e o serviço não monta nenhum e-mail da governança nem do financeiro. Para
o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel`.

O que afasta: o hóspede, que já era interessado na base, não virou ouvinte. `ReservationService.confirm` ainda monta e
manda o e-mail dele à mão (`ReservationService.java:79-83`, com o texto em `confirmationEmailBody`, `150-156`), e o
serviço ainda depende do `EmailSender` (`ReservationService.java:20`). A referência do pattern dá esse caso como
malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". Por isso a classe é
`malfeito`, e não `acerto`.

**O exagero.** Não há estrutura sem necessidade. A interface tem duas implementações e é o ponto de extensão que o
pedido anuncia. O segundo construtor de `ReservationService` (`ReservationService.java:23-26`), que passa uma lista
vazia, existe só para manter as chamadas dos testes da base. Ele é uma pequena sobra, não uma camada.

**O resto do código.** O código novo é curto e legível, e usa os tipos certos: `BigDecimal` com `TextFormat.money`,
`LocalDate` com `TextFormat.date` e o `enum` de situação em `ReservationService.java:100`. Defeitos pequenos e
localizados, que não chegam a mudar a nota:
- um nome de tipo escrito por extenso, `hotel.notifications.ReservationNotificationListener`, em vez de um import
  (`Hotel.java:29`);
- `%n` (o separador de linha da plataforma) nos corpos dos e-mails novos (`FinanceNotificationListener.java:22,33`,
  `GovernanceNotificationListener.java:21-23`), enquanto o resto do sistema usa `\n` (`ReservationService.java:151-155`);
- uma checagem `refund != null` (`FinanceNotificationListener.java:30`) sobre um valor que o serviço acabou de
  preencher (`ReservationService.java:105`).

O resto do projeto veio da base sem mudança.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados, 30 métodos escritos (a base tinha 26). Cobertura de
linhas de 94,4% (519 de 550; base 93,8%) e de ramos de 87,5% (112 de 128; base 87,1%). O pacote `hotel.notifications`
tem 33 de 33 linhas e 3 de 4 ramos cobertos.
**Nota:** com problemas

O agente escreveu 4 testes contra o banco (`notifications/NotificationsTest.java`), que cobrem os quatro casos: a
confirmação avisa os três (`:54-80`), o cancelamento da confirmada avisa governança e financeiro (`:82-102`), a
pendente cancelada não avisa ninguém (`:104-112`) e a NON_REFUNDABLE cancelada não avisa o financeiro (`:114-125`).
Ele também ajustou o teste da API para os três e-mails, com os destinatários (`web/HttpApiTest.java:81-84`). Nenhum
teste da base foi apagado nem enfraquecido; a única mudança foi a contagem de mensagens, de 1 para 3, que o pedido
exige.

As asserções sobre os valores são fracas:
- o dinheiro nunca é conferido, só a presença de `"R$"` (`NotificationsTest.java:79` e `:101`). Se o financeiro
  recebesse o total sem desconto, ou zero, nenhum teste falharia;
- o número de hóspedes é conferido com `contains("1")` (`:73`), que passa sozinho porque o corpo já traz "Quarto 101".
  Apagar a linha dos hóspedes não derrubaria o teste;
- os assuntos são conferidos com `contains`, e não por igualdade (`:70,78,94,100`);
- não há teste que confira que o e-mail do hóspede ficou igual ao da base. O assunto é conferido (`:65`), o corpo não.

A regra do "nada a devolver" tem rede: trocar `signum() > 0` por `>= 0` derruba o teste da linha `:122`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 1 migração Flyway, igual à da base, e nenhuma migração
nova. Nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice,
nenhuma data em texto. A regra de não haver duas reservas na mesma noite é protegida por um `EXCLUDE USING gist`. As
colunas de situação e de tipo não têm CHECK, e o código não abre nenhuma transação explícita.
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mexeu nele: os e-mails novos vão para a tabela `messages` que já
existia, pelo mesmo `SentMessages.add` com parâmetros (`messages/SentMessages.java:20-35`), e
`V1__create_tables.sql` ficou igual. As colunas sem CHECK e a falta de transação vêm da base. O agente piorou um pouco
o segundo ponto: a confirmação agora faz quatro escritas separadas (a reserva e três mensagens,
`ReservationService.java:77-86`) sem transação. Se uma das mensagens falhar, a reserva já está confirmada e os avisos
seguintes não saem. Esse risco já existia na base com uma mensagem, e só aparece com falha do banco no meio da
operação.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam. A parte comum: 46 de 46. Os dados depois de reiniciar: 3 de
3. O cenário 03: 15 de 15. As reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. Na confirmação, o hóspede continua recebendo o e-mail de antes, a governança
recebe "Reserva confirmada - quarto 201" com a entrada, a saída e o número de hóspedes, e o financeiro recebe
"Pagamento recebido - reserva 1" com R$ 779,80. No cancelamento de uma confirmada, a governança recebe "Reserva
cancelada - quarto 201" com as datas, e o financeiro recebe "Reembolso - reserva 1" com o valor a devolver. Na
NON_REFUNDABLE cancelada, o financeiro não é avisado, e a pendente cancelada não avisa ninguém. O que já funcionava
continua funcionando, inclusive depois de reiniciar e com dois pedidos ao mesmo tempo.

## Dúvidas

- A classe do pattern ficou entre `malfeito` e `acerto`. Para governança e financeiro, a estrutura está certa. O que
  decidiu foi o e-mail do hóspede fora dos ouvintes (`ReservationService.java:80-83`), que a referência do cenário
  aponta como malfeito.
- A nota do design ficou em `bom`, apesar dos detalhes listados (o nome por extenso, `%n`, a checagem de `null`). São
  pequenos e localizados, e um revisor mais rigoroso poderia pedir que fossem corrigidos antes de aceitar.
