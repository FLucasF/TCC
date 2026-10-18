# Padrões: Adapter

## Visão geral

O agente separou a TransLog numa classe só dela, `TransLogClient` (`src/main/java/com/loja/order/service/TransLogClient.java`). Essa classe monta a URL `/v2/quote`, faz a chamada HTTP com timeout de 2 s, lê `price_cents`, `eta_hours` e o `error`, e devolve um `QuoteResponse(price, etaDays)` já em reais e em dias. Essa metade do adaptador está bem feita. A outra metade ficou de fora: quem chama a classe ainda fala a língua da TransLog. O cálculo do frete em `OrderService` escolhe o código de serviço dela (`"STD"`/`"EXP"`) e converte o peso de quilos para gramas antes de chamar a classe. Por isso a assinatura do cliente (`quote(String zipCode, int weightGrams, String service)`) já pede as unidades e os códigos da transportadora. Trocar de transportadora mexeria no `TransLogClient` e também no cálculo do frete. No controle negativo houve contenção: a retirada e o motoboy têm valor e prazo fixos escritos na própria regra, sem nenhuma porta de transportadora. Não achei estrutura sem necessidade perto deste pattern.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O lado da TransLog existe e é uma peça só:
- `TransLogClient.java:38-39` monta `"%s/v2/quote?zip=%s&weight_g=%d&service=%s"`.
- `:48-52` faz a chamada e trata um status diferente de 200 como fora do ar.
- `:56-57` trata o `error` como CEP não atendido.
- `:60-65` lê `price_cents` e `eta_hours`.
- `:67-71` converte centavos em reais.
- `:73` converte horas em dias, arredondando para cima.
- `:75` devolve `QuoteResponse(BigDecimal price, int etaDays)` (`:31`), que já está na nossa língua.

A resposta, portanto, está bem isolada.

O pedido não está isolado. O vocabulário vaza em três lugares:
- **Os códigos de serviço.** `OrderService.java:80` traduz a nossa entrega para o código da TransLog fora do adaptador: `String service = shippingMethod == ShippingMethod.ECONOMY ? "STD" : "EXP";`. Essa tradução é trabalho do adaptador.
- **A unidade de peso (gramas).** `OrderService.java:83` converte o peso para a unidade dela antes da chamada: `totalWeight.intValue() * 1000`. A régua chama isso de vazamento literalmente ("converter os quilos em gramas antes de chamar a transportadora"). De passagem: a conta corta a parte decimal dos quilos antes de multiplicar (1,8 kg vira 1000 g). Quem mede esse erro é a dimensão do comportamento, mas é o tipo de erro que o vazamento facilita.
- **A assinatura do adaptador.** O próprio contrato da classe é escrito no vocabulário dela, em `TransLogClient.java:33`: `quote(String zipCode, int weightGrams, String service)`. Ela recebe gramas e um código de serviço em texto, em vez do peso em quilos e da nossa `ShippingMethod`. É isso que obriga o `OrderService` a fazer as duas conversões acima.

Há ainda dois pontos menores, que não mudam a conclusão:
- `RoundingUtil.ceilDays(double hours)` (`src/main/java/com/loja/order/util/RoundingUtil.java:11-13`) é uma conversão de horas para dias declarada fora do adaptador. Nenhum código de produção a chama (só `RoundingUtilTest.java:19-23`), e o adaptador faz a conversão por conta própria em `:73`. É uma sobra, não um vazamento ativo.
- As exceções `ZipCodeNotServedException` e `TransLogUnavailableException`, aninhadas no cliente (`TransLogClient.java:81-82`), são capturadas pelo nome em `OrderService.java:88-91`. Elas estão do lado da TransLog e são traduzidas logo para os nossos códigos, então não as conto como vazamento (ver Dúvidas).

O lado da TransLog existe, mas o código de serviço e a unidade em gramas vazam para o cálculo do frete. Pela régua, é o caso de malfeito.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy estão em `OrderService.java:73-77`:
- `STORE_PICKUP`: `deliveryDays = 1`, com frete zero vindo do valor inicial em `:70`.
- `COURIER`: `shippingCost = new BigDecimal("18.00"); deliveryDays = 0`.

São valores fixos, internos, escritos na própria regra. Não há interface de transportadora, nem um "cliente" do motoboy, nem a retirada passando pelo `TransLogClient`, que só é chamado no ramo `else` (`:78-92`). O `if` sobre os casos de entrega pertence ao P1 (Strategy) e não é julgado aqui. Para este controle, a resposta tem o tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não há estrutura que ninguém pediu:
- o `TransLogClient` é uma classe concreta, sem uma interface de implementação única;
- não há camada que só repassa a chamada da transportadora;
- não há fábrica nem builder de clientes.

As únicas interfaces do projeto são os repositórios do Spring Data (`repository/*.java`). O `JacksonConfig` (`src/main/java/com/loja/order/config/JacksonConfig.java:10-15`) é configuração, não estrutura de padrão.
Conclusão: contenção

## O que mudaria primeiro

1. **Mudar o contrato do adaptador para a nossa língua.** Em vez de `quote(String zipCode, int weightGrams, String service)`, ele passaria a receber `quote(String zipCode, BigDecimal weightKg, ShippingMethod method)`. A tradução `ECONOMY -> "STD"`, `EXPRESS -> "EXP"` e a conversão para gramas iriam para dentro do `TransLogClient`. Assim o `OrderService` deixa de conhecer qualquer termo da TransLog, trocar de transportadora passa a mexer só nessa classe, e a conversão do peso fica num lugar só, onde é mais fácil acertá-la.
2. **Apagar o `RoundingUtil.ceilDays`.** É uma conversão de horas, a unidade da TransLog, solta num utilitário genérico e repetindo o que o adaptador já faz.

## Dúvidas

- **As exceções aninhadas no cliente.** `ZipCodeNotServedException` e `TransLogUnavailableException` são capturadas pelo nome fora do adaptador (`OrderService.java:88-91`), e o nome da primeira lembra o `ZIP_NOT_SERVED`. Li essas exceções como tipos do lado da TransLog, traduzidos logo para os nossos códigos (`SHIPPING_METHOD_UNAVAILABLE`, `CARRIER_UNAVAILABLE`). Elas não carregam campo, unidade nem código da API, então não as contei como vazamento. Uma leitura mais rígida as contaria, mas isso não mudaria a conclusão, que já é malfeito pelos dois vazamentos claros.
- **O `RoundingUtil.ceilDays`.** Não o contei como vazamento ativo porque nenhum código de produção o usa.
