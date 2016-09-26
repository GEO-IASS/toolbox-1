/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.conceptdrift.NaiveBayesVirtualConceptDriftDetector;
import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.BayesianParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**

 */
public class ConceptDriftDetector extends Model {

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    public enum DriftDetector {GLOBAL};

    /** Represents the variance added when making a transition*/
    double transitionVariance;

    /** Represents the index of the class variable of the classifier*/
    int classIndex;

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    DriftDetector conceptDriftDetector;

    /** Represents the seed of the class*/
    int seed;


    /** Represents the list of hidden vars modelling concept drift*/
    List<Variable> hiddenVars;

    /** Represents the fading factor.*/
    double fading;

    /** Represents the number of global hidden variables*/
    int numberOfGlobalVars;

    /** Represents whether there is or not a global hidden variable modelling concept drift*/
    boolean globalHidden;


    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * The following parameters are set to their default values: numStatesHiddenVar = 2
     * and diagonal = true.
     * @param attributes object of the class Attributes
     */
    public ConceptDriftDetector(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        transitionVariance=0.1;
        classIndex = atts.getNumberOfAttributes()-1;
        conceptDriftDetector = DriftDetector.GLOBAL;
        seed = 0;
        fading = 1.0;
        numberOfGlobalVars = 1;
        globalHidden = true;
        super.windowSize = 1000;
    }




    /**
     * Builds the DAG over the set of variables given with the structure of the model
     */
    @Override
    protected void buildDAG() {


        String className = atts.getFullListOfAttributes().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (int i = 0; i < this.numberOfGlobalVars ; i++) {
            hiddenVars.add(vars.newGaussianVariable("GlobalHidden_"+i));
        }

        Variable classVariable = vars.getVariableByName(className);

        dag = new DAG(vars);

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = vars.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            if (this.globalHidden) {
                for (int i = 0; i < this.numberOfGlobalVars ; i++) {
                    dag.getParentSet(variable).addParent(hiddenVars.get(i));
                }
            }
        }
    }

    @Override
   protected  void initLearning() {

        if (this.getDAG()==null)
            buildDAG();

        if(learningAlgorithm==null) {
            SVB svb = new SVB();
            svb.setSeed(this.seed);
            svb.setPlateuStructure(new PlateuIIDReplication(hiddenVars));
            GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
            gaussianHiddenTransitionMethod.setFading(fading);
            svb.setTransitionMethod(gaussianHiddenTransitionMethod);
            svb.setDAG(dag);

            svb.setOutput(false);
            svb.getPlateuStructure().getVMP().setMaxIter(1000);
            svb.getPlateuStructure().getVMP().setThreshold(0.001);

            learningAlgorithm = svb;
        }
        learningAlgorithm.setWindowsSize(windowSize);
        if (this.getDAG()!=null)
            learningAlgorithm.setDAG(this.getDAG());
        else
            throw new IllegalArgumentException("Non provided dag");

        learningAlgorithm.setOutput(true);
        learningAlgorithm.initLearning();
        initialized=true;
    }





    /////// Getters and setters

    public double getTransitionVariance() {
        return transitionVariance;
    }

    public void setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
        resetModel();
    }

    public int getClassIndex() {
        return classIndex;
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
        resetModel();
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
        resetModel();
    }

    public double getFading() {
        return fading;
    }

    public void setFading(double fading) {
        this.fading = fading;
        resetModel();
    }

    public int getNumberOfGlobalVars() {
        return numberOfGlobalVars;
    }

    public void setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
        resetModel();
    }

    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {


        int windowSize = 1000;

       // DataStream<DataInstance> data = DataSetGenerator.generate(1234,100, 1, 3);
        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("./datasets/DriftSets/sea.arff");

        System.out.println(data.getAttributes().toString());


        //Build the model
        Model model = new ConceptDriftDetector(data.getAttributes());
        model.setWindowSize(windowSize);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)){
            model.updateModel(batch);
            System.out.println(model.getPosteriorDistribution("GlobalHidden_0").
                    toString());


        }

        System.out.println(model.getDAG());

    }
}

