# Cenário 05

## Visão geral

O agente concentrou num lugar só a escolha de qual documento criar. O método `DocumentGenerator.generate` olha o tipo de identificação do hóspede (CPF, CNPJ ou passaporte) e monta o comprovante, a fatura ou o *booking receipt*. Os três lugares onde o documento sai (o e-mail de confirmação, o check-out e a segunda via) pedem o documento a esse método, sem decidir nada por conta própria. Isso é o que o cenário pede, na forma de fábrica concentrada. Também não há estrutura sobrando. O ponto fraco está no comportamento, conferido por leitura. O *booking receipt* sai sem separador de milhar (`BRL 1234.56`), e o formato ainda depende do locale da máquina. A confirmação exige um `amountPaid` no corpo da requisição, quando o pago deveria ser o total da reserva. Um identificador incompleto passa se vier junto com um completo. Dezembro inteiro é cobrado como alta temporada, quando ela só começa no dia 15. E menos de um hóspede dá 422 em vez de 400.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `model` tem os objetos de dados:
- `Room`;
- `Reservation`, com os enums `Status` e `RatePlan` e o campo `identification`;
- `Message`;
- `Document`, uma classe só de dados, com todos os campos de identificação mais `text` e `issRetained`;
- `GuestIdentification`, uma `sealed class` com três subclasses aninhadas: `CPF`, `CNPJ` (com `companyName`) e `Passport` (com `country`).

O `repository.Repository` guarda tudo na memória: os quartos ficam num `HashMap` populado no construtor, junto com as reservas e a lista de mensagens. O pacote `service` tem:
- `PricingService`: diárias, alta temporada, desconto, total e ISS;
- `ReservationService`: criar, confirmar, check-in, check-out e cancelar, e também o envio do e-mail;
- `ReportService`: os dois CSVs;
- `DocumentGenerator`: os documentos.

O `Main` monta as dependências à mão (`Main.java:24-28`), sobe o Javalin na porta de `PORT` ou na 7070 e declara as rotas em lambdas. Cada rota lê o corpo como `Map`, chama o serviço e serializa a resposta à mão (`serializeReservation`, `serializeDocument`). Os erros viram JSON em `handleReservationException` (`Main.java:351-362`). O jar sai pelo `maven-shade-plugin`. Os testes do agente são unitários: os de `PricingServiceTest` cobrem as tarifas e o ISS, e os de `ReservationServiceTest` cobrem criação com os três tipos, estados, cancelamento, conflito de quarto e a mensagem enviada. Não há teste HTTP nem teste do texto dos documentos.

**A parte que pede o pattern.** Cada tipo de hóspede é uma subclasse de `GuestIdentification` (`model/GuestIdentification.java:3-30`). O tipo é escolhido uma vez, na entrada, em `Main.parseIdentification` (`Main.java:259-284`). A escolha do documento mora em `DocumentGenerator.generate` (`service/DocumentGenerator.java:21-54`). Uma cadeia de `instanceof` decide o título, o texto e os campos de cada caso:
- o CPF gera o comprovante (`:32-35`, texto em `:56-72`);
- o CNPJ gera a fatura e calcula o ISS (`:36-41`, texto em `:74-92`);
- o passaporte gera o *booking receipt* (`:42-46`, texto em `:94-111`).

Os três consumidores só chamam `generate`:
- o e-mail de confirmação, em `service/ReservationService.java:72`, que encaixa `document.text` no corpo em português (`:73-75`);
- o check-out, em `Main.java:147`;
- a segunda via, em `Main.java:176`.

Nenhum dos três pergunta qual é o tipo de hóspede.

**O raciocínio.** As instruções aceitam como acerto "uma classe ou um método que escolhe o documento pelo tipo de hóspede", e é isso que existe. A decisão de qual documento criar está num ponto só, e os três lugares passam por ele. Para a agência de turismo, bastaria uma nova subclasse de identificação e um novo ramo em `generate`, sem mexer no e-mail, no check-out nem na segunda via. O que afasta o desenho da forma clássica: `Document` não é polimórfico, é uma classe só com todos os campos, e o `if/else` não é exaustivo, porque se nenhum ramo casa, sai um documento vazio sem erro de compilação. Nada disso, porém, faz um dos três lugares criar o documento à mão, nem leva a escolha para fora da fábrica. Existe um segundo `instanceof` sobre o tipo em `serializeReservation` (`Main.java:293-311`), mas ele serializa a reserva e não cria documento, então fica fora do que o pattern resolve. Quanto ao exagero, não há interfaces de implementação única, fábricas extras nem camadas que só repassam. O que pesou no comportamento está no item abaixo.

## Item a item

**O pattern pedido.** A escolha do documento por tipo de hóspede está concentrada em `DocumentGenerator.generate` (`service/DocumentGenerator.java:32-47`). O e-mail de confirmação (`service/ReservationService.java:72`), o check-out (`Main.java:147`) e a segunda via (`Main.java:176`) pedem o documento a esse método e usam o resultado sem verificar o tipo. É a fábrica concentrada que as instruções aceitam como acerto: um lugar decide, e todos os casos (inclusive o comprovante com CPF, que já existia) passam por ele.
Conclusão: acerto

**O exagero colateral.** Não há. A montagem é manual e direta (`Main.java:24-28`). `PricingService`, `ReportService` e `Repository` são classes concretas com trabalho real, e não existe interface, fábrica ou builder sem necessidade. O campo `issRetained` de `Document` (`model/Document.java:24`) não é serializado, mas isso é sobra pequena, não estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço.

O que confere:
- O comprovante com CPF segue o modelo do enunciado (`service/DocumentGenerator.java:62-71`), com "1 diária" ou "N diárias" e a linha de desconto.
- A fatura mostra a empresa no lugar do CPF e o ISS com `HALF_EVEN` (`service/PricingService.java:40-42`): 779,80 × 0,05 dá R$ 38,99.
- O documento é o mesmo nos três lugares.
- O JSON do documento tem os cinco campos de identificação, com null nos que não se aplicam, e o campo `text` (`Main.java:328-349`).
- O e-mail sai em português, com o assunto pedido.

O que não confere:
- O *booking receipt* usa `String.format("%.2f", ...)` (`service/DocumentGenerator.java:120-122`). Sai `BRL 1234.56`, sem a vírgula de milhar pedida, e com vírgula decimal se o locale da máquina for pt-BR. As datas `2027-03-10` e o "1 night"/"N nights" estão certos.
- Um incompleto acompanhado de um completo passa. Por exemplo, `guestCpf` com `companyCnpj` sem `companyName` conta como um jeito só e é aceito (`Main.java:266-271`). O enunciado manda recusar.
- A confirmação lê `amountPaid` do corpo (`Main.java:103-104`). Sem esse campo, dá 400. Com ele, grava o valor do cliente em vez do total da reserva (`service/ReservationService.java:68`).
- A alta temporada pega dezembro inteiro, com `month >= 12` (`service/PricingService.java:48`), em vez de começar em 15/12.
- Menos de um hóspede dá 422 CAPACITY_EXCEEDED (`service/ReservationService.java:32-33`), e o pedido é 400 INVALID_REQUEST.
- O financeiro inclui as canceladas que não deixaram dinheiro, com valor 0.00 (`service/ReportService.java:56-62`), e o CSV também depende do locale.
- A segunda via de reserva não paga responde 404 RESERVATION_NOT_FOUND (`Main.java:167-172`).
- `/rooms` sai na ordem do `HashMap`.
Conclusão: em parte

## Dúvidas

- A forma usada foi a de fábrica concentrada: um método com uma cadeia de `instanceof` sobre `GuestIdentification`. Não é a forma clássica com criador sobrescrito, nem um método polimórfico no tipo de hóspede. O `Document` devolvido é uma classe única, não uma hierarquia de documentos. Isso ainda cabe no acerto, porque quem decide qual documento criar é a fábrica, e não um código posterior.
- O `if/else` em `generate` não é exaustivo: um tipo novo sem ramo produziria um documento vazio sem aviso. O `instanceof` repetido em `serializeReservation` (`Main.java:293-311`) também teria de crescer com a agência. Pesei os dois como fora do escopo da criação do documento, mas um leitor mais estrito poderia ver aí um traço de malfeito.
