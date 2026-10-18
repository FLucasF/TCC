# As instruções do pattern: Factory (a criação concentrada)

Este pattern cuida de como os objetos são criados (F) e de um controle negativo (os objetos sem regra). O que se
mede é o **princípio criacional**, a criação concentrada num lugar só, e não a *Factory Method* estrita do GoF.

## O ponto: a criação dos objetos de cada caso (F)

Os objetos de cada caso dependem de outros: a econômica e a expressa precisam do cliente da TransLog; o WELCOME10
precisa dos pedidos do cliente; o cálculo precisa de todas as peças. A resposta certa é montar esses objetos num
lugar só, para que um caso novo, ou uma dependência nova, mude esse lugar e não os que usam. Um contêiner de injeção
é uma fábrica (no Spring, as classes com `@Component` injetadas já são criação concentrada); sem contêiner, como no
Javalin, a criação concentrada é a montagem num lugar só (o `main`, ou uma classe que cria as peças e as entrega). (Se o resumo e o pedido registrado
usam **o mesmo cálculo** é o A5 da arquitetura; aqui conta só como as peças são criadas.)

**`F`: como os objetos de cada caso (as entregas, os cupons, as formas de pagamento, os níveis) são criados, e a
criação está concentrada?** As formas que aparecem: pelo contêiner do Spring (as classes com `@Component`/`@Service`,
injetadas), por métodos `@Bean` numa configuração, pela montagem à mão num lugar só (o `main` ou uma classe de
composição), por uma fábrica (um `switch`/`if` que só devolve o objeto, ou um
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
