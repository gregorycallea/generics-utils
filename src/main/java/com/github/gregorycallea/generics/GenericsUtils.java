package com.github.gregorycallea.generics;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Generics utility methods
 *
 * @author gregorycallea <gregory.callea@hotmail.it>
 *
 * @since 2019-02-15 yyyy/mm/dd
 */
public class GenericsUtils {

    /**
     * Given a target class it searches and navigates on its hierarchy until the specified root class and returns the type of generic parameter with specific number declared on the root class.
     *
     * Example)
     * If you have the following parent class and child hierarchy:
     *
     * Base<I,E,F>
     * BaseA<F> extends Base<String,Boolean,F>
     * BaseB extends BaseA<Integer>
     *
     * Starting from *BaseB* ( klass ) you can dinamically evaluate the type of each generic defined on the parent class *Base* ( rootClass )
     * via the parameter type number on the parent class ( paramTypeNumber ) that are:
     *
     * I = 0
     * E = 1
     * F = 2
     *
     * @param klass           The target class
     * @param rootClass       The parameterized root class
     * @param paramTypeNumber The parameter type number on the parameterized root class
     * @return The requested type
     *
     * @throws GenericsException
     */
    public static Type getParameterizedType(final Class klass, final Class rootClass, final int paramTypeNumber) throws GenericsException {

        final int targetClassParametersNumber = rootClass.getTypeParameters().length;
        if (targetClassParametersNumber == 0) {
            throw new GenericsException(String.format("Target class [%s] has no parameters type", rootClass.getName()));
        } else if (targetClassParametersNumber - 1 < paramTypeNumber)
            throw new GenericsException(String.format("Target class [%s] has parameters type which index start from [0] to [%s]. You requested instead parameter with index [%s]", rootClass, paramTypeNumber - 1, targetClassParametersNumber));

        Type type = analyzeParameterizedTypes(klass, klass, rootClass, paramTypeNumber, null);
        if (!isDefined(type))
            throw new GenericsException(String.format("Parameter [%s] with index [%d] defined on class [%s] has not been valued yet on child class [%s]", type, paramTypeNumber, rootClass.getName(), klass.getName()));
        return type;
    }

    /**
     * Check if specified type is defined
     *
     * @param type The input type
     * @return True it type is defined. Otherwise false.
     */
    private static boolean isDefined(Type type) {
        if (type instanceof Class)
            return true;
        if (type instanceof GenericArrayType)
            return isDefined(((GenericArrayType) type).getGenericComponentType());
        if (type instanceof WildcardType) {
            for (final Type lowerBound : ((WildcardType) type).getLowerBounds()) {
                if (!isDefined(lowerBound))
                    return false;
            }
            for (final Type upperBound : ((WildcardType) type).getUpperBounds()) {
                if (!isDefined(upperBound))
                    return false;
            }
            return true;
        }
        if (!(type instanceof ParameterizedType))
            return false;
        for (final Type typeArgument : ((ParameterizedType) type).getActualTypeArguments()) {
            if (!isDefined(typeArgument))
                return false;
        }
        return true;
    }

    /**
     * Given a target class if recursively searches and navigates on its hierarchy until the specified root class and return the type of parameter type with specific number
     * On each step, it passes the child class types values on the next step in order to know what is the type implemented by the child class on the parent one.
     *
     * @param klass           The recursive step class
     * @param targetClass     The target class
     * @param rootClass       The parameterized root class
     * @param paramTypeNumber The parameter type number on the parameterized root class
     * @param childClassTypes The child class types map
     * @return The requested type
     * @throws GenericsException
     */
    public static Type analyzeParameterizedTypes(final Class klass, final Class targetClass, final Class rootClass, final int paramTypeNumber, Map<Integer, Type> childClassTypes) throws GenericsException {

        Type superclassType = klass.getGenericSuperclass();
        Map<TypeVariable<?>, Type> currentClassTypes = new HashMap<>();
        int z = 0;
        if (childClassTypes != null) {
            for (TypeVariable<?> variable : klass.getTypeParameters()) {
                currentClassTypes.put(variable, childClassTypes.get(z));
                z++;
            }
        }

        Map<Integer, Type> superClassesTypes = new HashMap<>();
        if (superclassType instanceof ParameterizedType) {
            int i = 0;
            for (final Type argType : ((ParameterizedType) superclassType).getActualTypeArguments()) {
                if (argType instanceof Class) {
                    superClassesTypes.put(i, argType);
                } else if (argType instanceof GenericArrayType) {
                    final Type arrayComponentType = childClassTypes.get(i);
                    Class<?> rawArrayComponentType;
                    boolean parametrized = arrayComponentType instanceof ParameterizedType;
                    if (parametrized) {
                        rawArrayComponentType = (Class<?>) ((ParameterizedType) arrayComponentType).getRawType();
                    } else if (arrayComponentType instanceof Class) {
                        rawArrayComponentType = (Class<?>) arrayComponentType;
                    } else {
                        throw new GenericsException("Array can't be of generic type");
                    }
                    // TODO Add safety check if argType.getGenericComponentType() is assignable to arrayComponentType
                    Class<?> arrayClassType = Array.newInstance(rawArrayComponentType, 0).getClass(); // rawArrayComponentType.arrayType()
                    Type arrayType = parametrized ? new ResolvedGenericArrayType(arrayComponentType) : arrayClassType;
                    superClassesTypes.put(i, arrayType);
                } else {
                    if(currentClassTypes.containsKey(argType)){
                        superClassesTypes.put(i, currentClassTypes.get(argType));
                    }else{
                        superClassesTypes.put(i, argType);
                    }
                }
                i++;
            }
        }

        if (klass != rootClass) {
            final Class superClass = klass.getSuperclass();
            if (superClass == null)
                throw new GenericsException(String.format("Class [%s] not found on class parent hierarchy [%s]", rootClass,targetClass));
            return analyzeParameterizedTypes(superClass,targetClass, rootClass, paramTypeNumber, superClassesTypes);
        }
        return childClassTypes.get(paramTypeNumber);

    }

    /**
     * The specific resolved generic array type class
     */
    private static class ResolvedGenericArrayType implements GenericArrayType {
        private final Type genericComponentType;

        ResolvedGenericArrayType(Type genericComponentType) {
            this.genericComponentType = genericComponentType;
        }

        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }

        @Override
        public String toString() {
            return getGenericComponentType().toString() + "[]";
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof GenericArrayType) {
                GenericArrayType that = (GenericArrayType) o;
                return Objects.equals(genericComponentType, that.getGenericComponentType());
            } else
                return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(genericComponentType);
        }
    }

    /**
     * Returns class for given type
     *
     * @param type The type object
     * @return The object class
     * @throws GenericsException
     */
    public static <E> Class<E> getClassFromType(final Type type) throws GenericsException {
        String className = type.toString().split(" ")[1];
        try {
            return (Class<E>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new GenericsException(String.format("Class [%s] has not been found on classpath!", className));
        }
    }

}
