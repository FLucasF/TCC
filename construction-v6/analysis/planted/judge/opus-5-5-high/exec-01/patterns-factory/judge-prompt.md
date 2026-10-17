# A correção de um serviço

Este guia é para a IA que corrige **uma dimensão de uma execução** da construção: o serviço de pedidos de uma loja
online, em Java com Spring Boot e PostgreSQL, que um agente de programação escreveu do zero a partir de um
enunciado. A régua da dimensão vem logo depois deste guia: `architecture.md`, `tests.md`, `database.md` ou
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

# As instruções do pattern: Factory (a criação concentrada)

Este pattern cuida de como os objetos são criados (F) e de um controle negativo (os objetos sem regra). O que se
mede é o **princípio criacional**, a criação concentrada num lugar só, e não a *Factory Method* estrita do GoF.

## O ponto: a criação dos objetos de cada caso (F)

Os objetos de cada caso dependem de outros: a econômica e a expressa precisam do cliente da TransLog; o WELCOME10
precisa dos pedidos do cliente; o cálculo precisa de todas as peças. A resposta certa é montar esses objetos num
lugar só, para que um caso novo, ou uma dependência nova, mude esse lugar e não os que usam. O contêiner do Spring é
uma fábrica: as classes com `@Component` injetadas já são criação concentrada. (Se o resumo e o pedido registrado
usam **o mesmo cálculo** é o A5 da arquitetura; aqui conta só como as peças são criadas.)

**`F`: como os objetos de cada caso (as entregas, os cupons, as formas de pagamento, os níveis) são criados, e a
criação está concentrada?** As formas que aparecem: pelo contêiner do Spring (as classes com `@Component`/`@Service`,
injetadas), por métodos `@Bean` numa configuração, por uma fábrica (um `switch`/`if` que só devolve o objeto, ou um
registro), por constantes de enum com corpo, por `new` espalhado nos lugares que usam, ou não há objeto por caso.

**Este item é descritivo** (decisão do Lucas, 14/10/2026: nos 8 pilotos, o contêiner do Spring cria todas as
estratégias de quem as tem, e a pergunta não separa ninguém). Diga a forma e se a criação está concentrada, sem
chamar de acerto ou omissão; a conclusão do F é `não se aplica`, a marca de um item que descreve. Vale registrar quando aparece o `new` dos casos e das suas dependências espalhado e
repetido (`new ExpressShipping(new TranslogClient(url))` em mais de um lugar, cada fluxo criando as suas peças).
Quando o agente não criou objeto por caso (a variação
está em `if`), diga isso: a criação não tem o que concentrar, e o problema já aparece no Strategy.

## O controle negativo: os objetos sem regra (F-exagero)

O item do carrinho (`sku` e quantidade), o aviso ao cliente, o resumo: objetos sem regra de criação, que se criam
direto. É **contenção** o construtor, um record, um método de fábrica estático simples. É **exagero** uma fábrica, um
builder ou uma "factory" para um objeto que só junta campos, sem nenhuma decisão na criação.
