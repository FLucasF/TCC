# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, finas), `hotel.reservations` (as regras e o
acesso ao banco das reservas), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava cada e-mail na tabela
`messages`), `hotel.documents` (o comprovante e a formatação), `hotel.reports` e `hotel` (a montagem em `Hotel`).
Uma confirmação faz este caminho: `POST /reservations/{id}/confirmation` (`HttpApi.java:70`) chama
`ReservationService.confirm`, que atualiza a reserva no PostgreSQL e manda o e-mail. A mudança do agente é pequena e
está bem localizada. Ele criou uma interface `ReservationListener` com dois ouvintes, `HousekeepingNotifier` e
`FinanceNotifier`. Os dois são registrados em `Hotel.java:29-30`, e o serviço percorre a lista na confirmação e no
cancelamento de uma reserva confirmada. O comportamento está todo certo (66 de 66 conferências) e os 28 testes
passam. O defeito mais sério é de design: o e-mail do hóspede continua chamado direto pelo serviço
(`ReservationService.java:71-75`, com o texto em `confirmationEmailBody`, `:141-147`). O Observer ficou pela metade,
porque só os novos interessados viraram ouvintes.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O
`hotel.reservations` passou de 13 para 16 classes e de 508 para 613 linhas. A maior classe continua sendo
`HttpApi` (170 linhas). `ReservationService` subiu de 138 para 154 linhas.
**Nota:** bom

- **A borda HTTP:** continua fina. As rotas só leem o id e chamam o serviço (`web/HttpApi.java:70-73`), e o
  agente não mexeu no pacote `web`.
- **A montagem:** os ouvintes são criados e ligados num lugar só, a classe de montagem (`Hotel.java:29-32`). O
  serviço recebe a lista pronta pelo construtor (`ReservationService.java:21-31`) e não cria nada por dentro.
- **As regras:** os ouvintes não conhecem nem Javalin nem JDBC. Só usam o `EmailSender` e o `TextFormat`.
- **O ciclo:** não há ciclo novo. O ciclo `documents` ↔ `reservations` vem da base (`ReservationService.java:3`
  importa `ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `Reservation`). O agente só
  acrescentou mais duas arestas no mesmo sentido (`FinanceNotifier.java:3` e `HousekeepingNotifier.java:3` importam
  `TextFormat`).
- **Um reparo pequeno:** os avisos aos setores (endereços e textos de e-mail) ficaram dentro do pacote de regras
  das reservas (`reservations/FinanceNotifier.java`, `reservations/HousekeepingNotifier.java`). Com o restaurante
  e o transfer chegando, um pacote próprio para os avisos deixaria `reservations` mais enxuto. Isso não mistura
  camadas.

## Design de baixo nível

**Números:** há três interessados nos eventos. Dois deles, governança e financeiro, estão na estrutura de ouvintes
(`ReservationListener` com 2 implementações). O hóspede, que é o caso que já existia na base, ficou fora dela e
continua com chamada direta.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

**O que aproxima do Observer.** A interface `ReservationListener` tem dois eventos (`ReservationListener.java:7-14`),
e há uma implementação por setor (`HousekeepingNotifier.java:7`, `FinanceNotifier.java:8`). O serviço recebe a lista
de fora (`ReservationService.java:26`), a percorre na confirmação (`:76-78`) e no cancelamento (`:99-103`), e não
conhece os setores. Os ouvintes são registrados na montagem (`Hotel.java:29-30`), então incluir o restaurante ou o
transfer é escrever uma classe e acrescentá-la nessa lista. Cada setor decide o próprio texto e as próprias
condições, como o financeiro, que só avisa quando há reembolso (`FinanceNotifier.java:29`).

**O que afasta do Observer.** O hóspede, que é o interessado que a base já tinha, não virou ouvinte. O serviço
continua montando e mandando o e-mail dele à mão (`ReservationService.java:71-75`, com o texto em `:141-147`). Por
isso o serviço depende ao mesmo tempo do `EmailSender` e da lista de ouvintes (`:18-19`). A referência do pattern
classifica exatamente esse caso como malfeito ("o hóspede tem chamada direta e só a governança e o financeiro
viraram ouvintes").

**O exagero.** Não há. Nenhuma fábrica, builder ou camada de repasse foi criada. A única interface nova é a do
pattern, e ela tem duas implementações.

**O resto do código.** Está limpo e no estilo da base: dinheiro em `BigDecimal` formatado por
`TextFormat.money` e datas em `LocalDate` com `TextFormat.date`. Há três pontos menores:

- **Uma falha num aviso derruba os seguintes.** Uma exceção num ouvinte se propaga depois que a reserva já foi
  gravada (`ReservationService.java:70` e `:76-78`). Os ouvintes seguintes não rodam e a API responde 500 para
  uma confirmação que aconteceu. A base já tinha isso com o e-mail do hóspede, mas agora a cadeia é mais longa.
- **Todo ouvinte implementa os dois eventos.** A interface obriga isso (`ReservationListener.java:10,13`), então um
  restaurante que só quer confirmações terá um método vazio. Métodos `default` evitariam.
- **Uma repetição pequena.** As linhas de entrada e saída aparecem duas vezes em `HousekeepingNotifier.java:22-23`
  e `:33-34`.

Nenhum desses pontos pede reescrita.

## Testes

**Números:** 28 testes rodaram, nenhum falhou e nenhum foi pulado; há 28 métodos escritos. A base tinha 26, e os 26
continuam lá. A cobertura é de 94,3% das linhas (517/548) e 87,5% dos ramos (112/128); na base era 93,8% e 87,1%. No
pacote `hotel.reservations`, 252 de 265 linhas e 60 de 64 ramos (`measures/tests.md`).
**Nota:** com problemas

**O que está testado, com valores:**

- **A confirmação:** destinatário, assunto, datas, número de hóspedes e o valor R$ 212,42 do financeiro
  (`ReservationServiceTest.java:88-112`).
- **O cancelamento:** os dois assuntos da governança, o reembolso de R$ 499,80 do plano STANDARD e a ausência de
  aviso ao financeiro no NON_REFUNDABLE (`:140-169`).
- **A reserva pendente cancelada:** não manda nada (`:172-179`).
- **A API:** os três destinatários (`HttpApiTest.java:81-84`).

Os asserts da base foram ajustados de 1 para 3 mensagens (`ReservationServiceTest.java:95`, `:120`), não apagados.

**A pergunta da mutação:**

- Trocar `> 0` por `>= 0` em `FinanceNotifier.java:29` faz o `noneMatch` de `:167-168` falhar.
- Tirar o `if (wasConfirmed)` (`ReservationService.java:99`) faz o teste da pendente falhar.
- Apagar as linhas de datas do aviso de cancelamento (`HousekeepingNotifier.java:33-34`) **não derruba nenhum
  teste**. O teste do cancelamento só confere o assunto da governança (`ReservationServiceTest.java:157-162`), e o
  pedido exige "as datas que ficaram livres".

**A ordem dos e-mails.** `ReservationServiceTest.java:103-111` e `HttpApiTest.java:83-84` dependem dela
(`sent.get(1)` é a governança). O dono disse que tanto faz a ordem, e trocar a ordem dos ouvintes em `Hotel` quebra
testes sem quebrar nenhuma regra.

**O nível.** Não há teste do cancelamento pela API. A mistura de níveis, com serviço contra banco real e API,
segue a da base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (6 colunas, todas ok). Nenhuma
chave estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE` contra reservas sobrepostas. A migração
`V1__create_tables.sql` está igual à da base, e o agente não acrescentou nenhuma (`measures/database.md`). Só uma
conferência acusou algo: `status`, `rate_plan`, `type` e `channel` são `text` sem `CHECK`.
**Nota:** bom

O agente não precisava mudar o esquema e não mudou: os avisos aos setores vão para a mesma tabela `messages`, pelo
`EmailSender` que já existia. O que as conferências acusaram vem da base: as colunas de situação sem `CHECK`
(`V1__create_tables.sql:28,33`). A regra de não ter duas reservas na mesma noite é protegida pelo banco
(`V1__create_tables.sql:39-40`). O SQL usa parâmetros. As concatenações listadas pelo script juntam só a constante
`COLUMNS` (`ReservationStore.java:98,111`).

Uma observação: a gravação da reserva (`ReservationStore.java:64-65`) e a dos e-mails (`SentMessages.java:150-151`)
usam conexões separadas e não ficam numa transação. Se a gravação de um aviso falhar, a reserva fica confirmada ou
cancelada com só parte das mensagens gravadas. A base já fazia assim com o e-mail do hóspede; o agente só aumentou
o número de gravações que ficam de fora: 3 na confirmação e até 2 no cancelamento.

## Comportamento

**Números:** 66 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| A parte comum | 46 de 46 |
| Os dados depois de reiniciar | 3 de 3 |
| O cenário 03 | 15 de 15 |
| Duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu acontece pela API:

- **Na confirmação:** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada -
  quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com
  R$ 779,80.
- **No cancelamento de uma reserva confirmada:** a governança recebe as datas liberadas, e o financeiro recebe
  "Reembolso - reserva 1" com R$ 779,80. Numa NON_REFUNDABLE, só a governança é avisada.
- **Na reserva pendente cancelada:** ninguém é avisado.

O que já funcionava continua funcionando: preços, recusas, relatórios, os dados depois de reiniciar e as reservas
simultâneas.

## Dúvidas

- **Na nota dos testes:** fiquei entre `bom` e `com problemas`. Quase toda a parte nova está conferida com valores.
  A falta é uma regra só, as datas no aviso de cancelamento, mais o acoplamento à ordem dos e-mails. Dei `com
  problemas` porque o critério pede as regras novas conferidas, e aquela ficou sem nenhum teste que a proteja.
