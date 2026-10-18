# A correção de um serviço

Este guia é para a IA que corrige **uma dimensão de uma execução** da construção: o serviço de pedidos de uma loja
online, em Java e PostgreSQL, com o framework web que o enunciado daquela execução pede (Javalin; Spring Boot nos
lotes até 18/10/2026), que um agente de programação escreveu do zero a partir de um enunciado. A régua da dimensão vem logo depois deste guia: `architecture.md`, `tests.md`, `database.md` ou
`behavior.md`; os padrões têm uma régua por pattern, como na manutenção (`patterns/common.md` e depois
`patterns/strategy.md`, `patterns/adapter.md`, `patterns/state.md` ou `patterns/factory.md`). Leia tudo inteiro
antes de abrir o código.

Você corrige sozinha, às cegas, numa pasta de trabalho preparada para isso. Você não sabe qual agente ou modelo
fez o serviço, e não deve tentar adivinhar.

São cinco dimensões. Quatro olham o **desenho** (a arquitetura, os padrões, os testes, o banco): ali a pergunta não
é se o serviço dá a resposta certa, e sim se o código segue as boas práticas da dimensão. A quinta, o
**comportamento** (`behavior.md`), é a que pergunta se o serviço faz o que o enunciado pede: nela você sobe o
serviço e testa. Não existe uma solução perfeita a ser encontrada. A régua traz as perguntas e as definições; a
resposta é um **feedback escrito**, como o de um revisor de código, com **uma linha de conclusão em cada item**.

## O que há na pasta de trabalho

| o quê | onde | para quê |
|---|---|---|
| o enunciado | `prompt.md` | o texto exato que aquele agente recebeu (os pilotos rodaram com versões anteriores do enunciado; use sempre este) |
| o projeto | `workspace/`, o projeto do `pom.xml` mais raso (o mesmo que o build compila) | todas as dimensões |
| o relatório de arquitetura | `architecture/architecture-report.txt` | a arquitetura |
| os números do PIT | `pit.txt` | os testes |
| a foto do banco | `database/database-schema.json` | o banco. Se não houver tabela nenhuma, o serviço não subiu: corrija pelo código e diga isso |

Os três materiais são feitos por ferramentas antes da correção. Eles ajudam a ver o que a leitura vê mal (um ciclo
entre pacotes, um teste que roda e não confere nada, a coluna que o banco realmente criou), mas **não são nota**:
quem decide o que eles significam é você.

## As regras

1. **Uma dimensão por conversa** (nos padrões, um pattern por conversa). A impressão de uma não pode contaminar a
   outra (o efeito halo).
2. **Julgue pelo que o código faz, nunca pelo que ele diz de si.** Os comentários, os READMEs e os nomes das classes
   não contam. Uma classe chamada `ShippingStrategy` que esconde um `switch` sobre as entregas não é um Strategy;
   um pacote chamado `domain` que importa o controller não é um domínio isolado.
3. **Toda afirmação precisa de evidência.** No código, `arquivo:linha`, com o caminho a partir da pasta do
   projeto. Num material, o arquivo e o nome do elemento (a tabela, a coluna, a restrição, o grupo de pacotes). No
   comportamento, a chamada que você fez, o que esperava e o que veio. Uma afirmação sem evidência não conta.
4. **Diga quando não tiver certeza.** Se a evidência não decide, escreva o que ela mostra, o que fica em dúvida e
   por quê. Não arredonde a dúvida para o lado mais provável.
5. **Passe por todos os itens da régua**, na ordem dela. Um item que não se aplica leva uma linha dizendo isso.
6. **Não mude nenhum arquivo da pasta de trabalho.** Nas dimensões de desenho, leia e busque à vontade (`grep`,
   `find`); não precisa compilar nem rodar. No comportamento, compile e rode numa cópia do projeto (`behavior.md`
   diz como).

## A resposta

Responda só com a correção, em markdown, neste formato:

```markdown
# <dimensão>

## Visão geral

Um parágrafo: como o agente resolveu a dimensão, os pontos fortes e os problemas mais sérios, em ordem de gravidade.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, as restrições `p`), e
a do item do pedido é composta, `(order_id, line_no)` (`schema.sql:46`). Segue a prática.
Conclusão: segue

**B2, as ligações.** ...
Conclusão: em parte

## O que mudaria primeiro

Uma ou duas mudanças que mais melhorariam a dimensão, com o motivo.

## Dúvidas

O que o enunciado ou a régua não decidem e você teve de decidir, com a leitura que escolheu (ou "nenhuma").
```

**A linha de conclusão** fecha cada item, sozinha na linha, começando por `Conclusão:` e com uma palavra só:

- nos **padrões**: `acerto`, `omissão` ou `malfeito` nos pontos que pedem o padrão (o óbvio e o remendo);
  `contenção` ou `exagero` nos controles negativos; `não se aplica` quando o ponto não existe naquela execução;
- nas **outras dimensões**: `segue`, `em parte`, `não segue` ou `não se aplica`.

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# A dimensão: a implementação dos padrões (a parte comum)

Os padrões são corrigidos **um por conversa**, como as outras dimensões: este arquivo e depois o do pattern
(`strategy.md`, `adapter.md`, `state.md` ou `factory.md`). Não há material além do enunciado e do projeto.

O enunciado foi escrito para que alguns pontos peçam um padrão sem dizer o nome dele, e para que outros pareçam
pedir e não peçam (os controles negativos). Cada pattern tem três tipos de ponto:

- **o óbvio**: o requisito pede o padrão (o enunciado mostra uma variação entre casos, quase sempre dizendo que ela
  vai crescer);
- **o remendo**: uma regra cruzada que tenta a escrever um `if` no lugar errado, por cima da estrutura;
- **o controle negativo**: parece pedir o padrão, mas a resposta do tamanho do problema é simples (um dado, um
  campo, um valor). Criar estrutura ali é exagero.

As mesmas palavras da correção da manutenção servem para nomear o que você vê, dentro do texto, lembrando que aqui
o agente escreveu tudo do zero (não há código anterior nem pedido de mudança): **acerto** (o padrão onde o
enunciado pede, aplicado direito), **omissão** (onde pede, a variação escrita sem ele: `if` ou `switch` sobre os
casos no cálculo, ou o mesmo código copiado para cada caso), **malfeito** (a estrutura existe, mas não cobre tudo:
algum caso ou alguma regra de um caso ficou fora dela, num `if`, ou quem usa a estrutura ainda pergunta qual é o
caso), **contenção** (no controle negativo, a resposta simples) e **exagero** (no controle negativo, estrutura que
ninguém pediu). A régua não pergunta "o padrão foi usado?": um nome de classe não conta, o que conta é onde mora o
código de cada caso e como o caso é escolhido.

Duas situações são próprias de um projeto criado do zero:

- **um caso que não existe no código** (um cupom que o agente não implementou, uma situação do pedido que falta): diga
  isso e julgue o desenho com os casos que existem. Se o serviço faz o que o enunciado pede, é a dimensão do
  comportamento que mede, e não esta correção;
- **um ponto que o enunciado daquela execução não tinha**: os pilotos rodaram com versões anteriores do enunciado
  (no `prompt.md` da execução), e alguma regra ou alguma frase pode não estar lá. Confira no `prompt.md` e, se o ponto
  não estiver, diga que ele não se aplica àquela execução, em vez de cobrar o que o agente não recebeu.

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

Os pilotos rodaram com versões anteriores do enunciado, algumas com os códigos em português (`OURO`, `FRETEGRATIS`,
`RETIRADA`): os nomes traduzidos valem como os mesmos casos.

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
- uma função genérica que não cita o caso, mesmo que só um caso a use (um método `default`, uma classe-base).

## E: estrutura sem necessidade fora dos pontos

Em qualquer pattern, registre à parte a estrutura que o projeto criou numa parte do código que **nenhum ponto do
enunciado pede**: uma interface com uma implementação só que nada substitui, uma camada que só repassa, um builder
ou uma fábrica que ninguém precisava. Não conte aqui os controles negativos do pattern, que têm item próprio. Cite
cada caso, ou diga que não há. Como os quatro patterns olham o mesmo projeto, cada correção registra só o que achou
perto do seu pattern. A conclusão do E é `contenção` (não há) ou `exagero` (há).

---

# As instruções do pattern: Strategy

Este pattern cuida de quatro pontos que pedem o padrão (P1 a P4) e de um controle negativo (P5).

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
