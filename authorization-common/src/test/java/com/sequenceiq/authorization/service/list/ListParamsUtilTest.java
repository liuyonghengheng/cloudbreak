package com.sequenceiq.authorization.service.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.annotation.FilterParam;

@ExtendWith(MockitoExtension.class)
public class ListParamsUtilTest {

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    private ListParamsUtil underTest = new ListParamsUtil();

    @Test
    public void testWhenArgumentIsPresent() throws NoSuchMethodException {
        givenArguments("Something");

        Map<String, Object> params = underTest.getFilterParams(TestClass.class.getMethod("method1", String.class), proceedingJoinPoint);

        assertEquals(Map.of("name", "Something"), params);
    }

    @Test
    public void testWhenArgumentIsNull() throws NoSuchMethodException {
        givenNullArgument();

        Map<String, Object> params = underTest.getFilterParams(TestClass.class.getMethod("method1", String.class), proceedingJoinPoint);

        Map<String, Object> expected = new HashMap<>();
        expected.put("name", null);
        assertEquals(expected, params);
    }

    @Test
    public void testWithMultipleParams() throws NoSuchMethodException {
        givenArguments("A", 1, true);

        Map<String, Object> params = underTest.getFilterParams(TestClass.class.getMethod("method2", String.class, int.class, boolean.class),
                proceedingJoinPoint);

        assertEquals(Map.of("a", "A", "b", 1, "c", true), params);
    }

    @Test
    public void testOnlyFilterParamsCatched() throws NoSuchMethodException {
        givenArguments(2, true, "C");

        Map<String, Object> params = underTest.getFilterParams(TestClass.class.getMethod("method3", int.class, boolean.class, String.class),
                proceedingJoinPoint);

        assertEquals(Map.of("a", 2, "c", "C"), params);
    }

    private void givenArguments(Object... args) {
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
    }

    private void givenNullArgument() {
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{null});
    }

    public static class TestClass {
        public void method1(@FilterParam("name") String name) {
        }

        public void method2(@FilterParam("a") String a, @FilterParam("b") int b, @FilterParam("c") boolean c) {
        }

        public void method3(@FilterParam("a") int a, boolean b, @FilterParam("c") String c) {
        }
    }

}