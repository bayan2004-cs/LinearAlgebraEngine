package spl.lae;

import java.io.IOException;

import memory.SharedVector;
import memory.VectorOrientation;
import parser.*;

public class Main {
	public static void main(String[] args) throws IOException {
        // Done: main
		if(args.length < 1) {
			System.err.println("No path to input JSON file provided.");
			return;
		}

		// Parse input arguments
		int numThreads = Integer.parseInt(args[0]);
		String inputFilePath = args[1];
		String outputFilePath = args[2];
		
		// Init Linear Algebra Engine and Input Parser
		InputParser parser = new InputParser();
		LinearAlgebraEngine lae;
		try { 
			ComputationNode root = parser.parse(inputFilePath);			
			lae = new LinearAlgebraEngine(numThreads);
			ComputationNode result =lae.run(root);
			OutputWriter.write(result.getMatrix(), outputFilePath);
			
			boolean testing = true;
			// Print worker report if in testing mode
			if(testing) {
				System.out.println("Worker report");
				System.out.println(lae.getWorkerReport());
			}
		
		}
		catch (Exception e) {// Catch parsing exceptions
			OutputWriter.write(e.getMessage(), outputFilePath);
			return;
		}

	}
}