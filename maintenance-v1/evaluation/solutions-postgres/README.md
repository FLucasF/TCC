# As soluções de referência sobre a base com PostgreSQL (21/10/2026)

O material de calibração do verificador de qualidade do `TCC_V1` (`TCC_V1/evaluation/quality`). Fica aqui, fora do
`TCC_V1`, como as soluções de antes: o `TCC_V1` não guarda respostas.

## O que tem

- **As 10 soluções dos cenários ímpares** (`NN-pattern.patch` e `NN-naive.patch`), as mesmas de `../solutions/`, refeitas
  sobre a base do `TCC_V1` com banco (`TCC_V1/maintenance/experiment/base/hotel`). O desenho é o mesmo; o que mudou é o
  necessário para o banco: os testes usam o `EmptyDatabase` da base, o serviço grava cada mudança da reserva, e o
  dado novo vira uma migração nova (no 01, o `company_code`; no 05, os campos de identificação, com um `CHECK` de que
  há um jeito só; no 07, o `status` da mensagem). Os testes da 01-pattern e da 05-pattern foram completados depois da
  primeira calibração (todas as regras novas conferidas com valores, e o campo novo pela API), porque elas servem de
  exemplo bom de testes. As 10 compilam, passam nos próprios testes e passam em todas as conferências dos roteiros de
  comportamento do verificador (`check_NN.sh`).
- **Os 6 exemplos estragados de propósito** (`architecture-*`, `tests-*`, `database-*`), cada um partindo de uma
  solução com o pattern, para calibrar as colunas de arquitetura, testes e banco:

| exemplo | parte de | o que foi estragado | esperado |
|---|---|---|---|
| `architecture-mixed` | 01-pattern | o check-in feito dentro da rota, com SQL e a regra copiada | arquitetura `com problemas` |
| `architecture-routes` | 09-pattern | sem o serviço: cada rota faz a regra e o SQL | arquitetura `ruim` |
| `tests-none` | 05-pattern | todos os testes apagados, menos um que só vê o serviço subir | testes `ruim` |
| `tests-hollow` | 03-pattern | as asserções viraram chamadas cujo resultado ninguém olha (a cobertura não muda) | testes `ruim` |
| `database-types` | 01-pattern | dinheiro em `double precision`, datas em texto, sem chave estrangeira nem restrições, a migração da base editada | banco `ruim` |
| `database-code` | 05-pattern | sem migrações (as tabelas criadas no código) e o SQL da reserva montado por concatenação | banco `ruim` |

- **O gabarito da calibração** (`cases.json`): o cenário de cada caso, as pesquisas em que ele é revisado e as notas
  esperadas. As 10 soluções são revisadas nas duas pesquisas (a classe do pattern e o comportamento); a 01-pattern e
  a 05-pattern, na manutenção, também servem de exemplo bom de arquitetura, testes e banco. Os 6 estragados são
  revisados só na manutenção, onde um defeito que a base já tinha (o ciclo entre `documents` e `reservations`) não
  conta contra o agente.

## Os geradores (`generators/`)

Os `solve_NN.py` são os de `../solutions/generators/`, com as trocas de texto ajustadas para a base com banco, e os
`spoil_*.py` fazem os exemplos estragados a partir das soluções salvas. Cada um copia a base para uma pasta
temporária, aplica as trocas, roda o `mvn verify` contra um PostgreSQL local vazio e grava o patch. O banco local:

```bash
docker run -d --name tcc-base-pg -e POSTGRES_PASSWORD=hotel -e POSTGRES_USER=hotel -e POSTGRES_DB=hotel -p 55433:5432 postgres:18-alpine
```

```bash
python generators/solve_01.py
```

Os `spoil_*.py` dependem das soluções já gravadas (rodar os `solve_NN.py` antes).
