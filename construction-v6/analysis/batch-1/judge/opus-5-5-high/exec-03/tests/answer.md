# testes

## Visão geral

O agente escreveu só seis testes de unidade, em duas classes: `PricingServiceTest` (4) e `RoundingUtilTest` (2). Não há nenhum teste de Spring, de controller, de repositório nem do `OrderService`. O ponto forte é que esses testes são puros, determinísticos e rodam sem banco nem rede. O arredondamento "meio para o par" e o prazo em dias da TransLog são conferidos com valores exatos, e os dois exemplos do enunciado (2,995 e 2,985) aparecem no teste. Os problemas mais sérios, em ordem:

1. **Quase nada do enunciado está coberto.** O PIT mede 17% de cobertura de linhas e pega 39 dos 307 erros plantados (`mutation_score` 0,127). Desses 307, 257 caem em linhas que nenhum teste executa. Recusas, ciclo do pedido, pagamento repetido, concorrência e TransLog ficam todos sem teste.
2. **O cálculo também fica pela metade.** O total final, o seguro, o ajuste do pagamento e a parcela nunca são conferidos por valor, e nenhum dos sete exemplos do financeiro é reproduzido.
3. **Dependências e configuração de teste que ninguém usa.** O `pom.xml` traz Testcontainers e há um `application-test.yml` com H2, mas nada disso é usado.

O pouco que é executado é bem conferido (`test_strength` 0,780: 39 dos 50 erros executados são pegos). A falta de testes pesa muito mais que a fraqueza das asserções.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?** Área por área:

- **O cálculo: em parte.**
  - `PricingServiceTest.java:33-35` confere o subtotal (159.80), o desconto (0.00) e o frete (0.00) de um pedido simples com retirada na loja.
  - `PricingServiceTest.java:105` confere o frete zerado do GOLD.
  - O seguro, o ajuste da forma de pagamento, o total final (`:37` só faz `assertNotNull`) e o valor da parcela nunca são conferidos.
  - Nenhum dos exemplos 1 a 7 do enunciado está nos testes: o teste do GOLD (`:86-108`) usa os itens do exemplo 7, mas frete 50.00 inventado, região SOUTHEAST, e não confere o total.
  - A regra dos 30% de limite do cupom (`PricingService.java:108-112`), o BUY3PAY2, o MINUS50 e o FREESHIPPING não têm teste.
  - Os juros da tabela Price (`PricingService.java:166-179`) não têm teste.
- **As recusas: não cobertas.** Nenhum teste confere um código de erro. `validateCoupon` (`OrderService.java:357-385`), que levanta `COUPON_NOT_APPLICABLE`, nunca é executado. O único teste próximo, `shouldNotApplyWelcomeCouponForRepeatingCustomer` (`PricingServiceTest.java:64-83`), confere que o desconto vem 0.00, e não a recusa que o exemplo 9 pede.
- **O ciclo do pedido: não coberto.** Não há teste de `Order` nem de `OrderService` (pagar, enviar, entregar, cancelar, transições proibidas, 48 horas).
- **O pagamento repetido: não coberto.** O desvio do `transactionId` igual (`OrderService.java:192-194`) não é executado por nenhum teste.
- **A concorrência: não coberta.** Os `@Lock(PESSIMISTIC_WRITE)` dos repositórios (`ProductRepository.java:10`, `OrderRepository.java:11`) não são exercitados.
- **A transportadora: não coberta.** Nenhum teste toca o `TransLogClient`, nem para a TransLog fora do ar nem para o `ZIP_NOT_SERVED`. O que se testa dela é só a conversão de horas em dias (`RoundingUtilTest.java:19-23`).

De seis áreas, uma está coberta em parte e cinco não estão. Os números do PIT confirmam: 257 dos 307 erros plantados (84%) ficam em linhas sem teste.
Conclusão: não segue

**T2, as asserções conferem valores?**

- **O lado bom.** Em geral, sim: são `assertEquals` sobre `BigDecimal` com escala e valor exatos.
  - `RoundingUtilTest.java:11-14` confere quatro arredondamentos, inclusive os dois do enunciado.
  - `RoundingUtilTest.java:19-23` confere cinco conversões de horas em dias.
  - `PricingServiceTest.java:33-35`, `:82` e `:105` conferem valores do resumo.
- **As asserções fracas ou vazias.** Das 12 asserções do `PricingServiceTest`:
  - `:37` só confere que o total final não é nulo. Aqui o Pix sem cupom dá 5% de desconto, e um erro nessa conta passa.
  - `:60` e `:106` só conferem que o desconto e o crédito são maiores que zero. Um WELCOME10 de 5% ou um crédito GOLD de 2% passariam.
  - `:36` e `:38` conferem o prazo e as parcelas, mas os dois só ecoam o que o próprio teste passou como argumento (`deliveryDays` 1 em `:25`, `installments` 1 em `:28`). Não testam regra nenhuma.
  - `:82` confere um valor exato, mas fixa como certo um comportamento que o enunciado não pede (desconto zero no lugar da recusa).
- **A proporção.** Somando as duas classes, 21 asserções. 3 são fracas (`:37`, `:60`, `:106`) e 2 são tautológicas (`:36`, `:38`). As outras 16 conferem um resultado de negócio ou de arredondamento.
- **O PIT.** O `test_strength` de 0,780 é razoável: as asserções pegam a maioria dos erros nas linhas que executam. As 11 mutações que sobreviveram são compatíveis com as asserções fracas acima. Os valores mais importantes do resumo (total final, seguro, ajuste, parcela) não são conferidos em nenhum lugar. Isso esconde, por exemplo, um erro na taxa do seguro: ela vem de um `double` (`Region.java:6-12`, `PricingService.java:139-141`) e nenhuma asserção a confere.

Conclusão: em parte

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?**

- **Rodam.** Os seis testes instanciam as classes direto (`new PricingService()` em `PricingServiceTest.java:12`, métodos estáticos em `RoundingUtilTest`). Não há `@SpringBootTest`, `@ActiveProfiles`, `@Testcontainers`, `@Container`, `@MockBean` nem servidor falso (busca em `src/test` sem resultado). Nenhum teste precisa de PostgreSQL, Docker ou da TransLog real, e o PIT rodou com `situation` `ok`.
- **Mas foi por evitar, não por resolver.** Não há nenhuma estratégia de banco ou de transportadora que funcione:
  - O `pom.xml` declara `spring-boot-testcontainers` e `org.testcontainers:postgresql` com escopo de teste, mas nenhum teste usa.
  - `src/test/resources/application-test.yml` configura H2, Flyway e `translog.url: http://localhost:9999`, mas nenhum teste ativa o perfil `test`.
  - Se alguém escrever um teste de integração seguindo a configuração que já está ali, ninguém sabe se ele roda: as migrações escritas para PostgreSQL rodariam no H2, e a TransLog apontaria para uma porta sem servidor. Isso é uma dúvida, não algo verificado.

Hoje o item é cumprido.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?**

- **Nos testes, não.** Os seis testes não usam relógio, `sleep`, ordem de execução nem estado compartilhado. Cada um monta a sua entrada, e o `PricingService` não guarda estado (o campo em `PricingServiceTest.java:12` é só a instância sem estado).
- **Mas o código das 48 horas não é testável com tempo controlado.** A regra usa `Instant.now()` direto (`Order.java:195` e `:199`), e a data do pedido também (`Order.java:87`). Não há `Clock` injetado. Hoje não há teste das 48 horas para depender disso, mas um teste do limite exato (48 h aceita, 48 h e 1 s recusa) teria de usar o relógio real ou mexer no `createdAt` por fora.

Pelos testes que existem, o item é cumprido.
Conclusão: segue

## O que mudaria primeiro

1. **Testar o `OrderService` com repositórios falsos ou H2 e um dublê do `TransLogClient`.** Em especial a ordem das recusas, as transições do pedido (inclusive as proibidas), o pagamento repetido com o mesmo `transactionId` e o cancelamento de um pedido pago (estoque e crédito voltando). É ali que estão os 257 erros plantados sem cobertura e as regras que o enunciado mais destaca.
2. **Transformar os exemplos 1 a 7 do financeiro em um teste parametrizado do `PricingService`** que confira todos os campos do resumo, inclusive o total final, o seguro, o ajuste e a parcela. Ao mesmo tempo, trocar os `assertNotNull` e `> 0` por valores exatos e injetar um `Clock` para poder testar o limite das 48 horas.

## Dúvidas

- **T1, "não segue" ou "em parte".** Há cobertura parcial de uma das seis áreas (o cálculo, sem o total final). Escolhi "não segue" porque as outras cinco não têm teste nenhum e nem a área coberta confere o valor final. Uma leitura mais generosa daria "em parte".
- **T3 e T4, "segue" por omissão.** Os dois são cumpridos porque os testes não chegam ao banco, à TransLog nem ao relógio, e não porque o agente os tenha isolado de verdade. Registrei a conclusão pelo que os testes fazem hoje e deixei a ressalva no texto.
