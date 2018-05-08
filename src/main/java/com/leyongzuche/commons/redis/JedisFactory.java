package com.leyongzuche.commons.redis;


import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.Pool;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author :  pengqingsong
 * Date : 03/03/2017 15:21
 * Description : 封装getResourece,close这些细节，可以直接使用各种命令。
 * 建议使用getJedis1，因为javassist的实现不是采用代理完成，而getJedis0是采用代理实现。
 * 性能的对比可以参考JedisFactoryTest
 * Test :
 */
public final class JedisFactory {


    public static Jedis getJedis(String host, int port, int connectionTimeout,
                                 int maxIdle, int maxTotal, int maxWaitMillis,
                                 boolean testOnBorrow, boolean blockWhenExhausted) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        jedisPoolConfig.setTestOnBorrow(testOnBorrow);
        jedisPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
        Jedis jedis = getJedis0(jedisPoolConfig, host, port, connectionTimeout);
        return jedis;
    }


    private static Jedis getJedis0(final JedisPoolConfig jedisPoolConfig, final String host, final int port, final int connectionTimeout) {

        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Jedis.class);
        enhancer.setCallback(new InvocationHandler() {

            private JedisPool pool = new JedisPool(jedisPoolConfig, host, port, connectionTimeout);

            private Exception ex = null;

            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                Jedis jedis = getJedis();
                if (jedis == null) {
                    throw ex;
                }

                Object result = null;
                Exception invokeEx = null;
                try {
                    result = method.invoke(jedis, objects);
                } catch (Exception e) {
                    invokeEx = e;
                }

                try {
                    jedis.close();
                } catch (Throwable e) {
                }

                if (invokeEx != null) {
                    throw invokeEx;
                }
                return result;
            }

            private Jedis getJedis() {
                Jedis jedis = null;
                for (int i = 0; i < 3; i++) {
                    jedis = getJedisFromPool();
                    if (jedis != null) {
                        break;
                    }
                    if (jedis == null) {
                        pool.destroy();
                        pool = new JedisPool(jedisPoolConfig, host, port, connectionTimeout);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {

                    }
                }
                return jedis;
            }

            private Jedis getJedisFromPool() {
                Jedis jedis = null;
                for (int i = 0; i < 3; i++) {
                    try {
                        jedis = pool.getResource();
                        ex = null;
                        break;
                    } catch (Exception e) {
                        ex = e;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {

                    }
                }
                return jedis;
            }
        });
        return (Jedis) enhancer.create();
    }

    private static Jedis getJedis1(final Pool<Jedis> jedisPool) throws NotFoundException, CannotCompileException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(Jedis.class));
        CtClass jedisCtClass = pool.get(Jedis.class.getName());
        CtClass jedisPoolCtClass = pool.get(Pool.class.getName());

        CtClass proxyCtClass = pool.makeClass("com.rocky.commons.redis.RockyJedis");
        proxyCtClass.setSuperclass(jedisCtClass);

        proxyCtClass.addField(CtField.make("private redis.clients.util.Pool pool;", proxyCtClass));

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{jedisPoolCtClass}, proxyCtClass);
        ctConstructor.callsSuper();
        ctConstructor.setBody("{ $0.pool = $1;}");
        proxyCtClass.addConstructor(ctConstructor);

        CtClass stringCtClass = pool.get(String.class.getName());
        CtMethod jedisSetMethod = jedisCtClass.getDeclaredMethod("set", new CtClass[]{stringCtClass, stringCtClass});
        CtMethod cm = new CtMethod(jedisSetMethod.getReturnType(), jedisSetMethod.getName(), jedisSetMethod.getParameterTypes(), proxyCtClass);
        StringBuilder setMethodBody = new StringBuilder();
        setMethodBody.append("{");
        setMethodBody.append("    redis.clients.jedis.Jedis jedis = (redis.clients.jedis.Jedis)$0.pool.getResource();");
        setMethodBody.append("    java.lang.String result = ").append("jedis.").append(jedisSetMethod.getName()).append("($$);");
        setMethodBody.append("    try {");
        setMethodBody.append("        jedis.close();");
        setMethodBody.append("    } catch (Throwable e) {}");
        setMethodBody.append("    return result;");
        setMethodBody.append("}");
        cm.setBody(setMethodBody.toString());
        proxyCtClass.addMethod(cm);
        Class rockyJedisClazz = proxyCtClass.toClass();
        Constructor constructor = rockyJedisClazz.getConstructor(Pool.class);
        Jedis jedis = (Jedis) constructor.newInstance(jedisPool);
        return jedis;
    }
}
