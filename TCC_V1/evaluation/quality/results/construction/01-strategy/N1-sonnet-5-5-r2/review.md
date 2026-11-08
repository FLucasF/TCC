# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em sete pacotes: `api` (rotas e leitura do corpo), `service` (o serviço de reservas e a interface do repositório), `domain` (reserva, quartos, temporada, dinheiro, comprovante), `plan` (os planos de tarifa), `report` (os CSV), `infra` (Hikari, criação do esquema, repositório JDBC) e `pousada.Main`, que liga as peças. Uma requisição entra numa rota de uma linha (`src/main/java/pousada/api/Api.java:44-54`). O corpo é validado em `ReservationRequest` e passa ao `ReservationService`. O serviço pede o preço ao `RatePlan` escolhido por `RatePlans.byCode`, e a reserva troca de estado ela mesma (`domain/Reservation.java:53-73`). A gravação fica no `PostgresReservationRepository`, com uma trava consultiva por quarto, e a troca de estado só é gravada se o estado ainda for o que se leu (`infra/PostgresReservationRepository.java:40-56,106`). Os dados ficam no PostgreSQL, em duas tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. O Strategy está bem feito para os cinco planos, e o roteiro passou inteiro (72/72). Não há defeito sério. O que pesa: um ciclo pequeno entre `domain` e `plan`; o esquema sem migrações e sem nenhuma restrição ou índice além das chaves primárias; e nenhum teste passa pelo repositório Postgres.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`pousada.domain` ↔ `pousada.plan`). Javalin só em `pousada` e `pousada.api`. JDBC só em `pousada.infra` (e em `pousada`, pelo `DataSource` do `Main`). `domain`, `plan`, `service` e `report` não usam Javalin nem JDBC. Maior classe: `PostgresReservationRepository`, 201 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta, numa linha (`api/Api.java:44-54`). Os erros viram JSON num só lugar (`api/Api.java:56-60`). A validação do corpo está à parte, em `api/ReservationRequest.java:34-48`.
- **As regras não conhecem Javalin nem SQL.** `service/ReservationService.java:28-43` cria a reserva pedindo o preço ao plano, e as trocas de estado estão em `domain/Reservation.java:53-83`.
- **O acesso ao banco está isolado.** A interface `ReservationRepository` fica em `service` (`service/ReservationRepository.java:10`) e a implementação em `infra`, o que dá `infra -> service` e não o contrário.
- **A montagem está num lugar só,** em `Main.java:14-18`.
- **O defeito é o ciclo `domain` ↔ `plan`.** `Reservation` guarda um `RatePlan` e o chama no cancelamento (`domain/Reservation.java:3,17,71`). Os planos, por sua vez, usam `Money`, `AppException`, `Room` e `Season` do `domain` (por exemplo `plan/Stay.java:3-5`, `plan/CorporatePlan.java:3-5`). O cálculo do preço está dividido entre `domain/Season.java:22-25` e `plan/Stay.java:19-25`. É um ciclo pequeno, entre pacotes vizinhos, mas mostra que a fronteira entre "domínio" e "plano" não está decidida.
- **Detalhe menor.** `Reports` lê direto do repositório (`report/Reports.java:15-19`), o que é aceitável para relatórios.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`). Nenhum `switch` ou `if` sobre o plano fora deles. Um só ponto de seleção, um mapa, em `plan/RatePlans.java:13-32`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `plan/RatePlan.java:9-33` reúne as três coisas que cada plano tem:
- quem pode usar (`validate`);
- o preço (`subtotal`, que por padrão é a soma com temporada, e `discount`);
- a devolução (`refund`).

O cálculo é o mesmo para todos, em `quote` (`plan/RatePlan.java:28-32`). Cada plano tem a sua regra dentro da própria classe:
- o código da empresa e a taxa (`plan/CorporatePlan.java:11-32`);
- sexta mais duas noites e o preço fechado por tipo de quarto, trocando o `subtotal` (`plan/WeekendPackagePlan.java:13-34`);
- o mínimo de 7 noites (`plan/LongStayPlan.java:18-23`);
- a devolução zero (`plan/NonRefundablePlan.java:19-21`) e a metade (`plan/WeekendPackagePlan.java:42-44`).

Quem usa a estrutura não pergunta qual é o plano:
- a criação (`service/ReservationService.java:31-33`);
- o cancelamento (`domain/Reservation.java:71`);
- a leitura do banco (`infra/PostgresReservationRepository.java:196`).

Os dois planos que o texto já trazia também viraram classes. Um plano novo é uma classe e uma linha na lista de `RatePlans`.

**O exagero.** Não há estrutura sem uso. A única interface fora do pattern é `ReservationRepository`, e ela tem uma segunda implementação de verdade nos testes (`src/test/java/pousada/InMemoryRepository.java:15`). `Stay` e `Pricing` são registros pequenos que carregam o que o plano precisa (`plan/Stay.java:12`, `plan/Pricing.java:7-11`).

**O resto do código.** Os tipos estão certos:
- `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:12-19`);
- `LocalDate` e `YearMonth` para datas;
- `enum` para situação, tipo de quarto, canal e código de erro, com o status HTTP dentro do código de erro (`domain/ErrorCode.java:3-9`).

As trocas de estado são controladas por `require` (`domain/Reservation.java:75-83`). Não há `static` mutável: o catálogo de quartos e o de planos são listas imutáveis.

Pontos menores, que não mudam a nota:
- o construtor de 16 parâmetros da `Reservation`, repetido em `withId` (`domain/Reservation.java:26-51`);
- um `java.util.function.Function` com o nome completo (`service/ReservationService.java:83`);
- o `catch (Exception)` final que imprime a pilha e devolve 500 (`api/Api.java:57-60`). É um último recurso razoável, não engole a causa.

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados. 9 métodos escritos. Cobertura de linhas 298/456 (65,4%) e de ramos 54/88 (61,4%). Por pacote: `domain` 121/121 linhas, `plan` 51/55, `service` 32/33, `report` 27/27, `infra` 0/130.
**Nota:** com problemas

**O que está testado com valores.** `RatePlanTest` confere os exemplos do texto com os três valores:
- TECNOSUL 779,80 / 93,58 / 686,22 (`src/test/java/pousada/RatePlanTest.java:35`);
- o pacote deluxe 820,00, e a suíte em julho a 1.200,00, sem temporada (`RatePlanTest.java:42-43`);
- a devolução de 410,00 (`RatePlanTest.java:44`);
- a estadia longa 1.749,30 / 174,93 / 1.574,37 (`RatePlanTest.java:51`);
- a alta temporada 599,76 (`RatePlanTest.java:30`);
- o não reembolsável com 15% (`RatePlanTest.java:57`).

Também estão testadas as recusas: convênio sem código ou com código desconhecido, pacote fora de sexta a domingo, estadia de 6 noites (`RatePlanTest.java:36-37,45-46,52`). `ApiTest` sobe o Javalin de verdade sobre um repositório em memória e confere:
- o fluxo inteiro, com o e-mail (`src/test/java/pousada/ApiTest.java:60-87`);
- os códigos de status das recusas (`ApiTest.java:90-100`);
- a sobreposição e a devolução da metade do pacote, 600,00 (`ApiTest.java:103-111`);
- os dois CSV comparados inteiros (`ApiTest.java:114-125`).

**O que ficou sem rede.**
- **O repositório Postgres não é testado** (`infra` 0/130). Ninguém confere a consulta de sobreposição, a trava por quarto, a gravação condicionada ao estado, nem as consultas dos relatórios. O `InMemoryRepository` reimplementa essas regras e nem confere o estado esperado (`InMemoryRepository.java:38-42`). Um erro em `check_in < ? AND check_out > ?` (`infra/PostgresReservationRepository.java:45-46`) passaria.
- **A pergunta da mutação.** Trocar 0.18 da AGROVALE ou 0.25 da FARMAVIDA (`plan/CorporatePlan.java:13-14`) por outro número não quebra nenhum teste, porque só a TECNOSUL tem valor conferido. Trocar a devolução do convênio ou da estadia longa por zero (`plan/CorporatePlan.java:35-37`, `plan/LongStayPlan.java:31-33`) também passaria.
- **Asserções fracas em parte.** Os erros conferem só o status HTTP, sem o código (`ApiTest.java:91-98`). O comprovante do check-out confere só o título (`ApiTest.java:85`).

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), as duas com chave primária. Dinheiro em `numeric(12,2)` nas 5 colunas, datas em `date`, 15 de 16 colunas `NOT NULL` (só `refund` aceita nulo, de propósito). As conferências acusaram:
- o esquema criado por `CREATE TABLE` dentro do Java, com 0 arquivos de migração;
- `status`, `rate_plan` e `channel` como `text` sem `CHECK`;
- nenhuma restrição `UNIQUE`, `EXCLUDE` ou `CHECK`;
- 0 chaves estrangeiras (os quartos ficam no código, não no banco).

Há 2 transações explícitas e um pool Hikari. Os candidatos a "SQL montado com `+`" só concatenam constantes.
**Nota:** com problemas

**O que está certo.**
- Tipos certos e SQL sempre com parâmetros (`infra/PostgresReservationRepository.java:44-46,58-61,105-106`).
- Conexões em try-with-resources sobre um pool (`infra/Database.java:17-27`).
- **A regra do quarto ocupado vale entre processos.** A criação abre uma transação, pega `pg_advisory_xact_lock` pelo número do quarto, confere a sobreposição e insere, tudo junto (`infra/PostgresReservationRepository.java:37-83`). Duas reservas simultâneas não passam juntas, nem com mais de um processo, e o roteiro confirmou isso.
- A troca de estado grava com `WHERE id = ? AND status = ?` e insere a mensagem na mesma transação (`infra/PostgresReservationRepository.java:100-128`). Uma confirmação dupla não gera dois e-mails.
- A criação do esquema é protegida por uma trava consultiva (`infra/Database.java:32`).

**Os defeitos.**
- **O esquema não tem como evoluir.** É `CREATE TABLE IF NOT EXISTS` no código (`infra/Database.java:34-60`). O primeiro pedido que precisar de uma coluna nova numa tabela que já existe não terá caminho.
- **O banco não protege nenhuma regra.** Não há `EXCLUDE` contra sobreposição, nem `CHECK` em `status`, `rate_plan`, `guests > 0` ou `check_out > check_in`. Tudo depende do código.
- **Não há índice em `room_number`/`check_in`,** que as consultas de sobreposição e dos relatórios usam (`infra/PostgresReservationRepository.java:45,157-158,166`).
- **O `companyCode` do convênio não é gravado.** Não há coluna para ele (`infra/Database.java:35-52`). O desconto fica certo porque é gravado já calculado, mas o banco não diz de que empresa é a reserva.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 01: 21/21;
- as reservas ao mesmo tempo: 2/2.

**Nota:** bom

Tudo o que o texto pede funcionou pela API, a partir de um banco vazio:
- os três convênios com os valores certos (686,22, 639,44 e 584,85);
- a recusa do convênio sem código ou com código desconhecido;
- o pacote a preço fechado, inclusive em julho, com metade devolvida (410,00);
- as recusas do pacote fora de sexta a domingo;
- a estadia longa com temporada (1.754,30) e a recusa com 6 noites;
- as devoluções integrais do convênio e da estadia longa;
- o `companyCode` ignorado nos outros planos;
- a parte comum inteira: preços, erros, e-mail, comprovante e relatórios.

Os dados sobrevivem a um reinício, e nas dez rodadas de reservas simultâneas sempre uma foi criada e a outra recusada com 409. Para o dono da pousada, o sistema calcula e cobra como ele descreveu.

Um ponto fora do roteiro, que o texto não fechou: a validação do plano roda antes da capacidade (`service/ReservationService.java:31-34`). Um pedido com gente demais e com uma regra de plano quebrada recebe 400, e não 422.

## Dúvidas

1. A nota de arquitetura depende de quanto pesa um único ciclo pequeno entre `domain` e `plan` num projeto feito do zero. Pela regra da skill, um ciclo novo entre dois pacotes leva a "com problemas", mas o resto da separação está limpo.
2. A ausência de chave estrangeira para quartos vem de uma escolha que o texto permite ("os quartos quase nunca mudam"): o catálogo fica no código, em `domain/RoomCatalog.java:7-12`. Não contei isso como defeito à parte.
