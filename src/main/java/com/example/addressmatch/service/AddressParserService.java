package com.example.addressmatch.service;

import com.example.addressmatch.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AddressParserService {

    public List<String> parseAddressComponents(String address) {
        List<String> components = new ArrayList<>();
        String remaining = CommonUtils.cleanAddress(address);

        if (remaining.isEmpty()) {
            return components;
        }

        for (Pattern pattern : CommonUtils.ADDRESS_PATTERNS) {
            Matcher matcher = pattern.matcher(remaining);
            if (matcher.find()) {
                String component = matcher.group(1);
                components.add(component);
                remaining = remaining.substring(matcher.end()).trim();

                if (remaining.startsWith("市辖区")) {
                    remaining = remaining.substring(3);
                }
            } else {
                components.add("");
            }
        }

        if (!remaining.isEmpty()) {
            components.add(remaining);
        }

        log.debug("地址解析结果: {} -> {}", address, components);
        return components;
    }
}