package com.inomera.telco.commons.kafkakryo;

import com.esotericsoftware.kryo.Kryo;
import lombok.RequiredArgsConstructor;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class ThreadLocalKryoFactory implements KryoFactory {
    private static final ThreadLocal<Kryo> KRYO_HOLDER = new ThreadLocal<>();

    private final KryoClassRegistry kryoClassRegistry;

    @Override
    public Kryo getKryo() {
        final Kryo cachedKryo = KRYO_HOLDER.get();
        if (cachedKryo != null) {
            return cachedKryo;
        }

        final Kryo newKryo = new Kryo(null);
        kryoClassRegistry.registerClasses(newKryo);
        KRYO_HOLDER.set(newKryo);

        return newKryo;
    }
}
