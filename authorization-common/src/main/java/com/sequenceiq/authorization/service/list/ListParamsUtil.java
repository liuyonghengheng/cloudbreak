package com.sequenceiq.authorization.service.list;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.FilterParam;

@Component
public class ListParamsUtil {

    public Map<String, Object> getFilterParams(Method method, ProceedingJoinPoint proceedingJoinPoint) {
        Map<String, Object> params = new HashMap<>();
        int paramIndex = 0;
        List<Object> args = Lists.newArrayList(proceedingJoinPoint.getArgs());
        List<Parameter> parameters = Lists.newArrayList(method.getParameters());
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(FilterParam.class)) {
                FilterParam filterParam = parameter.getAnnotation(FilterParam.class);
                params.put(filterParam.value(), args.get(paramIndex));
            }
            paramIndex++;
        }
        return params;
    }
}
