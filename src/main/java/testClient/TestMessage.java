package testClient;

public class TestMessage {
    private int result;
    public TestMessage(String testName, String expected, String received){
        System.out.println(testName);

        if(expected.equals("true")){
            expected = "Successful";
        }
        if(expected.equals("false")){
            expected = "Failure";
        }
        System.out.println("Expected: "+expected);
        System.out.println("Received: "+received);
        if(expected.toUpperCase().equals(received.toUpperCase())){
            result=1;
        }else{
            result=0;
        }

    }

    public TestMessage(String testName, String expected, Boolean received){

        this(testName,expected,expected);
    }
    public TestMessage(String testName, String[] expectedSplit, String received){
        System.out.println(testName);
        String[] receivedSplit = received.split(",");
        System.out.print("Expected: ");
        for(int i=0;i< expectedSplit.length;i++){
            System.out.print(expectedSplit[i]+" ");
        }
        System.out.println();

        System.out.print("Received: ");
        for (int i=0;i<receivedSplit.length;i++){
            System.out.print(receivedSplit[i]+" ");
        }
        System.out.println();
        boolean found=false;
        result=1;
        if(expectedSplit.length!=receivedSplit.length){
            result=0;
        }
        if(expectedSplit.length==0 && receivedSplit.length==1){
            result=1;
        }
        for(String s1 : expectedSplit){
            found=false;
            for(String s2 : receivedSplit){
                if(s1.toUpperCase().equals(s2.toUpperCase())){
                    found=true;
                    break;
                }
            }
            if(!found){
                result=0;
                break;
            }
        }
    }
    public int getResult(){
        return result;
    }
}