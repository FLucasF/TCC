package com.example.checkout.service;

import com.example.checkout.club.ClubLevel;
import com.example.checkout.coupon.Coupon;
import com.example.checkout.model.Item;
import com.example.checkout.model.Order;
import com.example.checkout.model.ResumoRequest;
import com.example.checkout.model.ResumoResponse;
import com.example.checkout.money.Money;
import com.example.checkout.payment.PaymentMethod;
import com.example.checkout.payment.PaymentResult;
import com.example.checkout.region.Region;
import com.example.checkout.shipping.ShippingModality;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req == null ? null : req.itens());

        ClubLevel clube = parseEnum(req.nivelClube(), ClubLevel.class, "NIVEL_CLUBE_INVALIDO");
        Region regiao = parseEnum(req.regiao(), Region.class, "REGIAO_INVALIDA");
        ShippingModality entrega = parseEnum(req.modalidadeEntrega(), ShippingModality.class, "MODALIDADE_INVALIDA");

        Order order = new Order(req.itens());

        if (!entrega.atende(order)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Coupon cupom = null;
        if (req.cupom() != null) {
            try {
                cupom = Coupon.valueOf(req.cupom());
            } catch (IllegalArgumentException e) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            if (!cupom.aplicavel(order)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        PaymentMethod pagamento = parseEnum(req.formaPagamento(), PaymentMethod.class, "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = Money.round(order.subtotal());
        BigDecimal freteBruto = clube.freteGratis() ? BigDecimal.ZERO : entrega.custo(order);
        BigDecimal frete = Money.round(freteBruto);
        BigDecimal descontoCupom = cupom == null ? Money.ZERO : Money.round(cupom.desconto(order, frete));
        BigDecimal seguro = Money.round(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = Money.round(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        PaymentResult pr = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = Money.round(subtotal.multiply(clube.percentualCredito()));
        boolean brinde = clube.ganhaBrinde(subtotal);

        return new ResumoResponse(
            subtotal, descontoCupom, frete, entrega.prazoDias(),
            seguro, pr.ajuste(), pr.totalFinal(), parcelas, pr.valorParcela(),
            credito, brinde
        );
    }

    private void validarItens(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (Item item : itens) {
            if (item == null
                || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private <T extends Enum<T>> T parseEnum(String valor, Class<T> tipo, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
