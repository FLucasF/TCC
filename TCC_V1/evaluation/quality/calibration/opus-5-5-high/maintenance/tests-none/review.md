# Revisão: manutenção, cenário 05

## Visão geral

O projeto continua com o desenho da base: `hotel.web` (`HttpApi`, Javalin) lê a requisição e chama o
`ReservationService` (`hotel.reservations`). O serviço aplica as regras e usa o `ReservationStore` (JDBC com `?`)
para ler e gravar no PostgreSQL. O esquema é criado pelo Flyway. Para atender o pedido, o agente fez quatro coisas.
Criou em `hotel.documents` uma fábrica concentrada (`DocumentFactory`), que escolhe pelo tipo de hóspede um de três
`DocumentWriter`. Trocou os três `new ReservationReceipt(...)` do serviço por `documents.issue(...)`. Acrescentou ao
`Guest` os quatro campos novos. Escreveu uma migração nova, `V2__guest_identification.sql`, com um CHECK de "um jeito
só". O comportamento está certo: 83 de 83 conferências passaram. O problema mais sério está nos testes. O agente
apagou os quatro arquivos de teste da base (26 testes, 476 linhas) e pôs no lugar um único teste que só confere se
`GET /rooms` responde 200. A cobertura de linhas caiu de 93,8% para 18,9%, e a parte nova ficou sem nenhum teste.
Esses 26 testes ainda passam sem mudança contra o código final: eu os copiei para uma cópia da `final` em /tmp e o
resultado foi 26 de 26, sem falhas. Ou seja, nada obrigava a apagá-los. O README também não foi atualizado: ainda
mostra o `guestCpf` como o único jeito de identificar o hóspede.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base, os
números eram os mesmos: 7 pacotes, 17 dependências e o mesmo ciclo. A maior classe é `HttpApi`, com 184 linhas (170
na base).
**Nota:** bom

O agente não criou nenhuma dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` veio da base: lá,
`ReservationService` já criava `ReservationReceipt`, e `ReservationReceipt` já lia `Reservation`. O código novo de
documentos fica em `hotel.documents` e não depende de Javalin nem de JDBC (`measures/dependencies.md`, tabela de
bibliotecas). O serviço só pede o documento à fábrica (`src/main/java/hotel/reservations/ReservationService.java:66`,
`:111` e `:120`). Os campos novos do banco ficam todos no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:32-54` e `:128-131`). A borda HTTP continua fina. A regra
"exatamente um jeito de identificar" ficou na validação do corpo
(`src/main/java/hotel/web/HttpApi.java:126-134`), junto da validação de campos obrigatórios que a base já fazia ali.
Não é regra de negócio misturada à rota, e o banco reforça a mesma regra com um CHECK. Há um ponto pequeno: a
`DocumentFactory` é criada com `new` dentro do serviço (`ReservationService.java:19`), e não na montagem
(`src/main/java/hotel/Hotel.java:20-28`). Como não há variação de implementação, isso não pesa na nota.

## Design de baixo nível

**Números:** dos 3 tipos de documento, os 3 estão na estrutura, inclusive o comprovante da base, que virou
`PersonReceiptWriter`. Os 3 lugares que emitem o documento (confirmação, check-out e segunda via) passam pela fábrica,
e nenhum cria o documento à mão. A decisão sobre o tipo de hóspede aparece num lugar só
(`DocumentFactory.java:19-27`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada, uma das formas que a referência aceita como acerto.
`DocumentFactory.issue` (`src/main/java/hotel/documents/DocumentFactory.java:14-17`) escolhe o `DocumentWriter` pelo
hóspede (`:19-27`) e monta o `ReservationReceipt` com o título e o texto daquele tipo. O comprovante que existia na
base saiu de `ReservationReceipt.text()` e virou `PersonReceiptWriter`
(`src/main/java/hotel/documents/PersonReceiptWriter.java:15-31`), com o mesmo texto. Assim, o caso antigo também está
na estrutura. Os três pontos de uso não perguntam qual é o tipo de hóspede (`ReservationService.java:66`, `:111`,
`:120`). O documento das agências vai precisar de um writer novo e de um ramo novo em `writerFor`, só nesse lugar.

**O exagero.** Não há. A interface `DocumentWriter` (`src/main/java/hotel/documents/DocumentWriter.java:7-11`) tem
três implementações, e elas são a própria variação que o pattern resolve. Fora isso, o agente não criou nenhuma
camada, builder ou interface.

**O resto do código.**
- **Duplicação.** O texto em português está copiado: `PersonReceiptWriter.java:20-29` e
  `CompanyInvoiceWriter.java:23-32` têm as mesmas dez linhas (quarto, entrada/saída com "diária(s)", diárias,
  desconto, total). O `ForeignReceiptWriter.java:23-32` repete o mesmo esqueleto em inglês. Uma mudança no
  comprovante, ou um quarto documento em português (o das agências), vai precisar de mais uma cópia.
- **O ISS.** O cálculo do ISS (`CompanyInvoiceWriter.java:33-34`) põe a taxa (`0.05`) e o arredondamento direto no
  texto da fatura. O cálculo está certo (meio para o par, como o resto), mas repete à mão o `cents` que o
  `PriceCalculator` já tem (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`).
- **O tipo de hóspede.** O tipo fica implícito na nulidade dos campos (`src/main/java/hotel/reservations/Guest.java:20-26`).
  "Pessoa" é o que sobra no `writerFor` (`DocumentFactory.java:26`), e um `Guest` sem identificação sairia como
  comprovante com "CPF null". O `Guest` também ficou com sete campos e quatro deles anuláveis. Isso só não vira
  problema porque a entrada HTTP (`HttpApi.java:126-134`) e o CHECK do banco seguram a regra.

São defeitos localizados, e o restante do código, que veio da base, está limpo.

## Testes

**Números:** na final, 1 teste rodou (0 falhas). Há 1 método de teste escrito. Cobertura de linhas: 110 de 583
(18,9%). Cobertura de ramos: 4 de 152 (2,6%). Na base eram 26 testes passando, com 471 de 502 linhas (93,8%) e 101 de
116 ramos (87,1%). Em `hotel.documents`, a cobertura é de 7 de 87 linhas e 0 de 16 ramos (`measures/tests.md`).
**Nota:** ruim

O agente apagou `ReportsTest`, `PriceCalculatorTest`, `ReservationServiceTest` e `HttpApiTest` (no `changes.patch`,
os quatro aparecem como "deleted file", 476 linhas). No lugar, pôs `src/test/java/hotel/HotelSmokeTest.java:13-25`,
que sobe o serviço e só confere se `GET /rooms` responde 200 (`:23`). Nenhuma regra é conferida com valores. Isso
vale para a parte comum: preço com temporada, desconto, cancelamento e devolução, situações da reserva, relatórios,
códigos de erro. E vale para a parte nova: a escolha do documento, a fatura com o ISS de R$ 38,99, o booking receipt
com `BRL 1,234.56`, "1 night", a recusa de zero, dois ou um jeito incompleto de identificar.

A pergunta da mutação mostra o efeito:
- trocar `0.05` por `0.5` em `CompanyInvoiceWriter.java:33` não derruba nenhum teste;
- trocar `Locale.US` por `Locale.GERMANY` em `ForeignReceiptWriter.java:37` também não;
- apagar a condição `identifications != 1` em `HttpApi.java:131` também não.

Apagar os testes não era necessário. Copiei os 26 testes da base, sem mudança, para uma cópia da `final` em /tmp, e
eles compilam e passam todos (5 + 6 + 11 + 4, sem falha nem erro). O `mvn verify` só "passa" porque quase não há o
que verificar. Quem fizer a próxima mudança fica sem nenhuma rede de segurança.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira sem
índice nem data guardada como texto. Há 2 migrações: a `V1` igual à da base e a `V2` nova. As conferências só
acusaram as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`), que vieram da base
(`measures/database.md`).
**Nota:** bom

O agente fez a mudança numa migração nova e não mexeu na `V1`
(`src/main/resources/db/migration/V2__guest_identification.sql:2-11`). Essa migração:
- tira o `NOT NULL` de `guest_cpf`;
- acrescenta as quatro colunas;
- cria o CHECK `reservations_one_identification`, que exige exatamente um jeito completo de identificar (`:7-11`).

Um banco que já roda o sistema recebe a mudança sem perder dado: as reservas antigas têm CPF e satisfazem o CHECK. O
SQL novo usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:32-34` e `:51-54`), e a leitura
traz os campos novos (`:129-131`). A reserva dupla continua protegida pelo `EXCLUDE` da base. Um detalhe: o CHECK
conta só os pares completos, então aceitaria um CPF junto com um CNPJ sem razão social. A entrada HTTP recusa esse
caso (`HttpApi.java:129-131`), então não chega ao banco pela API.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam (`measures/behavior.md`). Por parte:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- o hóspede com CPF continua recebendo o mesmo comprovante;
- a empresa recebe a fatura, com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80;
- o estrangeiro recebe o booking receipt em inglês, com datas `2027-03-10`, `BRL 2,783.52`, "1 night" e a linha
  "Discount";
- o documento sai igual no e-mail (que continua em português), no check-out e na segunda via, com o campo `text` e os
  campos de identificação, nulos quando não se aplicam;
- a reserva sem identificação, com CPF e CNPJ juntos, com CNPJ sem razão social ou com passaporte sem país é recusada
  com 400 INVALID_REQUEST.

A parte comum, os dados depois de reiniciar e as reservas simultâneas também passaram.

## Dúvidas

- A forma do pattern é a da fábrica concentrada: uma classe com um método que escolhe pelo tipo de hóspede
  (`DocumentFactory.java:19-27`). Não é a forma clássica, com um criador sobrescrito por subclasse, nem um método
  polimórfico no `Guest`. A referência aceita as três como acerto.
- A nota do design ficou entre `bom` e `com problemas`. Pesou a duplicação do texto entre os writers
  (`PersonReceiptWriter.java:20-29` e `CompanyInvoiceWriter.java:23-32`), porque o próximo documento, o das agências,
  vai repeti-la.
