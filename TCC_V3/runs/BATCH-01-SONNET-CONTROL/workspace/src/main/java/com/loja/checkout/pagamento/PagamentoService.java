package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagamentoService {

    private final Map<String, PagamentoEstrategia> estrategiasPorCodigo;

    public PagamentoService(List<PagamentoEstrategia> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(PagamentoEstrategia::getCodigo, Function.identity()));
    }

    public PagamentoEstrategia buscarEstrategia(String codigo) {
        PagamentoEstrategia estrategia = codigo == null ? null : estrategiasPorCodigo.get(codigo);
        if (estrategia == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return estrategia;
    }
}
