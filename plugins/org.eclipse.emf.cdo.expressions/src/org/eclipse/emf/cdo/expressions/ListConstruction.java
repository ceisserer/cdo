/*
 * Copyright (c) 2013, 2016 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.expressions;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>List Value</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.expressions.ListConstruction#getElements <em>Elements</em>}</li>
 * </ul>
 *
 * @see org.eclipse.emf.cdo.expressions.ExpressionsPackage#getListConstruction()
 * @model
 * @generated
 */
public interface ListConstruction extends Expression
{
  /**
   * Returns the value of the '<em><b>Elements</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.cdo.expressions.Expression}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Elements</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Elements</em>' containment reference list.
   * @see org.eclipse.emf.cdo.expressions.ExpressionsPackage#getListConstruction_Elements()
   * @model containment="true"
   * @generated
   */
  EList<Expression> getElements();

} // ListValue
