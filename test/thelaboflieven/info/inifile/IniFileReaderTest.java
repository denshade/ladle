package thelaboflieven.info.inifile;

import org.junit.Test;

import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class IniFileReaderTest {
    @Test
    public void parsesSectionAndKey() throws Exception {
        var temp = Files.createTempFile("ladle", ".ini");
        try (var writer = new FileWriter(temp.toFile())) {
            writer.write("[main]\nkey = value\n");
        }

        var reader = new IniFileReader();
        var data = reader.parseIniFile(temp.toString());

        assertEquals("value", data.get("main").get("key"));
    }
}
