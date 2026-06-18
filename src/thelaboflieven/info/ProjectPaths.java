package thelaboflieven.info;

import java.io.File;
import java.io.IOException;

public final class ProjectPaths {
    private ProjectPaths() {
    }

    public static String relativeTo(File projectDir, File file) throws IOException {
        var projectPath = projectDir.getCanonicalFile().toPath();
        var filePath = file.getCanonicalFile().toPath();
        if (filePath.startsWith(projectPath)) {
            return projectPath.relativize(filePath).toString().replace('\\', '/');
        }
        return file.getPath();
    }
}
