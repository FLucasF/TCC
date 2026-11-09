# Revisão: manutenção, cenário 04

## Visão geral

O sistema da base está organizado em sete pacotes por assunto (`hotel`, `hotel.web`, `hotel.reservations`,
`hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports`). Uma requisição entra pelas rotas Javalin de
`hotel.web.HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-79`), que só leem a entrada e chamam o
`ReservationService`; as regras ficam no serviço e no `PriceCalculator`, o SQL em `ReservationStore`, `Rooms` e
`SentMessages`, e tudo é montado num lugar só, `src/main/java/hotel/Hotel.java:20-28`. Os dados ficam no PostgreSQL,
com o esquema criado por uma migração Flyway. O pedido era pontual: trocar o assunto do e-mail de confirmação e
substituir a linha de entrada e saída por três linhas novas, com uma frase vaga sobre o WhatsApp no futuro. O agente
fez exatamente isso: mudou duas linhas em `ReservationService` (`src/main/java/hotel/reservations/ReservationService.java:67`
e `:129-131`), sem estrutura nova, e atualizou o teste do e-mail. O `diff -rq base final` só mostra esses dois
arquivos. Não achei nada sério: os 26 testes passam, as 58 conferências do roteiro passam e o banco não mudou.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e um ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais em `measures/dependencies.md` e `measures/dependencies-base.md`. A única diferença é `hotel.reservations`, que passou de 508 para 510 linhas.
**Nota:** bom

A mudança ficou onde o texto do e-mail já estava, sem dependência nova. A borda HTTP é fina
(`src/main/java/hotel/web/HttpApi.java:62-79`), o Javalin só aparece em `hotel` e `hotel.web`, e o JDBC fica nas
classes de dados. O ciclo `documents` ↔ `reservations` veio da base: `ReservationReceipt` recebe um `Reservation`
(`src/main/java/hotel/documents/ReservationReceipt.java:3,24`), e o serviço cria o comprovante
(`src/main/java/hotel/reservations/ReservationService.java:3,64`). O agente não o piorou.

## Design de baixo nível

**Números:** a mudança tem 1 linha no assunto (`ReservationService.java:67`) e 3 linhas no corpo no lugar de 1 (`ReservationService.java:129-131`). Não há classe, interface nem método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é o controle negativo. A frase sobre o WhatsApp tentava provocar um canal de mensagem abstrato, mas o agente
não criou nenhum. Mudou o assunto e o texto em `confirmationEmailBody`
(`src/main/java/hotel/reservations/ReservationService.java:125-133`), que é a resposta certa pelos critérios. Também
reaproveitou a constante `HOTEL_NAME` no assunto novo (`ReservationService.java:67`) em vez de repetir o nome da
pousada. O resto do código vem da base e está em ordem: dinheiro em `BigDecimal`, datas em `LocalDate`, situações e
planos em `enum` (o `switch` do reembolso em `ReservationService.java:82-85`), erros com código próprio
(`HotelException`/`ErrorCode`) e métodos curtos.

## Testes

**Números:** 26 testes rodaram (0 falhas, 0 erros, 0 pulados) e há 26 métodos escritos; cobertura de 93,8% das linhas (471/502) e 87,1% dos ramos (101/116). Os números são idênticos aos da base (`measures/tests-base.md`).
**Nota:** bom

O agente não criou teste novo, mas atualizou o que confere o e-mail. `confirmingSendsTheReceiptByEmail`
(`src/test/java/hotel/reservations/ReservationServiceTest.java:84-101`) confere o assunto novo com `assertEquals`
(`:94`) e o bloco das três linhas novas, em ordem, seguido de "Até breve!" (`:97-100`). As asserções do comprovante
continuam (`:95-96`). A pergunta da mutação: trocar uma palavra do assunto ou de qualquer das três linhas, ou apagar
uma delas, derruba o teste. Ficam duas brechas pequenas. O teste não confere que a linha antiga saiu: o `contains`
continuaria passando se ela ficasse antes do bloco novo. E ele também não confere a saudação "Olá, ...". O roteiro do
comportamento cobre as duas coisas, mas elas não têm teste no projeto. Nenhum teste da base foi apagado ou
enfraquecido.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em texto. As conferências só acusaram as colunas de situação e de tipo sem CHECK (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`). A migração `V1__create_tables.sql` está igual à da base.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: nenhuma migração editada nem acrescentada. O esquema protege a
regra principal com um `EXCLUDE USING gist` sobre o quarto e o intervalo de datas das reservas não canceladas. O SQL
usa parâmetros: os `+` que o script lista em `src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`
só juntam constantes. Dois pontos vieram da base e não são do agente. As colunas de situação não têm CHECK. E, em
`confirm`, a atualização da reserva e a gravação do e-mail correm em operações separadas, sem transação
(`ReservationService.java:63-68`).

## Comportamento

**Números:** 58 de 58 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 7 de 7 no cenário 04 e 2 de 2 nas reservas simultâneas.
**Nota:** bom

O hóspede recebe o e-mail com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O corpo mantém a
saudação e o comprovante, traz as três linhas novas na ordem pedida e termina com o "Até breve!", e a linha antiga
saiu. Todo o resto continua funcionando como antes: preços, recusas, cancelamentos, relatórios, dados depois de
reiniciar e reservas concorrentes. Para o dono da pousada, a mudança está pronta para uso.

## Dúvidas

nenhuma
