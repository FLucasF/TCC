# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin com pacotes por assunto: `hotel.web` (rotas e JSON), `hotel.reservations` (o serviço de
regras, o `ReservationStore` com o JDBC, o preço), `hotel.documents` (o comprovante e a formatação), `hotel.rooms`,
`hotel.messages` e `hotel.reports`, montados em `hotel.Hotel`. O banco é criado por Flyway. Uma requisição passa
pelo `HttpApi`, vai ao `ReservationService` e chega ao `ReservationStore`. O agente acrescentou ao `Guest` os quatro
campos novos, uma migração V2, a validação "um jeito só" no `HttpApi`, e dois records novos, `Invoice` e
`BookingReceipt`. O texto de cada documento, porém, existe duas vezes: uma nos records e outra em métodos
privados do `ReservationService`. O tipo de hóspede é decidido em dois `if` paralelos, e `checkOut`/`receiptCopy`
passaram a devolver `Object`. O defeito mais sério está na fatura e no booking receipt: o JSON que sai no check-out e
na segunda via não tem `text` nem os campos de hoje, e o roteiro falha em 16 das 32 conferências do cenário. Há ainda
um erro de dinheiro: a fatura de uma reserva com desconto mostra como "Total" o valor sem desconto e calcula o ISS
sobre ele, sem arredondar para centavos.

## Arquitetura

**Números:** 7 pacotes, 16 dependências (17 na base), 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). `ReservationService` passou de 138 para 226 linhas e é agora a maior classe.
**Nota:** com problemas

As camadas da base se mantiveram. As rotas continuam finas, o SQL novo ficou no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:18`, `:127`), a montagem não mudou, e o agente não criou
nenhum ciclo novo. A dependência `hotel.web -> hotel.documents` sumiu, mas por um motivo ruim: `checkOut` e
`receiptCopy` agora devolvem `Object` (`src/main/java/hotel/reservations/ReservationService.java:109`, `:120`).
O problema de lugar é que a montagem do texto dos três documentos (português, inglês, formato de dinheiro e de
data) foi parar dentro do serviço de regras (`ReservationService.java:155-211`, incluindo um `englishMoney` próprio
em `:209`), quando a base a deixava em `hotel.documents`. Isso fez o serviço crescer 64% e misturou apresentação com
regra. O cálculo do ISS, uma regra fiscal, também ficou no serviço, e em duas cópias (`:136-138` e `:172-174`). O
`HttpApi` agora chama o serviço para gerar o texto do documento a cada resposta de reserva
(`src/main/java/hotel/web/HttpApi.java:205-209`). A mistura é localizada, mas é exatamente o lugar que o próximo
documento (o das agências) vai engordar.

## Design de baixo nível

**Números:** 3 tipos de documento; a escolha pelo tipo de hóspede aparece em 2 `if`/`else if` paralelos (`ReservationService.java:132-143` para o objeto, `:145-153` para o texto). Os 3 lugares de saída se dividem assim: check-out e segunda via usam um, o e-mail usa o outro. Cada texto de documento está escrito 2 vezes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O que aproxima do acerto: existe um método que concentra a escolha do documento,
`generateDocument` (`ReservationService.java:132`), e `checkOut` (`:116`) e `receiptCopy` (`:125`) pedem o
documento a ele. O que afasta: o terceiro lugar, o e-mail de confirmação, não usa essa fábrica. Ele chama
`generateDocumentText` (`:71`), que repete a mesma decisão por tipo de hóspede (`:146-152`) e monta o texto à mão,
com cópias próprias de cada documento (`:155-207`). A fábrica não tem tipo comum: devolve `Object` (`:132`), então
quem usa o documento precisa saber qual é para chegar ao conteúdo. Os testes fazem cast
(`src/test/java/hotel/reservations/ReservationServiceTest.java:141`, `:155`), e o `HttpApi` só consegue
serializá-lo às cegas. A escolha também é feita por `null`-check no `Guest` (`cpf() != null`, `companyCnpj() != null`,
senão passaporte), e não por um tipo de hóspede. Pela referência, isso é `malfeito`: a fábrica existe, mas um dos
três lugares cria o documento por outro caminho, com a escolha repetida. Para o documento das agências, alguém teria
de mexer nos dois `if` e escrever o texto em dois lugares.

**O exagero.** Não há estrutura sem necessidade. Os métodos `Guest.withCpf/withCompany/withPassport`
(`src/main/java/hotel/reservations/Guest.java:17-27`) são só construtores com nome.

**O resto do código.**
- Duplicação grave. O texto do comprovante está em `ReservationReceipt.java:50-64` e em
  `ReservationService.java:155-169`. O da fatura está em `Invoice.java:37-49` e em `ReservationService.java:171-188`.
  O do booking receipt está em `BookingReceipt.java:41-58` e em `ReservationService.java:190-207`. O
  `englishMoney` também existe duas vezes (`BookingReceipt.java` e `ReservationService.java:209`). O e-mail e o
  documento podem passar a divergir na primeira correção que alguém fizer num só dos lados.
- O ISS está errado e duplicado (`ReservationService.java:136-138`, `:172-174`). É calculado sobre o `subtotal`, e
  não sobre o total (o pedido diz "5% do total"), e não é arredondado para centavos como o resto
  (`PriceCalculator.cents`, HALF_EVEN). O `divide` sem escala deixa 58.485 no JSON, e o texto o arredonda por
  `%.2f` (HALF_UP) para 58,49.
- `Invoice.text()` escreve "Total:" com o `subtotal` (`Invoice.java:47`; `ReservationService.java:185`), e o record
  nem tem os campos `discount` e `total`.
- `Object` como tipo de retorno no lugar de um tipo de documento (`ReservationService.java:109`, `:120`, `:132`).
- Nomes qualificados inline em vez de `import` (`HttpApi.java:66-83`: `hotel.reservations.Reservation`;
  `ReservationService.java:191`, `:210`: `java.time.format.DateTimeFormatter`, `java.util.Locale`).
- `ReservationReceipt` agora carrega os campos de empresa e passaporte, que serão sempre `null` nele
  (`ReservationReceipt.java:15-18`). E o `title` deixou de ser usado no texto: o título está fixo em
  `ReservationReceipt.java:52`.

Os defeitos estão espalhados por todo o código novo e tornam a mudança insegura. Por isso a nota é `ruim`.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos (26 na base, +2). Cobertura de linhas 84,8% (base 93,8%), de ramos 79,8% (base 87,1%). Em `hotel.documents`, 34 de 91 linhas e 3 de 10 ramos (na base, 30 de 30).
**Nota:** com problemas

Os 26 testes da base continuam passando. O agente só os adaptou: trocou `new Guest(...)` por `Guest.withCpf` e
acrescentou casts (`ReservationServiceTest.java:141`, `:155`). Nenhum teste foi enfraquecido além disso. Os dois
testes novos são pela API (`src/test/java/hotel/web/HttpApiTest.java:109` e `:126`). Eles conferem que os campos
de empresa e de passaporte voltam na reserva e que `guestCpf` vem `null`. Do documento, conferem só que
`text` contém "Fatura de hospedagem" ou "Booking receipt" (`:123`, `:140`), e esse `text` é o da reserva, não o do
documento. Ficaram sem teste:

- o valor do ISS (trocar 5 por 50, ou `subtotal` por `total`, não derruba nenhum teste);
- o formato inglês (`BRL 1,234.56`, a data `2027-03-10`, "1 night", a linha "Discount:");
- as recusas novas (nenhuma identificação, duas juntas, CNPJ sem razão social, passaporte sem país);
- o documento no check-out e na segunda via para empresa e estrangeiro. É justamente onde está o defeito que o
  roteiro acha: `Invoice` e `BookingReceipt` quase não são executados (a cobertura de `hotel.documents` caiu
  para 37%).

Os testes passam e protegem a base, mas a parte nova do cenário ficou quase sem rede.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`, a chave estrangeira com índice, a restrição EXCLUDE contra reserva dupla. Migrações: V1 igual à da base, V2 nova. Nenhuma conferência acusou defeito de tipo.
**Nota:** com problemas

A evolução está certa: o agente não tocou na V1 e criou
`src/main/resources/db/migration/V2__add_identification_fields.sql:1-8`, que acrescenta as quatro colunas e tira
o `NOT NULL` de `guest_cpf`. O SQL continua com parâmetros (`ReservationStore.java:31-50`), e a leitura traz as
colunas novas (`:127-142`). O defeito é de integridade. A base garantia no banco que todo hóspede tinha
identificação (`V1__create_tables.sql:24`, `guest_cpf text NOT NULL`). A V2 tira essa garantia
(`V2__add_identification_fields.sql:7-8`) e não põe nada no lugar: nenhum `CHECK` de "exatamente um dos três
jeitos" ou de "CNPJ junto com a razão social". Hoje, só o `HttpApi.java:136-163` impede uma reserva sem documento
ou com dois. Uma linha assim faria o `generateDocument` emitir um booking receipt com "passport null"
(`ReservationService.java:140-141`). O ISS e o total da fatura não são guardados, são calculados, então o erro de
dinheiro pesa no design e no comportamento, não aqui.

## Comportamento

**Números:** 67 de 83 conferências passaram. Parte comum: 46 de 46. Depois de reiniciar: 3 de 3. Cenário 05: 16 de 32. Reservas simultâneas: 2 de 2.
**Nota:** ruim

O que funciona: a pessoa com CPF continua igual; o e-mail de confirmação leva a fatura ou o booking receipt certo,
em português por fora; as quatro recusas dão 400 INVALID_REQUEST; a consulta da reserva mostra os campos novos com
`null` nos que não se aplicam.

As 16 falhas estão todas no documento de empresa e de estrangeiro. O JSON da segunda via
(`GET /reservations/{id}/document`) e o do check-out vêm sem `text` e sem `title`. `Invoice` e `BookingReceipt`
têm `text()` como método, não como componente do record, e o Jackson não o serializa (`Invoice.java:9-20`,
`:37`; `BookingReceipt.java:11-23`, `:41`). A fatura também vem sem os campos de hoje (`guestCpf`, `discount`,
`total`, os de passaporte). Chamei a API à mão para confirmar. A segunda via da fatura veio
`{"reservationId":1,"companyName":...,"subtotal":1169.70,"issRetained":58.485}`, sem `text`, `total` ou
`discount`. Na mesma chamada apareceu um erro de dinheiro que o roteiro não cobre: uma reserva NON_REFUNDABLE de
empresa (subtotal R$ 1.169,70, desconto R$ 175,46, total R$ 994,24) recebeu por e-mail uma fatura com
"Total: R$ 1.169,70" e "ISS retido (5%): R$ 58,49". O certo seria R$ 994,24 e R$ 49,71.

Para o dono da pousada: o site não consegue mostrar a fatura nem o booking receipt no check-out e na segunda via,
que são dois dos três lugares onde o documento deve sair. E toda empresa com tarifa não reembolsável recebe uma
fatura com valor e ISS maiores do que os que ela pagou. É dado errado em documento fiscal.

## Dúvidas

- A forma de fábrica usada é um método privado concentrado no serviço (`generateDocument`), mas o e-mail decide por
  outro `if` paralelo. Classifiquei como `malfeito`, e não `omissão`, porque existe um ponto de criação usado em dois
  dos três lugares. Um leitor mais severo poderia ver os dois `if` repetidos como `omissão`.
- O pedido não diz se o ISS da fatura deve considerar o desconto, mas diz "5% do total" e mostra "Total" na fatura.
  Por isso tratei o uso do `subtotal` como erro, e não como escolha.
- O campo `text` também foi posto na resposta da reserva (`ReservationResponse`), o que o pedido não pedia. Não pesei
  isso, porque não quebra nada.
