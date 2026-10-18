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

# As instruções do pattern: Factory Method

Este avaliador cuida de dois enunciados, que você reconhece pela parte do fim do `enunciado.md`: o do **documento
por tipo de hóspede** (cenário 05, que pede o pattern) e o das **crianças na reserva** (cenário 06, controle
negativo).

## O que o enunciado descreve

Na parte comum, o comprovante sai em três lugares: no e-mail de confirmação, no check-out e na segunda via. A
reserva e o pedido de reserva são objetos de dados.

## Cenário 05: o documento por tipo de hóspede (pede o pattern)

O enunciado traz três tipos de hóspede (pessoa com CPF, empresa, estrangeiro), cada um com o seu documento (o
comprovante, a fatura de hospedagem, o *booking receipt* em inglês), que sai nos mesmos três lugares, e diz que as
agências de turismo vão precisar de um documento próprio. A resposta certa é concentrar a criação: a escolha de qual
documento criar mora num lugar só, e os três lugares pedem o documento a esse lugar.

É **acerto** a forma clássica do Factory Method (um criador com o método de fábrica sobrescrito por tipo) e também
uma fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede) ou um método
polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`). Conte nas dúvidas qual das formas foi usada.

É **omissão** quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares (mesmo que o
texto de cada documento esteja em funções auxiliares separadas: o que importa é onde se decide qual criar).

É **malfeito** quando a fábrica existe, mas um dos três lugares cria o documento à mão, ou quando a fábrica devolve
sempre o mesmo documento e a escolha acontece depois, fora dela.

O comportamento pedido: a reserva aceita exatamente um jeito de identificar o hóspede (o CPF; ou o CNPJ com a razão
social; ou o passaporte com o país) e recusa nenhum, dois ou um incompleto; a fatura mostra a empresa no lugar do
CPF e o ISS retido de 5% sobre o total (779,80 dá R$ 38,99); o *booking receipt* é em inglês, com `BRL 1,234.56`,
datas `2027-03-10` e "1 night" ou "N nights"; o documento sai igual no e-mail de confirmação, no check-out e na
segunda via, e o JSON dele tem o campo `text`; a pessoa com CPF recebe o comprovante da parte comum.

## Cenário 06: as crianças na reserva (controle negativo)

O enunciado acrescenta o número de crianças à reserva. A frase "a gente já pensou em cobrar alguma coisa das
crianças maiores no futuro, mas por enquanto criança não paga" é a isca.

É **contenção** um campo a mais no pedido de reserva e na reserva, a soma na conferência da capacidade e a linha no
comprovante; a reserva é criada direto.

É **exagero** criar um *builder* ou uma fábrica para a reserva ou para o pedido de reserva, ou uma "política de
preço de criança" (uma interface com uma implementação que cobra zero), sem que ninguém tenha pedido preço.

O comportamento pedido: o campo `children` é opcional (zero quando não vem) e não pode ser negativo; adultos e
crianças juntos não passam da capacidade do quarto; criança não muda o preço; o comprovante tem "Crianças: N" logo
depois da linha do quarto, só quando há crianças.
