# Revisão: construção, cenário 05

## Visão geral

O projeto está dividido em pacotes por assunto: `hotel` (a montagem e o banco), `web` (o Javalin), `reservations` (as
regras e o acesso às reservas), `rooms`, `documents`, `messages` e `reports`. Uma requisição entra numa rota curta de
`web/HttpApi.java:61-91`, que lê o corpo e chama o `ReservationService`. O serviço valida, calcula o preço com o
`PriceCalculator` e grava pelo `ReservationStore`, que usa JDBC com parâmetros sobre um pool Hikari. Os dados ficam no
PostgreSQL, num esquema criado por duas migrações do Flyway. A parte do cenário é uma fábrica concentrada:
`documents/DocumentFactory.java:19-27` escolhe o escritor do documento pelo tipo de hóspede, e os três lugares que
emitem o documento pedem a ela. Todas as 83 conferências do comportamento passaram, inclusive as dez rodadas de reservas
simultâneas, que uma restrição `EXCLUDE` do banco segura. O sistema não tem nada sério. Os defeitos são menores: um
ciclo entre `documents` e `reservations`, um trecho repetido nos escritores de documento, relatórios que trazem a
tabela inteira de reservas para filtrar em Java e a confirmação sem transação.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A maior
classe tem 184 linhas (`HttpApi`). O Javalin aparece só em `hotel` e `hotel.web`. O JDBC aparece em `hotel`,
`messages`, `reservations` e `rooms`.
**Nota:** com problemas

- **A borda é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/HttpApi.java:62-90`). O
  mapeamento de erro para status HTTP fica num lugar só (`web/HttpApi.java:93-107`). A validação da forma do corpo
  também fica na borda (`web/HttpApi.java:109-140`), e isso é razoável. A regra "exatamente uma identificação"
  (`web/HttpApi.java:126-134`), porém, só existe na borda e no CHECK do banco. Nem o `Guest` nem o serviço a garantem.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` (`reservations/ReservationService.java:29-121`)
  e `PriceCalculator` só falam com o `ReservationStore`, o `Rooms` e o `EmailSender`.
- **O acesso a dados fica isolado em classes próprias:** `ReservationStore`, `Rooms` e `SentMessages`. O
  `ReservationStore` mora no mesmo pacote das regras (`reservations`), e isso é aceitável numa organização por assunto.
- **A montagem fica num lugar só** (`Hotel.java:20-28`), com uma exceção: o serviço cria a própria fábrica de
  documentos com `new` (`reservations/ReservationService.java:19`).
- **O ciclo:** `documents` usa `Reservation`, `Guest` e `Room` (`documents/DocumentFactory.java:3-5`). `reservations`
  usa `DocumentFactory` e `ReservationReceipt` (`reservations/ReservationService.java:3-4`). É um ciclo pequeno entre
  dois vizinhos, mas é do próprio agente. Ele sumiria se o serviço recebesse a emissão do documento por fora ou se o
  documento ficasse do lado de `reservations`.
- **Os relatórios** dependem direto do `ReservationStore` (`reports/OccupancyReport.java:16`,
  `reports/RevenueReport.java:17`). Isso é aceitável, mas aumenta o uso do pacote `reservations`.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura e booking receipt), todos na estrutura e nenhum fora. A escolha
acontece em 1 lugar (`documents/DocumentFactory.java:19-27`). Os 3 pontos de emissão pedem o documento à fábrica:
confirmação (`reservations/ReservationService.java:66`), check-out (`:111`) e segunda via (`:120`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

- **O pattern.** É uma fábrica concentrada. `DocumentFactory.issue` (`documents/DocumentFactory.java:14-17`) pergunta
  uma vez qual é o tipo de hóspede, escolhe um `DocumentWriter` (`documents/DocumentWriter.java:7-12`, com 3
  implementações) e monta o `ReservationReceipt`. Ninguém mais decide qual documento criar: o e-mail usa o
  `receipt.text()` (`reservations/ReservationService.java:127-133`) sem saber o tipo. Uma agência entra com um escritor
  novo e um ramo novo em `writerFor`. A referência aceita essa forma como acerto.
- **O que afasta da forma ideal.** A escolha se apoia em perguntas sobre campos nulos (`reservations/Guest.java:20-26`,
  `isCompany`/`isForeigner`), não num tipo de hóspede. O `Guest` é um record achatado com sete campos, cinco deles
  anuláveis (`reservations/Guest.java:7-14`), e por isso combinações inválidas cabem no tipo. A ordem dos `if` em
  `documents/DocumentFactory.java:20-26` também decide o caso "empresa e passaporte ao mesmo tempo", que só a borda
  impede.
- **O exagero.** Não há. A interface `DocumentWriter` tem três implementações reais. Não há builder, camada de
  repasse nem interface com uma implementação só.
- **O resto do código, os defeitos.**
  - Há duplicação entre `documents/PersonReceiptWriter.java:20-29` e `documents/CompanyInvoiceWriter.java:23-32`: as
    linhas de quarto, datas, diárias, desconto e total são iguais, copiadas. O plural "diária/diárias" também está
    repetido.
  - O ISS repete o arredondamento à mão (`documents/CompanyInvoiceWriter.java:33-34`) em vez de usar o
    `PriceCalculator.cents` que já existe (`reservations/PriceCalculator.java:50-52`). O resultado é o mesmo, mas a
    regra fica em dois lugares.
  - `Reservation.changeStatus` aceita qualquer transição (`reservations/Reservation.java:102-104`). As regras de
    situação ficam todas no serviço (`reservations/ReservationService.java:61, 80-81, 95, 106`), e a entidade não as
    protege.
  - O `catch (Exception)` em `web/HttpApi.java:113` converte qualquer falha de leitura do corpo em 400, sem guardar a
    causa.
- **O resto do código, os acertos.** O dinheiro está em `BigDecimal`, arredondado com `HALF_EVEN` a cada passo
  (`reservations/PriceCalculator.java:19-39`). As datas são `LocalDate`. O plano de tarifa, a situação e o tipo de
  quarto são `enum`, e os `switch` sobre eles são exaustivos (`reservations/PriceCalculator.java:26-29`,
  `reservations/ReservationService.java:84-87`). Os métodos são curtos e os nomes são claros.

## Testes

**Números:** 32 testes rodaram, 0 falharam, 0 foram pulados, e há 32 métodos escritos. A cobertura é de 94,5% das
linhas (551/583) e 88,2% dos ramos (134/152). Em `documents`, 86/87 linhas e 14/16 ramos. São 5 classes contra o banco
real e 1 pela API HTTP, sem mocks.
**Nota:** bom

- **A parte nova está conferida com o texto inteiro.**
  - Fatura com ISS de R$ 38,99: `documents/DocumentsByGuestTest.java:52-72`.
  - Booking receipt inteiro: `documents/DocumentsByGuestTest.java:87-105`.
  - "1 night", "Discount" e "BRL 2,783.52": `documents/DocumentsByGuestTest.java:108-116`.
  - Fatura no e-mail e no check-out: `documents/DocumentsByGuestTest.java:75-84`.
  - Recusas de identificação ausente, dupla ou incompleta: `web/HttpApiTest.java:109-128`.
- **A pergunta da mutação.** Trocar 0.05 por outro valor, trocar a ordem de escolha na fábrica ou trocar o formato
  `Locale.US` derruba algum teste.
- **A parte comum também está conferida com valores.**
  - Temporada nas bordas (15/12 e 29/02): `reservations/PriceCalculatorTest.java:31-54`.
  - Arredondamento para o par, 37,485 → 37,48: `reservations/PriceCalculatorTest.java:57-64`.
  - Devolução por plano: `reservations/ReservationServiceTest.java:109-122`.
  - Transições: `reservations/ReservationServiceTest.java:99-156`.
  - Relatórios comparados linha a linha: `reports/ReportsTest.java:56-115`.
- **O que fica sem rede.**
  - A linha "Desconto" da fatura de empresa: `documents/CompanyInvoiceWriter.java:30` nunca roda.
  - O "1 diária" em português.
  - O texto completo do comprovante com CPF: só se confere a linha do hóspede em
    `documents/DocumentsByGuestTest.java:127`.
  - As reservas simultâneas: nenhum teste dispara dois pedidos ao mesmo tempo.

  São lacunas pequenas.

## Banco

**Números:** 3 tabelas, todas com chave primária. São 2 migrações Flyway. Todo o dinheiro está em `numeric(10,2)`, as
datas em `date`, e a única chave estrangeira tem índice (o da `EXCLUDE`). As conferências acusaram 4 colunas de
situação ou tipo sem CHECK (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`). O
código não tem nenhuma transação explícita.
**Nota:** com problemas

- **Os acertos.**
  - O esquema evolui por migrações versionadas (`src/main/resources/db/migration/V1__create_tables.sql`,
    `V2__guest_identification.sql`), aplicadas ao subir (`Database.java:28`).
  - A regra de não ter duas reservas na mesma noite está no banco, com
    `EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')`
    (`V1__create_tables.sql:38-40`). A violação é traduzida em 409 (`reservations/ReservationStore.java:59-63`). Por
    isso o `synchronized` do serviço não é a única proteção.
  - A identificação única também tem CHECK (`V2__guest_identification.sql:8-11`).
  - Todo o SQL usa parâmetros. Os quatro trechos montados com `+` juntam só constantes
    (`reservations/ReservationStore.java:18-20, 32-34, 87-88, 103, 116`).
  - As conexões vêm do pool e são devolvidas por try-with-resources.
- **Os defeitos.**
  - Os dois relatórios carregam a tabela inteira de reservas e filtram em Java (`reservations/ReservationStore.java:113-126`,
    usado em `reports/OccupancyReport.java:25` e `reports/RevenueReport.java:25`). A ocupação ainda busca o quarto
    linha a linha (`reports/OccupancyReport.java:36`). A tabela cresce sem limite.
  - A confirmação grava a reserva e a mensagem em duas conexões, sem transação
    (`reservations/ReservationService.java:64-70`). Se a gravação do e-mail falhar, a reserva fica confirmada e paga
    sem o e-mail registrado.
  - `status` e `rate_plan` são `text` sem CHECK (`V1__create_tables.sql:28, 33`).
  - O CHECK da identificação deixa passar um CPF com um CNPJ sem razão social, porque a soma ainda dá 1
    (`V2__guest_identification.sql:9-11`). A borda impede isso, mas o banco não.

## Comportamento

**Números:** 83 de 83 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 05: 32/32;
- reservas simultâneas: 2/2, ou seja, nas 10 rodadas sempre uma reserva criada e a outra recusada com 409.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- os preços com alta temporada e desconto, com o arredondamento certo;
- as recusas com os códigos combinados;
- o e-mail de confirmação com o documento certo;
- os relatórios em CSV;
- os dados guardados depois de reiniciar.

Na parte nova, a pessoa com CPF continua recebendo o comprovante. A empresa recebe a fatura com o ISS de R$ 38,99,
dentro de um e-mail em português, e a recebe também no check-out. O estrangeiro recebe o booking receipt em inglês,
com "BRL 2,783.52", datas 2027-03-10 e "1 night". A reserva sem identificação, com duas identificações ou com uma
incompleta é recusada com 400. Duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- A forma do pattern é uma fábrica concentrada (classe `DocumentFactory` com um método que escolhe o escritor pelo
  tipo de hóspede). Não é o Factory Method clássico com criador sobrescrito, nem um método polimórfico no hóspede. A
  referência aceita essa forma como acerto.
- A escolha depende de perguntas sobre campos nulos de um `Guest` achatado. Isso pesou só na nota do design, não na
  classe.
- A nota do banco ficou entre `bom` e `com problemas`. O esquema é bom. O que pesou foram os relatórios que leem a
  tabela inteira e a confirmação sem transação, ambos localizados.
