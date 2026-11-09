# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes. Uma requisição chega em `hotel.web.HttpApi`, que lê o corpo e chama
`hotel.reservations.ReservationService`. O serviço valida, calcula o preço com `PriceCalculator` e grava com
`ReservationStore` (JDBC com pool e Flyway, tabelas `rooms`, `reservations` e `messages`). A montagem fica toda em
`hotel.Hotel`. O pedido era pontual: o acréscimo da alta temporada passa de 20% para 25%, e a semana da Páscoa de 2027
e de 2028 entra como alta temporada. O agente mudou só o que precisava. Em `src/main`, só o
`PriceCalculator.java` (a constante passou a valer 0,25 e uma lista de dois períodos da Páscoa entrou no
`isHighSeason`). Fora isso, mudou o README e dois arquivos de teste: atualizou os valores esperados e escreveu um teste
novo. `diff -rq base final` mostra só esses quatro arquivos. Os 27 testes passam, e o roteiro do comportamento passou
inteiro (72 de 72). Não achei nada sério. Os poucos defeitos que encontrei vieram da base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Tudo isso é
igual em `measures/dependencies-base.md`. A única diferença está em `hotel.reservations`: uma classe e 15 linhas a
mais, que são o `record` privado `EasterWeek` e a lista dentro de `PriceCalculator`.
**Nota:** bom

A mudança ficou dentro da classe que já cuidava do preço (`src/main/java/hotel/reservations/PriceCalculator.java:42-63`)
e não criou nenhuma dependência nova. As camadas da base continuam intactas:
- a borda HTTP é fina, com rotas que só leem a requisição, chamam o serviço e escrevem a resposta
  (`src/main/java/hotel/web/HttpApi.java:61-91`);
- as regras não usam Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:27-48`);
- o SQL fica isolado em `ReservationStore`;
- a montagem acontece num lugar só (`src/main/java/hotel/Hotel.java:25`).

O ciclo já existia na base: `ReservationReceipt` importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`)
e `ReservationService` importa `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`). Não
é do agente.

## Design de baixo nível

**Números:** a única mudança em `src/main` é em `PriceCalculator`:
- uma constante trocada (`0.20` → `0.25`, linha 17);
- uma lista com dois períodos (linhas 43-45);
- um `record` privado de duas datas com um `contains` (linhas 47-51);
- uma condição a mais no `isHighSeason` (linha 58).

Não há interface nova, nem classe por temporada, nem acréscimo por temporada.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este cenário é um controle negativo, e a frase que tenta o agente é "um acréscimo diferente para cada temporada". O
agente não caiu nela. O acréscimo continua sendo uma única constante (`PriceCalculator.java:17`), usada num só ponto
(`PriceCalculator.java:37`). As datas da Páscoa entraram como dado, numa lista de períodos
(`PriceCalculator.java:43-45`), que é exatamente o que `references/controls.md` chama de contenção. O `record EasterWeek`
(`PriceCalculator.java:47-51`) é privado e só guarda o par de datas com o teste "a data está no intervalo". É o formato
do dado, não uma estrutura para variar comportamento. Os períodos de dezembro a fevereiro e de julho ficaram como
estavam (`PriceCalculator.java:59-62`).

O resto do código vem da base e está limpo:
- dinheiro em `BigDecimal`, com `HALF_EVEN` concentrado em `cents` (`PriceCalculator.java:65-67`);
- `LocalDate` para as datas;
- `enum` para planos, situações e códigos de erro;
- `switch` exaustivo sobre o `enum` (`PriceCalculator.java:27-30`, `HttpApi.java:100-107`);
- erros de domínio numa exceção própria, mapeada para HTTP num lugar só (`HttpApi.java:93-98`).

Achei dois detalhes, ambos do agente e pequenos demais para mudar a nota:
- a constante `EASTER_WEEKS` foi declarada no meio da classe, entre dois métodos (`PriceCalculator.java:43`), e não
  junto das outras constantes, no topo;
- a linha nova do README não foi quebrada como as vizinhas (`README.md:32`).

## Testes

**Números:**
- `final`: 27 testes rodaram, 0 falhas, 0 erros, 0 pulados, 27 métodos escritos; cobertura de 93,9% das linhas
  (476/507) e 87,7% dos ramos (107/122).
- `base`: 26 testes; 93,8% das linhas e 87,1% dos ramos.
- `hotel.reservations` passou de 49/52 para 55/58 ramos cobertos.

**Nota:** bom

O agente manteve os 26 testes da base e não apagou nem afrouxou nenhum. Os valores esperados que dependiam dos 20%
foram recalculados para 25%, com o comentário do cálculo atualizado:
- `src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36`: 312,38 por noite, total 562,28;
- `PriceCalculatorTest.java:45`: total 562,28;
- `PriceCalculatorTest.java:71-74`: subtotal 2899,52, desconto 434,93, total 2464,59;
- `src/test/java/hotel/web/HttpApiTest.java:77` e `HttpApiTest.java:118`: 974,76.

O teste novo `easterWeekIsHighSeason` (`PriceCalculatorTest.java:78-86`) confere os dois domingos e a véspera e o dia
seguinte de cada ano, em 2027 e em 2028.

A pergunta da mutação dá resultado positivo:
- trocar 0,25 por outro valor derruba as linhas 36, 45 e 72-74;
- deslocar uma data da Páscoa em um dia, ou trocar `isBefore`/`isAfter` pelas versões inclusivas, derruba as linhas
  79-86;
- apagar a condição da Páscoa (`PriceCalculator.java:58`) derruba as linhas 80-81 e 84-85.

Faltou só um teste que passe uma estadia na Páscoa pelo `calculate`, com o valor final. A rede existe mesmo assim,
porque o `nightPrice` já é conferido com valor nos testes de dezembro e de julho.

## Banco

**Números:**
- 3 tabelas, todas com chave primária;
- dinheiro sempre em `numeric(10,2)`;
- nenhuma chave estrangeira sem índice e nenhuma data guardada como texto;
- 1 migração Flyway (`V1__create_tables.sql`), "igual" à da base.

O script acusou apenas as colunas `status`, `rate_plan`, `type` e `channel`, que estão em `text` sem `CHECK`.
**Nota:** bom

O agente não mexeu no banco, e não precisava: o pedido muda só o preço. A migração da base ficou intocada, então quem
já roda o sistema sobe sem problema com o Flyway. O preço de cada reserva é gravado quando ela é criada
(`src/main/resources/db/migration/V1__create_tables.sql:30-32`, `ReservationService.java:46-47`), e por isso as
reservas antigas guardam o valor de 20% com que foram feitas. É o comportamento sensato para o dono.

Herdado da base, sem piora:
- as colunas de situação e de tipo sem `CHECK` (`V1__create_tables.sql:28`, `:33`);
- o `ReservationStore.all()`, que lê a tabela inteira (`src/main/java/hotel/reservations/ReservationStore.java:108-111`).

A dupla reserva é barrada pelo próprio banco, com um `EXCLUDE USING gist` (`V1__create_tables.sql:38-40`).

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- acréscimo novo: 5/5;
- semana da Páscoa: 10/10;
- os outros períodos continuam: 6/6;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, isso quer dizer que tudo o que ele pediu funciona:
- um standard numa noite de temporada custa R$ 312,38, um deluxe R$ 487,38 e uma suíte R$ 724,88;
- o não reembolsável aplica os 15% sobre o valor novo (desconto de R$ 46,86, total de R$ 265,52);
- as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, e as vizinhas de fora custam a
  diária normal;
- uma estadia de 20 a 22/03/2027 sai R$ 562,28 (uma noite normal e uma de temporada).

Os limites de dezembro, fevereiro e julho continuam como antes, e o resto do sistema (recusas, confirmação com
e-mail, cancelamentos, relatórios, dados depois de reiniciar, reservas simultâneas) segue funcionando.

## Dúvidas

A dúvida é pequena: o `record` privado `EasterWeek` (`PriceCalculator.java:47-51`) pode ser visto como um tipo a mais
que não era necessário, já que duas condições no `isHighSeason` resolveriam. Dei `contenção` porque a referência aceita
"uma lista de períodos" como contenção, e o `record` é só o formato desse dado: privado, sem interface e sem
comportamento que varie.
