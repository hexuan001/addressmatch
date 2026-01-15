package com.example.addressmatch.util;

import java.util.*;
import java.util.regex.Pattern;

public class CommonUtils {

    public static final Pattern[] ADDRESS_PATTERNS = {
            Pattern.compile("(.*?(省|自治区|直辖市))"),
            Pattern.compile("(.*?(市|自治州|地区|盟))"),
            Pattern.compile("(.*?(区|县|县级市|自治县))"),
            Pattern.compile("(.*?(街道|镇|乡))"),
            Pattern.compile("(.*?(社区|村|居委会))"),
            Pattern.compile("(.*?(路|街|大道|胡同|巷))"),
            Pattern.compile("(\\d+号楼?|\\d+栋|\\d+幢|\\d+座)"),
            Pattern.compile("(\\d+单元|\\d+门)"),
            Pattern.compile("(\\d+室|\\d+号|\\d+户)")
    };

    public static String cleanAddress(String address) {
        if (address == null) return "";
        return address.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[(（][^)）]*[)）]", "")
                .replaceAll("[，。；：]", "");
    }

    public static boolean isFuzzyMatch(String str1, String str2) {
        if (str1.equals(str2)) return true;
        if (str1.contains(str2) || str2.contains(str1)) return true;
        return false;
    }
}