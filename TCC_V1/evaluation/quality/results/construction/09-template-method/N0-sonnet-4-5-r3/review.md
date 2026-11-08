# Revisão: construção, cenário 09

## Visão geral

O projeto tem camadas clássicas em pacotes separados: `controller` (os handlers do Javalin), `service` (as regras),
`repository` (JDBC puro), `model`, `util`, `exception` e `config`. Tudo é montado à mão em `Main.java:27-65`. Uma
requisição passa pelo controller, que lê o corpo como `Map` e valida os campos (`ReservationController.java:21-44`).
Depois vem o serviço, que calcula o preço e a situação da reserva (`ReservationService.java:28-96`), e por fim o
repositório, que grava no PostgreSQL (`ReservationRepository.java:21-65`). O esquema é criado com
`CREATE TABLE IF NOT EXISTS` em `Database.java:23-70`.

O defeito mais sério está no banco. A coluna `guest_cpf` é `VARCHAR(11)` (`Database.java:40`), e o controller aceita o
CPF do jeito que ele chega (`ReservationController.java:28`). Com o CPF escrito como o texto mostra (111.222.333-44),
**toda criação de reserva dá 500**: no log do serviço aparece "value too long for type character varying(11)". Por
isso só 24 das 64 conferências do roteiro passaram.

Outros problemas:

- a reserva dupla não tem nenhuma proteção. Com o CPF só com dígitos, duas reservas simultâneas do mesmo quarto foram
  criadas nas 9 rodadas novas que eu fiz;
- os quatro relatórios são quatro métodos escritos inteiros. Não há esqueleto comum (omissão do Template Method);
- os testes cobrem 2,8% das linhas e só testam as classes utilitárias;
- o dinheiro do quarto é guardado em `double`.

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `controller` e na raiz (`Main`), e o JDBC só em `repository` e `config`. A maior classe é `ReservationRepository`,
com 276 linhas, e as outras têm todas menos de 250.
**Nota:** bom

- **A borda HTTP é fina.** Os handlers leem a requisição, chamam o serviço e montam a resposta
  (`ReservationController.java:46-80`, `ReportController.java:18-35`). Os erros viram JSON num único lugar
  (`Main.java:67-80`).
- **As regras não conhecem o HTTP nem o SQL.** Preço, temporada, situações da reserva, devolução e relatórios ficam em
  `service` e não importam nada do Javalin nem do JDBC. `ReservationService.java:103-202` usa só o repositório e os
  modelos.
- **O acesso ao banco está isolado** nos três repositórios. A montagem fica toda em `Main.java:27-44`, com as
  dependências passadas pelo construtor e sem `static` global.
- **As dependências vão na direção certa:** controller → service → repository → config.
- **Ressalvas pequenas, que não mudam a nota:**
  - `ReportController.java:44-60`, `70-86` e `96-112` repetem o mesmo trecho que interpreta o mês (isso pesa no
    design);
  - `ReportService.java:33-34` faz uma consulta por quarto para montar o relatório de ocupação;
  - não há pool de conexões: cada operação abre uma conexão com o `DriverManager` (`Database.java:20`). Isso pesa no
    banco.

## Design de baixo nível

**Números:** dos 4 relatórios, 0 passam por uma estrutura comum e os 4 estão escritos inteiros. A linha de título
está copiada 4 vezes (`ReportService.java:27`, `55`, `82`, `110`). A linha do total também está copiada 4 vezes
(`ReportService.java:47`, `74`, `102`, `138`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto avisa que virão mais relatórios e que "quando o jeito dos relatórios mudar, quero que mude em
todos de uma vez" (o exemplo dado é a linha da data no fim). Mesmo assim, `ReportService` tem quatro métodos
independentes:

- `generateOccupancyReport` (`ReportService.java:24-50`);
- `generateRevenueReport` (`52-77`);
- `generateTourismTaxReport` (`79-105`);
- `generateRatePlansReport` (`107-142`).

Cada um repete a mesma sequência: abre um `StringBuilder`, escreve o título com o nome da pousada, escreve o cabeçalho,
monta as linhas e escreve o total. Não existe classe abstrata com método-modelo, nem um escritor genérico que receba as
partes que variam. Para acrescentar a linha de data no fim, é preciso mexer nos quatro métodos, e cada relatório novo
será mais uma cópia. Isso é exatamente o caso de **omissão** da referência.

A duplicação também aparece no controller, com três cópias da leitura do mês (`ReportController.java:44-60`, `70-86`,
`96-112`).

**O exagero.** Não há interface com uma implementação só, nem fábricas, nem camadas que só repassam: **contenção**.

**O resto do design:**

- **Dinheiro em `double`.** A diária está em `double` (`RoomType.java:4-6`, `9`), e o desconto do plano também
  (`RatePlan.java:4-5`, `7`). O preço da noite é calculado com `baseRate * 1.20` em ponto flutuante e só depois
  arredondado (`ReservationService.java:74-78`). O resultado sai certo nos valores de hoje por sorte:
  389.90 × 1.20 dá `467.87999999999994`, que arredonda para 467,88. Por causa disso, o `GET /rooms` devolve
  `nightlyRate: 249.9`, com uma casa só.
- **Formatação de moeda pelo `NumberFormat` de pt-BR** (`MoneyUtil.java:10`, `21`). No Java 21 ele separa "R$" do
  valor com um espaço não separável (U+00A0), e não com um espaço comum. O e-mail e o comprovante saem como
  `R$ 899,64`, e não como o texto pede ("R$ 1.234,56"). Conferi com `cat -A` no corpo da mensagem.
- **`catch (Exception e)` que esconde a causa** (`ReportController.java:58`, `84`, `110`). Qualquer erro do serviço ou
  do banco vira "Invalid month format" com status 400.
- **O mês não é validado:** `2027-13` é aceito e devolve 200 com um relatório vazio (`ReportController.java:45-53`).
- **Corpo que não é JSON:** o `ctx.bodyAsClass(Map.class)` fica fora do `try` (`ReservationController.java:22`), então
  esse caso cai no tratamento genérico e responde 500.
- **Modelos mutáveis e consultas repetidas.** `Reservation` e `Document` são modelos mutáveis, cheios de setters
  (`Reservation.java:29-155`). A lista de colunas do `SELECT` está repetida cinco vezes em `ReservationRepository.java`
  (`96-99`, `121-124`, `156-159`, `190-193`, `222-225`).
- **O que está bem:** os nomes são claros, há `enum` para situação, plano e tipo de quarto, `LocalDate` para as datas,
  e as transições de situação ficam num lugar só (`ReservationService.java:103-189`).

## Testes

**Números:** 13 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 13 métodos de teste escritos. A cobertura é de
2,8% das linhas (25 de 878) e 10,0% dos ramos (14 de 140). Só o pacote `util` é exercitado; `service`, `repository`,
`controller` e `model` têm 0% (`measures/tests.md`).
**Nota:** ruim

- **Existem só três classes de teste**, todas de utilitários:
  - `CpfUtilTest` (a formatação do CPF);
  - `DateUtilTest` (a temporada e os formatos de data);
  - `MoneyUtilTest` (o arredondamento e os formatos de moeda).
- **Nenhum teste confere as regras que o texto pede:** o preço de uma reserva, o desconto do NON_REFUNDABLE, as
  transições de situação, o cancelamento e a devolução, os códigos de erro, o e-mail, o comprovante ou qualquer um dos
  quatro relatórios. Isso inclui a parte nova do cenário: a taxa de R$ 4,50 por hóspede por noite e o relatório por
  plano.
- **A pergunta da mutação.** Se alguém trocasse `4.50` por `5.40` (`ReportService.java:14`), tirasse o filtro de
  situação de `findPaidByCheckInMonth` (`ReservationRepository.java:196`) ou apagasse a linha de título de um dos
  relatórios, nenhum teste falharia.
- **O único teste de moeda é fraco.** `MoneyUtilTest.java:22` aceita qualquer texto que contenha "1.234,56", e por isso
  deixa passar o espaço não separável que estraga o comprovante.
- **Ferramentas declaradas e não usadas.** O `pom.xml` declara RestAssured (`pom.xml:68-72`) e o failsafe
  (`pom.xml:91-100`), mas não existe nenhum teste pela API nem contra o banco.
- **A rede mais importante falta.** Um único teste pela API com o CPF do exemplo do texto teria mostrado que nenhuma
  reserva era criada.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` nas cinco colunas de valor e as
datas estão em `date`. As conferências acusaram:

- 0 chaves estrangeiras (`reservations.room_number` não aponta para `rooms`);
- 0 restrições de regra (sem CHECK, UNIQUE ou EXCLUDE);
- nenhum CHECK sobre `status`, `rate_plan`, `type` e `channel`;
- nenhum pool de conexões;
- 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado no código com `CREATE TABLE IF NOT EXISTS`, sem migrações (`measures/database.md`).
**Nota:** ruim

- **O tipo da coluna do CPF quebra a gravação.** `guest_cpf VARCHAR(11)` (`Database.java:40`) não comporta o CPF no
  formato que o texto mostra, e nada normaliza o valor antes (`ReservationController.java:28`,
  `ReservationService.java:55`). O `INSERT` falha com "value too long for type character varying(11)"
  (`measures/behavior-service.log`) e a API responde 500. É por isso que nenhuma reserva do roteiro foi criada.
- **A reserva dupla não tem proteção.** A regra "um quarto não pode ter duas reservas na mesma noite" é só uma consulta
  seguida de um `INSERT`, em conexões separadas, sem transação, sem trava e sem restrição `EXCLUDE`
  (`ReservationService.java:46-63`, `ReservationRepository.java:119-152`). Com o serviço de pé e o CPF só com dígitos,
  disparei dois pedidos simultâneos para o 202 em 9 conjuntos de datas novos, e **os 9 tiveram as duas reservas
  criadas**: o banco ficou com 2 reservas em cada noite. Isso é dado errado.
- **Os quartos são apagados e regravados a cada subida** (`Database.java:76-82`). Só funciona porque não existe chave
  estrangeira; a capacidade e a diária nem estão no banco, só no `enum` (`RoomType.java:4-6`).
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:27-63`) não sabe acrescentar uma coluna numa
  tabela que já existe, e não há migrações.
- **O que está certo:** o SQL é todo com parâmetros (`?`), as conexões são fechadas com try-with-resources, e os dados
  persistem (as mensagens continuaram lá depois de reiniciar). Cada operação, porém, abre uma conexão nova com o
  `DriverManager` (`Database.java:20`).

## Comportamento

**Números:** 24 de 64 conferências passaram (`measures/behavior.md`):

- parte comum: 15 de 46;
- dados depois de reiniciar: 1 de 3;
- cenário 09: 7 de 13;
- reservas simultâneas: 1 de 2. Nas 10 rodadas os dois pedidos deram 500.

**Nota:** ruim

**O que falhou.** Quase todas as falhas vêm de uma causa só: `POST /reservations` responde 500 com o CPF
"111.222.333-44", o formato que o próprio texto usa. Para o dono da pousada, isso quer dizer que **o site não consegue
criar nenhuma reserva** se o hóspede digitar o CPF com pontos e traço. Sem reserva não há confirmação, e-mail,
check-in, check-out, cancelamento nem linhas nos relatórios. Por isso os relatórios do roteiro vieram só com título,
cabeçalho e total zero.

O que passou sem depender de reserva:

- os quartos;
- as recusas de capacidade, de quarto inexistente, de datas e de campos faltando;
- as linhas de título dos quatro relatórios e o relatório de taxa de um mês vazio.

**O que um CPF só com dígitos mostra.** Repeti o fluxo com `11122233344`, com o serviço de pé contra um banco vazio.
Os cálculos e os relatórios ficam certos:

- 899,64 e 795,40 em julho; 37,48 e 212,42 no NON_REFUNDABLE de uma noite;
- a ocupação dá `total,1/5`;
- o financeiro dá `total,2390.92`, contando a NON_REFUNDABLE cancelada;
- a taxa de turismo dá 27.00 + 18.00 = `total,45.00`;
- os planos dão `STANDARD,2,1595.52`, `NON_REFUNDABLE,1,795.40` e `total,3,2390.92`.

Mesmo assim, aparecem outros defeitos:

- o comprovante no e-mail usa espaço não separável depois de "R$";
- corpo que não é JSON dá 500 em vez de 400;
- `month=2027-13` é aceito com 200;
- no `POST /reservations` e no cancelamento, `discount`, `amountPaid` e `refund` vêm como `0` em vez de `0.00`;
- a reserva dupla acontece em pedidos simultâneos (veja Banco).

O defeito que decide a nota, porém, é a criação de reserva quebrada com o CPF do exemplo do texto.

## Dúvidas

- O texto não diz em que formato o site manda o CPF; o roteiro usa o formato do comprovante (111.222.333-44). Mesmo
  que o site mande só dígitos, um CPF com pontuação deveria ser recusado com 400 ou normalizado, nunca derrubar a
  requisição com 500. Por isso mantive o defeito como do agente.
- Na reserva simultânea, o roteiro não chegou a medir nada, porque os dois pedidos falharam com 500. A conclusão de
  que duas reservas do mesmo quarto são criadas vem do meu teste manual, com o CPF só com dígitos.
