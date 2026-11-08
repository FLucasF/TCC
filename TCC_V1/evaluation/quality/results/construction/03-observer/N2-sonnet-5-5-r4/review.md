# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin em seis pacotes: `web` (rotas, leitura do corpo e as visões JSON), `domain` (a reserva e as transições de estado, o preço, o comprovante, o serviço e as interfaces `ReservationRepository` e `ReservationListener`), `notification` (os três ouvintes e o `MessageLog`), `persistence` (JDBC puro), `report` (os dois CSV) e o `Main`, que monta tudo. Uma requisição chega em `web/Api.java:45-58`, o `RequestParser` transforma o corpo em `NewReservation`, o `ReservationService` aplica a regra e grava pelo `JdbcReservationRepository`, e na confirmação e no cancelamento percorre a lista de ouvintes (`domain/ReservationService.java:53,63`), recebida do `Main.java:26-32`. Os dados ficam no PostgreSQL, em duas tabelas (`reservations` e `messages`) criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:29-55`). O comportamento passa inteiro (66 de 66) e o Observer está bem aplicado. Não há nada grave. Os pontos fracos estão no banco: não há migrações, nada impede duas reservas na mesma noite além de uma trava que só vale dentro do processo, não há pool de conexões e os relatórios trazem a tabela inteira. Nos testes, a persistência não é exercitada e o texto dos novos e-mails não é conferido.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `web` e no pacote raiz (o `Main`), o JDBC só em `persistence` e o Jackson só em `web`. `domain` não depende de nenhum outro pacote e é usado por 5. A maior classe é `JdbcReservationRepository`, com 129 linhas.
**Nota:** bom

- A borda HTTP é fina. Cada rota é uma linha que lê, chama o serviço e escreve (`web/Api.java:45-58`). Os erros viram JSON num lugar só (`web/Api.java:39-43`), e a validação de formato fica no `RequestParser` (`web/RequestParser.java:26-89`).
- As regras não conhecem Javalin nem SQL. As transições de estado estão em `domain/Reservation.java:51-72`, o preço em `domain/Quote.java:10-33` e a devolução em `domain/RatePlan.java:21-23`. O serviço só fala com a interface `ReservationRepository` (`domain/ReservationRepository.java:7-20`).
- O acesso ao banco está isolado em `persistence/`, que implementa as interfaces do domínio e de `notification` (`persistence/JdbcReservationRepository.java:15`, `persistence/JdbcMessageLog.java:15`).
- A montagem fica num lugar só, `Main.java:19-35`. Os ouvintes, com os endereços da governança e do financeiro, são registrados ali (`Main.java:26-29`).
- Ressalvas pequenas, que não mudam a nota: o texto do comprovante e o formato das datas brasileiras (`domain/ReceiptText.java`, `domain/Formats.java`) moram no domínio, embora sejam apresentação. A `Api` também liga os relatórios diretamente a `service.all()` (`web/Api.java:55-58`), em vez de passar por um serviço de relatórios.

## Design de baixo nível

**Números:** 3 interessados no texto (hóspede, governança e financeiro). Os 3 são ouvintes separados de `ReservationListener`, e os 2 eventos (confirmação e cancelamento) passam pela estrutura. Nenhum e-mail é montado fora dela. As 2 interfaces fora do pattern (`ReservationRepository` e `MessageLog`) têm uma segunda implementação nos testes (`src/test/.../InMemory.java:11,29`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** `ReservationListener` (`domain/ReservationListener.java:7-15`) tem dois eventos com implementação padrão vazia. Cada interessado é uma classe: `GuestConfirmationNotice` (`notification/GuestConfirmationNotice.java:9-25`), `HousekeepingNotice` (`notification/HousekeepingNotice.java:9-36`) e `FinanceNotice` (`notification/FinanceNotice.java:11-36`). O serviço recebe a lista de fora (`domain/ReservationService.java:15-19`), percorre-a sem saber quem está nela (`domain/ReservationService.java:53,63`) e não monta nenhum e-mail. O e-mail do hóspede também é um ouvinte. Para o restaurante ou o transfer basta uma classe nova e uma linha no `Main.java:26-29`. Pela referência, é acerto.
- **O que afasta um pouco.** O evento de cancelamento leva a situação anterior (`previous`), e cada ouvinte decide sozinho se a reserva estava confirmada. A mesma condição aparece em dois lugares (`notification/HousekeepingNotice.java:30`, `notification/FinanceNotice.java:30`), e todo ouvinte novo de cancelamento vai ter de repeti-la. Para guardar essa situação, o serviço usa um array de um elemento dentro do lambda (`domain/ReservationService.java:58-62`), um truque que funciona mas é pouco legível. Os ouvintes rodam depois da gravação e fora da trava (`domain/ReservationService.java:52-53`): se um deles falhar, a reserva fica confirmada, a resposta é 500 e os avisos saem pela metade.
- **O exagero.** Não há. As duas interfaces fora do pattern são substituídas nos testes, e não há fábricas, builders nem camadas que só repassam.
- **O resto do código.** Dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:17-19`, `domain/Quote.java:14-18`, `domain/RatePlan.java:17-19`), as datas são `LocalDate`, e situação, plano e tipo de quarto são `enum` (`domain/RoomType.java`, `domain/RatePlan.java`, `domain/ReservationStatus.java`). O estado da `Reservation` só muda pelas transições, que se protegem (`domain/Reservation.java:51-78`). Os métodos são curtos, os nomes dizem o que fazem e não há duplicação relevante. As `SQLException` viram `IllegalStateException` com a causa, e o `catch (Exception)` geral imprime a pilha (`web/Api.java:40-43`). Nada aqui pediria mudança antes de aceitar.

## Testes

**Números:** 13 testes rodaram, 0 falhas, 0 erros e 0 pulados. São 13 métodos escritos, em 3 classes. Cobertura de linhas de 68,3% (317 de 464) e de ramos de 72,4% (76 de 105). Por pacote: `domain` 178 de 179 linhas, `notification` 40 de 40, `report` 25 de 25, `web` 74 de 87 e `persistence` 0 de 119 (`measures/tests.md`).
**Nota:** com problemas

- **O que está testado com valores.** O preço em alta temporada (599,76) e o desconto do não reembolsável com o arredondamento (116,97 e 662,83), em `QuoteTest.java:14-27`. Os limites da temporada, em `QuoteTest.java:30-36`. O formato R$ 1.234,56, em `QuoteTest.java:39-41`. O e-mail do hóspede, conferido palavra por palavra (`ReservationServiceTest.java:39-52`). As transições e recusas (`ReservationServiceTest.java:90-112`). Os dois CSV, comparados por inteiro (`ApiTest.java:53-56`). Os códigos HTTP das recusas (`ApiTest.java:61-71`).
- **A parte nova.** Os testes conferem quem recebe e com qual assunto: três mensagens na confirmação (`ReservationServiceTest.java:36,53-54`), duas no cancelamento de uma confirmada STANDARD (`ReservationServiceTest.java:72-74`), só a governança no não reembolsável (`ReservationServiceTest.java:84-85`) e ninguém no cancelamento de uma pendente (`ReservationServiceTest.java:62`). O corpo desses e-mails não é conferido em nenhum teste. Pela pergunta da mutação: tirar a linha "Hóspedes" ou trocar as datas da governança (`notification/HousekeepingNotice.java:23-25`), escrever `r.quote().subtotal()` no lugar de `amountPaid()` no aviso de pagamento ou formatar errado o valor do reembolso (`notification/FinanceNotice.java:25,35`) não derrubaria nenhum teste.
- **O nível.** Todos os testes usam repositórios em memória (`InMemory.java`). A camada JDBC nunca roda nos testes (0 de 119 linhas). Se alguém invertesse `check_in < ? AND check_out > ?` na consulta que procura sobreposição (`persistence/JdbcReservationRepository.java:104`), nenhum teste acusaria. A regra de que uma NON_REFUNDABLE cancelada continua no relatório financeiro também não tem teste. `cancelledReservationFreesRoom` (`ReservationServiceTest.java:115-119`) não tem asserção: só conta com a ausência de exceção.

## Banco

**Números:** 2 tabelas (`messages`, `reservations`), ambas com chave primária. As 5 colunas de dinheiro estão em `numeric(12,2)`. Há 0 chaves estrangeiras, 0 CHECK e 0 UNIQUE/EXCLUDE. As colunas `status`, `rate_plan` e `channel` são `text` sem CHECK. Há 0 pool de conexões, 1 `DriverManager.getConnection`, 0 transações explícitas e 0 `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

- **O esquema não evolui.** Ele vem de `CREATE TABLE IF NOT EXISTS` dentro do código (`persistence/Database.java:29-55`), sem migrações versionadas. O próximo pedido que acrescentar uma coluna não terá como alterar um banco que já existe.
- **Os tipos estão certos.** Dinheiro em `NUMERIC(12,2)`, datas em `DATE`, `NOT NULL` em tudo que é obrigatório e `refund` com nulo de propósito (`persistence/Database.java:31-46`). Os quartos não estão no banco (`domain/RoomCatalog.java:8-13`), então `room_number` não tem chave estrangeira. O texto diz que os quartos quase nunca mudam, e é uma escolha defensável, mas o banco aceita qualquer número de quarto. A situação e o plano são texto livre, sem CHECK.
- **As reservas duplicadas.** Uma `ReentrantLock` no serviço (`domain/ReservationService.java:13,34-43`) envolve a consulta de sobreposição e o insert. Isso funciona com um processo só, e o roteiro de concorrência passou, mas o banco não protege a regra: não há restrição `EXCLUDE`, transação nem trava de linha. Duas instâncias do serviço, ou um insert por fora, gravariam duas reservas na mesma noite.
- **O código que fala com o banco.** Todo o SQL usa parâmetros (`persistence/JdbcReservationRepository.java:27-47,60-65,102-109`), e as conexões são fechadas com try-with-resources. Não há pool: cada chamada abre uma conexão nova por `DriverManager` (`persistence/Database.java:20-22`). A mudança de situação e a gravação dos avisos não estão numa mesma transação (`domain/ReservationService.java:52-53`, `persistence/JdbcMessageLog.java:24-40`), então uma falha no meio deixa a reserva confirmada sem todos os e-mails.
- **A carga.** Os dois relatórios carregam todas as reservas (`persistence/JdbcReservationRepository.java:86-98`, chamado em `web/Api.java:56,58`) e filtram a noite e o mês em Java (`report/OccupancyReport.java:16-19`, `report/RevenueReport.java:17-21`). A tabela cresce sem limite.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam. A parte comum passou em 46 de 46, os dados depois de reiniciar em 3 de 3, o cenário 03 em 15 de 15 e as duas reservas ao mesmo tempo em 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede. Os preços estão certos, com a temporada e o "meio para o par" (599,76; 37,48 de desconto e 212,42 de total). As recusas trazem os códigos combinados, o comprovante e o e-mail do hóspede saem no formato pedido e os dois CSV estão certos, inclusive o não reembolsável cancelado que continua no financeiro. Depois de reiniciar, as reservas e as mensagens continuam lá. Na confirmação, a governança recebe as datas e o número de hóspedes, e o financeiro recebe o valor pago. No cancelamento de uma confirmada, a governança recebe as datas liberadas, e o financeiro recebe o reembolso só quando há o que devolver. Cancelar uma reserva pendente não avisa ninguém. As reservas simultâneas foram recusadas em todas as rodadas. Essa proteção, porém, vale só para um processo, e o defeito já foi contado no Banco.

## Dúvidas

Nenhuma sobre a classificação. No design, a nota `bom` convive com dois pontos menores (a condição "estava confirmada" repetida em cada ouvinte e o array usado como variável mutável em `domain/ReservationService.java:58-62`). Um revisor mais rigoroso poderia pedir que fossem corrigidos, mas eles não chegam a tornar o código difícil de mudar.
