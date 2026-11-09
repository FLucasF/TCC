# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em 7 pacotes. Uma requisição entra pelas rotas de `hotel.web.HttpApi`, que leem o pedido e chamam `hotel.reservations.ReservationService`. O serviço aplica as regras, calcula o preço com `PriceCalculator` e grava por `ReservationStore`, com JDBC sobre um pool. O esquema do PostgreSQL vem de uma migração do Flyway (`V1__create_tables.sql`). O pedido era pontual: o acréscimo da alta temporada passa de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 entra na alta temporada. O agente mudou só três arquivos (`diff -rq base final`, sem contar `target/`). Em `PriceCalculator.java:16`, trocou a constante para `0.25`. Em `PriceCalculator.java:48` e `:51-60`, acrescentou o método privado `isEasterWeek` com as duas semanas. Também atualizou os valores esperados nos testes da base e escreveu dois testes novos para os limites da Páscoa. Não criou nenhuma estrutura para a ideia de "um acréscimo diferente para cada temporada". As 72 conferências do roteiro passaram, e os 28 testes também. Não há defeito sério. O único ponto que ficou para trás é o `README.md:31-32`: ele ainda diz "a noite custa 20% a mais" e não cita a Páscoa.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, com 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), segundo `measures/dependencies.md`. A base tem a mesma coisa (`measures/dependencies-base.md`): a única diferença é `hotel.reservations`, que passou de 508 para 520 linhas.
**Nota:** bom

A mudança ficou dentro de `hotel.reservations.PriceCalculator`, que é onde a regra de preço já morava. Nenhuma dependência nova surgiu, e as camadas continuam como estavam. As rotas são finas e só chamam o serviço (`src/main/java/hotel/web/HttpApi.java:62-90`). O serviço não conhece o Javalin nem o SQL (`src/main/java/hotel/reservations/ReservationService.java:27-48`). O JDBC fica isolado em `ReservationStore`, `Rooms` e `SentMessages`. O ciclo vem da base, e o agente não o tocou: `ReservationService.java:3` importa `hotel.documents.ReservationReceipt`, e `src/main/java/hotel/documents/ReservationReceipt.java:3` importa `hotel.reservations.Reservation`.

## Design de baixo nível

**Números:** 1 constante alterada (`PriceCalculator.java:16`), 1 condição a mais em `isHighSeason` (`:48`) e 1 método privado novo com 2 períodos fixos (`:51-60`). Nenhuma classe, interface ou fábrica nova.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

A frase do pedido que puxava para mais estrutura era "a gente já pensou em ter um acréscimo diferente para cada temporada, mas por enquanto é o mesmo para todas". O agente não caiu nela:

- O acréscimo continua sendo uma constante só (`PriceCalculator.java:16`), usada no único lugar que calcula o preço da noite (`:36`).
- A Páscoa entrou como dado, no método que já decidia a temporada. Em `isHighSeason` (`:42-49`), é uma condição a mais que chama `isEasterWeek` (`:51-60`), um `switch` pelo ano com o intervalo fechado de cada semana. Os dois domingos estão dentro, como o pedido diz, e qualquer outro ano dá `false`.
- Não há interface de temporada, classe por temporada nem acréscimo parametrizado.

É exatamente a "contenção" de `references/controls.md` para o cenário 02: duas condições a mais no `isHighSeason`.

Um detalhe pequeno: o ano aparece duas vezes em cada caso (`case 2027` e `LocalDate.of(2027, ...)`, em `:54` e `:56`). Uma lista de períodos seria um pouco mais fácil de estender quando o dono mandar os próximos anos. Mesmo assim, o que está lá é claro e curto.

O resto do código é da base e continua bom:

- dinheiro em `BigDecimal`, arredondado a cada passo com `HALF_EVEN` (`PriceCalculator.java:62-64`);
- datas em `LocalDate`, e planos e situações em `enum`;
- erros por `HotelException` com códigos (`HttpApi.java:93-107`).

A documentação não acompanhou a mudança: o `README.md:31-32` ainda descreve a regra antiga, com 20% e sem a Páscoa. Isso é documentação, não design do código, e por isso não entra na nota. Mesmo assim, alguém precisa corrigir.

## Testes

**Números:** 28 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 28 métodos escritos. Cobertura de linhas: 478 de 510 (93,7%). Cobertura de ramos: 113 de 129 (87,6%). Na base eram 26 testes, todos passando, com 93,8% das linhas e 87,1% dos ramos. Em `PriceCalculator`, falta cobrir 1 ramo, no `switch` da linha 53 (pelo `jacoco.xml`).
**Nota:** bom

O agente manteve os 26 testes da base. Não apagou nem enfraqueceu nenhum: só trocou os valores esperados pelos do acréscimo de 25%, e todos continuam sendo valores exatos:

- `src/test/java/hotel/reservations/PriceCalculatorTest.java:36`: 562,28, que é 249,90 + 312,38, o valor que o dono deu;
- `PriceCalculatorTest.java:45`: o mesmo valor, na virada de fevereiro em ano bissexto;
- `PriceCalculatorTest.java:72-74`: o não reembolsável sobre o preço de alta temporada (2899,52, 434,93 e 2464,59);
- `src/test/java/hotel/web/HttpApiTest.java:77` e `:118`: o total da reserva e o relatório financeiro (974,76).

Os dois testes novos conferem os limites da Páscoa nos dois lados: o sábado antes, os dois domingos, um dia no meio e a segunda depois (`PriceCalculatorTest.java:78-84` para 2027 e `:87-93` para 2028).

A pergunta da mutação:

- **Acréscimo de volta para 0,20:** cinco asserções falham.
- **Um limite deslocado (`isBefore` trocado por `isAfter`, ou um dia a mais ou a menos):** os testes de limite falham.
- **A chamada a `isEasterWeek` apagada:** os testes da Páscoa falham.

O que fica sem rede: o ramo `default -> false` (`PriceCalculator.java:58`) nunca roda, porque todos os testes usam datas de 2027 ou 2028. Se ele passasse a devolver `true`, nenhum teste notaria. Também não há um teste que calcule o preço de uma noite de Páscoa com `calculate`. A regra fica coberta pela combinação de `isHighSeason` com os testes de preço, e o roteiro do comportamento confere esse preço pela API. São lacunas pequenas, que não mudam a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)`, as chaves estrangeiras têm índice e nenhuma data está guardada como texto. Há uma `EXCLUDE USING gist` contra duas reservas do mesmo quarto em noites que se cruzam. A única conferência que acusou algo foram 4 colunas de situação ou tipo em `text` sem `CHECK`. A migração da base, `V1__create_tables.sql`, está igual (`measures/database.md`).
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: nenhuma migração foi editada ou acrescentada, e nenhuma linha de SQL mudou. O que o script aponta vem da base: `status`, `rate_plan`, `type` e `channel` em `text` sem `CHECK`. É um defeito menor, porque o código grava esses campos a partir de `enum`s. Os candidatos a "SQL montado com `+`" são todos constantes, como `ReservationStore.java:33`, `:83`, `:98` e `:111`. O valor de fora vai sempre por `?`.

## Comportamento

**Números:** 72 de 72 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- acréscimo novo: 5 de 5;
- semana da Páscoa: 10 de 10;
- outros períodos que continuam: 6 de 6;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou, e o dono da pousada recebe exatamente o que pediu:

- O standard numa noite de temporada sai a R$ 312,38. O deluxe sai a R$ 487,38 e a suíte, a R$ 724,88.
- O não reembolsável aplica os 15% sobre o valor novo: desconto de R$ 46,86 e total de R$ 265,52.
- As noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada. As noites vizinhas (20/03, 29/03, 08/04 e 17/04) continuam com o preço normal.
- Os outros períodos continuam como antes: 15/12 até o fim de fevereiro, e julho inteiro.
- O resto do sistema funciona como na base: recusas, confirmação com e-mail, cancelamentos, relatórios e dados que ficam depois de reiniciar.

## Dúvidas

O `README.md:31-32` ficou desatualizado: ainda diz 20% e não cita a Páscoa. Contei isso na visão geral e no design, sem baixar nenhuma nota. Nenhuma coluna mede a documentação, e o código, os testes e a API estão certos. Um revisor mais rígido poderia pedir essa correção antes de aceitar.
