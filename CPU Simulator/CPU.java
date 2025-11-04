import java.util.*;

public class CPU
{
    private Registers registers = new Registers();
    private Cache[] cacheArr = new Cache[3];
    private int[] cacheLatencies = new int[]{5, 10, 35};
    private ArrayList<RequestEntry> cacheQueue = new ArrayList<>(); // all the requests currently being sent to cache
    
    ArrayList<String> programInstructions = new ArrayList();
    
    PipelineEntry[] pipeline = new PipelineEntry[6]; // fetch inst., decode inst., calculate op., fetch op., execute inst., write op. 
    private HashSet<String> hazardSet = new HashSet(); // marks registers and addresses that are yet to be written to (RAW hazard)
    private ArrayDeque<RequestEntry> requestQueue = new ArrayDeque(); // buffer for received requestEntries
    
    private Bus bus;
    
    private int nextEntryID = 1;
    
    public CPU(Bus bus)
    {
        this.bus = bus;
        cacheArr[0] = new Cache(8, this, 0);
        cacheArr[1] = new Cache(16, this, 1);
        cacheArr[2] = new Cache(32, this, 2);
    }
    
    private void simulate()
    {
        // clock external stuff
        bus.cycle();
        clockCache();
        
        // update the pipeline
        for (int i = pipeline.length - 1; i >= 0; i--)
        {
            if (pipeline[i] != null) continue; // The pipeline has a stall
            if (i > 0 && pipeline[i] == null) continue; // there is nothing to pipeline
            
            if (i == 0) // (FI) Fetch Instruction
            {
                PipelineEntry temp = new PipelineEntry();
                temp.instruction = programInstructions.get( registers.getRegister("PC") ); // get the next instruction
                registers.setRegister("PC", registers.getRegister("PC") + 1); // increment the program counter
            }
            else if (i == 1) // (DI) Decode Instruction
            {
                pipeline[i].decodedInstruction = pipeline[i].instruction.split(" "); // turn the instruction into tokens
            }
            else if (i == 2) // (CO) Calculate Operands
            {
                pipeline[i].calculatedBuffer = new String[pipeline[i].decodedInstruction.length];
                // TODO: CONTINUE HERE
            }
        }
    }
    
    private void clockCache() // decrease eta of everything in the cacheQueue
    {
        for (int i = cacheQueue.size() - 1; i >= 0; i--)
        {
            RequestEntry entry = cacheQueue.get(i);
            
            entry.eta -= 1;
            if (entry.eta <= 0) // actualy interact with the cache
            {
                cacheQueue.remove(entry);
                if (entry.writeFlag == true) // write to cache
                {
                    cacheArr[entry.targetMemoryLayer].put(entry.address, entry.data);
                }
                else // read from cache
                {
                    int data = cacheArr[entry.targetMemoryLayer].get(entry.address);
                    if (data == -1) // cache miss
                    {
                        cacheWriteBack(entry);
                    }
                    else // cache hit
                    {
                        if (entry.targetMemoryLayer > 0) // promote to L1 cache
                        {
                            cacheArr[entry.targetMemoryLayer].evictNode(entry.address);
                            cacheArr[0].put(entry.address, data); // instantly promote to L1 cache (We can pretend that the promotion occured simultaniously as we were requesting the data from cache)
                        }
                        
                        entry.data = data;
                        entry.complete = true;
                        receiveMemory(entry);
                    }
                }
            }
        }
    }
    
    private void requestMemory(RequestEntry entry) // prepairs a read/write request to memory
    {
        if (entry.targetMemoryLayer <= 2) // check cache
        {
            cacheQueue.add(entry);
        }
        else // check main memory
        {
            bus.uploadRequest(entry);
        }
    }
    
    public void receiveMemory(RequestEntry entry) // deals with receiving completed memory requests
    {
        // TODO: unmark data hazards
        // TODO: update the relavent pipeline entries
    }
    
    public void receiveInstruction() // receives instructions from I/O
    {
        
    }
    
    public void cacheWriteBack(RequestEntry entry) // redirect cache data to a lower tier of memory
    {
        if (entry.targetMemoryLayer == 3) entry.eta = bus.busLatency;
        else entry.eta = cacheLatencies[entry.targetMemoryLayer];
        
        // TODO: mark the address in entry as a data hazard
        
        requestMemory(entry);
    }
    
    public int requestID()
    {
        int res = nextEntryID;
        nextEntryID++;
        return res;
    }
    
    private int doAlgebra(String equation)
    {
        for (int i = 0; i < equation.length(); i++)
        {
            if (equation.charAt(i) == '+')
            {
                String leftString = equation.substring(0, i);
                String rightString = equation.substring(i + 1);
                return doAlgebra(leftString) + doAlgebra(rightString);
            }
            if (equation.charAt(i) == '-')
            {
                String leftString = equation.substring(0, i);
                String rightString = equation.substring(i + 1);
                return doAlgebra(leftString) - doAlgebra(rightString);
            }
        }
        int mulIndex = equation.indexOf('*');
        if (mulIndex == -1)
        {
            String leftString = equation.substring(0, mulIndex);
            String rightString = equation.substring(mulIndex + 1);
            return doAlgebra(leftString) * doAlgebra(rightString);
        }
        return Integer.parseInt(equation);
    }
    
    private class PipelineEntry
    {
        public boolean stall = false; // in case of data hazards
        
        public String instruction; // (FI)
        public String[] decodedInstruction; // (DI)
        public String[] calculatedBuffer; // (CO)
    }
}