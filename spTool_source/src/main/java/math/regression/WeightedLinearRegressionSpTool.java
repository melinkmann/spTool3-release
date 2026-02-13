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

package math.regression;

//  https://github.com/lukehutch/WeightedLinearRegression.java/blob/master/WeightedLinearRegression.java

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpTool2 modified version of the code.
 * <p>
 * Weighted Linear Regression.
 * <p>
 * Ported from C# to Java by Luke Hutchison from
 * <a href="https://www.codeproject.com/Articles/25335/An-Algorithm-for-Weighted-Linear-Regression">An
 * Algorithm for Weighted Linear Regression</a>, Dr. Walt Fair, PE.
 * <p>
 * Originally licensed under the <a href="https://www.codeproject.com/info/cpol10.aspx">Code Project
 * Open License</a>.
 */
public class WeightedLinearRegressionSpTool implements Serializable {

  private static final Logger LOGGER = LogManager.getLogger(WLS.class);

  @Serial
  private static final long serialVersionUID = 1L;

  private double[][] V; // Least squares and var/covar matrix
  private double[] C; // Coefficients
  private double[] SEC; // Std Error of coefficients
  private double RYSQ; // Multiple correlation coefficient
  private double SDV; // Standard deviation of errors
  private double FReg; // Fisher F statistic for regression
  private double[] Ycalc; // Calculated values of Y
  private double[] DY; // Residual values of Y

  public WeightedLinearRegressionSpTool() {
    // Empty
  }

  public double[][] getLeastSquareCovarMatrix() {
    return V;
  }

  public double[] getCoefficients() {
    return C;
  }

  public double[] getStdErrOfCoefficients() {
    return SEC;
  }

  public double getRSqare() {
    return RYSQ;
  }

  public double getStdDevOfErrors() {
    return SDV;
  }

  public double getRegFisherF() {
    return FReg;
  }

  public double[] getCalculatedY() {
    return Ycalc;
  }

  public double[] getResidualsOfY() {
    return DY;
  }

  /**
   * Run a 1D weighted regression.
   */
  public void regress1d(double[] y, double[] x, double[] w) throws IllegalArgumentException{
    if (x.length == 0) {
      throw new IllegalArgumentException("Need more than 0 points");
    }
    if (x.length != y.length || x.length != w.length) {
      throw new IllegalArgumentException("Length mismatch");
    }

    // Insert 0th dimension with constant 1.0 value, so that bias value c can be determined
    double[][] X = new double[2][];
    X[0] = new double[x.length];
    Arrays.fill(X[0], 1.0);
    X[1] = x;
    regress(y, X, w);
  }

  /**
   * Calculate the weighted linear regression for a set of points.
   *
   * @param Y Y[j] is the j-th measured or observed dependent variable value.
   * @param X X[i][j] is the j-th measured independent variable value for the i-th variable.
   * @param W W[j] is the j-th weight value.
   */
  public void regress(double[] Y, double[][] X, double[] W) throws IllegalStateException {
    // Y[j]   = j-th observed data point
    // X[i][j] = j-th value of the i-th independent variable
    // W[j]   = j-th weight value

    int M = Y.length; // M = Number of data points
    int N = X.length; // N = Number of linear terms
    int NDF = M - N; // Degrees of freedom
    Ycalc = new double[M];
    DY = new double[M];
    // If not enough data, don't attempt regression
    if (NDF < 1) {
      throw new IllegalStateException(
          "Not enough degrees of freedom. Need more data points as input.");
    }
    V = new double[N][N];
    C = new double[N];
    SEC = new double[N];
    double[] B = new double[N]; // Vector for LSQ

    // Clear the matrices to start out
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        V[i][j] = 0;
      }
    }

    // Form Least Squares Matrix
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        V[i][j] = 0;
        for (int k = 0; k < M; k++) {
          V[i][j] = V[i][j] + W[k] * X[i][k] * X[j][k];
        }
      }
      B[i] = 0;
      for (int k = 0; k < M; k++) {
        B[i] = B[i] + W[k] * X[i][k] * Y[k];
      }
    }
    // V now contains the raw least squares matrix
    if (!SymmetricMatrixInvert(V)) {
      throw new IllegalStateException(
          "Matrix inversion failed. Probably insufficient data points or unstable matrix.");
    }
    // V now contains the inverted least square matrix
    // Matrix multiply to get coefficients C = VB
    for (int i = 0; i < N; i++) {
      C[i] = 0;
      for (int j = 0; j < N; j++) {
        C[i] = C[i] + V[i][j] * B[j];
      }
    }

    // Calculate statistics
    double TSS = 0;
    double RSS = 0;
    double YBAR = 0;
    double WSUM = 0;
    for (int k = 0; k < M; k++) {
      YBAR = YBAR + W[k] * Y[k];
      WSUM = WSUM + W[k];
    }
    YBAR = YBAR / WSUM;
    for (int k = 0; k < M; k++) {
      Ycalc[k] = 0;
      for (int i = 0; i < N; i++) {
        Ycalc[k] = Ycalc[k] + C[i] * X[i][k];
      }
      DY[k] = Ycalc[k] - Y[k];
      TSS = TSS + W[k] * (Y[k] - YBAR) * (Y[k] - YBAR);
      RSS = RSS + W[k] * DY[k] * DY[k];
    }
    double SSQ = RSS / NDF;
    RYSQ = 1 - RSS / TSS;
    FReg = 9999999;
    if (RYSQ < 0.9999999) {
      FReg = RYSQ / (1 - RYSQ) * NDF / (N - 1);
    }
    SDV = Math.sqrt(SSQ);

    // Calculate var-covar matrix and std error of coefficients
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        V[i][j] = V[i][j] * SSQ;
      }
      SEC[i] = Math.sqrt(V[i][i]);
    }
  }

  private static boolean SymmetricMatrixInvert(double[][] V) {
    int N = V.length;
    double[] t = new double[N];
    double[] Q = new double[N];
    double[] R = new double[N];

    // Invert a symetric matrix in V
    for (int M = 0; M < N; M++) {
      R[M] = 1;
    }
    for (int M = 0, K = 0; M < N; M++) {
      double Big = 0;
      for (int L = 0; L < N; L++) {
        double AB = Math.abs(V[L][L]);
        if ((AB > Big) && (R[L] != 0)) {
          Big = AB;
          K = L;
        }
      }
      if (Big == 0) {
        return false;
      }
      R[K] = 0;
      Q[K] = 1 / V[K][K];
      t[K] = 1;
      V[K][K] = 0;
      if (K != 0) {
        for (int L = 0; L < K; L++) {
          t[L] = V[L][K];
          if (R[L] == 0) {
            Q[L] = V[L][K] * Q[K];
          } else {
            Q[L] = -V[L][K] * Q[K];
          }
          V[L][K] = 0;
        }
      }
      if ((K + 1) < N) {
        for (int L = K + 1; L < N; L++) {
          if (R[L] != 0) {
            t[L] = V[K][L];
          } else {
            t[L] = -V[K][L];
          }
          Q[L] = -V[K][L] * Q[K];
          V[K][L] = 0;
        }
      }
      for (int L = 0; L < N; L++) {
        for (K = L; K < N; K++) {
          V[L][K] = V[L][K] + t[L] * Q[K];
        }
      }
    }
    int M = N;
    int L = N - 1;
    for (int K = 1; K < N; K++) {
      M = M - 1;
      L = L - 1;
      for (int J = 0; J <= L; J++) {
        V[M][J] = V[J][M];
      }
    }
    return true;
  }

}
