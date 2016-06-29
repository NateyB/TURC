package k.robust;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author jonb3_000
 */
public interface TeamFinderInterface
{

    /**
     * Finds a team with the specified robustness.
     *
     * @param k The robustness of the team.
     * @return A k-robust team of agents.
     */
    TeamInterface findTeam(int k);

}
