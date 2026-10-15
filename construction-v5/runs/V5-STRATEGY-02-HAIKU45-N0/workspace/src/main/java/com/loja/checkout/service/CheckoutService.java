package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public CheckoutResponse calcular(CheckoutRequest request) {
        try {
            return validarECalcular(request);
        } catch (ValidationException e) {
            return CheckoutResponse.error(e.getErrorCode());
        }
    }

    private CheckoutResponse validarECalcular(CheckoutRequest request) {
        // 1. VALIDAÇÃO: Carrinho válido
        validarPedido(request);

        // 2. VALIDAÇÃO: Nível do clube
        NivelClube nivelClube = validarNivelClube(request.nivelClube());

        // 3. VALIDAÇÃO: Região
        Regiao regiao = validarRegiao(request.regiao());

        // 4. VALIDAÇÃO: Modalidade entrega
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());

        // Calcular peso total
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        // 5. VALIDAÇÃO: Modalidade disponível para este pedido
        validarDisponibilidade(modalidade, pesoTotal);

        // 6. VALIDAÇÃO: Cupom válido
        Optional<Cupom> cupom = Optional.empty();
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            cupom = validarCupom(request.cupom());
        }

        // 7. VALIDAÇÃO: Cupom aplicável
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        validarAplicabilidadeCupom(cupom, subtotalProdutos);

        // 8. VALIDAÇÃO: Forma de pagamento
        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());

        // 9. VALIDAÇÃO: Parcelas válida
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);

        // CÁLCULO DOS VALORES
        // Primeiro calcular frete sem considerar FRETEGRATIS
        BigDecimal freteOriginal = calcularFrete(modalidade, pesoTotal, nivelClube);

        // Depois calcular desconto (que pode ser baseado no frete para FRETEGRATIS)
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, freteOriginal, request.itens());

        // Se FRETEGRATIS, o frete aparece como zero na resposta
        BigDecimal frete = (cupom.isPresent() && cupom.get() == Cupom.FRETEGRATIS) ? arredondar(BigDecimal.ZERO) : freteOriginal;

        int prazoEntrega = modalidade.getPrazoDias();
        BigDecimal seguro = calcularSeguro(regiao, subtotalProdutos);

        // Subtotal: produtos - desconto + frete + seguro
        BigDecimal totalAntesAjuste = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 10. VALIDAÇÃO: Forma de pagamento disponível
        validarDisponibilidadeFormaPagamento(formaPagamento, totalAntesAjuste);

        // Ajuste de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesAjuste, parcelas);
        BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);

        // Cálculo de parcelas
        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        // Crédito da próxima compra
        BigDecimal creditoProxima = calcularCredito(nivelClube, subtotalProdutos);

        // Brinde
        boolean brinde = (nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntrega,
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProxima,
                brinde,
                null
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ValidationException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.itens()) {
            if (item.nome() == null || item.nome().isEmpty() ||
                    item.precoUnitario() <= 0 ||
                    item.quantidade() <= 0 ||
                    item.pesoKg() < 0) {
                throw new ValidationException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null || nivel.isEmpty()) {
            throw new ValidationException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            throw new ValidationException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }
    }

    private void validarDisponibilidade(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new ValidationException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Optional<Cupom> validarCupom(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return Optional.empty();
        }
        return Cupom.byCodigo(codigoCupom)
                .or(() -> { throw new ValidationException("CUPOM_INVALIDO"); });
    }

    private void validarAplicabilidadeCupom(Optional<Cupom> cupom, BigDecimal subtotalProdutos) {
        if (cupom.isPresent() && cupom.get() == Cupom.MENOS50) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new ValidationException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private FormaPagamento validarFormaPagamento(String forma) {
        if (forma == null || forma.isEmpty()) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(forma);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelas(FormaPagamento forma, int parcelas) {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new ValidationException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new ValidationException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento forma, BigDecimal total) {
        if (forma == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> BigDecimal.valueOf(item.pesoKg() * item.quantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal precoUnitario = BigDecimal.valueOf(item.precoUnitario());
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(precoUnitario.multiply(quantidade));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(Optional<Cupom> cupom, BigDecimal subtotalProdutos, BigDecimal freteOriginal, List<ItemRequest> itens) {
        if (cupom.isEmpty()) {
            return arredondar(BigDecimal.ZERO);
        }

        switch (cupom.get()) {
            case BEMVINDO10:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case MENOS50:
                return arredondar(new BigDecimal("50.00"));
            case FRETEGRATIS:
                // Desconto igual ao valor do frete
                return freteOriginal;
            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);
            default:
                return arredondar(BigDecimal.ZERO);
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int quantidadeGratis = item.quantidade() / 3;
            BigDecimal precoUnitario = BigDecimal.valueOf(item.precoUnitario());
            desconto = desconto.add(precoUnitario.multiply(BigDecimal.valueOf(quantidadeGratis)));
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return arredondar(BigDecimal.ZERO);
        }

        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return arredondar(BigDecimal.ZERO);
        }

        BigDecimal frete = modalidade.getTaxaBase()
                .add(modalidade.getTaxaPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(Regiao regiao, BigDecimal subtotalProdutos) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return arredondar(seguro);
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(nivelClube.getPercentualCredito());
        return arredondar(credito);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento forma, BigDecimal total, int parcelas) {
        if (forma == FormaPagamento.PIX) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
            return arredondar(desconto).negate();
        } else if (forma == FormaPagamento.BOLETO) {
            return arredondar(new BigDecimal("3.49"));
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                return arredondar(BigDecimal.ZERO);
            }
            // Juros: tabela Price
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal parcelasDecimal = BigDecimal.valueOf(parcelas);

            // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.add(taxaMensal).pow(-parcelas, new java.math.MathContext(10))
            );
            BigDecimal parcela = total.multiply(taxaMensal).divide(denominador, SCALE, ROUNDING);
            BigDecimal totalComJuros = parcela.multiply(parcelasDecimal);
            BigDecimal juros = totalComJuros.subtract(total);
            return arredondar(juros);
        }
        return arredondar(BigDecimal.ZERO);
    }

    private BigDecimal calcularValorParcela(FormaPagamento forma, BigDecimal totalFinal, int parcelas) {
        if (parcelas == 1) {
            return totalFinal;
        }
        BigDecimal parcela = totalFinal.divide(BigDecimal.valueOf(parcelas), SCALE, ROUNDING);
        return parcela;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return valor.setScale(SCALE, ROUNDING);
    }

    private static class ValidationException extends RuntimeException {
        private final String errorCode;

        public ValidationException(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
