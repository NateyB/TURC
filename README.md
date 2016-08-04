# TURC

TURC stands for Tulsa Undergraduate Research Challenge. It's a program at the University
of Tulsa giving undergraduates the opportunity to involve themselves with actual
research. I'm participating in TURC this summer of 2016 with my mentor as Dr. Sandip Sen,
a computer science professor acquainted with artificial intelligence & machine learning.


## KRobustness
### Description
The K-Robust problem is a generalization of the set-covering problem. The problem is to
minimize the resources spent while still selecting a k-robust team of agents, each of
which has a cost. A k-robust team of agents is a team wherein, given a set of tasks, if
you remove any *k* agents from the team, the team is still able to perform all of the
tasks.

A quick example would be if you need agents who can vacuum (like a roomba), and another
agent who can cook (like my roommate), and you have exactly 1 of each, then you have a 
*0 robust* team because you cannot remove any agents and still be able to perform both 
tasks. If you add three agents who can vacuum and two agents who can cook, then you have 
a *2 robust* team, because you can still perform vacuuming *and* cooking if you remove
the only two agents who can cook (but your team is not *3 robust* because you cannot
remove the three cooking agents and still be able to cook.

### My heuristic
My heuristic for this problem was trifold:
 * Assign to each task an average cost based off of the cost of agents who can perform
 that task, and the number of tasks that each of those agents can perform.
 * Weight the desire for each task by the number of agents still missing who are needed
 to be able to perform that task. This weight is designed to ameliorate myopism.
 * Greedily select each agent according to sum of the need of each task it can perform
 times the task's cost, multiplied by the number of tasks that the agent can perform to
 be finally divided by the agent's cost.

### Future Improvements
 * The calculation could be better. There's no point to multiply the sum of the need
 times the cost of each task by the number of tasks that the agent can perform.


## ANAC
### Description
The Automated Negotiating Agents Competition, or ANAC, is an annual competition whose 
participants design an agent to negotiate with other participants' agents for differing
weights and priorities on resources.

### My strategy
The following principle was the basis of my agent: Consider the upper bound of the
possible utility that all agents can achieve as a function of time and knowledge of
each opponent. Then, if the current deal is less than that upper bound, try to gain
more knowledge to offer a better deal which is more likely to be accepted by all parties.

### Future Improvements
 * Improve the utility calculation function


## MinecraftAI
### Description
Microsoft's open source [Project Malmo](https://github.com/Microsoft/malmo) enables
developers to create automated bots for the video game Minecraft, a wonderful environment
for experimenting with artificial intelligence. I intend to use this world as my 
playground for various AI research.

### My research
Coming soon!

## NormEmergence
### Description
The Norm Emergence project that I've worked on includes generating network topologies
from starting parameter values. The software that I've created here takes in an input
file specifying nodes and their dependencies (which, for this project, is just a way
of representing all possible logical orderings of payoffs), constructs a graph,
places a linking cost somewhere in the orderings, and if the placement is unique
(i.e., the tuple of the sets of all nodes ranked more highly and all nodes ranked
more lowly is unique), then a topology is constructed from that ordering.