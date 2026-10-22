# Cenário 09

## Visão geral

O agente escreveu os quatro relatórios como quatro métodos inteiros e independentes dentro de uma única `ReportService`. Cada método monta o próprio `StringBuilder`, escreve a própria linha de título, o próprio cabeçalho, filtra, ordena, escreve as linhas e o total. Não existe esqueleto comum, nem classe abstrata, nem escritor genérico. A linha de título nova foi copiada nos quatro métodos, e o filtro e a ordenação do financeiro foram copiados nos novos. Isso é a omissão descrita nas instruções. O resto do desenho é simples e não tem exagero. Pela leitura, os relatórios novos estão certos, mas há problemas de comportamento: a ocupação conta reservas pendentes, que não foram pagas. O comprovante escreve o dinheiro sem separador de milhar. E `String.format("%.2f")` depende do locale padrão.

## Como foi implementado, e por que o julgamento

**O projeto.** Ele está em `com.pousada` e tem quatro pacotes:
- `api`: handlers estáticos que registram as rotas no Javalin (`RoomsApiHandler`, `ReservationsApiHandler`, `MessagesApiHandler`, `ReportsApiHandler`).
- `domain`: `Reservation`, `Room`, `Receipt`, `Message` e os enums `RatePlan`, `RoomType` e `ReservationStatus`.
- `repository`: mapas e listas em memória. `RoomRepository` cria os cinco quartos no construtor (`src/main/java/com/pousada/repository/RoomRepository.java:9-14`), e `ReservationRepository` numera as reservas a partir de 1 com um `AtomicLong`.
- `service`: `PricingService` faz o preço, a alta temporada e o desconto. `ReservationService` cuida das transições de estado, do comprovante e do e-mail guardado no `MessageRepository`. `ReportService` faz os relatórios.

`Application.main` instancia tudo à mão, registra as rotas e sobe na porta `PORT` ou na 7070 (`src/main/java/com/pousada/Application.java:18-37`). O jar sai do `maven-shade-plugin`. Uma requisição segue este caminho: rota no handler, extração e validação dos parâmetros, chamada ao service, que lê e grava no repositório. Os erros viram `IllegalArgumentException` com o código na mensagem, e um `switch` em `handleError` os traduz em status HTTP. **Não há testes**: o projeto não tem `src/test`, e o JUnit 4 aparece no `pom.xml` sem nenhum teste.

**A parte que pede o pattern.** Os quatro relatórios são `generateOccupancyReport`, `generateRevenueReport`, `generateTourismTaxReport` e `generateRatePlansReport` (`src/main/java/com/pousada/service/ReportService.java:33`, `:73`, `:113`, `:154`). Cada um repete a mesma sequência:
1. cria um `StringBuilder`;
2. escreve o título (`POUSADA_NAME` + `",<nome>,"` + período) nas linhas `:36`, `:76`, `:116` e `:157`;
3. escreve o cabeçalho literal nas linhas `:37`, `:77`, `:117` e `:158`;
4. seleciona e ordena;
5. percorre as linhas acumulando o total;
6. escreve `"total,..."` nas linhas `:68`, `:108`, `:149` e `:196`.

O filtro e o comparador do financeiro (`:79-90`) foram copiados no da taxa (`:119-129`) e no dos planos (`:160-166`). O cálculo "pago menos devolvido" também foi copiado (`:94-96` e `:181-183`). Os únicos pontos comuns são `formatDate` e `formatMoney` (`:201-207`), funções utilitárias que não guardam a ordem dos passos. A camada de rotas repete o mesmo padrão: quatro blocos quase iguais (`src/main/java/com/pousada/api/ReportsApiHandler.java:12`, `:31`, `:50`, `:69`).

**O raciocínio.** As instruções pedem o esqueleto (título, cabeçalho, linhas, total) num lugar só, com cada relatório preenchendo os passos que mudam. Aqui não há esqueleto nenhum, nem para uma parte dos relatórios. Isso afasta o caso de `malfeito`, que pressupõe que a estrutura exista para algum dos relatórios. Os quatro relatórios estão escritos inteiros, cada um com a sua linha de título, que é a definição literal de `omissão`. A consequência prática é a que o dono da pousada quer evitar: a linha com a data de emissão do ano que vem teria de ser acrescentada em quatro lugares, e os próximos relatórios seriam mais cópias. Não há exagero colateral: são classes concretas, sem interfaces nem fábricas. No comportamento, pesaram o filtro da ocupação, a formatação do comprovante e a dependência de locale.

## Item a item

**O pattern pedido.** Os quatro relatórios são métodos independentes que repetem a sequência inteira, título incluído (`src/main/java/com/pousada/service/ReportService.java:36`, `:76`, `:116`, `:157`). A seleção e a ordenação foram copiadas (`:79-90`, `:119-129`, `:160-166`), e a linha de total foi escrita à mão em cada um (`:68`, `:108`, `:149`, `:196`). Não existe método-modelo, classe base nem escritor genérico que receba as partes que variam. O que é compartilhado são só os formatadores `formatDate` e `formatMoney` (`:201-207`). Não é Template Method nem a composição equivalente.
Conclusão: omissão

**O exagero colateral.** Não há. Os services, os repositórios e os handlers são classes concretas, cada uma com papel real. Não há interface com implementação única, fábrica, builder nem camada que só repassa. `Application` monta tudo diretamente (`src/main/java/com/pousada/Application.java:18-32`).
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço nem rodar o Maven, como a correção pede.

O que confere:
- **Títulos:** os quatro relatórios têm o título com o nome e o período certos (`ReportService.java:36`, `:76`, `:116`, `:157`). `YearMonth.toString()` dá `2027-07` e `LocalDate.toString()` dá `2027-07-10`.
- **Taxa de turismo:** usa só as reservas pagas do mês (`:121-123`), na ordem do financeiro (`:124-128`), com as colunas pedidas (`:117`) e 4,50 × hóspedes × noites (`:133-136`). 2 hóspedes por 3 noites dão `27.00`, e o total vem no fim (`:149`).
- **Planos:** percorre `RatePlan.values()` na ordem STANDARD e depois NON_REFUNDABLE (`:176`; `domain/RatePlan.java:4-5`), mesmo sem reservas (`getOrDefault`, `:177`). Usa o mesmo filtro do financeiro, com a não reembolsável cancelada incluída (`:162-165`), e a linha `total,<qtd>,<valor>` (`:196`).
- **Financeiro:** filtro, ordenação, colunas e total como pedido (`:77-108`).
- **Preço e estados:** o preço com alta temporada, desconto e HALF_EVEN está em `PricingService.java:12-41`. As transições e o reembolso estão em `ReservationService.java:73-190` e conferem.

Os problemas:
1. **A ocupação conta reservas pendentes.** O filtro é `status != CANCELLED` (`ReportService.java:49`), mas o enunciado pede só as pagas. Uma reserva PENDING aparece no relatório e entra na contagem `total,x/5`.
2. **O comprovante não tem separador de milhar.** `formatMoney` faz `String.format("%.2f").replace(".", ",")` (`domain/Receipt.java:105`), o que dá `R$ 1234,56` em vez de `R$ 1.234,56`. O erro aparece no e-mail de confirmação.
3. **O formato do dinheiro no CSV depende do locale.** `String.format("%.2f", value)` usa o locale padrão da JVM (`ReportService.java:206`). Numa máquina pt_BR, os CSVs sairiam `599,76` e o comprovante, depois do `replace`, sairia certo por acaso.
4. **O JSON perde as duas casas.** Ele usa `doubleValue()` (`api/ReservationsApiHandler.java:138-140`), então 600.00 sai `600.0`. Nesse ponto, a regra de "número com 2 casas" não é cumprida.

Os quatro relatórios novos e o financeiro estão certos. A ocupação e o comprovante, não.
Conclusão: em parte

## Dúvidas

- Não há testes no projeto (falta `src/test`), embora a correção diga que o projeto passou nos testes do próprio agente. Isso não muda a classe.
- A dependência de locale do ponto 3 só vira erro conforme a máquina em que o jar rodar. Contei como risco e não como falha certa.
