# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes. Uma requisição passa por `hotel.web.HttpApi`, que lê o JSON e monta o
pedido, segue para `hotel.reservations.ReservationService`, onde ficam as regras, e chega a
`hotel.reservations.ReservationStore`, que faz o JDBC com `?`. As migrações do Flyway ficam em `db/migration`. O agente
mudou a base assim:

- O `Guest` ganhou quatro campos que podem vir nulos: CNPJ, razão social, passaporte e país.
- Entrou uma migração nova, `V2`, com as colunas e um `CHECK` de "exatamente uma identificação".
- O `HttpApi` passou a conferir o "um jeito e só um".
- O documento passou a ser escolhido num lugar só, `hotel.documents.DocumentFactory`, com um `DocumentWriter` para cada
  documento. Os três lugares que emitem documento usam essa fábrica.

O comportamento está todo certo: 83 de 83 conferências passaram.

O mais sério é nos testes. O agente **apagou os quatro arquivos de teste da base**, com 26 testes, e pôs no lugar um
único teste que só confere o status 200 de `GET /rooms`. A cobertura caiu de 93,8% para 18,9% das linhas, e nada da
parte nova tem teste. Não havia motivo para apagar: rodei os 26 testes da base contra o código final, numa cópia em
`/tmp`, e todos passaram (27 rodaram, 0 falhas).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base, sem nenhuma aresta nova. Há 1 ciclo,
`hotel.documents ↔ hotel.reservations`, que já existia na base (`measures/dependencies-base.md`: 7 pacotes, 17
dependências, o mesmo ciclo). O `hotel.documents` cresceu de 79 para 222 linhas e o `hotel.web` de 236 para 265. A
maior classe continua sendo o `HttpApi`, com 184 linhas (eram 170).
**Nota:** bom

- **As camadas continuam no lugar.** O Javalin só aparece em `hotel` e em `hotel.web`, e o JDBC só em `reservations`,
  `rooms` e `messages`.
- **Os documentos não dependem de infraestrutura.** O pacote não usa Javalin nem JDBC; os writers recebem
  `Reservation` e `Room` (`DocumentWriter.java:10`).
- **A troca nos três lugares é de uma linha.** O serviço só troca o `new ReservationReceipt(...)` por
  `documents.issue(...)` (`ReservationService.java:66`, `:111`, `:120`).
- **O ciclo é o da base.** O `ReservationService` já importava `hotel.documents.ReservationReceipt` na base. O agente
  só acrescentou o import de `DocumentFactory` (`ReservationService.java:3`), sem criar ciclo novo.
- **A validação nova ficou na borda.** Ela está em `HttpApi.java:126-134`, como a validação de campos obrigatórios que a
  base já fazia ali (`HttpApi.java:116-125`). Isso é leitura de entrada, não regra de negócio.
- **O banco protege a mesma regra.** O `CHECK` da `V2__guest_identification.sql:8-11` faz a mesma conferência.

Há só um ponto pequeno: a fábrica é criada por dentro do serviço (`ReservationService.java:19`,
`new DocumentFactory()`), e não montada no `Hotel` com as outras peças. Como ela não tem dependências nem estado, isso
não pesa.

## Design de baixo nível

**Números:** há 3 documentos e todos estão na estrutura: o comprovante (`PersonReceiptWriter`), que é o caso que já
existia na base, a fatura (`CompanyInvoiceWriter`) e o booking receipt (`ForeignReceiptWriter`). A escolha mora num
único método (`DocumentFactory.java:19-27`). Nenhum dos três pontos de emissão cria documento à mão: são 3 de 3 pela
fábrica.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente usou uma fábrica concentrada:

- `DocumentFactory.issue` (`DocumentFactory.java:14-17`) pergunta ao `Guest` qual é o tipo
  (`writerFor`, `:19-27`, com `isCompany()` e `isForeigner()`, em `Guest.java:20-26`) e entrega o
  `ReservationReceipt` com o título e o texto do writer certo.
- Os três lugares (`ReservationService.java:66`, `:111`, `:120`) pedem o documento à fábrica e não perguntam o tipo de
  hóspede.
- O comprovante da base também foi para a estrutura: o texto saiu de `ReservationReceipt.text()` e foi para
  `PersonReceiptWriter.java:15-30`.
- Uma agência de turismo vai precisar de um writer novo e de um ramo em `writerFor`, sem mexer no serviço.

A referência do pattern aceita essa forma como acerto.

**O exagero.** Não há. A interface `DocumentWriter` (`DocumentWriter.java:7-12`) tem três implementações e é a própria
estrutura que o pattern pede. Não entrou nenhuma camada, builder ou fábrica além disso.

**O resto do código.** Tem defeitos localizados:

- **Texto duplicado.** O corpo da fatura copia o do comprovante quase linha a linha: compare
  `CompanyInvoiceWriter.java:22-32` com `PersonReceiptWriter.java:18-29` (hóspede, quarto, entrada e saída, diárias,
  desconto e total). Uma mudança no comprovante português precisa ser feita duas vezes.
- **O título repetido.** O título também se repete dentro de cada writer: `PersonReceiptWriter.java:11` e `:17`,
  `CompanyInvoiceWriter.java:13` e `:19`.
- **O tipo de hóspede é deduzido de campos nulos.** O `Guest` é um record com sete campos, quatro deles opcionais
  (`Guest.java:7-14`). O tipo sai de qual campo não é nulo (`Guest.java:20-26`); não há `enum` nem tipo próprio. O
  próprio `Guest` aceita qualquer combinação, e a regra "um e só um" fica só no `HttpApi.java:126-134` e no `CHECK` do
  banco.
- **A alíquota do ISS solta no texto.** O 5% é um literal `new BigDecimal("0.05")` dentro do código que monta o texto
  (`CompanyInvoiceWriter.java:33-34`), sem nome. O arredondamento está certo: meio para o par, como o
  `PriceCalculator.cents`.

O dinheiro continua em `BigDecimal` e as datas em `LocalDate`. O formato `BRL 1,234.56` sai de
`String.format(Locale.US, "%,.2f")` (`ForeignReceiptWriter.java:36-38`), o que está certo.

## Testes

**Números:**

| | final | base |
|---|---|---|
| testes que rodaram | 1 (0 falhas, 0 erros, 0 pulados) | 26 (0 falhas) |
| métodos de teste escritos | 1 | 26 |
| cobertura de linhas | 18,9% (110 de 583) | 93,8% |
| cobertura de ramos | 2,6% (4 de 152) | 87,1% |
| `hotel.documents`, linhas | 7 de 87 | — |
| `hotel.documents`, ramos | 0 de 16 | — |

**Nota:** ruim

**O agente apagou os quatro arquivos de teste da base.** O `changes.patch` marca como `deleted file`:

- `ReportsTest` (5 testes);
- `PriceCalculatorTest` (6);
- `ReservationServiceTest` (11);
- `HttpApiTest` (4).

Isso tirou do projeto os testes de preço com temporada e arredondamento, de cancelamento e devolução, de situações da
reserva, de códigos de erro e dos relatórios. A remoção não era necessária: o agente manteve o construtor de três
argumentos do `Guest` (`Guest.java:16-18`) e o acessor `text()` do `ReservationReceipt`. Copiei os testes da base para
uma cópia do `final/` em `/tmp` e rodei `mvn test`: 27 testes, 0 falhas.

**O teste que entrou não confere regra nenhuma.** É o `HotelSmokeTest.theServiceStarts`
(`src/test/java/hotel/HotelSmokeTest.java:13-25`). Ele sobe o serviço e só confere o status 200 de `GET /rooms` (`:23`).

**A pergunta da mutação.** Todas as mudanças abaixo passariam despercebidas, porque nada da parte nova é testado:

- trocar `0.05` por `0.5` (`CompanyInvoiceWriter.java:33`);
- trocar `Locale.US` por `pt-BR` no booking receipt (`ForeignReceiptWriter.java:37`);
- inverter a ordem de `writerFor` (`DocumentFactory.java:20-26`);
- apagar a conferência `identifications != 1` (`HttpApi.java:131`).

O `mvn verify` passa, mas só porque quase não há o que verificar.

## Banco

**Números:** 3 tabelas, todas com chave primária. Flyway com 2 migrações: a `V1` está igual à da base e a
`V2__guest_identification.sql` é nova. O dinheiro está em `numeric(10,2)` e não há chave estrangeira sem índice nem
data em texto. As colunas `status`, `rate_plan`, `type` e `channel` não têm `CHECK`, como na base. Entrou uma restrição
nova, `reservations_one_identification`. Os candidatos de "SQL montado com `+`" só concatenam constantes (`COLUMNS`) e
usam `?` para os dados.
**Nota:** bom

**A evolução do esquema está certa.** Uma migração nova (`V2__guest_identification.sql:2-11`):

- torna `guest_cpf` opcional (`:3`);
- acrescenta as quatro colunas (`:4-7`);
- e o `CHECK` de exatamente uma identificação (`:8-11`).

A `V1` não foi tocada, então quem já roda o sistema sobe sem problema.

**O código fala com o banco com parâmetros.** A gravação e a leitura das colunas novas usam `?`
(`ReservationStore.java:32-34`, `:51-54`, `:129-131`).

**A proteção contra reserva dupla é da base e continua.** É o `EXCLUDE` da `V1`, e o roteiro confirmou:
2 de 2 conferências de reservas simultâneas.

**Um detalhe pequeno no `CHECK`.** Ele conta um par só quando os dois campos estão preenchidos (`V2:10-11`). Por isso
aceitaria um CPF com um CNPJ sem razão social. O `HttpApi.java:129-131` recusa esse caso antes, então nenhum dado
errado entra pela API.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, isto quer dizer:

- **Quem tem CPF** continua recebendo o mesmo comprovante.
- **A empresa** recebe a fatura com a empresa no lugar do CPF e o ISS retido de 5% certo (R$ 38,99 sobre R$ 779,80).
- **O estrangeiro** recebe o booking receipt em inglês, com `BRL 2,783.52`, as datas `2027-03-10`, "1 night" e a
  linha "Discount".
- **O e-mail** continua em português e leva o documento certo.
- **O check-out e a segunda via** devolvem o mesmo documento, com os campos novos e o `text`.
- **As recusas pedidas** dão 400 `INVALID_REQUEST`: nenhuma identificação, CPF e CNPJ juntos, CNPJ sem razão social e
  passaporte sem país.
- **Tudo o que já funcionava** continua funcionando.

## Dúvidas

- **A forma do pattern.** É uma fábrica concentrada (uma classe que escolhe o writer pelo tipo de hóspede), e não o
  Factory Method clássico com criador sobrescrito. A referência aceita as duas como acerto.
- **A nota do design.** Ficou entre `bom` e `com problemas`. Pesaram a cópia do texto do comprovante na fatura e o tipo
  de hóspede deduzido de campos nulos, sem a regra "um e só um" no domínio. São defeitos localizados, mas são o lugar
  onde o documento da agência vai doer.
- **O README** não foi atualizado com os campos novos da API (ele está idêntico ao da base). Não pesei isso em nenhuma
  coluna.
