package engine.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GmEventsJaxb {

    @XmlElement(name = "GM-event", required = true)
    private List<GmEventJaxb> eventList = new ArrayList<>();

    public List<GmEventJaxb> getEventList() {
        return eventList;
    }
}
