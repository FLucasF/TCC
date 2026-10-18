# Cenário 07

## Visão geral

O agente pôs as três regras novas dentro do próprio `EmailSender.send`, sem nenhuma estrutura: um `if` para o endereço inválido, um para o modo de teste e um para a cópia. O modo de teste é lido do ambiente ali mesmo, e a combinação das regras conforme o modo também fica nesse método. A classe `Hotel` continua montando um `EmailSender` concreto e nada mais. Separar a validação em `EmailValidator` e o `status` em enum ficou bom. O problema mais sério é de desenho: não há onde encaixar as regras já anunciadas (não mandar de madrugada, limite por dia, cada uma valendo sempre, só no teste ou só fora dele) sem mais `if` dentro do `send`. No comportamento quase tudo confere. A exceção é a validação, que aceita endereços com o ponto no começo ou no fim do domínio.

## Item a item

**O pedido (pede o pattern).** Em `final/src/main/java/hotel/messages/EmailSender.java:13-15` o envio bloqueia o endereço inválido. Em `EmailSender.java:17-18` ele lê `HOTEL_TEST_MODE` com `System.getenv`. Em `EmailSender.java:24-28` ele desvia a mensagem para a caixa de testes, e em `EmailSender.java:32-34` faz a cópia para o arquivo só fora do teste. São os três `if` dentro do `send`, que é o caso de omissão descrito nas instruções. Não existe interface de envio, não há um embrulho por regra, e a combinação por modo não é decidida na montagem. Em `final/src/main/java/hotel/Hotel.java:24` continua `new EmailSender(sentMessages)`, como na base. A única peça separada é `final/src/main/java/hotel/messages/EmailValidator.java`, um utilitário estático chamado de dentro do `send`, não uma regra em volta do envio. Cada regra nova (madrugada, limite por dia) vai exigir mais um `if` e mais uma verificação de modo nesse mesmo método.
Conclusão: omissão

**O exagero colateral.** Não há. As únicas peças novas são o enum `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3-6`), que o próprio pedido exige com o campo `status`, e o `EmailValidator`, que é uma classe utilitária sem interface nem fábrica. Fora delas, as mudanças se limitam ao `Message`, ao `SentMessages` e aos testes.
Conclusão: contenção

**O comportamento.** Montei uma cópia da `final/`. O `mvn verify` passou: 30 testes, nenhuma falha, incluindo os do agente (2 em `EmailValidatorTest`, 2 novos em `HttpApiTest` e os ajustes em `ReservationServiceTest`). Subi o serviço duas vezes, normal na porta 7070 e com `HOTEL_TEST_MODE=true` na 7071. Em cada uma criei e confirmei reservas com os endereços `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `ana@.com` e `ana@gmail.`, e depois consultei `GET /messages`.

O que confere:
- **Modo normal:** `carla@example.com` sai como `SENT` e logo depois vem a cópia para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", o mesmo corpo e `SENT`. `ana@gmail` e `ana.gmail.com` aparecem como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:** a mensagem vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: carla@example.com", sem cópia. Os inválidos continuam `BLOCKED`, com o endereço original.
- **O que não mudou:** check-in, check-out com o comprovante, relatório `/reports/revenue?month=2027-07` e o erro `OPERATION_NOT_ALLOWED` ao confirmar de novo continuam como antes.

O que não confere é a validação. O pedido diz que, depois do @, tem de vir "um nome com pelo menos um ponto no meio". O código só conta se há algum ponto (`EmailValidator.java:31-38`) e não olha a posição dele. Por isso `ana@.com` e `ana@gmail.` saíram como `SENT`, com cópia no modo normal e desviados no modo de teste. O esperado era `BLOCKED`. No fim derrubei os dois serviços.
Conclusão: em parte

## Dúvidas

Ler "ponto no meio" como "nem no começo nem no fim do domínio" é interpretação minha do texto do pedido. Os dois exemplos do pedido (`ana@gmail` e `ana.gmail.com`) são bloqueados certo. Se a leitura for mais frouxa, o comportamento passaria a `segue`, mas a classe do desenho não muda.
