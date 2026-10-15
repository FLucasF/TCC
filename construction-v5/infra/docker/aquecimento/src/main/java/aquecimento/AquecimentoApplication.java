package aquecimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Existe só durante o build da imagem. Um projeto Spring Boot mínimo, compilado
// e testado uma vez para o ~/.m2 ficar quente, e apagado em seguida. O agente
// nunca vê nada disto: a pasta de trabalho dele nasce vazia.
@SpringBootApplication
public class AquecimentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquecimentoApplication.class, args);
    }

}
