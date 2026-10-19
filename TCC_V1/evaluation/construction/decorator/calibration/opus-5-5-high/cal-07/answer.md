# Cenário 07

## Visão geral

O agente resolveu as regras de envio com um Decorator. Há uma interface `MessageSender` e um envio real (`EmailSender`), que só grava a mensagem na lista. Cada regra é um embrulho próprio: `BlockInvalidAddress`, `ArchiveCopy` e `RedirectToTestInbox`. A pilha é montada em `Hotel.messageSender`, que escolhe os embrulhos conforme `HOTEL_TEST_MODE`. Nenhum embrulho e nem o envio de dentro perguntam pelo modo, e a reserva só conhece a interface. O resto do sistema é enxuto, sem interfaces nem fábricas sobrando. Nos testes contra o serviço, todas as regras da parte final e da parte comum deram certo, e os 29 testes do agente passam. O ponto mais fraco é pequeno: o bloqueio grava a mensagem `BLOCKED` direto na lista (`SentMessages`), em vez de passar pela cadeia. Isso funciona, mas prende esse embrulho ao armazenamento das mensagens.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido em subpacotes:
- `rooms`: os cinco quartos fixos no código (`Rooms.java:10-15`).
- `reservations`: o `ReservationService` (o ciclo de vida da reserva), a `Reservation`, o `PriceCalculator` (alta temporada e desconto, com arredondamento `HALF_EVEN` a cada passo, `PriceCalculator.java:33-52`) e o `ReservationStore`, um `LinkedHashMap` em memória.
- `documents`: o comprovante, um record com o texto formatado.
- `reports`: os dois CSV.
- `messages`: o envio e as regras.
- `web`: o `HttpApi`, com as rotas Javalin e o mapeamento de erro para status HTTP (`HttpApi.java:100-107`).

O caminho de uma requisição é sempre o mesmo. A rota lê e valida o corpo (`HttpApi.java:109-130`), chama o `ReservationService` e devolve um `ReservationResponse` ou o comprovante. As falhas viram `HotelException`, tratada num único lugar (`HttpApi.java:94-97`). A classe `Hotel` monta tudo à mão e sobe o servidor na porta de `PORT`, com 7070 se a variável não vier (`Hotel.java:23-32`, `55-58`). O jar sombreado, com a classe principal no manifesto, é `target/hotel.jar`. O agente escreveu testes de preço, de serviço, de relatórios, de API e das regras de envio, 29 no total.

**A parte que pede o pattern.**
- `MessageSender` (`MessageSender.java:4-7`) é a interface única do envio, com `send(to, subject, body)`.
- `EmailSender` é o envio real e só grava a mensagem como `SENT` (`EmailSender.java:14`).
- `ArchiveCopy` repassa o envio ao próximo e depois manda pelo mesmo próximo a cópia para `arquivo@...` com "[Cópia] " (`ArchiveCopy.java:16-17`).
- `RedirectToTestInbox` troca o destinatário, o assunto e a primeira linha antes de repassar (`RedirectToTestInbox.java:16`).
- `BlockInvalidAddress` testa o endereço com uma regex (`BlockInvalidAddress.java:9`). Se o endereço for inválido, grava `BLOCKED` com o endereço original e não chama o próximo; se for válido, repassa (`BlockInvalidAddress.java:21-24`).

A combinação por modo existe num único lugar, a montagem. Ali o modo escolhe entre `RedirectToTestInbox(email)` e `ArchiveCopy(email)`, e o bloqueio fica sempre por fora (`Hotel.java:40-42`). Quem usa o envio é o `ReservationService`, que recebe a interface e só chama `send` (`ReservationService.java:17`, `69-72`).

**O raciocínio.** É o que as instruções chamam de acerto:
- há uma interface de envio, um envio real e um embrulho por regra, e cada embrulho chama o próximo;
- a escolha de quais regras valem em cada modo está na montagem, não dentro das regras;
- nenhum embrulho consulta `HOTEL_TEST_MODE`, e o `EmailSender` também não;
- as três regras estão dentro da estrutura.

Uma regra nova, como a de madrugada ou o limite diário, entra como mais um embrulho e mais uma linha na montagem. O único desvio é o curto-circuito do bloqueio, que grava direto em `SentMessages` (`BlockInvalidAddress.java:12`, `22`). Não é uma regra fora da estrutura nem pergunta pelo modo, então não muda a classe. O teste de unidade monta as pilhas à mão (`SendingRulesTest.java:21`, `29`, `40`) e não exercita `Hotel.messageSender`, mas eu conferi essa montagem subindo o serviço. Quanto ao exagero, as interfaces que existem são as do pattern; `Rooms`, `ReservationStore`, `PriceCalculator` e os relatórios são classes concretas, e não há fábrica, builder nem camada que só repassa. No comportamento, tudo o que testei bateu com o enunciado.

## Item a item

**O pattern pedido.** O texto pedia que três regras se combinassem de jeitos diferentes conforme o modo, e que mais regras pudessem entrar. O agente fez um Decorator clássico:
- uma interface (`MessageSender.java:4-7`);
- um envio concreto (`EmailSender.java:14`);
- um embrulho por regra (`BlockInvalidAddress.java:20-25`, `ArchiveCopy.java:15-19`, `RedirectToTestInbox.java:15-17`), cada um com o próximo injetado no construtor;
- a pilha decidida na montagem conforme `HOTEL_TEST_MODE` (`Hotel.java:38-43`).

O cliente só vê a interface (`ReservationService.java:69-72`). Nenhuma peça da cadeia pergunta pelo modo, e nenhuma regra ficou fora dela.
Conclusão: acerto

**O exagero colateral.** Não há. A única interface do projeto é a do pattern, que tem quatro implementações. O resto são classes concretas montadas à mão em `Hotel.java:23-32`, sem fábricas, builders ou camadas que só repassam. Os records de resposta (`ReservationResponse.java:9-46`) servem ao formato da API.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia com `mvn -q package -DskipTests` e subi o serviço duas vezes: na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Nas duas, criei e confirmei reservas com os endereços `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `dani@mail.com.br`.

- **Modo normal:** cada envio válido saiu como `SENT` para o hóspede e foi seguido, logo depois, da cópia para `arquivo@pousadamarealta.com.br`. A cópia tem o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o corpo idêntico ao original (conferido com `jq`). Os três inválidos saíram como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:** os válidos foram para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto, a primeira linha "Destinatário original: carla@example.com" e sem cópia. Os inválidos continuaram `BLOCKED`, com o endereço original.
- **Corpo do e-mail de confirmação:** idêntico ao modelo do enunciado.
- **Preços:** STANDARD de 10/07 a 12/07/2027 deu 599.76; NON_REFUNDABLE de uma noite no 102 em julho deu 299.88 − 44.98 = 254.90.
- **Recusas:** 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 400 `INVALID_REQUEST` (corpo que não é JSON e mês `2027-7`), 409 `OPERATION_NOT_ALLOWED` (confirmar duas vezes) e 404 `RESERVATION_NOT_FOUND`.
- **Cancelamentos:** o não reembolsável devolveu 0.00 e o normal devolveu 599.76.
- **Check-out:** devolveu o comprovante com todos os campos. A segunda via de uma reserva cancelada deu 409.
- **Relatórios:** ocupação `total,1/5` com `text/csv; charset=utf-8`; o financeiro de 2027-07 incluiu a não reembolsável cancelada e deu `total,1314.28`.
- **`mvn verify` da cópia:** 29 testes, 0 falhas, `BUILD SUCCESS`.

No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. O único ponto discutível é o `BlockInvalidAddress` gravar a mensagem bloqueada direto em `SentMessages` (`BlockInvalidAddress.java:22`), em vez de delegar à cadeia. É um curto-circuito legítimo de um embrulho, não uma regra fora da estrutura nem uma consulta ao modo, por isso o julgamento ficou em acerto.
