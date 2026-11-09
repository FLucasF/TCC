# Revisão: manutenção, cenário 02

## Visão geral

O sistema (da base) tem sete pacotes: `hotel` (montagem e `Database`), `hotel.web` (Javalin), `hotel.reservations`
(regras, cálculo de preço e acesso a dados em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.reports` e
`hotel.documents`. Uma requisição entra pela rota em `hotel/web/HttpApi.java:64-67`, que lê o corpo e chama
`ReservationService.create` (`hotel/reservations/ReservationService.java:27`). O serviço valida, calcula o preço com
`PriceCalculator` e grava pelo `ReservationStore` no PostgreSQL. O esquema vem de uma migração do Flyway, e o preço
fica guardado na reserva. O pedido era pontual: o acréscimo de 20% passa a 25%, e a semana da Páscoa de 2027 e 2028
entra na alta temporada. O agente fez só isso. Mudou um arquivo de produção, `PriceCalculator.java`: a constante
(`:16`) e duas condições por ano em `isHighSeason` (`:53-61`). Também acertou os valores esperados de dois arquivos de
teste e acrescentou dois testes. Não criou nenhuma estrutura para "um acréscimo diferente para cada temporada". Os 28
testes passam, e as 72 conferências do roteiro também. Há um defeito menor: o `README.md:31-32` continua dizendo "20% a
mais" e não fala da Páscoa. O dono disse que o README explica o sistema, e ele ficou desatualizado.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são
iguais aos da base (`measures/dependencies-base.md`). A única diferença é o tamanho de `hotel.reservations`, que passou
de 508 para 523 linhas, porque `PriceCalculator` foi de 53 para 68.
**Nota:** bom

A mudança ficou toda dentro de `hotel/reservations/PriceCalculator.java`, onde a regra de temporada já morava. Ela não
criou dependência nova nem pacote novo. O resto da arquitetura vem da base e está correto:

- As rotas são finas: leem a requisição, chamam o serviço e escrevem a resposta (`hotel/web/HttpApi.java:61-91`).
- As regras não conhecem o Javalin (pela tabela de bibliotecas, só `hotel` e `hotel.web` usam Javalin).
- O SQL fica nas classes de acesso a dados (`ReservationStore`, `Rooms`, `SentMessages`).
- O ciclo `documents` ↔ `reservations` já existia na base: `ReservationService.java:3` importa `ReservationReceipt`, e
  `ReservationReceipt.java:3` importa `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 1 constante mudada (`PriceCalculator.java:16`, de 0,20 para 0,25). Duas faixas de datas da Páscoa entraram
como condições por ano em `isHighSeason` (`PriceCalculator.java:53-61`). Nenhuma classe, interface ou parâmetro novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tenta o agente com "um acréscimo diferente para cada temporada", mas diz que "por enquanto é o mesmo para
todas". O agente não caiu: não há regra de temporada abstrata, nem classe por temporada, nem acréscimo parametrizado.
A taxa continua numa constante só, aplicada em `nightPrice` (`PriceCalculator.java:33-39`). As datas da Páscoa entraram
como dado, no lugar onde as outras temporadas já estavam (`PriceCalculator.java:45-63`). É exatamente a resposta que os
critérios de `controls.md` descrevem para este cenário ("duas condições a mais no `isHighSeason`").

A forma é aceitável, com dois detalhes pequenos:

- O `if (date.getYear() == 2027) return ...` (`:54-57`) mistura o ano com o intervalo. Uma lista de períodos facilitaria
  o próximo ano que o dono vai mandar.
- As datas estão escritas no corpo do método, e não numa constante nomeada.

O comentário do método foi atualizado (`:41-44`).

O resto do código vem da base e está correto:

- `BigDecimal` com `HALF_EVEN` em centavos (`PriceCalculator.java:65-67`) e `LocalDate` para as datas.
- `enum` para o plano, a situação e os códigos de erro.
- Erros com código próprio (`HotelException`), mapeados num lugar só (`HttpApi.java:100-107`).

## Testes

**Números:** 28 testes rodaram, 0 falharam; 28 métodos escritos (a base tinha 26, com 26 passando). Cobertura de linhas
de 93,7% (478/510) e de ramos de 87,5% (112/128). Na base eram 93,8% e 87,1%. Em `hotel.reservations`, os ramos cobertos
passaram de 49/52 para 60/64.
**Nota:** bom

O agente atualizou os valores esperados dos testes da base para a taxa nova, com valores conferidos:

- 562,28 em `PriceCalculatorTest.java:36` e `:45`;
- 2899,52, 434,93 e 2464,59 na suíte não reembolsável (`:104-106`);
- 974,76 pela API (`HttpApiTest.java:77` e `:118`).

Nenhum teste foi apagado ou enfraquecido: as asserções continuam com valores exatos.

Os dois testes novos conferem as duas pontas de cada semana da Páscoa: o dia anterior, o domingo de Ramos, um dia do
meio, o domingo de Páscoa e o dia seguinte (`PriceCalculatorTest.java:57-69` e `:73-85`). Cada um também confere o total
de uma estadia que atravessa a borda (874,66).

A pergunta da mutação dá boa rede:

- Voltar a taxa para 0,20 derruba `:36`, `:45`, `:104` e o teste da API.
- Trocar `isBefore` por `isAfter`, ou deslocar uma borda de um dia, derruba as asserções de `:58-62` e `:74-78`.

Faltam só duas coisas pequenas: nenhum teste confere que a Páscoa de outro ano (2026 ou 2029) continua em baixa
temporada, e não há teste da Páscoa pela API.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, e a chave
estrangeira tem índice. Uma restrição `EXCLUDE` impede a reserva dupla. A única migração, `V1__create_tables.sql`, está
igual à da base. O script acusou só colunas de situação e de tipo em `text` sem `CHECK`.
**Nota:** bom

O agente não mexeu no banco, e o pedido não pedia isso. Como o preço é calculado na criação e gravado na reserva
(`V1__create_tables.sql:30-32`), as reservas antigas mantêm o valor de quando foram feitas. A taxa nova vale só para as
reservas novas, que é o sensato.

Os pontos que o script acusou vêm da base:

- `status` e `rate_plan` sem `CHECK` (`V1__create_tables.sql:28` e `:33`);
- os relatórios carregam a tabela inteira de reservas para filtrar em Java (`ReservationStore.java:108-111`, usado em
  `OccupancyReport.java:25` e `RevenueReport.java:25`).

O SQL usa parâmetros. Os trechos concatenados só juntam a constante `COLUMNS`.

## Comportamento

**Números:** 72 de 72 conferências passaram:

- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- acréscimo novo: 5/5;
- semana da Páscoa: 10/10;
- os outros períodos continuam: 6/6;
- reservas simultâneas: 2/2.

**Nota:** bom

Pela API, o sistema faz o que o dono pediu:

- O standard em alta temporada sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88.
- O não reembolsável calcula o desconto sobre o valor novo.
- As noites de 21 a 28/03/2027 e de 09 a 16/04/2028 cobram o acréscimo, e as noites vizinhas não.
- Dezembro a partir do dia 15, fevereiro e julho continuam como antes.

Nada falhou.

## Dúvidas

- O `README.md:31-32` ainda descreve a alta temporada com 20% e sem a Páscoa. É documentação, e não código, por isso
  não entrou em nenhuma nota. Mas um revisor pediria para atualizar antes de aceitar: o dono disse que é ali que se
  explica o sistema.
