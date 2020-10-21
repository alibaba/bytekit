import java.util.concurrent.TimeUnit;

public class AAA {

    public static void main(String[] args) throws InterruptedException {
        
        
        for(int i = 0 ;;++i) {
            
            TimeUnit.SECONDS.sleep(1);
            
            AAA aaa  = new AAA("sss" + (i%2));
            
            System.err.println(aaa);
        }
        
        
    }
    
    
    private AAA(String sss) {
    }
}
