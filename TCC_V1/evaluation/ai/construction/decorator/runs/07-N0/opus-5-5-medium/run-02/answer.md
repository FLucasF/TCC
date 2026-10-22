# Cenário 07

## Visão geral

O agente montou um sistema pequeno e direto, com três serviços, um `Main` que registra as rotas e modelos simples. As três regras das mensagens (bloquear endereço inválido, desviar para a caixa de testes no modo de teste, mandar a cópia para o arquivo) ficaram como três `if` em sequência dentro de um único método, `MessageService.sendEmail`. Não há interface de envio, não há embrulhos e a montagem não escolhe nenhuma combinação: o serviço recebe um `boolean testMode` e decide sozinho. As regras anunciadas (madrugada, limite por dia, a segunda pousada sem cópia) não têm onde entrar sem mexer de novo nesse método. O desenho é uma omissão do Decorator. A estrutura em volta é contida. O problema mais sério fora do desenho é de comportamento: na criação de reserva, todo erro de negócio vira 400. As regras das mensagens, em si, estão quase todas certas na leitura.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica no pacote `com.pousadamarealta`:
- `Main` lê `HOTEL_TEST_MODE` e `PORT`, monta os serviços com `new`, cria o Javalin e registra todas as rotas em lambdas. Cada lambda trata o erro com `try/catch` e com o `switch` de `handleReservationError` (`Main.java:278-305`).
- `service/ReservationService` guarda as reservas numa `ArrayList`, com os quartos fixos num `HashMap` (`ReservationService.java:21-27`). Calcula preços e transições de estado, gera o comprovante como texto e manda o e-mail de confirmação.
- `service/ReportService` gera os dois CSV.
- `service/MessageService` guarda a lista de mensagens.
- `util/Money` arredonda com `BigDecimal` e `HALF_EVEN`, mas guarda os valores em `double`. `util/DateUtils` cuida da alta temporada. `util/EmailValidator` valida o endereço.
- `dto/*` são as respostas JSON.

O caminho de uma requisição é: rota no `Main`, depois o método do `ReservationService`, que lança `IllegalArgumentException` com o código do erro, e por fim o `switch` do `Main`, que traduz esse código em status HTTP. Os dados ficam só na memória. Os testes são unitários de serviço: `MessageServiceTest`, `ReservationServiceTest`, `ReportServiceTest`, `MoneyTest` e `EmailValidatorTest`.

**A parte das mensagens.** A confirmação chama `messageService.sendEmail(...)` (`ReservationService.java:245`). O `MessageService` recebe o modo no construtor (`MessageService.java:19-21`; montado em `Main.java:31`), e `sendEmail` faz tudo em sequência:
1. Se o endereço é inválido, grava `BLOCKED` com o endereço original e retorna (`MessageService.java:26-30`).
2. Se está em `testMode`, troca o destinatário, o assunto e o corpo (`MessageService.java:36-40`).
3. Grava a mensagem como `SENT` (`MessageService.java:42-43`).
4. Se não está em `testMode`, grava a cópia para o arquivo (`MessageService.java:45-49`).

Cada regra é um bloco dentro do mesmo método, e a combinação por modo ("cópia só fora do teste") está escrita dentro do próprio envio, no `!testMode` da linha 45.

**O raciocínio.** As instruções pedem uma interface de envio, o envio real e um embrulho por regra, empilhados na montagem conforme o modo. Também aceitam uma corrente ou lista de filtros, desde que cada regra seja uma peça própria e a combinação fique na montagem. Aqui não existe nada disso: não há interface, nenhuma regra é uma peça separada, e a montagem só passa um `boolean`. É exatamente o caso que as instruções chamam de omissão: os três `if` dentro do envio. Não é malfeito, porque não há estrutura parcial convivendo com o jeito antigo; simplesmente não há estrutura.

No exagero colateral, não encontrei interface, fábrica ou camada que só repassa. Fora disso, a dívida que existe vai na direção contrária (o `switch` de erros, o tipo do quarto deduzido do número no relatório) e não conta como exagero.

No comportamento, as regras de mensagem conferem quase todas por leitura. Mas há falhas importantes no resto da API: o mapeamento de erros na criação de reserva, o formato do comprovante devolvido e o reembolso como `null`.

## Item a item

**O pattern pedido.** As três regras estão em `MessageService.sendEmail`: o bloqueio em `MessageService.java:26-30`, o desvio de teste em `MessageService.java:36-40` e a cópia condicionada a `!testMode` em `MessageService.java:45-49`. O modo é um `boolean` guardado no próprio serviço (`MessageService.java:17-21`), e a montagem em `Main.java:31` só repassa esse valor. Não há interface de envio nem uma peça por regra, e a combinação por modo está dentro do envio. Por isso nenhuma das regras futuras (madrugada, limite diário, instalação sem cópia) pode ser ligada ou desligada na montagem sem editar esse método. É o caso de omissão descrito nas instruções.
Conclusão: omissão

**O exagero colateral.** Não há estrutura sem necessidade. Os serviços são classes concretas, montadas com `new` no `Main` (`Main.java:31-33`), sem interfaces de uma implementação só, fábricas ou builders. O `dto/ComprovanteResponse.java` existe mas não é usado, o que é código morto e não exagero de estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço.

O que confere:
- **Validação do endereço:** um único @, algo antes e domínio com ponto no meio (`EmailValidator.java:16-41`, na numeração do arquivo). Recusa `ana@gmail` e `ana.gmail.com`.
- **Endereço bloqueado:** fica com o endereço original e sem cópia, também no modo de teste, porque o retorno na linha 29 vem antes do desvio.
- **Cópia:** sai logo depois da original, com "[Cópia] " no assunto e o mesmo texto (`MessageService.java:46-48`).
- **Modo de teste:** manda para `testes@...`, com "[TESTE] " no assunto e "Destinatário original: …" na primeira linha, sem a cópia. O agente pôs uma linha em branco depois dela, o que é aceitável.
- **Campo `status`:** `SENT`/`BLOCKED` aparece no `GET /messages` (`MessageResponse.java`, campo `status`).
- **Modo e porta:** `HOTEL_TEST_MODE` e `PORT` são lidos em `Main.java:20-29`.
- **Preços:** o cálculo arredonda a cada noite com `HALF_EVEN` (`ReservationService.java:173-189`).

O que não confere no resto do sistema:
- **Erros na criação de reserva:** em `POST /reservations`, o `catch (Exception e)` de `Main.java:85-88` captura a `IllegalArgumentException` com o código. Assim, quarto inexistente, quarto ocupado e capacidade excedida saem todos como 400 `INVALID_REQUEST`, e não como 404, 409 e 422.
- **Menos de um hóspede:** dá `CAPACITY_EXCEEDED`, e não 400 (`ReservationService.java:40-42`).
- **Comprovante:** check-out e segunda via devolvem texto puro, e não o JSON com `title`, `reservationId` e os outros campos (`Main.java:170-172`, `Main.java:194-196`).
- **Dinheiro no comprovante:** sai sem separador de milhar, "R$ 1234,56" em vez de "R$ 1.234,56" (`ReservationService.java:249`).
- **Reembolso:** cancelamento não reembolsável ou de reserva pendente deixa `refund` como `null` (`ReservationService.java:166-168`), e o `@JsonInclude(NON_NULL)` tira o campo da resposta (`ReservationResponse.java:7`).
- **Duas casas no JSON:** os valores vão como `double`, então `nightlyRate` sai como 249.9 e não 249.90.

As regras pedidas para as mensagens seguem. O resto do sistema tem falhas relevantes na API.
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe: não existe estrutura nenhuma para as regras de mensagem. A única ponderação foi no comportamento. Escolhi "em parte", e não "segue", porque as regras do cenário conferem, mas a criação de reserva e o formato do comprovante não seguem o contrato da API.
