import java.util.*;

public class Main
{
    private static final int memoryCapacity = 1024;
    
    public static void main(String[] args)
    {
        // Initialize stuff
        Scanner input = new Scanner(System.in);
        
        CPU cpu = new CPU();
        InputOutput inputOutput = new InputOutput();
        MainMemory mainMemory = new MainMemory(memoryCapacity);
        Bus bus = new Bus(cpu, inputOutput, mainMemory);
        
        cpu.setBut(bus);
        inputOutput.setBut(bus);
        
        // read the script from the user
        ArrayList<String> script = new ArrayList();
        String line = "";
        System.out.println("Please type your script here. End the script by typing a single \"#\"");
        do
        {
            line = input.nextLine();
            script.add(line);
        }
        while (line.compareTo("#") != 0);
        script.removeLast();
        
        inputOutput.sendInstructions(script);
        while (cpu.shutdown == false || cpu.executingScript == true)
        {
            cpu.simulate();
        }
        
        System.out.println("END OF SIMULATION!");
    }
}