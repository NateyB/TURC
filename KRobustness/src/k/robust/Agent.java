package k.robust;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jonb3_000
 */
public class Agent {
    
    public final int ID;
    protected Task[] tasks;
    public final double cost;
    
    public Agent(int id, Task[] tasks, double c) {
        ID = id;
        this.tasks = tasks;
        cost = c;
    }
    
    public final Task[] getTasks() {
        return tasks;
    }
    
    public String toString() {
        return "" + ID;
    }
    
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append(ID + ":");
        for (Task t : tasks) {
            sb.append(t);
            sb.append(" ");
        }
        sb.append("\t");
        sb.append(cost);
        return sb.toString();
    }
    
}
