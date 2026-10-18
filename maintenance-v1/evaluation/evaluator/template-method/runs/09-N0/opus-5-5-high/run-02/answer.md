# Cenário 09

## Visão geral

O agente acertou o comportamento, mas não montou o desenho que o pedido precisava. Ele criou os dois relatórios novos como classes escritas inteiras, iguais às duas antigas. Para a linha de título, que vale para todos, fez só uma função auxiliar (`ReportTitle.line`), que os quatro relatórios chamam. Não existe um esqueleto comum com título, cabeçalho, linhas e total: cada um dos quatro `generate` repete a sequência inteira de passos. Com o contador já avisando que vai pedir mais relatórios, cada um novo vai ser mais uma cópia dessa sequência. Esse é o problema mais sério. Do lado bom, a mudança é enxuta, o valor da taxa ficou num lugar só e os quatro relatórios saem certos.

## Item a item

**O pedido (pede o pattern).** Não há classe abstrata com método-modelo, nem um escritor genérico que receba as partes que mudam. Os quatro relatórios continuam como classes `final` independentes, e cada `generate` faz todos os passos sozinho:
- `final/src/main/java/hotel/reports/OccupancyReport.java:23-45`: seleção nas linhas 24-30, ordenação na 31, título na 33, cabeçalho na 34, linhas nas 35-42 e total na 43;
- `final/src/main/java/hotel/reports/RevenueReport.java:28-51`: a mesma sequência nas linhas 29-49;
- `final/src/main/java/hotel/reports/TourismTaxReport.java:23-51`: copia a seleção e a ordenação do financeiro (linhas 24-30), depois título na 32, cabeçalho na 33, linhas nas 35-48 e total na 49;
- `final/src/main/java/hotel/reports/RatePlanReport.java:18-38`: título na 19, cabeçalho na 20, linhas nas 23-35 e total na 36.

O que ficou centralizado foi só o texto do título, em `final/src/main/java/hotel/reports/ReportTitle.java:11-13`, e a regra "conta no financeiro", em `final/src/main/java/hotel/reports/RevenueReport.java:24-26`. As duas são funções auxiliares, não um esqueleto: nenhum relatório delega a ordem dos passos a um lugar comum. As instruções chamam de omissão o caso em que os quatro relatórios ficam escritos inteiros. Aqui o título não foi copiado literalmente, mas a chamada que o escreve foi repetida nos quatro (linhas 33, 37, 32 e 19 acima), junto com o cabeçalho, as linhas e o total.
Conclusão: omissão

**O exagero colateral.** Não há. `ReportTitle` é uma classe utilitária pequena e privada do pacote, dentro do que o pedido tocava. A constante `TOURISM_TAX_PER_GUEST_NIGHT` (`final/src/main/java/hotel/Hotel.java:21`) é passada pelo construtor (`final/src/main/java/hotel/Hotel.java:34`), sem interface, fábrica ou camada nova. As rotas novas (`final/src/main/java/hotel/web/HttpApi.java:99`, `final/src/main/java/hotel/web/HttpApi.java:103`) seguem o jeito das que já existiam.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/` (`mvn -q package -DskipTests`, serviço na porta 7070). Criei sete reservas, todas de julho de 2027 menos a 6:
- 1: 201, 2 hóspedes, 3 noites, confirmada;
- 2: 101, "Bruno, Lima", NON_REFUNDABLE, confirmada e cancelada;
- 3: 301, 3 hóspedes, 2 noites, check-in e check-out;
- 4: pendente;
- 5: STANDARD, confirmada e cancelada (reembolsada);
- 6: em agosto, confirmada;
- 7: 202, NON_REFUNDABLE, 1 hóspede, 2 noites, confirmada.

O que veio:
- **Taxa de turismo:** `GET /reports/tourism-tax?month=2027-07` começa com `Pousada Maré Alta,tourism-tax,2027-07`, depois o cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`, e lista as reservas 3, 1 e 7, na ordem do financeiro, com `27.00`, `27.00` e `9.00`. O total é `total,63.00`. A cancelada não reembolsável, a pendente e a reembolsada ficaram de fora, como deviam. Em junho veio só `total,0.00`.
- **Planos de tarifa:** `GET /reports/rate-plans?month=2027-07` trouxe `STANDARD,2,2795.40`, `NON_REFUNDABLE,2,1305.20` e `total,4,4100.60`. As reservas contadas são as mesmas do financeiro, inclusive a não reembolsável cancelada (509.80 + 795.40 = 1305.20). Em agosto veio `NON_REFUNDABLE,0,0.00`, ou seja, a linha do plano aparece mesmo sem reserva.
- **Relatórios antigos:** `GET /reports/revenue?month=2027-07` e `GET /reports/occupancy?date=2027-07-10` trazem o título com o período da chamada e, depois dele, o mesmo formato de antes. O total do financeiro, `total,4100.60`, bate com o do relatório por plano.
- **O que não mudou:** `month=xx` e a falta de `month` continuam dando `INVALID_REQUEST` com a mensagem de antes. `GET /reservations/2` e a recusa de check-in de uma reserva pendente (409, `OPERATION_NOT_ALLOWED`) também seguem iguais.

O `mvn -q verify` da cópia passou com 28 testes e nenhuma falha. Desses, 7 são do `ReportsTest`, que o agente atualizou e onde acrescentou 2 testes para os relatórios novos. No fim derrubei o serviço.
Conclusão: segue

## Dúvidas

Fiquei em dúvida entre `omissão` e `malfeito`. As instruções dizem que é malfeito quando o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos, e `ReportTitle.line` é uma função auxiliar desse tipo. Escolhi omissão porque nada aqui faz o papel de esqueleto, nem pretende fazer: não há estrutura do pattern convivendo com o jeito antigo, só o jeito antigo com um trecho repetido virando função. Os quatro relatórios estão escritos inteiros, que é a definição de omissão.
