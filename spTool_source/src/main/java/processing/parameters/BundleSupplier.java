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

package processing.parameters;

import java.io.Serial;
import java.io.Serializable;

import processing.parameterSets.ParamBundle;
import processing.parameterSets.bundle.*;

public interface BundleSupplier extends Serializable {

  ParamBundle createNewBundle(SpawnControlParameter parentParameter);

  String getBundleID();

  public static class ElementBundleSupplier implements BundleSupplier {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public ElementBundleSupplier() {
    }

    @Override
    public ParamBundle createNewBundle(SpawnControlParameter parentParameter) {
      return new ElementBundle(parentParameter);
    }

    @Override
    public String getBundleID() {
      return ParamBundle.BUNDLE_ID_ELEMENT;
    }
  }

  public static class SiABundleSupplier implements BundleSupplier {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public SiABundleSupplier() {
    }

    @Override
    public ParamBundle createNewBundle(SpawnControlParameter parentParameter) {
      return new SiaBundle(parentParameter);
    }

    @Override
    public String getBundleID() {
      return ParamBundle.BUNDLE_ID_SIA;
    }
  }

  public static class RoiStartStopBundleSupplier implements BundleSupplier {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public RoiStartStopBundleSupplier() {
    }

    @Override
    public ParamBundle createNewBundle(SpawnControlParameter parentParameter) {
      return new RoiStartStopBundle(parentParameter);
    }

    @Override
    public String getBundleID() {
      return ParamBundle.BUNDLE_ID_ROI_START_STOP;
    }
  }

  public static class RoiSigFactorBundleSupplier implements BundleSupplier {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public RoiSigFactorBundleSupplier() {
    }

    @Override
    public ParamBundle createNewBundle(SpawnControlParameter parentParameter) {
      return new RoiSigFactorBundle(parentParameter);
    }

    @Override
    public String getBundleID() {
      return ParamBundle.BUNDLE_ID_ROI_SIG_FACTOR;
    }
  }

  public static class AlignFilterStartStopBundleSupplier implements BundleSupplier {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public AlignFilterStartStopBundleSupplier() {
    }

    @Override
    public ParamBundle createNewBundle(SpawnControlParameter parentParameter) {
      return new AlignFilterStartStopBundle(parentParameter);
    }

    @Override
    public String getBundleID() {
      return ParamBundle.BUNDLE_ID_ALIGN_FILTER_START_STOP;
    }
  }
}
