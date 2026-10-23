# Revisão: manutenção, cenário 05

## Visão geral

A base tem sete pacotes: `hotel` (a montagem), `hotel.web` (Javalin), `hotel.reservations` (as regras e o `ReservationStore` com JDBC), `hotel.documents`, `hotel.messages`, `hotel.reports` e `hotel.rooms`. Uma requisição entra em `HttpApi`, que converte o corpo em `NewReservation`, chama o `ReservationService` e devolve um record de resposta. Os dados ficam no PostgreSQL, com migrações do Flyway. O agente trocou o CPF do `Guest` por uma `sealed interface GuestIdentity` com três records (`IndividualIdentity`, `CorporateIdentity`, `ForeignIdentity`). Criou uma interface `DocumentGenerator` com três implementações e uma fábrica concentrada, `DocumentGenerators.forIdentity`. Os três lugares que emitem o documento passaram a pedir o documento a essa fábrica. Também acrescentou a migração `V2__add_guest_identity_fields.sql`, sem editar a V1. O sistema faz o que o pedido descreve: 83 de 83 conferências passaram, e os 29 testes passam. O que há de mais sério é pequeno:
- o texto do comprovante e o da fatura são cópias quase linha a linha;
- os testes novos não conferem os valores da parte nova (o ISS, o formato do dinheiro em inglês);
- a validação aceita um CPF junto com um CNPJ incompleto e simplesmente descarta o CNPJ.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e `measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O Javalin só aparece em `hotel` e `hotel.web`, e o JDBC em `hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`, como na base. A maior classe continua sendo `HttpApi` (170 → 195 linhas); o `ReservationStore` passou de 143 para 191 linhas.
**Nota:** bom

O agente pôs cada parte da mudança na camada certa:
- **Na borda HTTP:** só lê os campos novos e monta a identidade (`src/main/java/hotel/web/HttpApi.java:137-155`). A resposta só converte os campos (`src/main/java/hotel/web/ReservationResponse.java:58-76`).
- **Nas regras:** o serviço só troca o `new ReservationReceipt(...)` pela fábrica (`src/main/java/hotel/reservations/ReservationService.java:65`, `:111`, `:121`), sem Javalin nem SQL.
- **No acesso ao banco:** a leitura e a gravação das colunas novas ficam todas no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:65-90` e `:174-190`).

Os tipos de identidade moram em `hotel.reservations` e os geradores em `hotel.documents`. Isso usa o ciclo que a base já tinha (antes o documento já lia `Reservation`, e o serviço já criava o documento), sem criar um ciclo novo. O ciclo vem da base.

## Design de baixo nível

**Números:** 3 de 3 tipos de documento passam pela estrutura (comprovante, fatura, booking receipt), e o caso que já existia (o CPF) também. Os 3 de 3 lugares que emitem o documento (confirmação/e-mail, check-out e segunda via) pedem o documento à fábrica. Nenhum lugar cria o documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente usou uma fábrica concentrada: `DocumentGenerators.forIdentity` (`src/main/java/hotel/documents/DocumentGenerators.java:18-24`) escolhe o gerador pelo tipo de identidade num `switch` exaustivo sobre a `sealed interface`. Cada gerador (`BrazilianReceiptGenerator`, `CorporateInvoiceGenerator`, `EnglishReceiptGenerator`) implementa `DocumentGenerator.generate` e monta o seu documento e o seu texto. O `ReservationService` não pergunta qual é o caso: só chama a fábrica nos três lugares (`ReservationService.java:65-66`, `:111-112`, `:121-122`). O comprovante da base deixou de ser montado no construtor do record (o diff de `ReservationReceipt.java`) e passou para a estrutura. Para pôr uma agência de turismo, bastam um novo record de identidade e um novo gerador. O `switch` exaustivo obriga a tratar o caso novo na fábrica. A forma usada é a fábrica concentrada, que a referência aceita como acerto.

O que afasta um pouco: cada gerador ainda faz um downcast da identidade que a fábrica já conhecia, por exemplo `(IndividualIdentity) reservation.guest().identity()` (`BrazilianReceiptGenerator.java:11`, `CorporateInvoiceGenerator.java:13`, `EnglishReceiptGenerator.java:11`). A ligação entre fábrica e gerador só é conferida em tempo de execução, com `ClassCastException` se alguém errar.

**Exagero.** Não há estrutura sem necessidade. A interface `DocumentGenerator` tem três implementações e é o próprio pattern, e não apareceram builders nem camadas de repasse. Contenção.

**O resto do design.**
- **Duplicação:**
  - O texto da fatura é uma cópia do texto do comprovante. As linhas de Quarto, Entrada/Saída/diárias, Diárias, Desconto e Total estão repetidas (`BrazilianReceiptGenerator.java:38-47` e `CorporateInvoiceGenerator.java:45-54`).
  - O record de 17 componentes posicionais é montado três vezes, com quatro ou cinco `null` em cada (`BrazilianReceiptGenerator.java:13-31`, `CorporateInvoiceGenerator.java:15-33`, `EnglishReceiptGenerator.java:13-31`). Trocar dois `String` de lugar passaria pelo compilador.
  - O ISS repete o arredondamento à mão (`CorporateInvoiceGenerator.java:36-38`) em vez de usar `PriceCalculator.cents` (`PriceCalculator.java:50-52`). O resultado é o mesmo (`HALF_EVEN`), mas a regra fica em dois lugares.
- **Perguntas sobre a identidade:** a pergunta "qual identidade é esta" se repete fora dos documentos, em `ReservationStore.java:67-90`, em cinco `instanceof` em `ReservationResponse.java:58-76` e em `ReservationStore.java:174-190`. Para a conversão de campos é aceitável, mas um tipo novo de hóspede obriga a mexer em todos esses lugares.
- **Validação:** `HttpApi.java:138-140` só conta uma identidade quando ela está completa, e ignora em silêncio os campos que sobram (o que está em "Comportamento").
- **Tipos:** o dinheiro continua em `BigDecimal` e as datas em `LocalDate`, e o `GuestIdentity` selado é um bom tipo de domínio.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos, contra 26 na base, todos mantidos. A cobertura de linhas é de 94,7% (609/643), contra 93,8% na base, e a de ramos de 86,5% (147/170), contra 87,1%. O pacote `hotel.documents` tem 108/110 linhas e 11/15 ramos cobertos (`measures/tests.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base e só adaptou a construção do `Guest` (`ReportsTest`, `ReservationServiceTest`). Acrescentou três testes pela API em `src/test/java/hotel/web/HttpApiTest.java`:
- a empresa (`:124`);
- o estrangeiro (`:148`);
- nenhuma identidade e duas identidades (`:173`).

As asserções da parte nova são fracas:
- **O ISS:** só se confere que o texto contém `"ISS retido (5%):"` (`HttpApiTest.java:144`), sem o valor R$ 38,99. Se alguém trocasse `0.05` por `0.10` em `CorporateInvoiceGenerator.java:37`, nenhum teste falharia.
- **O dinheiro em inglês:** só se confere `contains("BRL")` (`HttpApiTest.java:169`). Trocar `Locale.US` por `pt-BR` em `TextFormat.java:27` (o que daria "BRL 779,80") também passaria.
- **O que ninguém confere:**
  - "1 night";
  - a linha "Discount:";
  - que o e-mail de confirmação leve a fatura ou o booking receipt;
  - a segunda via dos tipos novos;
  - a recusa de uma identidade incompleta (CNPJ sem razão social, passaporte sem país).

Nenhum teste compara o texto inteiro dos documentos novos. O comprovante do CPF fica coberto pelos testes da base (`ReservationServiceTest.java:84`, `:155`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, as datas em `date`, e não há chave estrangeira sem índice. A V1 está igual à da base e a V2 é nova. As únicas conferências que acusaram algo foram `rate_plan`, `status`, `type` e `channel` sem CHECK, o que veio da base (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo, com uma migração nova (`src/main/resources/db/migration/V2__add_guest_identity_fields.sql`):
- quatro colunas `text`;
- `guest_cpf` deixa de ser `NOT NULL` (`:8`);
- um CHECK que obriga exatamente uma identidade completa (`:11-16`).

As linhas que já existem no banco, todas com CPF, passam no CHECK, então a migração aplica sobre dados da base sem perder nada. O CHECK não proíbe colunas soltas: um CPF com `company_name` preenchido passaria, mas o código nunca grava isso (`ReservationStore.java:67-90`). O SQL continua com parâmetros, e os trechos com `+` só juntam constantes (`ReservationStore.java:35`, `:127`, `:140`). A gravação com o índice inicial e as posições `startIndex + n` (`ReservationStore.java:44`, `:65-90`) é frágil de manter, mas está correta. A exclusão por `gist` contra reserva dupla vem da base.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo funciona:
- **Pessoa com CPF:** continua recebendo o mesmo comprovante.
- **Empresa:** recebe a fatura com a razão social, o CNPJ e o ISS de R$ 38,99 sobre R$ 779,80.
- **Estrangeiro:** recebe o booking receipt com "BRL 2,783.52", as datas 2027-03-10 e "1 night".
- **Onde o documento sai:** no e-mail em português, no check-out e na segunda via.
- **Recusas:** os pedidos sem identificação, com duas identificações ou com uma incompleta recebem 400 INVALID_REQUEST.

Fora do roteiro, chamei a API com o serviço de pé:
- um pedido com `guestCpf` e um `companyCnpj` sem `companyName` foi aceito (201) como reserva de pessoa física, e o CNPJ foi descartado em silêncio;
- um pedido com CNPJ e razão social mais um `guestCountry` solto também foi aceito.

A causa está em `HttpApi.java:138-142`. O pedido diz "tem que ser um desses jeitos, e só um", e um campo que sobra poderia ter recebido 400. É um ponto de borda que o texto não fecha de todo, por isso não muda a nota.

## Dúvidas

- A forma do pattern é a da fábrica concentrada (`DocumentGenerators.forIdentity` com um `switch` sobre a `sealed interface`), não a do criador com o método sobrescrito nem a do método polimórfico na identidade. A referência aceita as três como acerto.
- Não sei se aceitar um CPF junto com um CNPJ incompleto, descartando o CNPJ, é um erro ou uma leitura possível de "só um desses jeitos". Contei como ponto de borda e não baixei a nota do comportamento por isso.
