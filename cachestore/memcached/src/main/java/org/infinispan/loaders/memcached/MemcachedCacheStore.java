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

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.keymappers.DefaultTwoWayKey2StringMapper;
import org.infinispan.loaders.keymappers.TwoWayKey2StringMapper;
import org.infinispan.loaders.keymappers.UnsupportedKeyTypeException;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.util.InfinispanCollections;
import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.infinispan.util.logging.LogFactory;
import org.infinispan.util.logging.Log;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

@CacheLoaderMetadata(configurationClass = MemcachedCacheStoreConfig.class)
public class MemcachedCacheStore extends AbstractCacheStore {

    private Log log = LogFactory.getLog(MemcachedCacheStore.class);

    private MemcachedClient client;
    private TwoWayKey2StringMapper keyMapper;
    private Set<Object> keys = new ConcurrentHashSet<Object>();

    private String hostname;
    private int port;

    private MemcachedCacheStoreConfig config;

    @Override
    public void init(CacheLoaderConfig clc, Cache<?, ?> cache, StreamingMarshaller m)
            throws CacheLoaderException {
        super.init(clc, cache, m);
        this.config = (MemcachedCacheStoreConfig) clc;
    }

    @Override
    public void start() throws CacheLoaderException {
        keyMapper = new DefaultTwoWayKey2StringMapper();
        hostname = config.getHostname();
        port = config.getPort();

        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder().setProtocol(ConnectionFactoryBuilder.Protocol.BINARY).build();
        try {
            client = new MemcachedClient(connectionFactory, Arrays.asList(new InetSocketAddress(hostname, port)));
        } catch (IOException e) {
            throw new CacheLoaderException("Error while creating memcached client.", e);
        }
        super.start();
    }

    @Override
    public void stop() throws CacheLoaderException {
        client.shutdown();
        super.stop();
    }

    @Override
    protected void purgeInternal() throws CacheLoaderException {
        log.warn("Explicit purging not implemented yet"); //TODO: This can lead to OOM as expired keys are evicted only when accessed
    }

    @Override
    public void store(InternalCacheEntry entry) throws CacheLoaderException {
        Object key = entry.getKey();
        keys.add(key);
        String stringKey = hashKey(key);
        try {
            client.set(stringKey, Integer.MAX_VALUE, marshall(entry));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        }
    }

    @Override
    public void toStream(ObjectOutput out) throws CacheLoaderException {
        //TODO: It should not loadAll() as it can throw OOM
        try {
            Set<InternalCacheEntry> loadAll = loadAll();
            for (InternalCacheEntry entry : loadAll) {
                getMarshaller().objectToObjectStream(entry, out);
            }
            getMarshaller().objectToObjectStream(null, out);
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        }
    }

    @Override
    public void fromStream(ObjectInput in) throws CacheLoaderException {
        try {
            while (true) {
                InternalCacheEntry entry = (InternalCacheEntry) getMarshaller().objectFromObjectStream(in);
                if (entry == null)
                    break;
                store(entry);
                keys.add(entry.getKey());
            }
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        } catch (ClassNotFoundException e) {
            throw new CacheLoaderException(e);
        } catch (InterruptedException ie) {
            if (log.isTraceEnabled())
                log.trace("Interrupted while reading from stream");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void clear() throws CacheLoaderException {
        try {
            keys.clear();
            client.flush().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new CacheLoaderException(e);
        }

    }

    @Override
    public boolean remove(Object key) throws CacheLoaderException {
        String stringKey = hashKey(key);
        boolean exist = (client.get(stringKey) != null);
        if (exist) {
            keys.remove(key);
            client.delete(stringKey);
        }
        return exist;
    }

    @Override
    public InternalCacheEntry load(Object key) throws CacheLoaderException {
        try {
            String stringkey = hashKey(key);
            byte[] buffer = (byte[]) client.get(stringkey);
            InternalCacheEntry entry =  getNotExpiredEntryFromByteArray(key, buffer);
            if (entry == null) {
                keys.remove(key);
            }
            return entry;
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        } catch (ClassNotFoundException e) {
            throw  new CacheLoaderException(e);
        }

    }

    private InternalCacheEntry getNotExpiredEntryFromByteArray(Object key, byte[] buffer) throws IOException, ClassNotFoundException {
        InternalCacheEntry entry = unmarshall(buffer, key);
        if (entry == null || entry.isExpired(System.currentTimeMillis())) {
            return null;
        } else {
            return entry;
        }
    }

    @Override
    public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
        Set<InternalCacheEntry> result = new HashSet<InternalCacheEntry>();
        try {
            Map<String, Object> objects = client.getBulk(new ConvertingKeyIterator(keys.iterator()));
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                Object objectKey = keyMapper.getKeyMapping(entry.getKey());
                InternalCacheEntry object = getNotExpiredEntryFromByteArray(objectKey, (byte[]) entry.getValue());
                if (object == null) {
                    keys.remove(objectKey);
                } else {
                    result.add(object);
                }
            }
            return result;
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        } catch (ClassNotFoundException e) {
            throw new CacheLoaderException(e);
        }
    }

    @Override
    public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {
        Set<InternalCacheEntry> result = new HashSet<InternalCacheEntry>();
        Iterator<Object> keyIterator = keys.iterator();
        try {
            while (keyIterator.hasNext() && result.size() < numEntries) {
                Object key = keyIterator.next();
                byte[] buffer = (byte[]) client.get(hashKey(key));
                InternalCacheEntry entry = getNotExpiredEntryFromByteArray(key, buffer);
                if (entry == null) {
                    keyIterator.remove();
                } else {
                    result.add(entry);
                }
            }
            return result;
        } catch (IOException e) {
            throw new CacheLoaderException(e);
        } catch (ClassNotFoundException e) {
            throw new CacheLoaderException(e);
        }
    }

    @Override
    public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {
        return (keysToExclude == null || keysToExclude.isEmpty()) ? Collections.unmodifiableSet(keys) :
                InfinispanCollections.difference(keys, keysToExclude);
    }

    @Override
    public Class<? extends CacheLoaderConfig> getConfigurationClass() {
        return MemcachedCacheStoreConfig.class;
    }

    private byte[] marshall(InternalCacheEntry entry) throws IOException, InterruptedException {
        return getMarshaller().objectToByteBuffer(entry.toInternalCacheValue());
    }

    private InternalCacheEntry unmarshall(Object o, Object key) throws IOException,
            ClassNotFoundException {
        if (o == null)
            return null;
        byte b[] = (byte[]) o;
        InternalCacheValue v = (InternalCacheValue) getMarshaller().objectFromByteBuffer(b);
        return v.toInternalCacheEntry(key);
    }

    private String hashKey(Object key) throws UnsupportedKeyTypeException {
        if (!keyMapper.isSupportedType(key.getClass())) {
            throw new UnsupportedKeyTypeException(key);
        }
        return keyMapper.getStringMapping(key);
    }

    private class ConvertingKeyIterator implements Iterator<String> {
        private Iterator<Object> iterator;

        public ConvertingKeyIterator(Iterator<Object> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            try {
                return hashKey(iterator.next());
            } catch (UnsupportedKeyTypeException e) {
                throw new RuntimeException(e); //TODO: Change me. I'm ugly!
            }
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
