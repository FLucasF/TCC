package com.loja.checkout.coupon;

import com.loja.checkout.PedidoContext;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final Map<String, CupomStrategy> cuponsPorCodigo;

    public CouponService(List<CupomStrategy> cupons) {
        this.cuponsPorCodigo = cupons.stream()
                .collect(Collectors.toMap(CupomStrategy::getCodigo, Function.identity()));
    }

    public CupomStrategy resolver(String codigo, PedidoContext pedido, BigDecimal subtotalProdutos) {
        CupomStrategy cupom = cuponsPorCodigo.get(codigo);
        if (cupom == null) {
            throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
        }
        if (!cupom.aplicavel(pedido, subtotalProdutos)) {
            throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
        }
        return cupom;
    }
}
