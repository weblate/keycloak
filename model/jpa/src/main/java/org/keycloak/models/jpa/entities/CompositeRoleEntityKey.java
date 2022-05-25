package org.keycloak.models.jpa.entities;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * The composite primary key representation for {@link CompositeRoleEntity}.
 * Required to perform lookup by primary key through JPA entity manager.
 */
@Embeddable
public class CompositeRoleEntityKey implements Serializable {

    private static final long serialVersionUID = -6078479162595894390L;

    private String compositeId;

    private String childRoleId;

    public CompositeRoleEntityKey() {
    }

    public CompositeRoleEntityKey(String compositeId, String childRoleId) {
        this.compositeId = compositeId;
        this.childRoleId = childRoleId;
    }

    public String getCompositeId() {
        return compositeId;
    }

    public void setCompositeId(String compositeId) {
        this.compositeId = compositeId;
    }

    public String getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(String childRoleId) {
        this.childRoleId = childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleEntityKey)) return false;

        CompositeRoleEntityKey that = (CompositeRoleEntityKey) o;

        if (!compositeId.equals(that.compositeId)) return false;
        if (!childRoleId.equals(that.childRoleId)) return false;

        return true;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(compositeId, childRoleId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CompositeRolePK [")
                .append("compositeId=").append(compositeId)
                .append(", childRoleId=").append(childRoleId)
                .append("]");
        return builder.toString();
    }

}