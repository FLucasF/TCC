# Revisão: construção, cenário 09

## Visão geral

O projeto tem dois pacotes: `pousada` (23 classes, 929 linhas), onde ficam juntos a borda HTTP (`Api`), a montagem
(`App`), as regras (`Reservation`, `Pricing`, `RatePlan`, `ReservationService`) e o acesso ao banco (`*Repository`,
`Db`, `Schema`), e `pousada.report` (11 classes, 336 linhas) com os quatro relatórios. Uma requisição entra numa rota
de uma linha em `Api.java:38-55`, que chama o `ReservationService`. O serviço abre uma transação JDBC (`Db.tx`,
`Db.java:28-42`), usa os repositórios com SQL parametrizado e deixa as mudanças de situação para o record imutável
`Reservation` (`Reservation.java:26-44`). Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF
NOT EXISTS` no código (`Schema.java:18-52`). Os relatórios seguem um Template Method limpo (`Report.generate`, final,
`Report.java:27-38`), com os quatro relatórios como subclasses. As 64 conferências do roteiro passaram, inclusive as
reservas simultâneas, e os 7 testes passaram. Não há defeito sério. Os pontos a corrigir são menores: um ciclo entre
os dois pacotes, o esquema sem migrações (com a chave estrangeira sem índice e sem CHECK), uma conexão nova por
transação sem pool, e testes que deixam algumas regras sem conferir.

## Arquitetura

**Números:** 2 pacotes (`pousada` com 23 classes e 929 linhas, `pousada.report` com 11 classes e 336 linhas), 2
dependências entre eles (`pousada -> pousada.report` e `pousada.report -> pousada`), 1 ciclo (entre os dois). O
Javalin só aparece em `pousada`. O JDBC aparece nos dois.
**Nota:** com problemas

Os pontos fortes:
- **A borda HTTP é fina.** Cada rota lê o parâmetro, chama o serviço e escreve a resposta (`Api.java:38-49`). As
  rotas dos relatórios saem de um laço sobre a lista de relatórios (`Api.java:51-55`). A leitura e a validação do
  corpo ficam em `ReservationRequest.parse` (`ReservationRequest.java:12-27`), sem regra de negócio no handler.
- **As regras não conhecem o Javalin nem montam SQL.** As regras da reserva moram no record (`Reservation.java:26-49`),
  o preço em `Pricing` (`Pricing.java:28-37`) e o plano em `RatePlan` (`RatePlan.java:18-24`). O SQL fica todo nos
  repositórios (`ReservationRepository.java`, `RoomRepository.java`, `MessageRepository.java`).
- **A montagem fica num lugar só.** `App.build` (`App.java:16-23`) cria os repositórios, o serviço, a `ReportSource`
  e os relatórios, e entrega tudo à `Api`.

Os pontos fracos:
- **O ciclo de pacotes.** `pousada` usa `pousada.report` só na montagem e na borda (`App.java:3-4`, `Api.java:10`). Já
  `pousada.report` usa o núcleo: `Db`, os repositórios e `Reservation` (`ReportSource.java:3-8`), e também a
  `ApiException` (`Report.java:3`, `Report.java:29,35`). O ciclo vem de a borda e a montagem morarem no mesmo pacote
  que o domínio e os dados. Separá-las desfaria o ciclo.
- **Tudo num pacote só.** As camadas existem como classes, mas não como pacotes: HTTP, regras e dados dividem o mesmo
  pacote `pousada`.
- **O domínio usa um erro com cara de HTTP.** O domínio lança a `ApiException`, que carrega o código HTTP
  (`Reservation.java:67`, com o 409 em `ApiException.java:37-39`).
- **O serviço recebe a conexão.** O `ReservationService` recebe e repassa `java.sql.Connection`
  (`ReservationService.java:3,83-101`), ou seja, a transação JDBC aparece na camada de regras. É uma escolha
  pragmática, mas é um vazamento pequeno.

Nenhuma classe é desproporcional: a maior é `ReservationRepository`, com 135 linhas.

## Design de baixo nível

**Números:** 4 de 4 relatórios (ocupação, financeiro, taxa de turismo, planos) passam pelo método-modelo
`Report.generate`. Nenhum relatório fica fora da estrutura, e a linha de título fica num lugar só
(`ReportRenderer.java:18`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `Report<P>` é uma classe abstrata com o método-modelo `final` `generate` (`Report.java:27-38`). Ele lê
e valida o período, chama o passo abstrato `parse` (`Report.java:40`), depois o passo abstrato `build`
(`Report.java:42`), e entrega o resultado ao `ReportRenderer`. O renderer escreve título, cabeçalho, linhas e total
sempre na mesma ordem (`ReportRenderer.java:16-23`). Cada relatório só preenche o que muda e devolve um `ReportData`
(cabeçalho, linhas, total, `ReportData.java:6`):
- `OccupancyReport.java:20-34`;
- `RevenueReport.java:18-31`;
- `TourismTaxReport.java:20-34`;
- `RatePlanReport.java:21-36`.

Dois passos intermediários tiram do caminho a repetição do formato do período: `DailyReport.java:13-19` e
`MonthlyReport.java:13-19`, ambos com `parse` final. Nenhum relatório escreve CSV, título ou cabeçalho à mão, e nenhum
lugar pergunta "qual relatório é este".

O texto pede duas coisas, e as duas ficam simples:
- **A linha de data no ano que vem** entra só no `ReportRenderer`.
- **Um relatório novo** é uma subclasse mais uma linha em `Reports.all` (`Reports.java:14-20`). A rota sai sozinha
  (`Api.java:51-55`).

A taxa de turismo chega por parâmetro do construtor (`TourismTaxReport.java:14-18`), vinda de uma constante única
(`Reports.java:9`). Isso facilita a troca anual, embora ainda exija recompilar.

**O exagero.** Não há. As classes que poderiam parecer estrutura extra fazem trabalho:
- `DailyReport` tem uma subclasse só, mas é um passo do próprio pattern.
- `ReportSource` concentra as consultas que os relatórios dividem (`ReportSource.java:30-46`).
- `ReportData` é um record de dados.

Não há interfaces com uma implementação só, nem fábricas, nem camadas que só repassam.

**O resto do código.** Os tipos estão certos:
- `BigDecimal`, com `HALF_EVEN` a cada passo (`Money.java:16-18`, `Pricing.java:25,32,36`, `RatePlan.java:19`);
- `LocalDate` e `YearMonth` para datas e meses;
- `enum` para situação, plano, tipo e canal (`ReservationStatus.java`, `RatePlan.java`, `RoomType.java`,
  `Message.java:4-6`).

O plano de tarifa guarda os próprios dados (desconto e devolução, `RatePlan.java:7-24`), e isso evita `if` por plano.
As mudanças de situação ficam no record imutável, com a guarda num lugar só (`Reservation.java:61-68`). Os erros não
são engolidos: o tratador genérico registra a pilha (`Api.java:28-31`), e a transação desfaz e relança
(`Db.java:35-38`).

Os poucos reparos são menores:
- o construtor de 16 argumentos repetido em `withId` e `with` (`Reservation.java:51-59`);
- a ocupação lê quartos e reservas em duas transações separadas (`OccupancyReport.java:22,24`, `ReportSource.java:27,32`);
- o domínio lançando `ApiException`, já citado na arquitetura.

Nenhum desses reparos impediria a aceitação.

## Testes

**Números:** 7 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 7 métodos de teste escritos, 5 de unidade em
`PricingTest` e 2 pela API contra o PostgreSQL em `ApiIntegrationTest`. A cobertura é de 92,6% das linhas (441 de 476)
e de 70,6% dos ramos (72 de 102). Em `pousada.report`, são 109 de 112 linhas e 12 de 18 ramos.
**Nota:** com problemas

**O que os testes conferem com valores:**
- o exemplo do texto, 599,76 (`PricingTest.java:160-166`);
- o desconto não reembolsável com arredondamento, 74,97 e 424,83 (`PricingTest.java:169-174`);
- as bordas da alta temporada (`PricingTest.java:177-184`);
- o meio para o par (`PricingTest.java:187-190`);
- o texto do comprovante igual ao do enunciado (`PricingTest.java:193-207`).

O teste de ponta a ponta sobe o `App.build` real num schema próprio do banco (`ApiIntegrationTest.java:33-46`) e
confere:
- as recusas 409, 422, 400 e 404 (`ApiIntegrationTest.java:88-93`);
- o pago igual ao total (`ApiIntegrationTest.java:97`);
- o e-mail, com assunto, canal e destinatário (`ApiIntegrationTest.java:99-109`);
- o título e o cabeçalho da ocupação, e o total `1/5` e `0/5` (`ApiIntegrationTest.java:111-114`);
- a taxa de turismo com valor, 2 hóspedes × 2 noites = 18,00 (`ApiIntegrationTest.java:116-118`);
- o relatório de planos com o STANDARD zerado antes, e o total 795,40 de uma não reembolsável cancelada
  (`ApiIntegrationTest.java:125-127`);
- o mês fora do formato e o `Content-Type` (`ApiIntegrationTest.java:128-130`).

**A pergunta da mutação.**
- **Pegos pelos testes:** trocar 4,50, trocar a ordem dos planos, ou mudar o formato do título no `ReportRenderer`.
- **Sem rede:**
  - **A devolução no plano STANDARD.** Se `RatePlan.refundFor` (`RatePlan.java:22-24`) devolvesse sempre zero, nenhum
    teste falharia: o único cancelamento com sucesso é de uma não reembolsável (`ApiIntegrationTest.java:120-121`).
  - **O financeiro.** O teste só procura `,CANCELLED,` (`ApiIntegrationTest.java:124`). A ordem (`ORDER BY check_in,
    id`, `ReservationRepository.java:85`), os valores das linhas e o filtro "deixou dinheiro"
    (`ReportSource.java:43-46`) não são conferidos diretamente.
  - **A ocupação.** Nada confere que uma reserva PENDING fica de fora (`ReportSource.java:31-34`).
  - **O comprovante.** A linha "Desconto" e o "1 diária" (`Receipt.java:35,37-39`) não têm teste.

O teste de API pula sozinho se `DB_URL` não vier (`ApiIntegrationTest.java:36`). Nesse caso o `mvn verify` passa
rodando só os 5 testes de unidade. Aqui ele rodou.

A iniciativa é boa e o nível é misturado, mas são poucos métodos que concentram muitas asserções, com regras do texto
sem conferir.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Os 6 campos de dinheiro são
`numeric(12,2)`, as datas são `date`, e há 1 chave estrangeira (`reservations.room_number -> rooms`). O esquema não
tem migrações (0 arquivos de esquema; `CREATE TABLE` no código). As conferências acusaram:
- a chave estrangeira `reservations(room_number)` sem índice;
- as colunas de situação, plano, tipo e canal sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum pool, com 1 `DriverManager.getConnection`.

Os 3 SQL montados com `+` só juntam constantes (`RoomRepository.java:12,15,27,36`, e o `FOR UPDATE` constante em
`ReservationRepository.java:64`).
**Nota:** com problemas

**O que está certo:**
- **Os tipos e as obrigatoriedades.** Dinheiro, datas e `NOT NULL` estão certos (`Schema.java:27-43`). Só `refund`
  aceita nulo, de propósito, porque é null até o cancelamento.
- **O SQL.** Todo ele é parametrizado, e as conexões são fechadas com try-with-resources.
- **As escritas que andam juntas.** Elas ficam numa transação: confirmar e gravar o e-mail, por exemplo, em
  `ReservationService.java:51-58`.
- **A reserva duplicada.** O banco a impede: a criação trava a linha do quarto com `SELECT ... FOR UPDATE`
  (`RoomRepository.java:26-33`, `ReservationService.java:31`) antes de procurar sobreposição
  (`ReservationRepository.java:69-81`), dentro da mesma transação. Essa trava vale entre processos, e as 10 rodadas
  simultâneas do roteiro passaram.
- **A subida de mais de uma instância ao mesmo tempo.** O `Schema.init` usa uma trava consultiva
  (`Schema.java:61`) e `ON CONFLICT DO NOTHING` nos quartos (`Schema.java:77`).

**Os defeitos:**
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Schema.java:20,27,46`) não sabe acrescentar uma coluna.
  O próximo pedido que mexer numa tabela vai exigir migração à mão.
- **A chave estrangeira sem índice.** Falta um índice em `reservations(room_number)`, a coluna de todas as consultas
  de sobreposição.
- **A regra só no código.** Nenhuma restrição protege a regra no próprio banco: não há EXCLUDE para noites
  sobrepostas nem CHECK em `status`, `rate_plan` ou `guests >= 1`.
- **Sem pool.** Cada transação abre uma conexão nova com `DriverManager` (`Db.java:23-25`).
- **Filtro em Java.** Os relatórios mensais trazem todas as reservas do mês e filtram em Java
  (`ReportSource.java:37-46`). Como a consulta é limitada ao mês, o custo é aceitável.

## Comportamento

**Números:** 64 de 64 conferências passaram, nenhuma falhou:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09, os relatórios: 13 de 13;
- as duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o roteiro conferiu funciona como o texto descreve:
- **O preço.** A alta temporada e o arredondamento estão certos (599,76; o desconto de 37,48 meio para o par).
- **As recusas.** Saem com os códigos certos.
- **O ciclo da reserva.** Confirmação, e-mail com comprovante, check-in, check-out e segunda via funcionam.
- **As devoluções.** São as certas para cada plano e situação.
- **Os quatro relatórios.** Começam com a linha de título, e a taxa de turismo e os planos trazem os valores pedidos,
  inclusive o mês vazio e o plano sem reserva.
- **Os dados.** Continuam depois de reiniciar o serviço.
- **As reservas simultâneas.** Duas reservas ao mesmo tempo nunca criam as duas.

Para o dono da pousada, o sistema faz o que ele pediu.

## Dúvidas

- Na arquitetura, o ciclo entre `pousada` e `pousada.report` pesou para `com problemas`. Ele é pequeno e vem só de
  `App`/`Api` (a montagem e a borda) morarem no pacote do domínio, e um revisor menos rígido poderia dar `bom`.
- A taxa de turismo é uma constante no código (`Reports.java:9`). O texto diz que a prefeitura a muda todo ano, e o
  código não decide se isso deveria vir de configuração. Ficou como observação, sem pesar na nota.
