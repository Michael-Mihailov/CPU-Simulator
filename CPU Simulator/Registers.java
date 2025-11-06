import java.util.*;

public class Registers
{
    private HashMap<String, Integer> registerMap = new HashMap(); // map storing the 32-bit registers
    private HashSet<String> userRegisters = new HashSet(); // EAX, EBX, ECX, EDX
    
    public Registers()
    {
        registerMap.put("EAX", 0); userRegisters.add("EAX");
        registerMap.put("EBX", 0); userRegisters.add("EBX");
        registerMap.put("ECX", 0); userRegisters.add("ECX");
        registerMap.put("EDX", 0); userRegisters.add("EDX");
        
        registerMap.put("PC", 0);
        registerMap.put("OVERFLOW", 0);
        registerMap.put("ZERO", 0);
    }
    
    public boolean isUserRegister(String name)
    {
        return userRegisters.contains(name);
    }
    public HashSet<String> viewUserRegisters()
    {
        return new HashSet(userRegisters);
    }
    
    public int getRegister(String name)
    {
        return registerMap.getOrDefault(name, 0);
    }
    
    public void setRegister(String name, int value)
    {
        if (registerMap.containsKey(name))
        {
            registerMap.put(name, value);
        }
    }
}