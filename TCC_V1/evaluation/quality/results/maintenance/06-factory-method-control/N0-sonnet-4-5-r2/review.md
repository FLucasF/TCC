# Revisão: manutenção, cenário 06

## Visão geral

O sistema da base tem sete pacotes: `hotel` (a montagem e o banco), `hotel.web` (o Javalin), `hotel.reservations` (as regras e o acesso ao banco das reservas), `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages` e `hotel.reports`. Uma requisição entra em `HttpApi`, que lê o corpo e o transforma num `NewReservation` (`src/main/java/hotel/web/HttpApi.java:109-131`). Depois passa por `ReservationService.create`, que confere as regras (`src/main/java/hotel/reservations/ReservationService.java:27-52`), e chega a `ReservationStore`, que fala com o PostgreSQL por JDBC com parâmetros. O esquema vem de migrações do Flyway. O pedido era acrescentar as crianças na reserva, e o agente fez a mudança pequena, onde cada coisa já ficava:

- um campo `children` em `CreateReservationRequest`, `NewReservation`, `Reservation` e `ReservationResponse`;
- zero quando o campo não vem (`HttpApi.java:127`);
- a recusa de número negativo e a soma de adultos e crianças na conferência da capacidade (`ReservationService.java:34-45`);
- a linha "Crianças: N" no comprovante (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`);
- uma migração nova, `V2__add_children_column.sql`, sem mexer na V1.

Não criou nenhuma estrutura de preço para crianças. Os 35 testes passam, e as 61 conferências do roteiro também. Não achei nada sério. O único ponto pequeno: o README não foi atualizado e não cita o campo `children`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Pacotes, dependências e ciclo são idênticos na base (`measures/dependencies-base.md`). Só cresceram as linhas: `hotel.reservations` passou de 508 para 520, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

O agente não criou pacote nem dependência nova. Cada parte da mudança ficou na sua camada:

- A borda HTTP continua só traduzindo o pedido. O zero quando o campo falta fica em `HttpApi.java:127`, e o campo de entrada em `src/main/java/hotel/web/CreateReservationRequest.java:15`.
- A regra fica no serviço, que não conhece Javalin nem JDBC (`ReservationService.java:34-45`).
- O SQL ficou em `ReservationStore`: a lista de colunas em `src/main/java/hotel/reservations/ReservationStore.java:19`, o INSERT nas linhas 31-43 e a leitura da linha na 132.

O ciclo `documents` ↔ `reservations` já existia na base, porque o serviço cria o comprovante e o comprovante lê a reserva. O agente não o piorou: ele só passou mais um dado da reserva (`ReservationReceipt.java:33`).

## Design de baixo nível

**Números:** nenhuma classe, interface, fábrica ou *builder* novo. Foram 11 arquivos de produção tocados (um deles a migração nova), todos com campo, parâmetro ou linha a mais. Nenhum tipo de "preço de criança" foi criado: o `PriceCalculator` ficou igual à base.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tentava o agente com "cobrar alguma coisa das crianças maiores no futuro", e ele não caiu na tentação. O preço continua calculado só por quarto, datas e plano (`ReservationService.java:50`). A reserva continua criada direto, por construtor, a partir do `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:6`, `src/main/java/hotel/reservations/Reservation.java:38`). É exatamente a contenção que `controls.md` descreve para este cenário: um campo a mais, a soma na capacidade, a linha no comprovante e a coluna nova.

O resto do código está limpo:

- A soma tem um nome que diz o que é (`totalOccupants`, `ReservationService.java:40`).
- A linha do comprovante fica condicionada a `children > 0`, no ponto pedido, logo depois do quarto (`ReservationReceipt.java:46-49`).
- `Integer` anulável só na borda e `int` daí para dentro (`CreateReservationRequest.java:15` contra `NewReservation.java:6`).

Ressalvas pequenas, nenhuma que impeça aceitar:

- `NewReservation` agora recebe dois `int` vizinhos (`guests, children`, `NewReservation.java:6`), fáceis de trocar sem o compilador perceber. Mas o record posicional já vinha da base.
- A mensagem de capacidade excedida ainda diz "guests" (`ReservationService.java:44`), embora agora conte também as crianças.

## Testes

**Números:** 35 testes rodaram, 0 falharam, 0 pulados, e há 35 métodos escritos. A base tinha 26, todos mantidos. Cobertura de linhas: 94,2% (484 de 514), contra 93,8% na base. Cobertura de ramos: 88,5% (108 de 122), contra 87,1% na base. `hotel.documents` tem 33 de 33 linhas e 6 de 6 ramos.
**Nota:** bom

O agente escreveu 9 testes novos, nos dois níveis:

- **No serviço, contra o banco** (`src/test/java/hotel/reservations/ReservationServiceTest.java`):
  - a criança não muda o preço, com o valor (`499.80`, linha 173);
  - 1 adulto + 1 criança cabe no quarto para dois (linhas 164-172), o que pega a troca de `>` por `>=`;
  - 1 adulto + 2 crianças é recusado com `CAPACITY_EXCEEDED` (linha 183), o que pega o esquecimento da soma;
  - número negativo é recusado com `INVALID_REQUEST` (linha 192);
  - a linha "Crianças: 1" aparece no comprovante (linha 205) e some quando não há criança (linha 215).
- **Pela API** (`src/test/java/hotel/web/HttpApiTest.java`):
  - sem o campo, `children` vale 0 (linhas 124-130);
  - o campo volta na criação e numa consulta, que lê do banco (linhas 132-148);
  - capacidade com 422 (linhas 150-160) e negativo com 400 (linhas 162-172).

Nenhum teste da base foi apagado ou enfraquecido: os dois *helpers* só ganharam o `0` de crianças (`ReservationServiceTest.java:36`, `src/test/java/hotel/reports/ReportsTest.java:33`).

Na pergunta da mutação, só uma coisa fica sem rede: a posição da linha "Crianças" no comprovante. O teste só confere `contains` (`ReservationServiceTest.java:205`). Se a linha fosse para o fim do comprovante, nenhum teste do agente falharia, mas o roteiro do comportamento confere essa posição. Detalhe menor: `assertTrue(!...)` em vez de `assertFalse` (linha 215).

## Banco

**Números:** 3 tabelas, 2 migrações do Flyway. A V1 está igual à da base, e a V2 é nova. Nenhuma conferência acusou problema: há chave primária em todas as tabelas, o dinheiro está em `numeric(10,2)`, a chave estrangeira tem índice e as datas estão em `date`. A coluna nova é `children integer NOT NULL DEFAULT 0` com `CHECK (children >= 0)`. A exclusão `reservations_no_double_booking` da base continua lá.
**Nota:** bom

A mudança no banco é a certa para quem já roda o sistema. A migração nova (`src/main/resources/db/migration/V2__add_children_column.sql:2`) acrescenta a coluna com `DEFAULT 0`, então as reservas antigas ficam com zero crianças sem quebrar nada. A regra de "não negativo" também fica protegida no banco, não só no Java. O SQL novo continua com parâmetros (`ReservationStore.java:31-43`): os `+` que o script lista juntam só constantes (`COLUMNS`, linha 19).

O que a medida ainda aponta veio da base: `status` e `rate_plan` em `text` sem `CHECK`, nenhuma transação explícita e a listagem de todas as reservas (`ReservationStore.java:112`).

## Comportamento

**Números:** 61 de 61 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| as crianças na reserva | 7 de 7 |
| o comprovante | 2 de 2 |
| as crianças depois de reiniciar | 1 de 1 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, quer dizer que tudo o que já funcionava continua igual:

- o preço, as recusas, a confirmação com e-mail, check-in e check-out, os cancelamentos e os relatórios;
- os dados depois de reiniciar o serviço;
- a proteção contra reserva duplicada.

E o pedido novo funciona como ele descreveu:

- sem o campo, são zero crianças;
- criança não muda o preço;
- adultos e crianças juntos não passam da capacidade (422);
- número negativo é recusado (400);
- a consulta mostra as crianças, também depois de reiniciar;
- o comprovante traz "Crianças: 2" logo depois da linha do quarto e fica como era quando não há criança.

## Dúvidas

O texto não diz como recusar quando o site manda `children: null` explicitamente. O código trata como zero (`HttpApi.java:127`), o que parece sensato, mas o roteiro não testa esse caso. O README da base não foi atualizado com o campo `children`; isso não pesa em nenhuma coluna.
