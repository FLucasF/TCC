# A avaliação de uma execução

Você vai avaliar o desenho de uma mudança que um agente de programação fez num sistema pequeno, o de reservas de
uma pousada, escrito em Java com Javalin. O sistema de partida não tem nenhum design pattern. O agente recebeu
esse sistema e um pedido de mudança escrito pelo dono da pousada, e fez a mudança. Você não sabe qual agente ou
modelo fez o trabalho, e não deve tentar adivinhar.

Na pasta de trabalho você tem quatro coisas. O `pedido.md` é o texto exato que o agente recebeu. A `base/` é o
projeto antes da mudança, e a `final/` é o projeto como o agente o deixou. O `changes.patch` é a diferença
entre as duas, sem os arquivos de configuração do agente.

## O que se mede

Os pedidos são de dois tipos. Uns pedem, sem dizer o nome, um pattern: o texto mostra uma variação que já existe
e que vai continuar crescendo, e a resposta certa é reorganizar o código com o pattern, inclusive nos casos que
já existiam. Os outros são controles negativos: parecem pedir uma estrutura, mas são uma mudança pontual, e a
resposta certa é a mudança simples. Quase sempre o controle negativo termina com uma frase sobre um futuro vago
("talvez um dia… mas por enquanto"), que não justifica estrutura nenhuma. As instruções do pattern, mais abaixo,
dizem qual pedido é de qual tipo e o que conta em cada caso.

Você vai dar a cada execução uma destas classes:

- `acerto`: o pedido precisava do pattern, e ele foi aplicado direito;
- `alternativa equivalente`: o pedido precisava do pattern, e a variação foi resolvida com outra estrutura que as
  instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito antigo. Por exemplo, só os casos novos foram
  para a estrutura nova, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissão`: o pedido precisava do pattern, e a mudança foi feita sem ele (mais um `if`, mais um `case`, mais
  uma cópia);
- `contenção`: o pedido era um controle negativo, e a mudança foi a simples;
- `exagero`: o pedido era um controle negativo, e foi criada estrutura que ninguém pediu (uma interface, uma
  classe por caso, uma fábrica, um embrulho, um esqueleto genérico).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura criada numa parte do
código que o pedido não tocava (uma interface com uma implementação só, uma fábrica, um builder, uma camada
nova). Isso vale em qualquer pedido e é registrado à parte da classe. A segunda é o **comportamento**: se as
regras do pedido parecem implementadas como foram pedidas (valores, recusas, mensagens). Um pattern correto com o
comportamento errado continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `pedido.md` inteiro antes de olhar o código. Depois leia o `changes.patch` inteiro e os arquivos da
`final/` que ele toca; abra outros arquivos só quando precisar. Julgue pelo código, nunca pelo que o agente diz
na resposta final, nos comentários ou nos nomes das classes: uma classe chamada `Strategy` que esconde um
`switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`, com o caminho a partir da `final/` (ou da
`base/`, quando for sobre o que havia antes). Se ficar em dúvida entre duas classes, escolha a que a evidência
sustenta melhor e conte a dúvida na seção "Dúvidas"; não invente uma classe nova.

Para o comportamento, não confie só na leitura: teste. Faça uma cópia da `final/` (nunca mude a `final/` nem a
`base/`), rode `mvn -q package -DskipTests` nela e suba o serviço com `PORT=7070 java -jar target/hotel.jar &`.
Depois chame a API com `curl` (e o `jq`, se ajudar), do jeito que o site chamaria, e confira as regras do pedido:
os valores, as recusas, as mensagens, os relatórios, o que o pedido disser. Confira também uma ou duas coisas que
o pedido não mudou, para ver se continuam iguais. O README da `base/` mostra a API. Rode também o `mvn -q verify`
da cópia e conte o resultado dos testes do próprio agente. No fim, derrube o serviço.

Use `certo` quando as regras que você testou conferem com o pedido, `errado` quando você achou uma que não confere
(diga a chamada que fez, o que esperava e o que veio) e `nao_verificado` quando não deu para testar (por exemplo,
o serviço não subiu: diga por quê). Nas `observacoes`, conte o que testou.

## A resposta

Responda só com a correção, em markdown, sem texto antes ou depois, neste formato (o mesmo da correção da
construção):

```markdown
# Cenário 01

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Item a item

**O pedido (pede o pattern).** O que o agente fez, com a evidência em `arquivo:linha`, e por que isso é ou não
o pattern que o pedido precisava.
Conclusão: acerto

**O exagero colateral.** A estrutura criada fora do que o pedido tocava, com a evidência, ou "não há".
Conclusão: contenção

**O comportamento.** O que você testou (as chamadas principais), o que esperava e o que veio.
Conclusão: segue

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```

São três itens, sempre nesta ordem, e cada um fecha com uma linha de conclusão, sozinha na linha, começando por
`Conclusão:` e com uma palavra só:

- **o pedido**: uma das seis classes (`acerto`, `alternativa equivalente`, `malfeito`, `omissão`, `contenção`,
  `exagero`). No título do item, diga se o pedido pede o pattern ou é um controle negativo;
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue` (o que você testou confere), `em parte`, `não segue` ou `não se aplica` (não deu
  para testar: diga por quê).

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

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
cópia; cada mensagem tem o campo `status` (`SENT` ou `BLOCKED`). Para testar os dois modos, suba o serviço duas
vezes: uma sem a variável e outra com `HOTEL_TEST_MODE=true PORT=7071 java -jar target/hotel.jar &`.

## Cenário 08: a assinatura nas mensagens (controle negativo)

O pedido manda toda mensagem terminar com a assinatura da pousada. A frase "talvez a gente troque a assinatura no
Natal, mas por enquanto é essa o ano todo" é a isca.

É **contenção** acrescentar a assinatura num lugar só: no `EmailSender.send`, por onde passa toda mensagem, ou no
texto da confirmação, que hoje é a única mensagem.

É **exagero** criar um embrulho (Decorator) só para a assinatura, uma interface de envio para poder embrulhar, um
"provedor de assinatura" ou uma assinatura por época do ano.

O comportamento pedido: o corpo de toda mensagem termina com uma linha em branco, "--", "Pousada Maré Alta" e
"(79) 3200-0000 | @pousadamarealta", e a assinatura aparece uma vez só.
