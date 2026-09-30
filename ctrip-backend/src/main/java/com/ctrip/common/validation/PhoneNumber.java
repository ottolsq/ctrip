package com.ctrip.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * 手机号格式校验注解。
 *
 * <p>支持国际格式（可选 + 前缀），号码长度 8~15 位，例如：
 * <ul>
 *   <li>+8613800138000（国际格式）
 *   <li>13800138000（国内格式）
 * </ul>
 *
 * <p>内部类 {@link Validator} 实现具体校验逻辑，与注解定义放在同一文件保持内聚。
 */
@Documented
@Constraint(validatedBy = PhoneNumber.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "手机号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 校验逻辑实现。 */
    class Validator implements ConstraintValidator<PhoneNumber, String> {

        // 国际手机号正则：可选 + 开头，首位非 0，总长度 8~15 位
        private static final Pattern PHONE_PATTERN =
                Pattern.compile("^\\+?[1-9]\\d{7,14}$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // null 由 @NotBlank 等其他注解处理，此处只校验格式
            if (value == null) {
                return true;
            }
            return PHONE_PATTERN.matcher(value).matches();
        }
    }
}
