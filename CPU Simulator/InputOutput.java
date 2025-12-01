import java.util.*;

public class InputOutput
{
    private Bus bus;
    
    public InputOutput()
    {
        
    }
    
    public void setBut(Bus bus)
    {
        this.bus = bus;
    }
    
    public void sendInstructions(ArrayList<String> daScript) // send the script to the CPU one line at a time
    {
        for (int lineNum = 0; lineNum < daScript.size(); lineNum++)
        {
            RequestEntry entry = new RequestEntry(-lineNum, bus.busLatency + (daScript.size() - lineNum), lineNum, 0, daScript.get(lineNum));
            entry.writeFlag = true;
            
            if (lineNum == daScript.size() - 1) entry.complete = true;
            
            bus.uploadRequest(entry);
        }
    }
    
    public void receiveLog(RequestEntry entry) // receive logs from the CPU
    {
        System.out.println("Log #" + entry.id);
        System.out.println("Completion Cycle #" + entry.address);
        System.out.println("Instruction Completed: " + entry.strData);
    }
}