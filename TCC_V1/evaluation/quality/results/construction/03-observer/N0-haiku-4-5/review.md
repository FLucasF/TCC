# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: o raiz `com.pousadamarealta`, com `Main`, `Database`, `ReservationService`, `ReportService` e `MessageService`, todos com métodos `static`, e mais `api` (DTOs), `models` (classes com campos públicos), `exceptions`, `utils` (preço, datas, dinheiro) e `notifications` (os ouvintes da governança e do financeiro). Uma requisição entra numa lambda em `Main.java`, que chama um método estático do serviço. O serviço valida, calcula, monta o SQL com JDBC puro (`DriverManager` a cada chamada) e grava no PostgreSQL. O esquema é um `CREATE TABLE IF NOT EXISTS` em `Database.java`. **O sistema não funciona.** O serviço morre ao subir, porque as rotas usam a sintaxe `:id` (`Main.java:41`), que o Javalin 6 recusa ("Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'", em `measures/database.md` e `measures/behavior.md`). Corrigido isso numa cópia temporária fora de `projeto/`, aparece um segundo erro que também bloqueia: o `pom.xml:28-32` traz o Gson e não o Jackson, e `Main.java:11` não configura outro conversor de JSON. Por isso todo `ctx.json`/`bodyAsClass` responde 500 ("It looks like you don't have an object mapper configured"), inclusive o `GET /rooms`. Nenhuma das onze rotas pedidas responde certo. Os 7 testes passam porque nenhum deles sobe a API.

## Arquitetura

**Números:** 6 pacotes, 9 dependências entre eles, 1 ciclo (`com.pousadamarealta` ↔ `com.pousadamarealta.notifications`). Javalin e JDBC moram no mesmo pacote raiz. A maior classe é `ReservationService`, com 366 linhas, de 789 do pacote raiz.
**Nota:** ruim

- A borda HTTP é fina: cada handler lê o corpo, chama o serviço e traduz as exceções (`Main.java:20-39`), e as regras não conhecem o Javalin. Até aqui, está certo.
- Não existe camada de dados. As regras e o SQL moram nas mesmas classes, em todas elas. `ReservationService` valida o pedido (`ReservationService.java:38-65`), decide a situação e a devolução (`:170-181`), monta o texto do e-mail do hóspede (`:341-365`) e escreve todo o SQL (`:84-117`, `:144-154`, `:183-193`, `:271-296`). `ReportService` monta a consulta e o CSV no mesmo método (`ReportService.java:20-61`, `:80-131`). `MessageService` é o acesso à tabela `messages`, mas também mora no pacote raiz.
- Não há montagem: tudo é `static`. `Database` lê o ambiente num inicializador estático (`Database.java:10-18`). O serviço chama `Database.getConnection()` direto. `NotificationManager` registra os ouvintes num bloco `static` (`notifications/NotificationManager.java:11-14`), e não no `main`.
- O ciclo nasce do lugar errado da responsabilidade. O serviço chama `NotificationManager` (`ReservationService.java:162`, `:196`), e os ouvintes voltam ao pacote raiz para chamar `MessageService.sendMessage` (`notifications/GovernanceNotificationListener.java:5`, `:23`; `notifications/FinanceNotificationListener.java:6`, `:21`).
- Uma classe de 366 linhas faz validação, preço, transições, SQL e texto de e-mail. É nela que o próximo pedido vai doer.

## Design de baixo nível

**Números:** dos três interessados na confirmação (hóspede, governança, financeiro), 2 estão na estrutura de ouvintes e 1 fica fora: o hóspede, por chamada direta. Os dois do cancelamento estão na estrutura. A interface `NotificationListener` tem 2 implementações. A lista de ouvintes é estática e preenchida dentro do próprio `NotificationManager`.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern, o que aproxima:** há uma interface de ouvinte com dois eventos, `onReservationConfirmed` e `onReservationCancelled` (`notifications/NotificationListener.java:7-10`). Há uma implementação por interessado, e cada uma monta o seu e-mail (`GovernanceNotificationListener.java:11-43`, `FinanceNotificationListener.java:11-38`). O serviço percorre a lista sem saber quem está nela (`NotificationManager.java:20-30`).

**O pattern, o que afasta:**
- O hóspede não é ouvinte. A confirmação ainda manda o e-mail dele à mão, com o texto montado no próprio serviço (`ReservationService.java:158-160`, `:341-365`). É exatamente o caso que a referência chama de malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".
- Os ouvintes não vêm de fora. A lista é um `static` preenchido no bloco estático do próprio `NotificationManager` (`NotificationManager.java:9-14`). Para tirar o restaurante ou o transfer, que "entram e saem conforme os contratos", é preciso editar essa classe, e não a montagem. O serviço também não recebe a lista: chama o `NotificationManager` estático.
- A regra "só avisa quem já estava confirmada" ficou espalhada pelos ouvintes. A governança pergunta a situação da reserva, e aceita até `CHECKED_OUT`, que nunca chega ali (`GovernanceNotificationListener.java:29-33`). O financeiro olha se a devolução é positiva (`FinanceNotificationListener.java:27-29`). O evento não diz "uma reserva confirmada foi cancelada": cada ouvinte novo terá de repetir a mesma pergunta.

**O exagero:** não há. A interface tem duas implementações reais, e não aparecem fábricas nem camadas de repasse.

**O resto do código:**
- Há duplicação. O mesmo bloco `try/catch` que traduz exceção em resposta aparece em oito handlers (`Main.java:20-146`), em vez de um `app.exception`. O mapeamento da linha do quarto para `Room` está copiado (`ReservationService.java:22-27` e `:307-312`). O comprovante é montado duas vezes, em formatos diferentes: no DTO (`:253-269`) e no texto do e-mail (`:349-359`).
- O estado é mutável e global. Todas as classes de `models` e `api` têm campos públicos mutáveis (`models/Reservation.java:7-22`, `models/Room.java:6-9`). Há `static` mutável: a lista de ouvintes num `ArrayList` sem proteção (`NotificationManager.java:9`) e um contador que nunca é usado (`MessageService.java:9`). Um `NumberFormat` compartilhado entre as threads do Javalin (`utils/PriceFormatter.java:8`) não é thread-safe e pode embaralhar valores formatados ao mesmo tempo.
- Faltam tipos certos na entrada. `ReservationRequest` usa `int` para `roomNumber` e `guests` (`api/ReservationRequest.java:4`, `:10`), e o campo ausente vira 0 sem ninguém notar. O plano chega como `String` e só vira `enum` depois das outras validações (`ReservationService.java:78`). Os relatórios guardam as linhas em `Map<String,String>` (`ReportService.java:36`, `:102`). O dinheiro em `BigDecimal` com `HALF_EVEN` em cada passo está certo (`utils/PriceCalculator.java:23-48`).
- Os erros são mal tratados. Toda `SQLException` vira `RuntimeException` genérica. `catch (Exception e)` esconde a causa no parse do mês (`ReportService.java:73`). `DateParser.parse(null)` dá `NullPointerException` e 500, em vez de 400, quando falta `date` (`utils/DateParser.java:11-16`, `ReportService.java:16`). Um `id` não numérico também dá 500 (`Main.java:43`).

São defeitos espalhados por todas as classes, e o `static` em tudo torna o código difícil de testar e de mudar com segurança.

## Testes

**Números:** 7 testes rodaram, com 0 falhas; 7 métodos escritos. A cobertura é de 9,1% das linhas (58 de 638) e 17,2% dos ramos (21 de 122). O pacote `notifications` tem 0 de 48 linhas cobertas, e o pacote raiz, 28 de 477. Nenhum teste passa pela API.
**Nota:** ruim

- `PriceCalculatorTest.java:31-92` confere o cálculo do preço com valores: a temporada nas bordas (14/12 e 15/12), o R$ 299,88 da noite de julho, o R$ 599,76, o desconto de R$ 89,96 e o total de R$ 509,80. É a única regra testada de verdade.
- `IntegrationTest.java:7-18` só confere que existem 5 quartos, com o primeiro 101 e o último 301.
- Nada testa a parte do cenário. Se alguém apagasse `register(new FinanceNotificationListener())` (`NotificationManager.java:13`), trocasse o assunto "Reserva confirmada - quarto" ou invertesse a condição `<= 0` do reembolso (`FinanceNotificationListener.java:27`), nenhum teste falharia.
- Também não há teste das transições, do cancelamento e da devolução, dos códigos de erro, dos relatórios, do comprovante nem do e-mail do hóspede.
- Os testes não sobem o `Main`. Por isso o `mvn verify` passa (`measures/tests-maven.log`, BUILD SUCCESS) com um serviço que não sobe.
- As dependências do Testcontainers estão no `pom.xml:40-51`, mas nenhum teste as usa.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice (`reservations.room_number`), nenhum `CHECK`/`UNIQUE`/`EXCLUDE`, nenhuma transação, nenhum `FOR UPDATE` e nenhum pool. O script registrou que o serviço não subiu contra o banco vazio.
**Nota:** ruim

- O esquema foi criado antes de o serviço morrer, porque `Database.initialize()` roda antes das rotas (`Main.java:10`). Mas o serviço não sobe contra um banco vazio, e a skill do banco conta isso.
- Nada protege contra a reserva dupla, e esse é o defeito próprio da coluna que pesa mais. A disponibilidade é conferida numa conexão (`ReservationService.java:76`, `:271-296`) e o `INSERT` é feito em outra (`:84-117`), sem transação, trava ou restrição (não há `EXCLUDE` sobre o quarto e as datas). Nem um `synchronized` existe. Dois pedidos ao mesmo tempo gravam duas reservas para o mesmo quarto e as mesmas noites, ou seja, dado errado.
- O mesmo vale para as transições. Confirmar e cancelar leem a situação e depois fazem `UPDATE ... WHERE id = ?`, sem conferir a situação no `WHERE` (`ReservationService.java:145`, `:184`). Duas confirmações simultâneas passam as duas e mandam os e-mails em dobro.
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:34-71`), sem migrações, e não sabe evoluir uma tabela que já existe.
- Não há `CHECK` nas colunas de situação, plano e tipo (`Database.java:44`, `:53`, `:58`), nem em `guests > 0` ou `check_out > check_in`.
- A chave estrangeira `room_number` não tem índice (`Database.java:46`).
- Cada consulta abre uma conexão nova com `DriverManager`, sem pool (`Database.java:20-22`). Uma única criação de reserva abre quatro conexões.
- O que está certo: o SQL sempre com `?`, as conexões fechadas com try-with-resources e os tipos certos.

## Comportamento

**Números:** 0 conferências rodaram, de nenhuma das quatro partes (comum, reinício, cenário, simultâneas). O roteiro registrou "não deu para testar (o serviço não subiu contra um banco vazio)", com a exceção do Javalin em `Main.java:41` (`measures/behavior.md`, `measures/behavior-service.log`).
**Nota:** ruim

Para o dono da pousada, o sistema simplesmente não abre: o `java -jar` termina com erro antes de atender a porta. E mesmo sem o `:id`, não haveria resposta útil. Numa cópia temporária com as rotas trocadas para `{id}`, `GET /rooms` e `POST /reservations` responderam 500, por falta do Jackson (`pom.xml:28-32`). Então nenhum site, recepção ou contador consegue usar nada.

Pela leitura do código, ficaram ainda estes erros que o roteiro pegaria se o serviço respondesse. Eles não mudam a nota:
- O cancelamento é aceito depois do check-in e devolve o dinheiro, e o texto proíbe isso (`ReservationService.java:170-181`).
- O cancelamento não reembolsável grava `refund` nulo em vez de 0,00 (`:187`).
- A segunda via sai para reserva cancelada (`:246`).
- O relatório de ocupação conta reservas pendentes, que não estão pagas (`ReportService.java:74`).
- O financeiro lista a reserva STANDARD cancelada com valor 0,00 (`ReportService.java:134` filtra o pago, não o que ficou).
- Faltar `roomNumber` dá 404 em vez de 400 (`api/ReservationRequest.java:4`).
- Faltar `date` dá 500 (`utils/DateParser.java:13`).
- Um corpo que não é JSON não é tratado (`Main.java:22` só captura `IllegalArgumentException`).
- As reservas simultâneas não têm proteção (ver o Banco).

Os e-mails da governança e do financeiro estão escritos com os assuntos e destinos pedidos, mas nunca chegaram a ser exercitados.

## Dúvidas

- **Arquitetura entre `ruim` e `com problemas`:** a borda HTTP é fina e as regras não conhecem o Javalin, mas não existe camada de dados, tudo é `static` e há um ciclo. Dei `ruim` pela soma desses pontos.
- **Banco:** a falha de subida vem das rotas, não do esquema, mas a skill do banco conta o serviço que não sobe contra um banco vazio. Sem isso, a coluna ficaria entre `com problemas` e `ruim`, por causa da falta total de proteção contra a reserva dupla.
- **Comportamento com o Jackson:** o erro do Jackson foi visto numa cópia temporária com as rotas corrigidas, fora de `projeto/`. O roteiro oficial não chegou a medir isso.
