# A correção de um serviço

Você vai corrigir uma dimensão do desenho de um serviço que um agente de programação escreveu do zero: o serviço
de pedidos de uma loja online, em Java com Spring Boot e PostgreSQL. O agente recebeu o enunciado que está na
pasta de trabalho e escreveu o projeto inteiro. Você não sabe qual agente ou modelo fez o trabalho, e não deve
tentar adivinhar.

A pergunta não é se o serviço dá a resposta certa (isso é medido à parte, por uma suíte de testes), e sim se o
desenho segue as boas práticas da dimensão que está nas instruções abaixo. Não existe uma solução perfeita a ser
encontrada: cada item da régua pergunta uma coisa sobre o código, e você responde com um dos valores da lista
fechada daquele item.

## A pasta de trabalho

- `enunciado.md`: o texto exato que o agente recebeu.
- `projeto/`: o projeto do agente, sem a pasta `target/`, sem os arquivos de documentação e **sem comentários**.
  Os comentários foram trocados por linhas em branco, então os números de linha são os do original.
- os materiais da dimensão, quando houver (as instruções da dimensão dizem quais são e como ler cada um).

## As regras

1. **Leia o enunciado inteiro antes de olhar o código.** Os itens da régua citam trechos dele.
2. **Julgue pelo que o código faz, nunca pelo que ele diz de si.** Uma classe chamada `ShippingStrategy` que
   esconde um `switch` sobre as entregas não é um Strategy; um pacote chamado `domain` que importa o controller
   não é um domínio isolado.
3. **Toda resposta precisa de evidência.** No código, o formato é `arquivo:linha`, com o caminho a partir de
   `projeto/`. Num material (o relatório de arquitetura, a foto do banco), cite o arquivo do material e o nome
   do elemento (a tabela, a coluna, a restrição, o grupo de pacotes). Uma resposta sem evidência não conta.
4. **Na dúvida entre dois valores, não escolha o mais provável.** Responda `indeterminado` e conte a dúvida
   no campo `observacao` do item. `indeterminado` é uma resposta válida; um valor chutado não é.
5. **Responda todos os itens**, na ordem da régua, com os ids da régua. Um item que não se aplica ao projeto
   (por exemplo, a foto do banco quando o serviço não subiu) recebe o valor `nao_se_aplica`, quando a lista do
   item o tiver, ou `indeterminado`, com o motivo.
6. **Não mude nenhum arquivo.** Você pode ler à vontade e usar comandos de leitura e busca (`grep`, `find`,
   `cat`). Não precisa compilar nem rodar o projeto.

## A resposta

Responda só com um objeto JSON, sem texto antes ou depois:

```json
{
  "dimensao": "banco",
  "itens": [
    {
      "id": "B1",
      "resposta": "atende",
      "evidencias": [
        {"arquivo": "banco.json", "linha": null, "o_que_mostra": "as 6 tabelas têm PRIMARY KEY"},
        {"arquivo": "src/main/java/com/loja/Order.java", "linha": 18, "o_que_mostra": "@Id no pedido"}
      ],
      "observacao": ""
    }
  ],
  "resumo": "duas ou três frases sobre a dimensão como um todo",
  "duvidas": ""
}
```

A `dimensao` é a das instruções abaixo. A `linha` é `null` quando a evidência é um material e não um arquivo de
código. O `resumo` não muda nenhuma resposta: é só a leitura geral, para quem for revisar a correção.

---

# A dimensão: a implementação dos padrões

`"dimensao": "padroes"`. Não há material além do enunciado e do projeto.

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
  TransLog: isso é trabalho do adaptador, medido no P6, e não uma decisão de entrega.

## P1 a P4: Strategy (a entrega, o cupom, o pagamento e o clube)

O enunciado diz que esses pontos vão crescer ("quase toda semana entra uma opção nova de entrega", "o marketing
adora inventar promoção", "estamos estudando criar mais níveis"). Cada um tem **dois itens**: `P1-local` e
`P1-selecao`, `P2-local` e `P2-selecao`, e assim até o P4.

**`Pn-local`: o código de cada caso mora na casa dele?**

| valor | quando |
|---|---|
| `isolado` | todo o código **específico** de cada caso está na casa dele |
| `espalhado` | algum caso tem código específico **também** fora da casa dele, inclusive um `if` que o nomeia numa validação ou na escolha do código de erro |

Se os casos não têm casa (só existem como texto ou como constantes de um enum sem corpo, comparados em métodos
de todos), qualquer código específico de um caso está fora da casa: é `espalhado`. Como conferir: busque os nomes
dos casos e também o que eles fazem (para o clube: `credit`, `shipping`, `gift`); para cada linha fora da casa,
pergunte *ela faz alguma coisa só para este caso?*

**`Pn-selecao`: o caminho do texto do pedido até a conta nomeia os casos?**

| valor | quando |
|---|---|
| `consulta` | o caso é achado **sem nomear** nenhum: um `Map`, beans injetados e achados por código, o `valueOf` de um enum que carrega o comportamento, um laço genérico. Um `if` que só recusa o pedido (valida, não calcula) não muda isto |
| `condicional_unica` | um `switch`/`if` que nomeia os casos **num lugar só** e **devolve um objeto** para outro calcular (uma fábrica) |
| `condicional_no_calculo` | um `switch`/`if` que nomeia os casos e **faz a conta** ali. Vale também quando o caso foi achado por consulta, mas a conta de algum caso é feita por um `if` que o nomeia (o `if` passou por cima da estrutura) |

Um `enum` que é só rótulo, com a conta num `switch` que nomeia os casos, é `condicional_no_calculo`. Um
`switch (this)` ou `if (this == GOLD)` dentro de um método do enum que faz a conta também.

## P5: a região (o controle negativo)

O enunciado só muda **números** por região: a porcentagem e o limite do seguro, e os dias a mais do Norte e do
Nordeste. Separar cada região numa peça é o exagero que este ponto mede. Dois itens:

**`P5-forma`: que formato a variação tem?** `enum_dados` (constantes que só carregam valores, e um método só para
todas), `enum_com_corpo` (alguma constante com corpo próprio, `NORTH { ... }`), `classes` (uma classe por região),
`mapa`, `switch` (ou cadeia de `if`), `outro` (descrever em `observacao`).

**`P5-proporcao`: a resposta tem o tamanho do problema?**

| valor | quando |
|---|---|
| `dados` | a variação como dado: um enum que só carrega números, um mapa de números, uma tabela, um arquivo de configuração |
| `condicional` | um `switch`/`if` que só **devolve o número** de cada região |
| `estrutura` | uma classe por região, uma constante de enum **com corpo**, ou um mapa ou `switch` que devolve um objeto com **comportamento próprio** por região |

Uma lista ou mapa de objetos **da mesma classe** para todas as regiões, só com os números, é uma tabela: `dados`.
É `estrutura` quando cada região tem classe, corpo ou função própria, mesmo que todas façam a mesma conta.

## P6: Adapter (a TransLog)

**`P6`: o vocabulário da transportadora fica do lado dela?**

O lado da TransLog são os arquivos que fazem a chamada HTTP, mais os da mesma pasta que não conhecem os nossos
casos (os records da resposta, os enums dela). O vocabulário dela é: o formato da API (`price_cents`,
`eta_hours`, `ZIP_NOT_SERVED`, a rota `/v2/quote`, `weight_g`), os nomes desses campos no nosso código
(`priceCents`, `etaHours`), os códigos de serviço (`"STD"`, `"EXP"`) **e as unidades dela** (centavos, horas,
gramas).

| valor | quando |
|---|---|
| `isolado` | nada do vocabulário aparece fora do lado da TransLog: o resto do código recebe o frete em reais e o prazo em dias |
| `vazado` | algum termo, campo ou unidade da TransLog é usado fora do lado dela. **Converter a unidade fora do adaptador é vazamento**: uma estratégia de entrega que faz `etaHours / 24` recebeu a unidade da transportadora, e não a nossa |

Declarar um campo com esse nome (o record da resposta, um `@JsonProperty`) do lado dela não é vazamento.

## P7: State, secundário (a situação do pedido)

**`P7`: as decisões sobre a situação moram na própria situação?**

Uma decisão é uma comparação com uma situação (`status == PAID`), um rótulo de `switch` ou um conjunto que não
cita todas as situações. A casa é o tipo da situação (`OrderStatus`, `OrderState`, uma classe por situação),
inclusive aninhado (`enum Status` dentro de `Order`). Não contam a lista de válidos e o rótulo que só traduz o
texto para a constante.

| valor | quando |
|---|---|
| `isolado` | as decisões de qual operação é permitida e do que muda em cada transição estão no tipo da situação |
| `espalhado` | alguma decisão sobre a situação está fora dele (num serviço, no controller), inclusive o "cancelar um pago devolve o estoque, um não pago não" decidido com um `if` de situação no cancelamento |

## F: Factory (descritivo)

**`F`: como os objetos de cada caso (as entregas, os cupons, as formas de pagamento, os níveis) são criados?**
Um ou mais destes, juntos com `+`: `container` (as classes têm `@Component`/`@Service` e o Spring as cria),
`configuration` (métodos `@Bean`), `factory` (um `switch`/`if` que só devolve o objeto), `new` (um `new` de uma
classe de caso fora dessas formas), `enum` (constantes com corpo), ou `nenhum` (não há objeto por caso). Este item
não é bom nem ruim: só descreve.

## E: estrutura sem necessidade

**`E`: o projeto criou estrutura que nenhum ponto do enunciado pede?** Exemplos: uma fábrica ou um builder para
um objeto sem regra (o item do carrinho, o aviso), uma interface com um adaptador para um valor fixo (o frete do
motoboy, a retirada), uma classe por situação do aviso (que só vai de `NEW` para `READ`), uma interface com uma
implementação só que nada substitui.

| valor | quando |
|---|---|
| `nao_houve` | não há estrutura desse tipo |
| `houve` | há; cite cada caso como evidência |

Não conte aqui a estrutura dos pontos P1 a P7, que já tem item próprio.
