# Naive-Bayes

# Create Jar File 
1. First run command "mvn clean install" to generate jar file.

## Running Naive Bayes Locally.
1. For training run command:<br/> java -cp Assignment1-DS222-0.0.1-SNAPSHOT.jar in.ds222.Assignment1.local.NBTrain /scratch/ds222-2017/assignment-1/DBPedia.full/full_train.txt ~/ds222/a1/NBTrainoutput.txt

2. For Testing run command:<br/> java -cp Assignment1-DS222-0.0.1-SNAPSHOT.jar in.ds222.Assignment1.local.NBTest ~/ds222/a1/NBTrainoutput.txt /scratch/ds222-2017/assignment-1/DBPedia.full/full_test.txt


## Running Mapreduce Naive Bayes
1. First create two HDFS directory for storing train and test output.<br/>
  a. hdfs dfs -mkdir -p /user/anilesh/ds222/a1/train<br/>
  b. hdfs dfs -mkdir -p /user/anilesh/ds222/a1/test/output

2. For training run command:<br/> hadoop jar Assignment1-DS222-0.0.1-SNAPSHOT.jar in.ds222.Assignment1.hadoop.NBTrain /user/ds222/assignment-1/DBPedia.full/full_train.txt /user/anilesh/ds222/a1/train/output/ 1<br/>
where 1 is no. of reducers

3. For testing run command:<br/> hadoop jar Assignment1-DS222-0.0.1-SNAPSHOT.jar in.ds222.Assignment1.hadoop.NBTest2 /user/anilesh/ds222/a1/train/output/ /user/ds222/assignment-1/DBPedia.full/full_test.txt /user/anilesh/ds222/a1/test/output/ 10<br/>
where 10 is no. of reducers

