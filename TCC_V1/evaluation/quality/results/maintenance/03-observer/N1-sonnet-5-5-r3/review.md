# Revisão: manutenção, cenário 03

## Visão geral

A base já era dividida em pacotes por assunto: `hotel.web` (as rotas Javalin e os JSON), `hotel.reservations` (o
serviço, as regras e o `ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents`
(comprovante e formatação) e `hotel.reports`. Tudo é montado em `hotel.Hotel`. Uma requisição entra em `HttpApi`,
chama `ReservationService`, que lê e grava pelo `ReservationStore` no PostgreSQL. O esquema vem de uma migração Flyway
(`V1__create_tables.sql`). Para atender o pedido, o agente criou um evento selado `ReservationEvent` (`Confirmed`,
`CancelledAfterPayment`) e uma interface `ReservationListener`, os dois em `hotel.reservations`. Criou também dois
ouvintes num pacote novo, `hotel.notifications` (`GovernanceNotifier`, `FinanceNotifier`). O serviço recebe a lista
de ouvintes de fora, pela montagem em `Hotel.java:30-33`, e a percorre em `ReservationService.java:133-135`. O
comportamento está certo: o roteiro passou em 66 de 66 conferências, e os testes passam (30 de 30). O mais sério é de
design: o e-mail do hóspede continua como uma chamada direta dentro do serviço (`ReservationService.java:72-75`), em
vez de virar um ouvinte como os outros. Por isso o Observer ficou pela metade.

## Arquitetura

**Números:** 8 pacotes (base: 7) e 22 dependências entre pacotes (base: 17). Há 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base e não foi piorado. O pacote novo
`hotel.notifications` depende de 4 pacotes (`documents`, `messages`, `reservations`, `rooms`) e só `hotel` depende
dele.
**Nota:** bom

As dependências da mudança apontam para o lado certo. A interface `ReservationListener` e o evento ficam em
`hotel.reservations` (`ReservationListener.java:4`, `ReservationEvent.java:7`), e as implementações ficam em
`hotel.notifications`. Assim, as regras não conhecem quem é avisado: o jdeps não mostra nenhuma aresta
`reservations -> notifications`. A ligação acontece num lugar só, a classe de montagem (`Hotel.java:28-33`), e o
`EmailSender` passou a ser uma instância única, compartilhada pelo serviço e pelos ouvintes (`Hotel.java:28`). Os
ouvintes não usam Javalin nem JDBC (a tabela de bibliotecas mostra `hotel.notifications` sem nenhuma). Eles
reaproveitam `TextFormat` (`GovernanceNotifier.java:28`, `FinanceNotifier.java:26`) em vez de copiar a formatação.
`ReservationService` subiu de 138 para 154 linhas e continua de tamanho razoável. O agente não tocou em `HttpApi`. O
ciclo `documents`/`reservations` veio da base.

## Design de baixo nível

**Números:** há 3 interessados na confirmação: o hóspede, a governança e o financeiro. 2 estão na estrutura de
ouvintes (governança e financeiro) e 1 está fora dela: o hóspede, com chamada direta em
`ReservationService.java:72-75`. No cancelamento, os 2 interessados estão na estrutura. São 2 implementações de
`ReservationListener`.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

O que aproxima do Observer: o serviço anuncia o que aconteceu (`publish`, `ReservationService.java:76` e `:97-99`) e
não sabe quem ouve. A lista de ouvintes chega pelo construtor e é copiada para não mudar depois
(`ReservationService.java:31`). Cada interessado é uma classe separada (`GovernanceNotifier.java:10`,
`FinanceNotifier.java:9`), registrada na montagem (`Hotel.java:30-31`). Os textos da governança e do financeiro não
moram mais no serviço. A regra "só avisa no cancelamento de uma reserva já confirmada" fica no serviço, que decide qual
evento emitir (`ReservationService.java:90`, `:97`). A regra "só avisa o financeiro se houver reembolso" fica no
ouvinte (`FinanceNotifier.java:28`). As duas estão no lugar certo.

O que afasta: o e-mail do hóspede não virou ouvinte. `confirm` ainda monta o comprovante e chama o `EmailSender`
direto (`ReservationService.java:71-75`, com o texto em `confirmationEmailBody`, `:141-147`). O serviço continua
recebendo o `EmailSender` (`:18`, `:25`). A referência diz com todas as letras que é malfeito quando "o hóspede tem
chamada direta e só a governança e o financeiro viraram ouvintes". É exatamente o caso aqui. Um pedido futuro (por
exemplo, tirar o e-mail do hóspede, ou mandá-lo também por SMS) ainda vai mexer no serviço.

Exagero: nenhum. A interface tem duas implementações e foi feita para receber mais (restaurante, transfer). O evento
selado com dois `record`s é o mínimo para levar os dados de cada evento. Não há fábrica, builder nem camada que só
repassa.

O resto do código que o agente escreveu está limpo, com tipos certos (`BigDecimal` no reembolso,
`ReservationEvent.java:18`) e nomes claros. Ficam detalhes pequenos:
- `confirm` busca o mesmo quarto duas vezes no banco (`ReservationService.java:71` e `:76`).
- O campo `refund` de `CancelledAfterPayment` (`ReservationEvent.java:18`) repete o que `Reservation.refund()` já
  guarda (`Reservation.java:86`).
- Cada ouvinte faz `switch` exaustivo sobre o evento selado (`GovernanceNotifier.java:24`,
  `FinanceNotifier.java:22`). Um tipo de evento novo obriga a mexer em todos os ouvintes, mesmo nos que não se
  interessam por ele. Isso é aceitável com dois eventos, mas vale lembrar com o crescimento que o pedido anuncia.

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 30 testes rodaram, 0 falharam (base: 26, 0 falhas). Há 30 métodos escritos, 4 deles novos em
`NotifiersTest`. Cobertura de linhas: 94,3% (509/540; base: 93,8%). Cobertura de ramos: 88,1% (111/126; base: 87,1%).
`hotel.notifications` tem 26/26 linhas e 6/6 ramos cobertos.
**Nota:** com problemas

O agente testou a parte nova contra o banco de verdade, pelo serviço:
- confirmação com assunto, datas e número de hóspedes da governança (`NotifiersTest.java:54-58`), assunto do
  financeiro (`:61`) e o hóspede ainda recebendo o e-mail dele (`:63`);
- cancelamento de reserva confirmada (`:74-78`);
- NON_REFUNDABLE sem aviso ao financeiro (`:89-90`), o que protege a condição `signum() > 0`;
- reserva pendente cancelada sem aviso nenhum (`:99`).

O teste pela API também ficou mais forte, não mais fraco: passou a exigir os três e-mails e os destinatários
(`HttpApiTest.java:81-84`), o que confere o registro feito em `Hotel`. Nenhum teste da base foi apagado ou
enfraquecido. Os testes da base só ganharam o argumento `List.of()` no construtor.

As asserções têm buracos na parte de dinheiro:
- O e-mail de reembolso só confere que o corpo contém `"R$ "` (`NotifiersTest.java:79`). Se o ouvinte mandasse o
  valor errado (o total no lugar do reembolso, ou zero), nenhum teste falharia.
- O valor pago é conferido com um esperado calculado a partir do próprio código, com
  `price().total().toString().replace('.', ',')` (`:62`), e não com um valor fixo. Esse esperado não tem o separador
  de milhar que `TextFormat.money` produz, então quebraria com um total acima de R$ 999,99.

São defeitos localizados, mas na regra nova que mexe com dinheiro.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`.
Nenhuma chave estrangeira sem índice e nenhuma data em texto. Há uma `EXCLUDE USING gist` contra duas reservas na
mesma noite. Há 1 migração Flyway, igual à da base. As conferências só acusaram as colunas de situação e de tipo em
`text` sem CHECK (`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`), que vêm da base.
**Nota:** bom

O pedido não exigia mudar o banco, e o agente não mexeu nele: a migração `V1__create_tables.sql` está igual à da base,
e os avisos à equipe ficam na mesma tabela `messages` pelo mesmo `EmailSender`. O SQL com `+` em
`ReservationStore.java:33,83,98,111` só concatena constantes (veio da base). Há uma observação de leve, sem defeito
novo de esquema: a confirmação grava a reserva (`ReservationService.java:70`) e depois grava três mensagens, cada uma
na sua própria conexão, sem transação (o script conta 0 `setAutoCommit(false)`). Se uma dessas gravações falhar, a
reserva fica confirmada com parte dos avisos. A base já fazia isso com o e-mail do hóspede, e o agente só estendeu o
padrão.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 03: 15/15;
- reservas simultâneas: 2/2.
**Nota:** bom

Na confirmação, o hóspede recebe o mesmo e-mail de antes. A governança recebe "Reserva confirmada - quarto 201", com
10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80. No
cancelamento da reserva confirmada, chegam "Reserva cancelada - quarto 201", com as datas, e "Reembolso - reserva 1",
com R$ 779,80. A NON_REFUNDABLE cancelada avisa só a governança, e a reserva pendente cancelada não manda nada. Para o
dono da pousada, o que ele pediu funciona como ele descreveu, e nada do que já funcionava quebrou.

## Dúvidas

Na classe do pattern, a estrutura está bem feita para a governança e o financeiro, e só o hóspede ficou fora dela.
Pela regra da referência, isso é malfeito, não acerto. Mas o pedido dizia que o e-mail do hóspede "continua igual", o
que pode ter levado o agente a não mexer nele. O e-mail continuaria igual se fosse um ouvinte, então a leitura
literal da referência prevalece.
