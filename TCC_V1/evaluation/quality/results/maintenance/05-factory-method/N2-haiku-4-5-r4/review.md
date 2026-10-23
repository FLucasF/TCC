# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin, `HttpApi`) lê a requisição e chama `ReservationService`
(`hotel.reservations`), que guarda os dados no PostgreSQL por `ReservationStore` (JDBC, migrações do Flyway). Os
documentos ficam em `hotel.documents`. A mudança: o `Guest` ganhou quatro campos (`Guest.java:3-14`), uma migração nova
`V2` acrescenta as colunas, a validação de "exatamente uma identificação" mora em `HttpApi.java:127-149`, e uma
`sealed interface Document` com três registros (`ReservationReceipt`, `InvoiceDocument`, `BookingReceipt`) é escolhida
num lugar só, `ReservationService.createDocument` (`ReservationService.java:116-124`), que os três lugares de emissão
usam. Os textos dos documentos saem certos. O problema mais sério é o `DocumentResponse` novo
(`web/DocumentResponse.java:26-43`): ele passa adiante só o `reservationId` e o `text`, e preenche todos os outros
campos com `null` ou `0`. No check-out e na segunda via, o JSON do documento vem agora com `total: 0`, `guestName:
null`, `roomNumber: 0` e sem `title`, e isso vale também para o hóspede com CPF, que devia continuar como antes. Foram
7 das 83 conferências do roteiro que falharam, todas por essa causa. E os testes da base que teriam pegado isso foram
enfraquecidos.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, nos dois lados (base e final). Há 1 ciclo,
`hotel.documents ↔ hotel.reservations`, que já existia na base. A maior classe é `HttpApi` (201 linhas; eram 170).
A única diferença nas bibliotecas: agora `hotel.documents` usa Jackson.
**Nota:** bom

As camadas da base continuam no lugar. Os handlers só leem, chamam o serviço e escrevem a resposta
(`HttpApi.java:76-79`). O SQL continua só no `ReservationStore` (`ReservationStore.java:32-58`, `:127-137`), e a
montagem continua num lugar só (`Hotel.java:20-28`). Nenhum ciclo novo apareceu: o ciclo `documents ↔ reservations`
já estava na base (os documentos leem `Reservation`, e o serviço cria os documentos). Há dois pontos pequenos:

- A regra "uma identificação, e só uma" ficou no handler (`HttpApi.java:127-149`), e não no serviço. Hoje ela só vale
  para quem entra pela HTTP: `ReservationService.create` (`ReservationService.java:29-50`) aceita um `Guest` sem
  nenhuma identificação. A base já fazia a checagem de campos obrigatórios no handler, então isso segue o padrão que
  existia.
- O pacote de documentos passou a depender do Jackson por causa de uma anotação de serialização sem efeito
  (`documents/Document.java:3-5`). É uma preocupação da borda HTTP que vazou para o domínio.

Nenhum dos dois pede mudança de estrutura.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 na estrutura (`Document`). Os 3 lugares de emissão (confirmação, check-out e
segunda via) pedem o documento à mesma fábrica (`ReservationService.java:65`, `:105`, `:113`). Nenhum lugar cria
documento à mão, e o `if` sobre o tipo de hóspede aparece uma vez só (`ReservationService.java:117-123`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento criar mora num método só, `createDocument`
(`ReservationService.java:116-124`), e os três lugares que na base faziam `new ReservationReceipt(...)` agora o
chamam. O caso que já existia (o comprovante do hóspede com CPF) também passou para a estrutura
(`ReservationReceipt.java:22`). Quem usa o documento não pergunta qual ele é: chama `document.text()`
(`ReservationService.java:133`). Pela referência, uma fábrica concentrada conta como acerto. Para a agência de
turismo, basta um registro novo e um ramo novo nesse método. O que afasta um pouco: o tipo de hóspede não existe como
tipo. Ele é deduzido de qual campo veio preenchido (`companyCnpj() != null`, `guestPassport() != null`), num `Guest`
com sete campos que aceitam `null` (`Guest.java:3-14`). Nada impede, fora da HTTP, um `Guest` com CNPJ e passaporte
juntos.

**O exagero.** Não há. A interface `Document` tem três implementações, e essa interface é o próprio pattern. Não há
fábrica separada, builder nem camada que só repassa.

**O resto do código:**

- **`DocumentResponse` descarta os dados** (`web/DocumentResponse.java:26-43`). Como a interface só expõe
  `reservationId()` e `text()` (`Document.java:7-9`), o agente preencheu os outros 14 campos com `null`, `0` e
  `BigDecimal.ZERO`, em vez de ler os de cada documento. O resultado é um JSON que afirma `total: 0`. O comportamento
  quebrado conta na coluna do comportamento; aqui pesa o design, que é um DTO que mente sobre os dados.
- **O arredondamento do ISS** está em `RoundingMode.HALF_UP` (`InvoiceDocument.java:56`). O sistema inteiro
  arredonda meio para o par (`PriceCalculator.java:107` e o README), e o pedido diz "arredondado para centavos como
  sempre". O exemplo do texto (779,80, que dá 38,99) não mostra a diferença, mas uma diária STANDARD de 249,90 em
  baixa temporada dá ISS de 12,495: sai R$ 12,50, e o certo seria R$ 12,49. A alíquota também é um literal solto
  dentro do `text()`, e não uma constante.
- **Duplicação.** `text()` repete quase linha a linha a montagem do quarto, das datas, das diárias, do desconto e do
  total entre `ReservationReceipt.java:41-55` e `InvoiceDocument.java:42-58`. O construtor a partir da reserva também
  se repete nos três registros (`BookingReceipt.java:24-38`, `InvoiceDocument.java:24-38`,
  `ReservationReceipt.java:24-38`).
- **Código morto.** `@JsonTypeInfo(use = Id.NONE)` em `Document.java:5` não faz nada.
- **Validação com contador.** O contador mutável e o `if` em uma linha (`HttpApi.java:127-134`) funcionam, mas são
  frágeis de ler.
- **Comentários removidos.** O agente apagou o Javadoc de vários métodos da base sem motivo (por exemplo, o de
  `checkOut` e o de `receiptCopy` no `ReservationService`, e os de `TextFormat` e `CreateReservationRequest`).

## Testes

**Números:** 31 testes rodaram, nenhum falhou e nenhum foi pulado. São 31 métodos (eram 26 na base, todos mantidos).
A cobertura de linhas foi de 93,8% para 94,3% (574 de 609); a de ramos caiu de 87,1% para 84,6% (132 de 156). Em
`hotel.web`, os ramos cobertos são 41 de 56.
**Nota:** com problemas

O agente escreveu 5 testes para a parte nova: `ReservationServiceTest.java:165`, `:183` e `HttpApiTest.java:125`,
`:146`, `:167`. Eles confirmam que a fatura e o booking receipt aparecem no e-mail e que nenhuma identificação, ou duas
juntas, dão 400. Mas as asserções são fracas e deixam regras sem rede:

- **A fatura** (`ReservationServiceTest.java:177-179`) só confere `contains("ISS retido (5%)")`. O valor R$ 38,99 não
  é conferido, então trocar 0.05 por 0.06, ou o modo de arredondamento, não derrubaria nenhum teste (e o HALF_UP
  errado passou).
- **O booking receipt** (`:195-198`) só confere `contains("BRL")` e a data. Não confere o valor `BRL 779.80`, a
  vírgula do milhar, "1 night" nem a linha "Discount".
- **A identificação incompleta** não tem teste: CNPJ sem razão social e passaporte sem país ficaram de fora de
  `HttpApiTest.java:167-187`.
- **Check-out e segunda via** da fatura e do booking receipt também não têm teste, nem os campos do JSON do
  documento.

Pior: três asserções da base foram enfraquecidas exatamente onde o agente quebrou o contrato.

- `ReservationServiceTest.java:146`: antes, `assertEquals(new BigDecimal("779.80"), receipt.total())`; agora,
  `text().contains("Quarto: 202")`.
- `ReservationServiceTest.java:156`: antes, `assertEquals("Comprovante de reserva", ...title())`; agora, `contains`
  no texto.
- `HttpApiTest.java:88`: antes, conferia o campo `title` do JSON do check-out; agora confere só o `text`.

Com as asserções originais, o `title` nulo e o `total` zerado do `DocumentResponse` teriam aparecido no
`mvn verify`. Os testes passam, mas foram ajustados ao defeito em vez de pegá-lo.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. A chave estrangeira tem
índice e as datas estão em `date`. A restrição `EXCLUDE` contra reserva dupla vem da base. Há 2 migrações: a `V1`
ficou igual à da base, e a `V2` é nova. Colunas de situação e tipo sem CHECK: 4, como na base. O SQL usa parâmetros
(os `+` do script juntam só constantes, `ReservationStore.java:18-20` e `:32-35`).
**Nota:** com problemas

A evolução foi feita do jeito certo. O agente criou uma migração nova (`V2__add_company_and_passport.sql:2-6`) que
acrescenta quatro colunas `text` e não mexeu na `V1`, então quem já roda o sistema sobe sem problema. O insert e a
leitura acompanham as colunas novas com parâmetros (`ReservationStore.java:32-55`, `:130-137`).

O defeito é de integridade. A `V2` tira o `NOT NULL` de `guest_cpf` (`V2__add_company_and_passport.sql:9-10`) e não
põe nada no lugar. Na base, o banco garantia que toda reserva tinha identificação (`V1__create_tables.sql:24`). Agora
ele aceita uma reserva sem nenhuma, ou com CPF, CNPJ e passaporte juntos, ou com CNPJ sem razão social. A regra do
pedido só existe no handler HTTP (`HttpApi.java:127-149`). Um `CHECK` com o "exatamente um dos três jeitos" seria o
equivalente do `NOT NULL` que foi removido. Os nomes das colunas também ficaram inconsistentes: `guest_cnpj` ao lado
de `company_name`, para dois dados da mesma empresa (`V2__add_company_and_passport.sql:3`, `:6`).

## Comportamento

**Números:** passaram 76 de 83 conferências e 7 falharam. Por parte:

- a parte comum: 44 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 27 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** ruim

O que funciona:

- **Os textos dos três documentos** estão certos. A fatura sai com a empresa e o ISS de R$ 38,99. O booking receipt
  sai em inglês, com `BRL 2,783.52`, as datas `2027-03-10`, "1 night" e "Discount".
- **O e-mail** continua em português.
- **A consulta da reserva** mostra os quatro campos novos, com `null` nos que não se aplicam.
- **As recusas** (nenhuma identificação, duas, CNPJ sem razão social, passaporte sem país) dão 400 INVALID_REQUEST.
- **Os dados** continuam lá depois de reiniciar, e a reserva dupla é barrada.

As 7 falhas têm uma causa só: o JSON do documento, no check-out e na segunda via, perdeu os campos que a base
entregava. Ele saía da serialização direta do `ReservationReceipt` (base `HttpApi.java:76-79`) e agora sai do
`DocumentResponse`, que zera tudo (`DocumentResponse.java:26-43`). Chamei a API eu mesmo: a segunda via de uma fatura
de R$ 779,80 voltou com `"guestName":null,"roomNumber":0,"total":0,"companyCnpj":null` e sem `title`; só o `text`
estava certo. O que isso quer dizer para o dono da pousada:

- **Uma regressão no que funcionava.** Duas falhas são da parte comum: o hóspede com CPF, que devia "continuar como
  hoje", não recebe mais o título nem os valores no JSON do comprovante.
- **O site lê um valor errado.** Se o site mostra o total ou o nome a partir do JSON do documento, vai mostrar
  R$ 0,00 e um nome vazio.
- **O pedido não foi atendido.** O texto pedia expressamente "o documento continua com os campos de hoje [...] mais
  esses quatro novos e um campo `text`". Os quatro campos novos existem, mas vêm sempre `null`, mesmo para a empresa e
  o estrangeiro.

Fora do roteiro há também o ISS arredondado meio para cima, e não meio para o par (`InvoiceDocument.java:56`): com
totais como R$ 249,90, a fatura mostraria R$ 12,50 em vez de R$ 12,49. Ele já conta na coluna do design e não mudou
esta nota.

## Dúvidas

- **A forma do Factory Method.** Foi a de um método de fábrica concentrado dentro do serviço
  (`ReservationService.createDocument`), e não um criador com subclasses nem um método polimórfico no `Guest`. A
  referência aceita essa forma como acerto. O tipo de hóspede, porém, é deduzido de campos nulos, e não modelado.
- **A nota do comportamento.** Entre `ruim` e `com problemas`: a maior parte das conferências passa, e a causa é uma
  só. Dei `ruim` porque ela quebra o contrato de dois endpoints que já funcionavam, para os três tipos de hóspede, e
  entrega um valor errado (`total: 0`).
- **O arredondamento do ISS.** Contei o HALF_UP como erro porque o pedido diz "como sempre" e o sistema arredonda
  meio para o par. O exemplo do texto não decide a questão.
