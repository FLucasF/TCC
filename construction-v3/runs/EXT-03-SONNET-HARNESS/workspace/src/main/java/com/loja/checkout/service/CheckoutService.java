package com.loja.checkout.service;

import com.loja.checkout.coupon.Coupon;
import com.loja.checkout.coupon.CouponRegistry;
import com.loja.checkout.delivery.DeliveryMethod;
import com.loja.checkout.delivery.DeliveryMethodRegistry;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.ItemPedido;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.payment.PaymentMethod;
import com.loja.checkout.payment.PaymentMethodRegistry;
import com.loja.checkout.payment.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final DeliveryMethodRegistry deliveryMethodRegistry;
    private final CouponRegistry couponRegistry;
    private final PaymentMethodRegistry paymentMethodRegistry;

    public CheckoutService(DeliveryMethodRegistry deliveryMethodRegistry,
                            CouponRegistry couponRegistry,
                            PaymentMethodRegistry paymentMethodRegistry) {
        this.deliveryMethodRegistry = deliveryMethodRegistry;
        this.couponRegistry = couponRegistry;
        this.paymentMethodRegistry = paymentMethodRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        DeliveryMethod deliveryMethod = validarModalidadeEntrega(request.modalidadeEntrega());

        BigDecimal pesoTotal = itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!deliveryMethod.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = validarCupom(request.cupom(), subtotalProdutos, itens);

        PaymentMethod paymentMethod = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!paymentMethod.isParcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal baseFrete = deliveryMethod.calcularCusto(pesoTotal);
        BigDecimal frete = nivelClube.paganteFrete() ? baseFrete : Dinheiro.arredondar(BigDecimal.ZERO);
        BigDecimal descontoCupom = coupon == null
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : coupon.calcularDesconto(subtotalProdutos, itens, frete);

        BigDecimal totalProdutosComCupomEFrete = subtotalProdutos.subtract(descontoCupom).add(frete);

        if (!paymentMethod.isDisponivel(totalProdutosComCupomEFrete)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal imposto = regiao.calcularImposto(subtotalProdutos.subtract(descontoCupom));
        BigDecimal totalPedido = totalProdutosComCupomEFrete.add(imposto);

        ResultadoPagamento resultadoPagamento = paymentMethod.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.totalFinal().subtract(totalPedido);

        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                deliveryMethod.getPrazoDias(),
                imposto,
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return itensRequest.stream()
                .map(this::validarItem)
                .toList();
    }

    private ItemPedido validarItem(ItemRequest item) {
        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private NivelClube validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private DeliveryMethod validarModalidadeEntrega(String modalidadeEntrega) {
        return deliveryMethodRegistry.buscar(modalidadeEntrega)
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
    }

    private Coupon validarCupom(String codigoCupom, BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        Coupon coupon = couponRegistry.buscar(codigoCupom)
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        if (!coupon.isAplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return coupon;
    }

    private PaymentMethod validarFormaPagamento(String formaPagamento) {
        return paymentMethodRegistry.buscar(formaPagamento)
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
    }
}
