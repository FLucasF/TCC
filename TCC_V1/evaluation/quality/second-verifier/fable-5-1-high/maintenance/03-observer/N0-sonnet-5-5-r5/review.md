# Revisão: manutenção, cenário 03

## Visão geral

O projeto é o da base, com uma camada nova por cima: `hotel.web` (o Javalin, só lendo a requisição e escrevendo a
resposta), `hotel.reservations` (as regras e o acesso ao banco das reservas), `hotel.rooms`, `hotel.messages`
(o `EmailSender`, que guarda cada e-mail na tabela `messages`), `hotel.documents` (o comprovante e o formato de dinheiro
e data), `hotel.reports` e a montagem em `hotel.Hotel`. Uma confirmação entra por `HttpApi.java:70`, vai a
`ReservationService.confirm` (`ReservationService.java:69-83`), que grava a reserva pelo `ReservationStore`, manda o
e-mail do hóspede direto e depois percorre uma lista de `ReservationListener` (`:81`); os dois ouvintes novos,
`GovernanceNotifier` e `FinanceNotifier`, no pacote novo `hotel.notifications`, montam e enviam os e-mails da governança
e do financeiro, e são registrados na montagem (`Hotel.java:30-32`). Os dados ficam num PostgreSQL com três tabelas,
criadas por uma migração do Flyway que o agente não tocou. O sistema faz tudo o que o pedido pede (66 de 66
conferências do roteiro) e os 30 testes passam. O ponto mais sério é de design: o agente criou o Observer para a
governança e o financeiro, mas deixou o e-mail do hóspede como chamada direta dentro do serviço, de modo que o serviço
ainda conhece um dos interessados.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 21 dependências entre eles (a base, 17), 1 ciclo (`hotel.documents` <->
`hotel.reservations`), o mesmo que `measures/dependencies-base.md` já acusava na base.
**Nota:** bom

O agente acrescentou um pacote, `hotel.notifications` (2 classes, 77 linhas), que depende de `documents`, `messages` e
`reservations` e é usado só por `hotel`, a montagem. A interface de ouvinte ficou em `hotel.reservations`
(`ReservationListener.java:7`) e as implementações em `hotel.notifications`: a dependência aponta dos ouvintes para o
serviço, e não o contrário, por isso nenhum ciclo novo apareceu; o único ciclo é o da base (`ReservationReceipt` em
`documents` lê `Reservation`, e `ReservationService.java:3` importa `ReservationReceipt`), que o agente não piorou.
As camadas continuam separadas: o serviço não usa Javalin nem JDBC (`ReservationService.java:1-8` só importa
`documents`, `messages`, `rooms` e `java.*`), os ouvintes não usam biblioteca nenhuma (a tabela de bibliotecas em
`measures/dependencies.md` mostra `hotel.notifications` vazio), e a borda HTTP não mudou (`HttpApi.java:70-73` só
chama o serviço). A montagem segue num lugar só: `Hotel.java:28-34` cria o `EmailSender` uma vez, a lista de ouvintes
e o serviço, e o comentário em `Hotel.java:29` diz onde entra o próximo interessado. A maior classe continua sendo
`HttpApi` (170 linhas); `ReservationService` cresceu de 138 para 153 linhas, nada preocupante.

## Design de baixo nível

**Números:** 3 interessados no total; 2 estão na estrutura (governança e financeiro, um ouvinte cada) e 1 fora dela (o
hóspede, chamada direta no serviço). 2 eventos (confirmação e cancelamento), os dois anunciados pela lista de ouvintes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

O que aproxima do Observer: há uma interface de ouvinte com os dois eventos (`ReservationListener.java:10` e `:17`,
com métodos `default` para quem só quer um deles); uma implementação por interessado
(`GovernanceNotifier.java:9`, `FinanceNotifier.java:9`), cada uma montando o seu próprio e-mail com o `TextFormat`
da base (`GovernanceNotifier.java:23-26`, `FinanceNotifier.java:24`); o serviço recebe a lista de fora
(`ReservationService.java:26-32`, com `List.copyOf`) e só a percorre, na confirmação (`:81`) e no cancelamento
(`:102`), sem saber quem está nela; e a lista é montada em `Hotel.java:30-32`. A regra "sem nada a devolver, o
financeiro não é avisado" está no ouvinte do financeiro (`FinanceNotifier.java:29`), e não no serviço, o que é o
lugar certo.

O que afasta, e decide a classe: o e-mail do hóspede continuou como chamada direta dentro do serviço
(`ReservationService.java:76-80`), com o texto em `confirmationEmailBody` (`:140-146`) e o `EmailSender` como campo do
serviço (`:18`). A referência do pattern diz com todas as letras que "o hóspede tem chamada direta e só a governança e
o financeiro viraram ouvintes" é malfeito: o serviço ainda conhece um interessado e monta um e-mail, e o próximo
pedido que mexer no e-mail do hóspede mexe no serviço. O pedido dizia que o e-mail do hóspede "continua igual", mas
isso é sobre o conteúdo, não sobre onde ele mora; o mesmo texto podia sair de um `GuestNotifier`.

Sem exagero: o agente não criou fábrica, builder nem camada que só repassa. O construtor de quatro argumentos que
ficou (`ReservationService.java:21-24`) existe só para os testes da base continuarem compilando e delega para o de
cinco; é uma conveniência, não estrutura.

O resto do código é o da base, limpo: `BigDecimal` com centavos para dinheiro, `LocalDate` para datas, enums para
situação e plano, classes `final` e exceções com código (`HotelException`). No código do agente, um ponto de atenção,
que não derruba a nota: a regra "reserva pendente cancelada não avisa ninguém" não está no serviço, que anuncia o
cancelamento de qualquer jeito com um `boolean wasConfirmed` (`ReservationService.java:95`, `:102`), e cada ouvinte a
repete por conta própria (`GovernanceNotifier.java:31`, `FinanceNotifier.java:29`); o restaurante e o transfer vão ter
de lembrar de fazer o mesmo. Um detalhe de estilo: `StaffNotificationTest.java:58` e `:73` usam o nome completo
`hotel.documents.TextFormat` em vez de importar.

## Testes

**Números:** 30 testes rodaram, 0 falharam, 0 pulados; 30 métodos de teste escritos (a base tinha 26, todos mantidos;
o agente acrescentou 4 em `StaffNotificationTest`). Cobertura de 93,9% das linhas (510 de 543) e 87,9% dos ramos
(109 de 124); a base, 93,8% e 87,1%. O pacote novo `hotel.notifications`: 31 de 31 linhas e 6 de 6 ramos.
**Nota:** com problemas

O agente testou o que mudou, com valores na maior parte: o assunto da governança e do financeiro na confirmação
(`StaffNotificationTest.java:51`, `:56`), as datas `10/03/2027` e `12/03/2027` no corpo (`:52-53`), o valor pago
(`:57-58`), os dois avisos no cancelamento de uma confirmada (`:67-74`), a governança avisada e o financeiro não quando
não há devolução (`:76-84`) e nenhuma mensagem ao cancelar uma pendente (`:87-93`). A contagem de mensagens em cada
caso (3, 5, 4, 0) amarra quem recebeu e quem não recebeu, e os testes rodam contra o banco de verdade
(`EmptyDatabase`). A única mudança num teste da base é `HttpApiTest.java:81`, de 1 para 3 mensagens, que é a
consequência direta do pedido, não um enfraquecimento.

Dois buracos na parte nova, pela pergunta da mutação:

- `StaffNotificationTest.java:54` confere "quantos hóspedes vêm" com `body().contains("2")`, e o corpo já contém
  `10/03/2027` e `12/03/2027`; se alguém apagasse a linha `Hóspedes:` de `GovernanceNotifier.java:25`, nenhum teste
  falharia.
- Os endereços de verdade estão em `Hotel.java:31-32`, mas `StaffNotificationTest` monta os seus próprios ouvintes
  (`:26-28`) e `HttpApiTest.java:81-82` só conta 3 mensagens e olha o destinatário da primeira; se alguém trocasse
  `governanca@` por outro endereço na montagem, nenhum teste falharia.

Dois pontos menores: o valor do financeiro é conferido contra `TextFormat.money(confirmed.amountPaid())` (`:57-58`,
`:73-74`), não contra o literal `R$ 779,80`, então um erro no formatador passaria; e o teste do cancelamento depende
da ordem das mensagens (`all.get(3)`, `all.get(4)` em `:69` e `:72`), que o pedido disse não importar, quando o
próprio teste tinha o auxiliar `to(address)` (`:43-47`) para não depender dela.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`); 1 migração do Flyway, igual à da base; nenhuma
conferência acusou algo (toda tabela com chave primária, dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem
índice, nenhuma data em texto).
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mudou: `V1__create_tables.sql` é idêntico ao da base (o script o
confere), e a tabela `messages` (`V1__create_tables.sql:44-50`), com `recipient`, `subject` e `body`, já acomoda os
e-mails para a governança e o financeiro, que entram pelo mesmo `SentMessages.add`
(`SentMessages.java:20-35`, SQL com `?`). Os candidatos a SQL montado com `+` que o script lista
(`ReservationStore.java:33`, `:83`, `:98`, `:111`) juntam só constantes (`COLUMNS` e pedaços literais), nunca dado de
fora. Conexões em try-with-resources com pool Hikari (`Database.java:23-28`), migrações no `Database.java:28`. A dupla
reserva é impedida pelo banco (`V1__create_tables.sql:38-40`, `EXCLUDE USING gist`), e o código traduz a violação em
`ROOM_UNAVAILABLE` (`ReservationStore.java:55-57`), o que o roteiro confirmou em 10 rodadas simultâneas.

Dois pontos herdados da base, sem peso para o agente: as colunas `status`, `rate_plan`, `type` e `channel` são `text`
sem `CHECK`; e a confirmação grava a reserva e depois insere os e-mails sem transação (`ReservationService.java:75-81`
chama `store.update` e três `send` separados), de modo que uma falha num ouvinte deixaria a reserva confirmada com
parte dos avisos. A base já fazia isso com o e-mail do hóspede; o agente só acrescentou dois inserts ao mesmo padrão.

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de 3; o cenário
03, 15 de 15; as duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada: o hóspede continua recebendo o mesmo e-mail de confirmação; a governança recebe
"Reserva confirmada - quarto 201" com a entrada, a saída e os 2 hóspedes, e o financeiro, "Pagamento recebido - reserva
1" com `R$ 779,80`; ao cancelar essa reserva, a governança recebe "Reserva cancelada - quarto 201" com as datas, e o
financeiro, "Reembolso - reserva 1" com `R$ 779,80`; na `NON_REFUNDABLE` cancelada, a governança é avisada e o
financeiro não; cancelar uma reserva pendente não manda nada. Tudo o que já funcionava (preços, recusas, check-in e
check-out, relatórios) continua igual, as reservas e mensagens sobrevivem ao reinício, e duas reservas ao mesmo tempo
para o mesmo quarto nunca entram as duas. O log do serviço (`measures/behavior-service.log`) não tem erro nem aviso.

## Dúvidas

- Se o agente deixou o e-mail do hóspede fora da lista de ouvintes de propósito, por ler "que continua igual" como
  "não mexa nesse código", ou por descuido, a evidência não diz; para a classe não faz diferença, porque a referência
  trata esse arranjo como malfeito de qualquer modo.
- A nota dos testes fica na fronteira entre `bom` e `com problemas`: as regras novas estão todas testadas, e o que
  pesou foram a asserção vazia do número de hóspedes (`StaffNotificationTest.java:54`) e os endereços da montagem sem
  teste. Um revisor menos exigente aceitaria como `bom`.
