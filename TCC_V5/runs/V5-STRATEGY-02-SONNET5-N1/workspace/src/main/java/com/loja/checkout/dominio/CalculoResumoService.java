package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.NiveisClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.regiao.Regiao;
import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculoResumoService {

    private final ModalidadesEntrega modalidadesEntrega;
    private final Cupons cupons;
    private final NiveisClube niveisClube;
    private final FormasPagamento formasPagamento;

    public CalculoResumoService(ModalidadesEntrega modalidadesEntrega, Cupons cupons,
            NiveisClube niveisClube, FormasPagamento formasPagamento) {
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = validarItens(request.itens());
        NivelClube nivel = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        BigDecimal pesoTotal = somarPeso(itens);
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(itens));

        BigDecimal freteBase = Dinheiro.arredondar(modalidade.calcularValor(pesoTotal));
        BigDecimal frete = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        Cupom cupom = validarCupom(request.cupom());
        ContextoCupom contextoCupom = new ContextoCupom(subtotalProdutos, frete, itens);
        if (cupom != null && !cupom.aplicavel(contextoCupom)) {
            throw new PedidoException("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(contextoCupom));

        BigDecimal seguro = Dinheiro.arredondar(regiao.calcularSeguro(subtotalProdutos));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivel.calcularCredito(subtotalProdutos));
        boolean brinde = nivel.elegivelBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                resultadoPagamento.parcelas(),
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        return itensRequest.stream().map(this::validarItem).toList();
    }

    private Item validarItem(ItemRequest itemRequest) {
        if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario() <= 0
                || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                || itemRequest.pesoKg() == null || itemRequest.pesoKg() <= 0) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        return new Item(
                itemRequest.nome(),
                BigDecimal.valueOf(itemRequest.precoUnitario()),
                itemRequest.quantidade(),
                BigDecimal.valueOf(itemRequest.pesoKg()));
    }

    private NivelClube validarNivelClube(String codigo) {
        return niveisClube.buscar(codigo)
                .orElseThrow(() -> new PedidoException("NIVEL_CLUBE_INVALIDO"));
    }

    private Regiao validarRegiao(String codigo) {
        if (codigo == null) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException ex) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String codigo) {
        return modalidadesEntrega.buscar(codigo)
                .orElseThrow(() -> new PedidoException("MODALIDADE_INVALIDA"));
    }

    private Cupom validarCupom(String codigo) {
        if (codigo == null) {
            return null;
        }
        return cupons.buscar(codigo)
                .orElseThrow(() -> new PedidoException("CUPOM_INVALIDO"));
    }

    private FormaPagamento validarFormaPagamento(String codigo) {
        return formasPagamento.buscar(codigo)
                .orElseThrow(() -> new PedidoException("FORMA_PAGAMENTO_INVALIDA"));
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        return itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<Item> itens) {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
