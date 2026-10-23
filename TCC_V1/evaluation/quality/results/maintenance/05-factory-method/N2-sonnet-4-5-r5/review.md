# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base (pacotes `web`, `reservations`, `documents`, `messages`, `reports`, `rooms`, a
montagem em `hotel`): a requisição entra em `HttpApi`, que valida o corpo e monta o `Guest`, passa pelo
`ReservationService` e chega ao `ReservationStore` (JDBC, PostgreSQL, migrações do Flyway). Para os três tipos de
hóspede, criou uma hierarquia de documentos (`ReservationDocument` abstrata, com `IndividualReceipt`,
`CompanyInvoice` e `BookingReceipt`), uma fábrica concentrada (`DocumentFactory`, um `switch` sobre o tipo do
hóspede), quatro colunas novas numa migração `V2` com um `CHECK` de "exatamente uma identificação", e transformou o
antigo `ReservationReceipt` num invólucro que pede o documento à fábrica. Os textos da fatura e do booking receipt
saem exatamente como o dono pediu (ISS de R$ 38,99, `BRL 2,783.52`, "1 night", "Discount:"). O defeito mais sério é
de contrato: o campo `title` do documento sumiu do JSON do check-out e da segunda via (o pedido dizia que o documento
"continua com os campos de hoje"), e o agente mudou os dois testes da base que conferiam esse campo em vez de
mantê-lo. Isso responde por todas as 6 falhas do roteiro.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); na base,
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.documents` passou de 2 classes/79 linhas
para 9 classes/420 linhas; a maior classe continua `HttpApi` (170 → 189 linhas).
**Nota:** bom

A mudança não criou dependência nova nem ciclo novo: o ciclo `documents` ↔ `reservations` já existia na base
(`ReservationService` usa `ReservationReceipt`, que lê `Reservation`). A regra nova ficou no lugar certo: a escolha
do documento e o texto de cada um moram em `hotel.documents` (`src/main/java/hotel/documents/DocumentFactory.java:12-17`),
sem Javalin nem JDBC; o SQL novo ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-55`,
`:130-137`); a borda HTTP só ganhou a validação de "uma identificação só" e a montagem do `Guest`
(`src/main/java/hotel/web/HttpApi.java:127-145`), que é leitura da requisição, e a conversão para
`DocumentResponse` (`HttpApi.java:77`, `:79`). Um detalhe de forma: `HttpApi.java:77` e `:79` usam o nome
qualificado `hotel.documents.DocumentResponse` em vez de um `import`, e o `DocumentResponse` (um formato de JSON)
foi para `hotel.documents` e não para `hotel.web`, ao lado do `ReservationResponse`; nada que pese na nota.

## Design de baixo nível

**Números:** 3 casos de documento (o comprovante que já existia, a fatura, o booking receipt), os 3 na estrutura;
a escolha de qual criar num lugar só (`DocumentFactory.create`); os 3 lugares que emitem o documento
(`ReservationService.java:64`, `:109`, `:118`) passam todos pela fábrica, via `ReservationReceipt.java:14-16`;
fora da estrutura, 6 testes `instanceof` sobre o documento concreto em `ReservationReceipt.java:27-66`.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** A referência aceita como acerto "uma fábrica concentrada (uma classe ou um método que escolhe o
documento pelo tipo de hóspede)", e é o que foi feito: `DocumentFactory.java:12-17` decide entre
`IndividualReceipt`, `CompanyInvoice` e `BookingReceipt` pelo `GuestType`, e cada documento sabe escrever o seu
texto (`text()` abstrato em `ReservationDocument.java:77`). O caso que já existia (o comprovante do CPF) também foi
para a estrutura (`IndividualReceipt.java`), e os três lugares do serviço continuam chamando
`new ReservationReceipt(...)`, que delega à fábrica; nenhum deles cria documento à mão. A agência de turismo entraria
com uma classe nova e um `case` novo, e o `switch` exaustivo sobre o `enum` (`DocumentFactory.java:13`) obriga o
compilador a lembrar do caso. O que afasta: quem consome o documento ainda pergunta qual ele é. `ReservationReceipt`
faz `instanceof` seis vezes (`ReservationReceipt.java:27`, `:34`, `:41`, `:48`, `:55`, `:62`) para achatar os campos
próprios de cada tipo no JSON, e a agência vai pedir mais `instanceof` ali e mais campos em `DocumentResponse`. Como a
escolha de qual documento criar continua num lugar só, fica em acerto (ver Dúvidas).

**O exagero.** `ReservationReceipt` virou uma camada que só repassa: 11 dos seus 17 métodos apenas devolvem o
mesmo método do documento (`ReservationReceipt.java:18-24`, `:68-102`), e o resto são os `instanceof`. Em seguida,
`DocumentResponse` (`DocumentResponse.java:27-46`) copia esse invólucro campo a campo. São duas camadas entre o
documento e o JSON, onde bastaria o `ReservationDocument` (com métodos que devolvem `null` por padrão, sobrescritos
por tipo) e o `DocumentResponse`.

**O resto do código.**
- Duplicação: `IndividualReceipt.text()` e `CompanyInvoice.text()` repetem as mesmas oito linhas (quarto, datas,
  diárias, desconto, total) (`IndividualReceipt.java:25-33` e `CompanyInvoice.java:42-50`), e `BookingReceipt.java:40-49`
  repete a mesma sequência em inglês; a classe-base `ReservationDocument` não oferece nenhum trecho comum.
- `Guest` virou um registro com sete campos anuláveis (`Guest.java:3-10`), e o tipo é deduzido de qual campo não é
  nulo, com `FOREIGN` como o "senão" (`Guest.java:24-31`): um `Guest` sem nenhuma identificação vira estrangeiro
  calado. As fábricas estáticas `Guest.individual/company/foreign` (`Guest.java:12-22`) ajudam, e o `CHECK` do banco
  segura a inconsistência, mas um tipo por caso (ou o tipo guardado) seria mais seguro.
- `ReservationDocument` expõe o estado por campos `protected` (`ReservationDocument.java:12-21`), imutáveis
  (`final`), o que é aceitável.
- O dinheiro continua em `BigDecimal`, e o ISS é arredondado para centavos com `HALF_EVEN`, como o resto do sistema
  (`CompanyInvoice.java:21`); as datas continuam `LocalDate`. Bom.

## Testes

**Números:** final: 33 testes rodaram, 0 falhas, 0 erros, 0 pulados; 33 métodos escritos; cobertura de linhas
620/653 (94,9%), de ramos 145/163 (89,0%). Base: 26 testes, todos passando, linhas 93,8%, ramos 87,1%. O agente
acrescentou 7 testes (`GuestTypesTest`); `hotel.documents` tem 143/145 linhas e 23/27 ramos cobertos.
**Nota:** com problemas

O que é bom: os testes novos sobem a API de verdade contra o PostgreSQL e conferem os três jeitos de reservar com
valores: o ISS (`assertEquals("38.99", ...)`, `src/test/java/hotel/web/GuestTypesTest.java:135`), o dinheiro em
`BRL 779.80` e a data `2027-03-10` (`GuestTypesTest.java:172-173`), os campos que vêm `null` (`:83-84`, `:95`, `:118`,
`:121`, `:158-159`, `:176`) e as quatro recusas: sem identificação, CPF com CNPJ, CNPJ sem razão social e passaporte sem
país (`:179-267`).

O que pesa:
- **Testes da base enfraquecidos para acomodar uma regressão.** Os testes da base conferiam o campo `title` do
  documento; o agente trocou por `text().startsWith("Comprovante de reserva")` (`HttpApiTest.java:87` e
  `ReservationServiceTest.java:155`), e com isso o sumiço do campo `title` do JSON (que o roteiro de comportamento
  acusa 6 vezes) passou sem nenhum teste reclamar.
- **Asserções por pedaço, não pelo texto inteiro.** Os textos são conferidos com `contains` e `startsWith`
  (`GuestTypesTest.java:129-132`, `:169-173`); um "Hóspede:" faltando na fatura, ou uma linha fora de ordem,
  passaria.
- **Regras novas sem rede (a pergunta da mutação):** trocar `" night"` por `" nights"` no singular
  (`BookingReceipt.java:43`), apagar a linha `Discount:` (`BookingReceipt.java:45-47`) ou trocar o separador de
  milhar (`%,.2f` por `%.2f`, `BookingReceipt.java:53`) não derrubaria nenhum teste: nenhum testa uma noite, um
  desconto ou um valor acima de mil no booking receipt. Também não há teste de que o e-mail de confirmação leva a
  fatura ou o booking receipt, nem da segunda via dos tipos novos.

## Banco

**Números:** 3 tabelas, todas com chave primária; 2 migrações do Flyway (a `V1` da base igual, a `V2` nova);
dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em texto; as colunas `status`,
`rate_plan`, `type` e `channel` sem `CHECK` (vêm da base); 4 candidatos a SQL montado com `+`, todos com constantes.
**Nota:** bom

A mudança no esquema foi feita como o README da base pede: uma migração nova
(`src/main/resources/db/migration/V2__add_guest_identification_types.sql:2-12`) que solta o `NOT NULL` do
`guest_cpf`, acrescenta as quatro colunas e um `CHECK` que garante exatamente uma identificação completa
(`V2__...sql:8-12`). As reservas que já existem (com CPF e o resto nulo) passam no `CHECK`, então a migração sobe
num banco com dados. O `INSERT` e a leitura usam parâmetros (`ReservationStore.java:32-55`, `:130-137`). A
proteção contra duas reservas na mesma noite continua a restrição `EXCLUDE` da base. A falta de `CHECK` nas colunas
de situação e a consulta que lê a tabela inteira vêm da base, e o agente não as piorou.

## Comportamento

**Números:** 77 de 83 conferências passaram. Parte comum: 44 de 46; dados depois de reiniciar: 3 de 3; cenário 05:
28 de 32; duas reservas ao mesmo tempo: 2 de 2.
**Nota:** com problemas

As 6 falhas têm a mesma causa: o JSON do documento (check-out e segunda via) não traz mais o campo `title`
(veio `null` onde a base mandava "Comprovante de reserva"). Na base, o documento era um `record` com `title`
(`base/src/main/java/hotel/documents/ReservationReceipt.java:11`); o `DocumentResponse` novo não tem esse campo
(`src/main/java/hotel/documents/DocumentResponse.java:8-25`). O pedido era claro: "o documento continua com os campos
de hoje [...] mais esses quatro novos e um campo `text`". Para o dono da pousada, isso quer dizer que a tela do site
que mostrava o título do comprovante (na segunda via e no check-out) passa a mostrar vazio, para todos os hóspedes,
inclusive os de CPF, que deviam ver tudo como hoje; e o site também não consegue distinguir, por esse campo, se o
documento é a fatura ou o booking receipt (só lendo o `text`).

Todo o resto funciona: o texto do comprovante, da fatura (empresa, hóspede, ISS de R$ 38,99) e do booking receipt
(datas ISO, `BRL 2,783.52`, "1 night", "Discount:") sai certo; o e-mail continua em português e leva a fatura; as
quatro recusas dão 400 `INVALID_REQUEST`; os dados sobrevivem ao reinício; e as reservas simultâneas são barradas.

Fora do roteiro, pela leitura: um pedido com CPF e só o `companyCnpj` (sem a razão social) é aceito como pessoa
física, e o CNPJ é descartado em silêncio (`HttpApi.java:127-131`: o par incompleto não conta como identificação).
O texto pede "um desses jeitos, e só um"; recusar com 400 seria a leitura mais estrita. Não pesa na nota.

## Dúvidas

- A forma do pattern: fábrica concentrada (uma classe com um método estático e um `switch` sobre o `GuestType`,
  `DocumentFactory.java:12-17`), que a referência aceita como acerto. Pesei contra os seis `instanceof` em
  `ReservationReceipt.java:27-66`, que são o consumidor perguntando qual é o documento; eles não decidem qual criar
  (decidem quais campos expor no JSON), por isso fiquei em acerto e não em malfeito, mas um revisor mais estrito
  poderia ler ali "quem usa a estrutura ainda perguntando qual é o caso".
- O exagero: o `ReservationReceipt` foi mantido, talvez, para não mexer no `ReservationService`; mesmo assim, como
  camada, ele só repassa e faz `instanceof`, e por isso contei como exagero. É um caso de fronteira.
