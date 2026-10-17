# O juiz no lote pilots-1: claude-opus-5-5, effort high

Os códigos estão sorteados; a chave fica em `key.json`. `?` é um item sem resposta, `!` um valor fora da lista da régua.

## padroes

| código | P1-local | P1-selecao | P2-local | P2-selecao | P3-local | P3-selecao | P4-local | P4-selecao | P5-forma | P5-proporcao | P6 | P7 | F | E |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| svc-01 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | isolado | isolado | container | indeterminado |
| svc-02 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | isolado | container | houve |
| svc-03 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | isolado | espalhado | container | nao_houve |
| svc-04 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional | vazado | espalhado | nenhum | indeterminado |
| svc-05 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | espalhado | container | nao_houve |
| svc-06 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional | vazado | espalhado | nenhum | nao_houve |
| svc-07 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | isolado | container | houve |
| svc-08 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | isolado | espalhado | container | nao_houve |

## arquitetura

| código | A1 | A2 | A3 | A4 | A5 | A6 |
|---|---|---|---|---|---|---|
| svc-01 | nao_atende | leves | atende | atende | atende | por_funcionalidade |
| svc-02 | nao_atende | estruturais | atende | atende | atende | por_funcionalidade |
| svc-03 | nao_atende | leves | atende | atende | atende | misto |
| svc-04 | nao_atende | sem_ciclos | atende | atende | atende | por_camada |
| svc-05 | indeterminado | sem_ciclos | atende | atende | atende | misto |
| svc-06 | nao_atende | sem_ciclos | nao_atende | atende | atende | por_camada |
| svc-07 | nao_atende | leves | atende | atende | atende | por_funcionalidade |
| svc-08 | nao_atende | sem_ciclos | atende | atende | atende | por_camada |

## testes

| código | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| svc-01 | amplo | valores | atende | atende |
| svc-02 | amplo | valores | atende | atende |
| svc-03 | amplo | valores | atende | atende |
| svc-04 | parcial | mistas | atende | atende |
| svc-05 | parcial | valores | atende | nao_atende |
| svc-06 | minimo | valores | nao_atende | atende |
| svc-07 | amplo | valores | atende | atende |
| svc-08 | indeterminado | valores | atende | nao_atende |

## banco

| código | B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8 | B9 | B10 |
|---|---|---|---|---|---|---|---|---|---|---|
| svc-01 | atende | atende | parcial | atende | atende | atende | parcial | atende | script_sql | nao_atende |
| svc-02 | atende | atende | parcial | atende | atende | atende | atende | atende | script_sql | nao_atende |
| svc-03 | atende | nao_atende | nao_se_aplica | atende | nao_atende | atende | parcial | atende | script_sql | nao_atende |
| svc-04 | atende | parcial | nao_atende | atende | atende | atende | nao_atende | atende | ddl_auto | nao_atende |
| svc-05 | atende | parcial | atende | atende | atende | atende | parcial | atende | script_sql | nao_atende |
| svc-06 | atende | atende | parcial | atende | nao_atende | atende | parcial | indeterminado | migracao_versionada | nao_atende |
| svc-07 | atende | atende | parcial | atende | atende | atende | parcial | atende | script_sql | nao_atende |
| svc-08 | atende | nao_atende | nao_se_aplica | atende | atende | atende | parcial | nao_atende | script_sql | atende |

Custo: US$ 15.29.
