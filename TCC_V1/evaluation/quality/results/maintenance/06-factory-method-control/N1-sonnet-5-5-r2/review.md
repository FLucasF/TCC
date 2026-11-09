# Revisão: manutenção, cenário 06

## Visão geral

O sistema (da base) tem sete pacotes: `hotel.web` (Javalin; `HttpApi` lê o JSON, chama o serviço e escreve a
resposta), `hotel.reservations` (as regras em `ReservationService` e o JDBC isolado em `ReservationStore`),
`hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel` (a montagem). Os dados
ficam no PostgreSQL, com o esquema criado por migrações do Flyway. O pedido era pontual: o campo `children` na
reserva (opcional, zero se faltar, nunca negativo), adultos e crianças juntos dentro da capacidade, criança sem
diária e a linha "Crianças: N" no comprovante. O agente fez exatamente isso, no lugar onde cada coisa já morava: um
campo no pedido HTTP, no `NewReservation`, na `Reservation`, na resposta e no comprovante, a soma na conferência da
capacidade, uma migração nova `V2__add_children.sql` com `NOT NULL DEFAULT 0 CHECK (children >= 0)`, e quatro testes
novos. Não acrescentou estrutura nenhuma por causa da frase sobre "cobrar das crianças maiores no futuro". Os 30
testes passam, e as 61 conferências do roteiro também. Não achei nada sério.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies-base.md`); só mudaram as linhas (reservations 508 → 521, web 236 → 240, documents
79 → 84).
**Nota:** bom

A mudança respeita as camadas que já existiam. A borda HTTP só traduz o campo opcional para zero
(`src/main/java/hotel/web/HttpApi.java:128-130`) e o expõe na resposta
(`src/main/java/hotel/web/ReservationResponse.java:18,38`). As regras novas (não negativo e a soma na capacidade)
ficam no serviço, que não conhece Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:34-44`).
O SQL novo fica só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-43,132`). O
ciclo `documents` ↔ `reservations` veio da base (`ReservationService.java:3` importa `ReservationReceipt`, que lê
`Reservation`). O agente não o criou nem o piorou: o comprovante passou a ler `reservation.children()`
(`src/main/java/hotel/documents/ReservationReceipt.java:33`) pela mesma aresta que já existia. Nenhuma dependência
nova entre pacotes.

## Design de baixo nível

**Números:** 13 arquivos tocados no `changes.patch` (8 de código, 1 migração, 3 de teste e o README). Nenhuma classe,
interface, builder, fábrica ou política de preço nova; a reserva continua criada direto pelo construtor.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É controle negativo. A resposta é exatamente a que `references/controls.md` chama de contenção: um campo a mais no
pedido (`src/main/java/hotel/reservations/NewReservation.java:6`, `src/main/java/hotel/web/CreateReservationRequest.java:15`)
e na reserva (`src/main/java/hotel/reservations/Reservation.java:14,38,71`), a soma na conferência da capacidade
(`ReservationService.java:40`), a linha no comprovante com um `if` igual ao do desconto, que já estava ali
(`ReservationReceipt.java:47-49`, ao lado de `:54-56`), e a coluna nova no banco. Não há nenhuma "política de preço
de criança": o `PriceCalculator` nem foi tocado, e a frase sobre o futuro ficou fora do código.

O resto do código continua no nível da base. O dinheiro está em `BigDecimal` e as datas em `LocalDate`. O campo
opcional é `Integer` só no DTO HTTP e vira `int` no domínio (`HttpApi.java:130`), o que deixa o `null` só na borda. O
erro de número negativo usa o código que já existia, `INVALID_REQUEST` (`ReservationService.java:34-36`). O
`catch (Exception)` de `HttpApi.java:113` vem da base, tem comentário e devolve um erro de domínio; não é do agente.
Os índices dos parâmetros do `INSERT` foram todos renumerados com cuidado (`ReservationStore.java:36-50`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados (a base tinha 26); 30 métodos escritos. Linhas
94,2% (base: 93,8%) e ramos 88,5% (base: 87,1%). O pacote `hotel.documents` tem 33 de 33 linhas e 6 de 6 ramos.
**Nota:** bom

Os 26 testes da base continuam lá e passando. Só mudaram os construtores, que ganharam o `0` das crianças
(`src/test/java/hotel/reservations/ReservationServiceTest.java:37`, `src/test/java/hotel/reports/ReportsTest.java:33`),
sem enfraquecer nenhuma asserção. Os testes novos conferem as regras com valores:

- a criança não muda o preço: 1 adulto e 1 criança dão R$ 749,70 (`ReservationServiceTest.java:173`); a criança fica
  gravada (`:172`); a linha sai exatamente entre "Quarto" e "Entrada" (`:176`);
- a capacidade: 2 adultos e 1 criança no quarto para dois dão `CAPACITY_EXCEEDED` (`:185`), e 1 adulto e 1 criança
  no mesmo quarto passam (`:165-172`, o limite exato); um número negativo dá `INVALID_REQUEST` (`:187`);
- sem criança, nenhuma linha "Crianças" (`:196`);
- pela API: o campo opcional dá 0, a consulta mostra `children`, e há o 422 e o 400
  (`src/test/java/hotel/web/HttpApiTest.java:124-140`).

A pergunta da mutação: trocar `>` por `>=` ou tirar `+ request.children()` na capacidade derruba `:185` ou
`:165-172`. Tirar o `if (children > 0)` derruba `:196`. Tirar a validação de negativo deixaria o `CHECK` do banco
estourar como erro 500, e `:187` falharia. Fica uma lacuna pequena: `:196` só confere a ausência da palavra, não que
o comprovante sem criança ficou igual ao de antes, mas os testes da base que leem o comprovante cobrem isso em parte.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok), nenhuma chave
estrangeira sem índice, nenhuma data em texto. Flyway com 2 migrações: `V1__create_tables.sql` igual à da base e
`V2__add_children.sql` nova. As colunas `status`, `rate_plan`, `type` e `channel` estão sem `CHECK` (vem da base).
**Nota:** bom

O esquema evolui do jeito certo: uma migração nova, sem editar a da base
(`src/main/resources/db/migration/V2__add_children.sql:2`). O `NOT NULL DEFAULT 0` preenche as reservas que já
existem com zero crianças, e o `CHECK (children >= 0)` protege a regra também no banco. O SQL novo usa parâmetros
(`ReservationStore.java:31-33,43`) e a leitura lê a coluna (`ReservationStore.java:132`). Os candidatos a "SQL com
`+`" que o script lista só juntam constantes (`COLUMNS`). A duplicação de reserva continua protegida pela restrição
`EXCLUDE` da base. Os defeitos menores (texto sem `CHECK` nas situações e tipos, a listagem que traz a tabela inteira
em `ReservationStore.java:112`) vieram da base, e o agente não os piorou.

## Comportamento

**Números:** 61 de 61 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 10 de 10 no
cenário 06 (7 sobre as crianças na reserva, 2 sobre o comprovante, 1 depois de reiniciar) e 2 de 2 nas reservas
simultâneas.
**Nota:** bom

Para o dono da pousada, isso quer dizer que tudo o que funcionava continua funcionando e que o pedido saiu como foi
escrito:

- a reserva sem o campo tem zero crianças;
- dois adultos e uma criança cabem no quarto para três e são recusados no quarto para dois (422);
- a criança não muda o preço, e um número negativo é recusado (400);
- a consulta mostra as crianças, também depois de reiniciar;
- o comprovante ganha "Crianças: 2" logo depois do quarto e fica igual quando não há criança.

## Dúvidas

Nenhuma.
