# Revisão: manutenção, cenário 10

## Visão geral

A base é um serviço Javalin em 7 pacotes. A borda HTTP fica em `hotel.web`. As regras ficam em `hotel.reservations`: serviço, cálculo de preço e reserva. O acesso ao banco está em classes `*Store`/`Rooms`/`SentMessages`, com JDBC e pool, e o esquema é criado pelo Flyway (`V1__create_tables.sql`). Os relatórios CSV ficam em `hotel.reports`, e tudo é montado em `hotel.Hotel`. Uma requisição a `GET /reports/occupancy` entra em `web/HttpApi.java:83-86`. De lá, chama `OccupancyReport.generate`, que lê `ReservationStore.all()` e monta o CSV. O pedido era pontual: a coluna `guest` depois de `reservation`, com a vírgula do nome trocada por espaço. O agente mudou só duas linhas de produção em `src/main/java/hotel/reports/OccupancyReport.java` (`:34` e `:40`), ajustou os dois testes do relatório e acrescentou um teste para a vírgula. Não criou nenhuma estrutura para o "telefone, talvez um dia". Todos os scripts mediram, e não há nada sério a apontar na mudança. Os defeitos menores que existem (um ciclo de pacotes, colunas de situação sem CHECK, o relatório que lê a tabela inteira) já estavam na base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies.md` contra `measures/dependencies-base.md`). A única diferença é `hotel.reports`, que passou de 95 para 96 linhas.
**Nota:** bom

A mudança fica no lugar onde o relatório já mora (`OccupancyReport.java:34`, `:40`). Ela não cria dependência nova: o `Guest` vem de `reservation.guest()`, e `Reservation` já era usado pelo pacote. A borda HTTP continua fina: `HttpApi.java:83-86` só lê a data e devolve o CSV. As regras não usam Javalin, o JDBC fica nas classes de dados (`ReservationStore.java`) e a montagem continua num lugar só (`Hotel.java:27`). O ciclo vem da base: `ReservationService.java:3` importa `hotel.documents.ReservationReceipt`, que por sua vez usa `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, com 2 linhas: o cabeçalho e um `append` com `replace(',', ' ')`. Nenhuma classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Pelos critérios do controle 10 em `references/controls.md`, a contenção esperada era "uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço". O agente fez exatamente isso: o cabeçalho em `OccupancyReport.java:34` e o valor em `OccupancyReport.java:40`, na posição pedida, depois de `reservation.id()` (`:39`). Não há esqueleto genérico de relatórios, colunas configuráveis nem um campo de telefone antecipado. A `RevenueReport` não foi tocada.

O resto do código vem da base e é limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:52`);
- `LocalDate`/`YearMonth` para as datas e `enum` para situação e plano;
- estado mutável só por métodos de pacote em `Reservation.java:123-135`;
- erros com código próprio (`HotelException`/`ErrorCode`), mapeados num lugar só (`HttpApi.java:100-107`).

Há um `catch (Exception)` em `HttpApi.java:113`, que vem da base e está comentado e justificado.

A troca é literal: `"Lima, Bruno"` vira `"Lima  Bruno"`, com dois espaços. É o que o texto pede ("troca por um espaço"). Aspas e quebras de linha no nome não são tratadas, mas o texto também não pedia isso.

## Testes

**Números:** 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 27 métodos escritos (a base tinha 26, todos mantidos). A cobertura de linhas é 93,8% (472/503) e a de ramos 87,1% (101/116). `hotel.reports` está com 49/49 linhas e 22/22 ramos (`measures/tests.md`).
**Nota:** bom

Os dois testes da base sobre a ocupação foram atualizados com o CSV inteiro, comparado por igualdade, e não enfraquecidos (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `:72-76`). O teste novo, `occupancyReplacesCommasInTheGuestName` (`ReportsTest.java:80-93`), confere a troca com um valor exato (`Lima  Bruno`). Ele roda contra o banco de verdade, via `EmptyDatabase`.

Na pergunta da mutação:
- se alguém apagasse o `replace`, `:90` falharia;
- se trocasse a ordem das colunas ou esquecesse o cabeçalho, `:61` e `:73` falhariam;
- se o código imprimisse outro campo (o e-mail, por exemplo), os testes também falhariam.

Um pequeno ponto fraco: todas as reservas do `@BeforeEach` usam o mesmo nome, "Bruno Lima" (`ReportsTest.java:31`). Se o código trocasse o hóspede de uma reserva pelo de outra, nenhum teste veria, mas o teste da vírgula cobre um nome diferente numa linha só. O teste pela API (`HttpApiTest.java:113-116`) só confere o total, como na base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, não há chave estrangeira sem índice nem data como texto, e há um `EXCLUDE USING gist` contra duas reservas na mesma noite. A única migração, `V1__create_tables.sql`, está igual à da base. Uma conferência acusou algo: as colunas `status`, `rate_plan`, `type` e `channel` estão sem CHECK (`measures/database.md`).
**Nota:** bom

O pedido não mexe no banco: o nome já estava em `reservations.guest_name`. O agente acertou em não acrescentar migração nem editar a que existia. O SQL usa parâmetros: os `+` em `ReservationStore.java:33`, `:83`, `:98` e `:111` só juntam constantes. As conexões são fechadas com try-with-resources, e a duplicação de reserva é barrada pelo banco (`ReservationStore.java:55-58` trata o `23P01`).

Os pontos menores vêm da base:
- as colunas de situação não têm CHECK;
- `OccupancyReport.java:25` lê a tabela inteira (`store.all()`) e filtra em Java.

## Comportamento

**Números:** 55 de 55 conferências passaram, com 0 falhas (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 10: 4 de 4;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Pela API, o relatório de ocupação traz o cabeçalho `room,type,reservation,guest,check_in,check_out`. O nome `Silva,Ana` sai como `Silva Ana` na linha da reserva, e o total continua `total,1/5`. O relatório financeiro mantém o mesmo cabeçalho. Tudo o que funcionava antes continua funcionando. Para o dono da pousada, isso quer dizer que a recepção passa a ver o nome do hóspede, e nada mais mudou.

## Dúvidas

Nenhuma que mude as notas. A troca literal da vírgula pode deixar dois espaços seguidos (`Lima  Bruno`, `ReportsTest.java:90`). É a leitura literal do pedido, e o roteiro só testa um nome sem espaço depois da vírgula.
