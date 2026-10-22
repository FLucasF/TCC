# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas e DTOs), `hotel.reservations` (regras, modelo e o
`ReservationStore` com JDBC), `hotel.documents` (o comprovante e a formatação), `hotel.messages`, `hotel.reports`,
`hotel.rooms` e a montagem em `hotel`. Uma requisição entra em `HttpApi`, que valida o corpo e monta a
`NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:109-150`), passa pelo `ReservationService` e chega ao
`ReservationStore`, que guarda tudo no PostgreSQL com migrações do Flyway. O agente acrescentou os dois novos tipos de
hóspede de forma limpa: `Guest` com três fábricas estáticas, uma migração nova (`V2__add_guest_types.sql`) com um CHECK
que garante exatamente uma identificação, uma interface `DocumentGenerator` com três implementações e a escolha do
documento num lugar só (`ReservationReceipt.java:60-68`), que os três pontos do serviço usam. O `mvn verify` passa (34
testes) e o roteiro de comportamento passa inteiro (83 de 83). O defeito mais sério está na validação da entrada: uma
identificação completa somada a outra incompleta (CPF + CNPJ sem razão social, por exemplo) é aceita, e o dado a mais é
descartado sem aviso. Também há duplicação de texto entre os três geradores, e o tipo do hóspede só se descobre
testando se os campos são nulos, em três cadeias de `if` diferentes.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, na `final` e na `base` (mesmas arestas); 1 ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`).
**Nota:** bom

A mudança respeita as camadas da base. A borda HTTP só lê e valida o corpo e monta o `Guest`
(`final/src/main/java/hotel/web/HttpApi.java:127-146`). O SQL novo fica no `ReservationStore`
(`ReservationStore.java:31-35`, `127-146`). Os geradores de documento (`hotel.documents`) não usam Javalin nem JDBC
(tabela de bibliotecas em `measures/dependencies.md`). O ciclo `documents` ↔ `reservations` vem da base:
`ReservationService` importa `ReservationReceipt`, e `ReservationReceipt` lê `Reservation`. O agente reforçou um pouco
esse acoplamento quando tornou público o `PriceCalculator.cents` (`PriceCalculator.java:50`) para usá-lo em
`CompanyDocumentGenerator.java:23`, mas não criou nenhuma aresta nova. As maiores classes cresceram pouco: `HttpApi`
foi de 170 para 190 linhas e `ReservationStore` de 143 para 165. A montagem continua num lugar só (`hotel/Hotel.java`,
que não foi tocado).

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 dentro da estrutura (`IndividualDocumentGenerator`,
`CompanyDocumentGenerator`, `ForeignDocumentGenerator`). A escolha fica num lugar só
(`ReservationReceipt.java:60-68`). Os 3 lugares que emitem documento (`ReservationService.java:64`, `109` e `118`)
pedem o documento a esse ponto, e nenhum deles monta um à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern:** foi usada uma fábrica concentrada. `ReservationReceipt.selectGenerator`
(`final/src/main/java/hotel/documents/ReservationReceipt.java:60-68`) decide pelo tipo de hóspede qual
`DocumentGenerator` usar, e o construtor público (`:29-31`) é o único caminho para criar o documento. O e-mail de
confirmação (`ReservationService.java:64`, texto em `:128`), o check-out (`:109`) e a segunda via (`:118`) não
perguntam qual é o tipo do hóspede. O comprovante da pessoa com CPF, que já existia, também foi para dentro da
estrutura (`IndividualDocumentGenerator.java`), em vez de ficar no `text()` antigo do record. Para incluir a agência de
turismo, basta um gerador novo e um ramo novo nesse método. Isso conta como acerto.

**O exagero:** não há. A interface tem três implementações e faz parte do pattern. `EnglishFormat`
(`EnglishFormat.java`) espelha o `TextFormat` da base. As fábricas estáticas de `Guest` (`Guest.java:31-41`) evitam
construir um hóspede incoerente. Não há camada que só repasse chamadas.

**O resto do código:**
- **Duplicação nos geradores:** o comprovante e a fatura repetem quase as mesmas linhas. Quarto, entrada/saída com
  "diária(s)", diárias, desconto e total estão em `IndividualDocumentGenerator.java:22-31` e de novo em
  `CompanyDocumentGenerator.java:33-43`. O booking receipt repete a mesma sequência com outro formato
  (`ForeignDocumentGenerator.java:28-37`). Uma mudança no layout comum precisa ser feita em três lugares.
- **Tipo de hóspede implícito:** o tipo não tem `enum` nem subtipo. Ele é deduzido de qual campo não é nulo, em três
  cadeias de `if` separadas: `ReservationReceipt.java:61-67`, `ReservationStore.java:135-146` e `HttpApi.java:139-146`.
  O hóspede da agência vai exigir mexer nas três, e o `else` final de cada uma trata qualquer caso desconhecido como
  estrangeiro.
- **Tipos e estado:** dinheiro continua em `BigDecimal` e o ISS usa o arredondamento da casa
  (`CompanyDocumentGenerator.java:13`, `:23`). O `Guest` deixou de ser record e virou uma classe imutável com getters
  escritos à mão (`Guest.java:4-69`), sem `equals`/`hashCode`. Não faz falta no código atual, mas é uma perda em
  relação à base.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 pulados, e há 34 métodos no código (a base tinha 26: 8 novos, nenhum
apagado). Cobertura de linhas de 94,8% (585/617) e de ramos de 88,8% (135/152), contra 93,8% e 87,1% na base. Em
`hotel.documents`, 97/99 linhas e 14/16 ramos.
**Nota:** com problemas

**O que está testado, com valores:**
- a fatura pelo e-mail de confirmação, com "Fatura de hospedagem nº 1", a linha da empresa, o hóspede,
  "ISS retido (5%): R$ 38,99" e o total (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:164-180`);
- o booking receipt, com o hóspede e o passaporte, as datas ISO, "2 nights" e "BRL 779.80" (`:182-198`);
- "1 night" no check-out (`:200-211`);
- pela API, os campos nulos/preenchidos na reserva da empresa e na do estrangeiro, com o título do documento na segunda
  via (`HttpApiTest.java:124-168`);
- três recusas com 400 INVALID_REQUEST: nenhuma identificação, CPF + empresa e CNPJ sem razão social
  (`HttpApiTest.java:170-206`).

Os 26 testes da base continuam lá e passam (só trocaram `new Guest` por `Guest.withCpf`).

**O que fica sem rede:**
- a linha "Discount:" do booking receipt (os dois ramos não cobertos de `hotel.documents` são os de desconto da fatura
  e do recibo em inglês);
- o separador de milhar "BRL 1,234.56": nenhum teste usa valor acima de mil, então trocar `Locale.US` em
  `EnglishFormat.java:18` não derrubaria nada;
- o arredondamento do ISS: 779,80 × 5% dá exatamente 38,99, então trocar o modo de arredondamento passaria;
- o passaporte sem país;
- uma identificação completa somada a outra incompleta, justamente o caso que a API aceita errado (ver
  Comportamento).

A taxa de 5% está protegida: trocar 0.05 por outra taxa derruba o teste de `:178`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`, nenhuma chave
estrangeira sem índice. As migrações são 2: `V1__create_tables.sql` está igual à da base e `V2__add_guest_types.sql` é
nova. As colunas `status`, `rate_plan`, `type` e `channel` são texto sem CHECK, como já eram na base
(`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: criou uma migração nova sem editar a V1
(`final/src/main/resources/db/migration/V2__add_guest_types.sql:4-18`). Ela tira o `NOT NULL` de `guest_cpf`,
acrescenta as quatro colunas e protege no banco a regra de "exatamente uma identificação completa" com o CHECK
`reservations_one_guest_type` (`:11-18`). Reservas antigas, que têm só CPF, satisfazem o CHECK, então a migração aplica
sobre um banco com dados. O INSERT e o SELECT novos usam parâmetros `?` (`ReservationStore.java:32-55`), e o que é
concatenado é só constante (`COLUMNS`, `:18-20`). A proteção contra reserva duplicada (EXCLUDE gist) vem da base e
continua lá. As colunas de situação sem CHECK também vêm da base.

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 05 32/32,
reservas simultâneas 2/2 (`measures/behavior.md`).
**Nota:** com problemas

O roteiro passa inteiro:
- a fatura com "ISS retido (5%): R$ 38,99";
- o booking receipt com "BRL 2,783.52", "Discount:" e "1 night";
- o documento igual no e-mail, no check-out e na segunda via, com o campo `text`;
- o comprovante de CPF igual ao da base;
- as quatro recusas do roteiro.

Fora do roteiro, chamei a API à mão e achei um caso que a regra "tem que ser um desses jeitos, e só um" não cobre:

- **CPF + CNPJ sem razão social:** a API responde 201, guarda a reserva como pessoa física e devolve `companyCnpj:
  null`. O CNPJ enviado é descartado em silêncio.
- **CNPJ e razão social + passaporte sem país:** também responde 201, como empresa, e o passaporte é descartado.

A causa está em `HttpApi.java:128-133`: uma identificação incompleta simplesmente não entra na contagem. Para o dono
da pousada, isso quer dizer que um pedido de empresa mal preenchido no site pode virar um comprovante de pessoa física
sem nenhum erro avisando. A referência do cenário pede recusar "nenhum, dois ou um incompleto". O resto do sistema
funciona como antes, e os dados sobrevivem ao reinício.

## Dúvidas

- A forma do pattern é uma fábrica concentrada: um método estático privado dentro do próprio `ReservationReceipt`,
  chamado pelo construtor. Não é um criador com o método de fábrica sobrescrito, nem um método polimórfico no `Guest`.
  A referência aceita essa forma como acerto.
- A nota `com problemas` no comportamento vem de um caso que achei à mão, não do roteiro (que passou 83/83). Dá para
  ler o texto como se "um incompleto ao lado de um completo" estivesse em aberto, e aí a nota seria `bom`. Pesei a
  referência do cenário e o fato de o dado ser descartado sem aviso.
- O README não foi atualizado com os campos novos (`final/README.md` é igual ao da base). Isso não pesou em nenhuma
  coluna.
