package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Busca;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pagamento;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.RecusaPedido;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(PedidoRequest request) {
        Pedido pedido = new Pedido(request.itens());
        NivelClube nivel = buscar(NivelClube.class, request.nivelClube(), Erro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = buscar(Regiao.class, request.regiao(), Erro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = buscar(ModalidadeEntrega.class, request.modalidadeEntrega(), Erro.MODALIDADE_INVALIDA);
        modalidade.validarPara(pedido);

        Optional<Cupom> cupom = Optional.ofNullable(request.cupom())
                .filter(codigo -> !codigo.isBlank())
                .map(codigo -> buscar(Cupom.class, codigo, Erro.CUPOM_INVALIDO));
        cupom.ifPresent(c -> c.validarPara(pedido));

        FormaPagamento forma = buscar(FormaPagamento.class, request.formaPagamento(), Erro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        forma.validarParcelas(parcelas);

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.ajustarFrete(modalidade.frete(pedido));
        BigDecimal desconto = cupom.map(c -> c.desconto(pedido, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        forma.validarDisponibilidade(total);
        Pagamento pagamento = forma.aplicar(total, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(total),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.temBrinde(subtotal));
    }

    private static <E extends Enum<E>> E buscar(Class<E> tipo, String nome, Erro erro) {
        return Busca.porNome(tipo, nome).orElseThrow(() -> new RecusaPedido(erro));
    }
}
