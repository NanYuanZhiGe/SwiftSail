package com.nyzg.geo.conf;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Configuration
public class KryoSerializer {
    private final GenericObjectPool<Kryo> pool;

    public KryoSerializer() {
        GenericObjectPoolConfig<Kryo> config = new GenericObjectPoolConfig<>();

        config.setMaxIdle(20);
        config.setMinIdle(5);
        config.setMaxTotal(50);
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        pool = new GenericObjectPool<>(new KryoFactory(), config);
    }

    private static class KryoFactory extends BasePooledObjectFactory<Kryo> {
        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            registerClasses(kryo);
            kryo.setReferences(false);
            return kryo;
        }

        @Override
        public PooledObject<Kryo> wrap(Kryo kryo) {
            return new DefaultPooledObject<>(kryo);
        }


        @Override
        public boolean validateObject(PooledObject<Kryo> p) {
            return p.getObject() != null;
        }
    }

    private static void registerClasses(Kryo kryo) {
        kryo.register(java.util.ArrayList.class, 10);
        kryo.register(java.util.HashMap.class, 11);
        kryo.register(byte[].class, 12);
        kryo.register(String.class, 13);
        kryo.register(Void.class, 21);
    }


    public byte[] serialize(Object data) {
        if (data == null) return null;

        Kryo kryo = null;
        try {
            kryo = pool.borrowObject(); // 从池借出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);

            kryo.writeClassAndObject(output, data);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo serialize error", e);
        } finally {
            if (kryo != null) {
                pool.returnObject(kryo);
            }
        }
    }

    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null) return null;

        Kryo kryo = null;
        try {
            kryo = pool.borrowObject(); // 从池借出
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            Input input = new Input(bais);

            Object obj = kryo.readClassAndObject(input);
            return clazz.cast(obj);
        } catch (Exception e) {
            throw new RuntimeException("Kryo deserialize error", e);
        } finally {
            if (kryo != null) {
                pool.returnObject(kryo);
            }
        }
    }

    @PreDestroy
    public void preDestroy() {
        pool.close();
    }
}
