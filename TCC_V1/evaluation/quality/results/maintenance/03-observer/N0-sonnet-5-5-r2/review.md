# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin pequeno em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations`
(as regras em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`
(o `EmailSender`, que grava os e-mails na tabela `messages`), `hotel.documents` e `hotel.reports`. A montagem fica em
`hotel.Hotel`. Uma requisição entra pela `HttpApi`, chama o `ReservationService`, que usa o `ReservationStore`
(JDBC sobre PostgreSQL, com o esquema criado por migração do Flyway). Para o pedido, o agente criou a interface
`ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:9`), dois ouvintes num pacote
novo `hotel.notifications` (`Governance` e `Finance`), e fez o serviço percorrer a lista de ouvintes na confirmação e
no cancelamento de reserva confirmada. O comportamento está todo certo (66 de 66 conferências). O que há de mais sério
está no desenho: o e-mail do hóspede continua sendo uma chamada direta no serviço, fora dos ouvintes, e os ouvintes
da pousada (com os endereços fixos) são criados dentro do construtor do `ReservationService`, e não na montagem
(`Hotel`), o que cria um ciclo novo entre `reservations` e `notifications`.

## Arquitetura

**Números:** 8 pacotes (7 na base), 22 dependências entre eles (17 na base); 3 ciclos medidos pelo `tsort`, contra 1
na base: o `documents ↔ reservations` já existia na base; o agente acrescentou `reservations ↔ notifications` (e o
ciclo de três que junta os dois). `ReservationService` passou de 138 para 178 linhas e é a maior classe.
**Nota:** com problemas

As camadas continuam como na base: a borda HTTP não foi tocada, os ouvintes não conhecem Javalin nem JDBC
(`hotel.notifications` não usa biblioteca nenhuma, pela tabela de `measures/dependencies.md`). O defeito novo é a
montagem no lugar errado: o construtor de quatro argumentos do serviço cria `new Governance(emailSender,
"governanca@...")` e `new Finance(emailSender, "financeiro@...")`
(`final/src/main/java/hotel/reservations/ReservationService.java:29-34`), o que obriga `hotel.reservations` a
importar as classes concretas de `hotel.notifications` (`ReservationService.java:5-6`), enquanto `hotel.notifications`
depende de `hotel.reservations` pela interface e pela `Reservation` (`final/src/main/java/hotel/notifications/Finance.java:5-6`).
Daí o ciclo novo. A classe de montagem, `Hotel`, não foi mudada e continua usando o construtor antigo
(`final/src/main/java/hotel/Hotel.java:24-25`), então quem entra e sai da lista de avisados (o restaurante, o transfer)
continua sendo decidido dentro do serviço de regras. O ciclo `documents ↔ reservations` veio da base.

## Design de baixo nível

**Números:** 5 avisos no pedido (governança e financeiro na confirmação; governança e financeiro no cancelamento de
reserva confirmada; mais o hóspede, que já existia): 4 estão em ouvintes, 1 (o e-mail do hóspede) fica fora da
estrutura, como chamada direta no serviço. Interface de ouvinte com 2 implementações; nenhuma estrutura além disso.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Observer: há uma interface de ouvinte com um método por evento
(`ReservationListener.java:12` e `:16`), uma implementação por interessado (`Governance.java:10`, `Finance.java:10`),
e o serviço percorre a lista sem saber quem está nela (`ReservationService.java:151-159`), tanto na confirmação
(`:92`) quanto no cancelamento só de reserva confirmada (`:106` e `:113-116`). Os textos dos e-mails da equipe ficam
nos ouvintes, não no serviço, e há um construtor que recebe a lista de fora (`:36-47`). A regra "sem reembolso não
avisa o financeiro" ficou no próprio ouvinte (`Finance.java:31-33`), o que é o lugar certo.

O que afasta: o caso que já existia, o e-mail do hóspede, não virou ouvinte. O serviço continua montando o
comprovante e mandando o e-mail direto (`ReservationService.java:86-90`, com o texto em `:165-171`), e só depois avisa
os ouvintes. A referência do pattern diz com todas as letras que "o hóspede tem chamada direta e só a governança e o
financeiro viraram ouvintes" é malfeito. Além disso, a lista que vale em produção é escolhida dentro do serviço
(`:29-34`), não registrada na montagem: para pôr o restaurante na lista é preciso editar o `ReservationService`, o
oposto do que o pedido quer ("o restaurante e o transfer entram e saem conforme os contratos").

Exagero: nenhum. A interface tem duas implementações reais e os métodos `default` vazios
(`ReservationListener.java:12-17`) evitam que cada ouvinte implemente o evento que não lhe interessa.

O resto do código novo é limpo, mas com defeitos localizados: os endereços de e-mail fixos no serviço de regras
(`ReservationService.java:32-33`); o quarto buscado duas vezes no banco na mesma confirmação (`:86` e `:91`); o
`LOG` estático no meio dos campos de instância (`:23`); e o `null` checado em `Finance.java:31`, quando o cancelamento
sempre preenche o reembolso antes de avisar (`ReservationService.java:111`). O `catch (RuntimeException)` em
`ReservationService.java:155` engole a falha de um ouvinte, mas a registra no log com a causa e explica por quê (a
reserva já foi gravada); é uma escolha defensável, não um erro escondido. O restante (preço, `BigDecimal`, `enum` de
situação) vem da base, sem mudança.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos (26 na base, 2 novos). Cobertura de
linhas 93,7% (519 de 554; base 93,8%), de ramos 87,3% (110 de 126; base 87,1%); `hotel.notifications` com 33 de 33
linhas e 3 de 4 ramos.
**Nota:** com problemas

Os dois testes novos conferem bem os assuntos, os destinatários, as datas e o número de hóspedes da governança
(`final/src/test/java/hotel/reservations/ReservationServiceTest.java:100-114`), e que reserva pendente cancelada não
manda nada e que a NON_REFUNDABLE cancelada só avisa a governança (`:117-140`). Os testes da base que contavam um
e-mail passaram a contar três (`:91`, `:148`, `final/src/test/java/hotel/web/HttpApiTest.java:81`), o que é a mudança
pedida, não um enfraquecimento.

O ponto fraco é o dinheiro, que é o que o financeiro precisa: o valor pago e o valor do reembolso só são conferidos
com `contains("R$ ")` (`ReservationServiceTest.java:113` e `:133`). Pela pergunta da mutação: se o `Finance`
mandasse o total em vez do pago, ou o pago em vez do reembolso (`Finance.java:26` e `:38`), nenhum teste falharia. Também
ficam sem teste o isolamento de falha de um ouvinte (`ReservationService.java:153-157`) e o construtor que recebe a
lista de fora (`:36`), que é justamente o ponto de extensão para o restaurante e o transfer: todos os testes usam o
construtor com a lista fixa (`ReservationServiceTest.java:24`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; a única migração (`V1__create_tables.sql`) igual à da base; as colunas de situação e tipo
(`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`) sem CHECK.
**Nota:** bom

O agente não mexeu no banco, e não precisava: os e-mails novos vão pela mesma tabela `messages`, pelo `EmailSender`
que já existia. O esquema continua protegendo a regra de quarto duplicado com o `EXCLUDE USING gist` e as restrições
de datas e hóspedes (`measures/database.md`). As colunas de situação sem CHECK, a falta de transação explícita e o
`SELECT` sem filtro em `final/src/main/java/hotel/reservations/ReservationStore.java:111` vêm da base. Os SQL montados
com `+` (`ReservationStore.java:33`, `:83`, `:98`, `:111`) juntam só constantes, com os valores em parâmetros `?`.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46 de 46; dados depois de reiniciar 3 de 3; cenário 03 15 de
15; reservas simultâneas 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API: na confirmação, o hóspede continua recebendo o mesmo e-mail, a governança
recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e os 2 hóspedes, e o financeiro recebe "Pagamento
recebido - reserva 1" com R$ 779,80; no cancelamento da reserva confirmada, a governança recebe as datas liberadas e o
financeiro o reembolso de R$ 779,80; a NON_REFUNDABLE cancelada não avisa o financeiro, e a pendente cancelada não
avisa ninguém. O que já funcionava continua funcionando, e os e-mails continuam guardados depois de reiniciar.

## Dúvidas

Entre `malfeito` e `acerto`: a estrutura do Observer está certa para os avisos novos, e o defeito é só no caso que
já existia (o hóspede) e no lugar onde a lista é montada. A referência do cenário classifica explicitamente "o hóspede
com chamada direta e só a governança e o financeiro como ouvintes" como malfeito, e foi isso que pesou.
