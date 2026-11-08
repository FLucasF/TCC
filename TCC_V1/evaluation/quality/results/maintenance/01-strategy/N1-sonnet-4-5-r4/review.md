# Revisão: manutenção, cenário 01

## Visão geral

O agente manteve a estrutura da base: `hotel.web.HttpApi` lê o JSON (`CreateReservationRequest`, agora com
`companyCode`), monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:128-135`) e chama
`ReservationService`. O serviço valida, pede o preço ao `PriceCalculator` (`ReservationService.java:47-48`) e grava com
`ReservationStore` (JDBC com `?`) no PostgreSQL. O esquema evolui com o Flyway, e há uma migração nova
(`V2__add_company_code.sql`). Os três planos novos funcionam pela API: as 72 conferências do roteiro passaram, e o
`mvn verify` passa com 42 testes. O problema mais sério é de design. O texto avisa que o comercial vive inventando
plano, mas o agente estendeu os dois `switch` sobre `RatePlan` que a base já tinha: o do preço, que agora traz as
regras de quem pode usar cada plano, e o da devolução. Cada plano novo vai continuar exigindo mudanças em três
lugares: o `enum`, o `PriceCalculator` e o `ReservationService`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais nos
dois lados de `measures/dependencies-base.md` e `measures/dependencies.md`. Só mudou o tamanho: `hotel.reservations`
passou de 508 para 597 linhas e `hotel.web` de 236 para 243. `PriceCalculator` passou de 53 para 124 linhas.
**Nota:** bom

A mudança não criou dependência nem ciclo novo. O ciclo entre `hotel.documents` e `hotel.reservations` veio da base
(`ReservationService.java:3` usa `ReservationReceipt`). A borda HTTP continua fina e só repassa o campo novo
(`HttpApi.java:128-135`). As regras dos planos novos ficaram no pacote de regras, sem Javalin e sem SQL
(`PriceCalculator.java:45-86`, `ReservationService.java:84-89`). A coluna nova entrou só no `ReservationStore`
(`ReservationStore.java:31-47`, `:132`), e a montagem continua num lugar só, como na base.

## Design de baixo nível

**Números:** 5 planos (os 2 da base e os 3 novos) e nenhum na estrutura de Strategy. O comportamento de cada plano
está em 2 `switch` de comportamento: o do preço e das regras de uso (`PriceCalculator.java:24-30`, mais 3 métodos
privados com `if` de validação) e o da devolução (`ReservationService.java:84-89`). Nenhuma interface, classe
abstrata nem constante do `enum` tem corpo próprio.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência chama isto de omissão: os `switch` que já existiam cresceram para os cinco casos.
`RatePlan` continua um `enum` sem comportamento (`RatePlan.java:3-14`). O preço é escolhido por
`switch (ratePlan)` em `PriceCalculator.java:24-30`, e cada caso cai num método privado da mesma classe. As regras de
quem pode usar o plano estão nesses métodos: 7 noites em `:79`, sexta a domingo em `:62-69`, o código da empresa em
`:46-55`. A devolução continua num segundo `switch` em `ReservationService.java:84-89`. O que aproxima o código do
pattern: cada plano tem o seu método de preço, e a validação fica junto do preço. Mas nada disso é uma unidade que se
possa trocar. Um plano novo (melhor idade, antecedência de 60 dias) exige mexer no `enum`, nos dois `switch` e na
assinatura de `calculate`. A assinatura já ganhou um `String companyCode` que só serve a um plano
(`PriceCalculator.java:22`) e que o serviço passa para todos os planos (`ReservationService.java:48`).

**O exagero.** Nenhum: o agente não criou interface, fábrica nem camada a mais.

**O resto do código.**
- **Duplicação:** as mesmas três linhas (somar as noites, calcular o desconto, montar o `Price`) se repetem em
  `calculateStandard`, `calculateNonRefundable`, `calculateCorporate` e `calculateLongStay`
  (`PriceCalculator.java:34-36`, `:40-42`, `:56-58`, `:83-85`). Só a taxa muda.
- **Laço repetido:** `countNights` (`PriceCalculator.java:88-94`) repete o laço de `sumNights` (`:96-102`) só para
  contar dias.
- **Tabelas no código:** as taxas das empresas (`:49-55`) e os preços do pacote (`:70-74`) estão escritos dentro de
  `switch`, sem constantes. O `LONG_STAY_DISCOUNT` virou constante (`:20`), a taxa das empresas não.
- **Código aceito em minúsculas:** `companyCode.toUpperCase()` (`:49`) não indica o `Locale` e aceita códigos em
  minúsculas, que o texto não pede.
- **Código guardado sem uso:** o `companyCode` é gravado em qualquer plano (`ReservationStore.java:47`), embora o
  texto diga que, fora do convênio, ele deve ser ignorado.
- **README desatualizado:** o README ficou igual ao da base e não fala dos planos novos (`README.md:32-37`).

Os tipos estão certos: `BigDecimal`, arredondamento `HALF_EVEN` e `LocalDate`/`DayOfWeek`.

## Testes

**Números:** 42 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 42 métodos escritos (a base tinha 26, e os 26
continuam passando). Cobertura de linhas: 94,4% (520 de 551; a base tinha 93,8%). Cobertura de ramos: 87,9% (124 de
141; a base tinha 87,1%). Em `hotel.reservations`: 253 de 266 linhas e 72 de 77 ramos (`measures/tests.md`).
**Nota:** bom

O agente testou a parte nova com valores, em dois níveis:

- **Unidade** (`PriceCalculatorTest`, contra o banco):
  - os exemplos do financeiro: 779,80 / 93,58 / 686,22 (`PriceCalculatorTest.java:80`) e 1.749,30 / 174,93 /
    1.574,37 (`:159`);
  - Agrovale e Farmavida, com o arredondamento (`:92`, `:102`);
  - o convênio sem código e com código desconhecido, conferindo `INVALID_REQUEST` (`:112`);
  - os três preços do pacote (`:125`), o pacote de sábado a segunda (`:141`) e o de três noites (`:150`);
  - a estadia longa com 6 noites (`:171`) e em alta temporada (`:179`).
- **Devolução** (`ReservationServiceTest.java:165`, `:173`, `:182`).
- **Pela API** (`HttpApiTest.java:124`, `:145`, `:172`).

A pergunta da mutação pega as trocas principais:
- trocar 0.12 por 0.21 quebra `PriceCalculatorTest.java:80`;
- trocar `nights < 7` por `<= 7` quebra o teste das 7 noites (`:159`);
- trocar a metade por outra fração quebra `ReservationServiceTest.java:173`.

Ficam sem rede:
- o pacote em alta temporada (todos os testes do pacote usam março);
- o `companyCode` ignorado nos outros planos;
- o código da empresa sem `INVALID_REQUEST` conferido pela API: `HttpApiTest.java:142` e `:153` só olham o status 400.

Os testes pela API também comparam dinheiro com `asDouble` (`HttpApiTest.java:133-135`), o que é mais fraco que
comparar `BigDecimal`. O mesmo jeito já existia na base (`:77`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira
sem índice e nenhuma data em texto. O roteiro achou `rate_plan`, `status`, `type` e `channel` sem `CHECK`; isso veio
da base. Há 2 migrações: a V1 está igual à da base e a V2 é nova (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo. Criou uma migração nova, que só acrescenta uma coluna que aceita nulo
(`src/main/resources/db/migration/V2__add_company_code.sql:2`), e não editou a V1. Um banco que já roda sobe sem
perder dado. O SQL novo usa parâmetros (`ReservationStore.java:31-47`) e a leitura traz a coluna nova (`:132`).
Continuam da base a proteção contra reserva duplicada (o `EXCLUDE USING gist` da V1) e o pool de conexões. Um ponto
pequeno: a coluna `company_code` recebe o código mesmo quando o plano não é o convênio (`ReservationStore.java:47`).
Assim, uma reserva `STANDARD` pode ficar com um código de empresa gravado, o que confunde quem ler a tabela, mas não
muda nenhum valor.

## Comportamento

**Números:** 72 de 72 conferências passaram, e nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que o pedido traz funciona pela API:
- os três convênios, com os descontos certos;
- a recusa com 400 `INVALID_REQUEST` quando o código falta ou não existe;
- o pacote com preço fechado nos três tipos de quarto, também em julho, recusado fora de sexta a domingo, com R$ 410,00
  de volta no cancelamento do deluxe;
- a estadia longa com a temporada valendo e recusada com 6 noites;
- a devolução total no convênio e na estadia longa;
- o `companyCode` ignorado no `STANDARD`;
- o `NON_REFUNDABLE` como era.

O que já funcionava continua funcionando.

## Dúvidas

- A ordem das validações: as regras dos planos são conferidas depois da disponibilidade do quarto
  (`ReservationService.java:43-48`). Por isso, um convênio sem código num quarto ocupado responde 409, não 400. O
  texto não fecha esse caso, e o roteiro não o testa.
- O código da empresa em minúsculas (`tecnosul`) é aceito (`PriceCalculator.java:49`). O texto diz que o código é o
  nome em maiúsculas, mas não diz se o código em minúsculas deve ser recusado. Tratei como escolha razoável, não como
  erro.
