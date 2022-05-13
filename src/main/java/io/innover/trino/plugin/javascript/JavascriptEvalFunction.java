/*
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

package io.innover.trino.plugin.javascript;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlNullable;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavascriptEvalFunction
{
    private JavascriptEvalFunction()
    {}

    @ScalarFunction("javascript_eval")
    @Description("Returns TRUE if the argument is NULL")
    @SqlType(StandardTypes.VARCHAR)
    // "function sum(a,b){ return a + b;}"
    public static Slice eval(@SqlNullable @SqlType(StandardTypes.VARCHAR) Slice slice)
    {
        // TODO: exception handling
        // TODO: timeout
        // TODO; better output typing
        // TODO: externalize script engine creation
        // TODO: cqche function
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        try {
            // evaluate JavaScript code from String
            engine.eval(slice.toStringUtf8());
            return Slices.utf8Slice(((Invocable) engine).invokeFunction("sum", 1, 1).toString());
        }
        catch (ScriptException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
