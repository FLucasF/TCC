# Revisão: manutenção, cenário 05

## Visão geral

O projeto (herdado da base) tem sete pacotes: `hotel.web` (o Javalin, `HttpApi`, e os registros de entrada e saída
em JSON), `hotel.reservations` (as regras em `ReservationService`, o preço, e o JDBC em `ReservationStore`),
`hotel.documents` (os documentos do hóspede), `hotel.rooms`, `hotel.messages`, `hotel.reports` e `hotel` (a
montagem). Uma requisição entra em `HttpApi`, que valida o corpo e monta o `Guest` (`web/HttpApi.java:112-155`). Daí
vai para o `ReservationService`, que grava pelo `ReservationStore` num PostgreSQL com o esquema criado pelo Flyway.
Para atender o pedido, o agente transformou o `Guest` numa interface selada com três registros (`IndividualGuest`,
`CorporateGuest` e `ForeignGuest`) e o `ReservationReceipt` numa interface selada `ReservationDocument` com três
documentos. A escolha do documento ficou num método só, `ReservationDocument.from`, que os três lugares chamam. Também
acrescentou a migração `V2` e seis testes. O defeito mais sério é que o campo `title` sumiu do JSON do documento (no
check-out e na segunda via), embora o pedido diga que "o documento continua com os campos de hoje". Seis conferências
do roteiro falham só por isso. Para não acusar a falta, o agente enfraqueceu os dois testes da base que conferiam esse
campo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos números da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.documents` passou de 2 para 5 classes (de 79 para 310 linhas) e agora usa Jackson. A maior classe é `HttpApi`,
que foi de 170 para 195 linhas.
**Nota:** bom

As camadas continuam como na base. A borda HTTP lê o corpo, monta o `Guest` e chama o serviço
(`web/HttpApi.java:67-70` e `:135-155`). A validação de "exatamente uma identificação" ficou junto da validação dos
outros campos, que a base já fazia na borda (`web/HttpApi.java:119-128`). As regras continuam sem Javalin e sem SQL
(`reservations/ReservationService.java:64`, `:109` e `:118` só pedem o documento à fábrica). O SQL novo ficou isolado
no `ReservationStore` (`reservations/ReservationStore.java:41-56` e `:145-161`), e a montagem não mudou. O ciclo
`documents` ↔ `reservations` veio da base; o agente não o piorou, só acrescentou mais setas no mesmo sentido (os
documentos leem `CorporateGuest`, `ForeignGuest`, `IndividualGuest` e `PriceCalculator.cents`, que precisou virar
`public` em `reservations/PriceCalculator.java:50`). O único deslize é pequeno: uma anotação de serialização do Jackson
(`@JsonProperty`) entrou no pacote de documentos (`documents/ReservationDocument.java:3` e `:17`), que antes não
conhecia JSON. Ela não mistura Javalin nem SQL nas regras, e por isso não muda a nota.

## Design de baixo nível

**Números:** os 3 tipos de documento ficam na estrutura (`IndividualReceipt`, `CorporateInvoice` e `ForeignReceipt`),
e nenhum fica fora dela. Os 3 lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via)
pedem todos à mesma fábrica, e nenhum cria o documento à mão. Fora da fábrica, a pergunta "qual é o tipo de hóspede?"
se repete em 3 lugares (a gravação, a leitura e a resposta da reserva) para tratar os campos de identificação.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A criação está concentrada num método de fábrica estático, `ReservationDocument.from`
(`documents/ReservationDocument.java:37-44`). Ele faz um `switch` sobre o `Guest` selado e devolve o documento de cada
tipo. Os três lugares só chamam esse método (`reservations/ReservationService.java:64`, `:109` e `:118`). O e-mail
recebe o documento pronto e só usa `document.text()` (`ReservationService.java:125-130`). O caso que já existia (o
comprovante do CPF) também entrou na estrutura (`documents/IndividualReceipt.java`), e o texto dele é igual ao da
base. Um documento para as agências de turismo exige um registro novo e um `case` novo nesse único `switch`. O
compilador obriga o `case`, porque `Guest` e `ReservationDocument` são selados. É a "fábrica concentrada" que a
referência aceita como acerto.

**O exagero.** Não há estrutura sem necessidade. As duas interfaces seladas têm três implementações cada, e não
apareceram builder, camada de repasse nem fábrica a mais.

**O resto do código.** Há defeitos localizados:
- **O campo `title`.** O agente apagou o componente `title` do registro do comprovante (era `ReservationReceipt.java:11`
  na base) e passou o título para dentro do texto (`documents/IndividualReceipt.java:62`). Com isso, o JSON do documento
  perdeu um campo que o site usava.
- **Duplicação.** O texto da fatura repete quase linha a linha o do comprovante: o quarto, as datas, as diárias, o
  desconto e o total (`documents/CorporateInvoice.java:62-78` e `documents/IndividualReceipt.java:60-74`). Os três
  registros repetem os mesmos dez componentes e o mesmo construtor a partir de `Reservation` e `Room`.
- **A interface larga.** `ReservationDocument` declara os campos de identificação de todos os tipos
  (`documents/ReservationDocument.java:23-27`). Por isso cada documento implementa quatro métodos que devolvem `null`
  (por exemplo, `documents/IndividualReceipt.java:39-57`), e um documento de agência obrigaria a mexer nos três.
- **A identificação fora da estrutura.** Ela é tratada por cadeias de `instanceof` em três lugares:
  `reservations/ReservationStore.java:43-56` (na gravação), `:154-161` (na leitura) e
  `web/ReservationResponse.java:53-71`. A cadeia da gravação não tem ramo final, e um quarto tipo de hóspede gravaria
  todos os campos de identificação sem valor. A leitura cai em `ForeignGuest` sempre que o CPF e o CNPJ vêm nulos,
  mesmo que o passaporte também venha nulo (`ReservationStore.java:159-161`).
- **Imports com curinga.** Os novos imports usam `import hotel.reservations.*` (`documents/ReservationDocument.java:4`
  e `web/ReservationResponse.java:3`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 32 métodos de teste no código. A base tinha 26,
e todos continuam lá. A cobertura é de 93,3% das linhas (589 de 631) e 88,6% dos ramos (148 de 167). Na base, era de
93,8% e 87,1%. `hotel.documents` tem 97 de 109 linhas e 12 de 15 ramos cobertos; `hotel.web`, 48 de 58 ramos
(`measures/tests.md`).
**Nota:** com problemas

O agente acrescentou seis testes pela API (`web/HttpApiTest.java:125-229`). Eles conferem com valor o ISS da fatura
("ISS retido (5%): R$ 38,99", `:148`) e a linha da empresa (`:146`). Também conferem a data ISO e o "BRL 779.80" do
booking receipt (`:174-177`), os campos `companyCnpj` e `guestPassport` na consulta, e três recusas: nenhuma
identificação, duas identificações e o CNPJ sem a razão social (`:181-213`). Os defeitos:
- **Dois testes da base enfraquecidos.** `HttpApiTest.java:88` conferia `title` igual a "Comprovante de reserva" e
  agora só procura essa frase dentro de `text`; `ReservationServiceTest.java:155` mudou do mesmo jeito. Foi isso que
  escondeu a perda do campo `title`, que o roteiro do comportamento acusa.
- **Mutações que nenhum teste pega:**
  - O teste do singular confere `text.contains("1 night")` (`HttpApiTest.java:228`), e "1 nights" também contém "1
    night". Se o texto saísse sempre no plural, nenhum teste falharia.
  - O separador de milhar do booking receipt (`ForeignReceipt.java:81`) não é testado, porque os 779,80 do teste não
    têm milhar.
  - A linha "Discount:" do booking receipt não é testada.
  - Nenhum teste confere que o e-mail de confirmação leva a fatura ou o booking receipt.
  - Nenhum teste confere o passaporte sem o país.
- **Nível dos testes.** Não há teste de unidade dos documentos: tudo passa pela API, com asserções `contains` sobre
  pedaços do texto.

## Banco

**Números:** 3 tabelas, todas com chave primária e com o dinheiro em `numeric(10,2)`. A `V1` ficou igual à da base, e
a `V2` é nova. Nenhuma chave estrangeira sem índice, nenhuma data em texto. As restrições da base continuam,
inclusive o `EXCLUDE` contra duas reservas na mesma noite. `reservations` tem 20 colunas e 14 `NOT NULL`. O SQL usa
parâmetros: os trechos montados com `+` só juntam constantes (`measures/database.md`).
**Nota:** com problemas

O agente fez a mudança do esquema do jeito certo, numa migração nova (`V2__add_guest_identification_fields.sql`), sem
mexer na `V1`. O SQL novo usa parâmetros (`reservations/ReservationStore.java:32-56`). O defeito é a integridade: a
`V2` tira o `NOT NULL` de `guest_cpf` (`V2__add_guest_identification_fields.sql:10`) e não põe nada no lugar. O
comentário da linha 2 diz "Exactly one set of identification fields must be present per reservation", mas não há
`CHECK` que garanta isso: o banco aceita uma reserva sem nenhuma identificação, ou com duas. Uma linha assim é lida em
silêncio como estrangeiro com passaporte nulo (`reservations/ReservationStore.java:159-161`). Hoje só a validação da
API protege a regra. As colunas de situação (`status`, `rate_plan`) seguem sem `CHECK`, como já eram na base.

## Comportamento

**Números:** 77 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 44 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 28 de 32;
- reservas simultâneas: 2 de 2.
**Nota:** com problemas

As seis falhas têm a mesma causa: o campo `title` do documento vem `null`. São elas:
- a segunda via e o check-out da parte comum;
- o documento com CPF, a fatura, o check-out da empresa e o booking receipt, no cenário.

O agente removeu esse campo (veja o design), embora o pedido diga que o documento continua "com os campos de hoje,
mais esses quatro novos e um campo `text`". Para o dono da pousada, o site que mostrava o título do documento a partir
desse campo deixa de mostrá-lo, até para quem tem CPF, que não devia notar mudança nenhuma. O conteúdo está certo:
- A fatura traz a empresa no lugar do CPF e o ISS de R$ 38,99.
- O booking receipt sai em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount".
- O e-mail continua em português e leva o documento certo.
- As quatro recusas (nenhuma identificação, CPF com CNPJ, CNPJ sem razão social, passaporte sem país) respondem 400
  `INVALID_REQUEST`.
- Os dados resistem ao reinício, e as reservas simultâneas são barradas.

## Dúvidas

- A forma do pattern é um método de fábrica estático com `switch` sobre a interface selada `Guest`
  (`documents/ReservationDocument.java:37-44`), e não a forma clássica com criadores sobrescritos nem um
  `guest.issueDocument(...)`. A referência aceita essa forma como acerto.
- Não testei um caso que a leitura do código sugere: um pedido com CPF e só o CNPJ (sem a razão social) seria aceito
  como pessoa com CPF, e o CNPJ seria ignorado em silêncio (`web/HttpApi.java:136-140`). O texto diz "tem que ser um
  desses jeitos, e só um", o que pode ser lido como pedido de recusa.
