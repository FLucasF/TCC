# Cenário 05

## Visão geral

O agente montou um sistema enxuto em camadas: um controller do Javalin, serviços em memória e utilitários de formatação. A escolha do documento por tipo de hóspede ficou num lugar só, o método `DocumentService.generateDocument` (`src/main/java/br/com/pousada/service/DocumentService.java:11-45`). Os três lugares onde o documento sai chamam esse método: o e-mail de confirmação, o check-out e a segunda via. Nenhum deles decide sozinho qual documento criar. Isso é uma fábrica concentrada, que as instruções aceitam como acerto. O ponto fraco do desenho é que o "tipo de hóspede" não existe como conceito no código. A fábrica descobre o tipo olhando quais campos anuláveis estão preenchidos, e o terceiro caso é o `else` que sobra. Não há exagero de estrutura. No comportamento, as regras da parte nova conferem na leitura. Os problemas estão na parte comum: o relatório financeiro inclui reservas que não deixaram dinheiro, um corpo que não é JSON provavelmente vira 500, e os valores zero saem como `0` em vez de `0.00`.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `br.com.pousada` tem quatro partes:
- `App` monta tudo à mão e sobe o servidor (`src/main/java/br/com/pousada/App.java:236-258`). A porta vem de `PORT`, com 7070 se ela não vier (`App.java:260-270`). O Jackson é configurado com `JavaTimeModule` para as datas.
- `controller.ApiController` registra as rotas e traduz as exceções em `{error, message}` (`src/main/java/br/com/pousada/controller/ApiController.java:27-88`).
- `service` reúne:
  - `RoomService`: os cinco quartos fixos num `HashMap` (`src/main/java/br/com/pousada/service/RoomService.java:319-325`);
  - `ReservationService`: as reservas num `HashMap`, numeradas por um `AtomicLong` que começa em 1, com o ciclo de estados e as regras (`src/main/java/br/com/pousada/service/ReservationService.java`);
  - `MessageService`: a lista de mensagens enviadas;
  - `ReportService`: os dois CSVs;
  - `DocumentService`: os documentos.
- `model` tem classes de dados simples (`Reservation`, `Document`, `Message`, `Room`, os enums). `util` tem formatadores de dinheiro, data, CPF e CNPJ, e o `PriceCalculator`.

O caminho de uma requisição: a rota no `ApiController` lê o corpo como `Map` e valida os campos, inclusive a identificação. Depois chama o `ReservationService`, que aplica as regras de quarto, capacidade, datas e conflito, calcula os preços com o `PriceCalculator` e guarda a reserva no mapa em memória. O jar é gerado pelo `maven-shade-plugin`, com `mainClass` `br.com.pousada.App`. Os testes que o agente escreveu são unitários: `PriceCalculatorTest` cobre a temporada, o desconto e o ISS de 38,99, e `ReservationServiceTest` cobre a criação com CPF, CNPJ e passaporte, a capacidade, a indisponibilidade, a confirmação, o cancelamento e o check-in/check-out. Não há teste da API HTTP.

**A parte que pede o pattern.** A reserva guarda os cinco campos de identificação como `String` anuláveis (`src/main/java/br/com/pousada/model/Reservation.java:12-16`). Nenhuma classe ou enum representa o tipo de hóspede.

`DocumentService.generateDocument` escolhe o caso (`src/main/java/br/com/pousada/service/DocumentService.java:15-24`):
- com `guestCpf` preenchido, sai o comprovante;
- senão, com `companyCnpj` preenchido, sai a fatura;
- senão, sai o *booking receipt*.

Cada documento tem o seu texto montado num método privado: `generateComprovante` (`DocumentService.java:47-66`), `generateFatura` (`DocumentService.java:68-89`, com o ISS na linha 86) e `generateBookingReceipt` (`DocumentService.java:91-111`). O produto é uma única classe de dados, `Document`, com `title`, os cinco campos e `text` (`src/main/java/br/com/pousada/model/Document.java`).

Os três consumidores pedem o documento sempre a esse método:
- o e-mail de confirmação (`ReservationService.java:529`), que só embrulha `document.getText()` no texto fixo em português (`ReservationService.java:532-540`);
- o check-out (`ReservationService.java:510`);
- a segunda via (`ReservationService.java:524`).

**O raciocínio do julgamento.**

O que aproxima do pattern: a decisão de qual documento criar existe uma vez só, e nenhum dos três lugares faz `if` sobre o tipo de hóspede ou monta texto à mão. Um documento para agências seria um ramo novo no mesmo método, sem mexer nos consumidores. É a forma "um método que escolhe o documento pelo tipo de hóspede" que as instruções contam como acerto.

O que afasta da forma ideal:
- o tipo é deduzido de campos anuláveis, não modelado;
- o último caso é um `else` que pega tudo que não for CPF nem CNPJ;
- a fábrica é uma cadeia de `if` dentro de um serviço, e não um criador polimórfico.

Nada disso cai em `malfeito` ou `omissão` pelas instruções. Não há criação à mão em nenhum dos três lugares, a escolha não acontece fora da fábrica, e o `if` não se repete.

Sobre o exagero: não há interfaces com uma implementação só, nem fábricas, builders ou camadas que só repassam. Os serviços são classes concretas montadas no `main`.

Sobre o comportamento: as regras da parte nova conferem na leitura. Os desvios estão na parte comum do sistema e são detalhados no item.

## Item a item

**O pattern pedido.** Há uma fábrica concentrada num método: `DocumentService.generateDocument` decide qual documento criar pelo jeito de identificar o hóspede (`src/main/java/br/com/pousada/service/DocumentService.java:15-24`) e devolve o `Document` com o título e o texto do caso. Os três lugares pedem o documento a ela:
- o e-mail de confirmação, em `src/main/java/br/com/pousada/service/ReservationService.java:529`;
- o check-out, em `ReservationService.java:510`;
- a segunda via, em `ReservationService.java:524`.

Nenhum desses lugares escolhe o caso ou cria o documento à mão, e um caso novo (o das agências) entraria só nesse método. É a forma "fábrica concentrada" que as instruções aceitam. A fragilidade é que o tipo de hóspede não está modelado: ele é inferido de campos anuláveis, com o *booking receipt* como `else` final (`DocumentService.java:21-23`). Isso não muda a classe.
Conclusão: acerto

**O exagero colateral.** Não há. As classes são concretas e montadas diretamente em `src/main/java/br/com/pousada/App.java:239-245`. Não há interface com uma implementação só, fábrica, builder ou camada de repasse fora da parte do pattern. Os formatadores em `util/` têm uso real e repetido.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço nem chamar a API.

O que confere com o pedido:
- **Identificação.** O controller conta os jeitos completos e recusa quando não há exatamente um (`src/main/java/br/com/pousada/controller/ApiController.java:117-124`). Também recusa um par incompleto mesmo quando há outro jeito válido (`ApiController.java:126-137`). Nenhum, dois ou um incompleto dão 400 INVALID_REQUEST, pelo handler de `IllegalArgumentException` (`ApiController.java:82-87`).
- **Fatura.** Mostra empresa, CNPJ e hóspede no lugar do CPF (`DocumentService.java:70-73`). O ISS é `round(total × 0,05)` com HALF_EVEN (`src/main/java/br/com/pousada/util/PriceCalculator.java:483-485`), e 779,80 dá 38,99.
- **Booking receipt.** Sai em inglês, com `BRL 1,234.56` (`src/main/java/br/com/pousada/util/MoneyFormatter.java:410-412`), datas `yyyy-MM-dd` (`src/main/java/br/com/pousada/util/DateFormatter.java:372`) e "night"/"nights" (`DocumentService.java:101`). O desconto entra antes do total (`DocumentService.java:104-106`).
- **Comprovante e e-mail.** O comprovante segue o modelo do enunciado (`DocumentService.java:47-66`). O e-mail tem o assunto e o texto pedidos e embute o mesmo `text` (`ReservationService.java:531-542`).
- **Documento.** Sai idêntico nos três lugares, porque todos chamam o mesmo método. O JSON tem os cinco campos, com null nos que não se aplicam, e `text` (`src/main/java/br/com/pousada/model/Document.java`). A segunda via só sai para reserva paga (`ReservationService.java:517-521`).
- **Parte comum.** Os estados, o cancelamento com reembolso e os conflitos de quarto conferem.

O que não confere:
- O relatório financeiro filtra apenas `!= PENDING` (`src/main/java/br/com/pousada/service/ReportService.java:59`). Por isso inclui, com valor 0.00, as reservas STANDARD canceladas com reembolso integral e as canceladas ainda pendentes. O enunciado pede só as que "deixaram dinheiro com a pousada".
- O corpo vai por `ctx.bodyAsClass(Map.class)` (`ApiController.java:96`) sem tratar o erro de parse do Jackson. Um corpo que não é JSON deve virar 500, e não 400. Um campo com tipo errado, como `roomNumber` numérico, gera `ClassCastException` (`ApiController.java:98`), também 500.
- O desconto zero e o `amountPaid` inicial são `BigDecimal.ZERO` (`PriceCalculator.java:476`, `src/main/java/br/com/pousada/model/Reservation.java:55`). Eles saem no JSON como `0`, e não com 2 casas.

Conclusão: em parte

## Dúvidas

- A forma usada foi a fábrica concentrada (um método que escolhe pelo tipo de hóspede), e não o Factory Method clássico nem um método polimórfico no tipo de hóspede. Há uma dúvida menor: o tipo de hóspede não é modelado e é deduzido da presença de campos anuláveis, e o produto é uma única classe `Document` diferenciada só por `title` e `text`. Pelas instruções, a escolha concentrada num lugar basta para o acerto, por isso não rebaixei a classe.
- O 500 para corpo não-JSON é inferido do comportamento padrão do `bodyAsClass` do Javalin 6, sem handler para a exceção do Jackson. Não foi confirmado executando.
