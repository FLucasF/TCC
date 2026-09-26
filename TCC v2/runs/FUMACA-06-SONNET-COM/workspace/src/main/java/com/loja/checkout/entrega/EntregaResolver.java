package com.loja.checkout.entrega;

import com.loja.checkout.erro.RegraNegocioException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EntregaResolver {

    private final Map<String, OpcaoEntrega> opcoesPorCodigo;

    public EntregaResolver(List<OpcaoEntrega> opcoes) {
        this.opcoesPorCodigo = opcoes.stream().collect(Collectors.toMap(OpcaoEntrega::codigo, Function.identity()));
    }

    public OpcaoEntrega resolver(String codigo) {
        OpcaoEntrega opcao = opcoesPorCodigo.get(codigo);
        if (opcao == null) {
            throw new RegraNegocioException("MODALIDADE_INVALIDA");
        }
        return opcao;
    }
}
