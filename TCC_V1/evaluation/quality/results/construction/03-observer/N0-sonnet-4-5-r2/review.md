# Revisão: construção, cenário 03

## Visão geral

O projeto é um serviço Javalin montado à mão em `Main.java` (`Main.java:36-101`), com pacotes por camada:
`web.controller` (handlers), `web.dto` e `web.mapper` (a borda), `service` (as regras e o `EventPublisher`),
`service.listeners` (os cinco interessados nos avisos), `domain.model` e `domain.events`, `repository` (JDBC puro
com HikariCP) e `util` (preço e formatação). Uma requisição passa assim: o handler (`ReservationController`) lê o
corpo, chama o `ReservationService`, que usa os repositórios e publica eventos, e o handler devolve um DTO. Os dados
ficam em três tabelas que o código cria com `CREATE TABLE IF NOT EXISTS`. A parte que o texto pede desde o primeiro
dia (os avisos) é um Observer correto. O mais sério está em outro lugar. A coluna `guest_cpf` é `VARCHAR(11)`
(`repository/ReservationRepository.java:29`), e um `catch (Exception)` genérico (`ReservationController.java:82`)
transforma o erro do banco em `400 INVALID_REQUEST`. Por isso uma reserva com o CPF escrito como o texto o escreve
(`111.222.333-44`) é recusada, e o roteiro de comportamento passou só 18 de 66 conferências. Além disso, nada
impede que o mesmo quarto tenha duas reservas: com pedidos simultâneos, as duas foram criadas em 9 de 10 rodadas.
Os testes de integração não sobem o serviço e não rodam no `mvn verify` (12 erros), e a cobertura de linhas fica em
2,7%.

## Arquitetura

**Números:** 11 pacotes, 23 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `web.controller` e no `Main`; o JDBC aparece em `repository`, `config`, no `Main` e em `service` (só pelo
`SQLException`). A maior classe é `ReservationRepository` (247 linhas), e logo depois vem `ReservationService` (243).
**Nota:** com problemas

As dependências vão na direção certa (borda → serviço → repositório → modelo), sem ciclo. As peças são criadas e
ligadas num só lugar (`Main.java:38-78`), inclusive o registro dos ouvintes. O SQL fica isolado nos repositórios. As
regras de preço e de situação da reserva ficam no serviço, sem `Context` do Javalin.

Há, porém, regras que moram na borda HTTP:
- `ReportController` monta os dois CSVs e calcula ali mesmo o dinheiro que fica com a pousada
  (`web/controller/ReportController.java:91-103`), que é uma regra de negócio. Ele também guarda um mapa fixo
  número→tipo do quarto (`ReportController.java:117`) e consulta o banco no construtor para contar os quartos
  (`ReportController.java:23`).
- A regra "a segunda via só existe para reserva paga" fica no handler (`ReservationController.java:175`), e não no
  serviço.
- O serviço declara e captura `java.sql.SQLException` em todos os métodos (por exemplo
  `service/ReservationService.java:55-58`), então o tipo de erro do JDBC vaza para a camada de regras.
- As exceções de domínio são classes aninhadas no `ReservationService`, que os handlers precisam conhecer.

São misturas localizadas. O desenho geral está bom.

## Design de baixo nível

**Números:** cinco interessados (o hóspede, a governança e o financeiro na confirmação; a governança e o financeiro
no cancelamento), todos ouvintes; nenhum caso fora da estrutura. Há 2 eventos, 1 interface de ouvinte e 5
implementações, registradas em `Main.java:63-73`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Este é o Observer que a referência descreve:
- A interface `EventListener<T>` (`domain/events/EventListener.java:3`) tem uma implementação por interessado, em
  `service/listeners/`. O e-mail do hóspede também é um ouvinte (`GuestNotificationListener`).
- O `ReservationService` só anuncia, depois de gravar, que a reserva foi confirmada ou cancelada
  (`service/ReservationService.java:122` e `:153`), e não conhece nenhum destinatário.
- O `EventPublisher` percorre a lista de ouvintes por tipo de evento (`service/EventPublisher.java:19-26`) e é
  recebido de fora, no construtor.
- O registro fica na montagem (`Main.java:63-73`). Para o restaurante ou o transfer, basta uma classe nova e uma
  linha no `main`.
- As condições do cancelamento ficam em cada ouvinte, e não no serviço: só reserva que estava confirmada
  (`FinanceCancellationListener.java:23`, `HousekeepingCancellationListener.java:21`) e só quando há algo a devolver
  (`FinanceCancellationListener.java:29`). O evento leva `wasConfirmed` para isso.

Não há caso montado à mão no serviço.

**Exagero.** Não há estrutura sem uso fora do pattern. O `RoomService` só repassa o repositório e converte a exceção,
o que é pouco para pesar. Os DTOs com getters e setters são longos, mas existem para a borda.

**O resto do código** tem defeitos que um revisor pediria para corrigir:
- **Duplicação.** O tipo do quarto é deduzido do número por um `switch` fixo copiado em três lugares:
  `GuestNotificationListener.java:61`, `ReportController.java:117` e `web/mapper/DomainMapper.java:71`. Isso
  acontece mesmo com o `RoomType` existindo e o quarto estando no banco. Um quarto novo deixa o comprovante e o
  relatório com o tipo vazio.
- **Dinheiro como `double`.** O valor é calculado certo em `BigDecimal` com HALF_EVEN (`util/PriceCalculator.java`),
  mas vira `double` na saída: `DomainMapper.java:15,31-38,55-57` e
  `String.format("%.2f", netAmount.doubleValue())` em `ReportController.java:101,106`. O JSON sai como `779.8` e
  `0.0`, e não com 2 casas como o texto pede.
- **Erros.** O `catch (Exception e)` em `ReservationController.java:82` responde `400 INVALID_REQUEST` para qualquer
  falha, inclusive erro do banco: foi isso que escondeu o problema do CPF. Nos outros handlers, a `RuntimeException`
  que embrulha o `SQLException` não é tratada e vira 500 sem o formato `{error, message}`.
- **Método longo.** `createReservation` valida, confere a disponibilidade, calcula o preço noite a noite e monta a
  entidade por setters num só método (`ReservationService.java:30-97`). O laço de preço (`:74-84`) devia estar no
  `PriceCalculator`.
- **Estado.** A `Reservation` é toda mutável por setters (`domain/model/Reservation.java`), e as transições de
  situação ficam espalhadas no serviço, em vez de na entidade.

## Testes

**Números:** 22 testes rodaram, 0 falhas e 12 erros (todos os 12 de `ReservationSystemIT`, `Connect`); 22 métodos
escritos. Cobertura de linhas 28 de 1041 (2,7%); de ramos 15 de 152 (9,9%), quase toda em `util` (28 de 31 linhas).
Fora de `util`, a cobertura é 0% em todos os pacotes (`measures/tests.md`).
**Nota:** ruim

- **Os testes de integração não rodam.** `ReservationSystemIT` chama `http://localhost:7070`
  (`src/test/java/com/pousadamarealta/ReservationSystemIT.java:19`), mas nada no teste sobe o serviço nem prepara um
  banco. No `mvn verify`, os 12 dão erro de conexão (`measures/tests-maven.log`). Esses testes também dependem da
  ordem (`@Order`) e de um banco vazio, porque conferem `id == 1` (`:71`).
- **O que roda é pouco.** Só os testes de unidade de `PriceCalculator` e `Formatter` rodam, e eles são bons:
  - a temporada nas bordas (`PriceCalculatorTest.java:28-36`);
  - o meio para o par (`:13-19`);
  - o desconto de 15% com valor (`:56-62`);
  - o formato `R$ 1.234,56` (`FormatterTest.java:13-17`).
- **A parte nova não tem rede.** Os avisos do cenário só aparecem no teste de integração que não roda
  (`ReservationSystemIT.java:130-170`), e mesmo ali faltam os avisos do cancelamento, o "não avisar o financeiro sem
  reembolso" e o "pendente cancelada não avisa ninguém".
- **A pergunta da mutação.** Apagar a linha `if (!event.wasConfirmed()) return;` de um ouvinte de cancelamento, ou
  trocar `compareTo(ZERO) == 0` por `!= 0` no financeiro, não derruba nenhum teste. O cancelamento só confere
  `refund > 0` (`ReservationSystemIT.java:309`).
- **Sem teste de unidade das regras centrais.** As transições de situação, as recusas e o relatório financeiro não
  têm teste de unidade, e o serviço, os ouvintes e os repositórios têm 0% de cobertura.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`. As conferências acusaram:
- esquema criado por `CREATE TABLE IF NOT EXISTS` no código, sem migração;
- nenhuma chave estrangeira (a reserva não está ligada ao quarto);
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição que proteja uma regra;
- 0 transações explícitas e 0 `FOR UPDATE`.

SQL com parâmetros, sem montagem com `+` (`measures/database.md`).
**Nota:** com problemas

Os tipos de dinheiro e datas estão certos. Todo o SQL usa `?`, as conexões são devolvidas com try-with-resources e
há um pool (`config/DatabaseConfig.java`). Os dados sobrevivem ao reinício (3 de 3 no roteiro, numa execução com o
CPF só com dígitos). Os defeitos:
- **CPF que não cabe.** `guest_cpf VARCHAR(11)` (`repository/ReservationRepository.java:29`) só aceita o CPF sem
  pontuação. O CPF como o texto o mostra (`111.222.333-44`, 14 caracteres) estoura a coluna. É o defeito que derruba
  o comportamento (abaixo).
- **Quarto sem proteção contra duas reservas.** Não há EXCLUDE, trava nem transação; o serviço só consulta e depois
  insere (`ReservationService.java:55` e `:95`). Com pedidos simultâneos, as duas reservas foram criadas em 9 de 10
  rodadas (medido numa cópia do roteiro com o CPF só com dígitos).
- **Sem chave estrangeira.** Não há chave estrangeira de `reservations.room_number` para `rooms.number`
  (`ReservationRepository.java:26`).
- **Esquema que não evolui.** O esquema é criado no código (`RoomRepository.java:24`,
  `ReservationRepository.java:24`, `MessageRepository.java:23`), e o próximo pedido que mudar uma coluna não terá
  como evoluir uma tabela que já existe.
- **Gravações separadas.** A gravação da confirmação e as dos e-mails não estão numa transação
  (`ReservationService.java:121-122`): se um ouvinte falhar, a reserva fica confirmada com parte dos avisos.

A falta de proteção contra a dupla reserva é o defeito que mais pesa aqui.

## Comportamento

**Números:** 18 de 66 conferências passaram (`measures/behavior.md`):
- a parte comum: 14 de 46;
- depois de reiniciar: 1 de 3;
- o cenário 03: 2 de 15;
- reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

**Por que quase tudo falhou.** O roteiro cria as reservas com `guestCpf: "111.222.333-44"`, o formato que o próprio
texto usa. O serviço responde `400 {"error":"INVALID_REQUEST","message":"Invalid request"}`, e eu reproduzi isso à
mão. O valor não cabe em `VARCHAR(11)`, e o `catch (Exception)` de `ReservationController.java:82` transforma o
erro do banco num 400 genérico. Com nenhuma reserva criada, falham em cascata a confirmação, o e-mail, o check-in, o
check-out, os cancelamentos, os relatórios e quase todo o cenário. Para o dono da pousada, isso quer dizer que o site
não consegue reservar quando manda o CPF com pontos e traço, e a mensagem de erro não diz por quê.

**Com o CPF só com dígitos.** Para separar esse defeito do resto, rodei uma cópia do roteiro fora da pasta de
revisão, trocando só o CPF por `11122233344`: passaram 62 de 66, com o cenário 03 em 15 de 15 e o reinício em 3 de 3.
As falhas que sobram:
- **Dupla reserva.** O mesmo quarto foi reservado duas vezes para as mesmas noites em 9 de 10 rodadas simultâneas.
  É risco de *overbooking* real.
- **Dinheiro sem 2 casas.** O JSON vem como `0.0`, `779.8` (`DomainMapper.java:31-38`), e o texto pede número com 2
  casas.
- **O número do quarto como texto.** `GET /rooms` devolve `number` como texto (`"201"`), e o roteiro procura um
  número. O texto não fixou o tipo, então essa falha não pesa.

**O relatório financeiro.** À mão, vi que uma STANDARD confirmada e cancelada com devolução integral aparece no
relatório com `0.00` (`ReservationRepository.java:201` filtra por `amount_paid > 0 OR refund IS NOT NULL`). Pelo
texto, ela não deixou dinheiro com a pousada e não devia aparecer. O total continua certo.

**A nota.** Na medida oficial, a parte comum quebra e o cenário quase todo falha. Mesmo na leitura mais generosa,
fica a dupla reserva. Daí `ruim`.

## Dúvidas

- **O formato do CPF na entrada.** O texto não diz em que formato o site manda o CPF. Ele só o mostra formatado no
  comprovante. Se o site mandasse só os dígitos, o sistema funcionaria quase todo (62 de 66 na cópia do roteiro). A
  nota do comportamento segue a medida oficial (18 de 66) e a dupla reserva, que falha nos dois casos.
- **A nota do banco.** Ficou entre `com problemas` e `ruim`. A falta total de proteção contra dupla reserva é risco de
  dado errado, mas a skill do banco põe a proteção fraca da sobreposição entre os exemplos de `com problemas`, e o
  efeito dela já pesa no comportamento.
