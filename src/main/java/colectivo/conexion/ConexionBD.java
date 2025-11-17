package colectivo.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.excepciones.ConexionException;

public class ConexionBD {
	private static final Logger LOGGER = LogManager.getLogger(ConexionBD.class.getName());

	private static Connection con = null;
	
	// Nos conectamos a la base de datos (con los datos de conexión del archivo jdbc.properties)
	public static Connection getConnection() {
		try {
			
			if (con == null) {
				Runtime.getRuntime().addShutdownHook(new MiShDwnHook());
                ResourceBundle rb = ResourceBundle.getBundle("bd");
				
                String driver = rb.getString("driver");
                String url = rb.getString("url");
                String usr = rb.getString("usuario");
                String psw = rb.getString("contraseña");
                String sch = rb.getString("schema");
                
                Class.forName(driver);
                con = DriverManager.getConnection(url, usr, psw);

                // Setear el schema manualmente
                try (Statement st = con.createStatement()) {
                    st.execute("SET search_path TO " + sch);
                }
				LOGGER.info("Conexion exitosa a la base de datos.");
			}
			return con;
		} catch (Exception ex) {
			LOGGER.error("getConnection: Error al crear la conexion: " + ex.getMessage());
			throw new ConexionException("Error al crear la conexion", ex);
		}
	}

	public static class MiShDwnHook extends Thread {
		// justo antes de finalizar el programa la JVM invocara
		// a este metodo donde podemos cerrar la conexion
		public void run() {
			try {
				Connection con = ConexionBD.getConnection();
				con.close();
			} catch (Exception ex) {
				LOGGER.error("MiShDwnHook.run: Error al cerrar la conexion: " + ex.getMessage());
				ex.printStackTrace();
				throw new ConexionException("Error al cerrar la conexion",ex);
			}
		}
	}
}
