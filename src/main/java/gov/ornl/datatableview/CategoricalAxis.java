package gov.ornl.datatableview;

import gov.ornl.datatable.*;
import gov.ornl.util.GraphicsUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.text.DecimalFormat;
import java.util.*;

public class CategoricalAxis extends UnivariateAxis {
    private static final DecimalFormat percentageFormat = new DecimalFormat("0.0#%");

    private final static double DEFAULT_CATEGORY_STROKE_WIDTH = 1.f;
    private final static Color DEFAULT_CATEGORY_STROKE_COLOR = new Color(0.1, 0.1, 0.1, 1.0);
    private final static Color DEFAULT_CATEGORY_FILL_COLOR = Color.LIGHTGRAY;
    private final static Color DEFAULT_QUERY_STROKE_COLOR = new Color(0.5, 0.5, 0.5, 1.0);
    private final static Color DEFAULT_SELECTED_CATEGORY_STROKE_COLOR = new Color(DEFAULT_SELECTION_FILL_COLOR.getRed(),
        DEFAULT_SELECTION_FILL_COLOR.getGreen(), DEFAULT_SELECTION_FILL_COLOR.getBlue(), 1.);

    // category rectangles
    private Group categoriesRectangleGroup;
    private HashMap<String, Rectangle> categoriesRectangleMap = new HashMap<>();
    private Group categoriesNameGraphicsGroup;

    private Group selectionIndicatorsGroup;

    // query category rectangles
    private Group queryCategoriesRectangleGroup;
    private HashMap<String, Rectangle> queryCategoriesRectangleMap = new HashMap<>();

    private Group nonQueryCategoriesRectangleGroup;
    private HashMap<String, Rectangle> nonQueryCategoriesRectangleMap = new HashMap<>();

    private BooleanProperty showCategoryLabels = new SimpleBooleanProperty(false);
    private BooleanProperty categoryHeightProportionalToCount = new SimpleBooleanProperty(true);


    public CategoricalAxis(DataTableView dataTableView, CategoricalColumn column) {
        super(dataTableView, column);

        getAxisBar().setWidth(getAxisBar().getWidth() + 10);

        categoriesRectangleGroup = new Group();
        categoriesNameGraphicsGroup = new Group();
        queryCategoriesRectangleGroup = new Group();
        nonQueryCategoriesRectangleGroup = new Group();
        selectionIndicatorsGroup = new Group();

        getGraphicsGroup().getChildren().addAll(selectionIndicatorsGroup, categoriesRectangleGroup, categoriesNameGraphicsGroup);
        
        getUpperContextBar().setVisible(false);
        getLowerContextBar().setVisible(false);
        getUpperContextBarHandle().setVisible(false);
        getLowerContextBarHandle().setVisible(false);

        registerListeners();
    }

    public boolean isCategoryHeightProportionalToCount() { return categoryHeightProportionalToCount.get(); }

    public void setCategoryHeightProportionalToCount(boolean enabled) {
        if (enabled != isCategoryHeightProportionalToCount()) {
            categoryHeightProportionalToCount.set(enabled);
        }
    }

    public BooleanProperty categoryHeightProportionalToCountProperty() { return categoryHeightProportionalToCount; }

    public boolean isShowingCategoryLabels() { return showCategoryLabels.get(); }

    public void setShowCategoryLabels(boolean show) {
        if (show != isShowingCategoryLabels()) {
            showCategoryLabels.set(show);
        }
    }

    public BooleanProperty showCategoryLabelsProperty() { return showCategoryLabels; }

    @Override
    protected AxisSelection addAxisSelection(ColumnSelection columnSelection) {
        // see if an axis selection already exists for the column selection
        for (AxisSelection axisSelection : getAxisSelectionList()) {
            if (axisSelection.getColumnSelection() == columnSelection) {
                // an axis selection already exists for the given column selection so abort
                return null;
            }
        }

        CategoricalColumnSelection categoricalColumnSelection = (CategoricalColumnSelection)columnSelection;

        CategoricalAxisSelection newAxisSelection = new CategoricalAxisSelection(this, categoricalColumnSelection);
        getAxisSelectionList().add(newAxisSelection);

        return newAxisSelection;
    }

    @Override
    protected Object getValueForAxisPosition(double axisPosition) {
        for (String category : categoriesRectangleMap.keySet()) {
            Rectangle rectangle = categoriesRectangleMap.get(category);
            if (axisPosition >= rectangle.getLayoutBounds().getMinY() &&
                    axisPosition < rectangle.getLayoutBounds().getMaxY()) {
                return category;
            }
        }
        return null;
    }

    protected double getAxisPositionForValue(String category) {
        Rectangle categoryRectangle = categoriesRectangleMap.get(category);
        return categoryRectangle.getY() + (categoryRectangle.getHeight() / 2.);
    }

    public List<String> getCategories() {
        return categoricalColumn().getCategories();
    }

    public Rectangle getCategoryRectangle(String category) {
        return categoriesRectangleMap.get(category);
    }

    public Rectangle getQueryCategoryRectangle(String category) {
        return queryCategoriesRectangleMap.get(category);
    }

    public Rectangle getNonQueryCategoryRectangle(String category) {
        return nonQueryCategoriesRectangleMap.get(category);
    }

    private CategoricalColumn categoricalColumn() {
        return (CategoricalColumn)getColumn();
    }

    private void registerListeners() {
        categoriesNameGraphicsGroup.visibleProperty().bind(showCategoryLabelsProperty());
        categoryHeightProportionalToCount.addListener(observable -> {
            getDataTableView().resizeView();
        });
    }

    private void handleCategoryRectangleClicked(Rectangle rectangle, String category) {
        // if there are no current axis selections, make a new selection and add this category
        if (getAxisSelectionList().isEmpty()) {
            HashSet<String> categories = new HashSet<>();
            categories.add(category);
            CategoricalColumnSelection columnSelection = new CategoricalColumnSelection(categoricalColumn(), categories);
            CategoricalAxisSelection axisSelection = new CategoricalAxisSelection(this, columnSelection);
            getAxisSelectionList().add(axisSelection);
            getDataTable().addColumnSelectionToActiveQuery(columnSelection);
        } else {
            ArrayList<AxisSelection> selectionsToRemove = new ArrayList<>();
            for (AxisSelection selection : getAxisSelectionList()) {
                CategoricalAxisSelection categoricalSelection = (CategoricalAxisSelection)selection;
                CategoricalColumnSelection categoricalColumnSelection = (CategoricalColumnSelection)categoricalSelection.getColumnSelection();

                if (categoricalColumnSelection.getSelectedCategories().contains(category)) {
                    // remove the category from the selection
                    categoricalColumnSelection.removeCategory(category);
                    if (categoricalColumnSelection.getSelectedCategories().isEmpty()) {
                        selectionsToRemove.add(selection);
                    }
                } else {
                    categoricalColumnSelection.addCategory(category);
                }
            }
            if (!selectionsToRemove.isEmpty()) {
                getAxisSelectionList().removeAll(selectionsToRemove);
            }
        }
//        rectangle.toFront();
    }

    public void resize(double left, double top, double width, double height) {
        super.resize(left, top, width, height);

        if (!getDataTable().isEmpty()) {
            CategoricalHistogram histogram = categoricalColumn().getStatistics().getHistogram();

            HashSet<String> selectedCategories = new HashSet<>();
            for (AxisSelection axisSelection : getAxisSelectionList()) {
                selectedCategories.addAll(((CategoricalColumnSelection)axisSelection.getColumnSelection()).getSelectedCategories());
            }

            // remove previously shown category shapes
            categoriesRectangleGroup.getChildren().clear();
            categoriesRectangleMap.clear();
            categoriesNameGraphicsGroup.getChildren().clear();
            selectionIndicatorsGroup.getChildren().clear();

            double lastRectangleBottomY = getMaxFocusPosition();

            List<String> categories = histogram.getCategories();
            if (isCategoryHeightProportionalToCount()) {
                categories.sort((o1, o2) -> histogram.getCategoryCount(o2) - histogram.getCategoryCount(o1));
            } else {
                categories.sort(Comparator.reverseOrder());
            }

            for (String category : categories) {
                int categoryCount = histogram.getCategoryCount(category);
                if (isCategoryHeightProportionalToCount() && categoryCount == 0) {
                    continue;
                }

                double y = lastRectangleBottomY;
                Rectangle rectangle;
                if (isCategoryHeightProportionalToCount()) {
                    double categoryHeight = GraphicsUtil.mapValue(categoryCount, 0, histogram.getTotalCount(), 0, getMinFocusPosition() - getMaxFocusPosition());
                    rectangle = new Rectangle(getAxisBar().getX()+4, y, getAxisBar().getWidth()-8, categoryHeight);
                } else {
                    double categoryHeight = (getMinFocusPosition() - getMaxFocusPosition()) / categoricalColumn().getCategories().size();
                    rectangle = new Rectangle(getAxisBar().getX()+4, y, getAxisBar().getWidth()-8, categoryHeight);
                }

                if (rectangle.getHeight() > 6) {
                    rectangle.setStroke(DEFAULT_CATEGORY_STROKE_COLOR);
                } else {
                    double opacity = GraphicsUtil.mapValue(rectangle.getHeight(), 0, 6, 0.1, 1.0);
                    rectangle.setStroke(DEFAULT_CATEGORY_STROKE_COLOR.deriveColor(1.,1.,1., opacity));
                }

                rectangle.setFill(DEFAULT_CATEGORY_FILL_COLOR);
                rectangle.setStrokeWidth(DEFAULT_CATEGORY_STROKE_WIDTH);
                rectangle.setArcHeight(6);
                rectangle.setArcWidth(6);

                Text categoryName = new Text(category);
                categoryName.setFill(DEFAULT_TEXT_COLOR);
                categoryName.setFont(Font.font(DEFAULT_TEXT_SIZE));
                categoryName.setMouseTransparent(true);
//                categoryName.setX(getCenterX() - (categoryName.getLayoutBounds().getWidth() / 2.));
                categoryName.setX(getBarLeftX() - (categoryName.getLayoutBounds().getWidth() + 1.));
                categoryName.setY(y + categoryName.getLayoutBounds().getHeight() + 2);

                Rectangle categoryNameRectangle = new Rectangle(categoryName.getLayoutBounds().getMinX(), categoryName.getLayoutBounds().getMinY(),
                        categoryName.getLayoutBounds().getWidth(), categoryName.getLayoutBounds().getHeight());
                categoryNameRectangle.setFill(Color.GHOSTWHITE.deriveColor(1., 1., 1., 0.7));
                categoryNameRectangle.setStroke(Color.TRANSPARENT);
                categoryNameRectangle.setMouseTransparent(true);

                rectangle.setOnMouseClicked(event -> {
                    handleCategoryRectangleClicked(rectangle, category);
                });
                rectangle.setOnMouseEntered(event -> {
                    rectangle.setStrokeWidth(DEFAULT_CATEGORY_STROKE_WIDTH+2);
                    rectangle.toFront();
                });
                rectangle.setOnMouseExited(event -> {
                    rectangle.setStrokeWidth(DEFAULT_CATEGORY_STROKE_WIDTH);
                });

                Tooltip.install(rectangle, new Tooltip(category + ": " + categoryCount + "/" +
                        histogram.getTotalCount() + " (" + percentageFormat.format((double) categoryCount / histogram.getTotalCount()) + ") of total"));

                categoriesRectangleMap.put(category, rectangle);
//                if (categoricalColumn().getCategories().size() < (getAxisBar().getHeight()/4.)) {
                    categoriesRectangleGroup.getChildren().add(rectangle);
//                }

                if (selectedCategories.contains(category)) {
                    Rectangle selectionIndicator = new Rectangle(getAxisBar().getX()+1, y, getAxisBar().getWidth()-2, rectangle.getHeight());
                    selectionIndicator.setFill(DEFAULT_SELECTED_CATEGORY_STROKE_COLOR);
                    selectionIndicator.setStroke(null);
                    selectionIndicator.setMouseTransparent(true);
                    selectionIndicatorsGroup.getChildren().add(selectionIndicator);

//                    rectangle.setFill(DEFAULT_SELECTED_CATEGORY_STROKE_COLOR);
//                    Rectangle innerRectangle = new Rectangle(rectangle.getX() + 1, rectangle.getY() + 1,
//                            rectangle.getWidth() - 2, rectangle.getHeight() - 2);
//                    innerRectangle.setStroke(DEFAULT_SELECTED_CATEGORY_STROKE_COLOR);
//                    innerRectangle.setFill(null);
//                    innerRectangle.setArcWidth(6);
//                    innerRectangle.setArcHeight(6);
//                    innerRectangle.setMouseTransparent(true);
//                    categoriesRectangleGroup.getChildren().add(innerRectangle);
                }

                lastRectangleBottomY = rectangle.getY() + rectangle.getHeight();

                categoriesNameGraphicsGroup.getChildren().addAll(categoryNameRectangle, categoryName);
            }

            if (getGraphicsGroup().getChildren().contains(queryCategoriesRectangleGroup)) {
                getGraphicsGroup().getChildren().remove(queryCategoriesRectangleGroup);
            }
            if (getGraphicsGroup().getChildren().contains(nonQueryCategoriesRectangleGroup)) {
                getGraphicsGroup().getChildren().remove(nonQueryCategoriesRectangleGroup);
            }

            queryCategoriesRectangleGroup.getChildren().clear();
            queryCategoriesRectangleMap.clear();
            nonQueryCategoriesRectangleGroup.getChildren().clear();
            nonQueryCategoriesRectangleMap.clear();

            if (getDataTable().getActiveQuery().hasColumnSelections()) {
                CategoricalColumnSummaryStats queryColumnSummaryStats = (CategoricalColumnSummaryStats)getDataTable().getActiveQuery().getColumnQuerySummaryStats(getColumn());
                CategoricalHistogram queryHistogram = queryColumnSummaryStats.getHistogram();

                for (String category : queryHistogram.getCategories()) {
                    int queryCategoryCount = queryHistogram.getCategoryCount(category);
                    Rectangle overallCategoryRectangle = categoriesRectangleMap.get(category);

                    if ((overallCategoryRectangle != null) && (overallCategoryRectangle.getHeight() > 5)) {
                        int overallCategoryCount = histogram.getCategoryCount(category);
                        int nonQueryCategoryCount = overallCategoryCount - queryCategoryCount;

                        Rectangle queryRectangle = new Rectangle(overallCategoryRectangle.getLayoutBounds().getMinX() + 3,
                                0, overallCategoryRectangle.getLayoutBounds().getWidth() - 6, 0);
                        Rectangle nonQueryRectangle = new Rectangle(overallCategoryRectangle.getLayoutBounds().getMinX() + 3,
                                0, overallCategoryRectangle.getLayoutBounds().getWidth() - 6, 0);

                        if (queryCategoryCount > 0) {
                            queryRectangle.setY(overallCategoryRectangle.getY() + 2d);

                            if (nonQueryCategoryCount > 0) {
                                double queryRectangleHeight = GraphicsUtil.mapValue(queryCategoryCount, 0, overallCategoryCount,
                                        0, overallCategoryRectangle.getHeight() - 4);
                                queryRectangle.setHeight(queryRectangleHeight);
                            } else {
                                queryRectangle.setHeight(overallCategoryRectangle.getHeight() - 4);
                            }

//                            queryRectangle.setArcHeight(6);
//                            queryRectangle.setArcWidth(6);
                            queryRectangle.setStroke(DEFAULT_QUERY_STROKE_COLOR);
                            queryRectangle.setFill(new Color(getDataTableView().getSelectedItemsColor().getRed(),
                                    getDataTableView().getSelectedItemsColor().getGreen(),
                                    getDataTableView().getSelectedItemsColor().getBlue(),
                                    1.0));
                            queryRectangle.setMouseTransparent(true);

                            queryCategoriesRectangleMap.put(category, queryRectangle);
                            queryCategoriesRectangleGroup.getChildren().add(queryRectangle);
                        }

                        if (nonQueryCategoryCount > 0) {
                            if (queryCategoryCount > 0) {
                                double nonQueryRectangleHeight = GraphicsUtil.mapValue(nonQueryCategoryCount, 0,
                                        overallCategoryCount, 0, overallCategoryRectangle.getHeight() - 4);
                                nonQueryRectangle.setHeight(nonQueryRectangleHeight);
                                nonQueryRectangle.setY(queryRectangle.getY() + queryRectangle.getHeight());
                            } else {
                                nonQueryRectangle.setHeight(overallCategoryRectangle.getHeight() - 4);
                                nonQueryRectangle.setY(overallCategoryRectangle.getY() + 2d);
                            }

//                            nonQueryRectangle.setArcHeight(6);
//                            nonQueryRectangle.setArcWidth(6);
                            nonQueryRectangle.setStroke(DEFAULT_QUERY_STROKE_COLOR);
                            nonQueryRectangle.setFill(new Color(getDataTableView().getUnselectedItemsColor().getRed(),
                                    getDataTableView().getUnselectedItemsColor().getGreen(),
                                    getDataTableView().getUnselectedItemsColor().getBlue(),
                                    1.0));
                            nonQueryRectangle.setMouseTransparent(true);

                            nonQueryCategoriesRectangleMap.put(category, nonQueryRectangle);
                            nonQueryCategoriesRectangleGroup.getChildren().add(nonQueryRectangle);
                        }

                        Tooltip.install(overallCategoryRectangle,
                                new Tooltip(category + ": " + queryCategoryCount + "/" + overallCategoryCount +
                                        " (" + percentageFormat.format((double) queryCategoryCount / overallCategoryCount) + ") selected"));
//                                new Tooltip(category + " : " + overallCategoryCount + " of " +
//                                        histogram.getTotalCount() + " (" +
//                                        percentageFormat.format((double) overallCategoryCount / histogram.getTotalCount()) + " of total)\n" +
//                                        queryCategoryCount + " of " + overallCategoryCount + " selected (" +
//                                        percentageFormat.format((double) queryCategoryCount / overallCategoryCount) + ")"));
                    }
                }

                getGraphicsGroup().getChildren().addAll(nonQueryCategoriesRectangleGroup, queryCategoriesRectangleGroup);
            }

            categoriesNameGraphicsGroup.toFront();
        }
    }
}
