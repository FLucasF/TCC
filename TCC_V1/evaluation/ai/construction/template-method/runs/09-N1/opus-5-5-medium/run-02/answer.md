# Cenário 09

## Visão geral

O agente entregou os quatro relatórios pedidos, todos começando com a linha de título, mas sem nenhuma estrutura de relatório. Os quatro são métodos independentes de `ReportService`, e cada um repete a sequência inteira: filtrar, ordenar, escrever título, cabeçalho, linhas e total. A linha de título está copiada à mão quatro vezes (`ReportService.java:32`, `:60`, `:91`, `:128`). Não existe esqueleto em lugar nenhum, então a mudança anunciada no enunciado (uma linha final com a data em todos os relatórios) teria de ser feita em quatro lugares. O desenho é uma omissão. A parte boa é que o título, a taxa de turismo e a ordem dos planos estão certos. O problema de comportamento mais sério é que o financeiro e o relatório por plano deixam de fora a reserva não reembolsável cancelada, que o enunciado manda contar.

## Como foi implementado, e por que o julgamento

**O projeto.**
- **Pacotes.** Há quatro: `domain` (`Reservation`, `Room`, `RoomType`, `RatePlan`, `ReservationStatus`, `Message`), `service` (`ReservationService`, `ReportService` e três repositórios concretos em memória, feitos com `HashMap`), `util` (`PriceCalculator`, com métodos estáticos de preço e arredondamento `HALF_EVEN`, `PriceCalculator.java:486-488`) e `api` (`ApiController`, que registra todas as rotas).
- **Montagem e subida.** `Application.main` monta tudo à mão, lê `PORT` com 7070 como padrão e sobe o Javalin (`Application.java:498-518`).
- **Caminho de uma requisição.** A rota em `ApiController` valida a entrada e as transições de estado, chama `ReservationService` ou `ReportService`, e estes leem dos repositórios. Para os relatórios, cada rota interpreta `date`/`month`, chama um método `generateXxxReport` e devolve `text/csv;charset=utf-8` (`ApiController.java:269-331`).
- **Testes.** São JUnit 4 sobre os serviços e o `PriceCalculator`. O `ReportServiceTest` tem um teste por relatório, que confere o título e algumas linhas com `contains`.

**A parte que pede o pattern.** Os quatro relatórios são quatro métodos públicos da mesma classe:
- **Ocupação** (`ReportService.java:22-47`): faz o próprio laço de seleção, ordena por quarto, escreve título, cabeçalho, linhas e `total,n/5`.
- **Financeiro** (`ReportService.java:49-78`): mesmo laço, ordenação por dia de entrada e número, título, cabeçalho, linhas e total.
- **Taxa de turismo** (`ReportService.java:80-114`): copia a seleção e a ordenação do financeiro (`:81-88` é idêntico a `:50-57`) e tem a taxa de R$ 4,50 como variável local (`:95`).
- **Por plano** (`ReportService.java:116-154`): agrupa num `HashMap` e percorre `STANDARD` e `NON_REFUNDABLE` numa lista fixa.

O que é compartilhado são só funções auxiliares privadas: filtros (`isPaid`, `isPaidAndStartsInMonth`), formatação e `calculateAmount` (`:156-188`). Não há classe abstrata, interface de relatório, nem escritor genérico que receba as partes que variam. A sequência título → cabeçalho → linhas → total está escrita por extenso em cada um dos quatro métodos.

**O raciocínio.** As instruções dizem que é omissão quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título. É exatamente o caso. Não chega a ser malfeito, porque nenhum relatório passa por um esqueleto. Os auxiliares de formatação nem formam uma função utilitária de CSV que escreva os passos. O que aproxima o código do pattern é pouco: alguns predicados e formatadores reaproveitados.

**Exagero colateral.** Não há interfaces de uma implementação só, fábricas, builders nem camadas de repasse. Os repositórios são classes concretas simples.

**Comportamento.**
- **Correto:** os títulos, a taxa e a ordem dos planos.
- **Errado:** o financeiro e o relatório por plano filtram por `isPaid`, que só aceita `CONFIRMED`/`CHECKED_IN`/`CHECKED_OUT` (`ReportService.java:167-170`, usado em `:52` e `:122`). A reserva não reembolsável cancelada, que manteve o pagamento, fica de fora.
- **Detalhes menores:**
  - Os valores do CSV usam `String.format("%.2f")` com o locale padrão (`:187`), o que vira vírgula numa JVM em pt-BR.
  - O comprovante não põe separador de milhar (`ReservationService.java:149`).

## Item a item

**O pattern pedido.** Cada relatório é um método completo que repete a mesma sequência de passos:
- título escrito à mão em `ReportService.java:32`, `:60`, `:91` e `:128`;
- cabeçalho em `:33`, `:61`, `:92` e `:129`;
- laço de linhas e total próprios em `:35-44`, `:64-75`, `:97-111` e `:134-151`.

A seleção e a ordenação do financeiro estão copiadas na taxa de turismo (`:50-57` e `:81-88`). Não existe método-modelo nem escritor genérico; o que se compartilha são só auxiliares de filtro e de formatação (`:156-188`). Os quatro relatórios, inclusive os dois que já existiam, ficaram escritos inteiros com a sua linha de título, o que as instruções definem como omissão. Quando o enunciado pedir a linha final com a data, será preciso mexer em quatro lugares.
Conclusão: omissão

**O exagero colateral.** Não há:
- os repositórios são classes concretas sem interface (`ReservationRepository.java`, `RoomRepository.java`);
- o `PriceCalculator` é um utilitário estático;
- a montagem é direta em `Application.java:498-504`.

Não há abstração sem uso fora da parte do pattern.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço nem chamei a API.

O que confere:
- Os títulos seguem "Pousada Maré Alta,<nome>,<período>" com `occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada (`ReportService.java:32`, `:60`, `:91`, `:128`).
- A taxa é calculada como hóspedes × noites × 4,50 (`:95-100`), com as colunas pedidas (`:92`), as mesmas reservas pagas e a mesma ordem do financeiro.
- O relatório por plano tem sempre as duas linhas, `STANDARD` e depois `NON_REFUNDABLE`, mesmo vazias (`:117-119`, `:134`).
- A ocupação conta quem entra na noite e não quem sai (`:158`), ordena por quarto e escreve `total,n/5` (`:44`).

O que não confere:
- O financeiro e o relatório por plano usam `isPaid` (`:52`, `:122`, `:167-170`), que exclui `CANCELLED`. Com isso, a reserva não reembolsável cancelada, que deixou dinheiro com a pousada, não aparece em nenhum dos dois, contra o que o enunciado pede explicitamente.

Detalhes menores:
- As linhas de total do financeiro e da taxa têm uma coluna vazia a menos do que o cabeçalho (`:75`, `:111`).
- Os valores do CSV saem de `String.format("%.2f")` com o locale padrão (`:187`).
- Fora dos relatórios, o comprovante formata o dinheiro sem separador de milhar ("R$ 1234,56", `ReservationService.java:149`), quando o pedido é "R$ 1.234,56".
Conclusão: em parte

## Dúvidas

- Não fica decidido só pela leitura como o mapeador JSON padrão do Javalin 6 serializa os `LocalDate` de `checkIn`/`checkOut` (`ApiController.java:357-358`). O `jackson-datatype-jsr310` está no `pom.xml`, mas a configuração do mapeador não aparece no projeto, então as datas podem sair como `"2027-07-10"` ou como array.
- O formato exato da linha "total" do financeiro não está fixado no enunciado, então o preenchimento de colunas vazias em `:75` foi tratado como detalhe menor, e não como falha.
