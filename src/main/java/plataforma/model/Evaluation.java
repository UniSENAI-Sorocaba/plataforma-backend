package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.CascadeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Notas de um avaliador da banca, por critério da rubrica (0 a 10). */
@Entity
@Table(schema = "academic_pi", name = "evaluation",
        uniqueConstraints = @UniqueConstraint(name = "uk_evaluation_board_evaluator", columnNames = {"board_id", "evaluator_id"}))
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private ExaminationBoard board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private UserAccount evaluator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_version_id")
    private RubricVersion rubricVersion;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvaluationCriterionScore> criterionScores = new ArrayList<>();

    @Column(name = "technical_grade", precision = 10, scale = 4)
    private BigDecimal technicalGrade;

    @Column(name = "presentation_grade", precision = 10, scale = 4)
    private BigDecimal presentationGrade;

    @Column(name = "artifact_grade", precision = 10, scale = 4)
    private BigDecimal artifactGrade;

    /** Resultado calculado pela versão da rubrica informada no lançamento. */
    @Column(name = "final_grade", precision = 4, scale = 2)
    private BigDecimal finalGrade;

    @Column(columnDefinition = "text")
    private String feedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public ExaminationBoard getBoard() {
        return board;
    }

    public void setBoard(ExaminationBoard board) {
        this.board = board;
    }

    public UserAccount getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(UserAccount evaluator) {
        this.evaluator = evaluator;
    }

    public RubricVersion getRubricVersion() {
        return rubricVersion;
    }

    public void setRubricVersion(RubricVersion rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public List<EvaluationCriterionScore> getCriterionScores() {
        return List.copyOf(criterionScores);
    }

    public void replaceCriterionScores(List<EvaluationCriterionScore> scores) {
        criterionScores.clear();
        scores.forEach(score -> {
            criterionScores.add(score);
            score.setEvaluation(this);
        });
    }

    public BigDecimal getTechnicalGrade() {
        return technicalGrade;
    }

    public void setTechnicalGrade(BigDecimal technicalGrade) {
        this.technicalGrade = technicalGrade;
    }

    public BigDecimal getPresentationGrade() {
        return presentationGrade;
    }

    public void setPresentationGrade(BigDecimal presentationGrade) {
        this.presentationGrade = presentationGrade;
    }

    public BigDecimal getArtifactGrade() {
        return artifactGrade;
    }

    public void setArtifactGrade(BigDecimal artifactGrade) {
        this.artifactGrade = artifactGrade;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
