# Revisão: construção, cenário 03

## Visão geral

O agente montou o sistema num pacote só, `br.com.marealta`, com 24 classes de produção (cerca de 1.000 linhas) e 3 de teste. Uma requisição entra por `Routes`, que lê o corpo e o converte num `NewReservation` (`Routes.java:66-86`). Daí vai para `ReservationService`, que guarda as regras de situação no próprio record `Reservation` (`Reservation.java:34-53`) e o preço em `Pricing`/`RatePlan`. O SQL fica em `ReservationRepository` e `MessageRepository`, e cada operação roda numa transação aberta por `Database.inTransaction` (`Database.java:65-79`). Os dados ficam no PostgreSQL, em duas tabelas que o próprio código cria ao subir (`Database.java:33-58`). Os quartos são fixos no código (`Rooms.java:8-13`). As peças são criadas e ligadas num lugar só, `App.create` (`App.java:16-30`), e é ali também que se registram os interessados nos eventos da reserva. O pattern pedido (Observer) foi aplicado direito, e as 66 conferências do comportamento passaram. Não há defeito sério. Os pontos fracos são dois. Primeiro, as camadas existem como classes, mas não como pacotes. Segundo, o esquema é criado com `CREATE TABLE IF NOT EXISTS`, sem migrações e sem restrições no banco.

## Arquitetura

**Números:** 1 pacote (`br.com.marealta`, 27 classes compiladas, 1.017 linhas), 0 dependências entre pacotes, nenhum ciclo. O único pacote usa Javalin, JDBC, Jackson e o pool. A maior classe tem 136 linhas (`ReservationRepository`).
**Nota:** com problemas

Pelas classes, a separação é boa:
- **Borda HTTP fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta, em uma linha (`Routes.java:36-48`). Os erros viram JSON num tratador central (`Routes.java:50-55`).
- **Regras sem Javalin e sem SQL.** Ficam em `Reservation.java:34-53`, `Pricing.java:15-36` e `RatePlan.java:17-23`.
- **SQL isolado.** Mora só nos dois repositórios (`ReservationRepository.java:21-114`, `MessageRepository.java:12-37`).
- **Montagem num lugar só.** `App.java:16-30`, sem `new` de acesso a dados dentro dos serviços e sem `static` mutável.

Mesmo assim, há três ressalvas:
- **Um pacote plano.** As 27 classes não deixam a estrutura visível: HTTP, regras, dados e montagem moram juntos, e nada impede uma rota de chamar um repositório. O jdeps não tem o que medir, porque não há fronteiras.
- **O serviço conhece a transação JDBC.** `ReservationService` e `ReportService` recebem `Database` e passam a `Connection` adiante (`ReservationService.java:32-41` e `88-95`). O serviço ainda importa `java.sql.Connection` e `java.sql.SQLException` sem usar (`ReservationService.java:3-4`).
- **Status HTTP na exceção de domínio.** A exceção de domínio guarda o código HTTP (`DomainException.java:18-41`).

São defeitos leves num sistema deste tamanho, mas a falta de pacotes é exatamente o que a skill pede para apontar.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação; cancelamento de reserva confirmada). Os 3 interessados estão na estrutura e nenhum fica fora: o serviço não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `ReservationListener` é a interface do ouvinte, com um método `default` vazio por evento (`ReservationListener.java:10-16`). Há uma implementação por interessado:
- `GuestNotifier` (`GuestNotifier.java:13-22`), ou seja, o e-mail do hóspede também é um ouvinte;
- `HousekeepingNotifier` (`HousekeepingNotifier.java:13-27`);
- `FinanceNotifier` (`FinanceNotifier.java:13-26`).

O serviço recebe a lista de fora (`ReservationService.java:17-23`), percorre os ouvintes sem saber quem são (`ReservationService.java:98-102`) e só decide qual evento anunciar:
- confirmação em `ReservationService.java:50`;
- cancelamento de reserva que estava confirmada em `ReservationService.java:54-56`.

A lista é montada em `App.java:19-22`. Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha ali.

A regra "sem reembolso, não avisa o financeiro" está dentro do próprio ouvinte (`FinanceNotifier.java:22`). Os ouvintes devolvem as mensagens em vez de mandá-las, e o serviço as grava na mesma transação da mudança de situação (`ReservationService.java:91-93`). É uma variação sensata: o aviso não sai se a mudança falhar.

Nada afasta o projeto do pattern.

**Exagero.** Não há. Nenhuma interface tem uma implementação só, sem uso: `Database.Work` é a interface funcional do `inTransaction`. Não há fábrica, builder nem camada que só repassa.

**O resto do código.**
- **Tipos certos.** Dinheiro em `BigDecimal`, arredondado com `HALF_EVEN` a cada passo (`Money.java:12-14`, `Pricing.java:28-35`). Datas em `LocalDate` e situação em `enum` (`ReservationStatus.java`, `RatePlan.java`).
- **Estado sob controle.** `Reservation` é um record imutável, e as transições devolvem uma nova instância.
- **Erros tratados num lugar só,** sem nenhum engolido.

Detalhes que não mudam a nota:
- dois imports sem uso (`ReservationService.java:3-4`);
- o construtor de 16 argumentos posicionais, repetido em `Reservation.java:25-26`, `Reservation.java:30-31` e `ReservationService.java:37-40`, que é frágil para trocar campos de lugar;
- o `catch (Throwable)` com `rollback` em `Database.java:72-74`: se o próprio rollback falhar, ele esconde a exceção original.

## Testes

**Números:** 13 testes rodaram, 0 falhas, 0 erros, 0 pulados; 13 métodos escritos em 3 classes (`ApiTest` 4, `NotificationsTest` 5, `PricingTest` 4). A cobertura é de 95,3% das linhas (384/403) e 80,2% dos ramos (77/96). Há testes de unidade e testes pela API contra o PostgreSQL de `DB_URL`, num schema próprio; nenhum usa mocks.
**Nota:** bom

As regras pedidas são conferidas com valores:
- **Preço.** A alta temporada (599,76) em `PricingTest.java:12-17`. As fronteiras da temporada, inclusive 14/12, 15/12, 29/02 e 01/08, em `PricingTest.java:20-28`. O desconto não reembolsável (74,97 e 424,83) em `PricingTest.java:31-36`. O "meio para o par" com 0,045 → 0,04 em `PricingTest.java:39-43`.
- **Recusas e códigos de erro.** Todos em `ApiTest.java:96-110`.
- **Ciclo de vida e relatórios.** O ciclo, a ocupação com a regra de quem sai não contar e a segunda via em `ApiTest.java:113-159`. Os cancelamentos e o financeiro, com o CSV exato, em `ApiTest.java:162-195`.

A parte nova também é conferida:
- **E-mail do hóspede.** O texto inteiro, igual ao do enunciado (`NotificationsTest.java:17-35`).
- **Avisos da confirmação.** Os assuntos da governança e do financeiro, e o valor pago (`NotificationsTest.java:48-56`).
- **Reembolso.** O financeiro só é avisado quando há o que devolver (`NotificationsTest.java:59-71`).
- **Ligação dos ouvintes.** A contagem de mensagens pela API: 3 na confirmação, 2 no cancelamento normal, 1 no não reembolsável e 0 no cancelamento de pendente (`ApiTest.java:136`, `179`, `189`, `169`).

A pergunta da mutação:
- **Trocar `signum() <= 0` por `< 0`** no `FinanceNotifier`: `NotificationsTest.java:68` falha.
- **Avisar no cancelamento de pendente:** `ApiTest.java:169` falha.

Ficam sem rede:
- **Hóspedes no e-mail da governança.** Esse e-mail pode perder o número de hóspedes sem nenhum teste falhar, porque `NotificationsTest.java:52` só procura `"2"`, e o 2 já aparece nas datas 2027.
- **Datas do aviso de cancelamento.** Ninguém confere as datas que ficaram livres; `NotificationsTest.java:69-70` só olha o assunto.
- **Reservas simultâneas.** Não há teste delas.

São lacunas pequenas diante do resto.

## Banco

**Números:** 2 tabelas (`reservations`, com 16 colunas, e `messages`, com 5), ambas com chave primária. Dinheiro em `numeric(12,2)` nas 5 colunas de valor e datas em `date`. O script acusou o seguinte:
- 0 arquivos de migração, com o esquema criado por `CREATE TABLE` no Java;
- 0 chaves estrangeiras;
- 0 CHECK/UNIQUE/EXCLUDE;
- `status`, `rate_plan` e `channel` em `text` sem CHECK.

Os SQL montados com `+` só concatenam constantes.
**Nota:** com problemas

**O que está certo.** Os tipos estão certos e o `NOT NULL` está em tudo o que é obrigatório (`refund` aceita nulo de propósito, `Database.java:49`). Todo SQL usa parâmetros: as concatenações de `ReservationRepository.java:32`, `83`, `87`, `92` e `100-101` juntam só a constante `COLUMNS` e trechos literais. As conexões vêm de um pool Hikari, com try-with-resources (`Database.java:19-25`, `66`). Cada mudança de situação e as mensagens que ela gera são gravadas na mesma transação (`ReservationService.java:88-95`), com `SELECT ... FOR UPDATE` na reserva (`ReservationRepository.java:83`). A dupla reserva é barrada por um `pg_advisory_xact_lock` por quarto antes de conferir a sobreposição (`ReservationRepository.java:21-26`, `ReservationService.java:33-36`). Essa trava vale entre processos, e o roteiro confirmou que funciona.

**Os defeitos:**
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` dentro de `Database.java:33-58`, sem migrações versionadas, e o próximo pedido que acrescentar uma coluna não terá como alterar uma tabela que já existe.
- **O banco não protege nenhuma regra sozinho.** Não há tabela de quartos nem chave estrangeira para ela (`room_number` é um `INT` solto, `Database.java:35`). Não há `EXCLUDE` contra noites sobrepostas: a regra depende de todo código passar pela trava. Não há CHECK em `status`, `rate_plan`, `guests > 0` ou `check_out > check_in`.
- **Faltam índices.** Não há índice em `room_number`/`check_in`, que a conferência de disponibilidade e os relatórios usam (`ReservationRepository.java:31-32`, `92-93`, `100-101`). Hoje isso não pesa, com poucas linhas.

## Comportamento

**Números:** 66 de 66 conferências passaram, nenhuma falhou:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 03 (avisos da confirmação e do cancelamento): 15/15;
- duas reservas ao mesmo tempo: 2/2, ou seja, em 10 rodadas, sempre uma criada e a outra recusada com 409.
**Nota:** bom

Tudo o que o texto descreve funciona pela API:
- **Preço.** O preço da alta temporada (599,76) e o desconto não reembolsável com "meio para o par" (37,48 e 212,42).
- **Recusas.** Todas, com os códigos certos.
- **Ciclo de vida.** O ciclo completo e as devoluções por plano.
- **Relatórios.** Os dois, em `text/csv`.

As reservas e as mensagens continuam no banco depois de reiniciar o serviço. Na parte nova:
- **Na confirmação,** a governança e o financeiro recebem os e-mails com os assuntos, as datas, o número de hóspedes e o valor pedidos.
- **No cancelamento de uma reserva confirmada,** a governança é avisada, e o financeiro também, só quando há algo a devolver.
- **Uma reserva pendente cancelada** não avisa ninguém.

Para o dono da pousada, o sistema faz o que ele pediu, inclusive com dois pedidos simultâneos para o mesmo quarto.

## Dúvidas

Na arquitetura, a nota ficou entre `bom` e `com problemas`. Pelas classes, a separação de responsabilidades atende a todos os critérios de `bom`. O que a rebaixa é o pacote único de 27 classes (e, de leve, o serviço que lida com a `Connection` da transação e a exceção de domínio com status HTTP). Um revisor menos rígido com pacotes num sistema deste tamanho poderia dar `bom`.
