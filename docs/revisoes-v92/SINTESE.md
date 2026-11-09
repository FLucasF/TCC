# Síntese da revisão da v92

Três frentes que as rodadas anteriores não cobriram. Os relatórios completos estão nesta pasta: `citacoes-A-H.md`,
`citacoes-J-Z.md`, `norma-e-portugues.md` e `proposta-resumo-conclusao.md`. Nada foi aplicado ao artigo.

## 1. Fidelidade das citações às fontes

Foram conferidas 65 ocorrências: **45 fiéis, 10 parciais, nenhuma sem suporte e 10 sem fonte salva**. Os 24 DOIs da lista
batem com o registro da Crossref. Os casos de Galster, Hou e Wang foram reconferidos nas fontes depois.

| # | Citação | Seção | Problema | Correção proposta |
|---|---|---|---|---|
| 1 | Wang et al. 2025 | 7 | O texto não afirma que modelos concordam entre si sem concordar com humanos; o Achado 3 destaca o inverso, e só a sumarização dá dados compatíveis. | Citar o resultado da sumarização com os números e dizer que nas outras tarefas os modelos se aproximam dos humanos. |
| 2 | Muñoz Barón et al. 2020 | 7 | O estudo não avalia se o código está correto. | Dizer que a medida se relaciona com o tempo de compreensão e a avaliação subjetiva, com resultados mistos no acerto, e não mede correção. |
| 3 | Zhang e Budgen 2012 | 7 | A revisão não conclui que o ganho varia por *pattern* e tarefa. | Manter Prechelt e Vokáč nessa afirmação e dizer que a revisão achou apoio limitado. |
| 4 | Zhang e Budgen 2012 | 1 | Estende ao presente um juízo de 2012. | "Uma revisão publicada em 2012 concluiu que faltava conhecimento". |
| 5 | Arcuri e Briand 2014 | 7 | O "piso de dez" é o mínimo admitido com muitos artefatos e pouco tempo; a recomendação geral é de ao menos 1.000 execuções. | Dizer as duas coisas. |
| 6 | Galster et al. 2026 | 1 | Os 5,5% e 4,6% são de 2.853 repositórios que já usam ferramentas de agente. | Trocar a base na frase. |
| 7 | Hou et al. 2024 | 1 | 0,92% é a parcela dos registros de tarefa (4 de 436), e não de artigos. | "0,92% das tarefas registradas". |
| 8 | Kim 2026 | 3 | "Apenas uma é de código" ignora a tarefa que gera a suíte de testes. | "Uma produz a implementação e outra a suíte de testes". |
| 9 | Gamma et al. 1995 | 2 | "Num objeto próprio" não vale para o Template Method, em que a variação fica em subclasses. | "Em um objeto ou em uma subclasse própria". |
| 10 | Martin 2003 | 2 | Martin credita o princípio a Meyer. | Mencionar o crédito. |

**Sem fonte salva:** a documentação da Anthropic (4 ocorrências; a mais sensível é "tratado como contexto, e não como regra imposta"), Aniche (2), Ampatzoglou (1, o texto salvo termina antes do resultado citado) e as páginas de Semgrep, SonarQube e JaCoCo (só declaram o uso). Vale abrir a documentação da Anthropic e o artigo de Ampatzoglou antes da entrega.

**Sobre preprints:** nove textos citados pela versão publicada foram lidos em cópias do arXiv. A citação é da publicação, mas a conferência vale para o texto dessas cópias.

## 2. Norma e português

O `.docx` está conforme em A4, margens, fonte, espaçamento, título, seções e posição das 13 legendas. A estrutura do Art. 6º está completa. O artigo tem cerca de 23 páginas, por simulação. São 83 itens a corrigir e 32 de estilo.

O mais importante: tirar o parágrafo "Rascunho de 23/10/2026..."; incluir o bloco de autoria, que depende dos seus dados; reescrever os cinco rótulos em negrito e o tom informal da Seção 2; uniformizar o itálico de *pattern* (16 de 101 ocorrências sem itálico) e a alternância com "padrão"; consertar as emendas mal encaixadas; corrigir erros pontuais como "júniores" e "LCOM médio"; e uniformizar periódicos e campos das referências. Medidas de recuo e formato do bloco de autoria foram avaliadas de memória e precisam ser conferidas no modelo do professor.

## 3. Resumo e Conclusão

O Resumo atual ocupa cerca de 25 linhas. A proposta tem 845 caracteres, cerca de 9,4 linhas, e a Conclusão foi reescrita em prosa acadêmica, sem informação nova.
