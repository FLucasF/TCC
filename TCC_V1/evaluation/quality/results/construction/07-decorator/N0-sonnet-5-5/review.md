# Revisão: construção, cenário 07

## Visão geral

O projeto tem um pacote só, `br.com.marealta`, com 9 arquivos e 1087 linhas. `Main` monta as peças (`Main.java:37-49`) e registra as rotas do Javalin, que leem o corpo, chamam o `Service` e escrevem o JSON ou o CSV (`Main.java:67-117`). O `Service` guarda as regras da reserva e também todo o SQL. Ele abre uma transação por operação com `Database.tx` (`Database.java:60-72`) e trava a linha do quarto com `FOR UPDATE` para não deixar duas reservas na mesma noite (`Service.java:90`). O preço (`Pricing`), o comprovante (`Receipt`) e as regras de envio (`Messaging`) são classes sem Javalin e sem JDBC. Os dados ficam no PostgreSQL, em três tabelas que o código cria com `CREATE TABLE IF NOT EXISTS` (`Database.java:79-118`). As regras de envio são uma lista de regras independentes, que a `Messaging.Pipeline` monta a partir da configuração por modo (`Messaging.java:70-86`). O roteiro passa inteiro: 65 de 65. Nada é grave. Os defeitos que precisam de conserto são três: o `Service` mistura regra e SQL, o esquema não tem migrações nem restrições, e há pequenas duplicações.

## Arquitetura

**Números:** 1 pacote (28 classes, contando as internas; 1087 linhas), 0 dependências entre pacotes, 0 ciclos. O mesmo pacote usa Javalin, JDBC, Jackson e o pool. Maiores classes: `Service` com 352 linhas, `Main` com 236 e `Messaging` com 154.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço (`Main.java:80-86`). A leitura e a validação do corpo ficam em `Main.parseNew` (`Main.java:166-197`), o que é aceitável para uma borda. O Javalin só aparece em `Main`.
- **A montagem fica num lugar só,** o `main` (`Main.java:37-44`). O `Clock` e a `Config` entram por parâmetro, e por isso os testes conseguem trocar o relógio (`ApiIT.java:229-240`). Há uma exceção: a `Pipeline` de mensagens é criada dentro do construtor do `Service` (`Service.java:43`), e não no `main`.
- **O problema principal é o `Service`.** O próprio comentário diz "Regras de negócio e acesso ao banco" (`Service.java:25`), e é isso mesmo.
  - A regra de devolução do cancelamento (`Service.java:196-200`) está colada ao `UPDATE` (`Service.java:201-206`).
  - As transições de situação ficam junto do SQL (`Service.java:157-168`).
  - O texto do e-mail é montado no meio da transação (`Service.java:183-187`).
  - Os relatórios juntam a consulta e a formatação do CSV (`Service.java:298-351`).
  - Não existe uma classe de acesso a dados separada. Quando o próximo pedido mexer numa regra da reserva, a mudança vai cair nesse arquivo, que já é o maior.
- **As outras regras estão bem separadas** (`Pricing`, `Receipt`, `Messaging`). Isso, e a ausência de ciclos, segura a nota em "com problemas" e não em "ruim". Mesmo assim, tudo num pacote só e regra com SQL num mesmo `Service` é uma mistura que alguém vai ter de desfazer.

## Design de baixo nível

**Números:** as três regras do texto (endereço inválido, cópia para o arquivo, desvio no modo de teste) estão todas na estrutura. As duas regras anunciadas (madrugada e limite diário) também estão. Fora dela não há nenhum `if` de regra de envio: o `Service.send` só chama `pipeline.process` (`Service.java:249`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não há embrulhos do envio com a mesma interface, como num Decorator. O que há é uma corrente de regras, a `Messaging.Rule` (`Messaging.java:43-45`), aplicada em ordem sobre a lista de mensagens que vão sair (`Messaging.java:88-96`). A referência aceita isso de antemão: cada regra é uma peça própria e a combinação por modo é decidida na montagem.

O que aproxima do certo:
- Cada regra é uma peça própria: `blockWhen` para endereço, madrugada e limite (`Messaging.java:113-123`), `archiveCopy` (`Messaging.java:125-136`) e `testRedirect` (`Messaging.java:138-149`).
- Nenhuma regra pergunta pelo modo de teste por dentro.
- A combinação por modo fica num lugar só, o construtor da `Pipeline` (`Messaging.java:70-86`). Ele lê um `Scope` por regra (`ALWAYS`, `TEST_ONLY`, `LIVE_ONLY`, `NEVER`; `Config.java:14-25`) com valores padrão (`Config.java:33-37`), e cada instalação pode trocá-los por variáveis de ambiente (`Config.java:42-51`). Isso resolve "a segunda pousada não quer a cópia": `HOTEL_RULE_ARCHIVE_COPY=NEVER`.

O que afasta do Decorator:
- A ordem das regras é fixa no construtor.
- As regras dependem dos marcadores `sent` e `copy` da `Outgoing` (`Messaging.java:117`, `129`, `142`). O bloqueio só funciona porque roda antes da cópia, e o desvio porque roda por último. O comentário em `Messaging.java:15-16` admite isso.
- As regras mudam a lista no lugar e mexem no índice dentro do laço (`Messaging.java:130-133`), o que é frágil.

**Exagero:** não há interface com uma implementação só, nem fábrica, nem camada que só repassa. `Database.Work` (`Database.java:13-15`) é a interface funcional da transação e tem uso real. As duas regras extras, com a sua configuração, ficam dentro da parte do pattern (veja as Dúvidas).

**O resto do código:**
- **Os tipos estão certos:** `BigDecimal` com `HALF_EVEN` a cada passo (`Pricing.java:48-50`, `39-45`), `LocalDate`, `YearMonth` e `enum` para situação, plano e tipo (`Domain.java:10-21`).
- **A formatação do dinheiro em reais** não depende do locale da JVM (`Receipt.java:40-52`).

Os defeitos, todos localizados:
- **Duplicação:** `confirm` e `cancel` refazem à mão o "carrega com trava, confere a situação, atualiza" (`Service.java:172-180`, `195-206`), em vez de usar o `transition` que já existe (`Service.java:157-168`).
- **Regra repetida:** a lista das situações pagas está no `Status.isPaid` (`Domain.java:18-20`) e de novo como texto no SQL do relatório de ocupação (`Service.java:305`).
- **Campos públicos:** a `ApiException` tem `status` e `code` públicos (`ApiException.java:4-5`).
- **Configuração inválida:** um valor errado nas variáveis `HOTEL_RULE_*` derruba a subida com um `IllegalArgumentException` sem mensagem clara (`Config.java:65-67`).

## Testes

**Números:** 20 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 20 métodos escritos: 13 em `DomainTest` e 7 em `ApiIT`, este contra um PostgreSQL embutido e pela API. Cobertura de 92,7% das linhas (486 de 524) e 82,0% dos ramos (164 de 200).
**Nota:** bom

- **A parte nova está testada com valores, nas duas pontas.**
  - Unidade: a validação do endereço, com dez endereços inválidos e dois válidos (`DomainTest.java:77-84`).
  - Bloqueio sem cópia nos dois modos, com o endereço original (`DomainTest.java:105-114`).
  - A cópia com "[Cópia] " e o mesmo texto (`DomainTest.java:95-103`).
  - O desvio com "[TESTE] " e "Destinatário original: …" (`DomainTest.java:116-123`).
  - A cópia desligada por configuração (`DomainTest.java:135-139`), a madrugada e o limite por modo (`DomainTest.java:141-164`).
  - Pela API: o e-mail inteiro, palavra por palavra, e a cópia logo depois (`ApiIT.java:290-311`), o bloqueio (`ApiIT.java:424-434`) e o modo de teste (`ApiIT.java:436-447`).
- **Pergunta da mutação na parte nova.** Trocar o prefixo "[Cópia] ", apagar a regra do endereço ou deixar a cópia sair no modo de teste quebraria testes.
- **A parte comum também está coberta com valores:** a temporada nas bordas (`DomainTest.java:23-31`), o exemplo de R$ 599,76 (`DomainTest.java:33-40`), as devoluções (`ApiIT.java:364-396`), os dois relatórios comparados texto a texto (`ApiIT.java:398-422`) e as recusas (`ApiIT.java:327-362`).
- **Lacunas:**
  - **O "meio para o par" não está garantido.** O teste que diz conferir isso usa 112,455, que dá 112,46 tanto com `HALF_EVEN` quanto com `HALF_UP` (`DomainTest.java:43-48`; o comentário está errado). Trocar o modo de arredondamento passaria sem nenhum teste falhar.
  - Não há teste das reservas simultâneas nem dos dados depois de reiniciar.
  - Muitas recusas conferem só o código HTTP, sem o `error` (`ApiIT.java:330-338`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric` com 2 casas nas 6 colunas. A única chave estrangeira tem índice e não há data guardada como texto. Há 0 restrições `CHECK`, `UNIQUE` ou `EXCLUDE`, e as 5 colunas de situação e tipo não têm `CHECK`. O esquema é criado por `CREATE TABLE` no Java, com 0 arquivos de migração.
**Nota:** com problemas

- **Os tipos estão certos.** Dinheiro em `NUMERIC(14,2)` e `NUMERIC(12,2)` (`Database.java:84`, `97-102`), datas em `DATE` (`Database.java:92-93`), a reserva ligada ao quarto por chave estrangeira (`Database.java:88`), com índice (`Database.java:104`), e `NOT NULL` no que é obrigatório.
- **O SQL usa parâmetros.** As duas concatenações acrescentam só a constante `" FOR UPDATE"` (`Service.java:74`, `137`).
- **As conexões são bem tratadas.** Saem do HikariCP e voltam em try-with-resources (`Database.java:61`), e cada operação roda numa transação só (`Database.java:60-72`). Por exemplo, a confirmação e a gravação das mensagens andam juntas (`Service.java:170-190`).
- **A reserva dupla é barrada no banco,** não só no processo: a trava `FOR UPDATE` na linha do quarto (`Service.java:72-74`, `90`) serializa as reservas do mesmo quarto, e o roteiro confirmou isso em 10 rodadas.
- **O esquema não tem como evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do código (`Database.java:79-118`), sem migrações versionadas, e esse comando não sabe acrescentar uma coluna numa tabela que já existe.
- **O banco não protege nenhuma regra sozinho.** Não há `EXCLUDE` contra a sobreposição de datas, nem `CHECK` em `status`, `rate_plan`, `guests > 0` ou `check_out > check_in`. Tudo depende do código.

## Comportamento

**Números:** 65 de 65 conferências passaram, e 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 07 no modo normal: 9 de 9;
- cenário 07 no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono da pousada descreveu funciona pela API:
- o preço com a alta temporada e o desconto arredondado "meio para o par" (R$ 37,48 numa noite não reembolsável);
- as recusas com os códigos certos;
- o ciclo da reserva, as devoluções e os dois relatórios;
- os dados continuam lá quando o sistema reinicia.

Nas regras dos e-mails:
- o endereço válido gera a mensagem e a cópia para o arquivo logo depois;
- os quatro endereços inválidos ficam `BLOCKED`, com o endereço original e sem cópia;
- no modo de teste vai uma mensagem só, para `testes@pousadamarealta.com.br`, com "[TESTE] " e a linha do destinatário original, e o endereço inválido continua bloqueado.

Nas dez rodadas de reservas simultâneas, nunca entraram duas.

## Dúvidas

- **As regras da madrugada e do limite diário.** O agente implementou as duas, desligadas por padrão (`Config.java:34`), com o fuso, o horário e o limite configuráveis. Também pôs uma trava por destinatário para o limite valer sob concorrência (`Service.java:237-241`) e as colunas `guest_address` e `copy` com índice (`Database.java:113-117`). O texto diz "já pediram", o que pode ser lido como pedido feito ou como pedido anunciado. Contei como parte da variação, por isso dei contenção. Se a leitura for que ainda não era para fazer, isso seria trabalho antecipado, embora pequeno e dentro da estrutura.
- **A classe do pattern.** Ficou entre alternativa equivalente e malfeito. Escolhi alternativa equivalente porque nenhuma regra decide o modo por dentro. Ainda assim, a dependência entre as regras pela ordem e pelos marcadores `sent` e `copy` (`Messaging.java:117`, `129`) as deixa menos independentes do que embrulhos de verdade.
