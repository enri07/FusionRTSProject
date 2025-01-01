/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fusionrts;

import ai.*;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import java.util.ArrayList;
import java.util.List;

import ai.mcts.MCTSNode;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;
import tournaments.Tournament;

/**
 *
 */
public class FusionRTS extends AIWithComputationBudget implements InterruptibleAI {
    public static int DEBUG = 0;
    public EvaluationFunction ef;
       
    public AI playoutPolicy = new RandomBiasedAI();
    protected long maxActionsSoFar = 0;
    
    protected GameState gsToStartFrom;
    protected FusionRTSNode tree;
    protected int current_iteration = 0;
            
    public int MAX_SIMULATION_TIME = 1024;
    public int MAX_TREE_DEPTH = 10;
    
    protected int player;
    
    public float epsilon_0 = 0.2f;
    public float epsilon_l = 0.25f;
    public float epsilon_g = 0.0f;

    // these variables are for using a discount factor on the epsilon values above. My experiments indicate that things work better without discount
    // So, they are just maintained here for completeness:
    public float initial_epsilon_0 = 0.2f;
    public float initial_epsilon_l = 0.25f;
    public float initial_epsilon_g = 0.0f;
    public float discount_0 = 0.999f;
    public float discount_l = 0.999f;
    public float discount_g = 0.999f;
    
    public int globalStrategy = FusionRTSNode.UCB1;
    public boolean forceExplorationOfNonSampledActions = true;
    
    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;
    public long total_time = 0;
    public long avgTimeSimulation = 0;
    public long avgDeepTree = 0;
    
    // NEW: Progressive History enhancement
    public boolean PH_enabled = false;
    public GlobalMaps_PH PHStructures;
    // NEW: Tree Reuse enhancement
    public boolean TR_enabled = false;
    public FusionRTSNode rootToBeReused = null;
    // NEW: AWLM heuristic
    public boolean AWLM_enabled = false;

    public FusionRTS(UnitTypeTable utt) {
        this(100,-1,100,10,
             0.3f, 0.0f, 0.4f,
             new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true,
             false, false, false);
    }

    // Specific build function to determine which enanchment we want to enable
    public FusionRTS(UnitTypeTable utt, boolean PH_flag, boolean TR_flag,
            boolean heuristic_flag ) {
        this(100,-1,100,10,
             0.3f, 0.0f, 0.4f,
             new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true,
             PH_flag, TR_flag, heuristic_flag);
        
    }    
    
    
    public FusionRTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                               float e_l, float discout_l,
                               float e_g, float discout_g, 
                               float e_0, float discout_0, 
                               AI policy, EvaluationFunction a_ef,
                               boolean fensa, boolean PH_flag, boolean TR_flag,
                               boolean AWLM_flag) {
        super(available_time, max_playouts);
        MAX_SIMULATION_TIME = lookahead;
        playoutPolicy = policy;
        MAX_TREE_DEPTH = max_depth;
        initial_epsilon_l = epsilon_l = e_l;
        initial_epsilon_g = epsilon_g = e_g;
        initial_epsilon_0 = epsilon_0 = e_0;
        discount_l = discout_l;
        discount_g = discout_g;
        discount_0 = discout_0;
        ef = a_ef;
        forceExplorationOfNonSampledActions = fensa;
        
        // Set the enanchments to be used
        
        // Progressive history
        if (PH_flag) {
            globalStrategy = FusionRTSNode.UCB1PH;
            PHStructures = new GlobalMaps_PH();
        }
        else{
            globalStrategy = FusionRTSNode.UCB1;
        }
        
        // Tree reuse
        if (TR_flag) {
            TR_enabled = true;
        }
        
        // AWLM heuristic
        if(AWLM_flag) {
            ef = new AWLM_EvaluationFunction();
        }
    }    

    public FusionRTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                            float e_l, float e_g, float e_0, 
                            AI policy, EvaluationFunction a_ef, 
                            boolean fensa, boolean PH_flag, boolean TR_flag,
                            boolean AWLM_flag) {
        super(available_time, max_playouts);
        MAX_SIMULATION_TIME = lookahead;
        playoutPolicy = policy;
        MAX_TREE_DEPTH = max_depth;
        initial_epsilon_l = epsilon_l = e_l;
        initial_epsilon_g = epsilon_g = e_g;
        initial_epsilon_0 = epsilon_0 = e_0;
        discount_l = 1.0f;
        discount_g = 1.0f;
        discount_0 = 1.0f;
        ef = a_ef;
        forceExplorationOfNonSampledActions = fensa;
        
        // Set the enanchments to be used
        
        // Progressive history
        if (PH_flag) {
            globalStrategy = FusionRTSNode.UCB1PH;
            PHStructures = new GlobalMaps_PH();
        } else {
            globalStrategy = FusionRTSNode.UCB1;
        }
        
        // Tree reuse
        if (TR_flag) {
            TR_enabled = true;
        }
        
        // AWLM heuristic
        if(AWLM_flag) {
            ef = new AWLM_EvaluationFunction();
        }
    }
    
    public void reset() {
        tree = null;
        gsToStartFrom = null;
        total_runs = 0;
        total_cycles_executed = 0;
        total_actions_issued = 0;
        total_time = 0;
        current_iteration = 0;
    }    
        
    
    public AI clone() {
        return new FusionRTS(TIME_BUDGET, ITERATIONS_BUDGET, MAX_SIMULATION_TIME, 
                MAX_TREE_DEPTH, epsilon_l, discount_l, epsilon_g, discount_g, 
                epsilon_0, discount_0, playoutPolicy, ef, forceExplorationOfNonSampledActions,
                PH_enabled, TR_enabled, AWLM_enabled);
    }    
    
    
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        
        if (gs.canExecuteAnyAction(player)) {
            startNewComputation(player, gs.clone()); // It simply generates the root node with all the possible actions to be computed (CHILD NODES NOT INITIALIZED)
            computeDuringOneGameFrame();
            return getBestActionSoFar();
        } else {
            return new PlayerAction();        
        }       
    }
    
    
    public void startNewComputation(int a_player, GameState gs) throws Exception {
        player = a_player;
        current_iteration = 0;

        // New: tree reuse enhancement
        // if it can not find the same state then it makes a new root 
        tree = findNewState(gs);
        if(tree == null) {
            tree = new FusionRTSNode(player, 1 - player, gs, null, ef.upperBound(gs),
                    current_iteration++, forceExplorationOfNonSampledActions,
                    PHStructures, null);
        }

        
        if (tree.moveGenerator == null) {
            maxActionsSoFar = 0;
        } else {
            maxActionsSoFar = Math.max(tree.moveGenerator.getSize(), maxActionsSoFar); // Number of available actions from current state     
        }
        gsToStartFrom = gs;
        
        epsilon_l = initial_epsilon_l;
        epsilon_g = initial_epsilon_g;
        epsilon_0 = initial_epsilon_0;        
    }

    public void computeDuringOneGameFrame() throws Exception {        
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        long end = start;
        long count = 0;
        while(true) {
            // Here we call iteration method
            if (!iteration(player)) break;
            count++;
            end = System.currentTimeMillis();
            if (TIME_BUDGET>=0 && (end - start)>=TIME_BUDGET) break; 
            if (ITERATIONS_BUDGET>=0 && count>=ITERATIONS_BUDGET) break;   
            // We continue to cycle it until we end the computation time
        }
//        System.out.println("HL: " + count + " time: " + (System.currentTimeMillis() - start) + " (" + available_time + "," + max_playouts + ")");
        total_time += (end - start);
        total_cycles_executed++;
    }
    
    
    public boolean iteration(int player) throws Exception {
        // Selection + Expansion
        FusionRTSNode leaf = tree.selectLeaf(player, 1-player, epsilon_l, epsilon_g, 
                epsilon_0, globalStrategy, MAX_TREE_DEPTH, current_iteration++);

        if (leaf != null) {
            GameState gs = leaf.gs.clone();
            simulate(gs, gs.getTime() + MAX_SIMULATION_TIME); // Playout

            if(avgDeepTree > 0 && leaf.depth > 0) {
                avgDeepTree = (leaf.depth + avgDeepTree)/2;
            } else {
                if(avgDeepTree == 0) {
                    avgDeepTree = leaf.depth;
                }
            }

            int time = gs.getTime() - gsToStartFrom.getTime();
            if(avgTimeSimulation > 0 && time > 0) {
                avgTimeSimulation = (time + avgTimeSimulation)/2;
            } else {
                if(avgTimeSimulation == 0) {
                    avgTimeSimulation = time;
                }
            }
            double evaluation = ef.evaluate(player, 1 - player, gs) * Math.pow(0.99, time/10.0); // Evaluate final state

            leaf.propagateEvaluation(evaluation,null); // Backpropagation          

            // update the epsilon values:
            epsilon_0*=discount_0;
            epsilon_l*=discount_l;
            epsilon_g*=discount_g;
            total_runs++;
            
//            System.out.println(total_runs + " - " + epsilon_0 + ", " + epsilon_l + ", " + epsilon_g);
            
        } else {
            // no actions to choose from :)
            System.err.println(this.getClass().getSimpleName() + ": claims there are no more leafs to explore...");
            return false;
        }
        return true;
    }
    
    public PlayerAction getBestActionSoFar() {
        int idx = getMostVisitedActionIdx();
        if (idx == -1) {
            if (DEBUG >= 1) System.out.println("NaiveMCTS no children selected. Returning an empty action");
            return new PlayerAction();
        }
        if (DEBUG >= 2) tree.showNode(0,1,ef);
        if (DEBUG >= 1) {
            FusionRTSNode best = (FusionRTSNode) tree.children.get(idx);
            System.out.println("FusionRTS selected children " + tree.actions.get(idx) + " explored " + best.visit_count + " Avg evaluation: " + (best.accum_evaluation/((double)best.visit_count)));
        }

        // NEW: tree reuse enhancement
        if( TR_enabled ) {
            FusionRTSNode best = (FusionRTSNode) tree.children.get(idx);
            // Remove parent connection
            best.parent = null;
            best.depth = 0;
            rootToBeReused = best;
        }

        return tree.actions.get(idx);
    }
    
    
    public int getMostVisitedActionIdx() {
        total_actions_issued++;
            
        int bestIdx = -1;
        FusionRTSNode best = null;
        if (DEBUG>=2) {
            System.out.println("Number of playouts: " + tree.visit_count);
            tree.printUnitActionTable();
        }
        if (tree.children==null) return -1;
        for(int i = 0;i<tree.children.size();i++) {
            FusionRTSNode child = (FusionRTSNode)tree.children.get(i);
            if (DEBUG>=2) {
                System.out.println("child " + tree.actions.get(i) + " explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)));
            }
//            if (best == null || (child.accum_evaluation/child.visit_count)>(best.accum_evaluation/best.visit_count)) {
            if (best == null || child.visit_count>best.visit_count) {
                best = child;
                bestIdx = i;
            }
        }
        
        return bestIdx;
    }
        
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do {
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(playoutPolicy.getAction(0, gs));
                gs.issue(playoutPolicy.getAction(1, gs));
            }
        } while(!gameover && gs.getTime() < time);
    }

    // NEW: tree reuse
    public FusionRTSNode findNewState( GameState currentGs ) {
        // We look if any of the known child of the node has a game state equal
        // to the new one
        if (rootToBeReused == null || rootToBeReused.children == null) return null;
        for(MCTSNode child : rootToBeReused.children) {
            FusionRTSNode childrenNode = (FusionRTSNode)child;
            if(currentGs.equals(childrenNode.gs)) {
                // found correct node
                //System.out.println("Found it!");
                childrenNode.parent = null;
                childrenNode.depth = 0;
                return childrenNode;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + MAX_SIMULATION_TIME + "," + MAX_TREE_DEPTH + "," + epsilon_l + ", " + discount_l + ", " + epsilon_g + ", " + discount_g + ", " + epsilon_0 + ", " + discount_0 + ", " + playoutPolicy + ", " + ef + ")";
    }
    
    @Override
    public String statisticsString() {
        return "Total runs: " + total_runs + 
               ", runs per action: " + (total_runs/(float)total_actions_issued) + 
               ", runs per cycle: " + (total_runs/(float)total_cycles_executed) + 
               ", average time per cycle: " + (total_time/(float)total_cycles_executed) + 
               ", max branching factor: " + maxActionsSoFar;
    }
    @Override
    public String getTournamentColumnsStatistics() {
        return "avgTimeSimulation" + Tournament.splitter + "avgDeepTree"
                + (PHStructures != null ?
                    String.join(Tournament.splitter, PHStructures.statisticsVisitActions.keySet())
                    :
                    "")
                ;
    }

    @Override
    public String getTournamentStatistics() {
        return avgTimeSimulation + Tournament.splitter + avgDeepTree + Tournament.splitter
                + (PHStructures != null ?
                    String.join(Tournament.splitter, PHStructures.statisticsVisitActions.values().stream().map((Object::toString)).toList())
                    :
                    "")
                ;
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("MaxTreeDepth",int.class,10));
        
        parameters.add(new ParameterSpecification("E_l",float.class,0.3));
        parameters.add(new ParameterSpecification("Discount_l",float.class,1.0));
        parameters.add(new ParameterSpecification("E_g",float.class,0.0));
        parameters.add(new ParameterSpecification("Discount_g",float.class,1.0));
        parameters.add(new ParameterSpecification("E_0",float.class,0.4));
        parameters.add(new ParameterSpecification("Discount_0",float.class,1.0));
                
        parameters.add(new ParameterSpecification("DefaultPolicy",AI.class, playoutPolicy));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));

        parameters.add(new ParameterSpecification("ForceExplorationOfNonSampledActions",boolean.class,true));
        
        parameters.add(new ParameterSpecification("PH_Flag",boolean.class,false));
        parameters.add(new ParameterSpecification("TR_Flag",boolean.class,false));
        parameters.add(new ParameterSpecification("AWLM_Flag",boolean.class,false));
        
        return parameters;
    }
    
    public int getPlayoutLookahead() {
        return MAX_SIMULATION_TIME;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        MAX_SIMULATION_TIME = a_pola;
    }


    public int getMaxTreeDepth() {
        return MAX_TREE_DEPTH;
    }
    
    
    public void setMaxTreeDepth(int a_mtd) {
        MAX_TREE_DEPTH = a_mtd;
    }
    
    
    public float getE_l() {
        return epsilon_l;
    }
    
    
    public void setE_l(float a_e_l) {
        epsilon_l = a_e_l;
    }


    public float getDiscount_l() {
        return discount_l;
    }
    
    
    public void setDiscount_l(float a_discount_l) {
        discount_l = a_discount_l;
    }


    public float getE_g() {
        return epsilon_g;
    }
    
    
    public void setE_g(float a_e_g) {
        epsilon_g = a_e_g;
    }


    public float getDiscount_g() {
        return discount_g;
    }
    
    
    public void setDiscount_g(float a_discount_g) {
        discount_g = a_discount_g;
    }


    public float getE_0() {
        return epsilon_0;
    }
    
    
    public void setE_0(float a_e_0) {
        epsilon_0 = a_e_0;
    }


    public float getDiscount_0() {
        return discount_0;
    }
    
    
    public void setDiscount_0(float a_discount_0) {
        discount_0 = a_discount_0;
    }
    
    
    
    public AI getDefaultPolicy() {
        return playoutPolicy;
    }
    
    
    public void setDefaultPolicy(AI a_dp) {
        playoutPolicy = a_dp;
    }
    
    
    public EvaluationFunction getEvaluationFunction() {
        return ef;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        ef = a_ef;
    }
    
    public boolean getForceExplorationOfNonSampledActions() {
        return forceExplorationOfNonSampledActions;
    }
    
    public void setForceExplorationOfNonSampledActions(boolean fensa)
    {
        forceExplorationOfNonSampledActions = fensa;
    }

    public boolean getPH_Flag() {
        return PH_enabled;
    }
    
    public void setPH_Flag(boolean flag)
    {
        PH_enabled = flag;
        
        // Progressive history
        if (flag) {
            globalStrategy = FusionRTSNode.UCB1PH;
            PHStructures = new GlobalMaps_PH();
        } else {
            globalStrategy = FusionRTSNode.UCB1;
        }
    }  
    
    public boolean getTR_Flag() {
        return TR_enabled;
    }
    
    public void setTR_Flag(boolean flag) {
        TR_enabled = flag;
    }  
    
    public boolean getAWLM_Flag() {
        return AWLM_enabled;
    }
    
    public void setAWLM_Flag(boolean flag)
    {
        AWLM_enabled = flag;
        
        // AWLM heuristic
        if(flag) {
            ef = new AWLM_EvaluationFunction();
        }
    }  
}
