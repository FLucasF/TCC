# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve o desenho da base: `hotel.web.HttpApi` lê o JSON e chama o `ReservationService` (pacote
`hotel.reservations`), que guarda a reserva pelo `ReservationStore` (JDBC com pool, PostgreSQL, esquema pelo Flyway) e
monta o documento pelo pacote `hotel.documents`. A mudança fica em poucos lugares. A identificação do hóspede virou um
tipo selado, `Identification` (`Cpf`, `Company`, `Passport`), em
`src/main/java/hotel/reservations/Identification.java:7-38`, com uma validação só para "um jeito, e só um"
(`Identification.java:61-77`). O antigo `ReservationReceipt` virou `ReservationDocument`, que pede o layout certo a uma
fábrica concentrada, `DocumentLayout.of` (`src/main/java/hotel/documents/DocumentLayout.java:16-22`). Cada tipo de
documento é uma subclasse de layout (comprovante, fatura, booking receipt), e as três saídas do documento (confirmação,
check-out e segunda via) chamam `ReservationDocument.of` (`ReservationService.java:64,109,118`). O banco ganhou uma
migração nova, a V2, com uma CHECK que exige exatamente uma identificação. Todos os scripts mediram sem erro: 32 testes
passaram e as 83 conferências do roteiro passaram. Não achei nenhum defeito sério. Os pontos abertos são pequenos: o
arredondamento do ISS repetido, a leitura do banco reaproveitando a validação da requisição e um teste que não pega o
separador de milhar em inglês.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. As
linhas em `hotel.documents` passaram de 79 para 276, em `hotel.reservations` de 508 para 603 e em `hotel.web` de 236
para 259. A maior classe é `DocumentLayout` (189 linhas), seguida de `HttpApi` (178).
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes, e o único ciclo vem da base: o serviço cria o documento, e o
documento lê a `Reservation`. A borda HTTP continua fina. `HttpApi.java:110-138` só passa os cinco campos para
`Identification.of`, e a regra ("só um jeito") fica em `hotel.reservations`, sem Javalin. O SQL dos campos novos ficou
no `ReservationStore` (`ReservationStore.java:31-56,130-137`). O texto dos documentos ficou em `hotel.documents`, um
pacote que não usa JDBC nem Javalin (tabela de bibliotecas em `measures/dependencies.md`). A montagem continua num lugar
só, em `Hotel.java`, que o agente não mexeu. `DocumentLayout` passou a ser a maior classe, mas porque reúne três
subclasses pequenas, e não porque faz de tudo.

## Design de baixo nível

**Números:** há 3 tipos de documento (o comprovante da base, a fatura e o booking receipt), os três na estrutura. A
escolha de qual criar acontece em 1 lugar (`DocumentLayout.java:16-22`), e nenhuma das 3 saídas do documento decide
por conta própria (`ReservationService.java:64,109,118`). Sobrou 1 `new` de documento fora da fábrica, e ele é interno
à própria fábrica (`ReservationDocument.java:37,59`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A decisão de qual documento criar mora num lugar só. `DocumentLayout.of` faz um `switch` sobre o tipo
selado `Identification`, e o compilador obriga a tratar todos os casos (`DocumentLayout.java:17-21`). As três saídas
pedem o documento a `ReservationDocument.of(reservation, room)` (`ReservationDocument.java:34-56`) e não perguntam
pelo tipo de hóspede (`ReservationService.java:64,109,118`). O caso que já existia, o comprovante para quem tem CPF,
também entrou na estrutura (`ReceiptLayout`, `DocumentLayout.java:65-112`) e não ficou como um caminho à parte. Para a
agência de turismo, bastam uma variante nova de `Identification`, uma subclasse de layout e um `case` no único
`switch`. Essa é a forma de "fábrica concentrada" que a referência aceita como acerto. Por dentro, os layouts usam um
método-modelo, `text` (`DocumentLayout.java:48-62`), para não repetir a ordem das linhas. A fatura herda do comprovante
e muda só o título, a linha do hóspede e o fechamento com o ISS (`DocumentLayout.java:115-134`).

**O exagero.** Não há estrutura sem necessidade. Não há interfaces com uma implementação só, nem builder, nem camada
que só repassa. As subclasses de layout fazem parte do que o pattern resolve.

**O resto do código.** Está limpo, com poucos pontos menores:
- O arredondamento do ISS repete à mão o `setScale(2, HALF_EVEN)` (`DocumentLayout.java:131`) em vez de usar a regra
  única de centavos, `PriceCalculator.cents` (`PriceCalculator.java:50`). Essa regra é visível só dentro do pacote,
  por isso o agente copiou. Hoje o resultado é o mesmo.
- `ReservationDocument.of` monta o registro com `text` nulo e depois faz uma cópia com o texto
  (`ReservationDocument.java:37-55,58-62`). Funciona, mas é um desvio esquisito.
- A leitura do banco reaproveita a validação da requisição (`ReservationStore.java:131`). Uma linha inconsistente no
  banco viraria um 400 `INVALID_REQUEST`, e não um erro interno. A CHECK da V2 torna isso pouco provável.
- Há uma linha com a indentação errada (`ReservationResponse.java:37`).

Fora isso, os tipos estão certos: `BigDecimal` para o ISS, `LocalDate` para as datas e um tipo selado no lugar de
textos soltos. A validação de "um e só um" (`Identification.java:62-73`) é curta e legível.

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 32 métodos de teste no código (a base tinha
26, todos mantidos). Cobertura de linhas: 564 de 594 (94,9%), contra 93,8% na base. Cobertura de ramos: 144 de 159
(90,6%), contra 87,1% na base. O pacote `hotel.documents` tem 76 de 76 linhas e 9 de 9 ramos cobertos
(`measures/tests.md`).
**Nota:** bom

O agente testou a parte nova com valores, e não só com o status da resposta:
- **A fatura:** o texto inteiro, igual ao do pedido, com o ISS de R$ 38,99, e conferido também dentro do e-mail
  (`ReservationServiceTest.java:167-192`).
- **O booking receipt:** o texto inteiro, com as datas ISO e o BRL (`ReservationServiceTest.java:195-209`).
- **Um booking receipt de uma noite, com desconto:** "1 night", "Discount: BRL 86.98" e o total
  (`ReservationServiceTest.java:212-222`).
- **O comprovante de quem tem CPF:** continua igual, com os campos que não se aplicam nulos
  (`ReservationServiceTest.java:225-234`).
- **Pela API:** oito combinações inválidas recusadas com 400 `INVALID_REQUEST`, os campos nulos na reserva e no
  documento, e o ISS de R$ 46,79 no `text` (`HttpApiTest.java:109-134`). Há ainda o booking receipt pela API
  (`HttpApiTest.java:137-151`).

Os 26 testes da base continuam lá. O agente só trocou o construtor do `Guest`, sem enfraquecer nenhuma asserção.

**A pergunta da mutação.** Trocar 0.05 por outro valor, trocar "night" por "nights" ou apagar a linha do desconto faria
os testes falharem. Duas mudanças passariam sem que nenhum teste falhasse:
- trocar `%,.2f` por `%.2f` no BRL (`DocumentLayout.java:182`), porque nenhum teste tem um valor acima de 1.000;
- trocar o layout devolvido no check-out por outro, porque o teste de check-out usa só CPF
  (`ReservationServiceTest.java:141`).

As duas lacunas são pequenas: a fábrica é a mesma nas três saídas, e o roteiro do comportamento confere o milhar.

## Banco

**Números:** 3 tabelas, todas com chave primária. O esquema é feito pelo Flyway, com 2 migrações: a V1 está igual à da
base, e a V2 é nova. Dinheiro está em `numeric(10,2)` e datas em `date`. Nenhuma chave estrangeira está sem índice. A
EXCLUDE contra duas reservas na mesma noite vem da base. As colunas de situação e de plano são `text` sem CHECK, também
vindas da base (`measures/database.md`).
**Nota:** bom

O agente fez a mudança do jeito certo para quem já roda o sistema. Ele não editou a V1 e acrescentou
`V2__guest_identification.sql`, que:
- solta o `NOT NULL` de `guest_cpf` (`V2__guest_identification.sql:3`);
- cria as quatro colunas novas (`V2__guest_identification.sql:4-7`);
- protege a regra "exatamente um jeito de identificar" com uma CHECK (`V2__guest_identification.sql:8-14`).

As reservas antigas, que têm todas um CPF, satisfazem a CHECK, então a migração passa sobre um banco com dados. O SQL
novo usa só parâmetros `?`; os pedaços concatenados são constantes (`ReservationStore.java:32-35`, a constante
`COLUMNS`). As conexões continuam em try-with-resources. Um ponto pequeno: a CHECK exige só `IS NOT NULL`, e quem barra
o texto em branco é o código Java (`Identification.java:79-81`).

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- **Quem tem CPF** continua recebendo o mesmo comprovante.
- **A empresa** recebe a fatura com a razão social, o CNPJ e o ISS retido de R$ 38,99, no e-mail (que continua em
  português), no check-out e na segunda via.
- **O estrangeiro** recebe o booking receipt com "BRL 2,783.52", as datas no formato 2027-03-10, "1 night" e a linha
  "Discount".
- **As identificações inválidas** são recusadas com 400 `INVALID_REQUEST`: nenhuma, duas juntas ou uma incompleta.
- **A consulta da reserva e o documento** trazem os campos novos, com null nos que não se aplicam, e o documento traz
  o `text`.

Nada do que já funcionava quebrou. As reservas continuam lá depois de reiniciar, e duas reservas ao mesmo tempo para o
mesmo quarto não passam.

## Dúvidas

A forma de acerto usada é a "fábrica concentrada": um método estático, `DocumentLayout.of`, que escolhe o layout por
um `switch` sobre o tipo selado `Identification`. Não é a forma clássica, com um criador sobrescrito por tipo, nem um
método polimórfico no hóspede. A referência aceita as três formas. Fora isso, nenhuma.
