# Revisão: construção, cenário 09

## Visão geral

O projeto tem 6 pacotes: `com.pousada` (só o `App`, com as rotas do Javalin), `service` (`ReservationService`, `ReportGenerator`, `DocumentGenerator`), `db` (uma classe `Database` com todo o JDBC e o `CREATE TABLE`), `model`, `dto` e `util` (`PriceCalculator`). Uma requisição entra num lambda em `App.java`, que lê o corpo, chama o serviço (ou, em várias rotas, a `Database` direto) e monta o DTO à mão. Os dados ficam no PostgreSQL, em 3 tabelas criadas por `CREATE TABLE IF NOT EXISTS` (`db/Database.java:34-82`). O defeito mais sério está na confirmação: o sistema espera que o site mande `amountPaid` no corpo (`App.java:144-146`, `dto/ConfirmationRequest.java:6`), e o texto não pede esse campo. Sem ele, a confirmação responde 400. Com isso quebra o resto do ciclo: não sai o e-mail, não há check-in nem check-out, e os quatro relatórios saem vazios (38 de 64 conferências passaram). Além disso, o pago vem do cliente em vez de ser o total da reserva. Os quatro relatórios estão escritos inteiros, um por um, sem o esqueleto comum que o texto pedia. O comprovante formata o dinheiro pelo locale padrão ("R$ 499.80"). Os testes cobrem 3,4% das linhas, e duas reservas ao mesmo tempo para o mesmo quarto foram criadas nas 10 rodadas.

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, nenhum ciclo. Javalin e Jackson só em `com.pousada`, JDBC e o pool só em `com.pousada.db`. As maiores classes são `Database` (420 linhas) e `App` (405 linhas).
**Nota:** com problemas

As dependências apontam para o lado certo (borda → serviço → dados) e as regras não conhecem o Javalin nem o JDBC: os serviços recebem a `Database` pelo construtor (`service/ReservationService.java:14`), e a montagem acontece num lugar só (`App.java:44-50`). Mas a borda HTTP não é fina:
- Várias rotas falam com a `Database` sem passar pelo serviço (`App.java:64`, `App.java:148`, `App.java:210`, `App.java:251`, `App.java:280`).
- A regra "segunda via só para reserva paga" mora no handler (`App.java:245`).
- O envio do e-mail de confirmação, com assunto e gravação da mensagem, também mora no handler (`App.java:151-153`), fora do serviço e sem transação junto com a confirmação.
- A montagem do comprovante está copiada em duas rotas (`App.java:213-224` e `App.java:254-265`).
- O estado do `App` fica em campos `static` mutáveis (`App.java:21-24`).

`Database` é um único DAO de 420 linhas que cuida de quartos, reservas, mensagens, relatórios e da criação do esquema. É o lugar onde o próximo pedido vai doer. São misturas localizadas, sem ciclo.

## Design de baixo nível

**Números:** dos 4 relatórios, 0 passam por um esqueleto comum. Os 4 são métodos escritos inteiros em `ReportGenerator` (`service/ReportGenerator.java:20`, `:41`, `:71`, `:100`), cada um com a sua linha de título copiada (`:24`, `:45`, `:75`, `:104`) e a sua própria sequência de cabeçalho, linhas e total.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede o Template Method. Ele descreve quatro relatórios com os mesmos passos, anuncia mais dois e avisa que "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez" (a linha com a data no fim). Não existe classe abstrata, método-modelo nem escritor genérico que receba as partes que variam. Cada método monta o seu `StringBuilder`, escreve o título "Pousada Maré Alta,<nome>,<período>", o cabeçalho, as linhas e o total (`service/ReportGenerator.java:20-39`, `:41-69`, `:71-98`, `:100-135`). Acrescentar a linha de data obrigaria a mexer nos quatro métodos, e o próximo relatório seria mais uma cópia. Pela referência, isso é omissão. O cálculo do "dinheiro que ficou com a pousada" também está copiado (`:51-54` e `:113-116`). A taxa de R$ 4,50, que "muda todo ano", é uma constante local dentro do método (`:78`). O total de quartos da ocupação está fixo em "/5" (`:36`).

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma camada que só repassa. Contenção.

**O resto do código.** Os defeitos estão espalhados:
- **Dinheiro e locale:** o comprovante usa `String.format("R$ %.2f")`, que depende do locale padrão e nunca põe separador de milhar (`service/DocumentGenerator.java:46`). Na prática o e-mail saiu "R$ 499.80" e não "R$ 499,80". O CSV usa o mesmo `String.format("%.2f")` (`service/ReportGenerator.java:143`) e sairia com vírgula num servidor em pt_BR.
- **O pago vem do cliente:** `confirmReservation` recebe o `amountPaid` como parâmetro (`service/ReservationService.java:73`) em vez de usar o total da reserva.
- **Erros escondidos:** todo handler termina em `catch (Exception)` que responde 400 INVALID_REQUEST com a mensagem crua (`App.java:162-164`, e o mesmo em todas as rotas). Um erro do banco vira erro do cliente: com `{}` no corpo da confirmação, a resposta foi 400 com "null value in column amount_paid violates not-null constraint".
- **Plano como texto:** o plano de tarifa passa para o cálculo como `String` (`service/ReservationService.java:45`, `util/PriceCalculator.java:49`) em vez do `enum` que já existe.
- **Desconto sem as 2 casas:** o desconto "zero" é `BigDecimal.ZERO` sem escala (`util/PriceCalculator.java:53`), e o JSON sai `"discount":0`.
- **Exceções copiadas:** seis classes de exceção iguais, cada uma com um campo `code` público (`service/ReservationService.java:135-181`).
- **Formatador repetido:** `DateTimeFormatter.ofPattern("yyyy-MM-dd")` é recriado em dezenas de lugares (`App.java:96`, `:219`, `:392`, `service/ReportGenerator.java:32-33` ...).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados, 7 métodos escritos, num arquivo só. Cobertura de linhas 28 de 829 (3,4%) e de ramos 14 de 138 (10,1%). Só `com.pousada.util` está coberto (28 de 29 linhas); `service`, `db` e `com.pousada` estão em 0%.
**Nota:** ruim

Os testes que existem conferem valores, e certos:
- a temporada (`src/test/java/com/pousada/util/PriceCalculatorTest.java:12-18`);
- a diária de 299,88 (`:30`);
- o subtotal de 599,76 (`:61`);
- o desconto de 89,96 e o total (`:72`, `:80`).

Mas só testam o `PriceCalculator`. Não há teste para as situações da reserva, o cancelamento e a devolução, os códigos de erro, o comprovante, o e-mail, a API ou o banco. Também não há nenhum teste dos relatórios, que são a parte nova do cenário. Pela pergunta da mutação:
- trocar 4.50 por 5.40 (`service/ReportGenerator.java:78`) não derruba nenhum teste;
- apagar a linha de título de um relatório não derruba nenhum teste;
- trocar a ordem STANDARD/NON_REFUNDABLE não derruba nenhum teste;
- incluir reservas canceladas na taxa de turismo não derruba nenhum teste.

Nenhum teste chama a confirmação, que é justamente o que está quebrado. A coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 4 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- nenhum SQL montado com `+`.

**Nota:** com problemas

O que está certo: os tipos, as chaves, `NOT NULL` no obrigatório, SQL todo com `?`, conexões em try-with-resources e um pool HikariCP (`db/Database.java:16-25`). Os dados sobrevivem ao reinício: as mensagens continuaram lá.

Os defeitos:
- **Sem migrações:** o esquema é criado por `CREATE TABLE IF NOT EXISTS` no código (`db/Database.java:34-82`), que não sabe evoluir uma tabela existente.
- **Reserva dupla sem proteção:** a regra "um quarto não pode ter duas reservas na mesma noite" é um SELECT seguido de um INSERT em conexões separadas, sem transação, trava nem restrição (`service/ReservationService.java:38` e `:62`; `db/Database.java:229-253` e `:172-208`). No roteiro, as duas reservas simultâneas foram criadas nas 10 rodadas. Um `EXCLUDE` com `daterange` ou uma trava teriam impedido isso.
- **Escritas soltas:** a confirmação e a gravação do e-mail são duas escritas separadas, sem transação (`App.java:146` e `:152`).
- **Consulta compartilhada:** a consulta do mês serve a três relatórios com regras diferentes (`db/Database.java:372-373`). Ela inclui canceladas com `amount_paid > 0`, e a taxa de turismo, que deve contar só as pagas não canceladas, usa a mesma consulta (`service/ReportGenerator.java:72`).

## Comportamento

**Números:** 38 de 64 conferências passaram (26 falharam).
- Parte comum: 29 de 46.
- Dados depois de reiniciar: 1 de 3.
- Cenário 09: 7 de 13.
- Reservas simultâneas: 1 de 2.

**Nota:** ruim

**Onde quebra.** A parte comum quebra na confirmação. O site confirma sem mandar corpo, como o texto descreve, e o sistema responde 400 INVALID_REQUEST. Sem confirmação:
- não sai o e-mail;
- não há segunda via, check-in nem check-out;
- o cancelamento da STANDARD não devolve nada;
- a ocupação e o financeiro saem vazios.

Por isso falham também as conferências depois de reiniciar: a reserva ficou CANCELLED e o quarto ficou livre. Pela mesma razão, falham as linhas dos quatro relatórios do cenário. Passam só os títulos, os cabeçalhos, o mês vazio e as recusas.

**O que funciona.** A criação da reserva com o preço certo (599,76; desconto de 37,48), as recusas com os códigos certos e o cancelamento que libera o quarto.

**Fora do roteiro**, chamando a API à mão com `{"amountPaid":499.80}` no corpo:
- o e-mail sai com "R$ 499.80" em vez de "R$ 499,80";
- a segunda via é emitida para uma reserva cancelada (`App.java:245` só barra a PENDING);
- o financeiro lista a STANDARD cancelada e devolvida com 0.00, quando o texto diz que só entram as que deixaram dinheiro;
- o pago é o valor que o cliente mandar, qualquer que seja.

**Para o dono da pousada:** pelo site, nenhuma reserva chega a ser confirmada. O hóspede não recebe o comprovante, e a recepção não consegue fazer check-in. O contador e a prefeitura recebem relatórios zerados. E, quando duas pessoas reservam o mesmo quarto ao mesmo tempo, as duas reservas são aceitas.

## Dúvidas

O texto não diz se a confirmação traz um corpo. Mas diz que "o que ele pagou é o total da reserva", então exigir `amountPaid` do cliente é erro do agente, não um ponto em aberto. Fica sem resposta como os relatórios se comportariam se a confirmação funcionasse. Pela leitura, a taxa de turismo contaria a NON_REFUNDABLE cancelada (`db/Database.java:372`, usada em `service/ReportGenerator.java:72`), o que o texto não quer. O roteiro não chegou a mostrar isso, porque os relatórios saíram vazios.
