package com.loja.checkout;

import com.loja.checkout.clube.BeneficiosClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, BeneficiosClube> clube;
    private final Map<String, FormaPagamento> formasPagamento;
    private final Map<String, BigDecimal> seguroPorRegiao;

    public CheckoutService(
            Map<String, ModalidadeEntrega> modalidades,
            Map<String, Cupom> cupons,
            Map<String, BeneficiosClube> clube,
            Map<String, FormaPagamento> formasPagamento,
            Map<String, BigDecimal> seguroPorRegiao) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.clube = clube;
        this.formasPagamento = formasPagamento;
        this.seguroPorRegiao = seguroPorRegiao;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req.itens());

        BeneficiosClube beneficios = clube.get(req.nivelClube());
        if (beneficios == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        BigDecimal pctSeguro = seguroPorRegiao.get(req.regiao());
        if (pctSeguro == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega entrega = modalidades.get(req.modalidadeEntrega());
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(req.itens());
        if (!entrega.aceitaPedido(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = calcularSubtotal(req.itens());

        BigDecimal freteNominal = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = beneficios.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteNominal;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (req.cupom() != null) {
            Cupom cupom = cupons.get(req.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");

            CupomContexto ctx = new CupomContexto(subtotal, frete, req.itens());
            if (!cupom.isAplicavel(ctx)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");

            descontoCupom = cupom.calcularDesconto(ctx);
        }

        FormaPagamento pagamento = formasPagamento.get(req.formaPagamento());
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.aceitaParcelas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal seguro = round(subtotal.multiply(pctSeguro));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.isDisponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = round(subtotal.multiply(beneficios.pctCashback()));

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                creditoProximaCompra,
                beneficios.temBrinde(subtotal)
        );
    }

    private void validarItens(List<Item> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (Item item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<Item> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
