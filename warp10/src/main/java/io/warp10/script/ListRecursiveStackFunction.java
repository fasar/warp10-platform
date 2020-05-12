//
//   Copyright 2019  SenX S.A.S.
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

package io.warp10.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class to inherit when a function can be called on a single element or a list of elements or list of list of element, etc.
 * This class can be used, for instance, for a function working on GTSs, GTSEncoders and lists thereof, including lists with mixed types.
 * The idea is to generate a function using the parameters on the stack and then to apply this function on the element
 * or the list of elements.
 * This is similar to GTSStackFunction for GTSs but it is a tad faster if the function uses some parameters as this
 * implementation does not use a Map for parameters.
 * This is similar to ElementOrListStackFunction but it is faster has it checks for List after the other cases which
 * it optimal in almost all the cases where the number of elements is higher than the nesting depth of the lists.
 */
public abstract class ListRecursiveStackFunction extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  /**
   * Interface defining the function to be generated which is applied to each element.
   * This method should check the type of the given element and throw a WarpScriptException if the element is of
   * unexpected type.
   */
  public abstract class ElementStackFunction {
    /**
     * Apply the function on a single element (not a List).
     * If it can't handle the element, MUST call unhandled.
     *
     * @param element The element to apply the function on.
     * @return The result of the application of the function.
     * @throws WarpScriptException If an error occurs.
     */
    public abstract Object applyOnElement(Object element) throws WarpScriptException;

    /**
     * Method to call when applyOnElement cannot handle the type of the given element.
     *
     * @param o
     * @return An ArrayList where each element is the result of applyOnElement.
     * @throws WarpScriptException If not given a list.
     */
    public ArrayList<Object> unhandled(Object o) throws WarpScriptException {
      if (o instanceof List) {
        List elements = (List) o;
        ArrayList<Object> result = new ArrayList<Object>(elements.size());
        for (Object e: elements) {
          result.add(applyOnElement(e));
        }
        return result;
      } else {
        throw new WarpScriptException(getUnhandledErrorMessage());
      }
    }
  }

  /**
   * Generates the function to be applied on the element(s) using the parameters on the stack.
   *
   * @param stack The stack to be used for parameters retrieval.
   * @return An ElementStackFunction which will be applied on each given elements or on the given element.
   * @throws WarpScriptException
   */
  public abstract ElementStackFunction generateFunction(WarpScriptStack stack) throws WarpScriptException;

  /**
   * Return the error message when the given element cannot be handled by this function.
   *
   * @return An error message.
   */
  public abstract String getUnhandledErrorMessage();

  public ListRecursiveStackFunction(String name) {
    super(name);
  }

  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    ElementStackFunction function = generateFunction(stack);

    Object o = stack.pop();

    stack.push(function.applyOnElement(o));

    return stack;
  }
}

