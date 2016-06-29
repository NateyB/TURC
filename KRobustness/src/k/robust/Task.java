package k.robust;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jonb3_000
 */
public class Task {
    
    public final int ID;
    
    public Task(int t) {
        ID = t;
    }
    
    @Override
    public final boolean equals(Object other) {
        return other instanceof Task && ((Task) other).ID == ID;
    }
    
    public String toString() {
        return "" + ID;
    }
    
}
