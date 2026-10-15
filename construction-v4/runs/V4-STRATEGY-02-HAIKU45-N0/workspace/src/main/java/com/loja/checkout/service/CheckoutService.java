package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {
    private static final BigDecimal ONE = BigDecimal.ONE;

    public CheckoutResponse calcularResumo(CheckoutRequest request) throws Exception {
        validarEntrada(request);

        List<ItemPedido> itens = request.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        String cupomCod = request.getCupom();
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        NivelClube nivelClube = NivelClube.valueOf(request.getNivelClube());
        Regiao regiao = Regiao.valueOf(request.getRegiao());

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        BigDecimal frete = calcularFrete(modalidade, itens, nivelClube);
        BigDecimal descontoCupom = calcularDescontoCupom(cupomCod, subtotalProdutos, itens, frete);
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalAntesAjuste = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesAjuste, parcelas);
        BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);

        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, nivelClube);
        Boolean brinde = verificarBrinde(subtotalProdutos, nivelClube);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas, formaPagamento);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazo());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarEntrada(CheckoutRequest request) throws Exception {
        // 1. Validar carrinho
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new Exception("PEDIDO_INVALIDO");
        }
        for (ItemPedido item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                throw new Exception("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar nível do clube
        try {
            NivelClube.valueOf(request.getNivelClube());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new Exception("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Validar região
        try {
            Regiao.valueOf(request.getRegiao());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new Exception("REGIAO_INVALIDA");
        }

        // 4. Validar modalidade entrega
        try {
            ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new Exception("MODALIDADE_INVALIDA");
        }

        // 5. Validar disponibilidade da modalidade
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
            if (pesoTotal.compareTo(new BigDecimal("5")) > 0) {
                throw new Exception("MODALIDADE_INDISPONIVEL");
            }
        }

        // 6. Validar cupom
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            if (!isCupomValido(request.getCupom())) {
                throw new Exception("CUPOM_INVALIDO");
            }
            BigDecimal subtotal = calcularSubtotal(request.getItens());
            if (!isCupomAplicavel(request.getCupom(), subtotal)) {
                throw new Exception("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Validar forma de pagamento
        try {
            FormaPagamento.valueOf(request.getFormaPagamento());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new Exception("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Validar parcelamento
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());
        if (!isParcelamentoValido(formaPagamento, parcelas)) {
            throw new Exception("PARCELAMENTO_INVALIDO");
        }

        // 10. Validar disponibilidade forma pagamento
        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
        BigDecimal frete = calcularFrete(modalidade, request.getItens(), NivelClube.valueOf(request.getNivelClube()));
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, request.getItens(), frete);
        BigDecimal seguro = calcularSeguro(subtotalProdutos, Regiao.valueOf(request.getRegiao()));
        BigDecimal total = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        if (formaPagamento == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000")) > 0) {
            throw new Exception("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private boolean isCupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") ||
               cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") ||
               cupom.equals("LEVE3PAGUE2");
    }

    private boolean isCupomAplicavel(String cupom, BigDecimal subtotal) {
        if (cupom.equals("MENOS50")) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }
        return true;
    }

    private boolean isParcelamentoValido(FormaPagamento formaPagamento, Integer parcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            return parcelas == 1;
        }
        if (formaPagamento == FormaPagamento.CARTAO) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemPedido> itens, BigDecimal frete) {
        if (cupom == null || cupom.isEmpty()) {
            return zero();
        }

        if (cupom.equals("BEMVINDO10")) {
            return arredondar(subtotal.multiply(new BigDecimal("0.10")));
        } else if (cupom.equals("MENOS50")) {
            return arredondar(BigDecimal.valueOf(50));
        } else if (cupom.equals("FRETEGRATIS")) {
            return arredondar(frete);
        } else if (cupom.equals("LEVE3PAGUE2")) {
            BigDecimal subtotalComCupom = calcularSubtotalLeve3Pague2(itens);
            return arredondar(subtotal.subtract(subtotalComCupom));
        }

        return zero();
    }

    private BigDecimal calcularSubtotalLeve3Pague2(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemPedido item : itens) {
            int quantidadeAPagar = item.getQuantidade() - (item.getQuantidade() / 3);
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidadeAPagar)));
        }

        return arredondar(subtotal);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemPedido> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return zero();
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return arredondar(modalidade.calcularFrete(pesoTotal));
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            pesoTotal = pesoTotal.add(item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }
        return arredondar(pesoTotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        return arredondar(subtotal.multiply(regiao.getSeguroPercentual()));
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, Integer parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            return arredondar(total.multiply(new BigDecimal("-0.05")));
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            return arredondar(new BigDecimal("3.49"));
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                return zero();
            } else {
                BigDecimal taxa = new BigDecimal("0.0199");
                BigDecimal juros = calcularJuros(total, taxa, parcelas);
                return arredondar(juros);
            }
        }
        return zero();
    }

    private BigDecimal calcularJuros(BigDecimal principal, BigDecimal taxaMensal, Integer parcelas) {
        BigDecimal um = ONE;
        BigDecimal taxa = taxaMensal.add(um);
        BigDecimal denominador = um.subtract(taxa.pow(parcelas * -1, new java.math.MathContext(128)));
        BigDecimal parcela = principal.multiply(taxaMensal).divide(denominador, 10, RoundingMode.HALF_EVEN);
        parcela = arredondar(parcela);
        BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal juros = totalComJuros.subtract(principal);
        return arredondar(juros);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, Integer parcelas, FormaPagamento formaPagamento) {
        BigDecimal parcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN);
        return arredondar(parcela);
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivelClube) {
        return arredondar(subtotal.multiply(nivelClube.getCreditoPercentual()));
    }

    private boolean verificarBrinde(BigDecimal subtotal, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500")) > 0) {
            return true;
        }
        return false;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
}
