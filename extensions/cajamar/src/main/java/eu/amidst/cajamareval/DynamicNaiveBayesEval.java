package eu.amidst.cajamareval;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.IOException;

/**
 * Created by dario on 6/6/16.
 */
public class DynamicNaiveBayesEval {

    public static void main(String[] args) throws IOException {

        String fileDay0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train0.arff";
        String fileDay1 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train1.arff";

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink0 =  DataFlinkLoader.loadDynamicDataFromFile(env, fileDay0, true);
//        dynamicDataInstanceDataFlink0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 =  DataFlinkLoader.loadDynamicDataFromFile(env, fileDay1, true);
//        dynamicDataInstanceDataFlink1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));


        DataStream<DynamicDataInstance> dataInstanceDataStream0 = DynamicDataStreamLoader.loadFromFile(fileDay0);
        dataInstanceDataStream0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        System.out.println();

        DataStream<DynamicDataInstance> dataInstanceDataStream1 = DynamicDataStreamLoader.loadFromFile(fileDay1);
        dataInstanceDataStream1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

//        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dynamicDataInstanceDataFlink);

    }
}