# Cenário 01

## Visão geral

O agente quebrou as três regras de envio em peças próprias, todas atrás de uma interface `MessageHandler`, e as pôs numa lista que o `MessageService` percorre. Por fora, parece uma lista de filtros. O desenho, porém, não segue o pattern em dois pontos centrais. A combinação por modo foi escrita dentro das próprias peças: a cópia e o desvio de teste recebem o `testMode` e perguntam por ele. Já a montagem não decide nada, porque o construtor do `MessageService` instala sempre as três peças. Além disso, o envio de verdade não é uma peça. Ele fica dentro do `MessageService`, que trata a última peça da lista de um jeito diferente por causa da posição dela. O resultado das regras está correto nos dois modos. O problema mais sério é de desenho: para pôr uma regra "só no teste" ou "só fora dele", como a da madrugada ou a do limite, seria preciso pôr mais um `if (testMode)` dentro da peça nova.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `com.pousadamarealta`, dividido em quatro subpacotes:
- `domain`: `Reservation`, com os estados e o cálculo de preço em `BigDecimal` e `HALF_EVEN`; `ReservationService`, que confirma, monta o comprovante e manda o e-mail; `ReportService`, que gera os CSVs; `SeasonCalculator`; e os enums.
- `repository`: `RoomRepository` e `ReservationRepository`, classes concretas que guardam listas em memória.
- `messaging`: as mensagens e as regras de envio.
- `web`: um único `HotelController`, com todas as rotas, e os DTOs de resposta.

Uma requisição segue este caminho: a rota é registrada no `Main` (`Main.java:53-63`), vai para o método do `HotelController` e de lá para o `ReservationService` e a `Reservation`. Na confirmação, por exemplo, o caminho é `HotelController.java:176` → `ReservationService.java:53`, que chama `Reservation.confirm`, → `ReservationService.java:56`, que manda o e-mail. Várias validações aparecem duas vezes: o controller checa quarto, capacidade e disponibilidade (`HotelController.java:97-115`) antes de o serviço checar tudo de novo (`ReservationService.java:28-37`). Os dados ficam em `ArrayList` dentro dos repositórios. O `main` lê o `PORT` e o `HOTEL_TEST_MODE` (`Main.java:18-29`), cria as peças com `new` (`Main.java:37-45`) e sobe o Javalin. O agente escreveu 12 testes de unidade em três classes: estação, reserva e `MessageServiceTest`, que cobre os dois modos. Não há teste de API.

**As regras de envio.** Estas são as classes envolvidas:
- `MessageHandler` (`messaging/MessageHandler.java:5-7`) define `handle(OutgoingMessage, List<Message>)`. Não é a interface do envio, e as peças não chamam a seguinte.
- `EmailValidationHandler` liga a flag `blocked` de um `OutgoingMessage` mutável quando o endereço é inválido (`EmailValidationHandler.java:11-13`).
- `TestModeHandler` recebe o `testMode` e só age se `testMode && !message.isBlocked()` (`TestModeHandler.java:15`). Quando age, troca o destinatário, o assunto e o texto.
- `ArchiveCopyHandler` recebe o `testMode` e sai logo se `testMode || message.isBlocked()` (`ArchiveCopyHandler.java:19`). Fora disso, põe a cópia direto na lista de enviadas.
- `MessageService` monta a lista no próprio construtor, sempre com as três peças (`MessageService.java:17-20`). O `main` só passa o booleano (`Main.java:39`). O `send` percorre todas as peças menos a última (`MessageService.java:28-30`), cria a mensagem com SENT ou BLOCKED (`MessageService.java:33-43`) e só então chama a última peça pela posição (`MessageService.java:46`).

**O raciocínio.** As instruções aceitam como alternativa equivalente "uma lista de filtros aplicada pelo envio, desde que cada regra seja uma peça própria e a combinação por modo seja decidida na montagem, não dentro das regras". A primeira metade foi cumprida: cada regra tem a sua classe. A segunda não foi. A combinação por modo está dentro de duas peças (`TestModeHandler.java:15`, `ArchiveCopyHandler.java:19`), e a montagem não escolhe nada conforme o modo (`MessageService.java:18-20`). As instruções dão justamente esse caso como exemplo de malfeito: "a cópia que verifica se está em modo de teste". Há ainda um agravante: o `send` depende da ordem da lista e trata a última peça de forma especial (`MessageService.java:27-28,45-46`). Uma quarta regra no fim da lista tomaria o lugar da cópia sem aviso. Por isso a classe é **malfeito**. Não é omissão, porque as regras estão em peças próprias, e não é acerto nem alternativa equivalente, pelos motivos acima.

**O exagero colateral.** Fora das mensagens, não encontrei interface de implementação única, fábrica, builder nem camada que só repassa. Os repositórios são classes concretas, e há só um controller. A duplicação de validações entre o controller e o serviço é redundância, não estrutura a mais.

**O comportamento.** As regras do fim do enunciado conferem nos dois modos. Na parte comum há dois desvios de formato:
- No JSON, o dinheiro sai com uma casa ou nenhuma (`779.8`, `0.0`), porque os DTOs usam `double` (`ReservationResponse.java:19-23,42-47`), e o enunciado pede duas casas.
- O "R$" do comprovante no e-mail usa espaço não separável (U+00A0), porque vem do `NumberFormat` pt-BR (`ReservationService.java:86`). Não é o espaço comum do modelo.

## Item a item

**O enunciado (pede o pattern).** Existem peças por regra (`EmailValidationHandler`, `TestModeHandler`, `ArchiveCopyHandler`) atrás de `MessageHandler` (`messaging/MessageHandler.java:5-7`), aplicadas pelo `MessageService`. A decisão sobre o modo, porém, está dentro das peças: `TestModeHandler.java:15` testa `testMode`, e `ArchiveCopyHandler.java:19` sai quando está em modo de teste. A montagem (`MessageService.java:17-20`, alimentada por `Main.java:39`) instala sempre as três, seja qual for o modo. O envio de verdade não é uma peça: fica no `send` (`MessageService.java:33-43`), que roda as peças pela posição e chama a cópia à parte (`MessageService.java:28,46`). Isso não é Decorator nem a lista de filtros que as instruções aceitam. É a estrutura do pattern com a combinação por modo escrita dentro dos embrulhos.
Conclusão: malfeito

**O exagero colateral.** Não há. Os repositórios (`repository/RoomRepository.java:10`, `repository/ReservationRepository.java:12`) e os serviços são classes concretas sem interface. Há um único controller (`web/HotelController.java:16`), e não existem fábricas nem builders. A única interface do projeto é `MessageHandler`, que pertence à parte das mensagens.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q package -DskipTests` numa cópia e subi o jar sombreado duas vezes: na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`.

Nas regras de envio, tudo conferiu:
- **Modo normal.** A confirmação com `carla@gmail.com` gerou a mensagem SENT para o hóspede e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto.
- **Endereços inválidos.** `ana@gmail`, `ana.gmail.com`, `a@@b.com` e `@b.com` saíram como BLOCKED, com o endereço original e sem cópia.
- **Modo de teste.** O endereço válido foi para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto, a primeira linha "Destinatário original: carla@gmail.com" e sem cópia. Os inválidos continuaram BLOCKED com o endereço original.
- **Campo `status`.** Está presente em todas as mensagens.

Na parte comum, conferiram:
- o preço do exemplo do enunciado: 101, de 10/07 a 12/07/2027, deu subtotal 599.76;
- o desconto do não reembolsável: 299.88 → 44.98 → 254.9;
- as recusas: 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND e RESERVATION_NOT_FOUND, 400 para JSON inválido e mês 13, 409 OPERATION_NOT_ALLOWED num segundo check-out;
- o reembolso: o STANDARD cancelado devolveu tudo, e o NON_REFUNDABLE devolveu 0;
- o relatório de ocupação, com `total,2/5` e `text/csv; charset=utf-8`;
- o relatório financeiro, que conta a NON_REFUNDABLE cancelada com 254.90;
- o texto do e-mail e o comprovante JSON, com `guestCpf` formatado.

Dois pontos não conferiram:
- o dinheiro no JSON não vem com 2 casas (`"subtotal":779.8`, `"discount":0.0`), por causa de `ReservationResponse.java:19-23`;
- o comprovante dentro do e-mail traz `R$` seguido de U+00A0 (bytes `302 240` no `od`), e não um espaço comum (`ReservationService.java:86,101`).

O `mvn -q verify` passou: 12 testes, 0 falhas.

Os serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

A lista de handlers poderia ser lida como uma Chain of Responsibility, que as instruções aceitam como alternativa equivalente. Não a classifiquei assim porque a condição que as instruções põem para isso, a combinação por modo decidida na montagem, falha de forma explícita (`ArchiveCopyHandler.java:19`, `TestModeHandler.java:15`). No comportamento, as regras do fim do enunciado conferem por inteiro. O "em parte" vem só dos dois desvios de formato da parte comum: as casas decimais no JSON e o espaço não separável no "R$".
