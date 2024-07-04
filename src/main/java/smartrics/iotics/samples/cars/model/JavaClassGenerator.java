package smartrics.iotics.samples.cars.model;

import smartrics.iotics.connectors.twins.annotations.Feed;
import smartrics.iotics.connectors.twins.annotations.LiteralProperty;
import smartrics.iotics.connectors.twins.annotations.XsdDatatype;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class JavaClassGenerator {

    public static GeneratedClass generateClass(String className, List<TTLParser.Property> properties, String packageName) {
        StringBuilder classCode = new StringBuilder();
        classCode.append("package ").append(packageName).append(";\n\n");

        classCode.append("import com.iotics.api.GeoLocation;\n");
        classCode.append("import smartrics.iotics.connectors.twins.*;\n");
        classCode.append("import smartrics.iotics.connectors.twins.annotations.*;\n");
        classCode.append("import smartrics.iotics.host.IoticsApi;\n");
        classCode.append("import smartrics.iotics.host.UriConstants;\n");
        classCode.append("import smartrics.iotics.identity.Identity;\n");
        classCode.append("import smartrics.iotics.identity.IdentityManager;\n");
        classCode.append("import smartrics.iotics.connectors.twins.annotations.LiteralProperty;\n");
        classCode.append("import smartrics.iotics.connectors.twins.annotations.XsdDatatype;\n");
        classCode.append("import smartrics.iotics.connectors.twins.annotations.Feed;\n\n");
        classCode.append("public class ").append(simpleName(className)).append(" extends AbstractTwin implements MappablePublisher, MappableMaker, AnnotationMapper {\n");

        Set<String> processedProperties = new HashSet<>();

        for (TTLParser.Property property : properties) {
            String propName = simpleName(property.name);

            if (!processedProperties.contains(propName)) {
                if ("http://data.iotics.com/iotics#advertises".equals(property.name) && property.targetClass != null) {
                    // Generate method with @Feed annotation
                    String methodName = "get" + capitalize(simpleName(property.targetClass));
                    String feedId = "get_it_from_the_shacl";  // Generate unique feed ID
                    classCode.append("    @Feed(id = \"").append(feedId).append("\")\n");
                    classCode.append("    public ").append(simpleName(property.targetClass)).append(" ").append(methodName).append("() { return new ").append(simpleName(property.targetClass)).append("(); }\n");
                } else if (!"http://data.iotics.com/iotics#advertises".equals(property.name)) {
                    // Generate attribute and methods with @LiteralProperty annotation
                    if(property.datatype == null) {
                        continue;
                    }
                    String propType = javaType(property.datatype);
                    XsdDatatype xsdDatatype = xsdType(property.datatype);

                    classCode.append("    @LiteralProperty(iri=\"").append(property.name).append("\", dataType=XsdDatatype.").append(xsdDatatype.name()).append(")\n");
                    classCode.append("    private ").append(propType).append(" ").append(propName).append(";\n");
                    classCode.append("    public ").append(propType).append(" get").append(capitalize(propName)).append("() { return ").append(propName).append("; }\n");
                    classCode.append("    public void set").append(capitalize(propName)).append("(").append(propType).append(" ").append(propName).append(") { this.").append(propName).append(" = ").append(propName).append("; }\n");
                }
                processedProperties.add(propName);
            }
        }

        classCode.append("}\n");

        // Save the generated class to a file
        return new GeneratedClass(packageName, simpleName(className), classCode.toString());
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String simpleName(String uri) {
        int lastSlashIndex = uri.lastIndexOf('/');
        int lastHashIndex = uri.lastIndexOf('#');
        int index = Math.max(lastSlashIndex, lastHashIndex);
        return uri.substring(index + 1);
    }

    private static String javaType(String datatype) {
        switch (datatype) {
            case "http://www.w3.org/2001/XMLSchema#string":
                return "String";
            case "http://www.w3.org/2001/XMLSchema#int":
                return "int";
            case "http://www.w3.org/2001/XMLSchema#boolean":
                return "boolean";
            case "http://www.w3.org/2001/XMLSchema#integer":
                return "Integer";
            case "http://www.w3.org/2001/XMLSchema#float":
                return "float";
            case "http://www.w3.org/2001/XMLSchema#double":
                return "double";
            case "http://www.w3.org/2001/XMLSchema#date":
                return "java.time.LocalDate";
            case "http://www.w3.org/2001/XMLSchema#dateTime":
                return "java.time.LocalDateTime";
            case "http://www.w3.org/2001/XMLSchema#anyURI":
                return "java.net.URI";
            default:
                return "String"; // Default to String if datatype is unknown
        }
    }

    private static XsdDatatype xsdType(String datatype) {
        return switch (datatype) {
            case "http://www.w3.org/2001/XMLSchema#string" -> XsdDatatype.string;
            case "http://www.w3.org/2001/XMLSchema#int" -> XsdDatatype.int_;
            case "http://www.w3.org/2001/XMLSchema#boolean" -> XsdDatatype.boolean_;
            case "http://www.w3.org/2001/XMLSchema#integer" -> XsdDatatype.integer;
            case "http://www.w3.org/2001/XMLSchema#float" -> XsdDatatype.float_;
            case "http://www.w3.org/2001/XMLSchema#double" -> XsdDatatype.double_;
            case "http://www.w3.org/2001/XMLSchema#date" -> XsdDatatype.date;
            case "http://www.w3.org/2001/XMLSchema#dateTime" -> XsdDatatype.dateTime;
            case "http://www.w3.org/2001/XMLSchema#anyURI" -> XsdDatatype.anyURI;
            default -> XsdDatatype.string; // Default to String if datatype is unknown
        };
    }
}
