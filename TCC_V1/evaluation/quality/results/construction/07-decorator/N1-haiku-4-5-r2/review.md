# Revisão: construção, cenário 07

## Visão geral

O projeto tem 6 pacotes (`api`, `services`, `db`, `models`, `utils` e a raiz com o `main`), cerca de 1.280 linhas. Uma requisição entra em `ApiRoutes` (Javalin + Gson, mapas montados à mão), vai para `ReservationService`/`ReportService`, que chamam `Database` (JDBC com HikariCP e uma tabela `reservations` criada com `CREATE TABLE IF NOT EXISTS`). As mensagens ficam numa `ArrayList` em memória dentro de `MessageService`. As camadas existem e não há ciclo, mas o sistema tem defeitos graves. O mais sério: a confirmação exige no corpo um `amountPaid` que o texto não prevê (`src/main/java/com/pousada/api/ApiRoutes.java:92`). Sem esse campo dá 400 e todo o fluxo para (confirmação, e-mail, check-in, check-out, relatórios e as regras de envio do cenário). Com ele, o hóspede "paga" o valor que quiser. Há mais:
- a consulta de ocupação tem um erro de precedência de `OR`/`AND` e lista reservas de outras datas (`src/main/java/com/pousada/db/Database.java:172-173`);
- o arredondamento é `HALF_UP`, e não "meio para o par";
- a alta temporada vale o mês de dezembro inteiro;
- as mensagens somem quando o serviço reinicia;
- nada impede duas reservas simultâneas do mesmo quarto (9 de 10 rodadas criaram as duas).

As três regras de envio estão em três `if` dentro de `MessageService.sendMessage`, sem o Decorator.

## Arquitetura

**Números:** 6 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `com.pousada` e `com.pousada.api`, e o JDBC só em `com.pousada.db`. As maiores classes são `ApiRoutes` (264 linhas), `Database` (249) e `ReservationService` (224).
**Nota:** com problemas

A divisão em camadas existe e as dependências apontam da borda para as regras e das regras para os dados. Os serviços não conhecem o Javalin nem o SQL (`src/main/java/com/pousada/services/ReservationService.java:1-11`), o SQL está todo em `Database`, e a montagem fica num lugar só (`src/main/java/com/pousada/Application.java:24-31`). Problemas localizados:
- **Uma regra de negócio na borda HTTP:** quanto foi pago é lido do corpo da requisição em `src/main/java/com/pousada/api/ApiRoutes.java:91-94`. Pelo texto, o pago é o total da reserva, e essa regra devia estar no serviço.
- **Estado global:** os quartos são um array `public static` mutável (`src/main/java/com/pousada/models/Room.java:38`), lido direto pela borda (`ApiRoutes.java:38`), pelos serviços (`ReservationService.java:216-223`, `ReportService.java:26`) e repetido como o número fixo "/5" (`ReportService.java:38`).
- **Mensagens fora da camada de dados:** a lista de mensagens é guardada pelo próprio serviço, em memória (`src/main/java/com/pousada/services/MessageService.java:9-10`).
- **Erros acoplados por texto:** os erros passam da regra para a borda pela mensagem da exceção, e a borda faz um `switch` sobre o texto (`ApiRoutes.java:228-262`). As duas camadas ficam presas aos mesmos literais.
- **Dependência morta:** `ApiRoutes` recebe um `Database` que não usa (`ApiRoutes.java:28`). É a dependência `api -> db` que o jdeps mostra.

## Design de baixo nível

**Números:** nenhuma das 3 regras de envio está numa estrutura. Bloqueio, desvio para teste e cópia são 3 `if` dentro de um único método (`MessageService.java:28`, `:34`, `:42`). Não existe interface de envio nem embrulho. A combinação por modo é feita por duas flags no construtor.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto anuncia regras que vão crescer (madrugada, limite por dia) e que valem em combinações diferentes por modo e por instalação. O agente pôs tudo em `MessageService.sendMessage`:
- o endereço inválido grava a mensagem como bloqueada e sai (`MessageService.java:28-32`);
- o modo de teste reescreve destinatário, assunto e texto (`:34-38`);
- a cópia depende de `!testMode && archiveEnabled` (`:42-45`).

É exatamente o caso que a referência chama de omissão: os três `if` ficam dentro do envio, e a regra "a cópia só fora do teste" está escrita dentro da própria regra da cópia. A flag `archiveEnabled` até permitiria a "segunda pousada sem cópia", mas a montagem passa sempre `true` (`Application.java:25`). Cada regra nova vai virar mais um `if` e mais um parâmetro no construtor.

**O exagero.** Não há interfaces, fábricas nem camadas que só repassam. Contenção.

**O resto do código.** Há defeitos espalhados em todas as classes:
- **Arredondamento errado.** É `HALF_UP` em vez de "meio para o par" em três lugares: `src/main/java/com/pousada/utils/PriceCalculator.java:27`, `models/Receipt.java:75` e `services/ReportService.java:81`. O roteiro mostra o efeito: desconto de 37,49 em vez de 37,48.
- **Alta temporada errada.** É `month >= 12`, ou seja, de 1º de dezembro em diante (`PriceCalculator.java:34`). Uma diária DELUXE de 01/12/2027 saiu a 467,88 numa chamada feita por mim. A variável `yearMonth` (`:31`) e o campo `nightPrice` do resultado (`:11`, `:69`) não são usados.
- **Texto solto onde há enum.** O plano chega a `PriceCalculator` como `String` comparada com `"NON_REFUNDABLE"` (`PriceCalculator.java:59`), embora exista o enum `Reservation.RatePlan`.
- **Erros.** Os erros são `IllegalArgumentException` com o código no texto (`ReservationService.java:25-38`). Cada handler tem um `catch (Exception)` (`ApiRoutes.java:67`, `96`, `106`, `116`, `126`, `136`) que transforma em 400 qualquer falha, até uma do banco, e devolve ao cliente a mensagem interna. Por exemplo, `"Cannot invoke \"java.util.Map.get(Object)\" because \"body\" is null"` e `"Failed to save reservation"` (`ApiRoutes.java:258-260`).
- **Duplicação.** A montagem do comprovante está copiada três vezes (`ReservationService.java:89`, `144`, `174`), e a busca do tipo do quarto também (`:86-87`, `141-142`, `171-172`).
- **Dinheiro no JSON.** O dinheiro vai como `double` (`ApiRoutes.java:43`, `195-200`, `215-217`): 0,00 sai `0.0` e 499,80 sai `499.8`.
- **Leitura frágil do corpo.** O corpo é lido como `Map` cru, com casts para `Double` (`ApiRoutes.java:51-59`).
- **Estado sem proteção.** Há `static` mutável (`Room.java:38`). Os contadores `nextReservationId++` (`db/Database.java:76`) e `nextMessageId++` (`MessageService.java:49`), e a `ArrayList` de mensagens, não têm proteção nenhuma num servidor que atende várias requisições ao mesmo tempo.

## Testes

**Números:** 10 testes rodaram e passaram (0 falhas, 0 pulados), e há 10 métodos de teste em 3 arquivos. A cobertura é de 13,8% das linhas (97 de 704) e 26,2% dos ramos (43 de 164). Os pacotes `api`, `db` e a raiz têm 0%. `utils` tem 54 de 56 linhas cobertas. Não há teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

**O que está testado, com valores:**
- **A parte nova do cenário.** `src/test/java/com/pousada/MessageServiceTest.java:15-33` confere a mensagem e a cópia: destinatário, `[Cópia] `, mesmo texto e `SENT`. Também há testes para o bloqueio com o endereço original (`:36-47`), o modo de teste sem cópia (`:50-69`) e a cópia desligada (`:72-78`).
- **A validação de endereço.** `src/test/java/com/pousada/EmailValidatorTest.java:8-26` usa os exemplos do texto (`ana@gmail`, `ana.gmail.com`).
- **Três preços** (`src/test/java/com/pousada/PriceCalculatorTest.java:10-57`).

**As fraquezas:**
- O modo de teste confere o "Destinatário original" com `contains` (`MessageServiceTest.java:59`), não como a primeira linha.
- Nenhum preço testado tem empate na terceira casa, por isso o `HALF_UP` passa. Com dezembro também acontece: nenhum caso cai entre 1º e 14/12, então os dois erros de preço ficam sem rede.
- `ReservationService` e `ReportService` não têm teste nenhum. Ficam sem conferência as situações da reserva, o cancelamento e a devolução, os códigos de erro, os dois relatórios, a confirmação e o e-mail montado.
- Pela pergunta da mutação: trocar o `<=`/`>` da consulta de ocupação, inverter a regra da devolução (`ReservationService.java:201-205`) ou apagar a exigência de `PENDING` na confirmação (`:76`) não derrubaria nenhum teste.
- Os defeitos mais graves que o roteiro achou (a confirmação quebrada, a ocupação errada) estão justamente no código sem teste.
- O `pom.xml` declara Testcontainers e Liquibase, que nunca são usados.

## Banco

**Números:** 1 tabela (`reservations`, 17 colunas, 15 `NOT NULL`, com chave primária), criada por `CREATE TABLE IF NOT EXISTS` no Java. O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há chave estrangeira, nem `CHECK` (em `status` e `rate_plan`), nem `UNIQUE`/`EXCLUDE`. Não há transação explícita nem `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

**O que está certo.** Os tipos estão certos, o SQL usa parâmetros e o pool devolve as conexões com try-with-resources (`src/main/java/com/pousada/db/Database.java:83-105`).

**Os defeitos sérios:**
- **Mensagens fora do banco.** As mensagens não ficam no banco: são uma `ArrayList` em `MessageService` (`src/main/java/com/pousada/services/MessageService.java:10`). O texto pede que "nada se perca quando o sistema reiniciar", e a recepção consulta essa lista para saber a quem ligar. Depois de um reinício, ela some.
- **Id gerado em memória.** O id da reserva vem de um contador na memória do processo, lido no `MAX(id)` ao subir (`Database.java:15`, `59-73`, `76`). O `++` não é atômico: duas requisições simultâneas podem pegar o mesmo id, e duas instâncias do serviço certamente pegam. Não há `SERIAL`/`IDENTITY`. Uma inserção que falha (por exemplo, sem `guestName`) consome um número mesmo assim.
- **Dupla reserva.** Nada impede duas reservas do mesmo quarto na mesma noite. A verificação é um `SELECT COUNT(*)` (`Database.java:145-167`) seguido de um `INSERT` separado, sem transação, trava ou restrição `EXCLUDE`. O roteiro criou as duas reservas em 9 de 10 rodadas simultâneas.
- **Consulta de ocupação errada.** Faltam parênteses: `status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' AND check_in <= ? AND check_out > ?` (`Database.java:172-173`). Com isso, toda reserva confirmada ou com check-in aparece em qualquer data. Numa chamada minha, a ocupação de 20/07/2027 listou uma reserva de 10-12/07 e outra de agosto.

**Os defeitos menores:**
- Não há tabela de quartos nem chave estrangeira para eles.
- Não há `CHECK` nas colunas de situação.
- O esquema não tem migrações (o Liquibase está no `pom.xml` e não é usado), então não sabe evoluir.

## Comportamento

**Números:** 29 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 27 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário 07, modo normal | 1 de 9 |
| cenário 07, modo de teste | 0 de 5 |
| duas reservas ao mesmo tempo | 0 de 2 |

**Nota:** ruim

**O que funciona.** Os quartos, a criação da reserva, as recusas na criação (400, 404, 409, 422), o preço de alta temporada de julho e a devolução nos cancelamentos sem pagamento.

**O que falha:**
- **A confirmação.** O site chama `POST /reservations/{id}/confirmation` como o texto descreve, sem corpo, e recebe 400 com a mensagem de um `NullPointerException`. Para o dono, nenhuma reserva é confirmada, nenhum e-mail sai e não há check-in nem check-out. Os relatórios ficam vazios, e as regras de envio do cenário (bloqueio, cópia, modo de teste) nunca chegam a ser exercidas: daí o 1 de 14 no cenário. As falhas em cascata da parte comum (segunda via, check-in, check-out, financeiro, a reserva que fica `CANCELLED` em vez de `CHECKED_OUT`, o reinício) vêm daí. Numa chamada minha, com `{"amountPaid": ...}` no corpo, as regras de envio funcionaram no modo normal (mensagem `SENT` e a cópia `[Cópia] ` logo depois). Isso mostra que a lógica existe, mas o cliente decide quanto pagou: aceitou 1,00 numa reserva de 499,80.
- **O arredondamento.** É `HALF_UP`: o NON_REFUNDABLE de uma noite dá desconto de 37,49 e total de 212,41, e não 37,48 e 212,42. Diferença de centavos contra o hóspede e no caixa.
- **O formato do dinheiro.** O JSON traz `0.0` e não `0.00`, porque usa `double`.
- **O relatório de ocupação.** Está errado por causa do erro de precedência na consulta. O roteiro não pegou isso por causa da confirmação quebrada, mas a minha chamada mostrou.
- **A alta temporada.** Começa em 1º de dezembro e não no dia 15 (fora do roteiro, visto numa chamada minha).
- **O reinício.** As mensagens se perdem a cada reinício. A conferência "as mensagens continuam lá" passou só porque a lista estava vazia antes.
- **A concorrência.** Duas reservas simultâneas do mesmo quarto foram criadas em 9 de 10 rodadas. O quarto fica vendido duas vezes.

## Dúvidas

- O texto não diz se a confirmação leva corpo. Mas diz que "o que ele pagou é o total da reserva", e o roteiro (o "site") chama sem corpo. Por isso tratei como erro do agente, e não como escolha razoável.
- A nota "ruim" no design de baixo nível pesa defeitos espalhados: arredondamento errado copiado em três classes, erros passados por texto, `catch (Exception)` em todos os handlers, duplicação e estado `static`. Um revisor mais brando poderia vê-los como localizados e dar "com problemas".
- Nos testes, a parte nova está bem conferida, mas o núcleo da reserva não tem teste. Fiquei entre "com problemas" e "ruim" e escolhi "com problemas" porque os testes que existem conferem valores.
