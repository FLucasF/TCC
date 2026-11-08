# Revisão: construção, cenário 09

## Visão geral

O projeto é um Javalin 6.7.0 com 8 pacotes: `api` (os handlers, todos `static`), `service` (reservas, preço, e-mail e relatórios), `repository` (JDBC puro com HikariCP), `model`, `config`, `exception` e `util`. A requisição entra por uma rota em `Main.java:26-41`, passa a um método estático do controlador, que lê o corpo como `com.google.gson.JsonObject` e chama o serviço. O serviço usa os repositórios, que falam com o PostgreSQL. O esquema nasce de um `CREATE TABLE IF NOT EXISTS` em `config/DatabaseConfig.java:32-70`. O defeito mais sério derruba o sistema inteiro: o projeto monta e escreve o JSON com Gson (`ReservationController.java:21`, `:37`), mas `ctx.bodyAsClass`/`ctx.json` do Javalin precisam de um mapeador JSON, e o `pom.xml:17-60` não tem o `jackson-databind` nem configura outro. Por isso toda rota que lê ou escreve JSON responde 500 ("It looks like you don't have an object mapper configured", `measures/behavior-service.log:25`): não dá para listar quartos, criar, confirmar, cancelar nem consultar reservas. Mesmo que o JSON funcionasse, ainda haveria estes erros: a confirmação lê um `amountPaid` do corpo em vez de usar o total da reserva (`ReservationController.java:64`); o financeiro e o relatório por plano deixam de fora a NON_REFUNDABLE cancelada (`ReservationRepository.java:205`); a segunda via sai para reserva não paga (`ReservationController.java:105-117`); e os quatro relatórios estão escritos inteiros, cada um à mão, sem o esqueleto comum que o texto pede.

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `com.pousada` e `com.pousada.api`, e o JDBC só em `config` e `repository`. Maiores classes: `ReservationRepository` (244 linhas), `ReservationController` (183), `ReportService` (163).
**Nota:** com problemas

A direção das camadas está certa e não há ciclo: os serviços não conhecem o Javalin nem o JDBC (`service/*` sem `io.javalin` nem `java.sql`), e o SQL fica nos repositórios. Os defeitos:

- **A borda HTTP salta o serviço e guarda regra.** `ReservationController.java:48` e `:108` vão direto ao `ReservationRepository`, e `RoomController.java:12` e `MessageController.java:12` também. `getDocument` (`ReservationController.java:105-117`) decide sozinho quando dá a segunda via, e por isso esquece a regra "só para reserva paga". O comprovante em JSON é montado no controlador (`ReservationController.java:141-159`), que busca o quarto no repositório (`:142`), enquanto o comprovante em texto mora em `EmailService.java:28-52`: a mesma regra fica em dois lugares de duas camadas.
- **Não há montagem.** Tudo é `static`: serviços, repositórios e controladores se chamam por nome de classe, e o pool é um `static` global com inicialização preguiçosa (`DatabaseConfig.java:9`, `:84-89`). Nada pode ser trocado nem isolado num teste, o que explica os testes só de funções puras.
- **O tratamento de erro tem três implementações.** `Main.java:58-61` cuida dele, mas cada controlador também tem o seu `sendErrorResponse` (`ReservationController.java:176-182`, `ReportController.java:90-93`). O de relatórios faz `ctx.json` de uma `String` já montada à mão, o que geraria uma string JSON escapada em vez de um objeto.

## Design de baixo nível

**Números:** dos 4 relatórios, 0 passam por uma estrutura comum. São 4 métodos escritos inteiros em `ReportService.java` (`:21-51`, `:53-81`, `:83-112`, `:114-158`), cada um repetindo a linha de título, o cabeçalho, o laço de linhas e o total.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede um esqueleto único (título, cabeçalho, linhas, total) e avisa que virão mais relatórios e uma linha de data no fim de todos ("quando o jeito dos relatórios mudar, quero que mude em todos de uma vez"). O agente escreveu cada relatório inteiro. A linha de título está copiada quatro vezes (`ReportService.java:34`, `:57`, `:87`, `:137`), com `LODGE_NAME` concatenado à mão em cada uma. A linha do total é escrita de um jeito diferente em cada relatório (`:48`, `:78`, `:109`, `:155`). O cálculo do dinheiro que fica com a pousada (pago menos devolvido) está copiado em `:63-66` e `:129-132`. Não há classe abstrata, método-modelo nem escritor genérico que receba as partes que variam. Acrescentar a linha de data obrigaria a mexer nos quatro métodos, e um quinto relatório seria mais uma cópia. Isso é a definição de omissão da referência.

**O exagero.** Não há estrutura sobrando: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma camada que só repassa. Contenção.

**O resto do código.** Os defeitos estão espalhados:
- **Duplicação.** O comprovante está em `EmailService.java:28-52` (texto) e em `ReservationController.java:141-159` (JSON). Os três tratamentos de erro aparecem na Arquitetura. Os quatro handlers de relatório são quase idênticos (`ReportController.java:14-88`), cada um repetindo validação, parse e cabeçalho `text/csv`. O `DateTimeFormatter` `yyyy-MM-dd` é declarado três vezes.
- **Erros escondidos.** `catch (Exception e)` transforma qualquer falha em 400 "Invalid request body" (`ReservationController.java:40-42`, `:70-72`; `ReportController.java:28-30`). Foi isso que escondeu do agente a falta do mapeador JSON. Os repositórios embrulham tudo em `RuntimeException` genérica (`ReservationRepository.java:46-48` e seguintes). Um `id` não numérico vira `NumberFormatException`, e daí 500 (`ReservationController.java:47`).
- **Lógica morta e ordem errada.** `cancel` muda o status para `CANCELLED` (`ReservationService.java:107`) antes de `calculateRefund`, que pergunta se a reserva está `PENDING` (`:118`). O ramo nunca roda: o resultado da pendente só sai certo porque `amountPaid` é zero.
- **A regra de pagamento.** A confirmação recebe o valor pago do cliente (`ReservationService.java:44`, `:55`), quando o texto diz que o pago é o total da reserva.
- **Formatação dependente da máquina.** `String.format("%.2f", ...)` usa o locale padrão (`EmailService.java:70-72`, `ReportService.java:160-162`): o comprovante não tem o separador de milhar pedido ("R$ 1.234,56" sairia "R$ 1234.56" ou "1234,56"), e o CSV pode sair com vírgula decimal numa máquina em português.
- **Tipos e estado.** `BigDecimal` com `HALF_EVEN` a cada passo (`PriceUtil.java:7-8`, `PricingService.java:31-41`) e `LocalDate` estão certos. Mas `PricingResult` tem campos públicos mutáveis (`PricingService.java:11-16`), `Reservation` é um saco de setters (`model/Reservation.java:42-168`) e `RatePlan`/`RoomType` repetem o `name()` do enum num campo `name` (`RatePlan.java:9`, `RoomType.java:10`).

## Testes

**Números:** 16 testes rodaram, 0 falharam, 0 pulados; 16 métodos escritos. Cobertura de linhas 62 de 843 (7,4%) e de ramos 14 de 198 (7,1%). Os pacotes `api`, `repository`, `config` e `com.pousada` têm 0% (`measures/tests.md`).
**Nota:** ruim

Os testes que existem conferem com valores o preço e o arredondamento: alta temporada com 599,76 (`PricingServiceTest.java:29-41`), o desconto NON_REFUNDABLE com 74,97 / 424,83 (`:44-56`), a suíte em dezembro (`:74-86`) e as bordas da temporada em 14 e 15 de dezembro (`DateUtilTest.java:19-20`). Isso é tudo. Não há nenhum teste para:
- a situação da reserva (confirmar, check-in, check-out e as recusas 409);
- o cancelamento e a devolução;
- as recusas da API (400, 404, 409, 422);
- o comprovante e o e-mail;
- **a parte nova do cenário**: nenhum dos quatro relatórios, nem a linha de título, a taxa de R$ 4,50 ou a linha por plano.

A pergunta da mutação, para a parte nova: se `TOURISM_TAX_PER_GUEST_PER_NIGHT` (`ReportService.java:19`) virasse 5.40, se o título trocasse `tourism-tax` por outra coisa (`:87`), ou se a ordem dos planos se invertesse, nenhum teste falharia. Nenhum teste sobe a API ou toca o banco. Por isso o `mvn verify` passa com um sistema que responde 500 em quase todas as rotas: os testes não chegaram a ver o defeito que derruba tudo.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` nas 6 colunas, datas em `date`, a chave estrangeira `reservations.room_number` com índice. Nenhum CHECK, UNIQUE ou EXCLUDE. 0 transações explícitas e 0 `FOR UPDATE`. 1 SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

Os tipos estão certos e o serviço sobe contra um banco vazio e cria o que precisa, com os quartos semeados de forma idempotente (`RoomRepository.java:13-22`, `ON CONFLICT DO NOTHING`). O SQL usa parâmetros e try-with-resources com pool (`ReservationRepository.java:23-24`). O único SQL com `+` (`RoomRepository.java:53`) concatena um `int`, então não abre injeção, mas foge do padrão do resto. Os defeitos:

- **O esquema não evolui.** É um `CREATE TABLE IF NOT EXISTS` dentro do Java (`DatabaseConfig.java:32-70`), sem migrações: a primeira coluna nova numa tabela existente não será aplicada.
- **Nada protege contra duas reservas na mesma noite.** O serviço consulta a disponibilidade (`ReservationRepository.java:89-112`) e depois insere (`ReservationService.java:28-41`) em conexões separadas, sem transação, sem trava e sem restrição `EXCLUDE` no banco. Dois pedidos simultâneos podem passar os dois. O roteiro não conseguiu medir isso porque todo POST deu 500, mas a corrida está no código.
- **Sem CHECK nas colunas de situação, plano, tipo e canal**, nem em `guests >= 1` ou `check_out > check_in`.
- **A confirmação e o e-mail não andam juntos.** São duas escritas separadas sem transação (`ReservationService.java:57-58`): se a gravação da mensagem falhar, a reserva fica confirmada sem e-mail.

## Comportamento

**Números:** 11 de 64 conferências passaram. Parte comum: 4 de 46. Dados depois de reiniciar: 1 de 3. Cenário 09 (relatórios): 5 de 13. Reservas simultâneas: 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

Na prática o serviço sobe mas não funciona. `GET /rooms`, `POST /reservations`, as recusas (400, 404, 409, 422) e as chamadas com id dão 500, porque o Javalin não tem mapeador JSON (`measures/behavior-service.log:25`). Nenhuma reserva chega a existir, e por isso caem também a confirmação, o e-mail, o check-in, o check-out, os cancelamentos e a segunda via. As reservas simultâneas deram 500 nos dois pedidos nas 10 rodadas: "nenhuma com as duas criadas" passa só porque nada é criado. O que passou: as linhas de título dos relatórios, os cabeçalhos e o `text/csv`, porque os relatórios usam `ctx.result` e não o JSON. Mas eles sempre saem vazios, e mês fora do formato dá 500 em vez de 400 (o `ApiException` relançado em `ReportController.java:29` cai no handler de `Main.java:58-61`, que também chama `ctx.json`).

Pela leitura, mesmo com o JSON funcionando, sobrariam estes erros:
- a confirmação exige `amountPaid` no corpo e grava o que o cliente mandar (`ReservationController.java:64`), e confirmar sem corpo daria erro;
- a segunda via sai para reserva pendente ou cancelada (`ReservationController.java:105-117`);
- o financeiro e o de planos deixam fora a NON_REFUNDABLE cancelada que ficou com o dinheiro (`ReservationRepository.java:205`), justo o caso que o texto destaca;
- as linhas de total do financeiro e da taxa saem com colunas vazias (`total,,,,0.00`, `total,,,,,0.00`; `ReportService.java:78`, `:109`), quando o texto pede `total` e o valor.

Para o dono da pousada: o site não consegue reservar, a recepção não consegue confirmar nem fazer check-in, e os relatórios saem sempre zerados.

## Dúvidas

- A falha "cinco quartos (veio: 4)" no roteiro parece ser a contagem feita sobre a página de erro do Javalin, não sobre uma lista de quartos: o `GET /rooms` respondeu 500.
- Não pude observar a corrida das reservas simultâneas pela API, porque todo POST falhou. A nota do banco vem da leitura do código (verificação seguida de inserção, sem transação nem restrição).
- A forma da linha de total com colunas vazias (`total,,,,0.00`) poderia ser lida como uma escolha de alinhar as colunas do CSV. O texto diz "uma linha 'total' com a soma" e, para a taxa, "a última linha é 'total' e o valor", então tratei como desvio, mas ele pesa pouco perto do resto.
