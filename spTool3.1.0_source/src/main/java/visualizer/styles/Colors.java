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

package visualizer.styles;

import com.google.common.collect.Iterables;
import dataModelNew.mz.Element;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import sandbox.montecarlo.Isotope;
import util.SnF;

public interface Colors {

  java.awt.Color CHART_BACKGROUND = new java.awt.Color(0xEEEEEE);

  java.awt.Color PLOT_BACKGROUND = new java.awt.Color(0xFFFFFF); // "0xDCDCDC"
  java.awt.Color PLOT_GRIDLINES = new java.awt.Color(0xA4A4BB);
  java.awt.Color PLOT_ZERO_MARKER = new java.awt.Color(0x373742);
  java.awt.Color PLOT_ANY_AXIS_MARKER = new java.awt.Color(0x08083D);

  java.awt.Color BOXPLOT_BACKGROUND = new java.awt.Color(250, 250, 250);

  java.awt.Color CHART_PLOT_TRANSPARENT = new java.awt.Color(0xFFFFFF, true);
  java.awt.Color RANGER_MARKERS = new java.awt.Color(0x780404);

  java.awt.Color LABEL_LIGHT = new java.awt.Color(0xD4D4D4);
  java.awt.Color LABEL_DARK = new java.awt.Color(0x575757);

  static final HashMap<Isotope, Colors> DEFAULT_ISOTOPE_MAP = new HashMap<>();

  java.awt.Color get();

  java.awt.Color get(double alpha);

  default javafx.scene.paint.Color getFX() {
    return getFX(1);
  }

  default javafx.scene.paint.Color getFX(double alpha) {
    Color col = get(alpha);
    alpha = Math.min(alpha, 1); // do not exceed limits
    return javafx.scene.paint.Color.rgb(col.getRed(), col.getGreen(), col.getBlue(),
        alpha);
  }

  static javafx.scene.paint.Color getFX(java.awt.Color col) {
    return getFX(col, 1);
  }

  static javafx.scene.paint.Color getFX(java.awt.Color col, double alpha) {
    return javafx.scene.paint.Color.rgb(col.getRed(), col.getGreen(), col.getBlue(), alpha);
  }


  static javafx.scene.paint.Color removeAlpha(javafx.scene.paint.Color col) {
    return javafx.scene.paint.Color.color(col.getRed(), col.getGreen(), col.getBlue());
  }

  static Iterable<Colors> getIterator(Colors[] colors) {
    final Iterable<Colors> iterator = Iterables.cycle(Arrays.asList(colors));
    return iterator;
  }

  static List<Colors> getDefaultColors() {
    List<Colors> colors = new ArrayList<>();

    colors.add(OkabeItoColors.ORANGE_DARK);
    colors.add(OkabeItoColors.VIOLET_DARK);
    colors.add(OkabeItoColors.BLACK_DARK);
    colors.add(OkabeItoColors.VERMILION_DARK);
    colors.add(OkabeItoColors.GREEN_BLUE_DARK);
    colors.add(OkabeItoColors.SKY_BLUE_DARK);
    colors.add(OkabeItoColors.PINK_DARK);

    colors.add(new SpColor(0d, 0d, 1d));
    colors.add(new SpColor(0d, 0.9d, 0d));
    colors.add(new SpColor(1d, 0d, 0d));
    colors.add(SpV2Colors.BLACK);

    colors.add(OkabeItoColors.VIOLET);
    colors.add(OkabeItoColors.GREEN_BLUE);
    colors.add(OkabeItoColors.VERMILION);
    colors.add(OkabeItoColors.SKY_BLUE);
    colors.add(OkabeItoColors.YELLOW);
    colors.add(OkabeItoColors.PINK);

    colors.add(SpV2Colors.GREY);
    colors.add(SpV2Colors.RED);
    colors.add(SpV2Colors.BLUE);

    return colors;
  }

  static Iterable<Colors> getDefaultIterator() {
    Iterable<Colors> iterator = Iterables.cycle(getDefaultColors());
    return iterator;
  }

  static Colors getColor(Isotope isotope) {
    if (DEFAULT_ISOTOPE_MAP.isEmpty()) {
      Iterator<Colors> iterator = getDefaultIterator().iterator();
      for (Element element : Element.values()) {
        for (Isotope iso : element.getIsotopes()) {
          DEFAULT_ISOTOPE_MAP.put(iso, iterator.next());
        }
      }
    }
    if (isotope != null && DEFAULT_ISOTOPE_MAP.containsKey(isotope)) {
      return DEFAULT_ISOTOPE_MAP.get(isotope);
    } else {
      return OkabeItoColors.BLACK;
    }
  }

  static String colorToRgbForXML(javafx.scene.paint.Color color) {
    String rgb = color.getRed() + "_"
        + color.getGreen() + "_"
        + color.getBlue();
    return rgb;
  }

  static javafx.scene.paint.Color rgbFromXmlToColor(String string) {
    double r = 0;
    double g = 0;
    double b = 0;
    // try to parse
    String[] rgb = string.split("_");
    if (rgb.length == 3) {
      r = SnF.strToDouble(rgb[0], 0);
      g = SnF.strToDouble(rgb[1], 0);
      b = SnF.strToDouble(rgb[2], 0);
    }
    if (r > 1 || g > 1 || b > 1) {
      r = r / 255;
      g = g / 255;
      b = b / 255;
    }
    return new javafx.scene.paint.Color(r, g, b, 1);
  }

  public static javafx.scene.paint.Color awtToFx(java.awt.Color awtColor) {
    int r = awtColor.getRed();
    int g = awtColor.getGreen();
    int b = awtColor.getBlue();
    int a = awtColor.getAlpha();
    return javafx.scene.paint.Color.rgb(r, g, b, a / 255.0);
  }

  public static java.awt.Color fxToAwt(javafx.scene.paint.Color fxColor) {
    int r = (int) Math.round(fxColor.getRed() * 255);
    int g = (int) Math.round(fxColor.getGreen() * 255);
    int b = (int) Math.round(fxColor.getBlue() * 255);
    int a = (int) Math.round(fxColor.getOpacity() * 255);
    return new java.awt.Color(r, g, b, a);
  }

  ///////////////////////////////////////////////////////////

  /**
   * For iterative changes: check the original color. Else, after some iterations, a dark color
   * becomes bright and we just go back and forth.
   */
  public static Colors variation(Colors originalColor, Colors col) {
    if (isDark(originalColor)) {
      return new SpColor(col.get().brighter().brighter());
    } else {
      return new SpColor(col.get().darker().darker().darker());
    }
  }

  /**
   * Changes brightness based on RGB. Perception is not very convincing.
   */
  public static Colors variationRGB(Colors col, int n) {
    boolean isDark = isDark(col);
    for (int i = 0; i < n; i++) {
      if (isDark) {
        col = whiter(col);
      } else {
        col = blacker(col);
      }
    }
    return col;
  }

  public static boolean isDark(Colors col) {
    return (col.get().getRed() < 100
        && col.get().getGreen() < 100
        && col.get().getBlue() < 100);
  }

  // --------------------
  public static Colors whiter(Colors color) {
    // 0 - 1
    double factor = 0.3;

    int r = color.get().getRed();
    int g = color.get().getGreen();
    int b = color.get().getBlue();

    int newR = (int) (r + (255 - r) * factor);
    int newG = (int) (g + (255 - g) * factor);
    int newB = (int) (b + (255 - b) * factor);

    return new SpColor(clamp(newR), clamp(newG), clamp(newB));
  }

  public static Colors blacker(Colors color) {
    // 0 - 1
    double factor = 0.3;

    int r = color.get().getRed();
    int g = color.get().getGreen();
    int b = color.get().getBlue();

    int newR = (int) (r * (1 - factor));
    int newG = (int) (g * (1 - factor));
    int newB = (int) (b * (1 - factor));

    return new SpColor(clamp(newR), clamp(newG), clamp(newB));
  }

  private static int clamp(int value) {
    return Math.min(255, Math.max(0, value));
  }


  /**
   * ChatGPT. Variation based on HSB. Performs/looks better.
   */
  public static Colors variationHSB(Colors rootColor, Colors color, int n) {
    Color col = color.get();
    Color root = rootColor.get();

    float[] rootHSB = Color.RGBtoHSB(root.getRed(), root.getGreen(), root.getBlue(), null);
    float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);

    // You can tweak these values to get smoother or stronger variation
    float factor = 0.2f; // 15% brightness change per step

    // Start from current brightness
    float brightness = hsb[2];

    // Decide direction based on whether the color is dark or light
    boolean isDark = rootHSB[2] < 0.5f;

    for (int i = 0; i < n; i++) {
      if (isDark) {
        brightness = Math.min(1f, brightness + factor);
      } else {
        brightness = Math.max(0f, brightness - factor);
      }
    }

    // Reconstruct color with same hue and saturation but modified brightness
    Color newColor = Color.getHSBColor(hsb[0], hsb[1], brightness);

    return new SpColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
  }
  ///////////////////////////////////////////////////////////

  /**
   * Mix colors in RGB space. Works not so well.
   */
  public static Colors averageColorRGB(Colors c1, Colors c2) {
    int red = (c1.get().getRed() + c2.get().getRed()) / 2;
    int green = (c1.get().getGreen() + c2.get().getGreen()) / 2;
    int blue = (c1.get().getBlue() + c2.get().getBlue()) / 2;
    int alpha = (c1.get().getAlpha() + c2.get().getAlpha()) / 2;

    return new SpColor(new Color(red, green, blue, alpha));
  }

  /**
   * ChatGPT. Mix colors advanced way.
   */
  public static Colors averageColorLAB(Colors c1, Colors c2, double ratio) {
    Color color1 = c1.get();
    Color color2 = c2.get();

    double[] lab1 = rgbToLab(color1);
    double[] lab2 = rgbToLab(color2);

    // --- Step 1: interpolate linearly in LAB ---
    double L = lab1[0] * (1 - ratio) + lab2[0] * ratio;
    double a = lab1[1] * (1 - ratio) + lab2[1] * ratio;
    double b = lab1[2] * (1 - ratio) + lab2[2] * ratio;

    // --- Step 2: check contrast (ΔE) vs both endpoints ---
    double deltaE1 = deltaE(lab1, new double[]{L, a, b});
    double deltaE2 = deltaE(lab2, new double[]{L, a, b});

    // --- Step 3: auto-adjust if too similar (low contrast) ---
    // If the mixed color is too close to both, tweak lightness/saturation slightly.
    if (Math.min(deltaE1, deltaE2) < 20) { // 20 ≈ perceptual threshold for distinctness
      if (L > 50) {
        L = Math.min(100, L + 10); // brighten light colors
      } else {
        L = Math.max(0, L - 10);          // darken dark colors
      }

      // Increase chroma (distance from neutral gray)
      a *= 1.15;
      b *= 1.15;
    }

    // --- Step 4: convert back to RGB ---
    Color mixed = labToRgb(L, a, b);

    return new SpColor(mixed.getRed(), mixed.getGreen(), mixed.getBlue());
  }

  private static double deltaE(double[] lab1, double[] lab2) {
    double dL = lab1[0] - lab2[0];
    double da = lab1[1] - lab2[1];
    double db = lab1[2] - lab2[2];
    return Math.sqrt(dL * dL + da * da + db * db);
  }

  private static double[] rgbToLab(Color color) {
    double r = pivotRgb(color.getRed() / 255.0);
    double g = pivotRgb(color.getGreen() / 255.0);
    double b = pivotRgb(color.getBlue() / 255.0);

    double X = r * 0.4124 + g * 0.3576 + b * 0.1805;
    double Y = r * 0.2126 + g * 0.7152 + b * 0.0722;
    double Z = r * 0.0193 + g * 0.1192 + b * 0.9505;

    X /= 0.95047;
    Y /= 1.00000;
    Z /= 1.08883;

    X = pivotXYZ(X);
    Y = pivotXYZ(Y);
    Z = pivotXYZ(Z);

    double L = 116 * Y - 16;
    double a = 500 * (X - Y);
    double bVal = 200 * (Y - Z);

    return new double[]{L, a, bVal};
  }

  private static Color labToRgb(double L, double a, double b) {
    double Y = (L + 16) / 116.0;
    double X = a / 500.0 + Y;
    double Z = Y - b / 200.0;

    X = invPivotXYZ(X) * 0.95047;
    Y = invPivotXYZ(Y) * 1.00000;
    Z = invPivotXYZ(Z) * 1.08883;

    double r = X * 3.2406 + Y * -1.5372 + Z * -0.4986;
    double g = X * -0.9689 + Y * 1.8758 + Z * 0.0415;
    double bVal = X * 0.0557 + Y * -0.2040 + Z * 1.0570;

    r = invPivotRgb(r);
    g = invPivotRgb(g);
    bVal = invPivotRgb(bVal);

    return new Color(clamp((int) Math.round(r * 255)),
        clamp((int) Math.round(g * 255)),
        clamp((int) Math.round(bVal * 255)));
  }

  private static double pivotRgb(double c) {
    return (c <= 0.04045) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  }

  private static double invPivotRgb(double c) {
    return (c <= 0.0031308) ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
  }

  private static double pivotXYZ(double t) {
    return (t > 0.008856) ? Math.cbrt(t) : (7.787 * t + 16.0 / 116.0);
  }

  private static double invPivotXYZ(double t) {
    double t3 = t * t * t;
    return (t3 > 0.008856) ? t3 : (t - 16.0 / 116.0) / 7.787;
  }

  //////////////////////////////////////////////////////////////////////////////////


  public static class SpColor implements Colors {

    // FROM 0 - 1 !!!
    // AWT: 0-255 if int, 0 - 1 if double, getters: int 0-255
    // FX: 0-1 "color()" or 0-255 "rgb()"; getters: 0-1
    private final double r;
    private final double g;
    private final double b;

    public SpColor(double r, double g, double b) {
      if (r > 1 || g > 1 || b > 1) {
        this.r = r / 255;
        this.g = g / 255;
        this.b = b / 255;
      } else {
        this.r = r;
        this.g = g;
        this.b = b;
      }
    }

    public SpColor(int r, int g, int b) {
      this.r = r / 255d;
      this.g = g / 255d;
      this.b = b / 255d;
    }


    public SpColor(javafx.scene.paint.Color color) {
      this.r = color.getRed();
      this.g = color.getGreen();
      this.b = color.getBlue();
    }

    public SpColor(Color color) {
      this.r = color.getRed() / 255d;
      this.g = color.getGreen() / 255d;
      this.b = color.getBlue() / 255d;
    }

    @Override
    public Color get() {
      return get(1d);
    }

    @Override
    public Color get(double alpha) {
      return new java.awt.Color((float) r, (float) g, (float) b, (float) alpha);
    }

    @Override
    public javafx.scene.paint.Color getFX() {
      return getFX(1d);
    }

    @Override
    public javafx.scene.paint.Color getFX(double alpha) {
      return javafx.scene.paint.Color.color(r, g, b, alpha);
    }
  }

}
