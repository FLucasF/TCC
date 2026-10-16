# As instruções do pattern: Decorator

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o das **regras para as
mensagens que a pousada manda** (cenário 07, que pede o pattern) e o da **assinatura nas mensagens** (cenário 08,
controle negativo).

## O que havia na base

Todo envio passa por `EmailSender.send`, que só guarda a mensagem na lista de mensagens enviadas
(`SentMessages`). Não havia nenhum comportamento extra no envio.

## Cenário 07: as regras de envio (pede o pattern)

O pedido traz, agora, três comportamentos novos em volta do mesmo envio (bloquear endereço inválido, mandar uma
cópia para o arquivo, desviar tudo para a caixa de testes no modo de teste), que se combinam de jeitos diferentes
conforme o modo (a cópia só fora do teste; o bloqueio sempre), e anuncia outras regras (não mandar de madrugada,
limite por dia) que podem valer sempre, só no teste ou só fora dele. A resposta certa é o Decorator: cada regra é
um embrulho do envio, com a mesma interface do envio, e a montagem do sistema (a classe `Hotel`) empilha os
embrulhos conforme o modo.

É **acerto** uma interface de envio com o envio real e um embrulho por regra, cada embrulho chamando o próximo,
e a combinação decidida na montagem.

É **alternativa equivalente** uma corrente de regras (Chain of Responsibility) ou uma lista de filtros aplicada
pelo envio, desde que cada regra seja uma peça própria e a combinação por modo seja decidida na montagem, não
dentro das regras.

É **omissão** quando os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do `send`, ou espalhados
por quem chama o envio.

É **malfeito** quando os embrulhos existem, mas o envio de dentro ainda pergunta pelo modo de teste; ou quando a
combinação por modo está escrita dentro de um dos embrulhos (por exemplo, a cópia que verifica se está em modo de
teste); ou quando uma das três regras ficou de fora da estrutura.

O comportamento pedido: um endereço é válido com um único @, algo antes dele e, depois, um nome com pelo menos um
ponto no meio; mensagem para endereço inválido não sai e aparece na lista como `BLOCKED` (sem cópia), também no
modo de teste, com o endereço original; toda mensagem enviada gera, logo depois dela, uma cópia para
`arquivo@pousadamarealta.com.br` com o assunto começando por "[Cópia] "; com `HOTEL_TEST_MODE=true`, tudo vai para
`testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …", sem a
cópia; cada mensagem tem o campo `status` (`SENT` ou `BLOCKED`).

## Cenário 08: a assinatura nas mensagens (controle negativo)

O pedido manda toda mensagem terminar com a assinatura da pousada. A frase "talvez a gente troque a assinatura no
Natal, mas por enquanto é essa o ano todo" é a isca.

É **contenção** acrescentar a assinatura num lugar só: no `EmailSender.send`, por onde passa toda mensagem, ou no
texto da confirmação, que hoje é a única mensagem.

É **exagero** criar um embrulho (Decorator) só para a assinatura, uma interface de envio para poder embrulhar, um
"provedor de assinatura" ou uma assinatura por época do ano.

O comportamento pedido: o corpo de toda mensagem termina com uma linha em branco, "--", "Pousada Maré Alta" e
"(79) 3200-0000 | @pousadamarealta", e a assinatura aparece uma vez só.
