/*
 * Copyright (c) 2009-2013, 2015, 2016, 2018, 2019 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Victor Roldan Betancort - maintenance
 *    Simon McDuff - maintenance
 *    Christian W. Damus (CEA) - support registered dynamic UML profiles
 *    Christian W. Damus (CEA) - don't process EAnnotations for proxy resolution
 */
package org.eclipse.emf.cdo.common.model;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.function.Predicate;

/**
 * Predefined {@link java.util.function.Predicate predicates} to test for various {@link EStructuralFeature feature} types.
 *
 * @author Eike Stepper
 * @since 4.9
 */
public final class EMFPredicates
{
  public static final Predicate<EStructuralFeature> ATTRIBUTES = new Predicate<EStructuralFeature>()
  {
    @Override
    public boolean test(EStructuralFeature feature)
    {
      return feature instanceof EAttribute;
    }
  };

  public static final Predicate<EStructuralFeature> REFERENCES = new Predicate<EStructuralFeature>()
  {
    @Override
    public boolean test(EStructuralFeature feature)
    {
      return feature instanceof EReference;
    }
  };

  public static final Predicate<EStructuralFeature> CONTAINER_REFERENCES = new Predicate<EStructuralFeature>()
  {
    @Override
    public boolean test(EStructuralFeature feature)
    {
      if (feature instanceof EReference)
      {
        EReference reference = (EReference)feature;
        return reference.isContainer();
      }

      return false;
    }
  };

  public static final Predicate<EStructuralFeature> CROSS_REFERENCES = new Predicate<EStructuralFeature>()
  {
    @Override
    public boolean test(EStructuralFeature feature)
    {
      if (feature instanceof EReference)
      {
        EReference reference = (EReference)feature;
        return !(reference.isContainer() || reference.isContainment());
      }

      return false;
    }
  };

  public static final Predicate<EStructuralFeature> CONTAINMENT_REFERENCES = new Predicate<EStructuralFeature>()
  {
    @Override
    public boolean test(EStructuralFeature feature)
    {
      if (feature instanceof EReference)
      {
        EReference reference = (EReference)feature;
        return reference.isContainment();
      }

      return false;
    }
  };

  private EMFPredicates()
  {
  }
}
