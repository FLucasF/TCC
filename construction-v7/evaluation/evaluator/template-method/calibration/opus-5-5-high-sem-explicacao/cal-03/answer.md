# Cenário 01

## Visão geral

O agente montou um sistema enxuto e correto. Há um pacote `reports` com uma classe concreta por relatório, e o `Hotel` liga tudo sem interfaces nem camadas que só repassam. A parte do fim do enunciado, porém, foi feita sem o pattern. Os quatro relatórios (ocupação, financeiro, taxa de turismo e planos) estão escritos inteiros, cada um do seu jeito. Cada um repete a sequência título → cabeçalho → linhas → total e tem a própria cópia da linha "Pousada Maré Alta,<nome>,<período>". Não existe esqueleto comum, nem por herança nem por composição. O comportamento está todo certo: os quatro relatórios saíram como pedido e a parte comum também confere. O problema sério é só o de desenho: o relatório de cancelamentos e o de hóspedes por estado, que o contador já avisou que vai pedir, vão sair como a quinta e a sexta cópias.

## Item a item

**O enunciado (pede o pattern).** Não há classe abstrata, interface nem escritor genérico no pacote `reports`: são quatro classes `final` independentes, cada uma com seu próprio `generate`. Cada relatório monta o CSV inteiro à mão, e a linha de título aparece copiada em cada um:
- `src/main/java/hotel/reports/OccupancyReport.java:34`
- `src/main/java/hotel/reports/RevenueReport.java:35`
- `src/main/java/hotel/reports/TourismTaxReport.java:33`
- `src/main/java/hotel/reports/RatePlanReport.java:31`

O resto da sequência também se repete em cada classe:
- **Cabeçalho:** `OccupancyReport.java:35`, `RevenueReport.java:36`, `TourismTaxReport.java:34`, `RatePlanReport.java:32`.
- **Linha do total:** `OccupancyReport.java:44`, `RevenueReport.java:47`, `TourismTaxReport.java:48`, `RatePlanReport.java:46`.
- **Seleção:** o laço de `RevenueReport.java:24-31` foi copiado literalmente em `RatePlanReport.java:21-28`.
- **Ordenação:** o comparador de `RevenueReport.java:32` foi repetido em `TourismTaxReport.java:30`.

A cópia também aparece na API. As quatro rotas em `src/main/java/hotel/web/HttpApi.java:138-153` repetem o mesmo bloco, uma por relatório, e o `HttpApi` recebe cada relatório como um campo próprio (`HttpApi.java:79-82`, construtor em `HttpApi.java:84-91`). Isso é exatamente o caso de omissão das instruções: os quatro relatórios escritos inteiros, cada um com a sua linha de título. Um relatório novo exige uma classe inteira nova, um campo e um parâmetro novos no `HttpApi` e uma rota copiada.
Conclusão: omissão

**O exagero colateral.** Não há:
- `Rooms`, `ReservationStore`, `PriceCalculator` e `ReservationService` são classes concretas usadas diretamente (`src/main/java/hotel/Hotel.java:22-29`). Não há interface com uma implementação só, fábrica nem builder próprio.
- O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:3-15`) é uma classe concreta pequena que fixa o canal `EMAIL` ao gravar a mensagem. Ela tem um papel e não é uma camada que só repassa.
- O `JsonMapper.builder()` em `HttpApi.java:100-103` é a API do Jackson, não um builder do agente.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q package -DskipTests` numa cópia e subi `target/hotel.jar` com `PORT=7070`. Criei seis reservas e mudei a situação delas: confirmadas, check-in e check-out, cancelamento de uma NON_REFUNDABLE paga e de uma STANDARD paga, uma pendente e uma em agosto.

Os relatórios:
- **Ocupação** (`occupancy?date=2027-07-10`): título `Pousada Maré Alta,occupancy,2027-07-10`, linhas dos quartos 101 e 202 em ordem de quarto, `total,2/5`. Em 2027-07-12, quem sai naquele dia não conta (`total,1/5`).
- **Financeiro** (`revenue?month=2027-07`): título certo. A NON_REFUNDABLE cancelada continua contando (`2,201,...,CANCELLED,1193.09`), a STANDARD cancelada com reembolso e a pendente ficam de fora. A ordem é por entrada e depois por número, com `total,3196.49`.
- **Taxa de turismo** (`tourism-tax?month=2027-07`): título `Pousada Maré Alta,tourism-tax,2027-07` e as colunas `reservation,room,check_in,check_out,guests,nights,tax`. Saíram 2×2 = `18.00` e 2×3 = `27.00`, mais `total,45.00`. A cancelada e a pendente ficam de fora, e a ordem é a do financeiro.
- **Planos** (`rate-plans?month=2027-07`): `STANDARD,2,2003.40`, `NON_REFUNDABLE,1,1193.09` e `total,3,3196.49`. A NON_REFUNDABLE cancelada conta e a soma bate com o financeiro. Em 2027-09 saem as duas linhas zeradas e `total,0,0.00`.
- Os quatro relatórios vêm com `Content-Type: text/csv; charset=utf-8`. Um mês fora do formato (`2027-7`) ou sem o parâmetro dá 400 `INVALID_REQUEST`.

A parte comum:
- STANDARD de 10/07 a 12/07/2027 dá 2 noites e `599.76`. A NON_REFUNDABLE no 201 de 10/03 a 12/03/2027 dá `779.80`, desconto `116.97` e total `662.83`.
- O e-mail de confirmação saiu com canal, assunto e texto exatamente como no enunciado.
- O check-out devolve o comprovante. A segunda via de reserva pendente dá 409.
- Os erros: quarto ocupado dá 409 `ROOM_UNAVAILABLE`, capacidade excedida dá 422 `CAPACITY_EXCEEDED`, corpo que não é JSON dá 400, reserva inexistente dá 404 e check-out de reserva só confirmada dá 409.

O `mvn verify` da cópia passou: 28 testes, 0 falhas, `BUILD SUCCESS`. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.
