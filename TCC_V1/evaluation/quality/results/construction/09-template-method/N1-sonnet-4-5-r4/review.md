# Revisão: construção, cenário 09

## Visão geral

O projeto é um serviço Javalin montado à mão em `App.java`: os controllers (`api/controller`) leem a requisição e chamam os serviços (`service`, `pricing`) ou os relatórios (`report`); os serviços e os relatórios usam repositórios JDBC (`repository`), que abrem uma conexão nova por chamada (`database/Database.java:20`) num PostgreSQL cujo esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:27-66`). Uma reserva segue o caminho `ReservationController.createReservation` → `ReservationService.createReservation` → `ReservationRepository.findConflicting` e `save`. Os relatórios seguem um Template Method de verdade (`report/ReportGenerator.java:8-31`, com as quatro subclasses). O mais sério está no que sai: os relatórios escrevem o dinheiro com `stripTrailingZeros()` (`27`, `795.4`, `0` em vez de `27.00`, `795.40`, `0.00`), o financeiro e o de planos contam a reserva STANDARD cancelada e reembolsada, que não deixou dinheiro nenhum, o e-mail e o comprovante saem com `R$ 599.76` em vez de `R$ 599,76`, e nada impede duas reservas do mesmo quarto feitas ao mesmo tempo: nas 10 rodadas, as 10 criaram as duas.

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, nenhum ciclo. O Javalin só aparece em `br.com.marealta` e em `api.controller`, e o JDBC só em `database` e `repository`. A maior classe é `ReservationRepository` (270 linhas).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: os controllers não têm SQL nem regra de preço, os serviços não conhecem o Javalin (`service/ReservationService.java:1-10` importa só domínio, pricing e repositórios), o SQL fica nos repositórios e tudo é montado num lugar só (`App.java:37-63`). Há duas misturas localizadas:
- **A regra da segunda via mora no handler.** `api/controller/ReservationController.java:81-83` decide no controller quem pode ter a segunda via e lança uma `IllegalStateException` genérica. A regra está incompleta: deixa passar a reserva cancelada. O controller também monta o comprovante consultando o `RoomRepository` direto (`ReservationController.java:89-108`). Enquanto isso, o texto do comprovante fica em `service/DocumentService.java`, então o mesmo comprovante tem duas montagens em camadas diferentes.
- **Uma dependência criada por dentro.** `service/MessageService.java:29` faz `new DocumentService()` dentro do método, em vez de receber a peça na montagem (o `App.java:49` cria outra instância, que vai para o controller).

O tratamento de erro também esconde falhas: `App.java:122-127` transforma qualquer exceção, inclusive uma falha de banco, em 400 `INVALID_REQUEST`. Não há ciclo, e o tamanho das classes é equilibrado.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlansReport` estendem `ReportGenerator`), e nenhum escreve a própria linha de título. Mesmo assim, 4 de 4 repetem a seleção das reservas em dois passos (`buildDataRows` e `buildTotal`), e 3 de 3 relatórios com dinheiro têm o seu próprio `formatDecimal`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do pattern pedido.** `report/ReportGenerator.java:8-21` é o método-modelo: título, cabeçalho, linhas e total, nessa ordem. Os passos que variam são abstratos (`ReportGenerator.java:28-31`: nome, cabeçalho, linhas, total) e o título é montado só na base (`ReportGenerator.java:24-26`). Os dois relatórios que o texto já trazia e os dois novos são subclasses. Uma linha de rodapé com a data, como o dono anuncia para o ano que vem, se põe num lugar só.

**O que afasta.** `generate` não é `final` (`ReportGenerator.java:8`). "Escolher as reservas" não é um passo do esqueleto, e cada subclasse repete a lista de situações e a consulta duas vezes, uma para as linhas e outra para o total: `OccupancyReport.java:36-42` e `:63-69`, `RevenueReport.java:37-46` e `:68-77`, `TourismTaxReport.java:37-45` e `:68-76`, `RatePlansReport.java:37-46` e `:73-82`. Isso faz duas idas ao banco por relatório, e as linhas e o total podem divergir entre elas. A formatação do dinheiro, que é "o jeito dos relatórios", também ficou em cada subclasse (`RevenueReport.java:92-94`, `TourismTaxReport.java:91-93`, `RatePlansReport.java:97-99`), copiada e errada (`stripTrailingZeros`). Nada disso tira os relatórios do esqueleto, por isso a classe é acerto.

**O exagero.** Não há interface sem segunda implementação, fábrica nem camada que só repassa. Os serviços e repositórios são classes concretas, no tamanho do problema.

**O resto do código.**
- **Duplicação:**
  - o tipo do quarto está escrito à mão num `switch` em `report/OccupancyReport.java:74-81` e em `service/DocumentService.java:42-49`, embora já esteja na tabela `rooms` e em `Room.getType()`;
  - `calculateNetAmount` aparece duas vezes (`RevenueReport.java:86-90`, `RatePlansReport.java:91-95`);
  - o total de quartos é uma constante (`OccupancyReport.java:14`) em vez de vir do banco.
- **Formatação do dinheiro:** `service/DocumentService.java:62-63` formata o valor pelo locale padrão e troca a vírgula por ponto, o que dá `R$ 5.799.00` em qualquer locale.
- **Erros:**
  - `catch (Exception)` geral virando 400 (`App.java:122-127`);
  - `IllegalStateException` usada como código de erro (`ReservationController.java:82`);
  - um `roomNumber` ausente vira 404 em vez de 400, porque a validação (`ReservationService.java:135-144`) não confere campos obrigatórios e deixa o `null` chegar ao banco ou ao `NullPointerException`.
- **Estado:** `domain/Reservation.java` é um saco de getters e setters, e as transições de situação são `setStatus` soltos no serviço (`ReservationService.java:74,91,111,124`).

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`pricing/PricingService.java:46-48`), `LocalDate` e `enum` para situação e plano.

## Testes

**Números:** 22 testes rodaram, 0 falhas, 0 erros, 0 pulados; 22 métodos escritos. A cobertura é de 42,6% das linhas (343 de 805) e 46,5% dos ramos (53 de 114). O pacote `report` tem 0 de 143 linhas e 0 de 18 ramos cobertos, e `api.controller` tem 0 de 98.
**Nota:** com problemas

**O que está testado, com valores.**
- O preço, em `pricing/PricingServiceTest.java`: a noite de alta temporada a 299,88 (`:33`), o subtotal de 599,76 (`:54`) e o desconto de 89,96 (`:63`).
- As bordas da temporada, em `pricing/SeasonCalculatorTest.java`.
- O fluxo de situações e as devoluções, contra o banco, em `IntegrationTest.java`: o total de 499,80 (`:66`), o desconto de 116,97 e o total de 662,83 (`:112-113`), e a devolução no STANDARD e zero no NON_REFUNDABLE (`:133`, `:153`).

**A parte nova do cenário não tem nenhum teste.** Os quatro relatórios, a linha de título, a taxa de turismo e o relatório por plano estão com 0% de cobertura. Pela pergunta da mutação:
- trocar 4.50 por 5.40 em `TourismTaxReport.java:15` não derruba nenhum teste;
- tirar `CANCELLED` da lista em `RevenueReport.java:41` também não;
- trocar `check_out >` por `>=` na ocupação (`repository/ReservationRepository.java:173`) também não.

**Testes que congelam ou escondem defeitos.**
- `service/DocumentServiceTest.java:35-37` afirma `"Diárias: R$ 779.80"`, `"Desconto: R$ 116.97"` e `"Total: R$ 662.83"`, enquanto o texto pede `R$ 779,80`: o teste garante o defeito.
- Não há teste das recusas (os códigos de erro), nem nenhum pela API.
- `IntegrationTest.java:32-37` vira "pulado" em silêncio quando não há banco.
- Os testes não limpam o banco: as reservas fixas, como o 101 de 10 a 12/03/2027 em `:53-58`, colidem com `ROOM_UNAVAILABLE` numa segunda execução sobre o mesmo banco.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` (6 de 6 colunas ok) e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 0 CHECK nas colunas de situação, plano, tipo e canal;
- 0 restrição que proteja uma regra;
- 0 pool, 0 transação, 0 `FOR UPDATE`;
- esquema criado no código, sem migração.

**Nota:** ruim

**A regra que o banco devia proteger não tem proteção nenhuma.** Um quarto não pode ter duas reservas na mesma noite, mas não há restrição (`EXCLUDE`), trava, transação nem `synchronized`. `ReservationService.java:42-50` consulta os conflitos numa conexão e `:64` grava em outra, sem nada entre as duas. O roteiro do comportamento mostra a consequência: em 10 de 10 rodadas de dois pedidos simultâneos, as duas reservas foram criadas. É dado errado no banco (o mesmo quarto vendido duas vezes), e por isso a nota é `ruim`.

**Os outros defeitos.**
- O esquema nasce de `CREATE TABLE IF NOT EXISTS` (`database/Database.java:27-66`), que não sabe acrescentar uma coluna numa tabela que já existe: o próximo pedido que mudar o banco vai exigir mexer à mão em quem já roda.
- A chave estrangeira `reservations.room_number` (`Database.java:54`) não tem índice, e é por ela que sai a consulta de conflitos.
- `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK.
- Cada chamada abre uma conexão nova pelo `DriverManager` (`Database.java:20`), sem pool.
- Os relatórios por mês filtram com `EXTRACT(YEAR/MONTH FROM check_in)` (`ReservationRepository.java:210-211`), que não usa índice.

**O que está certo.**
- O SQL usa parâmetros. O único `formatted` só põe `?` gerados por `placeholders` (`ReservationRepository.java:176`, `:214`, `:267-269`), e não dado de fora.
- As conexões são fechadas com try-with-resources.
- Os quartos são semeados uma vez só (`Database.java:68-84`).

## Comportamento

**Números:** 53 de 64 conferências passaram (11 falharam):
- a parte comum: 41 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 8 de 13;
- as duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

A parte comum funciona: preços, arredondamento, recusas, fluxo, devoluções, ocupação e dados que sobrevivem ao reinício. As falhas que pesaram:

- **Duas reservas do mesmo quarto ao mesmo tempo:** em 10 de 10 rodadas, as duas foram criadas. Para o dono, é quarto vendido duas vezes num momento de procura alta (a causa está na coluna do banco).
- **Dinheiro nos relatórios sem 2 casas.** A taxa de turismo sai `27`, `18`, `total,45` e, no mês vazio, `total,0`. O financeiro sai `795.4`, e o de planos, `NON_REFUNDABLE,0,0`. Tudo vem de `stripTrailingZeros()` (`RevenueReport.java:93`, `TourismTaxReport.java:92`, `RatePlansReport.java:98`). O texto pede `27.00`, e o contador recebe um CSV com formato instável.
- **O financeiro e o de planos contam quem não deixou dinheiro.** A STANDARD cancelada e reembolsada aparece no financeiro como `5,202,...,CANCELLED,0` e entra na contagem do relatório de planos (`STANDARD,3` em vez de `2`; `total,4` em vez de `3`). As listas de situação incluem `CANCELLED` sem filtrar pelo valor líquido (`RevenueReport.java:41`, `RatePlansReport.java:41`). A gerência vê uma reserva a mais no plano normal.
- **O e-mail traz o comprovante com o dinheiro no formato errado:** `Total: R$ 599.76` em vez de `R$ 599,76`, e, chamando a API eu mesmo, `R$ 5.799.00` para valores com milhar (`DocumentService.java:62-63`). É o texto que o hóspede recebe.
- **A segunda via de reserva pendente devolve 400 `INVALID_REQUEST` em vez de 409 `OPERATION_NOT_ALLOWED`** (`ReservationController.java:81-83` com `App.java:122-127`). Chamando a API, a segunda via de uma reserva cancelada sai com 200, mas o texto diz que ela só existe para reserva paga.
- **No JSON, os zeros saem sem 2 casas:** `"amountPaid":0` e `"discount":0`, porque o `BigDecimal.ZERO` não tem escala (`domain/Reservation.java:26`, `pricing/PricingService.java:39`).

Uma falha não pesa: "o 201 comporta 3" e "a diária do 201" vieram vazias porque o sistema devolve `number` como texto (`"201"`) e o roteiro procura o número `201`. O texto não fixa o tipo, e o próprio POST aceita os dois. Fora do roteiro, um `roomNumber` ausente devolve 404 `ROOM_NOT_FOUND` em vez de 400 `INVALID_REQUEST`.

## Dúvidas

- **Design, classe:** dei acerto porque os quatro relatórios passam pelo esqueleto e só a base escreve o título. A seleção das reservas repetida em dois passos de cada subclasse, e a formatação copiada em três delas, podiam puxar para malfeito; não puxei porque nenhum relatório refaz a sequência de passos por conta própria.
- **Testes:** a parte nova não tem nenhum teste, e um dos testes garante um defeito; isso fica entre `com problemas` e `ruim`. Dei `com problemas` porque as regras de preço e de cancelamento são conferidas com valores e passam.
- **Comportamento:** dei `com problemas` porque a parte comum passa e 8 de 13 conferências do cenário também. Mas três dos quatro relatórios saem com algum defeito, então a nota fica perto de `ruim`.
