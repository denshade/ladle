package thelaboflieven.info.inifile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class IniFileReader {
    public Map<String, Map<String, String>> parseIniFile(String filePath) throws IOException {
        Map<String, Map<String, String>> iniData = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentSection = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                    continue; // skip comments/empty lines
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                    iniData.putIfAbsent(currentSection, new LinkedHashMap<>());
                } else if (currentSection != null && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts.length > 1 ? parts[1].trim() : "";
                    iniData.get(currentSection).put(key, value);
                }
            }
        }
        return iniData;
    }
}
