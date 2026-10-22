# Revisão: manutenção, cenário 05

## Visão geral

O projeto mantém a estrutura da base: `hotel.web` (Javalin, `HttpApi` lê o JSON e chama o serviço), `hotel.reservations` (o `ReservationService` com as regras e o `ReservationStore` com o JDBC), `hotel.documents` (o `ReservationReceipt` e o `TextFormat`), `hotel.messages`, `hotel.reports` e `hotel.rooms`. Os dados ficam no PostgreSQL, com o esquema criado pelo Flyway. Para o pedido, o agente pôs quatro campos novos no `Guest`, que deduz o tipo de hóspede (`GuestType`) pelo que veio nulo. Criou a migração `V2` com quatro colunas e tirou o `NOT NULL` do CPF. A escolha do documento ficou concentrada no construtor do `ReservationReceipt`, com um `switch` sobre o tipo, e os três lugares que emitem o documento continuam chamando esse construtor. O roteiro passou inteiro (83 de 83), e os 34 testes passam. O mais sério está na validação de "um jeito, e só um". O `HttpApi` conta só as identificações completas, e o `Guest.type()` decide o tipo por `!= null`. Por isso a API aceita combinações que o texto manda recusar, e em alguns casos emite o documento errado. Eu conferi pela API: um estrangeiro que manda passaporte e país, mais um CNPJ solto, recebe uma "Fatura de hospedagem" com "Empresa: null". Os testes novos conferem rótulos, mas não os valores (nem o ISS nem o formato `BRL 1,234.56`). O README não foi atualizado com os campos novos.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). Nas linhas, `hotel.documents` passou de 79 para 231, `hotel.web` de 236 para 273 e `hotel.reservations` de 508 para 545.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo. Ele reforçou o ciclo da base: `ReservationReceipt` agora também importa `hotel.reservations.GuestType` (`src/main/java/hotel/documents/ReservationReceipt.java:3`), mas a aresta `documents -> reservations` já existia. A borda HTTP continua fina. O JDBC continua só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-55` e `:129-138`), e as regras não conhecem o Javalin. A montagem continua em `Hotel.java`, sem mudança.

Há uma ressalva pequena. A regra "exatamente uma identificação" mora só na borda HTTP (`src/main/java/hotel/web/HttpApi.java:127-143`), e nem o domínio (`Guest`) nem o banco a garantem. A base já validava os campos obrigatórios ali, então o agente seguiu o padrão que encontrou. O efeito disso aparece no design e no comportamento. `ReservationReceipt` cresceu de 55 para 192 linhas e virou a segunda maior classe, mas por causa dos três textos, não por mistura de camadas.

## Design de baixo nível

**Números:** há 3 documentos (comprovante, fatura e booking receipt), todos escolhidos num só lugar, o construtor de `ReservationReceipt`. Os 3 lugares que emitem o documento continuam chamando esse construtor e nenhum pergunta o tipo: `ReservationService.java:64`, `:109` e `:118`. A decisão aparece em dois `switch` sobre o mesmo `GuestType` na mesma classe (`ReservationReceipt.java:67-73` e `:92-102`). O tipo é deduzido por nulos em `Guest.java:10-18`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede três documentos que saem em três lugares, e avisa que as agências terão um quarto documento. A escolha de qual documento criar ficou concentrada. O construtor `ReservationReceipt(Reservation, Room)` (`src/main/java/hotel/documents/ReservationReceipt.java:30-65`) decide pelo tipo de hóspede, e `confirm`, `checkOut` e `receiptCopy` só chamam esse construtor (`src/main/java/hotel/reservations/ReservationService.java:64`, `:109`, `:118`). O caso que já existia (o comprovante com CPF) também foi para a estrutura (`ReservationReceipt.java:105-130`). A referência aceita como acerto essa forma de "fábrica concentrada" (um método que escolhe o documento pelo tipo). O que afasta do ideal:

- a decisão está repartida em dois `switch` paralelos, `titleFor` (`:67-73`) e `generateText` (`:92-102`). O título e o texto de um mesmo documento não ficam juntos, e a agência vai exigir mexer nos dois. Como os `switch` são exaustivos sobre um `enum`, o compilador avisa se faltar um caso;
- o documento é um só `record` com todos os campos de todos os tipos. Cada tipo novo acrescenta campos ao `Guest`, ao `ReservationReceipt`, ao `ReservationResponse` e ao `ReservationStore`.

**O exagero.** Não há estrutura sem necessidade. O agente criou só o `enum GuestType` (`src/main/java/hotel/reservations/GuestType.java`), que é usado.

**O resto do código:**

- `generateText` recebe 16 parâmetros (`ReservationReceipt.java:75-91`), e o construtor extrai de novo, à mão, todos os campos que acabou de passar ao `this(...)` (`:48-64` repete `:32-47`). Bastava passar a `Reservation` e o `Room`, ou usar os campos do próprio `record`;
- `generateIndividualReceipt` e `generateCompanyInvoice` repetem, linha por linha, o quarto, as datas, as diárias, o desconto e o total (`:120-128` e `:149-157`);
- o ISS usa um número mágico (`new BigDecimal("0.05")`) e repete o arredondamento com `java.math.RoundingMode` escrito por extenso (`:158`), em vez de uma constante e do `PriceCalculator.cents`, que já faz isso (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`). O resultado (R$ 38,99) está certo;
- há código morto na validação. `hasCompany` já exige a razão social e `hasPassport` já exige o país (`src/main/java/hotel/web/HttpApi.java:128-129`), então os testes de `:137-139` e `:141-143` nunca disparam;
- o `Guest` virou um `record` com sete campos de texto que podem ser nulos (`src/main/java/hotel/reservations/Guest.java:3-10`), em vez de um tipo para cada identificação. O `type()` decide por `!= null` (`:11-18`), mas a validação decide por `isBlank` (`HttpApi.java:127-129`). Essa diferença é a origem dos erros descritos em Comportamento: um `guestCpf: ""` vira `INDIVIDUAL`, e um `companyCnpj` solto ao lado de um passaporte completo vira `COMPANY`.

## Testes

**Números:** 34 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 34 métodos escritos (a base tinha 26). A cobertura é de 94,4% das linhas (574 de 608) e 87,3% dos ramos (138 de 158); na base, era de 93,8% e 87,1%. Em `hotel.documents`, os testes cobrem 94 de 96 linhas e 15 de 18 ramos.
**Nota:** com problemas

Os 26 testes da base continuam lá e passam. O agente só acrescentou os quatro `null` aos construtores de `Guest` (`src/test/java/hotel/reservations/ReservationServiceTest.java:34`, `src/test/java/hotel/reports/ReportsTest.java:31`). Ele escreveu 8 testes novos: 3 no serviço (`ReservationServiceTest.java:164`, `:183`, `:203`) e 5 pela API (`src/test/java/hotel/web/HttpApiTest.java:124`, `:147`, `:171`, `:184`, `:196`). Eles cobrem os três tipos, as recusas (nenhuma identificação, CPF e CNPJ juntos, CNPJ sem razão social) e o "1 night".

As asserções da parte nova, porém, conferem rótulos e não valores:

- o ISS só é conferido pelo rótulo, `contains("ISS retido (5%):")` (`ReservationServiceTest.java:179`, `HttpApiTest.java:143`). Se alguém trocasse `0.05` por `0.5`, ou mudasse o arredondamento, nenhum teste falharia. O valor que o texto dá (R$ 38,99) não aparece em teste nenhum;
- no booking receipt, o dinheiro só é conferido por `contains("BRL")` (`ReservationServiceTest.java:199`, `HttpApiTest.java:167`). Se as vírgulas e os pontos de `TextFormat.usMoney` fossem trocados, nenhum teste veria;
- ficaram sem teste a linha `Discount:` do booking receipt, o milhar, o documento novo no e-mail de confirmação e no check-out (os testes novos só olham a segunda via), o passaporte sem país e as combinações mistas (um jeito completo mais um pedaço de outro), que são justamente as que estão erradas;
- `foreignGuestWithSingleNightUsesCorrectPlural` (`ReservationServiceTest.java:212`) confere `contains("1 night")`, que também passaria com "1 nights".

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, e não há chave estrangeira sem índice. Foram aplicadas 2 migrações: a `V1` está igual à da base, e a `V2__add_guest_identification_types.sql` é nova. As colunas novas são `text` e não têm `CHECK`. O SQL usa parâmetros: os candidatos montados com `+` só concatenam constantes.
**Nota:** com problemas

O esquema evoluiu do jeito certo, com uma migração nova e sem editar a `V1`, o que preserva os bancos que já estão rodando. O `INSERT` e a leitura acompanham as colunas novas, com parâmetros `?` (`src/main/java/hotel/reservations/ReservationStore.java:32-55`, `:129-138`).

O defeito está no que o banco deixou de proteger. A `V2` tira o `NOT NULL` de `guest_cpf` (`src/main/resources/db/migration/V2__add_guest_identification_types.sql:2`) e não põe nada no lugar. Na base, toda reserva tinha identificação; agora o banco aceita uma reserva sem nenhuma, ou com duas. Faltou um `CHECK`, por exemplo `num_nonnulls(guest_cpf, company_cnpj, guest_passport) = 1`, com o CNPJ amarrado à razão social e o passaporte ao país. Isso já gravou dado incoerente. No teste manual, a reserva 2 ficou com CNPJ e passaporte ao mesmo tempo, e a reserva 3 ficou com `guest_cpf = ''` ao lado de um CNPJ. O tipo de hóspede também não é guardado: ele é deduzido dos nulos a cada leitura (`Guest.java:11-18`), e por isso esses dados ruins mudam o documento que sai. Os demais pontos vêm da base e o agente não os piorou: `status` e `rate_plan` em `text` sem `CHECK`, e o `all()`, que traz a tabela inteira (`ReservationStore.java:114-117`).

## Comportamento

**Números:** 83 de 83 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 32 de 32 no cenário 05 e 2 de 2 nas reservas simultâneas.
**Nota:** com problemas

Pelo roteiro, tudo funciona. A pessoa com CPF continua recebendo o mesmo comprovante. A fatura mostra a empresa, o hóspede e o ISS de R$ 38,99. O booking receipt sai em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha `Discount`. O documento é o mesmo no e-mail (que continua em português), no check-out e na segunda via. As recusas do roteiro dão 400.

O texto, porém, diz que tem de ser "um desses jeitos, e só um; se não for, a resposta é 400". Chamei a API à mão (serviço de pé, banco vazio) e encontrei três casos que deviam dar 400 e foram aceitos:

- CPF mais um CNPJ sem razão social: criada com 201, com `guestCpf` e `companyCnpj` preenchidos. O `HttpApi` só conta a empresa completa (`src/main/java/hotel/web/HttpApi.java:128-132`), e o CNPJ solto é gravado assim mesmo;
- passaporte e país mais um CNPJ solto: criada com 201. Na segunda via, o estrangeiro recebe `"title":"Fatura de hospedagem"` com o texto `Empresa: null (CNPJ 12.345.678/0001-90)`, porque `Guest.type()` olha o CNPJ antes do passaporte (`src/main/java/hotel/reservations/Guest.java:12-15`);
- `"guestCpf": ""` mais a empresa completa: criada com 201. A empresa recebe o comprovante pessoal, com `Hóspede: Ana (CPF )` e sem a fatura nem o ISS, porque `""` não é nulo (`Guest.java:12`).

Para o dono da pousada: se o site mandar um campo a mais ou vazio, a reserva entra mesmo assim, e o hóspede pode receber o documento de outro tipo, com "null" no lugar da razão social. Quando é uma empresa, a fatura com o ISS retido simplesmente não sai. O caso normal funciona; o defeito aparece com pedidos mal formados que o texto mandava recusar.

## Dúvidas

- Classifiquei o pattern como **acerto**, na forma de "fábrica concentrada": um construtor de `record` que escolhe o texto e o título por `switch` sobre `GuestType`, e não um criador com o método sobrescrito nem um método polimórfico no hóspede. Pesa contra o fato de a escolha estar em dois `switch` paralelos e de o produto ser sempre a mesma classe com campos opcionais. Pesa a favor o fato de que nenhum dos três lugares pergunta o tipo e de que um tipo novo exige mudar uma classe só para a escolha.
- Os três casos de combinação mista não estão no roteiro. Pesei-os no comportamento porque o texto descreve essa regra com todas as letras ("só um; se não for, 400") e porque um deles troca o documento entregue. Se a leitura for só pelo roteiro, a nota seria `bom`.
- No banco, dei `com problemas` pela falta de uma restrição que a base tinha (o `NOT NULL` do CPF) e que não foi substituída. O texto não pede restrição no banco, então isso depende de quanto se espera que o banco proteja a regra nova.
