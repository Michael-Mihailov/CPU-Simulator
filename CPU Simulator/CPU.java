import java.util.*;

public class CPU
{
    private Registers registers = new Registers();
    private Cache[] cacheArr = new Cache[3];
    private int[] cacheLatencies = new int[]{5, 10, 35};
    private ArrayList<RequestEntry> cacheQueue = new ArrayList<>(); // all the requests currently being sent to cache
    
    ArrayList<String> programInstructions = new ArrayList();
    
    PipelineEntry[] pipeline = new PipelineEntry[6]; // fetch inst., decode inst., calculate op., fetch op., execute inst., write op., write bk. 
    private HashSet<String> hazardSet = new HashSet(); // marks registers and addresses that are yet to be written to (RAW hazard)
    private ArrayDeque<RequestEntry> requestQueue = new ArrayDeque(); // buffer for received requestEntries
    
    private Bus bus;
    
    private int nextEntryID = 1;
    private int logNum = 1; // the ID# of the next log
    private int cycleNum = 0; // The current clock cycle number
    
    
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
            PipelineEntry currentPipeEntry = pipeline[i];
            
            if (pipeline[i] != null) continue; // The pipeline has a stall
            if (i > 0 && pipeline[i] == null) continue; // there is nothing to pipeline
            if (pipeline[i].stall == true) continue; // wait for the stall to be resolved
            
            if (i == 0) // (FI) Fetch Instruction
            {
                currentPipeEntry = new PipelineEntry();
                currentPipeEntry.instruction = programInstructions.get( registers.getRegister("PC") ); // get the next instruction
                registers.setRegister("PC", registers.getRegister("PC") + 1); // increment the program counter
                
                pipeline[i] = currentPipeEntry; // add a new entry to the pipeline
            }
            else if (i == 1) // (DI) Decode Instruction
            {
                currentPipeEntry.decodedInstruction = currentPipeEntry.instruction.split(" "); // turn the instruction into tokens
                
                // mark register as a hazard if need
                String temp = currentPipeEntry.decodedInstruction[1];
                if ((temp.charAt(0) == '[' && temp.charAt(temp.length()-1) == ']') == false) // check if the destination is NOT memory
                {
                    hazardSet.add(temp);
                    currentPipeEntry.exclusiveSet.add(temp); // NOTE: POSSIBLE ERRORS
                }
                
                pipeline[i] = currentPipeEntry;
                pipeline[i-1] = null;
            }
            else if (i == 2) // (CO) Calculate Operands
            {
                currentPipeEntry.calculatedBuffer = new String[currentPipeEntry.decodedInstruction.length];
                
                for (int j = 0; j < currentPipeEntry.calculatedBuffer.length; j++)
                {
                    String temp = currentPipeEntry.decodedInstruction[j];
                    // replace registers with their values (and check for hazards)
                    for (String userRegister : registers.viewUserRegisters())
                    {
                        if (temp.contains(userRegister))
                        {
                            if (hazardSet.contains(userRegister) && currentPipeEntry.exclusiveSet.contains(userRegister) == false) // check if the register is a hazard
                            {
                                currentPipeEntry.stall = true;
                                currentPipeEntry.stallCondition.add(userRegister);
                            }
                            else // not a data hazard
                            {
                                temp.replaceAll(userRegister, ""+registers.getRegister(userRegister));
                            }
                        }
                    }
                    
                    if (temp.charAt(0) == '[' && temp.charAt(temp.length()-1) == ']') // check if it is memory
                    {
                        temp = temp.substring(1, temp.length() - 1);
                        temp = "[" + doAlgebra(temp) + "]";
                        
                        if (j == 1) // flag data hazard
                        {
                            hazardSet.add(temp);
                            currentPipeEntry.exclusiveSet.add(temp); // NOTE: POSSIBLE ERRORS
                        }
                    }
                    
                    currentPipeEntry.calculatedBuffer[j] = temp;
                }
                
                // mark memory as a hazard if needed
                String temp = currentPipeEntry.calculatedBuffer[1];
                if (temp.charAt(0) == '[' && temp.charAt(temp.length()-1) == ']') // check if it is memory
                {
                    temp = temp.substring(1, temp.length() - 1);
                    hazardSet.add(temp);
                }
                
                pipeline[i] = currentPipeEntry;
                pipeline[i-1] = null;
            }
            else if (i == 3) // (FO) Fetch Operands
            {
                for(int j = 1; i < currentPipeEntry.calculatedBuffer.length; i++)
                {
                    String temp = currentPipeEntry.calculatedBuffer[j];
                    
                    if (temp.charAt(0) == '[' && temp.charAt(temp.length()-1) == ']') // check if it is memory
                    {
                        // check what the address is
                        temp = temp.substring(1, temp.length() - 1);
                        int address = Integer.parseInt(temp);
                        
                        // check for hazards
                        if (hazardSet.contains(address + "") && currentPipeEntry.exclusiveSet.contains(address + "") == false)
                        {
                            currentPipeEntry.stall = true;
                            currentPipeEntry.stallCondition.add(address + "");
                        }
                        
                        // more stuff for creating request
                        int daID = requestID();
                        int eta = cacheLatencies[0];
                        int control = -1;
                        int data = 0;
                        RequestEntry tempEntry = new RequestEntry(daID, eta, address, control, data);
                        tempEntry.targetMemoryLayer = 0;
                        requestMemory(tempEntry);
                        
                        // put the id into the fetchBuffer temporarily
                        currentPipeEntry.fetchBuffer[j] = "#" + daID;
                    }
                    else
                    {
                        currentPipeEntry.fetchBuffer[j] = temp;
                    }
                }
                
                pipeline[i] = currentPipeEntry;
                pipeline[i-1] = null;
            }
            else if (i == 4) // (EI) Execute Instructions
            {
                int temp = 0;
                if (currentPipeEntry.fetchBuffer[0] == "ADD")
                {
                    temp = Integer.parseInt(currentPipeEntry.fetchBuffer[1]) + Integer.parseInt(currentPipeEntry.fetchBuffer[2]);
                }
                else if (currentPipeEntry.fetchBuffer[0] == "SUB")
                {
                    temp = Integer.parseInt(currentPipeEntry.fetchBuffer[1]) - Integer.parseInt(currentPipeEntry.fetchBuffer[2]);
                }
                else if (currentPipeEntry.fetchBuffer[0] == "LOAD")
                {
                    temp = Integer.parseInt(currentPipeEntry.fetchBuffer[2]);
                }
                else if (currentPipeEntry.fetchBuffer[0] == "STORE")
                {
                    temp = Integer.parseInt(currentPipeEntry.fetchBuffer[2]);
                }
                else if (currentPipeEntry.fetchBuffer[0] == "HALT")
                {
                    // TODO: DO LATER
                }
                
                currentPipeEntry.executedValue = temp;
                
                pipeline[i] = currentPipeEntry;
                pipeline[i-1] = null;
            }
            else if (i == 5) // (WO) Write Operand
            {
                String destination = currentPipeEntry.calculatedBuffer[1];
                if (destination.charAt(0) == '[' && destination.charAt(destination.length()-1) == ']') // check if it is memory
                {
                    // write to memory
                    destination = destination.substring(1, destination.length() - 1);
                    int daID = requestID();
                    int eta = cacheLatencies[0];
                    int control = -1;
                    int data = 0;
                    RequestEntry tempEntry = new RequestEntry(daID, eta, Integer.parseInt(destination), control, data);
                    tempEntry.writeFlag = true;
                    tempEntry.targetMemoryLayer = 0;
                    requestMemory(tempEntry);
                }
                else // it is a register
                {
                    registers.setRegister(destination, currentPipeEntry.executedValue);
                    hazardSet.remove(destination); // remove the hazard
                }
                
                pipeline[i] = currentPipeEntry;
                pipeline[i-1] = null;
            }
            else if (i == 6) // (WB) Write Back
            {
                int daID = cycleNum; // send over the current clock cycle
                int eta = bus.busLatency;
                int address = logNum; logNum++;
                int control = 1;
                String strData = currentPipeEntry.instruction;
                RequestEntry tempEntry = new RequestEntry(daID, eta, address, control, strData);
                tempEntry.writeFlag = true;
                
                bus.uploadRequest(tempEntry); // Send a log to I/O
                
                pipeline[i-1] = null;
            }
        }
    }
    
    private class PipelineEntry
    {
        public boolean stall = false; // in case of data hazards
        public HashSet<String> stallCondition = new HashSet(); // the address/name of the data needed to resolve the stall
        
        public HashSet<String> exclusiveSet = new HashSet(); // The data hazards that ONLY THIS ENTRY can access
        
        public String instruction; // (FI)
        public String[] decodedInstruction; // (DI)
        public String[] calculatedBuffer; // (CO)
        public String[] fetchBuffer; // (FO)
        public int executedValue = 0;
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
            if (entry.writeFlag == false) entry.control = 2;
            else entry.control = 3;
            bus.uploadRequest(entry);
        }
    }
    
    public void receiveMemory(RequestEntry entry) // deals with receiving completed memory requests
    {
        // TODO: unmark data hazards
        // TODO: update the relavent pipeline entries
        
    }
    
    public void receiveInstruction(RequestEntry entry) // receives instructions from I/O
    {
        String line = entry.strData;
        programInstructions.add(line);
        
        if (entry.complete == true)
        {
            // TODO: Start the actual programm
        }
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
}