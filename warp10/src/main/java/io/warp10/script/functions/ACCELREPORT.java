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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.standalone.StandaloneAcceleratedStoreClient;

public class ACCELREPORT extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private static final String KEY_ACCELERATED = "accelerated";
  private static final String KEY_STATUS = "status";
  private static final String KEY_CACHE = "cache";
  private static final String KEY_PERSIST = "persist";
  private static final String KEY_CHUNK_COUNT = "chunkcount";
  private static final String KEY_CHUNK_SPAN = "chunkspan";
  
  private static final String KEY_DEFAULTS_WRITE = "defaults.write";
  private static final String KEY_DEFAULTS_DELETE = "defaults.delete";
  private static final String KEY_DEFAULTS_READ = "defaults.read";
  
  public ACCELREPORT(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    Object status = stack.getAttribute(StandaloneAcceleratedStoreClient.ATTR_REPORT);
    
    Map<Object,Object> report = new HashMap<Object,Object>();
    
    report.put(KEY_CACHE, StandaloneAcceleratedStoreClient.isCache());
    report.put(KEY_PERSIST, StandaloneAcceleratedStoreClient.isPersist());
    report.put(KEY_STATUS, StandaloneAcceleratedStoreClient.isInstantiated());
    report.put(KEY_ACCELERATED, StandaloneAcceleratedStoreClient.accelerated());
    report.put(KEY_CHUNK_COUNT, (long) StandaloneAcceleratedStoreClient.getChunkCount());
    report.put(KEY_CHUNK_SPAN, StandaloneAcceleratedStoreClient.getChunkSpan());
    List<String> defaults = new ArrayList<String>(2);
    if (StandaloneAcceleratedStoreClient.getDefaultWriteNocache()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOCACHE);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.CACHE);      
    }
    if (StandaloneAcceleratedStoreClient.getDefaultWriteNopersist()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOPERSIST);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.PERSIST);            
    }
    report.put(KEY_DEFAULTS_WRITE, defaults);
    
    defaults = new ArrayList<String>(2);
    if (StandaloneAcceleratedStoreClient.getDefaultReadNocache()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOCACHE);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.CACHE);      
    }
    if (StandaloneAcceleratedStoreClient.getDefaultReadNopersist()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOPERSIST);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.PERSIST);            
    }
    report.put(KEY_DEFAULTS_READ, defaults);
    
    defaults = new ArrayList<String>(2);
    if (StandaloneAcceleratedStoreClient.getDefaultDeleteNocache()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOCACHE);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.CACHE);      
    }
    if (StandaloneAcceleratedStoreClient.getDefaultDeleteNopersist()) {
      defaults.add(StandaloneAcceleratedStoreClient.NOPERSIST);
    } else {
      defaults.add(StandaloneAcceleratedStoreClient.PERSIST);            
    }
    report.put(KEY_DEFAULTS_DELETE, defaults);
    
    stack.push(report);

    return stack;
  }

}
