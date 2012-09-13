package beast.evolution.likelihood;

import beast.core.MCMCNodeFactory;
import beast.evolution.sitemodel.DPSiteModel;
import beast.evolution.sitemodel.DPNtdRateSepSiteModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.substitutionmodel.SwitchingNtdBMA;
import beast.core.parameter.ChangeType;
import beast.core.Input;

import java.util.ArrayList;

/**
 * @author Chieh-Hsi Wu
 */
public class DPSepTreeLikelihood extends DPTreeLikelihood{

    private DPNtdRateSepSiteModel dpSiteModel;
    //private WVTreeLikelihood[][] treeLiksMatrix;
    private NewWVTreeLikelihood[][] treeLiksMatrix;
    private NewWVTreeLikelihood[][] storedTreeLiksMatrix;
    private ChangeType changeType = ChangeType.ALL;

    public Input<DPNtdRateSepSiteModel> dpSepSiteModelInput = new Input<DPNtdRateSepSiteModel>(
            "dpSepSiteModelList",
            "array which points a set of unique parameter values",
            Input.Validate.REQUIRED
    );

    public DPSepTreeLikelihood(){
        /*
         * We need DPNtdRateSepSiteModel specifically,
         * therefore an Input<DPSiteModel> is not useful.
         */
        dpSiteModelInput.setRule(Input.Validate.OPTIONAL);
        dpValInput.setRule(Input.Validate.OPTIONAL);
    }

    public void initAndValidate() throws Exception{


        alignment = alignmentInput.get();
        int patternCount = alignment.getPatternCount();
        dpSiteModel = dpSepSiteModelInput.get();
        int siteModelCount = dpSiteModel.getSiteModelCount();

        /*
         * Set up a 3 dimensional array to store the weights of each ntdBMA/rate combination (dimensions: ntdBMA, rates, weights).
         */
        int[][][]clusterWeights = new int[dpSiteModel.getSubstClusterLimit()][dpSiteModel.getRatesClusterLimit()][patternCount];
        int[] clusterIds;
        int siteCount = alignment.getSiteCount();
        for(int i = 0; i < siteCount; i++){
            clusterIds = dpSiteModel.getCurrClusters(i);
            clusterWeights[clusterIds[dpSiteModel.NTDBMA]][clusterIds[dpSiteModel.RATES]][alignment.getPatternIndex(i)]++;
        }

        /*
         *  Traverse through the list site models and create tree likelihoods.
         *  The tree likelihoods are then added to a list and the matrix.
         *  The order of likelihoods in the list corresponds to that of site models in the DPSiteModel list.
         */
        //treeLiksMatrix = new WVTreeLikelihood[dpSiteModel.getSubstClusterLimit()][dpSiteModel.getRatesClusterLimit()];
        treeLiksMatrix = new NewWVTreeLikelihood[dpSiteModel.getSubstClusterLimit()][dpSiteModel.getRatesClusterLimit()];
        storedTreeLiksMatrix = new NewWVTreeLikelihood[dpSiteModel.getSubstClusterLimit()][dpSiteModel.getRatesClusterLimit()];
        for(int i = 0; i < siteModelCount;i++){

            //Get the ids and hence the positions of siteModel/treeLikelihoods in a list
            int ntdBMAId = ((SwitchingNtdBMA)dpSiteModel.getSiteModel(i).getSubstitutionModel()).getIDNumber();
            int ratesId = dpSiteModel.getSiteModel(i).getRateParameter().getIDNumber();

            //Create the tree likelihood
            //WVTreeLikelihood treeLik = new WVTreeLikelihood(clusterWeights[ntdBMAId][ratesId]);

            NewWVTreeLikelihood treeLik = new NewWVTreeLikelihood(
                    clusterWeights[ntdBMAId][ratesId],
                    alignment,
                    treeInput.get(),
                    useAmbiguitiesInput.get(),
                    dpSiteModel.getSiteModel(i),
                    branchRateModelInput.get());

            //Add to list and matrix for the convenience of processesing
            treeLiks.add(treeLik);
            treeLiksMatrix[ntdBMAId][ratesId] = treeLik;
            storedTreeLiksMatrix[ntdBMAId][ratesId] = treeLik;
        }
    }

    /*public double calculateLogP() throws Exception{
        int sum = getSumWeight();
        if(sum != alignment.getSiteCount()){
            throw new RuntimeException("Weights ("+sum+") and site count("+alignment.getSiteCount()+").");
        }
        return super.calculateLogP();
    }*/

    private void update(){
        int dirtySite = dpSiteModel.getLastDirtySite();
        int[] currClusterIds = dpSiteModel.getCurrClusters(dirtySite);
        int[] prevClusterIds = dpSiteModel.getPrevClusters(dirtySite);

        //Create a new tree likelihood (with zero weights) when there is a new site model.
        if(treeLiksMatrix[currClusterIds[dpSiteModel.NTDBMA]][currClusterIds[dpSiteModel.RATES]] == null){
            //System.out.println("Add clusters at: "+currClusterIds[dpSiteModel.NTDBMA]+" "+currClusterIds[dpSiteModel.RATES]);
            addTreeLikelihood(currClusterIds[dpSiteModel.NTDBMA],currClusterIds[dpSiteModel.RATES]);
        }
        //System.out.println(prevClusterIds[dpSiteModel.NTDBMA]+" "+prevClusterIds[dpSiteModel.RATES]);

        //Move weight
        moveWeight(
            prevClusterIds[dpSiteModel.NTDBMA],
            prevClusterIds[dpSiteModel.RATES],
            currClusterIds[dpSiteModel.NTDBMA],
            currClusterIds[dpSiteModel.RATES],
            dirtySite,
            1
        );

        //Remove likelihoods that have zero weights
        if(dpSiteModel.getCombinationWeight(prevClusterIds[dpSiteModel.NTDBMA], prevClusterIds[dpSiteModel.RATES]) == 0){
            treeLiks.remove(treeLiksMatrix[prevClusterIds[dpSiteModel.NTDBMA]][prevClusterIds[dpSiteModel.RATES]]);
            treeLiksMatrix[prevClusterIds[dpSiteModel.NTDBMA]][prevClusterIds[dpSiteModel.RATES]] = null;
        }
    }

    private void updates(){
        int[] dirtySites =  dpSiteModel.getLastDirtySites();
        for(int dirtySite:dirtySites){
            update(dirtySite);            
        }

        for(int dirtySite:dirtySites){
        //int dirtySite = dpSiteModel.getLastDirtySite();
            int[] prevClusterIds = dpSiteModel.getPrevClusters(dirtySite);

        //Remove likelihoods that have zero weights
            if(dpSiteModel.getCombinationWeight(prevClusterIds[dpSiteModel.NTDBMA], prevClusterIds[dpSiteModel.RATES]) == 0){
                //System.out.println("Remove?");
                treeLiks.remove(treeLiksMatrix[prevClusterIds[dpSiteModel.NTDBMA]][prevClusterIds[dpSiteModel.RATES]]);
                treeLiksMatrix[prevClusterIds[dpSiteModel.NTDBMA]][prevClusterIds[dpSiteModel.RATES]] = null;
            }
        }
    }

    private void update(int dirtySite){
        //int dirtySite = dpSiteModel.getLastDirtySite();
        int[] currClusterIds = dpSiteModel.getCurrClusters(dirtySite);
        int[] prevClusterIds = dpSiteModel.getPrevClusters(dirtySite);

        //Create a new tree likelihood (with zero weights) when there is a new site model.
        if(treeLiksMatrix[currClusterIds[dpSiteModel.NTDBMA]][currClusterIds[dpSiteModel.RATES]] == null){
            //System.out.println("Add cluster at: "+currClusterIds[dpSiteModel.NTDBMA]+" "+currClusterIds[dpSiteModel.RATES]);
            addTreeLikelihood(currClusterIds[dpSiteModel.NTDBMA],currClusterIds[dpSiteModel.RATES]);
        }
        //System.out.println(prevClusterIds[dpSiteModel.NTDBMA]+" "+prevClusterIds[dpSiteModel.RATES]);
        //Move weight


        moveWeight(
            prevClusterIds[dpSiteModel.NTDBMA],
            prevClusterIds[dpSiteModel.RATES],
            currClusterIds[dpSiteModel.NTDBMA],
            currClusterIds[dpSiteModel.RATES],
            dirtySite,
            1
        );

        /*    for(int i = 0; i < treeLiksMatrix.length;i++){
                for(int j = 0; j < treeLiksMatrix[i].length; j++){
                    System.out.print(i +" "+j+": ");
                    if(treeLiksMatrix[i][j] == null){
                        System.out.println("null");

                    }else{
                    int[] weights = treeLiksMatrix[i][j].getPatternWeights();
                    for(int k = 0; k < weights.length;k++){
                        System.out.print(weights[k]+" ");
                    }
                    System.out.println();
                    }
                }
            }
    
         */


    }


    /*
     * Moving weight from one cluster combination to another
     */
    public void moveWeight(
            int ntdBMAIndexFrom,
            int rateIndexFrom,
            int ntdBMAIndexTo,
            int rateIndexTo,
            int dirtySite,
            int weight){
        /*for(int i = 0; i < treeLiksMatrix.length;i++){
                for(int j = 0; j < treeLiksMatrix[i].length; j++){
                    System.out.print(i +" "+j+": ");
                    if(treeLiksMatrix[i][j] == null){
                        System.out.println("null");

                    }else{
                    int[] weights = treeLiksMatrix[i][j].getPatternWeights();
                    for(int k = 0; k < weights.length;k++){
                        System.out.print(weights[k]+" ");
                    }
                    System.out.println();
                    }
                }
            }
        System.out.println(ntdBMAIndexFrom+" "+rateIndexFrom+" "+(treeLiksMatrix[ntdBMAIndexFrom][rateIndexFrom] ==null));*/
        //try{
        treeLiksMatrix[ntdBMAIndexFrom][rateIndexFrom].removeWeight(alignment.getPatternIndex(dirtySite),weight);
        treeLiksMatrix[ntdBMAIndexTo][rateIndexTo].addWeight(alignment.getPatternIndex(dirtySite),weight);
        //}catch(Exception e){
        //}
    }


    public void addTreeLikelihood(int substModelID, int rateID){
        //SiteModel siteModel = dpSiteModel.getLastAdded();
        SiteModel siteModel = dpSiteModel.getSiteModel(substModelID, rateID);


        int[] patternWeights = new int[alignment.getPatternCount()];

        //WVTreeLikelihood treeLik = new WVTreeLikelihood(patternWeights);
        //NewWVTreeLikelihood treeLik = new NewWVTreeLikelihood(patternWeights);
        NewWVTreeLikelihood treeLik = new NewWVTreeLikelihood(
                    patternWeights,
                    alignment,
                    treeInput.get(),
                    useAmbiguitiesInput.get(),
                    siteModel,
                    branchRateModelInput.get());
        try{



            treeLik.calculateLogP();
            treeLik.store();
            treeLiks.add(treeLik);
            treeLiksMatrix[substModelID][rateID] = treeLik;
        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    public double getSiteLogLikelihood(int iCluster, int iSite){
        throw new RuntimeException("Retrieving the site given a site index and cluster index is not applicable in this case.");
    }

    /*
     * @param inputType either ntdBMA or rates
     * @clusterID id of the cluster
     * @siteIndex the index of a site in an alignment
     */
    public double getSiteLogLikelihood(int inputType, int clusterID, int siteIndex){
        //System.out.println("pattern: "+alignment.getPatternIndex(siteIndex));
        int[] currClusters = dpSiteModel.getCurrClusters(siteIndex);
        //System.out.println("clusterID: "+clusterID+" "+prevClusters[dpSiteModel.RATES]+" "+alignment.getPatternIndex(siteIndex));
        if(inputType == dpSiteModel.NTDBMA){
            if(treeLiksMatrix[clusterID][currClusters[dpSiteModel.RATES]] != null){

                //WVTreeLikelihood tmpTL = treeLiksMatrix[clusterID][prevClusters[dpSiteModel.RATES]];
                NewWVTreeLikelihood tmpTL = treeLiksMatrix[clusterID][currClusters[dpSiteModel.RATES]];
                //System.out.println("clusterID: "+clusterID+" "+prevClusters[dpSiteModel.RATES]+" "+alignment.getPatternIndex(siteIndex));
                //tmpTL.printThings();
                return tmpTL.getPatternLogLikelihood(alignment.getPatternIndex(siteIndex));

            }
        }else{

            if(treeLiksMatrix[currClusters[dpSiteModel.NTDBMA]][clusterID] != null){
                //WVTreeLikelihood tmpTL = treeLiksMatrix[prevClusters[dpSiteModel.NTDBMA]][clusterID];
                //System.out.println("hi!!");
                NewWVTreeLikelihood tmpTL = treeLiksMatrix[currClusters[dpSiteModel.NTDBMA]][clusterID];
                return tmpTL.getPatternLogLikelihood(alignment.getPatternIndex(siteIndex));
            }
        }

        return Double.NaN;

    }




    public void printThings(){
        //for(WVTreeLikelihood treeLik:treeLiks){
        for(NewWVTreeLikelihood treeLik:treeLiks){
            treeLik.printThings();
            for(int i = 0; i < alignment.getPatternCount();i++){
                System.out.print(treeLik.getPatternLogLikelihood(alignment.getPatternIndex(i))+" ");
            }
            System.out.println();
        }
        System.out.print(0+" "+0+" ");
        treeLiksMatrix[0][0].printThings();
        System.out.print(0+" "+1+" ");
        treeLiksMatrix[0][1].printThings();
        System.out.print(0+" "+2+" ");
        treeLiksMatrix[0][2].printThings();
        System.out.print(0+" "+3+" ");
        treeLiksMatrix[0][3].printThings();
    }


    public int getSumWeight(){
        int sum = 0;
        //for(WVTreeLikelihood treeLik: treeLiks){
        for(NewWVTreeLikelihood treeLik: treeLiks){
            sum +=treeLik.weightSum();
        }
        return sum;
    }


    //boolean storeTreeLikelihoods = false;

    @Override
    protected boolean requiresRecalculation() {
        /*for(int i = 0; i < treeLiksMatrix.length; i++){
                for(int j = 0; j < treeLiksMatrix[i].length; j++){
                    if(treeLiksMatrix[i][j] != null)
                        System.out.println("treeLik: "+i+" "+j+" "+treeLiksMatrix[i][j].m_siteModel.isDirtyCalculation()+" "
                                +(dpSiteModel.storedSiteModelsMatrix[i][j] == treeLiksMatrix[i][j].m_siteModel));
                }
            } */
        boolean recalculate = false;
        if(dpSiteModel.isDirtyCalculation()){

            changeType = dpSiteModel.getChangeType();
            //System.out.println("treeLik requires recal!!"+changeType);
            if(changeType == ChangeType.ADDED || changeType == ChangeType.REMOVED || changeType == ChangeType.POINTER_CHANGED){
                //System.out.println("changeType: "+changeType);
                update();
            }else if(changeType == ChangeType.SPLIT || changeType == ChangeType.MERGE){
                //storeTreeLikelihoods();

                updates();
            }else if(changeType == ChangeType.POINTERS_SWAPPED){
                int[] dirtySites = dpSiteModel.getLastDirtySites();
                if(dirtySites[0] !=  dirtySites[1]){
                    updates();
                }
            }else if(changeType == ChangeType.VALUE_CHANGED){

                changeType = ChangeType.VALUE_CHANGED;

            }else{
                changeType = ChangeType.ALL;
            }
            recalculate = true;
        }else if(treeInput.get().somethingIsDirty()){
            recalculate = true;

        }else if(branchRateModelInput.get().isDirtyCalculation()){
            recalculate = true;
        }
        
        if(recalculate){
            //SiteModel siteModel = (SiteModel)treeLiks.get(0).m_siteModel;
            for(TreeLikelihood treeLik:treeLiks){
                MCMCNodeFactory.checkDirtiness(treeLik);
                //System.out.println(treeLik.m_siteModel.isDirtyCalculation());

            }


        }
        return recalculate;
    }


    public void store(){
        //storeTreeLikelihoods = true;
        for(int i = 0; i < treeLiksMatrix.length;i++){
            System.arraycopy(treeLiksMatrix[i],0,storedTreeLiksMatrix[i],0, treeLiksMatrix[i].length);
        }
        super.store();

    }

    /*public void storeTreeLikelihoods(){
        storeTreeLikelihoods = true;
        for(int i = 0; i < treeLiksMatrix.length;i++){
            System.arraycopy(treeLiksMatrix[i],0,storedTreeLiksMatrix[i],0, treeLiksMatrix[i].length);
        }
        //super.store();
    }*/

    public void restore(){
        //if(storeTreeLikelihoods){
        NewWVTreeLikelihood[][] tmp = treeLiksMatrix;
        treeLiksMatrix = storedTreeLiksMatrix;
        storedTreeLiksMatrix = tmp;
        //storeTreeLikelihoods = false;
        //}
        super.restore();
    }



}