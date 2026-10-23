# Revisão: construção, cenário 05

## Visão geral

O projeto (`projeto/`, cerca de 1.400 linhas em 7 pacotes) tem as camadas no papel: `api` (uma classe `Api` com todas as rotas do Javalin, `api/Api.java:28-40`), `service` (reservas, documentos, relatórios, mensagens), `repository` (JDBC com `PreparedStatement` sobre um pool Hikari), `domain` (objetos de dados e enums) e `util` (preço, temporada, formatação). Tudo é `static`: não há montagem, cada classe chama a outra direto (`Main.java:21`, `repository/ReservationRepository.java:28`). Os dados ficam no PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:29-73`). O problema mais sério é que **o sistema não funciona pela API**: o `pom.xml` traz o Gson (`pom.xml:31-35`) e não traz o Jackson, que o `ctx.json(...)` do Javalin 6 exige. Por isso toda resposta JSON, incluindo as de erro, vira 500 ("It looks like you don't have an object mapper configured", `measures/behavior-service.log:25`). O roteiro passou em 6 de 83 conferências. O pior efeito: a reserva é gravada no banco e o site recebe 500, sem saber o número dela. Numa prova manual, dois POSTs iguais devolveram 500 e 500, e a tabela ficou com a reserva 1 PENDING. Mesmo sem esse defeito, o código erra regras do texto. O check-out devolve a reserva, e não o documento (`api/Api.java:216`). A confirmação exige um `amountPaid` no corpo (`api/Api.java:135-140`). O relatório financeiro deixa de fora a NON_REFUNDABLE cancelada (`repository/ReservationRepository.java:112`). O dia 29 de fevereiro não conta como alta temporada (`util/SeasonUtil.java:23`). A identificação errada vira 409 ROOM_UNAVAILABLE. A escolha do documento por tipo de hóspede é um `if` copiado em dois lugares, sem fábrica. Os testes cobrem só os utilitários (9,2% das linhas).

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `com.posadamareaita` e `api`, e o JDBC só em `database` e `repository`. Maior classe: `Api.java` com 312 linhas, seguida de `ReservationService.java` com 189.
**Nota:** com problemas

As dependências apontam no sentido certo (api → service → repository → database) e as regras não conhecem o Javalin nem o SQL. Os problemas estão na borda e na montagem:

- **A borda HTTP reproduz a regra.** O serviço devolve `Optional.empty()` para seis motivos diferentes (`service/ReservationService.java:28,33,38,44,49,55`). O handler tenta adivinhar qual foi e repete as conferências de capacidade, hóspedes e datas (`api/Api.java:93-105`). Como esse diagnóstico não conhece a regra de identificação, uma identificação errada cai no `else` e vira 409 ROOM_UNAVAILABLE (`api/Api.java:102-103`). O mesmo jeito de adivinhar aparece em todas as transições (`api/Api.java:143-149, 164-170, 185-191, 206-212`).
- **A borda vai direto ao acesso a dados.** Ela chama `RoomRepository.findAll()` (`api/Api.java:43`), `ReservationRepository.findById` (`api/Api.java:117,144`) e `MessageRepository.findAll()` (`api/Api.java:246`) sem passar por um serviço. O pacote `service` também está fino: `ReservationService.getDocument` só repassa o `findById` (`service/ReservationService.java:176-178`).
- **Não há montagem.** Todas as classes são utilitários `static`. O `DataSource` é um `static` global, preenchido por `Database.initialize` (`database/Database.java:11,19`) e lido por cada repositório (`repository/ReservationRepository.java:28`, `repository/RoomRepository.java:18`). Nada pode ser trocado nem testado em isolamento.
- **`Api` é a classe grande:** onze handlers, a validação, o parse do JSON (Gson) e o mapeamento das respostas em `HashMap` (`api/Api.java:288-311`). O próximo pedido vai mexer nela.

## Design de baixo nível

**Números:** três tipos de documento (comprovante, fatura, booking receipt), em três métodos de texto (`service/DocumentService.java:12, 37, 62`). A decisão de qual documento criar está copiada em 2 lugares (`service/DocumentService.java:107-113` e `service/ReservationService.java:95-101`). O terceiro lugar, o check-out, não produz documento nenhum (`service/ReservationService.java:127-142`, `api/Api.java:216`). Nenhuma fábrica, nenhum tipo de hóspede.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede que a criação do documento fique num lugar só, porque ele sai em três lugares e as agências de turismo vão pedir um quarto tipo. O agente escreveu três funções de texto. A escolha entre elas é um `if/else if/else` sobre `getGuestCpf() != null` / `getCompanyCnpj() != null`, copiado na segunda via (`service/DocumentService.java:107-113`) e na confirmação (`service/ReservationService.java:95-101`). O check-out não chama nenhum dos dois. A referência chama isso de omissão: a decisão de qual criar se repete em cada lugar, mesmo com o texto de cada documento em funções separadas. Para a agência, seria preciso mexer em pelo menos dois `if`, no `DocumentService` e na regra de identificação (`service/ReservationService.java:24-29`). O tipo de hóspede também não existe como tipo: são cinco `String` soltas na `Reservation` (`domain/Reservation.java:19-23`), e o tipo é deduzido por qual delas não é null. O resultado sai fixo em `title = "Comprovante de reserva"` até para a fatura e o booking receipt (`service/DocumentService.java:89`).

**Exagero.** Não há estrutura sem necessidade. O `MessageService` só repassa ao repositório (`service/MessageService.java:7-10`), mas é uma classe pequena e não pesa.

**O resto do código:**
- **Erros engolidos.** Todo handler termina em `catch (Exception e)`, que joga fora a causa e responde um código fixo: 400 na criação e na confirmação (`api/Api.java:109-111, 154-156`), 404 RESERVATION_NOT_FOUND nas outras (`api/Api.java:125-127, 175-177, 196-198, 217-219, 240-242`). Uma falha de banco vira "reserva não encontrada". Foi isso que escondeu o erro do Jackson: o próprio `catch` chama `ctx.json` e falha de novo.
- **`Optional.empty()` para tudo.** Os motivos da recusa se perdem (ver Arquitetura). Exceções de domínio ou um resultado com o motivo evitariam a cópia da regra no handler.
- **Duplicação.** As linhas Quarto/Entrada/Diárias/Desconto/Total aparecem copiadas no comprovante e na fatura (`service/DocumentService.java:22-32` e `46-56`), e de novo, traduzidas, no booking receipt (`72-82`). O número de noites é calculado duas vezes (`service/ReservationService.java:65` e `util/PriceCalculator.java:26`). O mapeamento da reserva para JSON é montado à mão num `HashMap` (`api/Api.java:288-311`), e o de quartos e mensagens usa a inicialização com chaves duplas, que cria classes anônimas (`api/Api.java:44-49, 247-253`).
- **O estado.** `Reservation` é um saco de setters (`domain/Reservation.java:58-76`), e as transições de estado moram no serviço como `setStatus` (`service/ReservationService.java:87, 121, 138, 169`), sem que a reserva proteja a própria regra. `PriceResult` tem campos públicos mutáveis (`util/PriceCalculator.java:13-15`).
- **Os tipos.** `BigDecimal` com `HALF_EVEN` está certo para o dinheiro (`util/MoneyUtil.java:7-9`), e `LocalDate` para as datas. Mas o plano chega como `String` ao serviço e é convertido duas vezes (`api/Api.java:75`, `service/ReservationService.java:59`). O canal é texto solto, `"EMAIL"` (`service/MessageService.java:8`). `RoomType.displayName` repete o nome do enum (`domain/RoomType.java:4-6`). O CSV formata dinheiro com `String.format("%.2f")` no locale padrão da JVM (`service/ReportService.java:58,63`); num locale pt-BR sairia `599,76`.
- **Uma regra errada.** A alta temporada vai "até o fim de fevereiro", mas o código para no dia 28 (`util/SeasonUtil.java:23`): a noite de 29/02/2028 sai sem os 20%.

## Testes

**Números:** 15 testes rodaram, 0 falhas, 0 erros, 0 pulados; 15 métodos escritos, em 3 classes, todas no pacote `util`. Cobertura: 68 de 739 linhas (9,2%) e 21 de 210 ramos (10,0%). `api`, `service`, `repository` e `database` têm 0% (`measures/tests.md`). Nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que existe confere valores. A formatação de dinheiro e datas em português e em inglês, com milhar (`util/FormattingUtilTest.java:12-19`), está coberta, assim como "1 diária"/"1 night" (`util/FormattingUtilTest.java:42-49`) e os limites da temporada (`util/SeasonUtilTest.java:19-22`). Mas o que o texto mais pede fica sem rede:

- **Nada da parte nova do cenário.** Não há teste do documento por tipo de hóspede, da fatura com o ISS de 5% (R$ 38,99), do booking receipt inteiro, da identificação "um, e só um" nem do e-mail. Se alguém trocasse `0.05` por `0.5` em `service/DocumentService.java:38`, ou o `!= 1` por `> 1` em `service/ReservationService.java:27`, nenhum teste falharia.
- **Nada da máquina de estados, do cancelamento com devolução, dos relatórios nem dos códigos de erro.**
- **O teste do desconto é tautológico.** O esperado é calculado com a mesma fórmula do código (`util/PriceCalculatorTest.java:50-55`), e o da alta temporada também (`util/PriceCalculatorTest.java:34-38`). Os valores do texto (599,76; 37,48; 212,42) não aparecem.
- **O teste da temporada confirma o defeito.** Ele afirma o dia 28 de fevereiro num ano que não é bissexto e nunca testa o dia 29 (`util/SeasonUtilTest.java:31-35`).
- Nenhum teste sobe o serviço. Um único teste pela API teria mostrado que toda resposta JSON dá 500.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Os 6 campos de dinheiro estão em `numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice (`reservations(room_number) -> rooms`), as colunas de situação, plano, tipo e canal sem CHECK, e nenhuma restrição UNIQUE, EXCLUDE ou CHECK. Esquema criado por `CREATE TABLE` no código, sem migrações. Nenhuma transação explícita nem `FOR UPDATE`; nenhum SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

Os tipos e as chaves estão certos (`database/Database.java:29-73`). Todo o SQL usa `?` (`repository/ReservationRepository.java:20-47, 87-104`), as conexões vêm de um pool Hikari e são devolvidas com try-with-resources (`database/Database.java:14-19`, `repository/ReservationRepository.java:28`). O serviço sobe contra um banco vazio e semeia os quartos (`database/Database.java:80-97`). Os defeitos:

- **Nada impede duas reservas do mesmo quarto na mesma noite.** O serviço consulta os conflitos (`service/ReservationService.java:53`) e insere depois (`service/ReservationService.java:70`), em conexões separadas, sem transação, sem trava e sem restrição de exclusão no banco. Dois pedidos simultâneos podem passar os dois pela consulta e gravar duas reservas sobrepostas. O roteiro não pôde provar isso: as 20 chamadas deram 500 pelo erro do Jackson.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`database/Database.java:29, 39, 65`) não acrescenta coluna numa tabela que já existe. O próximo pedido que mudar o banco não tem caminho.
- **O banco não protege as regras.** Não há CHECK de `guests >= 1`, de `check_out > check_in`, dos valores de `status`/`rate_plan`, nem de "exatamente uma identificação" (`database/Database.java:39-62`). A chave estrangeira `room_number` não tem índice. Também não há índice em `(room_number, check_in, check_out)`, que a consulta de disponibilidade usa (`repository/ReservationRepository.java:87-92`).
- **O relatório financeiro usa `EXTRACT(YEAR/MONTH FROM check_in)`** (`repository/ReservationRepository.java:110-111`). Isso impede usar índice; faria mais sentido um intervalo de datas. Nota menor.

## Comportamento

**Números:** 6 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 4 de 46;
- dados depois de reiniciar: 1 de 3;
- cenário 05: 0 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** ruim

O serviço sobe, mas **toda resposta JSON dá 500**. O Javalin 6 precisa do `jackson-databind` para o `ctx.json(...)`, e o `pom.xml` só traz o Gson (`pom.xml:31-35`). O log mostra a mensagem do Javalin pedindo o Jackson (`measures/behavior-service.log:25-30`). Falham, com 500:
- `GET /rooms`;
- a criação da reserva e todas as recusas: 400, 404, 409 e 422 também saem como 500, porque o `catch` chama `ctx.json` de novo;
- `GET /messages`.

Só os relatórios em CSV respondem (`ctx.result`), e vêm vazios, porque nenhuma reserva chegou a ser confirmada. Os 404 que o roteiro viu depois vêm do `catch` que transforma tudo em RESERVATION_NOT_FOUND (`api/Api.java:125-127`).

Para o dono da pousada: o site não consegue listar quartos nem fazer reservas. Pior, cada tentativa grava a reserva no banco e devolve erro. Numa prova manual, dois POSTs iguais do quarto 101 deram 500 e 500, e a tabela ficou com a reserva 1 PENDING de R$ 599,76. O hóspede tenta de novo e o quarto aparece ocupado por uma reserva que ninguém sabe que existe. Nenhuma das 32 conferências do cenário (comprovante, fatura, booking receipt, identificação) chegou a ser medida.

Mesmo com o Jackson no lugar, a leitura do código mostra outras falhas contra o texto:
- **O check-out devolve a reserva, e não o comprovante** (`api/Api.java:216`).
- **A confirmação exige `amountPaid` no corpo,** e sem ele responde 400 (`api/Api.java:135-138`). O texto diz que o pago é o total (`service/ReservationService.java:88`).
- **A segunda via sai para reserva pendente ou cancelada,** sem conferir a situação (`service/ReservationService.java:176-178`). O texto pede que ela exista só para reserva paga.
- **O financeiro deixa de fora a NON_REFUNDABLE cancelada** (`repository/ReservationRepository.java:112`), e o texto diz que ela continua contando.
- **A identificação errada vira 409 ROOM_UNAVAILABLE em vez de 400** (`api/Api.java:102-103`). Nenhuma, duas ou uma incompleta caem nesse erro. CNPJ sem razão social e passaporte sem país são aceitos (`service/ReservationService.java:24-29`).
- **O título do documento é sempre "Comprovante de reserva"** (`service/DocumentService.java:89`), e o `discount` vem `null` quando é zero (`service/DocumentService.java:103`).
- **O dinheiro no JSON perde as duas casas quando é zero.** `BigDecimal.ZERO` sai como `0`, não `0.00`, em `amountPaid` e `discount` (`domain/Reservation.java:42`, `util/PriceCalculator.java:37`).
- **A noite de 29 de fevereiro fica fora da alta temporada** (`util/SeasonUtil.java:23`).

Os textos dos três documentos e do e-mail, lidos no código (`service/DocumentService.java:12-85`, `service/ReservationService.java:180-188`), batem com o enunciado. Isso inclui o ISS com `HALF_EVEN` sobre o total e o "BRL 1,234.56", mas nenhum deles chega ao hóspede.

## Dúvidas

- Na classe do pattern, a escolha está entre `omissão` e `malfeito`. Não há fábrica nem método que concentre a escolha: o `if` está copiado em dois lugares e o terceiro, o check-out, nem gera documento. Dei `omissão`, pela regra da referência (a decisão de qual criar repetida onde o documento é pedido). Nenhuma das formas aceitas (Factory Method clássico, fábrica concentrada, método no tipo de hóspede) foi usada.
- As reservas simultâneas não puderam ser medidas: todas as chamadas deram 500. A leitura do código (consulta e depois inserção, sem transação nem restrição) indica que a duplicação é possível. Dei `com problemas` no banco pela régua da skill, que reserva o `ruim` para dinheiro em ponto flutuante, SQL montado com dado de fora ou dado perdido. Não provei a duplicação na prática.
- Não sei se o agente rodou o serviço alguma vez. O `mvn verify` passa porque nenhum teste sobe o Javalin.
