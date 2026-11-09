# Revisão: manutenção, cenário 06

## Visão geral

O sistema que veio da base está dividido em sete pacotes: `hotel` (a montagem), `hotel.web` (Javalin), `hotel.reservations` (regras e acesso ao banco), `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages` e `hotel.reports`. Uma requisição entra por `HttpApi`, que monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:109-131`). A regra fica em `ReservationService.create` (`ReservationService.java:27-51`). Os dados ficam no PostgreSQL, por meio de `ReservationStore` (JDBC com `?`), e o esquema é criado por migrações do Flyway. O pedido era pontual: o campo `children`, opcional e valendo 0 quando falta, a soma de adultos e crianças na conferência da capacidade, a recusa de um número negativo, a linha "Crianças: N" no comprovante e nenhuma mudança no preço. O agente fez exatamente isso. Mexeu em 8 classes de produção com poucas linhas cada, criou uma migração nova (`V2__add_children.sql`) e escreveu 3 testes novos. Não criou nenhuma estrutura para a "cobrança futura" das crianças. O roteiro passou inteiro (61 de 61). O que achei foram dois detalhes pequenos: o README não fala do campo novo, e um caso do comprovante ficou sem teste.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os pacotes, as dependências e o ciclo são os mesmos da base (`measures/dependencies-base.md`). Só mudou a contagem de linhas: `hotel.reservations` foi de 508 para 522, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

A mudança respeitou as camadas que já existiam:
- **Borda HTTP:** a borda só lê o campo e põe 0 quando ele falta (`HttpApi.java:129-130`). A resposta ganhou um campo (`ReservationResponse.java:18,38`).
- **Regras:** as duas regras novas, a do número negativo e a da capacidade, ficaram no serviço, sem Javalin e sem SQL (`ReservationService.java:34-44`).
- **SQL:** ficou só em `ReservationStore.java:18-19,31-50,132`.
- **Comprovante:** a linha nova está no próprio comprovante (`ReservationReceipt.java:47-49`).

O ciclo `documents` ↔ `reservations` vem da base: `ReservationReceipt` recebe um `Reservation`, e o serviço cria o comprovante. O agente não o piorou. A maior classe continua sendo `HttpApi` (171 linhas), com uma linha a mais.

## Design de baixo nível

**Números:** 8 classes de produção tocadas com mudanças de 1 a 9 linhas, 1 migração nova e nenhuma classe, interface, fábrica ou *builder* novo. Nada do patch trata de preço de criança.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido termina com a frase que tenta o agente: "cobrar alguma coisa das crianças maiores no futuro". O agente não caiu nela. Fez a mudança pequena que `controls.md` descreve, no lugar onde cada coisa já mora:
- um campo a mais no pedido e na reserva (`NewReservation.java:6`, `Reservation.java:14,38,72-74`);
- a soma na conferência da capacidade (`ReservationService.java:40`);
- a recusa do número negativo, junto da recusa que já existia para "nenhum hóspede" (`ReservationService.java:34-36`);
- a linha condicional no comprovante (`ReservationReceipt.java:47-49`);
- a coluna nova por migração.

A reserva continua sendo criada direto (`ReservationStore.java:53`). O `PriceCalculator` não foi tocado, e não há política de preço de criança. O valor padrão 0 fica num ternário na borda (`HttpApi.java:130`), que é o lugar certo para resolver um campo opcional do JSON.

No resto do código não há defeito novo. Os tipos estão certos (`int` para contagens; o dinheiro continua `BigDecimal`). Os comentários de `Reservation.guests()` e `children()` deixam claro que `guests` agora conta só os adultos (`Reservation.java:66,71`).

Há um ponto menor. `NewReservation` passou a ter dois `int` vizinhos (`guests, children`, `NewReservation.java:6`), que é fácil trocar numa chamada posicional. Num *record* deste tamanho, isso é aceitável.

Dois pontos que um revisor notaria vêm da base, não do agente: o `catch (Exception unreadableBody)` (`HttpApi.java:113`) e o `switch` do plano no cancelamento (`ReservationService.java:85-88`).

## Testes

**Números:** em `final`, rodaram 29 testes (0 falhas, 0 erros, 0 pulados), de 29 métodos escritos. A cobertura foi de 94,2% das linhas (483/513) e 88,5% dos ramos (108/122). Na base eram 26 testes, com 93,8% das linhas e 87,1% dos ramos. `hotel.documents` tem 6 de 6 ramos cobertos.
**Nota:** bom

Os 26 testes da base continuam passando. As únicas mudanças neles foram mecânicas: o `0` de crianças nos ajudantes `stay`/`book` (`ReservationServiceTest.java:36`, `ReportsTest.java:33`). Nenhum teste foi apagado ou enfraquecido.

Os 3 testes novos conferem as regras com valores:
- `childrenTakeAPlaceInTheRoomButDoNotChangeThePrice` (`ReservationServiceTest.java:64-76`): 1 adulto e 1 criança no quarto 101, que comporta 2. Confere que o total é R$ 749,70 (3 × 249,90, sem cobrar a criança), que a reserva lida do banco tem 1 criança e que a linha `"Quarto: 101 (STANDARD)\nCrianças: 1\n"` aparece no e-mail, inclusive na posição certa.
- `childrenCountForCapacityAndCannotBeNegative` (`ReservationServiceTest.java:78-88`): 2 adultos e 1 criança no 101 dão `CAPACITY_EXCEEDED`; −1 criança dá `INVALID_REQUEST`.
- `childrenAreOptionalAndShownOnTheReservation` (`HttpApiTest.java:91-104`): sem o campo, vem `children: 0`. Com 1 criança, a consulta mostra 1. Com 2 adultos e 2 crianças no 201 (que comporta 3), vem 422; com −1, vem 400.

Fiz a pergunta da mutação em três regras:
- Trocar `>` por `>=` na capacidade (`ReservationService.java:40`): o teste de 1+1 no quarto para dois falharia. Há rede.
- Apagar `+ request.children()`: o 2+1 deixaria de dar 422. Há rede.
- Trocar `children > 0` por `>= 0` no comprovante (`ReservationReceipt.java:47`): nenhum teste falharia. O teste que já existia, `confirmingSendsTheReceiptByEmail` (`ReservationServiceTest.java:110-122`), só confere trechos com `contains` e não percebe uma linha "Crianças: 0" a mais. A regra "sem criança, o comprovante fica como está" ficou sem rede. É uma lacuna pequena; o roteiro do comportamento confirma que hoje o comprovante sem criança está certo.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está guardada como texto. As migrações: `V1__create_tables.sql` está igual à da base e `V2__add_children.sql` é nova. A única conferência que acusou algo foram as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`), e isso vem da base.
**Nota:** bom

A coluna nova entrou do jeito certo, numa migração nova, sem editar a V1: `ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)` (`src/main/resources/db/migration/V2__add_children.sql:2`). O `DEFAULT 0` dá zero crianças às reservas que já existem num banco em uso, e o `CHECK` repete no banco a regra do número negativo.

O SQL continua com parâmetros. A coluna entra na lista `COLUMNS`, que é uma constante (`ReservationStore.java:18-19`), no `INSERT` com 15 `?` e índices renumerados corretamente (`ReservationStore.java:31-50`) e na leitura (`ReservationStore.java:132`). A proteção contra reserva dupla vem da base e continua lá: o `EXCLUDE USING gist` sobre o quarto e as noites.

## Comportamento

**Números:** 61 de 61 conferências passaram, nenhuma falhou:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- crianças na reserva: 7 de 7;
- comprovante: 2 de 2;
- crianças depois de reiniciar: 1 de 1;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- sem o campo `children`, a reserva tem zero crianças;
- dois adultos e uma criança cabem num quarto para três, e o preço não muda com a criança;
- a consulta mostra as crianças;
- adultos e crianças acima da capacidade são recusados com 422, e um número negativo com 400;
- o comprovante ganha "Crianças: 2" logo depois da linha do quarto e não muda quando não há criança;
- as crianças continuam gravadas depois de reiniciar.

Tudo o que já funcionava continua funcionando, inclusive o caso das duas reservas ao mesmo tempo.

## Dúvidas

O README não foi atualizado: o exemplo do corpo de `POST /reservations` (`final/README.md:61-72`) não mostra `children` e não diz que `guests` agora conta só os adultos. Para o pessoal do site, a informação está só no código e nos comentários (`CreateReservationRequest.java:6`). Achei que isso não basta para tirar a nota de nenhuma coluna, mas é um ponto que alguém precisa ajustar.
