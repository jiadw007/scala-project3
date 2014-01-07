1 Problem denition
We talked extensively in class about the overlay networks and how they can be used to provide services. The goal of this project is to implement in Scala using the actor model the Pastry protocol and a simple object access service to prove its usefulness.
The specication of the Pastry protocol can be found in the paper Pastry: Scalable, decentralized object location and routing for large-scale peer-to-peer systems. by A. Rowstron and P. Druschel. You can find the paper at http://research.microsoft.com/en-us/um/people/antr/PAST/pastry.pdf.
The paper above, in section 2.3 contains a specication of the Pastry API
and of the API to be implemented by the application.

2 Requirements
You have to implement the network join and routing as described in the Pastry
paper and encode the simple application that associates a key (same as the ids
used in pastry) with a string. You can change the message type sent and the specfic activity as long as you implement it using a similar API to the one described in the paper.
Input: The input provided (as command line to your project3.scala) will be of the form:
project3.scala numNodes numRequests
Where numNodes is the number of peers to be created in the peer to peer system and numRequests the number of requests each peer has to make. When
all peers performed that many requests, the program can exit. Each peer should
send a request/second.
Output: Print the average number of hops (node connections) that have to be traversed to deliever a message.
Actor modeling: In this project you have to use exclusively the actor facility in Scala (projects that do not use multiple actors or use any other form of parallelism will receive no credit). You should have one actor for each of the peers modeled.

