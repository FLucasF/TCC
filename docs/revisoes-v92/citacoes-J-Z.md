# Fidelidade das citações, referências de J a Z (artigo-v92)

Conferência feita em cada fonte salva em `leitura/` (`text/`, `livros/`). Os arquivos `vereditos/` não foram usados como base. A coluna "onde está" traz a página e um resumo em português, sem transcrever a fonte. As páginas são as do arquivo salvo, exceto em Prechelt, Vokáč e Zhang, em que se indica a página impressa.

## Contagem

| classificação | ocorrências |
|---|---|
| FIEL | 20 |
| PARCIAL | 6 |
| SEM SUPORTE | 0 |
| FONTE AUSENTE | 3 |
| **total** | **29** |

## Ocorrências

| # | seção | trecho do artigo | referência | classificação | onde está na fonte |
|---|---|---|---|---|---|
| 1 | 1 | LLMs baseados na arquitetura Transformer | Vaswani et al. 2017 | FIEL | p. 1: propõe a arquitetura Transformer. O artigo não trata de LLMs; o elo vem de Brown et al. (2020), que cita Vaswani ao falar de modelos de linguagem Transformer. |
| 2 | 1 | agente: entidade que percebe o ambiente, decide e age | Xi et al. 2025 | FIEL | Resumo, p. 1: entidades artificiais que percebem o ambiente, tomam decisões e agem. |
| 3 | 1 | ciclo de raciocinar, agir e observar o resultado | Yao et al. 2023 | FIEL | p. 1-2 e Figura 1: raciocínio e ações intercalados, com observação devolvida pelo ambiente. |
| 4 | 1 | o laço com retorno do ambiente distingue o agente de um LLM isolado | Liu et al. 2026 | FIEL | Resumo e p. 4 (critério de inclusão): LLM isolado não basta; o traço central é perceber retorno e agir de modo iterativo. |
| 5 | 1 | o efeito varia conforme o pattern e a tarefa | Prechelt et al. 2001 | FIEL | Resumo, p. 1134: efeitos positivos na maior parte das nove tarefas de manutenção e negativos em algumas. |
| 6 | 1 | o efeito varia conforme o pattern e a tarefa | Vokáč et al. 2004 | FIEL | p. 149-150 e 191: cada pattern tem natureza própria; resultados relatados por programa e tarefa. |
| 7 | 1 | ainda não há conhecimento suficiente para julgar quando usar cada um | Zhang e Budgen 2012 | PARCIAL | p. 1229: em 2012 os autores dizem estar longe desse conhecimento; a busca vai até o fim de 2009. "Ainda" estende o juízo ao presente. |
| 8 | 1 | efeito das regras é questão em aberto | Jiang e Nam 2026 | FIEL | p. 11, Discussão: o impacto real das regras no desempenho do LLM é declarado questão em aberto. |
| 9 | 2 | princípio vem de Meyer, que o resolve com herança | Meyer 1997 | FIEL | §3.3, p. 57 (módulos abertos e fechados) e p. 60 (herança como solução); capítulo de herança: herança como encarnação do princípio. |
| 10 | 2 | a fórmula "aberto para extensão, fechado para modificação", com interfaces, é de Martin | Martin 2003 | PARCIAL | Cap. 9, p. 99-101: a frase está lá, mas Martin credita o princípio a Meyer (nota com Meyer97, p. 57). Os meios citados são abstrações: classes-base abstratas e interfaces. |
| 11 | 2 | módulos pouco dependentes e elementos com um mesmo propósito | Yourdon e Constantine 1978 | FIEL | p. 76-77 (acoplamento como grau de interdependência; sistemas fracamente acoplados); cap. 7, p. 95 em diante (coesão; módulo funcional dedicado a uma só função). |
| 12 | 3 | cinco modelos de empresas diferentes; dois programas Java sem pattern; propriedades em lógica de predicados; etapa avaliada nomeia pattern e local | Kim 2025 | FIEL | p. 3 (programa-base sem patterns; lógica de predicados), p. 3 (versões de ChatGPT-4o, Claude 3.5 Sonnet, Microsoft 365 Copilot, Gemini 1.5, Llama 3.1), p. 8 (dois programas Java), p. 9 (prompt com pattern e local). |
| 13 | 3 | 13 modelos, seis famílias, Singleton nomeado, sem agente, sem controle negativo, sem manutenção | Kjellberg et al. 2026 | FIEL | p. 5 (13 modelos; GPT, Llama, Qwen, Gemma, Mistral, DeepSeek), p. 5-7 (quatro estratégias nomeiam o Singleton). A linha de base não nomeia, mas não é estratégia. As ausências foram confirmadas por busca. |
| 14 | 3 | dois modelos, pattern nomeado, sem orientação persistente | Zisa et al. 2026 | FIEL | p. 1 (GPT-4o e Gemini 2.5 Flash Preview), p. 2-3 (nome do pattern é argumento da ferramenta e entra no prompt). |
| 15 | 3 | cobre modelos de mais de uma empresa | Kim 2025 | FIEL | p. 3: cinco produtos de cinco empresas. |
| 16 | 3 | cobre modelos de mais de uma empresa | Kjellberg et al. 2026 | FIEL | p. 5, Tabela 1. |
| 17 | 3 | cobre modelos de mais de uma empresa | Zisa et al. 2026 | FIEL | p. 1: um modelo da OpenAI e um do Google. |
| 18 | 3 | critério por propriedades mostra qual parte falhou | Kim 2025 | FIEL | p. 10-11: tabelas com propriedades, grau de satisfação e comentários por pattern e modelo. |
| 19 | 3 | apontam o efeito de regras persistentes como questão em aberto | Jiang e Nam 2026 | FIEL | p. 1 (diretivas persistentes) e p. 11 (impacto em aberto). |
| 20 | 3 | três formas de organizar agentes; nove tarefas, apenas uma de código; capacidade do LLM pesa mais | Kim 2026 | PARCIAL | Papel, objetivo e história (p. 6), nove/quatro/um agentes (p. 4), nenhum vencedor e LLM mais influente (p. 1): fiéis. Porém T8 é Java e T9 gera suíte de testes com métodos, asserções e preparação (p. 3-4, 10). |
| 21 | 4.1 | classificado pelo Semgrep | Semgrep 2026 | FONTE AUSENTE | Página não salva. A frase só declara o uso da ferramenta. |
| 22 | 4.1 | métricas obtidas com o SonarQube | SonarSource 2026 | FONTE AUSENTE | Página não salva. A frase só declara o uso da ferramenta. |
| 23 | 5.2 | cobertura com o JaCoCo | JaCoCo 2026 | FONTE AUSENTE | Página não salva. A frase só declara o uso da ferramenta. |
| 24 | 7 | vazamento implícito de dados de treino é ameaça | Sallou et al. 2024 | FIEL | Seção 2.2, p. 2: vazamento implícito, com dúvida sobre memorização de código. A inferência sobre patterns é do próprio artigo e está redigida como tal. |
| 25 | 7 | esse ganho varia conforme o pattern e a tarefa | Prechelt et al. 2001 | FIEL | Resumo, p. 1134. |
| 26 | 7 | esse ganho varia conforme o pattern e a tarefa | Vokáč et al. 2004 | FIEL | p. 149-150 e 191. |
| 27 | 7 | esse ganho varia conforme o pattern e a tarefa | Zhang e Budgen 2012 | PARCIAL | Resumo, p. 1213, e p. 1229: sem apoio firme às alegações gerais, algum apoio qualitativo à manutenção; p. 1225 relata divergências por pattern. Não conclui variação por pattern e tarefa. A Introdução não atribui isso a Zhang. |
| 28 | 7 | modelos podem concordar entre si sem concordar com humanos | Wang et al. 2025 | PARCIAL | Não está dito no texto. Dados compatíveis só na sumarização (Tabelas 3 e 5, p. 12 e 14). O Achado 3 (p. 15) destaca o inverso: LLMs grandes alinham-se aos humanos e entre si. |
| 29 | 7 | complexidade cognitiva indica esforço de leitura, e não correção | Muñoz Barón et al. 2020 | PARCIAL | Resumo, p. 1, e p. 7: correlação com tempo de compreensão e notas subjetivas; resultado misto com acerto em tarefas de compreensão. O estudo não avalia a correção do código. |

## Correções propostas

**#7, Seção 1 (Zhang e Budgen 2012).** "[...] e uma revisão publicada em 2012, com a literatura até 2009, concluiu que faltava conhecimento para julgar, com base em evidências, quando cada um deve ser usado [Zhang e Budgen 2012]."

**#10, Seção 2 (Martin 2003).** "O princípio vem de Meyer, que o resolve com herança [Meyer 1997]; Martin o enuncia como 'aberto para extensão, fechado para modificação', creditando-o a Meyer, e o realiza por abstrações, como classes abstratas e interfaces [Martin 2003]."

**#20, Seção 3 (Kim 2026).** "[...] em nove tarefas, das quais uma produz a implementação em Java e outra a suíte de testes; as demais produzem requisitos e modelos."

**#27, Seção 7 (Zhang e Budgen 2012).** "Esse ganho, como registrado na Introdução, varia conforme o *pattern* e a tarefa [Prechelt et al. 2001; Vokáč et al. 2004], e a revisão de Zhang e Budgen (2012) encontrou apenas apoio limitado, sobretudo qualitativo, ao valor dos *patterns* na manutenção."

**#28, Seção 7 (Wang et al. 2025).** "Isso mostra que a leitura se repete, e não que está certa: em Wang et al. (2025), na sumarização de código, métodos com LLMs grandes tiveram correlação entre si de 31,5 a 79,4, mas abaixo de 30 com as notas humanas, embora na tradução e na geração se aproximassem mais delas; e substituir todos os avaliadores humanos é arriscado [Ahmed et al. 2025]."

**#29, Seção 7 (Muñoz Barón et al. 2020).** "[...] de modo que sua queda com a orientação não é evidência independente da mudança de desenho; ela se correlaciona com o tempo de compreensão e com a avaliação subjetiva, com resultados mistos no acerto das tarefas de compreensão [Muñoz Barón et al. 2020], e não mede se o código está correto."

## Dados das referências

Conferem com a fonte salva: Jiang e Nam (MSR '26, 13 páginas, DOI), Kim 2025 (JSS 230, 112519), Kim 2026 (IST 192, 108022), Kjellberg et al. (PROMISE '26, DOI), Meyer 1997 (2. ed.), Muñoz Barón et al. (ESEM '20, 12 páginas, DOI), Prechelt et al. (TSE 27(12), 1134-1144), Sallou et al. (ICSE-NIER '24, 5 páginas, DOI), Vokáč et al. (ESE 9, 149-195), Wang et al. (Proc. ACM Softw. Eng. 2, ISSTA086, DOI), Yao et al. (ICLR 2023), Yourdon e Constantine (2. ed., 1978, Yourdon Press), Zhang e Budgen (TSE 38(5), 1213-1231), Zisa et al. (SANER 2026, 222-227, DOI). Martin: copyright de 2003, catalogação de 2002; "2003" é aceitável.

Não verificáveis na cópia salva: Liu et al. (só arXiv v2; periódico e DOI ausentes), Xi et al. (só arXiv de 2023; volume, número e DOI ausentes), Vaswani et al. (o evento confere; as páginas 5998-6008 não aparecem) e o número 3 do volume de Vokáč et al.

Observação de método: Jiang e Nam, Kjellberg, Liu, Muñoz Barón, Sallou, Vaswani, Wang, Xi e Yao foram lidos em cópias do arXiv, embora a lista cite a publicação final. O cabeçalho do artigo afirma que preprints ficam de fora; a conferência vale para o texto dessas cópias.
