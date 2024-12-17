package fusionrts;

import java.util.List;
import rts.UnitAction;
import rts.units.Unit;

/**
 *
 * @author Enrico Calandrini
 * 
 * This class extends the UnitActionTableEntry allowing to have resizable 
 * arrays to consider new actions.
 */
public class ExtendedUnitActionTableEntry {
    public Unit u;
    public int nactions = 0;
    public List<UnitAction> actions;
    public List<Double> accum_evaluation;
    public List<Integer> visit_count;
}
