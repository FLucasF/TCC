# Revisão: construção, cenário 03

## Visão geral

O projeto (`projeto/`) tem 7 pacotes e cerca de 1.240 linhas de Java em `src/main`. Uma requisição entra pelo
`web/WebApp.java`, que só lê a requisição (o corpo do POST é validado em `web/ReservationRequestParser.java`), chama o
`app/ReservationService.java` e escreve a resposta. As regras de preço, de situação e de devolução ficam no
`domain/` (`Reservation`, `Quote`, `Season`, `RatePlan`), que não usa nem Javalin nem JDBC. Os dados ficam no
PostgreSQL, em duas tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:28-55`),
e o acesso passa por `JdbcReservationRepository` e `JdbcMessageRepository`. Os quartos não estão no banco: são um
catálogo fixo no código (`domain/RoomCatalog.java:7-12`). Os avisos da confirmação e do cancelamento seguem o Observer:
um `Notifier` percorre uma lista de `Subscriber` montada no `Main` (`Main.java:28-33`). O roteiro de comportamento
passou inteiro (66 de 66), e os 14 testes passam. Os pontos fracos são todos moderados. No banco: o esquema é criado
no código e não tem migrações, a regra "um quarto por noite" só é protegida por um `synchronized` dentro do processo, e
a confirmação grava a reserva e os e-mails fora de uma transação. Na arquitetura há um ciclo pequeno entre `app` e
`notification`. Nos testes, a camada JDBC não tem nenhuma cobertura.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`app` ↔ `notification`) (`measures/dependencies.md`). Só
o `web` e o pacote raiz (`Main`) usam Javalin. Só a `persistence` e o `Main` (que abre o pool) usam JDBC. A maior
classe é o `JdbcReservationRepository` (143 linhas).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota tem uma linha e chama o serviço (`web/WebApp.java:53-66`). Os erros viram
  `{error, message}` num lugar só (`web/WebApp.java:68-73`), a partir do status HTTP que fica no próprio
  `ErrorCode` (`domain/ErrorCode.java:4-9`).
- **As regras estão separadas.** O serviço (`app/ReservationService.java`) não conhece nem `Context` nem SQL. As
  transições e a devolução ficam na `Reservation` (`domain/Reservation.java:39-57`), e o preço fica em
  `Quote`/`Season`/`RatePlan`.
- **Os dados estão isolados.** As interfaces de repositório estão em `app/ReservationRepository.java` e
  `app/MessageRepository.java`, e as implementações JDBC ficam só em `persistence/`.
- **A montagem acontece num lugar só,** o `Main.java:21-36`, sem `static` mutável e sem `new` de repositório dentro
  dos serviços.
- **O ciclo:** o serviço importa `notification.Notifier` e `notification.ReservationEvent`
  (`app/ReservationService.java:10-11`). Na outra direção, o `notification` importa `app.Notification` e
  `app.MessageRepository` (`notification/Notifier.java:3-4`, `notification/Subscriber.java:3`). O evento e a mensagem
  a enviar estão no pacote errado: se `ReservationEvent` e `Notification` ficassem do mesmo lado (ou o serviço
  dependesse de uma interface de publicação em `app`), o ciclo sumiria. O ciclo é pequeno e fica entre dois pacotes
  vizinhos, mas é o único defeito estrutural do projeto, e é ele que tira o `bom`.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança e financeiro), cada um numa classe que implementa `Subscriber`; 2
eventos (`Confirmed` e `Cancelled`); nenhum e-mail montado fora de um ouvinte; a lista de ouvintes é montada no `Main`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern pedido (Observer).** O serviço só anuncia o que aconteceu, com `notifier.publish(new
  ReservationEvent.Confirmed(...))` e `...Cancelled(...)` (`app/ReservationService.java:47,55`). Ele não sabe quem é
  avisado.
  - O `Notifier` recebe a lista de fora (`notification/Notifier.java:13-16`) e entrega o evento a cada ouvinte
    (`notification/Notifier.java:18-24`).
  - Cada interessado é uma classe: `GuestSubscriber`, `HousekeepingSubscriber` e `FinanceSubscriber`. O e-mail do
    hóspede também é um ouvinte (`notification/GuestSubscriber.java:19-28`), e não uma chamada direta.
  - A interface tem métodos `default` vazios por evento (`notification/Subscriber.java:13-19`). Com isso, um ouvinte
    novo (o restaurante, o transfer) só implementa o evento que lhe interessa e entra na lista em `Main.java:28-31`.
  - A regra "sem devolução, não avisa o financeiro" está dentro do próprio ouvinte
    (`notification/FinanceSubscriber.java:24-26`).
  - O serviço decide apenas que o cancelamento de uma reserva pendente não gera evento
    (`app/ReservationService.java:52-56`). Isso é a definição do evento, e não um conhecimento sobre os destinatários.
  - O `switch` em `notification/Notifier.java:27-30` escolhe o método pelo tipo de evento, numa interface `sealed`, e
    não pelo destinatário; não afasta do pattern. A única ressalva é que um evento novo exige mexer nesse `switch` e
    na interface. É aceitável para dois eventos, e o compilador avisa se um caso faltar.
- **O exagero.** As duas interfaces de repositório têm uma segunda implementação de verdade, a dos testes
  (`src/test/.../InMemoryRepositories.java`). `Quote`, `Csv` e `RoomView` cumprem um papel cada um. Não há fábrica,
  builder nem camada que só repasse. Por isso, contenção.
- **O resto do código.**
  - Os tipos estão certos: `BigDecimal` com `HALF_EVEN` num lugar só (`domain/Money.java:13-15`), arredondando a cada
    passo (`domain/Season.java:24`, `domain/RatePlan.java:18`, `domain/Quote.java:16`); `LocalDate`; e `enum` para
    situação, plano, tipo de quarto e canal.
  - A `Reservation` é um `record` imutável, com transições que devolvem uma cópia (`domain/Reservation.java:39-57`).
  - Os métodos são curtos, e não há duplicação que pese.
  - Há defeitos pequenos:
    - `Reservation.requireStatus` é público sem precisar (`domain/Reservation.java:73`);
    - o `cancel` lê a reserva duas vezes (`app/ReservationService.java:52-53`);
    - o `WebApp` usa o `ReservationRequestParser.invalid` como utilitário de erro (`web/WebApp.java:88,94`);
    - o `id` da reserva é um `Long` que pode ser nulo antes de gravar (`domain/Reservation.java:9,29`).

    Nada disso pediria mudança antes de aceitar.

## Testes

**Números:** 14 testes rodaram, 0 falharam, 0 erros, 0 pulados; 14 métodos de teste escritos. Cobertura de 67,7% das
linhas (296 de 437) e 80,6% dos ramos (79 de 98). Por pacote: `app`, `domain`, `notification` e `report` ficam entre
92% e 100%; `persistence` fica em 0 de 118 linhas (`measures/tests.md`).
**Nota:** com problemas

- **O que está bem testado.** As regras pedidas são conferidas com valores:
  - o exemplo do texto (599,76) e as bordas da temporada, inclusive 29/02 e 14/12 contra 15/12
    (`PricingTest.java:18-33`);
  - a estadia que atravessa o início da temporada (`PricingTest.java:36-40`);
  - o arredondamento meio para o par, com 37,485 virando 37,48 (`PricingTest.java:43-48`);
  - todo o ciclo de vida e as recusas 409 (`SystemTest.java:223-240`);
  - os códigos de erro (`SystemTest.java:243-257`);
  - a sobreposição e as estadias encostadas (`SystemTest.java:260-266`);
  - os dois CSVs comparados por inteiro (`SystemTest.java:269-297`).
- **A parte nova também é testada pela API:**
  - na confirmação, o e-mail do hóspede é comparado por inteiro, e são conferidos os assuntos e as datas da governança
    e o valor do financeiro (`SystemTest.java:159-187`);
  - no cancelamento, o reembolso no plano STANDARD (`SystemTest.java:190-201`);
  - o financeiro em silêncio no plano NON_REFUNDABLE (`SystemTest.java:204-212`);
  - nenhum aviso para uma reserva pendente (`SystemTest.java:215-220`).
- **O que fica sem rede (a pergunta da mutação):**
  - se alguém apagasse a linha "Hóspedes" do e-mail da governança (`notification/HousekeepingSubscriber.java:19`),
    nenhum teste falharia. O teste só confere as datas (`SystemTest.java:183-184`);
  - se o e-mail de reembolso trouxesse o valor errado (`notification/FinanceSubscriber.java:28`), por exemplo
    `amountPaid` no lugar de `refund`, ou se o aviso de cancelamento da governança não trouxesse as datas, nenhum teste
    falharia. O teste do cancelamento só confere os assuntos (`SystemTest.java:198-200`).
- **O banco não é testado.** Os testes trocam o banco por repositórios em memória (`SystemTest.java:91-92`), e por
  isso o SQL nunca roda. A consulta de sobreposição, com os parâmetros invertidos de propósito
  (`persistence/JdbcReservationRepository.java:73-78`), e o mapeamento das 16 colunas ficam sem nenhum teste. O
  roteiro de comportamento mostra que esse código está certo, mas nenhum teste do agente pegaria uma regressão nele.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), as duas com chave primária. As conferências acusaram:

- nenhuma migração: o esquema é criado por `CREATE TABLE` em `persistence/Database.java`;
- `status`, `rate_plan` e `channel` em `text`, sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`.

O dinheiro está todo em `numeric(12,2)`, as datas em `date`, e não há chave estrangeira (`measures/database.md`).
**Nota:** com problemas

- **Está certo:**
  - os tipos (`persistence/Database.java:35-45`);
  - `NOT NULL` em tudo o que é obrigatório; só `refund` aceita nulo, de propósito;
  - um índice por quarto (`persistence/Database.java:47`);
  - o SQL todo com `?`. As concatenações que o script listou (`persistence/JdbcReservationRepository.java:62,68,74`)
    só juntam constantes;
  - o pool Hikari e try-with-resources em todo acesso.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:28-55`) não sabe
  acrescentar uma coluna a uma tabela que já existe. O próximo pedido que mudar o banco vai precisar de um mecanismo
  de migração que o projeto não tem.
- **A regra "um quarto por noite" só vale dentro do processo.** Quem impede a duplicação é o `synchronized` em
  `app/ReservationService.java:26`, que junta numa só operação a consulta de `existsActiveOverlap` e o `insert`. Não
  há restrição `EXCLUDE` nem trava no banco. Com duas instâncias do serviço, duas reservas do mesmo quarto passariam.
  O roteiro, com uma instância, passou.
- **A confirmação não tem transação.** Ela grava a reserva (`app/ReservationService.java:46` →
  `persistence/JdbcReservationRepository.java:47-58`) e depois cada mensagem numa conexão própria
  (`persistence/JdbcMessageRepository.java:25-41`), sem transação. Se uma gravação de mensagem falhar, a reserva fica
  confirmada, e o cliente recebe 500 com parte dos avisos perdida.
- **Faltam restrições.** Não há CHECK em `status`, `rate_plan` nem `guests`. Também não há tabela de quartos nem
  chave estrangeira, porque os quartos ficam no código (`domain/RoomCatalog.java:7-12`). Para quartos que "quase nunca
  mudam", é uma escolha defensável, mas o banco aceita `room_number` de um quarto que não existe.
- **A carga.** Os dois relatórios trazem a tabela inteira e filtram em Java (`report/OccupancyReport.java:21-24`,
  `report/RevenueReport.java:20-23`, via `persistence/JdbcReservationRepository.java:67-68`). Isso cresce sem limite
  com o tempo.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam (`measures/behavior.md`):

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- os preços, com a temporada e o meio para o par;
- as recusas, com os códigos certos;
- o ciclo da reserva, o comprovante e a segunda via;
- os dois relatórios em CSV;
- os dados continuam lá depois de reiniciar.

A parte nova também funciona. Na confirmação, saem os três e-mails, com as datas, o número de hóspedes e o valor pago.
No cancelamento de uma reserva confirmada, a governança é avisada com as datas, e o financeiro é avisado só quando há
algo a devolver. Uma reserva pendente cancelada não gera aviso nenhum.

As dez rodadas de reservas simultâneas não criaram nenhuma duplicada. A proteção, porém, é um `synchronized` dentro
do processo, o que conta na coluna do banco.

## Dúvidas

O ciclo `app` ↔ `notification` é pequeno, e o resto da arquitetura é limpo. Entre `bom` e `com problemas`, ficou `com
problemas` porque a skill põe "um ciclo novo entre dois pacotes" nessa nota. Nos testes, a nota `com problemas` também
fica perto da fronteira: as regras centrais estão bem conferidas, e o que pesou foi a camada JDBC sem nenhum teste e
dois conteúdos dos avisos novos (o número de hóspedes e o valor do reembolso) sem asserção.
