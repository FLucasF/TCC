# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin com 7 pacotes. `hotel.web` (`HttpApi`) lê a requisição e chama `ReservationService` em
`hotel.reservations`. O serviço aplica as regras e usa `PriceCalculator` para o preço e `ReservationStore` (JDBC com
pool) para o PostgreSQL, cujo esquema é criado por migrações do Flyway. Para criar os três planos novos, o agente
estendeu o `enum RatePlan` (`RatePlan.java:8-13`) e fez crescer os dois `switch` de comportamento que a base já tinha:
o do preço (`PriceCalculator.java:27-33`) e o da devolução (`ReservationService.java:126-131`). Também criou um
terceiro `switch`, o da validação (`ReservationService.java:58-84`), e duas tabelas estáticas, `CompanyDiscount` e
`WeekendPackagePrice`. Acrescentou ainda o campo `companyCode` do pedido até o banco e uma migração nova,
`V2__add_company_code.sql`. O sistema faz tudo o que o texto pede: 72 de 72 conferências passaram, e os 43 testes
também. O defeito mais sério é de design. O texto avisa que "o comercial vive inventando plano", e o agente não deu a
cada plano um lugar próprio. Cada plano novo continua exigindo mudanças em três `switch` espalhados por duas classes.
Além disso, o laço das diárias foi copiado quatro vezes em `PriceCalculator`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reservations` passou de 13 classes e 508 linhas para 15 classes e 658 linhas. `ReservationService` passou a ser
a maior classe, com 184 linhas (tinha 138); `PriceCalculator` foi de 53 para 106 linhas.
**Nota:** bom

O agente respeitou as camadas da base:
- **A borda HTTP continua fina.** A mudança lá só passa o campo novo adiante (`HttpApi.java:128-135`,
  `CreateReservationRequest.java:16`).
- **As regras ficaram em `hotel.reservations`,** sem Javalin e sem SQL: `ReservationService.java:58-92` e
  `PriceCalculator.java:25-83`.
- **O SQL continua só em `ReservationStore`** (`ReservationStore.java:31-33` e `:50`).

Nenhuma dependência nova entre pacotes e nenhum ciclo novo; o ciclo `documents` ↔ `reservations` veio da base. O
único ponto a observar é o crescimento de `ReservationService`: as regras de elegibilidade de cada plano ficaram
dentro dele (`ReservationService.java:58-92`). Isso é sobretudo questão de design (ver abaixo), e o tamanho ainda é
aceitável.

## Design de baixo nível

**Números:** cinco planos e nenhuma estrutura por plano. O comportamento dos planos é decidido em 3 `switch` sobre
`RatePlan`:
- o preço, em `PriceCalculator.java:27-33` (5 casos);
- a validação, em `ReservationService.java:59-83` (5 casos);
- a devolução, em `ReservationService.java:126-131` (5 casos).

Dos 5 planos, 0 estão numa estratégia própria.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido.** O texto pede um Strategy: cada plano com o seu preço, a sua regra de uso e a sua devolução, e
o aviso de que virão mais planos. O agente não criou essa estrutura. `RatePlan` continua um `enum` sem comportamento
(`RatePlan.java:3-14`). Os dois `switch` de comportamento da base cresceram para os cinco casos:
- `PriceCalculator.java:27-33` despacha para um método privado por plano;
- `ReservationService.java:126-131` decide a devolução.

O agente ainda criou um terceiro `switch`, com `if` por plano, para as regras de uso:
- sexta a domingo, em `ReservationService.java:67-73` e `:86-92`;
- 7 noites, em `:74-80`;
- o código da empresa, em `:60-66`.

O próximo plano ("melhor idade", "60 dias de antecedência") vai exigir mudanças nos três lugares, em duas classes.
Segundo a referência, isso é exatamente a **omissão**. `CompanyDiscount` e `WeekendPackagePrice` são só tabelas de
consulta, e não estratégias de plano.

**O exagero.** Não há estrutura sem necessidade. `CompanyDiscount` (`CompanyDiscount.java:6-20`) e
`WeekendPackagePrice` (`WeekendPackagePrice.java:7-17`) são mapas pequenos que guardam dados que o texto dá. Não há
interface sem uso, fábrica nem camada que só repassa: **contenção**.

**O resto do código:**
- **Duplicação.** O laço que soma as diárias (`for … nightPrice … nights++`) foi copiado quatro vezes:
  `PriceCalculator.java:37-42`, `:47-52`, `:58-63` e `:75-80`. A base tinha esse laço uma vez só. Se a regra da
  temporada mudar, as quatro cópias continuam corretas, porque todas chamam `nightPrice`. Mas qualquer mudança na
  contagem de noites ou no subtotal terá de ser feita em quatro lugares.
- **`null` sem cuidado.** A sobrecarga pública `calculate(room, in, out, plan)` passa `companyCode = null`
  (`PriceCalculator.java:21-23`). Com `CORPORATE`, `CompanyDiscount.discountFor(null)` devolve `null`, e
  `subtotal.multiply(null)` lança `NullPointerException` (`PriceCalculator.java:64-65`). O cálculo só é seguro porque
  o serviço valida o código antes (`ReservationService.java:51`). A regra de uso e o preço do mesmo plano moram em
  classes diferentes, e uma depende da outra sem nada que as ligue.
- **Pacote sem as datas.** `calculateWeekendPackage` recebe `checkIn`/`checkOut` e não os usa
  (`PriceCalculator.java:69-72`). Também fixa `2` noites em vez de contá-las e confia que o serviço já validou as
  datas.
- **Detalhes menores.** O import `java.time.DayOfWeek` não é usado em `PriceCalculator.java:6`. O `switch` de
  validação usa a forma antiga, com `case …:`/`break` (`ReservationService.java:59-83`), enquanto o resto do código
  usa a forma com seta. A metade do pacote é calculada com `divide(…, 2, HALF_EVEN)` seguida de `cents(...)`
  redundante (`ReservationService.java:129-130`).

Os tipos de domínio estão certos: `BigDecimal` com 2 casas e `HALF_EVEN`, `LocalDate` e `enum`. São defeitos
localizados, mas a duplicação é nova e real: **com problemas**.

## Testes

**Números:** 43 testes rodaram, 0 falhas, 0 erros e 0 pulados; há 43 métodos de teste (a base tinha 26, e o agente
acrescentou 17 sem apagar nenhum). Cobertura de linhas de 94,2% (532/565) e de ramos de 89,4% (127/142); na base eram
93,8% e 87,1%. Em `hotel.reservations`, a cobertura é de 265/280 linhas e 75/78 ramos (`measures/tests.md`).
**Nota:** com problemas

Os testes novos conferem, com valores, os três exemplos do financeiro, no cálculo e no serviço:
- Tecnosul 779,80 / 93,58 / 686,22: `PriceCalculatorTest.java:78-91` e `ReservationServiceTest.java:177-182`;
- pacote deluxe 820,00 e devolução de 410,00: `ReservationServiceTest.java:201-213`;
- estadia longa 1.749,30 / 174,93 / 1.574,37: `PriceCalculatorTest.java:115-124` e
  `ReservationServiceTest.java:222-227`.

Também conferem a estadia longa com temporada (`PriceCalculatorTest.java:127-138`) e as recusas com o código
`INVALID_REQUEST`:
- convênio sem código e com código inválido: `ReservationServiceTest.java:169-174`;
- pacote fora de sexta a domingo: `:193-198`;
- estadia longa com 6 noites: `:216-219`.

Pela API, há a criação e a recusa com o código `INVALID_REQUEST` (`HttpApiTest.java:124-172`). Os testes da base
continuam passando; só foram ajustados para o campo novo (`ReportsTest.java:33` e `ReservationServiceTest.java:33-42`).

A pergunta da mutação mostra regras novas sem rede:
- **Agrovale e Farmavida.** Trocar 0.18 por 0.81, ou 0.25 por qualquer outro valor, em `CompanyDiscount.java:9-11`
  não derruba nenhum teste. A Agrovale só aparece num teste de devolução que compara `refund` com `amountPaid`
  (`ReservationServiceTest.java:185-190`), e a Farmavida não aparece em nenhum.
- **O preço do pacote na suíte** (R$ 1.200,00) nunca é conferido (`PriceCalculatorTest.java:94-112` só testa o
  standard e o deluxe).
- **A exigência de exatamente 2 noites.** Se alguém apagasse a conferência em `ReservationService.java:87-89`, nenhum
  teste falharia: os casos recusados começam numa quarta-feira (`ReservationServiceTest.java:193-198`), e a sexta a
  segunda não é testada.
- **O pacote na alta temporada,** que deveria continuar com o preço fechado, não é testado.
- **O `companyCode` ignorado nos outros planos** não é testado.

As devoluções do convênio e da estadia longa só comparam `refund` com `amountPaid`
(`ReservationServiceTest.java:190` e `:234`), sem valor esperado. Os testes são bons e passam, mas deixam parte das
regras novas sem conferir.

## Banco

**Números:** 3 tabelas, todas com chave primária, e 2 migrações do Flyway. A `V1` é igual à da base; a `V2` é nova e
acrescenta só `ALTER TABLE reservations ADD COLUMN company_code text` (`V2__add_company_code.sql:1`). O dinheiro está
todo em `numeric(10,2)`. Não há chave estrangeira sem índice nem data em texto, e a restrição `EXCLUDE` contra a dupla
reserva continua. As únicas conferências que acusaram algo foram `rate_plan`, `status`, `type` e `channel` sem
`CHECK`, todas vindas da base (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a que já estava aplicada, e uma coluna
anulável, que não quebra as reservas antigas. O SQL continua com parâmetros (`ReservationStore.java:31-33` e `:50`).
A leitura usa `row.getString("company_code")` (`ReservationStore.java:133`), e as conexões continuam em
try-with-resources com pool.

Dois pontos pequenos:
- O código da empresa é gravado em qualquer plano (`ReservationStore.java:50`), mesmo quando o texto diz que ele deve
  ser ignorado. Uma reserva STANDARD pode ficar com `company_code` preenchido, o que não altera preço nem devolução.
- `rate_plan` continua sem `CHECK` para os valores do `enum`, como na base.

## Comportamento

**Números:** 72 de 72 conferências passaram e nenhuma falhou (`measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 01: 21/21;
- reservas simultâneas: 2/2.

**Nota:** bom

Para o dono da pousada, os três planos funcionam como o texto pede:
- **Convênio.** Tecnosul 686,22, Agrovale 639,44 e Farmavida 584,85. Sem o código, ou com um código que não existe, a
  resposta é 400 `INVALID_REQUEST`. Se cancelar depois de pagar, recebe tudo de volta.
- **Pacote.** 520,00, 820,00 e 1.200,00, inclusive em julho. A devolução é de 410,00. Sábado a segunda e sexta a
  segunda são recusados.
- **Estadia longa.** 1.574,37, a temporada é aplicada nas noites de temporada, 6 noites são recusadas e a devolução é
  integral.

O `companyCode` é ignorado no STANDARD, e o NON_REFUNDABLE e a parte comum continuam como antes. Duas reservas ao
mesmo tempo nunca são criadas juntas.

Um detalhe fora do roteiro, que não muda a nota: a validação do plano roda depois da verificação de quarto ocupado
(`ReservationService.java:46-51`). Por isso, um convênio sem código num quarto ocupado recebe 409 `ROOM_UNAVAILABLE`
em vez de 400. O texto não define essa ordem.

## Dúvidas

A classe de design ficou entre `omissão` e `malfeito`. Escolhi `omissão` porque não há nenhuma estrutura por plano:
`CompanyDiscount` e `WeekendPackagePrice` são tabelas de dados, não o comportamento do plano, e todo o comportamento
continua em `switch` sobre o `enum`. Na nota de testes, `com problemas` contra `bom` também é uma decisão no limite.
Os três exemplos do texto estão testados com valores, mas dois dos três descontos de empresa, o preço da suíte e a
regra de exatamente duas noites ficaram sem nenhum teste que os confira.
