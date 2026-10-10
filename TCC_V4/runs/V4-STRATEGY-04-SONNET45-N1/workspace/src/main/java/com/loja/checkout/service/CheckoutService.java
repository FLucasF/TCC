package com.loja.checkout.service;

import com.loja.checkout.clube.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.CodigoErro;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.modalidade.*;
import com.loja.checkout.pagamento.*;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeStrategy> modalidades = new HashMap<>();
    private final Map<String, CupomStrategy> cupons = new HashMap<>();
    private final Map<String, PagamentoStrategy> formasPagamento = new HashMap<>();
    private final Map<NivelClube, ClubeStrategy> niveisClube = new HashMap<>();

    public CheckoutService() {
        modalidades.put("ECONOMICA", new EconomicaStrategy());
        modalidades.put("EXPRESSA", new ExpressaStrategy());
        modalidades.put("RETIRADA_LOJA", new RetiradaLojaStrategy());
        modalidades.put("MOTOBOY", new MotoboyStrategy());

        cupons.put("BEMVINDO10", new BemVindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());

        formasPagamento.put("PIX", new PixStrategy());
        formasPagamento.put("CARTAO", new CartaoStrategy());
        formasPagamento.put("BOLETO", new BoletoStrategy());

        niveisClube.put(NivelClube.BRONZE, new BronzeStrategy());
        niveisClube.put(NivelClube.PRATA, new PrataStrategy());
        niveisClube.put(NivelClube.OURO, new OuroStrategy());
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarPedido(request);

        NivelClube nivelClube = validarNivelClube(request.getNivelClube());
        Regiao regiao = validarRegiao(request.getRegiao());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        ModalidadeStrategy modalidade = validarModalidade(request.getModalidadeEntrega(), pesoTotal);

        ClubeStrategy clube = niveisClube.get(nivelClube);
        BigDecimal frete = clube.temFreteGratis()
            ? BigDecimal.ZERO.setScale(2)
            : modalidade.calcularFrete(pesoTotal);

        CupomStrategy cupom = validarCupom(request.getCupom(), subtotalProdutos, request.getItens());
        BigDecimal descontoCupom = cupom != null
            ? cupom.calcularDesconto(subtotalProdutos, frete, request.getItens())
            : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalPedido = Arredondamento.arredondar(totalPedido);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        PagamentoStrategy pagamento = validarPagamento(request.getFormaPagamento(), parcelas, totalPedido);

        BigDecimal ajustePagamento = pagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = pagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal valorParcela = pagamento.calcularValorParcela(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.temBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.obterPrazo());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private NivelClube validarNivelClube(String nivelStr) {
        if (nivelStr == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        try {
            return NivelClube.valueOf(nivelStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private Regiao validarRegiao(String regiaoStr) {
        if (regiaoStr == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        try {
            return Regiao.valueOf(regiaoStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private ModalidadeStrategy validarModalidade(String modalidadeStr, BigDecimal pesoTotal) {
        if (modalidadeStr == null || !modalidades.containsKey(modalidadeStr)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        ModalidadeStrategy modalidade = modalidades.get(modalidadeStr);
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        return modalidade;
    }

    private CupomStrategy validarCupom(String cupomStr, BigDecimal subtotalProdutos, java.util.List<ItemCarrinho> itens) {
        if (cupomStr == null || cupomStr.isEmpty()) {
            return null;
        }

        if (!cupons.containsKey(cupomStr)) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }

        CupomStrategy cupom = cupons.get(cupomStr);
        if (!cupom.ehAplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        return cupom;
    }

    private PagamentoStrategy validarPagamento(String formaPagamentoStr, int parcelas, BigDecimal totalPedido) {
        if (formaPagamentoStr == null || !formasPagamento.containsKey(formaPagamentoStr)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        PagamentoStrategy pagamento = formasPagamento.get(formaPagamentoStr);

        if (!pagamento.validarParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        if (!pagamento.estaDisponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        return pagamento;
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal totalItem = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(totalItem);
        }

        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return Arredondamento.arredondar(seguro);
    }
}
