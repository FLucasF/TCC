# Orientações para construir este projeto

- Antes de escrever cada parte, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do
  código teriam de mudar? Se forem vários, ou se uma classe passaria a conhecer coisas que não são
  responsabilidade dela, organize para que o próximo caso entre num lugar só, sem mexer no que já funciona.
  Acrescentar mais uma condição num método que já existe é mexer no que já funciona. Os primeiros casos entram
  nessa organização do mesmo jeito que os próximos.
- Quando vários casos repetem a mesma sequência de passos, escreva a sequência uma vez só e deixe cada caso mudar
  apenas o que é dele.
- Faça essa organização só onde o enunciado mostra que a variação existe e vai continuar. Um valor, um texto, um
  campo pedem uma solução simples, sem estrutura para o que ninguém pediu.
- Antes de decidir a estrutura, use a skill `design-patterns`.
