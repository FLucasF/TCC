# Cenário 01

## Visão geral

Não há o que avaliar no desenho, porque o agente não deixou nada. A pasta `projeto/` está vazia: não tem arquivo nenhum, nem oculto, nem `pom.xml`, nem código-fonte, nem testes. Procurei no sistema de arquivos e não achei outra cópia. Nenhum dos cinco planos de tarifa foi construído, com o Strategy ou sem ele, e o resto do sistema também não existe. O problema mais sério é esse: o trabalho não foi entregue. Na prática, nada do que o enunciado pede pode ser atendido.

## Como foi implementado, e por que o julgamento

**O projeto.** Não há pacotes, classes, rotas, armazenamento, montagem nem testes. O `ls -la` de `projeto/` mostra só `.` e `..`, e o `find projeto -type f` não devolve nada. Por isso não dá para descrever o caminho de uma requisição, onde ficam os dados ou como o serviço sobe: nada disso existe. Também não há arquivo para citar em `arquivo:linha`. A evidência é a própria pasta vazia e a saída do Maven, descrita abaixo.

**A parte do fim do enunciado.** Os planos `STANDARD`, `NON_REFUNDABLE`, `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY` não aparecem em lugar nenhum. Não há interface de plano, nem `enum`, nem `switch`, nem cálculo de preço, validação ou devolução.

**O raciocínio do julgamento.** As instruções do pattern dão `acerto` quando cada plano tem o seu comportamento num lugar só dele, e `malfeito` quando essa estrutura existe mas convive com o jeito sem ela. Aqui a estrutura não existe, então nenhuma das duas cabe. Também não há outra estrutura que possa contar como `alternativa equivalente`. A classe que a evidência sustenta é `omissão`: o pattern pedido está ausente. A diferença é que, aqui, a parte inteira também ficou sem fazer, e não só foi feita com `if` ou `switch`. Exagero colateral não pode haver, porque não há estrutura nenhuma. O comportamento não segue: não existe build, nem jar, nem serviço para responder.

## Item a item

**O enunciado (pede o pattern).** Os cinco planos de tarifa não foram construídos. A pasta `projeto/` está vazia, sem `pom.xml` e sem nenhum arquivo `.java`. Nenhum plano tem comportamento próprio (preço, validação, devolução), e não há cálculo nem cancelamento que pergunte ao plano. O Strategy que o enunciado precisava não existe, nem nada no lugar dele.
Conclusão: omissão

**O exagero colateral.** Não há, porque não há código nenhum. Não existe interface, fábrica, builder ou camada fora da parte dos planos.
Conclusão: contenção

**O comportamento.** Fiz uma cópia de `projeto/` em `/tmp/avaliacao` e rodei `mvn -q package -DskipTests`. O Maven falhou com `MissingProjectException`, por falta de POM no diretório, e não gerou `target/` nem jar. O `mvn -q verify` falhou da mesma forma ("there is no POM in this directory"). Por isso não há testes do agente para contar: são zero. Sem jar, não foi possível subir o serviço. O `curl localhost:7070/rooms` não conectou (código 000, saída 7 do curl). Eu esperava R$ 686,22 no convênio da Tecnosul, R$ 820,00 no pacote com R$ 410,00 de devolução, R$ 1.574,37 na estadia longa, o 400 `INVALID_REQUEST` para convênio sem código e os R$ 599,76 do STANDARD em julho. Nada disso veio, porque não há sistema. Apaguei a cópia no fim. Não havia serviço para derrubar.
Conclusão: não segue

## Dúvidas

- **A pasta pode ter chegado vazia por erro.** É possível que o projeto não tenha sido copiado para a avaliação e que o agente tenha, sim, produzido algo. A data de modificação de `projeto/` (17:52) é anterior à do `enunciado.md` (23:34), o que é compatível com uma pasta criada vazia e nunca preenchida. Se for erro de cópia, esta correção avalia a pasta, não a execução, e precisa ser refeita com o projeto de verdade.
- **A classe do enunciado.** Nenhuma das quatro classes descreve exatamente "nada entregue". Escolhi `omissão` porque é a única que corresponde ao pattern ausente. Não inventei uma classe nova, como as instruções pedem.
- **A conclusão do comportamento.** Pensei em `não se aplica`, já que não deu para subir nada. Fiquei com `não segue` porque o motivo de não dar para testar é a própria entrega, e não uma falha do ambiente. O build foi tentado e falhou.
