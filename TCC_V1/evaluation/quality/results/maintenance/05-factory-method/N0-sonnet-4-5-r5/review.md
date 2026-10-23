# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes (`hotel` monta tudo; `hotel.web` tem as rotas; `hotel.reservations` tem
as regras e o acesso ao banco em `ReservationStore`; `hotel.documents` tem o comprovante; `hotel.rooms`,
`hotel.messages` e `hotel.reports`), com os dados no PostgreSQL e as migrações no Flyway. Uma requisição entra em
`HttpApi`, que lê o corpo e chama o `ReservationService`, que usa `Rooms`/`ReservationStore` (JDBC) e o
`EmailSender`. O agente acrescentou ao `Guest` quatro campos que podem ser nulos e um `type()` deduzido dos nulos,
criou um `GuestType`, dois documentos novos (`InvoiceReceipt` e `BookingReceipt`), um documento único para o JSON
(`DocumentReceipt`, que tem um `of(...)` que escolhe o documento pelo tipo de hóspede), a migração `V2` com um CHECK
de "exatamente uma identificação", e a validação em `HttpApi`. Tudo funciona: 83 de 83 conferências e 43 testes
passando. O que há de mais sério está no design. A escolha do documento existe em dois lugares: o `DocumentReceipt.of`
serve o check-out e a segunda via, mas o e-mail de confirmação tem um `switch` próprio, igual ao outro
(`ReservationService.java:128-134`). Além disso, a camada web passou a montar o documento sozinha
(`ReservationResponse.java:60`).

## Arquitetura

**Números:** 7 pacotes (os mesmos da base), 17 dependências entre eles (as mesmas da base) e 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). Os pacotes que
cresceram foram `hotel.documents` (de 2 classes e 79 linhas para 6 e 312), `hotel.reservations` (de 508 para 568
linhas) e `hotel.web` (de 236 para 285 linhas). A maior classe continua sendo `HttpApi` (de 170 para 199 linhas).
**Nota:** com problemas

As camadas da base continuam de pé: o JDBC fica só em `ReservationStore`, `Rooms` e `SentMessages`, e o Javalin só
em `hotel.web` e `hotel`. As colunas novas entraram no lugar certo (`ReservationStore.java:18-20,32-55,130-137`). O
ciclo veio da base, mas o agente o apertou. `ReservationService` agora importa três classes de documento
(`ReservationService.java:3-6`). `InvoiceReceipt` passou a depender de `PriceCalculator`, para o que o agente tornou
`PriceCalculator.cents` público (`PriceCalculator.java:50`, `InvoiceReceipt.java:46`).

Há uma mistura localizada, nova. A camada web passou a criar o documento por conta própria:

- `HttpApi.toResponse` busca o quarto em `Rooms` dentro do handler (`HttpApi.java:191-194`);
- `ReservationResponse` chama `DocumentReceipt.of` e repete a regra "só reserva paga tem documento", que é do
  serviço (`ReservationResponse.java:60` contra `ReservationService.java:118`).

Assim a decisão "qual documento, e quando" mora no serviço e também no DTO HTTP. As rotas também ficaram
desiguais. A criação e o cancelamento usam o construtor sem quarto e devolvem sempre `document: null`
(`HttpApi.java:66,77`). Consulta, confirmação e check-in devolvem o documento (`HttpApi.java:68-81`).

## Design de baixo nível

**Números:** a escolha do documento pelo tipo de hóspede aparece em 2 `switch` iguais
(`DocumentReceipt.java:30-36` e `ReservationService.java:128-134`). Dos três lugares que o texto pede, 2 usam a
fábrica (check-out e segunda via, `ReservationService.java:112,121`) e 1 decide sozinho (o e-mail,
`ReservationService.java:67`). Há ainda um quarto lugar que cria o documento, a resposta HTTP
(`ReservationResponse.java:60`). Os três documentos não têm um tipo comum.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima de um acerto:

- há uma fábrica concentrada, `DocumentReceipt.of(reservation, room)` (`DocumentReceipt.java:30-36`), que escolhe
  o documento pelo `guest().type()`;
- o caso que já existia (o comprovante da base) entrou nela;
- o check-out e a segunda via pedem o documento a ela (`ReservationService.java:112,121`).

O que afasta: o e-mail de confirmação, um dos três lugares que o texto nomeia, não usa a fábrica. Ele tem um
`generateDocumentText` com o seu próprio `switch` sobre `GuestType`, que cria `ReservationReceipt`, `InvoiceReceipt`
ou `BookingReceipt` à mão (`ReservationService.java:67,128-134`). Bastaria `DocumentReceipt.of(...).text()`. É o caso
que a referência chama de malfeito: a fábrica existe, mas um dos lugares cria o documento à mão. Na prática, a
agência de turismo que o dono anuncia vai exigir mexer em dois `switch`, e se alguém esquecer um deles, o e-mail e a
segunda via passam a divergir.

A forma da fábrica também pesa. Os três documentos são records sem nada em comum. `DocumentReceipt` é uma "união"
com todos os campos de todos os tipos, preenchida por três métodos de cópia de quase 20 linhas cada, que copiam
campo por campo e escrevem `null` nos que não se aplicam (`DocumentReceipt.java:38-98`). Cada documento novo pede
uma variante nova no enum, uma cláusula no `switch`, um `fromX` novo, talvez campos novos na união e a outra cláusula
em `ReservationService`.

**O exagero.** Fora da parte do pattern, não há interface de uma implementação só, fábrica, builder nem camada que
só repassa: contenção. A cópia campo a campo de `DocumentReceipt` é desajeitada, mas faz parte da própria solução do
pattern (veja as Dúvidas).

**O resto do código.**

- **O `Guest` virou um saco de campos que podem ser nulos.** O tipo é deduzido por qual campo não é nulo, e
  qualquer combinação que sobre cai em `FOREIGN` (`Guest.java:24-31`). Um `Guest` sem nenhuma identificação vira
  estrangeiro em silêncio.
- **A API de criação do `Guest` é assimétrica e fácil de errar.** Um construtor de 3 strings é a pessoa com CPF, um
  de 4 strings é a empresa, e o estrangeiro vem de um método estático (`Guest.java:12-22`). Trocar a ordem de
  `companyCnpj` e `companyName` compila.
- **O texto do documento está copiado.** As linhas de quarto, datas, diárias, desconto e total se repetem quase
  iguais em `ReservationReceipt.java:40-54` e `InvoiceReceipt.java:49-65`, e mudam só o idioma em
  `BookingReceipt.java:47-62`.
- **A taxa do ISS é um literal solto**, sem nome (`InvoiceReceipt.java:46`).
- **Há um nome de classe completo** em `HttpApi.java:191` (`hotel.reservations.Reservation`), e o `var` aparece
  misturado com o estilo explícito da base (`HttpApi.java:69,73,79`).

O dinheiro continua em `BigDecimal` com o arredondamento da base, e as datas em `LocalDate`. São defeitos
localizados, que não tornam o código perigoso de mudar.

## Testes

**Números:** 43 testes rodaram, 0 falharam, 0 pulados; 43 métodos escritos, 17 deles novos. A base tinha 26, e
todos continuam passando: o único mudado só trocou o tipo de retorno (`ReservationServiceTest.java:141`). A cobertura
foi de 93,8% para 95,2% nas linhas (634 de 666) e de 87,1% para 90,5% nos ramos (143 de 158). Em `hotel.documents`,
140 de 142 linhas e 13 de 15 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O que vai bem: há testes nos dois níveis, pelo serviço contra o banco (`GuestTypesTest`) e pela API
(`GuestTypesApiTest`). Alguns valores da parte nova são conferidos:

- o ISS de R$ 38,99 (`GuestTypesTest.java:74`);
- "Total: BRL 499.80" (`GuestTypesTest.java:105`);
- o e-mail levando a fatura e o booking receipt (`GuestTypesTest.java:84,110`);
- as quatro recusas com INVALID_REQUEST: nenhuma identificação, duas, CNPJ sem razão social, passaporte sem país
  (`GuestTypesApiTest.java:107-170`);
- os campos nulos na consulta (`GuestTypesApiTest.java:66-67,84-85,102-103`).

Feita a pergunta da mutação, várias regras novas ficam sem rede:

- **"1 night":** o teste confere `contains("1 night")` (`GuestTypesTest.java:123`), e "1 nights" também contém
  "1 night". Se alguém apagar o singular, o teste continua passando.
- **O milhar com vírgula** (`BRL 1,234.56`): nenhum teste usa valor acima de mil. Trocar `"%,.2f"` por `"%.2f"` em
  `BookingReceipt.java:66` não derruba nada.
- **A linha "Discount:" do booking receipt e o desconto na fatura:** nenhum teste novo usa `NON_REFUNDABLE`.
  Apagar o `if` de `BookingReceipt.java:56-58` não derruba nada.
- **Um teste que confere a si mesmo:** `GuestTypesTest.java:76-79` recalcula o ISS com a mesma fórmula do código
  e compara com 38,99, sem passar pelo sistema.
- **Asserções fracas:** quase todas as asserções de texto usam `contains` de um pedaço, em vez do texto inteiro
  que o dono deu. Nenhum teste confere que o documento do e-mail é igual ao da segunda via e ao do check-out.
  `checkOutReturnsCorrectDocumentForEachGuestType` confere só o título e `assertNotNull`
  (`GuestTypesTest.java:157-163`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave
estrangeira está sem índice e nenhuma data está em texto. Há 2 migrações: a `V1` da base ficou igual e a `V2` é
nova. A `reservations` ganhou 4 colunas e 1 CHECK novo. As únicas conferências que acusaram algo são `status`,
`rate_plan`, `type` e `channel` em texto sem CHECK, e isso vem da base (`measures/database.md`).
**Nota:** bom

O esquema evoluiu do jeito certo: uma migração nova, sem editar a da base
(`src/main/resources/db/migration/V2__add_guest_types.sql`). Ela tira o `NOT NULL` de `guest_cpf` e acrescenta
`company_cnpj`, `company_name`, `guest_passport` e `guest_country`, que podem ser nulos (`V2__add_guest_types.sql:3-8`).
As reservas que já existem, todas com CPF, continuam válidas. O banco também protege a regra de "exatamente uma
identificação, e completa" com um CHECK (`V2__add_guest_types.sql:11-18`), o que é mais do que o texto exigia.

O SQL novo usa parâmetros: o INSERT passou de 14 para 18 `?` (`ReservationStore.java:32-55`). A leitura monta o
`Guest` com as colunas novas (`ReservationStore.java:130-137`), e o `+` dos SELECT junta só constantes. A trava contra
duas reservas na mesma noite continua sendo o `EXCLUDE` da base.

Um ponto para o futuro, que não pesa na nota: com uma coluna por tipo de identificação, a agência de turismo vai
exigir reescrever o CHECK inteiro.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam. A parte comum passou em 46 de 46, os dados depois de
reiniciar em 3 de 3, o cenário 05 em 32 de 32, e as duas reservas simultâneas em 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, o resultado é o seguinte:

- **CPF:** quem tem CPF recebe o mesmo comprovante de antes.
- **Empresa:** recebe a fatura com a empresa no lugar do CPF e o ISS de R$ 38,99 sobre R$ 779,80, dentro de um
  e-mail em português, no check-out e na segunda via.
- **Estrangeiro:** recebe o booking receipt com `BRL 2,783.52`, as datas `2027-03-10`, "1 night" e a linha
  "Discount:".
- **Recusas:** as quatro combinações erradas do roteiro dão 400 INVALID_REQUEST.
- **O resto:** a parte comum, os dados depois de reiniciar e a trava contra reserva dupla continuam funcionando.

Fora do roteiro, chamei a API à mão. Um pedido com `guestCpf` completo mais um `companyCnpj` sem `companyName` é
aceito (201), e o CNPJ é descartado em silêncio. O mesmo acontece com CPF mais `guestPassport` sem país
(`HttpApi.java:133-141`: só conta como identificação o par completo). O texto diz "só um; se não for, 400", e esse
caso pode ser lido como um pedido errado, mas é um ponto que o texto não fechou (veja as Dúvidas). O agente também
acrescentou, sem que o texto pedisse, um campo `document` na resposta de `GET /reservations/{id}`, da confirmação e
do check-in. É um acréscimo que não quebra quem já consome a API.

## Dúvidas

- **A forma do pattern:** o agente usou uma fábrica concentrada, um método estático `DocumentReceipt.of` com
  `switch` sobre `GuestType`, que a referência aceita como acerto. A classe foi para malfeito porque o e-mail de
  confirmação não usa essa fábrica e repete o `switch` (`ReservationService.java:128-134`). Pode-se discutir se isso
  é malfeito ou omissão parcial; ficou malfeito porque a fábrica existe e serve dois dos três lugares.
- **O exagero:** a união `DocumentReceipt` e os três métodos de cópia (`DocumentReceipt.java:38-98`) poderiam ser
  lidos como uma camada que só repassa. Contei como parte da solução do pattern, e não como exagero, porque é o
  jeito que o agente escolheu para entregar um JSON único com os campos nulos que o texto pede.
- **A validação:** o texto não diz se um campo de identificação solto e incompleto, ao lado de uma identificação
  completa (CPF + CNPJ sem razão social), deve dar 400. O agente aceita o pedido e ignora o campo solto; o banco
  não guarda o campo solto, então não há dado inconsistente.
