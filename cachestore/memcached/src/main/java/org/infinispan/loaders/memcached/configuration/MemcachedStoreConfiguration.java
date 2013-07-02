/*
* C2B2, The Leading Independent Middleware Experts.
* Copyright 2013, C2B2 Consulting Limited.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.infinispan.loaders.memcached.configuration;

import org.infinispan.configuration.BuiltBy;
import org.infinispan.configuration.cache.*;
import org.infinispan.loaders.memcached.MemcachedCacheStoreConfig;
import org.infinispan.util.TypedProperties;

@BuiltBy(MemcachedStoreConfigurationBuilder.class)
public class MemcachedStoreConfiguration extends AbstractStoreConfiguration implements LegacyLoaderAdapter<MemcachedCacheStoreConfig> {

    private String hostname;
    private int port;

    public MemcachedStoreConfiguration(String hostname, int port, boolean purgeOnStartup, boolean purgeSynchronously, int purgerThreads, boolean fetchPersistentState,
                                       boolean ignoreModifications, TypedProperties properties, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore) {

        super(purgeOnStartup, purgeSynchronously, purgerThreads, fetchPersistentState, ignoreModifications, properties,
                async, singletonStore);
        this.hostname = hostname;
        this.port = port;
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }

    @Override
    public MemcachedCacheStoreConfig adapt() {
        MemcachedCacheStoreConfig config = new MemcachedCacheStoreConfig();
        LegacyConfigurationAdaptor.adapt(this, config);

        config.setHostname(hostname);
        config.setPort(port);

        return config;
    }
}
