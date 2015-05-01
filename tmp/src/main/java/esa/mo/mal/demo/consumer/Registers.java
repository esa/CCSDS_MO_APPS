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
package esa.mo.mal.demo.consumer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cesar Coelho
 */
public class Registers implements Serializable {

    transient private final String filename = "Consumer_Database.data";
    private Vector parameterTableDataVector;
    private Vector aggregationTableDataVector;
    private List<Vector> parameterSetsTableDataVectorList;
    private ParameterLabel[] labels;
    
    Registers(){
    }
    
    private Registers(Vector parameterTableData1, Vector aggregationTableData1, ParameterLabel[] labels1, List<Vector> parameterSetsTableDataVectorList1){
            this.parameterTableDataVector = parameterTableData1;
            this.aggregationTableDataVector = aggregationTableData1;
            this.labels = labels1;
            this.parameterSetsTableDataVectorList = parameterSetsTableDataVectorList1;
    }

    public Vector getParameterTableDataVector(){
        return parameterTableDataVector;
    }
    
    public Vector getAggregationTableDataVector(){
        return aggregationTableDataVector;
    }

    public List getparameterSetsTableDataVectorList(){
        return parameterSetsTableDataVectorList;
    }

    public Vector getparameterSetsTableDataVector(int index){
        return parameterSetsTableDataVectorList.get(index);
    }

    public int sizeOfParameterSetsTable(){
        return parameterSetsTableDataVectorList.size();
    }
    
    public ParameterLabel getLabels(int i){
        return this.labels[i];
    }

    public boolean save(MityDemoConsumerGui gui){  // returns true if the file was successfully saved
        try {
            List<Vector> parameterSetsTableDataVectorList1 = new ArrayList<>();
            for (int i = 0; i < gui.parameterSetsTableDataAll.size(); i++)
                parameterSetsTableDataVectorList1.add(gui.parameterSetsTableDataAll.get(i).getDataVector());
            
            Registers obj = new Registers(gui.parameterTableData.getDataVector(), gui.aggregationTableData.getDataVector(), 
                    gui.labels, parameterSetsTableDataVectorList1 );
            serializeDataOut(obj);
            return true;
        } catch (IOException e){
            return false;
        }
    }

    public boolean load(MityDemoConsumerGui gui) {  
            try {
                Registers loadedFile = (Registers) serializeDataIn();
//                System.arraycopy(loadedFile.labels, 0, gui.labels, 0, loadedFile.labels.length);
                labels = loadedFile.labels;
                parameterTableDataVector = loadedFile.parameterTableDataVector;
                aggregationTableDataVector = loadedFile.aggregationTableDataVector;
                parameterSetsTableDataVectorList = loadedFile.parameterSetsTableDataVectorList;
                return true;
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Registers.class.getName()).log(Level.INFO, "There are no previous saved configurations!", ex);
                return false;
            }
    }

    public void serializeDataOut(Registers ish) throws IOException{
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(ish);
        oos.flush();
        oos.close();
    }

    public Registers serializeDataIn() throws FileNotFoundException, IOException, ClassNotFoundException{
        try{
            FileInputStream fin = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fin);
            Registers iHandler = (Registers) ois.readObject();
            ois.close();
            return iHandler;

        } catch (FileNotFoundException e){
            throw new FileNotFoundException();
        } catch (IOException e){
            Logger.getLogger(Registers.class.getName()).log(Level.SEVERE, null, e);
            throw new IOException();
        }
    }

}
