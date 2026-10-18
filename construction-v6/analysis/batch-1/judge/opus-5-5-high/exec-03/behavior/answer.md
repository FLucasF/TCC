# comportamento

## Visão geral

O serviço não sobe, então não foi possível testar o comportamento. O jar compila (`mvn -q package -DskipTests`, cerca de 11 s), mas a aplicação morre em uns 3,7 s com dois defeitos, cada um suficiente para impedir a subida:

1. **Mapeamento duplicado no Hibernate.** O log mostra `org.hibernate.MappingException: Column 'installments' is duplicated in mapping for entity 'com.loja.order.model.Order'`. A entidade `Order` mapeia a coluna `installments` em `src/main/java/com/loja/order/model/Order.java:49-50`. O `OrderSummary` embutido (`@Embedded`, `Order.java:68-69`) tem outro campo `installments` (`src/main/java/com/loja/order/model/OrderSummary.java:15`), sem `@Column` nem `@AttributeOverride`, e cai na mesma coluna.
2. **O banco nunca é criado.** O projeto usa só `flyway-core` e `flyway-database-postgresql` (`pom.xml`). No Spring Boot 4 isso não basta para o Flyway rodar sozinho: falta o módulo `spring-boot-flyway` (ou o starter). O jar não tem esse módulo (`jar tf` lista `flyway-core-12.4.0.jar`, mas nenhum `spring-boot-flyway`) e o log não tem nenhuma linha do Flyway. Depois das duas tentativas, o banco continua sem nenhuma tabela em `public`. Como o `application.yml` pede `ddl-auto: validate`, corrigir só o mapeamento levaria a outra falha na validação do esquema.

O `mvn verify` passa, mas com seis testes de unidade que nunca carregam o contexto do Spring. Por isso nenhum deles pegaria essa falha. Do C2 ao C9, tudo depende do serviço no ar e fica como `não se aplica`.

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run/app` e compilei. Subi com `TRANSLOG_URL=http://localhost:9099 java -jar target/order-service-1.0.0.jar --server.port=8081`, apontando para o stub da TransLog já no ar. Esperava a porta 8081 respondendo e as tabelas e o catálogo criados no banco vazio.

Nas duas tentativas veio o mesmo resultado: o processo termina com `exit=1` em cerca de 3,7 s, com `Failed to initialize JPA EntityManagerFactory ... MappingException: Column 'installments' is duplicated in mapping for entity 'com.loja.order.model.Order'`, seguido de `Application run failed`. Nenhuma requisição chegou a ser atendida.

O serviço também não cria o que precisa. Consultei o banco via JDBC (`select count(*) from information_schema.tables where table_schema='public'`) e o resultado foi `0`. As migrações `V1__Initial_schema.sql` e `V2__Initial_catalog.sql` nunca rodaram, porque o jar não traz a autoconfiguração do Flyway do Boot 4.
Conclusão: não segue

**C2, o catálogo.** Não deu para testar, porque o serviço não sobe (C1) e o banco ficou sem a tabela `products`. A migração `V2__Initial_catalog.sql` tem as oito linhas do anexo, mas ela nunca roda.
Conclusão: não se aplica

**C3, o resumo.** Não deu para testar os dez exemplos nem as fronteiras: não há serviço para chamar `POST /checkout/summary` (C1).
Conclusão: não se aplica

**C4, as recusas.** Não deu para testar a tabela "Quando dá erro" nem a ordem das verificações, porque o serviço não sobe (C1).
Conclusão: não se aplica

**C5, a TransLog.** Deixei um stub com `HttpServer` do JDK pronto na porta 9099 (preço configurável, `ZIP_NOT_SERVED`, atraso, status de erro), mas o serviço nunca chegou a chamá-lo (C1).
Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para testar registro, pagamento, envio, entrega, cancelamento, prazo de 48 horas nem avisos, porque o serviço não sobe (C1).
Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar nem em sequência nem em paralelo nas duas máquinas, porque nenhuma instância sobe (C1).
Conclusão: não se aplica

**C8, a última peça.** Não deu para testar a disputa de estoque entre as duas máquinas, porque nenhuma instância sobe (C1).
Conclusão: não se aplica

**C9, os dados não se perdem.** Não havia dado nenhum para persistir, e o banco nem chegou a ter tabelas (C1).
Conclusão: não se aplica

**C10, os testes do próprio agente.** Rodei `mvn -q verify` na cópia e ele terminou com `exit=0`. Rodaram 6 testes, sem falhas, erros ou testes pulados: `RoundingUtilTest` com 2 e `PricingServiceTest` com 4, segundo `target/surefire-reports/*.xml`. Nenhum usa `@SpringBootTest`. Por isso o build passa mesmo com a aplicação incapaz de subir.
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o serviço subir.** Isso pede duas mudanças. A primeira é dar ao `installments` do resumo uma coluna própria: o esquema já tem `installments_summary`, então basta um `@AttributeOverride` em `Order.java:68` ou um `@Column(name = "installments_summary")` em `OrderSummary.java:15`. A segunda é trocar `flyway-core` por `spring-boot-starter-flyway` no `pom.xml`, para as migrações rodarem no banco vazio. Sem essas duas mudanças, nenhum outro requisito do enunciado pode ser atendido.
2. **Ter um teste que suba o contexto do Spring** (por exemplo, com o H2 que o `application-test.yml` já configura). Ele teria pegado as duas falhas no próprio `mvn verify`.

## Dúvidas

- Não corrigi a cópia para seguir testando. O guia manda marcar como `não se aplica` os itens que dependem do serviço quando ele não sobe, e um serviço que eu mesmo consertasse já não seria o serviço avaliado.
- Na leitura do código para planejar os testes, vi pontos que provavelmente falhariam se o serviço subisse. Eles **não foram testados** e não entram em nenhuma conclusão:
  - `orders.customer_cpf` tem uma FK para `customers` (`V1__Initial_schema.sql`), mas o cliente só é criado no pagamento (`OrderService.java:212-215`). Assim, registrar o primeiro pedido de um CPF provavelmente falharia, e a exceção cairia no `catch (Exception)` que devolve `503 CARRIER_UNAVAILABLE` (`OrderController.java:41-43`).
  - O peso enviado à TransLog é truncado para kg inteiros antes da conversão (`totalWeight.intValue() * 1000`, `OrderService.java:83`).
  - O prazo de 48 horas é medido com `Instant.now()`, não com o `paidAt` recebido (`Order.java:198-199`).
  - Com FREESHIPPING, o frete é zerado e o cupom ainda desconta o mesmo valor (`PricingService.java:131-133`). Isso contraria o Exemplo 6.
  - Um cupom vazio é aceito em vez de dar `INVALID_COUPON` (`OrderService.java:359`).
  - Na falta de estoque no pagamento, o pedido não é cancelado e o cliente não é avisado (`OrderService.java:403-410`).
  - O seguro e o crédito usam taxas `double` (`Region.java`, `ClubTier.java`), o que pode errar o "meio para o par" exatamente nos empates.
