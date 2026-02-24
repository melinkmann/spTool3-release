package math.units.enums;

import math.units.ConvertibleUnit;

import java.io.Serializable;

public enum MolarUnit implements ConvertibleUnit, Serializable {

  MOL {
    @Override
    public double getMultiplierToSI() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "mol";
    }
  },

  MILLI_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-3;
    }

    @Override
    public String getLiteralString() {
      return "mmol";
    }
  },

  MICRO_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-6;
    }

    @Override
    public String getLiteralString() {
      return "µmol";
    }
  },

  NANO_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-9;
    }

    @Override
    public String getLiteralString() {
      return "nmol";
    }
  },

  PICO_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-12;
    }

    @Override
    public String getLiteralString() {
      return "pmol";
    }
  },

  FEMTO_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-15;
    }

    @Override
    public String getLiteralString() {
      return "fmol";
    }
  },

  ATTO_MOL {
    @Override
    public double getMultiplierToSI() {
      return 1E-18;
    }

    @Override
    public String getLiteralString() {
      return "amol";
    }
  };


  @Override
  public double toSIRoot(double value) {
    final double val = value * getMultiplierToSI();
    return val;
  }


  @Override
  public double toTarget(double siUnitValue) {
    final double val = siUnitValue / getMultiplierToSI();
    return val;
  }

  public abstract double getMultiplierToSI();

  @Override
  public boolean isCompatible(ConvertibleUnit unit) {
    return unit instanceof MolarUnit;
  }


}
