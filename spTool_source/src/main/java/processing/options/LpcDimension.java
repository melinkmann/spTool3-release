package processing.options;

public enum LpcDimension {
  LARGEST_FERET_DIAMETER(1, "Largest Feret Diameter [µm]"),
  SMALLEST_FERET_DIAMETER(2, "Smallest Feret Diameter [µm]"),
  LARGEST_LEGENDRE_DIAMETER(3, "Largest Legendre Diameter [µm]"),
  SMALLEST_LEGENDRE_DIAMETER(4, "Smallest Legendre Diameter [µm]"),
  CIRCLE_EQUIVALENT_DIAMETER(5, "Circle-Equivalent Diameter [µm]"),
  CIRCULARITY(6, "Circularity [-]"),
  CONVEXITY(7, "Convexity [-]"),
  SOLIDITY(8, "Solidity [-]"),
  PARTICLE_DIAMETER_ASPECT_RATIO(9, "Particle Diameter Aspect Ratio [-]");

  private final int col;
  private final String displayName;

  LpcDimension(int col, String displayName) {
    this.col = col;
    this.displayName = displayName;
  }

  public int getCol() {
    return col;
  }

  @Override
  public String toString() {
    return displayName;
  }
}