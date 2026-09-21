package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.error.CheckoutException;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.Modalidade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal TAXA_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final double LIMITE_MOTOBOY_KG = 5.0;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, request.getItens());
        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getCupom());
        Integer prazoEntrega = obterPrazoEntrega(request.getModalidadeEntrega());

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .setScale(2, ROUNDING);

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal ajustePagamento = calcularAjustePagamento(
            request.getFormaPagamento(),
            totalPedido,
            parcelas
        );

        BigDecimal totalFinal = totalPedido.add(ajustePagamento).setScale(2, ROUNDING);

        BigDecimal valorParcela = calcularValorParcela(
            request.getFormaPagamento(),
            totalFinal,
            parcelas
        );

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntrega,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(ResumoRequest request) {
        List<ItemRequest> itens = request.getItens();

        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        String modalidade = request.getModalidadeEntrega();
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (Modalidade.fromCodigo(modalidade) == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (!validarModalidadeDisponivel(modalidade, calcularPesoTotal(itens))) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        String cupom = request.getCupom();
        if (cupom != null && !cupom.isEmpty()) {
            if (Cupom.fromCodigo(cupom) == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            if (!validarCupomAplicavel(cupom, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        String forma = request.getFormaPagamento();
        if (forma == null || forma.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (FormaPagamento.fromCodigo(forma) == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!validarParcelas(forma, parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal desconto = calcularDescontoCupom(cupom, subtotal, itens);
        BigDecimal frete = calcularFrete(modalidade, itens, cupom);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).setScale(2, ROUNDING);

        if (!validarFormaPagamentoDisponivel(forma, totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private boolean validarModalidadeDisponivel(String modalidade, double pesoTotal) {
        if ("MOTOBOY".equals(modalidade)) {
            return pesoTotal <= LIMITE_MOTOBOY_KG;
        }
        return true;
    }

    private boolean validarCupomAplicavel(String cupom, List<ItemRequest> itens) {
        if ("MENOS50".equals(cupom)) {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                subtotal = subtotal.add(
                    BigDecimal.valueOf(item.getPrecoUnitario() * item.getQuantidade())
                );
            }
            subtotal = subtotal.setScale(2, ROUNDING);
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        return true;
    }

    private boolean validarParcelas(String forma, Integer parcelas) {
        if ("PIX".equals(forma) || "BOLETO".equals(forma)) {
            return parcelas == 1;
        }
        if ("CARTAO".equals(forma)) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean validarFormaPagamentoDisponivel(String forma, BigDecimal totalPedido) {
        if ("BOLETO".equals(forma)) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }
        return true;
    }

    private double calcularPesoTotal(List<ItemRequest> itens) {
        double total = 0;
        for (ItemRequest item : itens) {
            total += item.getPesoKg() * item.getQuantidade();
        }
        return total;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal precoItem = BigDecimal.valueOf(item.getPrecoUnitario());
            int quantidade = item.getQuantidade();
            subtotal = subtotal.add(
                precoItem.multiply(BigDecimal.valueOf(quantidade))
            );
        }

        return subtotal.setScale(2, ROUNDING);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, ROUNDING);
        }

        if ("BEMVINDO10".equals(cupom)) {
            return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, ROUNDING);
        }

        if ("MENOS50".equals(cupom)) {
            return new BigDecimal("50.00");
        }

        if ("FRETEGRATIS".equals(cupom)) {
            return BigDecimal.ZERO.setScale(2, ROUNDING);
        }

        if ("LEVE3PAGUE2".equals(cupom)) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                BigDecimal precoItem = BigDecimal.valueOf(item.getPrecoUnitario());
                int quantidade = item.getQuantidade();
                int trios = quantidade / 3;
                desconto = desconto.add(precoItem.multiply(BigDecimal.valueOf(trios)));
            }
            return desconto.setScale(2, ROUNDING);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularFrete(String modalidade, List<ItemRequest> itens, String cupom) {
        if ("FRETEGRATIS".equals(cupom)) {
            return BigDecimal.ZERO;
        }

        Modalidade m = Modalidade.fromCodigo(modalidade);
        if (m == null) {
            return BigDecimal.ZERO;
        }

        double pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = m.getTaxaBase()
            .add(m.getTaxaPorKg().multiply(BigDecimal.valueOf(pesoTotal)));

        return frete.setScale(2, ROUNDING);
    }

    private Integer obterPrazoEntrega(String modalidade) {
        Modalidade m = Modalidade.fromCodigo(modalidade);
        return m != null ? m.getPrazoDias() : 0;
    }

    private BigDecimal calcularAjustePagamento(String forma, BigDecimal totalPedido, Integer parcelas) {
        if ("PIX".equals(forma)) {
            return totalPedido.multiply(TAXA_PIX)
                .negate()
                .setScale(2, ROUNDING);
        }

        if ("BOLETO".equals(forma)) {
            return TARIFA_BOLETO;
        }

        if ("CARTAO".equals(forma)) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO.setScale(2, ROUNDING);
            }

            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
            java.math.BigDecimal potencia = umMaisTaxa.pow(parcelas, new java.math.MathContext(15));
            BigDecimal divisor = potencia.subtract(BigDecimal.ONE);

            BigDecimal parcela = totalPedido
                .multiply(TAXA_JUROS_MENSAL)
                .multiply(potencia)
                .divide(divisor, 2, ROUNDING);

            BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas))
                .setScale(2, ROUNDING);

            return totalComJuros.subtract(totalPedido).setScale(2, ROUNDING);
        }

        return BigDecimal.ZERO.setScale(2, ROUNDING);
    }

    private BigDecimal calcularValorParcela(String forma, BigDecimal totalFinal, Integer parcelas) {
        if ("CARTAO".equals(forma) && parcelas > 3) {
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
            java.math.BigDecimal potencia = umMaisTaxa.pow(parcelas, new java.math.MathContext(15));
            BigDecimal divisor = potencia.subtract(BigDecimal.ONE);

            return totalFinal
                .multiply(TAXA_JUROS_MENSAL)
                .multiply(potencia)
                .divide(divisor, 2, ROUNDING);
        }

        return totalFinal.divide(BigDecimal.valueOf(parcelas), 2, ROUNDING);
    }
}
