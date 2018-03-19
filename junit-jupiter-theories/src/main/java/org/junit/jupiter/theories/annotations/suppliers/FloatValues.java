package org.junit.jupiter.theories.annotations.suppliers;

import org.apiguardian.api.API;
import org.junit.jupiter.theories.suppliers.FloatTheoryArgumentSupplier;

import java.lang.annotation.*;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * Parameter argument supplier annotation that can be added to a theory parameter to specify the exact values that will be used for that parameter. Provides
 * {@code float} values.
 */
@Target(ElementType.PARAMETER)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSuppliedBy(FloatTheoryArgumentSupplier.class)
@API(status = EXPERIMENTAL, since = "5.2")
public @interface FloatValues {
    /**
     * @return the value(s) to use for the annotated theory parameter
     */
    float[] value();
}
