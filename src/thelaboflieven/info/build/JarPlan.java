package thelaboflieven.info.build;

import java.util.List;

public record JarPlan(List<String> command, String outputJar, String classesDirectory) {
}
