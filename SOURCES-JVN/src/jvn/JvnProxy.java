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

        // Check if the method is from JvnObject interface
        if (method.getDeclaringClass().equals(JvnObject.class)) {
            System.out.println("JvnProxy: Invoking JVN method '" + method.getName() + "'");
            return method.invoke(jvnObject, args);
        }

        // 1. CHERCHER LES ANNOTATIONS SUR LA MÉTHODE DE L'INTERFACE (que nous avons)
        boolean hasReadAnnotation = method.isAnnotationPresent(Read.class);
        boolean hasWriteAnnotation = method.isAnnotationPresent(Write.class);

        try {
            // 2. VERROUILLER D'ABORD
            // (C'est cet appel qui va télécharger l'objet si sharedObject est null)
            if (hasReadAnnotation) {
                System.out.println("JvnProxy: @Read annotation detected. Acquiring read lock.");
                jvnObject.jvnLockRead();
            } else if (hasWriteAnnotation) {
                System.out.println("JvnProxy: @Write annotation detected. Acquiring write lock.");
                jvnObject.jvnLockWrite();
            }

            // 3. OBTENIR L'OBJET PARTAGÉ (maintenant il n'est plus null)
            Object sharedObject = jvnObject.jvnGetSharedObject();
            if (sharedObject == null) {
                // Si ça arrive, c'est un bug dans votre jvnLockRead/Write
                throw new JvnException("FATAL: L'objet partagé est null même après le verrouillage !");
            }

            // 4. INVOQUER LA MÉTHODE SUR L'OBJET RÉEL
            result = method.invoke(sharedObject, args);

        } finally {
            // 5. DÉVERROUILLER
            if (hasReadAnnotation || hasWriteAnnotation) {
                System.out.println("JvnProxy: Releasing lock for method '" + method.getName() + "'.");
                jvnObject.jvnUnLock();
            }
        }

        return result;
    }
}