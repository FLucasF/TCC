package com.loja.service;

import com.loja.domain.Cupom;
import com.loja.domain.ModalidadeEntrega;
import com.loja.dto.ItemPedido;
import com.loja.dto.ResumoRequisicao;
import com.loja.dto.ResumoResposta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ResumoService {

    public ResumoResposta calcularResumo(ResumoRequisicao requisicao) {
        validarRequisicao(requisicao);

        Double subtotalProdutos = calcularSubtotalProdutos(requisicao);

        Double descontoCupom = calcularDescontoCupom(requisicao, subtotalProdutos);

        Double frete = calcularFrete(requisicao);

        Integer prazo = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega()).getPrazoDias();

        Double totalAntesPagamento = arredondar(subtotalProdutos - descontoCupom + frete);

        Integer parcelas = requisicao.getParcelas();
        String forma = requisicao.getFormaPagamento();

        Double ajustePagamento = calcularAjustePagamento(forma, totalAntesPagamento, parcelas);

        Double totalFinal = arredondar(totalAntesPagamento + ajustePagamento);

        Double valorParcela = calcularValorParcela(forma, totalFinal, parcelas);

        return new ResumoResposta(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazo,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarRequisicao(ResumoRequisicao requisicao) {
        validarCarrinho(requisicao.getItens());
        validarModalidadeEntrega(requisicao);
        validarCupom(requisicao);
        validarFormaPagamento(requisicao);
    }

    private void validarCarrinho(java.util.List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ValidationException("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
                throw new ValidationException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidadeEntrega(ResumoRequisicao requisicao) {
        String modalidade = requisicao.getModalidadeEntrega();

        if (modalidade == null || modalidade.isEmpty()) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega entrega;
        try {
            entrega = ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }

        if (entrega == ModalidadeEntrega.MOTOBOY) {
            Double pesoTotal = requisicao.getItens().stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();

            if (pesoTotal > 5.0) {
                throw new ValidationException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarCupom(ResumoRequisicao requisicao) {
        String codigoCupom = requisicao.getCupom();

        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return;
        }

        Cupom cupom = Cupom.fromCodigo(codigoCupom);
        if (cupom == null) {
            throw new ValidationException("CUPOM_INVALIDO");
        }

        if (cupom == Cupom.MENOS50) {
            Double subtotal = calcularSubtotalProdutos(requisicao);
            if (subtotal < 300.0) {
                throw new ValidationException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarFormaPagamento(ResumoRequisicao requisicao) {
        String forma = requisicao.getFormaPagamento();

        if (forma == null || forma.isEmpty()) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!forma.equals("PIX") && !forma.equals("CARTAO") && !forma.equals("BOLETO")) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = requisicao.getParcelas();

        if (forma.equals("PIX") || forma.equals("BOLETO")) {
            if (parcelas != null && parcelas != 1) {
                throw new ValidationException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma.equals("CARTAO")) {
            if (parcelas == null || parcelas < 1 || parcelas > 12) {
                throw new ValidationException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private Double calcularSubtotalProdutos(ResumoRequisicao requisicao) {
        Double subtotal = requisicao.getItens().stream()
            .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
            .sum();

        return arredondar(subtotal);
    }

    private Double calcularDescontoCupom(ResumoRequisicao requisicao, Double subtotalProdutos) {
        String codigoCupom = requisicao.getCupom();

        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return 0.0;
        }

        Cupom cupom = Cupom.fromCodigo(codigoCupom);
        if (cupom == null) {
            return 0.0;
        }

        Double desconto = 0.0;

        if (cupom == Cupom.BEMVINDO10) {
            desconto = arredondar(subtotalProdutos * 0.10);
        } else if (cupom == Cupom.MENOS50) {
            desconto = 50.0;
        } else if (cupom == Cupom.FRETEGRATIS) {
            Double frete = calcularFrete(requisicao);
            desconto = frete;
        } else if (cupom == Cupom.LEVE3PAGUE2) {
            desconto = calcularDescontoLeve3Pague2(requisicao.getItens());
        }

        return arredondar(desconto);
    }

    private Double calcularDescontoLeve3Pague2(java.util.List<ItemPedido> itens) {
        Double desconto = 0.0;

        for (ItemPedido item : itens) {
            Integer quantidade = item.getQuantidade();
            if (quantidade >= 3) {
                Integer unidadesGratis = quantidade / 3;
                Double valorUnidadesGratis = arredondar(item.getPrecoUnitario() * unidadesGratis);
                desconto += valorUnidadesGratis;
            }
        }

        return arredondar(desconto);
    }

    private Double calcularFrete(ResumoRequisicao requisicao) {
        ModalidadeEntrega entrega = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());

        Double pesoTotal = requisicao.getItens().stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();

        Double frete = entrega.calcularFrete(pesoTotal);

        return arredondar(frete);
    }

    private Double calcularAjustePagamento(String forma, Double totalAntesPagamento, Integer parcelas) {
        if (forma.equals("PIX")) {
            return -arredondar(totalAntesPagamento * 0.05);
        } else if (forma.equals("BOLETO")) {
            if (totalAntesPagamento > 1000.0) {
                throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
            return 3.49;
        } else if (forma.equals("CARTAO")) {
            if (parcelas <= 3) {
                return 0.0;
            } else {
                return calcularJurosCartao(totalAntesPagamento, parcelas);
            }
        }

        return 0.0;
    }

    private Double calcularJurosCartao(Double total, Integer parcelas) {
        Double taxaMensal = 0.0199;

        Double parcela = total * (taxaMensal / (1.0 - Math.pow(1.0 + taxaMensal, -parcelas)));
        parcela = arredondar(parcela);

        Double totalComJuros = arredondar(parcela * parcelas);

        return arredondar(totalComJuros - total);
    }

    private Double calcularValorParcela(String forma, Double totalFinal, Integer parcelas) {
        return arredondar(totalFinal / parcelas);
    }

    private Double arredondar(Double valor) {
        BigDecimal bd = new BigDecimal(valor);
        bd = bd.setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }

    public static class ValidationException extends RuntimeException {
        private final String codigo;

        public ValidationException(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
