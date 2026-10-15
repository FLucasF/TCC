# TCC_V7: a manutenção com patterns (em construção)

Uma pesquisa separada do V6. O V6 mede a **construção** de um serviço do zero (o checkout da loja).
O V7 mede a **manutenção**: o agente recebe um serviço que já existe e funciona, escrito sem nenhum
pattern, e um pedido de mudança. A pergunta é o que ele faz com o pattern:

- **aplica, e aplica certo**, quando a mudança precisa dele;
- **não aplica** (mais um `if`, mais uma cópia) quando a mudança precisava;
- **aplica sem necessidade** (exagero).

**Nada aqui está congelado.** É um rascunho de trabalho.

## O desenho, até agora

| | |
|---|---|
| patterns medidos | Strategy, Observer, Factory (Factory Method), Decorator e Template Method |
| domínio | uma pousada (reservas), sugerido por IA (Claude Opus 5.5, 15/10/2026), como pediu o orientador |
| pilha | Java 21 + Javalin 6.7.0, sem Spring Boot: o Spring entrega de graça o Observer (eventos), a Factory (o contêiner) e o Decorator (AOP), e aí não há o que medir |
| a base | um projeto só, escrito antes e igual para todos; cada execução sai de uma cópia limpa dela e implementa **um** cenário |
| cenários | 10: 5 que pedem o pattern e 5 **controles negativos**, pedidos parecidos em que a resposta certa é uma mudança pequena (são eles que medem o exagero) |
| execuções | de início, 1 por modelo em cada cenário |
| avaliação | por agente, com a leitura do Lucas numa amostra para medir a concordância |
| o texto dos pedidos | mais escrito e menos técnico: a voz do dono da pousada, sem nomear pattern |

O princípio da base é deixar cada ponto a **um passo da regra de três**: hoje o código está certo sem
pattern, e é o pedido de mudança que passa a justificá-lo. Os detalhes estão no roteiro.

## O que fica onde

| pasta | o que tem |
|---|---|
| [`experiment/base/ROTEIRO-DA-BASE.md`](experiment/base/ROTEIRO-DA-BASE.md) | o roteiro da base: as sementes de cada pattern, os pontos dos controles negativos, o que a base não pode ter, as decisões em aberto |
| [`experiment/base/hotel/`](experiment/base/hotel/) | a base: o sistema de reservas da Pousada Maré Alta (`mvn verify`; `java -jar target/hotel.jar`) |
| [`experiment/scenarios/`](experiment/scenarios/) | os 10 pedidos de mudança, na voz do dono da pousada, e a moldura comum (`frame.md`): o agente recebe a moldura com **um** pedido no lugar de `{{PEDIDO}}`. Os ímpares pedem o pattern; os pares são os controles negativos |
| [`evaluation/answer-key/GABARITO.md`](evaluation/answer-key/GABARITO.md) | o gabarito: a regra que separa os dois tipos de cenário, a classificação, e, para cada cenário, o acerto, as alternativas aceitas, a omissão, o malfeito, o exagero e o que a suíte confere |
