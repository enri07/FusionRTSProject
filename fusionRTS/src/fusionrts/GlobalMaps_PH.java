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

    // Link for each (unit type, x and y coordinates and player)  the corresponding 
    // entry in Global_unitActionTable
    HashMap<NoIDKey, ExtendedUnitActionTableEntry> typeXYMap;
    //player-typeUnit-action-direction -> num
    HashMap<String, Integer> statisticsVisitActions;
    // this is a test for understand if there should be any improvements with this feature
    boolean IS_ENABLE_RESET_VISIT_COUNT = false;
    int RESET_VISIT_COUNT_UPPER_BOUND = 5000;
    // Initialize structures based on specific player and state
    
    public GlobalMaps_PH() {
        typeXYMap = new LinkedHashMap<>();
        statisticsVisitActions = new LinkedHashMap<>();
    }
    
    public void update(List<Pair<Unit, UnitAction>> unitActionList, double evaluation) {
        for(Pair<Unit, UnitAction> pair : unitActionList) {
            
            NoIDKey newKey = new NoIDKey(pair.m_a);
            String statisticKey = getStatisticKey(pair);
            if(statisticsVisitActions.containsKey(statisticKey)) {
                statisticsVisitActions.put(statisticKey, statisticsVisitActions.get(statisticKey) + 1);
            } else {
                statisticsVisitActions.put(statisticKey, 1);
            }

            if(typeXYMap.containsKey(newKey)) {
                // Retrieve index of the unit in the list and actual entry
                ExtendedUnitActionTableEntry ae = typeXYMap.get(newKey);

                // Retrieve index of particular action
                int indexOfAction = ae.actions.indexOf(pair.m_b);

                if (indexOfAction != -1) { // if the action is present in the list
                    // Update counter and evaluation of the action
                    List<Integer> visitCountList = ae.visitCountList;
                    List<Double> acc_evaluation = ae.accumEvaluationList;

                    // reset counter if the game went too far
                    if (visitCountList.get(indexOfAction) > RESET_VISIT_COUNT_UPPER_BOUND && IS_ENABLE_RESET_VISIT_COUNT) {
                        visitCountList.set(indexOfAction, 1);
                        acc_evaluation.set(indexOfAction, evaluation);
                    } else {
                            visitCountList.set(indexOfAction, visitCountList.get(indexOfAction) + 1);
                            acc_evaluation.set(indexOfAction, acc_evaluation.get(indexOfAction) + evaluation);
                    }
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

                typeXYMap.put(newKey, ae);
            }
        }
    }

    private static String getStatisticKey(Pair<Unit, UnitAction> pair) {
        String statisticKey = pair.m_a.getPlayer() + "-" + pair.m_a.getType().name + "-" + pair.m_b.getActionName();
        if(pair.m_b.getDirection() != UnitAction.DIRECTION_NONE) {
            if (pair.m_b.getDirection() == UnitAction.DIRECTION_UP) {
                statisticKey += "-up";
            }
            if (pair.m_b.getDirection() == UnitAction.DIRECTION_RIGHT) {
                statisticKey += "-right";
            }
            if (pair.m_b.getDirection() == UnitAction.DIRECTION_DOWN) {
                statisticKey += "-down";
            }
            if (pair.m_b.getDirection() == UnitAction.DIRECTION_LEFT) {
                statisticKey += "-left";
            }
        }
        return statisticKey;
    }

    public double getValueImpactAction(List<Pair<Unit, UnitAction>> unit_action_list) {
        // Function used ot evaluate sa/na
        double meanEvaluation = 0;
        double meanVisits = 0;
        
        for(Pair<Unit, UnitAction> pair : unit_action_list) {
            NoIDKey newKey = new NoIDKey(pair.m_a); 

            if(typeXYMap.containsKey(newKey)) {
                // Retrieve index of the unit in the list and actual entry
                ExtendedUnitActionTableEntry ae = typeXYMap.get(newKey);

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

        return meanEvaluation/meanVisits;
    }
}
