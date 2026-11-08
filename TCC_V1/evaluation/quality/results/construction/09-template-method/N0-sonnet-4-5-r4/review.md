# Revisão: construção, cenário 09

## Visão geral

O agente montou o projeto em camadas por pacote: `handler` (Javalin), `service` (regras, sem Javalin e sem SQL),
`repository` (JDBC puro com `?`), `model`, `dto` e `database` (cria as tabelas e põe os quartos). Tudo é montado à mão
em `App.java:27-69`. Uma requisição entra no handler, que valida e chama o serviço; o serviço lê e grava pelos
repositórios. Os dados ficam no PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código
(`database/DatabaseManager.java:27-64`). A camada de regras está limpa e o dinheiro está em `BigDecimal`/`numeric(10,2)`.
Os problemas mais sérios: (1) duas reservas simultâneas do mesmo quarto foram criadas as duas em 10 de 10 rodadas,
porque nem o banco nem o código impedem a reserva dupla; (2) a segunda via sai para reserva pendente ou cancelada
(`handler/ReservationHandler.java:179-199`); (3) o Template Method pedido não existe: os quatro relatórios estão escritos
inteiros, cada um com a própria linha de título (`service/ReportService.java:44,79,115,157`); (4) quase não há testes
(3 testes, 1,5% de cobertura de linhas, nenhum sobre relatórios).

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `handler`
e na raiz (`App`), JDBC só em `repository` e `database`; `service` não usa nenhuma das duas. A maior classe é
`ReservationRepository` (260 linhas), sem nenhuma classe muito maior que as outras.
**Nota:** com problemas

O desenho geral está certo: as dependências vão de `handler` para `service` e depois para `repository`, as regras não
conhecem `Context` nem SQL, e a montagem fica num lugar só (`App.java:27-44`). Mas a borda HTTP passa por cima da camada
de regras em pontos localizados:

- `ReservationHandler` recebe os repositórios além do serviço (`handler/ReservationHandler.java:19-20`, montado em
  `App.java:41-42`). `getReservation` e `getDocument` leem direto do repositório (`handler/ReservationHandler.java:89,182`)
  e montam o comprovante no handler, com a busca do quarto (`handler/ReservationHandler.java:185-199`). Por isso a regra
  "a segunda via só existe para reserva paga" ficou de fora: o serviço tem a checagem em
  `service/ReservationService.java:171`, mas o caminho da segunda via não passa por ela.
- O comprovante tem dois donos: o texto é montado no serviço (`service/ReservationService.java:170-198`), e o JSON é
  copiado duas vezes no handler (`handler/ReservationHandler.java:154-166` e `187-199`).
- `RoomHandler` e `MessageHandler` falam direto com o repositório (`handler/RoomHandler.java:19`,
  `handler/MessageHandler.java:19`). Isso é aceitável para leituras simples.
- As exceções de domínio são classes internas de `ReservationService` (`service/ReservationService.java:224-243`), e o
  handler de reservas as usa para o próprio controle de fluxo (`handler/ReservationHandler.java:90`).

São misturas localizadas, sem ciclo: com problemas.

## Design de baixo nível

**Números:** 4 relatórios, 0 na estrutura. Os quatro são métodos inteiros de `ReportService`
(`service/ReportService.java:32,67,105,142`), cada um com a linha de título, o cabeçalho, o laço das linhas e o total
escritos à mão. A linha "Pousada Maré Alta,<nome>,<período>" aparece copiada 4 vezes (`:44`, `:79`, `:115`, `:157`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto anuncia mais relatórios (cancelamentos, hóspedes por estado) e uma mudança que tem de valer para
todos de uma vez (a linha com a data no fim). Pede um esqueleto único (título, cabeçalho, linhas, total) e passos por
relatório. Não há classe abstrata nem escritor genérico que receba as partes que variam: cada relatório repete a
sequência inteira com `StringBuilder`. A linha da data pedida para o ano que vem teria de ser copiada em quatro lugares
(depois do total em `:62`, `:100`, `:137`, `:185`), e cada relatório novo seria mais um método inteiro. A única coisa
compartilhada é `formatCsvNumber` (`:190`), uma função utilitária e não o esqueleto. Pela referência, isso é omissão.
A taxa de turismo também ficou numa constante (`service/ReportService.java:17`), embora o texto diga que o valor muda
todo ano. Isso é um detalhe e não muda a classe.

**Exagero.** Não há interfaces de uma implementação só, fábricas nem camadas que só repassam. A estrutura é a mínima:
contenção.

**O resto do código.**
- Duplicação nos relatórios. O filtro "reservas que deixaram dinheiro" está copiado (`service/ReportService.java:70-76` e
  `:145-151`), e o cálculo "pago menos devolvido" também (`:85-88` e `:169-172`). Se a regra do financeiro mudar, o
  relatório por plano, que o texto diz contar as mesmas reservas, pode ficar diferente sem ninguém notar.
- Duplicação nos handlers. A validação do mês está copiada três vezes (`handler/ReportHandler.java:43-56`, `67-80`,
  `91-104`), e o JSON do comprovante duas vezes (`handler/ReservationHandler.java:154-166` e `187-199`).
  `handler/ReservationHandler.java:150` gera o texto do comprovante e o joga fora (variável `receipt` sem uso).
- Erros escondidos. `catch (Exception e)` transforma qualquer falha, inclusive do banco, em `400 INVALID_REQUEST`
  (`handler/ReportHandler.java:38-40`, idem `:62`, `:86`, `:110`; `handler/ReservationHandler.java:81-83`). Um banco fora
  do ar vira "pedido inválido". O `catch (OperationNotAllowedException)` de `getDocument`
  (`handler/ReservationHandler.java:207`) é código morto, porque nada ali lança essa exceção. É o sintoma do defeito
  da segunda via.
- Tipos. `BigDecimal` com `HALF_EVEN` a cada passo (`service/PricingService.java:37-38`,
  `service/ReservationService.java:79-89`), `LocalDate` e `enum` para quarto, plano e situação estão certos. Mas o
  dinheiro sai para o JSON como `double` (`handler/ReservationHandler.java:224-229`, `handler/RoomHandler.java:26`),
  e por isso o valor perde as 2 casas que o texto pede (`0.0`, `249.9`).
- Regra repetida. O desconto do plano está no `enum` (`model/RatePlan.java:6-7`), mas o serviço ainda pergunta
  `== NON_REFUNDABLE` antes de usá-lo (`service/ReservationService.java:83-84`). A devolução também pergunta o plano
  (`:138`).
- Estado. `Reservation` é uma bolsa de setters públicos (`model/Reservation.java`), e qualquer um muda a situação sem
  passar pelas transições, que estão espalhadas pelo serviço (`service/ReservationService.java:96-167`).

São defeitos reais, mas localizados nos relatórios e nos handlers. O núcleo de preço e de situações é legível.

## Testes

**Números:** 3 testes rodaram, 0 falhas, 0 erros, 0 pulados; 3 métodos de teste escritos, todos em
`PricingServiceTest`. Cobertura: linhas 14 de 911 (1,5%), ramos 11 de 152 (7,2%). Os pacotes `handler`,
`repository`, `database` e `model` estão em 0%, e `service` em 14 de 256 linhas (`measures/tests.md`). Nenhum teste
pela API nem contra o banco.
**Nota:** ruim

O que existe confere valores. A alta temporada nas bordas (14/12 fora, 15/12 dentro, 30/06 e 01/08 fora) está em
`src/test/java/com/pousadamarealta/PricingServiceTest.java:17-27`, a noite de R$ 299,88 em `:35-37` e o "meio para o
par" em `:42-45`. Só que isso cobre uma única classe de 40 linhas. Nada testa:
- a soma das noites, o desconto de 15% e o total (`service/ReservationService.java:66-90`);
- as transições de situação, o cancelamento e a devolução;
- as recusas e os códigos de erro;
- o comprovante, o e-mail e o conflito de datas;
- e, sobretudo, nenhum dos quatro relatórios, a parte que o cenário pede.

A pergunta da mutação: trocar `4.50` por `5.40` (`service/ReportService.java:17`), tirar a NON_REFUNDABLE cancelada do
filtro do relatório por plano (`:149-150`) ou apagar a linha de título (`:157`) não derrubaria nenhum teste. A
coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 5 colunas de dinheiro estão
em `numeric(10,2)` e as datas em `date`. O script acusou: 1 chave estrangeira sem índice (`reservations.room_number` ->
`rooms`), 4 colunas de situação ou tipo sem `CHECK`, nenhuma restrição `UNIQUE`/`EXCLUDE`/`CHECK`, nenhum pool, 0
transações explícitas, 0 `FOR UPDATE`, nenhum SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

- O que está certo. Tipos e chaves estão no lugar (`database/DatabaseManager.java:34-54`), há `NOT NULL` em tudo o
  que é obrigatório, todo SQL usa `?` (por exemplo `repository/ReservationRepository.java:139-176`), as conexões
  voltam por try-with-resources, e os dados sobrevivem ao reinício (3 de 3 no roteiro do comportamento).
- O esquema não evolui. `CREATE TABLE IF NOT EXISTS` dentro do código (`database/DatabaseManager.java:27-64`), sem
  migrações. Uma coluna nova no próximo pedido não chega a um banco que já existe.
- Nada protege a regra "um quarto, uma reserva por noite". A checagem é ler e depois gravar, em duas conexões
  separadas, sem transação nem trava (`service/ReservationService.java:46-49` e `:63`). Não há restrição `EXCLUDE`
  (nem um índice) que o banco possa fazer valer. O efeito aparece no comportamento: 10 de 10 rodadas com reserva dupla.
- A confirmação grava a reserva e a mensagem em duas conexões sem transação (`service/ReservationService.java:102-104`).
  Uma falha no meio deixa a reserva paga sem o e-mail registrado.
- Cada chamada abre uma conexão nova com `DriverManager.getConnection` (`database/DatabaseManager.java:19-20`), sem pool.
- Carga. O relatório de ocupação faz uma consulta por quarto (`service/ReportService.java:36-41`), e o filtro do mês
  usa `TO_CHAR(check_in, 'YYYY-MM') = ?` (`repository/ReservationRepository.java:216`), que não aproveita índice.
  Não pesa com cinco quartos.

## Comportamento

**Números:** 59 de 64 conferências passaram (`measures/behavior.md`): a parte comum 42 de 46, os dados depois de
reiniciar 3 de 3, o cenário 09 (relatórios) 13 de 13, as reservas simultâneas 1 de 2.
**Nota:** com problemas

O cenário funciona inteiro. Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>", a taxa de turismo
dá os valores certos (inclusive o mês vazio com `0.00`), e o relatório por plano lista STANDARD e NON_REFUNDABLE mesmo
sem reserva e conta a NON_REFUNDABLE cancelada. Preço, temporada, arredondamento, recusas, e-mail, check-in,
check-out, devoluções e os dois relatórios originais passam. As falhas:

- **Reservas simultâneas:** em 10 de 10 rodadas as duas reservas do mesmo quarto e das mesmas noites foram criadas
  (201 e 201). Para o dono, se dois hóspedes reservarem o mesmo quarto pelo site quase ao mesmo tempo, os dois ficam
  com a reserva. É o defeito de banco descrito acima, e é o que mais pesa aqui.
- **Segunda via de reserva pendente:** respondeu 200 com o comprovante, quando devia ser 409 OPERATION_NOT_ALLOWED.
  Conferi à mão que o mesmo vale para uma reserva cancelada (200). A recepção pode entregar um comprovante de algo que
  não foi pago (`handler/ReservationHandler.java:179-199` não olha a situação).
- **Dinheiro com 2 casas no JSON:** veio `"amountPaid":0.0` e `"discount":0.0`, e em `/rooms` `"nightlyRate":389.9`. O
  texto pede número com 2 casas (599.76). Vem do `doubleValue()` nos handlers.
- **"o 201 comporta 3" e "a diária do 201 é 389.90":** o roteiro procura `number == 201` como número, e o projeto
  devolve `"number":"201"` como texto. Conferi com `curl`: o 201 tem `capacity` 3 e `nightlyRate` 389.9. O texto não
  diz o tipo do número do quarto, e texto é uma escolha razoável, então a falha da capacidade não pesa. A da diária
  pesa só pela falta das 2 casas, já contada acima.

A parte comum funciona no essencial e o cenário passa todo. Pesam a reserva dupla, a segunda via sem guarda e o
formato do dinheiro: com problemas.

## Dúvidas

- Banco ou comportamento: a ausência total de proteção contra a reserva dupla (nem trava, nem transação, nem
  restrição) está perto de "risco de dado errado", que seria `ruim`. Pesei o efeito no comportamento, onde a skill
  põe "só as reservas simultâneas falham" em `com problemas`, e deixei o banco em `com problemas` pelos critérios
  explícitos da skill: tipos, chaves e SQL certos, dados que sobrevivem.
- Design: a nota `com problemas`, e não `ruim`, depende de ver a duplicação como localizada (relatórios e handlers).
  Um revisor mais rigoroso pode somar a cópia dos relatórios, o `catch (Exception)` e o dinheiro em `double` e
  chegar a `ruim`.
