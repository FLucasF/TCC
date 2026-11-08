# Revisão: construção, cenário 01

## Visão geral

O projeto tem cinco pacotes. No pacote raiz `com.pousadamareaita` ficam o `main`, o `Database` (com o pool estático e o `CREATE TABLE`) e quatro handlers estáticos do Javalin. Ao lado deles estão `service` (`ReservationService`, `EmailService`), `repository` (JDBC com SQL montado por `String.format`), `model` (POJOs) e `util` (`PricingCalculator`). Uma requisição passa por `Application` → `ReservationHandler` (método estático) → `ReservationService` → `ReservationRepository` → PostgreSQL. Os dados ficam em três tabelas criadas no código. O problema mais sério é que o sistema não funciona para o uso básico. O `pom.xml` não traz o `jackson-datatype-jsr310`, então serializar a reserva (`LocalDate`) lança `InvalidDefinitionException`. O `catch (Exception)` em `ReservationHandler.java:51` transforma isso em 400 `INVALID_REQUEST`, **depois** de a reserva já ter sido gravada. Por isso toda criação de reserva responde 400, o site nunca recebe o id, e o fluxo inteiro (confirmação, e-mail, check-in, check-out, relatórios) para aí. O roteiro mostra 24 de 72 conferências passando. Há outros defeitos de regra: a confirmação exige que o cliente mande o `amountPaid`, a alta temporada vale para dezembro inteiro, e o financeiro descarta as reservas não reembolsáveis canceladas. Há também duplicação real de reservas sob concorrência (5 pares duplicados no banco) e SQL montado com dado do hóspede.

## Arquitetura

**Números:** 5 pacotes e 8 dependências entre eles. Há 1 ciclo de três pacotes: `com.pousadamareaita` → `service` → `repository` → `com.pousadamareaita`. Javalin, JDBC, Jackson e o pool aparecem todos no pacote raiz. A maior classe é `ReservationHandler`, com 225 linhas (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem, e as regras em `service`/`util` não dependem do Javalin nem do JDBC (`measures/dependencies.md`, tabela de bibliotecas). Mas há defeitos.

- **O ciclo:** os repositórios importam `com.pousadamareaita.Database` (`repository/ReservationRepository.java:3`, `RoomRepository.java:3`, `MessageRepository.java:3`). Esse pacote raiz também guarda os handlers, que dependem do serviço. A conexão mora no mesmo pacote da borda HTTP.
- **A montagem não existe:** cada classe cria as suas dependências em campos `static final` (`ReservationHandler.java:19`, `ReportHandler.java:16`, `RoomHandler.java:12`, `MessageHandler.java:11`, `service/ReservationService.java:18-19`, `service/EmailService.java:10`). O `DataSource` é um `static` global e mutável (`Database.java:15`). Nada pode ser substituído num teste.
- **A borda pula a regra:** `ReportHandler` fala direto com o repositório (`ReportHandler.java:16`) e monta os relatórios ali mesmo, com regra de negócio: a conta pago − devolvido e o tipo do quarto num `switch` (`ReportHandler.java:37`). `ReservationHandler.java:152` decide no handler quem tem direito à segunda via.
- **Os erros como fluxo:** o serviço lança uma `ApiException` com código HTTP dentro dela (`service/ReservationService.java:199-208`), o que prende a regra ao HTTP.

São misturas localizadas, não a falta de camadas, e por isso a nota é `com problemas`.

## Design de baixo nível

**Números:** dos 5 planos de tarifa, 0 estão numa estrutura própria. O comportamento dos planos está espalhado por 4 cadeias de `if` sobre a string do plano: o preço (`util/PricingCalculator.java:43` e `:50-59`), a devolução (`util/PricingCalculator.java:106-112`), a validação (`service/ReservationService.java:157-169`) e mais a lista `VALID_RATE_PLANS` (`service/ReservationService.java:20`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

- **O pattern:** o texto avisa que "o comercial vive inventando plano", e que cada plano tem o seu preço, a sua regra de quem pode usar e a sua devolução. Não há Strategy nem nada equivalente. O plano é uma `String` solta (`model/Reservation.java`, campo `ratePlan`), e cada comportamento é decidido por `if ("CORPORATE".equals(ratePlan))` em três lugares diferentes. O próximo plano ("melhor idade", "60 dias") obriga a mexer em `PricingCalculator.calculatePrice`, `PricingCalculator.calculateRefund`, `ReservationService.validateRatePlanRules` e `VALID_RATE_PLANS`. Os dados de cada plano também ficam soltos: os descontos por empresa em `PricingCalculator.java:10`, os preços do pacote por **número do quarto** (e não por tipo) em `PricingCalculator.java:17`, e a lista de empresas válidas repetida à mão em `ReservationService.java:173-175`. Pela referência, é omissão.
- **O exagero:** não há interface, fábrica nem camada sem necessidade. A classe é `contenção`.
- **O resto do código:**
  - **Duplicação:** o tipo do quarto é deduzido do número por um `switch` copiado três vezes (`ReservationHandler.java:169`, `service/EmailService.java:56`, `ReportHandler.java:37`), embora `Room.type` exista no banco. O mapeamento da linha da reserva e a lista de colunas se repetem em quatro consultas (`repository/ReservationRepository.java:61`, `:100`, `:124`, `:150`). O "buscar ou 404" se repete em cada método do serviço (`ReservationService.java:70-73`, `:91-94`, `:116-119`, `:134-137`).
  - **Os erros:** os repositórios engolem `SQLException` e devolvem vazio (`repository/ReservationRepository.java:73`, `:115`, `:141`, `:166`; `RoomRepository.java:37`, `:59`). Uma falha do banco vira "reserva não existe" (404), ou um relatório vazio. Cada handler tem `catch (Exception)` que transforma qualquer defeito interno em 400 `INVALID_REQUEST` (`ReservationHandler.java:51`, `:70`, `:92`, `:106`, `:120`, `:135`, `:161`). Foi exatamente isso que escondeu o erro de serialização que quebra o sistema.
  - **Os tipos:** o `status` é um `enum`, mas o plano é texto. `PricingResult` e `ApiException` têm campos públicos (`util/PricingCalculator.java:25-28`, `service/ReservationService.java:200-201`).
  - **As regras erradas no código:** a temporada considera `month == 12` inteiro, contra o pedido "de 15 de dezembro", e há uma variável `day` calculada e não usada (`util/PricingCalculator.java:97-100`). A confirmação aceita o valor pago vindo do cliente em vez do total (`ReservationHandler.java:81-87`). Zero hóspedes cai em `CAPACITY_EXCEEDED` (`service/ReservationService.java:43`).

Os defeitos estão espalhados pelas camadas, e por isso a nota é `ruim`.

## Testes

**Números:** 7 testes rodaram, com 0 falhas e 0 erros, de 7 métodos escritos, todos numa classe só (`PricingCalculatorTest`). A cobertura é de 54 de 744 linhas (7,3%) e 20 de 204 ramos (9,8%). Só o pacote `util` é coberto (54/58), e `service`, `repository`, handlers e `model` têm 0% (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que existe confere valores de verdade:

- os exemplos do texto: Tecnosul 686,22 (`PricingCalculatorTest.java:48-61`), pacote 820,00 (`:64-77`), devolução 410,00 (`:80-85`) e estadia longa 1.574,37 (`:88-101`);
- julho com 599,76 (`:104-115`) e o não reembolsável (`:32-45`).

O resto das regras não tem teste nenhum:

- a validação dos planos novos (sexta a domingo, 7 noites, código da empresa: `ReservationService.java:155-185`);
- Agrovale e Farmavida;
- a devolução do convênio e da estadia longa;
- as transições de situação, os códigos de erro, o comprovante e o e-mail, os relatórios e a concorrência.

Na pergunta da mutação: trocar `0.18` por `0.81` em `PricingCalculator.java:13`, apagar a chamada `validateRatePlanRules` (`ReservationService.java:51`) ou trocar `nights < 7` por `nights <= 7` não faz nenhum teste falhar. A temporada errada de dezembro (`PricingCalculator.java:100`) passa porque nenhum teste usa 1 a 14 de dezembro. E o defeito que derruba o sistema inteiro (a serialização de `LocalDate`) passou pelo `mvn verify` porque nenhum teste chama a API. Os testes existentes são bons, mas a coluna está quase ausente: 1 classe e 7% de cobertura.

## Banco

**Números:** 3 tabelas, todas com chave primária, e dinheiro em `numeric(10,2)` em todas as colunas de valor. As conferências acusaram:

- a chave estrangeira `reservations(room_number) → rooms` sem índice;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações e 0 `FOR UPDATE`;
- o esquema criado por `CREATE TABLE IF NOT EXISTS` no código, sem migração (`measures/database.md`).

**Nota:** ruim

- **SQL montado com dado de fora:** todas as escritas e as consultas por data usam `String.format` com os valores colados no texto. Isso inclui o nome, o e-mail, o CPF e o código da empresa que vêm do corpo da requisição (`repository/ReservationRepository.java:22-42`, `:80-86`, `:99-105`; `repository/MessageRepository.java:19-27`). A única proteção é um `replace("'", "''")` feito à mão (`ReservationRepository.java:201-204`, `MessageRepository.java:70-73`). Nenhum `PreparedStatement` com `?` no projeto. É o critério de `ruim` da skill.
- **A duplicação de reserva não é protegida:** a disponibilidade é conferida noite a noite com uma consulta por noite (`service/ReservationService.java:187-197`) e depois gravada (`:66`), sem transação, sem trava e sem restrição no banco. No banco que o roteiro de comportamento deixou (`review_behavior_2`), há **5 pares de reservas não canceladas no mesmo quarto e na mesma noite** (quarto 301, noites de 04/02, 08/02, 12/02, 14/02 e 18/02/2030). Dado errado gravado.
- **O esquema não evolui:** o `CREATE TABLE IF NOT EXISTS` (`Database.java:32`, `:39`, `:60`) é executado partindo o texto em `;` (`Database.java:80`). Ele não acrescenta coluna numa tabela existente.
- **Os detalhes:**
  - o tipo e a capacidade do quarto estão no banco, mas o código os ignora e repete o tipo por número (ver Design);
  - o pool (Hikari) e o try-with-resources estão certos (`repository/ReservationRepository.java:44-46`).

## Comportamento

**Números:** 24 de 72 conferências passaram (48 falharam):

- a parte comum: 16 de 46;
- depois de reiniciar: 2 de 3;
- o cenário 01: 5 de 21;
- a concorrência: 1 de 2 (`measures/behavior.md`).

**Nota:** ruim

- **A falha principal:** `POST /reservations` responde **400 `INVALID_REQUEST` para toda reserva válida**. O log (`measures/behavior-service.log`, 52 ocorrências) mostra `InvalidDefinitionException: Java 8 date/time type java.time.LocalDate not supported by default`: falta o módulo `jackson-datatype-jsr310` no `pom.xml`. A exceção nasce em `ctx.json(...)` (`ReservationHandler.java:48`), depois de `reservationRepo.save` (`service/ReservationService.java:66`). A reserva fica gravada, mas o site recebe um erro e nenhum id. O mesmo vale para `GET /reservations/{id}` e para as respostas de confirmação, check-in e cancelamento, que serializam a mesma estrutura. Para o dono da pousada, o site não consegue fazer uma reserva sequer, e cada tentativa ocupa o quarto sem o hóspede saber.
- **O que passa sozinho:** os quartos, as recusas de validação (409 de quarto ocupado, 422, 404, 400) e as recusas do cenário (convênio sem código ou com código inválido, pacote fora de sexta a domingo, estadia longa com 6 noites). O 409 de quarto ocupado só passa porque a tentativa "fracassada" anterior ficou gravada.
- **Os defeitos que o roteiro não alcança, porque a criação quebra antes, mas que estão no código:**
  - a confirmação sem corpo responde 400, porque o handler exige um `amountPaid` mandado pelo cliente, e o pago é o que o cliente disser, não o total (`ReservationHandler.java:81-87`);
  - a temporada cobra +20% de 1 a 14 de dezembro (`util/PricingCalculator.java:100`);
  - o financeiro exclui toda reserva `CANCELLED` (`repository/ReservationRepository.java:128`), então a não reembolsável cancelada, que o texto manda contar, some do relatório;
  - a segunda via é emitida para reserva cancelada (`ReservationHandler.java:152` só recusa `PENDING`);
  - zero hóspedes responde 422 em vez de 400 (esta o roteiro pegou).
- **Concorrência:** o roteiro marcou "nenhuma rodada com as duas criadas" porque as duas respostas vieram 400. Mas o banco mostra que em 5 das 10 rodadas as duas reservas foram gravadas (ver Banco).
- **Reinício:** as mensagens e a ocupação continuam. A conferência da reserva falhou só porque o id nunca chegou ao roteiro.

## Dúvidas

- Na coluna de testes, o que existe é de boa qualidade (valores exatos dos exemplos do texto). Dei `ruim` por estar quase ausente (1 classe, 7% de linhas, nada da validação, das situações, da API ou do banco). Um leitor mais brando poderia dar `com problemas`.
- Os defeitos de regra listados no Comportamento (confirmação exigindo `amountPaid`, dezembro inteiro como temporada, financeiro sem as canceladas, segunda via de cancelada) vêm da leitura do código. O roteiro não chegou a exercitá-los, porque a criação da reserva falha antes.
