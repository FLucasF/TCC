# Revisão: manutenção, cenário 05

## Visão geral

A base já era um sistema em pacotes por assunto: `hotel.web` (Javalin, as rotas em `HttpApi`), `hotel.reservations`
(o `ReservationService` com as regras e o `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.messages`,
`hotel.reports` e `hotel.documents` (o comprovante); os dados ficam no PostgreSQL, com o esquema criado por migrações
do Flyway. Uma requisição entra em `HttpApi`, que valida o corpo e chama o `ReservationService`, que usa os stores. A
mudança do agente fez o `ReservationReceipt` (que era um record) virar uma de três subclasses de uma classe abstrata
selada `Document` (`ReservationReceipt`, `InvoiceDocument` e `BookingReceipt`). A escolha do documento ficou num
método de fábrica estático, `Document.of` (`src/main/java/hotel/documents/Document.java:61-77`), que os três lugares do
serviço chamam. A identificação do hóspede virou quatro campos anuláveis a mais no `Guest`, com uma migração nova
(`V2`). O roteiro do comportamento passou inteiro (83 de 83) e os 31 testes passam. Os defeitos são de acabamento:

- o título do documento escolhido de novo por um `if` sobre os campos (`Document.java:141-150`);
- o mesmo `super(...)` de 15 argumentos copiado nas três subclasses;
- o ISS arredondado com `HALF_UP`, quando o resto do sistema usa `HALF_EVEN`;
- o DTO `ReservationResponse` buscando o quarto no banco e montando o documento;
- testes novos que só conferem trechos do texto;
- `guest_cpf` deixou de ser `NOT NULL`, e nenhuma restrição entrou no lugar.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O
Javalin fica só em `hotel` e `hotel.web`, o JDBC só em `hotel`, `hotel.messages`, `hotel.reservations` e
`hotel.rooms`. A maior classe é `HttpApi` (199 linhas, eram 170).
**Nota:** com problemas

O agente manteve o desenho da base: as rotas continuam finas, o SQL novo ficou no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:31-55`, `:129-137`) e a montagem continua no `main`. Não entrou
nenhuma dependência nem ciclo novo.

O defeito é uma mistura localizada que ele introduziu na borda HTTP. `ReservationResponse` é um DTO de resposta, e o
construtor dele agora recebe o repositório `Rooms`. Ele faz uma consulta ao banco e chama a fábrica de documentos
(`src/main/java/hotel/web/ReservationResponse.java:34`, `:56`). Com isso, as cinco rotas que devolvem a reserva
(`src/main/java/hotel/web/HttpApi.java:66-75`) passam a ir ao banco buscar o quarto e a montar um documento dentro do
objeto de serialização. Esse trabalho é do serviço. Além disso, o campo `text` na consulta da reserva não foi pedido:
o pedido põe o `text` só no documento.

A regra nova "exatamente um jeito de identificar" ficou na validação do `HttpApi`
(`src/main/java/hotel/web/HttpApi.java:127-147`). Isso segue o padrão da base, que já validava os campos obrigatórios
ali, mas engorda a classe que já era a maior.

## Design de baixo nível

**Números:** 3 tipos de documento, todos na estrutura (as três subclasses de `Document`). Os 3 lugares que emitem o
documento chamam a mesma fábrica: `ReservationService.java:64` (confirmação/e-mail), `:109` (check-out) e `:118`
(segunda via); um quarto uso está em `ReservationResponse.java:56`. Não sobrou nenhum `new ReservationReceipt` fora da
fábrica. Mas há um segundo lugar que decide pelo tipo de hóspede: `title()`, em `Document.java:141-150`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência aceita como acerto uma fábrica concentrada que escolhe o documento pelo tipo de hóspede, e
é isso o que existe. `Document.of` (`src/main/java/hotel/documents/Document.java:61-77`) decide entre
`ReservationReceipt`, `InvoiceDocument` e `BookingReceipt`, cada um com o seu `text()` sobrescrito
(`ReservationReceipt.java:28`, `InvoiceDocument.java:29`, `BookingReceipt.java:35`). Os três lugares do serviço, que na
base faziam `new ReservationReceipt`, pedem o documento à fábrica, e o caso que já existia (CPF) também entrou na
estrutura.

Duas coisas afastam o código do ideal:

- **O título escolhido duas vezes.** `title()` volta a perguntar qual é o caso com uma cadeia de `if` sobre os campos
  e um fallback `""` (`Document.java:141-150`), em vez de cada subclasse dar o seu título. Uma agência de turismo
  exigiria mexer em `of`, em `title()`, na lista `permits` (`Document.java:10`), nos campos da classe-mãe e no
  `Guest`.
- **O tipo de hóspede não é um tipo.** Ele é deduzido de qual campo veio preenchido: o `Guest` tem quatro campos
  anuláveis a mais (`src/main/java/hotel/reservations/Guest.java:3-10`), e a classe-mãe carrega os campos de
  identificação de todos os tipos (`Document.java:12-26`).

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com implementação única nem camada de repasse. O
`DocumentResponse` (`src/main/java/hotel/web/DocumentResponse.java`) é só um DTO de resposta, que se justifica.

**O resto do código.**

- **Duplicação.**
  - O construtor de cada subclasse repete a mesma chamada `super(...)` de 15 argumentos (`BookingReceipt.java:16-31`,
    `InvoiceDocument.java:10-25`, `ReservationReceipt.java:9-24`), quando bastaria um construtor `(Reservation, Room)`
    na classe-mãe.
  - O texto da fatura copia linha a linha o do comprovante: quarto, datas, diárias, desconto e total
    (`InvoiceDocument.java:37-45` contra `ReservationReceipt.java:32-40`).
- **Dinheiro.**
  - O ISS usa uma constante mágica e outro modo de arredondamento: `new BigDecimal("0.05")` com `HALF_UP`
    (`InvoiceDocument.java:30-31`).
  - O sistema arredonda "como sempre" com `HALF_EVEN` em `PriceCalculator.cents` (`PriceCalculator.java:50-51`), que o
    agente não reusou. Num total como R$ 390,10, o ISS de R$ 19,505 vira R$ 19,51 aqui e daria R$ 19,50 pela regra do
    sistema.
- **Limpeza.** Há imports sem uso: `Document.java:6-8` (`DateTimeFormatter`, `Locale`; `LocalDate` é usado) e
  `BookingReceipt.java:6` (`LocalDate`).

São defeitos localizados, que um revisor pediria para corrigir antes de aceitar.

## Testes

**Números:** com `final`, 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados (31 métodos escritos). A cobertura é
de 94,5% das linhas (641/678) e 86,7% dos ramos (137/158); `hotel.documents` tem 146/150 linhas e 19/24 ramos. Na
base eram 26 testes, com 93,8% das linhas e 87,1% dos ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base passando. Só os adaptou à nova assinatura de `Guest` e ao tipo `Document`
(`src/test/java/hotel/reservations/ReservationServiceTest.java:141-145`), sem enfraquecer nenhum. Ele acrescentou 5
testes:

- 2 no serviço (`ReservationServiceTest.java:164`, `:177`);
- 3 pela API (`src/test/java/hotel/web/HttpApiTest.java:124`, `:141`, `:159`).

O teste de recusas pela API é bom: cobre a falta de identificação, CPF com CNPJ, CNPJ sem razão social e passaporte
sem país (`HttpApiTest.java:159-190`).

Os testes do documento, porém, só conferem trechos do texto com `contains`, sem os valores que o pedido dá. Por
exemplo, `contains("ISS retido (5%)")` e `contains("BRL")` (`ReservationServiceTest.java:173`, `:188`;
`HttpApiTest.java:137`, `:155`).

Pela pergunta da mutação, nenhum teste falharia se alguém:

- trocasse `0.05` por `0.5` no ISS (`InvoiceDocument.java:30`), porque o valor R$ 38,99 não é conferido;
- trocasse o formato `%,.2f` em `Locale.US` pelo brasileiro, já que só se procura "BRL";
- trocasse `" night"` por `" nights"`;
- apagasse a linha `Discount:`.

O e-mail com a fatura e o check-out da empresa ou do estrangeiro também não têm teste. A parte nova roda, mas fica
quase sem rede nos valores.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway; a `V1` da base está igual e a `V2` é nova. Nenhuma tabela sem chave
primária, o dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice e nenhuma data em texto. O
`EXCLUDE` contra reserva dupla vem da base. As colunas novas são todas `text` sem restrição (`measures/database.md`).
**Nota:** com problemas

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a da base
(`src/main/resources/db/migration/V2__add_guest_identification_alternatives.sql:2-7`). O SQL novo usa parâmetros `?`
(`src/main/java/hotel/reservations/ReservationStore.java:32-55`), e a concatenação que o script aponta só junta
constantes (`COLUMNS`, `ReservationStore.java:18-20`).

O defeito é que a migração tira o `NOT NULL` de `guest_cpf` (`V2__...sql:3`, que na base era
`V1__create_tables.sql:24`) e não põe nada no lugar. Nenhum `CHECK` garante que a linha tem exatamente uma
identificação (o CPF, ou o CNPJ com a razão social, ou o passaporte com o país). A regra existe só na borda HTTP
(`HttpApi.java:127-147`). Uma linha sem nenhuma identificação passaria a quebrar `Document.of`, que lança
`IllegalArgumentException` (`Document.java:75`), e com ela a consulta da reserva.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:

- **CPF:** quem tem CPF continua recebendo o mesmo comprovante.
- **Empresa:** a empresa recebe a fatura com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80. A
  fatura vai dentro do e-mail em português e sai também no check-out.
- **Estrangeiro:** o estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, as datas `2027-03-10`,
  "1 night" e a linha `Discount`.
- **Recusas:** as quatro formas erradas de identificar o hóspede voltam 400 com INVALID_REQUEST.
- **O resto:** os dados sobrevivem ao reinício, e duas reservas simultâneas do mesmo quarto não passam.

O arredondamento do ISS com `HALF_UP` (ver o Design) não aparece no roteiro: no valor do pedido o resultado é exato.

## Dúvidas

- **A classe do pattern** fica entre acerto e malfeito. A forma usada é a fábrica concentrada (um método estático
  `Document.of` na classe-mãe selada, com uma subclasse por documento), e todos os lugares que emitem o documento
  passam por ela. Mas `title()` (`Document.java:141-150`) repete a decisão pelo tipo de hóspede dentro da própria
  estrutura. Dei acerto porque a escolha de qual documento criar mora num lugar só e ninguém fora dela pergunta o
  caso.
- **O arredondamento do ISS.** O pedido diz "arredondado para centavos como sempre". Não sei se o dono notaria a
  diferença entre `HALF_UP` e o `HALF_EVEN` do sistema, que só aparece quando o ISS cai exatamente em meio centavo.
- **O `text` na consulta da reserva** (`ReservationResponse.java:32`) não foi pedido. Não quebra nada pelo roteiro.
