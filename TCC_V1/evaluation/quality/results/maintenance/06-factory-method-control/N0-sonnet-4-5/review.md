# Revisão: manutenção, cenário 06

## Visão geral

O sistema da base tem sete pacotes: `hotel` (montagem e `main`), `hotel.web` (as rotas do Javalin), `hotel.reservations` (as regras, o preço e o acesso ao banco), `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages` e `hotel.reports`. Uma requisição entra em `HttpApi`, que monta um `NewReservation` e chama `ReservationService.create`. O serviço confere as regras e grava pelo `ReservationStore`, com JDBC e SQL parametrizado, num PostgreSQL cujo esquema vem de migrações do Flyway. O pedido era pontual: o campo `children` (opcional, zero por padrão, não pode ser negativo), a soma de adultos e crianças na conferência da capacidade e a linha "Crianças: N" no comprovante. O agente fez exatamente isso, no lugar onde cada coisa já morava. Acrescentou um campo em `NewReservation`, `Reservation`, `CreateReservationRequest`, `ReservationResponse` e `ReservationReceipt`, a soma em `ReservationService.java:40-41`, uma migração nova `V2__add_children.sql` e oito testes. Não criou nenhuma estrutura para a cobrança futura de crianças. Nada sério a apontar: as 61 conferências do roteiro passam e os 34 testes passam. A única falha menor é que o README não foi atualizado com o campo novo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` contra `measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. Só as linhas mudaram: `hotel.reservations` passou de 508 para 520, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

A mudança respeitou as camadas que já existiam:
- **Borda HTTP:** só lê o campo e aplica o valor padrão (`final/src/main/java/hotel/web/HttpApi.java:128`).
- **Regras:** a validação e a capacidade ficam no serviço, que não conhece Javalin nem SQL (`final/src/main/java/hotel/reservations/ReservationService.java:34-45`).
- **Banco:** o SQL fica só no `ReservationStore` (`final/src/main/java/hotel/reservations/ReservationStore.java:18-19,31-50,132`).

Nenhuma dependência nova entre pacotes e nenhum ciclo novo. O ciclo `documents` ↔ `reservations` existe porque `ReservationReceipt.from` recebe uma `Reservation` e `ReservationService.receiptCopy` devolve um `ReservationReceipt`. Ele vem da base e o agente não o piorou. A maior classe continua sendo `HttpApi`, com 171 linhas (eram 170).

## Design de baixo nível

**Números:** 11 arquivos de produção tocados, todos com mudança de uma a seis linhas, mais uma migração de uma linha. Nenhuma classe, interface, fábrica ou *builder* novo. Nada de política de preço por criança.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tenta o agente com a frase "cobrar alguma coisa das crianças maiores no futuro". O agente não caiu nessa tentação:
- **Preço:** `PriceCalculator` continua sem nenhuma menção a crianças, e o cálculo segue `priceCalculator.calculate(room, checkIn, checkOut, ratePlan)` (`final/src/main/java/hotel/reservations/ReservationService.java:50`).
- **Reserva:** continua criada direto pelo construtor (`final/src/main/java/hotel/reservations/NewReservation.java:5-6`, um componente a mais no record).
- **Capacidade:** a soma é uma variável local, `totalOccupants` (`ReservationService.java:40`).
- **Comprovante:** a linha nova é um `if (children > 0)` logo depois da linha do quarto (`final/src/main/java/hotel/documents/ReservationReceipt.java:46-49`), no mesmo estilo da linha de desconto que já existia (`ReservationReceipt.java:54-56`).

É a mudança pequena que `references/controls.md` descreve para o cenário 06.

O resto do design continua limpo:
- **Tipos:** `children` é `int` no domínio e `Integer` só no JSON de entrada, para distinguir o campo ausente (`final/src/main/java/hotel/web/CreateReservationRequest.java:15`, `HttpApi.java:128`).
- **Validação:** a recusa do número negativo usa o mesmo `HotelException(INVALID_REQUEST)` das outras validações (`ReservationService.java:34-36`).
- **Defeitos menores:**
  - A mensagem de capacidade excedida ainda diz "takes at most N guests" (`ReservationService.java:44`), quando agora `guests` são só os adultos. É uma imprecisão de texto, não de regra.
  - O agente renumerou à mão os índices do `PreparedStatement` (`ReservationStore.java:42-50`). Está certo, mas é frágil; esse jeito de fazer já vinha da base.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados, com 34 métodos escritos. A base tinha 26, todos mantidos e passando. Cobertura de linhas: 94,2% (484 de 514; a base tinha 93,8%). Cobertura de ramos: 88,5% (108 de 122; a base tinha 87,1%). `hotel.documents` tem 6 de 6 ramos cobertos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente acrescentou oito testes que conferem a parte nova com valores, em dois níveis.

No serviço, contra o banco:
- o número negativo é recusado (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:164`);
- um adulto e duas crianças no quarto 101, para dois, dão `CAPACITY_EXCEEDED` (`:173`);
- dois adultos e uma criança no 201, para três, são aceitos, com o total de 779.80, igual ao de sem criança (`:183`);
- "Crianças: 2" aparece no comprovante (`:198-209`) e não aparece sem criança (`:213-223`).

Pela API:
- sem o campo, `children` volta 0 (`final/src/test/java/hotel/web/HttpApiTest.java:124`);
- o campo informado volta no JSON (`:134`);
- 2+2 no quarto 201 dá 422 `CAPACITY_EXCEEDED` (`:150`).

A pergunta da mutação:
- **Tirar `children` da soma** derruba os dois testes de capacidade.
- **Trocar `>` por `>=`** derruba `childrenAreFreeButCountTowardsCapacity`, que ocupa o quarto exatamente até a capacidade.
- **Cobrar a criança no preço** derruba o mesmo teste, pelo total de 779.80.
- **Ler `children` errado do banco** é pego pelo teste do comprovante, que relê a reserva pelo `find`.

Ficam sem rede dois pontos:
- a posição da linha "Crianças" logo depois da linha do quarto, porque o teste só usa `contains` (`ReservationServiceTest.java:209`);
- a recusa do negativo pela API (400), testada só no serviço.

São lacunas pequenas, e o roteiro do comportamento confere as duas.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. A `V1__create_tables.sql` está igual à da base e a `V2__add_children.sql` é nova. As conferências não acusaram tabela sem chave, dinheiro fora de `numeric(10,2)`, chave estrangeira sem índice nem data como texto. Há um `CHECK (children >= 0)` novo. As colunas `status`, `rate_plan`, `type` e `channel` estão sem CHECK, o que já vinha da base (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo. A coluna entra por uma migração nova, sem editar a V1 (`final/src/main/resources/db/migration/V2__add_children.sql:2`), e o `ALTER TABLE ... ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)` funciona num banco que já tem reservas: as antigas ficam com zero crianças. O banco protege também a regra do "não pode ser negativo". O SQL novo continua parametrizado (`final/src/main/java/hotel/reservations/ReservationStore.java:31-50`), e a leitura do campo está no mapeamento único da linha (`ReservationStore.java:132`). A proteção contra duas reservas na mesma noite (o `EXCLUDE USING gist` e o `synchronized` no `create`) vem da base, e o agente não mexeu nela. As colunas de situação sem CHECK também vêm da base.

## Comportamento

**Números:** 61 de 61 conferências passaram, 0 falharam (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- crianças na reserva: 7 de 7;
- comprovante: 2 de 2;
- crianças depois de reiniciar: 1 de 1;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- sem o campo, a reserva tem zero crianças;
- dois adultos e uma criança cabem num quarto para três, e a criança não muda o preço;
- a consulta mostra as crianças;
- adultos e crianças acima da capacidade dão 422, e o negativo dá 400;
- a linha "Crianças: 2" aparece logo depois da linha do quarto, e sem criança o comprovante fica igual ao de antes;
- o número de crianças continua lá depois de reiniciar.

Nada do que já funcionava quebrou. Para o dono da pousada, a mudança está pronta para uso. A única pendência é o README, que não mostra o campo `children` no exemplo do corpo de `POST /reservations` (`final/README.md:61-72`). O pessoal do site vai precisar saber do campo por outra via.

## Dúvidas

nenhuma
