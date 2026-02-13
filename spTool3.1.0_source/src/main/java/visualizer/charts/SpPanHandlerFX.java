/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package visualizer.charts;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javafx.scene.input.MouseEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.interaction.AbstractMouseHandlerFX;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;

public class SpPanHandlerFX extends AbstractMouseHandlerFX {

    private final ViewerZoomMemory zoomMemory;

    /** The last mouse location seen during panning. */
    private Point2D panLast;
 
    private double panW;
    private double panH;
    
    /**
     * Creates a new instance that requires no modifier keys.
     * 
     * @param id  the id ({@code null} not permitted).
     */
    public SpPanHandlerFX(String id, ViewerZoomMemory zoomMemory) {
        this(id,zoomMemory, true, false, false, false);
    }
    
    /**
     * Creates a new instance that will be activated using the specified 
     * combination of modifier keys.
     * 
     * @param id  the id ({@code null} not permitted).
     * @param altKey  require ALT key?
     * @param ctrlKey  require CTRL key?
     * @param metaKey  require META key?
     * @param shiftKey   require SHIFT key?
     */
    public SpPanHandlerFX(String id, ViewerZoomMemory zoomMemory,
        boolean altKey, boolean ctrlKey,
            boolean metaKey, boolean shiftKey) {
        super(id, altKey, ctrlKey, metaKey, shiftKey);
        this.zoomMemory = zoomMemory;
    }
    
    /**
     * Handles a mouse pressed event by recording the initial mouse pointer
     * location.
     * 
     * @param canvas  the JavaFX canvas ({@code null} not permitted).
     * @param e  the mouse event ({@code null} not permitted).
     */
    @Override
    public void handleMousePressed(ChartCanvas canvas, MouseEvent e) {
    	if (canvas.getChart() == null) {
    		return;
    	}
        Plot plot = canvas.getChart().getPlot();
        if (!(plot instanceof Pannable)) {
            canvas.clearLiveHandler();
            return;
        }
        Pannable pannable = (Pannable) plot;
        if (pannable.isDomainPannable() || pannable.isRangePannable()) {
            Point2D point = new Point2D.Double(e.getX(), e.getY());
            Rectangle2D dataArea = canvas.findDataArea(point);
            if (dataArea != null && dataArea.contains(point)) {
                this.panW = dataArea.getWidth();
                this.panH = dataArea.getHeight();
                this.panLast = point;
                canvas.setCursor(javafx.scene.Cursor.MOVE);
            }
        }
        // the actual panning occurs later in the mouseDragged() method
    }
    
    /**
     * Handles a mouse dragged event by calculating the distance panned and
     * updating the axes accordingly.
     * 
     * @param canvas  the JavaFX canvas ({@code null} not permitted).
     * @param e  the mouse event ({@code null} not permitted).
     */
    @Override
    public void handleMouseDragged(ChartCanvas canvas, MouseEvent e) {
        if (this.panLast == null) {
            //handle panning if we have a start point else unregister
            canvas.clearLiveHandler();
            return;
        }

        JFreeChart chart = canvas.getChart();
        if (chart == null) {
            return;
        }
        double dx = e.getX() - this.panLast.getX();
        double dy = e.getY() - this.panLast.getY();
        if (dx == 0.0 && dy == 0.0) {
            return;
        }
        double wPercent = -dx / this.panW;
        double hPercent = dy / this.panH;
        boolean old = chart.getPlot().isNotify();
        chart.getPlot().setNotify(false);
        Pannable p = (Pannable) chart.getPlot();
        PlotRenderingInfo info = canvas.getRenderingInfo().getPlotInfo();
        if (p.getOrientation().isVertical()) {
            // store before setting the new thing
            zoomMemory.storeZoom();
            // then set
            p.panDomainAxes(wPercent, info, this.panLast);
            p.panRangeAxes(hPercent, info, this.panLast);
        }
        else {
            // store before setting the new thing
            zoomMemory.storeZoom();
            // then set
            p.panDomainAxes(hPercent, info, this.panLast);
            p.panRangeAxes(wPercent, info, this.panLast);
        }
        this.panLast = new Point2D.Double(e.getX(), e.getY());
        chart.getPlot().setNotify(old);
    }

    @Override
    public void handleMouseReleased(ChartCanvas canvas, MouseEvent e) {  
        //if we have been panning reset the cursor
        //unregister in any case
        if (this.panLast != null) {
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
        this.panLast = null;
        canvas.clearLiveHandler();
    }


    /**
     * Returns {@code true} if the specified mouse event has modifier
     * keys that match this handler.
     *
     * @param e  the mouse event ({@code null} not permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean hasMatchingModifiers(MouseEvent e) {
        boolean b =  super.hasMatchingModifiers(e);
        b = b || e.getClickCount() == 2;
        return b;
    }



}