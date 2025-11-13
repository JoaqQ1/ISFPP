package colectivo.configuracion;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Factory {
    private static final Logger LOGGER = LogManager.getLogger(Factory.class.getName());
    
    // CACHÉ DE SERVICIOS: Para "INTERFAZ" y otras cosas de factory.properties
    private static final ConcurrentHashMap<String, Object> SERVICE_CACHE = new ConcurrentHashMap<>();



    // --- MÉTODOS GENÉRICOS (Redirigen a los métodos con caché) ---
    // Tu código de LineaSecuencialDAO llama a estos, así que los mantenemos.

    public static Object getInstancia(String objName) {
        if(objName == null) {
            LOGGER.error("getInstancia: Parámetro nulo proporcionado: objName=null");
            throw new IllegalArgumentException("Parámetro nulo no permitido");
        }

        return SERVICE_CACHE.computeIfAbsent(objName, Factory::createServiceInstance);
    }

    public static <T> T getInstancia(String objName, Class<T> type) {
        if(objName == null || type == null) {
            LOGGER.error("getInstancia: Parámetros nulos proporcionados: objName=" + objName + ", type=" + type);
            throw new IllegalArgumentException("Parámetros nulos no permitidos");
        }

        
        try {
            Object instance = SERVICE_CACHE.computeIfAbsent(objName, Factory::createServiceInstance);
            
            if (instance == null) {
                LOGGER.error("getInstancia: computeIfAbsent devolvió null para: " + objName);
                throw new RuntimeException("No se pudo crear la instancia para: " + objName);
            }
            if (!type.isAssignableFrom(instance.getClass())) {
                LOGGER.warn("getInstancia: La instancia cacheada para " + objName + " no es del tipo " + type.getName());
                throw new RuntimeException("Instancia incompatible con el tipo: " + objName);
            }
            return type.cast(instance);
        } catch (Exception ex) {
            LOGGER.error("getInstancia: Error al obtener la instancia de: " + objName, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Método helper para crear instancias que van en el CACHÉ DE SERVICIOS
     * (leídas desde factory.properties).
     */
    private static Object createServiceInstance(String name) {
        // Lee desde factory.properties
        ResourceBundle rb = ResourceBundle.getBundle("factory");
        String className = rb.getString(name);
        try {
            LOGGER.info("Creando y cacheando instancia de SERVICIO: " + className);
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOGGER.error("createServiceInstance: Error al crear la instancia de: " + name, e);
            throw new RuntimeException(e);
        }
    }
}