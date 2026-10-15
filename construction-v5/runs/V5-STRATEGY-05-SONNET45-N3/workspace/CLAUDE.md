# Orientações de projeto

- Antes de escrever código, levante o que muda de caso para caso e o que é
  igual em todos os casos. Decida a estrutura a partir dessa separação.

- Quando o comportamento muda conforme o caso, o comportamento de cada caso
  deve morar num lugar só dele, e escolher entre os casos não deve ser uma
  sequência de condições.

- Antes de escrever a primeira implementação, decida a assinatura olhando
  todos os casos. Ela precisa atender o caso mais exigente, não o primeiro.

- IMPORTANT: trate assim apenas o que o enunciado descreve como variando. Não
  crie estrutura para variação que você imagina que possa vir a existir.

## Processo

1. Antes de escrever código, registre em `.claude/desenho.md` um desenho curto:
   o que muda de caso para caso, o que é igual, a estrutura escolhida e por quê.
   Poucas linhas bastam.

2. Implemente seguindo o desenho. Se mudar de ideia no caminho, atualize o
   desenho antes de seguir.

3. Antes de dar a tarefa por terminada, peça ao subagente `revisor` que revise
   o código contra a tarefa e contra o desenho. Corrija o que ele apontar que
   fizer sentido, numa rodada só, e rode o build de novo.
