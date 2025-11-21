package Utils;

/**
 * 
 * 
 */
public class ClassPathUtils {
    /**
     * 
     * 
     * @return 
     */
    public static String buildClasspath() {
        String sep = System.getProperty("path.separator");
        String cp = System.getProperty("java.class.path");

        String gsonPath = "lib/gson-2.13.1.jar";
        String flatLafPath = "lib/flatlaf-3.6.2.jar";
        String flatLafThemesPath = "lib/flatlaf-intellij-themes-3.6.2.jar";

        return String.join(sep,
                cp,
                gsonPath,
                flatLafPath,
                flatLafThemesPath
        );
    }
}
