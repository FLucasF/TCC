# Revisão: manutenção, cenário 05

## Visão geral

O projeto é o da base, em sete pacotes: `hotel.web` (as rotas do Javalin em `HttpApi`, os DTOs), `hotel.reservations`
(o `ReservationService` com as regras, o `ReservationStore` com o JDBC, o preço), `hotel.rooms`, `hotel.documents`
(o comprovante), `hotel.messages` (os e-mails), `hotel.reports` e `hotel` (a montagem e o Flyway). Uma requisição entra
em `HttpApi`, que valida o corpo e chama o `ReservationService`, que usa o `ReservationStore` para gravar no
PostgreSQL (migrações do Flyway). Para o pedido, o agente acrescentou ao `Guest` os campos da empresa e do passaporte
(`reservations/Guest.java:7-26`), uma migração nova (`V2__guest_identification.sql`), a validação de "um jeito e só um"
em `web/HttpApi.java:126-134`, e os textos dos três documentos em `documents/DocumentTexts.java`. O sistema faz tudo o
que o dono pediu (83 de 83 conferências do roteiro passam) e `mvn verify` passa. O mais sério é de design: a escolha de
qual documento criar ficou num `if/else` sobre o tipo de hóspede **copiado nos três lugares** em que o documento sai
(`reservations/ReservationService.java:67-76`, `:123-132`, `:144-153`), justamente o ponto que o pedido avisa que vai
crescer (as agências de turismo).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); na base, os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. Maiores classes: `HttpApi` 184 linhas (base 170),
`ReservationService` 174 (base 138), `ReservationStore` 150 (base 143).
**Nota:** bom

O agente não criou dependência nem ciclo novo: o ciclo `documents` ↔ `reservations` já existia na base (o
`ReservationReceipt` lê a `Reservation`, e o serviço cria o recibo); o `DocumentTexts` novo só reforça a mesma aresta
(`documents/DocumentTexts.java:3-4`, `reservations/ReservationService.java:3`). As camadas continuam no lugar: a
validação do corpo fica na borda HTTP, como já ficava a dos outros campos (`web/HttpApi.java:116-134`), as regras não
conhecem o Javalin, e o SQL das colunas novas fica só no `ReservationStore`
(`reservations/ReservationStore.java:18-20`, `:32-54`, `:129-131`). A montagem não mudou. O crescimento de 36 linhas
do `ReservationService` vem da cópia do `if/else` (ver o design), não de uma responsabilidade nova no lugar errado.

## Design de baixo nível

**Números:** 3 tipos de documento; a decisão de qual criar aparece 3 vezes (uma em cada lugar de onde o documento
sai), e 0 vezes numa fábrica ou num método do hóspede. Os três textos ficam em 3 funções estáticas, das quais duas
repetem 6 das suas linhas.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve três documentos que saem nos mesmos três lugares (e-mail de confirmação, check-out e
segunda via) e avisa que vem um quarto (as agências). A referência pede que a escolha de qual documento criar more num
lugar só. Aqui ela está num `if (isCompany) ... else if (isForeigner) ... else` repetido em `confirm`
(`reservations/ReservationService.java:66-76`), em `checkOut` (`:122-133`) e em `receiptCopy` (`:143-154`), cada cópia
com os mesmos títulos em texto solto e a mesma chamada a `DocumentTexts`. Separar os textos em funções
(`documents/DocumentTexts.java:15`, `:33`, `:55`) não muda a classe: a referência diz expressamente que, com o `if`
repetido nos três lugares, é omissão mesmo que o texto de cada documento esteja em funções auxiliares. Para a agência
de turismo, alguém vai ter que achar e mudar os três `if`s, mais o `Guest`, mais o `ReservationReceipt`; esquecer um
deles faz o documento errado sair num dos lugares. Um método privado `documentFor(reservation, room)` no serviço já
teria concentrado a escolha.

**O exagero.** Nada de estrutura sem uso: não há interface, fábrica nem camada nova; o `DocumentTexts` é uma classe de
funções estáticas, e o construtor de compatibilidade `Guest(name, email, cpf)` (`reservations/Guest.java:16-18`) é
usado pelos testes da base. Contenção.

**O resto do código.**
- Duplicação: além do `if/else` triplicado, `personReceipt` e `companyInvoice` repetem o mesmo bloco de quarto, datas,
  diárias, desconto e total (`documents/DocumentTexts.java:20-29` e `:39-48`); uma mudança no comprovante (um campo a
  mais) precisa ser feita duas vezes.
- O tipo do hóspede é deduzido de quais campos nulos estão preenchidos (`reservations/Guest.java:20-26`), num record de
  sete `String`s em que só alguns valem por vez; o mesmo acontece no `ReservationReceipt`, que ganhou quatro campos
  quase sempre nulos e recebe o título como texto livre (`documents/ReservationReceipt.java:13-28`,
  `reservations/ReservationService.java:68`). Um tipo para a identificação (um `enum` ou um tipo selado) daria ao
  compilador a garantia que hoje só está na validação HTTP e na `CHECK` do banco.
- O que está certo: o ISS em `BigDecimal` com 2 casas e o mesmo arredondamento meio-para-o-par do resto do sistema
  (`documents/DocumentTexts.java:49-50`); o dinheiro em inglês com `Locale.US` (`:74-76`); as datas `LocalDate`; a
  validação em `web/HttpApi.java:126-134` cobre nenhum, dois e o incompleto, e normaliza branco para `null`
  (`:135-137`, `:181-183`).

Defeitos localizados (a duplicação e os campos nulos), que não impedem aceitar: `com problemas`.

## Testes

**Números:** final: 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos. Linhas 553 de 591 (93,6%),
ramos 130 de 160 (81,2%). Base: 26 testes, 0 falhas; linhas 93,8%, ramos 87,1%. No pacote `hotel.web`, os ramos caíram
de 18/28 para 29/48 (os ramos novos da validação ficaram quase todos sem teste); em `hotel.documents`, 8 de 12.
**Nota:** com problemas

O agente manteve os 26 testes da base sem mudar nenhum (o `diff` de `src/test` só mostra a pasta nova) e escreveu 3
testes em `src/test/java/hotel/documents/DocumentsByGuestTest.java`, contra o banco e pelo serviço, com valores do
pedido: a fatura com a empresa e o ISS de R$ 38,99 (`:47-49`) e o booking receipt com as datas, "2 nights" e o total
em BRL (`:59-61`). Trocar 0.05 por outra taxa, ou o formato da data em inglês, quebraria um teste.

Ficaram sem rede, pela pergunta da mutação:
- a regra de "um jeito e só um" (`web/HttpApi.java:126-134`): nenhum teste manda nenhuma identificação, duas juntas,
  CNPJ sem razão social ou passaporte sem país; apagar `companyIncomplete`/`passportIncomplete` não quebra nada (é por
  isso que os ramos de `hotel.web` caíram para 29/48);
- o "1 night" e a linha "Discount:" do booking receipt (`documents/DocumentTexts.java:62-69`), e o milhar com vírgula
  (o teste usa 779.80, sem milhar);
- o documento nos três lugares: os testes novos só olham a segunda via (`receiptCopy`, `DocumentsByGuestTest.java:45`,
  `:57`); o e-mail com a fatura e o check-out com a fatura não são conferidos, e são justamente as três cópias do
  `if/else`;
- o texto do comprovante da pessoa com CPF: o teste novo só olha o título (`:68`), e o texto do comprovante foi
  reescrito (saiu de `ReservationReceipt.text()` e foi para `DocumentTexts.personReceipt`) sem nenhum teste conferir
  que ele continua igual ao da base;
- os campos novos no JSON da reserva e do documento (nada pela API).

Os testes passam e conferem com valores o centro da parte nova (a fatura e o ISS), mas deixam a validação, os três
lugares e o comprovante de hoje sem conferir.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` (6 colunas, todas ok); chave
estrangeira com índice; nenhuma data em texto; 2 migrações (V1 igual à da base, V2 nova). Restrições: a `EXCLUDE` da
base contra reserva dupla e a `CHECK` nova de uma identificação só. As colunas de situação sem `CHECK` vêm da base.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem tocar na V1
(`src/main/resources/db/migration/V2__guest_identification.sql:2-11`), que solta o `NOT NULL` do CPF, acrescenta as
quatro colunas e protege a regra de "exatamente um jeito" no próprio banco com uma `CHECK` (`:8-11`). As reservas que
já existem continuam válidas (todas têm CPF). O SQL continua com parâmetros (`reservations/ReservationStore.java:32-54`;
os `+` que o script lista só juntam constantes). Detalhe pequeno: a `CHECK` aceita uma linha com CPF e um CNPJ sem
razão social (o par incompleto conta 0), coisa que a validação HTTP impede, mas o banco não; não perde nem corrompe
dado do que o sistema grava hoje. As colunas de situação sem `CHECK` (`status`, `rate_plan`) já eram assim na base.

## Comportamento

**Números:** 83 de 83 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 05
32 de 32, as reservas simultâneas 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API: a pessoa com CPF continua com o comprovante de hoje (título e texto); a
empresa recebe a fatura com a empresa no lugar do CPF e o ISS retido de R$ 38,99, no e-mail (em português), no
check-out e na segunda via; o estrangeiro recebe o booking receipt com as datas 2027-03-10, "1 night", a linha
"Discount:" e o milhar com vírgula (BRL 2,783.52); a consulta e o documento trazem os campos novos, com `null` nos que
não se aplicam, e o campo `text`; e as quatro recusas (nenhuma identificação, CPF com CNPJ, CNPJ sem razão social,
passaporte sem país) dão 400 INVALID_REQUEST. Nada da parte comum quebrou, os dados sobrevivem ao reinício e o quarto
não é reservado duas vezes.

## Dúvidas

- A classe do pattern não deixou dúvida: o `if/else` sobre o tipo de hóspede está nos três lugares
  (`reservations/ReservationService.java:67`, `:123`, `:144`), o caso que a referência chama de omissão; não há fábrica
  nem método polimórfico no hóspede.
- A nota do design ficou em `com problemas` e não em `ruim` porque a duplicação está concentrada num serviço e numa
  classe de textos, curtos e fáceis de achar; um revisor mais rígido poderia pesar mais a cópia do `if/else`, porque
  ela é o ponto exato que o próximo pedido (as agências) vai mudar.
