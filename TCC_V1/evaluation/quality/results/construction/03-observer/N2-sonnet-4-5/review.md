# Revisão: construção, cenário 03

## Visão geral

O projeto tem sete pacotes: `api` (os handlers do Javalin), `api.dto`, `service`, `service.observer`, `domain`,
`repository` e a raiz (`App`, a montagem, e `DatabaseSetup`, o esquema). Uma requisição entra pelas rotas de
`App.java:49-59`, passa por um controller (`api/ReservationController.java`), que chama o `ReservationService`. O
serviço usa a entidade `Reservation`, que calcula o preço e as mudanças de situação, e grava pelo
`ReservationRepository`, que usa JDBC puro com `DriverManager`. Os dados ficam no PostgreSQL, em três tabelas criadas
com `CREATE TABLE IF NOT EXISTS` (`DatabaseSetup.java:23-56`). Os avisos da confirmação e do cancelamento estão bem
resolvidos com um Observer: são três ouvintes registrados em `App.java:27-30`, e as 15 conferências do cenário
passaram. Há três problemas sérios. Primeiro, nada impede duas reservas do mesmo quarto na mesma noite: nas 10
rodadas de pedidos simultâneos, as duas reservas foram criadas. Segundo, o banco não guarda o preço da reserva.
Subtotal, desconto, total e até o valor pago são recalculados a cada leitura a partir das diárias que estão no
código. Terceiro, o único teste pela API e pelo banco nunca roda: ele precisa de Docker e, mesmo com Docker, passa a
configuração por `System.setProperty`, que o `App` não lê. Por isso o `mvn verify` falha.

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, nenhum ciclo. O Javalin só aparece em `api` e na raiz, e o JDBC só
em `repository` e na raiz (`DatabaseSetup`). A maior classe é `ReservationRepository`, com 244 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: de `api` para `service`, de `service` para `repository` e
de `repository` para `domain`. A montagem fica num lugar só (`App.java:20-39`), e os serviços recebem as dependências
pelo construtor (`service/ReservationService.java:12-15`). As regras não conhecem o Javalin nem o SQL. Os problemas
estão localizados:

- Há uma regra de negócio dentro de um handler. A regra "a segunda via só existe para reserva paga" está no
  controller (`api/ReservationController.java:150-156`), e não no serviço.
- Parte da validação da reserva também ficou no controller (`api/ReservationController.java:24-37`), e o serviço
  repete uma parte dela (`service/ReservationService.java:29-31`).
- A camada de serviço é pulada em `api/MessageController.java:4,17`: o handler fala direto com o
  `MessageRepository`. É pouco, mas é a única rota assim.
- A camada de dados reconstrói o objeto repetindo as transições de situação: ela chama `confirm()`, `checkIn()`,
  `checkOut()` e `cancel()` em sequência (`repository/ReservationRepository.java:225-240`). Essa lógica de domínio
  está no lugar errado e é frágil, como se vê na coluna do banco.

## Design de baixo nível

**Números:** a interface `ReservationObserver` tem 3 ouvintes: hóspede, governança e financeiro. Os 5 avisos pedidos
estão todos nos ouvintes (2 da confirmação para equipe, 2 do cancelamento, mais o do hóspede). Nenhum aviso é
montado fora da estrutura. O `ReservationService` não conhece nenhum destinatário.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A interface `service/observer/ReservationObserver.java:5-8` tem dois eventos,
`onReservationConfirmed` e `onReservationCancelled`. Cada interessado é uma classe:

- `GuestNotificationObserver` monta o e-mail com o comprovante;
- `HousekeepingNotificationObserver` cuida da governança, na confirmação e no cancelamento;
- `FinanceNotificationObserver` cuida do pagamento e do reembolso, e só avisa quando há o que devolver
  (`FinanceNotificationObserver.java:37`).

O sujeito, `service/NotificationService.java:11-27`, percorre a lista de ouvintes. A lista é preenchida de fora, na
montagem (`App.java:27-30`), e o `ReservationService` só anuncia o evento (`ReservationService.java:53,71`). O
e-mail do hóspede também é um ouvinte. Para pôr o restaurante ou o transfer, basta criar uma classe e acrescentar uma
linha em `App`, sem tocar no serviço.

Dois pontos afastam um pouco da forma ideal, sem mudar a classe:

- A regra "reserva pendente cancelada não avisa ninguém" não é decidida pelo sujeito. Ela vai como o parâmetro
  `wasConfirmed` (`NotificationService.java:23`), e cada ouvinte precisa conferir sozinho
  (`HousekeepingNotificationObserver.java:40`, `FinanceNotificationObserver.java:37`).
- A interface obriga todo ouvinte a implementar os dois eventos, e por isso o do hóspede tem um método vazio
  (`GuestNotificationObserver.java:42-44`).

**Exagero.** Não há interface com uma implementação só, fábrica ou builder. O único caso discutível é o
`RoomService`, que só repassa as chamadas ao repositório (`service/RoomService.java:16-22`). Esse ponto está nas
Dúvidas.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo
(`domain/Reservation.java:52-58,73-74`), `LocalDate` para as datas, e enums para a situação, o plano e o tipo de
quarto. Os defeitos são estes:

- **Validação de situação duplicada.** O serviço confere a situação (`ReservationService.java:46,62,80,94`) e a
  entidade confere de novo (`Reservation.java:79,87,94,101`), com as mesmas mensagens.
- **Formatação copiada.** `formatMoney`, `PT_BR` e o formatador de datas estão repetidos em vários ouvintes
  (`GuestNotificationObserver.java:13-14,76-78`, `FinanceNotificationObserver.java:12,56-58`,
  `HousekeepingNotificationObserver.java:11`).
- **Comprovante preso ao e-mail.** O texto do comprovante só existe dentro do ouvinte do hóspede
  (`GuestNotificationObserver.java:46-66`), mas ele é um documento do domínio.
- **`catch (Exception)` genérico.** Em `ReservationController.java:70`, ele transforma qualquer falha em 400
  `INVALID_REQUEST`, inclusive uma queda do banco. A falta de nome, e-mail ou CPF só é recusada porque a coluna
  `NOT NULL` estoura e cai nesse `catch`. Não há validação explícita.
- **Quarto ausente vira 404.** Um `roomNumber` ausente vira `findByNumber(null)` e responde 404 `ROOM_NOT_FOUND`
  (`ReservationController.java:39-40`), quando devia ser 400.
- **Tratamento de erro repetido.** Os mesmos blocos `catch` aparecem nos seis handlers
  (`ReservationController.java:81-85,93-99,107-113,121-127,135-141,159-163`), sem um tratador de exceções do Javalin.
- **Sobras.** Há uma variável sem uso, `wasConfirmed`, em `Reservation.java:105`. O `amountPaid` nasce como
  `BigDecimal.ZERO` sem escala (`Reservation.java:43`) e sai no JSON como `0`, não como `0.00`.
- **Reconstrução frágil.** A entidade é reconstruída repetindo as transições
  (`ReservationRepository.java:225-240`). A condição do cancelamento, nas linhas 235-236, é difícil de seguir.

## Testes

**Números:** rodaram 13 testes. Os 12 de unidade passaram, e o de integração (`ReservationSystemIT`) deu erro por
falta de Docker. São 26 métodos de teste no código, 14 deles no IT, que nunca chegam a executar. A cobertura é de
11,2% das linhas (85 de 757) e 15,3% dos ramos (23 de 150). Os pacotes `api`, `service`, `service.observer` e
`repository` ficaram com 0%.
**Nota:** ruim

Os testes de unidade (`ReservationTest.java`) conferem com valores o preço básico (779,80, na linha 21), a alta
temporada (599,76, nas linhas 34 e 136-156), o desconto com meio para o par (74,97 e 424,83, nas linhas 44-46), a
devolução no cancelamento (linhas 66-91) e algumas transições de situação.

Tudo o que passa pela API, pelo banco e pela parte nova do cenário está só em `ReservationSystemIT.java`, e esse
teste não funciona por dois motivos:

1. Ele depende de Testcontainers e Docker (`ReservationSystemIT.java:20-24`), e não do banco de `DB_URL` que o texto
   promete. O log (`measures/tests-maven.log`) mostra "Could not find a valid Docker environment", e o
   `mvn verify` termina com erro. O texto exige que ele passe.
2. Mesmo com Docker, o teste está quebrado. Ele passa banco e porta por `System.setProperty`
   (`ReservationSystemIT.java:34-37`), mas o `App` lê `System.getenv` (`App.java:15-18`). O serviço subiria na porta
   7070 contra `localhost:5432/pousada`, e o RestAssured chamaria a porta 7071. Isso mostra que esse teste nunca rodou
   com sucesso.

A pergunta da mutação na parte nova tem resposta ruim. Nenhum teste que roda falharia se alguém fizesse uma destas
mudanças:

- apagasse o `if (!wasConfirmed) return` da governança (`HousekeepingNotificationObserver.java:40-42`), que faz a
  pendente cancelada avisar;
- trocasse `== 0` por `!= 0` no financeiro (`FinanceNotificationObserver.java:37`);
- tirasse um ouvinte de `App.java:28-30`.

As recusas por código de erro, os relatórios CSV e o conflito de datas também ficam sem rede. Na temporada, trocar
`day >= 15` por `day > 15` (`Reservation.java:65`) também passaria, porque o teste de dezembro usa o dia 20.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations` e `messages`), todas com chave primária. Os dois campos de dinheiro
gravados são `numeric(10,2)`. Nenhuma chave estrangeira, nenhum CHECK, nenhum UNIQUE ou EXCLUDE. O esquema é criado
no código, sem migração. Há 11 chamadas a `DriverManager.getConnection`, nenhum pool, nenhuma transação explícita e
nenhum `FOR UPDATE`. Nenhum SQL é montado com `+`.
**Nota:** ruim

- **Reserva em dobro.** Nada impede duas reservas do mesmo quarto na mesma noite. O serviço consulta o conflito
  (`ReservationRepository.java:114-148`) e depois insere (`ReservationRepository.java:30-65`), em duas conexões
  separadas, sem transação, trava ou restrição. Também não há `synchronized` (`ReservationService.java:21-35`). O
  roteiro do comportamento criou as duas reservas em 10 de 10 rodadas simultâneas. É dado errado, e acontece na
  operação mais comum do sistema.
- **Preço não guardado.** A tabela `reservations` não guarda subtotal, desconto nem total
  (`DatabaseSetup.java:31-45`). Ao ler, `buildReservation` recalcula tudo a partir de `RoomType`, que está no código
  (`ReservationRepository.java:211-221`, `domain/RoomType.java:6-8`). Até o `amount_paid` gravado é ignorado: ele
  volta como o `total` recalculado por `confirm()`, e a coluna só serve para comparar nas linhas 235-236. Se o dono
  mudar uma diária, as reservas antigas, o valor pago e o relatório financeiro mudam junto.
- **Sem chave estrangeira.** `reservations.room_number` não está ligado a `rooms` (`DatabaseSetup.java:33`), e o tipo
  do quarto fica copiado em `reservations.room_type`. As colunas de situação, plano e canal são `varchar` sem CHECK.
- **Esquema que não evolui.** O esquema é criado com `CREATE TABLE IF NOT EXISTS` (`DatabaseSetup.java:24,31,49`),
  que não sabe acrescentar uma coluna numa tabela que já existe. Não há Flyway nem migrações.
- **Sem transação entre as escritas.** A confirmação grava a situação (`ReservationService.java:51`) e depois cada
  ouvinte grava a sua mensagem numa conexão própria (`MessageRepository.java:26`). Se uma gravação falhar no meio, a
  reserva fica confirmada sem os avisos.
- **Detalhes menores.** Não há pool: cada operação abre uma conexão nova. O relatório mensal filtra com
  `EXTRACT(... FROM check_in)` (`ReservationRepository.java:185-186`), que não usa índice.

Os pontos bons são o SQL sempre com `?`, as conexões fechadas com try-with-resources, as datas em `date` e o
dinheiro em `numeric(10,2)`. Os dados continuam lá depois de reiniciar o serviço.

## Comportamento

**Números:** 62 de 66 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 43 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 03 | 15 de 15 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

A parte nova funciona inteira:

- na confirmação, os e-mails do hóspede, da governança (com as datas e os hóspedes) e do financeiro (com R$ 779,80);
- no cancelamento de reserva confirmada, os avisos à governança e ao financeiro;
- o financeiro não é avisado quando não há nada a devolver;
- nenhum aviso quando a reserva cancelada ainda estava pendente.

As falhas:

- **Reservas simultâneas.** Em 10 de 10 rodadas, dois pedidos ao mesmo tempo criaram duas reservas para o mesmo
  quarto e as mesmas noites. Para o dono, isso é overbooking real quando dois hóspedes reservam pelo site ao mesmo
  tempo. O defeito pesa na coluna do banco, e aqui ele aparece como falha do roteiro.
- **`amountPaid` como `0`.** Antes da confirmação, o JSON traz `"amountPaid":0`, e não `0.00` como o texto pede
  (`domain/Reservation.java:43`). É um erro pequeno, mas é do agente. O mesmo deve acontecer com o `refund` de uma
  pendente cancelada, que copia esse zero.
- **"o 201 comporta 3" e "a diária do 201 é 389.90".** As duas falharam porque o projeto devolve `number` como texto,
  `"201"` (`api/dto/RoomDTO.java:8`), e o roteiro procura o número 201. O texto não diz o tipo do campo, e o
  `roomNumber` da reserva também é texto, de forma coerente. É uma escolha razoável num ponto aberto e não pesa como
  erro.

## Dúvidas

- **Exagero.** O `RoomService` (`service/RoomService.java:16-22`) é uma camada que só repassa ao repositório. Ela
  poderia contar como exagero, mas é uma classe só e pequena, que segue o caminho controller → serviço → repositório
  do resto. Por isso fiquei com `contenção`.
- **Tipo do número do quarto.** A falha das duas conferências do quarto 201 depende do tipo esperado para `number`.
  Tratei como ponto aberto pelo texto.
