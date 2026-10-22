# Cenário 09

## Visão geral

O agente criou uma classe abstrata `Report` e pôs os quatro relatórios como subclasses dela: a ocupação, o financeiro, a taxa de turismo e os planos. Só que essa base não tem método-modelo nem passos abstratos. Ela tem apenas um método utilitário, `buildCsv`, que põe a linha de título na frente de um texto já pronto. Cada relatório continua fazendo inteiro, no seu `generate`, a sequência que o pedido descreve: escolher, ordenar, escrever o cabeçalho, as linhas e o total. Esse é o caso que as instruções chamam de "malfeito": o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos. Uma mudança no jeito de todos os relatórios fica fácil só se for no começo ou no fim do texto, e nada obriga um relatório novo a seguir a forma. Pela leitura do código, o comportamento confere com o pedido.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com pacotes por assunto:
- `hotel.reservations` tem as reservas, o serviço de reservas, o cálculo de preço e o armazenamento em memória (`ReservationStore`, um `LinkedHashMap`, `final/src/main/java/hotel/reservations/ReservationStore.java:12`).
- `hotel.rooms` tem os quartos.
- `hotel.messages` tem os e-mails.
- `hotel.documents` tem os recibos.
- `hotel.reports` tem os relatórios em CSV.
- `hotel.web.HttpApi` tem as rotas.

O sistema é montado à mão em `final/src/main/java/hotel/Hotel.java:21-35`, que cria o store, o serviço e os quatro relatórios e os passa ao `HttpApi`. Uma requisição como `GET /reports/tourism-tax?month=2027-07` entra em `final/src/main/java/hotel/web/HttpApi.java:99-102`. A rota lê o mês com `readMonth`, que responde `INVALID_REQUEST` quando o mês vem mal escrito (`HttpApi.java:166-173`), e chama `tourismTaxReport.generate(month)`. Esse método percorre `store.all()` e devolve o texto CSV.

O agente estendeu `ReportsTest`: pôs a linha de título nos cinco testes que já existiam e acrescentou três testes novos, um da taxa e dois dos planos (`final/src/test/java/hotel/reports/ReportsTest.java:115-156`). Não mexeu nos testes HTTP.

**A parte que pede o pattern.** A base `final/src/main/java/hotel/reports/Report.java:4-16` é `abstract`, mas não tem nenhum método abstrato. O único método, `buildCsv(reportName, period, headerAndRows)` (`Report.java:8-15`), escreve "Pousada Maré Alta,<nome>,<período>" e cola depois dele o texto recebido.

Cada subclasse tem o seu `generate`, com assinatura própria, e faz toda a sequência dentro dele:
- `OccupancyReport.java:24-31` seleciona e ordena, `:33-43` escreve o cabeçalho, as linhas e o total, e `:45` chama `buildCsv`.
- `RevenueReport.java:24-31` seleciona e ordena, `:34-46` escreve o resto, e `:48` chama `buildCsv`.
- `TourismTaxReport.java:24-31` seleciona e ordena, `:33-55` escreve o resto, e `:57` chama `buildCsv`.
- `RatePlansReport.java:21-37` agrega, `:39-60` escreve o resto, e `:62` chama `buildCsv`.

O nome do relatório e o período são passados como texto em cada chamada. Os relatórios são escolhidos pela rota, com um campo e um tipo concreto para cada um em `HttpApi`. Não existe um tipo comum que alguém use.

**O raciocínio.** O que aproxima o desenho do pattern é haver uma superclasse comum para os quatro relatórios, os dois antigos inclusive, e a linha de título estar num lugar só. O que o afasta é que o método-modelo, a parte central do Template Method, não existe. O esqueleto "título, cabeçalho, linhas, total" não está escrito em lugar nenhum da base. A ordem dos passos, o laço por reserva e a linha do total ficam repetidos em cada relatório, e a base só envolve o resultado. O pedido avisa: "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez". Com esse desenho, uma mudança no meio da forma, como o cabeçalho ou o jeito do total, teria de ser feita nos quatro arquivos, e um relatório novo pode pular passos sem que nada o impeça. Isso bate com o "malfeito" das instruções: "quando o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos".

No exagero colateral, não há estrutura sobrando. A base `Report` tem pouca coisa, mas é a tentativa de pattern, não uma estrutura colateral. Não há fábricas, interfaces de uma implementação só nem camadas que só repassam.

No comportamento, a leitura confere as regras pedidas, como mostra o item abaixo.

## Item a item

**O pattern pedido.** A classe `Report` (`final/src/main/java/hotel/reports/Report.java:4`) é abstrata, mas não tem método-modelo nem passos abstratos. Ela só oferece `buildCsv` (`Report.java:8-15`), que põe o título na frente de um texto pronto. Os quatro relatórios estendem `Report` (`OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:13`, `RatePlansReport.java:12`). Mesmo assim, cada um escreve por conta própria a seleção, a ordenação, o cabeçalho, o laço das linhas e o total (por exemplo `RevenueReport.java:24-46` e `TourismTaxReport.java:24-55`). Por isso o esqueleto não está num lugar só. Há só um auxiliar de formatação, e a sequência continua copiada em cada relatório. A estrutura do pattern existe, mas convive com o jeito sem ela.
Conclusão: malfeito

**O exagero colateral.** Não há. As únicas peças novas são a base `Report`, os dois relatórios pedidos e as duas rotas (`HttpApi.java:99-106`). Não há interface de uma implementação só, fábrica, builder nem camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código. Não subi o serviço, não rodei o Maven e não chamei a API.
- **Título.** Os quatro começam com "Pousada Maré Alta,<nome>,<período>" (`Report.java:6-12`), com os nomes `occupancy`, `revenue`, `tourism-tax` e `rate-plans` (`OccupancyReport.java:45`, `RevenueReport.java:48`, `TourismTaxReport.java:57`, `RatePlansReport.java:62`). O período é a data ou o mês da chamada.
- **Ocupação e financeiro.** Depois do título, as linhas são iguais às da base. Na diferença só mudou o nome da variável, de `csv` para `rows`.
- **Taxa de turismo.**
  - Seleção: reservas com `isPaid()`, ou seja CONFIRMED, CHECKED_IN ou CHECKED_OUT (`Reservation.java:80-84`), que começam no mês (`TourismTaxReport.java:26-27`).
  - Ordem: a mesma do financeiro, por check-in e depois por id (`TourismTaxReport.java:31` e `RevenueReport.java:32`).
  - Valor: 4.50 × hóspedes × noites, com duas casas (`TourismTaxReport.java:15` e `:39-42`). Para 2 hóspedes e 3 noites isso dá 27.00.
  - Saída: as colunas pedidas (`:34`) e o total no fim, no formato "total,<valor>" (`:55`).
- **Planos.**
  - Reservas contadas: as mesmas do financeiro, `amountKept() > 0` e começo no mês (`RatePlansReport.java:30-32`). Isso inclui a não reembolsável cancelada, que ainda tem valor retido.
  - Linhas: STANDARD e depois NON_REFUNDABLE, escritas em ordem fixa (`:45-51`), com zero quando não há reserva, porque os mapas começam zerados (`:24-27`).
  - Saída: as colunas pedidas (`:40`) e o total "total,<quantidade>,<valor>" (`:58-60`).
- **Rotas.** As rotas novas usam `readMonth`, com a mesma validação das que já existiam (`HttpApi.java:99-106`). As antigas não mudaram.

Tudo confere com o pedido.
Conclusão: segue

## Dúvidas

A dúvida foi entre "malfeito" e "acerto". Há uma superclasse comum para os quatro relatórios, e o título, que é o único passo que o pedido mudou agora, está num lugar só. Mas a base não tem método-modelo nem passo abstrato, e a sequência continua repetida em cada `generate`. É exatamente o que as instruções descrevem como "malfeito", então fiquei com essa classe.
