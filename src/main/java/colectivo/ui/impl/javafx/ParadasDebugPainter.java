package colectivo.ui.impl.javafx;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.util.Collection;

/**
 * [DEBUG] Dibuja sobre el mapa un conjunto de paradas
 * usando un punto de color rodeado por un borde negro.
 * No dibuja líneas entre ellas.
 */
public class ParadasDebugPainter implements Painter<JXMapViewer> {

    private final Collection<GeoPosition> paradas;
    private final Color colorInterior; // <-- Color ahora es un parámetro

    /**
     * @param paradas Colección de paradas a dibujar
     * @param colorInterior El color de relleno para los puntos
     */
    public ParadasDebugPainter(Collection<GeoPosition> paradas, Color colorInterior) {
        this.paradas = paradas;
        this.colorInterior = colorInterior;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        if (paradas == null || paradas.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle viewport = map.getViewportBounds();

        // Dibuja puntos en cada parada
        int radio = 10;
        for (GeoPosition parada : paradas) {
            Point2D punto = map.getTileFactory().geoToPixel(parada, map.getZoom());
            int x = (int) (punto.getX() - viewport.getX());
            int y = (int) (punto.getY() - viewport.getY());
            
            // Círculo exterior negro (borde)
            g2.setColor(Color.BLACK);
            g2.fill(new Ellipse2D.Double(x - radio / 2.0, y - radio / 2.0, radio, radio));
            
            // Círculo interior del color especificado
            g2.setColor(this.colorInterior); // <-- Usamos el color del constructor
            g2.fill(new Ellipse2D.Double(x - radio / 2.0 + 2, y - radio / 2.0 + 2, radio - 4, radio - 4));
        }

        g2.dispose();
    }
}