Já li o enunciado e todo o código de relatórios, serviço, repositório e API; agora escrevo a correção.

# Cenário 09

## Visão geral

O agente juntou os quatro relatórios numa só classe, `ReportService`, com um método `generateXxxReport` para cada um. Também criou um auxiliar privado, `ReportBuilder`, que só faz `String.join(",")` e acrescenta a quebra de linha. Esse auxiliar não tem esqueleto. Cada um dos quatro métodos repete a sequência inteira à mão: escolher as reservas, ordenar, título, cabeçalho, laço de linhas, total, `build`. A linha de título também aparece copiada em cada um, com a constante do nome da pousada. É o caso que as instruções descrevem como malfeito: o "esqueleto" é só um utilitário de CSV. Quando os relatórios tiverem de terminar com a data em que foram tirados, vai ser preciso mexer nos quatro métodos, e depois nos que vierem. O conteúdo dos quatro relatórios confere com o pedido. Fora dos relatórios há um defeito: um corpo que não é JSON deve dar 500 em vez de 400.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` tem estas partes:
- `App`: monta tudo à mão e registra as rotas (`App.java:20-54`).
- `api.ApiHandler`: lê os parâmetros, chama o serviço e converte para JSON; o tratamento de erro é um `instanceof` em cadeia (`ApiHandler.java:209-231`).
- `service`: `ReservationService` (criar, confirmar, cancelar, check-in, check-out, comprovante e e-mail), `PricingService` (alta temporada, arredondamento, taxa de turismo) e `ReportService` (os quatro relatórios).
- `repository`: listas em memória (`ReservationRepository`, `RoomRepository`, `MessageRepository`).
- `domain`: `Reservation`, `Room`, os enums `RoomType`, `RatePlan` e `ReservationStatus`, mais `ReceiptDocument` e `Message`.

Um relatório segue este caminho: a rota `/reports/...` (`App.java:51-54`) chama o handler, que valida `date` ou `month` (`ApiHandler.java:133-207`, quatro métodos quase iguais). O handler chama `reportService.generateXxxReport`, que consulta o `ReservationRepository` e devolve `text/csv; charset=utf-8`. A porta vem de `PORT`, com 7070 como padrão (`App.java:64-74`). Os testes do agente são unitários dos serviços: `PricingServiceTest`, `ReservationServiceTest` e `ReportServiceTest`, este com casos para os quatro relatórios e para um plano sem reservas.

**A parte que pede o pattern.** Cada relatório é um método público de `ReportService`:
- ocupação: `ReportService.java:32-59`;
- financeiro: `ReportService.java:61-91`;
- taxa de turismo: `ReportService.java:93-125`;
- planos de tarifa: `ReportService.java:127-163`.

Não há nenhum tipo que represente "um relatório", nada que escolha o caso: quem escolhe é a rota, chamando o método certo. Cada método faz tudo por conta própria: filtra e ordena as reservas, cria um `new ReportBuilder()`, chama `addTitleLine(POUSADA_NAME, "<nome>", <período>)` (linhas 42, 74, 104, 137), depois `addHeaderLine`, um laço de `addLine`, `addTotalLine` e `build()`. O `ReportBuilder` (`ReportService.java:169-191`) tem quatro métodos que fazem a mesma coisa, um join por vírgula. Ele não guarda a ordem dos passos, não chama nada de quem o usa e não garante que o título venha primeiro nem que o total venha por último.

**O raciocínio.**
- **O que aproxima do pattern:** existe uma peça comum, e o nome da pousada está numa constante só (`ReportService.java:17`).
- **O que afasta dele:** o que o Template Method pede é que a sequência título, cabeçalho, linhas e total fique num lugar só, com os relatórios preenchendo só os passos que mudam. Aqui a sequência está escrita quatro vezes, e o título também.
- **Por que não é alternativa equivalente:** a composição aceita é um escritor genérico que recebe as partes que variam e conduz os passos. O `ReportBuilder` não conduz nada.
- **Por que não é omissão:** o que existe é exatamente o caso que as instruções chamam de malfeito, "o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos". Não são quatro relatórios escritos inteiros sem nenhuma peça comum. A mudança anunciada (a linha final com a data) teria de ser feita nos quatro métodos.

**Exagero colateral.** Não encontrei: não há interfaces, fábricas nem camadas que só repassam. O `ReportBuilder` é pequeno e privado.

**Comportamento.** Conferi por leitura e as regras dos relatórios batem com o pedido. Fora deles há um desvio no tratamento de erro (detalhes no item abaixo).

## Item a item

**O pattern pedido.** Os quatro relatórios são métodos independentes de `ReportService` (`ReportService.java:32`, `:61`, `:93`, `:127`). Cada um repete a sequência completa:
- título: `:42`, `:74`, `:104`, `:137`;
- cabeçalho: `:43`, `:75`, `:105`, `:138`;
- laço de linhas: `:45`, `:77`, `:107`, `:143`;
- total: `:56`, `:88`, `:122`, `:160`.

A única peça comum é o `ReportBuilder` (`ReportService.java:169-191`), um utilitário de junção por vírgula sem método-modelo. Não há classe abstrata com passos abstratos, nem escritor genérico que receba as partes variáveis. A ordem dos passos e a linha de título continuam espalhadas pelos quatro, então não é o Template Method que o texto pedia. Também não é a composição aceita como equivalente. É o caso de "esqueleto" reduzido a utilitário de CSV.
Conclusão: malfeito

**O exagero colateral.** Não há. A estrutura é enxuta: serviços concretos, repositórios concretos e um auxiliar privado (`ReportService.java:169`), sem interface de implementação única, fábrica nem builder fora do lugar.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem subir o serviço.

Relatórios (todos conferem):
- **Títulos:** os quatro têm "Pousada Maré Alta,<nome>,<período>", com `occupancy`/data e `revenue`, `tourism-tax`, `rate-plans`/mês (`ReportService.java:42,74,104,137`; `YearMonth.toString()` dá `2027-07`).
- **Ocupação:** reservas pagas com `checkIn <= data < checkOut` (`ReservationRepository.java:40-45`), ordenadas por quarto, total `n/5` (`ReportService.java:56`).
- **Financeiro:** reservas que começam no mês com pago menos devolvido maior que zero (`ReportService.java:64`; `Reservation.java:220-226` na saída concatenada), ordenadas por entrada e depois por id. Uma não reembolsável cancelada conta, porque o reembolso é 0 (`ReservationService.java:88-92`).
- **Taxa de turismo:** `isPaid` (CONFIRMED, CHECKED_IN, CHECKED_OUT), na mesma ordem do financeiro, com as colunas pedidas. A taxa é 4,50 × hóspedes × noites (`PricingService.java:255,285-290`), e 2×3 dá 27.00.
- **Planos:** mesmo filtro do financeiro (`ReportService.java:130`), uma linha por `RatePlan.values()` na ordem STANDARD, NON_REFUNDABLE, mesmo sem reservas (`getOrDefault`, `:144`), e total `total,qtd,valor` (`:160`).

Resto do sistema (regras de diária, alta temporada, desconto HALF_EVEN, transições, cancelamento e reembolso, conflito de quarto, capacidade, comprovante e e-mail): conferem na leitura. Os desvios:
- **Corpo que não é JSON:** o enunciado pede 400. Pela leitura, cai em 500. `ctx.bodyAsClass` (`ApiHandler.java:59`) lança uma exceção do Jackson, que não é `IllegalArgumentException`, então não é pega em `:75`. Ela vai para o `handleException`, que devolve 500 `INTERNAL_ERROR` (`ApiHandler.java:228-230`).
- **`amountPaid` zero no JSON:** sai como `0`, e não com 2 casas, porque é `BigDecimal.ZERO` (`Reservation.java:134`). O enunciado pede número com 2 casas.
- **Formatação de dinheiro no comprovante:** depende da localidade da JVM (`ReservationService.java:232`).

Conclusão: em parte

## Dúvidas

- Não rodei o projeto, então o 500 para corpo que não é JSON é dedução: tomei como certo que o Javalin 6 deixa passar a exceção do Jackson em `bodyAsClass` sem convertê-la em 400. Se ele já a converter, o comportamento ficaria em `segue`, a não ser pelo detalhe do `0` em `amountPaid`.
- Pensei entre `malfeito` e `omissão`. O `ReportBuilder` não guarda nenhum passo e o título está copiado nos quatro, o que lembra omissão. Escolhi `malfeito` porque as instruções descrevem com exatidão este caso, o do utilitário de CSV com a sequência repetida, como malfeito.
