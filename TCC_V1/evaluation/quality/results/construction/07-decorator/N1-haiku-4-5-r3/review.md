# Revisão: construção, cenário 07

## Visão geral

O projeto tem seis pacotes: `Application` (as rotas do Javalin e a conversão para JSON, 319 linhas), `service` (reserva, mensagens, relatórios, comprovante), `db` (repositórios JDBC estáticos e a criação do esquema), `model`, `util` (preço, formatação, validação de e-mail) e `api` (o corpo de erro). Uma requisição entra numa lambda de `Application.main`, que lê o corpo com Gson, chama um método estático do serviço (ou do repositório direto) e responde com `ctx.json`. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` em `db/Database.java:16-62`. O mais sério: **o sistema não funciona pela API**. `Javalin.create().start(port)` (`Application.java:35`) sobe o servidor antes de registrar as rotas, e o registro de `app.get("/reservations/:id", ...)` (`Application.java:83`) lança `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'` (`measures/behavior-service.log:19`). A thread `main` morre e só ficam de pé `GET /rooms` e `POST /reservations`. Essas duas também respondem 500, porque o projeto não tem Jackson (`pom.xml:23-50` só traz Gson) e todo `ctx.json` falha com "you don't have an object mapper configured". O roteiro passou em 3 de 65 conferências. A parte que o cenário pede (as regras de envio) virou três `if` dentro de `MessageService.sendMessage`, sem estrutura nenhuma. E os únicos testes (9) cobrem só `util`, com 4,5% das linhas.

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, nenhum ciclo. `Application` depende de 4 pacotes, `service` de 3 (`db`, `model`, `util`). Javalin só no pacote raiz, JDBC só em `db`. A maior classe é `Application.java` (319 linhas), seguida de `ReservationRepository.java` (183).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: os serviços não conhecem o Javalin nem o JDBC (`service/ReservationService.java:1-10`), e o SQL fica em `db/`. Mas a separação vaza em vários pontos:

- **Handlers que pulam o serviço e carregam regra.** `GET /reservations/{id}` e a segunda via chamam `ReservationRepository.findById` direto (`Application.java:86`, `Application.java:161`). A regra "a segunda via só existe para reserva paga" mora no handler (`Application.java:168-172`). O check-out e a segunda via montam o `Document` dentro da rota, com `RoomRepository.findByNumber` (`Application.java:145-148`, `Application.java:174-177`). `GET /rooms` também vai direto ao repositório (`Application.java:39`).
- **Validação repetida em duas camadas:** a saída depois da entrada é conferida em `Application.java:66` e de novo em `ReservationService.java:23`.
- **Sem montagem.** Tudo é `static`: os repositórios, os serviços e a configuração do banco lida em campos estáticos (`db/Database.java:6-8`). O modo de teste é lido num `static final` dentro do serviço (`service/MessageService.java:13`). Não há um lugar que crie e ligue as peças, e é justamente isso que o cenário precisaria para combinar as regras de envio por instalação.
- **`Application` faz de tudo:** rotas, leitura do corpo, mapeamento de erro por `switch` de texto (`Application.java:254-280`) e serialização manual (`Application.java:282-318`).

Não há ciclo, e a direção borda → regras → dados se mantém. Por isso a nota é `com problemas`, e não `ruim`.

## Design de baixo nível

**Números:** das 3 regras de envio, 0 numa estrutura própria. As 3 (endereço inválido, modo de teste, cópia para o arquivo) são `if` dentro de um único método, `MessageService.sendMessage` (`service/MessageService.java:15-41`). Não há interface de envio nem embrulho.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede regras que se empilham em volta do envio e mudam por modo e por instalação (a segunda pousada sem a cópia, as regras de madrugada e de limite por dia "sempre, só no teste ou só fora dele"). O agente escreveu os três casos à mão num método só:

- o bloqueio em `MessageService.java:18-21`;
- o desvio do modo de teste em `MessageService.java:27-31`;
- a cópia condicionada ao modo em `MessageService.java:35-38` (`!IS_TEST_MODE`).

A combinação por modo está fixa dentro do envio, e o modo vem de um `static final` lido do ambiente (`MessageService.java:13`). Desligar a cópia para a segunda pousada, ou acrescentar a regra da madrugada, obriga a editar esse método e a misturar mais condições. É o caso que a referência chama de omissão: os três `if` dentro do envio. Nada aproxima o código do Decorator, nem uma interface de envio. A condição `status == MessageStatus.SENT` em `MessageService.java:35` é sempre verdadeira nesse ponto, porque o caso bloqueado já retornou na linha 20.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma camada que só repassa. Contenção.

**O resto do código.** Os defeitos estão espalhados:

- **Erros como texto.** Os serviços sinalizam o erro com `IllegalArgumentException("ROOM_NOT_FOUND")` e similares (`ReservationService.java:16`, `:20`, `:28`, `:50`, `:54`...). A borda traduz a mensagem num `switch` de strings (`Application.java:254-280`), e qualquer `IllegalArgumentException` de outra origem cai no `default` como 400.
- **`catch (Exception)` que esconde a causa.** Em `POST /reservations` e na confirmação, qualquer falha, inclusive do banco, vira `400 INVALID_REQUEST "Invalid request format"` (`Application.java:77-80`, `Application.java:109-112`). Nos relatórios, também (`Application.java:214-217`, `Application.java:234-237`).
- **Duplicação.** A montagem do `Document` está copiada (`Application.java:146-148` e `Application.java:175-177`), e a conferência das datas também (`Application.java:66`, `ReservationService.java:23`). O relatório de ocupação faz três consultas iguais, uma por situação, e as concatena (`ReportService.java:15-20`), o que quebra a ordem por número de quarto pedida.
- **Tipos.** O dinheiro é calculado em `BigDecimal` com `HALF_EVEN` (`util/PriceCalculator.java:11-13`), o que está certo, mas sai no JSON como `double` (`Application.java:294-298`), e `0.00` vira `0.0`, contra o texto, que pede 2 casas. O plano de tarifa entra em `calculateDiscount` como `String` comparada com `"NON_REFUNDABLE"` (`PriceCalculator.java:38-39`; a chamada é `ratePlan.name()` em `ReservationService.java:40`), embora o `enum RatePlan` exista. A segunda via decide "reserva paga" por `amountPaid > 0` (`Application.java:168`), e não pela situação, então uma STANDARD cancelada e reembolsada ainda emite comprovante.
- **Código morto ou enganoso.** `PriceCalculator.calculateSubtotal` não é usado (`PriceCalculator.java:34-36`). O parâmetro `status` de `findByMonthAndStatus` é ignorado (`db/ReservationRepository.java:141-146`). `validateRequiredFields` declara `throws Exception` sem precisar (`Application.java:246`).
- **Estado global.** Tudo é `static` (os serviços, os repositórios, o `Gson` e a configuração), o que impede trocar uma peça, como o envio, sem editar quem a usa.

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 erros, 0 pulados. São 9 métodos escritos, em 2 arquivos (`EmailValidatorTest`, `PriceCalculatorTest`). Cobertura: 29 de 643 linhas (4,5%) e 26 de 174 ramos (14,9%). `service`, `db`, `model` e `Application` têm 0% (de `measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que está testado tem valores:

- a temporada (`src/test/java/com/pousada/marealta/util/PriceCalculatorTest.java:11-30`);
- a noite de alta temporada R$ 299,88 (`PriceCalculatorTest.java:33-40`);
- o meio para o par (`PriceCalculatorTest.java:43-51`);
- o desconto de 15% (`PriceCalculatorTest.java:54-58`);
- a validação de e-mail com os exemplos do texto (`EmailValidatorTest.java:8-24`).

Todo o resto fica sem rede:

- a criação da reserva e a soma das noites (`ReservationService.java:31-41`);
- as transições de situação e os 409;
- o cancelamento e a devolução (`ReservationService.java:102-107`);
- o conflito de datas (`ReservationRepository.java:101-118`);
- o comprovante e o e-mail (`DocumentService.java`, `ReservationService.java:120-128`);
- os dois relatórios;
- os códigos de erro.

Da parte nova do cenário, só a validação de endereço tem teste. A pergunta da mutação deixa isso claro: apagar a cópia para o arquivo (`MessageService.java:35-38`), trocar `!IS_TEST_MODE` por `IS_TEST_MODE`, ou tirar o prefixo `"[TESTE] "` (`MessageService.java:29`) não derruba nenhum teste. Também não há teste que suba a aplicação, e por isso o `mvn verify` passa com um serviço que morre ao registrar as rotas. A coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` nas 6 colunas, datas em `date`. Uma chave estrangeira sem índice: `reservations(room_number) -> rooms`. Nenhum CHECK, UNIQUE ou EXCLUDE. Esquema criado por `CREATE TABLE` no código, sem migração. Sem pool (1 `DriverManager.getConnection`), 0 transações explícitas, 0 `FOR UPDATE`. Um candidato de SQL montado com `+` (de `measures/database.md`).
**Nota:** com problemas

O que está certo:

- os tipos: dinheiro em `NUMERIC(10,2)` e datas em `DATE` (`src/main/java/com/pousada/marealta/db/Database.java:21`, `:30-31`, `:35-40`);
- `NOT NULL` no que é obrigatório;
- a reserva ligada ao quarto por chave estrangeira (`Database.java:26`);
- os quartos semeados com `ON CONFLICT DO NOTHING` (`Database.java:52-61`);
- as conexões fechadas por try-with-resources em todos os repositórios.

O serviço sobe contra um banco vazio e cria tudo (`measures/database.md`). As mensagens sobrevivem ao reiniciar (`measures/behavior.md`, "as mensagens continuam lá").

Os defeitos:

- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` em código (`Database.java:16-50`), sem migração versionada: a próxima coluna nova não chega a um banco que já existe.
- **A regra de uma reserva por quarto e noite só existe como "consulta, depois insere".** São duas chamadas separadas, em conexões diferentes e sem transação nem trava (`ReservationService.java:27` → `ReservationRepository.java:101-118`, depois `ReservationRepository.java:16-47`). Não há restrição no banco (nenhum EXCLUDE), então dois pedidos simultâneos podem criar as duas reservas. O roteiro não chegou a provar isso, porque todos os pedidos deram 500 antes.
- **Confirmação não atômica.** A mudança de situação (`ReservationService.java:57`) e a gravação do e-mail (`ReservationService.java:59`) não estão numa transação: se o envio falha, a reserva fica confirmada sem mensagem e o cliente recebe erro.
- **Detalhes.** A chave estrangeira não tem índice. Situação, plano, tipo e canal são `varchar` sem CHECK. Cada chamada abre uma conexão nova pelo `DriverManager` (`Database.java:10-12`), sem pool. `RoomRepository.java:34` monta o SQL com `+`, mas o valor concatenado é um `int`, então não há injeção; é só má prática ao lado dos `PreparedStatement` do resto. O relatório financeiro filtra o mês com `EXTRACT(...)` sobre `check_in` (`ReservationRepository.java:144`), o que impede o uso de índice: vale uma nota, não derruba a coluna.

## Comportamento

**Números:** 3 de 65 conferências passaram (de `measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 1 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário, modo normal | 0 de 9 |
| cenário, modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O sistema não atende o site. A thread `main` morre no registro de `/reservations/:id` (`Application.java:83`; `measures/behavior-service.log:19`, "Javalin 4 switched from ':param' to '{param}'"). Por isso nenhuma rota com `{id}` nem os relatórios existem: confirmação, check-in, check-out, cancelamento, segunda via e `/reports/*` respondem 404 "Endpoint ... not found". As duas rotas que chegam a ser registradas respondem 500, porque não há mapeador de JSON (sem Jackson no `pom.xml`). Assim, `GET /rooms` dá 500 e `POST /reservations` dá 500 até nas recusas (409, 422, 404 e 400 viram 500). As três conferências que passaram não provam nada: o `refund` "null" vem de uma resposta de erro, "as mensagens continuam lá" compara duas listas vazias, e "nenhuma rodada com as duas criadas" vem de todos os pedidos terem falhado.

Para o dono da pousada, isso quer dizer que o site não consegue listar quartos, criar, confirmar nem cancelar reservas. A recepção não faz check-in nem check-out, o contador não tira relatório, e nenhum e-mail é gerado, nem bloqueado, nem copiado, nem desviado no modo de teste.

Pela leitura, mesmo com as rotas e o JSON consertados, sobrariam desvios do texto:

- a confirmação exige `amountPaid` no corpo (`Application.java:103`), quando o texto diz que o pago é o total;
- a segunda via de reserva pendente responde 404 (`Application.java:168-171`), e a cancelada com devolução ainda emite comprovante;
- a ocupação não sai em ordem de quarto (`ReportService.java:15-20`);
- o financeiro lista canceladas com valor 0 (`ReportService.java:42-51`; a consulta inclui todas as `CANCELLED`, `ReservationRepository.java:145`) e a linha do total sai `total,,,,X` (`ReportService.java:63`);
- o dinheiro sai como `double`, com `0.0` em vez de `0.00` (`Application.java:298`).

A regra de envio do código (`MessageService.java:15-41`) bate com o texto nos três casos, mas nunca chega a rodar pela API.

## Dúvidas

- A segunda via de reserva não paga: o texto não dá o código. O roteiro espera 409 `OPERATION_NOT_ALLOWED`, e o agente escolheu 404 `RESERVATION_NOT_FOUND`. Isso não pesou na nota, que já é decidida pela queda do serviço.
- A linha do total do financeiro com colunas vazias (`total,,,,599.76`) é uma leitura possível de "uma linha total com a soma". Também não pesou.
- A corrida entre duas reservas simultâneas não foi medida pelo roteiro (todos os pedidos deram 500). O risco vem só da leitura do código.
