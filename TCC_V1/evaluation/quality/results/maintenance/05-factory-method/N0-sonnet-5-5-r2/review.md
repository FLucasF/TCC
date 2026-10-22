# Revisão: manutenção, cenário 05

## Visão geral

O agente partiu da base (7 pacotes: `hotel.web` com o Javalin, `hotel.reservations` com o serviço, as regras e o
acesso ao banco, `hotel.documents` com o comprovante, mais `rooms`, `messages`, `reports` e a montagem em `hotel`) e
mudou pouco: a requisição `POST /reservations` passa por `HttpApi.readNewReservation`, que agora lê os quatro campos
novos e confere que há exatamente uma identificação (`web/HttpApi.java:126-155`); o `Guest` ganhou quatro campos
anuláveis (`reservations/Guest.java:7-14`); o `ReservationStore` grava e lê as colunas novas; uma migração nova
(`V2__guest_identification.sql`) acrescenta as colunas e um CHECK de "uma identificação só". Os dados ficam no
PostgreSQL, com Flyway. O comportamento está todo certo (83 de 83 conferências) e o banco foi bem evoluído. O ponto
mais fraco é o design da parte que o pedido mirava: não há fábrica nem tipos de documento; o próprio
`ReservationReceipt` virou os três documentos ao mesmo tempo e pergunta duas vezes, com `if` sobre campos nulos, qual
deles é (`documents/ReservationReceipt.java:57-65` e `69-77`). A agência de turismo, já anunciada no pedido, vai
exigir mexer nos dois `if`, no record e no `Guest`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`); idênticos à
base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). A única mudança na tabela de
bibliotecas é `hotel.documents` passar a usar Jackson.
**Nota:** bom

A mudança respeita as camadas que a base já tinha: a borda HTTP continua lendo o corpo e validando a entrada
(`web/HttpApi.java:116-136`, como a base já fazia com os campos obrigatórios), o serviço não conhece Javalin nem SQL
(`reservations/ReservationService.java` não mudou), e o SQL das colunas novas ficou no `ReservationStore`
(`reservations/ReservationStore.java:31-58` e `129-136`). O ciclo `documents` ↔ `reservations` veio da base (o
serviço cria o `ReservationReceipt`, que lê a `Reservation`); o agente só o reforçou um pouco ao tornar público
`PriceCalculator.cents` (`reservations/PriceCalculator.java:50`) para usá-lo na fatura
(`documents/ReservationReceipt.java:93`). Dois detalhes pequenos, que não mudam a nota: a anotação do Jackson no
documento (`documents/ReservationReceipt.java:3,68`), só para o `text()` sair no JSON; e a regra "exatamente uma
identificação" mora só na borda HTTP (`web/HttpApi.java:140-155`) e no CHECK do banco, não no `Guest`: quem chamar
`ReservationService.create` por outro caminho com um `Guest` inválido só é barrado pelo banco.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), 0 em uma estrutura própria; a escolha do
documento é feita por 2 cadeias de `if` sobre campos nulos dentro do mesmo record (`titleFor` e `text()`); os 3
lugares que emitem o documento (`ReservationService.java:64`, `109`, `118`) continuam com `new ReservationReceipt(...)`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pedido descreve uma variação que cresce (CPF, empresa, estrangeiro, e a agência de turismo já anunciada), e a
resposta esperada é concentrar a escolha do documento num lugar (uma fábrica, um método de fábrica por tipo de
hóspede, ou um método polimórfico no hóspede). O agente não criou estrutura nenhuma: o `ReservationReceipt` passou a
ter os campos de todos os documentos (`documents/ReservationReceipt.java:16-32`) e decide qual ele é por `if`, uma
vez para o título (`ReservationReceipt.java:57-65`) e outra para o texto (`ReservationReceipt.java:69-77`), as duas
perguntando `companyCnpj != null` / `passport != null`. O tipo de hóspede nem existe como tipo: é deduzido de quais
dos sete campos anuláveis do `Guest` estão preenchidos (`reservations/Guest.java:7-14`). O que aproxima do pattern:
como o construtor do record já era o ponto único de criação na base, a decisão não ficou repetida nos três lugares de
`ReservationService`, e o texto de cada documento está em métodos separados (`receiptText`, `invoiceText`,
`bookingReceiptText`). Mas a variação mora em `if` sobre campos, e uma agência de turismo exige mexer nos dois `if`,
acrescentar campos ao record, ao `Guest`, à resposta e ao request: é a omissão que a referência descreve. Exagero:
nenhum; não há interface, fábrica ou camada a mais.

O resto do código: a duplicação entre as duas cadeias de `if` (a mesma pergunta em dois lugares que precisam
concordar); o `Guest` com sete campos anuláveis e sem nenhuma garantia de que só uma identificação está preenchida
(`Guest.java:7-18`; a validação está em `web/HttpApi.java:140-155`, longe do tipo), e o mesmo no record do documento;
o cabeçalho "título nº id" repetido em `receiptText` e `invoiceText` (`ReservationReceipt.java:81,89`) e o corpo do
booking receipt copiado em estrutura do corpo em português (`ReservationReceipt.java:110-125` contra `98-108`). O que
está certo: dinheiro em `BigDecimal`, o ISS arredondado com o mesmo `cents` (meio para o par) do resto
(`ReservationReceipt.java:93`), o formato inglês concentrado em `TextFormat` (`documents/TextFormat.java:25-31`), e a
validação de `requireOneIdentification` legível. São defeitos localizados, daí `com problemas`.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos (a base tinha 26, todos mantidos
sem mudança). Cobertura de linhas 94,6% (546/577; base 93,8%), de ramos 84,9% (141/166; base 87,1%); em
`hotel.documents`, 70/71 linhas e 14/16 ramos; em `hotel.web`, 48/66 ramos.
**Nota:** com problemas

O agente escreveu dois testes para a mudança. `ReservationServiceTest.java:44-52` confere o texto inteiro do booking
receipt com os valores do pedido (BRL 779.80, datas ISO, "2 nights"). `HttpApiTest.java:109-128` confere pela API três
recusas (nenhuma identificação, CNPJ sem razão social, CPF com passaporte), a criação por empresa com `guestCpf` null,
o título "Fatura de hospedagem", `guestPassport` null no documento e o ISS de R$ 38,99 no fim do texto. Os testes da
base passam e nenhum foi enfraquecido.

Ficam sem rede, pela pergunta da mutação: a linha "Empresa: ... (CNPJ ...)" e a linha "Hóspede:" da fatura (o teste
só olha o `endsWith` do ISS, `HttpApiTest.java:127`); "1 night" (trocar o `nights == 1` em
`ReservationReceipt.java:118` não quebra nada); a linha "Discount:" em inglês (`ReservationReceipt.java:120-122`); o
separador de milhar `%,.2f` (`TextFormat.java:26`; 779.80 não tem milhar); passaporte sem país e CPF com CNPJ (o
ramo `foreigner` de `HttpApi.java:145` poderia virar só `passport != null` sem teste falhar); e o documento novo no
e-mail de confirmação e no check-out (só a segunda via é testada). O comportamento passa em tudo isso, mas os testes do
agente não o garantiriam.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; 2 migrações do Flyway (`V1` igual à base, `V2` nova); `status`, `rate_plan`, `type` e
`channel` sem CHECK (veio da base); SQL montado com `+` só com constantes.
**Nota:** bom

O esquema evoluiu do jeito certo: uma migração nova (`src/main/resources/db/migration/V2__guest_identification.sql:3-15`),
sem tocar na `V1`, que tira o `NOT NULL` do CPF, acrescenta as quatro colunas e um CHECK que garante exatamente uma
identificação completa (`V2__guest_identification.sql:9-15`). As reservas antigas (todas com CPF e as colunas novas
nulas) satisfazem o CHECK, então quem já roda o sistema sobe sem problema. O `INSERT` e a leitura usam parâmetros
`?` (`reservations/ReservationStore.java:31-58`, `129-136`); as concatenações apontadas pelo script juntam só a
constante `COLUMNS`. A reserva dupla continua protegida pelo `EXCLUDE USING gist` da base. As colunas novas são
`text` sem limite, o que é o padrão da base.

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 05 32/32,
reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API: a pessoa com CPF recebe o mesmo comprovante de antes;
a empresa recebe a fatura com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80, no e-mail (que
continua em português), no check-out e na segunda via; o estrangeiro recebe o booking receipt com BRL 2,783.52, datas
2027-03-10, "1 night" e a linha "Discount"; nenhuma, duas ou uma identificação incompleta dão 400 INVALID_REQUEST; e a
consulta e o documento trazem os campos novos, com null nos que não se aplicam, e o `text`. Nada do que funcionava
quebrou, e os dados sobrevivem a um reinício.

## Dúvidas

- A classe do pattern ficou entre `omissão` e `malfeito`. A decisão não está repetida nos três lugares que emitem o
  documento (porque o construtor de `ReservationReceipt` já era o ponto único na base), o que afasta a definição
  literal de omissão; mas não há fábrica nem estrutura alguma criada pelo agente, e a variação mora em duas cadeias de
  `if` sobre campos nulos dentro de um record único (`ReservationReceipt.java:57-77`), que é o caso "a variação ficou
  num `if`". Fiquei com `omissão` porque não existe a estrutura que o `malfeito` pressupõe.
