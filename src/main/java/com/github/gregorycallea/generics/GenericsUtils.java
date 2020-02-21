package com.github.gregorycallea.generics;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Generics utility methods
 *
 * @author gregorycallea <gregory.callea@hotmail.it>
 * @author bratlomiej.mazur <gotofinal@gotofinal.com>
 * @since 2019-02-15 yyyy/mm/dd
 */
public class GenericsUtils {

    /**
     * Given a target class it searches and navigates on its hierarchy until the specified root class and returns the type of generic parameter with specific number declared on the root class.
     * <p>
     * Example)
     * If you have the following parent class and child hierarchy:
     * <p>
     * Base<I,E,F>
     * BaseA<F> extends Base<String,Boolean,F>
     * BaseB extends BaseA<Integer>
     * <p>
     * Starting from *BaseB* ( klass ) you can dinamically evaluate the type of each generic defined on the parent class *Base* ( rootClass )
     * via the parameter type number on the parent class ( paramTypeNumber ) that are:
     * <p>
     * I = 0
     * E = 1
     * F = 2
     *
     * @param klass           The target class
     * @param rootClass       The parameterized root class
     * @param paramTypeNumber The parameter type number on the parameterized root class
     * @return The requested type
     * @throws GenericsException
     */
    public static Type getParameterizedType(final Class<?> klass, final Class<?> rootClass, final int paramTypeNumber) throws GenericsException {

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
                if (argType instanceof TypeVariable) {
                    superClassesTypes.put(i, currentClassTypes.getOrDefault(argType, argType));
                } else {
                    superClassesTypes.put(i, refineType(argType, currentClassTypes));
                }
                i++;
            }
        }

        if (klass != rootClass) {
            final Class<?> superClass = klass.getSuperclass();
            if (superClass == null)
                throw new GenericsException(String.format("Class [%s] not found on class parent hierarchy [%s]", rootClass, targetClass));
            return analyzeParameterizedTypes(superClass, targetClass, rootClass, paramTypeNumber, superClassesTypes);
        }
        return childClassTypes.get(paramTypeNumber);

    }

    private static Type refineType(Type type, Map<TypeVariable<?>, Type> typeVariablesMap) throws GenericsException {
        if (type instanceof Class) {
            return type;
        }
        if (type instanceof GenericArrayType) {
            return refineArrayType((GenericArrayType) type, typeVariablesMap);
        }
        if (type instanceof ParameterizedType) {
            return refineParameterizedType((ParameterizedType) type, typeVariablesMap);
        }
        if (type instanceof WildcardType) {
            return refineWildcardType((WildcardType) type, typeVariablesMap);
        }
        if (type instanceof TypeVariable) {
            return typeVariablesMap.get(type);
        }
        throw new GenericsException("Unsolvable generic type: " + type);
    }

    private static Type[] refineTypes(Type[] types, Map<TypeVariable<?>, Type> typeVariablesMap) throws GenericsException {
        Type[] refinedTypes = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            refinedTypes[i] = refineType(types[i], typeVariablesMap);
        }
        return refinedTypes;
    }

    private static Type refineWildcardType(WildcardType wildcardType, Map<TypeVariable<?>, Type> typeVariablesMap) throws GenericsException {
        Type[] refinedUpperBounds = refineTypes(wildcardType.getUpperBounds(), typeVariablesMap);
        Type[] refinedLowerBounds = refineTypes(wildcardType.getLowerBounds(), typeVariablesMap);
        return new ResolvedWildcardType(refinedUpperBounds, refinedLowerBounds);
    }

    private static Type refineParameterizedType(ParameterizedType parameterizedType, Map<TypeVariable<?>, Type> typeVariablesMap) throws GenericsException {
        Type[] refinedTypeArguments = refineTypes(parameterizedType.getActualTypeArguments(), typeVariablesMap);
        return new ResolvedParameterizedType(parameterizedType.getRawType(), refinedTypeArguments, parameterizedType.getOwnerType());
    }


    private static Type refineArrayType(GenericArrayType genericArrayType, Map<TypeVariable<?>, Type> typeVariablesMap) throws GenericsException {
        int levels = getArrayDimensions(genericArrayType);
        Type arrayComponentType = refineType(getArrayNestedComponentType(genericArrayType), typeVariablesMap);
        return buildArrayType(arrayComponentType, levels);
    }

    private static Type buildArrayType(Type componentType, int levels) throws GenericsException {
        if (componentType instanceof Class) {
            return Array.newInstance(((Class<?>) componentType), new int[levels]).getClass();
        } else if (componentType instanceof ParameterizedType) {
            GenericArrayType genericArrayType = new ResolvedGenericArrayType(componentType);
            for (int i = 1; i < levels; i++) {
                genericArrayType = new ResolvedGenericArrayType(genericArrayType);
            }
            return genericArrayType;
        } else {
            throw new GenericsException("Array can't be of generic type");
        }
    }

    private static int getArrayDimensions(GenericArrayType genericArrayType) {
        int levels = 1;
        GenericArrayType currentArrayLevel = genericArrayType;
        while (currentArrayLevel.getGenericComponentType() instanceof GenericArrayType) {
            currentArrayLevel = (GenericArrayType) currentArrayLevel.getGenericComponentType();
            levels += 1;
        }
        return levels;
    }

    private static Type getArrayNestedComponentType(GenericArrayType genericArrayType) {
        GenericArrayType currentArrayLevel = genericArrayType;
        while (currentArrayLevel.getGenericComponentType() instanceof GenericArrayType) {
            currentArrayLevel = (GenericArrayType) currentArrayLevel.getGenericComponentType();
        }
        return currentArrayLevel.getGenericComponentType();
    }

    private static class ResolvedGenericArrayType implements GenericArrayType {
        private final Type genericComponentType;

        ResolvedGenericArrayType(Type genericComponentType) {
            this.genericComponentType = genericComponentType;
        }

        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }

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

    private static class ResolvedParameterizedType implements ParameterizedType {
        private final Type[] actualTypeArguments;
        private final Class<?> rawType;
        private final Type ownerType;

        private ResolvedParameterizedType(Type rawType, Type[] actualTypeArguments, Type ownerType) {
            this.actualTypeArguments = actualTypeArguments;
            this.rawType = (Class<?>) rawType;
            this.ownerType = (ownerType != null) ? ownerType : this.rawType.getDeclaringClass();
        }

        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        public Class<?> getRawType() {
            return rawType;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType that = (ParameterizedType) o;
            if (this == that)
                return true;
            Type thatOwner = that.getOwnerType();
            Type thatRawType = that.getRawType();
            return Objects.equals(ownerType, thatOwner) && Objects.equals(rawType, thatRawType) &&
                    Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(actualTypeArguments) ^
                    Objects.hashCode(ownerType) ^
                    Objects.hashCode(rawType);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (ownerType != null) {
                sb.append(ownerType.getTypeName());
                sb.append("$");
                if (ownerType instanceof ResolvedParameterizedType) {
                    sb.append(rawType.getName().replace(((ResolvedParameterizedType) ownerType).rawType.getName() + "$", ""));
                } else
                    sb.append(rawType.getSimpleName());
            } else
                sb.append(rawType.getName());
            if (actualTypeArguments != null) {
                StringJoiner sj = new StringJoiner(", ", "<", ">");
                sj.setEmptyValue("");
                for (Type t : actualTypeArguments) {
                    sj.add(t.getTypeName());
                }
                sb.append(sj.toString());
            }
            return sb.toString();
        }
    }

    private static class ResolvedWildcardType implements WildcardType {
        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        public ResolvedWildcardType(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds;
            this.lowerBounds = lowerBounds;
        }

        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        public String toString() {
            Type[] lowerBounds = getLowerBounds();
            Type[] bounds = lowerBounds;
            StringBuilder sb = new StringBuilder();
            if (lowerBounds.length > 0)
                sb.append("? super ");
            else {
                Type[] upperBounds = getUpperBounds();
                if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class)) {
                    bounds = upperBounds;
                    sb.append("? extends ");
                } else
                    return "?";
            }
            StringJoiner sj = new StringJoiner(" & ");
            for (Type bound : bounds) {
                sj.add(bound.getTypeName());
            }
            sb.append(sj.toString());
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof WildcardType) {
                WildcardType that = (WildcardType) o;
                return Arrays.equals(this.getLowerBounds(), that.getLowerBounds()) && Arrays.equals(this.getUpperBounds(), that.getUpperBounds());
            } else
                return false;
        }

        @Override
        public int hashCode() {
            Type[] lowerBounds = getLowerBounds();
            Type[] upperBounds = getUpperBounds();
            return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
        }
    }
}
