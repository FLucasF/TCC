# As instruções do pattern: Strategy

Este pattern cuida de quatro pontos que pedem o padrão (P1 a P4) e de um controle negativo (P5). A correção vai em
`correcao/padroes-strategy.md`.

## Os pontos que pedem o padrão (P1 a P4)

| ponto | o que no enunciado pede o padrão |
|---|---|
| P1, a entrega | "quase toda semana entra uma opção nova de entrega, cada uma com seu jeito de cobrar, seu prazo e suas limitações" |
| P2, o cupom | "o pessoal do marketing adora inventar promoção" |
| P3, a forma de pagamento | sem frase de crescimento: três formas, cada uma com o seu ajuste (o desconto do Pix, os juros do cartão, a tarifa do boleto), as suas parcelas e as suas recusas |
| P4, o nível do clube | "estamos estudando criar mais níveis, e cada um vai ter seu conjunto de vantagens" |

A resposta certa é o Strategy em cada um: o comportamento de cada caso (a conta, o prazo, a regra de quem pode
usar, as vantagens) num lugar só dele, e o cálculo pedindo ao caso, sem nomear os casos. Como o agente escreveu o
projeto do zero, a pergunta é se o serviço **já nasceu** assim. Para cada ponto, responda a duas perguntas.

**`Pn-local`: onde mora o código de cada caso?** O certo é todo o código **específico** de cada caso estar na casa
dele. O problema é algum caso ter código específico **também** fora da casa, inclusive um `if` que o nomeia numa
validação ou na escolha do código de erro. Se os casos não têm casa (só existem como texto ou como constantes de um
enum sem corpo, comparados em métodos de todos), qualquer código específico de um caso está fora da casa. Como
conferir: busque os nomes dos casos e também o que eles fazem (para o clube: `credit`, `shipping`, `gift`); para cada
linha fora da casa, pergunte *ela faz alguma coisa só para este caso?* Cite cada linha que escapa.

**`Pn-selecao`: como o caso é escolhido?** Do código que chega na requisição da compra (o `shippingMethod`, o
`coupon`, o `paymentMethod`, o `clubTier`) até a conta, há três formas, da melhor para a pior:
- **por consulta**, sem nomear nenhum caso: um `Map`, beans injetados e achados por código, o `valueOf` de um enum
  que carrega o comportamento, um laço genérico. Um `if` que só recusa o pedido (valida, não calcula) não muda isto;
- **por uma condicional única** que nomeia os casos num lugar só e **devolve um objeto** para outro calcular (uma
  fábrica);
- **por uma condicional no cálculo**, que nomeia os casos e **faz a conta** ali. Vale também quando o caso foi
  achado por consulta, mas a conta de algum caso é feita por um `if` que o nomeia (o `if` passou por cima da
  estrutura).

**Acerto** é uma interface (ou classe abstrata) com uma implementação por caso, escolhida por consulta ou por uma
condicional única; também é acerto o `enum` como estratégia, cada constante com o seu próprio corpo (métodos
sobrescritos, ou o comportamento passado no construtor) ou um mapa do caso para uma função própria. Um `enum` que só
carrega **dados** de cada caso (as porcentagens do clube, por exemplo), com um método só para todos, também é acerto
quando os casos diferem só nesses dados.

**Omissão** é a variação resolvida com `switch` ou `if` que nomeiam os casos no cálculo ou no serviço. Um `enum` que
é só rótulo, com a conta num `switch` que nomeia os casos, é omissão; um `switch (this)` ou `if (this == GOLD)`
dentro de um método do enum que faz a conta também: é o mesmo `switch`, só que dentro do enum, e o próximo caso continua
mexendo em vários lugares.

**Malfeito** é a estrutura existir, mas algum caso escapar dela: a conta de um caso feita por um `if` fora da casa,
uma regra de um caso no serviço ou no controller, quem usa a estrutura perguntando qual é o caso concreto.

## Os remendos: as regras cruzadas

O enunciado tem quatro regras que tentam a escrever um `if` no lugar errado, um item para cada:

| item | a regra | onde ela pode morar | o remendo |
|---|---|---|---|
| R1 | o GOLD não paga frete, **exceto no motoboy** | no nível (ou numa política de frete) | `if (tier == GOLD && method != COURIER)` no cálculo do frete |
| R2 | o FREESHIPPING **não vale na retirada** | no cupom | `if (coupon == FREESHIPPING && method == STORE_PICKUP)` no serviço |
| R3 | o desconto do Pix **só sem cupom** | no Pix, que recebe o contexto | `if (payment == PIX && coupon == null)` no serviço |
| R4 | o WELCOME10 **só na primeira compra** | no cupom, que consulta os pedidos | a checagem do histórico no controller ou no serviço |

**A regra cruzada na casa do par não espalha.** Citar o caso do outro ponto dentro da casa do par (o `STORE_PICKUP`
dentro do cupom FREESHIPPING, o `COURIER` dentro do nível GOLD, ou o inverso) é onde a regra deve morar. Citar num
terceiro lugar (um serviço, o controller, o cálculo geral) espalha, e é o remendo. **A tradução para o código da
transportadora** (`EXPRESS -> "EXP"`, `ECONOMY -> "STD"`) dentro do lado da TransLog também não espalha a entrega: é
trabalho do adaptador.

Para cada regra, diga onde o agente a pôs.

## O controle negativo: a região (P5)

O enunciado só muda **números** por região: a porcentagem e o limite do seguro, e os dias a mais do Norte e do
Nordeste ("a seguradora cobra por região", com a tabela). Parece pedir uma estrutura por região, e não pede.

**`P5-forma`: que forma a variação tem?** Um enum que só carrega os números, um enum com corpo por constante, uma
classe por região, um mapa, um `switch`, ou outra (descreva).

**`P5-proporcao`: a resposta tem o tamanho do problema?** É **contenção** a variação como **dado**: um enum que só
carrega números (e um método só para todas as regiões), um mapa de números, uma tabela, um arquivo de configuração.
Uma lista ou mapa de objetos **da mesma classe** para todas as regiões, só com os números, também é uma tabela. Um
`switch` que só devolve o número de cada região é aceitável, mas é condicional, não dado. É **exagero** a estrutura
por região: uma classe por região, uma constante de enum **com corpo**, ou um mapa ou `switch` que devolve um objeto
com comportamento próprio por região, mesmo que todas façam a mesma conta.
