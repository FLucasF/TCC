# Cenário 05

## Visão geral

O agente deu ao tipo de hóspede uma hierarquia própria: uma interface selada `GuestIdentification` com três records (CPF, CNPJ e passaporte). Cada record gera o texto do seu documento num método polimórfico, `generateDocument`. Os três lugares onde o documento sai (o e-mail de confirmação, o check-out e a segunda via) passam todos por `DocumentService.generateDocument`. Até aí o desenho se aproxima do pattern. O problema é que o documento não nasce inteiro nesse método polimórfico. O título é escolhido num `if/instanceof` em `DocumentService`, e os campos do JSON do documento são preenchidos por outra cadeia de `instanceof` no construtor de `Document`. O código que usa a estrutura continua perguntando qual é o caso concreto. Um quarto tipo (a agência) obrigaria a mexer em três lugares, e num deles o caso novo cairia sem aviso no título "Comprovante de reserva". No comportamento, o mais grave é o JSON do documento: os valores saem como texto no formato brasileiro (`"779,80"`) e as datas como `10/03/2027`. Além disso, a validação aceita uma identificação a mais quando ela vem incompleta.

## Como foi implementado, e por que o julgamento

**O projeto.** É um Javalin montado à mão em `App.java`, sem framework de injeção:

- **Repositórios** (`repository/`): guardam os dados em memória, em `ArrayList` com `AtomicLong` para os ids. Os cinco quartos são criados no construtor de `RoomRepository` (`repository/RoomRepository.java:13-20`).
- **Serviços** (`service/`): `RoomService`, `ReservationService`, `DocumentService`, `MessageService` e `ReportService`.
- **Controladores** (`controller/`): recebem as rotas.
- **DTOs** (`dto/`): fazem a entrada e a saída em JSON.

`App.main` cria tudo, registra as rotas (`App.java:46-59`) e sobe na porta da variável `PORT`, ou na 7070 se ela não vier (`App.java:67-77`).

O caminho de uma requisição é este. A rota chama o controlador, que lê o corpo com `ctx.bodyAsClass`, valida no DTO (`CreateReservationRequest.validate`) e chama `ReservationService`. O serviço confere quarto, capacidade e conflito de noites, e cria o `Reservation`. O próprio `Reservation` calcula as noites, o subtotal com alta temporada, o desconto e o total, e controla as mudanças de estado (`domain/Reservation.java:41-125`).

O agente escreveu quatro testes, em `BasicTest.java`: criação, confirmação, alta temporada e desconto. Nenhum deles toca o documento, a fatura, o *booking receipt* ou a validação da identificação.

**A parte do pattern.** Cada caso é um record que implementa `GuestIdentification`, uma interface selada que só aceita os três tipos (`domain/GuestIdentification.java:12-16`):

- `CpfIdentification` monta o comprovante (`domain/CpfIdentification.java:21-37`);
- `CnpjIdentification` monta a fatura, com o ISS de 5% arredondado meio para o par (`domain/CnpjIdentification.java:22-43`);
- `PassportIdentification` monta o *booking receipt*, com `BRL` e as datas reordenadas (`domain/PassportIdentification.java:22-48`).

Qual record é criado se decide uma vez só, na entrada, em `CreateReservationRequest.toGuestIdentification` (`dto/CreateReservationRequest.java:22-46`). Os três lugares pedem o documento ao mesmo método:

- o e-mail, em `service/MessageService.java:20`;
- o check-out, em `controller/ReservationController.java:106`;
- a segunda via, em `controller/ReservationController.java:129`.

`DocumentService.generateDocument`, por sua vez, faz duas coisas fora do polimorfismo:

- escolhe o título por `instanceof` (`service/DocumentService.java:37-45`), e o `else` cai em "Comprovante de reserva";
- constrói `Document`, cujo construtor separa os cinco campos de identificação com outra cadeia de `instanceof` e termina num `throw` para tipo desconhecido (`domain/Document.java:31-51`).

Só depois disso o construtor chama `identification.generateDocument(data)` (`domain/Document.java:66`). O mesmo `instanceof` aparece ainda em `dto/ReservationResponse.java:40-48`.

**O raciocínio.** O que aproxima o desenho do pattern: o texto de cada documento é criado por um método polimórfico no próprio tipo de hóspede, que é uma das formas que as instruções aceitam. E os três lugares passam por um ponto único. O que afasta: o documento que sai é sempre a mesma classe `Document`, e o que o distingue além do texto (o título e os campos) é decidido fora do polimorfismo, perguntando o tipo concreto. É o caso de "quem usa a estrutura ainda pergunta qual é o caso concreto". Para a agência não basta escrever uma classe nova: é preciso mexer em `DocumentService.getDocumentTitle`, no construtor de `Document` e em `ReservationResponse`. O título é o pior dos três, porque a agência cairia sem erro em "Comprovante de reserva". Por isso a classe é `malfeito`, e não `acerto`.

No exagero, o desenho é contido. Há `Document.DocumentData` e um `DocumentResponse` que copia `Document`, mas os dois carregam dados reais. `RoomService` só repassa para o repositório, mas é uma camada fina e comum nesse tipo de projeto.

No comportamento, as regras de preço e de estado conferem por leitura. Falham, porém, o formato do JSON do documento e a recusa de uma identificação incompleta junto com uma completa.

## Item a item

**O pattern pedido.** O texto do documento é polimórfico, com `generateDocument` em cada record (`domain/CpfIdentification.java:21`, `domain/CnpjIdentification.java:22`, `domain/PassportIdentification.java:22`). Os três lugares usam `DocumentService.generateDocument` (`service/MessageService.java:20`, `controller/ReservationController.java:106`, `controller/ReservationController.java:129`). Mas o mesmo serviço escolhe o título por `instanceof`, com um padrão que serve de saída silenciosa (`service/DocumentService.java:37-45`). E o construtor de `Document` volta a perguntar o tipo concreto para preencher os campos (`domain/Document.java:31-51`). A estrutura do pattern existe, mas convive com a escolha por tipo feita à mão no próprio ponto que deveria concentrá-la. Um tipo novo exige editar essas cadeias, em vez de só acrescentar uma classe.
Conclusão: malfeito

**O exagero colateral.** Não há estrutura relevante sem necessidade. `Document.DocumentData` (`domain/Document.java:141-151`) e `DocumentResponse` (`dto/DocumentResponse.java:7-47`) transportam dados de verdade. `RoomService` (`service/RoomService.java:9-23`) só repassa para o repositório, mas é uma camada fina e convencional, e não pesa no desenho.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código. Não subi o serviço, não rodei o Maven e não chamei a API.

O que confere:

- **Preço e estados**: a alta temporada e o arredondamento meio para o par (`domain/Reservation.java:47-89`) dão 599,76 no exemplo de julho. As transições e o reembolso estão como foram pedidos (`domain/Reservation.java:91-125`).
- **Fatura**: o ISS sobre 779,80 dá R$ 38,99 (`domain/CnpjIdentification.java:38-40`). A empresa aparece no lugar do CPF.
- **Booking receipt**: sai em inglês, com "1 night" ou "N nights", e as datas reordenadas para `2027-03-10` (`domain/PassportIdentification.java:28-38`).
- **Comprovante do CPF**: segue o modelo do ponto de partida.
- **E-mail**: o assunto e o texto em português envolvem o documento (`service/MessageService.java:22-36`).
- **Segunda via**: só existe para reserva paga (`controller/ReservationController.java:123-127`).
- **Recusas que funcionam**: nenhuma identificação, ou duas identificações completas, dão 400 (`dto/CreateReservationRequest.java:41-43`).

O que não confere:

1. **Valores no JSON do documento.** `subtotal`, `discount` e `total` saem como texto no formato brasileiro (`"779,80"`), e não como número com 2 casas (`domain/Document.java:58-60`, `dto/DocumentResponse.java:21-23`).
2. **Datas no JSON do documento.** `checkIn` e `checkOut` saem como `10/03/2027`, e não como `2027-03-10` (`service/DocumentService.java:11,19-20`).
3. **Identificação incompleta junto com uma completa.** Uma identificação incompleta não entra na contagem. Assim, `guestCpf` mais `companyCnpj` sem `companyName` é aceito como CPF, quando deveria ser recusado (`dto/CreateReservationRequest.java:26-39`).
4. **Formato do dinheiro depende do locale.** O dinheiro é formatado com `String.format("%,.2f")` sem fixar o locale (`domain/PassportIdentification.java:51`, `domain/CpfIdentification.java:40`). Numa JVM com locale brasileiro, o `BRL 1,234.56` e o `R$ 1.234,56` saem trocados.
5. **Possível 500 com corpo inválido.** O controlador só captura `IllegalArgumentException` (`controller/ReservationController.java:35`). Um corpo que não é JSON, ou uma data mal formatada no corpo, provavelmente vira 500, e não 400.

O resto do sistema (quartos, relatórios e mensagens) está como foi pedido, a não ser por detalhes como o `total,0` sem casas decimais num relatório financeiro vazio (`service/ReportService.java:64,84`).
Conclusão: em parte

## Dúvidas

- **A forma usada.** É a do método polimórfico no próprio tipo de hóspede (`identification.generateDocument(...)`). Daria para defender `acerto`: os três lugares passam por um ponto único, e a escolha do texto é polimórfica. Pesou contra a escolha do título por `instanceof` dentro desse mesmo ponto, com uma saída padrão silenciosa, junto com a outra cadeia de `instanceof` no construtor de `Document`. Foi isso que fez a evidência apoiar melhor `malfeito`.
- **Erro com corpo inválido.** Não confirmei sem rodar se o Javalin 6.7.0 embrulha o erro de desserialização do Jackson numa exceção que o controlador captura. O 500 citado no item 5 do comportamento é provável, não verificado.
