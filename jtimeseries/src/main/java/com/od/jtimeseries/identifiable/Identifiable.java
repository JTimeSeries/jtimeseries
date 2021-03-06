/**
 * Copyright (C) 2011 (nick @ objectdefinitions.com)
 *
 * This file is part of JTimeseries.
 *
 * JTimeseries is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JTimeseries is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JTimeseries.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.od.jtimeseries.identifiable;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 04-Jan-2009
 * Time: 15:22:20
 *
 * JTimeseries API supports a tree of Identifiables, where an Identifiable instance is a node of
 * the tree structure, which has a String id
 *
 * The path is the path through the tree taking each id, and terminating with the id of this
 * node, with a '.' separator between ids
 */
public interface Identifiable {

    String NAMESPACE_SEPARATOR = ".";
    int DESCRIPTION_MAX_LENGTH = 350; //sized to allow description to easily fit into a single UDP packet of 512bytes

    String getId();

    String getParentPath();

    /**
     * @return fully qualified namespace and id
     */
    String getPath();

    /**
     * @param description, must not be greater than 350 characters in length since this may be sent over the network
     * in a UDP package, or displayed in a UI which requires a maximum sizing
     */
    void setDescription(String description);

    String getDescription();

    Identifiable getParent();

    /**
     * Set the parent of a Identifiable without firing events
     * n.b. parent is set automatically when you add this node to its parent using parentNode.addChild(), so you should
     * not usually need to call this method from application code. parent.adChild() is the preferred way to do things, since it will
     * also fire the appropriate IdentifiableTreeEvent
     * @return old parent or null if no previous parent
     */
    Identifiable setParent(Identifiable parent);

    Identifiable addChild(Identifiable... identifiables);

    boolean removeChild(Identifiable c);

    List<Identifiable> getChildren();

    int getChildCount();

    <E extends Identifiable> List<E> getChildren(Class<E> classType);

    Identifiable get(String path);

    boolean contains(String path);

    /**
     * @return the Identifiable at path which is assignable to classType, or null if no identifiable exists at this path
     * @throws WrongClassTypeException if there is an identifiable at path which is not assignable to classType
     */
    <E extends Identifiable> E get(String path, Class<E> classType);

    /**
     * @return a newly created Identifiable at path which is assignable to classType
     * @throws DuplicateIdException if there is already an identifiable at path
     */
    <E extends Identifiable> E create(String path, String description, Class<E> classType, Object... parameters);

    /**
     * @return Identifiable at path which is assignable to classType - the existing instance if an Identifiable already exists, if not a newly created instance
     * @throws WrongClassTypeException if there is already an identifiable at path which is not assignable to classType
     */
    <E extends Identifiable> E getOrCreate(String path, String description, Class<E> classType, Object... parameters);

    Identifiable remove(String path);

    <E extends Identifiable> E remove(String path, Class<E> classType); 

    /**
     * Structural changes made to the context tree structure should be made
     * while holding the write lock on this Object
     */
    ReentrantReadWriteLock getTreeLock();

    Identifiable getRoot();

    boolean isRoot();

    boolean containsChildWithId(String id);

    boolean containsChild(Identifiable i);

    /**
     * @return value associated with propertyName for this identifiable, or null if property not set
     */
    String getProperty(String propertyName);

    /**
     * @return a copy of the properties associated with this identifiable
     */
    Properties getProperties();


    /**
     * Add all the properties into the identifiables properties map, replacing any with matching keys
     */
    void putAllProperties(Properties p);
    
     /**
     * This mechanism allows an Identifiable to 'inherit' a property from a parent
     *
     * @return a value for propertyName by searching the identifiable tree starting with this node
     * and progressing upwards to the root until a value is found. returns null if no value can be found.
     */
    String findProperty(String propertyName);

    /**
     * Set the propertyName to the supplied value
     * @return the previous value for propertyName, or null if property was not set
     */
    String setProperty(String propertyName, String value);

    /**
     * Remove the property with key propertyName
     * @return the previous value for propertyName, or null if property was not set
     */
    String removeProperty(String propertyName);

    <E extends Identifiable> E getFromAncestors(String id, Class<E> clazz);

    void addTreeListener(IdentifiableTreeListener l);

    void removeTreeListener(IdentifiableTreeListener l);

    /**
     * fire an event to IdentifiableTreeListener to indicate this node has changed
     */
    void fireNodeChanged(Object changeDescription);

    <E extends Identifiable> QueryResult<E> findAll(Class<E> assignableToClass);

    <E extends Identifiable> QueryResult<E> findAll(String searchPattern, Class<E> assignableToClass);

    <E extends Identifiable> QueryResult<E> findAll(Class<E> assignableToClass, FindCriteria<E> criteria);
}
