package processing.options;

public enum LpcDimension {

  // =========================
  // Shared features
  // =========================

  PARTICLE_ID(0, 0, "Particle ID"),
  FRAME(1, 11, "Frame number"),

  CIRCLE_EQUIVALENT_DIAMETER(5, 5, "Circle-Equivalent Diameter [µm]"),
  CIRCULARITY(6, 6, "Circularity [-]"),
  CONVEXITY(7, 7, "Convexity [-]"),

  X(14, 13, "X Position [px]"),
  Y(15, 12, "Y Position [px]"),

  // =========================
  // Tom-only features
  // =========================
  FRAME_COUNT(2, -1, "Frame Count [-]"),
  AREA(3, -1, "Area [px·px]"),
  ASPECT_RATIO(4, -1, "Aspect Ratio [-]"),
  INTENSITY(8, -1, "Intensity [-]"),
  MAXIMUM_WIDTH(9, -1, "Maximum Width [µm]"),
  MINIMUM_WIDTH(10, -1, "Minimum Width [µm]"),
  PERIMETER(11, -1, "Perimeter [µm]"),
  RADIUS(12, -1, "Radius [µm]"),
  SHARPNESS(13, -1, "Sharpness [-]"),


  // =========================
  // Rafa-only features
  // =========================

  LARGEST_FERET_DIAMETER(-1, 1, "Largest Feret Diameter [µm]"),
  SMALLEST_FERET_DIAMETER(-1, 2, "Smallest Feret Diameter [µm]"),
  LARGEST_LEGENDRE_DIAMETER(-1, 3, "Largest Legendre Diameter [µm]"),
  SMALLEST_LEGENDRE_DIAMETER(-1, 4, "Smallest Legendre Diameter [µm]"),
  SOLIDITY(-1, 8, "Solidity [-]"),
  PARTICLE_DIAMETER_ASPECT_RATIO(-1, 9, "Particle Diameter Aspect Ratio [-]"),
  DETECTION_TIME(-1, 10, "Detection Time [sec.]");


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