# Revisão: construção, cenário 05

## Visão geral

O projeto tem 7 pacotes em camadas convencionais: `Main` registra as rotas, que apontam para métodos estáticos dos
handlers (`handler/`). Os handlers leem o JSON com Gson e chamam os serviços (`service/`). Os serviços chamam os DAOs
(`dao/`), e os DAOs fazem JDBC sobre uma única `Connection` estática, guardada em `config/DatabaseConfig.java:13`. Os
dados ficam em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`DatabaseConfig.java:36-90`). Os
textos dos documentos ficam em `util/DocumentFormatter.java`, e o cálculo de dinheiro em `util/MoneyUtil.java`.

O serviço sobe, mas o sistema não funciona por dois defeitos, e cada um sozinho já basta para derrubá-lo:

- **Falta um conversor de JSON.** O `pom.xml` não traz o Jackson, e todo `ctx.json(...)` responde 500 com a mensagem
  do Javalin "It looks like you don't have an object mapper configured" (`measures/behavior.md`).
- **A conexão é fechada no primeiro uso.** Todo DAO abre a conexão compartilhada num try-with-resources
  (`dao/RoomDAO.java:21`, `dao/ReservationDAO.java:19`, `dao/MessageDAO.java:17`). Ao fim do primeiro acesso, ela
  fica fechada, e a partir daí `DatabaseConfig.getConnection()` lança "Database connection not initialized"
  (`DatabaseConfig.java:112-116`). Essa mensagem aparece 26 vezes em `measures/behavior-service.log`.

Por isso só 3 de 83 conferências do roteiro passaram. Além disso, não há nenhum teste, e a escolha do documento por
tipo de hóspede é um `if/else` copiado nos três lugares onde o documento sai.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`com.pousada` e `handler`, e o JDBC só em `config` e `dao`. A maior classe é `ReservationHandler`, com 385 linhas,
mais que o dobro da segunda, `ReservationService`, com 184.
**Nota:** com problemas

O que está certo: os serviços não conhecem o Javalin nem o JDBC, o SQL fica nos DAOs e as dependências não formam
ciclo.

Os problemas:

- **Regras na borda HTTP.** Duas regras moram nos handlers:
  - a escolha de qual documento emitir (`handler/ReservationHandler.java:269-275` e `:338-344`);
  - a regra de que a segunda via só existe para reserva paga (`ReservationHandler.java:299-301`).
- **Handlers que pulam o serviço.** Os handlers falam direto com o DAO (`ReservationHandler.java:23` e `:239`,
  `handler/RoomHandler.java:12`).
- **Montagem espalhada.** Não existe um ponto de montagem. Cada classe cria as suas dependências em campos
  `static final` (`ReservationHandler.java:22-23`, `service/ReservationService.java:18-19`,
  `service/ReportService.java:19-20`). O `MessageService.sendEmail` é um método estático chamado de dentro do serviço
  de reservas (`service/MessageService.java:14`).
- **Conexão global.** A conexão é estática e global (`DatabaseConfig.java:13`), não um pool injetado.
- **Handler grande.** O `ReservationHandler` concentra a validação do corpo (`:40-92`), o mapeamento de exceção para
  código HTTP, repetido em cada método, e a montagem do JSON do documento, copiada duas vezes. É o lugar onde o
  próximo pedido vai doer.

## Design de baixo nível

**Números:** 3 tipos de documento, nenhum dentro de uma estrutura. A decisão sobre qual documento criar aparece 3
vezes, como `if (getGuestCpf() != null) … else if (getCompanyCnpj() != null) … else …`, uma em cada lugar onde o
documento sai.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede que a escolha do documento more num lugar só, porque as agências vão trazer um quarto
tipo. O agente pôs o texto de cada documento numa função separada (`util/DocumentFormatter.java:45`, `:70` e
`:94`), mas a decisão sobre qual chamar está copiada nos três lugares:

- no e-mail de confirmação (`service/ReservationService.java:167-173`);
- no check-out (`handler/ReservationHandler.java:269-275`);
- na segunda via (`handler/ReservationHandler.java:338-344`).

A referência chama esse caso de omissão: o que conta é onde se decide qual documento criar. O tipo de hóspede nem
existe como tipo: ele é deduzido de qual campo de texto não é `null`. O título do documento no JSON ficou fixo em
"Comprovante de reserva" para os três tipos (`ReservationHandler.java:251` e `:320`). Para a fatura e o booking
receipt, esse título está errado. A agência vai exigir mexer em três lugares e em mais um `else`.

**Exagero:** não há interfaces, fábricas nem camadas que só repassam.

**O resto do código** tem defeitos espalhados:

- **Duplicação:**
  - o JSON do documento é montado duas vezes, linha por linha (`ReservationHandler.java:250-266` e `:319-335`);
  - o bloco `JsonObject error … addProperty("error") … addProperty("message")` se repete cerca de 20 vezes no handler;
  - o construtor de 20 parâmetros de `Reservation` é chamado duas vezes seguidas com os mesmos valores
    (`ReservationService.java:61-78`);
  - as três funções de documento copiam o mesmo layout (`DocumentFormatter.java:45-118`).
- **Tipos:**
  - situação, plano de tarifa e tipo de quarto são `String` soltas (`"PENDING"` em `ReservationService.java:67`, `:94`
    e `:118`);
  - o código de erro viaja como mensagem de `IllegalArgumentException` e é traduzido em status por comparações de
    texto (`ReservationHandler.java:115-130`).
- **Erros:**
  - `catch (Exception)` transforma uma falha do banco em 400 INVALID_REQUEST (`ReservationHandler.java:131-137` e
    `:186`);
  - no `getDocument`, todo `IllegalArgumentException` vira 404 (`ReservationHandler.java:353-357`).
- **Estado:** a `Connection` é estática e mutável e fica fechada depois do primeiro uso (`DatabaseConfig.java:13`
  e `:112-116`, junto com os try-with-resources dos DAOs). É um defeito de estado que derruba o sistema.
- **Regra lida errado.** A confirmação exige um `amountPaid` no corpo (`ReservationHandler.java:163-172`), mas o texto
  diz que o pago é o total da reserva.
- **Formatação que depende do ambiente:**
  - o dinheiro em reais usa `NumberFormat.getCurrencyInstance(pt_BR)` (`DocumentFormatter.java:24-27`), que no Java 21
    põe um espaço inseparável (U+00A0) depois de "R$". Conferi com jshell: `[82, 36, 160, ...]`. O texto vira
    "R$ 779,80", e não "R$ 779,80";
  - o CSV usa `String.format("%.2f")` sem fixar o locale (`ReportService.java:73` e `:78`), e numa máquina em pt-BR
    sai `599,76`.
- **Código morto:** um método `@Deprecated` que nada usa (`MoneyUtil.java:58-61`).

## Testes

**Números:** 0 testes rodaram (0 falhas, 0 erros), 0 métodos de teste escritos, 0 arquivos de teste. Não há cobertura,
porque o JaCoCo não teve o que medir (`measures/tests.md`). O `mvn verify` termina em BUILD SUCCESS só porque não há
testes (`measures/tests-maven.log`).
**Nota:** ruim

O agente declarou o JUnit 4 no `pom.xml`, mas não escreveu nenhum teste: `src/test` nem existe. Nenhuma regra tem rede:

- o preço com a temporada e o arredondamento;
- o desconto do NON_REFUNDABLE;
- as transições e as devoluções;
- os códigos de erro;
- nada da parte nova do cenário: o ISS de 5%, o formato `BRL 1,234.56`, a regra de "exatamente uma identificação".

Trocar 0.05 por 0.5 em `MoneyUtil.java:10`, ou `>= 15` por `> 15` em `isHighSeason`, não faria nada falhar. Um único
teste pela API teria mostrado os dois defeitos que derrubam o sistema.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. O script acusou uma chave estrangeira sem índice (`reservations.room_number`) e
nenhum CHECK sobre `status`, `rate_plan`, `type` e `channel`. A única restrição de regra é `UNIQUE (rooms.number)`. Não
há pool, nem transação explícita, nem `FOR UPDATE`. O único SQL montado com `+` é `RoomDAO.java:25`, com um `int`
(`measures/database.md`).
**Nota:** ruim

O esquema em si é razoável: tipos certos, chave estrangeira da reserva para o quarto e `NOT NULL` no que é
obrigatório. O SQL usa parâmetros, e a concatenação em `RoomDAO.java:25` recebe um `int`, então não é injeção. Mas o
código que fala com o banco não funciona:

- **A conexão.** Existe uma única conexão estática, aberta por `DriverManager` (`DatabaseConfig.java:25`) e fechada
  pelo primeiro try-with-resources de qualquer DAO (`RoomDAO.java:21`, `ReservationDAO.java:19`, `MessageDAO.java:17`).
  Depois disso, todo acesso lança "Database connection not initialized" (26 vezes no log do comportamento). O serviço
  sobe contra o banco vazio e cria as tabelas, mas a primeira requisição já fecha a conexão. Nenhuma reserva chega a
  ser gravada, o que é "dados que não ficam no banco".
- **Como o esquema é criado.** É `CREATE TABLE IF NOT EXISTS` no código (`DatabaseConfig.java:36-90`), sem migração
  versionada, e não sabe evoluir uma tabela que já existe.
- **Reserva dupla.** Nada no banco impede duas reservas do mesmo quarto na mesma noite: nem uma restrição de exclusão,
  nem uma transação ou trava. O serviço consulta e depois insere sem transação (`ReservationService.java:49-52` e
  `:70`). Mesmo com a conexão consertada, ficaria a corrida, e a conexão única compartilhada entre as threads do Jetty
  também não é segura.
- **Índice.** A chave estrangeira `reservations.room_number` não tem índice, e é por ela que a disponibilidade é
  consultada (`ReservationDAO.java:93-99`).

## Comportamento

**Números:** 3 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 1 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 05 | 0 de 32 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas quase todas as chamadas respondem 500:

- `GET /rooms`, a criação de reserva, as recusas (400, 404, 409, 422) e os dois relatórios esbarram na falta do
  conversor de JSON (a mensagem do Javalin pede o `jackson-databind`) ou na conexão fechada.
- Como nenhuma reserva é criada, tudo o que vem depois falha com 404: confirmação, e-mail, check-in, check-out,
  segunda via, cancelamentos e devoluções.
- Nada da parte do cenário pôde ser conferido: a fatura com ISS de R$ 38,99, o booking receipt e as recusas de
  identificação respondem 500.

As 3 conferências que passaram não provam nada:

- "refund null" passou porque o campo veio vazio;
- "as mensagens continuam lá" passou porque não havia mensagem nenhuma;
- "nenhuma rodada com as duas reservas criadas" passou porque as duas respostas eram 500.

Para o dono da pousada, isto quer dizer que o sistema não aceita nenhuma reserva nem mostra os quartos.

Pela leitura, mesmo com os dois defeitos consertados, ainda falhariam:

- a confirmação sem `amountPaid` no corpo, que daria 400 (`ReservationHandler.java:163-169`);
- o título "Comprovante de reserva" fixo para a fatura e o booking receipt (`:251` e `:320`);
- o espaço inseparável em "R$" no comprovante e na fatura (`DocumentFormatter.java:24-27`).

## Dúvidas

- O roteiro não chegou a exercitar o texto dos três documentos. A conclusão de que eles sairiam quase certos (ISS,
  formato BRL, "1 night"), exceto pelo espaço inseparável em "R$", vem só da leitura de `DocumentFormatter.java` e de
  `MoneyUtil.java`.
- O `formatCPF` só formata 11 dígitos sem pontuação e repassa o resto como veio (`DocumentFormatter.java:120-126`). Se
  o site mandar o CPF já formatado, ele sai como veio. O roteiro não conseguiu conferir isso.
