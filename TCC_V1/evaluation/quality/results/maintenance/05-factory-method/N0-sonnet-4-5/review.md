# Revisão: manutenção, cenário 05

## Visão geral

A base já tinha a forma que o agente manteve: sete pacotes (`hotel` monta tudo, `hotel.web` com as rotas do Javalin, `hotel.reservations` com o serviço, o cálculo de preço e o acesso ao banco em JDBC, `hotel.documents` com o comprovante, mais `rooms`, `messages` e `reports`). Uma requisição entra em `HttpApi`, que lê o JSON e chama o `ReservationService`. O serviço grava pelo `ReservationStore` no PostgreSQL, cujo esquema é criado por migrações do Flyway. O agente acrescentou quatro campos de identificação ao `Guest` e às colunas (migração nova `V2`), e criou uma fábrica concentrada, `ReservationDocument.from`, que escolhe entre o comprovante, a fatura de hospedagem e o *booking receipt*. Os três lugares que emitem documento usam essa fábrica. O roteiro do comportamento passou inteiro (83 de 83), e os 29 testes passam. Não há defeito sério. O que pesa é de design e de testes:
- a fábrica copia à mão, três vezes, os 17 campos de cada documento para um registro achatado;
- o tipo de hóspede é deduzido de campos que podem ser nulos, e essa decisão também aparece na borda HTTP;
- os testes novos conferem só trechos do texto e não os valores (o ISS, o formato `BRL 1,234.56`, "1 night").

O README não foi atualizado com o novo corpo de `POST /reservations` (`final/README.md` é igual ao da base).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e `measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. Nenhuma dependência nova. O Javalin aparece só em `hotel` e `hotel.web`. A maior classe é `HttpApi`, com 191 linhas (eram 170).
**Nota:** bom

A mudança respeitou as camadas da base. A borda continua fina: as rotas só chamam o serviço (`src/main/java/hotel/web/HttpApi.java:64-79`). A regra de qual documento emitir ficou em `hotel.documents` (`src/main/java/hotel/documents/ReservationDocument.java:28-90`), sem Javalin nem SQL. O SQL novo ficou só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-55` e `:129-137`). O ciclo `documents` ↔ `reservations` vem da base: o documento lê `Reservation`, e o serviço monta o documento. O agente não o piorou, e a dependência nova `ReservationService` → `ReservationDocument` segue o mesmo sentido. A conferência de "um jeito só de identificar o hóspede" ficou na borda (`src/main/java/hotel/web/HttpApi.java:127-147`). É validação de entrada, no mesmo lugar onde a base já conferia os campos obrigatórios, então não chega a misturar camadas. Ela só não protege quem criar um `Guest` por outro caminho; o CHECK do banco cobre essa parte.

## Design de baixo nível

**Números:** 3 tipos de documento (o comprovante da base mais os 2 novos), todos escolhidos num único lugar (`ReservationDocument.from`). Os 3 lugares que emitem documento passam pela fábrica: `ReservationService.java:65` (e-mail), `:110` (check-out) e `:119` (segunda via). Nenhum lugar cria o documento à mão. A escolha do tipo de hóspede é feita em 2 lugares (`HttpApi` e a fábrica).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É a forma "fábrica concentrada" que a referência aceita. `ReservationDocument.from(reservation, room)` decide pelo tipo de hóspede qual documento montar (`src/main/java/hotel/documents/ReservationDocument.java:29`, `:49`, `:69`). O caso que já existia (o `ReservationReceipt` da base, intacto) também passa por ela. Os três `new ReservationReceipt` que a base tinha no `ReservationService` viraram chamadas à fábrica (`src/main/java/hotel/reservations/ReservationService.java:65`, `:110`, `:119`). Uma agência de turismo pediria um ramo novo num único lugar.

O que afasta da forma ideal:
- **Não há um tipo comum entre os documentos.** `ReservationReceipt`, `HostingInvoice` e `InternationalReceipt` são três records sem relação entre si. A fábrica devolve um quarto record, achatado, e copia campo a campo cada um dos três para ele: são 17 argumentos repetidos três vezes, quase iguais (`ReservationDocument.java:31-48`, `:51-68`, `:71-88`). Os três records repetem também o mesmo construtor de 12 a 13 campos (`HostingInvoice.java:25-41`, `InternationalReceipt.java:26-42`). Uma agência acrescentaria mais um record, mais um bloco de cópia e mais colunas nulas no `ReservationDocument`.
- **O tipo de hóspede não é um tipo.** O `Guest` virou sete campos, quatro deles anuláveis, com predicados `isIndividual`/`isCompany`/`isForeign` (`src/main/java/hotel/reservations/Guest.java:3-26`). O `else` da fábrica supõe que é estrangeiro sem perguntar (`ReservationDocument.java:69`), e por isso `isForeign` não é usado em lugar nenhum (`Guest.java:24`). A mesma decisão de "qual dos três jeitos" é tomada de novo, com outro `if`, em `HttpApi.java:138-147`. Um `enum` ou um tipo selado deixaria isso explícito.

Sobre o exagero: nenhuma interface com uma implementação só, nenhuma camada que só repassa. Os records novos são os próprios documentos que o pedido descreve.

**O resto do código.** O ISS usa um número mágico dentro de `text()` e repete o arredondamento que a base já centralizava em `PriceCalculator.cents` (`src/main/java/hotel/documents/HostingInvoice.java:57-58`). O valor sai certo, mas a regra "centavos, meio para o par" agora mora em dois lugares. Sobrou um `import hotel.documents.ReservationReceipt` sem uso (`src/main/java/hotel/reservations/ReservationService.java:4`). Fora isso, o código segue o estilo da base: `BigDecimal` para dinheiro, `LocalDate` para datas, e o formato inglês isolado em métodos privados (`InternationalReceipt.java:61-67`).

## Testes

**Números:** 29 testes rodaram, 0 falharam, 0 pulados, 29 métodos escritos (a base tinha 26). Cobertura de linhas: 611 de 645 (94,7%; na base, 93,8%). Cobertura de ramos: 133 de 154 (86,4%; na base, 87,1%). Em `hotel.documents`: 140 de 142 linhas e 12 de 16 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base passando. A única mudança neles foi o tipo de retorno do check-out (`src/test/java/hotel/reservations/ReservationServiceTest.java:141-145`), sem enfraquecer nada. Ele acrescentou três testes pela API:
- `createsReservationForCompanyWithCnpj`, em `src/test/java/hotel/web/HttpApiTest.java:124`;
- `createsReservationForForeignGuestWithPassport`, em `:152`;
- `requiresExactlyOneIdentificationType`, em `:182`.

Os três testam o que mudou: os campos null na consulta, o título do documento e as recusas com 400 `INVALID_REQUEST` (nenhum jeito, CPF com CNPJ, CNPJ sem a razão social).

As asserções do texto, porém, são `contains` de trechos, e os valores pedidos ficam sem conferência:
- **O ISS.** O teste só confere o rótulo `"ISS retido (5%)"` (`HttpApiTest.java:148`). Se alguém trocasse `0.05` por `0.10` em `HostingInvoice.java:57`, nenhum teste falharia. O R$ 38,99 do pedido não aparece em teste nenhum.
- **O dinheiro em inglês.** O teste confere só o prefixo `"BRL "` (`HttpApiTest.java:178`). Trocar `Locale.US` pelo português em `InternationalReceipt.java:66` passaria.
- **Nada coberto por teste:** "1 night", a linha "Discount:", o documento dentro do e-mail para empresa e para estrangeiro, e a segunda via dos tipos novos.
- **Sem o caso "passaporte sem país".**

Os testes passam e cobrem bem as linhas, mas a parte nova do cenário fica, em boa parte, sem conferência de valor.

## Banco

**Números:** 3 tabelas, todas com chave primária. Flyway com 2 migrações: a `V1__create_tables.sql` igual à da base e a `V2__add_guest_identification_types.sql` nova. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. As conferências não acusaram nada de novo. As colunas `status`/`rate_plan`/`type`/`channel` sem CHECK vêm da base (`measures/database.md`).
**Nota:** bom

A mudança no esquema é uma migração nova, sem editar a da base. Ela solta o `NOT NULL` do `guest_cpf`, acrescenta as quatro colunas e protege a regra "um jeito só" com um CHECK (`src/main/resources/db/migration/V2__add_guest_identification_types.sql:3-18`). As reservas que já existem têm CPF e passam no CHECK, então a migração sobe sobre um banco com dados. O SQL continua com parâmetros: o `+` em `ReservationStore.java:35`, `:89`, `:104` e `:117` só junta constantes. As conexões continuam em try-with-resources com o pool da base. O EXCLUDE contra duas reservas na mesma noite continua lá, vindo da base.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- a pessoa com CPF recebe o mesmo comprovante de antes;
- a empresa recebe a fatura com a razão social, o CNPJ e o ISS de R$ 38,99;
- o estrangeiro recebe o *booking receipt* com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount";
- o e-mail continua em português, com o documento certo dentro;
- o check-out e a segunda via devolvem o mesmo documento, com os campos que não se aplicam em null e o campo `text`;
- as quatro recusas (nenhum jeito, CPF com CNPJ, CNPJ sem razão social, passaporte sem país) dão 400 `INVALID_REQUEST`.

Fora do roteiro, pela leitura de `HttpApi.java:127-136`: um pedido com CPF e só o CNPJ, sem a razão social, conta como "um jeito só". A reserva é aceita como pessoa física e o CNPJ é descartado em silêncio. O pedido diz "tem que ser um desses jeitos, e só um", e esse caso misto é um ponto que o texto não fecha. Não muda a nota.

## Dúvidas

- A forma do pattern é a da fábrica concentrada (um método estático, `ReservationDocument.from`, que escolhe pelo tipo de hóspede), não a clássica com criadores sobrescritos nem um método polimórfico no `Guest`. Classifiquei como acerto, porque a escolha está num lugar só e os três pontos de emissão a usam. A falta de um tipo comum entre os documentos e a cópia em três blocos pesaram na nota, não na classe.
- O caso "CPF junto com um CNPJ sem razão social", aceito como CPF, foi lido no código e não testado pela API. O texto não diz se esse pedido deve ser recusado.
