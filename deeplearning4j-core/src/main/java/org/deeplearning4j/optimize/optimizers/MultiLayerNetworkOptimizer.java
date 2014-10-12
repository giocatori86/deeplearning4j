package org.deeplearning4j.optimize.optimizers;

import java.io.Serializable;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.deeplearning4j.nn.BaseMultiLayerNetwork;
import org.deeplearning4j.nn.gradient.OutputLayerGradient;

import org.deeplearning4j.optimize.api.OptimizableByGradientValueMatrix;
import org.deeplearning4j.optimize.api.TrainingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Optimizes the logistic layer for finetuning
 * a multi layer network. This is meant to be used
 * after pretraining.
 * @author Adam Gibson
 *
 */
public class MultiLayerNetworkOptimizer implements Serializable,OptimizableByGradientValueMatrix {

    private static final long serialVersionUID = -3012638773299331828L;

    protected BaseMultiLayerNetwork network;

    private static Logger log = LoggerFactory.getLogger(MultiLayerNetworkOptimizer.class);
    private double lr;
    private int currentIteration;

    public MultiLayerNetworkOptimizer(BaseMultiLayerNetwork network,double lr) {
        this.network = network;
        this.lr = lr;
    }

    @Override
    public void setCurrentIteration(int value) {
        this.currentIteration = value;
    }

    public void optimize(INDArray labels,double lr,int epochs,TrainingEvaluator eval) {
        network.getOutputLayer().setLabels(labels);

        if(!network.isForceNumEpochs()) {
            //network.getOutputLayer().fit(labels,lr,epochs,eval);

            if(network.isShouldBackProp())
                network.backProp(lr, epochs,eval);

        }

        else {
            log.info("Training for " + epochs + " epochs");
            List<INDArray> activations = network.feedForward();
            INDArray train = activations.get(activations.size() - 1);

            for(int i = 0; i < epochs; i++) {
                if(i % network.getDefaultConfiguration().getResetAdaGradIterations() == 0)
                    network.getOutputLayer().getAdaGrad().historicalGradient = null;
                network.getOutputLayer().train(train, labels,lr);

            }


            if(network.isShouldBackProp())
                network.backProp(lr, epochs,eval);

        }



    }

    /**
     *
     * @param labels
     * @param lr
     * @param iteration
     */
    public void optimize(INDArray labels,double lr,int iteration) {
        network.getOutputLayer().setLabels(labels);
        if(!network.isForceNumEpochs()) {
            if(network.isShouldBackProp())
                network.backProp(lr, iteration);
            network.getOutputLayer().trainTillConvergence(lr,iteration);

        }

        else {
            log.info("Training for " + iteration + " iteration");

            if(network.isShouldBackProp())
                network.backProp(lr, iteration);

        }



    }








    @Override
    public int getNumParameters() {
        return network.getOutputLayer().getW().length() + network.getOutputLayer().getB().length();
    }




    public void getParameters(double[] buffer) {
        int idx = 0;
        for(int i = 0; i < network.getOutputLayer().getW().length(); i++) {
            buffer[idx++] = (double) network.getOutputLayer().getW().getScalar(i).element();

        }
        for(int i = 0; i < network.getOutputLayer().getB().length(); i++) {
            buffer[idx++] = (double) network.getOutputLayer().getB().getScalar(i).element();
        }
    }



    @Override
    public double getParameter(int index) {
        if(index >= network.getOutputLayer().getW().length()) {
            int i = index - network.getOutputLayer().getB().length();
            return (double) network.getOutputLayer().getB().getScalar(i).element();
        }
        else
            return (double) network.getOutputLayer().getW().getScalar(index).element();
    }




    public void setParameters(double[] params) {
        int idx = 0;
        for(int i = 0; i < network.getOutputLayer().getW().length(); i++) {
            network.getOutputLayer().getW().putScalar(i, params[idx++]);
        }


        for(int i = 0; i < network.getOutputLayer().getB().length(); i++) {
            network.getOutputLayer().getB().putScalar(i, params[idx++]);
        }
    }



    @Override
    public void setParameter(int index, double value) {
        if(index >= network.getOutputLayer().getW().length()) {
            int i = index - network.getOutputLayer().getB().length();
            network.getOutputLayer().getB().putScalar(i, value);
        }
        else
            network.getOutputLayer().getW().putScalar(index, value);
    }




    public void getValueGradient(double[] buffer) {
        OutputLayerGradient gradient = network.getOutputLayer().getGradient(lr);

        INDArray weightGradient = gradient.getwGradient();
        INDArray biasGradient = gradient.getbGradient();

        int idx = 0;

        for(int i = 0; i < weightGradient.length(); i++)
            buffer[idx++] = (double) weightGradient.getScalar(i).element();
        for(int i = 0; i < biasGradient.length(); i++)
            buffer[idx++] = (double) biasGradient.getScalar(i).element();

    }



    @Override
    public double getValue() {
        return (double) network.score();
    }



    @Override
    public INDArray getParameters() {
        double[] d = new double[getNumParameters()];
        this.getParameters(d);
        return Nd4j.create(d);
    }



    @Override
    public void setParameters(INDArray params) {
        this.setParameters(params.data().asDouble());
    }




    @Override
    public INDArray getValueGradient(int iteration) {
        double[] buffer = new double[getNumParameters()];
        getValueGradient(buffer);
        return Nd4j.create(buffer);
    }


}
