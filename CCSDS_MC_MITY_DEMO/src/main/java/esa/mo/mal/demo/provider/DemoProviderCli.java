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
import java.io.File;
import java.util.logging.Logger;

/**
 * This class provides a simple cli for the control of the provider. It allows control of the generation of updates, the
 * rate the updates are generated, the size of the set of updates (the pool) and the block size of the update sets.
 */
public class DemoProviderCli
{
  /**
   * Logger
   */
  public static final java.util.logging.Logger LOGGER = Logger.getLogger("org.ccsds.moims.mo.mal.demo.provider");
  private static final ParameterManager parameterManager = new ParameterManager();
  private static final ParameterProviderServiceImpl parameterService = new ParameterProviderServiceImpl();
  private static final AggregationProviderServiceImpl aggregationService = new AggregationProviderServiceImpl();

  /**
   * Main command line entry point.
   *
   * @param args the command line arguments
   * @throws java.lang.Exception If there is an error
   */
  public static void main(final String args[]) throws Exception
  {
    final java.util.Properties sysProps = System.getProperties();

    File file = new File(System.getProperty("provider.properties", "demoProvider.properties"));
    if (file.exists())
      sysProps.putAll(StructureHelper.loadProperties(file.toURI().toURL(), "provider.properties"));

    file = new File(System.getProperty("broker.properties", "sharedBrokerURI.properties"));
    if (file.exists())
      sysProps.putAll(StructureHelper.loadProperties(file.toURI().toURL(), "broker.properties"));
    
    System.setProperties(sysProps);
    parameterService.init(parameterManager);
    aggregationService.init(parameterManager);

  }
}
