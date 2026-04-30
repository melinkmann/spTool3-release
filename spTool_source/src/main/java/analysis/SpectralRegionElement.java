package analysis;

import dataModelNew.mz.Element;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;

import java.util.*;

public class SpectralRegionElement {

  private final List<Element> elements;
  private final List<String> names;
  private final List<double[]> intensities; // layout: [element][particle]

  public SpectralRegionElement(List<SpectralArray> spectralArrays) {

    // Group SpectralArrays by their parent Element (multiple isotopes per element are summed later)
    HashMap<Element, List<SpectralArray>> map = new LinkedHashMap<>();
    for (SpectralArray spectralArray : spectralArrays) {
      Isotope isotope = spectralArray.getIsotope();
      if (isotope != null) {
        Element element = isotope.getElement();
        map.computeIfAbsent(element, k -> new ArrayList<>()).add(spectralArray);
      }
    }

    // Preserve insertion order of elements
    elements = new ArrayList<>(map.keySet());

    int elementCount = elements.size();
    // Each SpectralArray holds one intensity value per region (particle)
    int regionsCount = elementCount == 0 ? 0 : spectralArrays.get(0).getIntensity().length;

    names = new ArrayList<>(elementCount);
    intensities = new ArrayList<>(elementCount);

    for (int i = 0; i < elementCount; i++) {

      Element element = elements.get(i);
      List<SpectralArray> subArrs = map.get(element);
      names.add(element.getSymbol());

      // Sum intensities across all isotopes belonging to this element
      double[] data = new double[regionsCount];
      if (subArrs != null) {
        for (SpectralArray subArr : subArrs) {
          data = ArrUtils.add(data, subArr.getIntensity());
        }
      }

      // Store as one double[] per element, indexed by particle — layout: [element][particle]
      intensities.add(data);
    }
  }

  public List<String> getNames() {
    return names;
  }

  public List<Element> getElements() {
    return elements;
  }

  public List<double[]> getIntensities() {
    return intensities;
  }
}