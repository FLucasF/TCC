# testes

## Visão geral

O agente escreveu um único arquivo de teste, `src/test/java/com/shop/BasicTest.java`, com 4 métodos. Todos testam o `PaymentService`, a última etapa do cálculo: o ajuste da forma de pagamento. Os números do PIT (`pit.txt`, `exec-01`, situation `ok`) mostram o tamanho disso. A cobertura de linhas é 0,01. Dos 371 erros plantados, 6 foram pegos, 1 passou e 364 caíram em linhas que nenhum teste executa. O mutation score é 0,016 e o test strength é 0,857. O ponto forte é pequeno: nos três testes de cálculo, as asserções conferem valores exatos. Isso explica o test strength alto, já que quase tudo que os testes executam eles conferem. O problema mais sério é a cobertura. Ficam sem nenhum teste o resto do cálculo do resumo (subtotal, cupons, frete, seguro, crédito, brinde), todas as recusas, o ciclo do pedido, o pagamento repetido, a concorrência e a TransLog. Os casos de pagamento que mais tinham risco também ficaram de fora: Pix com cupom de desconto zero e cartão de 4 a 12 vezes. Os testes rodam sem banco nem internet e não dependem de relógio, mas só porque não chegam perto de nada que use essas coisas.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem.** Área por área:
- **O cálculo:** em parte, e só a última etapa. `testPixDiscount` (`BasicTest.java:20-32`) confere o desconto do Pix sobre um total de 100,00. `testBankSlipFee` (`:35-47`) confere a tarifa do boleto. `testCreditCardNoInterest` (`:50-63`) confere 3 vezes sem juros. Nenhum teste confere o subtotal, o desconto de cupom, o frete, o prazo, o seguro, o crédito do clube ou o brinde, nem algum dos exemplos do enunciado de ponta a ponta. O cartão com juros também fica sem teste. É justamente o caso que quebraria: `PaymentService.java:64` chama `onePlusRate.pow(-installments)`, e `BigDecimal.pow(int)` lança `ArithmeticException` com expoente negativo. O exemplo 2 do enunciado (6 vezes) falharia, e nenhum teste mostra isso. O caso "Pix com cupom de desconto zero não ganha os 5%" também não tem teste, e o código o trata ao contrário do enunciado (`PaymentService.java:37` só tira o desconto do Pix quando o cupom deu desconto maior que zero).
- **As recusas:** sem cobertura. Nenhum teste confere um código de erro.
- **O ciclo do pedido:** sem cobertura. Nenhum teste toca `OrderService` nem `OrderStatus`.
- **O pagamento repetido:** sem cobertura.
- **A concorrência:** sem cobertura.
- **A transportadora:** sem cobertura. `ShippingService` não aparece em nenhum teste.

Os números confirmam: 364 dos 371 mutantes ficaram em linhas que nenhum teste executa (`pit.txt`, `no_coverage`). Das seis áreas, só um pedaço de uma está coberto.
Conclusão: não segue

**T2, as asserções conferem valores.** Nos testes que existem, sim. Três dos quatro conferem resultados do negócio com valores exatos em `BigDecimal`: `finalTotal` igual a 95.00 e `adjustment` igual a -5.00 (`BasicTest.java:30-31`), 103.49 e 3.49 (`:45-46`), 300.00, 100.00 e 3 parcelas (`:60-62`). O quarto, `testPaymentServiceExists` (`:14-17`), só confere que um `new PaymentService()` não é nulo. Ele não testa nada que possa falhar. O PIT bate com essa leitura: test strength de 0,857, com 6 pegos e 1 sobrevivente entre os mutantes que os testes executam. Não é o padrão "cobre muito e confere pouco"; é o contrário, cobre quase nada e confere bem o que cobre. Os valores escolhidos são redondos (100,00 e 300,00), então nenhum teste força o arredondamento meio para o par. Proporção: 3 de 4 conferem valores, 1 é vazio.
Conclusão: em parte

**T3, os testes rodam sem banco e sem internet.** Sim. Os quatro testes instanciam `PaymentService` direto (`BasicTest.java:15, 21, 36, 51`), uma classe sem banco e sem HTTP (`PaymentService.java:1-75`). Nenhum teste usa PostgreSQL, Docker, Testcontainers ou a TransLog de verdade, e a medida do PIT rodou (`situation ok`). Duas ressalvas. Primeira: o H2 está declarado no escopo de teste (`pom.xml:65-70`), mas nenhum teste o usa, então o projeto não tem um jeito pronto de testar repositórios ou a TransLog sem a infraestrutura real. Segunda: o requisito é cumprido porque os testes não chegam a essas partes, não porque o agente montou dublês.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas.** Não. Nenhum teste usa relógio, `sleep` ou estado compartilhado. Cada método cria o seu `PaymentService` (`BasicTest.java:15, 21, 36, 51`), e a ordem de execução não importa. Uma observação sobre o código principal, que vale para quando houver testes do prazo de 48 horas: o horário de criação vem de `Instant.now()` direto no repositório (`OrderRepository.java:58`), sem um relógio que se possa injetar. A comparação usa `Duration.toHours() > 48` (`OrderService.java:106-107`), que trunca, e por isso 48h59min ainda seria aceito. Hoje nenhum teste depende disso.
Conclusão: segue

## O que mudaria primeiro

1. **Testes do cálculo do resumo com os exemplos do enunciado.** Seria um teste parametrizado do `CheckoutService` com a TransLog trocada por um dublê, conferindo os 11 campos de cada um dos 10 exemplos. Isso cobre a área com mais regras e na hora acusaria os dois defeitos que os testes atuais deixam passar: o cartão com juros que lança exceção e o desconto do Pix com cupom zerado.
2. **Testes do ciclo do pedido sobre o H2 que já está no `pom.xml`, com um relógio injetado.** Seriam as transições permitidas e proibidas, o limite de exatamente 48 horas, o pagamento repetido com o mesmo `transactionId` e a última peça disputada. São as regras que o enunciado diz já terem dado problema, e hoje nenhuma tem teste.

## Dúvidas

Em T3 e T4, os testes seguem a prática no sentido estrito da pergunta: não precisam de banco, internet nem relógio. Mas seguem só porque são poucos. Marquei "segue" porque a pergunta é se eles rodam sem essas dependências, e rodam. A falta de dublês e de relógio controlado entra nas ressalvas, e a cobertura baixa está contada em T1.
