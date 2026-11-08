# Revisão: construção, cenário 01

## Visão geral

O agente não entregou nada. A pasta `projeto/` está vazia: `ls -la projeto` mostra só `.` e `..`, sem nenhum arquivo, nem escondido, e sem `pom.xml`, código-fonte, testes, migrações ou README. Então não há pacotes nem caminho de requisição para descrever, e os dados não ficam em lugar nenhum. Os cinco scripts rodaram até o fim (código de saída 0) e todos disseram a mesma coisa: "sem medida (não há pom.xml no projeto)", ou "não deu para testar". Não existe um sistema de reservas para o dono da pousada usar: nem a parte comum (quartos, preço com temporada, situações da reserva, comprovante, e-mail, relatórios), nem os três planos novos (CORPORATE, WEEKEND_PACKAGE, LONG_STAY). Esse é o defeito mais sério possível, e ele dá `ruim` em todas as colunas.

## Arquitetura

**Números:** não há medida. `measures/dependencies.md` diz "Resultado: sem medida (não há pom.xml no projeto)". São 0 pacotes, 0 dependências e 0 ciclos.
**Nota:** ruim

Não há código para avaliar. Não existe borda HTTP (Javalin), camada de regras, acesso a dados nem ponto de montagem. A skill dá `ruim` quando as camadas não existem, e aqui não existe nada: a coluna está ausente.

## Design de baixo nível

**Números:** dos cinco planos de tarifa, nenhum está numa estrutura, e nenhum foi implementado de outro jeito. Não há nenhum arquivo Java em `projeto/`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

O texto pede o Strategy sem dizer o nome. São cinco planos (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE e LONG_STAY), cada um com o seu preço, as suas regras de quem pode usar e a sua devolução, e o enunciado avisa que "o comercial vive inventando plano". Nenhum plano foi implementado, nem dentro de uma estrutura nem fora dela. Das quatro classes permitidas, `omissão` é a que melhor descreve a ausência total. Como não há estrutura nenhuma, não pode haver estrutura sem necessidade, por isso `contenção`. A nota é `ruim` porque não há código para avaliar quanto a duplicação, tipos (`BigDecimal`, `LocalDate`, `enum`), estado ou tratamento de erros.

## Testes

**Números:** não há medida. `measures/tests.md` diz "Resultado: sem medida (não há pom.xml no projeto)". Rodaram 0 testes e falharam 0, há 0 métodos de teste escritos, e a cobertura de linhas e de ramos não existe.
**Nota:** ruim

Não há testes. O enunciado pede que o `mvn verify` passe, mas sem `pom.xml` o comando nem roda. Nenhuma regra está conferida: preço com temporada, arredondamento meio para o par, planos, devoluções, códigos de erro.

## Banco

**Números:** não há medida. `measures/database.md` diz "Resultado: sem medida (não há pom.xml no projeto)". São 0 tabelas, nenhum esquema foi criado, e nenhuma conferência pôde rodar.
**Nota:** ruim

Não há serviço para subir contra o PostgreSQL vazio, nem esquema, migração ou código de acesso ao banco. O enunciado pede que os dados fiquem no PostgreSQL "para nada se perder quando o sistema reiniciar", e nada é guardado. A skill dá `ruim` quando o serviço não sobe contra um banco vazio, e aqui ele nem existe.

## Comportamento

**Números:** `measures/behavior.md` diz "Resultado: não deu para testar (não há pom.xml no projeto)". Passaram 0 conferências, e nenhuma das quatro partes do roteiro rodou: a parte comum, os dados depois de reiniciar, o cenário e as reservas simultâneas.
**Nota:** ruim

O serviço não existe, então não sobe. Para o dono da pousada, isso quer dizer que não há sistema: o site não consegue criar nem confirmar reservas, a recepção não faz check-in nem check-out e não consulta os e-mails enviados, e o contador não tira os relatórios de ocupação e financeiro. Os valores que o financeiro conferiu também não puderam ser verificados: R$ 686,22 no convênio Tecnosul, R$ 820,00 e R$ 410,00 no pacote, R$ 1.574,37 na estadia longa.

## Dúvidas

A classe do pattern, quando não há entrega nenhuma: as quatro classes permitidas supõem que exista algum código, e `omissão` foi escolhida por ser a que descreve a ausência da variação. Fora isso, nenhuma: a evidência (a pasta vazia e os cinco scripts sem medida) é a mesma para todas as colunas.
