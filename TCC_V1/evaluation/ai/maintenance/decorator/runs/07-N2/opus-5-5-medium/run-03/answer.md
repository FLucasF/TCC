# Cenário 07

## Visão geral

O agente pôs as três regras novas dentro de um único método, `SentMessages.add`. Esse método valida o endereço, pergunta se o sistema está em modo de teste, reescreve a mensagem e faz a cópia para o arquivo, tudo com `if`s em sequência. A combinação de regras por modo é controlada por dois booleanos numa classe `EmailConfiguration`. Não há uma interface de envio, nem um embrulho por regra, nem montagem que empilhe os embrulhos. Isso é a omissão descrita nas instruções: "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". O comportamento pedido funciona nos caminhos principais. A única falha está no validador de endereço, que só verifica se existe um ponto depois do @, e não se ele está "no meio" do nome. Por isso aceita `ana@gmail.` e `ana@.com`. Não há exagero colateral.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin em pacotes por assunto:
- `hotel.rooms`: os quartos.
- `hotel.reservations`: `ReservationService`, `PriceCalculator`, `ReservationStore`, além das entidades e erros.
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios de ocupação e receita.
- `hotel.messages`: `EmailSender`, `SentMessages`, `Message`.
- `hotel.web`: `HttpApi` e os DTOs.

A classe `Hotel` monta tudo à mão no construtor (`final/src/main/java/hotel/Hotel.java:20-30`), e o `main` sobe o servidor na porta da variável `PORT`. Os dados ficam em memória: as reservas no `ReservationStore`, as mensagens na lista de `SentMessages`.

O caminho de uma requisição é este. A rota em `HttpApi` chama o `ReservationService`. Na confirmação, o serviço chama `emailSender.send(...)` (`final/src/main/java/hotel/reservations/ReservationService.java:69`). O `EmailSender.send` só repassa para `sentMessages.add("EMAIL", ...)` (`final/src/main/java/hotel/messages/EmailSender.java:13`). Por fim, `GET /messages` devolve `sentMessages.all()` serializado (`final/src/main/java/hotel/web/HttpApi.java:81`).

O agente escreveu estes testes:
- `EmailValidatorTest`;
- `EmailConfigurationTest`, que só testa getters;
- `SentMessagesTest`, que cobre bloqueio, modo de teste, cópia e cópia desligada;
- `EmailRulesTest`, que vai pela HTTP, mas só no modo normal.

Ele também ajustou `HttpApiTest` e `ReservationServiceTest` para a mensagem extra da cópia e para o campo `status`.

**A parte que pede o pattern.** O agente criou estas peças:
- `MessageStatus` (SENT/BLOCKED);
- o campo `status` no record `Message` (`final/src/main/java/hotel/messages/Message.java:3`);
- `EmailValidator`, um utilitário estático;
- `EmailConfiguration`, com dois booleanos: `testMode`, lido de `HOTEL_TEST_MODE`, e `archiveCopy`, sempre `true` no construtor padrão (`final/src/main/java/hotel/messages/EmailConfiguration.java:8-11`).

Todas as regras vivem em `SentMessages.add` (`final/src/main/java/hotel/messages/SentMessages.java:20-56`):
- o bloqueio é um `if (!EmailValidator.isValid(to))`, que grava a mensagem como BLOCKED e retorna (`:26-30`);
- o desvio de teste é um `if (config.isTestMode())`, que reescreve destinatário, assunto e corpo (`:32-39`);
- a cópia é um `if (!config.isTestMode() && config.shouldArchiveCopy())`, que acrescenta a segunda mensagem (`:44-53`).

Nenhuma regra é uma peça própria. A combinação "cópia só fora do teste" está escrita dentro da própria lógica do envio (`:44`). Para acrescentar as regras anunciadas (madrugada, limite por dia, cada uma valendo sempre, só no teste ou só fora), seria preciso abrir esse método de novo e pôr mais `if`s e mais booleanos na `EmailConfiguration`. `Hotel` não decide nada sobre a combinação: só cria a configuração (`final/src/main/java/hotel/Hotel.java:23-24`).

**O raciocínio do julgamento.** O Decorator pede uma interface de envio, o envio real e um embrulho por regra, com a combinação decidida na montagem. Nada disso existe aqui:
- `EmailSender` continua sendo uma classe final concreta, sem interface;
- não há nenhum embrulho;
- não há lista de filtros nem corrente.

Os três `if`s estão juntos dentro do envio. É o caso literal de omissão nas instruções. Não é malfeito, porque não existe estrutura parcial do pattern convivendo com o jeito antigo. Simplesmente não existe a estrutura.

Quanto ao exagero colateral, `EmailConfiguration` e `EmailValidator` são peças simples e usadas, e nenhuma camada só repassa. Por isso a conclusão é contenção.

Quanto ao comportamento, conferido por leitura: bloqueio, status, cópia logo depois da original, desvio em modo de teste e ausência de cópia no teste estão certos. A falha é o validador: `after.contains(".")` (`final/src/main/java/hotel/messages/EmailValidator.java:33`) aceita um ponto no início ou no fim do domínio.

## Item a item

**O pattern pedido.** As três regras são `if`s em sequência dentro de `SentMessages.add`:
- a validação, em `final/src/main/java/hotel/messages/SentMessages.java:26`;
- o modo de teste, em `final/src/main/java/hotel/messages/SentMessages.java:32`;
- a cópia condicionada ao modo, em `final/src/main/java/hotel/messages/SentMessages.java:44`.

O envio (`final/src/main/java/hotel/messages/EmailSender.java:12-14`) continua concreto e sem interface. A montagem (`final/src/main/java/hotel/Hotel.java:23-24`) não empilha nada: só passa uma configuração de booleanos (`final/src/main/java/hotel/messages/EmailConfiguration.java:5-6`) que o próprio envio consulta. Nenhuma regra é uma peça intercambiável. As novas regras (madrugada, limite por dia) e as novas combinações por instalação exigiriam editar o mesmo método. É exatamente a omissão descrita nas instruções.
Conclusão: omissão

**O exagero colateral.** Não há. `EmailConfiguration` (`final/src/main/java/hotel/messages/EmailConfiguration.java:3`) é um portador simples de opções, e `EmailValidator` (`final/src/main/java/hotel/messages/EmailValidator.java:3`) é um utilitário estático usado de fato. Não foram criadas interfaces de uma implementação só, fábricas, builders nem camadas de repasse.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço:
- Endereço inválido: a mensagem é gravada como `BLOCKED`, com o endereço original e sem cópia, também no modo de teste (`final/src/main/java/hotel/messages/SentMessages.java:26-30`). Confere.
- Modo normal: a mensagem sai `SENT` e logo depois vem a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " + o assunto original e o mesmo corpo (`final/src/main/java/hotel/messages/SentMessages.java:41-53`). Confere.
- `HOTEL_TEST_MODE=true`: destinatário `testes@pousadamarealta.com.br`, assunto "[TESTE] ", primeira linha "Destinatário original: …" e nenhuma cópia (`final/src/main/java/hotel/messages/SentMessages.java:32-35`, `:44`; a leitura da variável está em `final/src/main/java/hotel/messages/EmailConfiguration.java:9`). Confere.
- O campo `status` sai no JSON pelo record (`final/src/main/java/hotel/messages/Message.java:3`). Confere.
- Validador: as verificações de um único @, de algo antes e de algo depois estão certas. Mas a regra "um nome com pelo menos um ponto no meio" virou só `after.contains(".")` (`final/src/main/java/hotel/messages/EmailValidator.java:33`). Por isso `ana@gmail.` e `ana@.com` são aceitos e enviados, quando deveriam ser bloqueados. Não confere.

O resto do sistema não foi alterado além do ajuste no número de mensagens.
Conclusão: em parte

## Dúvidas

No comportamento, hesitei entre `segue` e `em parte`. Os caminhos principais e os exemplos do pedido (`ana@gmail`, `ana.gmail.com`) funcionam. O desvio está só nos casos de ponto na borda do domínio. Escolhi `em parte` porque o pedido diz explicitamente "ponto no meio", e o código não verifica isso.
