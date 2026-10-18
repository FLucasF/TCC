# Cenário 07

## Visão geral

O agente pôs as três regras novas (bloquear endereço inválido, desviar para a caixa de testes no modo de teste, mandar a cópia para o arquivo) como três `if` seguidos dentro de `SentMessages.add`. Essa é a classe que deveria só guardar a lista de mensagens. Não criou interface de envio, nem embrulho por regra, nem montagem por modo em `Hotel`. O modo de teste é lido de uma constante estática dentro da própria lista. A regra da cópia sabe do modo de teste: ela só roda quando o modo está desligado. As regras anunciadas (madrugada, limite por dia, que podem valer "sempre, só no teste ou só fora dele") vão exigir mais `if` nesse mesmo método. O que funciona bem: o desvio para a caixa de testes, a cópia logo depois da original e o campo `status`. O problema mais sério é de desenho, e é omissão do pattern. Há também um erro de comportamento: endereços como `ana@.com` e `ana@gmail.`, cujo ponto não está "no meio" do nome, passam como válidos e são enviados.

## Item a item

**O pedido (pede o pattern).** As três regras estão no mesmo método, como condicionais em sequência:
- o bloqueio está em `src/main/java/hotel/messages/SentMessages.java:16-20`;
- o desvio de teste está em `src/main/java/hotel/messages/SentMessages.java:26-30`;
- a cópia está em `src/main/java/hotel/messages/SentMessages.java:35-38`, guardada por `if (!TEST_MODE)`.

O modo vem de `System.getenv` numa constante estática (`src/main/java/hotel/messages/SentMessages.java:11`). A combinação por modo fica, portanto, dentro do código das regras, e não na montagem. `EmailSender.send` continua delegando direto à lista (`src/main/java/hotel/messages/EmailSender.java:12-14`). A montagem em `Hotel` também não mudou: continua `new EmailSender(sentMessages)` (`src/main/java/hotel/Hotel.java:22-24`). Não há interface de envio, embrulho, corrente nem lista de filtros. É o caso que as instruções descrevem como omissão: os três `if` dentro do envio, aqui na própria classe que guarda as mensagens. Para ligar uma regra nova "só no teste" ou "só fora dele", seria preciso mais um `if` com `TEST_MODE` nesse método.
Conclusão: omissão

**O exagero colateral.** O patch só toca `Message.java` (o campo `status`), `SentMessages.java` e dois testes. Não há interface, fábrica, builder nem camada nova em outra parte do código.
Conclusão: contenção

**O comportamento.** Subi a cópia da `final/` duas vezes: na porta 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Criei e confirmei reservas via `POST /reservations` e `POST /reservations/{id}/confirmation` e conferi `GET /messages`. O `mvn -q verify` passou: 27 testes, 0 falhas (5 de relatórios, 6 de preço, 12 do serviço de reservas, 4 da API HTTP). Os testes do agente cobrem o bloqueio e a cópia, mas não o modo de teste.

O que confere com o pedido:
- **Modo normal:** `ana@gmail.com` gera a mensagem `SENT`. Logo depois dela vem a cópia para `arquivo@pousadamarealta.com.br`, com assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", corpo idêntico e `status` `SENT`.
- **Bloqueio, nos dois modos:** `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `@gmail.com` aparecem como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:** a mensagem vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: ana@gmail.com". Não sai cópia.
- **O que o pedido não mudou:** o comprovante final do check-out, os relatórios de ocupação e de receita (CSV) e o erro `INVALID_REQUEST` do corpo incompleto continuam como antes.

O que não confere: mandei `ana@.com` e `ana@gmail.`. Esperava `BLOCKED`, porque o pedido exige um nome com "pelo menos um ponto no meio" depois do @. Veio `SENT`, com a cópia para o arquivo no modo normal e o desvio para a caixa de testes no modo de teste. A causa é que a validação só confere `domain.contains(".")` (`src/main/java/hotel/messages/SentMessages.java:62-63`), sem exigir nada antes nem depois do ponto.
Conclusão: em parte

## Dúvidas

O comportamento ficou entre `em parte` e `não segue`. Escolhi `em parte` porque só a regra do endereço válido erra, e só nos casos de borda: os exemplos do próprio pedido (`ana@gmail`, `ana.gmail.com`) são bloqueados, e todas as outras regras conferem. Quanto ao desenho, não há dúvida.
