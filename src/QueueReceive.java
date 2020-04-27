import org.apache.activemq.artemis.jms.client.ActiveMQConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class QueueReceive implements MessageListener, Runnable {
    public final String JNDI_FACTORY = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";

    //*************** Connection Factory JNDI name *************************
    public final String JMS_FACTORY = "ConnectionFactory";

    //*************** Remote enabled Queue JNDI name *************************
    public final static String QUEUE = "dynamicQueues/mytest-queue-b3";

    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueReceiver qreceiver;
    private Queue queue;
    private boolean quit = false;
    private int counter = 1;
    private String brokerURL = null;
    private String username = null;
    private String password = null;
    private String userQueue = null;
    private long msgCount = -1;

    /*public void onMessage(Message msg) {
        try {
            String msgText;
            if (msg instanceof TextMessage) {
                msgText = ((TextMessage) msg).getText();
            } else {
                msgText = msg.toString();
            }
            if (msgText.equalsIgnoreCase("hello world")) {
                System.out.println("\n\t " + msgText + ", hence rolling back");
                // Here we are rolling back the session.
                // The "Hello World" message which we received is not committed, hence it's undelivered and goes back to the TestQ
                qsession.rollback();
            } else {
                System.out.println("\n\t " + msgText);
                // Here we are committing the session to acknowledge that we have received the message from the TestQ
                qsession.commit();
            }

            if (msgText.equalsIgnoreCase("quit")) {
                synchronized (this) {
                    quit = true;
                    this.notifyAll();
                }
            }
        } catch (JMSException jmse) {
            jmse.printStackTrace();
        }
    }*/

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserQueue() {
        return userQueue;
    }

    public void setUserQueue(String userQueue) {
        this.userQueue = "dynamicQueues/"+userQueue;
    }

    public long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(long msgCount) {
        this.msgCount = msgCount;
    }

    public QueueReceive(String brokerURL, String username, String password, String userQueue, long msgCount) {
        this.brokerURL = brokerURL;
        this.username = username;
        this.password = password;
        this.userQueue = "dynamicQueues/"+userQueue;
        this.msgCount = msgCount;
    }

    @Override
    public void onMessage(Message message) {
        try {
//            System.out.println("Message Received : " + counter);
            qsession.commit();
            if (this.counter==this.getMsgCount()){
//                System.out.println("Condition reached : " + counter + this.getMsgCount());
                synchronized (this) {
                    quit = true;
                    this.notifyAll();
                }
            }
            this.counter++;
            if (this.counter % 1000 == 0){
                System.out.println("Message Received : " + this.counter);
            }
        }catch (JMSException e){
            e.printStackTrace();
        }
    }

    public void init(Context ctx, String queueName) throws NamingException, JMSException {
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);

        //*************** Creating Queue Connection using the UserName & Password *************************
        qcon = qconFactory.createQueueConnection(this.getUsername(), this.getPassword());            //<------------- Change the UserName & Password

        //Creating a *transacted* JMS Session to test our DLQ
        qsession = qcon.createQueueSession(true, 0);

        queue = (Queue) ctx.lookup(queueName);
        qreceiver = qsession.createReceiver(queue);
        qreceiver.setMessageListener(this);
        ((ActiveMQConnection)qcon).setFailoverListener(new FailOverListenerImpl());
        qcon.start();
    }

    public void close() throws JMSException {
        qreceiver.close();
        qsession.close();
        qcon.close();
    }

    private InitialContext getInitialContext(String url, String username, String password) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, url);

//*************** UserName & Password for the Initial Context for JNDI lookup *************************
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        return new InitialContext(env);
    }

    @Override
    public void run() {
//        QueueReceive qr = new QueueReceive();
        InitialContext ic = null;
        try {
//            System.out.println("Broker URL: " + getBrokerURL());
            ic = getInitialContext(getBrokerURL(), getUsername(), getPassword());
            init(ic, getUserQueue());
//            System.out.println("JMS Ready To Receive Messages from "+ getUserQueue());

//            synchronized (this) {
                while (!quit) {
                   /* try {
//                        this.wait();
                    } catch (InterruptedException ie) {
                    }
                    if (counter % 1000 == 0){
                        System.out.println(counter + " messages receieved ");
                    }*/
                }
//            }
            close();
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }

    }
}
