# Revisão: construção, cenário 09

## Visão geral

O projeto tem 7 pacotes em Java 21 com Javalin, HikariCP e Flyway. `hotel` faz a montagem e a conexão com o banco
(`Hotel.java:22-31`, `Database.java:18-29`). `hotel.web` é a borda HTTP. `hotel.reservations` tem as regras
(`ReservationService`, `PriceCalculator`) e o acesso ao banco das reservas (`ReservationStore`). `hotel.rooms` e
`hotel.messages` têm seus próprios acessos ao banco. `hotel.documents` cuida do comprovante e `hotel.reports` dos
quatro relatórios.

Uma requisição segue este caminho: a rota em `HttpApi.java:72-106` lê os parâmetros, chama o `ReservationService` ou
um relatório, e o serviço grava pelo `ReservationStore` com SQL parametrizado. Os dados ficam em três tabelas criadas
por uma migração do Flyway (`V1__create_tables.sql`). Uma restrição `EXCLUDE` impede reserva dupla no próprio banco.

O roteiro de comportamento passou inteiro (64 de 64) e os 28 testes passam. O pattern pedido foi bem aplicado: uma
classe abstrata `CsvReport` com o esqueleto `final` e os quatro relatórios como subclasses. Não há nada sério. Os
defeitos são pequenos:
- um ciclo entre `hotel.reservations` e `hotel.documents`;
- os relatórios carregam a tabela inteira de reservas para filtrar em Java;
- algumas duplicações pequenas;
- os testes da taxa de turismo usam sempre 1 hóspede, então o fator "por hóspede" fica sem conferência.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A maior classe é `HttpApi` (186 linhas); depois vêm `ReservationStore` (143) e `ReservationService` (138).
**Nota:** com problemas

- **Borda HTTP fina.** Cada rota só converte a entrada, chama a regra e escreve a saída (`HttpApi.java:72-106`). Os
  códigos de erro viram status HTTP num lugar só (`HttpApi.java:110-123`). A validação de presença dos campos fica na
  borda (`HttpApi.java:132-142`). As regras de negócio ficam no serviço: datas, hóspedes, capacidade e
  disponibilidade (`ReservationService.java:28-45`), e as transições e a devolução (`ReservationService.java:57-110`).
- **Regras sem Javalin e sem SQL.** O `ReservationService` não conhece o Javalin e não tem SQL. O JDBC fica em
  `ReservationStore`, `Rooms` e `SentMessages`. O medidor marca JDBC em `hotel.reservations` por causa do
  `ReservationStore`, que mora no mesmo pacote do serviço. Isso é aceitável neste tamanho.
- **Montagem num lugar só.** Tudo é ligado no construtor de `Hotel` (`Hotel.java:22-31`), sem `static` global.
- **O ciclo.** `ReservationService` cria o `ReservationReceipt` (`ReservationService.java:3,64,109,118`), e o
  `ReservationReceipt` lê a `Reservation` (`ReservationReceipt.java:3,24-37`). O comprovante depende da reserva e as
  regras dependem do comprovante. O serviço também monta o texto do e-mail
  (`ReservationService.java:125-131`), que é apresentação dentro da regra. Bastaria o serviço devolver a reserva e
  um componente de documentos/mensagens montar o comprovante e o e-mail para o ciclo sumir.
- **Os relatórios consultam o banco direto.** Eles leem do `ReservationStore` e filtram em Java
  (`OccupancyReport.java:34`, `RevenueReport.java:24`, `TourismTaxReport.java:38`). A seleção fica no relatório, e não
  numa consulta.

É um ciclo pequeno entre dois pacotes vizinhos, o caso que a skill chama de `com problemas`.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`CsvReport.generate`) e nenhum fica fora. A linha de título é escrita num único lugar (`CsvReport.java:18`). Há 5 passos abstratos (`name`, `header`, `select`, `rows`, `total`) e nenhuma interface de implementação única.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `CsvReport<P>` é uma classe abstrata com o método-modelo `final` (`CsvReport.java:15-25`). Ele escreve
o título "Pousada Maré Alta,<nome>,<período>", o cabeçalho, as linhas e o "total,". Os passos variáveis são
abstratos (`CsvReport.java:27-37`). Os quatro relatórios são subclasses e só preenchem os passos:
- `OccupancyReport.java:12`;
- `RevenueReport.java:14`;
- `TourismTaxReport.java:11`;
- `RatePlanReport.java:12`.

Os dois relatórios "antigos" (ocupação e financeiro) também passam pelo esqueleto. Nenhum relatório repete a
sequência de passos ou a linha de título. A mudança anunciada no texto (uma linha final com a data em todos os
relatórios) cabe numa linha do `generate`. O tipo genérico `P` (data ou mês) tem uso real: os períodos são de tipos
diferentes. Isso não é exagero.

Um ponto pequeno afasta o resultado do ideal: `RatePlanReport.select` chama um `static` de uma classe irmã
(`RatePlanReport.java:36` → `RevenueReport.java:23`). Isso garante "as mesmas reservas do financeiro", mas acopla
duas subclasses. O lugar natural seria uma consulta no `ReservationStore` ou um helper na base.

**Exagero.** Não há interfaces de implementação única, fábricas, builders nem camadas que só repassam. A
estrutura é a necessária: contenção.

**O resto do código.** Os tipos estão certos:
- `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:22-30,50-52`);
- `LocalDate`/`YearMonth` para datas;
- `enum` para situação, plano e tipo de quarto;
- `Price` e `Guest` como `record`.

Os defeitos, todos localizados:
- **Duplicações:**
  - O nome da pousada está em duas constantes (`ReservationService.java:12` e `CsvReport.java:13`).
  - A soma de dinheiro com `reduce(BigDecimal.ZERO.setScale(2), ...)` aparece três vezes (`RatePlanReport.java:21`,
    `RevenueReport.java:55-56`, `TourismTaxReport.java:54-55`).
  - A ordenação "data de entrada, depois número" aparece duas vezes (`RevenueReport.java:27`,
    `TourismTaxReport.java:41`).
- **O estado da reserva é mutável sem guarda.** `Reservation.changeStatus` aceita qualquer transição
  (`Reservation.java:102-104`). Quem guarda as transições é o serviço (`ReservationService.java:59,78-79,93,104`), e
  não a própria reserva.
- **A taxa de turismo é uma constante no código** (`TourismTaxReport.java:13`). O dono avisou que a prefeitura muda
  o valor todo ano, e cada mudança exigirá recompilar. Ela também não tem vigência por data: um relatório antigo,
  tirado depois da mudança, sairá com a taxa nova.
- **Erros de leitura do corpo.** `catch (Exception)` em `HttpApi.java:129` engole qualquer exceção ao ler o corpo
  como 400. É aceitável ali, mas largo demais.
- **Busca por linha no relatório de ocupação.** `OccupancyReport.java:44` busca o quarto no banco uma vez para cada
  linha.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos em 4 classes. Cobertura de linhas: 500 de 537 (93,1%). Cobertura de ramos: 89 de 104 (85,6%). `hotel.reports` tem 73/73 linhas e 10/10 ramos.
**Nota:** com problemas

Os testes rodam contra o PostgreSQL de `DB_URL` (`EmptyDatabase.java:14-19`, que esvazia as tabelas e reinicia a
numeração). Eles misturam três níveis:
- unidade (`PriceCalculatorTest`);
- serviço com banco (`ReservationServiceTest`, `ReportsTest`);
- API por HTTP (`HttpApiTest`).

As regras são conferidas com valores:
- **Temporada.** Ela começa em 15/12, termina com fevereiro (inclusive o ano bissexto) e inclui julho
  (`PriceCalculatorTest.java:30-54`).
- **"Meio para o par".** O desconto de 37,485 vira 37,48 (`PriceCalculatorTest.java:56-64`).
- **Devolução por plano e reserva pendente** (`ReservationServiceTest.java:108-122`).
- **Transições proibidas** (`ReservationServiceTest.java:99-146`).
- **Códigos de erro** (`ReservationServiceTest.java:52-62`, `HttpApiTest.java:91-106`).
- **Relatórios.** Os testes de relatório comparam o CSV inteiro, com o título (`ReportsTest.java:56-146`), e incluem
  a cancelada não reembolsável no financeiro (`ReportsTest.java:95-109`).

Na pergunta da mutação sobre a parte nova, porém, há buracos:
- **O fator "por hóspede" da taxa de turismo não é conferido.** Todas as reservas dos testes de relatório têm
  1 hóspede (`ReportsTest.java:33`). Se `taxOf` (`TourismTaxReport.java:21-24`) multiplicasse só pelas noites,
  `tourismTaxIsChargedPerGuestPerNight` (`ReportsTest.java:111-122`) continuaria passando (9,00 = 4,50 × 2 noites).
  O exemplo do próprio texto (2 hóspedes × 3 noites = 27,00) não está nos testes.
- **O relatório por plano é testado só com reservas STANDARD.** A linha NON_REFUNDABLE sai sempre com 0
  (`ReportsTest.java:124-135`). Uma troca na soma da linha NON_REFUNDABLE, ou a inclusão de canceladas não
  reembolsáveis nela, passaria sem teste.
- **As rotas HTTP dos dois relatórios novos não são chamadas por nenhum teste.** `HttpApiTest.java:108-121` cobre só
  a ocupação e o financeiro.
- **O e-mail é conferido só por trechos** (`ReservationServiceTest.java:95-96`). O texto completo, com saudação e
  rodapé, não é comparado.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 1 migração do Flyway. Nenhuma tabela sem chave primária. 6 colunas de dinheiro, todas `numeric(10,2)`. Nenhuma chave estrangeira sem índice. Nenhuma data como texto. As 4 colunas de situação/tipo (`status`, `rate_plan`, `type`, `channel`) estão sem CHECK. 0 transações explícitas e 0 `FOR UPDATE`.
**Nota:** com problemas

Os pontos fortes:
- **Migração versionada.** O esquema vem do Flyway ao subir (`Database.java:28`,
  `V1__create_tables.sql`). Os quartos são semeados pela migração (`V1__create_tables.sql:9-14`).
- **Tipos e chaves.** Dinheiro em `numeric(10,2)` e datas em `date`. A reserva aponta para o quarto por chave
  estrangeira (`V1__create_tables.sql:21`), e o índice GiST da exclusão começa por `room_number`.
- **Restrições de regra.** Há CHECK de datas e de hóspedes (`V1__create_tables.sql:27,36`).
- **Reserva dupla barrada no banco.** A restrição está em `V1__create_tables.sql:38-40`
  (`EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE status <> 'CANCELLED'`).
  A violação 23P01 vira 409 (`ReservationStore.java:55-58`). Isso vale entre processos, e não só pelo
  `synchronized` do serviço.
- **SQL seguro.** Todo SQL usa `?`. O único texto concatenado é a constante `COLUMNS`
  (`ReservationStore.java:18,98,111`). As conexões vêm do pool e são devolvidas com try-with-resources.

Os defeitos:
- **Os relatórios trazem a tabela inteira de reservas.** Os quatro chamam `store.all()`
  (`ReservationStore.java:108-121`) e filtram em Java por data, mês e situação: `OccupancyReport.java:34`,
  `RevenueReport.java:24` (usado também pelo `RatePlanReport`) e `TourismTaxReport.java:38`. A tabela cresce sem
  limite, e cada relatório fica mais lento com o tempo.
- **Confirmação e e-mail não estão numa transação.** A confirmação grava a reserva
  (`ReservationService.java:62-63`) e depois a mensagem (`ReservationService.java:65-68`), em conexões separadas. Se a
  segunda escrita falhar, a reserva fica confirmada sem o e-mail registrado.
- **As transições não são condicionais.** O `UPDATE` não tem condição sobre a situação anterior
  (`ReservationStore.java:67`). A regra só é protegida pelo `synchronized` do processo
  (`ReservationService.java:57,76,91,102`). Com duas instâncias, uma reserva poderia ser confirmada duas vezes.
- **Situação e plano aceitam qualquer texto.** `status` e `rate_plan` não têm CHECK (`V1__create_tables.sql:28,33`).

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 09 (os relatórios): 13 de 13;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o roteiro confere funciona como o texto pede:
- **Preços.** Os preços com temporada e "meio para o par" saem certos (599,76; 37,48; 212,42).
- **Erros.** As recusas trazem os códigos e status certos.
- **Confirmação.** A confirmação envia o e-mail com o comprovante.
- **Ciclo da reserva e devoluções.** Check-in, check-out e as devoluções por plano funcionam.
- **Os quatro relatórios.** Todos saem com a linha de título "Pousada Maré Alta,<nome>,<período>". A taxa de turismo
  e o relatório por plano estão certos, inclusive o plano sem reserva com 0 e o mês vazio com 0,00.
- **Reinício.** Os dados continuam lá depois de reiniciar.
- **Reservas simultâneas.** Nas 10 rodadas de reservas simultâneas, nenhuma deixou passar duas reservas.

Para o dono da pousada, o sistema faz o que foi combinado.

## Dúvidas

- **Nota de Testes.** Ficou entre `com problemas` e `bom`. A cobertura é alta e as regras antigas estão bem
  conferidas, mas a regra central da parte nova (R$ 4,50 *por hóspede* por noite) não tem um teste que falhe se o
  fator hóspedes sumir. Por isso ficou `com problemas`.
- **Nota de Banco.** Também ficou na fronteira. Não há nada que perca ou corrompa dado no uso de um processo só. O
  que puxa para baixo é a leitura da tabela inteira nos relatórios, mais a falta de transação e de escrita
  condicional.
