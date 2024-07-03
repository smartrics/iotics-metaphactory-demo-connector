package smartrics.iotics.samples.cars;

import smartrics.iotics.connectors.twins.annotations.PayloadValue;
import smartrics.iotics.connectors.twins.annotations.StringLiteralProperty;
import smartrics.iotics.connectors.twins.annotations.XsdDatatype;
import smartrics.iotics.host.UriConstants;

public class LocationData {
    @PayloadValue(label = "wktLiteral", dataType = XsdDatatype.string)
    private final String wktLiteral;
    @PayloadValue(label = "speed", dataType = XsdDatatype.double_)
    private final double speed;
    @PayloadValue(label = "direction", dataType = XsdDatatype.double_)
    private final double direction;

    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    private String label = "LocationData";
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private String comment = "Car Location Data";

    public LocationData(double lat, double lon, double speed, double direction) {
        this.wktLiteral = "POINT("+ lat + " " + lon + ")";
        this.direction = direction;
        this.speed = speed;
    }

    public String wktLiteral() {
        return wktLiteral;
    }

    public double speed() {
        return speed;
    }

    public double direction() {
        return direction;
    }
}
