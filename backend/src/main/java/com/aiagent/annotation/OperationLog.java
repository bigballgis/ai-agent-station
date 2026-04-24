package com.aiagent.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String value() default "";
    String module() default "";
    /** Resource type being operated on (e.g., "Agent", "User", "Workflow") */
    String resourceType() default "";
}
