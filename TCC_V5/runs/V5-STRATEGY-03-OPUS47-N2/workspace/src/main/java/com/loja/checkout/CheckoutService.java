package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.RegistroNiveis;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.RegistroCupons;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.RegistroModalidades;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.RegistroPagamentos;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {
    private final RegistroModalidades modalidades = new RegistroModalidades();
    private final RegistroCupons cupons = new RegistroCupons();
    private final RegistroNiveis niveis = new RegistroNiveis();
    private final RegistroPagamentos pagamentos = new RegistroPagamentos();

    public CheckoutResponse calcular(CheckoutRequest req) {
        Pedido pedido = montarPedido(req.itens);
        NivelClube nivel = niveis.obter(req.nivelClube);
        Regiao regiao = Regiao.obter(req.regiao);
        ModalidadeEntrega modalidade = modalidades.obter(req.modalidadeEntrega);
        modalidade.validar(pedido);

        BigDecimal subtotal = Dinheiro.arredonda(pedido.subtotalProdutos());
        BigDecimal frete = Dinheiro.arredonda(nivel.ajustaFrete(modalidade.frete(pedido)));

        Optional<Cupom> cupom = cupons.obter(req.cupom);
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (cupom.isPresent()) {
            cupom.get().validar(pedido, subtotal, frete);
            descontoCupom = Dinheiro.arredonda(cupom.get().calcularDesconto(pedido, subtotal, frete));
        }

        BigDecimal seguro = Dinheiro.arredonda(subtotal.multiply(regiao.percentualSeguro));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento pagamento = pagamentos.obter(req.formaPagamento);
        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        pagamento.validarParcelas(parcelas);
        pagamento.validar(totalPedido);
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredonda(resultado.ajuste()),
                Dinheiro.arredonda(resultado.totalFinal()),
                parcelas,
                Dinheiro.arredonda(resultado.valorParcela()),
                Dinheiro.arredonda(nivel.credito(subtotal)),
                nivel.brinde(subtotal)
        );
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new ErroPedido("PEDIDO_INVALIDO");
        List<Pedido.Item> convertidos = itens.stream().map(this::converterItem).toList();
        return new Pedido(convertidos);
    }

    private Pedido.Item converterItem(ItemRequest r) {
        if (r == null) throw new ErroPedido("PEDIDO_INVALIDO");
        if (positivo(r.precoUnitario) == null) throw new ErroPedido("PEDIDO_INVALIDO");
        if (r.quantidade == null || r.quantidade <= 0) throw new ErroPedido("PEDIDO_INVALIDO");
        if (positivo(r.pesoKg) == null) throw new ErroPedido("PEDIDO_INVALIDO");
        return new Pedido.Item(r.nome, r.precoUnitario, r.quantidade, r.pesoKg);
    }

    private BigDecimal positivo(BigDecimal v) {
        if (v == null) return null;
        if (v.compareTo(BigDecimal.ZERO) <= 0) return null;
        return v;
    }
}
