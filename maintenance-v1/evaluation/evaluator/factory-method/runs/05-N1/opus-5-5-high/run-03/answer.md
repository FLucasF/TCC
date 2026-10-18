# Cenário 05

## Visão geral

O agente não criou documentos diferentes nem um lugar que escolhe qual criar. Continua existindo um único tipo de documento, o `ReservationReceipt`. Dentro dele, duas cadeias de `if` iguais testam qual campo do `Guest` não é nulo: uma escolhe o título e a outra escolhe qual de três funções de texto chamar. O `ReservationService` ficou como estava. Os três lugares continuam fazendo `new ReservationReceipt(...)`, e tudo funciona porque o construtor decide o texto. O lado bom é que a decisão não se espalhou pelos três lugares, e os três documentos saem certos. O problema de desenho é que não existe um tipo de hóspede nem um tipo de documento. A variação virou `if` sobre campos nulos, repetido dentro do record, e o documento das agências vai exigir outro ramo nas duas cadeias. O problema de comportamento: um CPF junto com um CNPJ ou passaporte incompleto é aceito, quando deveria ser recusado.

## Item a item

**O pedido (pede o pattern).** O `ReservationService` não mudou (`base/` e `final/` são idênticos). Os três lugares ainda criam o documento com `new ReservationReceipt(reservation, roomOf(reservation))`: em `final/src/main/java/hotel/reservations/ReservationService.java:68` (confirmação), `:110` (check-out) e `:119` (segunda via). Como só existe esse record, a escolha do documento passou para dentro do construtor dele (`final/src/main/java/hotel/documents/ReservationReceipt.java:30-49`).

O teste sobre o tipo de hóspede aparece duas vezes, igual. Uma vez em `titleFor` (`ReservationReceipt.java:51-59`), que verifica `companyCnpj() != null` e depois `passport() != null`, e de novo em `buildText` (`ReservationReceipt.java:61-69`), com as mesmas condições. Daí saem três funções de texto que são quase cópias umas das outras (`:71`, `:89`, `:111`). O hóspede também não ganhou um tipo: o `Guest` só recebeu quatro campos que podem ser nulos (`final/src/main/java/hotel/reservations/Guest.java:3-11`). A decisão de qual hóspede é qual depende de testar nulos, e o mesmo acontece no `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:144-151`).

Não há método de fábrica, nem fábrica que devolva documentos diferentes, nem um `guest.issueDocument(...)`. O que existe é um único documento com `if/else` dentro. É a mudança feita "com mais um `if`": para o documento das agências seria preciso mais um campo nulo no `Guest`, mais um ramo em `titleFor`, mais um ramo em `buildText` e mais uma função de texto copiada.
Conclusão: omissão

**O exagero colateral.** Não há. O que mudou fora do documento ficou no que o pedido tocava e não criou estrutura: dois construtores a mais e um método estático `withPassport` no `Guest` (`Guest.java:12-22`), duas funções de formatação no `TextFormat` (`final/src/main/java/hotel/documents/TextFormat.java:23`, `:31`) e os campos novos no `CreateReservationRequest` e no `ReservationResponse`. Nenhuma interface, fábrica ou camada nova.
Conclusão: contenção

**O comportamento.** Rodei o `mvn -q verify` numa cópia da `final/`: passou, com 35 testes e nenhuma falha. São 5 testes novos no `ReservationReceiptTest`, 4 testes novos entre os 8 do `HttpApiTest`, mais os testes de antes. Depois subi o serviço na porta 7070 e fiz as chamadas abaixo.

- **Empresa** (quarto 201, de 10/03 a 12/03/2027): a reserva volta com o CNPJ e a razão social, e os outros campos de identificação vêm `null`. O e-mail de confirmação, a segunda via e o check-out trazem a "Fatura de hospedagem nº 1", igual ao exemplo do pedido, com "ISS retido (5%): R$ 38,99". O JSON tem `title`, os quatro campos novos e `text`.
- **Estrangeiro**: o *booking receipt* sai certo na confirmação e no check-out, com "2 nights", "BRL 779.80" e datas no formato `2027-03-10`.
  - Com 1 noite na alta temporada e plano `NON_REFUNDABLE`: "1 night" e a linha "Discount: BRL 104.38" antes do total.
  - Com 3 noites na suíte: "BRL 1,739.70".
- **CPF**: o comprovante sai idêntico ao de antes, e o e-mail continua em português.
- **Recusas que funcionam**, todas com 400 e `INVALID_REQUEST`: sem identificação nenhuma, CPF com CNPJ, CPF com passaporte, só o CNPJ, só o passaporte.
- **O que não mudou e continua igual**: capacidade excedida (422), quarto ocupado (409), segunda via de reserva pendente (409), cancelamento `NON_REFUNDABLE` (devolve 0,00), e os relatórios de faturamento e de ocupação.

O que não confere: `POST /reservations` com `"guestCpf":"111","companyCnpj":"2"` (CNPJ sem razão social) deveria voltar 400, porque o pedido manda recusar dois jeitos ou um jeito incompleto. Voltou 201 e a reserva foi criada só com o CPF, e o CNPJ sumiu sem aviso. O mesmo acontece com CPF mais `guestCountry` sozinho, e com passaporte completo mais `companyName` sozinho. A causa é que `hasCompany` e `hasPassport` só são verdadeiros quando os dois campos vêm preenchidos (`HttpApi.java:128-129`). Por isso um par incompleto ao lado de outro jeito completo nunca é recusado, e as verificações de `HttpApi.java:137-141` nunca disparam.
Conclusão: em parte

## Dúvidas

Fiquei entre `omissão` e `acerto` por "fábrica concentrada", porque o construtor do `ReservationReceipt` é, na prática, o único lugar que decide, e os três pontos do serviço não repetem o `if`. Escolhi `omissão` porque esse construtor não escolhe entre documentos: ele monta sempre o mesmo record e só troca o título e o texto. Além disso, a decisão aparece duas vezes, em `titleFor` e em `buildText`. Não há nenhuma das formas que as instruções aceitam (método de fábrica sobrescrito, fábrica que devolve o documento certo, método polimórfico no tipo de hóspede). E o tipo de hóspede nem existe: é deduzido testando campos nulos.
