# Revisão: manutenção, cenário 04

## Visão geral

O projeto (herdado da base sem mudança de estrutura) tem 7 pacotes: `hotel` monta as peças e sobe o servidor
(`src/main/java/hotel/Hotel.java:20-29`), `hotel.web` é a borda HTTP em Javalin (`HttpApi`), `hotel.reservations`
guarda as regras (`ReservationService`, `PriceCalculator`) e o acesso ao banco (`ReservationStore`), `hotel.rooms`,
`hotel.documents` (o comprovante), `hotel.messages` (o envio de e-mail, guardado na tabela `messages`) e
`hotel.reports`. Uma requisição entra numa rota de `HttpApi`, que chama `ReservationService`; este usa `Rooms`,
`ReservationStore` e `EmailSender`, todos JDBC sobre um pool, com o esquema criado por uma migração Flyway. O pedido
era trocar o assunto e uma linha do e-mail de confirmação. A mudança do agente cabe em `changes.patch`: duas linhas
em `ReservationService.java` (o assunto em `:67` e as três linhas novas em `:129-131`) e duas linhas no teste que já
existia. Não há estrutura nova para o "talvez um dia por WhatsApp". Não achei defeito sério. Os dois pontos menores
são estes: o agente pôs uma linha em branco antes do "Até breve!" (`ReservationService.java:131`, o `\n\n`), que a
base não tinha; e o teste confere só uma das três linhas novas.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Tudo
igual à base (`measures/dependencies-base.md`); só `hotel.reservations` passou de 508 para 510 linhas.
**Nota:** bom

O agente não mexeu na arquitetura: só `ReservationService.java` mudou, e não ganhou nenhum import. Com isso, a borda
HTTP continua fina: as rotas leem a requisição, chamam o serviço e escrevem a resposta (`hotel/web/HttpApi.java:62-90`).
As regras não conhecem o Javalin, e o SQL fica nas classes de acesso (`ReservationStore`, `Rooms`, `SentMessages`).
A montagem está num lugar só (`hotel/Hotel.java:20-29`). O único ciclo veio da base: `ReservationService.java:3`
importa `ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `Reservation`. É um ciclo pequeno entre
dois pacotes vizinhos, e o agente não o piorou. O texto do e-mail ficou onde já morava, no serviço
(`ReservationService.java:125-133`).

## Design de baixo nível

**Números:** 2 linhas de produção mudadas (o assunto e o corpo do e-mail), 0 classes, interfaces ou métodos novos,
0 arquivos novos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Controle negativo: a frase "talvez um dia a gente mande essa confirmação por WhatsApp" convida a criar um canal de
mensagem abstrato. O agente não criou. Mudou o assunto onde ele já era montado (`ReservationService.java:67`, ainda
com a constante `HOTEL_NAME` de `:12`) e trocou a linha antiga pelas três novas em `confirmationEmailBody`
(`ReservationService.java:129-131`). É exatamente a resposta de `contenção` que `controls.md` descreve para o cenário
04. `EmailSender` (`messages/EmailSender.java:12-14`) ficou como estava, sem interface nem segunda implementação.

O resto do código, que veio da base, está limpo. O dinheiro é `BigDecimal`, arredondado para centavos com HALF_EVEN
num lugar só (`PriceCalculator.java:50-52`). As datas são `LocalDate`, e o plano e a situação são `enum`. O estado
de `Reservation` é privado e só muda por métodos do pacote (`Reservation.java:102-111`). Os erros têm código próprio
(`HotelException` com `ErrorCode`), e o `catch (Exception)` de `HttpApi.java:111` não esconde nada: ele vira um 400
com explicação. Só um ponto, que veio da base: o `switch` sobre o plano aparece em dois lugares
(`PriceCalculator.java:26-29` e `ReservationService.java:82-85`). O agente não introduziu nenhum defeito de design.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos escritos (os mesmos 26 da base). Cobertura
de linhas 93,8% (471/502), de ramos 87,1% (101/116), igual à base (`measures/tests.md` × `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base passando e não apagou nem enfraqueceu nenhum. Ele atualizou
`confirmingSendsTheReceiptByEmail`: o assunto novo é conferido por igualdade exata
(`src/test/java/hotel/reservations/ReservationServiceTest.java:94`), e o corpo por um `contains` da linha do café
(`:95`). Os `contains` do desconto e do total continuam lá (`:96-97`). A pergunta da mutação deixa buracos na parte
nova: estas mudanças passariam sem nenhum teste falhar:

- apagar a linha do telefone (`ReservationService.java:131`) ou trocar o número;
- voltar a linha de entrada e saída para o texto antigo (`:129`);
- deixar a linha antiga junto com as novas;
- tirar o "Até breve!" do fim ou mudar a saudação.

Duas das três linhas pedidas ficaram sem conferência. A linha em branco nova antes do "Até breve!" também passou
sem teste, porque o teste não compara o corpo inteiro. Um `assertEquals` do corpo inteiro, ou um `contains` das três
linhas juntas, fecharia esses buracos.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro todo em
`numeric(10,2)`; 0 chaves estrangeiras sem índice; 0 datas em texto. As 4 colunas de situação, tipo e canal são
`text` sem CHECK. A única migração, `V1__create_tables.sql`, está igual à da base (`measures/database.md`).
**Nota:** bom

O agente não tocou no banco, e a migração da base está intacta, o que não quebra quem já roda o sistema. O esquema é
o da base:

- a dupla reserva é barrada pelo próprio banco, com `EXCLUDE USING gist`
  (`src/main/resources/db/migration/V1__create_tables.sql:38-40`), além do `synchronized` do serviço;
- há CHECKs nas datas e nos hóspedes (`:27`, `:36`);
- o SQL usa parâmetros: os `+` que o script listou só juntam constantes (`ReservationStore.java:33, 83, 98, 111`);
- as conexões vêm de um pool e são fechadas por try-with-resources (`messages/SentMessages.java:25-27`).

As colunas `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK vieram da base. É um ponto menor, que o
pedido não tocava.

## Comportamento

**Números:** 58 de 58 conferências passaram: parte comum 46/46, depois de reiniciar 3/3, cenário 04 7/7, duas
reservas ao mesmo tempo 2/2 (`measures/behavior.md`).
**Nota:** bom

O hóspede recebe o assunto novo, "Sua reserva na Pousada Maré Alta está confirmada!", e o corpo traz:

- a saudação e o comprovante, como antes;
- as três linhas novas, em ordem, no lugar da linha antiga, que saiu;
- o "Até breve!" no fim.

O resto do sistema continua funcionando: preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar e
as reservas simultâneas. A única diferença fora do pedido é que agora há uma linha em branco entre o telefone da
recepção e o "Até breve!" (`ReservationService.java:131`). O dono pediu que o resto do e-mail ficasse "como está",
mas essa linha em branco só muda o espaçamento e o roteiro não a acusa, por isso não pesa na nota.

## Dúvidas

- A linha em branco antes do "Até breve!": dá para ler como uma escolha razoável de legibilidade ou como um pequeno
  desvio de "o resto do e-mail continua como está". Tratei como desvio menor, sem peso nas notas.
- Os testes: dei `com problemas` porque duas das três linhas novas não têm conferência. Para uma mudança só de
  texto, outro revisor poderia aceitar o teste como está (o assunto é conferido por igualdade exata e uma linha nova
  está lá) e dar `bom`.
