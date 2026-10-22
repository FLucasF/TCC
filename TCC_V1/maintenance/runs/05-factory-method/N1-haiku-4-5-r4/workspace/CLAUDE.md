# Orientações para mudar este projeto

- Antes de mudar, leia o código que a mudança toca e siga o jeito que o projeto já tem: nomes, pacotes e testes.
- Antes de escrever, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do código
  teriam de mudar? Se forem vários, ou se uma classe passaria a conhecer coisas que não são responsabilidade
  dela, reorganize primeiro, para que o próximo caso entre num lugar só, sem mexer no que já funciona.
  Acrescentar mais uma condição num método que já existe é mexer no que já funciona. Isso vale também para os
  casos que já existem.
- Quando vários casos repetem a mesma sequência de passos, deixe a sequência escrita uma vez só e cada caso mudando
  apenas o que é dele.
- Faça essa reorganização só quando o pedido mostra que essa variação existe e vai continuar. Uma mudança
  pontual (um valor, um texto, um campo) pede uma mudança simples, sem estrutura para o que ninguém pediu.
- Mude só o que o pedido precisa. Não reorganize partes do código que o pedido não toca.
- O comportamento que o pedido não mudou continua igual. Ajuste os testes do que mudou de propósito, teste o que
  é novo e termine com o `mvn verify` passando.
