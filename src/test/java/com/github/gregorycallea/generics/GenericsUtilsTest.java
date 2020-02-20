package com.github.gregorycallea.generics;

import com.google.gson.Gson;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Generics utility methods unit tests
 *
 * @author gregorycallea <gregory.callea@hotmail.it>
 * @since 2019-02-15 yyyy/mm/dd
 */
public class GenericsUtilsTest {

    //=====
    // Test case n°1
    //
    // Discussed on Stackoverflow question: https://stackoverflow.com/questions/53686017/using-a-generic-type-of-any-nested-subclass-within-its-abstract-superclass
    //=====

    //Data classes

    class User {
        private String name;

        public User(String name){
            this.name=name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof User)) return false;
            User user = (User) o;
            return Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(name);
        }
    }

    class UniversityUser extends User {
        private int universityID;

        public UniversityUser(String name, int universityID) {
            super(name);
            this.universityID = universityID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UniversityUser)) return false;
            if (!super.equals(o)) return false;
            UniversityUser that = (UniversityUser) o;
            return universityID == that.universityID;
        }

        @Override
        public int hashCode() {

            return Objects.hash(super.hashCode(), universityID);
        }
    }

    class Student extends UniversityUser {
        private int studentID;

        public Student(String name, int universityID, int studentID) {
            super(name, universityID);
            this.studentID = studentID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Student)) return false;
            if (!super.equals(o)) return false;
            Student student = (Student) o;
            return studentID == student.studentID;
        }

        @Override
        public int hashCode() {

            return Objects.hash(super.hashCode(), studentID);
        }
    }

    class Teacher extends UniversityUser {
        private int teacherID;

        public Teacher(String name, int universityID, int teacherID) {
            super(name, universityID);
            this.teacherID = teacherID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Teacher)) return false;
            Teacher teacher = (Teacher) o;
            return teacherID == teacher.teacherID;
        }

        @Override
        public int hashCode() {

            return Objects.hash(teacherID);
        }
    }

    //Data handler classes

    abstract class AbstractRequestHandler<I,O> {
        I input;
        O output;

        public AbstractRequestHandler(String inputJson) throws GenericsException {
            this.input = new Gson().fromJson(inputJson,GenericsUtils.getParameterizedType(getClass(), AbstractRequestHandler.class, 0));
        }
    }

    abstract class AbstractUserRequestHandler<I extends User,O> extends AbstractRequestHandler<I,O>{
        public AbstractUserRequestHandler(String inputJson) throws GenericsException {
            super(inputJson);
        }
    }
    abstract class AbstractUniversityRequestHandler<I extends UniversityUser> extends AbstractUserRequestHandler<I,String>{
        public AbstractUniversityRequestHandler(String inputJson) throws GenericsException {
            super(inputJson);
        }
    }
    class StudentRequestHandler extends AbstractUniversityRequestHandler<Student>{
        public StudentRequestHandler(String inputJson) throws GenericsException {
            super(inputJson);
        }
    }
    class TeacherRequestHandler extends AbstractUniversityRequestHandler<Teacher>{
        public TeacherRequestHandler(String inputJson) throws GenericsException {
            super(inputJson);
        }
    }

    @Test
    public void testCase1() throws Exception {
        final Teacher teacher = new Teacher("Greg",1,1);
        final String requestJson = new Gson().toJson(teacher);

        final Teacher inputRequestObj = new TeacherRequestHandler(requestJson).input;
        if(!teacher.equals(inputRequestObj))
            throw new Exception("Input json has not been parsed correctly from handler!");
    }

    //=====
    // Test case n°2
    //
    // Classes hierarchy:
    //
    // Base<I,E,F>
    // BaseA<G,H> extends Base<H,Boolean,G>
    // BaseB<T> extends BaseA<T,String>
    // BaseC<H> extends BaseB<H>
    // BaseD extends BaseC<Integer>
    // BaseE extends BaseD
    // BaseF extends BaseE
    // BaseG extends BaseF
    // BaseH<H> extends BaseG<H,Double>
    // BaseI<T> extends BaseF<T>
    // BaseL<J> extends BaseI<J>
    // BaseM extends BaseL<Float>
    // BaseN extends BaseM
    //
    //=====

    private class Base<I, E, F> {
        I var1;
        E var2;
        F var3;
    }

    private class BaseA<G, H> extends Base<H, Boolean, G> {
    }

    private class BaseB<T> extends BaseA<T, String> {
    }

    private class BaseC<H> extends BaseB<H> {
    }

    private class BaseD extends BaseC<Integer> {
    }

    private class BaseE extends BaseD {
    }

    private class BaseF extends BaseE {
    }

    private class BaseG<H,I> extends BaseF{
    }

    private class BaseH<H> extends BaseG<H,Double>{
    }

    private class BaseI<T> extends BaseH<T>{
    }

    private class BaseL<J> extends BaseH<J>{
    }

    private class BaseM extends BaseL<Float>{
    }

    private class BaseN extends BaseM{
    }

    private class NotBaseChild {
    }

    @Test
    public void testCase2() throws GenericsException {

        Type parameterizedType;
        
        parameterizedType = GenericsUtils.getParameterizedType(BaseG.class, Base.class, 0);
        assertThat((((Class) parameterizedType).getSimpleName()), is("String"));

        parameterizedType = GenericsUtils.getParameterizedType(BaseH.class, Base.class, 0);
        assertThat((((Class) parameterizedType).getSimpleName()), is("String"));

        parameterizedType = GenericsUtils.getParameterizedType(BaseF.class, Base.class, 1);
        assertThat((((Class) parameterizedType).getSimpleName()), is("Boolean"));

        parameterizedType = GenericsUtils.getParameterizedType(BaseF.class, Base.class, 2);
        assertThat((((Class) parameterizedType).getSimpleName()), is("Integer"));

        parameterizedType = GenericsUtils.getParameterizedType(BaseH.class, Base.class, 2);
        assertThat((((Class) parameterizedType).getSimpleName()), is("Integer"));

        //Expected exception => Class with no target class on parents hierarchy
        try {
            GenericsUtils.getParameterizedType(NotBaseChild.class, Base.class, 2);
        } catch (GenericsException e) {
            printExceptedException(e);
        }

        //Expected exception => Generic Parameter with index 2 declared on Base class has not yet a type assigned at class BaseB
        try {
            GenericsUtils.getParameterizedType(BaseB.class, Base.class, 2);
        } catch (GenericsException e) {
            printExceptedException(e);
        }


        //Expected exception => Base declares 3 generic parameters. So available index are from 0 to 2.
        try {
            GenericsUtils.getParameterizedType(BaseF.class, Base.class, 3);
        } catch (GenericsException e) {
            printExceptedException(e);
        }

        //Expected exception => BaseE doesn't declare generic parameters
        try {
            GenericsUtils.getParameterizedType(BaseF.class, BaseE.class, 2);
        } catch (GenericsException e) {
            printExceptedException(e);
        }

        parameterizedType = GenericsUtils.getParameterizedType(BaseN.class, Base.class, 2);
        assertThat((((Class) parameterizedType).getSimpleName()), is("Integer"));
        
    }

    /**
     * Prints excepted exception message:
     * @param e The expected exception raised
     */
    private void printExceptedException(final Exception e){
        System.out.println("Excepted exception has been raised: " + e.getMessage());
    }

}
