package com.loja.checkout.service;

import com.loja.checkout.club.ClubLevel;
import com.loja.checkout.club.ClubLevelRegistry;
import com.loja.checkout.coupon.Coupon;
import com.loja.checkout.coupon.CouponContext;
import com.loja.checkout.coupon.CouponRegistry;
import com.loja.checkout.delivery.DeliveryMethod;
import com.loja.checkout.delivery.DeliveryMethodRegistry;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.payment.PaymentMethod;
import com.loja.checkout.payment.PaymentMethodRegistry;
import com.loja.checkout.payment.PaymentResult;
import com.loja.checkout.region.Regiao;
import com.loja.checkout.util.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final DeliveryMethodRegistry deliveryMethodRegistry;
    private final CouponRegistry couponRegistry;
    private final ClubLevelRegistry clubLevelRegistry;
    private final PaymentMethodRegistry paymentMethodRegistry;

    public CheckoutService(DeliveryMethodRegistry deliveryMethodRegistry,
                            CouponRegistry couponRegistry,
                            ClubLevelRegistry clubLevelRegistry,
                            PaymentMethodRegistry paymentMethodRegistry) {
        this.deliveryMethodRegistry = deliveryMethodRegistry;
        this.couponRegistry = couponRegistry;
        this.clubLevelRegistry = clubLevelRegistry;
        this.paymentMethodRegistry = paymentMethodRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemRequest> itens = request.itens();
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(item -> !item.isValido())) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        ClubLevel clubLevel = clubLevelRegistry.buscar(request.nivelClube());
        if (clubLevel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = Regiao.buscar(request.regiao());
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        DeliveryMethod deliveryMethod = deliveryMethodRegistry.buscar(request.modalidadeEntrega());
        if (deliveryMethod == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        if (!deliveryMethod.isDisponivel(pesoTotalKg)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteBruto = deliveryMethod.calcularFrete(pesoTotalKg);
        BigDecimal frete = clubLevel.isFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteBruto;

        Coupon coupon = null;
        if (request.cupom() != null) {
            coupon = couponRegistry.buscar(request.cupom());
            if (coupon == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            CouponContext couponContext = new CouponContext(itens, subtotalProdutos, frete);
            if (!coupon.isAplicavel(couponContext)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
        BigDecimal descontoCupom = coupon == null
                ? BigDecimal.ZERO.setScale(2)
                : Money.round(coupon.calcularDesconto(new CouponContext(itens, subtotalProdutos, frete)));

        PaymentMethod paymentMethod = paymentMethodRegistry.buscar(request.formaPagamento());
        if (paymentMethod == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.parcelasOuPadrao();
        if (!paymentMethod.isParcelasValida(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = Money.round(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
        BigDecimal totalPedido = Money.round(subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!paymentMethod.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        PaymentResult paymentResult = paymentMethod.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = paymentResult.totalFinal().subtract(totalPedido);

        BigDecimal creditoProximaCompra = Money.round(clubLevel.calcularCredito(subtotalProdutos));
        boolean brinde = clubLevel.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                deliveryMethod.getPrazoDias(),
                seguro,
                ajustePagamento,
                paymentResult.totalFinal(),
                parcelas,
                paymentResult.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Money.round(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }
}
