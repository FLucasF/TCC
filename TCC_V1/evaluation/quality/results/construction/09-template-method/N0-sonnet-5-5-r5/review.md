# Revisão: construção, cenário 09

## Visão geral

O projeto tem cinco pacotes, com 1.318 linhas em `src/main`. `web` tem as rotas do Javalin e a leitura do corpo; `domain` tem as regras (`ReservationService`, `Pricing`, `Money`, os records e os enums); `report` tem os quatro relatórios e o `CsvRenderer`; `store` tem as interfaces `Store`/`Tx`, o `JdbcStore` e um `MemoryStore` para os testes. A raiz (`App`, `Config`) faz a montagem. Uma requisição entra numa rota de uma linha (`web/Routes.java:24-31`). A rota chama o `ReservationService`, que abre uma transação com `store.tx(...)` e trabalha sobre um `Tx`, e o `JdbcTx` executa SQL com parâmetros no PostgreSQL. Os dados ficam em três tabelas, criadas por `CREATE TABLE IF NOT EXISTS` dentro do código (`store/JdbcStore.java:42-58`). Os relatórios seguem um esqueleto único: cada um devolve um `ReportTable` (nome, período, cabeçalho, linhas, total), e só o `CsvRenderer` o escreve. O roteiro de comportamento passou inteiro (64 de 64). Não há nada sério. Os defeitos que encontrei: um ciclo entre `domain` e `store`, o esquema criado no código sem migrações e sem `CHECK`, e o SQL de verdade sem nenhum teste.

## Arquitetura

**Números:** 5 pacotes, 11 dependências entre eles, 1 ciclo (`domain` ↔ `store`). A maior classe é `JdbcStore`, com 248 linhas; a seguinte é `ReservationService`, com 130.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Routes.java:22-33`). A validação do corpo fica em `web/RequestParser.java:17-41`, e o tratamento de erros num lugar só (`web/Routes.java:42-51`).
- **As regras não dependem do Javalin nem do JDBC.** Pela tabela de bibliotecas, `domain` e `report` não usam nenhuma. As regras de preço, de situação da reserva e de devolução estão em `domain/Pricing.java:24-34`, `domain/ReservationService.java:54-111` e `domain/RatePlan.java:5-19`.
- **O acesso ao banco está isolado.** Só `store` usa JDBC (`store/JdbcStore.java`).
- **A montagem fica num lugar só.** É `App.create`/`App.main` (`App.java:26-51`), que recebe o `Store` de fora; os testes o trocam pelo `MemoryStore`.

Os defeitos:

- **O ciclo `domain` ↔ `store`.** As interfaces `Store` e `Tx` ficam em `store` e o serviço de domínio as usa (`domain/ReservationService.java:3-4`). Já `store` importa os records do domínio (`store/JdbcStore.java:3-8`, `store/Tx.java:3-5`). A interface `Tx` deveria morar no domínio, ou os records num pacote à parte.
- **Os relatórios recebem o `Tx` direto da rota, sem passar por um serviço.** Quem abre a transação é a própria rota, `store.tx(tx -> report.build(...))` em `web/Routes.java:37`. Por isso `web` depende de `store`, e `report` depende de `store` (`report/Report.java:3`, `report/Ledger.java:4`).

O ciclo é pequeno, entre pacotes vizinhos, mas o critério conta qualquer ciclo novo entre dois pacotes.

## Design de baixo nível

**Números:** os 4 relatórios passam pelo mesmo esqueleto, e nenhum fica fora: todos estão em `report/Reports.java:11-15` e são escritos por `report/CsvRenderer.java:14-21`. A linha de título é escrita num lugar só (`report/CsvRenderer.java:16`). Nenhum relatório escreve CSV por conta própria.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** Não há classe abstrata com método-modelo. O agente usou a composição que a referência aceita: um escritor genérico (`CsvRenderer.render`, `report/CsvRenderer.java:14-21`) que recebe as partes que variam como um objeto (`report/ReportTable.java:6-7`). O esqueleto (título, cabeçalho, linhas, total) está ali e em mais lugar nenhum. Cada relatório só preenche o que muda:

- a seleção: `report/OccupancyReport.java:24`, `report/RevenueReport.java:20`, `report/TourismTaxReport.java:27`, `report/RatePlanReport.java:22`, com as seleções comuns em `report/Ledger.java:12-24`;
- a linha e o total, nos mesmos arquivos.

Os quatro relatórios passam por ele, os dois da parte comum inclusive. Um relatório novo é uma implementação de `Report` acrescentada em `report/Reports.java:11-15`; as rotas saem do laço em `web/Routes.java:35-40`. A linha com a data de emissão entraria num lugar só (`report/CsvRenderer.java:19`).

O que afasta um pouco do esqueleto ideal:

- a ordem "montar a tabela, depois escrever" fica na rota (`web/Routes.java:37`) e não num método do próprio esqueleto;
- o `ReportTable.total` é uma lista livre de textos, então cada relatório monta o próprio total.

**O exagero.** Não há estrutura sem uso:

- `Store`/`Tx` têm duas implementações de verdade (`store/JdbcStore.java`, `store/MemoryStore.java`);
- `Reports.all` é uma lista;
- a tabela de vigências da taxa (`domain/TourismTax.java:14-30`) responde a um pedido que o texto faz: "a prefeitura muda o valor da taxa todo ano".

**O resto do código** é limpo:

- o dinheiro é `BigDecimal` com meio para o par em cada passo (`domain/Money.java:15-17`, `domain/Pricing.java:21-32`);
- as datas são `LocalDate`;
- as situações e os planos são `enum` com o comportamento dentro: `Status.isPaid` em `domain/Status.java:7-9` e `RatePlan.refundable` em `domain/RatePlan.java:19`;
- `Reservation` é um record imutável com transições nomeadas (`domain/Reservation.java:29-42`);
- os erros são uma exceção de domínio com código e status (`domain/DomainException.java:17-39`).

Restam detalhes que não mudam a nota:

- a taxa de cada reserva é calculada duas vezes (`report/TourismTaxReport.java:30` e `:35`);
- o padrão de data e a sua validação estão copiados em `web/RequestParser.java:13,59-69` e `report/Periods.java:10,15-24`;
- o texto do e-mail é montado dentro de `ReservationService.confirm` (`domain/ReservationService.java:61-66`);
- o `MemoryStore`, que só os testes usam, mora em `src/main` (`store/MemoryStore.java:14`).

## Testes

**Números:** 11 testes rodaram, 0 falhas, 0 erros e 0 pulados; há 11 métodos de teste escritos. Cobertura de 69,7% das linhas (340 de 488) e 70,1% dos ramos (101 de 144). Por pacote: `domain` 145 de 146 linhas, `report` 98 de 102, `web` 58 de 66, `store` 30 de 144.
**Nota:** com problemas

As regras são conferidas com valores exatos:

- a fronteira da alta temporada (`PricingTest.java:21-28`), o exemplo de R$ 599,76 (`PricingTest.java:31-35`), a virada de temporada noite a noite (`PricingTest.java:38-42`) e o meio para o par no desconto (37,48, em `PricingTest.java:45-50`);
- o e-mail inteiro, texto a texto (`ApiTest.java:149-162`);
- as transições recusadas (`ApiTest.java:137-143,167`) e as devoluções (`ApiTest.java:176-193`);
- a parte nova, com o CSV inteiro dos quatro relatórios (`ApiTest.java:224-282`): a linha de título, a NON_REFUNDABLE cancelada que conta, a STANDARD cancelada com devolução que não conta, a pendente fora da taxa de turismo (18,00 + 27,00 = 45,00) e o relatório de planos de um mês vazio com as duas linhas zeradas.

A pergunta da mutação na parte nova: os testes pegariam a troca de 4,50 por outro valor, a troca de `isPaid` por `kept > 0` na taxa de turismo e a omissão do título.

O defeito: todos os testes da API rodam sobre o `MemoryStore` (`ApiTest.java:84`), que reimplementa em Java o que o `JdbcStore` faz em SQL. O código que roda em produção não tem nenhum teste:

- o conflito de noites só entre as não canceladas (`store/JdbcStore.java:115-116`);
- a seleção da ocupação (`store/JdbcStore.java:199-200`);
- a ordem do financeiro (`store/JdbcStore.java:189-190`);
- a trava `FOR UPDATE`, que é o que impede duas reservas ao mesmo tempo (`store/JdbcStore.java:109`).

Daí os 30 de 144 do pacote `store`: uma troca de `<` por `<=` no SQL de conflito passaria por todos os testes. Há também algumas recusas conferidas só pelo status, sem o código do erro: 400, 409 e 422 em `ApiTest.java:197-215`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(12,2)` nas 6 colunas e datas em `date`. Uma chave estrangeira, `reservations.room_number`, que tem índice. Nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`, e as 4 colunas de situação e de tipo estão sem `CHECK`. O esquema vem de `CREATE TABLE` no código, e não há migrações.
**Nota:** com problemas

O que está certo:

- **Os tipos, as chaves e o `NOT NULL`.** Estão certos em `store/JdbcStore.java:42-58`. `refund` aceita nulo de propósito, porque o texto pede `null` antes do cancelamento.
- **O SQL tem parâmetros em tudo.** Os `+` que o script listou só juntam texto constante, como `RESERVATION_COLUMNS` e `FOR UPDATE` (`store/JdbcStore.java:25-27,109,151`).
- **As conexões estão bem cuidadas.** Usam pool Hikari (`App.java:35-41`) e try-with-resources (`store/JdbcStore.java:80,228`).
- **Cada operação é uma transação com rollback** (`store/JdbcStore.java:79-93`). A confirmação grava a reserva e a mensagem na mesma transação (`domain/ReservationService.java:59-61`).
- **A regra de uma reserva por quarto e noite vale entre processos.** A proteção é um `SELECT ... FOR UPDATE` na linha do quarto, que serializa as reservas do mesmo quarto no próprio PostgreSQL (`store/JdbcStore.java:108-109`, chamado em `domain/ReservationService.java:38`), seguido da verificação de conflito (`store/JdbcStore.java:115-116`).

Os defeitos:

- **O esquema não evolui.** É criado por `CREATE TABLE IF NOT EXISTS` (`store/JdbcStore.java:42-58`), e esse comando não sabe acrescentar uma coluna numa tabela que já tem dados. O próximo pedido que mexer no banco vai precisar de migração feita à mão. A trava `pg_advisory_xact_lock` (`store/JdbcStore.java:41`) só evita duas inicializações ao mesmo tempo.
- **O banco não protege nada sozinho.** Não há `CHECK` em `status`, `rate_plan`, `type` e `channel`, nem em `guests >= 1` ou `check_out > check_in`. Também não há uma restrição `EXCLUDE` contra noites sobrepostas: a regra depende de todo código que grava passar pela trava.

## Comportamento

**Números:** 64 de 64 conferências passaram, e nenhuma falhou:

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- os preços, com a temporada e o arredondamento;
- as recusas, com os códigos certos;
- a confirmação, com o e-mail e o comprovante;
- o check-in, o check-out e a segunda via;
- os cancelamentos, com a devolução certa para cada plano;
- os quatro relatórios, com a linha de título.

Na taxa de turismo, só as reservas pagas que começam no mês entram, e o mês sem reservas sai com total 0.00. No relatório de planos, os dois planos aparecem mesmo sem reserva, e a NON_REFUNDABLE cancelada continua contando. Os dados continuam lá depois de reiniciar. Em 10 rodadas de dois pedidos simultâneos para o mesmo quarto, sempre um foi criado e o outro recusado com 409.

## Dúvidas

No design, a estrutura é a composição que a referência aceita: um escritor genérico que recebe um `ReportTable`. Ela não chega a ser a classe abstrata do Template Method. Fiquei entre `alternativa equivalente` e `acerto` e dei a primeira, porque não há método-modelo. O único passo que fica fora do `CsvRenderer`, montar a tabela e depois escrever, está na rota (`web/Routes.java:37`) e é comum aos quatro relatórios, então não é repetição por relatório.
