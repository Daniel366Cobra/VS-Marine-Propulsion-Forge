package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility;

public class PropellerThrustCalculator {

    private final float propBladeCount;
    private final float propDiskArea;
    private final float propEffectiveArea;
    private final float propDiameter;
    private final float propPitch;

    private static final float ATMO_PRESSURE = 100000.0f;
    private static final float VAPOR_PRESSURE = 2500.0f;
    private static final float WATER_DENSITY = 1000.0f;
    private static final float g = 9.8f;


    public PropellerThrustCalculator(float bladeArea, int bladeCount, float propDiameter, float bladeAngle) {
        this.propBladeCount = bladeCount;
        this.propDiameter = propDiameter;
        this.propEffectiveArea = bladeArea * bladeCount;
        this.propDiskArea = (float)Math.PI * propDiameter / 4.0f;
        this.propPitch = (float) Math.PI * propDiameter * (float) Math.tan(Math.toRadians(bladeAngle));
    }

    /**
     * Static thrust estimation assuming no slip and no cavitation.
     *
     * @param RPM Propeller RPM.
     * @return Thrust in newtons.
     */
    public float thrust(float RPM) {
        //Assuming no slip, in 1 second the prop moves the fluid the distance equal to its geometric pitch times RPS (rotations per second).
        float RPS = RPM / 60.0f;
        float fluidVel = RPS * propPitch;

        //In 1 second, the moved volume is equal to the effective area of the prop times the velocity.
        //For mass, multiply by fluid density.
        float fluidMassFlowRate = WATER_DENSITY * propEffectiveArea * fluidVel;

        //Acceleration is equal to velocity times RPS (rotations per second).
        float fluidAccel = fluidVel * RPS;

        //Finally, force is equal to mass flow rate times acceleration.
        return fluidMassFlowRate * fluidAccel;
    }

    public float getPropEffectiveArea() {
        return propEffectiveArea;
    }

    /**
     * Calculates the cavitation number (Ca or sigma).
     *
     * @param RPM Propeller RPM.
     * @param shipSpeed Speed of the ship in m/s, used here as freestream speed.
     * @param depth Propeller shaft depth in m.
     * @return sigma.
     */
    public float caNumber(float RPM, float shipSpeed, float depth) {
        //Local pressure at prop shaft is proportional to the depth.
        float localPressure = ATMO_PRESSURE + WATER_DENSITY * g * depth;

        //Angular velocity is RPS times 2 Pi.
        float RPS = RPM / 60.0f;
        float angularVel = RPS * 2.0f * (float) Math.PI;

        //Linear velocity at disk edge is equal to angular velocity times disk radius.
        float bladeTipVel = angularVel * propDiameter / 2.0f;

        //Finally, return the cavitation number.
        return (localPressure - VAPOR_PRESSURE) / (0.5f * WATER_DENSITY * (shipSpeed * shipSpeed + bladeTipVel * bladeTipVel));
    }

    public float earKeller(float thrust, float depth) {
        //Local pressure at prop shaft is proportional to the depth.
        float depthVaporPressure = VAPOR_PRESSURE + WATER_DENSITY * g * depth;
        return ((1.3f + 0.3f * propBladeCount) * thrust) / ((ATMO_PRESSURE - depthVaporPressure) * propDiameter * propDiameter) + 0.2f;
    }

    /**
     * Burrill's estimation of critical cavitation number.
     * If given Ca is over the critical one, cavitation is unlikely.
     *
     * @param RPM Propeller RPM.
     * @param thrust Propeller thrust in newtons.
     * @return Burrill's estimation for the critical cavitation number.
     */
    public float caNumberCritBurrill(float RPM, float thrust) {
        //Angular velocity is RPS times 2 Pi.
        float RPS = RPM / 60.0f;
        float angularVel = RPS * 2.0f * (float) Math.PI;

        //Linear velocity at disk edge is equal to angular velocity times disk radius.
        float bladeTipVel = angularVel * 0.5f * propDiameter;

        //Thrust coefficient calculation
        float thrustCoeff = thrust / (0.5f * WATER_DENSITY * propDiskArea * bladeTipVel * bladeTipVel);
        //From real world tests
        float k = 0.25f;

        return thrustCoeff / k;
    }

    public float caNumberCritKeller() {
        return 0.2f + 0.4f * (propPitch / propDiameter);
    }
}
