# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `http` (rotas Javalin, conversão da requisição e do JSON), `service` (os casos de uso e os
relatórios), `plan` (os cinco planos de tarifa), `domain` (reserva, quarto, dinheiro, temporada, comprovante, erros) e
`persistence` (pool Hikari, transação e repositórios JDBC), além de `Main`, que monta tudo. Uma requisição entra em
`http/App.java:32-39`. `RequestParser` transforma o corpo num `NewReservation`, o `ReservationService` valida, pede o
preço ao plano (`service/ReservationService.java:38-49`) e grava numa transação com trava por quarto
(`service/ReservationService.java:51-62`). Os dados ficam em duas tabelas no PostgreSQL, `reservations` e
`messages`; os quartos ficam só no código (`domain/Rooms.java:8-13`). O comportamento está todo certo (72 de 72) e
os planos seguem um Strategy limpo. O mais sério está no banco e nos testes. O esquema é criado com
`CREATE TABLE IF NOT EXISTS` no código, sem migrações, sem tabela de quartos, sem chave estrangeira e sem `CHECK`. Os
testes são só 6 testes de unidade dos planos, com 20% de cobertura de linhas.

## Arquitetura

**Números:** 6 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). As dependências vão
de `http` para `service`, de `service` para `plan` e `persistence`, e todos dependem de `domain`, que não depende de
ninguém. O Javalin só aparece em `http` e em `Main`; o JDBC aparece em `persistence` e também em `service`. A maior
classe tem 133 linhas (`ReservationService`).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota de `http/App.java:30-46` lê a entrada, chama um método do serviço e escreve a
  resposta. A conversão fica em `http/RequestParser.java` e `http/Json.java`, e o mapeamento de erro para status HTTP
  é um único `app.exception` (`http/App.java:48-53`).
- **As regras não conhecem o Javalin.** Preço, situação da reserva e devolução moram em `domain` e `plan`, sem
  Javalin e sem JDBC. A máquina de estados fica em `domain/Reservation.java:68-93` e os planos em `plan/*.java`.
- **O SQL fica todo em `persistence`** (`ReservationRepository`, `MessageRepository`), com a transação centralizada em
  `persistence/Database.java:67-81`.
- **Defeito 1, a montagem:** os serviços criam os seus repositórios por dentro
  (`service/ReservationService.java:24-25`, `service/ReportService.java:17`) em vez de recebê-los do `Main`, que só
  injeta o `Database` (`Main.java:10-15`).
- **Defeito 2, o JDBC no serviço:** a assinatura de `load` recebe `java.sql.Connection` e lança `SQLException`
  (`service/ReservationService.java:120`), e as lambdas de `database.tx` passam a conexão adiante. É por isso que o
  jdeps mostra `service` usando JDBC.
- **A consequência:** juntos, os dois defeitos impedem testar o serviço sem um PostgreSQL de verdade. Não é por acaso
  que o pacote `service` tem 0% de cobertura. É uma mistura localizada, que alguém deve corrigir antes do próximo
  pedido, mas não compromete a separação geral.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura e nenhum fica fora dela. Há uma interface `RatePlan` com uma classe por
plano (`plan/StandardPlan.java`, `NonRefundablePlan.java`, `CorporatePlan.java`, `WeekendPackagePlan.java`,
`LongStayPlan.java`), escolhida por um mapa (`plan/RatePlans.java:10-16`). Não existe nenhum `switch` sobre o plano.
Uma só comparação com o nome de um plano fica fora da estrutura (`service/ReservationService.java:56`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `plan/RatePlan.java:13-34` reúne as três coisas que variam por plano:

- quem pode usar: `validate`;
- o preço: `quote`/`discount`;
- a devolução: `refund`.

Os métodos têm padrões sensatos: sem desconto, devolve tudo. Cada plano sobrescreve só o que muda:

- o convênio valida o código e aplica o desconto da empresa (`plan/CorporatePlan.java:23-32`);
- o pacote valida sexta mais duas noites, troca o cálculo inteiro pelo preço fechado e devolve metade
  (`plan/WeekendPackagePlan.java:26-40`);
- a estadia longa exige 7 noites e dá 10% (`plan/LongStayPlan.java:19-28`);
- o não reembolsável dá 15% e não devolve nada (`plan/NonRefundablePlan.java:14-22`).

Os dois planos que já existiam na parte comum também estão na estrutura (`StandardPlan` e `NonRefundablePlan`). O
serviço pede ao plano sem nomeá-lo: `plan.validate(stay)` e `plan.quote(stay)` em
`service/ReservationService.java:48-49`. O cancelamento passa a regra do plano para a reserva
(`service/ReservationService.java:82`, `domain/Reservation.java:82-85`). Um plano novo é uma classe nova mais uma linha
em `RatePlans`. A única regra de plano fora do plano é guardar o `companyCode` só quando o plano é `"CORPORATE"`
(`service/ReservationService.java:56`). É pequena e não é de preço nem de devolução (veja as Dúvidas).

**O exagero.** Não há estrutura sem necessidade. `Quote`, `Stay` e `NewReservation` são records pequenos e usados.
Não há fábricas, builders nem interfaces com uma implementação só.

**O resto do código.**

- Dinheiro em `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:16-27`, `domain/Stay.java:16-27`) e datas
  em `LocalDate`.
- Os estados da reserva são um `enum` (`domain/ReservationStatus.java`), e as transições são verificadas num só
  lugar (`domain/Reservation.java:87-93`).
- Não há estado estático mutável.
- O tratamento genérico de exceção registra a causa (`http/App.java:50-53`).

Os defeitos são pequenos:

- a reserva guarda o plano como texto (`domain/Reservation.java:18`) e o serviço procura o plano de novo pelo nome
  no cancelamento (`service/ReservationService.java:82`);
- `RatePlans.find` devolve `null` em vez de `Optional` (`plan/RatePlans.java:22`);
- o construtor de `Reservation` tem 16 parâmetros (`domain/Reservation.java:27-30`);
- a regra "o que fica com a pousada é o pago menos o devolvido" aparece duas vezes, em SQL
  (`persistence/ReservationRepository.java:100`) e em Java (`service/ReportService.java:42-43`).

Nada disso torna o código difícil de mudar.

## Testes

**Números:** 6 testes rodaram, nenhum falhou e nenhum foi pulado; há 6 métodos escritos, num arquivo só. A cobertura
é de 97 de 483 linhas (20,1%) e 23 de 90 ramos (25,6%). Por pacote: `plan` 37/38, `domain` 60/123, e `http`,
`service` e `persistence` com 0 (`measures/tests.md`). Não há testes pela API nem contra o banco.
**Nota:** com problemas

**O que está testado.** Os testes que existem conferem, com valores, a parte nova do cenário:

- o exemplo da Tecnosul (779,80 / 93,58 / 686,22) e a recusa sem código ou com código desconhecido
  (`RatePlanTest.java:42-49`);
- o pacote a 820,00 com devolução de 410,00 e as duas recusas de data (`RatePlanTest.java:52-59`);
- a estadia longa (1.749,30 / 174,93 / 1.574,37) e a recusa com 6 noites (`RatePlanTest.java:62-68`);
- a alta temporada com 599,76 (`RatePlanTest.java:29-32`);
- o texto do comprovante com desconto (`RatePlanTest.java:71-84`).

**O que fica sem rede.** Grande parte das regras pedidas não tem teste:

- a máquina de estados (confirmar, check-in, check-out, cancelar fora de hora);
- o cancelamento de reserva pendente;
- a recusa por quarto ocupado e por capacidade;
- os códigos de erro da API;
- o e-mail;
- os dois relatórios CSV;
- a persistência.

**A pergunta da mutação:**

- trocar 0.18 da Agrovale ou 0.25 da Farmavida (`plan/CorporatePlan.java:14-15`) não derruba teste nenhum;
- tirar a temporada do `LONG_STAY`, ou aplicá-la ao pacote, também não (nenhum teste combina esses planos com a alta
  temporada);
- trocar a devolução integral do convênio ou da estadia longa também não, porque só a devolução do pacote e a do não
  reembolsável são verificadas.

## Banco

**Números:** 2 tabelas (`reservations`, 16 colunas, e `messages`, 5 colunas), ambas com chave primária
(`measures/database.md`). O dinheiro está em `numeric(12,2)` e as datas em `date`. As conferências acusaram:

- nenhuma chave estrangeira;
- nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`;
- `status`, `rate_plan` e `channel` em `text` sem `CHECK`;
- o esquema criado por `CREATE TABLE` dentro do Java, com 0 arquivos de migração.

O SQL montado com `+` só junta constantes.
**Nota:** com problemas

**O que está certo.**

- Os tipos estão certos e os campos obrigatórios têm `NOT NULL` (`persistence/Database.java:16-43`).
- Todo SQL usa parâmetros. As concatenações de `persistence/ReservationRepository.java:85-100` juntam só `COLUMNS` e
  trechos fixos.
- Há um pool (Hikari, `persistence/Database.java:47-54`) e as conexões são devolvidas com try-with-resources.
- Cada operação roda numa transação (`persistence/Database.java:67-81`). A confirmação e o e-mail andam juntos
  (`service/ReservationService.java:70-76`), e as transições travam a linha com `FOR UPDATE`
  (`persistence/ReservationRepository.java:85`).
- A reserva dupla é impedida por `pg_advisory_xact_lock` por número de quarto, seguido da consulta de sobreposição,
  na mesma transação (`persistence/ReservationRepository.java:22-41`). Essa trava vale entre processos, e o roteiro
  confirmou que funciona.

**Os defeitos.**

- O esquema não evolui: `CREATE TABLE IF NOT EXISTS` (`persistence/Database.java:16-43`) não sabe acrescentar uma
  coluna a uma tabela que já existe. O próximo plano que precisar de um campo novo vai exigir uma alteração à mão
  num banco em uso.
- Os quartos não existem no banco (`domain/Rooms.java:8-13`), então `reservations.room_number` não tem chave
  estrangeira.
- Nenhuma restrição protege as regras no próprio banco: não há `CHECK` em `status` nem em `guests > 0`, nem
  `check_out > check_in`, nem um `EXCLUDE` contra sobreposição. Tudo depende de o código passar sempre pela trava.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou (`measures/behavior.md`). Por parte:

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- os planos novos do cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2 (em 10 rodadas, sempre uma criada e a outra recusada com 409).

**Nota:** bom

Tudo o que o dono da pousada descreveu funciona pela API:

- o preço com temporada e arredondamento meio para o par (599,76; desconto de 37,48);
- os erros com os códigos combinados;
- a confirmação com o e-mail e o comprovante;
- o check-in e o check-out;
- as devoluções de cada plano;
- os dois relatórios CSV, incluindo a não reembolsável cancelada que continua contando.

Os valores que o financeiro passou conferem: Tecnosul 686,22, Agrovale 639,44, Farmavida 584,85, pacote 820,00 com
devolução de 410,00, estadia longa 1.574,37 e, com noites de temporada, 1.754,30. O pacote recusa datas fora de
sexta a domingo, a estadia longa recusa menos de 7 noites e o `companyCode` é ignorado nos outros planos. As reservas
e as mensagens continuam lá depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- **Uma regra de plano no serviço.** `service/ReservationService.java:56` decide pelo nome `"CORPORATE"` se guarda o
  código da empresa. É exatamente a regra pequena, fora de preço e de devolução, que a referência manda contar aqui
  sem mudar a classe. Mantive `acerto`, mas um plano novo que também use um código precisaria mexer nessa linha.
- **A nota da arquitetura.** Os serviços criam os próprios repositórios e expõem `Connection`. Isso fica na
  fronteira entre `bom` e `com problemas`. Dei `com problemas` porque isso impede testar o serviço sem banco, e a
  cobertura de 0% em `service` mostra o efeito.
- **Duas decisões em pontos que o texto não fechou**, nenhuma testada pelo roteiro:
  - um id de reserva que não é número responde 404 `RESERVATION_NOT_FOUND` (`http/App.java:62-68`);
  - a capacidade (422) é verificada antes das regras do plano (400) (`service/ReservationService.java:43-48`).
