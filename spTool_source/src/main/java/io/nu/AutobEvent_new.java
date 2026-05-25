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

package io.nu;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal: one blanking event from a {@code .autob} file.
 * Binary layout per event (little-endian):
 * uint32  cycNumber
 * uint32  segNumber
 * uint32  acqNumber
 * uint32  trigStartTime    (skipped)
 * uint32  trigEndTime      (skipped)
 * uint8   type             0 = blanker opened, 1 = blanker closed
 * int32   numEdges
 * numEdges × uint32        raw ToF edge positions
 */
public class AutobEvent_new implements Serializable {
  @Serial
  long serialVersionUID = 1_000_000L;

  final int cycNumber;
  final int segNumber;
  final int acqNumber;
  final int type;
  final long[] edges;

  AutobEvent_new(int cycNumber, int segNumber, int acqNumber, int type, long[] edges) {
    this.cycNumber = cycNumber;
    this.segNumber = segNumber;
    this.acqNumber = acqNumber;
    this.type = type;
    this.edges = edges;
  }
}
