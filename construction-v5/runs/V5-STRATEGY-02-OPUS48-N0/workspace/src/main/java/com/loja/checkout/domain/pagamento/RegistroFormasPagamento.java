package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CodigoErro;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reune todas as formas de pagamento. Qualquer forma nova marcada com
 * {@code @Component} entra aqui automaticamente.
 */
@Component
public class RegistroFormasPagamento {

    private final Map<String, FormaPagamento> porCodigo;

    public RegistroFormasPagamento(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    /** Busca pela forma; lanca FORMA_PAGAMENTO_INVALIDA se nao existir ou nao vier. */
    public FormaPagamento buscar(String codigo) {
        FormaPagamento forma = codigo == null ? null : porCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }
}
