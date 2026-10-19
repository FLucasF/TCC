# Cenário 01

## Visão geral

O agente montou um sistema enxuto e correto. A parte comum ficou num serviço único, com o domínio em enums e records. Os quatro relatórios são classes que implementam uma interface `Report`, e uma função compartilhada, `Csv.build`, escreve na ordem certa o título, o cabeçalho, as linhas e o total. A linha de título fica num lugar só, e as quatro rotas passam pelo mesmo laço. O problema mais sério é o desenho: esse "esqueleto" é uma função utilitária de formatação que recebe valores já prontos. Cada relatório ainda conduz sozinho, no seu `generate`, a sequência inteira: ler o período, escolher e ordenar as reservas, montar as linhas, somar o total e só então chamar o formatador. Nenhum passo variável é entregue ao esqueleto como método abstrato ou como função. O comportamento confere em tudo o que testei.

## Como foi implementado, e por que o julgamento

**O projeto.** Há três pacotes:
- **`br.com.maretalta`** tem:
  - `Main`, que lê `PORT` e a configuração da taxa e sobe o servidor;
  - `App`, que registra as rotas Javalin, lê o JSON à mão com Jackson e serializa as respostas;
  - `ReservationService`, que guarda os quartos e as reservas numa lista em memória e aplica as regras de criação, confirmação, cancelamento, check-in, check-out e segunda via;
  - `Outbox`, a lista de mensagens enviadas;
  - `ReceiptFormatter`, o texto do comprovante e do e-mail;
  - `ApiException`, os erros com código.
- **`domain`** tem `Money` (arredondamento meio para o par e formatação), `Pricing` (preço por noite com alta temporada e a cotação), os enums `RatePlan`, `RoomType` e `Status`, o record `Room` e a entidade `Reservation`.
- **`report`** tem os quatro relatórios, a interface `Report`, o utilitário `Csv` e `TourismTaxPolicy`.

Uma requisição segue um caminho curto: a rota em `App` (por exemplo `src/main/java/br/com/maretalta/App.java:63`) chama o método do serviço, que valida, altera a `Reservation` sob `synchronized` e a devolve para ser serializada (`App.java:159-178`). O sistema é montado no construtor de `App` (`App.java:45-54`) e sobe em `Main.java:8-11`. O agente escreveu 4 testes de ponta a ponta, com cliente HTTP, em `src/test/java/br/com/maretalta/AppTest.java`. Eles cobrem a alta temporada, o e-mail e o comprovante, os erros e os relatórios (`AppTest.java:66-80`).

**A parte do fim.** A interface `Report` (`src/main/java/br/com/maretalta/report/Report.java:4-12`) tem só `name()`, `paramName()` e `generate(String)`. `App` percorre a lista e cria uma rota `/reports/{nome}` para cada relatório (`App.java:74-79`). Cada relatório é uma classe:
- `OccupancyReport.java:23-37`
- `RevenueReport.java:24-37`
- `TourismTaxReport.java:29-42`
- `RatePlanReport.java:25-42`

Em todos o `generate` repete a mesma sequência:
1. ler o período (`Csv.parseDate` ou `Csv.parseMonth`);
2. abrir `service.read`;
3. escolher e ordenar as reservas, com filtro próprio ou com os auxiliares `Csv.startingIn` e `Csv.revenueReservations` (`Csv.java:50-60`);
4. transformar as reservas em linhas;
5. somar o total;
6. chamar `Csv.build(name(), periodo, cabecalho, linhas, total)`.

`Csv.build` (`Csv.java:19-26`) só concatena título, cabeçalho, linhas e total. A taxa de turismo usa `TourismTaxPolicy` (`TourismTaxPolicy.java:20-22`), com valor padrão de 4,50 e valores por ano vindos de variáveis de ambiente, o que responde ao "a prefeitura muda o valor todo ano". O relatório por plano percorre `RatePlan.values()` (`RatePlanReport.java:32-38`), e por isso os dois planos aparecem mesmo sem reserva.

**O raciocínio.** O que aproxima o desenho do pattern:
- a ordem título → cabeçalho → linhas → total está num lugar só, e nenhum relatório escreve a própria linha de título;
- os quatro relatórios, ocupação e financeiro inclusive, passam pela mesma função.

O que afasta:
- não há método-modelo nem passos abstratos (`Report.java:11` é um único `generate` que cada classe escreve inteiro);
- também não é a composição que as instruções aceitam: `Csv.build` não recebe a seleção, a linha e o total como objetos ou funções, recebe strings e listas já calculadas;
- quem conduz os passos é cada relatório. A leitura do período se repete em `RevenueReport.java:25`, `TourismTaxReport.java:30` e `RatePlanReport.java:26`. A seleção e a soma se repetem em `RevenueReport.java:27-33`, `TourismTaxReport.java:32-38` e `RatePlanReport.java:28-38`.

Um quinto relatório (cancelamentos, hóspedes por estado) teria de reescrever essa sequência e lembrar de chamar `Csv.build`. Isso corresponde ao caso que as instruções descrevem como "o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos". Por isso a classe é `malfeito`.

No exagero, não achei estrutura sobrando fora da parte do fim. A interface `Report` tem quatro implementações e é a própria parte do fim, e `TourismTaxPolicy` atende ao pedido da taxa anual. No comportamento, todos os valores, recusas e títulos conferiram.

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios implementam `Report` (`src/main/java/br/com/maretalta/report/Report.java:4-12`) e usam `Csv.build` (`src/main/java/br/com/maretalta/report/Csv.java:19-26`) para escrever título, cabeçalho, linhas e total. Porém cada `generate` executa sozinho a sequência ler período → selecionar e ordenar → linhas → total → formatar:
- `src/main/java/br/com/maretalta/report/OccupancyReport.java:24-36`
- `src/main/java/br/com/maretalta/report/RevenueReport.java:25-35`
- `src/main/java/br/com/maretalta/report/TourismTaxReport.java:30-40`
- `src/main/java/br/com/maretalta/report/RatePlanReport.java:26-40`

Não há classe abstrata com método-modelo, nem um escritor genérico que receba a seleção, a linha e o total como funções. A linha de título não está copiada, mas o "esqueleto" é um utilitário de formatação, e a sequência dos passos continua em cada relatório.
Conclusão: malfeito

**O exagero colateral.** Não há. As estruturas extras ficam na parte do fim e se justificam: `Report` tem quatro implementações, e `TourismTaxPolicy` (`src/main/java/br/com/maretalta/report/TourismTaxPolicy.java:9-38`) atende à taxa anual. Fora dela há só classes concretas: `ReservationService`, `Outbox`, `ReceiptFormatter`, e `Pricing` e `Money` como utilitários estáticos. Não há interface de implementação única, fábrica, builder ou camada que só repassa.
Conclusão: contenção

**O comportamento.** Testei numa cópia, com o jar sombreado `target/reservas-1.0.0.jar` na porta 7070. Criei seis reservas: as de julho com estados variados (confirmada; não reembolsável confirmada e cancelada; standard confirmada e cancelada com devolução; pendente; não reembolsável com check-out) e uma de março. Resultados:
- **Preço:** a 1 (101, de 10/07 a 12/07) deu 599,76. A não reembolsável no 201 deu subtotal 1403,64, desconto 210,55 e total 1193,09.
- **Ocupação:** em 2027-07-10 começa com `Pousada Maré Alta,occupancy,2027-07-10`, lista 101 e 102 e fecha com `total,2/5`. Em 2027-07-12 quem sai não conta, e veio `total,1/5`.
- **Financeiro:** em 2027-07 veio a ordem 2, 1, 5 (por entrada e depois por número), com a não reembolsável cancelada a 1193,09 e `total,2557.54`. A standard cancelada com devolução e a pendente ficaram de fora.
- **Taxa de turismo:** em 2027-07 veio o título certo, as colunas `reservation,room,check_in,check_out,guests,nights,tax`, as linhas 18.00 (2×2) e 27.00 (2×3) e `total,45.00`. A cancelada ficou de fora.
- **Por plano:** em 2027-07 veio `STANDARD,1,599.76`, `NON_REFUNDABLE,2,1957.78` e `total,3,2557.54`, batendo com o financeiro. Em 2027-08 os dois planos aparecem zerados e o total é `total,0,0.00`.
- **Recusas dos relatórios:** todos respondem `text/csv; charset=utf-8`. `month=2027-7`, mês ausente e `date=2027-13-01` dão 400 `INVALID_REQUEST`.
- **Parte comum:**
  - a segunda via do exemplo (201, de 10/03 a 12/03) veio com 779,80;
  - o e-mail tem o assunto, o texto e o comprovante como o enunciado pede, com o CPF formatado;
  - a segunda via de reserva pendente dá 409 e o check-in de reserva cancelada dá 409 `OPERATION_NOT_ALLOWED`;
  - quarto ocupado dá 409, capacidade excedida dá 422, quarto inexistente dá 404, corpo que não é JSON dá 400 e reserva inexistente dá 404;
  - o quarto liberado por um cancelamento pode ser reservado de novo.

O `mvn -q verify` da cópia passou: 4 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

A fronteira entre `malfeito` e `alternativa equivalente` é a única dúvida. A favor da alternativa: os quatro relatórios passam pela mesma `Csv.build`, que recebe as partes variáveis (nome, período, colunas, linhas, total) e põe o título e a ordem num lugar só. Por isso nenhum relatório tem a linha de título copiada. Fiquei com `malfeito` porque essas partes chegam como valores já calculados, não como objetos ou funções que o esqueleto chame. A seleção, a montagem das linhas e o total continuam conduzidos por cada `generate`, que é o caso que as instruções descrevem como malfeito.
