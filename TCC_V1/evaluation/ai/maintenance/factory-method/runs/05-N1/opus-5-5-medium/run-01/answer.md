# Cenário 05

## Visão geral

O agente juntou a escolha do documento num lugar só. Antes havia um `ReservationReceipt` montado com `new` em três pontos do serviço. Agora há o `ReservationDocument.issue(reservation, room)`, uma fábrica concentrada: ela olha a identificação do hóspede, escolhe o layout (comprovante, fatura ou *booking receipt*) e monta o documento. Os três lugares (`confirm`, `checkOut` e `receiptCopy`) pedem o documento a ela e não decidem nada. A identificação virou um tipo selado com três variantes, e os textos ficaram numa hierarquia de layouts em que cada tipo de documento sobrescreve os rótulos e formatos de um roteiro comum. Isso vale também para o comprovante de CPF, que já existia. Para trazer as agências, basta uma variante e um layout novos, e a decisão só muda num ponto. Não achei problema sério. O ponto mais discutível é pequeno: o `IdentificationReader` da camada web é um pouco elaborado para validar três combinações de campos, mas é privado e local, e não chega a ser exagero.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin organizado em pacotes:
- `hotel.web` tem o `HttpApi`, com as rotas, o `CreateReservationRequest`, o `ReservationResponse` e o novo `IdentificationReader`.
- `hotel.reservations` tem o `ReservationService`, a `Reservation`, o `Guest`, o novo `Identification`, o `PriceCalculator` e o `ReservationStore` em memória.
- `hotel.documents` tem o documento e os layouts.
- `hotel.messages` tem o e-mail, guardado em `SentMessages`.
- `hotel.reports` gera os CSVs.

Uma criação de reserva passa por `HttpApi` → `newReservation`. Lá se validam os campos obrigatórios (`final/src/main/java/hotel/web/HttpApi.java:116-125`), e a identificação vem do `IdentificationReader.read(body)` (`HttpApi.java:126`), que monta um `Guest` com `Identification`. O `ReservationService` aplica a regra e grava no store em memória.

**A identificação.** O `Guest` deixou de ter `cpf` e passou a ter `Identification identification` (`final/src/main/java/hotel/reservations/Guest.java:3`). `Identification` é uma `sealed interface` (`final/src/main/java/hotel/reservations/Identification.java:6`) com três records: `Cpf`, `Company` e `Passport` (`Identification.java:29-38`). Os acessores que não se aplicam devolvem `null` por padrão, e é isso que alimenta os campos nulos no JSON da reserva e do documento.

**Os testes do agente.** O agente escreveu o `ReservationDocumentTest`, com os textos completos dos três documentos, o desconto, "1 night" e o e-mail em português. Também acrescentou dois testes ao `HttpApiTest`: um com as combinações inválidas de identificação e um com o documento do estrangeiro pela rota `/document`. Os testes que já existiam foram ajustados para `new Identification.Cpf(...)`.

**A parte do pattern.**
- **A fábrica.** É o `ReservationDocument.issue` (`final/src/main/java/hotel/documents/ReservationDocument.java:33`). Ele chama `layoutFor(identification)` (`ReservationDocument.java:56-61`), um `switch` exaustivo sobre a identificação selada: `Cpf` vai para `ReceiptLayout`, `Company` para `InvoiceLayout` e `Passport` para `BookingReceiptLayout`. Depois preenche o record, com os campos de antes, os quatro novos e `text = layout.render(...)`.
- **O roteiro comum.** Fica em `DocumentLayout.render` (`final/src/main/java/hotel/documents/DocumentLayout.java:42-57`). As subclasses dão o título, os rótulos, `nights`, `money`, `date`, `identityLines` e `closingLines`.
- **Onde mora cada regra.**
  - A linha "Empresa:" e o ISS de 5% ficam no `InvoiceLayout` (`final/src/main/java/hotel/documents/InvoiceLayout.java:19-23` e `:26-29`).
  - O inglês, `BRL`, a data ISO e "night/nights" ficam no `BookingReceiptLayout` (`final/src/main/java/hotel/documents/BookingReceiptLayout.java:45`, `:65`, `:70`).
  - O comprovante de CPF fica no `ReceiptLayout`.
- **Os três lugares.** Todos chamam a fábrica: `final/src/main/java/hotel/reservations/ReservationService.java:68` (`confirm`, cujo documento vai para o e-mail), `:110` (`checkOut`) e `:119` (`receiptCopy`). Nenhum deles cria o documento à mão, e ninguém fora da fábrica pergunta qual é a identificação para decidir o documento.

**O julgamento.**
- **O pattern.** As instruções aceitam como acerto uma fábrica concentrada que escolhe o documento pelo tipo de hóspede, e é o que existe aqui. A escolha está num único `switch` (`ReservationDocument.java:57`) e os três pontos de emissão passam por ela. O comprovante de CPF, que já existia, também passou pela estrutura: o antigo `ReservationReceipt` foi apagado e virou o `ReceiptLayout`. O fato de a fábrica devolver sempre o mesmo record, `ReservationDocument`, não é o caso "malfeito" das instruções, porque a escolha do layout, que gera o texto e o título, acontece dentro da fábrica e não depois, fora dela.
- **O exagero colateral.** A hierarquia de layouts é a própria variação; não é estrutura sobrando. O `IdentificationReader`, com um record `Kind` e `Function` (`final/src/main/java/hotel/web/IdentificationReader.java:13-22`), é um pouco engenhoso para três combinações, mas é uma classe de validação privada que resolve a regra "exatamente um, completo" sem `if` repetido. Também não há interface com uma implementação só, builder ou camada que só repassa.
- **O comportamento.** Conferi por leitura e as regras batem.

## Item a item

**O pattern pedido.** A criação do documento ficou concentrada em `ReservationDocument.issue` (`final/src/main/java/hotel/documents/ReservationDocument.java:33`). O método escolhe o layout pela identificação do hóspede num único `switch` (`ReservationDocument.java:57-61`), e os três lugares que emitem o documento usam essa fábrica: `final/src/main/java/hotel/reservations/ReservationService.java:68` (e-mail de confirmação), `:110` (check-out) e `:119` (segunda via). O caso que já existia, o CPF, foi para a mesma estrutura (`ReceiptLayout`), e o `ReservationReceipt` da base foi removido. Não há `if` sobre o tipo de hóspede espalhado nem criação à mão fora da fábrica. Uma agência entraria como mais uma variante de `Identification` e mais um layout, e a mudança na escolha seria num lugar só. É a forma "fábrica concentrada" que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. A hierarquia `DocumentLayout` e as três subclasses é a variação que o pattern resolve. O `IdentificationReader` (`final/src/main/java/hotel/web/IdentificationReader.java:13-44`) é um pouco elaborado (um record `Kind` com uma `Function` de construção), mas é privado à camada web e substitui a validação de três combinações; não é uma camada, fábrica ou interface sem uso. A única mudança de visibilidade foi `PriceCalculator.cents`, que virou `public` para o ISS usar o mesmo arredondamento.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código: não subi o serviço nem rodei testes.
- **Identificação.** O `IdentificationReader.read` (`final/src/main/java/hotel/web/IdentificationReader.java:27-44`) considera "dada" uma forma com algum campo não vazio. Ele recusa com 400 `INVALID_REQUEST` quando há nenhuma ou mais de uma forma dada, ou quando a forma dada está incompleta (`:37-42`). Um CPF em branco conta como ausente, o que mantém a recusa da base. O `guestCpf` deixou de ser obrigatório em `HttpApi.java:116-125`.
- **A reserva consultada.** Ela mostra os quatro campos novos, com `null` nos que não se aplicam (`final/src/main/java/hotel/web/ReservationResponse.java:37-41`, via os acessores padrão de `Identification`).
- **A fatura.** Tem "Empresa: … (CNPJ …)" seguida de "Hóspede: …" (`InvoiceLayout.java:19-23`). O ISS é `cents(total × 0,05)` com HALF_EVEN, o mesmo arredondamento da base (`InvoiceLayout.java:28`; `base/src/main/java/hotel/reservations/PriceCalculator.java:51`). Para 779,80 dá exatamente R$ 38,99.
- **O *booking receipt*.** Tem "Booking receipt no. N", "Guest: … (passport …, …)", datas `LocalDate.toString()` (2027-03-10), "1 night" ou "N nights", e `BRL` com `Locale.US` e `%,.2f` (`final/src/main/java/hotel/documents/TextFormat.java:21-23`), o que dá `BRL 1,234.56`. A linha "Discount:" só aparece com desconto (`DocumentLayout.java:51-53`).
- **O CPF.** O `ReceiptLayout`, pelo roteiro do `DocumentLayout.render`, gera texto idêntico ao do `ReservationReceipt.text()` da base, linha a linha.
- **O mesmo documento nos três lugares.** Os três chamam o mesmo `issue`. O e-mail continua em português em volta do `receipt.text()` (`ReservationService.java:68-72`).
- **O JSON.** `text` agora é um componente do record (`ReservationDocument.java:31`) e aparece no JSON de `/check-out` e `/document` (`HttpApi.java:76-79`), junto com os quatro campos novos e os de identificação nulos.

Os relatórios não usavam o CPF, e não vi nada quebrado no resto.
Conclusão: segue

## Dúvidas

A forma usada foi a **fábrica concentrada**: um método estático, `ReservationDocument.issue`, com um `switch` sobre o tipo selado `Identification`. Não é a forma clássica do Factory Method (um criador por tipo com método sobrescrito) nem um método polimórfico em `Identification`. O polimorfismo que existe está nos layouts (que lembram um Template Method), não na escolha.

Fica uma dúvida pequena. A fábrica sempre devolve o mesmo record, `ReservationDocument`, e o que varia é o layout escolhido lá dentro. Não considerei isso o "malfeito" de "devolver sempre o mesmo documento e escolher depois", porque a escolha acontece dentro da fábrica e o texto e o título já saem prontos dela.
