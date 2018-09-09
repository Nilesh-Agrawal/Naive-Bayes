package in.ds222.Assignment1.local;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import in.ds222.Assignment1.CONSTANTS;

public class NBTest {
	public static void main(String[] args) throws IOException {
		String[] classes = new String[50];
		HashMap<String, HashMap<String, Long>> countOfYForX = new HashMap<String, HashMap<String, Long>>();
		HashMap<String, Long> countOfYForAnyX = new HashMap<String, Long>();
		HashMap<String, Long> countOfYForY_ = new HashMap<String, Long>();
		long countOfY = 0L;
		long X_VOCAB = 0L;
		long Y_VOCAB = 0L;

		System.out.println("Start Time : "+System.nanoTime());
		
		String value;
		int i = 0;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(args[0]));
		while ((value = bufferedReader.readLine()) != null) {
			String[] instanceText = value.toString().split("\t");
			String[] keyParts = instanceText[0].split("@");

			if (keyParts[1].trim().equals("Y") && !keyParts[2].equals("ANY") && !keyParts[2].equals("VOCAB")) {
				countOfYForY_.put(keyParts[2], Long.parseLong(instanceText[1].trim()));
				classes[i++] = keyParts[2];
			} else if (keyParts[1].trim().equals("X") && keyParts[2].equals("ANY"))
				countOfYForAnyX.put(keyParts[4], Long.parseLong(instanceText[1].trim()));
			else if (keyParts[1].trim().equals("X") && !keyParts[2].equals("ANY") && !keyParts[2].equals("VOCAB")) {
				HashMap<String, Long> tempHashMap = countOfYForX.get(keyParts[4]);
				if (tempHashMap == null)
					tempHashMap = new HashMap<String, Long>();
				tempHashMap.put(keyParts[2], Long.parseLong(instanceText[1].trim()));
				countOfYForX.put(keyParts[4], tempHashMap);
			} else if (keyParts[1].trim().equals("Y") && keyParts[2].equals("ANY"))
				countOfY = Long.parseLong(instanceText[1].trim());
			else if (keyParts[1].trim().equals("X") && keyParts[2].equals("VOCAB"))
				X_VOCAB = Long.parseLong(instanceText[1].trim());
			else if (keyParts[1].trim().equals("Y") && keyParts[2].equals("VOCAB"))
				Y_VOCAB = Long.parseLong(instanceText[1].trim());
		}
		bufferedReader.close();

		bufferedReader = new BufferedReader(new FileReader(args[1]));
		long succes = 0, failure = 0;
		while ((value = bufferedReader.readLine()) != null) {
			HashMap<String, Double> probForEachClass = new HashMap<String, Double>();
			double prob = 0;
			String[] instanceText = value.toString().split("\t");
			// String[] trueClasses = instanceText[0].split(",");
			String text = instanceText[1].substring(instanceText[1].indexOf("\"") + 1,
					instanceText[1].lastIndexOf("\""));
			text = text.replace("@", "");
			text = text.replaceAll("\\p{P}", "").toLowerCase();
			String[] words = text.split(" ");
			for (String Y : classes) {
				// prob = (double) countOfYForY_.get(Y) / countOfY;
				// prob = ((double) countOfYForY_.get(Y) + 1.0) / ((double) countOfY + Y_VOCAB);
				prob = ((double) countOfYForY_.get(Y) + (1.0 / Y_VOCAB)) / ((double) countOfY + 1.0);
				HashMap<String, Long> tempHashMap = countOfYForX.get(Y);
				long tempValue = countOfYForAnyX.get(Y);
				for (String X : words)
					if (!CONSTANTS.stopWordSet.contains(X.trim().toLowerCase())) {
						// prob = prob * (((double) (tempHashMap.get(X) == null ? 0 :
						// tempHashMap.get(X)) + 1.0)
						// / (tempValue + X_VOCAB));
						prob = prob
								* (((double) (tempHashMap.get(X) == null ? 0 : tempHashMap.get(X)) + (1.0 / X_VOCAB))
										/ (tempValue + 1.0));
					}
				probForEachClass.put(Y, prob);
			}

			double maxProb = -1.0;
			String className = "class";
			for (String stringY : probForEachClass.keySet()) {
				if (maxProb < probForEachClass.get(stringY)) {
					maxProb = probForEachClass.get(stringY);
					className = stringY;
				}
			}

			if (instanceText[0].trim().toLowerCase().contains(className.trim().toLowerCase()))
				succes++;
			else
				failure++;

		}
		bufferedReader.close();

		System.out.println("No of Successful Classification: " + succes);
		System.out.println("No of UnSuccessful Classification: " + failure);
		System.out.println("Total Accuracy: " + ((float) succes) / (succes + failure));
		
		System.out.println("End Time : "+System.nanoTime());
	}

}
