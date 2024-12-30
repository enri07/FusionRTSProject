/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fusionrts;

import ai.mcts.MCTSNode;
import ai.mcts.naivemcts.UnitActionTableEntry;
import java.math.BigInteger;
import java.util.*;
import rts.*;
import rts.units.Unit;
import util.Pair;
import util.Sampler;

/**
 *
 */
public class FusionRTSNode extends MCTSNode {
    
    public static final int E_GREEDY = 0;
    public static final int UCB1 = 1;
    public static final int UCB1PH = 2;
    
    static public int DEBUG = 0;
    
    public static float C = 0.05f;   // exploration constant for UCB1
    public static float W = 1f;   // constant for PH
    
    boolean forceExplorationOfNonSampledActions = true;
    public PlayerActionGenerator moveGenerator;
    HashMap<BigInteger,FusionRTSNode> childrenMap = new LinkedHashMap<>();    // associates action codes with children
    // Decomposition of the player actions in unit actions, and their contributions:
    public List<UnitActionTableEntry> unitActionTable;
    double evaluation_bound;    // this is the maximum positive value that the evaluation function can return
    public BigInteger[] multipliers;
    
    // New pointer to the global structures used to implement the Progressive History technique
    GlobalMaps_PH globalStructuresPH;
    // In order to backpropagate also on the global structures, we need to store
    // in each node the action that lead to it
    public List<Pair<Unit, UnitAction>> unitActionList;

    public FusionRTSNode(int maxPlayer, int minPlayer, GameState gameState, FusionRTSNode parentNode,
            double evaluationBound, int aCreationID, boolean fensa,
            GlobalMaps_PH PHStructures , List<Pair<Unit, UnitAction>> parentUnitActionList) throws Exception {
        parent = parentNode;
        gs = gameState;
        if (parent == null) depth = 0; else depth = parent.depth + 1;
        evaluation_bound = evaluationBound;
        creation_ID = aCreationID;
        forceExplorationOfNonSampledActions = fensa;
        
        // New structures for PH enhancement
        globalStructuresPH = PHStructures;
        unitActionList = parentUnitActionList;
        
        while (gs.winner() == -1 &&
               !gs.gameover() &&
               !gs.canExecuteAnyAction(maxPlayer) &&
               !gs.canExecuteAnyAction(minPlayer)) {
            gs.cycle();
        }
        if (gs.winner() != -1 || gs.gameover()) {
            type = -1;
        } else if (gs.canExecuteAnyAction(maxPlayer)) {
            type = 0;
            moveGenerator = new PlayerActionGenerator(gs, maxPlayer);
            actions = new ArrayList<>();
            children = new ArrayList<>();
            unitActionTable = new LinkedList<>();
            multipliers = new BigInteger[moveGenerator.getChoices().size()];
            BigInteger baseMultiplier = BigInteger.ONE;
            int idx = 0;
            for (Pair<Unit, List<UnitAction>> choice : moveGenerator.getChoices()) {
                UnitActionTableEntry ae = new UnitActionTableEntry();
                ae.u = choice.m_a;
                ae.nactions = choice.m_b.size();
                ae.actions = choice.m_b;
                ae.accum_evaluation = new double[ae.nactions];
                ae.visit_count = new int[ae.nactions];
                for (int i = 0; i < ae.nactions; i++) {
                    ae.accum_evaluation[i] = 0;
                    ae.visit_count[i] = 0;
                }
                unitActionTable.add(ae);
                multipliers[idx] = baseMultiplier;
                baseMultiplier = baseMultiplier.multiply(BigInteger.valueOf(ae.nactions));
                idx++;
             }
        } else if (gs.canExecuteAnyAction(minPlayer)) {
            type = 1;
            moveGenerator = new PlayerActionGenerator(gs, minPlayer);
            actions = new ArrayList<>();
            children = new ArrayList<>();
            unitActionTable = new LinkedList<>();
            multipliers = new BigInteger[moveGenerator.getChoices().size()];
            BigInteger baseMultiplier = BigInteger.ONE;
            int idx = 0;
            for (Pair<Unit, List<UnitAction>> choice : moveGenerator.getChoices()) {
                UnitActionTableEntry ae = new UnitActionTableEntry();
                ae.u = choice.m_a;
                ae.nactions = choice.m_b.size();
                ae.actions = choice.m_b;
                ae.accum_evaluation = new double[ae.nactions];
                ae.visit_count = new int[ae.nactions];
                for (int i = 0; i < ae.nactions; i++) {
                    ae.accum_evaluation[i] = 0;
                    ae.visit_count[i] = 0;
                }
                unitActionTable.add(ae);
                multipliers[idx] = baseMultiplier;
                baseMultiplier = baseMultiplier.multiply(BigInteger.valueOf(ae.nactions));
                idx++;
           }
        } else {
            type = -1;
            System.err.println("FusionRTSNode: This should not have happened...");
        }
    }


    // Naive Sampling:
    public FusionRTSNode selectLeaf(int maxPlayer, int minPlayer, float epsilon_l,
            float epsilon_g, float epsilon_0, int global_strategy, int max_depth,
            int aCreationID) throws Exception {
        if (unitActionTable == null) return this;
        if (depth>=max_depth) return this;

        // In the first iteration no child is initialized, thus we will go straight to else (LocalMABS).
        // In following iteration (without considering the second term) we have a path already constructed.
        if (!children.isEmpty() && r.nextFloat() >= epsilon_0) {
            // sample from the global MAB:
            FusionRTSNode selected = null;
            if (global_strategy==E_GREEDY) selected = selectFromAlreadySampledEpsilonGreedy(epsilon_g);
            else if (global_strategy==UCB1) selected = selectFromAlreadySampledUCB1(C);
            else if (global_strategy==UCB1PH) selected = selectFromAlreadySampledUCB1_withPH(C,W);
            // After having identified the best child, we continue to explore until we find a leaf.
            assert selected != null;
            return selected.selectLeaf(maxPlayer, minPlayer, epsilon_l, epsilon_g, epsilon_0, global_strategy, max_depth, aCreationID);
        } else {
            // sample from the local MABs (this might recursively call "selectLeaf" internally):
            return selectLeafUsingLocalMABs(maxPlayer, minPlayer, epsilon_l, epsilon_g, epsilon_0, global_strategy, max_depth, aCreationID);
        }
    }



    public FusionRTSNode selectFromAlreadySampledEpsilonGreedy(float epsilon_g) throws Exception {
        if (r.nextFloat()>=epsilon_g) {
            // Choose the best path!
            FusionRTSNode best = null;
            for(MCTSNode pate:children) {
                if (type==0) {
                    // max node:
                    if (best==null || (pate.accum_evaluation/pate.visit_count)>(best.accum_evaluation/best.visit_count)) {
                        best = (FusionRTSNode)pate;
                    }
                } else {
                    // min node:
                    if (best==null || (pate.accum_evaluation/pate.visit_count)<(best.accum_evaluation/best.visit_count)) {
                        best = (FusionRTSNode)pate;
                    }
                }
            }

            return best;
        } else {
            // choose one at random from the ones seen so far:
            return (FusionRTSNode)children.get(r.nextInt(children.size()));
        }
    }


    public FusionRTSNode selectFromAlreadySampledUCB1(float C) throws Exception {
        FusionRTSNode best = null;
        double bestScore = 0;
        for(MCTSNode pate:children) {
            double exploitation = (pate.accum_evaluation) / pate.visit_count;
            double exploration = Math.sqrt(Math.log(visit_count)/pate.visit_count);
            if (type==0) {
                // max node:
                exploitation = (evaluation_bound + exploitation)/(2*evaluation_bound);
            } else {
                exploitation = (evaluation_bound - exploitation)/(2*evaluation_bound);
            }

            double tmp = C*exploitation + exploration;
            if (best==null || tmp>bestScore) {
                best = (FusionRTSNode)pate;
                bestScore = tmp;
            }
        }

        return best;
    }

    public FusionRTSNode selectFromAlreadySampledUCB1_withPH(float C, float W) throws Exception {
        FusionRTSNode best = null;
        double bestScore = 0;
        for(MCTSNode pate : children) {
            double exploitation = (pate.accum_evaluation) / pate.visit_count;
            double exploration = Math.sqrt(Math.log(visit_count)/pate.visit_count);
            //            System.out.println(exploitation + " + " + exploration);

            // Retrieve child node in FusionRTS class
            FusionRTSNode fusion_node = (FusionRTSNode)pate;

            // New part given by PH enhancement
            // Retrieve sa/na
            double hist_bias = globalStructuresPH.getValueImpactAction(fusion_node.unitActionList);

            // type == 1 means that this is a node where we are exploring action
            // of the opponent player. Thus, we want to MINIMIZE the expectation
            // which is equivalent to study the best move of the opponent.
            if (type==0) {
                // max node:
               exploitation = (evaluation_bound + exploitation)/(2*evaluation_bound);
            } else {
               exploitation = (evaluation_bound - exploitation)/(2*evaluation_bound);
            }
            hist_bias = (evaluation_bound + hist_bias)/(2*evaluation_bound);

            // Evaluate progressive bias
            double prog_bias = W/(pate.visit_count - pate.accum_evaluation + 1 );

            double normal_term = C*exploitation + exploration;

            double prog_history = hist_bias*prog_bias;
            //System.out.println("exploitation = " + exploitation + "exploration = " + exploration +" statistics = " + first_term + " second_term = " + second_term);

            double tmp = normal_term + prog_history;

            if (best==null || tmp>bestScore) {
                best = (FusionRTSNode)pate;
                bestScore = tmp;
            }
        }

        return best;
    }

    public FusionRTSNode selectLeafUsingLocalMABs(int maxPlayer, int minPlayer, float epsilon_l, float epsilon_g, float epsilon_0, int global_strategy, int max_depth, int aCreationID) throws Exception {
        PlayerAction pa2;
        BigInteger actionCode;

        // For each unit, rank the unitActions according to preference:
        List<double []> distributions = new LinkedList<>();
        List<Integer> notSampledYet = new LinkedList<>();
        for(UnitActionTableEntry ate:unitActionTable) {
            // ate refers to a single specific unit, so ate.nactions is the number of possible moves the unit can make
            double []dist = new double[ate.nactions];
            int bestIdx = -1;
            double bestEvaluation = 0;
            int visits = 0;
            for(int i = 0;i<ate.nactions;i++) {
                if (type==0) { // We are sampling an action for our player
                    // max node:
                    if (bestIdx==-1 || // No preferred action sampled yet
                        (visits!=0 && ate.visit_count[i]==0) || // Never explored the particular action
                        (visits!=0 && (ate.accum_evaluation[i]/ate.visit_count[i])>bestEvaluation)) { // Explored the particular action and the evaluated is better
                        bestIdx = i;
                        if (ate.visit_count[i]>0) bestEvaluation = (ate.accum_evaluation[i]/ate.visit_count[i]);
                                             else bestEvaluation = 0;
                        visits = ate.visit_count[i];
                    }
                } else { // We are sampling an action for the other player
                    // min node:
                    if (bestIdx==-1 ||
                        (visits!=0 && ate.visit_count[i]==0) ||
                        (visits!=0 && (ate.accum_evaluation[i]/ate.visit_count[i])<bestEvaluation)) {
                        bestIdx = i;
                        if (ate.visit_count[i]>0) bestEvaluation = (ate.accum_evaluation[i]/ate.visit_count[i]);
                                             else bestEvaluation = 0;
                        visits = ate.visit_count[i];
                    }
                }
                dist[i] = epsilon_l/ate.nactions;
            }
            if (ate.visit_count[bestIdx]!=0) {
                dist[bestIdx] = (1-epsilon_l) + (epsilon_l/ate.nactions);
            } else {
                if (forceExplorationOfNonSampledActions) {
                    for(int j = 0;j<dist.length;j++)
                        if (ate.visit_count[j]>0) dist[j] = 0;
                }
            }

            if (DEBUG>=3) {
                System.out.print("[ ");
                for(int i = 0;i<ate.nactions;i++) System.out.print("(" + ate.visit_count[i] + "," + ate.accum_evaluation[i]/ate.visit_count[i] + ")");
                System.out.println("]");
                System.out.print("[ ");
                for (double v : dist) System.out.print(v + " ");
                System.out.println("]");
            }

            notSampledYet.add(distributions.size());
            distributions.add(dist);
            // in dist we have the preference for each unit and each action.
            // In distributions, we have the dist of all the units
        }

        // Select the best combination that results in a valid player action by epsilon-greedy sampling:
        ResourceUsage base_ru = new ResourceUsage();
        for(Unit u:gs.getUnits()) {
            UnitAction ua = gs.getUnitAction(u);
            if (ua!=null) {
                ResourceUsage ru = ua.resourceUsage(u, gs.getPhysicalGameState());
                base_ru.merge(ru);
            }
        }

        pa2 = new PlayerAction();
        actionCode = BigInteger.ZERO;
        pa2.setResourceUsage(base_ru.clone());

        // NEW: Prepare list of (unit,action) leading to the new child
        List<Pair<Unit, UnitAction>> child_unit_action_list = new ArrayList<>();
        while(!notSampledYet.isEmpty()) {
            int i = notSampledYet.remove(r.nextInt(notSampledYet.size()));

            try {
                UnitActionTableEntry ate = unitActionTable.get(i);
                int code;
                UnitAction ua;
                ResourceUsage r2;

                // try one at random:
                double []distribution = distributions.get(i);
                code = Sampler.weighted(distribution);
                ua = ate.actions.get(code);
                r2 = ua.resourceUsage(ate.u, gs.getPhysicalGameState());
                if (!pa2.getResourceUsage().consistentWith(r2, gs)) {
                    // sample at random, eliminating the ones that have not worked so far:
                    List<Double> dist_l = new ArrayList<>();
                    List<Integer> dist_outputs = new ArrayList<>();

                    for(int j = 0;j<distribution.length;j++) {
                        dist_l.add(distribution[j]);
                        dist_outputs.add(j);
                    }
                    do{
                        int idx = dist_outputs.indexOf(code);
                        dist_l.remove(idx);
                        dist_outputs.remove(idx);
                        code = (Integer)Sampler.weighted(dist_l, dist_outputs);
                        ua = ate.actions.get(code);
                        r2 = ua.resourceUsage(ate.u, gs.getPhysicalGameState());
                    }while(!pa2.getResourceUsage().consistentWith(r2, gs));
                }

                // DEBUG code:
                if (gs.getUnit(ate.u.getID())==null) throw new Error("Issuing an action to an inexisting unit!!!");


                pa2.getResourceUsage().merge(r2);
                pa2.addUnitAction(ate.u, ua);

                // NEW: Update also global maps with new action selected for PH enhancement
                if(globalStructuresPH != null){
                    child_unit_action_list.add(new Pair<>(ate.u, ua));
                }

                actionCode = actionCode.add(BigInteger.valueOf(code).multiply(multipliers[i]));

            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        // After we defined the action to perform, retrieve the child to which the action leads. In the first iteration is null (no child initialized)
        FusionRTSNode pate = childrenMap.get(actionCode);
        if (pate==null) {
            actions.add(pa2);
            GameState gs2 = gs.cloneIssue(pa2);
            // Generate new child with this as parent
            FusionRTSNode node = new FusionRTSNode(maxPlayer, minPlayer, gs2.clone(),
                    this, evaluation_bound, aCreationID, forceExplorationOfNonSampledActions,
                    globalStructuresPH , child_unit_action_list );
            childrenMap.put(actionCode,node);
            children.add(node);
            return node; // We have found a new child, so we can stop the selection in the tree and return this
        }

        return pate.selectLeaf(maxPlayer, minPlayer, epsilon_l, epsilon_g, epsilon_0, global_strategy, max_depth, aCreationID);
    }


    public UnitActionTableEntry getActionTableEntry(Unit u) {
        for(UnitActionTableEntry e:unitActionTable) {
            if (e.u == u) return e;
        }
        throw new Error("Could not find Action Table Entry!");
    }


    public void propagateEvaluation(double evaluation, FusionRTSNode child) {
        accum_evaluation += evaluation;
        visit_count++;

        // Also update the global structures if PH enhancement is enabled
        if(globalStructuresPH != null && unitActionList != null ){
            globalStructuresPH.update(unitActionList, evaluation);
        }

//        if (child!=null) System.out.println(evaluation);

        // update the unitAction table:
        if (child != null) {
            int idx = children.indexOf(child);
            PlayerAction pa = actions.get(idx);

            for (Pair<Unit, UnitAction> ua : pa.getActions()) {
                UnitActionTableEntry actionTable = getActionTableEntry(ua.m_a);
                idx = actionTable.actions.indexOf(ua.m_b);

                if (idx==-1) {
                    System.out.println("Looking for action: " + ua.m_b);
                    System.out.println("Available actions are: " + actionTable.actions);
                }

                actionTable.accum_evaluation[idx] += evaluation;
                actionTable.visit_count[idx]++;
            }
        }

        if (parent != null) {
            ((FusionRTSNode)parent).propagateEvaluation(evaluation, this);
        }
    }

    public void printUnitActionTable() {
        for (UnitActionTableEntry uat : unitActionTable) {
            System.out.println("Actions for unit " + uat.u);
            for (int i = 0; i < uat.nactions; i++) {
                System.out.println("   " + uat.actions.get(i) + " visited " + uat.visit_count[i] + " with average evaluation " + (uat.accum_evaluation[i] / uat.visit_count[i]));
            }
        }
    }    
}
