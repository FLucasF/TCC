# Índice de Arquivos - Sistema de Reservas Pousada Maré Alta

## 📚 Comece por aqui

1. **RESUMO.md** - Leia primeiro! Resumo executivo em português
2. **README.md** - Documentação técnica completa
3. **API_EXAMPLES.md** - Exemplos práticos de todas as APIs

## 📖 Documentação

| Arquivo | Descrição |
|---------|-----------|
| **RESUMO.md** | Resumo executivo em português (comece aqui!) |
| **README.md** | Documentação técnica completa |
| **API_EXAMPLES.md** | Exemplos de requisições e casos de teste |
| **CHECKLIST.md** | Checklist completo de implementação |
| **STRUCTURE.md** | Arquitetura e estrutura do código |
| **INDEX.md** | Este arquivo - índice de tudo |

## ⚙️ Configuração

| Arquivo | Descrição |
|---------|-----------|
| **pom.xml** | Configuração do Maven |
| **.env.example** | Exemplo de variáveis de ambiente |
| **create_database.sql** | Script SQL para criar o banco |
| **run.sh** | Script bash para executar o sistema |

## 💻 Código Fonte

### Estrutura de Pastas

```
src/
├── main/java/com/pousadamarealta/
│   ├── Main.java                    # Ponto de entrada
│   ├── controller/                  # Controllers REST (4 arquivos)
│   ├── dto/                        # DTOs (2 arquivos)
│   ├── model/                      # Entidades (6 arquivos)
│   ├── repository/                 # Acesso a dados (4 arquivos)
│   └── service/                    # Lógica de negócio (3 arquivos)
└── test/java/com/pousadamarealta/
    └── PricingServiceTest.java     # Testes
```

### Controllers (src/main/java/com/pousadamarealta/controller/)

- **RoomController.java** - GET /rooms
- **ReservationController.java** - Todas as operações de reserva
- **MessageController.java** - GET /messages
- **ReportController.java** - Relatórios CSV

### DTOs (src/main/java/com/pousadamarealta/dto/)

- **ErrorResponse.java** - Resposta de erro padronizada
- **ReceiptResponse.java** - Comprovante de reserva

### Models (src/main/java/com/pousadamarealta/model/)

- **Room.java** - Entidade Quarto
- **RoomType.java** - Enum: STANDARD, DELUXE, SUITE
- **Reservation.java** - Entidade Reserva
- **ReservationStatus.java** - Enum: PENDING, CONFIRMED, etc.
- **RatePlan.java** - Enum: 5 planos de tarifa
- **Message.java** - Entidade Mensagem

### Repositories (src/main/java/com/pousadamarealta/repository/)

- **DatabaseManager.java** - Pool de conexões e inicialização
- **RoomRepository.java** - CRUD de quartos
- **ReservationRepository.java** - CRUD de reservas + queries
- **MessageRepository.java** - CRUD de mensagens

### Services (src/main/java/com/pousadamarealta/service/)

- **PricingService.java** - Cálculo de preços, 5 planos, alta temporada
- **ReservationService.java** - Lógica de reservas, e-mails, comprovantes
- **ReportService.java** - Geração de relatórios CSV

### Testes (src/test/java/com/pousadamarealta/)

- **PricingServiceTest.java** - 6 testes de cálculo de preços

## 🎯 Para cada perfil de usuário

### Desenvolvedor do Site
👉 Leia: **API_EXAMPLES.md**
- Exemplos de todas as requisições
- Casos de teste prontos
- Códigos de erro

### Recepcionista
👉 Leia: **RESUMO.md**
- Como funciona o sistema
- Quais operações estão disponíveis

### Contador
👉 Leia: **README.md** seção "Relatórios"
- Como gerar relatório de ocupação
- Como gerar relatório financeiro
- Formato dos CSVs

### Administrador de Sistema
👉 Leia: **README.md** seção "Configuração"
- Variáveis de ambiente
- Como compilar e executar
- Requisitos do banco de dados

### Desenvolvedor/Mantenedor
👉 Leia: **STRUCTURE.md**
- Arquitetura do sistema
- Fluxo de dados
- Organização do código

## 🚀 Quick Start

```bash
# 1. Criar banco
createdb pousada

# 2. Compilar
mvn package

# 3. Executar
java -jar target/reservas-1.0.0.jar

# 4. Testar
curl http://localhost:7070/rooms
```

## 📦 Artefatos Gerados

| Arquivo | Descrição |
|---------|-----------|
| **target/reservas-1.0.0.jar** | JAR executável (9.4 MB) |
| **target/classes/** | Classes compiladas |
| **target/test-classes/** | Testes compilados |

## 🔍 Como Encontrar...

### "Preciso adicionar uma nova empresa no convênio"
👉 `src/main/java/com/pousadamarealta/service/PricingService.java`
- Procure por `CORPORATE_DISCOUNTS`

### "Preciso mudar as datas da alta temporada"
👉 `src/main/java/com/pousadamarealta/service/PricingService.java`
- Procure por `isHighSeason()`

### "Preciso mudar o formato do comprovante"
👉 `src/main/java/com/pousadamarealta/service/ReservationService.java`
- Procure por `formatReceiptText()`

### "Preciso adicionar um novo plano de tarifa"
👉 Siga esta ordem:
1. `model/RatePlan.java` - adicione o enum
2. `service/PricingService.java` - adicione a lógica de cálculo
3. `service/PricingService.java` - adicione a validação
4. `test/PricingServiceTest.java` - adicione testes

### "Preciso mudar a mensagem de e-mail"
👉 `src/main/java/com/pousadamarealta/service/ReservationService.java`
- Procure por `sendConfirmationEmail()`

### "Preciso adicionar um novo endpoint"
👉 Siga esta ordem:
1. `controller/` - crie método no controller apropriado
2. `Main.java` - registre a rota
3. `API_EXAMPLES.md` - adicione exemplo

## 📊 Estatísticas do Projeto

- **Classes Java**: 20
- **Testes**: 6
- **Endpoints REST**: 11
- **Linhas de código**: ~2.500
- **Tamanho do JAR**: 9.4 MB
- **Tempo de build**: ~10 segundos

## ✅ Status

- ✅ Compilação: OK
- ✅ Testes: 6/6 passando
- ✅ JAR: Gerado
- ✅ Documentação: Completa
- ✅ Pronto para produção

---

**Última atualização**: 02/11/2026
**Versão**: 1.0.0
