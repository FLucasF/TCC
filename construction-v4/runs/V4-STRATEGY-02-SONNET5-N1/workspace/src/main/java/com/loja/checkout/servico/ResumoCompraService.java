package com.loja.checkout.servico;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoCompraRequest;
import com.loja.checkout.web.dto.ResumoCompraResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoCompraResponse calcular(ResumoCompraRequest requisicao) {
        List<Item> itens = validarItens(requisicao.itens());
        NivelClube nivel = converter(NivelClube.class, requisicao.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = converter(Regiao.class, requisicao.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega entrega = converter(ModalidadeEntrega.class, requisicao.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoTotal = pesoTotal(itens);
        if (!entrega.disponivelPara(pesoTotal)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(subtotal(itens));

        Cupom cupom = null;
        if (requisicao.cupom() != null && !requisicao.cupom().isBlank()) {
            cupom = converter(Cupom.class, requisicao.cupom(), CodigoErro.CUPOM_INVALIDO);
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }

        FormaPagamento pagamento = converter(FormaPagamento.class, requisicao.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal frete = nivel.freteGratis() ? Dinheiro.zero() : Dinheiro.arredondar(entrega.calcularFrete(pesoTotal));
        BigDecimal descontoCupom = cupom == null ? Dinheiro.zero() : Dinheiro.arredondar(cupom.desconto(itens, subtotalProdutos, frete));
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!pagamento.disponivelPara(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        return new ResumoCompraResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itensRequisicao) {
        if (itensRequisicao == null || itensRequisicao.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>();
        for (ItemRequest itemRequisicao : itensRequisicao) {
            if (itemRequisicao.precoUnitario() == null || itemRequisicao.precoUnitario().signum() <= 0
                    || itemRequisicao.quantidade() == null || itemRequisicao.quantidade() <= 0
                    || itemRequisicao.pesoKg() == null || itemRequisicao.pesoKg().signum() <= 0) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new Item(itemRequisicao.nome(), itemRequisicao.precoUnitario(), itemRequisicao.quantidade(), itemRequisicao.pesoKg()));
        }
        return itens;
    }

    private BigDecimal subtotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    private BigDecimal pesoTotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            total = total.add(item.pesoTotal());
        }
        return total;
    }

    private <E extends Enum<E>> E converter(Class<E> tipo, String valor, CodigoErro codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new PedidoRecusadoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException ex) {
            throw new PedidoRecusadoException(codigoErro);
        }
    }
}
