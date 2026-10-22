# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas e JSON), `hotel.reservations` (serviço, preço,
reserva e o `ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.rooms`, `hotel.messages`,
`hotel.reports`, e a montagem em `hotel`. Uma requisição entra em `HttpApi`, que monta um `NewReservation` e chama o
`ReservationService`. O serviço grava pelo `ReservationStore` num PostgreSQL com migrações do Flyway. O agente
acrescentou quatro campos de identificação ao `Guest`, ao pedido, à resposta e ao documento, mais a migração
`V2__guest_identification.sql`, e resolveu os três documentos com condicionais dentro do mesmo record
`ReservationReceipt`. Tudo o que o texto pede funciona: o roteiro passou em 83 de 83 conferências e os 30 testes
passam. O mais sério está no design. A escolha do documento não ficou numa estrutura de criação: está espalhada em
quatro `if` sobre campos nulos dentro de um único tipo. A agência de turismo, que o texto já anuncia, vai obrigar a
mexer em todos eles. Além disso, o banco e o `Guest` deixaram de garantir que o hóspede tem alguma identificação.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md` só diferem nas linhas). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que
já vinha da base. A maior classe é `HttpApi`, que passou de 170 para 185 linhas; `ReservationReceipt` passou de 55
para 113.
**Nota:** bom

A mudança respeitou as camadas da base:
- A borda HTTP só lê os campos novos e monta o `Guest` (`src/main/java/hotel/web/HttpApi.java:126-138`).
- A persistência dos campos novos ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:18-20`,
  `:32-55`, `:130-133`).
- O serviço não mudou: continua sem Javalin e sem SQL.

O ciclo `documents` ↔ `reservations` vem da base (o `ReservationService` cria o `ReservationReceipt`, que lê a
`Reservation`). O agente só o reforçou um pouco: o documento passou a chamar `PriceCalculator.cents`, que precisou
virar `public` (`src/main/java/hotel/reservations/PriceCalculator.java:50`).

Há dois pontos pequenos, que não mudam a nota:
- **Jackson no pacote de documentos.** O pacote `hotel.documents` passou a usar o Jackson, pelo `@JsonProperty("text")`
  em `src/main/java/hotel/documents/ReservationReceipt.java:64`. O documento de domínio agora sabe do formato JSON.
- **A regra "um só jeito de identificar" só existe na borda HTTP** (`HttpApi.java:126-135`). Isso segue o padrão da
  base, que também validava os campos obrigatórios ali. Mas nada abaixo da borda protege essa regra (ver o design e o
  banco).

## Design de baixo nível

**Números:** um único tipo de documento (`ReservationReceipt`) cobre os 3 casos. Não há nenhum caso numa estrutura
própria. A escolha aparece em 4 condicionais dentro do record (`ReservationReceipt.java:57-60`, `:66`, `:75`, `:90`).
Os 3 lugares de emissão (`ReservationService.java:64`, `:109`, `:118`) continuam com `new ReservationReceipt(...)`,
como na base.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede que a escolha de qual documento criar more num lugar só, e avisa que a agência de turismo
vai trazer um quarto documento. O que existe:
- O agente manteve um só record com 16 componentes, quatro deles mutuamente exclusivos e nulos conforme o caso
  (`ReservationReceipt.java:16-32`).
- O tipo de hóspede é deduzido de campos nulos, de jeitos diferentes em cada ponto:
  - `titleFor` pergunta primeiro se é empresa (`:57`);
  - `text()` pergunta primeiro se há passaporte (`:66`);
  - `portugueseText` pergunta duas vezes se há CNPJ, uma para a linha da empresa (`:75`) e outra para o ISS (`:90`).
- O construtor devolve sempre o mesmo documento, e a variação é resolvida depois, no momento de escrever o texto.

Não há fábrica, nem documento por tipo, nem um método polimórfico no hóspede. Para a agência, será preciso mexer em
`titleFor`, em `text()`, provavelmente criar mais um `xxxText()`, e acrescentar campos ao `Guest` e ao record.

O que aproxima de um acerto: o `if` não ficou repetido nos três lugares do serviço, porque todos passam pelo mesmo
construtor. A decisão está numa classe só, mas dividida em quatro condicionais em vez de uma escolha única (ver
Dúvidas).

**O exagero.** Não há estrutura sem necessidade. O agente não criou interface, fábrica ou camada nova (contenção).

**O resto do código.**
- **O hóspede é modelado com campos anuláveis.** O `Guest` virou sete `String`, sem tipo para o jeito de identificar
  e sem invariante (`src/main/java/hotel/reservations/Guest.java:4-11`). Nada impede, por exemplo, um `Guest` com
  CNPJ e passaporte juntos. Nesse caso, o título diria "Fatura de hospedagem" (`ReservationReceipt.java:57`) e o
  texto sairia em inglês (`:66`).
- **A validação é difícil de seguir.** São cinco booleanos e uma condição com três negações compostas
  (`HttpApi.java:126-132`).
- **Os textos duplicam a estrutura.** `portugueseText` e `englishText` repetem a mesma sequência de linhas, uma em
  cada língua (`ReservationReceipt.java:72-95` e `:97-112`).
- **O ISS só existe no texto.** Ele é calculado ali dentro (`:90-93`) e não vira um campo do documento.

O que está certo:
- o dinheiro continua em `BigDecimal`, com o arredondamento da base (`ReservationReceipt.java:91`);
- as datas em `LocalDate`, com o formato inglês isolado em `TextFormat` (`src/main/java/hotel/documents/TextFormat.java:25-31`).

## Testes

**Números:** 30 testes rodaram, 0 falharam e 0 foram pulados, com 30 métodos escritos; a base tinha 26. A cobertura
de linhas passou de 93,8% para 94,5% (528 de 559), e a de ramos de 87,1% para 89,4% (152 de 170). Em
`hotel.documents`: 62 de 63 linhas e 16 de 18 ramos (de `measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base sem enfraquecer nenhum e acrescentou 4 testes que conferem valores:
- **A fatura** confere o texto inteiro, inclusive o "ISS retido (5%): R$ 38,99", e confere que ela vai no e-mail
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:169-189`). Trocar a taxa de 0.05 quebraria esse teste.
- **O booking receipt** confere o texto inteiro, o assunto e o início do e-mail em português
  (`ReservationServiceTest.java:192-208`).
- **As sete combinações inválidas** de identificação dão 400 INVALID_REQUEST
  (`src/test/java/hotel/web/HttpApiTest.java:129-139`).
- **Pela API,** os campos `null` na reserva e no documento e o `text` no JSON (`HttpApiTest.java:142-159`).

Pela pergunta da mutação, três regras novas que o texto pede explicitamente ficam sem rede:
- **O milhar com vírgula** (`BRL 1,234.56`): trocar `"%,.2f"` por `"%.2f"` em `TextFormat.java:26` não quebra nenhum
  teste, porque só se testa 779.80.
- **O "1 night"** (`ReservationReceipt.java:105`).
- **A linha "Discount:"** do booking receipt (`:107-109`). São esses os 2 ramos sem cobertura do pacote.

Também não há teste de que o check-out devolve a fatura ou o booking receipt.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. A V1 está igual à da base, e a `V2__guest_identification.sql` é
nova. As conferências não acusaram chave primária faltando, dinheiro fora de `numeric(10,2)`, chave estrangeira sem
índice nem data em texto. As colunas `status` e `rate_plan` estão sem CHECK, mas isso vem da base. Há 4 candidatos a
SQL montado com `+`, todos constantes (de `measures/database.md`).
**Nota:** com problemas

**O que está certo:**
- A mudança foi feita como deve: uma migração nova, sem editar a da base
  (`src/main/resources/db/migration/V2__guest_identification.sql:2-7`).
- O SQL novo é todo parametrizado (`ReservationStore.java:32-55`).
- A leitura devolve os campos novos (`:130-133`).

**O defeito:** a migração tira o `NOT NULL` de `guest_cpf` (`V2__guest_identification.sql:3`) e não põe nada no
lugar. Não há CHECK que exija exatamente uma identificação (CPF; ou CNPJ com razão social; ou passaporte com país)
(`measures/database-schema.sql:94`, `:106-109`). Antes, o banco garantia que toda reserva tinha um documento do
hóspede. Agora aceita uma reserva sem nenhum, ou com dois. A regra só existe na borda HTTP (`HttpApi.java:131`), e o
`Guest` também não a protege. Um `CHECK (num_nonnulls(...) ...)` resolveria.

O resto vem da base, sem piora:
- o EXCLUDE que impede duas reservas do mesmo quarto na mesma noite;
- o pool de conexões e o try-with-resources.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou (de `measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **O hóspede com CPF** continua recebendo o comprovante de antes, e os campos novos vêm `null`.
- **A empresa** recebe a fatura com a empresa no lugar do CPF e o ISS de R$ 38,99. A fatura vai no e-mail, que
  continua em português, e sai também no check-out.
- **O estrangeiro** recebe o booking receipt com:
  - as datas no formato 2027-03-10;
  - os valores em `BRL`, com a vírgula do milhar (`BRL 2,783.52`);
  - "1 night" e a linha "Discount:" quando há desconto.
- **As recusas:** a reserva sem identificação, com CPF e CNPJ juntos, com CNPJ sem razão social ou com passaporte sem
  país dá 400 INVALID_REQUEST.

A parte comum, os dados depois de reiniciar e as reservas simultâneas continuam certos.

## Dúvidas

- **Omissão ou malfeito/acerto.** A referência descreve a omissão como o `if` repetido nos três lugares, e aqui ele
  não se repete: os três lugares chamam o mesmo construtor, e a decisão está toda dentro de `ReservationReceipt`.
  Mesmo assim, não há uma escolha única de qual documento criar. Há um só tipo, com a variação resolvida em quatro
  condicionais sobre campos nulos (`ReservationReceipt.java:57`, `:66`, `:75`, `:90`). Isso é "a variação ficou num
  `if`", e dei omissão. Outro revisor poderia ler o construtor como uma "fábrica" que devolve sempre o mesmo documento,
  com a escolha feita depois, o que seria malfeito. Não é nenhuma das formas de acerto: nem um criador com o método de
  fábrica sobrescrito, nem uma fábrica que escolhe entre documentos, nem `guest.issueDocument(...)`.
- **A nota da arquitetura** ficou em `bom` porque a validação da identificação na borda segue o padrão da base. Um
  revisor mais rígido poderia contar como mistura localizada o fato de essa regra não existir abaixo do HTTP. Contei
  esse ponto no design e no banco.
