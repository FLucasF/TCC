Terminei a leitura do patch e dos arquivos da `final/`; segue a correção.

# Cenário 05

## Visão geral

O agente trocou o `ReservationReceipt` único da base por três documentos. A escolha de qual criar fica num método polimórfico do tipo de identificação do hóspede: `Identification.documentLayout()` é sobrescrito em `CpfIdentification`, `CompanyIdentification` e `PassportIdentification`. Os três lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) chamam um único método privado, `ReservationService.documentOf`, e nenhum deles pergunta qual é o tipo do hóspede. O comprovante da pessoa com CPF também passou pela estrutura (`ReceiptLayout`), e não existe mais nenhum `new` do documento espalhado pelo código. O texto de cada documento é montado por uma classe abstrata com um roteiro fixo (`DocumentLayout`), e cada tipo preenche só as suas partes. Não encontrei problema sério. O ponto mais discutível é que o pacote de domínio (`reservations`) passou a depender do pacote `documents`, porque cada identificação conhece a classe do seu layout. Isso é uma questão de acoplamento e não muda a classe que dei.

## Como foi implementado, e por que o julgamento

**O projeto.** O `Hotel` (`final/src/main/java/hotel/Hotel.java:19-27`) monta tudo com `new`: `Rooms`, `ReservationStore` (os dados ficam em memória), `PriceCalculator`, `EmailSender` com `SentMessages`, o `ReservationService` e o `HttpApi` (Javalin), além dos relatórios em `reports`. Uma requisição entra por uma rota do `HttpApi`. No `POST /reservations`, o corpo vira `CreateReservationRequest`, é validado e convertido em `NewReservation` com um `Guest` (`web/HttpApi.java:117-133`), e então segue para o `ReservationService`, que calcula o preço, guarda a reserva e muda os estados. As rotas de check-out e de segunda via devolvem o documento direto como JSON (`web/HttpApi.java:81`, `web/HttpApi.java:83`).

Na mudança, o `Guest` deixou de ter `cpf` e passou a ter uma `Identification` (`reservations/Guest.java:3`), que é uma interface com três records. O `HttpApi.readIdentification` (`web/HttpApi.java:136-153`) decide qual dos três records criar a partir do corpo da requisição. O `ReservationResponse` mostra os cinco campos de identificação, e os que não se aplicam vêm null (`web/ReservationResponse.java:37-41`). Nos testes, o agente criou o `DocumentLayoutTest`, com o texto exato dos três documentos, a linha de desconto com "1 night" e o e-mail com o *booking receipt*. No `HttpApiTest`, acrescentou um teste da regra "exatamente um jeito" (aceita a empresa e o passaporte; recusa nenhum, um incompleto e dois ao mesmo tempo). Os testes antigos foram ajustados para usar `new CpfIdentification(...)`.

**A parte do pattern.**
- **Onde se escolhe o documento.** A escolha mora no tipo do hóspede. `Identification` declara `DocumentLayout documentLayout()` (`reservations/Identification.java:11`). Cada record devolve o seu layout:
  - `CpfIdentification` → `ReceiptLayout` (`reservations/CpfIdentification.java:10-12`);
  - `CompanyIdentification` → `InvoiceLayout` (`reservations/CompanyIdentification.java:10-12`);
  - `PassportIdentification` → `BookingReceiptLayout` (`reservations/PassportIdentification.java:10-12`).
- **Quem cria o documento.** O `DocumentLayout.issue` (`documents/DocumentLayout.java:16-37`) é `final` e cria o `ReservationDocument`, com os campos de identificação e o `text`. O texto segue um roteiro fixo (`documents/DocumentLayout.java:39-53`) com passos abstratos: título, cabeçalho, identificação, datas, rótulos, formato do dinheiro, e um `closing` que por padrão é vazio.
- **Os três documentos.**
  - `PortugueseLayout` (`documents/PortugueseLayout.java:7`) reúne o que o comprovante e a fatura têm em comum: "nº", datas `dd/MM/yyyy`, "diária(s)" e `R$`.
  - `ReceiptLayout` só define o título e a linha com o CPF (`documents/ReceiptLayout.java:9-16`).
  - `InvoiceLayout` define a empresa e o hóspede, e acrescenta o ISS no `closing` (`documents/InvoiceLayout.java:18-27`).
  - `BookingReceiptLayout` define tudo em inglês (`documents/BookingReceiptLayout.java:11-56`).
- **Os três lugares.** Todos passam pelo mesmo método:
  - `confirm` em `reservations/ReservationService.java:68`;
  - `checkOut` em `reservations/ReservationService.java:110`;
  - `receiptCopy` em `reservations/ReservationService.java:119`;
  - e os três chamam `documentOf` (`reservations/ReservationService.java:122-124`), que faz só `identification().documentLayout().issue(...)`.

**O raciocínio.** Essa é a forma "método polimórfico no próprio tipo de hóspede" que as instruções aceitam como acerto. A decisão de qual documento criar está num lugar só: cada tipo de identificação responde pelo seu documento, e não há `if` ou `switch` sobre o tipo nos três lugares. Os três pedem o documento ao mesmo ponto. Os casos que já existiam também foram para a estrutura: o comprovante da base virou `ReceiptLayout`, e o `ReservationReceipt` com construtor próprio foi apagado (`changes.patch:317-377`). Para a agência de turismo, basta um novo record de `Identification` e um novo layout, sem mexer no serviço.

O único `if` sobre o tipo está em `readIdentification` (`web/HttpApi.java:140-152`). Esse é o ponto em que a entrada vira o tipo concreto, o que é inevitável, e não é uma escolha de documento repetida. As identificações têm métodos padrão que devolvem null (`reservations/Identification.java:13-31`), e o `DocumentLayout.issue` os lê para preencher o JSON (`documents/DocumentLayout.java:23-27`). Isso serve aos campos "que não se aplicam vêm null" do pedido e não decide o tipo.

No exagero colateral, pesei três estruturas, e as três se justificam:
- o roteiro do `DocumentLayout`, que é um Template Method: há três documentos e mais um anunciado;
- o `PortugueseLayout`, que é compartilhado de fato por dois documentos;
- a interface `Identification`, que tem três implementações.

No comportamento, conferi as regras por leitura (abaixo) e todas conferem.

## Item a item

**O pattern pedido.** O agente usou um método polimórfico no tipo de hóspede, `Identification.documentLayout()` (`reservations/Identification.java:11`), sobrescrito nas três identificações (`reservations/CpfIdentification.java:10-12`, `reservations/CompanyIdentification.java:10-12`, `reservations/PassportIdentification.java:10-12`). O documento é criado por `DocumentLayout.issue` (`documents/DocumentLayout.java:16-37`). O e-mail de confirmação, o check-out e a segunda via passam todos por `documentOf` (`reservations/ReservationService.java:68`, `:110`, `:119`, `:122-124`). Nenhum deles cria o documento à mão nem pergunta o tipo. O comprovante antigo da pessoa com CPF também foi para a estrutura (`documents/ReceiptLayout.java`), e o `ReservationReceipt` foi apagado. A escolha mora num lugar só, e um tipo novo (a agência) entra sem mexer nos três lugares. Isso é o pattern que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. A interface `Identification` tem três implementações. O `PortugueseLayout` é compartilhado por dois layouts (`documents/InvoiceLayout.java:8`, `documents/ReceiptLayout.java:6`). O roteiro abstrato do `DocumentLayout` atende três documentos e o quarto anunciado. O `PriceCalculator.cents` passou a ser público (`changes.patch:490-491`) para ser reaproveitado no cálculo do ISS, e isso não acrescenta estrutura. Também não há fábrica avulsa, builder nem camada que só repasse.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço, sem rodar o Maven e sem chamar a API:
- **Exatamente um jeito de identificar.** `readIdentification` (`web/HttpApi.java:136-153`) aceita só o CPF, ou o CNPJ com a razão social, ou o passaporte com o país. Recusa com `INVALID_REQUEST` quando não vem nenhum, quando vêm dois ou quando um vem incompleto: basta um campo de empresa ou de passaporte presente para contar como aquele jeito, e então os dois campos são exigidos. Os demais campos continuam obrigatórios (`web/HttpApi.java:120-128`).
- **A fatura.** Mostra "Empresa: … (CNPJ …)" e "Hóspede: …" no lugar do CPF (`documents/InvoiceLayout.java:18-22`). O ISS é 5% arredondado com `cents` (`documents/InvoiceLayout.java:26`): 779,80 × 0,05 = 38,99, que sai como `R$ 38,99`.
- **O *booking receipt*.** Sai em inglês, com `BRL` e `Locale.US` "%,.2f" (`documents/BookingReceiptLayout.java:55`), o que dá `BRL 1,234.56`. As datas usam `LocalDate.toString()` (`documents/BookingReceiptLayout.java:34`), que dá `2027-03-10`. Sai "1 night" ou "N nights" (`documents/BookingReceiptLayout.java:35`), e "Discount:" antes do total quando há desconto (`documents/DocumentLayout.java:47-49`).
- **O comprovante de quem tem CPF.** O texto é o mesmo da base: título "Comprovante de reserva nº", "Hóspede: X (CPF Y)", as mesmas linhas e nenhum `closing` (`documents/ReceiptLayout.java`, `documents/PortugueseLayout.java`, comparados com o `ReservationReceipt.text()` apagado em `changes.patch:340-354`).
- **O mesmo documento nos três lugares.** Os três saem do mesmo `documentOf`. O e-mail continua em português e só troca o documento que vai dentro dele (`reservations/ReservationService.java:130-135`).
- **Os campos do JSON.** O documento tem os campos de antes, os quatro novos e o `text` (`documents/ReservationDocument.java:12-29`). A reserva mostra os campos de identificação, com null nos que não se aplicam (`web/ReservationResponse.java:37-41`).

Não vi nada que quebre o que já existia.
Conclusão: segue

## Dúvidas

- **A forma usada.** É o método polimórfico no tipo de hóspede (`guest.identification().documentLayout()`), que devolve um objeto criador (`DocumentLayout`), e é esse objeto que cria o `ReservationDocument` em `issue`. Por isso fica próximo também da forma clássica: um criador com subclasses, embora o que as subclasses sobrescrevem sejam os passos do texto, e não o método que cria o documento.
- **O acoplamento.** O pacote `reservations` passou a importar `hotel.documents`, já que as identificações conhecem as classes dos layouts. É uma escolha de acoplamento discutível, mas não afeta onde a decisão mora, e não a contei como exagero nem como malfeito.
