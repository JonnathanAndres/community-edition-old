/*
 * #%L
 * Alfresco Data model classes
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.search.impl.querymodel;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.service.namespace.QName;

/**
 * @author andyh
 */
public interface Function
{   
    /**
     * Evaluation a function
     * 
     * @param args Map<String, Argument>
     * @param context FunctionEvaluationContext
     * @return Serializable
     */
    public Serializable getValue(Map<String, Argument> args, FunctionEvaluationContext context);

    /**
     * Get the return type for the function
     * 
     * @return QName
     */
    public QName getReturnType();

    /**
     * Get the function name
     * 
     * @return String
     */
    public String getName();
    
    /**
     * Get the argument Definitions
     * @return LinkedHashMap
     */
    public LinkedHashMap<String, ArgumentDefinition> getArgumentDefinitions();
    
    
    /**
     * Get the argument Definition
     * @return ArgumentDefinition
     */
    public ArgumentDefinition getArgumentDefinition(String name);
    

}
