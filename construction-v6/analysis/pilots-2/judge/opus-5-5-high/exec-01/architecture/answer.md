# Arquitetura

## Visão geral

O projeto está organizado por camada: `controller`, `service`, `repository`, `entity`, `dto`, mais `client`, `config`, `exception`, `listener` e `util`. Não há ciclos entre pacotes. O acesso ao banco passa todo por repositórios Spring Data. O resumo e o pedido usam o mesmo cálculo, chamado de um só lugar.

O problema mais sério é que a regra de negócio conhece a web, e de dois jeitos:
- os três serviços de negócio recusam lançando `OrderException` com o código HTTP já escolhido (`HttpStatus.UNPROCESSABLE_ENTITY`, `CONFLICT`, `SERVICE_UNAVAILABLE`);
- o cálculo e a validação recebem o próprio corpo da requisição da API, o `OrderRequestDTO`.

O relatório não pega isso: o `HttpStatus` fica no `org.springframework.http`, fora do que ele conta como "web de entrada". O segundo problema é que os controllers nem sempre só traduzem. O `OrderController` decide uma recusa `INVALID_ORDER` antes de chamar o serviço. O `CustomerController` e o `ProductController` vão direto aos repositórios, e um deles muda o estado do aviso.

## Item a item

**A1, a regra de negócio não conhece a web?** Não. Os três serviços de regra importam `org.springframework.http.HttpStatus`:
- `service/OrderService.java:12`, usado em `:82`, `:91`, `:101` e `:199`;
- `service/PricingService.java:10`, usado em `:154`, `:175`, `:177` (`SERVICE_UNAVAILABLE`) e `:223`;
- `service/ValidationService.java:10`, usado em `:62`, `:92`, `:101` e outros.

A exceção de negócio carrega o código HTTP como campo (`exception/OrderException.java:3`, `:7`). Quem decide se a recusa é 422, 409 ou 503 é a regra de negócio, não a camada web.

Além disso, a regra recebe o objeto de entrada da API. O `OrderRequestDTO` é o `@RequestBody` de `/checkout/summary` e de `/orders` (`controller/OrderController.java:25`, `:36`). Ele chega sem tradução a estes métodos:
- `OrderService.calculateSummary` e `createOrder` (`service/OrderService.java:42`, `:48`);
- `PricingService.calculateSummary` (`service/PricingService.java:27`);
- `ValidationService.validateOrderRequest` (`service/ValidationService.java:28`).

O relatório confirma a dependência `service -> dto` (8 dependências de classe), que inclui também `OrderItemDTO` e `CustomerDTO`. O pacote `dto` não mora com os controllers, mas o que ele guarda são os corpos de requisição e de resposta da API. O relatório diz "Classes outside the web layer that use the incoming web: none" porque não conta o `HttpStatus`; a régua conta os códigos HTTP. Uma classe de negócio que conhece a web já basta, e aqui são três.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não traz nenhum grupo ("Cycles: 0 group(s), 0 of 10 packages caught in one"). As dependências vão todas num sentido só: `controller -> service -> repository -> entity`, com `dto -> entity` e nenhuma volta.
Conclusão: segue

**A3, o controller só traduz?** Em parte. Os fluxos principais do `OrderController` só traduzem: chamam o `OrderService`, convertem `Order`/`OrderSummary` em DTO e trocam `OrderException` por status e corpo (`controller/OrderController.java:24-110`). Há três desvios:
- O próprio controller recusa com `INVALID_ORDER` quando falta `transactionId`/`paidAt` (`controller/OrderController.java:60-63`) ou `trackingCode` (`:76-79`). Ele faz isso antes de o serviço procurar o pedido. O enunciado manda conferir `ORDER_NOT_FOUND` primeiro; com a decisão no controller, um pedido inexistente sem esses campos recebe 422 em vez de 404.
- O `CustomerController` não tem serviço: lê e grava pelos repositórios (`controller/CustomerController.java:30`, `:43`, `:59`). Ele decide a regra "CPF que nunca registrou pedido é `CUSTOMER_NOT_FOUND`" (`:45`). Ele também marca o aviso como lido e o grava: `n.setStatus(NotificationStatus.READ)` e `notificationRepository.save(n)` (`:66-67`). Isso é mudar estado de negócio dentro do controller.
- O `ProductController` também consulta o repositório direto (`controller/ProductController.java:25`). É só uma consulta, então pesa pouco.

Nenhum controller faz conta, muda a situação do pedido ou mexe no estoque. Por isso a conclusão não chega a `não segue`.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** Sim. Os serviços falam com os dados só pelas interfaces `JpaRepository` (`repository/*.java`). As duas consultas escritas à mão são JPQL em `@Query`, dentro dos próprios repositórios (`repository/OrderRepository.java:13`, `repository/CustomerNotificationRepository.java:13`). Uma busca por `Jdbc`, `EntityManager`, `createQuery`, `nativeQuery` e `DataSource` não acha nada no código principal. O único desvio de caminho é o dos controllers que usam o repositório direto, já contado em A3.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Sim. Os dois fluxos fazem as mesmas duas chamadas e mais nada:
- `OrderService.calculateSummary` (`service/OrderService.java:43-44`) e `createOrder` (`:50-53`) chamam ambos `validationService.validateOrderRequest(request)` seguido de `pricingService.calculateSummary(request)`;
- o pedido grava esse mesmo `OrderSummary` (`:68`).

Não há nenhuma etapa refeita só num dos fluxos. Uma observação fora da pergunta: dentro do cálculo único, o frete é calculado duas vezes quando o cupom é `FREESHIPPING` (`service/PricingService.java:31` e `:100`), com duas consultas à TransLog. O desconto pode sair de uma cotação diferente da do frete mostrado. Isso afeta os dois fluxos igualmente; não é uma divergência entre eles.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada:
- `controller` (3 classes), `service` (4), `repository` (4), `entity` (13), `dto` (11);
- pacotes técnicos de apoio: `client` (TransLog), `config`, `exception`, `listener` (carga do catálogo na subida) e `util` (arredondamento).

Todos os assuntos (pedido, catálogo, cliente, avisos) se misturam dentro de cada camada (contagens do relatório).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar a web da regra de negócio.** As exceções de negócio passariam a carregar só o código do problema, com o mapeamento para 422, 409, 404 ou 503 feito na camada web. Um `@RestControllerAdvice` faria isso e ainda eliminaria os `try/catch` repetidos em cada endpoint. O controller também traduziria o `OrderRequestDTO` para um tipo de entrada próprio do domínio antes de chamar o serviço. É a mudança que resolve A1, a violação mais séria.
2. **Passar a recusa e a mudança de estado para os serviços.** A conferência de `transactionId`/`paidAt`/`trackingCode` iria para o `OrderService`, depois da busca do pedido, o que de quebra corrige a ordem dos erros. O "marcar como lido" e a regra do `CUSTOMER_NOT_FOUND` iriam para um serviço de clientes e avisos, deixando os controllers só como tradutores.

## Dúvidas

- **A1:** o pacote `dto` não mora com os controllers, que é a pista que a régua dá. Mesmo assim, considerei o `OrderRequestDTO` um objeto da API: ele é o `@RequestBody` dos endpoints e não tem outro papel. Contei o `HttpStatus` como "código HTTP" no sentido da régua, mesmo que o relatório não o classifique como web de entrada. Esse ponto sozinho já decidiria o item.
- **A3:** a falta de um campo obrigatório pode ser lida como validação de entrada, que caberia ao controller. Tratei como recusa do negócio porque o enunciado a põe na sequência de verificações do pedido, depois de `ORDER_NOT_FOUND`.
