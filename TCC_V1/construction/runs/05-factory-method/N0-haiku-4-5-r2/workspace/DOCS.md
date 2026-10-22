# 📚 Índice de Documentação - Pousada Maré Alta

## 📖 Documentação Principal

### 🚀 [ENTREGA.md](ENTREGA.md) - **COMECE AQUI**
Resumo executivo, componentes entregues e status final.
**Para**: Entender o projeto em 5 minutos

### ⚡ [INICIO-RAPIDO.md](INICIO-RAPIDO.md)
Como compilar, executar e testar em minutos.
**Para**: Começar a usar o sistema

### 📖 [README.md](README.md)
Documentação completa da API e arquitetura.
**Para**: Referência técnica detalhada

## 🔧 Guias Técnicos

### 💻 [EXEMPLOS.md](EXEMPLOS.md)
Exemplos completos de requisições e respostas para cada endpoint.
**Para**: Entender como usar a API

### 🗄️ [BANCO-DADOS.md](BANCO-DADOS.md)
Schema PostgreSQL, estrutura de tabelas e queries principais.
**Para**: Detalhes do banco de dados

### 🏗️ [SISTEMA.md](SISTEMA.md)
Visão geral técnica, arquitetura e componentes.
**Para**: Entender a estrutura do sistema

### ✅ [CHECKLIST.md](CHECKLIST.md)
Lista completa de tudo que foi implementado.
**Para**: Validar que nada foi deixado de lado

## 🎯 Fluxos Rápidos

### Para Iniciantes
1. Ler: [ENTREGA.md](ENTREGA.md) (overview)
2. Ler: [INICIO-RAPIDO.md](INICIO-RAPIDO.md) (setup)
3. Executar: `./run.sh`
4. Testar: `./test-api.sh`

### Para Desenvolvedores
1. Ler: [SISTEMA.md](SISTEMA.md) (arquitetura)
2. Ler: [README.md](README.md) (API)
3. Ler: [BANCO-DADOS.md](BANCO-DADOS.md) (DB)
4. Explorar: `src/main/java/com/pousada`

### Para Administradores
1. Ler: [BANCO-DADOS.md](BANCO-DADOS.md) (backup)
2. Ler: [INICIO-RAPIDO.md](INICIO-RAPIDO.md) (deploy)
3. Usar: `docker-compose.yml`

## 📋 Referência Rápida

### Compilação
```bash
mvn clean package
```

### Execução
```bash
java -jar target/mare-alta-1.0.0.jar
# ou
./run.sh
```

### Testes
```bash
mvn verify
./test-api.sh
```

### Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| GET | /rooms | Listar quartos |
| POST | /reservations | Criar reserva |
| GET | /reservations/{id} | Consultar reserva |
| POST | /reservations/{id}/confirmation | Confirmar |
| POST | /reservations/{id}/check-in | Check-in |
| POST | /reservations/{id}/check-out | Check-out |
| POST | /reservations/{id}/cancellation | Cancelar |
| GET | /reservations/{id}/document | Segunda via |
| GET | /messages | Mensagens |
| GET | /reports/occupancy?date= | Ocupação |
| GET | /reports/revenue?month= | Receita |

### Variáveis de Ambiente
```bash
DB_URL="jdbc:postgresql://localhost:5432/marealta"
DB_USER="pousada"
DB_PASSWORD="senha123"
PORT=7070
```

## 📊 Estrutura de Arquivos

### Código Fonte
```
src/main/java/com/pousada/
├── App.java
├── api/               (4 controllers)
├── db/                (1 database)
├── model/             (4 modelos)
├── repository/        (3 repositórios)
├── service/           (2 serviços)
└── util/              (2 utilitários)
```

### Testes
```
src/test/java/com/pousada/
└── PricingTest.java   (10 testes)
```

### Configuração
```
pom.xml
docker-compose.yml
run.sh
test-api.sh
```

### Documentação
```
README.md
INICIO-RAPIDO.md
EXEMPLOS.md
SISTEMA.md
BANCO-DADOS.md
CHECKLIST.md
ENTREGA.md
DOCS.md (este arquivo)
```

## 🎓 Tópicos por Nível

### Nível 1: Usuário Final
- Ler: [ENTREGA.md](ENTREGA.md)
- Entender: Fluxos de reserva
- Usar: API com curl

### Nível 2: Operacional
- Ler: [INICIO-RAPIDO.md](INICIO-RAPIDO.md)
- Executar: Sistema com Docker
- Monitorar: Logs e mensagens
- Backup: Banco de dados

### Nível 3: Desenvolvedor
- Ler: [SISTEMA.md](SISTEMA.md)
- Estudar: Código em `src/`
- Modificar: Adicionar features
- Testar: Com `mvn verify`

### Nível 4: DevOps
- Ler: [BANCO-DADOS.md](BANCO-DADOS.md)
- Configurar: Produção
- Deploy: Com Docker Compose
- Scale: Múltiplas instâncias

## 🔍 Procurar Por...

### CPF, CNPJ, Passaporte
[EXEMPLOS.md](EXEMPLOS.md) - Exemplos de cada tipo

### Arredondamento
[README.md](README.md#arredondamento) - Banker's Rounding

### Alta Temporada
[SISTEMA.md](SISTEMA.md#funcionalidades-de-negócio) - Datas

### Cancelamento
[EXEMPLOS.md](EXEMPLOS.md#10-cancelar-reserva) - Reembolso

### Email
[ENTREGA.md](ENTREGA.md#5-emails-automáticos) - Configuração

### Relatórios
[EXEMPLOS.md](EXEMPLOS.md#11-relatório-de-ocupação) - CSV

### Erros
[README.md](README.md#códigos-de-erro) - Códigos HTTP

## 💾 Downloads

### JAR Executável
```
target/mare-alta-1.0.0.jar (7.2 MB)
```

### Docker Image
```
java:21-slim + PostgreSQL:18
```

## 📞 Suporte

Para dúvidas ou problemas:

1. Verifique [EXEMPLOS.md](EXEMPLOS.md)
2. Veja [README.md](README.md#troubleshooting)
3. Consulte [BANCO-DADOS.md](BANCO-DADOS.md#monitoramento)

## ✅ Checklist de Leitura

Para entender completamente o sistema:

- [ ] Ler [ENTREGA.md](ENTREGA.md) (5 min)
- [ ] Ler [INICIO-RAPIDO.md](INICIO-RAPIDO.md) (5 min)
- [ ] Executar `./run.sh` (2 min)
- [ ] Rodar `./test-api.sh` (1 min)
- [ ] Ler [EXEMPLOS.md](EXEMPLOS.md) (10 min)
- [ ] Ler [SISTEMA.md](SISTEMA.md) (10 min)
- [ ] Explorar código em `src/` (15 min)
- [ ] Ler [BANCO-DADOS.md](BANCO-DADOS.md) (10 min)

**Total: ~60 minutos para dominar o sistema**

## 🎯 Metas Alcançadas

- ✅ API funcional
- ✅ 3 tipos de identificação
- ✅ Cálculos precisos
- ✅ Documentação completa
- ✅ Exemplos funcionais
- ✅ Pronto para produção

---

**Última atualização**: 2026-10-22  
**Status**: 🚀 Pronto para usar!
