package com.inomera.telco.commons.kafkakryo;

import com.esotericsoftware.kryo.Kryo;

/**
 * @author Serdar Kuzucu
 */
public interface KryoClassRegistry {
    void registerClasses(Kryo kryo);
}
