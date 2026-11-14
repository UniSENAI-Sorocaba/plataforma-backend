package plataforma.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import plataforma.rubric.RubricCalculationPolicy;
import plataforma.rubric.RubricCriterionDefinition;
import plataforma.rubric.RubricFormula;

/** Snapshot imutável das regras de uma rubrica. */
@Entity
@Table(schema = "academic_pi", name = "rubric_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_rubric_version_number", columnNames = {"rubric_id", "version_number"}))
public class RubricVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id", nullable = false)
    private Rubric rubric;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "minimum_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal minimumScore;

    @Column(name = "maximum_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal maximumScore;

    @Column(name = "rounding_scale", nullable = false)
    private int roundingScale;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 32)
    private RoundingMode roundingMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RubricFormula formula;

    @Column(name = "created_by_id")
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "rubricVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<RubricCriterion> criteria = new ArrayList<>();

    protected RubricVersion() {
        // JPA
    }

    public RubricVersion(int versionNumber, BigDecimal minimumScore, BigDecimal maximumScore,
            int roundingScale, RoundingMode roundingMode, RubricFormula formula, UUID createdById) {
        this.versionNumber = versionNumber;
        this.minimumScore = minimumScore;
        this.maximumScore = maximumScore;
        this.roundingScale = roundingScale;
        this.roundingMode = roundingMode;
        this.formula = formula;
        this.createdById = createdById;
    }

    void setRubric(Rubric rubric) {
        this.rubric = rubric;
    }

    public void addCriterion(RubricCriterion criterion) {
        criteria.add(criterion);
        criterion.setRubricVersion(this);
    }

    public UUID getId() {
        return id;
    }

    public Rubric getRubric() {
        return rubric;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public BigDecimal getMinimumScore() {
        return minimumScore;
    }

    public BigDecimal getMaximumScore() {
        return maximumScore;
    }

    public int getRoundingScale() {
        return roundingScale;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public RubricFormula getFormula() {
        return formula;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<RubricCriterion> getCriteria() {
        return List.copyOf(criteria);
    }

    public RubricCalculationPolicy calculationPolicy() {
        return new RubricCalculationPolicy(
                minimumScore,
                maximumScore,
                roundingScale,
                roundingMode,
                formula,
                criteria.stream().map(criterion -> new RubricCriterionDefinition(
                        criterion.getCode(), criterion.getWeight(), criterion.getMinimumScore(),
                        criterion.getMaximumScore())).toList());
    }
}
