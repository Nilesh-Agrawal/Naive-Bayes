package in.ds222.Assignment1.local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import in.ds222.Assignment1.CONSTANTS;

public class NBTrain {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		System.out.println("Start Time : "+System.nanoTime());
		HashMap<String, HashMap<String, Long>> countOfYForX = new HashMap<String, HashMap<String, Long>>();
		HashMap<String, Long> countOfYForAnyX = new HashMap<String, Long>();
		HashMap<String, Long> countOfYForY_ = new HashMap<String, Long>();
		long countOfY = 0L;
		long X_VOCAB = 0L;
		long Y_VOCAB = 0L;

		BufferedReader bufferedReader = new BufferedReader(new FileReader(args[0]));
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			String[] instanceText = line.toString().split("\t");
			String[] classes = instanceText[0].split(",");
			String text = instanceText[1].substring(instanceText[1].indexOf("\"") + 1,
					instanceText[1].lastIndexOf("\""));
			text = text.replace("@", "");
			text = text.replaceAll("\\p{P}", "");
			String[] words = text.split(" ");
			long value;

			for (String Y : classes) {
				countOfY++;
				value = countOfYForY_.get(Y.trim().toLowerCase()) == null ? 0
						: countOfYForY_.get(Y.trim().toLowerCase());
				value++;
				countOfYForY_.put(Y.trim().toLowerCase(), value);
			}
			for (String X : words) {
				if (!CONSTANTS.stopWordSet.contains(X.trim().toLowerCase())) {
					for (String Y : classes) {
						value = countOfYForAnyX.get(Y.trim().toLowerCase()) == null ? 0
								: countOfYForAnyX.get(Y.trim().toLowerCase());
						value++;
						countOfYForAnyX.put(Y.trim().toLowerCase(), value);

						HashMap<String, Long> tempHashMap = countOfYForX.get(X.trim().toLowerCase());
						if (tempHashMap == null)
							tempHashMap = new HashMap<String, Long>();
						value = tempHashMap.get(Y.trim().toLowerCase()) == null ? 0
								: tempHashMap.get(Y.trim().toLowerCase());
						value++;
						tempHashMap.put(Y.trim().toLowerCase(), value);
						countOfYForX.put(X.trim().toLowerCase(), tempHashMap);

					}
				}
			}
		}
		bufferedReader.close();

		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(args[1]));
		for (String Y : countOfYForAnyX.keySet())
			bufferedWriter.write("TRAIN@X@ANY@Y@" + Y + "\t" + String.valueOf(countOfYForAnyX.get(Y)) + "\n");
		for (String Y : countOfYForY_.keySet()) {
			bufferedWriter.write("TRAIN@Y@" + Y + "\t" + String.valueOf(countOfYForY_.get(Y)) + "\n");
			Y_VOCAB++;
		}
		bufferedWriter.write("TRAIN@Y@ANY" + "\t" + String.valueOf(countOfY) + "\n");
		HashMap<String, Long> tempHashMap;
		for (String X : countOfYForX.keySet()) {
			tempHashMap = countOfYForX.get(X);
			for (String Y : tempHashMap.keySet()) {
				bufferedWriter.write("TRAIN@X@" + X + "@Y@" + Y + "\t" + String.valueOf(tempHashMap.get(Y)) + "\n");
			}
			X_VOCAB++;
		}
		bufferedWriter.write("TRAIN@X@VOCAB" + "\t" + String.valueOf(X_VOCAB) + "\n");
		bufferedWriter.write("TRAIN@Y@VOCAB" + "\t" + String.valueOf(Y_VOCAB));
		bufferedWriter.close();
		System.out.println("End Time : "+System.nanoTime());
	}

}
