package fusionrts;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;

public class FusionRTSWithTR extends FusionRTS {

    public FusionRTSWithTR(UnitTypeTable utt) {
        super(100, -1, 100, 10, 0.3f, 0.0f, 0.4f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true, false, true, false);
        this.TR_enabled = true;
    }

    public FusionRTSWithTR(int available_time, int max_playouts, int lookahead, int max_depth,
                           float e_l, float discout_l, float e_g, float discout_g,
                           float e_0, float discout_0, AI policy, EvaluationFunction a_ef,
                           boolean fensa, boolean PH_flag, boolean AWLM_flag) {
        super(available_time, max_playouts, lookahead, max_depth, e_l, discout_l, e_g, discout_g, e_0, discout_0, policy, a_ef, fensa, PH_flag, true, AWLM_flag);
        this.TR_enabled = true;
    }
}