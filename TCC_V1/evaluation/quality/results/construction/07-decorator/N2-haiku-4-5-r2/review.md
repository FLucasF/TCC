# Revisão: construção, cenário 07

## Visão geral

O agente montou um projeto Maven com 8 pacotes: `http` (handlers do Javalin, montados em `Api.register`), `service`
(regras da reserva, relatórios e envio de mensagens), `messaging` (as regras de envio, numa lista de
`MessageProcessingRule`), `repository` (JDBC puro, uma conexão nova por chamada via `DriverManager`), `domain`, `util`
(preço, dinheiro, validação de e-mail) e `config` (variáveis de ambiente em campos `static` e `CREATE TABLE IF NOT
EXISTS`). Uma requisição passa pelo handler, que lê o corpo com `ctx.bodyAsClass(JsonObject.class)` (Gson), chama o
serviço, que chama o repositório, e responde com `ctx.json(...)`. O defeito mais sério é que **o sistema não funciona
pela API**: o `pom.xml` não traz o Jackson e o agente não configurou o Gson como `JsonMapper` do Javalin, então todo
`ctx.json`/`ctx.bodyAsClass` lança exceção e o serviço responde 500 (o log do roteiro traz "It looks like you don't
have an object mapper configured", `measures/behavior-service.log:486`). O roteiro do comportamento passou em só 6 de
65 conferências. Além disso, a leitura mostra erros de regra que apareceriam mesmo com o JSON consertado: a alta
temporada cobre dezembro inteiro, o preço de todas as noites sai da data de entrada, a confirmação exige um campo
`amount` que o texto não prevê, e o dinheiro sai no JSON como texto.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `http`
e no `Main`; JDBC só em `repository` e `config`. Maior classe: `http/ReservationHandler.java`, 257 linhas.
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo (borda → serviço → repositório), sem ciclo, e o SQL
está todo nos repositórios. Mas a borda HTTP não é fina:

- `ReservationHandler` monta o e-mail de confirmação e dispara o envio (`src/main/java/com/pousada/marealta/http/ReservationHandler.java:82-84`
  e `:196-208`): a regra "ao confirmar, mandar o comprovante" mora no handler, não no serviço. Monta também o
  comprovante consultando o repositório direto (`:174`, `:197`), e decide no handler quem tem direito à segunda via
  (`:135-137`).
- A tradução de erro para HTTP compara o texto da mensagem da exceção (`ReservationHandler.java:218-243`): o serviço
  lança `IllegalArgumentException("Quarto não encontrado")` (`service/ReservationService.java:26`) e a borda depende
  dessa frase exata.
- `http` depende de `repository` direto (`RoomHandler.java:20`, `ReservationHandler.java:21`).
- A montagem fica em `http/Api.java:19-38` (aceitável), mas a configuração é estado global `static`
  (`config/AppConfig.java:4-8`), lido de dentro das regras de envio (`messaging/ArchiveCopyRule.java:17`,
  `messaging/TestModeRedirectRule.java:13`) e de cada repositório via `DatabaseConfig.getConnection()` estático
  (`config/DatabaseConfig.java:34-40`).

São misturas localizadas, não a ausência de camadas; por isso `com problemas`.

## Design de baixo nível

**Números:** 3 regras de envio na estrutura (`EmailValidationRule`, `TestModeRedirectRule`, `ArchiveCopyRule`), 0
fora dela; mas 2 das 3 decidem o modo por dentro (`AppConfig.isTestMode()`), e a montagem empilha sempre as mesmas 3.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O agente fez uma lista de filtros (`MessageProcessor`, `messaging/MessageProcessor.java:18-29`), que a
referência aceita como alternativa equivalente, *desde que a combinação por modo seja decidida na montagem e não
dentro das regras*. Não é o que aconteceu:

- `ArchiveCopyRule.process` pergunta `AppConfig.isTestMode()` para não copiar no modo de teste
  (`messaging/ArchiveCopyRule.java:17`), exatamente o exemplo de malfeito da referência;
- `TestModeRedirectRule` também pergunta o modo por dentro (`messaging/TestModeRedirectRule.java:13`), em vez de só
  entrar na lista quando o modo de teste está ligado;
- a montagem é fixa e não olha o modo (`http/Api.java:28-31`): para a "segunda pousada sem cópia" ou para uma regra
  "só fora do teste", seria preciso editar as regras, e não a montagem. O `ArchiveCopyRule(boolean enabled)`
  (`ArchiveCopyRule.java:11`) aponta nessa direção, mas recebe sempre `true`;
- as regras dependem da ordem e de um estado compartilhado: cada uma recebe a mesma `Message` mutável
  (`MessageProcessor.java:24-26`) e muda `to`, `subject` e `body` (`TestModeRedirectRule.java:18-20`); a cópia lê o
  status que a validação deixou (`ArchiveCopyRule.java:17`). Trocar a ordem em `Api.java:29-31` muda o resultado sem
  aviso.

O que aproxima: cada regra é uma classe própria, com a mesma interface (`MessageProcessingRule.java:5-7`), e o envio
(`MessageService.sendMessage`, `service/MessageService.java:18-23`) não tem os três `if`. Por isso não é omissão.

**O exagero.** Pouco a apontar: `MessageProcessingContext` é só um invólucro de uma lista
(`messaging/MessageProcessingContext.java:7-21`) e `Money` tem um método só, mas nada disso é uma camada sem
propósito. Contenção.

**O resto do código.** Os defeitos estão espalhados:

- erros: todo repositório engole a exceção do banco e devolve vazio (`repository/ReservationRepository.java:92-95`,
  `:118-121`, `:144-147`, `:170-173`; `RoomRepository.java:29-31`, `:45-48`, `:61-64`; `MessageRepository.java:58-60`).
  Uma queda do banco vira "reserva não encontrada", ou pior, "nenhum conflito" na checagem de disponibilidade
  (`ReservationRepository.java:98-122`), que deixa a reserva duplicada passar. Os handlers fazem `catch (Exception)`
  e respondem 400 genérico a qualquer erro (`ReservationHandler.java:51-53` e mais seis);
- códigos de erro carregados no texto da exceção e comparados por `equals` (`ReservationHandler.java:218-243`);
  "menos de um hóspede" cai no mesmo texto da capacidade e vira 422 `CAPACITY_EXCEEDED`
  (`ReservationService.java:28-30`), quando o texto pede 400;
- regras de preço erradas: `isHighSeason` considera dezembro inteiro (`util/PricingCalculator.java:41-43`) e não de
  15 de dezembro; e o preço da noite é calculado uma vez com a data de entrada e multiplicado pelas noites
  (`ReservationService.java:42-43`), então uma estadia de 27/02 a 02/03 paga alta temporada em todas as noites;
- dinheiro formatado com `String.format("%.2f")`, que depende da localidade da máquina e sai como texto no JSON
  (`ReservationHandler.java:160-166`, `:190-192`); no comprovante não há separador de milhar
  (`domain/Receipt.java:60-62`), então R$ 1.234,56 sai "R$ 1234,56";
- duplicação: a construção do `Receipt` copiada em `ReservationHandler.java:174-178` e `:197-201`;
- estado: `static` mutável em `AppConfig.java:4-8`; `Message` e `Reservation` com setters usados de fora
  (`Message.java:43-68`, `Reservation.java:112-130`); a mudança de situação fica em `if` espalhados pelo serviço
  (`ReservationService.java:57`, `:71`, `:84`, `:97-114`), sem a reserva proteger a própria transição.

Os tipos estão certos (`BigDecimal` com `HALF_EVEN` em `util/Money.java:10`, `LocalDate`, `enum`), mas o conjunto dos
defeitos de erro e de estado torna o código difícil de mudar com segurança.

## Testes

**Números:** 16 testes rodaram, 0 falharam, 0 pulados; 16 métodos escritos em 3 classes. Cobertura de linhas 60 de 842
(7,1%) e de ramos 32 de 170 (18,8%); `http`, `repository`, `service` e `config` com 0% (`measures/tests.md`). Nenhum
teste pela API nem contra o banco.
**Nota:** ruim

O que existe confere valores: o preço de julho (`src/test/java/com/pousada/marealta/util/PricingCalculatorTest.java:13-18`,
299,88), a soma (`:29-33`, 599,76), o desconto meio-para-o-par (`:36-40`, 89,96) e o validador de e-mail com os casos
do texto (`util/EmailValidatorTest.java:96-135`). Mas é quase só isso:

- nenhuma das regras de envio do cenário além da validação tem teste: a cópia para o arquivo, o "[Cópia] ", o desvio
  do modo de teste, o "[TESTE] " e o "Destinatário original" não aparecem em nenhum teste. Se alguém apagasse a
  linha `AppConfig.isTestMode()` de `ArchiveCopyRule.java:17`, ou trocasse o prefixo de `TestModeRedirectRule.java:8`,
  nada falharia;
- a temporada só é testada em julho e maio (`PricingCalculatorTest.java:14`, `:22`); por isso o erro de dezembro
  inteiro (`PricingCalculator.java:41-43`) passou sem ser notado;
- as situações da reserva, o cancelamento e a devolução, os códigos de erro e os relatórios não têm teste;
- nenhum teste sobe a aplicação: o defeito que derruba toda a API (falta o mapeador JSON) passou pelo `mvn verify`.

Com 7% das linhas cobertas e quase toda a parte nova sem conferência, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 6 colunas; datas em `date`. Acusado: a chave estrangeira `reservations(room_number)` sem índice; nenhum `CHECK`
nas 5 colunas de situação, tipo, plano e canal; nenhuma restrição `UNIQUE`/`EXCLUDE`; nenhum pool, nenhuma transação,
nenhum `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

O serviço sobe contra um banco vazio e cria o esquema, com tipos certos e SQL sempre com parâmetros
(`ReservationRepository.java:29-42`, nenhum candidato de SQL montado com `+`). Os defeitos:

- o esquema é criado por `CREATE TABLE IF NOT EXISTS` dentro do código (`config/DatabaseConfig.java:42-89`), sem
  migração: o próximo pedido que acrescentar uma coluna não terá como evoluir um banco que já existe;
- nada impede duas reservas do mesmo quarto na mesma noite: o serviço consulta conflitos e depois insere, em duas
  conexões separadas, sem transação, trava nem restrição (`service/ReservationService.java:36-50`). Pior: a consulta
  de conflitos devolve lista vazia se o banco der erro (`ReservationRepository.java:118-121`), o que deixa passar a
  reserva;
- uma conexão nova a cada chamada, por `DriverManager`, sem pool (`DatabaseConfig.java:34-40`);
- a carga inicial dos quartos engole o erro (`RoomRepository.java:29-31`) e faz `executeBatch` a cada linha
  (`:89-90`), fora de transação;
- a confirmação grava a reserva e as mensagens em operações separadas, sem transação
  (`ReservationHandler.java:80-84`).

Os dados ficam no banco (o roteiro confirma que as mensagens continuam depois de reiniciar), e nada do que a coluna
chama de `ruim` (dinheiro em ponto flutuante, SQL montado, tabela sem chave) aparece.

## Comportamento

**Números:** 6 de 65 conferências passaram: parte comum 4 de 46; depois de reiniciar 1 de 3; cenário no modo normal
0 de 9; cenário no modo de teste 0 de 5; reservas simultâneas 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

O sistema não funciona para o dono da pousada. Todo endpoint que lê ou escreve JSON responde 500, porque o Javalin
não tem mapeador JSON (sem `jackson-databind` no `pom.xml:20-61`, sem configurar o Gson), e o log diz isso
(`measures/behavior-service.log:486`). Por isso a listagem de quartos, a criação de reserva e todas as recusas (400,
404, 409, 422) saem como 500; sem reserva criada, a confirmação, o e-mail, o check-in, o check-out, os cancelamentos e
os relatórios com dados também falham; e nenhuma das 14 conferências das regras de envio (bloqueio, cópia, modo de
teste) pôde passar. As 6 que passaram não provam regra: os cabeçalhos dos CSV, um `refund null` lido de um corpo de
erro, as mensagens "continuarem" numa lista vazia e "nenhuma rodada com duas reservas" (as duas deram 500).

Mesmo com o JSON consertado, a leitura mostra falhas que o roteiro pegaria:

- a confirmação exige um corpo com `amount` (`ReservationHandler.java:76-79`); o texto diz que o pago é o total da
  reserva, e o site chama sem esse campo;
- dinheiro no JSON como texto (`"599.76"`), não como número (`ReservationHandler.java:160-166`);
- a segunda via de reserva não paga responde 404 sem corpo (`ReservationHandler.java:135-139`), e não 409
  `OPERATION_NOT_ALLOWED`; data ou mês fora do formato responde 400 sem o `{ "error": "INVALID_REQUEST" }`
  (`ReportHandler.java:23`, `:33`, `:51`);
- zero hóspede vira 422 `CAPACITY_EXCEEDED` em vez de 400 (`ReservationService.java:28-30`);
- reservas de 1º a 14 de dezembro e estadias que atravessam a troca de temporada saem com o preço errado
  (`PricingCalculator.java:41-43`, `ReservationService.java:42`).

## Dúvidas

- A coluna do comportamento não pôde medir as regras de envio, porque a API cai antes. A leitura sugere que,
  consertado o JSON, o bloqueio, a cópia e o desvio sairiam quase certos, com uma diferença possível: o modo de teste
  põe uma linha em branco depois de "Destinatário original: …" (`TestModeRedirectRule.java:20`), e o texto fala só de
  "uma primeira linha a mais". Isso não foi confirmado por chamada.
- Entre `malfeito` e `alternativa equivalente`: a lista de regras é uma estrutura que a referência aceita, mas a
  combinação por modo está dentro das regras (`ArchiveCopyRule.java:17`, `TestModeRedirectRule.java:13`), que é o
  critério explícito de `malfeito`; fiquei com `malfeito`.
