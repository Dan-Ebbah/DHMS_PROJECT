Distributed Health Care Management System
===========================================

This is a Fault-tolerant and highly available distributed system. Please see Design Doc.pdf to understand the design and the characteristics of the system. 

System Requirements:
----------------------
- Java 8 / JDK8

How to run:
-------------
- Use any local network such as WiFi to deploy the replica managers, replicas, sequencer, front ends, and test-clients.
- Start each replica manager on a different node, which will in turn start the replica on the node. Set the IP addresses of all the replica managers correctly depending on your network, before starting them.
- Each replica hosts 3 web services each on a different end point.
- Start the Sequencer on a different node. Make sure that the IP addresses of RMs are set correctly with in the Sequencer.
- Set the IP address of the Sequencer within the Front End and start Front Ends on multiple nodes, one for each test client.
- Set the IP address of the FE within the test client and start it. The test client sends requests to the Front End and we can see the logs of the test-clients to see whether the test cases ran successfully. Logs of the servers are available at each replica.