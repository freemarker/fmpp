/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fmpp.tdd;

/**
 * Callbacks that let you control the behaviour of TDD expression evaluation.
 */
public interface EvaluationEnvironment {
    /**
     * The code of event that indicates that we have started to evaluate the
     * value in a key:value pair.
     */
    int EVENT_ENTER_HASH_KEY = 1;
    
    /**
     * The code of event that indicates that we have finished to evaluate the
     * value in a key:value pair.
     */
    int EVENT_LEAVE_HASH_KEY = -1;

    /**
     * The code of event that indicates that we have started to evaluate the
     * parameter list in a function call.
     */
    int EVENT_ENTER_FUNCTION_PARAMS = 3;
    
    /**
     * The code of event that indicates that we have finished to evaluate the
     * parameter list in a function call.
     */
    int EVENT_LEAVE_FUNCTION_PARAMS = -3;

    /**
     * The code of event that indicates that we have started to evaluate the
     * items in a sequence. This does not include function call parameter lists.
     */
    int EVENT_ENTER_SEQUENCE = 4;
    
    /**
     * The code of event that indicates that we have finished to evaluate the
     * items in a sequence.
     */
    int EVENT_LEAVE_SEQUENCE = -4;


    /**
     * The code of event that indicates that we have started to evaluate the
     * items in a hash.
     */
    int EVENT_ENTER_HASH = 5;
    
    /**
     * The code of event that indicates that we have finished to evaluate the
     * items in a sequence.
     */
    int EVENT_LEAVE_HASH = -5;
    
    Object RETURN_SKIP = new Object();

    Object RETURN_FRAGMENT = new Object();
    
    /**
     * Evaluates the function call. This method may simply returns its
     * parameter, which means that the function was not resolved, and thus the
     * function call will be availble for further interpretation in the result
     * of the TDD expression evaluation.
     * 
     * @param fc the function call to evaluate.
     *   
     * @return the return value of the function call. During the evaluation of
     *     a TDD expression, function calls will be replaced with their return
     *     values. 
     *     If the return value is a {@link FunctionCall} object, it will not be
     *     evaluated again. This way, the final result of a TDD expression
     *     evaluation can contain {@link FunctionCall} objects.
     */
    Object evalFunctionCall(FunctionCall fc, Interpreter ip) throws Exception;
    
    /**
     * Notifies about an event during expression evaluation.
     * 
     * @param event An <code>EVENT_...</code> constant. Further events may will
     *     be added later, so the implementation must silently ignore events
     *     that it does not know. It is guaranteed that for each
     *     <code>EVENT_ENTER_...</code> event there will be an
     *     <code>EVENT_LEAVE_...</code> event later, except if
     *     <code>notifyContextChange</code> has thrown exception during handling
     *     <code>EVENT_ENTER_...</code>, in which case it is guaranteed that
     *     there will be no corresponding <code>EVENT_LEAVE_...</code> event.
     * @param ip the {@link Interpreter} instance that evaluates the text.
     *      The value returned by {@link Interpreter#getPosition()} will be
     *      the position in the text where the this even has been created:
     *      <ul>
     *        <li>{@link #EVENT_ENTER_HASH_KEY}: points the first character
     *            of the <i>value</i> of the key:<i>value</i> pair.
     *        <li>{@link #EVENT_ENTER_SEQUENCE}, {@link #EVENT_ENTER_HASH}, and
     *            {@link #EVENT_ENTER_FUNCTION_PARAMS}: points the first
     *            character after the <tt>[</tt> and <tt>(</tt> respectively.  
     *        <li>{@link #EVENT_LEAVE_SEQUENCE}, {@link #EVENT_LEAVE_HASH}, and
     *            {@link #EVENT_LEAVE_FUNCTION_PARAMS}: points the
     *            terminating character, that is, the <tt>]</tt> or <tt>)</tt>
     *            or the character after the end of the string.
     *      </ul>  
     * @param name For {@link #EVENT_ENTER_HASH_KEY} and
     *     {@link #EVENT_ENTER_FUNCTION_PARAMS}, the name of the hash key or
     *     function. It is <code>null</code> otherwise.
     * @param extra Even specific extra information.
     *     <ul>
     *       <li>For {@link #EVENT_ENTER_HASH}, {@link #EVENT_LEAVE_HASH}, 
     *           {@link #EVENT_ENTER_SEQUENCE}, {@link #EVENT_LEAVE_SEQUENCE} it
     *           is the <code>Map</code> or <code>List</code> that is being
     *           built by the hash or sequence. It's OK to modify this
     *           <code>Map</code> or <code>List</code>.
     *       <li>For other events it's
     *           value is currently <code>null</code>.  
     *     </ul>
     * @return return The allowed return values and their meaning depends on
     *     the event. But return value <code>null</code> always means
     *     "do nothing special". The currently defiend non-<code>null</code>
     *     return values for the events:
     *     <ul>
     *       <li>{@link #EVENT_ENTER_HASH_KEY}:
     *          <ul>
     *            <li>{@link #RETURN_SKIP}: Skip the key:value
     *                pair. That is, the key:value pair will not be added to
     *                the map. The value expression will not be evaluated.
     *            <li>{@link #RETURN_FRAGMENT}: The value of the key:value pair
     *                will be the {@link Fragment} that stores the value
     *                expression. The value expression will not be evaluated.
     *                However, if the value is implicit boolean 
     *                <code>true</code>, (i.e. you omit the value) then
     *                {@link #RETURN_FRAGMENT} has no effect. 
     *          </ul>
     *       <li>
     *       <li>{@link #EVENT_ENTER_HASH} if the hash uses <tt>{</tt> and
     *           <tt>}</tt>):
     *          <ul>
     *            <li>{@link #RETURN_FRAGMENT}: The value of the hash will be
     *                the {@link Fragment} that stores the hash expression.
     *                The hash expression will not be evaluated.
     *          </ul>
     *       </li>
     *     </ul>
     */
    Object notify(int event, Interpreter ip, String name, Object extra)
            throws Exception;
}
