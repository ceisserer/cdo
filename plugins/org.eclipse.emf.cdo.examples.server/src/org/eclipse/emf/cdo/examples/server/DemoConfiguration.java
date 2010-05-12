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
package org.eclipse.emf.cdo.examples.server;

import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSessionManager;

import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.IDBConnectionProvider;
import org.eclipse.net4j.signal.ISignalProtocol;
import org.eclipse.net4j.util.container.ContainerEventAdapter;
import org.eclipse.net4j.util.container.IContainer;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.lifecycle.Lifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.security.IUserManager;
import org.eclipse.net4j.util.security.UserManager;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Eike Stepper
 */
public class DemoConfiguration extends Lifecycle
{
  private static final int NAME_LENGTH = 64;

  private static final String NAME_ALPHABET = "abcdefghijklmnopqrstuvwxyz";

  private Mode mode = Mode.NORMAL;

  private String[] userIDs;

  private transient String name;

  private transient InternalRepository repository;

  private transient long lastAccess;

  public DemoConfiguration()
  {
  }

  public Mode getMode()
  {
    return mode;
  }

  public void setMode(Mode mode)
  {
    checkInactive();
    this.mode = mode;
  }

  public String[] getUserIDs()
  {
    return userIDs;
  }

  public void setUserIDs(String[] userIDs)
  {
    checkInactive();
    this.userIDs = userIDs;
  }

  public String getName()
  {
    return name;
  }

  public IRepository getRepository()
  {
    return repository;
  }

  public long getLastAccess()
  {
    return lastAccess;
  }

  @Override
  protected void doActivate() throws Exception
  {
    name = createRandomName();

    IDBStore store = createStore();

    Map<String, String> props = new HashMap<String, String>();
    props.put(IRepository.Props.OVERRIDE_UUID, ""); // Use repo name
    props.put(IRepository.Props.SUPPORTING_AUDITS, mode == Mode.NORMAL ? "false" : "true");
    props.put(IRepository.Props.SUPPORTING_BRANCHES, mode == Mode.BRANCHING ? "true" : "false");

    repository = (InternalRepository)CDOServerUtil.createRepository(name, store, props);

    InternalSessionManager sessionManager = createSessionManager();

    if (userIDs != null)
    {
      IUserManager userManager = createUserManager();
      sessionManager.setUserManager(userManager);
    }

    LifecycleUtil.activate(repository);
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    if (repository != null)
    {
      LifecycleUtil.deactivate(repository);
      repository = null;
    }
  }

  protected String createRandomName()
  {
    Random random = new Random(System.currentTimeMillis());
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < NAME_LENGTH; i++)
    {
      int pos = random.nextInt(NAME_ALPHABET.length());
      builder.append(NAME_ALPHABET.charAt(pos));
    }

    return builder.toString();
  }

  protected IDBStore createStore()
  {
    IMappingStrategy mappingStrategy = createMappingStrategy();
    IDBAdapter dbAdapter = DBUtil.getDBAdapter("h2");
    IDBConnectionProvider dbConnectionProvider = DBUtil.createConnectionProvider(createDataSource());
    IDBStore store = CDODBUtil.createStore(mappingStrategy, dbAdapter, dbConnectionProvider);
    return store;
  }

  protected IMappingStrategy createMappingStrategy()
  {
    switch (mode)
    {
    case NORMAL:
      return CDODBUtil.createHorizontalMappingStrategy(false, false);
    case AUDITING:
      return CDODBUtil.createHorizontalMappingStrategy(true, false);
    case BRANCHING:
      return CDODBUtil.createHorizontalMappingStrategy(true, true);
    default:
      throw new IllegalStateException("Invalid mode: " + mode);
    }
  }

  protected DataSource createDataSource()
  {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:databases/" + name + "/h2test;SCHEMA=" + name);
    return dataSource;
  }

  protected InternalSessionManager createSessionManager()
  {
    InternalSessionManager sessionManager = (InternalSessionManager)CDOServerUtil.createSessionManager();
    repository.setSessionManager(sessionManager);
    sessionManager.addListener(new ContainerEventAdapter<ISession>()
    {
      @Override
      protected void onAdded(IContainer<ISession> container, ISession session)
      {
        ISignalProtocol<?> protocol = (ISignalProtocol<?>)session.getProtocol();
        protocol.addListener(new IListener()
        {
          public void notifyEvent(IEvent event)
          {
            lastAccess = System.currentTimeMillis();
          }
        });
      }
    });

    return sessionManager;
  }

  protected IUserManager createUserManager()
  {
    UserManager userManager = new UserManager();
    for (int i = 0; i < userIDs.length; i++)
    {
      String userID = userIDs[i];
      userManager.addUser(userID, ("pw" + (i + 1)).toCharArray());
    }

    userManager.activate();
    return userManager;
  }

  /**
   * @author Eike Stepper
   */
  public enum Mode
  {
    NORMAL, AUDITING, BRANCHING
  }
}
