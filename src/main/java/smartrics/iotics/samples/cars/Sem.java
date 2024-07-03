package smartrics.iotics.samples.cars;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

public class Sem {
    private static final String IOTICS_NAMESPACE = "http://data.iotics.com/iotics#";
    private static final String GEO_NAMESPACE = "http://www.opengis.net/ont/geosparql#";

    private static final IRI HAS_GEOMETRY = Values.iri(GEO_NAMESPACE, "hasGeometry");
    private static final IRI POINT_NAME = Values.iri(IOTICS_NAMESPACE, "pointName");
    private static final IRI POINT_ID = Values.iri(IOTICS_NAMESPACE, "pointID");
    private static final IRI VALUE_KEY = Values.iri(IOTICS_NAMESPACE, "valueKey");
    private static final IRI SPATIAL_OBJECT = Values.iri(GEO_NAMESPACE, "SpatialObject");
    private static final IRI AS_WKT = Values.iri(GEO_NAMESPACE, "asWKT");
    private static final IRI WKT_LITERAL = Values.iri(GEO_NAMESPACE, "wktLiteral");
    private static final IRI PAYLOAD = Values.iri(IOTICS_NAMESPACE, "payload");

    public static <T> Model createModel(Binding binding, T value) {
        ValueFactory vf = Values.getValueFactory();

        // Create the Model
        Model model = new LinkedHashModel();

        // Define the resource IRI
        IRI resource = vf.createIRI(binding.valueID());

        // Add the pointName and value triples
        model.add(resource, POINT_NAME, vf.createLiteral(binding.pointName()));
        model.add(resource, POINT_ID, vf.createLiteral(binding.pointID()));
        model.add(resource, VALUE_KEY, vf.createLiteral(binding.valueKey()));
        switch (value) {
            case String s -> model.add(resource, PAYLOAD, vf.createLiteral(s));
            case Boolean b -> model.add(resource, PAYLOAD, vf.createLiteral(value.toString(), XSD.BOOLEAN));
            case Double v -> model.add(resource, PAYLOAD, vf.createLiteral(value.toString(), XSD.DOUBLE));
            case null, default -> throw new IllegalArgumentException("unsupported value type: " + value);
        }

        return model;
    }

    public static Model locationModel(Binding binding, String wktLiteral) {
        ValueFactory vf = Values.getValueFactory();

        // Create the Model
        Model model = new LinkedHashModel();

        // Define the resource IRI
        IRI resource = vf.createIRI(binding.valueID());

        IRI geomIRI = vf.createIRI(binding.valueID() + "_geometry");

        // Add the geometry triple
        model.add(resource, POINT_NAME, vf.createLiteral(binding.pointName()));
        model.add(resource, POINT_ID, vf.createLiteral(binding.pointID()));
        model.add(resource, VALUE_KEY, vf.createLiteral(binding.valueKey()));
        model.add(resource, HAS_GEOMETRY, geomIRI);
        model.add(geomIRI, RDF.TYPE, SPATIAL_OBJECT);
        model.add(geomIRI, AS_WKT, vf.createLiteral(wktLiteral, WKT_LITERAL));

        return model;
    }

}
