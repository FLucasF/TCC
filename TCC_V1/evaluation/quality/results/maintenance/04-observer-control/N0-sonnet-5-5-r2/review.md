# Revisão: manutenção, cenário 04

## Visão geral

O projeto (que veio da base) tem 7 pacotes: `hotel.web` (as rotas do Javalin em `HttpApi`), `hotel.reservations` (o
`ReservationService` com as regras, o `PriceCalculator` e o `ReservationStore` com o JDBC), `hotel.rooms`,
`hotel.messages` (o `EmailSender`, que grava cada e-mail na tabela `messages`), `hotel.documents` (o comprovante),
`hotel.reports` e `hotel` (a montagem em `Hotel.java:191-200`). Uma requisição entra em `HttpApi`, chama o
`ReservationService`, e o serviço grava pelo `ReservationStore` num PostgreSQL com o esquema criado por Flyway. O
pedido era mudar o texto do e-mail de confirmação, e o agente fez exatamente isso: o `changes.patch` muda só três
linhas de código em `ReservationService.java` (o assunto na linha 67 e as três linhas novas em 129-131, no lugar da
linha antiga) e ajusta o teste que já existia. Não criou nenhuma estrutura para o WhatsApp do "talvez um dia". As 58
conferências do roteiro passam e os 26 testes passam. O único ponto fraco é o teste da mudança: ele confere só uma
das três linhas novas.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, com 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Tudo
igual à base (`measures/dependencies-base.md`); a única diferença é `ReservationService.java`, que passou de 138 para
140 linhas.
**Nota:** bom

O agente não mexeu na estrutura. A borda HTTP é fina: as rotas só leem a requisição, chamam o serviço e escrevem a
resposta (`web/HttpApi.java:61-91`). As regras não conhecem o Javalin nem o SQL (`reservations/ReservationService.java:1-7`),
o JDBC fica nas classes de acesso a dados (`reservations/ReservationStore.java`, `messages/SentMessages.java`,
`rooms/Rooms.java`) e a montagem fica num lugar só (`Hotel.java:191-200`). O ciclo `documents` ↔ `reservations` já
existia na base: `ReservationService.java:3,64` cria o `ReservationReceipt`, e `documents/ReservationReceipt.java:3`
usa `Reservation`. O agente não o piorou. O texto novo ficou no mesmo lugar onde o texto antigo já estava, sem nova
dependência.

## Design de baixo nível

**Números:** a mudança tem 3 linhas de produção alteradas ou acrescentadas (o assunto e as três linhas do corpo),
num único método privado que já existia (`confirmationEmailBody`). Há 0 classes, interfaces ou métodos novos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É o controle negativo, e a resposta certa era trocar o assunto e o texto onde eles já estavam. Foi o que o agente
fez: o assunto em `src/main/java/hotel/reservations/ReservationService.java:67`, reaproveitando a constante
`HOTEL_NAME` (linha 12), e as três linhas pedidas em `ReservationService.java:129-131`, no lugar da antiga "A entrada
é a partir das 14h, e a saída, até as 12h.". A saudação (126-127), o comprovante (128) e o "Até breve!" (132)
continuam como estavam. Não há canal de mensagem abstrato, ouvinte de confirmação nem modelo de mensagem para o
WhatsApp: o `EmailSender` (`messages/EmailSender.java:1-15`) está igual à base.

O resto do código veio da base e está limpo. O dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo
(`reservations/PriceCalculator.java:363-365`), as datas são `LocalDate`, as situações e os planos são `enum`, o
estado da reserva só muda por métodos do pacote (`reservations/Reservation.java:102-114`) e os erros têm código
próprio (`HotelException`/`ErrorCode`). Um único `catch (Exception)` traduz o corpo ilegível em 400
(`web/HttpApi.java:113`), e ele é justificado. Um detalhe pequeno: o assunto novo repete quase a mesma frase da
segunda linha do corpo (`ReservationService.java:67` e `:127`), mas foi o texto que o dono pediu.

## Testes

**Números:** 26 testes rodaram, 0 falharam e 0 foram pulados; há 26 métodos de teste no código (os mesmos da base,
nenhum novo e nenhum apagado). A cobertura é de 93,8% das linhas (471 de 502) e 87,1% dos ramos (101 de 116), igual à
base (`measures/tests.md` e `measures/tests-base.md` são idênticos).
**Nota:** com problemas

A base continua passando, e o agente atualizou o teste que já conferia o e-mail. Agora ele confere o assunto novo com
o valor exato (`src/test/java/hotel/reservations/ReservationServiceTest.java:94`) e uma das três linhas novas, a do
café da manhã (`ReservationServiceTest.java:95`). As outras regras do pedido ficaram sem teste, como mostra a pergunta
da mutação:
- se a linha do telefone "(79) 3200-0000" fosse apagada, nenhum teste falharia;
- se a linha nova "Entrada a partir das 14h e saída até as 12h." saísse, ou se a linha antiga continuasse no corpo,
  nenhum teste falharia;
- nada confere a ordem das três linhas, nem que a saudação e o "Até breve!" continuam no fim.

Um `assertEquals` sobre o corpo inteiro, ou um `contains` para cada linha nova mais um `assertFalse` para a antiga,
teria fechado isso. As regras da base (preço, temporada, desconto, cancelamento, situações, recusas e relatórios)
continuam conferidas com valores (por exemplo, `ReservationServiceTest.java:96-97`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(10,2)`, não há chave estrangeira sem índice nem data guardada como texto, e há uma restrição `EXCLUDE` contra
a reserva dupla. As conferências apontam 4 colunas de situação ou tipo sem `CHECK`. A migração `V1__create_tables.sql`
está igual à da base, e não há migração nova.
**Nota:** bom

O pedido não tocava no banco, e o agente não mexeu nele: a migração da base ficou intacta (`measures/database.md`,
"igual"). O esquema é bom: a chave estrangeira do quarto está em
`src/main/resources/db/migration/V1__create_tables.sql:21`, o dinheiro em `numeric(10, 2)` (linhas 30-35) e a reserva
dupla é barrada no próprio banco (linhas 38-40). O SQL usa parâmetros: o que entra com `+` é a constante `COLUMNS`
(`reservations/ReservationStore.java:18-19,98,111`). As conexões são devolvidas com try-with-resources. Dois defeitos
pequenos vieram da base e não são do agente: as colunas `status`, `rate_plan`, `type` e `channel` são `text` sem
`CHECK` (`V1__create_tables.sql:4,28,33,46`), e os relatórios leem a tabela inteira de reservas para filtrar em Java
(`reports/RevenueReport.java:25`, `reports/OccupancyReport.java:25`).

## Comportamento

**Números:** 58 de 58 conferências passaram e nenhuma falhou: a parte comum 46 de 46, os dados depois de reiniciar 3
de 3, o cenário 04 7 de 7 e as reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, o hóspede passa a receber o e-mail com o assunto "Sua reserva na Pousada Maré Alta está
confirmada!". O e-mail traz as três linhas pedidas, na ordem, no lugar da linha antiga, que saiu. A saudação, o
comprovante e o "Até breve!" continuam no fim. O resto do sistema funciona como antes: preços, recusas, confirmação,
check-in, check-out, cancelamentos, relatórios, dados guardados depois de reiniciar, e nenhuma reserva dupla em 10
rodadas simultâneas.

## Dúvidas

nenhuma
