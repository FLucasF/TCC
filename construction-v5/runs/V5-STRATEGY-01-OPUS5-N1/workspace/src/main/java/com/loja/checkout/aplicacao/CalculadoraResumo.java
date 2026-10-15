package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.BaseCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(PedidoRequest requisicao) {
        Pedido pedido = pedidoDe(requisicao);
        NivelClube nivelClube = Opcao.exigir(NivelClube.class, requisicao.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Opcao.exigir(Regiao.class, requisicao.regiao(), CodigoErro.REGIAO_INVALIDA);

        ModalidadeEntrega modalidade =
                Opcao.exigir(ModalidadeEntrega.class, requisicao.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        BigDecimal pesoKg = pedido.pesoTotalKg();
        if (!modalidade.atende(pesoKg)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivelClube.frete(modalidade.frete(pesoKg));
        BigDecimal descontoCupom = descontoDe(requisicao.cupom(),
                new BaseCupom(pedido.itens(), subtotalProdutos, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento =
                Opcao.exigir(FormaPagamento.class, requisicao.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                pagamento.ajuste(totalPedido),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private Pedido pedidoDe(PedidoRequest requisicao) {
        List<ItemRequest> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(CalculadoraResumo::itemDe).toList());
    }

    private static Item itemDe(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoDe(String codigo, BaseCupom base) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        Cupom cupom = Opcao.exigir(Cupom.class, codigo, CodigoErro.CUPOM_INVALIDO);
        if (!cupom.aplicavel(base)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(base);
    }
}
