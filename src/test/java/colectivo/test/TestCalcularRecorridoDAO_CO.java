
package colectivo.test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import colectivo.negocio.Calculo;
import colectivo.persistencia.dao.LineaDAO;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.persistencia.dao.TramoDAO;

class TestCalcularRecorridoDAO_CO {

	private Map<Integer, Parada> paradas;
	private Map<String, Linea> lineas;
	private Map<String, Tramo> tramos;

	private int diaSemana;
	private LocalTime horaLlegaParada;
	
	private Calculo calculo;	

	@BeforeEach
	void setUp() throws Exception {

		paradas = ((ParadaDAO) Factory.getInstancia(Constantes.PARADA)).buscarTodos();

		tramos = ((TramoDAO) Factory.getInstancia(Constantes.TRAMO)).buscarTodos();

		lineas = ((LineaDAO) Factory.getInstancia(Constantes.LINEA)).buscarTodos();

		diaSemana = 1; // lunes
		horaLlegaParada = LocalTime.of(10, 35);

		calculo = new Calculo();
	}

	@Test
	void testSinColectivo() {
		Parada paradaOrigen = paradas.get(118);
		Parada paradaDestino = paradas.get(19);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertTrue(recorridos.isEmpty());
	}

	@Test
	void testDirecto() {
		
		Parada paradaOrigen = paradas.get(5);
		Parada paradaDestino = paradas.get(117);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);
		assertEquals(1, recorridos.size());
		assertEquals(1, recorridos.get(0).size());

		Recorrido recorrido1;
		recorrido1 = recorridos.get(0).get(0);
		

		// recorrido1
		assertEquals(lineas.get("L1I"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(5));
		paradas1.add(paradas.get(6));
		paradas1.add(paradas.get(7));
		paradas1.add(paradas.get(116));
		paradas1.add(paradas.get(117));
		

		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(11, 00), recorrido1.getHoraSalida());
		assertEquals(170, recorrido1.getDuracion());

	}

	@Test
	void testConexion() {
		Parada paradaOrigen = paradas.get(141); // 143 conexion
		Parada paradaDestino = paradas.get(123);
		
		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertEquals(1, recorridos.size());
		assertEquals(2, recorridos.get(0).size());
		// assertEquals(2, recorridos.get(1).size());

		Recorrido recorrido1;
		Recorrido recorrido2;
		
		recorrido1 = recorridos.get(0).get(0);
		recorrido2 = recorridos.get(0).get(1);


		// recorrido1
		// 100;151;143 CONEXION 143;124
		assertEquals(lineas.get("L1R"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(141));
		paradas1.add(paradas.get(144));
		paradas1.add(paradas.get(142));
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(10, 39,56), recorrido1.getHoraSalida()); // tarda 930 segundos en llegar a la parada 100
		assertEquals(100, recorrido1.getDuracion());

		assertEquals(lineas.get("L3I"), recorrido2.getLinea());
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(142));
		paradas2.add(paradas.get(123));
		assertIterableEquals(paradas2, recorrido2.getParadas());
		
		assertEquals(LocalTime.of(10, 45, 8), recorrido2.getHoraSalida());
		assertEquals(29, recorrido2.getDuracion());
	}

	@Test
	void testConexionCaminando() {
		Parada paradaOrigen = paradas.get(8);
		Parada paradaDestino = paradas.get(112);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);
		
		assertEquals(2, recorridos.size());
		assertEquals(3, recorridos.get(0).size());		

		Recorrido recorrido1 = recorridos.get(0).get(0);
		Recorrido recorrido2 = recorridos.get(0).get(1);
		Recorrido recorrido3 = recorridos.get(0).get(2);
		
		
		// recorrido1
		assertEquals(lineas.get("L2I"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(8));
		paradas1.add(paradas.get(37));
		
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(11, 04,30), recorrido1.getHoraSalida());
		assertEquals(60, recorrido1.getDuracion());
		
		// recorrido2
		assertNull(recorrido2.getLinea()); // Caminando
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(37));
		paradas2.add(paradas.get(113));		
		assertIterableEquals(paradas2, recorrido2.getParadas());
		assertEquals(LocalTime.of(11, 05,30), recorrido2.getHoraSalida());
		assertEquals(660, recorrido2.getDuracion());

		// recorrido3
		assertEquals(lineas.get("L1I"), recorrido3.getLinea());
		List<Parada> paradas3 = new ArrayList<Parada>();
		paradas3.add(paradas.get(113));
		paradas3.add(paradas.get(112));
		assertIterableEquals(paradas3, recorrido3.getParadas());
		assertEquals(LocalTime.of(11, 44,20), recorrido3.getHoraSalida());
		assertEquals(40, recorrido3.getDuracion());

		Recorrido recorrido4 = recorridos.get(0).get(0);
		Recorrido recorrido5 = recorridos.get(0).get(1);
		Recorrido recorrido6 = recorridos.get(0).get(2);

		assertEquals(lineas.get("L2I"), recorrido4.getLinea());
		List<Parada> paradas4 = new ArrayList<Parada>();
		paradas4.add(paradas.get(8));
		paradas4.add(paradas.get(37));
		
		assertIterableEquals(paradas4, recorrido4.getParadas());
		assertEquals(LocalTime.of(11, 04,30), recorrido4.getHoraSalida());
		assertEquals(60, recorrido4.getDuracion());

		// recorrido2
		assertNull(recorrido5.getLinea()); // Caminando
		List<Parada> paradas5 = new ArrayList<Parada>();
		paradas5.add(paradas.get(37));
		paradas5.add(paradas.get(113));		
		assertIterableEquals(paradas5, recorrido2.getParadas());
		assertEquals(LocalTime.of(11, 05,30), recorrido5.getHoraSalida());
		assertEquals(660, recorrido2.getDuracion());

		// recorrido3
		assertEquals(lineas.get("L1I"), recorrido6.getLinea());
		List<Parada> paradas6 = new ArrayList<Parada>();
		paradas6.add(paradas.get(113));
		paradas6.add(paradas.get(112));
		assertIterableEquals(paradas6, recorrido6.getParadas());
		assertEquals(LocalTime.of(11, 44,20), recorrido6.getHoraSalida());
		assertEquals(40, recorrido6.getDuracion());

	}
}
