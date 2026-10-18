# comportamento

## Visão geral

O serviço não sobe. Com o banco vazio, o Hibernate recusa o mapeamento da entidade `Order` e a aplicação para em cerca de 2 segundos com `Application run failed`. O motivo é a coluna `installments`, mapeada duas vezes: uma pelo campo do pedido (`src/main/java/com/loja/order/model/Order.java:49-50`) e outra pelo campo de mesmo nome do resumo embutido (`src/main/java/com/loja/order/model/OrderSummary.java:15`, embutido em `Order.java:68`), sem `@AttributeOverride`. Há um segundo problema por trás desse: o Flyway não roda. O `pom.xml:56-62` traz só `flyway-core` e `flyway-database-postgresql`, sem o módulo de autoconfiguração do Spring Boot 4 (o jar não contém nenhum `spring-boot-flyway`). Por isso o log não tem nenhuma linha do Flyway e o banco continua com 0 tabelas depois da tentativa, o que confere com a foto vazia de `database/database-schema.json`. Mesmo corrigido o mapeamento, o `ddl-auto: validate` (`application.yml:6`) acharia o banco sem tabelas. Os itens C2 a C9 dependem do serviço no ar e não puderam ser testados. O `mvn verify` passa, com 6 testes de unidade, mas nenhum deles carrega o contexto do Spring, então nenhum pega a falha na subida.

## Item a item

**C1, o serviço sobe.** Copiei o projeto, compilei com `mvn -q package -DskipTests` (compilou em ~11 s) e subi com `TRANSLOG_URL=http://localhost:9090 java -jar target/order-service-1.0.0.jar --server.port=8081`, com o banco vazio e o stub da TransLog no ar.
- O que eu esperava: o serviço criar as tabelas e o catálogo, e subir.
- O que veio: a conexão com o PostgreSQL 18.6 abre (`HikariPool-1 - Start completed`), mas logo em seguida aparece `org.hibernate.MappingException: Column 'installments' is duplicated in mapping for entity 'com.loja.order.model.Order'` e `Application run failed`. O processo morre entre 04:31:47.710 e 04:31:49.864.
- Não aparece nenhuma linha do Flyway no log. Uma consulta a `information_schema.tables` depois da tentativa mostrou `tables=0`: as migrações `V1__Initial_schema.sql` e `V2__Initial_catalog.sql` nunca rodaram.

Conclusão: não segue

**C2, o catálogo.** Não deu para consultar `GET /products/{sku}` porque o serviço não sobe, e o banco ficou sem a tabela `products`. A migração `V2__Initial_catalog.sql` traz os oito produtos com os valores do anexo e `REL-001` como `TRUE`, mas ela nunca é aplicada.

Conclusão: não se aplica

**C3, o resumo.** Não deu para chamar `POST /checkout/summary` com o serviço fora do ar.

Conclusão: não se aplica

**C4, as recusas.** Não deu para testar as recusas com o serviço fora do ar.

Conclusão: não se aplica

**C5, a TransLog.** O stub ficou no ar na porta 9090, mas o serviço nunca chegou a chamá-lo.

Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para registrar nem movimentar pedidos com o serviço fora do ar.

Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar com o serviço fora do ar.

Conclusão: não se aplica

**C8, a última peça.** Não deu para subir nem uma máquina, muito menos duas.

Conclusão: não se aplica

**C9, os dados não se perdem.** Sem a primeira subida, não há o que reiniciar. O banco ficou sem tabelas.

Conclusão: não se aplica

**C10, os testes do próprio agente.** O `mvn -q verify` na cópia termina com código 0. Rodam 6 testes, nenhum falha:
- 4 em `PricingServiceTest`;
- 2 em `RoundingUtilTest`.

Nenhum usa `@SpringBootTest`. O `src/test/resources/application-test.yml` existe, mas nenhum teste o carrega. É por isso que o build passa com um serviço que não sobe.

Conclusão: segue

## O que mudaria primeiro

1. **Fazer o serviço subir.** Isso pede duas mudanças:
   - dar à coluna do resumo o nome que a própria migração já prevê, `installments_summary` (`V1__Initial_schema.sql`), com `@AttributeOverride` ou `@Column` no `OrderSummary`;
   - trocar `flyway-core` pelo starter de Flyway do Spring Boot 4, para que as migrações rodem antes do `validate`.

   Sem essas duas, nada do enunciado pode ser usado.
2. **Ter pelo menos um teste que suba o contexto**, com o perfil de teste que já existe. Ele teria pegado as duas falhas no próprio `mvn verify`.

Lendo o código, sem ter testado nada disso, há outros pontos que provavelmente apareceriam depois da subida:
- O peso é truncado para quilos inteiros antes de ir para gramas (`OrderService.java:83`, `totalWeight.intValue() * 1000`), então 1,8 kg seria enviado à TransLog como 1000 g.
- `orders.customer_cpf` tem chave estrangeira para `customers` (`V1__Initial_schema.sql:18`), mas o cliente só é criado no pagamento. O primeiro `POST /orders` de um CPF novo provavelmente viola essa chave. O erro cairia no `catch (Exception)` genérico e voltaria como `503 CARRIER_UNAVAILABLE` (`OrderController.java:42`).

## Dúvidas

O guia pede o tempo de subida em C1, mas como o serviço não sobe, registrei só o tempo até a falha, cerca de 2 s. Marquei C2 a C9 como `não se aplica`, como o guia manda quando o serviço não sobe. A falha em subir pesa só em C1. Em C10 contei apenas os testes e o resultado do build, sem julgar se os testes são bons.

Todos os processos (o serviço e o stub) foram derrubados e as cópias temporárias apagadas. A pasta de trabalho não foi alterada.
