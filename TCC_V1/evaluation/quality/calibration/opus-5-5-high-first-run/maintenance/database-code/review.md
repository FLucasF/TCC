# Revisão: manutenção, cenário 05

## Visão geral

O projeto manteve a forma da base: `hotel.web.HttpApi` lê a requisição e chama `hotel.reservations.ReservationService`, que guarda pelo `ReservationStore` (JDBC com pool Hikari), e a montagem fica em `Hotel.java:21-27`. O agente pôs a escolha do documento numa fábrica, `hotel.documents.DocumentFactory`, que monta o comprovante, a fatura ou o *booking receipt*, e os três lugares que emitem documento passam a chamá-la. Essa parte está bem feita, e o roteiro passa 83 de 83 conferências contra um banco vazio. O banco, porém, piorou muito em relação à base. O agente **apagou a migração do Flyway** (`V1__create_tables.sql`) e a trocou por um `CREATE TABLE IF NOT EXISTS` escrito em Java, sem chave estrangeira, sem `NOT NULL`, sem `CHECK` e sem a restrição de exclusão que impedia duas reservas do mesmo quarto na mesma noite (`Database.java:17-31`). Ele também reescreveu o `INSERT` da reserva **concatenando no SQL o que o cliente manda** (`ReservationStore.java:34-44`). Conferi as consequências chamando a API eu mesmo:
- um hóspede chamado "Sean O'Brien" recebe 500;
- um CPF forjado gravou no banco uma reserva CONFIRMED com R$ 0,01 pago, enquanto a API respondia PENDING de R$ 499,80;
- sobre um banco criado pela versão anterior (o banco que a pousada já usa), a versão nova responde 500 até para consultar uma reserva antiga, e não cria nenhuma reserva nova, porque as colunas novas nunca são acrescentadas.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies.md` e `measures/dependencies-base.md`). O pacote `hotel.documents` passou de 2 classes (79 linhas) para 7 (222 linhas). O pacote `hotel` deixou de usar o Flyway.
**Nota:** com problemas

As camadas continuam separadas como na base. As rotas só leem e respondem (`web/HttpApi.java:64-79`). O SQL fica só nas classes de acesso a dados (`reservations/ReservationStore.java`). As novas classes de documento não conhecem nem o Javalin nem o JDBC. O ciclo `documents` ↔ `reservations` já existia na base, e o agente não o piorou: `ReservationService` usa `DocumentFactory`, e os geradores de documento leem `Reservation`.

Os dois pontos a corrigir são do agente:
- **A regra "exatamente um jeito de identificar o hóspede" mora no handler HTTP** (`web/HttpApi.java:126-134`). O `Guest` só a anuncia num comentário (`reservations/Guest.java:4-5`) e não a garante, e o `ReservationService.create` (`reservations/ReservationService.java:29-50`) aceita um hóspede sem nenhuma identificação ou com as três. Quem chamar o serviço por outro caminho (os testes, por exemplo, fazem isso) passa por cima da regra, e a fábrica escolhe então pela ordem dos `if`.
- **A fábrica é criada por dentro do serviço** (`reservations/ReservationService.java:19`) e não na montagem de `Hotel.java:25`. Como ela não tem dependências, o defeito é pequeno.

Há ainda uma mudança de responsabilidade: o esquema e a carga dos quartos foram parar como texto dentro da classe que abre o pool (`Database.java:17-31`). Esse defeito é tratado na coluna do banco.

## Design de baixo nível

**Números:** 3 de 3 tipos de documento na estrutura, inclusive o comprovante que já existia. 3 de 3 lugares que emitem documento pedem à fábrica (`ReservationService.java:66`, `:111` e `:120`). Nenhum `new ReservationReceipt` sobra fora da fábrica, e a decisão está num lugar só (`documents/DocumentFactory.java:19-27`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada: `DocumentFactory.writerFor` escolhe pelo tipo de hóspede (`documents/DocumentFactory.java:19-27`), e cada `DocumentWriter` (`documents/DocumentWriter.java:7-12`) monta o título e o texto do seu documento (`PersonReceiptWriter`, `CompanyInvoiceWriter`, `ForeignReceiptWriter`). O texto que existia na base saiu do `record` e foi para `PersonReceiptWriter`, de modo que a pessoa com CPF também passa pela estrutura. Uma agência de turismo pede um gerador novo e um ramo novo num lugar só.

O que afasta um pouco do ideal:
- o tipo de hóspede não existe como tipo: ele é deduzido de campos nulos (`reservations/Guest.java:20-26`), com a empresa vencendo o estrangeiro pela ordem dos `if`;
- o `title()` repete a primeira linha do `text()` em cada gerador (por exemplo, `documents/PersonReceiptWriter.java:10-17`).

**O exagero.** A interface `DocumentWriter` tem três implementações e é a peça do pattern. Não há builder, camada que só repassa nem interface com uma única implementação: contenção.

**O resto do código** (o que puxa a nota para baixo):
- **SQL montado à mão com dado de fora:** `reservations/ReservationStore.java:34-41`, com a função de aspas improvisada em `:52-54`. Isso troca um `PreparedStatement` que funcionava por um defeito de segurança e de correção.
- **Código morto:** a constante `EXCLUSION_VIOLATION` ficou sem uso (`reservations/ReservationStore.java:23`), junto com o tratamento que transformava a violação da restrição em `ROOM_UNAVAILABLE`.
- **Duplicação:**
  - `CompanyInvoiceWriter` copia linha a linha a parte comum do comprovante: compare `documents/CompanyInvoiceWriter.java:22-32` com `documents/PersonReceiptWriter.java:20-29`;
  - o arredondamento do ISS repete à mão o `PriceCalculator.cents` (`documents/CompanyInvoiceWriter.java:33-34`).
- **Um registro com sete campos que podem ser nulos** (`reservations/Guest.java:8-15`), e a invariante que eles deveriam respeitar não é garantida em lugar nenhum.

São defeitos localizados, mas o primeiro precisa ser corrigido antes de aceitar.

## Testes

**Números:** 29 testes rodaram, 0 falharam, 0 pulados; 29 métodos escritos (a base tinha 26, todos mantidos e passando). Cobertura de linhas de 94,3% (541 de 574) e de ramos de 82,9% (126 de 152); a base tinha 93,8% e 87,1%. No pacote `hotel.web`, os ramos cobertos são 29 de 48 (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente acrescentou três testes, todos contra o banco, em `src/test/java/hotel/documents/DocumentsByGuestTest.java`. Eles conferem com valores:
- a linha da empresa e o ISS de R$ 38,99 na fatura (`:47-49`);
- as datas e o total em BRL do *booking receipt* (`:59-61`);
- o título do comprovante (`:68`).

Pela pergunta da mutação, trocar 0.05 por 0.06 ou o `Locale.US` por outro faria um teste falhar. Ficam sem nenhum teste:
- **A validação da identificação**, a regra nova mais importante (`web/HttpApi.java:126-134`): apagar `companyIncomplete` ou trocar `!= 1` por `< 1` não derruba nenhum teste, e daí vêm os ramos descobertos de `hotel.web`.
- **Os campos novos no JSON** da reserva e do documento, incluindo o `text`.
- **"1 night"**, a linha "Discount:" e o separador de milhar em inglês (`documents/ForeignReceiptWriter.java:28-37`).
- **O documento novo dentro do e-mail e no check-out.**
- **Um nome com apóstrofo:** o teste que pegaria o `INSERT` concatenado.

## Banco

**Números:** 3 tabelas, todas com chave primária e dinheiro em `numeric(10,2)`. Em `reservations` e nas outras tabelas resta só o `NOT NULL` da chave primária, e não há nenhuma chave estrangeira, nenhum `CHECK` e nenhum `UNIQUE`/`EXCLUDE`. O esquema é criado por `CREATE TABLE` em `Database.java`, e a migração da base `V1__create_tables.sql` foi **APAGADA** (`measures/database.md`).
**Nota:** ruim

Comparado com a base, o agente desmontou o banco:
- **A migração versionada foi apagada.** O Flyway continua declarado no `pom.xml:43-49`, mas não é mais chamado, e em seu lugar entrou um `CREATE TABLE IF NOT EXISTS` (`Database.java:17-31`, executado em `:45-49`). Esse comando não acrescenta colunas numa tabela que já existe. Testei: subi a `base` num banco, criei uma reserva e subi a `final` sobre o mesmo banco. `GET /reservations/1`, a criação com CPF e a criação com empresa responderam 500, com `column "company_cnpj" does not exist`. No banco da pousada, a versão nova não funciona.
- **As restrições sumiram.** Saíram a chave estrangeira para `rooms`, os `NOT NULL`, os `CHECK` (capacidade, diária, `guests >= 1`, saída depois da entrada) e a `EXCLUDE USING gist` que impedia duas reservas do mesmo quarto na mesma noite. Agora a reserva dupla só é evitada pelo `synchronized` de `ReservationService.java:29`, que vale para um processo só. A restrição de exclusão da base é a que o pedido de mudança não tinha nenhum motivo para tocar.
- **O SQL é montado com dado de fora:** `ReservationStore.java:34-44`. O script não o listou porque procura `+` antes de constantes, mas a leitura não deixa dúvida. Testei:
  - nome "Sean O'Brien" → 500 (`Unterminated string literal`);
  - um `guestCpf` forjado gravou no banco a reserva com `status = CONFIRMED`, `total = 0.01` e `amount_paid = 0.01`, enquanto a API respondia `PENDING` e `499.80`.

  É dado errado gravado a partir de qualquer pedido do site.

O resto do código continua com parâmetros (`update`, `isRoomTaken`, `find`, `all`), try-with-resources e pool, como na base.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas simultâneas | 2 de 2 |

**Nota:** ruim

Contra um banco vazio, tudo o que o roteiro confere funciona:
- a fatura com o ISS de R$ 38,99;
- o *booking receipt* com datas ISO, "1 night", "Discount:" e BRL 2,783.52;
- as quatro recusas com 400;
- o documento no e-mail, no check-out e na segunda via;
- os campos `null` que não se aplicam;
- as reservas simultâneas, que o `synchronized` resolve com um processo só.

Fora do roteiro, chamei a API eu mesmo e achei três defeitos que pesam para o dono da pousada:
1. **No banco que a pousada já usa, o sistema para de funcionar:** consultar uma reserva antiga e criar qualquer reserva nova dá 500. Isso contraria o pedido de que "tudo o que funciona hoje tem que continuar funcionando".
2. **Um hóspede com apóstrofo no nome** (O'Brien, D'Ávila), justamente entre os estrangeiros que motivaram a mudança, não consegue reservar (500).
3. **Um pedido malicioso grava uma reserva paga por R$ 0,01**, e o relatório financeiro e o check-in passam a acreditar nela.

## Dúvidas

- Os dois testes manuais que derrubaram o comportamento ficam fora do roteiro fixo, que passou 83 de 83. Se a coluna for julgada só pelo roteiro, que roda contra um banco vazio, a nota seria `bom`. Dei `ruim` porque as falhas foram reproduzidas pela API (o banco criado pela base dá 500, o nome com apóstrofo dá 500, o CPF forjado grava CONFIRMED de R$ 0,01) e atingem o uso real da pousada.
- A forma do pattern é a fábrica concentrada (uma classe com um método que escolhe o gerador pelo tipo de hóspede), e não o Factory Method clássico com subclasses do criador; a referência aceita as duas como acerto.
- Na arquitetura, a nota `com problemas` se apoia em defeitos pequenos (a regra de identificação no handler e a fábrica criada dentro do serviço). Um revisor menos exigente poderia dar `bom`.
