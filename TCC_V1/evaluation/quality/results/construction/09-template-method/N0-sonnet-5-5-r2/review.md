# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `web` (Javalin, JSON e leitura do corpo), `service` (as regras da reserva, o comprovante
e o e-mail), `domain` (registros, enums, `Money` e `Pricing`), `report` (os quatro relatórios e o escritor de CSV),
`store` (JDBC com HikariCP e uma versão em memória usada nos testes) e a raiz (`Main` e `Config`). Uma requisição entra
por `WebApp`, que repassa numa linha só ao `ReservationService`. O serviço abre uma transação com `Store.transaction`
e usa os métodos de `Tx`, que o `PostgresStore` implementa com SQL parametrizado. Os dados ficam em três tabelas que o
próprio código cria ao subir. Os relatórios devolvem um `ReportData` (nome, período, cabeçalho, linhas e total), e
o `CsvWriter.render` monta o CSV de todos eles no mesmo lugar. O roteiro passou inteiro (64 de 64), inclusive as
reservas simultâneas, e os testes passam e cobrem 92% das linhas. O defeito mais sério está no banco: o esquema é
criado com `CREATE TABLE IF NOT EXISTS` no Java, sem migrações e sem nenhuma restrição `CHECK`.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, nenhum ciclo. O Javalin aparece só em `web` e na raiz, o JDBC e
o pool só em `store`. A maior classe é `PostgresStore`, com 340 linhas; as outras ficam abaixo de 160.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta
  (`web/WebApp.java:28-40`). As rotas dos relatórios são registradas num laço sobre a lista de relatórios
  (`web/WebApp.java:42-47`). A leitura e a validação do corpo ficam em `web/RequestParser.java:18-42`.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` trabalha com `Store`/`Tx`
  (`service/ReservationService.java:43-57`, `65-75`). A transição de estado e a devolução estão em
  `service/ReservationService.java:77-88`. O preço fica em `domain/Pricing.java:29-40`.
- **O acesso ao banco fica isolado** em `store/PostgresStore.java`.
- **A montagem acontece num lugar só,** em `Main.java:11-19`.

Pontos menores:

- `report` depende de `service` só para pegar a constante `HOTEL_NAME` (`report/CsvWriter.java:3,15`) e a
  `ApiException` (`report/Periods.java:3`). O nome da pousada estaria melhor no domínio ou na configuração.
- `ApiException` carrega o código HTTP dentro do serviço (`service/ApiException.java:7,17-29`).
- Os relatórios leem direto do `Store` (`report/MonthlyReport.java:24`), sem passar pelo serviço. Num sistema deste
  tamanho, isso é aceitável.

Nenhum desses pontos cria ciclo nem mistura camadas.

## Design de baixo nível

**Números:** os 4 relatórios passam pelo mesmo esqueleto de título, cabeçalho, linhas e total
(`CsvWriter.render`); nenhum fica fora dele. Três dos quatro (revenue, tourism-tax e rate-plans) também herdam de
`MonthlyReport`, cujo método-modelo `generate` é `final`. A linha de título está escrita em um lugar só.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O esqueleto pedido está num lugar só, `report/CsvWriter.java:13-24`, que escreve nesta ordem:

1. o título, com o nome da pousada, o nome do relatório e o período (linha 15);
2. o cabeçalho (linha 16);
3. as linhas (linha 17);
4. o total (linha 18).

Os quatro relatórios passam por ele (`web/WebApp.java:42-46`, `report/Reports.java:11-17`). Cada relatório só
entrega as partes que variam, como dados num `ReportData` (`report/ReportData.java:6-7`): o nome, o cabeçalho, as
linhas e o total. Nenhum deles repete a sequência dos passos nem escreve a linha de título. A linha com a data,
pedida para o ano que vem, entraria só em `CsvWriter.render`.

Isso é a composição que a referência aceita: um escritor genérico que recebe as partes que variam, com os quatro
relatórios passando por ele. Os dois que já existiam (ocupação e financeiro) também passam. Por isso, a classe é
alternativa equivalente.

Há também um Template Method de verdade para a parte mensal. Em `report/MonthlyReport.java:21-28`, `generate` é
`final`: valida o mês, busca as reservas que começam no mês e chama o passo abstrato `build`. As subclasses
`RevenueReport`, `TourismTaxReport` e `RatePlansReport` preenchem só esse passo, e a seleção comum de quem deixou
dinheiro é reaproveitada (`retainingMoney`, `report/MonthlyReport.java:31-33`). A ocupação implementa `Report` direto
(`report/OccupancyReport.java:12-38`), o que faz sentido porque o período dela é um dia, não um mês.

O valor da taxa vem da configuração (`Config.java:19-28`, `report/TourismTaxReport.java:11-16`), como pede a
mudança anual da prefeitura.

**O exagero.** Não há estrutura sem uso:

- `Report` tem quatro implementações.
- `MonthlyReport` tem três subclasses.
- `Store`/`Tx` têm duas implementações, e a de memória é usada pelos testes (`MemoryApiTest.java:9`).
- `Reports.all` é só uma lista.

**O resto do código.** O código é limpo:

- dinheiro em `BigDecimal` com `HALF_EVEN`, arredondado a cada passo (`domain/Money.java:15-17`,
  `domain/Pricing.java:29-40`);
- datas em `LocalDate`, situações e planos em `enum`;
- registros imutáveis, com `with*` para as mudanças (`domain/Reservation.java:25-38`);
- erros com código, e um único tratador genérico que registra a causa no log (`web/WebApp.java:49-54`).

Há pequenos pontos, nenhum que bloqueie a aceitação:

- A validação de data está duplicada, com a mesma regex e o mesmo parse, em `web/RequestParser.java:14,60-68` e em
  `report/Periods.java:10,15-22`.
- `Receipt` guarda as datas como `String` e as converte de volta para `LocalDate` para formatar
  (`service/Receipt.java:18-19,48-50`).
- O construtor de 16 argumentos de `Reservation` é copiado em `store/MemoryStore.java:48-51` e em
  `store/PostgresStore.java:218-220`.
- A criação do esquema faz um cast de `Tx` para `PgTx` (`store/PostgresStore.java:43`).
- `MemoryStore`, que só os testes usam, está em `src/main` (`store/MemoryStore.java:16`).

## Testes

**Números:** 28 testes rodaram, nenhum falhou e nenhum foi pulado. São 16 métodos escritos: 12 em `ApiTestBase`,
rodados duas vezes (com `MemoryStore` e com PostgreSQL), e 4 em `PricingTest`. A cobertura é de 92,0% das linhas
(481 de 523) e 78,3% dos ramos (108 de 138). Os pacotes de regras estão quase todos cobertos: `domain` 39/39,
`service` 95/98, `report` 91/93.
**Nota:** bom

Os testes conferem as regras com valores, e não só os códigos de status:

- **O preço:** o exemplo de 599,76 (`PricingTest.java:30-33`); o meio para o par, com 37,485 virando 37,48
  (`PricingTest.java:42-45`); as fronteiras da alta temporada (`PricingTest.java:18-26`); e uma estadia que atravessa
  o dia 15 de dezembro (`PricingTest.java:49-52`).
- **Os erros:** todos os códigos (`ApiTestBase.java:100-112`).
- **A sobreposição:** com reservas encostadas e com uma cancelada (`ApiTestBase.java:115-121`).
- **O e-mail:** o texto inteiro, letra por letra (`ApiTestBase.java:124-148`).
- **O comprovante:** a linha do desconto e "1 diária" (`ApiTestBase.java:151-156`).
- **O ciclo de vida e a segunda via:** `ApiTestBase.java:159-177`.
- **As devoluções:** `ApiTestBase.java:180-197`.

A parte nova do cenário é testada com o CSV inteiro, a partir de um mês com uma reserva pendente, uma cancelada com
devolução e uma não reembolsável cancelada (`ApiTestBase.java:200-210`):

- a linha de título nos quatro relatórios (`ApiTestBase.java:217-223`, `231-238`, `244-250`, `256-269`);
- a taxa de 18,00 e de 27,00 (`ApiTestBase.java:247-249`);
- a não reembolsável cancelada contando no relatório por plano (`ApiTestBase.java:259-261`);
- os dois planos aparecendo mesmo num mês vazio (`ApiTestBase.java:263-269`).

A pergunta da mutação tem resposta boa. Trocar `isPaid` por "não cancelada" na taxa poria a pendente na lista.
Trocar `> 0` por `>= 0` em `retainingMoney` poria a cancelada com devolução no relatório. Mudar a multiplicação da
taxa mudaria o 27,00. Os três quebrariam os testes.

O que fica sem rede:

- a leitura da taxa pela variável de ambiente e o valor padrão de 4,50 (`Config.java:22-28`): o pacote raiz tem
  6 de 28 linhas cobertas, e o teste passa 4,50 direto (`ApiTestBase.java:36`);
- as reservas simultâneas, sem nenhum teste de concorrência.

O teste contra o PostgreSQL só roda quando `DB_URL` está definida (`PostgresApiTest.java:11`). Nesta medição ela
estava definida, e os 12 testes rodaram.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(12,2)` nas 6 colunas, as datas em `date`, e a chave estrangeira `reservations.room_number` tem índice.
As conferências acusaram duas coisas: nenhum arquivo de esquema ou migração (o `CREATE TABLE` está no Java) e
nenhuma restrição `CHECK`, `UNIQUE` ou `EXCLUDE` (os campos `status`, `rate_plan`, `type` e `channel` são `varchar`
sem `CHECK`). Há 3 candidatos a SQL montado com `+`, todos com texto constante.
**Nota:** com problemas

**O que está certo:**

- Os tipos estão certos e o `NOT NULL` está onde precisa (`store/PostgresStore.java:53-70`). Só `refund` aceita
  null, como o texto pede.
- Todo dado de fora entra por `?`. Os três trechos montados com `+` (`store/PostgresStore.java:182,239,274`) juntam só
  constantes: a lista de colunas, `" FOR UPDATE"` ou `""`, e os `where`/`order` fixos de `startingBetween` e
  `occupying` (`store/PostgresStore.java:287,295`).
- As conexões vêm do HikariCP e são devolvidas com try-with-resources. Cada operação roda numa transação com
  rollback (`store/PostgresStore.java:101-116`).
- A confirmação grava a reserva e a mensagem na mesma transação (`service/ReservationService.java:66-74`).
- A sobreposição é protegida por uma trava de linha no banco: `SELECT ... FROM rooms WHERE number = ? FOR UPDATE`
  (`store/PostgresStore.java:167-175`), chamada antes da verificação (`service/ReservationService.java:49-50`). A
  trava vale entre processos, e o roteiro confirmou isso.

**Os defeitos:**

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código
  (`store/PostgresStore.java:47-79`), sem migrações versionadas. Quando o próximo pedido precisar de uma coluna (por
  exemplo, o estado do hóspede para o relatório que o contador já anunciou), esse código não vai saber acrescentá-la
  numa tabela que já existe.
- **O banco não protege as regras sozinho.** Não há `CHECK` em `status`, `rate_plan` nem `guests`, e não há uma
  restrição `EXCLUDE` contra noites sobrepostas. Toda a proteção depende de o código pegar a trava.
- **Os quartos não se atualizam.** Eles são semeados com `ON CONFLICT DO NOTHING` (`store/PostgresStore.java:83-93`),
  então uma mudança de diária em `Config.ROOMS` (`Config.java:12-17`) nunca chega a um banco que já existe.

## Comportamento

**Números:** 64 de 64 conferências passaram, nenhuma falhou:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 09 | 13 de 13 |
| as duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto descreve:

- o preço com a alta temporada e o meio para o par;
- todas as recusas, com os códigos combinados;
- a confirmação, com o e-mail e o comprovante;
- o check-in, o check-out e a segunda via;
- as devoluções por plano;
- os quatro relatórios começando com "Pousada Maré Alta,<nome>,<período>";
- a taxa de turismo só das reservas pagas;
- o relatório por plano com STANDARD e NON_REFUNDABLE, mesmo sem reservas;
- a não reembolsável cancelada contando no financeiro e nos planos.

Os dados continuam lá depois de reiniciar, e em dez rodadas de duas reservas simultâneas para o mesmo quarto, sempre
só uma foi criada.

## Dúvidas

Na classe do pattern, fiquei entre `acerto` e `alternativa equivalente`. Há uma classe abstrata com o método-modelo
`final` (`report/MonthlyReport.java:22`), mas ela cobre só os três relatórios mensais. O esqueleto comum aos quatro
(título, cabeçalho, linhas e total) está numa composição: `CsvWriter.render` recebe as partes como dados. Como é essa
composição que garante que "quando o jeito dos relatórios mudar, mude em todos de uma vez", escolhi
`alternativa equivalente`.
