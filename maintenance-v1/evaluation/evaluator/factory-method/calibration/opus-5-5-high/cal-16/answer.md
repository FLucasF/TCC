# Cenário 05

## Visão geral

O agente juntou num lugar só a escolha de qual documento emitir. O `DocumentFactory` tem um método, `writerFor`, que olha o tipo de hóspede e escolhe entre três escritores, um para cada documento, todos atrás da interface `DocumentWriter`. Os três lugares que antes faziam `new ReservationReceipt(...)` agora chamam `documents.issue(...)`: a confirmação, o check-out e a segunda via. O comprovante de quem tem CPF passou para a nova estrutura igual ao de antes. Assim, o documento das agências entra com mais um escritor e mais um ramo na fábrica, sem mexer no `ReservationService`. Nos testes pela API, todas as regras do pedido conferiram. Não achei problema sério. A ressalva pequena é que a regra de "um jeito só de identificar o hóspede" só é checada na camada HTTP: o `Guest` em si aceita combinações inválidas, e nesse caso a fábrica decide pela ordem dos `if`.

## Item a item

**O pedido (pede o pattern).** Na base, o comprovante era montado com `new` em três lugares do serviço (`base/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`). O texto ficava num método fixo do próprio record, `base/src/main/java/hotel/documents/ReservationReceipt.java:40-53`. Na final:
- **A escolha fica num lugar só.** `src/main/java/hotel/documents/DocumentFactory.java:19-27` decide pelo tipo de hóspede, usando `Guest.isCompany()` e `Guest.isForeigner()` (`src/main/java/hotel/reservations/Guest.java:20`, `:24`). Em `DocumentFactory.java:14-17` a fábrica monta o documento com o título e o texto que vêm do escritor escolhido.
- **Os três lugares só pedem o documento.** `src/main/java/hotel/reservations/ReservationService.java:70`, `:112` e `:121` chamam `documents.issue(...)` e nenhum deles pergunta qual é o tipo de hóspede.
- **Cada documento tem a sua classe.** `PersonReceiptWriter.java`, `CompanyInvoiceWriter.java` e `ForeignReceiptWriter.java` implementam `DocumentWriter.java:7-12`.
- **O caso que já existia foi migrado.** O texto do comprovante de hoje saiu do record e foi para `PersonReceiptWriter.java:15-31`, igual linha por linha.
- **Ninguém mais monta o documento à mão.** O único `new ReservationReceipt` está em `DocumentFactory.java:16`, e o e-mail usa o `text` que vem pronto (`ReservationService.java:131`).

Não sobrou `if` sobre o tipo de hóspede fora da fábrica. É a forma de fábrica concentrada que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. Toda a estrutura nova é do documento, que é o que o pedido mexe: a interface `DocumentWriter` tem três implementações, não uma só. As outras mudanças são as que o pedido exige:
- `Guest` ganhou campos e dois testes de tipo (`Guest.java:20-26`), e manteve o construtor antigo de três argumentos (`:16`).
- O request e o response ganharam os quatro campos novos.
- A validação ficou em `HttpApi.java:126-137`.

Não há builder, camada nova nem fábrica fora do documento.
Conclusão: contenção

**O comportamento.** Compilei uma cópia da `final/` e subi o serviço na porta 7070. Pela API, testei o seguinte:

- **Criação com cada jeito de identificar.** Com CPF, com CNPJ mais razão social e com passaporte mais país, a resposta foi 201. O JSON traz os quatro campos novos e os que não se aplicam vêm `null`.
- **Recusas.** Todos estes casos deram 400 `INVALID_REQUEST`, como o pedido manda:
  - nenhuma identificação;
  - CPF com CNPJ e razão social;
  - CPF com passaporte e país;
  - CNPJ e razão social com passaporte e país;
  - só CNPJ, só razão social, só passaporte ou só país;
  - CPF em branco (`"   "`);
  - CPF com só a razão social.
- **Fatura (reserva 1, quarto 201, de 10 a 12/03/2027).** Na segunda via, no e-mail de confirmação e no check-out veio o texto exato do exemplo do pedido: "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Total: R$ 779,80" e "ISS retido (5%): R$ 38,99". O arredondamento é meio para o par (`CompanyInvoiceWriter.java:33-34`), como o resto do sistema.
- **Booking receipt.** Testei três casos:
  - 2 noites: "Booking receipt no. 2", "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights", "Nights: BRL 779.80".
  - Suíte em julho, no plano `NON_REFUNDABLE`: "Nights: BRL 2,087.64", "Discount: BRL 313.15" antes do total e "Total: BRL 1,774.49".
  - 1 noite: "1 night".
- **Comprovante de CPF.** O texto é o mesmo de hoje ("Comprovante de reserva nº 3 / Hóspede: Ana Souza (CPF 111.222.333-44) …").
- **E-mail.** Continua em português ("Olá…", "Sua reserva… está confirmada", "A entrada é a partir das 14h…") e só o documento de dentro muda.
- **JSON do documento.** Na segunda via e no check-out vêm `title`, os campos de antes, os quatro de identificação (com `null` nos que não se aplicam) e `text`.
- **O que não mudou.** Testei e continua igual:
  - capacidade estourada: 422 `CAPACITY_EXCEEDED`;
  - quarto ocupado: 409 `ROOM_UNAVAILABLE`;
  - sem nome: 400 "every field of the reservation is required";
  - cancelamento `NON_REFUNDABLE` com reembolso 0,00;
  - reserva inexistente: 404;
  - relatórios de receita e de ocupação no mesmo formato CSV.

O `mvn -q verify` da cópia passou (exit 0) com 29 testes e nenhuma falha: 3 novos em `DocumentsByGuestTest`, 5 em `ReportsTest`, 6 em `PriceCalculatorTest`, 11 em `ReservationServiceTest` e 4 em `HttpApiTest`. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

- **Forma usada.** É uma fábrica concentrada: `DocumentFactory.writerFor` escolhe entre três estratégias de escrita e o `issue` monta um único tipo de documento (`ReservationReceipt`) com o título e o texto delas. Não é o Factory Method clássico, com um criador sobrescrito por tipo, nem o método polimórfico no hóspede. Não há uma classe de documento por tipo: o que varia por tipo é o escritor. Como a escolha acontece dentro da fábrica e não depois, não vejo nisso o caso "a fábrica devolve sempre o mesmo documento e a escolha acontece depois".
- **Regra do hóspede só na API.** A regra de "exatamente um jeito de identificar" só vale em `HttpApi.java:126-134`. O `Guest` aceita qualquer combinação, e um `Guest` com CNPJ e passaporte criado direto pelo código receberia a fatura, porque esse `if` vem primeiro em `DocumentFactory.java:20`. Pela API isso não acontece, então não afeta a conclusão.
