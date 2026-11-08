# Revisão: manutenção, cenário 03

## Visão geral

O projeto é o da base, com sete pacotes: `hotel.web` (as rotas Javalin em `HttpApi`), `hotel.reservations` (o serviço com as regras, o `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava as mensagens na tabela `messages`), `hotel.documents`, `hotel.reports` e `hotel`, que monta as peças. Uma requisição entra pelo `HttpApi`, chama o `ReservationService` e chega ao PostgreSQL pelos stores. As tabelas são criadas por uma migração Flyway. Para o pedido, o agente criou a interface `ReservationEventListener` (`src/main/java/hotel/reservations/ReservationEventListener.java:3`) e dois ouvintes, `GovernanceListener` e `FinanceListener`. Eles são registrados em `Hotel` (`src/main/java/hotel/Hotel.java:29-30`) e chamados pelo serviço na confirmação e no cancelamento de uma reserva que estava confirmada (`src/main/java/hotel/reservations/ReservationService.java:76,97-99`). O comportamento está todo certo (66 de 66). O problema mais sério está no design: o e-mail do hóspede continua sendo uma chamada direta ao `EmailSender` dentro do serviço (`ReservationService.java:71-75`, `137-143`). Ele não virou um ouvinte, e o pattern fica pela metade.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem o mesmo número de pacotes e de dependências e o mesmo ciclo. `hotel.reservations` passou de 13 para 16 classes (de 508 para 611 linhas). O `ReservationService` passou de 138 para 154 linhas.
**Nota:** bom

O agente não criou nenhuma dependência nem ciclo novo (comparando `measures/dependencies.md` com `measures/dependencies-base.md`). O ciclo `documents` ↔ `reservations` já existia na base. Os ouvintes usam `TextFormat` (`src/main/java/hotel/reservations/GovernanceListener.java:3`, `FinanceListener.java:3`), o que reforça esse ciclo mas não cria outro. A montagem continua num lugar só: os ouvintes são criados e registrados em `src/main/java/hotel/Hotel.java:26-30`, e o `EmailSender` agora é compartilhado. As regras não conhecem Javalin nem SQL. Um ponto discutível: os ouvintes, que sabem endereços de e-mail e o texto das mensagens de outros setores, ficaram dentro do pacote de domínio `hotel.reservations`. Um pacote próprio para os avisos deixaria o domínio mais limpo à medida que entrarem o restaurante e o transfer. Num sistema deste tamanho, isso não chega a ser um defeito.

## Design de baixo nível

**Números:** há 3 interessados na confirmação (hóspede, governança, financeiro): 2 estão na estrutura de ouvintes e 1 fica fora (o hóspede, chamado direto pelo serviço). Os 2 interessados no cancelamento (governança, financeiro) estão na estrutura. A interface tem 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Observer:**
- Existe uma interface de ouvinte com um método por evento (`ReservationEventListener.java:4,6`) e uma implementação por interessado (`GovernanceListener`, `FinanceListener`).
- O serviço percorre a lista sem saber quem está nela (`ReservationService.java:151-153`).
- O registro é feito de fora, na montagem (`Hotel.java:29-30`).
- A regra "só avisa no cancelamento se a reserva estava confirmada" fica no serviço, que é quem conhece o estado (`ReservationService.java:90,97`).
- A regra "sem nada a devolver, o financeiro não é avisado" fica no ouvinte do financeiro (`FinanceListener.java:26-29`).
- O restaurante e o transfer entram com uma classe nova e uma linha em `Hotel`.

**O que afasta:** o hóspede, que era o interessado que já existia, continua fora da estrutura. O serviço ainda recebe o `EmailSender` (`ReservationService.java:19,27`), monta o comprovante e o texto do e-mail do hóspede, e manda à mão antes de avisar os ouvintes (`ReservationService.java:71-75`, `137-143`). A referência do pattern classifica exatamente esse caso como malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".

**Exagero:** nenhum. A interface tem duas implementações e o helper `notifyListeners` é pequeno. Nada foi criado sem uso.

**O resto do código** (o que o agente escreveu):
- `GovernanceListener` recebe e guarda um `Rooms` que nunca usa (`GovernanceListener.java:10,14`), e `Hotel.java:29` passa esse `Rooms` sem motivo.
- Os ouvintes são chamados em sequência, sem isolamento de erro (`ReservationService.java:152`). Se a gravação do aviso da governança falhar, o financeiro não é avisado, e o cliente recebe um erro de uma confirmação que já foi gravada (`ReservationService.java:70`). Uma nova tentativa recusa a confirmação, e o aviso se perde. A base já gravava o e-mail do hóspede depois do `store.update`, mas a mudança aumentou o número de escritas soltas depois dele.
- Os endereços de destino aparecem repetidos como literais (`GovernanceListener.java:22,30`; `FinanceListener.java:20,33`).
- `java.util.function.Consumer` está escrito com o nome completo em vez de importado (`ReservationService.java:151`).
- No teste, `hotel.reservations.ReservationEventListener` aparece com o nome completo, embora esteja no mesmo pacote (`src/test/java/hotel/reservations/ReservationServiceTest.java:201`).

São defeitos localizados. O dinheiro continua em `BigDecimal` formatado por `TextFormat.money`, e as datas em `LocalDate` formatadas por `TextFormat.date`.

## Testes

**Números:**
- `final`: 35 testes rodaram, 0 falhas, 0 erros, 0 pulados; 35 métodos escritos. Cobertura de linhas 94,6% (521/551) e de ramos 87,1% (108/124).
- `base`: 26 testes, 93,8% de linhas e 87,1% de ramos.
- O agente acrescentou 9 testes: 3 em `ReservationServiceTest`, 6 em `HttpApiTest`.

**Nota:** com problemas

**O que está bem:**
- Os 26 testes da base continuam passando.
- O único teste da base que mudou (`src/test/java/hotel/web/HttpApiTest.java:82-85`) passou a esperar 3 mensagens em vez de 1. É a mudança pedida, não um enfraquecimento.
- Os testes de unidade usam um ouvinte falso para conferir que a confirmação avisa, que o cancelamento de uma reserva confirmada avisa e que o de uma pendente não avisa (`ReservationServiceTest.java:163-198`).
- Pela API, o aviso de confirmação da governança é conferido com o assunto exato, as datas e o número de hóspedes (`HttpApiTest.java:135-139`), e o do financeiro com o assunto exato e o valor (`HttpApiTest.java:152-153`).
- O não aviso ao financeiro numa NON_REFUNDABLE (`HttpApiTest.java:207-228`) e o silêncio no cancelamento de uma pendente (`HttpApiTest.java:231-238`) também são conferidos.

**O que fica sem rede no cancelamento:**
- O reembolso só confere que o corpo contém "R$" (`HttpApiTest.java:200`). Se o ouvinte mandasse o total em vez do `refund`, ou um valor errado, nenhum teste falharia.
- O assunto do cancelamento é conferido só por `contains("cancelada")` e `contains("Reembolso")` (`HttpApiTest.java:170,198`), sem o número do quarto ou da reserva. Trocar `roomNumber()` por `id()` no assunto passaria.
- O teste de cancelamento da NON_REFUNDABLE não confere que a governança é avisada.

Os testes de confirmação dependem da ordem das mensagens (`HttpApiTest.java:84-85,134,150`). O dono disse que a ordem tanto faz, e esses testes quebrariam se a ordem de registro mudasse. Há também um `get("/messages")` sem efeito (`HttpApiTest.java:162`).

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em `numeric(10,2)` e nenhuma chave estrangeira está sem índice. Há um `EXCLUDE` contra duas reservas sobrepostas. A única migração (`V1__create_tables.sql`) está igual à da base. As conferências só acusaram colunas de situação/tipo em `text` sem CHECK (`status`, `rate_plan`, `type`, `channel`), e isso vem da base.
**Nota:** bom

O pedido não precisava mudar o esquema, e o agente não mexeu nele: nenhuma migração foi editada ou acrescentada. Os avisos novos são gravados pelo mesmo `EmailSender` → `SentMessages` da base, com SQL parametrizado. Não há transação explícita (`setAutoCommit(false)`: 0). A confirmação grava a reserva e depois cada e-mail em escritas separadas, então uma falha no meio deixa a reserva confirmada com avisos faltando. Esse modelo vem da base, e o agente só acrescentou mais escritas nele (o efeito no design está na seção anterior).

## Comportamento

**Números:** 66 de 66 conferências passaram:
- parte comum: 46/46;
- reinício: 3/3;
- cenário 03: 15/15;
- reservas simultâneas: 2/2.

**Nota:** bom

Na confirmação, o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80. No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto 201" com as datas, e o financeiro recebe "Reembolso - reserva 1" com R$ 779,80. Numa NON_REFUNDABLE, só a governança é avisada. Uma reserva pendente cancelada não manda nada. Para o dono da pousada, tudo o que ele pediu funciona e nada do que existia quebrou.

## Dúvidas

A classe ficou entre `malfeito` e `acerto`. A referência cita literalmente o caso "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes" como malfeito, e é exatamente o que o código faz (`ReservationService.java:71-75`). Por isso fiquei com `malfeito`, embora os interessados novos estejam bem estruturados.
