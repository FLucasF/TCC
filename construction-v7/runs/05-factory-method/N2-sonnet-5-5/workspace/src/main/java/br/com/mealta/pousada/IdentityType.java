package br.com.mealta.pousada;

import java.util.List;
import java.util.Map;

/** Reconhece um jeito de identificação a partir dos campos do corpo da requisição. */
public interface IdentityType {
    /** Nomes dos campos do JSON que pertencem a este tipo. */
    List<String> fieldNames();

    /** Cria a identidade; recebe todos os campos do tipo, já preenchidos. */
    Identity create(Map<String, String> values);
}
