/*
 * Copyright (c) 2007, 2011, 2012, 2015, 2019 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.util.concurrent;

/**
 * @author Eike Stepper
 * @deprecated As of 3.9 subject to future removal.
 */
@Deprecated
public class Sleeper
{
  private static final int DEFAULT_INTERVAL = 10;

  private long start;

  private int interval;

  public Sleeper()
  {
    this(DEFAULT_INTERVAL);
  }

  public Sleeper(int interval)
  {
    this.interval = interval;
    restart();
  }

  public int getInterval()
  {
    return interval;
  }

  public long getStart()
  {
    return start;
  }

  public void restart()
  {
    start = System.currentTimeMillis();
  }

  public void sleep(long millis)
  {
    while (System.currentTimeMillis() < start + millis)
    {
      ConcurrencyUtil.sleep(interval);
    }
  }

  public void resleep(long millis)
  {
    restart();
    sleep(millis);
  }
}
