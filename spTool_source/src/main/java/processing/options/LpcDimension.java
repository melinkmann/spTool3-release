package processing.options;

public enum LpcDimension {

  // =========================
  // Shared geometric features
  // =========================

  LARGEST_FERET_DIAMETER(1, 1, "Largest Feret Diameter [µm]"),
  SMALLEST_FERET_DIAMETER(2, 2, "Smallest Feret Diameter [µm]"),
  LARGEST_LEGENDRE_DIAMETER(3, 3, "Largest Legendre Diameter [µm]"),
  SMALLEST_LEGENDRE_DIAMETER(4, 4, "Smallest Legendre Diameter [µm]"),

  CIRCLE_EQUIVALENT_DIAMETER(5, 5, "Circle-Equivalent Diameter [µm]"),
  CIRCULARITY(6, 6, "Circularity [-]"),
  CONVEXITY(7, 7, "Convexity [-]"),
  SOLIDITY(8, 8, "Solidity [-]"),

  PARTICLE_DIAMETER_ASPECT_RATIO(9, 9, "Particle Diameter Aspect Ratio [-]"),

  // =========================
  // Tom-only features
  // =========================

  MAXIMUM_WIDTH(10, -1, "Maximum Width [µm]"),
  MINIMUM_WIDTH(11, -1, "Minimum Width [µm]"),
  PERIMETER(12, -1, "Perimeter [µm]"),
  RADIUS(13, -1, "Radius [µm]"),
  SHARPNESS(14, -1, "Sharpness [-]"),
  X(15, -1, "X Position [px]"),
  Y(16, -1, "Y Position [px]"),

  // =========================
  // Rafa-only features
  // =========================

  DETECTION_TIME(-1, 10, "Detection Time [sec.]"),
  FRAME_ID(-1, 11, "Frame ID"),
  CENTROID_ROW(-1, 12, "Centroid Row [px]"),
  CENTROID_COL(-1, 13, "Centroid Column [px]"),

  // =========================
  // Special / shared identifiers
  // =========================

  PARTICLE_ID(0, 0, "Particle ID");

  private final int tomCol;
  private final int rafaCol;
  private final String displayName;

  LpcDimension(int tomCol, int rafaCol, String displayName) {
    this.tomCol = tomCol;
    this.rafaCol = rafaCol;
    this.displayName = displayName;
  }

  public int getCol(boolean isRafa, boolean isTom) {
    if (isRafa) return rafaCol;
    if (isTom) return tomCol;
    return -1;
  }

  public boolean existsInTom() {
    return tomCol != -1;
  }

  public boolean existsInRafa() {
    return rafaCol != -1;
  }

  @Override
  public String toString() {
    return displayName;
  }
}