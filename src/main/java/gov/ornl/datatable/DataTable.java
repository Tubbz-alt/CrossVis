package gov.ornl.datatable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.SetChangeListener;
import javafx.scene.image.Image;
import javafx.util.Pair;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DataTable {
    private static final int DEFAULT_NUM_HISTOGRAM_BINS = 50;
    private static final int MAX_NUM_HISTOGRAM_BINS = 100;

    private final static Logger log = Logger.getLogger(DataTable.class.getName());

    // List of enabled tuples
    protected ArrayList<Tuple> tuples;

    protected HashSet<Tuple> focusTuples = new HashSet<>();

    protected HashSet<Tuple> contextTuples = new HashSet<>();

    // List of enabled columns
    protected ArrayList<Column> columns;

    // List of disabled columns
    protected ArrayList<Column> disabledColumns;

    // List of tuple elements for disabled columns
    protected ArrayList<Tuple> disabledColumnTuples;

    // List of active listeners
    private ArrayList<DataTableListener> listeners;

    // Special columns
    private Column highlightedColumn = null;

    // List of saved queries
    private ArrayList<Query> savedQueryList;

    // Current active query
    private Query activeQuery;

    // Sequential number for future query IDs
    private int nextQueryNumber = 2;

    // The number of histogram bins to use
    private int numHistogramBins = DEFAULT_NUM_HISTOGRAM_BINS;

    // Current maximum 2D histogram bin count (the max count of all 2D histograms)
    private int maxHistogram2DBinCount = 0;

    // boolean property controls where or not query statistics are calculated
    private BooleanProperty calculateQueryStatistics = new SimpleBooleanProperty(true);

    // boolean property controls whether or not nonquery statistics are calculated
    private BooleanProperty calculateNonQueryStatistics = new SimpleBooleanProperty(false);

    public DataTable() {
        tuples = new ArrayList<>();
        columns = new ArrayList<>();
        disabledColumnTuples = new ArrayList<>();
        disabledColumns = new ArrayList<>();

        activeQuery = new Query("Q1", this);
        listeners = new ArrayList<>();
    }

    public Set<Tuple> getContextTuples() { return contextTuples; }

    public Set<Tuple> getFocusTuples() { return focusTuples; }

    private void setContextTuples() {
        focusTuples.clear();
        contextTuples.clear();

        for (Column column : columns) {
            column.getFocusTuples().clear();
            column.getLowerContextTuples().clear();
            column.getUpperContextTuples().clear();
        }

        for (Tuple tuple : tuples) {
            boolean isContext = false;
            for (int i = 0; i < tuple.getElementCount(); i++) {
                if (!columns.get(i).setFocusContext(tuple, i)) {
                    isContext = true;
                }
            }

            if (isContext) {
                contextTuples.add(tuple);
            } else {
                focusTuples.add(tuple);
            }
        }
    }

//	public DataTable getDuplicate() {
//		DataTable newDataTable = new DataTable();
//		ArrayList<Column> newColumns = new ArrayList<>();
//		for (Column column : columns) {
//			if (column instanceof DoubleColumn) {
//				newColumns.add(new DoubleColumn(column.getName()));
//			} else if (column instanceof TemporalColumn) {
//				newColumns.add(new TemporalColumn(column.getName()));
//			} else if (column instanceof CategoricalColumn) {
//				newColumns.add(new CategoricalColumn(column.getName(), ((CategoricalColumn)column).getCategories()));
//			}
//		}
//
//		ArrayList<Tuple> newTuples = new ArrayList<>();
//		for (Tuple tuple : tuples) {
//			Tuple newTuple = tuple.createCopy();
//			newTuples.add(newTuple);
//		}
//
//		newDataTable.setData(newTuples, newColumns);
//		return newDataTable;
//	}

    public boolean getCalculateQueryStatistics() {
        return calculateQueryStatistics.get();
    }

    public boolean getCalculateNonQueryStatistics() {
        return calculateNonQueryStatistics.get();
    }

    public void setCalculateQueryStatistics(boolean enabled) {
        calculateQueryStatistics.set(enabled);
        getActiveQuery().calculateStatistics();
        fireDataTableStatisticsChanged();
    }

    public void setCalculateNonQueryStatistics(boolean enabled) {
        calculateNonQueryStatistics.set(enabled);
        getActiveQuery().calculateStatistics();
        fireDataTableStatisticsChanged();
    }

    public int getNumHistogramBins() {
        return numHistogramBins;
    }

    public void setNumHistogramBins(int numBins) {
        if (numBins != numHistogramBins) {
            numHistogramBins = numBins;
            for (Column column : columns) {
                if (!(column instanceof ImageColumn)) {
                    column.getStatistics().setNumHistogramBins(numHistogramBins);
                }
            }

            activeQuery.setNumHistogramBins(numHistogramBins);

            fireNumHistogramBinsChanged();
        }
    }

    public ArrayList<DoubleColumn> getEnabledDoubleColumns() {
        ArrayList<DoubleColumn> doubleColumns = new ArrayList<>();
        for (Column column : columns) {
            if (column instanceof DoubleColumn) {
                doubleColumns.add((DoubleColumn)column);
            }
        }

        if (doubleColumns.isEmpty()) {
            return null;
        }

        return doubleColumns;
    }

    public ArrayList<DoubleColumn> getDoubleColumns() {
        ArrayList<DoubleColumn> doubleColumns = new ArrayList<>();
        for (Column column : columns) {
            if (column instanceof DoubleColumn) {
                doubleColumns.add((DoubleColumn)column);
            }
        }

        for (Column column : disabledColumns) {
            if (column instanceof DoubleColumn) {
                doubleColumns.add((DoubleColumn)column);
            }
        }

        if (doubleColumns.isEmpty()) {
            return null;
        }
        return doubleColumns;
    }

    public ArrayList<TemporalColumn> getTemporalColumns() {
        ArrayList<TemporalColumn> temporalColumns = new ArrayList<>();
        for (Column column : columns) {
            if (column instanceof TemporalColumn) {
                temporalColumns.add((TemporalColumn)column);
            }
        }
        for (Column column : disabledColumns) {
            if (column instanceof TemporalColumn) {
                temporalColumns.add((TemporalColumn)column);
            }
        }

        if (temporalColumns.isEmpty()) {
            return null;
        }
        return temporalColumns;
    }

    public ArrayList<CategoricalColumn> getCategoricalColumns() {
        ArrayList<CategoricalColumn> categoricalColumns = new ArrayList<>();
        for (Column column : columns) {
            if (column instanceof CategoricalColumn) {
                categoricalColumns.add((CategoricalColumn)column);
            }
        }
        for (Column column : disabledColumns) {
            if (column instanceof CategoricalColumn) {
                categoricalColumns.add((CategoricalColumn)column);
            }
        }

        if (categoricalColumns.isEmpty()) {
            return null;
        }
        return categoricalColumns;
    }

    public ImageColumn getImageColumn() {
        for (Column column : columns) {
            if (column instanceof ImageColumn) {
                return (ImageColumn)column;
            }
        }

        for (Column column : disabledColumns) {
            if (column instanceof ImageColumn) {
                return (ImageColumn)column;
            }
        }

        return null;
    }

    public final Query getActiveQuery() { return activeQuery; }

//    public int getMaxHistogram2DBinCount() { return maxHistogram2DBinCount; }

    public boolean isEmpty() {
        return tuples.isEmpty();
    }

    public Column getHighlightedColumn() {
        return highlightedColumn;
    }

    public void setHighlightedColumn(Column column) {
        if (column != highlightedColumn) {
            Column oldHighlightedColumn = highlightedColumn;

            if (column == null) {
                highlightedColumn = null;
                fireHighlightedColumnChanged(oldHighlightedColumn);
            } else if (columns.contains(column)) {
                highlightedColumn = column;
                fireHighlightedColumnChanged(oldHighlightedColumn);
            }
        }
    }

    public void setData(ArrayList<Tuple> tuples, ArrayList<Column> columns) {
        clearDataModel();

        if (columns.isEmpty()) {
            return;
        }

        if (tuples != null && !tuples.isEmpty()) {
            this.tuples.addAll(tuples);
            numHistogramBins = (int)Math.floor(Math.sqrt(tuples.size()));
            if (numHistogramBins > MAX_NUM_HISTOGRAM_BINS) {
                numHistogramBins = MAX_NUM_HISTOGRAM_BINS;
            }
        }

        this.columns.addAll(columns);
        for (Column column : this.columns) {
            column.setDataModel(this);
        }

        calculateStatistics();
        getActiveQuery().setQueriedTuples();
        setContextTuples();
        fireDataModelReset();
    }

    public void addTuples(ArrayList<Tuple> newTuples) {
        this.tuples.addAll(newTuples);
        setContextTuples();
        calculateStatistics();

        fireTuplesAdded(newTuples);
    }

    public void clear() {
        clearDataModel();
        fireDataModelReset();
    }

    private void clearDataModel() {
        tuples.clear();
        disabledColumnTuples.clear();
        activeQuery = new Query("Q" + (nextQueryNumber++), this);
//		removeColumnSelectionsFromActiveQuery();
        columns.clear();
        disabledColumns.clear();
        highlightedColumn = null;
    }

    public void setColumnName(Column column, String name) {
        if (columns.contains(column)) {
            column.setName(name);
            fireColumnNameChanged(column);
        } else if (disabledColumns.contains(column)) {
            column.setName(name);
            fireColumnNameChanged(column);
        }
    }

    public ArrayList<Column> getColumns() {
        return columns;
    }

    public ArrayList<Tuple> getTuples() {
        return tuples;
    }

    public Object[] getColumnValues(Column column) {
        int index = getColumnIndex(column);
        return getColumnValues(index);
    }

    public Object[] getColumnValues(int columnIndex) {
        Column column = columns.get(columnIndex);

        Object[] values = new Object[tuples.size()];

        for (int ituple = 0; ituple < tuples.size(); ituple++) {
            Tuple tuple = tuples.get(ituple);
            values[ituple] = tuple.getElement(columnIndex);
        }

        return values;
    }

    public Object[] getColumnQueriedValues(int columnIndex) {
        Column column = columns.get(columnIndex);

        Object[] values = new Object[getActiveQuery().getQueriedTuples().size()];

        int tupleCounter = 0;
        for (Tuple tuple : getActiveQuery().getQueriedTuples()) {
            values[tupleCounter++] = tuple.getElement(columnIndex);
        }

        return values;
    }

    public void addDataTableListener(DataTableListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public boolean removeDataModelListener(DataTableListener listener) {
        return listeners.remove(listener);
    }

    public Tuple getTuple(int idx) {
        return tuples.get(idx);
    }

    public Tuple getDisabledTuple(int idx) {
        return disabledColumnTuples.get(idx);
    }

    public Column getColumn(int idx) {
        return columns.get(idx);
    }

    public Column getColumn(String columnName) {
        for (Column column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public int getColumnIndex(Column column) {
        return columns.indexOf(column);
    }

    public int getTupleCount() {
        return tuples.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public void disableColumn(Column disabledColumn) {
        if (!disabledColumns.contains(disabledColumn)) {
            removeTupleElementsForColumn(disabledColumn);
            disabledColumn.setEnabled(false);

            if (disabledColumn == this.highlightedColumn) {
                highlightedColumn = null;
//                fireHighlightedColumnChanged(disabledColumn);
            }

            disabledColumns.add(disabledColumn);
            columns.remove(disabledColumn);

            calculateStatistics();

            getActiveQuery().removeColumnSelections(disabledColumn);
            getActiveQuery().setQueriedTuples();

            fireColumnDisabled(disabledColumn);
        }
    }

    public void disableColumns(ArrayList<Column> columns) {
        for (Column column : columns) {
            if (!disabledColumns.contains(column)) {
                removeTupleElementsForColumn(column);
                column.setEnabled(false);

                if (column == this.highlightedColumn) {
                    highlightedColumn = null;
//                    fireHighlightedColumnChanged(column);
                }

                disabledColumns.add(column);
                columns.remove(column);
                removeColumnSelectionsFromActiveQuery(column);
            }
        }

        calculateStatistics();

        fireColumnsDisabled(columns);
    }

    private void fireBivariateColumnAdded(BivariateColumn bivariateColumn) {
        int idx = columns.indexOf(bivariateColumn);
        for (DataTableListener listener : listeners) {
            listener.dataTableBivariateColumnAdded(this, bivariateColumn, idx);
        }
    }

    public void addBivariateColumn(Column column1, Column column2, int columnIndex) {
        String title = column2.getName() + " vs. " + column1.getName();
        BivariateColumn biColumn = new BivariateColumn(title, column1, column2);
        biColumn.setDataModel(this);

        int idx = columnIndex < 0 ? 0 : columnIndex > columns.size() ? columns.size() : columnIndex;

        int col1Index = columns.indexOf(column1);
        int col2Index = columns.indexOf(column2);

        // add bivariate pair to tuples
        for (Tuple tuple : tuples) {
            Object biValues[] = {tuple.getElement(col1Index), tuple.getElement(col2Index)};
            tuple.addElement(idx, biValues);
        }

        columns.add(idx, biColumn);

        calculateStatistics();
        getActiveQuery().calculateStatistics();

        fireBivariateColumnAdded(biColumn);
    }

    public void setDoubleColumnScaleExtents(ArrayList<DoubleColumn> columns, double minValue, double maxValue,
                                            boolean setFocusExtents) {
        for (DoubleColumn column : columns) {
            column.setMinimumScaleValue(minValue);
            column.setMaximumScaleValue(maxValue);
            if (setFocusExtents) {
                column.setMinimumFocusValue(minValue);
                column.setMaximumFocusValue(maxValue);
            }
        }
        fireDataTableColumnExtentsChanged();
    }

    public void setDoubleColumnFocusExtents(DoubleColumn column, double minValue, double maxValue) {
        column.setMinimumFocusValue(minValue);
        column.setMaximumFocusValue(maxValue);

        ArrayList<ColumnSelection> selectionRangesToRemove = new ArrayList<>();

        List<ColumnSelection> columnSelections = getActiveQuery().getColumnSelections(column);
        if (columnSelections != null) {
            for (ColumnSelection columnSelection : columnSelections) {
                DoubleColumnSelectionRange doubleColumnSelection = (DoubleColumnSelectionRange)columnSelection;
                if (doubleColumnSelection.getMinValue() > column.getMaximumFocusValue() ||
                        doubleColumnSelection.getMaxValue() < column.getMinimumFocusValue()) {
                    selectionRangesToRemove.add(doubleColumnSelection);
                } else if (doubleColumnSelection.getMaxValue() > column.getMaximumFocusValue()) {
                    doubleColumnSelection.setMaxValue(column.getMaximumFocusValue());
                } else if (doubleColumnSelection.getMinValue() < column.getMinimumFocusValue()) {
                    doubleColumnSelection.setMinValue(column.getMinimumFocusValue());
                }
            }
        }

        if (!selectionRangesToRemove.isEmpty()) {
            getActiveQuery().removeColumnSelections(selectionRangesToRemove);
            getActiveQuery().setQueriedTuples();
            fireColumnSelectionsRemoved(selectionRangesToRemove);
        }

        setContextTuples();
        fireDataTableColumnFocusExtentsChanged();
    }

    public void setTemporalColumnFocusExtents(TemporalColumn column, Instant startInstant, Instant endInstant) {
        column.setStartFocusValue(startInstant);
        column.setEndFocusValue(endInstant);

        ArrayList<ColumnSelection> selectionRangesToRemove = new ArrayList<>();

        List<ColumnSelection> columnSelections = getActiveQuery().getColumnSelections(column);
        if (columnSelections != null) {
            for (ColumnSelection columnSelection : columnSelections) {
                TemporalColumnSelectionRange temporalColumnSelection = (TemporalColumnSelectionRange)columnSelection;
                if (temporalColumnSelection.getStartInstant().isAfter(column.getEndFocusValue()) ||
                        temporalColumnSelection.getEndInstant().isBefore(column.getStartFocusValue())) {
                    selectionRangesToRemove.add(temporalColumnSelection);
                } else if (temporalColumnSelection.getEndInstant().isAfter(column.getEndFocusValue())) {
                    temporalColumnSelection.setEndInstant(column.getEndFocusValue());
                } else if (temporalColumnSelection.getStartInstant().isBefore(column.getStartFocusValue())) {
                    temporalColumnSelection.setStartInstant(column.getStartFocusValue());
                }
            }
        }

        if (!selectionRangesToRemove.isEmpty()) {
            getActiveQuery().removeColumnSelections(selectionRangesToRemove);
            getActiveQuery().setQueriedTuples();
            fireColumnSelectionsRemoved(selectionRangesToRemove);
        }

        setContextTuples();
        fireDataTableColumnFocusExtentsChanged();
    }

    public void setTemporalColumnScaleExtents(ArrayList<TemporalColumn> columns, Instant startInstant, Instant endInstant) {
        for (TemporalColumn column : columns) {
            column.setStartScaleValue(startInstant);
            column.setEndScaleValue(endInstant);
        }
        fireDataTableColumnExtentsChanged();
    }

    public void enableColumn(Column column) {
        if (disabledColumns.contains(column)) {
            // move elements from disable column tuples to active tuples list
            addTupleElementsForDisabledColumn(column);
            disabledColumns.remove(column);
            column.setEnabled(true);
            columns.add(column);
            calculateStatistics();
            getActiveQuery().calculateStatistics();
            // fireDataModelChanged();
            fireColumnEnabled(column);
        }
    }

    public int getDisabledColumnCount() {
        return disabledColumns.size();
    }

    public ArrayList<Column> getDisabledColumns() {
        return disabledColumns;
    }

    public void removeColumnSelectionFromActiveQuery(ColumnSelection selectionRange) {
        getActiveQuery().removeColumnSelection(selectionRange);
        getActiveQuery().setQueriedTuples();
        fireColumnSelectionRemoved(selectionRange);
    }

    public int removeUnselectedTuples() {
        int tuplesRemoved = 0;

        if (getActiveQuery().hasColumnSelections()) {
            tuplesRemoved = getActiveQuery().getNonQueriedTuples().size();
            removeTuples(getActiveQuery().getNonQueriedTuples(), getActiveQuery().getQueriedTuples());
//			tuplesRemoved = getActiveQuery().getNonQueriedTuples().size();
//			tuples.clear();
//			tuples.addAll(getActiveQuery().getQueriedTuples());
//			getActiveQuery().clear();
//			calculateStatistics();
//			for (Column column : columns) {
//				if (column instanceof DoubleColumn) {
//					((DoubleColumn)column).setMinimumScaleValue(((DoubleColumn)column).getStatistics().getMinValue());
//					((DoubleColumn)column).setMaximumScaleValue(((DoubleColumn)column).getStatistics().getMaxValue());
//				} else if (column instanceof TemporalColumn) {
//					((TemporalColumn)column).setStartScaleValue(((TemporalColumn)column).getStatistics().getStartInstant());
//					((TemporalColumn)column).setEndScaleValue(((TemporalColumn)column).getStatistics().getEndInstant());
//				}
//			}
//			getActiveQuery().setQueriedTuples();
            fireTuplesRemoved(tuplesRemoved);
        }

        return tuplesRemoved;
    }

    private void removeTuples(Set<Tuple> tuplesToRemove, Set<Tuple> tuplesToKeep) {
        tuples.clear();
        tuples.addAll(tuplesToKeep);
        getActiveQuery().clear();
        for (Column column : columns) {
            if (column instanceof CategoricalColumn) {
                ((CategoricalColumn)column).getCategories().clear();
                int columnIndex = columns.indexOf(column);
                for (Tuple tuple : tuples) {
                    ((CategoricalColumn)column).addCategory((String)tuple.getElement(columnIndex));
                }
            }
        }
        calculateStatistics();
        for (Column column : columns) {
            if (column instanceof DoubleColumn) {
                ((DoubleColumn)column).setMinimumScaleValue(((DoubleColumn)column).getStatistics().getMinValue());
                ((DoubleColumn)column).setMaximumScaleValue(((DoubleColumn)column).getStatistics().getMaxValue());
            } else if (column instanceof TemporalColumn) {
                ((TemporalColumn)column).setStartScaleValue(((TemporalColumn)column).getStatistics().getStartInstant());
                ((TemporalColumn)column).setEndScaleValue(((TemporalColumn)column).getStatistics().getEndInstant());
            }
        }
        getActiveQuery().setQueriedTuples();
    }

    public int removeSelectedTuples() {
        int tuplesRemoved = 0;

        if (getActiveQuery().hasColumnSelections()) {
            tuplesRemoved = getActiveQuery().getQueriedTuples().size();
            removeTuples(getActiveQuery().getQueriedTuples(), getActiveQuery().getNonQueriedTuples());
//			tuples.clear();
//			tuples.addAll(getActiveQuery().getNonQueriedTuples());
//			getActiveQuery().clear();
//			for (Column column : columns) {
//				if (column instanceof CategoricalColumn) {
//					((CategoricalColumn)column).getCategories().clear();
//					int columnIndex = columns.indexOf(column);
//					for (Tuple tuple : tuples) {
//						((CategoricalColumn)column).addCategory((String)tuple.getElement(columnIndex));
//					}
//				}
//			}
//			calculateStatistics();
//			for (Column column : columns) {
//				if (column instanceof DoubleColumn) {
//					((DoubleColumn)column).setMinimumScaleValue(((DoubleColumn)column).getStatistics().getMinValue());
//					((DoubleColumn)column).setMaximumScaleValue(((DoubleColumn)column).getStatistics().getMaxValue());
//				} else if (column instanceof TemporalColumn) {
//					((TemporalColumn)column).setStartScaleValue(((TemporalColumn)column).getStatistics().getStartInstant());
//					((TemporalColumn)column).setEndScaleValue(((TemporalColumn)column).getStatistics().getEndInstant());
//				}
//			}
//			getActiveQuery().setQueriedTuples();
            fireTuplesRemoved(tuplesRemoved);
        }

        return tuplesRemoved;
    }

//	public void saveActiveQuery() {
//		savedQueryList.add(activeQuery);
//		activeQuery = new Query("Q"+(nextQueryNumber++));
//	}

//	public Query getActiveQuery() {
//		return activeQuery;
//	}

//	public void setActiveQuery(String queryID) {
//		for (Query query : savedQueryList) {
//			if (query.getID().equals(queryID)) {
//				activeQuery = query;
//				savedQueryList.remove(query);
//				return;
//			}
//		}
//	}

    public void removeColumnSelectionsFromActiveQuery() {
        activeQuery = new Query("Q" + (nextQueryNumber++), this);
        fireQueryCleared();
    }

//	private void clearAllQueryColumnSelections() {
//		getActiveQuery().clear();
//	}

    public void removeColumnSelectionsFromActiveQuery(Column column) {
        if (activeQuery != null) {
            getActiveQuery().removeColumnSelections(column);
            getActiveQuery().setQueriedTuples();
            fireQueryColumnCleared(column);
        }
    }

//	public ArrayList<Query> getSavedQueryList() {
//		return savedQueryList;
//	}

//	public Query getQueryByID(String ID) {
//		if (activeQuery.getID().equals(ID)) {
//			return activeQuery;
//		} else {
//			for (Query query : savedQueryList) {
//				if (query.getID().equals(ID)) {
//					return query;
//				}
//			}
//		}
//
//		return null;
//	}

    public void addColumnSelectionToActiveQuery(ColumnSelection newColumnSelectionRange) {
        getActiveQuery().addColumnSelection(newColumnSelectionRange);
        getActiveQuery().setQueriedTuples();
        fireColumnSelectionAdded(newColumnSelectionRange);

        if (newColumnSelectionRange instanceof DoubleColumnSelectionRange) {
            ((DoubleColumnSelectionRange)newColumnSelectionRange).rangeValuesProperty().addListener((observable, oldValue, newValue) -> {
                getActiveQuery().setQueriedTuples();
                fireColumnSelectionChanged(newColumnSelectionRange);
            });
        } else if (newColumnSelectionRange instanceof TemporalColumnSelectionRange) {
            ((TemporalColumnSelectionRange)newColumnSelectionRange).rangeInstantsProperty().addListener((observable, oldValue, newValue) -> {
                getActiveQuery().setQueriedTuples();
                fireColumnSelectionChanged(newColumnSelectionRange);
            });
        } else if (newColumnSelectionRange instanceof CategoricalColumnSelection) {
            ((CategoricalColumnSelection)newColumnSelectionRange).selectedCategoriesProperty().addListener((SetChangeListener<String>) change -> {
                if (((CategoricalColumnSelection) newColumnSelectionRange).getSelectedCategories().isEmpty()) {
                    getActiveQuery().removeColumnSelection(newColumnSelectionRange);
                }
                getActiveQuery().setQueriedTuples();
                fireColumnSelectionChanged(newColumnSelectionRange);
            });
//            ((CategoricalColumnSelection)newColumnSelectionRange).selectedCategoriesProperty().addListener((observable, oldValue, newValue) -> {
//                getActiveQuery().setQueriedTuples();
//                fireColumnSelectionChanged(newColumnSelectionRange);
//            });
        } else if (newColumnSelectionRange instanceof ImageColumnSelection) {
            ((ImageColumnSelection)newColumnSelectionRange).selectedImagePairSetProperty().addListener((SetChangeListener<Pair<File, Image>>) change -> {
                if (((ImageColumnSelection)newColumnSelectionRange).getSelectedImagePairs().isEmpty()) {
                    getActiveQuery().removeColumnSelection(newColumnSelectionRange);
                }
                getActiveQuery().setQueriedTuples();
                fireColumnSelectionChanged(newColumnSelectionRange);
            });
        }
    }

//    public void orderColumnsByCorrelation (DoubleColumn compareColumn, boolean useQueryCorrelations) {
//        int compareColumnIndex = getColumnIndex(compareColumn);
//
//        ArrayList<DoubleColumn> newColumnList = new ArrayList<DoubleColumn>();
//        ArrayList<ColumnSortRecord> positiveColumnList = new ArrayList<ColumnSortRecord>();
//        ArrayList<ColumnSortRecord> negativeColumnList = new ArrayList<ColumnSortRecord>();
//        ArrayList<ColumnSortRecord> nanColumnList = new ArrayList<ColumnSortRecord>();
//
//        for (int i = 0; i < columns.size(); i++) {
//            DoubleColumn column = columns.get(i);
//            if (column == compareColumn) {
//                continue;
//            }
//
//			double corrCoef;
//            if (useQueryCorrelations) {
//                corrCoef = getActiveQuery().getColumnQuerySummaryStats(column).getCorrelationCoefficients().get(compareColumnIndex);
//            } else {
//                corrCoef = column.getSummaryStats().getCorrelationCoefficients().get(compareColumnIndex);
//            }
//
//            ColumnSortRecord columnSortRecord = new ColumnSortRecord(column, corrCoef);
//            if (Double.isNaN(corrCoef)) {
//                nanColumnList.add(columnSortRecord);
//            } else if (corrCoef < 0.) {
//                negativeColumnList.add(columnSortRecord);
//            } else {
//                positiveColumnList.add(columnSortRecord);
//            }
//        }
//
//        // add negatively correlated crossvis
//        if (!negativeColumnList.isEmpty()) {
//            Object sortedRecords[] = negativeColumnList.toArray();
//            Arrays.sort(sortedRecords);
//
//            for (Object recordObject : sortedRecords) {
//                ColumnSortRecord sortRecord = (ColumnSortRecord)recordObject;
//                newColumnList.add(sortRecord.column);
//            }
//        }
//
//        // compare axis goes between negative and positive correlated crossvis
//        newColumnList.add(compareColumn);
//
//        // add positively correlated crossvis
//        if (!positiveColumnList.isEmpty()) {
//            Object sortedRecords[] = positiveColumnList.toArray();
//            Arrays.sort(sortedRecords);
//
//            for (Object recordObject : sortedRecords) {
//                ColumnSortRecord sortRecord = (ColumnSortRecord)recordObject;
//                newColumnList.add(sortRecord.column);
//            }
//        }
//
//        // add nan crossvis at bottom of the list
//        if (!nanColumnList.isEmpty()) {
//            for (ColumnSortRecord sortRecord : nanColumnList) {
//                newColumnList.add(sortRecord.column);
//            }
//        }
//
//        changeColumnOrder(newColumnList);
//    }

    public void changeColumnOrder(Column column, int newColumnIndex) {
        ArrayList<Column> newColumnOrder = new ArrayList<>(columns);
        int currentColumnIndex = newColumnOrder.indexOf(column);

        if (currentColumnIndex == newColumnIndex) {
            return;
        }

        newColumnOrder.remove(currentColumnIndex);

        if (currentColumnIndex < newColumnIndex) {
            newColumnIndex--;
        }

        newColumnOrder.add(newColumnIndex, column);

//		if (newColumnIndex <= 0) {
//			newColumnOrder.add(0, column);
//		} else if (newColumnIndex > newColumnOrder.size()) {
//			newColumnOrder.add(newColumnOrder.size(), column);
//		} else if (currentColumnIndex > newColumnIndex) {
//			newColumnOrder.add(newColumnIndex + 1, column);
//		} else {
//			newColumnOrder.add(newColumnIndex, column);
//		}

        changeColumnOrder(newColumnOrder);
    }

    public void changeColumnOrder(ArrayList<Column> newColumnOrder) {
        // determine destination indices for new column order
        int dstColumnIndices[] = new int[newColumnOrder.size()];
        for (int i = 0; i < newColumnOrder.size(); i++) {
            // find index of column in new column order
            Column column = newColumnOrder.get(i);
            dstColumnIndices[i] = columns.indexOf(column);
        }

        // reset columns array
        columns = newColumnOrder;

        // rearrange column correlation coefficients
        for (int iColumn = 0; iColumn < columns.size(); iColumn++) {
            Column column = columns.get(iColumn);
            if (column instanceof DoubleColumn) {
                List<Double> currentCoefList = ((DoubleColumnSummaryStats)column.getStatistics()).getCorrelationCoefficientList();
                ArrayList<Double> newCoefList = new ArrayList<>();
                for (int i = 0; i < currentCoefList.size(); i++) {
                    newCoefList.add(currentCoefList.get(dstColumnIndices[i]));
                }
                ((DoubleColumnSummaryStats)column.getStatistics()).setCorrelationCoefficientList(newCoefList);
            }
        }

        // move tuple elements to reflect new column order
        for (int iTuple = 0; iTuple < tuples.size(); iTuple++) {
            Tuple tuple = tuples.get(iTuple);
            Object elements[] = tuple.getElementsAsArray();
            tuple.removeAllElements();

            for (int iElement = 0; iElement < elements.length; iElement++) {
                tuple.addElement(elements[dstColumnIndices[iElement]]);
            }
        }

        // move query statistics to reflect new column order
		if (getActiveQuery().hasColumnSelections()) {
            for (int iColumn = 0; iColumn < columns.size(); iColumn++) {
                Column column = columns.get(iColumn);
                if (column instanceof DoubleColumn) {
                    DoubleColumnSummaryStats summaryStats = (DoubleColumnSummaryStats)getActiveQuery().getColumnQuerySummaryStats(column);
                    List<Double> corrCoef = summaryStats.getCorrelationCoefficientList();

//                            ArrayList < Double > corrCoef = summaryStats.getCorrelationCoefficients();
                    ArrayList<Double> newCorrCoef = new ArrayList<Double>();
                    for (int iCorrCoef = 0; iCorrCoef < corrCoef.size(); iCorrCoef++) {
                        newCorrCoef.add(corrCoef.get(dstColumnIndices[iCorrCoef]));
                    }
                    summaryStats.setCorrelationCoefficientList(newCorrCoef);
//                    summaryStats.setCorrelationCoefficients(newCorrCoef);
                }
//				ArrayList<Histogram2DOLD> histogram2DList = summaryStats.getHistogram2DList();
//				ArrayList<Histogram2DOLD> newHistogram2DList = new ArrayList<>();
//				for (int i = 0; i < histogram2DList.size(); i++) {
//					newHistogram2DList.add(histogram2DList.get(dstColumnIndices[i]));
//				}
//				summaryStats.setHistogram2DList(newHistogram2DList);
            }
        }

        fireColumnOrderChanged();
    }

    // get index of column and remove all tuple elements at this index
    // add the tuple elements to a list of disabledColumnTuples for later enabling
    private void removeTupleElementsForColumn(Column column) {
        int columnIndex = columns.indexOf(column);

        for (int iTuple = 0; iTuple < tuples.size(); iTuple++) {
            Tuple tuple = tuples.get(iTuple);
            Object elementValue = tuple.getElement(columnIndex);
            tuple.removeElement(columnIndex);

            if (disabledColumnTuples.size() != tuples.size()) {
                Tuple disabledTuple = new Tuple(this);
                disabledTuple.addElement(elementValue);
                disabledColumnTuples.add(disabledTuple);
            } else {
                Tuple disabledTuple = disabledColumnTuples.get(iTuple);
                disabledTuple.addElement(elementValue);
            }
        }
    }

    private void addTupleElementsForDisabledColumn(Column column) {
        int columnIndex = disabledColumns.indexOf(column);
        if (columnIndex != -1) {
            for (int iTuple = 0; iTuple < disabledColumnTuples.size(); iTuple++) {
                Tuple disabledTuple = disabledColumnTuples.get(iTuple);
                Object elementValue = disabledTuple.getElement(columnIndex);
                disabledTuple.removeElement(columnIndex);

                if (disabledColumnTuples.size() != tuples.size()) {
                    Tuple tuple = new Tuple(this);
                    tuple.addElement(elementValue);
                    tuples.add(tuple);
                } else {
                    Tuple tuple = tuples.get(iTuple);
                    tuple.addElement(elementValue);
                }
            }
        }
    }

    private void calculateStatistics() {
        long start = System.currentTimeMillis();
        for (Column column : columns) {
            column.calculateStatistics();
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Calling column.calculateStatistics for all columns took " + elapsed + " ms");
    }

    public void fireNumHistogramBinsChanged() {
        for (DataTableListener listener : listeners) {
            listener.dataTableNumHistogramBinsChanged(this);
        }
    }

    private void fireColumnDisabled(Column column) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnDisabled(this, column);
        }
    }

    private void fireColumnsDisabled(ArrayList<Column> disabledColumns) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnsDisabled(this, disabledColumns);
        }
    }

    private void fireColumnEnabled(Column column) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnEnabled(this, column);
        }
    }

    private void fireDataTableColumnFocusExtentsChanged() {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnFocusExtentsChanged(this);
        }
    }

    private void fireDataTableColumnExtentsChanged() {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnExtentsChanged(this);
        }
    }

    private void fireDataTableStatisticsChanged() {
        for (DataTableListener listener : listeners) {
            listener.dataTableStatisticsChanged(this);
        }
    }

    private void fireColumnOrderChanged() {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnOrderChanged(this);
        }
    }

    private void fireColumnNameChanged(Column column) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnNameChanged(this, column);
        }
    }

    private void fireDataModelReset() {
        for (DataTableListener listener : listeners) {
            listener.dataTableReset(this);
        }
    }

    private void fireTuplesAdded(ArrayList<Tuple> newTuples) {
        for (DataTableListener listener : listeners) {
            listener.dataTableTuplesAdded(this, newTuples);
        }
    }

    private void fireTuplesRemoved(int numTuplesRemoved) {
        for (DataTableListener listener : listeners) {
            listener.dataTableTuplesRemoved(this, numTuplesRemoved);
        }
    }

    public void fireHighlightedColumnChanged(Column oldHighlightedColumn) {
        for (DataTableListener listener : listeners) {
            listener.dataTableHighlightedColumnChanged(this, oldHighlightedColumn, highlightedColumn);
        }
    }

    public void fireColumnSelectionAdded(ColumnSelection columnSelectionRange) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnSelectionAdded(this, columnSelectionRange);
        }
    }

    public void fireColumnSelectionRemoved(ColumnSelection columnSelectionRange) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnSelectionRemoved(this, columnSelectionRange);
        }
    }

    public void fireColumnSelectionsRemoved(List<ColumnSelection> columnSelections) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnSelectionsRemoved(this, columnSelections);
        }
    }

    public void fireColumnSelectionChanged(ColumnSelection columnSelectionRange) {
        for (DataTableListener listener : listeners) {
            listener.dataTableColumnSelectionChanged(this, columnSelectionRange);
        }
    }

    public void fireQueryCleared() {
        for (DataTableListener listener : listeners) {
            listener.dataTableAllColumnSelectionsRemoved(this);
        }
    }

    public void fireQueryColumnCleared(Column column) {
        for (DataTableListener listener : listeners) {
            listener.dataTableAllColumnSelectionsForColumnRemoved(this, column);
        }
    }
}