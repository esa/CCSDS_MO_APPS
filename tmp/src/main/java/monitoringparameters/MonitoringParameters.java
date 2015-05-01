/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO MC Mity Demo Application
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package monitoringparameters;

import instrumentssimulator.InstrumentsSimulator;
import java.time.ZoneOffset;

/**
 *
 * @author Cesar Coelho
 */
public class MonitoringParameters{

    private InstrumentsSimulator instrumentSimulator;
    
    public MonitoringParameters() {
        instrumentSimulator = new InstrumentsSimulator();
    }
    
    /**
     *
     * @param name
     * @return
     */
    public double acquire(String name) {

        if (name == null)
            return 0;
        
        switch (name) {
            case "GPS.Latitude":                    return instrumentSimulator.getGPSlatitude();
            case "GPS.Longitude":                   return instrumentSimulator.getGPSlongitude();
            case "GPS.Altitude":                    return instrumentSimulator.getGPSaltitude();
            case "GPS.Time":                        return instrumentSimulator.getGPStime().toEpochSecond(ZoneOffset.UTC);
            case "FineADCS.Magnetometer.B_r":       return instrumentSimulator.getFineADCSmagnetometerBr();
            case "FineADCS.Magnetometer.B_theta":   return instrumentSimulator.getFineADCSmagnetometerBtheta();
            case "Zero":                            return 0;
            case "One":                             return 1;
            case "Two":                             return 2;
            case "Three":                           return 3;
            case "Four":                            return 4;

            default:                                return 0;  // Parameter not found
        }
    }
    
}
