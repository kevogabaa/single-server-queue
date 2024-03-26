# Single-Server Queueing System

This project is a simulation of a single-server queueing system. It's written in Java and can be run in any environment that supports Java.

## Project Description

The simulation models a queueing system with a single server. It tracks various statistics such as the average delay in the queue, the average number in the queue, server utilization, and the time the simulation ended.

The system handles arrival and departure events. If the server is busy when a customer arrives, the customer is added to the queue. If the queue is full, the simulation stops. If the server is idle, the arriving customer is immediately served. When a customer is served, they leave the system and the next customer in the queue (if any) is served.

## Files

- `src/Main.java`: This is the main file that contains the logic for the simulation.
- `mm1.out`: This file contains the output of the simulation.

## How to Run

1. Compile the Java file: `javac src/Main.java`
2. Run the compiled file: `java src/Main`
3. Check the output in the `mm1.out` file.

## Output

The output of the simulation is written to the `mm1.out` file. It includes the following statistics:

- Average delay in queue
- Average number in queue
- Server utilization
- Time simulation ended

## Contributing

This is a simple project for learning purposes. Contributions are welcome. Please feel free to fork the project and submit a pull request with your changes.

## License

This project is licensed under the MIT License.