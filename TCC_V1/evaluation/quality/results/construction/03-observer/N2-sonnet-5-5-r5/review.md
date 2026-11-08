# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web` (Javalin: `Routes` e os records de JSON em `ApiDtos`), `service` (`ReservationService`, `ReportService`), `domain` (`Reservation` com as transições de situação, `Pricing`, `RatePlan` com desconto e devolução por plano, `Receipt`, `Money`), `persistence` (JDBC puro com `DriverManager`, um repositório por tabela e o `Database`, que cria o esquema), `notification` (a interface `ReservationListener` e um ouvinte por interessado) e a montagem em `App`. Uma requisição entra em `Routes` (`web/Routes.java:53-59`), que lê o corpo, chama o serviço e devolve o record de JSON. O serviço carrega a reserva do repositório, aplica a transição no domínio, grava com uma checagem otimista da situação anterior (`persistence/ReservationRepository.java:92-105`) e avisa os ouvintes (`service/ReservationService.java:48,57`). Os dados ficam em três tabelas do PostgreSQL, e as mensagens enviadas também. O roteiro de comportamento passou inteiro (66 de 66), e o Observer foi aplicado direito. O que há de mais sério é pouco: o esquema é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migração, e a confirmação e os avisos que ela dispara não estão na mesma transação.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo. O Javalin só aparece em `web` e na montagem, e o JDBC só em `persistence`. A maior classe é `ReservationRepository`, com 159 linhas.
**Nota:** bom

As camadas estão separadas, e as dependências apontam da borda para as regras e das regras para os dados. Os handlers são uma linha cada (`web/Routes.java:51-66`): leem a requisição, chamam o serviço e escrevem a resposta. A validação de formato fica na borda (`web/Routes.java:69-138`). As regras de preço, de situação e de devolução moram no domínio, sem Javalin e sem SQL (`domain/Reservation.java:46-104`, `domain/Pricing.java:14-31`, `domain/RatePlan.java`). O SQL está todo em `persistence`. A montagem é feita num lugar só (`App.java:30-50`): os repositórios, a lista de ouvintes e os serviços são criados ali e passados pelo construtor, sem `new` de dependência dentro das classes e sem `static` global mutável. Nenhuma classe destoa no tamanho.

Há pontos menores, e nenhum pede mudança antes de aceitar:
- `Routes` usa `RoomRepository` e `MessageRepository` direto para as duas listagens (`web/Routes.java:51,61`) e pula o serviço. Como são só leituras, isso não chega a ser regra na borda.
- O `ErrorCode` do domínio guarda o status HTTP (`domain/ErrorCode.java:4-9`), um detalhe da borda que vazou para as regras.
- Não há pool de conexões: cada operação abre uma conexão nova (`persistence/Database.java:28-30`).

## Design de baixo nível

**Números:** 3 interessados (o hóspede, a governança e o financeiro), os 3 como ouvintes da mesma interface. 2 eventos (confirmação e cancelamento), os 2 anunciados pela lista. Nenhum e-mail é montado fora dos ouvintes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: `ReservationListener` (`notification/ReservationListener.java:10-18`) tem `onConfirmed` e `onCancelled` com implementação vazia por padrão, então cada interessado implementa só o que lhe importa. O serviço recebe a lista de fora (`service/ReservationService.java:23-28`) e só a percorre (`service/ReservationService.java:48,57`), sem conhecer destinatário nem texto. Os três interessados são classes separadas:
- `GuestConfirmationEmail`, para o hóspede, que também virou ouvinte (`notification/GuestConfirmationEmail.java:8-23`);
- `HousekeepingNotifier` (`notification/HousekeepingNotifier.java:10-35`);
- `FinanceNotifier` (`notification/FinanceNotifier.java:10-32`).

O registro, com os endereços, é feito na montagem (`App.java:36-39`). Incluir o restaurante ou o transfer é escrever uma classe e acrescentá-la nessa lista, sem tocar no serviço. As regras do tipo "só avisa se estava confirmada" e "só avisa o financeiro se há algo a devolver" ficam dentro de cada ouvinte (`notification/FinanceNotifier.java:27`, `notification/HousekeepingNotifier.java:29`), e não no serviço. O que afasta um pouco: o evento é passado como `(Reservation, ReservationStatus previous)`, e não como um objeto de evento, então cada ouvinte refaz a mesma checagem da situação anterior. É pequeno.

O exagero: nenhum. A única interface fora do pattern é a `Binder` privada (`persistence/ReservationRepository.java:130-132`), que existe porque o `bind` lança `SQLException`. Não há fábrica, builder nem camada que só repassa.

O resto do código está bom:
- O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` em cada passo (`domain/Money.java:19-21`, `domain/Pricing.java:21`, `domain/RatePlan.java:20`, `domain/Reservation.java:61`).
- As datas são `LocalDate`, e situação, plano e tipo de quarto são `enum`.
- As transições são métodos do domínio, guardados por `requireStatus` (`domain/Reservation.java:65-95`).

Há pequenas coisas a apontar:
- A validação de hóspedes e de datas aparece em dois lugares (`service/ReservationService.java:32-34` e `domain/Reservation.java:48-53`).
- A formatação de datas mora numa classe chamada `Money` (`domain/Money.java:32-34`).
- O mapeamento do quarto se repete, com nome qualificado em vez de import (`persistence/ReservationRepository.java:150-151` e `persistence/RoomRepository.java:47-48`).
- O tratador genérico usa `printStackTrace` (`web/Routes.java:46-49`), mas não engole o erro.

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 10 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 10 métodos de teste em 2 classes: 8 de domínio e 2 ponta a ponta pela API contra o PostgreSQL. A cobertura é de 91,3% das linhas (442 de 484) e 77,1% dos ramos (74 de 96). No pacote `notification`, são 35 de 36 linhas e 6 de 6 ramos.
**Nota:** com problemas

As regras comuns estão conferidas com valores:
- a temporada, com as bordas e o ano bissexto (`src/test/.../DomainTest.java:21-36`);
- o meio para o par, com 37,485 virando 37,48 (`DomainTest.java:39-44`);
- o texto do comprovante, exato (`DomainTest.java:54-66`);
- as transições e as devoluções (`DomainTest.java:79-106`);
- os códigos de erro, os relatórios em CSV exatos e o content-type (`src/test/.../EndToEndTest.java:86-142`).

A parte nova também tem testes:
- os 3 destinatários e os assuntos da confirmação (`EndToEndTest.java:108-131`);
- nenhum aviso ao cancelar uma reserva pendente;
- 2 avisos ao cancelar uma STANDARD confirmada;
- só a governança ao cancelar uma NON_REFUNDABLE (`EndToEndTest.java:155-189`).

Mas parte das regras novas ficou sem conferir. Fazendo a pergunta da mutação:
- se alguém apagar a linha `"Hóspedes: " + r.guests()` em `notification/HousekeepingNotifier.java:24`, nenhum teste falha, porque o teste só procura as duas datas (`EndToEndTest.java:120-121`);
- se trocar `r.refund()` por `r.amountPaid()` em `notification/FinanceNotifier.java:31`, ou apagar o valor do texto, nenhum teste falha, porque no cancelamento só se conferem os assuntos;
- se apagar as datas do aviso de cancelamento da governança (`notification/HousekeepingNotifier.java:34`), nenhum teste falha.

Além disso, os ouvintes não têm teste de unidade isolado: tudo passa pela API. A trava de reservas simultâneas (`persistence/ReservationRepository.java:39-42`) não é testada. E `fullFlow` é um método único que confere dezenas de coisas, o que torna difícil saber o que quebrou quando ele falha.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(12,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice: `reservations(room_number)`;
- 0 CHECK nas colunas de situação, plano, tipo e canal;
- 0 restrições UNIQUE ou EXCLUDE;
- 0 pool de conexões.
**Nota:** com problemas

O que está certo:
- os tipos (`persistence/Database.java:43-59`) e o `NOT NULL` em tudo, menos em `refund`, que é nulo de propósito;
- a chave estrangeira da reserva para o quarto (`persistence/Database.java:45`);
- o SQL com parâmetros. O único SQL montado com `+` que recebe valores (`persistence/Database.java:70`) insere constantes do mapa `ROOMS`, e não dado de fora;
- as conexões fechadas com try-with-resources.

A regra de um quarto por noite é protegida no banco, e não só no processo: a transação trava a linha do quarto com `SELECT ... FOR UPDATE`, confere a sobreposição e só então insere (`persistence/ReservationRepository.java:34-89`). As transições usam uma checagem otimista da situação (`persistence/ReservationRepository.java:95`).

Os defeitos:
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:37-67`), sem migração versionada. A próxima coluna nova não chega a um banco que já existe.
- `reservations.room_number` não tem índice, e é por ela que se faz a busca de sobreposição.
- Não há CHECK nem restrição de exclusão que segure a regra de um quarto por noite sem depender do código.
- Não há pool: é `DriverManager` a cada chamada (`persistence/Database.java:28-30`).
- A confirmação grava a reserva numa conexão, e cada ouvinte grava a sua mensagem em outra (`service/ReservationService.java:46-48`, `persistence/MessageRepository.java:20-31`). Se a gravação de um aviso falhar, a reserva fica confirmada, os avisos seguintes não saem e o cliente recebe 500.

## Comportamento

**Números:** 66 de 66 conferências passaram, nenhuma falhou:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono da pousada descreveu funciona pela API: os preços com a temporada e os arredondamentos, as recusas com os códigos certos, o ciclo da reserva, as devoluções, os dois relatórios e os dados que continuam lá depois de reiniciar. Na parte nova, a confirmação avisa o hóspede, a governança (com as datas e o número de hóspedes) e o financeiro (com o valor pago). O cancelamento de uma reserva confirmada avisa a governança com as datas liberadas e, só quando há reembolso, o financeiro com o valor. O cancelamento de uma reserva pendente não avisa ninguém. Nas dez rodadas de reservas simultâneas, nunca foram criadas duas.

## Dúvidas

No banco, a falta de atomicidade entre a confirmação e os avisos (`service/ReservationService.java:46-48`) não apareceu no roteiro, porque nenhuma gravação de aviso falhou. Fica como risco lido no código, não observado.
