# Proposta para o Resumo e a Conclusão

Nada disto foi aplicado ao artigo. A proposta usa só afirmações e números que já estão na v92.

## Resumo proposto

Este trabalho investiga como orientações persistentes, em arquivos de projeto e *skills*, alteram o uso de *design patterns* por agentes de programação baseados em LLMs. Em estudo pré-registrado, 11 modelos Claude construíram o mesmo serviço em quatro níveis de orientação (220 execuções): um arquivo de princípios elevou de 67% para 93% o acerto no ponto mais complexo do Strategy, sem alteração detectada no exagero e sem piora da correção no conjunto, embora o Haiku 4.5 tenha piorado. Em extensão exploratória (450 execuções), o uso correto aumentou, do nível sem orientação ao mais orientado, em nove de dez combinações de cinco *patterns* e duas tarefas, com exceção do Strategy na construção. Em 90 controles negativos na manutenção, 88 execuções mantiveram a mudança pequena. O ganho concentrou-se nos modelos que não acertavam sem orientação.

**O que saiu do Resumo e continua no corpo do artigo:** os números da construção e da manutenção por nível, a calibração do verificador, a complexidade cognitiva e as métricas de orientação a objetos.

## Conclusão proposta (Seção 8)

Este trabalho avaliou experimentalmente o efeito de orientações persistentes sobre a decisão de agentes de programação de aplicar *design patterns* não nomeados no pedido. No estudo principal, com pré-registro, um arquivo de princípios aumentou o uso correto do padrão Strategy, de 67% para 93% no ponto mais complexo, sem alteração detectada no exagero e sem piorar a correção no conjunto, embora com piora no Haiku 4.5. A complexidade cognitiva diminuiu, em parte em decorrência da própria troca de condicionais, e o ganho apareceu nos modelos que não acertavam sem orientação, os únicos com espaço para melhorar.

Na extensão, com cinco rodadas, observou-se, de N0 a N2, a mesma direção em nove das dez combinações de *pattern* e tarefa, com a exceção do Strategy na construção. O aumento foi maior na manutenção quando se somaram o arquivo e a *skill*, pois essa tarefa parte de um nível menor e se aproxima da construção; apenas com o arquivo, os aumentos foram próximos, e o texto do arquivo difere entre as tarefas. Nas demais dimensões de qualidade, o modelo e o ponto de partida pesaram mais que a orientação. Quanto aos custos, na construção com a *skill*, o exagero subiu de 4 para 9 execuções em 75, ao passo que, nos controles negativos da manutenção, a estrutura desnecessária foi rara: 88 de 90 execuções mantiveram a mudança pequena, e as duas exceções ocorreram no N2. As métricas de orientação a objetos, em sua maioria descritivas e analisadas depois dos resultados, são compatíveis com um mecanismo, que em parte apenas redescreve a mudança de desenho: a orientação troca condicionais por polimorfismo e distribui a complexidade por mais classes, com pouca mudança no acoplamento médio.

Como trabalhos futuros, indicam-se o estudo de outras famílias de modelos e de agentes; a leitura humana das notas do verificador; o controle negativo também na construção e com mais rodadas; e a medida do ganho do *pattern*, por meio da solicitação de um novo caso sobre o código gerado e da contagem dos lugares que precisam mudar e dos comportamentos antigos que se quebram.
