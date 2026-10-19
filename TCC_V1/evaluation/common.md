# A correção de uma execução

Você vai corrigir o desenho de um sistema pequeno, o de reservas de uma pousada, escrito em Java com Javalin, que um
agente de programação construiu do zero (na construção) ou mudou a partir de uma base sem nenhum pattern (na
manutenção), a partir de um texto escrito pelo dono da pousada. Você não sabe qual agente ou modelo fez o trabalho, e
não deve tentar adivinhar.

## O que olhar

O texto que o agente recebeu pede, sem dizer o nome, um pattern: ele mostra uma variação que já tem vários casos e
que vai continuar crescendo. O que interessa é como o agente resolveu essa variação: se usou o pattern, se usou outra
estrutura que resolve igual, se a estrutura existe mas convive com o jeito sem ela (só parte dos casos nela, quem usa
a estrutura ainda perguntando qual é o caso, um lugar que continua fazendo à mão), ou se deixou a variação num `if`,
num `case` ou numa cópia por caso.

Olhe também duas coisas fora disso: estrutura sem necessidade no resto do sistema (uma interface com uma
implementação só que nada substitui, uma fábrica, um builder, uma camada que só repassa) e o comportamento (se as
regras pedidas foram implementadas como foram pedidas, e se o resto do sistema funciona). Um desenho certo com o
comportamento errado continua um desenho certo, mas o problema tem de aparecer na correção.

## Como corrigir

Leia o texto que o agente recebeu inteiro antes de olhar o código; depois leia o código que trata da parte que pede
o pattern, e o resto quando precisar. Julgue pelo código, nunca pelo que o agente diz na resposta final, nos
comentários, no README ou nos nomes das classes: uma classe chamada `Strategy` que esconde um `switch` não é um
Strategy. Aponte a evidência no formato `arquivo:linha`.

Para o comportamento, não confie só na leitura: teste. Suba o serviço (`PORT=7070 java -jar <o jar> &`), chame a API
com `curl` do jeito que o texto diz que o site chama, e confira as regras pedidas e uma ou duas coisas do resto do
sistema. Rode também o `mvn -q verify` e conte o resultado dos testes do próprio agente. No fim, derrube o serviço.
Se o agente não entregou nada (a pasta vazia, ou nenhuma linha mudada), diga isso com todas as letras.

## A resposta

Responda em markdown, neste formato:

```markdown
# Cenário NN

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Como foi implementado

Para quem não vai abrir o código: os pacotes e as classes principais e o que cada um faz; o caminho de uma
requisição, da rota até a regra e os dados; os testes que o agente escreveu. Depois, com mais detalhe, a parte que
pede o pattern: as classes envolvidas, como cada caso é representado e escolhido, e onde mora cada regra dele.

## A correção

O desenho da parte que pede o pattern: o que o aproxima do pattern, o que o afasta dele, e por quê, com
`arquivo:linha`. A estrutura sem necessidade no resto do sistema, se houver. O comportamento: o que você testou, o
que esperava e o que veio.

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```
