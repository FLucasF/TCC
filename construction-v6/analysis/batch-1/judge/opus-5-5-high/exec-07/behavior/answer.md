# comportamento

## Visão geral

O serviço não sobe. Com o banco vazio e acessível (o Hikari abre a conexão), a inicialização para em cerca de 3 segundos com `Unable to load class [org.hibernate.dialect.PostgreSQL12Dialect]`. O agente fixou um dialeto em `src/main/resources/application.properties:9` que não existe no Hibernate 7.4.5, a versão que o Spring Boot 4.1.1 traz. Por isso não foi possível testar nenhuma regra do enunciado, e os itens C2 a C9 ficam como `não se aplica`. Isso bate com a foto do banco (`database/database-schema.json` sem tabelas): eu também confirmei por JDBC que nenhuma tabela foi criada.

Para saber se o dialeto era o único bloqueio, rodei um diagnóstico à parte, que não conta na conclusão: corrigi o dialeto por linha de comando. Com isso, o serviço falha num segundo ponto. O mapeamento do item do pedido usa a mesma coluna duas vezes: `DuplicateMappingException: Table [order_items] contains physical column name [order_id] referred to by multiple logical column names` (`src/main/java/com/example/domain/entity/OrderItem.java:14` e `src/main/java/com/example/domain/entity/Order.java:37-38`).

O `mvn verify` passa porque o único teste não sobe o contexto do Spring e não toca no banco. Nada no build mostra que o serviço não funciona.

## Item a item

**C1, o serviço sobe.** Fiz assim:
- Copiei `workspace/` para `/tmp/run/app` e rodei `mvn -q package -DskipTests`, que compila sem erro.
- Subi com `TRANSLOG_URL=http://localhost:9099 java -jar target/shop-orders-service-1.0.0.jar --server.port=8081`.

O processo terminou com código 1 em 3,1 s, com `Application run failed`. A causa no log: `Caused by: java.lang.ClassNotFoundException: Could not load requested class: org.hibernate.dialect.PostgreSQL12Dialect`. A propriedade vem de `src/main/resources/application.properties:9`, e o jar traz `hibernate-core-7.4.5.Final.jar`, em que essa classe não existe. Não é problema de conexão, porque antes do erro o log mostra `HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection`. Rodei duas vezes e o resultado foi o mesmo. `curl localhost:8081/products/CAM-001` não conecta (código 000). Depois disso, o banco continuava sem nenhuma tabela.

O diagnóstico extra (não conta para a conclusão) foi subir com `--spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`. Ele falha de novo, agora com `DuplicateMappingException` na coluna `order_id` de `order_items`. Ou seja, há pelo menos dois defeitos que impedem a subida.
Conclusão: não segue

**C2, o catálogo.** Não deu para testar porque o serviço não sobe. Pela leitura, sem teste: a carga em `src/main/java/com/example/infrastructure/DataInitializer.java:274-288` traz os oito produtos com os valores do anexo e `REL-001` marcado como fora de linha.
Conclusão: não se aplica

**C3, o resumo.** Não deu para testar os dez exemplos nem as fronteiras porque o serviço não sobe. Montei o stub da TransLog (`Stub.java`, porta 9099), mas ele não recebeu nenhuma chamada.
Conclusão: não se aplica

**C4, as recusas.** Não deu para testar porque o serviço não sobe. Pela leitura, sem teste, a ordem de validação em `src/main/java/com/example/application/service/ValidationService.java:19-28` não é a da tabela:
- confere a região antes do nível do clube;
- confere o estoque antes do CPF e do e-mail.
Seria a primeira coisa a confirmar se o serviço subisse.
Conclusão: não se aplica

**C5, a TransLog.** Não deu para testar porque o serviço não sobe. Pela leitura, sem teste, há dois pontos a confirmar quando o serviço subir:
- TransLog fora do ar: `src/main/java/com/example/application/service/TransLogService.java:258-277` devolve `null` tanto para `ZIP_NOT_SERVED` quanto para TransLog fora do ar, e `OrderService.java:178-179` transforma os dois casos em `SHIPPING_METHOD_UNAVAILABLE`. O `CARRIER_UNAVAILABLE` do enunciado parece não sair nunca.
- Preço com mais de duas casas: `price_cents` é lido com `intValue()` (`TransLogService.java:269`), que corta as casas.
Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para testar porque o serviço não sobe.
Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar nem em sequência nem com chamadas simultâneas porque o serviço não sobe. Pela leitura, sem teste, há três pontos a confirmar:
- Repetição de uma recusa: só a repetição de um pagamento que deu certo é reconhecida (`OrderService.java:81-84`). A repetição de uma recusa (expirado, sem estoque) cairia em `OPERATION_NOT_ALLOWED`, e não na mesma recusa.
- Chamadas simultâneas: não há `@Transactional`, trava nem `@Version` em lugar nenhum do código.
- Duas máquinas: o `ddl-auto=create-drop` (`application.properties:10`) faria a segunda máquina apagar as tabelas da primeira ao subir.
Conclusão: não se aplica

**C8, a última peça.** Não deu para testar porque o serviço não sobe. Pela leitura, sem teste: o estoque é conferido e depois gravado em passos separados, ler e depois somar (`OrderService.java:97` e `ProductService.updateStock`), sem transação nem trava. Dois pagamentos ao mesmo tempo poderiam passar os dois.
Conclusão: não se aplica

**C9, os dados não se perdem.** Não deu para testar porque o serviço não sobe. Pela leitura, sem teste: `spring.jpa.hibernate.ddl-auto=create-drop` (`application.properties:10`) apaga o esquema ao desligar e o recria ao subir. Os pedidos, o estoque e os avisos se perderiam a cada reinício, que é justamente o que o enunciado proíbe.
Conclusão: não se aplica

**C10, os testes do próprio agente.** Na cópia, `mvn verify` termina em `BUILD SUCCESS` com `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. É um teste só, `OrderSummaryCalculatorTest.testSimpleCalculation`. Detalhe à parte: `-DskipTests` não pula os testes, porque `pom.xml:90` liga `skipTests` a `${skip.tests}`. O teste rodou também no `package`.
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o serviço subir.** São duas mudanças:
   - Tirar `spring.jpa.database-platform` de `application.properties:9`. O Hibernate 7 descobre sozinho o dialeto do PostgreSQL.
   - Mapear a coluna `order_id` de `order_items` uma vez só (`OrderItem.java:14` contra `Order.java:37-38`).

   Sem isso, nada do enunciado funciona. Um teste que suba o contexto do Spring teria pegado os dois erros já no `mvn verify`.
2. **Trocar `ddl-auto=create-drop` por uma criação que não apague nada.** Por exemplo, um `schema.sql` idempotente ou Flyway. Do jeito que está, cada reinício e cada segunda máquina apagariam os dados, contra a exigência explícita do enunciado de não perder nada e rodar em duas máquinas.

## Dúvidas

1. **O diagnóstico com o dialeto corrigido.** Sobre o C1, o guia diz para marcar como `não se aplica` os itens que dependem de o serviço subir. Fiz isso, mas rodei esse diagnóstico extra só para saber se havia um único bloqueio, e ele não entrou em nenhuma conclusão. O serviço também não sobe com essa correção, então a conclusão do C1 seria a mesma.
2. **As observações de leitura em C2, C4, C5 e C7 a C9.** São só leitura do código, marcadas como tal, para orientar quem for testar uma versão corrigida. Elas não mudaram nenhuma conclusão.
