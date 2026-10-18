# Padrões: Adapter

## Visão geral

O agente juntou toda a conversa com a TransLog numa classe só, `application/service/TransLogService.java`. Ela monta a rota `/v2/quote`, traduz `ECONOMY`/`EXPRESS` em `STD`/`EXP`, passa os quilos para gramas, lê `price_cents` e `eta_hours` e devolve um `ShippingQuote` com o frete em reais e o prazo em dias. Nenhum termo, campo ou unidade da TransLog aparece fora dessa classe, e é isso que o ponto mede. As entregas sem transportadora (retirada e motoboy) ficaram como valores fixos dentro da regra de frete, sem porta nem "cliente" falso: é contenção.

O isolamento do vocabulário está certo, mas a peça tem três defeitos de desenho, do mais grave ao mais leve:

1. **As duas falhas da TransLog viram uma só.** O adaptador devolve `null` tanto para "CEP não atendido" quanto para "site fora do ar", e quem chama transforma isso sempre em `SHIPPING_METHOD_UNAVAILABLE`. A `CarrierUnavailableException` existe, mas nada a lança.
2. **Não há interface de transportadora.** As duas classes que chamam dependem direto da classe concreta da TransLog.
3. **A dependência está invertida.** O tipo que o adaptador devolve é uma classe aninhada da calculadora, `OrderSummaryCalculator.ShippingQuote`. Ou seja, o adaptador depende de quem o usa.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O enunciado desta execução traz o vocabulário em inglês da TransLog (`prompt.md`, seção "A TransLog"), então o ponto se aplica. O lado da TransLog é o arquivo `application/service/TransLogService.java`, o único que faz a chamada HTTP. Os outros arquivos da pasta são serviços nossos. Tudo o que é dela fica ali dentro:

- **O código de serviço:** `"STD"`/`"EXP"` a partir da nossa entrega, na linha 31. Essa tradução é trabalho do adaptador.
- **A unidade de peso:** a conversão para gramas, `Math.round(weightKg.doubleValue() * 1000)`, na linha 32.
- **A rota e os parâmetros:** `/v2/quote?zip=…&weight_g=…&service=…`, na linha 34.
- **A resposta de erro:** a leitura de `error` (o `ZIP_NOT_SERVED`), na linha 52.
- **Os campos da resposta:** `price_cents` e `eta_hours`, e as variáveis `priceCents`/`etaHours`, nas linhas 56 e 57.
- **As outras unidades:** a conversão de centavos para reais (linha 59) e de horas para dias com arredondamento para cima (linha 60).

Quem chama fala só a nossa língua:

- `OrderSummaryCalculator.java:148` e `OrderService.java:173-177` chamam `getQuote(zipCode, peso em kg, ShippingMethod)` e recebem `cost`/`days` (`OrderSummaryCalculator.java:308-316`).
- Os dias a mais do Norte e do Nordeste são somados sobre os dias já traduzidos (`OrderSummaryCalculator.java:165, 172-184`). Essa é uma regra nossa, não da TransLog.
- Um `grep` por `cents`, `hours`, `weight_g`, `"STD"`, `"EXP"`, `ZIP_NOT_SERVED`, `/ 24` e `* 1000` no código principal, fora do adaptador, só encontra `48 * 60 * 60 * 1000` (`OrderService.java:25`). Esse é o prazo de 48 horas do pagamento, em milissegundos, e não tem relação com a TransLog.

Não há vazamento. Ficam três ressalvas, que não mudam a conclusão porque o ponto mede o isolamento do vocabulário:

1. **A tradução das falhas perde informação.** As duas respostas de falha da TransLog (o `ZIP_NOT_SERVED` na linha 52 e o "fora do ar" nas linhas 45 e 63) viram o mesmo `null`. Quem chama converte esse `null` sempre em `ShippingMethodUnavailableException` (`OrderService.java:178-179`), e a `CarrierUnavailableException` (`application/exception/CarrierUnavailableException.java`) não é lançada em lugar nenhum. O adaptador não traduz "fora do ar" para a nossa língua.
2. **A peça não tem uma porta (interface) própria.** `OrderSummaryCalculator.java:16` e `OrderService.java:23` dependem da classe concreta. Além disso, o tipo de retorno mora dentro da calculadora (`TransLogService.java:29`, `OrderSummaryCalculator.java:308`). Trocar de transportadora mexeria nessas duas classes, embora só no nome do tipo, não no vocabulário.
3. **Uma entrega desconhecida vira `EXP` sem aviso.** O ternário da linha 31 manda para `EXP` qualquer entrega que não seja `ECONOMY`.

Conclusão: acerto

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são valores fixos dentro da própria regra de frete:

- **A retirada:** `new ShippingQuote(BigDecimal.ZERO, 1)` em `OrderSummaryCalculator.java:151`.
- **O motoboy:** `new ShippingQuote(new BigDecimal("18"), 0)` em `OrderSummaryCalculator.java:158`, com o limite de 5 kg na linha 155. O mesmo limite aparece repetido em `ValidationService.java:89-92`.

Nenhuma das duas passa pela `TransLogService`. Também não existe interface de transportadora, "cliente" do motoboy nem adaptador para devolver um número fixo. O `ShippingQuote` reaproveitado é só um par valor/prazo, não uma porta de transportadora. A forma como essas entregas são escolhidas (um `switch` sobre `ShippingMethod`, nas linhas 145-161) é assunto do Strategy (P1), não deste ponto.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** Perto da integração com a TransLog não há estrutura que ninguém pediu: nenhuma interface com uma implementação só, nenhuma camada que só repassa, nenhuma fábrica de clientes HTTP. As únicas interfaces do projeto são os repositórios do Spring Data (`domain/repository/*.java`). `ValidationService` e `CouponValidator` são criados com `new` dentro de `OrderService.java:39-40`, sem nenhuma camada extra. O problema aqui é o contrário: falta uma porta. Isso está registrado no P6, não aqui.

Conclusão: contenção

## O que mudaria primeiro

1. **Fazer o adaptador devolver um resultado nosso que separe as falhas.** Por exemplo, uma cotação, um "não atende este CEP" e um "transportadora indisponível", ou exceções do nosso domínio, no lugar de `null`. Hoje a falha de rede e o CEP não atendido chegam iguais a quem chama, e o `CARRIER_UNAVAILABLE` nunca sai.
2. **Dar uma porta própria à transportadora.** Uma interface com uma operação de cotar (CEP, peso em kg, entrega) que devolve frete em reais e prazo em dias, num tipo que pertença a essa porta e não à calculadora. Assim a `TransLogService` passa a ser só uma implementação, e trocar de transportadora deixa de mexer em `OrderSummaryCalculator` e `OrderService`.

## Dúvidas

- **Até onde vai o "lado da TransLog".** Ele não tem pasta própria: a `TransLogService` divide `application/service/` com os serviços nossos. Considerei como lado da TransLog só esse arquivo, que é o único que faz a chamada HTTP e o único que conhece o formato dela.
- **Como tratar a perda da distinção entre as falhas.** Contei isso como ressalva, não como vazamento, porque nenhum termo da TransLog sai do adaptador. Se a régua entendesse que uma tradução incompleta das falhas é cobertura parcial da peça, a leitura poderia pender para `malfeito`. Fiquei com `acerto` porque a régua diz explicitamente que este ponto mede o isolamento do vocabulário.
