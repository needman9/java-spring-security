/* Copyright 2004 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.acegisecurity;

/**
 * Represents the interface of a secured object.
 *
 * @author Ben Alex
 * @version $Id$
 */
public interface ITargetObject {
    //~ Methods ================================================================

    public Integer computeHashCode(String input);

    public int countLength(String input);

    public String makeLowerCase(String input);

    public String makeUpperCase(String input);

    public String publicMakeLowerCase(String input);
}
