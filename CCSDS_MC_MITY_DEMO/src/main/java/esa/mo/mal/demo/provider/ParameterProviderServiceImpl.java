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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import org.ccsds.moims.mo.com.structures.ObjectType;
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
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mal.structures.Union;
import org.ccsds.moims.mo.mal.structures.UpdateHeader;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.structures.UpdateType;
import org.ccsds.moims.mo.mal.transport.MALErrorBody;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mc.MCHelper;
import org.ccsds.moims.mo.mc.parameter.ParameterHelper;
import org.ccsds.moims.mo.mc.parameter.body.GetValueResponse;
import org.ccsds.moims.mo.mc.parameter.provider.MonitorValuePublisher;
import org.ccsds.moims.mo.mc.parameter.provider.ParameterInheritanceSkeleton;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValueList;

/**
 *
 */
public class ParameterProviderServiceImpl extends ParameterInheritanceSkeleton 
{
  private static final int DEFAULT_SLEEP = 10000;
  private IdentifierList domain;
  private MALContextFactory malFactory;
  private MALContext mal;
  private MALProviderManager providerMgr;
  private MALProvider parameterServiceProvider;
  private boolean initialiased = false;
  private boolean running = false;
  private MonitorValuePublisher publisher;
  private boolean isRegistered = false;
  private ParameterManager manager;
  private PeriodicReportingManager periodicReportingManager;
  private final ObjectType parameterDefinitionObjType = new ObjectType (
          ParameterDefinitionDetails.AREA_SHORT_FORM,
          ParameterDefinitionDetails.SERVICE_SHORT_FORM,
          ParameterDefinitionDetails.AREA_VERSION,
          new UShort(ParameterDefinitionDetails.TYPE_SHORT_FORM)          
  );
          
  public ParameterDefinitionDetails makeNewParameterDefinition(String name, String description, int interval){
      ParameterDefinitionDetails PDef = new ParameterDefinitionDetails();

      PDef.setName(new Identifier(name));
      PDef.setDescription(description);
      Union union = new Union(Union.DOUBLE_TYPE_SHORT_FORM);
      PDef.setRawType( (byte) 5);
      PDef.setDescription("meters");
      PDef.setGenerationEnabled(true);  // shall not matter, because when we add it it will be false!
      PDef.setUpdateInterval(new Duration( interval));
      PDef.setValidityExpression(null);
      PDef.setConversion(null);

      return PDef;
  }

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
        COMHelper.deepInit(MALContextFactory.getElementFactoryRegistry());

      ParameterHelper.init(MALContextFactory.getElementFactoryRegistry());
      
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
      
      manager = parameterManager;
      periodicReportingManager = new PeriodicReportingManager();
      periodicReportingManager.init(); // Initialize the Periodic Reporting Manager
      
/*    
      LongList objIds = new LongList();
      Long objId;
      objId = manager.add(makeNewParameterDefinition("GPS.Altitude", "Altitude of the GPS", 1)); // 16 seconds
      objIds.add(objId);
      objId = manager.add(makeNewParameterDefinition("GPS.Latitude", "Latitude of the GPS", 4)); // 2 seconds
      objIds.add(objId);
      objId = manager.add(makeNewParameterDefinition("GPS.Longitude", "Longitude of the GPS", 10)); // 5 seconds
      objIds.add(objId);
      
      periodicReportingManager.refreshList(objIds);
  
 **/
      
      manager.setGenerationEnabledAll(false);

      DemoProviderCli.LOGGER.info("Parameter service READY");
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
    if (null != parameterServiceProvider){
      parameterServiceProvider.close();
    }

    // start transport
    URI sharedBrokerURI = null;
    if ((null != System.getProperty("demo.provider.useSharedBroker"))
            && (null != System.getProperty("shared.broker.uri")))
    {
      sharedBrokerURI = new URI(System.getProperty("shared.broker.uri"));
    }

    parameterServiceProvider = providerMgr.createProvider("Parameter",
            null,
            ParameterHelper.PARAMETER_SERVICE,
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

    DemoProviderCli.LOGGER.log(Level.INFO, "Parameter Service URI       : {0}", parameterServiceProvider.getURI());
    DemoProviderCli.LOGGER.log(Level.INFO, "Parameter Service broker URI: {0}", parameterServiceProvider.getBrokerURI());


    try
    {
      final File file = new File("demoServiceURI.properties");
      final FileOutputStream fos = new FileOutputStream(file);
      final OutputStreamWriter osw = new OutputStreamWriter(fos);
      final BufferedWriter wrt = new BufferedWriter(osw);
      wrt.append("ParameterURI=" + parameterServiceProvider.getURI());
      wrt.newLine();
      wrt.append("ParameterBroker=" + parameterServiceProvider.getBrokerURI());
      wrt.newLine();
      wrt.close();
      fos.close();
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
    try
    {
      running = false;

      if (null != parameterServiceProvider)
      {
        parameterServiceProvider.close();
      }
      if (null != providerMgr)
      {
        providerMgr.close();
      }
      if (null != mal)
      {
        mal.close();
      }
    }
    catch (MALException ex)
    {
      DemoProviderCli.LOGGER.log(Level.WARNING, "Exception during close down of the provider {0}", ex);
    }
  }

  private void publishParameterUpdate(final Long objId)
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
         "Generating Parameter update for the Parameter Definition: {0} (Identifier: {1})",
            new Object[]
            {
              objId, new Identifier (manager.get(objId).getName().toString())
            });

    //  requirements: 3.3.5.2.1 , 3.3.5.2.2 , 3.3.5.2.3 , 3.3.5.2.4
    final EntityKey ekey = new EntityKey(new Identifier (manager.get(objId).getName().toString()), objId, manager.generatePValobjId(), null);
    final Time timestamp = new Time(System.currentTimeMillis()); //  requirement: 3.3.5.2.5

    final UpdateHeaderList hdrlst = new UpdateHeaderList();
    final ObjectIdList objectIdlst = new ObjectIdList();
    final ParameterValueList pVallst = new ParameterValueList();
    final ParameterValue parameterValue = manager.getParameterValue(objId);
            
    hdrlst.add(new UpdateHeader(timestamp, new URI("SomeURI"), UpdateType.UPDATE, ekey));
    objectIdlst.add(null); // requirement: 3.3.5.2.7 (3.3.5.2.6 not necessary)
    pVallst.add(parameterValue); // requirement: 3.3.5.2.8

    publisher.publish(hdrlst, objectIdlst, pVallst);

    }
    catch (IllegalArgumentException | MALException | MALInteractionException ex)
    {
      ex.printStackTrace();
    }
  }

  @Override
  public GetValueResponse getValue(final LongList lLongList, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement 3.3.6.2.1
    
    LongList outLongLst = new LongList();
    UIntegerList unkIndexList = new UIntegerList();
    ParameterValueList outPValLst;
    ParameterDefinitionDetails tempPDef;
    Long tempLong;

    if (null == lLongList) // Is the input null?
        throw new IllegalArgumentException("LongList argument must not be null");

    for (int index = 0; index < lLongList.size(); index++) {
        tempLong = lLongList.get(index);

        if (tempLong == 0){  // Is it the wildcard '0'? requirement: 3.3.6.2.2
            outLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            outLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }

        tempPDef = manager.get(tempLong);

        if (tempPDef != null){ // Does the ParameterDefinition exist?
            outLongLst.add(tempLong); //yap
        }else{
            unkIndexList.add(new UInteger(index)); // add the index to the list of errors
        } 
    }

    outPValLst = manager.getParameterValues(outLongLst); // requirement: 3.3.6.2.4

    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.3.6.2.3
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );
    
                
    return new GetValueResponse(outLongLst, outPValLst);
  }

  @Override
  public void enableGeneration(final Boolean lBoolean, final InstanceBooleanPairList lInstanceBooleanPairList, 
          final MALInteraction interaction) throws MALException, MALInteractionException  { // requirement: 3.3.7.2.1
    UIntegerList unkIndexList = new UIntegerList();
    UIntegerList invIndexList = new UIntegerList();
    InstanceBooleanPair tempBPair;
    
    if (null == lBoolean || null == lInstanceBooleanPairList) // Are the inputs null?
        throw new IllegalArgumentException("Boolean and InstanceBooleanPairList arguments must not be null");

    if (lBoolean)  // Are the objId group identifiers?
        throw new IllegalArgumentException("The MO M&C Group Service was not implemented. Group object instance identifiers cannot be used!");
            
    for (int index = 0; index < lInstanceBooleanPairList.size(); index++) {
        tempBPair = lInstanceBooleanPairList.get(index);

        if (tempBPair.getId() == 0){  // Is it the wildcard '0'? requirement: 3.3.7.2.4
            manager.setGenerationEnabledAll(tempBPair.getValue());
            periodicReportingManager.refreshAll();
            break;
        }

        if (lBoolean){
            // Insert code here to make the Group service work with the enableGeneration operation
        //    if (!groupService.exists(tempBPair.getId()))  // does it exist? 
                invIndexList.add(new UInteger(index)); // requirement: 3.3.7.2.7 (incomplete: group service not available)

        }else{
            manager.setGenerationEnabled(tempBPair.getId(), tempBPair.getValue()); // requirement: 3.3.7.2.5
            periodicReportingManager.refresh(tempBPair.getId());

            if (!manager.exists(tempBPair.getId()))  // does it exist? 
                unkIndexList.add(new UInteger(index)); // requirement: 3.3.7.2.6 (incomplete: group service not available)
                
        }
    }

    // The Parameter Definition is not updated on the COM archive (should requirement). // requirement: 3.3.7.2.8
    
    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.3.7.2.6 (error: a)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    if (!invIndexList.isEmpty()) // requirement: 3.3.7.2.7(incomplete: group service not available) (error: b)
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

  }
  
  @Override
  public void setValue(final LongList paramDefInstId, final ParameterValueList newValues, 
          final MALInteraction interaction) throws MALException, MALInteractionException  {

      // Will be available on the next version of the Standard! The requirements are currently not available.
  }

  @Override
  public LongList listDefinition(final IdentifierList lIdentifier, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement: 3.3.8.2.1
    LongList outLongLst = new LongList();
    Long tempLong;
    
    if (null == lIdentifier) // Is the input null?
        throw new IllegalArgumentException("IdentifierList argument must not be null");
    
      for (Identifier tempIdentifier : lIdentifier) {
        // Check for the wildcard
        if ( tempIdentifier.toString().equals("*") ){  // requirement: 3.3.8.2.2
            outLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            outLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }
        tempLong = manager.list(tempIdentifier);
        if (tempLong != null) // Does the ParameterDefinition exist?
            outLongLst.add(tempLong); //yes, add it to the response variable. requirement: 3.3.8.2.3
      }

    // Errors
    // The operation does not return any errors.

    return outLongLst;
  }
  
  @Override
  public LongList addDefinition(final ParameterDefinitionDetailsList lParameterDefinitionList, 
          final MALInteraction interaction) throws MALException, MALInteractionException  {
    LongList outLongLst = new LongList();
    UIntegerList invIndexList = new UIntegerList();
    UIntegerList dupIndexList = new UIntegerList();
    ParameterDefinitionDetails tempParameterDefinition;
    
    if (null == lParameterDefinitionList) // Is the input null?
        throw new IllegalArgumentException("ParameterDefinitionList argument must not be null");

    for (int index = 0; index < lParameterDefinitionList.size(); index++) { // requirement: 3.3.9.2.5 (incremental "for cycle" guarantees that)
        tempParameterDefinition = lParameterDefinitionList.get(index);

        // Check if the name field of the ParameterDefinition is invalid.
        if ( tempParameterDefinition.getName() == null  ||
             tempParameterDefinition.getName().equals( new Identifier("*") ) ||
             tempParameterDefinition.getName().equals( new Identifier("")  ) ){ // requirement: 3.3.9.2.2
                invIndexList.add(new UInteger(index));
        }

        if (manager.list(tempParameterDefinition.getName()) == null){ // Is the supplied name unique? requirement: 3.3.9.2.3
            outLongLst.add( manager.add(tempParameterDefinition) ); //  requirement: 3.3.9.2.4
        }else{
            dupIndexList.add(new UInteger(index));
        }
    }

    periodicReportingManager.refreshList(outLongLst); // Refresh the Periodic Reporting Manager for the added Definitions

    // Errors
    if (!invIndexList.isEmpty()) // requirement: 3.3.9.2.2
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

    if (!dupIndexList.isEmpty()) // requirement: 3.3.9.2.3
        throw new MALInteractionException(new MALStandardError(COMHelper.DUPLICATE_ERROR_NUMBER, dupIndexList) );

    return outLongLst; // requirement: 3.3.9.2.4
  }


  
  @Override
  public void updateDefinition(final LongList lLongList, final ParameterDefinitionDetailsList lParameterDefinitionList, 
          final MALInteraction interaction) throws MALException, MALInteractionException  { // requirement: 3.3.10.2.1

    UIntegerList unkIndexList = new UIntegerList();
    UIntegerList invIndexList = new UIntegerList();
    Long tempLong;
    ParameterDefinitionDetails tempParameterDefinition;
    ParameterDefinitionDetails oldParameterDefinition;
    
    if (null == lParameterDefinitionList || null == lLongList) // Are the inputs null?
        throw new IllegalArgumentException("LongList and ParameterDefinitionList arguments must not be null");

    for (int index = 0; index < lLongList.size(); index++) {
        tempParameterDefinition = lParameterDefinitionList.get(index);
        tempLong = lLongList.get(index);
        oldParameterDefinition = manager.get(tempLong);

        if (oldParameterDefinition == null){ // The object instance identifier could not be found? // requirement: 3.3.10.2.5
            unkIndexList.add(new UInteger(index));
            continue;
        }

        if (tempParameterDefinition.getName().equals(oldParameterDefinition.getName())){ // Are the names equal? requirement: 3.3.10.2.6
            manager.update(tempLong, tempParameterDefinition); // Change on the manager
            periodicReportingManager.refresh(tempLong);// then, refresh the Periodic updates
        }else{
            invIndexList.add(new UInteger(index));
        }
    }

    // Errors
    if (!invIndexList.isEmpty()) // requirement: 3.3.10.2.6 (error: a)
        throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, invIndexList) );

    if (!unkIndexList.isEmpty()) // requirement: 3.3.10.2.5 (error: b)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

  }

  @Override
  public void removeDefinition(final LongList lLongList, final MALInteraction interaction) throws MALException, MALInteractionException
  { // requirement: 3.3.11.2.1
    UIntegerList unkIndexList = new UIntegerList();
    Long tempLong;
    LongList tempLongLst = new LongList();
    
    if (null == lLongList) // Is the input null?
        throw new IllegalArgumentException("LongList argument must not be null");

    for (int index = 0; index < lLongList.size(); index++) {
        tempLong = lLongList.get(index);

        if (tempLong == 0){  // Is it the wildcard '0'? requirement: 3.3.11.2.2
            tempLongLst.clear();  // if the wildcard is in the middle of the input list, we clear the output list and...
            tempLongLst.addAll(manager.listAll()); // ... add all in a row
            break;
        }

        if (manager.exists(tempLong)){ // Does it match an existing definition? requirement: 3.3.11.2.3
            tempLongLst.add(tempLong);
        }else{
            unkIndexList.add(new UInteger(index)); // requirement: 3.3.11.2.3
        }
    }

    // Errors
    if (!unkIndexList.isEmpty()) // requirement: 3.3.11.2.3 (error: a, b)
        throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, unkIndexList) );

    // requirement: 3.3.11.2.5 (Inserting the errors before this line guarantees that the requirement is met)
    for (Long tempLong2: tempLongLst)
        manager.delete(tempLong2);

    periodicReportingManager.refreshList(tempLongLst); // Refresh the Periodic Reporting Manager for the removed Definitions
    // COM archive is left untouched. requirement: 3.3.11.2.4

  }
  
  private static final class PublishInteractionListener implements MALPublishInteractionListener
  {
    @Override
    public void publishDeregisterAckReceived(final MALMessageHeader header, final Map qosProperties)
            throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishDeregisterAckReceived");
    }

    @Override
    public void publishErrorReceived(final MALMessageHeader header, final MALErrorBody body, final Map qosProperties)
            throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishErrorReceived");
    }

    @Override
    public void publishRegisterAckReceived(final MALMessageHeader header, final Map qosProperties)
            throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishRegisterAckReceived");
    }

    @Override
    public void publishRegisterErrorReceived(final MALMessageHeader header,
            final MALErrorBody body,
            final Map qosProperties)
            throws MALException
    {
      DemoProviderCli.LOGGER.fine("PublishInteractionListener::publishRegisterErrorReceived");
    }
  }

  
  private class PeriodicReportingManager { // requirement: 3.3.2.1a
    
    List<Timer> timerList; // Timers list
    LongList objIdList; // Object Instance Identifier list
//    DurationList updateIntervalList; //Update Interval List
    boolean active = false; // Flag that determines if the Manager is on or off
    
    public PeriodicReportingManager(){
        timerList = new ArrayList<>();
        objIdList = new LongList();
 //       updateIntervalList = new DurationList();
    }

    public void start(){        active = true;     }
    public void pause(){        active = false;    }

    public void init(){   // Refresh all the Parameter Definitions on the Manager
        this.refreshList(manager.listAll());
        active = true; // set active flag to true
    }

    public void refresh(Long objId){
        // get parameter definition
        ParameterDefinitionDetails PDef = manager.get(objId);
        int index = objIdList.indexOf(objId);
        
        if (index != -1) // Does it exist in the Periodic Manager?
            this.removePeriodicReporting(objId);                

        if(PDef != null){ // Does it exist in the Aggregation Definitions List?
//            if (PDef.getUpdateInterval().getValue() != (float) 0 && PDef.getGenerationEnabled() ) // Is the periodic reporting active?
            if (PDef.getUpdateInterval().getValue() != 0 && PDef.getGenerationEnabled() ) // Is the periodic reporting active?
                this.addPeriodicReporting(objId);
        }
    }

    public void refreshList(LongList objIds){
        if (objIds == null) return;
        for (Long objId : objIds)
            refresh(objId);
    }
    
    public void refreshAll(){
        this.refreshList(manager.listAll());
    }

    private void addPeriodicReporting(Long objId){
        Timer timer = new Timer();
        int index = timerList.size();
        objIdList.add(index, objId);
        timerList.add(index, timer);
        startTimer(index, manager.get(objId).getUpdateInterval());      
    }

    private void removePeriodicReporting(Long objId){
        int index = objIdList.indexOf(objId);
        this.stopTimer(index);
        objIdList.remove(index);
        timerList.remove(index);
    }

    private void startTimer(final int index, final Duration interval){  // requirement: 3.3.2.11
        final Long objId = objIdList.get(index);

        timerList.get(index).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (active){
                    if (objId == -1) return;
                    if (manager.get(objId).getGenerationEnabled())
                        publishParameterUpdate(objId);
                }
            }
        }, 0, (int) (interval.getValue() * 1000) ); // the time has to be converted to milliseconds by multiplying by 1000
    }

    private void stopTimer(int index){
        timerList.get(index).cancel();
    }

  }

}
