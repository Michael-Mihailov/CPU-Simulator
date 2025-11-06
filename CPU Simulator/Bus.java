import java.util.*;

public class Bus
{
    private CPU cpu;
    private InputOutput inOut;
    private MainMemory mainMemory;
    
    public final int busLatency = 50; // made the same for all requests for simplicity
    
    private ArrayDeque<RequestEntry> busQueue = new ArrayDeque();
    
    public Bus(CPU cpu, InputOutput inOut, MainMemory mainMemory)
    {
        
    }
    
    public void cycle() // simulate movement along the bus
    {
        for (int i = 0; i < busQueue.size(); i++)
        {
            RequestEntry entry = busQueue.remove();
            entry.eta -= 1;
            
            if (entry.eta != 0)
            {
                busQueue.add(entry);
                continue;
            }
            
            int resData = 0;
            
            switch (entry.control)
            {
                case 0: // I/O to CPU
                    cpu.receiveInstruction(entry);
                    break;
                case 1: // CPU to I/O
                    inOut.receiveLog(entry);
                    break;
                case 2: // CPU to Main Memory READ
                    resData = mainMemory.getDword(entry.address);
                    break;
                case 3: // CPU to Main Memory WRITE
                    mainMemory.putDword(entry.address, entry.data);
            }
            
            // TODO
        }
    }
    
    /*
     * Handles requests from the CPU and I/O
     * 
     * Address: the location to lookup in memory (simulates the Address Bus)
     * Control: contains information about the type of operation (simulates the Control Bus)
     * 0 == program instruction from I/O to CPU
     * 1 == diagnostic data from CPU to I/O
     * 2 == read request from CPU to Main Memory
     * 3 == write request from CPU to Main Memory
     * 
     * Data: contains the data being transfered (simulates the Data Bus)
     */
    public void uploadRequest(RequestEntry entry)
    {
        busQueue.add(entry);
    }
}