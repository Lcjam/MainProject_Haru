package com.haru.migration;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record MigrationFile(String version, String description, String script, Path path, String checksum, String content) {
    private static final Pattern NAME = Pattern.compile("V([0-9]+(?:\\.[0-9]+)*)__([A-Za-z0-9_-]+)\\.sql");

    static MigrationFile from(Path path, String checksum, String content) {
        Matcher matcher = NAME.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid migration filename: " + path.getFileName());
        }
        return new MigrationFile(matcher.group(1), matcher.group(2).replace('_', ' '), path.getFileName().toString(), path,
                checksum, content);
    }

    static Comparator<MigrationFile> numericOrder() {
        return (left, right) -> compareVersions(left.version, right.version);
    }

    static int compareVersions(String left, String right) {
        List<BigInteger> a = Arrays.stream(left.split("\\.")).map(BigInteger::new).toList();
        List<BigInteger> b = Arrays.stream(right.split("\\.")).map(BigInteger::new).toList();
        int width = Math.max(a.size(), b.size());
        for (int index = 0; index < width; index++) {
            BigInteger av = index < a.size() ? a.get(index) : BigInteger.ZERO;
            BigInteger bv = index < b.size() ? b.get(index) : BigInteger.ZERO;
            int comparison = av.compareTo(bv);
            if (comparison != 0) return comparison;
        }
        return left.compareTo(right);
    }
}
