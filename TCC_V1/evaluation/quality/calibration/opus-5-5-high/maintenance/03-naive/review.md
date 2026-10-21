# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web.HttpApi` recebe a requisição e chama `hotel.reservations.ReservationService`, que guarda os dados no PostgreSQL por `ReservationStore`, com o esquema criado por uma migração do Flyway. Os e-mails passam por `hotel.messages.EmailSender` e ficam gravados na tabela `messages`. A montagem é feita em `hotel.Hotel`. O agente mudou um único arquivo de produção, `ReservationService.java` (+23 linhas), e trocou três números esperados nos testes (de 1 para 3 mensagens). Os quatro e-mails novos são enviados do jeito que o dono pediu: o roteiro passou 66 de 66 conferências. O problema mais sério está no design. O pedido avisa que "vai ter cada vez mais gente querendo ser avisada" e que os interessados "entram e saem". Mesmo assim, o agente escreveu os endereços, os assuntos e os textos da governança e do financeiro direto dentro de `confirm` e `cancel` (`src/main/java/hotel/reservations/ReservationService.java:73-80` e `:101-110`), em chamadas seguidas ao `EmailSender`. Não há nenhum ouvinte, e cada novo interessado (restaurante, transfer) vai exigir mudar o serviço de reservas. Além disso, os testes não conferem nada do conteúdo novo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, nos dois casos. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md` e `measures/dependencies.md` só diferem nas linhas de `hotel.reservations`, que foram de 508 para 531, e no tamanho de `ReservationService`, de 138 para 161 linhas).
**Nota:** bom

A mudança não alterou as camadas. As rotas continuam finas: `src/main/java/hotel/web/HttpApi.java:70-73` só chama `reservations.confirm` e `reservations.cancel`. As regras não usam Javalin. O SQL fica nas classes de dados (`ReservationStore`, `SentMessages`), e a montagem continua num lugar só (`src/main/java/hotel/Hotel.java:59-67`). O ciclo `documents` ↔ `reservations` vem da base: `ReservationService.java:3` importa `ReservationReceipt`, e `src/main/java/hotel/documents/ReservationReceipt.java:3` importa `Reservation`. O agente acrescentou mais um uso do mesmo sentido (`ReservationService.java:4`, `TextFormat`), sem criar dependência nova entre pacotes. O único ponto de atenção é o de responsabilidade. Agora o serviço de reservas guarda os endereços dos setores (`ReservationService.java:14-15`) e o texto de cada aviso, e por isso ficou 23 linhas maior. Isso pesa mais no design, logo abaixo, do que aqui.

## Design de baixo nível

**Números:** 5 interessados (o hóspede na confirmação, a governança e o financeiro na confirmação e no cancelamento). Nenhum está numa estrutura de ouvintes: os 5 são chamadas diretas a `emailSender.send` dentro do serviço (`ReservationService.java:68`, `:73`, `:78`, `:103`, `:107`). O agente não criou nenhuma interface nova, nenhuma lista de ouvintes e nada novo na montagem (`Hotel.java` não mudou).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve um Observer: vários interessados em dois acontecimentos (confirmação e cancelamento), a lista crescendo e mudando com os contratos. O agente não fez nada que se aproxime disso.
- Na confirmação, os e-mails saem um depois do outro, no próprio `confirm` (`ReservationService.java:68-80`).
- No cancelamento, o serviço decide quem avisar com `if (wasConfirmed)` e `if (refund.signum() > 0)`, e monta os textos ele mesmo (`ReservationService.java:94`, `:101-110`).
- O serviço conhece todos os destinatários pelas constantes `HOUSEKEEPING_EMAIL` e `FINANCE_EMAIL` (`:14-15`).
- O e-mail do hóspede continua sendo uma chamada direta, como na base.

Para pôr o restaurante ou o transfer, ou tirar um deles, será preciso editar `confirm` e `cancel` e publicar o sistema de novo. É exatamente o caso que a referência classifica como omissão.

**O exagero.** Não há estrutura sem necessidade: o agente reaproveitou o `TextFormat` que já existia (`src/main/java/hotel/documents/TextFormat.java:17-23`) para escrever valores e datas como no comprovante.

**O resto do código.** A nota vem do que o agente acrescentou:
- **Métodos longos.** `confirm` passou a mudar a situação da reserva, gravar e montar três e-mails (`ReservationService.java:60-82`). `cancel` ganhou o sinal `wasConfirmed` e dois `if` aninhados com a montagem dos textos (`:94-110`). Os dois métodos misturam a regra da reserva com a redação dos avisos.
- **Duas convenções no mesmo arquivo.** O texto do hóspede tem um método próprio (`confirmationEmailBody`, `:148-154`), mas os quatro textos novos foram escritos por concatenação, no meio dos métodos (`:74-80`, `:104-108`).
- **Consulta repetida.** `roomOf(reservation)` é chamado duas vezes seguidas em `confirm` (`:67` e `:72`). São duas consultas ao banco pelo mesmo quarto, quando bastava uma variável.

O `switch` do plano para calcular a devolução (`:95-98`) e o resto das classes vêm da base, sem piora. Os tipos estão certos: `BigDecimal` no dinheiro e `LocalDate` nas datas.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 26 métodos de teste, os mesmos 26 da base: nenhum teste novo. A cobertura foi de 94,0% de linhas (489 de 520) e 87,7% de ramos (107 de 122). Na base, era 93,8% (471 de 502) e 87,1% (101 de 116) (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

Os testes da base continuam passando. O agente não apagou nenhum, mas só trocou o número esperado de mensagens de 1 para 3:
- `src/test/java/hotel/reservations/ReservationServiceTest.java:91`
- `src/test/java/hotel/reservations/ReservationServiceTest.java:105`
- `src/test/java/hotel/web/HttpApiTest.java:81`

É tudo o que confere a parte nova. Nenhum teste olha o destinatário, o assunto ou o texto dos e-mails da governança e do financeiro. Nenhum teste confere os avisos do cancelamento.

Fazendo a pergunta da mutação, os testes continuariam passando se alguém fizesse qualquer uma destas mudanças:
- trocar `governanca@` por outro endereço, ou trocar o assunto "Pagamento recebido - reserva N" (`ReservationService.java:73`, `:78`);
- tirar a data de saída ou o número de hóspedes do e-mail da governança (`:75-77`);
- apagar o `if (wasConfirmed)` (`:101`), o que mandaria avisos ao cancelar uma reserva pendente;
- trocar `refund.signum() > 0` por `>= 0` (`:106`), o que avisaria o financeiro de um reembolso de R$ 0,00;
- apagar o bloco inteiro do cancelamento (`:101-110`).

O teste de cancelamento (`ReservationServiceTest.java:109-118`) só confere o valor da devolução, nunca as mensagens. A cobertura subiu porque os testes antigos passam pelas linhas novas, não porque as confiram.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data guardada como texto. Há uma restrição `EXCLUDE` contra duas reservas no mesmo quarto e nas mesmas noites. A única migração, `V1__create_tables.sql`, está igual à da base. As colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) não têm CHECK, o que vem da base (`measures/database.md`).
**Nota:** bom

O agente não mexeu no esquema nem no acesso a dados. Os avisos novos usam a tabela `messages` que já existia, por `SentMessages.add` (`src/main/java/hotel/messages/SentMessages.java:20-35`), com SQL parametrizado e conexões devolvidas. Como na base, a mudança de situação e as mensagens são gravadas em conexões separadas, sem transação (`ReservationService.java:66-80`). Se uma gravação de mensagem falhar, a reserva fica confirmada com parte dos avisos. Antes era só um e-mail nessa situação, agora são três, mas o jeito vem da base e o pedido não tratava disso.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu. Quando uma reserva é confirmada:
- o hóspede recebe o mesmo e-mail de antes;
- a governança recebe "Reserva confirmada - quarto 201", com a entrada, a saída e o número de hóspedes;
- o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.

Quando uma reserva confirmada é cancelada:
- a governança recebe "Reserva cancelada - quarto 201", com as datas;
- o financeiro recebe "Reembolso - reserva 1", com o valor, mas só quando há algo a devolver.

Uma reserva NON_REFUNDABLE cancelada avisa só a governança, e uma reserva pendente cancelada não avisa ninguém. Todo o resto continua funcionando, e as reservas e as mensagens continuam lá depois de reiniciar. O problema deste trabalho não aparece no que o sistema faz hoje. Aparece no próximo pedido: avisar o restaurante ou o transfer vai exigir mexer de novo no coração das reservas.

## Dúvidas

Na nota dos testes, fiquei entre `com problemas` e `ruim`. Os testes passam e conferem o resto do sistema com valores. Da parte nova, porém, só conferem a quantidade de mensagens (3): nada do destinatário, do assunto, do texto ou do cancelamento. Dei `com problemas` porque a coluna não está ausente e os 26 testes da base continuam valendo. Mas, para a mudança pedida, a rede de testes é praticamente nenhuma.
