# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto Javalin em camadas clássicas: `controller` (as rotas, com mapas montados à mão como DTO),
`service` (as regras da reserva, o documento, o e-mail e os relatórios), `repository` (JDBC puro com `?`), `model`
(entidades, enums e a identificação do hóspede como `sealed interface` com três `record`) e `util` (o cálculo de preço
e o `DatabaseInitializer`, que cria as tabelas com `CREATE TABLE IF NOT EXISTS` e também entrega as conexões por
`DriverManager`). Uma requisição entra em `App.java:64-77`, passa pelo controller, que valida o corpo e monta a
`Reservation`, vai ao `ReservationService` e termina num repositório; tudo fica no PostgreSQL, em três tabelas. A
escolha do documento por tipo de hóspede está concentrada num lugar só (`DocumentService.generateDocument`). Há três
defeitos sérios. Primeiro: o controller lê o corpo como `Map` e faz `(String) body.get("roomNumber")`
(`ReservationController.java:33`), então um `roomNumber` numérico derruba **toda** criação de reserva com 500, e o
roteiro do comportamento passou só 11 de 83 conferências. Segundo: nada no banco nem no código impede duas reservas
simultâneas do mesmo quarto. Chamei a API à mão (com o número do quarto como texto) e as dez rodadas simultâneas
criaram as duas reservas. Terceiro: o agente escreveu só 13 testes, que cobrem 4,8% das linhas e nada da parte nova.

## Arquitetura

**Números:** 8 pacotes, 17 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
na raiz e em `controller`. O JDBC aparece só em `repository` e `util`. A maior classe é `ReservationRepository`, com
278 linhas.
**Nota:** bom

- **As dependências seguem uma direção só.** Elas vão de `controller` para `service`, de `service` para `repository`
  e daí para `model`. As regras (`service`, `util/PriceCalculator`) não conhecem o Javalin nem montam SQL. Por
  exemplo, `ReservationService.java:28-69` só chama repositórios e `PriceCalculator`.
- **A montagem fica num lugar só.** Todas as peças são criadas e ligadas no `main` (`App.java:30-54`), com injeção
  pelo construtor e sem `static` global de estado.
- **O SQL está isolado** nos três repositórios. `ReportService.java:24-48` recebe as listas já filtradas pelo banco.

Pontos menores, que não derrubam a nota:

- `DatabaseInitializer` mora em `util` e faz dois papéis: cria o esquema e fornece as conexões
  (`util/DatabaseInitializer.java:19-83`). Por isso os repositórios dependem de `util`.
- Os controllers de leitura chamam os repositórios direto, sem passar pelo serviço (`ReservationController.java:63`,
  `RoomController.java:19`, `MessageController.java:19`). Num sistema deste tamanho isso é aceitável.
- O handler de check-out orquestra duas chamadas ao serviço (`ReservationController.java:86-91`): muda a situação,
  descarta o resultado e lê de novo para gerar o documento. Esse passo deveria morar no serviço.
- A regra "exatamente um jeito de identificar" está no controller (`ReservationController.java:133-162`), e não no
  domínio.

## Design de baixo nível

**Números:** há 3 tipos de documento (comprovante, fatura, booking receipt) e 3 lugares onde ele sai (o e-mail da
confirmação, o check-out e a segunda via). A escolha do documento acontece num método só, que os 3 lugares chamam. O
`if/else instanceof` sobre o tipo de hóspede aparece em mais 4 lugares, mas lá só mapeia campos, sem decidir qual
documento criar.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento criar está concentrada em `DocumentService.generateDocument`
(`service/DocumentService.java:18-50`). Ele decide o título e o texto pelo tipo de `GuestIdentification` (linhas
33-47) e delega a montagem do texto a um método por documento (`generateReceiptText`, `generateInvoiceText`,
`generateBookingReceiptText`, linhas 52-121). Os três lugares pedem o documento a esse método e nenhum o monta à mão:

- a confirmação: `ReservationService.java:86`, que entrega o documento ao `EmailService`
  (`EmailService.java:14-35`, com o e-mail em português e o `document.getText()` no meio);
- a segunda via: `ReservationService.java:156`;
- o check-out: `ReservationController.java:89`, que chama `getDocument`.

Pela referência, isso é a "fábrica concentrada", e por isso conta como acerto.

Uma agência de turismo exigiria um novo `record` e um novo ramo em `generateDocument`. O que afasta da forma ideal:

- O produto é uma classe de dados só (`model/Document.java`), com os cinco campos de identificação nulos ou
  preenchidos à mão em cada ramo.
- O `if/else instanceof` não tem ramo final nem é um `switch` exaustivo sobre a `sealed interface`. Um quarto tipo de
  hóspede compilaria sem aviso e geraria um documento com título e texto `null`.
- O mesmo encadeamento sobre o tipo de hóspede se repete para mapear os campos em quatro lugares:
  `ReservationController.java:171-190` (o JSON), `ReservationRepository.java:221-241` (a gravação),
  `ReservationRepository.java:256-262` (a leitura) e `ReservationController.java:140-161` (a entrada). A agência
  exigiria mexer em todos eles.

**O exagero.** Não há. Não existem interfaces com uma implementação só, fábricas sem necessidade ou camadas que só
repassam. Os métodos estáticos de `AppException` (`exception/AppException.java:21-43`) são só atalhos para os códigos
de erro.

**O resto do código.** Tem defeitos localizados, alguns com efeito visível na API:

- **O corpo da requisição não tem tipo.** O controller lê um `Map` cru e faz cast (`ReservationController.java:29-39`
  e `134-138`). Um campo com o tipo inesperado vira `ClassCastException` e cai no `catch (Exception)` genérico
  (`App.java:84-88`), que responde 500 em vez de 400. Foi isso que derrubou o roteiro inteiro do comportamento. Um
  corpo que não é JSON também dá 500.
- **O dinheiro é formatado pela localidade do Java.** `NumberFormat.getCurrencyInstance(pt-BR)`
  (`DocumentService.java:12`) põe um espaço não separável (U+00A0) entre "R$" e o número. Conferi os bytes na saída.
  Por isso todos os documentos e e-mails em português trazem "R$ 599,76", e não "R$ 599,76". O booking receipt
  é formatado com `Locale.US` e uma troca de "$" por "BRL " (linhas 135-138).
- **A escala do zero não é fixada.** `BigDecimal.ZERO` sem `setScale(2)` aparece em `PriceCalculator.java:61`,
  `ReservationService.java:65` e `ReservationService.java:110`. O JSON sai com `"discount":0`, `"amountPaid":0` e
  `"refund":0`, em vez de `0.00`. O relatório financeiro vazio fecha com `total,0`
  (`ReportService.java:76` com `formatCsvAmount`, linha 81, que faz só `toString()`).
- **Há código duplicado.**
  - O comprovante e a fatura repetem quase todas as linhas (`DocumentService.java:52-72` e `74-98`).
  - O `create` e o `update` repetem os 19 parâmetros (`ReservationRepository.java:34-54` e `105-125`).
  - A lista de colunas do `SELECT` aparece três vezes (linhas 71-74, 163-166 e 192-195).
- **O estado da reserva fica exposto.** `Reservation` é um saco de setters, com um construtor de 16 argumentos
  (`model/Reservation.java:27-48`). As transições de estado e o reembolso vivem no serviço
  (`ReservationService.java:71-141`), o que é aceitável aqui.
- **Os erros perdem o tipo.** As `SQLException` viram `RuntimeException` genérica em todos os repositórios (por
  exemplo, `ReservationRepository.java:65-67`).

O que está bem: `BigDecimal` com `HALF_EVEN` a cada passo (`util/PriceCalculator.java:34-67`), `LocalDate` para as
datas e `enum` para o tipo do quarto, a situação e o plano.

## Testes

**Números:** 13 testes rodaram e todos passaram (0 falhas, 0 erros, 0 pulados), em 13 métodos escritos e 2 classes.
A cobertura é de 45 de 940 linhas (4,8%) e de 16 de 160 ramos (10,0%). Os pacotes `controller`, `service` e
`repository` estão em 0%. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** ruim

- **O que é testado:** só o cálculo de preço, em `PriceCalculatorTest` (`src/test/.../util/PriceCalculatorTest.java`).
  Os testes conferem com valores a alta temporada (linhas 14-38), a noite de R$ 299,88 (linha 44), os R$ 599,76 do
  exemplo do texto (linha 59), o desconto de 89,96 (linha 66) e o total (linha 81).
- **`GuestIdentificationTest`** (`src/test/.../model/GuestIdentificationTest.java:9-27`) só confere que um `record`
  devolve o que recebeu no construtor. Ele executa código, mas não confere nenhuma regra.
- **Nada da parte nova do cenário é testado:**
  - a escolha do documento por tipo de hóspede;
  - o texto da fatura e o ISS de 5%;
  - o booking receipt ("1 night", "BRL 2,783.52");
  - a recusa de identificação dupla, ausente ou incompleta;
  - o e-mail.
- **Também ficam sem teste:** as transições de estado, o cancelamento e a devolução, os relatórios, os códigos de erro
  e a persistência.
- **A pergunta da mutação.** Se alguém trocasse `ISS_RATE` de 0.05 por 0.50 (`DocumentService.java:16`),
  "nights" por "night", ou apagasse a linha do desconto no booking receipt, nenhum teste falharia. O mesmo vale para
  trocar a ordem de "Empresa" e "Hóspede" na fatura.
- **Os defeitos visíveis passaram sem rede.** Um único teste pela API, com o corpo do exemplo, teria pegado o 500 do
  `roomNumber` numérico e o espaço não separável no "R$".

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram: 0 chaves estrangeiras, 0 `CHECK`, 0
`UNIQUE`/`EXCLUDE`, nenhum pool, 0 transações explícitas e 0 `FOR UPDATE`. O esquema é criado por `CREATE TABLE` no
código, sem migrações (`measures/database.md`).
**Nota:** ruim

- **O defeito sério: duas reservas na mesma noite.** O conflito é conferido com um `SELECT COUNT(*)`
  (`repository/ReservationRepository.java:136-160`), e o `INSERT` vem depois, em outra conexão
  (`ReservationService.java:40-46` e `68`). Não há transação, trava ou restrição de exclusão entre os dois. Com o
  serviço de pé e o número do quarto como texto, mandei dez pares de pedidos simultâneos para o mesmo quarto e as
  mesmas noites: **as dez rodadas criaram as duas reservas** (201 e 201). Isso é dado errado no banco: um quarto
  vendido duas vezes. O roteiro oficial não viu o problema porque lá todas as criações caíram no 500 antes.
- **A reserva não está ligada ao quarto.** `reservations.room_number` não tem chave estrangeira para `rooms`
  (`util/DatabaseInitializer.java:31-33`). O banco aceitaria uma reserva de um quarto inexistente.
- **Os campos de situação e de tipo aceitam qualquer texto.** `status`, `rate_plan`, `type` e `channel` são
  `varchar` sem `CHECK` (linhas 26, 44, 49 e 58). Também não há `CHECK` de que exatamente um dos jeitos de
  identificação está preenchido (linhas 36-40).
- **O esquema não evolui.** Ele nasce com `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:19-75`), que não
  consegue acrescentar uma coluna numa tabela que já existe. O próximo pedido do dono vai exigir mexer no banco de
  quem já roda o sistema na mão.
- **Cada consulta abre uma conexão nova.** `DriverManager.getConnection` é chamado a cada consulta
  (`DatabaseInitializer.java:81-83`), sem pool. As conexões são devolvidas com try-with-resources.
- **O que está certo:** o SQL usa parâmetros `?` em todo lugar. A única concatenação
  (`ReservationRepository.java:141`) junta só um trecho constante. Os tipos estão certos para dinheiro e datas, e os
  dados sobrevivem ao reinício (a conferência das mensagens depois do reinício passou).

## Comportamento

**Números:** 11 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum do enunciado | 9 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 05 (o documento por tipo de hóspede) | 0 de 32 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**Por que quase tudo falhou.** Toda criação de reserva respondeu 500. Pelo log do serviço
(`measures/behavior-service.log`), a causa é `ClassCastException: Integer cannot be cast to String` em
`ReservationController.java:33`. O roteiro manda `"roomNumber": 201` como número, e o agente decidiu que o número do
quarto é texto: `GET /rooms` devolve `"number":"101"`, o que também faz falhar "o 201 comporta 3". O texto não fecha o
tipo, e tratar o número como texto seria uma escolha defensável. Responder 500 a um número, porém, não é: o mínimo
seria aceitar os dois ou recusar com 400. Sem reserva criada, caem em cascata a confirmação, o e-mail, o check-in, o
check-out, os cancelamentos, os relatórios com dados, a persistência das reservas e as 32 conferências do cenário.

**O que acontece com o número do quarto como texto.** Para saber o que sobra, subi o serviço à mão, mandei o número
do quarto como texto e chamei a API:

- **O que funciona:**
  - os preços (R$ 599,76; desconto de 37,48 e total de 212,42 no NON_REFUNDABLE; R$ 2.783,52 em julho);
  - os códigos 409, 422, 404 e 400 das recusas;
  - as quatro recusas de identificação (nenhuma, CPF com CNPJ, CNPJ sem razão social, passaporte sem país);
  - as transições e as devoluções;
  - os dois relatórios e o `text/csv; charset=utf-8`;
  - a fatura com o ISS de R$ 38,99 e o e-mail em português com a fatura dentro;
  - o booking receipt ("BRL 2,783.52", "4 nights", datas ISO).
- **O que continua errado:**
  - Um corpo que não é JSON dá 500, e não 400.
  - O dinheiro sai como `0` em vez de `0.00` em `discount` e `amountPaid` na criação e em `refund` no cancelamento
    não reembolsável.
  - Em todos os documentos e e-mails em português, o "R$" vem seguido de um espaço não separável (U+00A0). Um
    programa que procure "Total: R$ 779,80" não encontra.
  - O financeiro de um mês vazio fecha com `total,0`.
  - Duas reservas simultâneas do mesmo quarto são ambas criadas, dez de dez (contado na coluna do banco).

**Para o dono da pousada.** Do jeito que o site provavelmente vai mandar o pedido, com o número do quarto como
número, nenhuma reserva entra no sistema: o site recebe "erro interno". E, mesmo com o número como texto, dois
hóspedes que reservem o mesmo quarto no mesmo instante ficam os dois com a reserva.

## Dúvidas

- **O tipo do `roomNumber`.** O texto não diz se é número ou texto. Se o site combinar mandar texto, a maior parte do
  sistema funciona (como descrevi no comportamento), e a nota de comportamento ficaria em `com problemas`, e não em
  `ruim`, por causa da reserva dupla simultânea, do "R$" com espaço não separável e dos 500 em corpo inválido. O
  defeito de responder 500, em vez de 400 ou de aceitar os dois tipos, é do agente de qualquer forma.
- **A forma do pattern.** A forma usada foi a "fábrica concentrada" (um método que escolhe o documento pelo tipo de
  hóspede), e não um criador com subclasses nem um método no próprio tipo de hóspede. Classifiquei como acerto. O
  `instanceof` repetido em outros quatro lugares só mapeia campos e não decide qual documento criar, e por isso não o
  contei como malfeito.
- **A nota de arquitetura.** Ficou entre `bom` e `com problemas`, por causa da regra de identificação e da
  orquestração do check-out dentro do controller. Pesei como pontos menores.
