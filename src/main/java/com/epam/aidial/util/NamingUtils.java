package com.epam.aidial.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NamingUtils {
    private static final String NAME_PREFIX = "app-ctrl";

    public static String dialAuthSecretName(String name) {
        return kubeName("dial-auth", name);
    }

    public static String buildJobName(String name) {
        return kubeName("build", name);
    }

    public static String appName(String name) {
        return kubeName("app", name);
    }

    private String kubeName(String type, String name) {
        return "%s-%s-%s".formatted(NAME_PREFIX, type, name);
    }
}
