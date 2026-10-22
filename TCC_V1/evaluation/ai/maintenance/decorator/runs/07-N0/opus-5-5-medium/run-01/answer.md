# Cenário 07

## Visão geral

O agente pôs as três regras pedidas dentro de `EmailSender.send`, uma depois da outra, em `if`s: valida o endereço, olha se está em modo de teste e, se não estiver, faz a cópia para o arquivo (`final/src/main/java/hotel/messages/EmailSender.java:21-36`). Não criou uma interface de envio, nem embrulhos, nem lista de regras. A combinação de regras por modo também ficou fixa dentro do envio, e não na montagem: `Hotel` monta o `EmailSender` como na base (`final/src/main/java/hotel/Hotel.java:24`). O comportamento pedido está quase todo certo, e os testes do agente cobrem os dois modos. O problema sério é o desenho: as regras anunciadas (madrugada, limite por dia) e a "segunda pousada sem cópia" só cabem mexendo de novo nesse método, com mais `if`s.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin pequeno, com estes pacotes:
- `rooms`: os quartos;
- `reservations`: `ReservationService`, `PriceCalculator` e `ReservationStore`, que guarda os dados em memória;
- `messages`: `EmailSender`, `SentMessages` e `Message`;
- `reports`, `documents` e `web`: `HttpApi`, com as rotas.

A montagem fica em `Hotel` (`final/src/main/java/hotel/Hotel.java:19-28`), que cria as peças e sobe o servidor na porta da variável `PORT`. A confirmação de uma reserva sai da rota em `HttpApi`, passa por `ReservationService`, que chama `emailSender.send(...)` (`final/src/main/java/hotel/reservations/ReservationService.java:69`), e o envio grava em `SentMessages`. O `GET /messages` devolve `sentMessages.all()` (`final/src/main/java/hotel/web/HttpApi.java:81`).

**O que mudou:**
- `Message` ganhou o campo `status` como `String` (`final/src/main/java/hotel/messages/Message.java:3`), e `SentMessages.add` passou a recebê-lo.
- Duas classes novas: `EmailValidator`, com um método estático, e `TestModeProvider`, uma interface funcional que lê `HOTEL_TEST_MODE` (`final/src/main/java/hotel/messages/TestModeProvider.java:3-8`).
- Testes: o agente escreveu `EmailSenderTest` e `EmailValidatorTest`, ajustou `ReservationServiceTest` e `HttpApiTest` para a cópia (agora são 2 mensagens) e acrescentou um teste de bloqueio a partir da reserva.

**A parte que pede o pattern.** Não há representação própria para cada regra. As três estão escritas em sequência dentro de um único método:
- o bloqueio está em `EmailSender.java:22-24`;
- o desvio para o teste, em `EmailSender.java:26-30`, com `return` antecipado, e é isso que suprime a cópia;
- a cópia, em `EmailSender.java:32-34`.

A regra de combinação ("cópia só fora do teste, bloqueio sempre") está codificada na ordem dos `if`s e nesse `return`, dentro do envio. Nada disso se decide na montagem. Para desligar a cópia numa segunda pousada, ou ligar a regra da madrugada só no teste, é preciso editar o método.

**O julgamento.** As instruções do pattern dizem que é omissão quando "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio", e é exatamente o caso. Não existe nenhuma estrutura parcial que justifique "malfeito": não há embrulho nem corrente, e o envio de dentro é o próprio lugar das regras.

Sobre o exagero colateral, a `TestModeProvider` tem uma única implementação em produção, mas os testes a trocam por `() -> true` (`final/src/test/java/hotel/messages/EmailSenderTest.java:62`). Ela é, portanto, um ponto de substituição usado, e não uma camada vazia. O `EmailValidator` estático é só uma função auxiliar. Não vi estrutura desnecessária.

Sobre o comportamento, conferi por leitura: as regras estão certas, com um desvio pequeno no validador, descrito abaixo.

## Item a item

**O pattern pedido.** O texto pedia que cada regra fosse um embrulho do envio, com a combinação decidida na montagem. O agente escreveu as três regras como `if`s sequenciais dentro de `EmailSender.send`: validação em `final/src/main/java/hotel/messages/EmailSender.java:22`, modo de teste em `:26` e cópia em `:32-34`. A combinação por modo é a ordem desses `if`s mais o `return` de `:29`. A montagem em `final/src/main/java/hotel/Hotel.java:24` não escolhe nada. Não há interface de envio, nem decorators, nem lista de filtros, e as regras anunciadas exigiriam mais `if`s no mesmo método. É o caso descrito como omissão nas instruções.
Conclusão: omissão

**O exagero colateral.** Não há. A `TestModeProvider` (`final/src/main/java/hotel/messages/TestModeProvider.java:3-8`) é substituída nos testes (`final/src/test/java/hotel/messages/EmailSenderTest.java:62,77`), e o `EmailValidator` é só um método estático. Nenhuma fábrica, builder ou camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código: não subi o serviço nem chamei a API.

O que confere:
- **Endereço inválido:** sai uma única mensagem `BLOCKED` com o endereço original e sem cópia, nos dois modos, porque a validação vem antes de tudo (`EmailSender.java:22-24`).
- **Modo normal:** sai a mensagem `SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e o mesmo texto (`EmailSender.java:32-34`).
- **Modo de teste:** a mensagem vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …", sem cópia (`EmailSender.java:26-30`).
- **Variável de ambiente:** o modo é ligado por `HOTEL_TEST_MODE`, lida em `TestModeProvider.java:7`. A comparação ignora maiúsculas, o que é tolerável.
- **Campo `status`:** existe na mensagem e sai no JSON (`Message.java:3`).
- **Fluxo de reserva:** continua igual (`ReservationService.java:69`).

O desvio: o validador aceita `ana@.com` e `ana@gmail.`, porque `domain.contains(".")` em `EmailValidator.java:15` não exige que o ponto esteja "no meio", como o pedido diz. Os casos citados pelo dono (`ana@gmail`, `ana.gmail.com`) são recusados.
Conclusão: em parte

## Dúvidas

Há uma só, sobre o comportamento. A falha do validador com ponto na borda do domínio (`ana@.com`, `ana@gmail.`) é uma divergência real da regra escrita, mas é marginal: os casos do dia a dia e os exemplos do pedido funcionam. Fiquei entre "segue" e "em parte" e escolhi "em parte" porque a regra pede explicitamente "um ponto no meio".
