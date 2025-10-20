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


        // Check if the method is from JvnObject interface
        if (method.getDeclaringClass().equals(JvnObject.class)) {
            System.out.println("JvnProxy: Invoking JVN method '" + method.getName() + "'");
            return method.invoke(jvnObject, args);
        }

        // Récupérer l'objet partagé
        Object sharedObject = jvnObject.jvnGetSharedObject();
        if (sharedObject == null) {
            throw new JvnException("L'objet partagé est null");
        }

        // Chercher la méthode sur l'objet d'origine pour trouver les annotations
        Method originalMethod = null;
        try {
            originalMethod = sharedObject.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            System.out.println("JvnProxy: Warning - Method not found on shared object");
        }

        boolean hasReadAnnotation = originalMethod != null && originalMethod.isAnnotationPresent(Read.class);
        boolean hasWriteAnnotation = originalMethod != null && originalMethod.isAnnotationPresent(Write.class);

        if (hasReadAnnotation) {
            System.out.println("JvnProxy: @Read annotation detected. Acquiring read lock.");
            jvnObject.jvnLockRead();
        } else if (hasWriteAnnotation) {
            System.out.println("JvnProxy: @Write annotation detected. Acquiring write lock.");
            jvnObject.jvnLockWrite();
        }

        try {
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