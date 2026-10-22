# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `web` (uma classe `App` com as rotas do Javalin, a leitura do JSON e a escrita das respostas), `service` (`ReservationService`, `ReportService`, `Receipt`), `plan` (os planos de tarifa, o dinheiro e a temporada), `db` (`Database` com o pool Hikari, o esquema e as transações; `Repository` com o SQL), `model` (records e o enum de situação) e o pacote raiz (`Main` e `ApiException`). Uma requisição entra em `App` (`web/App.java:38`), que valida o corpo e resolve o plano por `RatePlans.find` (`web/App.java:86`). Daí vai para `ReservationService`, que abre uma transação (`service/ReservationService.java:31`), trava a linha do quarto com `FOR UPDATE` (`db/Repository.java:28-30`), pede o preço ao plano (`service/ReservationService.java:34`) e grava pelo `Repository`. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`db/Database.java:10-52`). Os cinco planos de tarifa seguem um Strategy limpo, e o roteiro de comportamento passou inteiro (72 de 72). Os pontos fracos não são graves: os testes só cobrem os planos e o comprovante (20,7% das linhas, nada de serviço, banco ou API); o esquema não tem migrações; e há um pouco de mistura de camadas (SQL dentro do `ReportService`, um ciclo de pacotes por causa da `ApiException`).

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, 3 ciclos pelo `tsort`, todos passando pelo pacote raiz `br.com.marealta` (raiz ↔ web; raiz → service → raiz; raiz → web → service → plan → raiz). O Javalin aparece só em `web` e na raiz (`Main`). O JDBC aparece em `db` e em `service`. Maior classe: `web/App.java`, com 209 linhas.
**Nota:** com problemas

A borda HTTP é fina. Cada rota cabe numa linha, lê a entrada, chama o serviço e escreve a resposta (`web/App.java:37-48`). O serviço não conhece o `Context` do Javalin. As regras de preço, de quem pode usar o plano e de devolução ficam em `plan`, sem Javalin e sem JDBC. A montagem acontece num lugar só (`Main.java:11-16`), e o serviço recebe o `Database` pelo construtor (`service/ReservationService.java:20`).

Os defeitos:

- **SQL fora da camada de dados.** O `ReportService` monta e executa SQL dentro do próprio serviço (`service/ReportService.java:19-23` e `:44-47`) em vez de usar o `Repository`. Ele ainda chama `Repository.rooms` no meio (`service/ReportService.java:35`). O acesso ao banco fica dividido entre dois pacotes.
- **O ciclo de pacotes.** Ele nasce de pôr a `ApiException` no mesmo pacote do `Main` (`ApiException.java:1`). Os planos lançam essa exceção (`plan/CorporatePlan.java:18`, `plan/LongStayPlan.java:13`, `plan/WeekendPackagePlan.java:18`) e o `Main` depende de `web`, `service` e `db`. Com isso, `plan` depende do pacote de montagem. A regra também carrega o status HTTP: `ApiException.invalid` fixa o 400 em `ApiException.java:16`, e é esse 400 que os planos devolvem. O certo seria a regra lançar uma exceção de domínio e a borda HTTP traduzir para o status. É um ciclo pequeno, mas as dependências não apontam só da borda para dentro.
- **Transações nos serviços.** Os serviços controlam a transação recebendo uma `java.sql.Connection` (`service/ReservationService.java:9`, `:111`, `:124`). É uma escolha defensável para manter a trava e a gravação na mesma transação, mas o JDBC vaza para a camada de regras.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`), 0 `switch` ou `if` sobre o plano no cálculo do preço ou no cancelamento. Há uma regra pequena de plano fora da estrutura (`instanceof CorporatePlan` para guardar o código da empresa).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface `RatePlan` tem três operações: `code()`, `quote(QuoteRequest)` (valida quem pode usar e calcula o preço) e `refund(amountPaid)` (`plan/RatePlan.java:10-19`). Há uma implementação por plano, os dois do ponto de partida inclusive. A escolha é feita num único ponto, um mapa por código (`plan/RatePlans.java:10-27`). Cada plano guarda suas próprias regras:

- o convênio, a tabela de empresas e a recusa sem código ou com código desconhecido (`plan/CorporatePlan.java:8-24`);
- o pacote, a regra "sexta a domingo", o preço fechado por tipo de quarto e a devolução de metade (`plan/WeekendPackagePlan.java:9-27`);
- a estadia longa, o mínimo de 7 noites e os 10% (`plan/LongStayPlan.java:7-15`);
- o não reembolsável, a devolução zero (`plan/NonRefundablePlan.java:8`).

O serviço pede o preço ao plano sem perguntar qual é (`service/ReservationService.java:34`). O cancelamento também pede a devolução ao plano (`service/ReservationService.java:78-79`). A parte comum (noite com temporada, soma arredondada, desconto percentual) fica em `AbstractRatePlan` (`plan/AbstractRatePlan.java:11-30`), e cada plano a reaproveita sem duplicação. O único ponto que afasta do ideal é `service/ReservationService.java:42`: `n.plan() instanceof CorporatePlan` decide no serviço se o código da empresa é guardado. A referência diz que uma regra isolada desse tipo não muda a classe; ela vai nas Dúvidas.

**O exagero.** Não há estrutura sobrando. A classe abstrata serve a quatro dos cinco planos. `Quote` e `QuoteRequest` são records pequenos que carregam dados de verdade. Não há fábrica, builder nem camada que só repassa.

**O resto do código.**

- Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em todo passo (`plan/Money.java:10-20`), `LocalDate` e `YearMonth` nas datas, e um enum para a situação da reserva com `isPaid()` (`model/ReservationStatus.java:3-9`).
- O estado é controlado: records imutáveis e o mapa estático de planos preenchido uma vez só, privado (`plan/RatePlans.java:10-21`).
- Os erros são tratados: o `catch (Exception)` geral só existe na borda e registra a causa antes de responder 500 (`web/App.java:51-54`).

Detalhes que não pedem mudança antes de aceitar:

- o tipo do quarto é texto solto, e o preço do pacote é um mapa por `String` (`plan/WeekendPackagePlan.java:9-12`, `model/Room.java:5`);
- o `Reservation` é um record com 17 campos posicionais, montado à mão em `service/ReservationService.java:43-46`;
- o texto do e-mail é concatenado dentro do `confirm` (`service/ReservationService.java:63-68`).

## Testes

**Números:** 6 testes rodaram, 0 falhas, 0 erros, 0 pulados; 6 métodos escritos, num único arquivo (`RatePlanTest`). Cobertura de 89 de 430 linhas (20,7%) e 37 de 132 ramos (28,0%). Por pacote: `plan` tem 68 de 73 linhas; `db` e `web` têm 0; `service` tem 14 de 106. Não há nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe é bom e confere valores. Os testes cobrem:

- a temporada nas bordas, como 15/12, 14/12, 29/02 e 01/03 (`RatePlanTest.java:26-33`);
- o não reembolsável, com valores e devolução zero (`RatePlanTest.java:37-42`);
- o exemplo da Tecnosul, R$ 686,22, e as duas recusas do convênio com status 400 (`RatePlanTest.java:46-52`);
- o pacote: R$ 820,00, devolução de R$ 410,00, preço fechado em julho e as duas recusas de data (`RatePlanTest.java:56-64`);
- a estadia longa: R$ 1.574,37 e a recusa com 6 noites (`RatePlanTest.java:68-73`);
- o texto do comprovante e a formatação do dinheiro (`RatePlanTest.java:77-88`).

Pela pergunta da mutação:

- Trocar o 0.12 da Tecnosul ou o `< 7` da estadia longa faria algum teste falhar.
- Trocar os 18% da Agrovale ou os 25% da Farmavida não faria: só a Tecnosul é testada.
- Mudar a devolução do convênio ou da estadia longa de "tudo" para outra coisa também passaria sem falha, porque `refund` só é conferido no não reembolsável e no pacote.
- A linha "Desconto:" e o "1 diária" do comprovante também não são conferidos.

E fica sem nenhum teste tudo o que não é cálculo: as transições de situação e as recusas 409, o conflito de datas (`db/Repository.java:48-59`), a capacidade (422), a confirmação com o e-mail, os dois relatórios CSV, a persistência e a API inteira (`web` com 0 de 125 linhas). O texto não pedia testes, e a iniciativa cobriu bem a parte nova do cenário. Mas o resto do sistema ficou sem rede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice. As conferências acusaram duas coisas: o esquema é criado por `CREATE TABLE` no código, sem migrações (0 arquivos de esquema), e nenhuma restrição protege uma regra (0 CHECK, 0 UNIQUE ou EXCLUDE; `status`, `rate_plan`, `type` e `channel` são `text` sem CHECK). O SQL com `+` são só constantes. Há pool (Hikari), 1 ponto de transação explícita e 2 `FOR UPDATE`.
**Nota:** com problemas

Os dados estão bem guardados:

- os tipos estão certos (`db/Database.java:11-44`), com `NOT NULL` no que é obrigatório e a reserva ligada ao quarto por chave estrangeira (`db/Database.java:19`);
- todo SQL usa parâmetros, e o único trecho dinâmico é a constante `" FOR UPDATE"` (`db/Repository.java:91`);
- as conexões voltam ao pool por try-with-resources e cada operação roda numa transação com rollback (`db/Database.java:91-105`);
- a confirmação grava a situação e a mensagem na mesma transação (`service/ReservationService.java:57-69`).

A duplicação de reserva é evitada no banco, e não no processo. A criação trava a linha do quarto com `SELECT ... FOR UPDATE` (`db/Repository.java:28-30`) antes de procurar conflito (`db/Repository.java:48-59`), o que serializa pedidos do mesmo quarto mesmo entre várias instâncias. O roteiro de concorrência confirmou isso nas 10 rodadas.

Os defeitos:

- **O esquema não evolui.** Ele é um bloco de `CREATE TABLE IF NOT EXISTS` executado ao subir (`db/Database.java:10-52`, `:79`). Não há migração versionada: o próximo pedido que acrescentar uma coluna não terá como alterar um banco que já roda.
- **O banco não protege as regras sozinho.** A regra de não sobrepor datas depende só da trava no código, sem `EXCLUDE`. Também não há `CHECK` para `status`, `rate_plan`, `guests >= 1` ou `check_out > check_in`.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. Parte comum: 46 de 46; dados depois de reiniciar: 3 de 3; cenário 01: 21 de 21; duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o roteiro chamou funcionou como o texto descreve:

- o preço com temporada e o arredondamento meio para o par (R$ 599,76; desconto de R$ 37,48);
- todas as recusas com o status e o código certos;
- a confirmação com o e-mail e o comprovante, o check-in, o check-out e a segunda via;
- as devoluções do normal, do não reembolsável e da reserva pendente;
- os dois relatórios CSV, inclusive a não reembolsável cancelada continuando no financeiro;
- os dados preservados depois de reiniciar.

Na parte nova também passou tudo:

- os três convênios (R$ 686,22, R$ 639,44, R$ 584,85) e as recusas sem código ou com código desconhecido;
- o pacote de R$ 820,00, R$ 520,00 e R$ 1.200,00, inclusive em julho, com devolução de R$ 410,00 e as recusas de data;
- a estadia longa de R$ 1.574,37, inclusive com noites de temporada, com a recusa com 6 noites e a devolução integral;
- o `companyCode` ignorado nos outros planos.

As duas reservas simultâneas nunca criaram duplicata. Para o dono da pousada: o sistema faz o que ele pediu, inclusive os três planos novos.

## Dúvidas

- `service/ReservationService.java:42` decide pelo tipo do plano (`instanceof CorporatePlan`) se o `companyCode` é guardado. É uma regra de plano fora do plano, pequena e que não é de preço nem de devolução. Pela referência ela não muda a classe (mantive `acerto`), mas o próximo plano que precise guardar um dado próprio vai repetir esse `instanceof`.
- O preço é calculado antes da conferência de capacidade (`service/ReservationService.java:34-38`). Um pedido que viola ao mesmo tempo a regra do plano e a capacidade recebe 400 e não 422. O texto não define essa ordem, e o roteiro não testa a combinação.
