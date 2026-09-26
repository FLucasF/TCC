package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogo das formas de pagamento aceitas. */
@Component
public class FormasPagamento {

    private final Map<String, FormaPagamento> porCodigo;

    public FormasPagamento(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream().collect(Collectors.toMap(
                FormaPagamento::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public FormaPagamento buscar(String codigo) {
        FormaPagamento forma = codigo == null ? null : porCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }
}
