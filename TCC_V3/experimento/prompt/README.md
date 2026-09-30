# O enunciado

| arquivo | hash | pontos de variação | execuções |
|---|---|---|---|
| `prompt.md` | `b7cdb594cb49efee` | P1 entrega · P2 cupons · P3 pagamento · **P4 clube** · **P5 imposto** | `EXT-01` a `03` — o experimento |
| `../../historico/piloto/prompt.md` | `53db3424b3972795` | P1 entrega · P2 cupons · P3 pagamento | `BATCH-01` a `03` — o piloto |
| `state.md` | `ebffe1724ca316b5` | E1 ações por situação · E2 efeitos do cancelamento e da devolução · **E3 texto** (controle negativo) | `STATE-01` a `03`, a rodar, com `PROMPT_FILE` |

O `state.md` é o segundo padrão testado (State): a situação de um pedido depois
da compra, que muda a cada ação. É um experimento à parte, com o mesmo harness e a
mesma bancada; o gabarito dele está em `avaliacao/state/gabarito.md`.

Nenhum dos dois muda. O de cinco pontos se chamava `prompt-estendido.md` até
virar o enunciado único; o de três era o `prompt.md` e foi para
`historico/piloto/`. Os bytes são os mesmos, e os hashes acima conferem com o
`meta.json` de cada execução.

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

## P5 — imposto por região. **Controle negativo**, testa a **quarta** regra

> trate assim apenas o que o enunciado descreve como variando. Não crie estrutura
> para variação que você imagina que possa vir a existir.

Aqui Strategy é a resposta **errada**. São cinco regiões e a única coisa que muda
é a porcentagem — o enunciado diz isso explicitamente: *"é só a porcentagem que
muda, a conta é a mesma em todas"*. Uma tabela de dados é a resposta certa.

Se o braço com harness escrever cinco classes para isso, **é custo do harness** —
e no enunciado original esse custo seria invisível, porque não havia ponto onde
exagerar fosse errado.

---

## O que mais mudou em relação ao piloto

Além dos dois pontos:

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
