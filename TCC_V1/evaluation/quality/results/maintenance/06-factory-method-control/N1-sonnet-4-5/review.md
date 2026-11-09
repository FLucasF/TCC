# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin com sete pacotes: `hotel` (montagem e `main`), `hotel.web` (rotas e DTOs JSON),
`hotel.reservations` (regras em `ReservationService`, SQL em `ReservationStore`), `hotel.rooms`, `hotel.documents`
(o comprovante), `hotel.messages` e `hotel.reports`. Uma requisição `POST /reservations` passa pelo
`HttpApi.readNewReservation` (final/src/main/java/hotel/web/HttpApi.java:109), vira um `NewReservation`, é validada em
`ReservationService.create` (final/src/main/java/hotel/reservations/ReservationService.java:27) e é gravada por
`ReservationStore.add` no PostgreSQL, com o esquema criado por migrações do Flyway. O pedido era pontual: informar as
crianças no campo opcional `children`, somá-las aos adultos na conferência da capacidade, sem cobrar diária, e pôr a
linha "Crianças: N" no comprovante. O agente fez exatamente isso, e só isso: um campo a mais em cada lugar por onde
a reserva passa, a soma na capacidade, a linha no comprovante, uma migração nova (`V2__add_children.sql`) e 7 testes.
Não criou nenhuma estrutura para a "cobrança futura" das crianças. Os 33 testes passam e as 61 conferências do
roteiro também. Não encontrei nenhum defeito sério que tenha vindo do agente.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, iguais aos da base (`measures/dependencies.md` contra
`measures/dependencies-base.md`); 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base. Só
mudaram as linhas: `hotel.reservations` passou de 508 para 519, `hotel.web` de 236 para 240 e `hotel.documents` de 79
para 84.
**Nota:** bom

Cada parte da mudança ficou na camada a que pertence:
- **Borda HTTP:** a leitura do campo opcional, com zero quando ele não vem, está em
  `final/src/main/java/hotel/web/HttpApi.java:127`. A resposta ganhou o campo em
  `final/src/main/java/hotel/web/ReservationResponse.java:18` e `:38`.
- **Regras:** a regra está no serviço, que não conhece Javalin nem SQL. A recusa do número negativo fica em
  `final/src/main/java/hotel/reservations/ReservationService.java:34-36`, e a soma na capacidade em `:40`.
- **Dados:** o SQL continua isolado em `ReservationStore`, com o campo novo em
  `final/src/main/java/hotel/reservations/ReservationStore.java:19`, `:32-33`, `:43` e `:132`.
- **Comprovante:** a linha nova fica no próprio comprovante,
  `final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`.

O ciclo `documents` ↔ `reservations` vem da base: `ReservationService` cria o `ReservationReceipt`
(`ReservationService.java:67`), e o recibo lê a `Reservation`. O agente só usou essa dependência que já existia
(`ReservationReceipt.java:33`) e não acrescentou nenhuma.

## Design de baixo nível

**Números:** 12 arquivos tocados pelo patch: 8 de código, 3 de teste e o README, mais a migração nova. Nenhuma classe,
interface, fábrica ou *builder* novo; nenhum código de preço tocado (`PriceCalculator` igual ao da base).
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O que o pedido tinha de errado a evitar era a "cobrança futura" das crianças maiores. O agente não caiu nela:
- não há política de preço de criança, nem idade por criança, nem *builder* ou fábrica para a reserva;
- o `NewReservation` continua um `record` com um componente a mais
  (`final/src/main/java/hotel/reservations/NewReservation.java:6`);
- a `Reservation` continua sendo criada direto pelo construtor de sempre, que só copia o campo novo
  (`final/src/main/java/hotel/reservations/Reservation.java:14`, `:38`, `:70-72`).

Isso é o que `references/controls.md` chama de contenção para o cenário 06: um campo a mais na reserva e no pedido,
a soma na capacidade, a linha no comprovante e a coluna nova.

No resto, a mudança segue o estilo da base:
- o tipo é `int`;
- o padrão zero é aplicado num ponto só, a borda HTTP (`HttpApi.java:127`);
- a validação fica ao lado da validação dos adultos (`ReservationService.java:31-36`);
- a linha do comprovante segue o mesmo `if` que a base já usa para o desconto (`ReservationReceipt.java:47-49`, ao
  lado de `:54-56`).

Há só detalhes pequenos, que não pedem mudança:
- a mensagem de capacidade continua dizendo "guests" (`ReservationService.java:43`), agora que conta adultos e
  crianças;
- o campo `children` entrou no meio do `record` `ReservationReceipt` (`:17`), o que muda a ordem posicional de um
  tipo público. Ninguém mais usa o construtor canônico; os três usos passam por `ReservationReceipt(Reservation, Room)`
  (`ReservationService.java:67`, `:112`, `:121`).

O `catch (Exception)` em `HttpApi.java:113` vem da base.

## Testes

**Números:** 33 testes rodaram, 0 falhas, 0 erros e 0 pulados, com 33 métodos escritos. A base tinha 26, todos
mantidos; o agente acrescentou 7. A cobertura de linhas passou de 93,8% para 94,2% (483 de 513) e a de ramos de
87,1% para 88,5% (108 de 122); `hotel.documents` tem 6 de 6 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

A parte nova é conferida com valores, nos dois níveis:
- **Pelo serviço** (`final/src/test/java/hotel/reservations/ReservationServiceTest.java`):
  - a capacidade com 1 adulto e 2 crianças é recusada, e com 1 e 1 no quarto de dois é aceita (`:168-172`). Isso
    pega a troca de `>` por `>=` e a falta da soma;
  - o número negativo é recusado com `INVALID_REQUEST` (`:175-178`);
  - o comprovante tem "Crianças: 1" e `children() == 1` depois de reler do banco pela segunda via (`:181-189`);
  - sem criança, o comprovante não tem a linha (`:192-200`).
- **Pela API** (`final/src/test/java/hotel/web/HttpApiTest.java`):
  - sem o campo, `children` vem 0 (`:124-130`);
  - com 2 crianças, o campo volta 2 (`:133-144`);
  - 1 adulto e 2 crianças no quarto 101 dá 422 `CAPACITY_EXCEEDED` (`:147-157`).

Os testes da base só mudaram para passar o zero novo (`ReportsTest.java:33`, e o `stay` sobrecarregado em
`ReservationServiceTest.java:33-41`); nenhum foi enfraquecido.

Ficam duas lacunas pequenas:
- nenhum teste confere que a criança não muda o preço. Se alguém passasse a cobrar crianças, nenhum teste falharia.
  O agente, porém, não escreveu código para isso, e o roteiro do comportamento confere esse ponto;
- o `contains` em `ReservationServiceTest.java:188` não confere que a linha vem logo depois da do quarto.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. `V1__create_tables.sql` está igual à da base, e
`V2__add_children.sql` é nova. Nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave
estrangeira sem índice e nenhuma data em texto. As colunas de situação e de tipo sem CHECK (`status`, `rate_plan`,
`type`, `channel`) vêm da base (`measures/database.md`).
**Nota:** bom

A coluna nova entrou por uma migração versionada, sem editar a que já existia. Ela tem `NOT NULL DEFAULT 0` e
`CHECK (children >= 0)` (`final/src/main/resources/db/migration/V2__add_children.sql:2`), então as reservas que já
estão no banco ficam com zero crianças, e o banco também barra o número negativo. O SQL continua com parâmetros
(`?`); a renumeração dos índices do `INSERT` está certa (`ReservationStore.java:31-50`, 15 colunas e 15 parâmetros),
e a leitura traz a coluna (`ReservationStore.java:132`). Os concatenados com `+` que o script lista são constantes
(`COLUMNS`). A restrição `EXCLUDE` contra a reserva dupla vem da base e continua no lugar.

## Comportamento

**Números:** 61 de 61 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| crianças na reserva | 7 de 7 |
| comprovante | 2 de 2 |
| crianças depois de reiniciar | 1 de 1 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer:
- o site pode mandar `children`; sem o campo, a reserva vale zero crianças, e um número negativo é recusado com 400;
- dois adultos e uma criança cabem no quarto para três, mas não no quarto para dois (422);
- o preço não muda com crianças;
- a consulta mostra as crianças;
- o comprovante traz "Crianças: 2" logo depois da linha do quarto, e fica como antes quando não há criança;
- as crianças continuam gravadas depois de reiniciar o serviço;
- tudo o que já funcionava continua funcionando.

## Dúvidas

Nenhuma.
