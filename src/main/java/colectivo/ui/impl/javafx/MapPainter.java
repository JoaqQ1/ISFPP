package colectivo.ui.impl.javafx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * Dibuja sobre el mapa los tramos de un recorrido, uniendo las paradas con líneas
 * y marcando las paradas con puntos negros. Solo el último tramo tiene una flecha.
 */
public class MapPainter implements Painter<JXMapViewer> {

    private static final Logger LOGGER = LogManager.getLogger(MapPainter.class.getName());
    private final List<GeoPosition> paradas;
    private final Color color;

    public MapPainter(List<GeoPosition> paradas, Color color) {
        if(paradas == null || color == null) {
            LOGGER.error("Parámetros nulos proporcionados");
            throw new IllegalArgumentException("Parámetros nulos no permitidos");
        }
        this.paradas = paradas;
        this.color = color;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        if (paradas == null || paradas.size() < 2) {
            LOGGER.warn("paint: No hay suficientes paradas para dibujar");
            return;
        }
        if(g == null) {
            LOGGER.warn("paint: El objeto Graphics2D es nulo");
            return;
        }
        if(h == 0 || w == 0) {
            LOGGER.warn("paint: Dimensiones del viewport no válidas");
            return;
        }
        if(map == null){
            LOGGER.warn("paint: El objeto JXMapViewer es nulo");
            return;
        }
        

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle viewport = map.getViewportBounds();
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);

        // Dibuja líneas entre paradas
        for (int i = 0; i < paradas.size() - 1; i++) {
            GeoPosition desde = paradas.get(i);
            GeoPosition hasta = paradas.get(i + 1);

            Point2D ptDesde = map.getTileFactory().geoToPixel(desde, map.getZoom());
            Point2D ptHasta = map.getTileFactory().geoToPixel(hasta, map.getZoom());

            int x1 = (int) (ptDesde.getX() - viewport.getX());
            int y1 = (int) (ptDesde.getY() - viewport.getY());
            int x2 = (int) (ptHasta.getX() - viewport.getX());
            int y2 = (int) (ptHasta.getY() - viewport.getY());

            // Línea principal
            g2.draw(new Line2D.Double(x1, y1, x2, y2));

            // Si es el último tramo, agregamos la flecha
            if (i == paradas.size() - 2) {
                dibujarFlecha(g2, x1, y1, x2, y2);
            }
        }

        // Dibuja puntos negros en cada parada
        int radio = 8;
        for (GeoPosition parada : paradas) {
            Point2D punto = map.getTileFactory().geoToPixel(parada, map.getZoom());
            int x = (int) (punto.getX() - viewport.getX());
            int y = (int) (punto.getY() - viewport.getY());
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - radio / 2.0, y - radio / 2.0, radio, radio));
            g2.setColor(Color.BLACK);
            g2.fill(new Ellipse2D.Double(x - radio / 2.0 + 2, y - radio / 2.0 + 2, radio - 4, radio - 4));
        }


        g2.dispose();
    }

    /** Dibuja una flecha al final del recorrido */
    private void dibujarFlecha(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double phi = Math.toRadians(30);
        int tamaño = 10;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double theta = Math.atan2(dy, dx);
        double rho = theta + phi;

        for (int j = 0; j < 2; j++) {
            double x = x2 - tamaño * Math.cos(rho);
            double y = y2 - tamaño * Math.sin(rho);
            g2.draw(new Line2D.Double(x2, y2, x, y));
            rho = theta - phi;
        }
    }
}
