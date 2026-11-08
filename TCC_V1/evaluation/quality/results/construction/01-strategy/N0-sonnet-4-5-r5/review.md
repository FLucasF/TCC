# Revisão: construção, cenário 01

## Visão geral

O projeto é um serviço Javalin dividido em seis pacotes: `controller` (as rotas), `service` (`ReservationService`, `PricingService`, `ReportService`), `repository` (JDBC puro com HikariCP), `model`, `dto` e o `Main`, onde tudo é montado. Uma requisição entra pelo handler (`ReservationController.createReservation`, que lê o corpo como `JsonNode`). O handler chama o `ReservationService`, que pede o preço e a validação ao `PricingService` e grava pelo `ReservationRepository`. Os dados ficam em três tabelas no PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` dentro do `DatabaseManager`. O sistema quase todo funciona pela API: 68 de 72 conferências passam, e a parte nova do cenário passa inteira (21 de 21). Os problemas mais sérios são estes:

- **Nenhum Strategy.** Os cinco planos de tarifa são decididos por três `switch` no `PricingService` (preço, devolução e validação).
- **Reserva dupla.** Nada no banco nem no código impede duas reservas do mesmo quarto na mesma noite. Em 9 de 10 rodadas simultâneas, as duas reservas foram criadas.
- **Número de noites errado.** As noites são contadas com `Period.getDays()` (`service/PricingService.java:42`), e isso dá errado em estadias de um mês ou mais. Conferi chamando a API: uma reserva de 01/03 a 01/04/2027 ficou com `nights: 0`, e o e-mail disse "0 diárias". Uma estadia longa de 01/03 a 03/04/2027 (33 noites) foi recusada com "Long stay requires minimum 7 nights".
- **Testes quase ausentes:** 6 testes, que cobrem 9,7% das linhas.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, sem ciclo (`measures/dependencies.md`). O Javalin aparece só em `controller` e no pacote raiz, e o JDBC só em `repository`. A maior classe é `ReservationService`, com 277 linhas.
**Nota:** com problemas

As camadas existem, e as dependências apontam na direção certa: borda → serviço → repositório. As regras não conhecem o Javalin nem o SQL: `service` não usa Javalin nem JDBC, segundo a tabela do jdeps. A montagem dos objetos fica num lugar só (`Main.java:22-35`). Os defeitos são localizados:

- **O acesso ao banco passa por um `static` global.** O `DataSource` é um campo estático mutável (`repository/DatabaseManager.java:12`). Todo repositório o busca por dentro, com `DatabaseManager.getConnection()` (`repository/ReservationRepository.java:26`, `repository/RoomRepository.java:17`, `repository/MessageRepository.java:18`), em vez de recebê-lo do `Main`. Por isso não dá para trocar o banco num teste.
- **Dois controllers pulam o serviço** e falam direto com o repositório: `controller/RoomController.java:21` e `controller/MessageController.java:18`. Em `RoomController.java:23-32`, a montagem da resposta é feita à mão com `HashMap`.
- **O serviço de reservas acumula apresentação.** O `ReservationService` monta o texto do comprovante, o formato do dinheiro, o formato do CPF e o corpo do e-mail (`service/ReservationService.java:178-240`), além das seis exceções da aplicação (`:242-276`). Por isso `service` depende de `dto` (`ReservationService.java:3`), e a classe é a maior do projeto.
- **O tratamento de erro se repete em cada rota.** Os mesmos `try/catch` se repetem em cada handler (`controller/ReservationController.java:83-151`) em vez de um `app.exception(...)` central.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem o próprio comportamento num lugar só dele. O comportamento de cada plano está espalhado por 3 `switch` sobre o `RatePlan`, em `PricingService.java:44-50` (preço), `:133-137` (devolução) e `:144-161` (validação). O preço do pacote fica ainda num quarto lugar, o `enum` de tipo de quarto (`model/RoomType.java:6-8`). O `RatePlan` é um `enum` sem comportamento (`model/RatePlan.java:3-9`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto avisa que "o comercial vive inventando plano", cada um com o seu preço, a sua regra de quem pode usar e a sua devolução. O agente não criou nenhuma estrutura por plano. Um plano novo (melhor idade, antecedência de 60 dias) exige mexer em três `switch` do `PricingService` e no `enum`. É exatamente o caso que a referência chama de omissão: os `switch` de comportamento crescem para os cinco casos. O que aproxima do certo: o preço, a validação e a devolução pelo menos estão todos numa classe só, sem `if (ratePlan == …)` no `ReservationService`. Ele só chama `validateRatePlan`, `calculatePrice` e `calculateRefund` (`ReservationService.java:48`, `:55`, `:103`).

**O exagero.** Não há interfaces de uma implementação só, fábricas ou camadas que só repassam.

**O resto do código:**

- **Duplicação e um erro de data.** As noites são calculadas duas vezes com `(int) checkIn.until(checkOut).getDays()` (`PricingService.java:42` e `:142`). O `Period.getDays()` devolve só o componente de dias, então o número de noites sai errado a partir de um mês de estadia (o bug descrito na Visão geral). O código da empresa também é validado duas vezes (`PricingService.java:67-70` e `:146`). Os métodos do não reembolsável, do convênio e da estadia longa são cópias, que só mudam a taxa (`:58-63`, `:65-76`, `:83-88`).
- **O estado.** `PricingResult` tem campos públicos (`PricingService.java:27-30`). `Reservation` é um objeto só de getters e setters (`model/Reservation.java`), e as transições de situação são feitas com `setStatus` solto no serviço (`ReservationService.java:85`, `:105`, `:120`, `:134`).
- **Os erros.**
  - Um `catch (Exception e)` transforma qualquer falha em 400 e esconde a causa (`ReservationController.java:66`).
  - Um plano desconhecido devolve a mensagem interna do Java, "No enum constant com.pousadamarealta.model.RatePlan.XYZ" (`ReservationController.java:40` com `:56-57`).
  - Um campo enviado como `null` passa pela verificação de presença (`:26-28`), e `asText()` grava o texto "null" como nome do hóspede (`:34`). Conferi isso pela API.
- **O dinheiro.** O tipo é o certo: `BigDecimal` com `HALF_EVEN` (`PricingService.java:164-166`). Mas o formato depende do `Locale` padrão da JVM (`String.format("R$ %,.2f")` em `ReservationService.java:227` e `%.2f` em `service/ReportService.java:62` e `:75`). Numa máquina em pt_BR, o comprovante e os CSV sairiam com a pontuação trocada. Além disso, os zeros são `BigDecimal.ZERO` sem escala (`ReservationService.java:72`, `PricingService.java:55`), e saem no JSON como `0` em vez de `0.00`.

## Testes

**Números:** 6 testes rodaram, 0 falhas; 6 métodos escritos, num arquivo só. Cobertura de 77 de 795 linhas (9,7%) e de 15 de 132 ramos (11,4%). `controller`, `repository` e `dto` têm 0% de cobertura, e `service` tem 54 de 247 linhas (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

**O que existe, e confere com valores.** O único arquivo, `src/test/java/com/pousadamarealta/PricingServiceTest.java`, confere os exemplos do texto:

- o STANDARD em julho dá 599,76 (`:18-32`);
- a Tecnosul dá 93,58 de desconto e 686,22 de total (`:50-64`);
- o pacote deluxe sai 820,00 (`:66-80`) e devolve 410,00 no cancelamento (`:82-87`);
- a estadia longa dá 1.749,30, 174,93 e 1.574,37 (`:89-103`).

**O que ficou sem rede** (pela pergunta da mutação, nenhum teste falharia se alguém mexesse nestas regras):

- **A validação dos planos:** sexta a domingo, o mínimo de 7 noites (trocar `< 7` por `<= 7` em `PricingService.java:157`) e o código da empresa ausente ou inválido.
- **As taxas da Agrovale e da Farmavida** (trocar 0.18 por 0.81 em `PricingService.java:22`).
- **A devolução do convênio, da estadia longa e do não reembolsável** (`PricingService.java:134-135`).
- **O resto do sistema:** o desconto do não reembolsável, as transições de situação, os códigos de erro, o comprovante e o e-mail, os dois relatórios e a persistência.

Num sistema de cerca de 800 linhas, um arquivo de seis testes de preço com 9,7% de cobertura deixa a coluna quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)`, e as datas em `date`. As conferências acusaram:

- a chave estrangeira `reservations(room_number)` sem índice;
- `status`, `rate_plan`, `type` e `channel` sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no código, sem migração;
- nenhum SQL montado com `+` (`measures/database.md`).

**Nota:** com problemas

**O que está certo.** Os tipos estão certos, há chaves primárias, e a reserva se liga ao quarto por chave estrangeira (`repository/DatabaseManager.java:53`). Todo SQL usa parâmetros `?` (`repository/ReservationRepository.java:17-44`, `:114-129`). As conexões vêm de um pool (HikariCP, `DatabaseManager.java:15-21`) e voltam por try-with-resources. Os dados sobrevivem ao reinício (3 de 3 conferências no roteiro).

**Os defeitos:**

- **Nada protege a regra mais importante, um quarto por noite.** A disponibilidade é conferida com um `SELECT` (`ReservationRepository.java:114-121`), e o `INSERT` vem depois, numa conexão separada (`ReservationService.java:50-74`). Não há transação, trava nem restrição `EXCLUDE` no esquema. O roteiro do comportamento criou as duas reservas em 9 de 10 rodadas simultâneas.
- **O esquema não sabe evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` (`DatabaseManager.java:43-80`), que não acrescenta uma coluna numa tabela que já existe. O próximo plano com um campo novo vai exigir mexer no banco à mão.
- **Restrições ausentes.** Não há índice na chave estrangeira. Não há CHECK em `status` e `rate_plan`, nem em `check_out > check_in` e `guests >= 1`.
- **Um tamanho que pode cortar dados.** `guest_cpf` é `VARCHAR(14)` (`DatabaseManager.java:56`), mas a API aceita qualquer texto, e um CPF maior faria o `INSERT` falhar com 400 genérico, pelo `catch (Exception)` do controller.

## Comportamento

**Números:** 68 de 72 conferências passaram (`measures/behavior.md`):

- a parte comum: 43 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01 (os planos novos): 21 de 21;
- as reservas simultâneas: 1 de 2.

**Nota:** com problemas

Todos os valores do cenário conferem: Tecnosul, Agrovale e Farmavida, as recusas do convênio, o pacote nos três tipos de quarto e em julho, a devolução de 410,00, a estadia longa com e sem temporada, e o `companyCode` ignorado nos outros planos. As falhas:

- **Reservas simultâneas.** Em 9 de 10 rodadas, dois pedidos ao mesmo tempo para o mesmo quarto e as mesmas noites criaram as duas reservas. Para o dono da pousada, isso quer dizer dois hóspedes vendidos para o mesmo quarto quando o site recebe pedidos próximos. É a falha que mais pesa.
- **"O 201 comporta 3" e "a diária do 201 é 389.90".** O `GET /rooms` devolve tudo certo, mas com `number` como texto (`"201"`), e o roteiro procura um número. O texto não fixa o tipo do `number`, então é uma escolha razoável e não pesa.
- **Dinheiro com 2 casas.** Na criação, `amountPaid` (e `discount` nos planos sem desconto) sai como `0`, e não `0.00` como o texto pede ("número com 2 casas"). A causa é o `BigDecimal.ZERO` sem escala (`ReservationService.java:72`, `PricingService.java:55`). É um defeito pequeno, mas real, no contrato com o site.

**Fora do roteiro, conferido por mim pela API:**

- Uma reserva de 01/03 a 01/04/2027 foi criada com `nights: 0`. O comprovante e o e-mail disseram "0 diárias", embora o valor (R$ 7.746,90) esteja certo.
- Uma estadia longa de 33 noites foi recusada como se tivesse menos de 7.
- Um nome enviado como `null` foi gravado como "null".

Isso atinge exatamente o tipo de hóspede que a estadia longa quer atrair. Esses pontos pesam no design, onde está a causa, e não mudam a nota desta coluna.

## Dúvidas

- **A nota dos testes:** fiquei entre `com problemas` e `ruim`. Os seis testes conferem com valores todos os exemplos de preço do texto, e isso conta a favor. Mas cobrem só uma classe (9,7% das linhas) e nada da validação dos planos, das transições, dos erros, dos relatórios ou do banco. Por isso dei `ruim`.
- **A nota do banco:** a ausência total de proteção contra reserva dupla é um risco de dado errado, e poderia justificar `ruim`. Fiquei com `com problemas` porque a skill do banco lista a duplicação mal protegida entre os defeitos dessa nota, e o resto do esquema (tipos, chaves, parâmetros, persistência) está correto.
