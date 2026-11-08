# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (as rotas, `HttpApi`) chama `hotel.reservations.ReservationService` (as regras), que guarda em `ReservationStore` (JDBC sobre PostgreSQL, esquema por Flyway) e manda e-mails por `hotel.messages.EmailSender`, que só grava na tabela `messages`; a montagem fica em `hotel.Hotel`. A mudança do agente é pequena: tirou o e-mail do hóspede de `ReservationService` e criou `ReservationNotifier` (`src/main/java/hotel/reservations/ReservationNotifier.java`), uma classe só que conhece os três destinos (hóspede, governança, financeiro) e manda os e-mails um depois do outro; o serviço cria essa classe por dentro (`ReservationService.java:24`) e a chama na confirmação e no cancelamento de reserva confirmada (`ReservationService.java:64`, `:85-87`). O comportamento ficou certo: as 66 conferências passaram, e os 29 testes também. O ponto mais sério é o design: o texto avisa que restaurante e transfer vão entrar e sair, e o que o agente entregou é um "notificador" fechado. Para cada novo interessado alguém vai ter de editar essa classe. Pela referência do cenário, isso é omissão do Observer.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies-base.md`); `hotel.reservations` passou de 13 para 14 classes (508 → 583 linhas).
**Nota:** com problemas

As camadas da base continuam inteiras. `HttpApi` não foi tocado, as regras não usam Javalin, e o SQL continua só em `ReservationStore`/`Rooms`/`SentMessages`. A classe nova depende de `hotel.documents` e `hotel.messages` (`ReservationNotifier.java:3-6`), dependências que `hotel.reservations` já tinha. O ciclo `documents` ↔ `reservations` já existia na base (`ReservationReceipt` lê `Reservation`, e o serviço monta o comprovante), e o agente não o piorou. O defeito que o agente trouxe está na montagem. `ReservationService` continua recebendo um `EmailSender` (`ReservationService.java:19-20`) e cria o `ReservationNotifier` com `new` dentro do construtor (`ReservationService.java:24`). Por isso `Hotel` (`Hotel.java:24-25`), o lugar onde as peças se ligam, não sabe quem é avisado e não consegue trocar nem acrescentar ninguém. É uma mistura localizada, mas fica exatamente onde o próximo pedido (restaurante, transfer) vai bater.

## Design de baixo nível

**Números:** 5 avisos (hóspede, governança e financeiro na confirmação; governança e financeiro no cancelamento), todos escritos à mão em uma classe só; 0 ouvintes, 0 interfaces de ouvinte, 0 registros na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

Pela referência do cenário, a resposta certa é o serviço anunciar o evento sem saber quem o ouve, com um ouvinte por interessado registrado na montagem. Nada disso foi feito. `ReservationNotifier.notifyConfirmation` (`ReservationNotifier.java:18-35`) manda os três e-mails em sequência, com os endereços escritos na própria classe. `notifyCancellation` (`:37-49`) faz o mesmo com o `if` do reembolso dentro dela (`:44`). O serviço também continua decidindo quem é avisado: o `if (wasConfirmed)` (`ReservationService.java:78`, `:85`) é uma regra de notificação morando na regra de cancelamento. A referência diz com todas as letras que "um 'notificador' único que conhece todos os destinos é omissão, mesmo que esteja numa classe separada". Para pôr o restaurante, alguém vai ter de editar `ReservationNotifier`; para tirá-lo quando o contrato acabar, também. A favor do agente: a extração tirou do serviço o texto dos e-mails, e cada corpo ficou num método pequeno (`:51-82`), com `TextFormat` reaproveitado para datas e dinheiro.

Exagero não há: nenhuma interface sem uso, fábrica ou camada de repasse foi criada.

No resto do código há defeitos pequenos e localizados. Os endereços estão repetidos como literais (`governanca@...` em `ReservationNotifier.java:27` e `:39`; `financeiro@...` em `:32` e `:45`). O nome da pousada passou a aparecer escrito duas vezes (`:23`, `:53`). Enquanto isso, a constante `HOTEL_NAME` ficou em `ReservationService.java:12` sem nenhum uso, quando na base ela era usada (base `ReservationService.java:67`, `:127`). Os assuntos montam o número do quarto de dois jeitos diferentes, `room.number()` (`:40`) e `reservation.roomNumber()` (`:72`). O resto do código (tipos `BigDecimal`/`LocalDate`, `enum` para situação e plano, erros com `HotelException`) vem da base e não piorou.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26). Cobertura de linhas 94,2% (506/537; base 93,8%) e de ramos 87,7% (107/122; base 87,1%); `hotel.reservations` com 244/257 linhas e 55/58 ramos.
**Nota:** bom

O agente testou com valores tudo o que mudou e manteve os 26 testes da base passando. Os dois testes da base que ele alterou continuam firmes: só passaram a esperar 3 mensagens em vez de 1 (`ReservationServiceTest.java:121`, `HttpApiTest.java:81`), como o pedido manda. O teste da confirmação (`ReservationServiceTest.java:84-114`) confere o e-mail do hóspede como era, o assunto da governança com o quarto, as duas datas e "Hóspedes: 1" (`:106`), e o assunto e o valor do financeiro. Os três testes novos cobrem o cancelamento de pendente sem nenhum aviso (`:141-145`), o de confirmada STANDARD com a governança e as datas e o reembolso "R$ 499,80" (`:149-170`), e o de NON_REFUNDABLE sem o financeiro (`:173-186`). Fiz a pergunta da mutação: apagar o `if (wasConfirmed)` quebra `:145`; trocar `signum() > 0` por `>= 0` quebra `:180`; apagar a linha dos hóspedes quebra `:106`. Há uma ressalva pequena: os testes leem as mensagens pela posição (`sent.get(1)`, `get(3)`...), presos a uma ordem que o dono disse não importar, e vão quebrar à toa se a ordem mudar. Os testes rodam contra o banco de verdade e pela API, sem mocks.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro todo em `numeric(10,2)`; a chave estrangeira tem índice; nenhuma data em texto; `EXCLUDE` contra duas reservas no mesmo quarto e noite; a migração `V1__create_tables.sql` está igual à da base; o script acusou só as colunas `status`, `rate_plan`, `type` e `channel` sem CHECK.
**Nota:** bom

O agente não mexeu no banco, e nem precisava: os avisos são gravados na tabela `messages` que já existia, pelo `EmailSender` da base. As colunas sem CHECK vêm da base. Os quatro SQL montados com `+` que o script listou (`ReservationStore.java:33`, `:83`, `:98`, `:111`) só concatenam constantes, e os valores entram por `?`. Há um ponto que também vem da base e que a mudança só aumentou: a confirmação grava a reserva (`ReservationService.java:63`) e depois grava as mensagens em escritas separadas, sem transação. Se a terceira gravação falhar, a reserva fica confirmada com só parte dos avisos guardados. Na base era uma escrita a mais; agora são três.

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum 46/46, os dados depois de reiniciar 3/3, o cenário 03 15/15 e as reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona. O hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201" com entrada, saída e número de hóspedes. O financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80. Quando uma reserva confirmada é cancelada, chegam "Reserva cancelada - quarto 201" com as datas e "Reembolso - reserva 1" com o valor. O financeiro não é avisado quando não há o que devolver, e uma reserva pendente cancelada não avisa ninguém. O que já funcionava antes continua funcionando, inclusive depois de reiniciar e com duas reservas do mesmo quarto ao mesmo tempo. O problema de design não aparece hoje pela API; vai aparecer no dia de pôr o restaurante ou o transfer.

## Dúvidas

Na nota da arquitetura, o `new ReservationNotifier` dentro do serviço (`ReservationService.java:24`) é ao mesmo tempo um defeito de montagem e parte da omissão do pattern. Contei-o nas duas colunas porque a skill de arquitetura cita literalmente "um `new` dentro do serviço" como montagem fora do lugar. Quem achar que isso é contar o mesmo defeito duas vezes pode subir a arquitetura para `bom`.
