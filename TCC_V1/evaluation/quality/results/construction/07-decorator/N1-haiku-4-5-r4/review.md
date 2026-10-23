# Revisão: construção, cenário 07

## Visão geral

Os caminhos de código citados abaixo são relativos a `projeto/src/main/java/com/pousadamarealta/`. Os de teste vêm com o caminho a partir de `projeto/`. O projeto tem 7 pacotes (`api`, `service`, `dao`, `db`, `models`, `util` e o `Main`) e cerca de 1.180 linhas. Uma requisição entra em `ApiRoutes` (uma classe estática com todas as rotas), passa por serviços estáticos (`ReservationService`, `MessageService`, `ReportService`) e chega a DAOs estáticos que usam JDBC puro. A conexão vem de um singleton estático, `Database`, que abre uma conexão nova por chamada com o `DriverManager` e cria o esquema com `CREATE TABLE IF NOT EXISTS` (`src/main/java/com/pousadamarealta/db/Database.java:33-77`). A parte comum funciona quase toda, e as três regras de e-mail produzem o resultado pedido nos dois modos (14 de 14 conferências). Mas:

- elas estão escritas como `if`s dentro de um único método (`service/MessageService.java:16-40`), sem nenhuma estrutura para as próximas regras que o texto anuncia: o Decorator ficou de fora;
- nada impede a reserva em dobro: em 10 rodadas de dois pedidos simultâneos, as 10 criaram as duas reservas;
- os testes cobrem 4,7% das linhas e não tocam nas regras de envio, que são a parte nova;
- o comprovante sai com o CPF embaralhado ("CPF 111..22.2.3-33-44"), no e-mail de confirmação, quando o hóspede manda o CPF já formatado.

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é `api/ApiRoutes.java`, com 285 linhas. O pacote `api` usa Javalin, Jackson e JDBC, e depende também de `dao`.
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo (api → service → dao → db), sem ciclo. O SQL está isolado nos DAOs e as regras de preço em `util/PricingCalculator.java`. Os problemas:

- **A borda HTTP tem regras de negócio e acesso direto ao banco.** A validação da reserva e a capacidade do quarto são decididas no handler (`api/ApiRoutes.java:53-89`), que chama `RoomDAO` sem passar pelo serviço (`api/ApiRoutes.java:15`, `:37`, `:77`, `:162`, `:187`). A regra de quem pode ter segunda via também está no handler (`api/ApiRoutes.java:179`, `amountPaid > 0`).
- **As regras do serviço não comunicam o motivo da recusa.** `ReservationService.createReservation` devolve `null` em quatro situações diferentes (`service/ReservationService.java:15-30`). Por isso o handler precisa repetir as verificações antes de chamar o serviço e trata qualquer `null` como `ROOM_UNAVAILABLE` (`api/ApiRoutes.java:93`).
- **Não há um lugar onde as peças são montadas.** Tudo é `static`: os DAOs, os serviços e o `Database.getInstance()` (`db/Database.java:9`, `:25`). O modo de teste é lido de uma variável de ambiente num campo estático dentro do serviço (`service/MessageService.java:14`), em vez de ser decidido no `Main` (`Main.java:8-25`, que só lê as variáveis de ambiente e registra as rotas). É exatamente o que impediria a "segunda pousada" de ter outra combinação de regras sem mexer no código.

## Design de baixo nível

**Números:** 3 regras de envio pedidas, nenhuma numa estrutura própria. As 3 estão em `if`s dentro de `MessageService.sendEmail`, e a validação do endereço aparece duas vezes. Não existe interface de envio nem embrulho. As regras anunciadas (madrugada, limite por dia, combinação por modo e por instalação) não têm onde entrar.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve três comportamentos em volta do mesmo envio, que se combinam por modo, e anuncia outros que "podem valer sempre, só no modo de teste ou só fora dele", com combinações diferentes por instalação. Todos os três foram escritos dentro de um único método estático:

- o bloqueio, em `service/MessageService.java:22-23`;
- o desvio para a caixa de testes, em `:26-30`;
- a cópia para o arquivo, em `:35-36`, condicionada a `!TEST_MODE` e validando o endereço de novo.

O modo é uma constante estática lida do ambiente (`:14`). Não há interface de envio, nenhum embrulho e nenhuma montagem. Para acrescentar "não mandar de madrugada só fora do teste", seria preciso abrir esse método e somar mais um `if` ao que já existe. Para tirar a cópia na segunda pousada, seria preciso mudar o código. É a omissão descrita na referência: os três `if`s dentro do envio.

**O exagero.** Nenhum: não há interfaces sem uso, fábricas ou camadas que só repassam. A classe `util/PriceRounding.java` tem um método só, mas é usada por todo o cálculo de preço.

**O resto do código.**
- **O `null` faz o papel de erro.** Os métodos de `ReservationService` devolvem `null` tanto para "não existe" quanto para "não pode" (`service/ReservationService.java:55-57`, `:80-82`, `:91-93`, `:100-104`). Com isso, confirmar, cancelar ou fazer check-in de uma reserva que não existe responde 409 `OPERATION_NOT_ALLOWED`, e não 404 (`api/ApiRoutes.java:121-124`; conferido com `curl`: `POST /reservations/99/confirmation` → 409).
- **Uma exceção genérica esconde a causa.** O `catch (Exception e)` de `api/ApiRoutes.java:101-104` transforma qualquer falha, inclusive uma `SQLException` do banco, num 400 `INVALID_REQUEST`.
- **Há duplicação.**
  - O mesmo comprovante é montado em três lugares, com a mesma lista de 11 argumentos: `api/ApiRoutes.java:160-165`, `:185-190` e `service/ReservationService.java:64-69`.
  - As verificações de hóspedes e capacidade estão no handler (`api/ApiRoutes.java:71`, `:84`) e de novo no serviço (`service/ReservationService.java:24-26`).
- **O dinheiro perde o tipo na saída.** Ele é `BigDecimal` no cálculo, mas vai para o JSON como `double` (`api/ApiRoutes.java:260-265`, `:280-282`, `:43`), e por isso sai `0.0` e `779.8`, não `0.00` e `779.80`.
- **A formatação do CPF supõe 11 dígitos sem pontuação.** `service/ReceiptFormatter.java:62-68` pontua de novo um CPF que já chega formatado (veja Comportamento).
- **Há código morto:** `util/EmailValidator.java:6` (um `Pattern` nunca usado) e `util/PricingCalculator.java:18` (`calculateSubtotal`, nunca chamado).
- **O estado global é mutável:** `db/Database.java:9`.

Os defeitos estão concentrados no tratamento de erro e na montagem do comprovante. O cálculo de preço é limpo e usa `HALF_EVEN` em cada passo (`util/PricingCalculator.java:11-31`).

## Testes

**Números:** 14 testes rodaram, 0 falharam, 0 foram pulados; 14 métodos escritos. Cobertura de linhas: 28 de 601 (4,7%). Cobertura de ramos: 26 de 180 (14,4%). `api`, `service`, `dao` e `db` têm 0% (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

Os testes só conferem `PricingCalculator` e `EmailValidator`. Esses dois conferem valores certos:

- 299,88 na alta temporada (`src/test/java/com/pousadamarealta/util/PricingCalculatorTest.java:130-135`);
- desconto de 89,96 e total de 509,80 (`:178-197`);
- os endereços inválidos do texto (`src/test/java/com/pousadamarealta/util/EmailValidatorTest.java:110-117`).

Os dois "testes de integração" não chamam o serviço: copiam o laço de `ReservationService` para dentro do teste (`src/test/java/com/pousadamarealta/integration/PricingIntegrationTest.java:20-26`, `SpecificationExampleTest.java:79-85`). Se alguém apagasse o arredondamento em `service/ReservationService.java:41`, nenhum teste falharia.

Pela pergunta da mutação na parte nova, nenhuma destas mudanças faria um teste falhar, porque `MessageService` tem 0% de cobertura:

- apagar a cópia para o arquivo (`service/MessageService.java:35-36`);
- inverter o `!TEST_MODE`;
- tirar o prefixo "[TESTE] ".

Também ficam sem rede:
- a fronteira de 14/12 contra 15/12 (só 15/12 é testado, em `PricingCalculatorTest.java:140`; trocar `day >= 15` por `day >= 14` passaria);
- as transições de situação, o cancelamento com devolução, os códigos de erro, o comprovante e os relatórios.

O texto não pedia testes, mas o pouco que existe deixa sem rede quase tudo o que ele pede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(10,2)` e as datas em `date`. As conferências acusaram:

- 1 chave estrangeira sem índice (`reservations.room_number`);
- 5 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações, 0 `FOR UPDATE` e nenhum pool;
- esquema criado por `CREATE TABLE` no código, sem migrações (`measures/database.md`).

**Nota:** ruim

Os tipos estão certos e o SQL usa sempre parâmetros (`dao/ReservationDAO.java:13-43`; o script não achou SQL montado com dado de fora). O defeito sério é que **nada protege a regra de um quarto por noite**:

- a disponibilidade é consultada (`dao/ReservationDAO.java:95-112`) e a reserva é inserida depois (`service/ReservationService.java:20` e `:46`);
- as duas operações usam conexões separadas, sem transação, sem trava e sem restrição `EXCLUDE` no esquema (`db/Database.java:47-66`);
- não há sequer um `synchronized`.

O roteiro de comportamento mostrou o efeito: nas 10 rodadas de dois pedidos simultâneos, as 10 criaram as duas reservas. O resultado são dois hóspedes no mesmo quarto na mesma noite.

Há outras escritas que deveriam andar juntas e não andam:
- o cancelamento grava a situação e a devolução em dois comandos separados (`service/ReservationService.java:108-109`);
- a confirmação grava o pagamento e depois o e-mail, fora de uma transação (`:60`, `:72`).

Além disso:
- o esquema não evolui: `CREATE TABLE IF NOT EXISTS` (`db/Database.java:38-77`) não sabe acrescentar uma coluna numa tabela que já existe;
- a chave estrangeira `room_number` não tem índice;
- não há pool, e cada operação abre uma conexão nova com o `DriverManager` (`db/Database.java:33`);
- o relatório financeiro filtra por `EXTRACT(YEAR/MONTH FROM check_in)` (`dao/ReservationDAO.java:141-143`), que não usa índice;
- o relatório de ocupação faz uma consulta ao quarto por linha (`service/ReportService.java:22`).

## Comportamento

**Números:** 61 de 65 conferências passaram:

| parte | passaram |
|---|---|
| parte comum | 43 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário, modo normal | 9 de 9 |
| cenário, modo de teste | 5 de 5 |
| reservas simultâneas | 1 de 2 |

(`measures/behavior.md`)

**Nota:** com problemas

As regras de envio fazem o que o texto pede nos dois modos:
- bloqueio com o endereço original e sem cópia;
- cópia "[Cópia] " logo depois da original;
- desvio para testes@ com "[TESTE] " e "Destinatário original: ";
- campo `status`.

Os dados sobrevivem ao reinício. As falhas:

- **Reservas simultâneas:** nas 10 rodadas, o mesmo quarto foi vendido duas vezes para as mesmas noites. Para o dono da pousada, isso quer dizer overbooking quando dois hóspedes reservam ao mesmo tempo pelo site. A causa está no banco e pesa naquela coluna.
- **O dinheiro no JSON sai sem as 2 casas:** `"amountPaid":0.0`, `"total":779.8` (`api/ApiRoutes.java:260-264`). O texto pede "número com 2 casas", e o site pode exibir valores truncados.
- **A segunda via de uma reserva pendente** responde 404 `RESERVATION_NOT_FOUND` (`api/ApiRoutes.java:179-182`), dizendo que a reserva não existe quando ela existe. O texto não dá o código desse caso, mas 409 `OPERATION_NOT_ALLOWED` é a leitura natural.
- **A segunda via sai para uma reserva cancelada.** O texto restringe a segunda via a reservas pagas (confirmadas, com check-in ou com check-out), mas o código testa `amountPaid > 0` em vez da situação. Conferido com `curl`: uma STANDARD confirmada e cancelada recebe o comprovante, com 200.
- **O relatório financeiro lista a reserva cancelada com devolução, com 0.00**, embora ela não tenha deixado dinheiro com a pousada (`dao/ReservationDAO.java:143` filtra `amount_paid > 0`, não o líquido). A linha de total também sai como `total,,,,599.76` em vez de `total,599.76`. A soma está certa; o formato é uma escolha que o texto não fechou de todo.

Fora do roteiro, chamando a API diretamente:

- **O comprovante do e-mail sai com o CPF embaralhado** quando ele chega formatado ("111.222.333-44" vira "CPF 111..22.2.3-33-44"), por `service/ReceiptFormatter.java:62-68`. É o texto que o hóspede recebe.
- **Confirmar, cancelar ou fazer check-in de uma reserva que não existe** responde 409, e não 404 `RESERVATION_NOT_FOUND` (`api/ApiRoutes.java:121-124`).
- **Um id não numérico** responde 400 no formato do Javalin, sem o `{"error","message"}` combinado.

## Dúvidas

- Na nota do design de baixo nível, fiquei entre `com problemas` e `ruim`. Dei `com problemas` porque os defeitos (o `null` como erro, a duplicação do comprovante, o `catch (Exception)`) estão concentrados em poucos lugares de um código pequeno.
- Na nota do banco, a régua da skill lista a duplicação "protegida só dentro do processo" como `com problemas`. Aqui não há proteção nenhuma e a duplicação aconteceu nas 10 rodadas, por isso dei `ruim` pelo risco de dado errado.
- O formato da linha de total do relatório financeiro (`total,,,,599.76`) é discutível, porque o texto só mostra o exemplo da ocupação.
