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

package esa.mo.mal.demo.provider;

import esa.mo.mal.demo.util.StructureHelper;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import monitoringparameters.MonitoringParameters;
import org.ccsds.moims.mo.com.archive.structures.ExpressionOperator;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterConversion;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValueList;
import org.ccsds.moims.mo.mc.structures.ConditionalReference;
import org.ccsds.moims.mo.mc.structures.ParameterExpression;

/**
 *
 * @author Cesar Coelho
 */
public class ParameterManager implements Serializable{
    
    private LongList objInst; // Object instance identifiers
    private ParameterDefinitionDetailsList PDeflist; // Parameter Definitions list
//    private ParameterValueList PVallist; // Parameter Values list
    private Long counter; // Counter (different for every Definition)
    private Long uniquePValObjId;
    private final int savePValObjIdLimit = 20;  // Used to store the uniquePValObjId only once every "savePValObjIdLimit" times
    transient private final String filename = "db_ParameterDefinitions.data";
    transient private MonitoringParameters monitoringParameters;   // transient: marks members that won't be serialized.

    public ParameterManager(){
        this.objInst = new LongList(); // start a list of obj inst identifiers
        this.PDeflist = new ParameterDefinitionDetailsList(); // start a list of ParameterDefinitions
//        this.PVallist = new ParameterValueList(); // start a list of ParameterDefinitions
        this.counter = new Long(0); // The zeroth value will not be used (reserved for the wildcard)
        this.uniquePValObjId = new Long(0); // The zeroth value will not be used (reserved for the wildcard)
        this.monitoringParameters = new MonitoringParameters(); // start the monitoring list
        this.load(); // Load the file
    }
    
    private boolean save(){  // returns true if the file was successfully saved
        try {
            serializeDataOut(this);
            return true;
        } catch (IOException e){
            return false;
//            throw new IllegalArgumentException("The file: " + filename + ", could not be stored!" + e);
        }
    }

    public Long generatePValobjId(){ 
        uniquePValObjId++;
        if (uniquePValObjId % savePValObjIdLimit  == 0) // It is used to avoid constant saving every time we generate a new obj Inst identifier.
            this.save();
        return this.uniquePValObjId;
    }

    private boolean load() {  
        try {
            ParameterManager loadedFile = (ParameterManager) serializeDataIn();
            this.counter = loadedFile.counter;
            this.uniquePValObjId = loadedFile.uniquePValObjId + savePValObjIdLimit; // Guarantees the obj inst id stays unique
            this.PDeflist.addAll(loadedFile.PDeflist);
            this.objInst.addAll(loadedFile.objInst);
//            this.PVallist.addAll(loadedFile.PVallist);
            this.save();
            return true;        // returns true if the file was successfully loaded
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException | ClassNotFoundException ex) {
            return false;
        }
    }
      
    public boolean exists(Long input){
        return objInst.indexOf(input) != -1;
    }

    public ParameterDefinitionDetails get(Long input){
        int index = objInst.indexOf(input);
        if (index == -1) return null;
        return PDeflist.get(index);
    }
   
    public ParameterValueList getParameterValues(LongList input){
        ParameterValueList pValList = new ParameterValueList();
        for (Long input1 : input) 
            pValList.add(getParameterValue(input1));
        
        return pValList;
    }

    protected ParameterValue getParameterValue(Long objId) {
        if (!this.exists(objId)) {
            ParameterValue pVal = new ParameterValue();
            pVal.setValid(false);
            pVal.setInvalidSubState(new UOctet((short) 1)); // 1: EXPIRED
            pVal.setConvertedValue(null);
            pVal.setRawValue(null);
            return pVal;
        }

        ParameterDefinitionDetails pDef = this.get(objId);
        Attribute rawValue = new Union(this.monitoringParameters.acquire(pDef.getName().getValue()));
        Attribute convertedValue = null; // there's no conversion service

        UOctet invalidSubState = generateInvalidSubState(pDef, rawValue, convertedValue);

//        Validity validity = generateValidityState(pDef, (convertedValue != null) );
/*
         if (validity == Validity.UNVERIFIED ||
         validity == Validity.INVALID ||
         validity == Validity.VALID_RAW_ONLY ){
         convertedValue = null;  // requirement: 3.3.2.24
         }
         */
        if (invalidSubState.equals(new UOctet((short) 4))
                || invalidSubState.equals(new UOctet((short) 5))
                || invalidSubState.equals(new UOctet((short) 2))) {
            convertedValue = null;  // requirement: 3.3.2.24
        }

        Attribute unionConvertedValue = (convertedValue == null) ? null : convertedValue;  // Union doesn't directly accept null values

//        UOctet invalidSubState = new UOctet((short) validity.getNumericValue().getValue());

        /*
         public ParameterValue(Boolean isValid, 
         org.ccsds.moims.mo.mal.structures.UOctet invalidSubState, 
         org.ccsds.moims.mo.mal.structures.Attribute rawValue, 
         org.ccsds.moims.mo.mal.structures.Attribute convertedValue)
         */
        // Generate final Parameter Value
        return new ParameterValue((invalidSubState.getValue() == 0),
                invalidSubState, rawValue, unionConvertedValue);
    }

    protected UOctet generateInvalidSubState(ParameterDefinitionDetails definition, Attribute value, Attribute convertedValue) {
        if (definition == null) // Does not exist?
        {
            return new UOctet((short) 4); // UNVERIFIED
        }
        // Figure 3-2: Flow Chart for Determining the Validity of a Parameter (page 38)
        final ParameterExpression validityExpression = definition.getValidityExpression();

        if (validityExpression == null) // requirement: 3.3.2.19
        {
            return new UOctet((short) 0); // VALID
        }
        // The code continues... (requirement: 3.3.2.18)

        ParameterDefinitionDetails paramValidityDefinition = this.get(definition.getValidityExpression().getParameterId().getInstId());
        if (paramValidityDefinition == null) // The validity parameter does not exist?
        {
            return new UOctet((short) 4); // UNVERIFIED
        }

        if (validityExpression.getUseConverted()) // Is the validity checking for the converted or for the raw value?
        {
            value = convertedValue;
        }

        Attribute parameterIdValue = value;
//        Boolean eval = StructureHelper.evaluateExpression(parameterIdValue, validityExpression.getOperator().toString(), validityExpression.getValue());
        Boolean eval = StructureHelper.evaluateExpression(parameterIdValue, validityExpression.getOperator(), validityExpression.getValue());

        if (eval == null) // The expression was not evaluated?
        {
            return new UOctet((short) 4); // UNVERIFIED
        }//            return Validity.UNVERIFIED; // requirement: 3.3.2.20

        if (!eval) // Is the validity expression false?
        {
            return new UOctet((short) 5); // INVALID
        }//            return Validity.INVALID;  // requirement: 3.3.2.21

        ParameterConversion conversion = paramValidityDefinition.getConversion();

        if (conversion == null) // There's no conversion to apply?
        {
            return new UOctet((short) 0); // VALID
        }//            return Validity.VALID;  // requirement: 3.3.2.22

        // The conversion failed?
        if (convertedValue == null) {
            return new UOctet((short) 2); // INVALID_RAW
        }//            return Validity.VALID_RAW_ONLY;  // requirement: 3.3.2.23

        return new UOctet((short) 0); // VALID
//        return Validity.VALID;

    }

    private Double generateConvertedValue(Double rawValue, ParameterConversion conversion){
        // Not implemented yet...
        if (conversion == null)  // No conversion?
            return null;
        
        Double finalValue = rawValue.doubleValue();
        
        for (int i = 0; i < conversion.getConversionConditions().size(); i++){
            // Cycle through all the conditions and keep applying them;
            finalValue = applyConversion(finalValue, conversion.getConversionConditions().get(i));
        }
        
        return null;
    }
    
    private Double applyConversion(Double value, ConditionalReference condition){
        // The conditions are not implemented...
        
        return new Double(value.doubleValue() * 1);
        
    }

    public ParameterDefinitionDetailsList getAll(){
        return this.PDeflist;
    }

    public Long list(Identifier input){
        for (int i = 0; i < PDeflist.size(); i++) {
            if (PDeflist.get(i).getName().equals(input))
                return objInst.get(i);
	}
        return null; // Not found!
    }

    public LongList listAll(){ // requirement: 3.3.2.8
        return this.objInst;
    }

    public Long add(ParameterDefinitionDetails definition){ // requirement: 3.3.2.5
        definition.setGenerationEnabled(false);  // requirement: 3.3.2.7
        PDeflist.add(definition);
        counter++; // This line as to go before any writing (because it's initialized as zero and that's the wildcard)
        objInst.add(counter);
//        PVallist.add(getParameterValue(counter));  // get the first reading from the instrument and store
        this.save();
        return counter;
    }
      
    public boolean update(Long inst, ParameterDefinitionDetails definition){ // requirement: 3.3.2.5
        int index = objInst.indexOf(inst);
        if (index == -1) return false;
        PDeflist.set(index, definition);
        this.save();
        return true;
    }

    public boolean delete(Long inst){ // requirement: 3.3.2.5
        int index = objInst.indexOf(inst);
        if (index == -1) return false;
        PDeflist.remove(index);
        objInst.remove(index);
        this.save();
        return true;
    }

    public boolean setGenerationEnabled(Long inst, Boolean bool){ // requirement: 3.3.2.5
        int index = objInst.indexOf(inst);
        if (index == -1) return false;
        if (PDeflist.get(index).getGenerationEnabled().booleanValue() == bool) // Is it set with the requested value already?
            return false; // the value was not changed
            
        PDeflist.get(index).setGenerationEnabled(bool);
        this.save();
        return true;
    }

    public void setGenerationEnabledAll(Boolean bool){ 
        for (ParameterDefinitionDetails PDeflist1 : PDeflist)
            PDeflist1.setGenerationEnabled(bool);

        this.save();
    }
    
    public void serializeDataOut(ParameterManager ish) throws IOException{
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(ish);
        oos.flush();
        oos.close();
    }

    public ParameterManager serializeDataIn() throws FileNotFoundException, IOException, ClassNotFoundException{
        try{
            FileInputStream fin = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fin);

            ParameterManager iHandler = (ParameterManager) ois.readObject();
            ois.close();
            return iHandler;
        } catch (FileNotFoundException e){
            throw new FileNotFoundException();
        } catch (IOException e){
            throw new IOException();
        }
    }

}
