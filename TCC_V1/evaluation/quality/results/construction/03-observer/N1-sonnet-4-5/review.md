# Revisão: construção, cenário 03

## Visão geral

O agente montou um projeto Javalin em camadas: `controller` (os handlers), `service` (as regras), `repository` (JDBC puro com HikariCP), `domain` (modelo, enums e eventos) e `dto`, tudo ligado à mão em `App.java:34-82`. Uma requisição entra no controller, que lê o corpo e chama o `ReservationService`. O serviço valida, calcula o preço, grava pelo repositório e publica um evento num `EventPublisher` (`ReservationService.java:78,96`). Três ouvintes registrados no `main` (`App.java:42-45`) gravam os e-mails na tabela `messages`. O esquema é criado com `CREATE TABLE IF NOT EXISTS` (`config/Database.java:43-80`). O Observer pedido pelo texto está bem feito. Os problemas mais sérios estão fora dele:
- **CPF com máscara:** a coluna `guest_cpf` é `VARCHAR(11)` (`config/Database.java:55`). Um CPF escrito como o texto mostra (`111.222.333-44`) estoura o tamanho da coluna, e toda criação de reserva responde 500 (`measures/behavior-service.log:68`). O roteiro passou em só 16 de 66 conferências.
- **Reserva dobrada:** nada impede duas reservas do mesmo quarto ao mesmo tempo. Num teste manual, 9 de 10 rodadas simultâneas criaram as duas.
- **Preço errado em centavos:** o preço é guardado como uma diária média arredondada (`ReservationService.java:141`). Quando a estadia atravessa o início ou o fim da alta temporada, o total sai com 1 centavo de diferença.
- **`mvn verify` não passa:** o teste de integração não teria como funcionar nem com Docker.

## Arquitetura

**Números:** 11 pacotes, 31 dependências entre eles, nenhum ciclo. Só `controller` (e o `App`) usam o Javalin; só `repository` e `config` usam o JDBC; `service` e `service.event.listeners` não usam nenhum dos dois. As maiores classes são `ReservationRepository` (235 linhas), `ReservationService` (198) e `ReservationController` (175).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: da borda para as regras, e das regras para os dados.
- **A borda HTTP é fina na maior parte:** os handlers convertem a requisição, chamam o serviço e traduzem as exceções (`controller/ReservationController.java:26-59`).
- **As regras não conhecem o Javalin nem o SQL** (`service/ReservationService.java`), e o SQL fica nos três repositórios.
- **A montagem fica num lugar só** (`App.java:36-59`).

As misturas são localizadas:
- **O acesso ao banco é um `static` global.** `Database` guarda o pool num campo estático (`config/Database.java:12`) e os repositórios o buscam por dentro, com `Database.getConnection()` (`repository/ReservationRepository.java:34`, `repository/MessageRepository.java:23`). O pool não é injetado. Por isso o teste de unidade precisa de subclasses dos repositórios (`ReservationServiceTest.java:209-259`), e o teste de integração inicializa o banco duas vezes (`ReservationSystemIT.java:47` e `App.java:34`).
- **Uma regra mora no handler.** A segunda via só existe para reserva paga, e essa regra é decidida em `controller/ReservationController.java:137-141`. Ela está duplicada em `service/DocumentService.java:17-19`.
- **Um handler fala direto com o repositório:** `controller/MessageController.java:17` não passa por nenhum serviço.

Nada disso é grave, mas um revisor exigente pediria para injetar o `DataSource` e tirar a regra do handler.

## Design de baixo nível

**Números:** há 3 interessados (hóspede, governança e financeiro) e 2 eventos (confirmação e cancelamento). Os 3 interessados estão na estrutura e nenhum fica fora dela: são 3 implementações de `EventListener`, registradas em `App.java:43-45`. O serviço não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** É um Observer de verdade:
- Há uma interface de ouvinte (`service/event/EventListener.java:5-7`) e um publicador que percorre a lista (`service/event/EventPublisher.java:12-20`). Esse publicador é criado e preenchido no `main` e entregue ao serviço pelo construtor (`App.java:42-50`).
- O serviço só anuncia o que aconteceu: `eventPublisher.publish(new ReservationConfirmedEvent(...))` em `ReservationService.java:78` e `ReservationCancelledEvent(reservation, wasConfirmed)` em `ReservationService.java:96`. Ele não sabe quem é avisado.
- O e-mail do hóspede também é um ouvinte (`service/event/listeners/GuestNotificationListener.java:39-43`), e o cancelamento usa a mesma estrutura que a confirmação.
- Pôr o restaurante ou o transfer é escrever uma classe e uma linha no `main`.

O que afasta um pouco:
- Cada ouvinte escolhe o evento com `instanceof` (`FinanceNotificationListener.java:34-41`, `HousekeepingNotificationListener.java:25-33`).
- A regra "a reserva pendente cancelada não avisa ninguém" é repetida em cada ouvinte, pelo `wasConfirmed()` (`HousekeepingNotificationListener.java:29`, `FinanceNotificationListener.java:38`), em vez de estar num lugar só.

É uma variação aceitável, não um desvio.

**O exagero** é pequeno, mas existe:
- `RoomService` é uma camada que só repassa ao repositório (`service/RoomService.java:15-21`), e `getRoomByNumber` nem é usado.
- O record `Document` (`domain/model/Document.java:8-21`) é copiado campo a campo num `DocumentResponse` quase idêntico (`dto/DocumentResponse.java:23-51`).

**O resto do código:**
- **O preço é modelado errado.** O serviço soma as noites e guarda a média arredondada como "diária" (`service/ReservationService.java:125-142`). Depois o subtotal é refeito como diária vezes noites (`domain/model/Reservation.java:111-114`). O texto pede a soma das noites, cada uma arredondada. Quando a estadia mistura temporadas, o total erra 1 centavo: DELUXE de 13 a 16/12/2027 deveria dar 389,90 + 389,90 + 467,88 = 1.247,68, e o sistema devolveu 1.247,67.
- **A formatação está duplicada.** O formatador de dinheiro em pt-BR aparece igual em `GuestNotificationListener.java:23-28` e em `FinanceNotificationListener.java:20-25`. O de datas aparece em `GuestNotificationListener.java:20` e em `HousekeepingNotificationListener.java:16`.
- **O comprovante é montado em dois lugares.** O texto sai de `GuestNotificationListener.java:63-83` e o objeto sai de `DocumentService.java:16-38`.
- **As transições são conferidas duas vezes:** no serviço (`ReservationService.java:71-73`, `87-90`) e na entidade (`Reservation.java:129-131`, `137-139`).
- **O tratamento de erros se repete.** O mesmo bloco `try/catch` está copiado em cada handler (`ReservationController.java:74-129`), em vez de um `app.exception` só. `IllegalArgumentException` vira 400 por atacado (`ReservationController.java:48`), e o corpo que não é JSON escapa como 500.
- **O zero perde as duas casas.** `BigDecimal.ZERO` sem escala aparece em `Reservation.java:48` e `147`, e por isso `amountPaid` e `refund` saem como `0` em vez de `0.00` no JSON.

São defeitos localizados, e o erro de preço é o mais sério deles.

## Testes

**Números:** pelo `measures/tests.md`:
- rodaram 10 testes: 9 de unidade passaram e 1 erro, que é a classe de integração inteira (ela não achou Docker);
- há 19 métodos de teste escritos;
- a cobertura de linhas é de 145 de 832 (17,4%) e a de ramos, de 35 de 190 (18,4%);
- os ouvintes têm 0 de 102 linhas cobertas, e o controller, 0 de 155.

**Nota:** ruim

`ReservationServiceTest.java` confere regras com valores:
- baixa temporada, 499,80 (`ReservationServiceTest.java:49-61`);
- alta temporada, 299,88 e 599,76 (`ReservationServiceTest.java:63-75`);
- desconto de 173,97 sobre 1.159,80 (`ReservationServiceTest.java:77-88`);
- capacidade, conflito, confirmação com pago igual ao total, e as devoluções dos dois planos (`ReservationServiceTest.java:90-198`).

Esses testes conferem que o serviço publica o evento e com que `wasConfirmed`. Não conferem nenhum e-mail.

A parte nova do cenário (os destinatários, os assuntos, o valor pago, a regra "sem devolução não avisa o financeiro") só aparece em `ReservationSystemIT.java:115-199`. Esse teste não rodou, e não rodaria nem com Docker:
- Ele passa o banco do Testcontainers por `System.setProperty` (`ReservationSystemIT.java:42-45`), mas `App.main` lê `System.getenv` (`App.java:100-102`). O serviço tentaria `localhost:5432/pousada`.
- Ele confere um campo `nightlyRate` na reserva (`ReservationSystemIT.java:228`), e a resposta não tem esse campo (`dto/ReservationResponse.java:7-24`).
- Ele espera com `Thread.sleep(2000)` (`ReservationSystemIT.java:53`).

O resultado é que o `mvn verify`, pedido pelo texto, falha.

Na pergunta da mutação, nenhum teste que roda falharia se alguém:
- trocasse o endereço da governança;
- tirasse o `hasRefund` do financeiro (`FinanceNotificationListener.java:38`);
- apagasse o `wasConfirmed()` da governança (`HousekeepingNotificationListener.java:29`).

O caso de temporada misturada, que tem o erro de centavo, também não tem teste.

## Banco

**Números:**
- **Tabelas:** 3 (`rooms`, `reservations`, `messages`), todas com chave primária.
- **Dinheiro:** em `numeric(10,2)`.
- **Datas:** em `date`.
- **Chave estrangeira:** `reservations → rooms`, com índice.
- **Status, plano, tipo e canal:** sem CHECK.
- **Restrições de regra (UNIQUE/EXCLUDE/CHECK):** nenhuma.
- **Transações explícitas:** 0. **`FOR UPDATE`:** 0.
- **SQL montado com `+`:** nenhum.

**Nota:** ruim

- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` dentro do código (`config/Database.java:43-80`), sem migrações, e não saberia acrescentar uma coluna numa tabela que já existe.
- **A coluna do CPF quebra a criação.** `guest_cpf VARCHAR(11)` (`config/Database.java:55`) só aceita CPF sem máscara. O texto mostra o CPF como `111.222.333-44`, e com ele o INSERT falha: "value too long for type character varying(11)" (`measures/behavior-service.log:68`). Quem chama recebe um 500, porque a `RuntimeException` do repositório não é tratada (`repository/ReservationRepository.java:47`).
- **Nada protege a regra do quarto.** "Um quarto não pode ter duas reservas na mesma noite" depende só de um `SELECT COUNT(*)` seguido de um INSERT separado (`ReservationService.java:49-60`, `repository/ReservationRepository.java:118-153`). Não há transação, trava, `synchronized` nem restrição `EXCLUDE`. No teste manual com CPF só de dígitos, 9 de 10 rodadas de dois pedidos simultâneos criaram as duas reservas para o mesmo quarto e a mesma noite. Isso é dado errado no banco.
- **A confirmação também não é atômica.** O status é gravado e cada mensagem é gravada numa conexão separada, sem transação (`ReservationService.java:75-78`), e duas confirmações simultâneas passariam ambas pela conferência de PENDING.

Pontos bons: o SQL usa sempre parâmetros, as conexões são devolvidas com try-with-resources e há um pool. Os filtros por mês usam `EXTRACT` (`repository/ReservationRepository.java:161-162`), que não aproveita o índice de `check_in`; com este volume, isso é só uma nota.

## Comportamento

**Números:** 16 de 66 conferências passaram:
- a parte comum: 12 de 46;
- depois de reiniciar: 1 de 3;
- o cenário 03: 2 de 15;
- as reservas simultâneas: 1 de 2 (nenhuma rodada criou as duas, mas só porque todas deram 500).

**Nota:** ruim

Quase todas as falhas vêm de uma causa só: criar a reserva com o CPF `111.222.333-44` responde 500. Sem reserva não há confirmação, e-mail, check-in, relatório nem aviso para a governança ou o financeiro. Para o dono da pousada, isso quer dizer que o site não consegue criar reservas se mandar o CPF com pontos e traço, que é o formato que o próprio texto usa. O texto não diz como o CPF chega, mas responder 500 nunca é aceitável.

Para separar esse defeito do resto, subi o serviço e chamei a API com o CPF só de dígitos (`11122233344`):
- **Funcionou:**
  - a parte do cenário: os três e-mails da confirmação, com o assunto, as datas, "Hóspedes: 2" e "R$ 779,80";
  - os dois do cancelamento de uma confirmada, com "Reembolso - reserva 1" e "R$ 779,80";
  - o cancelamento de uma pendente, sem aviso nenhum;
  - o e-mail do hóspede, com o comprovante no formato pedido;
  - o check-in, o check-out com o comprovante, a segunda via recusada com 409 para reserva pendente, e os dois relatórios.
- **Continuou errado:**
  - **o total com temporada misturada** erra 1 centavo: 1.247,67 em vez de 1.247,68 no DELUXE de 13 a 16/12/2027, e 1.325,67 em vez de 1.325,66 no de 27/02 a 02/03/2027;
  - **um corpo que não é JSON** dá 500 em vez de 400 INVALID_REQUEST;
  - **`amountPaid` e `refund`** saem como `0` em vez de `0.00`, e o financeiro de um mês vazio sai `total,0`;
  - **as reservas simultâneas:** 9 de 10 rodadas criaram as duas reservas do mesmo quarto, o que para a pousada é hóspede chegando a um quarto já ocupado. Esse defeito pesa na coluna do banco, mas aparece aqui numa chamada que o texto descreve.

A falha "o 201 comporta 3" vem de outra coisa: o roteiro procura o quarto pelo número 201, e o sistema devolve `"number":"201"` como texto. O texto não fixa esse tipo, então essa falha não pesou.

## Dúvidas

- **O formato do CPF.** O texto não diz como o site manda o CPF. Mas o único exemplo vem com máscara, e a resposta ao formato inesperado é um 500, não uma recusa. Por isso tratei como erro do agente.
- **O exagero.** É pequeno: um `RoomService` que só repassa e um DTO que copia o `Document`. Um revisor menos estrito poderia dar `contenção`.
- **A nota dos testes.** Os testes de unidade que rodam são bons no que cobrem. A nota `ruim` vem de três fatos lidos no código: a parte nova só é conferida por um teste de integração que não funcionaria nem com Docker, a cobertura é de 17%, e o `mvn verify` falha.
