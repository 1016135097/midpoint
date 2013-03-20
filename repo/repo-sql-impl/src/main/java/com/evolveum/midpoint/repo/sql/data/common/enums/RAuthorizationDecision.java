/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;

/**
 * @author lazyman
 */
public enum RAuthorizationDecision {

    ALLOW(AuthorizationDecisionType.ALLOW),

    DENY(AuthorizationDecisionType.DENY);

    private AuthorizationDecisionType decision;

    private RAuthorizationDecision(AuthorizationDecisionType decision) {
        this.decision = decision;
    }

    public AuthorizationDecisionType getDecision() {
        return decision;
    }

    public static RAuthorizationDecision getDecision(AuthorizationDecisionType decision) {
        if (decision == null) {
            return null;
        }

        for (RAuthorizationDecision dec : RAuthorizationDecision.values()) {
            if (dec.getDecision().equals(decision)) {
                return dec;
            }
        }

        throw new IllegalArgumentException("Unknown authorization decision type: " + decision);
    }
}
