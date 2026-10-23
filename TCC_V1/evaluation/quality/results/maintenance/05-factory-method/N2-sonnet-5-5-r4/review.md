# Revisão: manutenção, cenário 05

## Visão geral

O agente mudou a base sem mexer na sua forma: `hotel.web` (Javalin, `HttpApi`) lê o JSON, monta o `NewReservation` e chama o `ReservationService` (`hotel.reservations`), que fala com o banco pelo `ReservationStore` (JDBC, PostgreSQL, migrações do Flyway). O documento continua sendo o record `ReservationReceipt` (`hotel.documents`), criado com `new` nos três lugares de sempre (`ReservationService.java:64`, `:109`, `:118`). A mudança: o `Guest` passou a ter uma `Identification` (três records: `CpfIdentification`, `CompanyIdentification`, `PassportIdentification`). Ela é validada num lugar só (`Identifications.java:14-27`), e o construtor do `ReservationReceipt` pede a `DocumentLayouts` (`DocumentLayouts.java:11-21`) o layout de cada tipo. O layout é uma classe abstrata com o texto escrito uma vez (`DocumentLayout.java:47-63`), com uma subclasse por documento. Uma migração nova (`V2__guest_identification.sql`) acrescenta as colunas. Tudo passa: 31 testes, 83 de 83 conferências do roteiro. O que há de mais sério é pequeno. O banco deixou de garantir que a reserva tem identificação: o `NOT NULL` do CPF caiu e não entrou um `CHECK` no lugar. A interface `Identification` devolve `null` por padrão para cada campo de cada tipo, e isso faz cada tipo novo mexer em vários arquivos.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base; um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). Nenhuma dependência nova entre pacotes. `hotel.documents` passou de 2 para 7 classes (79 → 341 linhas) e `hotel.reservations` de 13 para 19 (508 → 612). A maior classe continua sendo `HttpApi` (170 → 173 linhas).
**Nota:** bom

- **A borda HTTP continua fina.** A única mudança em `HttpApi.java:118-131` foi tirar o `guestCpf` da lista de campos obrigatórios e passar os cinco campos de identificação para `Identifications.of` (`HttpApi.java:128`). A regra "um jeito só, e completo" mora em `hotel.reservations` (`Identifications.java:14-27`), sem Javalin.
- **O SQL continua só no `ReservationStore`** (`ReservationStore.java:31-60` e `:130-137`). Os layouts dos documentos não conhecem JDBC nem Javalin (`measures/dependencies.md`, tabela das bibliotecas).
- **A escolha do documento fica no pacote dos documentos** (`DocumentLayouts.java`). O pacote passou a importar os tipos de identificação de `hotel.reservations`, mas essa direção (`documents -> reservations`) já existia.
- O único ajuste de visibilidade foi tornar `PriceCalculator.cents` público (`PriceCalculator.java:50`), para a fatura arredondar o ISS como o resto do sistema arredonda. É razoável e não cria dependência nova.

O ciclo `documents` ↔ `reservations` vem da base: o serviço cria o `ReservationReceipt` e o recibo lê a `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), os 3 na estrutura, inclusive o comprovante com CPF que já existia, e nenhum fora dela. 3 lugares criam o documento (`ReservationService.java:64`, `:109`, `:118`), e todos passam pelo mesmo construtor, que pede o layout à fábrica (`ReservationReceipt.java:33-35`). Não sobrou nenhum `if`/`switch` sobre o tipo de hóspede no código de produção.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A decisão de qual documento criar está num lugar só: o mapa `LAYOUTS` de `DocumentLayouts.java:11-14`, do tipo de identificação para o layout. Os três lugares do serviço não mudaram nada: continuam com `new ReservationReceipt(reservation, room)`, e o construtor delega a escolha a `DocumentLayouts.of` (`ReservationReceipt.java:33-35`). O texto é escrito uma vez em `DocumentLayout.text` (`DocumentLayout.java:47-63`). Cada documento só diz o que é dele:

- o idioma, nos rótulos;
- o dinheiro: `BookingReceiptLayout.java:64`, com `Locale.US` e separador de milhar;
- as datas;
- quem é nomeado: `party`;
- o que vem depois do total: `closing`, com o ISS em `InvoiceLayout.java:26-27`.

A fatura herda do comprovante (`InvoiceLayout.java:9`) e troca só o título, a parte e o fecho, como o pedido descreve. O caso que já existia (CPF) foi para a estrutura (`ReceiptLayout.java`) e o texto dele continua igual ao da base. A agência entra com uma identificação nova, um layout e uma linha no mapa. É uma fábrica concentrada, que a referência aceita como acerto.

**O exagero.** Não há estrutura sem uso:

- as três identificações e os três layouts têm, cada um, o seu caso;
- `Identifications.Candidate` (`Identifications.java:29-38`) é um record privado pequeno que evita três `if`s quase iguais;
- o construtor de conveniência `Guest(name, email, cpf)` (`Guest.java:6`) mantém os testes da base sem mudança.

**O resto do código (o que pesa na nota):**
- **A interface `Identification` é "gorda".** Ela tem um getter com `default null` para cada campo de cada tipo (`Identification.java:7-27`). Os layouts leem os campos por esses getters genéricos (`InvoiceLayout.java:20-21`, `BookingReceiptLayout.java:23-24`) e dependem de o mapa ter juntado o layout certo com a identificação certa. Nada no tipo garante isso. Na agência, além da identificação, do layout e da linha no mapa que o README promete, alguém vai mexer também em:
  - `Identification`;
  - `Identifications.of`;
  - os campos de `ReservationReceipt`, `ReservationResponse` e `CreateReservationRequest`;
  - o `COLUMNS` e o `INSERT` do `ReservationStore`.

  Parte disso é inevitável, porque são campos novos na API e no banco. Mas a interface poderia ser `sealed`, com a leitura tipada por caso.
- **Um tipo de identificação sem layout no mapa vira `null` sem aviso.** `DocumentLayouts.of` (`DocumentLayouts.java:19-21`) devolve `null` e o erro aparece como `NullPointerException` em `ReservationReceipt.java:39`. No `confirm`, isso acontece depois de a reserva já ter sido gravada como `CONFIRMED` (`ReservationService.java:62-64`): a reserva fica confirmada sem o e-mail. Um `requireNonNull` com mensagem, ou um `switch` exaustivo sobre uma interface `sealed`, pegaria o esquecimento na hora.
- **O mesmo validador serve para o pedido do site e para a leitura do banco.** Ao ler do banco, o `ReservationStore` usa `Identifications.of` (`ReservationStore.java:131`), que lança `INVALID_REQUEST`. Uma linha inconsistente no banco viraria um 400 "pedido inválido" no `GET` da reserva. Isso é pequeno, mas mistura validação de entrada com leitura de dados.
- O resto está limpo: `BigDecimal` com o arredondamento da casa para o ISS (`InvoiceLayout.java:27`), `LocalDate`, nomes claros, métodos curtos. O que não mudou veio da base sem defeitos novos.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados; 31 métodos escritos (a base tinha 26, todos mantidos e sem mudança). Cobertura de 94,9% das linhas (559/589) e 88,1% dos ramos (111/126); na base, 93,8% e 87,1%. Em `hotel.documents`: 79 de 79 linhas e 6 de 6 ramos.
**Nota:** bom

**O que a parte nova confere, com valores:**
- `ReservationServiceTest.java:170-191` compara o texto inteiro da fatura do exemplo do pedido, inclusive `ISS retido (5%): R$ 38,99`. Confere também `companyCnpj`, o `guestCpf` nulo e se o e-mail de confirmação leva a fatura.
- `ReservationServiceTest.java:194-207` compara o texto inteiro do booking receipt do exemplo (`BRL 779.80`, datas ISO, "2 nights").
- `ReservationServiceTest.java:210-220` confere "1 night" e `Discount: BRL 37.48` antes do total.
- `HttpApiTest.java:128-146`: criação pela API com empresa e com passaporte, os campos nulos e o documento pela segunda via (título, `guestCountry`, `text`).
- `HttpApiTest.java:149-161`: cinco combinações recusadas com 400 `INVALID_REQUEST` (nenhuma; CPF com passaporte; CNPJ sem razão social; passaporte sem país; CPF com país).

**A pergunta da mutação.** Trocar 0.05 por 0.5, tirar o `Discount`, trocar "night" por "nights" ou aceitar duas identificações seria pego.

**O que ficaria sem rede:**
- **O separador de milhar do booking receipt.** Trocar `"%,.2f"` por `"%.2f"` em `BookingReceiptLayout.java:64` não quebra nenhum teste: nenhum valor testado passa de 999. O roteiro do comportamento conferiu o milhar, e está certo.
- **O texto do comprovante com CPF.** Ele foi reescrito (passou de `ReservationReceipt.text` para `ReceiptLayout`), mas nenhum teste o compara inteiro. A base já só conferia o título e as linhas de desconto e total (`ReservationServiceTest.java:95-96` e `:155`). Um erro na linha "Hóspede: … (CPF …)" passaria. A falta vem da base, mas o agente mexeu nesse código e não pôs uma rede.
- **O check-out com fatura ou booking receipt.** Não é testado à parte, mas passa pelo mesmo construtor que os testes exercitam.

Os testes rodam contra o PostgreSQL de verdade e pela API, sem mocks, como os da base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data como texto. As restrições da base continuam, inclusive o `EXCLUDE` contra duas reservas na mesma noite. `V1__create_tables.sql` está igual ao da base, e `V2__guest_identification.sql` é nova. O SQL é todo com `?`: as concatenações apontadas pelo script juntam só constantes (`COLUMNS`). As colunas de situação e de plano não têm `CHECK`, mas isso vem da base.
**Nota:** com problemas

- A mudança no esquema entrou do jeito certo: uma migração nova, sem editar a da base (`V2__guest_identification.sql:2-6`). Quem já roda o sistema sobe sem perder dado, e as reservas antigas, que têm só CPF, continuam válidas.
- **O banco deixou de proteger a identificação.** Na base, `guest_cpf` era `NOT NULL` (`V1__create_tables.sql:24`), então toda reserva tinha identificação. A V2 tira esse `NOT NULL` (`V2__guest_identification.sql:2`) e não põe nada no lugar. Hoje o banco aceita uma reserva sem nenhuma identificação, com duas, ou com CNPJ sem razão social. A regra "um jeito só, e completo" existe só no Java (`Identifications.java:14-27`). Se uma linha dessas aparecer (uma correção feita à mão, outro sistema escrevendo), a leitura falha com `INVALID_REQUEST` (`ReservationStore.java:131`). Isso derruba o `GET` dessa reserva e qualquer listagem que leia todas as reservas (`ReservationStore.java:118`, usada pelos relatórios). Um `CHECK` (com `num_nonnulls` sobre os três grupos) manteria a garantia que a base tinha.
- O código que fala com o banco continua bom: as colunas novas entram por parâmetro (`ReservationStore.java:51-56`), as conexões ficam em try-with-resources e passam pelo pool da base.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam. A parte comum deu 46 de 46; os dados depois de reiniciar, 3 de 3; o cenário 05, 32 de 32; as duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o pedido descreve funciona pela API:

- **Com CPF:** o hóspede continua recebendo o mesmo comprovante.
- **Empresa:** recebe a fatura com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80. A fatura vai no e-mail, que continua em português, e no check-out.
- **Estrangeiro:** recebe o booking receipt em inglês, com `BRL`, separador de milhar (`BRL 2,783.52`), datas `2027-03-10`, "1 night" e a linha `Discount`.
- **Recusas:** nenhuma identificação, duas identificações, ou uma incompleta, dão 400 `INVALID_REQUEST`.
- **Campos nulos:** na consulta da reserva e no documento, os campos que não se aplicam vêm `null`.
- **O resto:** o que já funcionava (preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar, as reservas simultâneas) continua funcionando.

A falta de `CHECK` no banco (veja a coluna Banco) não aparece em nenhuma chamada que o texto descreve, e não muda esta nota.

## Dúvidas

- A forma do pattern é uma **fábrica concentrada**: um mapa do tipo de identificação para o layout, em `DocumentLayouts.java:11-21`, chamado pelo construtor de `ReservationReceipt`. Os layouts são uma pequena hierarquia com o texto escrito uma vez. O que a fábrica escolhe é o layout, e não uma classe de documento: o documento continua sendo um record só, com `title` e `text` vindos do layout. Pela referência isso é acerto, porque a escolha mora num lugar só e os três lugares passam por ela. Alguém poderia ler como "a fábrica devolve sempre o mesmo documento", mas a variação inteira (título, texto, fecho) está no objeto escolhido pela fábrica, e não depois dela.
- A nota `com problemas` do design se apoia em pontos pequenos e localizados: a interface `Identification` com `default null` e o `null` sem aviso de `DocumentLayouts.of`. Um revisor mais tolerante poderia aceitar o código como está.
