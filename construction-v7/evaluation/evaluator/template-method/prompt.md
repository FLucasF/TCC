# As instruções do pattern: Template Method

Este avaliador cuida de um enunciado, que você reconhece pela parte do fim do `enunciado.md`: o dos **quatro
relatórios com título** (cenário 09, que pede o pattern).

## O que o enunciado descreve

Na parte comum, dois relatórios em CSV, a ocupação de uma noite e o financeiro de um mês, com os mesmos passos:
escolher as reservas, ordenar, escrever o cabeçalho, uma linha por reserva e a linha do total.

## Cenário 09: os quatro relatórios (pede o pattern)

O enunciado traz mais dois relatórios (a taxa de turismo e o relatório por plano de tarifa), o que faz quatro com os
mesmos passos, todos começando com uma linha de título, e diz que o contador vai pedir mais. A resposta certa é o
Template Method: o esqueleto (título, cabeçalho, linhas, total) num lugar só, e cada relatório preenchendo só os
passos que mudam.

É **acerto** uma classe abstrata com o método-modelo (o esqueleto, de preferência `final`) e os passos abstratos,
com os quatro relatórios como subclasses, a ocupação e o financeiro inclusive.

É **alternativa equivalente** a composição: um escritor de relatório genérico que recebe as partes que variam (o
nome, as colunas, a seleção, a linha, o total) como objetos ou funções, desde que os quatro relatórios passem por
ele.

É **omissão** quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título.

É **malfeito** quando o esqueleto existe só para uma parte dos relatórios e os outros ficam inteiros (com a linha de
título copiada neles), ou quando o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a
sequência dos passos.

O comportamento pedido: os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>" (os nomes são
`occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada); a taxa de turismo cobra R$ 4,50
por hóspede por noite das reservas pagas que começam no mês, na ordem do relatório financeiro, com as colunas
`reservation,room,check_in,check_out,guests,nights,tax` e o total no fim; o relatório por plano tem uma linha por
plano (`STANDARD` e depois `NON_REFUNDABLE`, mesmo sem reserva), com a quantidade e o dinheiro que ficou com a
pousada, contando as mesmas reservas do relatório financeiro (inclusive uma não reembolsável cancelada), e o total
no fim; depois do título, a ocupação e o financeiro são como a parte comum descreve.
