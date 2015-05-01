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

import esa.mo.mal.demo.util.StructureHelper;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.archive.structures.ExpressionOperator;
import org.ccsds.moims.mo.com.structures.InstanceBooleanPair;
import org.ccsds.moims.mo.com.structures.InstanceBooleanPairList;
import org.ccsds.moims.mo.com.structures.ObjectIdList;
import org.ccsds.moims.mo.com.structures.ObjectKey;
import org.ccsds.moims.mo.mal.MALContext;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.consumer.MALConsumer;
import org.ccsds.moims.mo.mal.consumer.MALConsumerManager;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.EntityKey;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.EntityRequest;
import org.ccsds.moims.mo.mal.structures.EntityRequestList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mal.structures.UpdateHeader;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mc.MCHelper;
import org.ccsds.moims.mo.mc.aggregation.AggregationHelper;
import org.ccsds.moims.mo.mc.aggregation.consumer.AggregationAdapter;
import org.ccsds.moims.mo.mc.aggregation.consumer.AggregationStub;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationCategory;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetails;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetailsList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterSet;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterSetList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationSetValue;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValue;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValueList;
import org.ccsds.moims.mo.mc.aggregation.structures.ThresholdFilter;
import org.ccsds.moims.mo.mc.aggregation.structures.ThresholdType;
import org.ccsds.moims.mo.mc.parameter.ParameterHelper;
import org.ccsds.moims.mo.mc.parameter.consumer.ParameterAdapter;
import org.ccsds.moims.mo.mc.parameter.consumer.ParameterStub;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterConversion;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValueList;
import org.ccsds.moims.mo.mc.structures.ParameterExpression;


/**
 * This class provides a simple form for the control of the consumer.
 */
public class MityDemoConsumerGui extends javax.swing.JFrame
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger LOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.demo.consumer");
  private final IdentifierList domain = new IdentifierList();
  private final Identifier network = new Identifier("GROUND");
  private final SessionType session = SessionType.LIVE;
  private final Identifier sessionName = new Identifier("LIVE");
  private final int numberOfColumns = 5;
  public ParameterLabel[] labels = new ParameterLabel[32*numberOfColumns];
  private final int map_width = 750; // number of pixels on the width of the map

  private final Subscription subRequestWildcard;
  private final Subscription subRequestHalf;
  private final Subscription subRequestAll;
  private final DelayManager delayManager;
  
  private WorldMap map;  // Map coordinates sync
  private double mapLatitude = 0;
  private double mapLongitude = 0;
  
  private final ParameterConsumerAdapter adapterParameter = new ParameterConsumerAdapter();
  private final AggregationConsumerAdapter adapterAggregation = new AggregationConsumerAdapter();
  private MALContextFactory malFactory;
  private MALContext mal;
  private MALConsumerManager consumerMgr;
  private MALConsumer tmConsumer = null;
  private MALConsumer tmConsumer1 = null;
  private ParameterStub parameterService = null;
  private AggregationStub aggregationService = null;
  private boolean running = true;
  private boolean isAddDef = false;
  public DefaultTableModel parameterTableData;
  public DefaultTableModel aggregationTableData;
  public DefaultTableModel parameterSetsTableData;
  public List<DefaultTableModel> parameterSetsTableDataAll = new ArrayList<>();
  private int parameterDefinitionSelectedIndex = 0;
  private int aggregationDefinitionSelectedIndex = 0;

  private String[] parameterSetsTableCol = new String [] { "Parameter", "sampleInterval", "th-type", "th-value" };
  
  
  
  /**
   * Main command line entry point.
   *
   * @param args the command line arguments
   */
  public static void main(final String args[])
  {
    try
    {
      final Properties sysProps = System.getProperties();

      final File file = new File(System.getProperty("provider.properties", "demoConsumer.properties"));
      if (file.exists())
      {
        sysProps.putAll(StructureHelper.loadProperties(file.toURI().toURL(), "provider.properties"));
      }

      System.setProperties(sysProps);

      final String name = System.getProperty("application.name", "Consumer Interface (CCSDS MO)");

      final MityDemoConsumerGui gui = new MityDemoConsumerGui(name);
      gui.init();

      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          gui.setVisible(true);
        }
      });
    }
    catch (MalformedURLException | MALException ex)
    {
      LOGGER.log(Level.SEVERE, "Exception thrown during initialisation of Demo Consumer {0}", ex);
    }
  }

  /**
   * Creates new form MityDemoProviderGui
   *
   * @param name The name to display on the title bar of the form.
   */
  public MityDemoConsumerGui(final String name)
  {
    initComponents();

    this.setTitle(name);

    delayManager = new DelayManager(delayLabel, 16);

    map = new WorldMap(map_width);
    
    String[] parameterTableCol = new String [] {
        "Obj Inst Id", "name", "description", "rawType", "rawUnit", "generationEnabled", "updateInterval"    };
        
    parameterTableData = new javax.swing.table.DefaultTableModel(
        new Object [][] { }, parameterTableCol         ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, 
                java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class
            };
            
            @Override               //all cells false
            public boolean isCellEditable(int row, int column) { return false;  }
               
            @Override
            public Class getColumnClass(int columnIndex) { return types [columnIndex];   }
        };
    
    String[] aggregationTableCol = new String [] {
                "Obj Inst Id", "name", "description", "category", 
            "generationEnabled", "updateInterval", "filterEnabled", "filteredTimeout"  };

    aggregationTableData = new javax.swing.table.DefaultTableModel(
        new Object [][] { }, aggregationTableCol         ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, 
                java.lang.Boolean.class, java.lang.Float.class, java.lang.Boolean.class, java.lang.Float.class
            };
            
            @Override               //all cells false
            public boolean isCellEditable(int row, int column) { return false;  }
               
            @Override
            public Class getColumnClass(int columnIndex) { return types [columnIndex];   }
        };
        
        
    parameterSetsTableData = new javax.swing.table.DefaultTableModel(
        new Object [][] { }, parameterSetsTableCol     ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.String.class, java.lang.Float.class
            };
            
            @Override               //all cells false
            public boolean isCellEditable(int row, int column) { return false;  }
               
            @Override
            public Class getColumnClass(int columnIndex) { return types [columnIndex];   }
        };

//    parameterSetsTableDataAll.add( new DefaultTableModel() );

    Registers reg = new Registers();

    boolean loadSuccess = reg.load(this);
    if (loadSuccess){
        parameterTableData.setDataVector( reg.getParameterTableDataVector(), new Vector<String>(Arrays.asList(parameterTableCol)) );
        aggregationTableData.setDataVector( reg.getAggregationTableDataVector(), new Vector<String>(Arrays.asList(aggregationTableCol)) );
        for (int i = 0; i < reg.sizeOfParameterSetsTable(); i++){
            parameterSetsTableDataAll.add( i, new DefaultTableModel() );
            parameterSetsTableDataAll.get(i).setDataVector(reg.getparameterSetsTableDataVector(i), new Vector<String>(Arrays.asList(parameterSetsTableCol)));
        }
    }
    
        final java.awt.Dimension dim = new java.awt.Dimension(64, 16);
        for (int i = 0; i < labels.length; ++i)
        {
            labels[i] = new ParameterLabel(i, delayManager);

            if (!loadSuccess){
                labels[i].setMinimumSize(dim);
                labels[i].setPreferredSize(dim);
                labels[i].setMaximumSize(dim);
                labels[i].setOpaque(true);
                labels[i].setBackground(Color.WHITE);
                labels[i].setForeground(Color.GREEN);
                labels[i].setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            }else{
                labels[i] = reg.getLabels(i);
            }

            this.ObjIdslotsTab.add(labels[i]);
        }

    parameterTable.setModel(parameterTableData);
    aggregationTable.setModel(aggregationTableData);
    parameterSetsTable.setModel(parameterSetsTableData);
    parameterTable.getTableHeader().setReorderingAllowed(false);
    aggregationTable.getTableHeader().setReorderingAllowed(false);
    parameterSetsTable.getTableHeader().setReorderingAllowed(false);
    
    for (int i = 0; i < parameterTable.getRowCount(); i++)  // Set all the enable fields on the parameter table to false
        parameterTable.setValueAt(false, i, 5);

    for (int i = 0; i < aggregationTable.getRowCount(); i++) // Set all the enable fields on the aggregation table to false
        aggregationTable.setValueAt(false, i, 4);
    

    // Set window size for the Add and Modify Parameter Definition
    editParameter.setSize(400, 500);
    editParameter.setPreferredSize(new Dimension(400, 500));
    editParameter.setResizable(false);

    domain.add(new Identifier("esa"));
    domain.add(new Identifier("mission"));

    final Identifier subscriptionId = new Identifier("SUB");
    // set up the wildcard subscription
    {
      final EntityKey entitykey = new EntityKey(new Identifier("*"), 0L, 0L, 0L);
      final EntityKeyList entityKeys = new EntityKeyList();
      entityKeys.add(entitykey);

      final EntityRequest entity = new EntityRequest(null, false, false, false, false, entityKeys);
      final EntityRequestList entities = new EntityRequestList();
      entities.add(entity);

      subRequestWildcard = new Subscription(subscriptionId, entities);
    }
    // set up the named first half subscription
    {
      final EntityKeyList entityKeys = new EntityKeyList();

      for (int i = 0; i < (labels.length / 2); i++)
      {
        final EntityKey entitykey = new EntityKey(new Identifier(String.valueOf(i)), 0L, 0L, 0L);
        entityKeys.add(entitykey);
      }

      final EntityRequest entity = new EntityRequest(null, false, false, false, false, entityKeys);
      final EntityRequestList entities = new EntityRequestList();
      entities.add(entity);

      subRequestHalf = new Subscription(subscriptionId, entities);
    }
    // set up the named all subscription
    {
      final EntityKeyList entityKeys = new EntityKeyList();

      for (int i = 0; i < labels.length; i++)
      {
        final EntityKey entitykey = new EntityKey(new Identifier(String.valueOf(i)), 0L, 0L, 0L);
        entityKeys.add(entitykey);
      }

      final EntityRequest entity = new EntityRequest(null, false, false, false, false, entityKeys);

      final EntityRequestList entities = new EntityRequestList();
      entities.add(entity);

      subRequestAll = new Subscription(subscriptionId, entities);
    }
  }

  public AggregationDefinitionDetails makeNewAggregationDefinition(String name, String description, AggregationCategory category, boolean generationEnabled, 
          float updateInterval, boolean filterEnabled, float filteredTimeout, AggregationParameterSetList parameterSets){
      AggregationDefinitionDetails aDef = new AggregationDefinitionDetails();

      aDef.setName(new Identifier(name));
      aDef.setDescription(description);
      aDef.setCategory(category);
      aDef.setGenerationEnabled(generationEnabled);

             
      aDef.setUpdateInterval( makeDuration(updateInterval) );

      aDef.setFilterEnabled(filterEnabled);  // shall not matter, because when we add it it will be false!
      aDef.setFilteredTimeout( makeDuration(filteredTimeout) ) ;
      aDef.setParameterSets(parameterSets);
      
      return aDef;
  }

  @SuppressWarnings("cast")
  public Duration makeDuration(double input){
      Duration durationOne = new Duration(1);
      Object value = durationOne.getValue();

//      return new Duration((int) Math.round(input));  // Then it is an int! (round the number before)
      return new Duration(input);
  }

  public ParameterDefinitionDetails makeNewParameterDefinition(String name, int rawType, String rawUnit, String description, 
          boolean generationEnabled, float interval, ParameterExpression validityExpression, ParameterConversion conversion){
      ParameterDefinitionDetails PDef = new ParameterDefinitionDetails();

      PDef.setName(new Identifier(name));
      PDef.setDescription(description);
      PDef.setRawType((byte) rawType);
      PDef.setRawUnit(rawUnit);
      PDef.setDescription(description);
      PDef.setGenerationEnabled(generationEnabled);  // shall not matter, because when we add it it will be false!
      PDef.setUpdateInterval( makeDuration(interval ) ) ;
      PDef.setValidityExpression(validityExpression);
      PDef.setConversion(conversion);
      
      return PDef;
  }

  public AggregationParameterSetList makeNewAggregationParameterSetList(){
      AggregationParameterSetList aggRefList = new AggregationParameterSetList();

      for (int i=0; i< parameterSetsTableData.getRowCount(); i++){
          AggregationParameterSet aggRef = new AggregationParameterSet();
          aggRef.setDomain(domain);
          LongList longList = new LongList();
          longList.add(getObjIdFromName (parameterSetsTableData.getValueAt(i, 0).toString()));
          aggRef.setParameters(longList);
          aggRef.setSampleInterval( makeDuration(Float.parseFloat( parameterSetsTableData.getValueAt(i, 1).toString() )) );
          if (parameterSetsTableData.getValueAt(i, 2).equals("-") ){
              aggRef.setPeriodicFilter(null);
          }else{
              ThresholdFilter periodicFilter = new ThresholdFilter();
              periodicFilter.setThresholdType(ThresholdType.fromString(parameterSetsTableData.getValueAt(i, 2).toString()));
              periodicFilter.setThresholdValue( makeDuration(Float.parseFloat(parameterSetsTableData.getValueAt(i, 3).toString() )) );
              aggRef.setPeriodicFilter(periodicFilter);
          }
          aggRefList.add(aggRef);
      }
      
      return aggRefList;
  }
  
  // Get ObjId from Name
  private Long getObjIdFromName(String name){
      for(int i = 0; i<parameterTableData.getRowCount(); i++){
          String parameter = parameterTableData.getValueAt(i, 1).toString();
          if (parameter.equals(name))
              return new Long(parameterTableData.getValueAt(i, 0).toString());
      }
      return null; // Not found (it shouldn't occur...)
  }

  // Get ObjId from Name
  private Long getParameterSetsObjIdFromAggNameAndIndex(String name, int index){
      for(int i = 0; i < aggregationTableData.getRowCount(); i++){
          String aggregation = aggregationTableData.getValueAt(i, 1).toString();
          if (aggregation.equals(name) && index <= parameterSetsTableDataAll.get(i).getRowCount())
              return getObjIdFromName(parameterSetsTableDataAll.get(i).getValueAt(index, 0).toString());
      }
      return null; // Not found (it shouldn't occur...)
  }

  public ParameterExpression makeNewParameterExpression(Long instId, int operator, Boolean useConverted, String value){
      ParameterExpression PExp = new ParameterExpression();
      
      PExp.setParameterId(new ObjectKey(domain, instId));
      PExp.setOperator(ExpressionOperator.fromOrdinal(operator));
      PExp.setUseConverted(useConverted);
      PExp.setValue(new Union(value));
      
      return PExp;
  }

  private void init() throws MALException, MalformedURLException
  {
    loadURIs();

    malFactory = MALContextFactory.newFactory();
    mal = malFactory.createMALContext(System.getProperties());

    MALHelper.init(MALContextFactory.getElementFactoryRegistry());
    MCHelper.init(MALContextFactory.getElementFactoryRegistry());
    COMHelper.deepInit(MALContextFactory.getElementFactoryRegistry());
    ParameterHelper.init(MALContextFactory.getElementFactoryRegistry());
    AggregationHelper.init(MALContextFactory.getElementFactoryRegistry());

    consumerMgr = mal.createConsumerManager();

    startService();

  }

  private void startService() throws MALException, MalformedURLException
  {
    // close old transport
    if (null != tmConsumer && null != tmConsumer1)
    {
      deregMenuItemActionPerformed(null);
      tmConsumer.close();
      tmConsumer1.close();
    }

    loadURIs();

    final String Parametertpuri = System.getProperty("ParameterURI");
    final String Parametertburi = System.getProperty("ParameterBroker");
    final String Aggregationtpuri = System.getProperty("AggregationURI");
    final String Aggregationtburi = System.getProperty("AggregationBroker");

    tmConsumer = consumerMgr.createConsumer((String) null,
            new URI(Parametertpuri),
            new URI(Parametertburi),
            ParameterHelper.PARAMETER_SERVICE,
            new Blob("".getBytes()),
            domain,
            network,
            session,
            sessionName,
            QoSLevel.ASSURED,
            System.getProperties(),
            new UInteger(0));

    tmConsumer1 = consumerMgr.createConsumer((String) null,
            new URI(Aggregationtpuri),
            new URI(Aggregationtburi),
            AggregationHelper.AGGREGATION_SERVICE,
            new Blob("".getBytes()),
            domain,
            network,
            session,
            sessionName,
            QoSLevel.ASSURED,
            System.getProperties(),
            new UInteger(0));

    parameterService = new ParameterStub(tmConsumer);
    aggregationService = new AggregationStub(tmConsumer1);
    
  }

  private void registerSubscription()
  {
    if (this.regWildcardRadioButtonMenuItem.isSelected())
    {
      this.regWildcardRadioButtonMenuItemActionPerformed(null);
    }
    else
    {
      if (this.regHalfRadioButtonMenuItem.isSelected())
      {
        this.regHalfRadioButtonMenuItemActionPerformed(null);
      }
      else
      {
        if (this.regAllRadioButtonMenuItem.isSelected())
        {
          this.regAllRadioButtonMenuItemActionPerformed(null);
        }
      }
    }
  }

  private void loadURIs() throws MalformedURLException
  {
    final java.util.Properties sysProps = System.getProperties();
    final String configFile = System.getProperty("providerURI.properties", "demoServiceURI.properties");
    final java.io.File file = new java.io.File(configFile);
    if (file.exists())
      sysProps.putAll(StructureHelper.loadProperties(file.toURI().toURL(), "providerURI.properties"));

    if (!uri1.getText().equals("") && !uri2.getText().equals("") &&
        !uri3.getText().equals("") && !uri4.getText().equals("")     ){
            System.setProperty("ParameterURI",      uri1.getText());
            System.setProperty("ParameterBroker",   uri2.getText());
            System.setProperty("AggregationURI",    uri3.getText());
            System.setProperty("AggregationBroker", uri4.getText());
    }

    System.setProperties(sysProps);

    // Initial fillng of the text boxes
    if (uri1.getText().equals("") && uri2.getText().equals("") &&
        uri3.getText().equals("") && uri4.getText().equals("")     ){
            uri1.setText(System.getProperty("ParameterURI"));
            uri2.setText(System.getProperty("ParameterBroker"));
            uri3.setText(System.getProperty("AggregationURI"));
            uri4.setText(System.getProperty("AggregationBroker"));
    }
  }

  private class ParameterConsumerAdapter extends ParameterAdapter
  {
    @Override
    public void monitorValueNotifyReceived(final MALMessageHeader msgHeader,
            final Identifier lIdentifier,
            final UpdateHeaderList lUpdateHeaderList,
            final ObjectIdList lObjectIdList,
            final ParameterValueList lParameterValueList,
            final Map qosp)
    {
      LOGGER.log(Level.INFO, "Received update parameters list of size : {0}", lObjectIdList.size());
      final long iDiff = System.currentTimeMillis() - msgHeader.getTimestamp().getValue();

      for (int i = 0; i < lObjectIdList.size(); i++)
      {
        final UpdateHeader updateHeader = lUpdateHeaderList.get(i);
        final ParameterValue parameterValue = lParameterValueList.get(i);
        final String name = updateHeader.getKey().getFirstSubKey().getValue();

        try
        {
          final int objId = updateHeader.getKey().getSecondSubKey().intValue();
          
          final int index = (int) ((5*numberOfColumns)*Math.floor (objId/(5)) + objId%numberOfColumns);


//          pictureLabel.setIcon(map.addCoordinate(+37.0620, -7.8070));

          if ((0 <= index) && (index < labels.length))
          {
            UOctet validityState = parameterValue.getInvalidSubState();
//            Validity validityString = Validity.fromNumericValue(new UInteger(validityState.getValue()));
            Union rawValue = (Union) parameterValue.getRawValue();
            
            Union convertedValue = (Union) parameterValue.getConvertedValue();
            String convertedValueStr = (convertedValue == null) ? "null" : convertedValue.getDoubleValue().toString() ;
            
            labels[index+0*numberOfColumns].setNewValue( String.valueOf(objId)  , iDiff);
//            labels[index+1*numberOfColumns].setNewValue( validityString.toString() , iDiff);
            labels[index+1*numberOfColumns].setNewValue( validityState.toString() , iDiff);
            labels[index+2*numberOfColumns].setNewValue( rawValue.toString() , iDiff);
            labels[index+3*numberOfColumns].setNewValue( convertedValueStr , iDiff);
            
            // Aggregation Map
//            if (!labels[12].getText().equals("") && !labels[13].getText().equals("") )
//                pictureLabel.setIcon(map.addCoordinate(Double.valueOf(labels[12].getText()), Double.valueOf(labels[13].getText()) ));
          }
        }
        catch (NumberFormatException ex)
        {
          LOGGER.log(Level.WARNING, "Error decoding update with name: {0}", name);
        }
      }
    }
  }

  private class AggregationConsumerAdapter extends AggregationAdapter
  {
    @Override
    public void monitorValueNotifyReceived(final MALMessageHeader msgHeader,
            final Identifier lIdentifier,
            final UpdateHeaderList lUpdateHeaderList,
            final ObjectIdList lObjectIdList,
            final AggregationValueList lAggregationValueList,
            final Map qosp)
    {
      LOGGER.log(Level.INFO, "Received update aggregations list of size : {0}", lObjectIdList.size());
      final long iDiff = System.currentTimeMillis() - msgHeader.getTimestamp().getValue();

      final UpdateHeader updateHeader = lUpdateHeaderList.get(0);
      final String Aggname = updateHeader.getKey().getFirstSubKey().getValue();
      final int objId = updateHeader.getKey().getSecondSubKey().intValue();

      try{
        if (msgBoxOn.isSelected() && lUpdateHeaderList.size() != 0 && lAggregationValueList.size() != 0){
            String str = "";
            final AggregationValue aggregationValue = lAggregationValueList.get(0);
            str += "AggregationValue generationMode: " + aggregationValue.getGenerationMode().toString() + " (filtered: " + aggregationValue.getFiltered().toString() + ")" + "\n";
            
            str += "Aggregation objId " + objId + " (name: " + Aggname + "):" + "\n";

            for(int i=0; i < aggregationValue.getParameterSetValues().size(); i++){  // Cycle through parameterSetValues
                str += "- AggregationParameterSet values index: " + i + "\n";
                str += "deltaTime: " + aggregationValue.getParameterSetValues().get(i).getDeltaTime();
                str += " and intervalTime: " + aggregationValue.getParameterSetValues().get(i).getIntervalTime() + "\n";
                AggregationSetValue parameterSetsValue = aggregationValue.getParameterSetValues().get(i);

                for(int j=0; j < parameterSetsValue.getValues().size(); j++){ // Cycle through the values
                    if (parameterSetsValue.getValues().get(j) == null)
                        continue;
                    str += "values index: " + j + "\n";
                    str += "validityState: " + parameterSetsValue.getValues().get(j).getInvalidSubState().toString() + "\n";
                    if (parameterSetsValue.getValues().get(j).getRawValue() != null)
                        str += "rawValue: " + parameterSetsValue.getValues().get(j).getRawValue().toString() + "\n";
                    if (parameterSetsValue.getValues().get(j).getConvertedValue() != null)
                        str += "convertedValue: " + parameterSetsValue.getValues().get(j).getConvertedValue().toString() + "\n";
                    str += "\n";
                }
            }
            
            JOptionPane.showMessageDialog(null, str, "Returned Values from the Provider", JOptionPane.PLAIN_MESSAGE);
        }

        if (Aggname.equals("Map")){
            for (int i = 0; i < lAggregationValueList.get(0).getParameterSetValues().size(); i++ ){
                if (getParameterSetsObjIdFromAggNameAndIndex(Aggname, i) == null || lAggregationValueList.get(0).getParameterSetValues().get(i).getValues().get(0) == null) // Not found... :/
                    continue;
                
                // Is it the latitude parameter?
                if ( getParameterSetsObjIdFromAggNameAndIndex(Aggname, i).equals(getObjIdFromName("GPS.Latitude")) )
                    mapLatitude = Double.parseDouble(lAggregationValueList.get(0).getParameterSetValues().get(i).getValues().get(0).getRawValue().toString());

                // Is it the longitude parameter?
                if ( getParameterSetsObjIdFromAggNameAndIndex(Aggname, i).equals(getObjIdFromName("GPS.Longitude")) )
                    mapLongitude = Double.parseDouble(lAggregationValueList.get(0).getParameterSetValues().get(i).getValues().get(0).getRawValue().toString());
                    
                if (mapLatitude != 0 && mapLongitude != 0 ){  // So, it's still the same message?
                    pictureLabel.setIcon(map.addCoordinate(mapLatitude, mapLongitude));
                    break;
                }
            }
            mapLatitude = 0;
            mapLongitude = 0;
        }
        
    }catch (NumberFormatException ex){
        LOGGER.log(Level.WARNING, "Error decoding update with name: {0}", lUpdateHeaderList.get(0).getKey().getFirstSubKey().getValue());
        }
    }
  }
  
  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
   * content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        jButton1 = new javax.swing.JButton();
        subscriptionButtonGroup = new javax.swing.ButtonGroup();
        editParameter = new javax.swing.JFrame();
        jPanel3 = new javax.swing.JPanel();
        titleEditParameter = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nameTF = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        descriptionTF = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        rawTypeCB = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        rawUnitTF = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        updateIntervalTF = new javax.swing.JTextField();
        generationEnabledCB = new javax.swing.JCheckBox();
        jSeparator7 = new javax.swing.JSeparator();
        validityExpressionCB = new javax.swing.JCheckBox();
        jPanel10 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        validity1 = new javax.swing.JComboBox();
        validity2 = new javax.swing.JComboBox();
        validity3 = new javax.swing.JTextField();
        validity4 = new javax.swing.JCheckBox();
        jSeparator4 = new javax.swing.JSeparator();
        conversionCB = new javax.swing.JCheckBox();
        jSeparator8 = new javax.swing.JSeparator();
        submitButton = new javax.swing.JButton();
        editAggregation = new javax.swing.JFrame();
        jPanel13 = new javax.swing.JPanel();
        titleEditParameter1 = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        jPanel14 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        nameTF1 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        descriptionTF1 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        categoryCB1 = new javax.swing.JComboBox();
        jLabel19 = new javax.swing.JLabel();
        updateIntervalTF1 = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        filteredTimeoutTF1 = new javax.swing.JTextField();
        generationEnabledCB1 = new javax.swing.JCheckBox();
        filterEnabledCB1 = new javax.swing.JCheckBox();
        jSeparator10 = new javax.swing.JSeparator();
        jPanel15 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        parameterCB1 = new javax.swing.JComboBox();
        sampleIntervalTB1 = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        thresholdTypeCB1 = new javax.swing.JComboBox();
        thresholdValueTB1 = new javax.swing.JTextField();
        aggregateParameterButton = new javax.swing.JButton();
        jSeparator12 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        parameterSetsTable = new javax.swing.JTable();
        removeParameter = new javax.swing.JButton();
        jSeparator13 = new javax.swing.JSeparator();
        submitButton1 = new javax.swing.JButton();
        tabs = new javax.swing.JTabbedPane();
        homeTab = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        jPanel9 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        uri1 = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        uri2 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        uri3 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        uri4 = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        parameterTab = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        parameterTable = new javax.swing.JTable();
        jSeparator3 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        getValueButton = new javax.swing.JButton();
        enableDefinitionButton = new javax.swing.JButton();
        addDefinitionButton = new javax.swing.JButton();
        updateDefinitionButton = new javax.swing.JButton();
        removeDefinitionButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        getValueAllButton = new javax.swing.JButton();
        enableAllDefinition = new javax.swing.JButton();
        listDefinitionAllButton = new javax.swing.JButton();
        removeDefinitionAllButton = new javax.swing.JButton();
        ObjIdslotsTab = new javax.swing.JPanel();
        aggregationTab = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        aggregationTable = new javax.swing.JTable();
        jSeparator5 = new javax.swing.JSeparator();
        jPanel7 = new javax.swing.JPanel();
        getValueButtonAgg = new javax.swing.JButton();
        enableDefinitionButtonAgg = new javax.swing.JButton();
        enableFilterButtonAgg = new javax.swing.JButton();
        addDefinitionButtonAgg = new javax.swing.JButton();
        updateDefinitionButtonAgg = new javax.swing.JButton();
        removeDefinitionButtonAgg = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        getValueAllButtonAgg = new javax.swing.JButton();
        enableDefinitionAllAgg = new javax.swing.JButton();
        enableFilterAllAgg = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        listDefinitionAllButtonAgg = new javax.swing.JButton();
        removeDefinitionAllButtonAgg = new javax.swing.JButton();
        msgBoxOn = new javax.swing.JCheckBox();
        exampleTab = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jSeparator11 = new javax.swing.JSeparator();
        pictureLabel = new javax.swing.JLabel();
        jToolBar1 = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        delayLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        quitMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        regWildcardRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        regHalfRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        regAllRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        deregMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem1 = new javax.swing.JMenuItem();

        jButton1.setText("jButton1");
        jButton1.setName("jButton1"); // NOI18N

        editParameter.setMinimumSize(new java.awt.Dimension(400, 270));
        editParameter.setName("editParameter"); // NOI18N
        editParameter.setResizable(false);

        jPanel3.setMaximumSize(new java.awt.Dimension(400, 300));
        jPanel3.setMinimumSize(new java.awt.Dimension(400, 300));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(400, 300));

        titleEditParameter.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        titleEditParameter.setText("Auto-change Label");
        titleEditParameter.setName("titleEditParameter"); // NOI18N
        jPanel3.add(titleEditParameter);

        jSeparator2.setName("jSeparator2"); // NOI18N
        jSeparator2.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel3.add(jSeparator2);

        jPanel4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel4.setMaximumSize(new java.awt.Dimension(280, 400));
        jPanel4.setMinimumSize(new java.awt.Dimension(280, 400));
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(280, 400));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("name");
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel2);

        nameTF.setText("name");
        nameTF.setName("nameTF"); // NOI18N
        nameTF.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel4.add(nameTF);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("description");
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel3);

        descriptionTF.setText("description");
        descriptionTF.setName("descriptionTF"); // NOI18N
        descriptionTF.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel4.add(descriptionTF);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("rawType");
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel4);

        rawTypeCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-", "Blob", "Boolean", "Duration", "Float", "Double", "Identifier", "Octet", "UOctet", "Short", "UShort", "Integer", "UInteger", "Long", "ULong", "String", "Time", "FineTime", "URI", " " }));
        rawTypeCB.setMaximumSize(new java.awt.Dimension(150, 20));
        rawTypeCB.setMinimumSize(new java.awt.Dimension(150, 20));
        rawTypeCB.setName("rawTypeCB"); // NOI18N
        rawTypeCB.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel4.add(rawTypeCB);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("rawUnit");
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel5);

        rawUnitTF.setText("2");
        rawUnitTF.setName("rawUnitTF"); // NOI18N
        rawUnitTF.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel4.add(rawUnitTF);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("updateInterval");
        jLabel7.setName("jLabel7"); // NOI18N
        jLabel7.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel7);

        updateIntervalTF.setText("3");
        updateIntervalTF.setName("updateIntervalTF"); // NOI18N
        updateIntervalTF.setPreferredSize(new java.awt.Dimension(150, 20));
        updateIntervalTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateIntervalTFActionPerformed(evt);
            }
        });
        jPanel4.add(updateIntervalTF);

        generationEnabledCB.setText("generationEnabled");
        generationEnabledCB.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        generationEnabledCB.setName("generationEnabledCB"); // NOI18N
        generationEnabledCB.setOpaque(false);
        generationEnabledCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generationEnabledCBActionPerformed(evt);
            }
        });
        jPanel4.add(generationEnabledCB);

        jSeparator7.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator7.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator7.setName("jSeparator7"); // NOI18N
        jSeparator7.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel4.add(jSeparator7);

        validityExpressionCB.setText("validityExpression");
        validityExpressionCB.setName("validityExpressionCB"); // NOI18N
        validityExpressionCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validityExpressionCBActionPerformed(evt);
            }
        });
        jPanel4.add(validityExpressionCB);

        jPanel10.setMaximumSize(new java.awt.Dimension(280, 70));
        jPanel10.setMinimumSize(new java.awt.Dimension(280, 70));
        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setPreferredSize(new java.awt.Dimension(280, 70));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("parameter");
        jLabel8.setMaximumSize(new java.awt.Dimension(40, 14));
        jLabel8.setMinimumSize(new java.awt.Dimension(40, 14));
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel10.add(jLabel8);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("operator");
        jLabel9.setName("jLabel9"); // NOI18N
        jLabel9.setPreferredSize(new java.awt.Dimension(90, 14));
        jPanel10.add(jLabel9);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("value");
        jLabel11.setName("jLabel11"); // NOI18N
        jLabel11.setPreferredSize(new java.awt.Dimension(60, 14));
        jPanel10.add(jLabel11);

        validity1.setEnabled(false);
        validity1.setMaximumSize(new java.awt.Dimension(100, 20));
        validity1.setMinimumSize(new java.awt.Dimension(100, 20));
        validity1.setName("validity1"); // NOI18N
        validity1.setPreferredSize(new java.awt.Dimension(100, 20));
        validity1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validity1ActionPerformed(evt);
            }
        });
        jPanel10.add(validity1);

        validity2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "≠", ">", "≥", "<", "≤", "CONTAINS", "ICONTAINS" }));
        validity2.setEnabled(false);
        validity2.setMaximumSize(new java.awt.Dimension(90, 20));
        validity2.setMinimumSize(new java.awt.Dimension(90, 20));
        validity2.setName("validity2"); // NOI18N
        validity2.setPreferredSize(new java.awt.Dimension(90, 20));
        validity2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validity2ActionPerformed(evt);
            }
        });
        jPanel10.add(validity2);

        validity3.setEnabled(false);
        validity3.setName("validity3"); // NOI18N
        validity3.setPreferredSize(new java.awt.Dimension(60, 20));
        validity3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validity3ActionPerformed(evt);
            }
        });
        jPanel10.add(validity3);

        validity4.setText("useConverted");
        validity4.setEnabled(false);
        validity4.setMaximumSize(new java.awt.Dimension(180, 23));
        validity4.setMinimumSize(new java.awt.Dimension(180, 23));
        validity4.setName("validity4"); // NOI18N
        validity4.setPreferredSize(null);
        validity4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validity4ActionPerformed(evt);
            }
        });
        jPanel10.add(validity4);

        jPanel4.add(jPanel10);
        jPanel10.getAccessibleContext().setAccessibleName("");

        jSeparator4.setEnabled(false);
        jSeparator4.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator4.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator4.setName("jSeparator4"); // NOI18N
        jSeparator4.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel4.add(jSeparator4);

        conversionCB.setText("conversion");
        conversionCB.setEnabled(false);
        conversionCB.setName("conversionCB"); // NOI18N
        conversionCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conversionCBActionPerformed(evt);
            }
        });
        jPanel4.add(conversionCB);

        jSeparator8.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator8.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator8.setName("jSeparator8"); // NOI18N
        jSeparator8.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel4.add(jSeparator8);

        submitButton.setText("Submit");
        submitButton.setName("submitButton"); // NOI18N
        submitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitButtonActionPerformed(evt);
            }
        });
        jPanel4.add(submitButton);

        jPanel3.add(jPanel4);

        editParameter.getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        editAggregation.setMinimumSize(new java.awt.Dimension(400, 590));
        editAggregation.setName("editAggregation"); // NOI18N
        editAggregation.setResizable(false);

        jPanel13.setMaximumSize(new java.awt.Dimension(400, 300));
        jPanel13.setMinimumSize(new java.awt.Dimension(400, 300));
        jPanel13.setName("jPanel13"); // NOI18N
        jPanel13.setPreferredSize(new java.awt.Dimension(400, 300));

        titleEditParameter1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        titleEditParameter1.setText("Auto-change Label");
        titleEditParameter1.setName("titleEditParameter1"); // NOI18N
        jPanel13.add(titleEditParameter1);

        jSeparator9.setName("jSeparator9"); // NOI18N
        jSeparator9.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel13.add(jSeparator9);

        jPanel14.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel14.setMaximumSize(new java.awt.Dimension(280, 400));
        jPanel14.setMinimumSize(new java.awt.Dimension(280, 400));
        jPanel14.setName("jPanel14"); // NOI18N
        jPanel14.setPreferredSize(new java.awt.Dimension(280, 520));

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel15.setText("name");
        jLabel15.setName("jLabel15"); // NOI18N
        jLabel15.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel14.add(jLabel15);

        nameTF1.setText("name");
        nameTF1.setName("nameTF1"); // NOI18N
        nameTF1.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel14.add(nameTF1);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("description");
        jLabel16.setName("jLabel16"); // NOI18N
        jLabel16.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel14.add(jLabel16);

        descriptionTF1.setText("description");
        descriptionTF1.setName("descriptionTF1"); // NOI18N
        descriptionTF1.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel14.add(descriptionTF1);

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setText("category");
        jLabel17.setName("jLabel17"); // NOI18N
        jLabel17.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel14.add(jLabel17);

        categoryCB1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-", "GENERAL", "DIAGNOSTIC" }));
        categoryCB1.setMaximumSize(new java.awt.Dimension(150, 20));
        categoryCB1.setMinimumSize(new java.awt.Dimension(150, 20));
        categoryCB1.setName("categoryCB1"); // NOI18N
        categoryCB1.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel14.add(categoryCB1);

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setText("updateInterval");
        jLabel19.setName("jLabel19"); // NOI18N
        jLabel19.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel14.add(jLabel19);

        updateIntervalTF1.setText("3");
        updateIntervalTF1.setName("updateIntervalTF1"); // NOI18N
        updateIntervalTF1.setPreferredSize(new java.awt.Dimension(150, 20));
        updateIntervalTF1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateIntervalTF1ActionPerformed(evt);
            }
        });
        jPanel14.add(updateIntervalTF1);

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel18.setText("filteredTimeout");
        jLabel18.setName("jLabel18"); // NOI18N
        jLabel18.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel14.add(jLabel18);

        filteredTimeoutTF1.setText("2");
        filteredTimeoutTF1.setName("filteredTimeoutTF1"); // NOI18N
        filteredTimeoutTF1.setPreferredSize(new java.awt.Dimension(150, 20));
        jPanel14.add(filteredTimeoutTF1);

        generationEnabledCB1.setText("generationEnabled");
        generationEnabledCB1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        generationEnabledCB1.setName("generationEnabledCB1"); // NOI18N
        generationEnabledCB1.setOpaque(false);
        generationEnabledCB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generationEnabledCB1ActionPerformed(evt);
            }
        });
        jPanel14.add(generationEnabledCB1);

        filterEnabledCB1.setText("filterEnabled");
        filterEnabledCB1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        filterEnabledCB1.setName("filterEnabledCB1"); // NOI18N
        filterEnabledCB1.setOpaque(false);
        filterEnabledCB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterEnabledCB1ActionPerformed(evt);
            }
        });
        jPanel14.add(filterEnabledCB1);

        jSeparator10.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator10.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator10.setName("jSeparator10"); // NOI18N
        jSeparator10.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel14.add(jSeparator10);

        jPanel15.setMaximumSize(new java.awt.Dimension(280, 70));
        jPanel15.setMinimumSize(new java.awt.Dimension(280, 70));
        jPanel15.setName("jPanel15"); // NOI18N
        jPanel15.setPreferredSize(new java.awt.Dimension(280, 120));

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("parameter");
        jLabel20.setMaximumSize(new java.awt.Dimension(40, 14));
        jLabel20.setMinimumSize(new java.awt.Dimension(40, 14));
        jLabel20.setName("jLabel20"); // NOI18N
        jLabel20.setPreferredSize(new java.awt.Dimension(130, 14));
        jPanel15.add(jLabel20);

        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel22.setText("sampleInterval");
        jLabel22.setName("jLabel22"); // NOI18N
        jLabel22.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel15.add(jLabel22);

        parameterCB1.setMaximumSize(new java.awt.Dimension(100, 20));
        parameterCB1.setMinimumSize(new java.awt.Dimension(100, 20));
        parameterCB1.setName("parameterCB1"); // NOI18N
        parameterCB1.setPreferredSize(new java.awt.Dimension(130, 20));
        parameterCB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parameterCB1ActionPerformed(evt);
            }
        });
        jPanel15.add(parameterCB1);

        sampleIntervalTB1.setName("sampleIntervalTB1"); // NOI18N
        sampleIntervalTB1.setPreferredSize(new java.awt.Dimension(100, 20));
        sampleIntervalTB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleIntervalTB1ActionPerformed(evt);
            }
        });
        jPanel15.add(sampleIntervalTB1);

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel21.setText("thresholdType");
        jLabel21.setName("jLabel21"); // NOI18N
        jLabel21.setPreferredSize(new java.awt.Dimension(130, 14));
        jPanel15.add(jLabel21);

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel23.setText("thresholdValue");
        jLabel23.setName("jLabel23"); // NOI18N
        jLabel23.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel15.add(jLabel23);

        thresholdTypeCB1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-", "PERCENTAGE", "DELTA" }));
        thresholdTypeCB1.setMaximumSize(new java.awt.Dimension(150, 20));
        thresholdTypeCB1.setMinimumSize(new java.awt.Dimension(150, 20));
        thresholdTypeCB1.setName("thresholdTypeCB1"); // NOI18N
        thresholdTypeCB1.setPreferredSize(new java.awt.Dimension(130, 20));
        jPanel15.add(thresholdTypeCB1);

        thresholdValueTB1.setName("thresholdValueTB1"); // NOI18N
        thresholdValueTB1.setPreferredSize(new java.awt.Dimension(100, 20));
        thresholdValueTB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thresholdValueTB1ActionPerformed(evt);
            }
        });
        jPanel15.add(thresholdValueTB1);

        aggregateParameterButton.setText("Aggregate Parameter");
        aggregateParameterButton.setName("aggregateParameterButton"); // NOI18N
        aggregateParameterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aggregateParameterButtonActionPerformed(evt);
            }
        });
        jPanel15.add(aggregateParameterButton);

        jPanel14.add(jPanel15);

        jSeparator12.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator12.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator12.setName("jSeparator12"); // NOI18N
        jSeparator12.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel14.add(jSeparator12);

        jScrollPane1.setName("jScrollPane1"); // NOI18N
        jScrollPane1.setPreferredSize(new java.awt.Dimension(280, 100));

        parameterSetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "parameter", "sampleInterval", "th-Type", "th-Value"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        parameterSetsTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        parameterSetsTable.setMaximumSize(null);
        parameterSetsTable.setMinimumSize(null);
        parameterSetsTable.setName("parameterSetsTable"); // NOI18N
        parameterSetsTable.setPreferredSize(null);
        parameterSetsTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(parameterSetsTable);

        jPanel14.add(jScrollPane1);

        removeParameter.setText("Remove Parameter");
        removeParameter.setName("removeParameter"); // NOI18N
        removeParameter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeParameterActionPerformed(evt);
            }
        });
        jPanel14.add(removeParameter);

        jSeparator13.setMaximumSize(new java.awt.Dimension(250, 10));
        jSeparator13.setMinimumSize(new java.awt.Dimension(250, 10));
        jSeparator13.setName("jSeparator13"); // NOI18N
        jSeparator13.setPreferredSize(new java.awt.Dimension(250, 10));
        jPanel14.add(jSeparator13);

        submitButton1.setText("Submit");
        submitButton1.setName("submitButton1"); // NOI18N
        submitButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitButton1ActionPerformed(evt);
            }
        });
        jPanel14.add(submitButton1);

        jPanel13.add(jPanel14);

        editAggregation.getContentPane().add(jPanel13, java.awt.BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(800, 600));
        setMinimumSize(new java.awt.Dimension(800, 600));
        setName("Form"); // NOI18N
        setPreferredSize(new java.awt.Dimension(800, 600));
        setResizable(false);
        getContentPane().setLayout(new java.awt.BorderLayout(0, 4));

        tabs.setToolTipText("");
        tabs.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        tabs.setMaximumSize(new java.awt.Dimension(800, 600));
        tabs.setMinimumSize(new java.awt.Dimension(800, 600));
        tabs.setName("tabs"); // NOI18N
        tabs.setPreferredSize(new java.awt.Dimension(800, 600));
        tabs.setRequestFocusEnabled(false);

        homeTab.setName("homeTab"); // NOI18N

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel6.setText("To start, please connect to the provider:");
        jLabel6.setName("jLabel6"); // NOI18N
        homeTab.add(jLabel6);

        jSeparator6.setName("jSeparator6"); // NOI18N
        jSeparator6.setPreferredSize(new java.awt.Dimension(700, 15));
        homeTab.add(jSeparator6);

        jPanel9.setName("jPanel9"); // NOI18N
        jPanel9.setPreferredSize(new java.awt.Dimension(550, 150));

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Parameter Service URI:");
        jLabel10.setName("jLabel10"); // NOI18N
        jLabel10.setPreferredSize(new java.awt.Dimension(150, 14));
        jPanel9.add(jLabel10);

        uri1.setName("uri1"); // NOI18N
        uri1.setPreferredSize(new java.awt.Dimension(350, 20));
        uri1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uri1ActionPerformed(evt);
            }
        });
        jPanel9.add(uri1);

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Parameter Broker URI:");
        jLabel12.setName("jLabel12"); // NOI18N
        jLabel12.setPreferredSize(new java.awt.Dimension(150, 14));
        jPanel9.add(jLabel12);

        uri2.setName("uri2"); // NOI18N
        uri2.setPreferredSize(new java.awt.Dimension(350, 20));
        uri2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uri2ActionPerformed(evt);
            }
        });
        jPanel9.add(uri2);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Aggregation Service URI:");
        jLabel13.setName("jLabel13"); // NOI18N
        jLabel13.setPreferredSize(new java.awt.Dimension(150, 14));
        jPanel9.add(jLabel13);

        uri3.setName("uri3"); // NOI18N
        uri3.setPreferredSize(new java.awt.Dimension(350, 20));
        uri3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uri3ActionPerformed(evt);
            }
        });
        jPanel9.add(uri3);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel14.setText("Aggregation Service URI:");
        jLabel14.setName("jLabel14"); // NOI18N
        jLabel14.setPreferredSize(new java.awt.Dimension(150, 14));
        jPanel9.add(jLabel14);

        uri4.setName("uri4"); // NOI18N
        uri4.setPreferredSize(new java.awt.Dimension(350, 20));
        uri4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uri4ActionPerformed(evt);
            }
        });
        jPanel9.add(uri4);

        connectButton.setText("Connect to Provider");
        connectButton.setName("connectButton"); // NOI18N
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        jPanel9.add(connectButton);

        homeTab.add(jPanel9);

        jScrollPane4.setName("jScrollPane4"); // NOI18N
        jScrollPane4.setPreferredSize(new java.awt.Dimension(350, 240));

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Instructions:\n1. Parameter Service Tab: Add some parameters and interact with them by playing with their definitions.\n2. Published Parameter Values: Visualize the received parameter values. The slots are sorted by the object instance identifier value.\n3. Aggregation Service: Make an aggregation of parameters.\nHint: To visualize the location of the spacecraft on a map, make an Aggregation named: \"Map\" and aggregate the parameters GPS.Longitude and GPS.Latitude. It is necessary to select a small updateInterval and the definition must be enabled.\n4. Aggregation Map: Visualize the Aggregation Map defined on the previous step.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setMinimumSize(new java.awt.Dimension(600, 120));
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane4.setViewportView(jTextArea1);

        homeTab.add(jScrollPane4);

        jScrollPane5.setName("jScrollPane5"); // NOI18N
        jScrollPane5.setPreferredSize(new java.awt.Dimension(250, 240));

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setText("    Reserved Parameters:\nname: GPS.Altitude\nname: GPS.Longitude\nname: GPS.Latitude\nname: FineADCS.Magnetometer.B_r\nname: FineADCS.Magnetometer.B_theta\nname: Zero\nname: One\nname: Two\nname: Three\nname: Four\n    Reserved Aggregation:\nname: Map\nparameters: Latitude, Longitude");
        jTextArea2.setMinimumSize(new java.awt.Dimension(2000, 120));
        jTextArea2.setName("jTextArea2"); // NOI18N
        jScrollPane5.setViewportView(jTextArea2);

        homeTab.add(jScrollPane5);

        tabs.addTab("Home", homeTab);

        parameterTab.setName("parameterTab"); // NOI18N

        jPanel2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setPreferredSize(new java.awt.Dimension(798, 350));

        jScrollPane2.setHorizontalScrollBar(null);
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setPreferredSize(new java.awt.Dimension(796, 380));
        jScrollPane2.setRequestFocusEnabled(false);

        parameterTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null,  new Boolean(true), null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Obj Inst Id", "name", "description", "rawType", "rawUnit", "generationEnabled", "updateInterval"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Float.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        parameterTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        parameterTable.setAutoscrolls(false);
        parameterTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        parameterTable.setMaximumSize(null);
        parameterTable.setMinimumSize(null);
        parameterTable.setName("parameterTable"); // NOI18N
        parameterTable.setPreferredSize(null);

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, parameterTable, org.jdesktop.beansbinding.ObjectProperty.create(), parameterTable, org.jdesktop.beansbinding.BeanProperty.create("elements"));
        bindingGroup.addBinding(binding);
        binding.bind();
        parameterTable.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                parameterTableComponentAdded(evt);
            }
        });
        jScrollPane2.setViewportView(parameterTable);
        if (parameterTable.getColumnModel().getColumnCount() > 0) {
            parameterTable.getColumnModel().getColumn(0).setHeaderValue("Obj Inst Id");
            parameterTable.getColumnModel().getColumn(1).setHeaderValue("name");
            parameterTable.getColumnModel().getColumn(2).setHeaderValue("description");
            parameterTable.getColumnModel().getColumn(3).setHeaderValue("rawType");
            parameterTable.getColumnModel().getColumn(4).setHeaderValue("rawUnit");
            parameterTable.getColumnModel().getColumn(5).setHeaderValue("generationEnabled");
            parameterTable.getColumnModel().getColumn(6).setHeaderValue("updateInterval");
        }

        jPanel2.add(jScrollPane2);

        parameterTab.add(jPanel2);

        jSeparator3.setName("jSeparator3"); // NOI18N
        jSeparator3.setPreferredSize(new java.awt.Dimension(700, 25));
        parameterTab.add(jSeparator3);

        jPanel1.setName("jPanel1"); // NOI18N

        getValueButton.setText("getValue");
        getValueButton.setName("getValueButton"); // NOI18N
        getValueButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getValueButtonActionPerformed(evt);
            }
        });
        jPanel1.add(getValueButton);

        enableDefinitionButton.setText("enableGeneration");
        enableDefinitionButton.setName("enableDefinitionButton"); // NOI18N
        enableDefinitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableDefinitionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(enableDefinitionButton);

        addDefinitionButton.setText("addDefinition");
        addDefinitionButton.setName("addDefinitionButton"); // NOI18N
        addDefinitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDefinitionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(addDefinitionButton);

        updateDefinitionButton.setText("updateDefinition");
        updateDefinitionButton.setName("updateDefinitionButton"); // NOI18N
        updateDefinitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateDefinitionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(updateDefinitionButton);

        removeDefinitionButton.setText("removeDefinition");
        removeDefinitionButton.setName("removeDefinitionButton"); // NOI18N
        removeDefinitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefinitionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(removeDefinitionButton);

        parameterTab.add(jPanel1);

        jPanel5.setName("jPanel5"); // NOI18N

        getValueAllButton.setText("getValue(0)");
        getValueAllButton.setName("getValueAllButton"); // NOI18N
        getValueAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getValueAllButtonActionPerformed(evt);
            }
        });
        jPanel5.add(getValueAllButton);

        enableAllDefinition.setText("enableGeneration(group=false, 0)");
        enableAllDefinition.setName("enableAllDefinition"); // NOI18N
        enableAllDefinition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableAllDefinitionActionPerformed(evt);
            }
        });
        jPanel5.add(enableAllDefinition);

        listDefinitionAllButton.setText("listDefinition(\"*\")");
        listDefinitionAllButton.setName("listDefinitionAllButton"); // NOI18N
        listDefinitionAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listDefinitionAllButtonActionPerformed(evt);
            }
        });
        jPanel5.add(listDefinitionAllButton);

        removeDefinitionAllButton.setText("removeDefinition(0)");
        removeDefinitionAllButton.setName("removeDefinitionAllButton"); // NOI18N
        removeDefinitionAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefinitionAllButtonActionPerformed(evt);
            }
        });
        jPanel5.add(removeDefinitionAllButton);

        parameterTab.add(jPanel5);

        tabs.addTab("Parameter Service", parameterTab);

        ObjIdslotsTab.setEnabled(false);
        ObjIdslotsTab.setName("ObjIdslotsTab"); // NOI18N
        ObjIdslotsTab.setPreferredSize(new java.awt.Dimension(800, 600));
        ObjIdslotsTab.setLayout(new java.awt.GridLayout(32, 16, 1, 1));
        tabs.addTab("Published Parameter Values", ObjIdslotsTab);

        aggregationTab.setName("aggregationTab"); // NOI18N

        jPanel6.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setPreferredSize(new java.awt.Dimension(798, 350));

        jScrollPane3.setHorizontalScrollBar(null);
        jScrollPane3.setName("jScrollPane3"); // NOI18N
        jScrollPane3.setPreferredSize(new java.awt.Dimension(796, 380));
        jScrollPane3.setRequestFocusEnabled(false);

        aggregationTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null,  new Boolean(true), null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Obj Inst Id", "name", "description", "category", "generationEnabled", "updateInterval", "filterEnabled", "filteredTimeout"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Float.class, java.lang.Object.class, java.lang.Float.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        aggregationTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        aggregationTable.setAutoscrolls(false);
        aggregationTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        aggregationTable.setMaximumSize(null);
        aggregationTable.setMinimumSize(null);
        aggregationTable.setName("aggregationTable"); // NOI18N
        aggregationTable.setPreferredSize(null);
        aggregationTable.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                aggregationTableComponentAdded(evt);
            }
        });
        jScrollPane3.setViewportView(aggregationTable);

        jPanel6.add(jScrollPane3);

        aggregationTab.add(jPanel6);

        jSeparator5.setName("jSeparator5"); // NOI18N
        jSeparator5.setPreferredSize(new java.awt.Dimension(700, 5));
        aggregationTab.add(jSeparator5);

        jPanel7.setName("jPanel7"); // NOI18N

        getValueButtonAgg.setText("getValue");
        getValueButtonAgg.setName("getValueButtonAgg"); // NOI18N
        getValueButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getValueButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(getValueButtonAgg);

        enableDefinitionButtonAgg.setText("enableGeneration");
        enableDefinitionButtonAgg.setName("enableDefinitionButtonAgg"); // NOI18N
        enableDefinitionButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableDefinitionButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(enableDefinitionButtonAgg);

        enableFilterButtonAgg.setText("enableFilter");
        enableFilterButtonAgg.setName("enableFilterButtonAgg"); // NOI18N
        enableFilterButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableFilterButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(enableFilterButtonAgg);

        addDefinitionButtonAgg.setText("addDefinition");
        addDefinitionButtonAgg.setName("addDefinitionButtonAgg"); // NOI18N
        addDefinitionButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDefinitionButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(addDefinitionButtonAgg);

        updateDefinitionButtonAgg.setText("updateDefinition");
        updateDefinitionButtonAgg.setName("updateDefinitionButtonAgg"); // NOI18N
        updateDefinitionButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateDefinitionButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(updateDefinitionButtonAgg);

        removeDefinitionButtonAgg.setText("removeDefinition");
        removeDefinitionButtonAgg.setName("removeDefinitionButtonAgg"); // NOI18N
        removeDefinitionButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefinitionButtonAggActionPerformed(evt);
            }
        });
        jPanel7.add(removeDefinitionButtonAgg);

        aggregationTab.add(jPanel7);

        jPanel8.setName("jPanel8"); // NOI18N

        getValueAllButtonAgg.setText("getValue(0)");
        getValueAllButtonAgg.setName("getValueAllButtonAgg"); // NOI18N
        getValueAllButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getValueAllButtonAggActionPerformed(evt);
            }
        });
        jPanel8.add(getValueAllButtonAgg);

        enableDefinitionAllAgg.setText("enableGeneration(group=false, 0)");
        enableDefinitionAllAgg.setName("enableDefinitionAllAgg"); // NOI18N
        enableDefinitionAllAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableDefinitionAllAggActionPerformed(evt);
            }
        });
        jPanel8.add(enableDefinitionAllAgg);

        enableFilterAllAgg.setText("enableFilter(group=false, 0)");
        enableFilterAllAgg.setName("enableFilterAllAgg"); // NOI18N
        enableFilterAllAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableFilterAllAggActionPerformed(evt);
            }
        });
        jPanel8.add(enableFilterAllAgg);

        aggregationTab.add(jPanel8);

        jPanel11.setName("jPanel11"); // NOI18N

        listDefinitionAllButtonAgg.setText("listDefinition(\"*\")");
        listDefinitionAllButtonAgg.setName("listDefinitionAllButtonAgg"); // NOI18N
        listDefinitionAllButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listDefinitionAllButtonAggActionPerformed(evt);
            }
        });
        jPanel11.add(listDefinitionAllButtonAgg);

        removeDefinitionAllButtonAgg.setText("removeDefinition(0)");
        removeDefinitionAllButtonAgg.setName("removeDefinitionAllButtonAgg"); // NOI18N
        removeDefinitionAllButtonAgg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefinitionAllButtonAggActionPerformed(evt);
            }
        });
        jPanel11.add(removeDefinitionAllButtonAgg);

        msgBoxOn.setText("Display Published AggregationValues");
        msgBoxOn.setName("msgBoxOn"); // NOI18N
        msgBoxOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                msgBoxOnActionPerformed(evt);
            }
        });
        jPanel11.add(msgBoxOn);

        aggregationTab.add(jPanel11);

        tabs.addTab("Aggregation Service", aggregationTab);

        exampleTab.setName("exampleTab"); // NOI18N

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel24.setText("Aggregation name: Map");
        jLabel24.setName("jLabel24"); // NOI18N
        exampleTab.add(jLabel24);

        jSeparator11.setName("jSeparator11"); // NOI18N
        jSeparator11.setPreferredSize(new java.awt.Dimension(700, 15));
        exampleTab.add(jSeparator11);

        pictureLabel.setName("pictureLabel"); // NOI18N
        exampleTab.add(pictureLabel);

        tabs.addTab("Aggregation: Map", exampleTab);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        jLabel1.setText("Average Delay:");
        jLabel1.setName("jLabel1"); // NOI18N
        jToolBar1.add(jLabel1);

        delayLabel.setText("0.0");
        delayLabel.setName("delayLabel"); // NOI18N
        jToolBar1.add(delayLabel);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);

        jMenuBar1.setName("jMenuBar1"); // NOI18N

        jMenu1.setText("File");
        jMenu1.setName("jMenu1"); // NOI18N

        quitMenuItem.setText("Quit");
        quitMenuItem.setName("quitMenuItem"); // NOI18N
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(quitMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Consumer");
        jMenu2.setName("jMenu2"); // NOI18N

        jMenu3.setText("Register");
        jMenu3.setName("jMenu3"); // NOI18N

        subscriptionButtonGroup.add(regWildcardRadioButtonMenuItem);
        regWildcardRadioButtonMenuItem.setSelected(true);
        regWildcardRadioButtonMenuItem.setText("Wildcard");
        regWildcardRadioButtonMenuItem.setName("regWildcardRadioButtonMenuItem"); // NOI18N
        regWildcardRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regWildcardRadioButtonMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(regWildcardRadioButtonMenuItem);

        subscriptionButtonGroup.add(regHalfRadioButtonMenuItem);
        regHalfRadioButtonMenuItem.setText("Half");
        regHalfRadioButtonMenuItem.setName("regHalfRadioButtonMenuItem"); // NOI18N
        regHalfRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regHalfRadioButtonMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(regHalfRadioButtonMenuItem);

        subscriptionButtonGroup.add(regAllRadioButtonMenuItem);
        regAllRadioButtonMenuItem.setText("All");
        regAllRadioButtonMenuItem.setName("regAllRadioButtonMenuItem"); // NOI18N
        regAllRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regAllRadioButtonMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(regAllRadioButtonMenuItem);

        jMenu2.add(jMenu3);

        deregMenuItem.setText("Deregister");
        deregMenuItem.setName("deregMenuItem"); // NOI18N
        deregMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deregMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(deregMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jMenu2.add(jSeparator1);

        jMenuItem1.setText("Reconnect");
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void save2File(){
        Registers reg = new Registers();
        reg.save(this);
    }
    
    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_quitMenuItemActionPerformed
    {//GEN-HEADEREND:event_quitMenuItemActionPerformed
      try
      {
        running = false;
        this.save2File();

        if (null != tmConsumer && null != tmConsumer1)
        {
          deregMenuItemActionPerformed(null);
          tmConsumer.close(); 
          tmConsumer1.close();
        }
        if (null != consumerMgr)
          consumerMgr.close();

        if (null != mal)
          mal.close();

      }
      catch (MALException ex)
      {
        LOGGER.log(Level.SEVERE, "Exception during close down of the consumer {0}", ex);
      }

      dispose();
    }//GEN-LAST:event_quitMenuItemActionPerformed

    private void removeDefinitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefinitionButtonActionPerformed
        
        try
        {
        if (parameterTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        LOGGER.info("removeDefinition started");
        
        Long objId = new Long(parameterTable.getValueAt(parameterTable.getSelectedRow(), 0).toString());
        LongList longlist = new LongList();
        longlist.add(objId);
      
        parameterService.removeDefinition(longlist);
        LOGGER.info("removeDefinition executed");
        
        for (Long longlist1 : longlist) {
            final int slot = longlist1.intValue();
            final int index = (int) ((5*numberOfColumns)*Math.floor (slot/(5)) + slot%numberOfColumns);
            labels[index + 0*numberOfColumns].setRed();
            labels[index + 1*numberOfColumns].setRed();
            labels[index + 2*numberOfColumns].setRed();
            labels[index + 3*numberOfColumns].setRed();
        }
        
        parameterTableData.removeRow(parameterTable.getSelectedRow());
        this.save2File();

        }
        catch (MALException | MALInteractionException ex){
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();
    }//GEN-LAST:event_removeDefinitionButtonActionPerformed

    private void enableDefinitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableDefinitionButtonActionPerformed
        if (parameterTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        try
        {
        LOGGER.info("enableGeneration started");
        
        Long objId = new Long(parameterTable.getValueAt(parameterTable.getSelectedRow(), 0).toString());
        String str = parameterTable.getValueAt(parameterTable.getSelectedRow(), 5).toString();
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( objId, !curState) ); 
      
        parameterService.enableGeneration(false, BoolPairList);
        LOGGER.info("enableGeneration executed");
        
        parameterTable.setValueAt(!curState, parameterTable.getSelectedRow(), 5);
        this.save2File();
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_enableDefinitionButtonActionPerformed

    private void listDefinitionAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listDefinitionAllButtonActionPerformed
      try {
      LOGGER.info("listDefinition(\"*\") started");
      IdentifierList IdList = new IdentifierList();
      IdList.add(new Identifier ("*"));

      LongList output = parameterService.listDefinition(IdList);

      String str = "Object instance identifiers on the provider: \n";
          for (Long output1 : output)
              str += output1.toString() + "\n";

      JOptionPane.showMessageDialog(null, str, "Returned List from the Provider", JOptionPane.PLAIN_MESSAGE);
      LOGGER.log(Level.INFO, "listDefinition(\"*\") returned {0} object instance identifiers", output.size());
          
      } catch (MALInteractionException | MALException ex) {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
      }
    }//GEN-LAST:event_listDefinitionAllButtonActionPerformed

    private void addDefinitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDefinitionButtonActionPerformed
        titleEditParameter.setText("Add a new Parameter Definition");
        nameTF.setText("");
        descriptionTF.setText("");
        rawTypeCB.setSelectedIndex(5);  // Double
        rawUnitTF.setText("");
        updateIntervalTF.setText("");
        generationEnabledCB.setSelected(false);
        generationEnabledCB.setEnabled(false);
        validityExpressionCB.setSelected(false);
        validityExpressionCBActionPerformed(null);
        
        refreshParametersComboBox();
        isAddDef = true;
        editParameter.setVisible(true);
        
    }//GEN-LAST:event_addDefinitionButtonActionPerformed

  @SuppressWarnings("unchecked")
    private void refreshParametersComboBox(){
        validity1.removeAllItems();
        parameterCB1.removeAllItems();
        for (int i = 0; i < parameterTableData.getRowCount(); i++ ){
            validity1.addItem(parameterTableData.getValueAt(i, 1));
            parameterCB1.addItem(parameterTableData.getValueAt(i, 1));
        }

    }
    
    private void generationEnabledCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generationEnabledCBActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_generationEnabledCBActionPerformed

    
    
    private void submitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed

        if (nameTF.getText().equals("") ||
            descriptionTF.getText().equals("") ||
            descriptionTF.getText().equals("") ||
            rawTypeCB.getSelectedIndex() == 0 ||
            rawUnitTF.getText().equals("")     ||
            updateIntervalTF.getText().equals("")  ){
                JOptionPane.showMessageDialog(null, "Please fill-in all the necessary fields!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;
        }
        
        try{   Double.parseDouble(updateIntervalTF.getText());  // Check if it is a number
        }catch(NumberFormatException nfe)   {  
            JOptionPane.showMessageDialog(null, "updateInterval is not a number!", "Warning!", JOptionPane.PLAIN_MESSAGE);
            return;  
        }  

            ParameterExpression PExp;
        if (validityExpressionCB.isSelected() ){
            if (validity2.getSelectedIndex() != -1 && !validity3.getText().equals("")){
                Long instIs = new Long(parameterTableData.getValueAt(validity1.getSelectedIndex(), 0).toString());
                PExp = makeNewParameterExpression( instIs, validity2.getSelectedIndex(), validity4.isSelected() , validity3.getText() );
            }else{
                JOptionPane.showMessageDialog(null, "Please select an operator and a value!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;
            }
        }else{
            PExp = null;
        }

            
        ParameterDefinitionDetails Pdef;
        Pdef = makeNewParameterDefinition(nameTF.getText(),
                rawTypeCB.getSelectedIndex(), 
                rawUnitTF.getText(), 
                descriptionTF.getText(), 
                generationEnabledCB.isSelected(), 
                Float.parseFloat(updateIntervalTF.getText()),
                PExp,
                null );
        
        ParameterDefinitionDetailsList PDefs = new ParameterDefinitionDetailsList();
        PDefs.add(Pdef);

        editParameter.setVisible(false);
             
    try
    {
        if (isAddDef){  // Are we adding a new definition?
          LOGGER.info("addDefinition started");
          LongList output = parameterService.addDefinition(PDefs);
          LOGGER.log(Level.INFO, "addDefinition returned {0} object instance identifiers", output.size());
          parameterTableData.addRow(
            new Object [] {output.get(0).intValue(), Pdef.getName(), Pdef.getDescription(), 
                rawTypeCB.getItemAt(Pdef.getRawType()).toString(), Pdef.getRawUnit(), Pdef.getGenerationEnabled(), Pdef.getUpdateInterval().getValue()}
            );
        }else{  // Well, then we are updating a previous selected definition
          LOGGER.info("updateDefinition started");
          LongList objIds = new LongList();
          objIds.add(new Long(parameterTableData.getValueAt(parameterDefinitionSelectedIndex, 0).toString()));
          parameterService.updateDefinition(objIds, PDefs);  // Execute the update
          parameterTableData.removeRow(parameterDefinitionSelectedIndex);
          parameterTableData.insertRow(parameterDefinitionSelectedIndex, 
            new Object [] {objIds.get(0).intValue(), Pdef.getName(), Pdef.getDescription(), 
                rawTypeCB.getItemAt(Pdef.getRawType()).toString(), Pdef.getRawUnit(), Pdef.getGenerationEnabled(), Pdef.getUpdateInterval().getValue()}
            );
          LOGGER.info("updateDefinition executed");
        }
    }
    catch (MALException | MALInteractionException ex)
    {
      Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
    }
        
    this.save2File();
    }//GEN-LAST:event_submitButtonActionPerformed

    private void updateDefinitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateDefinitionButtonActionPerformed

    if (parameterTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        titleEditParameter.setText("Update Parameter Definition");
        parameterDefinitionSelectedIndex = parameterTable.getSelectedRow();

        nameTF.setText          (parameterTable.getValueAt(parameterDefinitionSelectedIndex, 1).toString());
        descriptionTF.setText   (parameterTable.getValueAt(parameterDefinitionSelectedIndex, 2).toString());
        rawTypeCB.setSelectedItem(parameterTable.getValueAt(parameterDefinitionSelectedIndex, 3).toString());
        
        rawUnitTF.setText       (parameterTable.getValueAt(parameterDefinitionSelectedIndex, 4).toString());
        updateIntervalTF.setText(parameterTable.getValueAt(parameterDefinitionSelectedIndex, 6).toString());
        
        validityExpressionCB.setSelected(false);
        validityExpressionCBActionPerformed(null);

        String str = parameterTable.getValueAt(parameterTable.getSelectedRow(), 5).toString();
        Boolean curState = (str.equals("true")); // String to Boolean conversion

        refreshParametersComboBox();

        generationEnabledCB.setSelected(curState);
        generationEnabledCB.setEnabled(true);

        isAddDef = false;
        editParameter.setVisible(true);
        
    }//GEN-LAST:event_updateDefinitionButtonActionPerformed

    private void updateIntervalTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateIntervalTFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_updateIntervalTFActionPerformed

    private void parameterTableComponentAdded(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_parameterTableComponentAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_parameterTableComponentAdded

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        this.jMenuItem1ActionPerformed(null);
    }//GEN-LAST:event_connectButtonActionPerformed

    private void enableAllDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableAllDefinitionActionPerformed
        try
        {
        LOGGER.info("enableGeneration(0) started");
        
        String str;
        if (parameterTable.getSelectedRow() == -1){  // Used to avoid problems if no row is selected
            if (parameterTable.getRowCount() != 0){
                str = parameterTable.getValueAt(0, 5).toString(); // Get the status from selection
            }else{
                str = "true";
            }
        }else{
            str = parameterTable.getValueAt(parameterTable.getSelectedRow(), 5).toString(); // Get the status from selection
        }
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( (long) 0, !curState) );  // Zero is the wildcard
      
        parameterService.enableGeneration(false, BoolPairList);  // false: no group service
        LOGGER.info("enableGeneration(0) executed");
        
        for (int i = 0; i < parameterTable.getRowCount(); i++)
            parameterTable.setValueAt(!curState, i, 5);
        
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();

    }//GEN-LAST:event_enableAllDefinitionActionPerformed

    private void removeDefinitionAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefinitionAllButtonActionPerformed
      try
        {
        LOGGER.info("removeDefinition(0) started");
        
        Long objId = new Long(0);
        LongList longlist = new LongList();
        longlist.add(objId);
      
        parameterService.removeDefinition(longlist);
        LOGGER.info("removeDefinition(0) executed");
        
        for (int i=0; i<longlist.size(); i++) // Set the slots to red
            labels[longlist.get(i).intValue()].setRed();

        while (parameterTableData.getRowCount() != 0)
            parameterTableData.removeRow(parameterTableData.getRowCount() - 1);
        
        }
      
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

      this.save2File();

    }//GEN-LAST:event_removeDefinitionAllButtonActionPerformed

    private void getValueAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getValueAllButtonActionPerformed
      try
        {
        LOGGER.info("getValue(0) started");
        
        Long objId = (long) 0;
        LongList longlist = new LongList();
        longlist.add(objId);
      
        org.ccsds.moims.mo.mc.parameter.body.GetValueResponse value = parameterService.getValue(longlist);
        LOGGER.info("getValue(0) executed");
        
        String str = "";
        for(int i=0; i<value.getBodyElement0().size(); i++){
            str += "The value for objId " + value.getBodyElement0().get(i).toString() + " is:" + "\n" + value.getBodyElement1().get(i).toString() + "\n";
        }
            
        JOptionPane.showMessageDialog(null, str, "Returned List from the Provider", JOptionPane.PLAIN_MESSAGE);
        
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

      this.save2File();
    }//GEN-LAST:event_getValueAllButtonActionPerformed

    private void getValueButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getValueButtonActionPerformed
      try
        {
        if (parameterTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        LOGGER.info("getValue started");
        
        Long objId = new Long(parameterTable.getValueAt(parameterTable.getSelectedRow(), 0).toString());
        LongList longlist = new LongList();
        longlist.add(objId);
      
        org.ccsds.moims.mo.mc.parameter.body.GetValueResponse value = parameterService.getValue(longlist);
        LOGGER.info("getValue executed");
        
        String str = "";
        for(int i=0; i<value.getBodyElement0().size(); i++){
            str += "The value for objId " + value.getBodyElement0().get(i).toString() + " is:" + "\n" + value.getBodyElement1().get(i).toString() + "\n";
        }
            
        JOptionPane.showMessageDialog(null, str, "Returned Values from the Provider", JOptionPane.PLAIN_MESSAGE);
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();
    }//GEN-LAST:event_getValueButtonActionPerformed

    private void aggregationTableComponentAdded(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_aggregationTableComponentAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_aggregationTableComponentAdded

    private void getValueButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getValueButtonAggActionPerformed
      try{
        if (aggregationTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        LOGGER.info("getValue started (Aggregation)");
        
        Long objId = new Long(aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 0).toString());
        LongList longlist = new LongList();
        longlist.add(objId);
      
        org.ccsds.moims.mo.mc.aggregation.body.GetValueResponse value = aggregationService.getValue(longlist);
        LOGGER.info("getValue executed (Aggregation)");
        
        String str = "";
        for(int h=0; h<value.getBodyElement1().size(); h++){
            str += "The value for objId " + value.getBodyElement0().get(h).toString() + " (AggregationValue index: " + h + ") is:" + "\n";
            for(int i=0; i<value.getBodyElement1().get(h).getParameterSetValues().size(); i++){
                for(int j=0; j<value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().size(); j++){
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j) == null)
                        continue;
                    str += "(parameterSetValue index: " + i + ") " + "validityState: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getInvalidSubState().toString() + "\n";
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getRawValue() != null)
                        str += "(parameterSetValue index: " + i + ") " + "rawValue: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getRawValue().toString() + "\n";
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getConvertedValue() != null)
                        str += "(parameterSetValue index: " + i + ") " + "convertedValue: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getConvertedValue().toString() + "\n";
                    str += "\n";
                }
            }
            str += "---------------------------------------\n";
        }       
        
        JOptionPane.showMessageDialog(null, str, "Returned Values from the Provider", JOptionPane.PLAIN_MESSAGE);
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();
    }//GEN-LAST:event_getValueButtonAggActionPerformed

    private void enableDefinitionButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableDefinitionButtonAggActionPerformed
        if (aggregationTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        try
        {
        LOGGER.info("enableGeneration started (Aggregation)");
        
        Long objId = new Long(aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 0).toString());
        String str = aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 4).toString();
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( objId, !curState) ); 
      
        aggregationService.enableGeneration(false, BoolPairList);
        LOGGER.info("enableGeneration executed (Aggregation)");
        
        aggregationTable.setValueAt(!curState, aggregationTable.getSelectedRow(), 4);
        this.save2File();
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_enableDefinitionButtonAggActionPerformed

    private void addDefinitionButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDefinitionButtonAggActionPerformed
        titleEditParameter1.setText("Add a new Aggregation Definition");
        nameTF1.setText("");
        descriptionTF1.setText("");
        categoryCB1.setSelectedIndex(0);  // Default dash

        updateIntervalTF1.setText("");
        filteredTimeoutTF1.setText("");
        generationEnabledCB1.setSelected(false);
        generationEnabledCB1.setEnabled(false);
        filterEnabledCB1.setSelected(false);
        
        thresholdTypeCB1.setSelectedIndex(0);
        thresholdValueTB1.setText("");
        
        parameterSetsTableData = new DefaultTableModel();
        parameterSetsTableData.setColumnIdentifiers(new Vector<String>(Arrays.asList(parameterSetsTableCol)));
        parameterSetsTable.setModel(parameterSetsTableData);

        refreshParametersComboBox();
        isAddDef = true;
        editAggregation.setVisible(true);
        
    }//GEN-LAST:event_addDefinitionButtonAggActionPerformed

    private void updateDefinitionButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateDefinitionButtonAggActionPerformed
    if (aggregationTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        titleEditParameter1.setText("Update Aggregation Definition");
        aggregationDefinitionSelectedIndex = aggregationTable.getSelectedRow();

        nameTF1.setText            (aggregationTable.getValueAt(aggregationDefinitionSelectedIndex, 1).toString());
        descriptionTF1.setText     (aggregationTable.getValueAt(aggregationDefinitionSelectedIndex, 2).toString());
        categoryCB1.setSelectedItem(aggregationTable.getValueAt(aggregationDefinitionSelectedIndex, 3).toString());
        
        updateIntervalTF1.setText  (aggregationTable.getValueAt(aggregationDefinitionSelectedIndex, 5).toString());
        filteredTimeoutTF1.setText (aggregationTable.getValueAt(aggregationDefinitionSelectedIndex, 7).toString());
        
        filterEnabledCB1.setSelected(false);
        validityExpressionCBActionPerformed(null);

        Boolean curState = (aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 4).toString().equals("true")); // String to Boolean conversion
        generationEnabledCB1.setSelected(curState);
        generationEnabledCB1.setEnabled(true);

        curState = (aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 6).toString().equals("true")); // String to Boolean conversion
        filterEnabledCB1.setSelected(curState);

        parameterSetsTableData = new DefaultTableModel();
        parameterSetsTableData.setDataVector(parameterSetsTableDataAll.get(aggregationDefinitionSelectedIndex).getDataVector(),
                new Vector<String>(Arrays.asList(parameterSetsTableCol)));
        parameterSetsTable.setModel(parameterSetsTableData);
        
        refreshParametersComboBox();

        isAddDef = false;
        editAggregation.setVisible(true);
    }//GEN-LAST:event_updateDefinitionButtonAggActionPerformed

    private void removeDefinitionButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefinitionButtonAggActionPerformed
        try{
        if (aggregationTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        LOGGER.info("removeDefinition started (Aggregation)");
        
        Long objId = new Long(aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 0).toString());
        LongList longlist = new LongList();
        longlist.add(objId);
      
        aggregationService.removeDefinition(longlist);
        LOGGER.info("removeDefinition executed (Aggregation)");

        parameterSetsTableDataAll.remove(aggregationTable.getSelectedRow());
        aggregationTableData.removeRow(aggregationTable.getSelectedRow());
        this.save2File();

        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();

    }//GEN-LAST:event_removeDefinitionButtonAggActionPerformed

    private void getValueAllButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getValueAllButtonAggActionPerformed
      try
        {
        LOGGER.info("getValue(0) started (Aggregation)");
        
        Long objId = (long) 0;
        LongList longlist = new LongList();
        longlist.add(objId);
            
        org.ccsds.moims.mo.mc.aggregation.body.GetValueResponse value = aggregationService.getValue(longlist);
        LOGGER.info("getValue(0) executed (Aggregation)");
        
        String str = "";
        for(int h=0; h<value.getBodyElement1().size(); h++){
            str += "The value for objId " + value.getBodyElement0().get(h).toString() + " (AggregationValue index: " + h + ") is:" + "\n";
            for(int i=0; i<value.getBodyElement1().get(h).getParameterSetValues().size(); i++){
                for(int j=0; j<value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().size(); j++){
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j) == null)
                        continue;
                    str += "(parameterSetValue index: " + i + ") " + "validityState: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getInvalidSubState().toString() + "\n";
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getRawValue() != null)
                        str += "(parameterSetValue index: " + i + ") " + "rawValue: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getRawValue().toString() + "\n";
                    if (value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getConvertedValue() != null)
                        str += "(parameterSetValue index: " + i + ") " + "convertedValue: " + value.getBodyElement1().get(h).getParameterSetValues().get(i).getValues().get(j).getConvertedValue().toString() + "\n";
                    str += "\n";
                }
            }
            str += "---------------------------------------\n";
        }
            
        JOptionPane.showMessageDialog(null, str, "Returned List from the Provider", JOptionPane.PLAIN_MESSAGE);
        
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

      this.save2File();
    }//GEN-LAST:event_getValueAllButtonAggActionPerformed

    private void enableDefinitionAllAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableDefinitionAllAggActionPerformed
        try
        {
        LOGGER.info("enableGeneration(0) started (Aggregation)");
        
        String str;
        if (aggregationTable.getSelectedRow() == -1){  // Used to avoid problems if no row is selected
            if (aggregationTable.getRowCount() != 0){
                str = aggregationTable.getValueAt(0, 4).toString(); // Get the status from selection
            }else{
                str = "true";
            }
        }else{
            str = aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 4).toString(); // Get the status from selection
        }
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( (long) 0, !curState) );  // Zero is the wildcard
      
        aggregationService.enableGeneration(false, BoolPairList);  // false: no group service
        LOGGER.info("enableGeneration(0) executed (Aggregation)");
        
        for (int i = 0; i < aggregationTable.getRowCount(); i++)
            aggregationTable.setValueAt(!curState, i, 4);
        
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();

    }//GEN-LAST:event_enableDefinitionAllAggActionPerformed

    private void listDefinitionAllButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listDefinitionAllButtonAggActionPerformed
      try {
      LOGGER.info("listDefinition(\"*\") started (Aggregation)");
      IdentifierList IdList = new IdentifierList();
      IdList.add(new Identifier ("*"));

      LongList output =  aggregationService.listDefinition(IdList);

      String str = "Object instance identifiers on the provider: \n";
          for (Long output1 : output) {
              str += output1.toString() + "\n";
          }

      JOptionPane.showMessageDialog(null, str, "Returned List from the Provider", JOptionPane.PLAIN_MESSAGE);
      LOGGER.log(Level.INFO, "listDefinition(\"*\") returned {0} object instance identifiers (Aggregation)", output.size());
          
      } catch (MALInteractionException | MALException ex) {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
      }

    }//GEN-LAST:event_listDefinitionAllButtonAggActionPerformed

    private void removeDefinitionAllButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefinitionAllButtonAggActionPerformed
      try
        {
        LOGGER.info("removeDefinition(0) started (Aggregation)");
        
        Long objId = new Long(0);
        LongList longlist = new LongList();
        longlist.add(objId);
      
        aggregationService.removeDefinition(longlist);
        LOGGER.info("removeDefinition(0) executed (Aggregation)");
        
        while (aggregationTableData.getRowCount() != 0){
            parameterSetsTableDataAll.remove(aggregationTableData.getRowCount() - 1);
            aggregationTableData.removeRow(aggregationTableData.getRowCount() - 1);
        }

        }     
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

      this.save2File();
    }//GEN-LAST:event_removeDefinitionAllButtonAggActionPerformed

    private void enableFilterButtonAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableFilterButtonAggActionPerformed
        if (aggregationTable.getSelectedRow() == -1)  // The row is not selected?
            return;  // Well, then nothing to be done here folks!

        try{
        LOGGER.info("enableFilter started (Aggregation)");
        
        Long objId = new Long(aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 0).toString());
        String str = aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 6).toString();
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( objId, !curState) ); 
      
        aggregationService.enableFilter(false, BoolPairList);
        LOGGER.info("enableFilter executed (Aggregation)");
        
        aggregationTable.setValueAt(!curState, aggregationTable.getSelectedRow(), 6);
        this.save2File();
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_enableFilterButtonAggActionPerformed

    private void enableFilterAllAggActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableFilterAllAggActionPerformed
        try
        {
        LOGGER.info("enableFilter(0) started (Aggregation)");
        
        String str;
        if (aggregationTable.getSelectedRow() == -1){  // Used to avoid problems if no row is selected
            if (aggregationTable.getRowCount() != 0){
                str = aggregationTable.getValueAt(0, 4).toString(); // Get the status from selection
            }else{
                str = "true";
            }
        }else{
            str = aggregationTable.getValueAt(aggregationTable.getSelectedRow(), 6).toString(); // Get the status from selection
        }
        Boolean curState = (str.equals("true")); // String to Boolean conversion
        InstanceBooleanPairList BoolPairList = new InstanceBooleanPairList();
        BoolPairList.add(new InstanceBooleanPair( (long) 0, !curState) );  // Zero is the wildcard
      
        aggregationService.enableFilter(false, BoolPairList);  // false: no group service
        LOGGER.info("enableFilter(0) executed (Aggregation)");
        
        for (int i = 0; i < aggregationTable.getRowCount(); i++)
            aggregationTable.setValueAt(!curState, i, 6);
        
        }
        catch (MALException | MALInteractionException ex)
        {
          Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.save2File();
    }//GEN-LAST:event_enableFilterAllAggActionPerformed

    private void conversionCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conversionCBActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_conversionCBActionPerformed

    private void validity3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validity3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_validity3ActionPerformed

    private void validity4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validity4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_validity4ActionPerformed

    private void validity2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validity2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_validity2ActionPerformed

    private void validityExpressionCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validityExpressionCBActionPerformed

        validity1.setEnabled(validityExpressionCB.isSelected());
        validity2.setEnabled(validityExpressionCB.isSelected());
        validity3.setEnabled(validityExpressionCB.isSelected());
        validity4.setEnabled(validityExpressionCB.isSelected());

    }//GEN-LAST:event_validityExpressionCBActionPerformed

    private void validity1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validity1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_validity1ActionPerformed

    private void uri1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uri1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_uri1ActionPerformed

    private void uri2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uri2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_uri2ActionPerformed

    private void uri3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uri3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_uri3ActionPerformed

    private void uri4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uri4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_uri4ActionPerformed

    private void updateIntervalTF1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateIntervalTF1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_updateIntervalTF1ActionPerformed

    private void generationEnabledCB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generationEnabledCB1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_generationEnabledCB1ActionPerformed

    private void parameterCB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parameterCB1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_parameterCB1ActionPerformed

    private void sampleIntervalTB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleIntervalTB1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sampleIntervalTB1ActionPerformed

    private void submitButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButton1ActionPerformed

        if (nameTF1.getText().equals("") ||
            descriptionTF1.getText().equals("") ||
            categoryCB1.getSelectedIndex() == 0 ||
            updateIntervalTF1.getText().equals("")     ||
            filteredTimeoutTF1.getText().equals("")  ){
                JOptionPane.showMessageDialog(null, "Please fill-in all the necessary fields!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;
        }
        Float updateInterval;
        Float filteredTimeout;
        try{   updateInterval = Float.parseFloat(updateIntervalTF1.getText());  // Check if it is a number
               filteredTimeout = Float.parseFloat(filteredTimeoutTF1.getText());  // Check if it is a number
        }catch(NumberFormatException nfe)   {  
            JOptionPane.showMessageDialog(null, "updateInterval or filteredTimeout is not a number!", "Warning!", JOptionPane.PLAIN_MESSAGE);
            return;  
        }  

        AggregationDefinitionDetails aDef;
        aDef = makeNewAggregationDefinition(nameTF1.getText(),
                descriptionTF1.getText(), 
                AggregationCategory.fromOrdinal(categoryCB1.getSelectedIndex()), 
                generationEnabledCB1.isSelected(), 
                updateInterval, 
                filterEnabledCB1.isSelected(), 
                filteredTimeout,
                makeNewAggregationParameterSetList() );
        
        AggregationDefinitionDetailsList aDefs = new AggregationDefinitionDetailsList();
        aDefs.add(aDef);
        editAggregation.setVisible(false);
             
    try
    {
        if (isAddDef){  // Are we adding a new definition?
          LOGGER.info("addDefinition started (Aggregation)");
          LongList output = aggregationService.addDefinition(aDefs);
          LOGGER.log(Level.INFO, "addDefinition returned {0} object instance identifiers (Aggregation)", output.size());
          aggregationTableData.addRow(
            new Object [] {output.get(0).intValue(), aDef.getName(), aDef.getDescription(), 
                categoryCB1.getItemAt(aDef.getCategory().getOrdinal()).toString(), aDef.getGenerationEnabled(),
                aDef.getUpdateInterval().getValue(), aDef.getFilterEnabled(), aDef.getFilteredTimeout().getValue() }
            );
          DefaultTableModel tmp = new DefaultTableModel();
          tmp.setDataVector(parameterSetsTableData.getDataVector(), new Vector<String>(Arrays.asList(parameterSetsTableCol)));
          parameterSetsTableDataAll.add(tmp);

        }else{  // Well, then we are updating a previous selected definition
          LOGGER.info("updateDefinition started (Aggregation)");
          LongList objIds = new LongList();
          objIds.add(new Long(aggregationTableData.getValueAt(aggregationDefinitionSelectedIndex, 0).toString()));
          aggregationService.updateDefinition(objIds, aDefs);  // Execute the update
          aggregationTableData.removeRow(aggregationDefinitionSelectedIndex);
          aggregationTableData.insertRow(aggregationDefinitionSelectedIndex, 
            new Object [] {objIds.get(0).intValue(), aDef.getName(), aDef.getDescription(),
                categoryCB1.getItemAt(aDef.getCategory().getOrdinal()).toString(),  aDef.getGenerationEnabled(),
                aDef.getUpdateInterval().getValue(), aDef.getFilterEnabled(), aDef.getFilteredTimeout().getValue() }
            );
          DefaultTableModel tmp = new DefaultTableModel();
          tmp.setDataVector(parameterSetsTableData.getDataVector(), new Vector<String>(Arrays.asList(parameterSetsTableCol)));
          parameterSetsTableDataAll.set(aggregationDefinitionSelectedIndex, tmp);
          LOGGER.info("updateDefinition executed (Aggregation)");
        }
    }
    catch (MALException | MALInteractionException ex)
    {
      Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
    }
        
    this.save2File();


    }//GEN-LAST:event_submitButton1ActionPerformed

    private void filterEnabledCB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterEnabledCB1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_filterEnabledCB1ActionPerformed

    private void thresholdValueTB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdValueTB1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_thresholdValueTB1ActionPerformed

    private void aggregateParameterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggregateParameterButtonActionPerformed
        
        if (sampleIntervalTB1.getText().equals("") ||
            parameterCB1.getSelectedIndex() == -1   ){
                JOptionPane.showMessageDialog(null, "Please fill-in all the necessary fields!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;
        }
        
        try{    Double.parseDouble(sampleIntervalTB1.getText());  // Check if it is a number
        }catch(NumberFormatException nfe)   {  
            JOptionPane.showMessageDialog(null, "sampleInterval is not a number!", "Warning!", JOptionPane.PLAIN_MESSAGE);
            return;  
        }  
        
        Double thresholdValue = null; 
        
        if ( thresholdTypeCB1.getSelectedIndex() != 0 ){
            if  ( thresholdValueTB1.getText().equals("") ){
                JOptionPane.showMessageDialog(null, "Please enter a thresholdValue!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;  
            }

            try{   thresholdValue = Double.parseDouble(thresholdValueTB1.getText());  // Check if it is a number
            }catch(NumberFormatException nfe)   {  
                JOptionPane.showMessageDialog(null, "thresholdValue is not a number!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                return;  
            }  
        }

        parameterSetsTableData.addRow(
            new Object [] { parameterCB1.getSelectedItem().toString() , Double.parseDouble(sampleIntervalTB1.getText()), 
                thresholdTypeCB1.getSelectedItem().toString() , thresholdValue  }
            );

    }//GEN-LAST:event_aggregateParameterButtonActionPerformed

    private void removeParameterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeParameterActionPerformed
        if (parameterSetsTable.getSelectedRow() != -1)  // Did we select a parameter?
            parameterSetsTableData.removeRow(parameterSetsTable.getSelectedRow());
    }//GEN-LAST:event_removeParameterActionPerformed

    private void msgBoxOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_msgBoxOnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_msgBoxOnActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try
        {
            deregMenuItemActionPerformed(null);
            this.delayManager.resetDelay();
            labels[0].reset();
            StructureHelper.clearLoadedPropertiesList();
            startService();
            registerSubscription();
        }
        catch (MALException | MalformedURLException ex)
        {
            Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void deregMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deregMenuItemActionPerformed
        try
        {
            final Identifier subscriptionId = new Identifier("SUB");
            final IdentifierList subLst = new IdentifierList();
            subLst.add(subscriptionId);
            //        if (parameterService.getConsumer().getTransmitErrorListener() != null)
            parameterService.monitorValueDeregister(subLst);
            //        if (aggregationService.getConsumer().getTransmitErrorListener() != null)
            aggregationService.monitorValueDeregister(subLst);
        }
        catch (MALException | MALInteractionException ex)
        {
            Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_deregMenuItemActionPerformed

    private void regAllRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regAllRadioButtonMenuItemActionPerformed
        try
        {
            parameterService.monitorValueRegister(subRequestAll, adapterParameter);
            aggregationService.monitorValueRegister(subRequestAll, adapterAggregation);
        }
        catch (MALException | MALInteractionException ex)
        {
            JOptionPane.showMessageDialog(null, "Could not connect to the provider!", "Error!", JOptionPane.PLAIN_MESSAGE);
            Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_regAllRadioButtonMenuItemActionPerformed

    private void regHalfRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regHalfRadioButtonMenuItemActionPerformed
        try
        {
            parameterService.monitorValueRegister(subRequestHalf, adapterParameter);
            aggregationService.monitorValueRegister(subRequestHalf, adapterAggregation);
        }
        catch (MALException | MALInteractionException ex)
        {
            JOptionPane.showMessageDialog(null, "Could not connect to the provider!", "Error!", JOptionPane.PLAIN_MESSAGE);
            Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_regHalfRadioButtonMenuItemActionPerformed

    private void regWildcardRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regWildcardRadioButtonMenuItemActionPerformed
        try
        {
            parameterService.monitorValueRegister(subRequestWildcard, adapterParameter);
            aggregationService.monitorValueRegister(subRequestWildcard, adapterAggregation);
        }
        catch (MALException | MALInteractionException ex)
        {
            JOptionPane.showMessageDialog(null, "Could not connect to the provider!", "Error!", JOptionPane.PLAIN_MESSAGE);
            Logger.getLogger(MityDemoConsumerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_regWildcardRadioButtonMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ObjIdslotsTab;
    private javax.swing.JButton addDefinitionButton;
    private javax.swing.JButton addDefinitionButtonAgg;
    private javax.swing.JButton aggregateParameterButton;
    private javax.swing.JPanel aggregationTab;
    private javax.swing.JTable aggregationTable;
    private javax.swing.JComboBox categoryCB1;
    private javax.swing.JButton connectButton;
    private javax.swing.JCheckBox conversionCB;
    private javax.swing.JLabel delayLabel;
    private javax.swing.JMenuItem deregMenuItem;
    private javax.swing.JTextField descriptionTF;
    private javax.swing.JTextField descriptionTF1;
    private javax.swing.JFrame editAggregation;
    private javax.swing.JFrame editParameter;
    private javax.swing.JButton enableAllDefinition;
    private javax.swing.JButton enableDefinitionAllAgg;
    private javax.swing.JButton enableDefinitionButton;
    private javax.swing.JButton enableDefinitionButtonAgg;
    private javax.swing.JButton enableFilterAllAgg;
    private javax.swing.JButton enableFilterButtonAgg;
    private javax.swing.JPanel exampleTab;
    private javax.swing.JCheckBox filterEnabledCB1;
    private javax.swing.JTextField filteredTimeoutTF1;
    private javax.swing.JCheckBox generationEnabledCB;
    private javax.swing.JCheckBox generationEnabledCB1;
    private javax.swing.JButton getValueAllButton;
    private javax.swing.JButton getValueAllButtonAgg;
    private javax.swing.JButton getValueButton;
    private javax.swing.JButton getValueButtonAgg;
    private javax.swing.JPanel homeTab;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator13;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton listDefinitionAllButton;
    private javax.swing.JButton listDefinitionAllButtonAgg;
    private javax.swing.JCheckBox msgBoxOn;
    private javax.swing.JTextField nameTF;
    private javax.swing.JTextField nameTF1;
    private javax.swing.JComboBox parameterCB1;
    private javax.swing.JTable parameterSetsTable;
    private javax.swing.JPanel parameterTab;
    private javax.swing.JTable parameterTable;
    private javax.swing.JLabel pictureLabel;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JComboBox rawTypeCB;
    private javax.swing.JTextField rawUnitTF;
    private javax.swing.JRadioButtonMenuItem regAllRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem regHalfRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem regWildcardRadioButtonMenuItem;
    private javax.swing.JButton removeDefinitionAllButton;
    private javax.swing.JButton removeDefinitionAllButtonAgg;
    private javax.swing.JButton removeDefinitionButton;
    private javax.swing.JButton removeDefinitionButtonAgg;
    private javax.swing.JButton removeParameter;
    private javax.swing.JTextField sampleIntervalTB1;
    private javax.swing.JButton submitButton;
    private javax.swing.JButton submitButton1;
    private javax.swing.ButtonGroup subscriptionButtonGroup;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JComboBox thresholdTypeCB1;
    private javax.swing.JTextField thresholdValueTB1;
    private javax.swing.JLabel titleEditParameter;
    private javax.swing.JLabel titleEditParameter1;
    private javax.swing.JButton updateDefinitionButton;
    private javax.swing.JButton updateDefinitionButtonAgg;
    private javax.swing.JTextField updateIntervalTF;
    private javax.swing.JTextField updateIntervalTF1;
    private javax.swing.JTextField uri1;
    private javax.swing.JTextField uri2;
    private javax.swing.JTextField uri3;
    private javax.swing.JTextField uri4;
    private javax.swing.JComboBox validity1;
    private javax.swing.JComboBox validity2;
    private javax.swing.JTextField validity3;
    private javax.swing.JCheckBox validity4;
    private javax.swing.JCheckBox validityExpressionCB;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
