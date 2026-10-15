package br.tcc.checkout.dominio.entrega;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioModalidadesEntrega {
    private static final Map<String, ModalidadeEntrega> MODALIDADES = new HashMap<>();

    static {
        MODALIDADES.put("ECONOMICA", new ModalidadeEconomica());
        MODALIDADES.put("EXPRESSA", new ModalidadeExpressa());
        MODALIDADES.put("RETIRADA_LOJA", new ModalidadeRetiradaLoja());
        MODALIDADES.put("MOTOBOY", new ModalidadeMotoboy());
    }

    public Optional<ModalidadeEntrega> obter(String codigo) {
        return Optional.ofNullable(MODALIDADES.get(codigo));
    }

    public static RepositorioModalidadesEntrega criar() {
        return new RepositorioModalidadesEntrega();
    }
}
