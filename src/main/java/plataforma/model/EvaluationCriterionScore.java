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

/** Nota lançada para um critério da versão usada pela avaliação. */
@Entity
@Table(schema = "academic_pi", name = "evaluation_criterion_score",
        uniqueConstraints = @UniqueConstraint(name = "uk_evaluation_criterion_score", columnNames = {"evaluation_id", "criterion_id"}))
public class EvaluationCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private RubricCriterion criterion;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal score;

    protected EvaluationCriterionScore() {
        // JPA
    }

    public EvaluationCriterionScore(RubricCriterion criterion, BigDecimal score) {
        this.criterion = criterion;
        this.score = score;
    }

    void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public UUID getId() {
        return id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public RubricCriterion getCriterion() {
        return criterion;
    }

    public BigDecimal getScore() {
        return score;
    }
}
