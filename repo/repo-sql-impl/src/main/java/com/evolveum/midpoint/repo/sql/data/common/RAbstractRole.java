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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.enums.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RRoleApproverRef;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_abstract_role")
@org.hibernate.annotations.Table(appliesTo = "m_abstract_role",
        indexes = {@Index(name = "iRequestable", columnNames = "requestable")})
public abstract class RAbstractRole extends RObject {

    private Set<RAssignment> assignments;
    private Set<RExclusion> exclusions;
    @QueryAttribute
    private Boolean requestable;
    private Set<RObjectReference> approverRefs;
    private String approvalProcess;
    private String approvalSchema;
    private String approvalExpression;
    private String automaticallyApproved;
    private Set<RAuthorization> authorizations;

    @OneToMany(mappedBy = RAuthorization.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAuthorization> getAuthorizations() {
        if (authorizations == null) {
            authorizations = new HashSet<RAuthorization>();
        }
        return authorizations;
    }

    public Boolean getRequestable() {
        return requestable;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getAutomaticallyApproved() {
        return automaticallyApproved;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getApprovalExpression() {
        return approvalExpression;
    }

    @Column(nullable = true)
    public String getApprovalProcess() {
        return approvalProcess;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getApprovalSchema() {
        return approvalSchema;
    }

    @OneToMany(mappedBy = RAssignment.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignment> getAssignments() {
        if (assignments == null) {
            assignments = new HashSet<RAssignment>();
        }
        return assignments;
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RExclusion> getExclusions() {
        if (exclusions == null) {
            exclusions = new HashSet<RExclusion>();
        }
        return exclusions;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RRoleApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getApproverRefs() {
        if (approverRefs == null) {
            approverRefs = new HashSet<RObjectReference>();
        }
        return approverRefs;
    }

    public void setApproverRefs(Set<RObjectReference> approverRefs) {
        this.approverRefs = approverRefs;
    }

    public void setExclusions(Set<RExclusion> exclusions) {
        this.exclusions = exclusions;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setApprovalProcess(String approvalProcess) {
        this.approvalProcess = approvalProcess;
    }

    public void setApprovalSchema(String approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    public void setApprovalExpression(String approvalExpression) {
        this.approvalExpression = approvalExpression;
    }

    public void setAutomaticallyApproved(String automaticallyApproved) {
        this.automaticallyApproved = automaticallyApproved;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }

    public void setAuthorizations(Set<RAuthorization> authorizations) {
        this.authorizations = authorizations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RAbstractRole that = (RAbstractRole) o;

        if (assignments != null ? !assignments.equals(that.assignments) : that.assignments != null)
            return false;
        if (exclusions != null ? !exclusions.equals(that.exclusions) : that.exclusions != null)
            return false;
        if (approverRefs != null ? !approverRefs.equals(that.approverRefs) : that.approverRefs != null)
            return false;
        if (approvalProcess != null ? !approvalProcess.equals(that.approvalProcess) : that.approvalProcess != null)
            return false;
        if (approvalSchema != null ? !approvalSchema.equals(that.approvalSchema) : that.approvalSchema != null)
            return false;
        if (approvalExpression != null ? !approvalExpression.equals(that.approvalExpression) : that.approvalExpression != null)
            return false;
        if (automaticallyApproved != null ? !automaticallyApproved.equals(that.automaticallyApproved) : that.automaticallyApproved != null)
            return false;
        if (requestable != null ? !requestable.equals(that.requestable) : that.requestable != null)
            return false;
        if (authorizations != null ? !authorizations.equals(that.authorizations) : that.authorizations != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (approvalProcess != null ? approvalProcess.hashCode() : 0);
        result = 31 * result + (approvalSchema != null ? approvalSchema.hashCode() : 0);
        result = 31 * result + (approvalExpression != null ? approvalExpression.hashCode() : 0);
        result = 31 * result + (automaticallyApproved != null ? automaticallyApproved.hashCode() : 0);
        result = 31 * result + (requestable != null ? requestable.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RAbstractRole repo, AbstractRoleType jaxb, PrismContext prismContext)
            throws DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setRequestable(repo.getRequestable());
        if (repo.getAssignments() != null) {
            for (RAssignment rAssignment : repo.getAssignments()) {
                jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
            }
        }
        if (repo.getExclusions() != null) {
            for (RExclusion rExclusion : repo.getExclusions()) {
                jaxb.getExclusion().add(rExclusion.toJAXB(prismContext));
            }
        }
        if (repo.getAuthorizations() != null) {
            for (RAuthorization rAuth : repo.getAuthorizations()) {
                jaxb.getAuthorization().add(rAuth.toJAXB(prismContext));
            }
        }

        if (repo.getApproverRefs() != null) {
            for (RObjectReference repoRef : repo.getApproverRefs()) {
                jaxb.getApproverRef().add(repoRef.toJAXB(prismContext));
            }
        }

        jaxb.setApprovalProcess(repo.getApprovalProcess());
        try {
            jaxb.setApprovalSchema(RUtil.toJAXB(RoleType.class, new ItemPath(RoleType.F_APPROVAL_SCHEMA),
                    repo.getApprovalSchema(), ApprovalSchemaType.class, prismContext));

            if (StringUtils.isNotEmpty(repo.getApprovalExpression())) {
                List expressions = RUtil.toJAXB(RoleType.class, new ItemPath(RoleType.F_APPROVER_EXPRESSION),
                        repo.getApprovalExpression(), List.class, prismContext);
                jaxb.getApproverExpression().addAll(expressions);
            }

            if (StringUtils.isNotEmpty(repo.getAutomaticallyApproved())) {
                jaxb.setAutomaticallyApproved(RUtil.toJAXB(RoleType.class, new ItemPath(RoleType.F_AUTOMATICALLY_APPROVED),
                        repo.getAutomaticallyApproved(), ExpressionType.class, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(AbstractRoleType jaxb, RAbstractRole repo, PrismContext prismContext)
            throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);
        repo.setRequestable(jaxb.isRequestable());

        ContainerIdGenerator gen = new ContainerIdGenerator();
        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment(repo);

            RAssignment.copyFromJAXB(assignment, rAssignment, jaxb, prismContext);
            gen.generate(null, rAssignment);

            repo.getAssignments().add(rAssignment);
        }

        for (ExclusionType exclusion : jaxb.getExclusion()) {
            RExclusion rExclusion = new RExclusion(repo);

            RExclusion.copyFromJAXB(exclusion, rExclusion, jaxb, prismContext);
            gen.generate(null, rExclusion);

            repo.getExclusions().add(rExclusion);
        }

        for (AuthorizationType exclusion : jaxb.getAuthorization()) {
            RAuthorization rAuth = new RAuthorization(repo);

            RAuthorization.copyFromJAXB(exclusion, rAuth, jaxb, prismContext);
            gen.generate(null, rAuth);

            repo.getAuthorizations().add(rAuth);
        }

        for (ObjectReferenceType approverRef : jaxb.getApproverRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(approverRef, prismContext, repo, RReferenceOwner.ROLE_APPROVER);
            if (ref != null) {
                repo.getApproverRefs().add(ref);
            }
        }

        repo.setApprovalProcess(jaxb.getApprovalProcess());
        try {
            repo.setApprovalSchema(RUtil.toRepo(jaxb.getApprovalSchema(), prismContext));

            repo.setApprovalExpression(RUtil.toRepo(jaxb.getApproverExpression(), prismContext));
            repo.setAutomaticallyApproved(RUtil.toRepo(jaxb.getAutomaticallyApproved(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
