# Revisão: construção, cenário 01

## Visão geral

O projeto (`projeto/`) tem 8 pacotes: `api` (rotas Javalin, uma classe por recurso, cada uma com o seu `register`
estático), `service` (regras da reserva, relatórios e mensagens), `rateplan` (uma interface `RatePlan` e uma classe por
plano, escolhidas num mapa em `RatePlanFactory`), `repository` (JDBC com `?`), `db` (o pool Hikari num campo
`static` e o esquema criado com `CREATE TABLE IF NOT EXISTS` no código), `domain` e `utils` (dinheiro com
`HALF_EVEN`, temporada, texto do comprovante). Uma requisição passa assim: `ReservationApi` lê o corpo com `org.json`,
chama `ReservationService`, que usa os repositórios e o plano, e responde com `ctx.json(JSONObject)`. O defeito mais
sério é que o `pom.xml` não traz o `jackson-databind`, que o Javalin 6 usa no `ctx.json` (`pom.xml:21-80`). Por isso
**toda resposta JSON do sistema sai como 500**, inclusive as de erro: `GET /rooms`, a criação, a confirmação, o
check-in, o check-out, a segunda via, `/messages` e os 404/409/422. Só os relatórios CSV respondem. Quando o 500 sai,
a escrita já foi feita: as reservas são gravadas no banco, mas o site recebe erro. Além disso: a confirmação exige
um `amountPaid` mandado pelo cliente em vez de usar o total; o cancelamento é aceito depois do check-in; o dinheiro no
comprovante e no e-mail sai como `R$ 779.80`; o desconto do convênio é calculado fora do plano; e nada impede duas
reservas do mesmo quarto ao mesmo tempo (o roteiro deixou 3 reservas duplicadas gravadas no banco).

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O maior arquivo é
`ReservationApi` (269 linhas), seguido de `ReservationService` (186). O pacote `service` usa JDBC.
**Nota:** com problemas

As camadas existem, e as dependências vão da borda para as regras e das regras para os dados, sem ciclo. As regras
(`service`, `rateplan`) não conhecem o Javalin. Há três defeitos:

- **A montagem está espalhada e usa estado global.** Cada classe de rota cria as suas peças em campos `static`:
  `api/ReservationApi.java:22-26` cria os três repositórios, um `MessageService` e o `ReservationService`;
  `api/MessageApi.java:14-15` cria outro `MessageService` e outro `MessageRepository`; `api/ReportApi.java:15-16` e
  `api/RoomApi.java:10` criam mais um `RoomRepository` cada. O acesso ao banco é um singleton estático
  (`db/Database.java:13`, `:41`) que todo repositório chama direto. O `Main` só chama `register`
  (`Main.java:28-31`) e não liga nada.
- **SQL dentro do serviço.** `service/ReportService.java:32-41` e `:76-83` abrem a conexão e montam as consultas dos
  relatórios. Esse SQL deveria estar num repositório.
- **A borda HTTP faz trabalho de regra e de dados.** `ReservationApi` vai direto aos repositórios em `:77`, `:159-160`,
  `:192` e `:208`, e a regra de quem pode ter segunda via fica no handler (`:201-206`). O check-out chama o serviço e
  depois relê a reserva e o quarto (`:157-161`). O JSON do comprovante é montado duas vezes, igual (`:163-175` e
  `:210-222`).

## Design de baixo nível

**Números:** 5 planos, e os 5 têm classe própria com `validate`, `calculatePrice` e `calculateRefund`
(`rateplan/*.java`). Validação e devolução estão inteiras na estrutura. No preço, 4 dos 5 ficaram nela: o desconto do
`CORPORATE` é decidido no serviço por nome do plano (1 caso fora). O laço das noites com a temporada está copiado em 4
classes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Strategy).** A estrutura foi feita para os cinco planos, inclusive os dois que já existiam: interface
`rateplan/RatePlan.java:7-12`, uma classe por plano, seleção num único mapa (`rateplan/RatePlanFactory.java:6-12`).
A validação do pacote (`WeekendPackagePlan.java:18-31`), as 7 noites (`LongStayPlan.java:14-18`), o código da empresa
(`CorporatePlan.java:10-25`) e as devoluções (`WeekendPackagePlan.java:41`, `NonRefundablePlan.java:33`) estão cada uma
no seu plano. O cancelamento só pede ao plano (`service/ReservationService.java:131-132`). O que afasta do acerto: o
preço do convênio não está no plano. `CorporatePlan.calculatePrice` devolve desconto zero (`CorporatePlan.java:37`), e
o serviço pergunta qual é o plano e faz um cast para aplicar o desconto da empresa:
`if ("CORPORATE".equals(ratePlanName)) { CorporatePlan corporatePlan = (CorporatePlan) ratePlan; ... }`
(`service/ReservationService.java:74-79`). É exatamente o "quem usa a estrutura ainda perguntando qual é o caso". O
próximo plano que precise de um dado da reserva para o preço (a melhor idade, a antecedência) vai acrescentar outro
`if` aí. A interface também espalha o `companyCode`, que só um plano usa, pela assinatura de `validate` de todos
(`RatePlan.java:8`). Por isso a classe é **malfeito**, e não acerto.

**Exagero:** contenção. `RatePlanFactory` é o próprio ponto de seleção que o pattern pede. Não há interfaces com uma
implementação só, builders nem camadas que só repassam.

**O resto do código:**
- Duplicação. O laço noite a noite com a temporada aparece igual em `StandardPlan.java:19-23`,
  `NonRefundablePlan.java:21-25`, `CorporatePlan.java:31-35` e `LongStayPlan.java:25-29`. O bloco
  `{error, message}` é repetido em cada handler de `ReservationApi`, e o JSON do comprovante está copiado
  (`:163-175`/`:210-222`). A existência do plano é conferida duas vezes (`ReservationService.java:51` e `:61`).
- Tipos. O plano é guardado como `String` (`domain/Reservation.java:19`). O dinheiro vai para o JSON passando por
  `double` e `String.format` (`ReservationApi.java:257-259`, `RoomApi.java:23`), e isso perde as 2 casas (0.00 vira
  0.0). O valor pago é lido como `double` (`ReservationApi.java:105`). `formatMoney` usa `"R$ %.2f"` no locale da JVM
  (`utils/ComprovantePrinter.java:35-37`): não há separador de milhar e sai ponto em vez de vírgula (conferido: o
  e-mail gravado diz `Diárias: R$ 779.80`).
- Erros. Há 11 `catch (Exception e)` nas rotas. Na criação, qualquer erro vira 400 INVALID_REQUEST
  (`ReservationApi.java:66-71`), inclusive falha de banco. Nas outras rotas, qualquer erro vira 500, inclusive um id
  que não é número (`:76`) e uma data de relatório inválida (`api/ReportApi.java:27`, `:32-35`).
- Outros pontos. Variável sem uso (`comprovante`, `ReservationApi.java:161`). Loggers declarados e nunca usados nos
  repositórios. O total de quartos fixo em `int totalRooms = 5` (`service/ReportService.java:63`). Os defeitos estão
  localizados (rotas e cópias nos planos). Os tipos centrais estão certos (`BigDecimal` com `HALF_EVEN` em
  `utils/Money.java`, `LocalDate`, `enum` para situação e tipo de quarto). Daí `com problemas`, e não `ruim`.

## Testes

**Números:** 7 testes rodaram, 0 falhas, 0 erros, 0 pulados; 7 métodos escritos, num arquivo só
(`RatePlanIT.java`). Cobertura de linhas 86/798 (10,8%) e de ramos 22/160 (13,8%). `api`, `service`, `repository` e
`db` têm 0% (`measures/tests.md`). Nenhum teste pela API nem contra o banco.
**Nota:** ruim

Os testes são de unidade sobre as classes de plano (JUnit 4, rodados pelo failsafe por causa do sufixo `IT`). Eles
conferem com valores três exemplos do texto: o deluxe de 779,80 (`src/test/java/com/pousada/RatePlanIT.java:35`),
o pacote de 820,00 (`:86`) e a estadia longa de 1.749,30/174,93 (`:99-100`). O resto está quase todo ausente:

- O desconto do convênio é calculado **dentro do próprio teste** com `Money.percentage(...)` (`:63`). Assim, o `if`
  do serviço que aplica esse desconto (`ReservationService.java:74-79`) nunca é exercitado. Se alguém apagasse aquele
  `if`, o convênio sairia sem desconto e nenhum teste falharia.
- O não reembolsável só confere `discount > 0` (`:49`): um desconto de 51% também passaria.
- Nenhum teste das devoluções. Trocar a metade do pacote (`WeekendPackagePlan.java:41`) por zero ou pelo total não
  derruba nada.
- Nenhuma recusa do pacote (sábado, três noites) e nenhum convênio sem código. Nenhum teste da alta temporada (todas
  as datas são de março), das situações da reserva, dos códigos de erro, dos relatórios ou do e-mail.
- O mais grave: a suíte passa enquanto todas as respostas JSON da API saem como 500. Não existe nenhum teste que suba
  o serviço.

As dependências do Testcontainers estão no `pom.xml` e não são usadas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`
nas 6 colunas e datas em `date`. A chave estrangeira da reserva para o quarto tem índice. **Acusado:** nenhuma
restrição `CHECK`, `UNIQUE` ou `EXCLUDE`; status, plano, tipo e canal em `varchar` sem `CHECK`; esquema criado por
`CREATE TABLE` no código, sem migração; nenhum `FOR UPDATE`; uma única transação explícita, a da criação do esquema
(`measures/database.md`).
**Nota:** ruim

Os tipos e as chaves estão certos (`db/Database.java:53-89`). O SQL usa parâmetros em todos os repositórios, e as
conexões vêm do pool Hikari em try-with-resources. Os problemas:

- **Nada protege a regra de um quarto por noite.** A criação confere a disponibilidade
  (`repository/ReservationRepository.java:113-131`) e só depois insere (`service/ReservationService.java:56` e `:91`).
  São duas idas ao banco sem transação, sem trava e sem restrição. No roteiro de reservas simultâneas, o banco
  `review_behavior_2` ficou com **3 pares duplicados** (quarto 301, PENDING, ids 14/15, 19/20 e 22/23, noites de 06/02,
  14/02 e 18/02/2030) em 10 rodadas. O HTTP respondeu 500 nos dois pedidos e escondeu isso. É dado errado gravado, não
  um risco teórico. Por isso a nota é `ruim`.
- O esquema não evolui. `CREATE TABLE IF NOT EXISTS` (`db/Database.java:53`, `:60`, `:82`) não sabe acrescentar uma
  coluna a uma tabela que já existe, e não há Flyway nem Liquibase. O texto é dividido por `;` (`:97`) com
  `createStatement` que nunca é fechado (`:100`).
- Não há `CHECK` na situação nem no plano (`db/Database.java:69`, `:75`), nem `guests >= 1` ou `check_out > check_in`.
- O relatório financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)` (`service/ReportService.java:80`). Isso não usa
  o índice de `check_in` (`db/Database.java:93-94`). Pesa pouco neste tamanho.

## Comportamento

**Números:** 6 de 72 conferências passaram (`measures/behavior.md`). Parte comum: 4 de 46. Dados depois de
reiniciar: 1 de 3. Cenário 01: 0 de 21. Reservas simultâneas: 1 de 2.
**Nota:** ruim

O serviço sobe, mas qualquer `ctx.json` falha com "It looks like you don't have an object mapper configured… add the
'jackson-databind' dependency" (`measures/behavior.md`, primeira conferência de dinheiro). Daí os 500 em
`GET /rooms`, na criação, em todas as recusas (400/404/409/422) e nas transições. Para o dono da pousada, o site não
consegue listar quartos nem reservar: cada pedido recebe erro, embora a reserva fique gravada, e o hóspede pode
tentar de novo e gerar outra. As 6 conferências que passaram são o cabeçalho dos CSV, o `refund` nulo (por acaso) e
"nenhuma rodada com as duas criadas", que passou porque as duas responderam 500. No banco, porém, 3 rodadas ficaram
duplicadas (ver Banco).

Olhando o banco do roteiro, os preços do cenário **estão certos** no que foi gravado: TECNOSUL 779,80/93,58/686,22,
AGROVALE 140,36, FARMAVIDA 194,95, pacote 520/820/1.200 (também em julho), estadia longa 1.574,37 e 1.754,30 com
temporada, NON_REFUNDABLE 37,48. O cálculo funciona, mas nada disso chega ao site. Conferi à mão, pela API, outros
defeitos que o roteiro não alcançou por causa dos 500:

- A confirmação sem corpo falha e não confirma. Com `{"amountPaid":1}`, a reserva fica CONFIRMED com `amount_paid`
  1,00 em vez do total (`ReservationApi.java:96-105`, `ReservationService.java:104`). O texto diz que o pago é o
  total.
- O cancelamento depois do check-in é aceito: a reserva ficou CANCELLED com devolução de 1,00
  (`ReservationService.java:125-128` só barra CHECKED_OUT e CANCELLED).
- O e-mail sai com `Diárias: R$ 779.80` e `Total: R$ 779.80`, e não com `R$ 779,80`.
- Uma data inválida no relatório dá 500 em texto, e não 400 INVALID_REQUEST (`api/ReportApi.java:32-35`).
- A segunda via de reserva não paga daria 400 INVALID_REQUEST, e não 409 (`ReservationApi.java:201-206`).
- O financeiro incluiria uma STANDARD cancelada com devolução total como linha de 0.00 (`ReportService.java:81`), e o
  texto pede só as que deixaram dinheiro.

## Dúvidas

- Banco: a coluna da skill chama de `com problemas` a duplicação protegida só dentro do processo. Aqui não há proteção
  nenhuma e as duplicatas foram gravadas de fato, por isso dei `ruim`. Um leitor que siga só a lista de `ruim` da skill
  (ponto flutuante, SQL concatenado, sem chave, migração editada) daria `com problemas`.
- Testes: entre `com problemas` e `ruim`. Os 7 testes passam e conferem três valores do texto, mas cobrem 10,8% das
  linhas, nada da API, do serviço ou das devoluções, e não perceberam a API inteira quebrada. Dei `ruim` por "quase
  ausente".
- Os defeitos de confirmação, cancelamento depois do check-in, formato do dinheiro e relatório foram conferidos à mão
  ou pela leitura. O roteiro não chegou a eles porque tudo antes deles respondeu 500.
