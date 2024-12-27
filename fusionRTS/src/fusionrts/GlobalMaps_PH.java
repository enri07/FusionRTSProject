/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import rts.UnitAction;
import rts.units.Unit;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 
 * Main class implementing the progressive history enhancement
 *
 */
public class GlobalMaps_PH {
    
    // New pointer to the global structures used to implement the Progressive History technique
    List<ExtendedUnitActionTableEntry> unitActionTable;
    // Link for each (unit type, x and y coordinates and player)  the corresponding 
    // entry in Global_unitActionTable
    HashMap<NoIDKey, Integer> typeXYMap;
    // Store also the maximum index of the list
    int last_idx;
    
    // Initialize structures based on specific player and state
    /*public GlobalMaps_PH(int a_player, GameState gs ) throws Exception {
        PlayerActionGenerator MoveGenerator = new PlayerActionGenerator(gs, a_player);
        
        int idx = 0;
        for (Pair<Unit, List<UnitAction>> choice : MoveGenerator.getChoices()) {
            // m_a represents the actual unit selected
            // m_b stores all the possible actions to be performed by m_a
            ExtendedUnitActionTableEntry ae = new ExtendedUnitActionTableEntry();
            ae.u = choice.m_a; 
            ae.nactions = choice.m_b.size(); 
            ae.actions = new ArrayList<>(choice.m_b);
            ae.accumEvaluationList = new ArrayList<>();
            ae.visitCountList = new ArrayList<>();
            for (int i = 0; i < ae.nactions; i++) {
                ae.accumEvaluationList.add(0.0);
                ae.visitCountList.add(0);
            }
            unitActionTable.add(ae);
            // Generate new key
            NoIDKey newKey = new NoIDKey(ae.u);
            
            // Add a new entry in the hash map
            typeXYMap.put(newKey,idx);
            idx++;
            }
        last_idx = idx;
    }*/
    
    public GlobalMaps_PH() {
        // Nothing to do
        unitActionTable = new ArrayList<>();
        typeXYMap = new LinkedHashMap<>();
    }
    
    public void update(List<Pair<Unit, UnitAction>> unitActionList, double evaluation) {
        for(Pair<Unit, UnitAction> pair : unitActionList) {
            
            NoIDKey newKey = new NoIDKey(pair.m_a); 

            if(typeXYMap.containsKey(newKey)) {
                // Retrieve index of the unit in the list and actual entry
                ExtendedUnitActionTableEntry ae = unitActionTable.get(typeXYMap.get(newKey));

                // Retrieve index of particular action
                int indexOfAction = ae.actions.indexOf(pair.m_b);

                if (indexOfAction != -1) { // if the action is present in the list
                    // Update counter and evaluation of the action
                    List<Integer> visitCountList = ae.visitCountList;
                    List<Double> acc_evaluation = ae.accumEvaluationList;
                    
                    // reset counter if the game went too far
                    //    if (visitCountList.get(indexOfAction) > 5000 ){
                    //        visitCountList.set(indexOfAction, 1);
                    //        acc_evaluation.set(indexOfAction, evaluation);
                    //    }
                    //    else{
                            visitCountList.set(indexOfAction, visitCountList.get(indexOfAction) + 1);
                            acc_evaluation.set(indexOfAction, acc_evaluation.get(indexOfAction) + evaluation);
                    //    }
                } else { // if the action is not present
                    // We are adding a new possible move for a particular unit
                    ae.actions.add(pair.m_b);
                    ae.nactions++;
                    ae.accumEvaluationList.add(evaluation);
                    ae.visitCountList.add(1);
                } 
            } else {
                // We are adding a new entry to the list
                ExtendedUnitActionTableEntry ae = new ExtendedUnitActionTableEntry();
                ae.u = pair.m_a;
                ae.nactions = 1; // At the moment we are simply keeping track of 1 action
                ae.actions = new ArrayList<>();
                ae.actions.add(pair.m_b);
                ae.accumEvaluationList = new ArrayList<>();
                ae.accumEvaluationList.add(evaluation);
                ae.visitCountList = new ArrayList<>();
                ae.visitCountList.add(1);

                unitActionTable.add(ae);
                typeXYMap.put(newKey, last_idx);

                // Update index counter
                last_idx++;
            }
        }
    }
    
    public double get_statistic(List<Pair<Unit, UnitAction>> unit_action_list) {
        // Function used ot evaluate sa/na
        double meanEvaluation = 0;
        double meanVisits = 0;
        
        for(Pair<Unit, UnitAction> pair : unit_action_list) {
            NoIDKey newKey = new NoIDKey(pair.m_a); 

            if(typeXYMap.containsKey(newKey)) {
                // Retrieve index of the unit in the list and actual entry
                ExtendedUnitActionTableEntry ae = unitActionTable.get(typeXYMap.get(newKey));

                // Retrieve index of particular action
                int indexOfAction = ae.actions.indexOf(pair.m_b);

                if (indexOfAction != -1) {
                    // Update counter
                    meanVisits += ae.visitCountList.get(indexOfAction);
                    meanEvaluation += ae.accumEvaluationList.get(indexOfAction);
                } else {
                    // This should never happen, it means we choose a path with an
                    // action that has not been inserted in global list
                    System.err.println("GlobalMaps_PH: This should not have happened...");
                } 
            } else {
                // This should never happen, it means we choose a path with a
                // unit that has not been inserted in global list
                System.err.println("GlobalMaps_PH: This should not have happened...");
            }
        }
        // double statistic = meanEvaluation/meanVisits;
        // System.out.println("Evaluation = " + meanEvaluation + "Visits = " + meanVisits);
        
        return meanEvaluation/meanVisits;
    }
}
