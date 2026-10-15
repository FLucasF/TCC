package com.loja.checkout.pagamento;

import com.loja.checkout.erro.RegraNegocioException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PagamentoResolver {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public PagamentoResolver(List<FormaPagamento> formas) {
        this.formasPorCodigo = formas.stream().collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento resolver(String codigo) {
        FormaPagamento forma = formasPorCodigo.get(codigo);
        if (forma == null) {
            throw new RegraNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }
}
