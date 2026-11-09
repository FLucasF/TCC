# Revisão: manutenção, cenário 10

## Visão geral

O sistema da base tem sete pacotes: `hotel` (montagem e `main`), `hotel.web` (as rotas Javalin), `hotel.reservations` (regras e acesso a dados das reservas), `hotel.rooms`, `hotel.messages`, `hotel.documents` (comprovante) e `hotel.reports` (os dois CSV). Uma requisição entra pelo `HttpApi`, que lê o pedido e chama o `ReservationService` ou um relatório. Esses chamam `ReservationStore`/`Rooms`, que usam JDBC com parâmetros sobre um PostgreSQL criado por Flyway. O pedido era pequeno: uma coluna `guest`, logo depois de `reservation`, no relatório de ocupação, com a vírgula do nome trocada por espaço. O agente fez exatamente isso. O `changes.patch` muda uma linha do cabeçalho e acrescenta uma linha de código em `src/main/java/hotel/reports/OccupancyReport.java:34,40`, e ajusta os testes do relatório em `src/test/java/hotel/reports/ReportsTest.java`. O `diff -rq` entre `base/` e `final/` confirma que nada mais mudou. Não achei nada sério. O único ciclo entre pacotes e as poucas fraquezas de banco já vinham da base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base é igual: os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. A única diferença é `hotel.reports`, que passou de 95 para 96 linhas (`measures/dependencies.md` comparado com `measures/dependencies-base.md`).
**Nota:** bom

A mudança ficou onde o relatório já mora (`src/main/java/hotel/reports/OccupancyReport.java:40`). Ela não criou nenhuma dependência nova: o nome vem de `Reservation.guest()`, que o relatório já usava. A borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:83-86` só lê a data e devolve o CSV). As regras não usam Javalin, e o JDBC fica nas classes de acesso a dados. O ciclo `documents` ↔ `reservations` (`src/main/java/hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`, e `src/main/java/hotel/documents/ReservationReceipt.java:3` importa `Reservation`) já existia na base. O agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, com 1 linha alterada e 1 acrescentada. Nenhuma classe, interface ou método novo em `src/main`. Nos testes, um método auxiliar a mais (uma sobrecarga de `book`).
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

A frase que tentava ("talvez um dia a recepção peça o telefone do hóspede também") não levou a nenhuma estrutura nova. Não há esqueleto de relatórios, colunas configuráveis nem abstração de coluna: a coluna entrou no cabeçalho (`src/main/java/hotel/reports/OccupancyReport.java:34`) e no `StringBuilder` que já montava a linha (`src/main/java/hotel/reports/OccupancyReport.java:40`). A vírgula é trocada por espaço com `replace(',', ' ')`, sem nada a mais. É a resposta `contenção` que `references/controls.md` descreve para este cenário.

O resto do código vem da base e está limpo. Dinheiro em `BigDecimal` com arredondamento num lugar só (`src/main/java/hotel/reservations/PriceCalculator.java:50`), datas em `LocalDate`, situação e plano em `enum`. Não há `static` mutável, e os erros de SQL são traduzidos sem ser engolidos (`src/main/java/hotel/reservations/ReservationStore.java:54`). O `catch (Exception)` em `src/main/java/hotel/web/HttpApi.java:113` é da base, está comentado e devolve 400, o que é razoável. No que o agente escreveu, só um detalhe de formatação: falta um espaço depois da vírgula em `src/test/java/hotel/reports/ReportsTest.java:35` (`new Guest(name,"bruno@example.com", ...)`).

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos de teste escritos. Cobertura de linhas de 93,8% (472 de 503) e de ramos de 87,1% (101 de 116). No pacote `hotel.reports`: 49 de 49 linhas e 22 de 22 ramos (`measures/tests.md`). A base tinha os mesmos 26 testes, todos passando, com 471 de 502 linhas e 101 de 116 ramos (`measures/tests-base.md`).
**Nota:** bom

O agente não apagou nem enfraqueceu nenhum teste da base. Ele atualizou os dois testes de ocupação para conferir, com valores, o CSV inteiro com a coluna nova: `src/test/java/hotel/reports/ReportsTest.java:64-69` confere `Bruno Lima` nas duas linhas. Para cobrir a vírgula, a reserva da suíte passou a se chamar `Lima, Bruno` (`src/test/java/hotel/reports/ReportsTest.java:49`), e `src/test/java/hotel/reports/ReportsTest.java:76-80` confere `Lima  Bruno`.

A pergunta da mutação tem resposta boa. Sem o `replace`, o teste da linha 76 falha. Com a coluna fora do lugar ou fora do cabeçalho, os dois testes falham. Se a vírgula virasse outro caractere, também falham. A comparação do texto inteiro protege ainda "o resto do relatório continua igual".

O teste pela API (`src/test/java/hotel/web/HttpApiTest.java:113-116`) só confere o status, o tipo e o total, mas isso já era assim na base e a regra nova fica coberta no nível do relatório. O agente não escreveu um método de teste novo: reaproveitou os dois que já existiam. Isso basta para uma coluna de uma linha.

## Banco

**Números:** 3 tabelas, 1 migração Flyway (`V1__create_tables.sql`, igual à da base). Nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. As conferências só acusaram as colunas de situação e de tipo sem CHECK (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`) e nenhuma transação explícita (`measures/database.md`).
**Nota:** bom

O pedido não precisava mexer no banco, e o agente não mexeu: o nome já está em `guest_name text NOT NULL` (`src/main/resources/db/migration/V1__create_tables.sql:22`), e a migração da base ficou intacta. A sobreposição de reservas é protegida pelo próprio banco, com um `EXCLUDE USING gist` sobre o quarto e o intervalo de datas. O SQL montado com `+` só concatena constantes (`src/main/java/hotel/reservations/ReservationStore.java:18,98,111`).

Três pontos menores vêm da base, não do agente:
- as colunas de situação e de tipo não têm CHECK;
- o relatório de ocupação carrega todas as reservas para filtrar em Java (`src/main/java/hotel/reports/OccupancyReport.java:25`, `store.all()`);
- não há transações explícitas.

## Comportamento

**Números:** 55 de 55 conferências passaram, nenhuma falhou (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 10: 4 de 4;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Pela API, o relatório de ocupação traz a coluna `guest` logo depois de `reservation`, troca a vírgula do nome por espaço e mantém a linha do total. O relatório financeiro continua com o mesmo cabeçalho. Todo o resto do sistema segue funcionando como na base: preços, recusas, confirmação e e-mail, check-in e check-out, cancelamentos, dados depois de reiniciar e as reservas simultâneas. Para o dono da pousada, a recepção já vê o nome do hóspede na ocupação, e nada do que funcionava quebrou.

## Dúvidas

Um nome com aspas ou com quebra de linha ainda bagunçaria o CSV (`src/main/java/hotel/reports/OccupancyReport.java:40` só troca a vírgula). O pedido, porém, só fala da vírgula, e a validação da API (`src/main/java/hotel/web/HttpApi.java:118`) só recusa nome em branco. Não contei isso contra o agente.
