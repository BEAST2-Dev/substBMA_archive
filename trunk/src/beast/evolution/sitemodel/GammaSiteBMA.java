package beast.evolution.sitemodel;

import beast.core.parameter.RealParameter;
import beast.core.parameter.IntegerParameter;
import beast.core.Input;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Node;

/**
 * @author Chieh-Hsi Wu
 *
 * BSSVS on Gamma site model
 *
 */
public class GammaSiteBMA extends SiteModel {

    public Input<RealParameter> logShape =
            new Input<RealParameter>("logShape", "shape parameter of gamma distribution. Ignored if gammaCategoryCount 1 or less",Input.Validate.REQUIRED);
    public Input<RealParameter> logitInvar =
            new Input<RealParameter>("logitInvar", "proportion of sites that is invariant: should be between 0 (default) and 1",Input.Validate.REQUIRED);
    public Input<IntegerParameter> modelChoose = new Input<IntegerParameter>("modelChoose",
            "Integer parameter that is a bit vector presenting the model", Input.Validate.REQUIRED);


    public static final int SHAPE_INDEX = 0;
    public static final int INVAR_INDEX = 1;
    public static final int PRESENT = 1;
    public static final int ABSENT = 0;

    public GammaSiteBMA() {
        gammaCategoryCount.setRule(Input.Validate.REQUIRED);
    }

    public void initAndValidate() throws Exception {


        //seting the bound for mu
        if (muParameter.get() != null) {
            muParameter.get().setBounds(0.0, Double.POSITIVE_INFINITY);
        }

        //setting bounds for shape
        logShape.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        //setting bounds for invar
        logitInvar.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        //category count
        categoryCount = gammaCategoryCount.get()+1;

        
        categoryRates = new double[categoryCount];
        categoryProportions = new double[categoryCount];

        ratesKnown = false;

        addCondition(muParameter);
        addCondition(invarParameter);
        addCondition(shapeParameter);
    }

    @Override
    protected boolean requiresRecalculation() {
       // we only get here if something is dirty in its inputs
        boolean recalculate = false;
        /*if(m_pSubstModel.isDirty()){
            recalculate = true;

        }else */if(modelChoose.get().somethingIsDirty()){


            recalculate = true;
        }else if(logShape.get().somethingIsDirty()){
            if(modelChoose.get().getValue(SHAPE_INDEX) == PRESENT){
                recalculate = true;
            }
        }else if(logitInvar.get().somethingIsDirty()){
            if(modelChoose.get().getValue(INVAR_INDEX) == PRESENT){
                recalculate = true;
            }
        }

        if(recalculate){
            ratesKnown = false;
        }
        //return recalculate;
        return m_pSubstModel.isDirty() || recalculate;


    }

    /**
     * discretization of gamma distribution with equal proportions in each
     * category
     */
    protected void calculateCategoryRates(Node node) {
        double propVariable = 1.0;
        int cat = 0;

        categoryRates[0] = 0.0;
        //back transform from logit space
        categoryProportions[0] = modelChoose.get().getValue(INVAR_INDEX)*(1/(1+Math.exp(-logitInvar.get().getValue(0))));
        propVariable = 1.0 - categoryProportions[0];
        cat = 1;
        
        //If including the gamma shape parameter.
        if (modelChoose.get().getValue(SHAPE_INDEX) == PRESENT) {

            //back transform from log-space
            final double a = Math.exp(logShape.get().getValue(0));
            double mean = 0.0;
            final int gammaCatCount = categoryCount - cat;

            for (int i = 0; i < gammaCatCount; i++) {
                try {
                    categoryRates[i + cat] = GammaDistributionQuantile((2.0 * i + 1.0) / (2.0 * gammaCatCount), a, 1.0 / a);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Something went wrong with the gamma distribution calculation");
                    System.exit(-1);
                }
                //sum of the gamma categorical rates
                mean += categoryRates[i + cat];

                categoryProportions[i + cat] = propVariable / gammaCatCount;
            }

            //mean rate over all categories.
            mean = (propVariable * mean) / gammaCatCount;

            for (int i = 0; i < gammaCatCount; i++) {
                //divide rates by the mean so that the average across all sites equals to 1.0
                categoryRates[i + cat] /= mean;

            }

        } else {
            final int gammaCatCount = categoryCount - cat;
            for (int i = 0; i < gammaCatCount; i++) {
                categoryRates[i + cat] = 1.0 / propVariable;
                categoryProportions[i + cat] = propVariable/gammaCatCount;
            }
            
        }


        ratesKnown = true;
    }

}