# Revisão: construção, cenário 05

## Visão geral

O projeto foi feito em sete pacotes sob `hotel`. Em `hotel` ficam a montagem e o pool com Flyway (`Hotel.java:20-29`, `Database.java:18-29`). Em `hotel.web` fica a borda HTTP (`HttpApi`). Em `hotel.reservations` ficam as regras (`ReservationService`, `PriceCalculator`, `Reservation`) e o acesso aos dados das reservas (`ReservationStore`). Os outros quatro são `hotel.rooms`, `hotel.documents` (os textos dos documentos), `hotel.messages` (os e-mails guardados) e `hotel.reports` (os CSV). Uma requisição passa pela rota do Javalin, que monta um `NewReservation` (`HttpApi.java:109-140`). Daí vai para o `ReservationService`, que confere as regras, calcula o preço e grava pelo `ReservationStore` com JDBC e `?`. Os dados ficam no PostgreSQL, num esquema criado por duas migrações do Flyway. Há também uma restrição `EXCLUDE` contra duas reservas na mesma noite (`V1__create_tables.sql:38-40`). O sistema faz tudo o que o texto pede: 83 de 83 conferências passaram, e os 32 testes do próprio agente passaram. O mais sério é o pattern, que ficou de fora. A escolha do documento por tipo de hóspede é um `if/else if/else` copiado nos três lugares onde o documento sai (`ReservationService.java:67-76`, `123-132`, `144-153`). Com isso, a agência de turismo anunciada no texto vai exigir mexer nos três.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há 1 ciclo: `hotel.documents` ↔ `hotel.reservations` (de `measures/dependencies.md`).
**Nota:** com problemas

As camadas estão bem separadas. As rotas só leem a requisição, chamam o serviço e escrevem a resposta (`HttpApi.java:62-90`). Os erros viram status num lugar só (`HttpApi.java:93-107`). As regras não conhecem o Javalin: a tabela do jdeps mostra Javalin só em `hotel` e `hotel.web`. O SQL fica isolado em três classes de acesso a dados: `ReservationStore`, `Rooms` e `SentMessages`. O serviço não monta SQL. A montagem acontece num lugar só, com as dependências passadas pelo construtor (`Hotel.java:20-29`).

Três defeitos:
- **O ciclo novo entre `documents` e `reservations`.** `DocumentTexts` e `ReservationReceipt` leem `Reservation` (`DocumentTexts.java:3`, `ReservationReceipt.java:3`). Ao mesmo tempo, o `ReservationService` monta os documentos e o corpo do e-mail (`ReservationService.java:3-4`, `66-80`, `161-167`). A responsabilidade de "qual documento e com que texto" ficou dividida entre os dois pacotes.
- **A regra "exatamente uma identificação" mora só no handler HTTP** (`HttpApi.java:126-134`). O serviço aceita qualquer `Guest`. Quem chamar o serviço por outro caminho só é barrado pelo CHECK do banco (`V2__guest_identification.sql:8-11`), e com um `IllegalStateException` em vez de `INVALID_REQUEST` (`ReservationStore.java:64`).
- **Relatórios fora do pacote das regras.** Os relatórios dependem direto do `ReservationStore` e filtram em Java (`OccupancyReport.java:25`, `RevenueReport.java:25`). Isso é aceitável neste tamanho.

Nenhuma classe é desproporcional: as maiores são `HttpApi` com 184 linhas e `ReservationService` com 174.

## Design de baixo nível

**Números:** dos 3 lugares onde o documento é criado (o e-mail de confirmação, o check-out e a segunda via), 0 pedem o documento a uma fábrica. Os 3 repetem o mesmo `if (isCompany) … else if (isForeigner) … else` com o `new ReservationReceipt(...)` e o título escrito à mão. Os 3 tipos de documento ficam fora de qualquer estrutura.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede três documentos, um por tipo de hóspede, que saem nos mesmos três lugares, e anuncia um quarto documento (a agência). A referência pede que a escolha de qual documento criar more num lugar só. Aqui ela mora em três cópias idênticas: `ReservationService.java:67-76` em `confirm`, `123-132` em `checkOut` e `144-153` em `receiptCopy`.

O texto de cada documento foi separado em funções (`DocumentTexts.personReceipt`, `companyInvoice` e `foreignReceipt`), mas a referência é explícita: o que importa é onde se decide qual criar. A decisão depende de flags derivadas de campos nulos (`Guest.java:20-26`), e o título também é passado à mão em cada cópia. Uma agência exigiria um quarto ramo nas três cópias, um novo `isAgency()` e mais campos nulos no `Guest`. Isso é omissão.

**O exagero.** Nenhum: não há interface com uma implementação só, fábrica sem uso nem camada que só repasse. `PriceCalculator`, `EmailSender` e `Rooms` são classes concretas e diretas.

**O resto do código.** Os tipos estão certos. Dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:50-52`, `22-30`), datas são `LocalDate` e as situações e planos são `enum`. Os erros têm códigos próprios (`HotelException`, `ErrorCode`). Os defeitos que um revisor apontaria:
- **Duplicação nos textos.** `personReceipt` e `companyInvoice` repetem as mesmas seis linhas de quarto, datas, diárias, desconto e total (`DocumentTexts.java:20-29` e `39-48`). O "1 diária / N diárias" e o "Desconto" estão copiados.
- **O hóspede como um record de 7 campos, dos quais 4 ficam sempre nulos** (`Guest.java:7-14`). Os três jeitos de identificar não viraram tipos, e o código pergunta por nulidade (`isCompany`, `isForeigner`).
- **Constante solta.** O ISS de 5% é um literal `new BigDecimal("0.05")` com arredondamento à mão (`DocumentTexts.java:49-50`), enquanto as outras taxas são constantes (`PriceCalculator.java:16-17`).
- **Transições espalhadas.** As transições de situação ficam no serviço. A `Reservation` expõe um `changeStatus` livre (`Reservation.java:102-104`), e as regras de transição não estão na entidade.

São defeitos localizados. O resto (o serviço, o cálculo, a borda HTTP) é legível e curto.

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 32 métodos de teste escritos. A cobertura é de 94,2% das linhas (557/591) e 88,1% dos ramos (141/160). Em `hotel.reservations` ela é de 243/258 linhas e 64/68 ramos (de `measures/tests.md`). Os testes rodam contra o PostgreSQL de verdade, e 1 classe passa pela API HTTP. Não há mocks.
**Nota:** bom

O texto não pedia testes, e o agente escreveu um conjunto bom e com valores:
- **Preço:** a temporada nos limites, como 14 e 15/12 e 29/02 em ano bissexto (`PriceCalculatorTest.java:31-46`). O meio para o par também é conferido, com 37,485 → 37,48 e um comentário de que meio para cima daria 37,49 (`PriceCalculatorTest.java:57-63`). O desconto sobre a alta temporada também está lá (`:67-74`).
- **Situações, devolução e quarto:** as transições de situação e as recusas por código (`ReservationServiceTest.java:53-61`, `100-105`, `125-145`). A devolução por plano e o valor que fica com a pousada (`:109-121`). O quarto ocupado, o dia de saída livre e o quarto liberado pelo cancelamento (`:65-80`).
- **Relatórios:** conferidos por CSV inteiro, inclusive a não reembolsável cancelada (`ReportsTest.java:57-114`).
- **A parte nova do cenário:** a fatura e o booking receipt são comparados pelo texto inteiro (`DocumentsByGuestTest.java:62-71`, `96-103`). Também estão lá o "1 night", o "Discount" e o milhar "BRL 2,783.52" (`:108-115`), além da fatura no e-mail e no check-out (`:75-83`). Pela API, a regra "exatamente uma identificação", com nenhuma, duas e uma incompleta (`HttpApiTest.java:109-127`).

A pergunta da mutação:
- Trocar o 0,05 do ISS, mudar o formato das datas ou do dinheiro em inglês, ou inverter a ordem `isCompany`/`isForeigner` em `confirm`: algum teste falharia.
- Trocar o ramo do estrangeiro em `checkOut` (`ReservationService.java:126-128`) não seria pego: o check-out só é testado para a empresa e só pelo título (`DocumentsByGuestTest.java:83`).
- O comprovante da pessoa com CPF é conferido só por um `contains` (`DocumentsByGuestTest.java:127`). Nenhum teste confere o texto inteiro, nem o "1 diária".
- Nenhum teste lê pela API o JSON do documento (os cinco campos e o `text`).

São lacunas pequenas diante do conjunto.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations` e `messages`), todas com chave primária, criadas por 2 migrações do Flyway. O dinheiro está em `numeric(10,2)` nas 6 colunas, e as datas em `date`. A única chave estrangeira tem índice. As conferências acusaram só as 4 colunas de situação e tipo sem CHECK (`status`, `rate_plan`, `rooms.type` e `messages.channel`). Há 0 transações explícitas e 0 `FOR UPDATE` (de `measures/database.md`).
**Nota:** com problemas

**O que está certo:**
- **Migrações.** O esquema é criado e evoluído por migrações versionadas: a V2 acrescenta as colunas de identificação sem editar a V1 (`V2__guest_identification.sql:2-11`).
- **Restrições.** Os tipos estão certos e há restrições que protegem as regras. A principal é um `EXCLUDE USING gist` sobre `daterange` que impede duas reservas não canceladas na mesma noite mesmo entre processos (`V1__create_tables.sql:38-40`). O código traduz a violação para `ROOM_UNAVAILABLE` (`ReservationStore.java:60-62`). Há também um CHECK de exatamente uma identificação (`V2__guest_identification.sql:8-11`) e um de saída depois da entrada (`V1__create_tables.sql:36`).
- **Código.** O SQL usa parâmetros: os quatro candidatos montados com `+` só concatenam constantes (`ReservationStore.java:18-20`, `34`, `88`, `103`, `116`). As conexões vêm do Hikari e voltam por try-with-resources.

**Os defeitos:**
- **Os relatórios trazem a tabela de reservas inteira para filtrar em Java** (`OccupancyReport.java:25`, `RevenueReport.java:25`, via `ReservationStore.java:113-126`). A tabela cresce sem limite: o relatório de uma noite vai ler todas as reservas da história da pousada.
- **As mudanças de situação só são protegidas dentro do processo.** A proteção é o `synchronized` do serviço (`ReservationService.java:58`, `88`, `103`, `114`). O `UPDATE` grava por `id` sem conferir a situação anterior (`ReservationStore.java:72`). Com duas instâncias, duas confirmações simultâneas passariam as duas e mandariam dois e-mails.
- **A confirmação faz duas escritas sem transação:** grava a situação e o pago (`ReservationService.java:64`) e só depois guarda o e-mail (`:77-80`), cada uma na sua conexão. Uma falha entre as duas deixa a reserva confirmada sem a mensagem.
- **Situações e planos sem CHECK.** `status` e `rate_plan` são `text` sem CHECK (`V1__create_tables.sql:28`, `33`). Um valor fora do enum só explode na leitura, no `valueOf` (`ReservationStore.java:138`, `146`).

Nada disso perde dados no uso normal, mas são pontos que alguém precisa corrigir.

## Comportamento

**Números:** 83 de 83 conferências passaram, e nenhuma falhou (de `measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 05: 32/32;
- duas reservas ao mesmo tempo: 2/2, em 10 rodadas, sempre uma criada e a outra recusada com 409.
**Nota:** bom

O sistema faz o que o dono da pousada descreveu:
- **Preços e arredondamento:** 599,76 no exemplo de julho e 37,48 / 212,42 com meio para o par.
- **Situações, erros e devoluções:** as situações e os códigos de erro estão certos, e as devoluções seguem cada plano.
- **E-mail e relatórios:** o e-mail de confirmação chega com o comprovante, e os relatórios em CSV saem com os totais certos.
- **Dados:** continuam lá depois de reiniciar.
- **A parte nova do cenário:**
  - a empresa recebe a fatura com a razão social no lugar do CPF e o ISS de R$ 38,99, no e-mail (em português), no check-out e na segunda via;
  - o estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, datas `2027-03-10` e "1 night";
  - a reserva recusa com 400 a falta de identificação, duas identificações juntas e uma incompleta.
- **Reservas simultâneas:** duas reservas ao mesmo tempo nunca ocupam o mesmo quarto, graças à restrição do banco.

## Dúvidas

- **A classe do pattern** não deixa dúvida: não há fábrica, nem método polimórfico no hóspede, nem criador. Nenhuma das formas que a referência aceita foi usada; a escolha está copiada nos três lugares.
- **A nota do banco** fica entre `bom` e `com problemas`. A proteção mais importante, contra a reserva dupla, é exemplar. Pesaram os relatórios que leem a tabela inteira, as transições protegidas só no processo e a confirmação sem transação. Cada um sozinho seria uma ressalva.
- **A nota da arquitetura** pesou o ciclo `documents` ↔ `reservations` e a regra de identificação só na borda HTTP. Num sistema deste tamanho, um revisor mais tolerante poderia aceitar os dois.
