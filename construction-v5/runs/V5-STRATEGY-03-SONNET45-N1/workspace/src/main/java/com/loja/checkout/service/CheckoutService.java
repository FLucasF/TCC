package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.CodigoErro;
import com.loja.checkout.estrategia.clube.*;
import com.loja.checkout.estrategia.cupom.*;
import com.loja.checkout.estrategia.modalidade.*;
import com.loja.checkout.estrategia.pagamento.Boleto;
import com.loja.checkout.estrategia.pagamento.Cartao;
import com.loja.checkout.estrategia.pagamento.FormaPagamento;
import com.loja.checkout.estrategia.pagamento.Pix;
import com.loja.checkout.estrategia.regiao.*;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.util.Moeda;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, Regiao> regioes;

    public CheckoutService() {
        this.modalidades = new HashMap<>();
        modalidades.put("ECONOMICA", new Economica());
        modalidades.put("EXPRESSA", new Expressa());
        modalidades.put("RETIRADA_LOJA", new RetiradaLoja());
        modalidades.put("MOTOBOY", new Motoboy());

        this.cupons = new HashMap<>();
        cupons.put("BEMVINDO10", new BemVindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());

        this.formasPagamento = new HashMap<>();
        formasPagamento.put("PIX", new Pix());
        formasPagamento.put("CARTAO", new Cartao());
        formasPagamento.put("BOLETO", new Boleto());

        this.niveisClube = new HashMap<>();
        niveisClube.put("BRONZE", new Bronze());
        niveisClube.put("PRATA", new Prata());
        niveisClube.put("OURO", new Ouro());

        this.regioes = new HashMap<>();
        regioes.put("SUDESTE", new Sudeste());
        regioes.put("SUL", new Sul());
        regioes.put("CENTRO_OESTE", new CentroOeste());
        regioes.put("NORTE", new Norte());
        regioes.put("NORDESTE", new Nordeste());
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        NivelClube nivelClube = obterNivelClube(request.getNivelClube());
        Regiao regiao = obterRegiao(request.getRegiao());
        ModalidadeEntrega modalidade = obterModalidade(request.getModalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarDisponibilidadeModalidade(modalidade, pesoTotal);

        BigDecimal freteSemBeneficio = modalidade.calcularFrete(pesoTotal);
        int prazo = modalidade.obterPrazo();

        BigDecimal descontoCupom = Moeda.de(0.00);
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            Cupom cupom = obterCupom(request.getCupom());
            validarAplicabilidadeCupom(cupom, subtotalProdutos);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, freteSemBeneficio, request.getItens());
        }

        BigDecimal frete = nivelClube.freteGratis() ? Moeda.de(0.00) : freteSemBeneficio;
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);
        BigDecimal totalPedido = Moeda.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = obterFormaPagamento(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);
        validarDisponibilidadeFormaPagamento(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = Moeda.arredondar(totalPedido.add(ajustePagamento));
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazo);
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
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private NivelClube obterNivelClube(String codigo) {
        if (codigo == null || !niveisClube.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return niveisClube.get(codigo);
    }

    private Regiao obterRegiao(String codigo) {
        if (codigo == null || !regioes.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        return regioes.get(codigo);
    }

    private ModalidadeEntrega obterModalidade(String codigo) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidades.get(codigo);
    }

    private void validarDisponibilidadeModalidade(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    private Cupom obterCupom(String codigo) {
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return cupons.get(codigo);
    }

    private void validarAplicabilidadeCupom(Cupom cupom, BigDecimal subtotalProdutos) {
        if (!cupom.eAplicavel(subtotalProdutos)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || !formasPagamento.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return formasPagamento.get(codigo);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.estaDisponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return Moeda.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }
}
