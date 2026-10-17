# A dimensão: a implementação dos padrões (a parte comum)

Os padrões são corrigidos **um por conversa**, como as outras dimensões: este arquivo e depois o do pattern
(`strategy.md`, `adapter.md`, `state.md` ou `factory.md`). Não há material além do enunciado e do projeto.

O enunciado foi escrito para que alguns pontos peçam um padrão sem dizer o nome dele, e para que outros pareçam
pedir e não peçam (os controles negativos). Cada pattern tem três tipos de ponto:

- **o óbvio**: o requisito pede o padrão (o enunciado mostra uma variação entre casos, quase sempre dizendo que ela
  vai crescer);
- **o remendo**: uma regra cruzada que tenta a escrever um `if` no lugar errado, por cima da estrutura;
- **o controle negativo**: parece pedir o padrão, mas a resposta do tamanho do problema é simples (um dado, um
  campo, um valor). Criar estrutura ali é exagero.

As mesmas palavras da correção da manutenção servem para nomear o que você vê, dentro do texto, lembrando que aqui
o agente escreveu tudo do zero (não há código anterior nem pedido de mudança): **acerto** (o padrão onde o
enunciado pede, aplicado direito), **omissão** (onde pede, a variação escrita sem ele: `if` ou `switch` sobre os
casos no cálculo, ou o mesmo código copiado para cada caso), **malfeito** (a estrutura existe, mas não cobre tudo:
algum caso ou alguma regra de um caso ficou fora dela, num `if`, ou quem usa a estrutura ainda pergunta qual é o
caso), **contenção** (no controle negativo, a resposta simples) e **exagero** (no controle negativo, estrutura que
ninguém pediu). A régua não pergunta "o padrão foi usado?": um nome de classe não conta, o que conta é onde mora o
código de cada caso e como o caso é escolhido.

Duas situações são próprias de um projeto criado do zero:

- **um caso que não existe no código** (um cupom que o agente não implementou, uma situação do pedido que falta): diga
  isso e julgue o desenho com os casos que existem. Se o serviço faz o que o enunciado pede, é a dimensão do
  comportamento que mede, e não esta correção;
- **um ponto que o enunciado daquela execução não tinha**: os pilotos rodaram com versões anteriores do enunciado
  (no `prompt.md` da execução), e alguma regra ou alguma frase pode não estar lá. Confira no `prompt.md` e, se o ponto
  não estiver, diga que ele não se aplica àquela execução, em vez de cobrar o que o agente não recebeu.

## As definições

**Os casos de cada ponto**, com os códigos do enunciado:

| ponto | os casos |
|---|---|
| P1, a entrega | `ECONOMY`, `EXPRESS`, `STORE_PICKUP`, `COURIER` |
| P2, o cupom | `WELCOME10`, `MINUS50`, `FREESHIPPING`, `BUY3PAY2` |
| P3, a forma de pagamento | `PIX`, `CREDIT_CARD`, `BANK_SLIP` |
| P4, o nível do clube | `BRONZE`, `SILVER`, `GOLD` |
| P5, a região | `SOUTHEAST`, `SOUTH`, `MIDWEST`, `NORTH`, `NORTHEAST` |
| P7, a situação do pedido | `AWAITING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

Os pilotos rodaram com versões anteriores do enunciado, algumas com os códigos em português (`OURO`, `FRETEGRATIS`,
`RETIRADA`): os nomes traduzidos valem como os mesmos casos.

**Nomear um caso** é escrever qual ele é: o texto `"GOLD"`, a constante `GOLD`, a classe `GoldTier`,
`instanceof GoldTier`, `case GOLD ->`, o nome traduzido (`Nivel.OURO`) e a comparação pela ordem
(`tier.ordinal() >= 2`, `tier.compareTo(SILVER) > 0`). O teste: *se eu criar um caso parecido, esta linha
precisa mudar?* Se precisa, ela nomeia. `tier.freeShipping()` não nomeia: pergunta a qualquer nível, e um nível
novo responde sozinho.

**A casa de um caso** é a unidade que é só dele: uma classe, uma constante de enum com corpo próprio, uma entrada
de mapa com uma função própria. **Uma classe aninhada é casa** (uma `class FreeShipping` dentro de `Coupons.java`
é a casa do FREESHIPPING).

**Não conta como código de um caso fora da casa dele:**
- o nome usado só como chave de registro, ou numa lista de válidos (um bloco que cita todos os casos só para
  conferir se o código existe ou para traduzir o texto para a constante);
- uma constante de um caso declarada fora dele;
- uma função genérica que não cita o caso, mesmo que só um caso a use (um método `default`, uma classe-base).

## E: estrutura sem necessidade fora dos pontos

Em qualquer pattern, registre à parte a estrutura que o projeto criou numa parte do código que **nenhum ponto do
enunciado pede**: uma interface com uma implementação só que nada substitui, uma camada que só repassa, um builder
ou uma fábrica que ninguém precisava. Não conte aqui os controles negativos do pattern, que têm item próprio. Cite
cada caso, ou diga que não há. Como os quatro patterns olham o mesmo projeto, cada correção registra só o que achou
perto do seu pattern. A conclusão do E é `contenção` (não há) ou `exagero` (há).
