//
//   Copyright 2020  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.functions;

import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptBucketizerFunction;
import io.warp10.script.WarpScriptStack.Macro;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bucketizes some GTS instances using a bucketduration rather than a bucketspan.
 */
public class DURATION_BUCKETIZE extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  private static final String DEFAULT_NAME = "DURATION.BUCKETIZE";
  private static final Matcher DURATION_RE = Pattern.compile("^P(?!$)(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?(T(?=\\d)(\\d+H)?(\\d+M)?((\\d+|\\d.(\\d)+)S)?)?$").matcher("");
  private static final String DURATION_ATTRIBUTE_KEY = ".bucketduration";
  private static final String OFFSET_ATTRIBUTE_KEY = ".bucketoffset";

  public DURATION_BUCKETIZE(String name) {
    super(name);
  }

  public DURATION_BUCKETIZE() {
    super(DEFAULT_NAME);
  }

  public static String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object top = stack.pop();

    //
    // Handle parameters
    //

    if (!(top instanceof List)) {
      throw new WarpScriptException(getName() + " expects a list as input.");
    }

    List<Object> params = (List<Object>) top;

    if (5 > params.size()) {
      throw new WarpScriptException(getName() + " needs a list of at least 5 parameters as input.");
    }

    DateTimeZone dtz = DateTimeZone.UTC;
    if (params.get(params.size() - 1) instanceof String) {
      String tz = (String) params.remove(params.size() - 1);
      dtz = DateTimeZone.forID(tz);
    }

    for (int i = 0; i < params.size() - 4; i++) {
      if (!(params.get(i) instanceof GeoTimeSerie) && !(params.get(i) instanceof List)) {
        throw new WarpScriptException(getName() + " expects a list of Geo Time Series as first parameter.");
      }
    }

    if (!(params.get(params.size() - 4) instanceof WarpScriptBucketizerFunction) && !(params.get(params.size() - 4) instanceof Macro) && null != params.get(params.size() - 4)) {
      throw new WarpScriptException(getName() + " expects a bucketizer function, a macro, or NULL as fourth to last parameter.");
    }

    if (!(params.get(params.size() - 3) instanceof Long) || !(params.get(params.size() - 2) instanceof String) || !(params.get(params.size() - 1) instanceof Long)) {
      throw new WarpScriptException(getName() + " expects lastbucket, bucketduration, bucketcount (and optionally timezone) as last parameters.");
    }

    List<GeoTimeSerie> series = new ArrayList<GeoTimeSerie>();


    for (int i = 0; i < params.size() - 4; i++) {
      if (params.get(i) instanceof GeoTimeSerie) {
        series.add((GeoTimeSerie) params.get(i));
      } else if (params.get(i) instanceof List) {
        for (Object o : (List) params.get(i)) {
          if (!(o instanceof GeoTimeSerie)) {
            throw new WarpScriptException(getName() + " expects a list of Geo Time Series as first parameter.");
          }
          series.add((GeoTimeSerie) o);
        }
      }
    }

    Object bucketizer = params.get(params.size() - 4);
    long lastbucket = (long) params.get(params.size() - 3);
    String bucketduration = (String) params.get(params.size() - 2);
    long bucketcount = (long) params.get(params.size() - 1);

    //
    // Check that lastbucket is not 0
    //

    if (0 == lastbucket) {
      throw new WarpScriptException(getName() + " does not allow lastbucket to be 0. It must be specified.");
    }

    //
    // Check that bucketcount is not negative or null and not over maxbuckets
    //

    if (bucketcount <= 0) {
      throw new WarpScriptException(getName() + " expects a positive bucketcount.");
    }

    long maxbuckets = (long) stack.getAttribute(WarpScriptStack.ATTRIBUTE_MAX_BUCKETS);
    if (bucketcount > maxbuckets) {
      throw new WarpScriptException("Bucket count (" + bucketcount + ") would exceed maximum value of " + maxbuckets);
    }

    //
    // Check that input gts are not already duration-bucketized
    //

    for (GeoTimeSerie gts : series) {
      if (gts.getMetadata().getAttributes().get(DURATION_ATTRIBUTE_KEY) != null || gts.getMetadata().getAttributes().get(OFFSET_ATTRIBUTE_KEY) != null) {
        throw new WarpScriptException(getName() + " expects GTS for which the attributes " + DURATION_ATTRIBUTE_KEY + " and " + OFFSET_ATTRIBUTE_KEY + " are not be set. If an input GTS is supposed to be already duration-bucketized, duration-unbucketize them first before applying a new duration-bucketization.");
      }
    }

    //
    // Check nullity of bucketizer
    //

    if (null == bucketizer) {
      throw new WarpScriptException(getName() + " expects a non null bucketizer.");
    }

    //
    // Convert duration to joda.time.Period
    //

    if (!DURATION_RE.reset(bucketduration).matches()) {
      throw new WarpScriptException(getName() + "expects the bucketduration parameter to be a valid ISO8601 duration with positive coefficients.");
    }
    ADDDURATION.ReadWritablePeriodWithSubSecondOffset bucketperiod = ADDDURATION.durationToPeriod(bucketduration);

    //
    // Compute bucketindex of lastbucket and compute bucketoffset
    //

    long bucketoffset;
    int lastbucket_index;
    if (lastbucket > 0) {
      long boundary = ADDDURATION.addPeriod(0, bucketperiod, dtz);

      lastbucket_index = 0;
      while (boundary <= lastbucket) {
        boundary = ADDDURATION.addPeriod(boundary, bucketperiod, dtz);
        lastbucket_index++;
      }
      bucketoffset = boundary - (lastbucket + 1);

    } else {
      long boundary = ADDDURATION.addPeriod(lastbucket, bucketperiod, dtz);

      lastbucket_index = -1;
      while (boundary < 0) {
        boundary = ADDDURATION.addPeriod(boundary, bucketperiod, dtz);
      }
      lastbucket_index--;
      bucketoffset = -(ADDDURATION.addPeriod(boundary, bucketperiod, dtz, -1) + 1);
    }

    //
    // Duration-Bucketize
    //

    List<GeoTimeSerie> bucketized = new ArrayList<GeoTimeSerie>();
    for (GeoTimeSerie gts : series) {

      GeoTimeSerie b = durationBucketize(gts, bucketperiod, dtz, bucketcount, lastbucket, lastbucket_index, bucketizer, maxbuckets, bucketizer instanceof Macro ? stack : null);
      b.getMetadata().getAttributes().put(DURATION_ATTRIBUTE_KEY, bucketduration);
      b.getMetadata().getAttributes().put(OFFSET_ATTRIBUTE_KEY, String.valueOf(bucketoffset));

      bucketized.add(b);
    }

    stack.push(bucketized);
    return stack;
  }

  private void aggregateAndSet(Object aggregator, GeoTimeSerie subgts, GeoTimeSerie bucketized, long bucketend, WarpScriptStack stack) throws WarpScriptException {
    Object[] aggregated;
    if (null != stack) {
      stack.push(subgts);
      Object res = stack.peek();

      if (res instanceof List) {
        aggregated = MACROMAPPER.listToObjects((List<Object>) stack.pop());
      } else {
        aggregated = MACROMAPPER.stackToObjects(stack);
      }

    } else {
      aggregated = (Object[]) ((WarpScriptBucketizerFunction) aggregator).apply(subgts, bucketend);
    }

    //
    // Only set value if it is non null
    //

    if (null != aggregated[3]) {
      GTSHelper.setValue(bucketized, bucketend, (long) aggregated[1], (long) aggregated[2], aggregated[3], false);
    }
  }


  public GeoTimeSerie durationBucketize(GeoTimeSerie gts, ADDDURATION.ReadWritablePeriodWithSubSecondOffset bucketperiod, DateTimeZone dtz, long bucketcount, long lastbucket, int lastbucket_index, Object aggregator, long maxbuckets, WarpScriptStack stack) throws WarpScriptException {

    long lastTick = GTSHelper.lasttick(gts);
    long firsTick = GTSHelper.firsttick(gts);
    int hint = Math.min(gts.size(), (int) (1.05 * (lastTick - firsTick) / ADDDURATION.addPeriod(0, bucketperiod, dtz)));

    GeoTimeSerie durationBucketized = gts.cloneEmpty(hint);

    //
    // We loop through the input GTS values in reverse order
    // We feed a buffer of values while traversing
    //

    GTSHelper.sort(gts);
    GeoTimeSerie subgts = gts.cloneEmpty();

    if (null != stack) {
      if (!(aggregator instanceof Macro)) {
        throw new WarpScriptException("Expected a macro as bucketizer.");
      }
    } else {
      if (!(aggregator instanceof WarpScriptBucketizerFunction)) {
        throw new WarpScriptException("Invalid bucketizer function.");
      }
    }

    // initialize bucketstart (start boundary), bucketend (end boundary) and bucketindex of current tick
    long bucketend = lastbucket;
    long bucketstart = ADDDURATION.addPeriod(lastbucket, bucketperiod, dtz, -1) + 1;
    int bucketindex = lastbucket_index;

    for (int i = gts.size() - 1; i >= 0; i--) {
      long tick = GTSHelper.tickAtIndex(gts, i);

      if (tick < bucketstart) {

        //
        // Break off the loop if bucketcount is exceeded (except if it is equal to 0)
        //

        if (bucketcount != 0 && lastbucket_index - bucketindex + 1 >= bucketcount) {
          break;
        }

        if (lastbucket_index - bucketindex + 2 > maxbuckets) {
          throw new WarpScriptException("Bucket count (" + String.valueOf(lastbucket_index - bucketindex + 2) + ") is exceeding maximum value of " + maxbuckets);
        }

        //
        // Call the aggregation function on the last batch
        //

        if (subgts.size() > 0) {
          aggregateAndSet(aggregator, subgts, durationBucketized, bucketend, stack);

          //
          // Reset buffer
          //

          subgts = GTSHelper.shrinkTo(subgts, 0);
        }
      }

      // update bucketend, bucketstart and bucketindex
      while (tick < bucketstart) {
        bucketend = bucketstart - 1;
        bucketstart = ADDDURATION.addPeriod(bucketstart, bucketperiod, dtz, -1);
        bucketindex--;
      }

      //  save value in subgts (if tick is not more recent than lastbucket)
      if (tick <= lastbucket) {
        GTSHelper.setValue(subgts, tick, GTSHelper.locationAtIndex(gts, i), GTSHelper.elevationAtIndex(gts, i), GTSHelper.valueAtIndex(gts, i), false);
      }
    }

    //
    // Aggregate on the last batch
    //

    if (subgts.size() > 0) {
      aggregateAndSet(aggregator, subgts, durationBucketized, bucketend, stack);
    }

    //
    // Set bucket parameters
    //

    GTSHelper.setLastBucket(durationBucketized, lastbucket);
    GTSHelper.setBucketSpan(durationBucketized, 1);
    GTSHelper.setBucketCount(durationBucketized, bucketcount == 0 ? durationBucketized.size() : Math.toIntExact(bucketcount));

    //
    // Reverse the order
    //

    GTSHelper.sort(durationBucketized);

    return durationBucketized;
  }
}
