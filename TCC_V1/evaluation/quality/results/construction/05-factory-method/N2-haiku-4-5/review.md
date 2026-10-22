# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto Maven com Javalin 6.7.0, PostgreSQL via HikariCP e Gson, dividido em 8 pacotes. Uma requisição entra pelas rotas do `Main` (`Main.java:55-68`), passa pelos handlers de `api.handlers`, que leem o corpo com Gson e chamam `ReservationService`. O serviço usa `PricingService` para o preço e `DocumentService` para o documento, e grava pelos repositórios de `repository`, que usam JDBC puro sobre um `DataSource` estático (`DatabaseConnection.java:10`). O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `SchemaInitializer.java`. O defeito mais sério derruba o sistema inteiro: os handlers montam a resposta como `JsonObject`/`JsonArray` do Gson e a entregam a `ctx.json(...)` (`ReservationHandler.java:87`, `RoomHandler.java:32`, `MessageHandler.java:33`). Mas o Javalin serializa com Jackson, e nenhum mapeador Gson foi configurado (`Main.java:53`). O Jackson falha (`JsonMappingException: JsonObject ["asDouble"]`, reproduzido à mão), e toda resposta JSON vira 500, inclusive as respostas de erro. As escritas acontecem no banco antes do 500, então o site nunca fica sabendo o que foi criado. Só os dois relatórios CSV respondem, e o roteiro passou em 6 de 83 conferências. Há outros defeitos: dinheiro em `double`, o comprovante com o valor escrito errado (`R$ 779.80`), o preço da temporada decidido só pela data de entrada, nenhuma proteção contra reserva dupla (reproduzi 10 duplicações em 10 rodadas) e só 4 testes.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo. A maior classe é `ReservationService`, com 212 linhas, seguida de `ReservationHandler`, com 207 (de `measures/dependencies.md`).
**Nota:** com problemas

As camadas existem, e as dependências vão na direção certa: handlers → service → repository → db, e o `domain` só depende de `util`. A montagem está num lugar só (`Main.java:34-51`), e o domínio não conhece Javalin nem JDBC. Três problemas localizados:

- **Uma regra de negócio na borda HTTP.** A validação de "exatamente um jeito de identificação" (CPF, ou CNPJ com razão social, ou passaporte com país) fica toda no handler (`ReservationHandler.java:50-80`). O serviço recebe os cinco campos soltos e não confere nada (`ReservationService.java:40-52`).
- **Handlers que pulam o serviço.** `RoomHandler` e `MessageHandler` falam direto com os repositórios (`RoomHandler.java:13-20`, `MessageHandler.java:13-20`). O jdeps mostra `api.handlers -> repository` e JDBC no pacote dos handlers, porque `SQLException` sobe até as assinaturas deles (`ReservationHandler.java:25`).
- **Um estado global.** O pool é um `static` mutável (`DatabaseConnection.java:10-33`), e cada repositório o busca por conta própria (`ReservationRepository.java:28`, `RoomRepository.java:17`) em vez de recebê-lo na montagem. Isso amarra os repositórios a um banco global e dificulta testá-los.

## Design de baixo nível

**Números:** um único ponto de criação do documento (`DocumentService.generateDocument`), chamado pelos 3 lugares (e-mail de confirmação, check-out, segunda via). A estrutura cobre os 3 tipos de documento, e nenhum fica fora dela.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** A escolha de qual documento criar mora num só lugar, `DocumentService.java:45-55`, com um `if`/`else` sobre `IdentificationType`. Os três lugares pedem o documento a esse método e nenhum o monta à mão: `ReservationService.java:196` (e-mail), `:147` (check-out) e `:192` (segunda via). Pela referência, isso é a "fábrica concentrada", um acerto. O que afasta da forma clássica:

- o documento é uma classe de dados só (`Document.java`), com `title` e `text` preenchidos pela fábrica, e não um tipo por documento;
- o tipo de hóspede é deduzido de qual campo veio preenchido (`Reservation.java:190-195`), não modelado;
- uma agência de turismo exigiria mexer em quatro lugares: o enum, os campos, mais um `else if` e mais um gerador.

**O exagero.** Não há estrutura sem necessidade: nem interfaces com uma implementação só, nem camadas de repasse. `PricingResult` (`PricingService.java:10-24`) é só um portador de valores.

**O resto do código** tem defeitos espalhados:

- **Dinheiro em `double` em toda parte.** Aparece em `Reservation.java:20-25`, `Document.java:19-21`, `RoomType.java:10`, `PricingService.java:32-43` e `ReservationRepository.java:185-191`. O arredondamento "meio para o par" é feito sobre o valor binário do `double` (`MoneyUtils.java:10`, `new BigDecimal(value)`), o que erra justamente nos casos de meio centavo que a regra pede. Os DTOs ainda arredondam de novo com `Math.round(x * 100) / 100.0` (`ReservationDTO.java:24-30`, `DocumentDTO.java:23-29`).
- **Formatação de dinheiro errada.**
  - `formatBRL` usa `String.format("R$ %,.2f")` e depois troca toda vírgula por ponto (`MoneyUtils.java:19`). O resultado é `R$ 779.80` e `R$ 2.783.52` (reproduzido), quando o texto pede `R$ 779,80` e `R$ 2.783,52`.
  - `formatBRLEnglish` não põe separador de milhar (`MoneyUtils.java:24`): `BRL 2783.52` no lugar de `BRL 2,783.52`.
  - Os formatos dependem do locale da máquina. Com `pt_BR`, o CSV sairia `599,76` (`MoneyUtils.java:15`).
- **Regra de preço errada.** A alta temporada é decidida pela data de entrada e aplicada a todas as noites (`PricingService.java:29`, `:34-37`, `:48-53`). Uma estadia de 30/06 a 02/07, ou de 14/12 a 16/12, sai com o preço errado.
- **A mesma regra em mais de um lugar.** A exigência da razão social e do país não é validada: `companyName` só é lido "se vier" (`ReservationHandler.java:63-65`, `:70-72`). Um CNPJ sem razão social é aceito e gravado com `company_name` nulo (reproduzido).
- **Erros por texto.**
  - Os códigos de erro viajam como mensagem de `IllegalArgumentException` (`ReservationService.java:56`, `:61`, `:71`, `:102`) e são decifrados por um `switch` de strings no handler (`ReservationHandler.java:179-205`).
  - O `catch (Exception e)` transforma qualquer falha em 400 e esconde a causa (`ReservationHandler.java:90`, `ReportHandler.java:32`, `:51`).
  - O `IllegalArgumentException` de "quarto não encontrado" do `DocumentService` (`DocumentService.java:23`) cairia como 400.
- **Duplicação.**
  - Os três geradores de texto repetem as mesmas linhas, como quarto, datas, diárias, desconto e total (`DocumentService.java:60-106`).
  - `addNullableProperty` foi copiado nos dois DTOs (`ReservationDTO.java:37`, `DocumentDTO.java:34`).
  - O bloco "parse do id, `NumberFormatException`, `handleError`" se repete seis vezes em `ReservationHandler.java:96-177`.
- **Estado e assinaturas.** As entidades são sacos de setters mutáveis (`Reservation.java`, `Document.java`), e `PricingResult` tem campos públicos mutáveis (`PricingService.java:11-15`). `createReservation` recebe 12 parâmetros soltos (`ReservationService.java:40-52`).
- **A causa do 500 é uma escolha de design.** O agente trouxe o Gson (`pom.xml:46-50`), montou a resposta com a árvore do Gson e a entregou ao mapeador Jackson do Javalin.

## Testes

**Números:** 4 testes rodaram e 0 falharam. Há 4 métodos de teste escritos, todos em `PricingServiceTest`. A cobertura é de 45 de 914 linhas (4,9%) e de 9 de 212 ramos (4,2%). Os pacotes `api`, `repository` e `db` têm 0% (de `measures/tests.md`).
**Nota:** ruim

Os quatro testes conferem o preço com valores:

- a baixa temporada (`PricingServiceTest.java:14-25`);
- a alta temporada com 299,88 e 599,76 (`:28-39`);
- o desconto do NON_REFUNDABLE com 74,97 e 424,83 (`:42-53`);
- o DELUXE (`:56-67`).

Fica tudo o mais sem teste: as situações da reserva, o cancelamento e a devolução, as recusas e os códigos de erro, os relatórios, o banco e a API. A parte nova do cenário também não tem teste nenhum: a escolha do documento por tipo de hóspede, a fatura com o ISS de 5%, o booking receipt em inglês e a validação de "exatamente um jeito de identificação".

A pergunta da mutação: trocar `0.05` por `0.5` (`DocumentService.java:109`) ou apagar a linha do ISS (`:89`) não derruba nenhum teste. Também não derruba nada inverter o `== 1 ? " night"` (`:99`) ou devolver sempre o comprovante. Os testes também não pegam a temporada decidida só pela entrada, porque nenhum cruza a virada da temporada. Nenhum teste sobe a API, e por isso o 500 em todas as respostas JSON passou despercebido com o `mvn verify` verde.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram: uma chave estrangeira sem índice (`reservations.room_number -> rooms`), status, plano, tipo e canal sem CHECK, nenhuma restrição que proteja uma regra, 0 transações, 0 `FOR UPDATE` e um SQL montado com `+` (de `measures/database.md`).
**Nota:** ruim

- **Nada impede a reserva dupla.** A verificação de disponibilidade é um `SELECT` seguido de um `INSERT` em conexões separadas, sem transação, sem trava e sem restrição `EXCLUDE` (`ReservationService.java:69-72`, `ReservationRepository.java:95-118` e `:19-60`). Rodei à mão dez rodadas de dois pedidos simultâneos para o 101 nas mesmas noites, e as dez gravaram as duas reservas no banco. O roteiro do comportamento deu "nenhuma com as duas criadas" só porque as duas respostas foram 500. É dado errado: o mesmo quarto vendido duas vezes.
- **Um esquema que não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`SchemaInitializer.java:11-52`), sem migrações, então o próximo pedido que acrescentar uma coluna não terá como alterar uma tabela que já existe.
- **Faltam índice e CHECKs.** A chave estrangeira `room_number` não tem índice (`SchemaInitializer.java:40`). Faltam CHECK para `status`, `rate_plan`, `guests >= 1` e `check_out > check_in`, e o banco não garante "exatamente uma identificação".
- **Quartos pela metade no banco.** A tabela `rooms` só guarda número e tipo (`SchemaInitializer.java:12-15`); a capacidade e a diária vivem num `enum` Java (`RoomType.java:4-6`).
- **SQL montado com `+`.** O caso é `RoomRepository.java:33` (`"... WHERE number = " + number`). O valor que entra é um `int` já convertido (`ReservationHandler.java:42`), então não há injeção, mas foge do padrão `?` do resto.
- **Dinheiro passa por `double`.** As colunas estão certas, mas a leitura e a escrita passam por `double` (`rs.getDouble`, `BigDecimal.valueOf(double)`, em `ReservationRepository.java:43-48` e `:185-191`).
- **O que está certo:** SQL parametrizado no resto, conexões devolvidas com try-with-resources e pool Hikari. Os dados sobrevivem ao reinício: as mensagens continuam lá, e a reserva também, embora a API não consiga mostrá-la.

## Comportamento

**Números:** 6 de 83 conferências passaram (de `measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 4 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário 05 | 0 de 32 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas toda rota que responde JSON devolve 500: `GET /rooms`, a criação, a consulta, a confirmação, o check-in, o check-out, a segunda via, `GET /messages` e até as respostas de erro, como 400, 404, 409 e 422. A causa, já descrita acima, é o Gson entregue ao Jackson do Javalin (`ReservationHandler.java:87`, `Main.java:53`). Só os relatórios CSV respondem, porque usam `ctx.result` (`ReportHandler.java:30-31`). Os cabeçalhos deles estão certos, mas saem vazios, porque o roteiro não conseguiu confirmar nenhuma reserva pela API. Pior: as operações acontecem no banco mesmo devolvendo 500. Reproduzi à mão uma criação que gravou a reserva `PENDING` e uma confirmação que gravou `CONFIRMED` e mandou o e-mail. O site recebe "erro" e não sabe o id do que criou.

Para o dono da pousada, isso quer dizer que o site não consegue reservar nada nem listar quartos, e cada tentativa pode deixar uma reserva fantasma ocupando o quarto. Mesmo que a serialização fosse consertada, a leitura do código e as chamadas à mão mostram estes erros:

- o comprovante e a fatura com o dinheiro escrito errado: `R$ 779.80`, `ISS retido (5%): R$ 38.99`, `R$ 2.783.52` (`MoneyUtils.java:19`);
- o booking receipt sem o milhar: `BRL 2783.52` (`MoneyUtils.java:24`);
- CNPJ sem razão social, e passaporte sem país, aceitos (`ReservationHandler.java:63-72`);
- estadias que cruzam a virada da temporada com o preço errado (`PricingService.java:29`);
- quarto vendido duas vezes em pedidos simultâneos (ver Banco).

## Dúvidas

- A forma do pattern é a "fábrica concentrada": um método que escolhe por `if`/`else` sobre o tipo de identificação, deduzido dos campos preenchidos, e devolve uma classe `Document` única com título e texto diferentes. Não é a forma clássica com um criador por tipo. Contei como acerto, porque a escolha fica num lugar só e os três lugares a usam. Um revisor mais estrito poderia ver o tipo deduzido de campos nulos como frágil.
- Não pude conferir pela API os valores do cenário (o ISS de R$ 38,99, o "1 night", a linha de desconto), porque nenhuma resposta JSON sai. O que digo sobre eles vem da leitura de `DocumentService.java` e do texto do e-mail gravado no banco numa chamada à mão.
- O JSON do documento leva as datas como `10/03/2027` (`Document.java:160-166`) e `discount` nulo quando não há desconto (`DocumentService.java:40-42`). O texto não fecha o formato desses campos no documento. Não pesei.
