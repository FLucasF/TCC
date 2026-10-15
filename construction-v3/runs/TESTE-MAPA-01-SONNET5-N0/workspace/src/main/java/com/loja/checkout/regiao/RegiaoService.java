package com.loja.checkout.regiao;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import org.springframework.stereotype.Service;

@Service
public class RegiaoService {

    public Regiao buscar(String codigo) {
        if (codigo == null) {
            throw new ErroPedidoException(CodigoErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException(CodigoErro.REGIAO_INVALIDA);
        }
    }
}
