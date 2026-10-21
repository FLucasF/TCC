# Revisão: manutenção, cenário 05

## Visão geral

A base já tinha sete pacotes: `hotel` (a montagem), `hotel.web` (Javalin), `hotel.reservations` (o serviço, as regras e
o `ReservationStore` com JDBC), `hotel.documents`, `hotel.messages`, `hotel.reports` e `hotel.rooms`. Os dados ficam no
PostgreSQL, e o esquema é criado por migrações do Flyway. Uma requisição entra em `HttpApi`, que monta um
`NewReservation`. O `ReservationService` aplica as regras e grava pelo `ReservationStore`. O agente fez a mudança sem
mexer nessa forma. O passo novo é que os três lugares que antes faziam `new ReservationReceipt(...)` (a confirmação
com e-mail, o check-out e a segunda via) agora pedem o documento a uma `DocumentFactory`. É ela que escolhe, num lugar
só, entre três `DocumentWriter`: o comprovante, a fatura e o booking receipt. Para os novos campos, o agente criou uma
migração nova (V2) com um CHECK de "um jeito só" de identificar o hóspede. O sistema passa em tudo o que foi medido:
83 de 83 conferências, 32 de 32 testes e o esquema em ordem. Não há nada sério. Os defeitos que achei são de design
local: o texto dos documentos está duplicado entre a fatura e o comprovante, e o `Guest` virou um registro de campos
anuláveis cujo tipo é deduzido de `null`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, igual à base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O
`hotel.documents` passou de 2 classes e 79 linhas para 7 classes e 222 linhas; o `hotel.web` passou de 236 para 265
linhas.
**Nota:** bom

- **O ciclo veio da base.** O agente não acrescentou nenhuma dependência entre pacotes, e o grafo do jdeps é idêntico
  nas duas versões. O ciclo existe porque o serviço monta o documento e o documento lê a `Reservation`, como já era na
  base.
- **O pacote certo.** A parte nova ficou toda em `hotel.documents`, que não usa Javalin nem JDBC (tabela de
  bibliotecas em `measures/dependencies.md`). O serviço só troca o `new` por `documents.issue(...)`, em
  `src/main/java/hotel/reservations/ReservationService.java:66`, `:111` e `:120`.
- **O banco.** O acesso continua isolado no `ReservationStore`: as colunas novas entram em
  `src/main/java/hotel/reservations/ReservationStore.java:19`, `:51-54` e `:129-131`.
- **A borda HTTP.** A conferência de "exatamente uma identificação" ficou no handler
  (`src/main/java/hotel/web/HttpApi.java:126-134`), junto da conferência de campos obrigatórios que a base já fazia
  ali. É validação da forma do pedido, coerente com a base, e o banco a reforça com o CHECK da V2.
- **A montagem.** A `DocumentFactory` é criada dentro do serviço
  (`src/main/java/hotel/reservations/ReservationService.java:19`), e não no `Hotel`. Como ela não guarda estado nem
  depende de nada, não pesa.
- **O tamanho.** Nenhuma classe cresceu demais: a maior é `HttpApi`, com 184 linhas.

## Design de baixo nível

**Números:** os 3 tipos de documento estão na estrutura e nenhum ficou fora. Há 1 lugar que decide qual documento
criar (`DocumentFactory.writerFor`), e os 3 lugares de emissão passam por ele. Nenhum `if` sobre o tipo de hóspede
aparece fora da fábrica.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada: `src/main/java/hotel/documents/DocumentFactory.java:19-27` escolhe o
`DocumentWriter` pelo tipo de hóspede, e `issue` (`:14-17`) monta o `ReservationReceipt` com o título e o texto
daquele writer.

- O caso que já existia (a pessoa com CPF) também foi para a estrutura, como `PersonReceiptWriter`. O texto que antes
  morava em `ReservationReceipt.text()` saiu de lá.
- Os três lugares (`ReservationService.java:66`, `:111` e `:120`) pedem o documento à fábrica, e nenhum o cria à mão.
- Uma agência de turismo exige um writer novo e um ramo novo em `writerFor`, num lugar só.

A referência aceita essa forma como acerto.

**O exagero.** A interface `DocumentWriter` tem três implementações e é a própria peça do pattern. Não há builder,
camada de repasse nem interface com uma implementação só. Por isso, contenção.

**O resto do código.** Estes são os defeitos que um revisor pediria para corrigir:

- **Duplicação do texto.** `CompanyInvoiceWriter.java:23-32` repete, linha por linha, `PersonReceiptWriter.java:20-29`:
  o quarto, as datas, "diária/diárias", as diárias, o desconto e o total. Uma mudança no formato do comprovante (o que
  o próximo pedido pode trazer) precisa ser feita duas vezes. O agente não extraiu as linhas comuns.
- **Regra de arredondamento copiada.** O ISS é calculado com o literal `new BigDecimal("0.05")` e um
  `setScale(2, HALF_EVEN)` escrito à mão (`CompanyInvoiceWriter.java:33-34`). Ele repete a regra de
  `PriceCalculator.cents` (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`) em vez de usá-la, e a taxa
  não tem nome.
- **O tipo de hóspede deduzido de `null`.** `src/main/java/hotel/reservations/Guest.java:7-27` virou um registro com
  sete campos, quatro deles anuláveis. O tipo sai de `companyCnpj != null` e `passport != null` (`:20-26`), e a
  invariante "um jeito só" não está no `Guest`: ela está na borda HTTP (`HttpApi.java:126-134`) e no banco. Um `Guest`
  montado em outro lugar com CPF e CNPJ cai em silêncio na fatura, porque a ordem dos `if` em `DocumentFactory.java:20-25`
  decide. Um tipo selado ou um `enum` com a identificação deixaria isso explícito, e a agência anunciada vai
  acrescentar mais campos anuláveis no `Guest`, no `ReservationReceipt` e no `ReservationResponse`.

O resto segue o padrão da base:

- `BigDecimal` para dinheiro e `LocalDate` para datas;
- o booking receipt formata com `Locale.US` (`ForeignReceiptWriter.java:32-34`);
- `nullIfBlank` normaliza a entrada (`HttpApi.java:135-137`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 32 métodos escritos. A base tinha 26 testes,
todos mantidos, e o agente acrescentou 6: 5 em `DocumentsByGuestTest` e 1 em `HttpApiTest`. A cobertura de linhas
subiu de 93,8% para 94,5% (551 de 583), e a de ramos de 87,1% para 88,2% (134 de 152). Em `hotel.documents` são 86 de
87 linhas e 14 de 16 ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** bom

**Os valores da parte nova estão conferidos:**

- a fatura inteira, linha por linha, com o ISS de R$ 38,99
  (`src/test/java/hotel/documents/DocumentsByGuestTest.java:62-71`);
- a fatura no e-mail em português e no check-out (`:80-83`);
- o booking receipt inteiro (`:96-103`);
- o "1 night", a linha Discount e o milhar `BRL 2,783.52` (`:113-115`);
- os campos que vêm null em cada documento (`:60-61`, `:95` e `:126`);
- pela API, as cinco recusas com 400 INVALID_REQUEST (nenhuma identificação, CPF com CNPJ, CPF com passaporte, CNPJ
  sem razão social, passaporte sem país) e a criação com passaporte, com os campos null no JSON
  (`src/test/java/hotel/web/HttpApiTest.java:109-129`).

O agente não apagou nem enfraqueceu nenhum teste da base.

**A pergunta da mutação.** Trocar 0.05 por outra taxa, apagar a linha do ISS, trocar "nights" ou tirar a conferência
de "incompleto" faria algum teste falhar. Ficariam sem rede:

- trocar `HALF_EVEN` por `HALF_UP` no ISS, porque nenhum caso tem meio centavo;
- apagar a linha "Desconto" da fatura, porque nenhuma fatura com NON_REFUNDABLE é testada;
- o texto completo do comprovante da pessoa com CPF, que só é conferido por um `contains` (`:127`). A base também não
  conferia esse texto inteiro.

Também não há teste, pela API, do JSON do documento com o campo `text` nem da criação por empresa. São lacunas
pequenas diante do que está conferido.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice e
nenhuma data em texto. Há 2 migrações do Flyway: a V1 está igual à da base e a V2 é nova. As conferências só acusaram
as colunas de situação e de tipo em `text` sem CHECK (`status`, `rate_plan`, `type`, `channel`), que vêm da base
(`measures/database.md`).
**Nota:** bom

- **A migração nova.** O agente não editou a V1 e criou
  `src/main/resources/db/migration/V2__guest_identification.sql:2-11`, que faz três coisas: tira o `NOT NULL` de
  `guest_cpf`, acrescenta as quatro colunas e cria o CHECK `reservations_one_identification`. As reservas que já
  existem têm CPF e passam pelo CHECK, então quem já roda o sistema sobe sem perder dado.
- **SQL com parâmetros.** O INSERT ganhou quatro parâmetros `?`
  (`src/main/java/hotel/reservations/ReservationStore.java:32-34` e `:51-54`). Os `+` que o script lista só juntam
  constantes.
- **Reservas simultâneas.** A proteção contra duas reservas na mesma noite continua sendo o `EXCLUDE` da base.
- **Um detalhe do CHECK.** Ele conta a empresa e o passaporte só quando os dois campos de cada par estão preenchidos.
  Uma linha com CPF e só o `company_cnpj` passaria no banco. A borda HTTP recusa esse caso (`HttpApi.java:129-131`),
  então o efeito é só que o banco protege um pouco menos que a aplicação.

## Comportamento

**Números:** 83 de 83 conferências passaram e nenhuma falhou (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada:

- **A pessoa com CPF** recebe o mesmo comprovante de antes, com o mesmo texto.
- **A empresa** recebe a fatura, com a razão social e o CNPJ no lugar do CPF e o ISS retido de R$ 38,99 sobre
  R$ 779,80. A fatura vai no e-mail, que continua em português, e volta no check-out.
- **O estrangeiro** recebe o booking receipt em inglês, com `BRL 779.80`, as datas `2027-03-10`, "1 night", a linha
  Discount e o milhar com vírgula.
- **As recusas.** Uma reserva sem identificação, com CPF e CNPJ juntos, com CNPJ sem razão social ou com passaporte
  sem país é recusada com 400 INVALID_REQUEST.
- **O que já existia.** Tudo continua funcionando: os preços, os cancelamentos, os relatórios, os dados depois de
  reiniciar e a proteção contra duas reservas do mesmo quarto ao mesmo tempo.

## Dúvidas

- **A forma do pattern.** O agente usou uma fábrica concentrada: uma classe com um método privado que escolhe o
  writer por `if`. Não usou a forma clássica (um criador com o método de fábrica sobrescrito) nem um método
  polimórfico no `Guest`. A referência aceita essa forma como acerto.
- **O arredondamento do ISS.** O pedido diz "arredondado para centavos como sempre", e o agente usou meio para o par
  (`HALF_EVEN`), como o resto do sistema faz. Nenhum teste nem conferência do roteiro tem um caso de meio centavo, por
  isso não sei se o dono esperaria outro arredondamento num total como R$ 100,10, em que o ISS é R$ 5,005.
- **O README.** Ficou igual ao da base e não descreve os três tipos de hóspede. O pedido não pedia para atualizá-lo,
  então não pesei isso em nenhuma coluna.
