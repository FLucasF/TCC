# Revisão: manutenção, cenário 04

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` (montagem e banco), `hotel.web` (rotas), `hotel.reservations`
(regras e acesso a dados das reservas), `hotel.rooms`, `hotel.messages`, `hotel.documents` (comprovante) e
`hotel.reports`. Uma requisição entra pela `HttpApi`, chama o `ReservationService`, que usa o `ReservationStore`
(JDBC com pool) sobre o PostgreSQL, com o esquema criado pelo Flyway. O pedido era mudar o assunto e o texto do e-mail
de confirmação, e a frase sobre o WhatsApp no fim estava lá para tentar o agente a criar estrutura. O agente mudou só
duas coisas: o assunto (`src/main/java/hotel/reservations/ReservationService.java:67`) e as linhas do corpo em
`confirmationEmailBody` (`ReservationService.java:129-131`). Também atualizou o teste que já existia
(`src/test/java/hotel/reservations/ReservationServiceTest.java:94-98`). Não criou nenhuma abstração de canal. As 58
conferências do comportamento passaram, e os 26 testes também. Não achei nada sério.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos na base: só as linhas de `hotel.reservations` mudaram, de 508 para 510 (`measures/dependencies.md` contra
`measures/dependencies-base.md`).
**Nota:** bom

A mudança fica num método privado de uma classe de regras e não mexe na estrutura. As camadas continuam separadas.
Só `hotel` e `hotel.web` usam o Javalin. O JDBC fica nas classes de dados (`ReservationStore`, `Rooms`,
`SentMessages`). A montagem fica toda em `src/main/java/hotel/Hotel.java:21-27`. O ciclo entre documents e
reservations vem da base: `ReservationReceipt` importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`),
e o serviço cria o comprovante (`ReservationService.java:3`, `:64`). O agente não o piorou.

## Design de baixo nível

**Números:** 2 arquivos mudados (1 de produção e 1 de teste). Nenhuma classe, interface ou método novo. Na produção,
mudaram 1 linha do assunto e 1 linha do corpo, que virou 3.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido dava ao agente a deixa para construir um canal de mensagem abstrato ("talvez um dia por WhatsApp"). Ele não a
seguiu. O texto foi mudado onde já morava: o assunto na chamada a `emailSender.send` (`ReservationService.java:65-68`),
reaproveitando a constante `HOTEL_NAME` (`:12`), e as três linhas no lugar da antiga, em `confirmationEmailBody`
(`:125-133`). É exatamente a contenção que `controls.md` descreve para este cenário. A saudação, o comprovante e o
"Até breve!" ficaram como estavam. `EmailSender` continua com um método só (`src/main/java/hotel/messages/EmailSender.java:9-11`).

O resto do código vem da base e é limpo. O dinheiro é `BigDecimal` com arredondamento em um lugar só
(`PriceCalculator.java:50-52`), os planos e as situações são `enum`, e a reserva só muda por métodos privados do pacote
(`Reservation.java:87-99`). O agente não acrescentou duplicação nem estado novo.

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 26 métodos escritos. A cobertura é de 93,8% das
linhas (471 de 502) e 87,1% dos ramos (101 de 116), idêntica à da base (`measures/tests.md` contra
`measures/tests-base.md`).
**Nota:** bom

O agente atualizou o teste que já conferia o e-mail, sem apagar nem enfraquecer nada. O assunto novo é conferido por
igualdade (`src/test/java/hotel/reservations/ReservationServiceTest.java:94`). As três linhas novas são conferidas
juntas, em ordem e logo antes do "Até breve!" (`:95-98`). O comprovante continua conferido com valores (`:99-100`).

A pergunta da mutação: se alguém mudasse o assunto, uma das três linhas ou a ordem delas, o teste falharia. Duas coisas
ficam sem rede:

- a linha antiga ("A entrada é a partir das 14h, e a saída, até as 12h.") continuar no corpo, acima das novas;
- a saudação "Olá, ..." sumir.

Nenhuma das duas era conferida na base. É uma lacuna pequena, que um `assertFalse(body.contains(...))` resolveria, e
não basta para tirar a nota.

## Banco

**Números:** 3 tabelas, 1 migração Flyway (`V1__create_tables.sql`), igual à da base. O dinheiro está todo em
`numeric(10,2)`. Toda tabela tem chave primária, e a chave estrangeira tem índice. As datas não estão em texto. Há um
`EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma noite. A única conferência que acusou algo: 4
colunas de situação ou tipo em `text` sem `CHECK` (`measures/database.md`).
**Nota:** bom

O pedido não tocava o banco, e o agente não o tocou: a migração está igual à da base. As colunas `status`,
`rate_plan`, `type` e `channel` sem `CHECK` vêm da base (`src/main/resources/db/migration/V1__create_tables.sql`). O SQL
montado com `+` só junta constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`). Os dados entram por parâmetros
`?`.

## Comportamento

**Números:** 58 de 58 conferências passaram, nenhuma falhou. Por parte: a parte comum 46 de 46, depois de reiniciar
3 de 3, o cenário 04 7 de 7, e as reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Pela API, o hóspede recebe um e-mail só, com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O e-mail
mantém a saudação e o comprovante, traz as três linhas novas em ordem e termina com o "Até breve!". A linha antiga
saiu. Todo o resto do sistema continua funcionando para o dono da pousada como antes: preços, recusas, cancelamentos,
relatórios, os dados depois de reiniciar e a proteção contra reserva dupla.

## Dúvidas

nenhuma
