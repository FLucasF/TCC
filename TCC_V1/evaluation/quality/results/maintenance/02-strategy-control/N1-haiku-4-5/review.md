# Revisão: manutenção, cenário 02

## Visão geral

O sistema (vem da base, intacto) tem sete pacotes: `hotel` monta as peças e sobe o serviço (`src/main/java/hotel/Hotel.java:21-27`); `hotel.web` é a borda Javalin, que lê a requisição, chama o `ReservationService` e devolve JSON (`src/main/java/hotel/web/HttpApi.java:62-81`); `hotel.reservations` guarda as regras (preço, situações da reserva, cancelamento) e o acesso ao banco (`ReservationStore`); `hotel.rooms`, `hotel.messages`, `hotel.reports` e `hotel.documents` cuidam do resto. Os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway (`src/main/resources/db/migration/V1__create_tables.sql`). O pedido era pontual: o acréscimo da alta temporada passa de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 entra como alta temporada. O agente mudou só três arquivos, todos com o tamanho certo. Em `PriceCalculator` trocou a constante (`src/main/java/hotel/reservations/PriceCalculator.java:16`) e acrescentou `isEasterWeek` com as duas faixas de datas (`PriceCalculator.java:54-62`), chamado no fim de `isHighSeason` (`PriceCalculator.java:50`). Nos testes, atualizou os valores esperados e escreveu três testes novos. Não criou nenhuma estrutura nova. Os 29 testes passam, as 72 conferências do roteiro passam e o esquema não mudou. Não achei nenhum defeito sério. O único ponto em aberto fica fora do código: o `README.md:31-32` continua dizendo "20% a mais" e não cita a Páscoa. O agente atualizou o javadoc (`PriceCalculator.java:41`), mas não o README que o dono citou no pedido.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são iguais aos da base (`measures/dependencies-base.md`). A única diferença é o `hotel.reservations`, que foi de 508 para 522 linhas (o `PriceCalculator` foi de 53 para 67).
**Nota:** bom

O agente não mexeu na estrutura: a regra nova fica onde a regra de preço já ficava (`PriceCalculator.java:42-62`), sem Javalin e sem JDBC. A borda HTTP é fina (`HttpApi.java:62-81`, cada rota chama o serviço e serializa a resposta). O SQL está isolado nas classes de acesso a dados (`ReservationStore`, `Rooms`, `SentMessages`). A montagem acontece num lugar só (`Hotel.java:21-27`). O único ciclo já existia na base: `ReservationService` usa `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`), e `ReservationReceipt` usa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). Como o agente não o criou nem o piorou, ele vem da base.

## Design de baixo nível

**Números:** 1 constante alterada (`0.20` → `0.25`, `PriceCalculator.java:16`) e 1 método novo de 9 linhas com as duas faixas da Páscoa (`PriceCalculator.java:54-62`). Nenhuma classe, interface ou parâmetro novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido termina com a frase que tenta o agente ("a gente já pensou em ter um acréscimo diferente para cada temporada, mas por enquanto é o mesmo para todas"). O agente não caiu nela. Não há classe por temporada, interface de regra de temporada nem acréscimo parametrizado por temporada. Continua havendo um acréscimo só (`PriceCalculator.java:16`, usado em `PriceCalculator.java:36`), e a Páscoa entrou como duas condições de data a mais, chamadas no fim de `isHighSeason` (`PriceCalculator.java:50`). É exatamente a "contenção" que os critérios do cenário descrevem. As datas seguem o pedido: as noites do domingo de Ramos ao domingo de Páscoa, com os dois domingos (`!isBefore`/`!isAfter`, `PriceCalculator.java:57` e `:59`). O código cobre só 2027 e 2028, como o dono pediu.

Há um detalhe pequeno de estilo: o `if/else if` por ano com as datas escritas duas vezes em cada linha (`PriceCalculator.java:56-60`). Uma lista de períodos seria um pouco mais fácil de estender quando o dono mandar os próximos anos. Isso não chega a ser defeito com dois anos. O resto do código, que vem da base, usa tipos certos: `BigDecimal` com HALF_EVEN para dinheiro (`PriceCalculator.java:64-66`), `LocalDate` para datas, `enum` para planos e situações, `switch` exaustivo (`PriceCalculator.java:26-29`). Os erros têm código próprio (`ReservationService.java:133-137`), e não há `static` mutável.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26). Cobertura de linhas: 478/510 (93,7%; a base tinha 93,8%). Cobertura de ramos: 112/128 (87,5%; a base tinha 87,1%). No `hotel.reservations`, os ramos foram de 49/52 para 60/64.
**Nota:** bom

O agente atualizou os testes da base que dependiam dos 20%, com os valores novos certos, e não apagou nem enfraqueceu nenhum:
- a fronteira de 15/12 dá 562,28 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:36`);
- 29/02 de ano bissexto dá 562,28 (`PriceCalculatorTest.java:45`);
- a suíte não reembolsável em alta dá 2899,52, 434,93 e 2464,59 (`PriceCalculatorTest.java:97-99`);
- pela API, o total de 974,76 e o relatório financeiro (`src/test/java/hotel/web/HttpApiTest.java:77` e `:118`).

A parte nova está testada nas duas pontas de cada ano: 20/03, 21/03, 28/03 e 29/03 de 2027 (`PriceCalculatorTest.java:57-62`), e 08/04, 09/04, 16/04 e 17/04 de 2028 (`PriceCalculatorTest.java:65-70`). Há ainda um teste de preço com valor dentro da Páscoa (624,76, `PriceCalculatorTest.java:73-78`).

Pela pergunta da mutação, todas as mudanças ficam com rede:
- trocar `0.25` por outro valor quebra várias asserções;
- trocar `!isAfter` por `isBefore` quebra os testes de 28/03 e de 16/04;
- apagar a chamada a `isEasterWeek` quebra os dois testes de Páscoa.

Só o `return false` para os outros anos (`PriceCalculator.java:61`) não tem um teste explícito, por exemplo a Páscoa de 2029 em baixa temporada. Ele é coberto indiretamente pelos testes de baixa temporada da base.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)` e datas em `date`. A chave estrangeira tem índice, e há o `EXCLUDE USING gist`, que impede duas reservas do mesmo quarto na mesma noite. A única migração (`V1__create_tables.sql`) está igual à da base. As únicas acusações do script são as colunas de situação e de tipo em `text` sem CHECK (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`).
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: nenhuma migração editada ou acrescentada, nenhum SQL novo. As colunas de situação sem CHECK vêm da base, e o agente não as piorou. Os candidatos a "SQL montado com `+`" (`src/main/java/hotel/reservations/ReservationStore.java:33`, `:83`, `:98`, `:111`) juntam só texto constante (`COLUMNS`) com parâmetros `?`, e também vêm da base. O serviço subiu contra um banco vazio e criou o esquema.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o acréscimo novo: 5/5;
- a semana da Páscoa: 10/10;
- os outros períodos: 6/6;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:
- um standard numa noite de temporada sai R$ 312,38, como ele escreveu; o deluxe sai R$ 487,38 e a suíte, R$ 724,88;
- o desconto do não reembolsável incide sobre o valor novo (46,86, com total de 265,52);
- as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, e as noites vizinhas (20/03, 29/03, 08/04, 17/04) não;
- os outros períodos continuam como estavam (14/12 fora e 15/12 dentro, 28/02 dentro e 01/03 fora, 31/07 dentro e 01/08 fora).

Todo o resto continua funcionando: recusas, confirmação com e-mail, check-in e check-out, devoluções, relatórios, dados depois de reiniciar e duas reservas simultâneas.

## Dúvidas

O `README.md:31-32` continua dizendo que a noite de temporada custa 20% a mais e não cita a semana da Páscoa. O agente atualizou o comentário no código (`PriceCalculator.java:41`), mas não esse documento, que o dono disse explicar o sistema. Nenhuma coluna mede a documentação, então isso não entrou nas notas. Mesmo assim, alguém precisa corrigir o README para ele não contradizer o sistema.
