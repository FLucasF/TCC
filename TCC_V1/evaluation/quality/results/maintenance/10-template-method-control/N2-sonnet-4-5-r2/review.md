# Revisão: manutenção, cenário 10

## Visão geral

O sistema da base tem sete pacotes: `hotel` (montagem, `Hotel.java`), `hotel.web` (as rotas do Javalin, `HttpApi`), `hotel.reservations` (as regras, o preço e o acesso a dados `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição entra pelo `HttpApi`, que chama o serviço ou o relatório, e os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway. O pedido era pequeno: uma coluna `guest` no relatório de ocupação, logo depois de `reservation`, com a vírgula do nome trocada por espaço, e uma frase sobre um telefone que talvez venha um dia. O agente mudou só dois arquivos (`diff -rq base final`): três linhas em `src/main/java/hotel/reports/OccupancyReport.java` (34, 37 e 41) e os testes do relatório em `src/test/java/hotel/reports/ReportsTest.java`, com dois testes da base ajustados e um novo. Não criou estrutura para o telefone. Os 27 testes passam, as 55 conferências do roteiro passam, e não achei nenhum defeito sério introduzido pelo agente. Os poucos pontos fracos que existem (o ciclo `documents`↔`reservations`, o relatório que lê a tabela inteira, as colunas de situação sem `CHECK`) já estavam na base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os mesmos números: só muda `hotel.reports`, que passa de 95 para 97 linhas (`measures/dependencies.md` comparado com `measures/dependencies-base.md`).
**Nota:** bom

A mudança fica onde o relatório já estava (`OccupancyReport.java:34-43`) e não acrescenta nenhuma dependência: `Reservation.guest()` já vinha de `hotel.reservations`, que `hotel.reports` já usava (`OccupancyReport.java:3-4`). A borda HTTP continua fina: a rota só lê a data e devolve o CSV (`web/HttpApi.java:83-85`). O relatório não conhece nem o Javalin nem o JDBC: lê pelo `ReservationStore` e pelo `Rooms`, que recebe no construtor (`OccupancyReport.java:18-21`). A montagem continua num lugar só (`Hotel.java:27`). O ciclo `documents`↔`reservations` vem da base (`reservations/ReservationService.java:3` importa `ReservationReceipt`, que por sua vez usa `Reservation`), e o agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, com 3 linhas (1 trocada e 2 novas), e nenhuma classe, interface ou método novo. O caso pedido (a coluna `guest` com a troca da vírgula) fica todo dentro do `OccupancyReport`. Nada foi feito para o telefone.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este cenário é o controle negativo. A frase "talvez um dia a recepção peça o telefone do hóspede também" convida a montar um esqueleto genérico de relatórios ou colunas configuráveis, e o agente não fez isso. Ele mudou o cabeçalho (`OccupancyReport.java:34`), calculou o nome numa variável local com `replace(',', ' ')` (`OccupancyReport.java:37`) e acrescentou um `append` no lugar certo da linha (`OccupancyReport.java:41`). É exatamente a resposta que `references/controls.md` descreve como contenção. A troca é literal, como o texto pede: "Silva, Ana Paula" vira "Silva  Ana Paula", com dois espaços (`ReportsTest.java:90`). É uma leitura fiel do pedido, não um defeito. O agente também não tratou aspas nem quebras de linha no nome, que o texto não pede.

O resto do código vem da base e está bem resolvido. O dinheiro é `BigDecimal`, com arredondamento centralizado em `PriceCalculator.cents`, e as datas são `LocalDate`. As situações são `enum` (`ReservationStatus`, `RatePlan`). A devolução usa um `switch` exaustivo sobre o plano (`reservations/ReservationService.java:82-85`). Os erros têm código próprio (`HotelException`/`ErrorCode`), e as `SQLException` são capturadas só para relançar com a causa (`reservations/ReservationStore.java:54` e seguintes). O único `catch (Exception)` está na leitura do corpo JSON (`web/HttpApi.java:113`), e o comentário ao lado explica por quê. Nada disso foi tocado pelo agente.

## Testes

**Números:** 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 27 métodos escritos, contra 26 na base. A cobertura de linhas é de 93,8% (473 de 504) e a de ramos, de 87,1% (101 de 116). No pacote `hotel.reports`, todas as linhas (50 de 50) e todos os ramos (22 de 22) são cobertos (`measures/tests.md`).
**Nota:** bom

O agente manteve os 26 testes da base passando e não apagou nem enfraqueceu nenhum. Os dois testes de ocupação da base foram atualizados para o novo cabeçalho e para o nome em cada linha, com o CSV inteiro conferido por `assertEquals` (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `72-76`). O teste novo `guestNameWithCommaIsReplacedWithSpace` cria uma reserva com o nome "Silva, Ana Paula" e confere a linha exata (`ReportsTest.java:79-93`). A pergunta da mutação tem resposta: se alguém tirasse o `replace`, mudasse a posição da coluna ou apagasse o `append` do nome, algum dos três testes falharia. Há um ponto pequeno: o teste novo fixa o id `6` (`ReportsTest.java:90`), o que depende da ordem do `@BeforeEach`. Os testes da base já fazem o mesmo com os ids 1, 2 e 3, então isso não pesa. O teste pela API da ocupação (`web/HttpApiTest.java:113-116`) só confere o total. É assim desde a base, e a coluna fica coberta no nível de unidade.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, não há chave estrangeira sem índice nem data guardada como texto, e há um `EXCLUDE USING gist` contra duas reservas na mesma noite. As conferências só acusaram as colunas de situação e de tipo sem `CHECK` (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`). A única migração, `V1__create_tables.sql`, está "igual" à da base (`measures/database.md`).
**Nota:** bom

O agente não mexeu no banco, e não precisava: o nome do hóspede já estava gravado e já vinha em `Reservation.guest()`. O esquema continua criado por migração do Flyway, que o agente não editou. O SQL usa parâmetros: as concatenações listadas pelo script só juntam constantes, como `COLUMNS` (`reservations/ReservationStore.java:98` e `111`). Há dois pontos fracos, ambos da base. O relatório de ocupação traz a tabela inteira (`store.all()`, `reports/OccupancyReport.java:25`) e filtra em Java. As colunas de situação não têm `CHECK`. Nenhum dos dois vem do agente.

## Comportamento

**Números:** 55 de 55 conferências passaram e nenhuma falhou. Por parte: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 4 de 4 no cenário 10 (o cabeçalho com `guest` depois de `reservation`, a vírgula virando espaço, o total mantido, o financeiro com o mesmo cabeçalho) e 2 de 2 nas reservas simultâneas (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada: a recepção passa a ver o nome do hóspede no relatório de ocupação, na posição pedida, e um nome com vírgula não quebra o CSV. Todo o resto continua como antes: preços, recusas, confirmação com e-mail, cancelamentos, relatório financeiro, dados guardados depois de reiniciar e a proteção contra duas reservas do mesmo quarto ao mesmo tempo.

## Dúvidas

Nenhuma que mude as notas. O texto diz "troca por um espaço", e o agente seguiu ao pé da letra, o que deixa dois espaços em "Silva  Ana Paula". Trocar ", " por um espaço só também seria uma leitura sensata, e a evidência (o roteiro e o texto) aceita as duas.
