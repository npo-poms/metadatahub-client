import jakarta.xml.bind.JAXB;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.poms.*;
import nl.vpro.domain.media.Channel;

void main() throws Exception {
    System.setProperty("log4j2.root.level","INFO");

    var configuration = new Configuration();
    var client = configuration.createClient();
    var poms = new MetadataHubMediaService(client);

    JAXB.marshal(poms.getProgram( "POW_05977062"), System.out);

    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}
