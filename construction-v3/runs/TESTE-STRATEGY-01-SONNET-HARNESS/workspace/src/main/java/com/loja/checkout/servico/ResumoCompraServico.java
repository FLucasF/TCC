package com.loja.checkout.servico;

import com.loja.checkout.api.CheckoutException;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraServico {

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = converterItens(request);
        validarItens(itens);

        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());

        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());
        BigDecimal pesoTotal = itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(
                itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal frete = nivelClube.isentoFrete()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.custo(pesoTotal);

        Cupom cupom = validarCupom(request.cupom(), subtotalProdutos, itens);
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : cupom.desconto(subtotalProdutos, itens, frete);

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal produtosComDesconto = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom));
        BigDecimal totalPedidoSemImposto = Dinheiro.arredondar(produtosComDesconto.add(frete));
        if (!formaPagamento.disponivel(totalPedidoSemImposto)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal imposto = Dinheiro.arredondar(produtosComDesconto.multiply(regiao.percentualImposto()));
        BigDecimal totalPedido = Dinheiro.arredondar(totalPedidoSemImposto.add(imposto));

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.elegivelBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                imposto,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> converterItens(ResumoRequest request) {
        if (request.itens() == null) {
            return List.of();
        }
        return request.itens().stream()
                .map(item -> new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList();
    }

    private void validarItens(List<Item> itens) {
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private NivelClube validarNivelClube(String nivelClube) {
        return converterEnum(nivelClube, NivelClube.class, "NIVEL_CLUBE_INVALIDO");
    }

    private Regiao validarRegiao(String regiao) {
        return converterEnum(regiao, Regiao.class, "REGIAO_INVALIDA");
    }

    private ModalidadeEntrega validarModalidade(String modalidadeEntrega) {
        return converterEnum(modalidadeEntrega, ModalidadeEntrega.class, "MODALIDADE_INVALIDA");
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        return converterEnum(formaPagamento, FormaPagamento.class, "FORMA_PAGAMENTO_INVALIDA");
    }

    private Cupom validarCupom(String cupomInformado, BigDecimal subtotalProdutos, List<Item> itens) {
        if (cupomInformado == null || cupomInformado.isBlank()) {
            return null;
        }
        Cupom cupom = converterEnum(cupomInformado, Cupom.class, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private <T extends Enum<T>> T converterEnum(String valor, Class<T> tipo, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException excecao) {
            throw new CheckoutException(codigoErro);
        }
    }
}
