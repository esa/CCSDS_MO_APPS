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
package instrumentssimulator;

import java.time.LocalDateTime;


/**
 *
 * @author Cesar Coelho
 */
public class InstrumentsSimulator {

    private Orbit darkDusk;
    private GPS gps;
    private FineADCS fineADCS;
    
    /**
     * Constructor for the Instruments Simulator Block
     */
    public InstrumentsSimulator() {
        // TODO code application logic here
        
        // Values from the OPS-SAT document: a = 6371+650= 7021 km ; i = 98.05 deg  (orbital period: 1.63 hours)
        // (double a, double i, double RAAN, double arg_per, double true_anomaly)
        darkDusk = new Orbit ( 7021, 98.05*(Math.PI/180), (340)*(Math.PI/180), (0)*(Math.PI/180), 0);
        
        gps = new GPS(darkDusk);
        fineADCS = new FineADCS(darkDusk);
           
    }

    public void printRealPosition() {
        Orbit.OrbitParameters param = darkDusk.getParameters();
        System.out.printf("\n\nLatitude, Longitude: %f, %f\nAltitude: %f\nTime: %s\n\n\n", 
                param.getlatitude(), param.getlongitude(), param.geta(), param.gettime().toString());
    }

    public double getGPSlatitude(){
        return gps.getPosition().getlatitude();
    }

    public double getGPSaltitude(){
        return GPS.truncateDecimal(gps.getPosition().getGPSaltitude(), 1).doubleValue();
    }
    
    public double getGPSlongitude(){
        return gps.getPosition().getlongitude();
    }

    public LocalDateTime getGPStime(){
        return gps.getPosition().gettime();
    }
    
    public double getFineADCSmagnetometerBr(){
        return fineADCS.magnetometer.getB_r();
    }

    public double getFineADCSmagnetometerBtheta(){
        return fineADCS.magnetometer.getB_theta();
    }
    
    
}
