package com.loja.service;

import com.loja.domain.Regiao;
import com.loja.domain.clube.Beneficios;
import com.loja.domain.clube.Bronze;
import com.loja.domain.clube.NivelClube;
import com.loja.domain.clube.Ouro;
import com.loja.domain.clube.Prata;
import com.loja.domain.cupom.BemVindo10;
import com.loja.domain.cupom.Cupom;
import com.loja.domain.cupom.FreteGratis;
import com.loja.domain.cupom.Leve3Pague2;
import com.loja.domain.cupom.Menos50;
import com.loja.domain.cupom.ResultadoDesconto;
import com.loja.domain.entrega.Economica;
import com.loja.domain.entrega.Expressa;
import com.loja.domain.entrega.ModalidadeEntrega;
import com.loja.domain.entrega.Motoboy;
import com.loja.domain.entrega.RetiradaLoja;
import com.loja.domain.entrega.ResultadoFrete;
import com.loja.domain.pagamento.Boleto;
import com.loja.domain.pagamento.Cartao;
import com.loja.domain.pagamento.FormaPagamento;
import com.loja.domain.pagamento.Pix;
import com.loja.domain.pagamento.ResultadoPagamento;
import com.loja.exception.CheckoutException;
import com.loja.model.ItemCarrinho;
import com.loja.model.PedidoRequest;
import com.loja.model.ResumoResponse;
import com.loja.util.Moeda;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumoService {
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService() {
        modalidades = new HashMap<>();
        modalidades.put("ECONOMICA", new Economica());
        modalidades.put("EXPRESSA", new Expressa());
        modalidades.put("RETIRADA_LOJA", new RetiradaLoja());
        modalidades.put("MOTOBOY", new Motoboy());

        cupons = new HashMap<>();
        cupons.put("BEMVINDO10", new BemVindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());

        niveisClube = new HashMap<>();
        niveisClube.put("BRONZE", new Bronze());
        niveisClube.put("PRATA", new Prata());
        niveisClube.put("OURO", new Ouro());

        formasPagamento = new HashMap<>();
        formasPagamento.put("PIX", new Pix());
        formasPagamento.put("CARTAO", new Cartao());
        formasPagamento.put("BOLETO", new Boleto());
    }

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());

        Regiao regiao = obterRegiao(pedido.getRegiao());
        NivelClube nivelClube = obterNivelClube(pedido.getNivelClube());
        ModalidadeEntrega modalidadeEntrega = obterModalidadeEntrega(pedido.getModalidadeEntrega(), pesoTotal);

        ResultadoFrete resultadoFrete = modalidadeEntrega.calcularFrete(pesoTotal);
        Beneficios beneficios = nivelClube.calcularBeneficios(subtotalProdutos);

        BigDecimal frete = beneficios.isFreteGratis() ? new BigDecimal("0.00") : resultadoFrete.getValor();

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            Cupom cupom = obterCupom(pedido.getCupom());
            if (!cupom.aplicavel(subtotalProdutos, pedido.getItens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            ResultadoDesconto resultadoDesconto = cupom.calcularDesconto(subtotalProdutos, frete, pedido.getItens());
            descontoCupom = resultadoDesconto.getDesconto();
        }

        BigDecimal seguro = Moeda.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
        BigDecimal totalPedido = Moeda.arredondar(
            subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro)
        );

        FormaPagamento formaPagamento = obterFormaPagamento(pedido.getFormaPagamento());
        int parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;

        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (!formaPagamento.aceita(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(resultadoFrete.getPrazoDias());
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(resultadoPagamento.getAjuste());
        resposta.setTotalFinal(resultadoPagamento.getTotalFinal());
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(resultadoPagamento.getValorParcela());
        resposta.setCreditoProximaCompra(beneficios.getCredito());
        resposta.setBrinde(beneficios.isBrinde());

        return resposta;
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = new BigDecimal("0.00");
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return Moeda.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private Regiao obterRegiao(String codigoRegiao) {
        if (codigoRegiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigoRegiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private NivelClube obterNivelClube(String codigoNivel) {
        if (codigoNivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        NivelClube nivel = niveisClube.get(codigoNivel);
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return nivel;
    }

    private ModalidadeEntrega obterModalidadeEntrega(String codigoModalidade, BigDecimal pesoTotal) {
        if (codigoModalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = modalidades.get(codigoModalidade);
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        if (!modalidade.aceita(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return modalidade;
    }

    private Cupom obterCupom(String codigoCupom) {
        Cupom cupom = cupons.get(codigoCupom);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private FormaPagamento obterFormaPagamento(String codigoFormaPagamento) {
        if (codigoFormaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento = formasPagamento.get(codigoFormaPagamento);
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return formaPagamento;
    }
}
