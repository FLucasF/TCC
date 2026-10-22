# Cenário 07

## Visão geral

O agente resolveu as regras de envio com uma lista de filtros. A interface `MessageRule` tem três implementações, uma por regra: bloqueio, cópia para o arquivo e desvio para a caixa de testes. Um `MessagePipeline` aplica as regras em sequência. A escolha das regras conforme o modo é feita num só lugar, ao montar o pipeline, e nenhuma regra pergunta pelo modo de teste. Esse desenho está entre as estruturas que as instruções aceitam como equivalentes ao Decorator. O agente não criou estrutura desnecessária. O ponto mais fraco do desenho é o lugar dessa montagem: ela fica dentro do construtor do `MessageService`, que recebe um `boolean`, e não no `main`. O problema mais sério de comportamento está na validação do endereço. Ela só confere se há um ponto em algum lugar depois do @, e por isso aceita `ana@gmail.` e `ana@.com`.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `com.pousadamarealta`, dividido assim:
- `api`: os controllers do Javalin para quartos, reservas, mensagens e relatórios.
- `service`: `ReservationService` (estados, comprovante e e-mail de confirmação), `PricingService` (diárias, alta temporada, desconto, arredondamento `HALF_EVEN`), `ReportService` (os CSVs) e `MessageService`.
- `repository`: repositórios em memória com `HashMap`/`ArrayList` e ids de `AtomicLong` começando em 1.
- `domain`: as entidades e os enums.
- `messaging`: as regras de envio.

Uma requisição passa por três etapas. A rota é registrada em `Main.java:50-63`. O controller valida o JSON e converte as exceções do serviço em códigos HTTP (`ReservationController.java:116-146`). O serviço aplica a regra e grava no repositório. O `Main` lê `PORT` e `HOTEL_TEST_MODE` (`Main.java:22-25`), monta tudo à mão e sobe o Javalin. O agente escreveu dois testes:
- `PricingTest`: preços e desconto.
- `MessageRulesTest`: oito casos das regras de envio, nos dois modos, usando o `MessageService` diretamente.

**A parte do cenário.** O contrato é `MessageRule.apply(OutboundMessage) → List<OutboundMessage>` (`messaging/MessageRule.java:5-7`), e cada regra é uma classe própria:
- `EmailValidationRule` marca a mensagem como bloqueada quando o endereço é inválido (`EmailValidationRule.java:8-11`).
- `ArchiveCopyRule` devolve a original e, logo depois, a cópia para `arquivo@…` com "[Cópia] " no assunto (`ArchiveCopyRule.java:15-26`).
- `TestModeRule` troca o destinatário, o assunto e a primeira linha do texto (`TestModeRule.java:14-17`).

As duas últimas deixam passar sem mudança uma mensagem já bloqueada (`ArchiveCopyRule.java:11`, `TestModeRule.java:10`). Essa verificação olha um dado da mensagem, não o modo.

O `MessagePipeline` aplica cada regra a cada mensagem que sai da regra anterior (`MessagePipeline.java:16-22`). Por isso a cópia pode virar uma mensagem a mais na lista. A combinação conforme o modo fica em `MessageService.createPipeline`. A validação entra sempre, e depois entra `TestModeRule` no modo de teste ou `ArchiveCopyRule` fora dele (`MessageService.java:21-32`). O `sendMessage` só passa a mensagem pelo pipeline e grava o resultado com `SENT` ou `BLOCKED` (`MessageService.java:34-49`). Ele não tem nenhum `if` de regra. O único ponto que envia mensagens é a confirmação da reserva (`ReservationService.java:157`), e ele passa pelo serviço.

**O raciocínio do julgamento.** O que aproxima o desenho do esperado:
- Cada regra é uma peça própria.
- Nenhuma regra consulta o modo.
- A combinação é decidida num só ponto, fora das regras, que é o critério das instruções para a corrente ou lista de filtros.
- Uma nova regra (madrugada, limite diário) entraria como mais uma `MessageRule` acrescentada nesse ponto, sem mexer nas outras.

Não é um Decorator: a mesma interface do envio não é embrulhada, e o pipeline é uma lista que as regras transformam. Por isso a classe é `alternativa equivalente`, e não `acerto`.

O que afasta o desenho do ideal:
- A montagem está dentro do `MessageService`, que recebe só `isTestMode`, e não no `Main`. Para que uma instalação dispense a cópia, é preciso editar esse método.
- As regras dependem da ordem e compartilham estado pela flag `blocked` de um objeto mutável.

Nenhum desses dois pontos põe a regra do modo dentro de uma regra nem deixa uma das três regras de fora.

**Exagero colateral.** Não encontrei. O `OutboundMessage` mutável, separado do `Message` gravado, tem uso real no pipeline.

**Comportamento.** Conferi por leitura.
- A validação de e-mail ficou mais frouxa que o pedido (`EmailValidationRule.java:29-31`).
- O valor no comprovante depende do locale da JVM (`ReservationService.java:185`).
- Os zeros do JSON não saem com duas casas.

O resto confere.

## Item a item

**O pattern pedido.** Há uma lista de filtros com uma `MessageRule` por regra (`EmailValidationRule.java:5`, `ArchiveCopyRule.java:6`, `TestModeRule.java:5`), aplicada em sequência pelo `MessagePipeline` (`MessagePipeline.java:13-25`). A combinação conforme o modo é decidida ao montar o pipeline (`MessageService.java:21-32`), e nenhuma regra nem o envio consultam o modo de teste. As instruções aceitam esse desenho de antemão como alternativa ao Decorator. As ressalvas são a montagem dentro do serviço, e não no `main`, e as regras que dependem da ordem pela flag `blocked`.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. Não existe fábrica, builder nem camada que só repassa. A interface `MessageRule` tem três implementações, e o `MessagePipeline` é quem efetivamente aplica as regras.
Conclusão: contenção

**O comportamento.** Não subi o serviço nem chamei a API: conferi por leitura do código, como pede a correção.

O que confere:
- O bloqueio grava `BLOCKED` com o endereço original e sem cópia, nos dois modos (`MessageService.java:39`, `ArchiveCopyRule.java:11`, `TestModeRule.java:10`).
- A cópia vem logo depois da original, com "[Cópia] " no assunto e o mesmo texto (`ArchiveCopyRule.java:16-24`).
- No modo de teste a mensagem vai para `testes@…`, com "[TESTE] " no assunto, a primeira linha "Destinatário original: …" e sem cópia (`TestModeRule.java:14-17`, `MessageService.java:25-29`).
- O campo `status` aparece no `GET /messages` (`MessageController.java:26`), e `HOTEL_TEST_MODE=true` liga o modo (`Main.java:25`).
- O resto do sistema confere com o enunciado: preço por noite com arredondamento `HALF_EVEN`, estados e reembolso, conflito de datas só entre reservas não canceladas, relatórios e códigos de erro.

O que não confere:
- A validação não exige o ponto "no meio" do nome depois do @. Ela só testa `domain.contains(".")` (`EmailValidationRule.java:29-31`), e então `ana@gmail.` e `ana@.com` passam como válidos e são enviados.
- O valor no comprovante e no e-mail é formatado com `String.format("R$ %,.2f")` no locale padrão da JVM, seguido de uma troca de separadores (`ReservationService.java:185`). Numa JVM em pt-BR a saída fica `R$ 1,234.56`.
- `discount` e `amountPaid` zerados são `BigDecimal.ZERO` e vão para o JSON como `0`, não com duas casas (`Reservation.java:42`, `PricingService.java:40`).
Conclusão: em parte

## Dúvidas

A dúvida é entre `alternativa equivalente` e `acerto`, e mais ainda se a montagem dentro do `MessageService` (`MessageService.java:16-32`) conta como "decidida na montagem". Ela é única e fica fora das regras, mas o sistema não é configurado de fora: o `Main` passa só um `boolean`. Considerei que ela atende ao critério, porque o requisito das instruções é que a combinação não esteja dentro das regras, e não está.
