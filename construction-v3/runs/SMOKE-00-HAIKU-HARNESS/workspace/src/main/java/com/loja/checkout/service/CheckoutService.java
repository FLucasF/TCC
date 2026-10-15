package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        // Validação 1: Pedido válido
        validarPedido(request);

        // Validação 2: Modalidade válida
        String modalidadeStr = request.getModalidadeEntrega();
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(modalidadeStr);
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // Cálculo de peso total
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        // Validação 3: Modalidade disponível para o peso
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Validação 4: Cupom válido (se informado)
        Cupom cupom = null;
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            cupom = Cupom.fromCodigo(request.getCupom());
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }

        // Cálculo de subtotal
        BigDecimal subtotal = calcularSubtotal(request.getItens());

        // Validação 5: Cupom aplicável
        if (cupom != null && !cupom.isAplicavel(subtotal)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // Validação 6: Forma de pagamento válida
        String formaPagamentoStr = request.getFormaPagamento();
        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(formaPagamentoStr);
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // Cálculo de desconto do cupom
        BigDecimal desconto = cupom != null && cupom != Cupom.FRETEGRATIS
                ? cupom.calcularDesconto(subtotal, request.getItens())
                : BigDecimal.ZERO;
        desconto = desconto.setScale(SCALE, ROUNDING);

        // Cálculo de frete
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);
        frete = frete.setScale(SCALE, ROUNDING);

        // Se for FRETEGRATIS, o desconto é igual ao frete
        if (cupom == Cupom.FRETEGRATIS) {
            desconto = frete;
        }

        // Total sem ajuste de pagamento
        BigDecimal total = subtotal.subtract(desconto).add(frete)
                .setScale(SCALE, ROUNDING);

        // Validação 7 e 8: Parcelamento e forma de pagamento disponível
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas, total);

        // Cálculo de ajuste de pagamento
        BigDecimal ajuste = calcularAjustePagamento(formaPagamento, parcelas, total);
        ajuste = ajuste.setScale(SCALE, ROUNDING);

        // Total final
        BigDecimal totalFinal = total.add(ajuste).setScale(SCALE, ROUNDING);

        // Cálculo de valor da parcela
        BigDecimal valorParcela = calcularValorParcela(formaPagamento, parcelas, totalFinal);
        valorParcela = valorParcela.setScale(SCALE, ROUNDING);

        return new CheckoutResponse(
                subtotal,
                desconto,
                frete,
                modalidade.getPrazoEntregaDias(),
                ajuste,
                totalFinal,
                parcelas,
                valorParcela
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getQuantidade() == null ||
                item.getPesoKg() == null) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }

            if (item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal preco = item.getPrecoUnitario()
                    .multiply(new BigDecimal(item.getQuantidade()))
                    .setScale(SCALE, ROUNDING);
            subtotal = subtotal.add(preco);
        }
        return subtotal.setScale(SCALE, ROUNDING);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getTaxaBase()
                .add(modalidade.getTaxaPorKg().multiply(pesoTotal));
        return frete.setScale(SCALE, ROUNDING);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas, BigDecimal total) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        // Validação 8: Boleto acima de R$ 1.000,00
        if (formaPagamento == FormaPagamento.BOLETO &&
            total.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, int parcelas, BigDecimal total) {
        if (formaPagamento == FormaPagamento.PIX) {
            // Desconto de 5%
            return total.multiply(new BigDecimal("0.05"))
                    .setScale(SCALE, ROUNDING)
                    .negate();
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            // Tarifa de R$ 3,49
            return new BigDecimal("3.49");
        }

        if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                // Sem juros
                return BigDecimal.ZERO;
            } else {
                // Com juros de 1,99% ao mês (tabela Price)
                BigDecimal taxaMensal = new BigDecimal("0.0199");
                BigDecimal numerador = total.multiply(taxaMensal);
                BigDecimal denominador = BigDecimal.ONE.subtract(
                        BigDecimal.ONE.add(taxaMensal)
                                .pow(-parcelas, new java.math.MathContext(50))
                );
                BigDecimal parcela = numerador.divide(denominador, 50, RoundingMode.HALF_EVEN)
                        .setScale(SCALE, ROUNDING);
                BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas))
                        .setScale(SCALE, ROUNDING);
                return totalComJuros.subtract(total)
                        .setScale(SCALE, ROUNDING);
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularValorParcela(FormaPagamento formaPagamento, int parcelas, BigDecimal totalFinal) {
        if (parcelas <= 1) {
            return totalFinal;
        }

        return totalFinal.divide(new BigDecimal(parcelas), SCALE, ROUNDING);
    }
}
