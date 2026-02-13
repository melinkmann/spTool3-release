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

package processing.options;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum OutlierModel implements Serializable {

  NOTHING {
    @Override
    public String toString() {
      return "No outlier removal";
    }
  },

  ONE_ITERATION {
    @Override
    public String toString() {
      return "One iteration";
    }
  },

  FIXED_NUMBER_ITERATION {
    @Override
    public String toString() {
      return "Fixed number of iterations";
    }
  },

  ITERATE_TO_CONVERGENCE {
    @Override
    public String toString() {
      return "Unlimited iterations";
    }
  },

  SMART_INCREMENT_ITERATION {
    @Override
    public String toString() {
      return "Smart-increment iteration";
    }
  },

  ROSNER {
    @Override
    public String toString() {
      return "Rosner";
    }
  };

  public static List<OutlierModel> listIterativeTests() {
    OutlierModel[] members = new OutlierModel[]{
        ONE_ITERATION,
        FIXED_NUMBER_ITERATION,
        ITERATE_TO_CONVERGENCE,
        SMART_INCREMENT_ITERATION};
    return new ArrayList<>(Arrays.asList(members));
  }

  public static List<OutlierModel> listAllExceptNothing() {
    OutlierModel[] members = OutlierModel.values();
    List<OutlierModel> list = new ArrayList<>(Arrays.asList(members));
    list.remove(OutlierModel.NOTHING);
    return list;
  }

  public static OutlierModel[] getGaussianTests() {
    return OutlierModel.values();
  }

  public static OutlierModel[] getPoissonTests() {
    OutlierModel[] members = new OutlierModel[]{
        NOTHING,
        ONE_ITERATION,
        FIXED_NUMBER_ITERATION,
        ITERATE_TO_CONVERGENCE};
    return members;
  }


}
