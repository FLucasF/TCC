# Revisão: manutenção, cenário 10

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` monta as peças (`Hotel.java:21-27`) e abre o pool (`Database.java:23-27`), `hotel.web` é a borda HTTP, `hotel.reservations` tem as regras e o acesso a dados (`ReservationStore`), `hotel.reports` tem os dois CSVs, e as reservas ficam no PostgreSQL, num esquema criado por migração do Flyway. Uma requisição a `GET /reports/occupancy` passa por `HttpApi.java:83-86`, que lê a data e chama `OccupancyReport.generate`. O relatório lê as reservas por `ReservationStore.all()` e monta o CSV. O pedido era uma coluna `guest` logo depois de `reservation`, com as vírgulas do nome trocadas por espaço. O agente mudou só dois arquivos (`changes.patch`, e `diff -rq base final` confirma): três linhas em `OccupancyReport.java` (34, 37, 41) e o `ReportsTest`, onde ajustou dois testes e acrescentou um. É a mudança pequena, no lugar certo, e não criou nenhuma estrutura para o "telefone, talvez um dia". Não há nada sério: todos os testes passam e as 55 conferências do roteiro também.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são iguais aos de `measures/dependencies-base.md`; só `hotel.reports` passou de 95 para 97 linhas.
**Nota:** bom

O agente não mudou nenhuma dependência. A coluna nova usa `reservation.guest()`, que o relatório já recebia por `ReservationStore` (`OccupancyReport.java:3-4`, `:37`), então não há import novo nem pacote novo. A borda continua fina: a rota só lê a data e escreve o CSV (`web/HttpApi.java:83-86`). As regras não conhecem o Javalin (`hotel.reservations` não usa Javalin, segundo `dependencies.md`), e a montagem fica num lugar só (`Hotel.java:21-27`). O único ciclo veio da base: `reservations/ReservationService.java:3` importa `documents.ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `reservations.Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de código mudado, 3 linhas trocadas ou acrescentadas (`OccupancyReport.java:34`, `:37`, `:41`), 0 classes, interfaces ou métodos novos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Pelos critérios do controle 10, a resposta certa é "uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço", e foi exatamente isso. O cabeçalho ganhou `guest` depois de `reservation` (`OccupancyReport.java:34`). O nome passa por `replace(',', ' ')` numa variável local (`:37`) e entra na posição certa (`:41`). Não há esqueleto genérico de relatório, nem colunas configuráveis, nem um "formatador de CSV" ou interface pensada para um telefone que o sistema nem guarda. O agente também não refatorou o `RevenueReport` nem outra parte da base.

O resto do código é da base e está limpo:
- o dinheiro é `BigDecimal` com arredondamento centralizado (`PriceCalculator.cents`, usado em `ReservationService.java:84`);
- as datas são `LocalDate`;
- situação e plano são `enum`, com `switch` exaustivo (`ReservationService.java:82-85`, `HttpApi.java:100-106`);
- os erros carregam um código próprio (`HotelException`/`ErrorCode`), e o `SQLException` é embrulhado com a causa (`ReservationStore.java:59`).

Um único ponto menor, e da base: o `catch (Exception)` em `HttpApi.java:113` vira 400 sem guardar a causa, mas está documentado e é localizado.

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros, 0 pulados, 27 métodos escritos (a base tinha 26). Cobertura de linhas 93,8% (473/504) e de ramos 87,1% (101/116); `hotel.reports` tem 50/50 linhas e 22/22 ramos.
**Nota:** bom

O agente atualizou os dois testes de ocupação para o CSV novo e conferiu o texto inteiro, com valores (`src/test/java/hotel/reports/ReportsTest.java:60-65`, `:72-76`). Também acrescentou um teste para a regra da vírgula: o nome `"Silva, João"` precisa sair `Silva  João`, na coluna certa (`ReportsTest.java:80-93`). Nenhum teste da base foi apagado ou enfraquecido: o patch só troca as linhas esperadas.

A pergunta da mutação, nas três partes da regra nova:
- sem o `replace`, a linha esperada em `:90` não bate;
- com a coluna em outra posição, ou fora do cabeçalho, os três `assertEquals` falham;
- se o nome viesse de outro campo (o e-mail, por exemplo), `Bruno Lima` em `:62` falha.

Nenhuma das três ficaria sem rede. O teste pela API (`web/HttpApiTest.java:113-116`) só confere o status, o tipo e o total, mas isso vem da base, e a regra fica coberta no nível do relatório.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` nas 6 colunas; 0 chaves estrangeiras sem índice; 0 datas em texto. A migração `V1__create_tables.sql` está igual à da base, e não há migração nova. Há 4 colunas de situação ou tipo sem CHECK (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`).
**Nota:** bom

O pedido não precisava de mudança no esquema, porque o nome já é guardado (`src/main/resources/db/migration/V1__create_tables.sql:22`, `guest_name text NOT NULL`), e o agente não tocou no banco nem editou a migração. A dupla reserva é barrada no próprio banco (`EXCLUDE USING gist`, `V1__create_tables.sql:38-40`), e o SQL usa parâmetros. Os quatro candidatos a SQL montado com `+` só juntam constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`). Duas observações vêm da base e o agente não as piorou:
- as colunas de situação ou tipo não têm CHECK (`V1__create_tables.sql:4`, `:28`, `:33`, `:46`);
- o relatório de ocupação traz a tabela inteira (`ReservationStore.java:111`) para filtrar em Java (`OccupancyReport.java:25-30`).

## Comportamento

**Números:** 55 de 55 conferências passaram, 0 falharam:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 10: 4/4;
- reservas simultâneas: 2/2.
**Nota:** bom

Pela API, o cabeçalho da ocupação traz `guest` logo depois de `reservation`, a vírgula do nome vira espaço, o total continua certo e o relatório financeiro não mudou. O resto do sistema continua funcionando: preço, recusas, ciclo da reserva, devoluções e os dados depois de reiniciar. Nas dez rodadas de duas reservas simultâneas, uma foi criada e a outra recusada com 409. Para o dono da pousada, a recepção já vê o nome do hóspede, e nada do que funcionava parou.

## Dúvidas

- A troca da vírgula por espaço é literal. Um nome como `"Silva, João"` vira `Silva  João`, com dois espaços (`ReportsTest.java:90`). Essa é a leitura literal do pedido; juntar os espaços também seria aceitável, e o texto não decide entre as duas.
- O nome não é tratado contra outros caracteres que bagunçariam o CSV (aspas ou quebra de linha). O pedido só falou da vírgula, então não contei como defeito.
