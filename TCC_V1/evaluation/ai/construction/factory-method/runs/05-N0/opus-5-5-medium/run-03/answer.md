# Cenário 05

## Visão geral

O agente montou um sistema Javalin simples, em camadas de controller e service, com os dados em memória. Concentrou a escolha do documento por tipo de hóspede num lugar só, o método estático `DocumentService.generateDocument`, e os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) pedem o documento a esse método. Isso é uma fábrica concentrada, que as instruções aceitam como acerto. Não há estrutura sobrando. O problema mais sério está no comportamento: a formatação de dinheiro nos textos dos documentos está trocada. O comprovante e a fatura saem com `R$ 779.80`, quando deveria ser `R$ 779,80`, e o *booking receipt* sai com `BRL 779,80`, quando deveria ser `BRL 779.80`. Nenhum dos dois põe separador de milhar. Com isso, o texto de todos os documentos, inclusive o comprovante do ponto de partida, sai diferente do que foi pedido.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousamarealta` tem quatro subpacotes:
- `controllers`: `RoomController` e `ReservationController`, este com todas as rotas de reserva, mensagens e relatórios;
- `services`: `ReservationService` (guarda as reservas num `HashMap` e muda o status), `PriceService` (métodos estáticos de preço e ISS), `DocumentService` (estático, gera o documento), `MessageService` (a lista de mensagens enviadas), `ReportService` (os CSVs) e `RoomService` (os cinco quartos fixos);
- `models`: `Reservation`, `Document`, `Room`, `Message` e os enums `RatePlan` e `ReservationStatus`, todos objetos de dados com getters e setters;
- `dtos`: `CreateReservationRequest` e `ErrorResponse`.

Uma requisição entra pela rota em `ReservationController.setupRoutes` (`src/main/java/com/pousamarealta/controllers/ReservationController.java:27-38`). O controller valida a entrada, confere o status da reserva e devolve o erro adequado. Depois chama o `ReservationService` e, quando precisa, o `DocumentService` e o `MessageService`. Todos os dados ficam em memória: as reservas em `ReservationService.java:9`, os quartos em `RoomService.java:249-255` e as mensagens em `MessageService.java:7`. O `App.main` monta os services à mão, lê a variável `PORT` (com 7070 como padrão) e registra as rotas (`App.java:9-35`). O jar é gerado pelo shade plugin. O único teste é `PriceServiceTest`, que cobre temporada, diária, soma das noites, desconto e número de noites. Não há nenhum teste de documento, de API ou de identificação.

**A parte que pede o pattern.** Não existe um tipo de hóspede explícito. A reserva guarda os cinco campos de identificação soltos (`models/Reservation.java:11-15`). O documento é uma classe só, `models/Document.java`, com `title`, os cinco campos e `text`. A criação mora em `DocumentService.generateDocument` (`services/DocumentService.java:12-42`). O método preenche os campos comuns e decide, numa cadeia `if/else if` sobre qual identificador está presente (`DocumentService.java:30-39`), o título e o texto. O texto de cada tipo vem de um auxiliar privado: `generateComprovante` (`:44-61`), `generateFatura` (`:63-84`, com o ISS calculado por `PriceService.calculateISS`, `PriceService.java:66-69`) e `generateBookingReceipt` (`:86-105`). Os três consumidores só chamam a fábrica:
- a confirmação (`ReservationController.java:164`), que embute `doc.getText()` no e-mail (`:165`, `:307-313`);
- o check-out (`:237`);
- a segunda via (`:263`).

Nenhum deles pergunta pelo tipo de hóspede.

**O raciocínio.** O que as instruções exigem é que a decisão de qual documento criar fique num lugar só e que os três lugares peçam o documento a esse lugar. As duas coisas estão no código. A decisão aparece uma única vez (`DocumentService.java:30-39`), e confirmação, check-out e segunda via chamam o mesmo método. Nenhum dos três monta o documento à mão, e a fábrica não devolve um documento genérico para alguém escolher depois. Para criar o documento da agência basta mexer nesse método (um ramo e um auxiliar novos), sem tocar nos consumidores. O que afasta o desenho do ideal é pouco: a escolha é feita por presença de campos nulos, e não por um tipo de hóspede, e a fábrica é estática. Isso não muda a classe. No exagero colateral não há interfaces, fábricas extras nem camadas de repasse. Os services estáticos e as classes de dados são o mínimo. No comportamento pesou a formatação de dinheiro errada em todos os textos (`DocumentService.java:107-119`), além de recusas com o código errado (menos de um hóspede).

## Item a item

**O pattern pedido.** A escolha do documento está concentrada em `DocumentService.generateDocument` (`src/main/java/com/pousamarealta/services/DocumentService.java:12-42`), com o único `if` sobre o tipo de identificação em `:30-39`. Os três lugares pedem o documento a esse método:
- o e-mail de confirmação, em `controllers/ReservationController.java:164-167`;
- o check-out, em `controllers/ReservationController.java:237`;
- a segunda via, em `controllers/ReservationController.java:263`.

Nenhum deles decide o tipo nem monta texto à mão. Os três casos, inclusive o comprovante que já existia, passam pela mesma fábrica. É a forma de fábrica concentrada que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. Não existem interfaces com uma implementação só, builders nem camadas que só repassam. Os services são classes simples ou métodos estáticos (`services/PriceService.java`, `services/DocumentService.java`), e o `App` monta tudo diretamente (`App.java:9-35`).
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço nem chamei a API.

O que confere:
- **Identificação:** a recusa de nenhum ou de mais de um identificador está certa (`ReservationController.java:58-66`), assim como a de CNPJ sem razão social e a de passaporte sem país (`:68-76`).
- **Fatura:** mostra a empresa no lugar do CPF (`DocumentService.java:66-67`), e o ISS de 5% sai com HALF_EVEN, o que dá 38,99 para 779,80 (`PriceService.java:66-69`).
- **Booking receipt:** está em inglês, com datas `yyyy-MM-dd` e "1 night"/"N nights" (`DocumentService.java:93-95`).
- **Os três lugares:** o documento é o mesmo nos três, porque vem da mesma função, e o JSON tem `text` e os cinco campos (`models/Document.java`).
- **E-mail:** em português, com o documento no meio (`ReservationController.java:307-313`).
- **Preços:** os cálculos seguem o pedido, com 599,76 para o exemplo de julho (`PriceService.java:28-60`).

O que não confere:
- **Dinheiro nos textos.** `formatMoney` usa `"R$ %d.%02d"` (`DocumentService.java:111`), o que dá `R$ 779.80` em vez de `R$ 779,80`. `formatMoneyEN` usa `"BRL %d,%02d"` (`:118`), o que dá `BRL 779,80` em vez de `BRL 779.80`. Nenhum dos dois põe separador de milhar: um total de 1.234,56 sairia `R$ 1234.56` e `BRL 1234,56`. Com isso, o texto do comprovante, da fatura (inclusive `ISS retido (5%): R$ 38.99`), do *booking receipt* e do e-mail sai sempre diferente do pedido.
- **Menos de um hóspede.** Recebe 422 `CAPACITY_EXCEEDED` em vez de 400 `INVALID_REQUEST` (`ReservationController.java:84-86`). O mesmo acontece quando o campo `guests` falta, porque ele vale 0 por padrão (`dtos/CreateReservationRequest.java:14`).
- **Campos de outro jeito de identificação.** A validação só conta `guestCpf`, `companyCnpj` e `guestPassport` (`:59-61`). Um CPF junto com `companyName` ou com `guestCountry` é aceito.
- **Datas no JSON do documento.** Vêm em `dd/MM/yyyy` para todos os tipos (`DocumentService.java:23-24`), inclusive para o estrangeiro, enquanto o resto da API usa `2027-03-10`.

Conclusão: em parte

## Dúvidas

- **A forma usada** é a fábrica concentrada, num método estático que escolhe pelo identificador preenchido. Não é o Factory Method clássico nem um método polimórfico no tipo de hóspede, porque o projeto não tem um tipo de hóspede. O documento também não tem subclasses: é uma classe só com título e texto variáveis.
- **Peso da formatação no comportamento.** Pesei entre `em parte` e `não segue`. Fiquei com `em parte` porque a identificação, o ISS, a escolha do documento, os três lugares e os preços estão certos. Mas o texto de todos os documentos sai fora do formato pedido.
- **Datas da reserva no JSON.** A `Reservation` guarda `LocalDate`, e o `App` usa `new JavalinJackson()` sem configuração própria (`App.java:27-29`). Só pela leitura não deu para garantir que as datas saem como `2027-07-10` e não como array ou timestamp, porque isso depende do mapper padrão do Javalin 6.7.0. Também `amountPaid` começa como `BigDecimal.ZERO` (`ReservationService.java:44`), o que pode sair `0` e não `0.00`. Não contei isso no julgamento.
