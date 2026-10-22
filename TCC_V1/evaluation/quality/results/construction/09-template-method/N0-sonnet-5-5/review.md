# Revisão: construção, cenário 09

## Visão geral

O projeto (Java 21, Javalin 6.7.0, HikariCP + JDBC puro, ~1.150 linhas em `src/main`) tem seis pacotes: `web` (uma
classe, `App`, com as rotas e o JSON feito à mão), `service` (`ReservationService` com as regras da reserva, `Mailer`
e `ApiException`), `domain` (records e enums: `Reservation`, `Room`, `RoomType`, `RatePlan`, `Status`, `Pricing`,
`Money`, `Receipt`), `report` (os quatro relatórios), `store` (a interface `Store`/`Store.Tx`, com `PostgresStore` e
`InMemoryStore` para os testes) e o `Main`, que lê o ambiente e liga as peças. Uma requisição entra numa rota de
`App` (`web/App.java:62-68`), que lê o corpo e chama o serviço. O serviço abre uma transação em `Store.inTransaction`
(`service/ReservationService.java:34`), trava o quarto com `pg_advisory_xact_lock` (`store/PostgresStore.java:127-135`)
e grava. Os dados ficam em duas tabelas (`reservations`, `messages`), criadas por `CREATE TABLE IF NOT EXISTS` no
código. Os quatro relatórios passam por um mesmo escritor de CSV (`report/ReportWriter.java:12-19`), que escreve
título, cabeçalho, linhas e total. O roteiro do comportamento passou inteiro (64/64), os 19 testes passam com 92,8% de
cobertura de linhas, e o jdeps não mostra ciclos. O ponto mais fraco é o banco: não há migrações versionadas, não há
tabela de quartos nem chave estrangeira, e nenhuma restrição (CHECK, EXCLUDE) protege as regras no próprio banco.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `web` e no `Main`, o JDBC e o pool só em `store`, e `domain` não depende de nenhum outro pacote. A maior classe é
`PostgresStore` (288 linhas), seguida de `App` (178).
**Nota:** bom

- **Borda HTTP fina.** As rotas só convertem a entrada (`web/App.java:94-143`), chamam o serviço e escrevem o JSON
  (`web/App.java:147-177`). Nenhuma regra de preço ou de situação mora nos handlers. As três transições que devolvem
  a reserva usam um só auxiliar (`web/App.java:88-90`), e as rotas dos relatórios saem do registro de tipos
  (`web/App.java:79-84`).
- **Regras sem Javalin e sem SQL.** O preço fica em `domain/Pricing.java:24-34` e as transições em
  `service/ReservationService.java:48-99`; essas classes só conhecem a interface `Store.Tx`. A confirmação e o
  e-mail são gravados na mesma transação (`service/ReservationService.java:49-57`).
- **Acesso a dados isolado.** Todo o SQL está em `store/PostgresStore.java`, atrás de uma interface pequena
  (`store/Store.java:13-44`).
- **Montagem.** O `Main` cria o store e lê a taxa e a porta (`Main.java:13-20`). `App.create` cria o serviço e o
  registro de relatórios (`web/App.java:36-37`). São dois lugares, mas os dois ficam na borda; nenhuma classe de
  regra cria as próprias dependências.
- **Pequenas ressalvas.** O pacote `report` depende de `service` só por causa da `ApiException`
  (`report/MonthlyReport.java:4`, `report/OccupancyReport.java:5`, `report/Reports.java:3`). Essa exceção carrega o
  código HTTP dentro da camada de regras (`service/ApiException.java:16-21`). Os relatórios também leem o `Store`
  diretamente, sem passar pelo serviço (`report/Reports.java:31`). Nada disso cria ciclo, e num sistema deste
  tamanho é aceitável.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo mesmo esqueleto (`ReportWriter.toCsv`), e nenhum escreve título,
cabeçalho ou total por conta própria. 3 dos 4 estendem a base abstrata `MonthlyReport`, que tem um método-modelo
`final` só para validar o período. A ocupação implementa `ReportType` direto.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** A sequência que o texto quer mudar "em todos de uma vez" está num lugar só:
`report/ReportWriter.java:12-19` escreve o título `Pousada Maré Alta,<nome>,<período>`, depois o cabeçalho, as
linhas e o total. Cada relatório entrega só as partes que variam, num record `Report(name, period, header, rows,
total)` (`report/Report.java:9`):

- ocupação: `report/OccupancyReport.java:26-33`;
- financeiro: `report/RevenueReport.java:16-23`;
- taxa de turismo: `report/TourismTaxReport.java:27-35`;
- planos: `report/RatePlanReport.java:18-27`.

Para pôr a linha com a data de emissão no fim, basta uma linha em `ReportWriter`. Isso é a composição que a
referência aceita, "um escritor genérico que recebe as partes que variam, desde que os quatro relatórios passem por
ele". Não é o Template Method clássico: o esqueleto do CSV não é um método `final` numa classe abstrata da qual os
quatro herdam. Há, sim, um método-modelo de verdade em `report/MonthlyReport.java:15-29` (`build` final, que valida
o mês e chama o `build(tx, YearMonth)` abstrato), mas ele cuida da validação do período, não do esqueleto do
relatório. Outros pontos a favor:

- Um relatório novo é uma classe e uma linha no registro (`report/Reports.java:20-26`); a rota sai sozinha
  (`web/App.java:79-84`).
- A taxa por hóspede por noite é um parâmetro do relatório (`report/TourismTaxReport.java:15-17`), configurável por
  ambiente (`Main.java:17`), o que atende ao "a prefeitura muda o valor todo ano".

**Exagero.** Nenhuma estrutura sem uso. A interface `Store` tem duas implementações que são usadas (a de Postgres e a
de memória dos testes, `ApiTest.java:29`), e `ReportType` com `MonthlyReport` é o próprio pattern. Não há fábricas,
builders nem camadas de repasse. Uma sobra pequena: `Reports.csv(String name, ...)` não é chamado por ninguém
(`report/Reports.java:34-38`).

**O resto do código.** Os tipos estão certos e o estado está bem controlado:

- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`, `domain/Pricing.java:21-32`);
- datas em `LocalDate`, situação e plano em `enum`, e o plano carrega o desconto e se é reembolsável
  (`domain/RatePlan.java:5-19`);
- a reserva é um record imutável, com `withX` (`domain/Reservation.java:24-42`);
- a regra "dinheiro que ficou" está num lugar só (`domain/Reservation.java:49-51`), e as transições checam a situação
  por um auxiliar (`service/ReservationService.java:109-112`).

Os defeitos são pequenos e localizados, nada que um revisor exigente bloquearia:

- A validação de data do período está duplicada: `report/OccupancyReport.java:17-25` repete o padrão de
  `report/MonthlyReport.java:17-25`, e `web/App.java:126-134` repete a mesma ideia.
- A soma dos valores retidos aparece em três lugares (`report/RevenueReport.java:20`,
  `report/RatePlanReport.java:22` e `:25`).
- `Reservation.room()` usa `orElseThrow()` sem mensagem (`domain/Reservation.java:45`).
- O `catch (Exception)` geral imprime a pilha e devolve 500 (`web/App.java:42-45`). Não engole o erro, mas também
  não o registra num log.

## Testes

**Números:** 19 testes rodaram, 0 falharam, 0 pulados. São 12 métodos escritos: `ApiTest` (7), que
`PostgresApiTest` repete contra o PostgreSQL de verdade, num schema descartável, e `PricingTest` (5). Cobertura:
92,8% das linhas (462/498) e 80,3% dos ramos (106/132), com `domain` em 73/73 linhas e `service` em 70/71
(`measures/tests.md`).
**Nota:** bom

**As regras pedidas, conferidas com valores:**

- **Preço.** As bordas da alta temporada (`PricingTest.java:15-22`), o exemplo de R$ 599,76 (`PricingTest.java:25-30`),
  a estadia que mistura baixa e alta com o desconto de 15% arredondado (`PricingTest.java:33-39`), o "meio para o
  par" (`PricingTest.java:42-45`) e o formato R$ 1.234,56 (`PricingTest.java:48-52`).
- **Ciclo da reserva.** O texto inteiro do e-mail (`ApiTest.java:95-108`), a linha de desconto e o "1 diária"
  (`ApiTest.java:125`), as devoluções nos três casos (`ApiTest.java:129-145`), os códigos de erro
  (`ApiTest.java:148-165`) e a sobreposição de noites, com o dia de saída livre e a cancelada liberando o quarto
  (`ApiTest.java:168-176`).
- **A parte nova.** `ApiTest.java:179-228` compara o CSV inteiro com a linha de título nos quatro relatórios. A
  ocupação e o financeiro incluem a NON_REFUNDABLE cancelada que continua contando (1193.09). A taxa de turismo
  confere 2 hóspedes × 2 noites × 4,50 = 18,00 e deixa de fora a paga cancelada e a pendente. O relatório de planos
  confere um mês vazio, com os dois planos zerados.

**A pergunta da mutação.** Trocar 4,50 por outro valor, tirar a linha de título, inverter a ordem dos planos ou
incluir as canceladas na taxa: todas essas mudanças derrubam algum teste. Ficam sem rede:

- O valor do NON_REFUNDABLE e o total do relatório de planos com dados: a asserção só usa
  `startsWith(... "NON_REFUNDABLE,1,")` e `contains("\ntotal,2,")` (`ApiTest.java:217-218`). Um erro no valor dessa
  linha passaria.
- Uma reserva CHECKED_OUT na taxa de turismo: a regra `isPaid` aparece no relatório só com CHECKED_IN.
- A taxa com várias reservas, para conferir a ordem e a soma do total.

Há também uma asserção vazia, `assertNotEquals(c, 0)` (`ApiTest.java:227`). A concorrência e o reinício não têm
teste. São lacunas pequenas perto do que está conferido.

## Banco

**Números:** 2 tabelas (`reservations`, 16 colunas, 15 NOT NULL; `messages`, 5 colunas, 5 NOT NULL), as duas com
chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0
UNIQUE/EXCLUDE. `status`, `rate_plan` e `channel` são `text` sem CHECK. O esquema é criado por `CREATE TABLE` no
Java, sem arquivo de migração. Os 4 candidatos a SQL montado com `+` só concatenam constantes
(`measures/database.md`).
**Nota:** com problemas

**O que está certo:**

- **Tipos.** Dinheiro com 2 casas e datas em `date` (`store/PostgresStore.java:47-64`).
- **SQL parametrizado.** Todo o SQL usa `?`; o que entra por concatenação são constantes do código: `COLUMNS`
  (`store/PostgresStore.java:24-25`), o sufixo `" FOR UPDATE"` (`store/PostgresStore.java:181`) e os trechos fixos de
  `startingIn` e `paidOccupying` (`store/PostgresStore.java:214-221`).
- **Conexões.** Pool Hikari (`store/PostgresStore.java:29-37`) e try-with-resources em tudo.
- **Transações.** Cada operação roda numa transação com rollback (`store/PostgresStore.java:90-104`). As transições
  usam `SELECT ... FOR UPDATE` (`store/PostgresStore.java:181`).
- **Reserva duplicada.** Um `pg_advisory_xact_lock` por quarto, tomado antes de verificar a sobreposição
  (`service/ReservationService.java:35-36`, `store/PostgresStore.java:127-135`). Essa trava vale entre processos que
  usam o mesmo banco, e o roteiro confirmou: 10 de 10 rodadas simultâneas com uma só reserva criada.
- **Consultas.** Os relatórios filtram por mês ou por noite no SQL, com índices nas colunas usadas
  (`store/PostgresStore.java:65-66`, `:214-221`).

**Os defeitos:**

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do código
  (`store/PostgresStore.java:46-74`), sem migrações versionadas. O próximo pedido já anunciado, "hóspedes por
  estado", vai precisar de uma coluna nova, e esse mecanismo não sabe acrescentar coluna numa tabela que já existe.
- **O banco não protege nada sozinho.** Não existe tabela de quartos: os quartos vivem só no código
  (`domain/Room.java:8-13`), então `reservations.room_number` não tem chave estrangeira. Nenhum CHECK cobre `status`,
  `rate_plan`, `guests >= 1` ou `check_out > check_in`, e nenhuma restrição EXCLUDE impede a sobreposição de noites.
  A integridade depende só do código.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum do enunciado | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 09 (os relatórios) | 13 de 13 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:

- os preços saem como o texto pede (599,76; o desconto de 37,48 no não reembolsável);
- o e-mail sai com o comprovante;
- as devoluções e as recusas seguem as regras, com os códigos de erro combinados;
- os dados sobrevivem ao reinício;
- duas reservas simultâneas do mesmo quarto nunca passam juntas;
- os quatro relatórios começam com a linha de título e trazem as colunas, a ordem e os totais pedidos, inclusive a
  taxa de turismo e o relatório por plano com o plano sem reservas aparecendo zerado.

## Dúvidas

A classe do pattern ficou entre `acerto` e `alternativa equivalente`. Existe um método-modelo `final` numa classe
abstrata (`report/MonthlyReport.java:15-29`), mas ele cobre só a validação do mês e só 3 dos 4 relatórios. O
esqueleto do CSV, que é o que o texto pede, está num escritor comum que recebe as partes de cada relatório como dados
(`report/ReportWriter.java:12-19`). Por isso a classe é `alternativa equivalente`.
