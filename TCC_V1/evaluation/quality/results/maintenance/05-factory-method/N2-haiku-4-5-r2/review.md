# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas e DTOs), `hotel.reservations` (serviço, regras de
preço e `ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports`,
`hotel.rooms` e `hotel` (montagem). Uma requisição entra pelo `HttpApi` e chama o `ReservationService`, que usa o
`ReservationStore`. Os dados ficam no PostgreSQL, com migrações do Flyway. O agente pôs no `Guest` quatro campos que
podem vir nulos e acrescentou a migração `V2`, as classes `CompanyInvoice` e `BookingReceipt` e um método
`generateDocumentText` que escolhe o documento. Só que esse método só é chamado no e-mail de confirmação e nas
respostas da reserva. O check-out (`ReservationService.java:104-112`) e a segunda via (`ReservationService.java:115-121`)
continuam montando `new ReservationReceipt(...)` para qualquer hóspede. Por isso a empresa e o estrangeiro recebem o
comprovante brasileiro, com "CPF null", sem o campo `text` e sem os campos novos: 20 das 32 conferências do cenário
falharam. Há mais dois problemas. Um pedido com CNPJ sem razão social, ou com passaporte sem país, dá 500 em vez de 400.
E o ISS é calculado sobre o subtotal, quando o pedido diz que é sobre o total.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem as mesmas
dependências e o mesmo ciclo (`measures/dependencies-base.md`). Os tamanhos mudaram assim: `HttpApi` passou de 170
para 200 linhas, `ReservationService` de 138 para 153 e `ReservationStore` de 143 para 156.
**Nota:** bom

O agente seguiu as camadas da base. As rotas só leem a requisição, chamam o serviço e escrevem a resposta
(`HttpApi.java:64-91`). A escolha do documento ficou no serviço (`ReservationService.java:123-134`), sem Javalin e sem
SQL. As colunas novas ficaram só no `ReservationStore` (`ReservationStore.java:31-55`, `:129-137`). Nenhuma
dependência nova apareceu entre pacotes. O ciclo `documents` ↔ `reservations` vem da base: os documentos leem
`Reservation`, e o serviço cria os documentos. O agente só pôs mais duas classes nele. Há um ponto pequeno. A
validação de "exatamente uma identificação" e a montagem do `Guest` por caso ficaram na borda HTTP
(`HttpApi.java:139-156`), e o serviço repete a mesma pergunta sobre qual campo está preenchido
(`ReservationService.java:127-133`). Isso é design (ver abaixo); não chega a misturar camadas.

## Design de baixo nível

**Números:** a escolha de qual documento criar está num lugar só (`generateDocumentText`), mas só 1 dos 3 lugares do
pedido a usa: o e-mail. O check-out e a segunda via continuam com `new ReservationReceipt` à mão. A pergunta sobre o
tipo de hóspede aparece em 2 lugares (`HttpApi.java:149-156` e `ReservationService.java:127-133`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern:** a referência aceita como acerto uma fábrica concentrada, isto é, um método que escolhe o documento
pelo tipo de hóspede. O que aproxima o código disso: `generateDocumentText` (`ReservationService.java:123-134`) é esse
ponto único, cada documento tem a sua classe (`CompanyInvoice`, `BookingReceipt`) e o e-mail o usa
(`ReservationService.java:66`).

O que afasta, e define o "malfeito":

- dois dos três lugares que o pedido cita ainda criam o documento à mão, sempre o mesmo:
  - `checkOut` (`ReservationService.java:111`);
  - `receiptCopy` (`ReservationService.java:120`), com o tipo de retorno fixo em `ReservationReceipt`
    (`:104`, `:115`).
- A fábrica não devolve um documento. Devolve só o `String` do texto, porque os três records não têm um tipo comum.
  Assim, quem precisa do documento em JSON (o check-out e a segunda via) não tem como usá-la.
- Ela também volta a ler a reserva do banco por id (`:124`), e cada rota de `HttpApi` faz uma segunda consulta
  (`HttpApi.java:67-86`).

Uma agência de turismo, no modelo atual, vai precisar de mais um `else if` em `ReservationService.java:127-133`, de
mais um ramo em `HttpApi.java:149-156`, de mais colunas no `Guest` e de mais campos que podem vir nulos.

**O exagero:** não há estrutura sem necessidade: nenhuma interface vazia, fábrica extra ou camada que só repassa.

**O resto do código:**

- O tipo de hóspede não existe como tipo. Ele é deduzido de qual campo veio não nulo, num `Guest` com sete campos dos
  quais quatro ficam sempre nulos (`Guest.java:3-14`). Essa dedução está duplicada em `HttpApi.java:149-156` e
  `ReservationService.java:127-133`.
- A validação conta um grupo como "presente" quando só uma das metades vem preenchida (`HttpApi.java:141-142`). Depois,
  o `else` final (`:154-155`) trata qualquer caso que sobrou como passaporte. Um CNPJ sem razão social vira, então, um
  "estrangeiro" com passaporte nulo, que o banco recusa com 500.
- O ISS é calculado sobre `subtotal` (`CompanyInvoice.java:38`), quando o pedido diz "5% do total". Com desconto
  (`NON_REFUNDABLE`), o valor sai errado. O arredondamento é `HALF_UP` (`:39`), enquanto o resto do sistema usa
  `HALF_EVEN` (`PriceCalculator.java:51`), e o pedido diz "arredondado para centavos como sempre".
- `BookingReceipt.java:61` usa `String.format("BRL %,.2f", ...)` sem `Locale`. Num servidor em pt-BR, o recibo em
  inglês sairia "BRL 779,80". A base fixava o locale em `TextFormat.java:9`.
- O texto de cada documento repete a montagem de quarto, datas e diárias (`CompanyInvoice.java:44-48`,
  `BookingReceipt.java:45-49`), sem uma parte comum.
- O agente apagou comentários da base que ainda valiam (`ReservationReceipt.java:9`,
  `CreateReservationRequest.java:6`, o comentário do `catch` em `HttpApi.java:125`).

## Testes

**Números:** 31 testes rodaram, 0 falharam; 31 métodos escritos (a base tinha 26, todos mantidos). Cobertura de
linhas: 94,2% (568 de 603); a base tinha 93,8%. Ramos: 85,9% (122 de 142); a base tinha 87,1%. Em `hotel.documents`,
86 de 87 linhas e 7 de 10 ramos (`measures/tests.md`).
**Nota:** com problemas

Os cinco testes novos estão todos em `HttpApiTest.java:123-194` e conferem pouco do que o pedido traz:

- A fatura e o booking receipt são conferidos só com `contains("Fatura de hospedagem")`, `contains("Tecnosul Ltda")`
  ou `contains("Booking receipt")` (`HttpApiTest.java:137-138`, `:155-156`). Nenhum teste confere o ISS de R$ 38,99, o
  formato `BRL 779.80`, as datas `2027-03-10`, "1 night" ou a linha "Discount:". Pela pergunta da mutação: trocar
  `subtotal` por `total` no ISS, trocar `0.05` por `0.50`, ou tirar o `%,` do dinheiro em inglês não derruba nenhum
  teste.
- Nenhum teste passa pelo check-out nem pela segunda via de uma empresa ou de um estrangeiro. Por isso o defeito
  principal (o documento errado nesses dois lugares) passa com todos os testes verdes.
- As recusas testadas são nenhuma identificação e duas identificações (`:159-182`). Falta o caso incompleto (CNPJ sem
  razão social), que dá 500.
- `documentTextIncludesCorrectInformation` (`:185-194`) confere o texto do comprovante com CPF com valores, o que é
  bom, mas só cobre o caso que já existia.

## Banco

**Números:** 3 tabelas, 2 migrações (a `V1` da base igual; a `V2` nova). Nenhuma conferência acusou problema: todas
as tabelas têm chave primária, o dinheiro está em `numeric(10,2)`, a chave estrangeira tem índice e as datas estão em
`date`. As colunas de situação sem CHECK (`status`, `rate_plan`, `type`, `channel`) já vinham assim da base
(`measures/database.md`).
**Nota:** bom

O esquema evoluiu do jeito certo:

- uma migração nova (`V2__add_company_and_passport.sql:1-6`), sem editar a `V1`, que tira o `NOT NULL` do CPF e
  acrescenta as quatro colunas;
- um CHECK que garante exatamente uma forma de identificação completa (`V2__add_company_and_passport.sql:8-13`). Ele é
  compatível com as linhas antigas, que têm CPF e nada mais.

O SQL continua com parâmetros (`ReservationStore.java:32-55`). As concatenações apontadas pelo script juntam só
constantes (`COLUMNS`, `ReservationStore.java:18-20`). A conexão volta ao pool por try-with-resources. A proteção
contra reserva dupla é a restrição `EXCLUDE` da base. Um detalhe de nome: a coluna se chama `guest_cnpj`, mas o campo
Java é `companyCnpj` (`ReservationStore.java:135`). Foi o CHECK novo que transformou a falha da validação HTTP em 500,
em vez de gravar um dado errado. O banco cumpriu o papel dele, e o defeito conta no comportamento e no design.

## Comportamento

**Números:** 63 de 83 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 12 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** ruim

O que funciona é a criação dos três tipos (201, com os campos que não se aplicam em null), as recusas de "nenhuma" e
de "duas" identificações, e o e-mail de confirmação, que leva a fatura em português.

O que falhou, e o que significa para o dono da pousada:

- **O check-out e a segunda via entregam o documento errado.** Para a empresa e para o estrangeiro, os dois devolvem o
  "Comprovante de reserva" brasileiro, com "CPF null" (`ReservationService.java:111`, `:120`).
  - Nesses dois lugares, a empresa nunca recebe a fatura com o ISS retido, e o estrangeiro nunca recebe o booking
    receipt.
  - O JSON do documento não tem o campo `text` nem os quatro campos novos, nem para o hóspede com CPF.
  - Daí vêm 18 das 20 falhas: o tipo e o texto da fatura, o tipo e o texto do booking receipt, "1 night", "Discount:",
    `BRL 2,783.52`, os cinco campos e o texto do CPF.
- **CNPJ sem razão social, ou passaporte sem país, dá 500 em vez de 400 INVALID_REQUEST.** O site recebe um erro
  interno, e o log mostra a violação de `check_one_identification` (`measures/behavior-service.log:56`, `:115`).

Há dois defeitos fora do roteiro, já contados no design:

- o ISS sobre o subtotal faria a fatura de uma reserva `NON_REFUNDABLE` mostrar um imposto maior que 5% do total;
- o booking receipt depende do locale da máquina.

A maior parte do cenário falha nos lugares que o dono citou ("no check-out e na segunda via"), e por isso a nota é
`ruim`.

## Dúvidas

- A classe: a fábrica concentrada existe (`generateDocumentText`), mas dois dos três lugares criam o documento à mão.
  Pela referência isso é `malfeito`. A forma usada é um método de fábrica no serviço que devolve só o texto, não um
  documento.
- O pedido diz que o JSON do documento ganha o campo `text`. O agente pôs `text` na resposta da reserva
  (`ReservationResponse.java:30`), que o pedido não menciona, e não o pôs no documento. Não fica claro se ele leu
  "documento" como a reserva; de qualquer forma, o JSON do check-out e o da segunda via não têm o campo.
