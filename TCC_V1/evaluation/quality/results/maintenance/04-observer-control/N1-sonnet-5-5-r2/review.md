# Revisão: manutenção, cenário 04

## Visão geral

A base é um serviço Javalin em sete pacotes. O `hotel` monta as peças (`src/main/java/hotel/Hotel.java:20-29`). O
`hotel.web` é a borda HTTP (`HttpApi`). O `hotel.reservations` guarda as regras (`ReservationService`,
`PriceCalculator`) e o acesso aos dados das reservas (`ReservationStore`). O `hotel.messages` guarda o envio e o
registro dos e-mails. O `hotel.documents` faz o comprovante, e há ainda `hotel.rooms` e `hotel.reports`. Uma
requisição de confirmação entra por `HttpApi.java:70-71`. Ela chama `ReservationService.confirm`
(`ReservationService.java:57-70`), que grava a reserva pelo `ReservationStore` e manda o e-mail pelo `EmailSender`.
O `EmailSender` grava a mensagem na tabela `messages` (`SentMessages.java:24-28`). Os dados ficam no PostgreSQL, num
esquema criado por uma migração do Flyway. O pedido era mudar o assunto e trocar uma linha do corpo do e-mail de
confirmação por três. O agente fez exatamente isso. A mudança é de 2 arquivos e 3 trechos: o assunto
(`ReservationService.java:67`), as três linhas no lugar da antiga (`ReservationService.java:129-131`) e a asserção
do teste atualizada (`src/test/java/hotel/reservations/ReservationServiceTest.java:94-98`). Não criou nenhuma
estrutura para o "talvez um dia por WhatsApp". O `mvn verify` passa, e as 58 conferências do roteiro também.
Encontrei apenas pontos menores, todos vindos da base.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já
existia na base: `measures/dependencies-base.md` é igual a `measures/dependencies.md`, exceto pelas 2 linhas a mais
em `ReservationService` (138 → 140).
**Nota:** bom

O agente não mexeu na estrutura: nenhum pacote, classe ou dependência nova. A borda HTTP é fina: cada rota só lê a
requisição, chama o serviço e escreve a resposta (`src/main/java/hotel/web/HttpApi.java:61-91`). As regras não
conhecem o Javalin (pela tabela de bibliotecas, só `hotel` e `hotel.web` usam Javalin). A montagem fica num lugar só
(`src/main/java/hotel/Hotel.java:20-29`). O texto do e-mail continua onde já morava, em
`ReservationService.confirmationEmailBody` (`src/main/java/hotel/reservations/ReservationService.java:125-133`).

O ciclo vem da base, e o agente não o piorou: `ReservationService.java:3` usa `ReservationReceipt`, e
`ReservationReceipt` usa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`).

## Design de baixo nível

**Números:** 3 trechos mudados no código de produção, todos dentro de `ReservationService`: 1 linha no assunto e
1 linha trocada por 3 no corpo. Nenhuma classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é um controle negativo, com a frase "talvez um dia a gente mande essa confirmação por WhatsApp também". A
resposta certa, pelos critérios de `controls.md`, é mudar o assunto e o texto onde eles já moram. Foi o que o agente
fez:

- O assunto passou a ser `"Sua reserva na " + HOTEL_NAME + " está confirmada!"` (`ReservationService.java:67`). Ele
  reaproveita a constante `HOTEL_NAME` (`ReservationService.java:12`), como a base já fazia.
- As três linhas novas entraram no lugar da linha do horário (`ReservationService.java:129-131`). A saudação, o
  comprovante e o "Até breve!" ficaram intactos (`ReservationService.java:126-128,132`).

O agente não criou canal de mensagem abstrato, interface `MessageSender`, ouvinte de confirmação nem modelo de
mensagem. O `EmailSender` continua igual (`src/main/java/hotel/messages/EmailSender.java:4-15`).

O resto do código vem da base e é limpo:

- dinheiro em `BigDecimal`, arredondado a centavos em `PriceCalculator.cents` (`PriceCalculator.java:50-52`);
- datas em `LocalDate`;
- planos e situações em `enum`;
- o estado da `Reservation` só muda por métodos com visibilidade de pacote (`Reservation.java:102-114`).

O único `catch (Exception)` é o da leitura do corpo JSON (`HttpApi.java:113`). Ele traduz a falha para
`INVALID_REQUEST` e está comentado. Também vem da base.

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 26 métodos escritos, os mesmos 26 da base.
Cobertura de 93,8% das linhas (471 de 502) e de 87,1% dos ramos (101 de 116), igual à base
(`measures/tests-base.md` não difere de `measures/tests.md`).
**Nota:** bom

O agente atualizou o teste que já conferia o e-mail, sem apagar nem enfraquecer nenhum outro.
`confirmingSendsTheReceiptByEmail` confere:

- o assunto novo, por igualdade exata (`src/test/java/hotel/reservations/ReservationServiceTest.java:94`);
- as três linhas novas, em ordem, seguidas de "Até breve!\n" (`ReservationServiceTest.java:95-98`);
- o comprovante com os valores (`ReservationServiceTest.java:99-100`).

A pergunta da mutação: se alguém mudasse o assunto, trocasse um horário ou o telefone, invertesse as linhas ou
apagasse uma delas, o teste falharia. Duas coisas ficaram sem teste:

- que a linha antiga ("A entrada é a partir das 14h, e a saída, até as 12h.") saiu. Se ela voltasse acima das novas,
  o `contains` continuaria passando;
- que a saudação "Olá, …!" continua.

As duas lacunas são pequenas, e o roteiro do comportamento confere as duas coisas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Todo o dinheiro está em
`numeric(10,2)`. A chave estrangeira tem índice e não há datas em texto. Há uma restrição `EXCLUDE` contra reserva
dupla. 1 migração Flyway, `V1__create_tables.sql`, igual à da base.
**Nota:** bom

O agente não tocou no banco: a migração está "igual" à da base (`measures/database.md`). O pedido não exigia mudança
no banco, e o e-mail novo cabe na coluna `body text` (`src/main/resources/db/migration/V1__create_tables.sql:49`).

A reserva dupla é protegida pelo próprio banco, com uma restrição `EXCLUDE` (`V1__create_tables.sql:38-40`) que não
depende do `synchronized`. Os candidatos a "SQL montado com `+`" em `ReservationStore.java:33,83,98,111` só
concatenam constantes (`COLUMNS`), e os valores entram por `?`.

Três pontos menores, todos da base:

- `status`, `rate_plan`, `type` e `channel` são texto sem `CHECK` (`V1__create_tables.sql:4,28,33,46`);
- os relatórios carregam a tabela inteira de reservas (`ReservationStore.java:108-111`);
- não há transação explícita.

## Comportamento

**Números:** 58 de 58 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- reinício: 3 de 3;
- cenário 04: 7 de 7;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, o resultado é este:

- o hóspede recebe o e-mail com o assunto novo, a saudação, o comprovante, as três linhas novas na ordem pedida e o
  "Até breve!" no fim;
- a linha antiga do horário saiu;
- todo o resto continua como antes: preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar e as
  duas reservas simultâneas.

## Dúvidas

nenhuma
