package gov.ornl.pcpview;

import gov.ornl.datatable.Column;
import gov.ornl.datatable.DataTable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public abstract class PCPAxis {
    public final static double DEFAULT_NAME_TEXT_SIZE = 12d;
    public final static Color DEFAULT_LABEL_COLOR = Color.BLACK;

    private PCPView pcpView;

    protected Text titleText;
    protected String title;
    protected Rectangle titleTextRectangle;
    protected DoubleProperty titleTextRotation = new SimpleDoubleProperty(0.0);;

    protected Color labelColor = DEFAULT_LABEL_COLOR;

    protected BooleanProperty highlighted;

    protected double centerX = 0d;
    protected double centerY = 0d;
    protected BoundingBox bounds;

    protected Group graphicsGroup = new Group();

    public PCPAxis(PCPView pcpView, String title) {
        this.pcpView = pcpView;
        this.title = title;

        titleText = new Text(title);
        titleText.setFont(new Font(DEFAULT_NAME_TEXT_SIZE));
        titleText.setSmooth(true);
        titleText.rotateProperty().bindBidirectional(titleTextRotation);
        titleText.setFill(labelColor);
        titleText.setMouseTransparent(true);

        titleTextRectangle = new Rectangle();
        titleTextRectangle.setStrokeWidth(3.);
        titleTextRectangle.setStroke(Color.TRANSPARENT);
        titleTextRectangle.setFill(Color.TRANSPARENT);
        titleTextRectangle.setArcWidth(6.);
        titleTextRectangle.setArcHeight(6.);

        graphicsGroup.getChildren().addAll(titleTextRectangle, titleText);

        registerListeners();
    }

    protected Text getTitleText() { return titleText; }

    public Group getGraphicsGroup() { return graphicsGroup; }

    public Bounds getBounds() { return bounds; }

    private void registerListeners() {
        titleText.textProperty().addListener((observable, oldValue, newValue) -> {
            titleText.setX(bounds.getMinX() + ((bounds.getWidth() - titleText.getLayoutBounds().getWidth()) / 2.));
            titleText.setY(bounds.getMinY() + titleText.getLayoutBounds().getHeight());
        });
    }

    protected PCPView getPCPView() { return pcpView; }

    protected DataTable getDataTable() { return pcpView.getDataTable(); }

    public void resize(double left, double top, double width, double height) {
        bounds = new BoundingBox(left, top, width, height);
        centerX = left + (width / 2.);
        centerY = top + (height / 2.);

        titleText.setText(title);
        if (titleText.getLayoutBounds().getWidth() > bounds.getWidth()) {
            // truncate the column name to fit axis bounds
            while (titleText.getLayoutBounds().getWidth() > bounds.getWidth()) {
                titleText.setText(titleText.getText().substring(0, titleText.getText().length() - 1));
            }
        }
        titleText.setX(bounds.getMinX() + ((width - titleText.getLayoutBounds().getWidth()) / 2.));
        titleText.setY(bounds.getMinY() + titleText.getLayoutBounds().getHeight());
        titleText.setRotate(getTitleTextRotation());

        titleTextRectangle.setX(titleText.getX() - 4.);
        titleTextRectangle.setY(titleText.getY() - titleText.getLayoutBounds().getHeight());
        titleTextRectangle.setWidth(titleText.getLayoutBounds().getWidth() + 8.);
        titleTextRectangle.setHeight(titleText.getLayoutBounds().getHeight() + 4.);
    }

    public final double getTitleTextRotation() { return titleTextRotation.get(); }

    public final void setTitleTextRotation(double value) { titleTextRotation.set(value); }

    public DoubleProperty titleTextRotationProperty() { return titleTextRotation; }
}
