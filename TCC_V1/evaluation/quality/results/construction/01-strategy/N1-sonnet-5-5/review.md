# Revisão: construção, cenário 01

## Visão geral

Projeto Maven com seis pacotes. A ordem de montagem está em `Main.java:13-20`. A borda HTTP fica em `web` (`Api`, `RequestParser` e `Views`) e só lê o pedido, chama o serviço e escreve a resposta. Os serviços ficam em `service`: `ReservationService`, `ReportService`, o comprovante e o e-mail. As regras ficam em `domain`: a máquina de estados da reserva, a temporada e o dinheiro. Os planos de tarifa ficam em `plan`, um Strategy com uma classe por plano. O acesso ao PostgreSQL fica em `infra`, com HikariCP, transação explícita e SQL com parâmetros.

O caminho de `POST /reservations` é este: `Api.java:42` → `RequestParser.reservation` → `ReservationService.create`. O serviço pede ao plano o preço (`plan.quote`), e depois `ReservationRepository` tranca o quarto com um advisory lock e insere a reserva. Os quartos e os planos ficam no código (`Room.java:8-13`, `RatePlans.java:9-14`), e as reservas e as mensagens ficam no banco.

O roteiro de comportamento passou inteiro: 72 de 72. Não há defeito sério. Os pontos fracos são todos pequenos:
- o esquema é criado no código, sem migrações e sem nenhuma restrição além das chaves primárias;
- há um ciclo pequeno entre `domain` e `plan`;
- a exceção do domínio carrega o status HTTP.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, 1 ciclo (`domain` ↔ `plan`). Javalin só em `web` e no `Main`; JDBC em `infra` e em `service`. A maior classe é `ReservationRepository`, com 150 linhas.
**Nota:** com problemas

Os pontos fortes:
- **Borda fina:** cada rota é uma linha que delega ao serviço (`web/Api.java:40-64`). A validação de formato fica em `web/RequestParser.java`, e a forma do JSON em `web/Views.java`.
- **SQL isolado:** todo o SQL está em `infra/ReservationRepository.java` e `infra/MessageRepository.java`.
- **Montagem num lugar só:** tudo é criado e ligado em `Main.java:13-17`.
- **Regras nos objetos certos:** a máquina de estados está em `domain/Reservation.java:51-71`, e o preço e a devolução estão nos planos.

O ciclo `domain` ↔ `plan` vem de `domain/Reservation.java:3`. A reserva recebe um `RatePlan` em `pending` (`:36`, só para ler `plan.code()`) e em `cancel(RatePlan plan)` (`:66`). Ao mesmo tempo, os planos dependem de `Stay`, `Pricing`, `Money` e `ApiException` do domínio. O ciclo é pequeno e entre vizinhos, mas é um ciclo que o projeto criou.

Há mais duas misturas localizadas:
- **HTTP no domínio:** `domain/ApiException.java:8-36` guarda no domínio o status HTTP (400, 404, 409, 422). As regras e os planos lançam códigos HTTP.
- **Transação no serviço:** o serviço conhece `java.sql.Connection` e `SQLException` (`service/ReservationService.java:13-14,20-22`), porque é ele quem conduz a transação e passa a conexão aos repositórios. Não há SQL nele, mas a camada de regras depende do JDBC.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`STANDARD`, `NON_REFUNDABLE`, `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY`) e 0 fora dela. Não há nenhum `switch` nem `if` sobre o plano fora de `plan/`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface `plan/RatePlan.java:11-20` tem três métodos:
- `code()`;
- `quote(Stay)`, que dá o preço e recusa quem não pode usar o plano;
- `refund(amountPaid)`, que dá a devolução.

Há uma implementação por plano, inclusive os dois que já existiam (`StandardPlan` e `NonRefundablePlan`), e um ponto de seleção só: a lista em `plan/RatePlans.java:9-14`. Cada plano guarda o próprio comportamento:
- **o convênio:** a tabela de empresas e a recusa sem código ficam em `plan/CorporatePlan.java:11-28`;
- **o pacote:** sexta a domingo, preço fechado por tipo e metade na devolução ficam em `plan/WeekendPackagePlan.java:16-38`;
- **a estadia longa:** o mínimo de 7 noites fica em `plan/LongStayPlan.java:17-22`.

Quem usa a estrutura não nomeia o plano:
- o preço vem de `plan.quote(stay)` (`service/ReservationService.java:42`);
- a devolução vem de `plan.refund(amountPaid)` (`domain/Reservation.java:70`);
- a linha de desconto do comprovante depende só do valor (`service/Receipt.java:42`).

O código da empresa chega ao plano como parte do contexto `Stay` (`domain/Stay.java:8`). Por isso nenhuma regra de plano fica no serviço.

**O exagero.** A classe abstrata `plan/DiscountPlan.java:9-24` é uma base comum de verdade: quatro planos a usam, e ela evita copiar o cálculo "soma das noites menos percentual". As interfaces funcionais `Db.Work` (`infra/Db.java:13`) e `ReservationService.Operation` (`service/ReservationService.java:20`) existem para receber lambdas. Não há fábrica, builder nem camada que só repassa, então a nota é contenção.

**O resto do código.** É bem tipado:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:20-22`, `domain/Season.java:22-25`, `plan/DiscountPlan.java:17-18`);
- datas em `LocalDate`;
- a situação da reserva num `enum`;
- a reserva é um `record` imutável, com transições que conferem a situação (`domain/Reservation.java:51-81`).

Os erros não são engolidos: `RequestParser` traduz cada falha de formato para 400, e o tratamento genérico registra a pilha (`web/Api.java:34-38`). Há três detalhes pequenos, que não mudam a nota:
- **plano como texto:** a reserva guarda o plano como `String` (`domain/Reservation.java:24`) e o serviço o procura de novo pelo código (`service/ReservationService.java:70`);
- **busca repetida:** `Room.find(...).orElseThrow()` aparece repetido em `service/Receipt.java:29` e em `service/ReportService.java:28`;
- **status HTTP no domínio:** a `ApiException` com status HTTP, já citada na arquitetura.

## Testes

**Números:** rodaram 17 testes, 0 falharam e 0 foram pulados; há 17 métodos escritos (11 em `PricingTest` e 6 em `ApiTest`). A cobertura é de 95,2% das linhas (454/477) e 75,5% dos ramos (71/94). Os pacotes `plan` e `service` têm 100% das linhas.
**Nota:** bom

Os testes conferem as regras com valores:
- **a temporada e o arredondamento:** R$ 599,76 e o início em 15/12 (`PricingTest.java:29-36`);
- **o não reembolsável:** R$ 74,97 de desconto (`:39-41`);
- **os três exemplos do financeiro:**
  - R$ 686,22 (`:45`);
  - R$ 820,00 e R$ 410,00 (`:56`, `:68`);
  - R$ 1.574,37 (`:73`);
- **as recusas dos planos:** o convênio sem código ou com código desconhecido (`:50-51`), o pacote fora de sexta a domingo (`:62-63`) e 6 noites na estadia longa (`:74`);
- **as devoluções de cada plano** (`:78-84`).

O `ApiTest` sobe a API de verdade contra o PostgreSQL de `DB_URL`, num schema próprio (`ApiTest.java:130-145`), e confere:
- o ciclo de vida inteiro, com o texto exato do e-mail e o comprovante (`:191-238`);
- a sobreposição de noites e a liberação do quarto ao cancelar (`:241-249`);
- os códigos de erro (`:271-293`);
- os dois CSVs exatos, inclusive o não reembolsável cancelado no financeiro (`:296-315`).

A pergunta da mutação:
- trocar `<` por `<=` no mínimo de noites ou mudar 12% do Tecnosul derruba um teste;
- trocar entre si os 18% da Agrovale e os 25% da Farmavida não derruba nenhum, porque essas duas empresas não são testadas;
- a virada da temporada no fim de fevereiro também não tem teste;
- não há teste das reservas simultâneas nem do reinício.

Há duas sobras sem efeito: `Money.of("1");` em `PricingTest.java:79` e `revenue.replace(pending + ",", pending + ",")` em `ApiTest.java:314`. Por fim, `ApiTest.java:133` usa `assumeTrue`, então sem `DB_URL` os testes de API são pulados em silêncio. Na medição eles rodaram.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), cada uma com chave primária. O dinheiro está todo em `numeric(12,2)`, as datas em `date`, e há `NOT NULL` em 15 das 16 colunas de `reservations` (só `refund` aceita nulo, de propósito). O script acusou isto:
- 0 restrições CHECK, UNIQUE ou EXCLUDE;
- `status`, `rate_plan` e `channel` em `text` sem CHECK;
- o esquema criado por `CREATE TABLE` no código, sem arquivo de migração.

**Nota:** com problemas

O que está certo:
- **o SQL:** todo com parâmetros. Os trechos montados com `+` só concatenam constantes (`COLUMNS` e `PAID_STATUSES`, em `infra/ReservationRepository.java:18-21`).
- **as conexões:** vêm de um pool e são devolvidas com try-with-resources (`infra/Db.java:30-44`).
- **a transação:** a confirmação grava a reserva e o e-mail na mesma transação (`service/ReservationService.java:60-65`).
- **a reserva dupla:** é impedida por `pg_advisory_xact_lock(room)` antes da conferência de sobreposição, dentro da transação (`infra/ReservationRepository.java:24-29`, `service/ReservationService.java:44-50`). A trava vale entre processos, e as transições de situação usam `SELECT ... FOR UPDATE` (`infra/ReservationRepository.java:90-92`).

O que precisa de correção:
- **sem migrações:** o esquema é criado por `CREATE TABLE IF NOT EXISTS` em `infra/Db.java:50-76`, sem migração versionada. O primeiro pedido que acrescentar uma coluna (por exemplo, guardar o `companyCode`) não tem como evoluir um banco que já tem dados.
- **nenhuma restrição:** o banco não protege nenhuma regra. Não há EXCLUDE para noites sobrepostas, nem CHECK em `status`, `rate_plan` e `guests >= 1`. A garantia contra reserva dupla depende de todo escritor passar pela trava do código.
- **sem índice de busca:** não há índice em `room_number`/`check_in`, que são as colunas das consultas de disponibilidade e dos relatórios.
- **quarto sem chave estrangeira:** os quartos não são tabela (`domain/Room.java:8-13`), então `room_number` não tem chave estrangeira. É uma escolha aceitável, já que o texto diz que os quartos quase nunca mudam.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 01, os planos novos: 21/21;
- reservas simultâneas: 2/2.

**Nota:** bom

Tudo o que o texto descreve funciona pela API. Para o dono da pousada, isso quer dizer:
- **os exemplos do financeiro conferem:**
  - Tecnosul: R$ 779,80, R$ 93,58 e R$ 686,22;
  - pacote: R$ 820,00, e R$ 410,00 na devolução;
  - estadia longa: R$ 1.749,30, R$ 174,93 e R$ 1.574,37;
- **os outros convênios estão certos:** Agrovale e Farmavida;
- **o pacote:** mantém o preço fechado em julho e é recusado fora de sexta a domingo;
- **a estadia longa:** cobra a temporada nas noites de temporada e é recusada com 6 noites;
- **o `companyCode` nos outros planos:** é ignorado;
- **o resto do sistema:** a parte comum (e-mail, comprovante, relatórios, devoluções), os dados depois de reiniciar e as reservas simultâneas também passaram.

Nenhuma falha a explicar.

## Dúvidas

O texto não diz se o `companyCode` deve aparecer na reserva devolvida. O projeto não o guarda nem o devolve: ele só vive em `Stay` durante o cálculo (`domain/Stay.java:8`), e a tabela não tem a coluna. O roteiro não confere isso, e não está claro se a recepção ou o contador precisariam saber a empresa depois.
