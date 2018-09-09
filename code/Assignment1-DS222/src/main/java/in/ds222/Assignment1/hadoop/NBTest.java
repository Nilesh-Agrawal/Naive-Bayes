package in.ds222.Assignment1.hadoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import in.ds222.Assignment1.CONSTANTS;

public class NBTest2 {

	public static class TestMapper1 extends Mapper<LongWritable, Text, Text, Text> {
		public String[] classes = new String[50];

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
				// String[] instanceText = value.toString().split("\t");
				String[] keyParts = text.toString().split("\t")[0].split("@");
				if (keyParts[1].trim().equals("Y") && !keyParts[2].equals("ANY") && !keyParts[2].equals("VOCAB"))
					classes[i++] = keyParts[2];

			}
			bufferedReader.close();
			// fs.close();
		}

		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.map(key, value, context);

			if (!value.toString().startsWith("TRAIN")) {
				String[] instanceText = value.toString().split("\t");
				String text = instanceText[1].substring(instanceText[1].indexOf("\"") + 1,
						instanceText[1].lastIndexOf("\""));
				text = text.replace("@", "");
				text = text.replaceAll("\\p{P}", "").toLowerCase();
				String[] words = text.split(" ");
				context.write(new Text("CLASS@" + key.toString()), new Text(instanceText[0].trim().toLowerCase()));
				for (String X : words)
					if (!CONSTANTS.stopWordSet.contains(X.trim().toLowerCase())) {
						for (String Y : classes)
							context.write(new Text("X@" + X.trim().toLowerCase() + "@Y@" + Y.trim().toLowerCase()),
									new Text("ID@" + key.toString()));
					}
			} else {
				// TRAIN@X@baring@Y@black-and-white_films 2
				String[] instanceText = value.toString().split("\t");
				String[] keyParts = instanceText[0].split("@");
				if (keyParts.length == 5 && keyParts[1].trim().equals("X") && !keyParts[2].trim().equals("ANY"))
					context.write(
							new Text(
									"X@" + keyParts[2].trim().toLowerCase() + "@Y@" + keyParts[4].trim().toLowerCase()),
							new Text("TRAIN@" + instanceText[1].trim()));
			}
		}
	}

	public static class TestReducer1 extends Reducer<Text, Text, Text, DoubleWritable> {

		HashMap<String, Long> countOfYForAnyX = new HashMap<String, Long>();
		long X_VOCAB = 0L;

		@Override
		protected void setup(Reducer<Text, Text, Text, DoubleWritable>.Context context)
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
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fs.open(path)));
			while ((text = bufferedReader.readLine()) != null) {
				String[] instanceText = text.toString().split("\t");
				String[] keyParts = instanceText[0].split("@");
				if (keyParts[1].trim().equals("X") && keyParts[2].trim().equals("ANY"))
					countOfYForAnyX.put(keyParts[4].trim(), Long.parseLong(instanceText[1].trim()));
				else if (keyParts[1].trim().equals("X") && keyParts[2].trim().equals("VOCAB"))
					X_VOCAB = Long.parseLong(instanceText[1].trim());
			}
			// System.out.println("++++++++++++++countOfYForAnyX+++++++" +
			// countOfYForAnyX.size());
			bufferedReader.close();
		}

		@Override
		protected void reduce(Text key, Iterable<Text> values,
				Reducer<Text, Text, Text, DoubleWritable>.Context context) throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			if (key.toString().startsWith("CLASS")) {
				context.write(new Text(key.toString() + "@" + values.iterator().next()), new DoubleWritable(0));
				return;
			}

			ArrayList<Long> documnetIds = new ArrayList<Long>();
			long countForXY = 0;
			for (Text text : values) {
				String[] stringTextParts = text.toString().split("@");
				if (stringTextParts[0].equals("ID"))
					documnetIds.add(Long.parseLong(stringTextParts[1].trim()));
				else // if (stringTextParts[0].equals("TRAIN"))
					countForXY = Long.parseLong(stringTextParts[1].trim());
			}

			String Y = key.toString().split("@")[3];
			double prob = 0.0;
			prob = ((double) countForXY + (1.0)) / ((double)countOfYForAnyX.get(Y) + X_VOCAB);
			for (Long documentId : documnetIds) {
				context.write(new Text(documentId + "@Y@" + Y), new DoubleWritable(prob));
			}
		}
	}

	public static class TestMapper2 extends Mapper<LongWritable, Text, Text, DoubleWritable> {

		@Override
		protected void map(LongWritable key, Text value,
				Mapper<LongWritable, Text, Text, DoubleWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.map(key, value, context);

			String[] instanceText = value.toString().split("\t");
			// documentId + "@Y@" + Y
			context.write(new Text(instanceText[0].trim()),
					new DoubleWritable(Double.parseDouble(instanceText[1].trim())));
		}
	}

	public static class TestReducer2 extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

		HashMap<String, Long> countOfYForY_ = new HashMap<String, Long>();
		long Y_VOCAB = 0L;
		long countOfYForAny = 0L;

		@Override
		protected void setup(Reducer<Text, DoubleWritable, Text, DoubleWritable>.Context context)
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
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fs.open(path)));
			while ((text = bufferedReader.readLine()) != null) {
				String[] instanceText = text.toString().split("\t");
				String[] keyParts = instanceText[0].split("@");
				if (keyParts[1].trim().equals("Y") && !keyParts[2].trim().equals("ANY"))
					countOfYForY_.put(keyParts[2].trim(), Long.parseLong(instanceText[1].trim()));
				else if (keyParts[1].trim().equals("Y") && keyParts[2].trim().equals("VOCAB"))
					Y_VOCAB = Long.parseLong(instanceText[1].trim());
				else if (keyParts[1].trim().equals("Y") && keyParts[2].trim().equals("ANY"))
					countOfYForAny = Long.parseLong(instanceText[1].trim());

			}
			bufferedReader.close();
		}

		@Override
		protected void reduce(Text key, Iterable<DoubleWritable> values,
				Reducer<Text, DoubleWritable, Text, DoubleWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			if (key.toString().startsWith("CLASS")) {
				context.write(key, new DoubleWritable(0));
				return;
			}

			String[] keyParts = key.toString().trim().split("@");
			double prob = 1.0;
			prob = ((((double) countOfYForY_.get(keyParts[2].trim()) + (1.0))/ ((double) countOfYForAny + Y_VOCAB)));
			for (DoubleWritable value : values)
				prob *= value.get();
			context.write(new Text("TEST@" + keyParts[0].trim() + "@Y@" + keyParts[2].trim()),
					new DoubleWritable(prob));
		}
	}

	public static class TestMapper3 extends Mapper<LongWritable, Text, LongWritable, Text> {

		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, LongWritable, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.map(key, value, context);

			String[] instanceText = value.toString().split("\t");
			// documentId + "@Y@" + Y
			if (instanceText[0].startsWith("TEST"))
				context.write(new LongWritable(Long.parseLong(instanceText[0].trim().split("@")[1])),
						new Text(instanceText[0].trim().split("@")[3] + "@" + instanceText[1].trim()));
			else {
				// CLASS@documentID@classnames
				context.write(new LongWritable(Long.parseLong(instanceText[0].trim().toLowerCase().split("@")[1])),
						new Text(instanceText[0].trim()));
			}
		}
	}

	public static class TestReducer3 extends Reducer<LongWritable, Text, LongWritable, Text> {

		long success = 0L;
		long failure = 0L;

		@Override
		protected void reduce(LongWritable key, Iterable<Text> values,
				Reducer<LongWritable, Text, LongWritable, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.reduce(key, values, context);
			String maxClassName = "class";
			double maxClassValue = -1;
			String classNames = "";
			String stringText = "";

			String[] stringSplitText;
			double currClassValue = 0.0;
			for (Text text : values) {
				stringText = text.toString();
				if (stringText.startsWith("CLASS"))
					classNames = stringText.split("@")[2].trim();
				else {
					stringSplitText = stringText.split("@");
					currClassValue = Double.parseDouble(stringSplitText[1].trim());
					if (maxClassValue < currClassValue) {
						maxClassName = stringSplitText[0].trim();
						maxClassValue = currClassValue;
					}
				}
			}

			if (classNames.toLowerCase().contains(maxClassName.toLowerCase()))
				success++;
			else
				failure++;

			context.write(key, new Text(maxClassName));

		}

		@Override
		protected void cleanup(Reducer<LongWritable, Text, LongWritable, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			super.cleanup(context);
			context.write(new LongWritable(0L),
					new Text("No. of Successful Classification: " + String.valueOf(success)));
			context.write(new LongWritable(0L),
					new Text("No. of Unsuccessful Classification: " + String.valueOf(failure)));
			context.write(new LongWritable(0L),
					new Text("Accuracy: " + String.valueOf(((float) success) / (success + failure))));
		}

	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		Configuration conf1 = new Configuration();
		conf1.set("filePath", args[0] + "part-r-00000");
		Job job1 = Job.getInstance(conf1, "NB Test-Phase1");
		job1.setNumReduceTasks(Integer.parseInt(args[3]));
		job1.setJarByClass(NBTest2.class);
		job1.setReducerClass(TestReducer1.class);
		job1.setMapperClass(TestMapper1.class);
		job1.setMapOutputValueClass(Text.class);
		job1.setMapOutputKeyClass(Text.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(DoubleWritable.class);
		FileInputFormat.addInputPaths(job1, args[0] + "," + args[1]);
		// FileInputFormat.addInputPath(job1, new Path(args[0]));
		// FileInputFormat.addInputPath(job1, new Path(args[1]));
		FileOutputFormat.setOutputPath(job1, new Path(args[2] + "1/"));
		job1.waitForCompletion(true);

		Configuration conf2 = new Configuration();
		conf2.set("filePath", args[0] + "part-r-00000");
		Job job2 = Job.getInstance(conf2, "NB Test-Phase2");
		job2.setNumReduceTasks(Integer.parseInt(args[3]));
		job2.setJarByClass(NBTest2.class);
		job2.setMapperClass(TestMapper2.class);
		job2.setReducerClass(TestReducer2.class);
		// job1.setMapOutputKeyClass(theClass);
		// job1.setMapOutputValueClass(theClass);
		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(DoubleWritable.class);
		FileInputFormat.addInputPath(job2, new Path(args[2] + "1/"));
		FileOutputFormat.setOutputPath(job2, new Path(args[2] + "2/"));
		job2.waitForCompletion(true);

		Configuration conf3 = new Configuration();
		Job job3 = Job.getInstance(conf3, "NB Test-Phase3");
		job3.setNumReduceTasks(1);
		job3.setJarByClass(NBTest2.class);
		job3.setMapperClass(TestMapper3.class);
		job3.setReducerClass(TestReducer3.class);
		// job1.setMapOutputKeyClass(theClass);
		// job1.setMapOutputValueClass(theClass);
		job3.setOutputKeyClass(LongWritable.class);
		job3.setOutputValueClass(Text.class);
		// FileInputFormat.addInputPaths(job3, args[1] + "," + args[2] + "2/");
		// FileInputFormat.addInputPath(job3, new Path(args[1]));
		FileInputFormat.addInputPath(job3, new Path(args[2] + "2/"));
		FileOutputFormat.setOutputPath(job3, new Path(args[2] + "3/"));
		System.exit(job3.waitForCompletion(true) ? 0 : 1);

	}

}
