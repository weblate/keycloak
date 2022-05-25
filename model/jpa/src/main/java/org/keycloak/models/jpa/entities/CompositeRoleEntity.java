package org.keycloak.models.jpa.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.jpa.QueryHints;

@Entity
@Table(name="COMPOSITE_ROLE", uniqueConstraints = {
      @UniqueConstraint(columnNames = { "COMPOSITE", "CHILD_ROLE" })
})
@NamedQueries({
    @NamedQuery(name="getChildrenRoles",
            query="select compositerole.child from CompositeRoleEntity compositerole where compositerole.composite.id = :roleId order by compositerole.child.id"),
    @NamedQuery(name="removeCompositeAndChildRoleEntry",
            query="delete from CompositeRoleEntity compositerole where compositerole.composite.id = :compositeId and compositerole.child.id = :childId"),
})
public class CompositeRoleEntity implements Serializable {
    private static final long serialVersionUID = 7375648337789837909L;

    @EmbeddedId
    private CompositeRoleEntityKey key = new CompositeRoleEntityKey();

    @MapsId("compositeId")
    @JoinColumn(name="COMPOSITE", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private RoleEntity composite;

    @MapsId("childRoleId")
    @JoinColumn(name="CHILD_ROLE", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private RoleEntity child;

    public CompositeRoleEntity() {
    }

    public CompositeRoleEntity(RoleEntity composite, RoleEntity child) {
        this.composite = composite;
        this.child = child;
    }
    public RoleEntity getComposite() {
        return composite;
    }

    public RoleEntity getChild() {
        return child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleEntity)) return false;

        CompositeRoleEntity that = (CompositeRoleEntity) o;

        if (!composite.equals(that.composite)) return false;
        if (!child.equals(that.child)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(composite.getId(), child.getId());
    }
    
}
