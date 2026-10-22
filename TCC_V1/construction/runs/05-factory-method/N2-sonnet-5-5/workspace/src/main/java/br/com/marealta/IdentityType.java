package br.com.marealta;

import java.util.List;

/** Um jeito de se identificar: seus campos e o documento que o hóspede recebe. */
public interface IdentityType {
    String kind();

    /** Nomes dos campos da API que este tipo exige, todos obrigatórios. */
    List<String> fields();

    DocumentFormat format();
}
