# Revisão: manutenção, cenário 05

## Visão geral

A base já era organizada por pacotes: `hotel.web` (Javalin, `HttpApi`), `hotel.reservations` (`ReservationService`, `PriceCalculator`, `ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports`, `hotel.rooms`, e a montagem fica em `hotel.Hotel`. Uma requisição entra em `HttpApi`, passa pelo `ReservationService` e chega ao `ReservationStore`, que guarda os dados no PostgreSQL. O esquema é criado pelo Flyway. Para atender o pedido, o agente acrescentou quatro campos anuláveis ao `Guest`, à requisição, à resposta e ao `ReservationReceipt`, além de uma migração nova (`V2`). A validação de "um jeito só" ficou em `HttpApi.readNewReservation`. A escolha do documento ficou dentro do construtor de `ReservationReceipt`, em duas cadeias de `if` sobre campos nulos e em três métodos de texto quase iguais. O roteiro passa inteiro (83 de 83). Testando à mão, achei três problemas que ele não cobre. O ISS é arredondado com `HALF_UP`, e não "meio para o par" como o resto do sistema: com total de R$ 1.169,70, a fatura mostra R$ 58,49 em vez de R$ 58,48. Um CPF em branco volta a ser aceito, coisa que a base recusava. E a reserva aceita campos de dois jeitos misturados (CPF junto com `companyName` ou `guestCountry`). Os testes novos só conferem pedaços do texto, sem valores.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à base (`measures/dependencies-base.md`). A mudança não criou nenhuma dependência nova. `hotel.documents` passou de 2 classes e 79 linhas para 3 classes e 166 linhas. `HttpApi` passou de 170 para 193 linhas.
**Nota:** bom

A mudança respeitou as camadas da base. O SQL novo está todo em `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-53` e `:127-135`). Os campos novos entram pela borda (`src/main/java/hotel/web/CreateReservationRequest.java:6-14`, `ReservationResponse.java:15-18`). O documento continua em `hotel.documents` e não depende de Javalin nem de JDBC (`measures/dependencies.md`, tabela de bibliotecas). A regra "exatamente um jeito de identificar" ficou na borda HTTP (`src/main/java/hotel/web/HttpApi.java:127-141`), junto da validação de campos obrigatórios que a base já fazia ali (`HttpApi.java:116-125`). É uma escolha coerente com a base, mas uma chamada direta ao `ReservationService` não passa por essa regra. O ciclo `documents` ↔ `reservations` veio da base: o `ReservationReceipt` lê `Reservation`, e o `ReservationService` cria o `ReservationReceipt`. O agente não o piorou.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 escolhidos no mesmo lugar (`ReservationReceipt`), e os 3 pontos de uso (`ReservationService.java:64`, `:109`, `:118`) não mudaram. A escolha aparece duas vezes, em duas cadeias de `if` sobre nulos (`getTitle` e `generateText`). Não há um tipo por documento: os três textos são três métodos estáticos copiados (cerca de 50 linhas quase iguais).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do pattern.** A escolha do documento ficou num lugar só. Os três pontos que emitem o documento (o e-mail de confirmação, o check-out e a segunda via, em `src/main/java/hotel/reservations/ReservationService.java:64,109,118`) continuam chamando `new ReservationReceipt(reservation, room)` sem perguntar qual é o tipo do hóspede. Por isso não é omissão: o `if` não se repete nos três lugares.

**O que afasta.** Não há fábrica nem tipos de documento. O construtor do record decide duas vezes, separadamente, qual é o caso: o título em `getTitle` (`src/main/java/hotel/documents/ReservationReceipt.java:50-57`) e o texto em `generateText` (`:59-66`). As duas cadeias de `if` testam `companyCnpj() != null` e depois `guestPassport() != null`, e precisam ficar em sincronia. O tipo do hóspede não existe como tipo: ele é deduzido de qual campo anulável veio preenchido, com uma precedência implícita (o CNPJ ganha do passaporte). O documento da agência, que o pedido já anuncia, exigiria mexer nos campos do `Guest` (`src/main/java/hotel/reservations/Guest.java:3-14`), nas duas cadeias de `if`, nos campos do record e num quarto método copiado. Por isso classifico como malfeito: a escolha está concentrada, mas pergunta o caso duas vezes e não entrega objetos distintos.

**Exagero.** Contenção: nada além do necessário. `TextFormatEnglish` (`src/main/java/hotel/documents/TextFormatEnglish.java`) é uma classe utilitária pequena e justificada. O construtor de compatibilidade de `Guest` (`Guest.java:12-14`) é razoável.

**O resto do código:**
- **Duplicação.** `generateReceiptText`, `generateInvoiceText` e `generateBookingReceiptText` (`ReservationReceipt.java:68-82`, `:84-102`, `:104-118`) repetem as linhas do quarto, das datas, do singular e plural, do desconto e do total. As duas versões em português são idênticas, com exceção das linhas da empresa e do ISS.
- **Arredondamento.** O ISS faz o próprio arredondamento com `RoundingMode.HALF_UP` (`ReservationReceipt.java:98-99`). O sistema arredonda tudo "meio para o par" num lugar só (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`, `cents`), e o pedido diz "arredondado para centavos como sempre". É uma regra de dinheiro duplicada e diferente da que o sistema usa.
- **Validação.** Ela conta campos não nulos, e não campos não vazios (`src/main/java/hotel/web/HttpApi.java:127-129`). Com isso, um `guestCpf: ""` ou um `companyCnpj: " "` passa a valer, e os campos que sobram de outro jeito (`companyName` ou `guestCountry` junto com o CPF) não são recusados.
- **Javadoc removido.** O agente apagou o comentário "Every field is required" de `CreateReservationRequest` sem pôr outro no lugar.

São defeitos localizados no código novo, mas a regra de dinheiro errada pesa.

## Testes

**Números:** 29 testes rodaram, 0 falharam e 0 foram pulados; há 29 métodos de teste (eram 26 na base, todos mantidos e passando). A cobertura de linhas foi de 94,3% (549 de 582), contra 93,8% na base. A de ramos foi de 87,7% (128 de 146), contra 87,1% na base. Em `hotel.documents`: 76 de 78 linhas e 16 de 20 ramos.
**Nota:** com problemas

O agente escreveu três testes novos, todos pela API, em `src/test/java/hotel/web/HttpApiTest.java`:
- `acceptsCompanyCnpjInsteadOfCpf` (`:109`): confere o título "Fatura de hospedagem" e só que o texto contém "ISS retido" (`:127-128`).
- `acceptsPassportInsteadOfCpf` (`:132`): confere o título "Booking receipt" e que o texto contém "BRL" e "2027-03-10" (`:151`).
- `rejectsInvalidIdentificationCombinations` (`:156`): três recusas com 400 INVALID_REQUEST (CPF com CNPJ, CNPJ sem razão social, passaporte sem país).

**A pergunta da mutação.** Nenhum teste confere um valor do documento novo. Se alguém trocasse `0.05` por `0.5`, ou `HALF_UP` por qualquer outro modo, nenhum teste falharia. Por isso o erro de arredondamento do ISS passou despercebido. Nenhum teste falharia também se alguém trocasse o `Locale.US` de `TextFormatEnglish.money` (mudando o separador de milhar), apagasse a linha "Discount:" ou o "1 night", ou tirasse o nome da empresa da fatura.

**O que ficou sem teste:**
- a fatura e o booking receipt no e-mail de confirmação e na segunda via;
- a reserva sem nenhuma identificação;
- o CPF em branco (que a base recusava e agora passa);
- a ausência de um teste de unidade dos textos dos documentos.

Os testes passam e exercitam o caminho novo, mas deixam as regras novas quase sem conferência de valor.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`, nenhuma chave estrangeira sem índice. A restrição `EXCLUDE` contra reserva dupla veio da base. Há 2 migrações: a `V1` ficou igual e a `V2` é nova. O SQL montado com `+` concatena apenas constantes (`COLUMNS`).
**Nota:** com problemas

A evolução foi feita do jeito certo. O agente criou uma migração nova (`src/main/resources/db/migration/V2__add_company_and_passport_fields.sql:1-4`), sem editar a `V1`. Os campos novos são gravados com parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:39-42`) e lidos em `:128-135`.

O defeito está em `V2__add_company_and_passport_fields.sql:6`. Ele tira o `NOT NULL` de `guest_cpf` (na base, `V1__create_tables.sql:24`), mas não põe nenhuma regra no lugar. Antes, o banco garantia que toda reserva tinha identificação. Agora aceita uma reserva sem CPF, sem CNPJ e sem passaporte, ou com CNPJ sem razão social. Uma `CHECK` (por exemplo, `num_nonnulls(guest_cpf, company_cnpj, guest_passport) = 1` e os pares obrigatórios) protegeria a regra que só a borda HTTP confere hoje, e a confere mal (veja o comportamento). As colunas `status` e `rate_plan` em `text`, sem `CHECK`, vieram da base.

## Comportamento

**Números:** 83 de 83 conferências passaram:

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 05 | 32 de 32 |
| reservas simultâneas | 2 de 2 |

**Nota:** com problemas

O roteiro inteiro passa. A fatura de 779,80 dá ISS de R$ 38,99. O booking receipt sai com `BRL 2,783.52`, datas ISO, "1 night" e "Discount:". O documento é o mesmo no e-mail, no check-out e na segunda via, e as recusas pedidas (nenhuma identificação, CPF com CNPJ, CNPJ sem razão social, passaporte sem país) passam.

Com o serviço de pé, chamei a API à mão e achei três defeitos fora do roteiro, em chamadas que o pedido descreve:

- **ISS com o arredondamento errado.** A fatura de uma reserva de 3 diárias no quarto 201 (total R$ 1.169,70) mostrou "ISS retido (5%): R$ 58,49". Arredondado "como sempre", meio para o par como o resto do sistema, o valor é R$ 58,48. Para o dono da pousada, isso significa fatura com imposto retido errado em um centavo sempre que o 5% cai exatamente na metade do centavo. A causa está em `ReservationReceipt.java:99`.
- **Regressão no CPF em branco.** `"guestCpf": ""` sem outra identificação criou a reserva (201), e o comprovante saiu com "CPF " vazio. A base recusava esse caso com 400 (`base/src/main/java/hotel/web/HttpApi.java:120`), e o pedido diz que tudo o que funciona hoje tem de continuar funcionando. Da mesma forma, `"companyCnpj": " "` com razão social virou uma fatura com "(CNPJ  )".
- **Dois jeitos misturados aceitos.** CPF junto com `companyName` e `guestCountry` criou a reserva com 201 e gravou os campos que não se aplicam. A consulta passou a mostrá-los preenchidos, em vez de null como o pedido diz. Passaporte e país junto com `companyName` também foram aceitos. O pedido diz "tem que ser um desses jeitos, e só um".

A parte comum, a persistência e a concorrência estão inteiras. Mas o valor errado do ISS e a regressão no CPF são erros num ponto que o texto fechou.

## Dúvidas

- **A classe do pattern.** Hesitei entre `malfeito` e `acerto`. A referência aceita como acerto "um método que escolhe o documento pelo tipo de hóspede", e a escolha está concentrada no construtor de `ReservationReceipt` (os três pontos de uso não perguntam o tipo). Ficou `malfeito` porque a decisão é tomada duas vezes (`getTitle` e `generateText`), não existe tipo de hóspede nem tipo de documento (só nulos), e cada documento é uma cópia de texto.
- **Os campos de sobra.** O texto não diz explicitamente se `companyName` ou `guestCountry` enviados junto com um CPF devem ser recusados ou ignorados. Contei como defeito porque "só um" jeito foi pedido e porque a consulta passa a mostrá-los preenchidos.
