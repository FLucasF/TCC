# Revisão: manutenção, cenário 05

## Visão geral

A base já era um sistema em pacotes por assunto: `hotel.web` (rotas Javalin e JSON), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports`, `hotel.rooms` e a
montagem em `hotel`. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. Uma requisição de
criação passa por `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:110`), segue para
`ReservationService.create` e termina em `ReservationStore.add`. Para atender o pedido, o agente criou um tipo
`Identification`, com três records (`CpfIdentification`, `CompanyIdentification`, `PassportIdentification`), escolhido
num lugar só (`Identifications.of`, `src/main/java/hotel/reservations/Identifications.java:10`). Cada um desses tipos
devolve o seu `DocumentLayout` (`ReceiptLayout`, `InvoiceLayout`, `BookingReceiptLayout`), que escreve o texto por um
roteiro comum (`DocumentLayout.render`, `src/main/java/hotel/documents/DocumentLayout.java:40`). O comprovante
(`ReservationReceipt`) pede o layout à identificação, de modo que os três lugares do serviço não mudaram. Também
acrescentou uma migração nova (V2), sem mexer na V1. O comportamento passa inteiro (83 de 83) e os 28 testes passam.
Não há nada sério. Há três coisas para corrigir:
- os testes novos não conferem o texto da fatura nem o do booking receipt (o `BookingReceiptLayout` não é executado
  por nenhum teste);
- a interface `Identification` tem um getter que devolve `null` para cada campo de cada tipo;
- há código mal indentado e um método de teste que ninguém chama.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, as mesmas da base (`measures/dependencies.md` e
`measures/dependencies-base.md` diferem só nas linhas e no número de classes). Há 1 ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base. `hotel.reservations` passou de 13 para 18 classes (508 → 624 linhas) e
`hotel.documents` de 2 para 6 (79 → 278). A maior classe continua sendo `HttpApi` (178 linhas; eram 170).
**Nota:** bom

- **Borda HTTP:** continua fina. A mudança em `HttpApi.java:117-137` só tirou o `guestCpf` da lista de campos
  obrigatórios e delega a escolha do tipo de hóspede a `Identifications.of`, que mora em `reservations` e lança a
  `HotelException` do domínio. Nenhuma regra nova entrou nos handlers.
- **Regras sem Javalin:** continuam sem Javalin.
- **SQL isolado:** o SQL novo ficou todo em `ReservationStore` (`ReservationStore.java:18`, `:31-57`, `:130-137`).
- **Montagem:** num lugar só; o agente não mexeu nela.
- **O ciclo:** já vinha da base (`ReservationService` cria `documents.ReservationReceipt`, que lê
  `reservations.Reservation`). O agente o apertou: agora os tipos de domínio apontam para layouts concretos de
  apresentação (`CpfIdentification.java:3-4`, `CompanyIdentification.java:3-4`, `PassportIdentification.java:3-4`
  importam `hotel.documents.*Layout`). Os layouts, por sua vez, leem `Reservation`, `Identification`, `Price` e
  `PriceCalculator` (`InvoiceLayout.java:3-6`). Não é um ciclo novo entre pacotes e o conjunto é pequeno, por isso não
  derruba a nota. Mas é o ponto em que domínio e apresentação mais se misturam.

## Design de baixo nível

**Números:** 3 de 3 tipos de documento (inclusive o comprovante que já existia) estão na estrutura e 0 fora dela. Os 3
lugares que emitem o documento (`ReservationService.java:64`, `:109` e `:118`) usam o mesmo construtor, que pergunta à
identificação qual layout usar (`ReservationReceipt.java:39` e `:55`). Não há `if`, `switch` nem `instanceof` sobre o
tipo de hóspede fora de `Identifications.of`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha do documento é um método polimórfico no tipo de hóspede, `Identification.layout()`
(`Identification.java:11`), sobrescrito por tipo:
- `CpfIdentification.java:10` → `ReceiptLayout`;
- `CompanyIdentification.java:10` → `InvoiceLayout`;
- `PassportIdentification.java:10` → `BookingReceiptLayout`.

Isso corresponde à forma "método polimórfico no próprio tipo de hóspede" que a referência aceita como acerto. O
comprovante da base também entrou na estrutura (`ReceiptLayout`). Os três lugares do serviço nem precisaram mudar,
porque `new ReservationReceipt(reservation, room)` delega a `identification.layout()`. A decisão de qual tipo criar a
partir dos campos fica num lugar só (`Identifications.java:10-27`), usado na entrada HTTP (`HttpApi.java:130`) e na
leitura do banco (`ReservationStore.java:131`).

Por cima disso, `DocumentLayout.render` (`DocumentLayout.java:40-57`) é um Template Method: o roteiro fixo, com a linha
de desconto condicional e um gancho `closingLines` para o ISS. A fatura herda do comprovante e troca só o título, a
linha do hóspede e o fecho (`InvoiceLayout.java:13-31`). O ISS reaproveita o arredondamento de sempre
(`PriceCalculator.cents`, que passou a ser público em `PriceCalculator.java:50`).

Para a agência de turismo, o caminho é:
- uma classe `Identification` nova;
- um layout novo;
- um ramo em `Identifications.of`;
- os campos na requisição, na resposta, no documento, no `ReservationStore` e na migração.

Essa última parte decorre da API plana que o texto pede.

**Exagero:** não há. Cada abstração nova tem três implementações e é usada (`DocumentLayout`, `Identification`). Não há
fábrica, builder nem camada que só repassa.

**O design do resto (o que pede correção):**
- **Getters que devolvem `null`:** `Identification` tem um getter `default` que devolve `null` para cada campo de cada
  tipo (`Identification.java:13-31`). Cada tipo novo obriga a acrescentar getters na interface comum. Além disso, os
  layouts dependem de `null` implícito: `BookingReceiptLayout.java:21` lê `guest.passport()` e `guest.country()` pela
  interface genérica, e `InvoiceLayout.java:20` faz o mesmo com a empresa. Se a identificação for de outro tipo, o
  texto sai com "null" em vez de falhar.
- **Layout criado duas vezes:** o construtor de `ReservationReceipt` chama `identification.layout()` duas vezes
  (`ReservationReceipt.java:39` e `:55`) e cria duas instâncias para o mesmo documento.
- **Indentação quebrada:** em `ReservationStore.java:131-137` os argumentos estão no nível do método e a linha
  `Guest guest = ...` está na coluna 0. Em `ReservationServiceTest.java:39-46` o novo método `stay(Identification,
  String)` e o `errorOf` seguinte perderam a indentação.
- **Código morto:** esse mesmo `stay(Identification, String)` (`ReservationServiceTest.java:39`) não é chamado em lugar
  nenhum.
- **Erro do banco tratado como erro do cliente:** `Identifications.of`, reaproveitado na leitura do banco
  (`ReservationStore.java:131`), responderia `INVALID_REQUEST` (400) se uma linha gravada estivesse inconsistente. O
  defeito é do servidor, não do pedido.

O resto (`BigDecimal` com o arredondamento da base, `LocalDate`, records imutáveis) está certo.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados, 28 métodos escritos; a base tinha 26. O agente
acrescentou 2 testes, ambos em `HttpApiTest`. Nenhum teste da base foi apagado ou enfraquecido; os da base só trocaram
a CPF solta por `new CpfIdentification(...)`. A cobertura de linhas é 92,2% (547 de 593; a base tinha 93,8%) e a de
ramos 85,4% (135 de 158; a base tinha 87,1%). Em `hotel.documents` caiu de 30 de 30 para 62 de 77 linhas. Pelo
`target/site/jacoco/jacoco.csv`, `BookingReceiptLayout` tem 0 de 13 linhas cobertas, e `TextFormat.englishMoney` e
`TextFormat.englishDate` nunca rodam.
**Nota:** com problemas

**O que os testes novos conferem:**
- **Recusas:** `refusesAnIdentificationThatIsNotExactlyOneKind` (`HttpApiTest.java:155`) cobre nenhuma identificação,
  CPF com passaporte, CNPJ sem razão social, passaporte sem país e CPF com empresa, todos com status e código.
- **Empresa:** `acceptsACompanyOrAPassportInsteadOfTheCpf` (`HttpApiTest.java:131`) confere os campos `null` na
  reserva, o título "Fatura de hospedagem" e o CNPJ no documento.
- **Estrangeiro:** o mesmo teste só cria a reserva com passaporte e confere os campos da reserva
  (`HttpApiTest.java:146-151`).

**O que fica sem rede (a pergunta da mutação):**
- **Valor do ISS:** só se confere que o texto contém o rótulo "ISS retido (5%)" (`HttpApiTest.java:144`). Trocar
  `0.05` por `0.50` em `InvoiceLayout.java:15`, ou apagar o arredondamento, não derruba nenhum teste. O texto dá o
  valor esperado (R$ 38,99) e ele não foi usado.
- **Booking receipt:** a reserva com passaporte nunca é confirmada, e o documento dela nunca é pedido. Trocar o formato
  `BRL %,.2f` (`TextFormat.java:39`), o formato da data ou "1 night"/"nights" (`BookingReceiptLayout.java:30`) não
  derruba nenhum teste.
- **E-mail e check-out:** nada confere que o e-mail de confirmação e o check-out levam o documento do tipo certo. Só a
  segunda via da empresa é testada.

O texto do comprovante com CPF também não era conferido linha a linha na base; essa lacuna veio da base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Não há chave estrangeira sem
índice nem data em texto. A V1 está igual à da base e a V2 é nova. O que o script acusou: as colunas de situação e de
tipo sem CHECK (`status`, `rate_plan`, `channel`, `type`), todas vindas da base. Os 4 candidatos a SQL montado com `+`
concatenam só constantes (`ReservationStore.java:18`, `:35`).
**Nota:** bom

- **A migração:** a mudança do esquema veio numa migração nova, `V2__guest_identification.sql:2-7`. Ela tira o
  `NOT NULL` de `guest_cpf` e acrescenta quatro colunas `text` anuláveis. As reservas antigas, todas com CPF, continuam
  legíveis, porque `Identifications.of` as reconhece como `CpfIdentification`.
- **O SQL:** o `INSERT` e o `SELECT` foram estendidos com parâmetros (`ReservationStore.java:31-57`), com
  try-with-resources e pool, como na base.
- **Reservas simultâneas:** a proteção contra reserva duplicada continua sendo o `EXCLUDE USING gist` da base.
- **O que um revisor ainda pediria:** um `CHECK` que exija exatamente uma das três identificações completas. A regra
  "um jeito, e só um" mora apenas em Java. Se uma linha inconsistente entrar no banco por fora do serviço, `read`
  (`ReservationStore.java:131`) lança uma exceção, e qualquer listagem que passe por ela quebra (os relatórios usam
  `findAll`). É uma ressalva pequena: hoje só o serviço escreve na tabela, e sempre pela validação.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- **Hóspede com CPF:** continua recebendo o mesmo comprovante, com o mesmo texto.
- **Empresa:** recebe a fatura, com a empresa no lugar do CPF, o hóspede e o ISS retido de R$ 38,99 sobre R$ 779,80.
  Ela vai no e-mail em português, no check-out e na segunda via.
- **Estrangeiro:** recebe o booking receipt em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha
  "Discount:" quando há desconto.
- **Recusas:** nenhuma identificação, duas juntas ou uma incompleta dão 400 `INVALID_REQUEST`.
- **O resto:** as reservas e as mensagens continuam depois de reiniciar, e duas reservas simultâneas do mesmo quarto
  nunca são criadas as duas.

Para o dono da pousada, nada do que funcionava quebrou. As defesas contra erro futuro são as que faltam nos testes, não
no comportamento de hoje.

## Dúvidas

- **A forma do pattern:** é um método polimórfico no tipo de hóspede (`Identification.layout()`), combinado com um
  Template Method nos layouts. A referência aceita essa forma como acerto.
- **Onde fica a escolha:** os três lugares do serviço continuam escrevendo `new ReservationReceipt(...)`, mas a escolha
  do documento acontece dentro do construtor, num lugar só. Por isso não contei como "lugar que cria à mão".
- **A nota do design:** ficou em `com problemas` por defeitos pequenos e localizados (getters que devolvem `null`,
  indentação quebrada, código morto). Um revisor mais tolerante poderia dar `bom`.
- **A pasta `target/`:** os scripts de medição gravaram a saída do Maven em `final/target` e `base/target`. Os fontes
  não foram tocados.
