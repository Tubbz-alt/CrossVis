package gov.ornl.datatable;

import javafx.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TemporalHistogram extends Histogram {
    public Instant values[];
    public Tuple tuples[] = null;
    private int numBins;
    private Duration binDuration;

//    private int binCounts[];
    private Bin bins[];
    private int maxBinCount;

    private Instant startInstant;
    private Instant endInstant;
    private Duration histogramDuration;

    public TemporalHistogram(String name, Instant values[], Tuple tuples[], int numBins, Instant startInstant,
                             Instant endInstant){
        super(name);
        this.values = values;
        this.tuples = tuples;
        this.numBins = numBins;

        this.startInstant = Instant.from(startInstant);
        this.endInstant = Instant.from(endInstant);

        calculate();
    }

    public TemporalHistogram(String name, Instant values[], int numBins) {
        super(name);

        this.values = values;
        this.numBins = numBins;

        calculate();
    }

    public TemporalHistogram(String name, Instant values[], int numBins, Instant startInstant, Instant endInstant) {
        super(name);

        this.values = values;
        this.numBins = numBins;

        this.startInstant = Instant.from(startInstant);
        this.endInstant = Instant.from(endInstant);

        calculate();
    }

    public int getBinCount(int i) {
        return bins[i].binValues.size();
    }

    public List<Instant> getBinValues(int i) {
        return bins[i].binValues;
    }

    public List<Tuple> getBinTuples(int i) {
        return bins[i].binTuples;
    }

    public Instant getBinLowerBound(int i) {
        return startInstant.plus(binDuration.multipliedBy(i));
    }

    public Instant getBinUpperBound(int i) {
        return startInstant.plus(binDuration.multipliedBy(i + 1));
    }

    public Instant getStartInstant() {
        return startInstant;
    }

    public Instant getEndInstant() {
        return endInstant;
    }

    public int getNumBins() {
        return numBins;
    }

    public void setNumBins(int numBins) {
        this.numBins = numBins;
        calculate();
    }

    public void setValues (Instant values[]) {
        this.values = values;
        this.tuples = null;
        calculate();
    }

    public void setValues (Instant values[], Tuple tuples[]) {
        this.values = values;
        this.tuples = tuples;
        calculate();
    }

    public void setStartInstant(Instant startInstant) {
        this.startInstant = startInstant;
        calculate();
    }

    public void setEndInstant(Instant endInstant) {
        this.endInstant = endInstant;
        calculate();
    }

    public int getMaxBinCount() {
        return maxBinCount;
    }

    private void calculateRange() {
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                startInstant = values[i];
                endInstant = values[i];
            } else {
                if (values[i].isBefore(startInstant)) {
                    startInstant = values[i];
                } else if (values[i].isAfter(endInstant)) {
                    endInstant = values[i];
                }
            }
        }
    }

    private Pair<ChronoUnit, Long> findBestTemporalUnit(Duration duration, int numBins) {
        if (duration.toMillis() < numBins) {
            return new Pair<ChronoUnit, Long>(ChronoUnit.MILLIS, duration.toMillis());
        } else if ((duration.toMillis() * 1000) < numBins) {
            return new Pair<ChronoUnit, Long>(ChronoUnit.SECONDS, duration.toMillis() * 1000);
        } else if ((duration.toMillis() * 6000) < numBins) {
            return new Pair<ChronoUnit, Long>(ChronoUnit.MINUTES, duration.toMillis() * 60000);
        } else if (duration.toHours() < numBins) {
            return new Pair<ChronoUnit, Long>(ChronoUnit.HOURS, duration.toHours());
        } else {
            return new Pair<ChronoUnit, Long>(ChronoUnit.DAYS, duration.toDays());
        }

//        while (!foundBestTemporalUnit) {
//            long numUnits = duration.get(bestTemporalUnit);
//            if (numUnits < numBins) {
//                foundBestTemporalUnit = true;
//            }
//
//            if (bestTemporalUnit == ChronoUnit.MILLIS) {
//                bestTemporalUnit = ChronoUnit.SECONDS;
//            } else if (bestTemporalUnit == ChronoUnit.SECONDS) {
//                bestTemporalUnit = ChronoUnit.MINUTES;
//            } else if (bestTemporalUnit == ChronoUnit.MINUTES) {
//                bestTemporalUnit = ChronoUnit.HOURS;
//            } else if (bestTemporalUnit == ChronoUnit.HOURS) {
//                bestTemporalUnit = ChronoUnit.DAYS;
//            } else if (bestTemporalUnit == ChronoUnit.DAYS) {
//                bestTemporalUnit = ChronoUnit.WEEKS;
//            } else if (bestTemporalUnit == ChronoUnit.YEARS) {
//                bestTemporalUnit = ChronoUnit.DECADES;
//            } else if (bestTemporalUnit == ChronoUnit.DECADES) {
//                bestTemporalUnit = ChronoUnit.CENTURIES;
//                foundBestTemporalUnit = true;
//            }
//        }

//        return bestTemporalUnit;
    }
    
    public void calculate() {
        if (startInstant == null || endInstant == null) {
            calculateRange();
        }

        histogramDuration = Duration.between(startInstant, endInstant);

        // find best bin size for temporal time units
//        Pair<ChronoUnit, Long> bestTemporalUnit = findBestTemporalUnit(histogramDuration, numBins);
//        long bestNumBins = histogramDuration.get(bestTemporalUnit);

        binDuration = histogramDuration.dividedBy(numBins);


//        binCounts = new int[numBins];
//        Arrays.fill(binCounts, 0);
        bins = new Bin[numBins];
        for (int i = 0; i < bins.length; i++) {
            bins[i] = new Bin();
        }
        maxBinCount = 0;

        if (values != null) {
            for (int ivalue = 0; ivalue < values.length; ivalue++) {
                Instant value = values[ivalue];

                Duration valueOffsetDuration = Duration.between(startInstant, value);
                int binIndex = (int) (valueOffsetDuration.toMillis() / binDuration.toMillis());

                if (binIndex < 0) {
                    // the value is smaller than the minValue
                } else if (binIndex >= numBins) {
                    // if the value is equal to the max value increment the last bin
                    if (value.equals(endInstant)) {
                        bins[numBins - 1].binValues.add(value);
                        if (tuples != null) {
                            bins[numBins - 1].binTuples.add(tuples[ivalue]);
                        }

                        if (bins[numBins-1].binValues.size() > maxBinCount) {
                            maxBinCount = bins[numBins-1].binValues.size();
                        }

//                        binCounts[numBins - 1]++;
//                        if (binCounts[numBins - 1] > maxBinCount) {
//                            maxBinCount = binCounts[numBins - 1];
//                        }
                    }
                } else {
                    bins[binIndex].binValues.add(value);
                    if (tuples != null) {
                        bins[binIndex].binTuples.add(tuples[ivalue]);
                    }
                    if (bins[binIndex].binValues.size() > maxBinCount) {
                        maxBinCount = bins[binIndex].binValues.size();
                    }
//                    binCounts[binIndex]++;
//                    if (binCounts[binIndex] > maxBinCount) {
//                        maxBinCount = binCounts[binIndex];
//                    }
                }
            }
        }
    }

    class Bin {
        public ArrayList<Instant> binValues = new ArrayList<>();
        public ArrayList<Tuple> binTuples = new ArrayList<>();
    }
}
