package colectivo.configuracion;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.constantes.Constantes;
import colectivo.persistencia.dao.LineaDAO;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.persistencia.dao.TramoDAO;
import colectivo.persistencia.dao.bd.LineaBdDAO;
import colectivo.persistencia.dao.bd.ParadaBdDAO;
import colectivo.persistencia.dao.bd.TramoBdDAO;
import colectivo.persistencia.dao.secuencial.LineaSecuencialDAO;
import colectivo.persistencia.dao.secuencial.ParadaSecuencialDAO;
import colectivo.persistencia.dao.secuencial.TramoSecuencialDAO;

public class Factory {
    private static final Logger LOGGER = LogManager.getLogger(Factory.class.getName());
    
    // CACHÉ DE SERVICIOS: Para "INTERFAZ" y otras cosas de factory.properties
    private static final ConcurrentHashMap<String, Object> SERVICE_CACHE = new ConcurrentHashMap<>();

    // CACHÉ DE DAOs: Para LINEA, PARADA, TRAMO.
    private static final ConcurrentHashMap<String, Object> DAO_CACHE = new ConcurrentHashMap<>();
    
    // Guarda el tipo de persistencia con el que se creó el DAO_CACHE
    private static String cachedPersistenceType = null;


    /**
     * Devuelve la implementación de LineaDAO (Singleton) basada en la
     * configuración global actual.
     */
    public static LineaDAO getLineaDAO() {
        checkAndClearDAOCache(); // ¡Paso 1: Validar el caché!
        
        // ¡Paso 2: Usar computeIfAbsent en el CACHÉ DE DAOs!
        return (LineaDAO) DAO_CACHE.computeIfAbsent(Constantes.LINEA, (key) -> {
            String tipo = ConfiguracionGlobal.geConfiguracionGlobal().getPersistenciaTipo();
            LOGGER.info("Factory: Creando y cacheando instancia de LineaDAO para persistencia: " + tipo);
            if (Constantes.BD.equalsIgnoreCase(tipo)) {
                return new LineaBdDAO();
            } else {
                return new LineaSecuencialDAO();
            }
        });
    }

    /**
     * Devuelve la implementación de ParadaDAO (Singleton) basada en la
     * configuración global actual.
     */
    public static ParadaDAO getParadaDAO() {
        checkAndClearDAOCache(); // ¡Paso 1!
        
        // ¡Paso 2!
        return (ParadaDAO) DAO_CACHE.computeIfAbsent(Constantes.PARADA, (key) -> {
            String tipo = ConfiguracionGlobal.geConfiguracionGlobal().getPersistenciaTipo();
            LOGGER.info("Factory: Creando y cacheando instancia de ParadaDAO para persistencia: " + tipo);
            if (Constantes.BD.equalsIgnoreCase(tipo)) {
                return new ParadaBdDAO();
            } else {
                return new ParadaSecuencialDAO();
            }
        });
    }

    /**
     * Devuelve la implementación de TramoDAO (Singleton) basada en la
     * configuración global actual.
     */
    public static TramoDAO getTramoDAO() {
        checkAndClearDAOCache(); // ¡Paso 1!
        
        // ¡Paso 2!
        return (TramoDAO) DAO_CACHE.computeIfAbsent(Constantes.TRAMO, (key) -> {
            String tipo = ConfiguracionGlobal.geConfiguracionGlobal().getPersistenciaTipo();
            LOGGER.info("Factory: Creando y cacheando instancia de TramoDAO para persistencia: " + tipo);
            if (Constantes.BD.equalsIgnoreCase(tipo)) {
                return new TramoBdDAO();
            } else {
                return new TramoSecuencialDAO();
            }
        });
    }

    /**
     * Comprueba si el tipo de persistencia ha cambiado.
     * Si ha cambiado, limpia el caché de DAOs.
     * Es 'synchronized' para ser seguro entre hilos (thread-safe).
     */
    private static synchronized void checkAndClearDAOCache() {
        String persistenceType = ConfiguracionGlobal.geConfiguracionGlobal().getPersistenciaTipo();

        if (cachedPersistenceType == null) {
            // Primera vez que se llama, solo se establece el tipo
            cachedPersistenceType = persistenceType;
            return;
        }

        if (!cachedPersistenceType.equals(persistenceType)) {
            // ¡El tipo ha cambiado!
            LOGGER.info("Cambio de persistencia detectado (de " + cachedPersistenceType + " a " + persistenceType + "). Limpiando caché de DAOs.");
            DAO_CACHE.clear(); // Limpia TODO el caché de DAOs
            cachedPersistenceType = persistenceType; // Actualiza el tipo para la próxima vez
        }
    }


    // --- MÉTODOS GENÉRICOS (Redirigen a los métodos con caché) ---
    // Tu código de LineaSecuencialDAO llama a estos, así que los mantenemos.

    public static Object getInstancia(String objName) {
        // 1. Redirigir DAOs a los nuevos métodos (que ahora SÍ usan caché)
        if (Constantes.LINEA.equals(objName)) {
            return getLineaDAO();
        }
        if (Constantes.PARADA.equals(objName)) {
            return getParadaDAO();
        }
        if (Constantes.TRAMO.equals(objName)) {
            return getTramoDAO();
        }

        // 2. Lógica de caché para "INTERFAZ" y otros (usa el CACHÉ DE SERVICIOS)
        return SERVICE_CACHE.computeIfAbsent(objName, Factory::createServiceInstance);
    }

    public static Object getInstancia(String objName, Class<?> type) {
        // 1. Redirigir DAOs
        if (Constantes.LINEA.equals(objName)) {
            return getLineaDAO();
        }
        if (Constantes.PARADA.equals(objName)) {
            return getParadaDAO();
        }
        if (Constantes.TRAMO.equals(objName)) {
            return getTramoDAO();
        }

        // 2. Lógica original de caché (thread-safe) para "INTERFAZ"
        try {
            Object instance = SERVICE_CACHE.computeIfAbsent(objName, Factory::createServiceInstance);
            
            if (instance == null) {
                LOGGER.error("computeIfAbsent devolvió null para: " + objName);
                throw new RuntimeException("No se pudo crear la instancia para: " + objName);
            }
            if (!type.isAssignableFrom(instance.getClass())) {
                LOGGER.warn("La instancia cacheada para " + objName + " no es del tipo " + type.getName());
                throw new RuntimeException("Instancia incompatible con el tipo: " + objName);
            }
            return instance;
        } catch (Exception ex) {
            LOGGER.error("Error al obtener la instancia de: " + objName, ex);
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
            LOGGER.error("Error al crear la instancia de: " + name, e);
            return null;
        }
    }
}