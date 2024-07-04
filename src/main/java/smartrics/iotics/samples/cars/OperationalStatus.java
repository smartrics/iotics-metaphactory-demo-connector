package smartrics.iotics.samples.cars;

import smartrics.iotics.connectors.twins.annotations.PayloadValue;
import smartrics.iotics.connectors.twins.annotations.StringLiteralProperty;
import smartrics.iotics.connectors.twins.annotations.XsdDatatype;
import smartrics.iotics.host.UriConstants;

public class OperationalStatus {
    @PayloadValue(label = "value", dataType = XsdDatatype.boolean_)
    private final Boolean opStatus;
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Label)
    private String label = "Operational Status";
    @StringLiteralProperty(iri = UriConstants.RDFSProperty.Comment)
    private String comment = "Car Operational Status";

    public OperationalStatus(Boolean opStatus) {
        this.opStatus = opStatus;
    }

    public Boolean opStatus() {
        return opStatus;
    }

    @Override
    public String toString() {
        return "OperationalStatus{" +
                "opStatus=" + opStatus +
                '}';
    }
}
