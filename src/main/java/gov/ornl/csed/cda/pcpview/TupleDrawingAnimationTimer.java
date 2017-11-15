package gov.ornl.csed.cda.pcpview;

import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by csg on 10/21/16.
 */
public class TupleDrawingAnimationTimer extends AnimationTimer {
    public final static Logger log = LoggerFactory.getLogger(TupleDrawingAnimationTimer.class);

    private Canvas canvas;
    private Color tupleColor;
    private ArrayBlockingQueue<PCPTuple> tupleQueue;
    private int maxTuplesPerFrame;
    private ArrayList<PCPQuantitativeAxis> axisList;
    private PCPTemporalAxis temporalAxis;
    private BooleanProperty running;
    public long id;

    public TupleDrawingAnimationTimer (Canvas canvas, Collection<PCPTuple> tuples, ArrayList<PCPQuantitativeAxis> axisList, PCPTemporalAxis temporalAxis, Color tupleColor, int maxTuplesPerFrame) {
        id = System.currentTimeMillis();
        this.canvas = canvas;
        this.axisList = axisList;
        this.temporalAxis = temporalAxis;
        this.tupleColor = tupleColor;
        tupleQueue = new ArrayBlockingQueue<PCPTuple>(tuples.size());
        tupleQueue.addAll(tuples);
        this.maxTuplesPerFrame = maxTuplesPerFrame;
        running = new SimpleBooleanProperty(false);
    }

    public final boolean isRunning() { return running.get(); }

    public ReadOnlyBooleanProperty runningProperty() { return running; }

    @Override
    public void handle(long now) {
//        log.debug("Drawing frame");
        canvas.getGraphicsContext2D().setStroke(tupleColor);

        for (int ituple = 0; ituple < maxTuplesPerFrame; ituple++) {
            PCPTuple pcpTuple = tupleQueue.poll();
            if (pcpTuple == null) {
//                log.debug("Stopping Timer normally (id: " + id + ")");
                this.stop();
                break;
            } else {
                if (temporalAxis != null) {
                    canvas.getGraphicsContext2D().strokeLine(temporalAxis.getBarRightX(), pcpTuple.getYPoints()[0],
                            axisList.get(0).getBarLeftX(), pcpTuple.getYPoints()[1]);
                    for (int i = 2; i < pcpTuple.getXPoints().length; i++) {
                        canvas.getGraphicsContext2D().strokeLine(axisList.get(i - 2).getBarRightX(), pcpTuple.getYPoints()[i - 1],
                                axisList.get(i - 1).getBarLeftX(), pcpTuple.getYPoints()[i]);
                    }
                } else {
                    for (int i = 1; i < pcpTuple.getXPoints().length; i++) {
                        canvas.getGraphicsContext2D().strokeLine(axisList.get(i - 1).getBarRightX(), pcpTuple.getYPoints()[i - 1],
                                axisList.get(i).getBarLeftX(), pcpTuple.getYPoints()[i]);
                    }
                }
            }
        }
//        log.debug("Finished Frame");
    }

    @Override
    public void start() {
//        log.debug("Started TupleDrawingAnimationTimer with id " + id);
        super.start();
        running.set(true);
    }

    @Override
    public void stop() {
        super.stop();
        running.set(false);
    }
}
