package de.telekom.swepm.utils;

import org.springframework.stereotype.Component;

@Component
public class HtmlEscaper {
    public String escapeHTML(String str) {
        if (str == null) return null;
        return str.replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
    }
}
