import com.ecwid.ipcounter.IpCounter;

public class Main {
    public static void main(String[] args) {
        String fileName = "./ipcounter/src/ipAddresses.txt";
        IpCounter counter = new IpCounter();
        long count = counter.countUniqueIp(fileName);
        System.out.println("Unique Ip addresses - "+count);
    }
}