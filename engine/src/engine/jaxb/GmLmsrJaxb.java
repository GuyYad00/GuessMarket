package engine.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GmLmsrJaxb {

    @XmlElement(name = "b", required = true)
    private int b;

    public int getB() {
        return b;
    }
}
