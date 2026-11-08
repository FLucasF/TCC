# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `pousada` (o `Main`, que monta tudo), `web` (as rotas Javalin em `Api`), `service`
(o `ReservationService`, o parse do corpo em `NewReservation` e o `ConfirmationMailer`), `domain` (a reserva com as
transições, o preço, o comprovante, o dinheiro), `reports` (os quatro relatórios CSV) e `repo` (as interfaces de
acesso a dados, a versão JDBC e a versão em memória que os testes usam). Uma requisição entra em `Api`
(`web/Api.java:54-79`), chama o serviço ou o relatório, o serviço muda a `Reservation` (`domain/Reservation.java:54-77`)
e grava pelo `JdbcReservationRepository`. Os dados ficam em duas tabelas no PostgreSQL, criadas por
`CREATE TABLE IF NOT EXISTS` no código (`repo/Database.java:28-54`). O pattern pedido, o Template Method, está bem
aplicado: `Report.render` é `final` e os quatro relatórios são subclasses dele. As 64 conferências do comportamento
passaram. Os pontos mais fracos estão no banco: não há migrações, nenhuma restrição do banco impede duas reservas do
mesmo quarto na mesma noite (só o `synchronized` dentro do processo), não há pool de conexões e nenhuma transação.
Também há poucos testes: três métodos, todos contra o repositório em memória.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). JDBC só em `repo`
(e no `Main`, que monta a conexão); Javalin só em `web` (e no `Main`). A maior classe é `JdbcReservationRepository`,
com 152 linhas.
**Nota:** bom

- A borda HTTP é fina. Cada rota lê o id ou o corpo, chama um método do serviço e escreve a resposta
  (`web/Api.java:63-69`). Os relatórios são registrados num laço sobre a lista injetada (`web/Api.java:71-79`), e os
  erros viram JSON num lugar só (`web/Api.java:81-86`).
- As regras não conhecem nem o Javalin nem o JDBC. As transições ficam em `domain/Reservation.java:54-77` e
  `domain/ReservationStatus.java:11-28`, o preço em `domain/Pricing.java:24-37` e a capacidade e a disponibilidade em
  `service/ReservationService.java:19-30`.
- O acesso ao banco está isolado em `repo/` atrás de `ReservationRepository` e `MessageRepository`. Os relatórios
  dependem só da interface (`reports/MonthReport.java:11-29`).
- A montagem acontece num lugar só, em `Main.java:22-34`. O valor da taxa de turismo é injetado ali
  (`Main.java:20,30`), e não fica escondido dentro do relatório.
- Um ponto pequeno: o parse do JSON do pedido (`JsonNode`, do Jackson) fica no pacote `service`
  (`service/NewReservation.java:3,13-34`), quando é coisa da borda HTTP. A regra em si (`ReservationService`) não usa
  Jackson, então isso não pesa na nota. O catálogo de quartos é `static`, mas imutável (`domain/RoomCatalog.java:7-12`),
  o que é razoável para quartos que "quase nunca mudam".

## Design de baixo nível

**Números:** os 4 relatórios (occupancy, revenue, tourism-tax, rate-plans) passam pelo esqueleto `Report.render`, e
nenhum fica fora dele. A linha de título é escrita num lugar só (`reports/Report.java:43`), e nenhum relatório a repete.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** `Report<P>` é uma classe abstrata. O método-modelo é `public final String render(...)`
  (`reports/Report.java:34-48`), que segue a ordem: ler o período, montar o corpo, escrever o título, o cabeçalho, as
  linhas e o total. Os passos que variam são abstratos: `name`, `periodParam`, `parsePeriod`, `header` e `body`
  (`reports/Report.java:19-28`). Há também um gancho opcional, `periodText` (`reports/Report.java:30-32`). Os quatro
  relatórios são subclasses e só preenchem esses passos:
  - `OccupancyReport` estende `Report<LocalDate>`;
  - `RevenueReport`, `TourismTaxReport` e `RatePlanReport` estendem `MonthReport`, uma camada intermediária que junta
    o que os três relatórios de mês têm em comum: o parâmetro `month`, o parse e a seleção das reservas que começam no
    mês (`reports/MonthReport.java:17-34`).

  A linha de data que o dono quer para o ano que vem entraria só em `render`. Um relatório novo entra só na lista do
  `Main.java:27-31`, porque a `Api` registra as rotas sozinha (`web/Api.java:71`). Uma ressalva pequena: o texto
  `"total,"` é montado em cada subclasse (`reports/RevenueReport.java:33`, `reports/TourismTaxReport.java:42`,
  `reports/RatePlanReport.java:41`, `reports/OccupancyReport.java:44`). O esqueleto poderia cuidar desse prefixo.
- **O exagero.** Não há. As interfaces de repositório têm duas implementações cada, a JDBC e a em memória, e a em
  memória é usada de verdade nos testes (`src/test/java/pousada/PousadaTest.java:43-44`). `MonthReport` evita que três
  relatórios repitam o mesmo código. Não há fábricas, builders nem camadas que só repassam.
- **O resto do código.**
  - Dinheiro é sempre `BigDecimal`, arredondado com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`,
    `domain/Pricing.java:26,32-35`, `domain/RatePlan.java:17-19`).
  - Datas são `LocalDate`, e situação e plano são `enum`.
  - As transições ficam numa tabela (`domain/ReservationStatus.java:11-19`), com uma única porta,
    `moveTo` (`domain/Reservation.java:72-77`). O desconto e a devolução moram no enum do plano
    (`domain/RatePlan.java:6-23`), sem `if` por plano.
  - Os métodos são curtos e os nomes dizem o que fazem.

  Os detalhes menores: o `id` da `Reservation` é mutável, por causa do `assignId` (`domain/Reservation.java:7,88-90`).
  O `Money.plain` depende de o valor já vir com escala 2 (`domain/Money.java:32-34`).

## Testes

**Números:** 3 testes rodaram, 0 falhas, 0 erros, 0 pulados; 3 métodos de teste escritos (`measures/tests.md`).
Cobertura de linhas: 335 de 485 (69,1%). Cobertura de ramos: 67 de 104 (64,4%). Por pacote: `domain` 142/142,
`reports` 71/72, `repo` só 26/144 (o JDBC não é exercitado), `pousada` 0/14.
**Nota:** com problemas

- **O que está bem.** Os três métodos conferem valores de verdade:
  - o preço na alta temporada, 599,76, e o desconto NON_REFUNDABLE de 89,96, com total de 509,80
    (`PousadaTest.java:71-81`);
  - o e-mail inteiro, texto por texto (`PousadaTest.java:103-116`);
  - o CSV de ocupação inteiro, com o título (`PousadaTest.java:120-121`);
  - os relatórios de planos e de taxa de turismo inteiros, com a NON_REFUNDABLE cancelada contando nos planos
    (`PousadaTest.java:143-156`).
- **O que falta ou está fraco.**
  - Tudo roda contra `InMemoryReservationRepository` (`PousadaTest.java:43-48`). O SQL de verdade, inclusive o de
    sobreposição e o de ocupação, nunca é testado: `repo` tem 26 de 144 linhas cobertas.
  - As recusas conferem só o status HTTP, nunca o código do erro (`PousadaTest.java:92-98`). Trocar
    `ROOM_UNAVAILABLE` por outro código não quebraria nenhum teste.
  - A asserção em `PousadaTest.java:141` não confere a segunda via: ela compara o total do check-out, e a resposta de
    `/document` só serve para escolher entre o total e 0.
  - O relatório financeiro só tem o total conferido, num caso sem linhas (`PousadaTest.java:127`).
  - São três métodos longos, que testam muitas regras de uma vez.
- **A pergunta da mutação na parte nova.**
  - A taxa de turismo é conferida só com uma reserva de 1 hóspede por 1 noite (`PousadaTest.java:154-155`). Trocar
    `guests * nights` por só `nights` ou só `guests` (`reports/TourismTaxReport.java:31`) não quebraria nenhum teste.
    O comentário em `PousadaTest.java:150` fala de 27,00, mas essa reserva está cancelada e não entra.
  - O valor de R$ 4,50 do `Main.java:20` não é testado, porque o teste injeta o seu próprio valor.
  - Trocar `signum() > 0` por `>= 0` em `reports/MonthReport.java:33` também passaria, porque não há reserva pendente
    em julho no teste.
  - O filtro de pagas da taxa de turismo e a ordem STANDARD e depois NON_REFUNDABLE estão protegidos.

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), as duas com chave primária. Dinheiro em `numeric(12,2)` nas 5
colunas. Datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. 0 transações explícitas e 0
`FOR UPDATE` (`measures/database.md`). O esquema é criado no código Java, sem migrações.
**Nota:** com problemas

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro de `repo/Database.java:28-54`, sem
  Flyway ou Liquibase. A primeira coluna nova (por exemplo, o estado do hóspede para o relatório que o contador já
  anunciou) não chega a um banco que já existe.
- **Nenhuma restrição protege as regras.** Nada no banco impede duas reservas do mesmo quarto na mesma noite: não há
  `EXCLUDE` nem trava. A proteção é só o `synchronized` do `ReservationService` (`service/ReservationService.java:19`),
  que vale para um processo só. Com duas instâncias, ou com uma escrita fora do serviço, a reserva dobrada passa.
  `status`, `rate_plan` e `channel` são `text` sem CHECK (`repo/Database.java:38,43,50`). Não há tabela de quartos, nem
  chave estrangeira, nem índice em `room_number`/`check_in`, que as consultas de sobreposição e de relatório usam
  (`repo/JdbcReservationRepository.java:70-71,86,95-96`).
- **Não há pool nem transação.** `PGSimpleDataSource` (`repo/Database.java:16`) abre uma conexão física nova a cada
  chamada; o "pool: 1 arquivo" do script é esse `DataSource`, que não é um pool. A confirmação grava a reserva e
  depois a mensagem em duas conexões separadas, sem transação (`service/ReservationService.java:39-41`). Se a segunda
  escrita falhar, a reserva fica confirmada sem o e-mail registrado.
- **O que está certo.** Todo SQL usa parâmetros `?`. O que entra com `+` é a constante `COLUMNS`
  (`repo/JdbcReservationRepository.java:22-23`). As conexões são devolvidas com try-with-resources
  (`repo/JdbcReservationRepository.java:36,53,72,107`). Os dados ficam no banco e sobrevivem ao reinício (3 de 3 no
  comportamento).

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam (`measures/behavior.md`):
- parte comum: 46 de 46;
- reinício: 3 de 3;
- cenário 09 (os relatórios): 13 de 13;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o roteiro confere funciona como o texto pede. O dono tem:
- os preços com a temporada e o arredondamento "meio para o par" (599,76; desconto de 37,48);
- as recusas com os códigos certos;
- o e-mail e o comprovante;
- as devoluções de cada plano;
- os quatro relatórios, com a linha "Pousada Maré Alta,<nome>,<período>", a taxa de turismo só das pagas, os planos
  com as linhas de zero e a NON_REFUNDABLE cancelada contando.

As dez rodadas de reservas simultâneas não criaram nenhuma reserva dobrada. Isso vem do `synchronized` num único
processo: a falta de proteção no banco já foi contada na coluna Banco e não muda esta nota. O log do serviço não tem
nenhuma exceção.

## Dúvidas

Nenhuma.
