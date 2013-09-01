/*
 * Copyright (c) 2008-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Martin Taal - initial api
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.server.internal.hibernate.tuplizer;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.revision.CDOListFactory;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.server.IStoreChunkReader.Chunk;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateStoreAccessor;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateStoreChunkReader;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateThreadContext;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateUtil;
import org.eclipse.emf.cdo.spi.common.revision.CDOReferenceAdjuster;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDOList;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a moveable list so that hibernate always sees an object view while cdo always sees a cdoid view. The same for
 * EEnum: cdo wants to see an int (the ordinal), hibernate the real eenum value. This to support querying with EENum
 * parameters.
 *
 * @author Martin Taal
 */
public class WrappedHibernateList implements InternalCDOList
{
  private List<Object> delegate;

  private boolean frozen;

  private int cachedSize = -1;

  private final EStructuralFeature eFeature;

  private final InternalCDORevision owner;

  private Chunk cachedChunk;

  private int currentListChunk = -1;

  public WrappedHibernateList(InternalCDORevision owner, EStructuralFeature eFeature)
  {
    this.owner = owner;
    this.eFeature = eFeature;
    final HibernateStoreAccessor accessor = HibernateThreadContext.getCurrentStoreAccessor();
    if (accessor != null)
    {
      currentListChunk = accessor.getCurrentListChunk();
    }
  }

  public void move(int newPosition, Object object)
  {
    checkFrozen();
    move(newPosition, indexOf(object));
  }

  public Object move(int targetIndex, int sourceIndex)
  {
    checkFrozen();
    int size = size();
    if (sourceIndex >= size)
    {
      throw new IndexOutOfBoundsException("sourceIndex=" + sourceIndex + ", size=" + size); //$NON-NLS-1$ //$NON-NLS-2$
    }

    if (targetIndex >= size)
    {
      throw new IndexOutOfBoundsException("targetIndex=" + targetIndex + ", size=" + size); //$NON-NLS-1$ //$NON-NLS-2$
    }

    Object object = get(sourceIndex);
    if (targetIndex == sourceIndex)
    {
      return object;
    }

    if (targetIndex < sourceIndex)
    {
      moveUp1(targetIndex, sourceIndex - targetIndex);
    }
    else
    {
      moveDown1(targetIndex, targetIndex - sourceIndex);
    }

    set(targetIndex, object);
    return object;
  }

  private void moveUp1(int index, int count)
  {
    for (int i = count; i > 0; i--)
    {
      set(index + i, get(index + i - 1));
    }
  }

  private void moveDown1(int index, int count)
  {
    for (int i = count; i > 0; i--)
    {
      set(index - i, get(index - i + 1));
    }
  }

  /**
   * There's a duplicate of this method in CDOListImpl!!!
   */
  public boolean adjustReferences(CDOReferenceAdjuster adjuster, EStructuralFeature feature)
  {
    boolean changed = false;

    CDOType type = CDOModelUtil.getType(feature);
    int size = size();
    for (int i = 0; i < size; i++)
    {
      Object element = get(i);
      Object newID = type.adjustReferences(adjuster, element, feature, i);
      if (newID != element) // Just an optimization for NOOP adjusters
      {
        set(i, newID);
        changed = true;
      }
    }

    return changed;
  }

  public InternalCDOList clone(EClassifier classifier)
  {
    CDOType type = CDOModelUtil.getType(classifier);
    int size = size();
    InternalCDOList list = (InternalCDOList)CDOListFactory.DEFAULT.createList(size, 0, 0);
    for (int i = 0; i < size; i++)
    {
      list.add(type.copyValue(get(i)));
    }

    if (classifier instanceof EClass)
    {
      WrappedHibernateList wrapped = new WrappedHibernateList(owner, eFeature);
      wrapped.setDelegate(list);
      return wrapped;
    }
    return list;
  }

  /**
   * @return the delegate
   */
  public List<Object> getDelegate()
  {
    if (delegate instanceof AbstractPersistentCollection && !((AbstractPersistentCollection)delegate).wasInitialized()
        && !isConnectedToSession())
    {
      // use a dummy auto-expanding list
      setDelegate(new UninitializedCollection<Object>()
      {
        private static final long serialVersionUID = 1L;

        @Override
        public Object set(int index, Object element)
        {
          ensureSize(index);
          return super.set(index, element);
        }

        @Override
        public Object get(int index)
        {
          ensureSize(index);
          final Object o = super.get(index);
          if (o == null)
          {
            return CDORevisionUtil.UNINITIALIZED;
          }
          return o;
        }

        private void ensureSize(int index)
        {
          if (index >= size())
          {
            for (int i = size() - 1; i <= index; i++)
            {
              add(null);
            }
          }
        }

      });
    }

    return delegate;
  }

  protected boolean isConnectedToSession()
  {
    final AbstractPersistentCollection persistentCollection = (AbstractPersistentCollection)delegate;
    final SessionImplementor session = persistentCollection.getSession();
    return session != null && session.isOpen()
        && session.getPersistenceContext().containsCollection(persistentCollection);
  }

  /**
   * @param delegate
   *          the delegate to set
   */
  public void setDelegate(List<Object> delegate)
  {
    this.delegate = delegate;
  }

  private static Object convertToCDO(Object value)
  {
    if (value == null)
    {
      return null;
    }

    // Eike: This seems wrong to me:
    // if (value instanceof CDOID)
    // {
    // return HibernateUtil.getInstance().getCDORevision((CDOID)value);
    // }

    if (value instanceof CDORevision || value instanceof HibernateProxy)
    {
      return HibernateUtil.getInstance().getCDOID(value);
    }

    if (value instanceof EEnumLiteral)
    {
      return ((EEnumLiteral)value).getValue();
    }

    return value;
  }

  private static List<Object> convertToCDO(List<?> ids)
  {
    List<Object> result = new ArrayList<Object>();
    for (Object o : ids)
    {
      result.add(convertToCDO(o));
    }

    return result;
  }

  protected Object getCDOValue(Object o)
  {
    if (o instanceof CDOID)
    {
      return o;
    }

    if (o instanceof HibernateProxy || o instanceof CDORevision)
    {
      return HibernateUtil.getInstance().getCDOID(o);
    }

    // primitive type
    return o;
  }

  protected List<Object> getCDOValues(Collection<?> c)
  {
    List<Object> newC = new ArrayList<Object>();
    for (Object o : c)
    {
      newC.add(getCDOValue(o));
    }

    return newC;
  }

  public void add(int index, Object element)
  {
    checkFrozen();
    getDelegate().add(index, getCDOValue(element));
  }

  public boolean add(Object o)
  {
    checkFrozen();
    return getDelegate().add(getCDOValue(o));
  }

  public boolean addAll(Collection<? extends Object> c)
  {
    checkFrozen();
    return getDelegate().addAll(getCDOValues(c));
  }

  public boolean addAll(int index, Collection<? extends Object> c)
  {
    checkFrozen();
    return getDelegate().addAll(index, getCDOValues(c));
  }

  public void clear()
  {
    checkFrozen();
    getDelegate().clear();
  }

  public boolean contains(Object o)
  {
    return getDelegate().contains(getCDOValue(o));
  }

  public boolean containsAll(Collection<?> c)
  {
    return getDelegate().containsAll(getCDOValues(c));
  }

  public Object get(int index)
  {
    final Object delegateValue = getDelegate().get(index);
    if (delegateValue instanceof CDOID)
    {
      return delegateValue;
    }

    return convertToCDO(delegateValue);
  }

  public Object get(int index, boolean resolve)
  {
    // if the collection is not initialized then always return
    // uninitialized to prevent loading it aggresively
    if (!resolve && currentListChunk > -1 && eFeature instanceof EReference
        && getDelegate() instanceof AbstractPersistentCollection)
    {
      final AbstractPersistentCollection collection = (AbstractPersistentCollection)getDelegate();
      if (!collection.wasInitialized())
      {
        final Object chunkedValue = getChunkedValue(index);
        if (chunkedValue != null)
        {
          return chunkedValue;
        }
        return CDORevisionUtil.UNINITIALIZED;
      }
    }

    return get(index);
  }

  private Object getChunkedValue(int index)
  {
    if (index >= currentListChunk)
    {
      return null;
    }
    readInitialChunk(index);
    if (cachedChunk != null)
    {
      // note index must be within the range as the chunk
      // is read again if index is too large.
      return cachedChunk.get(index);
    }
    return null;
  }

  private void readInitialChunk(int index)
  {

    if (cachedChunk != null)
    {
      if (index < cachedChunk.size())
      {
        // a valid chunk
        return;
      }
      // a not valid chunk
      // reread it
      cachedChunk = null;
    }
    final HibernateStoreAccessor accessor = HibernateThreadContext.getCurrentStoreAccessor();
    if (accessor == null)
    {
      return;
    }
    if (currentListChunk > -1)
    {
      final HibernateStoreChunkReader chunkReader = accessor.createChunkReader(owner, eFeature);
      chunkReader.addRangedChunk(0, currentListChunk);
      cachedChunk = chunkReader.executeRead().get(0);
    }
  }

  public int indexOf(Object o)
  {
    return getDelegate().indexOf(getCDOValue(o));
  }

  public boolean isEmpty()
  {
    return getDelegate().isEmpty();
  }

  public Iterator<Object> iterator()
  {
    return new CDOHibernateIterator(getDelegate().iterator());
  }

  public int lastIndexOf(Object o)
  {
    return getDelegate().lastIndexOf(getCDOValue(o));
  }

  public ListIterator<Object> listIterator()
  {
    return new CDOHibernateListIterator(this, getDelegate().listIterator());
  }

  public ListIterator<Object> listIterator(int index)
  {
    return new CDOHibernateListIterator(this, getDelegate().listIterator(index));
  }

  public Object remove(int index)
  {
    checkFrozen();
    return getDelegate().remove(index);
  }

  public boolean remove(Object o)
  {
    checkFrozen();
    return getDelegate().remove(getCDOValue(o));
  }

  public boolean removeAll(Collection<?> c)
  {
    checkFrozen();
    return getDelegate().removeAll(getCDOValues(c));
  }

  public boolean retainAll(Collection<?> c)
  {
    return getDelegate().retainAll(getCDOValues(c));
  }

  public Object set(int index, Object element)
  {
    checkFrozen();

    if (element == CDORevisionUtil.UNINITIALIZED)
    {
      return null;
    }

    if (element instanceof CDOID)
    {
      return getDelegate().set(index, element);
    }

    return getDelegate().set(index, getCDOValue(element));
  }

  public int size()
  {
    if (cachedSize != -1)
    {
      return cachedSize;
    }
    if (getDelegate() instanceof AbstractPersistentCollection)
    {
      final AbstractPersistentCollection collection = (AbstractPersistentCollection)getDelegate();
      if (collection.wasInitialized())
      {
        cachedSize = -1;
        return getDelegate().size();
      }
      final SessionImplementor session = collection.getSession();
      CollectionEntry entry = session.getPersistenceContext().getCollectionEntry(collection);
      CollectionPersister persister = entry.getLoadedPersister();
      if (collection.hasQueuedOperations())
      {
        session.flush();
      }
      cachedSize = persister.getSize(entry.getLoadedKey(), session);
      return cachedSize;
    }

    return getDelegate().size();
  }

  public List<Object> subList(int fromIndex, int toIndex)
  {
    return convertToCDO(getDelegate().subList(fromIndex, toIndex));
  }

  public Object[] toArray()
  {
    Object[] result = new Object[size()];
    int i = 0;
    for (Object o : this)
    {
      result[i++] = o;
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a)
  {
    int i = 0;
    for (Object o : this)
    {
      a[i++] = (T)o;
    }

    return a;
  }

  private static final class CDOHibernateIterator implements Iterator<Object>
  {
    private final Iterator<?> delegate;

    public CDOHibernateIterator(Iterator<?> delegate)
    {
      this.delegate = delegate;
    }

    public boolean hasNext()
    {
      return delegate.hasNext();
    }

    public Object next()
    {
      Object value = delegate.next();
      return convertToCDO(value);
    }

    public void remove()
    {
      delegate.remove();
    }
  }

  private static final class CDOHibernateListIterator implements ListIterator<Object>
  {
    private final ListIterator<Object> delegate;

    private final WrappedHibernateList owner;

    public CDOHibernateListIterator(WrappedHibernateList owner, ListIterator<Object> delegate)
    {
      this.delegate = delegate;
      this.owner = owner;
    }

    public void add(Object o)
    {
      owner.checkFrozen();

      delegate.add(HibernateUtil.getInstance().getCDOID(o));
    }

    public boolean hasNext()
    {
      return delegate.hasNext();
    }

    public boolean hasPrevious()
    {
      return delegate.hasPrevious();
    }

    public Object next()
    {
      Object value = delegate.next();
      return convertToCDO(value);
    }

    public int nextIndex()
    {
      return delegate.nextIndex();
    }

    public Object previous()
    {
      Object value = delegate.previous();
      return convertToCDO(value);
    }

    public int previousIndex()
    {
      return delegate.previousIndex();
    }

    public void remove()
    {
      owner.checkFrozen();
      delegate.remove();
    }

    public void set(Object o)
    {
      owner.checkFrozen();
      delegate.set(HibernateUtil.getInstance().getCDOID(o));
    }
  }

  public void freeze()
  {
    frozen = true;
  }

  private void checkFrozen()
  {
    // a frozen check always implies a modification
    cachedSize = -1;
    if (frozen)
    {
      throw new IllegalStateException("Cannot modify a frozen list");
    }
  }

  public void setWithoutFrozenCheck(int i, Object value)
  {
    getDelegate().set(i, value);
  }

  CDORevision getOwner()
  {
    return owner;
  }

  // tagging interface
  class UninitializedCollection<E> extends ArrayList<E>
  {
    private static final long serialVersionUID = 1L;
  }
}
