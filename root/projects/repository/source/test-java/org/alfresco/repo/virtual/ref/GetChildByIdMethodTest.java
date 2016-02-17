/* 
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see http://www.gnu.org/licenses/.
 */

package org.alfresco.repo.virtual.ref;

import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;

public class GetChildByIdMethodTest extends TestCase
{

    private String toChildPath(final String parentPath, String childName) throws ProtocolMethodException
    {
        StringParameter path = new StringParameter(parentPath);

        Reference ref = new Reference(Encodings.PLAIN.encoding,
                                      Protocols.VIRTUAL.protocol,
                                      new ClasspathResource("/a/class/path.js"),
                                      Arrays.asList(path));
        GetChildByIdMethod method = new GetChildByIdMethod(childName);

        Reference childRef = ref.execute(method);

        assertEquals(ref.getResource(),
                     childRef.getResource());
        assertEquals(ref.getProtocol(),
                     childRef.getProtocol());

        return childRef.execute(new GetTemplatePathMethod());
    }

    @Test
    public void testExecute() throws Exception
    {
        final String parentPath = "/root";
        final String childName = "aChid";
        String childPath = toChildPath(parentPath,
                                       childName);

        assertEquals(parentPath + "/" + childName,
                     childPath);
    }

    @Test
    public void testTrailingPath() throws Exception
    {
        String childPath = toChildPath("  /root/   ",
                                       "child");

        assertEquals("/root/child",
                     childPath);
    }
}
