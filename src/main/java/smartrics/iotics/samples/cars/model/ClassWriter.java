package smartrics.iotics.samples.cars.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ClassWriter {

    public static boolean saveToFile(File rootDirectory, GeneratedClass generatedClass) throws IOException {
        File directoryPath = new File(rootDirectory, generatedClass.packageName().replace('.', '/'));
        if(!directoryPath.exists() && !directoryPath.mkdirs()) {
            return false;
        }
        File filePath = new File(directoryPath, generatedClass.className() + ".java");
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(generatedClass.classCode());
        }
        return true;
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }

}
