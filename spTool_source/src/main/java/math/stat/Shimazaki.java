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

package math.stat;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.statistics.HistogramDataset;
import util.ArrUtils;

public class Shimazaki {

  private static final Logger LOGGER = LogManager.getLogger(Shimazaki.class);


  private final double[] data;
  private final int minBinCount;
  private int maxBinCount;

  private final double datMin;
  private final double datMax;

  private double bestWidth; // dummy
  private boolean isCalculated;

  public Shimazaki() {
    this.data = new double[]{1};
    this.datMin = 1;
    this.datMax = 1;
    this.minBinCount = 1;
    this.maxBinCount = 1;
    this.isCalculated = true;
    //
    this.bestWidth = 0; // dummy

  }

  public Shimazaki(double[] data, int minBinCount, int maxBinCount) {
    this.data = data;
    this.datMin = ArrUtils.getMin(data);
    this.datMax = ArrUtils.getMax(data);
    this.minBinCount = Math.max(2, minBinCount); // must be > 1
    this.maxBinCount = maxBinCount;
    this.isCalculated = false;
    //
    this.bestWidth = 0; // dummy
  }

  // spTool2 implementation.
  public double getBestWidth() {

    if (!isCalculated && data.length > 1) {
      // Possible no of bins for the histogram based on min/max
      int[] binCounts = IntStream.range(minBinCount, maxBinCount).toArray();
      // Calculate bin sizes i.e. width based on this
      double datSpan = datMax - datMin;

      double[] binWidths = ArrUtils.divide(datSpan, binCounts);

      // Computation of the Cost Function
      double[] cost = new double[binCounts.length];
      for (int i = 0; i < binCounts.length; i++) {
        HistogramDataset hds = new HistogramDataset();
        hds.addSeries("Dummy", data, binCounts[i]);

        double[] absFreqPerBin = new double[hds.getItemCount(0)];
        for (int n = 0; n < hds.getItemCount(0); n++) {
          absFreqPerBin[n] = hds.getY(0, n).doubleValue();
        }

        DescriptiveStatistics da = new DescriptiveStatistics(absFreqPerBin);
        double k = da.getMean(); // Mean of event count

        // Variance of event count, v = sum( (ki-k).^2 )/N(i);
        double v = da.getPopulationVariance();

        cost[i] = (2 * k - v) / Math.pow(binWidths[i], 2);    // The Cost Function
      }

      // Best bin width
      int idxOfLowestCost = ArrUtils.getIndexAtMin(cost);
      // Not the best idea because the min search is not perfect but cost fluctuates so that the penultimate point may be lower than the last but still an overall fail..
      if (idxOfLowestCost == cost.length - 1) {
        LOGGER.info("Model failed failed because best width was reached "
            + "for maximum number of bins, n=" + maxBinCount + ". "
            + "Chose middle of possible bins, which equals a bin width of "
            + binWidths[idxOfLowestCost / 2] + ".");
        // System.out.println(Arrays.toString(cost));
      }
      this.bestWidth = binWidths[idxOfLowestCost / 2];
      this.isCalculated = true;

      // Send cost function to clipboard
//      if (export) {
//        ExportWriter exp = new ClipboardWriter();
//        TabularSheet sheet = new TabularSheet(exp);
//        TabularBlock block = new TabularBlockVertical();
//        block.addColumn("NoOfBins", ArrUtils.toStringList(binCounts));
//        block.addColumn("BinWidth", ArrUtils.toStringList(binWidths, NF.D1C9));
//        block.addColumn("CostFunction", ArrUtils.toStringList(cost, NF.D1C9));
//        BlockCollection blockCollection = new BlockCollectionVertical();
//        blockCollection.addBlock(block);
//        sheet.addBlockCollection(blockCollection);
//        sheet.export();
//      }
    }
    return bestWidth;
  }


/*
Transcription from https://www.neuralengine.org/res/histogram.html

A Sample Matlab Program
The matlab function sshist.m is now available. [2008/11/19]
To plot an optimized histogram of the data x (a vector contains samples as an element), type

optN = sshist(x); hist(x,optN);

Below is a simple sample program.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% 2006 Author Hideaki Shimazaki
% Department of Physics, Kyoto University
% shimazaki at ton.scphys.kyoto-u.ac.jp
% Please feel free to use/modify this program.
%
% Data: the duration for eruptions of
% the Old Faithful geyser in Yellowstone National Park (in minutes)
clear all;
x = [4.37 3.87 4.00 4.03 3.50 4.08 2.25 4.70 1.73 4.93 1.73 4.62 ...
     3.43 4.25 1.68 3.92 3.68 3.10 4.03 1.77 4.08 1.75 3.20 1.85 ...
     4.62 1.97 4.50 3.92 4.35 2.33 3.83 1.88 4.60 1.80 4.73 1.77 ...
     4.57 1.85 3.52 4.00 3.70 3.72 4.25 3.58 3.80 3.77 3.75 2.50 ...
     4.50 4.10 3.70 3.80 3.43 4.00 2.27 4.40 4.05 4.25 3.33 2.00 ...
     4.33 2.93 4.58 1.90 3.58 3.73 3.73 1.82 4.63 3.50 4.00 3.67 ...
     1.67 4.60 1.67 4.00 1.80 4.42 1.90 4.63 2.93 3.50 1.97 4.28 ...
     1.83 4.13 1.83 4.65 4.20 3.93 4.33 1.83 4.53 2.03 4.18 4.43 ...
     4.07 4.13 3.95 4.10 2.27 4.58 1.90 4.50 1.95 4.83 4.12];

x_min = min(x);
x_max = max(x);

N_MIN = 4;              % Minimum number of bins (integer)
                        % N_MIN must be more than 1 (N_MIN > 1).
N_MAX = 50;             % Maximum number of bins (integer)

N = N_MIN:N_MAX;                      % # of Bins
D = (x_max - x_min) ./ N;             % Bin Size Vector


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Computation of the Cost Function
for i = 1: length(N)
	edges = linspace(x_min,x_max,N(i)+1);	% Bin edges

	ki = histc(x,edges);            % Count # of events in bins
	ki = ki(1:end-1);

	k = mean(ki);                   % Mean of event count
	v = sum( (ki-k).^2 )/N(i);      % Variance of event count

	C(i) = ( 2*k - v ) / D(i)^2;    % The Cost Function

end


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Optimal Bin Size Selectioin
[Cmin idx] = min(C);
optD = D(idx);                         % *Optimal bin size
edges = linspace(x_min,x_max,N(idx)+1);  % Optimal segmentation


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Display an Optimal Histogram and the Cost Function
subplot(1,2,1); hist(x,edges); axis square;
subplot(1,2,2); plot(D,C,'k.',optD,Cmin,'r*'); axis square;
 */


  // chatGPT translation of the matlab code
  public static double optimalBinWidth(double[] data) {
    Arrays.sort(data);
    int n = data.length;
    double min = data[0];
    double max = data[n - 1];

    int mMin = 5;  // Minimum number of bins
    int mMax = (int) (2 * Math.sqrt(n));  // Maximum number of bins (heuristic)
    double bestH = 0;
    double minCost = Double.POSITIVE_INFINITY;
    int bestM = 0;

    for (int m = mMin; m <= mMax; m++) {
      double h = (max - min) / m;  // Bin width
      int[] binCounts = new int[m];

      // Count data points in each bin
      for (double value : data) {
        int binIndex = Math.min(m - 1, (int) ((value - min) / h));
        binCounts[binIndex]++;
      }

      // Compute variance of bin counts
      double mean = (double) n / m;
      double variance = 0;
      for (int count : binCounts) {
        variance += Math.pow(count - mean, 2);
      }
      variance /= m;

      // Compute cost function C(h)
      double cost = (2 * mean - variance) / (h * h);

      // Find the minimum cost
      if (cost < minCost) {
        minCost = cost;
        bestH = h;
        bestM = m;
      }
    }

    // System.out.println("best width: " + bestH + " at bestM: " + bestM + " with mMax: " + mMax);
    return bestH;
  }

} // class