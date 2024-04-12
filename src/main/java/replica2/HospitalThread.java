package replica2;

import javax.xml.ws.Endpoint;

public class HospitalThread implements Runnable{
    String code;
    HospitalThread( String code){
        this.code = code;
    }

    @Override
    public void run() {
        Endpoint ep = Endpoint.publish(("http://localhost:8080/"+code),new AppointmentManagerImpl(code));
        System.out.println(code+" is running: " + ep.isPublished());
    }

}
