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

import instrumentssimulator.Orbit.OrbitParameters;
import java.math.BigDecimal;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Cesar Coelho
 */
public class GPS {
    private Orbit orbit;
    private Orbit.OrbitParameters Position;

    // GPS characteristics
    int SampleFrequency = 1*1000; //milliseconds (1 update per second)
    
    // Errors
    Orbit.OrbitParameters numericalError;
    Orbit.OrbitParameters positionError;
    
    // Timer
    Timer timer;
    
    public GPS(Orbit selectedOrbit) {
        this.orbit = selectedOrbit;
        this.timer = new Timer();

        Position = orbit.getParameters();
        
        // Generate a (10 meters) PositionError
        positionError = GPS.this.generateError(10, Position);

        // Schedule the GPS updates to a constant frequency
        timer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run() {
            // Get the parameters from the orbit
            Position = orbit.getParameters();
            
            // Generate a (2 meters) numericalError every: this.SampleFrequency
            numericalError = GPS.this.generateError(2, Position);
            
//            OrbitParameters test =  getPosition();
//            System.out.printf("\nLatitude, Longitude: %f, %f\nAltitude: %f\nTime: %s\n", test.getlatitude(), test.getlongitude(), test.getaltitude(), test.gettime().toString());
                       
          }
            }, 0, this.SampleFrequency);
        
    }

    public OrbitParameters getPosition(){

        // The next line shouldn't be here, because if I request the Position from the GPS faster than
        // the Samplefrequency of the GPS, then I shall get the same value
        //this.Position = this.orbit.getParameters();
        
        double latitude = truncateDecimal (this.Position.getlatitude() + this.positionError.getlatitude() + this.numericalError.getlatitude(), 6).doubleValue();
        double longitude = truncateDecimal (this.Position.getlongitude() + this.positionError.getlongitude() + this.numericalError.getlongitude(), 6).doubleValue();
        
        latitude = fixBoundaries(latitude, -90, 90);
        longitude = fixBoundaries(longitude, -180, 180);
        
        // No errors for the velocity vector were included
        OrbitParameters PositionWithErrors = new OrbitParameters(
        latitude ,
        longitude ,
        truncateDecimal (this.Position.geta() + this.positionError.geta() + this.numericalError.geta(), 1).doubleValue() ,
        new Vector(this.Position.getvelocity().x(), this.Position.getvelocity().y(), this.Position.getvelocity().z()),
        this.Position.gettime());

        return PositionWithErrors;
    }

    public static BigDecimal truncateDecimal(double val, int nDecimalPlaces){
        if ( val > 0) {
            return new BigDecimal(String.valueOf(val)).setScale(nDecimalPlaces, BigDecimal.ROUND_FLOOR);
        } else {
            return new BigDecimal(String.valueOf(val)).setScale(nDecimalPlaces, BigDecimal.ROUND_CEILING);
        }
    }
    
    private double fixBoundaries(double input, double low_limit, double top_limit){
        if (input < low_limit)
            return low_limit;

        if (input > top_limit)
            return top_limit;
            
        return input;  // nothing to be fixed
    }

    // k is the constant and it's the error in meters
    private OrbitParameters generateError(double k, OrbitParameters param){
      // Generate errors
//    System.out.printf("Time: %s\n", RealPosition.time.toString());
      Random randomno = new Random();
      
      // Factor to convert the k from meters to degrees
      double factor = 360/(2*Math.PI*param.geta());
      
      // The values are divided by 3 to represent a 3 sigma confidence interval
        OrbitParameters error = new OrbitParameters(
            factor*k/3*randomno.nextGaussian(),
            factor*k/3*randomno.nextGaussian(),
            k/3*randomno.nextGaussian(),
        new Vector(0, 0, 0),
        this.Position.gettime());

      return error;      
    }
}
