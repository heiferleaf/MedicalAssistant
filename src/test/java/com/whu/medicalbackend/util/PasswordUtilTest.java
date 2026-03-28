package com.whu.medicalbackend.util;

import com.whu.medicalbackend.common.util.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对于加密工具的单元测试
 */
@DisplayName("加密工具测试")
public class PasswordUtilTest {
    private static final String rawPassword;
    private static String encodedPassword;

    static {
        rawPassword = "123456";
    }

    /**
     * @BeforeAll 注解的方法，会在对象创建完后，执行一次，用来初始化类的静态成员，不包括final
     * 这是因为 final 需要在对象初始化结束（非static字段），或者类加载结束（static）后就得到值
     * 所以static的final字段，需要在静态代码中，直接初始化或者调用静态方法初始化，或者定义处初始化，这是static final 初始化的三种方式
     */
    @BeforeAll
    public static void init() {
        encodedPassword = PasswordUtil.encrypt(rawPassword);
    }

    /**
     * 对于原始的密码加密，加密后需要和加密前不一样
     * @param raw：原始密码
     */
    @ParameterizedTest
    @ValueSource(strings = {"sdas123", "adsfsdsg2321", "1231343"})
    @DisplayName("测试对于不同的原始密码，加密后不一样")
    public void testEncryptPassword(String raw) {
        assertNotEquals(raw, PasswordUtil.encrypt(raw));
    }

    @ParameterizedTest
    @MethodSource("pairOfPassword")
    @DisplayName("测试相同的原始密码，加密后相同")
    public void testEncryptResult(String expected, String rawPassword) {
        assertTrue(PasswordUtil.matches(rawPassword, expected));
    }

    /**
     * 参数化测试的参数提供方法,需要是静态方法
     * @return 键值对： expected rawArgument
     */
    public static Stream<Arguments> pairOfPassword() {
        return Stream.of(
                Arguments.of(PasswordUtil.encrypt("123"), "123"),
                Arguments.of(PasswordUtil.encrypt("qwe"), "qwe")
        );
    }

    /**
     * 测试匹配函数
     */
    @Test
    @DisplayName("测试正确密码匹配成功")
    public void testMatch() {
        String raw = "123456";

        boolean result = PasswordUtil.matches(raw, encodedPassword);

        assertTrue(result, "matches函数逻辑正确");
    }
    @Test
    @DisplayName("测试错误密码匹配失败")
    public void testMatch_() {
        String raw = "654321";

        boolean result = PasswordUtil.matches(raw, encodedPassword);

        assertFalse(result, "matches函数逻辑正确");
    }
}
