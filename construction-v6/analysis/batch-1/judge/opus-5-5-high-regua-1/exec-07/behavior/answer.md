# comportamento

## Visão geral

O serviço não sobe. Com o banco vazio, o jar compila, mas o Spring desiste cerca de dois segundos depois de começar: a configuração pede um dialeto do Hibernate que não existe na versão que o Spring Boot 4.1.1 traz (Hibernate 7.4.5). O banco fica sem nenhuma tabela, como mostra a foto (`database/database-schema.json`, `"tables": []`). Como a regra manda, os itens C2 a C9 ficam como `não se aplica`. Só para diagnóstico, que não entra na nota, troquei o dialeto pela linha de comando. O serviço caiu de novo, agora num erro de mapeamento entre o pedido e os itens. São pelo menos duas falhas independentes, cada uma suficiente para impedir a subida. O `mvn -q verify` passa, mas roda um único teste, que não carrega o contexto do Spring. Por isso nenhuma das duas falhas aparece no build.

Li também o código, mas sem testar nada. A leitura mostra outros problemas que o teste encontraria se o serviço subisse. Eles não entram nas conclusões e estão listados em "O que mudaria primeiro".

## Item a item

**C1, o serviço sobe.** Copiei o `workspace/` para outra pasta e compilei com `mvn -q package -DskipTests` (o jar foi gerado). Subi com `TRANSLOG_URL=http://localhost:9090 java -jar target/shop-orders-service-1.0.0.jar --server.port=8081`, apontando para o banco vazio das variáveis `DB_*`. O processo começou às 05:08:41.321 e às 05:08:43.411 registrou `Application run failed` e encerrou. O erro no log:
`Unable to resolve name [org.hibernate.dialect.PostgreSQL12Dialect] as strategy [org.hibernate.dialect.Dialect]` … `ClassNotFoundException: Could not load requested class: org.hibernate.dialect.PostgreSQL12Dialect`. A causa está em `src/main/resources/application.properties:9` (`spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL12Dialect`).

No teste de diagnóstico, que não conta para a conclusão, subi o mesmo jar com `--spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`. Ele caiu outra vez: `DuplicateMappingException: Table [order_items] contains physical column name [order_id] referred to by multiple logical column names: [order_id], [orderId]`. O conflito é entre o campo `orderId` de `OrderItem` (`src/main/java/com/example/domain/entity/OrderItem.java:13-14`) e o `@JoinColumn(name = "order_id")` de `Order` (`src/main/java/com/example/domain/entity/Order.java:37-38`). Não fui além disso, então não sei se haveria uma terceira falha. Nenhuma tabela foi criada.
Conclusão: não segue

**C2, o catálogo.** Não deu para testar porque o serviço não sobe. Pelo código, a carga do anexo existe e confere com os valores (`src/main/java/com/example/infrastructure/DataInitializer.java:305-312`), mas isso não foi visto rodando.
Conclusão: não se aplica

**C3, o resumo.** Não deu para testar os dez exemplos nem as fronteiras porque o serviço não sobe.
Conclusão: não se aplica

**C4, as recusas.** Não deu para testar a tabela "Quando dá erro" nem o pedido com dois problemas porque o serviço não sobe.
Conclusão: não se aplica

**C5, a TransLog.** Escrevi e subi o stub da TransLog (`HttpServer` do JDK, com preço, status e atraso configuráveis). O serviço nunca chegou a consultá-lo porque não sobe.
Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para registrar, pagar, enviar, entregar nem cancelar porque o serviço não sobe.
Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar porque o serviço não sobe.
Conclusão: não se aplica

**C8, a última peça.** Não deu para subir nem uma máquina, muito menos duas.
Conclusão: não se aplica

**C9, os dados não se perdem.** Não deu para testar porque o serviço não sobe. Pela leitura, o teste falharia: `application.properties:10` usa `spring.jpa.hibernate.ddl-auto=create-drop`, que apaga o esquema ao desligar e o recria ao subir. Isso não foi verificado rodando.
Conclusão: não se aplica

**C10, os testes do próprio agente.** O `mvn -q verify` da cópia terminou com código 0. Roda 1 teste (`TEST-com.example.OrderSummaryCalculatorTest.xml`: `tests="1" errors="0" skipped="0" failures="0"`). Esse teste usa Mockito e não carrega o contexto do Spring (`src/test/java/com/example/OrderSummaryCalculatorTest.java:20-34`), por isso o build passa mesmo com o serviço incapaz de subir. Um detalhe à parte: no `pom.xml`, o surefire está configurado com `<skipTests>${skip.tests}</skipTests>`, o que faz o `-DskipTests` ser ignorado. O teste rodou até no `package -DskipTests`.
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o serviço subir.** São duas correções. A primeira é remover a linha do dialeto em `application.properties:9`, ou trocar pelo `PostgreSQLDialect`, que o Hibernate 7 detecta sozinho. A segunda é acertar o mapeamento de `OrderItem.orderId` contra o `@JoinColumn` de `Order`: ou um `@ManyToOne` de verdade, ou o campo marcado como `insertable = false, updatable = false`. Junto com isso, convém um teste que suba o contexto (por exemplo, um `@SpringBootTest` com um banco em memória), para que o `mvn verify` pegue esse tipo de falha. Sem isso, nada do enunciado chega a funcionar.
2. **Corrigir o que a leitura aponta e que derrubaria os itens seguintes** (lido, não testado):
   - `ddl-auto=create-drop` (`application.properties:10`) perderia tudo a cada reinício (C9). Com duas máquinas, a segunda, ao subir, apagaria o esquema da primeira (C8).
   - O pagamento não tem transação nem trava: ele lê o estoque (`OrderService.java:194-202`) e depois grava `stock + quantidade` (`ProductService.updateStock`). Dois pagamentos simultâneos venderiam a mesma peça (C8).
   - `BigDecimal.pow(-installments)` (`OrderSummaryCalculator.java:490` e `:509`) lança `ArithmeticException` com expoente negativo. O cartão em 4× ou mais daria erro 500 (Exemplo 2).
   - A TransLog fora do ar e o CEP não atendido viram a mesma recusa: `TransLogService` devolve `null` nos dois casos e `OrderService.java:178-179` transforma isso em `SHIPPING_METHOD_UNAVAILABLE`. O `CARRIER_UNAVAILABLE` nunca sairia.
   - A ordem das validações (`ValidationService.java:20-27`) não segue a tabela: o CPF e o e-mail são conferidos depois do estoque, e a região antes do nível do clube. Um `Region.valueOf(null)` lançaria `NullPointerException`, que não é capturada, e daria 500. O cupom vazio é tratado como se não houvesse cupom (`ValidationService.java:106`), em vez de `INVALID_COUPON`.
   - O aviso de pagamento repetido depois de uma recusa devolveria `OPERATION_NOT_ALLOWED` em vez da mesma recusa (`OrderService.java:81-88`).
   - O cliente só passaria a existir no primeiro pagamento, não no primeiro pedido (`createOrder` não cria o cliente). Um CPF com pedidos mas sem avisos receberia 404 (`CustomerController.java:78-80`).
   - Os valores só são arredondados no fim (`OrderSummaryCalculator.java:281-307`), não em cada etapa.

## Dúvidas

- Fiz um diagnóstico além do que a régua pede: subi o jar com o dialeto trocado pela linha de comando, só para saber se o problema era uma linha ou mais. Ele mostrou uma segunda falha. Esse teste não entrou em nenhuma conclusão. Também não tentei contornar o segundo erro, porque aí eu estaria avaliando um serviço diferente do entregue.
- No C10, marquei `segue` porque o item só pergunta se o `verify` passa e quantos testes rodam. Ele passa, com 1 teste. A conclusão não diz nada sobre esse teste conseguir ou não pegar a falha de subida, o que é assunto da dimensão de testes.
