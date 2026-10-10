package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import com.loja.exception.CheckoutException;
import com.loja.service.club.*;
import com.loja.service.coupon.*;
import com.loja.service.payment.*;
import com.loja.service.shipping.*;
import com.loja.util.MoneyRounder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        var itens = request.itens();
        var subtotalProdutos = calcularSubtotal(itens);
        var pesoTotal = calcularPeso(itens);

        var nivelClube = parseNivelClube(request.nivelClube());
        var regiao = parseRegiao(request.regiao());
        var modalidade = parseModalidade(request.modalidadeEntrega());
        var formaPagamento = parseFormaPagamento(request.formaPagamento());

        var shipping = getShippingCalculator(modalidade);
        validarModalidadeDisponivel(shipping, pesoTotal);

        var frete = shipping.calculate(pesoTotal);
        var prazoEntrega = shipping.getPrazo();

        var clubBenefits = getClubBenefits(nivelClube);
        if (clubBenefits.shouldWaiveFrete()) {
            frete = BigDecimal.ZERO;
        }

        var seguro = calcularSeguro(subtotalProdutos, regiao);

        var descontoCupom = BigDecimal.ZERO;
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            var coupon = getCouponCalculator(request.cupom(), itens);
            coupon.validate(subtotalProdutos, pesoTotal);
            descontoCupom = coupon.calculate(subtotalProdutos, pesoTotal, frete);
        }

        var totalAntesPagamento = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalAntesPagamento = MoneyRounder.round(totalAntesPagamento);

        var parcelas = request.parcelas() != null ? request.parcelas() : 1;
        var paymentAdjuster = getPaymentAdjuster(formaPagamento);
        paymentAdjuster.validate(parcelas, totalAntesPagamento);

        var paymentAdjustment = paymentAdjuster.calculate(totalAntesPagamento, parcelas);
        var totalFinal = totalAntesPagamento.add(paymentAdjustment.ajuste());
        totalFinal = MoneyRounder.round(totalFinal);

        var credito = subtotalProdutos.multiply(clubBenefits.getCreditRate());
        credito = MoneyRounder.round(credito);

        var brinde = clubBenefits.shouldAddBrinde(subtotalProdutos);

        return new CheckoutResponse(
                MoneyRounder.round(subtotalProdutos),
                MoneyRounder.round(descontoCupom),
                MoneyRounder.round(frete),
                prazoEntrega,
                MoneyRounder.round(seguro),
                MoneyRounder.round(paymentAdjustment.ajuste()),
                totalFinal,
                paymentAdjustment.parcelas(),
                paymentAdjustment.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (var item : request.itens()) {
            if (item.precoUnitario() == null ||
                item.quantidade() == null ||
                item.pesoKg() == null ||
                item.precoUnitario().signum() <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg().signum() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : itens) {
            BigDecimal itemTotal = item.precoUnitario().multiply(new BigDecimal(item.quantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return MoneyRounder.round(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (var item : itens) {
            BigDecimal itemPeso = item.pesoKg().multiply(new BigDecimal(item.quantidade()));
            peso = peso.add(itemPeso);
        }
        return peso;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getTaxaSeguro());
        return MoneyRounder.round(seguro);
    }

    private NivelClube parseNivelClube(String nivelClube) {
        if (nivelClube == null || nivelClube.isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega parseModalidade(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private FormaPagamento parseFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ShippingCalculator shipping, BigDecimal pesoTotal) {
        if (!shipping.isAvailable(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private ShippingCalculator getShippingCalculator(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EconomicaShippingCalculator();
            case EXPRESSA -> new ExpressaShippingCalculator();
            case RETIRADA_LOJA -> new RetiradaLojaShippingCalculator();
            case MOTOBOY -> new MotoboyShippingCalculator();
        };
    }

    private CouponCalculator getCouponCalculator(String cupom, List<ItemCarrinho> itens) {
        return switch (cupom) {
            case "BEMVINDO10" -> new BemVindo10CouponCalculator();
            case "MENOS50" -> new Menos50CouponCalculator();
            case "FRETEGRATIS" -> new FreteGratisCouponCalculator();
            case "LEVE3PAGUE2" -> new Leve3Pague2CouponCalculator(itens);
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }

    private PaymentAdjuster getPaymentAdjuster(FormaPagamento formaPagamento) {
        return switch (formaPagamento) {
            case PIX -> new PixPaymentAdjuster();
            case CARTAO -> new CartaoPaymentAdjuster();
            case BOLETO -> new BoletoPaymentAdjuster();
        };
    }

    private ClubBenefits getClubBenefits(NivelClube nivelClube) {
        return switch (nivelClube) {
            case BRONZE -> new BronzeClubBenefits();
            case PRATA -> new PrataClubBenefits();
            case OURO -> new OuroClubBenefits();
        };
    }
}
