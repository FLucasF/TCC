# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `api` (um `ApiController` com todas as rotas do Javalin), `service` (reserva, mensagens, relatórios e as exceções de cada erro), `domain` (a `Reservation` com as transições de situação, os `enum` de quarto, situação e plano), `domain.rateplan` (uma interface `RatePlan` com uma classe por plano e uma `AbstractRatePlan` com a temporada e o arredondamento) e `repository` (JDBC com HikariCP e uma classe `Database` estática que cria as tabelas com `CREATE TABLE IF NOT EXISTS`). Uma requisição passa pelo handler, que monta uma `Reservation` a partir de um `Map`, depois pelo `ReservationService`, que valida, pede o preço ao plano e grava pelo repositório. As reservas e as mensagens ficam no PostgreSQL. Os quartos ficam só em memória (`RoomRepository.java:10-16`). As regras de preço estão certas: conferindo direto no banco, o convênio Tecnosul dá 779.80/93.58/686.22, o pacote deluxe 820.00, a estadia longa 1749.30/174.93/1574.37, e as recusas dos planos voltam 400. O problema mais sério é que a API não consegue devolver nenhuma reserva que ainda não foi cancelada. `toReservationResponse` monta a resposta com `Map.ofEntries` (`api/ApiController.java:198`), que não aceita `null`, e o `refund` é `null` até o cancelamento (`api/ApiController.java:214`). Por isso criar, consultar, confirmar e fazer o check-in respondem 500, embora a mudança fique gravada no banco. O site nunca recebe o `id` da reserva que criou. Além disso, `roomNumber` mandado como número também dá 500 (`api/ApiController.java:92`), duas reservas simultâneas do mesmo quarto são as duas gravadas, e o único teste exige Docker e não rodou.

## Arquitetura

**Números:** 6 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `com.pousada` e `com.pousada.api`, e o JDBC só em `com.pousada.repository`. `service`, `domain` e `domain.rateplan` não usam nenhuma biblioteca de infraestrutura. A maior classe é o `ApiController`, com 264 linhas.
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: api → service → repository → domain, sem ciclo. As regras não conhecem o Javalin nem o SQL: `ReservationService` recebe e devolve objetos do domínio, e as transições de situação ficam em `domain/Reservation.java:47-77`. O SQL fica isolado nos repositórios e é sempre parametrizado. Há porém misturas localizadas:

- **A montagem fica dentro do controller.** O `ApiController` cria os repositórios e os serviços no próprio construtor (`api/ApiController.java:33-40`), em vez de recebê-los do `Main`. Daí vem a dependência `api -> repository`.
- **O handler fala direto com o repositório.** O `GET /rooms` vai ao repositório sem passar por serviço (`api/ApiController.java:70`).
- **Uma regra mora no handler.** A segunda via só existe para reserva paga, e essa conferência está no handler (`api/ApiController.java:144`), não no serviço.
- **O tipo do quarto é deduzido do número em três lugares.** Um `switch` sobre o número do quarto aparece em `api/ApiController.java:219-224`, `service/MessageService.java:78-85` e `service/ReportService.java:68-75`, ignorando o `Room`/`RoomType` que já existe.
- **O acesso ao banco é global.** Ele passa por um `static` mutável (`repository/Database.java:10`, `Database.getConnection()` chamado em todos os repositórios, por exemplo `repository/MessageRepository.java:17`), e não por uma dependência injetada.

Nada disso quebra a separação, mas são os pontos que vão doer quando o sistema crescer.

## Design de baixo nível

**Números:** os 5 planos têm uma classe cada sob `RatePlan` (`domain/rateplan/*.java`) e são escolhidos num ponto só, o `RatePlanType.create()` (`domain/RatePlanType.java:5-37`). O preço, a validação e a devolução de todos passam pelo plano (`service/ReservationService.java:48-53`, `domain/Reservation.java:36` e `:74-75`). Fica fora da estrutura um `if` sobre o plano, com cast, no cálculo do desconto do convênio (`domain/Reservation.java:38-39`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A estrutura é um Strategy de verdade. Há uma interface com `validate`, `calculateSubtotal`, `calculateDiscount` e `calculateRefund` (`domain/rateplan/RatePlan.java:6-14`) e uma implementação por plano, inclusive o normal e o não reembolsável. A seleção é feita pelo `enum` com corpo por constante. A validação de cada plano está dentro dele: sexta a domingo em `WeekendPackageRatePlan.java:18-32`, 7 noites em `LongStayRatePlan.java:9-15`, código da empresa em `CorporateRatePlan.java:16-20`. A devolução também: `WeekendPackageRatePlan.java:46-49` e `NonRefundableRatePlan.java:33-35`.

O que afasta do acerto é o desconto do convênio. A assinatura de `calculateDiscount` não leva o código da empresa. Por isso o `CorporateRatePlan` implementa o método da interface devolvendo zero (`CorporateRatePlan.java:37-39`) e tem uma sobrecarga própria (`:41-45`). Quem usa a estrutura volta a perguntar qual é o plano: `if (ratePlanType == RatePlanType.CORPORATE && companyCode != null) ((CorporateRatePlan) ratePlan).calculateDiscount(...)` (`domain/Reservation.java:38-39`). O próximo plano que precise de um dado a mais (a melhor idade precisa da idade) vai repetir esse `if`, porque a estrutura não dá conta de todos os casos.

**O exagero.** Não há. A `AbstractRatePlan` guarda o que os planos compartilham (temporada, arredondamento HALF_EVEN), e o `enum` é o único ponto de escolha.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`AbstractRatePlan.java:10-12`), `LocalDate`, `enum` para situação, tipo e plano. Os defeitos são estes:

- **Laço da soma das noites copiado quatro vezes.** Está em `StandardRatePlan.java:13-24`, `NonRefundableRatePlan.java:13-24`, `CorporateRatePlan.java:23-34` e `LongStayRatePlan.java:18-29`, quando deveria estar na classe abstrata.
- **O pacote acha o preço pelo valor da diária.** O mapa usa um `BigDecimal` da diária como chave (`WeekendPackageRatePlan.java:11-15`, `:36`), e não o `RoomType`, que até é importado e não é usado (`:3`). Basta mudar uma diária para o pacote dar `NullPointerException`.
- **Número de noites calculado em dois lugares.** Ele aparece em `domain/Reservation.java:35` e em `AbstractRatePlan.java:41-43`.
- **`switch` do tipo de quarto triplicado.** São os mesmos três `switch` citados na arquitetura.
- **Dinheiro formatado com espaço inquebrável.** `NumberFormat.getCurrencyInstance(pt-BR)` (`service/MessageService.java:14`) põe um espaço inquebrável (U+00A0) entre "R$" e o valor. O texto pede "R$ 599,76", e o comprovante sai com outro caractere.
- **Montagem da resposta.** Usa `Map.ofEntries` com um valor que pode ser `null` (`api/ApiController.java:198`, `:214`), o defeito que derruba a API.
- **Entrada com casts sem tratamento.** O corpo é lido como `Map` cru, com casts `(String)` e `(Number)` sem tratar (`api/ApiController.java:92-100`). Um tipo inesperado vira `ClassCastException` e 500, e o `catch` (`:105`) não pega esse caso.
- **`Reservation` cheia de setters públicos.** Status e valores podem ser trocados por qualquer um (`domain/Reservation.java:97-231`).

## Testes

**Números:** rodou 1 classe de teste: 0 falhas e 1 erro ("Could not find a valid Docker environment"). Há 16 métodos de teste escritos, todos numa só classe, `IntegrationTest`. A cobertura é de 0 de 752 linhas (0,0%) e 0 de 130 ramos (0,0%), conforme `measures/tests.md`.
**Nota:** ruim

O único arquivo de teste sobe um PostgreSQL pelo Testcontainers (`IntegrationTest.java:24-28`) e ignora o `DB_URL` que o texto diz que o ambiente fornece. Sem Docker, nenhum teste roda, e o `mvn verify` que o texto exige termina em erro (`measures/tests-maven.log`, linhas 274-300). A falha não é só do ambiente: os testes, se tivessem rodado, teriam falhado. `shouldCreateStandardReservation` espera 201 (`IntegrationTest.java:90`), e o serviço responde 500 a toda criação (veja Comportamento). O agente entregou testes que nunca viu passar.

Pela leitura, alguns testes conferem valores: Tecnosul 93.58/686.22 (`:152-154`), pacote 820.00 (`:179-181`), estadia longa (`:206-208`), devolução de 410.00 do pacote (`:326`), 0.00 do não reembolsável (`:311`), 409 e 422 (`:230-233`, `:255-258`). Ficam sem nenhuma rede:

- as recusas dos planos novos: convênio sem código ou com código inválido, pacote fora de sexta a domingo, estadia longa com menos de 7 noites. Se alguém apagar `if (nights < 7)` em `LongStayRatePlan.java:12`, nenhum teste falha;
- Agrovale e Farmavida. Trocar 0.18 por 0.81 passa;
- o texto do comprovante e do e-mail;
- o conteúdo dos CSV, conferido só pelo cabeçalho e por "total," (`:338-340`, `:352-354`).

Os testes também dependem da ordem (`@Order`) e de ids fixos (`/reservations/1`, `/2`, `/4`), usam `Thread.sleep` (`:49`) e não há nenhum teste de unidade das estratégias, que seriam fáceis de testar isoladas.

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), ambas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há chave estrangeira, CHECK, UNIQUE nem EXCLUDE. O esquema é criado por `CREATE TABLE` no código, sem migração. Não há transação explícita nem `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

O que está bem: os tipos estão certos, há `NOT NULL` nos campos obrigatórios, o SQL é parametrizado em todos os repositórios, as conexões vêm de um pool (HikariCP) e são devolvidas com try-with-resources. Os dados sobrevivem ao reinício: "as mensagens continuam lá" passou no roteiro. Os defeitos:

- **O esquema não evolui.** As tabelas são criadas com `CREATE TABLE IF NOT EXISTS` dentro de `repository/Database.java:26-56`. Não há como acrescentar uma coluna numa tabela que já existe sem perder dados ou mexer à mão. Para um sistema em que "o comercial vive inventando plano", isso pesa.
- **Não existe tabela de quartos.** Os quartos estão fixos em memória (`repository/RoomRepository.java:10-16`), então `reservations.room_number` não tem chave estrangeira para nada. As colunas `status` e `rate_plan_type` são texto livre, sem CHECK.
- **Nada impede duas reservas do mesmo quarto na mesma noite.** A conferência de conflito (`service/ReservationService.java:55-63`) e o `INSERT` (`:31`, `repository/ReservationRepository.java:22-61`) rodam em conexões separadas, sem transação, trava ou restrição de exclusão. Numa conferência manual, com dez rodadas de dois pedidos simultâneos para o quarto 301 em noites diferentes, as dez rodadas gravaram as duas reservas no banco (2 linhas por noite). É dado errado, e só não pesa mais aqui porque o resto do esquema guarda certo.
- **Detalhes menores.** Os `ResultSet` não ficam no try-with-resources (por exemplo `repository/ReservationRepository.java:108`); fecham junto com o statement. As transições de situação gravam a linha inteira sem conferir a situação no `WHERE` (`repository/ReservationRepository.java:63-99`), então duas confirmações simultâneas mandariam dois e-mails.

## Comportamento

**Números:** 11 de 72 conferências passaram (61 falharam), conforme `measures/behavior.md`. Na parte comum, 9 de 46. Nos dados depois de reiniciar, 1 de 3. No cenário 01, 0 de 21. Nas duas reservas ao mesmo tempo, 1 de 2.
**Nota:** ruim

O roteiro manda `roomNumber` como número JSON (201). O projeto lê como texto, com cast `(String)` (`api/ApiController.java:92`), e responde 500 com `ClassCastException` (`measures/behavior-service.log`). O texto não diz se o número do quarto é número ou texto. Tratar como texto é uma escolha defensável; responder 500 a um número, e não aceitá-lo nem recusá-lo com 400, não é.

Para separar essa escolha do resto, rodei uma cópia do roteiro que manda o quarto como texto. Passaram só 24 de 72 (16 de 46 na parte comum, 5 de 21 no cenário). O log mostra 29 `NullPointerException` em `Map.ofEntries` (`api/ApiController.java:198`, `:214`). Com o `refund` nulo antes do cancelamento, toda resposta que traz uma reserva quebra: a criação, o `GET /reservations/{id}`, a confirmação e o check-in. Chamei a API à mão e conferi que a reserva é gravada, confirmada e muda de situação no banco, mas o cliente recebe sempre 500 e nunca fica sabendo o `id`. Só o cancelamento, que preenche o `refund`, e o check-out e a segunda via, que devolvem o comprovante, respondem certo.

Para o dono da pousada, isso quer dizer que o site não consegue fazer reserva nenhuma: cada tentativa dá erro, mas fica gravada mesmo assim. Um cliente que tenta de novo encontra o quarto "ocupado" por ele mesmo (409). Conferindo direto no banco, as contas estão certas: Tecnosul 779.80/93.58/686.22, pacote deluxe 820.00, estadia longa 1749.30/174.93/1574.37, devolução do convênio 686.22. As recusas dos planos voltam 400 INVALID_REQUEST, mas nada disso chega ao site.

Outras falhas próprias:

- **Corpo que não é JSON responde 500.** O texto pede 400 INVALID_REQUEST, e o erro de leitura do `bodyAsClass` (`api/ApiController.java:86`) não é tratado.
- **O comprovante e o e-mail usam o espaço inquebrável.** O separador vem de `service/MessageService.java:14`, então "Total: R$ 599,76" não confere com o texto pedido.
- **Reservas simultâneas.** No roteiro oficial, nenhuma rodada criou as duas reservas só porque todas deram 500 antes. Na conferência manual, as duas foram gravadas nas dez rodadas (veja Banco).

## Dúvidas

- **O tipo do número do quarto.** Não é decidido pelo texto ("cada um com number"). Isso explica as falhas do roteiro oficial em "o 201 comporta 3" e na diária, mas não muda a nota: com o quarto como texto, o NPE da resposta continua derrubando a criação e as transições.
- **A classe do design.** Fiquei entre `malfeito` e `acerto`. Os cinco planos estão na estrutura, inclusive os dois antigos, e o único desvio é o `if` sobre CORPORATE no desconto. Mas esse desvio está no cálculo do preço e obriga o chamador a nomear o plano e fazer cast, então ficou `malfeito`.
- **O teste com Testcontainers.** O ambiente de revisão não tem Docker, por isso não deu para rodar o `IntegrationTest`. Pela leitura e pelo comportamento medido, ele falharia no segundo método.
