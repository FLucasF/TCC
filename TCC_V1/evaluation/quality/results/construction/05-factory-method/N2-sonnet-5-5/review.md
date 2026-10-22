# Revisão: construção, cenário 05

## Visão geral

O projeto está todo num pacote só (`br.com.marealta`), com 34 classes e 1220 linhas. As camadas existem, mas como classes, não como pacotes. Uma requisição entra pelo `Api` (rotas Javalin, `Api.java:38-50`), que lê e valida o corpo (`Api.java:68-97`) e chama o `ReservationService` ou o `ReportService`. Essas duas classes não usam Javalin nem SQL. As regras da reserva ficam na própria `Reservation` (`Reservation.java:47-73`) e o preço no `PriceCalculator`. O acesso aos dados passa pelas interfaces `ReservationRepository`/`MessageRepository`, implementadas com JDBC puro (`JdbcReservationRepository`, `JdbcMessageRepository`). O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `Database.java:28-55`, e tudo é montado em `Main.java:7-21`. A variação do cenário foi resolvida com um método de fábrica no tipo de identificação: `IdentityType.format()`, sobrescrito por `CpfIdentity`, `CnpjIdentity` e `PassportIdentity`. Os três lugares que mostram o documento pedem o documento por esse método. O comportamento passou em todas as 83 conferências. Nada é grave. Os pontos mais sérios estão no banco: o esquema fica no código e não há migrações, não existe chave estrangeira nem restrição, a única proteção contra reserva duplicada é um `synchronized`, e a disponibilidade é calculada lendo a tabela inteira. Há também os dados de identificação guardados como um `Map<String,String>` sem tipo.

## Arquitetura

**Números:** 1 pacote (`br.com.marealta`, 34 classes, 1220 linhas), 0 dependências entre pacotes, nenhum ciclo. O único pacote usa Javalin, JDBC e Jackson. Maior classe: `Api.java`, com 164 linhas; as outras têm até 122 (`measures/dependencies.md`).
**Nota:** com problemas

A separação por classe é boa:
- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço e devolve uma visão (`Api.java:38-50`), e o `Api` cuida só de ler o corpo, converter tipos e montar o JSON e o CSV (`Api.java:82-163`).
- **Os serviços não conhecem Javalin nem JDBC** (`ReservationService.java:1-82`, `ReportService.java:1-49`). As regras de situação ficam na entidade (`Reservation.java:47-73`).
- **O SQL está isolado** nos dois repositórios JDBC e em `Database.java`.
- **A montagem é feita num lugar só** (`Main.java:7-21`). `Main.build` recebe os repositórios, e é isso que deixa os testes trocá-los por versões em memória (`ApiTest.java:25-26`).
- **As classes são pequenas e equilibradas.** A maior é o `Api`, com 164 linhas.

Os defeitos:
- **Nada impõe as camadas.** Como tudo está num pacote só, qualquer classe pode usar qualquer outra, e o jdeps não consegue ver as camadas. Hoje a disciplina depende só de convenção.
- **O domínio conhece o HTTP.** A exceção carrega o status HTTP (`AppException.java:7-23`), e quem a lança é a própria entidade (`Reservation.java:71`, um 409) e o registro de identificações (`IdentityTypes.java:37`, um 400).
- **O formato CSV está no serviço.** O `ReportService` monta o CSV (`ReportService.java:24-30`, `39-47`), uma parte de apresentação que poderia ficar na borda. Isso pesa pouco.

## Design de baixo nível

**Números:** 3 tipos de hóspede e 3 documentos. Os 3 casos estão na estrutura e nenhum fica fora dela. Os 3 lugares que mostram o documento (o e-mail de confirmação, o check-out e a segunda via) passam pelo mesmo método de fábrica, e não há nenhum `if` ou `switch` sobre o tipo de hóspede no código.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern: a forma clássica do Factory Method.**
- **O criador e as sobrescritas.** `IdentityType` declara `DocumentFormat format()` (`IdentityType.java:12`), e cada tipo de identificação sobrescreve o método e devolve o seu documento:
  - `CpfIdentity.java:12` devolve o `ReceiptFormat`;
  - `CnpjIdentity.java:13` devolve o `InvoiceFormat`;
  - `PassportIdentity.java:13` devolve o `BookingReceiptFormat`.
- **Quem usa nunca pergunta o tipo.** A reserva guarda a identificação (`GuestIdentity.java:9` repassa `type.format()`). Os três lugares pedem o documento à identificação:
  - o e-mail, em `ConfirmationEmail.java:9`;
  - o check-out e a segunda via, em `Views.java:45` e `Views.java:59` (chamados em `Api.java:44-47`).
- **A escolha também na entrada e na leitura do banco.** Ela fica num registro: `IdentityTypes.standard()` (`IdentityTypes.java:17`) lista os tipos, `parse` (`IdentityTypes.java:31-48`) descobre qual veio no corpo e exige exatamente um, e `byKind` (`IdentityTypes.java:25-28`) reconstrói o tipo a partir do banco.
- **A agência de turismo é uma classe nova.** Ela entraria como um `IdentityType` novo mais um `DocumentFormat` novo, acrescentados a essa lista. Os cinco campos nulos da resposta também saem da lista de tipos (`Views.java:73-77`), sem nomes fixos.

**O exagero.** Não há estrutura sem necessidade fora da parte do pattern:
- **As interfaces dos repositórios** (`ReservationRepository.java`, `MessageRepository.java`) têm uma segunda implementação, em memória, nos testes.
- **O `PriceCalculator` e o `RoomCatalog`** são classes concretas simples.
- **O `DocumentFormat`** é um Template Method com 12 métodos abstratos e um gancho (`DocumentFormat.java:13-53`), mais uma classe intermediária, `PortugueseDocumentFormat`. É bastante maquinaria para três textos, mas tudo isso fica dentro da variação que o cenário pede, e o render único evita copiar o corpo do documento em cada formato.

**O resto do código** é limpo: `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:13-24`, `PriceCalculator.java:9-24`), `LocalDate`/`YearMonth` para as datas, `enum` para situação, plano e tipo de quarto (`RatePlan.java`, `ReservationStatus.java`, `RoomType.java`), e métodos curtos. Os defeitos, localizados:
- **A identificação não tem tipo.** É um `Map<String,String>` genérico (`GuestIdentity.java:6`), e cada documento lê o campo pela chave em texto (`ReceiptFormat.java:9`, `InvoiceFormat.java:12`, `BookingReceiptFormat.java:13-14`). Se uma chave errada for usada, o resultado é um `null` silencioso no texto, e o compilador não acusa. Records por tipo (CPF, empresa, passaporte) dariam esse controle.
- **As exceções do domínio levam o status HTTP** (`AppException.java:7`, lançada em `Reservation.java:71`).
- **Um número inteiro grande no JSON passa por `asInt()`** (`Api.java:90`) e sofre overflow sem aviso, em vez de virar um 400.
- **A mensagem de RESERVATION_NOT_FOUND está duplicada** (`Api.java:142` e `ReservationService.java:45-46`).
- **A `Reservation` é mutável**, com um `assignId` público (`Reservation.java:89`). É aceitável, mas deixa o id ser trocado de fora.

## Testes

**Números:** 12 testes rodaram, nenhum falhou ou foi pulado (12 métodos escritos: 9 em `DomainTest` e 3 em `ApiTest`). Cobertura: linhas 385 de 540 (71,3%), ramos 79 de 114 (69,3%). 1 arquivo testa pela API, nenhum testa contra o banco e nenhum usa mocks (`measures/tests.md`).
**Nota:** bom

Os testes conferem as regras com valores, inclusive a parte nova do cenário:
- **O preço:** a alta temporada (599,76, `DomainTest.java:24-29`), o NON_REFUNDABLE com 116,97 de desconto e 662,83 de total (`DomainTest.java:32-38`) e os limites da temporada, 14/12, 15/12, 29/02 e 01/03 (`DomainTest.java:41-46`).
- **Os três documentos com o texto inteiro:**
  - o comprovante (`DomainTest.java:49-59`);
  - a fatura com o ISS de R$ 38,99 (`DomainTest.java:62-75`);
  - o booking receipt com "1 night", `Discount` e BRL (`DomainTest.java:78-90`).
- **O dinheiro formatado nos dois padrões** (`DomainTest.java:114-117`).
- **As situações e a devolução** (`DomainTest.java:93-111`, `ApiTest.java:104-111`).
- **Pela API:** o fluxo completo com a empresa, conferindo o e-mail com o ISS, o título da fatura no check-out e os dois relatórios em CSV linha a linha (`ApiTest.java:48-76`).
- **As recusas:** nenhuma identificação, duas juntas, CNPJ sem a razão social, datas, hóspedes, quarto e conflito (`ApiTest.java:79-101`).

A pergunta da mutação: trocar o 5 do ISS, o "night"/"nights" ou o separador do BRL faz algum teste falhar. Ficam sem rede:
- **Os códigos de erro.** Os testes de erro conferem só o status, não o código (`ApiTest.java:80-100`), então trocar ROOM_UNAVAILABLE por outro código passaria.
- **O "1 diária" em português.**
- **A NON_REFUNDABLE cancelada no relatório financeiro.**
- **A persistência.** O caminho JDBC (`JdbcReservationRepository`, `Database`) não é exercitado por nenhum teste, e é aí que está a maior parte das linhas não cobertas. O texto não pedia testes, e o que foi escrito é denso e confere valores.

## Banco

**Números:** 2 tabelas (`reservations`, com 17 colunas, e `messages`, com 5), ambas com chave primária. O dinheiro está em `numeric(12,2)` e as datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, UNIQUE ou EXCLUDE. `status`, `rate_plan`, `identity_kind` e `channel` são `text` sem CHECK. O esquema é criado com `CREATE TABLE` no código, sem migração. Não há pool: há uma chamada a `DriverManager.getConnection`, nenhuma transação explícita e nenhum `FOR UPDATE`. Os dois SQL montados com `+` só concatenam uma constante (`measures/database.md`).
**Nota:** com problemas

O que está certo:
- **Os tipos** são os certos (`Database.java:29-55`).
- **O SQL** é todo parametrizado. A concatenação em `JdbcReservationRepository.java:61,74` é a constante `COLUMNS` (linha 18).
- **As conexões** fecham por try-with-resources.
- **Os dados ficam no banco:** as 3 conferências de reinício passaram.

Os defeitos:
- **O esquema não tem como evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:28-55`), sem migrações versionadas, então o próximo pedido que acrescentar uma coluna não terá como alterar uma tabela que já existe.
- **O quarto não é uma tabela**, e `room_number` não tem chave estrangeira (`Database.java:31`).
- **Não há restrição para as regras** que o banco poderia proteger (situação, plano, a não sobreposição de noites).
- **A proteção contra reserva duplicada vale para um processo só.** É o `synchronized` em `ReservationService.java:20`, mais um `findAll()` que traz a tabela inteira para filtrar em Java (`ReservationService.java:31-34`, `JdbcReservationRepository.java:71-81`). Duas instâncias do serviço reservariam o mesmo quarto. Os relatórios também leem a tabela inteira (`ReportService.java:19`, `34`).
- **A confirmação faz duas escritas sem transação**: grava a reserva e depois a mensagem (`ReservationService.java:50-51`). Se a segunda falhar, a reserva fica confirmada sem o e-mail registrado.
- **Cada operação abre uma conexão nova** via `DriverManager`, sem pool (`Database.java:20`).
- **A identificação é guardada como JSON** dentro de uma coluna `text` (`Database.java:35`, `JdbcReservationRepository.java:89`), opaca para consulta.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou. Parte comum: 46 de 46. Reinício: 3 de 3. Cenário 05: 32 de 32. Duas reservas ao mesmo tempo: 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Tudo o que o texto pede funciona pela API, a partir de um banco vazio:
- **Os preços** com a temporada e o meio para o par (599,76; desconto de 37,48).
- **As recusas** com os códigos certos.
- **A confirmação** com o e-mail.
- **O check-in e o check-out.**
- **As devoluções:** STANDARD devolve o que foi pago, NON_REFUNDABLE não devolve nada, pendente não devolve nada.
- **Os dois relatórios em CSV**, inclusive a NON_REFUNDABLE cancelada que continua contando.
- **Os dados** continuam depois de reiniciar.

No cenário, cada hóspede recebe o seu documento nos três lugares:
- **o comprovante** para quem tem CPF;
- **a fatura** com a empresa e o ISS retido de 5%;
- **o booking receipt em inglês**, com BRL 2,783.52, datas 2027-03-10, "1 night" e a linha Discount.

O e-mail continua em português, os campos que não se aplicam vêm `null`, e as recusas por falta de identificação, por duas juntas ou por uma incompleta respondem 400. As reservas simultâneas também passaram, mas só por causa do `synchronized` de um processo. Para o dono da pousada, isso só importa se um dia o sistema rodar em mais de uma instância, e conta na coluna do banco.

## Dúvidas

- **A forma do pattern.** É a clássica: um método de fábrica, `IdentityType.format()`, sobrescrito em cada tipo de hóspede. O tipo de hóspede é o criador, e quem usa pede o documento pela identificação da reserva.
- **A nota da arquitetura.** A skill põe "tudo num pacote" entre os sinais de `ruim`, mas aqui as camadas existem como classes bem separadas: o HTTP só no `Api`, nenhum SQL fora dos repositórios, nenhum Javalin nos serviços. Por isso dei `com problemas`. Um revisor mais literal poderia dar `ruim`.
