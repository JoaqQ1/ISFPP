package colectivo.ui.impl.javafx.controllers;

import javafx.embed.swing.SwingNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.ui.impl.javafx.MapPainter;
import colectivo.ui.impl.javafx.ParadasDebugPainter;

/**
 * Micro-Controlador encargado EXCLUSIVAMENTE de la gestión del mapa.
 * Aísla la complejidad de Swing/JXMapViewer.
 */
public class MapaController {
    
    private static final Logger LOGGER = LogManager.getLogger(MapaController.class);
    private JXMapViewer mapViewer;
    private final Random generator = new Random();
    
    // Inicialización
    public void inicializarMapa(SwingNode swingNode, double lat, double lon, int zoomInicial) {
        SwingUtilities.invokeLater(() -> {
            TileFactoryInfo info = new TileFactoryInfo(0, 17, 17, 256, true, true,
                "https://tile.openstreetmap.org", "x", "y", "z") {
                @Override
                public String getTileUrl(int x, int y, int zoom) {
                    int z = 17 - zoom;
                    return String.format("%s/%d/%d/%d.png", getBaseURL(), z, x, y);
                }
            };

            mapViewer = new JXMapViewer();
            mapViewer.setTileFactory(new DefaultTileFactory(info));
            
            // Listeners
            PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
            mapViewer.addMouseListener(panListener);
            mapViewer.addMouseMotionListener(panListener);
            mapViewer.addMouseWheelListener(new org.jxmapviewer.input.ZoomMouseWheelListenerCenter(mapViewer));

            // Posición inicial
            GeoPosition centro = new GeoPosition(lat, lon);
            mapViewer.setZoom(zoomInicial);
            mapViewer.setAddressLocation(centro);

            javafx.application.Platform.runLater(() -> {
                swingNode.setContent(mapViewer);
            });
        });
        LOGGER.info("Sub-sistema de Mapa inicializado.");
    }

    // Operaciones de Zoom
    public void zoomIn() {
        if (mapViewer != null) SwingUtilities.invokeLater(() -> mapViewer.setZoom(Math.max(mapViewer.getZoom() - 1, 1)));
    }

    public void zoomOut() {
        if (mapViewer != null) SwingUtilities.invokeLater(() -> mapViewer.setZoom(Math.min(mapViewer.getZoom() + 1, 19)));
    }

    // Dibujado de Rutas
    public void dibujarRecorrido(List<Recorrido> recorridos) {
        if (mapViewer == null) return;
        
        // Convertir datos de modelo a datos visuales (GeoPosition)
        List<GeoPosition> paradasGeo = new ArrayList<>();
        for (Recorrido r : recorridos) {
            r.getParadas().forEach(p -> {
                GeoPosition pos = new GeoPosition(p.getLatitud(), p.getLongitud());
                // Evitar duplicados consecutivos para limpieza visual
                if (paradasGeo.isEmpty() || !paradasGeo.get(paradasGeo.size() - 1).equals(pos)) {
                    paradasGeo.add(pos);
                }
            });
        }
        
        SwingUtilities.invokeLater(() -> {
            MapPainter pintor = new MapPainter(paradasGeo, Color.RED); // Tu clase MapPainter existente
            mapViewer.setOverlayPainter(pintor);
        });
    }

    /**
     * Implementación exacta de tu lógica de debug con Capas (CompoundPainter).
     * Se ejecuta en un hilo nuevo para no congelar la UI.
     */
    public void dibujarModoDebug(Collection<Parada> todasLasParadas, Collection<Linea> todasLasLineas) {
        if (mapViewer == null) {
            LOGGER.warn("dibujarModoDebug: MapViewer es nulo.");
            return;
        }

        LOGGER.info("Iniciando tarea de dibujado DEBUG en hilo de fondo...");

        // Usamos un hilo estándar de Java para procesar los Painters (Simulando tu Task)
        new Thread(() -> {
            try {
                // --- INICIO LÓGICA DE FONDO (Tu código original adaptado) ---
                
                List<Painter<JXMapViewer>> painters = new ArrayList<>();

                // 1. CAPA BASE: Dibujamos TODAS las paradas en GRIS.
                Set<GeoPosition> todasLasParadasGeo = new HashSet<>();
                for (Parada p : todasLasParadas) {
                    todasLasParadasGeo.add(new GeoPosition(p.getLatitud(), p.getLongitud()));
                }

                // Asumiendo que tienes esta clase disponible:
                ParadasDebugPainter painterBase = new ParadasDebugPainter(
                    todasLasParadasGeo, 
                    Color.GRAY
                );
                painters.add(painterBase);

                // 2. CAPAS DE LÍNEA: Iteramos por cada línea
                for (Linea linea : todasLasLineas) {
                    Collection<GeoPosition> paradasDeLinea = new ArrayList<>();
                    if (linea.getParadas() != null) {
                        for (Parada p : linea.getParadas()) {
                            paradasDeLinea.add(new GeoPosition(p.getLatitud(), p.getLongitud()));
                        }
                    }

                    if (!paradasDeLinea.isEmpty()) {
                        Color colorLinea = Color.decode(generarColorAleatorio());
                        ParadasDebugPainter painterLinea = new ParadasDebugPainter(
                            paradasDeLinea, 
                            colorLinea
                        );
                        painters.add(painterLinea);
                    }
                }

                // 3. Crear el CompoundPainter
                CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);
                
                LOGGER.info("Debug: {} layers creados.", painters.size());

                // --- ACTUALIZACIÓN VISUAL (Volvemos al hilo de Swing) ---
                SwingUtilities.invokeLater(() -> {
                    mapViewer.setOverlayPainter(compoundPainter);
                    // mapViewer.repaint(); // A veces ayuda a forzar el repintado
                });

            } catch (Exception e) {
                LOGGER.error("Error en tarea de debug", e);
            }
        }).start();
    }

    // Helper para colores (copiado de tu código original)
    private String generarColorAleatorio() {
        float hue = generator.nextFloat();
        float saturation = 0.25f + (generator.nextFloat() * 0.20f);
        float brightness = 0.85f + (generator.nextFloat() * 0.10f);
        Color color = Color.getHSBColor(hue, saturation, brightness);
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public void limpiarMapa() {
        if (mapViewer != null) SwingUtilities.invokeLater(() -> mapViewer.setOverlayPainter(null));
    }

    // Para el modo debug
    public void setOverlayPainter(Painter<JXMapViewer> painter) {
        if (mapViewer != null) SwingUtilities.invokeLater(() -> mapViewer.setOverlayPainter(painter));
    }
    
    public JXMapViewer getMapViewer() {
        return mapViewer;
    }
}