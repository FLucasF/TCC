# Cenário 09

## Visão geral

O agente escreveu os quatro relatórios como quatro métodos completos e independentes dentro de `ReportService`. Não há esqueleto comum: cada método escreve por conta própria a linha de título, o cabeçalho, o laço das linhas e a linha do total. Isso é a omissão que as instruções do pattern descrevem, e a linha de título aparece copiada quatro vezes (`main/java/com/pousada/service/ReportService.java:28`, `:63`, `:103`, `:137`). O que está bom: não há estrutura sobrando, e os dois relatórios novos seguem o pedido pela leitura (títulos, colunas, taxa de R$ 4,50, os dois planos sempre presentes). O problema de desenho mais sério é que a próxima mudança "em todos de uma vez", como a linha final com a data, teria de ser feita à mão em quatro lugares. No comportamento, a ocupação conta reservas pendentes, quando deveria contar só as pagas, e há falhas fora dos relatórios: reservas encostadas umas nas outras são recusadas, erros de entrada devolvem 500 e há segunda via para reserva cancelada.

## Como foi implementado, e por que o julgamento

**O projeto.** Fica em `projeto/pousada-mare-alta/`, com o pacote `com.pousada` dividido assim:
- `model`: `Reservation`, `Room`, `RatePlan`, `ReservationStatus`, `RoomType`, `Message`.
- `dto`: `ReservationDTO`, `ComprovanteDTO`, `RoomDTO`, `MessageDTO`, `ErrorDTO`.
- `repository`: `ReservationRepository`, `RoomRepository` e `MessageRepository`, todos em memória. As reservas ficam num `LinkedHashMap` com um contador de id que começa em 1 (`main/java/com/pousada/repository/ReservationRepository.java:10-11`).
- `service`: `ReservationService` (criação, preço, transições de estado, reembolso), `DocumentService` (texto do comprovante), `MessageService` (e-mail guardado na lista) e `ReportService` (os quatro CSV).
- `util`: `PricingUtils` (arredondamento `HALF_EVEN` em 2 casas) e `SeasonUtils` (alta temporada).

`Application.main` monta tudo à mão: cria os repositórios, depois os serviços, sobe o Javalin na porta de `PORT` ou na 7070 (`main/java/com/pousada/Application.java:37-41`) e registra as rotas como lambdas no mesmo método. Uma requisição segue este caminho: a lambda lê o corpo ou o parâmetro, chama o serviço, o serviço lê e grava no repositório, e a lambda converte o resultado num DTO. Os erros saem do serviço como `RuntimeException` cuja mensagem é o código, e `handleError` traduz essa mensagem num status HTTP com um `switch` (`main/java/com/pousada/Application.java:282-292`).

Os testes do agente são de unidade, sobre os serviços: 10 em `ReservationServiceTest` e 4 em `ReportServiceTest`, um por relatório. Não há teste que passe pela API HTTP.

**A parte que pede o pattern.** Cada relatório é um método público de `ReportService`:
- `generateOccupancyReport` (`main/java/com/pousada/service/ReportService.java:26-59`)
- `generateRevenueReport` (`:61-99`)
- `generateTourismTaxReport` (`:101-133`)
- `generateRatePlansReport` (`:135-178`)

Os quatro repetem a mesma sequência inteira:
1. criam um `StringBuilder`;
2. escrevem `"Pousada Maré Alta,<nome>,"` com o período (`:28`, `:63`, `:103`, `:137`);
3. escrevem o cabeçalho fixo (`:29`, `:64`, `:104`, `:138`);
4. escolhem e ordenam as reservas, cada um com o seu filtro;
5. fazem o laço que escreve as linhas e acumula o total;
6. escrevem a linha `total` (`:56`, `:96`, `:130`, `:175`).

Também se repetem a formatação de datas (`DateTimeFormatter.ofPattern("yyyy-MM-dd")`, várias vezes por método) e a regra "o que ficou com a pousada" (`:74-93` e de novo em `:152-160`). O valor da taxa é uma constante local do método (`:107`). Cada relatório é escolhido pela rota que chama o método correspondente (`main/java/com/pousada/Application.java:223`, `:240`, `:257`, `:274`), e as quatro rotas também repetem a validação do parâmetro.

**O raciocínio do julgamento.**
- Para ser acerto faltam a classe abstrata com o método-modelo e os passos abstratos.
- Para ser alternativa equivalente falta um escritor genérico que receba as partes que variam.
- Não chega a malfeito, porque não existe nem uma estrutura parcial nem uma função utilitária de CSV que algum relatório use. Os únicos auxiliares são `formatAmount` e `isReservationOnDate` (`main/java/com/pousada/service/ReportService.java:180-186`), que não montam nenhum passo do relatório.

Isso é exatamente "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título": omissão.

No exagero colateral não pesou nada. Não há interfaces, fábricas nem camadas que só repassam; os repositórios e serviços são classes concretas, cada uma com um papel real.

No comportamento, os relatórios novos estão corretos pela leitura. Pesaram contra:
- a ocupação inclui reservas `PENDING`;
- a checagem de disponibilidade recusa uma reserva que começa no dia em que outra termina;
- várias entradas inválidas dão 500 em vez de 400;
- a segunda via sai para reserva cancelada.

## Item a item

**O pattern pedido.** Não há esqueleto comum. Os quatro métodos de `ReportService` escrevem cada um a sua linha de título (`main/java/com/pousada/service/ReportService.java:28`, `:63`, `:103`, `:137`), o seu cabeçalho (`:29`, `:64`, `:104`, `:138`), o seu laço de linhas (`:37-54`, `:73-94`, `:114-128`, `:166-173`) e a sua linha de total (`:56`, `:96`, `:130`, `:175`). Não existe classe abstrata, método-modelo nem escritor genérico que receba as partes que variam. A ocupação e o financeiro, que já existiam, e os dois relatórios novos estão escritos por inteiro do mesmo jeito. Uma mudança no formato de todos, como a linha final com a data que o dono anunciou, exige editar os quatro métodos.
Conclusão: omissão

**O exagero colateral.** Não há. A estrutura é enxuta: serviços e repositórios concretos, montados à mão em `main/java/com/pousada/Application.java:21-29`, sem interfaces de uma implementação só, fábricas ou builders.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

O que confere:
- Os quatro títulos saem no formato `Pousada Maré Alta,<nome>,<período>`, com os nomes `occupancy`, `revenue`, `tourism-tax` e `rate-plans` e o período da chamada (`main/java/com/pousada/service/ReportService.java:28`, `:63`, `:103`, `:137`).
- Taxa de turismo:
  - filtra `CONFIRMED`, `CHECKED_IN` e `CHECKED_OUT` das reservas que começam no mês (`:109-110`);
  - ordena por entrada e depois por número, como o financeiro (`:111`);
  - calcula 4,50 × hóspedes × noites (`:115-117`): 2 hóspedes por 3 noites dão 27.00;
  - tem as colunas pedidas (`:104`) e o total no fim (`:130`).
- Planos de tarifa: itera `RatePlan.values()` e por isso escreve sempre `STANDARD` e depois `NON_REFUNDABLE`, mesmo sem reservas (`:145-148`, `:166-173`). Conta o mesmo conjunto do financeiro, inclusive o não reembolsável cancelado (`:156-159`), e escreve `total,<qtd>,<valor>` (`:175`).
- Financeiro: as reservas pagas entram com pago menos devolvido, e o não reembolsável cancelado continua contando (`:74-93`).
- Fora dos relatórios: o preço com alta temporada e `HALF_EVEN` (`main/java/com/pousada/service/ReservationService.java:118-141`), as transições de estado (`:61-116`), o comprovante (`main/java/com/pousada/service/DocumentService.java:40-74`) e o e-mail (`main/java/com/pousada/service/MessageService.java:102-121`) seguem o pedido.

O que não confere:
- **Ocupação com pendentes.** O enunciado pede só as reservas pagas, mas o filtro exclui apenas `CANCELLED` (`main/java/com/pousada/service/ReportService.java:40`). Uma reserva `PENDING` aparece na lista e entra na contagem `total,x/5`.
- **Reservas encostadas recusadas.** A checagem de disponibilidade usa `!r.getCheckOut().isBefore(checkIn)` (`main/java/com/pousada/repository/ReservationRepository.java:56`). Uma reserva que entra no dia em que outra sai recebe `ROOM_UNAVAILABLE`, sem haver conflito de noite.
- **500 em vez de 400.** Data fora do formato, `ratePlan` inválido, campo de tipo errado ou corpo que não é JSON levantam exceções cuja mensagem não é um código conhecido. `handleError` cai no `default` e devolve 500 `INTERNAL_ERROR` (`main/java/com/pousada/Application.java:75-77`, `:291`). Uma mensagem `null` provoca um `NullPointerException` no próprio `switch`.
- **Segunda via de cancelada.** `/document` bloqueia só `PENDING` (`main/java/com/pousada/Application.java:176`), então emite a segunda via de uma reserva `CANCELLED`.
- **Reservas sem dinheiro listadas.** Um não reembolsável cancelado ainda pendente (pago 0) aparece no financeiro com 0.00 e é contado nos planos (`main/java/com/pousada/service/ReportService.java:83-92`, `:156-159`), embora não tenha deixado dinheiro.

Conclusão: em parte

## Dúvidas

- **Locale do CSV.** `formatAmount` usa `String.format("%.2f", ...)` sem fixar o locale (`main/java/com/pousada/service/ReportService.java:185`). Numa JVM com locale pt-BR, os valores do CSV sairiam como `27,00` e não `27.00`. Pela leitura, não dá para saber em qual locale o serviço roda.
- **Formato da linha de total.** O financeiro escreve `total,,,,<valor>` (`:96`) e a taxa `total,,,,,<valor>` (`:130`). O enunciado diz para a taxa "a última linha é 'total' e o valor", o que também pode ser lido como `total,<valor>`. Não contei isso como erro.
- **JSON.** O `ObjectMapper` configurado em `main/java/com/pousada/Application.java:32-34` nunca é entregue ao Javalin, que usa o mapper padrão dele. Não verifiquei se as datas saem como `2027-07-10`. Valores `BigDecimal.ZERO`, como `amountPaid` antes da confirmação e `discount` no plano normal, tendem a sair como `0`, sem as 2 casas pedidas.
