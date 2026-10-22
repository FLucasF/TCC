# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web.HttpApi` lê a requisição e chama `hotel.reservations.ReservationService`. O serviço guarda os dados no PostgreSQL por `ReservationStore` (JDBC, com o esquema criado pelo Flyway) e monta o comprovante (`hotel.documents.ReservationReceipt`) em três lugares: `confirm`, `checkOut` e `receiptCopy` (`ReservationService.java:64,109,118`). O agente não criou nenhuma classe. Acrescentou quatro campos que aceitam null em `Guest`, `CreateReservationRequest`, `ReservationResponse` e `ReservationReceipt`, uma migração nova (`V2__add_identification_types.sql`), a validação de "um jeito só" em `HttpApi.java:127-152` e, dentro do próprio record `ReservationReceipt`, dois `if/else if` sobre os campos do hóspede: um escolhe o título (`ReservationReceipt.java:51-59`) e o outro escolhe o texto (`ReservationReceipt.java:61-111`). O `mvn verify` passa (29 testes) e o roteiro passa inteiro (83 de 83). O mais sério é a variação, que ficou em `if` em vez de uma estrutura de criação. Há também dois defeitos que achei chamando a API à mão. Uma reserva com CPF que manda `"companyCnpj": ""` recebe uma "Fatura de hospedagem" com a empresa vazia. E a fatura de uma reserva com desconto não mostra a linha do desconto.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, nos dois lados. Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que é igual na base (`measures/dependencies-base.md`) e no final (`measures/dependencies.md`). As maiores classes: `HttpApi` passou de 170 para 204 linhas e `ReservationReceipt` de 55 para 200.
**Nota:** bom

O agente não mexeu na forma do sistema. As dependências e o ciclo são exatamente os da base: o ciclo vem de `ReservationService` montar o `ReservationReceipt` (`ReservationService.java:3,64`) e de o recibo ler `Reservation`. As camadas continuam no lugar:
- o SQL das colunas novas ficou em `ReservationStore.java:32-55,129-137`;
- as regras continuam sem Javalin e sem JDBC (veja a tabela de bibliotecas);
- a montagem continua em `hotel.Hotel`.

A validação de "exatamente um jeito de identificar" ficou na borda (`HttpApi.java:127-152`), junto da validação de campos obrigatórios que a base já fazia ali (`HttpApi.java:116-125`). É uma validação de requisição, aceitável nesse lugar. Fica um ponto de atenção: `ReservationReceipt` quadruplicou, e é ali que a próxima variação vai doer. Isso é design de baixo nível, não camada.

## Design de baixo nível

**Números:** nenhum tipo de documento e nenhum tipo de hóspede. A escolha entre os 3 documentos está em 2 cadeias `if/else if` iguais: `titleFor`, em `ReservationReceipt.java:51-59` e chamada 4 vezes, e `generateText`, em `ReservationReceipt.java:67-110`. São 3 métodos de texto com 12 ou 13 parâmetros cada (`ReservationReceipt.java:113,141,171`). Os 3 lugares que emitem o documento continuam com o mesmo `new ReservationReceipt(reservation, room)` da base.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve três documentos que saem nos mesmos três lugares e anuncia um quarto, o da agência. Pede uma criação concentrada que escolha o documento pelo tipo de hóspede. O que aproxima: os três lugares do `ReservationService` continuam chamando um ponto só (`ReservationService.java:64,109,118`). Mas isso já era assim na base, e o agente não fez nada para concentrar a criação. O que afasta:
- O documento continua um record único, com os campos de todos os tipos (`ReservationReceipt.java:11-28`).
- O "tipo de hóspede" não existe como tipo. Ele é deduzido de qual string é diferente de null (`ReservationReceipt.java:52,54,67,81`), e a mesma pergunta é feita duas vezes, em `titleFor` e em `generateText`.
- Para a agência de turismo, alguém teria de mexer nas duas cadeias de `if` e acrescentar mais campos que aceitam null em `Guest` (`Guest.java:3-10`), no record e na tabela.

A variação ficou num `if` com uma cópia por caso. Não é uma fábrica que devolve documentos diferentes, nem um método polimórfico no hóspede.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova.

**O resto do código:**
- **Duplicação.** As linhas de quarto, datas e diárias estão copiadas em `receiptTextFor` e `companyInvoiceTextFor` (`ReservationReceipt.java:129-133` e `161-165`).
- **Métodos longos.** `generateText` (`ReservationReceipt.java:61-111`) desmonta a reserva em 12 ou 13 argumentos soltos, em vez de passar `Reservation` e `Room`.
- **Constante solta.** O ISS de 5% é o literal `BigDecimal.valueOf(0.05)` (`ReservationReceipt.java:154`). Já `PriceCalculator.java:16-17` guarda as taxas em constantes com `new BigDecimal("…")`.
- **Regra decidida com dois critérios.** A validação olha `isBlank` (`HttpApi.java:127-129`), mas a escolha do documento olha `!= null` (`ReservationReceipt.java:52`). Por isso uma string vazia passa como "não informado" na entrada e como "empresa" na saída (veja Comportamento).
- **Campos vazios gravados como vieram.** `Guest` guarda os campos vazios sem normalizar para null (`HttpApi.java:154-161`).
- **Documentação apagada.** O agente removeu o javadoc de `CreateReservationRequest.java:6` em vez de corrigi-lo.

O arredondamento do ISS em `HALF_EVEN` (`ReservationReceipt.java:155`) segue o da base (`PriceCalculator.java:51`), como o pedido manda ("como sempre").

## Testes

**Números:** 29 testes rodaram, nenhum falhou e nenhum foi pulado, com 29 métodos. Na base eram 26 testes, e os 26 continuam iguais. A cobertura é de 94,5% das linhas (587/621) e 86,0% dos ramos (141/164); na base, 93,8% e 87,1%. `hotel.documents` tem 113 de 114 linhas e 15 de 18 ramos cobertos.
**Nota:** com problemas

Os três testes novos estão todos em `HttpApiTest.java`, e todos passam pela API:
- `createsReservationWithCompanyCnpj` (`HttpApiTest.java:124`);
- `createsReservationWithPassport` (`HttpApiTest.java:152`);
- `validatesIdentificationFields` (`HttpApiTest.java:181`).

Eles executam quase todo o código novo, mas conferem pouco com valores:
- O ISS só é conferido pelo rótulo, `contains("ISS retido (5%)")` (`HttpApiTest.java:148`). Se alguém trocar `0.05` por `0.50`, nenhum teste falha.
- O booking receipt só confere que há `"BRL"` e `"2027-03-10"` (`HttpApiTest.java:176-177`). Não confere `BRL 779.80`, a vírgula do milhar, `1 night` nem a linha `Discount:`. Apagar o ramo `nights == 1` ou a linha do desconto (`ReservationReceipt.java:192,194-196`) passa sem ser visto.
- Faltam três casos: o documento dentro do e-mail de confirmação, as recusas de par incompleto (CNPJ sem razão social, passaporte sem país, em `HttpApi.java:144-152`) e a segunda via.
- Não há teste de unidade dos documentos.

Os testes da base continuam passando e não foram enfraquecidos. O `ReservationServiceTest` é idêntico ao da base.

## Banco

**Números:** 3 tabelas. Há 2 migrações do Flyway: `V1` está igual à da base e `V2__add_identification_types.sql` é nova. Dinheiro em `numeric(10,2)`, chave primária em todas as tabelas, a chave estrangeira com índice e o `EXCLUDE` contra reserva dupla, todos mantidos. O script não acusou nada novo. Colunas de situação sem `CHECK` (`status`, `rate_plan`, `type`, `channel`) já eram assim na base.
**Nota:** com problemas

A evolução está certa. O agente acrescentou uma migração nova sem editar a `V1` (`V2__add_identification_types.sql:1-6`), e o SQL novo usa parâmetros (`ReservationStore.java:32-55`) e lê as colunas novas (`ReservationStore.java:130-137`).

Há um problema. A migração tira o `NOT NULL` de `guest_cpf` (`V2__add_identification_types.sql:2`) e não põe nada no lugar. Na base, o banco garantia que toda reserva tinha identificação; agora, uma linha sem CPF, sem CNPJ e sem passaporte, ou com os três, entra sem reclamação. Um `CHECK` de "exatamente um" (CPF; ou CNPJ e razão social; ou passaporte e país) protegeria a regra no banco. Hoje ela só existe em `HttpApi.java:131-152`. Por causa disso, valores vazios ficam gravados como `''` em vez de null: o teste à mão gravou `company_cnpj = ''` numa reserva com CPF.

## Comportamento

**Números:** 83 de 83 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- reservas simultâneas: 2 de 2.
**Nota:** com problemas

O roteiro passou inteiro, inclusive os valores do texto:
- a fatura com `ISS retido (5%): R$ 38,99`;
- o booking receipt com `BRL 779.80`, `BRL 2,783.52`, `1 night` e `Discount:`;
- o e-mail em português com o documento certo;
- as quatro recusas com 400.

Chamei a API à mão (`curl`, num banco vazio) e achei dois defeitos que o roteiro não cobre:
1. **Documento errado.** Uma reserva com `guestCpf` e `"companyCnpj": "", "companyName": ""` é aceita (201). A segunda via (`GET /reservations/1/document`) volta como "Fatura de hospedagem", com a linha `Empresa:  (CNPJ )` e ISS retido, em vez do comprovante com o CPF. Para o dono, isso quer dizer: se o site mandar os campos que não se aplicam como texto vazio em vez de omiti-los, a pessoa física recebe uma fatura de empresa em branco, por e-mail, no check-out e na segunda via. A causa é a diferença entre `HttpApi.java:127-129` (`isBlank`) e `ReservationReceipt.java:52,54` (`!= null`).
2. **Fatura sem desconto.** Uma reserva de empresa em `NON_REFUNDABLE` sai com `Diárias: R$ 779,80` e `Total: R$ 662,83`, sem a linha do desconto (`ReservationReceipt.java:165-166`). O texto diz que a fatura "é parecida com o comprovante", e o comprovante mostra o desconto (`ReservationReceipt.java:134-136`). A empresa recebe uma fatura cujas contas não fecham.

## Dúvidas

- **A classe do pattern.** Pode-se ler o construtor `ReservationReceipt(Reservation, Room)` como uma "fábrica concentrada" (um método que escolhe o documento pelo tipo), porque os três lugares pedem o documento a um ponto só. Não dei `acerto` por três motivos: esse ponto único já existia na base, a escolha está duplicada em duas cadeias de `if`, e não há documentos distintos, só um record com todos os campos e o texto escolhido por `if`.
- **O peso dos dois defeitos de comportamento.** O texto não diz se o site manda os campos que não se aplicam ausentes, null ou vazios. O texto também não dá um exemplo de fatura com desconto. Os dois defeitos contam como parte do cenário que não funciona, mas são casos de borda que o roteiro não exercita.
