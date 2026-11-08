# Revisão: construção, cenário 03

## Visão geral

O projeto tem 7 pacotes e 25 classes em `src/main` (cerca de 1.280 linhas): `Main` monta as rotas do Javalin com
handlers em lambdas (`Main.java:42-193`), que chamam métodos `static` de `service.ReservationService` e
`service.ReportService`; estes chamam repositórios JDBC também `static` (`db/*Repository.java`) sobre um pool Hikari
guardado num campo `static` (`db/Database.java:9`). Os dados ficam no PostgreSQL, em três tabelas criadas com
`CREATE TABLE IF NOT EXISTS` no código (`db/Database.java:26-65`). Os avisos do cenário ficam em `notification/`: uma
interface de ouvinte com dois métodos e dois ouvintes (governança e financeiro), registrados num despachante `static`
no `main` (`Main.java:39-40`). O mais sério: **o `pom.xml` não tem o Jackson e o JSON é escrito com `ctx.json(...)`
(`Main.java:46`, `71`, `85`...), então todas as respostas em JSON da API dão 500** ("It looks like you don't have an
object mapper configured"). Só 7 das 66 conferências do comportamento passam: o sistema sobe, grava no banco, mas não
responde nada útil ao site. Por baixo, a lógica funciona em boa parte (conferi no banco, à mão, os e-mails gerados).
Além disso, a confirmação exige no corpo um `amountPaid` que o texto não prevê (`Main.java:94-95`), o e-mail do
hóspede não passou pela estrutura de ouvintes, e não há nenhum teste.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só no pacote
raiz (`Main`); JDBC em `db`, mas também aparece em `service` e `notification` (por causa do `SQLException` nas
assinaturas). A maior classe é `Main.java` (298 linhas), seguida de `ReservationService.java` (213).
**Nota:** com problemas

As camadas existem e apontam na direção certa: a borda (`Main`) → `service` → `db`, sem ciclo, e os serviços não
conhecem o Javalin nem montam SQL (o SQL está todo em `db/*Repository.java`). Os defeitos:

- **Não há montagem:** tudo é `static`. `ReservationService` chama `ReservationRepository.findById(...)` direto
  (`service/ReservationService.java:92`), o pool é um `static` global (`db/Database.java:9`, `95-97`) e o despachante
  de eventos também (`notification/ReservationEventDispatcher.java:8`). Nada é criado e ligado num lugar; só os dois
  ouvintes são registrados no `main` (`Main.java:39-40`). Isso impede trocar uma peça (por exemplo, o banco num teste),
  e é provavelmente por isso que não há teste nenhum.
- **Regra e acesso a dados na borda:** a regra da segunda via (só reserva paga) está no handler, e testada pelo
  `amountPaid` e não pela situação (`Main.java:147-148`); o handler busca o tipo do quarto direto no repositório
  (`Main.java:237-245`), duplicando `ReservationService.getRoomType` (`service/ReservationService.java:208-212`).
- **Os códigos de erro viajam como texto** na mensagem de `IllegalArgumentException`
  (`service/ReservationService.java:31`, `37`, `46`, `96`) e são traduzidos por um `switch` de strings na borda
  (`Main.java:247-266`): qualquer `IllegalArgumentException` de outra origem (um `RatePlan.valueOf` com valor errado,
  `Main.java:65`) sai como 400 com a mensagem interna.
- `Main.java` concentra rotas, validação, conversão para JSON (`Main.java:199-235`) e os adaptadores do Gson
  (`Main.java:270-298`).

## Design de baixo nível

**Números:** dos três interessados que o texto descreve, dois (governança e financeiro) são ouvintes
(`notification/HousekeepingNotifier.java`, `notification/FinanceNotifier.java`); o e-mail do hóspede fica fora da
estrutura, com chamada direta no serviço (`service/ReservationService.java:103`, `167-181`). Os dois eventos
(confirmação e cancelamento) passam pelo despachante (`service/ReservationService.java:104`, `158`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Observer: há uma interface de ouvinte com `onConfirmed` e `onCancelled`
(`notification/ReservationEventListener.java:5-8`), uma implementação por interessado, cada uma montando o seu
e-mail; o serviço só anuncia (`fireOnConfirmed`/`fireOnCancelled`) e não sabe quem é avisado, e os ouvintes são
registrados no `main` (`Main.java:39-40`), com `addListener`/`removeListener` para o restaurante e o transfer entrarem
e saírem. O cancelamento também passa pela estrutura, e a regra "só avisa o financeiro se há o que devolver" mora no
ouvinte (`notification/FinanceNotifier.java:20`). O que afasta: o hóspede, que é um interessado na confirmação como os
outros, continua com chamada direta — o serviço monta o texto do e-mail e grava a mensagem ele mesmo
(`service/ReservationService.java:103`, `167-181`). É exatamente o caso que a referência chama de malfeito ("o
hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes"). Pesa também, em menor grau, que o
serviço não recebe a lista de ouvintes de fora: ela é uma `ArrayList` `static` global
(`notification/ReservationEventDispatcher.java:8`), mutável e sem proteção para uso concorrente. E a governança
descobre se a reserva cancelada estava confirmada pelo `amountPaid > 0`
(`notification/HousekeepingNotifier.java:25`), um indício indireto em vez do evento dizer de que situação a reserva
saiu.

**O exagero:** nenhuma estrutura sem necessidade — sem fábricas, builders ou interfaces de uma implementação só fora
do pattern. Há código morto (o record `model/Document.java` nunca é usado, `ReservationRepository.findByRoomAndDate`
em `db/ReservationRepository.java:79` e `DateFormatter.formatIsoYearMonth` em `util/DateFormatter.java:24` também
não), mas isso é sobra, não estrutura.

**O resto do código:**

- Tipos certos no essencial: `BigDecimal` com `HALF_EVEN` a cada passo (`util/Money.java:7-29`, usado em
  `service/ReservationService.java:53-65`), `LocalDate`, `enum` para situação, plano e tipo de quarto.
- Estado sem controle: `Reservation` tem todos os campos públicos e mutáveis (`model/Reservation.java:7-22`), e o
  serviço muda a situação atribuindo direto (`service/ReservationService.java:99-100`, `117`, `155`); as transições
  ficam espalhadas em `if`s por método, e `cancelReservation` lista as situações proibidas em vez das permitidas
  (`service/ReservationService.java:139-153`, com dois ramos que dão o mesmo `refund = ZERO`).
- A confirmação aceita o valor pago do cliente (`Main.java:94-95`, `service/ReservationService.java:100`) em vez de
  usar o total, como o texto diz; sem corpo, `body.get(...)` dá `NullPointerException` → 500.
- Erros: `catch (Exception e)` em todas as rotas, devolvendo `e.getMessage()` ao cliente com o código genérico
  `ERROR` (`Main.java:47-48`, `74-75` e outras); data fora do formato (`DateTimeParseException`, `Main.java:63`,
  `173`, `187`) e corpo que não é JSON (`JsonSyntaxException`, `Main.java:54`) caem aí e dão 500 em vez de 400.
- Duplicação: `getRoomType` duas vezes (`Main.java:237`, `service/ReservationService.java:208`), `sendMessage`
  idêntico nos dois ouvintes (`notification/FinanceNotifier.java:27-33`, `notification/HousekeepingNotifier.java:36-42`).
- Nome que engana: `DateFormatter` também formata dinheiro (`util/DateFormatter.java:16-18`), e com o padrão `0.00`,
  sem separador de milhar — R$ 1234,56 em vez do R$ 1.234,56 que o texto pede (`util/DateFormatter.java:10`).

Os defeitos são vários, mas o código é pequeno e legível; não chega a ser difícil de mudar com segurança, por isso
`com problemas` e não `ruim`.

## Testes

**Números:** 0 testes rodaram (0 falhas); 0 métodos de teste escritos; não há `src/test`; sem relatório de cobertura
(`measures/tests.md`). O `mvn verify` passa (`BUILD SUCCESS` em `measures/tests-maven.log`) só porque não há o que
testar.
**Nota:** ruim

Não há teste nenhum: nem do preço com a temporada e o arredondamento, nem das situações da reserva, nem dos códigos de
erro, nem da parte nova (os avisos da governança e do financeiro). Pela pergunta da mutação, qualquer mudança passaria
sem ser vista — trocar o `> 0` de `notification/FinanceNotifier.java:20` por `>= 0` (avisando o financeiro de reembolso
zero), apagar o `fireOnCancelled` de `service/ReservationService.java:158` ou trocar `1.20` em
`service/ReservationService.java:56`. A ausência de testes é também o que deixou passar o defeito que derruba a API
inteira (a falta do Jackson): um único teste pela API teria falhado.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)` em
todas as 6 colunas; datas em `date`; 1 chave estrangeira (`reservations.room_number → rooms`), coberta por índice;
nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`; nenhuma transação explícita, nenhum `FOR UPDATE`; pool Hikari; nenhum SQL
montado com `+` com dado de fora (`measures/database.md`).
**Nota:** com problemas

Os dados estão bem guardados: tipos certos, `NOT NULL` no que é obrigatório (`db/Database.java:33-51`), SQL sempre com
parâmetros (`db/ReservationRepository.java:17-20`, `65-66`, `100-101`), conexões em try-with-resources e um pool
(`db/Database.java:12-19`). As mensagens sobrevivem ao reinício (conferência "as mensagens continuam lá" passou), e
conferi à mão que as reservas e os avisos ficam gravados. Os defeitos:

- **O esquema é criado no código** com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:26-65`), sem migrações: a
  próxima coluna nova não chega a um banco que já existe.
- **A regra de "um quarto, uma reserva por noite" não é protegida:** o serviço consulta os conflitos e depois insere
  (`service/ReservationService.java:44-47` e `86`) em conexões separadas, sem transação, trava ou restrição de exclusão
  no banco; duas reservas simultâneas podem passar as duas. O roteiro das reservas simultâneas não chegou a medir isso,
  porque as duas respostas deram 500 (o defeito do JSON).
- **Escritas que deviam andar juntas não andam:** a confirmação grava a situação (`service/ReservationService.java:101`)
  e depois os e-mails, cada um em sua conexão (`db/MessageRepository.java:11-26`); uma falha no meio deixa a reserva
  confirmada sem os avisos.
- Situação, plano e tipo em `varchar` sem `CHECK` (`db/Database.java:28`, `42`, `47`).
- O índice parcial `WHERE status != 'CANCELLED'` (`db/Database.java:61-62`) é uma boa escolha para a consulta de
  conflito.

## Comportamento

**Números:** 7 de 66 conferências passaram (`measures/behavior.md`): parte comum 4 de 46; dados depois de reiniciar
1 de 3; cenário 03 1 de 15; reservas simultâneas 1 de 2.
**Nota:** ruim

O serviço sobe e cria o banco, mas **toda resposta em JSON dá 500**: o Javalin 6 precisa do `jackson-databind` para o
`ctx.json(...)`, e o `pom.xml` não o traz (só Gson, que o agente usa para ler, não para escrever). A mensagem de erro
do Javalin aparece no próprio corpo da resposta ("It looks like you don't have an object mapper configured"). Com
isso, `GET /rooms`, a criação, a consulta, a confirmação, o check-in, o check-out, a segunda via, `GET /messages` e
todos os erros com código (`ROOM_NOT_FOUND`, `CAPACITY_EXCEEDED`, `INVALID_REQUEST`...) falham. Pior: `POST
/reservations` grava a reserva antes de falhar ao responder, então o site recebe 500 de uma reserva que existe. Só
passaram o que não depende de JSON (os cabeçalhos dos relatórios em CSV) e as conferências de "nada aconteceu" (nenhuma
reserva dupla, nenhum aviso de pendente cancelada) — que passam porque nada funciona, não porque a regra existe.

Subindo o serviço à mão e olhando direto no banco, a lógica por baixo funciona em boa parte: a reserva STANDARD de
10 a 12/07/2027 fica com 599,76; ao confirmar a reserva 201 de 10 a 12/03/2027 (com `{"amountPaid":779.80}` no corpo)
saíram o e-mail do hóspede com o comprovante, "Reserva confirmada - quarto 201" para a governança (datas e "2
hóspede(s)") e "Pagamento recebido - reserva 1" para o financeiro (R$ 779,80); ao cancelar, "Reserva cancelada -
quarto 201" e "Reembolso - reserva 1" (R$ 779,80). Mas mesmo com o JSON consertado restariam erros que o roteiro
pegaria: a confirmação sem corpo, como o texto descreve, dá 500 (`Main.java:94-95`) e com corpo grava o valor que o
cliente mandar em vez do total; a segunda via de reserva não paga dá 422 em vez de 409 (`Main.java:148`) e é emitida
para uma reserva STANDARD cancelada (pois olha o `amountPaid`, não a situação); data, mês ou corpo fora do formato dão
500 em vez de 400; e os valores acima de mil saem sem o ponto de milhar (`util/DateFormatter.java:10`).

Para o dono da pousada: do jeito que está, o site não consegue usar o sistema para nada — nem listar os quartos.

## Dúvidas

- A conferência "cinco quartos" registrou 4 e a de "as mensagens continuam lá" passou; as duas leram corpos de erro do
  Javalin, não respostas do sistema, e não dizem nada sobre a regra. O banco tem os cinco quartos
  (`db/Database.java:77-81`).
- No design, a classe ficou em `malfeito` pela chamada direta do e-mail do hóspede, que a referência cita
  literalmente; o despachante `static` (que o serviço não recebe de fora) reforça a classe, mas não a decidiria sozinho.
