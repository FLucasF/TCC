# Revisão: construção, cenário 09

## Visão geral

O projeto foi feito em camadas: `api` (os controllers do Javalin e os DTOs), `service` (o preço, as situações da reserva, o comprovante e o e-mail), `report` (os quatro relatórios CSV), `repository` (JDBC puro com `PreparedStatement`), `domain` e `config` (a conexão e o `CREATE TABLE`). A montagem fica toda em `ApiServer.start()` (`src/main/java/com/pousada/api/ApiServer.java:29-65`). Uma requisição passa assim: o controller lê o corpo, chama o `ReservationService`, que valida, calcula o preço com o `PriceCalculator` e grava pelo `ReservationRepository` no PostgreSQL. Os relatórios seguem um Template Method certo (`ReportGenerator`, com os quatro relatórios como subclasses), e o roteiro do cenário passou inteiro (13 de 13). O problema mais sério é o quarto reservado em dobro. A conferência de conflito e o `INSERT` são duas chamadas separadas, sem transação, sem trava e sem restrição no banco (`src/main/java/com/pousada/service/ReservationService.java:51-79`). Nas 10 rodadas de pedidos simultâneos, as 10 criaram as duas reservas. Há ainda três defeitos menores: um corpo que não é JSON responde 500, os testes são só 5, do cálculo de preço (4,5% das linhas cobertas), e o esquema é criado no código, sem migrações.

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, nenhum ciclo. A borda (`api`) depende de 6 pacotes, entre eles `repository` e `config`, mas só porque é o lugar onde as peças são montadas. O Javalin só aparece em `api`, e o JDBC só em `repository` e `config`. A maior classe é `ReservationRepository`, com 257 linhas.
**Nota:** bom

- A borda HTTP é fina. Os handlers leem o corpo e os parâmetros, chamam o serviço e traduzem as exceções em códigos de erro (`src/main/java/com/pousada/api/ReservationController.java:37-83`). A única regra que mora num handler é a de que a segunda via só existe para reserva paga (`ReservationController.java:179-183`), mas ela delega a `Reservation.isPaid()` (`src/main/java/com/pousada/domain/Reservation.java:126-130`). É uma mistura pequena, porque o resto da máquina de estados está no serviço.
- As regras ficam em `service`, sem Javalin e sem SQL. A máquina de estados está em `ReservationService.java:18-22,153-156`, a devolução em `ReservationService.java:146-151` e o preço em `src/main/java/com/pousada/service/PriceCalculator.java`.
- O acesso ao banco fica isolado nos três repositórios. A montagem se faz num lugar só, por construtor (`ApiServer.java:29-65`), sem `static` mutável e sem `new` de repositório dentro dos serviços.
- O pacote `report` depende de `repository` direto, sem passar por um serviço (`src/main/java/com/pousada/report/RevenueReport.java:12-16`). Num sistema deste tamanho isso é aceitável.
- Um ponto pequeno: a montagem e o servidor HTTP estão na mesma classe, `ApiServer`. Não pesa.

## Design de baixo nível

**Números:** os 4 de 4 relatórios passam pelo esqueleto (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlansReport` estendem `ReportGenerator`), e nenhum monta a linha de título por conta própria. O cálculo de "pago menos devolvido" aparece copiado 3 vezes, e cada relatório que tem total faz 2 consultas iguais ao banco por chamada.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `src/main/java/com/pousada/report/ReportGenerator.java:9-18` é o método-modelo: título, cabeçalho, linhas e total, nessa ordem e num lugar só. Os passos que mudam são abstratos: `getReportName`, `generateHeader`, `generateDataLines` e `generateTotal` (`ReportGenerator.java:24-30`). O título fica no esqueleto, que é privado (`ReportGenerator.java:20-22`), e os formatos comuns de dinheiro e de data também ficam ali (`ReportGenerator.java:32-38`). Uma linha de rodapé com a data, que o dono já anunciou, entra numa linha de `generate()` e vale para os quatro. Os quatro relatórios são subclasses que preenchem só os passos, os dois da parte comum inclusive. O controller não pergunta qual é o caso: cada rota chama `generate(period)` no relatório dela (`src/main/java/com/pousada/api/ReportController.java:48,69,90,111`). Por isso a classe é acerto.

O que afasta um pouco:
- `generate()` não é `final`, então uma subclasse poderia trocar o esqueleto.
- `generateDataLines` e `generateTotal` recebem cada um só o período e buscam as reservas de novo. Assim cada relatório faz a mesma consulta duas vezes (`RevenueReport.java:31` e `:53`, `TourismTaxReport.java:33` e `:54`, `RatePlansReport.java:34` e `:59`, `OccupancyReport.java:33` e `:51`). Se uma reserva mudar entre as duas consultas, o total pode não bater com as linhas. Um passo de seleção no esqueleto, que passasse a lista para os dois passos, fecharia isso.
- O `ReportController` repete quatro vezes o mesmo handler (`ReportController.java:38-120`).

**O exagero.** Não há estrutura sem necessidade. `RoomService` (`src/main/java/com/pousada/service/RoomService.java:15-22`) quase só repassa ao repositório, mas é ele que traduz o quarto ausente em `RoomNotFoundException`. Não há interfaces de uma implementação só, fábricas ou builders. A nota é contenção.

**O resto do código.**
- Duplicação. O "pago menos devolvido" está copiado em `RevenueReport.java:35-37`, `RevenueReport.java:55-59`, `RatePlansReport.java:45-48` e `RatePlansReport.java:63-66`. Essa regra deveria morar na `Reservation`. A lista de colunas do `SELECT` aparece quatro vezes em `src/main/java/com/pousada/repository/ReservationRepository.java:93-95,148-150,180-182,211-213`. Os blocos `catch` são iguais em seis handlers (`ReservationController.java:92-98,107-116,125-134,...`).
- Formatação que depende da localidade da máquina. O dinheiro dos CSV sai de `String.format("%.2f", amount)` (`ReportGenerator.java:33`), e o do comprovante sai de `String.format("R$ %,.2f")` com troca de caracteres (`src/main/java/com/pousada/service/DocumentGenerator.java:71`), os dois sem `Locale`. Numa JVM em pt_BR, o CSV sairia `27,00`, o que quebra as colunas, e o comprovante sairia `R$ 1,234.56`. No ambiente do roteiro funcionou, mas a saída depende de onde o sistema roda.
- Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:18,33,38,44`), `LocalDate`, `YearMonth` e `enum` para situação, plano, tipo de quarto e canal. Há um defeito pequeno: o desconto zero e o pago zero são `BigDecimal.ZERO`, sem escala 2 (`PriceCalculator.java:40`, `ReservationService.java:75`), e por isso o JSON da criação mostra `0` (veja Comportamento).
- Os erros: exceções de domínio sem mensagem (`ReservationService.java:158-171`), um `RuntimeException("Room not found")` genérico (`DocumentGenerator.java:16`) e um `catch (Exception)` geral que só imprime a pilha (`ApiServer.java:67-71`). O erro de JSON malformado cai nesse `catch` geral, porque o handler não o trata (`ReservationController.java:39,68`).
- O estado é razoável: a `Reservation` tem setters só para status, pago e devolução (`Reservation.java:106-124`), e as transições passam pela tabela `ALLOWED_TRANSITIONS`.

Os defeitos são localizados, mas vários. Por isso a nota é `com problemas`.

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 erros e 0 pulados. São 5 métodos escritos, num único arquivo. A cobertura é de 44 de 977 linhas (4,5%) e de 14 de 132 ramos (10,6%). `report`, `api`, `repository` e `config` ficaram com 0%. Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

- O único arquivo de teste é `src/test/java/com/pousada/PriceCalculatorTest.java`. Ele confere o preço com valores: a baixa temporada (`:28-30`), o exemplo do enunciado de 599,76 (`:43`), o desconto de 15% com 74,97 e 424,83 (`:59-60`) e a fronteira de 15 de dezembro (`:64-89`). Esses testes são bons.
- Nada mais foi testado: a máquina de estados, o cancelamento e a devolução, a sobreposição de reservas, a capacidade, os códigos de erro, o comprovante, o e-mail e, sobretudo, a parte nova do cenário. Os quatro relatórios, a linha de título, a taxa de turismo e o relatório por plano não têm teste nenhum (pacote `report` com 0 de 117 linhas).
- A pergunta da mutação na parte nova: se `4.50` virasse `5.40` (`TourismTaxReport.java:12`), se o título trocasse a ordem dos campos (`ReportGenerator.java:21`) ou se o laço de `RatePlan.values()` fosse trocado por só os planos com reserva (`RatePlansReport.java:41`), nenhum teste falharia. A troca de `check_in <= ?` por `<` na ocupação (`ReservationRepository.java:153`) também passaria.
- O texto não pedia testes, mas a cobertura é quase nula fora do preço, e a parte nova do cenário ficou sem nenhuma rede. Por isso a nota é ruim.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice, `reservations(room_number) -> rooms`. Não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK, e nenhum CHECK nas colunas de situação, plano, tipo e canal. O esquema é criado por `CREATE TABLE` no código, com 0 arquivos de migração. Não há pool (um `DriverManager.getConnection`), nem transações explícitas (0), nem `FOR UPDATE` (0). Nenhum SQL é montado com `+`.
**Nota:** ruim

- **A duplicação de reserva não tem proteção nenhuma.** A conferência é um `SELECT COUNT(*)` (`src/main/java/com/pousada/repository/ReservationRepository.java:115-144`), e o `INSERT` vem depois, em outra conexão e sem transação (`ReservationRepository.java:22-64`, chamados em `ReservationService.java:51` e `:79`). Não há restrição `EXCLUDE` sobre o intervalo de datas, nem trava, nem `synchronized`. O roteiro mostrou o efeito: nas 10 rodadas de pedidos simultâneos, as 10 gravaram duas reservas para o mesmo quarto e as mesmas noites. É dado errado gravado, exatamente o que o enunciado proíbe ("Um quarto não pode ter duas reservas na mesma noite"). Por isso a nota é ruim.
- O esquema não evolui: `CREATE TABLE IF NOT EXISTS` em `src/main/java/com/pousada/config/Database.java:27-64` não sabe acrescentar uma coluna numa tabela que já existe, e o próximo pedido do contador vai precisar disso.
- Os tipos estão certos: o dinheiro em `DECIMAL(10,2)` (`Database.java:46-51`), as datas em `DATE`, `NOT NULL` no que é obrigatório e a reserva ligada ao quarto por chave estrangeira (`Database.java:52`), porém sem índice em `room_number`, que é justamente a coluna que a consulta de conflito filtra.
- A situação, o plano e o tipo são `VARCHAR` sem `CHECK` (`Database.java:30,44,49`).
- Os quartos guardam só o número e o tipo; a capacidade e a diária vêm do `enum` `RoomType` (`src/main/java/com/pousada/domain/RoomType.java:6-8`). É uma escolha aceitável, porque os quartos quase nunca mudam.
- O código: todo o SQL usa parâmetros `?` e as conexões são fechadas por try-with-resources, mas os `ResultSet` não ficam no try (`ReservationRepository.java:55,104`). Cada operação abre uma conexão nova pelo `DriverManager`, sem pool (`Database.java:19-21`). Os relatórios fazem cada consulta duas vezes (veja Design), e a ocupação busca o quarto uma vez por linha (`src/main/java/com/pousada/report/OccupancyReport.java:37`).

## Comportamento

**Números:** 61 de 64 conferências passaram e 3 falharam. Na parte comum, 44 de 46. Depois de reiniciar, 3 de 3. No cenário 09, 13 de 13. Nas reservas simultâneas, 1 de 2: as duas reservas foram criadas em 10 de 10 rodadas. A segunda conferência dessa parte passou só porque conta as rodadas que não deram nem `201 201` nem `201 409`.
**Nota:** com problemas

- **O cenário funciona inteiro.** Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A taxa de turismo cobra R$ 4,50 por hóspede por noite e fecha com o total, inclusive num mês vazio. O relatório por plano traz STANDARD e NON_REFUNDABLE mesmo sem reserva no mês. Mês fora do formato responde 400.
- **Duas reservas ao mesmo tempo para o mesmo quarto:** as duas são aceitas, todas as vezes. Para o dono, isso quer dizer que, se dois hóspedes reservarem o mesmo quarto no mesmo instante pelo site, os dois recebem a reserva, e depois os dois podem pagar e receber o e-mail de confirmação. A causa está no banco (veja Banco); aqui ela aparece numa chamada que o texto descreve, e por isso pesa nesta coluna.
- **Corpo que não é JSON responde 500 INTERNAL_ERROR, em vez de 400 INVALID_REQUEST.** O erro de leitura do JSON em `ctx.bodyAsClass` (`ReservationController.java:39`) não é capturado em `ReservationController.java:68` e cai no tratamento geral (`ApiServer.java:67-71`). O texto pedia esse caso explicitamente. Para o dono, o site recebe "erro interno" onde deveria receber "pedido inválido".
- **O dinheiro zero sai sem 2 casas no JSON da criação:** `"discount":0` e `"amountPaid"` igual a zero, quando o texto pede número com 2 casas (0.00). Os valores vêm de `BigDecimal.ZERO` sem escala (`PriceCalculator.java:40`, `ReservationService.java:75`). Depois de relida do banco, a reserva volta com 0.00. O valor está certo e só a forma difere, então pesa pouco.
- Os dados continuam depois de reiniciar.

A parte comum e o cenário funcionam; o defeito sério é a reserva em dobro sob concorrência. Por isso a nota é `com problemas`.

## Dúvidas

- A formatação de dinheiro sem `Locale` (`ReportGenerator.java:33`, `DocumentGenerator.java:71`) passou no ambiente do roteiro. A evidência não diz em que localidade a JVM da pousada vai rodar; numa em pt_BR, os CSV e o comprovante sairiam errados. Isso pesou no design, não no comportamento.
- A arquitetura ficou `bom`, apesar da regra pequena da segunda via no handler (`ReservationController.java:179-183`). Um revisor mais rígido poderia dar `com problemas` por isso.
