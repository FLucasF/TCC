# Revisão: manutenção, cenário 08

## Visão geral

O sistema da base está organizado em pacotes por assunto: `hotel` (montagem e banco), `web` (Javalin), `reservations`
(regras e acesso a dados das reservas), `rooms`, `messages`, `documents` e `reports`. Uma requisição entra pelo
`HttpApi`, vai para o `ReservationService`, que guarda tudo no PostgreSQL pelo `ReservationStore`. Os e-mails passam
pelo `EmailSender` e são gravados pelo `SentMessages` na tabela `messages`. O pedido era pequeno: pôr a assinatura no fim
de toda mensagem. O agente mexeu em dois arquivos e em nada mais (`diff -rq base final`). Pôs a assinatura no
`EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:12-19`), que é o único caminho de saída das
mensagens, e escreveu um teste. Não criou nenhuma estrutura nova, que era a resposta certa para este controle. O
defeito mais sério está no formato. O corpo da confirmação já terminava com uma quebra de linha
(`src/main/java/hotel/reservations/ReservationService.java:130`, `"Até breve!\n"`), e o agente acrescentou
`"\n\n--\n"` a esse corpo. Resultado: entre o texto e o `--` aparecem duas linhas em branco, e o dono pediu uma. O
roteiro do comportamento acusou isso, e o teste novo é fraco demais para pegar.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são idênticos aos da base (`measures/dependencies-base.md`); a única diferença é `hotel.messages`, que passou de 75
para 80 linhas.
**Nota:** bom

A mudança fica toda dentro de `hotel.messages` (`src/main/java/hotel/messages/EmailSender.java:12-19`). Não criou
dependência nova nem pacote novo. A borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:61-91`). As regras
não conhecem o Javalin e o SQL fica no `ReservationStore`, no `Rooms` e no `SentMessages`. A montagem está num lugar só
(`src/main/java/hotel/Hotel.java:25`). O ciclo `documents` ↔ `reservations` vem da base:
`src/main/java/hotel/documents/ReservationReceipt.java:3` importa `Reservation`, e
`src/main/java/hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`. O agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, +6/−1 linhas. A assinatura é acrescentada num lugar só, por onde passam
todas as mensagens: hoje só existe uma chamada a `send`, em `src/main/java/hotel/reservations/ReservationService.java:65`.
Nenhuma classe, interface ou enum novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é um controle negativo. A frase "talvez a gente troque a assinatura no Natal" convidava a criar um embrulho
(Decorator) ou uma assinatura por época do ano. O agente resistiu: a assinatura é um método privado que devolve um
texto fixo (`src/main/java/hotel/messages/EmailSender.java:17-19`), concatenado ao corpo em
`src/main/java/hotel/messages/EmailSender.java:13`. Se a pousada trocar a assinatura no Natal, a mudança acontece numa
linha só. Isso é exatamente a contenção descrita em `references/controls.md`.

Há dois pontos pequenos:
- O nome da pousada fica repetido. Está no texto da assinatura (`EmailSender.java:18`) e na constante
  `ReservationService.HOTEL_NAME` (`src/main/java/hotel/reservations/ReservationService.java:12`). Mas usar a
  constante faria `messages` depender de `reservations`, então a repetição se defende.
- A junção `body + "\n\n--\n"` (`EmailSender.java:13`) não olha como o corpo termina, e por isso sai a linha em branco
  a mais. Esse defeito pesa no comportamento, não aqui.

O resto do código vem da base e está limpo: `BigDecimal` com HALF_EVEN para o dinheiro
(`src/main/java/hotel/reservations/PriceCalculator.java:50-52`), `LocalDate` para as datas, `enum`s para a situação e o
plano, o estado da `Reservation` só muda por métodos do pacote
(`src/main/java/hotel/reservations/Reservation.java:102-114`), e os métodos são curtos.

## Testes

**Números:** final: 27 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 27 métodos de teste. Cobertura de linhas
473/504 (93,8%), de ramos 101/116 (87,1%). Base: 26 testes, cobertura de linhas 471/502 (93,8%), de ramos 101/116
(87,1%). O agente acrescentou 1 teste e não apagou nem enfraqueceu nenhum dos da base.
**Nota:** com problemas

O teste novo, `confirmationEmailIncludesTheSignature`
(`src/test/java/hotel/reservations/ReservationServiceTest.java:100-110`), só confere com `contains` que o corpo tem
`"--"`, `"Pousada Maré Alta"` e a linha do telefone. Ele é fraco:
- A conferência da linha 108 passaria sem mudança nenhuma. O corpo da base já continha "Pousada Maré Alta"
  (`src/main/java/hotel/reservations/ReservationService.java:127`).
- O teste não confere que a assinatura fica **no fim**, nem que há **uma** linha em branco antes do `--`, nem a ordem
  das três linhas.

Pela pergunta da mutação: o teste continua passando se a assinatura for posta antes do texto, se o separador ganhar ou
perder quebras de linha, ou se as linhas trocarem de ordem. Foi por isso que o defeito real passou: o corpo sai com duas
linhas em branco, e o roteiro do comportamento o acusou. Um `assertTrue(body.endsWith("Até breve!\n\n--\nPousada Maré
Alta\n(79) 3200-0000 | @pousadamarealta"))` teria apontado o erro. Fora isso, o resto dos testes vem da base e confere
as regras com valores.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Todo o dinheiro está em
`numeric(10,2)`. A chave estrangeira tem índice. Nenhuma data está guardada como texto. Há um EXCLUDE que impede duas
reservas do mesmo quarto na mesma noite. A conferência só acusou as colunas de situação, plano, tipo e canal sem CHECK.
A migração `V1__create_tables.sql` está igual à da base, e não há migração nova.
**Nota:** bom

O pedido não mexia no banco, e o agente também não mexeu: a assinatura vai para a coluna `body` que já existia, pelo
mesmo `INSERT` com parâmetros (`src/main/java/hotel/messages/SentMessages.java:21-30`). Os pontos que a medida levanta
vêm todos da base, e o agente não os piorou:
- as colunas `status`, `rate_plan`, `type` e `channel` sem CHECK;
- o SQL montado com `+`, que só junta constantes (`src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`).

## Comportamento

**Números:** 53 de 54 conferências passaram:
- parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 08: 2 de 3;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** com problemas

Tudo o que já funcionava continua funcionando. A saudação e o comprovante continuam no e-mail, e a assinatura aparece
no fim. A conferência que falhou é a do formato que o dono pediu: depois do texto, **uma** linha em branco e as três
linhas da assinatura. O corpo da confirmação já terminava com `\n` (`src/main/java/hotel/reservations/ReservationService.java:130`),
e o `EmailSender` acrescenta `\n\n--\n` (`src/main/java/hotel/messages/EmailSender.java:13`). Por isso o e-mail sai como
`Até breve!\n\n\n--\n...`, com duas linhas em branco antes do `--`.

O texto não deixava esse ponto em aberto ("pula uma linha"). Para o dono da pousada, todo e-mail de confirmação sai com
um espaço a mais antes da assinatura. É um defeito pequeno e só visual, mas é diferente do que ele pediu.

## Dúvidas

- A nota do comportamento ficou entre `bom` e `com problemas`. O defeito é só uma linha em branco a mais, mas contraria
  uma instrução explícita do texto, e por isso ficou `com problemas`.
- "Toda mensagem" hoje quer dizer só o e-mail de confirmação, porque `send` é chamado num lugar só. Pôr a assinatura no
  `EmailSender` também cobre os e-mails que vierem depois. Não há outro canal de mensagem no código para conferir.
