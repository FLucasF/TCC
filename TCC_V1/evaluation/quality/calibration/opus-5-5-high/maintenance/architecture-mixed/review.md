# Revisão: manutenção, cenário 01

## Visão geral

O projeto (herdado da base) tem sete pacotes. O `hotel.web` (`HttpApi`) recebe as rotas do Javalin e chama o `ReservationService` (`hotel.reservations`), que valida, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore` (JDBC com pool, PostgreSQL com migrações do Flyway). A montagem fica em `Hotel.java`. O agente trocou os dois `switch` da base (o desconto em `PriceCalculator.calculate` e a devolução em `ReservationService.cancel`) por uma interface `RatePlanPolicy` (validar, preço, devolução). Ela tem uma implementação para cada um dos cinco planos, escolhida por um `EnumMap` em `RatePlans`. Ele também acrescentou o `companyCode` na requisição, no registro `NewReservation` e numa migração nova, a `V2`. A parte pedida está bem feita e o comportamento confere inteiro (72 de 72). O que há de mais sério é uma mudança que ninguém pediu. A rota `POST /reservations/{id}/check-in` passou a fazer um `UPDATE` direto no banco, dentro do handler HTTP (`src/main/java/hotel/web/HttpApi.java:80-92`). Ela deixou de passar pelo `ReservationService.checkIn`, que agora só os testes chamam. Isso põe SQL na borda HTTP, duplica a regra do check-in e escapa da trava `synchronized` com que o serviço serializa as mudanças de situação. O README não foi atualizado e continua falando só de `STANDARD` e `NON_REFUNDABLE` (`README.md:32-38`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O que mudou: o `hotel.web` passou a usar JDBC (a coluna "JDBC" de `hotel.web` é "sim" na final e vazia na base). O `hotel.reservations` passou de 13 para 20 classes (de 508 para 731 linhas), e o `HttpApi` de 170 para 187 linhas.
**Nota:** com problemas

- A parte pedida ficou no lugar certo. As regras dos planos estão em `hotel.reservations` (`RatePlanPolicy.java:7-15` e uma classe por plano), sem Javalin nem JDBC. A montagem continua num lugar só: `Hotel.java:25-29` cria o `RatePlans` e o passa ao `PriceCalculator` e ao `ReservationService`. Na web, a mudança pedida é só ler o campo novo (`CreateReservationRequest.java:16`, `HttpApi.java:145-146`).
- A mistura nova é o check-in. `HttpApi.java:82-90` abre uma conexão e roda `UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ? AND status = 'CONFIRMED'` dentro da rota. Para isso, o `HttpApi` passou a receber o `DataSource` (`HttpApi.java:30`, `:38`, `:44`; `Hotel.java:29`). A regra "só uma reserva confirmada faz check-in" agora mora no handler, e o acesso ao banco escapa do `ReservationStore`. O `ReservationService.checkIn` (`ReservationService.java:90-98`) continua lá, mas nenhum código de produção o chama (só `ReservationServiceTest.java:129-141` e `ReportsTest.java:53`). O pedido não tinha nada sobre o check-in.
- O ciclo `documents` ↔ `reservations` veio da base e não piorou.

É uma mistura localizada (um handler com SQL e uma regra), e por isso a nota é "com problemas", não "ruim".

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os dois que já existiam e os três novos). Nenhum `switch` nem `if` sobre o plano fora dela. Há um único ponto de seleção (`RatePlans.java:11-21`). Preço, validação e devolução ficam cada um dentro do plano.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Strategy).** É acerto:
- `RatePlanPolicy` (`RatePlanPolicy.java:7-15`) reúne as três decisões de um plano: `validate`, `price` e `refund`.
- Cada plano tem a sua classe:
  - `StandardPlan.java:6-21`;
  - `NonRefundablePlan.java:6-23`;
  - `CorporatePlan.java:8-32`, com o mapa das empresas em `:10-13` e a recusa em `:17-20`;
  - `WeekendPackagePlan.java:10-38`, com sexta a domingo em `:20-25`, o preço fechado em `:29-32` e a metade em `:36`;
  - `LongStayPlan.java:8-29`, com as 7 noites em `:15-17`.
- Os dois `switch` da base sumiram. `PriceCalculator.calculate` só pede ao plano (`PriceCalculator.java:25-29`), e `ReservationService.cancel` também (`ReservationService.java:84`). Nenhum dos dois nomeia um plano.
- A escolha é um `EnumMap` montado num lugar só (`RatePlans.java:9-21`), que a referência aceita.
- O cálculo comum das diárias com a temporada ficou fora dos planos (`PriceCalculator.java:31-47`, `NightlyCharges.java:6`). O desconto percentual, comum a quatro planos, está num ajudante (`PercentOff.java:10-13`). Os dois evitam duplicação sem decidir por plano.
- Um próximo plano é uma constante no `enum` e uma linha em `RatePlans`.

**O exagero.** É contenção. `NightlyCharges` (um `record` de dois campos) e `PercentOff` (um método) são pequenos e usados por vários planos. `RatePlans` é o ponto de seleção que o pattern pede. Não há fábrica, builder nem camada que só repassa.

**O resto do código.** A nota é "com problemas" por causa do que o agente acrescentou fora do pedido:
- **Duplicação da regra do check-in:** a mesma transição de CONFIRMED para CHECKED_IN está em `HttpApi.java:84` (em SQL, com o status escrito como texto solto `'CHECKED_IN'`/`'CONFIRMED'`) e em `ReservationService.java:90-98` (com o `enum`). Esta segunda virou código morto em produção. Quem mudar a regra num lugar vai esquecer o outro.
- **`companyCode` gravado em qualquer plano:** o pedido diz que, nos outros planos, o código "pode ignorar". O preço o ignora, mas `ReservationStore.java:50` o grava sempre. Uma reserva `STANDARD` com `"companyCode": "TECNOSUL"` fica com `company_code = 'TECNOSUL'` no banco (conferido chamando a API).
- **Plano sem estratégia:** `RatePlans.policyFor` (`RatePlans.java:19-21`) devolve `null` para um plano que não foi registrado, e o erro apareceria como `NullPointerException` em `PriceCalculator.java:27`. É um risco pequeno, porque o construtor registra todos os cinco.
- **Pontos menores:**
  - os planos usam `PriceCalculator.cents` (`NonRefundablePlan.java:21`, `WeekendPackagePlan.java:31,36`, `PercentOff.java:11`), um acoplamento ao calculador só pelo arredondamento;
  - o pacote calcula as diárias que não usa (`PriceCalculator.java:28`, das quais `WeekendPackagePlan.java:31` só aproveita `nights()`).

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:58-60`), `LocalDate` e `DayOfWeek`. Os nomes são claros.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 34 métodos escritos (a base tinha 26, que continuam todos lá e passando). A cobertura de linhas é de 94,1% (530 de 563; a base tinha 93,8%) e a de ramos é de 87,3% (110 de 126; a base tinha 87,1%). Em `hotel.reservations`, 260 de 273 linhas e 57 de 60 ramos.
**Nota:** bom

**O que o agente acrescentou.** Um arquivo novo, `NewRatePlansTest.java`, com 7 testes contra o banco, e um teste de API (`HttpApiTest.java`, `theCompanyCodeComesInTheBody`). Os testes da base foram só adaptados à nova assinatura (`PriceCalculatorTest.java`, `ReservationServiceTest.java:24-37`, `ReportsTest.java:28-35`), sem perder asserções.

**As regras novas são conferidas com valores:**
- as três empresas, com diárias, desconto e total (`NewRatePlansTest.java:44-56`, inclusive os R$ 779,80, R$ 93,58 e R$ 686,22 do texto);
- a recusa sem código e com código desconhecido, e a devolução integral do convênio (`:59-67`);
- o pacote nos três tipos de quarto e em julho, e a devolução de R$ 410,00 (`:70-83`);
- as três recusas do pacote: sábado, 3 noites e 1 noite (`:86-93`);
- a estadia longa, com e sem temporada, e a devolução integral (`:96-107`);
- as 6 noites recusadas (`:110-113`);
- o código ignorado no `STANDARD` (`:116-121`);
- pela API, o 201 com total 686.22 e o 400 `INVALID_REQUEST` (`HttpApiTest.java:108-120`).

**A pergunta da mutação.** Trocar 0.12 por 0.21 quebra `:50-51`. Trocar `< 7` por `<= 7` quebra `:97`, porque a reserva de 7 noites passaria a ser recusada. Trocar 0.5 por 1 quebra `:82`. Tirar a checagem da sexta quebra `:87-88`.

**O que fica sem rede.** A recusa do check-in pela rota que de fato roda em produção não tem teste. O agente a reescreveu em SQL (`HttpApi.java:80-92`), mas `HttpApiTest.java:84` só confere o 200 do caminho feliz, e o teste de recusa (`ReservationServiceTest.java:137`) exercita o `ReservationService.checkIn`, que a produção não usa mais. Isso vem da mudança de arquitetura e não de uma regra nova do pedido, e por isso não baixa a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data em texto. As colunas de situação e de plano não têm CHECK (`reservations.rate_plan`, `reservations.status`), como já era na base. A dupla reserva é protegida por um `EXCLUDE USING gist`, que já existia na base. As migrações: a `V1` ficou igual e a `V2` é nova. O serviço subiu num banco vazio.
**Nota:** com problemas

**O que está certo:**
- a evolução do esquema: uma migração nova, `V2__reservation_company_code.sql` (`ALTER TABLE reservations ADD COLUMN company_code text`), sem editar a `V1`, de modo que um banco que já roda o sistema sobe sem problema;
- a coluna nova, que pode ser nula, o que está correto porque só o convênio a usa;
- o SQL com parâmetros e try-with-resources, inclusive no código novo (`ReservationStore.java:32-50`, `:133`; `HttpApi.java:82-85`).

**O que o agente piorou:**
- **Uma corrida nova entre check-in e cancelamento.** Na base, todas as mudanças de situação passavam por métodos `synchronized` do `ReservationService`, e por isso não se cruzavam dentro do processo. O check-in novo (`HttpApi.java:82-90`) grava direto no banco, fora dessa trava. O `cancel` (`ReservationService.java:78-88`) lê a reserva como CONFIRMED, calcula a devolução e grava com `UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?` (`ReservationStore.java:65-75`), sem condição sobre a situação anterior. Se um check-in cair entre a leitura e a gravação do cancelamento, a recepção recebe "CHECKED_IN" e a reserva termina CANCELLED, com a devolução registrada para um hóspede que está no quarto. A janela é pequena, mas é um risco de dado errado que a base não tinha.
- **O `company_code` gravado também nos planos em que deveria ser ignorado** (`ReservationStore.java:50`). Conferido: uma reserva `STANDARD` enviada com `TECNOSUL` ficou com `company_code = 'TECNOSUL'`. Não muda o valor cobrado, mas deixa no banco um dado que não vale nada.
- **O acesso ao banco espalhado:** além do `ReservationStore`, agora também em `HttpApi.java:82-90`. Isso já conta na arquitetura.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o texto pede funciona pela API:
- os três convênios, com os valores do financeiro (R$ 686,22 na Tecnosul, 639,44 na Agrovale e 584,85 na Farmavida);
- a recusa com 400 `INVALID_REQUEST` sem código ou com código desconhecido;
- o pacote de R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho, com devolução de R$ 410,00 e recusa de sábado a segunda e de sexta a segunda;
- a estadia longa de R$ 1.574,37 e de R$ 1.754,30 com temporada, e a recusa de 6 noites;
- o código ignorado no `STANDARD` e o `NON_REFUNDABLE` como antes.

O check-in reescrito também respondeu certo quando chamei a API à mão: 409 `OPERATION_NOT_ALLOWED` para uma reserva pendente, 404 para uma reserva que não existe e 404 para um id que não é número. Para o dono da pousada, os três planos novos funcionam como o comercial combinou, e o que já existia continua igual. A corrida entre check-in e cancelamento, descrita no Banco, não aparece no roteiro e não muda esta nota.

## Dúvidas

- O motivo de o agente reescrever o check-in em SQL dentro da rota. O pedido não toca no check-in, e a evidência só mostra a mudança, não a razão.
- O código da empresa é aceito só em maiúsculas: `tecnosul` é recusado com 400 (conferido pela API). O texto diz que o código "é o nome dela em maiúsculas", então é uma leitura razoável, mas uma pessoa digitando no site pode errar a caixa.
- Gravar o `company_code` nos planos que não são convênio é uma regra de plano pequena, fora do plano, e não mudou a classe do pattern (está contada no Banco e no Design).
