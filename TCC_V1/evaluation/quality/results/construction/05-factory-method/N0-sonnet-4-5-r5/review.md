# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto Maven com 7 pacotes. Em `br.com.maralta` fica o `Main`, que monta as peças e registra as rotas do Javalin. `dto` tem os objetos de entrada e saída, `service` tem as regras (preço, reserva, documento, e-mail, relatórios), `repository` tem o JDBC puro, `model` tem os objetos de dados e os enums, e `util` tem o dinheiro, a temporada e a criação do esquema. Uma requisição entra em `Main.java:68`, vira `Reservation` pelo DTO, passa por `ReservationService` e chega a `ReservationRepository`, que grava no PostgreSQL. As tabelas (`rooms`, `reservations` e `messages`) são criadas com `CREATE TABLE IF NOT EXISTS` em `util/DatabaseInitializer.java`.

O mais sério é que nenhuma reserva pode ser criada. A consulta de conflito em `repository/ReservationRepository.java:134` usa `(? IS NULL OR id != ?)` e passa `null` com `setObject` (`:145-146`). O PostgreSQL recusa a consulta ("could not determine data type of parameter $4", 25 vezes em `measures/behavior-service.log`). O tratamento genérico em `Main.java:57-61` transforma essa falha em `400 INVALID_REQUEST`, e por isso `POST /reservations` sempre responde 400. Sem reserva, nada do resto funciona: confirmação, e-mail, documentos, check-in e check-out, cancelamento e relatórios. O único teste que exercitaria esse caminho (`ReservationIT`) está desligado no `pom.xml:136`.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin e Jackson aparecem só em `br.com.maralta` (o `Main`). JDBC aparece só em `repository` e em `util` (o `DatabaseInitializer`). `service` não usa Javalin nem JDBC. A maior classe é `ReservationRepository` (252 linhas); depois vêm `Main` (191) e `Reservation` (182).
**Nota:** bom

- **Camadas:** as dependências vão da borda para as regras e das regras para os dados. Os handlers são finos: leem o corpo ou o parâmetro, chamam o serviço e escrevem a resposta (`Main.java:68-111`). As regras de preço, de situação da reserva e de devolução ficam fora do HTTP, em `service/PricingService.java`, `service/ReservationService.java:56-112` e `model/Reservation.java:156-176`.
- **Dados:** o SQL fica isolado nos três repositórios.
- **Montagem:** a montagem acontece num lugar só (`Main.java:27-40`), com os serviços recebendo as dependências pelo construtor.

Ressalvas menores, que não tiram a nota:

- `GET /messages` vai direto ao repositório, sem passar por um serviço (`Main.java:113-116`).
- A checagem de campos obrigatórios mora no `Main` (`Main.java:168-174`), enquanto o resto da validação está no serviço (`ReservationService.java:125-138`).
- A criação do esquema ficou em `util` (`util/DatabaseInitializer.java`) e não junto do acesso a dados.

## Design de baixo nível

**Números:** um único ponto escolhe o documento: `DocumentService.generateDocument` (`service/DocumentService.java:16`), com um `if/else if` sobre o tipo de identificação (`:36-42`). Os 3 lugares que emitem o documento chamam esse ponto: confirmação/e-mail (`ReservationService.java:68`), check-out (`:96`) e segunda via (`:122`). Nenhum lugar decide o documento por conta própria.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento criar mora num lugar só, e os três pontos de saída pedem o documento a esse lugar. É a "fábrica concentrada" que a referência aceita como acerto. O e-mail recebe o documento pronto e só o embrulha (`service/EmailService.java:15-27`), sem perguntar pelo tipo.

O que afasta do ideal:

- **Não há um tipo de documento por caso.** Há uma classe `Document` com todos os campos (`model/Document.java`), e cada ramo só preenche `title` e `text` (`DocumentService.java:47`, `:70`, `:97`).
- **O tipo de hóspede também não existe como tipo.** Ele é deduzido de campos nulos em `GuestIdentification` (`model/GuestIdentification.java:30-50`).
- **Cadeia sem `else` final** (`DocumentService.java:36-42`): uma identificação que não caia em nenhum caso devolve um documento sem texto, em silêncio.
- **Custo do próximo caso:** a agência de turismo vai exigir um novo ramo e um novo método aqui, além de campos novos em `GuestIdentification`, `Document`, nos DTOs e no banco.

**Exagero:** nenhuma interface com uma implementação só, nenhuma fábrica ou camada que só repasse. Os construtores estáticos `GuestIdentification.withCpf/withCompany/withPassport` (`GuestIdentification.java:12-30`) só são usados nos testes, o que é pouca coisa.

**O resto do código (a nota):**

- **Erros escondidos:** `app.exception(Exception.class, ...)` responde `400 INVALID_REQUEST` para qualquer exceção, inclusive falha de banco (`Main.java:57-61`). Foi isso que mascarou o defeito do SQL como erro do cliente. Os repositórios também fazem `catch (Exception e)` e embrulham tudo em `RuntimeException` (`ReservationRepository.java:51, 75, 120, 154, 184, 214`).
- **Duplicação:** `generateReceipt` e `generateInvoice` repetem quase linha por linha o quarto, as datas, as diárias, o desconto e o total (`DocumentService.java:54-65` e `:80-91`). O `booking receipt` repete a mesma sequência com outros rótulos (`:105-116`).
- **Dinheiro no CSV:** `String.format("%.2f", ...)` depende do locale da JVM (`service/ReportService.java:62`, `:67`); num servidor em pt-BR o valor sai com vírgula e quebra o CSV.
- **Consultas repetidas:** o relatório de ocupação busca o quarto no banco a cada linha (`ReportService.java:35`).
- **Modelo anêmico:** `Reservation` tem setters públicos para tudo e muda de situação por `setStatus` vindo do serviço (`ReservationService.java:63`, `:81`, `:92`, `:106`). As checagens `canConfirm`/`canCancel` (`model/Reservation.java:156-170`) não impedem uma mudança que as ignore.

Os tipos de domínio estão certos: `BigDecimal` com `HALF_EVEN` (`util/MoneyUtils.java:9-11`), `LocalDate`, e enums para situação, plano e tipo de quarto.

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros (`measures/tests.md`). Há 25 métodos de teste escritos; os 8 de `ReservationIT` não rodam, porque o `pom.xml:136` tem `<skipITs>true</skipITs>` e eles dependem de Testcontainers/Docker (`ReservationIT.java:22-26`). A cobertura é de 27,0% das linhas (243 de 901) e 19,3% dos ramos (34 de 176). `repository`, `dto`, `exception` e o `Main` ficam em 0%.
**Nota:** com problemas

**O que os testes que rodam conferem, com valores:**

- **Preço:** a diária de 299,88 na alta temporada e o subtotal (`PricingServiceTest`).
- **Temporada:** as datas de corte (`SeasonUtilsTest`).
- **Documentos:**
  - comprovante: título, linha do CPF, datas, "2 diárias", "R$ 779,80";
  - fatura: empresa, hóspede e "ISS retido (5%): R$ 38,99" (`DocumentServiceTest.java:64`);
  - booking receipt: "2 nights" e "BRL 779.80" (`:89-90`);
  - desconto e "1 diária" (`:131`).

**A pergunta da mutação:**

- Trocar o ISS de 0.05 por outro valor faria um teste falhar.
- Ficariam sem rede:
  - o "1 night" do booking receipt;
  - a linha "Discount:" em inglês;
  - o separador de milhar `BRL 2,783.52`;
  - a regra de "exatamente uma identificação" (`GuestIdentification.java:37`, `count == 1`, sem teste nenhum).
- Também ficariam sem rede as transições de situação, as devoluções e os dois relatórios. Os testes que conferem transições e devoluções (`ReservationIT.java:150-215`) estão desligados.
- As asserções de texto usam só `contains` em pedaços. Um texto com linhas a mais ou fora de ordem passaria.

O mais grave é o efeito do teste desligado. `ReservationIT.java:67` chama `createReservation`, que quebraria com o mesmo erro de SQL que derruba o sistema. Ao desligar os testes de integração, o agente entregou um `mvn verify` verde sobre um sistema que não cria reservas. Nada exercita o banco nem a API.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (5 colunas ok) e as datas em `date`. As conferências acusaram (`measures/database.md`):

- uma chave estrangeira sem índice (`reservations.room_number -> rooms`);
- as colunas de situação, plano, tipo e canal sem `CHECK`;
- nenhuma restrição `UNIQUE`/`EXCLUDE`/`CHECK`;
- 0 transações explícitas e 0 `FOR UPDATE`;
- esquema criado por `CREATE TABLE` no código, sem migrações.

**Nota:** com problemas

**O que está certo:**

- tipos de dinheiro e de data;
- `NOT NULL` no que é obrigatório;
- a reserva ligada ao quarto por chave estrangeira (`util/DatabaseInitializer.java:42`);
- SQL sempre com `?` e conexões fechadas com try-with-resources.

**Os defeitos:**

- **Esquema que não evolui:** ele é criado com `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:13-54`). O próximo pedido que acrescente uma coluna (por exemplo, a da agência de turismo) não tem como mudar um banco que já existe.
- **Sem pool de conexões:** o script contou "1 arquivo com pool", mas é `PGSimpleDataSource` (`Main.java:155`), que abre uma conexão nova a cada chamada.
- **Reserva dupla desprotegida:** nada impede duas reservas do mesmo quarto na mesma noite. A checagem de conflito (`ReservationService.java:43`) e o `INSERT` (`:48`) não ficam numa transação, sem trava e sem restrição `EXCLUDE`. Nem há um `synchronized`.
- **Relatório financeiro:** a consulta inclui reservas com `refund > 0` (`ReservationRepository.java:196`). Uma reserva STANDARD cancelada com devolução total entra no relatório com 0,00, embora não tenha deixado dinheiro com a pousada.

A consulta de conflito quebrada (`ReservationRepository.java:134`, `:145-146`) é código que fala com o banco. Como ela aparece na chamada `POST /reservations`, o peso dela vai para a coluna de comportamento.

## Comportamento

**Números:** 20 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 14 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário 05 | 4 de 32 |
| duas reservas ao mesmo tempo | 1 de 2 (as 10 rodadas deram `400 400`) |

**Nota:** ruim

**Por que tudo falha.** O serviço sobe, cria as tabelas e lista os quartos, mas `POST /reservations` responde sempre `400 {"error":"INVALID_REQUEST","message":"Failed to check room availability"}`. Conferi com `curl`, com o serviço de pé. A causa é a consulta `(? IS NULL OR id != ?)` com `null` sem tipo (`ReservationRepository.java:134`, `:145-146`), que o PostgreSQL recusa (`measures/behavior-service.log`). Daí vêm em cascata quase todas as falhas: confirmação e e-mail, segunda via, check-in, check-out, cancelamentos e devoluções, os dois relatórios vazios (`total,0/5` e `total,,,,,0.00`), a persistência depois de reiniciar e as reservas simultâneas.

**O cenário 05.** Os três documentos não chegam a ser emitidos. A fatura, o ISS de R$ 38,99, o booking receipt em inglês e o documento no e-mail não foram verificáveis pela API. Só passam as recusas de identificação inválida (nenhuma, CPF+CNPJ, CNPJ sem razão social, passaporte sem país), porque elas acontecem antes da consulta quebrada (`ReservationService.java:134-136`).

**O que passa.** `GET /rooms`, a capacidade excedida (422), o quarto inexistente (404), as datas e os hóspedes inválidos (400), a reserva inexistente (404) e o mês fora do formato.

**Falhas que não pesam:**

- "o 201 comporta 3" e "a diária do 201" falham porque o roteiro procura `number` numérico. O sistema devolve `"number":"201"` como texto, com capacidade 3 e diária 389.90 corretas. É uma escolha razoável num ponto que o texto não fechou.
- A linha final do financeiro sai como `total,,,,,X` (`ReportService.java:67`), enquanto o roteiro espera `total,X`. O texto só dá o exemplo da ocupação (`total,2/5`), então também é um ponto em aberto.

**Para o dono da pousada:** o site não consegue fazer nenhuma reserva. Hoje o sistema serve só para listar os quartos.

## Dúvidas

- **Classe do pattern:** dei `acerto` porque a escolha do documento está concentrada num método só (`DocumentService.generateDocument`), chamado pelos três lugares. É a forma "fábrica concentrada" que a referência aceita, e não o Factory Method clássico nem um método polimórfico no tipo de hóspede. Por dentro, porém, é uma cadeia `if/else` sobre campos nulos, sem um tipo de documento nem um tipo de hóspede. Um revisor mais rigoroso poderia ler isso como estrutura fraca, mas não é a omissão definida na referência, porque o `if` não se repete nos três lugares.
- **Documentos sem verificação pela API:** como nenhuma reserva é criada, o texto real dos documentos pela API (inclusive o separador de milhar `BRL 2,783.52`) e o filtro do relatório financeiro só foram avaliados pela leitura do código e pelos testes de unidade, não pela API.
