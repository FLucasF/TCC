# testes

## Visão geral

O agente escreveu só 2 métodos de teste. `CheckoutServiceTest.testExample1` monta o cálculo de ponta a ponta para o Exemplo 1 do enunciado, com um H2 em memória e um dublê da TransLog. `MoneyUtilsTest.testHalfEvenRounding` confere o arredondamento "meio para o par". O PIT rodou (`pit.txt`: `situation=ok`, `tests=2`): a cobertura de linhas é 0,31 e a nota de mutação é 0,243 (100 erros pegos de 412). Dos 312 erros não pegos, 271 estão em linhas que nenhum teste executa (`no_coverage`) e 41 estão em linhas executadas (`survived`). O ponto forte é a qualidade das asserções que existem: comparam os onze campos do resumo com os valores do financeiro, e a força dos testes é 0,709. Também rodam sem banco e sem internet, como o enunciado pede. O problema mais sério é a cobertura: cinco das seis áreas pedidas não têm teste nenhum (as recusas, o ciclo do pedido, o pagamento repetido, a concorrência, a transportadora), e o cálculo é conferido em um só dos sete exemplos. O segundo problema: o dublê da TransLog substitui o método inteiro `quote` e repete por conta própria a regra dos dias a mais por região. Assim, o prazo e o frete que o teste confere vêm do dublê, não do código de produção.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**
- **O cálculo: coberto em parte.** `CheckoutServiceTest.java:58-91` confere o Exemplo 1: subtotal, cupom WELCOME10 (10%), frete, prazo, seguro com a porcentagem do Norte, ajuste zero no PIX com cupom, total, parcela, crédito do BRONZE e brinde. Os outros seis exemplos ficam sem teste. Por isso não são exercitados:
  - os juros do cartão (tabela Price);
  - a tarifa do boleto e o desconto do Pix;
  - o teto de 30% e o limite de R$ 30,00 do seguro;
  - os cupons MINUS50, FREESHIPPING e BUY3PAY2;
  - o frete grátis e o brinde do GOLD;
  - o crédito do SILVER e do GOLD.

  Duas ressalvas sobre o próprio Exemplo 1:
  - O teste chama `calculateSummary(request)` sem conexão (`CheckoutServiceTest.java:78`). Com a conexão nula, a produção trata a compra como primeira sem consultar nada (`CheckoutService.java:62`), então a regra da "primeira compra" do WELCOME10 não é exercitada.
  - O prazo 5 e o frete 33,10 saem do dublê (`CheckoutServiceTest.java:101`).
- **As recusas: não cobertas.** Nenhum teste confere um código de erro: não há `assertThrows` nem uso de `ApiException` em `src/test`. A validação de `CheckoutService.java:116-231` roda no caminho feliz, mas nenhuma das quinze recusas é provocada.
- **O ciclo do pedido: não coberto.** Nenhum teste usa `OrderService` (pagar, enviar, entregar, cancelar), nem as transições permitidas nem as proibidas.
- **O pagamento repetido: não coberto.** Nenhum teste chama `processPayment` (`OrderService.java:79`).
- **A concorrência: não coberta.** Nenhum teste disputa a última peça.
- **A transportadora: não coberta.** `MockTransLogClient` sobrescreve `quote` por inteiro (`CheckoutServiceTest.java:98-104`). Por isso nunca rodam:
  - a leitura do `price_cents` e do `eta_hours`;
  - a conversão de centavos;
  - o arredondamento das horas para cima;
  - o `ZIP_NOT_SERVED`;
  - o caminho "fora do ar" (`TransLogClient.java:45-69`).

  O dublê também nunca devolve `null` nem lança exceção, então `SHIPPING_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE` (`ShippingCalculator.java:107-113`) ficam sem teste.

Das seis áreas, só o cálculo tem algum teste, e só com um exemplo.
Conclusão: não segue

**T2, as asserções conferem valores?** Sim, todas as asserções escritas conferem valores do negócio:
- `testExample1` compara os onze campos do resumo com os números do financeiro, como `BigDecimal` com escala, por exemplo `assertEquals(new BigDecimal("412.07"), summary.getFinalTotal())` (`CheckoutServiceTest.java:80-90`).
- `MoneyUtilsTest.java:13-16` confere quatro arredondamentos exatos, inclusive os dois casos do enunciado (2,995 → 3,00 e 2,985 → 2,98).
- Não há asserções do tipo "não deu exceção", "é 200" ou "não é nulo".

Os números do PIT confirmam: a força dos testes é 0,709, ou seja, dos erros plantados em linhas executadas, a maioria é pega. A nota de mutação baixa (0,243) vem da pouca cobertura, não de asserções frouxas. A ressalva: 2 das 11 asserções do resumo, `deliveryDays` = 5 e `shippingCost` = 33,10, conferem um valor que o próprio dublê montou (`CheckoutServiceTest.java:101`, `2 + (region == Region.NORTH ? 3 : 0)`). Se a regra dos dias a mais em `TransLogClient.java:58-62` estiver errada, o teste continua passando. Os 41 erros que sobreviveram em linhas executadas devem estar em parte aí e na validação duplicada, que roda sem ser conferida. Não tenho o relatório por mutante para dizer quais são.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet?** Sim, e os dois lados estão resolvidos:
- O banco é um H2 em memória em modo PostgreSQL (`CheckoutServiceTest.java:30`, `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`). O H2 está com `scope test` no `pom.xml`.
- A TransLog é um dublê, uma subclasse de `TransLogClient` com o endereço `http://mock` (`CheckoutServiceTest.java:93-96`). O `HttpClient` é construído, mas nunca chamado.
- Não há PostgreSQL, Docker, Testcontainers nem chamada à TransLog de verdade.

Nota menor: o H2 em modo PostgreSQL não garante que o SQL próprio do PostgreSQL funcione. Isso fica fora do que este item pergunta.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Hoje, não:
- Nenhum teste usa o relógio: o único `Instant.now()` do projeto está em `OrderService.java:55`, que nenhum teste executa.
- Não há `sleep`, e não há dependência da ordem dos testes.
- Cada teste monta o seu estado no `@BeforeEach` (`CheckoutServiceTest.java:28-56`).

Duas fragilidades latentes, que ainda não afetam nada:
- O banco H2 tem nome fixo (`mem:test`) e `DB_CLOSE_DELAY=-1`. Cada `@BeforeEach` abre um pool novo de conexões sobre o mesmo banco e nunca o fecha. Além disso, `initializeCatalog` não faz nada quando já há produtos (`ProductRepository.java:21-23`). Se outro teste baixar o estoque ou gravar pedidos, esse estado passa para o teste seguinte.
- O registro do pedido usa `Instant.now()` sem um relógio que se possa trocar (`OrderService.java:55`). Um teste das 48 horas ficaria preso ao relógio de verdade.

Como só um teste usa o banco e nenhum chega no relógio, o problema ainda não acontece.
Conclusão: segue

## O que mudaria primeiro

1. **Testar o `OrderService`** com o H2 que já existe: pagamento, envio, entrega e cancelamento, as transições proibidas (409), o mesmo `transactionId` duas vezes (o estoque e o crédito baixam uma vez só) e as 48 horas no limite. Para as 48 horas, o código precisa receber um `Clock` em vez de chamar `Instant.now()`. É a parte de maior risco do enunciado e hoje está com cobertura zero.
2. **Trocar o dublê.** Em vez de sobrescrever `TransLogClient.quote`, subir um servidor HTTP falso local (ou injetar um `HttpClient` falso), para que a leitura do JSON, a conversão de centavos, o arredondamento das horas, os dias a mais por região, o `ZIP_NOT_SERVED` e o "fora do ar" sejam do código de produção. Junto, transformar `testExample1` num teste parametrizado com os dez exemplos do financeiro, inclusive as recusas 8 a 10, para cobrir as recusas e as regras de pagamento, cupom e clube.

## Dúvidas

- No T2, escolhi `segue` porque todas as asserções escritas conferem valores de negócio. O fato de duas delas conferirem números do dublê pesei no T1 (a transportadora sem cobertura), e não no T2.
- No T4, escolhi `segue` porque a régua pergunta do que os testes dependem *hoje*. O banco com nome compartilhado e o `Instant.now()` sem relógio injetável ficaram como ressalva, porque nenhum teste existente é afetado.
- Não tive o relatório do PIT por mutante, só os totais. A explicação de onde estão os 41 sobreviventes é uma leitura minha do código, não um dado.
