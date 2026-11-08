# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `web` (as rotas do Javalin e a leitura do corpo), `service` (o caso de uso da reserva e o e-mail), `domain` (a reserva, o preço, os planos, o comprovante, os quartos), `report` (os quatro relatórios CSV), `repository` (o JDBC com HikariCP) e a raiz (`Main`, que monta as peças). Uma requisição entra em `web/Api.java:41-49`. A rota chama o `ReservationService`, que aplica a regra no domínio (`Reservation.confirm/cancel/...`, `Pricing.quote`) e grava pelo `ReservationRepository`. A troca de situação acontece dentro de uma transação com `SELECT ... FOR UPDATE` (`repository/PostgresReservationRepository.java:80-94`). As reservas e as mensagens ficam no PostgreSQL, em duas tabelas que o próprio código cria com `CREATE TABLE IF NOT EXISTS` (`repository/Database.java:39-65`). Os quartos ficam fixos no código (`domain/RoomCatalog.java:10-15`). Os relatórios seguem um Template Method limpo (`report/Report.java:30-39`). As 64 conferências de comportamento passaram, e não há ciclo entre pacotes. Os pontos fracos são menores: o esquema não tem migrações nem restrições (sem CHECK e sem chave estrangeira para o quarto), os relatórios trazem a tabela inteira para filtrar em Java, e a camada de banco não tem nenhum teste.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, nenhum ciclo. O Javalin aparece só em `com.pousada` e `com.pousada.web`, e o JDBC só em `com.pousada.repository`. A maior classe é `PostgresReservationRepository`, com 129 linhas.
**Nota:** bom

- A borda HTTP é fina: cada rota é uma linha que lê o id ou o corpo, chama o serviço e escreve o JSON (`web/Api.java:40-49`). A validação do corpo fica separada em `web/RequestParser.java:17-36`. Os erros viram resposta num lugar só (`web/Api.java:51-60`), a partir de um `ErrorCode` que já traz o status HTTP (`domain/ErrorCode.java:3-10`).
- As regras não conhecem nem Javalin nem JDBC. A capacidade e o preço ficam no serviço (`service/ReservationService.java:24-34`). As transições de situação ficam em `domain/Reservation.java:61-85`, o desconto e a devolução no enum do plano (`domain/RatePlan.java:6-35`) e a temporada em `domain/Pricing.java:16-36`.
- O acesso ao banco fica isolado em `repository/`, atrás de interfaces que os testes trocam por versões em memória (`src/test/java/com/pousada/InMemoryReservationRepository.java`).
- A montagem acontece num lugar só, em `Main.java:25-41`, e o valor da taxa de turismo é injetado no relatório (`Main.java:23,38`).
- O único ponto discutível é `report -> repository`: os relatórios buscam as reservas eles mesmos (`report/MonthlyReport.java:40-45`, `report/OccupancyReport.java:53`) em vez de recebê-las de um serviço. Num sistema deste tamanho isso é aceitável e não cria ciclo.

## Design de baixo nível

**Números:** os 4 relatórios (`occupancy`, `revenue`, `tourism-tax`, `rate-plans`) passam pelo mesmo esqueleto, e nenhum fica fora dele. A linha de título é escrita num lugar só (`report/Report.java:34`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** `Report<P>` é uma classe abstrata com o método-modelo `render`, declarado `final` (`report/Report.java:30-39`). Ele faz sempre a mesma sequência: lê o período, escreve o título "Pousada Maré Alta,nome,período", o cabeçalho, as linhas e o total. Os passos que mudam são abstratos: `name`, `parameter`, `parsePeriod`, `periodLabel`, `header` e `body` (`report/Report.java:17-28`).
  - Os quatro relatórios são subclasses, inclusive os dois da parte comum: `OccupancyReport.java:13`, `RevenueReport.java:10`, `TourismTaxReport.java:11` e `RatePlanReport.java:12`.
  - Os três relatórios de mês compartilham uma base intermediária, `MonthlyReport`. Ela cuida do parâmetro `month`, da leitura do mês, da seleção "começam no mês, por entrada e depois por número" e do filtro "deixaram dinheiro" (`report/MonthlyReport.java:20-50`). Assim o financeiro e o relatório de planos contam as mesmas reservas por construção.
  - Um relatório novo (cancelamentos, hóspedes por estado) é só uma subclasse a mais e uma linha em `Main.java:35-39`. A linha de data pedida para o ano que vem entra em `Report.render`, uma vez só, e vale para todos.
  - O valor da taxa vem pelo construtor (`report/TourismTaxReport.java:14-17`), o que responde ao "a prefeitura muda o valor todo ano".
- **O exagero.** Não há estrutura sem necessidade.
  - As duas interfaces de repositório têm uma segunda implementação de verdade, a dos testes.
  - O `ReportRegistry` (`report/ReportRegistry.java:9-19`) é um mapa nome→relatório que serve à rota única `/reports/{name}` (`web/Api.java:49,64-68`). Tem função, não só repassa.
  - Não há fábricas nem builders.
- **O resto do código.**
  - Dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`, `domain/Pricing.java:30-35`). As datas são `LocalDate`, e as situações, os planos e os canais são enums.
  - A máquina de estados é uma tabela "de onde pode sair" por transição (`domain/Reservation.java:61-85`).
  - O plano carrega o seu desconto e a sua devolução, sem `switch` (`domain/RatePlan.java:6-35`).
  - Os defeitos que sobram são pequenos:
    - `parsePeriod` repete o mesmo try/catch em `OccupancyReport.java:33-39` e `MonthlyReport.java:26-32`.
    - A soma de `retainedAmount` aparece em `RevenueReport.java:31` e `RatePlanReport.java:35`.
    - A reserva é mutável (`Reservation.java:22-24`), mas só muda pelos métodos de transição.
    - `RequestParser.java:21` faz `catch (Exception)`, porém só para traduzir o erro em 400.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 pulados, 6 métodos escritos. A cobertura é de 70,4% das linhas (321/456) e 61,8% dos ramos (47/76). Por pacote: `domain` 135/137, `report` 88/88, `service` 35/36 e `repository` 0/113.
**Nota:** com problemas

- **O que é bom.** Os testes passam pela API de verdade (o Javalin numa porta livre) e conferem valores exatos.
  - O preço de alta temporada, 599.76 (`ApiTest.java:53-61`).
  - O e-mail de confirmação inteiro, comparado caractere a caractere (`ApiTest.java:63-84`).
  - O desconto de 112.46 no comprovante do check-out (`ApiTest.java:116`).
  - A devolução zero do NON_REFUNDABLE (`ApiTest.java:102`).
  - Os quatro relatórios com o CSV inteiro: título, cabeçalho, linhas e total (`ApiTest.java:131-159`). Isso inclui a NON_REFUNDABLE cancelada que continua contando no financeiro e nos planos, a reserva pendente fora da taxa e os 27.00 de 2 hóspedes por 3 noites.
- **A pergunta da mutação na parte nova.** Os testes têm rede para estas três mudanças:
  - Trocar 4.50 por outro valor (`Main.java:23`) quebra `ApiTest.java:149`.
  - Tirar o filtro de reserva paga da taxa (`report/TourismTaxReport.java:31`) faz a reserva 3, pendente, aparecer.
  - Trocar `> 0` por `>= 0` em `report/MonthlyReport.java:49` faz a pendente entrar no financeiro.
- **As lacunas:**
  - A camada de banco não tem nenhum teste (0/113 linhas). A regra "um quarto não pode ter duas reservas na mesma noite" está implementada no SQL (`repository/PostgresReservationRepository.java:32-47`), mas o teste exercita uma cópia dela no repositório falso (`src/test/java/com/pousada/InMemoryReservationRepository.java:18-20`). Um erro no `check_in < ? AND check_out > ?` passaria sem ser notado.
  - Os limites da temporada (14 e 15 de dezembro, fim de fevereiro) não são testados.
  - A devolução do plano STANDARD não é testada, nem a de uma reserva pendente.
  - A noite de saída fora da ocupação não é testada (no teste, a reserva que sai em 12/07 está cancelada).
  - O plano sem reserva no mês aparecendo com 0 não é testado (os dois planos têm reserva em `ApiTest.java:156-157`).
  - As recusas em `ApiTest.java:89-95` conferem só o status HTTP, não o código de erro.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), as duas com chave primária. Os 5 campos de dinheiro são `numeric(12,2)` e as datas são `date`. O script acusou: 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE, `status`, `rate_plan` e `channel` em `text` sem CHECK, e o esquema criado no código, sem migração. Os 3 candidatos a SQL montado com `+` são constantes.
**Nota:** com problemas

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do Java (`repository/Database.java:39-65`), sem migrações versionadas. A primeira coluna nova que um pedido trouxer não chega a um banco que já existe. A criação está protegida por uma trava (`Database.java:38`), o que evita corrida entre duas instâncias subindo juntas.
- **Os tipos estão certos.** O dinheiro usa `NUMERIC(12,2)` (`Database.java:51-56`), as datas usam `DATE` e há `NOT NULL` em tudo que é obrigatório. Só `refund` aceita null, de propósito, porque o texto pede `null` antes do cancelamento.
- **Faltam restrições.** Nada no banco impede uma situação ou um plano inválido (`Database.java:49,54`), um número de hóspedes menor que 1 ou uma saída antes da entrada. Os quartos não estão no banco (`domain/RoomCatalog.java:10-15`), então `room_number` não tem chave estrangeira. É uma escolha defensável, porque o texto diz que os quartos quase nunca mudam, mas o banco aceita qualquer número de quarto.
- **A regra da noite dupla funciona entre processos.** Uma trava `pg_advisory_xact_lock` por quarto, dentro da transação, envolve a consulta de sobreposição e o insert (`repository/PostgresReservationRepository.java:31-50`). As transições de situação usam `FOR UPDATE` (`PostgresReservationRepository.java:82,111`). O roteiro confirmou: 10 de 10 rodadas simultâneas terminaram com uma reserva criada e a outra recusada. Mesmo assim, uma restrição `EXCLUDE` no banco protegeria melhor.
- **O código.** O SQL é parametrizado, as conexões vêm de um pool (`Database.java:23-33`) e são devolvidas por try-with-resources, e o rollback acontece no erro (`Database.java:71-85`).
- **A carga.** Os quatro relatórios fazem `SELECT` da tabela inteira e filtram em Java (`PostgresReservationRepository.java:97-108`, usado em `report/MonthlyReport.java:41` e `report/OccupancyReport.java:53`). A tabela cresce sem limite.
- **O e-mail fica fora da transação da confirmação.** A confirmação e a gravação do e-mail acontecem em transações separadas (`service/ReservationService.java:42-44`). Se o insert da mensagem falhar, a reserva fica CONFIRMED sem e-mail registrado.

## Comportamento

**Números:** 64 de 64 conferências passaram e 0 falharam. Por parte: parte comum 46/46, dados depois de reiniciar 3/3, cenário 09 (relatórios) 13/13 e reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- Os preços batem com a temporada e o arredondamento "meio para o par": 599.76, e o desconto de 37.48 numa noite.
- Os códigos de erro e as transições de situação estão certos, e as devoluções também (299.88 no STANDARD, 0.00 no NON_REFUNDABLE e na pendente).
- O e-mail sai com o comprovante, e a segunda via só existe para reserva paga.
- Os quatro relatórios começam com a linha de título e trazem as colunas e os totais pedidos. A taxa de turismo de um mês vazio sai com total 0.00, e o plano sem reserva aparece com 0.
- Os dados continuam lá depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca passam juntas.

Fora do roteiro: um nome de relatório que não existe devolve 404 com o código `NOT_FOUND` (`web/Api.java:65-66`), que o texto não define. É uma escolha razoável.

## Dúvidas

A nota dos testes ficou entre `bom` e `com problemas`. Os testes conferem com valores a parte nova e quase todo o código das regras. Dei `com problemas` porque a regra de não ter duas reservas na mesma noite, implementada em SQL, só é testada numa cópia em memória, e a camada de banco inteira ficou sem teste.
