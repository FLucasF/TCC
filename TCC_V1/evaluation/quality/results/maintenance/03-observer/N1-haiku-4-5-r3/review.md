# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as
regras em `ReservationService` e o JDBC em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que
grava cada e-mail na tabela `messages`), `hotel.documents` (comprovante e formatação), `hotel.reports`, e a montagem em
`hotel.Hotel`. Uma requisição entra no `HttpApi`, chama o `ReservationService`, que usa o `ReservationStore` e grava no
PostgreSQL (o esquema vem de uma migração do Flyway). Para o pedido, o agente criou um pacote novo
`hotel.notifications` com uma classe só, `DepartmentNotifications`, que conhece a governança e o financeiro e manda os
quatro e-mails ela mesma. O `ReservationService` continua mandando o e-mail do hóspede direto e passou a chamar
também essa classe (`final/src/main/java/hotel/reservations/ReservationService.java:78-82` e `:103-106`). O
comportamento está todo certo (66 de 66 conferências) e os testes passam. O mais sério é o design: o texto avisa que
"vai ter cada vez mais gente querendo ser avisada" e que essa gente "entra e sai", e a solução é um notificador
fechado, sem ouvintes. Para avisar o restaurante ou o transfer, vai ser preciso mexer de novo nessa classe e no
serviço. A mudança também criou um ciclo novo entre pacotes, e a peça nova não é ligada na montagem.

## Arquitetura

**Números:** 8 pacotes (7 na base), 22 dependências entre eles (17 na base). A base já tinha um ciclo
(`hotel.documents` ↔ `hotel.reservations`). A mudança acrescentou o ciclo `hotel.reservations` ↔
`hotel.notifications` e outro de três pacotes (`documents`, `reservations`, `notifications`), segundo
`measures/dependencies.md` comparado com `measures/dependencies-base.md`.
**Nota:** com problemas

As camadas da base continuam como estavam: o Javalin só aparece em `hotel` e `hotel.web`, e o JDBC fica nas classes
de acesso a dados. O pacote novo também não usa nenhuma biblioteca. Os problemas são da mudança:

- **O ciclo novo.** O serviço depende do notificador (`ReservationService.java:5`, `:19`), e o notificador depende do
  modelo de reservas (`DepartmentNotifications.java:6`, `import hotel.reservations.Reservation`). Assim, o pacote de
  avisos e o de reservas dependem um do outro. Um evento publicado pelo serviço, ou uma interface de ouvinte no pacote
  de reservas, apontaria a dependência para um lado só.
- **A montagem fora do lugar.** `Hotel.java:24-25` não mudou e ainda chama o construtor de quatro argumentos. Quem cria
  o `DepartmentNotifications` é o próprio serviço, por dentro (`ReservationService.java:21-24`, com
  `new DepartmentNotifications(emailSender)`). Quem é avisado é decidido dentro do serviço, e não no lugar onde as
  peças são ligadas, justamente o que o texto diz que vai mudar com os contratos.

O ciclo `documents` ↔ `reservations` veio da base.

## Design de baixo nível

**Números:** quatro casos novos (governança e financeiro, na confirmação e no cancelamento) mais o caso que já
existia (o e-mail do hóspede). Nenhum dos cinco ficou numa estrutura de ouvintes: quatro estão escritos à mão numa
classe notificadora, e o do hóspede continua no serviço.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Observer).** A referência diz que "um 'notificador' único que conhece todos os destinos é omissão,
mesmo que esteja numa classe separada". É o caso aqui:

- `DepartmentNotifications` tem os dois destinos fixos no código (`DepartmentNotifications.java:31`, `:37`, `:43`,
  `:53`) e uma sequência de chamadas por evento (`:18-26`).
- O serviço ainda manda o e-mail do hóspede à mão (`ReservationService.java:78-81`).
- O serviço decide quando avisar (`if (wasConfirmed)`, `ReservationService.java:96`, `:103-106`).
- Não há interface de ouvinte, lista de ouvintes, nem registro na montagem.

Para pôr o restaurante ou o transfer, alguém tem de editar `DepartmentNotifications`. Para tirá-los quando o contrato
acaba, também. O único ponto a favor é que o serviço recebe o notificador por um construtor
(`ReservationService.java:26-31`), mas ele recebe um só, concreto.

**O exagero.** Não há estrutura sem necessidade. O segundo construtor existe para manter o antigo funcionando, não
para acrescentar camadas. Conta como defeito de montagem (ver Arquitetura), não como exagero.

**O resto do código** (o que o agente escreveu):

- `import hotel.documents.ReservationReceipt` não é usado (`DepartmentNotifications.java:3`).
- Os endereços aparecem como texto solto, cada um repetido duas vezes (`:31`/`:43` e `:37`/`:53`), em vez de
  constantes ou configuração.
- As linhas de entrada e saída estão copiadas entre os dois textos da governança (`:57-58` e `:67-68`).
- `notifyFinanceCancellation` relê o `refund()` e o confere contra `null` (`:47-48`), mas o `refund` já foi gravado
  por `cancel` logo antes (`ReservationService.java:101`, `Reservation.java:111-113`). A conferência não faz mal, mas
  é um ramo que nunca acontece (a cobertura mostra 3 de 4 ramos).
- O serviço cria a dependência por dentro (`ReservationService.java:23`).

São defeitos pequenos e localizados. Os tipos estão certos: `BigDecimal` com `TextFormat.money` e `LocalDate` com
`TextFormat.date`.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados, 31 métodos (26 na base). Cobertura de linhas 94,3%
(515/546; 93,8% na base) e de ramos 87,1% (108/124; igual à base). `hotel.notifications`: 35 de 35 linhas e 3 de 4
ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente acrescentou cinco testes em `ReservationServiceTest`:

- cancelar uma confirmada avisa os dois departamentos (`ReservationServiceTest.java:163`);
- cancelar uma NON_REFUNDABLE não avisa o financeiro (`:178`);
- cancelar uma pendente não avisa ninguém (`:191`);
- os textos da confirmação (`:200`);
- os textos do cancelamento (`:216`).

Também ajustou duas contagens antigas, de 1 para 3 e-mails (`:91`, `:109`), e o teste da API (`HttpApiTest.java:81-84`).
Nenhum teste da base foi apagado. Os ajustes de contagem acompanham a mudança pedida e não enfraquecem nada.

Destinatários, assuntos, datas e número de hóspedes são conferidos com valores. Os textos do financeiro não são:
os dois testes só olham `contains("Valor: R$ ")` (`ReservationServiceTest.java:212` e `:228`).

A pergunta da mutação:

- Se o aviso de pagamento mandasse o total, o subtotal ou zero em vez de `amountPaid()`
  (`DepartmentNotifications.java:62`), nenhum teste falharia.
- Se o reembolso mandasse outro valor (`:72`), também não.
- Já apagar o `if (wasConfirmed)` ou a conferência de "nada a devolver" seria pego (`:191`, `:178`).

Os testes também dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(2)`), e o texto diz que a ordem tanto faz.
Isso é frágil, mas não errado.

## Banco

**Números:** 3 tabelas e 1 migração do Flyway, igual à da base. Nenhuma tabela sem chave, o dinheiro em
`numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. O `EXCLUDE` impede duas reservas na
mesma noite. As colunas de situação, plano, tipo e canal são `text` sem `CHECK` (veio da base). Não há transação
explícita (0 `setAutoCommit(false)`), segundo `measures/database.md`.
**Nota:** bom

O pedido não precisava mexer no esquema, e o agente não mexeu: a migração `V1__create_tables.sql` está igual à da
base, e os e-mails novos vão para a tabela `messages` que já existia, pelo `SentMessages.add` com SQL parametrizado
(`SentMessages.java:20-35`). O SQL montado com `+` em `ReservationStore.java:33/83/98/111` só junta constantes, e veio
da base.

Há um ponto, que já existia na base e que o agente estendeu: a confirmação grava a reserva (`ReservationService.java:75`)
e depois cada e-mail numa conexão própria, sem transação. Agora são três inserções em vez de uma. Se uma delas falhar,
a reserva fica confirmada, a requisição responde erro e alguns avisos ficam sem ser gravados. O mesmo vale para o
cancelamento (`:102-106`). Não chega a mudar a nota, porque o padrão é da base.

## Comportamento

**Números:** 66 de 66 conferências passaram (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:

- **Na confirmação:** o hóspede continua recebendo o e-mail de antes. A governança recebe "Reserva confirmada -
  quarto 201" com entrada, saída e número de hóspedes. O financeiro recebe "Pagamento recebido - reserva 1" com
  R$ 779,80.
- **No cancelamento de uma reserva confirmada:** a governança recebe "Reserva cancelada - quarto 201" com as datas.
  O financeiro recebe "Reembolso - reserva 1" com o valor, e só quando há algo a devolver (a NON_REFUNDABLE não o
  avisa).
- **Uma reserva pendente cancelada** não manda nada.
- **O resto:** tudo o que já funcionava continua funcionando, e os dados resistem a um reinício.

Nada falhou. O problema desta mudança não aparece pela API hoje: ele vai aparecer quando entrar o restaurante ou o
transfer (ver Design).

## Dúvidas

Na nota do design de baixo nível, fiquei entre `bom` e `com problemas`. O código que o agente escreveu é pequeno e
legível, e os defeitos (import morto, endereços repetidos como texto solto, as linhas de datas copiadas, o serviço
criando a dependência por dentro) são menores. Escolhi `com problemas` porque um revisor exigente pediria para corrigi
-los antes de aceitar.
