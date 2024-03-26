import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for the simulation of a single-server queueing system.
 */
public class Main {
    // Constants for the queue limit and server status
    private static final int Q_LIMIT = 100;
    private static final int BUSY = 1;
    private static final int IDLE = 0;

    // Variables for the simulation
    private static int next_event_type;
    private static int num_custs_delayed;
    private static int num_events;
    private static int num_in_q;
    private static int server_status;
    private static float area_num_in_q, area_server_status, mean_interarrival, mean_service,
            sim_time, total_of_delays;
    private static final float[] time_arrival = new float[Q_LIMIT + 1];
    private static float time_last_event;
    private static final float[] time_next_event = new float[3];
    private static File outfile;

    // Random number generator
    private static final Random random = new Random();

    // Logger for error handling
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Main method for the simulation.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            // Open input and output files
            File infile = new File("mm1.in");
            outfile = new File("mm1.out");
            FileWriter writer = new FileWriter(outfile);

            // Specify the number of events for the timing function
            num_events = 2;

            // Read input parameters
            Scanner scanner = new Scanner(infile);
            mean_interarrival = scanner.nextFloat();
            mean_service = scanner.nextFloat();
            int num_delays_required = scanner.nextInt();
            scanner.close();

            // Write report heading and input parameters
            writer.write("Single-server queueing system\n\n");
            writer.write(String.format("Mean interarrival time%11.3f minutes\n\n", mean_interarrival));
            writer.write(String.format("Mean service time%16.3f minutes\n\n", mean_service));
            writer.write(String.format("Number of customers%14d\n\n", num_delays_required));

            // Initialize the simulation
            initialize();

            // Run the simulation while more delays are still needed
            while (num_custs_delayed < num_delays_required) {
                // Determine the next event
                timing();

                // Update time-average statistical accumulators
                update_time_avg_stats();

                // Invoke the appropriate event function
                switch (next_event_type) {
                    case 1:
                        arrive();
                        break;
                    case 2:
                        depart();
                        break;
                }
            }

            // Invoke the report generator and end the simulation
            report(writer);
            writer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An IO exception occurred", e);
        }
    }

    /**
     * Initialize the simulation.
     */
    private static void initialize() {
        // Initialize the simulation clock
        sim_time = 0.0f;

        // Initialize the state variables
        server_status = IDLE;
        num_in_q = 0;
        time_last_event = 0.0f;

        // Initialize the statistical counters
        num_custs_delayed = 0;
        total_of_delays = 0.0f;
        area_num_in_q = 0.0f;
        area_server_status = 0.0f;

        // Initialize event list. Since no customers are present, the departure (service completion) event is eliminated from consideration.
        time_next_event[1] = sim_time + expon(mean_interarrival);
        time_next_event[2] = Float.POSITIVE_INFINITY; // 1.0e+30f can be represented as Float.POSITIVE_INFINITY in Java
    }

    /**
     * Determine the next event.
     */
    private static void timing() {
        int i;
        float min_time_next_event = Float.POSITIVE_INFINITY;
        next_event_type = 0;

        // Determine the event type of the next event to occur
        for (i = 1; i <= num_events; ++i) {
            if (time_next_event[i] < min_time_next_event) {
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }
        }

        // Check to see whether the event list is empty
        if (next_event_type == 0) {
            // The event list is empty, so stop the simulation
            try {
                FileWriter writer = new FileWriter(outfile, true);
                writer.write(String.format("\nEvent list empty at time %f", sim_time));
                writer.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "An IO exception occurred", e);
            }
            System.exit(1);
        }

        // The event list is not empty, so advance the simulation clock
        sim_time = min_time_next_event;
    }

    /**
     * Handle an arrival event.
     */
    private static void arrive() {
        float delay;

        // Arrival event function

        // Schedule next arrival
        time_next_event[1] = sim_time + expon(mean_interarrival);

        // Check to see whether server is busy
        if (server_status == BUSY) {
            // Server is busy, so increment number of customers in queue
            ++num_in_q;

            // Check to see whether an overflow condition exists
            if (num_in_q > Q_LIMIT) {
                // The queue has overflowed, so stop the simulation
                try {
                    FileWriter writer = new FileWriter(outfile, true);
                    writer.write(String.format("\nOverflow of the array time_arrival at time %f", sim_time));
                    writer.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "An IO exception occurred", e);
                }
                System.exit(2);
            }

            // There is still room in the queue, so store the time of arrival of the arriving customer at the (new) end of time_arrival
            time_arrival[num_in_q] = sim_time;
        } else {
            // Server is idle, so arriving customer has a delay of zero
            delay = 0.0f;
            total_of_delays += delay;

            // Increment the number of customers delayed, and make server busy
            ++num_custs_delayed;
            server_status = BUSY;

            // Schedule a departure (service completion)
            time_next_event[2] = sim_time + expon(mean_service);
        }
    }

    /**
     * Handle a departure event.
     */
    private static void depart() {
        int i;
        float delay;

        // Departure event function

        // Check to see whether the queue is empty
        if (num_in_q == 0) {
            // The queue is empty so make the server idle and eliminate the departure (service completion) event from consideration
            server_status = IDLE;
            time_next_event[2] = Float.POSITIVE_INFINITY; // 1.0e+30f can be represented as Float.POSITIVE_INFINITY in Java
        } else {
            // The queue is nonempty, so decrement the number of customers in queue
            --num_in_q;

            // Compute the delay of the customer who is beginning service and update the total delay accumulator
            delay = sim_time - time_arrival[1];
            total_of_delays += delay;

            // Increment the number of customers delayed, and schedule departure
            ++num_custs_delayed;
            time_next_event[2] = sim_time + expon(mean_service);

            // Move each customer in queue (if any) up one place
            for (i = 1; i <= num_in_q; ++i) {
                time_arrival[i] = time_arrival[i + 1];
            }
        }
    }

    /**
     * Generate a report of the simulation.
     * @param writer FileWriter for the output file
     * @throws IOException If an IO exception occurs
     */
    private static void report(FileWriter writer) throws IOException {
        // Compute and write estimates of desired measures of performance
        writer.write(String.format("\n\nAverage delay in queue%11.3f minutes\n\n",
                total_of_delays / num_custs_delayed));
        writer.write(String.format("Average number in queue%10.3f\n\n",
                area_num_in_q / sim_time));
        writer.write(String.format("Server utilization%15.3f\n\n",
                area_server_status / sim_time));
        writer.write(String.format("Time simulation ended%12.3f minutes", sim_time));
    }

    /**
     * Update time-average statistical accumulators.
     */
    private static void update_time_avg_stats() {
        float time_since_last_event;

        // Compute time since last event, and update last-event-time marker
        time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;

        // Update area under number-in-queue function
        area_num_in_q += num_in_q * time_since_last_event;

        // Update area under server-busy indicator function
        area_server_status += server_status * time_since_last_event;
    }

    /**
     * Generate an exponential random variate with mean "mean".
     * @param mean Mean for the exponential distribution
     * @return An exponential random variate
     */
    private static float expon(float mean) {
        return (float)(-mean * Math.log(random.nextDouble()));
    }
}
