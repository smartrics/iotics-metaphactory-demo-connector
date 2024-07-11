package smartrics.iotics.samples.cars.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TTLParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TTLParser.class);

    public static void main(String[] args) throws IOException {
        File folder = new File("src/main/resources");
        String packageName = "smartrics.iotics.samples.cars";
        String rootDirectoryPath = "target/generated-sources/";

        File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith("0.1.ttl"));

        if (listOfFiles == null) {
            LOGGER.warn("No .ttl files found in the directory.");
            return;
        }

        Model model = null;
        Repository repo = new SailRepository(new MemoryStore());
        repo.init();

        File rootDirectory = new File(rootDirectoryPath);
        ClassWriter.deleteDirectory(rootDirectory);
        if(!rootDirectory.mkdirs()) {
            throw new IllegalStateException("unable to make directories " + rootDirectory);
        }

        try (RepositoryConnection conn = repo.getConnection()) {
            for (File file : listOfFiles) {
                InputStream inputStream = new FileInputStream(file);
                model = Rio.parse(inputStream, "", RDFFormat.TURTLE);
                conn.add(model);
            }

            String queryString =
                    "PREFIX sh: <http://www.w3.org/ns/shacl#> " +
                            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                            "SELECT ?class ?property ?datatype ?targetClass WHERE { " +
                            "  ?shape a sh:NodeShape ; " +
                            "         sh:targetClass ?class ; " +
                            "         sh:property ?propertyShape . " +
                            "  ?propertyShape sh:path ?property . " +
                            "  OPTIONAL { ?propertyShape sh:datatype ?datatype } . " +
                            "  OPTIONAL { ?propertyShape sh:class ?targetClass } . " +
                            "}";

            TupleQuery query = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult result = query.evaluate()) {
                Map<String, List<Property>> classPropertiesMap = new HashMap<>();

                QueryResults.stream(result).forEach(bindingSet -> {
                    String className = bindingSet.getValue("class").stringValue();
                    String propertyName = bindingSet.getValue("property").stringValue();
                    String datatype = bindingSet.hasBinding("datatype") ? bindingSet.getValue("datatype").stringValue() : null;
                    String targetClass = bindingSet.hasBinding("targetClass") ? bindingSet.getValue("targetClass").stringValue() : null;

                    classPropertiesMap.computeIfAbsent(className, k -> new ArrayList<>())
                            .add(new Property(propertyName, datatype, targetClass));
                });

                for (Map.Entry<String, List<Property>> entry : classPropertiesMap.entrySet()) {
                    String className = entry.getKey();
                    List<Property> properties = entry.getValue();
                    GeneratedClass genClass = JavaClassGenerator.generateClass(className, properties, packageName);
                    if(!ClassWriter.saveToFile(rootDirectory, genClass)) {
                        LOGGER.warn("unable to write class " + genClass.className());
                    }
                }
            }
        }
    }

    public static class Property {
        String name;
        String datatype;
        String targetClass;

        public Property(String name, String datatype, String targetClass) {
            this.name = name;
            this.datatype = datatype;
            this.targetClass = targetClass;
        }

        @Override
        public String toString() {
            return "Property{" +
                    "name='" + name + '\'' +
                    ", datatype='" + datatype + '\'' +
                    ", targetClass='" + targetClass + '\'' +
                    '}';
        }
    }
}
