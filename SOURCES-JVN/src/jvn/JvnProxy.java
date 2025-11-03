package jvn;

import annotation.Read;
import annotation.Write;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JvnProxy implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;
    private JvnObject jvnObject;

    private JvnProxy(JvnObject jvnObject) {
        this.jvnObject = jvnObject;
    }

    public static Object newInstance(Object realObject, Class<?>... interfaces) {
        if (interfaces == null || interfaces.length == 0) {
            interfaces = realObject.getClass().getInterfaces();
        }
        return Proxy.newProxyInstance(
                realObject.getClass().getClassLoader(),
                interfaces,
                new JvnProxy((JvnObject) realObject));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        System.out.println("JvnProxy: Intercepted call to method '" + method.getName() + "'");

        if (method.getDeclaringClass().equals(JvnObject.class)) {
            System.out.println("JvnProxy: Invoking JVN method '" + method.getName() + "'");
            return method.invoke(jvnObject, args);
        }

        boolean hasReadAnnotation = method.isAnnotationPresent(Read.class);
        boolean hasWriteAnnotation = method.isAnnotationPresent(Write.class);

        try {
            if (hasReadAnnotation) {
                System.out.println("JvnProxy: @Read annotation detected. Acquiring read lock.");
                jvnObject.jvnLockRead();
            } else if (hasWriteAnnotation) {
                System.out.println("JvnProxy: @Write annotation detected. Acquiring write lock.");
                jvnObject.jvnLockWrite();
            }

            Object sharedObject = jvnObject.jvnGetSharedObject();
            if (sharedObject == null) {
                throw new JvnException("FATAL: L'objet partagé est null même après le verrouillage !");
            }

            result = method.invoke(sharedObject, args);

        } finally {
            if (hasReadAnnotation || hasWriteAnnotation) {
                System.out.println("JvnProxy: Releasing lock for method '" + method.getName() + "'.");
                jvnObject.jvnUnLock();
            }
        }

        return result;
    }
}