# O enunciado

| arquivo | hash | pontos de variação | execuções |
|---|---|---|---|
| `prompt.md` | `d798c11402a52f8c` | P1 entrega · P2 cupons · P3 pagamento · **P4 clube** · **P5 seguro** | o V4, a rodar |
| `../../history/state/state.md` | `4591f7425e1551fd` | E1 ações por situação · E2 efeitos do cancelamento e da devolução · **E3 texto** (controle negativo) | nenhuma: saiu do V4 em 07/10 |
| `../../history/prompt-v3/prompt.md` | `b7cdb594cb49efee` | os mesmos do `prompt.md` | `EXT-01` a `03`, `TESTE-STRATEGY-*` e `TESTE-P4-*`, na bancada |
| `../../history/prompt-v3/state.md` | `ebffe1724ca316b5` | os mesmos do `state.md` | `TESTE-STATE-*`, na bancada |
| `../../history/pilot/prompt.md` | `53db3424b3972795` | P1 entrega · P2 cupons · P3 pagamento | `BATCH-01` a `03` — o piloto (e os `SMOKE`) |

O `state.md` foi o segundo padrão (State): a situação de um pedido depois da
compra, que muda a cada ação. Ele provou que a bancada aceita um segundo padrão e
**saiu do V4 em 07/10**, que ficou só com o Strategy. O enunciado, a suíte e o
gabarito estão em `history/state/`, com o que foi provado e como trazer de volta.

Um enunciado que já rodou não muda: a versão nova é um arquivo novo, e a que
rodou vai para `history/` com os mesmos bytes, para o hash conferir com o
`meta.json` de cada execução. O de cinco pontos se chamava `prompt-estendido.md`
até virar o enunciado único; o de três era o `prompt.md` e foi para
`history/pilot/`. Os dois enunciados que rodaram na bancada até 06/10 estão em
`history/prompt-v3/`.

## O que mudou para o V4 (06 e 07/10/2026)

**O cliente entende o básico, nos dois enunciados (06/10).** Até 06/10, o cliente dizia
só "não sou programador", e o contrato da API vinha num anexo "combinado com o
desenvolvedor do site", seguido de "observações do time técnico". Um cliente
leigo dificilmente entregaria esse contrato, e a história ficava com três
autores. Agora o cliente **entende o básico** e diz que montou a parte técnica
pesquisando: o anexo é dele, e as observações viraram "o que pesquisei da parte
técnica". Mudaram três trechos: a frase de abertura, o título e a primeira frase
do anexo, e o título das observações. O contrato fica no enunciado porque é ele
que permite medir a correção com a mesma suíte de caixa-preta em todas as
execuções. No `state.md`, isso é tudo o que mudou (e ele saiu do V4 no dia seguinte).

**O imposto por região virou seguro por região (07/10).** O imposto somado no
checkout não existe no Brasil, onde o preço já traz os tributos; era o *sales
tax* americano. O P5 precisava da mesma **forma** (cinco regiões, só a
porcentagem muda, a mesma conta), não do tema, e o seguro contra extravio e roubo
cobrado por região mantém essa forma: a mesma que, no imposto, já pegou um
exagero na bancada (o Haiku HARNESS escreveu uma interface, cinco classes e uma
fábrica só para trocar a porcentagem). Uma embalagem para presente com preço fixo
por tamanho foi considerada e descartada: parece tabela de preço, não
comportamento, e o controle negativo arriscava ficar no piso, sem ninguém
exagerando. Alíquotas: Sudeste 1%, Sul 1%, Centro-Oeste 1,5%, Norte 2,5% e
Nordeste 2%, sobre os produtos sem desconto e sem frete. No contrato, o campo
`imposto` virou `seguro`.

**As três inconsistências do Strategy foram corrigidas** (estavam no §6 do
`OBJETIVO.md`):

| | na bancada | no V4 |
|---|---|---|
| exemplos 1 a 4 | sem clube nem região, totais sem imposto (vinham do piloto) | com clube, região e seguro nos totais; os cinco exemplos passam por todas as regiões e todos os níveis |
| resposta de exemplo do anexo | números do piloto misturados com um imposto sem desconto | a resposta da requisição ao lado (OURO, Sudeste, BEMVINDO10, Pix) |
| limite do boleto | "total do pedido (produtos − cupom + frete)", com o total definido com imposto no passo 5 | "o total do pedido", que inclui o seguro, sem parêntese |

A terceira se resolveu com a troca: o total do pedido tem uma definição só (passo
5), e o boleto usa essa. No exemplo 2, a região é Centro-Oeste de propósito, para
a parcela ficar longe do empate do arredondamento; senão o exemplo testaria
precisão numérica em vez da regra.

Os números novos **saíram da calculadora de referência**. O
`conferir-enunciado.mjs` confere os 66 números escritos no texto contra ela. A
calculadora em si é verificada como descrito no
[README da suíte](../../evaluation/acceptance-prototype/README.md). Fora o P5 e a
frase do boleto, as regras de negócio, o contrato e os erros não mudaram, e a
régua de desenho vale igual: o P5 continua sendo o controle negativo, com cinco
regiões.

---

## Por que o piloto não bastou

O piloto, com o enunciado de três pontos, mostrou **efeito de teto**: Opus 5 e Sonnet 5 deram 3 de 3
pontos extensíveis em **todas** as 12 execuções, nos dois braços. Onde o controle
já acerta tudo, não há espaço para o harness melhorar — e nenhum tamanho de
amostra resolve isso.

A razão é que os três pontos são **simétricos**: cada variante devolve um valor do
mesmo tipo. Frete devolve custo e prazo, cupom devolve desconto, pagamento devolve
ajuste. Uma interface de um método serve aos três, e por isso o modelo a encontra
sozinho.

Os dois pontos novos quebram essa simetria de formas diferentes, e cada um testa
uma regra específica do harness.

## P4 — clube da loja. Testa a **terceira** regra

> Antes de escrever a primeira implementação, decida a assinatura olhando todos os
> casos. Ela precisa atender o caso mais exigente, não o primeiro.

Nenhum ponto do enunciado original testava isso, porque em todos o primeiro caso
já servia.

| nível | o que afeta |
|---|---|
| BRONZE | nada |
| PRATA | **uma** saída: o crédito |
| OURO | **três** saídas: o crédito, o frete, e o brinde |

Quem olhar BRONZE e PRATA primeiro escreve algo como "devolve o crédito" — e o
OURO não cabe, porque ele zera o frete, que é calculado noutro lugar. O remendo
natural é um `if` sobre o nível dentro do cálculo do frete.

O frete grátis do OURO também **colide com o cupom FRETEGRATIS**, o que força
decidir a ordem em vez de copiar.

## P5 — seguro por região (era imposto até 06/10). **Controle negativo**, testa a **quarta** regra

> trate assim apenas o que o enunciado descreve como variando. Não crie estrutura
> para variação que você imagina que possa vir a existir.

Aqui Strategy é a resposta **errada**. São cinco regiões e a única coisa que muda
é a porcentagem — o enunciado diz isso explicitamente: *"é só a porcentagem que
muda, a conta é a mesma em todas"*. Uma tabela de dados é a resposta certa. A
regra é fictícia na medida em que a loja é fictícia; o que importa é a forma
(ver "O que mudou para o V4").

Se o braço com harness escrever cinco classes para isso, **é custo do harness** —
e no enunciado original esse custo seria invisível, porque não havia ponto onde
exagerar fosse errado.

---

## O que mais mudou em relação ao piloto

Além dos dois pontos (na versão de 24/09; no V4, leia "seguro" onde está "imposto"):

- a ordem de cálculo ganhou um passo (o imposto, entre o frete e o total)
- a requisição ganhou `nivelClube` e `regiao`
- a resposta ganhou `imposto`, `creditoProximaCompra` e `brinde`
- dois códigos de erro novos, `NIVEL_CLUBE_INVALIDO` e `REGIAO_INVALIDA`, nas
  posições 2 e 3 da precedência, o que renumerou os oito antigos
- um quinto exemplo conferido, cujo crédito do OURO cai num **empate** de
  arredondamento (20,485 → 20,48), exercitando o meio-para-o-par

O enunciado continua sem nenhuma palavra de arquitetura: não aparece "padrão",
"interface", "polimorfismo", "estratégia", "extensível", "abstrato" nem "classe".

## Um experimento, um piloto

A primeira ideia foi tratar os dois enunciados como dois experimentos. A decisão
final é outra: o experimento é **um só**, com este enunciado, e o lote de três
pontos é **piloto**. Ele não entra na análise, porque o estímulo é outro e as
execuções não são comparáveis. Fica registrado como o motivo de P4 e P5
existirem.
