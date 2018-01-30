/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentMapper extends ContainerMapper<AssignmentType, RAssignment> {

    @Override
    public RAssignment map(AssignmentType input, MapperContext context) {
        RAssignment ass = new RAssignment();

        RObject owner = (RObject) context.getOwner();

        RepositoryContext repositoryContext =
                new RepositoryContext(context.getRepositoryService(), context.getPrismContext());

        try {
            RAssignment.copyFromJAXB(input, ass, owner, repositoryContext);
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate assignment to entity", ex);
        }

        return ass;
    }
}
