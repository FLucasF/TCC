# Revisão: construção, cenário 07

## Visão geral

O projeto separa bem as camadas: `api.controller` (as rotas do Javalin), `service` (preço, situação da reserva, relatórios), `messaging` (as regras de envio), `database` (DAOs com JDBC puro e `CREATE TABLE` no código) e `domain` (os objetos e enums). Uma requisição entra pelas rotas de `Application.java:73-86`, passa pelo controller (por exemplo `ReservationController.java:22-43`), vai para `ReservationService` e chega a `ReservationDAO`. Os dados ficam em três tabelas no PostgreSQL. O problema mais sério: **nenhuma reserva pode ser criada**. A consulta de conflito `AND (? IS NULL OR id != ?)` com `setObject(4, null)` (`ReservationDAO.java:127,136-137`) faz o PostgreSQL recusar o comando ("could not determine data type of parameter $4", `measures/behavior-service.log:65`). Com isso, todo `POST /reservations` válido devolve 500, e confirmação, e-mail, check-in, check-out, relatórios e as regras de envio do cenário ficam inalcançáveis pela API. Os testes não pegaram o defeito porque nenhum deles toca o banco ou a API. Nas regras de envio, cada regra é uma peça própria, mas a combinação por modo ficou escrita dentro do serviço de envio, e não na montagem.

## Arquitetura

**Números:** 8 pacotes, 18 dependências entre eles, nenhum ciclo. O Javalin aparece só em `com.pousadamarealta` e `api.controller`, e o JDBC só em `database`. A maior classe é `ReservationDAO` (243 linhas), seguida de `ReservationService` (228).
**Nota:** bom

A borda HTTP é fina: os controllers leem parâmetros, convertem tipos e chamam o serviço (`ReservationController.java:22-43`, `ReportController.java:18-33`). As regras (temporada, desconto, transições de estado, devolução) moram em `service` sem Javalin e sem SQL (`ReservationService.java:30-70`, `:122-139`, `PricingService.java:13-63`). Todo o SQL fica nos DAOs. A montagem está num lugar só, `Application.java:29-45`, e passa tudo por construtor, sem `static` global. As dependências apontam da borda para as regras e das regras para os dados (`measures/dependencies.md`). Há dois reparos pequenos. A validação de campos obrigatórios fica no controller (`ReservationController.java:81-92`), o que é aceitável. O pacote `messaging` depende de `database` porque `MessageService` recebe o `MessageDAO` direto: é o "envio real" e as regras no mesmo objeto, mas isso é um assunto de design e não cria ciclo. Nada aqui um revisor exigiria mudar.

## Design de baixo nível

**Números:** as 3 regras pedidas (bloqueio, cópia, modo de teste) estão em 3 classes próprias que implementam `MessageRule` (`EmailValidationRule`, `ArchiveCopyRule`, `TestModeRule`). Nenhuma regra ficou fora da estrutura. A combinação por modo, porém, está dentro do serviço de envio (`MessageService.java:14-24`), e não na montagem.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A estrutura é uma lista de filtros aplicada pelo envio (`MessageService.java:27-44`), cada regra numa peça (`MessageRule.java`). Isso se aproxima da alternativa que a referência aceita. O que afasta: a lista é montada dentro do próprio `MessageService`, que recebe o `boolean testMode` e decide com `if (testMode) rules.add(new TestModeRule()); else rules.add(new ArchiveCopyRule());` (`MessageService.java:14-24`). `Application.java:37` só repassa o booleano. É o caso que a referência chama de malfeito: o envio ainda pergunta pelo modo de teste. Na prática, a "segunda pousada sem cópia para o arquivo" ou uma regra de madrugada "só fora do teste" exigem editar `MessageService`, quando deveriam ser só outra montagem. As regras também dependem da ordem por um acoplamento implícito. `TestModeRule` e `ArchiveCopyRule` só agem quando `status == SENT` (`TestModeRule.java:15`, `ArchiveCopyRule.java:17`), e esse status só é definido por `EmailValidationRule` (`EmailValidationRule.java:13-15`). Sem ela na frente, a mensagem sai com status `null` e as outras regras não fazem nada. Por fim, as regras alteram a mensagem que recebem em vez de devolver uma nova (`TestModeRule.java:16-18`).

**O exagero.** Não há estrutura sem necessidade. `MessageRule` tem três implementações, e não há fábricas, builders nem camadas que só repassam. O resto é direto (`RoomService` é fino, mas tem a regra do "não encontrado", `RoomService.java:20-23`).

**O resto do código.**
- **Duplicação:** o mapeamento `ResultSet` → `Reservation` está copiado três vezes (`ReservationDAO.java:92-110`, `:172-189`, `:217-234`). Os dados dos quartos também estão duplicados: capacidade e diária ficam no enum `RoomType` (`RoomType.java:6-8`) e também são gravados na tabela (`DatabaseManager.java:89-117`), mas `RoomDAO` só lê `number, type` (`RoomDAO.java:20,40`). A coluna do banco é decorativa, e as duas fontes podem divergir.
- **Tipos:** `BigDecimal` com `HALF_EVEN` em cada passo (`PricingService.java:17-18,34,39,43`), `LocalDate` e enums estão certos. O comprovante e a resposta da reserva saem como `Map<String,Object>` montado à mão (`ReservationService.java:173-186`, `ReservationController.java:110-129`), em vez de um tipo.
- **Formatação frágil:** `formatMoney` usa `String.format("R$ %,.2f")` no locale padrão da JVM e depois troca vírgula e ponto (`ReservationService.java:215`). Numa JVM com locale pt-BR, o resultado sai invertido ("R$ 1,234.56").
- **Regras com furo:** a segunda via só é recusada para `PENDING` (`ReservationService.java:144`), então uma reserva `CANCELLED` ganha comprovante, o que o texto não permite. A validação do e-mail só confere `afterAt.contains(".")` (`EmailValidationRule.java:37`) e aceita "ana@gmail." e "ana@.com", que não têm o ponto "no meio" pedido.
- **Erros:** o `JsonMapper` próprio embrulha qualquer erro do Jackson em `RuntimeException` (`Application.java:58,67`), e por isso o tratador de `JsonProcessingException` (`Application.java:123`) nunca dispara e um corpo inválido vira 500. Os DAOs também convertem tudo em `RuntimeException` genérica.
- **Estado:** `Reservation` e `Message` são sacos de setters, e as transições de estado vivem no serviço. Para este tamanho de sistema, isso é aceitável.

São defeitos localizados, que não chegam a tornar o código difícil de mudar.

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 17 métodos de teste escritos. A cobertura é de 10,9% das linhas (98 de 895) e 15,5% dos ramos (26 de 168). Os pacotes `database`, `api.controller` e `com.pousadamarealta` têm 0%, e `service` tem 27 de 196 linhas. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** ruim

O que existe confere valores. Em `PricingServiceTest.java`: a diária de alta temporada 299,88 (`:27`), o exemplo do texto 599,76 (`:59`), o desconto 116,97 e o total 662,83 (`:68,88`). Em `EmailValidationRuleTest.java`, os quatro endereços inválidos do texto (`:28,39,50,61`). Em `ArchiveCopyRuleTest.java` e `TestModeRuleTest.java`, o destinatário, o prefixo do assunto e a primeira linha (`ArchiveCopyRuleTest.java:21-25`, `TestModeRuleTest.java:21-24`). Mas a rede é estreita:
- **Nada testa o fluxo da reserva:** não há teste de criação, de conflito de noites, de capacidade, de transições de estado, de cancelamento e devolução, de comprovante, de relatórios nem dos códigos de erro. A prova é que o defeito que impede criar qualquer reserva (`ReservationDAO.java:127`) passou com o `mvn verify` verde.
- **Nada testa a combinação das regras do cenário** (`MessageService.sendMessage`): nem a ordem (a cópia logo depois da original), nem "bloqueada não gera cópia" de ponta a ponta, nem "no modo de teste não há cópia".
- **A pergunta da mutação:** se `day >= 15` virasse `day >= 16` em `PricingService.java:54`, nenhum teste falharia, porque só 20/12 é testado (`PricingServiceTest.java:33`). O mesmo vale se a ordem `TestModeRule`/`ArchiveCopyRule` em `MessageService.java:20-24` fosse invertida, ou se `afterAt.contains(".")` fosse afrouxada.

Pelos critérios da skill, testes que deixam quase todo o sistema sem conferência e não pegam um erro que quebra o sistema valem `ruim`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number)` sem índice, nenhum CHECK nas colunas de situação, tipo, plano e canal, nenhuma restrição UNIQUE ou EXCLUDE, 0 transações explícitas e 0 `FOR UPDATE`. O esquema é criado com `CREATE TABLE` no código, sem migrações (`measures/database.md`).
**Nota:** ruim

Os tipos estão certos (`DatabaseManager.java:26-66`), o SQL usa só parâmetros `?` (o script não achou SQL montado com `+`) e as conexões são devolvidas com try-with-resources. Mas:
- **A consulta de conflito não roda no PostgreSQL:** `(? IS NULL OR id != ?)` com `setObject(..., null)` (`ReservationDAO.java:127,136-137`) dá "could not determine data type of parameter $4" (`measures/behavior-service.log:65`). É o código que fala com o banco que impede gravar qualquer reserva. Nenhum dado de reserva chega à tabela.
- **Nada protege a regra de uma reserva por noite:** a proteção é só um "confere e depois insere" sem transação, sem trava e sem restrição `EXCLUDE` (`ReservationService.java:44-70`). Mesmo depois de corrigida a consulta, dois pedidos simultâneos podem gravar a mesma noite.
- **Escritas que deveriam andar juntas não andam:** a confirmação grava a reserva e depois as mensagens em comandos separados, sem transação (`ReservationService.java:87-89`, `MessageService.java:41-43`).
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` (`DatabaseManager.java:26-66`) não acrescenta uma coluna numa tabela que já existe.
- **Detalhes:** não há pool (cada chamada abre uma conexão nova pelo `PGSimpleDataSource`, `DatabaseManager.java:13-17`), e capacidade e diária na tabela `rooms` nunca são lidas (`RoomDAO.java:20`).

Um erro de SQL que impede gravar a entidade central é um defeito sério desta coluna.

## Comportamento

**Números:** 15 de 65 conferências passaram. A parte comum passou em 12 de 46; os dados depois de reiniciar, em 1 de 3; o cenário no modo normal, em 1 de 9; o cenário no modo de teste, em 0 de 5; as reservas simultâneas, em 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

Quase todas as falhas vêm de uma causa só: `POST /reservations` válido devolve 500, pelo erro de SQL descrito no Banco. Para o dono da pousada, isso quer dizer que o site não consegue criar nenhuma reserva. Também não há confirmação, e-mail, check-in, check-out, comprovante, cancelamento, linhas nos relatórios, nem regra de envio do cenário que dê para ver funcionando. As 15 conferências que passaram são o que acontece antes do SQL de conflito (404 de quarto, 422 de capacidade, 400 de hóspedes e de datas, campos faltando), a lista de quartos, os cabeçalhos dos CSVs e a reserva inexistente. As duas reservas simultâneas "nunca duplicaram" porque as duas recebem 500 (10 de 10 rodadas com `500 500`). Há outras falhas próprias:
- Um corpo que não é JSON dá 500 em vez de 400 (`Application.java:67` esconde a `JsonProcessingException`). Conferi chamando a API à mão: `Server Error 500`.
- "O 201 comporta 3" e "a diária do 201" falharam só porque o roteiro procura `number` como número, e o projeto devolve `"201"` como texto. Conferi `GET /rooms` à mão e os valores estão certos (`capacity:2/3/4`, `nightlyRate:249.90…`). O texto não fixa o tipo, então essa falha não pesa.

Fora do roteiro, pela leitura (sem mudar a nota): a segunda via de reserva cancelada seria emitida (`ReservationService.java:144`), e "ana@gmail." passaria como válido (`EmailValidationRule.java:37`).

## Dúvidas

- **A classe do pattern:** fiquei entre `malfeito` e `alternativa equivalente`. As três regras são peças próprias e nenhuma pergunta pelo modo, o que pende para a alternativa. Mas a escolha da combinação está escrita no serviço de envio (`MessageService.java:20-24`), e não na montagem, que é o que a referência põe como condição. Por isso dei `malfeito`.
- **O que passaria com a consulta corrigida:** não dá para saber, pela API, quanto do resto do comportamento estaria certo. Pela leitura, o preço, as transições, a devolução, os relatórios e as regras de envio parecem coerentes com o texto, com as exceções citadas acima.
