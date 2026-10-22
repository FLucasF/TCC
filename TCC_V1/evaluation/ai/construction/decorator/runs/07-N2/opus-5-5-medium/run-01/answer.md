Terminei de ler o enunciado e o código. Segue a correção.

# Cenário 07

## Visão geral

O agente resolveu as regras de envio com uma lista de filtros. Há uma interface `MessageRule` e uma classe por regra (validação do endereço, cópia para o arquivo, desvio para a caixa de testes). Um `MessageProcessor` aplica as regras em sequência, e o `App` monta a lista conforme o modo. Nenhuma regra e nenhum serviço pergunta pelo modo de teste, e a combinação fica toda na montagem: é a alternativa que as instruções aceitam de antemão. O problema mais sério é de comportamento e vem da ordem montada. No modo de teste, o desvio para `testes@…` roda antes da validação, que então confere o endereço de testes, que é sempre válido. Por isso, no modo de teste, uma mensagem para "ana@gmail" sai como `SENT` para a caixa de testes em vez de ficar `BLOCKED` com o endereço original. O enunciado pede exatamente o contrário.

## Como foi implementado, e por que o julgamento

**O projeto.** Os pacotes são estes:
- `domain`: `Reservation` (preço, desconto, transições de estado), `Document` (o comprovante e o texto dele), `Message`/`MessageStatus`, `Room`, `RoomType`, `RatePlan`.
- `service`: `ReservationService` (as reservas num `ConcurrentHashMap`, a disponibilidade, os filtros dos relatórios), `RoomService` (os cinco quartos fixos), `ReportService` (os CSV) e `MessageService` (a lista de mensagens enviadas).
- `api`: um controller por recurso, que lê o corpo como `Map`, traduz as exceções nos códigos de erro e monta os DTOs à mão.
- `messaging`: as regras de envio.

Uma requisição segue este caminho. A rota no `App.java:51-61` chama o controller, o controller chama o serviço, e o serviço chama o domínio. Na confirmação, o próprio controller monta o `Document` e chama `messageService.sendConfirmationEmail` (`api/ReservationController.java:98-99`). Tudo fica em memória. O `App.main` lê `PORT` e `HOTEL_TEST_MODE`, monta as peças e sobe o Javalin (`App.java:19-33`). O agente escreveu três grupos de testes:
- testes unitários de `Reservation`;
- `MessageRulesTest`, que testa cada regra sozinha e duas combinações;
- um `IntegrationTest`, que monta os serviços sem HTTP e só usa a combinação do modo normal.

**A parte do pattern.**
- `messaging/MessageRule.java:6-8` define `List<Message> process(List<Message>)`.
- `EmailValidationRule` marca como `BLOCKED` o que não casa com `^[^@]+@[^@]+\.[^@]+$` (`messaging/EmailValidationRule.java:10,19-24`).
- `ArchiveCopyRule` põe, logo depois de cada mensagem `SENT`, uma cópia para o arquivo com "[Cópia] " no assunto (`messaging/ArchiveCopyRule.java:21-26,30-38`).
- `TestModeRule` troca o destinatário, prefixa "[TESTE] " e põe "Destinatário original: …" na primeira linha do texto (`messaging/TestModeRule.java:21-25`).
- `MessageProcessor` passa a lista por cada regra, na ordem (`messaging/MessageProcessor.java:14-20`).
- `MessageService.sendEmail` cria a mensagem como `SENT`, passa pelo processador e grava o que voltou (`service/MessageService.java:37-53`). Ele não sabe nada de modo nem de regras.
- A combinação por modo está em `App.buildMessageProcessor` (`App.java:66-80`): `TestModeRule` só no teste, `EmailValidationRule` sempre, `ArchiveCopyRule` só fora do teste.

**O raciocínio.** Não é o Decorator estrito, porque nenhuma regra embrulha um envio nem chama o próximo. É uma lista de filtros aplicada por quem envia. As instruções aceitam isso como alternativa equivalente com duas condições, e as duas são cumpridas: cada regra é uma peça própria, e a combinação por modo é decidida na montagem (`App.java:69-77`). Nenhum dos sinais de malfeito aparece:
- o envio não pergunta pelo modo (`service/MessageService.java:37-53`);
- nenhuma regra decide pelo modo; a cópia olha o `status` (`ArchiveCopyRule.java:23`), não o modo;
- as três regras estão na estrutura.

As regras novas anunciadas (madrugada, limite por dia) entrariam como novas `MessageRule` acrescentadas na montagem. Sobre o exagero colateral: fora do pattern não há interfaces de uma implementação só, fábricas nem camadas que só repassam. Sobre o comportamento: o desenho aceita a combinação certa, mas a ordem escolhida em `App.java:69-73` põe o desvio antes da validação e quebra o bloqueio no modo de teste. O `messageProcessorShouldApplyRulesInOrder` (`test/.../MessageRulesTest.java:58-72`) testa essa mesma ordem só com um endereço válido, e por isso não pegou o erro.

## Item a item

**O pattern pedido.** As três regras são classes próprias que implementam `MessageRule`: `EmailValidationRule.java:9`, `ArchiveCopyRule.java:9` e `TestModeRule.java:7`. O `MessageProcessor.java:14-20` as aplica em sequência, e quem envia só conhece o processador (`MessageService.java:39`). A escolha por modo está só na montagem (`App.java:66-80`), e nenhuma regra nem o serviço consulta `HOTEL_TEST_MODE`, que só é lido em `App.java:94-97`. Não é um embrulho que chama o próximo, e sim uma lista de filtros com a combinação decidida na montagem. As instruções aceitam isso como equivalente.
Conclusão: alternativa equivalente

**O exagero colateral.** Fora da estrutura das regras, o projeto é direto: serviços concretos, controllers que montam DTOs e o domínio com as regras de preço e de estado. Não há interfaces de uma implementação só, fábricas, builders nem camadas que só repassam. O `MessageProcessor` faz parte da própria alternativa.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço, rodar o Maven nem chamar a API.
- **Modo normal: confere.** Um endereço válido gera a mensagem `SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e o mesmo texto (`ArchiveCopyRule.java:21-38`). Um endereço inválido ("ana@gmail", "ana.gmail.com") fica `BLOCKED` sem cópia (`EmailValidationRule.java:20-21`, `ArchiveCopyRule.java:23`). O `status` sai no GET /messages (`api/MessageController.java:33`).
- **Modo de teste: falha.** Com `HOTEL_TEST_MODE=true`, a `TestModeRule` roda primeiro e troca o `to` para `testes@…` (`App.java:69-73`, `TestModeRule.java:24`). Depois a validação confere esse endereço, que é válido. Esperava uma mensagem `BLOCKED` com o endereço original. O que vem é `SENT` para `testes@…`, com o endereço inválido só na primeira linha do texto. Com endereço válido, o modo de teste confere: vai para testes, com "[TESTE] " no assunto, a linha "Destinatário original: …" e sem cópia.
- **Detalhes menores das mensagens:**
  - A regex aceita um ponto no fim ("a@gmail.com.") e espaços, o que é tolerável diante de "um ponto no meio".
  - As cópias levam ids a partir de 1000000 (`App.java:76`), e o `MessageService` ajusta o contador em seguida (`MessageService.java:44-51`). Os ids das mensagens ficam 1, 1000000, 1000001…, estranhos mas únicos.
- **O resto do sistema, por leitura:**
  - O preço, a alta temporada, o arredondamento meio para o par (`domain/Reservation.java:101-126`), as transições e o reembolso (`Reservation.java:132-165`) conferem.
  - A disponibilidade, os erros e os relatórios também conferem.
  - Desvio: a segunda via é recusada só para `PENDING` (`api/ReservationController.java:162`), então uma reserva `CANCELLED` ainda recebe o comprovante, e o enunciado o limita às reservas pagas.
  - Desvio: o dinheiro vai no JSON como `double` (`ReservationController.java:188-193`), então valores redondos saem como `0.0` ou `100.0`, e não com 2 casas.
Conclusão: em parte

## Dúvidas

A dúvida principal é se o erro de ordem na montagem pesa no desenho, e não só no comportamento. A estrutura permite a combinação certa (bastaria pôr a validação antes do desvio), e nenhum critério de malfeito se aplica. Por isso deixei o erro no comportamento. Mas ele mostra que, numa lista de filtros que mexem na mensagem, a ordem é uma dependência implícita entre as regras.
