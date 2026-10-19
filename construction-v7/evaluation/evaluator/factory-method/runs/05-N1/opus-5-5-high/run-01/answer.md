# Cenário 01

## Visão geral

O agente concentrou a escolha do documento num lugar só. O `ReservationService` guarda um mapa que liga a classe de identificação do hóspede ao gerador de documento, com um gerador para cada tipo (CPF, CNPJ e passaporte). Os três lugares que emitem o documento (e-mail de confirmação, check-out e segunda via) chamam o mesmo método privado, que consulta esse mapa. É uma fábrica concentrada, e os três tipos, inclusive o CPF da parte comum, passam por ela. Não há estrutura sobrando fora dessa parte. O problema mais sério está no comportamento. O comprovante e a fatura escrevem "R$" seguido de um espaço não separável (U+00A0), e não do espaço comum do enunciado. Na parte comum, um corpo sem `ratePlan` devolve 500 e mesmo assim grava uma reserva quebrada, que bloqueia o quarto. Um corpo sem `checkIn` também devolve 500, e não 400.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `com.pousadamarealta` e se divide em quatro pacotes:
- `api`: os controllers, os DTOs (records) e o `ExceptionHandler`, que traduz as exceções nos códigos de erro.
- `service`: o `ReservationService`, o `ReportService`, as exceções e os geradores de documento.
- `domain`: `Reservation`, `Room`, as enums `RoomType`, `RatePlan` e `ReservationStatus`, `Message`, `Document` e as três identificações.
- `repository`: três repositórios concretos em memória, sobre `LinkedHashMap`, com ids em `AtomicLong`.

A montagem é manual, em `Application.main` (`src/main/java/com/pousadamarealta/Application.java:24-61`). Ele cria os repositórios, os serviços e os controllers, configura o Jackson e registra as rotas. A porta vem de `PORT`, com 7070 como padrão (`Application.java:67-77`). O shade plugin gera um jar executável.

O caminho de uma requisição: a rota chama o `ReservationController`. O controller valida o corpo e monta a identificação (`api/controller/ReservationController.java:90-130`), e o `ReservationService` aplica as regras de estado, capacidade e conflito. O preço é calculado pela própria `Reservation` (`domain/Reservation.java:91-113`) e não fica gravado: é recalculado a cada leitura.

Os testes do agente são 9, e os 9 passam no `mvn verify`:
- 4 de domínio, sobre alta temporada, desconto e reembolso;
- 5 de API, sobre quartos, criação e confirmação, identificação ausente, quarto inexistente e capacidade.

Nenhum teste cobre a fatura, o *booking receipt*, o check-out, a segunda via ou os relatórios. O teste de API copia a montagem das rotas em vez de reaproveitar a de `Application`.

**A parte do fim do enunciado.**
- **Identificação.** É a interface `GuestIdentification` (`domain/GuestIdentification.java:3-5`), com três classes de dados: `CpfIdentification`, `CnpjIdentification` e `PassportIdentification`. O controller escolhe a classe a partir dos campos do pedido (`ReservationController.java:119-130`), e a `Reservation` guarda a identificação (`domain/Reservation.java:13`).
- **Geradores.** São a interface `DocumentGenerator` (`service/DocumentGenerator.java:9-11`) e três implementações. Cada uma monta o texto e o `Document` com os cinco campos de identificação, deixando null os que não se aplicam:
  - `CpfDocumentGenerator`: o comprovante (`service/CpfDocumentGenerator.java:15-54`);
  - `CnpjDocumentGenerator`: a fatura, com o ISS de 5% em HALF_EVEN (`service/CnpjDocumentGenerator.java:18`, `:37`);
  - `PassportDocumentGenerator`: o recibo em inglês, com `BRL #,##0.00` e datas ISO (`service/PassportDocumentGenerator.java:12-20`, `:35`).
- **Escolha.** O `ReservationService` registra o mapa `Class → DocumentGenerator` no construtor (`service/ReservationService.java:17`, `:25-28`). O método `generateDocument` faz a busca (`ReservationService.java:138-142`), e os três lugares passam por ele: confirmação (`:74`), check-out (`:119`) e segunda via (`:135`). O e-mail é montado uma vez só, em português, e coloca `document.getText()` no meio (`:144-155`).

**O julgamento.** O que conta para o Factory Method está aqui. A decisão de qual documento criar mora num lugar só, e nenhum dos três pontos de emissão pergunta pelo tipo nem monta documento à mão. O CPF passa pela mesma estrutura dos casos novos. Para a agência de turismo, basta uma classe de identificação nova, um gerador novo e uma linha no mapa.

Três coisas afastam o desenho do ideal, sem mudar a classe:
- Os geradores fazem um downcast da identificação (`CnpjDocumentGenerator.java:17`). É seguro porque o mapa é indexado pela classe, mas o tipo não garante isso.
- Fora da criação do documento, o `ReservationResponse` mantém sua própria cadeia de `instanceof` para preencher os campos da reserva (`api/dto/ReservationResponse.java:37-41`). Esse é um segundo ponto que precisa conhecer cada tipo de hóspede.
- O método `formatForDocument` da interface não é chamado em lugar nenhum: é código morto.

Nada disso decide qual documento criar, então a classe fica em acerto.

No exagero colateral, os repositórios e serviços são classes concretas, sem interfaces ou fábricas sobrando. O único ponto discutível são DTOs que copiam campo a campo os objetos de domínio. No comportamento, as regras do fim conferem, menos o espaço depois de "R$" nos documentos em português. A parte comum tem falhas de validação: 500 com reserva persistida, campo ausente que vira 404 ou é aceito, e `amountPaid` como `0`.

## Item a item

**O enunciado (pede o pattern).** A escolha do documento é feita por uma fábrica concentrada: o mapa `Class<? extends GuestIdentification> → DocumentGenerator` (`src/main/java/com/pousadamarealta/service/ReservationService.java:17`, `:25-28`), consultado num único método (`ReservationService.java:138-142`). Os três lugares pedem o documento a esse método e não decidem nada: confirmação e e-mail (`ReservationService.java:74-75`), check-out (`:119`) e segunda via (`:135`). Cada documento tem sua classe: `service/CpfDocumentGenerator.java:10`, `service/CnpjDocumentGenerator.java:11` e `service/PassportDocumentGenerator.java:11`. O comprovante da pessoa com CPF passa pela mesma estrutura. Nenhum ponto de emissão tem `if` ou `switch` sobre o tipo de hóspede. É o que o enunciado pede.
Conclusão: acerto

**O exagero colateral.** Não há:
- os repositórios são classes concretas sem interface (`repository/ReservationRepository.java:10`, `repository/RoomRepository.java:8`, `repository/MessageRepository.java:8`);
- os serviços também (`service/ReservationService.java:13`, `service/ReportService.java:15`);
- não há fábricas, builders ou camadas que só repassam chamadas;
- as interfaces `DocumentGenerator` e `GuestIdentification` pertencem à variação pedida.

Os DTOs que copiam campo a campo objetos de domínio quase idênticos (`api/dto/DocumentResponse.java:29-49` e `domain/Document.java:6-49`, `api/dto/RoomResponse.java:15-22`) são uma separação comum entre API e domínio, não uma estrutura de desenho sem uso.
Conclusão: contenção

**O comportamento.** Testei o jar da cópia na porta 7070, com `curl` e `jq`.

O que confere:
- **Fatura.** A empresa aparece no lugar do CPF e a linha fica `ISS retido (5%): R$ 38,99` sobre 779,80. Com desconto, 795,40 dá `R$ 39,77`.
- **Booking receipt.** Sai em inglês, com `BRL 2,087.64`, `Discount: BRL 313.15`, datas `2027-03-10`, "1 night" e "3 nights".
- **Comprovante da pessoa com CPF.** Sai com "1 diária" e a linha de desconto.
- **Mesmo documento nos três lugares.** O texto da segunda via e o do check-out são iguais ao trecho do e-mail, que é em português para os três tipos.
- **Campos do JSON.** O documento traz `text` e os cinco campos, com null nos que não se aplicam. O mesmo vale para a reserva.
- **Recusas da identificação.** Nenhum jeito, dois jeitos, CNPJ sem razão social, razão social sem CNPJ, passaporte sem país e país sem passaporte dão 400 `INVALID_REQUEST`.
- **Parte comum.** Os preços conferem: 299,88 por noite em julho, 2087,64 − 313,15 = 1774,49. Também conferem:
  - os códigos 404, 409 e 422;
  - a segunda via de reserva pendente (409);
  - os reembolsos;
  - os dois CSVs: ordem, `total,2/5`, a não reembolsável cancelada contando no financeiro e `text/csv; charset=utf-8`.
- **Testes do agente.** O `mvn verify` passa, com 9 testes e nenhuma falha.

O que diverge:
- **Espaço depois de "R$".** No comprovante e na fatura, o dinheiro sai como `R$` + U+00A0 + `779,80` (verificado com `od -c`). A causa é o `NumberFormat.getCurrencyInstance(pt-BR)` (`service/CpfDocumentGenerator.java:12`, `service/CnpjDocumentGenerator.java:13`). Visualmente é igual ao enunciado, mas o texto difere.
- **CPF vazio.** `guestCpf: ""` é aceito e gera "(CPF )".
- **Corpo sem `ratePlan`.** Devolve 500, mas a reserva fica gravada com plano null (`service/ReservationService.java:50-56`). Ela bloqueia o quarto, e o `GET` dela também dá 500.
- **Corpo sem `checkIn`.** Devolve 500 (`api/controller/ReservationController.java:91`).
- **Corpo sem `roomNumber`.** Devolve 404 `ROOM_NOT_FOUND` em vez de 400.
- **Corpo sem `guestName`.** É aceito.
- **Valores com menos de 2 casas.** `amountPaid` sai como `0` antes da confirmação e `refund` como `0`.

Conclusão: em parte

## Dúvidas

- **Forma usada.** É uma fábrica concentrada no formato de registro (mapa de classe para gerador) dentro do próprio `ReservationService`, não numa classe de fábrica separada. Não é o Factory Method clássico com criador sobrescrito, nem um método polimórfico no tipo de hóspede. As instruções aceitam essa forma como acerto.
- **Outros pontos que conhecem cada tipo.** A escolha da classe de identificação a partir do pedido (`api/controller/ReservationController.java:119-130`) e os `instanceof` do `ReservationResponse` (`api/dto/ReservationResponse.java:37-41`) também precisariam mudar para um tipo novo. Pesei que nenhum dos dois decide qual documento criar, então não rebaixei para malfeito. Um avaliador mais rígido poderia ver o segundo ponto como uma decisão por tipo espalhada.
- **O espaço não separável.** Contei a diferença de bytes como desvio de comportamento ("em parte"). Se a comparação for visual, o comprovante e a fatura conferem.
