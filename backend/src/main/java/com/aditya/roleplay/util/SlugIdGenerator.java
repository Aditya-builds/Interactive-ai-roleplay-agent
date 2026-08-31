package com.aditya.roleplay.util;

import java.util.UUID;
import java.util.function.Predicate;

public final class SlugIdGenerator {

    private SlugIdGenerator() {
    }

    public static String fromName(String name) {
        String slug = normalize(name);
        if (slug.isBlank()) {
            slug = "item";
        }
        return slug + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String uniqueFromName(String name, Predicate<String> exists) {
        String base = normalize(name);
        if (base.isBlank()) {
            base = "item";
        }
        String candidate = base;
        int suffix = 1;
        while (exists.test(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
