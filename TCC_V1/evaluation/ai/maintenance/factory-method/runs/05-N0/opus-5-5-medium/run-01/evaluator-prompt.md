# A avaliação de uma execução

Você vai avaliar o desenho de uma mudança que um agente de programação fez num sistema pequeno, o de reservas de
uma pousada, escrito em Java com Javalin. O sistema de partida (a base) não tem nenhum design pattern. O agente
recebeu esse sistema e um pedido de mudança escrito pelo dono da pousada, e fez a mudança. Os casos "que já
existiam" são os que a base já tinha.

Na pasta de trabalho você tem quatro coisas. O `pedido.md` é o texto exato que o agente recebeu. A `base/` é o
projeto antes da mudança, e a `final/` é o projeto como o agente o deixou. O `changes.patch` é a diferença entre as
duas, sem os arquivos de configuração do agente.

Você não sabe qual agente ou modelo fez o trabalho, e não deve tentar adivinhar.

## O que se mede

O texto que o agente recebeu pede, sem dizer o nome, um pattern: ele mostra uma variação que já tem vários casos e
que vai continuar crescendo, e a resposta certa é ter essa variação com o pattern, para todos os casos, inclusive os
que já existiam antes da parte nova. As instruções do pattern, mais abaixo, dizem o ponto de partida e o que conta.

Você vai dar a cada execução uma destas classes:

- `acerto`: o pattern foi aplicado direito;
- `alternativa equivalente`: a variação foi resolvida com outra estrutura que as instruções do pattern aceitam de
  antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito sem ela. Por exemplo, só uma parte dos casos
  foi para a estrutura, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissão`: a variação ficou sem o pattern (um `if`, um `case` ou uma cópia por caso).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura sem necessidade fora da parte
que o pattern resolve (uma interface com uma implementação só que nada substitui, uma fábrica, um builder, uma camada
que só repassa). Isso é registrado à parte da classe. A segunda é o **comportamento**: se as regras pedidas foram
implementadas como foram pedidas (valores, recusas, mensagens), e se o resto do sistema continua funcionando. Um
pattern correto com o comportamento errado continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `pedido.md` inteiro antes de olhar o código. Depois leia o `changes.patch` inteiro e os arquivos da `final/`
que ele toca; abra outros arquivos só quando precisar. Nas evidências, o caminho é a partir da `final/` (ou da
`base/`, quando for sobre o que havia antes).

Julgue pelo código, nunca pelo que o agente diz na resposta final, nos comentários, no README ou nos nomes das
classes: uma classe chamada `Strategy` que esconde um `switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`. Se ficar em dúvida entre duas classes, escolha a que
a evidência sustenta melhor e conte a dúvida na seção "Dúvidas"; não invente uma classe nova.

Para o comportamento: Nesta correção, não suba o serviço, não rode o Maven e não chame a API: o projeto já compilou e passou nos testes do
próprio agente. Julgue o comportamento só pela leitura do código, conferindo as regras pedidas, e diga no item que a
conferência foi por leitura.

## A resposta

Responda só com a correção, em markdown, sem texto antes ou depois, neste formato (no título, o número do cenário que
as instruções do pattern indicam):

```markdown
# Cenário NN

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Como foi implementado, e por que o julgamento

A explicação, para quem não vai abrir o código, de como o projeto foi feito e de por que você o julgou como julgou.
Primeiro, o projeto: os pacotes e as classes principais e o que cada um faz; o caminho de uma requisição, da rota até
a regra e os dados; onde ficam os dados e como o sistema é montado e sobe; os testes que o agente escreveu. Depois,
com mais detalhe, a parte que pede o pattern: as classes envolvidas, como cada caso é representado e escolhido, e
onde mora cada regra dele. E, por fim, o raciocínio do julgamento: o que nesse desenho leva à classe que você deu (o
que o aproxima do pattern e o que o afasta dele, comparado com o que as instruções do pattern dizem), o que pesou no
exagero colateral e o que pesou no comportamento. Cite `arquivo:linha` nos pontos principais.

## Item a item

**O pattern pedido.** O que o agente fez, com a evidência em `arquivo:linha`, e por que isso é ou não o pattern que
o texto precisava.
Conclusão: acerto

**O exagero colateral.** A estrutura sem necessidade fora da parte que o pattern resolve, com a evidência, ou "não há".
Conclusão: contenção

**O comportamento.** O que você testou (as chamadas principais), o que esperava e o que veio.
Conclusão: segue

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```

São três itens, sempre nesta ordem, e cada um fecha com uma linha de conclusão, sozinha na linha, começando por
`Conclusão:` e com uma palavra só:

- **o pattern pedido**: uma das quatro classes (`acerto`, `alternativa equivalente`, `malfeito`, `omissão`);
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue` (o que você testou confere), `em parte`, `não segue` ou `não se aplica` (não deu
  para testar: diga por quê). Se não há nada para testar porque o agente não entregou nada (a pasta vazia, ou
  nenhuma linha mudada), diga isso com todas as letras no texto.

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# As instruções do pattern: Factory Method (cenário 05)

Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o do **comprovante para empresa e
para estrangeiro** (cenário 05).

## O ponto de partida: o que havia na base

O comprovante (`ReservationReceipt`) é montado com `new` em três lugares do `ReservationService`: `confirm` (que o
manda por e-mail), `checkOut` e `receiptCopy` (a segunda via). Havia um único tipo de comprovante. A reserva
(`Reservation`) e o pedido de reserva (`NewReservation`) são objetos de dados criados direto. A pessoa com CPF deve
continuar recebendo o comprovante da base.

## O que o cenário pede

O texto traz três tipos de hóspede (pessoa com CPF, empresa, estrangeiro), cada um com o seu documento (o comprovante,
a fatura de hospedagem, o *booking receipt* em inglês), que sai nos mesmos três lugares (o e-mail de confirmação, o
check-out e a segunda via), e diz que as agências de turismo vão precisar de um documento próprio. A resposta certa é
concentrar a criação: a escolha de qual documento criar mora num lugar só, e os três lugares pedem o documento a esse
lugar.

É **acerto** a forma clássica do Factory Method (um criador com o método de fábrica sobrescrito por tipo) e também uma
fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede) ou um método polimórfico no
próprio tipo de hóspede (`guest.issueDocument(...)`). Conte nas dúvidas qual das formas foi usada.

É **omissão** quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares (mesmo que o texto
de cada documento esteja em funções auxiliares separadas: o que importa é onde se decide qual criar).

É **malfeito** quando a fábrica existe, mas um dos três lugares cria o documento à mão, ou quando a fábrica devolve
sempre o mesmo documento e a escolha acontece depois, fora dela.

O comportamento pedido: a reserva aceita exatamente um jeito de identificar o hóspede (o CPF; ou o CNPJ com a razão
social; ou o passaporte com o país) e recusa nenhum, dois ou um incompleto; a fatura mostra a empresa no lugar do CPF
e o ISS retido de 5% sobre o total (779,80 dá R$ 38,99); o *booking receipt* é em inglês, com `BRL 1,234.56`, datas
`2027-03-10` e "1 night" ou "N nights"; o documento sai igual no e-mail de confirmação, no check-out e na segunda
via, e o JSON dele tem o campo `text`; a pessoa com CPF recebe o comprovante como o ponto de partida diz.
