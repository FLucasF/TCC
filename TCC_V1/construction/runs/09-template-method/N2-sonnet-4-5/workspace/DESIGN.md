# Decisões de Design

## Relatórios - Template Method

### Problema

O sistema precisa gerar 4 tipos de relatórios CSV:
- Ocupação (por data)
- Financeiro (por mês)
- Taxa de turismo (por mês)
- Planos de tarifa (por mês)

O contador já avisou que vai pedir mais tipos no futuro (cancelamentos, hóspedes por estado).

Além disso, mudanças estruturais devem afetar todos os relatórios:
- Atualmente: linha de título + cabeçalho + linhas de dados + total
- Futuro próximo: adicionar linha com data de geração no fim

### Análise (seguindo CLAUDE.md)

**Passo 1: Existe variação que merece estrutura?**
- ✓ Variação concreta: 4 tipos com colunas e lógica diferentes
- ✓ Crescimento esperado: explicitamente virão mais tipos
- ✓ Mudanças devem afetar todos: adicionar data de geração

**Passo 2: O que varia?**

Todos seguem a **mesma sequência fixa**:
1. Linha de título: `Pousada Maré Alta,<nome-relatorio>,<periodo>`
2. Linha de cabeçalho: nomes das colunas
3. Linhas de dados: uma por registro
4. Linha de total: agregação

O que varia são **alguns passos** dentro da sequência:
- Nome do relatório e período (para o título)
- Quais colunas no cabeçalho
- Quais dados em cada linha
- Como calcular o total

Isso é **Template Method**: sequência fixa com passos que variam por tipo.

**Não é Strategy porque:**
- Não é o algoritmo inteiro que muda
- Há estrutura comum compartilhada (título → cabeçalho → dados → total)
- Só alguns passos variam dentro do esqueleto fixo

### Solução Implementada

```java
public abstract class ReportGenerator {
    // Template Method - define a sequência fixa
    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitleLine()).append("\n");      // 1. Título (fixo no template)
        csv.append(buildHeaderLine()).append("\n");     // 2. Cabeçalho (varia)
        csv.append(buildDataLines());                   // 3. Dados (varia)
        csv.append(buildTotalLine()).append("\n");      // 4. Total (varia)
        return csv.toString();
    }

    // Métodos abstratos para as partes que variam
    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeaderLine();
    protected abstract String buildDataLines();
    protected abstract String buildTotalLine();

    // Método concreto usado pelo template
    private String buildTitleLine() {
        return "Pousada Maré Alta," + getReportName() + "," + getPeriod();
    }
}
```

Cada tipo de relatório é uma subclasse:
- `OccupancyReportGenerator`
- `RevenueReportGenerator`
- `TourismTaxReportGenerator`
- `RatePlansReportGenerator`

### Benefícios

1. **Adicionar novo tipo de relatório**: criar uma subclasse, implementar os métodos abstratos, registrar no controller. Pronto.

2. **Mudanças estruturais afetam todos**: para adicionar a data de geração no fim, basta modificar o método `generate()`:
   ```java
   public final String generate() {
       StringBuilder csv = new StringBuilder();
       csv.append(buildTitleLine()).append("\n");
       csv.append(buildHeaderLine()).append("\n");
       csv.append(buildDataLines());
       csv.append(buildTotalLine()).append("\n");
       csv.append(buildGenerationDateLine()).append("\n");  // <- aqui
       return csv.toString();
   }
   ```
   Todos os 4 (ou 10, ou 20) tipos ganham automaticamente.

3. **Sem duplicação**: formatação de números decimais, datas, etc. ficam nos métodos auxiliares da classe base.

4. **Cada caso em um lugar só**: adicionar "relatório de cancelamentos" não mexe nos existentes.

### Para o futuro

Quando o contador pedir novos relatórios:
1. Criar `CancellationsReportGenerator extends ReportGenerator`
2. Implementar os 5 métodos abstratos
3. Adicionar rota no `ReportController`
4. Pronto - sem mexer em nada que já funciona

Quando precisar adicionar a data de geração:
1. Modificar `generate()` na classe base
2. Todos os relatórios ganham automaticamente
