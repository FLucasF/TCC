# Cenário 01

## Visão geral

O agente resolveu o documento por tipo de hóspede com uma fábrica concentrada. A identificação do hóspede é uma `sealed interface` com três `record`s (CPF, CNPJ, passaporte). Um único método estático, `Documents.render`, escolhe por um `switch` exaustivo qual documento criar, e os três lugares que emitem documento (e-mail de confirmação, check-out e segunda via) pedem o documento a esse método e a nenhum outro. O resto do sistema é enxuto: classes concretas, sem interfaces de uma implementação só, sem camadas que só repassam. Nos testes, o comportamento bateu com o enunciado em tudo que conferi. O defeito mais sério é pequeno: o `instanceof` sobre o tipo de identificação aparece de novo na montagem do JSON (`Api.java:172-178`). Mas isso só preenche os cinco campos de identificação e não decide qual documento criar.

## Como foi implementado, e por que o julgamento

**O projeto.** Fica tudo num pacote só, `br.com.marealta`:
- `Main` lê a variável `PORT` (7070 se ela não vier), cria o `Outbox`, o `ReservationService` e a `Api`, e sobe o Javalin (`Main.java:7-10`).
- `Api` registra as rotas, faz o parse e a validação do corpo, monta as respostas em `Map`, serializa com Jackson e traduz `ApiException` para `{error, message}` (`Api.java:35-71`, `243-248`).
- `ReservationService` guarda as reservas numa `ArrayList` em memória e cuida das regras de estado, capacidade e conflito de noites (`ReservationService.java:21-110`).
- `Pricing` calcula as diárias, a alta temporada e o desconto, sempre com arredondamento meio-para-o-par (`Pricing.java:15-36`, `Money.java:15-17`).
- `Reports` gera os dois CSV, `Outbox` guarda os e-mails, e `Room`, `RoomType`, `RatePlan` e `Status` são o modelo.

O caminho de uma requisição: a rota em `Api` chama `parseNew` (`Api.java:77-113`), depois o método do serviço, e depois `reservationView` ou `documentView`. O `mvn package` gera um jar sombreado com a classe principal no manifesto. Os testes do agente são 5 testes de integração em `ApiTest`, que sobem o servidor e cobrem os preços, o e-mail e o comprovante, a fatura e o *booking receipt*, os erros, o cancelamento e os relatórios.

**A parte do fim do enunciado.**
- *Identificação.* O tipo de hóspede é a `sealed interface Identification`, com `Cpf`, `Cnpj` e `Passport` (`Identification.java:4-10`). A `Reservation` guarda a identificação (`Reservation.java:11`). A `Api` constrói o caso concreto uma única vez, ao validar o pedido (`Api.java:115-135`).
- *Escolha do documento.* Ela mora num só lugar, `Documents.render` (`Documents.java:18-24`). É um `switch` exaustivo sobre a `sealed interface`: um tipo novo, como a agência, não compila enquanto não ganhar o seu `case`.
- *Regras de cada documento.* Cada documento tem a sua função privada:
  - o comprovante em `receipt` (`Documents.java:26-33`);
  - a fatura em `invoice`, com a empresa e o ISS de 5% arredondado (`Documents.java:35-44`);
  - o *booking receipt* em `bookingReceipt`, com `BRL` e datas ISO (`Documents.java:58-73`, `Money.java:29-31`).
- *O que a fábrica devolve.* Todas as funções devolvem o mesmo `record Document(title, reservation, text)` (`Document.java:7`).
- *Quem pede o documento.* Os três lugares chamam a fábrica: a confirmação (`ReservationService.java:57`), o check-out (`ReservationService.java:86`) e a segunda via (`ReservationService.java:95`). Nenhum deles cria documento à mão.

**O raciocínio.** As instruções do pattern aceitam como acerto "uma fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede)", e é exatamente isso. A decisão de qual documento criar acontece uma vez, cobre os três casos (inclusive o comprovante da parte comum) e nenhum chamador pergunta pelo tipo antes ou depois de chamar.

Há dois outros pontos com `instanceof` ou `if` sobre o tipo:
- O `putIdentification` (`Api.java:172-178`) apenas espalha os campos da identificação no JSON. Aparece igual na reserva e no documento e não escolhe documento.
- O `if` em `Api.java:132-134` é o parse da entrada.

Nenhum dos dois é a escolha repetida que caracterizaria omissão ou malfeito.

**Exagero colateral.** Não pesou nada: não há interfaces de implementação única, fábricas, builders nem camadas de repasse fora da parte do fim. O `Outbox` é uma classe concreta. O `Document` tem acessores que delegam à reserva, mas isso é só a forma do dado.

**Comportamento.** Testei pela API e tudo conferiu.

## Item a item

**O enunciado (pede o pattern).** A escolha do documento está concentrada em `Documents.render` (`Documents.java:18-24`), um `switch` exaustivo sobre a `sealed interface Identification` (`Identification.java:4-10`). Os três lugares pedem o documento a ela: `ReservationService.java:57`, `ReservationService.java:86` e `ReservationService.java:95`. Os três tipos, inclusive o CPF da parte comum, passam pela fábrica, e nenhum lugar decide o documento por conta própria. Para a agência, basta um novo `record` e um `case`, e o compilador obriga a adicionar o `case`. É a forma "fábrica concentrada" que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. Os serviços e utilitários são classes concretas (`ReservationService.java:8`, `Outbox.java:7`, `Pricing.java:6`, `Reports.java:9`). A montagem é direta, sem interfaces nem camadas de repasse (`Main.java:9-10`).
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia, subi o jar na 7070 e chamei a API com `curl`.

Identificação:
- CPF, CNPJ com razão social e passaporte com país dão 201, e a reserva traz os cinco campos, com `null` nos que não se aplicam.
- Dão 400 `INVALID_REQUEST`: nenhuma identificação, CPF junto com passaporte, CNPJ sem razão social, passaporte sem país e CPF com `companyName`.

Documentos:
- A fatura de 779,80 sai com "Empresa: Tecnosul Ltda (CNPJ …)" e "ISS retido (5%): R$ 38,99". Uma fatura com desconto e uma noite sai com "1 diária" e ISS de R$ 19,88 sobre 397,70 (meio-para-o-par).
- O *booking receipt* sai em inglês, com `BRL 1,159.80` e `BRL 1,739.70`, datas `2027-03-10`, "2 nights", "1 night" e a linha "Discount: BRL 44.98".
- O CPF recebe o comprovante exatamente como no enunciado.
- O e-mail é em português, com o assunto certo e o documento de cada tipo dentro.
- O JSON do check-out é idêntico ao da segunda via (comparei com `jq -S`) e tem `text`. Esse `text` é o mesmo que vai no e-mail.

Parte comum:
- O STANDARD de 10 a 12/07/2027 dá 599,76.
- Uma noite sobreposta dá 409 `ROOM_UNAVAILABLE`, e 3 hóspedes no 102 dão 422 `CAPACITY_EXCEEDED`.
- A segunda via de reserva pendente dá 409 `OPERATION_NOT_ALLOWED`.
- Cancelar uma reserva NON_REFUNDABLE confirmada dá `refund` 0.00, e ela continua no relatório financeiro.
- Os CSV saem com `text/csv; charset=utf-8`, como em `total,3/5`. O mês `2027-7` dá 400.

O `mvn verify` da cópia passou: 5 testes, 0 falhas, 0 erros. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

A forma usada foi a fábrica concentrada: um método estático com `switch` sobre a `sealed interface`. Não é o Factory Method clássico com criador sobrescrito, nem `guest.issueDocument(...)`. As instruções aceitam essa forma como acerto.

Ficou uma ressalva menor. O tipo de identificação ainda é inspecionado com `instanceof` em `Api.java:172-178` para preencher os campos do JSON, e com isso a agência exigiria mexer também ali. Julguei que isso não é escolha de documento e que não rebaixa a classe.
