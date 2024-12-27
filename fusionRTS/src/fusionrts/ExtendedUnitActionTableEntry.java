package fusionrts;

import java.util.List;

import ai.mcts.naivemcts.UnitActionTableEntry;
import rts.UnitAction;
import rts.units.Unit;

/**
 *
 * This class extends the @UnitActionTableEntry allowing to have resizable
 * arrays to consider new actions.
 */
public class ExtendedUnitActionTableEntry extends UnitActionTableEntry {
    public List<Double> accumEvaluationList;
    public List<Integer> visitCountList;
}
