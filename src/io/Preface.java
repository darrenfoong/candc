package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Preface {
	public static void readPreface(BufferedReader in) throws IOException {
		String line = in.readLine();

		if (line == null || line.charAt(0) != '#') {
			throw new IllegalArgumentException("File does not start with the mandatory preface.");
		}

		while ((line = in.readLine()) != null) {
			if (line.isEmpty()) {
				break;
			}
			if (line.charAt(0) != '#') {
				throw new IllegalArgumentException("Uncommented line within preface.");
			}
		}
	}

	public static void printPreface(PrintWriter out) {
		out.println("# mandatory preface");
		out.println("# mandatory preface");
		out.println();
	}
}
