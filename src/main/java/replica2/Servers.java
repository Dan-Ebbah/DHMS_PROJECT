package replica2;

import java.util.ArrayList;

public class Servers
{
    public static void main(String args[]){
        //set up a thread for each server and start them
        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(makeServer("MTL"));
        threads.add(makeServer("QUE"));
        threads.add(makeServer("SHE"));
        for(Thread thread: threads){
            thread.start();
        }

    }
    public static Thread makeServer(String name){
        return new Thread(new HospitalThread(name));
    }
}
