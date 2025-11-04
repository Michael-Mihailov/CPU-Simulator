import java.util.*;

public class Cache // LRU replacement policy
{
    private int capacity;
    private CPU cpu;
    private int cacheID;
    
    private HashMap<Integer, Integer> cacheMap = new HashMap(); // where the data is stored
    // budget Linked List
    private HashMap<Integer, Integer> prevNodes = new HashMap();
    private HashMap<Integer, Integer> nextNodes = new HashMap();
    private int recentNode = -1; // most recent
    private int oldestNode = -1; // most old (to be evicted)
    
    public Cache(int capacity, CPU cpu, int cacheID)
    {
        this.capacity = capacity;
        this.cpu = cpu;
        this.cacheID = cacheID;
    }
    
    public int get(int key)
    {
        if (cacheMap.containsKey(key))
        {
            pruneNodes(key);
            return cacheMap.get(key);
        }
        return -1;
    }
    
    public void put(int key, int value)
    {
        cacheMap.put(key, value);
        if (cacheMap.size() == 1) oldestNode = key;
        pruneNodes(key);
    }
    
    public void evictNode(int key)
    {
        int nextKey = nextNodes.getOrDefault(key, -1);
        int prevKey = prevNodes.getOrDefault(key, -1);
        cacheMap.remove(key);
        prevNodes.remove(nextKey);
        nextNodes.remove(prevKey);
        prevNodes.remove(key);
        nextNodes.remove(key);
        if (nextKey != -1 && prevKey != -1)
        {
            nextNodes.put(prevKey, nextKey);
            prevNodes.put(nextKey, prevKey);
        }
    }
    
    private void pruneNodes(int key)
    {
        int oldNode = recentNode;
        int recentNode = key;
        
        if (recentNode == oldNode) return;
        
        if (recentNode == oldestNode) oldestNode = nextNodes.getOrDefault(oldestNode, recentNode);
        
        if (cacheMap.containsKey(recentNode))
        {
            int tempNext = nextNodes.getOrDefault(recentNode, -1);
            int tempPrev = prevNodes.getOrDefault(recentNode, -1);
            if (tempNext != -1)
            {
                prevNodes.remove(tempNext);
            }
            if (tempPrev != -1)
            {
                nextNodes.remove(tempPrev);
            }
            if (tempNext != -1 && tempPrev != -1)
            {
                nextNodes.put(tempPrev, tempNext);
                nextNodes.put(tempPrev, tempNext);
            }
            nextNodes.remove(recentNode);
            prevNodes.remove(recentNode);
        }
        if (oldNode != -1)
        {
            prevNodes.put(recentNode, oldNode);
            nextNodes.put(oldNode, recentNode);
        }
        
        if (cacheMap.size() <= capacity) return;
        // else evict a node
        memoryKickback(oldestNode, cacheMap.get(oldestNode));
        int nextOldNode = nextNodes.getOrDefault(oldestNode, -1);
        cacheMap.remove(oldestNode);
        prevNodes.remove(nextOldNode);
        nextNodes.remove(oldestNode);
        oldestNode = nextOldNode;
    }
    
    private void memoryKickback(int address, int data) // send the evicted data back to the CPU so it can store it in a lower tier of memory
    {
        RequestEntry temp = new RequestEntry(cpu.requestID(), -1, address, 3, data);
        temp.targetMemoryLayer = cacheID + 1;
        cpu.cacheWriteBack(temp);
    }
}