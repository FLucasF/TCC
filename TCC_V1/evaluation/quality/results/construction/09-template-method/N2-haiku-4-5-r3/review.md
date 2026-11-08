# Revisão: construção, cenário 09

## Visão geral

O projeto (`projeto/`, 1.700 linhas em 7 pacotes) segue um desenho em camadas: `Application` monta tudo à mão e registra as rotas do Javalin (`Application.java:59-72`); os handlers em `api` leem a requisição com Gson e chamam `ReservationService`, `EmailService` e `ReceiptService` (`service`), que usam os repositórios JDBC em `db` sobre um pool Hikari guardado num campo `static` (`Database.java:14`). O esquema vem de `CREATE TABLE IF NOT EXISTS` no código (`Database.java:31-85`), e os quatro relatórios ficam em `report`, cada um como uma classe que implementa `Report` e escreve o CSV inteiro. O problema mais sério é que **o sistema não funciona**. Ele não sobe: `ReceiptService.java:11` declara um `DateTimeFormatter.ofPattern("##0.00")` (um padrão de número passado a um formatador de datas, que nem é usado), e isso lança `IllegalArgumentException` ao carregar a classe. O `main` morre em `Application.java:45`, como mostram os logs de `measures/database.md` e `measures/behavior.md`. E mesmo corrigida essa linha, toda rota que devolve JSON responde 500: o `pom.xml` não traz o Jackson, e o Gson criado em `Application.java:36` nunca foi registrado como o JSON do Javalin. Além disso, não há nenhum teste, os relatórios financeiro e por plano deixam de fora a reserva não reembolsável cancelada, e nada impede duas reservas do mesmo quarto na mesma noite.

## Arquitetura

**Números:** 7 pacotes (`com.marealta`, `api`, `db`, `model`, `report`, `service`, `util`), 15 dependências entre eles, nenhum ciclo. O Javalin aparece só em `com.marealta` e `api`, e o JDBC só em `db`. A maior classe é `ReservationsHandler`, com 258 linhas.
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo (api → service → db → model, sem ciclo). As regras de preço e de situação ficam em `ReservationService.java:27-168`, sem Javalin nem SQL, e o SQL fica todo nos repositórios. Mesmo assim, há regra de negócio na borda e um estado global:

- `ReservationsHandler.java:168-173`: a regra de quando existe a segunda via (só para reserva paga) mora no handler, não no serviço.
- `ReservationsHandler.java:97`: o handler é que decide mandar o e-mail depois de confirmar. A confirmação e o envio são duas operações soltas, coordenadas pela borda HTTP.
- `ReservationsHandler.java:220-223`: o handler consulta o `RoomRepository` direto para pôr o tipo do quarto no comprovante, e monta o comprovante em JSON campo a campo, em duas cópias (`:121-141` e `:163-197`). O `ReceiptService` monta o mesmo comprovante em texto, e o handler gera esse texto e o joga fora (`:126`, `:175`).
- `ReservationsHandler.java:89-95`: o valor pago vem do corpo da requisição, quando o texto diz que o pago é o total da reserva, uma regra do domínio.
- `Database.java:14` e `:101`: o pool é um `static` global, e cada repositório chama `Database.getConnection()` por dentro (`ReservationRepository.java:26`, `RoomRepository.java:23`). A montagem em `Application` não liga o acesso a dados aos repositórios, só cria objetos sem dependências.
- `ReportsHandler.java:22-100` tem quatro métodos quase idênticos, e a `ReportRegistry` (`ReportRegistry.java:6-20`) é consultada com o nome fixo em cada um (`ReportsHandler.java:31`, `:51`, `:71`, `:91`).

São misturas localizadas, numa estrutura que, no geral, está separada.

## Design de baixo nível

**Números:** 4 relatórios, 0 no esqueleto comum, 4 escritos inteiros (cada um com a sua linha de título, o seu cabeçalho e o seu total). A `Report` é uma interface com um único `generate(LocalDate)`.
**Classe:** omissão
**Exagero:** exagero
**Nota:** ruim

**O pattern.** O texto pede um formato comum para os quatro relatórios (título, cabeçalho, linhas, total) que vai mudar "em todos de uma vez" (a linha com a data no ano que vem). O código não tem esqueleto nenhum. A interface `Report` (`Report.java:5-7`) só uniformiza a assinatura, e cada classe repete a sequência inteira:

- o título está escrito à mão quatro vezes (`OccupancyReport.java:27`, `RevenueReport.java:32`, `TourismTaxReport.java:34`, `RatePlansReport.java:48`);
- o mesmo vale para o cabeçalho (`:28`, `:33`, `:35`, `:49`) e para a linha do total (`OccupancyReport.java:45`, `RevenueReport.java:47`, `TourismTaxReport.java:50`, `RatePlansReport.java:64`);
- a consulta do mês e a lista de situações se repetem em três classes (`RevenueReport.java:22-29`, `TourismTaxReport.java:24-31`, `RatePlansReport.java:22-29`);
- o `formatMoney` também se repete em três (`RevenueReport.java:52`, `TourismTaxReport.java:55`, `RatePlansReport.java:69`).

Acrescentar a linha da data exigiria mexer nas quatro classes. É a omissão que a referência descreve: os quatro relatórios escritos inteiros, cada um com a sua linha de título.

**O exagero.** A `ReportRegistry` (`ReportRegistry.java:6-20`) é um mapa por nome que só repassa. Quem usa já sabe qual relatório quer e o pede com o nome fixo (`ReportsHandler.java:31`, `:51`, `:71`, `:91`), e `has` nunca é usado (`ReportRegistry.java:17`). Ela não tira nenhuma duplicação: os quatro métodos do handler continuam copiados.

**O resto do código.** Os defeitos estão espalhados:

- **Constante inválida que derruba o sistema:** `ReceiptService.java:11` (o `CURRENCY_FORMAT` com `#`, nunca usado) impede a classe de carregar.
- **Erros transmitidos como texto:** o serviço lança `IllegalArgumentException("ROOM_NOT_FOUND")` (`ReservationService.java:31`, `:35`, `:39`, `:48`, `:78`, `:85`), e o handler traduz a mensagem num `switch` (`ReservationsHandler.java:225-247`). Qualquer outra `IllegalArgumentException` vira 400 por acaso.
- **`catch (Exception)` em todo handler:** as falhas viram 400 `INVALID_REQUEST` (`ReservationsHandler.java:65`, `:78`, `:102`, `:115`, `:144`, `:157`, `:193`), inclusive um erro de banco.
- **Duplicação nos handlers:** o comprovante em JSON está copiado (`ReservationsHandler.java:126-139` e `:175-188`), a classe `ErrorResponse` aparece duas vezes (`ReservationsHandler.java:249`, `ReportsHandler.java:102`), e os quatro métodos de relatório são iguais (`ReportsHandler.java:22-100`).
- **Dinheiro formatado sem controle:** `String.format("%.2f")` depende do locale da máquina (`RevenueReport.java:53` e as outras cópias). O comprovante não põe o separador de milhar que o texto pede (`R$ 1.234,56`): `ReceiptService.java:45` dá `R$ 1234,56`.
- **Tipos e campos sem uso:** `Report.generate(LocalDate)` recebe o mês como "primeiro dia" (`ReportsHandler.java:52`), um tipo que não diz o que é. O campo `gson`, injetado em três handlers, nunca é usado (`RoomsHandler.java:12`, `MessagesHandler.java:14`, `ReservationsHandler.java:27`).
- **Modelo sem proteção:** `Reservation` é um objeto com setters para tudo (`Reservation.java:56-102`), e as regras de transição ficam fora dele.

Os pontos bons são o `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:6-29`, `ReservationService.java:143-161`), o `LocalDate` e os `enum` de situação e de plano.

## Testes

**Números:** 0 testes rodaram (0 falhas); 0 métodos de teste escritos; sem cobertura (nenhum relatório do JaCoCo). Não há `src/test`.
**Nota:** ruim

O projeto não tem nenhum teste. O `pom.xml:54-59` declara o JUnit 4 com escopo de teste, mas nenhuma classe de teste foi escrita, e o `mvn verify` passa só porque não há nada para rodar (`measures/tests-maven.log`, BUILD SUCCESS). Ficam sem rede todas as regras:

- o preço com a temporada e o arredondamento (`ReservationService.java:143-161`, `Season.java:9-26`);
- o desconto e o cancelamento com a devolução;
- as transições de situação;
- e, sobretudo, a parte nova: a taxa de R$ 4,50 por hóspede por noite (`TourismTaxReport.java:16`, `:39`), as duas linhas fixas do relatório por plano (`RatePlansReport.java:54-62`) e o título dos quatro relatórios.

Um único teste que subisse o serviço teria pegado o erro de partida em `ReceiptService.java:11` e a falta do Jackson.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas e as datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number) -> rooms` sem índice; nenhum CHECK nas colunas de situação, plano, tipo e canal; nenhuma restrição UNIQUE, EXCLUDE ou CHECK; 0 transações explícitas; 0 `FOR UPDATE`; nenhum SQL montado com `+`. **O serviço não subiu contra o banco vazio**: o esquema foi criado e o `main` morreu logo depois, em `ReceiptService.java:11`.
**Nota:** ruim

Os tipos estão certos, o SQL usa parâmetros (`ReservationRepository.java:20-43`), as conexões voltam ao pool por try-with-resources, e os quartos são semeados sem duplicar (`Database.java:66-84`). Os defeitos:

- **Nada impede duas reservas do mesmo quarto na mesma noite.** `ReservationService.java:46-73` consulta os conflitos (`ReservationRepository.java:82-108`) e depois insere, em conexões separadas, sem transação, sem trava e sem `EXCLUDE` no banco. Dois pedidos simultâneos passam os dois, e isso é dado errado no banco.
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` num texto quebrado em `;` (`Database.java:31-93`), sem migração, e não sabe acrescentar uma coluna numa tabela que já existe.
- **Sem índice e sem CHECK.** A chave estrangeira não tem índice, e não há CHECK em `status` nem em `rate_plan` (`Database.java:48`, `:53`), nem em `guests >= 1` ou `check_out > check_in`.
- **Confirmar e enviar o e-mail não andam juntos.** São duas escritas sem transação (`ReservationService.java:91` e `EmailService.java:26`, chamadas em sequência por `ReservationsHandler.java:95-97`).

O critério da skill põe na nota `ruim` o serviço que não sobe contra um banco vazio. Independente disso, a falta total de proteção contra a reserva duplicada já é um risco de dado errado.

## Comportamento

**Números:** 0 conferências passaram e 0 falharam. O roteiro `check_09.sh` não conseguiu testar: o serviço não subiu (`ExceptionInInitializerError` em `Application.java:45`, causado por `ReceiptService.java:11`, conforme `measures/behavior.md`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: o `java -jar` termina com erro logo depois de criar as tabelas, e nenhuma rota atende.

Fora do roteiro, para ver o que viria depois, montei uma cópia temporária em `/tmp`, só com a linha 11 do `ReceiptService` trocada, e chamei a API. `GET /rooms` e `POST /reservations` responderam **500**, com "It looks like you don't have an object mapper configured": falta o `jackson-databind` no `pom.xml`. Toda rota que devolve JSON, inclusive as respostas de erro, quebraria. Só os CSV responderam (`Pousada Maré Alta,tourism-tax,2027-07` / cabeçalho / `total,,,,,0.00`).

Pela leitura do código, ainda haveria estes erros contra o texto:

- **Confirmação:** exige `amountPaid` no corpo (`ReservationsHandler.java:89-95`), em vez de usar o total. Uma confirmação sem corpo daria 400, e qualquer valor seria aceito como pago.
- **Relatórios financeiro e por plano:** só contam CONFIRMED, CHECKED_IN e CHECKED_OUT (`RevenueReport.java:28`, `RatePlansReport.java:28`). Deixam de fora a reserva não reembolsável cancelada, que o texto manda contar.
- **Comprovante e e-mail:** escrevem `R$ 1234,56`, sem o ponto de milhar (`ReceiptService.java:45`).
- **Reservas simultâneas:** duas reservas ao mesmo tempo para o mesmo quarto passariam as duas (ver Banco).

## Dúvidas

- O exagero: a `ReportRegistry` é pequena. Contei-a como exagero porque é uma camada que só repassa (quem chama já sabe o nome fixo, e `has` não tem uso), mas um revisor poderia vê-la como inofensiva e dar `contenção`.
- A nota do banco: o esquema em si tem os tipos certos. O `ruim` vem do critério da skill (o serviço não sobe contra o banco vazio, por um defeito que não é de banco) e da falta total de proteção contra a reserva duplicada. Só com o segundo motivo, um revisor poderia ficar entre `com problemas` e `ruim`.
- As falhas que listei no Comportamento, além do serviço que não sobe, vêm da leitura e de uma cópia corrigida em `/tmp`, não do roteiro, que não chegou a rodar nenhuma conferência.
