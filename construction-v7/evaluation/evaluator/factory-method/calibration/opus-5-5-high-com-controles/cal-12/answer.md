# Cenário 01

## Visão geral

O agente juntou num lugar só a escolha do documento que cada tipo de hóspede recebe. A classe `DocumentFactory` decide entre três escritores (comprovante, fatura e *booking receipt*), que implementam uma mesma interface `DocumentWriter`. Os três lugares onde o documento sai pedem o documento à fábrica, e nenhum deles monta um à mão nem pergunta o tipo do hóspede. O resto do sistema tem estrutura simples: classes concretas, *records* e nenhuma interface além da dos documentos. Nos testes pela API, tudo o que o enunciado pede bateu. O ponto mais fraco é pequeno e de estilo, não de desenho. O tipo de hóspede não está modelado como tipo: a fábrica descobre o caso olhando quais campos do `Guest` estão preenchidos. Como essa decisão fica só dentro da fábrica, a forma ainda é a esperada.

## Item a item

**O enunciado (pede o pattern: documento por tipo de hóspede).** A escolha mora só em `src/main/java/hotel/documents/DocumentFactory.java:19-27`. O método `writerFor` devolve `companyInvoice`, `foreignReceipt` ou `personReceipt`. O método `issue` (`DocumentFactory.java:14-17`) monta o `ReservationReceipt` com o título e o texto do escritor escolhido. Cada documento tem a sua classe, todas implementando `DocumentWriter` (`documents/DocumentWriter.java:7-12`):
- `documents/PersonReceiptWriter.java:7`;
- `documents/CompanyInvoiceWriter.java:9`;
- `documents/ForeignReceiptWriter.java:9`.

Os três lugares passam pela fábrica:
- o e-mail de confirmação, em `reservations/ReservationService.java:70`;
- o check-out, em `reservations/ReservationService.java:112`;
- a segunda via, em `reservations/ReservationService.java:121`.

O `grep` confirma que `isCompany()` e `isForeigner()` só são usados em `DocumentFactory.java:20` e `:23`, e que os escritores só são criados ali (`DocumentFactory.java:10-12`). Não há `if` sobre o tipo de hóspede repetido fora da fábrica. O e-mail continua sempre em português e só encaixa o `receipt.text()` (`ReservationService.java:128-134`). Para o documento das agências, basta um escritor novo e um ramo novo em `writerFor`.
Conclusão: acerto

**O exagero colateral.** Não há. Fora dos documentos, tudo é classe concreta ou *record*: `Rooms`, `ReservationStore`, `PriceCalculator`, `OccupancyReport`, `RevenueReport` e `HttpApi`. Não há fábrica, *builder* nem interface com uma implementação só. Os planos de tarifa ficam num `switch` simples (`reservations/PriceCalculator.java:26-29`; `reservations/ReservationService.java:88-91`). O caso mais perto de uma camada que só repassa é o `messages/EmailSender.java:4-15`, que encaminha para `SentMessages.add`. Mas ele fixa o canal `"EMAIL"` e separa "enviar" de "registrar o que foi enviado", então não conta como embrulho vazio. O construtor de conveniência `Guest(name, email, cpf)` (`reservations/Guest.java:16-18`) é só um atalho e também não conta.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp`, com `mvn -q package -DskipTests` e `PORT=7070 java -jar target/hotel.jar`.

- **Criação da reserva:**
  - Com só o CPF, com o CNPJ junto da razão social e com o passaporte junto do país, a reserva sai com 201. Os campos que não se aplicam vêm `null`.
  - Saem 400 com `INVALID_REQUEST` nestes casos: sem identificação, com CPF e CNPJ juntos, com CNPJ sem razão social, com passaporte sem país e com país sem passaporte. A regra está em `web/HttpApi.java:126-134`.
- **Fatura (reserva nº 2, DELUXE de 10/03 a 12/03/2027):** a fatura mostra "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", o hóspede e "ISS retido (5%): R$ 38,99" sobre R$ 779,80. Com desconto e uma noite, saem 254,90 de total e R$ 12,74 de ISS (12,745 arredondado meio para o par), com "1 diária" e a linha "Desconto".
- ***Booking receipt*:**
  - O texto é em inglês, com "BRL 1,159.80", "2027-03-10" e "2 nights".
  - Com uma noite no plano NON_REFUNDABLE em julho, sai "1 night", "Discount: BRL 104.38" e "Total: BRL 591.50".
- **Comprovante (CPF):** o texto é igual ao do enunciado: "Hóspede: Carla Dias (CPF 111.222.333-44)", "2 diárias", "R$ 779,80".
- **Mesmo documento nos três lugares:**
  - Para as 5 reservas, o JSON do check-out é idêntico ao da segunda via (comparei com `cmp`), e ambos têm o campo `text`.
  - O corpo de cada e-mail em `/messages` contém exatamente esse `text`, com o assunto "Reserva confirmada - Pousada Maré Alta" e o resto do e-mail em português.
  - A segunda via de uma reserva pendente dá 409 com `OPERATION_NOT_ALLOWED`.
- **Parte comum:**
  - Um STANDARD de 10/07 a 12/07/2027 dá 599,76.
  - Mais hóspedes do que o quarto comporta dá 422 com `CAPACITY_EXCEEDED`, e um quarto já reservado nas mesmas noites dá 409 com `ROOM_UNAVAILABLE`. Um JSON inválido dá 400.
  - Ao cancelar uma reserva STANDARD confirmada, o `refund` é igual ao valor pago.
  - Os relatórios vêm como `text/csv; charset=utf-8`: o de ocupação termina com `total,2/5` e o financeiro com `total,846.40`. O mês `2027-7` dá 400.
- **Testes do agente e encerramento:** o `mvn verify` da cópia rodou 29 testes, com 0 falhas, 0 erros e BUILD SUCCESS. Depois derrubei o serviço.

Conclusão: segue

## Dúvidas

Quanto à forma usada: é a fábrica concentrada, não o Factory Method clássico com um criador sobrescrito por tipo. Um método da fábrica (`DocumentFactory.writerFor`, em `documents/DocumentFactory.java:19-27`) escolhe um objeto que implementa `DocumentWriter`, e cada documento fica na sua classe. O tipo de hóspede não é um tipo: é deduzido de quais campos do `Guest` vêm preenchidos (`reservations/Guest.java:20-26`), e o caso da pessoa com CPF é o "senão" da fábrica. Isso não muda a classe, porque a dedução só acontece dentro da fábrica. Mas um tipo explícito deixaria o caso das agências menos dependente de qual campo vem nulo. Os títulos "Fatura de hospedagem" e "Booking receipt" para os documentos novos foram decisão do agente, porque o enunciado só define o título do comprovante.
