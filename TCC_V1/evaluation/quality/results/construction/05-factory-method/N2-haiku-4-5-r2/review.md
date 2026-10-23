# Revisão: construção, cenário 05

## Visão geral

O projeto (`projeto/`) separa o código em pacotes por papel: `http` (as rotas Javalin em `AppRoutes` e os DTOs em
`http.request`/`http.response`), `service` (`ReservationService` e `ReportService`), `repository` (JDBC puro, um por
tabela), `database` (o `CREATE TABLE IF NOT EXISTS` e os quartos), `document` (a fábrica e os três formatadores),
`model` e `util`. A montagem fica toda no `Main.java:27-46`. Uma requisição entra na rota, que chama o serviço, que
chama o repositório (com uma conexão nova do `DriverManager` a cada chamada). O defeito mais sério é que o sistema
quase não funciona pela API. As rotas com parâmetro foram escritas com a sintaxe antiga `:id`
(`src/main/java/com/pousada/mare_alta/http/AppRoutes.java:87`), que o Javalin 6 recusa com exceção ("Javalin 4
switched from ':param' to '{param}'", em `measures/behavior-service.log`). Como o `Main.java:45-46` sobe o servidor
antes de registrar as rotas, o processo fica de pé só com `GET /rooms` e `POST /reservations`. Confirmação, check-in,
check-out, segunda via, cancelamento, `/messages` e os dois relatórios respondem 404 "Endpoint not found". Além disso,
o CPF e o CNPJ formatados como no enunciado não cabem nas colunas `VARCHAR(11)`/`VARCHAR(14)`
(`database/Database.java:48-49`), e por isso a criação de reserva com CPF ou CNPJ responde 400. Resultado: 18 de 83
conferências passaram.

## Arquitetura

**Números:** 10 pacotes, 19 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`com.pousada.mare_alta` (o `Main`) e em `http`; JDBC só em `database` e `repository`; `service` sem Javalin e sem JDBC.
As maiores classes são `ReservationService` (254 linhas) e `AppRoutes` (251).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: `http -> service -> repository -> database`. As regras
de preço, de situação e de devolução ficam em `service/ReservationService.java:88-202`, sem `Context` e sem SQL. Todo o
SQL está nos repositórios (`repository/ReservationRepository.java`, com `?` em todas as consultas), e a montagem é uma
só, no `Main.java:27-42`. Há misturas localizadas:

- A borda HTTP chama os repositórios direto, sem passar pela camada de regras. `AppRoutes` recebe `RoomRepository` e
  `MessageRepository` (`http/AppRoutes.java:23-24`) e os usa em `GET /rooms` (`http/AppRoutes.java:43`) e em
  `GET /messages` (`http/AppRoutes.java:190`). É a dependência `http -> repository` do jdeps.
- A borda decide o código de erro procurando trechos na mensagem da exceção (`e.getMessage().contains("Room not
  found")`, `"room capacity"`, `"not available"`, `"not found"`, em `http/AppRoutes.java:68-80` e 109, 126, 143, 160,
  177). A classificação das recusas, que é regra de negócio, depende de texto livre que o serviço escreve. Se alguém
  mudar uma mensagem em `ReservationService`, o código HTTP muda sem aviso.
- A validação da criação está dividida em dois lugares: os campos e o formato ficam na rota
  (`http/AppRoutes.java:229-250`), e os hóspedes, as datas e a identificação ficam no serviço
  (`service/ReservationService.java:43-56`).

Nada disso chega a desfazer as camadas, mas são defeitos que alguém precisa corrigir.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 dentro da estrutura (`CpfDocumentFormatter`, `CnpjDocumentFormatter`,
`PassportDocumentFormatter`, atrás da interface `DocumentFormatter`). A escolha mora num lugar só
(`DocumentFormatterFactory.getFormatter`, `document/DocumentFormatterFactory.java:3-16`) e é chamada uma única vez
(`service/ReservationService.java:226`), no `generateDocument`. Os três lugares que emitem o documento passam por ele:
o e-mail de confirmação (`service/ReservationService.java:236`), o check-out (`:155`) e a segunda via (`:167`).
Nenhum `if` sobre o tipo de hóspede fora da fábrica.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido: a fábrica é concentrada, uma das formas que a referência aceita como acerto. A decisão de qual
documento criar está num método só, e cada formatador monta o próprio texto e o próprio `Document`, com os campos que
não se aplicam em `null` (por exemplo, `document/CnpjDocumentFormatter.java:35-42`). Uma agência de turismo pediria
uma classe nova e um ramo novo na fábrica, sem tocar nos três lugares de uso. O que afasta um pouco do ideal: a
fábrica decide pelos campos nulos da reserva (`DocumentFormatterFactory.java:6-12`), e não por um tipo de hóspede
explícito, de modo que a mesma regra de "qual identificação é" aparece também na validação
(`service/ReservationService.java:51-53`). Isso não muda a classe.

Exagero: não há estrutura sem necessidade fora do pattern. `DocumentData` (`document/DocumentData.java`) quase repete
o `Document` campo a campo, mas é a entrada dos formatadores, então é uma redundância e não uma camada a mais. Conto
isso como duplicação, na nota.

O resto do código tem defeitos localizados, mas numerosos:

- **Erros:** códigos de erro decididos por `contains` na mensagem (`http/AppRoutes.java:68-80`). Há `catch
  (Exception)` em todas as rotas, e na criação esse catch transforma qualquer falha em 400 INVALID_REQUEST com a
  mensagem crua do banco (`http/AppRoutes.java:81-82`). Na medição veio "ERROR: value too long for type character
  varying(11)". Na confirmação, `body.get("amountPaid").toString()` (`http/AppRoutes.java:104`) dá
  `NullPointerException` e 500 se o corpo não trouxer `amountPaid`. O enunciado diz que o pago é o total, e o
  sistema pede esse valor ao cliente sem conferir nada.
- **Tipos:** o dinheiro sai no JSON como texto (`public String subtotal`, em `http/response/ReservationResponse.java:23`
  e `http/response/DocumentResponse.java:22`), formatado com `String.format("%.2f")` no locale padrão
  (`util/NumberFormatter.java:29`). Numa JVM em pt-BR isso vira `"599,76"`. O `formatCurrencyForCsv` tenta remediar
  com `.replace(',', '.')` (`util/NumberFormatter.java:36`). Os campos de identificação, os de quantidade e o
  `roomNumber` no pedido são `int` primitivo (`http/request/CreateReservationRequest.java:7` e 37), então um
  `roomNumber` ausente vira 0 e responde 404 ROOM_NOT_FOUND em vez de 400.
- **Regra errada no código:** o cancelamento só recusa `CHECKED_OUT` e `CANCELLED`
  (`service/ReservationService.java:110-113`), então aceita cancelar quem já fez check-in. A validação da
  identificação conta só CPF, CNPJ e passaporte (`service/ReservationService.java:51-56`) e não confere a razão social
  nem o país.
- **Código morto e duplicação:** as constantes `HIGH_SEASON_START_1`/`HIGH_SEASON_END_1` presas a 2024/2025 nunca são
  usadas (`util/DateUtil.java:8-9`), e `parseYearMonth` também não (`:38`). A variável `found` em
  `service/ReportService.java:31` e `:40` não serve para nada. O bloco de cabeçalho, datas, diárias e desconto é copiado
  quase igual nos três formatadores (por exemplo, `document/CpfDocumentFormatter.java:15-27` e
  `document/CnpjDocumentFormatter.java:15-30`).

## Testes

**Números:** 10 testes rodaram, 0 falhas, 0 erros, 0 pulados; 10 métodos escritos em 3 arquivos. Cobertura de 11,1%
das linhas (96 de 868) e 10,0% dos ramos (19 de 190). `service`, `repository` e `http` estão em 0%, `document` em 51
de 117 linhas (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

Os testes que existem conferem valores, mas só de peças de borda:

- a temporada (`src/test/java/com/pousada/mare_alta/util/DateUtilTest.java:9-40`, com o limite de 14 e 15/12);
- o arredondamento meio-para-o-par (`util/NumberFormatterTest.java:9-19`);
- o texto do comprovante com CPF, conferido por `contains` e não pelo texto inteiro
  (`document/CpfDocumentFormatterTest.java:21-28`).

A parte nova do cenário quase não tem rede. Não há teste da fatura, do ISS, do *booking receipt*, da fábrica nem da
regra "exatamente uma identificação". Se alguém trocar `0.05` por `0.50` em `CnpjDocumentFormatter.java:11`, trocar o
`Locale.US` do `PassportDocumentFormatter`, ou apagar um ramo da `DocumentFormatterFactory`, nenhum teste falha. O
mesmo vale para o preço da reserva, o desconto do NON_REFUNDABLE, as transições de situação, a devolução no
cancelamento, os relatórios e os códigos de erro: `ReservationService` e `ReportService` não são executados por
nenhum teste.

O pior defeito do sistema, as rotas `:id` que o Javalin recusa, passaria por qualquer teste que subisse a aplicação,
e nenhum teste faz isso. Com 11% de cobertura e nada sobre as regras do serviço nem sobre a parte nova, a coluna está
quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` (6 colunas, todas ok) e as datas em `date`. As conferências acusaram:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum CHECK em `status`, `rate_plan`, `type` e `channel`;
- nenhuma restrição UNIQUE ou EXCLUDE;
- nenhum pool, nenhuma transação explícita, nenhum `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no código, sem migração (`measures/database.md`).

Nenhum SQL é montado com `+`.
**Nota:** com problemas

O que está certo: tipos adequados, `NOT NULL` no que é obrigatório, a FK para o quarto, SQL só com parâmetros e
try-with-resources em todas as conexões (por exemplo, `repository/ReservationRepository.java:36-37`). Os dados ficam
no banco: as mensagens continuaram depois do reinício, e o 409 que veio na verificação manual depois de uma reserva
mostra que a reserva foi gravada.

Os defeitos:

- O esquema nasce de `CREATE TABLE IF NOT EXISTS` (`database/Database.java:32-76`), que não sabe evoluir uma tabela
  que já existe. É o próximo pedido, por exemplo o da agência, que vai doer.
- `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`database/Database.java:48-49`) só aceitam o número sem
  pontuação. O CPF `111.222.333-44` e o CNPJ `12.345.678/0001-90`, no formato do enunciado, estouram a coluna, e a
  criação falha com 400. O tamanho é uma escolha de tipo do banco que, junto com o formatador que espera só dígitos
  (`document/CpfDocumentFormatter.java:39-44`), quebra a criação de reserva para dois dos três tipos de hóspede.
- Nada impede duas reservas do mesmo quarto na mesma noite além de uma consulta seguida de um `INSERT`, sem transação
  nem trava (`service/ReservationService.java:67` e `repository/ReservationRepository.java:100-121`). Duas requisições
  simultâneas podem passar as duas.
- Não há pool: cada chamada abre uma conexão nova com o `DriverManager` (`database/Database.java:24`).
- O relatório financeiro filtra `status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')`
  (`repository/ReservationRepository.java:129`) e deixa de fora a NON_REFUNDABLE cancelada, que o texto manda contar.
  Esse defeito é de regra; conto-o no comportamento.

## Comportamento

**Números:** 18 de 83 conferências passaram (`measures/behavior.md`):

- a parte comum: 11 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 05: 5 de 32;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

O serviço sobe, mas registra só duas rotas. O `GET /reservations/:id` (`http/AppRoutes.java:87`) lança
`IllegalArgumentException: Path '/reservations/:id' invalid` depois do `start` (`Main.java:45-46`), e todas as rotas
seguintes ficam de fora. Confirmei à mão: `GET /messages` e `GET /reservations/1` respondem 404 "Endpoint ... not
found". Para o dono da pousada, isso quer dizer:

- Não dá para consultar, confirmar, cancelar, fazer check-in ou check-out de nenhuma reserva.
- Nenhum e-mail sai, nenhum comprovante, nenhuma fatura, nenhum *booking receipt*, nenhuma segunda via.
- Nenhum relatório: as 9 conferências de ocupação e financeiro dão 404, e o mês fora do formato dá 404 em vez de 400.

Na criação, que é o que funciona, aparecem mais estes defeitos:

- A reserva com o CPF no formato do enunciado responde 400 com "value too long for type character varying(11)", e a
  da empresa também, por causa do CNPJ. Só o estrangeiro consegue reservar (a conferência "estrangeiro: 201" passou).
- O quarto ocupado responde 400 em vez de 409 nas conferências do roteiro, porque a primeira reserva (com CPF) nem
  chegou a ser criada. À mão, com um CPF sem pontuação, veio o 409 ROOM_UNAVAILABLE correto.
- O dinheiro sai no JSON como texto (`"amountPaid":"0.00"`, visto à mão), não como número.
- Os campos que não se aplicam somem do JSON por causa do `@JsonInclude(NON_NULL)`
  (`http/response/ReservationResponse.java:7`), em vez de virem `null`.
- O passaporte sem o país foi aceito com 201 quando devia dar 400, porque a validação não confere o país
  (`service/ReservationService.java:51-56`).
- O "CNPJ sem a razão social: 400" passou só porque o CNPJ formatado estoura a coluna, e não porque a regra exista.

Nas reservas simultâneas, as 10 rodadas deram 400 e 400. Nenhuma duplicou, mas porque nenhuma foi criada, não porque
haja proteção. O que deu certo: os cinco quartos e a diária, as recusas de capacidade (422), quarto inexistente (404),
datas, hóspedes, JSON inválido e campos faltando (400). Sem ser medido pelo roteiro por causa dos 404, o código também
deixaria cancelar depois do check-in (`service/ReservationService.java:110-113`) e tiraria do financeiro a
NON_REFUNDABLE cancelada (`repository/ReservationRepository.java:129`). A parte comum quebra e a maior parte do
cenário falha.

## Dúvidas

- O texto não diz se o CPF e o CNPJ chegam com ou sem pontuação. O roteiro manda formatados, como o enunciado os
  mostra no comprovante. Mesmo que se aceitasse a escolha de guardar só dígitos, o sistema não normaliza a entrada:
  recusa com um erro de banco cru, e isso não é uma escolha razoável. A nota do comportamento seria `ruim` de qualquer
  modo, por causa das rotas `:id`.
- No design, a fábrica escolhe pelos campos nulos, e não por um tipo de hóspede. Considerei-a uma fábrica concentrada
  (acerto), porque a decisão está num lugar só e os três usos passam por ela. Como as rotas de confirmação, check-out
  e segunda via não respondem, não foi possível ver pela API que o mesmo documento sai nos três lugares. A conclusão
  vem só da leitura de `service/ReservationService.java:155`, 167, 226 e 236.
