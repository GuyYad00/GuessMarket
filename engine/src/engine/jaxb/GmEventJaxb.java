package engine.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GmEventJaxb {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "id", required = true)
    private int id;

    @XmlElement(name = "description", required = true)
    private String description;

    // Note: "comision" is intentionally misspelled - this is the element name in the official schema
    @XmlElement(name = "comision", required = true)
    private ComisionJaxb comision;

    @XmlElement(name = "GM-options", required = true)
    private GmOptionsJaxb options;

    @XmlElement(name = "GM-method", required = true)
    private GmMethodJaxb method;

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public ComisionJaxb getComision() {
        return comision;
    }

    public GmOptionsJaxb getOptions() {
        return options;
    }

    public GmMethodJaxb getMethod() {
        return method;
    }
}
