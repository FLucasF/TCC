package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoService(Catalogo<Entrega> entregas, Catalogo<Cupom> cupons, Catalogo<NivelClube> niveis,
            Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()
                || pedido.itens().stream().anyMatch(i -> i == null || !i.valido())) {
            throw recusa(CodigoErro.PEDIDO_INVALIDO);
        }
        Carrinho carrinho = new Carrinho(pedido.itens());

        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.buscar(pedido.regiao()), CodigoErro.REGIAO_INVALIDA);
        Entrega entrega = exigir(entregas.buscar(pedido.modalidadeEntrega()), CodigoErro.MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho)) {
            throw recusa(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(carrinho));
        BigDecimal desconto = descontoDoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = exigir(formasPagamento.buscar(pedido.formaPagamento()),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw recusa(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivel(totalPedido)) {
            throw recusa(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal descontoDoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(cupons.buscar(codigo), CodigoErro.CUPOM_INVALIDO);
        if (!cupom.aplicavel(carrinho)) {
            throw recusa(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(carrinho, frete);
    }

    private <T> T exigir(Optional<T> valor, CodigoErro erro) {
        return valor.orElseThrow((Supplier<PedidoRecusadoException>) () -> recusa(erro));
    }

    private PedidoRecusadoException recusa(CodigoErro erro) {
        return new PedidoRecusadoException(erro);
    }
}
