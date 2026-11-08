# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin em seis pacotes: `api` (os controllers, um por recurso), `service` (as regras,
os relatórios e os avisos), `db` (DAOs com JDBC puro e uma classe `Database` que cria o esquema com
`CREATE TABLE IF NOT EXISTS`), `model` (entidades e enums), `util` (preço, arredondamento e formatação) e o `Main`,
que liga as peças (`Main.java:17-33`). Uma requisição passa pelo controller, que lê o corpo como `Map`
(`ReservationController.java:23`), chama o `ReservationService`, e este usa os DAOs, que gravam no PostgreSQL. Para os
avisos, há uma lista de `NotificationHandler` no `NotificationService` (`NotificationService.java:16,38,48`). Ela tem
uma única implementação, que avisa a governança e o financeiro ao mesmo tempo, e o e-mail do hóspede continua sendo
mandado direto pelo serviço (`ReservationService.java:75`). Há três problemas sérios:
1. **A confirmação exige um campo `amountPaid` no corpo**, que o texto não pede (`ReservationController.java:68-73`).
   Uma confirmação como o texto descreve volta 400, e nada do que vem depois funciona: e-mails, check-in, check-out,
   devoluções e relatórios.
2. **O serviço não sobe de novo depois que existe alguma reserva.** A cada partida ele apaga os quartos
   (`Database.java:96`), e a chave estrangeira das reservas recusa isso.
3. **Duas reservas simultâneas do mesmo quarto passam as duas** em todas as 10 rodadas, porque nada protege contra
   essa sobreposição.

Só há 6 testes, todos do cálculo de preço.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. Javalin só em `br.marealta` e `br.marealta.api`;
JDBC só em `br.marealta.db`. A maior classe é `ReservationController` (221 linhas), seguida de `ReservationService`
(201).
**Nota:** com problemas

As camadas existem e apontam na direção certa: borda → regras → dados. As regras não conhecem nem o `Context` do
Javalin nem o SQL (`ReservationService.java:15-26`), o SQL está todo nos DAOs, e a montagem acontece num lugar só
(`Main.java:17-33`). Os defeitos são localizados:

- **O `MessageController` pula a camada de regras** e fala direto com o `MessageDAO` (`MessageController.java:3,10`).
  É por isso que existe a dependência `api -> db`.
- **O banco é um singleton estático** (`Database.java:13,24-38`) que lê as variáveis de ambiente por conta própria.
  O `Main` chama `Database.getInstance()` em vez de criar o objeto e passá-lo adiante.
- **O controller faz trabalho que não é de borda:**
  - monta o comprovante campo a campo, duas vezes (`ReservationController.java:109-122` e `140-153`);
  - decide quem pode tirar a segunda via olhando o valor pago (`ReservationController.java:133`), quando essa regra
    é do serviço;
  - traduz erros comparando o texto da mensagem da exceção (`ReservationController.java:198-219`).
- **O texto do comprovante depende do acesso a dados:** o serviço o monta consultando o `RoomDAO` com um `.get()` sem
  conferência (`ReservationService.java:185`).

## Design de baixo nível

**Números:** dos 3 interessados que o texto cita na confirmação (hóspede, governança, financeiro), 2 estão na
estrutura de ouvintes e o hóspede fica fora (`ReservationService.java:75,161-178`). Governança e financeiro não são
ouvintes separados: dividem uma única implementação, `DefaultNotificationHandler`, que conhece os dois destinos. No
cancelamento, os 2 interessados estão nessa mesma classe.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Observer).**

O que aproxima do pattern:
- existe uma interface de ouvinte com os dois eventos (`NotificationHandler.java:6-8`);
- o serviço percorre uma lista de ouvintes, sem saber quem são (`NotificationService.java:37-55`);
- a lista é preenchida na montagem (`Main.java:28`);
- o serviço de reservas só anuncia os eventos (`ReservationService.java:74,135`);
- a falha de um ouvinte não derruba a operação (`NotificationService.java:41,51`).

O que afasta do pattern:
1. **O hóspede tem chamada direta.** O e-mail dele é montado e mandado pelo próprio `ReservationService`
   (`ReservationService.java:75,161-178`). A referência dá esse caso como malfeito: "o hóspede tem chamada direta e só
   a governança e o financeiro viraram ouvintes".
2. **Há um ouvinte só para dois interessados.** `DefaultNotificationHandler` é um "notificador" que conhece a
   governança e o financeiro (`DefaultNotificationHandler.java:14-26,38,51`). Para tirar o financeiro, ou para tratar
   os dois de forma diferente, é preciso editar essa classe, em vez de desregistrar um ouvinte. O restaurante e o
   transfer até podem entrar como classes novas registradas no `Main`, mas o desenho que já existe não separa os
   interessados.
3. **O ouvinte depende de volta do serviço que o chama:** `DefaultNotificationHandler` recebe o `NotificationService`
   para usar o `send` (`DefaultNotificationHandler.java:8-12`). O mesmo objeto é ao mesmo tempo o "sujeito" e a caixa
   de saída.

**O exagero.** Não há fábricas, builders nem interfaces sobrando fora do pattern. Há dois detalhes pequenos:
- `RoomService` só repassa `roomDAO.findAll()` (`RoomService.java:15-17`);
- `Notification` repete os campos de `Message` (`Notification.java:5-8` e `Message.java:5-9`).

Nenhum dos dois chega a ser estrutura em excesso.

**O resto do código.**

O que está certo:
- `BigDecimal` com `HALF_EVEN` a cada passo (`MoneyRounder.java:8-12`, `PricingCalculator.java:22-54`);
- `LocalDate` para as datas e enums para situação, plano e tipo.

Os defeitos, concentrados no `ReservationController`:
- **Comprovante duplicado, com o mesmo erro nas duas cópias:** o bloco está em `ReservationController.java:109-122`
  e `140-153`, e as duas cópias põem o número do quarto em `roomType` (`:115` e `:146`).
- **Variável calculada e jogada fora:** `String document` em `:108` e `:139`.
- **Erros codificados como texto:** o código do erro vai na mensagem de uma `IllegalArgumentException`
  (`ReservationService.java:34,38,42`) e é traduzido por um `switch` sobre essa string
  (`ReservationController.java:198-219`).
- **`catch (Exception)` que esconde a causa:** responde 400 para qualquer erro, até falha do banco
  (`ReservationController.java:48,79`, `ReportController.java:30`).
- **Dinheiro convertido para `double` no JSON** (`ReservationController.java:171-175`).
- **Segunda via decidida pelo valor pago, não pela situação** (`ReservationController.java:133`). Na prática, uma
  reserva cancelada com reembolso ainda emite comprovante: conferi pela API, `GET /reservations/1/document` responde
  200 depois do cancelamento.
- **`Reservation` é um saco de setters**, sem nenhuma regra de transição dentro dela (`Reservation.java:42-168`). As
  transições ficam soltas no serviço, e a do cancelamento está errada: aceita CHECKED_IN
  (`ReservationService.java:115-118`).

## Testes

**Números:** 6 testes rodaram, 0 falharam (6 métodos escritos, numa classe só). Cobertura de linhas: 29 de 813
(3,6%). Cobertura de ramos: 16 de 164 (9,8%). `api`, `db` e `service` têm 0% de cobertura. Não há teste pela API nem
contra o banco.
**Nota:** ruim

O único arquivo de teste é `PricingCalculatorTest.java`. Ele confere com valores:
- a alta temporada (`:14-21`);
- o preço da noite em alta (`:30-31`, 299.88);
- a soma (`:38-40`, 599.76);
- o desconto (`:50-51`, 89.96);
- o total (`:59-60`).

Nada mais é testado: as situações da reserva, o cancelamento e a devolução, as recusas e os códigos de erro, o
comprovante, os relatórios e, principalmente, a parte nova do cenário (os avisos). Fazendo a pergunta da mutação na
parte nova, nenhum teste falharia se alguém:
- apagasse `notificationService.notifyReservationConfirmed` (`ReservationService.java:74`);
- trocasse o `> 0` que decide se o financeiro é avisado no cancelamento (`DefaultNotificationHandler.java:23`);
- tirasse o `if (wasConfirmed)` que impede avisos para uma reserva pendente (`ReservationService.java:134`).

O `pom.xml` declara Testcontainers e `junit-jupiter`, mas nenhum teste os usa. Os defeitos sérios que o roteiro
encontrou (a confirmação, o reinício, o cancelamento depois do check-in) passariam por esses testes sem nenhum alerta.

## Banco

**Números:** o script criou 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está
em `numeric(10,2)` e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice. Nenhuma restrição
(UNIQUE, EXCLUDE ou CHECK) protege uma regra. As colunas de situação e tipo não têm CHECK. Não há pool, transação
explícita nem `FOR UPDATE`. O script apontou 2 SQL montados com `+`.
**Nota:** ruim

- **O serviço não sobe de novo sobre um banco com reservas.** `initializeRooms` começa com `DELETE FROM rooms`
  (`Database.java:96`). Com qualquer reserva gravada, a chave estrangeira recusa a remoção, `initialize` lança
  `RuntimeException` (`Database.java:89-91`) e o processo morre. Reproduzi isso à mão: o log diz `update or delete on
  table "rooms" violates foreign key constraint "reservations_room_number_fkey"`, e o roteiro do comportamento mostra o
  mesmo ("o serviço sobe de novo sobre o mesmo banco: não subiu"). Os dados ficam no banco, mas o sistema não consegue
  mais usá-los depois do primeiro reinício.
- **Nada impede duas reservas na mesma noite.** A disponibilidade é conferida com um `SELECT`
  (`ReservationService.java:40`, `ReservationDAO.java:88-110`) e o `INSERT` vem depois, em outra conexão, sem
  transação, sem trava e sem restrição `EXCLUDE`. No roteiro, as 10 rodadas simultâneas criaram as duas reservas.
- **O esquema não evolui:** ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do Java (`Database.java:48-84`), sem
  migrações. Uma coluna nova não chega a um banco que já existe.
- **Uma conexão nova a cada chamada:** `DriverManager.getConnection` (`Database.java:41`), sem pool.
- **Os dois SQL com `+`** (`ReservationDAO.java:78`, `RoomDAO.java:46`) concatenam um `long` e um `int` já
  convertidos, então não há injeção. Mesmo assim, destoam do resto, que usa `?`.
- **Fora do roteiro:** o relatório financeiro filtra só CONFIRMED, CHECKED_IN e CHECKED_OUT (`ReservationDAO.java:116`).
  Com isso, a reserva NON_REFUNDABLE cancelada, que o texto manda contar, fica fora do relatório. Essa falha pesa no
  comportamento.

## Comportamento

**Números:** 30 de 64 conferências passaram.
- A parte comum: 27 de 46.
- Os dados depois de reiniciar: 0 de 1.
- O cenário 03: 2 de 15.
- As reservas simultâneas: 1 de 2.
**Nota:** ruim

- **A confirmação não funciona do jeito que o texto descreve.** `POST /reservations/{id}/confirmation` sem corpo
  responde 400, porque o controller exige `amountPaid` (`ReservationController.java:68-73`). O texto diz que o que o
  hóspede pagou é o total da reserva. Essa falha leva junto quase todo o resto:
  - o e-mail do hóspede;
  - os avisos da governança e do financeiro (13 das 15 conferências do cenário);
  - a segunda via, o check-in e o check-out;
  - as devoluções;
  - as linhas dos dois relatórios.

  Além disso, o valor pago passa a ser o que o cliente mandar, sem conferir se bate com o total, o que é um risco de
  dado errado no financeiro.
- **Com `amountPaid` no corpo, os avisos saem certos.** Conferi pela API e saíram os 5 e-mails esperados: governança
  com datas e hóspedes, financeiro com R$ 779,80, o hóspede com o comprovante e, no cancelamento, governança e
  reembolso. A parte nova existe, mas o site, chamando como foi combinado, nunca chega a ela.
- **Cancelamento depois do check-in é aceito** (200 em vez de 409), porque o serviço só recusa CHECKED_OUT e
  CANCELLED (`ReservationService.java:115-118`).
- **O sistema não sobe de novo depois da primeira reserva** (`Database.java:96`; o defeito pesa na coluna do banco).
  Para o dono, isso quer dizer que cada reinício depois de abrir para reservas deixa a pousada sem sistema.
- **Reservas simultâneas:** nas 10 rodadas, as duas reservas do mesmo quarto foram criadas, ou seja, o mesmo quarto
  foi vendido duas vezes.
- **O formato do JSON:** dinheiro sai como `0.0` e `779.8`, não com 2 casas (`ReservationController.java:171-175`), e
  as datas da reserva saem como `10/07/2029`, não `2029-07-10` (`:166-167`). Na segunda via, `roomType` vem com o
  número do quarto (`:146`).
- **O relatório financeiro deixa de fora a NON_REFUNDABLE cancelada** (`ReservationDAO.java:116`), embora o texto
  diga que ela continua contando.
- **Reserva pendente cancelada fica com `refund` null**, e o roteiro espera 0.00. O texto diz que nada é devolvido e
  que `refund` é null "enquanto não for cancelada", então null depois do cancelamento contraria o texto, mas pesa
  pouco perto do resto.

## Dúvidas

- **A classe do pattern ficou entre malfeito e omissão.** A estrutura de ouvintes existe e o serviço anuncia sem
  conhecer os destinos, o que afasta a omissão. Mas o único ouvinte é um "notificador" que conhece a governança e o
  financeiro, e o hóspede fica fora da lista. Dei malfeito porque a lista e o registro na montagem existem de verdade
  (`NotificationService.java:16-24`, `Main.java:28`).
- **O exagero:** `RoomService` (que só repassa) e `Notification` (cópia de `Message`) poderiam ser lidos como
  estrutura sem necessidade. Considerei os dois pequenos demais para marcar exagero.
