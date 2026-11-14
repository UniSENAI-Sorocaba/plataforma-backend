package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/** Critério pertencente a uma versão específica, nunca compartilhado entre versões. */
@Entity
@Table(schema = "academic_pi", name = "rubric_criterion",
        uniqueConstraints = @UniqueConstraint(name = "uk_rubric_criterion_code", columnNames = {"rubric_version_id", "code"}))
public class RubricCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_version_id", nullable = false)
    private RubricVersion rubricVersion;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal weight;

    @Column(name = "minimum_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal minimumScore;

    @Column(name = "maximum_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal maximumScore;

    @Column(nullable = false)
    private int position;

    protected RubricCriterion() {
        // JPA
    }

    public RubricCriterion(String code, String name, BigDecimal weight,
            BigDecimal minimumScore, BigDecimal maximumScore, int position) {
        this.code = code;
        this.name = name;
        this.weight = weight;
        this.minimumScore = minimumScore;
        this.maximumScore = maximumScore;
        this.position = position;
    }

    void setRubricVersion(RubricVersion rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public UUID getId() {
        return id;
    }

    public RubricVersion getRubricVersion() {
        return rubricVersion;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getMinimumScore() {
        return minimumScore;
    }

    public BigDecimal getMaximumScore() {
        return maximumScore;
    }

    public int getPosition() {
        return position;
    }
}
