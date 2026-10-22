# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em sete pacotes: `web` (os controllers e os DTOs em `web.dto`), `service` (as
regras e o e-mail de confirmação), `domain` (a reserva, que calcula o preço e controla as transições, e os enums),
`messaging` (as regras de envio) e `repository` (JDBC puro sobre um `PGSimpleDataSource`). Tudo é montado em
`App.java:24-66`. Uma requisição passa pelo controller, que valida o corpo (`web/ReservationController.java:30-64`),
segue para o `ReservationService`, que usa o domínio e grava pelo repositório. As regras de envio pedidas pelo cenário
são uma lista de `MessageRule` aplicada pelo `MessageProcessor`, e a combinação por modo é decidida em
`App.java:87-100`. Os dados ficam em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. O roteiro de
comportamento passou em 61 de 65 conferências, e as regras de envio passaram em todas (14 de 14). O problema mais
sério está em outro lugar: nada protege contra duas reservas para a mesma noite. O serviço consulta e depois insere,
sem transação, trava ou restrição no banco, e nas 10 rodadas de pedidos simultâneos as duas reservas foram criadas.
Há também defeitos menores. O `mvn verify` falhou na medição, com 10 erros de conexão num teste de integração frágil
que apaga as tabelas do banco de `DB_URL`. A validação de e-mail aceita `ana@.com` e `ana@gmail.`. E o JSON mostra
`0` em vez de `0.00` na resposta da criação.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `web` e na raiz (`App`), o JDBC só em `repository` e na raiz. `domain`, `messaging` e `service` não usam nenhuma
biblioteca.
**Nota:** com problemas

A separação básica está correta. As regras (`domain/Reservation.java:112-146`, `service/ReservationService.java`,
`messaging/*`) não conhecem nem o Javalin nem o JDBC. O SQL fica todo em `repository/*` e a montagem fica num lugar só
(`App.java:24-43`). As dependências seguem de `web` para `service`, de `service` para `repository` e de `repository`
para `domain`, sem ciclo.

Há misturas localizadas:

- **A camada `web` pula as regras e vai direto ao repositório** (`web -> repository` em `measures/dependencies.md`).
  `web/RoomController.java:15` recebe o `RoomRepository`. O `ReservationController` também o recebe e busca o quarto
  para montar o comprovante (`web/ReservationController.java:148` e `:170`).
- **Uma regra de negócio mora no handler.** A segunda via só existe para reserva paga, e quem decide isso é
  `web/ReservationController.java:164-168`, embora a mesma regra já esteja no serviço
  (`service/ReservationService.java:112-114`).
- **Validações de domínio ficam no controller.** A saída depois da entrada e o mínimo de um hóspede estão em
  `web/ReservationController.java:48` e `:53`. O domínio aceita uma reserva com 0 hóspedes ou com a saída antes da
  entrada.

Nenhuma classe domina o sistema: as maiores têm cerca de 230 linhas (`ReservationRepository`, `Reservation`).

## Design de baixo nível

**Números:** as 3 regras do cenário estão na estrutura: endereço inválido (`messaging/EmailValidationRule.java`),
cópia para o arquivo (`messaging/ArchiveCopyRule.java`) e modo de teste (`messaging/TestModeRule.java`). Nenhuma ficou
fora dela. A escolha por modo é um único `if` na montagem (`App.java:92-97`), e nenhuma regra pergunta pelo modo.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede um Decorator: cada regra embrulharia o envio. O agente fez outra coisa, que a referência
aceita de antemão: uma lista de filtros aplicada pelo envio. `MessageRule` tem um único método `apply`
(`messaging/MessageRule.java:3-5`), cada regra é uma classe própria, e o `MessageProcessor` aplica a lista em ordem
(`messaging/MessageProcessor.java:13-24`). A combinação é decidida na montagem: a validação vale sempre, e o modo de
teste e a cópia se excluem (`App.java:87-100`). Nenhuma regra consulta `HOTEL_TEST_MODE`. As regras novas anunciadas
(madrugada, limite por dia) caberiam como novas `MessageRule` que mudam o status. Uma pousada sem a cópia seria uma
lista sem `ArchiveCopyRule`.

O que afasta do Decorator:

- **As regras dependem da ordem e de estado mutável compartilhado.** Elas mexem por setters num único
  `ProcessedMessage` (`messaging/ProcessedMessage.java:26-40`). `TestModeRule` e `ArchiveCopyRule` só funcionam se
  vierem depois de `EmailValidationRule`, porque leem o status que ela deixou (`messaging/TestModeRule.java:10`,
  `messaging/ArchiveCopyRule.java:10`).
- **A cópia não passa pelas regras seguintes.** Ela é criada como "mensagem adicional"
  (`messaging/ArchiveCopyRule.java:14-23`) e vai direto para a saída (`messaging/MessageProcessor.java:22`). Uma regra
  de madrugada ou de limite posta depois da cópia não valeria para ela.

**O exagero.** Não há. `PendingMessage`, `ProcessedMessage` e `Message` são três representações da mensagem, mas as
duas primeiras fazem parte da estrutura do pattern. Não há interface com uma implementação só, fábrica nem camada que
só repassa. `MessageService` (`service/MessageService.java:20-33`) é fino, mas faz a ponte entre as regras e o
repositório.

**O resto do código.** Os tipos estão bem escolhidos: `BigDecimal` com `HALF_EVEN` a cada passo
(`domain/Reservation.java:69-110`), `LocalDate` e enums (`RoomType`, `RatePlan`, `ReservationStatus`). As transições
ficam no domínio (`domain/Reservation.java:112-146`). Há defeitos localizados:

- **O mapeamento de erros para HTTP está copiado** em cada handler, com os mesmos `catch` repetidos seis vezes
  (`web/ReservationController.java:79-86`, `:95-99`, `:107-112`, `:121-126`, `:135-140`, `:149-156`, `:172-176`). Um
  `app.exception(...)` resolveria isso num lugar só.
- **Um `catch (Exception e)` esconde a causa** (`web/ReservationController.java:85-86`). Qualquer falha do banco na
  criação vira `400 INVALID_REQUEST` "Invalid request body".
- **A validação do e-mail não segue a regra.** O texto pede "um nome com pelo menos um ponto no meio", mas
  `messaging/EmailValidationRule.java:33` só confere `afterAt.contains(".")`. Chamei a API e confirmei: `ana@.com` e
  `ana@gmail.` saem com status SENT e ainda geram cópia para o arquivo.
- **O zero sai sem as duas casas.** `BigDecimal.ZERO` tem escala 0 (`domain/Reservation.java:38` e `:102`), então a
  resposta da criação mostra `"amountPaid":0` e `"discount":0`. Lida do banco, a mesma reserva mostra `0.00`.
- **Há código morto:** a variável `jdbcUrl`, que nunca é usada (`App.java:79`).
- **O comprovante é montado em dois lugares que não se falam:** o texto no serviço
  (`service/ReservationService.java:111-137`) e o JSON no controller (`web/dto/DocumentResponse.java:23-38`), cada um
  com a sua checagem de "pago".

## Testes

**Números:** 34 testes rodaram na medição: 24 de unidade passaram e os 10 de integração (`ReservationSystemIT`)
deram erro de conexão (`java.net.ConnectException`). São 34 métodos de teste escritos. A cobertura é de 18,3% das
linhas (151 de 823) e 27,8% dos ramos (44 de 158). `messaging` tem 66 de 69 linhas cobertas e `domain` 85 de 162;
`service`, `repository` e `web` têm 0 (`measures/tests.md`).
**Nota:** com problemas

**Os testes de unidade conferem as regras com valores.** Em `ReservationTest` estão a alta temporada (599,76), o
desconto com meio para o par (89,96 e 509,80) e as devoluções no cancelamento por plano e por situação
(`src/test/java/com/pousadamarealta/domain/ReservationTest.java:25-58`, `:108-141`). `MessageProcessorTest` confere as
três regras do cenário: o bloqueio de `usergmail.com`, de `user@gmailcom` e de `user@@gmail.com`; o desvio para
`testes@`, com "[TESTE] " e a linha "Destinatário original"; a cópia com "[Cópia] " e o mesmo texto; e a ausência de
cópia para mensagem bloqueada (`src/test/java/com/pousadamarealta/messaging/MessageProcessorTest.java:31-177`).

**Os defeitos:**

- **O teste de integração é frágil.** Ele sobe o serviço num processo separado e espera um `Thread.sleep(3000)` fixo,
  sem conferir se o serviço ficou de pé (`src/test/java/com/pousadamarealta/ReservationSystemIT.java:61`). Na medição,
  com o JaCoCo ligado, os 10 testes falharam por conexão recusada. Rodei de novo numa cópia do projeto, sem o JaCoCo,
  e os 10 passaram, então a falha vem do tempo de espera e não de um erro do código. Como roda em outro processo, esse
  teste também não conta na cobertura.
- **O teste de integração apaga dados.** Ele faz `DROP TABLE` das três tabelas do banco que estiver em `DB_URL`
  (`ReservationSystemIT.java:79-81`). Quem rodar `mvn verify` com as variáveis de produção perde todas as reservas.
- **Os testes dependem da ordem.** Os IDs estão fixos (`/reservations/3/confirmation`, `ReservationSystemIT.java:298`)
  e a ordem é forçada por `@Order`.
- **Algumas regras ficam sem rede:**
  - Nada confere a montagem por modo (`App.java:92-97`). Se alguém pusesse a cópia também no modo de teste, nenhum
    teste falharia.
  - Nenhum teste cobre o ponto "no meio" do domínio, que é justamente o defeito encontrado.
  - O texto do comprovante e o corpo do e-mail não têm teste.
  - A disputa por um quarto também não. A troca de `<` por `<=` em `findByRoomAndDateRange` só seria pega pelo teste
    de integração, se ele rodasse.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):
- a chave estrangeira `reservations(room_number)` sem índice;
- nenhuma CHECK sobre as colunas de status, plano e tipo;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações explícitas e 0 `FOR UPDATE`.

O SQL usa sempre parâmetros, e o script não encontrou nenhum candidato a SQL montado com `+`.
**Nota:** ruim

**O defeito sério é a regra "um quarto não pode ter duas reservas na mesma noite": nada a protege.**
`service/ReservationService.java:36-46` consulta os conflitos (`repository/ReservationRepository.java:113-144`) e
depois insere (`repository/ReservationRepository.java:23-63`). Cada passo usa uma conexão própria em autocommit, sem
transação, sem trava e sem restrição de exclusão no esquema (`repository/DatabaseInitializer.java:26-46`). Não há nem
um `synchronized`. O roteiro mostrou o efeito: nas 10 rodadas de dois pedidos simultâneos, as duas reservas foram
criadas (`measures/behavior.md`). O resultado é um dado errado no banco: o mesmo quarto vendido duas vezes.

**Os outros defeitos:**

- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código
  (`repository/DatabaseInitializer.java:19-57`), sem migrações, e não saberia acrescentar uma coluna a um banco que já
  tem dados.
- **Faltam o índice da chave estrangeira e as CHECK.** Não há índice em `room_number` nem CHECK nos status e planos.
- **Não há pool de conexões.** `PGSimpleDataSource` (`App.java:78`) abre uma conexão física a cada chamada.
- **Escritas que deveriam andar juntas não estão numa transação.** A confirmação grava a reserva
  (`service/ReservationService.java:63`) e depois as mensagens, uma a uma (`service/MessageService.java:23-32`). Uma
  falha no meio deixa a reserva confirmada sem o e-mail, ou o e-mail sem a cópia.
- **O relatório de ocupação consulta o banco a cada linha.** Ele faz uma consulta do quarto por linha
  (`service/ReportService.java:32`), embora já tenha a lista de quartos na linha 26. É uma ineficiência pequena.

Os dados persistem, e as conexões são devolvidas com try-with-resources.

## Comportamento

**Números:** 61 de 65 conferências passaram (`measures/behavior.md`):
- a parte comum: 43 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 1 de 2.

**Nota:** com problemas

O cenário funciona inteiro pela API, nos dois modos:
- o endereço inválido sai BLOCKED, com o endereço original e sem cópia;
- a cópia vem logo depois da original, com "[Cópia] " e o mesmo texto;
- no modo de teste, tudo vai para `testes@`, com "[TESTE] " e "Destinatário original", e sem a cópia.

As falhas:

- **O mesmo quarto pode ser vendido duas vezes.** Em 10 de 10 rodadas, dois pedidos simultâneos para o mesmo quarto
  e as mesmas noites foram ambos aceitos (201 e 201). Num fim de semana de procura alta, o site pode vender o mesmo
  quarto para dois hóspedes. O defeito em si pesa na coluna do banco.
- **O zero aparece sem as duas casas.** A reserva recém-criada vem com `"amountPaid":0` e `"discount":0`, e não
  `0.00` como o texto pede. Ao ler a mesma reserva de novo, vem `0.00`, porque o valor passou pelo banco. É um
  defeito real, mas de formato.
- **"o 201 comporta 3" e "a diária do 201 é 389.90" vieram vazias.** O agente devolve `number` como texto (`"201"`) e
  o roteiro procura um número. Os valores estão certos (capacidade 3, diária 389.90, conferidos com `curl`). O texto
  não fixa o tipo do número do quarto, então é uma escolha razoável e não pesa.

Fora do roteiro, chamei a API e confirmei que `ana@.com` e `ana@gmail.` são aceitos como endereços válidos: recebem a
mensagem e geram cópia. Isso já foi contado no design.

## Dúvidas

- **Os 10 erros de conexão dos testes.** Eles aparecem com o JaCoCo ligado e somem sem ele. A causa provável é a
  espera fixa de 3 s, mas não descobri por que o serviço demorou mais na medição.
- **A nota do banco.** A falta de qualquer proteção contra a reserva dupla, confirmada nas 10 de 10 rodadas, me levou
  a `ruim`. Um leitor que siga só a lista de casos `ruim` da skill do banco poderia dar `com problemas`.
- **A classe do design.** A lista de regras foi classificada como alternativa equivalente, mas tem dois pontos fracos
  em relação ao Decorator: a dependência de ordem entre as regras e o fato de a cópia não passar pelas regras
  seguintes. Hoje isso não quebra nada.
