/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.handler;

import com.evolveum.midpoint.repo.sql.query.definition.Definition;

/**
 * @author lazyman
 */
public abstract class DefinitionHandler<D extends Definition> {

    private D definition;

    protected D getDefinition() {
        return definition;
    }

    public abstract boolean updateQueryContext();
//
//    public void updateVirtualQueryParams();
}
