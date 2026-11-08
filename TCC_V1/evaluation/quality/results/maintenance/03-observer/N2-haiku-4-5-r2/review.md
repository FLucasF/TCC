# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em sete pacotes. Uma requisição entra em `hotel.web.HttpApi`, chama
`hotel.reservations.ReservationService`, que guarda os dados no PostgreSQL por `ReservationStore`, `Rooms` e
`SentMessages`, com o esquema criado por uma migração do Flyway. Os e-mails são linhas na tabela `messages`. O agente
criou uma interface de ouvinte, `ReservationEventListener`, e dois records de evento. Criou também dois ouvintes,
`GovernanceListener` e `FinanceListener`. O serviço percorre a lista de ouvintes na confirmação e no cancelamento de
uma reserva confirmada. A lista é registrada na montagem (`Hotel.java:28-33`). O comportamento ficou certo: as 66
conferências do roteiro passaram. O problema mais sério é de design: o e-mail do hóspede não virou ouvinte. Ele
continua como chamada direta dentro de `ReservationService.confirm` (`ReservationService.java:71-75`), ao lado do laço
dos ouvintes. Por isso o pattern ficou pela metade. Os testes novos conferem só o destinatário e o assunto, nunca o
corpo dos e-mails.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), com os mesmos números da base (`measures/dependencies-base.md`). `hotel.reservations` passou de 13 para 19 classes (de 508 para 635 linhas).
**Nota:** bom

- **O ciclo veio da base:** a base já tinha `ReservationService.java:3` importando `hotel.documents.ReservationReceipt`.
  Os ouvintes novos acrescentaram mais dois usos na mesma direção (`FinanceListener.java:3` e
  `GovernanceListener.java:3` importam `TextFormat`). Isso não criou dependência nova entre pacotes.
- **A montagem ficou num lugar só:** `Hotel.java:27-33` cria um único `EmailSender` e registra os dois ouvintes.
- **As regras continuam sem Javalin e sem SQL:** a mudança não tocou na borda HTTP nem nas classes de acesso a dados.
- **Um ponto pequeno:** os ouvintes de departamento, com os endereços de e-mail fixos (`FinanceListener.java:10`,
  `GovernanceListener.java:9`), moram no pacote do domínio `hotel.reservations`. Num pacote próprio de avisos, o
  domínio não precisaria conhecer cada departamento. Isso não pesa na nota.

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro); 2 estão na estrutura de ouvintes e 1 (o hóspede) está fora dela, chamado direto pelo serviço. No cancelamento, os 2 interessados estão na estrutura.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Observer:**
- Há uma interface de ouvinte (`ReservationEventListener.java:4-8`), com uma implementação por departamento.
- O serviço recebe a lista de fora (`ReservationService.java:26,31`) e a percorre na confirmação
  (`ReservationService.java:77-79`) e no cancelamento (`ReservationService.java:100-105`).
- O serviço não monta os e-mails da governança nem do financeiro.
- A regra "sem devolução, não avisa o financeiro" ficou dentro do ouvinte (`FinanceListener.java:30`).
- A regra "pendente cancelada não avisa ninguém" ficou no serviço (`ReservationService.java:93,100`), o que é razoável:
  trata-se de qual evento acontece.

**O que afasta:** o caso que já existia, o hóspede, não foi para a estrutura. `ReservationService.confirm` ainda monta
o comprovante e chama `emailSender.send` direto (`ReservationService.java:71-75`, com o texto em
`confirmationEmailBody`, `ReservationService.java:143-149`). O serviço continua dependendo de `EmailSender`
(`ReservationService.java:18,25`). A referência descreve esse caso como malfeito: o hóspede tem chamada direta e só a
governança e o financeiro viraram ouvintes.

**Exagero:** contenção. A única estrutura sobrando está dentro da parte do pattern e é pequena:
- A interface selada `ReservationEvent` (`ReservationEvent.java:4`) não é usada como tipo em lugar nenhum.
- O componente `room` dos dois eventos (`ReservationConfirmedEvent.java:6`, `ReservationCancelledEvent.java:7`) nunca
  é lido: nenhum `.room()` no código.

**O resto do código, onde estão os defeitos da nota:**
- O componente `room` sem uso obriga o serviço a buscar o quarto de novo. `confirm` chama `roomOf(reservation)` duas
  vezes seguidas (`ReservationService.java:71` e `:76`), com duas consultas ao banco para o mesmo quarto.
- O evento de cancelamento carrega `refund` (`ReservationCancelledEvent.java:7`), um dado que a reserva já guarda em
  `Reservation.refund()` (`Reservation.java:86`).
- Os ouvintes rodam depois de `store.update` (`ReservationService.java:70,99`), sem nada que os isole. Se um deles
  lançar uma exceção, a API responde erro, mas a confirmação ou o cancelamento já estão gravados.
- Há uma duplicação pequena: `GovernanceListener.java:33-44` monta as linhas de entrada e saída duas vezes.

Nenhum desses defeitos é grave, mas um revisor exigente pediria a correção.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 pulados; 30 métodos escritos (a base tinha 26: 4 novos, nenhum apagado). Cobertura de linhas de 94,4% (521/552) e de ramos de 88,1% (111/126); na base, 93,8% e 87,1%. Em `hotel.reservations`: 257/270 linhas e 59/62 ramos.
**Nota:** com problemas

**O que o agente fez:**
- Escreveu quatro testes novos contra o banco, cobrindo os quatro caminhos do pedido: confirmação
  (`ReservationServiceTest.java:168-191`), cancelamento de reserva confirmada (`:194-216`), pendente cancelada sem aviso
  (`:219-236`) e NON_REFUNDABLE sem aviso ao financeiro (`:239-261`).
- Ajustou o teste da API para os três e-mails (`HttpApiTest.java:81-84`) sem enfraquecê-lo.
- Os 26 testes da base continuam passando.

**O que falta:** as asserções novas conferem só `to()` e `subject()`, nunca o corpo dos e-mails. Nada confere as datas
e o número de hóspedes da governança, nem o valor pago e o valor do reembolso do financeiro. Na pergunta da mutação,
nenhum teste falharia se alguém:
- trocasse `amountPaid()` por `total()` em `FinanceListener.java:22`;
- passasse para o financeiro o pago em vez de `refund` (`FinanceListener.java:33`);
- apagasse a linha dos hóspedes (`GovernanceListener.java:37`);
- trocasse a data de saída pela de entrada (`GovernanceListener.java:43`).

A regra `refund.signum() > 0` (`FinanceListener.java:30`) tem rede: o teste NON_REFUNDABLE conta 4 mensagens.

**Duas fragilidades menores:**
- Os testes dependem da ordem dos e-mails (índices `get(1)` e `get(2)`), que o dono disse não importar.
- O mesmo bloco de montagem do serviço com ouvintes está copiado nos quatro testes (`ReservationServiceTest.java:169-178`,
  `:195-204`, `:220-229`, `:240-249`).

## Banco

**Números:** 3 tabelas, todas com chave primária; o dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em texto. A conferência acusa `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK. A reserva dupla é protegida por `EXCLUDE USING gist`. A migração `V1__create_tables.sql` está igual à da base, e não há migração nova.
**Nota:** bom

- **O agente não mexeu no banco, e o pedido não exigia:** os avisos são linhas novas na tabela `messages` que já
  existia, gravadas por `SentMessages` com SQL parametrizado.
- **O que a conferência acusou veio da base:** as colunas de situação sem CHECK, e as escritas sem transação. Já na base,
  `confirm` gravava a reserva e depois o e-mail separadamente.
- **A mudança só aumentou o número de escritas soltas na confirmação:** uma atualização e três inserções
  (`ReservationService.java:70-79`), sem transação. É um risco pequeno de aviso faltando se o banco cair no meio, e não
  é um defeito novo de esquema.

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum 46/46, os dados depois de reiniciar 3/3, o cenário 03 15/15 e as reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **Na confirmação,** o hóspede recebe o mesmo e-mail de antes. A governança recebe "Reserva confirmada - quarto 201",
  com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- **No cancelamento de uma reserva confirmada,** a governança recebe as datas liberadas. O financeiro recebe o
  reembolso de R$ 779,80, e só quando há algo a devolver.
- **Uma reserva pendente cancelada** não manda nada.
- **O que já funcionava continua funcionando,** inclusive os dados depois de reiniciar e a recusa da reserva dupla.

## Dúvidas

- **A classe do pattern:** entre `malfeito` e `acerto` com ressalva, ficou `malfeito`. A referência descreve
  textualmente o caso do hóspede chamado direto como malfeito, e o código confirma (`ReservationService.java:71-75`).
- **A nota de design:** os defeitos do resto do código são pequenos e localizados (a consulta repetida, o componente sem
  uso, os ouvintes sem isolamento). Um revisor mais tolerante poderia dar `bom`.
