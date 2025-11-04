import java.util.*;

public class MainMemory
{
    private int capacity;
    private byte[] memory;

    public MainMemory(int capacity)
    {
        this.capacity = capacity;
        memory = new byte[capacity];
    }

    public byte getByte(int key)
    {
        return memory[key % capacity];
    }
    public int getDword(int key)
    {
        int res = 0;
        for (int i = 0; i < 4; i++)
        {
            int tempValue = memory[(key + i) % capacity];
            if (tempValue < 0) tempValue = (-1 * tempValue) + Byte.MAX_VALUE;
            res += tempValue * Math.pow(2, 8 * (3 - i));
        }
        return res;
    }
    
    public void putByte(int key, byte value)
    {
        memory[key % capacity] = value;
    }
    public void putDword(int key, int value)
    {
        long tempValue = value;
        if (value < 0) tempValue = (-1 * tempValue) + Integer.MAX_VALUE;
        for (int i = 0; i < 4; i++)
        {
            memory[(key + (3 - i)) % capacity] = (byte)((tempValue % 256) - 128); 
            tempValue /= 256;
        }
    }
}