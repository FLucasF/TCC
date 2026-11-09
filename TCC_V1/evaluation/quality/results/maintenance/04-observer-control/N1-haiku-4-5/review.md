# Revisão: manutenção, cenário 04

## Visão geral

O agente mexeu em dois arquivos e em seis linhas (`changes.patch`; `diff -r base final` dá a mesma coisa). Em
`src/main/java/hotel/reservations/ReservationService.java:67` ele trocou o assunto do e-mail. Em
`ReservationService.java:129-131`, no método privado `confirmationEmailBody`, trocou a linha do horário pelas três
linhas que o dono pediu. No teste `src/test/java/hotel/reservations/ReservationServiceTest.java:94`, atualizou o
assunto esperado. O projeto continua como a base o deixou: sete pacotes. O `hotel.web.HttpApi` cuida das rotas Javalin
e chama o `hotel.reservations.ReservationService`, que guarda as regras. Os dados ficam no PostgreSQL, acessados com
JDBC por `ReservationStore`, `Rooms` e `SentMessages`, e o esquema vem de uma migração do Flyway. Tudo é montado em
`hotel/Hotel.java:20-28`. O agente resistiu à frase sobre o WhatsApp e não criou nenhuma estrutura nova. A única coisa
a apontar é pequena: os testes não conferem as três linhas novas do corpo do e-mail.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos em `measures/dependencies.md` e em `measures/dependencies-base.md`. A única diferença é que
`ReservationService` passou de 138 para 140 linhas.
**Nota:** bom

O agente não mudou nenhuma dependência, nenhum pacote e nenhuma classe além do texto dentro de `ReservationService`.
A borda HTTP é fina: cada rota lê a requisição, chama o serviço e escreve a resposta (`hotel/web/HttpApi.java:62-90`).
As regras não conhecem o Javalin nem o JDBC: segundo o jdeps, `hotel.reservations` usa JDBC apenas por causa do
`ReservationStore`, e o serviço só recebe as dependências pelo construtor (`ReservationService.java:19-25`). Tudo é
montado num lugar só (`hotel/Hotel.java:20-28`). O ciclo `documents` ↔ `reservations` (`ReservationService.java:3`
importa `ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `Reservation`) já existia na base e não
é do agente. O texto ficou em `confirmationEmailBody`, que é onde ele já morava, sem nenhuma dependência nova.

## Design de baixo nível

**Números:** 2 linhas de código de produção mudadas e 3 acrescentadas, todas em
`ReservationService.java:67` e `:129-131`. Nenhuma classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é um controle negativo. A frase "talvez um dia a gente mande essa confirmação por WhatsApp também" convidava a
criar um canal de mensagem abstrato, um ouvinte da confirmação ou um modelo de mensagem. O agente não fez nada disso.
Ele mudou o assunto no ponto onde o e-mail é enviado (`ReservationService.java:65-68`) e o corpo no método privado que
já montava o texto (`ReservationService.java:125-133`), reaproveitando a constante `HOTEL_NAME`
(`ReservationService.java:12`). Ele não tocou no `EmailSender` (`hotel/messages/EmailSender.java`) nem em
`SentMessages`. A saudação, o comprovante e o "Até breve!" ficaram como estavam, como o pedido mandava.

O resto do código veio da base e está limpo:

- o dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` (`PriceCalculator.java:165-167`);
- as datas são `LocalDate`, e as situações e os planos são `enum` com `switch` exaustivo
  (`ReservationService.java:82-85`);
- os campos mutáveis de `Reservation` só mudam por métodos de pacote (`Reservation.java:102-114`).

O único ponto discutível também vem da base: o `catch (Exception)` em `web/HttpApi.java:113`. Ele é localizado,
comentado e devolve 400.

## Testes

**Números:** final com 26 testes rodados, 0 falhas, 0 erros e 0 pulados, e 26 métodos escritos. Linhas cobertas:
471 de 502 (93,8%). Ramos cobertos: 101 de 116 (87,1%). A base tem exatamente os mesmos números
(`measures/tests-base.md`).
**Nota:** com problemas

O `mvn verify` passa, como o pedido exigia. O agente não apagou nem enfraqueceu nenhum teste. Ele só atualizou o
assunto esperado em `ReservationServiceTest.java:94`, e esse teste confere o assunto novo com o valor exato. A parte
nova do corpo, porém, ficou sem conferência. Façamos a pergunta da mutação:

- se alguém apagasse a linha do café da manhã ou a do telefone da recepção (`ReservationService.java:130-131`),
  nenhum teste falharia;
- se a linha antiga voltasse no lugar de "Entrada a partir das 14h e saída até as 12h.", nenhum teste falharia;
- se o "Até breve!" sumisse, nenhum teste falharia.

O teste do e-mail só confere o desconto e o total do comprovante (`ReservationServiceTest.java:95-96`), e nenhum outro
teste lê o corpo (`grep` por "14h", "Café", "Até breve" em `src/test` não acha nada). O texto pedido é a mudança
inteira, e metade dela está sem teste. É fácil de corrigir, mas um revisor pediria para acrescentar.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira
sem índice e nenhuma data em texto. As colunas de situação, plano, tipo e canal são `text` sem CHECK. Há 1 migração
do Flyway, igual à da base (`measures/database.md`).
**Nota:** bom

O pedido não toca no banco, e o agente também não tocou: `V1__create_tables.sql` está igual à da base, e não há
migração nova nem mudança no acesso a dados. O esquema da base é sólido:

- a chave estrangeira do quarto está em `src/main/resources/db/migration/V1__create_tables.sql:21`;
- a restrição `EXCLUDE` impede reservar o mesmo quarto duas vezes nas mesmas noites (`V1__create_tables.sql:38-40`);
- há CHECKs nas datas e no número de hóspedes (`V1__create_tables.sql:27,36`);
- o SQL usa parâmetros: as concatenações em `ReservationStore.java:33,83,98,111` só juntam constantes.

O que um revisor poderia pedir vem da base, não do agente:

- `status`, `rate_plan`, `type` e `channel` são texto sem CHECK (`V1__create_tables.sql:4,28,33,46`);
- os relatórios carregam a tabela inteira de reservas para filtrar em Java (`reports/RevenueReport.java:25`,
  `reports/OccupancyReport.java:25`).

## Comportamento

**Números:** 58 de 58 conferências passaram (`measures/behavior.md`):

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 04: 7 de 7;
- reservas simultâneas: 2 de 2.

**Nota:** bom

O hóspede recebe o e-mail com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O corpo traz a saudação,
o comprovante, as três linhas novas na ordem pedida e o "Até breve!" no fim, e a linha antiga sumiu. Todo o resto do
sistema continua funcionando igual:

- preços, recusas, confirmação, check-in, check-out, cancelamentos e relatórios;
- os dados, que continuam no banco depois de reiniciar;
- a recusa da segunda reserva simultânea.

Para o dono da pousada, a mudança está pronta e não quebrou nada.

## Dúvidas

Nenhuma.
