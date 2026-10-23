# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`pousada`, 24 classes, 1073 linhas). Uma requisição entra pelas rotas em
`Main.routes` (`src/main/java/pousada/Main.java:22-45`), que só leem a requisição, chamam o `ReservationService` ou o
`Reports` e montam o JSON (`Main.java:104-142`). O serviço abre a transação com `Database.transaction`
(`Database.java:23-37`), as regras ficam nos records `Reservation`, `Pricing` e `RatePlan`, e todo o SQL está no
`Repository`. Os dados ficam no PostgreSQL, em três tabelas que `Database.initialize` cria com
`CREATE TABLE IF NOT EXISTS` (`Database.java:46-77`). Para a parte do cenário, cada `IdentificationType` traz os
campos que exige e o seu `Document` (`IdentificationTypes.java:12-15`), e os três lugares que emitem o documento pedem
o documento ao tipo de identificação. O comportamento passou inteiro (83 de 83). Não há nada de grave. Os defeitos
são de acabamento: tudo num pacote só, o serviço recebendo o `JsonNode` do Jackson, a identificação guardada como um
mapa de textos, um esquema sem migrações, sem CHECK e sem índice na chave estrangeira, e testes que só cobrem o preço
e os documentos (22,5% das linhas).

## Arquitetura

**Números:** 1 pacote (`pousada`, 24 classes, 1073 linhas), 0 dependências entre pacotes, 0 ciclos. O pacote usa
Javalin, JDBC e Jackson. Maiores classes: `Repository` (197 linhas), `ReservationService` (164), `Main` (143).
**Nota:** com problemas

As camadas existem como classes, mas não como pacotes:
- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço (`Main.java:33-43`), e os erros viram JSON num
  lugar só (`Main.java:27-31`).
- **O SQL está isolado.** Fica no `Repository` (`Repository.java:26-161`), e a DDL fica no `Database`
  (`Database.java:46-83`).
- **A montagem está num lugar só.** `Main.java:23-25` cria o repositório, o serviço e os relatórios e passa as
  dependências pelo construtor.
- **As regras não conhecem o Javalin.** As transições de estado ficam em `Reservation.java:10-28`, o preço em
  `Pricing.java:9-28` e a devolução em `RatePlan.java:21-23`.

O que mistura as camadas, em pontos localizados:
- **O serviço lê o JSON da requisição.** `ReservationService.create` recebe o `JsonNode` do Jackson e faz a
  validação de formato (`ReservationService.java:29-37`, `125-163`). `IdentificationTypes.fromRequest` também lê o
  `JsonNode` (`IdentificationTypes.java:30-51`). O tipo de domínio fica preso ao formato HTTP, e o serviço não pode
  ser chamado sem montar um JSON.
- **O serviço conhece `Connection` e `SQLException`** (`ReservationService.java:6-7`, `89-91`, `104-105`). Não
  escreve SQL, mas a transação vaza como parâmetro.
- **O domínio carrega códigos HTTP.** `Reservation.requireStatus` lança uma `ApiException`, que guarda o status 409
  (`Reservation.java:40`, `ApiException.java:17-19`).
- **Tudo está num pacote só.** Nenhum pacote separa a borda, as regras e os dados, e nada impede que a próxima
  mudança faça uma rota chamar o `Repository` direto.

Não há classe desproporcional.

## Design de baixo nível

**Números:** 3 de 3 tipos de identificação estão no registro `IdentificationTypes.ALL`
(`IdentificationTypes.java:12-15`). Os 3 lugares que emitem o documento (o e-mail, o check-out e a segunda via)
pedem o documento ao tipo e não decidem nada. Fora da estrutura, sobra 0 `if`/`switch` sobre o tipo de hóspede.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento emitir mora num lugar só:
- Cada `IdentificationType` é um record com o código, os campos exigidos e o `Document` daquele tipo
  (`IdentificationType.java:6`). O registro liga cada tipo ao seu documento: CPF a `CpfReceipt`, CNPJ a
  `LodgingInvoice` e passaporte a `BookingReceipt` (`IdentificationTypes.java:13-15`).
- O tipo é decidido uma vez, na criação, pelos campos presentes (`IdentificationTypes.java:30-51`), e é guardado com a
  reserva (`Repository.java:66`, `166-167`).
- O e-mail de confirmação usa `r.identification().type().document().render(r)` (`ReservationService.java:106`). O
  check-out e a segunda via passam os dois por `documentView` (`Main.java:39-40`), que faz a mesma chamada
  (`Main.java:126`).
- Nenhum dos três lugares pergunta qual é o tipo. Uma agência entra com uma linha no registro e uma subclasse de
  `Document`. Os campos novos aparecem sozinhos no JSON da reserva e do documento, por `allFields()`
  (`IdentificationTypes.java:20-24`, `Main.java:98-102`).

É a forma de "método polimórfico no tipo de hóspede", com um registro concentrado. O que afasta um pouco do clássico:
- O tipo não fabrica um documento novo por reserva. Ele guarda uma instância sem estado que renderiza qualquer
  reserva.
- A cadeia `r.identification().type().document()` aparece repetida em dois lugares (`Main.java:126`,
  `ReservationService.java:106`), em vez de um método `issueDocument` na reserva.

Nenhum dos dois põe a escolha fora do lugar único.

**Exagero:** não há. O `Document` com o Template Method (`Document.java:12-25`) e a camada intermediária
`PortugueseDocument` (`Document.java`, `PortugueseDocument.java:6-40`) servem aos três documentos, dois deles em
português. A interface `AfterChange` (`ReservationService.java:89-91`) e a `Database.Work` (`Database.java:9-11`) só
existem para deixar a lambda lançar `SQLException`.

**O resto do código.** O dinheiro está certo: `BigDecimal` com `HALF_EVEN` (`Money.java:11-17`), e as datas são
`LocalDate`. Os defeitos, localizados:
- **A identificação é um `Map<String, String>`** (`Identification.java:5`). Os nomes dos campos são repetidos como
  texto solto no registro (`IdentificationTypes.java:13-15`) e nos documentos (`CpfReceipt.java:13`,
  `LodgingInvoice.java:14-15`, `BookingReceipt.java:19-21`). Um nome digitado errado num documento imprime "null" sem
  nenhum erro.
- **O tipo do quarto é `String`** (`Room.java:5`), e não um `enum`.
- **O serviço usa `null` como "nenhum passo depois"** (`ReservationService.java:71-79`, `97`).
- **O `Repository` monta o `Room` duas vezes.** O trecho é copiado em `Repository.java:164-165` e
  `Repository.java:177-179`, e o parâmetro `numberColumn` sempre recebe `"number"`.
- **O `Database.transaction` pode esconder o erro original** (`Database.java:30-32`). Ele captura `Throwable` para dar
  rollback, e se o rollback falhar, a exceção do rollback substitui a original.

## Testes

**Números:** 6 testes rodaram, 0 falharam (0 erros, 0 pulados). Há 6 métodos de teste escritos. Cobertura: 102 de 454
linhas (22,5%) e 28 de 100 ramos (28,0%). São só testes de unidade, num arquivo: nenhum pela API, nenhum contra o
banco.
**Nota:** com problemas

O que é testado, com valores exatos:
- **O preço.** A alta temporada dá 599,76 (`DocumentsTest.java:24-30`). O NON_REFUNDABLE dá 779,80, com desconto de
  116,97 e total de 662,83 (`DocumentsTest.java:32-39`).
- **O texto inteiro dos três documentos.** O comprovante (`DocumentsTest.java:41-52`), a fatura com ISS de R$ 38,99
  (`DocumentsTest.java:54-67`) e o booking receipt com desconto e "1 night" (`DocumentsTest.java:69-81`). Este último
  confere o meio para o par, com 58,48.
- **A regra de "um jeito só" de identificação** (`DocumentsTest.java:83-92`).

A pergunta da mutação:
- Trocar o 0.05 do ISS (`LodgingInvoice.java:21`), apagar a linha de desconto (`Document.java:19-21`) ou trocar
  `" night"` por `" nights"` derruba um teste.
- Trocar `>= 15` por `> 15` na alta temporada de dezembro (`Pricing.java:27`) não derruba nenhum.
- Fazer o NON_REFUNDABLE devolver o pago (`RatePlan.java:22`) não derruba nenhum.
- Mudar `Status.isPaid` (`Status.java:6-8`) não derruba nenhum.
- Mudar a consulta de sobreposição (`Repository.java:50`) não derruba nenhum.

Também ficam sem teste:
- as transições de estado (`Reservation.java:10-28`);
- o e-mail de confirmação (`ReservationService.java:104-113`);
- os dois relatórios (`Reports.java`);
- os códigos de erro da API;
- a persistência.

A parte nova do cenário está bem conferida; a parte comum quase não está.

## Banco

**Números:**
- **O esquema:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
  `numeric(12,2)` e as datas em `date`.
- **O que as conferências acusaram:**
  - uma chave estrangeira sem índice, `reservations(room_number)` para `rooms`;
  - 0 CHECK, nem nas colunas `status`, `rate_plan`, `id_type`, `channel` e `type`;
  - 0 UNIQUE ou EXCLUDE;
  - 0 arquivos de migração;
  - 0 pool de conexões, com 1 chamada a `DriverManager.getConnection`;
  - nenhum SQL montado com dado de fora.
**Nota:** com problemas

O que está certo:
- **Os tipos e as obrigações.** Os tipos estão certos e há `NOT NULL` no que é obrigatório (`Database.java:46-77`). O
  `refund` aceita null, como o texto pede.
- **O SQL usa parâmetros em toda parte** (`Repository.java:26-161`). As partes concatenadas são constantes: o
  `RESERVATION_SELECT`, o `" for update of r"` e as cláusulas fixas de `paidOccupying` e `startingBetween`
  (`Repository.java:88`, `107-113`).
- **As conexões são fechadas** com try-with-resources (`Database.java:24`).
- **A reserva dupla é protegida pelo banco, e não só no processo.** A criação trava a linha do quarto com
  `SELECT ... FOR UPDATE` (`Repository.java:28`, `ReservationService.java:45`) antes de conferir a sobreposição e
  inserir, na mesma transação. As mudanças de situação travam a reserva (`Repository.java:87-89`). Isso explica as 10
  rodadas simultâneas sem duplicação.
- **O e-mail é gravado com a confirmação.** Ele é gravado na mesma transação (`ReservationService.java:93-101`).

Os defeitos:
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` no código (`Database.java:46-77`), sem migrações. A
  primeira coluna nova numa tabela já existente não será criada.
- **A chave estrangeira não tem índice** (`Database.java:55`).
- **Nenhuma regra está no banco.** Não há CHECK sobre as situações, os planos e os tipos, e não há restrição de
  sobreposição (EXCLUDE). Só a trava do código protege a regra.
- **A identificação é guardada como JSON num `text`.** As colunas são `id_type` e `id_values` (`Database.java:58-59`,
  `Repository.java:66-67`). Não dá para consultar nem restringir CPF ou CNPJ pelo banco.
- **Não há pool.** Cada transação abre uma conexão nova (`Database.java:99-101`).

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 05 | 32 de 32 |
| reservas simultâneas | 2 de 2 |
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- **As contas.** Os preços com a temporada e o meio para o par, os planos e as devoluções estão certos.
- **A reserva.** As situações e as recusas respondem com os códigos combinados. O e-mail de confirmação sai em
  português, e os dois relatórios em CSV saem certos.
- **Os dados.** As reservas e as mensagens sobrevivem ao reinício.
- **Os documentos do cenário.**
  - A empresa recebe a fatura com a razão social, o CNPJ e o ISS retido de R$ 38,99.
  - O estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, as datas em `2027-03-10` e "1 night".
  - A pessoa com CPF continua recebendo o comprovante.
  - O documento é o mesmo no e-mail, no check-out e na segunda via.
  - A reserva sem nenhum jeito de identificação, com dois jeitos ou com um jeito incompleto é recusada com 400.
- **A concorrência.** Duas reservas ao mesmo tempo para o mesmo quarto nunca são criadas as duas.

## Dúvidas

- **A nota da arquitetura.** A skill põe "tudo num pacote" entre os sinais de `ruim`. Dei `com problemas` porque as
  camadas existem como classes: a borda é fina, o SQL só está no `Repository`, e as regras não usam o Javalin. A
  mistura fica no `JsonNode` dentro do serviço e no código HTTP dentro do domínio.
- **A forma do Factory Method.** O agente usou um registro concentrado (`IdentificationTypes.ALL`) em que cada tipo
  de hóspede carrega o seu documento: um método polimórfico no tipo, por meio de um campo. Não é o criador clássico
  com o método de fábrica sobrescrito, e o documento é uma instância compartilhada, não criada por reserva. A
  referência aceita essa forma como `acerto`.
- **A nota do design.** Os defeitos do resto do código são pequenos, e a nota fica entre `bom` e `com problemas`. Dei
  `com problemas` sobretudo pela identificação em `Map<String, String>`, com os nomes dos campos espalhados em cinco
  arquivos.
