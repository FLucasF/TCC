# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin com Gson em sete pacotes: `api` (uma classe `ApiRoutes` com todas as rotas),
`services` (preço, reservas, notificações e relatórios), `repositories` (JDBC puro com `DriverManager`), `database`
(cria as tabelas com `CREATE TABLE IF NOT EXISTS` e grava os quartos), `models` e `exceptions`. Uma requisição entra em
`ApiRoutes`, que lê o JSON à mão, chama o `ReservationService`, e este usa os repositórios e o `NotificationService`. Os
dados ficam em três tabelas do PostgreSQL. O defeito mais sério é que **o sistema não sobe**:
`NotificationService.java:10` cria um `DateTimeFormatter.ofPattern("R$ #,##0.00")` (um formatador de datas usado
como se fosse de dinheiro, e que nem é usado), o Java recusa o padrão ("Unknown pattern letter: R") ao carregar a classe,
e o `main` cai com `ExceptionInInitializerError` em `Application.java:34`. O `mvn verify` passa só porque o único
teste nunca carrega essa classe. Por isso os scripts do banco e do comportamento não conseguiram medir nada pela API.
Além disso, a leitura mostra a alta temporada errada (dezembro inteiro), a confirmação que exige um `amountPaid` no
corpo, que o texto não prevê, e as notificações que pedem um Observer escritas como um notificador único com `if` por
destino.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `com.pousadamarealta` e `api`; o JDBC só em `database` e `repositories`. A maior classe é `ApiRoutes` (328 linhas).
**Nota:** com problemas

As dependências apontam na direção certa (api → services → repositories → database), sem ciclo, e a montagem fica
num lugar só, em `Application.java:25-57`, com os construtores recebendo as peças. Os serviços não conhecem o Javalin
nem o JDBC. Mas a borda HTTP não é fina:

- `ApiRoutes` fala direto com os repositórios, passando por cima dos serviços: `api/ApiRoutes.java:63` (`roomRepository.findAll()`)
  e `api/ApiRoutes.java:231` (`messageRepository.findAll()`); o construtor recebe três repositórios
  (`api/ApiRoutes.java:29-45`).
- Há regra de negócio no handler. A recusa da segunda via para reserva pendente está em `api/ApiRoutes.java:201`, e
  não no serviço. O tipo do quarto é deduzido do número em `api/ApiRoutes.java:307-311`, ignorando o `Room.type` que
  está no banco. O comprovante é montado duas vezes no handler (`api/ApiRoutes.java:174-186` e `:207-219`).
- Uma regra do relatório financeiro (só entra a reserva que deixou dinheiro, `pago - devolvido > 0`) mora no
  repositório, em `repositories/ReservationRepository.java:163-169`, enquanto o mesmo cálculo é repetido no
  `ReportService.java:60-63`.
- `ApiRoutes` concentra toda a borda (328 linhas) com um `try/catch` copiado em cada handler.

São misturas localizadas, que não chegam a desmontar as camadas.

## Design de baixo nível

**Números:** 5 destinos de aviso (hóspede na confirmação, governança e financeiro na confirmação e no cancelamento),
os 5 dentro de uma única classe `NotificationService`, escolhidos por `if` sobre texto. Nenhuma interface de ouvinte,
nenhuma lista de interessados.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede um Observer: o serviço avisa que a reserva foi confirmada ou cancelada sem saber quem
recebe o aviso, e o restaurante e o transfer entram e saem com o tempo. O agente fez o que a referência chama
de omissão: um "notificador" único que conhece todos os destinos. `services/NotificationService.java:17-21` chama, uma
depois da outra, `notifyGuest`, `notifyGovernance` e `notifyFinance`. `:23-30` faz o mesmo no cancelamento. Cada
método escolhe o texto por uma `String` mágica (`"confirmed"`, `"cancelled"`, `"payment_received"`, `"refund"`, em
`:51`, `:60`, `:79` e `:85`), e os endereços estão fixos no código (`:72`, `:95`). O `ReservationService` chama esse
notificador diretamente (`services/ReservationService.java:98` e `:157-159`). Para pôr o restaurante, é preciso editar
`NotificationService`. Nada se registra na montagem (`Application.java:34`). A condição "só se estava confirmada"
também aparece duas vezes (`ReservationService.java:157` e `NotificationService.java:24`).

**Exagero.** Não há estrutura sem necessidade: nada de interfaces com uma implementação só, fábricas ou camadas que só
repassam.

**O resto do código.** Os defeitos são espalhados:

- `services/NotificationService.java:10`: uma constante de dinheiro feita com `DateTimeFormatter`, que não é usada e
  derruba o sistema ao carregar a classe. É o defeito que impede o serviço de subir.
- `services/NotificationService.java:121-137`: `formatCurrency` usa `String.format("%.2f")`, que depende do locale da
  máquina. Num locale pt_BR ele gera "779,80", o `split("\\.")` não acha o ponto e `parts[1]` estoura. O mesmo
  `%.2f` aparece no CSV (`services/ReportService.java:65` e `:77`), que sairia com vírgula.
- Dinheiro vai para o JSON como `double` (`api/ApiRoutes.java:294-300`, `:184-186`, `:217-219`). Assim 509.80 sai como
  `509.8`, e o texto pede 2 casas.
- Duplicação: o tipo do quarto deduzido do número em três lugares (`api/ApiRoutes.java:307-311`,
  `services/ReportService.java:70-74`, `services/NotificationService.java:106-107`), o comprovante JSON duas vezes
  (`api/ApiRoutes.java:174-186` e `:207-219`) e o mesmo `try/catch` em cada handler.
- Os erros: cada handler termina em `catch (Exception e)` → 400 `INVALID_REQUEST` (`api/ApiRoutes.java:74, 107, 119,
  140, ...`). Assim uma falha do banco vira "pedido inválido" e a causa some. O mesmo acontece em
  `services/ReservationService.java:181-183`.
- A regra de alta temporada está errada: `services/PricingService.java:16` considera dezembro inteiro, e o texto diz a
  partir de 15 de dezembro.
- Restos sem uso: o parâmetro `roomNumber` de `validateReservationInput` (`services/ReservationService.java:164`), o
  `import DateTimeFormatter` em `api/ApiRoutes.java:17` e a `CREATE SEQUENCE` em `database/DatabaseConfig.java:74`.

Os tipos estão bem escolhidos (`BigDecimal` com `HALF_EVEN` em `PricingService`, `LocalDate`, `enum` para status e
plano), mas o conjunto é difícil de mudar com segurança, e um dos defeitos impede o sistema de rodar.

## Testes

**Números:** 13 testes rodaram, 0 falhas, 0 erros, 0 pulados; 13 métodos escritos, todos em
`PricingServiceTest`. Cobertura de linhas 21 de 708 (3,0%), de ramos 14 de 170 (8,2%). A API, os repositórios, o banco,
`ReservationService`, `NotificationService` e `ReportService` têm 0% (`measures/tests.md`).
**Nota:** ruim

Os testes que existem conferem valores: a noite de alta temporada R$ 299,88
(`src/test/java/com/pousadamarealta/services/PricingServiceTest.java:45-49`), as duas noites R$ 599,76 (`:58-65`), o
desconto de R$ 89,96 (`:67-72`), o total de R$ 509,80 (`:81-87`) e o arredondamento do DELUXE (`:97-102`). Mas
cobrem só o preço. Ficaram sem nenhum teste:

- a parte nova do cenário, os avisos à governança e ao financeiro. Se alguém apagasse `notifyFinance` ou trocasse
  `> 0` por `>= 0` em `NotificationService.java:26`, nada falharia;
- as situações da reserva, o cancelamento e a devolução, as recusas e os códigos de erro, os relatórios e o
  comprovante.

O teste de dezembro usa o dia 15 (`PricingServiceTest.java:20-22`), e por isso não pega o erro de considerar
dezembro inteiro. Nenhum teste sobe o sistema ou carrega `NotificationService`, e assim o `mvn verify` passa com um jar
que não sobe.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, as datas em `date`, e a chave estrangeira de `reservations.room_number` tem índice. Nenhum CHECK,
UNIQUE ou EXCLUDE. Não há pool, transação explícita nem `FOR UPDATE`, e o script não achou SQL montado com `+`. **O
serviço não subiu contra o banco vazio** (`ExceptionInInitializerError` em `NotificationService.java:10`,
`measures/database.md`); as tabelas foram criadas antes da queda.
**Nota:** ruim

A skill põe "o serviço que não sobe contra um banco vazio" na nota `ruim`, e foi o que aconteceu. A causa não está
no banco, mas o banco do sistema entregue não chega a ser usado. Sobre o próprio esquema e o código de dados:

- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no Java (`database/DatabaseConfig.java:20-78`), sem migrações.
  Esse método não sabe acrescentar uma coluna numa tabela que já existe.
- O tipo e os valores estão certos, e a chave estrangeira está no lugar (`database/DatabaseConfig.java:50`), mas
  status, plano e tipo são `varchar` sem CHECK, e `guests > 0` não é protegido.
- Nada impede duas reservas na mesma noite: o serviço consulta (`repositories/ReservationRepository.java:117-141`) e
  depois insere, sem trava nem transação e sem restrição `EXCLUDE`. Dois pedidos simultâneos passam os dois.
- A confirmação grava a reserva e depois as mensagens em conexões separadas (`services/ReservationService.java:96-98`),
  sem transação. Uma falha no meio deixa a reserva confirmada sem os avisos.
- Uma conexão nova por chamada, via `DriverManager` (`database/DatabaseConfig.java:17`), sem pool. Há um `ResultSet`
  sem fechar (`database/DatabaseConfig.java:84`) e uma `CREATE SEQUENCE` que não serve para nada (`:74`).
- O SQL usa parâmetros `?` em todo lugar.

## Comportamento

**Números:** nenhuma conferência rodou. O roteiro `check_03.sh` empacotou o projeto, mas o serviço não subiu contra
o PostgreSQL vazio (`measures/behavior.md`: `IllegalArgumentException: Unknown pattern letter: R` em
`NotificationService.<clinit>` (`NotificationService.java:10`)). Ficam 0 OK e 0 FALHOU, em todas as partes.
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: `java -jar` encerra na hora, e o site não consegue listar
quartos, criar ou confirmar reservas, nem tirar relatórios. Pela leitura do código, mesmo que essa linha fosse
corrigida, outras falhas apareceriam:

- **A confirmação exige um `amountPaid` no corpo.** `api/ApiRoutes.java:130-131` responde 400 sem ele, e grava o valor
  que o cliente mandar (`services/ReservationService.java:95`). O texto diz que o pago é o total da reserva. O site,
  chamando como foi combinado, não conseguiria confirmar nada, e com isso nenhum e-mail sairia.
- **De 1 a 14 de dezembro sairia 20% mais caro** (`services/PricingService.java:16`).
- **A entrada igual à saída seria aceita**, com 0 noites, em vez do 400 (`services/ReservationService.java:170` usa
  `isAfter`).
- **A segunda via sairia para reserva cancelada**, porque só a pendente é recusada (`api/ApiRoutes.java:201`).
- **O dinheiro iria no JSON sem as 2 casas** (`509.8`), e os CSVs dependeriam do locale da máquina.
- **Duas reservas simultâneas para o mesmo quarto** passariam as duas.

Os avisos do cenário, se a confirmação fosse alcançável, teriam os assuntos e os destinos pedidos
(`NotificationService.java:52`, `:61`, `:72`, `:80`, `:86`, `:95`), e a pendente cancelada não avisaria ninguém
(`ReservationService.java:157`).

## Dúvidas

As falhas listadas no comportamento, além da que impede o serviço de subir, vêm da leitura do código. O roteiro não
pôde confirmá-las porque o serviço não sobe, e eu não alterei o projeto para contornar a queda.
