package pousada.domain.identification;

import java.util.List;
import java.util.Map;

/** Uma forma de identificação aceita na criação da reserva. */
public interface IdentificationKind {

    /** Campos do corpo da requisição que pertencem a esta forma; todos são obrigatórios. */
    List<String> fieldNames();

    /** Recebe todos os campos, já verificados como presentes e não vazios. */
    Identification create(Map<String, String> values);
}
