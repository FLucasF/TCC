Já li o enunciado e o código. Achei uma decisão do tipo de documento fora da fábrica e duas falhas de comportamento; segue a correção.

# Cenário 05

## Visão geral

O agente separou a identificação do hóspede numa `sealed interface` com três records. A criação do texto ficou num `DocumentGeneratorFactory`, que escolhe um de três `DocumentGenerator` por um `switch` sobre o tipo de identificação. Os três lugares (e-mail de confirmação, check-out e segunda via) pedem o texto ao `DocumentService`, que passa pela fábrica. Até aí está bom. O problema mais sério de desenho é que a fábrica devolve só o texto. O `title` do documento no JSON é decidido de novo, fora dela, por uma cadeia de `instanceof` no `DocumentResponse`, que tem um `else` final que cai em "Booking receipt". Assim, quem usa a estrutura ainda pergunta qual é o caso concreto, e um quarto tipo (a agência) receberia o título errado sem nenhum aviso. No comportamento, os três documentos saem como o enunciado pede. Mas a segunda via sai para reserva cancelada, e o relatório financeiro lista reservas que não deixaram dinheiro.

## Como foi implementado, e por que o julgamento

**O projeto.** O projeto é um Maven com Javalin e não tem camada de controlador. Todas as rotas estão em `Application.main`, que também monta os objetos à mão: repositórios em memória, `DocumentService`, `EmailService`, `ReservationService` e `ReportService` (`src/main/java/com/pousada/Application.java:24-45`). A porta vem de `PORT`, com 7070 se ela não vier (`Application.java:40-41`).

O caminho de uma requisição é assim. A rota lê o DTO e valida os campos obrigatórios e as datas. Depois converte os campos de identificação num `GuestIdentification` (`Application.java:253-277`) e chama o `ReservationService`. O serviço aplica as regras de capacidade, de conflito, de preço (via `PriceCalculator`) e de estados, e guarda tudo em listas no `ReservationRepository`. As respostas são montadas por `ReservationResponse.from` e `DocumentResponse.from`.

Os relatórios CSV ficam no `ReportService`. Os testes do agente são unitários e cobrem só o `PriceCalculator` e os três geradores de texto (`src/test/java/com/pousada/document/DocumentGeneratorTest.java`). Nenhum teste passa pela API.

**A parte do pattern.**
- **Os tipos.** Cada tipo de hóspede é um record que implementa `GuestIdentification` (`src/main/java/com/pousada/domain/GuestIdentification.java:3-4`, `CpfIdentification.java:3`, `CnpjIdentification.java:3`, `PassportIdentification.java:3`).
- **O produto.** Cada documento é um `DocumentGenerator` (`document/DocumentGenerator.java:5-7`): `ComprovanteGenerator`, `FaturaGenerator` (com o ISS em `FaturaGenerator.java:41-43`) e `ReceiptGenerator` (com o dinheiro e as datas no formato inglês em `ReceiptGenerator.java:13` e `ReceiptGenerator.java:44-50`).
- **A escolha.** O generator é escolhido num lugar só, em `DocumentGeneratorFactory.getGenerator`, por um `switch` de pattern matching (`DocumentGeneratorFactory.java:10-15`).
- **Os três lugares.** O `DocumentService.generateDocument` chama a fábrica (`service/DocumentService.java:8-11`). O e-mail usa esse serviço (`service/EmailService.java:17`), e o check-out e a segunda via também (`Application.java:170` e `Application.java:191`). Ninguém cria o texto à mão.

**O que afasta do acerto.**
- **O título fora da fábrica.** O produto da fábrica é só uma `String`. O título do documento, que também depende de qual documento é, é escolhido de novo em `DocumentResponse.from` com `instanceof` (`api/dto/DocumentResponse.java:166-172`). Essa decisão fica fora da fábrica e repete a escolha que a fábrica já fez. Para uma agência, a fábrica seria obrigada a mudar, porque o `switch` sobre a `sealed interface` deixa de compilar. Já o `DocumentResponse` cairia no `else` e mostraria "Booking receipt" sem nenhum aviso.
- **Os casts nos geradores.** Cada gerador também faz cast da identificação para o tipo concreto (`ComprovanteGenerator.java:17`, `FaturaGenerator.java:19`, `ReceiptGenerator.java:20`). Isso funciona porque a fábrica garante o par, mas acopla o produto ao tipo por cast, e não por parâmetro.
- **Os campos de identificação.** A cadeia de `instanceof` que preenche os cinco campos (`DocumentResponse.java:177-195`, repetida em `ReservationResponse.java:43-62`) é a serialização da identificação, não a escolha do documento. Ela pesa pouco aqui.

Pela régua das instruções, isto é uma fábrica que existe e por onde os três lugares passam, mas em que parte da escolha do documento acontece depois, fora dela. Por isso dei `malfeito`, e não `acerto`.

**O exagero colateral.** O `DocumentGenerator` tem três implementações, e isso se justifica. O `DocumentService` (`DocumentService.java:6-12`) é uma camada fina que só junta a fábrica com o `generate`. Mas é o ponto único que os três lugares usam, então não o conto como exagero. Não há builders, fábricas de serviço nem interfaces com uma implementação só.

**O comportamento.** Conferi por leitura. Os três textos seguem os modelos do enunciado, o e-mail é sempre em português e a validação da identificação está correta. Há duas falhas:
- a segunda via só recusa `PENDING` (`Application.java:186`), então uma reserva cancelada também recebe o documento;
- o relatório financeiro filtra só `status != PENDING` (`ReportService.java:51`), então inclui reservas canceladas com reembolso total, que têm valor 0,00 e não deixaram dinheiro.

## Item a item

**O pattern pedido.** Há uma fábrica concentrada: `DocumentGeneratorFactory.getGenerator` faz um `switch` sobre a `GuestIdentification` selada e devolve `ComprovanteGenerator`, `FaturaGenerator` ou `ReceiptGenerator` (`document/DocumentGeneratorFactory.java:10-15`). Os três lugares pedem o texto por `DocumentService.generateDocument` (`service/DocumentService.java:9`): o e-mail em `service/EmailService.java:17`, o check-out em `Application.java:170` e a segunda via em `Application.java:191`. Nenhum deles monta o texto à mão. Mas o produto da fábrica é só o texto, e o título do documento (que é parte de qual documento é) é decidido de novo, fora da fábrica, por `instanceof` em `api/dto/DocumentResponse.java:166-172`. O `else` final devolve "Booking receipt" para qualquer tipo que não seja CPF ou CNPJ, então um quarto documento (o da agência) sairia com o título errado sem erro de compilação. A estrutura existe, mas convive com uma escolha por caso fora dela.
Conclusão: malfeito

**O exagero colateral.** Não há. A interface `DocumentGenerator` tem três implementações reais. O `DocumentService` (`service/DocumentService.java:6-12`) só repassa para a fábrica, mas é o ponto único que os três lugares usam. As demais classes (repositórios em memória, serviços, DTOs) têm função própria, e a montagem é manual em `Application.java:24-33`.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço, sem rodar o Maven e sem chamar a API.
- **O que confere:**
  - **Identificação.** São aceitos exatamente um CPF, ou um CNPJ com a razão social, ou um passaporte com o país. Nenhum, dois ou um par incompleto dão `null`, e a rota responde 400 INVALID_REQUEST (`Application.java:253-277`, `Application.java:72-76`).
  - **Comprovante.** Segue o modelo do ponto de partida, com "1 diária"/"N diárias", a linha de desconto e `R$ 1.234,56` em pt-BR (`ComprovanteGenerator.java:15-35`, `ComprovanteGenerator.java:49`).
  - **Fatura.** Mostra a empresa no lugar do CPF, depois o hóspede, e o ISS de 5% arredondado meio para o par. Para 779,80, o ISS dá 38,99 (`FaturaGenerator.java:20-23`, `FaturaGenerator.java:41-43`).
  - **Booking receipt.** Sai em inglês, com `BRL 1,234.56`, datas `2027-03-10`, "1 night"/"N nights" e "Discount:" (`ReceiptGenerator.java:13`, `ReceiptGenerator.java:28-36`, `ReceiptGenerator.java:44-50`).
  - **O mesmo texto nos três lugares.** O e-mail, o check-out e a segunda via usam o mesmo `generateDocument`. O e-mail tem assunto e moldura em português (`EmailService.java:19-34`).
  - **O JSON do documento.** Traz os cinco campos, com null nos que não se aplicam, e o campo `text` (`DocumentResponse.java:177-205`).
  - **O resto do sistema.** Preço, alta temporada, desconto e arredondamento conferem (`PriceCalculator.java:290-339`). As transições de estado e o reembolso também (`ReservationService.java:57-116`), assim como o conflito de quarto, que ignora as reservas canceladas (`ReservationRepository.java:34-44`), e o relatório de ocupação (`ReportService.java:22-45`).
- **O que não confere:**
  - A segunda via deveria existir só para reserva paga (confirmada, com check-in ou com check-out). O código bloqueia apenas `PENDING` (`Application.java:186`), então uma reserva cancelada também recebe o documento.
  - O relatório financeiro deveria listar só as reservas que deixaram dinheiro. O filtro é apenas `status != PENDING` (`ReportService.java:51`), então entram reservas canceladas com reembolso total ou canceladas antes de pagar, que aparecem com `0.00`.
  - Pontos menores: `BigDecimal.ZERO` sai no JSON como `0`, e não `0.00` (`Reservation.java:44`, `PriceCalculator.java:334`). O `String.format("%.2f")` do CSV depende do locale padrão da JVM (`ReportService.java:71`).

Conclusão: em parte

## Dúvidas

- **A forma usada.** Foi a fábrica concentrada: uma classe com um método estático que escolhe o generator pelo tipo de identificação (`DocumentGeneratorFactory.java:10-15`). Não foi a forma clássica de criador com método sobrescrito, nem um método polimórfico no tipo de hóspede.
- **Entre `acerto` e `malfeito`.** A escolha do texto está num lugar só e os três lugares passam por ela, o que puxa para `acerto`. Pesou para `malfeito` o título do documento ser decidido de novo, fora da fábrica, por `instanceof` com um `else` silencioso (`DocumentResponse.java:166-172`). Se o título for visto como detalhe de apresentação, e não como parte do documento, a classe poderia ser `acerto`.
