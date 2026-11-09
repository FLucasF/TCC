# Revisão: manutenção, cenário 08

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (serviço,
cálculo de preço e `ReservationStore` com JDBC), `hotel.rooms`, `hotel.documents` (comprovante), `hotel.reports`,
`hotel.messages` (envio e guarda dos e-mails) e `hotel`, que monta tudo (`Hotel.java:25`). Uma confirmação entra em
`HttpApi`, passa para `ReservationService.confirm` (`ReservationService.java:57`), que grava a reserva e manda o
e-mail por `EmailSender.send` (`ReservationService.java:65`); o e-mail fica na tabela `messages` do PostgreSQL
(`SentMessages.java:41-56`), criada por migração Flyway. O pedido era pôr a assinatura no fim de toda mensagem, e o
agente fez a mudança mínima: uma constante e uma concatenação em `EmailSender` (`EmailSender.java:6` e `:15`), o
único lugar por onde passa toda mensagem, e um teste novo (`ReservationServiceTest.java:158-166`). Só esses dois
arquivos mudaram (`diff -rq base final`). Não há nada sério. O único ponto fraco é o teste novo: ele confere que a
assinatura aparece no e-mail, mas não que ela fica no fim nem que vem depois de uma linha em branco.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base
(`measures/dependencies-base.md`). A única diferença é `hotel.messages`, que passou de 75 para 77 linhas.
**Nota:** bom

O agente não mexeu na estrutura: a assinatura entrou no pacote que já cuidava das mensagens
(`hotel/messages/EmailSender.java:6,15`), sem dependência nova. O ciclo `documents` ↔ `reservations` já estava na
base (`ReservationService.java:64` cria o `ReservationReceipt`, que por sua vez conhece `Reservation`). A camada HTTP
continua fina, as regras ficam sem Javalin, o JDBC fica em `ReservationStore`/`SentMessages`/`Rooms`, e as peças são
montadas num lugar só (`Hotel.java:25`). O agente não piorou nada.

## Design de baixo nível

**Números:** 1 constante privada e 1 concatenação em `EmailSender` (2 linhas de produção). Nenhuma classe, interface
ou parâmetro novo. A assinatura está num lugar só, e todas as mensagens passam por ele (a confirmação, em
`ReservationService.java:65`, é hoje o único envio).
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é um controle negativo, e a frase que tenta o agente é "talvez a gente troque a assinatura no Natal". O agente
não caiu nela. Não fez Decorator em volta do `EmailSender`, não criou assinatura por época do ano e não deixou nada
configurável. Fez o que `references/controls.md` chama de contenção: uma constante `private static final`
(`EmailSender.java:6`) acrescentada em `send` (`EmailSender.java:15`). Por estar no `send`, a regra vale para "toda
mensagem", e não só para a confirmação, que é o certo. Trocar a assinatura no Natal será mudar uma linha.

Um detalhe sem peso: a linha em branco antes do `--` só aparece porque `confirmationEmailBody` termina em `\n`
(`ReservationService.java:130`). A constante começa com um `\n` só, e uma mensagem futura sem `\n` no fim ficaria sem
a linha em branco. Hoje o resultado está certo (o roteiro do comportamento confere), mas a regra depende de um
detalhe de outro arquivo.

O resto do código vem da base e está em bom estado: `BigDecimal` para dinheiro (`ReservationService.java:82-85`),
`enum` para situação e plano, erros com código próprio (`ReservationService.java:133-137`) e exceções de SQL
embrulhadas com a causa (`SentMessages.java:53-55`). Não há nada do agente para apontar.

## Testes

**Números:** final com 27 testes rodados, 0 falhas, 0 erros, 0 pulados, 27 métodos escritos; linhas 471/502 (93,8%),
ramos 101/116 (87,1%). Base com 26 testes e a mesma cobertura. O agente acrescentou 1 teste e não apagou nem
enfraqueceu nenhum.
**Nota:** com problemas

O `mvn verify` passa, e os 26 testes da base continuam passando sem mudança. O teste novo,
`emailsHaveTheHotelSignature` (`src/test/java/hotel/reservations/ReservationServiceTest.java:158-166`), confirma uma
reserva e confere a assinatura com o texto exato das três linhas. É um teste de valor, contra o banco de verdade,
mas a asserção é `body.contains("--\nPousada…")` (`ReservationServiceTest.java:165`). Pela pergunta da mutação:

- se a assinatura fosse para o começo ou para o meio do texto, o teste continuaria passando, embora o pedido seja
  "terminar com";
- se alguém tirasse o `\n` inicial da constante (`EmailSender.java:6`), a linha em branco sumiria
  (`...Até breve!\n--`) e o teste continuaria passando, porque a busca começa no `--`;
- se alguém apagasse a assinatura, o teste falharia.

Duas das três partes da regra (no fim e depois de uma linha em branco) ficam sem rede. Um `endsWith("\n\n--\n…")`
resolveria. O teste só passa pela confirmação, mas como a assinatura está no `EmailSender`, isso basta para o código
de hoje.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto. A migração `V1__create_tables.sql` está igual à da base, e
não há migração nova. As conferências acusaram só as colunas de situação e de tipo sem `CHECK`
(`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`).
**Nota:** bom

O pedido não mexe no esquema, e o agente não mexeu: a assinatura vai dentro de `body`, que já é `text`
(`SentMessages.java:44,48`), com parâmetro `?`. As colunas sem `CHECK` vêm da base. A reserva dupla é barrada por um
`EXCLUDE USING gist` com `daterange` (base), e os trechos de SQL montados com `+` em `ReservationStore.java:33,83,98,111`
só juntam constantes (base). O agente não introduziu nenhum defeito aqui.

## Comportamento

**Números:** 54 de 54 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 08 (a
assinatura) 3/3, reservas simultâneas 2/2 (`measures/behavior.md`).
**Nota:** bom

O roteiro confirmou pela API que o e-mail termina com uma linha em branco e as três linhas da assinatura, e que a
saudação e o comprovante continuam lá. Também passou todo o resto que já funcionava (preços, recusas, cancelamentos,
relatórios, persistência, concorrência). Para o dono da pousada: todo e-mail sai com a assinatura do jeito que ele
pediu, e nada do que funcionava parou de funcionar.

## Dúvidas

- Testes: fiquei entre `com problemas` e `bom`. O teste novo existe, passa e confere o texto exato da assinatura. Mas
  não confere as duas exigências que diferenciam o pedido (estar no fim e vir depois de uma linha em branco). Como um
  revisor exigente pediria essa troca de `contains` por `endsWith` antes de aceitar, escolhi `com problemas`.
