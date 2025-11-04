package colectivo.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
                String usr = rb.getString("usr");
                String psw = rb.getString("psw");
                String sch = rb.getString("sch");
                
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
			LOGGER.error("Error al crear la conexion: " + ex.getMessage());
			throw new RuntimeException("Error al crear la conexion", ex);
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
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
	}
}