Terminei de ler o patch e os arquivos tocados. Escrevo agora a correção.

# Cenário 05

## Visão geral

O agente juntou a criação do documento num lugar só. A interface `DocumentGenerator` tem três implementações, uma por tipo de hóspede, e a classe `DocumentGenerators` escolhe a implementação por um mapa `GuestType → gerador`. Os três lugares que antes faziam `new ReservationReceipt(...)` (confirmação, check-out e segunda via) agora pedem o documento a essa fábrica. O caso que já existia, o comprovante da pessoa com CPF, também passou para a estrutura, e o `new` direto sumiu da base. O desenho está limpo. Os problemas estão no comportamento. A fatura não mostra a linha de desconto quando a reserva tem desconto. E a validação aceita um CPF junto com um CNPJ sem razão social, ou com um passaporte sem país, descartando calada a identificação incompleta.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:
- `hotel.web`: `HttpApi` (rotas e validação do corpo), `CreateReservationRequest` e `ReservationResponse` (os JSON).
- `hotel.reservations`: `ReservationService` (as regras de estado: criar, confirmar, cancelar, check-in, check-out e segunda via), `Reservation`, `Guest`, `PriceCalculator` e `ReservationStore`, que guarda os dados em memória.
- `hotel.documents`: o documento `ReservationReceipt` e a formatação `TextFormat`.
- `hotel.messages`: o e-mail simulado.
- `hotel.rooms` e `hotel.reports`.

Uma requisição chega à rota do `HttpApi`. Ele valida o corpo e monta um `Guest` e um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:139`), depois chama o `ReservationService`, que lê e grava no `ReservationStore`. O agente fez estes testes:
- `GuestTypesTest`: os três tipos passando pelo serviço, com uma noite e com desconto no *booking receipt*.
- `HttpApiTest`: casos novos para empresa, estrangeiro e as recusas (nenhuma identificação, duas, CNPJ sem razão social, passaporte sem país).

Nos testes que já existiam, ele só ajustou os construtores de `Guest`.

**A parte do pattern.**
- **O tipo de hóspede.** `Guest` ganhou os campos novos e o método `type()`, que deduz o `GuestType` (INDIVIDUAL, COMPANY ou FOREIGNER) pelo campo preenchido (`final/src/main/java/hotel/reservations/Guest.java:12-18`).
- **A fábrica.** `DocumentGenerators` guarda um `Map.of` com um gerador por tipo (`final/src/main/java/hotel/documents/DocumentGenerators.java:10-13`). `forGuest` devolve o gerador do tipo do hóspede (`:18-19`).
- **Os geradores.** Cada um monta o `ReservationReceipt` completo, com título, campos de identificação (os que não se aplicam ficam null) e o `text`:
  - `IndividualReceiptGenerator` reproduz o texto da base.
  - `CompanyInvoiceGenerator` calcula o ISS (`final/src/main/java/hotel/documents/CompanyInvoiceGenerator.java:37`).
  - `ForeignerReceiptGenerator` faz o texto em inglês, com `TextFormat.moneyEnglish` e `TextFormat.dateIso` (`final/src/main/java/hotel/documents/TextFormat.java:30,35`).
- **O documento.** `ReservationReceipt` virou um record só de dados, com o componente `text` (`final/src/main/java/hotel/documents/ReservationReceipt.java:25`).
- **Os três lugares de uso.** Todos pedem à fábrica: `final/src/main/java/hotel/reservations/ReservationService.java:69-70` (confirmação, e o e-mail usa `receipt.text()` na linha 131), `:112` (check-out) e `:121` (segunda via).

**O julgamento.** Quem escolhe qual documento criar é só a fábrica (o mapa). Os três lugares de uso não perguntam o tipo do hóspede, e nenhum deles cria o documento à mão. O caso que já existia passou pela mesma estrutura. A fábrica não devolve sempre o mesmo documento para a escolha acontecer depois: cada gerador cria o seu. É uma fábrica concentrada com produtos polimórficos, forma que as instruções aceitam como acerto.

O `if` em `Guest.type()` existe, mas só deduz o tipo a partir dos dados. Ele aparece uma vez, não se repete nos lugares de uso. Para a agência de turismo, bastaria um valor novo no enum, um gerador e uma entrada no mapa.

**O exagero colateral.** A interface tem três implementações reais. O enum e a fábrica são a própria resposta à variação. Não há camada que só repassa, builder ou fábrica sobrando.

**O comportamento.** Conferi por leitura. A maior parte está certa. Os dois problemas são estes:
- A fatura não tem a linha "Desconto" (`final/src/main/java/hotel/documents/CompanyInvoiceGenerator.java:59-61`). O comprovante da base e o *booking receipt* têm essa linha.
- A validação conta uma identificação incompleta como ausente, e não como erro (`final/src/main/java/hotel/web/HttpApi.java:128-133`).

## Item a item

**O pattern pedido.** A escolha do documento mora só em `DocumentGenerators.forGuest` (`final/src/main/java/hotel/documents/DocumentGenerators.java:10-19`). A fábrica devolve um dos três `DocumentGenerator`, e cada um cria o seu `ReservationReceipt`. A confirmação (`final/src/main/java/hotel/reservations/ReservationService.java:69`), o check-out (`:112`) e a segunda via (`:121`) pedem o documento à fábrica, sem `if` sobre o tipo e sem `new` direto. O construtor `ReservationReceipt(Reservation, Room)` da base foi retirado (`base/src/main/java/hotel/documents/ReservationReceipt.java:24`), e o comprovante de CPF também passa pela fábrica (`final/src/main/java/hotel/documents/IndividualReceiptGenerator.java:11`). Pelas instruções, é a criação concentrada.
Conclusão: acerto

**O exagero colateral.** Não há. `DocumentGenerator` tem três implementações que de fato se substituem, e `GuestType` com `DocumentGenerators` é a estrutura que a variação pede. Fora disso, o agente só acrescentou campos e duas funções de formatação.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem rodar nada.

O que confere:
- **Identificação.** Nenhuma, duas completas, ou só uma incompleta dão 400 INVALID_REQUEST (`final/src/main/java/hotel/web/HttpApi.java:128-136`).
- **Fatura.** Mostra "Empresa: … (CNPJ …)" e o ISS de 5% arredondado com o mesmo `cents` (HALF_EVEN) de sempre: 779,80 × 0,05 dá R$ 38,99 (`final/src/main/java/hotel/documents/CompanyInvoiceGenerator.java:37,61`).
- ***Booking receipt*.** Usa `BRL 1,234.56` via `Locale.US` (`final/src/main/java/hotel/documents/TextFormat.java:30`), datas ISO e "1 night"/"N nights", e põe "Discount:" antes do total (`final/src/main/java/hotel/documents/ForeignerReceiptGenerator.java:49-63`).
- **Os três lugares.** Saem da mesma fábrica, e o e-mail continua em português, com o `text` dentro.
- **JSON.** O documento tem os quatro campos novos e o `text`. A reserva mostra os campos novos, com null nos que não se aplicam (`final/src/main/java/hotel/web/ReservationResponse.java`).
- **Pessoa com CPF.** Recebe o mesmo texto e o mesmo título da base.

O que não confere:
1. **Fatura com desconto.** A fatura nunca mostra a linha "Desconto" (`final/src/main/java/hotel/documents/CompanyInvoiceGenerator.java:59-60`). Numa reserva NON_REFUNDABLE de empresa, "Diárias" e "Total" ficam diferentes sem explicação, embora o pedido diga que a fatura "é parecida com o comprovante".
2. **CPF com identificação incompleta.** Um CPF junto com um `companyCnpj` sem `companyName` (ou com um passaporte sem país) passa como um jeito só. O CNPJ é descartado em silêncio (`final/src/main/java/hotel/web/HttpApi.java:129,132,143`), quando deveria ser recusado como identificação incompleta.
Conclusão: em parte

## Dúvidas

- **Forma usada.** Uma fábrica concentrada (`DocumentGenerators`, com um mapa por `GuestType`) que devolve produtos polimórficos (`DocumentGenerator`). Não é o Factory Method clássico com criador sobrescrito, nem um método no próprio `Guest`.
- **O `if` em `Guest.type()`.** Fica fora da fábrica (`final/src/main/java/hotel/reservations/Guest.java:13-18`), mas só deduz o tipo dos dados e aparece num lugar só. Não pesou contra o acerto.
- **Desconto na fatura.** O exemplo do pedido não tem desconto, então dá para ler a falta da linha como uma decisão aceitável. Contei como falha porque o pedido diz que a fatura é parecida com o comprovante.
- **CPF com CNPJ incompleto.** Se essa combinação conta como "um incompleto" a recusar, é questão de leitura. Pesei como falha porque o pedido exige um jeito, e só um.
