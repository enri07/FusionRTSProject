package fusionrts;

import ai.evaluation.EvaluationFunction;
import java.util.LinkedList;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.*;

/**
 *
 * 
 * Against Worker in Large Maps Evaluation Function. This evaluation function 
 * uses the same base evaluation as SimpleSqrtEvaluationFunction3. In addition,
 * it stores the minimum (critical) distance between the players bases. If this distance 
 * is greater than 9 (usually on a 12x12 map and larger) it applies a discouraging
 * factor on the development of new workers. This factor is proportional to the number of
 * workers already available. In this way, bases will store more resources instead of
 * continuously deploying workers and this will lead to the creation of barracks and
 * fighting units. Conversely, if this critical distance is lower than 9 then the
 * same evaluation of the standard SimpleSqrtEvaluationFunction3 is used.
 * 
 */
public class AWLM_EvaluationFunction extends EvaluationFunction {    
    public static float WEIGHT_RESOURCE = 20;
    public static float WEIGHT_RESOURCE_IN_WORKER = 10;
    public static float UNIT_BONUS_MULTIPLIER = 40.0f;

    public static String UNIT_TYPE_WORKER = "Worker";
    public static String UNIT_TYPE_BASE = "Base";
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }

    public float evaluate(int maxPlayer, int minPlayer, GameState gs) {
        float s1 = base_score(maxPlayer, gs);
        float s2 = base_score(minPlayer, gs);
        if (s1 + s2 == 0) return 0.5f;
        return  (2*s1 / (s1 + s2)) - 1;
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = gs.getPlayer(player).getResources() * WEIGHT_RESOURCE;
        boolean myPlayerHasUnit = false;
        float counterWorker = 0;
        List<Unit> myBaseList = new LinkedList<>();
        List<Unit> opponentBaseList = new LinkedList<>();
        double critDistance = 40;
        
        for(Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player) {
                myPlayerHasUnit = true;
                if ((UNIT_TYPE_WORKER.equals(u.getType().name) && critDistance > 9)) {
                    counterWorker++;
                    score += u.getResources() * WEIGHT_RESOURCE_IN_WORKER / counterWorker;
                    score += (UNIT_BONUS_MULTIPLIER * (float) u.getCost() * (float) Math.sqrt((float) u.getHitPoints() /u.getMaxHitPoints()) / counterWorker);
                } else {
                    if (UNIT_TYPE_BASE.equals(u.getType().name)) {
                        myBaseList.add(u);
                        for(Unit opponent_base : opponentBaseList) {
                            double xDiff = Math.abs(opponent_base.getX() - u.getX());
                            double yDiff = Math.abs(opponent_base.getY() - u.getY());
                            double newDist = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
                            if(newDist < critDistance) {
                                critDistance = newDist;
                            }
                        }
                    }
                    score += u.getResources() * WEIGHT_RESOURCE_IN_WORKER;
                    score += (UNIT_BONUS_MULTIPLIER * (float) u.getCost() * (float) Math.sqrt((float) u.getHitPoints() / u.getMaxHitPoints()));
                }
            } else {
                if(UNIT_TYPE_BASE.equals(u.getType().name)) {
                    opponentBaseList.add(u);
                    for(Unit myBase : myBaseList) {
                        double xDiff = Math.abs(myBase.getX() - u.getX());
                        double yDiff = Math.abs(myBase.getY() - u.getY());
                        double newDist = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
                        if(newDist < critDistance) {
                            critDistance = newDist;
                        }   
                    }
                }
            }
        }
        //score -= 100/critDistance;
        
        if (!myPlayerHasUnit) return 0;
        return score;
    }    

}
