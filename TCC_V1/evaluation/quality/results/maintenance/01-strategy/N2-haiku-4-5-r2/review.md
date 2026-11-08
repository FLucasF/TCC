# Revisão: manutenção, cenário 01

## Visão geral

O projeto (Javalin + PostgreSQL com Flyway) tem sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations`
(as regras: `ReservationService`, `PriceCalculator`, e o acesso ao banco em `ReservationStore`), `hotel.rooms`,
`hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em `hotel.Hotel`. Uma criação de reserva entra por
`HttpApi` (que lê o corpo e monta a `NewReservation`, `src/main/java/hotel/web/HttpApi.java:127-135`), passa por
`ReservationService.create`, e é gravada por `ReservationStore.add` com SQL parametrizado na tabela `reservations`.
O agente resolveu os três planos novos com uma interface `RatePlanStrategy` (validação, preço e devolução) e uma
implementação por plano, os dois antigos inclusive, escolhida por um mapa em `RatePlanStrategies`. O serviço passou a
pedir ao plano a validação, o preço e a devolução. As 72 conferências do roteiro passam e os 37 testes também. Nada é
sério. Os defeitos são de acabamento: o `PriceCalculator.calculate` antigo, com o `switch` por plano, ficou como
código morto com um `default` que zera o desconto. O laço das noites ficou duplicado, há um campo sem uso, o pacote
devolve `"discount": 0` em vez de `0.00` na criação, e os testes deixam sem conferir parte das regras novas (a
Farmavida, os preços do pacote no standard e no deluxe, a saída no domingo).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.reservations` passou de 13 classes e 508 linhas
para 19 classes e 771 linhas. `hotel.web` passou de 236 para 243 linhas.
**Nota:** bom

A mudança não mexeu nas dependências entre pacotes: as seis classes novas ficaram em `hotel.reservations`, ao lado do
`PriceCalculator` e do serviço, e nenhuma conhece o Javalin ou o JDBC (`measures/dependencies.md`, a tabela de
bibliotecas). A borda HTTP continua fina. A única mudança nela é repassar o `companyCode` do corpo para a
`NewReservation` (`src/main/java/hotel/web/HttpApi.java:127-135`, `src/main/java/hotel/web/CreateReservationRequest.java:16`).
A regra do convênio não foi parar no handler. O ciclo `documents` ↔ `reservations` veio da base: o agente não o criou
nem o piorou.

Dois pontos pequenos, que não mudam a nota:
- `ReservationService` cria o seu `RatePlanStrategies` por dentro, no construtor
  (`src/main/java/hotel/reservations/ReservationService.java:26`), em vez de recebê-lo da montagem em
  `src/main/java/hotel/Hotel.java:25`. Num sistema deste tamanho, e com os planos sendo do próprio pacote, é aceitável.
- O campo `priceCalculator` do serviço ficou sem uso (`ReservationService.java:16` e `:24`); ele só serve para
  construir as estratégias.

## Design de baixo nível

**Números:** os 5 planos (os 2 que já existiam e os 3 novos) estão na estrutura: 5 implementações de `RatePlanStrategy`,
escolhidas num único mapa (`RatePlanStrategies.java:11-16`). Nenhum `switch` ou `if` de comportamento por plano está
no caminho de produção. O `switch` antigo de desconto sobrevive em `PriceCalculator.calculate`
(`PriceCalculator.java:19-32`), mas só os testes antigos o chamam.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `RatePlanStrategy` (`src/main/java/hotel/reservations/RatePlanStrategy.java:7-22`) reúne as três coisas
que o texto diz que cada plano traz: quem pode usar (`validate`), o preço (`calculatePrice`) e a devolução
(`calculateRefund`). Cada plano tem a sua classe com as três regras:
- o convênio, com o mapa das empresas e a recusa sem código ou com código desconhecido
  (`CorporatePlanStrategy.java:9-12`, `:21-32`);
- o pacote, com sexta a domingo, o preço fechado por tipo de quarto e a devolução de metade
  (`WeekendPackagePlanStrategy.java:13-16`, `:21-40`, `:49-51`);
- a estadia longa, com o mínimo de 7 noites e os 10% (`LongStayPlanStrategy.java:17-24`);
- o normal e o não reembolsável, que também viraram estratégias (`StandardPlanStrategy.java`,
  `NonRefundablePlanStrategy.java`). Isso é o que a referência pede para os casos que já existiam.

O serviço deixou de nomear os planos. A criação pede ao plano a validação e o preço
(`ReservationService.java:48-50`), e o cancelamento pede a devolução (`ReservationService.java:86-87`); o `switch` de
reembolso da base sumiu. O próximo plano (melhor idade, antecedência de 60 dias) pede uma classe nova, uma linha no
mapa e uma constante no `enum`.

**O exagero.** Não há estrutura sem necessidade. A interface tem cinco implementações, e o `RatePlanStrategies` é o
ponto único de seleção que o pattern pede, não uma fábrica a mais.

**O resto do código** (o que puxa a nota para baixo):
- **Código morto com armadilha.** `PriceCalculator.calculate` (`PriceCalculator.java:19-32`) não é mais chamado em
  produção, e o agente o manteve acrescentando `default -> cents(BigDecimal.ZERO)` (`:29`). Quem chamar esse método
  para um CORPORATE ou um LONG_STAY recebe o preço cheio, sem erro. O `default` também tira do compilador o aviso de
  plano faltando no `switch`. O método deveria ter sido apagado, ou os testes antigos levados para `nightlyPrice`.
- **Duplicação.** `nightlyPrice` (`PriceCalculator.java:35-44`) copia o laço das noites de `calculate` (`:20-25`).
- **A taxa do não reembolsável ficou fora do seu plano.** A constante `NON_REFUNDABLE_DISCOUNT` continua em
  `PriceCalculator.java:17`, e `NonRefundablePlanStrategy.java:22` a busca lá.
- **O tipo do dinheiro no pacote.** O desconto é `BigDecimal.ZERO` com escala 0 (`WeekendPackagePlanStrategy.java:45`)
  e não `cents(ZERO)`, como o resto do código faz. Por isso a resposta de criação do pacote traz `"discount":0`,
  enquanto o `GET` (lido do banco) traz `0.00`. Conferi com `curl`.
- **Detalhes de acabamento:**
  - o número de noites está fixo em `2` no preço do pacote (`WeekendPackagePlanStrategy.java:45`);
  - `import java.math.RoundingMode` sem uso (`WeekendPackagePlanStrategy.java:6`);
  - `java.time.temporal.ChronoUnit` escrito por extenso em vez de importado (`LongStayPlanStrategy.java:18`,
    `WeekendPackagePlanStrategy.java:35`);
  - o campo `priceCalculator` sem uso no serviço (`ReservationService.java:16`);
  - a validação do plano roda depois da consulta de disponibilidade (`ReservationService.java:44-49`). Assim, um pacote
    pedido de quarta a domingo num quarto ocupado recebe 409 `ROOM_UNAVAILABLE` em vez de 400 `INVALID_REQUEST`.
    Também conferi com `curl`.

## Testes

**Números:** 37 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 37 métodos de teste. A base tinha 26: o agente
acrescentou 11 (9 em `ReservationServiceTest`, 2 em `PriceCalculatorTest`) e não apagou nem mudou nenhum teste da base.
A cobertura de linhas foi de 93.8% na base para 94.3% (546 de 579). A de ramos foi de 87.1% para 86.3% (113 de 131).
Em `hotel.reservations`: 279 de 294 linhas e 61 de 67 ramos.
**Nota:** com problemas

**O que está conferido com valores.** Os testes novos rodam contra o banco real, pelo serviço:
- dois exemplos do texto: o convênio Tecnosul, com 93,58 de desconto e 686,22 de total
  (`ReservationServiceTest.java:181-190`), e a estadia longa, com 1.749,30, 174,93 e 1.574,37 (`:241-249`);
- a Agrovale, pela devolução de 639,44 (`:193-205`);
- as recusas do convênio: sem código, vazio e desconhecido (`:171-178`);
- o pacote na suíte, por 1.200,00, e a devolução de metade, 600,00 (`:216-232`);
- a fronteira das 6 noites da estadia longa (`:235-238`) e a devolução total da estadia longa (`:252-...`).

**O que fica sem rede** (a pergunta da mutação):
- Trocar `0.25` da Farmavida (`CorporatePlanStrategy.java:12`) por qualquer outro valor não derruba nenhum teste.
- Trocar os preços do pacote no standard (520,00) ou no deluxe (820,00) (`WeekendPackagePlanStrategy.java:14-15`)
  também não. O exemplo do texto, o deluxe de R$ 820,00 com volta de R$ 410,00, não está nos testes.
- Apagar a conferência do domingo ou a das 2 noites (`WeekendPackagePlanStrategy.java:30-39`) não derruba nada. As duas
  recusas do pacote (`ReservationServiceTest.java:210` e `:212`) começam numa quinta (11/03) e numa quarta (10/03), e
  as duas caem na conferência da sexta. O comentário "Friday to Saturday" na `:212` está errado: 10/03/2027 é quarta.
- Que o pacote não segue a temporada, e que a estadia longa segue, não tem teste.
- Nenhum teste passa pela API com o `companyCode`, nem confere o 400 dos planos pela HTTP. `HttpApiTest` não mudou.

Os dois testes novos de `PriceCalculatorTest` (`:78-100`) conferem só a conta genérica de `nightlyPrice` com uma taxa
passada à mão e uma diária inventada (449,90). Não conferem as regras de nenhum plano. Os testes antigos do
`PriceCalculatorTest` continuam exercitando o `calculate` morto.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(10,2)`. A chave estrangeira tem índice e não há data como texto. A única migração,
`V1__create_tables.sql`, está igual à da base, e o agente não acrescentou nenhuma. O `EXCLUDE` contra reserva dupla
continua. As únicas conferências que acusaram algo são `rate_plan`, `status`, `type` e `channel` em `text` sem `CHECK`,
e isso veio da base.
**Nota:** bom

A mudança não precisava mexer no esquema para funcionar. `rate_plan` é `text` sem `CHECK`
(`src/main/resources/db/migration/V1__create_tables.sql:29`), então os três planos novos gravam sem migração. O preço
calculado (diárias, desconto, total) é gravado como na base, com os mesmos `?`
(`src/main/java/hotel/reservations/ReservationStore.java:30-52`). A devolução de cada plano depende só do valor pago
e do plano, que ficam no banco: depois de reiniciar, o cancelamento continua certo. Os defeitos que a medida acusa
(as colunas de situação e de plano sem `CHECK`, o relatório que traz a tabela inteira) vieram da base.

Um ponto de dado, que não derruba a nota: o `companyCode` não é gravado em lugar nenhum (`ReservationStore.java:31-33`
não mudou). Uma reserva CORPORATE fica no banco sem a empresa que deu o desconto. Veja as dúvidas.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- as três empresas do convênio dão 12%, 18% e 25% (686,22, 639,44 e 584,85);
- o convênio sem código, ou com um código que não existe, é recusado com 400 `INVALID_REQUEST`;
- o pacote sai 520,00, 820,00 e 1.200,00, inclusive em julho, e é recusado fora de sexta a domingo;
- a estadia longa dá 10% com a temporada valendo nas noites de temporada (1.754,30) e recusa 6 noites;
- as devoluções são de metade no pacote e de tudo no convênio e na estadia longa;
- o `companyCode` é ignorado nos outros planos, e o normal e o não reembolsável continuam como antes.

Fora do roteiro, chamando a API à mão, achei dois detalhes que não mudam a nota (já contados no design):
- a criação de um pacote responde `"discount":0` em vez de `0.00` (`WeekendPackagePlanStrategy.java:45`);
- um pacote pedido em datas inválidas num quarto já ocupado recebe 409 em vez de 400.

O código da empresa em minúsculas (`tecnosul`) é aceito, por causa do `toUpperCase` em `CorporatePlanStrategy.java:26`.
O texto não fecha esse ponto, e a escolha é razoável.

## Dúvidas

- **A classe do pattern.** O `switch` por plano que a base tinha em `PriceCalculator.calculate`
  (`PriceCalculator.java:26-30`) não foi apagado: ganhou um `default`. Como nenhum caminho de produção o chama (só
  testes antigos), dei `acerto`. Uma leitura mais rígida poderia ver aí "a estrutura convivendo com o jeito sem ela" e
  dar `malfeito`.
- **O código da empresa.** O texto não diz que ele precisa ficar guardado, e o preço e a devolução não dependem dele
  depois da criação. Mas, sem ele, a pousada não sabe por qual empresa uma reserva CORPORATE ganhou o desconto. Não
  pesei isso no banco porque o texto não pede.
