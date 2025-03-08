package io.github.daniel366cobra.vs_marine_propulsion.ship_control;

import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.config.VSMarinePropulsionConfig;
import net.minecraft.util.Mth;

public class PropellerThrustCurve {

    private final float maxThrust;
    private final float maxThrustRPM;
    private final float cavitationRPM;

    public static final PropellerThrustCurve largeShipPropellerThrustCurve = new PropellerThrustCurve(
            VSMarinePropulsionConfig.LARGE_SHIP_PROPELLER_MAX_THRUST.get().floatValue(),
            VSMarinePropulsionConfig.LARGE_SHIP_PROPELLER_MAX_THRUST_RPM.get().floatValue(),
            VSMarinePropulsionConfig.LARGE_SHIP_PROPELLER_CAVITATION_RPM.get().floatValue());


    /**
     * This function approximates a propeller's thrust curve in relation to its RPM. It is composed of 2 parabolas:
     * one ascending: X from 0 to maxThrustRPM and Y from 0 to maxThrust,
     * and another descending: X from maxThrustRPM to cavitationRPM and Y from maxThrust to 0.
     * The parabolas can differ, making an asymmetric resulting curve.
     *
     * @param maxThrust     thrust generated at maxThrustRPM, the most efficient operation.
     * @param maxThrustRPM  the most efficient RPM where maximum thrust is generated.
     * @param cavitationRPM RPM where the thrust drops to 0 due to extensive cavitation.
     */
    private PropellerThrustCurve(float maxThrust, float maxThrustRPM, float cavitationRPM) {
        boolean negativeValues = maxThrustRPM < 0 || cavitationRPM < 0 || maxThrust < 0;
        boolean wrongCurveShape = (maxThrustRPM / cavitationRPM < 0.6) || (maxThrustRPM / cavitationRPM > 0.9);
        if (negativeValues || wrongCurveShape)
            throw new RuntimeException(VSMarinePropulsionMod.NAME + ": Wrong parameters for propeller thrust curve. Check that values are positive" +
                    " and that max thrust RPM is between 0.6 and 0.9 x cavitation RPM.");
        this.maxThrust = maxThrust;
        this.maxThrustRPM = maxThrustRPM;
        this.cavitationRPM = cavitationRPM;
    }

    public float calculateThrust(float RPM) {

        float thrust = 0.0f;

        //coefficients for parabolas
        float a1, b1, c1, a2, b2, c2;

        //RPM < 0 means "Ahead", otherwise "Astern".
        boolean ahead = RPM < 0;

        //Calculate thrust at the positive branch of parabolas
        RPM = Math.abs(RPM);

        a1 = -(float) (maxThrust / Math.pow(maxThrustRPM, 2.0f));
        b1 = 2.0f * maxThrust / maxThrustRPM;
        c1 = 0.0f;
        a2 = -(float) (maxThrust / Math.pow(cavitationRPM - maxThrustRPM, 2.0f));
        b2 = -2.0f * a2 / maxThrustRPM;
        c2 = (float) (maxThrust + a2 * Math.pow(maxThrustRPM, 2.0f));

        thrust = (RPM <= maxThrustRPM) ?
                (float) (a1 * Math.pow(RPM, 2.0) + b1 * RPM + c1)
                : (float) (a2 * Math.pow(RPM, 2.0) + b2 * RPM + c2);

        if (!ahead) thrust = -0.5f * thrust;

        return thrust;
    }
}
