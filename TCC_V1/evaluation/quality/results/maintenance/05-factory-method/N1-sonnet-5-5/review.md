# Revisão: manutenção, cenário 05

## Visão geral

O projeto mantém a forma da base: `hotel.web` (Javalin, `HttpApi` lê o corpo e chama o serviço), `hotel.reservations`
(o `ReservationService` com as regras e o `ReservationStore` com o JDBC), `hotel.documents` (o documento), `hotel.rooms`,
`hotel.messages`, `hotel.reports` e a montagem em `hotel`. Os dados ficam no PostgreSQL, com o esquema em migrações do
Flyway. O agente trocou o `ReservationReceipt` da base por um `ReservationDocument` cuja montagem passa por uma fábrica
só (`DocumentLayout.of`, `src/main/java/hotel/documents/DocumentLayout.java:39-45`), com um layout por tipo de hóspede
e o esqueleto do texto escrito uma vez só. A identificação virou um tipo selado (`Identification`, com três records),
validado num lugar só, e as quatro colunas novas entraram por uma migração nova (`V2__guest_identification.sql`). As
83 conferências do roteiro passaram, e os 35 testes também. O mais sério que achei é pequeno: o banco perdeu a garantia
de que toda reserva tem identificação (o `NOT NULL` do CPF saiu e nenhum `CHECK` entrou no lugar), e os testes não
conferem o separador de milhar do booking receipt.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da
base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.documents` passou de 2 para 7
classes (79 → 253 linhas); `hotel.reservations` de 13 para 17 classes. Javalin só em `hotel` e `hotel.web`; JDBC só em
`hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`.
**Nota:** bom

A mudança respeitou as camadas. A borda HTTP continua fina: `HttpApi` só lê os campos novos e passa para
`Identification.of` (`src/main/java/hotel/web/HttpApi.java:127-129`); a regra de "um jeito só" fica no domínio
(`src/main/java/hotel/reservations/Identification.java:39-59`), sem Javalin e sem SQL. O SQL das colunas novas ficou no
`ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-56` e `:130-137`). Os documentos não
conhecem nem o Javalin nem o JDBC; a única dependência nova dentro do ciclo é o `InvoiceLayout` usar
`PriceCalculator.cents` (`src/main/java/hotel/documents/InvoiceLayout.java:30`), que o agente tornou público
(`src/main/java/hotel/reservations/PriceCalculator.java:50`) para reusar o arredondamento em vez de copiá-lo. O ciclo
`documents` ↔ `reservations` já existia na base (o serviço cria o documento e o documento lê a reserva) e o agente não o
piorou: o número de dependências não mudou. Nenhuma classe cresceu demais (a maior continua sendo o `HttpApi`, 170 →
172 linhas).

## Design de baixo nível

**Números:** 3 de 3 tipos de documento dentro da estrutura (o comprovante da base, a fatura e o booking receipt); a
escolha de qual criar está em 1 lugar (`DocumentLayout.java:39-45`); os 3 lugares que emitem o documento
(`ReservationService.java:64`, `:109`, `:118`) pedem por `new ReservationDocument(reservation, room)`, que passa pela
fábrica (`ReservationDocument.java:32-34`); 0 lugares escolhendo o documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido. Na base, o `ReservationReceipt` era montado com `new` em três lugares do `ReservationService`. Agora
os três lugares continuam chamando um construtor só, e é ele que pede o layout à fábrica:
`ReservationDocument(Reservation, Room)` chama `DocumentLayout.of(reservation.guest().identification())`
(`src/main/java/hotel/documents/ReservationDocument.java:32-34`), um `switch` sobre o tipo selado que devolve
`ReceiptLayout`, `InvoiceLayout` ou `BookingReceiptLayout` (`src/main/java/hotel/documents/DocumentLayout.java:39-45`).
O caso que já existia (o CPF) também entrou na estrutura (`src/main/java/hotel/documents/ReceiptLayout.java`), e o texto
dele continua igual ao da base (o roteiro confere). Como `Identification` é `sealed`
(`src/main/java/hotel/reservations/Identification.java:7`), a agência de turismo vai ser um record novo mais um layout
novo, e o compilador recusa o `switch` até ele ter o caso novo. Isso deixa um só lugar para mexer, e esse lugar fica
visível. Cada layout dá só o que difere (título, linhas do hóspede, rodapé, idioma), e a sequência das linhas está
escrita uma vez em `DocumentLayout.text` (`DocumentLayout.java:64-82`). Assim a fatura e o booking receipt não copiam o
comprovante. O ISS mora no layout da fatura (`InvoiceLayout.java:29-32`), com o arredondamento da base.

O exagero: nenhum. O record `Wording` (`DocumentLayout.java:17-36`) tem duas instâncias de verdade (português e
inglês), usadas por três layouts. Não há interface com uma implementação só, nem builder, nem camada que só repassa.

O resto do código está bom e tem só detalhes:
- As cinco leituras padrão que devolvem `null` em `Identification` (`Identification.java:9-27`) tiram parte da força do
  tipo selado (`ReceiptLayout` chama `identification().cpf()` sem saber, pelo tipo, que é um `PersonalId`). Em troca,
  elas simplificam o JSON, que pede `null` nos campos que não se aplicam.
- `ReservationStore.read` usa o mesmo `Identification.of` da validação HTTP para remontar a reserva
  (`ReservationStore.java:131-136`). Uma linha inconsistente no banco viraria um `INVALID_REQUEST` (400) numa consulta.
- `reservation.guest().identification()` se repete cinco vezes (`ReservationDocument.java:41-45`), e falta um espaço
  em `setBigDecimal(18,` (`ReservationStore.java:56`).

Nada disso é algo que eu pediria para mudar antes de aceitar. Os tipos estão certos (`BigDecimal` com `cents`,
`LocalDate`, records).

## Testes

**Números:** 35 testes rodaram, 0 falhas, 0 erros, 0 pulados; 35 métodos escritos (na base: 26, todos mantidos). Linhas
94,9% (558/588) e ramos 90,7% (137/151); na base, 93,8% e 87,1%. `hotel.documents` está com 76/76 linhas e 7/7 ramos.
**Nota:** bom

O agente acrescentou 9 testes, que conferem a parte nova com valores:
- a fatura com o texto inteiro, incluindo `ISS retido (5%): R$ 38,99`, e o e-mail levando esse texto
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:170-193`);
- o booking receipt com o texto inteiro, as datas ISO e `BRL 779.80` (`ReservationServiceTest.java:195-214`);
- "1 night" e a linha `Discount` (`ReservationServiceTest.java:217-227`);
- a identificação que volta igual do banco (`ReservationServiceTest.java:230-235`);
- os três jeitos aceitos e as recusas (nenhum, dois, um incompleto) em teste de unidade
  (`src/test/java/hotel/reservations/IdentificationTest.java:16-39`) e pela API, com 400 e `INVALID_REQUEST`
  (`src/test/java/hotel/web/HttpApiTest.java:149-164`);
- os campos `null` e o `text` no JSON (`HttpApiTest.java:123-146`).

Os testes da base continuam lá, adaptados só no tipo (`ReservationReceipt` → `ReservationDocument`) e no nome do método
(`receiptCopy` → `documentCopy`), sem perder asserção.

A pergunta da mutação: trocar 0.05 por outro valor, apagar o rodapé ou trocar "night" por "nights" quebra um teste.
Ficam sem rede:
- o separador de milhar do booking receipt: trocar `%,.2f` por `%.2f` em `src/main/java/hotel/documents/TextFormat.java:26`
  passa em todos os testes, porque nenhum valor testado chega a 1.000;
- o texto inteiro do comprovante com CPF depois que ele foi reescrito: os testes da base conferem só pedaços
  (`ReservationServiceTest.java:100-101`) e o título.

O roteiro do comportamento confere os dois, e os dois estão certos. São lacunas pequenas perto do que está coberto.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; a chave estrangeira com índice; datas
em `date`; a reserva dupla impedida por `EXCLUDE USING gist`. Migrações: `V1__create_tables.sql` igual à da base e
`V2__guest_identification.sql` nova. O script listou 4 lugares com SQL montado com `+`, todos com constantes. As
colunas `status`, `rate_plan`, `type` e `channel` estão sem `CHECK`, como na base.
**Nota:** com problemas

A evolução foi feita do jeito certo: uma migração nova e só aditiva
(`src/main/resources/db/migration/V2__guest_identification.sql:2-7`), sem tocar na V1, o que mantém de pé um banco que
já roda. As linhas antigas, todas com CPF, continuam sendo lidas como `PersonalId`. O SQL usa parâmetros e
try-with-resources (`src/main/java/hotel/reservations/ReservationStore.java:32-56`).

O defeito: a migração tira o `NOT NULL` de `guest_cpf` (`V2__guest_identification.sql:2`) e não põe nada no lugar.
Na base, o banco garantia que toda reserva tinha identificação; agora ele aceita uma linha sem nenhuma, ou com CPF e
CNPJ juntos, ou com CNPJ sem razão social. A regra "um jeito só, e completo" ficou só no Java. Faltou um `CHECK`, por
exemplo `num_nonnulls(guest_cpf, company_cnpj, guest_passport) = 1` mais os pares completos. Isso pesa mais porque a
leitura passa a falhar numa linha assim: `Identification.of` lança `INVALID_REQUEST` dentro de
`ReservationStore.read` (`ReservationStore.java:131-136`), e uma única linha ruim derruba a consulta, a listagem e os
relatórios que leem todas as reservas. Pela API isso não acontece hoje, então não é um dado errado criado pelo
sistema, mas é uma garantia do banco que se perdeu. A falta de `CHECK` em `status` e `rate_plan` vem da base.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam. A parte comum: 46/46. Os dados depois de reiniciar: 3/3. O
cenário 05: 32/32. Duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- quem tem CPF continua recebendo o comprovante de antes, com o mesmo texto;
- a empresa recebe a fatura com a razão social, o CNPJ e o ISS retido de R$ 38,99 sobre R$ 779,80, no e-mail (que
  continua em português) e no check-out;
- o estrangeiro recebe o booking receipt em inglês, com as datas `2027-03-10`, `BRL 2,783.52` com o separador de
  milhar, "1 night" e a linha `Discount`;
- as combinações erradas (nenhuma identificação, CPF e CNPJ juntos, CNPJ sem razão social, passaporte sem país) dão 400
  `INVALID_REQUEST`;
- a consulta da reserva e o documento trazem os campos novos, com `null` nos que não se aplicam, e o documento traz o
  `text`.

Para o dono da pousada, a mudança está pronta para usar, e nada do que já funcionava quebrou. A falta de `CHECK` no
banco, contada na coluna do banco, não aparece em nenhuma chamada do roteiro.

## Dúvidas

- A forma do Factory Method usada é a fábrica concentrada: um método estático, `DocumentLayout.of`, com um `switch` sobre
  o tipo selado (`DocumentLayout.java:39-45`). Não é o criador clássico com o método sobrescrito, nem um método
  polimórfico no tipo de hóspede. A referência aceita a fábrica concentrada como acerto. O `switch` aparece num lugar
  só, e o compilador obriga a tratar um tipo novo.
- Na nota do banco, `com problemas` vem de uma garantia que o sistema perdeu (o `NOT NULL` do CPF), não de um dado
  errado que se possa ver pela API. Um revisor menos exigente poderia dar `bom`.
