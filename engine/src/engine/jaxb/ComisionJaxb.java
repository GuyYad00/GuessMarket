package engine.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComisionJaxb {

    @XmlValue
    private int percentage;

    @XmlAttribute(name = "type", required = true)
    private String type;

    public int getPercentage() {
        return percentage;
    }

    public String getType() {
        return type;
    }
}
