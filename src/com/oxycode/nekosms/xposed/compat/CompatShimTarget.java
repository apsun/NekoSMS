package com.oxycode.nekosms.xposed.compat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompatShimTarget {
    String packageName() default "";
    String manufacturer() default "";
    String[] models() default {};
}
