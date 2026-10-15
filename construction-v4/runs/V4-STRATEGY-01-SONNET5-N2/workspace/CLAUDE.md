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
