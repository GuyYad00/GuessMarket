package engine.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GmMethodJaxb {

    @XmlElement(name = "GM-LMSR", required = true)
    private GmLmsrJaxb lmsr;

    public GmLmsrJaxb getLmsr() {
        return lmsr;
    }
}
