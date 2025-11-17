package colectivo.util;

import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncService {

    private static final Logger LOGGER = LogManager.getLogger(AsyncService.class);
    // Usamos un CachedThreadPool para manejar múltiples peticiones si fuera necesario
    // Opcional: Usar newSingleThreadExecutor() si la BD no soporta concurrencia.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Ejecuta una tarea pesada en background y devuelve el resultado en el UI Thread.
     * * @param tarea   (Supplier) Lógica que retorna un valor (Corre en Background)
     * @param onExito (Consumer) Qué hacer con el resultado (Corre en JavaFX Thread)
     * @param onError (Consumer) Qué hacer si falla (Corre en JavaFX Thread)
     * @param <T>     Tipo de dato de retorno
     */
    public <T> void ejecutarAsync(Supplier<T> tarea, Consumer<T> onExito, Consumer<Exception> onError) {
        CompletableFuture.supplyAsync(tarea, executor)
            .whenComplete((resultado, error) -> {
                // Volvemos al hilo de JavaFX para tocar la UI
                Platform.runLater(() -> {
                    if (error != null) {
                        // Desempaquetar la excepción real del CompletableFuture
                        Throwable causaReal = error.getCause() != null ? error.getCause() : error;
                        LOGGER.error("ejecutarAsync:Error en tarea asíncrona", causaReal);
                        if (onError != null) {
                            onError.accept((Exception) causaReal);
                        }
                    } else {
                        if (onExito != null) {
                            onExito.accept(resultado);
                        }
                    }
                });
            });
    }

    /**
     * Ejecuta una tarea que no devuelve valor (void) en background.
     */
    public void ejecutarTarea(Runnable tarea, Runnable onExito, Consumer<Exception> onError) {
        CompletableFuture.runAsync(tarea, executor)
            .whenComplete((voidResult, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        Throwable causaReal = error.getCause() != null ? error.getCause() : error;
                        LOGGER.error("ejecutarTarea:Error en tarea void asíncrona", causaReal);
                        if (onError != null) onError.accept((Exception) causaReal);
                    } else {
                        if (onExito != null) onExito.run();
                    }
                });
            });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}