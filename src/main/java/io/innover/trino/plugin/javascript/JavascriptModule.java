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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import io.trino.spi.NodeManager;
import io.trino.spi.type.TypeManager;

import static io.airlift.configuration.ConfigBinder.configBinder;
import static java.util.Objects.requireNonNull;

public class JavascriptModule
        implements Module
{
    private final NodeManager nodeManager;
    private final TypeManager typeManager;

    public JavascriptModule(NodeManager nodeManager, TypeManager typeManager)
    {
        this.nodeManager = requireNonNull(nodeManager, "nodeManager is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(NodeManager.class).toInstance(nodeManager);
        binder.bind(TypeManager.class).toInstance(typeManager);

        binder.bind(JavascriptConnector.class).in(Scopes.SINGLETON);
        binder.bind(JavascriptMetadata.class).in(Scopes.SINGLETON);
        binder.bind(JavascriptSplitManager.class).in(Scopes.SINGLETON);
        binder.bind(JavascriptRecordSetProvider.class).in(Scopes.SINGLETON);
        configBinder(binder).bindConfig(JavascriptConfig.class);
    }
}
