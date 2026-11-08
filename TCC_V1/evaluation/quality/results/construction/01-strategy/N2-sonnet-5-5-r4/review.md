# Revisão: construção, cenário 01

## Visão geral

Projeto Java 21 + Javalin 6.7.0 + JDBC puro, em cinco pacotes: `pousada.web` (a classe `Api`, com as rotas, a leitura
do JSON e o tratamento de erros), `pousada.service` (`ReservationService`, `ReportService` e o comando
`CreateReservation`), `pousada.domain` (a reserva, os planos de tarifa, a temporada, o dinheiro e o comprovante),
`pousada.persistence` (os repositórios com SQL parametrizado e a `Database`, que abre a transação e cria o esquema) e
`pousada` (o `Main`, que monta tudo). Uma requisição entra numa rota de `web/Api.java:34-45`, que chama um método do
serviço. O serviço abre uma transação (`persistence/Database.java:20-34`), trava a linha do quarto ou da reserva com
`FOR UPDATE`, pede ao domínio a regra (o plano valida e calcula; a reserva muda de situação) e grava pelo repositório.
Os dados ficam em três tabelas do PostgreSQL, criadas por `CREATE TABLE IF NOT EXISTS` no código. Os cinco planos de
tarifa seguem um Strategy limpo, e o comportamento passou em todas as 72 conferências. Os pontos fracos são os testes
(só 6 testes de unidade, 17,9% das linhas) e o banco: o esquema é criado no código, sem migrações, e falta índice na
chave estrangeira.

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`pousada.web` e `pousada` (o `main`); o JDBC, em `pousada.persistence` e `pousada.service`. O domínio não depende de
nenhum outro pacote do projeto e é usado pelos outros 4. A maior classe tem 126 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha que lê o parâmetro, chama o serviço e responde
  (`web/Api.java:34-45`). A Api só converte o JSON e as datas (`web/Api.java:67-117`) e traduz as exceções no formato
  `{error, message}` (`web/Api.java:46-51`). Nenhuma regra de negócio mora nos handlers.
- **As regras ficam no domínio, sem Javalin e sem SQL.** As transições de situação estão em
  `domain/Reservation.java:15-33`, o preço e a temporada em `domain/Seasons.java:13-30` e cada plano na sua classe. O
  serviço só orquestra (`service/ReservationService.java:50-77`).
- **Os dados ficam isolados** nos repositórios, com todo o SQL em `persistence/`.
- **A montagem fica num lugar só,** o `Main.java:32-42`, inclusive a configuração dos planos e das empresas
  conveniadas (`Main.java:17-30`).
- **Duas ressalvas pequenas, que não pedem mudança num sistema deste tamanho:**
  - o serviço recebe e repassa `java.sql.Connection` (`service/ReservationService.java:9,99`). É por isso que o jdeps
    acusa JDBC em `service`, mas ali não há SQL: a conexão funciona como unidade de trabalho;
  - a `ApiException`, que carrega o status HTTP, mora no domínio (`domain/ApiException.java:16-21`).

## Design de baixo nível

**Números:** 5 planos de tarifa, os 5 na estrutura (`RatePlan`) e 0 fora dela. Não há nenhum `switch` nem `if` sobre
o plano no serviço, no cálculo ou no cancelamento. A seleção acontece num único ponto, um mapa por código
(`domain/RatePlanCatalog.java:11-17`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** A interface `RatePlan` (`domain/RatePlan.java:6-15`) junta as três coisas que o texto diz que cada
  plano traz: quem pode usar (`validate`), o preço (`quote`) e a devolução (`refund`). Os dois planos que já existiam
  também viraram classe:
  - `StandardPlan` (`domain/StandardPlan.java:5-9`) e `NonRefundablePlan` (`domain/NonRefundablePlan.java:5-9`);
  - o convênio, `CorporatePlan`, recebe o mapa empresa→percentual e valida o código
    (`domain/CorporatePlan.java:17-24`);
  - o pacote, `WeekendPackagePlan`, valida sexta→domingo, cobra o preço fechado por tipo de quarto e devolve metade
    (`domain/WeekendPackagePlan.java:18-34`);
  - a estadia longa, `LongStayPlan`, exige 7 noites e dá 10% (`domain/LongStayPlan.java:11-18`).

  Quem usa os planos não sabe qual plano tem nas mãos: a criação chama `plan.validate` e `plan.quote`
  (`service/ReservationService.java:57,67,71`), e o cancelamento chama `plan.refund` por meio de
  `Reservation.cancel(RatePlan)` (`domain/Reservation.java:30-33`, `service/ReservationService.java:88`). Um plano
  novo é uma classe nova mais uma linha em `Main.java:18-29`.
- **O exagero.** A classe abstrata `NightlyRatePlan` (`domain/NightlyRatePlan.java:6-22`) junta o cálculo comum
  ("soma das noites menos um percentual") a quatro dos cinco planos. É reuso real, e o pacote fica fora dela, como
  deve. `Booking` e `Quote` são records pequenos que dão nome ao que o plano recebe e ao que ele devolve. Não há
  interface com uma implementação só, nem fábrica ou camada que só repasse: é contenção.
- **O resto do código.**
  - O dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:14-16`, `domain/Seasons.java:18-30`,
    `domain/Quote.java:6-8`), as datas são `LocalDate` e a situação é um `enum` (`domain/ReservationStatus.java`).
  - A `Reservation` é imutável, e cada transição devolve uma nova (`domain/Reservation.java:53-56`).
  - Os métodos são curtos e os nomes dizem o que a coisa faz.
  - Os defeitos são pequenos:
    - o plano fica como `String` na reserva (`domain/Reservation.java:11`), e por isso `planOf` usa
      `orElseThrow()` sem mensagem (`service/ReservationService.java:107-109`);
    - `initialize` trata qualquer `IllegalStateException` como "o banco ainda não subiu" e tenta de novo 30 vezes
      (`persistence/Database.java:86-94`). Um erro de DDL demoraria cerca de 60 s para aparecer;
    - o tratador genérico usa `printStackTrace` (`web/Api.java:49`).

  Nenhum desses defeitos pede mudança antes de aceitar.

## Testes

**Números:** 6 testes rodaram, nenhum falhou nem foi pulado. Há 6 métodos de teste escritos, todos em
`src/test/java/pousada/domain/RatePlanTest.java`. A cobertura é de 74 de 414 linhas (17,9%) e de 22 de 100 ramos
(22,0%). O pacote `domain` tem 74 de 120 linhas cobertas, e `service`, `persistence` e `web` têm 0%. Não há nenhum
teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

- **O que está testado, com valores.** Os testes conferem a parte nova do cenário com os números do texto:
  - Tecnosul: 779,80 / 93,58 / 686,22, e a recusa sem código ou com código errado (`RatePlanTest.java:34-41`);
  - o pacote: 820,00, a devolução de 410,00, a recusa no sábado e a recusa com 3 noites (`RatePlanTest.java:44-51`);
  - a estadia longa: 1.749,30 / 174,93 / 1.574,37, e a recusa com 6 noites (`RatePlanTest.java:54-63`);
  - a alta temporada: 599,76 (`RatePlanTest.java:22-26`);
  - o não reembolsável devolve zero (`RatePlanTest.java:29-31`);
  - o texto do comprovante e o formato R$ 1.234,56 (`RatePlanTest.java:66-79`).

  Trocar 0.12 por 0.21, `<` por `<=` no `MIN_NIGHTS` ou a sexta por outro dia derrubaria algum teste.
- **O que fica sem rede:**
  - os percentuais da Agrovale e da Farmavida (`Main.java:23-24`). O teste monta o próprio catálogo só com a
    Tecnosul;
  - a devolução integral do convênio e da estadia longa: trocar `fullRefund` por `Money.ZERO` em
    `domain/CorporatePlan.java:24` ou `domain/LongStayPlan.java:18` não derruba nenhum teste;
  - o desconto de 15% do não reembolsável (`domain/NonRefundablePlan.java:7`);
  - as transições de situação (`domain/Reservation.java:15-33`), o cancelamento de reserva pendente, a sobreposição
    de noites (`persistence/ReservationRepository.java:73-86`);
  - os dois relatórios (`service/ReportService.java`), o e-mail, os códigos de erro da API;
  - nada passa pelo banco nem pelo HTTP.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(12,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 4 colunas de situação ou tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`);
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum pool de conexões (1 chamada a `DriverManager.getConnection`).

O SQL montado com `+` só junta constantes (`measures/database.md`).
**Nota:** com problemas

- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` dentro do código
  (`persistence/Database.java:43-73`), sem migrações versionadas. O próximo plano que precisar de uma coluna nova (o
  código da empresa, por exemplo) não tem como alterar uma tabela que já existe.
- **Os tipos e as chaves estão certos.** A reserva aponta para o quarto por chave estrangeira
  (`persistence/Database.java:52`), mas sem índice em `room_number`, a coluna que a consulta de sobreposição filtra
  (`persistence/ReservationRepository.java:75-77`).
- **As restrições.** Nenhum CHECK protege `status`, `rate_plan`, `guests > 0` ou `check_out > check_in`
  (`persistence/Database.java:50-66`): o banco aceita qualquer texto e qualquer número.
- **A reserva dupla está protegida entre processos,** e não só dentro de um. A criação trava a linha do quarto com
  `SELECT ... FOR UPDATE` (`persistence/RoomRepository.java:33-35`, chamado em
  `service/ReservationService.java:60`) antes de conferir a sobreposição e inserir, tudo na mesma transação. As
  transições também travam a reserva (`persistence/ReservationRepository.java:68-70`). A confirmação grava a reserva
  e a mensagem na mesma transação (`service/ReservationService.java:79-85`).
- **O código.** O SQL usa parâmetros e as conexões são fechadas com try-with-resources
  (`persistence/Database.java:21`). Mas cada transação abre uma conexão nova pelo `DriverManager`, sem pool.

## Comportamento

**Números:** 72 de 72 conferências passaram e nenhuma falhou (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o texto pede funciona pela API, a partir de um banco vazio:
- os preços com a temporada e o arredondamento "meio para o par";
- os códigos de erro;
- a confirmação com o e-mail e o comprovante, o check-in, o check-out e a segunda via;
- os cancelamentos com a devolução de cada plano;
- os dois relatórios em CSV;
- os três planos novos, com os valores do financeiro: R$ 686,22, R$ 820,00 e R$ 410,00, R$ 1.574,37. A Agrovale
  (R$ 639,44) e a Farmavida (R$ 584,85) também conferem.

Para o dono da pousada, isso quer dizer que as reservas, os preços, as devoluções e os relatórios saem como ele
descreveu, os dados continuam lá depois de reiniciar, e duas pessoas reservando o mesmo quarto ao mesmo tempo nunca
ficam as duas com ele.

## Dúvidas

- O `companyCode` não é guardado na reserva. Ele entra em `Booking` (`service/ReservationService.java:65-66`), mas não
  há coluna para ele (`persistence/Database.java:50-66`). O texto não pede que a API o devolva, e a devolução do
  convênio não depende dele, mas a pousada perde o registro de qual empresa deu o desconto. Contei isso como
  limitação, não como defeito.
- A `ApiException`, com status HTTP, no pacote do domínio (`domain/ApiException.java`) e a `Connection` repassada pelo
  serviço são escolhas de gosto num sistema deste tamanho. Não pesaram na nota de arquitetura.
