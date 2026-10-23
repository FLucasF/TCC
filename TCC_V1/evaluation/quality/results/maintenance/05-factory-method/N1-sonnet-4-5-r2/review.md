# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web` cuida do HTTP. `hotel.reservations` tem as regras e o acesso ao banco (`ReservationStore`). O documento do hóspede fica em `hotel.documents`. As peças são montadas em `hotel.Hotel`, e o PostgreSQL é criado por migrações do Flyway. Uma reserva entra por `HttpApi.readNewReservation`, passa por `ReservationService.create` e é gravada por `ReservationStore.add`.

O agente fez a mudança no ponto certo:
- a validação do tipo de identificação ficou na borda (`HttpApi.java:127-137`);
- `Guest` ganhou os quatro campos (`Guest.java:3-10`);
- a migração é nova, `V2`, e a `V1` ficou intocada;
- a escolha do documento ficou concentrada no construtor de `ReservationReceipt` (`ReservationReceipt.java:31-71`). Os três lugares que emitem o documento (`ReservationService.java:64,109,118`) não mudaram e não perguntam o tipo de hóspede.

O roteiro do comportamento passa inteiro (83 de 83), e os 35 testes passam. Os defeitos que encontrei:
- **Erro 500 em pedidos que deveriam dar 400.** Quando um pedido mistura um CPF com uma segunda identificação incompleta, ou manda o CPF como texto vazio junto com os dados da empresa, a validação deixa passar. Quem recusa é a restrição do banco, e o cliente recebe 500 em vez do 400 que o texto pede.
- **Texto copiado.** O texto do comprovante e o da fatura são quase o mesmo trecho, escrito duas vezes.
- **Plural sem teste.** O teste do "1 night" não pega uma troca para "1 nights".

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, exatamente as mesmas da base (`measures/dependencies.md` comparado com `measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. Os pacotes cresceram assim: `hotel.documents` de 79 para 166 linhas, `hotel.reservations` de 508 para 544, `hotel.web` de 236 para 267. A maior classe continua sendo `HttpApi`, com 189 linhas.
**Nota:** bom

- **Dependências e ciclo.** A mudança não acrescentou dependência entre pacotes. O ciclo vem da base: `ReservationService.java:3` importa `ReservationReceipt`, e `ReservationReceipt` lê `Reservation`. O agente reforçou esse sentido com dois imports novos em `ReservationReceipt.java:3-4`, `Guest` e `PriceCalculator`. O de `PriceCalculator` nem é usado.
- **Borda HTTP.** Continua fina. A regra "exatamente um jeito de identificar" ficou em `HttpApi.java:127-137`, junto com as outras validações de formato que a base já fazia ali (`HttpApi.java:116-125`). É lugar razoável para validar o corpo da requisição.
- **SQL.** Continua isolado em `ReservationStore`, que ganhou só as colunas novas (`ReservationStore.java:18-20, 32-45, 130-137`).
- **Montagem.** Continua num lugar só, `Hotel.java:20-28`.
- **Tamanho.** `ReservationReceipt` passou de 55 para 132 linhas, mas o crescimento é texto de documento, não mistura de camadas.

## Design de baixo nível

**Números:** há 3 documentos (comprovante, fatura e booking receipt). A escolha entre eles acontece em 1 classe (`ReservationReceipt`), mas em 2 cadeias de `if`: `titleFor` (`ReservationReceipt.java:52-60`) e `generateText` (`ReservationReceipt.java:62-71`). Os 3 lugares que pedem o documento (`ReservationService.java:64,109,118`) não fazem nenhuma escolha. Nenhuma interface ou classe nova foi criada.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que a referência pede está atendido: a decisão de qual documento criar mora num lugar só, e os três lugares pedem o documento a esse lugar.
- O construtor `ReservationReceipt(Reservation, Room)` (`ReservationReceipt.java:31-50`) funciona como uma fábrica concentrada: escolhe título e texto pelo tipo de hóspede.
- `confirm`, `checkOut` e `receiptCopy` continuam com o mesmo `new ReservationReceipt(...)` da base.
- O caso que já existia (o comprovante com CPF) também passou pela estrutura (`generateReceipt`, `ReservationReceipt.java:73-89`).

O que afasta da forma ideal:
- **A escolha está duplicada.** Ela aparece duas vezes na mesma classe (`titleFor` e `generateText`). Para incluir a agência de turismo, é preciso mexer nas duas cadeias, em `Guest` e nos campos do record.
- **O tipo de hóspede não existe como tipo.** Ele é deduzido de campos nulos (`Guest.java:16-26`). `isIndividual()` (`Guest.java:16`) nem é usado.

**O exagero.** Nenhum. O agente não criou interface, fábrica separada nem hierarquia sem necessidade.

**O resto do código:**
- **Texto duplicado.** `generateReceipt` (`ReservationReceipt.java:73-89`) e `generateInvoice` (`ReservationReceipt.java:91-112`) repetem quase linha a linha as linhas de quarto, datas, diárias, desconto e total. O mesmo esqueleto volta em `generateBookingReceipt` (`ReservationReceipt.java:114-131`), mudando só o idioma e o formato.
- **Arredondamento reescrito à mão.** O ISS faz o arredondamento direto ali (`ReservationReceipt.java:92-94`, `setScale(2, java.math.RoundingMode.HALF_EVEN)` com nome qualificado). A regra é a mesma de `PriceCalculator.cents` (`PriceCalculator.java:50-52`), cujo import ficou sem uso (`ReservationReceipt.java:4`). O 5% é um literal solto no meio do método.
- **Duas ideias de "campo presente" que não batem.** A validação trata o texto vazio como ausente (`HttpApi.java:127-129`, `isBlank`). O `Guest` trata qualquer valor não nulo como presente (`Guest.java:20-26`). Além disso, os campos de uma identificação incompleta são repassados sem limpeza (`HttpApi.java:139-146`). Essa discrepância é a causa do erro 500 descrito em Comportamento.

## Testes

**Números:** 35 testes rodaram, 0 falharam, 35 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas: 94,4% (558/591), contra 93,8% na base. Cobertura de ramos: 87,0% (134/154), contra 87,1%. Em `hotel.documents`: 85 de 87 linhas e 17 de 20 ramos. Todos os testes rodam contra o banco, sem mocks (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

**O que o agente acrescentou:** 4 testes no serviço (`ReservationServiceTest.java:179-239`) e 5 pela API (`HttpApiTest.java:124-207`). Nenhum teste da base foi apagado nem enfraquecido.

**O que ficou conferido com valores:**
- o ISS de R$ 38,99 (`ReservationServiceTest.java:194`). Trocar 0.05 por outra taxa derruba esse teste;
- a linha da empresa e a do hóspede na fatura;
- a linha do hóspede estrangeiro, as datas ISO e o milhar com vírgula (`ReservationServiceTest.java:235-236`);
- os campos nulos do documento (`ReservationServiceTest.java:189, 207`);
- três recusas com 400: nenhuma identificação, CPF com CNPJ, CNPJ sem razão social (`HttpApiTest.java:174-207`).

**O que ficaria sem rede (a pergunta da mutação):**
- **O singular "1 night".** O teste só confere `contains("1 night")` (`ReservationServiceTest.java:224`). Se o código escrevesse sempre " nights", o texto teria "1 nights", que contém "1 night", e o teste passaria.
- **A linha "Discount:" do booking receipt.** Nenhum teste a confere. Apagá-la não derruba nada.
- **O arredondamento do ISS.** 779,80 × 5% dá 38,99 exatos, então nenhum caso exercita o arredondamento.
- **O e-mail de confirmação.** Nenhum teste confere que o e-mail da empresa ou do estrangeiro leva a fatura ou o booking receipt.
- **Duas recusas pedidas.** "Passaporte sem país" não é testado. A mistura de CPF com uma identificação incompleta também não, e é justamente o caso que dá 500.
- **As asserções pela API são fracas em parte.** `HttpApiTest.java:144` confere só que existe "ISS retido (5%):", sem o valor. `HttpApiTest.java:166` confere só que o texto contém "BRL".

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` e datas em `date`. A chave estrangeira tem índice. Há 2 migrações: a `V1` igual à da base e a `V2` nova. A única coisa que as conferências acusaram vem da base: as colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) estão em `text` sem CHECK (`measures/database.md`).
**Nota:** bom

- **Migração.** O esquema evoluiu do jeito certo, com uma migração nova (`V2__add_guest_identification_types.sql:2-6`). Ela solta o `NOT NULL` de `guest_cpf` e acrescenta as quatro colunas como `text`, que aceitam nulo.
- **Restrição de identificação.** O banco também protege a regra "exatamente uma identificação" com um CHECK (`V2__add_guest_identification_types.sql:9-13`). É essa restrição que impede gravar uma reserva com CPF e CNPJ juntos.
- **Uma brecha pequena nessa restrição.** O ramo do CPF não exige `company_name` e `guest_country` nulos (`V2__add_guest_identification_types.sql:10`). Por isso uma reserva com CPF e um `guestCountry` solto é gravada: confirmei com uma chamada à mão, que respondeu 201 com `"guestCountry":"UK"`. O dado sobra, mas não é corrompido.
- **SQL.** Continua com parâmetros (`ReservationStore.java:32-55`). As concatenações apontadas pelo script juntam só constantes (`COLUMNS`).
- **Reservas duplicadas.** Continuam barradas pelo `EXCLUDE` da base.
- **Erro do banco vira 500.** Uma violação do CHECK novo vira `IllegalStateException` (`ReservationStore.java:60-66`, tratamento que veio da base) e chega ao cliente como 500. O efeito disso pesa em Comportamento.

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46/46, reinício 3/3, cenário 05 32/32, concorrência 2/2 (`measures/behavior.md`).
**Nota:** com problemas

**O que funciona, pelo roteiro:**
- tudo o que já funcionava continua funcionando;
- a pessoa com CPF recebe o mesmo comprovante;
- a fatura sai com a empresa e o ISS de R$ 38,99, no e-mail em português, no check-out e na segunda via;
- o booking receipt sai em inglês, com `BRL 2,783.52`, datas ISO, "1 night" e a linha "Discount";
- as quatro recusas do roteiro dão 400 INVALID_REQUEST.

**O que falha, fora do roteiro.** O texto diz que a identificação "tem que ser um desses jeitos, e só um; se não for, a resposta é 400". Chamei a API à mão com o serviço de pé:
- **CPF com `companyCnpj` sem `companyName`:** 500.
- **CPF com `guestPassport` sem `guestCountry`:** 500.
- **`guestCpf: ""` com CNPJ e razão social:** 500.
- **CPF com só `guestCountry`:** 201, e a reserva fica gravada com o país solto.

A causa: a validação só conta as identificações completas (`HttpApi.java:127-131`), mas repassa os campos incompletos ao `Guest` (`HttpApi.java:139-146`). A reserva errada chega ao banco, o CHECK a recusa, e a resposta vira "Server Error" (log: `violates check constraint "check_one_identification_type"`).

Para o dono da pousada, isso quer dizer o seguinte. Nenhum dado errado é gravado nesses casos de 500. Mas o site recebe um erro de servidor em vez da recusa combinada, e não tem como dizer ao cliente o que corrigir.

## Dúvidas

- **A classe do pattern.** Fiquei entre `acerto` e `malfeito`. A escolha está num lugar só e os três lugares não a repetem, o que cabe como fábrica concentrada num método. Mas ela aparece em duas cadeias de `if` na mesma classe (`titleFor` e `generateText`), e há um único tipo de documento, que muda só o texto. A forma usada foi a fábrica concentrada: o construtor do record escolhe pelo tipo de hóspede, que é deduzido dos campos nulos.
- **A nota de Comportamento.** O roteiro passa inteiro, e o 500 aparece só em combinações que o roteiro não testa. Dei `com problemas` porque o texto pede 400 para qualquer pedido que não seja exatamente um dos três jeitos, e esses pedidos são a mesma chamada que o texto descreve.
