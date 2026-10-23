# Revisão: manutenção, cenário 05

## Visão geral

O agente partiu de uma base com 7 pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.documents`,
`hotel.rooms`, `hotel.messages`, `hotel.reports`) e manteve o mesmo desenho. Uma requisição entra em
`hotel.web.HttpApi`, que lê o corpo e monta o `NewReservation`. A identificação do hóspede é validada em
`Identification.of` (`src/main/java/hotel/web/HttpApi.java:127-129`). Depois a requisição passa pelo
`ReservationService` e chega ao `ReservationStore`, que grava no PostgreSQL por JDBC; o esquema vem do Flyway.

A mudança tem três partes:
- um tipo selado `Identification` com três casos (`Cpf`, `Company`, `Passport`);
- uma fábrica concentrada que escolhe o leiaute do documento pelo tipo de identificação. Os três leiautes são
  subclasses de `DocumentLayout`, que guarda a sequência do texto, e um `enum Language` reúne as palavras e os
  formatos de cada idioma;
- uma migração nova (`V2__guest_identification.sql`).

Os 31 testes passam e as 83 conferências do roteiro também. Não há defeito sério. O que há a apontar é pequeno:
- um campo morto e rótulos repetidos à mão nos leiautes;
- a validação do pedido HTTP reaproveitada na leitura do banco;
- a regra "exatamente uma identificação" sem proteção no banco, depois que o agente tirou o `NOT NULL` do CPF.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.documents` passou de 2 classes (79 linhas) para 9 (283 linhas). `hotel.reservations` passou de 13 classes para
17. A maior classe continua sendo `HttpApi`, com 172 linhas (eram 170).
**Nota:** bom

- **Borda HTTP:** continua fina. A rota só tirou o `isBlank(guestCpf)` da checagem dos campos obrigatórios e delega a
  regra da identificação ao domínio (`src/main/java/hotel/web/HttpApi.java:117-129`).
- **Regras:** a regra de "um jeito só" ficou em `hotel.reservations`, em `Identification`
  (`src/main/java/hotel/reservations/Identification.java:39-60`), sem Javalin e sem JDBC.
- **Documentos:** a escolha e a montagem do documento ficaram em `hotel.documents`.
- **SQL:** continua todo no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-56` e
  `130-137`).
- **Dependências:** nenhuma dependência nova entre pacotes. O agente tornou público `PriceCalculator.cents`
  (`src/main/java/hotel/reservations/PriceCalculator.java:50`) para o cálculo do ISS em
  `src/main/java/hotel/documents/InvoiceLayout.java:34`. Isso usa uma aresta que já existia (documents → reservations)
  e não cria ciclo novo.
- **Ciclo:** o ciclo documents ↔ reservations vem da base. O serviço cria `ReservationDocument` e o documento lê
  `Reservation`; o agente não o piorou.

## Design de baixo nível

**Números:** os três casos (CPF, empresa, estrangeiro) estão na estrutura e nenhum fica fora dela. Inclui o caso que
já existia na base (o comprovante do CPF, que virou `ReceiptLayout`). A escolha mora num lugar só, um `switch`
exaustivo sobre o tipo selado (`src/main/java/hotel/documents/DocumentLayout.java:15-21`). Os três lugares que emitem
o documento (`confirm`, `checkOut` e `receiptCopy`, em
`src/main/java/hotel/reservations/ReservationService.java:64,109,118`) passam todos por essa fábrica, através do
construtor de `ReservationDocument` (`src/main/java/hotel/documents/ReservationDocument.java:33-35`). Nenhum lugar
pergunta pelo tipo de hóspede fora da fábrica.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É a forma "fábrica concentrada" que a referência aceita: `DocumentLayout.forIdentification` escolhe o
leiaute pelo tipo de identificação. Cada leiaute diz só o que é seu: o título, o idioma, as linhas de identificação e
as linhas depois do total (`ReceiptLayout.java:10-23`, `InvoiceLayout.java:15-36`, `BookingReceiptLayout.java:11-25`).
A sequência das linhas está escrita uma vez, em `DocumentLayout.text` (`DocumentLayout.java:36-54`).

A agência de turismo prevista no pedido fica fácil:
1. um caso novo em `Identification`;
2. uma subclasse de `DocumentLayout`.

O `switch` sobre o tipo selado não compila enquanto o caso novo não tiver documento, o que é um ponto a favor. O
comprovante do CPF saiu da classe da base (`ReservationReceipt`, apagada) e entrou na estrutura, sem cópia paralela.

**O exagero.** Não há estrutura sem uso. O `enum Language` (`src/main/java/hotel/documents/Language.java:8-60`) tem
dois valores e os dois são usados. A classe abstrata tem três subclasses. Não há interface com uma implementação só,
nem camada que só repassa.

**O resto do código.** Os defeitos são pequenos e localizados:
- **Campo morto e rótulos repetidos.** `Language.guest` é declarado e preenchido (`Language.java:35,47`) e nunca é
  lido. Os leiautes escrevem "Hóspede"/"Guest" à mão (`ReceiptLayout.java:21`, `InvoiceLayout.java:29`,
  `BookingReceiptLayout.java:23`), repetindo à mão o que o enum já guarda.
- **Acessores que devolvem `null`.** `Identification` tem acessores `default` que devolvem `null` para os casos que
  não se aplicam (`src/main/java/hotel/reservations/Identification.java:18-36`). Os leiautes leem `companyName()` ou
  `passportNumber()` pela interface (`InvoiceLayout.java:26-28`, `BookingReceiptLayout.java:22-24`) e confiam que a
  fábrica os casou com o caso certo. Fazer o casamento de padrão no record daria essa garantia pelo compilador.
- **Validação do pedido reaproveitada na leitura do banco.** `ReservationStore.read` monta a identificação com a mesma
  validação do pedido HTTP (`src/main/java/hotel/reservations/ReservationStore.java:131-136`). Uma linha
  inconsistente no banco viraria um `INVALID_REQUEST` (400) numa consulta, como se a culpa fosse do cliente.
- **Testes novos fora do recuo.** Os testes novos foram colados sem recuo, na coluna 0
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:158-240`,
  `src/test/java/hotel/web/HttpApiTest.java:108-140`). Isso também desalinhou o cabeçalho de dois testes da base.

O dinheiro continua em `BigDecimal`, com o arredondamento da base, e as datas em `LocalDate`.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 31 métodos de teste no código. A base tinha 26,
todos mantidos, e o agente escreveu 5 novos. A cobertura de linhas é de 95,0% (571 de 601) e a de ramos de 87,2% (130
de 149); na base eram 93,8% e 87,1%. `hotel.documents` tem 90 de 90 linhas e 7 de 7 ramos cobertos
(`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

Os testes novos conferem a parte nova com valores exatos, contra o banco de verdade:
- **Fatura:** o texto inteiro, com "ISS retido (5%): R$ 38,99", o `guestCpf` nulo e o e-mail levando o mesmo texto
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-186`).
- **Booking receipt:** o texto inteiro, em BRL e com datas ISO, e o e-mail em português
  (`ReservationServiceTest.java:188-207`). Um teste separado confere "1 night" e a linha "Discount: BRL 37.48"
  (`ReservationServiceTest.java:209-220`).
- **Comprovante do CPF:** o texto inteiro, para provar que ficou igual ao da base (`ReservationServiceTest.java:222-236`).
- **Pela API** (`src/test/java/hotel/web/HttpApiTest.java:108-137`):
  - as recusas: nenhuma identificação, CNPJ sem razão social, passaporte sem país, CPF junto com passaporte;
  - as reservas de empresa e de estrangeiro, com os campos que não se aplicam vindo `null`;
  - a segunda via com `companyCnpj` e `text`.

A pergunta da mutação:
- Trocar `0.05` por outro valor, ou tirar o `cents` do ISS, quebra os testes (o texto é comparado inteiro).
- Trocar o `nights == 1` também quebra.
- Trocar `"%,.2f"` por `"%.2f"` no inglês (`src/main/java/hotel/documents/Language.java:26`) passa despercebido:
  nenhum teste tem valor acima de mil, e o separador de milhar "BRL 1,234.56", que o pedido mostra, fica sem rede.
- O check-out da fatura e do booking receipt não é testado. Só o do CPF tem teste, e ele confere o total, não o
  título (`ReservationServiceTest.java:133-146`).

São lacunas pequenas perto do que está conferido, e os 26 testes da base continuam passando.

## Banco

**Números:** 3 tabelas, todas com chave primária. Há 2 migrações do Flyway: `V1__create_tables.sql` igual à da base e
`V2__guest_identification.sql` nova. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e
nenhuma data está guardada como texto. A restrição `EXCLUDE` contra duas reservas na mesma noite foi mantida. As
colunas de situação e de tipo não têm `CHECK`, e isso vem da base (`measures/database.md`).
**Nota:** com problemas

**A evolução está certa.** O agente não editou a migração da base. Criou a V2, que tira o `NOT NULL` de `guest_cpf` e
acrescenta as quatro colunas novas
(`src/main/resources/db/migration/V2__guest_identification.sql:2-6`). Um banco que já roda o sistema sobe sem perder
dado. O SQL novo continua com parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:32-56`); as
concatenações que o script lista são só a constante `COLUMNS`.

**O defeito.** Na base, o banco garantia que toda reserva tinha identificação: `guest_cpf text NOT NULL`
(`src/main/resources/db/migration/V1__create_tables.sql:24`). A V2 retira essa garantia e não põe outra no lugar.
Nada no banco impede uma reserva sem nenhuma identificação, com duas, ou com CNPJ sem razão social. Um
`CHECK (num_nonnulls(...) ...)` resolveria. A regra fica só em Java (`Identification.java:39-60`). Se uma linha
assim entrar por outro caminho, a leitura falha com `INVALID_REQUEST`
(`ReservationStore.java:131-136`), e com ela a consulta dessa reserva e a listagem de todas. O defeito não perde dado
no uso normal, mas enfraquece uma garantia que a base tinha.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo o que o pedido descreve funciona pela API:
- **A pessoa com CPF** recebe o mesmo comprovante de antes.
- **A empresa** recebe a fatura, com a empresa no lugar do CPF e o ISS retido de R$ 38,99, no e-mail (que continua
  em português), no check-out e na segunda via.
- **O estrangeiro** recebe o booking receipt em inglês, com "BRL 2,783.52", datas 2027-03-10, "1 night" e a linha
  "Discount".
- **As identificações erradas** (nenhuma, duas juntas, ou incompleta) dão 400 com `INVALID_REQUEST`.
- **Na resposta**, os campos que não se aplicam vêm `null`, e o documento traz o campo `text`.
- **O resto do sistema:** a parte comum continua inteira, os dados sobrevivem ao reinício e a reserva simultânea do
  mesmo quarto continua recusada.

O defeito do banco (a regra da identificação sem `CHECK`) não aparece em nenhuma chamada que o texto descreve; está
contado na coluna do banco.

## Dúvidas

- **A forma do pattern.** Foi a fábrica concentrada: um método estático, `DocumentLayout.forIdentification`, com
  `switch` sobre o tipo selado. Não foi o Factory Method clássico, nem um método polimórfico no próprio
  `Identification`. A referência aceita essa forma como acerto. O texto do documento é montado por um Template Method
  (`DocumentLayout.text`).
- **A nota de design.** Fiquei entre `bom` e `com problemas`. Os defeitos apontados são pequenos: o campo morto, os
  rótulos repetidos, os acessores que devolvem `null` e a validação do pedido usada na leitura do banco. Dei
  `com problemas` porque um revisor exigente pediria para corrigi-los antes de aceitar.
- **A nota do banco.** Pelo mesmo critério, a falta de `CHECK` também poderia ser vista como estilo herdado da base,
  que não tem `CHECK` nas colunas de situação.
