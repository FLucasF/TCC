# Revisão: construção, cenário 01

## Visão geral

O projeto tem sete pacotes. `api` traz uma classe só com as rotas do Javalin (`ReservationAPI`), `service` tem o `ReservationService`, `repository` tem uma classe `Database` com todo o JDBC, `domain` guarda as entidades e os enums, `domain.rateplan` tem uma interface `RatePlan` com cinco implementações e uma fábrica, e `util` tem `Money` e `DateUtils`. Uma requisição entra no handler, que lê o JSON com Gson, chama o serviço, recebe um `Result` com o código de erro e escreve a resposta com `ctx.json`. O serviço pede o plano à fábrica e grava pelo `Database`, que abre uma conexão nova por chamada. Os dados ficam em três tabelas do PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` no código. O defeito mais sério é que **o sistema não funciona pela API**. Todo `ctx.json(...)` usa o mapeador JSON padrão do Javalin, que é o Jackson, e o Jackson não está no `pom.xml`. O Gson é configurado em `ReservationAPI.java:21-35` e nunca é usado. Com isso, quase toda resposta JSON vira 500, inclusive as de erro: o log do serviço mostra 74 vezes "It looks like you don't have an object mapper configured". O roteiro do comportamento passou em 6 de 72 conferências. Mesmo com o Jackson acrescentado (testei numa cópia em /tmp), a confirmação continua quebrada: ela exige um corpo `{"amount": ...}` que o texto não prevê e, sem esse corpo, responde 500. Além disso, o dinheiro é `double` em todo o código, e os testes quase não existem (1,5% das linhas).

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo. `service` usa JDBC; as maiores classes são `ReservationAPI` (414 linhas), `Database` (358) e `ReservationService` (314).
**Nota:** com problemas

As camadas existem, e as dependências vão na direção certa: `api → service → repository → domain`, sem ciclo. As peças são montadas num lugar só, o `main` (`Application.java:30-38`), e as regras de preço não conhecem o Javalin nem o SQL (`domain/rateplan/*`). Os defeitos estão em pontos localizados:

- **Regra de negócio no handler.** Quais situações dão direito à segunda via é decidido na rota (`api/ReservationAPI.java:257-262`). A rota também monta o comprovante à mão, campo por campo (`ReservationAPI.java:275-289`). Já o texto do comprovante do e-mail é montado em outro lugar, no serviço (`service/ReservationService.java:192-211`). São dois "comprovantes" sem um modelo comum. O check-out, que deveria devolver o comprovante, devolve a reserva (`ReservationAPI.java:215`).
- **JDBC vazando para o serviço.** Todos os métodos do serviço capturam `SQLException` e a transformam num código `DATABASE_ERROR` (`ReservationService.java:70-72`, `83`, `106`, `126`, `146`, `172`). Alguns métodos ainda declaram `throws SQLException` para a API (`ReservationService.java:213-221`).
- **A borda repetida.** Cada handler repete o mesmo `switch` de código de erro para status HTTP e os mesmos `catch` (`ReservationAPI.java:158-162`, `183-187`, `206-210`, `229-233`). Não há um tratador de exceções central do Javalin.
- **Consulta N+1 no relatório.** O relatório de ocupação busca o quarto no banco uma vez por linha, dentro do laço (`ReservationService.java:231`). A segunda via traz todos os quartos para filtrar em Java (`ReservationAPI.java:265-268`).

## Design de baixo nível

**Números:** os 5 planos estão em classes que implementam `RatePlan` (preço, validação e devolução) e são escolhidos por um único `switch` de seleção (`RatePlanFactory.java:7-13`). Ainda assim, há 1 `if` de comportamento sobre o plano no serviço, com cast (`ReservationService.java:42-47`). A regra de preço por noite está copiada em 5 lugares, e a validação comum (hóspedes, capacidade, datas) em 5 classes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O que aproxima do Strategy: cada um dos cinco planos, os dois que já existiam inclusive, tem uma classe com `validate`, `calculatePrice` e `getRefundPercentage` (`domain/rateplan/RatePlan.java:6-11`). O cancelamento pede a devolução ao plano sem nomeá-lo (`ReservationService.java:163-166`). As regras de uso de cada plano ficam dentro dele: sexta a domingo em `WeekendPackageRatePlan.java:19-26`, 7 noites em `LongStayRatePlan.java:14-17` e o código da empresa em `CorporateRatePlan.java:18-24`.

O que afasta: a interface não carrega o código da empresa no cálculo. Por isso o serviço continua perguntando qual é o plano para calcular o preço: `if (ratePlanType == RatePlanType.CORPORATE)`, com cast para `CorporateRatePlan` e chamada a um método que só ele tem (`ReservationService.java:42-47`). O `calculatePrice` da própria interface, no convênio, devolve o preço **sem o desconto** (`CorporateRatePlan.java:43-60`). Ou seja, quem usar a estrutura como ela promete recebe um valor errado. Isso é exatamente "quem usa a estrutura ainda perguntando qual é o caso", e o próximo plano com um parâmetro próprio (a melhor idade, a antecedência de 60 dias) vai pedir mais um `if` no serviço. A estrutura existe e cobre quase tudo, mas convive com um desvio no ponto principal, o preço: por isso, malfeito.

**O exagero.** Contenção. A fábrica é o único ponto de seleção, que a referência aceita. `Result` e os records `ValidationError` e `PricingResult` são pequenos e usados. Há código morto (`DateUtils.isWeekend`/`formatYearMonth`, `Money.parseDouble`, `CorporateRatePlan.getCompanyDiscount`, o `Gson` inteiro), mas isso não é estrutura a mais.

**O resto do código.** Os defeitos estão espalhados:

- **Dinheiro em `double`** em toda parte: `Reservation.java:17-22`, `RoomType.java:10`, `RatePlan.java:19` e `Database.java:164-168` (`setDouble`). O arredondamento faz `new BigDecimal(double)` e volta para `double` (`util/Money.java:7-10`). O "meio para o par" vira, na prática, o arredondamento da representação binária. Ele acerta os exemplos que testei (37,48 e 86,98), mas por sorte da representação, não por construção.
- **A formatação depende da localidade do sistema.** `String.format("%.2f")` sem `Locale` aparece em `Money.java:13` e `Money.java:17`. Numa JVM em pt-BR, o CSV sairia com "599,76", e o `formatBr` quebraria o `split("\\.")`.
- **Duplicação.** O laço do preço por noite com a temporada está copiado em `StandardRatePlan.java:33-41`, `NonRefundableRatePlan.java:33-41`, `LongStayRatePlan.java:37-45` e duas vezes em `CorporateRatePlan.java:48-57` e `72-80`. As mesmas checagens de hóspedes, capacidade e datas se repetem em cada plano (por exemplo, `StandardRatePlan.java:11-23` e `NonRefundableRatePlan.java:11-23`), e as de hóspedes e capacidade nem são regras do plano.
- **Erros escondidos.** O `catch (Exception)` responde 500 `DATABASE_ERROR` para qualquer coisa, inclusive para um corpo que não é JSON (`ReservationAPI.java:121-122`; `getAsJsonObject` lança `IllegalStateException`, que não é `JsonSyntaxException`).
- **Um mapeador JSON configurado e nunca usado** (`ReservationAPI.java:21-35`). É a causa da quebra geral do sistema.

## Testes

**Números:** 5 testes rodaram, 0 falhas, 0 erros. São 5 métodos escritos em 2 arquivos (`DateUtilsTest`, `MoneyTest`). Cobertura: 14 de 955 linhas (1,5%) e 10 de 298 ramos (3,4%). Os pacotes `api`, `service`, `repository`, `domain` e `domain.rateplan` ficaram com 0% de cobertura.
**Nota:** ruim

O que existe confere só utilitários: a alta temporada em algumas datas (`DateUtilsTest.java:13-19`), a contagem de noites e a sexta→domingo. Nenhum plano de tarifa é testado: nem o preço, nem o desconto, nem a devolução, nem as recusas. A situação da reserva, os relatórios, a API e o banco também ficaram de fora. O teste de "meio para o par" usa tolerância de 0,01 (`MoneyTest.java:11-14`), então passaria com qualquer arredondamento, inclusive o "meio para cima". Pela pergunta da mutação: trocar 0.12 por 0.21 no convênio (`CorporateRatePlan.java:11`), 50.0 por 100.0 na devolução do pacote (`WeekendPackageRatePlan.java:46`) ou 7 por 6 noites (`LongStayRatePlan.java:9`) não derrubaria nenhum teste. Nenhum teste sobe a aplicação, e por isso nada pegou o fato de que nenhuma resposta JSON funciona.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e os 5 campos de dinheiro em `numeric(10,2)`. As conferências acusaram: a chave estrangeira `reservations(room_number)` sem índice; nenhum CHECK nas colunas de situação, plano, tipo e canal; nenhuma restrição UNIQUE/EXCLUDE; nenhum pool; nenhuma transação; nenhum `FOR UPDATE`; o esquema criado por `CREATE TABLE` no código, sem migração.
**Nota:** com problemas

O esquema em si está correto nos tipos: datas em `date`, dinheiro em `numeric(10,2)`, `NOT NULL` no que é obrigatório e a reserva ligada ao quarto (`repository/Database.java:36-55`). Todo SQL usa `?`. Os problemas:

- **Não há migrações.** O `CREATE TABLE IF NOT EXISTS` (`Database.java:29-74`) não sabe acrescentar uma coluna quando vier o próximo plano.
- **Nada impede duas reservas do mesmo quarto na mesma noite.** A disponibilidade é verificada numa conexão (`Database.java:229-247`) e a reserva é gravada em outra (`Database.java:145-177`), em passos separados no serviço (`ReservationService.java:35` e `66`). Não há transação nem trava, e nenhuma restrição no banco. O roteiro não conseguiu medir isso (toda criação deu 500), mas pela leitura do código a corrida existe.
- **Duas escritas sem transação.** Confirmar a reserva e guardar o e-mail são escritas separadas (`ReservationService.java:100-102`).
- **Uma conexão nova por chamada**, com `DriverManager` e sem pool (`Database.java:111-113`).
- **O dinheiro passa pelo JDBC como `double`** (`Database.java:164-168` e `349-355`). O `numeric(10,2)` da coluna protege o que fica gravado, mas o caminho até lá é ponto flutuante (esse defeito já pesou no design).

## Comportamento

**Números:** 6 de 72 conferências passaram. Parte comum: 4 de 46. Dados depois de reiniciar: 1 de 3. Cenário 01 (os planos novos): 0 de 21. Duas reservas ao mesmo tempo: 1 de 2 (passou só a conferência de "nenhuma com as duas criadas", porque nenhuma foi criada).
**Nota:** ruim

O serviço sobe, mas quase toda rota que responde JSON dá 500. Isso inclui `GET /rooms`, a criação de reserva e todas as respostas de erro (400, 404, 409, 422). A causa é o mapeador JSON que falta (`measures/behavior-service.log`; `ReservationAPI.java:68`, `90`, `118` etc.). Passaram só o que não depende de JSON: os cabeçalhos dos CSVs, o tipo `text/csv` e a lista de mensagens depois de reiniciar (que estava vazia). Para o dono da pousada, isso quer dizer que o site não consegue listar quartos, nem criar, confirmar ou cancelar reservas, e nenhum dos três planos novos pode ser usado.

Para separar o que é só esse defeito, rodei uma cópia em /tmp com o Jackson acrescentado. As contas dos planos estão certas: 599,76; Tecnosul 779,80 / 93,58 / 686,22; pacote 820,00 com devolução de 410,00; estadia longa 1.749,30 / 174,93 / 1.574,37; o convênio sem código foi recusado com 400. Mesmo assim, três defeitos continuariam:

- **A confirmação do texto não funciona.** `POST /reservations/{id}/confirmation` sem corpo responde 500 "Not a JSON Object". O agente exige um `amount` informado pelo cliente (`ReservationAPI.java:147-153`), quando o texto diz que o pago é o total da reserva. Sem confirmação não há e-mail, check-in, check-out, segunda via nem relatório com dados.
- **O check-out devolve a reserva, e não o comprovante** (`ReservationAPI.java:215`). A segunda via de uma reserva pendente responde 400, e não 409 (`ReservationAPI.java:260`).
- **O dinheiro sai sem as 2 casas** (`820.0`, `0.0`), e um corpo que não é JSON dá 500 em vez de 400.

## Dúvidas

- A classe do pattern ficou entre `malfeito` e `acerto`. Os cinco planos estão na estrutura, e a validação e a devolução são pedidas ao plano. O que decidiu pelo `malfeito` foi o `if (ratePlanType == RatePlanType.CORPORATE)` com cast, no ponto principal, o preço (`ReservationService.java:42-47`), somado ao `calculatePrice` do convênio que, pela interface, devolve o valor errado.
- Na nota do banco, considerei `ruim` por causa da falta total de proteção contra reservas duplicadas. Fiquei com `com problemas` porque o esquema guarda dinheiro em `numeric` e a skill põe a proteção fraca contra duplicação entre os defeitos de `com problemas`. A corrida não pôde ser medida, porque toda criação deu 500.
- As contas dos planos e o arredondamento foram conferidos só na cópia com o Jackson acrescentado. No projeto entregue, nada disso é visível pela API.
