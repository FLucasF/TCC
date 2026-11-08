# Revisão: manutenção, cenário 01

## Visão geral

O projeto é o da base, com a mesma divisão em 7 pacotes: `hotel.web` (as rotas do Javalin em `HttpApi`), `hotel.reservations` (o serviço, o cálculo do preço e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`, e a montagem em `hotel`. Uma requisição entra em `HttpApi`, que monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:128-135`). Ela passa por `ReservationService.create`, que pede o preço a `PriceCalculator.calculate`, e é gravada no PostgreSQL por JDBC em `ReservationStore`. O esquema vem das migrações do Flyway. O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` `RatePlan`, levou o `companyCode` da requisição até o banco e criou uma migração nova (`V2__add_company_code.sql`). O comportamento está certo: 72 de 72 conferências passaram e os 48 testes passam. O mais sério está no design: o pedido avisa que o comercial "vive inventando plano", e mesmo assim os `switch` sobre o plano que já existiam na base só cresceram. Com isso, cada plano novo continua mexendo no cálculo do preço e no cancelamento, e o `default` do preço esconde um plano esquecido.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais na base e na final (`measures/dependencies.md` e `measures/dependencies-base.md`). `hotel.reservations` passou de 508 para 600 linhas, e `PriceCalculator` de 53 para 123. Nenhuma classe nem pacote novo.
**Nota:** bom

A mudança respeita as camadas da base:

- **A borda HTTP:** só ganhou o campo `companyCode` (`final/src/main/java/hotel/web/CreateReservationRequest.java:16`) e o repasse dele (`final/src/main/java/hotel/web/HttpApi.java:135`). Nenhuma regra de plano entrou no handler.
- **As regras:** a validação dos planos e o preço estão em `PriceCalculator` (`final/src/main/java/hotel/reservations/PriceCalculator.java:59-101`), que não conhece Javalin nem JDBC. A devolução está no serviço (`final/src/main/java/hotel/reservations/ReservationService.java:83-87`).
- **O banco:** o SQL continua isolado em `ReservationStore` (`final/src/main/java/hotel/reservations/ReservationStore.java:31-44`, `:133`).
- **O ciclo:** `ReservationService` importa `hotel.documents.ReservationReceipt` (`final/src/main/java/hotel/reservations/ReservationService.java:3`) e o recibo lê a `Reservation`. O ciclo vem da base, e o agente não o piorou.
- **A montagem:** continua num lugar só (`hotel`), sem mudança.

## Design de baixo nível

**Números:** 5 planos, 0 numa estrutura própria. O comportamento dos planos é decidido por 3 `switch` sobre `RatePlan`:

- o que escolhe o cálculo (`PriceCalculator.java:35-40`);
- o desconto do normal e do não reembolsável (`PriceCalculator.java:51-55`);
- a devolução (`ReservationService.java:83-87`).

Fora disso, o laço das noites aparece copiado 3 vezes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede, sem dizer o nome, o Strategy: cada plano com o seu preço, a sua validação e a sua devolução num lugar só dele. Isso inclui os dois planos que já existiam. O agente fez o contrário e estendeu os `switch` da base:

- **O `enum` continua sem comportamento.** `RatePlan` só ganhou três constantes com comentário (`final/src/main/java/hotel/reservations/RatePlan.java:8-13`).
- **O preço é escolhido por um `switch` de comportamento.** `PriceCalculator.calculate` faz um `switch (ratePlan)` que chama um método privado por plano (`PriceCalculator.java:35-40`). O normal e o não reembolsável caem num `default` que leva a um segundo `switch` sobre o plano (`PriceCalculator.java:51-55`).
- **A validação de cada plano fica no calculador.** O código da empresa (`:60`), a sexta-feira e o domingo (`:77`, `:82`) e as 7 noites (`:96`) estão em `if` dentro de `PriceCalculator`, e não no plano.
- **A devolução é outro `switch`.** Ela continua num `switch` no serviço, agora com os cinco casos (`ReservationService.java:83-87`).
- **Os dados dos planos são constantes soltas.** Os descontos dos convênios e os preços do pacote ficam no calculador (`PriceCalculator.java:21-31`).

Os métodos privados por plano não formam uma estrutura: nada pode ser trocado nem acrescentado sem editar os dois `switch` do preço e o `switch` da devolução. O plano da melhor idade, que o texto já anuncia, vai mexer de novo em três lugares de duas classes. Nos critérios da referência, isso é **omissão**.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. Fica em **contenção**.

**O resto do código:**

- **Duplicação.** O laço que soma as noites com a temporada aparece três vezes, igual (`PriceCalculator.java:45-50`, `:65-70`, `:90-95`). O cálculo "desconto = cents(subtotal × taxa); total = subtotal − desconto" também se repete (`:51-56`, `:71-73`, `:99-100`).
- **Os `default` escondem um plano esquecido.** Os `default` em `PriceCalculator.java:39` e `:54` tiram do compilador a checagem de que todos os casos foram tratados. Um plano novo esquecido no `calculate` sai silenciosamente com o preço cheio e desconto zero. O `switch` da devolução (`ReservationService.java:83`) não tem `default` e ainda avisaria.
- **Um parâmetro que só um plano usa.** O `companyCode` virou parâmetro de `calculate` para todos os planos (`PriceCalculator.java:34`). Ele é um `String` solto, e só o convênio o usa.
- **Detalhes de estilo.** `java.time.temporal.ChronoUnit` vem com o nome completo em vez de import (`PriceCalculator.java:81`), e há uma linha longa fora do formato do resto (`ReservationService.java:47`).

O dinheiro continua em `BigDecimal` com HALF_EVEN, e a metade do pacote também (`ReservationService.java:86`). Os erros usam `HotelException` com `INVALID_REQUEST`, como o pedido manda.

## Testes

**Números:** final com 48 testes rodados, 0 falhas, 0 erros e 0 pulados (48 métodos escritos). A base tinha 26, e o agente acrescentou 22. Cobertura de linhas de 94,0% (518/551) e de ramos de 87,7% (121/138), contra 93,8% e 87,1% na base. No pacote `hotel.reservations`: 251/266 linhas e 69/74 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente manteve os 26 testes da base. Só acrescentou o argumento `null` na chamada (por exemplo `final/src/test/java/hotel/reservations/ReservationServiceTest.java:36`), sem enfraquecer nenhum. A parte nova é conferida com valores, em três níveis:

- **Unidade, no cálculo do preço:**
  - os três convênios: TECNOSUL 93,58/686,22 com os números do financeiro, AGROVALE 44,98 e FARMAVIDA 62,48 (`final/src/test/java/hotel/reservations/PriceCalculatorTest.java:79`, e os dois testes seguintes);
  - o convênio sem código e com código inválido (`PriceCalculatorTest.java:127`, `:136`);
  - os três preços do pacote, com 820,00 no deluxe (`PriceCalculatorTest.java:149`, e os dois seguintes);
  - o pacote fora de sexta e fora de domingo (`:186`, `:199`);
  - a estadia longa com 1.749,30/174,93/1.574,37 (`:212`), a recusa com 6 noites (`:228`) e a temporada antes do desconto (`:237`).
- **Serviço contra o banco:** a devolução de cada plano, com os 410,00 do pacote (`ReservationServiceTest.java:164`, `:183`, `:204`).
- **API:** os três planos, com o 400 `INVALID_REQUEST` das recusas (`final/src/test/java/hotel/web/HttpApiTest.java:124-207`).

A pergunta da mutação:

- **Pegas pelos testes.** Trocar 0.12 por 0.21, `< 7` por `<= 7`, ou a metade do pacote por outra fração faria um teste falhar.
- **Sem rede.** Apagar a checagem de "exatamente 2 noites" (`PriceCalculator.java:82-84`) passaria: nenhum teste reserva o pacote de uma sexta a um domingo de outra semana. Também não há teste de unidade do pacote em alta temporada (o roteiro do comportamento confere esse caso).

Os testes da API comparam dinheiro com `asDouble` (`HttpApiTest.java:135-137`), que é mais fraco que comparar o texto. São lacunas pequenas, que não mudam a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária, e todo o dinheiro em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data em texto. As colunas de situação e de plano (`rate_plan`, `status`, `type`, `channel`) não têm CHECK. Há 2 migrações: `V1` igual à da base e `V2` nova. O serviço subiu contra um banco vazio (`measures/database.md`).
**Nota:** bom

- **A migração.** O agente acrescentou a coluna numa migração nova e não editou a da base (`final/src/main/resources/db/migration/V2__add_company_code.sql:2`). Quem já roda o sistema sobe sem problema. A coluna `company_code text` aceita nulo, o que está certo porque só o convênio a usa.
- **O SQL.** Continua com parâmetros (`final/src/main/java/hotel/reservations/ReservationStore.java:31-44`). Os trechos montados com `+` só juntam constantes (`COLUMNS`, `:18-19`).
- **A reserva dupla.** Continua protegida pela restrição `EXCLUDE` da base.
- **O que veio da base.** A falta de CHECK em `rate_plan` e `status` já existia, e o agente não a piorou.
- **Um ponto pequeno.** O código da empresa é gravado mesmo quando o plano não é o convênio (`ReservationStore.java:44` grava `request.companyCode()` sem olhar o plano). Uma reserva STANDARD pode ficar com `company_code = 'TECNOSUL'` no banco, o que confunde quem ler a tabela. O preço ignora o código, como o pedido manda.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam (`measures/behavior.md`):

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- **O convênio.** Os três convênios dão os descontos certos (686,22, 639,44, 584,85). A reserva sem código ou com código inexistente é recusada com 400 `INVALID_REQUEST`, e o convênio devolve tudo no cancelamento.
- **O pacote.** Custa 520,00, 820,00 e 1.200,00, também em julho. Fora de sexta a domingo ele é recusado, e o cancelamento devolve a metade (410,00).
- **A estadia longa.** Dá 1.574,37 no exemplo do financeiro e aplica a temporada nas noites de temporada. Com 6 noites ela é recusada, e o cancelamento devolve tudo.
- **O `companyCode` nos outros planos.** É ignorado no preço.
- **O que já existia.** O normal e o não reembolsável continuam como antes, os dados sobrevivem ao reinício e duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- **A classe do pattern.** A escolha entre omissão e malfeito: o agente separou um método privado por plano novo (`PriceCalculator.java:59`, `:76`, `:89`), o que lembra uma estrutura. Mas nada é escolhido sem `switch` de comportamento, os dois planos da base continuam num `switch` interno (`:51-55`) e a devolução é outro `switch` (`ReservationService.java:83-87`). Como não existe estrutura de plano nenhuma, a evidência sustenta melhor a omissão.
- **O código da empresa gravado em todos os planos.** Ele é gravado também nos planos que não são o convênio (`ReservationStore.java:44`). É uma regra de plano pequena fora do plano e não muda a classe. O pedido diz que nesses planos o código "pode ignorar", o que vale para o preço; se isso também vale para o que fica gravado, o texto não fecha.
