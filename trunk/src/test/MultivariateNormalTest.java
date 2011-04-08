package test;

import beast.core.parameter.RealParameter;
import beast.core.parameter.IntegerParameter;
import beast.math.distributions.Multinomial;

/**
 * @author Chieh-Hsi Wu
 */
public class MultivariateNormalTest {
    interface Instance {

        String getMeans();
        String[] getPrecisions();
        double getX();
        double getFLogX();
    }

    /*Instance test0 = new Instance() {
        public String getMeans (){

            return "3 4 5 6";
        }

        public String[] getPrecisions(){
            String[] precisionMatrixRows = {
                    "0.9594002 0.9529819 0.3554853 0.1695793",
                    "0.1614785 0.7385403 0.3658894 0.6902582",
                    "0.2329883 0.9821853 0.2060556 0.8099044",
                    "0.8257352 0.1558912 0.4278813 0.0275757"
            };
            return precisionMatrixRows;
        }

        public double[] getX(){
            retu
        }

        public double getFLogX(){
            return -4.89440954649;
        }

    };


    Instance test1 = new Instance() {
        public String getProbs (){

            return "0.15 0.23 0.14 0.07 0.25 0.16";
        }

        public String getX(){
            return "3 2 1 6 2 1";
        }

        public double getFLogX(){
            return -13.0155888173;
        }

    };

    Instance test2 = new Instance() {
        public String getProbs (){

            return "0.11 0.22 0.12 0.24 0.3 0.01";
        }

        public String getX(){
            return "1 2 4 6 7 0";
        }

        public double getFLogX(){
            return -7.3470894106;
        }

    };

    Instance test4 = new Instance() {
        public String getProbs (){

            return "0.45 0.72 0.64 0.32 0.82 0.16 0.16 0.15 0.08 0.49";
        }

        public String getX(){
            return "1 8 4 0 1 0 2 0 1 3";
        }

        public double getFLogX(){
            return -15.3488523568;
        }

    };

    Instance[] all = new Instance[]{test0,test1,test2};

    public void TestMultinomial() throws Exception{
        for(Instance test: all){
            RealParameter probs = new RealParameter();
            IntegerParameter x  = new IntegerParameter();
            probs.initByName(
                    "value",test.getProbs(),
                    "lower","0",
                    "upper","1");
            x.initByName("value", test.getX(),
                    "lower", "0",
                    "upper", "100000000");
            Multinomial multiNom = new Multinomial();
            multiNom.initByName("probs", probs);
            double fLogX = multiNom.calcLogP(x);
            assertEquals(fLogX, test.getFLogX(), 1e-10);

        }
    } */   

}