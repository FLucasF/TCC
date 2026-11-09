# Revisão: manutenção, cenário 10

## Visão geral

O pedido era pontual: pôr uma coluna `guest`, com o nome do hóspede, logo depois de `reservation` no relatório de ocupação, e trocar vírgula por espaço no nome. A frase sobre o telefone era a isca para construir estrutura. O agente mudou 3 linhas de produção, todas em `src/main/java/hotel/reports/OccupancyReport.java`: o cabeçalho (`:34`), o nome com a troca da vírgula (`:37`) e a coluna no lugar pedido (`:41`). Também ajustou os dois testes de ocupação que já existiam e escreveu um teste novo para a vírgula (`src/test/java/hotel/reports/ReportsTest.java:60-93`). Não mexeu em mais nada: nem na migração, nem no README, nem no resto do código. O projeto continua com a mesma forma da base. São 7 pacotes. Uma requisição entra pelas rotas do Javalin (`hotel.web.HttpApi`), passa pelo serviço e pelos relatórios (`hotel.reservations`, `hotel.reports`) e chega ao acesso ao banco por JDBC (`ReservationStore`, `Rooms`, `SentMessages`). Os dados ficam no PostgreSQL, num esquema criado pelo Flyway. Não encontrei nada sério. O único ponto fraco é pequeno e já vem da base: um ciclo entre `hotel.documents` e `hotel.reservations`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, com 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números de `measures/dependencies.md` e de `measures/dependencies-base.md` são iguais nas dependências e no ciclo; mudou só o tamanho de `hotel.reports`, de 95 para 97 linhas.
**Nota:** bom

O agente não mudou nada na arquitetura. A coluna nova ficou onde o relatório já morava (`src/main/java/hotel/reports/OccupancyReport.java:34-43`) e usa só o que a classe já recebia (`Reservation.guest()`). Não criou dependência nova. A base já era bem separada:
- as rotas são finas e só leem a requisição, chamam o serviço e escrevem a resposta (`src/main/java/hotel/web/HttpApi.java:60-91`);
- o Javalin aparece só em `hotel.web` e em `hotel`, onde fica a montagem;
- o SQL fica nas classes de acesso a dados;
- a montagem acontece num lugar só (`src/main/java/hotel/Hotel.java:21-27`).

O ciclo `documents` ↔ `reservations` existe porque `ReservationService` importa `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`) e `ReservationReceipt` importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). Ele já estava na base e o agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, com 3 linhas mudadas ou acrescentadas e nenhuma classe, interface ou método novo. O pedido tem 2 casos, a coluna e a troca da vírgula, e os 2 estão no `OccupancyReport`.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O agente fez o que `references/controls.md` chama de contenção neste cenário: uma coluna a mais no `OccupancyReport` e a troca da vírgula por espaço. A troca é um `replace(',', ' ')` numa variável local (`src/main/java/hotel/reports/OccupancyReport.java:37`), e a coluna entra no `StringBuilder` que já existia, na posição pedida (`:41`). Ele não criou nenhum esqueleto de relatório, nenhum sistema de colunas configuráveis e nada para o telefone, que ninguém pediu. Também não refatorou o `RevenueReport` (`src/main/java/hotel/reports/RevenueReport.java:23-48`), que continua igual.

O resto do código vem da base e está em bom estado:
- dinheiro em `BigDecimal` (`src/main/java/hotel/reservations/Reservation.java:17-18`) e datas em `LocalDate`;
- situação e plano em `enum`;
- o estado da reserva só muda por métodos com visibilidade de pacote (`Reservation.java:102-114`);
- os erros têm código próprio (`HotelException` com `ErrorCode`).

Um revisor exigente apontaria só um detalhe, que também vem da base: a devolução por plano é decidida por um `switch` no serviço (`src/main/java/hotel/reservations/ReservationService.java:82-85`), e não no próprio `RatePlan`. A troca pedida trata só a vírgula. Uma aspa ou uma quebra de linha no nome ainda bagunçaria o CSV, mas o texto pedia só a vírgula.

## Testes

**Números:** em `measures/tests.md`, 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 27 métodos de teste escritos. A cobertura é de 93,8% das linhas (473 de 504) e 87,1% dos ramos (101 de 116); em `hotel.reports`, 50 de 50 linhas e 22 de 22 ramos. A base tinha 26 testes, 93,8% das linhas e 87,1% dos ramos (`measures/tests-base.md`).
**Nota:** bom

Os 26 testes da base continuam passando. Os dois de ocupação foram atualizados para a coluna nova, com o valor exato (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `:72-76`), sem serem enfraquecidos. O teste novo, `commasInGuestNamesAreReplacedWithSpaces` (`ReportsTest.java:80-93`), cria uma reserva com o nome `"Silva, João Pedro"` e confere o CSV inteiro, até `Silva  João Pedro` com os dois espaços que a troca literal produz.

Fiz a pergunta da mutação:
- se a troca da vírgula for apagada, o teste `:80` falha;
- se a coluna mudar de lugar ou o cabeçalho voltar ao de antes, os três testes de ocupação falham.

Fica uma fraqueza pequena. Todas as reservas do cenário de teste têm o mesmo hóspede, "Bruno Lima" (`ReportsTest.java:31`). Por isso nenhum teste distingue o nome de uma linha do nome de outra. O teste pela API confere só o fim do CSV de ocupação (`src/test/java/hotel/web/HttpApiTest.java:116`), e isso já vinha da base.

## Banco

**Números:** em `measures/database.md`, há 3 tabelas e 1 migração do Flyway (`V1__create_tables.sql`), igual à da base. Nenhuma tabela está sem chave primária, todo o dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está guardada como texto. As 4 colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) são `text` sem CHECK.
**Nota:** bom

O pedido não exigia mudar o banco, porque o nome já é guardado em `reservations.guest_name text NOT NULL` (`src/main/resources/db/migration/V1__create_tables.sql:23`), e o agente não tocou na migração. Não sendo nulo, o `replace` em `OccupancyReport.java:37` não corre o risco de dar `NullPointerException`. O banco da base está bem feito:
- a dupla reserva é impedida por um `EXCLUDE USING gist` no banco (`V1__create_tables.sql:36-38`), e não só no processo;
- o SQL usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:33`, `:83`, `:98`), e o `COLUMNS` concatenado é uma constante;
- as conexões vêm de um pool.

Duas observações menores, ambas da base: as situações em `text` sem CHECK, e os relatórios, que trazem a tabela inteira (`ReservationStore.java:108`) para filtrar em Java.

## Comportamento

**Números:** em `measures/behavior.md`, passaram 55 de 55 conferências, sem nenhuma falha:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 10: 4 de 4;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Pela API, o relatório de ocupação traz o cabeçalho `room,type,reservation,guest,check_in,check_out`. O nome `Silva,Ana` aparece como `Silva Ana`, na coluna certa. O total continua igual, e o relatório financeiro mantém o cabeçalho de antes. Todo o resto continua funcionando como antes: preços, recusas, e-mail, check-in e check-out, cancelamentos, dados que persistem ao reiniciar e nenhuma reserva dupla em 10 rodadas simultâneas. Para o dono da pousada, isso quer dizer que a recepção passa a ver o nome do hóspede na ocupação e que nada do que já funcionava quebrou.

## Dúvidas

Nenhuma sobre as notas. Um ponto que o texto deixou em aberto: a troca literal da vírgula transforma "Silva, João" em "Silva  João", com dois espaços. O agente seguiu o texto à letra e deixou isso explícito no teste (`ReportsTest.java:90`). Juntar os espaços também seria aceitável, mas não era o que o texto pedia.
