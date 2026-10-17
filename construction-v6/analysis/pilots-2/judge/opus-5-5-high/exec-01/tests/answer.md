# testes

## Visão geral

O agente escreveu uma única classe de teste, `PricingServiceTest`, com seis métodos. O nome promete testar o serviço de preço, mas ela só testa a classe utilitária estática `PricingUtil`: o arredondamento, a porcentagem, a conversão de horas em dias e a soma, a subtração e a multiplicação com arredondamento. A classe `PricingService` não aparece em nenhum teste. Por isso nenhum requisito do enunciado tem teste no nível do negócio: nem o cálculo do resumo, nem as recusas, nem o ciclo do pedido, nem o pagamento repetido, nem a concorrência, nem a TransLog. Esse é o problema mais sério, de longe.

O PIT confirma a pouca abrangência (`pit.txt`, execução `exec-01`, `situation=ok`). São 6 testes, que executam 1% das linhas (`line_coverage 0.01`). Dos 334 erros plantados, pegaram 8 (`mutation_score 0.024`), e 326 ficaram em linhas que nenhum teste executa. O lado bom é que as poucas asserções conferem valores exatos, e todo erro plantado nas linhas executadas foi pego (`test_strength 1.000`, `survived 0`). Os testes também rodam sem banco, sem internet e sem depender do relógio. Mas isso só acontece porque não tocam em nada que use banco, rede ou tempo.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?** Os seis testes estão em `src/test/java/com/loja/orderservice/service/PricingServiceTest.java`, e todos chamam só a `PricingUtil` (o import fica na linha 3). Área por área:
- **O cálculo: não coberto.** Nenhum teste monta um resumo nem confere subtotal, desconto, frete, seguro ou total final. Nenhum dos dez exemplos do enunciado foi transformado em teste. O que existe são peças isoladas da conta:
  - o arredondamento meio para o par com o próprio exemplo do enunciado, 2,995 → 3,00 e 2,985 → 2,98 (`PricingServiceTest.java:13-20`);
  - as 41 horas que viram 2 dias (`PricingServiceTest.java:33-34`) e os casos exatos de 24 e 48 horas (`:36-40`);
  - uma porcentagem de 10% sobre 100,00 (`:24-28`).
  
  A `PricingService.calculateSummary` (`src/main/java/com/loja/orderservice/service/PricingService.java:27`), onde estão as regras de cupom, frete, seguro, clube e pagamento, nunca é chamada. Funções que essas regras usam ficaram sem teste, como `PricingUtil.divide` (`src/main/java/com/loja/orderservice/util/PricingUtil.java:28`, usada na parcela em `PricingService.java:267`). A porcentagem fracionária do seguro (1,5%, 2,5%) também não é testada.
- **As recusas: não coberto.** Nenhum teste confere um código de erro. `ValidationService` e `OrderException` não aparecem no `src/test`.
- **O ciclo do pedido: não coberto.** Não há teste de `OrderService` nem dos controllers, nem das transições permitidas ou proibidas.
- **O pagamento repetido: não coberto.**
- **A concorrência: não coberto.**
- **A transportadora: não coberto.** `TransLogClient` não tem teste nem dublê, e não há teste da TransLog fora do ar nem do `ZIP_NOT_SERVED`.

Das seis áreas, nenhuma tem teste. O pouco que existe testa ferramentas aritméticas. Os números do PIT batem com isso: 326 dos 334 erros plantados ficaram em linhas que nenhum teste executa.
Conclusão: não segue

**T2, as asserções conferem valores?** Neste ponto, o pouco que existe está bem feito. As seis asserções usam `assertEquals` com o valor exato esperado (`PricingServiceTest.java:16, 20, 28, 34, 37, 40, 48, 56, 64`). Nenhuma confere só que não deu exceção, que algo não é nulo ou que o status é 200. Comparar com `new BigDecimal("3.00")` também confere a escala de 2 casas, porque `BigDecimal.equals` leva a escala em conta. Dois dos casos são regras do enunciado em si: o arredondamento meio para o par e as horas arredondadas para cima. O PIT confirma: `test_strength 1.000` e `survived 0`, ou seja, todo erro plantado numa linha executada pelos testes foi pego.

Há uma ressalva: os valores conferidos são de funções auxiliares, não de resultados do negócio como o total de um pedido, a situação depois de uma operação ou o código de uma recusa. Além disso, alguns casos não exercitam o arredondamento que a função promete. Em `testAdd`, `testSubtract` e `testMultiply` (`:44-64`), as entradas já têm 2 casas e o resultado não exige arredondar. Essa falta de alcance já foi contada no T1. Aqui, a pergunta é se as asserções que existem conferem valores, e elas conferem.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim. Os seis testes são de unidade puros, sobre métodos estáticos, sem contexto do Spring (não há `@SpringBootTest` nem `@ActiveProfiles` no `src/test`). Não usam banco, Docker ou rede. O agente chegou a preparar um H2 em memória para testes, mas nenhum teste usa essa configuração:
- `src/test/resources/application-test.properties:1-7` aponta para `jdbc:h2:mem:testdb`;
- o H2 entra com escopo de teste em `pom.xml:58-62`;
- `src/test/resources/application-test.properties:10` aponta a TransLog para `http://localhost:8888`, sem nenhum servidor falso que responda ali.

Não há dependência de PostgreSQL, Testcontainers ou da TransLog de verdade. Isso cumpre o que o enunciado pede, mas sem mérito: nada que use banco ou rede é testado. Por isso o `mvn verify` também não confere se o contexto da aplicação sobe.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Não. Os testes não usam relógio, `sleep`, estado compartilhado nem ordem de execução. Cada um monta seus próprios valores e chama uma função pura (`PricingServiceTest.java:13-65`). Fica uma observação sobre o código principal, que não pesa neste item. O registro do pedido carimba a hora com `Instant.now()` direto na entidade (`src/main/java/com/loja/orderservice/entity/Order.java:73`), sem um `Clock` que um teste possa controlar. O prazo de 48 horas é conferido contra essa hora (`src/main/java/com/loja/orderservice/service/OrderService.java:96-97`). Um teste futuro do caso de exatamente 48 horas teria de contornar isso, por exemplo passando um `paidAt` calculado a partir do `createdAt` que o banco devolveu. Hoje, nenhum teste esbarra nisso.
Conclusão: segue

## O que mudaria primeiro

1. **Testes de unidade da `PricingService` com os dez exemplos do enunciado.** A `ProductRepository` e a `TransLogClient` seriam trocadas por dublês que devolvem o catálogo e as respostas da TransLog de cada exemplo. Cada teste conferiria os onze campos do resumo e, nos exemplos 8 a 10, o código da recusa. É a mudança que mais aumentaria a nota do PIT, porque `PricingService` concentra a maior parte das regras. Também cobriria de uma vez o cálculo, parte das recusas e a TransLog sem atender o CEP. Bastaria mais um caso de dublê que lança erro para cobrir a TransLog fora do ar.
2. **Testes da `OrderService` com repositórios falsos ou H2.** O H2 já está no `pom.xml` e configurado em `application-test.properties`, mas nunca foi usado. Esses testes cobririam as transições permitidas e proibidas, o pagamento com 48 horas exatas e com mais de 48, e o mesmo `transactionId` repetido sem baixar o estoque nem dar o crédito duas vezes. São as regras em que um erro custa dinheiro: estoque vendido sem existir e crédito em dobro.

## Dúvidas

- **No T2, olhei só a qualidade das asserções que existem.** A régua diz que o bom é conferir "os resultados do negócio". As asserções conferem valores exatos, mas de funções auxiliares, e só duas delas correspondem a regras escritas no enunciado. Concluí `segue` porque nenhuma asserção é fraca, e o PIT confirma isso com `test_strength 1.000`. O pouco alcance ficou no T1, para não contar a mesma falha duas vezes. Quem preferir ler "resultados do negócio" ao pé da letra concluiria `em parte`.
- **O T3 e o T4 ficam satisfeitos sem mérito.** Os testes rodam sem banco e sem relógio porque não testam nada que dependa deles. Concluí `segue` porque a pergunta é se os testes que existem dependem dessas coisas, e eles não dependem.
