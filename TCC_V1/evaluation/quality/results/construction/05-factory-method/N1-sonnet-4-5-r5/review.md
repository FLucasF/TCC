# Revisão: construção, cenário 05

## Visão geral

O projeto tem 7 pacotes, sem framework além do Javalin. `App` cria uma única conexão JDBC (`App.java:25-29`), os três repositórios e o `ApiController`. Esse controlador registra as rotas e conduz cada caso de uso: valida, busca no repositório, chama a regra da `Reservation` ou de um serviço, grava e monta a resposta como `Map` (`api/ApiController.java:94-133`). O preço fica em `PricingService`. A máquina de estados fica na própria `Reservation` (`domain/model/Reservation.java:51-86`). O documento por tipo de hóspede sai do `DocumentService`, por um `switch` sobre a interface selada `GuestIdentification` (`domain/service/DocumentService.java:52-60`). Os dados ficam em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código (`infrastructure/database/Database.java:20-65`). O defeito mais sério quebra o sistema inteiro: a consulta de disponibilidade usa `(? IS NULL OR id != ?)` com `setObject(null)` (`infrastructure/database/ReservationRepository.java:100,107-108`). O PostgreSQL responde "could not determine data type of parameter $4" (`measures/behavior-service.log:20`), e por isso **nenhuma reserva pode ser criada: todo POST /reservations devolve 500**. Sem reserva, nada do resto acontece: confirmação, e-mail, documentos e relatórios. Os testes (4, só de preço) não chegam perto disso.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo. A maior classe é `ApiController`, com 337 linhas. O Javalin aparece só em `com.marealta` e `api`. JDBC aparece em `domain.service` (por causa do `EmailService`) e na infraestrutura (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem. O domínio (`domain.model`) não conhece Javalin nem JDBC. Todo o SQL fica nos repositórios (`infrastructure/database/*`, `infrastructure/messaging/MessageRepository.java`). As regras de situação e devolução estão na entidade (`Reservation.java:51-86`), e o preço num serviço sem dependências (`PricingService.java`). Há, porém, dois problemas:

- **Não há camada de aplicação: o controlador conduz os casos de uso.** `ApiController` fala direto com os três repositórios. Ele decide a sequência confirmar, gravar, gerar documento e mandar e-mail (`ApiController.java:144-162`). Ele faz a checagem de disponibilidade antes de gravar (`ApiController.java:103-108`) e a regra "segunda via só para reserva paga" (`ApiController.java:209-213`). Ele traduz a identificação do hóspede (`ApiController.java:264-275`) e escolhe o título do documento (`ApiController.java:328-336`). É a maior classe e o lugar onde o próximo pedido vai doer.
- **A montagem fica dividida.** `App` cria os repositórios, mas o `ApiController` cria por dentro `PricingService`, `DocumentService`, `EmailService`, `ReportService` e `ValidationService` (`ApiController.java:40-44`). Há ainda um `ObjectMapper` estático global (`api/JsonMapper.java:7-13`).

O `EmailService`, no pacote de domínio, depende da infraestrutura (`domain/service/EmailService.java:5,10`). Não chega a formar ciclo.

## Design de baixo nível

**Números:** os três lugares que emitem o documento (confirmação `ApiController.java:154`, check-out `:198`, segunda via `:216`) chamam o mesmo `documentService.generate`. A escolha do texto está num único `switch` exaustivo sobre a interface selada (`DocumentService.java:55-59`). O título do documento é escolhido de novo fora da fábrica, por `if/else` sobre os campos (`ApiController.java:328-336`). Há mais 3 cadeias de `instanceof` sobre o tipo de hóspede (`ApiController.java:285-289`, `DocumentService.java:26-30`, `ReservationRepository.java:127-145`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima de um acerto: a criação do documento está concentrada numa fábrica, `DocumentService.generate`, que os três lugares usam. O `switch` sobre a interface selada `GuestIdentification` (`domain/model/GuestIdentification.java:3-4`) obriga o compilador a acusar um quarto tipo (agência) sem caso. O que afasta: a fábrica devolve sempre o mesmo `Document` genérico (`domain/model/Document.java`), e quem o usa ainda pergunta qual é o caso. `getDocumentTitle` deduz o título olhando se `guestCpf` ou `companyCnpj` vêm preenchidos. No `else`, devolve "Booking receipt" para qualquer outro caso (`ApiController.java:328-336`). Uma agência receberia o título em inglês sem nenhum erro de compilação. Os campos de identificação do documento também são extraídos à mão com `instanceof` (`DocumentService.java:26-30`), copiando o que o controlador faz (`ApiController.java:285-289`). Por isso a classe é malfeito: a estrutura existe, mas parte da decisão acontece fora dela.

**O exagero.** Não há interface com uma implementação só, nem builder, nem camada que só repassa. A interface selada tem três implementações reais.

**O resto do código:**
- **Reflexão para restaurar o estado.** `ReservationRepository.java:212-230` usa `getDeclaredField(...).setAccessible(true)` para escrever `status`, `amountPaid` e `refund`, que são campos privados da `Reservation`. Isso contorna o encapsulamento e quebra calado se um campo mudar de nome.
- **Duplicação.** `generateComprovante` e `generateFatura` repetem as mesmas seis linhas (`DocumentService.java:62-82` e `84-109`). O bloco `findById(...).orElseThrow(RESERVATION_NOT_FOUND)` aparece seis vezes no controlador (`ApiController.java:136-139, 145-148, 165-168, 177-180, 189-192, 204-207`).
- **Formatação de dinheiro errada e não segura entre threads.** `NumberFormat` é estático e compartilhado entre as threads do Jetty (`DocumentService.java:14-20`), e `NumberFormat` não é thread-safe. Conferido no JDK 21 desta máquina, o resultado também está errado. Em pt-BR ele sai `R$ 1.234,56`, com espaço não separável em vez do espaço comum. Em inglês, `Locale.US` com BRL sai `R$1,234.56`, e o `replace("BRL", "BRL ")` (`DocumentService.java:136`) não faz nada. O *booking receipt* nunca mostraria `BRL 1,234.56`.
- **Erros.** Qualquer `IllegalStateException` vira 409 OPERATION_NOT_ALLOWED (`ApiController.java:71-73`), inclusive a que indica dado corrompido no banco (`ReservationRepository.java:209`). Corpo que não é JSON, data inválida no corpo e id não numérico (`Long.parseLong`, `ApiController.java:136`) caem no `catch (Exception)` genérico e viram 500 (`ApiController.java:75-78`).
- **Tipos e estado.** `PricingResult` tem campos públicos (`PricingService.java:15-27`). O zero inicial de `amountPaid` e do desconto é `BigDecimal.ZERO`, com escala 0 (`Reservation.java:47`, `PricingService.java:83`), e sai no JSON como `0`, não `0.00`. O total do financeiro sem linhas sai `total,0` (`ReportService.java:65,82-88`).

Por outro lado, `BigDecimal` com HALF_EVEN em cada passo (`PricingService.java:49-56,79-81`), `LocalDate` e os `enum` de situação, plano e tipo de quarto estão certos.

## Testes

**Números:** 4 testes rodaram (0 falhas, 0 erros, 0 pulados), 4 métodos escritos, num único arquivo (`PricingServiceTest`). Cobertura de linhas: 52 de 764 (6,8%). Cobertura de ramos: 14 de 225 (6,2%). `api`, `infrastructure.*` e `App` têm 0% (`measures/tests.md`).
**Nota:** ruim

A coluna está quase ausente. Os únicos testes conferem o preço com valores: STANDARD fora da temporada, 499,80 (`src/test/java/com/marealta/domain/service/PricingServiceTest.java:24-27`); NON_REFUNDABLE com desconto de 74,97 (`:39-42`); julho, 599,76 (`:54-57`). O quarto teste, sobre dezembro e janeiro, só confere `isGreaterThan(499.80)` (`:74`) e deixa sem uso a variável que calculou (`:71`). Uma troca de 15/12 por 16/12 em `PricingService.java:70` passaria.

Nada da parte nova do cenário tem teste: a escolha do documento, a fatura com ISS de 5%, o *booking receipt* em inglês, a validação de "exatamente uma identificação". Também ficam sem teste as transições de situação, a devolução no cancelamento, os relatórios, os códigos de erro e o acesso ao banco. A pergunta da mutação: trocar `0.05` por `0.5` no ISS (`DocumentService.java:104`), `" nights"` por `" night"` (`:122`) ou `count != 1` por `count > 1` (`ValidationService.java:59`) não derrubaria teste nenhum. Como não há teste pela API nem contra o banco, o defeito que impede criar qualquer reserva passou pelo `mvn verify`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number)` sem índice; nenhum CHECK em `status`, `rate_plan`, `type` e `channel`; nenhuma restrição UNIQUE, EXCLUDE ou CHECK que proteja uma regra; uma única conexão por `DriverManager`, sem pool; 0 transações explícitas; 0 `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

- **A consulta central não roda no PostgreSQL.** `hasConflictingReservation` usa `AND (? IS NULL OR id != ?)` e passa `null` com `setObject` (`infrastructure/database/ReservationRepository.java:93-117`, linhas 100 e 107-108). O driver não informa o tipo, e o servidor recusa a consulta: "could not determine data type of parameter $4" (`measures/behavior-service.log:20-29`). Essa consulta roda antes de todo INSERT (`ApiController.java:103-108`), então nenhuma reserva chega ao banco. Os dados do negócio não ficam guardados porque nunca podem ser gravados.
- **Nada impede a reserva em dobro, mesmo depois de consertada a consulta.** O código só verifica e depois insere (`ApiController.java:103-130`), em *autocommit*. Não há transação, trava ou restrição EXCLUDE.
- **Uma só conexão para todo o serviço.** A mesma `Connection` é compartilhada por todas as threads de requisição (`App.java:25-29`, `Database.java:9-13`), sem pool e sem reconexão. Se ela cair, o serviço para de responder até reiniciar.
- **Escritas que deviam andar juntas, separadas.** A confirmação grava a situação e depois a mensagem, sem transação (`ApiController.java:150-159`).
- **O esquema é criado no código e não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:20-65`) não sabe acrescentar uma coluna numa tabela que já existe, e não há migração.
- **Os relatórios carregam a tabela inteira** de reservas para filtrar em Java (`ApiController.java:242,257`).

Do lado certo: SQL sempre com parâmetros, try-with-resources nos statements, chave estrangeira para `rooms` (`Database.java:51`), dinheiro em `numeric(10,2)`, quartos inseridos com `ON CONFLICT DO NOTHING` (`RoomRepository.java:21-35`).

## Comportamento

**Números:** 19 de 83 conferências passaram. Parte comum: 13 de 46. Depois de reiniciar: 1 de 3. Cenário 05: 4 de 32. Reservas simultâneas: 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

Todo POST /reservations válido responde 500 INTERNAL_ERROR por causa da consulta de disponibilidade quebrada. Isso vale também para as 20 tentativas simultâneas. Conferi à mão: com o serviço de pé, criar uma reserva com CPF devolveu `{"error":"INTERNAL_ERROR",...}`. Daí vem quase toda a lista de FALHOU: confirmar, check-in, check-out, cancelamentos e devoluções respondem 404 porque a reserva não existe. O e-mail nunca sai. Os relatórios vêm vazios (`total,0/5`, `total,0`). O "quarto ocupado: 409" vira 500. No cenário, nenhum dos três documentos (comprovante, fatura, *booking receipt*) pôde ser emitido.

O que passou não depende de existir reserva: GET /rooms, as recusas que acontecem antes da consulta (capacidade 422, quarto inexistente 404, datas 400, hóspedes 400, campos faltando 400, mês fora do formato 400), as quatro recusas de identificação (nenhuma, CPF+CNPJ, CNPJ sem razão social, passaporte sem país) e as mensagens depois de reiniciar (a lista vazia continua vazia).

Duas outras falhas são do agente, independentes da consulta. O corpo que não é JSON dá 500 em vez de 400 INVALID_REQUEST (`ApiController.java:95,75-78`). E o "201 comporta 3" falhou porque o `number` do quarto sai como texto (`"201"`), não como número. Este último é uma escolha que o texto não fechou, e não pesa.

Para o dono da pousada: o site não consegue fazer reserva nenhuma. Mesmo depois de consertada a consulta, a leitura do código mostra que os documentos sairiam errados: o espaço não separável depois de "R$" no comprovante e na fatura, e "R$1,234.56" em vez de "BRL 1,234.56" no *booking receipt* (`DocumentService.java:14-20,134-137`). A reserva em dobro também continuaria sem proteção.

## Dúvidas

- **A classe do pattern ficou entre malfeito e acerto.** A forma usada é uma fábrica concentrada: um método com `switch` sobre a interface selada (`DocumentService.java:55-59`), que os três lugares chamam. Pesou para malfeito o título decidido fora da fábrica, com um `else` que serve qualquer caso novo (`ApiController.java:328-336`). Quem considerar o título um detalhe de apresentação daria acerto.
- **Não foi possível confirmar pela API** os defeitos dos documentos (espaço não separável, `R$` no lugar de `BRL`) nem a corrida das reservas simultâneas, porque nenhuma reserva pôde ser criada. Os dois primeiros vêm da leitura do código e de uma execução isolada do `NumberFormat` no JDK 21.0.12 desta máquina. A corrida vem da leitura de `ApiController.java:103-130`.
