Li o enunciado, os números do PIT, os três arquivos de teste, o `pom.xml` e o código principal que eles exercitam. Segue a correção.

# Testes

## Visão geral

O agente escreveu 10 testes em três classes:
- 5 testes de unidade de `CurrencyUtils`;
- 4 testes de unidade de `CheckoutService`, com o repositório e a TransLog simulados pelo Mockito;
- um `contextLoads` que sobe o Spring com H2.

O ponto forte é que tudo roda sem PostgreSQL, sem Docker e sem internet, e cada teste monta o seu próprio estado. O problema mais sério é o pouco que eles alcançam. Nada testa o pedido (`OrderService`): nem o ciclo de situações, nem o pagamento repetido, nem o estoque, nem a concorrência. A TransLog é simulada, mas nunca é configurada nem chamada. Nenhum dos sete exemplos do enunciado aparece num teste. O segundo problema é que as recusas são conferidas só pelo tipo da exceção, nunca pelo código de erro. O único teste de resumo com sucesso não confere o seguro, o ajuste nem o total final.

Os números do PIT (`pit.txt`, `exec-02`) dizem a mesma coisa:
- `situation` é `ok`, com `tests` 10;
- `line_coverage` é 0,32;
- `mutation_score` é 0,086: 95 erros pegos de 1111 plantados;
- 994 dos 1111 erros caem em linhas que nenhum teste executa (`no_coverage`).

A `test_strength` de 0,812 mostra que, onde os testes passam, eles costumam pegar o erro. A nota baixa vem sobretudo do que não é testado.

## Item a item

**T1, o que dos requisitos os testes cobrem.**
- **O cálculo, em parte.** `CurrencyUtilsTest.java:10-14` confere o arredondamento meio para o par com os dois exemplos do enunciado (2,995 → 3,00 e 2,985 → 2,98). `CheckoutServiceTest.java:76-83` monta um resumo simples: uma camiseta, retirada na loja, Pix, BRONZE. Confere o subtotal (79,90), o desconto zero, o frete zero e o prazo 1. Não confere o seguro, o ajuste do Pix nem o total final: só verifica que o total é maior que zero (`:83`). Não há teste de cupom, de seguro com limite, de juros do cartão, de boleto, de crédito do clube nem de brinde. Nenhum dos exemplos 1 a 7 do enunciado é reproduzido.
- **As recusas, não.** Três testes passam por recusas: carrinho vazio (`:41-50`), produto fora de linha (`:86-109`) e motoboy acima de 5 kg (`:111-134`). Os três fazem só `assertThrows(OrderServiceException.class, ...)`, sem olhar o `getErrorCode()` nem o HTTP. Nenhum teste confere uma recusa pelo código.
- **O ciclo do pedido, não.** Nenhum teste chama `OrderService` nem `OrderController` (`grep` em `src/test`).
- **O pagamento repetido, não.** Nenhum teste chama `processPayment`.
- **A concorrência, não.** Não existe teste com duas compras ao mesmo tempo.
- **A transportadora, não.** `TransLogClient` é um `@Mock` (`CheckoutServiceTest.java:29-30`), mas nenhum teste configura uma resposta nem usa ECONOMY ou EXPRESS. Não há teste da TransLog fora do ar nem do `ZIP_NOT_SERVED`. O `TransLogClient` real também não tem teste.

Pela leitura do código (sem rodar), as lacunas deixam passar erros que os exemplos do enunciado pegariam:
- o FREESHIPPING sempre recusa (`CheckoutService.java:227-228`), o que contradiz o exemplo 6;
- o GOLD não paga o frete também no motoboy (`CheckoutService.java:56-60`), o que contradiz o exemplo 3;
- o Pix dá o desconto mesmo com cupom (`CheckoutService.java:343-344`), o que contradiz o exemplo 1;
- o WELCOME10 não confere se é a primeira compra (`CheckoutService.java:220-221`), o que contradiz o exemplo 9;
- as horas truncadas na regra das 48 horas (`OrderService.java:86`) aceitam um pagamento com 48h59min.

Das seis áreas, uma está coberta em parte e cinco não estão cobertas.
Conclusão: não segue

**T2, as asserções conferem valores?** Varia muito entre as classes.
- `CurrencyUtilsTest` (5 testes) confere valores exatos com `assertEquals` em `BigDecimal`, incluindo a escala, por exemplo `CurrencyUtilsTest.java:19`. Isso é bom, mas são funções auxiliares, não resultados do negócio.
- Em `CheckoutServiceTest`, um teste confere quatro valores do resumo (`:79-82`). Ele termina com `assertTrue(summary.getFinalTotal().compareTo(BigDecimal.ZERO) > 0)` (`:83`), que aceita qualquer total positivo. A conta certa seria 79,90 + 0,80 de seguro − 4,04 do Pix = 76,66.
- Os outros três testes de `CheckoutServiceTest` só conferem que saiu uma `OrderServiceException` (`:49`, `:108`, `:133`). Um erro que troque `PRODUCT_DISCONTINUED` por outro código, ou o 422 por outro status, passa.
- `contextLoads` (`OrderServiceApplicationTest.java:10-12`) não tem nenhuma asserção.

Contando os 10 testes:
- 5 conferem valores, todos de utilitário;
- 1 confere valores do negócio pela metade;
- 3 conferem só a exceção;
- 1 não confere nada.

A `test_strength` de 0,812 mostra que os testes pegam a maior parte dos erros nas linhas que executam. Há uma diferença que não consigo explicar com certeza: a cobertura de linhas é 0,32, mas só 117 dos 1111 erros plantados (cerca de 10%) caem em linhas executadas. Uma leitura provável é que boa parte das linhas executadas vem do `contextLoads`, que roda a carga do catálogo em `DataInitializer` e a configuração sem conferir nada. Essas linhas têm poucos pontos onde se planta erro. Os números não decidem isso.
Conclusão: em parte

**T3, os testes rodam sem banco e sem internet?**
- O teste de contexto usa H2 em memória: `src/test/resources/application-test.yml:3-4`, `@ActiveProfiles("test")` em `OrderServiceApplicationTest.java:8` e a dependência `h2` com escopo `test` em `pom.xml:94-98`.
- Os testes de `CheckoutService` simulam o `ProductRepository` e o `TransLogClient` com Mockito (`CheckoutServiceTest.java:26-30`).
- Nada chama a TransLog de verdade. O `translog.url` de teste aponta para `localhost:8081` (`application-test.yml:14`), mas a subida do contexto não faz nenhuma cotação.
- O `pom.xml:80-92` declara Testcontainers (`testcontainers` e `postgresql`), mas nenhum teste usa (`grep` sem resultados em `src/test`). São só dependências sobrando, que não fazem nenhum teste precisar de Docker.
- A `situation` `ok` do PIT confirma que a suíte roda nesse ambiente.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?**
- Nenhum teste usa o relógio, `sleep` ou estado compartilhado.
- `CheckoutServiceTest` recria o serviço e os simulados a cada teste (`@BeforeEach`, `:35-39`).
- `CurrencyUtilsTest` só chama funções puras.
- `contextLoads` usa um H2 novo, com `create-drop`.
- Nada depende da ordem em que os testes rodam.

Isso se cumpre em parte porque os testes evitam tudo que envolve tempo. O código principal usa `Instant.now()` direto, sem um `Clock` que o teste possa controlar (`OrderService.java:51` e `:288`). A regra das 48 horas compara o `paidAt` recebido com esse `createdAt` (`OrderService.java:84-88`). Um teste futuro do caso "exatamente 48 horas" teria de calcular o `paidAt` a partir do relógio de verdade e não seria determinístico. Para os testes que existem, o item é cumprido.
Conclusão: segue

## O que mudaria primeiro

1. **Testar o resumo pelos exemplos do enunciado e as recusas pelo código.** Um teste parametrizado de `CheckoutService` com os exemplos 1 a 10, com a TransLog simulada respondendo os valores de cada exemplo, conferiria todos os campos do resumo. Isso tiraria o cálculo dos 994 erros não executados. Também pegaria os erros de cupom, de frete do GOLD e do Pix com cupom listados no T1. Nas recusas, o teste deveria conferir `getErrorCode()` e o status, não só o tipo da exceção.
2. **Testar o `OrderService` com um `Clock` injetado.** O teste deveria cobrir as transições permitidas e as proibidas, o limite exato das 48 horas, o aviso de pagamento repetido (o estoque e o crédito baixam uma vez só) e o cancelamento de um pedido pago (o estoque volta e o crédito sai). Hoje o ciclo do pedido, que é a parte de maior risco do enunciado, não tem nenhum teste.

## Dúvidas

- Considerei que o teste do resumo com retirada na loja cobre o cálculo "em parte". Ele confere o subtotal, o frete e o prazo, mas não o seguro, o ajuste nem o total, e não há outro caso de cálculo. Também considerei que o arredondamento em `CurrencyUtilsTest` conta como parte do cálculo, porque o enunciado dá exatamente esses exemplos.
- Não sei explicar com certeza a diferença entre a cobertura de linhas (0,32) e a fração de erros alcançados (cerca de 10%). A explicação pelo `contextLoads` e pela carga do catálogo é uma hipótese que os números não confirmam.
- Os erros do código citados no T1 vêm só da leitura, sem rodar o serviço. Usei-os só para mostrar o que uma cobertura melhor pegaria, não para julgar o comportamento.
