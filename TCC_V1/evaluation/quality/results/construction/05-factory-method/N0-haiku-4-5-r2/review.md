# Revisão: construção, cenário 05

## Visão geral

O projeto tem sete pacotes: `api` com quatro controllers estáticos que registram as rotas do Javalin, `service` com `ReservationService` e `ReportService`, `repository` com JDBC puro e `DriverManager`, `model` com classes de campos públicos, `util` com `Pricing` e `DocumentFormatter`, `db` com o esquema em `CREATE TABLE IF NOT EXISTS`, e `App`. Uma requisição entra no controller, que lê o JSON com Gson e chama o serviço; o serviço confere as regras e chama os repositórios, e os dados ficam em três tabelas do PostgreSQL. O problema mais sério é que o sistema não funciona pela API. As respostas usam `ctx.json(...)`, mas o `pom.xml` não traz o Jackson, que é o conversor de JSON que o Javalin 6 usa (`pom.xml:17-58`; o log do serviço diz "It looks like you don't have an object mapper configured"). Além disso, `reservationToJson` e `documentToJson` montam a resposta com `Map.ofEntries`, que não aceita `null` (`ReservationController.java:141-185`), e uma reserva sempre tem campos nulos. O resultado é que toda resposta em JSON vira 500: só 5 de 83 conferências passaram. A reserva fica gravada no banco mesmo quando o cliente recebe 500. Duas reservas ao mesmo tempo gravaram o mesmo quarto nas mesmas noites em 10 de 10 rodadas. E o dinheiro nos documentos sai no formato errado ("R$ 1.234.56").

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `com.pousada` e `api`, e o JDBC só em `db` e `repository`. As maiores classes são `ReservationRepository` (207 linhas), `ReservationController` (197) e `ReservationService` (187).
**Nota:** com problemas

O que está bem:
- As camadas existem e as dependências seguem a direção certa: `api` → `service` → `repository` → `db`.
- As regras de preço, de situação da reserva e de cancelamento ficam no serviço, sem Javalin e sem SQL (`ReservationService.java:28-147`).
- O SQL fica isolado nos repositórios.

O que está misturado:
- **Uma regra no handler.** A regra "exatamente um jeito de identificar o hóspede" fica no handler (`ReservationController.java:44-54`), e não no serviço. Além disso, ela só confere a presença de CPF, de CNPJ ou de passaporte; não exige a razão social com o CNPJ nem o país com o passaporte.
- **A montagem espalhada.** Cada controller cria o seu serviço ou repositório com `new` dentro de um `setup` estático (`ReservationController.java:17`, `ReportController.java:11`, `RoomController.java:15`, `MessageController.java:12`). Cada serviço, por sua vez, cria os próprios repositórios (`ReservationService.java:22-26`, `ReportService.java:18-21`). O `App` só passa o `Database` adiante (`App.java:18-26`).
- **A borda pula o serviço.** `api` depende direto de `repository` e de `db`: `RoomController` e `MessageController` falam com o repositório sem passar por um serviço.

## Design de baixo nível

**Números:** um ponto só escolhe o documento: `ReservationService.generateDocument`, um `if/else` com três casos (`ReservationService.java:149-175`). Os três lugares que emitem o documento chamam esse ponto: a confirmação (`:93`), o check-out (`:118`) e a segunda via (`:146`). Nenhum lugar monta o documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** A referência aceita como acerto "um método que escolhe o documento pelo tipo de hóspede". É o que existe aqui: a decisão entre comprovante, fatura e booking receipt mora num lugar só, e os três pontos de saída pedem o documento a ele. O e-mail só embrulha o texto (`DocumentFormatter.java:85-91`).

O que afasta da forma ideal:
- **Não há tipo de hóspede.** A escolha é deduzida de qual campo solto não é nulo (`ReservationService.java:157,161`).
- **O título é sempre o mesmo.** O `Document` é uma classe só, com o título fixo "Comprovante de reserva" para os três tipos (`ReservationService.java:171`). A fatura e o booking receipt saem com o título errado no JSON.
- **Os três textos são cópias.** As funções estáticas têm 11 ou 12 parâmetros e quase as mesmas linhas (`DocumentFormatter.java:27-83`).
- **A agência custa caro.** Acrescentar o documento da agência exige mexer em pelo menos cinco lugares: as colunas, `Reservation`, a contagem no controller (`ReservationController.java:44-46`), o formatador e a escolha.

**O exagero.** Não há estrutura sem necessidade. Os métodos `withStatus`, `withAmountPaid` e `withRefund` (`Reservation.java:56-72`) nunca são usados; isso é código morto, não exagero.

**O resto do código.** Os defeitos estão espalhados:
- **`null` sem cuidado, que derruba as respostas.** `Map.ofEntries` recebe valores nulos (`ReservationController.java:141-185`), e toda resposta de reserva e de documento lança `NullPointerException`.
- **Erros escondidos.** O `catch (Exception e)` transforma qualquer falha, até uma NPE, em 400 (`ReservationController.java:63-68`). E ainda monta `Map.of("message", e.getMessage())` com uma mensagem nula, que lança outra NPE (o log mostra a linha `:64`).
- **O dinheiro formatado errado.** `String.format("R$ %,.2f").replace(",", ".")` depende do idioma da máquina e troca todas as vírgulas (`DocumentFormatter.java:11-17`). Conferido no jshell, no idioma en_US: sai "R$ 1.234.56" e "BRL 2.783.52".
- **Texto solto no lugar de tipos.** A situação e o plano são `String` comparadas à mão em seis lugares (`ReservationService.java:84,102,112,124,129,143`). Um plano desconhecido vira o normal sem erro (`Pricing.java:34-38`).
- **Campos públicos e listas longas de parâmetros.** Os modelos têm campos públicos (`Reservation.java:7-26`), `createReservation` recebe 12 parâmetros (`ReservationService.java:28-31`) e o construtor de `Document` recebe 17 (`Document.java:25-28`).
- **Detalhes.** `ValidationException` repete o `message` que já vem de `Exception` (`ReservationService.java:177-185`). O subtotal é calculado com `calculateSubtotal(dailyRate, 1)` dentro do laço (`ReservationService.java:64`), uma volta desnecessária.

## Testes

**Números:** 10 testes rodaram, 0 falharam, 10 métodos escritos, tudo numa classe só (`PricingTest`). A cobertura é de 18 de 666 linhas (2,7%) e de 14 de 154 ramos (9,1%). Os pacotes `api`, `service`, `repository` e `model` têm 0% (`measures/tests.md`).
**Nota:** ruim

O que está testado:
- Os testes conferem com valores só as funções de `Pricing`: a alta temporada nas bordas (`PricingTest.java:12-35`), 299,88 por noite (`:42`), o desconto de 89,96 (`:57`), o total (`:72`) e o ISS de 38,99 (`:79`).

O que ficou sem rede:
- **A parte nova do cenário inteira.** Nenhum teste confere a escolha do documento, o texto do comprovante, da fatura ou do booking receipt, ou a validação da identificação. Se alguém apagasse um ramo de `generateDocument` ou trocasse "1 night" por "1 nights", nenhum teste falharia. O erro de formato do dinheiro (`DocumentFormatter.java:11-17`) passou justamente por isso.
- **As outras regras do texto.** Não há teste das situações da reserva, do cancelamento e da devolução, dos códigos de erro, dos relatórios, do banco ou da API.
- **O que derruba o sistema.** Um único teste pela API teria mostrado que nenhuma resposta em JSON funciona.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):
- uma chave estrangeira sem índice (`reservations.room_number`);
- nenhum CHECK, UNIQUE ou EXCLUDE;
- nenhuma transação, nenhum `FOR UPDATE` e nenhum pool;
- o esquema criado no código Java.
**Nota:** ruim

O que está certo:
- Os tipos, a chave estrangeira para `rooms` e o SQL com parâmetros (`?`) em todos os repositórios.

O que está errado:
- **A mesma noite reservada duas vezes.** Nada impede que um quarto tenha duas reservas na mesma noite. O serviço consulta os conflitos e depois insere, em conexões separadas e sem trava (`ReservationService.java:52-55,70`; `ReservationRepository.java:114-134,19-61`). Não há restrição no banco nem sequer um `synchronized`. Conferi no banco do roteiro (`review_behavior_2`): nas 10 rodadas de duas reservas simultâneas, as 10 gravaram duas reservas PENDING para o quarto 301 nas mesmas noites. Dado errado de verdade.
- **Escritas que deviam andar juntas e não andam.** A confirmação grava a situação e o valor pago em duas conexões separadas, sem transação (`ReservationService.java:88-89`). Se a segunda escrita falhar, fica uma reserva CONFIRMED com `amount_paid` 0. O cancelamento tem o mesmo problema (`:133-135`).
- **O esquema não evolui.** É um `CREATE TABLE IF NOT EXISTS` num texto Java (`Database.java:22-73`), sem migrações: a próxima coluna não chega a um banco que já existe.
- **Uma conexão nova a cada consulta.** Cada operação abre uma conexão com `DriverManager.getConnection`, sem pool (`Database.java:16-18`).
- **Nada restringe as situações e os planos.** `status` e `rate_plan` não têm CHECK (`Database.java:43,48`).
- **A devolução zero não é gravada.** Ela só é gravada quando é positiva (`ReservationService.java:134-136`), então uma reserva cancelada sem devolução fica com `refund` nulo, quando devia ter 0.

## Comportamento

**Números:** 5 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 3 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 05: 0 de 32;
- as reservas simultâneas: 1 de 2.
**Nota:** ruim

O serviço sobe e cria o banco, mas toda resposta em JSON quebra. Até `GET /rooms` responde 500. Há duas causas:
- falta o Jackson, que é o conversor de JSON do Javalin 6 (`pom.xml:17-58`; o log em `measures/behavior-service.log` diz "you don't have an object mapper configured");
- os `Map.ofEntries` recebem valores nulos (`ReservationController.java:141-185`).

Também respondem 500 os erros que deviam ser 400, 404, 409 e 422 (`ReservationController.java:188-196`), porque a resposta de erro também passa por `ctx.json`. Só passaram os pontos que respondem em texto: o cabeçalho e o tipo dos relatórios em CSV, e as mensagens que continuam depois de reiniciar (a lista estava vazia). Os relatórios saíram vazios porque nenhuma reserva chegou a ser confirmada.

O mais grave para o dono da pousada é que `POST /reservations` grava a reserva no banco e responde 500. O site acha que falhou, tenta de novo e cria reservas duplicadas. As reservas ao mesmo tempo também gravam duas vezes o mesmo quarto (veja Banco). O "OK" de "nenhuma rodada com as duas criadas" só aparece porque o roteiro olha o código de resposta, e todos foram 500.

O cenário inteiro falhou. Mesmo com o JSON consertado, alguns pontos continuariam errados:
- o dinheiro dos documentos sairia "R$ 779.80" e "BRL 2.783.52" (`DocumentFormatter.java:11-17`);
- o `title` da fatura e do booking receipt sairia "Comprovante de reserva" (`ReservationService.java:171`);
- um CNPJ sem razão social ou um passaporte sem país seriam aceitos (`ReservationController.java:44-46`);
- a devolução sairia `null` em vez de 0,00 numa reserva cancelada sem devolução (`ReservationService.java:134-136`).

## Dúvidas

- **A classe do pattern.** Dei acerto pela forma "método concentrado que escolhe pelo tipo de hóspede". Mas a escolha é um `if` sobre campos nulos dentro do serviço, e o título não varia com o documento. Um revisor mais rigoroso poderia ver nisso uma estrutura fraca, perto de malfeito.
- **A linha de total do relatório financeiro.** O texto não diz se ela traz colunas vazias. O agente escreveu `total,,,,valor` (`ReportService.java:78`) e o roteiro espera `total,599.76`. Não chegou a pesar, porque o relatório já falhou antes, sem reservas confirmadas.
- **"cinco quartos (veio: 4)".** Isso vem do roteiro lendo a página de erro 500, não de um quarto que faltou: o esquema cria os cinco (`Database.java:64-70`).
