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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.structures.InstanceBooleanPair;
import org.ccsds.moims.mo.com.structures.InstanceBooleanPairList;
import org.ccsds.moims.mo.com.structures.ObjectIdList;
import org.ccsds.moims.mo.mal.MALContext;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.provider.MALProvider;
import org.ccsds.moims.mo.mal.provider.MALProviderManager;
import org.ccsds.moims.mo.mal.provider.MALPublishInteractionListener;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.DurationList;
import org.ccsds.moims.mo.mal.structures.EntityKey;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UIntegerList;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UpdateHeader;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.structures.UpdateType;
import org.ccsds.moims.mo.mal.transport.MALErrorBody;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mc.MCHelper;
import org.ccsds.moims.mo.mc.aggregation.AggregationHelper;
import org.ccsds.moims.mo.mc.aggregation.body.GetValueResponse;
import org.ccsds.moims.mo.mc.aggregation.provider.AggregationInheritanceSkeleton;
import org.ccsds.moims.mo.mc.aggregation.provider.MonitorValuePublisher;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetails;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationDefinitionDetailsList;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValueList;
import org.ccsds.moims.mo.mc.aggregation.structures.GenerationMode;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterSet;
import org.ccsds.moims.mo.mc.aggregation.structures.AggregationValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValueList;


/**
 *
 */
public class AggregationProviderServiceImpl extends AggregationInheritanceSkeleton
{
  private IdentifierList domain;
  private MALContextFactory malFactory;
  private MALContext mal;
  private MALProviderManager providerMgr;
  private MALProvider aggregationServiceProvider;
  private boolean initialiased = false;
  private boolean running = false;
  private MonitorValuePublisher publisher;
  private boolean isRegistered = false;
  private AggregationManager manager;
  private PeriodicReportingManager periodicReportingManager;
  private PeriodicSamplingManager periodicSamplingManager;

  /**
   * creates the MAL objects, the publisher used to create updates and starts the publishing thread
   *
     * @param parameterManager
   * @throws MALException On initialisation error.
   */
  public synchronized void init(ParameterManager parameterManager) throws MALException
  {
    if (!initialiased)
    {
      malFactory = MALContextFactory.newFactory();
      mal = malFactory.createMALContext(System.getProperties());
      providerMgr = mal.createProviderManager();

      if (MALContextFactory.lookupArea(MALHelper.MAL_AREA_NAME, MALHelper.MAL_AREA_VERSION) == null)
        MALHelper.init(MALContextFactory.getElementFactoryRegistry());

      if (MALContextFactory.lookupArea(MCHelper.MC_AREA_NAME, MCHelper.MC_AREA_VERSION) == null)
        MCHelper.init(MALContextFactory.getElementFactoryRegistry());
      
      if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION) == null)
        COMHelper.init(MALContextFactory.getElementFactoryRegistry());
      
      AggregationHelper.init(MALContextFactory.getElementFactoryRegistry());

      domain = new IdentifierList();
      domain.add(new Identifier("esa"));
      domain.add(new Identifier("mission"));

      publisher = createMonitorValuePublisher(domain,
              new Identifier("GROUND"),
              SessionType.LIVE,
              new Identifier("LIVE"),
              QoSLevel.BESTEFFORT,
              null,
              new UInteger(0));

      startServices();

      running = true;
      
      manager = new AggregationManager(parameterManager);
      periodicReportingManager = new PeriodicReportingManager();
      periodicSamplingManager = new PeriodicSamplingManager();
      periodicReportingManager.init(); // Initialize the Periodic Reporting Manager
      periodicSamplingManager.init(); // Initialize the Periodic Sampling Manager
      
      manager.setGenerationEnabledAll(false);

      DemoProviderCli.LOGGER.info("Aggregation service READY");
      initialiased = true;
    }
  }

  /**
   * Closes any existing service providers and recreates them. Used to switch the transport used by the provider.
   *
   * @throws MALException On error.
   */
  public void startServices() throws MALException
  {
    // shut down old service transport
    if (null != aggregationServiceProvider)
    {
      aggregationServiceProvider.close();
    }

    // start transport
    URI sharedBrokerURI = null;
    if ((null != System.getProperty("demo.provider.useSharedBroker"))
            && (null != System.getProperty("shared.broker.uri")))
    {
      sharedBrokerURI = new URI(System.getProperty("shared.broker.uri"));
    }

    aggregationServiceProvider = providerMgr.createProvider("Aggregation",
            null,
            AggregationHelper.AGGREGATION_SERVICE,
            new Blob("".getBytes()),
            this,
            new QoSLevel[]
            {
              QoSLevel.ASSURED
            },
            new UInteger(1),
            System.getProperties(),
            true,
            sharedBrokerURI);

    DemoProviderCli.LOGGER.log(Level.INFO, "Aggregation Service URI       : {0}", aggregationServiceProvider.getURI());
    DemoProviderCli.LOGGER.log(Level.INFO, "Aggregation Service broker URI: {0}", aggregationServiceProvider.getBrokerURI());


    try (BufferedWriter wrt = new BufferedWriter(new FileWriter("demoServiceURI.properties", true)))
    {
      wrt.append("AggregationURI=" + aggregationServiceProvider.getURI());
      wrt.newLine();
      wrt.append("AggregationBroker=" + aggregationServiceProvider.getBrokerURI());
      wrt.newLine();
      wrt.close();
    }
    catch (IOException ex)
    {
      DemoProviderCli.LOGGER.log(Level.WARNING, "Unable to write URI information to properties file {0}", ex);
    }
  }

  /**
   * Closes all running threads and releases the MAL resources.
   */
  public void close()
  {
    try{
      running = false;

      if (null != aggregationServiceProvider)
        aggregationServiceProvider.close();

      if (null != providerMgr)
        providerMgr.close();

      if (null != mal)
        mal.close();

    }catch (MALException ex){
      DemoProviderCli.LOGGER.log(Level.WARNING, "Exception during close down of the provider {0}", ex);
    }
  }

  private void publishAggregationUpdate(final Long objId, final AggregationValue aVal)
  {
    try
    {
      if (!isRegistered)
      {
        final EntityKeyList lst = new EntityKeyList();
        lst.add(new EntityKey(new Identifier("*"), 0L, 0L, 0L));
        publisher.register(lst, new PublishInteractionListener());
        isRegistered = true;
      }

      DemoProviderCli.LOGGER.log(Level.FINE,
         "Generating Aggregation update for the Aggregation Definition objId: {0} (Identifier: {1})",
            new Object[] {
              objId, new Identifier (manager.get(objId).getName().toString())
            });

    // requirements: 3.7.5.2.1 , 3.7.5.2.2 , 3.7.5.2.3 , 3.7.5.2.4
    final EntityKey ekey = new EntityKey(new Identifier (manager.get(objId).getName().toString()), objId, manager.generateAValobjId(), null);
    final Time timestamp = new Time(System.currentTimeMillis()); //  requirement: 3.7.5.2.5

    final UpdateHeaderList hdrlst = new UpdateHeaderList();
    final ObjectIdList objectIdlst = new ObjectIdList();
    final AggregationValueList aValLst = new AggregationValueList();
            
    hdrlst.add(new UpdateHeader(timestamp, new URI("SomeURI"), UpdateType.UPDATE, ekey));
    objectIdlst.add(null); // requirement: 3.7.5.2.7 (3.7.5.2.6 not necessary)
    aValLst.add(aVal);
    
    publisher.publish(hdrlst, objectIdlst, aValLst); // requirement: 3.7.2.15

    }catch (IllegalArgumentException | MALException | MALInteractionException ex){
      ex.printStackTrace();
    }
  }

  @Override
  public GetValueResponse getValue(final LongList lLongList, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement 3.7.6.2.1
    
    LongList outLongLst = new LongList();
    UIntegerList unkIndexList = new UIntegerList();
    AggregationValueList outAValLst = new AggregationValueList();
    AggregationDefinitionDetails tempPDef;
    Long tempLong;

    if (null == lLongList) // Is the input null?
        throw new IllegalArgumentException("LongList argument must not be null");

    for (int index = 0; index < lLongList.size(); index++) { 
        tempLong = lLongList.get(index);

        if (tempLong == 0){  // Is it the wildcard '0'? requirement: 3.7.6.2.2
            outLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            outLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }

        tempPDef = manager.get(tempLong);

        if (tempPDef != null){ // Does the AggregationDefinition exist?
            outLongLst.add(tempLong); //yap
        }else{  // The requested aggregation is unknown
            unkIndexList.add(new UInteger(index)); // requirement: 3.7.6.2.3
        } 
    }

    outAValLst.addAll(manager.getAggregationValuesList(outLongLst, GenerationMode.ADHOC, false));  // requirement: 3.7.6.2.4

    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.7.6.2.3 (error: a, b)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );
    
                
    return new GetValueResponse(outLongLst, outAValLst);
  }

  @Override
  public void enableGeneration(final Boolean lBoolean, final InstanceBooleanPairList lInstanceBooleanPairList, 
          final MALInteraction interaction) throws MALException, MALInteractionException { // requirement: 3.7.7.2.1
    UIntegerList unkIndexList = new UIntegerList();
    UIntegerList invIndexList = new UIntegerList();
    InstanceBooleanPair tempBPair;
    
    if (null == lBoolean || null == lInstanceBooleanPairList) // Are the inputs null?
        throw new IllegalArgumentException("Boolean and InstanceBooleanPairList arguments must not be null");

    if (lBoolean)  // Are the objId group identifiers?
        throw new IllegalArgumentException("The MO M&C Group Service was not implemented. Group object instance identifiers cannot be used!");
            
    for (int index = 0; index < lInstanceBooleanPairList.size(); index++) {
        tempBPair = lInstanceBooleanPairList.get(index);

        if (tempBPair.getId() == 0){  // Is it the wildcard '0'? requirement: 3.7.7.2.4
            manager.setGenerationEnabledAll(tempBPair.getValue());
            periodicReportingManager.refreshAll();
            periodicSamplingManager.refreshAll();
            break;
        }

        if (lBoolean){
            // Insert code here to make the Group service work with the enableGeneration operation
        //    if (!groupService.exists(tempBPair.getId()))  // It does not exist? 
                invIndexList.add(new UInteger(index)); // requirement: 3.7.7.2.7 (incomplete: group service not available)

        }else{  // requirement: 3.7.7.2.8 (is respected because no error is generated if it is already enabled)
            manager.setGenerationEnabled(tempBPair.getId(), tempBPair.getValue()); // requirement: 3.7.7.2.5
            periodicReportingManager.refresh(tempBPair.getId());
            periodicSamplingManager.refresh(tempBPair.getId());

            if (!manager.exists(tempBPair.getId()))  // does it exist? 
                unkIndexList.add(new UInteger(index)); // requirement: 3.7.7.2.6 (incomplete: group service not available)
        }
    }

    // The Aggregation Definition is not updated on the COM archive (should requirement). // requirement: 3.7.7.2.9
    
    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.7.7.2.6 (error: a)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    if (!invIndexList.isEmpty()) // requirement: 3.7.7.2.7(incomplete: group service not available) (error: b)
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

  }

  @Override
  public void enableFilter(final Boolean lBoolean, final InstanceBooleanPairList lInstanceBooleanPairList, 
          final MALInteraction interaction) throws MALException, MALInteractionException  { // requirement: 3.7.8.2.1
    UIntegerList unkIndexList = new UIntegerList();
    UIntegerList invIndexList = new UIntegerList();
    InstanceBooleanPair tempBPair;
    
    if (null == lBoolean || null == lInstanceBooleanPairList) // Are the inputs null?
        throw new IllegalArgumentException("Boolean and InstanceBooleanPairList arguments must not be null");

    if (lBoolean)  // Are the objId group identifiers?
        throw new IllegalArgumentException("The MO M&C Group Service was not implemented. Group object instance identifiers cannot be used!");
            
    for (int index = 0; index < lInstanceBooleanPairList.size(); index++) {
        tempBPair = lInstanceBooleanPairList.get(index);

        if (tempBPair.getId() == 0){  // Is it the wildcard '0'? requirement: 3.7.8.2.4
            manager.setFilterEnabledAll(tempBPair.getValue());
            periodicReportingManager.refreshAll();
            periodicSamplingManager.refreshAll();
            break;
        }

        if (lBoolean){
            // Insert code here to make the Group service work with the enableGeneration operation
        //    if (!groupService.exists(tempBPair.getId()))  // does it exist? 
                invIndexList.add(new UInteger(index)); // requirement: 3.7.8.2.7 (incomplete: group service not available)

        }else{ // requirement: 3.7.8.2.8 (is respected because no error is generated if it is already enabled)
            manager.setFilterEnabled(tempBPair.getId(), tempBPair.getValue()); // requirement: 3.7.8.2.5
            periodicReportingManager.refresh(tempBPair.getId());
            periodicSamplingManager.refresh(tempBPair.getId());

            if (!manager.exists(tempBPair.getId()))  // does it exist? 
                unkIndexList.add(new UInteger(index)); // requirement: 3.7.8.2.6 (incomplete: group service not available)
        }
    }

    // The Aggregation Definition is not updated on the COM archive (should requirement). // requirement: 3.7.8.2.9
    
    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.7.8.2.6 (error: a)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    if (!invIndexList.isEmpty()) // requirement: 3.7.8.2.7(incomplete: group service not available) (error: b)
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

  }

  @Override
  public LongList listDefinition(final IdentifierList lIdentifier, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement: 3.7.9.2.1
    LongList outLongLst = new LongList();
    Long tempLong;
    
    if (null == lIdentifier) // Is the input null?
        throw new IllegalArgumentException("IdentifierList argument must not be null");
    
      for (Identifier tempIdentifier : lIdentifier) {
        // Check for the wildcard
        if ( tempIdentifier.toString().equals("*") ){  // requirement: 3.7.9.2.2
            outLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            outLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }
        tempLong = manager.list(tempIdentifier);
        if (tempLong != null) // Does the AggregationDefinition exist?
            outLongLst.add(tempLong); //yes, add it to the response variable. requirement: 3.7.9.2.3
      }

    // Errors
    // The operation does not return any errors.

    return outLongLst;
  }
  
  @Override
  public LongList addDefinition(final AggregationDefinitionDetailsList lAggregationDefinitionList, 
          final MALInteraction interaction) throws MALException, MALInteractionException  {
    LongList outLongLst = new LongList();
    UIntegerList invIndexList = new UIntegerList();
    UIntegerList dupIndexList = new UIntegerList();
    AggregationDefinitionDetails tempAggregationDefinition;
    
    if (null == lAggregationDefinitionList) // Is the input null?
        throw new IllegalArgumentException("AggregationDefinitionList argument must not be null");

    for (int index = 0; index < lAggregationDefinitionList.size(); index++) { // requirement: 3.7.10.2.5 (incremental "for cycle" guarantees that)
        tempAggregationDefinition = lAggregationDefinitionList.get(index);

        // Check if the name field of the AggregationDefinition is invalid.
        if ( tempAggregationDefinition.getName() == null  ||
             tempAggregationDefinition.getName().equals( new Identifier("*") ) ||
             tempAggregationDefinition.getName().equals( new Identifier("")  ) ){ // requirement: 3.7.10.2.2
                invIndexList.add(new UInteger(index));
        }

        if (manager.list(tempAggregationDefinition.getName()) == null){ // Is the supplied name unique? requirement: 3.7.10.2.3
            outLongLst.add( manager.add(tempAggregationDefinition) ); //  requirement: 3.7.10.2.4
        }else{
            dupIndexList.add(new UInteger(index));
        }
    }

    periodicReportingManager.refreshList(outLongLst); // Refresh the Periodic Reporting Manager for the added Definitions
    periodicSamplingManager.refreshList(outLongLst); // Refresh the Periodic Sampling Manager for the added Definitions

    // Errors
    if (!invIndexList.isEmpty()) // requirement: 3.7.10.2.2
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

    if (!dupIndexList.isEmpty()) // requirement: 3.7.10.2.3
        throw new MALInteractionException(new MALStandardError(COMHelper.DUPLICATE_ERROR_NUMBER, dupIndexList) );

    return outLongLst; // requirement: 3.7.10.2.4
  }

  @Override
  public void updateDefinition(final LongList lLongList, final AggregationDefinitionDetailsList lAggregationDefinitionList, 
          final MALInteraction interaction) throws MALException, MALInteractionException { // requirement: 3.7.11.2.1, 3.7.11.2.2, 3.7.11.2.3

    UIntegerList unkIndexList = new UIntegerList();
    UIntegerList invIndexList = new UIntegerList();
    Long tempLong;
    AggregationDefinitionDetails oldAggregationDefinition;
    AggregationDefinitionDetails tempAggregationDefinition;
    
    if (null == lAggregationDefinitionList || null == lLongList) // Are the inputs null?
        throw new IllegalArgumentException("LongList and AggregationDefinitionList arguments must not be null");

    for (int index = 0; index < lLongList.size(); index++) {
        tempAggregationDefinition = lAggregationDefinitionList.get(index);
        tempLong = lLongList.get(index);
        oldAggregationDefinition = manager.get(tempLong);

        if (oldAggregationDefinition == null){ // The object instance identifier could not be found? // requirement: 3.7.11.2.5
            unkIndexList.add(new UInteger(index));
            continue;
        }

        if (tempAggregationDefinition.getName().equals(oldAggregationDefinition.getName())){ // Are the names equal? requirement: 3.7.11.2.6
            manager.update(tempLong, tempAggregationDefinition); // Change on the manager
            periodicReportingManager.refresh(tempLong);// then, refresh the Periodic updates and samplings
            periodicSamplingManager.refresh(tempLong);
        }else{
            invIndexList.add(new UInteger(index));
        }
    }

    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.7.11.2.5 (error: a)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    if (!invIndexList.isEmpty()) // requirement: 3.7.11.2.6 (error: b)
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

  }

  @Override
  public void removeDefinition(final LongList lLongList, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement: 3.7.12.2.1
    UIntegerList unkIndexList = new UIntegerList();
    Long tempLong;
    LongList tempLongLst = new LongList();
    
    if (null == lLongList) // Is the input null?
        throw new IllegalArgumentException("LongList argument must not be null");

    for (int index = 0; index < lLongList.size(); index++) {
        tempLong = lLongList.get(index);

        if (tempLong == 0){  // Is it the wildcard '0'? requirement: 3.7.12.2.2
            tempLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            tempLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }

        if (manager.exists(tempLong)){ // Does it match an existing definition? requirement: 3.7.12.2.3
            tempLongLst.add(tempLong);
        }else{
            unkIndexList.add(new UInteger(index)); // requirement: 3.7.12.2.3
        }
    }

    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.7.12.2.3 (error: a, b)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    // requirement: 3.7.12.2.5 (Inserting the errors before this line guarantees that the requirement is met)
    for (Long tempLong2: tempLongLst)
        manager.delete(tempLong2);

    periodicReportingManager.refreshList(tempLongLst); // Refresh the Periodic Reporting Manager for the removed Definitions
    periodicSamplingManager.refreshList(tempLongLst); // Refresh the Periodic Sampling Manager for the removed Definitions
    // COM archive is left untouched. requirement: 3.7.12.2.4

  }
  
  private static final class PublishInteractionListener implements MALPublishInteractionListener
  {
    @Override
    public void publishDeregisterAckReceived(final MALMessageHeader header, final Map qosProperties) throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishDeregisterAckReceived");
    }

    @Override
    public void publishErrorReceived(final MALMessageHeader header, final MALErrorBody body, final Map qosProperties) throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishErrorReceived");
    }

    @Override
    public void publishRegisterAckReceived(final MALMessageHeader header, final Map qosProperties) throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishRegisterAckReceived");
    }

    @Override
    public void publishRegisterErrorReceived(final MALMessageHeader header, final MALErrorBody body, final Map qosProperties) throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishRegisterErrorReceived");
    }
  }

  private class PeriodicReportingManager { // requirement: 3.7.2.1a
    
    private List<Timer> updateTimerList; // updateInterval Timers list
    private List<Timer> filterTimeoutTimerList; // filterTimeout Timers list
    private LongList objIdList; // Object Instance Identifier list of the Aggregations
    private boolean active = false; // Flag that determines if the Manager is on or off
    
    public PeriodicReportingManager(){
        updateTimerList = new ArrayList<>();
        filterTimeoutTimerList = new ArrayList<>();
        objIdList = new LongList();
    }

    public void refreshAll(){   this.refreshList(manager.listAll());    }
    public void pause(){        active = false; }
    public void start(){        active = true;  }

    public void init(){
        this.refreshAll(); // Refresh all the Aggregation Definitions on the Manager
        this.start(); // set active flag to true
    }

    public void refresh(Long objId){
        // get aggregation definition
        AggregationDefinitionDetails ADef = manager.get(objId);
        int index = objIdList.indexOf(objId);
        
        if (index != -1) // Does it exist in the Periodic Reporting Manager?
            this.removePeriodicReporting(objId);              

        if(ADef != null){ // Does it exist in the Aggregation Definitions List?
//            if (ADef.getUpdateInterval().getValue() != (float) 0 && ADef.getGenerationEnabled() ) // Is the periodic reporting active? (requirement 3.7.2.12)
            if (ADef.getUpdateInterval().getValue() != 0 && ADef.getGenerationEnabled() ) // Is the periodic reporting active? (requirement 3.7.2.12)
                this.addPeriodicReporting(objId);  // requirement: 3.7.2.10
        }

        manager.resetPeriodicAggregationValuesList(objId); // Reset the Sampling Values
    }

    public void refreshList(LongList objIds){
        if (objIds == null) return;
        for (Long objId : objIds)
            refresh(objId);
    }

    private void addPeriodicReporting(Long objId){
        int index = updateTimerList.size();
        objIdList.add(index, objId);
        Timer timer = new Timer();
        updateTimerList.add(index, timer);
        this.startUpdatesTimer(index, manager.get(objId).getUpdateInterval());  // requirement 3.7.2.11
        
        // Is the filter enabled? If so, do we have a filter Timeout set?
//        if (manager.get(objId).getFilterEnabled() && manager.get(objId).getFilteredTimeout().getValue() != (float) 0){ // requirement 3.7.2.12
        if (manager.get(objId).getFilterEnabled() && manager.get(objId).getFilteredTimeout().getValue() != 0){ // requirement 3.7.2.12
            Timer timer2 = new Timer();
            filterTimeoutTimerList.add(index, timer2);
            this.startFilterTimeoutTimer(index, manager.get(objId).getFilteredTimeout());
        }else{
            filterTimeoutTimerList.add(index, null);
        }
    }

    private void removePeriodicReporting(Long objId){
        final int index = objIdList.indexOf(objId);
        this.stopUpdatesTimer(index);
        this.stopFilterTimeoutTimer(index);
        objIdList.remove(index);
        updateTimerList.remove(index);
        filterTimeoutTimerList.remove(index);
    }

    private void startUpdatesTimer(final int index, final Duration interval){
        updateTimerList.get(index).scheduleAtFixedRate(new TimerTask() {
        final Long objId = objIdList.get(index);
            @Override
            public void run() {  // requirement: 3.7.2.3
                if (active){
                    AggregationDefinitionDetails def = manager.get(objId);
                    if (def.getGenerationEnabled() ){  // requirement 3.7.2.9
                        
                        if (!def.getFilterEnabled()) // The Filter is not enabled?
                            publishAggregationUpdate(objId, manager.getAggregationValue(objId, GenerationMode.PERIODIC, false));

                        if (def.getFilterEnabled() && manager.isFilterTriggered(objId) == true){ // The Filter is on and triggered? requirement: 3.7.2.6
                            publishAggregationUpdate(objId, manager.getAggregationValue(objId, GenerationMode.PERIODIC, true)); // requirement: 3.7.5.2.8
                            manager.setFilterTriggered(objId, false); // Reset the trigger
                            resetFilterTimeoutTimer(objId);        // Reset the timer
                        }

                        manager.resetPeriodicAggregationValuesList(objId); // Reset the Sampled Values
                        
                    }
                }
            } // the time is being converted to milliseconds by multiplying by 1000  (starting delay included)
        }, (int) (interval.getValue() * 1000), (int) (interval.getValue() * 1000) ); // requirement: 3.7.2.3
    }

    private void stopUpdatesTimer(final int index){
        updateTimerList.get(index).cancel();
    }

    private void resetFilterTimeoutTimer(Long objId){
        int index = objIdList.indexOf(objId);
        if (index == -1) return;  // Get out if it didn't find the objId
        if (filterTimeoutTimerList.get(index) == null) return;  // Get out if the timer was not set
        this.stopFilterTimeoutTimer(index);
        Timer timer2 = new Timer();
        filterTimeoutTimerList.add(index, timer2);
        this.startFilterTimeoutTimer(index, manager.get(objId).getFilteredTimeout());      
    }
    
    private void startFilterTimeoutTimer(final int index, final Duration interval){
        filterTimeoutTimerList.get(index).scheduleAtFixedRate(new TimerTask() { 
        final Long objId = objIdList.get(index);
            @Override
            public void run() {  // requirement: 3.7.2.5
                if (active){
                    if (manager.get(objId).getFilterEnabled() && manager.get(objId).getGenerationEnabled())  // requirement 3.7.2.9
                        publishAggregationUpdate(objId, manager.getAggregationValue(objId, GenerationMode.FILTERED_TIMEOUT, true)); // requirement: 3.7.2.6
                }
            } // the time is being converted to milliseconds by multiplying by 1000
        }, (int) (interval.getValue() * 1000), (int) (interval.getValue() * 1000) );
    }

    private void stopFilterTimeoutTimer(final int index){
        if (filterTimeoutTimerList.get(index) != null)  // Does it exist?
            filterTimeoutTimerList.get(index).cancel();
    }

  }

  
  private class PeriodicSamplingManager { // requirement: 3.7.2.1a
    
    private List<Timer> sampletimerList; // Timers list
    private LongList aggregationObjId; // Corresponding object instance identifier of the above aggregation reference 
    private List<Integer> parameterSetIndex; // Corresponding Parameter set Index
    private boolean active = false; // Flag that determines if the Manager is on or off
    
    public PeriodicSamplingManager(){
        sampletimerList = new ArrayList<>();
        aggregationObjId = new LongList();
        parameterSetIndex = new ArrayList<>();
    }

    public void refreshAll(){  this.refreshList(manager.listAll());    }
    public void pause(){    active = false; }
    public void start(){    active = true;  }

    public void init(){
        this.refreshAll(); // Refresh all the Aggregation Definitions on the Manager
        this.start(); // set active flag to true
    }

    public void refresh(Long objId){
        final int index = aggregationObjId.indexOf(objId);

        if (index != -1) // Does it exist in the PeriodicSamplingManager?
            this.removePeriodicSampling(objId);
        
        if (manager.exists(objId)) // Does it exist in the Aggregation Definitions List?
            this.addPeriodicSampling(objId);

        manager.resetPeriodicAggregationValuesList(objId); // Reset the Sampled Values
    }

    public void refreshList(LongList objIds){
        if (objIds == null) return;
        for (Long objId : objIds)
            refresh(objId);
    }
    
    private void addPeriodicSampling(Long objId){
        if(!manager.get(objId).getGenerationEnabled()) return; // Periodic Sampling shall not occur if the generation is not enabled at the definition level
        final int parameterSetsTotal = manager.get(objId).getParameterSets().size();
        int index = sampletimerList.size();

        for (int indexOfParameterSet = 0; indexOfParameterSet < parameterSetsTotal; indexOfParameterSet++){
            Duration sampleInterval = manager.get(objId).getParameterSets().get(indexOfParameterSet).getSampleInterval();

            if (sampleInterval.getValue() >  manager.get(objId).getUpdateInterval().getValue())
                sampleInterval = new Duration(0);
                
            // Add to the Periodic Sampling Manager only if there's a sampleInterval selected for the parameterSet
            if (sampleInterval.getValue() != 0 ){
                aggregationObjId.add(index, objId);
                parameterSetIndex.add(index, indexOfParameterSet);
                Timer timer = new Timer();  // Take care of adding a new timer
                sampletimerList.add(index, timer);
                startTimer(index, sampleInterval);
                index++;
            }
        }
    }

    private void removePeriodicSampling(Long objId){
        for (int index = aggregationObjId.indexOf(objId) ; index != -1 ; index = aggregationObjId.indexOf(objId)){
            this.stopTimer(index);
            aggregationObjId.remove(index);
            sampletimerList.remove(index);
            parameterSetIndex.remove(index);
        }
    }

    private void startTimer(final int index, Duration interval){  // requirement: 3.7.2.11
        final Long objId = aggregationObjId.get(index);
        final int IndexOfparameterSet = parameterSetIndex.get(index);

        sampletimerList.get(index).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (active){
                    final AggregationDefinitionDetails aggregationDefinition = manager.get(objId);
                    final AggregationParameterSet aggregationParameterSet = aggregationDefinition.getParameterSets().get(IndexOfparameterSet);

                    // Add another sample on the AggregationValue that will be returned later:
                    final ParameterValueList previousParameterValue = manager.getLastParameterValue(objId, IndexOfparameterSet);
                    final ParameterValueList currentParameterValue = manager.updateParameterValue(objId, IndexOfparameterSet);
                    
                    if (previousParameterValue == null)  // Is it the first value? 
                        return;   // Then, there's no need to check the filter
                    
                    // Filter Comparison Process
                    if (aggregationDefinition.getFilterEnabled() &&  // Is the filter enabled? 
                        aggregationParameterSet.getPeriodicFilter() != null && // requirement: 3.7.2.7 (and 4.7.6: periodicFilter comment)
                        aggregationParameterSet.getParameters().size() == 1){  // Are we sampling one parameter? requirement: 3.7.2.8
                        
                        // In theory all the list should be null with the exception of the Parameter Value we want
                        // because size = 1 and the remaining Parameter Values inside get the null state
                        // So we can crawl the list until we find the first non-null element and compare it with the previousParameterValue
                        for (int i = 0; i < currentParameterValue.size(); i++){
                            ParameterValue current = currentParameterValue.get(i);
                            ParameterValue previous = previousParameterValue.get(i);
                            
                            if (current != null && previous != null){
                                // Compare the values:
                                if ( (current.getIsValid() && previous.getIsValid() ) ||  // Are the parameters valid?
                                     (current.getInvalidSubState().getValue() == 2 && previous.getInvalidSubState().getValue() == 2)   ){ // 2 stands for the INVALID_RAW state
                                    
                                    Boolean filterisTriggered = false;
                                    if (current.getIsValid() && previous.getIsValid() && 
                                        current.getConvertedValue() != null && previous.getConvertedValue() != null )  // requirement: 3.7.2.6
                                        filterisTriggered = manager.triggeredFilter(current.getConvertedValue(), previous.getConvertedValue(), aggregationParameterSet.getPeriodicFilter());

                                    if (current.getConvertedValue() == null && previous.getConvertedValue() == null )  // requirement: 3.7.2.6
                                        filterisTriggered = manager.triggeredFilter(current.getRawValue(), previous.getRawValue(), aggregationParameterSet.getPeriodicFilter());
                                    
                                    
                                    if ( filterisTriggered ){
                                        manager.setFilterTriggered(objId, true);
                                    }
                                }
                                
                            break;
                            }
                        }
                    }
                }
            }
        }, 0, (int) (interval.getValue() * 1000) ); // the time has to be converted to milliseconds by multiplying by 1000
    }

    private void stopTimer(int index){
        sampletimerList.get(index).cancel();
    }

  }

}
