package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Catalogo;
import com.loja.checkout.comum.CodigoErro;
import java.util.List;
import org.springframework.stereotype.Component;

/** Todas as formas de pagamento aceitas pela loja. */
@Component
public class CatalogoFormasPagamento extends Catalogo<FormaPagamento> {

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        super(formas, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }
}
