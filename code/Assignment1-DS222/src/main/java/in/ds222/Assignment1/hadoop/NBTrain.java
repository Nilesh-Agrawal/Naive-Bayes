package in.ds222.Assignment1.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import in.ds222.Assignment1.CONSTANTS;

public class NBTrain {

	public static class TrainMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, LongWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.map(key, value, context);
			String[] instanceText = value.toString().split("\t");
			String[] classes = instanceText[0].split(",");
			String text = instanceText[1].substring(instanceText[1].indexOf("\"") + 1,
					instanceText[1].lastIndexOf("\""));
			text = text.replace("@", "");
			text = text.replaceAll("\\p{P}", "");
			String[] words = text.split(" ");
			for (String Y : classes) {
				context.write(new Text("Y@ANY"), new LongWritable(1));
				context.write(new Text("Y@" + Y.trim().toLowerCase()), new LongWritable(1));
			}
			for (String X : words) {
				if (!CONSTANTS.stopWordSet.contains(X.trim().toLowerCase())) {
					for (String Y : classes) {
						context.write(new Text("X@ANY@Y@" + Y.trim().toLowerCase()), new LongWritable(1));
						context.write(new Text("X@" + X.trim().toLowerCase() + "@Y@" + Y.trim().toLowerCase()),
								new LongWritable(1));
					}
					context.write(new Text("VOCAB@" + X.trim().toLowerCase()), new LongWritable(1));
				}
			}
		}
	}

	public static class TrainReducer extends Reducer<Text, LongWritable, Text, LongWritable> {

		public long Y_VOCAB = 0L;
		public long X_VOCAB = 0L;

		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,
				Reducer<Text, LongWritable, Text, LongWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// super.reduce(key, values, context);
			long sum = 0L;
			String[] keyParts = key.toString().split("@");
			if (keyParts[0].equals("Y") && keyParts[1].equals("ANY")) {
				for (LongWritable value : values)
					sum++;
				context.write(new Text("TRAIN@Y@ANY"), new LongWritable(sum));
			} else if (keyParts[0].equals("Y")) {
				for (LongWritable value : values)
					sum++;
				context.write(new Text("TRAIN@Y@" + keyParts[1]), new LongWritable(sum));
				Y_VOCAB++;
			} else if (keyParts[0].equals("X") && keyParts[1].equals("ANY")) {
				for (LongWritable value : values)
					sum++;
				context.write(new Text("TRAIN@X@ANY@Y@" + keyParts[3]), new LongWritable(sum));
			} else if (keyParts[0].equals("X")) {
				for (LongWritable value : values)
					sum++;
				context.write(new Text("TRAIN@X@" + keyParts[1] + "@Y@" + keyParts[3]), new LongWritable(sum));
			} else if (keyParts[0].equals("VOCAB")) {
				X_VOCAB++;
			}
		}

		@Override
		protected void cleanup(Reducer<Text, LongWritable, Text, LongWritable>.Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			super.cleanup(context);
			context.write(new Text("TRAIN@X@VOCAB"), new LongWritable(X_VOCAB));
			context.write(new Text("TRAIN@Y@VOCAB"), new LongWritable(Y_VOCAB));
		}

	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "NB Train");

		job.setJarByClass(NBTrain.class);
		job.setNumReduceTasks(Integer.parseInt(args[2]));

		job.setMapperClass(TrainMapper.class);
		job.setReducerClass(TrainReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		// drwxr-xr-x - root ds222 0 2017-09-14 19:42
		// /user/ds222/assignment-1/DBPedia.full
		// drwxr-xr-x - root ds222 0 2017-09-14 19:41
		// /user/ds222/assignment-1/DBPedia.small
		// drwxr-xr-x - root ds222 0 2017-09-14 19:41
		// /user/ds222/assignment-1/DBPedia.verysmall

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
