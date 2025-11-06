package colectivo.configuracion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.conexion.ConexionBD;

public class ConfiguracionGlobal {
    private static final Logger LOGGER = LogManager.getLogger(ConfiguracionGlobal.class.getName());

    private static ConfiguracionGlobal configuracion = null;
    private Properties propiedades;
    private ResourceBundle resourceBundle;
    private Locale currentLocale;

    public static ConfiguracionGlobal getConfiguracionGlobal(){
        if (configuracion == null) {
			configuracion = new ConfiguracionGlobal();
		}
		return configuracion;
    }
    private ConfiguracionGlobal() {
        propiedades = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IOException("Archivo de configuración no encontrado");
            }
            propiedades.load(input);
            Locale.setDefault(new Locale(propiedades.getProperty("language"), propiedades.getProperty("country")));
			resourceBundle = ResourceBundle.getBundle(propiedades.getProperty("labels"));
            LOGGER.info("<init>: Configuración cargada correctamente.");
        } catch (IOException e) {
            LOGGER.error("<init>: Error cargando configuración", e);
            throw new RuntimeException("Error cargando configuración", e);
        }
    }

    public String get(String clave) {
        return propiedades.getProperty(clave);
    }

    public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
    public String getPersistenciaTipo() {
        return propiedades.getProperty("persistencia.tipo");
    }

    public String getNombreAplicacion() {
        return propiedades.getProperty("app.nombre");
    }
    public String getIdiomaActual(){
        return propiedades.getProperty("idioma.actual");
    }
    public void setLocale(Locale locale) {
        if(locale == null) {
            locale = Locale.getDefault();
        }
        this.currentLocale = locale;
        this.resourceBundle = ResourceBundle.getBundle("i18n.labels", locale);
    }

    public Locale getLocale() {
        return currentLocale;
    }

    public void setPersistenciaTipo(String tipo) {
        if (tipo != null && !tipo.isEmpty()) {
            propiedades.setProperty("persistencia.tipo", tipo);
        }
    }
    public double getOrigenLatitud() {
        return Double.parseDouble(propiedades.getProperty("origen.latitud"));
    }

    public double getOrigenLongitud() {
        return Double.parseDouble(propiedades.getProperty("origen.longitud"));
    }
    public int getZoom() {
        return Integer.parseInt(propiedades.getProperty("zoom.inicial"));
    }
    
    public String getCiudadActual() {
        return propiedades.getProperty("ciudad.actual");
    }
}
