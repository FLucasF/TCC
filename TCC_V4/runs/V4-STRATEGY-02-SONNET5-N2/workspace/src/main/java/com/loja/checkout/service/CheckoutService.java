package com.loja.checkout.service;

import com.loja.checkout.club.ClubLevel;
import com.loja.checkout.club.ClubLevelRegistry;
import com.loja.checkout.coupon.Coupon;
import com.loja.checkout.coupon.CouponRegistry;
import com.loja.checkout.coupon.CupomContexto;
import com.loja.checkout.delivery.DeliveryOption;
import com.loja.checkout.delivery.DeliveryOptionRegistry;
import com.loja.checkout.dto.ResumoPedidoRequest;
import com.loja.checkout.dto.ResumoPedidoResponse;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.PedidoException;
import com.loja.checkout.model.Item;
import com.loja.checkout.payment.PaymentMethod;
import com.loja.checkout.payment.PaymentMethodRegistry;
import com.loja.checkout.payment.ResultadoPagamento;
import com.loja.checkout.region.Regiao;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    private final DeliveryOptionRegistry deliveryOptionRegistry;
    private final CouponRegistry couponRegistry;
    private final ClubLevelRegistry clubLevelRegistry;
    private final PaymentMethodRegistry paymentMethodRegistry;

    public CheckoutService(DeliveryOptionRegistry deliveryOptionRegistry,
                            CouponRegistry couponRegistry,
                            ClubLevelRegistry clubLevelRegistry,
                            PaymentMethodRegistry paymentMethodRegistry) {
        this.deliveryOptionRegistry = deliveryOptionRegistry;
        this.couponRegistry = couponRegistry;
        this.clubLevelRegistry = clubLevelRegistry;
        this.paymentMethodRegistry = paymentMethodRegistry;
    }

    public ResumoPedidoResponse calcularResumo(ResumoPedidoRequest request) {
        List<Item> itens = validarItens(request);
        ClubLevel clubLevel = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        BigDecimal pesoTotal = somarPeso(itens);
        DeliveryOption deliveryOption = validarModalidadeEntrega(request.modalidadeEntrega(), pesoTotal);
        Coupon coupon = validarCupom(request.cupom());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(itens));

        BigDecimal frete = deliveryOption.calcularFrete(pesoTotal);
        if (clubLevel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = calcularDescontoCupom(coupon, itens, subtotalProdutos, frete);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        PaymentMethod paymentMethod = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelasOuPadrao();
        validarParcelamento(paymentMethod, parcelas);
        validarDisponibilidadeFormaPagamento(paymentMethod, totalPedido);

        ResultadoPagamento resultadoPagamento = paymentMethod.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = clubLevel.calcularCredito(subtotalProdutos);
        boolean brinde = clubLevel.concedeBrinde(subtotalProdutos);

        return new ResumoPedidoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                deliveryOption.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<Item> validarItens(ResumoPedidoRequest request) {
        List<Item> itens = request.itens() == null ? List.of() :
                request.itens().stream().map(item -> item.paraModelo()).toList();
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private ClubLevel validarNivelClube(String codigo) {
        return clubLevelRegistry.buscar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
    }

    private Regiao validarRegiao(String codigo) {
        return Regiao.buscar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.REGIAO_INVALIDA));
    }

    private DeliveryOption validarModalidadeEntrega(String codigo, BigDecimal pesoTotal) {
        DeliveryOption deliveryOption = deliveryOptionRegistry.buscar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.MODALIDADE_INVALIDA));
        if (!deliveryOption.disponivel(pesoTotal)) {
            throw new PedidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        return deliveryOption;
    }

    private Coupon validarCupom(String codigo) {
        if (codigo == null) {
            return null;
        }
        Coupon coupon = couponRegistry.buscar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.CUPOM_INVALIDO));
        return coupon;
    }

    private BigDecimal calcularDescontoCupom(Coupon coupon, List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (coupon == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        CupomContexto contexto = new CupomContexto(itens, subtotalProdutos, frete);
        if (!coupon.aplicavel(contexto)) {
            throw new PedidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(coupon.calcularDesconto(contexto));
    }

    private PaymentMethod validarFormaPagamento(String codigo) {
        return paymentMethodRegistry.buscar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
    }

    private void validarParcelamento(PaymentMethod paymentMethod, int parcelas) {
        if (!paymentMethod.parcelasPermitidas(parcelas)) {
            throw new PedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private void validarDisponibilidadeFormaPagamento(PaymentMethod paymentMethod, BigDecimal totalPedido) {
        if (!paymentMethod.disponivel(totalPedido)) {
            throw new PedidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    private BigDecimal somarPeso(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            total = total.add(item.pesoTotal());
        }
        return total;
    }
}
