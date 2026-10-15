package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, ModalidadeEntrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public ResumoService(List<ModalidadeEntrega> entregas, List<Cupom> cupons,
                         List<NivelClube> niveis, List<FormaPagamento> pagamentos) {
        this.entregas = Identificavel.indexar(entregas);
        this.cupons = Identificavel.indexar(cupons);
        this.niveis = Identificavel.indexar(niveis);
        this.pagamentos = Identificavel.indexar(pagamentos);
    }

    public ResumoResponse resumir(ResumoRequest pedido) {
        Carrinho carrinho = montarCarrinho(pedido.itens());
        NivelClube nivel = buscar(niveis, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = regiao(pedido.regiao());
        ModalidadeEntrega entrega = buscar(entregas, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = Dinheiro.arredondar(nivel.frete(entrega.frete(carrinho)));
        BigDecimal desconto = desconto(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = buscar(pagamentos, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal desconto(String codigoCupom, Carrinho carrinho, BigDecimal frete) {
        if (codigoCupom == null) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
        Cupom cupom = buscar(cupons, codigoCupom, CodigoErro.CUPOM_INVALIDO);
        if (!cupom.aplicavel(carrinho)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(carrinho, frete));
    }

    private Carrinho montarCarrinho(List<ResumoRequest.ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(this::item).toList());
    }

    private Item item(ResumoRequest.ItemRequest i) {
        if (i == null || i.precoUnitario() == null || i.quantidade() == null || i.pesoKg() == null
                || i.precoUnitario().signum() <= 0 || i.quantidade() <= 0 || i.pesoKg().signum() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
    }

    private Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PedidoRecusadoException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private static <T> T buscar(Map<String, T> opcoes, String codigo, CodigoErro erro) {
        T opcao = opcoes.get(codigo);
        if (opcao == null) {
            throw new PedidoRecusadoException(erro);
        }
        return opcao;
    }
}
