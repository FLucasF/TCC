# A dimensão: a implementação dos padrões

A correção vai em `correcao/padroes.md`. Não há material além do enunciado e do projeto.

O enunciado foi escrito para que alguns pontos peçam um padrão sem dizer o nome dele, e um ponto que parece pedir
e não pede (o controle negativo). A régua tem um item por ponto. Ela não pergunta "o padrão foi usado?", e sim
onde mora o código de cada caso e como o caso é escolhido: um nome de classe não conta, o que conta é o que o
código faz.

## As definições

**Os casos de cada ponto**, com os códigos do enunciado:

| ponto | os casos |
|---|---|
| P1, a entrega | `ECONOMY`, `EXPRESS`, `STORE_PICKUP`, `COURIER` |
| P2, o cupom | `WELCOME10`, `MINUS50`, `FREESHIPPING`, `BUY3PAY2` |
| P3, a forma de pagamento | `PIX`, `CREDIT_CARD`, `BANK_SLIP` |
| P4, o nível do clube | `BRONZE`, `SILVER`, `GOLD` |
| P5, a região | `SOUTHEAST`, `SOUTH`, `MIDWEST`, `NORTH`, `NORTHEAST` |
| P7, a situação do pedido | `AWAITING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

**Nomear um caso** é escrever qual ele é: o texto `"GOLD"`, a constante `GOLD`, a classe `GoldTier`,
`instanceof GoldTier`, `case GOLD ->`, o nome traduzido (`Nivel.OURO`) e a comparação pela ordem
(`tier.ordinal() >= 2`, `tier.compareTo(SILVER) > 0`). O teste: *se eu criar um caso parecido, esta linha
precisa mudar?* Se precisa, ela nomeia. `tier.freeShipping()` não nomeia: pergunta a qualquer nível, e um nível
novo responde sozinho.

**A casa de um caso** é a unidade que é só dele: uma classe, uma constante de enum com corpo próprio, uma entrada
de mapa com uma função própria. **Uma classe aninhada é casa** (uma `class FreeShipping` dentro de `Coupons.java`
é a casa do FREESHIPPING).

**Não conta como código de um caso fora da casa dele:**
- o nome usado só como chave de registro, ou numa lista de válidos (um bloco que cita todos os casos só para
  conferir se o código existe ou para traduzir o texto para a constante);
- uma constante de um caso declarada fora dele;
- uma função genérica que não cita o caso, mesmo que só um caso a use (um método `default`, uma classe-base);
- **a regra cruzada na casa do par.** O enunciado tem duas regras que obrigam a citar um caso de outro ponto: o
  FREESHIPPING não vale na retirada (`STORE_PICKUP`), e o GOLD paga o frete no motoboy (`COURIER`). Citar o caso
  do outro ponto dentro da casa do par (o `STORE_PICKUP` dentro do cupom FREESHIPPING, o `COURIER` dentro do
  nível GOLD, ou o inverso) não espalha. Citar num terceiro lugar (um serviço, o controller) espalha. O mesmo vale
  para o desconto do Pix "só sem cupom": a regra pode morar no Pix;
- **a tradução para o código da transportadora** (`EXPRESS -> "EXP"`, `ECONOMY -> "STD"`) dentro do lado da
  TransLog: isso é trabalho do adaptador, visto no P6, e não uma decisão de entrega.

## P1 a P4: Strategy (a entrega, o cupom, o pagamento e o clube)

O enunciado diz que esses pontos vão crescer ("quase toda semana entra uma opção nova de entrega", "o marketing
adora inventar promoção", "estamos estudando criar mais níveis"). Para cada um, responda a duas perguntas.

**Onde mora o código de cada caso?** O certo é todo o código **específico** de cada caso estar na casa dele. O
problema é algum caso ter código específico **também** fora da casa, inclusive um `if` que o nomeia numa validação
ou na escolha do código de erro. Se os casos não têm casa (só existem como texto ou como constantes de um enum sem
corpo, comparados em métodos de todos), qualquer código específico de um caso está fora da casa. Como conferir:
busque os nomes dos casos e também o que eles fazem (para o clube: `credit`, `shipping`, `gift`); para cada linha
fora da casa, pergunte *ela faz alguma coisa só para este caso?* Cite cada linha que escapa.

**Como o caso é escolhido?** Do texto do pedido até a conta, há três formas, da melhor para a pior:
- **por consulta**, sem nomear nenhum caso: um `Map`, beans injetados e achados por código, o `valueOf` de um enum
  que carrega o comportamento, um laço genérico. Um `if` que só recusa o pedido (valida, não calcula) não muda isto;
- **por uma condicional única** que nomeia os casos num lugar só e **devolve um objeto** para outro calcular (uma
  fábrica);
- **por uma condicional no cálculo**, que nomeia os casos e **faz a conta** ali. Vale também quando o caso foi
  achado por consulta, mas a conta de algum caso é feita por um `if` que o nomeia (o `if` passou por cima da
  estrutura). Um `enum` que é só rótulo, com a conta num `switch` que nomeia os casos, está aqui; um `switch (this)`
  ou `if (this == GOLD)` dentro de um método do enum que faz a conta também.

Diga qual das três é, e, se a estrutura existe mas algum caso escapa dela, qual.

## P5: a região (o controle negativo)

O enunciado só muda **números** por região: a porcentagem e o limite do seguro, e os dias a mais do Norte e do
Nordeste. A resposta do tamanho do problema é **a variação como dado**: um enum que só carrega números (e um método
só para todas as regiões), um mapa de números, uma tabela, um arquivo de configuração. Uma lista ou mapa de objetos
**da mesma classe** para todas as regiões, só com os números, também é uma tabela. Um `switch` que só devolve o
número de cada região é aceitável, mas é condicional, não dado.

O exagero, que este ponto mede, é **estrutura por região**: uma classe por região, uma constante de enum **com
corpo**, ou um mapa ou `switch` que devolve um objeto com comportamento próprio por região, mesmo que todas façam a
mesma conta. Diga a forma que o agente usou e se ela tem o tamanho do problema.

## P6: Adapter (a TransLog)

**O vocabulário da transportadora fica do lado dela?** O lado da TransLog são os arquivos que fazem a chamada
HTTP, mais os da mesma pasta que não conhecem os nossos casos (os records da resposta, os enums dela). O vocabulário
dela é: o formato da API (`price_cents`, `eta_hours`, `ZIP_NOT_SERVED`, a rota `/v2/quote`, `weight_g`), os nomes
desses campos no nosso código (`priceCents`, `etaHours`), os códigos de serviço (`"STD"`, `"EXP"`) **e as
unidades dela** (centavos, horas, gramas).

O certo é nada disso aparecer fora do lado da TransLog: o resto do código recebe o frete em reais e o prazo em dias,
e entrega o peso em quilos. É vazamento usar qualquer termo, campo ou unidade da TransLog fora do lado dela, e
**converter a unidade fora do adaptador é vazamento**: uma estratégia de entrega que faz `etaHours / 24` recebeu a
unidade da transportadora, e não a nossa. Declarar um campo com esse nome do lado dela (o record da resposta, um
`@JsonProperty`) não é vazamento. Se o enunciado daquela execução não tinha o vocabulário estrangeiro da TransLog,
diga que o ponto não mede nada ali.

## P7: State, secundário (a situação do pedido)

**As decisões sobre a situação moram na própria situação?** Uma decisão é uma comparação com uma situação
(`status == PAID`), um rótulo de `switch` ou um conjunto que não cita todas as situações. A casa é o tipo da situação
(`OrderStatus`, `OrderState`, uma classe por situação), inclusive aninhado (`enum Status` dentro de `Order`). Não
contam a lista de válidos e o rótulo que só traduz o texto para a constante.

O certo é as decisões de qual operação é permitida e do que muda em cada transição estarem no tipo da situação. O
problema é alguma decisão sobre a situação estar fora dele (num serviço, no controller), inclusive o "cancelar um
pago devolve o estoque, um não pago não" decidido com um `if` de situação no cancelamento.

## F: Factory (descritivo)

**Como os objetos de cada caso (as entregas, os cupons, as formas de pagamento, os níveis) são criados?** Pelo
contêiner do Spring (`@Component`/`@Service`), por métodos `@Bean`, por uma fábrica (um `switch`/`if` que só
devolve o objeto), por `new` espalhado, por constantes de enum com corpo, ou não há objeto por caso. Este item não
é bom nem ruim: só descreve.

## E: estrutura sem necessidade

**O projeto criou estrutura que nenhum ponto do enunciado pede?** Exemplos: uma fábrica ou um builder para um objeto
sem regra (o item do carrinho, o aviso), uma interface com um adaptador para um valor fixo (o frete do motoboy, a
retirada), uma classe por situação do aviso (que só vai de `NEW` para `READ`), uma interface com uma implementação
só que nada substitui. Cite cada caso, ou diga que não há. Não conte aqui a estrutura dos pontos P1 a P7, que já
tem item próprio.
