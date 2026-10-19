# Cenário 01

## Visão geral

O agente juntou a escolha do documento num lugar só. O método estático `DocumentGenerator.generateDocument` olha o tipo da identificação do hóspede (CPF, CNPJ ou passaporte) e monta o comprovante, a fatura ou o *booking receipt*. Os três lugares que emitem documento (e-mail de confirmação, check-out e segunda via) chamam esse método e não decidem nada por conta própria. É uma fábrica concentrada, que as instruções do pattern aceitam como acerto. O lado fraco do desenho é o objeto `Document`: ele tem um campo próprio para cada tipo de identificação e um campo `issRetained` que só a fatura usa, e os DTOs de saída repetem a pergunta "qual é o tipo?". Isso fica fora da criação do documento, mas encarece a chegada do documento das agências. Não há estrutura sobrando no resto do sistema. O problema mais sério é de comportamento. A confirmação exige um corpo com `amountPaid`: sem corpo ela devolve 500, e com corpo aceita qualquer valor como pago. Os valores acima de mil saem sem separador de milhar (`R$ 1739,70`, `BRL 1739.70`). Os campos que deveriam vir `null` somem do JSON. E há uma brecha na regra de "um jeito só" de identificar o hóspede.

## Como foi implementado, e por que o julgamento

**O projeto.** É um único módulo Maven com Javalin 6.7.0 (`pom.xml:20-22`). O `maven-assembly-plugin` gera o jar com as dependências, `target/marea-alta.jar`, com `Main-Class: com.pousada.Application` (`pom.xml:59-82`). Os pacotes são estes:
- `com.pousada`: `Application.java` lê `PORT`, usa 7070 se ela não vier, sobe o Javalin e registra as rotas.
- `api`: `ApiHandler.java` tem todas as rotas e também valida a entrada e traduz os erros. Em `api/dto` ficam as classes de entrada e saída do JSON.
- `domain`: `Reservation` guarda a máquina de estados e o reembolso, e há também `Room` e `Message`. A identificação é a interface selada `GuestIdentification` com três implementações de dados: `CpfIdentification`, `CnpjIdentification` e `PassportIdentification`.
- `service`: `PricingService` faz o cálculo com métodos estáticos e meio-para-o-par. `ReservationService` cuida das operações e do e-mail. `ReportService` monta os CSVs. `DocumentGenerator` cria os documentos.
- `repository`: `InMemoryRepository` é um singleton em memória que guarda quartos, reservas, mensagens e os contadores de id (`repository/InMemoryRepository.java:10-23`).

Uma requisição segue este caminho: rota em `ApiHandler` (`api/ApiHandler.java:31-43`), depois `ReservationService`, que busca no repositório, chama `Reservation.confirm/checkIn/...` e `DocumentGenerator`, e por fim a resposta é mapeada por `ReservationResponse` ou `DocumentResponse`. O agente não escreveu testes automatizados: não existe `src/test`. Só deixou três scripts `curl` na raiz (`test-api.sh`, `test-api-full.sh`, `test-comprehensive.sh`), e o `mvn verify` passa sem rodar teste nenhum.

**A parte do fim (o documento por tipo de hóspede).**
- **Identificação do hóspede.** `ApiHandler.parseGuestIdentification` conta quantas identificações completas vieram e cria a implementação certa de `GuestIdentification` (`api/ApiHandler.java:287-308`). A reserva guarda só essa interface (`domain/Reservation.java:20`).
- **A fábrica.** `DocumentGenerator.generateDocument` (`service/DocumentGenerator.java:13-25`) faz a escolha com uma cadeia de `instanceof`: CPF vai para `generateComprovante` (`:27-57`), CNPJ vai para `generateFatura` (`:59-95`, onde mora o ISS de 5% com HALF_EVEN, `:62-63`), e passaporte vai para `generateBookingReceipt` (`:97-128`, com datas ISO e `night/nights`). Cada ramo devolve um `Document` com o título, os dados e o `text` já montados.
- **Os três lugares que usam a fábrica:**
  - a confirmação, `service/ReservationService.java:61`, cujo `document.getText()` entra no e-mail em `:132-141`;
  - o check-out, `service/ReservationService.java:96`;
  - a segunda via, `service/ReservationService.java:129`.

  Nenhum deles cria documento ou pergunta pelo tipo.
- **Fora da criação.** `Document` tem três campos de identificação com tipo próprio e um `issRetained` (`domain/Document.java:9-11,21`). `DocumentResponse.fromDocument` vê qual desses campos não é nulo (`api/dto/DocumentResponse.java:76-86`). `ReservationResponse.fromReservation` repete a cadeia de `instanceof` para preencher os campos da reserva (`api/dto/ReservationResponse.java:93-102`).

**O raciocínio.**
- **O que leva ao acerto.** A decisão de qual documento criar está num ponto só (`DocumentGenerator.java:16-22`), e os três lugares pedem o documento a esse ponto. É a "fábrica concentrada" que as instruções aceitam como acerto. Não é omissão, porque não há `if` sobre o tipo repetido nos três lugares. Também não é malfeito: nenhum dos três monta o documento à mão, e a fábrica não devolve um documento genérico que depois é especializado fora dela.
- **O que afasta da forma ideal.** Os dois `instanceof` dos DTOs (`ReservationResponse.java:93-102`, `DocumentResponse.java:76-86`) e o `Document` com um campo por tipo mostram que o tipo de hóspede não sabe se expor sozinho. Para o documento das agências, além de um ramo novo na fábrica, será preciso mexer em `Document` e nos dois DTOs. Mas isso é a serialização dos campos de identificação, não a escolha do documento, por isso não muda a classe.
- **Exagero colateral.** Não vi interface com uma implementação só, nem fábrica, builder ou camada que só repassa fora da parte do fim. O singleton do repositório e os serviços estáticos são simples, não excessivos. As anotações Jackson de polimorfismo em `GuestIdentification` (`domain/GuestIdentification.java:6-11`) não são usadas, mas ficam dentro da parte do fim.
- **Comportamento.** Os textos dos três documentos, o ISS, o "1 night", as datas e a igualdade do documento nos três lugares conferem. Falham o formato de milhar nos documentos, a confirmação, os `null` no JSON, a brecha na identificação e algumas regras da parte comum (veja o item a item).

## Item a item

**O enunciado (pede o pattern).** A escolha do documento mora em `DocumentGenerator.generateDocument` (`service/DocumentGenerator.java:13-25`). Ali o tipo de `GuestIdentification` decide entre comprovante (`:27-57`), fatura (`:59-95`) e *booking receipt* (`:97-128`). O e-mail de confirmação (`service/ReservationService.java:61-65`), o check-out (`service/ReservationService.java:96`) e a segunda via (`service/ReservationService.java:129`) recebem o documento pronto dessa fábrica e não decidem nada. É o pattern na forma de fábrica concentrada: o caso novo, das agências, entra como um novo `GuestIdentification` e um ramo novo num lugar só. A ressalva, que não muda a classe, são os `instanceof` de serialização em `api/dto/ReservationResponse.java:93-102` e `api/dto/DocumentResponse.java:76-86`, e os campos por tipo em `domain/Document.java:9-11,21`.
Conclusão: acerto

**O exagero colateral.** Não há. O repositório é uma classe concreta só (`repository/InMemoryRepository.java:9`), os serviços são classes diretas com métodos estáticos (`service/PricingService.java`), e não existe interface de uma implementação, fábrica, builder ou camada que só repassa fora da parte do fim.
Conclusão: contenção

**O comportamento.** Montei a cópia em `/tmp/copia`, rodei `mvn -q package -DskipTests`, subi `target/marea-alta.jar` com `PORT=7070` e testei com `curl`/`jq`.

O que confere:
- **Os três documentos.** A reserva 201 com CPF, de 10/03 a 12/03/2027, deu `subtotal 779.80`. O comprovante saiu idêntico ao modelo. Com CNPJ (Tecnosul) saiu a fatura com `Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)`, `Hóspede: Ana Souza` e `ISS retido (5%): R$ 38,99`. Com passaporte saiu `Booking receipt no. 3`, com `Check-in: 2027-03-10`, `2 nights` e `BRL 499.80`.
- **Uma noite e desconto.** Uma noite deu `1 night`. O desconto aparece como `Discount: BRL 260.96` antes do total.
- **O mesmo documento nos três lugares.** O `text` do check-out é igual ao da segunda via e está inteiro dentro do e-mail. O e-mail continua em português, com o assunto certo, e o JSON do documento tem `text`.
- **Recusas de identificação.** Nenhuma identificação, CPF junto com CNPJ completo, só `companyCnpj` e só `guestPassport` dão 400 `INVALID_REQUEST`.
- **Parte comum.** Alta temporada de uma noite deu `299.88`. Capacidade excedida dá 422. Quarto ocupado dá 409. Segunda via de reserva pendente dá 409, e de reserva inexistente, 404. O cancelamento STANDARD devolve o pago, o NON_REFUNDABLE devolve 0 e o pendente devolve 0. A ocupação de 12/03 não conta quem sai nesse dia. Os CSVs vêm com `text/csv; charset=utf-8`.

O que falha:
- **Confirmação.** `POST /reservations/1/confirmation` sem corpo devolve **500** `INTERNAL_ERROR`, e com `{}` devolve 400, porque a rota exige `amountPaid` (`api/ApiHandler.java:128-134`). Com corpo, aceita o valor informado: paguei 1478.75 numa reserva de total 1478.74 e ficou `amountPaid 1478.75`. O enunciado diz que o pago é o total da reserva.
- **Separador de milhar.** Não existe: veio `Diárias: R$ 1739,70` (o certo é `R$ 1.739,70`) e `Nights: BRL 1739.70` (o certo é `BRL 1,739.70`). A formatação usa `String.format("%.2f")`, que depende da localidade (`service/DocumentGenerator.java:152-158`).
- **Campos `null`.** Os campos de identificação que não se aplicam e o `refund` antes do cancelamento **não vêm** como `null`: somem do JSON, por causa do `@JsonInclude(NON_NULL)` (`api/dto/ReservationResponse.java:10`, `api/dto/DocumentResponse.java:8`).
- **Identificação.** `guestCpf` junto com `companyCnpj` sem `companyName` foi **aceito** com 201, e o mesmo acontece com CPF junto com `guestCountry`. A contagem ignora a identificação incompleta quando há outra completa (`api/ApiHandler.java:295-304`).
- **Corpo que não é JSON.** Devolve 500 em vez de 400 (`api/ApiHandler.java:60,98-100`).
- **Ocupação.** Conta reserva PENDING: deu `total,1/5` com uma reserva só pendente (`repository/InMemoryRepository.java:93`).
- **Financeiro.** Lista o STANDARD cancelado com `0.00`, uma reserva que não deixou dinheiro (`service/ReportService.java:53-56`).
- **Menores.** O `discount` sai como `0` e não `0.00` (`service/PricingService.java:49`). As datas do JSON do comprovante e da fatura saem em `10/03/2027` (`service/DocumentGenerator.java:28-29,52-54`). `month=2027-7` é aceito.

**Os testes do agente.** O `mvn -q verify` da cópia terminou com saída 0, mas com **zero testes**: não há `src/test` nem relatório do surefire. No fim derrubei o serviço e confirmei que a porta 7070 ficou livre.
Conclusão: em parte

## Dúvidas

A forma usada foi a fábrica concentrada: um método estático com uma cadeia de `instanceof` sobre a interface selada `GuestIdentification` (`service/DocumentGenerator.java:13-25`). Não é o Factory Method clássico com subclasses criadoras, e não é um método polimórfico no tipo de hóspede. Pesei se os `instanceof` de serialização nos DTOs (`api/dto/ReservationResponse.java:93-102`, `api/dto/DocumentResponse.java:76-86`) e o `Document` com um campo por tipo (`domain/Document.java:9-11,21`) puxariam a classe para malfeito. Concluí que não: eles não decidem qual documento criar, e o critério de malfeito é um dos três lugares montar o documento à mão ou a escolha acontecer depois da fábrica. Mesmo assim, o documento das agências vai exigir mudanças nesses três pontos além da fábrica.
