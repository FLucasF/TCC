# Orientações para construir este projeto

- Antes de escrever cada parte, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do
  código teriam de mudar? Se forem vários, ou se uma classe passaria a conhecer coisas que não são
  responsabilidade dela, organize para que o próximo caso entre num lugar só, sem mexer no que já funciona.
- Faça essa organização só onde o enunciado mostra que a variação existe e vai continuar. Um valor, um texto, um
  campo pedem uma solução simples, sem estrutura para o que ninguém pediu.
- Quando o serviço conversa com um sistema de fora, os termos e as unidades dele ficam numa peça só, do lado dele.
- Teste o que o enunciado pede, conferindo os valores, e termine com o `mvn verify` passando.
- Antes de decidir a estrutura, consulte a skill `gof-patterns`.
