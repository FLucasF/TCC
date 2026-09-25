# Os enunciados

| arquivo | hash | pontos de variação |
|---|---|---|
| `prompt.md` | `53db3424b3972795` | P1 entrega · P2 cupons · P3 pagamento |
| `prompt-estendido.md` | `b7cdb594cb49efee` | os três, mais **P4 clube** e **P5 imposto** |

O `prompt.md` é o do lote `BATCH-01` a `03`. **Não muda.**

---

## Por que existe um estendido

O lote com o `prompt.md` mostrou **efeito de teto**: Opus 5 e Sonnet 5 deram 3 de 3
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

## O que mais mudou no estendido

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

## O que ele NÃO é

Não é substituto do `prompt.md`, é um **segundo** experimento. As execuções feitas
com ele não são comparáveis com as do lote, porque o estímulo é outro.

A leitura que os dois juntos permitem é mais forte que a de cada um: *o efeito é
invisível nesta dificuldade e aparece naquela* diz mais que *o efeito existe*.
