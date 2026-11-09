# Revisão: manutenção, cenário 04

## Visão geral

O sistema da base tem sete pacotes: `hotel` (montagem e conexão: `Hotel.java`, `Database.java`), `hotel.web` (as rotas
do Javalin), `hotel.reservations` (as regras e o acesso às reservas), `hotel.rooms`, `hotel.messages`,
`hotel.documents` (o comprovante) e `hotel.reports`. Uma requisição entra em `HttpApi` (por exemplo,
`src/main/java/hotel/web/HttpApi.java:70-71` para a confirmação), passa para `ReservationService.confirm`
(`src/main/java/hotel/reservations/ReservationService.java:57-70`), que grava pela `ReservationStore` (JDBC com pool
Hikari) e manda o e-mail pelo `EmailSender`, que o guarda na tabela `messages`. O esquema vem de uma migração do Flyway
(`src/main/resources/db/migration/V1__create_tables.sql`). O pedido era trocar o assunto e uma linha do e-mail de
confirmação, e terminava com uma frase sobre um WhatsApp no futuro. O agente mudou só duas linhas de código e acertou o
texto: o assunto em `ReservationService.java:67` e as três linhas novas em `ReservationService.java:129-131`. Também
atualizou a asserção do assunto no teste. Não criou estrutura nova para o "talvez um dia". O roteiro passou inteiro
(58 de 58) e os 26 testes passam. Nada sério: o único ponto fraco é que o teste não confere as três linhas novas do
corpo.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base, os
números são os mesmos: só `hotel.reservations` passou de 508 para 510 linhas, e `ReservationService` de 138 para 140.
**Nota:** bom

A mudança fica toda dentro de `ReservationService.confirmationEmailBody` e da chamada do envio
(`src/main/java/hotel/reservations/ReservationService.java:65-68` e `:125-133`). Não entrou nenhuma dependência nova
nem pacote novo. O ciclo `hotel.documents` ↔ `hotel.reservations` já existia na base:
`ReservationReceipt` lê `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`) e o serviço cria o
comprovante (`ReservationService.java:3`, `:64`). O agente não o piorou. O resto também veio da base e está em ordem:
- as rotas só leem, chamam e respondem (`HttpApi.java:61-91`);
- as regras não usam Javalin;
- o SQL fica em `ReservationStore`, `Rooms` e `SentMessages`;
- a montagem acontece num lugar só (`src/main/java/hotel/Hotel.java:20-29`).

## Design de baixo nível

**Números:** 2 linhas de produção alteradas, mais 2 acrescentadas, num único arquivo (`ReservationService.java`).
Nenhuma classe, interface ou método novo. Um teste ajustado.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Pelo controle negativo do cenário 04, a resposta certa é mudar o assunto e o texto onde eles já moram. Foi o que o
agente fez:
- o assunto virou `"Sua reserva na " + HOTEL_NAME + " está confirmada!"` (`ReservationService.java:67`), reaproveitando
  a constante que já existia (`:12`);
- a linha do horário deu lugar às três linhas pedidas, na ordem e com o texto exato (`:129-131`);
- a saudação, o comprovante e o "Até breve!" ficaram como estavam (`:126-128`, `:132`).

A frase "talvez um dia a gente mande essa confirmação por WhatsApp" não gerou estrutura nenhuma: nem canal abstrato,
nem modelo de mensagem, nem ouvinte de confirmação. O `EmailSender` está igual ao da base
(`src/main/java/hotel/messages/EmailSender.java`).

O resto do código veio da base, e o agente não o tocou. É um código limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`);
- datas em `LocalDate`;
- `enum`s para a situação e o plano;
- o estado da reserva só muda por métodos do pacote (`src/main/java/hotel/reservations/Reservation.java:102-114`).

Dois pontos menores, ambos da base:
- o plano de tarifa decide em dois `switch` separados (`PriceCalculator.java:26-29` e `ReservationService.java:82-85`);
- há um `catch (Exception)` na leitura do corpo, com comentário que o justifica (`src/main/java/hotel/web/HttpApi.java:113`).

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos de teste. Linhas cobertas: 471 de 502
(93,8%). Ramos: 101 de 116 (87,1%). É idêntico à base (`measures/tests-base.md`).
**Nota:** com problemas

O agente manteve todos os testes da base passando e não enfraqueceu nenhum. Ele ajustou a asserção do assunto para o
texto novo (`src/test/java/hotel/reservations/ReservationServiceTest.java:94`), de modo que um erro no assunto faria o
teste falhar.

Já o corpo do e-mail, que é a outra metade do pedido, ficou sem conferência: o teste só procura o desconto e o total
do comprovante (`ReservationServiceTest.java:95-96`). Nenhum teste procura "Café da manhã das 7h às 10h", o telefone
"(79) 3200-0000", a linha nova do horário, nem a ausência da linha antiga. Pela pergunta da mutação, os testes
continuariam verdes se alguém:
- apagasse a linha do café da manhã (`ReservationService.java:130`);
- trocasse o telefone (`:131`);
- voltasse a linha antiga do horário (`:129`).

Os 93,8% de cobertura executam essas linhas, mas não conferem o que elas dizem.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 1 migração do Flyway, mantida igual à da base. As
conferências não acusaram tabela sem chave primária, dinheiro fora de `numeric(10,2)`, chave estrangeira sem índice
nem data guardada como texto. As colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) estão sem
CHECK. Não há transação explícita (`setAutoCommit(false)`).
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: nenhuma migração nova nem editada, nenhuma consulta alterada. O que
existe veio da base e está bem feito:
- dinheiro em `numeric(10,2)` e datas em `date`;
- chave estrangeira do quarto (`V1__create_tables.sql:21`);
- a restrição `EXCLUDE` que impede duas reservas do mesmo quarto na mesma noite (`V1__create_tables.sql:38-40`);
- SQL com parâmetros: os `+` apontados pelo script só juntam constantes (`ReservationStore.java:18-19`, `:98`, `:111`);
- conexões em try-with-resources e um pool (`src/main/java/hotel/Database.java:23-28`).

Também da base, e sem piora: as colunas de situação e plano em `text`, sem CHECK (`V1__create_tables.sql:29`, `:33`).
Além disso, a confirmação grava a reserva e a mensagem em duas conexões, sem transação
(`ReservationService.java:63-68`), então uma falha no meio deixaria a reserva confirmada sem o e-mail guardado.

## Comportamento

**Números:** 58 de 58 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 04: 7 de 7;
- reservas simultâneas: 2 de 2.
**Nota:** bom

O hóspede recebe o e-mail com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O corpo traz a saudação,
o comprovante, as três linhas novas na ordem pedida e o "Até breve!" no fim, e a linha antiga do horário saiu. Todo o
resto continua funcionando como antes: preços, recusas, cancelamentos, relatórios, dados guardados depois de reiniciar
e a proteção contra reserva dupla. Para o dono da pousada, a mudança está pronta para uso.

## Dúvidas

nenhuma
