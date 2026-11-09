# Revisão: manutenção, cenário 10

## Visão geral

O sistema da base tem 7 pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages`). Uma requisição entra pelas rotas Javalin em `hotel.web.HttpApi`, que leem a requisição e chamam o serviço (`ReservationService`) ou os relatórios (`OccupancyReport`, `RevenueReport`). Esses chegam ao PostgreSQL pelas classes de acesso a dados (`ReservationStore`, `Rooms`, `SentMessages`), com JDBC e um pool. O esquema é criado por uma migração do Flyway. O pedido era pontual: uma coluna `guest`, logo depois de `reservation`, no relatório de ocupação, com as vírgulas do nome trocadas por espaço. A frase sobre o telefone no futuro estava lá para tentar o agente. Ele mudou só duas coisas: `src/main/java/hotel/reports/OccupancyReport.java` (o cabeçalho, mais uma linha que limpa o nome e mais uma linha que acrescenta a coluna) e `ReportsTest` (os dois testes de ocupação atualizados e um teste novo para a vírgula). O agente não criou nenhuma estrutura nova. Os 27 testes passam e as 55 conferências do roteiro passam. Não há nada sério. Os pontos fracos que existem já estavam na base.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, igual à base. Um ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). A única diferença é `hotel.reports`, que foi de 95 para 97 linhas.
**Nota:** bom

A mudança fica no lugar onde o relatório já estava, `OccupancyReport.java:34-41`. Ela não acrescenta nenhuma dependência entre pacotes, porque o nome vem de `reservation.guest()`, que `hotel.reports` já usava pelo `Reservation`. A borda HTTP continua fina: `HttpApi.java:83-86` lê a data e devolve o CSV. As regras não usam Javalin nem SQL: o JDBC fica só em `reservations` (no `ReservationStore`), `rooms`, `messages` e na montagem. A montagem fica num lugar só, `Hotel.java:27`. O ciclo entre `documents` e `reservations` veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 1 classe de produção mudada, com 3 linhas mudadas ou acrescentadas (`OccupancyReport.java:34`, `:37`, `:41`). Nenhuma classe, interface ou tipo novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O agente fez exatamente a mudança que `references/controls.md` descreve como contenção para este cenário: uma coluna a mais no `OccupancyReport` e a troca da vírgula por espaço. O cabeçalho ganhou `guest` depois de `reservation` (`OccupancyReport.java:34`). O nome é limpo numa variável local, com `replace(',', ' ')` (`:37`), e entra na linha na posição pedida (`:41`). Ele não criou esqueleto de relatório, sistema de colunas configuráveis nem nenhum tipo para o "telefone do futuro". O `RevenueReport` ficou intacto, como o pedido mandava ("o resto do relatório continua igual").

O resto do código, que vem da base, está em bom estado. O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` num lugar só (`PriceCalculator.java:50-52`, função `cents`). As datas são `LocalDate`. Situação e plano são `enum`, tratados com `switch` exaustivo (`ReservationService.java:82-85`, `PriceCalculator.java:26-29`). Os erros usam um código próprio (`HotelException`/`ErrorCode`), mapeado para HTTP num lugar só (`HttpApi.java:100-107`). Há um `catch (Exception)` em `HttpApi.java:113`, mas ele só serve para transformar um corpo ilegível num 400, e veio da base.

## Testes

**Números:** 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 27 métodos escritos; a base tinha 26. Cobertura: 93,8% das linhas (473/504) e 87,1% dos ramos (101/116). `hotel.reports` tem 50/50 linhas e 22/22 ramos.
**Nota:** bom

Os 26 testes da base continuam passando. Os dois que conferem o CSV de ocupação foram atualizados com o valor exato da nova coluna, e não enfraquecidos: os `assertEquals` continuam comparando o texto inteiro (`ReportsTest.java:60-65` e `:72-76`). O teste novo `guestNamesWithCommasAreReplacedWithSpaces` (`ReportsTest.java:80-93`) cria a reserva de "Silva, Maria" e confere a linha inteira, `Silva  Maria`. A pergunta da mutação dá resultado: apagar o `replace`, trocar a posição da coluna ou tirar o cabeçalho novo faz algum teste falhar. Há uma fraqueza pequena. Todas as reservas do `@BeforeEach` são do mesmo hóspede, "Bruno Lima" (`ReportsTest.java:31`), e o teste da vírgula tem uma linha só. Por isso, um erro que pusesse na linha o nome de outra reserva passaria despercebido nos testes, mas não é um risco realista com esse código (`OccupancyReport.java:37`). O teste da API só confere que o relatório termina com o total (`HttpApiTest.java:116`), e isso veio da base.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`). Uma migração do Flyway, `V1__create_tables.sql`, igual à da base. Todas as tabelas têm chave primária. O dinheiro é todo `numeric(10,2)`. Nenhuma chave estrangeira está sem índice e nenhuma data está em texto. Há um `EXCLUDE USING gist` contra a mesma noite no mesmo quarto. A única coisa que as conferências acusaram foi a falta de `CHECK` nas colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`).
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: o nome do hóspede já era guardado na reserva, e a migração da base continua igual (`measures/database.md`). O SQL usa parâmetros. Os `+` que o script listou em `ReservationStore.java:33`, `:83`, `:98` e `:111` só juntam constantes (`COLUMNS`). Os defeitos que existem vieram da base, e o agente não os piorou. Um é a falta de `CHECK` nas colunas de situação e de tipo. O outro é que `OccupancyReport.java:25` (e `RevenueReport.java:25`) leem a tabela inteira pelo `store.all()` (`ReservationStore.java:108-111`) e filtram a noite em Java, o que vai pesar quando houver muitas reservas.

## Comportamento

**Números:** 55 de 55 conferências passaram, nas quatro partes: a parte comum (46/46), os dados depois de reiniciar (3/3), o cenário 10 (4/4) e as reservas simultâneas (2/2).
**Nota:** bom

Pela API, o relatório de ocupação tem a coluna `guest` logo depois de `reservation`, a vírgula do nome vira espaço, e o total e o relatório financeiro continuam como estavam. Tudo o que funcionava antes continua funcionando: preços, recusas, confirmação com e-mail, cancelamentos, os dados depois de reiniciar e as reservas simultâneas do mesmo quarto. Para o dono da pousada, isso quer dizer que a recepção já vê o nome do hóspede e que nada mais mudou.

## Dúvidas

O agente leu o pedido ao pé da letra: "Silva, Maria" vira "Silva  Maria", com dois espaços (`ReportsTest.java:90`). O pedido diz "troca por um espaço", então a leitura é defensável, mas o dono talvez esperasse um espaço só. Um nome com aspas ou com quebra de linha também bagunçaria o CSV. O pedido falou só da vírgula, então isso não conta contra o agente, mas a evidência não decide se ele devia ter ido além.
