# Revisão: construção, cenário 01

## Visão geral

O projeto tem cinco pacotes. Em `com.pousada.api` ficam os handlers do Javalin, registrados por métodos `static register`. `com.pousada.service` junta as regras (`Validator`, `RatePlanCalculator`, `ReceiptService`) e o acesso ao banco: `ReservationService`, `RoomService` e `MessageService` são, na prática, classes de SQL. `com.pousada.db` tem os modelos com campos públicos e o `Database`, que cria o esquema com `CREATE TABLE IF NOT EXISTS` e guarda um pool Hikari estático. `com.pousada.util` tem dinheiro, datas e temporada. Uma requisição entra no handler (`ReservationAPI`). Ele valida, decide a situação da reserva e a devolução, e chama os métodos estáticos que gravam no PostgreSQL.

O defeito mais sério é que o sistema não serializa `LocalDate`. O `pom.xml:23-49` não traz o `jackson-datatype-jsr310`, e `Reservation.checkIn`/`checkOut` (`src/main/java/com/pousada/db/Reservation.java:24,27`) são `LocalDate`. Por isso toda resposta que devolve uma reserva ou um comprovante quebra depois de gravar:

- `POST /reservations` grava a reserva e responde 400 `INVALID_REQUEST`, porque o `catch (Exception)` em `ReservationAPI.java:83-86` esconde a exceção do Jackson. Reproduzi à mão: veio 400 e a linha PENDING ficou no banco.
- `GET /reservations/{id}`, a confirmação, o check-in, o check-out e a segunda via respondem 500. A confirmação grava CONFIRMED e manda o e-mail mesmo assim.

O site nunca recebe o id da reserva que criou. Além disso:

- o pacote de fim de semana é impossível de reservar (`Validator.java:71` exige saída na segunda);
- duas reservas simultâneas do mesmo quarto foram gravadas em 7 de 10 rodadas;
- não há nenhum teste.

## Arquitetura

**Números:** 5 pacotes (`com.pousada` 22 linhas, `api` 483, `db` 300, `service` 594, `util` 79), 7 dependências entre eles, nenhum ciclo. O JDBC aparece em `api`, `db` e `service`; o Javalin só em `com.pousada` e `api`. A maior classe é `ReservationAPI`, com 291 linhas.
**Nota:** com problemas

- **O que está certo:**
  - As dependências apontam da borda para as regras e para os dados, sem ciclo.
  - As regras de preço e de validação (`RatePlanCalculator`, `Validator`, `PriceCalculator`, `SeasonUtils`) não conhecem nem o Javalin nem o JDBC.
  - O SQL está concentrado nas três classes `*Service` com JDBC.
- **A máquina de estados e a devolução moram nos handlers:**
  - só confirma PENDING: `ReservationAPI.java:121`;
  - cancelamento: `ReservationAPI.java:157`;
  - check-in: `ReservationAPI.java:188`;
  - check-out: `ReservationAPI.java:217`;
  - quem tem segunda via: `ReservationAPI.java:247`;
  - o handler decide a devolução (`ReservationAPI.java:163`) e orquestra confirmar, montar o comprovante e mandar o e-mail (`ReservationAPI.java:127-133`).

  Não existe um serviço de reserva com essas regras: o `ReservationService` é só SQL (`ReservationService.java:14-163`). A mesma regra de "reserva paga" aparece de novo, como lista de situações, no SQL (`ReservationService.java:122,146`).
- **Não há montagem.** Tudo é `static`: o pool fica em `Database.java:9`, os serviços são métodos estáticos e o cache de quartos é um `static HashMap` (`RoomService.java:15-16`). `Application.java:12-20` só chama `register` estáticos, então nada pode ser trocado nem testado isoladamente.
- **A falha de leitura da requisição vira 400 genérico.** `ReservationAPI.java:83` pega qualquer `Exception` na borda, inclusive erro do banco. É isso que mascara o defeito de serialização.

As camadas existem e não há ciclo, mas as regras da situação da reserva estão na borda HTTP e a montagem é toda estática.

## Design de baixo nível

**Números:** 5 planos. Nenhum tem um objeto próprio. O comportamento de plano está em 3 `switch`/cadeias de `if` sobre o texto do plano:
- o preço: `RatePlanCalculator.java:26-33`;
- a devolução: `RatePlanCalculator.java:131-137`;
- a validação: `Validator.java:40-61`, mais a lista de planos válidos em `Validator.java:83-88`.

**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto avisa que "o comercial vive inventando plano", cada um com preço, regra de uso e devolução próprios. O código não tem nenhuma estrutura de plano: o `ratePlan` é uma `String`, e cada lugar pergunta qual é o plano.

- `RatePlanCalculator.calculate` escolhe por `switch` um método privado por plano (`RatePlanCalculator.java:27-31`).
- `getRefundAmount` repete o `switch` para a devolução (`RatePlanCalculator.java:132-136`).
- As regras de quem pode usar ficam em `if ("CORPORATE"...)`, `if ("WEEKEND_PACKAGE"...)` e `if ("LONG_STAY"...)` no `Validator` (`Validator.java:40,49,56`).
- O plano novo para a melhor idade teria de mexer em pelo menos quatro lugares: os dois `switch`, a lista de `isValidRatePlan` e um novo `if`.

Isso é a definição de omissão da referência.

**O exagero.** Não há interface, fábrica ou camada que só repassa: contenção.

**O resto do código:**

- **Duplicação:**
  - O laço que soma as noites com a temporada está copiado quatro vezes: `RatePlanCalculator.java:40-46`, `55-61`, `74-80` e `99-105`.
  - A lista de empresas existe em dois lugares e eles divergem: `Validator.java:91` aceita minúsculas com `toUpperCase()`, mas `RatePlanCalculator.java:114` não. Um `tecnosul` passa na validação e só é recusado por uma `IllegalArgumentException` que o `catch` genérico transforma em 400.
  - A classe `ErrorResponse` está copiada em quatro handlers: `MessageAPI.java:20`, `ReportAPI.java:124`, `ReservationAPI.java:282` e `RoomAPI.java:21`.
  - O mesmo bloco de 404/500 se repete em sete handlers (`ReservationAPI.java:89-262`).
- **Tipos:**
  - A situação da reserva e o plano são `String` soltas (`ReservationAPI.java:121,157,188,217,247`; `ReservationService.java:41,92`).
  - O preço do pacote é escolhido pelo número do quarto, não pelo tipo (`RatePlanCalculator.java:122-128`). Um quarto novo quebra o pacote.
  - O dinheiro usa `BigDecimal` com `HALF_EVEN` em cada passo (`PriceCalculator.java:8-31`), o que está certo.
- **Erros:**
  - O `catch (Exception)` em `ReservationAPI.java:83` esconde a causa real (a serialização de `LocalDate`) e devolve 400 para um erro do servidor.
  - O resto dos handlers só trata `SQLException`, então o erro do Jackson sai como 500 sem log útil.
- **Estado:**
  - Os campos públicos e mutáveis dos modelos (`Reservation.java:8-60`, `Room.java:7-17`) são o próprio JSON de resposta.
  - O cache estático `HashMap` com `boolean initialized` não é seguro entre threads (`RoomService.java:15-31`).
- **Formatação dependente do locale da JVM:**
  - `String.format("%.2f")` em `ReceiptService.java:60` e `ReportAPI.java:109,114`.
  - Com locale pt-BR, o CSV sairia com vírgula e o `split("\\.")` de `ReceiptService.java:61-70` estouraria o índice.

Os defeitos se espalham por todas as classes de regra e de borda. Somados à variação de planos sem estrutura, deixam o código difícil de mudar com segurança.

## Testes

**Números:** 0 testes rodaram (0 falhas), 0 métodos de teste escritos, nenhum arquivo em `src/test` e nenhum relatório do JaCoCo, portanto sem cobertura de linhas nem de ramos (`measures/tests.md`). O `mvn verify` passa (BUILD SUCCESS em `measures/tests-maven.log`), mas só porque não há o que verificar.
**Nota:** ruim

Não há teste. A única verificação é o `test-api.sh`, um roteiro de `curl | jq` que imprime as respostas sem conferir nada e que não roda no build.

Nenhuma regra tem rede:
- a temporada;
- o arredondamento meio para o par;
- os 12/18/25% do convênio;
- a sexta-domingo do pacote, que está errada (`Validator.java:71`) e qualquer teste teria pego;
- as 7 noites;
- as devoluções.

Os dois defeitos que derrubam o sistema (a serialização de `LocalDate` e o pacote impossível) seriam pegos pelo primeiro teste pela API.

## Banco

**Números:**
- 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária;
- dinheiro em `numeric(10,2)` (6 colunas, todas ok);
- datas em `date`;
- 0 chaves estrangeiras;
- 0 CHECK;
- a única restrição de regra é `UNIQUE (number)` em `rooms`;
- 0 transações explícitas e 0 `FOR UPDATE`;
- pool Hikari;
- nenhum SQL montado com `+` (`measures/database.md`).

**Nota:** ruim

- **O que está certo:** os tipos, a chave primária, o SQL com parâmetros `?` e as conexões em try-with-resources com pool (`ReservationService.java:19,53,...`).
- **Nada impede duas reservas no mesmo quarto na mesma noite.**
  - O handler consulta a disponibilidade (`ReservationService.java:99-116`) e depois insere em outra conexão (`ReservationAPI.java:68,77`), sem transação, sem trava e sem restrição `EXCLUDE`.
  - O roteiro do comportamento deixou a prova no banco `review_behavior_2`: 7 pares de reservas não canceladas do quarto 301 nas mesmas noites (ids 8/9, 10/11, 12/13, 14/15, 16/17, 19/20, 23/24), um por rodada em 7 das 10 rodadas.
  - É dado errado gravado de verdade, e ainda derruba o relatório de ocupação: `Collectors.toMap` em `ReportAPI.java:44-45` lança `IllegalStateException` com duas reservas pagas no mesmo quarto.
- **Sem chave estrangeira.** `reservations.room_number` não referencia `rooms` (`Database.java:44`).
- **Sem CHECK** na situação, no plano, em `guests >= 1` ou em `check_out > check_in` (`Database.java:42-61`).
- **O esquema é criado no código** com `CREATE TABLE IF NOT EXISTS` (`Database.java:31-77`), sem migrações. Não sabe acrescentar uma coluna numa tabela que já existe.
- **Escritas que deveriam andar juntas vão separadas.**
  - A confirmação grava CONFIRMED e depois insere a mensagem em outra conexão (`ReservationAPI.java:127,133`).
  - A checagem de situação e a atualização não são atômicas (`ReservationAPI.java:121-127`): duas confirmações simultâneas mandam dois e-mails.

## Comportamento

**Números:** 25 de 72 conferências passaram:
- a parte comum: 17 de 46;
- depois de reiniciar: 2 de 3;
- o cenário 01: 5 de 21;
- as reservas simultâneas: 1 de 2.

**Nota:** ruim

- **A parte comum quebra.**
  - Criar uma reserva válida responde 400 `INVALID_REQUEST` "Invalid request body", mas a reserva fica gravada.
  - Reproduzi à mão: `POST /reservations` deu 400. A linha PENDING com total 599.76 estava no banco. `GET /reservations/1` deu 500 com `InvalidDefinitionException: Java 8 date/time type java.time.LocalDate not supported`.
  - A confirmação deu 500, mas gravou CONFIRMED, `amount_paid` 599.76 e o e-mail. O check-in também deu 500.
  - Para o dono da pousada, o site nunca consegue mostrar ao hóspede a reserva que fez, nem confirmá-la sem erro. Cada tentativa repetida deixa uma reserva fantasma PENDING segurando o quarto, o que explica o 409 que veio depois no roteiro.
  - As falhas em cadeia do roteiro (confirmar, e-mail, check-in, check-out, cancelamentos, relatórios vazios) vêm desse defeito, porque o roteiro nunca recebe o id.
- **A conta está certa no banco.** Os valores gravados pelo convênio e pela estadia longa conferem com o texto: TECNOSUL, diárias 779.80, desconto 93.58, total 686.22; LONG_STAY, 1749.30, 174.93 e 1574.37. O cálculo funciona; a resposta é que não sai.
- **O pacote de fim de semana não existe na prática.** `Validator.java:71` exige saída na segunda, e `Validator.java:76` exige exatamente 2 noites. Nenhuma data passa nas duas: sexta 12/03/2027 a domingo 14/03/2027 deu 400, e sexta a segunda também. Os R$ 820,00 e os R$ 410,00 do texto nunca acontecem.
- **As 5 conferências do cenário que passaram são todas recusas:** o convênio sem código ou com código errado, o pacote de sábado e de 3 noites, e a estadia de 6 noites. Elas passariam num sistema que recusa tudo.
- **A segunda via de reserva pendente** responde 404 `RESERVATION_NOT_FOUND` (`ReservationAPI.java:247-250`). Esperava-se 409 `OPERATION_NOT_ALLOWED`: a reserva existe, é a situação dela que não permite.
- **Depois de reiniciar,** as mensagens e a ocupação do quarto continuam. A conferência da reserva falhou só por falta do id.
- **Nas reservas simultâneas,** a conferência "nenhuma rodada com as duas criadas" passou por acaso, porque toda criação responde 400. No banco, 7 das 10 rodadas gravaram as duas (veja Banco).
- **Fora do roteiro, no financeiro:**
  - a linha de total sai `total,,,,599.76` (`ReportAPI.java:114`); o roteiro espera `total,599.76`, e o texto não fecha o formato;
  - o filtro `refund IS NOT NULL` (`ReservationService.java:148`) põe no relatório, com 0.00, reservas que não deixaram dinheiro: a pendente cancelada e a STANDARD cancelada com devolução total.

## Dúvidas

- **Nota do banco.** A skill põe em `com problemas` a duplicação de reserva "protegida só dentro do processo" e não lista a falta total de proteção entre os casos de `ruim`. Dei `ruim` porque aqui não há proteção nenhuma e o banco medido tem 7 reservas duplicadas de verdade, o que é "risco de dado errado". Quem ler só a lista de `ruim` da skill poderia dar `com problemas`.
- **Formato da linha de total do financeiro.** Se é `total,,,,599.76`, com as colunas vazias, ou `total,599.76`: o texto diz só "uma linha total com a soma". Não pesou na nota.
