import org.apache.activemq.artemis.api.core.client.FailoverEventListener;
import org.apache.activemq.artemis.api.core.client.FailoverEventType;

public class FailOverListenerImpl implements FailoverEventListener {
    @Override
    public void failoverEvent(FailoverEventType failoverEventType) {
        System.out.println("Failover called for Receiver : "+failoverEventType.toString());
    }
}
