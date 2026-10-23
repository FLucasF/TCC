# Revisão: construção, cenário 07

## Visão geral

O agente montou um projeto Maven com Javalin 6, HikariCP e JDBC puro, em 10 pacotes. Tudo é ligado em `Application.main` (`Application.java:29-61`). O caminho de uma requisição é `ApiController` (rotas e validação do corpo), depois `ReservationService` (situações da reserva, preço via `PricingService`), depois os repositórios `Postgres*Repository` (SQL com `?`). Os quartos ficam fixos em memória (`InMemoryRoomRepository.java:13-19`), e as reservas e as mensagens ficam no PostgreSQL, em duas tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`DatabaseInitializer.java:14,35`). As regras de envio são uma lista de `MessageProcessor` que o `MessageService` monta por dentro.

O mais sério:
- **Reserva dupla:** nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Nas 10 rodadas simultâneas do roteiro, as duas reservas foram criadas todas as vezes.
- **Bloqueio de e-mail incompleto:** a regra de bloqueio não para a corrente. Uma mensagem bloqueada ainda gera cópia para o arquivo e, no modo de teste, perde o endereço original.
- **`mvn verify` não passa:** 10 das 18 execuções de teste falham, e o texto pedia que ele passasse.

## Arquitetura

**Números:** 10 pacotes, 23 dependências entre eles, 1 ciclo (`messaging` ↔ `messaging.rules`). O Javalin só aparece em `controller` e na raiz, e o JDBC só em `infrastructure` e na raiz (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem e apontam no sentido certo. As rotas chamam o serviço e escrevem a resposta (`ApiController.java:113-149`). As regras (`service`, `messaging`) não conhecem nem Javalin nem JDBC. O SQL está todo em `infrastructure/Postgres*Repository`. A montagem fica num lugar só (`Application.java:36-48`).

Os defeitos:
- **Montagem fora do `main`:** o `MessageService` cria as próprias regras por dentro (`MessageService.java:24-29`, com `new EmailValidationRule()` e `new ArchiveCopyRule()`). É isso que causa o ciclo: o pacote `rules` implementa uma interface de `messaging`, e `messaging` instancia as classes de `rules`. Também é por isso que a combinação de regras não pode ser decidida em `Application`.
- **Validação de negócio no controlador:** a saída depois da entrada e o mínimo de um hóspede ficam no handler (`ApiController.java:77-86`). O handler monta a entidade campo a campo e embrulha tudo num `catch (Exception)` (`ApiController.java:97-102`).
- **Repositório direto no controlador:** `GET /rooms` vai ao repositório sem passar pelo serviço (`ApiController.java:54`).
- **Montagem copiada nos testes:** os dois testes repetem a montagem inteira do `main` (`ReservationSystemIT.java:47-70`, `TestModeIT.java`), porque não há um método de montagem que se possa reaproveitar.

Nenhum desses defeitos é grave sozinho, mas o ciclo e a montagem escondida dentro do `MessageService` ficam exatamente no ponto que o texto diz que vai crescer.

## Design de baixo nível

**Números:** 3 regras pedidas (validação, cópia, modo de teste), as 3 em classes próprias atrás de `MessageProcessor`. A decisão por modo está escrita dentro de 2 das 3 (`ArchiveCopyRule.java:15`, `TestModeRule.java:15`), e a lista é fixa dentro do `MessageService` (`MessageService.java:24-29`), não na montagem.
**Classe:** malfeito
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O que aproxima de uma solução aceitável: cada regra é uma peça própria com a mesma interface (`MessageProcessor.java:6-8`), aplicada em sequência pelo envio (`MessageService.java:32-54`). Isso seria a "lista de filtros" que a referência aceita como alternativa equivalente.

O que afasta: a referência exige que a combinação por modo seja decidida na montagem, e aqui ela não é.
- **A combinação fica dentro das regras:** `ArchiveCopyRule` pergunta `context.isTestMode()` para não copiar (`ArchiveCopyRule.java:15`), que é literalmente o exemplo de malfeito da referência. `TestModeRule` também pergunta o modo por dentro (`TestModeRule.java:15`), em vez de só entrar na lista quando o modo de teste está ligado.
- **A lista não sai da montagem:** ela é fixa e criada dentro do serviço (`MessageService.java:24-29`). A "segunda pousada que não quer a cópia", e as regras de madrugada ou de limite que valem só num dos modos, obrigam a editar o `MessageService` ou as regras.
- **O bloqueio não para a corrente:** o `break` em `MessageService.java:46` sai só do laço interno. O laço externo (`MessageService.java:36`) segue e entrega a mensagem bloqueada ao `TestModeRule`, que troca o destinatário, e ao `ArchiveCopyRule`, que cria a cópia. O comportamento confirma isso (5 falhas do cenário).

**O exagero.** Há três interfaces de repositório com uma implementação cada (`domain/repository/*.java`), que nada substitui: nem os testes, que usam as classes do Postgres. Há também o `ProcessingContext` (`ProcessingContext.java:3-13`), que só embrulha um `boolean`. É estrutura sem uso, mas pequena.

**O resto do código:**
- **Causa escondida:** o `catch (Exception)` em `ApiController.java:97-102` transforma qualquer erro, até falha de SQL, em 400 `INVALID_REQUEST`. Os próprios testes mostram o efeito: sem as tabelas, a criação devolve 400 em vez de 500.
- **Formatador compartilhado entre threads:** `NumberFormat` estático (`DocumentService.java:16`) não é seguro com várias threads, e as requisições do Javalin chegam em threads diferentes. Ele também gera `R$` seguido de espaço não separável (U+00A0), não o espaço comum do texto.
- **Erro 500 para id inválido:** `Long.parseLong` sem tratamento (`ApiController.java:115-146`) responde 500 a `/reservations/abc`.
- **Variável sem uso:** em `ApiController.java:140`.
- **Exceção genérica:** `RuntimeException` sem código em `DocumentService.java:31`.
- **Entidade solta:** `Reservation` é um bag de setters públicos e muda sem controle. As transições ficam nos `canX()` (`Reservation.java:157-177`), mas qualquer um pode chamar `setStatus`.

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`PricingService.java:52-54`), `LocalDate` e `enum`s.

## Testes

**Números:** 18 execuções, 10 falhas, 0 pulados. São 15 métodos escritos em 2 classes, só testes pela API contra o banco, sem nenhum teste de unidade. A cobertura não foi medida: o relatório do JaCoCo falhou com `Unknown block type 7`, depois que o Surefire precisou matar o fork da JVM (`measures/tests.md`, `measures/tests-maven.log:710`). O `mvn verify` sai com erro.
**Nota:** ruim

O texto pedia que o `mvn verify` passasse, e ele não passa. Há duas causas:
- **Defeito do próprio teste (8 falhas):** em `ReservationSystemIT`, o `@BeforeEach` só apaga as tabelas (`ReservationSystemIT.java:81-84`, que chama `cleanDatabase` em `ReservationSystemIT.java:98-105`) e não as cria de novo. Do segundo teste em diante, tudo responde 400. Só os dois testes de relatório passam, porque recriam as tabelas por conta própria (`ReservationSystemIT.java:458-459`, `515-516`).
- **Defeito real do código (2 falhas):** `TestModeIT.testModeStillBlocksInvalidEmails` (`TestModeIT.java:226`) falha porque a mensagem bloqueada sai com `testes@...`. O teste roda duas vezes, porque o nome `TestModeIT` casa com o padrão `Test*` do Surefire e também com o do Failsafe.

O agente entregou o projeto sem ver os próprios testes passarem.

O que existe confere valores: o preço na baixa e na alta temporada e o desconto (`ReservationSystemIT.java:146-155` e seguintes), a cópia com "[Cópia] " (`ReservationSystemIT.java:322-329`), o redirecionamento no modo de teste (`TestModeIT.java:144-149`) e os relatórios com as linhas exatas (`ReservationSystemIT.java:503-506`).

O que ficaria sem rede numa mutação:
- **Validação de e-mail:** só `ana@gmail` e `invalid.email` são testados. Trocar `atIndex != lastIndexOf('@')` ou aceitar `@gmail.com` não derrubaria nada.
- **Limites da temporada:** 14 e 15 de dezembro e o fim de fevereiro não têm teste.
- **Arredondamento "meio para o par":** nenhum caso no meio exato.
- **Cancelamento NON_REFUNDABLE e de reserva pendente:** sem teste.
- **Ordem dos filtros e cópia de mensagem bloqueada fora do modo de teste:** o teste que pegaria isso (`ReservationSystemIT.java:367`) nunca chega a rodar, por causa da falha de ambiente.

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), as duas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há nenhuma chave estrangeira, nenhum CHECK e nenhum UNIQUE/EXCLUDE. Não há transação explícita nem `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

Os tipos estão certos, o SQL usa parâmetros (por exemplo, `PostgresReservationRepository.java:122-148`), as conexões voltam ao pool por try-with-resources e os dados sobrevivem ao reinício (3 de 3 no comportamento).

O defeito sério é a regra "um quarto não pode ter duas reservas na mesma noite". Ela é só um "consulta e depois insere" sem trava e sem transação: `findConflicting` em `ReservationService.java:42` e depois `save` em `ReservationService.java:68`. Não há nada no banco que a proteja, nem uma restrição `EXCLUDE` (`DatabaseInitializer.java:14-31`). O resultado foi medido: nas 10 rodadas simultâneas do roteiro, as duas reservas foram criadas todas as vezes. Isso é dado errado, a pousada vendendo o mesmo quarto duas vezes.

Os outros defeitos:
- **Esquema sem evolução:** `CREATE TABLE IF NOT EXISTS` no código (`DatabaseInitializer.java:14,35`), sem migrações. O próximo pedido que acrescentar uma coluna não terá como alterar um banco que já existe.
- **Nenhuma restrição:** sem CHECK em `status`, `rate_plan` ou `guests`, e `room_number` sem ligação com quarto nenhum. Os quartos ficam só em memória, o que o texto até tolera ("quase nunca mudam").
- **Confirmação sem transação:** a reserva é gravada (`ReservationService.java:87`) e as mensagens depois, em conexões separadas.

## Comportamento

**Números:** 55 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 42 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário, modo normal | 5 de 9 |
| cenário, modo de teste | 4 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

**Parte comum.** As 4 falhas:
- **O número do quarto como texto (2 falhas):** o 201 vem como `"201"` (texto). A conferência procura o número 201. O texto não diz o tipo do `number`, então essa escolha não pesa. Conferi à mão: capacidade 3 e diária 389.90 estão certas.
- **`amountPaid` sem as 2 casas:** na criação, ele vem como `0` em vez de `0.00`, porque o serviço devolve o `BigDecimal.ZERO` de `ReservationService.java:66` sem reler do banco. O texto pede 2 casas, então é erro pequeno.
- **O comprovante no e-mail:** ele não traz "Total: R$ 599,76" exatamente, porque o `NumberFormat` põe um espaço não separável depois do "R$" (conferido com `od`: bytes `302 240`). O hóspede vê algo parecido, mas um sistema que compare o texto não vê o comprovante pedido.

Fora do roteiro, chamando a API à mão:
- Uma STANDARD confirmada e cancelada aparece no financeiro com `0.00`. O filtro `amount_paid > 0` (`PostgresReservationRepository.java:185`) não olha a devolução, e o texto diz que entram só as reservas que "deixaram dinheiro".
- Um mês sem reservas dá `total,0` em vez de `total,0.00`.

**Cenário 07.** Esta é a parte que o dono disse precisar desde o primeiro dia:
- **O que funciona:** o envio válido com cópia logo depois, com "[Cópia] " e o mesmo texto, e o redirecionamento do modo de teste com "[TESTE] " e "Destinatário original: ".
- **O que falha, no modo normal (4 falhas):** os quatro endereços inválidos (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) são bloqueados, mas geram uma cópia para o arquivo, e o texto diz que mensagem bloqueada não gera cópia.
- **O que falha, no modo de teste (1 falha):** a mensagem bloqueada aparece com `testes@pousadamarealta.com.br` em vez do endereço digitado errado. Era justamente o endereço que a recepção precisava ver para ligar para o hóspede.

As duas falhas vêm do `break` que não interrompe a corrente (`MessageService.java:46`).

**Reservas simultâneas.** Nas 10 rodadas, as duas reservas foram criadas todas as vezes: a pousada pode vender o mesmo quarto duas vezes. O defeito pesa na coluna do banco.

A parte comum funciona e a maior parte do cenário também, mas a regra de bloqueio está errada nos dois modos.

## Dúvidas

- **Exagero:** marquei `exagero` pelas três interfaces de repositório com uma implementação só e pelo `ProcessingContext`, mas é estrutura pequena e comum. Um revisor menos estrito poderia dar `contenção`.
- **Classe:** entre `malfeito` e `alternativa equivalente`, a evidência fica com `malfeito`. A referência cita como malfeito a cópia que pergunta pelo modo de teste, e é o que `ArchiveCopyRule.java:15` faz.
- **Cobertura:** não houve número de cobertura, porque o relatório do JaCoCo falhou com o arquivo `jacoco.exec` corrompido. Não deu para saber quanto do código os testes que passam executam.
