# Revisão: manutenção, cenário 05

## Visão geral

A base já tinha as camadas separadas, e o agente as manteve: `hotel.web` (Javalin: `HttpApi`, os records do JSON),
`hotel.reservations` (as regras no `ReservationService` e o SQL no `ReservationStore`), `hotel.documents` (o
documento), `hotel.messages`, `hotel.reports`, `hotel.rooms`, e a montagem em `hotel.Hotel`. Uma requisição
`POST /reservations` passa por `HttpApi`, que lê o corpo, monta a `Identification` com
`CreateReservationRequest.identification()` (`src/main/java/hotel/web/CreateReservationRequest.java:27`), recusa com 400
quando não há um jeito só (`src/main/java/hotel/web/HttpApi.java:127`) e chama o `ReservationService`, que grava pelo
`ReservationStore` no PostgreSQL. O esquema evolui por Flyway: a V1 da base ficou igual e o agente acrescentou a V2. A
mudança foi feita com cuidado. O `ReservationReceipt` da base deu lugar a `ReservationDocument.of`, que pede o layout a
uma fábrica só (`DocumentLayout.forGuest`), e os três pontos do serviço usam essa fábrica. Os testes passam (30, eram
26), e o roteiro de comportamento passou inteiro (83 de 83). O defeito mais sério está no banco: a V2 tirou o
`NOT NULL` de `guest_cpf` e não pôs nada no lugar. Desde então o banco aceita uma reserva sem identificação nenhuma, e a
leitura transforma essa reserva num `Cpf(null)`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já estava na base. O
Javalin só aparece em `hotel` e `hotel.web`, e o JDBC fica em `reservations`, `messages`, `rooms` e `hotel`, como na
base.
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes e não piorou o ciclo, que veio da base: o serviço emite o
documento, e o documento lê a reserva. A borda HTTP continua fina. A validação da identificação mora no record do
corpo (`src/main/java/hotel/web/CreateReservationRequest.java:27-41`), e o handler só converte o resultado em erro
(`src/main/java/hotel/web/HttpApi.java:127-130`). As regras do documento ficam em `hotel.documents` e não conhecem nem
Javalin nem JDBC (`src/main/java/hotel/documents/DocumentLayout.java:1-7`). O SQL continua todo no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:31-57` e `130-137`). Houve uma única abertura: para calcular o
ISS, `PriceCalculator.cents` passou a ser `public` (`src/main/java/hotel/reservations/PriceCalculator.java:50`), e
`InvoiceLayout` usa esse método (`src/main/java/hotel/documents/InvoiceLayout.java:26`). Com isso o arredondamento fica
num lugar só, e a dependência de `documents` para `reservations` já existia. Nenhuma classe cresceu demais: a maior
ainda é `HttpApi`, com 173 linhas (eram 170).

## Design de baixo nível

**Números:** há 3 tipos de documento, e os três estão na estrutura: `ReceiptLayout` (o comprovante que já existia),
`InvoiceLayout` e `BookingReceiptLayout`. A escolha de qual criar acontece num único `switch`, exaustivo sobre o tipo
selado (`src/main/java/hotel/documents/DocumentLayout.java:24-30`). Os 3 lugares que emitem documento pedem a esse
ponto e não criam nada à mão (`src/main/java/hotel/reservations/ReservationService.java:64`, `:109` e `:118`). Não
sobrou nenhuma referência ao `ReservationReceipt`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É uma fábrica concentrada, que a referência aceita como acerto. `DocumentLayout.forGuest` escolhe o
layout pelo tipo de identificação, e cada layout é uma subclasse que sobrescreve só o que varia: quem é o hóspede
(`guestLines`) e o que vem depois do total (`closingLines`). Ficam na classe-mãe os passos comuns
(`src/main/java/hotel/documents/DocumentLayout.java:48-65`), o idioma e o formato de dinheiro e de data
(`src/main/java/hotel/documents/DocumentLanguage.java:12-15`). A base fazia um `new ReservationReceipt` em cada um dos
três pontos; agora os três chamam `ReservationDocument.of`
(`src/main/java/hotel/documents/ReservationDocument.java:34-55`), e o caso da base (CPF) também entrou na estrutura,
em `ReceiptLayout`. Para a agência de turismo bastam uma subclasse e um ramo no `switch`. Como `Identification` é
selada (`src/main/java/hotel/reservations/Identification.java:7`), o compilador aponta o ramo que faltar. O mesmo
`switch` também protege a escolha de outro jeito: ninguém fora da fábrica pergunta qual é o caso.

**O exagero.** Não há. `DocumentLanguage` tem dois valores e os dois são usados: o português, pelo comprovante e pela
fatura, e o inglês, pelo booking receipt. O enum evita copiar o texto do documento para cada idioma. A
`Identification` selada com três records representa o domínio de verdade. Não apareceu interface com uma
implementação só, nem camada que só repassa.

**O resto do código.** O dinheiro continua em `BigDecimal`. O ISS usa o mesmo arredondamento da base, e 779,80 dá
38,99 (`src/main/java/hotel/documents/InvoiceLayout.java:11` e `:26`). Encontrei pontos pequenos, que não chegam a
pedir mudança antes de aceitar:
- A interface `Identification` tem acessores `default` que devolvem `null` para todos os campos
  (`src/main/java/hotel/reservations/Identification.java:9-27`), e os layouts leem por eles em vez de pelo record
  certo (`src/main/java/hotel/documents/BookingReceiptLayout.java:15-16`,
  `src/main/java/hotel/documents/InvoiceLayout.java:19-20`). Isso enfraquece o tipo selado: se a fábrica e o layout
  se desencontrarem, o documento sai com "null" e o compilador não reclama. Em troca, é o que preenche com `null`, sem
  esforço, os campos do JSON.
- `readIdentification` cai em `new Identification.Cpf(null)` quando a linha não tem nenhuma identificação
  (`src/main/java/hotel/reservations/ReservationStore.java:130-137`).
- A declaração de `read` perdeu a indentação (`src/main/java/hotel/reservations/ReservationStore.java:140`).
- O construtor do `DocumentLanguage` recebe 12 argumentos posicionais (`src/main/java/hotel/documents/DocumentLanguage.java:30-33`).

## Testes

**Números:** 30 testes rodaram, sem falha nem pulado (a base tinha 26, todos mantidos). Há 30 métodos escritos. A
cobertura é de 94,8% das linhas (566 de 597) e 86,9% dos ramos (126 de 145); na base era 93,8% e 87,1%. O pacote
`hotel.documents` tem 87 de 87 linhas e 7 de 7 ramos cobertos, e `hotel.web` tem 36 de 50 ramos (era 18 de 28)
(`measures/tests.md` e `measures/tests-base.md`).
**Nota:** bom

O agente escreveu 4 testes, e eles conferem a parte nova com os valores do pedido:
- A fatura é conferida pelo texto inteiro, com o ISS de R$ 38,99. O teste confere também os campos nulos e se o
  e-mail leva a fatura e começa em português (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-187`).
- O booking receipt é conferido pelo texto inteiro (BRL, datas ISO, "2 nights")
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:191-210`), e há outro teste com "1 night" e a linha
  Discount (`:213-223`).
- Pela API, o teste passa por 9 combinações inválidas (nenhuma identificação, duas juntas, uma incompleta), todas com
  400 INVALID_REQUEST, e depois pelo caminho da empresa até o documento, com ISS de R$ 46,79
  (`src/test/java/hotel/web/HttpApiTest.java:109-136`).

Fiz a pergunta da mutação. Trocar 0.05 por outra taxa, o formato `%,.2f` do inglês, `== 1` por outra condição na
noite, ou a contagem de campos da validação: todas essas trocas derrubam algum teste. Os testes da base só mudaram no
tipo (`Guest` com `Identification.Cpf`, `ReservationDocument`), e nenhum ficou mais fraco.

Ficaram de fora duas coisas pequenas:
- A linha "(CPF …)" do comprovante não é conferida em texto. Isso já vinha da base, que só conferia o título e as
  linhas de desconto e total. Como o comprovante foi reescrito dentro da estrutura nova, uma troca nessa linha passaria
  sem ser vista.
- O booking receipt no check-out e no e-mail também não tem teste. A fatura tem.

O roteiro de comportamento cobre as duas lacunas.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, e a chave
estrangeira tem índice. A V1 da base ficou igual, e a V2 é nova (`measures/database.md`). O script só acusou o que já
vinha da base: as colunas de situação e de plano sem CHECK. A duplicação de reserva continua protegida pelo `EXCLUDE`
da base.
**Nota:** com problemas

A evolução foi bem feita. O agente não editou a V1, acrescentou a
`src/main/resources/db/migration/V2__guest_identification.sql` com quatro colunas, e as reservas antigas continuam
válidas. O SQL continua com parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:32-46`).

O defeito é que a V2 tira o `NOT NULL` de `guest_cpf` (`src/main/resources/db/migration/V2__guest_identification.sql:2`)
e não põe nada no lugar, nem um `CHECK` que exija um jeito só de identificar: o CPF, ou o CNPJ com a razão social, ou o
passaporte com o país. Na base, o banco garantia que toda reserva tinha identificação
(`base/src/main/resources/db/migration/V1__create_tables.sql:24`). Agora quem garante é só o `HttpApi`. Uma linha sem
identificação, ou com CNPJ e passaporte juntos, entra no banco. Na leitura, essa linha vira silenciosamente um
`Cpf(null)`, ou a empresa ganha prioridade (`src/main/java/hotel/reservations/ReservationStore.java:130-137`), e o
documento sai com "CPF null". Não perde dado na operação normal, mas a regra ficou mais fraca no banco.

## Comportamento

**Números:** passaram 83 de 83 conferências: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 32 de 32 no cenário
05 e 2 de 2 nas reservas simultâneas (`measures/behavior.md`).
**Nota:** bom

Tudo o que o pedido descreve funcionou pela API:
- O hóspede com CPF continua recebendo o mesmo comprovante.
- A empresa recebe a fatura: a empresa no lugar do CPF e o ISS retido de R$ 38,99. A fatura vai no e-mail, que continua
  em português, e sai no check-out e na segunda via.
- O estrangeiro recebe o booking receipt, com BRL 779.80, datas 2027-03-10, "1 night", a linha Discount e o milhar com
  vírgula (BRL 2,783.52).
- A reserva sem identificação, com CPF e CNPJ juntos ou com uma identificação incompleta é recusada com 400
  INVALID_REQUEST.
- Os campos que não se aplicam vêm `null` na reserva e no documento.

Para o dono da pousada, o sistema faz o que ele pediu, sem quebrar o que já funcionava. A falta de CHECK no banco não
aparece em nenhuma chamada que o pedido descreve, porque a API recusa antes; ela pesa na coluna do banco.

## Dúvidas

- A forma do Factory Method que o agente usou é a fábrica concentrada: um método estático com `switch` exaustivo sobre
  a `Identification` selada (`DocumentLayout.forGuest`), que devolve subclasses de `DocumentLayout`. Não é a forma
  clássica, com um criador sobrescrito por tipo, nem um método polimórfico no hóspede. A referência aceita essa forma
  como acerto.
- Na nota de design ficou uma dúvida pequena: os acessores `default` que devolvem `null` em `Identification` podem ser
  vistos como uma interface "gorda" que enfraquece o tipo selado. Mantive `bom` porque são usados de forma coerente para
  preencher os campos nulos do JSON, e os testes e o roteiro não mostraram nenhum `null` vazando no texto.
