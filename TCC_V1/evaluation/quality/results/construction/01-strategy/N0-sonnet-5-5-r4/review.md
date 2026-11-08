# Revisão: construção, cenário 01

## Visão geral

O projeto tem três pacotes. `br.com.marealta.web` guarda só a classe `Api`, com as rotas do Javalin e a conversão de e para JSON. `br.com.marealta.plan` guarda os cinco planos de tarifa, cada um numa classe que implementa `RatePlan`. No pacote raiz `br.com.marealta` fica todo o resto: a montagem (`App`), o pool e o esquema (`Db`), o acesso JDBC (`Repository`), os serviços (`ReservationService`, `ReportService`) e os tipos do domínio. Uma requisição segue este caminho: a rota em `web/Api.java:33-43` lê o corpo, monta um `CreateCommand` e chama o serviço. O serviço abre uma transação com `db.tx` (`Db.java:28-42`), trava a linha do quarto com `FOR UPDATE` (`ReservationService.java:40`, `Repository.java:26`), pede o preço ao plano (`ReservationService.java:45-48`) e grava pelo `Repository`. Os dados ficam no PostgreSQL, em três tabelas que `Db.migrate` cria por `CREATE TABLE IF NOT EXISTS` (`Db.java:59-114`). O sistema funciona: as 72 conferências do roteiro passaram, inclusive as reservas simultâneas. Nada aqui quebra o sistema nem arrisca dado. Os pontos fracos são três: os testes cobrem só a precificação (19% das linhas), o esquema é criado no código e não por migrações, e os pacotes formam ciclos.

## Arquitetura

**Números:** 3 pacotes (`br.com.marealta` com 16 classes e 618 linhas, `plan` com 9 classes e 204 linhas, `web` com 1 classe e 206 linhas), 5 dependências entre pacotes e 2 ciclos: raiz ↔ `web` e raiz ↔ `plan` ↔ `web`. O Javalin aparece na raiz (por causa do `App`) e em `web`. O JDBC só aparece na raiz. O pacote `plan` não usa nenhuma biblioteca.
**Nota:** com problemas

As camadas existem como classes:
- **A borda HTTP é fina.** Cada rota de `web/Api.java:33-43` só lê, chama o serviço e escreve. A validação de entrada fica em `parseCreate` (`web/Api.java:58-86`), que é o lugar certo para ela.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` e `ReportService` só chamam o `Repository`. Todo o SQL está em `Repository.java` e em `Db.java`.
- **A montagem fica num lugar só**, em `App.java:8-18`.

Os pacotes, porém, não acompanham essas camadas:
- **Ciclo raiz ↔ `web`.** O pacote raiz mistura a montagem, os serviços, o acesso a dados e o domínio. Como o `App` mora nele e importa `web.Api` (`App.java:3`), o ciclo aparece.
- **Ciclo raiz ↔ `plan`.** Os planos dependem de `Money`, `Room`, `RoomType` e `ApiException`, todos do pacote raiz (`plan/CorporatePlan.java:3-4`, `plan/WeekendPackagePlan.java:3-5`). O serviço da raiz, por sua vez, depende dos planos.
- **Os planos conhecem o HTTP.** Eles lançam `ApiException`, que carrega o código HTTP (`ApiException.java:17`, `plan/CorporatePlan.java:22`). A regra de negócio sabe que a recusa vira um 400.
- **Os serviços manipulam tipos JDBC.** Eles recebem `java.sql.Connection` e `SQLException` nos lambdas de transação (`ReservationService.java:9-10`, `ReservationService.java:114`). Não montam SQL, mas conhecem esses tipos.

Nenhum desses pontos mistura regra com rota. Ainda assim, são dois ciclos, e o pacote raiz faz de tudo.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura e nenhum ficou fora. São uma interface (`plan/RatePlan.java:9-18`) e cinco implementações, escolhidas num único ponto (`plan/RatePlans.java:8-19`). Fora dos planos sobrou uma regra pequena, guardar o `companyCode` só no convênio (`ReservationService.java:52-54`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern é um Strategy completo.** Cada plano tem num lugar só o preço, a validação e a devolução:
- `StandardPlan.java:8-12` e `NonRefundablePlan.java:12-17`, os dois do ponto de partida;
- `CorporatePlan.java:18-28`, com o mapa de descontos e a recusa sem código;
- `WeekendPackagePlan.java:21-28`, com sexta e duas noites, o preço fechado por tipo de quarto e a devolução da metade;
- `LongStayPlan.java:15-23`, com o mínimo de 7 noites e 10%.

O serviço pede o preço (`ReservationService.java:48`) e a devolução (`ReservationService.java:82-83`) sem nomear o plano. Não há `switch` nem `if` de comportamento por plano em lugar nenhum. `PricingContext` (`plan/PricingContext.java:11-36`) junta a temporada e a soma das noites, e os planos reaproveitam esse cálculo sem duplicá-lo. Um plano novo é uma classe e uma linha no registro.

**A única coisa de plano fora do plano** é `"CORPORATE".equals(plan.code())` em `ReservationService.java:52`, usada para decidir se o código da empresa é gravado. A referência diz que isso não muda a classe; o detalhe vai para as Dúvidas.

**Não há exagero.** `Quote` e `PricingContext` são records simples e necessários. `RatePlans` é uma lista imutável. Não há fábrica nem camada que só repassa.

**O resto do código tem defeitos pequenos e localizados:**
- **O plano é texto solto.** `Reservation.ratePlan` é `String` (`Reservation.java:15`), e o canal da mensagem também (`ReservationService.java:71`, `"EMAIL"`). Por isso o serviço precisa procurar o plano de novo pelo código ao cancelar, com um `orElseThrow()` sem mensagem (`ReservationService.java:82`). Um código desconhecido no banco viraria um 500 genérico.
- **O plano desconhecido é validado duas vezes**, em `web/Api.java:80` e em `ReservationService.java:45-46`. Como a validação da `Api` vem antes, um pedido com plano desconhecido e quarto inexistente recebe 400 em vez de 404.
- **A `Reservation` é montada com 17 argumentos posicionais** (`ReservationService.java:53-56`), fáceis de trocar sem perceber.
- **`Repository.update` repassa campos que não mudam.** Ele recebe a situação, o pago e a devolução todas as vezes (`Repository.java:90`), e quem chama repete `r.amountPaid()` e `r.refund()` (`ReservationService.java:92`, `ReservationService.java:100`).

O que está bem:
- o dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:15-26`);
- as datas em `LocalDate`;
- as situações num `enum` com `isPaid()` (`ReservationStatus.java:7-9`);
- nenhum estado mutável compartilhado;
- nenhuma exceção engolida: o rollback relança (`Db.java:35-38`), e o handler genérico registra o erro e responde 500 (`web/Api.java:51-54`).

## Testes

**Números:** 8 testes rodaram, nenhum falhou ou foi pulado. São 8 métodos de teste num único arquivo. A cobertura é de 19,0% das linhas (85 de 447) e 21,3% dos ramos (26 de 122). Por pacote:

| pacote | linhas | ramos |
|---|---|---|
| `plan` | 52 de 55 | 23 de 24 |
| raiz | 33 de 269 | 3 de 60 |
| `web` | 0 de 123 | 0 de 38 |

Não há teste pela API nem contra o banco.
**Nota:** com problemas

**O que está testado, com valores reais, está bem testado.** `PlansTest` confere:
- o exemplo de alta temporada, R$ 599,76 (`PlansTest.java:20-23`);
- os limites da temporada, inclusive 14/12, 15/12 e 29/02 (`PlansTest.java:25-30`);
- os três exemplos do texto: Tecnosul R$ 93,58 e R$ 686,22, o pacote R$ 820,00 com devolução de R$ 410,00, e a estadia longa R$ 174,93 e R$ 1.574,37 (`PlansTest.java:39-63`);
- as recusas do pacote e da estadia longa (`PlansTest.java:53-54`, `PlansTest.java:62`);
- o arredondamento meio para o par (`PlansTest.java:65-67`);
- o texto do comprovante, linha a linha (`PlansTest.java:69-81`).

**Fica de fora todo o resto do sistema:**
- a máquina de situações (confirmar, check-in, check-out e as recusas 409);
- o cancelamento pelo serviço;
- o choque de datas (409);
- a capacidade (422);
- os códigos de erro da API;
- o e-mail de confirmação;
- os dois relatórios CSV;
- a persistência.

A raiz tem 3 de 60 ramos cobertos, e o pacote `web` tem 0.

**A pergunta da mutação, na parte nova:**
- Trocar 0.18 (Agrovale) ou 0.25 (Farmavida) em `CorporatePlan.java:13-14` não quebra nenhum teste, porque só a Tecnosul é testada.
- Trocar `refund` de `CorporatePlan.java:28` ou de `LongStayPlan.java:23` para devolver zero, ou a metade, também não quebra nenhum teste. Só as devoluções do pacote e do não reembolsável são conferidas.
- Trocar o preço da suíte em `WeekendPackagePlan.java:16` passa sem ser notado.
- Uma estadia longa com noites de alta temporada não tem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice. Nenhum CHECK, UNIQUE ou EXCLUDE. O esquema é criado por `CREATE TABLE` no código, sem migrações. As consultas montadas com `+` só concatenam constantes.
**Nota:** com problemas

**O que está certo:**
- **Os tipos e as chaves.** Dinheiro com duas casas, datas em `date`, `NOT NULL` no que é obrigatório e a reserva ligada ao quarto (`Db.java:70-89`).
- **SQL com parâmetros.** As concatenações de `Repository.java:82` e `Repository.java:106-113` juntam só a lista constante de colunas e o `FOR UPDATE`.
- **As conexões.** Usam pool (HikariCP, `Db.java:18-26`) e são devolvidas por try-with-resources.
- **As transações.** Cada operação roda numa transação (`Db.java:28-42`). A confirmação grava a reserva e a mensagem na mesma transação (`ReservationService.java:66-74`).
- **A reserva dupla.** É barrada pela trava `FOR UPDATE` na linha do quarto antes de procurar o choque de datas (`ReservationService.java:40`, `Repository.java:26`, `Repository.java:40-50`). A trava vale entre processos, não só dentro de um, e o roteiro confirmou isso nas 10 rodadas simultâneas.
- **A subida com várias instâncias.** Ela é serializada por um `pg_advisory_xact_lock` (`Db.java:63`).

**Os defeitos:**
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro de `Db.migrate` (`Db.java:64-96`), sem migrações versionadas. A primeira coluna nova numa tabela que já existe não vai ser criada num banco em uso.
- **O banco não protege nenhuma regra por conta própria.** Não há CHECK para `check_out > check_in`, para `guests >= 1` nem para os valores de `status`, `rate_plan` e `type` (`Db.java:77-86`). Também não há restrição de exclusão contra datas sobrepostas. A regra da reserva dupla depende só de todo código passar pela trava.

## Comportamento

**Números:** 72 de 72 conferências passaram. Por parte:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 01 | 21 de 21 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- **Os exemplos do texto batem**: R$ 599,76, Tecnosul R$ 686,22, o pacote R$ 820,00 com R$ 410,00 de volta, e a estadia longa R$ 1.574,37.
- **Os outros convênios batem**: Agrovale 18% e Farmavida 25%.
- **O pacote** mantém o preço em julho, cobra R$ 520,00 no standard e R$ 1.200,00 na suíte, e recusa entrada fora da sexta ou mais de duas noites.
- **A estadia longa** aplica a temporada nas noites de julho e recusa menos de 7 noites.
- **As devoluções** estão certas em todos os planos.
- **As recusas** voltam com os códigos combinados.
- **O e-mail e os relatórios** saem como pedido.
- **Os dados** continuam lá depois de reiniciar.
- **Duas reservas simultâneas** do mesmo quarto nunca passam juntas.

Uma leitura fora do roteiro: um pedido com plano desconhecido e quarto inexistente recebe 400, e não 404 (`web/Api.java:80`). O texto não fecha essa ordem, então não pesa na nota.

## Dúvidas

- A regra de guardar o `companyCode` só no convênio está no serviço, com o nome do plano escrito à mão (`ReservationService.java:52-54`), e não no plano. A referência do Strategy diz que essa regra pequena, que não é de preço nem de devolução, não muda a classe. Por isso a classe ficou em `acerto`. Mas é o único ponto em que o serviço ainda pergunta qual é o plano, e um plano novo que precise guardar um dado próprio (a data de nascimento da melhor idade, por exemplo) passaria por aqui.
- A nota do design ficou em `com problemas` por defeitos pequenos e localizados: o plano como `String`, a validação duplicada e o construtor de 17 argumentos. Um revisor mais tolerante poderia dar `bom`.
