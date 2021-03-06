package gov.ornl.datatableview;

import gov.ornl.util.GraphicsUtil;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class CorrelationIndicatorRectangle extends Rectangle {
    public final static Color DEFAULT_CORRELATION_ZERO_COLOR = Color.GHOSTWHITE;
    public final static Color DEFAULT_CORRELATION_POSITIVE_COLOR = Color.DODGERBLUE;
    public final static Color DEFAULT_CORRELATION_NEGATIVE_COLOR = Color.DARKRED;

    private UnivariateAxis axis1;
    private UnivariateAxis axis2;
    private double correlation;

    public CorrelationIndicatorRectangle(UnivariateAxis axis1, UnivariateAxis axis2) {
        this.axis1 = axis1;
        this.axis2 = axis2;
    }

    public UnivariateAxis getAxis1() {
        return axis1;
    }

    public void setAxis1(UnivariateAxis axis1) {
        this.axis1 = axis1;
    }

    public UnivariateAxis getAxis2() {
        return axis2;
    }

    public void setAxis2(UnivariateAxis axis2) {
        this.axis2 = axis2;
    }

    public double getCorrelation() {
        return correlation;
    }

    public void setCorrelation(double correlation) {
        this.correlation = correlation;
        if (correlation > 0) {
            setFill(GraphicsUtil.lerpColorFX(DEFAULT_CORRELATION_ZERO_COLOR,
                    DEFAULT_CORRELATION_POSITIVE_COLOR, correlation));
        } else {
            setFill(GraphicsUtil.lerpColorFX(DEFAULT_CORRELATION_ZERO_COLOR,
                    DEFAULT_CORRELATION_NEGATIVE_COLOR, correlation * -1.));
        }

        Tooltip.install(this, new Tooltip(axis1.getColumn().getName() + " / " + axis2.getColumn().getName() +
                " correlation = " + correlation));
    }
}
