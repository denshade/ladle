package thelaboflieven.info.build;

public record BuildPlan(
        String command,
        int sourceFileCount,
        String javacPath,
        String parameters,
        String subprojectClasspath
) {
}
