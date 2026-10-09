package com.loja.checkout.service;

import com.loja.checkout.model.*;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final CalculadorFrete calculadorFrete = new CalculadorFrete();
    private final CalculadorCupom calculadorCupom = new CalculadorCupom();
    private final CalculadorSeguro calculadorSeguro = new CalculadorSeguro();
    private final CalculadorCredito calculadorCredito = new CalculadorCredito();
    private final CalculadorPagamento calculadorPagamento = new CalculadorPagamento();

    public Object processar(CheckoutRequest request) {
        var validacao = validar(request);
        if (validacao != null) {
            return validacao;
        }

        return calcular(request);
    }

    private ErrorResponse validar(CheckoutRequest request) {
        if (!validarPedido(request)) {
            return new ErrorResponse("PEDIDO_INVALIDO");
        }

        if (!validarNivelClube(request)) {
            return new ErrorResponse("NIVEL_CLUBE_INVALIDO");
        }

        if (!validarRegiao(request)) {
            return new ErrorResponse("REGIAO_INVALIDA");
        }

        if (!validarModalidadeEntrega(request)) {
            return new ErrorResponse("MODALIDADE_INVALIDA");
        }

        if (!validarDisponibilidadeModalidade(request)) {
            return new ErrorResponse("MODALIDADE_INDISPONIVEL");
        }

        if (!validarCupom(request)) {
            return new ErrorResponse("CUPOM_INVALIDO");
        }

        if (!validarAplicabilidadeCupom(request)) {
            return new ErrorResponse("CUPOM_NAO_APLICAVEL");
        }

        if (!validarFormaPagamento(request)) {
            return new ErrorResponse("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!validarParcelamento(request)) {
            return new ErrorResponse("PARCELAMENTO_INVALIDO");
        }

        if (!validarDisponibilidadeFormaPagamento(request)) {
            return new ErrorResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return null;
    }

    private boolean validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return false;
        }

        for (Item item : request.getItens()) {
            if (item.getNome() == null || item.getNome().isEmpty() ||
                item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                return false;
            }
        }

        return true;
    }

    private boolean validarNivelClube(CheckoutRequest request) {
        if (request.getNivelClube() == null) {
            return false;
        }
        return Arrays.asList(NivelClube.values()).contains(request.getNivelClube());
    }

    private boolean validarRegiao(CheckoutRequest request) {
        if (request.getRegiao() == null) {
            return false;
        }
        return Arrays.asList(Regiao.values()).contains(request.getRegiao());
    }

    private boolean validarModalidadeEntrega(CheckoutRequest request) {
        if (request.getModalidadeEntrega() == null) {
            return false;
        }
        return Arrays.asList(ModalidadeEntrega.values()).contains(request.getModalidadeEntrega());
    }

    private boolean validarDisponibilidadeModalidade(CheckoutRequest request) {
        ModalidadeEntrega modalidade = request.getModalidadeEntrega();
        List<Item> itens = request.getItens();

        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            double pesoTotal = itens.stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
            if (pesoTotal > 5.0) {
                return false;
            }
        }

        return true;
    }

    private boolean validarCupom(CheckoutRequest request) {
        String cupom = request.getCupom();
        if (cupom == null) {
            return true;
        }

        try {
            Cupom.valueOf(cupom);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean validarAplicabilidadeCupom(CheckoutRequest request) {
        String codigoCupom = request.getCupom();
        if (codigoCupom == null) {
            return true;
        }

        Cupom cupom = Cupom.valueOf(codigoCupom);
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        if (cupom == Cupom.MENOS50) {
            return subtotalProdutos.compareTo(Arredondador.arredondar(300.0)) >= 0;
        }

        return true;
    }

    private boolean validarFormaPagamento(CheckoutRequest request) {
        if (request.getFormaPagamento() == null) {
            return false;
        }
        return Arrays.asList(FormaPagamento.values()).contains(request.getFormaPagamento());
    }

    private boolean validarParcelamento(CheckoutRequest request) {
        FormaPagamento forma = request.getFormaPagamento();
        int parcelas = request.getParcelas();

        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            return parcelas == 1;
        }

        if (forma == FormaPagamento.CARTAO) {
            return parcelas >= 1 && parcelas <= 12;
        }

        return false;
    }

    private boolean validarDisponibilidadeFormaPagamento(CheckoutRequest request) {
        FormaPagamento forma = request.getFormaPagamento();

        if (forma == FormaPagamento.BOLETO) {
            BigDecimal total = calcularTotal(request);
            if (total.compareTo(Arredondador.arredondar(1000.0)) > 0) {
                return false;
            }
        }

        return true;
    }

    private CheckoutResponse calcular(CheckoutRequest request) {
        List<Item> itens = request.getItens();
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        CalculadorFrete.ResultadoFrete frete = calculadorFrete.calcular(itens, request.getModalidadeEntrega());

        BigDecimal descontoCupom = calculadorCupom.calcular(request.getCupom(), itens, subtotalProdutos, frete.valor());
        if (descontoCupom == null) {
            descontoCupom = BigDecimal.ZERO;
        }

        BigDecimal freteAplicado = frete.valor();
        if (request.getNivelClube() == NivelClube.OURO && request.getModalidadeEntrega() != ModalidadeEntrega.RETIRADA_LOJA) {
            freteAplicado = BigDecimal.ZERO;
        }

        BigDecimal seguro = calculadorSeguro.calcular(subtotalProdutos, request.getRegiao());

        BigDecimal totalAntesAjuste = subtotalProdutos
            .subtract(descontoCupom)
            .add(freteAplicado)
            .add(seguro);
        totalAntesAjuste = Arredondador.arredondar(totalAntesAjuste);

        CalculadorPagamento.ResultadoPagamento pagamento = calculadorPagamento.calcular(
            totalAntesAjuste, request.getFormaPagamento(), request.getParcelas()
        );

        BigDecimal creditoProximaCompra = calculadorCredito.calcular(subtotalProdutos, request.getNivelClube());

        boolean brinde = request.getNivelClube() == NivelClube.OURO && subtotalProdutos.compareTo(Arredondador.arredondar(500.0)) > 0;

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            freteAplicado,
            frete.prazo(),
            seguro,
            pagamento.ajuste(),
            pagamento.totalComAjuste(),
            pagamento.parcelas(),
            pagamento.valorParcela(),
            creditoProximaCompra,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return itens.stream()
            .map(item -> Arredondador.arredondar(item.getPrecoUnitario())
                .multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotal(CheckoutRequest request) {
        List<Item> itens = request.getItens();
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
        CalculadorFrete.ResultadoFrete frete = calculadorFrete.calcular(itens, request.getModalidadeEntrega());
        BigDecimal descontoCupom = calculadorCupom.calcular(request.getCupom(), itens, subtotalProdutos, frete.valor());
        if (descontoCupom == null) {
            descontoCupom = BigDecimal.ZERO;
        }
        BigDecimal seguro = calculadorSeguro.calcular(subtotalProdutos, request.getRegiao());

        return subtotalProdutos
            .subtract(descontoCupom)
            .add(frete.valor())
            .add(seguro);
    }
}
