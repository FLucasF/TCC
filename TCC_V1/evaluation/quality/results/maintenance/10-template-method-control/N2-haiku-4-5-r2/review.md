# Revisão: manutenção, cenário 10

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web.HttpApi` lê a requisição e chama a regra: `ReservationService` para o ciclo de vida da reserva, `OccupancyReport` e `RevenueReport` para os CSVs. Os dados ficam no PostgreSQL, com acesso isolado em `ReservationStore`, `Rooms` e `SentMessages`, e o esquema é criado por uma migração Flyway (`V1__create_tables.sql`). O pedido era uma coluna `guest` no relatório de ocupação, logo depois de `reservation`, com a vírgula do nome trocada por espaço. O agente mudou só dois arquivos (`diff -rq base final`): `src/main/java/hotel/reports/OccupancyReport.java` ganhou o cabeçalho novo (linha 34), a troca da vírgula (linha 37) e a coluna (linha 41), e `ReportsTest.java` teve os textos esperados atualizados. Não criou nenhuma estrutura para a frase sobre o telefone. O comportamento passa por inteiro (55/55). O ponto mais fraco é o teste: nenhum teste confere a troca da vírgula.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` contra `measures/dependencies-base.md`; a única diferença são as 2 linhas a mais em `hotel.reports`, que foi de 95 para 97 linhas). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
**Nota:** bom

A mudança fica dentro de `hotel.reports` e não acrescenta nenhuma dependência: `OccupancyReport` já usava `Reservation`, e `reservation.guest()` vem de lá (`OccupancyReport.java:3`, `:37`). A borda HTTP continua fina: a rota só lê a data e devolve o CSV (`web/HttpApi.java:83-86`). O relatório não conhece nem o Javalin nem o JDBC. O ciclo vem da base: `ReservationService` importa `ReservationReceipt` (`reservations/ReservationService.java:3`), e `ReservationReceipt` importa `Reservation` (`documents/ReservationReceipt.java:3`). O agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, com +3 linhas e −1. Nenhuma classe, interface ou método novo. A coluna nova está no mesmo laço que monta as outras colunas.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O critério do controle 10 diz que a resposta contida é "uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço", e foi exatamente isso. O cabeçalho mudou em `OccupancyReport.java:34`, o nome sai de `reservation.guest().name().replace(",", " ")` numa variável local com nome claro (`:37`) e a coluna entra na posição pedida (`:41`). A frase "talvez um dia a recepção peça o telefone" não virou nada no código: nenhum esqueleto de relatório nem colunas configuráveis. O resto do código é da base, que está limpa: dinheiro em `BigDecimal`, datas em `LocalDate`, `enum` para situação e plano, erros com `ErrorCode` (`web/HttpApi.java:100-107`) e nenhum `static` mutável. O agente não acrescentou duplicação nem método longo. Um nome com aspas ou com quebra de linha ainda bagunçaria o CSV, mas o pedido só falava da vírgula, e tratar só ela é a leitura literal e sensata.

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 26 métodos escritos, os mesmos 26 da base. Cobertura de 93,8% das linhas (473/504) e 87,1% dos ramos (101/116); em `hotel.reports`, 50/50 linhas e 22/22 ramos (`measures/tests.md`). A base tinha 471/502 linhas e os mesmos ramos.
**Nota:** com problemas

O agente manteve os testes da base passando, não apagou nenhum e atualizou os dois testes de ocupação para conferirem o CSV inteiro com a coluna nova, com valores (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `:72-76`). Isso protege o cabeçalho e a posição da coluna. Mas nenhum teste confere a regra nova da vírgula: todas as reservas do teste usam o nome `"Bruno Lima"` (`ReportsTest.java:31`), que não tem vírgula. Pela pergunta da mutação:

- se alguém apagasse o `.replace(",", " ")` de `OccupancyReport.java:37`, todos os testes continuariam passando;
- como todas as reservas têm o mesmo hóspede, um erro que pusesse o nome de outra reserva na linha também passaria;
- o teste da API só confere o fim do corpo (`web/HttpApiTest.java:116`).

A parte nova está pela metade sem rede.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto, e a restrição `EXCLUDE` contra duas reservas na mesma noite. Só as colunas de situação e de tipo aparecem sem `CHECK`. A migração `V1__create_tables.sql` está igual à da base, e não há migração nova (`measures/database.md`).
**Nota:** bom

O pedido não precisava mudar o banco: o nome do hóspede já é guardado em `guest_name text NOT NULL` (`src/main/resources/db/migration/V1__create_tables.sql:22`) e já é lido em `ReservationStore.java:124`. O agente, com razão, não tocou no esquema nem nas migrações. O que as conferências apontam vem da base: `status`, `rate_plan`, `type` e `channel` sem `CHECK`. O relatório de ocupação também lê a tabela inteira de reservas para filtrar em Java (`OccupancyReport.java:25` sobre `ReservationStore.java:108-111`), uma carga que cresce com o tempo. Nenhum dos dois é do agente.

## Comportamento

**Números:** 55 de 55 conferências passaram: a parte comum 46/46, depois de reiniciar 3/3, o cenário 10 4/4 e as reservas simultâneas 2/2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, o relatório de ocupação agora traz a coluna `guest` logo depois de `reservation`, e um nome com vírgula sai com espaço no lugar. O total e o relatório financeiro continuam iguais, e tudo o que já funcionava (preços, recusas, confirmação, cancelamentos, dados depois de reiniciar, duas reservas ao mesmo tempo) segue funcionando.

## Dúvidas

Nenhuma.
