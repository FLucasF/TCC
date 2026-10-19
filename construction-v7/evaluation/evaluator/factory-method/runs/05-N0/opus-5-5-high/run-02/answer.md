# Cenário 01

## Visão geral

O agente montou um sistema simples e enxuto: Javalin, um handler único, dois serviços, utilitários estáticos e um singleton em memória. Os três documentos (comprovante, fatura e *booking receipt*) saem com o texto certo. O problema mais sério é de desenho e está justamente na parte do fim do enunciado: não existe um lugar único que decide qual documento criar. A escolha por tipo de hóspede é uma cadeia `if/else if/else` sobre os campos da reserva, copiada em dois pontos, `service/ReservationService.java:157-165` (e-mail de confirmação) e `api/dto/DocumentResponse.java:45-51` (check-out e segunda via). Um documento de agência exigiria mexer nas duas cópias. Isso é uma omissão do pattern. No comportamento, os textos dos documentos conferem, mas o JSON omite os campos `null` que o enunciado pede. Há também erros na parte comum: a alta temporada está errada, uma reserva encostada antes de outra é recusada, um corpo que não é JSON dá 500 e a segunda via sai para reserva cancelada.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadameraalta` tem:
- `Main`, que lê `PORT` (padrão 7070), cria o Javalin com um `GsonJsonMapper` e registra as onze rotas em `Main.java:19-29`, todas apontando para métodos de `api/handler/ApiHandler`.
- `ApiHandler`, que faz o parse com Gson para DTOs públicos (`ReservationRequest`, `ReservationResponse`, `DocumentResponse` e outros). Chama `RequestValidator` (estático) e depois `ReservationService` ou `ReportService`. Os erros do serviço são `Exception` com o código na mensagem, traduzidos em status por uma cadeia de `if` em `ApiHandler.java:214-238`.
- `ReservationService`, que guarda as regras de estado, disponibilidade, preço (via `util/PriceCalculator`, estático) e o envio do e-mail.
- `ReportService`, que gera os dois CSVs.
- `repository/DataStore`, um singleton com `HashMap`s, os cinco quartos fixos (`DataStore.java:33-39`), os contadores de id e a lista de mensagens.

O caminho de uma requisição é: rota → `ApiHandler` → `RequestValidator` → `ReservationService` → `DataStore`. O jar é feito pelo shade plugin com `Main` no manifesto. O agente escreveu 21 testes JUnit 4: 11 de `PriceCalculator` e 10 de `ReservationService`. Nenhum toca nos documentos ou nos tipos de hóspede.

**A parte do fim.** O tipo de hóspede não é representado por nenhum tipo. A reserva (`model/Reservation.java`) carrega os cinco campos soltos (`guestCpf`, `companyCnpj`, `companyName`, `guestPassport`, `guestCountry`), e o "tipo" fica implícito em qual deles não é nulo. O texto de cada documento é uma função estática separada em `util/DocumentGenerator.java`: `generateReceipt` (`:7`), `generateInvoice` (`:27`, com o ISS em `:45`) e `generateBookingReceipt` (`:51`). A decisão de qual função chamar aparece em dois lugares:
- `ReservationService.getDocument` (`service/ReservationService.java:157-165`), usado só pelo e-mail de confirmação (`:81`);
- `DocumentResponse.fromReservation` (`api/dto/DocumentResponse.java:45-51`), usado pelo check-out (`api/handler/ApiHandler.java:136`) e pela segunda via (`ApiHandler.java:156`).

As duas cadeias são idênticas: `if cpf != null → receipt; else if cnpj != null → invoice; else → booking receipt`. A regra "exatamente um jeito de identificar" mora no `RequestValidator.java:39-53`.

**O raciocínio.** As instruções do pattern pedem que a escolha do documento more num lugar só e que os três lugares peçam o documento a esse lugar. Aqui não há criador, fábrica nem método polimórfico no tipo de hóspede. Há funções auxiliares de texto, e as instruções dizem explicitamente que separar o texto em funções não conta: o que importa é onde se decide qual criar. A decisão está duplicada. O check-out e a segunda via compartilham uma cópia, então são duas cópias e não três, mas nenhuma das duas é uma fábrica que a outra use: são `if`s paralelos. Quando a agência chegar, será preciso um novo campo, um novo ramo em `ReservationService.java:157` e outro em `DocumentResponse.java:45`, além de mexer no validador. Isso é **omissão**, e não malfeito: o malfeito pressupõe que a fábrica existe e que um dos lugares a contorna, e aqui ela não existe.

No exagero colateral não há interfaces de uma implementação só, fábricas, builders nem camadas de repasse. O `GsonJsonMapper` é necessário para o Javalin. O `DataStore` singleton é simples, e não estrutura sobrando. O que existe é o contrário do exagero, duplicação: o `switch` do tipo de quarto aparece copiado em `DocumentGenerator.java:71`, `DocumentResponse.java:56` e `ReportService.java:94`.

No comportamento, os documentos estão corretos em texto, mas o contrato JSON e várias regras da parte comum falham (detalhes abaixo).

## Item a item

**O enunciado (pede o pattern).** A escolha do documento por tipo de hóspede é uma cadeia `if/else` sobre campos nulos, repetida em `service/ReservationService.java:157-165` (e-mail de confirmação) e em `api/dto/DocumentResponse.java:45-51` (check-out em `api/handler/ApiHandler.java:136` e segunda via em `ApiHandler.java:156`). Os textos ficam em funções estáticas separadas (`util/DocumentGenerator.java:7`, `:27`, `:51`), mas nada concentra a decisão de qual criar. Não há classe ou método de fábrica nem tipo de hóspede polimórfico. Um quarto tipo, o da agência, exigiria editar as duas cadeias. Não é o pattern que o enunciado precisava.
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e diretas: `ApiHandler`, os dois serviços, os utilitários estáticos e o `DataStore` singleton (`repository/DataStore.java:11-24`). O `GsonJsonMapper` (`api/GsonJsonMapper.java:9`) é exigido pelo Javalin. O defeito de estrutura fora da parte do fim é a duplicação do `switch` de tipo de quarto (`DocumentGenerator.java:71`, `DocumentResponse.java:56`, `ReportService.java:94`), não estrutura a mais.
Conclusão: contenção

**O comportamento.** Testei com `curl` numa cópia, na porta 7070.

O que confere:
- **Identificação:** sem nenhuma identificação, CPF+CNPJ, CNPJ sem razão social, passaporte sem país e só o país dão 400 INVALID_REQUEST.
- **Fatura:** texto exato do enunciado (`Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)`, `ISS retido (5%): R$ 38,99` sobre 779,80).
- **Booking receipt:** em inglês (`BRL 1,159.80`, `2027-03-10`, `2 nights`; com 1 noite e NON_REFUNDABLE, `1 night` e `Discount: BRL 37.48` antes do total).
- **CPF:** o hóspede recebe o comprovante da parte comum.
- **Os três lugares:** o mesmo texto sai no e-mail (sempre em português, com o assunto certo), no check-out e na segunda via, e o JSON tem `text`.
- **Parte comum:** STANDARD de 10/07 a 12/07/2027 dá 599.76; capacidade excedida dá 422, sobreposição 409 e quarto inexistente 404. Os CSVs estão em `text/csv;charset=utf-8`, com `total,4/5`. Cancelar NON_REFUNDABLE confirmada dá `refund: 0`, e STANDARD confirmada devolve o total.

O que não confere:
- **Campos nulos omitidos:** os campos de identificação que não se aplicam somem do JSON da reserva e do documento, quando deviam vir `null`. O `refund` também some antes do cancelamento. O Gson está sem `serializeNulls`, em `api/GsonJsonMapper.java:10`.
- **CPF com campo solto:** CPF com `companyName` (ou com `guestCountry`) é aceito e o campo solto é devolvido junto (`RequestValidator.java:39-42` só conta CPF, CNPJ e passaporte).
- **Título fixo:** o `title` do documento é sempre "Comprovante de reserva", mesmo na fatura e no receipt (`DocumentResponse.java:28`).
- **Alta temporada:** 01 a 03/12/2027 no quarto 102 dá 599.76 em vez de 499.80. De 14 a 16/12 no 202 dá 935.76 em vez de 857.78. Dezembro inteiro conta, 29/02 fica fora (`util/PriceCalculator.java:15-18`), e todas as noites usam o preço da noite de entrada (`ReservationService.java:40-41`).
- **Reserva encostada antes:** uma reserva de 10 a 12/08 é recusada com 409 quando já existe outra de 12 a 14/08 no mesmo quarto (`ReservationService.java:153`).
- **Corpo inválido:** um corpo que não é JSON, ou um `ratePlan` inválido, dá 500 INTERNAL_ERROR (`ApiHandler.java:58-59`, `:235-237`).
- **Segunda via:** sai para reserva cancelada que tinha sido paga (`ApiHandler.java:150`). Para reserva pendente, dá 404 RESERVATION_NOT_FOUND.
- **Financeiro:** o relatório lista uma reserva STANDARD cancelada e reembolsada com `0.00` (`ReportService.java:77`).

`mvn -q verify`: 21 testes, 0 falhas, BUILD SUCCESS.
Conclusão: em parte

## Dúvidas

- **Classe.** A decisão aparece em duas cópias, não em três, porque o check-out e a segunda via compartilham `DocumentResponse.fromReservation`. Isso aproxima o caso de um "malfeito" com dois lugares de criação. Mantive omissão porque não existe fábrica alguma: as duas cadeias são `if`s paralelos, nenhuma é um ponto de criação que a outra use, e as instruções do pattern deixam claro que funções de texto separadas não contam. Nenhuma das formas aceitas (Factory Method clássico, fábrica concentrada ou método polimórfico no hóspede) foi usada.
- **Locale.** A formatação de dinheiro (`util/Formatter.java:25`, `:29`, `:33`) depende do locale padrão da JVM. Conferiu porque o ambiente está em `en_US`; num servidor em `pt_BR`, o `R$` e o CSV sairiam trocados. Não contei isso no resultado.
