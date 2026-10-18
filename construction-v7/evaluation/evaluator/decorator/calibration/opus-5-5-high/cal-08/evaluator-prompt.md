# A avaliação de uma execução

Você vai avaliar o desenho de um sistema pequeno, o de reservas de uma pousada, escrito em Java com Javalin, que um
agente de programação construiu do zero. O agente recebeu uma pasta vazia e um enunciado escrito pelo dono da
pousada: o enunciado descreve o sistema inteiro e, no fim, traz uma parte que precisa estar nele desde o primeiro
dia. Você não sabe qual agente ou modelo fez o trabalho, e não deve tentar adivinhar.

Na pasta de trabalho você tem duas coisas. O `enunciado.md` é o texto exato que o agente recebeu. O `projeto/` é o
projeto como o agente o deixou.

## O que se mede

A parte do fim do enunciado é de dois tipos. Umas pedem, sem dizer o nome, um pattern: o texto mostra uma variação
que já começa com vários casos e que vai continuar crescendo, e a resposta certa é construir essa parte com o
pattern, para todos os casos. As outras são controles negativos: parecem pedir uma estrutura, mas são uma regra
simples, e a resposta certa é a solução simples. Quase sempre o controle negativo termina com uma frase sobre um
futuro vago ("talvez um dia… mas por enquanto"), que não justifica estrutura nenhuma. As instruções do pattern, mais
abaixo, dizem qual enunciado é de qual tipo e o que conta em cada caso.

Você vai dar a cada execução uma destas classes:

- `acerto`: o enunciado precisava do pattern, e ele foi aplicado direito;
- `alternativa equivalente`: o enunciado precisava do pattern, e a variação foi resolvida com outra estrutura que
  as instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito sem ela. Por exemplo, só uma parte dos casos
  foi para a estrutura, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissão`: o enunciado precisava do pattern, e a parte foi construída sem ele (um `if`, um `case` ou uma cópia
  por caso);
- `contenção`: o enunciado era um controle negativo, e a solução foi a simples;
- `exagero`: o enunciado era um controle negativo, e foi criada estrutura que ninguém pediu (uma interface, uma
  classe por caso, uma fábrica, um embrulho, um esqueleto genérico).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura sem necessidade numa parte do
sistema fora da que o fim do enunciado traz (uma interface com uma implementação só que nada substitui, uma fábrica,
um builder, uma camada que só repassa). Isso vale em qualquer enunciado e é registrado à parte da classe. A segunda é
o **comportamento**: se as regras da parte do fim do enunciado foram implementadas como foram pedidas (valores,
recusas, mensagens), e se o resto do sistema descrito funciona. Um pattern correto com o comportamento errado
continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `enunciado.md` inteiro antes de olhar o código, com atenção à parte do fim. Depois leia o código do
`projeto/` que trata dessa parte, e o resto quando precisar. Julgue pelo código, nunca pelo que o agente diz nos
comentários, no README ou nos nomes das classes: uma classe chamada `Strategy` que esconde um `switch` não é um
Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`, com o caminho a partir do `projeto/`. Se ficar em
dúvida entre duas classes, escolha a que a evidência sustenta melhor e conte a dúvida na seção "Dúvidas"; não
invente uma classe nova.

Para o comportamento, não confie só na leitura: teste. Faça uma cópia do `projeto/` (nunca mude o `projeto/`), rode
`mvn -q package -DskipTests` nela e suba o serviço com `PORT=7070 java -jar <o jar que o build gerou> &` (o que tem
a classe principal no manifesto). Depois chame a API com `curl` (e o `jq`, se ajudar), do jeito que o enunciado
diz que o site chama, e confira as regras da parte do fim: os valores, as recusas, as mensagens, os relatórios, o
que ela disser. Confira também uma ou duas coisas da parte comum do enunciado (um preço, uma recusa, o comprovante).
Rode também o `mvn -q verify` da cópia e conte o resultado dos testes do próprio agente. No fim, derrube o serviço.

## A resposta

Responda só com a correção, em markdown, sem texto antes ou depois, neste formato (o mesmo da manutenção):

```markdown
# Cenário 01

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Item a item

**O enunciado (pede o pattern).** O que o agente fez, com a evidência em `arquivo:linha`, e por que isso é ou não
o pattern que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** A estrutura sem necessidade fora da parte do fim do enunciado, com a evidência, ou "não há".
Conclusão: contenção

**O comportamento.** O que você testou (as chamadas principais), o que esperava e o que veio.
Conclusão: segue

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```

São três itens, sempre nesta ordem, e cada um fecha com uma linha de conclusão, sozinha na linha, começando por
`Conclusão:` e com uma palavra só:

- **o enunciado**: uma das seis classes (`acerto`, `alternativa equivalente`, `malfeito`, `omissão`, `contenção`,
  `exagero`). No título do item, diga se o enunciado pede o pattern ou é um controle negativo;
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue` (o que você testou confere), `em parte`, `não segue` ou `não se aplica` (não deu
  para testar: diga por quê).

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# As instruções do pattern: Decorator

Este avaliador cuida de dois enunciados, que você reconhece pela parte do fim do `enunciado.md`: o das **regras
para as mensagens que a pousada manda** (cenário 07, que pede o pattern) e o da **assinatura nas mensagens**
(cenário 08, controle negativo).

## O que o enunciado descreve

Na parte comum, os e-mails não saem de verdade: ficam numa lista de mensagens enviadas, que a recepção consulta.

## Cenário 07: as regras de envio (pede o pattern)

O enunciado traz três comportamentos em volta do mesmo envio (bloquear endereço inválido, mandar uma cópia para o
arquivo, desviar tudo para a caixa de testes no modo de teste), que se combinam de jeitos diferentes conforme o modo
(a cópia só fora do teste; o bloqueio sempre), e anuncia outras regras (não mandar de madrugada, limite por dia) que
podem valer sempre, só no teste ou só fora dele. A resposta certa é o Decorator: cada regra é um embrulho do envio,
com a mesma interface do envio, e a montagem do sistema (o `main` ou a classe que monta as peças) empilha os
embrulhos conforme o modo.

É **acerto** uma interface de envio com o envio real e um embrulho por regra, cada embrulho chamando o próximo, e a
combinação decidida na montagem.

É **alternativa equivalente** uma corrente de regras (Chain of Responsibility) ou uma lista de filtros aplicada pelo
envio, desde que cada regra seja uma peça própria e a combinação por modo seja decidida na montagem, não dentro das
regras.

É **omissão** quando os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio, ou espalhados por
quem chama o envio.

É **malfeito** quando os embrulhos existem, mas o envio de dentro ainda pergunta pelo modo de teste; ou quando a
combinação por modo está escrita dentro de um dos embrulhos (por exemplo, a cópia que verifica se está em modo de
teste); ou quando uma das três regras ficou de fora da estrutura.

O comportamento pedido: um endereço é válido com um único @, algo antes dele e, depois, um nome com pelo menos um
ponto no meio; mensagem para endereço inválido não sai e aparece na lista como `BLOCKED` (sem cópia), também no
modo de teste, com o endereço original; toda mensagem enviada gera, logo depois dela, uma cópia para
`arquivo@pousadamarealta.com.br` com o assunto começando por "[Cópia] "; com `HOTEL_TEST_MODE=true`, tudo vai para
`testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …", sem a
cópia; cada mensagem tem o campo `status` (`SENT` ou `BLOCKED`). Para testar os dois modos, suba o serviço duas
vezes: uma sem a variável e outra com `HOTEL_TEST_MODE=true PORT=7071 java -jar <o jar> &`.

## Cenário 08: a assinatura nas mensagens (controle negativo)

O enunciado manda toda mensagem terminar com a assinatura da pousada. A frase "talvez a gente troque a assinatura no
Natal, mas por enquanto é essa o ano todo" é a isca.

É **contenção** a assinatura num lugar só: no envio, por onde passa toda mensagem, ou no texto da confirmação, que é
a única mensagem.

É **exagero** criar um embrulho (Decorator) só para a assinatura, uma interface de envio para poder embrulhar, um
"provedor de assinatura" ou uma assinatura por época do ano.

O comportamento pedido: o corpo de toda mensagem termina com uma linha em branco, "--", "Pousada Maré Alta" e
"(79) 3200-0000 | @pousadamarealta", e a assinatura aparece uma vez só.
