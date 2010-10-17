/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.location;

import org.eclipse.emf.cdo.internal.location.bundle.OM;

import org.eclipse.net4j.util.container.IContainer;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public interface IRepositoryLocationManager extends IContainer<IRepositoryLocation>
{
  public static final IRepositoryLocationManager INSTANCE = OM.getRepositoryLocationManager();

  public IRepositoryLocation[] getRepositoryLocations();

  public IRepositoryLocation addRepositoryLocation(String connectorType, String connectorDescription,
      String repositoryName);
}
