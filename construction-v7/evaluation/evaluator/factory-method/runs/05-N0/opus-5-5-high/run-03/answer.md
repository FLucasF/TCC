# Cenário 01

## Visão geral

O agente construiu o sistema em camadas simples (handlers, services, um formatador de texto e um calculador de preço) e escolhe o documento num lugar só. O método privado `ReservationService.generateDocument` decide entre comprovante, fatura e *booking receipt* pelo tipo de identificação do hóspede. Os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) passam todos por ele. Isso é a forma de "fábrica concentrada" que as instruções aceitam como acerto. Não há estrutura sobrando no resto do sistema. O problema mais sério está no comportamento. O dinheiro em reais não tem separador de milhar, então sai "R$ 1159,80" no lugar de "R$ 1.159,80", e isso aparece no comprovante e na fatura. A validação da identificação aceita um CPF acompanhado de um CNPJ sem razão social. E a parte comum tem falhas: segunda via para reserva cancelada, corpo que não é JSON dando 500, e o relatório financeiro listando uma reserva cancelada com reembolso total, que não deixou dinheiro.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` tem quatro partes:
- `handler/`: `ReservationHandler`, `RoomHandler`, `MessageHandler` e `ReportHandler` leem a requisição e montam o JSON de resposta com `Map`.
- `service/`: `ReservationService` guarda as reservas num `HashMap` e cuida das regras de estado e do documento. `RoomService` tem os cinco quartos fixos no construtor. `MessageService` é a lista de mensagens enviadas. `ReportService` gera os CSVs. `ServiceException` leva o código, a mensagem e o status HTTP.
- `model/`: classes de dados (`Reservation`, `GuestIdentification`, `Document`, `Room`, `Message`) e enums.
- `util/`: `PriceCalculator` (diárias, alta temporada, desconto, ISS, arredondamento `HALF_EVEN`) e `DocumentFormatter` (o texto de cada documento e o do e-mail).

`Application.main` monta tudo à mão, sem injeção de dependência (`Application.java:17-25`). Ele configura o Jackson, registra as rotas (`Application.java:37-47`) e transforma a `ServiceException` na resposta de erro (`Application.java:49-54`). A porta vem de `PORT` e cai para 7070 (`Application.java:67-77`).

Uma requisição percorre este caminho. Em `POST /reservations`, o handler lê um `JsonNode`, confere os campos obrigatórios e monta a `GuestIdentification` (`ReservationHandler.java:21-84`). Depois chama `ReservationService.createReservation`, que valida quarto, capacidade, hóspedes, datas e disponibilidade, calcula os preços e guarda a reserva (`ReservationService.java:24-69`). Os dados ficam só na memória.

Os testes do agente são 23 testes de unidade, todos passando no `mvn verify`: 8 do `PriceCalculator`, 11 do `ReservationService` e 4 do `DocumentFormatter`. Nenhum teste passa pela API HTTP.

**A parte do fim do enunciado.** O tipo de hóspede é representado por uma `GuestIdentification` com cinco campos que podem ser nulos. Três fábricas estáticas a criam (`withCpf`, `withCompany`, `withPassport`, em `GuestIdentification.java:19-29`), e três métodos dizem qual é o caso (`isCpf`/`isCompany`/`isPassport`, em `GuestIdentification.java:51-61`).

A escolha do documento mora só em `ReservationService.generateDocument` (`ReservationService.java:152-189`). O `if (id.isCpf()) … else if (id.isCompany()) … else` define o título e chama `DocumentFormatter.formatReceipt`, `formatInvoice` ou `formatBookingReceipt` (`ReservationService.java:159-168`). Em seguida o método monta um único `Document` com os cinco campos de identificação e o `text`. Os três lugares pedem o documento a esse método:
- o e-mail de confirmação, em `sendConfirmationEmail` (`ReservationService.java:192`), que só embrulha `document.getText()` no texto do e-mail (`DocumentFormatter.java:119-128`);
- o check-out (`ReservationService.java:137`);
- a segunda via (`ReservationService.java:149`).

As regras de cada documento ficam no `DocumentFormatter`:
- o comprovante em `DocumentFormatter.java:17-37`;
- a fatura em `DocumentFormatter.java:39-63`, com o ISS em `PriceCalculator.calculateIss` (`PriceCalculator.java:42-45`);
- o *booking receipt* em `DocumentFormatter.java:65-85`, com data ISO, "1 night"/"N nights" e `BRL` em `Locale.US` (`DocumentFormatter.java:87-111`).

**O raciocínio.** Para as instruções, o que conta é onde se decide qual documento criar. Aqui essa decisão aparece uma vez só, e todos os usos passam por ela. Não existe `if` sobre o tipo de hóspede no e-mail, no check-out nem na segunda via. A comparação de idioma (`"en".equals(lang)`) dentro do formatador é detalhe de formatação, não escolha de documento.

O que afasta o desenho da forma clássica: não há criador com o método sobrescrito por tipo nem polimorfismo no tipo de hóspede. O tipo é deduzido de campos nulos, e a fábrica é um método privado do serviço. Uma agência nova vai mexer em `GuestIdentification`, no `parseIdentification` do handler, no formatador e numa nova linha do `if`. Mas a decisão de qual documento criar continua num lugar só. Isso é a "fábrica concentrada" que as instruções aceitam, então a classe é acerto.

No exagero colateral não pesou nada. As camadas fazem trabalho de verdade, e não há interface com uma implementação só, builder ou fábrica sobrando.

No comportamento pesaram:
- a formatação de reais sem separador de milhar (`DocumentFormatter.java:113-115`);
- a validação que aceita CPF junto com um CNPJ incompleto (`ReservationHandler.java:61-72`);
- falhas da parte comum: segunda via de cancelada (`ReservationService.java:144`), 500 para corpo que não é JSON, financeiro com reserva cancelada que não deixou dinheiro (`ReportService.java:61-62`), e `discount`/`amountPaid` saindo `0` em vez de `0.00` no JSON.

## Item a item

**O enunciado (pede o pattern).** A escolha de qual documento criar está concentrada em `ReservationService.generateDocument` (`ReservationService.java:152-189`), num `if/else` sobre o tipo de identificação (`ReservationService.java:159-168`). Os três lugares só chamam esse método: o e-mail de confirmação (`ReservationService.java:192`), o check-out (`ReservationService.java:137`) e a segunda via (`ReservationService.java:149`). Nenhum deles cria o documento à mão ou pergunta qual é o tipo de hóspede, e a fábrica devolve o documento já escolhido, com título e texto certos. Os três tipos, inclusive o CPF da parte comum, passam pela mesma fábrica. É a forma "fábrica concentrada" que as instruções aceitam como pattern.
Conclusão: acerto

**O exagero colateral.** Não há. Os handlers convertem a requisição e a resposta, os services guardam o estado e as regras, e `PriceCalculator` e `DocumentFormatter` são utilitários estáticos usados de fato. Não há interface com uma implementação só, fábrica, builder ou camada que só repasse. As fábricas estáticas de `GuestIdentification` (`GuestIdentification.java:19-29`) são construtores com nome, sem estrutura extra.
Conclusão: contenção

**O comportamento.** Testei com o serviço no ar, chamando a API com `curl`.

O que conferiu:
- **Identificação.** Nenhum jeito, dois jeitos, CNPJ sem razão social e passaporte sem país dão 400 com INVALID_REQUEST.
- **Campos na reserva.** Os cinco campos de identificação vêm na reserva, com null nos que não se aplicam.
- **Comprovante e fatura.** O e-mail do CPF tem o comprovante exatamente como o enunciado mostra. A fatura de 779,80 mostra "Empresa: Tecnosul Ltda (CNPJ …)" e "ISS retido (5%): R$ 38,99".
- **Booking receipt.** Sai em inglês, com "BRL 1,159.80", datas "2027-03-10", "2 nights", e com "1 night" e "Discount: BRL 44.98" numa diária não reembolsável.
- **Mesmo documento nos três lugares.** O e-mail continua em português, com o documento certo dentro. O JSON do check-out tem `title` e `text` e é idêntico ao da segunda via, nos três tipos.
- **Parte comum.** STANDARD de 10/07 a 12/07 dá 599,76. Os 409 (ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED), o 422, os 404 e os 400 de campo faltando, de datas e de hóspedes conferem. Reembolso total no plano normal e zero no não reembolsável conferem. O CSV de ocupação ("total,3/5", `text/csv; charset=utf-8`) confere.
- **Testes do agente.** O `mvn verify` passa, com 23 testes e 0 falhas.

O que não conferiu:
- **Separador de milhar em reais.** Fatura e comprovante saem sem ele: "Diárias: R$ 1159,80", "R$ 1391,76", "Total: R$ 1183,00" (`DocumentFormatter.java:113-115`). O enunciado pede R$ 1.234,56.
- **CPF com CNPJ incompleto.** `guestCpf` junto com `companyCnpj` sem `companyName` é aceito com 201, e o CNPJ é descartado em silêncio. O mesmo acontece com um `guestCpf` vazio.
- **Segunda via de reserva cancelada.** É emitida com 200 (`ReservationService.java:144` só recusa PENDING).
- **Corpo que não é JSON.** Dá 500 "Server Error" em vez de 400.
- **Relatório financeiro.** Lista a reserva STANDARD cancelada e reembolsada com `0.00`, embora ela não tenha deixado dinheiro (`ReportService.java:61-62`).
- **Valores zero no JSON.** `discount` e `amountPaid` saem `0`, e não `0.00`.

Conclusão: em parte

## Dúvidas

A forma usada é a fábrica concentrada: um método privado (`ReservationService.java:152`) com `if/else` sobre o tipo de hóspede. Não é o Factory Method clássico nem um método polimórfico no tipo de hóspede. O tipo não é um tipo do Java: é deduzido de campos nulos em `GuestIdentification` (`GuestIdentification.java:51-61`). O `parseIdentification` do handler também faz um `if` por tipo (`ReservationHandler.java:71-83`), mas para ler a entrada, não para criar o documento, então não pesou na classe. Também ficou em aberto se aceitar CPF junto com um CNPJ incompleto viola o "recusa um incompleto" do enunciado. Li que sim, mas o texto permite discutir.
