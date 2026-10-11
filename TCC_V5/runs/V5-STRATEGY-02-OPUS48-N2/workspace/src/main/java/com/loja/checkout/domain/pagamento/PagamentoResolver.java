package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Acha a forma de pagamento pelo codigo; nao achou ou nao informado -> FORMA_PAGAMENTO_INVALIDA. */
@Component
public class PagamentoResolver {

    private final Map<String, FormaPagamento> porCodigo;

    public PagamentoResolver(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento resolver(String codigo) {
        FormaPagamento forma = codigo == null ? null : porCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }
}
