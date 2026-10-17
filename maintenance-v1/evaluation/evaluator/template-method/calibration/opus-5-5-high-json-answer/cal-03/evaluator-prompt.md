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
- `alternativa_equivalente`: o pedido precisava do pattern, e a variação foi resolvida com outra estrutura que as
  instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito antigo. Por exemplo, só os casos novos foram
  para a estrutura nova, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissao`: o pedido precisava do pattern, e a mudança foi feita sem ele (mais um `if`, mais um `case`, mais
  uma cópia);
- `contencao`: o pedido era um controle negativo, e a mudança foi a simples;
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
sustenta melhor e conte a dúvida no campo `duvidas`; não invente uma classe nova.

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

Responda só com um objeto JSON, sem texto antes ou depois:

```json
{
  "cenario": "01",
  "tipo": "pede_pattern",
  "classe": "acerto",
  "resumo": "duas ou três frases dizendo o que o agente fez com o desenho",
  "evidencias": [
    {"arquivo": "src/main/java/hotel/reservations/RatePlan.java", "linha": 18, "o_que_mostra": "..."}
  ],
  "exagero_colateral": {"houve": false, "evidencias": []},
  "comportamento": {"situacao": "certo", "observacoes": "..."},
  "duvidas": ""
}
```

O `tipo` é `pede_pattern` ou `controle_negativo`, e a `classe` é uma das seis acima.

---

# As instruções do pattern: Template Method

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o dos **relatórios novos,
todos com título** (cenário 09, que pede o pattern) e o do **nome do hóspede no relatório de ocupação** (cenário
10, controle negativo).

## O que havia na base

Dois relatórios em CSV, `OccupancyReport` e `RevenueReport`, escritos cada um inteiro, com os mesmos passos:
escolher as reservas, ordenar, escrever o cabeçalho, uma linha por reserva e a linha do total. Com dois
relatórios, a repetição era aceitável.

## Cenário 09: os relatórios novos (pede o pattern)

O pedido traz dois relatórios novos (a taxa de turismo e o relatório por plano de tarifa), o que faz quatro
relatórios com os mesmos passos, e uma mudança que vale para todos (a linha de título no começo), e diz que o
contador vai pedir mais. A resposta certa é o Template Method: o esqueleto (título, cabeçalho, linhas, total)
num lugar só, e cada relatório preenchendo só os passos que mudam.

É **acerto** uma classe abstrata com o método-modelo (o esqueleto, de preferência `final`) e os passos
abstratos, com os quatro relatórios como subclasses, os dois antigos inclusive.

É **alternativa equivalente** a composição: um escritor de relatório genérico que recebe as partes que variam (o
nome, as colunas, a seleção, a linha, o total) como objetos ou funções, desde que os quatro relatórios passem por
ele.

É **omissão** quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título copiada.

É **malfeito** quando o esqueleto existe só para os dois novos e os dois antigos continuam inteiros (com a linha
de título copiada neles), ou quando o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a
sequência dos passos.

O comportamento pedido: os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>" (os nomes são
`occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada); a taxa de turismo cobra
R$ 4,50 por hóspede por noite das reservas pagas que começam no mês, na ordem do relatório financeiro, com as
colunas `reservation,room,check_in,check_out,guests,nights,tax` e o total no fim; o relatório por plano tem uma
linha por plano (`STANDARD` e depois `NON_REFUNDABLE`, mesmo sem reserva), com a quantidade e o dinheiro que ficou
com a pousada, contando as mesmas reservas do relatório financeiro (inclusive uma não reembolsável cancelada), e
o total no fim; depois do título, os dois relatórios antigos continuam iguais.

## Cenário 10: o nome do hóspede na ocupação (controle negativo)

O pedido acrescenta a coluna do nome do hóspede ao relatório de ocupação. A frase "talvez um dia a recepção peça o
telefone do hóspede também" é a isca.

É **contenção** uma coluna a mais no `OccupancyReport`, com a troca da vírgula por espaço no nome.

É **exagero** criar um esqueleto genérico de relatórios, um sistema de colunas configuráveis (uma lista de
objetos-coluna), ou mexer no relatório financeiro, que o pedido não toca.

O comportamento pedido: a coluna `guest` logo depois de `reservation`, com o nome como está na reserva e a
vírgula trocada por um espaço; o resto do relatório de ocupação e o relatório financeiro iguais.
