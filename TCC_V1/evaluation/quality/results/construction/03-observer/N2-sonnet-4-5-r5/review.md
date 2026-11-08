# Revisão: construção, cenário 03

## Visão geral

O agente montou o projeto em camadas: `api` (um `ApiHandler` com todas as rotas e os DTOs em `api.dto`), `service` (`ReservationService`, `ReportService` e as exceções), `domain` (`Reservation` com as transições de situação, `PriceCalculator`, enums, `Formatter`), `domain.events` (os eventos e a interface do ouvinte), `listeners` (um ouvinte por interessado: hóspede, governança, financeiro) e `repository` (interfaces) com `repository.postgres` (JDBC puro). Tudo é ligado no `Main.java:33-83`. Uma requisição entra numa rota do Javalin, o `ApiHandler` valida e chama o serviço, o serviço carrega e grava pelos repositórios e, na confirmação e no cancelamento, percorre a lista de ouvintes. Os dados ficam em três tabelas do PostgreSQL, criadas por `CREATE TABLE IF NOT EXISTS` no `Database.java:24-65`. O Observer está bem feito. O defeito mais sério está no banco: a coluna do CPF é `VARCHAR(11)` (`Database.java:42`) e não aceita o CPF no formato que o próprio texto usa, `111.222.333-44`. O INSERT falha, e o `catch (Exception)` do `ApiHandler.java:94-96` devolve 400 "Invalid request", escondendo a causa. Com isso nenhuma reserva é criada no roteiro (18 de 66 conferências passaram). Além disso, nada impede duas reservas do mesmo quarto na mesma noite: num teste manual com CPF sem pontos, as 8 rodadas de pedidos simultâneos criaram as duas reservas. Os testes cobrem só o domínio (12,2% das linhas) e nada da parte nova. O teste de integração foi desligado (`ReservationSystemIT.java.disabled`).

## Arquitetura

**Números:** 9 pacotes, 20 dependências entre eles, nenhum ciclo. Javalin só em `api` e na raiz (`Main`), JDBC só em `repository.postgres` e na raiz. Maior classe: `PostgresReservationRepository` (275 linhas), depois `ApiHandler` (248).
**Nota:** com problemas

A separação é boa no geral:
- As regras de preço e de situação moram no domínio, sem Javalin nem SQL (`Reservation.java:86-120`, `PriceCalculator.java:12-53`).
- O SQL fica isolado nos repositórios Postgres.
- A montagem fica num lugar só (`Main.java:38-63`), inclusive a lista de ouvintes (`Main.java:44-48`).
- As dependências vão da borda para o serviço e do serviço para o domínio e os repositórios, sem ciclo.

Há misturas localizadas na borda HTTP:
- A regra "a segunda via só existe para reserva paga" está no handler (`ApiHandler.java:187-192`), e não no serviço nem no domínio.
- As validações de datas e de hóspedes estão no handler (`ApiHandler.java:45-56`) e repetidas no construtor de `Reservation` (`Reservation.java:30-35`).
- O handler pula o serviço e fala direto com `RoomRepository` e `MessageRepository` (`ApiHandler.java:35`, `ApiHandler.java:205`).
- O `ApiHandler` é uma classe só com as 11 rotas e o mesmo tratamento de erro copiado em cada uma (`ApiHandler.java:105-111`, `119-128`, `136-145`, `153-162`, `170-179`), em vez de um `app.exception(...)` central.

Nada disso é grave, mas são pontos que um revisor pediria para acertar.

## Design de baixo nível

**Números:** 3 interessados, os 3 na estrutura (hóspede, governança, financeiro), cada um num ouvinte. Nenhum e-mail é montado fora dos ouvintes. Confirmação e cancelamento passam pela mesma lista. Há 3 interfaces de repositório com uma implementação cada, que nenhum teste substitui.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** É um Observer como a referência descreve:
- Há uma interface de ouvinte (`domain/events/ReservationEventListener.java:3-6`) com eventos próprios (`ReservationConfirmed`, `ReservationCancelled`, este com a situação anterior).
- O `ReservationService` recebe a lista de fora (`ReservationService.java:18-24`) e só a percorre na confirmação (`ReservationService.java:46-48`) e no cancelamento (`ReservationService.java:71-73`), sem saber quem é avisado.
- Cada interessado é uma classe: `GuestNotifier` (o e-mail do hóspede também virou ouvinte, `GuestNotifier.java:17-36`), `HousekeepingNotifier` e `FinanceNotifier`.
- As regras de cada um ficam no próprio ouvinte: "só avisa no cancelamento se estava confirmada" (`HousekeepingNotifier.java:41`, `FinanceNotifier.java:41`) e "o financeiro não é avisado sem devolução" (`FinanceNotifier.java:48`).
- Os ouvintes são registrados na montagem (`Main.java:44-48`). Trazer o restaurante ou o transfer é escrever um ouvinte e acrescentar uma linha ali.

O que afasta um pouco: a interface tem um método por evento, sem implementação padrão. O hóspede precisa de um `onReservationCancelled` vazio (`GuestNotifier.java:40-41`), e um evento novo (a chegada, para o transfer) obriga a mexer em todos os ouvintes. Isso não muda a classe.

**O exagero.** `RoomRepository`, `ReservationRepository` e `MessageRepository` são interfaces com uma implementação só cada (`repository/*.java`), e nenhum teste usa outra implementação (os testes não tocam nos repositórios). É estrutura que nada substitui. O exagero é leve, mas é exagero pelo critério.

**O resto do código:**
- `catch (Exception)` que esconde a causa: `ApiHandler.java:94-96` transforma qualquer erro, inclusive a falha do INSERT por causa do CPF, em 400 "Invalid request". Foi isso que tornou invisível o defeito que quebra o sistema.
- O código de erro depende do texto da exceção: `e.getMessage().contains("capacity")` (`ApiHandler.java:87`). Trocar a mensagem em `Reservation.java:37` muda o código HTTP.
- A capacidade e a diária estão duplicadas: a tabela `rooms` guarda `capacity` e `nightly_rate` (`Database.java:25-30`, `75-82`), mas o repositório lê só `number` e `type` (`PostgresRoomRepository.java:25`) e usa os valores fixos do enum (`RoomType.java:6-8`). As duas fontes podem divergir sem ninguém perceber.
- O dinheiro é formatado por um `NumberFormat` `static` compartilhado (`Formatter.java:11`). Ele não é thread-safe, e o Javalin atende em várias threads. Além disso, o formato pt-BR do Java põe um espaço não separável (U+00A0) entre "R$" e o valor, e não o espaço comum do texto.
- O dinheiro nasce com escala 0 (`BigDecimal.ZERO` em `Reservation.java:57` e `113`), e a API devolve `amountPaid: 0` e `refund: 0` em vez de 0.00.
- O CSV usa `%.2f` com a locale padrão (`ReportService.java:61`, `73`). Numa JVM em pt-BR, o valor sairia como `599,76` e quebraria o CSV.
- Pontos bons: `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:10-37`), `LocalDate`, enums para situação e plano, e transições com guarda no domínio.

## Testes

**Números:** 18 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 18 métodos escritos, em 2 classes. Cobertura de linhas 98 de 803 (12,2%), de ramos 30 de 142 (21,1%). `listeners` 0 de 83 linhas, `service` 0 de 84, `api` 0 de 168, `repository.postgres` 0 de 232. Nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que existe é bom, mas pequeno:
- `PriceCalculatorTest.java` confere com valores a temporada de julho, dezembro e janeiro (`:25-52`), as duas noites de R$ 599,76 (`:55-63`), o desconto de 15% (116,97, `:66-72`) e o total (`:84-91`).
- `ReservationTest.java` confere as transições, a devolução zero do NON_REFUNDABLE e da pendente, e as recusas de capacidade, datas e hóspedes.

A parte nova do cenário, os avisos, não tem nenhum teste: os ouvintes têm 0 de 83 linhas cobertas, e o serviço que dispara os eventos tem 0 de 84. Na pergunta da mutação:
- apagar a guarda `previousStatus != CONFIRMED` (`HousekeepingNotifier.java:41`) não derruba nenhum teste;
- trocar o destinatário do financeiro (`FinanceNotifier.java:32`) também não;
- tirar o retorno quando não há devolução (`FinanceNotifier.java:48`) também não.

Outras faltas:
- Fora do domínio não há teste: nenhum confere o comprovante, os relatórios, os códigos de erro ou a duplicidade de reserva.
- A devolução do STANDARD confirmado é conferida só como "maior que zero" (`ReservationTest.java:85`), e não como o valor pago.
- O agente escreveu um teste de integração com Testcontainers (`ReservationSystemIT.java.disabled`, 386 linhas) e o desligou renomeando o arquivo, em vez de fazê-lo rodar contra o banco de `DB_URL`. Esse teste teria acusado o defeito do CPF.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` em todas as colunas, e as datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, nenhum UNIQUE ou EXCLUDE. 0 transações explícitas, 0 `FOR UPDATE`. O esquema é criado no código, sem migração. Há um SQL montado com `+` (`PostgresReservationRepository.java:165`), mas o que entra são só `?`, constantes.
**Nota:** ruim

Dois defeitos sérios:
- **O CPF não cabe.** `guest_cpf VARCHAR(11)` (`Database.java:42`) recusa o CPF no formato que o texto mostra (`111.222.333-44`, 14 caracteres). Com o serviço de pé, o mesmo pedido com `111.222.333-44` voltou 400, e com `11122233344` voltou 201. É um tipo errado que derruba a criação de reservas pela API.
- **Nada impede a reserva em dobro.** A disponibilidade é conferida noite a noite com SELECT (`ReservationService.java:83-94`, `PostgresReservationRepository.java:153-191`) e só depois vem o INSERT (`ReservationService.java:38`), sem transação, sem trava e sem restrição no banco (nenhum EXCLUDE). Não há nem um `synchronized`. Num teste manual com dois pedidos simultâneos para o quarto 202, nas mesmas noites, as 8 de 8 rodadas criaram as duas reservas (`select check_in, count(*) ... → 2` em todas). É dado errado no banco: o mesmo quarto vendido duas vezes.

Defeitos menores:
- Não há chave estrangeira da reserva para o quarto (`Database.java:36`).
- O esquema é criado por `CREATE TABLE IF NOT EXISTS` no código (`Database.java:24-65`), que não sabe evoluir uma tabela que já existe.
- Não há CHECK nas colunas de situação, plano e tipo.
- Não há pool de conexões: `PGSimpleDataSource` abre uma conexão nova a cada consulta (`Main.java:33`). O script contou 1 arquivo com pool, mas é só o `DataSource`.
- A confirmação grava a reserva e depois cada mensagem em conexões separadas (`ReservationService.java:42-48`), sem transação. Se um ouvinte falhar, a reserva fica confirmada com os avisos pela metade.
- Pontos bons: as conexões são fechadas com try-with-resources, e o SQL usa parâmetros.

## Comportamento

**Números:** 18 de 66 conferências passaram, 48 falharam.
- A parte comum: 14 de 46.
- Depois de reiniciar: 1 de 3.
- O cenário 03: 2 de 15.
- As reservas simultâneas: 1 de 2.

**Nota:** ruim

Quase todas as falhas têm a mesma causa: criar uma reserva com o CPF `111.222.333-44`, o formato do exemplo do texto, responde 400 INVALID_REQUEST (coluna `VARCHAR(11)`, contada no banco). Sem reserva criada, falham em cascata a confirmação, o e-mail, o check-in e o check-out, os cancelamentos, os relatórios, os dados depois de reiniciar e quase todo o cenário. As duas conferências do cenário que passaram ("NON_REFUNDABLE sem aviso ao financeiro" e "pendente cancelada não manda nada") passam só porque nada foi criado, e não provam a regra. A conferência de simultaneidade que passou, "nenhuma com as duas criadas", também passou porque tudo voltou 400. Para o dono da pousada, isso quer dizer que o site não consegue reservar quando o hóspede digita o CPF com pontos e traço.

Fora isso:
- **Uma escolha razoável, que não pesa:** o número do quarto vai como texto (`"201"`), e o roteiro procura pelo número 201. Por isso falham "o 201 comporta 3" e "a diária do 201". O texto não fecha o tipo, e `GET /rooms` traz a capacidade e a diária certas.
- **O que funciona, com CPF sem pontos:** chamei a API eu mesmo. O preço sai certo (599,76; 389,90 − 58,48 = 331,42). Os nove e-mails saem com os destinatários e os assuntos pedidos, inclusive "Reembolso" só quando há devolução e a governança avisada no cancelamento do NON_REFUNDABLE. O comprovante sai, e os dois relatórios dão `total,1/5` e `total,599.76`. Ou seja, o Observer funciona quando a reserva consegue nascer.
- **Erros que o roteiro acusaria mesmo sem o CPF:** `amountPaid: 0` e `refund: 0` sem as 2 casas pedidas. E o dinheiro dos e-mails sai como `R$ 599,76` (espaço não separável), que não é o `R$ 599,76` do texto, então a conferência "Total: R$ 599,76" falharia de qualquer forma.
- **A reserva em dobro:** no meu teste manual, 8 de 8 rodadas simultâneas criaram as duas reservas. Esse defeito pesa na coluna do banco e não muda esta nota.

## Dúvidas

- **A nota dos testes** ficou entre `com problemas` e `ruim`. Os 18 testes conferem valores de verdade no domínio, mas a parte nova do cenário não tem nenhum teste, a cobertura é de 12%, e o teste de integração foi desligado. Dei `ruim` pela ausência total de teste na parte que o cenário pede.
- **O exagero** vem só das três interfaces de repositório com uma implementação cada. É um padrão comum e leve, mas o critério da skill o conta.
- **O tipo do número do quarto** (texto ou número) o texto não decide. Não pesei as duas falhas que vêm daí.
