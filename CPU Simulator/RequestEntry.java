public class RequestEntry
{
    public int id; // to identify the request
    public int eta; // the number of cycles remaining for completion
    
    public int address;
    public int control;
    public int data; // data sent to memory
    public String strData; // instructions sent to the CPU
    
    public boolean writeFlag = false; // read by default
    public boolean complete = false; // incomplete by default
    public int targetMemoryLayer = -1; // 0 == L1Cache, 1 == L2Cache, 2 == L3Cache, 3 == Main Memory
    
    public RequestEntry(int id, int eta, int address, int control, int data)
    {
        this.id = id; this.eta = eta; this.address = address; this.control = control; this.data = data;
    }
    public RequestEntry(int id, int eta, int address, int control, String strData)
    {
        this.id = id; this.eta = eta; this.address = address; this.control = control; this.strData = strData;
    }
}