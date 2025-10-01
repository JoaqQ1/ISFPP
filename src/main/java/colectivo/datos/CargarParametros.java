package colectivo.datos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CargarParametros {

    private static String archivoLinea;
    private static String archivoParada;
    private static String archivoTramo;
    private static String archivoFrecuencia;

    /**
     * Carga los parámetros desde el archivo "config.properties".
     * 
     * ⚠️ Cambios importantes para Maven:
     * - En proyectos Maven, los archivos de configuración deben ubicarse en 
     *   "src/main/resources". Maven automáticamente los copia al classpath
     *   dentro de "target/classes".
     * - Por eso, en lugar de usar un FileInputStream con una ruta absoluta o relativa,
     *   se utiliza `getResourceAsStream("config.properties")`, que busca el archivo
     *   directamente en el classpath.
     * - Esto asegura que el archivo se pueda encontrar tanto en ejecución local
     *   como cuando se empaqueta el proyecto en un .jar.
     * 
     * @throws IOException si el archivo no se encuentra o no se puede leer.
     */
    public static void parametros() throws IOException {

        Properties prop = new Properties();

        // 🔑 Forma correcta en Maven: busca "config.properties" en el classpath
        try (InputStream input = CargarParametros.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new IOException("No se pudo encontrar config.properties en resources");
            }

            // Carga las propiedades desde el InputStream
            prop.load(input);

            // Guarda los valores en variables estáticas
            archivoLinea = prop.getProperty("linea");
            archivoParada = prop.getProperty("parada");
            archivoTramo = prop.getProperty("tramo");
            archivoFrecuencia = prop.getProperty("frecuencia");
        }

        // ❌ Anteriormente: se usaba FileInputStream("config.properties"), lo cual
        // dependía de la ruta de ejecución y fallaba al usar Maven o al empaquetar en un jar.
    }

    // Métodos getters para acceder a los archivos configurados
    public static String getArchivoLinea() {
        return archivoLinea;
    }

    public static String getArchivoParada() {
        return archivoParada;
    }

    public static String getArchivoTramo() {
        return archivoTramo;
    }

    public static String getArchivoFrecuencia() {
        return archivoFrecuencia;
    }
}
