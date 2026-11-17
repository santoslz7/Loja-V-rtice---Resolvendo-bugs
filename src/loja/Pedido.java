package loja;

import java.util.List;
import java.util.Map;

public class Pedido {
    public enum Status { EM_ABERTO, CANCELADO, ENTREGUE }

    private final int id;
    private final List<String> itens;
    private final int totalCentavos;
    private final Map<Integer, Integer> quantidades;
    private Status status = Status.EM_ABERTO;

    public Pedido(int id, List<String> itens, int totalCentavos, Map<Integer, Integer> quantidades) {
        this.id = id;
        this.itens = List.copyOf(itens);
        this.totalCentavos = totalCentavos;
        this.quantidades = Map.copyOf(quantidades);
    }

    public int getId() { return id; }
    public Status getStatus() { return status; }
    public Map<Integer, Integer> getQuantidades() { return quantidades; }
    public void cancelar() {
        if (status != Status.EM_ABERTO) {
            throw new ErroNegocio(409, "Somente pedidos em aberto podem ser cancelados.");
        }
        status = Status.CANCELADO;
    }

    public String toJson() {
        return "{\"id\":" + id + ",\"itens\":" + Json.lista(itens) +
               ",\"totalCentavos\":" + totalCentavos +
               ",\"status\":" + Json.entreAspas(status.name()) + "}";
    }
}
