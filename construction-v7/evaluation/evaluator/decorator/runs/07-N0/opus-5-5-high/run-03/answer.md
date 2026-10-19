# Cenário 01

## Visão geral

O agente montou um sistema de reservas simples e direto, em camadas (rotas no `App`, serviços, modelo, conversores/DTOs). Não criou nenhuma estrutura para as regras de envio de mensagens. As três regras (bloqueio de endereço inválido, desvio para a caixa de testes e cópia para o arquivo) ficaram como três `if` dentro de um único método, `MessageService.sendMessage`, e o modo de teste é um `boolean` guardado no próprio serviço. Isso é exatamente a omissão que as instruções descrevem: não há interface de envio, não há embrulho nem combinação decidida na montagem, e cada regra anunciada (madrugada, limite diário) teria de entrar como mais um `if` nesse mesmo método. Quando a mensagem chega a ser gerada, as regras funcionam. O problema mais sério no comportamento é a confirmação, que é o que dispara o e-mail. Chamada como o enunciado descreve (sem corpo), ela devolve 500 e nenhuma mensagem sai. O valor pago também vem do cliente, em vez de ser o total da reserva.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote raiz tem só `App` (`src/main/java/com/pousada/marealta/App.java`). Ele cria as peças no construtor: `RoomManager`, `ReservationService`, `MessageService` e `ReportService`. Também configura o Jackson como `JsonMapper` do Javalin e registra todas as rotas em `setupRoutes`. O `main` instancia o `App` e sobe na porta de `PORT` (7070 por padrão). O caminho de uma requisição é sempre o mesmo: a lambda da rota lê o corpo com `ctx.bodyAsClass` em um record de `api/dto`, chama um método do serviço e converte o modelo para DTO com uma classe estática de `api/converter`. Os erros de negócio são uma `ValidationException` com código, que `getStatusCode` traduz para 404, 409 ou 422 (`App.java:252-258`).

- **`service`**:
  - `RoomManager` tem os cinco quartos fixos num `HashMap`.
  - `ReservationService` guarda as reservas num `HashMap` em memória e implementa as transições de estado, a checagem de conflito noite a noite e o reembolso.
  - `PricingService` tem um método estático que calcula noites, subtotal, desconto e total com `HALF_EVEN`.
  - `ReportService` monta os dois CSV à mão.
- **`util`**: tem `DateUtil` (alta temporada), `MoneyUtil`, `DocumentFormatter` (o texto do comprovante) e `EmailValidator`.
- **`model`**: tem `Reservation` (mutável), `Room`, `ReservationDocument`, `Message` e os enums.
- **Testes do agente**: três classes unitárias (`PricingServiceTest`, `DateUtilTest`, `EmailValidatorTest`), com 13 testes no total. Nenhum testa a API nem as regras de envio.

**A parte das mensagens.** Há uma única classe, `MessageService` (`src/main/java/com/pousada/marealta/service/MessageService.java`). Ela recebe `testMode` no construtor (`MessageService.java:13-15`), e o `App` decide esse valor lendo `HOTEL_TEST_MODE` (`App.java:37-38`). O método `sendMessage` faz tudo em sequência:

- `if (!EmailValidator.isValid(to))`: grava como `BLOCKED` e retorna (`MessageService.java:23-27`);
- `if (testMode)`: troca destinatário, assunto e corpo (`MessageService.java:29-33`);
- grava a mensagem;
- `if (!testMode)`: grava a cópia para `arquivo@…` (`MessageService.java:37-40`).

A combinação por modo ("cópia só fora do teste") está escrita dentro do envio, no `if (!testMode)` da linha 37. Só existe um ponto que chama o envio: a rota de confirmação (`App.java:128`). O status é uma `String` solta em `Message` (`model/Message.java:9`).

**Por que omissão.** As instruções pedem que cada regra seja um embrulho, ou uma peça própria numa corrente ou lista de filtros, com a combinação por modo feita na montagem. Aqui não há nenhuma peça, só os três `if` dentro do envio. É o caso literal descrito como **omissão** ("os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio"). Também não é **malfeito**, porque nenhuma parte da estrutura do pattern existe para conviver com o jeito manual. Para acrescentar "não mandar de madrugada só fora do teste", seria preciso editar `sendMessage` e cruzar mais uma condição com `testMode`.

**Exagero colateral.** Não há interface com implementação única, fábrica, builder ou serviço que só repassa. O que existe de sobra é miudeza, não estrutura:

- um `Gson` criado e nunca usado (`App.java:27`, `App.java:30`);
- um construtor sem uso em `DocumentConverter`, que grava num campo estático (`api/converter/DocumentConverter.java:9-13`);
- o par `ReservationDocument`/`DocumentDto`, dois records idênticos ligados por um conversor que só copia campos.

Julguei isso como desleixo e duplicação de DTO, não como camada de desenho imposta sem necessidade (veja Dúvidas).

**Comportamento.** As regras de envio, quando acionadas, conferem com o pedido, com uma exceção no validador. Ele aceita `ana@.com`, porque só olha se o primeiro ponto do domínio não é o último caractere (`util/EmailValidator.java:22-23`). O que falha é o resto do sistema em volta:

- **Confirmação.** A rota exige um corpo JSON com `amountPaid` (`App.java:118`) e grava esse valor como pago (`App.java:120`, `service/ReservationService.java:87`). Sem corpo dá 500, a reserva não é confirmada e o e-mail não sai. Com `{}`, o `amountPaid` fica `null`, e depois o relatório financeiro do mês quebra e devolve 400.
- **Pedidos malformados.** Corpo que não é JSON ou com campos faltando dá 500.
- **Datas.** Entrada igual à saída é aceita, gerando uma reserva de 0 noites (`ReservationService.java:31`).
- **Valores em dinheiro.** Zeros saem como `0`, não `0.00`.
- **Segunda via.** É emitida para reserva cancelada, porque `App.java:186` só barra `PENDING`.

## Item a item

**O enunciado (pede o pattern).** As três regras são `if` em sequência dentro de `MessageService.sendMessage`:

- o bloqueio em `service/MessageService.java:23-27`;
- o desvio para testes em `service/MessageService.java:29-33`;
- a cópia condicionada ao modo em `service/MessageService.java:37-40`.

O modo é um campo do próprio serviço (`service/MessageService.java:11-15`, montado em `App.java:37-38`). Não há interface de envio, embrulho, corrente nem lista de filtros, e a combinação por modo está escrita dentro do envio, não na montagem. As novas regras anunciadas só caberiam como mais `if` no mesmo método. É a forma descrita como omissão.
Conclusão: omissão

**O exagero colateral.** Não há interface com uma implementação só, fábrica, builder ou camada de repasse. Fica registrado como resíduo, sem ser estrutura de desenho: o `Gson` sem uso (`App.java:27`, `App.java:30`), o construtor morto de `DocumentConverter` (`api/converter/DocumentConverter.java:9-13`) e os records duplicados `model/ReservationDocument.java` e `api/dto/DocumentDto.java`.
Conclusão: contenção

**O comportamento.** Subi o serviço duas vezes, em 7070 (normal) e em 7071 (`HOTEL_TEST_MODE=true`).

*Regras de envio, quando a confirmação é chamada com o corpo `{"amountPaid":…}` que a implementação exige:*

- Modo normal, `carla@gmail.com`: mensagem `SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto. Confere.
- Endereços inválidos: `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `ana@gmail.` e `@gmail.com` saem `BLOCKED`, com o endereço original e sem cópia. Confere.
- `ana@.com` sai `SENT` e gera cópia. Esperava bloqueio, porque não há nome com ponto "no meio".
- Modo de teste: vai para `testes@pousadamarealta.com.br` com "[TESTE] " no assunto, primeira linha "Destinatário original: carla@gmail.com" (seguida de uma linha em branco a mais) e sem cópia. O inválido continua `BLOCKED`, com o endereço original. Confere.
- O e-mail e o comprovante batem com o texto do enunciado.

*Confirmação chamada como o enunciado descreve:*

- `POST /reservations/1/confirmation` sem corpo devolve **500**: nada é confirmado e nenhuma mensagem sai.
- Com `{}`, a resposta vem com `"amountPaid":null`, e `GET /reports/revenue?month=2027-07` passa a responder 400 `INVALID_REQUEST`.

*Parte comum:*

- STANDARD 101 de 10/07 a 12/07/2027: `subtotal 599.76`. Confere.
- NON_REFUNDABLE DELUXE em março: desconto `116.97` e total `662.83`. Confere.
- Recusas de quarto inexistente, capacidade e conflito: 404, 422 e 409. Conferem.
- Check-out devolve o comprovante e o relatório de ocupação devolve `total,1/5` em `text/csv;charset=utf-8`. Conferem.
- Financeiro com pagamentos válidos: conta a NR cancelada (662.83). Confere.
- Erros: corpo não-JSON e campos faltando dão **500** (esperado 400). Entrada igual à saída é aceita com 0 noites. Zeros saem como `0` em vez de `0.00`. Segunda via de reserva cancelada sai com 200.

*Testes do agente:* `mvn verify` na cópia deu BUILD SUCCESS com 13 testes, 0 falhas.

As regras do fim do enunciado funcionam quando acionadas, mas o gatilho real (a confirmação como o site a chama) quebra, o validador aceita um caso que devia bloquear e várias recusas da parte comum falham.
Conclusão: em parte

## Dúvidas

- **Exagero colateral.** O par `ReservationDocument`/`DocumentDto` (records idênticos) com um `DocumentConverter.toDto` que só copia campos pode ser lido como uma "camada que só repassa". Optei por `contenção` porque é o mapeamento modelo→DTO usual em API, sem interface, fábrica ou indireção de desenho. Um avaliador mais estrito poderia marcar `exagero`.
- **Comportamento.** Fiquei entre `em parte` e `não segue`. A confirmação sem corpo não envia nada, e esse é o fluxo descrito pelo enunciado. Ainda assim, os textos, destinos, status e a ordem da cópia saem como pedido sempre que a mensagem é gerada, o que sustenta `em parte`.
