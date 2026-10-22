# Revisão: construção, cenário 05

## Visão geral

O projeto tem oito pacotes (`controller`, `routes`, `service`, `repository`, `database`, `model`, `dto` e a raiz com o
`Main`). Uma requisição entra pelo Javalin (`routes/Routes.java:15-40`) e cai num método de controller que recebe o
`Context`. Para as reservas é o `ReservationController`, e ele faz quase tudo: valida, calcula o preço, decide a
situação da reserva e a devolução, grava pelo repositório e manda o e-mail. Os dados ficam no PostgreSQL, em três
tabelas que o `DatabaseSetup` cria com `CREATE TABLE IF NOT EXISTS`, e o acesso é por JDBC puro, com uma conexão nova
do `DriverManager` a cada chamada. A parte do cenário está bem resolvida: a escolha do documento (comprovante, fatura
ou booking receipt) fica num lugar só, e as 32 conferências do cenário passaram. Há quatro problemas mais sérios:
- **Reserva em dobro:** duas reservas do mesmo quarto feitas ao mesmo tempo passam as duas, em 10 de 10 rodadas. Nada
  no código nem no banco impede isso.
- **Regras no controller:** as regras da reserva moram dentro de handlers que recebem o `Context` do Javalin.
- **Testes:** o único teste pela API depende de Docker e não roda, e mesmo com Docker iria para o banco errado. O
  `mvn verify` puro falha neste ambiente.
- **Contrato da API:** a API devolve as datas como lista (`[2027,3,10]`) e omite os campos `null` em vez de mandá-los
  como `null`.

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O `ReservationController`
tem 315 linhas e é a maior classe; o `ReservationRepository` vem depois, com 252. O Javalin aparece só em `controller`,
`routes` e na raiz, e o JDBC só em `database` e `repository`.
**Nota:** ruim

O SQL está isolado nos repositórios, a montagem fica num lugar (`routes/Routes.java:16-25`) e não há ciclo. Mas a regra
de negócio da reserva não tem camada própria: ela vive dentro dos handlers do Javalin.
- **O preço:** o cálculo da reserva (noites, soma, desconto de 15%, total) está em
  `controller/ReservationController.java:288-314`. Só o preço de uma noite foi para o `PricingService`.
- **A máquina de estados:** as situações da reserva estão espalhadas pelos handlers
  (`controller/ReservationController.java:122`, `:151-152`, `:183`, `:207`, `:234-235`).
- **A devolução:** a regra de devolução no cancelamento está em `controller/ReservationController.java:159-163`.
- **A disponibilidade e a gravação:** a conferência de disponibilidade e a gravação ficam no handler
  (`controller/ReservationController.java:63`, `:87`), sem um serviço entre eles.

Nenhuma dessas regras pode ser usada nem testada sem um `Context` do Javalin. O controller também fala direto com três
repositórios e cria o `EmailService` por dentro (`controller/ReservationController.java:27`), fora da montagem. O
`Database` é um acesso global estático (`database/Database.java:8-13`). As partes laterais estão separadas direito
(`ReportService`, `DocumentService`, `EmailService` não conhecem Javalin nem JDBC). O que não tem camada é justamente o
núcleo do sistema, a reserva, e o próximo pedido sobre ela vai mexer no handler.

## Design de baixo nível

**Números:** 3 tipos de documento, todos criados por um único método de escolha
(`service/DocumentService.java:21-51`). Os 3 lugares que emitem o documento chamam esse método (confirmação em
`controller/ReservationController.java:132`, check-out em `:216`, segunda via em `:241`). Nenhum lugar monta o documento
à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada: `DocumentService.generateDocument` preenche os dados comuns e decide pelo
tipo de hóspede qual título e qual texto produzir (`service/DocumentService.java:39-48`). Cada texto é gerado num
método próprio (`:53`, `:72`, `:95`). Os três lugares só pedem o documento e não perguntam qual é o caso, e o e-mail
recebe o documento pronto e só embute o `text` (`service/EmailService.java:15-27`). Uma agência de turismo entraria com
um ramo a mais e um gerador a mais, num lugar só. Há dois pontos que afastam a fábrica de um acerto limpo:
- **O tipo de hóspede é implícito.** A fábrica decide por campos nulos (`getGuestCpf() != null`...), e só funciona
  porque a validação, em outro arquivo, garante que exatamente um conjunto veio
  (`controller/ReservationController.java:260-286`). Um `enum`, ou um tipo de hóspede, deixaria isso explícito.
- **Não há `else`.** Se nenhum ramo casar, sai um documento sem título nem texto (`service/DocumentService.java:39-48`).

Não há interface, builder ou camada que só repassa: contenção.

**O resto do código.**
- **Duplicação.** O bloco "ler o id, buscar, 404 se não existe, 404 se o id não é número" aparece seis vezes
  (`controller/ReservationController.java:98-110`, `:114-139`, `:143-171`, `:175-195`, `:199-222`, `:226-247`).
  O comprovante e a fatura repetem as mesmas sete linhas de texto (`service/DocumentService.java:53-70` e `:72-93`).
  O mapeamento de `Room` está copiado em `repository/RoomRepository.java:24-28` e `:48-52`.
- **Os erros.** O `catch (Exception e)` de `controller/ReservationController.java:91` transforma qualquer falha,
  inclusive uma queda do banco, em `400 INVALID_REQUEST`, e esconde a causa.
- **O estado.** Os modelos são sacos de setters mutáveis (`model/Reservation.java`, `model/Document.java`), e a
  reserva muda de estado por `setStatus` solto no handler. Não há um método da própria reserva que guarde a regra da
  transição.
- **Os tipos e os valores fixos.**
  - O total de quartos está fixo em 5 no relatório (`service/ReportService.java:36`).
  - A variável `year` não é usada (`service/PricingService.java:11`).
  - O seed das diárias passa por `double` (`database/DatabaseSetup.java:81`). O `BigDecimal.valueOf` salva o valor,
    mas é um tipo errado para dinheiro.
  - Dinheiro é `BigDecimal` com `HALF_EVEN` (`service/PricingService.java:40`) e as datas são `LocalDate`. Esses estão
    certos.
- **O JSON.** O `@JsonInclude(NON_NULL)` em `model/Reservation.java:7` e `model/Document.java:7` contraria o texto, que
  pede os campos que não se aplicam como `null`.

## Testes

**Números:** 10 testes rodaram, com 1 erro (`PousadaIT`, "Could not find a valid Docker environment"); 29 métodos de
teste escritos (9 em `PricingServiceTest`, 20 em `PousadaIT`). Cobertura de 15 de 782 linhas (1,9%) e de 12 de 184
ramos (6,5%). Só o pacote `service` tem alguma cobertura (15 de 141 linhas) (`measures/tests.md`).
**Nota:** ruim

**O que roda.** Só os 9 testes de unidade do `PricingService`, e eles conferem com valores certos a alta temporada
(`src/test/java/br/com/marealte/service/PricingServiceTest.java:17-22`, com a borda 14/15 de dezembro), o acréscimo de
20% (`:48-56`, 299.88) e o arredondamento meio para o par (`:58-68`). Nada do resto roda em teste:
- **Sem teste nenhum:** o desconto de 15%, o cancelamento e a devolução, as situações da reserva, os relatórios e toda
  a parte do cenário (a fábrica, a fatura, o booking receipt, a recusa de identificação dupla ou incompleta).

**O teste pela API.** O `PousadaIT` é bom no conteúdo: confere os textos do comprovante, da fatura e do receipt
(`PousadaIT.java` perto das linhas 356-437), a devolução e os códigos de erro. Mas não roda por três motivos:
- **Precisa de Docker.** Ele sobe um Testcontainers (`src/test/java/br/com/marealte/PousadaIT.java:17-21`), e o texto
  não oferece Docker. Sem ele, um `mvn verify` puro falha, e o texto pede que ele passe. O script só chega a "BUILD
  SUCCESS" porque roda com `-Dmaven.test.failure.ignore`.
- **Iria para o banco errado.** Ele passa o endereço do container por `System.setProperty`
  (`PousadaIT.java:27-30`), mas o sistema lê `System.getenv` (`database/Database.java:8-10`). Mesmo com Docker, o
  servidor do teste não usaria o container.
- **Espera às cegas.** Ele espera o servidor com `Thread.sleep(3000)` (`PousadaIT.java:37`).

**A pergunta da mutação.**
- Trocar o ISS de `0.05` por `0.50` (`service/DocumentService.java:89`): nenhum teste falharia. Mesmo o IT só confere
  o rótulo "ISS retido (5%):" (`PousadaIT.java:412`), não o valor.
- Trocar o `else if` da fatura e o do receipt de lugar na fábrica: nenhum teste que roda falharia.
- Apagar a recusa de CNPJ sem razão social (`controller/ReservationController.java:275-278`): nenhum teste falharia.
  O IT não tem esse caso.

## Banco

**Números:**
- **Tabelas e tipos:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Os seis campos de
  dinheiro são `numeric(10,2)` e as datas são `date`.
- **Conferências que acusaram:** uma chave estrangeira sem índice (`reservations(room_number) -> rooms`). As colunas
  de situação, plano, tipo e canal não têm CHECK. Não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK.
- **O código:** não há pool, 1 `DriverManager.getConnection`, 0 transações explícitas, 0 `FOR UPDATE` e nenhum SQL
  montado com `+` (`measures/database.md`).
**Nota:** com problemas

Os tipos estão certos, a reserva liga ao quarto por chave estrangeira
(`database/DatabaseSetup.java:24`), todo SQL usa `?` e as conexões são fechadas com try-with-resources. Os defeitos:
- **Reserva em dobro.** Nada protege a regra de que um quarto não tem duas reservas na mesma noite. A conferência de
  disponibilidade é um `SELECT COUNT(*)` (`repository/ReservationRepository.java:107-141`) seguido de um `INSERT`
  noutra conexão (`:16-59`). Os dois estão em chamadas separadas do handler (`controller/ReservationController.java:63`
  e `:87`), sem transação, sem trava e sem `EXCLUDE`. O roteiro do comportamento mostrou as duas reservas criadas em
  10 de 10 rodadas simultâneas. É o defeito mais sério da coluna, e ele grava dado errado (dois hóspedes no mesmo
  quarto).
- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS`
  (`database/DatabaseSetup.java:11-54`), sem migração. O próximo pedido que acrescentar uma coluna não vai chegar a um
  banco que já existe.
- **Sem pool.** Cada chamada abre uma conexão nova (`database/Database.java:12-13`).
- **Sem transação na confirmação.** A mudança de situação (`controller/ReservationController.java:129`) e a mensagem
  gravada (`service/EmailService.java:26`) não andam juntas.
- **Sem restrições de domínio.** Faltam um CHECK em `status` e `rate_plan` e um CHECK de `guests >= 1` e de
  `check_out > check_in`.

## Comportamento

**Números:** 79 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 43 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 05 | 32 de 32 |
| as reservas simultâneas | 1 de 2 |

**Nota:** com problemas

**O cenário funciona por inteiro:**
- a fatura com a empresa e o ISS de R$ 38,99;
- o booking receipt em inglês, com `BRL 2,783.52`, as datas `2027-03-10` e "1 night";
- o e-mail em português levando o documento certo;
- as recusas de nenhuma, duas ou uma identificação incompleta.

O que falhou:
- **Reserva em dobro.** Em 10 de 10 rodadas, dois pedidos simultâneos para o mesmo quarto e as mesmas noites foram os
  dois criados. Para o dono, são dois hóspedes vendidos para o mesmo quarto quando o site recebe dois pedidos juntos.
  A causa está no banco (veja acima).
- **Dinheiro sem as duas casas.** Na criação, o `amountPaid` e o `discount` saem como `0`, não `0.00`, porque são o
  `BigDecimal.ZERO` sem escala (`controller/ReservationController.java:85`, `:304`). O texto pede as 2 casas. O defeito
  é pequeno, e depois da releitura do banco os valores saem certos.
- **O número do quarto como texto.** "O 201 comporta 3" e "a diária do 201 é 389.90" falharam só porque o `number`
  do quarto sai como texto (`"201"`). Os valores estão certos (chamei `GET /rooms` à mão: `"capacity":3`,
  `"nightlyRate":389.90`). O texto não fixa o tipo do número do quarto, então essa é uma escolha razoável e não pesa.

Achei mais dois pontos chamando a API à mão, fora do roteiro:
- **As datas como lista.** Na reserva e no documento, as datas saem como `"checkIn":[2027,3,10]`, não como
  `"2027-03-10"`, que é o formato que o texto combina com o site. O site vai receber as datas num formato que não
  espera.
- **Os campos `null` omitidos.** Os campos de identificação que não se aplicam, e o `refund` antes do cancelamento,
  não vêm como `null`: são omitidos. É o `NON_NULL` de `model/Reservation.java:7` e `model/Document.java:7`. O roteiro
  aceita a ausência, mas o texto pede `null`.

## Dúvidas

- **A nota da arquitetura.** Ficou entre `ruim` e `com problemas`. O SQL está isolado, não há ciclo e as partes
  laterais (documento, e-mail, relatórios) estão fora do Javalin. O núcleo, porém (preço da reserva, situações,
  devolução, disponibilidade), só existe dentro de handlers que recebem o `Context`. Dei `ruim` pelo critério "as
  regras dependem do Javalin".
- **A nota do banco.** A reserva em dobro, sem proteção nenhuma (nem dentro do processo), é um risco de dado errado
  que poderia justificar `ruim`. Dei `com problemas` porque os critérios da coluna para `ruim` (dinheiro em ponto
  flutuante, SQL montado, tabela sem chave, dado perdido) não se aplicam.
- **A forma do pattern.** Foi uma fábrica concentrada num método estático com uma cadeia de `if` sobre campos nulos,
  não a forma clássica com criadores sobrescritos. A referência aceita essa forma como acerto.
- **As datas como lista.** Não sei se o site do dono quebraria com elas: o texto só diz o formato das datas no corpo
  da criação. Contei como defeito de contrato, sem mudar a nota do comportamento.
