Li o enunciado e o código. Segue a correção.

# Cenário 05

## Visão geral

O agente juntou a escolha do documento num lugar só. A `DocumentRendererFactory` olha qual campo de identificação a reserva tem e devolve um de três renderizadores: `CpfReceiptRenderer`, `CnpjInvoiceRenderer` ou `PassportReceiptRenderer`. Os três lugares onde o documento sai (o e-mail de confirmação, o check-out e a segunda via) pedem o documento pelo mesmo `DocumentService.generateDocument`. Nenhum deles cria o documento à mão nem pergunta o tipo de hóspede. O desenho está certo, e um documento de agência entraria com um renderizador novo e um ramo novo na fábrica. O problema mais sério está no comportamento. O dinheiro é escrito com `String.format("%.2f")` e com troca de vírgula por ponto, e isso depende do locale padrão da JVM, que o projeto nunca fixa. Num servidor com locale inglês, o comprovante e a fatura saem com `R$ 779.80` e o *booking receipt* sai com `BRL 779,80`. Em `pt_BR`, falta o separador de milhar em `R$`. Há também falhas na parte comum, e os testes do agente se resumem a um único `assertNotNull`.

## Como foi implementado, e por que o julgamento

**O projeto.** Fica em `pousada-mare-alta/system`, com estes pacotes:

- `controller`: Javalin, um controller por recurso;
- `service`: reservas, preço, mensagens, relatórios, quartos e documento;
- `domain`: `Reservation` (classe mutável com as transições de estado), `Room`, o enum `RoomType` com diária e capacidade, `RatePlan`, `ReservationStatus` e `Message`;
- `dto`: as respostas JSON, montadas à mão;
- `document`: a parte do pattern;
- `util.MoneyRounder`: arredondamento HALF_EVEN feito a partir de um `double`.

O `Application.main` monta tudo à mão, sem injeção de dependência, e sobe o Javalin na porta de `PORT`, ou na 7070 se ela não vier (`system/src/main/java/com/pousada/Application.java:18-38`). Os dados ficam só em memória: um `TreeMap` de reservas em `ReservationService.java:14` e uma lista de mensagens em `MessageService.java:9`.

Uma requisição segue este caminho. A rota cai em `ReservationController`, que valida o corpo e confere o status antes de chamar o serviço (por exemplo, `ReservationController.java:198-202`). Depois vem `ReservationService` e, por fim, os métodos de transição da própria `Reservation` (`Reservation.java:133-161`). O preço é calculado em `PricingService.calculatePrice` (`PricingService.java:35-58`).

O único teste é `ApplicationTest.java:8-10`, que só confere se a classe `Application` existe.

**A parte do pattern.**

- A interface `DocumentRenderer` tem um único método, `render(Reservation)`, que devolve um `DocumentContent` (`document/DocumentRenderer.java:5-7`).
- Cada caso tem a sua implementação:
  - `CpfReceiptRenderer` monta o comprovante (`CpfReceiptRenderer.java:16-58`);
  - `CnpjInvoiceRenderer` monta a fatura com o ISS de 5% (`CnpjInvoiceRenderer.java:24` e `:41`);
  - `PassportReceiptRenderer` monta o recibo em inglês (`PassportReceiptRenderer.java:16-60`).
- Cada renderizador preenche o texto e também os cinco campos de identificação do `DocumentContent`, com `null` nos que não se aplicam.
- A escolha acontece só em `DocumentRendererFactory.getRenderer`, pelo campo que não é nulo (`DocumentRendererFactory.java:13-24`).
- `DocumentService.generateDocument` pede o renderizador à fábrica e chama `render` (`DocumentService.java:14-17`).
- Os três lugares usam esse serviço:
  - a confirmação, que põe `document.text()` dentro do e-mail em português (`ReservationController.java:206-210`);
  - o check-out (`ReservationController.java:287-288`);
  - a segunda via (`ReservationController.java:313-314`).

**O julgamento.** As instruções aceitam como acerto a "fábrica concentrada" e dão como omissão o `if` repetido nos três lugares. Aqui o `if` sobre o tipo de hóspede aparece uma vez só, dentro da fábrica, e quem consome o documento não sabe qual caso recebeu. Os três casos, inclusive o comprovante com CPF que já existia, passam pela mesma estrutura. Isso dá **acerto**.

No exagero colateral pesou o `DocumentService`. Ele só repassa: chama a fábrica e o `render`, sem nenhuma regra própria, e o controller poderia chamar a fábrica direto. É pouco, mas é o caso de "uma camada que só repassa" que o enunciado da avaliação cita.

No comportamento pesaram a formatação do dinheiro, que depende do locale nos três documentos, e falhas na parte comum: o relatório de ocupação conta reservas pendentes, e o `refund` fica `null` em reservas canceladas.

## Item a item

**O pattern pedido.** A escolha de qual documento criar mora só em `DocumentRendererFactory.getRenderer` (`system/src/main/java/com/pousada/document/DocumentRendererFactory.java:15-23`). Cada tipo de hóspede tem a sua classe concreta de `DocumentRenderer`:

- `CpfReceiptRenderer.java:7`
- `CnpjInvoiceRenderer.java:8`
- `PassportReceiptRenderer.java:7`

Os três lugares pedem o documento pelo mesmo caminho, `documentService.generateDocument(reservation)`:

- o e-mail de confirmação, em `ReservationController.java:206`;
- o check-out, em `:287`;
- a segunda via, em `:313`.

Nenhum dos três cria o documento à mão nem testa o tipo de hóspede. É uma fábrica concentrada, que as instruções aceitam como acerto. O caso que já existia (o CPF) também passa por ela.

Conclusão: acerto

**O exagero colateral.** `DocumentService` (`system/src/main/java/com/pousada/service/DocumentService.java:7-18`) é uma camada que só repassa para a fábrica e para o `render`, sem regra própria. O resto não mostra estrutura sobrando. Não há interfaces com uma implementação só fora do pattern; os serviços e os DTOs são concretos e diretos.

Conclusão: exagero

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

O que confere:

- **Identificação.** A reserva exige exatamente um entre `guestCpf`, `companyCnpj` e `guestPassport`. Também exige `companyName` junto do CNPJ e `guestCountry` junto do passaporte, e recusa os outros casos com 400 INVALID_REQUEST (`ReservationController.java:96-114`).
- **Os cinco campos.** Saem na reserva (`ReservationResponse.java:73-77`) e no documento, que também traz `text` (`DocumentResponse.java:63-67` e `:76`).
- **ISS.** É `round(total * 0.05)`, e 779,80 dá 38,99 (`CnpjInvoiceRenderer.java:24`).
- **Datas.** Saem `dd/MM/yyyy` em português e `yyyy-MM-dd` no recibo em inglês (`PassportReceiptRenderer.java:9`).
- **Rótulos.** "1 night"/"N nights" e "1 diária"/"N diárias" estão certos (`PassportReceiptRenderer.java:20`, `CpfReceiptRenderer.java:22`).
- **Desconto.** A linha de desconto entra antes do total.
- **E-mail.** É sempre em português, com o documento no meio (`ReservationController.java:321-329`).
- **Segunda via.** Só sai para reserva paga (`ReservationController.java:306-311`).

O que não confere:

- **Valores no texto dos documentos.** `"R$ %.2f"` (`CpfReceiptRenderer.java:30` e `:36`, `CnpjInvoiceRenderer.java:34-41`) depende do locale padrão, e nenhum `Locale` é definido no projeto.
  - Em locale inglês sai `R$ 779.80`, e nunca há separador de milhar (`R$ 1234,56` em vez de `R$ 1.234,56`).
  - O recibo em inglês formata com `%,.2f` e depois troca vírgula por ponto (`PassportReceiptRenderer.java:29-38`). Em locale inglês isso dá `BRL 779,80` e `BRL 1.234,56`, o contrário do pedido. Só sai certo se a JVM estiver em `pt_BR`.
  - Os relatórios CSV também usam `%.2f` (`ReportService.java:54` e `:60`). Em `pt_BR` sairiam `599,76`, quebrando o CSV.
- **Ocupação.** O relatório conta reservas pendentes, porque o filtro só exclui as canceladas (`ReservationService.java:122`). Deveria contar só as pagas.
- **Reembolso.** Uma reserva cancelada sem nada a devolver fica com `refund` `null`, em vez de zero (`ReservationService.java:100`).
- **Hóspedes negativos.** Dão 422 CAPACITY_EXCEEDED em vez de 400 (`ReservationService.java:38-39` e `ReservationController.java:155-157`).
- **Dinheiro no JSON.** Vai como `double`, então não sai garantido com 2 casas (sairia `779.8`).

Conclusão: em parte

## Dúvidas

- **A forma do pattern.** Foi a fábrica concentrada: uma classe que escolhe o renderizador pelo campo de identificação preenchido. Não é a forma clássica com um criador sobrescrito por tipo, nem um método polimórfico no tipo de hóspede. O tipo de hóspede não existe como objeto: são cinco campos que podem ser nulos em `Reservation` (`Reservation.java:10-14`), e a fábrica deduz o caso pelos nulos. Isso não tira a centralização, mas o tipo fica implícito.
- **O formato do dinheiro.** Se ele sai certo depende do locale da máquina onde o jar roda. Julguei pelo caso comum de um servidor em inglês ou com locale `C`, e mesmo em `pt_BR` falta o separador de milhar em `R$`.
- **Corpo que não é JSON.** O controller trata só `JsonSyntaxException`, que é do Gson (`ReservationController.java:163`). Mas `ctx.bodyAsClass` usa o mapeador padrão do Javalin, que é o Jackson, também presente no `pom.xml`. Pela leitura, não dá para afirmar se um corpo que não é JSON vira 400 INVALID_REQUEST ou outro erro.
- **As datas no JSON do documento.** `checkIn` e `checkOut` vão no formato do próprio documento (`CnpjInvoiceRenderer.java:56-57`), não no ISO pedido para a API em geral. O enunciado não define esse ponto com clareza, então não pesei.
