# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web`, `service`, `domain`, `notification`, `persistence` e a raiz com o `Main`. Uma requisição passa assim: a rota em `web/App.java:37-49` lê o corpo com o `RequestParser`, que valida e devolve um `NewReservation`, e chama o `ReservationService`. Cada operação do serviço roda numa transação (`persistence/Database.java:31-45`). As regras de preço, de situação e de devolução ficam no domínio, que é imutável (`domain/Reservation.java`, `domain/Pricing.java`). O SQL fica nos repositórios, e os dados (quartos, reservas e mensagens) ficam no PostgreSQL. Os avisos usam o Observer: o serviço publica um `ReservationEvent` e percorre uma lista de `Notifier` que recebe de fora (`service/ReservationService.java:122-128`, `Main.java:23`). As mensagens são gravadas na mesma transação da mudança de situação. O defeito mais sério está no banco: o esquema é criado com `CREATE TABLE IF NOT EXISTS` dentro do código, sem migrações, e nenhuma restrição do banco protege as situações nem os tipos. Nada quebra; as 66 conferências do roteiro passaram.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. O Javalin aparece só em `web` e na raiz, e o JDBC só em `persistence` e `service`. As maiores classes são `ReservationRepository` (144 linhas), `ReservationService` (129) e `Database` (128); o tamanho está bem distribuído.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha que lê, chama o serviço e escreve (`web/App.java:37-49`). O código de erro vira status HTTP num só `switch` (`web/App.java:85-92`). A validação da entrada fica isolada em `web/RequestParser.java:22-53`.
- **As regras não conhecem nem o Javalin nem o JDBC.** Isso vale para as transições e a devolução (`domain/Reservation.java:49-68`), para o preço e a temporada (`domain/Pricing.java:18-42`) e para o comprovante (`domain/Receipt.java:35-48`).
- **O acesso ao banco está isolado** em três repositórios e no `Database`. O serviço não monta SQL.
- **As peças são montadas num lugar só,** em `Main.java:18-26`, que cria os repositórios e os serviços e passa a lista de avisados.
- **Dois pontos pequenos, que não pedem mudança:**
  - O serviço recebe e repassa `java.sql.Connection` e declara `SQLException` (`service/ReservationService.java:16-17`, `:111`, `:117`, `:122`). O tipo do JDBC vaza para essa camada, mas sem SQL, como fronteira de transação.
  - O pacote `persistence` depende de `notification`, porque `MessageRepository` grava o `Message` (`persistence/MessageRepository.java:3-4`).

## Design de baixo nível

**Números:** há três interessados (hóspede, governança e financeiro) e dois eventos (confirmação e cancelamento). Os três interessados estão na estrutura, como implementações de `Notifier`; nenhum caso fica fora. O serviço não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern pedido.** É o Observer, aplicado direito:

- **Quem ouve.** `Notifier` é a interface do ouvinte (`notification/Notifier.java:9-13`), com uma implementação por interessado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. O e-mail do hóspede também é um ouvinte, não uma chamada direta (`notification/GuestNotifier.java:11-22`).
- **O que se anuncia.** O evento é um tipo selado com `Confirmed` e `Cancelled(wasConfirmed)` (`notification/ReservationEvent.java:7-19`).
- **O serviço não sabe quem é avisado.** Ele só cria o evento (`service/ReservationService.java:67` e `:78`) e percorre a lista que recebeu no construtor, copiada com `List.copyOf` (`service/ReservationService.java:34` e `:122-128`).
- **Quem registra.** A lista é montada em `Notifiers.standard()` (`notification/Notifiers.java:13-18`) e entregue pelo `Main` (`Main.java:23`).
- **O próximo interessado.** Para o restaurante ou o transfer, basta uma classe nova e uma linha na lista, sem mexer no serviço.
- **Quem decide se avisa.** Cada ouvinte decide sozinho se reage ao evento, por exemplo `notification/FinanceNotifier.java:23-29`, que só avisa quando há devolução. Ninguém de fora pergunta qual é o interessado.
- **O que afasta um pouco do ideal, sem mudar a classe.** O registro é uma lista fixa num método estático, e não algo que se configure por contrato. Mesmo assim, o restaurante e o transfer entram e saem trocando só essa lista.

**O exagero.** Não há estrutura sem necessidade:

- `Channel` tem um valor só, mas o texto pede o canal na mensagem (`notification/Channel.java:3-5`).
- `Database.Work` é a interface funcional que deixa a lambda lançar `SQLException` (`persistence/Database.java:26-29`).
- `NewReservation` é um record simples com os dados do pedido.
- Não há fábricas, builders nem camadas que só repassam.

**O resto do código.** Está bem cuidado:

- **Os tipos são os certos.** Dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:19-21`, `domain/Pricing.java:28` e `:39-40`), as datas são `LocalDate`, e as situações e os planos são `enum`. As regras de cada plano ficam no próprio `RatePlan` (`domain/RatePlan.java:5-24`).
- **O estado é controlado.** A reserva é imutável e cada transição devolve uma cópia (`domain/Reservation.java:85-88`). Não há `static` mutável.
- **As miudezas, nada que exija mudança:**
  - A validação de hóspedes e de datas aparece duas vezes, na borda e no domínio (`web/RequestParser.java:40-45` e `domain/Reservation.java:33-38`). É defensável, porque o domínio protege a própria regra.
  - O formatador de datas brasileiro mora em `Receipt` e é usado pela governança (`notification/HousekeepingNotifier.java:19-20`).
  - Os 16 campos da reserva são remontados à mão para colocar o id (`persistence/ReservationRepository.java:47-49`). Um `withId` evitaria isso.
  - O tratador genérico de erro usa `e.printStackTrace()` (`web/App.java:53`).

## Testes

**Números:** 16 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 16 métodos escritos: 10 em `DomainTest` (unidade) e 6 em `ApiTest`, que vai pela API, contra o PostgreSQL. A cobertura é de 93,7% das linhas (461 de 492) e 82,3% dos ramos (102 de 124). Em `notification` ela chega a 47 de 47 linhas e 15 de 16 ramos; em `domain`, a 89 de 90 linhas e 37 de 40 ramos.
**Nota:** bom

**As regras pedidas são conferidas com valores:**

- **O preço.** A temporada é testada nas bordas (`DomainTest.java:35-43`), assim como o exemplo do texto, R$ 599,76 (`DomainTest.java:46-52`). Há ainda uma estadia que cruza a mudança de temporada com o desconto e o arredondamento: 82,467 vira 82,47 (`DomainTest.java:55-61`), e o "meio para o par" (`DomainTest.java:64-67`).
- **O comprovante.** É comparado texto a texto, com e sem desconto, e com "1 diária" (`DomainTest.java:85-107`).
- **As situações e as devoluções.** Estão em `DomainTest.java:110-132`.
- **Todos os códigos de erro pela API.** Estão em `ApiTest.java:119-131`.
- **Os dois relatórios.** O CSV inteiro é comparado, inclusive a não reembolsável cancelada que continua contando (`ApiTest.java:215-243`).

**A parte nova do cenário também está coberta:**

- A confirmação gera as três mensagens, com os destinatários e os assuntos (`DomainTest.java:143-169`, `ApiTest.java:136-151`).
- O cancelamento de uma reserva confirmada avisa a governança e o financeiro (`DomainTest.java:171-175`).
- A não reembolsável cancelada avisa só a governança (`DomainTest.java:177-180`, `ApiTest.java:196-201`).
- A pendente cancelada não avisa ninguém (`DomainTest.java:182-184`, `ApiTest.java:177-182`).

**A pergunta da mutação:**

- Trocar `signum() > 0` por `>= 0` em `notification/FinanceNotifier.java:24` faria falhar `DomainTest.java:179`.
- Apagar o `when c.wasConfirmed()` de `notification/HousekeepingNotifier.java:28` faria falhar `DomainTest.java:184` e `ApiTest.java:182`.

**O que fica sem rede:**

- Apagar a linha "Hóspedes: N" do aviso à governança (`notification/HousekeepingNotifier.java:27`) não faz nenhum teste falhar. `DomainTest.java:166` confere só as datas.
- As datas do aviso de cancelamento à governança também não são conferidas.
- Não há teste de duas reservas simultâneas, nem de reinício.

**Defeitos menores:**

- Os testes de `ApiTest` dependem da ordem e do estado deixado pelo anterior (`ApiTest.java:28`, `@Order`).
- Sem `DB_URL`, eles são pulados em silêncio (`ApiTest.java:40`).

## Banco

**Números:**

- **As tabelas.** São 3 (`rooms`, `reservations` e `messages`), todas com chave primária.
- **Os valores em dinheiro.** As 6 colunas são `numeric(12,2)`.
- **As chaves estrangeiras.** Nenhuma está sem índice.
- **As datas.** Nenhuma está guardada como texto.
- **As restrições.** Não há nenhuma (CHECK, UNIQUE ou EXCLUDE). As 4 colunas de situação e de tipo são `text`, sem CHECK.
- **Como o esquema nasce.** São 0 arquivos de migração; o `CREATE TABLE` fica no Java.
- **O código.** Há pool de conexões (HikariCP), 2 transações explícitas e 2 `FOR UPDATE`.

**Nota:** com problemas

**O que está certo:**

- Os tipos são os certos: dinheiro em `numeric(12,2)` e datas em `date` (`persistence/Database.java:73-107`).
- A reserva está ligada ao quarto por chave estrangeira (`persistence/Database.java:83`), com o índice `(room_number, check_in, check_out)` (`persistence/Database.java:99`).
- Todo o SQL usa `?`. As concatenações que o script listou juntam só constantes (`COLUMNS` e o sufixo `" FOR UPDATE"`, em `persistence/ReservationRepository.java:18-19` e `:66-75`).
- As conexões saem de um pool e são devolvidas com try-with-resources (`persistence/Database.java:32`).
- A regra de "uma reserva por quarto por noite" é protegida pela trava da linha do quarto, `SELECT ... FOR UPDATE` (`persistence/RoomRepository.java:31`), seguida da checagem de sobreposição na mesma transação (`service/ReservationService.java:47-55`). Essa trava vale entre processos, não só dentro de um.
- A mudança de situação e as mensagens são gravadas na mesma transação (`service/ReservationService.java:63-69`).

**Os defeitos:**

- **O esquema não tem como evoluir.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`persistence/Database.java:73-107`), sem migrações versionadas. No próximo pedido que acrescentar uma coluna, esse código não altera uma tabela que já existe, e alguém vai ter de escrever o `ALTER` à mão.
- **O banco não protege nenhuma regra por si.** `status`, `rate_plan`, `type` e `channel` são `text` sem CHECK, e nada impede `guests < 1` nem `check_out <= check_in`. Também não há uma restrição de exclusão para a sobreposição de datas, que fica a cargo só do código.
- **Um detalhe menor.** O `migrate` tenta 30 vezes diante de qualquer `SQLException`, inclusive um erro de esquema, que não se resolve esperando (`persistence/Database.java:48-65`).

## Comportamento

**Números:** passaram 66 de 66 conferências, e 0 falharam.

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 03 | 15 de 15 |
| duas reservas ao mesmo tempo | 2 de 2 (10 rodadas) |

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:

- O preço, a alta temporada, o desconto e o arredondamento batem com o texto.
- As recusas devolvem os códigos combinados com o site.
- O comprovante e o e-mail do hóspede saem no formato pedido.
- Os relatórios em CSV estão certos, inclusive a reserva não reembolsável cancelada, que continua contando.
- Nada se perde ao reiniciar.
- Na confirmação, a governança e o financeiro recebem os e-mails com as datas, o número de hóspedes e o valor pago.
- No cancelamento de uma reserva confirmada, a governança é avisada, e o financeiro só quando há o que devolver. A reserva pendente cancelada não avisa ninguém.
- Duas pessoas tentando o mesmo quarto ao mesmo tempo nunca ficam as duas com a reserva.

## Dúvidas

Nenhuma sobre as notas. Há um ponto de leitura. O registro dos avisados é uma lista fixa num método estático (`notification/Notifiers.java:13-18`), e não algo que mude sem recompilar. Isso basta para o que o texto pede: o serviço recebe a lista de fora e não conhece os interessados. Mas, se "o restaurante e o transfer entram e saem conforme os contratos" fosse lido como uma configuração em tempo de execução, faltaria essa parte. Não tratei isso como defeito.
