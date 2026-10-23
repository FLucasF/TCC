# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin com sete pacotes. `api` tem o controlador e os mapeadores de JSON, `service` as regras, `domain` as entidades e enums, `repository` o JDBC, `infrastructure` o pool Hikari e o `CREATE TABLE`, e `messaging` as regras de envio. Toda a montagem fica em `Main.java:27-64`. Uma requisição passa por `ApiController`, depois por `ReservationService` e pelo `Reservation` do domínio, e chega a `ReservationRepository`, que usa SQL com `?` no PostgreSQL. As tabelas são criadas no código, sem migração. As regras de e-mail formam uma lista de políticas (`MessagePolicy`) que o `MessageProcessor` aplica em ordem.

Os problemas mais sérios:

- **Valores zerados na criação.** A reserva recém-criada sai com subtotal, desconto e total iguais a 0. O preço é calculado e depois jogado fora (`service/ReservationService.java:62-67`).
- **O bloqueio não para a corrente.** O bloqueio de endereço inválido não liga o indicador de bloqueio. Por isso a mensagem bloqueada ainda gera a cópia para o arquivo, e no modo de teste é desviada para `testes@`.
- **Reserva em dobro.** Nada impede duas reservas simultâneas do mesmo quarto, e isso aconteceu em 10 de 10 rodadas.
- **Recusas que viram 500.** Várias recusas que deviam dar 400 saem como 500.
- **Testes nunca rodaram.** Eles dependem de Docker e nunca rodaram; dois deles falhariam pelo código atual.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin e Jackson só em `com.pousada` e `com.pousada.api`, JDBC só em `repository` e `infrastructure`. A maior classe é `ReservationRepository`, com 253 linhas.
**Nota:** com problemas

As dependências seguem uma direção só: `api` depende de `service`, que depende de `repository` e de `messaging`; tudo depende de `domain`. A montagem está num lugar só, `Main.java:27-44`, com construtores recebendo as dependências, sem `new` escondido nem `static` global de estado. Os serviços não conhecem o `Context` nem o SQL, e o acesso ao banco está isolado nos três repositórios.

As misturas são localizadas, mas existem:

- **Regra no handler.** A regra "a segunda via só existe para reserva paga" mora no handler, `api/ApiController.java:145-148`, e não no serviço.
- **Comprovante montado na borda.** O handler de check-out e o de segunda via buscam o quarto e montam o comprovante eles mesmos (`api/ApiController.java:128-131`, `150-153`). Isso é trabalho de serviço. O texto montado nem é usado: `api/ApiMappers.java:61` recebe `documentText` e o ignora.
- **Validação dividida em duas camadas.** Parte da validação de entrada fica no controlador e parte no serviço, cada uma com a sua própria `InvalidRequestException` (`api/ApiController.java:225`, `service/ReservationService.java:161`). O controlador só trata a dele, `api/ApiController.java:65`.

## Design de baixo nível

**Números:** as 3 regras pedidas (bloqueio, cópia, modo de teste) estão em 3 classes que implementam `MessagePolicy`, e nenhuma ficou fora da estrutura. A combinação por modo está em `messaging/MessageProcessor.java:34-45`, chamada de `Main.java:33`.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O agente não fez Decorator. Fez uma lista de filtros: o `MessageProcessor` aplica cada `MessagePolicy` em ordem sobre um `PolicyResult` (`messaging/MessageProcessor.java:27-29`). A referência aceita essa estrutura: cada regra é uma peça própria (`BlockInvalidEmailPolicy`, `FileCopyPolicy`, `TestModePolicy`), e a combinação por modo é decidida na montagem (`MessageProcessor.java:38-42`: bloqueio sempre, e cópia ou desvio conforme o modo). Uma regra nova, como madrugada ou limite diário, entra como outra classe e outra linha na lista.

O que afasta do acerto:

- **O bloqueio não funciona.** O bloqueio não interrompe a corrente. `BlockInvalidEmailPolicy.java:11` chama `result.transform(PolicyResult.block(message).getMessage())`. Isso só troca a mensagem por uma com status BLOCKED; o campo `blocked` do resultado continua `false`, porque `PolicyResult.block` (`PolicyResult.java:24`) cria outro objeto, que é descartado. As regras seguintes perguntam `result.isBlocked()` (`FileCopyPolicy.java:12`, `TestModePolicy.java:17`), recebem `false` e agem sobre uma mensagem bloqueada. A interface do `PolicyResult` não tem um jeito de marcar o bloqueio, e isso induziu o erro.
- **Uma bandeira que sobrou.** `TestModePolicy` ainda carrega uma bandeira `testMode` (`TestModePolicy.java:9-17`) que a montagem sempre passa como `true` (`MessageProcessor.java:39`).

**O exagero.** Não há estrutura desnecessária relevante. `MessagePolicy` tem três implementações reais. O que sobra é pequeno: `RoomService` só repassa ao repositório, e `PolicyResult.getAdditionalMessages` não é usado.

**O resto do código.** Tem defeitos espalhados, e vários deles quebram regras:

- **Cálculo descartado.** `ReservationService.createReservation` calcula subtotal, desconto e total e não grava nenhum dos três na reserva (`service/ReservationService.java:62-67`). A reserva sai com subtotal 0.
- **Preço repetido.** O cálculo de preço e desconto está copiado em três lugares: `ReservationService.java:62-65`, `96-99` e `domain/Reservation.java:72-80`. `confirm` recebe um `totalValue` calculado fora (`Reservation.java:83`).
- **Refund preenchido cedo.** `refund` vira 0 na confirmação (`ReservationService.java:107`). O texto pede `null` até o cancelamento.
- **amountPaid apagado.** No cancelamento, `amountPaid` é zerado (`Reservation.java:117-118`), e o que foi pago se perde.
- **Setters públicos.** `setStatus` e `setSubtotal` são públicos (`Reservation.java:92`, `190`) e contornam a máquina de estados.
- **Erros que viram 500.** A exceção duplicada descrita na arquitetura faz "saída não depois da entrada", "nenhum hóspede" e "plano inválido" virarem 500. O mesmo acontece com um id que não é número (`Long.parseLong`, `api/ApiController.java:78` e seguintes). Um `guests` não numérico vira 0 por `asInt` (`api/ApiController.java:213`).
- **Moeda com espaço especial.** O dinheiro é formatado com `NumberFormat.getCurrencyInstance(pt-BR)` (`service/MessageService.java:19`, `89`), que põe um espaço não separável (U+00A0) entre "R$" e o número. O comprovante deixa de ser exatamente "R$ 599,76".
- **Tratamento de erro repetido.** O controlador repete o mesmo bloco de `catch` em seis métodos.

## Testes

**Números:** 1 classe rodou com 1 erro (`IllegalState Could not find a valid Docker environment`). Há 8 métodos de teste escritos e nenhum executou. A cobertura é de 0 de 820 linhas (0%) e 0 de 154 ramos (0%) (`measures/tests.md`).
**Nota:** ruim

**O ambiente.** O único arquivo, `src/test/java/com/pousada/IntegrationTest.java`, sobe um PostgreSQL por Testcontainers (`IntegrationTest.java:29-33`) e ignora o `DB_URL`. Sem Docker, nenhum teste roda.

**O que eles conferem.** Mesmo pela leitura, são fracos:

- **Valores errados.** `shouldApplyNonRefundableDiscount` espera desconto de 74,98 e total de 424,87 (`IntegrationTest.java:114-115`). O certo para 2 × 249,90 é 74,97 e 424,83, e os números esperados nem fecham entre si. O teste falharia.
- **Bloqueio que falharia.** `shouldBlockInvalidEmail` espera uma única mensagem (`IntegrationTest.java:132`). O código gera duas, a bloqueada e a cópia, então esse teste também falharia. O agente não rodou os testes.
- **Asserções frouxas.** Há `assertTrue(...> 0)` e `size() >= 2` (`IntegrationTest.java:78`, `81`).
- **Nada pela API.** Nenhum teste passa pela HTTP, então não conferem códigos de erro, CSV nem o texto do e-mail.
- **Valor na criação.** Nenhum teste confere o total antes da confirmação; é exatamente onde o valor sai zerado.
- **Regras sem rede.** Sem testes de cancelamento com devolução, de relatórios nem de comprovante. Na parte do cenário, trocar o endereço de testes, apagar o "[Cópia] " ou mudar a validação de "." não seria pego por nenhum teste que rode.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK nas colunas de situação, plano e tipo;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações e 0 `FOR UPDATE`.

**Nota:** ruim

**Tipos e SQL.** Estão certos: dinheiro em `numeric(10,2)` e datas em `date` (`infrastructure/Database.java:32-47`). O SQL usa parâmetros em todos os repositórios, as conexões vêm do Hikari com try-with-resources, e os dados sobrevivem ao reinício (3 de 3 no roteiro).

**A reserva em dobro.** A regra que o banco devia proteger, um quarto por noite, não tem proteção nenhuma. A checagem é um `SELECT` de conflito (`repository/ReservationRepository.java:129-138`) seguido de um `INSERT` em outra conexão, sem transação, trava ou restrição `EXCLUDE`. No roteiro, as duas reservas simultâneas foram criadas em 10 de 10 rodadas. É dado errado no banco: o mesmo quarto vendido duas vezes.

**Outros defeitos:**

- **Esquema sem migração.** O esquema é criado com `CREATE TABLE IF NOT EXISTS` dentro do código (`infrastructure/Database.java:22-77`), sem migrações, e não sabe evoluir uma tabela existente.
- **Chave sem índice.** A chave estrangeira não tem índice (`Database.java:35`).
- **Filtro do financeiro.** O filtro do relatório financeiro em SQL é confuso: `refund < amount_paid + COALESCE(refund, 0)` (`ReservationRepository.java:206`). O filtro de verdade é refeito em Java (`service/ReportService.java:59`).
- **Uma consulta por linha.** O relatório de ocupação faz uma consulta por linha para buscar o quarto (`service/ReportService.java:32`).

## Comportamento

**Números:** 50 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 37 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário, modo normal | 5 de 9 |
| cenário, modo de teste | 4 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**O que falhou e o que quer dizer para o dono da pousada:**

- **A reserva sai com preço zero.** Quem cria uma reserva recebe subtotal, desconto e total iguais a 0: o STANDARD de julho volta com total 0,00 em vez de 599,76, e o NON_REFUNDABLE de uma noite com desconto 0,00 e total 0,00 em vez de 37,48 e 212,42. O site mostraria R$ 0,00 ao hóspede antes de ele pagar. O valor só aparece depois da confirmação.
- **Recusas que viram erro interno.** "Saída igual à entrada" e "nenhum hóspede" devolvem 500 INTERNAL_ERROR em vez de 400 INVALID_REQUEST. Conferi à mão: plano inválido e id não numérico também dão 500.
- **E-mail com caractere diferente.** O e-mail e o comprovante não trazem exatamente "Total: R$ 599,76": o espaço depois de "R$" é um espaço não separável.
- **Dinheiro sem duas casas.** O dinheiro nem sempre vai com 2 casas no JSON: `"amountPaid":0` e `"subtotal":0`. Depois da confirmação, `refund` vem como 0 em vez de `null`, o que verifiquei à mão.
- **Endereço inválido ainda gera cópia.** No modo normal, para os quatro endereços inválidos, a lista mostra a mensagem BLOCKED e também a cópia para o arquivo. É exatamente o que o texto proibiu.
- **No modo de teste, o bloqueado perde o endereço.** A mensagem bloqueada é desviada para `testes@` e perde o endereço original. A recepção não saberia para quem ligar.
- **O mesmo quarto vendido duas vezes.** Com duas reservas ao mesmo tempo, as duas foram criadas em todas as 10 rodadas.
- **Duas falhas que não pesam.** "O 201 comporta 3" e "a diária é 389.90" falharam só porque `number` vem como texto (`"201"`) e o roteiro procura número. O texto não fixa o tipo, e `GET /rooms` traz `capacity` 3 e `nightlyRate` 389.90 corretos.

**O que funcionou.** Os estados, as devoluções no cancelamento, os dois relatórios, o reinício e o caminho feliz da cópia e do modo de teste funcionam.

## Dúvidas

- **A classe do pattern.** Dei `alternativa equivalente` porque as três regras estão em peças próprias e a combinação é decidida na montagem. Mas o mecanismo que coordena as peças, o bloqueio no `PolicyResult`, não funciona, e `TestModePolicy` ainda pergunta por um `testMode` que a montagem já decidiu. Um revisor mais rigoroso poderia ler isso como `malfeito`.
- **O exagero.** `RoomService`, que só repassa, foi contado como contenção por ser uma camada mínima e coerente com as outras.
- **O ambiente dos testes.** Não dá para saber se o agente tinha Docker no ambiente dele. Pelo código, pelo menos dois testes falhariam de qualquer jeito.
