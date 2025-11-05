
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
		Parada paradaOrigen = paradas.get(151);
		Parada paradaDestino = paradas.get(19);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertTrue(recorridos.isEmpty());
	}

	@Test
	void testDirecto() {
		
		Parada paradaOrigen = paradas.get(5);
		Parada paradaDestino = paradas.get(140);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);
		assertEquals(2, recorridos.size());
		assertEquals(1, recorridos.get(0).size());
		assertEquals(1, recorridos.get(1).size());

		Recorrido recorrido1;
		Recorrido recorrido2;
		if (recorridos.get(0).get(0).getLinea().equals(lineas.get("L1I"))) {
			recorrido1 = recorridos.get(0).get(0);
			recorrido2 = recorridos.get(1).get(0);
		} else {
			recorrido1 = recorridos.get(0).get(1);
			recorrido2 = recorridos.get(0).get(0);
		}

		// recorrido1
		assertEquals(lineas.get("L1I"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(5));
		paradas1.add(paradas.get(6));
		paradas1.add(paradas.get(7));
		paradas1.add(paradas.get(117));
		paradas1.add(paradas.get(118));
		paradas1.add(paradas.get(125));
		paradas1.add(paradas.get(104));
		paradas1.add(paradas.get(152));
		paradas1.add(paradas.get(99));
		paradas1.add(paradas.get(130));
		paradas1.add(paradas.get(128));
		paradas1.add(paradas.get(76));
		paradas1.add(paradas.get(147));
		paradas1.add(paradas.get(48));
		paradas1.add(paradas.get(51));
		paradas1.add(paradas.get(49));
		paradas1.add(paradas.get(140));

		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(11, 00), recorrido1.getHoraSalida());
		assertEquals(740, recorrido1.getDuracion());

		// recorrido2
		assertEquals(lineas.get("L1R"), recorrido2.getLinea());
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(5));
		paradas2.add(paradas.get(6));
		paradas2.add(paradas.get(7));
		paradas2.add(paradas.get(9));
		paradas2.add(paradas.get(84));
		paradas2.add(paradas.get(83));
		paradas2.add(paradas.get(59));
		paradas2.add(paradas.get(85));
		paradas2.add(paradas.get(128));
		paradas2.add(paradas.get(74));
		paradas2.add(paradas.get(75));
		paradas2.add(paradas.get(146));
		paradas2.add(paradas.get(47));
		paradas2.add(paradas.get(50));
		paradas2.add(paradas.get(52));
		paradas2.add(paradas.get(46));
		paradas2.add(paradas.get(159));
		paradas2.add(paradas.get(100));
		paradas2.add(paradas.get(151));
		paradas2.add(paradas.get(143));
		paradas2.add(paradas.get(145));
		paradas2.add(paradas.get(142));
		paradas2.add(paradas.get(148));
		paradas2.add(paradas.get(95));
		paradas2.add(paradas.get(27));
		paradas2.add(paradas.get(26));
		paradas2.add(paradas.get(110));
		paradas2.add(paradas.get(58));
		paradas2.add(paradas.get(79));
		paradas2.add(paradas.get(68));
		paradas2.add(paradas.get(82));
		paradas2.add(paradas.get(157));
		paradas2.add(paradas.get(120));
		paradas2.add(paradas.get(119));
		paradas2.add(paradas.get(98));
		paradas2.add(paradas.get(110));
		paradas2.add(paradas.get(24));
		paradas2.add(paradas.get(25));
		paradas2.add(paradas.get(95));
		paradas2.add(paradas.get(62));
		paradas2.add(paradas.get(34));
		paradas2.add(paradas.get(144));
		paradas2.add(paradas.get(143));
		paradas2.add(paradas.get(151));
		paradas2.add(paradas.get(113));
		paradas2.add(paradas.get(114));
		paradas2.add(paradas.get(140));

		assertIterableEquals(paradas2, recorrido2.getParadas());
		assertEquals(LocalTime.of(10, 46), recorrido2.getHoraSalida());
		assertEquals(2420, recorrido2.getDuracion());

	}

	@Test
	void testConexion() {
		Parada paradaOrigen = paradas.get(100); // 143 conexion
		Parada paradaDestino = paradas.get(124);
		
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
		paradas1.add(paradas.get(100));
		paradas1.add(paradas.get(151));
		paradas1.add(paradas.get(143));
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(11, 02), recorrido1.getHoraSalida()); // tarda 930 segundos en llegar a la parada 100
		assertEquals(60, recorrido1.getDuracion());

		assertEquals(lineas.get("L3I"), recorrido2.getLinea());
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(143));
		paradas2.add(paradas.get(124));
		assertIterableEquals(paradas2, recorrido2.getParadas());
		
		assertEquals(LocalTime.of(11, 04, 48), recorrido2.getHoraSalida());
		assertEquals(43, recorrido2.getDuracion());
	}

	@Test
	void testConexionCaminando() {
		Parada paradaOrigen = paradas.get(31);
		Parada paradaDestino = paradas.get(66);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);
		
		// assertEquals(1, recorridos.size());
		// assertEquals(3, recorridos.get(0).size());		

		// Recorrido recorrido1 = recorridos.get(0).get(0);
		// Recorrido recorrido2 = recorridos.get(0).get(1);
		// Recorrido recorrido3 = recorridos.get(0).get(2);
		
		// // recorrido1
		// assertEquals(lineas.get("L2R"), recorrido1.getLinea());
		// List<Parada> paradas1 = new ArrayList<Parada>();
		// paradas1.add(paradas.get(31));
		// paradas1.add(paradas.get(8));
		// paradas1.add(paradas.get(33));
		// paradas1.add(paradas.get(20));
		// paradas1.add(paradas.get(25));
		// paradas1.add(paradas.get(24));		
		// assertIterableEquals(paradas1, recorrido1.getParadas());
		// assertEquals(LocalTime.of(10, 39), recorrido1.getHoraSalida());
		// assertEquals(480, recorrido1.getDuracion());
		
		// // recorrido2
		// assertNull(recorrido2.getLinea()); // Caminando
		// List<Parada> paradas2 = new ArrayList<Parada>();
		// paradas2.add(paradas.get(24));
		// paradas2.add(paradas.get(75));		
		// assertIterableEquals(paradas2, recorrido2.getParadas());
		// assertEquals(LocalTime.of(10, 47), recorrido2.getHoraSalida());
		// assertEquals(120, recorrido2.getDuracion());

		// // recorrido3
		// assertEquals(lineas.get("L6I"), recorrido3.getLinea());
		// List<Parada> paradas3 = new ArrayList<Parada>();
		// paradas3.add(paradas.get(75));
		// paradas3.add(paradas.get(76));
		// paradas3.add(paradas.get(38));
		// paradas3.add(paradas.get(40));
		// paradas3.add(paradas.get(66));		
		// assertIterableEquals(paradas3, recorrido3.getParadas());
		// assertEquals(LocalTime.of(11, 02), recorrido3.getHoraSalida());
		// assertEquals(600, recorrido3.getDuracion());

	}
}
