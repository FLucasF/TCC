# Revisão: construção, cenário 07

## Visão geral

O projeto tem seis pacotes. `Main` registra as rotas do Javalin em handlers estáticos. `services` guarda as regras da reserva (`ReservationService`) e o envio de e-mail (`EmailService`). `db.Database` é uma classe só com todo o SQL, inclusive os dois relatórios. `util` tem o preço, o validador de e-mail e o texto do comprovante, e `models` tem DTOs com campos públicos. Uma requisição vai de `Main` para `ReservationService` e dali para `Database`, que abre uma conexão nova com `DriverManager` a cada chamada e grava em três tabelas criadas com `CREATE TABLE IF NOT EXISTS`. Duas coisas pesam mais.

- **O sistema não funciona pela API.** O `pom.xml` não traz o módulo de datas do Jackson (`pom.xml:28-32`), então nenhuma resposta com `Reservation` ou `Receipt` consegue ser escrita, porque esses objetos têm campos `LocalDate` (`models/Reservation.java:86-87`). Criar uma reserva grava a linha no banco, mas responde 400. `GET /reservations/{id}` responde 500. Confirmar, fazer o check-in, cancelar, fazer o check-out e pedir a segunda via respondem 400, embora as mudanças fiquem gravadas.
- **As regras de envio ficaram em `if`.** As três regras do fim do texto são três `if` dentro de `EmailService.sendEmail` (`services/EmailService.java:34,43,51`), sem nenhuma estrutura para as regras que o dono já anunciou.

Há também a reserva dupla: nada no código impede duas reservas simultâneas para o mesmo quarto.

## Arquitetura

**Números:** 6 pacotes, 9 dependências entre eles, nenhum ciclo. `Database` tem 342 linhas e `Main` 266; juntas, são 54% do código (608 de 1.134 linhas em `src/main`). JDBC aparece em `Main` e em `services` apenas por causa do `SQLException`.
**Nota:** com problemas

As dependências vão no sentido certo (borda → serviços → dados) e as regras não conhecem o Javalin. A separação, porém, falha em alguns pontos:

- **A borda pula o serviço.** Vários handlers chamam o `Database` direto: `GET /rooms` (`Main.java:61`), `GET /reservations/{id}` (`Main.java:97`), `GET /messages` (`Main.java:179`) e os dois relatórios (`Main.java:193,219`).
- **O CSV é montado no handler** (`Main.java:195-199,221-225`).
- **Regras de negócio moram na classe de dados.** O valor líquido do financeiro (`db/Database.java:302`), quais situações entram no relatório (`db/Database.java:249,292`) e o "/5" fixo da ocupação (`db/Database.java:276`) estão no `Database`.
- **`Database` faz tudo:** cria o esquema, cadastra os quartos, faz o CRUD de reservas e de mensagens e monta os relatórios.
- **A montagem usa estado estático.** Ela acontece no `main` (`Main.java:33-37`), mas guarda as peças em campos `static` mutáveis (`Main.java:21-23`).
- **O pagamento vem da borda.** O valor pago é lido do corpo da requisição no handler (`Main.java:113-114`), em vez de a regra usar o total.

## Design de baixo nível

**Números:** as 3 regras de envio estão fora de qualquer estrutura: 0 embrulhos, 3 `if` em `EmailService.sendEmail`. Não existe interface de envio. O modo de teste entra como `boolean` no serviço (`Main.java:36`, `services/EmailService.java:17-20`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido.** O texto pede o Decorator: um embrulho do envio para cada regra, com a combinação por modo decidida na montagem. O código faz o contrário:

- o bloqueio do endereço inválido é um `if` (`services/EmailService.java:34-37`);
- o desvio do modo de teste é outro `if` sobre o campo `testMode` (`services/EmailService.java:43-47`);
- a cópia para o arquivo é um terceiro `if (!testMode)` (`services/EmailService.java:51-55`).

Nada se aproxima do pattern. Não há interface de envio nem uma peça por regra. A combinação por modo ("cópia só fora do teste") está escrita dentro do próprio envio. A segunda pousada, que não quer a cópia, ou a regra da madrugada exigiriam mexer nesse método e acrescentar mais `if`. Isso é omissão.

**Exagero:** nenhum. Não há interfaces de uma implementação só, nem fábricas ou camadas que só repassam.

**O resto do código.** Os tipos estão certos onde mais importa: `BigDecimal` com `HALF_EVEN` a cada passo (`util/PricingUtil.java:47`), `LocalDate` nas datas e `enum` para as situações. Os defeitos, porém, se repetem:

- **Erros como texto solto.** Os códigos de erro viajam como mensagem de `IllegalArgumentException` (`services/ReservationService.java:28,36,44,65`…) e voltam a virar status num `switch` sobre a string (`Main.java:235-265`).
- **`catch (Exception)` em todos os handlers.** Cada handler tem um que transforma qualquer falha em 400 `INVALID_REQUEST` (`Main.java:88-91,120-123,133-136,146-149,159-162,172-175,203-206`). Foi isso que escondeu a quebra da serialização das datas, devolvida ao cliente como "pedido inválido". Erros de banco também viram 400.
- **Plano de tarifa como `String` sem validação.** A comparação é feita com literais (`util/PricingUtil.java:42`, `services/ReservationService.java:125`). Um `ratePlan` qualquer é aceito e tratado como STANDARD.
- **Montagem do `Receipt` copiada três vezes** (`services/ReservationService.java:72,109,145`). O mesmo vale para o bloco "busca, 404, confere a situação", repetido em cinco métodos.
- **Dinheiro do comprovante no formato errado.** Ele é escrito com `toPlainString()` (`util/ReceiptFormatter.java:36`), o que dá "R$ 599.76" em vez de "R$ 599,76" e não tem separador de milhar. Ao lado há um `DateTimeFormatter.ofPattern("0.00")` morto, usado para dinheiro (`util/ReceiptFormatter.java:10`).
- **Detalhes menores:** casts sem checagem do corpo como `Map` (`Main.java:67-76`), a variável `updated` sem uso (`services/ReservationService.java:106`) e campos públicos mutáveis em todos os modelos.

## Testes

**Números:** 25 testes rodaram, 0 falharam, 0 foram pulados; há 25 métodos de teste. Cobertura de linhas: 77 de 588 (13,1%). Cobertura de ramos: 44 de 171 (25,7%). `util` tem 63 de 66 linhas cobertas; `services`, `db` e `Main` têm 0. Nenhum teste passa pela API ou pelo banco.
**Nota:** com problemas

**O que está testado com valores:**

- o preço com a temporada e o arredondamento (`PricingTest.java:39-42` para 299,88; `:62` para 599,76; `:69` para o desconto 89,96; `:84` para o total 509,80);
- os limites da alta temporada (`PricingTest.java:20-24`);
- o validador de e-mail, com os casos do texto (`EmailValidatorTest.java:19,24,29,39,65`).

**O que ficou sem teste:**

- **A maior parte da parte nova do cenário:** a mensagem `BLOCKED` gravada, a cópia para o arquivo e o desvio no modo de teste (`EmailService`, 0% coberto).
- **A parte comum:** as transições de situação, o reembolso no cancelamento, os códigos de erro e os dois relatórios.

Pela pergunta da mutação, alguém poderia apagar a cópia para o arquivo (`services/EmailService.java:51-55`), trocar o `TEST_PREFIX` ou inverter o `if (testMode)`, e nenhum teste falharia.

**Problemas nos testes que existem:**

- `IntegrationTest` só formata o comprovante, sem banco nem HTTP, apesar do nome.
- Ele fixa o formato errado do dinheiro: `assertTrue(formatted.contains("R$ 779.80"))` (`IntegrationTest.java:33`, também `:47`).
- As asserções são do tipo `contains`, e não conferem o texto inteiro.
- Um único teste pela API teria pegado a quebra da serialização das datas, que derruba o sistema inteiro.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)` e as datas em `date`. Conferências que acusaram algo:

- a chave estrangeira `reservations(room_number)` não tem índice;
- não há CHECK nas colunas de situação, plano e tipo;
- não há UNIQUE, EXCLUDE nem CHECK protegendo regra nenhuma;
- não há pool de conexões, nenhuma transação explícita e nenhum `FOR UPDATE`.

Nenhum SQL é montado com `+`.
**Nota:** com problemas

O esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:30-69`), sem migrações, e não sabe evoluir uma tabela que já exista. O SQL é todo parametrizado e as conexões são fechadas com try-with-resources, mas cada chamada abre uma conexão nova por `DriverManager` (`db/Database.java:97`).

**Nada impede a reserva dupla.** O código consulta a disponibilidade (`db/Database.java:186-201`) e depois insere (`db/Database.java:128-158`), sem trava, sem transação e sem restrição de exclusão. Eu confirmei pela API: em 3 rodadas de dois `POST` simultâneos para o quarto 301 nas mesmas noites, o banco ficou com **2 linhas em cada rodada**.

**As transições de situação também não são atômicas.** O código lê a reserva e depois faz um `UPDATE ... WHERE id = ?` sem conferir a situação no próprio `UPDATE` (`db/Database.java:174-184`).

Os tipos e as chaves estão certos e nada se perde ao reiniciar, por isso não dou `ruim`. A falta total de proteção contra a reserva dupla é o defeito mais sério da coluna.

## Comportamento

**Números:** 21 de 65 conferências passaram, 44 falharam:

| parte | passaram |
|---|---|
| parte comum | 17 de 46 |
| depois de reiniciar | 2 de 3 |
| cenário 07, modo normal | 1 de 9 |
| cenário 07, modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas a parte comum quebra logo na primeira reserva.

**Criar a reserva.** `POST /reservations` grava a linha e responde 400 com a mensagem do Jackson: "Java 8 date/time type `java.time.LocalDate` not supported by default". `GET /reservations/{id}` responde 500. Sem o id da reserva, o roteiro não consegue seguir, e quase todas as outras conferências caem em cascata: confirmação, e-mail, check-in, check-out, cancelamentos, segunda via e os relatórios com dados.

Mesmo sem a falha das datas, sobrariam erros meus, conferidos com `curl` num banco à parte:

- **A confirmação exige um corpo com `amountPaid`** (`Main.java:113-114`). Sem esse corpo, ela responde 400. Com ele, aceita qualquer valor: confirmei uma reserva de R$ 212,42 com `amountPaid: 1`, e o financeiro passou a mostrar 1.00. O texto diz que "o que ele pagou é o total da reserva".
- **O comprovante e o e-mail saem com "R$ 599.76"**, e não com "R$ 599,76" como o texto mostra.
- **Respostas de erro.** Todas as operações que mudam a reserva respondem 400 `INVALID_REQUEST` mesmo quando a mudança foi gravada. O cliente do site recebe "pedido inválido" para uma reserva que foi criada ou confirmada de verdade.
- **O financeiro lista com 0.00 as canceladas que não deixaram dinheiro** (`db/Database.java:292`), contra "as reservas que deixaram dinheiro com a pousada".

**As regras de envio do cenário.** No meu teste manual, com o corpo que o código exige, elas funcionaram: o e-mail para o hóspede foi seguido da cópia "[Cópia] " para o arquivo, e "bia@gmail" ficou `BLOCKED` sem cópia. Pelo roteiro, que segue o texto, nenhuma mensagem chega a ser gerada, então 13 das 14 conferências do cenário falharam. A única que passou ("a cópia tem o mesmo texto") compara dois vazios.

**Reservas simultâneas.** Nas 10 rodadas, os dois pedidos voltaram com 400. Pelo meu teste, as duas reservas são gravadas, então a conferência "nenhuma com as duas criadas" passou só porque a resposta quebra.

**Depois de reiniciar,** as mensagens e a ocupação continuam lá. A conferência da reserva falhou porque o roteiro nunca recebeu o id dela.

Para o dono da pousada, isso quer dizer que o site não consegue criar nem acompanhar uma reserva, a confirmação depende de o site informar o valor pago, e o comprovante sai com o dinheiro no formato errado.

## Dúvidas

- **Banco.** A falta total de proteção contra a reserva dupla (confirmada no banco) poderia justificar `ruim`. Fiquei com `com problemas` porque o roteiro do comportamento já mede a concorrência, e porque o resto do esquema (tipos, chaves, parâmetros) está certo.
- **Design de baixo nível.** O `catch (Exception)` em todos os handlers e os erros como texto se espalham por toda a borda, e uma leitura mais dura daria `ruim`. Dei `com problemas` porque o código é pequeno e os tipos de domínio estão certos.
- **Testes.** Com 13% de linhas e nenhum teste da parte nova além do validador, a nota fica no limite entre `com problemas` e `ruim`. Pesou que os testes que existem conferem valores reais do preço.
