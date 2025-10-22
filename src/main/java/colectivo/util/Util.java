package colectivo.util;

import colectivo.modelo.Parada;

public final class Util {
     // Constructor privado para evitar instanciación
    private Util() { }

    /**
     * Genera una clave única de tramo en formato "inicio-fin".
     *
     * Ejemplo: si el tramo va de parada 3 a 5, retorna "3-5".
     *
     * @param inicio parada de inicio del tramo
     * @param fin parada de fin del tramo
     * @return cadena con formato "inicio-fin"
     */
    public static String claveTramo(Parada inicio, Parada fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las paradas de inicio y fin no pueden ser nulas.");
        }
        return String.format("%d-%d", inicio.getCodigo(), fin.getCodigo());
    }
    /**
     * Parsea un número decimal que usa coma (",") como separador
     * en lugar de punto (".").
     *
     * @param decimalConComa cadena con coma decimal (ej. "12,34")
     * @return el valor double equivalente (ej. 12.34)
     */
    public static double parsearDecimalConComa(String decimalConComa) {
        if (decimalConComa == null || decimalConComa.isBlank()) {
            return 0.0;
        }
        String reemplazado = decimalConComa.replace(',', '.');
        return Double.parseDouble(reemplazado);
    }
}
