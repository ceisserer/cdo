/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.db;

import org.eclipse.emf.cdo.common.CDOCommonRepository.IDGenerationLocation;
import org.eclipse.emf.cdo.tests.config.IScenario;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest;
import org.eclipse.emf.cdo.tests.offline.Bugzilla_319552_Test;
import org.eclipse.emf.cdo.tests.offline.Bugzilla_325097_Test;
import org.eclipse.emf.cdo.tests.offline.Bugzilla_326047_Test;
import org.eclipse.emf.cdo.tests.offline.Bugzilla_328352_Test;
import org.eclipse.emf.cdo.tests.offline.Bugzilla_329014_Test;
import org.eclipse.emf.cdo.tests.offline.FailoverTest;
import org.eclipse.emf.cdo.tests.offline.OfflineDelayedTest;
import org.eclipse.emf.cdo.tests.offline.OfflineLockingTest;
import org.eclipse.emf.cdo.tests.offline.OfflineRawTest;
import org.eclipse.emf.cdo.tests.offline.OfflineTest;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Eike Stepper
 */
public class AllTestsDBH2Offline extends DBConfigs
{
  public static Test suite()
  {
    return new AllTestsDBH2Offline().getTestSuite("CDO Tests (DBStore H2 Horizontal - offline mode)");
  }

  @Override
  protected void initConfigSuites(TestSuite parent)
  {
    addScenario(parent, COMBINED, new H2OfflineConfig(false, false, IDGenerationLocation.STORE), JVM, NATIVE);
    addScenario(parent, COMBINED, new H2OfflineConfig(false, false, IDGenerationLocation.CLIENT), JVM, NATIVE);
  }

  @Override
  protected void initTestClasses(List<Class<? extends ConfigTest>> testClasses, IScenario scenario)
  {
    testClasses.add(OfflineTest.class);
    testClasses.add(OfflineRawTest.class);
    testClasses.add(OfflineDelayedTest.class);
    testClasses.add(OfflineLockingTest.class);

    testClasses.add(Bugzilla_329014_Test.class);
    testClasses.add(Bugzilla_328352_Test.class);
    testClasses.add(Bugzilla_326047_Test.class);
    testClasses.add(Bugzilla_325097_Test.class);
    testClasses.add(Bugzilla_319552_Test.class);

    testClasses.add(FailoverTest.class);
  }
}
