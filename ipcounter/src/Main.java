import com.ecwid.ipcounter.IpCounter;

public class Main {
    public static void main(String[] args) {
        String fileName;
        if (args.length>0){
            fileName = args[0];
        } else {
            fileName = "./ipcounter/src/ipAddresses.txt";
        }
        IpCounter counter = new IpCounter();
        long count = counter.countUniqueIp(fileName);
        System.out.println("Unique Ip addresses - "+count);
    }
}