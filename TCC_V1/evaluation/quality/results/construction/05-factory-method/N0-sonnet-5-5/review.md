# Revisão: construção, cenário 05

## Visão geral

O projeto tem um pacote só, `br.com.marealta`, com 9 arquivos e cerca de 900 linhas. A montagem fica em `Main.java:9-13`, que cria o `Db`, o `Service` e o `Api`. Uma requisição entra pelas rotas do Javalin em `Api.java:33-58`. O `Api` lê e valida o JSON (`Api.java:72-120`) e chama o `Service`. Cada operação do `Service` roda numa transação (`Db.tx`, `Db.java:40-54`) e usa o `Repository`, que é estático e guarda todo o SQL com parâmetros. Os dados ficam em três tabelas do PostgreSQL (`rooms`, `reservations`, `messages`), criadas com `CREATE TABLE IF NOT EXISTS` dentro do código (`Db.java:56-104`). A escolha do documento pelo tipo de hóspede mora num só lugar, `Documents.render` (`Documents.java:19-25`), um `switch` sobre a interface selada `Identification`. Os três lugares que emitem documento passam por ela. O roteiro do comportamento passou inteiro (83 de 83). Nada é grave. Os pontos fracos:
- o pacote único;
- trechos de SQL que moram no `Service`;
- o esquema criado no código, sem migrações;
- testes só de unidade, que cobrem 17% das linhas.

## Arquitetura

**Números:** 1 pacote (`br.com.marealta`, 24 classes, 902 linhas), 0 dependências entre pacotes, sem ciclo. Esse único pacote usa o Javalin, o JDBC, o Jackson e o pool. A maior classe é `Api.java`, com 239 linhas, seguida de `Repository.java` (173) e `Service.java` (158).
**Nota:** com problemas

As camadas existem como classes, mas não como pacotes:
- **A borda HTTP é fina.** Cada rota tem uma linha que lê, chama o serviço e serializa (`Api.java:33-41`). A validação de entrada e a montagem do JSON também ficam no `Api` (`Api.java:72-238`), que é o lugar certo para elas.
- **As regras não conhecem o Javalin.** O `Service` não importa nada de `io.javalin`.
- **A montagem fica num lugar só.** É o `Main.java:9-13`, e o `Db` é injetado no `Service` (`Service.java:12`).

Pontos que pesam:
- **Há SQL dentro do `Service`.** Os relatórios mandam ao `Repository` pedaços de `WHERE ... ORDER BY` escritos no próprio serviço (`Service.java:110-112` e `Service.java:128-130`). O `Repository.reservations` aceita qualquer texto SQL (`Repository.java:111-112`). Com isso, uma regra de negócio fica escrita em SQL dentro do serviço: quais situações contam como "paga" e o que conta como "dinheiro que ficou". O `Service` também recebe `java.sql.Connection` diretamente (`Service.java:144`, `Service.java:149`).
- **O `Repository` é uma coleção de métodos `static` (`Repository.java:14-15`).** O `Service` chama o `Repository` direto, sem receber uma instância. Num sistema deste tamanho isso funciona, mas o acesso aos dados fica preso ao serviço.
- **Tudo está num pacote só, e quase tudo é package-private.** Nada impede o `Api` de chamar o `Repository`. Hoje a separação depende só de disciplina. O arquivo `Domain.java` junta seis tipos de primeiro nível (`Domain.java:6-49`).

## Design de baixo nível

**Números:**
- **Escolha do documento:** um único `switch` decide qual documento criar, `Documents.render` (`Documents.java:19-25`), com 3 de 3 casos dentro dele e 0 fora.
- **Lugares que emitem documento:** os três lugares que o texto pede passam pela fábrica:
  - a confirmação/e-mail (`Service.java:61`);
  - o check-out (`Service.java:93`, que chama `issue`, em `Service.java:151`);
  - a segunda via (`Service.java:104`, que também chama `issue`).
- **Outros lugares que dependem do tipo de identificação:** três, nenhum deles escolhe documento:
  - `Repository.java:71-75`, um `switch` para gravar os campos;
  - `Repository.java:124-130`, um `if` para ler os campos;
  - `Api.java:192-198`, o `instanceof` que monta o JSON.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente usou a forma de fábrica concentrada, que a referência aceita como acerto. `Documents.render` recebe a reserva e o quarto e escolhe, pelo tipo selado `Identification` (`Domain.java:22-26`), o documento a criar: `receipt`, `invoice` ou `bookingReceipt` (`Documents.java:21-23`). Os três lugares do texto pedem o documento a ela e nunca criam um documento à mão:
- a confirmação, `Service.java:61`;
- o check-out e a segunda via, por `issue`, em `Service.java:93`, `Service.java:104` e `Service.java:151`.

O e-mail é sempre em português e só embrulha o texto do documento (`Documents.java:75-81`). O comprovante e a fatura compartilham as linhas comuns em `portugueseLines` (`Documents.java:43-54`), sem cópia. O `switch` sobre a interface selada obriga o compilador a cobrar um caso novo quando a agência chegar (o comentário em `Documents.java:8-10` diz isso, e o código confirma). O que afasta um pouco da forma ideal:
- **A agência vai exigir mudanças em quatro lugares.** Além da fábrica, terão de mudar a gravação, a leitura e o JSON da identificação (`Repository.java:71-75`, `Repository.java:124-130` e `Api.java:192-198`).
- **A leitura cai em `Passport` sem avisar.** `Repository.java:124-130` é uma cadeia de `if` que, para qualquer linha sem CPF nem CNPJ, devolve `Passport` por padrão (`Repository.java:128-129`). Um tipo novo gravado no banco seria lido como passaporte com `null`, sem erro de compilação.

**O exagero.** Não há estrutura sem necessidade. A única interface própria, `Db.Work` (`Db.java:16`), é a função que a transação executa. Não há fábricas, builders nem camadas que só repassam.

**O resto do código.** Os tipos estão certos:
- `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:13-15`, `Pricing.java:28-40`);
- `LocalDate` e `YearMonth` para datas;
- enums para quarto, plano e situação (`Domain.java:6-17`);
- records imutáveis.

Os métodos são curtos e os nomes dizem o que fazem. Defeitos localizados:
- **A regra de "reserva paga" está escrita duas vezes.** Ela existe em `Status.isPaid` (`Domain.java:14-16`) e de novo como texto SQL no relatório de ocupação (`Service.java:111`).
- **O "dinheiro que ficou" também está calculado duas vezes:** no SQL (`Service.java:129`) e em Java (`Service.java:134`).
- **O canal da mensagem é texto solto.** O `"EMAIL"` aparece como literal (`Service.java:62`), embora o texto diga que outros canais virão.
- **Os campos do `ApiException` não são privados** (`Domain.java:38-39`).
- **O tratamento genérico de erros só imprime a pilha.** Ele faz `e.printStackTrace()` (`Api.java:28-31`) e não passa pelo log do slf4j, que o projeto já tem.
- **O relatório de ocupação busca o quarto de cada linha numa lista em memória** (`Service.java:116`). Com cinco quartos, isso não pesa.

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados. Há 8 métodos de teste escritos, todos em `DomainTest`. A cobertura é de 76 de 450 linhas (16,9%) e de 22 de 158 ramos (13,9%). Não há testes pela API nem contra o banco, e não há mocks.
**Nota:** com problemas

O que existe é bom e confere valores exatos:
- **Os limites da alta temporada:** 14/12, 15/12, 29/02, 01/03 e 31/07 (`DomainTest.java:21-27`).
- **O exemplo do texto:** R$ 599,76 (`DomainTest.java:30-35`).
- **O desconto com meio para o par:** 89,96 e 509,80 (`DomainTest.java:38-44`).
- **Os formatos de dinheiro:** `R$ 1.234,56` e `BRL 1,234.56` (`DomainTest.java:47-51`).
- **A parte nova, os três documentos com o texto inteiro:**
  - o comprovante (`DomainTest.java:54-64`);
  - a fatura com o ISS de R$ 38,99 (`DomainTest.java:67-79`);
  - o booking receipt de uma noite com desconto (`DomainTest.java:82-93`).
- **O e-mail que embrulha o documento** (`DomainTest.java:96-103`).

Pela pergunta da mutação, trocar o ISS de 0,05 (`Pricing.java:12`), apagar a linha `Discount` (`Documents.java:65`) ou trocar `"1 night"` (`Documents.java:63`) quebraria algum teste. Ficam sem rede:
- **A regra de "exatamente um jeito de se identificar"** (`Api.java:100-120`). Se alguém trocar `!= 1` por `> 1`, ou apagar a exigência de CNPJ junto com a razão social, nenhum teste falha.
- **As situações da reserva e as transições recusadas** (`Service.java:54-95`, `Service.java:154-157`).
- **A devolução no cancelamento** (`Service.java:72-73`). Trocar `RatePlan.STANDARD` por `NON_REFUNDABLE` passa sem falha.
- **A recusa de quarto ocupado e de capacidade** (`Service.java:34-40`, `Repository.java:47-58`).
- **Os dois relatórios** (`Service.java:108-142`).
- **Os códigos de erro da API.**
- **Que os três lugares (confirmação, check-out e segunda via) usam o mesmo documento.**

Nada exercita o banco nem a API. Na construção o texto não pede testes. A iniciativa existe e é de boa qualidade, mas deixa a maior parte das regras sem conferir.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(12,2)` nas 6 colunas e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice (`reservations_room_dates`). O script acusou:
- `CHECK` em nenhuma das colunas de situação e tipo (`status`, `rate_plan`, `type`, `channel`);
- nenhuma restrição `UNIQUE`, `EXCLUDE` ou `CHECK` que proteja uma regra;
- o esquema criado em `CREATE TABLE` no Java, com 0 arquivos de esquema e sem Flyway;
- 2 trechos de SQL montados com `+`. Os dois juntam só constantes.
**Nota:** com problemas

O que está certo:
- **Tipos e chaves.** Os tipos estão certos (`Db.java:61-95`). Há `NOT NULL` no que é obrigatório e chave estrangeira do quarto (`Db.java:69`), com índice composto que começa por ela (`Db.java:88`).
- **Inicialização segura.** A criação roda numa transação sob `pg_advisory_xact_lock` (`Db.java:59`), o que a protege de duas instâncias subindo ao mesmo tempo. Os quartos entram com `ON CONFLICT DO NOTHING` (`Db.java:96-101`).
- **Todo SQL usa parâmetros.** Os trechos com `+` juntam só constantes do código (`Repository.java:34`, `Repository.java:102`, `Repository.java:112`). Neste último, o `whereAndOrder` vem de literais do `Service` (`Service.java:110-112`, `Service.java:128-130`).
- **Conexões e transações.** As conexões vêm de um pool HikariCP (`Db.java:19-25`) e são devolvidas por try-with-resources (`Db.java:41`). Cada operação roda numa transação com rollback (`Db.java:40-54`).
- **A duplicação de reserva fica protegida no banco, não só no processo.** O `create` trava a linha do quarto com `FOR UPDATE` (`Service.java:32`, `Repository.java:34`) antes de conferir a sobreposição (`Repository.java:47-58`). As operações sobre a reserva também travam a linha (`Service.java:56` e seguintes). O roteiro concorrente passou nas 10 rodadas.

Os defeitos:
- **O esquema não tem como evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Db.java:60-95`), sem migrações versionadas. Quando a agência de turismo precisar de colunas novas, esse código não vai acrescentá-las numa tabela que já existe.
- **O banco não protege nenhuma regra sozinho:**
  - não há `CHECK` sobre `status`, `rate_plan` e `type`;
  - nada garante que exatamente um conjunto de colunas de identificação esteja preenchido (`Db.java:72-76`). Isso pesa junto com a leitura que cai em `Passport` por padrão (`Repository.java:128-129`);
  - não há `EXCLUDE` contra sobreposição. A regra depende de todo código que grava passar pela trava do quarto.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o texto descreve funcionou pela API, a partir de um banco vazio:
- os preços com temporada e arredondamento;
- as recusas com os códigos certos;
- a confirmação com e-mail, o check-in, o check-out e os cancelamentos com as devoluções certas;
- os dois relatórios em CSV;
- os dados, que continuam depois de reiniciar.

Na parte nova:
- A reserva aceita exatamente um jeito de identificação e recusa nenhum, dois ou um incompleto.
- A fatura mostra a empresa e o ISS de R$ 38,99.
- O booking receipt sai em inglês, com `BRL 2,783.52`, datas ISO, "1 night" e a linha `Discount`.
- O e-mail continua em português com o documento certo dentro.
- O check-out e a segunda via devolvem o mesmo documento, com o campo `text` e os cinco campos de identificação.

Em dez rodadas de reservas simultâneas, nunca saíram duas para o mesmo quarto. Para o dono da pousada, o sistema faz o que ele pediu.

## Dúvidas

- **A forma do pattern.** É uma fábrica concentrada (`Documents.render`, um `switch` sobre a interface selada), não o Factory Method clássico com um criador sobrescrito por tipo. A referência aceita as duas como acerto.
- **Pacote único e "tudo num pacote".** O critério de `ruim` na arquitetura fala em "tudo num pacote". Dei `com problemas` porque as camadas existem como classes separadas: a borda fina, o serviço sem Javalin e o SQL quase todo no `Repository`. A falha está na fronteira, não na ausência de camadas.
- **A nota do design.** Os defeitos são pequenos e localizados (a regra de "paga" e o "dinheiro que ficou" escritos duas vezes, a leitura que cai em `Passport` por padrão). Um revisor menos exigente daria `bom`.
