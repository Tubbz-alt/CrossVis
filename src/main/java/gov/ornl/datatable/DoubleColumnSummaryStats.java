package gov.ornl.datatable;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by csg on 11/25/14.
 */
public class DoubleColumnSummaryStats extends ColumnSummaryStats {
    private final static Logger log = Logger.getLogger(DoubleColumnSummaryStats.class.getName());

    private double[] values;

    private DoubleProperty meanValue;
    private DoubleProperty minValue;
    private DoubleProperty maxValue;
    private DoubleProperty standardDeviationValue;
    private DoubleProperty varianceValue;
    private DoubleProperty percentile25Value;
    private DoubleProperty percentile75Value;
    private DoubleProperty medianValue;
    private DoubleProperty skewnessValue;
    private DoubleProperty kurtosisValue;
    private DoubleProperty upperWhiskerValue;
    private DoubleProperty lowerWhiskerValue;

    private ListProperty<Double> correlationCoefficientList;

    private ObjectProperty<DoubleHistogram> histogram;

    public DoubleColumnSummaryStats(Column column, int numHistogramBins, Query query) {
        super(column, numHistogramBins, query);

        values = null;
        minValue = new SimpleDoubleProperty(Double.NaN);
        maxValue = new SimpleDoubleProperty(Double.NaN);
        meanValue = new SimpleDoubleProperty(Double.NaN);
        medianValue = new SimpleDoubleProperty(Double.NaN);
        standardDeviationValue = new SimpleDoubleProperty(Double.NaN);
        varianceValue = new SimpleDoubleProperty(Double.NaN);
        percentile25Value = new SimpleDoubleProperty(Double.NaN);
        percentile75Value = new SimpleDoubleProperty(Double.NaN);
        skewnessValue = new SimpleDoubleProperty(Double.NaN);
        kurtosisValue = new SimpleDoubleProperty(Double.NaN);
        upperWhiskerValue = new SimpleDoubleProperty(Double.NaN);
        lowerWhiskerValue = new SimpleDoubleProperty(Double.NaN);

        correlationCoefficientList = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public void setValues(double[] values, int numHistogramBins) {
        this.values = values;

        this.numHistogramBins = numHistogramBins;

        calculateStatistics();
    }

    public void setValues(double[] values) {
        this.values = values;

//        numHistogramBins = (int)Math.floor(Math.sqrt(values.length));
//        if (numHistogramBins > MAX_NUM_HISTOGRAM_BINS) {
//            numHistogramBins = MAX_NUM_HISTOGRAM_BINS;
//        }

        calculateStatistics();
    }

    public double[] getValues() {
        return values;
    }

    @Override
    public void calculateStatistics() {
        DescriptiveStatistics stats = new DescriptiveStatistics(values);

        setMinValue(stats.getMin());
        setMaxValue(stats.getMax());
        setMeanValue(stats.getMean());
        setMedianValue(stats.getPercentile(50));
        setVarianceValue(stats.getVariance());
        setStandardDeviationValue(stats.getStandardDeviation());
        setPercentile25Value(stats.getPercentile(25));
        setPercentile75Value(stats.getPercentile(75));
        setSkewnessValue(stats.getSkewness());
        setKurtosisValue(stats.getKurtosis());

        // calculate whiskers for box plot 1.5 of IQR
        double iqr_range = 1.5 * getIQR();
        double lowerFence = getPercentile25Value() - iqr_range;
        double upperFence = getPercentile75Value() + iqr_range;
        double sorted_data[] = stats.getSortedValues();

        // find upper datum that is not greater than upper fence
        if (upperFence >= getMaxValue()) {
            setUpperWhiskerValue(getMaxValue());
        } else {
            // find largest datum not larger than upper fence value
            for (int i = sorted_data.length - 1; i >= 0; i--) {
                if (sorted_data[i] <= upperFence) {
                    setUpperWhiskerValue(sorted_data[i]);
                    break;
                }
            }
        }

        if (lowerFence <= getMinValue()) {
            setLowerWhiskerValue(getMinValue());
        } else {
            // find smallest datum not less than lower fence value
            for (int i = 0; i < sorted_data.length; i++) {
                if (sorted_data[i] >= lowerFence) {
                    setLowerWhiskerValue(sorted_data[i]);
                    break;
                }
            }
        }

        calculateHistogram();
        calculateCorrelations();
    }

    private void calculateCorrelations() {
        correlationCoefficientList.clear();
        PearsonsCorrelation pCorr = new PearsonsCorrelation();

        for (int icol = 0; icol < getColumn().getDataTable().getColumns().size(); icol++) {
            Column otherColumn = getColumn().getDataTable().getColumn(icol);
            double coefficient = Double.NaN;
            if ((otherColumn == this.column) &&  (values.length > 1)) {
                // no need to computer correlation with itself
                coefficient = 1d;
            } else if ((otherColumn instanceof DoubleColumn) && (values.length > 1)) {
                if (query == null) {
                    coefficient = pCorr.correlation(values, ((DoubleColumn) otherColumn).getValues());
                } else {
                    if (((DoubleColumn)otherColumn).getQueriedValues().length == values.length) {
                        coefficient = pCorr.correlation(values, ((DoubleColumn) otherColumn).getQueriedValues());
                    } else if (((DoubleColumn)otherColumn).getNonqueriedValues().length == values.length) {
                        coefficient = pCorr.correlation(values, ((DoubleColumn) otherColumn).getNonqueriedValues());
                    } else {
                        coefficient = Double.NaN;
                    }
                }
            }
            correlationCoefficientList.add(coefficient);
        }
    }

    private DoubleColumn doubleColumn() {
        return (DoubleColumn)getColumn();
    }

    @Override
    public void calculateHistogram() {
        setHistogram(new DoubleHistogram(column.getName(), values, numHistogramBins,
                doubleColumn().getStatistics().getMinValue(),
                doubleColumn().getStatistics().getMaxValue()));
    }

    public DoubleHistogram getHistogram() {
        return histogramProperty().get();
    }

    public void setHistogram(DoubleHistogram histogram) {
        histogramProperty().set(histogram);
    }

    public ObjectProperty<DoubleHistogram> histogramProperty() {
        if (histogram == null) {
            histogram = new SimpleObjectProperty<>(this, "histogram");
        }
        return histogram;
    }

    public double getIQR() {
        return getPercentile75Value() - getPercentile25Value();
    }

    public void setMinValue(double value) {
        minValueProperty().set(value);
    }

    public double getMinValue() {
        return minValueProperty().get();
    }

    public ListProperty<Double> correlationCoefficientListProperty() {
        return correlationCoefficientList;
    }

    public List<Double> getCorrelationCoefficientList() {
        return correlationCoefficientList.get();
    }

    public void setCorrelationCoefficientList(List<Double> newCorrelationCoefficientList) {
        correlationCoefficientListProperty().clear();
        for (double corrCoef : newCorrelationCoefficientList) {
            correlationCoefficientListProperty().add(corrCoef);
        }
    }

    public DoubleProperty minValueProperty() {
        return minValue;
    }

    public void setMaxValue(double value) {
        maxValueProperty().set(value);
    }

    public double getMaxValue() {
        return maxValueProperty().get();
    }

    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public void setMedianValue(double value) {
        medianValueProperty().set(value);
    }

    public double getMedianValue() {
        return medianValueProperty().get();
    }

    public DoubleProperty medianValueProperty() {
        return medianValue;
    }

    public void setMeanValue(double value) {
        meanValueProperty().set(value);
    }

    public double getMeanValue() {
        return meanValueProperty().get();
    }

    public DoubleProperty meanValueProperty() {
        return meanValue;
    }
    
    public void setStandardDeviationValue(double value) {
        standardDeviationValueProperty().set(value);
    }

    public double getStandardDeviationValue() {
        return standardDeviationValueProperty().get();
    }

    public DoubleProperty standardDeviationValueProperty() {
        return standardDeviationValue;
    }

    public void setVarianceValue(double value) {
        varianceValueProperty().set(value);
    }

    public double getVarianceValue() {
        return varianceValueProperty().get();
    }

    public DoubleProperty varianceValueProperty() {
        return varianceValue;
    }

    public void setPercentile25Value(double value) {
        percentile25ValueProperty().set(value);
    }

    public double getPercentile25Value() {
        return percentile25ValueProperty().get();
    }

    public DoubleProperty percentile25ValueProperty() {
        return percentile25Value;
    }

    public void setPercentile75Value(double value) {
        percentile75ValueProperty().set(value);
    }

    public double getPercentile75Value() {
        return percentile75ValueProperty().get();
    }

    public DoubleProperty percentile75ValueProperty() {
        return percentile75Value;
    }

    public void setUpperWhiskerValue(double value) {
        upperWhiskerValueProperty().set(value);
    }

    public double getUpperWhiskerValue() {
        return upperWhiskerValueProperty().get();
    }

    public DoubleProperty upperWhiskerValueProperty() {
        return upperWhiskerValue;
    }

    public void setLowerWhiskerValue(double value) {
        lowerWhiskerValueProperty().set(value);
    }

    public double getLowerWhiskerValue() {
        return lowerWhiskerValueProperty().get();
    }

    public DoubleProperty lowerWhiskerValueProperty() {
        return lowerWhiskerValue;
    }

    public void setKurtosisValue(double value) {
        kurtosisValueProperty().set(value);
    }

    public double getKurtosisValue() {
        return kurtosisValueProperty().get();
    }

    public DoubleProperty kurtosisValueProperty() {
        return kurtosisValue;
    }

    public void setSkewnessValue(double value) {
        skewnessValueProperty().set(value);
    }

    public double getSkewnessValue() {
        return skewnessValueProperty().get();
    }

    public DoubleProperty skewnessValueProperty() {
        return skewnessValue;
    }
}
