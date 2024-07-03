package smartrics.iotics.samples.cars;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.List;

public class Sem {
    private static final String IOTICS_NAMESPACE = "http://data.iotics.com/iotics#";
    private static final String GEO_NAMESPACE = "http://www.opengis.net/ont/geosparql#";

    private static final IRI HAS_GEOMETRY = Values.iri(GEO_NAMESPACE, "hasGeometry");
    private static final IRI POINT_NAME = Values.iri(IOTICS_NAMESPACE, "pointName");
    private static final IRI POINT_ID = Values.iri(IOTICS_NAMESPACE, "pointID");
    private static final IRI TWIN_ID = Values.iri(IOTICS_NAMESPACE, "twinID");
    private static final IRI VALUE_KEY = Values.iri(IOTICS_NAMESPACE, "valueKey");
    private static final IRI SPATIAL_OBJECT = Values.iri(GEO_NAMESPACE, "SpatialObject");
    private static final IRI AS_WKT = Values.iri(GEO_NAMESPACE, "asWKT");
    private static final IRI WKT_LITERAL = Values.iri(GEO_NAMESPACE, "wktLiteral");
    private static final IRI PAYLOAD = Values.iri(IOTICS_NAMESPACE, "payload");
    private static final IRI HAS_VALUE = Values.iri(IOTICS_NAMESPACE, "hasValue");
    private static final IRI HAS_SPEED = Values.iri(IOTICS_NAMESPACE, "hasSpeed");
    private static final IRI HAS_DIRECTION = Values.iri(IOTICS_NAMESPACE, "hasDirection");

    public static Model createStatusModel(String did, List<Binding> status, OperationalStatus opStatus) {
        if(status.isEmpty()) {
            throw new IllegalStateException("unable to find binding");
        }
        Binding binding = status.getFirst();
        ValueFactory vf = Values.getValueFactory();
        // Create the Model
        Model model = new LinkedHashModel();
        // Define the resource IRI
        IRI resource = vf.createIRI(binding.valueID());
        // Add the pointName and value triples
        model.add(resource, POINT_NAME, vf.createLiteral(binding.pointName()));
        model.add(resource, POINT_ID, vf.createIRI(binding.pointID()));
        model.add(resource, TWIN_ID, vf.createIRI(did));

        IRI valueIRI = vf.createIRI(binding.valueID() + "_value");
        Binding.find(status, "value").ifPresent(b -> {
            model.add(resource, HAS_VALUE, valueIRI);
            model.add(valueIRI, VALUE_KEY, vf.createLiteral(b.valueKey()));
            model.add(valueIRI, PAYLOAD, vf.createLiteral(opStatus.opStatus()));
        });

        return model;

    }

    public static Model createLocationDataModel(String did, List<Binding> loc, LocationData locationData) {
        if(loc.isEmpty()) {
            throw new IllegalStateException("unable to find binding");
        }
        Binding binding = loc.getFirst();
        ValueFactory vf = Values.getValueFactory();

        // Create the Model
        Model model = new LinkedHashModel();

        // Define the resource IRI
        IRI resource = vf.createIRI(binding.valueID());

        // Add the geometry triple
        model.add(resource, POINT_NAME, vf.createLiteral(binding.pointName()));
        model.add(resource, POINT_ID, vf.createIRI(binding.pointID()));
        model.add(resource, TWIN_ID, vf.createIRI(did));

        IRI geomIRI = vf.createIRI(binding.valueID() + "_geometry");
        Binding.find(loc, "wktLiteral").ifPresent(b -> {
            model.add(resource, HAS_GEOMETRY, geomIRI);
            model.add(geomIRI, VALUE_KEY, vf.createLiteral(b.valueKey()));
            model.add(geomIRI, RDF.TYPE, SPATIAL_OBJECT);
            model.add(geomIRI, AS_WKT, vf.createLiteral(locationData.wktLiteral(), WKT_LITERAL));
        });

        IRI speedIRI = vf.createIRI(binding.valueID() + "_speed");
        Binding.find(loc, "speed").ifPresent(b -> {
            model.add(resource, HAS_SPEED, speedIRI);
            model.add(speedIRI, VALUE_KEY, vf.createLiteral(b.valueKey()));
            model.add(speedIRI, PAYLOAD, vf.createLiteral(locationData.speed()));
        });

        IRI directionIRI = vf.createIRI(binding.valueID() + "_direction");
        Binding.find(loc, "direction").ifPresent(b -> {
            model.add(resource, HAS_DIRECTION, directionIRI);
            model.add(directionIRI, VALUE_KEY, vf.createLiteral(b.valueKey()));
            model.add(directionIRI, PAYLOAD, vf.createLiteral(locationData.direction()));
        });

        return model;
    }
}
