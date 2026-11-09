# Revisão: manutenção, cenário 08

## Visão geral

O sistema que veio da base tem sete pacotes: `hotel` (montagem e banco), `hotel.web` (as rotas do Javalin),
`hotel.reservations` (as regras e o acesso à tabela de reservas), `hotel.rooms`, `hotel.documents` (o comprovante),
`hotel.messages` (o envio e o registro das mensagens) e `hotel.reports`. Uma requisição entra pela rota em
`web/HttpApi.java:61-91` e chama o `ReservationService`. Ele aplica a regra e grava pelo `ReservationStore` (JDBC com
`?`) num PostgreSQL cujo esquema vem de uma migração do Flyway. O pedido era pôr a assinatura no fim de toda mensagem,
com uma frase vaga sobre trocá-la no Natal (é o controle negativo). O agente mudou um arquivo só: criou a constante
`SIGNATURE` (`src/main/java/hotel/reservations/ReservationService.java:13-16`) e acrescentou uma linha em branco e a
assinatura no fim do texto da confirmação (`ReservationService.java:134-135`), que hoje é a única mensagem que o
sistema manda (`EmailSender.send` só é chamado em `ReservationService.java:69`). Não criou estrutura para a época do
Natal. As 54 conferências do roteiro passaram e os 26 testes da base continuam passando. O defeito que mais pesa é que
nenhum teste confere a assinatura.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos números: `measures/dependencies.md` e `measures/dependencies-base.md` só diferem nas 5 linhas a mais de
`ReservationService.java` (138 → 143).
**Nota:** bom

A mudança não criou dependência nova nem pacote novo. A assinatura ficou no serviço, junto do texto que ela completa
(`ReservationService.java:129-136`), e não entrou na borda HTTP nem no acesso ao banco. O ciclo já existia na base:
`ReservationService.java:3` importa `hotel.documents.ReservationReceipt`, e `documents/ReservationReceipt.java:3`
importa `hotel.reservations.Reservation`. O agente não o piorou. O resto também vem da base e está em ordem. As rotas
só leem a requisição e chamam o serviço (`web/HttpApi.java:64-90`). As regras não usam Javalin. As peças são montadas
num lugar só (`Hotel.java:20-28`).

## Design de baixo nível

**Números:** 1 arquivo mudado, +5/−1 linhas. Uma constante nova, nenhuma classe, interface ou método novo. A
assinatura é acrescentada num lugar só, no texto da única mensagem que o sistema manda.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Para o cenário 08, `references/controls.md` chama de contenção acrescentar a assinatura "num lugar só (no
`EmailSender.send` [...] ou no texto da confirmação, que hoje é a única mensagem)". Foi a segunda opção: uma
constante em text block (`ReservationService.java:13-16`), concatenada depois de `"Até breve!\n\n"`
(`ReservationService.java:134-135`). O agente não criou Decorator do `EmailSender`, assinatura por época do ano nem
configuração. A frase sobre o Natal não gerou estrutura.

Há um detalhe pequeno: o nome da pousada aparece de novo, como literal, dentro de `SIGNATURE`
(`ReservationService.java:15`), quando já existe `HOTEL_NAME` (`ReservationService.java:12`). É uma duplicação leve,
que um revisor poderia apontar sem exigir mudança.

Pôr a assinatura no `EmailSender.send` (`messages/EmailSender.java:12-13`) atenderia ao pé da letra o "toda
mensagem" para uma mensagem que surja no futuro. Mas a referência aceita as duas escolhas, e hoje não há outra
mensagem.

O resto do código é o da base e está bom. O dinheiro usa `BigDecimal` com arredondamento num ponto só
(`reservations/PriceCalculator.java:50-52`), as datas usam `LocalDate`, a situação e o plano são `enum`, e o estado da
reserva só muda por métodos do próprio pacote (`reservations/Reservation.java:102-114`). Os erros têm código próprio
(`web/HttpApi.java:100-107`).

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos de teste escritos. Cobertura de linhas
471/502 (93,8%), de ramos 101/116 (87,1%). `measures/tests.md` e `measures/tests-base.md` são idênticos.
**Nota:** com problemas

O agente manteve os 26 testes da base passando e não apagou nem enfraqueceu nenhum. Também não escreveu nenhum teste
para o que mudou. O teste do e-mail de confirmação (`src/test/java/hotel/reservations/ReservationServiceTest.java:83-97`)
confere o assunto, o desconto e o total com `contains`, mas nada sobre o fim do texto (nenhuma ocorrência de `--` nem
de "Pousada Maré Alta" no corpo, em `src/test`). A pergunta da mutação mostra o efeito: apagar
`+ SIGNATURE` (`ReservationService.java:135`), tirar a linha em branco (`"\n\n"` → `"\n"`, linha 134) ou errar o
telefone na linha 16 não derrubaria nenhum teste. A cobertura não cai porque a constante e a concatenação já são
executadas pelo teste da confirmação, mas a regra nova não tem conferência. A parte comum segue bem testada com
valores pelos testes da base (por exemplo, o arredondamento em `ReservationServiceTest.java:95-96`).

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`,
nenhuma chave estrangeira sem índice, nenhuma data em texto. O que o script acusou: as colunas de situação e de tipo
(`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`) sem `CHECK`. A migração
`V1__create_tables.sql` está igual à da base.
**Nota:** bom

O agente não mexeu no banco, e o pedido também não pedia isso: a mensagem continua gravada como texto em `messages`
pelo mesmo `INSERT` com parâmetros (`messages/SentMessages.java:24-29`). O que o script acusou vem da base:

- as colunas de situação sem `CHECK` (`src/main/resources/db/migration/V1__create_tables.sql:28` e `:33`);
- os dois relatórios trazem a tabela de reservas inteira para filtrar em Java (`reports/OccupancyReport.java:25`,
  `reports/RevenueReport.java:25`, via `reservations/ReservationStore.java:108-111`).

A base também já tinha a proteção certa contra duas reservas da mesma noite: um `EXCLUDE USING gist` sobre o quarto e
o intervalo de datas (`V1__create_tables.sql:39-40`).

## Comportamento

**Números:** 54 de 54 conferências passaram, 0 falharam. A parte comum: 46/46. Depois de reiniciar: 3/3. O cenário
08: 3/3. As reservas simultâneas: 2/2 (10 rodadas).
**Nota:** bom

O e-mail de confirmação agora termina em "Até breve!", uma linha em branco e as três linhas da assinatura, como o dono
pediu, e o texto de antes continua igual: a saudação "Olá, Carla Dias!" e o comprovante com "Total: R$ 249,90". Tudo
o que já funcionava continua funcionando: os preços, as recusas, o check-in e o check-out, os cancelamentos, os
relatórios, os dados depois de reiniciar e o bloqueio de duas reservas do mesmo quarto ao mesmo tempo.

## Dúvidas

O pedido diz "toda mensagem", e hoje só existe uma, a confirmação. A assinatura ficou no texto dela
(`ReservationService.java:134-135`) e não no `EmailSender.send`, por onde passaria qualquer mensagem futura. A
referência do controle aceita as duas escolhas como contenção. Se aparecer uma mensagem nova, quem a fizer vai ter de
lembrar de acrescentar a assinatura, e nenhum teste vai lembrar por ele.
