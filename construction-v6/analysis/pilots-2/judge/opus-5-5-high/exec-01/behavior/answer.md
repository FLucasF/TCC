# comportamento

## Visão geral

O serviço não sobe. Ele compila, mas na partida o Hibernate recusa o mapeamento da entidade `Order` com `MappingException: Column 'installments' is duplicated in mapping for entity 'com.loja.orderservice.entity.Order'`. O motivo: a entidade tem um campo próprio `installments` (`src/main/java/com/loja/orderservice/entity/Order.java:35`) e também embute o `OrderSummary` (`Order.java:43`), que tem outro campo `installments` (`src/main/java/com/loja/orderservice/entity/OrderSummary.java:15`). Os dois caem na mesma coluna. A aplicação para em cerca de 2,6 s, antes de criar qualquer tabela. A foto do banco confirma isso: `database/database-schema.json` não tem tabela nenhuma. Por isso nenhuma das chamadas da API pôde ser feita, e C2 a C9 ficam como `não se aplica`. O `mvn verify` passa, mas só porque os 6 testes são unitários sobre `PricingUtil` e nenhum deles sobe o contexto do Spring. Lendo o código (isto não foi testado e não entra nas conclusões), eu esperaria mais falhas mesmo depois de corrigido o mapeamento:
- `ddl-auto=create` (`src/main/resources/application.properties:10`) apaga o banco a cada partida, o que derrubaria C9 e a segunda máquina de C8.
- O `RestTemplate` não tem tempo limite (`src/main/java/com/loja/orderservice/config/WebClientConfig.java:11`).
- O GOLD não ganha o frete grátis (`PricingService` só fala de GOLD em `:286` e `:293`, no crédito e no brinde), e isso daria valores errados no Exemplo 7.
- O cupom vem antes das falhas da entrega na ordem das recusas: ele é conferido em `ValidationService.java:57`, e a TransLog só é chamada no cálculo de preço, depois.
- O aviso de pagamento repetido sobre um pedido já pago cai em `OPERATION_NOT_ALLOWED` (`OrderService.java:90`) antes de chegar à checagem de idempotência (`:105`).
- Não há trava no estoque (`OrderService.java:209`).

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/svc`. `mvn -q package -DskipTests` compilou em 16 s. Subi com `TRANSLOG_URL=http://localhost:9090 java -jar target/order-service-1.0.0.jar --server.port=8081`, apontando para a TransLog imitada. O pool de conexões chegou a se conectar ao PostgreSQL (`HikariPool-1 - Added connection`). Às 19:36:17.338 veio `Failed to initialize JPA EntityManagerFactory ... MappingException: Column 'installments' is duplicated in mapping for entity 'com.loja.orderservice.entity.Order'`, seguido de `Application run failed`, e o processo saiu. `curl localhost:8081/products/CAM-001` respondeu "connection refused" (código 7 do curl). O erro é de mapeamento e acontece sempre, não por acaso: o campo `installments` existe ao mesmo tempo em `Order.java:35` e no `@Embedded OrderSummary` (`Order.java:43`, `OrderSummary.java:15`). O banco continua sem tabelas (`database/database-schema.json`, `"tables": []`).
Conclusão: não segue

**C2, o catálogo.** Não deu para testar: o serviço não sobe (C1). O código de carga existe (`CatalogService.java:20-31`, chamado no `ApplicationReadyEvent`), mas esse evento nunca acontece.
Conclusão: não se aplica

**C3, o resumo.** Não deu para testar: `POST /checkout/summary` não responde porque o serviço não sobe (C1).
Conclusão: não se aplica

**C4, as recusas.** Não deu para testar: nenhuma rota responde (C1).
Conclusão: não se aplica

**C5, a TransLog.** Escrevi e subi a TransLog imitada (`Stub.java` na porta 9090, com preço normal, CEP não atendido, erro 500 e demora de 3 s, conforme o CEP). O serviço caiu antes de fazer qualquer consulta a ela (C1).
Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para testar: não há como registrar um pedido (C1).
Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar: não há pedido para pagar (C1).
Conclusão: não se aplica

**C8, a última peça.** Não deu para testar: nem uma máquina sobe, menos ainda duas (C1).
Conclusão: não se aplica

**C9, os dados não se perdem.** Não deu para testar: não há o que reiniciar (C1). Pela leitura, `spring.jpa.hibernate.ddl-auto=create` (`application.properties:10`) recriaria as tabelas a cada partida. Isso não foi testado e não muda a conclusão.
Conclusão: não se aplica

**C10, os testes do próprio agente.** `mvn -q verify` na cópia terminou com código 0, em 6,7 s. Rodam 6 testes, todos em `PricingServiceTest`: surefire `tests="6" errors="0" skipped="0" failures="0"`. Nenhum sobe o contexto do Spring, e por isso a falha de C1 passa despercebida.
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o serviço subir.** É preciso resolver a coluna `installments`, que aparece duas vezes na tabela `orders`. Uma saída é renomear a coluna de uma das duas (por exemplo, `@AttributeOverride` no `@Embedded` de `Order.java:43`). Outra é tirar o campo repetido da entidade, já que o resumo já guarda as parcelas. Sem isso, nada do enunciado funciona.
2. **Ter pelo menos um teste que suba o contexto** (`@SpringBootTest` com o H2 que já está no `pom.xml` e em `application-test.properties`). Com ele, o `mvn verify` teria pegado esse erro em vez de passar em verde. Logo depois, trocar `ddl-auto=create` por um esquema que não apague os dados ao reiniciar.

## Dúvidas

Uma só. Corrigir o mapeamento numa cópia só para ver o resto funcionando seria testar outro serviço, não o que foi entregue. Por isso não fiz, e C2 a C9 ficaram como `não se aplica`, como a régua pede quando o serviço não sobe. O que cito do código na visão geral e em C9 vem só da leitura, não de teste, e não entrou em nenhuma conclusão.
