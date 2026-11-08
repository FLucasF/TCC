# Revisão: construção, cenário 09

## Visão geral

O projeto é um Javalin 6.7.0 com Gson como conversor de JSON, dividido em `handler` (rotas), `service` (regras e
relatórios), `dao` (SQL com JDBC puro), `database` (uma conexão `static` única que cria as tabelas e os quartos),
`model` (classes de campos públicos) e `util` (preço e formatação). Uma requisição entra num handler, que lê o corpo
com Gson, chama um método `static` do serviço (às vezes vai direto ao DAO) e escreve a resposta. Os dados ficam no
PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. O mais sério: **o sistema não
funciona.** Na subida, `ReservationHandler.register` quebra em `app.get("/reservations/:id", ...)`
(`src/main/java/com/marealata/handler/ReservationHandler.java:71`; o log diz `Path '/reservations/:id' invalid -
Javalin 4 switched from ':param' to '{param}'`), por isso nenhuma rota com `{id}` existe, e `MessageHandler` e
`ReportHandler` nunca chegam a ser registrados (`App.java:37-39`). Só sobram `GET /rooms` e `POST /reservations`, e
esta última sempre devolve 400: o Gson não sabe ler nem escrever `LocalDate` no Java 21 (conferi com o jar do
projeto: `JsonIOException: Failed making field 'java.time.LocalDate#year' accessible`), e o `catch (Exception)` em
`ReservationHandler.java:66` transforma o erro num `INVALID_REQUEST`. Resultado: não dá para criar uma única reserva,
e o comportamento passa em 11 de 64 conferências. Além disso, a lógica tem erros que apareceriam assim que o resto
fosse consertado: alta temporada em dezembro inteiro, o financeiro sem a não reembolsável cancelada, dinheiro sem
separador de milhar. Os quatro relatórios estão escritos cada um por inteiro, sem o esqueleto comum que o texto pede.

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O `handler` usa
Javalin e JDBC (`SQLException`) e depende de `dao`, `model` e `service`. Maior classe: `ReservationHandler`, com 202
linhas.
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo (handler → service → dao → database), sem ciclo. O SQL fica
só nos DAOs (`dao/ReservationDao.java`, `dao/RoomDao.java`, `dao/MessageDao.java`), e o serviço não conhece o
Javalin. Mas a borda não é fina:

- Os handlers pulam o serviço e vão direto ao DAO: `ReservationHandler.java:74`, `:149`, `:174`, `:187`,
  `MessageHandler.java:23`, `RoomHandler.java:13`.
- Há regra de negócio no handler. Quem decide se a segunda via existe é a rota (`ReservationHandler.java:181`), e a
  montagem do comprovante está copiada em duas rotas (`ReservationHandler.java:149-156` e `:187-194`).
- Não há montagem. Tudo é método `static`, e todo DAO pega uma conexão global `static`
  (`database/Database.java:6`, `:106`). Nada se troca nem se isola num teste, e cada peça liga as outras por nome de
  classe.
- O tratamento de erro (o mapa de código para status) está copiado em cada rota, em vez de ficar num
  `app.exception(...)` só (`ReservationHandler.java:57-62`, `:99-103`, `:116-120`, `:133-137`, `:160-164`).

Essa mistura está em poucos lugares e não há ciclo, por isso dou `com problemas` e não `ruim`.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum. Os quatro são métodos inteiros em `ReportService`, cada um
repetindo título, cabeçalho, laço de linhas e total (`service/ReportService.java:16-36`, `:38-68`, `:70-102`,
`:104-145`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede que o jeito dos relatórios mude "em todos de uma vez" e já avisa que vêm mais dois. A
resposta esperada era um esqueleto (título, cabeçalho, linhas, total) num lugar só, com cada relatório preenchendo os
passos que variam. Não há nada disso. Cada método monta o próprio `StringBuilder`, escreve a própria linha de título
com `POUSADA_NAME` (`ReportService.java:20`, `:45`, `:77`, `:130`) e o próprio total. O cálculo "pago menos
devolvido" está copiado (`:51-53` e `:116-118`), e o intervalo do mês também (`:39-40`, `:71-72`, `:105-106`). A
linha de data que o dono quer no ano que vem teria de ser posta à mão em quatro lugares, e em seis quando chegarem os
dois relatórios anunciados. Pela referência, isso é omissão.

**O exagero.** Não há interface, fábrica nem camada sobrando: contenção.

**O resto do código.** Os defeitos estão espalhados:

- **Tabela de preços duplicada e divergente.** As diárias estão no banco (`Database.java:72-100`) e de novo,
  fixas, em `util/PriceCalculator.java:8-24`. O preço ignora o banco e escolhe a diária por um `switch` no número do
  quarto. Trocar uma diária no banco não muda o preço cobrado.
- **Regra de temporada errada.** `PriceCalculator.java:26-35` marca dezembro inteiro como alta temporada (o texto
  diz a partir de 15/12). A variável `dayOfMonth` (`:28`) é calculada e nunca usada.
- **Formatação de dinheiro.** `formatCurrency` usa `String.format("%.2f")` sem separador de milhar
  (`util/DocumentFormatter.java:44`) e escreve "R$ 1234,56" em vez de "R$ 1.234,56". O `CURRENCY_FORMATTER` com
  locale pt-BR está declarado e nunca é usado (`:12-17`). O `%.2f` do CSV (`:48`) depende do locale da máquina.
- **Erros escondidos.** O `catch (Exception)` em `ReservationHandler.java:66` engoliu a falha do Gson com
  `LocalDate`. É o motivo de toda criação de reserva sair 400 sem nenhum rastro no log.
- **Estado aberto e tipos fracos.** Os modelos têm campos públicos mutáveis (`model/Reservation.java:9-54`), e o
  serviço altera o objeto por fora (`ReservationService.java:54-55`, `:109-120`). O tipo do quarto é `String`
  (`model/Room.java:10`). A exceção tem campos públicos que duplicam a mensagem (`ReservationException.java:4-5`).
  `ErrorResponse` está copiada em quatro handlers.
- **Regras contra o texto.** A confirmação exige `amountPaid` no corpo e guarda o valor que vier
  (`ReservationHandler.java:91`, `ReservationService.java:55`), mas o texto diz que o pago é o total. O total da
  ocupação tem "/5" fixo (`ReportService.java:33`). A segunda via barra só a pendente e deixa passar a cancelada
  (`ReservationHandler.java:181`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados. São 7 métodos escritos, em 2 classes, todos de `util`.
Cobertura de 7,2% das linhas (50 de 693) e 9,4% dos ramos (16 de 170). Os pacotes `handler`, `service`, `dao` e
`database` estão em 0% (`measures/tests.md`).
**Nota:** ruim

Só a formatação e o preço são testados. Nenhum teste toca o serviço, a API ou o banco, e por isso nenhum percebeu que
o sistema não sobe direito nem cria reserva.

O que existe é fraco:

- `PriceCalculatorTest.java:12-19` confere a temporada sem nenhuma data entre 1 e 14 de dezembro. É exatamente onde
  está o erro (`PriceCalculator.java:31`), e o teste passa com ele.
- `DocumentFormatterTest.java:13-14` confere moeda só abaixo de mil. Não pega a falta do separador de milhar.
- `DocumentFormatterTest.java:43-48` testa o comprovante só com `contains`, sem comparar o texto inteiro.
- `testRounding` (`PriceCalculatorTest.java:30-35`) arredonda um valor que já tem duas casas: não confere o "meio
  para o par".

A parte nova do cenário não tem teste nenhum: trocar 4.50 por 5.40 em `ReportService.java:14`, apagar a linha de
título de qualquer relatório ou tirar `NON_REFUNDABLE` da conta dos planos não faria nenhum teste falhar. Cancelamento,
devolução, situações da reserva e códigos de erro também ficaram sem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Todo o dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice
(`reservations.room_number`), nenhum CHECK nas colunas de situação, plano, tipo e canal, e nenhuma restrição UNIQUE ou
EXCLUDE. Há 1 `DriverManager.getConnection`, nenhum pool, nenhuma transação e nenhum `FOR UPDATE`
(`measures/database.md`).
**Nota:** com problemas

Os tipos estão certos: dinheiro em `NUMERIC(10,2)` e datas em `DATE` (`database/Database.java:25-63`). Há chave
estrangeira da reserva para o quarto (`:51`), e todo SQL usa parâmetros `?` (os DAOs, sem nenhuma concatenação).

Os defeitos:

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:22-65`), sem
  migração, e não sabe acrescentar uma coluna numa tabela que já existe.
- **Uma conexão para o servidor inteiro.** É uma só, `static`, sem pool (`Database.java:6`, `:17`), dividida entre
  todas as threads do Jetty, sem reconexão. Se ela cair, o serviço para de falar com o banco até ser reiniciado.
- **Nada impede duas reservas na mesma noite.** A disponibilidade é "consulta, depois insere"
  (`ReservationService.java:26-40`), sem transação, sem trava e sem restrição EXCLUDE no banco. Duas criações
  simultâneas podem passar as duas. O roteiro não chegou a ver isso porque nenhuma reserva é criada.
- **Faltam o índice e os CHECK.** `reservations.room_number` não tem índice, e as colunas `status`, `rate_plan`,
  `type` e `channel` são `VARCHAR` sem CHECK.
- **Consulta repetida por linha.** O relatório de ocupação busca o quarto uma vez para cada reserva
  (`ReportService.java:24`).

O serviço sobe contra um banco vazio e cria o que precisa, por isso dou `com problemas` e não `ruim`.

## Comportamento

**Números:** 11 de 64 conferências passaram (`measures/behavior.md`):

- a parte comum: 9 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 09: 0 de 13;
- as duas reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

As únicas conferências de verdade são as quatro de `GET /rooms`. As outras que passaram passam por acaso:

- as recusas com 400 passam porque toda criação de reserva devolve 400;
- "refund null" e "as mensagens continuam lá" passam porque não há reserva nem mensagem;
- "nenhuma rodada com as duas criadas" passa porque nenhuma foi criada.

As causas são duas:

1. **A subida quebra.** Ela para em `ReservationHandler.java:71` (o log do serviço registra
   `IllegalArgumentException: Path '/reservations/:id' invalid`). Confirmar, cancelar, check-in, check-out, consultar
   a reserva e tirar a segunda via devolvem 404, e `/messages` e os quatro relatórios devolvem "Endpoint ... not
   found".
2. **Nenhuma reserva é criada.** `POST /reservations` devolve 400 `INVALID_REQUEST` a qualquer pedido válido, porque
   o Gson não converte `LocalDate`. Por isso também saem 400 os casos que deviam dar 404 `ROOM_NOT_FOUND`, 409
   `ROOM_UNAVAILABLE` e 422 `CAPACITY_EXCEEDED`.

Para o dono da pousada, o site não consegue fazer uma reserva, a recepção não faz check-in nem check-out e o contador
não tira nenhum relatório. Só a lista de quartos funciona.

Mesmo consertadas essas duas causas, a leitura do código mostra mais erros que o roteiro pegaria:

- reservas de 1 a 14 de dezembro cobradas com 20% a mais (`PriceCalculator.java:31`);
- o financeiro e o de planos sem a não reembolsável cancelada, porque a consulta só traz as pagas
  (`ReservationDao.java:149`): o dinheiro que ficou com a pousada sairia a menos;
- o comprovante e o e-mail com "R$ 1234,56" (`DocumentFormatter.java:44`);
- a confirmação exige um `amountPaid` que o texto não manda (`ReservationHandler.java:91`).

## Dúvidas

Não dá para saber, pela evidência, quantas conferências passariam se as duas falhas de subida fossem consertadas. Os
erros listados acima vêm da leitura do código, não da execução, porque o roteiro não chegou a eles. Nas notas de
arquitetura e de banco, pesei a estrutura que existe, e não o fato de o sistema não funcionar, que pesa no
comportamento.
