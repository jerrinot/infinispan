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
package org.infinispan.loaders.memcached;

import org.infinispan.loaders.BaseCacheStoreTest;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.keymappers.UnsupportedKeyTypeException;
import org.infinispan.loaders.memcached.configuration.MemcachedStoreConfiguration;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "loaders.memcached.MemcachedCacheStoreTest")
public class MemcachedCacheStoreTest extends BaseCacheStoreTest {

    @Override
    protected CacheStore createCacheStore() throws Exception {
        CacheStore store = new MemcachedCacheStore();
        MemcachedCacheStoreConfig config = new MemcachedCacheStoreConfig();
        config.setPurgeSynchronously(true);
        store.init(config, getCache(), getMarshaller());
        store.start();
        return store;
    }

    @Override
    @Test(expectedExceptions = UnsupportedKeyTypeException.class)
    public void testLoadAndStoreMarshalledValues() throws CacheLoaderException {
        super.testLoadAndStoreMarshalledValues();
    }
}
