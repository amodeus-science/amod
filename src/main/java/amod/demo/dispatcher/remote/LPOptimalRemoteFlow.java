package amod.demo.dispatcher.remote;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;

public class LPOptimalRemoteFlow {
    /** map with variableIDs in problem set up and linkIDs of virtualNetwork */
    private final Map<List<Integer>, Integer> rIDvarID = new HashMap<>();
    private final Map<List<Integer>, Integer> xIDvarID = new HashMap<>();
    private final Map<List<Integer>, Integer> dIDvarID = new HashMap<>();
    private final Map<List<Integer>, Integer> qIDvarID = new HashMap<>();
    private final glp_smcp parmLP = new glp_smcp();
    private final glp_iocp parmMILP = new glp_iocp();
    private final int nvNodes;
    private final int timeHorizon;
    private final Tensor travelTimes;
    private final Tensor lambda;
    private final Tensor starters;
    private final int nRvariables;
    private final int nXvariables;
    private final int nDvariables;
    private final int nQvariables;
    private final int nVehcileCons;
    private final int nRemoteCons;
    private final int nRemoteMaxCons;
    private final int nWaitingCustom;
    private final int rowTotal;
    private final int columnTotal;
    private final double tuningCoeffD;
    private final double tuningCoeffR;
    private final boolean milpFlag;
    // ---
    private glp_prob lp;
    private Tensor r_ij;
    private Tensor x_ij;
    private Tensor r_ijNotRound;
    private Tensor x_ijNotRound;
    private int columnId;
    private int rowId;

    /**
     * @param virtualNetwork
     *            the virtual network (not necessarily complete graph) on which
     *            the optimization is computed.
     */
    public LPOptimalRemoteFlow(VirtualNetwork<Link> virtualNetwork, int timeHorizon, Tensor travelTimes, Tensor starters,
            Tensor lambda, boolean milpFlag) {
        this.timeHorizon = timeHorizon;
        this.travelTimes = travelTimes;
        this.lambda = lambda;
        this.starters = starters;
        this.milpFlag = milpFlag;
        nvNodes = virtualNetwork.getvNodesCount();
        nRvariables = nvNodes * nvNodes * timeHorizon;
        nXvariables = nvNodes * nvNodes * timeHorizon;
        nDvariables = nvNodes * nvNodes * timeHorizon;
        nQvariables = nvNodes * nvNodes * timeHorizon;
        columnTotal = nRvariables + nXvariables + nDvariables + nQvariables;
        nVehcileCons = nvNodes * timeHorizon;
        nRemoteCons = nvNodes * nvNodes * timeHorizon;
        nRemoteMaxCons = timeHorizon;
        nWaitingCustom = nvNodes * nvNodes * timeHorizon;
        rowTotal = nVehcileCons + nWaitingCustom + nRemoteCons + nRemoteMaxCons;
        tuningCoeffD = 10000;
        tuningCoeffR = 5;

        r_ij = Array.zeros(nvNodes, nvNodes);
        x_ij = Array.zeros(nvNodes, nvNodes);
        r_ijNotRound = Array.zeros(nvNodes, nvNodes);
        x_ijNotRound = Array.zeros(nvNodes, nvNodes);
        
        if(milpFlag) {
            System.out.println("creating min flow MILP for system with " + rowTotal + " number of constraints and "
                    + columnTotal + " optimization variables.");
        } else {
            System.out.println("creating min flow LP for system with " + rowTotal + " number of constraints and "
                    + columnTotal + " optimization variables.");
        }

        
    }

    /** initiate the linear program */
    public void initiateLP() {
        try {
            lp = GLPK.glp_create_prob();
            GLPK.glp_set_prob_name(lp, "Rebalancing and Dispatching Problem");
            System.out.println("Problem created");

            // initiate COLUMN variables
            GLPK.glp_add_cols(lp, columnTotal);
            columnId = 0;

            initColumnR_ijt();
            initColumnX_ijt();
            initColumnD_ijt();
            initColumnQ_ijt();
            
            GlobalAssert.that(columnTotal == columnId);
            

            // initiate auxiliary ROW variables
            GLPK.glp_add_rows(lp, rowTotal);
            rowId = 0;

            // Allocate memory NOTE: the first value in this array is not used
            // as variables are counted 1,2,3,...,n*n
            SWIGTYPE_p_int ind = GLPK.new_intArray(columnTotal + 1);
            SWIGTYPE_p_double val = GLPK.new_doubleArray(columnTotal + 1);

            initRowConstraints(ind, val);

            GlobalAssert.that(rowTotal == rowId);

            // Free memory
            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);

            // OBJECTIVE vector
            GLPK.glp_set_obj_name(lp, "J");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);

            initObj();

        } catch (

        GlpkException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * solves the MILPMinFlow problem, given the right-hand side of the
     * equations and stores the solution alpha
     */
    public void solveLP(boolean mute) {

        GLPK.glp_term_out(mute ? GLPK.GLP_OFF : GLPK.GLP_ON);
        int ret = 0;
        int stat = 0;

        if(milpFlag == true) {

            GLPK.glp_init_iocp(parmMILP);
            parmMILP.setPresolve(GLPK.GLP_ON);
            parmMILP.setMip_gap(0.1);
//            parm.setTm_lim(10000);
            ret = GLPK.glp_intopt(lp, parmMILP); // ret==0 indicates of the
            // algorithm ran correctly
            stat = GLPK.glp_mip_status(lp);
        } else {
          GLPK.glp_init_smcp(parmLP);
          ret = GLPK.glp_simplex(lp, parmLP); // ret==0 indicates of the
                                    //           algorithm ran correctly
          stat = GLPK.glp_get_status(lp);
        } 
        
        GlobalAssert.that(ret == 0 || ret == 14);
        
        if (stat == GLPK.GLP_NOFEAS) {
            System.out.println("LP has found infeasible solution");
            closeLP();
            GlobalAssert.that(false);
        }

        if (stat == GLPK.GLP_FEAS) {
            System.out.println("LP has found suboptimal feasible solution");
        }
        
        if (stat == GLPK.GLP_OPT) {
            System.out.println("LP has found optimal solution");
        }
        readRandXoptimal();
        writeLPSolution();
        closeLP();
    }

    /** closing the MILP in order to release allocated memory */
    public void closeLP() {
        // release storage allocated for LP
        GLPK.glp_delete_prob(lp);
    }

    private void initColumnR_ijt() {
        // optimization variable alpha_ij
        for(int i=0; i<nvNodes; i++) {
            for(int j=0; j<nvNodes; j++) {
                for (int t = 1; t <= timeHorizon; t++) {
                    columnId++;
                    // variable name and initialization
                    String varName = ("r" + "_" + i + "," + j + "," + t);
                    GLPK.glp_set_col_name(lp, columnId, varName);
                    if (t == 1 && milpFlag==true) {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_IV);
                    } else {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_CV);
                    }

                    GLPK.glp_set_col_bnds(lp, columnId, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower
                                                                                         // bound:
                                                                                         // second
                                                                                         // number
                                                                                         // irrelevant
                    rIDvarID.put(Arrays.asList(i, j, t), columnId);
                }
            }
        }
    }

    private void initColumnX_ijt() {
        // optimization variable alpha_ij
        for(int i=0; i<nvNodes; i++) {
            for(int j=0; j<nvNodes; j++) {
                for (int t = 1; t <= timeHorizon; t++) {
                    columnId++;
                    // variable name and initialization
                    String varName = ("x" + "_" + i + "," + j + "," + t);
                    GLPK.glp_set_col_name(lp, columnId, varName);
                    if (t == 1 && milpFlag == true) {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_IV);
                    } else {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_CV);
                    }

                    GLPK.glp_set_col_bnds(lp, columnId, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower
                                                                                         // bound:
                                                                                         // second
                                                                                         // number
                                                                                         // irrelevant
                    xIDvarID.put(Arrays.asList(i, j, t), columnId);
                }
            }
        }
    }


    private void initColumnD_ijt() {
        // optimization variable alpha_ij
        for(int i=0; i<nvNodes; i++) {
            for(int j=0; j<nvNodes; j++) {
                for (int t = 1; t <= timeHorizon; t++) {
                    columnId++;
                    // variable name and initialization
                    String varName = ("d" + "_" + i + "," + j + "," + t);
                    GLPK.glp_set_col_name(lp, columnId, varName);
                    if (t == 1 && milpFlag == true) {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_IV);
                    } else {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_CV);
                    }

                    GLPK.glp_set_col_bnds(lp, columnId, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower
                                                                                         // bound:
                                                                                         // second
                                                                                         // number
                                                                                         // irrelevant
                    dIDvarID.put(Arrays.asList(i, j, t), columnId);
                }
            }
        }
    }
    
    private void initColumnQ_ijt() {
        // optimization variable alpha_ij
        for(int i=0; i<nvNodes; i++) {
            for(int j=0; j<nvNodes; j++) {
                for (int t = 1; t <= timeHorizon; t++) {
                    columnId++;
                    // variable name and initialization
                    String varName = ("q" + "_" + i + "," + j + "," + t);
                    GLPK.glp_set_col_name(lp, columnId, varName);
                    if (t == 1 && milpFlag == true) {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_IV);
                    } else {
                        GLPK.glp_set_col_kind(lp, columnId, GLPKConstants.GLP_CV);
                    }

                    GLPK.glp_set_col_bnds(lp, columnId, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower
                                                                                         // bound:
                                                                                         // second
                                                                                         // number
                                                                                         // irrelevant
                    qIDvarID.put(Arrays.asList(i, j, t), columnId);
                }
            }
        }
    }

    private void initRowConstraints(SWIGTYPE_p_int ind, SWIGTYPE_p_double val) {

        // vehicle conservation
        for (int t = 1; t <= timeHorizon; t++) {
            for (int i = 0; i < nvNodes; i++) {
                // set all coefficient entries of matrix A to zero first
                for (int var = 1; var <= columnTotal; var++) {
                    GLPK.intArray_setitem(ind, var, var);
                    GLPK.doubleArray_setitem(val, var, 0.0);
                }
                rowId++;
                String varName = ("C" + "_" + rowId);
                GLPK.glp_set_row_name(lp, rowId, varName);
                double s_it = starters.Get(i, t-1).number().doubleValue();
                GLPK.glp_set_row_bnds(lp, rowId, GLPKConstants.GLP_FX, s_it, 0.0);
                for (int j = 0; j < nvNodes; j++) {
                    int tau_ji = travelTimes.Get(j, i).number().intValue();
                    if (t > tau_ji) {
                        int indexRarrive = rIDvarID.get(Arrays.asList(j, i, (t - tau_ji)));
                        GLPK.intArray_setitem(ind, indexRarrive, indexRarrive);
                        GLPK.doubleArray_setitem(val, indexRarrive, -1.0);
                        int indexXarrive = xIDvarID.get(Arrays.asList(j, i, (t - tau_ji)));
                        GLPK.intArray_setitem(ind, indexXarrive, indexXarrive);
                        GLPK.doubleArray_setitem(val, indexXarrive, -1.0);
                    }
                    int indexR = rIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexR, indexR);
                    GLPK.doubleArray_setitem(val, indexR, 1.0);
                    int indexX = xIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexX, indexX);
                    GLPK.doubleArray_setitem(val, indexX, 1.0);
                }
                GLPK.glp_set_mat_row(lp, rowId, columnTotal, ind, val);
            }
        }
        
        for (int t = 1; t <= timeHorizon; t++) {
         // set all coefficient entries of matrix A to zero first
            for (int var = 1; var <= columnTotal; var++) {
                GLPK.intArray_setitem(ind, var, var);
                GLPK.doubleArray_setitem(val, var, 0.0);
            }
            rowId++;
            String varName = ("C" + "_" + rowId);
            GLPK.glp_set_row_name(lp, rowId, varName);
            GLPK.glp_set_row_bnds(lp, rowId, GLPKConstants.GLP_FX, 0, 0.0);
            for (int i = 0; i < nvNodes; i++) {             
                for (int j = 0; j < nvNodes; j++) {
                    int tau_ji = travelTimes.Get(j, i).number().intValue();
                    if (t < tau_ji) {
                        int indexRarrive = rIDvarID.get(Arrays.asList(j, i, (t - tau_ji)));
                        GLPK.intArray_setitem(ind, indexRarrive, indexRarrive);
                        GLPK.doubleArray_setitem(val, indexRarrive, -1.0);
                        int indexXarrive = xIDvarID.get(Arrays.asList(j, i, (t - tau_ji)));
                        GLPK.intArray_setitem(ind, indexXarrive, indexXarrive);
                        GLPK.doubleArray_setitem(val, indexXarrive, -1.0);
                    }
                    int indexQ = qIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexQ, indexQ);
                    GLPK.doubleArray_setitem(val, indexQ, 1.0);
                }
            }
            GLPK.glp_set_mat_row(lp, rowId, columnTotal, ind, val);
        }

        for (int t = 1; t <= timeHorizon; t++) {
            for (int i = 0; i < nvNodes; i++) {
                for (int j = 0; j < nvNodes; j++) {
                    for (int var = 1; var <= columnTotal; var++) {
                        GLPK.intArray_setitem(ind, var, var);
                        GLPK.doubleArray_setitem(val, var, 0.0);
                    }
                    rowId++;
                    String varName = ("C" + "_" + rowId);
                    GLPK.glp_set_row_name(lp, rowId, varName);
                    int indexQ = qIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexQ, indexQ);
                    GLPK.doubleArray_setitem(val, indexQ, 1.0);
                    int indexX = xIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexX, indexX);
                    GLPK.doubleArray_setitem(val, indexX, -1.0);
                    int indexR = rIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexR, indexR);
                    GLPK.doubleArray_setitem(val, indexR, -1.0);
                    GLPK.glp_set_row_bnds(lp, rowId, GLPKConstants.GLP_FX, 0, 0.0);
                    GLPK.glp_set_mat_row(lp, rowId, columnTotal, ind, val);
                }
            }
        }
        
        // waiting customers
        for (int t = 1; t <= timeHorizon; t++) {
            for (int i = 0; i < nvNodes; i++) {
                for (int j = 0; j < nvNodes; j++) {
                    for (int var = 1; var <= columnTotal; var++) {
                        GLPK.intArray_setitem(ind, var, var);
                        GLPK.doubleArray_setitem(val, var, 0.0);
                    }
                    rowId++;
                    String varName = ("C" + "_" + rowId);
                    GLPK.glp_set_row_name(lp, rowId, varName);
                    int indexD = dIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.intArray_setitem(ind, indexD, indexD);
                    GLPK.doubleArray_setitem(val, indexD, 1.0);
                    double lambdasum = 0;
                    for (int tau = 1; tau <= t; tau++) {
                        int indexX = xIDvarID.get(Arrays.asList(i, j, tau));
                        GLPK.intArray_setitem(ind, indexX, indexX);
                        GLPK.doubleArray_setitem(val, indexX, 1.0);
                        double lambda_ijt = lambda.Get(i, j, tau-1).number().doubleValue();
                        lambdasum = lambdasum + lambda_ijt;
                    }
                    GLPK.glp_set_row_bnds(lp, rowId, GLPKConstants.GLP_FX, lambdasum, 0.0);
                    GLPK.glp_set_mat_row(lp, rowId, columnTotal, ind, val);
                }
            }
        }

    }

    private void initObj() {
        for (int t = 1; t <= timeHorizon; t++) {
            for(int i=0; i<nvNodes; i++) {
                for(int j=0; j<nvNodes; j++) {
                    int tij = travelTimes.Get(i, j).number().intValue();
                    int indexD = dIDvarID.get(Arrays.asList(i, j, t));
                    GLPK.glp_set_obj_coef(lp, indexD, tuningCoeffD);
                    
                    int indexR = rIDvarID.get(Arrays.asList(i, j, t));
                    if(i==j) {
                        double costRii = 0.7*tuningCoeffR * tij;
                        GLPK.glp_set_obj_coef(lp, indexR, costRii);
                    } else {
                        double costRij = tuningCoeffR*tij;
                        GLPK.glp_set_obj_coef(lp, indexR, costRij);
                    }
                    
//                    int indexX = xIDvarID.get(Arrays.asList(i, j, t));
//                    double costX = tuningCoeffX * tij;
//                    GLPK.glp_set_obj_coef(lp, indexX, costX);
                }
            }
        }

    }

    private void readRandXoptimal() {
        for(int i=0; i<nvNodes; i++) {
            for(int j=0; j<nvNodes; j++) {
                int indexR = rIDvarID.get(Arrays.asList(i, j, 1));
                int indexX = xIDvarID.get(Arrays.asList(i, j, 1));
                if(milpFlag == true) {
                    if(i == j) {
                        r_ij.set(RealScalar.of(0), i, j);
                        r_ijNotRound.set(RealScalar.of(0), i, j);
                    } else {
                        r_ij.set(RealScalar.of(Math.round(GLPK.glp_mip_col_val(lp, indexR))), i, j);
                        r_ijNotRound.set(RealScalar.of(GLPK.glp_mip_col_val(lp, indexR)), i, j);
                    }
                    
                    x_ij.set(RealScalar.of(Math.round(GLPK.glp_mip_col_val(lp, indexX))), i, j);
                    x_ijNotRound.set(RealScalar.of(GLPK.glp_mip_col_val(lp, indexX)), i, j);
                } else {
                    if(i == j) {
                        r_ij.set(RealScalar.of(0), i, j);
                        r_ijNotRound.set(RealScalar.of(0), i, j);
                    } else {
                        System.out.println(GLPK.glp_get_col_prim(lp, indexR));
                        r_ij.set(RealScalar.of(Math.round(GLPK.glp_get_col_prim(lp, indexR))), i, j);
                        r_ijNotRound.set(RealScalar.of(GLPK.glp_get_col_prim(lp, indexR)), i, j);
                    }
                    System.out.println(GLPK.glp_get_col_prim(lp, indexX));
                    x_ij.set(RealScalar.of(Math.round(GLPK.glp_get_col_prim(lp, indexX))), i, j);
                    x_ijNotRound.set(RealScalar.of(GLPK.glp_get_col_prim(lp, indexX)), i, j);
                }
                
            }
        }
    }

    /** writes the solution of the LP on the consoles */
    public void writeLPSolution() {
        System.out.println("The solution is:");
        System.out.println("The Rebalancing: " + r_ij);
        System.out.println("The Dispatching: " + x_ij);
        
        int lambdasum = 0;
        double servedCustomer = 0;
        if(milpFlag == true) {
            for(int i=0; i<nvNodes; i++) {
                for(int j=0; j<nvNodes; j++) {
                    for(int t=1; t<=timeHorizon; t++) {
                        lambdasum = lambdasum + lambda.Get(i,j,t-1).number().intValue();
                        int xIndex = xIDvarID.get(Arrays.asList(i, j, t));
                        servedCustomer = servedCustomer + GLPK.glp_mip_col_val(lp, xIndex);
                    }
                }
            }
        } else {
            for(int i=0; i<nvNodes; i++) {
                for(int j=0; j<nvNodes; j++) {
                    for(int t=1; t<=timeHorizon; t++) {
                        lambdasum = lambdasum + lambda.Get(i,j,t-1).number().intValue();
                        int xIndex = xIDvarID.get(Arrays.asList(i, j, t));
                        servedCustomer = servedCustomer + GLPK.glp_get_col_prim(lp, xIndex);
                    }
                }
            }
        }
 
        
        double droppedCustomers = lambdasum - servedCustomer;
        
        System.out.println("Number of customers: " + lambdasum);
        System.out.println("Number of served customers: " + servedCustomer);
        System.out.println("Number of dropped customers: " + droppedCustomers);
   
    }

    /**
     * Returns the last solution of the LPMinFlow as Tensor. E.g. alpha(i,j)=n
     * means that n objects have to be sent from i to j
     */
    public Tensor getr_ij() {
        return r_ij;
    }
    
    /**
     * Returns the last solution of the LPMinFlow as Tensor. E.g. alpha(i,j)=n
     * means that n objects have to be sent from i to j
     */
    public Tensor getx_ij() {
        return x_ij;
    }
}
