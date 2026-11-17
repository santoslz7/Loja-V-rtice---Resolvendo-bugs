package loja;

import java.util.Collection;
import java.util.stream.Collectors;

public final class Json {
    private Json() { }

    public static String entreAspas(String texto) {
        if (texto == null) return "null";
        StringBuilder resultado = new StringBuilder("\"");
        for (char caractere : texto.toCharArray()) {
            switch (caractere) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> resultado.append(caractere < 32 ? " " : caractere);
            }
        }
        return resultado.append('"').toString();
    }

    public static String lista(Collection<String> itens) {
        return itens.stream().collect(Collectors.joining(",", "[", "]"));
    }
}
