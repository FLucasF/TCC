# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`pousada`, 23 classes, 981 linhas), com as camadas separadas por classe, não por pacote. Uma requisição entra em `Api` (Javalin), que lê e valida o JSON (`Api.java:69-117`) e chama o `ReservationService`. O serviço abre uma transação em `Database.inTransaction` (`Database.java:29-43`) e chama as funções estáticas de `Store`, onde mora todo o SQL. Os dados ficam no PostgreSQL, em duas tabelas (`reservations` e `messages`) que `Database.java:45-83` cria com `CREATE TABLE IF NOT EXISTS`. O catálogo de quartos fica fixo no código (`Room.java:7-12`). A variação pedida (o documento por tipo de hóspede) foi resolvida com um `sealed interface GuestIdentity` e uma fábrica concentrada, `Documents.issue`, que os três lugares chamam. O roteiro de comportamento passou inteiro (83 de 83), e os 10 testes passam com 95,9% de cobertura de linhas. Nada é grave. Os pontos fracos são estes: o esquema é criado no código, sem migrações e sem chave estrangeira nem `CHECK`; tudo fica num pacote só; e o serviço trabalha com `Connection` do JDBC e com códigos HTTP.

## Arquitetura

**Números:** 1 pacote (`pousada`, 23 classes, 981 linhas), 0 dependências entre pacotes, nenhum ciclo. Esse pacote único usa Javalin, JDBC, Jackson e o pool (HikariCP). As maiores classes são `Api` (240 linhas), `Store` (175) e `ReservationService` (122).
**Nota:** com problemas

- **O que está bom:**
  - **A borda HTTP é fina.** Cada rota é uma linha que lê, chama o serviço e escreve a resposta (`Api.java:40-63`).
  - **O SQL fica todo em `Store`**, e o DDL em `Database`.
  - **As regras de preço ficam fora do Javalin e do JDBC.** Estão em `Quote.java:10-31` e em `RatePlan`, e a montagem do documento em `Documents.java`.
  - **A montagem acontece num lugar só:** `Main.java:6-11`.
  - **Nenhuma classe domina o projeto.**
- **O que mistura as camadas:**
  - **O serviço conhece o JDBC.** `ReservationService.java:105-106` recebe `java.sql.Connection` e declara `SQLException`, e todos os métodos montam lambdas sobre a conexão (`ReservationService.java:24-32`, `40-47`).
  - **O `Store` é só um conjunto de métodos `static`** (`Store.java:15-16`). Por isso o serviço não pode trocar o acesso a dados, e as regras de estado (`ReservationService.java:39-82`) não podem ser exercitadas sem banco.
  - **O serviço lança exceções com código HTTP.** Status 404, 409 e 422 aparecem dentro dele (`ReservationService.java:18`, `20`, `27`, `120`; `ApiException.java:3-19`). A regra conhece o protocolo da borda.
  - **Uma regra de negócio mora na borda HTTP.** A regra "exatamente uma identificação" está em `Api.java:98-117`.
- **Um pacote só, sem fronteiras.** Com 23 classes nele, nada impede que a próxima mudança faça tudo depender de tudo.

Num sistema deste tamanho, nada disso é sério. Mas é mais do que um revisor exigente aceitaria como `bom`.

## Design de baixo nível

**Números:** há 3 tipos de documento, os 3 escolhidos num único `switch` exaustivo sobre um tipo selado (`Documents.java:19-34`). Os 3 lugares que emitem documento chamam essa fábrica, e nenhum cria o documento à mão: a confirmação (`ReservationService.java:45`), o check-out (`ReservationService.java:80`) e a segunda via (`ReservationService.java:90`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha de qual documento criar mora num lugar só, `Documents.issue` (`Documents.java:15-37`). A referência aceita essa forma de antemão como fábrica concentrada.

- **O que aproxima:**
  - **O `switch` é exaustivo** sobre o `sealed interface GuestIdentity` (`GuestIdentity.java:7-12`). Quando entrar a agência de turismo, o compilador aponta o `case` que falta.
  - **O e-mail não decide nada.** Ele recebe o documento pronto e só o embute (`Documents.java:71-77`).
  - **O JSON do documento é o mesmo para os três tipos** (`Api.java:213-229`).
- **O que afasta (pouco):**
  - **O caso da fatura entra por uma flag.** O método comum `portuguese(..., boolean iss)` acrescenta a linha do ISS por um parâmetro booleano (`Documents.java:39`, `49`). Um quarto documento em português, com uma linha própria, pediria outra flag.
  - **O texto inglês repete a estrutura do português** (`Documents.java:53-64` contra `39-51`).
  - **Há perguntas de tipo fora da fábrica, mas nenhuma escolhe documento.** Os acessores `default` do `GuestIdentity` são uma cadeia de `instanceof` (`GuestIdentity.java:14-22`), e há mais dois `if` sobre a identidade: a leitura do banco (`Store.java:149-156`) e a da requisição (`Api.java:104-116`). Esses três lugares tratam de serializar e desserializar a identidade, não de escolher o documento.

**O exagero.** Não há estrutura sem necessidade. `Database.TxWork` (`Database.java:25-27`) existe só porque a lambda precisa lançar `SQLException`. `BookingDocument` é um record de dados. Não há interfaces de uma implementação nem camadas que só repassam.

**O resto do código.**

- **Os tipos são os certos:**
  - `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:15-17`; `Quote.java:14`, `17-18`, `23`);
  - `LocalDate` para as datas;
  - `enum` para o plano, a situação e o tipo de quarto.
- **Não há estado mutável global,** e os records são imutáveis.
- **Os métodos são curtos.**
- **Os erros não são engolidos.** `Database.java:36-41` faz o rollback e relança, e `Api.java:35-38` registra a pilha antes de responder 500.
- **Ressalvas pequenas:**
  - `ReservationService.with` copia à mão os 16 campos do record (`ReservationService.java:112-117`).
  - `Reservation.room()` busca o quarto no catálogo a cada chamada (`Reservation.java:24-26`).
  - Há nomes totalmente qualificados no meio do código (`Api.java:107`, `ReservationService.java:105`, `112`).

Nada disso dificulta mudar o código com segurança.

## Testes

**Números:** 10 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 10 métodos de teste escritos: 6 de unidade em `DomainTest` e 4 pela API, contra o PostgreSQL, em `ApiIntegrationTest`. A cobertura é de 440 de 459 linhas (95,9%) e 105 de 143 ramos (73,4%).
**Nota:** bom

Os testes conferem as regras com valores, e a parte nova do cenário está testada.

- **O que está testado:**
  - **O preço:** a alta temporada com o exemplo do texto, 599.76, e a borda de 14 para 15/12, 549.78 (`DomainTest.java:17-25`).
  - **O desconto com meio para o par:** 116.97 e 662.83 (`DomainTest.java:28-34`).
  - **Os dois formatos de dinheiro** (`DomainTest.java:37-40`).
  - **Os três documentos, comparados linha a linha:**
    - o comprovante (`DomainTest.java:43-53`);
    - a fatura com o ISS de R$ 38,99 (`DomainTest.java:56-68`);
    - o *booking receipt* de uma noite, com desconto (`DomainTest.java:71-82`).
  - **Pela API** (`ApiIntegrationTest.java:159-266`):
    - os códigos de erro;
    - duas identificações juntas, e o CNPJ sem a razão social;
    - o ciclo inteiro de uma reserva, com o e-mail;
    - a ocupação e o financeiro, com o CSV exato;
    - os três reembolsos;
    - o e-mail em português que leva o *booking receipt*;
    - os campos `null` no documento.
- **A pergunta da mutação na parte nova:**
  - **Trocar o ISS de 0.05:** o teste falha (`DomainTest.java:67`, `ApiIntegrationTest.java:230`).
  - **Trocar "night"/"nights":** o teste falha (`DomainTest.java:78`).
  - **Trocar o `switch` de `Documents.issue`:** o teste falha.
- **O que ficaria sem rede:**
  - **Os textos em português de uma noite e com desconto.** Ninguém confere "1 diária" nem a linha "Desconto:" no comprovante e na fatura (`Documents.java:45`, `47`).
  - **A recusa de um pedido sem nenhuma identificação**, e a do passaporte sem o país.
  - **Duas reservas ao mesmo tempo.**
- **Uma observação de ambiente:** os testes de API se desligam sem `DB_URL` (`ApiIntegrationTest.java:117`). Então um `mvn verify` sem banco passa só com os 6 testes de unidade.

São lacunas pequenas perto do que está conferido.

## Banco

**Números:**

- 2 tabelas, `reservations` (20 colunas, 14 `NOT NULL`) e `messages` (5 colunas), ambas com chave primária.
- O esquema é criado por `CREATE TABLE` no código (`Database.java`), com 0 arquivos de migração.
- O dinheiro está em `numeric(12,2)` nas 5 colunas.
- 0 chaves estrangeiras.
- Nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`: `status`, `rate_plan` e `channel` ficam em `text` sem `CHECK`.
- Os 4 candidatos a SQL montado com `+` só concatenam constantes.

**Nota:** com problemas

- **O que está bom:**
  - **Os tipos estão certos:** `numeric(12,2)` para o dinheiro e `date` para as datas (`Database.java:60-70`).
  - **O SQL é todo parametrizado.** Os `+` em `Store.java:82`, `104`, `110` e `114` juntam só constantes do código.
  - **As conexões vêm de um pool** (HikariCP, `Database.java:15-21`) e são fechadas com try-with-resources.
  - **Toda operação roda numa transação** (`Database.java:29-43`).
  - **A confirmação grava o novo estado e o e-mail na mesma transação** (`ReservationService.java:40-47`).
  - **A reserva dupla é impedida por uma trava do próprio PostgreSQL.** `pg_advisory_xact_lock` por quarto, dentro da transação, antes da conferência de sobreposição (`Store.java:24-44`, `ReservationService.java:25-28`). Ela vale entre processos, não só dentro de um.
  - **As mudanças de estado travam a linha** com `FOR UPDATE` (`Store.java:82`, `ReservationService.java:107`).
  - **Os relatórios filtram no SQL** (`Store.java:102-111`).
- **Os defeitos:**
  - **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:49-80`), sem migrações versionadas. O próximo pedido que precise de uma coluna nova (a agência de turismo, por exemplo) não tem como alterar uma tabela que já existe.
  - **Não há tabela de quartos.** `room_number INT NOT NULL` não aponta para nada (`Database.java:52`).
  - **O banco não protege nenhuma regra:**
    - nenhum `CHECK` para `status` e `rate_plan`;
    - nenhum `CHECK` que garanta exatamente uma identificação: as cinco colunas de identidade são todas anuláveis (`Database.java:55-59`), e `Store.read` cai no caso estrangeiro para qualquer linha sem CPF nem CNPJ (`Store.java:154-156`);
    - nenhuma restrição `EXCLUDE` contra a sobreposição de noites, que fica só por conta da trava.

## Comportamento

**Números:** 83 de 83 conferências passaram, e nenhuma falhou.

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2, com só uma criada em cada uma das 10 rodadas.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto descreve:

- **O preço:** a temporada, o desconto com meio para o par e as recusas com os códigos pedidos.
- **A reserva e o dinheiro:** o ciclo PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, as devoluções e os dois relatórios em CSV.
- **Os dados** continuam lá depois de reiniciar.
- **A parte nova:**
  - quem tem CPF recebe o comprovante de sempre;
  - a empresa recebe a fatura com o ISS de R$ 38,99;
  - o estrangeiro recebe o *booking receipt* em inglês, com `BRL 2,783.52`, as datas `2027-03-10` e "1 night";
  - o documento sai no e-mail em português, no check-out e na segunda via;
  - as combinações erradas de identificação são recusadas com 400.

Os defeitos de banco apontados acima não aparecem em nenhuma chamada que o texto descreve.

## Dúvidas

- **A forma do Factory Method.** A forma usada é a fábrica concentrada: um método estático, `Documents.issue`, com `switch` sobre um tipo selado. Não é a forma clássica, com um criador por tipo, nem um método polimórfico em `GuestIdentity`. A referência aceita a fábrica concentrada como acerto.
- **A flag do ISS.** O `boolean iss` em `Documents.java:39` deixa parte da diferença entre comprovante e fatura num parâmetro, e não no caso do `switch`. Pesei isso como detalhe, não como malfeito, porque a escolha do documento continua num lugar só.
