package plataforma.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Identidade estável de uma rubrica; as regras vivem em versões imutáveis. */
@Entity
@Table(schema = "academic_pi", name = "rubric")
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_by_id")
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RubricVersion> versions = new ArrayList<>();

    protected Rubric() {
        // JPA
    }

    public Rubric(String code, String name, UUID createdById) {
        this.code = code;
        this.name = name;
        this.createdById = createdById;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<RubricVersion> getVersions() {
        return List.copyOf(versions);
    }

    public void addVersion(RubricVersion version) {
        versions.add(version);
        version.setRubric(this);
    }
}
