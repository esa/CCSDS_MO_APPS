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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.BooleanList;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.TimeList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetails;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetailsList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterSetList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationSetValue;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationSetValueList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValue;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValueList;
import org.ccsds.moims.mo.mc.aggregation.structures.GenerationMode;
import org.ccsds.moims.mo.mc.aggregation.structures.ThresholdFilter;
import org.ccsds.moims.mo.mc.aggregation.structures.ThresholdType;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValueList;

/**
 *
 * @author Cesar Coelho
 */
public class AggregationManager implements Serializable{
    
    private LongList objInst; // Object instance identifiers
    private BooleanList filterTriggered;  // Boolean Value that determines if the filter was triggered
    private AggregationDefinitionDetailsList aDeflist; // Aggregation Definitions list
    transient private AggregationValueList periodicAggregationValuesList; // Aggregation Value List per definition
    transient private List<TimeList> firstSampleTimeList; // Time of the first samaple of the Aggregation value
    private Long counter; // Counter (different for every Definition)
    private Long uniqueAValObjId;
    private final int saveAValObjIdLimit = 20;  // Used to store the uniqueAValObjId only once every "saveAValObjIdLimit" times
    transient private final String filename = "db_AggregationDefinitions.data";
    transient private final ParameterManager parameterManager;

    public AggregationManager(ParameterManager parameterManagerInput){
        this.objInst = new LongList(); // start a list of obj inst identifiers
        this.filterTriggered = new BooleanList(); // start a list that holds the status about the triggered filtering
        this.aDeflist = new AggregationDefinitionDetailsList(); // start a list of AggregationDefinitions
        this.periodicAggregationValuesList = new AggregationValueList(); // start the list of Aggregation Values lists
        this.firstSampleTimeList = new ArrayList<>(); // start the first sample times list
        this.counter = new Long(0); // The zeroth value will not be used (reserved for the wildcard)
        this.uniqueAValObjId = new Long(0); // The zeroth value will not be used (reserved for the wildcard)
        this.parameterManager = parameterManagerInput;
        this.load(); // Load the file
        this.createAggregationValuesList(this.objInst);
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

    private void createAggregationValuesList (LongList objIds){
        for (Long objId : objIds){
            int index = objInst.indexOf(objId);
            periodicAggregationValuesList.add(new AggregationValue());
            firstSampleTimeList.add(new TimeList());
            filterTriggered.add(false);
            populateAggregationValues(index);
        }
    }
    
    private void populateAggregationValues (final int index){
        final AggregationDefinitionDetails definition = this.aDeflist.get(index);
        AggregationSetValueList aggregationSetValueList = new AggregationSetValueList();
        for (int j = 0; j < definition.getParameterSets().size(); j++){
            firstSampleTimeList.get(index).add(j, new Time());
            aggregationSetValueList.add(j, new AggregationSetValue());
        }
        periodicAggregationValuesList.get(index).setParameterSetValues(aggregationSetValueList);
    }
   

    public Boolean resetPeriodicAggregationValuesList(Long objId){
        final int index = objInst.indexOf(objId);
        if (index == -1) return null;

        AggregationSetValueList aggregationSetValueList = periodicAggregationValuesList.get(index).getParameterSetValues();
        AggregationDefinitionDetails definition = this.aDeflist.get(index);
        for (int j = 0; j < definition.getParameterSets().size(); j++){
            firstSampleTimeList.get(index).set(j, new Time());
            aggregationSetValueList.set(j, new AggregationSetValue());
        }
        periodicAggregationValuesList.get(index).setParameterSetValues(aggregationSetValueList);

        this.setFilterTriggered(objId, false);  // Reset the filter state
        return true;
    }
    
    
    public Long generateAValobjId(){ 
        uniqueAValObjId++;
        if (uniqueAValObjId % saveAValObjIdLimit  == 0) // It is used to avoid constant saving every time we generate a new obj Inst identifier.
            this.save();
        return this.uniqueAValObjId;
    }

    private boolean load() {
        try {
            AggregationManager loadedFile = (AggregationManager) serializeDataIn();
            this.counter = loadedFile.counter;
            this.uniqueAValObjId = loadedFile.uniqueAValObjId + saveAValObjIdLimit; // Guarantees the obj inst id stays unique
            this.aDeflist.addAll(loadedFile.aDeflist);
            this.objInst.addAll(loadedFile.objInst);
//            this.periodicAggregationValuesList.addAll(loadedFile.periodicAggregationValuesList);
            this.save();
            return true;        // returns true if the file was successfully loaded
        } catch (FileNotFoundException ex ) {
            return false;
        } catch (IOException | ClassNotFoundException ex) {
            return false;
        }
    }
      
    public boolean exists(Long input){
        return objInst.indexOf(input) != -1;
    }

    public AggregationDefinitionDetails get(Long input){
        int index = objInst.indexOf(input);
        if (index == -1) return null;
        return aDeflist.get(index);
    }
   
    public ParameterValue sampleParameter(Long input){
        return parameterManager.getParameterValue(input);
    }

    public ParameterValueList sampleParameters(LongList input){
        ParameterValueList pValList = new ParameterValueList();
        for (Long input1 : input) 
            pValList.add(sampleParameter(input1));
        
        return pValList;
    }
    
    public Boolean triggeredFilter (Attribute previousValue, Attribute currentValue, ThresholdFilter filter ){
        if (filter == null) return false;  // If there's no filter, then it will never be ignored! // requirement: 3.7.2.7

        final Double previousValueDouble = Double.parseDouble(previousValue.toString());
        final Double currentValueDouble = Double.parseDouble(currentValue.toString());
        final Double thresholdValue = Double.parseDouble(filter.getThresholdValue().toString());
        
        if (filter.getThresholdType() == ThresholdType.DELTA)       // requirement: 3.7.2.8
            return ( Math.abs(previousValueDouble - currentValueDouble) > thresholdValue );
            
        if (filter.getThresholdType() == ThresholdType.PERCENTAGE)  // requirement: 3.7.2.8
            return ( Math.abs(previousValueDouble - currentValueDouble) / previousValueDouble * 100 > thresholdValue );

        return false;
    }

    public AggregationValueList getAggregationValuesList(LongList objIds, GenerationMode generationMode, Boolean filtered){
        AggregationValueList aValList = new AggregationValueList();
        for (Long objId : objIds) 
            aValList.add( getAggregationValue(objId, generationMode, filtered) );
        
        return aValList;
    }

    public AggregationValue getAggregationValue(Long objId, GenerationMode generationMode, Boolean filtered){
        final int index = objInst.indexOf(objId);
        if (index == -1) return null;

        AggregationValue aVal = new AggregationValue();
        AggregationParameterSetList parameterSets = this.aDeflist.get(index).getParameterSets();
        AggregationSetValueList parameterSetValues = new AggregationSetValueList();
        
        aVal.setGenerationMode(generationMode);
        aVal.setFiltered(filtered);

        for (int j = 0; j < parameterSets.size(); j++){  //Cycle through the parameterSets (requirement: 3.7.2.15)
            AggregationSetValue parameterSetValue = new AggregationSetValue();
            Duration sampleInterval = parameterSets.get(j).getSampleInterval();

            if ( generationMode == GenerationMode.PERIODIC &&
            sampleInterval.getValue() != 0  &&
            sampleInterval.getValue() <  this.aDeflist.get(index).getUpdateInterval().getValue() ){
                Time currentTime = new Time(System.currentTimeMillis());
                Time AggTimeStamp = new Time( currentTime.getValue() - (long) this.aDeflist.get(index).getUpdateInterval().getValue()*1000) ;
                Time firstSampleTime = new Time(this.firstSampleTimeList.get(index).get(j).getValue());
//int change:    Duration deltaTime = new Duration( (int) ((float)(firstSampleTime.getValue() - AggTimeStamp.getValue()))/1000 );  // Duration is in seconds but Time is in miliseconds
                Duration deltaTime = new Duration( ((float)(firstSampleTime.getValue() - AggTimeStamp.getValue()))/1000 );  // Duration is in seconds but Time is in miliseconds
                parameterSetValue.setDeltaTime( deltaTime );
                parameterSetValue.setIntervalTime(sampleInterval);
                parameterSetValue.setValues(periodicAggregationValuesList.get(index).getParameterSetValues().get(j).getValues()); // Get all the samples stored

            }else{  // One sample only of each?
                parameterSetValue.setDeltaTime(null);
                parameterSetValue.setIntervalTime(null);
                parameterSetValue.setValues(this.sampleParameters(parameterSets.get(j).getParameters())); // Do just one sampling...
            }
            parameterSetValues.add(parameterSetValue);
        }
        
        aVal.setParameterSetValues(parameterSetValues);
        return aVal;
    }

    public AggregationDefinitionDetailsList getAll(){
        return this.aDeflist;
    }
    
    public Boolean setFilterTriggered(Long objId, Boolean bool){
        final int index = objInst.indexOf(objId);
        if (index == -1) return false;
        
        this.filterTriggered.set(index, bool);
        return true;
    }

    public Boolean isFilterTriggered(Long objId){
        final int index = objInst.indexOf(objId);
        if (index == -1) return false;

        return this.filterTriggered.get(index);
    }

    public ParameterValueList getLastParameterValue(Long objId, int IndexOfparameterSet){
        final int index = objInst.indexOf(objId);
        if (index == -1) return null;
        
        ParameterValueList PValLst = new ParameterValueList();
        AggregationDefinitionDetails def = this.aDeflist.get(index);

        if (this.periodicAggregationValuesList.get(index).getParameterSetValues() == null)  // It was never sampled before?
            return null;

        if (this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).getValues() == null)  // It was never sampled before?
            return null;

        ParameterValueList currentPValLst = this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).getValues();
        int numberParametersPerSample = def.getParameterSets().get(IndexOfparameterSet).getParameters().size();
        int totalNumberParametersSampled = currentPValLst.size();
        
        PValLst.addAll(currentPValLst.subList(totalNumberParametersSampled - numberParametersPerSample, totalNumberParametersSampled));
        return PValLst;
    }

    public ParameterValueList updateParameterValue(Long objId, int IndexOfparameterSet){
        final int index = objInst.indexOf(objId);
        if (index == -1) return null;
        
        AggregationDefinitionDetails aggregationDefinition = this.aDeflist.get(index);

        ParameterValueList newSample = this.sampleParameters(aggregationDefinition.getParameterSets().get(IndexOfparameterSet).getParameters());

        if (this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).getValues() == null){ // Is it the first sample?
            this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).setValues(newSample);
            this.firstSampleTimeList.get(index).set(IndexOfparameterSet, new Time(System.currentTimeMillis())); 
        }else{
            ParameterValueList currentPValLst = this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).getValues();
            currentPValLst.addAll(newSample);  // then add to the current list the new Sample
            this.periodicAggregationValuesList.get(index).getParameterSetValues().get(IndexOfparameterSet).setValues(currentPValLst);
        }

        return newSample;
    }

    public Long list(Identifier input){
        for (int i = 0; i < aDeflist.size(); i++) {
            if (aDeflist.get(i).getName().equals(input))
                return objInst.get(i);
	}
        return null; // Not found!
    }

    public LongList listAll(){ // requirement: 3.3.2.8
        return this.objInst;
    }

    public Long add(AggregationDefinitionDetails definition){ // requirement: 3.3.2.5
        definition.setGenerationEnabled(false);  // requirement: 3.3.2.7
        counter++; // This line as to come before sttieng any objects (because counter is initialized as zero and that's the wildcard)
        aDeflist.add(definition);
        objInst.add(counter);
        filterTriggered.add(false);
        LongList longlist = new LongList();
        longlist.add(counter);
        this.createAggregationValuesList(longlist);
        
        this.save();
        return counter;
    }
      
    public boolean update(Long inst, AggregationDefinitionDetails definition){ // requirement: 3.3.2.5
        final int index = objInst.indexOf(inst);
        if (index == -1) return false;
        aDeflist.set(index, definition);  // requirement: 3.7.2.13
        populateAggregationValues(index);
        this.save();
        return true;
    }

    public boolean delete(Long inst){ // requirement: 3.3.2.5
        final int index = objInst.indexOf(inst);
        if (index == -1) return false;
        aDeflist.remove(index);
        objInst.remove(index);
        periodicAggregationValuesList.remove(index);
        firstSampleTimeList.remove(index);

        this.save();
        return true;
    }
   
    public boolean setGenerationEnabled(Long inst, Boolean bool){ // requirement: 3.3.2.5
        final int index = objInst.indexOf(inst);
        if (index == -1) return false;
        if (aDeflist.get(index).getGenerationEnabled().booleanValue() == bool) // Is it set with the requested value already?
            return false; // the value was not changed
            
        aDeflist.get(index).setGenerationEnabled(bool);
        this.save();
        return true;
    }

    public void setGenerationEnabledAll(Boolean bool){ 
        for (AggregationDefinitionDetails ADeflist1 : aDeflist)
            ADeflist1.setGenerationEnabled(bool);

        this.save();
    }
    
   public boolean setFilterEnabled(Long inst, Boolean bool){ // requirement: 3.3.2.5
        final int index = objInst.indexOf(inst);
        if (index == -1) return false;
        if (aDeflist.get(index).getFilterEnabled().booleanValue() == bool) // Is it set with the requested value already?
            return false; // the value was not changed
            
        aDeflist.get(index).setFilterEnabled(bool);
        this.save();
        return true;
    }

    public void setFilterEnabledAll(Boolean bool){ 
        for (AggregationDefinitionDetails ADeflist1 : aDeflist)
            ADeflist1.setFilterEnabled(bool);

        this.save();
    }
 
    public void serializeDataOut(AggregationManager ish) throws IOException{
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(ish);
        oos.flush();
        oos.close();
    }

    public AggregationManager serializeDataIn() throws FileNotFoundException, IOException, ClassNotFoundException{
        try{
            FileInputStream fin = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fin);

            AggregationManager iHandler = (AggregationManager) ois.readObject();
            ois.close();
            return iHandler;
        } catch (FileNotFoundException e){
            throw new FileNotFoundException();
        } catch (IOException e){
            throw new IOException();
        }
    }


}
