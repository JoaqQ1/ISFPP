package colectivo.util;

import java.time.Duration;
import java.time.LocalTime;

import colectivo.modelo.Recorrido;

/**
 * Clase utilitaria que provee métodos auxiliares para el manejo de tiempos
 * relacionados con recorridos y horarios del sistema de transporte.
 * 
 * <p>Incluye conversiones de segundos a {@link LocalTime}, 
 * cálculo de duración total de viaje y hora estimada de llegada al destino.</p>
 */
public class Tiempo {

    /**
     * Convierte una cantidad total de segundos a un objeto {@link LocalTime}.
     * 
     * <p>Por ejemplo, 3665 segundos se convierte en {@code 01:01:05}.</p>
     * 
     * @param totalSegundos cantidad total de segundos a convertir
     * @return una instancia de {@link LocalTime} que representa la hora, minutos y segundos equivalentes
     */
    public static LocalTime segundosATiempo(int totalSegundos) {

        // Calcular horas
        int horas = totalSegundos / 3600;
        int segundosRestantes = totalSegundos % 3600;

        // Calcular minutos
        int minutos = segundosRestantes / 60;
        int segundos = segundosRestantes % 60;

        return LocalTime.of(horas, minutos, segundos);
    }

    /**
     * Calcula la duración total del viaje desde la hora en que el pasajero llega a la parada
     * hasta la hora en que el colectivo llega al destino.
     * 
     * <p>La duración total se devuelve como un {@link LocalTime} en formato {@code HH:mm:ss}.
     * Si el colectivo llega antes de la hora de llegada del pasajero, se devuelve {@code 00:00:00}.</p>
     * 
     * @param r el {@link Recorrido} que contiene la hora de salida y la duración del viaje en segundos
     * @param horaLLegadaParada la hora a la que el pasajero llega a la parada de origen
     * @return un {@link LocalTime} que representa la duración total del viaje
     */
    public static LocalTime calcularDuracionTotalViaje(Recorrido r, LocalTime horaLLegadaParada) {

        if(r == null || horaLLegadaParada == null) return null;

        // Hora en que llega el colectivo al destino
        LocalTime horaLlegada = r.getHoraSalida().plusSeconds(r.getDuracion());

        Duration diferencia = Duration.between(horaLLegadaParada, horaLlegada);

        // Si la diferencia es negativa, la duración es cero (por ejemplo, si el pasajero llegó después)
        if (diferencia.isNegative()) diferencia = Duration.ZERO;

        LocalTime duracionTotal = Tiempo.segundosATiempo((int) diferencia.getSeconds());

        return duracionTotal;
    }

    /**
     * Calcula la hora estimada de llegada al destino en base a la hora de salida
     * y la duración del recorrido.
     * 
     * @param r el {@link Recorrido} del cual se obtiene la hora de salida y la duración en segundos
     * @return un {@link LocalTime} que representa la hora de llegada al destino
     */
    // public static LocalTime calcularHoraLlegadaDestino(Recorrido r)
    // if(r == null) return null;

    // return r.getHoraSalida().plusSeconds(r.getDuracion());
    public static LocalTime calcularHoraLlegadaDestino(Recorrido r) {
        return r.getHoraSalida().plusSeconds(r.getDuracion());
    }
}
