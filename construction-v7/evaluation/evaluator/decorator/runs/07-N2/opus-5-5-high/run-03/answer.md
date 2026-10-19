# Cenário 01

## Visão geral

Não há o que avaliar no desenho: a pasta `projeto/` está vazia. Não tem arquivo nenhum, nem oculto, e nenhum `pom.xml` ou `.java` em outro lugar da máquina. O agente não deixou sistema de reservas, nem a parte das regras de envio de mensagens que o fim do enunciado pede. O problema mais sério é exatamente esse: nada foi entregue. As três regras pedidas (bloquear endereço inválido, cópia para o arquivo, desvio no modo de teste) não existem em forma nenhuma, nem com Decorator nem sem ele.

## Como foi implementado, e por que o julgamento

O projeto não existe. `find projeto -mindepth 1` não devolve nada. O `ls -la projeto` mostra só `.` e `..`, e a pasta não muda desde 18/10 às 17:52. Procurei `pom.xml` e arquivos com "pousada" ou "reserv" no nome pelo sistema de arquivos inteiro e não achei nada. Por isso não há pacotes, classes, rotas, armazenamento, montagem nem testes para descrever. Também não há como citar `arquivo:linha`, porque não existe arquivo.

Sobre a parte do fim do enunciado: as instruções do pattern esperam uma interface de envio, o envio real e um embrulho por regra (endereço válido, cópia para `arquivo@`, desvio para `testes@`), empilhados na montagem conforme o `HOTEL_TEST_MODE`. Não existe nada disso, nem mesmo o jeito sem o pattern (os `if` dentro do envio).

Raciocínio do julgamento: das quatro classes, `acerto`, `alternativa equivalente` e `malfeito` exigem que exista alguma estrutura, e ela não existe. Sobra `omissão`, que pela definição é "a parte foi construída sem o pattern". Aqui a parte nem foi construída. É o caso extremo da omissão: o pattern não está lá. Sobre a escolha da classe, veja "Dúvidas". No exagero colateral, sem código não há estrutura sem necessidade. No comportamento, não houve como subir o serviço nem chamar a API.

## Item a item

**O enunciado (pede o pattern).** O enunciado pede que as regras de envio fiquem num Decorator (ou numa corrente ou lista de filtros montada por modo). O agente não entregou código nenhum: `projeto/` está vazio, então não há onde citar `arquivo:linha`. Não há interface de envio, embrulho, validação de endereço, cópia para o arquivo nem modo de teste. O pattern que o enunciado precisava não foi aplicado, e a parte também não foi feita de outro jeito.
Conclusão: omissão

**O exagero colateral.** Não há. Sem nenhum arquivo, não existe interface, fábrica, builder ou camada sobrando fora da parte do fim do enunciado.
Conclusão: contenção

**O comportamento.** Copiei o `projeto/` vazio para `/tmp/copia` e rodei `mvn -q package -DskipTests`. Ele falhou com "The goal you specified requires a project to execute but there is no POM in this directory (MissingProjectException)". O `mvn -q verify` falhou do mesmo jeito, e não há testes do agente para contar. Como não houve build, não houve jar, e não subi o serviço em nenhum dos dois modos. O `curl localhost:7070/rooms` não conectou (curl saiu com código 7). Não dá para conferir nada: nem as regras do fim (`BLOCKED`, a cópia `[Cópia] `, o desvio `[TESTE] ` com "Destinatário original: …", o campo `status`), nem a parte comum (o preço, as recusas, o comprovante). Como não houve serviço para levantar, não precisei derrubar nada.
Conclusão: não se aplica

## Dúvidas

A dúvida é a classe do primeiro item. As quatro classes partem do princípio de que a parte foi construída. Aqui não existe código nenhum, e a definição literal de `omissão` ("construída sem o pattern") não cabe perfeitamente. Escolhi `omissão` porque é a única que a evidência sustenta: nenhuma estrutura do pattern. Mas o caso é melhor descrito como "entrega vazia". Vale verificar se a pasta foi copiada errado para a avaliação, ou se a execução do agente terminou mesmo sem escrever nada. A pasta estar intacta desde 17:52 sugere que o agente não deixou nada nela.

No comportamento, `não se aplica` segue a regra de "não deu para testar". Na prática, porém, o sistema pedido não funciona, porque não existe. Quem somar os resultados talvez prefira contar isto como `não segue`.
