package com.github.gregorycallea.generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generics utility methods
 *
 * @author gregorycallea <gregory.callea@hotmail.it>
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
            throw new GenericsException(String.format("Target class [%s] has parameters type which index start from [0] to [%s]. You requested instead [%s]", rootClass, paramTypeNumber - 1, targetClassParametersNumber));

        Type type = analyzeParameterizedTypes(klass, rootClass, paramTypeNumber, null);
        if (!(type instanceof Class))
            throw new GenericsException(String.format("Parameter with index [%d] on class [%s] is not a concrete class starting from child class [%s]. It is instead a generic with name [%s]", paramTypeNumber, rootClass.getName(), klass.getName(), type));
        return type;
    }

    /**
     * Given a target class if recursively searches and navigates on its hierarchy until the specified root class and return the type of parameter type with specific number
     * On each step, it passes the child class types values on the next step in order to know what is the type implemented by the child class on the parent one.
     *
     * @param klass           The target class
     * @param rootClass       The parameterized root class
     * @param paramTypeNumber The parameter type number on the parameterized root class
     * @param childClassTypes The child class types map
     * @return The requested type
     *
     * @throws GenericsException
     */
    public static Type analyzeParameterizedTypes(final Class klass, final Class rootClass, final int paramTypeNumber, Map<Integer, Type> childClassTypes) {

        Type superclassType = klass.getGenericSuperclass();
        Map<TypeVariable, Type> currentClassTypes = new HashMap<>();
        int z = 0;
        for (TypeVariable variable : klass.getTypeParameters()) {
            if (childClassTypes != null) {
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
                } else {
                    superClassesTypes.put(i, currentClassTypes.containsKey(argType) ? currentClassTypes.get(argType) : argType);
                }
                i++;
            }
        }

        if (klass != rootClass)
            return analyzeParameterizedTypes(klass.getSuperclass(), rootClass, paramTypeNumber, superClassesTypes);
        return childClassTypes.get(paramTypeNumber);

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
