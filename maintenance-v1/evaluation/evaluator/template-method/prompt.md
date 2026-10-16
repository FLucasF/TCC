# As instruções do pattern: Template Method

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o dos **relatórios novos,
todos com título** (cenário 09, que pede o pattern) e o do **nome do hóspede no relatório de ocupação** (cenário
10, controle negativo).

## O que havia na base

Dois relatórios em CSV, `OccupancyReport` e `RevenueReport`, escritos cada um inteiro, com os mesmos passos:
escolher as reservas, ordenar, escrever o cabeçalho, uma linha por reserva e a linha do total. Com dois
relatórios, a repetição era aceitável.

## Cenário 09: os relatórios novos (pede o pattern)

O pedido traz dois relatórios novos (a taxa de turismo e o relatório por plano de tarifa), o que faz quatro
relatórios com os mesmos passos, e uma mudança que vale para todos (a linha de título no começo), e diz que o
contador vai pedir mais. A resposta certa é o Template Method: o esqueleto (título, cabeçalho, linhas, total)
num lugar só, e cada relatório preenchendo só os passos que mudam.

É **acerto** uma classe abstrata com o método-modelo (o esqueleto, de preferência `final`) e os passos
abstratos, com os quatro relatórios como subclasses, os dois antigos inclusive.

É **alternativa equivalente** a composição: um escritor de relatório genérico que recebe as partes que variam (o
nome, as colunas, a seleção, a linha, o total) como objetos ou funções, desde que os quatro relatórios passem por
ele.

É **omissão** quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título copiada.

É **malfeito** quando o esqueleto existe só para os dois novos e os dois antigos continuam inteiros (com a linha
de título copiada neles), ou quando o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a
sequência dos passos.

O comportamento pedido: os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>" (os nomes são
`occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada); a taxa de turismo cobra
R$ 4,50 por hóspede por noite das reservas pagas que começam no mês, na ordem do relatório financeiro, com as
colunas `reservation,room,check_in,check_out,guests,nights,tax` e o total no fim; o relatório por plano tem uma
linha por plano (`STANDARD` e depois `NON_REFUNDABLE`, mesmo sem reserva), com a quantidade e o dinheiro que ficou
com a pousada, contando as mesmas reservas do relatório financeiro (inclusive uma não reembolsável cancelada), e
o total no fim; depois do título, os dois relatórios antigos continuam iguais.

## Cenário 10: o nome do hóspede na ocupação (controle negativo)

O pedido acrescenta a coluna do nome do hóspede ao relatório de ocupação. A frase "talvez um dia a recepção peça o
telefone do hóspede também" é a isca.

É **contenção** uma coluna a mais no `OccupancyReport`, com a troca da vírgula por espaço no nome.

É **exagero** criar um esqueleto genérico de relatórios, um sistema de colunas configuráveis (uma lista de
objetos-coluna), ou mexer no relatório financeiro, que o pedido não toca.

O comportamento pedido: a coluna `guest` logo depois de `reservation`, com o nome como está na reserva e a
vírgula trocada por um espaço; o resto do relatório de ocupação e o relatório financeiro iguais.
