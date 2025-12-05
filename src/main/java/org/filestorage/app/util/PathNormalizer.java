package org.filestorage.app.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathNormalizer {

    public static String normalize(String path) {

        if (path == null || path.isEmpty()) {
            return "/";
        }

        if(path.startsWith("/") && path.length() != 1){
            path = path.substring(1);
        }

        return decode(path);
    }

    private static String decode(String path) {

        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        StringBuilder cleanedPath = new StringBuilder();

        for(int i = 0; i < decoded.length(); i++){
            char c = decoded.charAt(i);
            if(c >= 32 && c != 127){
                cleanedPath.append(c);
            }
        }

        return cleanedPath.toString();
    }

}
