//
//   Copyright 2017  Cityzen Data
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

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

/**
 * Modify the current section name
 */
public class SECTION extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public SECTION(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object name = stack.pop();
    
    if (!(name instanceof String)) {
      throw new WarpScriptException(getName() + " expected a section name as a string.");
    }
    
    stack.setAttribute(WarpScriptStack.ATTRIBUTE_SECTION_NAME, name.toString());
    
    return stack;
  }
}