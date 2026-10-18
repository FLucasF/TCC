# comportamento

## Visão geral

O serviço não sobe. O `schema.sql` tem um erro de sintaxe: na tabela `notifications` há uma linha `REFERENCES orders(id),` sem coluna antes (`src/main/resources/schema.sql:34`), e a mesma linha se repete em `payment_tracking` (`schema.sql:41`). O `Database.executeSchema` (`src/main/java/com/loja/repository/Database.java:30-35`) roda as instruções uma a uma, sem transação. Por isso as tabelas `products`, `customers` e `orders` chegam a ser criadas. Quando chega em `notifications`, o PostgreSQL recusa a instrução, sai uma `RuntimeException("Failed to initialize schema")` e o `main` termina antes do `Javalin.create(...).start(port)` (`App.java:15` e `App.java:29`). A carga do catálogo (`schema.sql:45`) nunca roda. Subi o serviço duas vezes, com o banco vazio e depois com as três tabelas já criadas, e as duas falharam do mesmo jeito. Nenhuma porta abriu. Por isso nenhum item da API deu para testar. O único item que se pôde conferir é o C10: o `mvn verify` passa, mas com um teste só.

## Item a item

**C1, o serviço sobe.** Copiei o `workspace/` para `/tmp/run/app` e compilei com `mvn -q package -DskipTests`, sem erro, em uns 20 s. Depois subi com `PORT=8081 TRANSLOG_URL=http://localhost:9099 java -jar target/order-service.jar`, usando o banco vazio de `DB_URL`. O processo terminou em poucos segundos com este erro no log:
```
Exception in thread "main" java.lang.RuntimeException: Failed to initialize schema
	at com.loja.repository.Database.executeSchema(Database.java:35)
	at com.loja.repository.Database.initialize(Database.java:20)
	at com.loja.App.main(App.java:15)
Caused by: org.postgresql.util.PSQLException: ERROR: syntax error at or near "REFERENCES"
  Position: 202
```
`curl localhost:8081/products/CAM-001` devolveu `000`, ou seja, ninguém atendia, e não havia processo vivo. Pela posição 202 do erro, o problema está na instrução `CREATE TABLE IF NOT EXISTS notifications` (`schema.sql:28-36`), na linha solta `REFERENCES orders(id),` (`schema.sql:34`). Consultei o banco por JDBC depois da falha: estavam lá só as tabelas `customers`, `orders` e `products`, e `products` com 0 linhas. Isso bate com `database/database-schema.json`, que também mostra só essas três tabelas. Subi uma segunda vez, já com essas tabelas no banco, e veio o mesmo erro, então a falha não depende do estado inicial do banco. O serviço não sobe nem cria o que precisa.
Conclusão: não segue

**C2, o catálogo.** Não deu para testar: o serviço não sobe e não há `GET /products/{sku}` para chamar. A tabela `products` ficou vazia (0 linhas), porque o `INSERT` do catálogo (`schema.sql:45`) vem depois da instrução que falha.
Conclusão: não se aplica

**C3, o resumo.** Não deu para testar: o serviço não sobe, então não houve `POST /checkout/summary` para os dez exemplos nem para as fronteiras.
Conclusão: não se aplica

**C4, as recusas.** Não deu para testar: o serviço não sobe, então não houve chamada para ver status e código de erro.
Conclusão: não se aplica

**C5, a TransLog.** Não deu para testar. O imitador da TransLog estava no ar na porta 9099 e respondia `{"price_cents": 3310, "eta_hours": 41}`, mas o serviço não chegou a chamá-lo: o arquivo que registra as chamadas recebidas pelo imitador ficou vazio.
Conclusão: não se aplica

**C6, o ciclo do pedido.** Não deu para testar: o serviço não sobe, então não há como registrar, pagar, enviar, entregar ou cancelar um pedido.
Conclusão: não se aplica

**C7, o aviso de pagamento repetido.** Não deu para testar: o serviço não sobe nem em uma máquina, quanto mais em duas. Além disso, a tabela `notifications`, onde ficariam os avisos `PAYMENT_APPROVED`, nem chega a existir.
Conclusão: não se aplica

**C8, a última peça.** Não deu para testar: o serviço não sobe nas máquinas 8081/8082 e não há produto no banco para disputar.
Conclusão: não se aplica

**C9, os dados não se perdem.** Não deu para testar: sem o serviço no ar não há pedido, estoque ou aviso gravado para conferir depois de reiniciar.
Conclusão: não se aplica

**C10, os testes do próprio agente.** `mvn -q verify` na cópia terminou com `exit=0`. O relatório do surefire mostra uma classe e um teste: `com.loja.service.CheckoutServiceTest`, com `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. Esse teste usa repositórios falsos e não toca no banco, por isso passa mesmo com o `schema.sql` quebrado. Ele confere o subtotal, o desconto e o frete de um único resumo (`src/test/java/com/loja/service/CheckoutServiceTest.java:26-41`). Isso bate com `pit.txt` (`tests=1`). O build passa, e roda um teste.
Conclusão: segue

## O que mudaria primeiro

1. **Consertar o `schema.sql` e testar a subida contra um banco de verdade.** As duas linhas `REFERENCES orders(id),` (`schema.sql:34` e `schema.sql:41`) precisam virar restrições válidas, por exemplo `FOREIGN KEY (order_id) REFERENCES orders(id)`. Só esse erro derruba a dimensão inteira. Seria bom também rodar o script inteiro numa transação: hoje uma falha no meio deixa o banco pela metade, com três tabelas e sem catálogo. Um único teste que subisse o serviço teria pegado o erro.
2. **Depois que subir, rever o pagamento olhando para as duas máquinas.** Isto é leitura do código, não testei. O controle de aviso repetido é um `HashMap` em memória, com `synchronized` dentro de um processo só (`OrderService.java:16` e `OrderService.java:65-70`). Esse controle não é compartilhado entre as máquinas e se perde quando o serviço reinicia. O estoque é conferido com uma leitura (`OrderService.java:89` e `OrderService.java:198`) e só depois baixado com `stock = stock - ?`, sem trava nem condição (`ProductRepository.java:37`). Do jeito que está escrito, os pontos C7 e C8 provavelmente falhariam. Não deu para confirmar porque o serviço não sobe.

## Dúvidas

- **Testar ou não um serviço consertado.** Pensei em corrigir o `schema.sql` numa cópia para testar o resto da API. Não fiz: o resultado seria de um serviço diferente do que o agente entregou, e `behavior.md` manda marcar como `não se aplica` os itens que dependem do serviço quando ele não sobe. As observações sobre pagamento e estoque no item 2 de "O que mudaria primeiro" saíram só da leitura do código e não entram em nenhuma conclusão.
- **O banco ficou alterado.** Ao subir, o serviço deixou no banco do teste as tabelas `customers`, `orders` e `products`, vazias. Isso é efeito da própria subida que falhou, não de algo que eu tenha feito à mão. A pasta de trabalho não foi alterada, e o imitador da TransLog e os processos do serviço foram derrubados no fim.
