Li o enunciado e o código todo; segue a correção.

# Cenário 07

## Visão geral

O agente montou uma corrente de regras de e-mail (`EmailRule` com `next`, e as subclasses `ValidationRule`, `TestModeRule` e `ArchiveCopyRule`). Cada uma das três regras pedidas virou uma peça própria, e o envio (`MessageService`) não tem nenhum `if` de regra. Isso é bom. O problema mais sério está em quem decide a combinação por modo. A corrente é montada sempre igual, com as três regras, dentro do construtor de `EmailRuleChain`. O modo de teste é passado para dentro das regras, e cada uma decide sozinha se vale ou não: a cópia verifica `!testMode` e o desvio verifica `testMode`. As instruções do pattern apontam justamente esse caso como malfeito. No comportamento, as regras de e-mail estão quase todas certas. O resto do sistema tem falhas importantes. A principal é que a confirmação exige um `amountPaid` no corpo da requisição, quando o enunciado manda usar o total. Também há o dinheiro do comprovante com ponto em vez de vírgula, e o relatório de ocupação ignora quem já fez check-in.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `com.pousadamarealta` e se divide assim:
- `handler/`: os controladores Javalin (`ReservationHandler`, `RoomHandler`, `MessageHandler`, `ReportHandler`);
- `service/`: `ReservationService`, `MessageService`, `ReportService` e duas "estratégias" de relatório;
- `repository/`: mapas e listas em memória, com ids vindos de `AtomicLong`, começando em 1;
- `model/`: `Reservation`, `Room`, `Message` e os enums;
- `tariff/`: `TarifPlan` com `StandardTarifPlan`, `NonRefundableTarifPlan` e `TarifPlanFactory`;
- `email/`: a corrente de regras;
- `util/`: `Money` (HALF_EVEN), `DateUtil` e `EmailValidator`;
- `config/Config`: lê `HOTEL_TEST_MODE` e `PORT`.

O `Main` cria os repositórios, os serviços (entre eles `new EmailRuleChain(Config.isTestMode(), Config.isArchiveCopyEnabled())`, em `src/main/java/com/pousadamarealta/Main.java:26`) e os handlers, registra as rotas e sobe na porta (`Main.java:37-49`).

Uma requisição segue este caminho: a rota chama o handler, o handler lê o JSON como `Map` e chama o service, e o service valida, calcula pelo `TarifPlan` e grava no repositório. Os erros viram `IllegalArgumentException` com um texto, e `handleError` traduz esse texto em código HTTP comparando trechos da mensagem (`ReservationHandler.java:185-199`). O e-mail de confirmação é montado no próprio handler (`ReservationHandler.java:80-86`) e entregue a `MessageService.sendEmail`.

Os testes do agente são unitários: data, dinheiro, plano de tarifa, validador de e-mail e corrente de regras (`src/test/java/com/pousadamarealta/EmailRuleTest.java`). Não há teste de API.

**A parte que pede o pattern.**
- `EmailRule` é uma classe abstrata com `next`. O método `process` é final: chama `shouldProcess` e `doProcess` e, se a mensagem não foi bloqueada, passa adiante (`email/EmailRule.java:10-18`).
- `ValidationRule` bloqueia o endereço inválido (`email/ValidationRule.java:12-16`).
- `TestModeRule` recebe `testMode` e só age quando ele é verdadeiro (`email/TestModeRule.java:6-13`). Nesse caso troca o destinatário, o assunto e o texto (`:17-20`).
- `ArchiveCopyRule` recebe `testMode`, `enabled` e uma referência à própria corrente. Ela só age com `enabled && !testMode && !blocked` (`email/ArchiveCopyRule.java:15-17`) e registra a cópia numa lista guardada na corrente (`:21-26`).
- A montagem é fixa, no construtor de `EmailRuleChain`: validação, depois teste, depois cópia, sempre as três (`email/EmailRuleChain.java:10-17`).
- `MessageService` roda a corrente. Depois grava a mensagem com `SENT` ou `BLOCKED` e, em seguida, as cópias que a corrente juntou (`service/MessageService.java:19-31`).

**Por que malfeito.** O desenho tem pontos a favor do pattern: cada regra é uma peça própria, o envio não pergunta pelo modo e o `MessageService` só aplica a corrente. Isso o aproxima da alternativa equivalente (Chain of Responsibility). Mas as instruções exigem que a combinação por modo seja decidida na montagem e não dentro das regras, e aqui acontece o contrário:
- a cópia carrega `testMode` e se desliga sozinha no modo de teste (`ArchiveCopyRule.java:16`), que é o exemplo literal de malfeito nas instruções;
- o desvio está sempre na corrente e decide por dentro se vale (`TestModeRule.java:12`);
- a ordem e a presença das peças estão fixas no construtor de `EmailRuleChain` (`EmailRuleChain.java:10-17`), não no `Main`. Para uma regra nova que valha "só no teste" ou "só fora dele", a solução natural neste desenho é mais um booleano dentro da regra.

Há ainda um acoplamento estranho: a regra de cópia escreve num estado mutável compartilhado da corrente (`EmailRuleChain.java:8,20,24-26`), e esse estado não é seguro com requisições simultâneas.

**Exagero colateral.** Pesou `ReportStrategy` (`service/ReportStrategy.java:6-8`). É uma interface com parâmetro `Object` que cada implementação converte com cast (`OccupancyReportStrategy.java:16`, `RevenueReportStrategy.java:17`). O `ReportService` instancia cada uma com nome fixo (`ReportService.java:17-25`). Ninguém escolhe nem substitui nada: é uma camada que só repassa, e ainda perde o tipo. A `TarifPlanFactory` (`tariff/TarifPlanFactory.java:6-11`) atende a uma variação real (dois planos com desconto e reembolso diferentes), então pesa pouco.

**Comportamento.** Conferi por leitura. As regras de e-mail estão quase certas. O resto do sistema tem desvios sérios, detalhados no item.

## Item a item

**O pattern pedido.** As três regras são peças próprias de uma corrente (`email/EmailRule.java:10-18`, `email/ValidationRule.java:12-16`, `email/TestModeRule.java:16-21`, `email/ArchiveCopyRule.java:20-27`), e o envio não tem `if` de regra (`service/MessageService.java:19-31`). Até aqui, seria uma alternativa equivalente. Só que a combinação por modo não é decidida na montagem. A corrente tem sempre as mesmas três peças, ligadas no construtor de `EmailRuleChain` (`email/EmailRuleChain.java:10-17`). O modo é passado para dentro das regras, e a cópia verifica `!testMode` (`email/ArchiveCopyRule.java:16`) enquanto o desvio verifica `testMode` (`email/TestModeRule.java:12`). As instruções do pattern descrevem esse caso como malfeito: "a cópia que verifica se está em modo de teste".
Conclusão: malfeito

**O exagero colateral.** `ReportStrategy` (`service/ReportStrategy.java:6-8`) é uma interface com `Object param`. As implementações fazem cast (`service/OccupancyReportStrategy.java:16`, `service/RevenueReportStrategy.java:17`) e são instanciadas com nome fixo em `ReportService` (`service/ReportService.java:18,23`). Ninguém troca nem escolhe estratégia: é uma camada que só repassa e ainda perde o tipo. (`TarifPlan`/`TarifPlanFactory` atendem a uma variação real e não contei como exagero.)
Conclusão: exagero

**O comportamento.** Conferi por leitura do código, sem subir o serviço.

O que está certo nas regras de e-mail:
- O validador exige um único @, algo antes e um ponto que não seja o primeiro nem o último caractere depois do @ (`util/EmailValidator.java:15-37`). `ana@gmail` e `ana.gmail.com` são bloqueados. Há uma folga: `a@b.c.` passa, porque só o primeiro ponto é verificado.
- A mensagem bloqueada interrompe a corrente antes do desvio de teste (`EmailRule.java:15`). Por isso fica com o endereço original, sem cópia, com `BLOCKED`, também no modo de teste (`MessageService.java:24-25`).
- A cópia vai para `arquivo@pousadamarealta.com.br` com "[Cópia] " e é gravada logo depois da original (`ArchiveCopyRule.java:21-25`, `MessageService.java:28-30`).
- No modo de teste, a mensagem vai para `testes@…` com "[TESTE] " e com a primeira linha "Destinatário original: …" (`TestModeRule.java:18-20`), sem cópia. O texto ganha uma linha em branco a mais depois dessa linha.
- O campo `status` existe no `GET /messages` (`handler/MessageHandler.java:27`).

O que está errado no resto do sistema:
- A confirmação exige `amountPaid` no corpo da requisição (`handler/ReservationHandler.java:74-75`). Um POST sem corpo dá 400. O enunciado manda que o valor pago seja o total da reserva.
- O comprovante escreve o dinheiro como `779.80`, sem a vírgula e sem o separador de milhar (`ReservationHandler.java:177-181`).
- O e-mail de confirmação não tem a linha em branco entre o "Total" e "A entrada…" (`ReservationHandler.java:83-84`).
- A ocupação conta só `CONFIRMED` e ignora `CHECKED_IN` e `CHECKED_OUT` (`service/OccupancyReportStrategy.java:27`).
- O financeiro inclui as canceladas com valor zero, tanto as pendentes quanto as do plano normal com reembolso total (`service/RevenueReportStrategy.java:32`).
- A segunda via sai para qualquer status, inclusive `PENDING` (`ReservationHandler.java:142-166`).
- Zero hóspedes dá 422 `CAPACITY_EXCEEDED` em vez de 400 (`service/ReservationService.java:39-40`, `ReservationHandler.java:192`).
- A falta de `guestName`, `guestEmail` ou `guestCpf` não é recusada (`ReservationHandler.java:35-37`).
Conclusão: em parte

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. A estrutura de corrente com uma peça por regra está lá, e o envio está limpo. Mas a decisão por modo vive dentro de `TestModeRule` e `ArchiveCopyRule`, e a montagem fica fixa num construtor que não recebe peças. As instruções descrevem esse caso como malfeito, e foi isso que decidiu. No comportamento, as regras de e-mail quase seguem, mas a confirmação que não funciona sem `amountPaid` quebra o fluxo principal que dispara o e-mail. Escolhi `em parte`, e não `não segue`, porque as regras pedidas pelo cenário estão certas pela leitura.
