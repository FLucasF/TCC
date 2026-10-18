Já li o enunciado, o projeto e os números do PIT. O projeto tem uma única classe de teste com dois métodos, então segue a correção.

# Testes

## Visão geral

O agente escreveu um único arquivo de teste, `src/test/java/com/loja/MoneyTest.java`, com dois métodos. Os dois testam só a classe `Money` (o arredondamento "meio para o par" e as quatro operações básicas). Os números do PIT confirmam isso (`pit.txt`, exec-02, `situation` ok):

- 2 testes;
- 1% das linhas do código principal executadas;
- 452 erros plantados, dos quais 443 ficam em linhas que nenhum teste executa;
- 7 erros pegos, ou seja, `mutation_score` 0,015.

O ponto forte é pequeno: o pouco que existe confere valores exatos e roda sem banco, sem internet e sem relógio. O problema mais sério é a cobertura. Nenhuma das seis áreas de negócio do enunciado é testada: o cálculo do resumo, as recusas, o ciclo do pedido, o pagamento repetido, a concorrência e a TransLog. Os testes conferem um tijolo da conta, não a conta.

## Item a item

**T1, o que dos requisitos os testes cobrem.** Área por área:
- **O cálculo:** não está coberto. `MoneyTest.java:11-14` confere o arredondamento que o enunciado exige (2,995 vira 3,00 e 2,985 vira 2,98), e `MoneyTest.java:17-24` confere soma, subtração e multiplicação. Mas nenhum teste chama o `CheckoutService` nem confere subtotal, desconto, frete, seguro ou total final. Nenhum dos dez exemplos do enunciado vira teste.
- **As recusas:** não estão cobertas. Nenhum teste confere um código de erro.
- **O ciclo do pedido:** não está coberto. Nenhum teste chama o `OrderService`.
- **O pagamento repetido:** não está coberto.
- **A concorrência:** não está coberta.
- **A TransLog:** não está coberta. Nenhum teste usa o `TransLogClient` ou as estratégias de frete.

As 443 mutações em linhas que nenhum teste executa (`pit.txt`) mostram que fica de fora praticamente todo o código, menos o `Money`.
Conclusão: não segue

**T2, as asserções conferem valores.** Sim, nos dois testes. Todas as cinco asserções são `assertEquals` contra um valor exato de dinheiro, por exemplo `assertEquals(Money.of(3.00), Money.of(2.995))` em `MoneyTest.java:12` e `assertEquals(Money.of(30.00), a.multiply(3))` em `MoneyTest.java:23`. Nenhuma se limita a conferir que não deu exceção ou que algo não é nulo. O PIT concorda: a `test_strength` é 0,778, isto é, dos 9 erros plantados nas linhas que os testes executam, eles pegam 7. Ressalva: a qualidade é boa, mas a amostra é minúscula. Não há asserção nenhuma sobre um resultado de negócio (um total, a situação de um pedido, um código de recusa), porque nada disso é testado.
Conclusão: em parte

**T3, rodam sem banco e sem internet.** Sim. `MoneyTest` é um teste de unidade puro sobre um objeto de valor, sem nenhum recurso externo. O `pom.xml` só traz `junit-jupiter` como dependência de teste (`pom.xml:53-58`), sem Testcontainers nem driver de banco em memória. Nenhum teste precisa de PostgreSQL, Docker ou da TransLog de verdade. Isso vale porque os testes nunca chegam aos repositórios (`repositories/impl/*`) nem ao `TransLogClient`, não porque o agente tenha montado dublês: não existe banco em memória, servidor falso nem implementação falsa das interfaces.
Conclusão: segue

**T4, dependem de coisas que mudam sozinhas.** Os testes não dependem de nada disso. Não usam relógio, `sleep` ou estado compartilhado, e cada método cria os seus próprios objetos (`MoneyTest.java:18-19`). Fica uma observação sobre o código principal: o registro do pedido usa `Instant.now()` direto, sem um relógio que se possa trocar (`src/main/java/com/loja/services/OrderService.java:52`). Por isso um teste da regra das 48 horas, se existisse, dependeria do relógio de verdade ou de montar o pedido por fora. Esse teste não existe, então o problema não aparece nos testes de hoje.
Conclusão: segue

## O que mudaria primeiro

1. **Transformar os dez exemplos do enunciado em testes do cálculo.** Seriam testes parametrizados sobre o `CheckoutService`, com repositórios em memória e um dublê do `ShippingStrategy`/`TransLogClient` com a resposta de cada exemplo. Cada teste conferiria subtotal, cupom, frete, prazo, seguro, ajuste, total final e crédito. Isso cobriria de uma vez o cálculo, a TransLog (exemplo 10, `ZIP_NOT_SERVED`) e parte das recusas, e é onde está a maioria dos 443 erros plantados que hoje ninguém executa.
2. **Testar o ciclo do pedido no `OrderService`.** Os testes cobririam as transições permitidas e proibidas, o pagamento repetido (o estoque e o crédito baixados uma vez só) e o limite de 48 horas (exatamente 48h aceita, 48h e 1s recusa). Para o limite de 48 horas ser testável sem o relógio real, antes é preciso trocar o `Instant.now()` de `OrderService.java:52` por um `Clock` injetado.

## Dúvidas

No T2, a régua pergunta se as asserções conferem valores. As poucas que existem conferem, mas nenhuma é sobre um resultado de negócio. Escolhi "em parte" em vez de "segue" porque a régua define o bom como "conferir os resultados do negócio", e essa parte o projeto não tem.

No T3 e no T4, os testes cumprem a regra, mas só porque são poucos e triviais. Mesmo assim marquei "segue", porque a pergunta é sobre o que os testes existentes fazem; a cobertura já pesa no T1.
