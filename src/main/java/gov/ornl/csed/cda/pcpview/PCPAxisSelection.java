package gov.ornl.csed.cda.pcpview;

import gov.ornl.csed.cda.datatable.ColumnSelectionRange;
import gov.ornl.csed.cda.datatable.DataModel;
import gov.ornl.csed.cda.util.GraphicsUtil;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Pair;
import javafx.util.converter.NumberStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class PCPAxisSelection {
    private final static Logger log = LoggerFactory.getLogger(PCPAxisSelection.class);

    public final static Color DEFAULT_TEXT_FILL = Color.BLACK;
    public final static double DEFAULT_TEXT_SIZE = 8d;
    public final static Color DEFAULT_SELECTION_RECTANGLE_FILL_COLOR = new Color(Color.YELLOW.getRed(), Color.YELLOW.getGreen(), Color.YELLOW.getBlue(), 0.2);

    private DataModel dataModel;
    private PCPAxis pcpAxis;
    private Pane pane;
    private ColumnSelectionRange selectionRange;

    private Rectangle rectangle;
    private Polyline topCrossbar;
    private Polyline bottomCrossbar;
    private Text minText;
    private Text maxText;
    private Group graphicsGroup;

    // dragging variables
    private Point2D dragStartPoint;
    private Point2D dragEndPoint;
    private boolean dragging;
    private DoubleProperty draggingMinValue;
    private DoubleProperty draggingMaxValue;

    public PCPAxisSelection(PCPAxis pcpAxis, ColumnSelectionRange selectionRange, double minValueY, double maxValueY, Pane pane, DataModel dataModel) {
        this.pcpAxis = pcpAxis;
        this.selectionRange = selectionRange;
        this.pane = pane;
        this.dataModel = dataModel;

        this.selectionRange.maxValueProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("Got change notification from ColumnSelectionRange max value property");
            relayout();
        });
        this.selectionRange.minValueProperty().addListener(((observable, oldValue, newValue) -> {
            log.debug("Got change notification from ColumnSelectionRange min value property");
            relayout();;
        }));

        double top = Math.min(minValueY, maxValueY);
        double bottom = Math.max(minValueY, maxValueY);
        rectangle = new Rectangle(pcpAxis.getVerticalBar().getX(), top, pcpAxis.getVerticalBar().getWidth(), bottom - top);
        rectangle.setFill(DEFAULT_SELECTION_RECTANGLE_FILL_COLOR);
//        rectangle.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                // clear the selection
//                pane.getChildren().remove(rectangle);
//                pcpAxis.getAxisSelectionList().remove(this);
//                // TODO: Reset queried tuples
//            }
//        });

        // make top and bottom crossbars
        double left = rectangle.getX();
        double right = rectangle.getX() + rectangle.getWidth();
        topCrossbar = new Polyline(left, (top + 2d), left, top, right, top, right, (top + 2d));
        topCrossbar.setStroke(Color.BLACK);
        topCrossbar.setStrokeWidth(2d);
        bottomCrossbar = new Polyline(left, (bottom - 2d), left, bottom, right, bottom, right, (bottom - 2d));
        bottomCrossbar.setStroke(topCrossbar.getStroke());
        bottomCrossbar.setStrokeWidth(topCrossbar.getStrokeWidth());

//        minText = new Text(String.valueOf(selectionRange.getMinValue()));
        minText = new Text();
        minText.textProperty().bindBidirectional(getColumnSelectionRange().minValueProperty(), new NumberStringConverter());
        minText.setFont(new Font(DEFAULT_TEXT_SIZE));
        minText.setX(pcpAxis.getCenterX() - (minText.getLayoutBounds().getWidth() / 2d));
        minText.setY(getBottomY() + minText.getLayoutBounds().getHeight());

        minText.setFill(DEFAULT_TEXT_FILL);
        minText.setVisible(false);

//        maxText = new Text(String.valueOf(selectionRange.getMaxValue()));
        maxText = new Text();
        maxText.textProperty().bindBidirectional(getColumnSelectionRange().maxValueProperty(), new NumberStringConverter());
        maxText.setFont(new Font(DEFAULT_TEXT_SIZE));
        maxText.setX(pcpAxis.getCenterX() - (maxText.getLayoutBounds().getWidth() / 2d));
        maxText.setY(getTopY() - 2d);
        maxText.setFill(DEFAULT_TEXT_FILL);
        maxText.setVisible(false);

        graphicsGroup = new Group(topCrossbar, bottomCrossbar, minText, maxText, rectangle);
        pane.getChildren().add(graphicsGroup);

        registerListeners();
    }

    private void registerListeners() {
        rectangle.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.V_RESIZE);
                minText.setVisible(true);
                maxText.setVisible(true);
            }
        });

        rectangle.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.DEFAULT);
                minText.setVisible(false);
                maxText.setVisible(false);
            }
        });

//        bottomCrossbar.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                Dialog dialog = createNumberInputDialog("Change Selection Minimum Value", "Enter New Minimum Value", "MinimumValue", getColumnSelectionRange().getMinValue());
//                Optional<Double> result = dialog.showAndWait();
//
//                result.ifPresent(newMinValue -> {
//                    System.out.println(result.get());
//                });
//            }
//        });

        bottomCrossbar.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.V_RESIZE);
                maxText.setVisible(true);
                minText.setVisible(false);
            }
        });

        bottomCrossbar.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.DEFAULT);
                maxText.setVisible(false);
                minText.setVisible(false);
            }
        });

        topCrossbar.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.V_RESIZE);
                minText.setVisible(true);
            }
        });

        topCrossbar.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pane.getScene().setCursor(Cursor.DEFAULT);
                minText.setVisible(false);
            }
        });
        
        topCrossbar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dragStartPoint = new Point2D(event.getX(), event.getY());
                dragEndPoint = new Point2D(event.getX(), event.getY());
            }
        });

        topCrossbar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (!dragging) {
                    dragging = true;

                    // unbind range selection max label from selection range max property
                    maxText.textProperty().unbindBidirectional(getColumnSelectionRange().maxValueProperty());

                    // bind range selection max labels to local value during drag operation
                    draggingMaxValue = new SimpleDoubleProperty(getColumnSelectionRange().getMaxValue());
                    maxText.textProperty().bindBidirectional(draggingMaxValue, new NumberStringConverter());
                }

                double deltaY = event.getY() - dragEndPoint.getY();
                dragEndPoint = new Point2D(event.getX(), event.getY());

                double topY = getTopY() + deltaY;

                if (topY < pcpAxis.getFocusTopY()) {
                    topY = pcpAxis.getFocusTopY();
                }

                if (topY > getBottomY()) {
                    topY = getBottomY();
                }

                draggingMaxValue.set(GraphicsUtil.mapValue(topY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin()));

                layoutGraphics(getBottomY(), topY);
//                double maxSelectionValue = GraphicsUtil.mapValue(topY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
//                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin());
//
//                update(getColumnSelectionRange().getMinValue(), maxSelectionValue, topY, getBottomY());
            }
        });

        topCrossbar.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (dragging) {
                    dragging = false;

                    // update column selection range max property
                    selectionRange.setMaxValue(draggingMaxValue.get());

                    // unbind selection range max label from dragging max range value
                    maxText.textProperty().unbindBidirectional(draggingMaxValue);

                    // bind selection range max label to column selection range max property
                    maxText.textProperty().bindBidirectional(getColumnSelectionRange().maxValueProperty(), new NumberStringConverter());

//                    dataModel.setQueriedTuples();
                }
            }
        });

        bottomCrossbar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dragStartPoint = new Point2D(event.getX(), event.getY());
                dragEndPoint = new Point2D(event.getX(), event.getY());
            }
        });

        bottomCrossbar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
//                dragging = true;
                if (!dragging) {
                    dragging = true;

                    // unbind range selection min labels from selection range min properties
                    minText.textProperty().unbindBidirectional(getColumnSelectionRange().minValueProperty());

                    // bind range selection min/max labels to local values during drag operation
                    draggingMinValue = new SimpleDoubleProperty(getColumnSelectionRange().getMinValue());
                    minText.textProperty().bindBidirectional(draggingMinValue, new NumberStringConverter());
                }

                double deltaY = event.getY() - dragEndPoint.getY();
                dragEndPoint = new Point2D(event.getX(), event.getY());

                double bottomY = getBottomY() + deltaY;

                if (bottomY > pcpAxis.getFocusBottomY()) {
                    bottomY = pcpAxis.getFocusBottomY();
                }

                if (bottomY < getTopY()) {
                    bottomY = getTopY();
                }

                draggingMinValue.set(GraphicsUtil.mapValue(bottomY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin()));
                layoutGraphics(bottomY, getTopY());

//                double minSelectionValue = GraphicsUtil.mapValue(bottomY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
//                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin());
//
//                update(minSelectionValue, getColumnSelectionRange().getMaxValue(), getTopY(), bottomY);
            }
        });

        bottomCrossbar.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (dragging) {
                    dragging = false;

                    // update column selection range min properties
                    selectionRange.setMinValue(draggingMinValue.get());

                    // unbind selection range min labels from dragging min range value
                    minText.textProperty().unbindBidirectional(draggingMinValue);

                    // bind selection range min labels to column selection range min property
                    minText.textProperty().bindBidirectional(getColumnSelectionRange().minValueProperty(), new NumberStringConverter());

//                    dataModel.setQueriedTuples();
                }
            }
        });

//        rectangle.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (event.getClickCount() == 2) {
//                    Dialog dialog = createNumberInputDialog("Change Selection Minimum Value", "Enter New Minimum Value", "MinimumValue", getColumnSelectionRange().getMinValue());
//                    Optional<Double> result = dialog.showAndWait();
//
//                    result.ifPresent(newMinValue -> {
//                        System.out.println(result.get());
//                    });
//                }
//            }
//        });

        rectangle.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dragStartPoint = new Point2D(event.getX(), event.getY());
                dragEndPoint = new Point2D(event.getX(), event.getY());

                if (event.isPopupTrigger()) {
                    Dialog dialog = createSelectionRangeInputDialog(getColumnSelectionRange().getMinValue(), getColumnSelectionRange().getMaxValue());
                    Optional<Pair<Double,Double>> result = dialog.showAndWait();

                    result.ifPresent(newMinValue -> {
                        double minValue = result.get().getKey();
                        double maxValue = result.get().getValue();

                        // ensure min is the min and max is the max
                        minValue = Math.min(minValue, maxValue);
                        maxValue = Math.max(minValue, maxValue);

                        // clamp within the bounds of the focus range
                        minValue = minValue < getPCPAxis().getColumn().getSummaryStats().getMin() ? getPCPAxis().getColumn().getSummaryStats().getMin() : minValue;
                        maxValue = maxValue > getPCPAxis().getColumn().getSummaryStats().getMax() ? getPCPAxis().getColumn().getSummaryStats().getMax() : maxValue;

                        // find the y positions for the min and max
                        double topY = GraphicsUtil.mapValue(maxValue, pcpAxis.getColumn().getSummaryStats().getMin(), pcpAxis.getColumn().getSummaryStats().getMax(),
                                pcpAxis.getFocusBottomY(), pcpAxis.getFocusTopY());
                        double bottomY = GraphicsUtil.mapValue(minValue, pcpAxis.getColumn().getSummaryStats().getMin(), pcpAxis.getColumn().getSummaryStats().getMax(),
                                pcpAxis.getFocusBottomY(), pcpAxis.getFocusTopY());

                        // update display and data model values
                        update(minValue, maxValue, bottomY, topY);
                    });
                }
            }
        });

        rectangle.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (!dragging) {
                    dragging = true;

                    // unbind range selection min/max labels from selection range min/max properties
                    minText.textProperty().unbindBidirectional(getColumnSelectionRange().minValueProperty());
                    maxText.textProperty().unbindBidirectional(getColumnSelectionRange().maxValueProperty());

                    // bind range selection min/max labels to local values during drag operation
                    draggingMinValue = new SimpleDoubleProperty(getColumnSelectionRange().getMinValue());
                    draggingMaxValue = new SimpleDoubleProperty(getColumnSelectionRange().getMaxValue());
                    minText.textProperty().bindBidirectional(draggingMinValue, new NumberStringConverter());
                    maxText.textProperty().bindBidirectional(draggingMaxValue, new NumberStringConverter());
                }

                double deltaY = event.getY() - dragEndPoint.getY();
                dragEndPoint = new Point2D(event.getX(), event.getY());

                double topY = getTopY() + deltaY;
                double bottomY = getBottomY() + deltaY;

                if (topY < pcpAxis.getFocusTopY()) {
                    deltaY = pcpAxis.getFocusTopY() - topY;
                    topY = pcpAxis.getFocusTopY();
                    bottomY = bottomY + deltaY;
                }

                if (bottomY > pcpAxis.getFocusBottomY()) {
                    deltaY = bottomY - pcpAxis.getFocusBottomY();
                    topY = topY - deltaY;
                    bottomY = pcpAxis.getFocusBottomY();
                }

                draggingMaxValue.set(GraphicsUtil.mapValue(topY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin()));
                draggingMinValue.set(GraphicsUtil.mapValue(bottomY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin()));

                layoutGraphics(bottomY, topY);
//                double maxSelectionValue = GraphicsUtil.mapValue(topY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
//                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin());
//                double minSelectionValue = GraphicsUtil.mapValue(bottomY, pcpAxis.getFocusTopY(), pcpAxis.getFocusBottomY(),
//                        pcpAxis.getColumn().getSummaryStats().getMax(), pcpAxis.getColumn().getSummaryStats().getMin());

//                update(minSelectionValue, maxSelectionValue, topY, bottomY);
            }
        });

        rectangle.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Axis selection mouse Released");
                if (dragging) {
                    dragging = false;

                    // update column selection range min/max properties
                    selectionRange.setMaxValue(draggingMaxValue.get());
                    selectionRange.setMinValue(draggingMinValue.get());

                    // unbind selection range min/max labels from dragging min/max range values
                    minText.textProperty().unbindBidirectional(draggingMinValue);
                    maxText.textProperty().unbindBidirectional(draggingMaxValue);

                    // bind selection range min/max labels to column selection range min/max properties
                    minText.textProperty().bindBidirectional(getColumnSelectionRange().minValueProperty(), new NumberStringConverter());
                    maxText.textProperty().bindBidirectional(getColumnSelectionRange().maxValueProperty(), new NumberStringConverter());

//                    dataModel.setQueriedTuples();
                } else {
                    pane.getChildren().remove(getGraphicsGroup());
                    pcpAxis.getAxisSelectionList().remove(this);
                    dataModel.clearColumnSelectionRange(getColumnSelectionRange());
//                    dataModel.setQueriedTuples();
                }
            }
        });
    }

    public void relayout() {
        double topY = GraphicsUtil.mapValue(selectionRange.getMaxValue(), pcpAxis.getColumn().getSummaryStats().getMin(), pcpAxis.getColumn().getSummaryStats().getMax(),
                pcpAxis.getFocusBottomY(), pcpAxis.getFocusTopY());
        double bottomY = GraphicsUtil.mapValue(selectionRange.getMinValue(), pcpAxis.getColumn().getSummaryStats().getMin(), pcpAxis.getColumn().getSummaryStats().getMax(),
                pcpAxis.getFocusBottomY(), pcpAxis.getFocusTopY());
        layoutGraphics(bottomY, topY);
    }

    private void layoutGraphics(double bottomY, double topY) {
        double top = Math.min(bottomY, topY);
        double bottom = Math.max(bottomY, topY);

        rectangle.setY(top);
        rectangle.setHeight(bottom - top);
        rectangle.setX(pcpAxis.getVerticalBar().getX());
        rectangle.setWidth(pcpAxis.getVerticalBar().getWidth());

        double left = rectangle.getX();
        double right = rectangle.getX() + rectangle.getWidth();
        topCrossbar.getPoints().set(0, left);
        topCrossbar.getPoints().set(1, top + 2d);
        topCrossbar.getPoints().set(2, left);
        topCrossbar.getPoints().set(3, top);
        topCrossbar.getPoints().set(4, right);
        topCrossbar.getPoints().set(5, top);
        topCrossbar.getPoints().set(6, right);
        topCrossbar.getPoints().set(7, top + 2d);

        bottomCrossbar.getPoints().set(0, left);
        bottomCrossbar.getPoints().set(1, bottom - 2d);
        bottomCrossbar.getPoints().set(2, left);
        bottomCrossbar.getPoints().set(3, bottom);
        bottomCrossbar.getPoints().set(4, right);
        bottomCrossbar.getPoints().set(5, bottom);
        bottomCrossbar.getPoints().set(6, right);
        bottomCrossbar.getPoints().set(7, bottom - 2d);

//        minText.setText(String.valueOf(selectionRange.getMinValue()));
        minText.setY(getBottomY() + minText.getLayoutBounds().getHeight());
        minText.setX(pcpAxis.getCenterX() - (minText.getLayoutBounds().getWidth() / 2d));

//        maxText.setText(String.valueOf(selectionRange.getMaxValue()));
        maxText.setX(pcpAxis.getCenterX() - (maxText.getLayoutBounds().getWidth() / 2d));
        maxText.setY(getTopY() - 2d);
    }

    public void update(double minValue, double maxValue, double minValueY, double maxValueY) {
        selectionRange.setMaxValue(maxValue);
        selectionRange.setMinValue(minValue);
        layoutGraphics(minValueY, maxValueY);
    }

    private Dialog<Pair<Double, Double>> createSelectionRangeInputDialog (double minValue, double maxValue) {
        Dialog<Pair<Double, Double>> dialog = new Dialog<>();
        dialog.setTitle("Change Selection Value Range");
        dialog.setHeaderText("Enter New Minimum and Maximum Range Values");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 150, 10, 10));

        NumberTextField minValueField = new NumberTextField();
        minValueField.setText(String.valueOf(minValue));
        NumberTextField maxValueField = new NumberTextField();
        maxValueField.setText(String.valueOf(maxValue));

        grid.add(new Label(" Maximum Value: "), 0, 0);
        grid.add(maxValueField, 1, 0);
        grid.add(new Label(" Minimum Value: "), 0, 1);
        grid.add(minValueField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> minValueField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<Double, Double> (new Double(minValueField.getText()), new Double(maxValueField.getText()));
            }
            return null;
        });

        return dialog;
    }

    public double getTopY () {
        return rectangle.getY();
    }

    public double getBottomY() {
        return rectangle.getY() + rectangle.getHeight();
    }

    public Group getGraphicsGroup() { return graphicsGroup; }

    public Rectangle getRectangle() { return rectangle; }

    public ColumnSelectionRange getColumnSelectionRange() {
        return selectionRange;
    }

    public PCPAxis getPCPAxis () { return pcpAxis; }

    public Pane getPane() { return pane; }
}
