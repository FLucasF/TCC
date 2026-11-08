# Revisão: construção, cenário 05

## Visão geral

O agente montou o projeto em seis pacotes (`api`, `service`, `repository`, `database`, `domain`, `util`) mais o `Main`. Uma requisição entra pelo `Api` (handlers estáticos registrados em `api/Api.java:28-39`), passa por um serviço estático (`service/ReservationService.java`), que lê e grava pelos repositórios estáticos (`repository/*.java`) sobre um pool HikariCP guardado num `static` em `database/Database.java:11`; o esquema é criado com `CREATE TABLE IF NOT EXISTS` ao subir (`database/Database.java:30-76`). O defeito mais sério é que o sistema não funciona: o `pom.xml` não traz o `jackson-databind` (nem configura outro `JsonMapper`), e no Javalin 6 todo `ctx.json(...)` sem um mapper lança exceção. O jar gerado não contém nenhuma classe `com.fasterxml.jackson` (conferido no `target/reservas.jar`), o log do serviço mostra o aviso "It looks like you don't have an object mapper configured" (`measures/behavior-service.log:25`), e o roteiro de comportamento teve 500 em toda chamada que devolve JSON: 6 de 83 conferências passaram, e as 6 são as que não dependem de JSON (os CSVs vazios, um `null`, "as mensagens continuam lá" com lista vazia). Fora isso, lendo o código: a confirmação exige um `amountPaid` no corpo que o texto não prevê (`api/Api.java:135-140`), o check-out devolve a reserva em vez do documento (`api/Api.java:201-220`), a segunda via sai para reserva pendente (`service/ReservationService.java:176-178`), e o Factory Method pedido ficou numa cadeia de `if` repetida.

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `com.posadamareaita` e `api`; JDBC só em `database` e `repository`; `service` e `domain` sem biblioteca. Maior classe: `api/Api.java` com 312 linhas.
**Nota:** com problemas

As camadas existem e apontam na direção certa (api → service → repository → database; `domain` e `util` sem dependências de framework), e não há ciclo. Mas a borda não é fina nem a regra fica só no serviço:

- Os handlers chamam os repositórios direto, pulando o serviço: `api/Api.java:43` (`RoomRepository.findAll`), `:95` (`RoomRepository.findByNumber`), `:117` (`ReservationRepository.findById`), `:144`, `:165`, `:186`, `:207`, `:233`, `:246`.
- O serviço devolve `Optional.empty()` para toda recusa, sem dizer o motivo (`service/ReservationService.java:27-56`), e o handler **refaz as regras** para descobrir qual foi (`api/Api.java:93-105`: consulta o quarto de novo, compara capacidade, data e hóspedes). A regra de negócio mora então em dois lugares, e uma recusa por identificação errada (`ReservationService.java:24-29`) cai no ramo "else" do handler e viraria `409 ROOM_UNAVAILABLE` (`api/Api.java:103`). O plano de tarifa é validado no handler (`api/Api.java:74-79`) e de novo no serviço (`:59`).
- A montagem é toda estática: `Database.dataSource` é um `static` global (`database/Database.java:11`), e todos os serviços e repositórios são classes de métodos estáticos (`service/ReservationService.java:17`, `repository/ReservationRepository.java:19`). Não há um lugar que cria e liga as peças: `Main.java:21-28` só inicializa o banco e registra rotas. Isso também é o que impede testar o serviço sem o PostgreSQL.
- A rota é registrada depois do `start` (`Main.java:24-28`): funciona, mas há uma janela em que o servidor já atende e responde 404.

Não há SQL nem Javalin no serviço, e o acesso ao banco está confinado em `repository/` e `database/`, por isso não é `ruim`.

## Design de baixo nível

**Números:** 3 tipos de documento em 3 métodos estáticos (`service/DocumentService.java:12`, `:37`, `:62`). A escolha de qual criar aparece em 2 lugares, como cadeia de `if` sobre os campos da reserva (`service/ReservationService.java:95-101` na confirmação e `service/DocumentService.java:107-113` na segunda via); o terceiro lugar, o check-out, não gera documento nenhum (`service/ReservationService.java:127-142`, `api/Api.java:201-220`). Nenhum caso está numa estrutura de fábrica: 0 na estrutura, 3 fora.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

*O pattern.* O texto pede que a escolha do documento (comprovante, fatura, booking receipt, e as agências que vêm aí) more num lugar só, com os três pontos de saída (e-mail, check-out, segunda via) pedindo o documento a esse lugar. O que há é um `if (res.getGuestCpf() != null) ... else if (res.getCompanyCnpj() != null) ... else ...` copiado em `ReservationService.java:95-101` e em `DocumentService.java:107-113`, e o check-out simplesmente devolve a reserva (`api/Api.java:218`), sem documento. Não existe tipo de hóspede, nem interface de documento, nem fábrica: o "tipo" é inferido pelos campos nulos a cada vez. O próprio `IMPLEMENTATION.md` do agente lista que uma agência de turismo exigiria tocar em quatro pontos (método novo, campos em `Reservation`, validação no serviço e a cadeia de `generateDocumentResponse`), sem contar a cadeia em `confirm`. É a definição de omissão na referência: o `if` sobre o tipo de hóspede repetido nos lugares que criam o documento. O que aproxima: o texto de cada documento está isolado num método próprio, e o título, o ISS (`DocumentService.java:38-39`, com `MoneyUtil.round`) e os formatos EN/PT (`util/FormattingUtil.java:12-43`) estão certos na leitura.

*O exagero.* Nada sobrando: sem interfaces de uma implementação só, sem builders. `MessageService` (`service/MessageService.java:7-10`) é um repasse de uma linha, mas é o ponto natural para o envio de verdade no futuro; não conta.

*O resto do código.*
- Duplicação: o corpo dos três documentos (Quarto/Entrada/Diárias/Desconto/Total) está copiado três vezes (`DocumentService.java:22-32`, `:46-56`, `:72-82`); `nights` é calculado em `ReservationService.java:65` e de novo em `PriceCalculator.java:26`; o mapeamento de erro é repetido em cada handler (`api/Api.java:93-105`, `:143-150`, `:164-171`, `:185-192`, `:206-213`).
- Erros: nove `catch (Exception e)` (`api/Api.java:109,125,154,175,196,217,240,267,283`) que transformam qualquer falha em `400 INVALID_REQUEST` ou `404 RESERVATION_NOT_FOUND`, sem registrar nada. Um banco fora do ar vira "reserva não encontrada"; e foi esse padrão que mascarou o erro do mapper: a exceção do `ctx.json` é capturada e o `catch` tenta outro `ctx.json`, que lança de novo e sai como 500. `Optional.empty()` como resposta para seis motivos diferentes de recusa (`ReservationService.java:24-56`) obriga quem chama a adivinhar.
- A validação da identificação conta só CPF, CNPJ e passaporte (`ReservationService.java:24-29`): CNPJ sem razão social e passaporte sem país passam, e a fatura sairia com "Empresa: null".
- Estado: `PriceResult` com campos públicos mutáveis (`util/PriceCalculator.java:13-15`); `static` mutável em `Database.java:11`.
- Tipos: `BigDecimal` com `HALF_EVEN` em cada passo (`util/PriceCalculator.java:30-48`, `util/MoneyUtil.java:7`), `LocalDate`, enums para plano, situação e tipo de quarto: certo. `String.format("%.2f")` no CSV depende do locale da JVM (`service/ReportService.java:58,63`), e `new Locale("pt","BR")` é construtor depreciado no Java 21 (`util/FormattingUtil.java:15`).
- A alta temporada termina em "28 de fevereiro" literal (`util/SeasonUtil.java:23`): 29/02 de ano bissexto fica fora, contra o "fim de fevereiro" do texto.
- O `discount` do documento vem `null` quando é zero (`DocumentService.java:103`), diferente da reserva, que manda `0.00`.

Os defeitos são identificáveis e o código é curto e legível, por isso `com problemas` e não `ruim`.

## Testes

**Números:** 15 testes rodaram, 0 falharam, 0 pulados; 15 métodos de teste em 3 arquivos; cobertura de 68 de 739 linhas (9,2%) e 21 de 210 ramos (10,0%). Por pacote: `util` 51/55 linhas, `domain` 17/84, e `api`, `service`, `repository`, `database` com 0 (`measures/tests.md`). Nenhum teste pela API, nenhum contra o banco, nenhum com mock. `mvn verify` passa (`measures/tests-maven.log:345`).
**Nota:** ruim

Os testes que existem são só de utilitários. Conferem com valores: os formatos de dinheiro e data em PT e EN e "1 diária"/"1 night" (`src/test/java/com/posadamareaita/util/FormattingUtilTest.java:12-49`), os limites da temporada (`SeasonUtilTest.java:11-42`) e o preço fora de temporada com desconto (`PriceCalculatorTest.java:14-57`). Mas dois dos três testes de preço recomputam o esperado com a mesma fórmula do código (`PriceCalculatorTest.java:34-36` multiplica 249,90 × 1,20 e arredonda com o próprio `MoneyUtil`; `:50-51` faz o mesmo com 0,15), em vez de afirmar 299,88 / 599,76 do texto: se alguém trocasse `HALF_EVEN` por `HALF_UP` em `MoneyUtil`, nenhum teste falharia (o único com valor literal, `:22-24`, não tem arredondamento). Pergunta da mutação na parte nova: trocar 0,05 por 0,15 no ISS (`DocumentService.java:38`), apagar a linha "Empresa:" da fatura, ou aceitar CPF e CNPJ juntos (`ReservationService.java:27`): nenhum teste falha, porque nada em `service`, `api` ou `repository` é executado. Ficam sem rede: as situações da reserva, o cancelamento com devolução, os códigos de erro, o e-mail, os relatórios, os três documentos, a identificação e o JSON. Um único teste pela API teria revelado que o sistema não responde JSON. É a coluna quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; 6 colunas de dinheiro em `numeric(10,2)`; datas em `date`; 1 chave estrangeira (`reservations.room_number → rooms`) sem índice; 0 CHECK, 0 UNIQUE/EXCLUDE; 0 transações explícitas, 0 `FOR UPDATE`; 0 SQL montado com dado de fora; esquema criado por `CREATE TABLE` no código, sem migração (`measures/database.md`).
**Nota:** com problemas

O que está certo: tipos (`database/Database.java:30-72`), pool HikariCP (`:13-19`), SQL sempre com `?` e `try-with-resources` em todos os repositórios (`repository/ReservationRepository.java:28-55`, `MessageRepository.java:18-30`), os dados ficam no banco (as mensagens continuaram lá depois do reinício, `measures/behavior.md`). Os defeitos:

- Nada impede duas reservas do mesmo quarto na mesma noite: a disponibilidade é um `SELECT` (`repository/ReservationRepository.java:87-105`) seguido de um `INSERT` em outra conexão do pool (`:19-56`), sem transação, sem trava e sem restrição de exclusão no banco; nem um `synchronized` no processo. Com dois pedidos ao mesmo tempo, os dois passam. O roteiro não conseguiu provar isso porque as duas chamadas deram 500 pelo mapper ausente, mas o código não tem proteção nenhuma; é o risco de dado errado mais sério da coluna.
- O esquema é `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:24-77`): a próxima coluna numa tabela que já existe não vai entrar.
- Chave estrangeira sem índice (`reservations.room_number`); `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK; não há CHECK para `check_out > check_in`, `guests >= 1` nem para "exatamente uma identificação".
- O `update` grava só `status`, `amount_paid` e `refund` (`repository/ReservationRepository.java:71-85`), o que é suficiente hoje, mas usa o objeto inteiro sem versão: duas confirmações simultâneas gravariam as duas.
- O filtro por mês usa `EXTRACT(YEAR ...)`/`EXTRACT(MONTH ...)` (`:109-112`), que não usa índice; não pesa numa pousada de cinco quartos.

## Comportamento

**Números:** 6 de 83 conferências passaram (`measures/behavior.md`): parte comum 4 de 46; dados depois de reiniciar 1 de 3; cenário 05 0 de 32; duas reservas ao mesmo tempo 1 de 2 (e esse "OK" é vazio: nenhuma rodada criou reserva alguma, as duas deram 500 em todas as 10).
**Nota:** ruim

O serviço sobe e cria o esquema, mas toda rota que responde JSON devolve 500: `GET /rooms`, `POST /reservations`, `GET /reservations/{id}`, confirmação, cancelamento, check-in, check-out, segunda via, `GET /messages` (este devolveu `[]` só porque a lista estava vazia e o mapper não é chamado, daí o único "OK" do reinício). A causa é uma só: o `pom.xml` não tem `jackson-databind` e o Javalin 6.7.0 não traz um mapper por padrão (`measures/behavior-service.log:25-36`; jar sem classe `com.fasterxml.jackson`). Até as recusas (400/404/409/422) viram 500, porque o `catch` tenta responder com `ctx.json` de novo (`api/Api.java:109`). Só os dois CSVs responderam 200 com o cabeçalho certo e `total,0/5` / `total,,,,0.00`, sem reserva nenhuma para listar. Para o dono da pousada: o site não lista quartos, não cria reserva, não confirma, não manda e-mail, não faz check-in nem check-out, não emite nenhum dos três documentos e os relatórios saem vazios. Nada do cenário 05 (fatura com ISS, booking receipt, recusas de identificação) pôde ser observado pela API.

Defeitos que apareceriam mesmo com o mapper no lugar, pela leitura do código: a confirmação exige `amountPaid` no corpo e responde 400 sem ele (`api/Api.java:135-138`), enquanto o texto diz que o pago é o total da reserva, logo `POST /reservations/{id}/confirmation` sem corpo, como o site chama, seria recusado e travaria todo o fluxo; o check-out devolve a reserva e não o comprovante (`api/Api.java:218`); a segunda via de reserva pendente sairia 200 em vez de `409 OPERATION_NOT_ALLOWED` (`service/ReservationService.java:176-178` não olha a situação); CNPJ sem razão social e passaporte sem país seriam aceitos (`ReservationService.java:24-29`); a linha de total do financeiro sai `total,,,,599.76` e não `total,599.76` (`service/ReportService.java:63`), um ponto que o texto deixava menos fechado que o do ocupação. O `title` do documento é sempre "Comprovante de reserva" (`DocumentService.java:89`), também para fatura e booking receipt; o texto só fixa o título do comprovante, então isso fica em aberto.

## Dúvidas

- O `title` do documento para fatura e booking receipt: o texto só dá "Comprovante de reserva"; o roteiro esperava "Fatura de hospedagem" e "Booking receipt", e o código manda sempre "Comprovante de reserva" (`DocumentService.java:89`). Não pesou na nota, já que nada chegou a responder.
- A linha de total do financeiro (`total,,,,599.76` contra `total,599.76`): o texto diz só "uma linha total com a soma"; conto como divergência leve.
- A proteção contra reserva dupla não pôde ser medida pelo roteiro (as duas chamadas deram 500); a nota do banco se apoia na leitura do código, que não tem transação, trava nem restrição.
