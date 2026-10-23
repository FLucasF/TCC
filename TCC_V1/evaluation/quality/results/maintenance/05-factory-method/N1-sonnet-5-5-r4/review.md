# Revisão: manutenção, cenário 05

## Visão geral

O projeto mantém a forma da base: `hotel.web` (Javalin, `HttpApi`) lê a requisição e chama `ReservationService`
(`hotel.reservations`), que guarda tudo no PostgreSQL por `ReservationStore` (JDBC com `?`, migrações do Flyway) e
monta o documento (`hotel.documents`) nos três lugares de sempre: `confirm`, `checkOut` e `receiptCopy`. O agente não
mexeu no `ReservationService` (o `diff` com a base dá igual). Ele trocou o CPF do `Guest` por um tipo selado
`Identification` (`Cpf`, `Company`, `Passport`), em que cada caso diz qual layout de documento usa. Os layouts formam
uma pequena hierarquia: `DocumentLayout` escreve a sequência das linhas uma vez só, `PortugueseLayout` serve ao
comprovante e à fatura, e `BookingReceiptLayout` faz a versão em inglês. No banco, entrou uma migração nova (`V2`) com
as quatro colunas e um CHECK que garante exatamente um jeito de identificar o hóspede. Todos os scripts mediram sem
erro: 32 testes passam e as 83 conferências do comportamento também. Não há defeito sério. O que se pode apontar:
alguns formatos pedidos sem teste, o tipo de domínio `Identification` passou a conhecer as classes concretas de
layout, e a interface devolve `null` por padrão em tudo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. As maiores
classes: `HttpApi` (de 170 para 192 linhas), `ReservationStore` (de 143 para 161) e `ReservationService` (138, sem
mudança).
**Nota:** bom

- **A borda HTTP continua fina.** As rotas só leem, chamam e respondem (`web/HttpApi.java:65-80`). A validação nova
  (exatamente um jeito de identificar o hóspede) ficou na leitura do corpo, junto da validação que a base já fazia ali
  (`web/HttpApi.java:133-152`). Os campos novos foram para os DTOs (`web/CreateReservationRequest.java`,
  `web/ReservationResponse.java:34-41`).
- **O SQL continua só no acesso a dados.** As colunas novas ficaram em `ReservationStore`
  (`reservations/ReservationStore.java:18-20, 33-57, 131-139`). Os layouts não usam JDBC nem Javalin: na tabela de
  bibliotecas, `hotel.documents` não tem nenhuma.
- **O ciclo veio da base, mas o agente o reforçou.** Na base, a seta de `reservations` para `documents` era só o
  `ReservationService` criando `ReservationReceipt`. Agora o tipo de domínio `Identification` importa as três classes
  concretas de layout (`reservations/Identification.java:3-6, 40-58`). Na outra direção, `InvoiceLayout` usa
  `PriceCalculator.cents`, que precisou virar `public` (`documents/InvoiceLayout.java:27`,
  `reservations/PriceCalculator.java:50`). O número de arestas entre os pacotes não mudou, e o ciclo é o mesmo da base.
  Mesmo assim, o acoplamento ficou mais apertado.
- **A montagem não mudou** (`Hotel.java`).

## Design de baixo nível

**Números:** 3 tipos de hóspede, todos na estrutura: cada um dos três records de `Identification` devolve o seu layout.
Os 3 lugares que emitem o documento (`confirm`, `checkOut`, `receiptCopy`) passam pelo mesmo construtor e nenhum
decide o tipo à mão. Não há `if`/`switch` sobre o tipo de hóspede na emissão. São 5 classes novas em
`hotel.documents` (`DocumentLayout`, `PortugueseLayout`, `ReceiptLayout`, `InvoiceLayout`, `BookingReceiptLayout`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha do documento é um método polimórfico no próprio tipo de hóspede:
`Identification.documentLayout()` (`reservations/Identification.java:15`), implementado por `Cpf`, `Company` e
`Passport` (`reservations/Identification.java:38-58`). A referência do cenário aceita essa forma como acerto. Os três
lugares do `ReservationService` continuam com `new ReservationReceipt(reservation, room)`
(`reservations/ReservationService.java:64, 109, 118`). Esse construtor pede o layout à identificação, sem perguntar o
tipo (`documents/ReservationReceipt.java:33-55`). O caso que já existia (o CPF) também entrou na estrutura
(`documents/ReceiptLayout.java`), e o texto dele saiu igual ao da base: o teste `ReservationDocumentTest.java:42-56` e
o roteiro do comportamento conferem isso. Um tipo novo, como a agência de turismo, é um record novo no `sealed
interface` mais um layout. A sequência de linhas mora uma vez só, em `DocumentLayout.text`
(`documents/DocumentLayout.java:36-55`). Cada documento só dá as suas palavras, os formatos, a linha do hóspede e o
fecho (o ISS em `documents/InvoiceLayout.java:25-28`). Assim, não há cópia do texto por tipo.

**O que afasta um pouco.**
- `Identification` tem cinco métodos `default` que devolvem `null` (`reservations/Identification.java:17-35`). Os
  layouts leem os dados por esses métodos, sem o tipo concreto (`documents/BookingReceiptLayout.java:35-38`,
  `documents/InvoiceLayout.java:19-22`). Se um layout fosse ligado à identificação errada, o documento sairia com
  "null" no texto, sem erro.
- `documentLayout()` cria um objeto novo a cada chamada e é chamado duas vezes por documento
  (`documents/ReservationReceipt.java:39, 55`). Não custa nada, mas é desajeitado.

**O exagero.** Contenção. `PortugueseLayout` é dividido por dois documentos, e o record `Words` concentra as palavras
fixas (`documents/DocumentLayout.java:15-18`). As duas peças servem à variação pedida, e não há interface com uma
implementação só, fábrica à parte nem camada que só repassa.

**O resto do código.**
- O dinheiro continua em `BigDecimal`, com o mesmo arredondamento: o ISS é `PriceCalculator.cents(total × 0,05)`, meio
  para o par (`documents/InvoiceLayout.java:27`).
- As datas continuam em `LocalDate`, e os formatos novos ficaram em `TextFormat`
  (`documents/TextFormat.java:25-31`).
- A decodificação dos campos soltos em `Identification` aparece duas vezes, uma em cada borda: no HTTP
  (`web/HttpApi.java:133-152`) e no banco (`reservations/ReservationStore.java:131-139`). Isso é mapeamento, não regra
  duplicada.
- A validação usa um conjunto de booleanos (`anyX`/`x`) que se lê com algum esforço, mas está correta.
- Há um detalhe de forma: uma linha em branco separa o javadoc da classe (`reservations/ReservationStore.java:15-17`).
- O nome `ReservationReceipt` ficou para um record que agora também é fatura e booking receipt. É um nome herdado, e o
  JSON não mudou de forma.

Nenhum desses pontos pede mudança antes de aceitar.

## Testes

**Números:** 32 testes rodaram, com 0 falhas e 0 pulados. São 32 métodos escritos; a base tinha 26, todos mantidos, e
o agente acrescentou 6. A cobertura de linhas é de 94,9% (553 de 583; na base, 93,8%), e a de ramos, de 89,7% (140 de
156; na base, 87,1%). `hotel.documents` tem 74 de 74 linhas cobertas, e `hotel.web` passou de 18 de 28 para 53 de 64
ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

**O que está testado, com valores.**
- O texto inteiro dos três documentos, com os exemplos do pedido: comprovante com CPF, fatura com o ISS de R$ 38,99 e
  booking receipt (`documents/ReservationDocumentTest.java:42-93`).
- O booking receipt com desconto, BRL 116.97 e total BRL 662.83 (`documents/ReservationDocumentTest.java:96-109`).
- Pela API (`web/HttpApiTest.java:96-117`): a reserva de empresa com os campos que não se aplicam em `null`, o título
  da segunda via, o ISS de R$ 46,79 na alta temporada e o e-mail com a fatura.
- Cinco recusas com 400 e INVALID_REQUEST: nenhum jeito, dois jeitos (duas combinações), CNPJ sem razão social e
  passaporte sem país (`web/HttpApiTest.java:120-131`).
- Os testes da base só mudaram na construção do `Guest` (`changes.patch`); nenhum foi apagado nem enfraquecido.

**O que ficou sem rede.** Dois formatos que o pedido mostra explicitamente:
- O milhar em inglês: todos os valores testados ficam abaixo de mil. Se `"%,.2f"` virasse `"%.2f"`
  (`documents/TextFormat.java:26`), nenhum teste falharia.
- O singular "1 night": só se testam 2 noites. Se a condição `nights == 1` de `documents/DocumentLayout.java:46` fosse
  invertida, os testes do comprovante pegariam o erro, mas trocar a palavra em `BookingReceiptLayout.java:12` passaria.

Também não se testam o check-out devolvendo a fatura ou o booking receipt, o e-mail do estrangeiro, nem o país sem
passaporte. São lacunas pequenas, mas são regras novas pedidas no texto.

## Banco

**Números:** 3 tabelas, todas com chave primária, e 2 migrações: a `V1` igual à da base e a `V2` nova. O dinheiro está
todo em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data como texto. A `reservations` ganhou um
CHECK de "um jeito só de identificar". O EXCLUDE contra duas reservas na mesma noite continua. As conferências só
acusaram as colunas `status`, `rate_plan`, `type` e `channel` sem CHECK, que vêm da base (`measures/database.md`).
**Nota:** bom

- **A mudança de esquema é uma migração nova.** `V2__guest_identification.sql` tira o `NOT NULL` do `guest_cpf`,
  acrescenta `company_cnpj`, `company_name`, `guest_passport` e `guest_country`, e cria o CHECK
  `reservations_one_identification` (`src/main/resources/db/migration/V2__guest_identification.sql:2-14`). As linhas que
  já existiam (só com CPF) satisfazem o CHECK, então a migração sobe sobre um banco com dados. A `V1` não foi editada.
- **A regra também está no banco**, não só no HTTP. A leitura volta ao tipo certo pela mesma regra
  (`reservations/ReservationStore.java:131-139`).
- **O SQL novo usa parâmetros** (`reservations/ReservationStore.java:33-57`). Os `+` que o script listou só juntam
  constantes (`COLUMNS`).
- **As conexões** continuam em try-with-resources sobre o pool da base.
- **Detalhes que vieram da base:** as colunas de situação e de tipo sem CHECK; e o ISS não é gravado (é calculado na
  hora a partir do total gravado, o que é coerente).

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

- **Para o dono da pousada, tudo o que ele pediu funciona pela API.** Quem tem CPF continua recebendo o mesmo
  comprovante. A empresa recebe a fatura, com a empresa no lugar do CPF e o ISS retido de 5%, no e-mail (em
  português), no check-out e na segunda via. O estrangeiro recebe o booking receipt em inglês, com BRL, as datas ISO,
  "1 night", a linha "Discount" e o milhar com vírgula (BRL 2,783.52). As combinações inválidas dão 400
  INVALID_REQUEST, e os campos que não se aplicam vêm `null` na reserva e no documento, que traz o campo `text`.
- **O que já funcionava continua funcionando:** preços, recusas, cancelamentos, relatórios, os dados depois de
  reiniciar e as reservas simultâneas.

## Dúvidas

- A forma do Factory Method usada foi um método polimórfico no tipo de identificação do hóspede
  (`Identification.documentLayout()`), que a referência aceita como acerto. O produto que varia é o layout do texto,
  não o record do documento: `ReservationReceipt` é um só, com os campos de todos os tipos. Isso atende ao JSON pedido
  ("os campos de hoje, mais esses quatro novos e `text`"), por isso não foi contado contra.
- Na nota dos testes, as lacunas (milhar em inglês e "1 night") são pequenas diante do resto, que é bem conferido. Dei
  `com problemas` porque são formatos que o pedido mostra explicitamente, mas a nota fica na fronteira com `bom`.
