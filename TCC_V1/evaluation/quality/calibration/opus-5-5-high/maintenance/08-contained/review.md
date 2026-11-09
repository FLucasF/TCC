# Revisão: manutenção, cenário 08

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o SQL em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o envio em `EmailSender` e o
SQL das mensagens em `SentMessages`), `hotel.documents` (o comprovante), `hotel.reports` e `hotel` (a montagem em
`Hotel` e o pool em `Database`). Uma requisição entra pela rota em `HttpApi`, chama o `ReservationService`, que guarda
os dados no PostgreSQL pelo `ReservationStore` e manda o e-mail pelo `EmailSender` → `SentMessages` (tabela
`messages`, criada pela migração Flyway `V1__create_tables.sql`). O pedido era pôr a assinatura no fim de toda mensagem.
O agente mudou um arquivo só, `src/main/java/hotel/messages/EmailSender.java`: uma constante `SIGNATURE` (linha 6) e
a concatenação dentro de `send` (linha 15), por onde passa toda mensagem. Foi a mudança pequena que o controle negativo
espera, e o roteiro passa inteiro (54 de 54). Há duas falhas pequenas. A primeira: nenhum teste confere a assinatura.
A segunda: o arquivo mudado foi gravado com fim de linha CRLF (`\r\n`), quando o resto do projeto usa LF, e isso faz o
`diff` mostrar o arquivo inteiro como alterado.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos números, e a única diferença é `hotel.messages`, que passou de 75 para 77 linhas (`measures/dependencies.md`
contra `measures/dependencies-base.md`).
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou dentro do pacote `hotel.messages`, sem nenhuma dependência nova. O
ciclo já vinha da base: `ReservationService` importa o comprovante (`src/main/java/hotel/reservations/ReservationService.java:3`),
e o comprovante importa a reserva (`src/main/java/hotel/documents/ReservationReceipt.java:3`). A borda HTTP é fina, e
as rotas só leem, chamam o serviço e respondem (`src/main/java/hotel/web/HttpApi.java:62-90`). As regras não usam o
Javalin nem o SQL (`ReservationService.java:27-131`). O SQL fica isolado em `ReservationStore`, `Rooms` e
`SentMessages` (`src/main/java/hotel/messages/SentMessages.java:20-55`). A montagem fica toda em
`src/main/java/hotel/Hotel.java:20-28`. O agente pôs a assinatura no lugar certo: no remetente, por onde passa todo
envio (`EmailSender.java:14-16`), e não na regra da reserva.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado (+3 −1 linhas), nenhuma classe, interface ou embrulho novo. A assinatura
mora num lugar só (`EmailSender.java:6`), e toda mensagem passa por ela: o único ponto de envio é `EmailSender.send`,
que a única mensagem de hoje, a confirmação, usa (`ReservationService.java:65-68`).
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

A frase sobre o futuro ("Talvez a gente troque a assinatura no Natal") não levou o agente a criar estrutura: não há
um Decorator em volta do `EmailSender`, nem uma assinatura por época do ano, nem configuração. Ficou só uma constante
(`EmailSender.java:6`) e uma linha em `send` (`EmailSender.java:15`). É exatamente a resposta de contenção que
`references/controls.md` descreve para este cenário. O `body.stripTrailing() + "\n\n" + SIGNATURE` garante uma linha
em branco entre o texto e a assinatura, sem depender de como o texto termina. Isso é sensato, porque a confirmação
termina com `\n` (`ReservationService.java:130`).

Há três detalhes, nenhum que impeça aceitar a mudança. Primeiro, o nome da pousada fica escrito duas vezes: na
assinatura (`EmailSender.java:6`) e em `ReservationService.HOTEL_NAME` (`ReservationService.java:12`). Num texto fixo,
isso é aceitável. Segundo, a `SIGNATURE` é visível no pacote, como se fosse para um teste, mas nenhum teste a usa.
Terceiro, o arquivo foi gravado com CRLF, quando o resto do projeto (por exemplo `SentMessages.java`) usa LF: é ruído
no `diff` e no histórico, e não muda o design. O resto do código vem da base e está bem feito: `BigDecimal` para o
dinheiro (`Reservation.java`), `LocalDate` para as datas, `enum` para as situações e os planos, campos privados com as
mudanças de estado em métodos visíveis só no pacote, e erros com código próprio (`HotelException`/`ErrorCode`,
`HttpApi.java:94-107`).

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos de teste no código, os mesmos da base.
Cobertura de linhas de 471 de 502 (93,8%) e de ramos de 101 de 116 (87,1%), idêntica à da base
(`measures/tests.md` contra `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base, e todos passam: ele não apagou nem enfraqueceu nenhum. Mas não escreveu nenhum
teste para a única regra que o pedido trouxe. O teste do e-mail de confirmação confere o conteúdo só com `contains`
(`src/test/java/hotel/reservations/ReservationServiceTest.java:95-96`), e o da API só confere o destinatário
(`src/test/java/hotel/web/HttpApiTest.java:80-82`). Por isso continuam passando com ou sem assinatura. A pergunta da
mutação: se alguém apagasse a assinatura de `EmailSender.java:15`, trocasse o `"\n\n"` por `"\n"` (sem a linha em
branco) ou errasse o telefone em `EmailSender.java:6`, nenhum teste falharia. A regra nova está toda sem rede. O resto
das regras da base está testado com valores (por exemplo, o desconto de 37,48 e o total de 212,42 em
`ReservationServiceTest.java:95-96`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto. A conferência só acusou as colunas de situação e de tipo
em `text` sem CHECK (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`). A migração
`V1__create_tables.sql` está igual à da base (`measures/database.md`).
**Nota:** bom

O pedido não pedia mudança no banco, e o agente não fez nenhuma: a migração da base está intacta, e o serviço sobe
contra um banco vazio. O esquema da base é sólido. A dupla reserva é barrada pelo próprio banco com um `EXCLUDE USING
gist` (`src/main/resources/db/migration/V1__create_tables.sql:38-40`), e o SQL usa parâmetros
(`SentMessages.java:22-27`), com try-with-resources e pool. As colunas de situação sem CHECK, e a confirmação que grava
a reserva e a mensagem em duas conexões, sem uma transação que as una (`ReservationService.java:62-68`), já vinham
da base, e o agente não as piorou.

## Comportamento

**Números:** 54 de 54 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 08
3 de 3, reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

O e-mail de confirmação termina como o dono pediu: o texto ("Até breve!"), uma linha em branco e as três linhas
`--`, `Pousada Maré Alta` e `(79) 3200-0000 | @pousadamarealta`. A saudação e o comprovante continuam iguais. Todo o
resto que já funcionava continua funcionando: preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar
e a recusa da segunda de duas reservas simultâneas. Para o dono da pousada, a assinatura sai em toda mensagem, e nada
mudou no resto.

## Dúvidas

- O pedido fala em "toda mensagem", e hoje a única é o e-mail de confirmação. Como a assinatura está no
  `EmailSender.send`, as mensagens que vierem pelo e-mail também a terão, mas um canal novo (um WhatsApp, por exemplo)
  precisaria repeti-la. Isso é coerente com a contenção que o cenário pede e não pesou na nota.
- Não sei se o CRLF no `EmailSender.java` veio de uma escolha do agente ou da ferramenta de edição dele. Não pesei
  isso em nenhuma coluna.
