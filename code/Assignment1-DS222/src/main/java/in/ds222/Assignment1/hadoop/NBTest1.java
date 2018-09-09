package in.ds222.Assignment1.hadoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import in.ds222.Assignment1.CONSTANTS;

public class NBTest {

	public static class TestMapper1 extends Mapper<LongWritable, Text, Text, Text> {

		public String[] classes = new String[50];
		public HashMap<String, HashMap<String, Long>> countOfYForX = new HashMap<String, HashMap<String, Long>>();
		public HashMap<String, Long> countOfYForAnyX = new HashMap<String, Long>();
		public HashMap<String, Long> countOfYForY_ = new HashMap<String, Long>();
		public long countOfY = 0L;
		public long X_VOCAB = 0L;
		public long Y_VOCAB = 0L;

		@Override
		protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			super.setup(context);

			Configuration conf = new Configuration();
			conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
			conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
			String filePath = context.getConfiguration().get("filePath");
			Path path = new Path(filePath);
			FileSystem fs = null;
			try {
				fs = FileSystem.get(new URI("hdfs://turing.cds.iisc.ac.in:8020"), conf);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String text;
			int i = 0;
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fs.open(path)));
			while ((text = bufferedReader.readLine()) != null) {
				String[] instanceText = text.toString().split("\t");
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
			// fs.close();
		}

		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.map(key, value, context);
			HashMap<String, Double> probForEachClass = new HashMap<String, Double>();
			double prob = 0;
			String[] instanceText = value.toString().split("\t");
			String[] trueClasses = instanceText[0].split(",");
			String text = instanceText[1].substring(instanceText[1].indexOf("\"") + 1,
					instanceText[1].lastIndexOf("\""));
			text = text.replace("@", "");
			text = text.replaceAll("\\p{P}", "").toLowerCase();
			String[] words = text.split(" ");
			for (String Y : classes) {
//				prob = (double) countOfYForY_.get(Y) / countOfY;
				prob = ((double) countOfYForY_.get(Y) + (1.0/Y_VOCAB)) / ((double)countOfY + 1.0);
				HashMap<String, Long> tempHashMap = countOfYForX.get(Y);
				long tempValue = countOfYForAnyX.get(Y);
				for (String X : words)
					if (!CONSTANTS.stopWordSet.contains(X.trim().toLowerCase())) {
						prob = prob * (((double) (tempHashMap.get(X) == null ? 0 : tempHashMap.get(X)) + (1.0/X_VOCAB))
								/ (tempValue + 1.0));
					}
				probForEachClass.put(Y, prob);
			}

			double maxProb = -1;
			String className = "class";
			for (String stringY : probForEachClass.keySet()) {
				if (maxProb < probForEachClass.get(stringY)) {
					maxProb = probForEachClass.get(stringY);
					className = stringY;
				}
			}

			// System.out.println("Prob" + String.valueOf(maxProb) + " Prdcited class: " +
			// className + " Original class"
			// + instanceText[0]);

			if (instanceText[0].trim().toLowerCase().contains(className.trim().toLowerCase())) {
//				System.out.println("Success");
				context.write(new Text("success"), new Text("s"));
			} else {
//				System.out.println("Failure");
				context.write(new Text("failure"), new Text("f"));
			}
		}
	}

	public static class TestReducer1 extends Reducer<Text, Text, Text, FloatWritable> {

		long sucess = 0L;
		long failure = 0L;

		@Override
		protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, FloatWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			if (key.toString().trim().toLowerCase().equals("success")) {
				for (Text text : values)
					sucess++;
			} else {
				for (Text text : values)
					failure++;
			}
		}

		@Override
		protected void cleanup(Reducer<Text, Text, Text, FloatWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			super.cleanup(context);
			context.write(new Text("No of Successful Classification"), new FloatWritable(sucess));
			context.write(new Text("No of UnSuccessful Classification"), new FloatWritable(failure));
			context.write(new Text("Total Accuracy"), new FloatWritable((float) sucess / ((float) sucess + failure)));
		}

	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		Configuration conf1 = new Configuration();
		conf1.set("filePath", args[0] + "part-r-00000");
		Job job1 = Job.getInstance(conf1, "NB Test-Phase1");
		job1.setNumReduceTasks(Integer.parseInt(args[3]));
		job1.setJarByClass(NBTest.class);
		job1.setReducerClass(TestReducer1.class);
		job1.setMapperClass(TestMapper1.class);
		job1.setMapOutputValueClass(Text.class);
		job1.setMapOutputKeyClass(Text.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(FloatWritable.class);
		// FileInputFormat.addInputPaths(job1, args[0] + "," + args[1]);
		FileInputFormat.addInputPath(job1, new Path(args[1]));
		// FileInputFormat.addInputPath(job1, new Path(args[1]));
		FileOutputFormat.setOutputPath(job1, new Path(args[2] + "1/"));
		System.exit(job1.waitForCompletion(true) ? 0 : 1);
	}
}
