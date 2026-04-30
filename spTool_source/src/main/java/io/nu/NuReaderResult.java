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

/*
 *  Java port of spcal/io/nu.py  (https://github.com/djdt/spcal)
 *  Original Python by djdt – ported to Java 15 for spTool3.
 */

package io.nu;

import dataModelNew.TISeries;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result container produced by {@link NuReader_v1}.
 *
 * <h2>Two-stage usage</h2>
 * <ol>
 *   <li>Call {@link NuReader_v1#readAvailableMZ(java.nio.file.Path)} to obtain
 *       an instance and inspect {@link #mzValues} to see every channel recorded
 *       in the dataset.</li>
 *   <li>Call {@link NuReader_v1#readSelectedChannels(java.nio.file.Path, List)} with
 *       a subset of those m/z values to populate {@link #channelData} and
 *       {@link #blankedTimeRanges}.</li>
 * </ol>
 *
 * <h2>Signal data</h2>
 * <p>Intensity values in each {@link TISeries} are expressed in ion counts
 * (raw ADC / {@code AverageSingleIonArea}) and are <em>never modified</em> by
 * the blanking step.  The original detector readings are preserved as-is.
 *
 * <h2>Auto-blanking metadata</h2>
 * <p>{@link #blankedTimeRanges} records, per matched m/z channel, the
 * time-index intervals where the Nu instrument's blanker was active.  Each
 * entry is a two-element {@code int[]} representing the half-open interval
 * {@code [startIndex, endIndex)} into the corresponding {@link TISeries} time
 * axis.  Use this information to exclude or flag those data points downstream
 * without altering the stored signal values.
 *
 * <p>Example:
 * <pre>{@code
 *   List<int[]> ranges = result.blankedTimeRanges.get(197.0);
 *   if (ranges != null) {
 *       for (int[] range : ranges) {
 *           int from = range[0]; // inclusive
 *           int to   = range[1]; // exclusive
 *           // indices [from, to) were blanked for this channel
 *       }
 *   }
 * }</pre>
 */
public final class NuReaderResult {

    // -------------------------------------------------------------------------
    // Stage-1 field
    // -------------------------------------------------------------------------

    /**
     * All m/z values (Da) discovered in the first acquisition, derived from
     * peak-centre times via the instrument's mass-calibration coefficients.
     * Sorted ascending.
     */
    public final List<Double> mzValues;

    /** Parsed contents of {@code run.info}. */
    public final NuRunInfo runInfo;

    // -------------------------------------------------------------------------
    // Stage-2 fields  (empty until readSelectedChannels has been called)
    // -------------------------------------------------------------------------

    /**
     * Signal data for the channels requested in Stage 2.
     *
     * <p>Key: matched m/z value in Da (may differ slightly from the requested
     * value – the nearest recorded channel within 0.1 Da is selected).
     * Value: time-intensity series in ion counts with a uniform dwell-time
     * spacing starting at {@code t = 0}.  Signal values are untouched by the
     * blanking step.
     *
     * <p>Empty when only Stage 1 has been performed.
     */
    public final Map<Double, TISeries> channelData;

    /**
     * Auto-blanked time-index ranges, keyed by the same matched m/z values
     * used in {@link #channelData}.
     *
     * <p>Each value is a list of {@code int[2]} arrays where
     * {@code int[0]} is the inclusive start index and {@code int[1]} is the
     * exclusive end index of a blanked interval in the corresponding
     * {@link TISeries}.  The list may be empty for a channel that has no
     * blanked intervals.
     *
     * <p>Empty map when only Stage 1 has been performed.
     */
    public final Map<Double, List<int[]>> blankedTimeRanges;

    // -------------------------------------------------------------------------
    // Package-private constructors – created only by NuReader
    // -------------------------------------------------------------------------

    /** Stage-1 constructor: channel discovery only, no signal data yet. */
    NuReaderResult(List<Double> mzValues, NuRunInfo runInfo) {
        this.mzValues          = Collections.unmodifiableList(mzValues);
        this.runInfo           = runInfo;
        this.channelData       = Collections.unmodifiableMap(new HashMap<>());
        this.blankedTimeRanges = Collections.unmodifiableMap(new HashMap<>());
    }

    /** Stage-2 constructor: full data including signals and blanking metadata. */
    NuReaderResult(
            List<Double>                mzValues,
            NuRunInfo                   runInfo,
            Map<Double, TISeries>       channelData,
            Map<Double, List<int[]>>    blankedTimeRanges) {

        this.mzValues          = Collections.unmodifiableList(mzValues);
        this.runInfo           = runInfo;
        this.channelData       = Collections.unmodifiableMap(new HashMap<>(channelData));
        this.blankedTimeRanges = Collections.unmodifiableMap(new HashMap<>(blankedTimeRanges));
    }

    // -------------------------------------------------------------------------
    // Convenience queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if channel signal data has been loaded (Stage 2
     * completed and at least one channel was matched).
     */
    public boolean hasChannelData() {
        return !channelData.isEmpty();
    }

    /**
     * Returns {@code true} if any auto-blanking ranges were recorded during
     * Stage 2.
     */
    public boolean hasBlankedRanges() {
        boolean found = false;
        for (List<int[]> ranges : blankedTimeRanges.values()) {
            if (!ranges.isEmpty()) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public String toString() {
        return "NuReaderResult{"
                + "availableChannels=" + mzValues.size()
                + ", loadedChannels="  + channelData.size()
                + ", runInfo="         + runInfo
                + '}';
    }
}
