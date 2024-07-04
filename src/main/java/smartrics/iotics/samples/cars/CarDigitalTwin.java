package smartrics.iotics.samples.cars;

import com.iotics.api.GeoLocation;
import smartrics.iotics.connectors.twins.*;
import smartrics.iotics.connectors.twins.annotations.*;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.UriConstants;
import smartrics.iotics.identity.Identity;
import smartrics.iotics.identity.IdentityManager;

public class CarDigitalTwin extends AbstractTwin implements MappablePublisher, MappableMaker, AnnotationMapper {

    public static final String ONT_PREFIX = "https://ontologies.metaphacts.com/iotics-car-digital-twin";
    public static final String SCHEMA_PREFIX = "http://schema.org";

    @UriProperty(iri = UriConstants.IOTICSProperties.HostAllowListName)
    public final String visibility = UriConstants.IOTICSProperties.HostAllowListValues.ALL.toString();
    @UriProperty(iri = UriConstants.RDFProperty.Type)
    private final String type = ONT_PREFIX + "/CarDigitalTwin";
    @LiteralProperty(iri = ONT_PREFIX + "/unit", dataType = XsdDatatype.int_)
    private Integer unit;
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private String comment;
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    private String label;
    @StringLiteralProperty(iri = ONT_PREFIX + "/manufacturerName")
    private String manufacturerName;
    @StringLiteralProperty(iri = SCHEMA_PREFIX + "/color")
    private String colour;
    @StringLiteralProperty(iri = ONT_PREFIX + "/model")
    private String model;
    @StringLiteralProperty(iri = SCHEMA_PREFIX + "/identifier")
    private String identifier;
    @StringLiteralProperty(iri = SCHEMA_PREFIX + "/givenName")
    private String owner;
    @LiteralProperty(iri = ONT_PREFIX + "/isOperational", dataType = XsdDatatype.boolean_)
    private Boolean isOperational;
    @Location
    private GeoLocation location;

    private MovingCar movingCar;

    CarDigitalTwin(IoticsApi api, IdentityManager sim, Identity myIdentity) {
        super(api, sim, myIdentity);
    }

    @Feed(id = "status")
    public OperationalStatus getOpStatus() {
        return this.movingCar.currentOperationalStatus();
    }

    @Feed(id = "locationData")
    public LocationData getLocationData() {
        return this.movingCar.currentLocationData();
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Mapper getMapper() {
        return this;
    }

    public void updateState() {
        this.movingCar.update();
    }

    public static final class CarDigitalTwinBuilder {
        private Integer unit;
        private String comment;
        private String label;
        private String manufacturerName;
        private String colour;
        private String model;
        private String identifier;
        private String owner;
        private Boolean isOperational;
        private GeoLocation location;
        private Identity myIdentity;
        private IoticsApi api;
        private IdentityManager sim;

        private CarDigitalTwinBuilder() {
        }

        public static CarDigitalTwinBuilder aCarDigitalTwin() {
            return new CarDigitalTwinBuilder();
        }

        public CarDigitalTwinBuilder withUnit(Integer unit) {
            this.unit = unit;
            return this;
        }

        public CarDigitalTwinBuilder withComment(String comment) {
            this.comment = comment;
            return this;
        }

        public CarDigitalTwinBuilder withLabel(String label) {
            this.label = label;
            return this;
        }

        public CarDigitalTwinBuilder withManufacturerName(String manufacturerName) {
            this.manufacturerName = manufacturerName;
            return this;
        }

        public CarDigitalTwinBuilder withColour(String colour) {
            this.colour = colour;
            return this;
        }

        public CarDigitalTwinBuilder withModel(String model) {
            this.model = model;
            return this;
        }

        public CarDigitalTwinBuilder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public CarDigitalTwinBuilder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public CarDigitalTwinBuilder withIsOperational(Boolean isOperational) {
            this.isOperational = isOperational;
            return this;
        }

        public CarDigitalTwinBuilder withLocation(GeoLocation location) {
            this.location = location;
            return this;
        }

        public CarDigitalTwinBuilder withMyIdentity(Identity myIdentity) {
            this.myIdentity = myIdentity;
            return this;
        }

        public CarDigitalTwinBuilder withApi(IoticsApi api) {
            this.api = api;
            return this;
        }

        public CarDigitalTwinBuilder withSim(IdentityManager sim) {
            this.sim = sim;
            return this;
        }

        public CarDigitalTwin build() {
            CarDigitalTwin carDigitalTwin = new CarDigitalTwin(api, sim, myIdentity);
            carDigitalTwin.label = this.label;
            carDigitalTwin.identifier = this.identifier;
            carDigitalTwin.owner = this.owner;
            carDigitalTwin.isOperational = this.isOperational;
            carDigitalTwin.comment = this.comment;
            carDigitalTwin.unit = this.unit;
            carDigitalTwin.location = this.location;
            carDigitalTwin.manufacturerName = this.manufacturerName;
            carDigitalTwin.colour = this.colour;
            carDigitalTwin.model = this.model;
            carDigitalTwin.movingCar = new MovingCar(this.location.getLat(), this.location.getLon());
            carDigitalTwin.movingCar.update();
            return carDigitalTwin;
        }
    }
}
