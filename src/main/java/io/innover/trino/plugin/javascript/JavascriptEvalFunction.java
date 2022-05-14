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
import io.trino.spi.block.Block;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlType;
import io.trino.spi.function.TypeParameter;
import io.trino.spi.type.CharType;
import io.trino.spi.type.DecimalType;
import io.trino.spi.type.Int128;
import io.trino.spi.type.RowType;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.TimeType;
import io.trino.spi.type.TimestampType;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.Chars.padSpaces;
import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.Decimals.isLongDecimal;
import static io.trino.spi.type.Decimals.isShortDecimal;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.spi.type.Timestamps.PICOSECONDS_PER_NANOSECOND;
import static io.trino.spi.type.Timestamps.roundDiv;
import static io.trino.spi.type.TinyintType.TINYINT;
import static io.trino.type.DateTimes.toLocalDateTime;
import static io.trino.type.DateTimes.toZonedDateTime;
import static io.trino.type.UnknownType.UNKNOWN;
import static java.lang.Float.intBitsToFloat;
import static java.lang.Math.toIntExact;

public class JavascriptEvalFunction
{
    private JavascriptEvalFunction()
    {}

    @ScalarFunction("javascript_eval")
    @TypeParameter("V")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice eval(
            @TypeParameter("V") RowType rowType,
            @SqlType(StandardTypes.VARCHAR) Slice slice,
            @SqlType("V") Block row)
    {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        try {
            // evaluate JavaScript code from String
            engine.eval(slice.toStringUtf8());
            return Slices.utf8Slice(((Invocable) engine).invokeFunction("udf", mapArgs(rowType, row)).toString());
        }
        catch (ScriptException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] mapArgs(RowType rowType, Block block)
    {
        int positionCount = block.getPositionCount();
        List<Type> types = rowType.getTypeParameters();
        Object[] output = new Object[positionCount];
        for (int position = 0; position < positionCount; position++) {
            Type type = types.get(0);
            if (type.equals(UNKNOWN)) {
                output[position] = null;
            }
            if (type.equals(BOOLEAN)) {
                output[position] = type.getBoolean(block, position);
            }
            if (type.equals(TINYINT) || type.equals(SMALLINT) || type.equals(INTEGER) || type.equals(BIGINT)) {
                output[position] = type.getLong(block, position);
            }
            if (type.equals(REAL)) {
                output[position] = intBitsToFloat(toIntExact(type.getLong(block, position)));
            }
            if (type.equals(DOUBLE)) {
                output[position] = type.getDouble(block, position);
            }
            if (type.equals(DATE)) {
                output[position] = LocalDate.ofEpochDay(type.getLong(block, position));
            }
            if (type instanceof TimestampWithTimeZoneType) {
                output[position] = toZonedDateTime(((TimestampWithTimeZoneType) type), block, position);
            }
            if (type instanceof TimestampType) {
                output[position] = toLocalDateTime(((TimestampType) type), block, position);
            }
            if (type instanceof TimeType) {
                output[position] = toLocalTime(type.getLong(block, position));
            }
            if (isShortDecimal(type)) {
                int scale = ((DecimalType) type).getScale();
                output[position] = BigDecimal.valueOf(type.getLong(block, position), scale);
            }
            if (isLongDecimal(type)) {
                int scale = ((DecimalType) type).getScale();
                output[position] = new BigDecimal(((Int128) type.getObject(block, position)).toBigInteger(), scale);
            }
            if (type instanceof VarcharType) {
                output[position] = type.getSlice(block, position).toStringUtf8();
            }
            if (type instanceof CharType) {
                CharType charType = (CharType) type;
                output[position] = padSpaces(type.getSlice(block, position), charType).toStringUtf8();
            }
            // TODO: recursive call to map objects
            // TODO: check for supported types
        }
        return output;
    }

    private static LocalTime toLocalTime(long value)
    {
        long nanoOfDay = roundDiv(value, PICOSECONDS_PER_NANOSECOND);
        return LocalTime.ofNanoOfDay(nanoOfDay);
    }
}
